/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2010 Zimbra, Inc.
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
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <stdlib.h>
#include <grp.h>

#include "Process.h"
#include "zjniutil.h"

JNIEXPORT jint JNICALL
Java_com_zimbra_znative_Process_getuid0(JNIEnv *env, jclass clz)
{
    return getuid();
}

JNIEXPORT jint JNICALL
Java_com_zimbra_znative_Process_geteuid0(JNIEnv *env, jclass clz)
{
    return geteuid();
}

JNIEXPORT jint JNICALL
Java_com_zimbra_znative_Process_getgid0(JNIEnv *env, jclass clz)
{
    return getgid();
}

JNIEXPORT jint JNICALL
Java_com_zimbra_znative_Process_getegid0(JNIEnv *env, jclass clz)
{
    return getegid();
}

static void
SetPrivileges(JNIEnv *env, const char *username, uid_t uid, gid_t gid)
{
    if (geteuid() != 0) {
        /* Nothing to do - we are not running as root. */
        return;
    }

    if (setgid(gid) == -1) {
        char msg[256];
        snprintf(msg, sizeof(msg), "setgid(%d): %s", gid, strerror(errno));
        ZimbraThrowOFE(env, msg);
        return;
    }
    
    if (initgroups(username, gid) == -1) {
        char msg[256];
        snprintf(msg, sizeof(msg), "initgroups(%s, %d): %s", username, gid, 
                 strerror(errno));
        ZimbraThrowOFE(env, msg);
        return;
    }

    if (setuid(uid) == -1) {
        char msg[256];
        snprintf(msg, sizeof(msg), "setuid(%d): %s", uid, strerror(errno));
        ZimbraThrowOFE(env, msg);
        return;
    }
}

JNIEXPORT void JNICALL
Java_com_zimbra_znative_Process_setPrivileges0(JNIEnv *env, jclass clz,
                                               jbyteArray jusername,
                                               jint uid,
                                               jint gid)
{
    int length;
    char *username;

    if (jusername == NULL) {
        ZimbraThrowNPE(env, "Process.setPrivileges0 username");
        return;
    }
    
    length = (*env)->GetArrayLength(env, jusername);
    if (length <= 0) {
        ZimbraThrowIAE(env, "Process.setPrivileges0 username length <= 0");
        return;
    }
    
    username = (char *)calloc(length + 1, 1);   /* +1 for \0 */
    if (username == NULL) {
        ZimbraThrowIAE(env, "Process.setPrivileges0 username malloc failed");
        return;
    } 
    (*env)->GetByteArrayRegion(env, jusername, 0, length, (jbyte *)username);

    SetPrivileges(env, username, uid, gid);

    free(username);
}
