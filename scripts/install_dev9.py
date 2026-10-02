#!/usr/bin/env python3
"""Install the built addon atomically and invalidate GTO's persistent resource cache."""
from pathlib import Path
import argparse
import hashlib
import os
import shutil
import tempfile
from datetime import datetime

project = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('minecraft', type=Path, help='Minecraft directory of the dev9 instance; close the game first')
args = parser.parse_args()
minecraft = args.minecraft.expanduser().resolve()
jar = project / 'build/libs/gto-additions-0.1.0-dev9.jar'
mods = minecraft / 'mods'
if not mods.is_dir() or not jar.is_file():
    parser.error('Instance mods folder or built addon jar not found')
backup = project / 'build/previous' / ('install-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
backup.mkdir(parents=True)
target = mods / jar.name
if target.exists():
    shutil.copy2(target, backup / target.name)
# Replacing the inode avoids changing an already-open ZIP's central directory in place.
with tempfile.NamedTemporaryFile(dir=mods, prefix='.gtoa-install-', suffix='.tmp', delete=False) as file:
    temporary = Path(file.name)
try:
    shutil.copyfile(jar, temporary)
    os.replace(temporary, target)
finally:
    temporary.unlink(missing_ok=True)
assert hashlib.sha256(target.read_bytes()).digest() == hashlib.sha256(jar.read_bytes()).digest()
cache = minecraft / 'gtocore/cache'
# The jar index, shared cached assets, and recipe/tag JSON caches must all be cleared.
stale_targets = [
    jar.name + '.bin',
    'resources',
    'resource_exist',
    'json/recipes',
    'tags/recipe_serializer',
    'tags/recipe_type',
]
for rel in stale_targets:
    target_file = cache / rel
    if target_file.is_file():
        dest = backup / rel.replace('/', '_')
        shutil.move(str(target_file), str(dest))
print('Addon instalado; cache de recursos e receitas do GTO invalidado.')
print('Backup:', backup)
