#!/usr/bin/env python3
"""Source-level original Cloth-setting coverage; does not construct or render the native GUI."""
from pathlib import Path
import datetime
import hashlib
import json
import re

root = Path(__file__).resolve().parents[2]
cloth = root / 'optional-integrations/cloth/src/main/java/com/tacz/guns/compat/cloth'
if not cloth.is_dir():
    cloth = root / 'src/main/java/com/tacz/guns/compat/cloth'
paths = sorted(cloth.glob('*/*ClothConfig.java'))
original = set()
for path in paths:
    code = re.sub(r'//[^\n]*', '', path.read_text())
    original.update(re.findall(r'\.setSaveConsumer\((\w+\.\w+)::set\)', code))
labels = root / 'src/main/java/com/tacz/guns/fabric/client/config/NativeConfigLabels.java'
fields = set(re.findall(r'add\(labels, "\w+", (\w+\.\w+),', labels.read_text()))
assert len(original) == 44, ('Unexpected original setting count', len(original))
assert original == fields, ('Missing', original - fields, 'Extra', fields - original)
report = {
    'checked_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'scope': 'Source-level coverage of surviving original Cloth entries; not GUI runtime validation',
    'original_active_settings': len(original),
    'native_original_label_matches': len(fields),
    'missing': sorted(original - fields),
    'extra_original_labels': sorted(fields - original),
    'additional_native_setting': 'render.ThirdPersonAnimationMode (explicit fallback title/description)',
    'source_sha256': {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
                      for path in paths + [labels]},
    'status': 'PASS',
}
out = root / 'build/native-config-catalog'
out.mkdir(parents=True, exist_ok=True)
(out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
print('PASS all 44 original active Cloth settings have native labels')
print('Report:', out / 'report.json')
