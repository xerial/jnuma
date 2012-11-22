package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * NUMA API Interface
 * @author leo
 */
public interface NumaAPI {

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


    /**
     * Release the numa buffer
     * @param buf
     */
    public void free(ByteBuffer buf);

}
