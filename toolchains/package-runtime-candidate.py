#!/usr/bin/env python3
"""Bundle verified pure-Java dependencies into a successfully compiled private candidate.

Uses Fabric's published nested-JAR metadata shape (the same synthetic ID scheme as
Loom 1.18.3), without executing Gradle/Loom or changing any library entry bytes.
No runtime-ready claim is made: actual client/server tests remain required.
"""
import argparse
import hashlib
import io
import os
import tempfile
import json
from pathlib import Path
import re
import zipfile
from jar_layout import JarLayoutWriter

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--prepare-libraries-only', action='store_true')
a = parser.parse_args()
verified = json.loads((ROOT / 'official-java-libraries/verified.json').read_text())
RUNTIME_COORDINATES = {
    'org.apache.commons:commons-math3:3.6.1',
    'org.apache.bcel:bcel:6.6.1',
    'com.maydaymemory:mae:1.1.2',
    'org.apache.maven:maven-artifact:3.9.12',
    'com.electronwill.night-config:core:3.6.7',
    'com.electronwill.night-config:toml:3.6.7',
    'com.github.FiguraMC.luaj:luaj-core:3.0.8-figura',
    'com.github.FiguraMC.luaj:luaj-jse:3.0.8-figura',
    'dev.kosmx.player-anim:anim-core:1.0.2-rc1+1.20',
}
records = [x for x in verified if x['coordinate'] in RUNTIME_COORDINATES]
assert {x['coordinate'] for x in records} == RUNTIME_COORDINATES, 'Missing declared core runtime dependency'
out = ROOT / 'official-java-libraries/nestable'
out.mkdir(exist_ok=True)
results = []
for record in records:
    source = Path(record['path'])
    original = source.read_bytes()
    assert hashlib.sha256(original).hexdigest() == record['sha256'], source
    group, artifact, version = record['coordinate'].split(':')
    mod_id = (group + '_' + artifact).replace('.', '_').lower()
    if len(mod_id) > 64:
        mod_id = mod_id[:50] + hashlib.sha256(mod_id.encode()).hexdigest()[:14]
    assert re.fullmatch(r'[a-z][a-z0-9_-]{1,63}', mod_id), mod_id
    assert re.fullmatch(r'\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?', version), version
    descriptor = {'schemaVersion': 1, 'id': mod_id, 'version': version, 'name': artifact,
                  'custom': {'fabric-loom:generated': True, 'tacz:builder': 'reviewed-direct-packager'}}
    target = out / source.name
    with zipfile.ZipFile(io.BytesIO(original)) as archive:
        entries = {info.filename: archive.read(info.filename) for info in archive.infolist()}
    assert 'fabric.mod.json' not in entries, 'Existing mod metadata requires separate review'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_STORED if name.endswith('/') else zipfile.ZIP_DEFLATED
            info.external_attr = ((0o40755 << 16) | 0x10) if name.endswith('/') else (0o100644 << 16)
            archive.writestr(info, entries[name])
        info = zipfile.ZipInfo('fabric.mod.json', (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        archive.writestr(info, json.dumps(descriptor, indent=2) + '\n')
    with zipfile.ZipFile(target) as archive:
        assert all(archive.read(name) == data for name, data in entries.items())
        assert set(archive.namelist()) == set(entries) | {'fabric.mod.json'}
    results.append({'coordinate': record['coordinate'], 'source_sha256': record['sha256'], 'mod_id': mod_id,
                    'path': str(target), 'sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
                    'all_original_entry_bytes_preserved': True})
(out / 'verified.json').write_text(json.dumps(results, indent=2) + '\n')
print('Verified nested-library preparation:', len(results))
if a.prepare_libraries_only:
    raise SystemExit(0)
project = ROOT / 'forge-release-port'
summary = json.loads((project / 'build/direct/latest.json').read_text())
if summary['exit_code'] != 0 or summary.get('changed_during_compile'):
    raise SystemExit('Full active-source compile has not passed; refusing candidate packaging.')
snapshot = json.loads(Path(summary['log']).with_name('sources.json').read_text())
current = {str(path.relative_to(project)): hashlib.sha256(path.read_bytes()).hexdigest()
           for path in (project / 'src/main/java').rglob('*.java')}
if current != snapshot:
    raise SystemExit('Sources changed after the successful compile; rebuild before candidate packaging.')
snapshot_dir = Path(summary['log']).parent
resource_snapshot = json.loads((snapshot_dir / 'resources.json').read_text())
current_resources = {str(path.relative_to(project)): hashlib.sha256(path.read_bytes()).hexdigest()
                     for path in (project / 'src/main/resources').rglob('*') if path.is_file()}
if current_resources != resource_snapshot:
    raise SystemExit('Resources changed after the successful compile; rebuild before candidate packaging.')
for name, digest in json.loads((snapshot_dir / 'build-inputs.json').read_text()).items():
    if hashlib.sha256((project / name).read_bytes()).hexdigest() != digest:
        raise SystemExit('Build input changed after the successful compile: ' + name)
source = Path(summary['jar'])
assert hashlib.sha256(source.read_bytes()).hexdigest() == summary['jar_sha256']
handle, temporary = tempfile.mkstemp(prefix=source.stem + '-runtime-candidate-', suffix='.jar.part', dir=source.parent)
os.close(handle)
candidate = Path(temporary)
with zipfile.ZipFile(source) as original, zipfile.ZipFile(candidate, 'w', zipfile.ZIP_DEFLATED) as output:
    layout = JarLayoutWriter(output)
    descriptor = json.loads(original.read('fabric.mod.json'))
    assert descriptor['id'] == 'tacz'
    assert not descriptor.get('jars'), 'Existing nested entries require explicit reconciliation'
    descriptor['jars'] = [{'file': 'META-INF/jars/' + Path(x['path']).name} for x in results]
    for info in original.infolist():
        if info.is_dir():
            layout.directory(info.filename)
            continue
        data = original.read(info.filename)
        if info.filename == 'fabric.mod.json':
            data = (json.dumps(descriptor, indent=2) + '\n').encode()
        layout.write_bytes(info.filename, data)
    layout.write(project / 'LICENSE', 'LICENSE')
    for library in results:
        layout.write(Path(library['path']), 'META-INF/jars/' + Path(library['path']).name)
with zipfile.ZipFile(candidate) as check:
    names = check.namelist()
    assert len(names) == len(set(names)), 'Duplicate candidate archive entries'
    mod = json.loads(check.read('fabric.mod.json'))
    scope = json.loads(check.read('tacz-integration-scope.json'))
    assert scope['active_source_roots'] == ['src/main/java']
    assert all(not m['runtime_enabled'] and m['status'] == 'unsupported-source-only' for m in scope['modules'])
    for module in scope['modules']:
        for preserved in module['files']:
            old = preserved['original_path']
            if old.startswith('src/main/java/'):
                assert old.removeprefix('src/main/java/').removesuffix('.java') + '.class' not in names
            if old.startswith('src/main/resources/'):
                assert old.removeprefix('src/main/resources/') not in names
    assert '${' not in check.read('fabric.mod.json').decode(), 'Unexpanded mod metadata'
    for group in mod.get('entrypoints', {}).values():
        for entry in group:
            value = entry if isinstance(entry, str) else entry['value']
            assert value.replace('.', '/') + '.class' in names, ('Missing entrypoint', value)
    for entry in mod.get('mixins', []):
        name = entry if isinstance(entry, str) else entry['config']
        mixins = json.loads(check.read(name))
        for side in ['mixins', 'client', 'server']:
            for suffix in mixins.get(side, []):
                cls = (mixins['package'] + '.' + suffix).replace('.', '/') + '.class'
                assert cls in names, ('Missing declared mixin class', cls)
    ids = set()
    for lib in mod['jars']:
        with zipfile.ZipFile(io.BytesIO(check.read(lib['file']))) as nested:
            info = json.loads(nested.read('fabric.mod.json'))
            assert info['id'] not in ids, 'Duplicate nested mod identifier'
            ids.add(info['id'])
digest = hashlib.sha256(candidate.read_bytes()).hexdigest()
final_path = source.with_name(source.stem + '-runtime-candidate-' + digest[:12] + '.jar')
if final_path.exists():
    assert hashlib.sha256(final_path.read_bytes()).hexdigest() == digest, 'Hash-prefix collision: refusing replacement'
    candidate.unlink()  # Remove only the temporary file just created by this invocation.
else:
    candidate.replace(final_path)
candidate = final_path
report = {'status': 'runtime candidate; execution/parity unverified', 'source_compile_log': summary['log'],
          'source_jar': str(source), 'source_sha256': summary['jar_sha256'], 'jar': str(candidate),
          'sha256': hashlib.sha256(candidate.read_bytes()).hexdigest(), 'libraries': results,
          'structural_checks': 'unique entries, expanded metadata, entrypoints, declared mixins and unique nested library IDs passed'}
(candidate.parent / ('runtime-candidate-' + digest[:12] + '.json')).write_text(json.dumps(report, indent=2) + '\n')
(project / 'build/direct/runtime-candidate-latest.json').write_text(json.dumps(report, indent=2) + '\n')
print('PACKAGED RUNTIME CANDIDATE:', candidate)
