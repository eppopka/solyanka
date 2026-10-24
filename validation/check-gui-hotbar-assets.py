"""Offline 1.3.9 regression: synthetic pack files, reversed-Z pixels and explicit UI/download contracts.
No real asset download, Fabric launch, Mixin application, native GUI or GPU acceptance.
"""
from pathlib import Path
import subprocess,tempfile,struct,zlib,json,re,html,zipfile
p=Path(__file__).resolve().parents[1];jar=p/'build/libs/solyanka-1.3.9.jar'
def pack(rows,mutate=None):
 blobs=[zlib.compress(data)for name,typ,w,h,data in rows];size=sum(23+len(n.encode())for n,*_ in rows);offset=16+size;idx=bytearray()
 for (name,typ,w,h,data),blob in zip(rows,blobs):
  n=name.encode();idx+=struct.pack('<H',len(n))+n+struct.pack('<BHHqii',typ,w,h,offset,len(blob),len(data));offset+=len(blob)
 body=bytearray(b'SCS2PAK1'+struct.pack('<ii',len(rows),size)+idx+b''.join(blobs));return mutate(body)if mutate else body
rows=[('data.txt',0,0,0,b'hello'),('pixel.vtex_c',1,1,1,b'\xff\x00\x00\xff')]
with tempfile.TemporaryDirectory()as td:
 w=Path(td);good=pack(rows);(w/'good.pack').write_bytes(good)
 def patch(data,offset,fmt,value):
  result=bytearray(data);struct.pack_into(fmt,result,offset,value);return result
 fixtures={'magic':b'INVALID!'+good[8:],'index':patch(good,12,'<i',17000000),'offset':patch(good,16+2+8+1+2+2,'<q',1),'rawsize':patch(good,16+2+8+1+2+2+8+4,'<i',67108865),'duplicate':pack([rows[0],rows[0]]),'texture':pack([('pixel.vtex_c',1,1,1,b'\xff')]),'deflate':good[:-1]}
 # Keep the index in range but corrupt the first compressed entry; construction succeeds, read fails closed.
 first=16+struct.unpack_from('<i',good,12)[0];bad=bytearray(good);bad[first:first+2]=b'xx';fixtures['deflate']=bad
 for name,data in fixtures.items():(w/(name+'.pack')).write_bytes(data)
 subprocess.run(['javac','-cp',str(jar),'-d',td,str(p/'validation/CheckCs2Pack.java')],check=True,capture_output=True,text=True)
 result=subprocess.check_output(['java','-cp',td+':'+str(jar),'CheckCs2Pack',td],text=True,timeout=20);assert '10 synthetic pack checks passed'in result,result
 # Tiny depth test: a model occupies the middle pixel, a far particle covers all pixels.
 js=r'''const c=document.createElement('canvas');c.width=3;c.height=1;const gl=c.getContext('webgl2',{depth:true,antialias:false});if(!gl)throw Error('WebGL2');
 function shader(t,s){const x=gl.createShader(t);gl.shaderSource(x,s);gl.compileShader(x);if(!gl.getShaderParameter(x,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(x));return x;}
 const p=gl.createProgram();gl.attachShader(p,shader(gl.VERTEX_SHADER,'#version 300 es\nprecision highp float;uniform float z;void main(){vec2 v=vec2((gl_VertexID==1)?3.0:-1.0,(gl_VertexID==2)?3.0:-1.0);gl_Position=vec4(v,z,1);}'));gl.attachShader(p,shader(gl.FRAGMENT_SHADER,'#version 300 es\nprecision highp float;uniform vec4 color;out vec4 outColor;void main(){outColor=color;}'));gl.linkProgram(p);if(!gl.getProgramParameter(p,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(p));gl.useProgram(p);gl.bindVertexArray(gl.createVertexArray());gl.viewport(0,0,3,1);gl.enable(gl.DEPTH_TEST);gl.enable(gl.SCISSOR_TEST);
 function draw(z,color,x,w,write=true){gl.scissor(x,0,w,1);gl.depthMask(write);gl.uniform1f(gl.getUniformLocation(p,'z'),z);gl.uniform4fv(gl.getUniformLocation(p,'color'),color);gl.drawArrays(gl.TRIANGLES,0,3);}
 gl.scissor(0,0,3,1);gl.clearColor(0,0,0,1);gl.clearDepth(0);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);gl.depthFunc(gl.GEQUAL);draw(.4,[1,1,1,1],1,1);draw(.2,[1,0,0,1],0,3,false);const far=new Uint8Array(12);gl.readPixels(0,0,3,1,gl.RGBA,gl.UNSIGNED_BYTE,far);draw(.6,[0,0,1,1],1,1,false);const near=new Uint8Array(12);gl.readPixels(0,0,3,1,gl.RGBA,gl.UNSIGNED_BYTE,near);gl.depthFunc(gl.ALWAYS);draw(.2,[0,1,0,1],0,3,false);const through=new Uint8Array(12);gl.readPixels(0,0,3,1,gl.RGBA,gl.UNSIGNED_BYTE,through);return {far:Array.from(far),near:Array.from(near),through:Array.from(through),gl_error:gl.getError()};'''
 page=w/'depth.html';page.write_text('<!doctype html><pre id="result"></pre><script>try{document.querySelector("pre").textContent=JSON.stringify((()=>{'+js+'})());}catch(e){document.querySelector("pre").textContent=JSON.stringify({error:String(e)});}</script>')
 out=subprocess.run(['chromium','--headless','--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--dump-dom',page.as_uri()],capture_output=True,text=True,timeout=60)
 m=re.search(r'<pre id="result">(.*?)</pre>',out.stdout,re.S);assert m,out.stderr[-1200:];pixels=json.loads(html.unescape(m.group(1)))
 assert pixels.get('far')==[255,0,0,255,255,255,255,255,255,0,0,255],pixels
 assert pixels.get('near')==[255,0,0,255,0,0,255,255,255,0,0,255]and pixels['gl_error']==0,pixels
