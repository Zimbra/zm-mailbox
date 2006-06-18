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
#include <sys/time.h>
#include <sys/resource.h>

#include "ResourceUsage.h"
#include "zjniutil.h"

JNIEXPORT void JNICALL
Java_com_zimbra_znative_ResourceUsage_getResourceUsage0(JNIEnv *env, jclass clz, jint who, jlongArray jdata)
{
  struct rusage ru;
  jlong data[com_zimbra_znative_ResourceUsage_RESOURCE_MAX];

  if (getrusage(who, &ru) == -1) {
    char msg[256];
    snprintf(msg, sizeof(msg), "getrusage(): %s", strerror(errno));
    ZimbraThrowOFE(env, msg);
    return;
  }
  
  data[com_zimbra_znative_ResourceUsage_RESOURCE_UTIME] = (ru.ru_utime.tv_sec * 1000) + (ru.ru_utime.tv_usec / 1000);
  data[com_zimbra_znative_ResourceUsage_RESOURCE_STIME] = (ru.ru_stime.tv_sec * 1000) + (ru.ru_stime.tv_usec / 1000);
  data[com_zimbra_znative_ResourceUsage_RESOURCE_MAXRSS] = ru.ru_maxrss;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_IXRSS] = ru.ru_ixrss;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_IDRSS] = ru.ru_idrss;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_ISRSS] = ru.ru_isrss;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_MINFLT] = ru.ru_minflt;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_MAJFLT] = ru.ru_majflt;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_NSWAP] = ru.ru_nswap;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_INBLOCK] = ru.ru_inblock;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_OUBLOCK] = ru.ru_oublock;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_MSGSND] = ru.ru_msgsnd;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_MSGRCV] = ru.ru_msgrcv;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_NSIGNALS] = ru.ru_nsignals;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_NVCSW] = ru.ru_nvcsw;
  data[com_zimbra_znative_ResourceUsage_RESOURCE_NIVCSW] = ru.ru_nivcsw;

  (*env)->SetLongArrayRegion(env, jdata, 0, com_zimbra_znative_ResourceUsage_RESOURCE_MAX, data);
}
