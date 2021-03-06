/*
 * Copyright 2012 Taro L. Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xerial.jnuma;


import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 * Numa API.
 *
 *
 * When allocating new {@link java.nio.ByteBuffer}s using this API,
 * you must release these buffers by calling {@link Numa#free(java.nio.ByteBuffer)} because
 * the allocated buffers are out of control of the GC of the JVM.
 *
 * @author Taro L. Saito
 */
public class Numa {

    /**
     * This type must be Object rather than NumaInterface because some JVM implementation loads the the class
     * specified in the field before the completion of the injection of NumaInterface and NumaNative to the root class loader.
     */
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
        boolean isAccessible = m.isAccessible();
        try {
            if(!isAccessible)
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
        finally {
            if(!isAccessible)
                m.setAccessible(false);
        }

    }


    private static Object init() {
        String osName = System.getProperty("os.name", "");
        if(osName.contains("Windows") || osName.contains("Mac"))
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
                inject(new String[]{loader, "xerial.jnuma.NumaInterface", "xerial.jnuma.NumaNative"}, rootClassLoader());
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

    /**
     * Returns true if the NUMA is available in this machine
     * @return
     */
    public static boolean isAvailable() {
        return ((NumaInterface) impl).isAvailable();
    }

    /**
     * Max number of numa nodes (local memories)
     * @return
     */
    public static int numNodes() {
        return ((NumaInterface) impl).maxNode()+1;
    }

    /**
     * The memory size of the node
     * @param node node number
     * @return memory byte size
     */
    public static long nodeSize(int node) {
        return ((NumaInterface) impl).nodeSize(node);
    }

    /**
     * The free memory size of the node
     * @param node node number
     * @return free memory byte size
     */
    public static long freeSize(int node) {
        return ((NumaInterface) impl).freeSize(node);
    }

    /**
     * Distance between two NUMA nodes. The distance is a multiple of 10s
     * @param node1
     * @param node2
     * @return node distance
     */
    public static int distance(int node1, int node2) {
        return ((NumaInterface) impl).distance(node1, node2);
    }

    /**
     * Get the number of CPUs available to this machine
     * @return
     */
    public static int numCPUs() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Create a bit mask for specifying CPU sets. From the LSB, it corresponds CPU0, CPU1, ...
     * @return
     */
    public static long[] newCPUBitMask() {
        int bufSize = (numCPUs() + 64 -1)/ 64;
        return new long[bufSize];
    }

    /**
     * Create a bit mask setting a single CPU on
     * @param cpu
     * @return
     */
    public static long[] newCPUBitMaskForOneCPU(int cpu) {
        long[] cpuMask = newCPUBitMask();
        cpuMask[cpu / 64] |= 1L << (cpu % 64);
        return cpuMask;
    }

    /**
     * Create a bit mask setting all CPUs on
     * @return
     */
    public static long[] newCPUBitMaskForAllCPUs() {
        long[] cpuMask = newCPUBitMask();
        int M = numCPUs();
        for(int i=0; i<cpuMask.length; ++i)
            cpuMask[i] |= (i*64 > M) ? ~0L : ~(~0L << (M % 64));
        return cpuMask;
    }

    /**
     * Return the bit vector showing the closest CPUs to the specified NUMA node
     * @param node numa node number
     * @return cpu bit vector
     */
    public static long[] nodeToCpus(int node) {
        long[] bv = new long[512/8];
        ((NumaInterface) impl).nodeToCpus(node, bv);
        return bv;
    }

    /**
     * Get the affinity bit vector of the current thread to the CPUs. In default,
     * all bits are set to 1. If you want to bind the current thread to a specific CPU, use {@link #setAffinity(int)}.
     *
     * Before terminating the thread, you should reset the affinity so that OS can assign (probably pooled) threads to arbitrary CPUs.
     * @return
     */
    public static long[] getAffinity() {
        long[] cpuMask = newCPUBitMask();
        ((NumaInterface) impl).getAffinity(0, cpuMask, numCPUs());
        return cpuMask;
    }

    /**
     * Set the affinity of this thread to a single cpu
     * @param cpu cpu number
     */
    public static void setAffinity(int cpu) {
        setAffinity(newCPUBitMaskForOneCPU(cpu));
    }

    /**
     * Set the affinity of this thread to CPUs specified in the bit vector
     * @param cpuBitMask bit vector. CPU0 corresponds to the LSB.
     */
    public static void setAffinity(long[] cpuBitMask) {
        ((NumaInterface) impl).setAffinity(0, cpuBitMask, numCPUs());
    }

    /**
     * Reset the affinity of the current thread to CPUs.
     */
    public static void resetAffinity() {
        ((NumaInterface) impl).setAffinity(0, newCPUBitMaskForAllCPUs(), numCPUs());
    }

    /**
     * Returns the preferred node of the current thread.
     * @return preferred numa node
     */
    public static int getPreferredNode() {
        return ((NumaInterface) impl).preferredNode();
    }

    /**
     * Set the memory allocation policy for the calling thread to local allocation.
     */
    public static void setLocalAlloc() {
        ((NumaInterface) impl).setLocalAlloc();
    }

    /**
     * Sets the preferred node for the current thread. The system will attempt to allocate memory from the preferred node, but will fall back to other
     * nodes if no memory is available on the preferred node. To reset the node preference, pass a node of -1 argument or call {@link #setLocalAlloc()} .
     * @param node
     */
    public static void setPreferred(int node) {
        ((NumaInterface) impl).setPreferred(node);
    }

    /**
     * Run the current thread and its children on a specified node. To reset the binding call {@link #runOnAllNodes()}
     * @param node
     */
    public static void runOnNode(int node) {
        ((NumaInterface) impl).runOnNode(node);
    }

    public static void runOnAllNodes() {
        runOnNode(-1);
    }


    /**
     * Allocate a new NUMA buffer using the current policy. You must release the acquired buffer by {@link #free(java.nio.ByteBuffer)} because
     * it is out of the control of GC.
     * @param capacity byte size of the buffer
     * @return new ByteBuffer
     */
    public static ByteBuffer alloc(int capacity) {
        return ((NumaInterface) impl).alloc(capacity);
    }

    /**
     * Allocate a new local NUMA buffer. You must release the acquired buffer by {@link #free(java.nio.ByteBuffer)} because
     * it is out of the control of GC.
     * @param capacity byte size of the buffer
     * @return new ByteBuffer
     */
    public static ByteBuffer allocLocal(int capacity) {
        return ((NumaInterface) impl).allocLocal(capacity);
    }

    /**
     * Allocate a new NUMA buffer on a specific node. You must release the acquired buffer by {@link #free(java.nio.ByteBuffer)} because
     * it is out of the control of GC.
     * @param capacity byte size of the buffer
     * @param node node number
     * @return new ByteBuffer
     */
    public static ByteBuffer allocOnNode(int capacity, int node) {
        return ((NumaInterface) impl).allocOnNode(capacity, node);
    }

    /**
     * Allocate a new NUMA buffer interleaved on multiple NUMA nodes. You must release the acquired buffer by {@link #free(java.nio.ByteBuffer)} because
     * it is out of the control of GC.
     * @param capacity byte size of the buffer
     * @return new ByteBuffer
     */
    public static ByteBuffer allocInterleaved(int capacity) {
        return ((NumaInterface) impl).allocInterleaved(capacity);
    }

    /**
     * Allocate a new NUMA buffer of the specified capacity
     * @param capacity
     * @return the raw memory address
     */
    public static long allocMemory(long capacity) {
        return ((NumaInterface) impl).allocMemory(capacity);
    }

    /**
     * Release the memory resource allocated at the specified address and capacity.
     * @param address
     * @param capacity
     */
    public static void free(long address, long capacity) {
        ((NumaInterface) impl).free(address, capacity);
    }

    /**
     * Release the memory resources of the numa ByteBuffer.
     * @param buf the buffer to release
     */
    public static void free(ByteBuffer buf) {
        ((NumaInterface) impl).free(buf);
    }


    /**
     * Send the array to a node. The array should be primitive type array. To send the array to a node correctly,
     * the array should not be touched before calling this method.
     * @param array
     * @param byteLength
     * @param node
     */
    public static void toNodeMemory(Object array, int byteLength, int node) {
        ((NumaInterface) impl).toNodeMemory(array, byteLength, node);
    }



}
