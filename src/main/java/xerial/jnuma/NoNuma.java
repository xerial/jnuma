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
 * A stub when accessing numa API is not supported in the system
 * @author leo
 */
public class NoNuma implements NumaInterface {


    public boolean isAvailable() {
        return false;
    }

    @Override
    public int maxNode() {
        return 1;
    }

    @Override
    public long nodeSize(int node) {
        return Runtime.getRuntime().maxMemory();
    }

    @Override
    public long freeSize(int node) {
        return Runtime.getRuntime().freeMemory();
    }

    @Override
    public int distance(int node1, int node2) {
        return 10;
    }

    @Override
    public void nodeToCpus(int node, long[] buffer) {
    }

    @Override
    public void getAffinity(int pid, long[] cpuBitMask, int numCPUs) {
        // do nothing
    }

    @Override
    public void setAffinity(int pid, long[] cpuBitMask, int numCPUs) {
        // do nothing
    }


    public ByteBuffer allocLocal(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }

    public ByteBuffer allocOnNode(int capacity, int node) {
        return allocLocal(capacity);
    }

    @Override
    public ByteBuffer allocInterleaved(int capacity) {
        return allocLocal(capacity);
    }

    @Override
    public void free(ByteBuffer buf) {
        // Simply clear the buffer and let the GC collect the freed memory
        buf.clear();
    }
}
