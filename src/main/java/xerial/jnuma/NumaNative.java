package xerial.jnuma;

import java.nio.ByteBuffer;

/**
 * @author Taro L. Saito
 */
public class NumaNative implements NumaAPI {

    public native ByteBuffer numaAlloc(int capacity);

}

