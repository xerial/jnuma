package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author Taro L. Saito
 */
public class NumaNative implements NumaAPI {

    public native boolean isAvailable();
    public native int maxNode();
    public native long nodeSize(int node);
    public native long freeSize(int node);
    public native int distance(int node1, int node2);
    public native int nodeToCpus(int node, long[] buffer, int bufferLen);

    public native ByteBuffer allocLocal(int capacity);

    public native ByteBuffer allocOnNode(int capacity, int node);
    public native ByteBuffer allocInterleaved(int capacity);

    public native void free(ByteBuffer buf);

}

