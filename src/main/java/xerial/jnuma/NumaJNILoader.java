package xerial.jnuma;

import java.util.HashSet;

/**
 * @author leo
 */
public class NumaJNILoader {
    private static HashSet<String> loadedLib = new HashSet<String>();

    public static synchronized void load(String libPath) {
        if(loadedLib.contains(libPath))
            return;

        try {
            System.load(libPath);
            loadedLib.add(libPath);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

}
