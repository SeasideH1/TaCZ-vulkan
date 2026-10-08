#!/usr/bin/env python3
"""Make a compile-only game JAR with Fabric's exact published transitive API tweaks.
No custom wideners, game launch/authentication patches, or Gradle execution.
"""
import argparse,hashlib,json,os,pathlib,subprocess,zipfile
p=argparse.ArgumentParser();p.add_argument('--java-home',type=pathlib.Path);a=p.parse_args()
r=pathlib.Path(__file__).resolve().parents[1]
mc=r/'official-minecraft';input=mc/'client-26.4-snapshot-3.jar';manifest=json.loads((mc/'26.4-snapshot-3.json').read_text())
assert hashlib.sha1(input.read_bytes()).hexdigest()==manifest['downloads']['client']['sha1']
out=mc/'compile-api';out.mkdir(exist_ok=True);rules=out/'published-rules';rules.mkdir(exist_ok=True)
entries=[]
for jar in sorted((r/'official-fabric/compile-nested').glob('*.jar')):
 with zipfile.ZipFile(jar) as z:
  if 'fabric.mod.json' not in z.namelist():continue
  d=json.loads(z.read('fabric.mod.json'));entry=d.get('accessWidener')
  if not entry:continue
  data=z.read(entry);p=rules/(jar.stem+'.rules');p.write_bytes(data)
  entries.append({'jar':str(jar),'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'entry':entry,'rules':str(p),'sha256':hashlib.sha256(data).hexdigest()})
verification=json.loads((r/'logs/class-tweaker-verification.json').read_text());tweaker=pathlib.Path(verification['jar']);assert hashlib.sha256(tweaker.read_bytes()).hexdigest()==verification['sha256']
asm=sorted((r/'official-fabric/libraries/org/ow2/asm').rglob('*.jar'));cp=os.pathsep.join([str(tweaker)]+[str(x) for x in asm]);classes=r/'toolchains/compile-api/classes';classes.mkdir(exist_ok=True);java=(a.java_home or r/'toolchains/jdk-25.0.4.1+1')/'bin'
subprocess.run([str(java/'javac'),'-proc:none','--release','25','-cp',cp,'-d',str(classes),str(r/'toolchains/compile-api/ApplyPublishedFabricTweaks.java')],check=True)
output=out/'fabric-transitive-client-26.4-snapshot-3.jar'
subprocess.run([str(java/'java'),'-Xmx2G','-cp',str(classes)+os.pathsep+cp,'ApplyPublishedFabricTweaks',str(input),str(output)]+[x['rules'] for x in entries],check=True)
provenance={'purpose':'compile-only exact published transitive Fabric API tweaks; never runtime','source_client':str(input),'source_sha1':manifest['downloads']['client']['sha1'],'fabric_api':'0.162.2+26.4','transformer':verification,'rules':entries,'output':str(output),'output_sha256':hashlib.sha256(output.read_bytes()).hexdigest()}
(out/'provenance.json').write_text(json.dumps(provenance,indent=2)+'\n');print('COMPILE_API_READY',output)
