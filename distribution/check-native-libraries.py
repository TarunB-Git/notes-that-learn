#!/usr/bin/env python3
"""Verify ELF LOAD segments in an APK for Android's 16 KB page configuration."""
import struct
import sys
import zipfile


def check_library(data):
    if data[:4] != b'\x7fELF' or data[4] not in (1, 2) or data[5] not in (1, 2):
        raise ValueError('invalid ELF header')
    endian = '<' if data[5] == 1 else '>'
    if data[4] == 2:
        offset = struct.unpack_from(endian + 'Q', data, 32)[0]
        size, count = struct.unpack_from(endian + 'HH', data, 54)
        fmt = endian + 'IIQQQQQQ'
    else:
        offset = struct.unpack_from(endian + 'I', data, 28)[0]
        size, count = struct.unpack_from(endian + 'HH', data, 42)
        fmt = endian + 'IIIIIIII'
    if size < struct.calcsize(fmt) or count == 0:
        raise ValueError('invalid program header table')
    alignments = []
    for index in range(count):
        row = struct.unpack_from(fmt, data, offset + index * size)
        if row[0] != 1:  # PT_LOAD
            continue
        file_offset, address = (row[2], row[3]) if data[4] == 2 else (row[1], row[2])
        alignment = row[7]
        if alignment < 16384 or file_offset % 16384 != address % 16384:
            raise ValueError(f'LOAD alignment {alignment:#x}, offset {file_offset:#x}, address {address:#x}')
        alignments.append(alignment)
    if not alignments:
        raise ValueError('no LOAD segments')
    return min(alignments)


def main(apk):
    failed = False
    with zipfile.ZipFile(apk) as archive:
        libraries = [name for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')]
        if not libraries:
            raise SystemExit('FAIL: no native libraries in APK')
        for name in libraries:
            try:
                alignment = check_library(archive.read(name))
                print(f'PASS: {name}: {alignment:#x}')
            except (ValueError, struct.error, IndexError) as error:
                print(f'FAIL: {name}: {error}')
                failed = True
    return int(failed)


if __name__ == '__main__':
    sys.exit(main(sys.argv[1]))
