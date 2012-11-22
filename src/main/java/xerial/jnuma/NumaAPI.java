package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * NUMA API Interface
 * @author leo
 */
public interface NumaAPI {

    public boolean isAvailable();

    public int maxNode();

    public long nodeSize(int node);

    public long freeSize(int node);

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
