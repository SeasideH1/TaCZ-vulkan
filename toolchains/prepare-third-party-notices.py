#!/usr/bin/env python3
"""Copy exact publisher notice bytes and source-origin metadata into the mod.
Reads already-reviewed local artifacts; no network or code execution.
"""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1];project=ROOT/'forge-release-port';out=project/'src/main/resources/META-INF/licenses';out.mkdir(parents=True,exist_ok=True)
licenses=ROOT/'reproduction/license-evidence';inventory=json.loads((licenses/'dependency-license-inventory.json').read_text());sources=json.loads((ROOT/'reproduction/dependency-sources/provenance.json').read_text());assert all('error'not in x for x in sources)
copied=[]
def copy(path,name,origin):
 data=path.read_bytes();target=out/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);copied.append({'file':name,'sha256':hashlib.sha256(data).hexdigest(),'evidence':origin})
for item in inventory:
 for notice in item['embedded_notices']:
  copy(ROOT/notice['saved_path'],item['coordinate'].replace(':','__')+'/'+Path(notice['jar_entry']).name,{'coordinate':item['coordinate'],'artifact_sha256':item['artifact_sha256'],'jar_entry':notice['jar_entry']})
for item in json.loads((licenses/'upstream/provenance.json').read_text()):
 assert 'error'not in item
 copy(ROOT/item['path'],item['name'],{'url':item['url'],'retrieved_file_sha256':item['sha256']})
copy(project/'LICENSE','TACZ-GPL-3.0.txt',{'repository':'https://github.com/MCModderAnchor/TACZ','commit':'b482eff8c94a733ac8d0910193fca3893954027c','path':'LICENSE'})
sbm=ROOT/'reproduction/source-reference/simplebedrockmodel-2.2.2-forge+mc1.20.1-sources.jar';sbm_sha=hashlib.sha256(sbm.read_bytes()).hexdigest()
mappings={
 'FirstPersonAnimation.java':'com/github/mcmodderanchor/simplebedrockmodel/v1/client/animation/IFPAnimationInstance.java',
 'FirstPersonItemRenderer.java':'com/github/mcmodderanchor/simplebedrockmodel/v1/client/renderer/IFPGeoItemRenderer.java',
 'FirstPersonRenderHandler.java':'com/github/mcmodderanchor/simplebedrockmodel/v1/client/handler/FirstPersonRenderHandler.java'}
origins=[]
with zipfile.ZipFile(sbm)as z:
 for destination,original in mappings.items():
  data=z.read(original);target=project/'upstream-reference/simplebedrockmodel-2.2.2'/original;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
  source=project/'src/main/java/com/tacz/guns/client/renderer/nativeapi'/destination
  header='/*\n * Adapted from SimpleBedrockModel 2.2.2 ('+Path(original).name+').\n * Original authors: TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh.\n * LGPL-3.0; see META-INF/licenses/simplebedrockmodel-LGPL.txt and\n * THIRD_PARTY_LICENSES.md for exact source provenance and porting scope.\n */\n'
  text=source.read_text()
  if not text.startswith('/*\n * Adapted from SimpleBedrockModel 2.2.2'):
   source.write_text(header+text)
  origins.append({'active_source':str(source.relative_to(project)),'original_entry':original,'original_source_sha256':hashlib.sha256(data).hexdigest(),'original_sources_jar_sha256':sbm_sha,'original_sources_repository_path':'libs/'+sbm.name,'original_repository_commit':'b482eff8c94a733ac8d0910193fca3893954027c','license':'LGPL-3.0 (published2.2.2 mod metadata)','changes':'Renamed contracts; native render queue and Fabric/26.4 API/event substitutions; original handler state transitions retained.'})
notice={'runtime_dependencies':[],'compile_only_dependencies':[],'adapted_source_origins':origins,'license_files':copied,'asset_notice':{'origin':'official TACZ1.1.8-hotfix2','license_as_declared_upstream':'CC-BY-NC-ND-4.0','terms_url':'https://creativecommons.org/licenses/by-nc-nd/4.0/','scope':'Original models, textures, animations, sounds and gunpack assets retain their authors and terms. No relicensing is claimed. Program/shader code notices are separate.'}}
source_by_coord={x['coordinate']:x for x in sources}
for item in inventory:
 d={k:item[k]for k in ['coordinate','artifact_url','artifact_sha256','pom_url','pom_sha256','licenses']}
 if item['coordinate']in source_by_coord:d['source_artifact']={k:v for k,v in source_by_coord[item['coordinate']].items()if k!='path'};notice['runtime_dependencies'].append(d)
 else:notice['compile_only_dependencies'].append(d)
manifest=project/'src/main/resources/META-INF/third-party-provenance.json';manifest.write_text(json.dumps(notice,indent=2)+'\n')
lines=['# Third-party code and asset notices','', 'TACZ core is derived from official MCModderAnchor/TACZ1.1.8-hotfix2, commit b482eff8c94a733ac8d0910193fca3893954027c. Its code and original assets have distinct upstream terms. This file records attribution and provenance; it does not relicense third-party work.','', '## Bundled pure-Java libraries','']
for x in notice['runtime_dependencies']:
 names=', '.join(y['name']for y in x['licenses'])or'Apache-2.0 (embedded license)'if x['coordinate'].startswith('org.apache.')else', '.join(y['name']for y in x['licenses'])or'MIT (upstream PlayerAnimator license)'
 lines += ['- '+x['coordinate']+' — '+names, '  Artifact: '+x['artifact_url'], '  SHA256: '+x['artifact_sha256'], '  Corresponding source: '+x['source_artifact']['url']+' (SHA256 '+x['source_artifact']['sha256']+')']
lines += ['', 'All original library entries are preserved in their nested JARs. Exact upstream license text/notice bytes are also under META-INF/licenses. Publisher POM and source-artifact evidence is recorded in META-INF/third-party-provenance.json; the source delivery includes the exact corresponding source JARs.','', '## Adapted SimpleBedrockModel source','', 'The following files adapt the LGPL-3.0 SimpleBedrockModel2.2.2 source shipped with the pinned official TACZ release. Its published binary metadata names the license and original authors: TartaricAcid, MaydayMemory, MoePus, Hidomatn and xjqsh. Original source entries are retained in upstream-reference/simplebedrockmodel-2.2.2 in the source delivery.']
for x in origins:lines.append('- '+x['active_source']+' ← '+x['original_entry'])
lines += ['', '## Original TACZ assets','', 'Original models, textures, animations, sounds and gunpack resources remain attributed to their original authors under the upstream CC BY-NC-ND4.0 declaration: https://creativecommons.org/licenses/by-nc-nd/4.0/ . The default-pack source bytes are preserved against the fixed release. Native shader/adapter program code is separate from those art assets.','', '## Build/runtime platform','', 'Minecraft, Fabric, Java and Gradle are external platform/build inputs, not embedded game/runtime distributions. Their exact versions, official URLs and hashes are in reproduction/build-inputs.lock.json. Minecraft binaries, mappings, assets downloaded from Mojang, JDK/Gradle archives, original Forge comparator binary and cached third-party integration binaries are not included in the source archive.','', 'Unsupported integration source modules remain under optional-integrations with their original attribution and explicit status. Their presence is not a runtime support claim.','']
text='\n'.join(lines);(project/'THIRD_PARTY_LICENSES.md').write_text(text);(project/'src/main/resources/META-INF/THIRD_PARTY_LICENSES.md').write_text(text)
print('Prepared',len(copied),'exact notice files and',len(origins),'adapted-source records; no runtime binary built.')
