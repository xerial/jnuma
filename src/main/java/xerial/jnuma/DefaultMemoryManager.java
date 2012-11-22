package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author leo
 */
public class DefaultMemoryManager implements NumaAPI {
    @Override
    public ByteBuffer numaAlloc(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }
}
