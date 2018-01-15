package com.tagtraum.mfsampledsp;

import org.junit.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMFAudioInputStream {

    @Test
    public void testReadAfterSeek() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadAfterSeek", filename);
        extractFile(filename, file);
        MFAudioInputStream in = null;
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            final int frames = fileFormat.getFrameLength();
            final int bytes = frames * fileFormat.getFormat().getFrameSize();
            assertFalse(AudioSystem.NOT_SPECIFIED == frames);

            // verify we can read the advertised number of bytes WHEN WE DON'T SEEK
            in = new MFAudioInputStream(new MFFileInputStream(file.toURI().toURL()), fileFormat.getFormat(), frames);
            assertTrue(in.isSeekable());
            int justRead;
            int allRead = 0;
            while ((justRead = in.read(new byte[1024 * 4])) != -1) {
                allRead += justRead;
            }
            assertEquals(bytes, allRead);
            in.close();

            // verify we can read the advertised number of bytes EVEN WHEN WE SEEK
            in = new MFAudioInputStream(new MFFileInputStream(file.toURI().toURL()), fileFormat.getFormat(), frames);
            assertTrue(in.isSeekable());
            allRead = 0;
            // read up to 90%
            while ((justRead = in.read(new byte[1024 * 4])) != -1 && allRead < 0.9 * frames) {
                allRead += justRead;
            }
            // seek to start
            in.seek(0, TimeUnit.SECONDS);
            // now re-read it all
            allRead = 0;
            while ((justRead = in.read(new byte[1024 * 4])) != -1) {
                allRead += justRead;
            }
            assertEquals(bytes, allRead);
            in.close();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file.delete();
        }
    }

    private void extractFile(final String filename, final File file) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getClass().getResourceAsStream(filename);
            out = new FileOutputStream(file);
            final byte[] buf = new byte[1024*64];
            int justRead;
            while ((justRead = in.read(buf)) != -1) {
                out.write(buf, 0, justRead);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
