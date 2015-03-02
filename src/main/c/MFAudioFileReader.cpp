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

#include "com_tagtraum_mfsampledsp_MFAudioFileReader.h"
#include "MFUtils.h"
#include "Propkey.h"

/**
 * Creates an AudioFileFormat instance for the given URL.
 *
 * @param env JNI env
 * @param instance calling stream instance
 * @param url url
 * @return AudioFileFormat instance
 */
JNIEXPORT jobject JNICALL Java_com_tagtraum_mfsampledsp_MFAudioFileReader_intGetAudioFormat__Ljava_lang_String_2(JNIEnv *env, jobject instance, jstring url) {

    HRESULT res = S_OK;
    jobject audioFormat = NULL;
    IMFMediaSource *pMediaSrc = NULL;
    IMFPresentationDescriptor *pPD = NULL;

    UINT64 mfDuration = 0;
    UINT32 mfBitRate = 0;
    UINT32 mfChannels = 0;
    double mfSampleRate = 0;
    UINT32 mfSampleRateInt = 0;
    UINT32 mfBitsPerSample = 0;
    UINT32 mfBytesPerSample = 0;
    BOOL mfVBR = FALSE;

    DWORD cStreams = 0;
    DWORD dwStreamId = 0;
    BOOL bSelected = FALSE;
    IMFStreamDescriptor *pSD = NULL;
    IMFMetadata *pMFMetadata = NULL;
    IMFMetadataProvider *pMetaProvider = NULL;
    IMFGetService *pMetadataService = NULL;

    IPropertyStore *pProps = NULL;

    res = mf_createMediaSource(env, url, &pMediaSrc);
    if (res) {
        // we already threw an exception
        goto bail;
    }

    // get attributes
    res = pMediaSrc->CreatePresentationDescriptor(&pPD);
    if (res) {
        throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to create presentation descriptor");
        goto bail;
    }
    res = pPD->GetUINT64(MF_PD_DURATION, &mfDuration);
    // error handling?
    res = pPD->GetUINT32(MF_PD_AUDIO_ENCODING_BITRATE, &mfBitRate);
    // error handling?
    res = pMediaSrc->QueryInterface(IID_IMFGetService, (void**)&pMetadataService);

    if (SUCCEEDED(pMetadataService->GetService(MF_METADATA_PROVIDER_SERVICE, IID_IMFMetadataProvider, (void**)&pMetaProvider))) {

        // we do this, in case we couldn't get a MF_PROPERTY_HANDLER_SERVICE (wave files)
#ifdef DEBUG
        fprintf(stderr, "Let's try MF_METADATA_PROVIDER_SERVICE\n");
#endif

        // iterate over streams
        res = pPD->GetStreamDescriptorCount(&cStreams);
        if (res) {
            throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to get stream count");
            goto bail;
        }
        for (DWORD i = 0; i < cStreams; i++) {
            bSelected = FALSE;
            SAFE_RELEASE(pSD)
            SAFE_RELEASE(pMFMetadata)

            res = pPD->GetStreamDescriptorByIndex(i, &bSelected, &pSD);
            if (res) {
                throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to get stream descriptor");
                goto bail;
            }
            if (FALSE == bSelected) {
                res = pPD->SelectStream(i);
                if (res) {
                    throwUnsupportedAudioFileExceptionIfError(env, res, "Failed to select stream");
                    goto bail;
                }
            }

            // ===

            DWORD cTypes = 0;
            BOOL  bTypeOK = FALSE;

            IMFMediaTypeHandler *pHandler = NULL;
            IMFMediaType *pMediaType = NULL;

            res = pSD->GetMediaTypeHandler(&pHandler);
            res = pHandler->GetMediaTypeCount(&cTypes);
            //fprintf(stderr, "3 GetMediaTypeCount : '%i'\n", cTypes);
            for (DWORD iType = 0; iType < cTypes; iType++) {
                res = pHandler->GetMediaTypeByIndex(iType, &pMediaType);
                if (FAILED(res)) {
                    break;
                }
                GUID mfMajorType;
                res = pMediaType->GetGUID(MF_MT_MAJOR_TYPE, &mfMajorType);
                if (mfMajorType != MFMediaType_Audio) continue;

                res = pMediaType->GetUINT32(MF_MT_AUDIO_NUM_CHANNELS, &mfChannels);
                res = pMediaType->GetDouble(MF_MT_AUDIO_FLOAT_SAMPLES_PER_SECOND, &mfSampleRate);
                res = pMediaType->GetUINT32(MF_MT_AUDIO_SAMPLES_PER_SECOND, &mfSampleRateInt);
                res = pMediaType->GetUINT32(MF_MT_AUDIO_BITS_PER_SAMPLE, &mfBitsPerSample);
                res = pMediaType->GetUINT32(MF_MT_SAMPLE_SIZE, &mfBytesPerSample);
            }

            // =====
            res = pSD->GetStreamIdentifier(&dwStreamId);
            if (res) {
                throwUnsupportedAudioFileExceptionIfError(env, res, "Failed get stream identifier");
                goto bail;
            }
        }
    } else if (SUCCEEDED(MFGetService(pMediaSrc, MF_PROPERTY_HANDLER_SERVICE, IID_PPV_ARGS(&pProps)))) {
#ifdef DEBUG
        fprintf(stderr, "Let's try MF_PROPERTY_HANDLER_SERVICE\n");
#endif
        PROPVARIANT pv;
        if (SUCCEEDED(pProps->GetValue(PKEY_Audio_ChannelCount, &pv))) {
            mfChannels = pv.uintVal;
        }
        if (SUCCEEDED(pProps->GetValue(PKEY_Audio_SampleRate, &pv))) {
            mfSampleRateInt = pv.uintVal;
        }
        if (SUCCEEDED(pProps->GetValue(PKEY_Audio_SampleSize, &pv))) {
            mfBitsPerSample = pv.uintVal;
            mfBytesPerSample = mfBitsPerSample/8; // this may not be accurate!!
        }
        if (SUCCEEDED(pProps->GetValue(PKEY_Audio_IsVariableBitRate, &pv))) {
            mfVBR = pv.boolVal;
        }
        if (SUCCEEDED(pProps->GetValue(PKEY_Audio_EncodingBitrate, &pv))) {
            mfBitRate = pv.uintVal;
        }
        PropVariantClear(&pv);
    } else {
        throwUnsupportedAudioFileExceptionIfError(env, 1, "Failed to read meta data");
        goto bail;
    }

    // we always decode to LPCM, so we ignore this
    jfloat sampleRate = (jfloat) (mfSampleRate == 0.0 ? (double) mfSampleRateInt : mfSampleRate);
    // if we don't know, we just say it's 16 bits per sample, because that's what we want to decode to
    jint sampleSize = mfBitsPerSample == 0 ? (mfBytesPerSample == 0 ? 16 : mfBytesPerSample * 8) : mfBitsPerSample;
    jint channels = mfChannels > 0 ? (jint)mfChannels : -1;
    jint frameSize = sampleSize == -1 || channels == -1 ? -1 : sampleSize * channels / 8;
    jboolean bigEndian = JNI_FALSE;
    jlong duration = mfDuration == 0 ? -1 : (jlong)(mfDuration /(float)10000);
    jint bitRate = (jint)mfBitRate;
    jboolean vbr = mfVBR == FALSE ? JNI_FALSE : JNI_TRUE;
    // we always decode to LPCM, that's why the following is true
    jfloat frameRate = sampleRate;

    audioFormat = mf_createAudioFormatObject(env,
        url, sampleRate, sampleSize, channels, frameSize, frameRate, bigEndian, duration, bitRate, vbr);

bail:
    SAFE_RELEASE(pProps);
    SAFE_RELEASE(pMetaProvider);
    SAFE_RELEASE(pMetadataService);
    SAFE_RELEASE(pPD);
    SAFE_RELEASE(pSD);
    SAFE_RELEASE(pMFMetadata);
    SAFE_SHUTDOWN(pMediaSrc);

    return audioFormat;
}

/**
 * Always throws an UnsupportedAudioFileException, because apparently Media Foundation needs
 * some hints about the mime type to properly open a stream.
 */
JNIEXPORT jobject JNICALL Java_com_tagtraum_mfsampledsp_MFAudioFileReader_intGetAudioFormat___3BI(JNIEnv *env, jobject instance, jbyteArray byteArray, jint length) {
    // apparently media foundation needs some hints about the mim type to properly open stream.
    // this is why we leave this unimplemented.
    throwUnsupportedAudioFileExceptionIfError(env, 1, "Guessing AudioFormat from streams is not supported");
    return NULL;
}

