/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

#include <jni.h>

#include "com_zimbra_znative_ProxyInfo.h"

#define UNSUPPORTED_OPERATION "java/lang/UnsupportedOperationException"

static void throwException(JNIEnv *env, const char *clsname, const char *msg) {
    jclass cls = (*env)->FindClass(env, clsname);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_zimbra_znative_ProxyInfo_getProxyInfo(JNIEnv *env, jclass cls, jstring jurl) {
    throwException(env, UNSUPPORTED_OPERATION, "ProxyInfo not supported on this platform");
    return NULL;
}


JNIEXPORT jboolean JNICALL
Java_com_zimbra_znative_ProxyInfo_isSupported(JNIEnv *env, jclass cls) {
    return JNI_FALSE;
}
