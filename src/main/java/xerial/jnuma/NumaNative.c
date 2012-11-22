#include <numa.h>
#include <stdio.h>
#include "NumaNative.h"

JNIEXPORT jobject JNICALL Java_xerial_jnuma_NumaNative_allocLocal
  (JNIEnv *env, jobject jobj, jint capacity)
{
   void* mem = numa_alloc_local((size_t) capacity);
   //printf("allocate local memory\n");
   if(mem == NULL)
     printf("failed to allocate local memory\n");
   return (*env)->NewDirectByteBuffer(env, mem, (jlong) capacity);
}

JNIEXPORT jobject JNICALL Java_xerial_jnuma_NumaNative_allocOnNode
  (JNIEnv *env, jobject jobj, jint capacity, jint node)
  {
   jobject b;
   void* mem = numa_alloc_onnode((size_t) capacity, (int) node);
   if(mem == NULL)
     printf("failed to allocate memory on node %d\n", (int) node);
   b = (*env)->NewDirectByteBuffer(env, mem, (jlong) capacity);
   return b;
  }

JNIEXPORT void JNICALL Java_xerial_jnuma_NumaNative_free
  (JNIEnv *env, jobject jobj, jobject buf) {
   //printf("free is called\n");
   void* mem = (*env)->GetDirectBufferAddress(env, buf);
   jlong capacity = (*env)->GetDirectBufferCapacity(env, buf);
   if(mem != 0) {
     //printf("free capacity:%d\n", capacity);
     numa_free(mem, (size_t) capacity);
   }
  }
