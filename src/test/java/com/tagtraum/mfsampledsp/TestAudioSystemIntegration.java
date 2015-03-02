/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.mfsampledsp;

import org.junit.Assert;
import org.junit.Test;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertTrue;

/**
 * TestAudioSystemIntegration.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestAudioSystemIntegration {

    @Test
    public void testAudioFileReader() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testAudioFileReader", filename);
        extractFile(filename, file);

        int bytesRead = 0;
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(file);
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Bytes read: " + bytesRead);
    }

    @Test
    public void testAudioFileReader2() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testAudioFileReader", filename);
        extractFile(filename, file);

        int bytesRead = 0;
        AudioInputStream in = null;
        try {
            final AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(file);
            in = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, mp3Stream);
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Bytes read: " + bytesRead);
    }

    @Test
    public void testWMA() throws UnsupportedAudioFileException, IOException, InterruptedException, InvocationTargetException {
        new MFAudioFileReader();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(new File("C:\\Users\\Public\\Music\\Sample Music\\BachCPE_SonataAmin_1.wma"));
                    System.out.println(audioFileFormat);
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
