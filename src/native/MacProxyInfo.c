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

#include <CoreServices/CoreServices.h>
#include <alloca.h>
#include <jni.h>

#include "ProxyInfo.h"

#define JLS "Ljava/lang/String;"

// Must match enum values in ProxyInfo.Type

#define TYPE_NONE   0
#define TYPE_AUTO_CONFIG_URL 1
#define TYPE_FTP    2
#define TYPE_HTTP   3
#define TYPE_HTTPS  4
#define TYPE_SOCKS  5
#define TYPE_UNKNOWN 6

static jclass pi_cls;
static jmethodID pi_ctrID;

static void check_initialized(JNIEnv *env) {
    static int initialized;
    if (!initialized) {
        pi_cls = (*env)->FindClass(env, "com/zimbra/znative/ProxyInfo");
        pi_cls = (*env)->NewGlobalRef(env, pi_cls);
        pi_ctrID = (*env)->GetMethodID(env, pi_cls, "<init>", "(I" JLS "I" JLS JLS ")V");
    }
}

static CFStringRef getCFString(JNIEnv *env, jstring js) {
    const jchar *chars = (*env)->GetStringChars(env, js, NULL);
    jint len = (*env)->GetStringLength(env, js);
    CFStringRef cs = CFStringCreateWithCharacters(NULL, chars, len);
    (*env)->ReleaseStringChars(env, js, chars);
    return cs;
}

static jstring getJString(JNIEnv *env, CFStringRef cs) {
    if (cs == NULL) return NULL;
    CFRange range = CFRangeMake(0, CFStringGetLength(cs));
    UniChar *chars = alloca(range.length);
    CFStringGetCharacters(cs, range, chars);
    return (*env)->NewString(env, (jchar *) chars, (jsize) range.length);
}

static jint getJInt(CFNumberRef number) {
    if (number != NULL) {
        jint jnumber;
        if (CFNumberGetValue(number, kCFNumberSInt32Type, &jnumber)) {
            return jnumber;
        }
    }
    return -1;
}

static jint getProxyType(CFStringRef type) {
    if (type == kCFProxyTypeNone)
        return TYPE_NONE;
    if (type == kCFProxyTypeAutoConfigurationURL)
        return TYPE_AUTO_CONFIG_URL;
    if (type == kCFProxyTypeFTP)
        return TYPE_FTP;
    if (type == kCFProxyTypeHTTP)
        return TYPE_HTTP;
    if (type == kCFProxyTypeHTTPS)
        return TYPE_HTTPS;
    if (type == kCFProxyTypeSOCKS)
        return TYPE_SOCKS;
    return TYPE_UNKNOWN;
}

static jobject getProxyInfo(JNIEnv *env, CFDictionaryRef proxy) {
    CFStringRef type = CFDictionaryGetValue(proxy, kCFProxyTypeKey);
    CFStringRef host = CFDictionaryGetValue(proxy, kCFProxyHostNameKey);
    CFNumberRef port = CFDictionaryGetValue(proxy, kCFProxyPortNumberKey);
    CFStringRef user = CFDictionaryGetValue(proxy, kCFProxyUsernameKey);
    CFStringRef pass = CFDictionaryGetValue(proxy, kCFProxyPasswordKey);
    return (*env)->NewObject(
        env, pi_cls, pi_ctrID, getProxyType(type), getJString(env, host),
        getJInt(port), getJString(env, user), getJString(env, pass));
}

JNIEXPORT jobjectArray JNICALL
Java_com_zimbra_znative_ProxyInfo_getProxyInfo(JNIEnv *env, jclass cls, jstring jurl) {
    check_initialized(env);
    CFStringRef urlstr = getCFString(env, jurl);
    CFURLRef url = CFURLCreateWithString(NULL, urlstr, NULL);
    CFDictionaryRef systemProxy = CFNetworkCopySystemProxySettings();
    CFArrayRef proxyArray = CFNetworkCopyProxiesForURL(url, systemProxy);
    CFIndex size = CFArrayGetCount(proxyArray);
    jobjectArray results = (*env)->NewObjectArray(env, (jsize) size, pi_cls, NULL);
    int i;
    for (i = 0; i < size; i++) {
        CFDictionaryRef proxy = CFArrayGetValueAtIndex(proxyArray, i);
        (*env)->SetObjectArrayElement(env, results, i, getProxyInfo(env, proxy));
    }
    CFRelease(proxyArray);
    CFRelease(systemProxy);
    CFRelease(url);
    CFRelease(urlstr);
    return results;
}

JNIEXPORT jboolean JNICALL
Java_com_zimbra_znative_ProxyInfo_isSupported(JNIEnv *env, jclass cls) {
    return JNI_TRUE;
}
