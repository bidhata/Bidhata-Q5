package org.bidhata.recorder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Plain PCM WAV file: header written up front, sizes patched in when the file is closed. */
final class WavWriter {
    private final RandomAccessFile out;
    private final int rate, channels;
    private long dataBytes;

    WavWriter(File f, int rate, int channels) throws IOException {
        this.rate = rate;
        this.channels = channels;
        out = new RandomAccessFile(f, "rw");
        out.setLength(0);
        writeHeader();
    }

    void write(byte[] buf, int len) throws IOException {
        out.write(buf, 0, len);
        dataBytes += len;
    }

    void close() throws IOException {
        out.seek(0);
        writeHeader();
        out.close();
    }

    private void writeHeader() throws IOException {
        int byteRate = rate * channels * 2;
        out.writeBytes("RIFF");
        out.writeInt(Integer.reverseBytes((int) (36 + dataBytes)));
        out.writeBytes("WAVE");
        out.writeBytes("fmt ");
        out.writeInt(Integer.reverseBytes(16));
        out.writeShort(Short.reverseBytes((short) 1));          // PCM
        out.writeShort(Short.reverseBytes((short) channels));
        out.writeInt(Integer.reverseBytes(rate));
        out.writeInt(Integer.reverseBytes(byteRate));
        out.writeShort(Short.reverseBytes((short) (channels * 2)));
        out.writeShort(Short.reverseBytes((short) 16));
        out.writeBytes("data");
        out.writeInt(Integer.reverseBytes((int) dataBytes));
    }
}
