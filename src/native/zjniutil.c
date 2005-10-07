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
