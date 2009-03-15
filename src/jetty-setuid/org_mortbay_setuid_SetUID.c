/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

#include <jni.h>
#include "org_mortbay_setuid_SetUID.h"
#include <sys/types.h>
#include <unistd.h>
  
JNIEXPORT jint JNICALL 
Java_org_mortbay_setuid_SetUID_setuid (JNIEnv * jnienv, jclass j, jint uid)
{
    return((jint)setuid((uid_t)uid));
}

JNIEXPORT jint JNICALL 
Java_org_mortbay_setuid_SetUID_setumask (JNIEnv * jnienv, jclass j, jint mask)
{
    return((jint)umask((mode_t)mask));
}
  
JNIEXPORT jint JNICALL 
Java_org_mortbay_setuid_SetUID_setgid (JNIEnv * jnienv, jclass j, jint gid)
{
    return((jint)setgid((gid_t)gid));
}
