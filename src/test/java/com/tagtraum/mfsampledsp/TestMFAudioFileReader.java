/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.mfsampledsp;

import org.junit.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * TestMFAudioFileReader.
 * <p/>
 * Date: 8/19/11
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestMFAudioFileReader {

    @Test
    public void testGetAudioFileFormatMP3File() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("testGetAudioFileFormatFileMP3", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("mp3", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(134769, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(2, format.getChannels());

            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(192000, (int) bitrate);

            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3056000, (long) duration);
            assertEquals(4, format.getFrameSize());
            assertEquals(44100f, format.getFrameRate(), 0.01f);
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSpaceInFilename() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("test space in filename", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testPunctuationInFilename() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("test ;:&=+@[]? in filename", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatM4AFile() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.m4a";
        final File file = File.createTempFile("testGetAudioFileFormatFileM4A", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("m4a", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(136180, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(2, format.getChannels());

            final Integer bitrate = (Integer)format.getProperty("bitrate");
            assertNotNull(bitrate);
            assertEquals(92760, (int) bitrate);

            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3088000, (long) duration);
            assertEquals(4, format.getFrameSize());
            assertEquals(44100f, format.getFrameRate(), 0.01f);
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatWaveFile() throws IOException, UnsupportedAudioFileException {
        // MediaFoundation does not support WAVE under Vista http://msdn.microsoft.com/en-us/library/dd757927(v=VS.85).aspx
        if (Float.parseFloat(System.getProperty("os.version")) < 6.1f) {
            System.out.println("Skipped testGetAudioFileFormatWaveFile(), because WAVE is not supported by Vista");
            return;
        }
        // first copy the file from resources to actual location in temp
        final String filename = "test.wav";
        final File file = File.createTempFile("testGetAudioFileFormatWaveFile", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(file);
            System.out.println(fileFormat);

            assertEquals("wav", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(133623, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(2, format.getChannels());

            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3030000, (long)duration);
            assertEquals(4, format.getFrameSize());
            assertEquals(44100f, format.getFrameRate(), 0.01);
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        } finally {
            file.delete();
        }
    }


    @Test
    public void testGetAudioFileFormatURL() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("testGetAudioFileFormatURL", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(MFAudioFileReader.fileToURL(file));
            System.out.println(fileFormat);

            assertEquals("mp3", fileFormat.getType().getExtension());
            assertEquals(file.length(), fileFormat.getByteLength());
            assertEquals(134769, fileFormat.getFrameLength());

            final AudioFormat format = fileFormat.getFormat();
            assertEquals(2, format.getChannels());
            final Long duration = (Long)fileFormat.getProperty("duration");
            assertNotNull(duration);
            assertEquals(3056000, (long)duration);
            assertEquals(4, format.getFrameSize());
            assertEquals(44100f, format.getFrameRate(),     0.01);
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testGetAudioFileFormatInputStream() throws IOException, UnsupportedAudioFileException {
        // first copy the file from resources to actual location in temp
        final String filename = "test.mp3";
        final File file = File.createTempFile("testGetAudioFileFormatInputStream", filename);
        extractFile(filename, file);
        try {
            final AudioFileFormat fileFormat = new MFAudioFileReader().getAudioFileFormat(new BufferedInputStream(new FileInputStream(file)));
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
        } finally {
            file.delete();
        }
    }

    @Test
    public void testBogusFile() throws IOException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testBogusFile", filename);
        FileOutputStream out = new FileOutputStream(file);
        final Random random = new Random();
        for (int i=0; i<8*1024; i++) {
            out.write(random.nextInt());
        }
        out.close();
        MFFileInputStream in = null;
        try {
            new MFAudioFileReader().getAudioFileFormat(MFAudioFileReader.fileToURL(file));
            fail("Expected UnsupportedAudioFileException");
        } catch (UnsupportedAudioFileException e) {
            // expected this
            e.printStackTrace();
            assertTrue(e.toString().endsWith("(0xC00D36C4)"));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testFileWithPunctuationToURL() throws MalformedURLException {
        final File file = new File("c:\\someDir\\;:&=+@[]?\\name.txt");
        final URL url = MFAudioFileReader.fileToURL(file);
        assertEquals("file:/c:/someDir/%3B%3A%26%3D%2B%40%5B%5D%3F/name.txt", url.toString());
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
