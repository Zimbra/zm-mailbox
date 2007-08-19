
#include <jni.h>
#include "org_mortbay_setuid_SetUID.h"
#include <sys/types.h>
#include <unistd.h>
  
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
