#!/usr/bin/env python
# Wrap a file in a MediaTek 512-byte image header (magic 0x58881688, size, 32-byte name, 0xFF pad).
# The Q5's lk refuses kernel/ramdisk blobs without it. Works with python 2 and 3.
import struct, sys
if len(sys.argv) != 4:
    sys.exit("usage: mtk_hdr.py <ROOTFS|RECOVERY|KERNEL> <in> <out>")
name, src, dst = sys.argv[1], sys.argv[2], sys.argv[3]
data = open(src, 'rb').read()
if data[:4] == b'\x88\x16\x88\x58':
    sys.exit("%s already has a MediaTek header" % src)
hdr = struct.pack('<II32s', 0x58881688, len(data), name.encode('ascii'))
hdr += b'\xff' * (512 - len(hdr))
open(dst, 'wb').write(hdr + data)
