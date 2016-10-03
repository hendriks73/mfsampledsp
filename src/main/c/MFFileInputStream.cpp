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

#include "com_tagtraum_mfsampledsp_MFFileInputStream.h"
#include "MFUtils.h"


static jfieldID nativeBufferFID = NULL;
static jmethodID rewindMID = NULL;
static jmethodID limitMID = NULL;

/**
 * Init static method and field ids for Java methods/fields, if we don't have them already.
 *
 * @param env JNIEnv
 * @param stream calling stream instance
 */
static void init_ids(JNIEnv *env, jobject stream) {
    // get method and field ids, if we don't have them already
    if (nativeBufferFID == NULL || rewindMID == NULL || limitMID == NULL) {
        nativeBufferFID = env->GetFieldID(env->GetObjectClass(stream), "nativeBuffer", "Ljava/nio/ByteBuffer;");
        jclass bufferClass = env->FindClass("java/nio/Buffer");
        rewindMID = env->GetMethodID(bufferClass, "rewind", "()Ljava/nio/Buffer;");
        limitMID = env->GetMethodID(bufferClass, "limit", "(I)Ljava/nio/Buffer;");
    }
}

/**
 * Get some properties from a IMFSourceReader.
 *
 * @param pReader a IMFSourceReader
 * @param pulFlags flags
 * @return HRESULT
 */
static HRESULT GetSourceFlags(IMFSourceReader *pReader, ULONG *pulFlags) {
    HRESULT res = S_OK;
    ULONG flags = 0;
    PROPVARIANT var;

    PropVariantInit(&var);
    res = pReader->GetPresentationAttribute(MF_SOURCE_READER_MEDIASOURCE, MF_SOURCE_READER_MEDIASOURCE_CHARACTERISTICS, &var);
    if (SUCCEEDED(res)) {
        res = PropVariantToUInt32(var, &flags);
    }
    if (SUCCEEDED(res)) {
        *pulFlags = flags;
    }
    PropVariantClear(&var);
    return res;
}

/**
 * Indicate whether we can seek in a IMFSourceReader.
 *
 * @param pReader a IMFSourceReader
 * @return BOOL
 */
static BOOL SourceCanSeek(IMFSourceReader *pReader) {
    BOOL canSeek = FALSE;
    ULONG flags;

    if (SUCCEEDED(GetSourceFlags(pReader, &flags))) {
        canSeek = ((flags & MFMEDIASOURCE_CAN_SEEK) == MFMEDIASOURCE_CAN_SEEK);
    }
    return canSeek;
}

/**
 * Fills the native Java buffer with new data from the source.
 *
 * @param env JNI env
 * @param stream calling stream instance
 * @param aioPtr pointer to the MFAudioIO struct
 */
JNIEXPORT void JNICALL Java_com_tagtraum_mfsampledsp_MFFileInputStream_fillNativeBuffer(JNIEnv *env, jobject stream, jlong aioPtr) {
#ifdef DEBUG
    fprintf(stderr, "fillNativeBuffer: %i\n", aioPtr);
#endif

    HRESULT res = S_OK;
    jobject byteBuffer = NULL;
    MFAudioIO *aio = (MFAudioIO*)aioPtr;
    char *nativeBuffer;
    UINT32 flags = 0;
    jlong nativeBufferLength;
    DWORD dwFlags = 0;
    DWORD cbAudioData = 0;
    DWORD cbBuffer = 0;
    DWORD pcbMaxLength = 0;
    BYTE *pAudioData = NULL;

    IMFSample *pSample = NULL;
    IMFMediaBuffer *pBuffer = NULL;

    init_ids(env, stream);

    // get java-managed byte buffer reference
    byteBuffer = env->GetObjectField(stream, nativeBufferFID);    
    if (byteBuffer == NULL) {
        throwIOExceptionIfError(env, 1, "Failed to get native buffer");
        goto bail;
    }

    res = aio->mediaSrcReader->ReadSample(
        (DWORD)MF_SOURCE_READER_FIRST_AUDIO_STREAM,
        0,
        NULL,
        &dwFlags,
        NULL,
        &pSample
        );

    if (dwFlags & MF_SOURCE_READERF_CURRENTMEDIATYPECHANGED) {
        throwIOExceptionIfError(env, 1, "Media type changed");
        goto bail;
    }
    if (dwFlags & MF_SOURCE_READERF_ENDOFSTREAM) {
        env->CallObjectMethod(byteBuffer, rewindMID);
        env->CallObjectMethod(byteBuffer, limitMID, 0);
        goto bail;
    }

    if (pSample == NULL) {
        fprintf(stderr, "No sample\n");
        goto bail;
    }
    // Get a pointer to the audio data in the sample.
    res = pSample->ConvertToContiguousBuffer(&pBuffer);
    if (res) {
        throwIOExceptionIfError(env, 1, "Failed convert to contiguous buffer");
        goto bail;
    }

    res = pBuffer->GetMaxLength(&pcbMaxLength);
    if (res) {
        throwIOExceptionIfError(env, 1, "Failed convert to contiguous buffer");
        goto bail;
    }

    nativeBufferLength = env->GetDirectBufferCapacity(byteBuffer);
    if (nativeBufferLength < pcbMaxLength) {
		// create bigger buffer
        jclass nativePeerInputStreamClass = env->FindClass("com/tagtraum/mfsampledsp/MFNativePeerInputStream");
        jmethodID setNativeBufferCapacityMID = env->GetMethodID(nativePeerInputStreamClass, "setNativeBufferCapacity", "(I)V");
		env->CallVoidMethod(stream, setNativeBufferCapacityMID, pcbMaxLength);
	    byteBuffer = env->GetObjectField(stream, nativeBufferFID);    
	    nativeBufferLength = env->GetDirectBufferCapacity(byteBuffer);
    }

    // get pointer to our java managed bytebuffer
    nativeBuffer = (char *)env->GetDirectBufferAddress(byteBuffer);
    if (nativeBuffer == NULL) {
        throwIOExceptionIfError(env, 1, "Failed to get address for native buffer");
        goto bail;
    }

    res = pBuffer->Lock(&pAudioData, NULL, &cbBuffer);
    if (res) {
        throwIOExceptionIfError(env, 1, "Failed to lock buffer");
        goto bail;
    }
    // copy to our native buffer
    memcpy_s(nativeBuffer, (rsize_t)nativeBufferLength, pAudioData, cbBuffer);
    res = pBuffer->Unlock();
    if (res) {
        throwIOExceptionIfError(env, 1, "Failed to unlock buffer");
        goto bail;
    }

    // we already wrote to the buffer, now we still need to
    // set new bytebuffer limit and position to 0.
    env->CallObjectMethod(byteBuffer, rewindMID);
    env->CallObjectMethod(byteBuffer, limitMID, (jint)cbBuffer);

bail:

    if (pAudioData) {
        pBuffer->Unlock();
    }
    SAFE_RELEASE(pSample);
    SAFE_RELEASE(pBuffer);
    pAudioData = NULL;
    return;
}

