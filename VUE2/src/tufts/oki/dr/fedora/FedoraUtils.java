/*
 * FedoraUtils.java
 *
 * Created on October 14, 2003, 12:21 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */

import javax.swing.AbstractAction;
public class FedoraUtils {
    
    /** Creates a new instance of FedoraUtils */
    public static final String SEPARATOR = ",";
    public static final java.net.URL CONF = getResource("fedora.conf");
    
    public static java.net.URL getResource(String name)
    {
        java.net.URL url = null;
        java.io.File f = new java.io.File(name);
        if (f.exists()) {
            try {
                url = f.toURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (url == null)
            url = ClassLoader.getSystemResource(name);
       return url;
    }
    
  
    public static java.util.Vector stringToVector(String str) {
        java.util.Vector vector = new java.util.Vector();
        java.util.StringTokenizer  st = new java.util.StringTokenizer(str,SEPARATOR);
        while(st.hasMoreTokens()){
            vector.add(st.nextToken());
        }
        return vector;
    }
    
    public static  String processId(String pid ) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(pid,":");
        String processString = "";
        while(st.hasMoreTokens()) {
            processString += st.nextToken();
        }
        return processString;
    }
    
    public static String getSaveFileName(osid.shared.Id objectId,osid.shared.Id behaviorId,osid.shared.Id disseminationId) throws osid.OsidException {
        String saveFileName = processId(objectId.getIdString()+"-"+behaviorId.getIdString()+"-"+disseminationId.getIdString());
        return saveFileName;
    }
    
    public static AbstractAction getFedoraAction(osid.dr.InfoField infofield) throws osid.dr.DigitalRepositoryException {
        final Dissemination dissemination = (Dissemination)infofield;
        try {
            AbstractAction fedoraAction = new AbstractAction(infofield.getInfoPart().getDisplayName()) {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                      DR dr = (DR) dissemination.getBehavior().getFedoraObject().getDR();
                      String fedoraUrl = dr.getFedoraProperties().getProperty("url.fedora.get","http://hosea.lib.tufts.edu:8080/fedora/get");
                      openURL(fedoraUrl+"/"+dissemination.getBehavior().getFedoraObject().getId().getIdString()+"/"
                        +dissemination.getBehavior().getId().getIdString()+"/"+dissemination.getId().getIdString()+"/");
                     } catch(Exception ex) { } 
                }

            };
            return fedoraAction;
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraUtils.getFedoraAction "+ex.getMessage());
        } 
    }
    
    
    
    // this part is for opening the resources.  Theis has been copied from VueUtil.java
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
        } else if (osn.startsWith("WINDOWS")) {
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
    
}
