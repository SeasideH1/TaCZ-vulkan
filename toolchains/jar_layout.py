"""Deterministic explicit JAR directories for ClassLoader resource-root lookup."""
from pathlib import PurePosixPath, Path
from zipfile import ZipInfo, ZIP_DEFLATED

class JarLayoutWriter:
    def __init__(self, archive):
        self.archive = archive
        self.entries = set(archive.namelist())

    def directory(self, name):
        name = str(name).rstrip('/')
        if not name:
            return
        path = PurePosixPath(name)
        if path.is_absolute() or '..' in path.parts:
            raise ValueError('Unsafe archive directory: ' + name)
        current = ''
        for part in path.parts:
            current += part + '/'
            if current in self.entries:
                continue
            entry = ZipInfo(current, (1980, 1, 1, 0, 0, 0))
            entry.external_attr = (0o40755 << 16) | 0x10
            self.archive.writestr(entry, b'')
            self.entries.add(current)

    def write_bytes(self, name, data):
        name = str(name)
        if name.endswith('/'):
            if data:
                raise ValueError('Directory entry has content: ' + name)
            self.directory(name)
            return
        path = PurePosixPath(name)
        if path.is_absolute() or '..' in path.parts or name in self.entries:
            raise ValueError('Unsafe or duplicate archive file: ' + name)
        if len(path.parts) > 1:
            self.directory('/'.join(path.parts[:-1]))
        entry = ZipInfo(name, (1980, 1, 1, 0, 0, 0))
        entry.compress_type = ZIP_DEFLATED
        entry.external_attr = 0o100644 << 16
        self.archive.writestr(entry, data)
        self.entries.add(name)

    def write(self, source, name):
        self.write_bytes(name, Path(source).read_bytes())
