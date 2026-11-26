package org.bidhata.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Bidhata home ("B"): a now-playing card (showing the audio output: LDAC, AAC, wired...) that
 * follows whichever player owns the active media session (normally HiBy), and eight tiles for
 * what a music player is used for. Everything else lives behind "More".
 */
public class HomeActivity extends Activity {

    /** The eight tile slots of the layout, in reading order. */
    private static final int[] SLOT_IDS = {
        R.id.tile_music, R.id.tile_radio, R.id.tile_books, R.id.tile_video,
        R.id.tile_recorder, R.id.tile_eq, R.id.tile_files, R.id.tile_more };

    private final Handler handler = new Handler();
    private MediaSessionManager sessions;
    private MediaController controller;

    private ImageView art;
    private TextView title, artist, codec;
    private ImageButton play;
    private ProgressBar progress;
    private Bitmap shownArt;
    private View card, vinyl, sheen;
    private android.animation.ObjectAnimator spin;
    private boolean resumed;

    // Swipe down anywhere on home to open notifications. The status bar is only 36 px tall
    // on this screen, right against the bezel, and easy to miss with a finger.
    private float swipeX, swipeY;
    private boolean swipeDone;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent e) {
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                swipeX = e.getX(); swipeY = e.getY(); swipeDone = false;
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                float dy = e.getY() - swipeY, dx = Math.abs(e.getX() - swipeX);
                float need = 72 * getResources().getDisplayMetrics().density;
                if (!swipeDone && dy > need && dy > 2 * dx) {
                    swipeDone = true;
                    openNotifications();
                    // the tile under the finger must not fire as a tap
                    android.view.MotionEvent cancel = android.view.MotionEvent.obtain(e);
                    cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    return true;
                }
                break;
        }
        return swipeDone || super.dispatchTouchEvent(e);
    }

    private void openNotifications() {
        try {
            Object sb = getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb);
        } catch (Exception ex) {
            android.util.Log.w("BidhataHome", "could not open notifications", ex);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        art = (ImageView) findViewById(R.id.np_art);
        card = findViewById(R.id.np_card);
        vinyl = findViewById(R.id.np_vinyl);
        sheen = findViewById(R.id.np_sheen);
        // 33 1/3 rpm would be a blur on this screen; one turn in 4 s reads as "spinning"
        spin = android.animation.ObjectAnimator.ofFloat(vinyl, "rotation", 0f, 360f);
        spin.setDuration(4000);
        spin.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        spin.setInterpolator(new android.view.animation.LinearInterpolator());
        art.setClipToOutline(true);
        title = (TextView) findViewById(R.id.np_title);
        artist = (TextView) findViewById(R.id.np_artist);
        codec = (TextView) findViewById(R.id.np_codec);
        play = (ImageButton) findViewById(R.id.np_play);
        progress = (ProgressBar) findViewById(R.id.np_progress);

        bindTiles();

        findViewById(R.id.np_prev).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (controller != null) controller.getTransportControls().skipToPrevious();
            }
        });
        findViewById(R.id.np_next).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (controller != null) controller.getTransportControls().skipToNext();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });
        findViewById(R.id.np_card).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openPlayer(); }
        });

        sessions = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        AppList.preload(this);
        if (savedInstanceState == null) entrance();
    }

    /** Fills the tile slots from the home config (the user's file, or the built-in default). */
    private void bindTiles() {
        java.util.List<HomeConfig.Tile> tiles = HomeConfig.tiles();
        String problem = HomeConfig.problem;
        if (problem != null) {
            HomeConfig.problem = null;   // say it once
            android.widget.Toast.makeText(this, "home.json: " + problem
                    + ". Showing the default home.", android.widget.Toast.LENGTH_LONG).show();
        }
        for (int i = 0; i < SLOT_IDS.length; i++) {
            View tile = findViewById(SLOT_IDS[i]);
            if (i >= tiles.size()) { tile.setVisibility(View.INVISIBLE); continue; }
            final HomeConfig.Tile t = tiles.get(i);
            tile.setVisibility(View.VISIBLE);
            ((TextView) tile.findViewById(R.id.tile_label)).setText(t.label);
            ImageView icon = (ImageView) tile.findViewById(R.id.tile_icon);
            Integer res = HomeConfig.ICONS.get(t.icon);
            android.graphics.drawable.Drawable own = res == null ? appIcon(t.apps) : null;
            if (own != null) icon.setImageDrawable(own);
            else icon.setImageResource(res != null ? res : R.drawable.c_more);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if ("sound".equals(t.action)) {
                        startActivity(new Intent(HomeActivity.this, SoundActivity.class));
                    } else if ("more".equals(t.action)) {
                        startActivity(new Intent(HomeActivity.this, AppsActivity.class));
                    } else {
                        launchFirst(t.apps);
                    }
                }
            });
            tile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { askEditHome(); return true; }
            });
        }
    }

    private android.graphics.drawable.Drawable appIcon(String[] packages) {
        for (String pkg : packages) {
            try { return getPackageManager().getApplicationIcon(pkg); }
            catch (android.content.pm.PackageManager.NameNotFoundException e) { /* try the next */ }
        }
        return null;
    }

    /** Long-press on a tile: edit the tiles as a text file, or go back to the default. */
    private void askEditHome() {
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.home_edit_title)
                .setItems(new CharSequence[] { getString(R.string.home_edit), getString(R.string.home_reset) },
                        new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        editHome(which == 1);
                    }
                }).show();
    }

    /** Writes the file off the main thread, then opens the editor or redraws the tiles. */
    private void editHome(final boolean reset) {
        new android.os.AsyncTask<Void, Void, Object>() {
            @Override protected Object doInBackground(Void... v) {
                try {
                    if (reset) {
                        HomeConfig.writeDefault();
                        HomeConfig.invalidate();
                        HomeConfig.refresh();
                        return null;
                    }
                    return HomeConfig.ensureFile();
                } catch (Exception e) {
                    return e;
                }
            }
            @Override protected void onPostExecute(Object r) {
                if (r instanceof Exception) {
                    android.widget.Toast.makeText(HomeActivity.this,
                            String.valueOf(((Exception) r).getMessage()), android.widget.Toast.LENGTH_LONG).show();
                } else if (r == null) {
                    bindTiles();
                } else {
                    Intent edit = new Intent(Intent.ACTION_VIEW);
                    edit.setDataAndType(android.net.Uri.fromFile((java.io.File) r), "text/plain");
                    edit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // straight into the bundled text editor when it is there, no chooser
                    edit.setPackage("net.gsantner.markor");
                    if (edit.resolveActivity(getPackageManager()) == null) edit.setPackage(null);
                    try { startActivity(edit); }
                    catch (RuntimeException e) {
                        android.widget.Toast.makeText(HomeActivity.this, String.valueOf(e.getMessage()),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            }
        }.execute();
    }

    /** Looks for a changed home.json in the background; redraws the tiles only if it changed. */
    private void refreshTiles() {
        new android.os.AsyncTask<Void, Void, Boolean>() {
            @Override protected Boolean doInBackground(Void... v) { return HomeConfig.refresh(); }
            @Override protected void onPostExecute(Boolean changed) {
                if (changed && !isFinishing()) bindTiles();
            }
        }.execute();
    }

    /** Card and tiles rise in one after another. Once, when the home screen is created. */
    private void entrance() {
        int[] ids = SLOT_IDS;
        float rise = 16 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            v.setAlpha(0f);
            v.setTranslationY(rise);
            v.animate().alpha(1f).translationY(0f).setStartDelay(40 + i * 35L).setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                    .withLayer().start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (LockActivity.locked) {
            // the home key must not be a way around the lockscreen
            LockActivity.show(this);
            return;
        }
        refreshTiles();   // came back from editing home.json: show the change
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, null);
            pickController(sessions.getActiveSessions(null));
        } catch (SecurityException e) {
            showNothing();
        }
        updateRoute();   // setController() above starts the ticker when a player is around
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        spin.pause();
        handler.removeCallbacks(ticker);
        try {
            sessions.removeOnActiveSessionsChangedListener(sessionsChanged);
        } catch (SecurityException ignored) {
        }
        setController(null);
    }

    @Override
    public void onBackPressed() {
        // home is home
    }

    // ---- apps ---------------------------------------------------------------------------------

    private void launchFirst(String[] packages) {
        for (String pkg : packages) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(i);
                return;
            }
        }
        // nothing installed for this tile yet: show the app list instead of doing nothing
        startActivity(new Intent(this, AppsActivity.class));
    }

    private void openPlayer() {
        if (controller != null) launchFirst(new String[] { controller.getPackageName() });
        else launchFirst(new String[] { "com.hiby.music", "org.videolan.vlc" });
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
        handler.removeCallbacks(ticker);
        if (controller == null) { showNothing(); updateRoute(); return; }
        handler.post(ticker);
        controller.registerCallback(callback);
        showMetadata(controller.getMetadata());
        showState(controller.getPlaybackState());
    }

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { showMetadata(m); }
        @Override public void onPlaybackStateChanged(PlaybackState s) { showState(s); }
        @Override public void onSessionDestroyed() { setController(null); }
    };

    /** Nothing loaded in any player: no card at all, the tiles take the room. */
    private void showNothing() {
        spin.cancel();
        shownArt = null;
        progress.setProgress(0);
        play.setImageResource(R.drawable.ic_play);
        if (card.getVisibility() == View.GONE) return;
        if (resumed) {
            card.animate().alpha(0f).translationY(-card.getHeight() / 3f).setDuration(220)
                    .withEndAction(new Runnable() {
                        @Override public void run() { card.setVisibility(View.GONE); }
                    }).start();
        } else {
            card.setVisibility(View.GONE);
        }
    }

    private void showCard() {
        if (card.getVisibility() == View.VISIBLE) return;
        card.setVisibility(View.VISIBLE);
        card.setAlpha(0f);
        card.setTranslationY(-24 * getResources().getDisplayMetrics().density);
        card.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }

    private void showMetadata(MediaMetadata m) {
        CharSequence t = m != null ? m.getText(MediaMetadata.METADATA_KEY_TITLE) : null;
        if (t == null || t.length() == 0) { showNothing(); return; }
        PlaybackState ps = controller != null ? controller.getPlaybackState() : null;
        if (ps == null || ps.getState() != PlaybackState.STATE_PLAYING) { showNothing(); return; }
        showCard();
        CharSequence a = m.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (a == null) a = m.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        title.setText(t);
        artist.setText(a != null ? a : "");
        Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        boolean noCover = CoverArt.looksLikePlaceholder(b);
        vinyl.setVisibility(noCover ? View.VISIBLE : View.GONE);
        sheen.setVisibility(noCover ? View.VISIBLE : View.GONE);
        art.setVisibility(noCover ? View.INVISIBLE : View.VISIBLE);
        if (noCover) {
            shownArt = null;
        } else if (b != shownArt) {
            shownArt = b;
            art.setImageBitmap(b);
            art.setAlpha(0.2f);
            art.animate().alpha(1f).setDuration(260).start();
        }
        play.setImageResource(R.drawable.ic_pause);
        if (resumed && vinyl.getVisibility() == View.VISIBLE) {
            if (!spin.isStarted()) spin.start(); else if (spin.isPaused()) spin.resume();
        }
    }

    private void showState(PlaybackState s) {
        boolean playing = s != null && s.getState() == PlaybackState.STATE_PLAYING;
        // the strip is for what is playing now: paused or stopped, it steps aside
        if (!playing) { showNothing(); return; }
        if (controller != null && card.getVisibility() != View.VISIBLE) {
            showMetadata(controller.getMetadata());
            return;
        }
        play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        // the record turns only while playing and only while the home screen is in front
        if (playing && resumed && vinyl.getVisibility() == View.VISIBLE) {
            if (!spin.isStarted()) spin.start(); else if (spin.isPaused()) spin.resume();
        } else if (spin.isStarted()) {
            spin.pause();
        }
    }

    private void togglePlay() {
        if (controller == null) { openPlayer(); return; }
        PlaybackState s = controller.getPlaybackState();
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) controller.getTransportControls().pause();
        else controller.getTransportControls().play();
    }

    /** Once a second: track position and the audio output pill. */
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateProgress();
            updateRoute();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateProgress() {
        if (controller == null) return;
        PlaybackState s = controller.getPlaybackState();
        MediaMetadata m = controller.getMetadata();
        if (s == null || m == null) return;
        long dur = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if (dur <= 0) { progress.setProgress(0); return; }
        long pos = s.getPosition();
        if (s.getState() == PlaybackState.STATE_PLAYING) {
            pos += (long) ((SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed());
        }
        progress.setProgress((int) Math.max(0, Math.min(1000, pos * 1000 / dur)));
    }

    // ---- status -------------------------------------------------------------------------------

    private void updateRoute() {
        codec.setText(outputName(this));
    }

    /** What the sound is going out through: the Bluetooth codec, wired, or the speaker. */
    static String outputName(Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am.isBluetoothA2dpOn()) {
            String c = sysprop("bluetooth.a2dp.codec");
            return c.isEmpty() ? "BLUETOOTH" : c;
        }
        return am.isWiredHeadsetOn() ? "WIRED" : "SPEAKER";
    }

    private static String sysprop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class);
            Object v = get.invoke(null, key);
            return v != null ? v.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

}
