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
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Effectively acts as a wrapper around our own {@link MFNativePeerInputStream}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class MFAudioInputStream extends AudioInputStream {

    private MFNativePeerInputStream nativePeerInputStream;

    public MFAudioInputStream(final MFNativePeerInputStream nativePeerInputStream, final AudioFormat format, final long length) {
        super(nativePeerInputStream, format, length);
        this.nativePeerInputStream = nativePeerInputStream;
    }

    MFNativePeerInputStream getNativePeerInputStream() {
        return nativePeerInputStream;
    }

    long getNativePeerInputStreamPointer() {
        return nativePeerInputStream.pointer;
    }

    /**
     * Indicates whether this stream is seekable.
     * Typically, stream based streams (as opposed to file-based streams)
     * are not seekable.
     *
     * @return true or false
     */
    public boolean isSeekable() {
        return nativePeerInputStream.isSeekable();
    }

    /**
     * Positions the stream at the desired timestamp.
     *
     * @param time time
     * @param timeUnit time unit
     * @throws UnsupportedOperationException if the operation is not supported
     * @throws IOException if something goes wrong
     */
    public void seek(final long time, final TimeUnit timeUnit) throws UnsupportedOperationException, IOException {
        nativePeerInputStream.seek(time, timeUnit);
        final long microSeconds = timeUnit.toMicros(time);
        framePos = (long)((getFormat().getFrameRate() * microSeconds) / 1000000L);
    }
}
