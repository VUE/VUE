package tufts.vue;

import java.util.*;

public class VueUtil
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    private static String currentDirectoryPath = "";
    
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
        } else if (osn.startsWith("WINDOWS")) {
            WindowsPlatform = true;
            System.out.println("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }
    }

    public static void main(String args[])
    {
        test_OpenURL();
    }

    public static void test_OpenURL()
    {
         System.getProperties().list(System.out);
        try {
            openURL("file:///tmp/two words.txt");       // does not work on OSX 10.2
            openURL("\"file:///tmp/two words.txt\"");   // does not work on OSX 10.2
            openURL("\'file:///tmp/two words.txt\'");   // does not work on OSX 10.2
            openURL("file:///tmp/two%20words.txt");     // works on OSX 10.2, but not Windows 2000
            //openURL("file:///tmp/foo.txt");
            //openURL("file:///tmp/index.html");
            //openURL("file:///tmp/does_not_exist");
            //openURL("file:///zip/About_Developer_Tools.pdf");
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    public static double getJavaVersion()
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
    
    private static void openURL_Mac(String url)
        throws java.io.IOException
    {
        System.err.println("Opening Mac URL: [" + url + "]");
        if (url.indexOf(':') < 0 && !url.startsWith("/")) {
            // OSX won't default to use current directory
            // for a relative reference, so we prepend
            // the current directory manually.
            url = "file://" + System.getProperty("user.dir") + "/" + url;
            System.err.println("Opening Mac URL: [" + url + "]");
        }
        if (getJavaVersion() >= 1.4f) {
            // FYI -- this will not compile using mac java 1.3
          //  com.apple.eio.FileManager.openURL(url);

            // use this if want to compile < 1.4
            //Class c = Class.forName("com.apple.eio.FileManager");
            //java.lang.reflect.Method openURL = c.getMethod("openURL", new Class[] { String[].class });
            //openURL.invoke(null, new Object[] { new String[] { url } });

        } else {
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
        private List[] lists = new List[4];
        int nlist = 0;
        int listIndex = 0;
        Iterator curIter;
        
        public GroupIterator(List l1, List l2)
        {
            this(l1, l2, null);
        }
        public GroupIterator(List l1, List l2, List l3)
        {
            if (l1 == null || l2 == null)
                throw new IllegalArgumentException("null list");
            lists[nlist++] = l1;
            lists[nlist++] = l2;
            if (l3 != null)
                lists[nlist++] = l3;
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (listIndex >= nlist)
                    return false;
                curIter = lists[listIndex++].iterator();
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
   
}
