"""Translated GLSL ES compilation and 1px formula tests on Chromium SwiftShader, NOT native Minecraft GPU validation."""
from pathlib import Path
import json,zipfile,re,subprocess,tempfile,html,hashlib
p=Path(__file__).resolve().parents[1];jar=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
with zipfile.ZipFile(jar) as z:vertex=z.read('assets/minecraft/shaders/core/screenquad.vsh').decode()
def es(s):return re.sub(r'#version\s+330','#version 300 es\nprecision highp float;\nprecision highp int;',s)
sources=dict(vertex=es(vertex),fragments={m:es((p/f'src/main/resources/assets/solyanka/shaders/post/{m}.fsh').read_text()) for m in ['accum_mix','accum_max']})
body=r'''
const canvas=document.createElement('canvas');canvas.width=1;canvas.height=1;const gl=canvas.getContext('webgl2',{premultipliedAlpha:false});if(!gl)throw Error('WebGL2 unavailable');
const reports=[];let pixelTests=0;const failures=[];
function shader(type,source) {const sh=gl.createShader(type);gl.shaderSource(sh,source);gl.compileShader(sh);if(!gl.getShaderParameter(sh,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(sh));return sh;}
function texture(slot,values) {gl.activeTexture(gl.TEXTURE0+slot);const t=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,t);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA8,1,1,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([...values,255]));return t;}
const vao=gl.createVertexArray();gl.bindVertexArray(vao);gl.viewport(0,0,1,1);
for(const [mode,fragment] of Object.entries(sources.fragments)) {
 const prog=gl.createProgram();gl.attachShader(prog,shader(gl.VERTEX_SHADER,sources.vertex));gl.attachShader(prog,shader(gl.FRAGMENT_SHADER,fragment));gl.linkProgram(prog);if(!gl.getProgramParameter(prog,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(prog));gl.useProgram(prog);
 gl.uniform1i(gl.getUniformLocation(prog,'MainSampler'),0);gl.uniform1i(gl.getUniformLocation(prog,'PrevSampler'),1);const idx=gl.getUniformBlockIndex(prog,'AccumulationUniforms');if(idx===gl.INVALID_INDEX)throw Error('Missing accumulation block');gl.uniformBlockBinding(prog,idx,0);const ubo=gl.createBuffer();gl.bindBuffer(gl.UNIFORM_BUFFER,ubo);gl.bindBufferBase(gl.UNIFORM_BUFFER,0,ubo);
 for(const [curr,prev] of [[[32,128,240],[220,64,8]],[[0,0,0],[255,255,255]],[[150,100,50],[150,100,50]]]) for(const factor of [0,.25,.75,.96]) {
  const a=texture(0,curr),b=texture(1,prev);gl.bufferData(gl.UNIFORM_BUFFER,new Float32Array([factor,0,0,0]),gl.DYNAMIC_DRAW);gl.drawArrays(gl.TRIANGLES,0,3);const got=new Uint8Array(4);gl.readPixels(0,0,1,1,gl.RGBA,gl.UNSIGNED_BYTE,got);
  const expected=curr.map((c,i)=>mode==='accum_max'?Math.max(c,prev[i]*factor):Math.sqrt(c*c*(1-factor)+prev[i]*prev[i]*factor));if(expected.some((x,i)=>Math.abs(got[i]-x)>2)||got[3]!==255)failures.push({mode,curr,prev,factor,got:Array.from(got),expected});pixelTests++;gl.deleteTexture(a);gl.deleteTexture(b);
 }
 reports.push({mode,compiled:true,linked:true});gl.deleteBuffer(ubo);gl.deleteProgram(prog);
}
return {translation:'Desktop GLSL 330 to ES 300 with precision declarations; native screenquad vertex shader unchanged apart from ES version',software_backend:'Chromium SwiftShader',reports,pixel_tests:pixelTests,pixel_failures:failures,minecraft_pipeline_tested:false,actual_gpu_tested:false};
'''
page='<!doctype html><pre id="result"></pre><script>try{const sources='+json.dumps(sources)+';const result=(()=>{'+body+'})();document.getElementById("result").textContent=JSON.stringify(result);}catch(e){document.getElementById("result").textContent=JSON.stringify({error:String(e)});}</script>'
with tempfile.TemporaryDirectory(prefix='solyanka-accum-') as temp:
 f=Path(temp)/'test.html';f.write_text(page);r=subprocess.run(['chromium','--headless','--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--dump-dom',f.as_uri()],capture_output=True,text=True,timeout=60)
 m=re.search(r'<pre id="result">(.*?)</pre>',r.stdout,re.S);assert m,r.stderr[-1000:];result=json.loads(html.unescape(m.group(1)))
(p/'validation/accumulation-translated-glsl.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));assert result.get('pixel_tests')==24 and not result['pixel_failures']
