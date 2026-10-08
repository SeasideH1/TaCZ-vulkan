#!/usr/bin/env python3
"""Exercise exact refit mutation methods with native containers, without simulated players."""
import datetime as dt
import hashlib
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
SHARED=ROOT.parent
JAVA=SHARED/'toolchains/jdk-25.0.4.1+1/bin'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    stamp=dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'-baseline'
    out=ROOT/'build/attachment-transactions/runs'/stamp;classes=out/'classes';classes.mkdir(parents=True)
    candidate=json.loads((ROOT/'build/direct/runtime-candidate-latest.json').read_text());assert sha(Path(candidate['jar']))==candidate['sha256']
    cp=[candidate['jar']]
    for folder in ('official-minecraft','official-java-libraries','official-fabric'):cp+=(SHARED/folder/'classpath.txt').read_text().strip().split(':')
    production=[ROOT/('src/main/java/com/tacz/guns/'+path+'.java') for path in ('fabric/inventory/AttachmentRefit','api/item/gun/AbstractGunItem','network/message/ClientMessageRefitGun','network/message/ClientMessageUnloadAttachment')]
    tests=[ROOT/'src/test/java/com/tacz/guns/crafting'/file for file in ('AllRecipeCraftingChecks.java','AttachmentTransactionChecks.java')]
    sources=production+tests
    plan=SHARED/'validation/tests/sequential-fixtures/stage-plan.json'
    inputs=list(dict.fromkeys(production+sources))+[plan]+list((ROOT/'src/main/resources/assets/tacz/custom/tacz_default_gun/data').rglob('*.json'))
    inputs += [ROOT/'src/main/resources/data/tacz/painting_variant/blood_strike_1.json',ROOT/'src/main/resources/data/tacz/recipes/misc/blood_strike_1.json']
    before={str(p):sha(p) for p in inputs}
    with (out/'compile.log').open('w') as log:compile_result=subprocess.run([str(JAVA/'javac'),'-proc:none','--release','25','-cp',':'.join(cp),'-d',str(classes),*map(str,sources)],stdout=log,stderr=subprocess.STDOUT)
    test_exit=None
    if compile_result.returncode==0:
        with (out/'result.log').open('w') as log:test_exit=subprocess.run([str(JAVA/'java'),'-cp',str(classes)+':'+':'.join(cp),'com.tacz.guns.crafting.AttachmentTransactionChecks',str(plan),str(out/'transaction-results.json')],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=180).returncode
    results=json.loads((out/'transaction-results.json').read_text()) if (out/'transaction-results.json').exists() else None
    after={str(p):sha(p) for p in inputs};status='PASS' if compile_result.returncode==test_exit==0 and before==after else 'FAIL'
    if status=='PASS' and results['magazine_count_sweep_failures']:status='FIXTURE_PASS_BASELINE_AMMO_LOSS_CONFIRMED'
    report={'status':status,'kind':'ACTUAL_REFIT_CORE_NATIVE_TRANSACTION_CHECKS','timestamp':stamp,'candidate_dependency_closure':candidate,'compile_exit':compile_result.returncode,'test_exit':test_exit,'results':results,'source_and_input_hashes_before':before,'source_and_input_hashes_after':after,'dependencies_sha256':{p:sha(Path(p)) for p in cp},'stable':before==after,'method_coverage':['AttachmentRefit.install','AttachmentRefit.unload','GunItemDataAccessor.installAttachment/unloadAttachment','GunAttachments.get/set','AbstractGunItem.dropAllAmmo(ItemStack,boolean,Consumer)','AttachmentDataUtils.getAmmoCountWithAttachment','native SimpleContainer.addItem and ItemStack NBT/stream codecs'],'boundaries_not_executed':['ServerPlayer inventory.add and world drop','AttachmentPropertyManager player cache/script/event effects','Fabric packet receipt, menu/client refresh','real GUI install/remove/visuals'],'bootstrap':'Dedicated-test-process guard; actual native registries/components/tag loader/index data','production_arithmetic':'UNCHANGED_USER_CHOSE_ORIGINAL_DEFECT','accepted_inherited_limitations':'Six explicit magazine-return losses remain conservation failures; test asserts exact original behavior'}
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n');(ROOT/'build/attachment-transactions/latest.json').write_text(json.dumps(report,indent=2)+'\n')
    print(status,out/'report.json');print((out/'result.log').read_text() if (out/'result.log').exists() else (out/'compile.log').read_text())
    return 0 if status!='FAIL' else 1

if __name__=='__main__':raise SystemExit(main())
