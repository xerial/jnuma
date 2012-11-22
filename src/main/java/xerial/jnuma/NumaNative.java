package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author Taro L. Saito
 */
public class NumaNative implements NumaAPI {


    public native ByteBuffer allocLocal(int capacity);

    public native ByteBuffer allocOnNode(int capacity, int node);

    public native void free(ByteBuffer buf);

}

