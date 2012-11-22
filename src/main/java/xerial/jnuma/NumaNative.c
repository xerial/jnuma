#include <numa.h>
#include <stdio.h>
#include "NumaNative.h"

JNIEXPORT jobject JNICALL Java_xerial_jnuma_NumaNative_numaAlloc
  (JNIEnv *env, jobject jobj, jint capacity)
{
   void* mem = numa_alloc_local((size_t) capacity);
   return (*env)->NewDirectByteBuffer(env, mem, (jlong) capacity);
}

