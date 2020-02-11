/* -*- Mode: c; c-basic-offset: 4 -*- */
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
#include <errno.h>
#include <string.h>
#include <sys/times.h>

#include "com_zimbra_znative_ProcessorUsage.h"
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
