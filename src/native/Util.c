/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

#include <jni.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/times.h>

#include "Util.h"
#include "zjniutil.h"

JNIEXPORT jlong JNICALL
Java_com_zimbra_znative_Util_getTicksPerSecond0(JNIEnv *env, jclass clz)
{
  long tps = sysconf(_SC_CLK_TCK);
  if (tps == -1) {
    char msg[256];
    snprintf(msg, sizeof(msg), "times(): %s", strerror(errno));
    ZimbraThrowOFE(env, msg);
    return -1;
  }
  return tps;
} 
