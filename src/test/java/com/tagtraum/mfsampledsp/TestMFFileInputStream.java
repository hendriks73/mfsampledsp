/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.mfsampledsp;

import org.junit.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * TestMFFileInputStream.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestMFFileInputStream {

    @Test
    public void testOpenAndCloseMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testOpenAndCloseMP3File", filename);
        extractFile(filename, file);
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            final byte[] buf = new byte[1024];
            // read some
            in.read(buf);
        }
    }

    @Test
    public void testOpenAndCloseInOtherThreadMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testOpenAndCloseInOtherThreadMP3File", filename);
        extractFile(filename, file);
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            final byte[] buf = new byte[1024];
            // read some
            in.read(buf);
            new Thread(new Runnable(){
                public void run() {
                    try {
                        System.out.println("Other thread close()");
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "OtherThread").start();
            synchronized (this) {
                try {
                    this.wait(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testReadThroughMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        }
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadThroughMP3FileWithPunctuation() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File;:&=+@[]?", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        }
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadThroughMP3FileWithDegrees() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File\u00b0", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        }
        System.out.println("Read " + bytesRead + " bytes.");
    }


    @Test
    public void testReadThroughWaveFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadThroughWaveFile", filename);
        extractFile(filename, file);

        // pre-computed reference values index 1024-50 to 1024 (excl.)
        final int[] referenceValues = new int[]{240, 255, 230, 255, 230, 255, 232, 255, 232, 255, 247, 255, 247, 255, 246, 255, 246, 255, 235, 255, 235, 255, 250, 255, 250, 255, 13, 0, 13, 0, 15, 0, 15, 0, 39, 0, 39, 0, 87, 0, 87, 0, 90, 0, 90, 0, 31, 0, 31, 0};

        long bytesRead = 0;
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
                if (bytesRead == 1024) {
                    for (int i=0; i<50; i++) {
                        assertEquals(referenceValues[i], buf[i+(1024-50)] & 0xFF);
                    }
                }
            }
        }
        assertEquals(133632L, (bytesRead / 4));
    }

    @Test
    public void testBogusFile() throws IOException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testBogusFile", filename);
        FileOutputStream out = new FileOutputStream(file);
        final Random random = new Random();
        for (int i=0; i<8*1024; i++) {
            out.write(random.nextInt());
        }
        out.close();
        try (final MFFileInputStream in = new MFFileInputStream(MFAudioFileReader.fileToURL(file))) {
            in.read(new byte[1024]);
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
            e.printStackTrace();
            assertTrue(e.toString().endsWith("(0xC00D36C4)"));
        }
    }


    @Test
    public void testNonExistingFile() throws IOException, UnsupportedAudioFileException {
        try (final MFFileInputStream in = new MFFileInputStream(new File("/Users/hendrik/bcisdbvigfeir.wav").toURI().toURL())) {
            in.read(new byte[1024]);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // expected this
        }
    }

    @Test
    public void testNonExistingURL() throws IOException, UnsupportedAudioFileException {
        try (final MFFileInputStream in = new MFFileInputStream(new URL("http://www.bubsdfaegfaeu.de/hendrik/bcisdbvigfeir.wav"))) {
            in.read(new byte[1024]);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // expected this
        }
    }

    @Test
    public void testSeekBackwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekBackwards", filename);
        extractFile(filename, file);
        try (final MFFileInputStream in = new MFFileInputStream(file.toURI().toURL())) {
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(0, TimeUnit.MICROSECONDS);
            in.read(new byte[1024 * 4]);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSeekForwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekForwards", filename);
        extractFile(filename, file);
        try (final MFFileInputStream in = new MFFileInputStream(file.toURI().toURL())) {
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(1, TimeUnit.SECONDS);
            in.read(new byte[1024 * 4]);
        } finally {
            file.delete();
        }
    }

    @Test(expected = IOException.class)
    public void testSeekAfterClose() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekAfterClose", filename);
        extractFile(filename, file);
        try (final MFFileInputStream in = new MFFileInputStream(file.toURI().toURL())) {
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.close();
            in.seek(1, TimeUnit.SECONDS);
        } finally {
            file.delete();
        }
    }

    private void extractFile(final String filename, final File file) throws IOException {
        try (final InputStream in = getClass().getResourceAsStream(filename);
             final OutputStream out = new FileOutputStream(file)) {
            final byte[] buf = new byte[1024*64];
            int justRead;
            while ((justRead = in.read(buf)) != -1) {
                out.write(buf, 0, justRead);
            }
        }
    }

}
