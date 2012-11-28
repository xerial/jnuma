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
 * NUMA API Interface. This interface is used when loading native class in the root class loader, so you are not allowed to use
 * this class. If you describe this class in an import statement or source codes, loading the native class ({@link xerial.jnuma.Numa}) will fail.
 * @author leo
 */
public interface NumaInterface {

    public boolean isAvailable();

    public int maxNode();

    public long nodeSize(int node);

    public long freeSize(int node);

    public int distance(int node1, int node2);
    public void nodeToCpus(int node, long[] buffer);
    public void getAffinity(int pid, long[] cpuBitMask, int numCPUs);
    public void setAffinity(int pid, long[] cpuBitMask, int numCPUs);

    public int preferredNode();
    public void setLocalAlloc();

    public void setPreferred(int node);
    public void runOnNode(int node);

    //public void bind(long[] nodeMask);

    public ByteBuffer alloc(int capacity);
    /**
     * Allocate a new ByteBuffer on local NUMA node
     * @param capacity
     * @return
     */
    public ByteBuffer allocLocal(int capacity);

    /**
     * Allocate a new ByteBuffer on the specified NUMA node
     * @param capacity
     * @param node
     * @return
     */
    public ByteBuffer allocOnNode(int capacity, int node);
    public ByteBuffer allocInterleaved(int capacity);

    /**
     * Release the numa buffer
     * @param buf
     */
    public void free(ByteBuffer buf);

}