assert pixels['through']==[0,255,0,255]*3,pixels
world=(p/'src/client/kotlin/dev/solyanka/client/feature/WorldOverlay.kt').read_text();assert world.count('if(depth) CompareOp.GREATER_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS,false')==2 and 'LESS_THAN_OR_EQUAL'not in world
native=Path.home()/'.gradle/caches/fabric-loom/26.2/minecraft-client.jar'
dump=subprocess.check_output(['javap','-p','-c','-classpath',str(native),'com.mojang.blaze3d.pipeline.DepthStencilState'],text=True);assert 'CompareOp.GREATER_THAN_OR_EQUAL' in dump
loader=(p/'src/client/java/dev/solyanka/client/cs2/Cs2Knives.java').read_text();down=(p/'src/client/java/dev/solyanka/client/cs2/Cs2AssetDownload.java').read_text()
assert 'Cs2AssetDownload.start()'not in loader and 'openSource(mode,path)'in loader
for token in ['Redirect.NEVER','done>SIZE','done!=SIZE','SHA256.equals','Files.deleteIfExists(temp)','LinkOption.NOFOLLOW_LINKS','setDaemon(true)','revision.incrementAndGet()']:assert token in down,token
assert down.index('!SHA256.equals')<down.index('Files.move(temp,target')
aura=(p/'src/client/kotlin/dev/solyanka/client/feature/ItemAura.kt').read_text();assert 'CompareOp.GREATER_THAN_OR_EQUAL' in aura and 'LESS_THAN_OR_EQUAL'not in aura
with zipfile.ZipFile(jar)as z:
 assert not any(n.endswith(('.pack','.vpk','.vtex_c','.vmdl_c'))for n in z.namelist())
 for name in ['dev/solyanka/client/cs2/pack/Cs2Pack.class','dev/solyanka/client/cs2/Cs2AssetDownload.class','dev/solyanka/client/feature/VanillaHotbar.class']:assert name in z.namelist()
result=dict(version='1.3.9',synthetic_pack_checks=10,reversed_z_native_default_verified=True,halo_world_depth_fixed=True,through_walls_bypass_tested=True,depth_pixels=pixels,download_explicit_only=True,download_pinned_size_and_hash=True,download_cancellation=True,assets_bundled=False,actual_asset_download_tested=False,native_gui_visual_tested=False,in_game_tested=False,actual_gpu_tested=False)
(p/'validation/gui-hotbar-assets.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
