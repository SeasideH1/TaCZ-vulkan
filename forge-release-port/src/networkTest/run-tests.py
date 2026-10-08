#!/usr/bin/env python3
"""Run focused tests against verified target jars. This is not the full-mod build or a game boot."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import time

project = Path(__file__).resolve().parents[2]
workspace = project.parent
java_home = workspace / 'toolchains/jdk-25.0.4.1+1'
classpath = os.pathsep.join((workspace / directory / 'classpath.txt').read_text().strip()
                           for directory in ['official-minecraft', 'official-fabric', 'official-java-libraries'])
production = [
    'network/LoginSession.java', 'network/NetworkContext.java', 'network/NetworkCodecs.java',
    'network/PayloadChannel.java', 'network/message/handshake/Acknowledge.java',
    'network/message/BulletSpawnData.java', 'api/entity/ReloadState.java',
    'entity/sync/core/IDataSerializer.java', 'entity/sync/core/Serializers.java',
    'entity/sync/ModSerializers.java', 'api/event/Event.java', 'api/event/Cancelable.java',
    'fabric/client/event/InputEvent.java', 'fabric/concurrent/GameThreadRepeater.java',
    'client/sound/GunSoundInstance.java', 'fabric/config/FabricConfigSpec.java',
    'fabric/config/ConfigEditSession.java', 'fabric/client/ClientTickDispatch.java',
]
tests = ['NetworkTransportTest', 'SyncedSerializerTest', 'InputCompatibilityTest',
         'BulletSpawnCodecTest', 'GameThreadRepeaterTest', 'WalkDistanceFormulaTest',
         'NativeMixinTargetTest', 'GunSoundRedirectTest', 'ConfigEditSessionTest', 'ClientTickDispatchTest']
sources = [project / 'src/main/java/com/tacz/guns' / name for name in production]
sources += [project / 'src/networkTest/java/com/tacz/guns/network' / (name + '.java') for name in tests]
stamp = time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
out = project / 'build/network-tests' / stamp
out.mkdir(parents=True, exist_ok=False)
classes = out / 'classes'
classes.mkdir()
report = {
    'target': '26.4-snapshot-3',
    'compile_scope': 'Focused production helpers and their tests; not the full production source set',
    'runtime_scope': 'No Minecraft client/server instance booted by this suite',
    'source_sha256': {str(path.relative_to(project)): hashlib.sha256(path.read_bytes()).hexdigest() for path in sources},
    'tests': {},
}
compile_run = subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-proc:none',
                              '-classpath', classpath, '-d', str(classes), *map(str, sources)],
                             text=True, capture_output=True)
(out / 'javac.log').write_text(compile_run.stdout + compile_run.stderr)
report['compile_exit_code'] = compile_run.returncode
if compile_run.returncode == 0:
    for name in tests:
        command = [str(java_home / 'bin/java'), '-classpath', str(classes) + os.pathsep + classpath,
                   'com.tacz.guns.network.' + name]
        if name == 'NativeMixinTargetTest':
            command.append(str(workspace / 'official-minecraft/client-26.4-snapshot-3.jar'))
        run = subprocess.run(command, text=True, capture_output=True)
        output = run.stdout + run.stderr
        (out / (name + '.log')).write_text(output)
        report['tests'][name] = {'exit_code': run.returncode, 'output': output.strip()}
        print(output.strip(), flush=True)
report['passed'] = compile_run.returncode == 0 and len(report['tests']) == len(tests) and all(
    test['exit_code'] == 0 for test in report['tests'].values())
(out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
print('Focused report:', out / 'report.json')
raise SystemExit(0 if report['passed'] else 1)
