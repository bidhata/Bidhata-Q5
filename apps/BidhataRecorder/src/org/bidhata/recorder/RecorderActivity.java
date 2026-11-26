package org.bidhata.recorder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A recorder that doesn't throw quality away: 48 kHz / 16-bit PCM straight from AudioRecord into
 * a WAV file, or AAC-LC at 256 kbps through MediaRecorder when space matters.
 */
public class RecorderActivity extends Activity {

    private static final int RATE = 48000;
    private static final int REQ_PERMS = 1;

    private final Handler ui = new Handler();
    private TextView timer, status;
    private ProgressBar level;
    private ImageButton recordBtn;
    private RadioGroup fmt;
    private Switch stereo;
    private ListView files;

    private volatile boolean recording;
    private Thread pcmThread;
    private MediaRecorder aac;
    private File current;
    private long startedAt;
    private volatile int peak;
    private PowerManager.WakeLock wake;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_recorder);
        timer = (TextView) findViewById(R.id.timer);
        status = (TextView) findViewById(R.id.status);
        level = (ProgressBar) findViewById(R.id.level);
        recordBtn = (ImageButton) findViewById(R.id.record);
        fmt = (RadioGroup) findViewById(R.id.fmt);
        stereo = (Switch) findViewById(R.id.stereo);
        files = (ListView) findViewById(R.id.files);

        wake = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BidhataRecorder");

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (recording) stop(); else start();
            }
        });
        files.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                File f = new File(dir(), (String) p.getItemAtPosition(pos));
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(f), f.getName().endsWith(".wav") ? "audio/wav" : "audio/mp4");
                startActivity(Intent.createChooser(i, f.getName()));
            }
        });
        askPermissions();
        refreshList();
    }

    private void askPermissions() {
        String[] perms = { android.Manifest.permission.RECORD_AUDIO,
                           android.Manifest.permission.WRITE_EXTERNAL_STORAGE };
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(perms, REQ_PERMS);
                return;
            }
        }
    }

    private boolean havePermissions() {
        return checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private File dir() {
        File d = new File(Environment.getExternalStorageDirectory(), "Recordings");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private void start() {
        if (!havePermissions()) { status.setText(R.string.need_perm); askPermissions(); return; }
        boolean wav = fmt.getCheckedRadioButtonId() == R.id.fmt_wav;
        int ch = stereo.isChecked() ? 2 : 1;
        String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        current = new File(dir(), name + (wav ? ".wav" : ".m4a"));
        try {
            if (wav) startPcm(current, ch); else startAac(current, ch);
        } catch (Exception e) {
            status.setText("Can't start: " + e.getMessage());
            return;
        }
        recording = true;
        wake.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startedAt = SystemClock.elapsedRealtime();
        recordBtn.setImageResource(R.drawable.ic_stop);
        fmt.setEnabled(false);
        stereo.setEnabled(false);
        status.setText((wav ? "WAV 48 kHz / 16-bit" : "AAC 256 kbps / 48 kHz") + (ch == 2 ? " stereo" : " mono"));
        ui.post(tick);
    }

    private void startPcm(final File f, final int ch) throws Exception {
        final int chMask = ch == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        final int min = AudioRecord.getMinBufferSize(RATE, chMask, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IllegalStateException("48 kHz not supported");
        final AudioRecord rec = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, chMask,
                AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 4, 16384));
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) { rec.release(); throw new IllegalStateException("no microphone"); }
        final WavWriter w = new WavWriter(f, RATE, ch);
        rec.startRecording();
        pcmThread = new Thread("pcm") {
            @Override public void run() {
                byte[] buf = new byte[8192];
                try {
                    while (recording) {
                        int n = rec.read(buf, 0, buf.length);
                        if (n <= 0) continue;
                        w.write(buf, n);
                        int p = 0;
                        for (int i = 0; i + 1 < n; i += 2) {
                            int s = Math.abs((short) ((buf[i] & 0xff) | (buf[i + 1] << 8)));
                            if (s > p) p = s;
                        }
                        peak = p;
                    }
                } catch (Exception ignored) {
                } finally {
                    try { rec.stop(); } catch (Exception ignored) { }
                    rec.release();
                    try { w.close(); } catch (Exception ignored) { }
                }
            }
        };
        pcmThread.start();
    }

    private void startAac(File f, int ch) throws Exception {
        aac = new MediaRecorder();
        aac.setAudioSource(MediaRecorder.AudioSource.MIC);
        aac.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        aac.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        aac.setAudioSamplingRate(RATE);
        aac.setAudioChannels(ch);
        aac.setAudioEncodingBitRate(256000);
        aac.setOutputFile(f.getAbsolutePath());
        aac.prepare();
        aac.start();
    }

    private void stop() {
        recording = false;
        if (aac != null) {
            try { aac.stop(); } catch (Exception ignored) { }
            aac.release();
            aac = null;
        }
        if (pcmThread != null) {
            try { pcmThread.join(2000); } catch (InterruptedException ignored) { }
            pcmThread = null;
        }
        if (wake.isHeld()) wake.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ui.removeCallbacks(tick);
        recordBtn.setImageResource(R.drawable.ic_mic);
        fmt.setEnabled(true);
        stereo.setEnabled(true);
        level.setProgress(0);
        if (current != null) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(current)));
            status.setText("Saved " + current.getName() + " (" + (current.length() / 1024) + " KB)");
        }
        refreshList();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long s = (SystemClock.elapsedRealtime() - startedAt) / 1000;
            timer.setText(String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
            int p = aac != null ? aac.getMaxAmplitude() : peak;
            level.setProgress(Math.min(100, p * 100 / 32767));
            if (s >= 3 && p == 0) status.setText(R.string.no_mic);
            if (recording) ui.postDelayed(this, 100);
        }
    };

    private void refreshList() {
        File[] fs = dir().listFiles();
        List<String> names = new ArrayList<>();
        if (fs != null) {
            Arrays.sort(fs, new Comparator<File>() {
                @Override public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
            });
            for (File f : fs) if (f.isFile()) names.add(f.getName());
        }
        files.setAdapter(new ArrayAdapter<>(this, R.layout.file_row, names));
    }

    @Override
    protected void onDestroy() {
        if (recording) stop();
        super.onDestroy();
    }
}
