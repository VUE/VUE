package tufts.vue;

import java.util.*;
import java.io.File;

public class VueUtil
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    private static String currentDirectoryPath = "";
    
    // Mac OSX Java 1.4.1 has a bug where stroke's are exactly 0.5
    // units off center (down/right from proper center).  You need to
    // draw a minumim stroke on top of a stroke of more than 1 to see
    // this, because at stroke width 1, this looks appears as a policy
    // of drawing strokes down/right to the line. Note that there are
    // other problems with Mac strokes -- a stroke width of 1.0
    // doesn't appear to scale with the graphics context, but any
    // value just over 1.0 will.
    public static boolean StrokeBug05 = false;
   
    static {
        String osName = System.getProperty("os.name");
        String javaSpec = System.getProperty("java.specification.version");

        try {
            javaVersion = Float.parseFloat(javaSpec);
            System.out.println("Java Version: " + javaVersion);
        } catch (Exception e) {
            System.err.println("Couldn't parse java.specifcaion.version: [" + javaSpec + "]");
            System.err.println(e);
        }

        String osn = osName.toUpperCase();
        if (osn.startsWith("MAC")) {
            MacPlatform = true;
            System.out.println("Mac JVM: " + osName);
            System.out.println("Mac mrj.version: " + System.getProperty("mrj.version"));
        } else if (osn.indexOf("WINDOWS") >= 0) {
            WindowsPlatform = true;
            System.out.println("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }
        //if (isMacPlatform()) 
        //  StrokeBug05 = true; // todo: only if mrj.version < 69.1, where they fixed this bug
        if (StrokeBug05)
            System.out.println("Stroke compensation active (0.5 unit offset bug)");
    }

    public static void main(String args[])
        throws java.io.IOException
    {
        System.out.println("cursor16 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(16,16));
        System.out.println("cursor24 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(24,24));
        System.out.println("cursor32 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(32,32));

                                       //.list(System.out);
        Hashtable props = System.getProperties();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            //System.out.println("[1;36m" + key + "[m=" + props.get(key));
            System.out.println(key + "=" + props.get(key));
        }
        
        if (args.length > 0)
            openURL(args[0]);
        else
            test_OpenURL();
        System.exit(0);
    }

    public static void test_OpenURL()
    {
        try {
            openURL("http://hosea.lib.tufts.edu:8080/fedora/get//tufts:7/demo:60/getThumbnail/");
            //openURL("file:///tmp/two words.txt");       // does not work on OSX 10.2 (does now with %20 space replacement)
            //openURL("\"file:///tmp/two words.txt\"");   // does not work on OSX 10.2
            //openURL("\'file:///tmp/two words.txt\'");   // does not work on OSX 10.2
            //openURL("file:///tmp/two%20words.txt");     // works on OSX 10.2, but not Windows 2000
            //openURL("file:///tmp/foo.txt");
            //openURL("file:///tmp/index.html");
            //openURL("file:///tmp/does_not_exist");
            //openURL("file:///zip/About_Developer_Tools.pdf");
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    /*
     * Be sure to compare this value to a constant *float*
     * value -- e.g. "1.4f" -- just "1.4" as double value may
     * actually appear to be less than 1.4, as that can't
     * be exactly represented by a double.
     */
    public static float getJavaVersion()
    {
        return javaVersion;
    }
       

    public static void openURL(String url)
        throws java.io.IOException
    {
        // todo: spawn this in another thread just in case it hangs
        
        // there appears to be no point in quoting the URL...
        String quotedURL;
        if (true || url.charAt(0) == '\'')
            quotedURL = url;
        else
            quotedURL = "\'" + url + "\'";

        //if (isMacPlatform()) quotedURL = "\'" + url + "\'";

        System.err.println("Opening URL [" + quotedURL + "]");
        if (isMacPlatform())
            openURL_Mac(quotedURL);
        else if (isUnixPlatform())
            openURL_Unix(quotedURL);
        else // default is a windows platform
            openURL_Windows(quotedURL);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url)
        throws java.io.IOException
    {
        String cmd = PC_OPENURL_CMD + " " + url;
        System.err.println("Opening PC URL with: [" + cmd + "]");
        Process p = Runtime.getRuntime().exec(cmd);
        if (false) {
            try {
                System.err.println("waiting...");
                p.waitFor();
            } catch (Exception ex) {
                System.err.println(ex);
            }
            System.err.println("exit value=" + p.exitValue());
        }
    }

    private static java.lang.reflect.Method macOpenURL_Method = null;
    private static void openURL_Mac(String url)
        throws java.io.IOException
    {
        url = url.replaceAll(" ", "%20");
        System.err.println("Opening Mac URL: [" + url + "]");
        if (url.indexOf(':') < 0 && !url.startsWith("/")) {
            // OSX won't default to use current directory
            // for a relative reference, so we prepend
            // the current directory manually.
            url = "file://" + System.getProperty("user.dir") + "/" + url;
            System.err.println("Opening Mac URL: [" + url + "]");
        }
        if (getJavaVersion() >= 1.4f) {
            // Can't call this directly because wont compile on the PC
            //com.apple.eio.FileManager.openURL(url);

            if (macOpenURL_Method == null) {
                try {
                    Class macFileManager = Class.forName("com.apple.eio.FileManager");
                    //Class macFileManager = ClassLoader.getSystemClassLoader().loadClass("com.apple.eio.FileManager");
                    macOpenURL_Method = macFileManager.getMethod("openURL", new Class[] { String.class });
                } catch (Exception e) {
                    System.err.println("Failed to find Mac FileManager or openURL method: will not be able to display URL content.");
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager:openURL " + e);
                }
            }
            
            if (macOpenURL_Method != null) {
                try {
                    macOpenURL_Method.invoke(null, new Object[] { url });
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager.openURL " + e);
                }
            } else
                throw new UnsupportedOperationException("openURL_Mac");


        } else {
            throw new UnsupportedOperationException("mac java <= 1.3 openURL unimplemented");
            // put this back if want to suppor mac java 1.3
            // this has been deprecated in mac java 1.4, so
            // just ignore the warning if using a 1.4 or beyond
            // compiler
            //    com.apple.mrj.MRJFileUtils.openURL(url);
        }
        System.err.println("returned from openURL_Mac " + url);
    }

    private static void openURL_Unix(String url)
        throws java.io.IOException
    {
        throw new java.io.IOException("Unimplemented");
    }


    public static boolean isWindowsPlatform()
    {
        return WindowsPlatform;
    }
    public static boolean isMacPlatform()
    {
        return MacPlatform;
    }
    public static boolean isUnixPlatform()
    {
        return UnixPlatform;
    }
    

    public static class GroupIterator implements Iterator
    {
        private Object[] iterables = new Object[4];
        int nIter = 0;
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator(Object l1, Object l2)
        {
            this(l1, l2, null);
        }
        public GroupIterator(Object l1, Object l2, Object l3)
        {
            if (l1 == null || l2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            iterables[nIter++] = l1;
            iterables[nIter++] = l2;
            if (l3 != null)
                iterables[nIter++] = l3;
            for (int i = 0; i < nIter; i++) {
                if (!(iterables[i] instanceof Collection) &&
                    !(iterables[i] instanceof Iterator))
                    throw new IllegalArgumentException("arg i not Collection or Iterator");
            }
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= nIter)
                    return false;
                if (iterables[iterIndex] instanceof Collection)
                    curIter = ((Collection)iterables[iterIndex]).iterator();
                else
                    curIter = (Iterator) iterables[iterIndex];
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            if (!curIter.hasNext()) {
                curIter = null;
                return hasNext();
            }
            return true;
        }

        public Object next()
        {
            if (curIter == null)
                return null;
            else
                return curIter.next();
        }

        public void remove()
        {
            if (curIter != null)
                curIter.remove();
        }
    }

    
    public static void  setCurrentDirectoryPath(String cdp) {
        currentDirectoryPath = cdp;
    }
    
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }    
    
    public static boolean isCurrentDirectoryPathSet() {
        if(currentDirectoryPath.equals("")) 
            return false;
        else
            return true;
    }

    public static File getDefaultUserFolder() {
        File userHome = new File(System.getProperty("user.home"));
        if(userHome == null) 
            userHome = new File(System.getProperty("java.io.tmpdir"));
        File userFolder = new File(userHome.getPath()+File.separatorChar+"vue");
        if(userFolder.isDirectory())
            return userFolder;
        if(!userFolder.mkdir())
            throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be created");
        return userFolder;
    }
    
    public static void deleteDefaultUserFolder() {
        File userFolder = getDefaultUserFolder();
        File[] files = userFolder.listFiles();
        System.out.println("file count = "+files.length);
        for(int i = 0; i<files.length;i++) {
            if(files[i].isFile() && !files[i].delete()) 
                throw new RuntimeException(files[i].getAbsolutePath()+":cannot be created");
        }
        if(!userFolder.delete()) 
             throw new RuntimeException(userFolder.getAbsolutePath()+":cannot be deleted");
    }

    public static void centerOnScreen(java.awt.Window window)
    {
        if (VueUtil.getJavaVersion() >= 1.4f) {
            java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            p.x -= window.getWidth() / 2;
            p.y -= window.getHeight() / 2;
            window.setLocation(p);
        } else {
            java.awt.Dimension d = window.getToolkit().getScreenSize();
            int x = d.width/2 - window.getWidth()/2;
            int y = d.height/2 - window.getHeight()/2;
            window.setLocation(x, y);
        }
    }
    
    
    public static void alert(javax.swing.JComponent component,String message,String title) {
         javax.swing.JOptionPane.showMessageDialog(component,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);                                      
    }
    
}
