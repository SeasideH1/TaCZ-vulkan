#!/usr/bin/env python3
"""Validate prepared attachment host fixtures against actual production item APIs."""
import datetime as dt
import hashlib
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
SHARED=ROOT.parent
JAVA=SHARED/'toolchains/jdk-25.0.4.1+1/bin'
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    stamp=dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    out=ROOT/'build/attachment-fixture-checks'/stamp
    classes=out/'classes';classes.mkdir(parents=True)
    manifest=ROOT/'build/direct/runtime-candidate-latest.json';candidate=json.loads(manifest.read_text())
    assert sha(Path(candidate['jar']))==candidate['sha256']
    cp=[candidate['jar']]
    for folder in ('official-minecraft','official-java-libraries','official-fabric'):
        cp+=(SHARED/folder/'classpath.txt').read_text().strip().split(':')
    sources=[ROOT/'src/test/java/com/tacz/guns/crafting'/name for name in ('AllRecipeCraftingChecks.java','AttachmentHostFixtureChecks.java')]
    plan=SHARED/'validation/tests/sequential-fixtures/stage-plan.json'
    inputs=sources+[plan]+list((ROOT/'src/main/resources/assets/tacz/custom/tacz_default_gun/data').rglob('*.json'))
    inputs += [ROOT/'src/main/resources/data/tacz/painting_variant/blood_strike_1.json',ROOT/'src/main/resources/data/tacz/recipes/misc/blood_strike_1.json']
    before={str(p):sha(p) for p in inputs}
    with (out/'compile.log').open('w') as log:
        compile_result=subprocess.run([str(JAVA/'javac'),'-proc:none','--release','25','-cp',':'.join(cp),'-d',str(classes),*map(str,sources)],stdout=log,stderr=subprocess.STDOUT)
    test_exit=None
    if compile_result.returncode==0:
        with (out/'result.log').open('w') as log:
            test_exit=subprocess.run([str(JAVA/'java'),'-cp',str(classes)+':'+':'.join(cp),'com.tacz.guns.crafting.AttachmentHostFixtureChecks',str(plan),str(out/'host-results.json')],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=180).returncode
    after={str(p):sha(p) for p in inputs}
    status='PASS' if compile_result.returncode==test_exit==0 and before==after else 'FAIL'
    report={'status':status,'kind':'ACTUAL_ATTACHMENT_HOST_FIXTURE_VALIDATION','timestamp':stamp,'candidate':candidate,'compile_exit':compile_result.returncode,'test_exit':test_exit,'source_and_input_hashes_before':before,'source_and_input_hashes_after':after,'dependencies_sha256':{p:sha(Path(p)) for p in cp},'stable':before==after,'host_results':str(out/'host-results.json'),'bootstrap':'Explicit dedicated-test-process pre-freeze guard; real native registries/components/tags and unmodified production item APIs','actual_gui_packets_player_conservation':'NOT_RUN','source_changes':'No production source or fixture changes'}
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    (ROOT/'build/attachment-fixture-checks/latest.json').write_text(json.dumps(report,indent=2)+'\n')
    print(status,out/'report.json')
    print((out/'result.log').read_text() if (out/'result.log').exists() else (out/'compile.log').read_text())
    return 0 if status=='PASS' else 1

if __name__=='__main__': raise SystemExit(main())
