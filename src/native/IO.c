/* -*- Mode: c; c-basic-offset: 4 -*- */
#include <jni.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <limits.h>
#include <alloca.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "IO.h"

static void
ThrowNPE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/NullPointerException");

    if (cls != 0) /* Otherwise an exception has already been thrown */
	(*env)->ThrowNew(env, cls, msg);
}

static void
ThrowIAE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");

    if (cls != 0) /* Otherwise an exception has already been thrown */
	(*env)->ThrowNew(env, cls, msg);
}

static void
ThrowIOE(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/io/IOException");
    if (cls != 0) /* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
}

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
	ThrowNPE(env, "oldpath");
	return;
    }

    if (jnewpath == NULL) {
	ThrowNPE(env, "newpath");
	return;
    }

    oldlen = (*env)->GetArrayLength(env, joldpath);
    if (oldlen <= 0) {
	ThrowIAE(env, "oldpath");
	return;
    }

    newlen = (*env)->GetArrayLength(env, jnewpath);
    if (newlen <= 0) {
	ThrowIAE(env, "newpath");
	return;
    }

    oldpath = alloca(oldlen + 1);   /* +1 for \0 */ 
    memset(oldpath, 0, oldlen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, joldpath, 0, oldlen, oldpath);

    newpath = alloca(newlen + 1);   /* +1 for \0 */
    memset(newpath, 0, newlen + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, jnewpath, 0, newlen, newpath);

    if (link(oldpath, newpath) == 0) {
	return;
    } else {
        char msg[256];
        snprintf(msg, sizeof(msg), "link(%s, %s): %s", oldpath, newpath, 
                 strerror(errno));
        ThrowIOE(env, msg);
    }
}

JNIEXPORT jint JNICALL Java_com_zimbra_znative_IO_linkCount0
(JNIEnv *env, jclass clz, jbyteArray jpath)
{
    struct stat sb;
    int len;
    char *path;

    if (jpath == NULL) {
        ThrowNPE(env, "path");
        return -1; /* retval ignored by on exception */
    }

    len = (*env)->GetArrayLength(env, jpath);
    if (len <= 0) {
        ThrowIAE(env, "path");
        return -1;
    }

    path = alloca(len + 1);      /* +1 for \0 */
    memset(path, 0, len + 1); /* +1 for \0 */
    (*env)->GetByteArrayRegion(env, jpath, 0, len, path);

    if (stat(path, &sb) == 0) {
        return (jint)sb.st_nlink;
    } else {
        char msg[256];
        snprintf(msg, sizeof(msg), "stat(%s): %s", path, strerror(errno));
        ThrowIOE(env, msg);
        return -1;
    }
} 
