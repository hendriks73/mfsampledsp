/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.mfsampledsp;

import org.junit.Assert;
import org.junit.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TestMFFileInputStream.
 * <p/>
 * Date: 8/20/11
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestMFFileInputStream {

    @Test
    public void testOpenAndCloseMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testOpenAndCloseMP3File", filename);
        extractFile(filename, file);
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
            final byte[] buf = new byte[1024];
            // read some
            in.read(buf);
        } finally {
            if (in != null) {
                in.close();
                // try to close second time - mustn't be a problem
                in.close();
            }
        }
    }

    @Test
    public void testOpenAndCloseInOtherThreadMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testOpenAndCloseInOtherThreadMP3File", filename);
        extractFile(filename, file);
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
            final MFFileInputStream finalIn = in;
            final byte[] buf = new byte[1024];
            // read some
            in.read(buf);
            new Thread(new Runnable(){
                public void run() {
                    try {
                        System.out.println("Other thread close()");
                        finalIn.close();
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
        } finally {
            if (in != null) {
                System.out.println("This thread close()");
                in.close();
            }
        }
    }

    @Test
    public void testReadThroughMP3File() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
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
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadThroughMP3FileWithPunctuation() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File;:&=+@[]?", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
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
        System.out.println("Read " + bytesRead + " bytes.");
    }

    @Test
    public void testReadThroughMP3FileWithDegrees() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testReadThroughMP3File\u00b0", filename);
        extractFile(filename, file);
        int bytesRead = 0;
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
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
        System.out.println("Read " + bytesRead + " bytes.");
    }


    @Test
    public void testReadThroughWaveFile() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.wav";
        final File file = File.createTempFile("testReadThroughWaveFile", filename);
        extractFile(filename, file);

        // pre-computed reference values index 1024-50 to 1024 (excl.)
        final int[] referenceValues = new int[]{240, 255, 230, 255, 230, 255, 232, 255, 232, 255, 247, 255, 247, 255, 246, 255, 246, 255, 235, 255, 235, 255, 250, 255, 250, 255, 13, 0, 13, 0, 15, 0, 15, 0, 39, 0, 39, 0, 87, 0, 87, 0, 90, 0, 90, 0, 31, 0, 31, 0};

        int bytesRead = 0;
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
            int justRead;
            final byte[] buf = new byte[1024];
            while ((justRead = in.read(buf)) != -1) {
                assertTrue(justRead > 0);
                bytesRead += justRead;
                if (bytesRead == 1024) {
                    for (int i=0; i<50; i++) {
                        Assert. assertEquals(referenceValues[i], buf[i+(1024-50)] & 0xFF);
                    }
                }
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
        Assert.assertEquals(133609, (bytesRead / 4));
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
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(MFAudioFileReader.fileToURL(file));
            in.read(new byte[1024]);
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
    public void testNonExistingFile() throws IOException, UnsupportedAudioFileException {
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(new File("/Users/hendrik/bcisdbvigfeir.wav").toURI().toURL());
            in.read(new byte[1024]);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // expected this
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
    public void testNonExistingURL() throws IOException, UnsupportedAudioFileException {
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(new URL("http://www.bubsdfaegfaeu.de/hendrik/bcisdbvigfeir.wav"));
            in.read(new byte[1024]);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // expected this
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
    public void testSeekBackwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekBackwards", filename);
        extractFile(filename, file);
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(file.toURI().toURL());
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(0, TimeUnit.MICROSECONDS);
            in.read(new byte[1024 * 4]);
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

    @Test
    public void testSeekForwards() throws IOException, UnsupportedAudioFileException {
        final String filename = "test.mp3";
        final File file = File.createTempFile("testSeekForwards", filename);
        extractFile(filename, file);
        MFFileInputStream in = null;
        try {
            in = new MFFileInputStream(file.toURI().toURL());
            assertTrue(in.isSeekable());
            in.read(new byte[1024 * 4]);
            in.seek(1, TimeUnit.SECONDS);
            in.read(new byte[1024 * 4]);
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
