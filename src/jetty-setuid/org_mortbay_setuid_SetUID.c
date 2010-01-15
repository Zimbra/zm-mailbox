/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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
#include <sys/resource.h>
#include <errno.h>
  
/*  Start of Helper functions Declaration */
jmethodID getJavaMethodId(JNIEnv *env, jclass clazz, const char *name, const char *sig);
void setJavaFieldInt(JNIEnv *env, jobject obj, const char *name, int value);
void throwNewJavaException(JNIEnv *env, const char *name, const char *msg);
void throwNewJavaSecurityException(JNIEnv *env, const char *msg);
int getJavaFieldInt(JNIEnv *env, jobject obj, const char *name);
/* End of Helper functions Declaration */


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


/*
 * Class:     org_mortbay_setuid_SetUID
 * Method:    getrlimitnofiles
 * Signature: ()Lorg/mortbay/setuid/RLimit;
 */
JNIEXPORT jobject JNICALL Java_org_mortbay_setuid_SetUID_getrlimitnofiles
  (JNIEnv *env, jclass j)
{
    struct rlimit rlim;
    int success = getrlimit(RLIMIT_NOFILE, &rlim);
    if (success < 0)
    {
        throwNewJavaSecurityException(env, "getrlimit failed");
        return NULL;
    }

    // get The java class org.mortbay.setuid.RLimit
    jclass cls = (*env)->FindClass(env, "org/mortbay/setuid/RLimit");
    if(!cls)
    {
        throwNewJavaSecurityException(env, "Class: org.mortbay.setuid.RLimit is not found!!!");
        return NULL;
    }
    
    // get the default constructor  of org.mortbay.setuid.RLimit
    jmethodID constructorMethod = getJavaMethodId(env, cls, "<init>", "()V");
    
    // construct org.mortbay.setuid.RLimit java object
    jobject retVal = (*env)->NewObject(env, cls,constructorMethod);
    if(!retVal)
    {
        throwNewJavaSecurityException(env, "Object Construction Error of Class: org.mortbay.setuid.RLimit!!!");
        return NULL;
    }
    setJavaFieldInt(env, retVal, "_soft", rlim.rlim_cur);
    setJavaFieldInt(env, retVal, "_hard", rlim.rlim_max);

    (*env)->DeleteLocalRef(env, cls);
    return retVal;
}

/*
 * Class:     org_mortbay_setuid_SetUID
 * Method:    setrlimitnofiles
 * Signature: (Lorg/mortbay/setuid/RLimit;)I
 */
JNIEXPORT jint JNICALL Java_org_mortbay_setuid_SetUID_setrlimitnofiles
  (JNIEnv *env, jclass j, jobject jo)
{
    struct rlimit rlim;

    jclass cls = (*env)->FindClass(env, "org/mortbay/setuid/RLimit");
    rlim.rlim_cur=getJavaFieldInt(env,jo, "_soft");
    rlim.rlim_max=getJavaFieldInt(env,jo, "_hard");
    int success = setrlimit(RLIMIT_NOFILE, &rlim);
    (*env)->DeleteLocalRef(env, cls);
    return (jint)success;  
}

/*  Start of Helper Functions Implimentations */

jmethodID getJavaMethodId(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
    jmethodID methodId = (*env)->GetMethodID(env, clazz,name,sig);
    if(!methodId)
    {
        char strErr[255];
        sprintf(strErr, "Java Method is not found: %s !!!", name);
        throwNewJavaSecurityException(env, strErr);
        return NULL;
    }
    
    return methodId;

}

int getJavaFieldInt(JNIEnv *env, jobject obj, const char *name)
{
    jclass clazz = (*env)->GetObjectClass(env, obj);
    jfieldID fieldId =  (*env)->GetFieldID(env, clazz, name, "I");
    if(!fieldId)
    {
        char strErr[255];
        sprintf(strErr, "Java Object Field is not found: int %s !!!", name);
        throwNewJavaSecurityException(env, strErr);
    }
    int val = (*env)->GetIntField(env, obj, fieldId);
    (*env)->DeleteLocalRef(env, clazz);
    return val;
}

void setJavaFieldInt(JNIEnv *env, jobject obj, const char *name, int value)
{
    jclass clazz = (*env)->GetObjectClass(env, obj);

    jfieldID fieldId =  (*env)->GetFieldID(env, clazz, name, "I");
    if(!fieldId)
    {
        char strErr[255];
        sprintf(strErr, "Java Object Field is not found: int %s !!!", name);
        throwNewJavaSecurityException(env, strErr);
    }
    
    (*env)->SetIntField(env, obj, fieldId, value);
    (*env)->DeleteLocalRef(env, clazz);
}
void throwNewJavaException(JNIEnv *env, const char *name, const char *msg)
{
    jclass clazz = (*env)->FindClass(env, name);
    if (clazz) 
    {
        (*env)->ThrowNew(env, clazz, msg);
    }
    (*env)->DeleteLocalRef(env, clazz);
}

void throwNewJavaSecurityException(JNIEnv *env, const char *msg)
{
    throwNewJavaException(env, "java/lang/SecurityException", msg);
}



/*  End of Helper Functions Implimentations */
