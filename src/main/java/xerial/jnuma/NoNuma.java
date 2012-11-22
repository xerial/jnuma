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
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long nodeSize(int node) {
        return 0;
    }

    @Override
    public long freeSize(int node) {
        return 0;
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
