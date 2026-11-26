#!/usr/bin/env python3
"""Turn a GNOME Adwaita *-symbolic.svg (16x16, plain paths) into an Android vector drawable.

Every shape is filled with one colour (the launcher tints them), keeping per-shape opacity and
fill-rule. Good enough for the symbolic set, which has no transforms worth speaking of.
Usage: svg2vd.py in.svg out.xml color size_dp
"""
import sys, re
import xml.etree.ElementTree as ET

src, dst, color, size = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
root = ET.parse(src).getroot()
ns = lambda t: t.split('}')[-1]
vb = (root.get('viewBox') or '0 0 16 16').split()
w, h = vb[2], vb[3]

def attr(el, name, inherited):
    v = el.get(name)
    if v is None:
        m = re.search(name + r'\s*:\s*([^;]+)', el.get('style', ''))
        v = m.group(1).strip() if m else None
    return v if v is not None else inherited.get(name)

NUM = re.compile(r'[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?')
ARGS = {'m': 2, 'l': 2, 'h': 1, 'v': 1, 'c': 6, 's': 4, 'q': 4, 't': 2, 'a': 7, 'z': 0}

def normalize(d):
    """Re-write path data with every number separated and every command explicit.

    Android 6's PathParser trips over SVG shorthand such as arc flags written without
    separators ("a1 1 0 011 1"), so spell everything out.
    """
    out, i, cmd = [], 0, None
    while i < len(d):
        ch = d[i]
        if ch in ' ,\t\r\n':
            i += 1
            continue
        if ch.isalpha():
            cmd = ch
            i += 1
            if cmd.lower() == 'z':
                out.append(cmd)
            continue
        n = ARGS[cmd.lower()]
        vals = []
        for k in range(n):
            while i < len(d) and d[i] in ' ,\t\r\n':
                i += 1
            if cmd.lower() == 'a' and k in (3, 4):
                vals.append(d[i]); i += 1          # flags are single digits
            else:
                m = NUM.match(d, i)
                vals.append(m.group(0)); i = m.end()
        out.append(cmd + ' ' + ' '.join(vals))
        if cmd == 'm': cmd = 'l'                   # implicit lineto after moveto
        elif cmd == 'M': cmd = 'L'
    return ' '.join(out)

paths = []
def walk(el, inh):
    tag = ns(el.tag)
    if tag in ('defs', 'clipPath', 'mask', 'title', 'metadata'):
        return
    if el.get('transform'):
        sys.stderr.write('warning: transform ignored in %s\n' % src)
    cur = dict(inh)
    for k in ('opacity', 'fill-opacity', 'fill-rule', 'fill'):
        v = attr(el, k, {})
        if v is not None:
            cur[k] = v if k not in ('opacity',) else str(float(v) * float(inh.get('opacity', '1')))
    d = None
    if tag == 'path':
        d = el.get('d')
    elif tag == 'rect':
        x, y = float(el.get('x', 0)), float(el.get('y', 0))
        rw, rh = float(el.get('width')), float(el.get('height'))
        rx = float(el.get('rx', el.get('ry', 0)) or 0)
        if rx:
            d = ('M%g,%g h%g a%g,%g 0 0 1 %g,%g v%g a%g,%g 0 0 1 %g,%g h%g a%g,%g 0 0 1 %g,%g v%g a%g,%g 0 0 1 %g,%g z'
                 % (x + rx, y, rw - 2 * rx, rx, rx, rx, rx, rh - 2 * rx, rx, rx, -rx, rx, -(rw - 2 * rx), rx, rx, -rx, -rx,
                    -(rh - 2 * rx), rx, rx, rx, -rx))
        else:
            d = 'M%g,%g h%g v%g h%g z' % (x, y, rw, rh, -rw)
    elif tag in ('circle', 'ellipse'):
        cx, cy = float(el.get('cx', 0)), float(el.get('cy', 0))
        rx = float(el.get('r', el.get('rx', 0))); ry = float(el.get('r', el.get('ry', 0)))
        d = 'M%g,%g a%g,%g 0 1,0 %g,0 a%g,%g 0 1,0 %g,0 z' % (cx - rx, cy, rx, ry, 2 * rx, rx, ry, -2 * rx)
    if d and cur.get('fill') != 'none':
        alpha = float(cur.get('opacity', '1')) * float(cur.get('fill-opacity', '1'))
        paths.append((d, alpha, cur.get('fill-rule') == 'evenodd'))
    for c in el:
        walk(c, cur)

walk(root, {})
if any(eo for _, _, eo in paths):
    # Android 6 vector drawables have no fillType: an even-odd icon would fill its holes in.
    sys.exit('error: %s needs even-odd fill (Android 7+); pick another icon' % src)
out = ['<vector xmlns:android="http://schemas.android.com/apk/res/android"',
       '    android:width="%sdp" android:height="%sdp"' % (size, size),
       '    android:viewportWidth="%s" android:viewportHeight="%s">' % (w, h)]
for d, a, eo in paths:
    out.append('    <path android:fillColor="%s"%s' % (color, ' android:fillAlpha="%.3g"' % a if a < 0.999 else ''))
    out.append('        android:pathData="%s" />' % normalize(d))
out.append('</vector>')
open(dst, 'w').write('\n'.join(out) + '\n')
print('%s: %d shapes' % (dst.split('/')[-1], len(paths)))
