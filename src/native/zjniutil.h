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

#ifndef _ZIMBRA_NATIVE_UTIL_H
#define _ZIMBRA_NATIVE_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

void
ZimbraThrowNPE(JNIEnv *env, const char *msg);

void
ZimbraThrowIAE(JNIEnv *env, const char *msg);

void
ZimbraThrowIOE(JNIEnv *env, const char *msg);

void
ZimbraThrowOFE(JNIEnv *env, const char *msg);

#ifdef __cplusplus
}
#endif
#endif
