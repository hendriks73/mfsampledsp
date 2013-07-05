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

#define WINVER _WIN32_WINNT_WIN7

#include <windows.h>
#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <stdio.h>
#include <mferror.h>
#include <winerror.h>
#include <jni.h>
#include <propvarutil.h>

#define SAFE_RELEASE(ptr) if(ptr) { ptr->Release(); ptr = NULL; }
#define SAFE_SHUTDOWN(ptr) if(ptr) { ptr->Shutdown(); ptr->Release(); ptr = NULL; }

/**
 * Struct holding JNI env and the open Media Source Reader.
 */
struct MFAudioIO
{
    JNIEnv *                    env;
    IMFSourceReader *           mediaSrcReader;
};


void throwUnsupportedAudioFileExceptionIfError(JNIEnv *, int, const char*);

void throwIOExceptionIfError(JNIEnv *, int, const char*);

void throwIllegalArgumentExceptionIfError(JNIEnv *, int, const char *);

void throwFileNotFoundExceptionIfError(JNIEnv *, int, const char *);

HRESULT mf_createMediaSource(JNIEnv *, jstring, IMFMediaSource **);

HRESULT mf_createMediaSourceReader(JNIEnv *, jstring, IMFSourceReader **);

jobject mf_createAudioFormatObject(JNIEnv*, jstring, jfloat, jint,
                                         jint, jint, jfloat, jboolean, jlong,
                                         jint, jboolean);
    
