package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * NUMA API Interface
 * @author leo
 */
public interface NumaAPI {

    /**
     * Allocate a new ByteBuffer
     * @param capacity
     * @return
     */
    public ByteBuffer numaAlloc(int capacity);

}
