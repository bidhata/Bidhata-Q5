package org.bidhata.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Progress drawn as a row of bars, played part in amber. The bar heights are a pattern seeded by
 * the track title (Android does not give us the real waveform), so each track has its own shape.
 */
public class WaveformView extends View {

    private static final int BARS = 64;
    private final float[] heights = new float[BARS];
    private final Paint played = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rest = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;

    public WaveformView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        played.setColor(0xFFF06A12);
        rest.setColor(0x55FFFFFF);
        played.setStrokeWidth(2f * d);
        rest.setStrokeWidth(2f * d);
        played.setStrokeCap(Paint.Cap.ROUND);
        rest.setStrokeCap(Paint.Cap.ROUND);
        setSeed("");
    }

    void setSeed(String seed) {
        long s = seed.hashCode() * 6364136223846793005L + 1442695040888963407L;
        for (int i = 0; i < BARS; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            float r = ((s >>> 33) & 0xFFFF) / 65535f;
            // a bit of musical shape: louder in the middle, quieter at the ends
            float env = (float) Math.sin(Math.PI * (i + 0.5) / BARS);
            heights[i] = 0.25f + 0.75f * (0.35f + 0.65f * r) * (0.5f + 0.5f * env);
        }
        invalidate();
    }

    void setProgress(float p) {
        p = Math.max(0f, Math.min(1f, p));
        if (Math.abs(p - progress) * BARS < 0.5f) return;   // redraw only when a bar changes
        progress = p;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), step = w / BARS, mid = h / 2f;
        for (int i = 0; i < BARS; i++) {
            float x = step * (i + 0.5f), half = heights[i] * (h / 2f - played.getStrokeWidth());
            c.drawLine(x, mid - half, x, mid + half, (i + 0.5f) / BARS <= progress ? played : rest);
        }
    }
}
