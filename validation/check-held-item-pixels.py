"""Optional translated GLSL ES check via headless Chromium/SwiftShader, not native Minecraft validation."""
from pathlib import Path
import zipfile,re,json,subprocess,tempfile,html
p=Path(__file__).resolve().parents[1];jar=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar';folder=p/'src/main/resources/assets/solyanka/shaders/core'
with zipfile.ZipFile(jar) as z:
 def expand(text,seen):
  def include(m):
   name=m.group(1);namespace,path=name.split(':',1) if ':' in name else ('minecraft',name);key=f'assets/{namespace}/shaders/include/{path}'
   if key in seen:return ''
   seen.add(key);return expand(z.read(key).decode(),seen)
  return re.sub(r'#moj_import\s*<([^>]+)>',include,text)
 sources={}
 for ext in ['vsh','fsh']:
  expanded=expand((folder/f'held_item.{ext}').read_text(),set());expanded=re.sub(r'^\s*#version[^\n]*','',expanded,flags=re.M)
  expanded=expanded.replace('(uv / 256.0)','(vec2(uv) / 256.0)') # Desktop implicit conversion in vanilla light.glsl is not valid in ES.
  sources[ext]='#version 300 es\nprecision highp float;\nprecision highp int;\n#define ALPHA_CUTOUT 0.1\n#define PORTAL_LAYERS 15\n'+expanded