/**
 * Opens a Media Source.
 *
 * @param env JNI env
 * @param stream calling stream instance
 * @param url URL of the media source
 * @return pointer to the MFAudioIO struct
 */
JNIEXPORT jlong JNICALL Java_com_tagtraum_mfsampledsp_MFFileInputStream_open(JNIEnv *env, jobject stream, jstring url) {

    HRESULT res = S_OK;
    MFAudioIO *aio = new MFAudioIO;
    IMFMediaType *pPartialType = NULL;

    // Create a partial media type that specifies uncompressed PCM audio.
    res = MFCreateMediaType(&pPartialType);
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to create media type");
        goto bail;
    }
    res = pPartialType->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Audio);
    res = pPartialType->SetGUID(MF_MT_SUBTYPE, MFAudioFormat_PCM);
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to set media type");
        goto bail;
    }

    res = mf_createMediaSourceReader(env, url, &(aio->mediaSrcReader));
    if (res) {
        // exception already thrown
        goto bail;
    }
    // Set this type on the source reader. The source reader will
    // load the necessary decoder.
    res = aio->mediaSrcReader->SetCurrentMediaType(
        (DWORD)MF_SOURCE_READER_FIRST_AUDIO_STREAM,
        NULL,
        pPartialType
        );
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to set media type");
        goto bail;
    }
    // Ensure the stream is selected.
    res = aio->mediaSrcReader->SetStreamSelection(
        (DWORD)MF_SOURCE_READER_FIRST_AUDIO_STREAM,
        TRUE
        );
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to select first audio stream");
        goto bail;
    }
    //fprintf(stderr, "Source is open : '%i'\n", 0);

bail:

    SAFE_RELEASE(pPartialType)
    if (res) {
        SAFE_RELEASE(aio->mediaSrcReader);
        delete aio;
        aio = NULL;
    } 
    return (jlong)aio;
}

/**
 * Indicates, whether we can seek in this media source.
 *
 * @param env JNI env
 * @param stream calling stream instance
 * @param aioPtr pointer to the MFAudioIO struct
 * @return true, if we can seek
 */
JNIEXPORT jboolean JNICALL Java_com_tagtraum_mfsampledsp_MFFileInputStream_isSeekable(JNIEnv *env, jobject stream, jlong aioPtr) {
    MFAudioIO *aio = (MFAudioIO*)aioPtr;

    if (SourceCanSeek(aio->mediaSrcReader)) return JNI_TRUE;
    else return JNI_FALSE;
}

/**
 * Seeks to a given timestamp in the media source.
 *
 * @param env JNI env
 * @param stream calling stream instance
 * @param aioPtr pointer to the MFAudioIO struct
 * @param hundredNanoSeconds timestamp in 100 nanoseconds
 */
JNIEXPORT void JNICALL Java_com_tagtraum_mfsampledsp_MFFileInputStream_seek(JNIEnv *env, jobject stream, jlong aioPtr, jlong hundredNanoSeconds) {
    HRESULT res = S_OK;
    MFAudioIO *aio = (MFAudioIO*)aioPtr;
    PROPVARIANT var;

    res = InitPropVariantFromInt64((LONGLONG)hundredNanoSeconds, &var);
    if (res) {
        throwIOExceptionIfError(env, res, "Failed to init property.");
        goto bail;
    }
    if (aio == NULL) {
        throwIOExceptionIfError(env, 1, "Cannot seek on closed MFAudioIO");
        goto bail;
    }
    res = aio->mediaSrcReader->SetCurrentPosition(GUID_NULL, var);
    PropVariantClear(&var);
    if (res) {
        throwIOExceptionIfError(env, res, "Failed to seek desired position.");
        goto bail;
    }

    bail:

    return;
}

/**
 * Closes the media source and frees all associated resources.
 *
 * @param env JNI env
 * @param stream calling stream instance
 * @param aioPtr pointer to the MFAudioIO struct
 */
JNIEXPORT void JNICALL Java_com_tagtraum_mfsampledsp_MFFileInputStream_close(JNIEnv *env, jobject stream, jlong aioPtr) {
    MFAudioIO *aio = (MFAudioIO*)aioPtr;
    // TODO: check this
    if (aio != NULL) {
        SAFE_RELEASE(aio->mediaSrcReader);
        delete aio;
        aio = NULL;
    }
}
