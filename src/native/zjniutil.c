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
#include "zjniutil.h"

void
ZimbraThrowNPE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/NullPointerException");

    if (cls != 0) /* Otherwise an exception has already been thrown */
	(*env)->ThrowNew(env, cls, msg);
}

void
ZimbraThrowIAE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");

    if (cls != 0) /* Otherwise an exception has already been thrown */
	(*env)->ThrowNew(env, cls, msg);
}

void
ZimbraThrowIOE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/io/IOException");
    if (cls != 0) /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
}

void
ZimbraThrowOFE(JNIEnv *env, const char  *msg)
{
    jclass cls = (*env)->FindClass(env, "com/zimbra/znative/OperationFailedException");
    if (cls != 0) /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
}
