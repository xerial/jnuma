package xerial.jnuma;


import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 *
 *
 * @author Taro L. Saito
 */
public class Numa {

    private static Object impl = init();

    private static ClassLoader rootClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while(cl.getParent() != null) {
            cl = cl.getParent();
        }
        return cl;
    }

    private static byte[] loadByteCode(String className) throws IOException {
        return loadResource(Numa.class.getResourceAsStream(String.format("/%s.class", className.replaceAll("\\.", "/"))));
    }

    private static byte[] loadResource(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for(int ret = 0; (ret = in.read(buf)) != -1;) {
            out.write(buf, 0, ret);
        }
        in.close();
        out.close();
        return out.toByteArray();
    }


    private static void inject(String[] classNames, ClassLoader cl) throws Exception {

        Class<?> clClass = Class.forName("java.lang.ClassLoader");

        Method m = clClass.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class, ProtectionDomain.class });
        m.setAccessible(true);
        for(String c : classNames) {

            try {
                cl.loadClass(c);
            }
            catch(ClassNotFoundException e) {
                byte[] byteCode = loadByteCode(c);
                m.invoke(cl, c, byteCode, 0, byteCode.length, System.class.getProtectionDomain());
            }
        }

    }




    private static Object init() {
        if(System.getProperty("os.name", "").contains("Windows"))
            return new NoNuma();
        else {
            // Extract the native lib to temp folder
            try {
                File libFile = new File(System.getProperty("java.io.tmpdir") + "/libjnuma.so");
                byte[] newLib = loadResource(Numa.class.getResourceAsStream("/xerial/jnuma/native/libjnuma.so"));
                if(!libFile.exists() || (libFile.exists() && !Arrays.equals(loadResource(new FileInputStream(libFile)), newLib))) {
                    FileOutputStream out = new FileOutputStream(libFile);
                    out.write(newLib);
                    out.close();
                }

                String loader = "xerial.jnuma.NumaJNILoader";
                String nativeAPI = "xerial.jnuma.NumaNative";
                inject(new String[]{loader, "xerial.jnuma.NumaAPI", "xerial.jnuma.NumaNative"}, rootClassLoader());
                Class<?> loaderClass = rootClassLoader().loadClass(loader);
                Method loadMethod = loaderClass.getDeclaredMethod("load", new Class[] {String.class});
                loadMethod.invoke(null, libFile.getAbsolutePath());
                return rootClassLoader().loadClass(nativeAPI).newInstance();
            }
            catch(Exception e) {
                e.printStackTrace();
                return new NoNuma();
            }
        }
    }

    public static boolean isAvailable() {
        return ((NumaAPI) impl).isAvailable();
    }

    public static int maxNodes() {
        return ((NumaAPI) impl).maxNode();
    }
    public static long nodeSize(int node) {
        return ((NumaAPI) impl).nodeSize(node);
    }

    public static long freeSize(int node) {
        return ((NumaAPI) impl).freeSize(node);
    }


    public static ByteBuffer allocLocal(int capacity) {
        return ((NumaAPI) impl).allocLocal(capacity);
    }
    public static ByteBuffer allocOnNode(int capacity, int node) {
        return ((NumaAPI) impl).allocOnNode(capacity, node);
    }
    public static ByteBuffer allocInterleaved(int capacity) {
        return ((NumaAPI) impl).allocInterleaved(capacity);
    }

    public static void free(ByteBuffer buf) {
        ((NumaAPI) impl).free(buf);
    }

}
