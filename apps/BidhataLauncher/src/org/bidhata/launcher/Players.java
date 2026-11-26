package org.bidhata.launcher;

import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;

import java.util.List;

/** Which media session the home screen and lockscreen should follow. */
final class Players {

    private Players() {}

    /**
     * The one that is playing; failing that, the first one with a track loaded (a paused HiBy).
     * Sessions with no track at all - Telecom keeps one around - are ignored.
     */
    static MediaController best(List<MediaController> list) {
        if (list == null) return null;
        MediaController loaded = null;
        for (MediaController c : list) {
            if (!hasTrack(c)) continue;
            PlaybackState s = c.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) return c;
            if (loaded == null) loaded = c;
        }
        return loaded;
    }

    private static boolean hasTrack(MediaController c) {
        MediaMetadata m = c.getMetadata();
        if (m == null) return false;
        CharSequence t = m.getText(MediaMetadata.METADATA_KEY_TITLE);
        return t != null && t.length() > 0;
    }
}
