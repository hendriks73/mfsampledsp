/*
 * =================================================
 * Copyright 2007 tagtraum industries incorporated
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * First tries to load a library the default way using {@link System#loadLibrary(String)},
 * upon failure falls back to the base directory of the given class package or the jar the class
 * is in. This way, a native library is found, if it is located in the same directory as a particular jar, identified
 * by a specific class from that jar.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public final class MFNativeLibraryLoader {

    private static final boolean OS_ARCH_I386 = "i386".equals(System.getProperty("os.arch")) || "x86".equals(System.getProperty("os.arch"));
    private static final String JAR_PROTOCOL = "jar";
    private static final String FILE_PROTOCOL = "file";
    private static final String CLASS_FILE_EXTENSION = ".class";
    private static final String ARCHITECTURE_CLASSIFIER = OS_ARCH_I386 ? "-i386" : "-x86_64";
    private static final String NATIVE_LIBRARY_EXTENSION = ".dll";
    private static final Set<String> LOADED = new HashSet<String>();

    private static Boolean mfSampledSPLibraryLoaded;

    private MFNativeLibraryLoader() {
    }

    /**
     * Loads the MFSampledSP library.
     *
     * @return true, if loading was successful
     */
    public static synchronized boolean loadLibrary() {
        if (mfSampledSPLibraryLoaded != null) {
            return mfSampledSPLibraryLoaded;
        }
        boolean loaded = false;
        try {
            MFNativeLibraryLoader.loadLibrary("mfsampledsp");
            loaded = true;
        } catch (Error e) {
            Logger.getLogger(MFNativeLibraryLoader.class.getName()).severe("Failed to load native library 'mfsampledsp'. Please check your library path. MFSampledSP will be dysfunctional.");
        }
        mfSampledSPLibraryLoaded = loaded;
        return mfSampledSPLibraryLoaded;
    }



    /**
     * Loads a library.
     *
     * @param libName name of the library, as described in {@link System#loadLibrary(String)} );
     */
    public static synchronized void loadLibrary(final String libName) {
        loadLibrary(libName, MFNativeLibraryLoader.class);
    }

    /**
     * Loads a library.
     *
     * @param libName name of the library, as described in {@link System#loadLibrary(String)} );
     * @param baseClass class that identifies the jar
     */
    public static synchronized void loadLibrary(final String libName, final Class baseClass) {
        final String key = libName + "|" + baseClass.getName();
        if (LOADED.contains(key)) return;
        try {
            System.loadLibrary(libName + ARCHITECTURE_CLASSIFIER);
            LOADED.add(key);
        } catch (Error e) {
            try {
                final String libFilename = findFile(libName, baseClass, new LibFileFilter(libName));
                Runtime.getRuntime().load(libFilename);
                LOADED.add(key);
            } catch (FileNotFoundException e1) {
                throw e;
            }
        }
    }

    /**
     * Finds a file that is either in the classpath or in the same directory as a given class's jar.
     *
     * @param name (partial) filename
     * @param baseClass base class
     * @param filter filter that determines whether a file is a match
     * @return file
     * @throws java.io.FileNotFoundException if a matching file cannot be found
     */
    public static String findFile(final String name, final Class baseClass, final FileFilter filter)
            throws FileNotFoundException {
        try {
            final String filename;
            final String fullyQualifiedBaseClassName = baseClass.getName();
            final String baseClassName = fullyQualifiedBaseClassName.substring(fullyQualifiedBaseClassName.lastIndexOf('.') + 1);
            final URL url = baseClass.getResource(baseClassName + CLASS_FILE_EXTENSION);
            if (url == null) {
                throw new FileNotFoundException("Failed to get URL of " + fullyQualifiedBaseClassName);
            } else {
                File directory = null;
                final String path = URLDecoder.decode(url.getPath(), "UTF-8");
                if (JAR_PROTOCOL.equals(url.getProtocol())) {
                    final String jarFileName = new URL(path.substring(0, path.lastIndexOf('!'))).getPath();
                    directory = new File(jarFileName).getParentFile();
                } else if (FILE_PROTOCOL.equals(url.getProtocol())) {
                    directory = new File(path.substring(0, path.length()
                            - fullyQualifiedBaseClassName.length() - CLASS_FILE_EXTENSION.length()));
                }
                final File[] libs = directory.listFiles(filter);
                if (libs == null || libs.length == 0) {
                    throw new FileNotFoundException("No matching files in " + directory);
                }
                filename = libs[0].toString();
            }
            return filename;
        } catch (UnsupportedEncodingException e) {
            final FileNotFoundException fnfe = new FileNotFoundException(name + ": " + e.toString());
            fnfe.initCause(e);
            throw fnfe;
        } catch (MalformedURLException e) {
            final FileNotFoundException fnfe = new FileNotFoundException(name + ": " + e.toString());
            fnfe.initCause(e);
            throw fnfe;
        }
    }

    private static class LibFileFilter implements FileFilter {
        private final String libName;

        public LibFileFilter(final String libName) {
            this.libName = libName;
        }

        public boolean accept(final File file) {
            final String fileString = file.toString();
            final String fileName = file.getName();
            return file.isFile()
                    && fileName.startsWith(libName)
                    && fileString.endsWith(ARCHITECTURE_CLASSIFIER + NATIVE_LIBRARY_EXTENSION);
        }
    }
}
