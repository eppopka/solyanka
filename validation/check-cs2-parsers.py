"""Exercise the compiled source parsers on generated fixtures; no CS2 assets."""
from pathlib import Path
import subprocess,tempfile,json
p=Path(__file__).resolve().parents[1];jar=p/'build/libs/solyanka-1.3.9.jar'
with tempfile.TemporaryDirectory()as temp:
 w=Path(temp);plain=(b'solyanka source parser fixture\n'*500)+bytes(range(256));(w/'plain').write_bytes(plain)
 subprocess.run(['zstd','-q','-f',str(w/'plain'),'-o',str(w/'plain.zst')],check=True)
 subprocess.run(['javac','-cp',str(jar),'-d',temp,str(p/'validation/CheckCs2Parsers.java')],check=True)
 out=subprocess.check_output(['java','-cp',temp+':'+str(jar),'CheckCs2Parsers',str(w/'fixture_dir.vpk'),str(w/'plain.zst'),str(w/'plain')],text=True)
 print(out);assert '8 synthetic parser checks passed' in out
 (p/'validation/cs2-parsers.json').write_text(json.dumps(dict(synthetic_checks=8,real_cs2_assets_tested=False,network_used=False),indent=2)+'\n')
