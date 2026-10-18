#!/usr/bin/env python3
"""Narrow release check, not a replacement for the F-Droid scanner."""
import sys
import zipfile
from pathlib import Path

apk = Path(sys.argv[1])
with zipfile.ZipFile(apk) as archive:
    names = archive.namelist()
    dex_files = [name for name in names if name.endswith('.dex')]
    if not dex_files:
        raise SystemExit('FAIL: no DEX files in APK')
    forbidden = (b'com/google/mlkit/', b'com/google/android/gms/', b'com/google/firebase/')
    for name in dex_files:
        data = archive.read(name)
        for marker in forbidden:
            if marker in data:
                raise SystemExit(f'FAIL: {marker.decode()} in {name}')
    if any('NOTICE-Google' in name for name in names):
        raise SystemExit('FAIL: standard Google assets included')
    print('PASS: no ML Kit, Play services or Firebase package descriptors in DEX')
    print('Native libraries:')
    for name in names:
        if name.startswith('lib/') and name.endswith('.so'):
            print(f'  {name}')