body=r'''const canvas=document.createElement('canvas');canvas.width=32;canvas.height=32;const gl=canvas.getContext('webgl2',{premultipliedAlpha:false,alpha:true});if(!gl)throw Error('WebGL2 unavailable');
function shader(t,s){const sh=gl.createShader(t);gl.shaderSource(sh,s);gl.compileShader(sh);if(!gl.getShaderParameter(sh,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(sh));return sh;}
const prog=gl.createProgram();gl.attachShader(prog,shader(gl.VERTEX_SHADER,sources.vsh));gl.attachShader(prog,shader(gl.FRAGMENT_SHADER,sources.fsh));gl.linkProgram(prog);if(!gl.getProgramParameter(prog,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(prog));gl.useProgram(prog);gl.bindVertexArray(gl.createVertexArray());gl.viewport(0,0,32,32);gl.disable(gl.BLEND);gl.disable(gl.CULL_FACE);
function attrib(name,size,values){const loc=gl.getAttribLocation(prog,name);if(loc<0)return;const b=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,b);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(values),gl.STATIC_DRAW);gl.enableVertexAttribArray(loc);gl.vertexAttribPointer(loc,size,gl.FLOAT,false,0,0);}
const xy=[[-1,-1],[1,-1],[1,1],[-1,-1],[1,1],[-1,1]];attrib('Position',3,xy.flatMap(([x,y])=>[x,y,0]));attrib('Color',4,xy.flatMap(()=>[1,1,1,1]));attrib('UV0',2,xy.flatMap(([x,y])=>[(x+1)/2,(y+1)/2]));
function uniformBlock(name,binding,setup){const idx=gl.getUniformBlockIndex(prog,name);if(idx===gl.INVALID_INDEX)return null;const size=gl.getActiveUniformBlockParameter(prog,idx,gl.UNIFORM_BLOCK_DATA_SIZE),data=new Float32Array(size/4);setup(data);const b=gl.createBuffer();gl.bindBuffer(gl.UNIFORM_BUFFER,b);gl.bufferData(gl.UNIFORM_BUFFER,data,gl.DYNAMIC_DRAW);gl.bindBufferBase(gl.UNIFORM_BUFFER,binding,b);gl.uniformBlockBinding(prog,idx,binding);return {b,data};}
function identity(d,offset){for(let i=0;i<4;i++)d[offset+i*5]=1;}
uniformBlock('DynamicTransforms',0,d=>{identity(d,0);d.set([1,1,1,1],16);identity(d,24);});uniformBlock('Projection',1,d=>identity(d,0));const globals=uniformBlock('Globals',2,d=>{d[8]=32;d[9]=32;});
function texture(name,slot,w,h,data){gl.activeTexture(gl.TEXTURE0+slot);const t=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,t);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.REPEAT);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.REPEAT);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA8,w,h,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array(data));gl.uniform1i(gl.getUniformLocation(prog,name),slot);return t;}
const atlas=texture('Sampler0',0,1,1,[180,140,100,255]);texture('Sampler1',1,1,1,[80,80,80,255]);const portal=[];for(let y=0;y<16;y++)for(let x=0;x<16;x++)portal.push(x*16,y*16,(x*29+y*37)%256,255);texture('Sampler3',3,16,16,portal);texture('Sampler4',4,16,16,portal);const config=texture('Sampler5',5,3,1,[79,214,234,191,0,200,64,255,0,0,0,0]);
function draw(style,speed,time,alpha=255,worldTime=time){const clock=new Uint8Array(new Float32Array([time*speed/255*4]).buffer);gl.activeTexture(gl.TEXTURE0);gl.bindTexture(gl.TEXTURE_2D,atlas);gl.texSubImage2D(gl.TEXTURE_2D,0,0,0,1,1,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([180,140,100,alpha]));gl.activeTexture(gl.TEXTURE5);gl.bindTexture(gl.TEXTURE_2D,config);gl.texSubImage2D(gl.TEXTURE_2D,0,0,0,3,1,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([79,214,234,191,style,200,speed,255,...clock]));globals.data[11]=worldTime;gl.bindBuffer(gl.UNIFORM_BUFFER,globals.b);gl.bufferSubData(gl.UNIFORM_BUFFER,0,globals.data);gl.clearColor(0,0,0,0);gl.clear(gl.COLOR_BUFFER_BIT);gl.drawArrays(gl.TRIANGLES,0,6);const out=new Uint8Array(32*32*4);gl.readPixels(0,0,32,32,gl.RGBA,gl.UNSIGNED_BYTE,out);return out;}
const cases=[],failures=[];for(let style=0;style<4;style++){
 const a=draw(style,64,0),b=draw(style,64,.1),frozenA=draw(style,0,0),frozenB=draw(style,0,.1),cutout=draw(style,64,0,0),resetWorld=draw(style,64,.1,255,0);let changed=0,animated=0,frozenDiff=0,discarded=0,worldResetDiff=0;
 for(let i=0;i<a.length;i+=4){if(a[i]!==180||a[i+1]!==140||a[i+2]!==100)changed++;if([0,1,2].some(c=>a[i+c]!==b[i+c]))animated++;if([0,1,2,3].some(c=>frozenA[i+c]!==frozenB[i+c]))frozenDiff++;if(cutout[i+3]===0)discarded++;if([0,1,2,3].some(c=>b[i+c]!==resetWorld[i+c]))worldResetDiff++;}
 const result={style,world_reset_pixel_differences:worldResetDiff,changed_pixels:changed,animated_pixels:animated,frozen_pixel_differences:frozenDiff,cutout_discarded_pixels:discarded};cases.push(result);if(changed===0||animated===0||frozenDiff!==0||worldResetDiff!==0||discarded!==1024)failures.push(result);
}
const err=gl.getError();if(err!==gl.NO_ERROR)throw Error('GL error '+err);return {translation:'GLSL 330 native includes expanded and translated to ES 300 for software testing',linked:true,native_vertex_shader_used:true,style_cases:cases,pixel_render_cases:24,failures,minecraft_pipeline_tested:false,actual_gpu_tested:false,backend:'Chromium SwiftShader'};'''
page='<!doctype html><html><body><pre id="result"></pre><script>try {const sources='+json.dumps(sources)+';const result=(()=>{'+body+'})();document.getElementById("result").textContent=JSON.stringify(result);}catch(e){document.getElementById("result").textContent=JSON.stringify({error:String(e)});}</script></body></html>'
with tempfile.TemporaryDirectory(prefix='solyanka-glsl-') as temp:
 f=Path(temp)/'check.html';f.write_text(page)
 r=subprocess.run(['chromium','--headless','--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--dump-dom',f.as_uri()],capture_output=True,text=True,timeout=60)
 m=re.search(r'<pre id="result">(.*?)</pre>',r.stdout,re.S);assert m,r.stderr[-1500:]
 result=json.loads(html.unescape(m.group(1)))
(p/'validation/held-item-pixels.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));assert result.get('linked') and len(result['style_cases'])==4 and not result['failures']
