#!/usr/bin/env python3
"""Copy compile-only APIs from an existing, unmodified GTO dev9 installation."""
import argparse
import shutil
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("minecraft", type=Path, help="GTO dev9 minecraft directory")
args = parser.parse_args()
mods = args.minecraft / "mods"
root = Path(__file__).resolve().parent.parent
libs = root / "libs"
libs.mkdir(exist_ok=True)
core = mods / "gtocore-forge-1.20.1-26.9.5.jar"
with zipfile.ZipFile(core) as archive:
    gtceu = "META-INF/jarjar/gtceu-1.20.1-forge-1.20.1-26.9.70.jar"
    (libs / "gtceu-26.9.70.jar").write_bytes(archive.read(gtceu))
    (libs / "ae2-15.269.3.jar").write_bytes(archive.read("META-INF/jarjar/appliedenergistics2-forge-1.20.1-15.269.3.jar"))
shutil.copy2(core, libs / "gtocore-26.9.5.jar")
for name, version in [("ldlib", "1.0.52.a"), ("datasynclib", "26.9.4")]:
    shutil.copy2(mods / f"{name}-forge-1.20.1-{version}.jar", libs / f"{name}-{version}.jar")
print("GTO dev9 compile APIs prepared in libs/; nothing modified in the modpack.")
