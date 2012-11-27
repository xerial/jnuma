/*
 * Copyright 2012 Taro L. Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#define _GNU_SOURCE
#include <numa.h>
#include <sched.h>
#include <stdio.h>
#include <stdint.h>
#include "NumaNative.h"


void throwException(JNIEnv *env, jobject self, int errorCode) {
   jclass c = (*env)->FindClass(env, "xerial/jnuma/NumaNative");
   if(c == 0)
     return;

   jmethodID m = (*env)->GetMethodID(env, c, "throwErro", "(I)V");
   if(m == 0)
     return;

   (*env)->CallVoidMethod(env, self, m, (jint) errorCode);
}


/*
 * Class:     xerial_jnuma_NumaNative
 * Method:    numaAvailable
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_xerial_jnuma_NumaNative_isAvailable
  (JNIEnv *env, jobject obj) {
     return numa_available() != -1;
  }

/*
 * Class:     xerial_jnuma_NumaNative
 * Method:    maxNode
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_xerial_jnuma_NumaNative_maxNode
  (JNIEnv *env, jobject obj) {
    return numa_max_node();
  }

/*
 * Class:     xerial_jnuma_NumaNative
 * Method:    nodeSize
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_xerial_jnuma_NumaNative_nodeSize
  (JNIEnv *env, jobject obj, jint node) {
     return (jlong) numa_node_size((int) node, NULL);
  }

/*
 * Class:     xerial_jnuma_NumaNative
 * Method:    freeSize
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_xerial_jnuma_NumaNative_freeSize
  (JNIEnv *env, jobject obj, jint node) {
     long free = 0;
     numa_node_size((int) node, &free);
     return free;
  }


/*
 * Class:     xerial_jnuma_NumaNative
 * Method:    distance
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_xerial_jnuma_NumaNative_distance
  (JNIEnv *env, jobject obj, jint node1, jint node2) {
  return numa_distance(node1, node2);
  }

JNIEXPORT void JNICALL Java_xerial_jnuma_NumaNative_nodeToCpus
  (JNIEnv *env, jobject obj, jint node, jlongArray array) {

   unsigned long* buf = (unsigned long*) (*env)->GetPrimitiveArrayCritical(env, (jarray) array, 0);
   jsize len = (size_t) (*env)->GetArrayLength(env, array);
   int ret = numa_node_to_cpus(node, buf, len);
   (*env)->ReleasePrimitiveArrayCritical(env, (jarray) array, buf, 0);
   return ret;
  }


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

JNIEXPORT jobject JNICALL Java_xerial_jnuma_NumaNative_allocInterleaved
  (JNIEnv *env, jobject obj, jint capacity) {
   jobject b;
   void* mem = numa_alloc_interleaved((size_t) capacity);
   if(mem == NULL)
     printf("failed to allocate interleaved memory\n");
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


JNIEXPORT void JNICALL Java_xerial_jnuma_NumaNative_getAffinity
  (JNIEnv *env, jobject obj, jint pid, jlongArray maskBuf, jint numCPUs) {

  uint64_t* in = (uint64_t*) (*env)->GetPrimitiveArrayCritical(env, (jarray) maskBuf, 0);
  cpu_set_t mask;
  int i;
  if(in == 0)
    throwException(env, obj, 10);

  CPU_ZERO(&mask);
  int ret = sched_getaffinity(0, sizeof(mask), &mask);
  if(ret < 0)
    return;

  for(i=0; i<numCPUs; ++i)
     if(CPU_ISSET(i, &mask))
       in[i / 64] |= (uint64_t) (((uint64_t) 1) << (i % 64));

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) maskBuf, (void*) in, (jint) 0);
}


JNIEXPORT void JNICALL Java_xerial_jnuma_NumaNative_setAffinity
  (JNIEnv *env, jobject obj, jint pid, jlongArray maskBuf, jint numCPUs) {

  uint64_t* in = (uint64_t*) (*env)->GetPrimitiveArrayCritical(env, (jarray) maskBuf, 0);
  cpu_set_t mask;
  int i;

  CPU_ZERO(&mask);
  for(i=0; i<numCPUs; ++i)
    if(in[i / 64] & ((uint64_t) 1 << (i % 64)))
     CPU_SET((int) i, &mask);

  (*env)->ReleasePrimitiveArrayCritical(env, (jarray) maskBuf, (void*) in, (jint) 0);

  int ret = sched_setaffinity(0, sizeof(cpu_set_t), &mask);
  if(ret < 0)
    throwException(env, obj, 11);
}
