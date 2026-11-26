package org.bidhata.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Bidhata lockscreen. LockWatcher brings it up when the screen goes off, so it is already there
 * when the player is woken. Swiping up finishes it; the window flag then drops the stock
 * (swipe) keyguard underneath, or shows the PIN pad if one is set.
 */
public class LockActivity extends Activity {

    private static String lastTitle;
    /** True from the moment the lock goes up until the user swipes it away. */
    static boolean locked;

    static void show(Context c) {
        locked = true;
        Intent lock = new Intent(c, LockActivity.class);
        lock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        c.startActivity(lock);
    }   // for "tap play to resume" after the player went away

    private final Handler handler = new Handler();
    private MediaSessionManager sessions;
    private MediaController controller;

    private View content, music, idleTop, artBox, scene;
    private ImageView art;
    private TextView title, artist, route, battery, pos, dur, resume;
    private ImageButton play;
    private View prev, next;
    private WaveformView wave;
    private Bitmap shownArt;
    private float downY;
    private boolean dragging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_lock);

        content = findViewById(R.id.lock_content);
        music = findViewById(R.id.lock_music);
        idleTop = findViewById(R.id.lock_idle_top);
        artBox = findViewById(R.id.lock_art_box);
        scene = findViewById(R.id.lock_scene);
        art = (ImageView) findViewById(R.id.lock_art);
        wave = (WaveformView) findViewById(R.id.lock_wave);
        title = (TextView) findViewById(R.id.lock_title);
        artist = (TextView) findViewById(R.id.lock_artist);
        route = (TextView) findViewById(R.id.lock_route);
        battery = (TextView) findViewById(R.id.lock_battery);
        pos = (TextView) findViewById(R.id.lock_pos);
        dur = (TextView) findViewById(R.id.lock_dur);
        resume = (TextView) findViewById(R.id.lock_resume);
        play = (ImageButton) findViewById(R.id.lock_play);

        findViewById(R.id.lock_prev).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (controller != null) controller.getTransportControls().skipToPrevious();
            }
        });
        findViewById(R.id.lock_next).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (controller != null) controller.getTransportControls().skipToNext();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });
        // tapping the hint unlocks too (not everyone swipes)
        findViewById(R.id.lock_unlock).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { unlock(); }
        });
        prev = findViewById(R.id.lock_prev);
        next = findViewById(R.id.lock_next);

        sessions = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) immersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, null);
            pickController(sessions.getActiveSessions(null));
        } catch (SecurityException e) {
            setController(null);
        }
        // the screen just came on: let the page breathe in rather than snap
        content.setTranslationY(0);
        content.setAlpha(0f);
        content.setScaleX(0.985f);
        content.setScaleY(0.985f);
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300)
                .setInterpolator(new DecelerateInterpolator()).withLayer().start();
        nudgeChevron();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        unregisterReceiver(batteryReceiver);
        try {
            sessions.removeOnActiveSessionsChangedListener(sessionsChanged);
        } catch (SecurityException ignored) {
        }
        if (controller != null) controller.unregisterCallback(callback);
        controller = null;
    }

    @Override
    public void onBackPressed() {
        // a lockscreen does not go "back"
    }

    // ---- swipe up to unlock -------------------------------------------------------------------

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = e.getRawY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dy = downY - e.getRawY();
                if (!dragging && dy > 24 * getResources().getDisplayMetrics().density) {
                    dragging = true;
                    // the buttons under the finger should not fire once this is a swipe
                    MotionEvent cancel = MotionEvent.obtain(e);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                if (dragging) {
                    float h = content.getHeight();
                    content.setTranslationY(-Math.max(0, dy));
                    content.setAlpha(1f - Math.min(1f, Math.max(0, dy) / (h * 0.5f)));
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    float dy = downY - e.getRawY();
                    if (dy > content.getHeight() * 0.22f) unlock();
                    else content.animate().translationY(0).alpha(1).setDuration(180)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(e);
    }

    /** Two small hops of the unlock arrow after waking; then it stays still (no idle animation). */
    private void nudgeChevron() {
        final View c = findViewById(R.id.lock_chevron);
        final float d = -6 * getResources().getDisplayMetrics().density;
        c.setTranslationY(0);
        c.animate().translationY(d).setStartDelay(500).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
            @Override public void run() {
                c.animate().translationY(0).setStartDelay(0).setDuration(380)
                        .setInterpolator(new android.view.animation.BounceInterpolator()).start();
            }
        }).start();
    }

    private void unlock() {
        locked = false;
        content.animate().translationY(-content.getHeight()).alpha(0).setDuration(160)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        finish();
                        overridePendingTransition(0, android.R.anim.fade_out);
                    }
                }).start();
    }

    // ---- now playing --------------------------------------------------------------------------

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged =
            new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override public void onActiveSessionsChanged(List<MediaController> list) { pickController(list); }
    };

    private void pickController(List<MediaController> list) {
        setController(Players.best(list));
    }

    private void setController(MediaController c) {
        if (controller != null) controller.unregisterCallback(callback);
        controller = c;
        if (controller != null) controller.registerCallback(callback);
        refresh();
    }

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { refresh(); }
        @Override public void onPlaybackStateChanged(PlaybackState s) { refresh(); }
        @Override public void onSessionDestroyed() { setController(null); }
    };

    /** Cover-and-waveform layout when a player has a track loaded, otherwise the landscape clock. */
    private void refresh() {
        MediaMetadata m = controller != null ? controller.getMetadata() : null;
        CharSequence t = m != null ? m.getText(MediaMetadata.METADATA_KEY_TITLE) : null;
        boolean hasTrack = t != null && t.length() > 0;

        music.setVisibility(hasTrack ? View.VISIBLE : View.GONE);
        artBox.setVisibility(hasTrack ? View.VISIBLE : View.GONE);
        scene.setVisibility(hasTrack ? View.GONE : View.VISIBLE);
        idleTop.setVisibility(hasTrack ? View.GONE : View.VISIBLE);
        resume.setVisibility(hasTrack ? View.GONE : View.VISIBLE);
        prev.setVisibility(hasTrack ? View.VISIBLE : View.INVISIBLE);
        next.setVisibility(hasTrack ? View.VISIBLE : View.INVISIBLE);

        if (hasTrack) {
            lastTitle = t.toString();
            CharSequence a = m.getText(MediaMetadata.METADATA_KEY_ARTIST);
            if (a == null) a = m.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            if (!t.toString().equals(title.getText().toString())) wave.setSeed(t.toString());
            title.setText(t);
            artist.setText(a != null ? a : "");
            Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (CoverArt.looksLikePlaceholder(b)) b = CoverArt.forAlbum(this, m.getText(MediaMetadata.METADATA_KEY_ALBUM), t);
            if (b != shownArt) {
                shownArt = b;
                art.setImageBitmap(b);
                art.setAlpha(0f);
                art.animate().alpha(1f).setDuration(400).start();
            }
        } else {
            shownArt = null;
            resume.setText(lastTitle != null ? getString(R.string.resume_hint, lastTitle) : "");
        }
        PlaybackState s = controller != null ? controller.getPlaybackState() : null;
        boolean playing = s != null && s.getState() == PlaybackState.STATE_PLAYING;
        play.setImageResource(playing ? R.drawable.l_pause : R.drawable.l_play);
    }

    private void togglePlay() {
        if (controller == null) {
            // nothing to resume through a session: a media key reaches the last player
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            long now = SystemClock.uptimeMillis();
            am.dispatchMediaKeyEvent(new android.view.KeyEvent(now, now,
                    android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY, 0));
            am.dispatchMediaKeyEvent(new android.view.KeyEvent(now, now,
                    android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY, 0));
            return;
        }
        PlaybackState s = controller.getPlaybackState();
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) controller.getTransportControls().pause();
        else controller.getTransportControls().play();
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateProgress();
            route.setText(HomeActivity.outputName(LockActivity.this));
            handler.postDelayed(this, 1000);
        }
    };

    private void updateProgress() {
        if (controller == null) return;
        PlaybackState s = controller.getPlaybackState();
        MediaMetadata m = controller.getMetadata();
        if (s == null || m == null) return;
        long d = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long p = s.getPosition();
        if (s.getState() == PlaybackState.STATE_PLAYING) {
            p += (long) ((SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed());
        }
        if (d > 0) {
            p = Math.max(0, Math.min(d, p));
            wave.setProgress((float) p / d);
            pos.setText(time(p));
            dur.setText(time(d));
        } else {
            wave.setProgress(0f);
            pos.setText("");
            dur.setText("");
        }
    }

    private static String time(long ms) {
        long s = ms / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
                         : String.format("%d:%02d", s / 60, s % 60);
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            boolean charging = i.getIntExtra(BatteryManager.EXTRA_STATUS, 0) == BatteryManager.BATTERY_STATUS_CHARGING;
            if (level >= 0) battery.setText((level * 100 / scale) + "%" + (charging ? " +" : ""));
        }
    };
}
