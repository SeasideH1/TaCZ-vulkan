#!/usr/bin/env python3
"""Recreate exact official build inputs without redistributing game/toolchain binaries.
Default verifies local inputs and regenerates local metadata. --fetch permits HTTPS downloads; --install-jdk
extracts the checksum-verified official JDK using Python's safe data filter.
Never launches Minecraft, changes authentication, or runs Gradle/Loom.
"""
import argparse,concurrent.futures,hashlib,json,os,shutil,tarfile,urllib.request,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--fetch',action='store_true');p.add_argument('--reuse-from',type=Path);p.add_argument('--install-jdk',action='store_true');p.add_argument('--with-gradle',action='store_true');p.add_argument('--with-server',action='store_true');p.add_argument('--assets',action='store_true');p.add_argument('--local-jdk',action='store_true',help='Skip the Linux JDK download when supplying a local compiler.');a=p.parse_args()
if a.local_jdk and a.install_jdk:p.error('--local-jdk and --install-jdk are mutually exclusive')
lock=json.loads((ROOT/'reproduction/build-inputs.lock.json').read_text())
def canonical(path):
 # Windows may return an extended-length prefix for some resolved paths only.
 # Normalize that spelling after resolving links, before the containment check.
 value=str(path.resolve())
 if os.name=='nt':
  if value.startswith('\\\\?\\UNC\\'):value='\\\\'+value[8:]
  elif value.startswith('\\\\?\\'):value=value[4:]
 return Path(value)

def safe(relative):
 target=canonical(ROOT/relative)
 if not target.is_relative_to(canonical(ROOT)):raise ValueError('Input path escapes workspace: '+relative)
 return target
rows=[x for x in lock['artifacts']if (x['category']!='optional-gradle-archive'or a.with_gradle)and(x['category']!='optional-server'or a.with_server)and(x['category']!='jdk-archive'or not a.local_jdk)]
def valid(path,row):return path.is_file()and path.stat().st_size==row['size']and hashlib.sha256(path.read_bytes()).hexdigest()==row['sha256']
def acquire(row):
 target=safe(row['path'])
 if target.exists():
  if not valid(target,row):raise ValueError('Existing input differs from locked hash; preserve/review it before retry: '+str(target))
  return
 target.parent.mkdir(parents=True,exist_ok=True)
 if a.reuse_from:
  old=a.reuse_from/row['path']
  if valid(old,row):shutil.copyfile(old,target);return
 if not a.fetch:raise FileNotFoundError('Missing locked input; rerun with --fetch or --reuse-from: '+row['path'])
 if not row['url'].startswith('https://'):raise ValueError('Non-HTTPS artifact URL')
 # A denied/error response stops this artifact; no mirrors or TLS weakening.
 with urllib.request.urlopen(urllib.request.Request(row['url'],headers={'User-Agent':'TACZ-private-repro/1.0'}),timeout=120)as response:data=response.read()
 if len(data)!=row['size']or hashlib.sha256(data).hexdigest()!=row['sha256']:raise ValueError('Locked artifact checksum mismatch: '+row['path'])
 temporary=target.with_suffix(target.suffix+'.part');temporary.write_bytes(data);temporary.replace(target)
with concurrent.futures.ThreadPoolExecutor(max_workers=6)as pool:list(pool.map(acquire,rows))
if a.install_jdk:
 target=ROOT/'toolchains/jdk-25.0.4.1+1'
 if not target.exists():
  with tarfile.open(ROOT/'downloads/jdk25.tar.gz')as archive:archive.extractall(ROOT/'toolchains',filter='data')
 if not(target/'bin/javac').is_file():raise ValueError('Official JDK archive did not produce expected compiler path')
mc=ROOT/'official-minecraft';fabric=ROOT/'official-fabric';java=ROOT/'official-java-libraries'
(mc/'classpath.txt').write_text(os.pathsep.join(str(safe(x))for x in lock['classpath_order']['minecraft'])+'\n')
profile=json.loads((ROOT/'reproduction/fabric-loader-profile.json').read_text());(fabric/'loader-profile.json').write_text(json.dumps(profile,indent=2)+'\n')
paths=[str(safe(x['path']))for x in rows if x['category']in('fabric-runtime','fabric-api')]
# These are compiler-only expansions. Runtime always uses intact publisher JARs.
for jar in list(paths):
 with zipfile.ZipFile(jar)as z:
  for name in z.namelist():
   if name.startswith('META-INF/jars/')and name.endswith('.jar'):
    dest=fabric/'compile-nested'/Path(name).name;dest.parent.mkdir(exist_ok=True);dest.write_bytes(z.read(name));paths.append(str(dest))
(fabric/'classpath.txt').write_text(os.pathsep.join(paths)+'\n');(fabric/'libraries-verified.json').write_text(json.dumps(paths,indent=2)+'\n')
records=[]
for item in lock['java_verified_records']:
 d=dict(item);d['path']=str(safe(d['path']));records.append(d)
(java/'verified.json').write_text(json.dumps(records,indent=2)+'\n');(java/'classpath.txt').write_text(os.pathsep.join(d['path']for d in records)+'\n')
ct=next(x for x in rows if x.get('coordinate')=='net.fabricmc:class-tweaker:0.3.0');logs=ROOT/'logs';logs.mkdir(exist_ok=True)
(logs/'class-tweaker-verification.json').write_text(json.dumps({'jar':str(safe(ct['path'])),'sha256':ct['sha256'],'publisher_checksum_url':ct['url']+'.sha256'},indent=2)+'\n')
if a.assets:
 assets=mc/'assets';index=json.loads((assets/'indexes/36.json').read_text())['objects'];unique={v['hash']:v['size']for v in index.values()}
 def asset(item):
  digest,size=item;target=assets/'objects'/digest[:2]/digest
  if target.exists():
   if target.stat().st_size!=size or hashlib.sha1(target.read_bytes()).hexdigest()!=digest:raise ValueError('Existing asset hash differs: '+digest)
   return
  if a.reuse_from:
   old=a.reuse_from/'official-minecraft/assets/objects'/digest[:2]/digest
   if old.is_file()and old.stat().st_size==size and hashlib.sha1(old.read_bytes()).hexdigest()==digest:target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(old,target);return
  if not a.fetch:raise FileNotFoundError('Missing asset '+digest)
  with urllib.request.urlopen('https://resources.download.minecraft.net/'+digest[:2]+'/'+digest,timeout=90)as response:data=response.read()
  if len(data)!=size or hashlib.sha1(data).hexdigest()!=digest:raise ValueError('Asset hash mismatch '+digest)
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
 with concurrent.futures.ThreadPoolExecutor(max_workers=8)as pool:list(pool.map(asset,unique.items()))
 (assets/'VERIFIED.json').write_text(json.dumps({'index':'36','objects':len(unique),'bytes':sum(unique.values())},indent=2)+'\n')
print('Verified',len(rows),'locked inputs; classpaths/provenance regenerated for',ROOT)
print('Next: python3 toolchains/prepare-fabric-compile-api.py; python3 toolchains/direct-build.py forge-release-port; python3 toolchains/package-runtime-candidate.py')
