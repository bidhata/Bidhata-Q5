package org.bidhata.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Stand-in cover for tracks without embedded art: one of our night-landscape scenes, picked from
 * the album name so an album always gets the same one.
 */
final class CoverArt {

    private static final int[] SCENES = { R.drawable.cover_0, R.drawable.cover_1, R.drawable.cover_2, R.drawable.cover_3 };
    private static final Bitmap[] cache = new Bitmap[SCENES.length];

    private CoverArt() {}

    /**
     * Players such as HiBy send a grey logo when a file has no cover. A real cover is rarely that
     * colourless and flat, so treat low-saturation, low-contrast art as "no cover".
     */
    static boolean looksLikePlaceholder(Bitmap b) {
        if (b == null) return true;
        Bitmap s = Bitmap.createScaledBitmap(b, 16, 16, true);
        float satSum = 0, lumMin = 1, lumMax = 0;
        float[] hsv = new float[3];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                android.graphics.Color.colorToHSV(s.getPixel(x, y), hsv);
                satSum += hsv[1];
                lumMin = Math.min(lumMin, hsv[2]);
                lumMax = Math.max(lumMax, hsv[2]);
            }
        }
        return satSum / 256f < 0.05f;
    }

    static Bitmap forAlbum(Context ctx, CharSequence album, CharSequence fallback) {
        String key = album != null && album.length() > 0 ? album.toString()
                : fallback != null ? fallback.toString() : "";
        int i = (key.hashCode() & 0x7fffffff) % SCENES.length;
        if (cache[i] == null) cache[i] = BitmapFactory.decodeResource(ctx.getResources(), SCENES[i]);
        return cache[i];
    }
}
