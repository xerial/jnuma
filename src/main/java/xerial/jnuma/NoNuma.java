package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author leo
 */
public class NoNuma implements NumaAPI {

    public ByteBuffer allocLocal(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }

    public ByteBuffer allocOnNode(int capacity, int node) {
        return ByteBuffer.allocateDirect(capacity);
    }

    @Override
    public void free(ByteBuffer buf) {
        // do nothing
    }
}
