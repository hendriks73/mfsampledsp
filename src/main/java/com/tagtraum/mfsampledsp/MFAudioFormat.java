/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * This file is part of MFSampledSP.
 *
 * FFSampledSP is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFSampledSP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFSampledSP; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * =================================================
 */
package com.tagtraum.mfsampledsp;

import javax.sound.sampled.AudioFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MFSampledSP's {@link AudioFormat} adding a special constructor
 * to be called from {@link MFAudioFileFormat}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class MFAudioFormat extends AudioFormat {

    public MFAudioFormat(final float sampleRate, final int sampleSize, final int channels,
                         final int packetSize, final float frameRate, final boolean bigEndian, final int bitRate, final boolean vbr) {
        super(Encoding.PCM_SIGNED, sampleRate, sampleSize, channels, packetSize, frameRate, bigEndian, createProperties(bitRate, vbr));
    }

    private static Map<String, Object> createProperties(final int bitRate, final boolean vbr) {
        final Map<String, Object> properties = new HashMap<>();
        if (bitRate > 0) properties.put("bitrate", bitRate);
        properties.put("vbr", vbr);
        return properties;
    }

    /**
     * Encoding that is aware of its Media Foundation type.
     */
    public static class MFEncoding extends Encoding {

        // constants from http://msdn.microsoft.com/en-us/library/aa372553%28v=VS.85%29.aspx
        private static final int MFAudioFormat_PCM = 1;
        private static final int MFAudioFormat_Float = 0x0003;
        private static final int MFAudioFormat_DTS = 0x0008;
        private static final int MFAudioFormat_Dolby_AC3_SPDIF = 0x0092;
        private static final int MFAudioFormat_DRM = 0x0009;
        private static final int MFAudioFormat_WMAudioV8 = 0x0161;
        private static final int MFAudioFormat_WMAudioV9 = 0x0162;
        private static final int MFAudioFormat_WMAudio_Lossless = 0x0163;
        private static final int MFAudioFormat_WMASPDIF = 0x0164;
        private static final int MFAudioFormat_MSP1 = 0x000A;
        private static final int MFAudioFormat_MP3 = 0x0055;
        private static final int MFAudioFormat_MPEG = 0x0050;
        private static final int MFAudioFormat_AAC = 0x1610;
        private static final int MFAudioFormat_ADTS = 0x1600;

        public static MFEncoding MP1 = new MFEncoding("MPEG-1, Layer 1", MFAudioFormat_MPEG);
        public static MFEncoding MP3 = new MFEncoding("MPEG-1, Layer 3", MFAudioFormat_MP3);

        public static MFEncoding FLOAT = new MFEncoding("IEEE Float", MFAudioFormat_Float);
        public static MFEncoding DTS = new MFEncoding("Digital Theater Systems", MFAudioFormat_DTS);
        public static MFEncoding DOLBY_AC3 = new MFEncoding("Dolby AC-3", MFAudioFormat_Dolby_AC3_SPDIF);
        public static MFEncoding DRM = new MFEncoding("DRM", MFAudioFormat_DRM);
        public static MFEncoding WM8 = new MFEncoding("Windows Media Audio 8", MFAudioFormat_WMAudioV8);
        public static MFEncoding WM9 = new MFEncoding("Windows Media Audio 9", MFAudioFormat_WMAudioV9);
        public static MFEncoding WM9_LOSSLESS = new MFEncoding("Windows Media Audio 9 Lossless", MFAudioFormat_WMAudio_Lossless);
        public static MFEncoding WM9_PRO = new MFEncoding("Windows Media Audio 9 Professional", MFAudioFormat_WMASPDIF);
        public static MFEncoding WM9_VOICE = new MFEncoding("Windows Media Audio 9 Voice", MFAudioFormat_MSP1);
        public static MFEncoding AAC = new MFEncoding("Advanced Audio Coding", MFAudioFormat_AAC);
        public static MFEncoding ADTS = new MFEncoding("ADTS", MFAudioFormat_ADTS);

        public static MFEncoding PCM_SIGNED = new MFEncoding(Encoding.PCM_SIGNED.toString(), MFAudioFormat_PCM);

        private static Map<Integer, MFEncoding> DATAFORMAT_MAP = new HashMap<>();
        private static Map<String, MFEncoding> NAME_MAP = new HashMap<>();

        static {
            DATAFORMAT_MAP.put(MP1.getDataFormat(), MP1);
            DATAFORMAT_MAP.put(MP3.getDataFormat(), MP3);

            DATAFORMAT_MAP.put(FLOAT.getDataFormat(), FLOAT);

            DATAFORMAT_MAP.put(DTS.getDataFormat(), DTS);
            DATAFORMAT_MAP.put(DOLBY_AC3.getDataFormat(), DOLBY_AC3);
            DATAFORMAT_MAP.put(DRM.getDataFormat(), DRM);
            DATAFORMAT_MAP.put(WM8.getDataFormat(), WM8);
            DATAFORMAT_MAP.put(WM9.getDataFormat(), WM9);

            DATAFORMAT_MAP.put(WM9_LOSSLESS.getDataFormat(), WM9_LOSSLESS);
            DATAFORMAT_MAP.put(WM9_PRO.getDataFormat(), WM9_PRO);
            DATAFORMAT_MAP.put(WM9_VOICE.getDataFormat(), WM9_VOICE);
            DATAFORMAT_MAP.put(AAC.getDataFormat(), AAC);
            DATAFORMAT_MAP.put(ADTS.getDataFormat(), ADTS);

            DATAFORMAT_MAP.put(PCM_SIGNED.getDataFormat(), PCM_SIGNED);

            for (final MFEncoding encoding : DATAFORMAT_MAP.values()) {
                NAME_MAP.put(encoding.toString(), encoding);
            }
        }

        private int dataFormat;

        public MFEncoding(final String name, final int dataFormat) {
            super(name);
            this.dataFormat = dataFormat;
        }

        public static Set<MFEncoding> getSupportedEncodings() {
            return new HashSet<>(DATAFORMAT_MAP.values());
        }

        public static synchronized MFEncoding getInstance(final String name) {
            return NAME_MAP.get(name);
        }

        public static synchronized MFEncoding getInstance(final int dataFormat) {
            MFEncoding encoding = DATAFORMAT_MAP.get(dataFormat);
            if (encoding == null) {
                encoding = new MFEncoding(toString(dataFormat), dataFormat);
                DATAFORMAT_MAP.put(dataFormat, encoding);
                NAME_MAP.put(encoding.toString(), encoding);
            }
            return encoding;
        }

        private static String toString(final int dataFormat) {
            return new String(
                    new char[]{(char) (dataFormat >> 24 & 0xff), (char) (dataFormat >> 16 & 0xff),
                            (char) (dataFormat >> 8 & 0xff), (char) (dataFormat & 0xff)}
            );
        }

        public int getDataFormat() {
            return dataFormat;
        }

    }
}
