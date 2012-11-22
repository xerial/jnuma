#include <numa.h>
#define _GNU_SOURCE
#include <sched.h>
#include <stdio.h>
#include "NumaNative.h"


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

JNIEXPORT jint JNICALL Java_xerial_jnuma_NumaNative_nodeToCpus
  (JNIEnv *env, jobject obj, jint node, jlongArray array, jint len) {

   unsigned long* buf = (unsigned long*) (*env)->GetPrimitiveArrayCritical(env, (jarray) array, 0);
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



JNIEXPORT jint JNICALL Java_xerial_jnuma_NumaNative_currentCpu
  (JNIEnv *env, jobject obj) {
     return sched_getcpu();

  }
