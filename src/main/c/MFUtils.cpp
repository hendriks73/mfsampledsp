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
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */

#include "MFUtils.h"

/**
 * Throws an UnsupportedAudioFileException exception
 */
void throwUnsupportedAudioFileExceptionIfError(JNIEnv *env, int err, const char * message) {
    if (err) {
        char formattedMessage [255];
        _snprintf_s(formattedMessage, 255, strlen(message)+13, "%s (0x%X)", message, err);
        jclass excCls = env->FindClass("javax/sound/sampled/UnsupportedAudioFileException");
        env->ThrowNew(excCls, formattedMessage);
    }
}

/**
 * Throws an IOException.
 */
void throwIOExceptionIfError(JNIEnv *env, int err, const char *message) {
    if (err) {
		//fprintf (stderr, "IOException: '%s' %d (%4.4s)\n", message, (int)err, (char*)&err);
        char formattedMessage [255];
        _snprintf_s(formattedMessage, 255, strlen(message)+13, "%s (0x%X)", message, err);
        jclass excCls = env->FindClass("java/io/IOException");
        env->ThrowNew(excCls, formattedMessage);
    }
}

/**
 * Throws an IllegalArgumentException.
 */
void throwIllegalArgumentExceptionIfError(JNIEnv *env, int err, const char *message) {
    if (err) {
        char formattedMessage [255];
        _snprintf_s(formattedMessage, 255, strlen(message)+13, "%s (0x%X)", message, err);
        jclass excCls = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(excCls, formattedMessage);
        
    }
}
/**
 * Throws a FileNotFoundException.
 */
void throwFileNotFoundExceptionIfError(JNIEnv *env, int err, const char *message) {
    if (err) {
        jclass excCls = env->FindClass("java/io/FileNotFoundException");
        env->ThrowNew(excCls, message);
    }
}

/**
 * Creates a Media Source.
 *
 * @param env JNI env
 * @param path path
 * @param ppMediaSrc media source
 * @return HRESULT
 */
HRESULT mf_createMediaSource(JNIEnv *env, jstring path, IMFMediaSource **ppMediaSrc) {

    HRESULT res = S_OK;
    const LPWSTR pwszFilePath = (LPWSTR)env->GetStringChars(path, NULL);
    IUnknown *pUnk = NULL;
    IMFSourceResolver *pResolver = NULL;
    MF_OBJECT_TYPE ObjectType = MF_OBJECT_INVALID;


    *ppMediaSrc = NULL;
    res = MFCreateSourceResolver(&pResolver);
    if (res != S_OK || pResolver == NULL) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to create source resolver");
        goto bail;
    }
        // File format may not match its extension so we ignore the extension
        res = pResolver->CreateObjectFromURL(
            pwszFilePath, 
            MF_RESOLUTION_MEDIASOURCE | MF_RESOLUTION_READ | MF_RESOLUTION_CONTENT_DOES_NOT_HAVE_TO_MATCH_EXTENSION_OR_MIME_TYPE, 
            NULL, 
            &ObjectType, 
            &pUnk);
    if (res != S_OK || pUnk == NULL) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to create object from url");
        goto bail;
    }
    res = pUnk->QueryInterface(
            IID_IMFMediaSource, 
            (void**)(ppMediaSrc));
    if (res != S_OK) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed get media source interface");
        goto bail;
    }

bail:

    SAFE_RELEASE(pResolver)
    SAFE_RELEASE(pUnk)
    env->ReleaseStringChars(path, (jchar *)pwszFilePath);

    return res;
}

/**
 * Creates a Media Source Reader.
 *
 * @param env JNI env
 * @param path path
 * @param ppMediaSrcReader media source reader
 * @return HRESULT
 */
HRESULT mf_createMediaSourceReader(JNIEnv *env, jstring path, IMFSourceReader **ppMediaSrcReader) {

    HRESULT res = S_OK;
    const LPWSTR pwszFilePath = (LPWSTR)env->GetStringChars(path, NULL);

    res = MFCreateSourceReaderFromURL(
        pwszFilePath, 
        NULL, 
        ppMediaSrcReader);
	if (HRESULT_CODE(res) == ERROR_FILE_NOT_FOUND
			|| HRESULT_CODE(res) == ERROR_PATH_NOT_FOUND
			|| HRESULT_CODE(res) == ERROR_NOT_DOS_DISK
			|| HRESULT_CODE(res) == ERROR_BAD_NETPATH) {
		const char * filePath = env->GetStringUTFChars(path, NULL);
        throwFileNotFoundExceptionIfError(env, res, filePath);
	    env->ReleaseStringUTFChars(path, filePath);
        goto bail;
	}
    if (res != S_OK) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to create source reader from url");
        goto bail;
    }

bail:

    env->ReleaseStringChars(path, (jchar *)pwszFilePath);

    return res;
}

/**
 * Creates an AudioFileFormat object.
 *
 * @return an AudioFileFormat object.
 */
jobject mf_createAudioFormatObject(JNIEnv *env, jstring url, jfloat sampleRate, jint sampleSize,
                                         jint channels, jint frameSize, jfloat frameRate, jboolean bigEndian, jlong duration,
                                         jint bitRate, jboolean vbr) {
    jclass audioFileFormatClass;
    jmethodID cid;
    jobject result = NULL;
    
    audioFileFormatClass = env->FindClass("com/tagtraum/mfsampledsp/MFAudioFileFormat");
    if (audioFileFormatClass == NULL) {
        return NULL; /* exception thrown */
    }
    
    /* Get the method ID for the constructor */
    cid = env->GetMethodID(audioFileFormatClass, "<init>", "(Ljava/lang/String;FIIIFZJIZ)V");
    if (cid == NULL) {
        return NULL; /* exception thrown */
    }
    
    /* Construct an QTAudioFileFormat object */
    result = env->NewObject(audioFileFormatClass, cid, url, sampleRate, sampleSize, channels, frameSize, frameRate, bigEndian, duration, bitRate, vbr);
    
    /* Free local references */
    env->DeleteLocalRef(audioFileFormatClass);
    return result;    
}

/**
 * Initializes COM and Media Foundation.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    HRESULT res = S_OK;

    // Initialize the COM library.
    res = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (res != S_OK && res != S_FALSE) {
        fprintf (stderr, "CoInitializeEx failed: '0x%X'\n", res);
        goto bail;
    }
    // Intialize the Media Foundation platform.
    res = MFStartup(MF_VERSION);
    if (res != S_OK) {
        fprintf (stderr, "MFStartup failed: '0x%X'\n", res);
        goto bail;
    }

bail:

    if (res == MF_E_BAD_STARTUP_VERSION) {
        fprintf (stderr, "MF_E_BAD_STARTUP_VERSION: '0x%X'\n", MF_VERSION);
    }
    return JNI_VERSION_1_4;
}

/**
 * Shuts down Media Foundation and uninitializes COM.
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    MFShutdown();
    CoUninitialize();
}