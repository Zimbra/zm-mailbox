/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

#include <jni.h>

#include "ProxyInfo.h"

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
