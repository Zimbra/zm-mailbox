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
#include <limits.h>
#include <alloca.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "IO.h"
#include "zjniutil.h"

JNIEXPORT void JNICALL 
Java_com_zimbra_znative_IO_link0(JNIEnv *env, 
                               jclass clz,
                               jbyteArray joldpath, 
                               jbyteArray jnewpath)
{
    int oldlen;
    int newlen;
    char *oldpath;
    char *newpath;

    if (joldpath == NULL) {
	ZimbraThrowNPE(env, "IO.link0 oldpath");
	return;
    }

    if (jnewpath == NULL) {
	ZimbraThrowNPE(env, "IO.link0 newpath");
	return;
    }

    oldlen = (*env)->GetArrayLength(env, joldpath);
    if (oldlen <= 0) {
	ZimbraThrowIAE(env, "IO.link0 oldpath length <= 0");
	return;
    }

    newlen = (*env)->GetArrayLength(env, jnewpath);
    if (newlen <= 0) {
	ZimbraThrowIAE(env, "IO.link0 newpath length <= 0");
	return;
    }

    oldpath = alloca(oldlen + 1);   /* +1 for \0 */ 
    memset(oldpath, 0, oldlen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, joldpath, 0, oldlen, (jbyte *)oldpath);

    newpath = alloca(newlen + 1);   /* +1 for \0 */
    memset(newpath, 0, newlen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, jnewpath, 0, newlen, (jbyte *)newpath);

    if (link(oldpath, newpath) == 0) {
	return;
    } else {
        char msg[256];
        snprintf(msg, sizeof(msg), "link(%s, %s): %s", oldpath, newpath, 
                 strerror(errno));
        ZimbraThrowIOE(env, msg);
    }
}

JNIEXPORT jint JNICALL Java_com_zimbra_znative_IO_linkCount0
(JNIEnv *env, jclass clz, jbyteArray jpath)
{
    struct stat sb;
    int len;
    char *path;

    if (jpath == NULL) {
        ZimbraThrowNPE(env, "IO.linkCount0 path");
        return -1; /* retval ignored by on exception */
    }

    len = (*env)->GetArrayLength(env, jpath);
    if (len <= 0) {
        ZimbraThrowIAE(env, "IO.linkCount0 path length <= 0");
        return -1;
    }

    path = alloca(len + 1);      /* +1 for \0 */
    memset(path, 0, len + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, jpath, 0, len, (jbyte *)path);

    if (stat(path, &sb) == 0) {
        return (jint)sb.st_nlink;
    } else {
        char msg[256];
        snprintf(msg, sizeof(msg), "stat(%s): %s", path, strerror(errno));
        ZimbraThrowIOE(env, msg);
        return -1;
    }
} 
