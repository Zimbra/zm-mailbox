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
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/times.h>

#include "com_zimbra_znative_Util.h"
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
