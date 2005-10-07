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
