package org.bidhata.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Personal boot screen: takes the text-free Bidhata boot animation from /system/media, writes
 * the owner's name into every frame (same serif style and fade-in as the stock "BIDHATA"),
 * and saves a new bootanimation.zip. The q5 root helper moves it to /data/system/theme/,
 * which the boot animation checks before the system one.
 */
final class BootName {

    static final String BASE = "/system/media/bidhata_boot_base.zip";
    private static final int INTRO_FRAMES = 72;          // part0 of the base animation
    private static final String SUBTITLE = "high fidelity, beautifully";

    private BootName() {}

    static void build(String name, File out) throws IOException {
        ZipFile base = new ZipFile(BASE);
        File tmp = new File(out.getPath() + ".part");
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(tmp));
        // the boot animation only reads uncompressed ("stored") entries
        zo.setMethod(ZipOutputStream.STORED);
        try {
            Paint title = textPaint(46f);
            fit(title, name);
            Paint sub = textPaint(15f);
            sub.setColor(0xE6EBE1CD);
            Enumeration<? extends ZipEntry> en = base.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                byte[] data = read(base.getInputStream(e));
                String n = e.getName();
                if (n.endsWith(".jpg")) {
                    float alpha = n.startsWith("part1/") ? 1f : fade(frameIndex(n));
                    if (alpha > 0f) data = stamp(data, name, title, sub, alpha);
                }
                put(zo, n, data);
            }
        } finally {
            zo.close();
            base.close();
        }
        if (!tmp.renameTo(out)) throw new IOException("cannot write " + out);
    }

    /** Same easing as the stock animation: the name fades in over the last third of the intro. */
    private static float fade(int i) {
        float t = (i / (float) (INTRO_FRAMES - 1) - 0.62f) / 0.3f;
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static int frameIndex(String entry) {
        String leaf = entry.substring(entry.lastIndexOf('/') + 1, entry.lastIndexOf('.'));
        try { return Integer.parseInt(leaf); } catch (NumberFormatException ex) { return 0; }
    }

    private static Paint textPaint(float size) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        p.setTextSize(size);
        p.setColor(0xFFF0ECE4);
        p.setShadowLayer(6f, 0f, 0f, 0x96000000);
        return p;
    }

    /** Long names get a smaller size so they stay inside the screen. */
    private static void fit(Paint p, String name) {
        while (p.getTextSize() > 20f && spacedWidth(p, name) > 440f) p.setTextSize(p.getTextSize() - 2f);
    }

    private static float spacing(Paint p) { return p.getTextSize() * 10f / 46f; }

    private static float spacedWidth(Paint p, String s) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += p.measureText(s, i, i + 1);
        return w + spacing(p) * Math.max(0, s.length() - 1);
    }

    private static byte[] stamp(byte[] jpg, String name, Paint title, Paint sub, float alpha) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inMutable = true;
        Bitmap b = BitmapFactory.decodeByteArray(jpg, 0, jpg.length, o);
        Canvas c = new Canvas(b);
        int a = Math.round(alpha * 255);
        title.setAlpha(a);
        sub.setAlpha(Math.round(alpha * 230));
        // letter-spaced, centred; the stock wordmark's top edge sits at y=104
        float x = (b.getWidth() - spacedWidth(title, name)) / 2f;
        float baseline = 104f - title.getFontMetrics().ascent;
        for (int i = 0; i < name.length(); i++) {
            c.drawText(name, i, i + 1, x, baseline, title);
            x += title.measureText(name, i, i + 1) + spacing(title);
        }
        float sw = sub.measureText(SUBTITLE);
        c.drawText(SUBTITLE, (b.getWidth() - sw) / 2f, 168f - sub.getFontMetrics().ascent, sub);
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        b.compress(Bitmap.CompressFormat.JPEG, 90, out);
        b.recycle();
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zo, String name, byte[] data) throws IOException {
        ZipEntry e = new ZipEntry(name);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        e.setCrc(crc.getValue());
        zo.putNextEntry(e);
        zo.write(data);
        zo.closeEntry();
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        try {
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            in.close();
        }
        return out.toByteArray();
    }
}
