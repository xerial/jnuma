package xerial.jnuma;

import java.io.*;
import java.nio.ByteBuffer;

/**
 *
 *
 * @author Taro L. Saito
 */
public class Numa {

    private static NumaAPI impl = init();

    private static NumaAPI init() {
        if(System.getProperty("os.name", "").contains("Windows"))
            return new DefaultMemoryManager();
        else {
            // Extract the native lib to temp folder
            try {
                InputStream in = Numa.class.getResourceAsStream("/xerial/jnuma/native/libjnuma.so");
                File libFile = new File(System.getProperty("java.io.tmpdir") + "/libjnuma.so");
                OutputStream out = new BufferedOutputStream(new FileOutputStream(libFile));
                byte[] buf = new byte[8192];
                for(int ret = 0; (ret = in.read(buf)) != -1;) {
                    out.write(buf, 0, ret);
                }
                in.close();
                out.close();
                System.load(libFile.getAbsolutePath());
                return new NumaNative();
            }
            catch(Exception e) {
                e.printStackTrace();
                return new DefaultMemoryManager();
            }
        }
    }

    public static ByteBuffer numaAlloc(int capacity) {
        return impl.numaAlloc(capacity);
    }

    public static void main(String[] args) {
        System.out.println("Hello NUMA!");
        numaAlloc(1024);
    }

}
