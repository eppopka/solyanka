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
  expanded=expand((folder/f'cs2_knife.{ext}').read_text(),set());expanded=re.sub(r'^\s*#version[^\n]*','',expanded,flags=re.M)
  expanded=expanded.replace('(uv / 256.0)','(vec2(uv) / 256.0)') # Desktop implicit conversion in vanilla light.glsl is not valid in ES.
  sources[ext]='#version 300 es\nprecision highp float;\nprecision highp int;\n#define ALPHA_CUTOUT 0.1\n#define PORTAL_LAYERS 15\n'+expanded
body=r'''const gl=document.createElement('canvas').getContext('webgl2');if(!gl) throw new Error('WebGL2 unavailable');
let shaders=[];let reports=[];for(const [name,type] of [['vsh',gl.VERTEX_SHADER],['fsh',gl.FRAGMENT_SHADER]]) {const s=gl.createShader(type);gl.shaderSource(s,sources[name]);gl.compileShader(s);reports.push({name,compiled:gl.getShaderParameter(s,gl.COMPILE_STATUS),log:gl.getShaderInfoLog(s)});shaders.push(s);}
const program=gl.createProgram();shaders.forEach(s=>gl.attachShader(program,s));gl.linkProgram(program);return {translation:'GLSL 330 imports expanded; ES 300 precision and explicit vec2 cast in vanilla light.glsl added for offline WebGL2 check',reports,linked:gl.getProgramParameter(program,gl.LINK_STATUS),log:gl.getProgramInfoLog(program),minecraft_pipeline_tested:false,actual_gpu_tested:false};'''
page='<!doctype html><html><body><pre id="result"></pre><script>try {const sources='+json.dumps(sources)+';const result=(()=>{'+body+'})();document.getElementById("result").textContent=JSON.stringify(result);}catch(e){document.getElementById("result").textContent=JSON.stringify({error:String(e)});}</script></body></html>'
with tempfile.TemporaryDirectory(prefix='solyanka-glsl-') as temp:
 f=Path(temp)/'check.html';f.write_text(page)
 r=subprocess.run(['chromium','--headless','--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--dump-dom',f.as_uri()],capture_output=True,text=True,timeout=60)
 m=re.search(r'<pre id="result">(.*?)</pre>',r.stdout,re.S);assert m,r.stderr[-1500:]
 result=json.loads(html.unescape(m.group(1)))
(p/'validation/cs2-translated-glsl.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));assert result.get('linked') and all(x['compiled'] for x in result['reports'])
