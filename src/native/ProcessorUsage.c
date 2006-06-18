/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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
#include <sys/times.h>

#include "ProcessorUsage.h"
#include "zjniutil.h"

JNIEXPORT void JNICALL
Java_com_zimbra_znative_ProcessorUsage_getProcessorUsage0(JNIEnv *env, jclass clz, jlongArray jdata)
{
  struct tms tms;
  clock_t wall;
  jlong data[com_zimbra_znative_ProcessorUsage_OFFSET_MAX];

  if ((wall = times(&tms)) == ((clock_t)-1)) {
    char msg[256];
    snprintf(msg, sizeof(msg), "times(): %s", strerror(errno));
    ZimbraThrowOFE(env, msg);
    return;
  }
  
  data[com_zimbra_znative_ProcessorUsage_OFFSET_UTICKS] = tms.tms_utime;
  data[com_zimbra_znative_ProcessorUsage_OFFSET_STICKS] = tms.tms_stime;
  data[com_zimbra_znative_ProcessorUsage_OFFSET_CUTICKS] = tms.tms_cutime;
  data[com_zimbra_znative_ProcessorUsage_OFFSET_CSTICKS] = tms.tms_cstime;
  data[com_zimbra_znative_ProcessorUsage_OFFSET_WTICKS] = wall;

  (*env)->SetLongArrayRegion(env, jdata, 0, com_zimbra_znative_ProcessorUsage_OFFSET_MAX, data);
}
