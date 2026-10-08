#!/usr/bin/env python3
"""Compile every active core source against verified artifacts; package only on a clean compile.

This deliberately does not execute Gradle, repository scripts, annotation processors,
or application code. It does not remap classes or replace a successful release build.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import zipfile
from jar_layout import JarLayoutWriter
from resource_migration import migrate_resource_bytes

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('project', choices=['forge-release-port', 'native-render-prototype'])
parser.add_argument('--java-home', type=Path, help='Use a local Java 25 JDK (including Windows).')
args = parser.parse_args()
project = ROOT / args.project
stamp = time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
out = project / 'build' / 'direct' / stamp
out.mkdir(parents=True, exist_ok=False)
classes = out / 'classes'
classes.mkdir()
java = (args.java_home or ROOT / 'toolchains' / 'jdk-25.0.4.1+1') / 'bin'
classpath = []
for name in ['official-minecraft', 'official-fabric']:
    classpath.extend((ROOT / name / 'classpath.txt').read_text().strip().split(os.pathsep))
extra = ROOT / 'official-java-libraries' / 'classpath.txt'
if extra.exists():
    classpath.extend(extra.read_text().strip().split(os.pathsep))
classpath = list(dict.fromkeys(classpath))
compile_api = ROOT / 'official-minecraft/compile-api/provenance.json'
applied_api_tweaks = None
if compile_api.exists():
    applied_api_tweaks = json.loads(compile_api.read_text())
    transformed = Path(applied_api_tweaks['output'])
    if hashlib.sha256(transformed.read_bytes()).hexdigest() != applied_api_tweaks['output_sha256']:
        raise SystemExit('Compile-only API JAR checksum changed; regenerate it.')
    classpath = [str(transformed) if p == applied_api_tweaks['source_client'] else p for p in classpath]

missing = [p for p in classpath if not Path(p).is_file()]
if missing:
    raise SystemExit('Missing dependency files: ' + ', '.join(missing))
sources = sorted((project / 'src/main/java').rglob('*.java'))
source_hashes = {str(p.relative_to(project)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
(out / 'sources.json').write_text(json.dumps(source_hashes, indent=2) + '\n')
resource_hashes = {str(p.relative_to(project)): hashlib.sha256(p.read_bytes()).hexdigest()
                   for p in sorted((project / 'src/main/resources').rglob('*')) if p.is_file()}
build_inputs = ['gradle.properties', 'build.gradle', '../toolchains/direct-build.py',
                '../toolchains/jar_layout.py', '../toolchains/resource_migration.py']
scope_manifest = project / 'optional-integrations/manifest.json'
if args.project == 'forge-release-port':
    scope = json.loads(scope_manifest.read_text())
    assert scope['active_source_roots'] == ['src/main/java']
    assert all(m['status'] == 'unsupported-source-only' and not m['runtime_enabled'] for m in scope['modules'])
    build_inputs.append('optional-integrations/manifest.json')
input_hashes = {name: hashlib.sha256((project / name).read_bytes()).hexdigest() for name in build_inputs}
(out / 'resources.json').write_text(json.dumps(resource_hashes, indent=2) + '\n')
(out / 'build-inputs.json').write_text(json.dumps(input_hashes, indent=2) + '\n')
# Quoting preserves classpaths and paths in javac's argument-file grammar.
def quote(value):
    return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"') + '"'
compiler_args = ['--release', '25', '-proc:none', '-encoding', 'UTF-8', '-Xmaxerrs', '20000', '-Xmaxwarns', '200', '-classpath', os.pathsep.join(classpath), '-d', str(classes)] + [str(p) for p in sources]
argfile = out / 'javac.args'
argfile.write_text('\n'.join(quote(a) for a in compiler_args) + '\n')
print('Compiling', len(sources), 'sources from', args.project, flush=True)
print('Diagnostics:', out / 'javac.log', flush=True)
with (out / 'javac.log').open('w') as log:
    result = subprocess.run([str(java / 'javac'), '-J-Xmx3G', '@' + str(argfile)], stdout=log, stderr=subprocess.STDOUT)
changed_sources = [name for name, digest in source_hashes.items() if not (project / name).is_file() or hashlib.sha256((project / name).read_bytes()).hexdigest() != digest]
new_sources = [str(path.relative_to(project)) for path in (project / 'src/main/java').rglob('*.java') if str(path.relative_to(project)) not in source_hashes]
current_resources = {str(p.relative_to(project)): hashlib.sha256(p.read_bytes()).hexdigest()
                     for p in (project / 'src/main/resources').rglob('*') if p.is_file()}
changed_resources = sorted(name for name in resource_hashes.keys() | current_resources.keys()
                           if resource_hashes.get(name) != current_resources.get(name))
changed_inputs = [name for name, digest in input_hashes.items()
                  if not (project / name).is_file() or hashlib.sha256((project / name).read_bytes()).hexdigest() != digest]
summary = {'published_transitive_api_tweaks': str(compile_api) if applied_api_tweaks else None, 'development_jar_only': True, 'runtime_dependencies_embedded': False, 'changed_during_compile': changed_sources + new_sources, 'project': args.project, 'target': '26.4-snapshot-3', 'source_count': len(sources), 'annotation_processing': False, 'source_exclusions': [], 'exit_code': result.returncode, 'log': str(out / 'javac.log'), 'classpath': classpath}
summary['changed_during_compile'] += changed_resources + changed_inputs
summary['active_source_roots'] = ['src/main/java']
summary['scope_manifest'] = str(scope_manifest) if scope_manifest.exists() else None
if result.returncode == 0 and summary['changed_during_compile']:
    summary['exit_code'] = 3
    print('Source changed during compilation; refusing to package mixed snapshot.', flush=True)
if summary['exit_code'] == 0:
    props = {}
    for line in (project / 'gradle.properties').read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            k, v = line.split('=', 1)
            props[k] = v
    replacements = {'version': props['mod_version'], 'minecraft_version': props['minecraft_version'], 'minecraft_dependency': props.get('minecraft_dependency', props['minecraft_version']), 'loader_version': props['loader_version']}
    jar = out / (args.project + '-' + props['mod_version'] + '.jar')
    def resource_path(path):
        parts = path.parts
        if len(parts) >= 4 and parts[0] == 'data':
            plural = {'loot_tables': 'loot_table', 'recipes': 'recipe', 'advancements': 'advancement', 'predicates': 'predicate', 'item_modifiers': 'item_modifier', 'functions': 'function', 'structures': 'structure'}
            tags = {'blocks': 'block', 'items': 'item', 'entity_types': 'entity_type', 'fluids': 'fluid', 'game_events': 'game_event'}
            parts = list(parts)
            parts[2] = plural.get(parts[2], parts[2])
            if parts[2] == 'tags' and len(parts) >= 5:
                parts[3] = tags.get(parts[3], parts[3])
        return '/'.join(parts)
    names = set()
    resource_transforms = []
    with zipfile.ZipFile(jar, 'w', zipfile.ZIP_DEFLATED) as archive:
        layout = JarLayoutWriter(archive)
        for path in sorted(classes.rglob('*')):
            if path.is_file():
                layout.write(path, path.relative_to(classes).as_posix())
        resources = project / 'src/main/resources'
        for path in sorted(resources.rglob('*')):
            if path.is_file():
                data = path.read_bytes()
                if path.name == 'fabric.mod.json':
                    text = data.decode()
                    for key, value in replacements.items():
                        text = text.replace('${' + key + '}', value)
                    descriptor = json.loads(text)
                    data = (json.dumps(descriptor, indent=2) + '\n').encode()
                name = resource_path(path.relative_to(resources))
                migrated = migrate_resource_bytes(name, data)
                if migrated != data:
                    resource_transforms.append({'source': str(path.relative_to(project)), 'archive_path': name,
                        'source_sha256': hashlib.sha256(data).hexdigest(), 'output_sha256': hashlib.sha256(migrated).hexdigest(),
                        'rule': 'reviewed top-level recipe schema or removed boat entity IDs'})
                data = migrated
                if name in names:
                    raise SystemExit('Resource path collision after migration: ' + name)
                names.add(name)
                layout.write_bytes(name, data)
        if scope_manifest.exists():
            layout.write(scope_manifest, 'tacz-integration-scope.json')
    summary['explicit_directory_entries'] = True
    summary['resource_transforms'] = resource_transforms
    summary['jar'] = str(jar)
    summary['jar_sha256'] = hashlib.sha256(jar.read_bytes()).hexdigest()
    print('PACKAGED:', jar, flush=True)
(out / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
(project / 'build' / 'direct' / 'latest.json').write_text(json.dumps(summary, indent=2) + '\n')
print('Compile exit code:', summary['exit_code'], flush=True)
raise SystemExit(summary['exit_code'])
