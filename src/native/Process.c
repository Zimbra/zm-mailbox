/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
#include <jni.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <alloca.h>
#include <sys/types.h>

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
SetPrivileges(const char *user, const char *group)
{
}

JNIEXPORT void JNICALL
Java_com_zimbra_znative_Process_setPrivileges0(JNIEnv *env, jclass clz,
                                               jbyteArray juser,
                                               jbyteArray jgroup)
{
    int userlen;
    int grouplen;
    char *user;
    char *group;

    if (juser == NULL) {
        ZimbraThrowNPE(env, "Process.setPrivileges0 user");
        return;
    }
    
    if (jgroup == NULL) {
        ZimbraThrowNPE(env, "Process.setPrivileges0 group");
        return;
    }

    userlen = (*env)->GetArrayLength(env, juser);
    if (userlen <= 0) {
        ZimbraThrowIAE(env, "Process.setPrivileges0 user length <= 0");
        return;
    }
    
    grouplen = (*env)->GetArrayLength(env, jgroup);
    if (grouplen <= 0) {
        ZimbraThrowIAE(env, "Process.setPrivileges0 group length <= 0");
        return;
    }

    user = alloca(userlen + 1);   /* +1 for \0 */
    memset(user, 0, userlen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, juser, 0, userlen, (jbyte *)user);

    group = alloca(grouplen + 1);   /* +1 for \0 */
    memset(group, 0, grouplen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, jgroup, 0, grouplen, (jbyte *)group);

    SetPrivileges(user, group);
}

