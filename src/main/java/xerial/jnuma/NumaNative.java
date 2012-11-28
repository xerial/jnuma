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

package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * Native code interface. Do not describe this class name in an import statement or source codes, because it will break
 * the native code loading mechanism. Use static methods in {@link xerial.jnuma.Numa} to access NUMA.
 *
 * @author Taro L. Saito
 */
public class NumaNative implements NumaInterface {

    public native boolean isAvailable();
    public native int maxNode();
    public native long nodeSize(int node);
    public native long freeSize(int node);
    public native int distance(int node1, int node2);
    public native void nodeToCpus(int node, long[] buffer);
    public native void getAffinity(int pid, long[] cpuBitMask, int numCPUs);
    public native void setAffinity(int pid, long[] cpuBitMask, int numCPUs);

    public native int preferredNode();
    public native void setLocalAlloc();

    public native void setPreferred(int node);
    public native void runOnNode(int node);

    public native ByteBuffer alloc(int capacity);
    public native ByteBuffer allocLocal(int capacity);
    public native ByteBuffer allocOnNode(int capacity, int node);
    public native ByteBuffer allocInterleaved(int capacity);

    public native void free(ByteBuffer buf);


    private void throwError(int errorCode) throws Exception {
        throw new Exception(String.format("NUMA error occurred %d", errorCode));
    }

}

