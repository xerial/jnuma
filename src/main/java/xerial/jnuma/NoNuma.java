package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author leo
 */
public class NoNuma implements NumaAPI {


    public boolean isAvailable() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int maxNode() {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long nodeSize(int node) {
        return -1;
    }

    @Override
    public long freeSize(int node) {
        return -1;
    }

    @Override
    public int distance(int node1, int node2) {
        return -1;
    }

    @Override
    public int nodeToCpus(int node, long[] buffer, int bufferLen) {
        return -1;
    }

    @Override
    public void getAffinity(int pid, byte[] cpuBitMask, int maskLen) {
        throw new UnsupportedOperationException("getAffinity");
    }

    @Override
    public void setAffinity(int pid, int cpu) {
        throw new UnsupportedOperationException("setAffinity");
    }

    public int currentCpu() {
        return -1;
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
        // Do nothing. Let the GC collect the freed buffer
    }
}
