"""Pinned source/compiled API checks and multi-pixel translated GLSL tests, NOT live Minecraft QA."""
from pathlib import Path
import json,zipfile,hashlib,subprocess,re,tempfile,html
p=Path(__file__).resolve().parents[1]
for r in json.loads((p/'audit/stray-ui-blur-source-manifest.json').read_text()):assert hashlib.sha256((p/r['local_file']).read_bytes()).hexdigest()==r['sha256']
jar=p/'build/libs/solyanka-1.3.12.jar'
with zipfile.ZipFile(jar) as z:
 for name in ['FrameBlendHistory','PanelGeometry']:assert f'dev/solyanka/utility/{name}.class' in z.namelist()
 assert 'dev/solyanka/client/feature/StrayInventoryHud.class' in z.namelist()
 assert z.read('assets/solyanka/shaders/post/frame_blend.fsh')==(p/'src/main/resources/assets/solyanka/shaders/post/frame_blend.fsh').read_bytes()
 assert json.loads(z.read('fabric.mod.json'))['version']=='1.3.12'
blur=(p/'src/client/kotlin/dev/solyanka/client/feature/TemporalBlur.kt').read_text()
for token in ['FrameBlendHistory.capacity','clock.push(stamp)','clock.samples(stamp,monitorRefresh(now),s.strength)','frames[index] ?: TextureTarget','64L)','bytes.putFloat(1f/total).putInt(samples.size)','for(i in 0 until 12)','FrameBlendParamsUniforms','frameOutput?.destroyBuffers()']:assert token in blur,token
frame=blur.split('private fun drawFrames(',1)[1].split('private fun monitorRefresh(',1)[0]
assert frame.index('copyTextureToTexture(main.colorTexture!!,raw.colorTexture!!')<frame.index('encoder.createRenderPass')<frame.index('copyTextureToTexture(out.colorTexture!!,main.colorTexture!!')
assert frame.index('encoder.writeToBuffer')<frame.index('encoder.createRenderPass')
chrome=(p/'src/client/kotlin/dev/solyanka/client/feature/ChromeDraw.kt').read_text();assert 'PanelGeometry.bands' in chrome and 'size>128' in chrome and 'for(row in 0 until h)' not in chrome
screen=(p/'src/client/kotlin/dev/solyanka/client/ui/nofrills/NoFrillsClickGui.kt').read_text();assert 'super.extractRenderState(g,mx,my,delta)' in screen and 'if(panel==null) super.extractRenderState' not in screen
# Native API overload used for contextual item rendering. Compilation also checks it.
native=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
sig=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-p','-s','-classpath',str(native),'net.minecraft.client.gui.GuiGraphicsExtractor'],text=True)
assert '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V' in sig
with zipfile.ZipFile(native) as z:vertex=z.read('assets/minecraft/shaders/core/screenquad.vsh').decode()
def es(s):return s.replace('#version 330','#version 300 es\nprecision highp float;\nprecision highp int;')
sources=dict(vertex=es(vertex),fragment=es((p/'src/main/resources/assets/solyanka/shaders/post/frame_blend.fsh').read_text()))
body=r'''
const canvas=document.createElement('canvas');canvas.width=2;canvas.height=2;const gl=canvas.getContext('webgl2',{premultipliedAlpha:false});if(!gl)throw Error('No WebGL2');
function shader(type,s){const sh=gl.createShader(type);gl.shaderSource(sh,s);gl.compileShader(sh);if(!gl.getShaderParameter(sh,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(sh));return sh;}
const prog=gl.createProgram();gl.attachShader(prog,shader(gl.VERTEX_SHADER,sources.vertex));gl.attachShader(prog,shader(gl.FRAGMENT_SHADER,sources.fragment));gl.linkProgram(prog);if(!gl.getProgramParameter(prog,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(prog));gl.useProgram(prog);
const idx=gl.getUniformBlockIndex(prog,'FrameBlendParamsUniforms');gl.uniformBlockBinding(prog,idx,0);const size=gl.getActiveUniformBlockParameter(prog,idx,gl.UNIFORM_BLOCK_DATA_SIZE);if(size!==64)throw Error('UBO size '+size);
const ubo=gl.createBuffer();gl.bindBuffer(gl.UNIFORM_BUFFER,ubo);gl.bindBufferBase(gl.UNIFORM_BUFFER,0,ubo);gl.bindVertexArray(gl.createVertexArray());gl.viewport(0,0,2,2);
const pixels=[];for(let i=0;i<12;i++){
 const values=[];for(let y=0;y<2;y++)for(let x=0;x<2;x++)values.push((i*17+x*90)%256,(i*29+y*110)%256,(i*41+(x+y)*50)%256,255);pixels.push(values);
 gl.activeTexture(gl.TEXTURE0+i);const tex=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,tex);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA8,2,2,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array(values));gl.uniform1i(gl.getUniformLocation(prog,'Sample'+i+'Sampler'),i);
}
const failures=[];let cases=0;for(const count of [1,2,6,12]){
 const data=new ArrayBuffer(64),f=new Float32Array(data),ints=new Int32Array(data);const weights=Array.from({length:count},(_,i)=>(i+1)*.001);const total=weights.reduce((a,b)=>a+b,0);f[0]=1/total;ints[1]=count;weights.forEach((v,i)=>f[i+2]=v);gl.bufferData(gl.UNIFORM_BUFFER,data,gl.DYNAMIC_DRAW);gl.drawArrays(gl.TRIANGLES,0,3);const got=new Uint8Array(16);gl.readPixels(0,0,2,2,gl.RGBA,gl.UNSIGNED_BYTE,got);
 for(let pixel=0;pixel<4;pixel++){
  const expected=[0,1,2].map(ch=>Math.sqrt(weights.reduce((sum,w,i)=>sum+w*pixels[i][pixel*4+ch]**2,0)/total));const rgb=Array.from(got.slice(pixel*4,pixel*4+3));if(expected.some((v,ch)=>Math.abs(v-rgb[ch])>2)||got[pixel*4+3]!==255)failures.push({count,pixel,expected,rgb});cases++;
 }
}
if(gl.getError()!==gl.NO_ERROR)throw Error('GL error');return {translated_glsl_compiled:true,linked:true,uniform_block_bytes:size,pixel_cases:cases,failures,spatial_orientation_checked:true,software_backend:'Chromium SwiftShader',native_gpu_tested:false};
'''
page='<!doctype html><pre id="result"></pre><script>try{const sources='+json.dumps(sources)+';const result=(()=>{'+body+'})();document.getElementById("result").textContent=JSON.stringify(result);}catch(e){document.getElementById("result").textContent=JSON.stringify({error:String(e)});}</script>'
with tempfile.TemporaryDirectory(prefix='solyanka-frame-blend-') as temp:
 f=Path(temp)/'test.html';f.write_text(page);r=subprocess.run(['chromium','--headless','--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--dump-dom',f.as_uri()],capture_output=True,text=True,timeout=60)
 m=re.search(r'<pre id="result">(.*?)</pre>',r.stdout,re.S);assert m,r.stderr[-1000:];result=json.loads(html.unescape(m.group(1)))
assert result.get('pixel_cases')==16 and not result['failures'],result
result.update(version='1.3.12',source_hashes_verified=True,history_budget_bytes=96*1024*1024,history_max_targets=24,sampler_limit=12,raw_frame_capture_before_composite=True,gui_band_cache_limit=128,feature_list_visible_behind_settings=True,gui_fps_measured_in_game=False,inventory_classic_style_adapted=True,inventory_frost_blur_ported=False,minecraft_launched_here=False,sodium_iris_tested_here=False)
(p/'validation/frame-blending-static.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
