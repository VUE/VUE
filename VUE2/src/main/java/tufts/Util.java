/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.text.BreakIterator;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import tufts.macosx.MacOSX;


/**
 * Utility class.  Provides code for determining what platform we're on,
 * platform-specific code for opening URL's, as well as various convenience
 * functions.
 *
 * @version $Revision: $ / $Date: $ / $Author: sfraize $
 */

public class Util
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Util.class);
    
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean MacAquaLAF = false;
    private static boolean MacAquaLAF_set = false;
    private static boolean UnixPlatform = false;
    private static final String PlatformName;
    private static final String OSVersion;
    private static final boolean OSisMacLeopard;
    private static float javaVersion = 1.0f;
    
    private static int MacMRJVersion = -1;
    private static float MacOSXVersion = 0f;

    private static boolean DEBUG = false;

    private static String PlatformEncoding;
    private static boolean JVM_is_64_bit;
    
    static {

        if (!DEBUG)
            DEBUG = (System.getProperty("tufts.Util.debug") != null);
           
        final String osName = System.getProperty("os.name");
        final String osArch = System.getProperty("os.arch");
        OSVersion = System.getProperty("os.version");
        final String javaSpec = System.getProperty("java.specification.version");

        final String bits = System.getProperty("sun.arch.data.model");

        if (bits.equals("64"))
            JVM_is_64_bit = true;
        else
            JVM_is_64_bit = false;

        PlatformName = osName + " " + OSVersion + " " + osArch;

        if (DEBUG) out(String.format("Platform: %s / %s / %s", osName, OSVersion, osArch));
        //if (DEBUG) out(String.format("Platform: %s / %s / %s; Leopard=%s", osName, OSVersion, osArch, OSisMacLeopard));
        
        try {
            javaVersion = Float.parseFloat(javaSpec);
            if (DEBUG) out("Java Version: " + javaVersion + "; JVM-bits=" + bits + "; 64bit=" + JVM_is_64_bit);
        } catch (Exception e) {
            errorOut("couldn't parse java.specifcaion.version: [" + javaSpec + "]");
            errorOut(e.toString());
        }

        final String osn = osName.toUpperCase();
        /*
         * fix for forums problem.  one of the strangest bugs i've seen in a while.  
         * something to keep in mind for future, calling toUpperCase() in a 
         * locale that uses diacritics will add diacritics to a string that 
         * did not originally contain them.  so, "windows vista" gets 
         * diacritics on the i in both words, the fix is so put the match 
         * we're looking for as the lower case word mac and winodws, and call 
         * toUpperCase on them so both sides of the comparison are always being
         * processed the same way.  note that if you call to upper on a string
         * that is already upper case it does not place in the diacritic characters.
         */
        if (osn.startsWith("mac".toUpperCase())) {
            MacPlatform = true;

            Float osVersionNumber = Float.parseFloat(OSVersion.substring(0,4));

            final String PROP_FORCE_OSX_VERSION = "tufts.Util.debug.setOSX";
            final String osxFlag = System.getProperty(PROP_FORCE_OSX_VERSION);
            if (DEBUG && osxFlag != null) {
                out("Property " + PROP_FORCE_OSX_VERSION + " is set to " + osxFlag);
                out("Mac OS X major version IS OVERRIDING-> " + osVersionNumber);
                osVersionNumber = Float.parseFloat(osxFlag.substring(0,4));
            }

            Util.MacOSXVersion = osVersionNumber;

            if (DEBUG) out("Mac OS X major version number (float): " + osVersionNumber);
            if (osVersionNumber >= 10.5) // Really this is a "leopard or later" flag
                OSisMacLeopard = true;
            else
                OSisMacLeopard = false;
                // OSisMacLeopard = // Really this is a "leopard (10.5) or later" flag
                //     OSVersion.startsWith("10.5") || // Leopard
                //     OSVersion.startsWith("10.6") || // Snow Leopard
                //     OSVersion.startsWith("10.7")    // Lion
                //     ;

            if (DEBUG) out(String.format("Mac Leopard (10.5) or later = %s", OSisMacLeopard));
            String mrj = System.getProperty("mrj.version");
            // This is null on Oracle JRE, so in this case user os.name 

            if (mrj == null || mrj == "")
              mrj = System.getProperty("java.runtime.version");

            int i = 0;
            while (i < mrj.length()) {
                if (!Character.isDigit(mrj.charAt(i)))
                    break;
                i++;
            }
            try {
                MacMRJVersion = Integer.parseInt(mrj.substring(0, i));
            } catch (NumberFormatException e){
                errorOut("couldn't parse mrj.version: " + e + mrj);
                errorOut(e.toString());
            }
            if (DEBUG) out("Mac mrj.version: \"" + mrj + "\" = " + MacMRJVersion);
            
        } else if (osn.indexOf("windows".toUpperCase()) >= 0) {
            WindowsPlatform = true;
            OSisMacLeopard = false;
            //if (DEBUG) out("Windows Platform: " + PlatformName);
        } else {
            UnixPlatform = true;
            OSisMacLeopard = false;
        }

        if (DEBUG) out(String.format("OSFlags: isWin=%b; isMac=%b(leopard=%b); isUnix=%b;", WindowsPlatform, MacPlatform, OSisMacLeopard, UnixPlatform));

        String term = System.getenv("TERM");
        boolean allowColor = false;

        if (term == null)
            term = "";
        
        allowColor = term.indexOf("color") >= 0;
        if (!allowColor) {
            term = System.getenv("SSH_TERM");
            if (term != null && term.indexOf("color") >= 0)
                allowColor = true;
        }

        if (allowColor) {
            TERM_RED    = "\033[1;31m";  
            TERM_GREEN  = "\033[1;32m";
            TERM_YELLOW = "\033[1;33m";
            TERM_BLUE   = "\033[1;34m";
            TERM_PURPLE = "\033[1;35m";
            TERM_CYAN   = "\033[1;36m";
            TERM_BOLD   = "\033[1m";
            TERM_CLEAR  = "\033[m";
        } else {
            TERM_RED = TERM_GREEN = TERM_YELLOW = TERM_BLUE = TERM_PURPLE = TERM_CYAN = TERM_BOLD = TERM_CLEAR = "";
        }
        //printStackTrace("TERM[" + term + "]");
    }
    
    /** Common escape codes for terminal text colors.  Set to empty string unless on a color terminal */
    public static final String TERM_RED, TERM_GREEN, TERM_YELLOW, TERM_BLUE, TERM_PURPLE, TERM_CYAN, TERM_BOLD, TERM_CLEAR;

    private static void out(String s) {
        if (Log.isDebugEnabled())
            Log.debug(s);
        else
            System.out.println("tufts.Util: " + s);
    }

    private static void errorOut(String s) {
        System.err.println("tufts.Util: " + s);
        Log.error(s);
    }

    public static String formatLines(String target, int maxLength)
    {
        Locale currentLocale = new Locale ("en","US");

    	return formatLines(target,maxLength,currentLocale);
    }
    
    public static String formatLines(String target, int maxLength, 
                                     Locale locale)
    {
        if (target == null) {
            Log.warn("attempt to line-break null string");
            return null;
        }
        
        try {
            target = breakLines(target, maxLength, locale);
        } catch (Throwable t) {
            Log.error("failed to line-break [" + target + "]; ", t);
        }

        return target;

    }

    public static String maxDisplay(final String s, int len) {

        if (s == null)
            return null;

        if (len < 4)
            len = 4;

        if (s.length() > len) {
            return s.substring(0,len-3) + "...";
        } else {
            return s;
        }
    }
    
    private static String breakLines(final String target, int maxLength, Locale currentLocale)
    {
    	final StringBuilder s = new StringBuilder(target.length() + 4);
    	
        final BreakIterator boundary = BreakIterator.getLineInstance(currentLocale);
        boundary.setText(target);
        
        int start = boundary.first();
        int end = boundary.next();
        int lineLength = 0;

        boolean didBreak = false;
		
        while (end != BreakIterator.DONE) {
            final String word = target.substring(start,end);

            lineLength += word.length();

//             Log.debug(String.format("len: %2d, word %s%s",
//                                     lineLength,
//                                     tags(word),
//                                     atExistingNewline ? ", @newline" : ""));

            if (lineLength >= maxLength) {

                final boolean atExistingNewline =
                    s.length() > 1 && s.charAt(s.length() - 1) == '\n';
            
                if (!atExistingNewline) {
                    //Log.debug("adding newline");
                    s.append('\n');
                    didBreak = true;
                }
                lineLength = word.length();
            }
			
            //Log.debug("adding word " + tags(word));
            s.append(word);
            start = end;
            end = boundary.next();
        }
        return didBreak ? s.toString() : target;
    } 

    /*
     * Be sure to compare this value to a constant *float*
     * value -- e.g. "1.4f" -- just "1.4" as double value may
     * actually appear to be less than 1.4, as that can't
     * be exactly represented by a double.
     */
    public static float getJavaVersion() {
        return javaVersion;
    }
       
    /** return mac runtime for java version.  Will return -1 if we're not running on mac platform. */
    public static int getMacMRJVersion() {
        return MacMRJVersion;
    }
    
    /** @return Mac OS X Major Version Number (significant to 1 decimal place) -- e.g., values 10.4 thru 10.8, etc
     * Will return 0 if not Mac Platform */
    public static float getMacOSXVersion() {
        return MacOSXVersion;
    }
    
    public static String getPlatformName() {
        return PlatformName == null ? (System.getProperty("os.name") + "?") : PlatformName;
    }
       
    public static String getOSVersion() {
        return OSVersion;
    }
    
    /** @return true if this is Mac OS X Leopard or *later*: E.g., Snow Leopard, Lion, Mountian Lion, etc
     * -- this named method kept for backward compatability
     * @deprecated
     */
    public static boolean isMacLeopard() {
        return OSisMacLeopard;
    }
    /** @return true if this is Mac OS X Leopard or *later*: E.g., Snow Leopard, Lion, Mountian Lion, etc */
    public static boolean isMacLeopardOrLater() {
        return OSisMacLeopard;
    }
       
    public static boolean isMacTiger() {
        return MacPlatform && !OSisMacLeopard;
    }
       

    /** @return true if we're running on a version Microsoft Windows */
    public static boolean isWindowsPlatform() {
        return WindowsPlatform;
    }
    /** @return true if we're running on an Apple Mac OS */
    public static boolean isMacPlatform() {
        return MacPlatform;
    }
    /** @return true if we're running on unix only platform (e.g., Linux -- Mac OS X not included) */
    public static boolean isUnixPlatform(){
        return UnixPlatform;
    }

    /**
     * @return true of Mac Cocoa extensions are supported.
     * Currently only true if running in a 32bit Java 1.5 VM.
     */
    public static boolean isMacCocoaSupported()
    {
    	return isMacPlatform() && !JVM_is_64_bit && javaVersion < 1.6;
    }
    
    public static String getDefaultPlatformEncoding() {
        if (PlatformEncoding == null)
            PlatformEncoding = (new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream())).getEncoding();
        return PlatformEncoding;
    }
    

    /** @return true if the current Look & Feel is Mac Aqua (not always true just because you're on a mac)
     * Note: do NOT call this from any static initializers the result may be changed by application startup
     * code. */
    public static boolean isMacAquaLookAndFeel()
    {
        // we can't set this at static init time because the LAF can be set after that
        if (MacAquaLAF_set == false) {
            MacAquaLAF =
                isMacPlatform() &&
                javax.swing.UIManager.getLookAndFeel().getName().toLowerCase().indexOf("aqua") >= 0;
            MacAquaLAF_set = true;
        }
        return MacAquaLAF;
    }

    /** @param url -- a url assumed to be in the platform default format (e.g., it may be
     * formatted in a way only understood on the current platform).
     */
    public static void openURL(String url)
        throws java.io.IOException
    {
        // TODO: isLocalFile should check for inital '\' also (File.separator) -- but follow-on code will now need re-testing
        boolean isLocalFile = (url.length() >= 5 && "file:".equalsIgnoreCase(url.substring(0,5))) || url.startsWith("/");
        boolean isMailTo = url.length() >= 7 && "mailto:".equalsIgnoreCase(url.substring(0,7));

        if (DEBUG) System.err.println("openURL_PLAT [" + url + "] isLocalFile=" + isLocalFile + " isMailTo=" + isMailTo);
        
        if (isMacPlatform())
            openURL_Mac(url, isLocalFile, isMailTo);
        else if (isUnixPlatform())
            openURL_Unix(url, isLocalFile, isMailTo);
        else // default is a windows platform
            openURL_Windows(url, isLocalFile, isMailTo);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url, boolean isLocalFile, boolean isMailTo)
        throws java.io.IOException
    {
        System.err.println("openURL_Win  [" + url + "]");

        // On at least Windows 2000, %20's don't work when given to url.dll FileProtocolHandler
        // (Should be using some kind of URLProtocolHanlder ?)
        
        // Also at least Win2K: file:///C:/foo/bar.html works, but start that with file:/ or file://
        // and it DOES NOT work -- you MUST have the three slashes, OR, you can have NO SLASHES,
        // and it will work... (file:C:/foo/bar.html)
        // ALSO, file:// or // file:/// works BY ITSELF (file:/ by itself still doesn't work).
        
        //url = url.replaceAll("%20", " ");

        if (!isMailTo) {
            url = decodeURL(url);
            url = decodeURL(url); // run twice in case any %2520 double encoded spaces
        }

        if (isLocalFile) {
            // below works, but we're doing a full conversion to windows path names now
            //url = url.replaceFirst("^file:/+", "file:");
            url = url.replaceFirst("^file:/+", "");
            url = url.replace('/', '\\');
            System.err.println("openURL_patch[" + url + "]");
            char c1 = 0;
            try { c1 = url.charAt(1); } catch (StringIndexOutOfBoundsException e) {}
            if (c1 == ':' || c1 == '|') {
                // Windows drive letter specification, e.g., "C:".
                // Also, I've seen D|/dir/file.txt in a shortcut file, so we allow that too.
                // In any case, we do noting: this string should be workable to url.dll
            } else {

                if (url.startsWith("file:")) {
                    // DO NOTHING.
                    // If we had "file:" above and NOT "file:/", we need this check here.
                    // This suggests this code needs refactoring.  SMF 2008-04-02
                } else {
                    // if this does NOT start with a drive specification, assume
                    // the first component is a host for a windows network
                    // drive, and thus we have to pre-pend \\ to it, to get \\host\file
                    url = "\\\\" + url;
                }
            }
            // at this point, "url" is really just a local windows file
            // in full windows syntax (backslashes, drive specs, etc)
            System.err.println("openURL_WinLF[" + url + "]");
        }

        String cmd = PC_OPENURL_CMD + " " + url;
        final int sizeURL = url.length();
        final int sizeCommand = cmd.length();
        final int sizeWinArgs = sizeCommand - 17; // subtract "rundll32 url.dll,"
        boolean debug = DEBUG;
        if (sizeURL > 2027 || sizeCommand > 2064 || sizeWinArgs >= 2048) {
            System.err.println("\nWarning: WinXP buffer lengths likely exceeded: command not likely to run (arglen=" + sizeWinArgs + ")");
            debug = true;
        } else {
            System.err.println();
        }
        if (debug) {
            System.err.println("exec       url length: " + sizeURL);
            System.err.println("exec   command length: " + sizeCommand);
            System.err.println("exec url.dll args len: " + sizeWinArgs);
        }

        if (sizeCommand > 2064) {
            System.err.println("exec: truncating command and hoping for the best...");
            cmd = cmd.substring(0,2063);
        }

        System.err.println("exec[" + cmd + "]");

        Runtime.getRuntime().exec(cmd);

//         final String mailto = "mailto:"
//             + "foo@bar.com?"
//             + "subject=TestSubject"
//             + "&attachment="
//             //+ "&Attach="
//             + "\""
//             + "c:\\\\foo.txt"
//             + "\""
//             ;
//         System.err.println("MAILTO: ["+mailto+"]");
//         Runtime.getRuntime().exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", mailto
//                                                 "mailto:"
//                                                 + "&subject=TestSubject"
//                                                 + "&attachment=" + "\"" + "c:\\\\foo.txt" + "\""
//             },
//             null);

        
//         if (false) {
//             Process p = Runtime.getRuntime().exec(cmd);
//             try {
//                 System.err.println("waiting...");
//                 p.waitFor();
//             } catch (Exception ex) {
//                 System.err.println(ex);
//             }
//             System.err.println("exit value=" + p.exitValue());
//         }
    }

    /**
     * Call a named static method with the given args.
     * The method signature is inferred from the argument types.  We map the primitive
     * type wrapper class to their primitive types (an auto un-boxing), so this
     * won't work for method calls that take args of Boolean, Integer, etc,
     * only boolean, int, etc.
     */
    public static Object execute(Object object, String className, String methodName, Object[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalArgumentException,
               IllegalAccessException,
               java.lang.reflect.InvocationTargetException
    {
        if (DEBUG) {
            // use multiple prints in remote case of a class-load or some such error along the way
            System.err.println("Util.execute:");
            System.err.println("\t className: [" + className + "]");
            System.err.println("\tmethodName: [" + methodName + "]");

            String desc;
            if (args == null)
                desc = "null";
            else if (args.length == 0)
                desc = "(none)";
            else if (args.length == 1)
                desc = objectTag(args[0]) + " [" + args[0] + "]";
            else
                desc = Arrays.asList(args).toString();
                
            System.err.println("\t      args: " + desc);
            System.err.println("\t    object: " + objectTag(object) + " [" + object + "]");
                               
        }
        
        final Class clazz = Class.forName(className);
        final Class[] argTypes;
        
        if (args == null) {
            argTypes = null;
        } else {
            argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                final Class argClass = args[i].getClass();
                if (argClass == Boolean.class) argTypes[i] = Boolean.TYPE;
                else if (argClass == Integer.class) argTypes[i] = Integer.TYPE;
                else if (argClass == Long.class)    argTypes[i] = Long.TYPE;
                else if (argClass == Short.class)   argTypes[i] = Short.TYPE;
                else if (argClass == Float.class)   argTypes[i] = Float.TYPE;
                else if (argClass == Double.class)  argTypes[i] = Double.TYPE;
                else if (argClass == Byte.class)    argTypes[i] = Byte.TYPE;
                else if (argClass == Character.class) argTypes[i] = Character.TYPE;
                else
                    argTypes[i] = args[i].getClass();
            }
        }
                
        java.lang.reflect.Method method = clazz.getMethod(methodName, argTypes);

        if (DEBUG) System.err.print("\t  invoking: " + method + " ... ");
        
        Object result = method.invoke(object, args);

        if (DEBUG) System.err.println("returned.");
        
        if (DEBUG && method.getReturnType() != void.class) {
            // use multiple prints in remote case of a class-load or some such error along the way
            //System.err.println("Util.execute:");
           System.err.println("\t    return: " + objectTag(result) + " [" + result + "]");
        }
        //System.out.println("Sucessfully invoked " + className + "." + methodName + ", result=" + result);
        
        return result;
    }

    public static Object execute(String className, String methodName, Object[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalArgumentException,
               IllegalAccessException,
               java.lang.reflect.InvocationTargetException
    {
        return execute(null, className, methodName, args);
    }
    
    /** Attempt a method call.  If it fails, quietly fail, printing a stack trace and returning null.
     * @param object - cannot be null (implying a static method), as is also used to get the class name
     * If object is a Class object, make this a static invocation in that class.
     */
    public static Object invoke(Object object, String methodName, Object[] args) 
    {
        String className;
        if (object instanceof Class) {
            className = ((Class)object).getName();
            object = null;
        } else
            className = object.getClass().getName();
        
        try {
            return execute(object, className, methodName, args);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    /**
     * Attempt a method call.  If it fails, quietly fail, printing a stack trace and returning null.
     * This is a convenience version that allows the passing in of a single argument directly.
     **/
    public static Object invoke(Object object, String methodName, Object arg0) {
        return invoke(object, methodName, new Object[] { arg0 });
    }
    public static Object invoke(Object object, String methodName) {
        return invoke(object, methodName, null);
    }
    
    /**
     * Attempt to invoke the given method with the given args.  If 
     * any exception occurs, (e.g., ClassNotFoundException because a
     * particular library isn't present) we quietly catch it and
     * pass it back as the return value;
     */
    
    public static Object executeIfFound(String className, String methodName, Object[] args) {
        try {
            return execute(className, methodName, args);
        } catch (Exception e) {
            return e;
        }
    }

    /** Encode a query pair URL such that it will work with the current platform openURL code.
     * E.g., construct a mailto: whose subject/body args are encoded such that they successfully
     * pass through to the local mail client.
     */
    public static String makeQueryURL(String urlStart, String ... nameValuePairs) {

        StringBuffer url = new StringBuffer(urlStart);

        boolean isKey = true;
        boolean isFirstKey = true;
        String curKey = "<no-key!>";
        for (String s : nameValuePairs) {
            //System.out.format("TOKEN[%s]\n", s);
            if (isKey) {
                if (isFirstKey) {
                    url.append('?');
                    isFirstKey = false;
                } else
                    url.append('&');
                curKey = s;
                System.out.format("KEY[%s]\n", s);
                url.append(s);
                url.append('=');
            } else if ("attachment".equals(curKey)) {
                // append windows pathname raw:
                // An "attachment" arg has been rumoured to work on Windows,
                // but as far as I can tell, these rumours are total BS.
                // Have yet to see this work in any configuration.
                url.append('"' + s + '"');
            } else {
                // value field:
                final String encodedValue = encodeURLData(s);
                if (DEBUG) {
                    System.out.format("    %-17s [%s]\n", curKey + "(RAW):", s);
                    System.out.format("    %-17s [%s]\n", curKey + "(ENCODED):", encodedValue);
                }
                url.append(encodedValue);
            }
            isKey = !isKey;
            
        }
        //if (DEBUG) System.out.println("QUERY URL: " + url);
        return url.toString();
    }
    
    public static String encodeURLData(String url) {
        try {
            url = URLEncoder.encode(url, "UTF-8");
            url = url.replaceAll("\\+", "%20"); // Mail clients don't understand '+' as space
        } catch (Throwable t) {
            // should never happen:
            printStackTrace(t);
        }
        return url;
    }



    // GAK!  Move this code to URLResource, as it's going to have to be smart
    // about local file drops for mac URL's, to be sure to UTF-8 DECODE them
    // first, so we get RID of special characters or spaces, which come
    // when the local file was dragged Safari, as opposed to the Finder
    // (e.g., Desktop), which already decodes them for us.

    // Also, we need to generically handle the decoding of ":" into "/"
    // for these URL's. (God forbid there is HTML in the name: e.g. <i>foo</i>
    // will send us a ':' in place of the '/', which we also don't
    // want to mistake for a protocol below.  Actually, do fix the ':'
    // in the display name (title), but not raw meta-data: could be confusing.


    private static java.lang.reflect.Method macOpenURL_Method = null;
    private static void openURL_Mac(String url, boolean isLocalFile, boolean isMailTo)
    {
        //if (DEBUG) System.err.println("openURL_Mac0 [" + url + "]");
        System.err.println("openURL_Mac0 [" + url + "], isLocal=" + isLocalFile);

        if (isLocalFile) {
            boolean changed = true;
            if (url.startsWith("file:////")) {
                // don't think we have to do this, but just in case
                // (was getting a complaint and couldn't tell if this was why or not,
                // so now we won't see it)
                url = "file:///" + url.substring(9);
            } else if (url.startsWith("/")) {
                // must have file: at head to be sure it works on mac
                url = "file:" + url;
            } else
                changed = false;
            if (DEBUG && changed) System.err.println("openURL_Mac1 [" + url + "]");
        }

        if (isMailTo) {
//             final String flatURL = url.toLowerCase();
//             final int queryIndex = flatURL.indexOf('?') + 1;
//             if (queryIndex > 1) {
//                 final String query = flatURL.substring(queryIndex);
//                 if (DEBUG) System.out.println("QUERY: [" + query + "]");
//                 final int subjectIndex = query.indexOf("subject=");
//             }
            
            //url = url.replaceAll(" ", "%20");
//             try {
//                 url = URLEncoder.encode(url, "UTF-8");
//             } catch (Throwable t) {
//                 printStackTrace(t);
//             }
        } else {
            try {
                url = encodeSpecialChars_Mac(url, isMailTo);
            } catch (Throwable t) {
                printStackTrace(t);
            }
        }

        // In Mac OS X, local files MUST have %20 and NO spaces in the URL's -- reverse of Windows.
        // But enforcing this for regular HTTP URL's breaks them: turns existing %20's into %2520's

        // If no protocol in the URL string, assume it's a pathname.  Normally, there
        // would be nothing to do as openURL handles that fine on MacOSX, unless it's
        // not absolute, it which case we make it absolute by assuming the users home
        // directory.
        
        if (!isMailTo && url.indexOf(':') < 0 && !url.startsWith("/")) {
            // Hack to make relative references relative to user home directory.  OSX
            // won't default to use current directory for a relative references, so 
            // we're prepending the home directory manually as a bail out try-for-it.
            url = "file://" + System.getProperty("user.home") + "/" + url;
            if (DEBUG) System.err.println("    OUM HOME [" + url + "]");
        }

        execMacOpenURL(url);
    }

    // WHAT WE KNOW WORKS FOR MAILTO: '@' encoded, but ? / & decoded, except WITHIN
    // the value of &field=value, which of course messes it up.
    // Also in the working state: All spaces as %20, newlines %0A
    // If we take out the colons tho, we're screwed.

    private static String encodeSpecialChars_Mac(String url, boolean isMailTo)
        throws java.io.IOException
    {
//         if (url.indexOf('%') >= 0) {

//             // If we were to make sure we're always handed URI's (at
//             // least on the mac) this would prevent accidentally
//             // decoding actual '+' characters in the file-name,
//             // as they strings would already come with %20 for spaces.

//             // However, if there was a special (e.g. Unicode) character
//             // anywhere in the string, we would still need to encode it,
//             // but then we'd have to determine if the '+' we see at this
//             // point is from the URLEncoder, or from the real File name.
//             // Eventually, this Util code should take a real File object
//             // so we can be more sure about what we're doing in these
//             // corner cases.

//             if (DEBUG) System.err.println("  NO-CLEANUP [" + url + "]");
//             return url;
//         }
        
        // In case there are any special characters (e.g., Unicode chars) in the
        // file name, we must first encode them for MacOSX (local files only?)
        // FYI, MacOSX openURL uses UTF-8, NOT the native MacRoman encoding.
        // URLEncoder encodes EVERYTHING other than alphas tho, so we need
        // to put it back.

        // Note: using the less intense encoding of URI's may make this simpler

        if (url.indexOf('%') >= 0) {
            // DECODE it, in case there are already any encodings,
            // we don't want to double-encode.
            url = java.net.URLDecoder.decode(url, "UTF-8");
            if (DEBUG) System.err.println("  DECODE UTF [" + url + "]");
        }

        url = java.net.URLEncoder.encode(url, "UTF-8"); // URLEncoder is way overzealous...
        if (DEBUG) System.err.println("  ENCODE UTF [" + url + "]");

        // now decode the over-coded stuff -- these are all crucial characters
        // that must be present...
        url = url.replaceAll("%3A", ":");
        url = url.replaceAll("%2F", "/");
        url = url.replaceAll("%3F", "?");
        url = url.replaceAll("%3D", "=");
        url = url.replaceAll("%26", "&");

        // Mac doesn't undestand '+' means ' ' in it's openURL support method:
        // url = url.replace('+', ' ');         // nor does it understand actual spaces
        url = url.replaceAll("\\+", "%20");

        if (DEBUG) System.err.println("     CLEANUP [" + url + "]");

        return url;
    }

    private static void execMacOpenURL(String url)
    {
        if (isMacLeopard()) {
            final String cmd = "/usr/bin/open";
            try {
                if (DEBUG) System.err.println("    Leopard: " + cmd + " " + url);
                Runtime.getRuntime().exec(new String[]{ cmd, url });
            } catch (Throwable t) {
                printStackTrace(t, cmd + " " + url);
            }
        } else if (getJavaVersion() >= 1.4f) {
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

        if (DEBUG) System.err.println("execMacOpenURL returns for (" + url + ")");
        
    }

    private static boolean HasGnomeOpen = false;
    private static boolean GnomeOpenSet = false;

    private static void openURL_Unix(String url, boolean isLocalFile, boolean isMailTo)
        throws java.io.IOException
    {
        // For now we just assume Netscape is installed.

        // todo: run down list of netscape, mozilla, konqueror (KDE
        // browser), gnome version?  KDE/Gnome may have their own
        // services for getting a default browser.
        
    	/*    	 
    	The problem I had with my 2 linux installations was that even though netscape wasn't 
    	installed the distribution provided a symlink named netscape which pointed to firefox
    	however firefox couldn't interpet the netscape parameters.  So below I've modified it
    	to try to load firefox first and then if that fails load netscape, i suppose konqueror
    	should be in the mix too maybe but this is better than it was and catches alot of cases.
    	*/
    	
    	final Runtime runtime = Runtime.getRuntime();
        Process process = null;
    	int waitFor = -1;

        if (!GnomeOpenSet) {

            // test for gnome    
            try {    	    	
                process = runtime.exec("gnome-open");    	
                waitFor = process.waitFor();
            }
            catch (InterruptedException e) {    			
                e.printStackTrace();    		
            }
            catch (IOException ioe2) {
                ioe2.printStackTrace();
                waitFor = 2;
            }

            GnomeOpenSet = true;
            if (waitFor != 2)
                HasGnomeOpen = true;
        }

        if (HasGnomeOpen) {
            //open file with gnome
            if (isLocalFile) {
                url = decodeURL(url);
                
                // SMF 2008-04-30: commented this out: was ensuring that most
                // local file links would not work -- not sure what cases
                // this was handling.
                //
                //url = "file:///" + url.substring(6); 
                
                // old:
                ////jjurl = url.replaceAll(" ","\\ ");
                ////url ="\""+url+"\"";
            }

            Log.info("gnome-open " + url);

            process = runtime.exec(new String[] {"gnome-open" , url});
            //			out("process: " + process);
            InputStream stdin = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<OUTPUT>");
            while ((line = br.readLine()) != null)
                System.out.println(line);
            System.out.println("<OUTPUT>");
            try
                {
                    int exitVal = process.waitFor();
                    System.out.println("exit val : " + exitVal);
                }
            catch(InterruptedException inte)
                {
                    inte.printStackTrace();
                }
            return;
        }
        else
            waitFor=-1;
    	

        //    	kde
    	try 
            {    	    	
    		System.out.println("TESTING KDE");
    		process =runtime.exec("kfmexec");    	
    		waitFor = process.waitFor();
            } catch (InterruptedException e) 
            {    			
                e.printStackTrace();    		
            }
    	catch (IOException ioe2)
            {
    		ioe2.printStackTrace();
    		waitFor=2;
   
            }
    	
    	if (waitFor != 2)
    	{
    		if (isLocalFile)
			{
				url = decodeURL(url);
				//jjurl = url.replaceAll(" ","\\ ");
				url = "file:///" + url.substring(6);
				//url ="\""+url+"\"";
				System.out.println(url);
			}
    		//open file with kde
    		process = runtime.exec(new String[] { "kfmclient","exec", url});
    		InputStream stdin = process.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			System.out.println("<OUTPUT>");
			while ((line = br.readLine()) != null)
				System.out.println(line);
			System.out.println("<OUTPUT>");
			
			System.out.println("<OUTPUT2>");
			stdin=process.getInputStream();
			isr = new InputStreamReader(stdin);
			br = new BufferedReader(isr);
			line = null;
			
			while ((line = br.readLine()) != null)
				System.out.println(line);
			System.out.println("<OUTPUT2>");
			try
			{
				int exitVal = process.waitFor();
				System.out.println("exit val : " + exitVal);
			}
			catch(InterruptedException inte)
			{
				inte.printStackTrace();
			}
			return;
    	}
    	
    	

    	    	
    	
    	//if (waitFor == 2)
    	try
    	{
    		process = runtime.exec(new String[] { "firefox", url});
            waitFor = process.waitFor();
            System.out.println("WAIT FOR : " + waitFor);
    	}
    	catch (java.io.IOException ioe3)
    	{
    		//firefox not available try netscape instead.
    		process = Runtime.getRuntime().exec(new String[] { "netscape",
                    "-remove",
                    "'openURL('" + url + "')" });
    	} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        out("process: " + process);
        
    }

    /**
     * Execute the given system command (arg0 is command, subsequent args are command arguments).
     * @param runDirectory -- if non-null, the directory to run the command in.
     *
     * The current impl will only return up to the first 256 chars of output, and will
     * use String.trim on it, to remove any trailing newline.
     *
     * @see java.lang.Runtime
     */

    public static String getSystemCommandOutput(String[] args, String runDirectory)
    {
        String output = null;
        
        try {
            File dir = null;
            if (runDirectory != null) {
                try {
                    dir = new File(runDirectory);
                } catch (Throwable t) {
                    printStackTrace(t, "Warning: couldn't create file from: " + runDirectory);
                }
            }

            Log.info("exec " + args[0] + ": " + Arrays.asList(args) + " in dir " + dir + " (" + runDirectory + ")");
            
            final Process proc = Runtime.getRuntime().exec(args, null, dir);
            final java.io.InputStream stream = proc.getInputStream();
            final byte[] buf = new byte[256];
            final int got = stream.read(buf);
        
            output = new String(buf, 0, got).trim();
            Log.debug("exec " + args[0] + ": got output[" + output + "]");
        } catch (Throwable t) {
            printStackTrace(t, "getSystemCommandOutput");
        }

        return output;
    }


    /**
     * Fast impl of replacing %xx hexidecimal codes with actual characters (e.g., %20 is a space, %2F is '/').
     * Leaves untouched any bad hex digits, or %xx strings that represent control characters (less than space/0x20
     * or greater than tilde/0x7E).
     *
     * @return String with replaced %xx codes.  If there are no '%' characters in the input string,
     * the original String object is returned.
     */
    public static String decodeURL(final String s)
    {
        //System.out.println("DECODING " + s);
        int i = s.indexOf('%');
        if (i < 0)
            return s;
        final int len = s.length();
        final StringBuffer buf = new StringBuffer(len);
        // copy in everything we've skipped in the original string up to now
        buf.append(s.substring(0, i));
        //System.out.println("DECODE START " + buf);
        for ( ; i < len; i++) {
            final char c = s.charAt(i);
            if (c == '%' && i+2 < len) {
                final int hex1 = Character.digit(s.charAt(++i), 16);
                final int hex2 = Character.digit(s.charAt(++i), 16);
                final char charValue = (char) (hex1 * 16 + hex2);
                
                //System.out.println("Got char value " + charValue + " from " + c1 + c2);
                
                if (hex1 < 1 || hex2 < 0 || charValue < 0x20 || charValue > 0x7E) {
                    // Pass through untouched if not two good hex characters (and first can't be 0),
                    // or if result is a control character (anything less than space, or greater than '~')
                    buf.append('%');
                    buf.append(s.charAt(i-1));
                    buf.append(s.charAt(i));
                } else {
                    buf.append(charValue);
                }
            } else {
                buf.append(c);
            }
        }
        if (DEBUG) {
            System.out.println("DECODED      [" + s + "]");
            System.out.println("     TO      [" + buf + "]");
        }
        return buf.toString();
    }

    public static final Iterator EmptyIterator = new EmptyIterable();
    public static final Iterable EmptyIterable = (Iterable) EmptyIterator;

    private static final class EmptyIterable implements java.util.Iterator, java.lang.Iterable {
        public boolean hasNext() { return false; }
        public Object next() { throw new NoSuchElementException(); }
        public void remove() { throw new UnsupportedOperationException(); }
        public String toString() { return "EmptyIterator"; }
        public Iterator iterator() { return this; }
    }

    public interface Itering<T> extends java.util.Iterator<T>, Iterable<T> {}

    public static abstract class AbstractItering<T> implements Itering<T> {
        public Iterator<T> iterator() {
            return this;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    private static abstract class IndexedItering<T> extends AbstractItering<T> {
        final int length;
        int index = 0;
        IndexedItering(int len) { length = len; }
        public boolean hasNext() {
            return index < length;
        }
        // could reset index if another "instance" is requested via iterator()
    }

    public static <T> Itering<T> iterable(T o) {
        return new SingletonIterable<T>(o);
    }

    public static Itering<org.w3c.dom.Node> iterable(final org.w3c.dom.NodeList nl) {
        return new IndexedItering<org.w3c.dom.Node>(nl.getLength()) {
            public org.w3c.dom.Node next() {
                return nl.item(index++);
            }
        };
    }
    public static Itering<org.w3c.dom.Node> iterable(final org.w3c.dom.NamedNodeMap nm) {
        return new IndexedItering<org.w3c.dom.Node>(nm.getLength()) {
            public org.w3c.dom.Node next() {
                return nm.item(index++);
            }
        };
    }

    public static Itering<org.w3c.dom.Node> iterable(final org.w3c.dom.Node n) {
        return iterable(n.getChildNodes());
    }
    
    
        
    /** Convenience class: provides a single element iterator.  Is also an iterable, returning self.
     * Each request for an iterable resets us to be iterated again (not threadsafe) */
    private static final class SingletonIterable<T> implements Itering<T> {
        private final T object;
        private boolean done;
        public SingletonIterable(T o) {
            object = o;
        }
        public boolean hasNext() { return !done; }
        public T next() { if (done) throw new NoSuchElementException(); done = true; return object; }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator<T> iterator() { done = false; return this; }
        public String toString() { return "[" + object + "]"; }
        
    };



    public static <T> List<T> asList(T[] array) {
        return new ExposedArrayList(array);
    }
    
    public static <T> T[] toArray(Collection<? extends T> bag, Class<T> clazz) {
        // return (T[]) bag.toArray(); class cast exception
        // can't create a new instance of the array w/out a known type -- unreliable to pull from collection (and could be empty)
        //return bag.toArray((T[])java.lang.reflect.Array.newInstance(?.getClass(), bag.size()));
        return bag.toArray((T[])java.lang.reflect.Array.newInstance(clazz, bag.size()));
    }
    
    /**
     * Identical to Arrays.asList, except that toArray() returns the internal array,
     * which allows for Collection.addAll(ExposedArrayList) to be used w/out triggering an array clone
     */
    private static final class ExposedArrayList<E> extends AbstractList<E>
	implements RandomAccess
    {
	private final Object[] a;

	ExposedArrayList(E[] array) {
            if (array == null) throw new NullPointerException();
	    a = array;
	}

        @Override
	public int size() { return a.length; }

        /** returns the internal array -- allows for Collection.addAll(ExposedArrayList) to be called w/out triggering a clone */
        @Override
	public Object[] toArray() { return a; }

        @Override
	public E get(int index) { return (E)a[index]; }

        @Override
	public E set(int index, E element) {
	    Object oldValue = a[index];
	    a[index] = element;
	    return (E)oldValue;
	}

        @Override
        public int indexOf(Object o) {
            if (o==null) {
                for (int i=0; i<a.length; i++)
                    if (a[i]==null)
                        return i;
            } else {
                for (int i=0; i<a.length; i++)
                    if (o.equals(a[i]))
                        return i;
            }
            return -1;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }
    }

    private static final class SkipNullsArrayList<T> extends ArrayList<T> {
        SkipNullsArrayList() {}

        @Override
        public boolean add(T c) {
            if (c == null) {
                //Util.printStackTrace("null not allowed");
            } else {
                super.add(c);
            }
            return true;
        }
    }

    public static ArrayList skipNullsArrayList() {
        return new SkipNullsArrayList();
    }
    

    public static <A, T extends A> int countTypes(final Iterable<A> list, final Class<T> clazz) {
        int count = 0;
        for (A item: list)
            if (clazz.isInstance(item))
                count++;
        return count;
    }
    
    public static <A, T extends A> boolean containsOnly(final Iterable<A> list, final Class<T> clazz) {
        for (A item: list)
            if (!clazz.isInstance(item))
                return false;
        return true;
    }

    public static <T> T getFirst(Collection<T> bag)
    {
        if (bag.isEmpty())
            return null;
        
        if (bag instanceof java.util.List) {
            return ((List<T>)bag).get(0);
        } else {
            return bag.iterator().next();
        }
    }

    public static <A, T extends A> List<T> extractType
                                (final Collection<A> list,
                                 final Class<T> clazz)
    {

        final List<T> desiredType = new ArrayList(list.size());

        for (A item : list)
            if (clazz.isInstance(item))
                desiredType.add((T)item);

        return desiredType;
    }
    
    /** choose the given object for inclusion */
    public interface Picker<T> { boolean match(T o); }
    /** exclude the given object from inclusion */
    public interface Filter<T> { boolean match(T o); }
    
    public static <T> List<T> extract
        (final Collection<T> list,
         final Picker<T> picker)
    {
        final List<T> desired = new ArrayList(list.size());
        
        for (T item : list) {
            try {
                if (picker.match(item))
                    desired.add(item);
            } catch (Throwable t) {
                Log.error("picker " + tags(picker) + " failed matching " + item, t);
            }
        }

        return desired;
    }
    
    /** reverse of extract */
    public static <T> List<T> filter
        (final Collection<T> list,
         final Filter<T> filter)
    {
        final List<T> desired = new ArrayList(list.size());
        
        for (T item : list) {
            try {
                if (!filter.match(item))
                    desired.add(item);
            } catch (Throwable t) {
                Log.error("filter " + tags(filter) + " failed matching " + item, t);
            }
        }

        return desired;
    }

    /**

     * Note the hairy generic method signature.  'A' is the generic type contained in
     * an iterable that is the source of all objects for the filter.  'T' is the
     * specific type that we're looking for.  T should be a subclass of A.

     * Currently creates a type filter that requires *exact* type matching: subclasses of the type do NOT match.

     */
    public static <A, T extends A> Iterable<T> typeFilter
                                (final Class<T> clazz,
                                 final Iterable<A> iterable)
    {
        return new IteratorTypeFilter<A,T>(iterable, clazz);
    }
    
    public static <A, T extends A> Iterable<T> typeFilter
                                (final Iterable<A> iterable,
                                 final Class<T> clazz)
    {
        return new IteratorTypeFilter<A,T>(iterable, clazz);
    }

    private static final class IteratorTypeFilter<A,T extends A> extends AbstractItering<T> {

        private static final Object NEXT_NEEDED = new Object();
        private static final Object EOL = new Object();
        
        final Iterator<A> iter;
        final Class<T> clazz;
        
        Object next = NEXT_NEEDED;
        
        // todo: could also allow an array of varied types to be looked for
        public IteratorTypeFilter(Iterable<A> i, Class<T> c) {
            iter = i.iterator();
            clazz = c;
        }

        private void advance() {
            if (iter.hasNext()) {
                while (true) {
                    next = iter.next();
                    //Log.debug(" ADVANCED " + tags(next));
                    if (next != null && next.getClass() == clazz)
                        break;
//                     if (clazz.isInstance(next))
//                         break;
                    if (!iter.hasNext()) {
                        next = EOL;
                        break;
                    }
                }
            } else {
                next = EOL;
            }
        }
        
        public boolean hasNext() {
            if (next == NEXT_NEEDED)
                advance();
            //if (next == EOL) Log.debug("EOL on " + tags(iter));
            return next != EOL;
        }
        
        public T next() {
            if (next == NEXT_NEEDED)
                advance();
            if (next == EOL)
                throw new NoSuchElementException("at end of iterator " + tags(iter));
            final Object result = next;
            next = NEXT_NEEDED;
            //Log.debug("RETURNING " + tags(result));
            return (T) result;
        }
        
    };

//     /** Flatten's a Map who's values are collections: returns a key/value for each value in each collection */
//     public static final class FlatteningIterator<K,V> extends AbstractItering<Map.Entry<K,V>> {

//         final Iterator<Map.Entry<Object,Collection>> keyIterator;
        
//         /** this object returned as the result every time */
//         final KVEntry entry = new KVEntry();
        
//         Iterator valueIterator;

//         FlatteningIterator(Map<Object,Collection> map) {
//             keyIterator = map.entrySet().iterator();
//             if (keyIterator.hasNext()) {
//                 findValueIteratorAndKey();
//             } else {
//                 valueIterator = Iterators.emptyIterator();
//             }
//         }

//         void findValueIteratorAndKey() {
//             final Map.Entry e = keyIterator.next();
//             entry.key = e.getKey();
//             Collection collection = (Collection) e.getValue();
//             valueIterator = collection.iterator();
//         }

//         public boolean hasNext() {
//             return valueIterator.hasNext() || keyIterator.hasNext();
//         }

//         /** note: current impl will always return the same Map.Entry object */
//         public Map.Entry next() {
//             if (!hasNext()) throw new NoSuchElementException();
            
//             if (!valueIterator.hasNext())
//                 findValueIteratorAndKey();

//             entry.value = valueIterator.next();
            
//             return entry;
//         }
//     }
    
    
    

    /** for providing a copy of a list -- especially useful for providing a concurrency safe iteration of a list */
    public static <T> List<T> copy(java.util.List<T> list) {
        if (list instanceof java.util.ArrayList)
            return (List<T>) ((java.util.ArrayList)list).clone();
        else
            return new ArrayList(list);
    }
    

    /** usage: for (SomeObject o : reverse(someObjectCollection)) { ... } */
    public static <T> Iterable<T> reverse(java.util.Collection<T> bag) {
        if (bag instanceof List) {
            return reverse((java.util.List)bag);
        } else {
            return new ReverseArrayIterator(bag.toArray());
        }
    }
    
    /** usage: for (SomeObject o : reverse(someObjectList)) { ... } */
    public static <T> Iterable<T> reverse(java.util.List<T> list) {
        return new ReverseListIterator<T>(list);
    }
    
    /** Convenience class: provides a reversed list iteration.  List should not modified during iteration.
        Currently does NOT provide fail-fast behaviour. */
    // could provide a standard fail-fast on concurrent modification by internally using a ListIterator
    private static final class ReverseListIterator<T> implements java.util.Iterator<T>, Iterable<T> {
        private final java.util.List<T> list;
        private int index;
        public ReverseListIterator(java.util.List<T> toReverse) {
            list = toReverse;
            index = list.size();
        }
        public boolean hasNext() { return index > 0; }
        public T next() { return list.get(--index); }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator iterator() { return this; }
    };
    
    private static final class ArrayIterator implements java.util.Iterator, Iterable {
        private final Object[] array;
        private int index;
        public ArrayIterator(Object[] a) {
            array = a;
            index = 0;
        }
        public boolean hasNext() { return index < array.length; }
        public Object next() { return array[index++]; }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator iterator() { return this; }
    };

    private static final class ReverseArrayIterator implements java.util.Iterator, Iterable {
        private final Object[] array;
        private int index;
        public ReverseArrayIterator(Object[] a) {
            array = a;
            index = a.length;
        }
        public boolean hasNext() { return index > 0; }
        public Object next() { return array[--index]; }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator iterator() { return this; }
    };

    /** GroupIterator allows you to construct a new iterator that
     * will aggregate an underlying set of Iterators and/or Collections */
    public static final class GroupIterator<T> extends java.util.ArrayList
        implements java.util.Iterator, java.lang.Iterable
    {
        int iterIndex = 0;
        Iterator<T> curIter;
        
        public GroupIterator() {}
        
        public GroupIterator(Iterable<T>... iterables) {
            super.addAll(new ExposedArrayList(iterables));
        }
        
        public GroupIterator(Iterator<T>... iterators) {
            super.addAll(new ExposedArrayList(iterators));
        }
        
        public GroupIterator(Object... mixedTypes) {
            for (Object o : mixedTypes)
                add(o);
        }
        
        /**
         * Add a new Iterator or Iterable to this GroupIterator.  This can only be
         * done before iteration has started for this GroupIterator.
         * @param o an Iterator or Collection
         * @return result of super.add (ArrayList.add)
         */
        @Override
        public boolean add(Object o) {
            if (!(o instanceof Iterable) &&
                !(o instanceof Iterator))
                throw new IllegalArgumentException("GroupIterator: can only add Iterable's or Iterators: " + o);
            return super.add(o);
        }

        /**
         * Add a new Iterable to to group.  This can only be done before iteration has started.
         */
        public void add(Iterable o) {
            super.add(o);
        }
        /**
         * Add a new Iterator to to group.  This can only be done before iteration has started.
         */
        public void add(Iterator o) {
            super.add(o);
        }
        

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= size())
                    return false;
                Object next = get(iterIndex);
                if (next instanceof Iterator)
                    curIter = (Iterator) next;
                else
                    curIter = ((Iterable)next).iterator();
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            // make the call on the underlying iterator
            if (curIter.hasNext()) {
                return true;
            } else {
                curIter = null;
                return hasNext();
            }
        }

        public T next()
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
            else
                throw new IllegalStateException(this + ": no underlying iterator");
        }

        @Override
        public Iterator iterator() {
            return this;
        }
    }


    /**
     * A soft-reference map for maps containing items can be garbage
     * collected if we run low on memory (e.g., images).  If that
     * happens, items will seem to have disspeared from the cache.
     * 
     * Not all HashMap methods covered: only safe to use
     * the onces explicity implemented here.
     */
    public static class SoftMap extends HashMap {

        public synchronized Object get(Object key) {
            //out("SoftMap; get: " + key);
            Object val = super.get(key);
            if (val == null)
                return null;
            Reference ref = (Reference) val;
            val = ref.get();
            if (val == null) {
                //out("SoftMap; image was garbage collected: " + key);
                super.remove(key);
                return null;
            } else
                return val;
        }
        
        public synchronized Object put(Object key, Object value) {
            //out("SoftMap; put: " + key);
            return super.put(key, new SoftReference(value));
        }

        public synchronized boolean containsKey(Object key) {
            return get(key) != null;
        }

        public synchronized void clear() {
            Iterator i = values().iterator();
            while (i.hasNext())
                ((Reference)i.next()).clear();
            super.clear();
        }
        
    }
    
    public static void dump(Object[] a) {
        dumpArray(a);
    }
    public static void dump(Collection c) {
        dumpCollection(c);
    }
    
    public static void dump(Map<?,?> m) {
        dump(m, "");
    }

    public static void dump(Map<?,?> m, String tag) {
        if (m != null) {
            //System.out.format("Map of size: %d (%s) %s\n", m.size(), m.getClass().getName(), tag);
            System.out.format("Map of size: %d (%s) %s\n", m.size(), tag(m), tag);
            for (Map.Entry<?,?> e : m.entrySet()) {
                System.out.format("    %24s: %s\n", tags(e.getKey()), tags(e.getValue()));
            }
        } else
            System.out.println("\t<NULL MAP>");
    }

    public static void dumpArray(Object[] a) {
        if (a != null)
            dumpCollection(Arrays.asList(a));
        else
            System.out.println("\t<NULL ARRAY>");
    }
    
    public static void dumpCollection(Collection c) {
        System.out.println("collection-size(" + c.size() + "): " + tag(c));
        try {
            dumpIterator(c.iterator());
        } catch (Throwable t) {
            Log.warn("dumping " + tag(c), t);
        }
    }
    public static void dumpIterator(Iterator i) {
        int x = 0;
        while (i.hasNext()) {
            //System.out.println((x<10?" ":"") + x + ": " + i.next());
            System.out.format("\t%2d: %s\n", x, tags(i.next()));
            //System.out.format("\t%2d: \"%s\"\n", x, i.next());
            x++;
        }
    }

    public static RuntimeException wrapIfChecked(String msg, Throwable t) {
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        else
            throw wrapException(msg, t);
    }
    
    public static RuntimeException wrapException(String msg, Throwable t) {
        return new WrappedException("(" + msg + ") ", t);
    }

    private static class WrappedException extends RuntimeException {
        WrappedException(String msg, Throwable cause) {
            super(msg, cause);
        }
        @Override public String toString() {
            return getMessage() + getCause().toString();
        }
    }

//     public static void dumpURI(URI u) {
//         dumpURI(u, null);
//     }
//     public static void dumpURI(URI u, String msg) {

//         synchronized (System.out) {
//             //System.out.format("%16s URI: %s %s\n", msg, u, System.identityHashCode(u), u, msg==null?"":"("+msg+")");
//             System.out.format("%16s URI: %s @%x\n",
//                               msg==null?"":('"'+msg+'"'),
//                               u,
//                               System.identityHashCode(u));
//             dumpField("hashCode",       Integer.toHexString(u.hashCode()));
//             dumpField("scheme",		u.getScheme());
//             dumpRawField("authority",   u.getAuthority(), u.getRawAuthority());
//             dumpField("userInfo",       u.getUserInfo());
//             dumpField("host",		u.getHost());
//             if (u.getPort() != -1)
//                 dumpField("port",	u.getPort());

//             dumpRawField("path",        u.getPath(), u.getRawPath());
//             dumpRawField("query",       u.getQuery(), u.getRawQuery());
//             dumpRawField("fragment",    u.getFragment(), u.getRawFragment());
//             System.out.println("-------------------------------------------------------");
//         }
//     }

//     private static void dumpField(String label, Object value) {
//         //if (value != null)
//             System.out.format("%20s: %s\n", label, value);
//     }
    
//     private static void dumpRawField(String label, Object value, Object rawValue) {
//         dumpField(label, value);

//         if (value != null && !value.equals(rawValue) || rawValue == null && value != null)
//             dumpField("raw" + label, rawValue);
//     }

    /** center the given window on default physical screen */
    public static void centerOnScreen(java.awt.Window window)
    {
        if (getJavaVersion() >= 1.4f) {
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


    static final double sFactor = 0.9;
    public static Color darkerColor(Color c) {
        return factorColor(c, sFactor);
    }
    public static Color brighterColor(Color c) {
        return factorColor(c, 1/sFactor);
    }
    public static Color factorColor(Color c, double factor)
    {
	return new Color((int)(c.getRed()  *factor),
			 (int)(c.getGreen()*factor),
			 (int)(c.getBlue() *factor));
    }


    public static final float brightness(java.awt.Color c) {
            
        if (c == null)
            return 0;

        final int r = c.getRed();
        final int g = c.getGreen();
        final int b = c.getBlue();

        int max = (r > g) ? r : g;
        if (b > max) max = b;
            
        return ((float) max) / 255f;
    }
        
    /** @return a new color, which is a mix a given color with alpha, plus another NON alpha color */
    // ... wouldn't take much more to mix multiple alpha's, tho normally some color at bottom should
    // always be non-alpha
    public static final Color alphaMix(java.awt.Color ac, java.awt.Color c) {
        final float r = ac.getRed();
        final float g = ac.getGreen();
        final float b = ac.getBlue();
        final float alpha = ac.getAlpha();
        final float mix = 255 - alpha;

        return new Color((int) ( (r * alpha + mix * c.getRed()  ) / 255f + 0.5f ),
                         (int) ( (g * alpha + mix * c.getGreen()) / 255f + 0.5f ),
                         (int) ( (b * alpha + mix * c.getBlue() ) / 255f + 0.5f ));
    }

    public static final Color alphaColor(java.awt.Color c, float alpha) {

        final int a = (int) ((alpha * 255f) + 0.5f); // covert % alpha to 0-255
        
        final int rgba =
            (c.getRGB() & 0x00FFFFFF) | // strip existing alpha
            ((a & 0xFF) << 24); // add new alpha
        
        return new Color(rgba, true);
    }
    

    /** a JPanel that anti-aliases text */
    public static class JPanelAA extends javax.swing.JPanel {
        public JPanelAA(java.awt.LayoutManager layout) {
            super(layout, true);
        }
        public JPanelAA() {}

        public void paint(java.awt.Graphics g) {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.

            // anti-alias is default on mac, so don't do it there.
            if (!isMacPlatform())
                ((java.awt.Graphics2D)g).setRenderingHint
                    (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                     java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }

    public static class JLabelAA extends javax.swing.JLabel {
        public JLabelAA(String text) {
            super(text);
        }
        public JLabelAA() {}

        public void paintComponent(java.awt.Graphics g) {
            // anti-alias is default on mac, so don't do it there.
            if (!isMacPlatform())
                ((java.awt.Graphics2D)g).setRenderingHint
                    (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                     java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g);
        }
    }
    
    
    /** This is for testing individual components. It will display the given component in frame
     * of the given size. */
    public static JFrame displayComponent(java.awt.Component comp, int width, int height)
    {
        javax.swing.JFrame frame = new javax.swing.JFrame(objectTag(comp));
        comp.setSize(comp.getPreferredSize());
        if (comp instanceof JComponent)
            frame.setContentPane((JComponent)comp);
        else
            frame.getContentPane().add(comp);
        if (width != 0 && height != 0)
            frame.setSize(width, height);
        else
            frame.pack();
        frame.validate();
        centerOnScreen(frame);
        frame.setVisible(true);
        return frame;
    }
    
    /** This is for testing individual components. It will display the given component in frame. */
    public static JFrame displayComponent(java.awt.Component comp) {
        return displayComponent(comp, 0, 0);
    }


//     public static void screenToBlack() {
//         if (!isMacPlatform())
//             return;
//         try {
//             MacOSX.goBlack();    
//         } catch (LinkageError e) {
//             eout(e);
//         }
//     }
    
//     public static void screenFadeFromBlack() {
//         if (isMacPlatform()) {
//             try {
//                 MacOSX.fadeFromBlack();
//             } catch (LinkageError e) {
//                 eout(e);
//             }
//         }
//     }

    /** For mac platform: to keep tool windows that are frames on top
     * of the main app window, you need to go to "native" java/cocoa code.
     * @param inActionTitle - the title of a window just shown / or currently
     * undergoing a show, to make sure we fix up, even if it doesn't claim
     * to be visible yet.
     */
    public static void adjustMacWindows(String mainWindowTitleStart,
                                        String ensureShown,
                                        String ensureHidden,
                                        boolean inFullScreenMode)
    {
        if (isMacPlatform()) {
            try {
                MacOSX.adjustMacWindows(mainWindowTitleStart, ensureShown, ensureHidden, inFullScreenMode);
            } catch (LinkageError e) {
                eout(e);
            } catch (Exception e) {
                System.err.println("failed to handle mac window adjustment");
                e.printStackTrace();
            }
        }
    }

    public static String upCaseInitial(String s) {
        if (s == null || s.length() < 1)
            return s;
        final char c0 = s.charAt(0);
        if (Character.isUpperCase(c0))
            return s;
        else
            return Character.toUpperCase(c0) + s.substring(1);
    }
        
    
    public static String upperCaseWords(String s)
    {
        //if (DEBUG) out("UCW in["+s+"]");

        final StringBuilder result = new StringBuilder(s.length());
        final String[] words = s.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            //if (DEBUG) out("word" + i + "["+word+"]");
            if (word.length() < 1)
                continue;
            if (Character.isLowerCase(word.charAt(0))) {
                result.append(Character.toUpperCase(word.charAt(0)));
                word = word.substring(1);
            }
            result.append(word);
            if (i + 1 < words.length)
                result.append(' ');
        }
        //if (DEBUG) out("UCWout["+result+"]");
        return result.toString();
    }

    private static void eout(LinkageError e) {
        if (e instanceof NoSuchMethodError)
            eout((NoSuchMethodError)e);
        else if (e instanceof NoClassDefFoundError)
            eout((NoClassDefFoundError)e);
        else {
            System.err.println(e + ": problem with Mac Java/Cocoa Code");
        }
    }
    
    private static void eout(NoSuchMethodError e) {
        // If tufts.macosx.Screen get's out of date, or
        // it's library is not included in the build, we'll
        // get a NoSuchMethodError
        System.err.println(e + ": tufts.macosx.Screen needs rebuild or VUE-MacOSX.jar library not in classpath");
    }
    private static void eout(NoClassDefFoundError e) {
        // We'll get this if /System/Library/Java isn't in the classpath
        System.err.println(e + ": Not Mac OS X Platform, or VUE-MacOSX.jar and/or /System/Library/Java not in classpath");
    }


    public static Rectangle2D.Float grow(Rectangle2D.Float r, int size) {
        r.x -= size;
        r.y -= size;
        r.width += size * 2;
        r.height += size * 2;
        return r;
    }
    
    public static Rectangle2D.Float grow(Rectangle2D.Float r, float size) {
        r.x -= size;
        r.y -= size;
        r.width += size * 2;
        r.height += size * 2;
        return r;
    }
    
    /** @return true if any value (x,y,width,height) is Float.NaN.  As a single
     * Float.NaN (or Double.NaN) will corrupt any accumulation of values, they can
     * ultimately cause java Graphics2D renderings to fail completely. For this reason
     * it's important to exclude bad Rectangle's from all manner of bounds computation
     * or drawing code. */
    public static boolean isBadRect(Rectangle2D.Float r) {
        return r.x != r.x
            || r.y != r.y
            || r.width != r.width
            || r.height != r.height;
    }
    
    /** @return true if any value (x,y,width,height) is Double.NaN */
    public static boolean isBadRect(Rectangle2D.Double r) {
        return r.x != r.x
            || r.y != r.y
            || r.width != r.width
            || r.height != r.height;
    }
    
    /** @return true if any value (x,y,width,height) is Double.NaN */
    public static boolean isBadRect(Rectangle2D r) {
        return Double.isNaN(r.getX())
            || Double.isNaN(r.getY())
            || Double.isNaN(r.getWidth())
            || Double.isNaN(r.getHeight());

    }
    
    /** @return true if either value is Double.NaN */
    public static boolean isBadPoint(Point2D p) {
        return Double.isNaN(p.getX())
            || Double.isNaN(p.getY());
    }
    
    /** @return true if either value is Float.NaN */
    public static boolean isBadPoint(Point2D.Float p) {
        return p.x != p.x
            || p.y != p.y;
    }
    
    /** @return true if either value is Double.NaN */
    public static boolean isBadPoint(Point2D.Double p) {
        return p.x != p.x
            || p.y != p.y;
    }

    public static String fmt(final Shape shape) {

        final Rectangle2D r;
        final String name;
        if (shape instanceof Rectangle2D) {
            r = (Rectangle2D) shape;
            name = "Rect";
        } else {
            if (shape == null) {
                name = "Shape";
                r = null;
            } else {
                name = shape.getClass().getName();
                r = shape.getBounds2D();
            }
        }
        
        return shape == null
            ? "<null-" + name + ">"
            : String.format("%s@%08x[%7.1f,%-7.1f %5.1fx%-5.1f]",
                            name,
                            shape == null ? 0 : System.identityHashCode(shape),
                            r.getX(), r.getY(), r.getWidth(), r.getHeight());
        
    }

    public static String fmt(QuadCurve2D.Float c) {
        return String.format("QuadCurveF[%.1f,%.1f %.1f,%,1f %.1f,%.1f]",
                             c.x1, c.y1,
                             c.x2, c.y2,
                             c.ctrlx, c.ctrly);
    }
    
    public static String fmt(CubicCurve2D.Float c) {
        return String.format("CubicCurveF[%.1f,%.1f %.1f,%,1f %.1f,%.1f %.1f,%.1f]",
                             c.x1, c.y1,
                             c.x2, c.y2,
                             c.ctrlx1, c.ctrly1,
                             c.ctrlx2, c.ctrly2);
    }
    
//     public static String fmt(CubicCurve2D c) {
//         return String.format("QuadCurve[%.1f,.1f %.1f,%,1f %.1f,%.1f]",
//                              c.getX1(), c.getY1(),
//                              c.getX2(), c.getY2(),
//                              c.getCtrlX1(), c.getCtrlY1(),
//                              c.getCtrlX2(), c.getCtrlY2());
//     }
    

    public static String fmt(final java.awt.Color c) {
        if (c == null)
            return "<null-Color>";
        
        if (c.getAlpha() != 0xFF)
            return String.format("Color(%d,%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        else
            return String.format("Color(%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static String fmt(java.awt.geom.Point2D p) {
        if (p == null)
            return "<null-Point2D>";
        else
            return String.format("%.1f,%.1f", p.getX(), p.getY());
    }
    public static String fmt(java.awt.Point p) {
        if (p == null)
            return "<null-Point>";
        else
            return String.format("%d,%d", p.x, p.y);
    }

    public static String fmt(java.awt.geom.Line2D l) {
        if (l == null)
            return "<null-Line2D>";
        else
            return String.format("%.1f,%.1f -> %.1f,%.1f", l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }
    public static String fmt(java.awt.geom.Dimension2D d) {
        if (d == null)
            return "<null-Dimension>";
        else
            return String.format("[%.1f x %.1f]", d.getWidth(), d.getHeight());
    }
    

    public static void out(Object o) {
        //System.out.println((o==null?"null":o.toString()));
        Log.debug("OUT: "+(o==null?"null":o.toString()));
    }

    /*
    public static void out(String s) {
        System.out.println(s==null?"null":s);
    }
    */
    public static String out(Object[] o) {
        if (o == null)
            return "<null Object[]>";
        else
            return Arrays.asList(o).toString();
    }
    
    public static String out(java.awt.geom.Point2D p) {
        return fmt(p);
        //return oneDigitDecimal(p.getX()) + "," + oneDigitDecimal(p.getY());
        //return (float)p.getX() + "," + (float)p.getY();
    }
    
    public static String out(java.awt.Dimension d) {
        return d == null ? "null" : (d.width + "x" + d.height);
    }

    public static String out(java.awt.geom.Rectangle2D r) {
        if (r == null)
            return "<null Rectangle2D>";
        else
            return String.format("[%7.1f,%-7.1f %5.1fx%-5.1f]", r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public static String out(java.awt.Rectangle r) {
        if (r == null)
            return "<null Rectangle>";
        else
            return String.format("[%4d,%4d %-5dx%4d]", r.x, r.y, r.width, r.height);
    }
    
    public static String out(java.awt.geom.RectangularShape r) {
        
        if (r == null)
            return "<null RectangularShape>";
        
        String name = r.getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        return name + "["
            + (float)r.getX() + "," + (float)r.getY()
            + " "
            + (float)r.getWidth() + "x" + (float)r.getHeight()
            + "]"
            ;
    }
    
    public static String out(java.awt.geom.Line2D l) {
        if (l == null)
            return "<null Line2D>";
        else
            return ""
                + (float)l.getX1() + "," + (float)l.getY1()
                + " -> "
                + (float)l.getX2() + "," + (float)l.getY2()
                ;
    }

    public static void outf(org.apache.log4j.Logger logger, String format, Object ... args)
    {
        if (args == null || args.length == 0) {
            logger.debug(format);
        } else {
            try {
                logger.debug(String.format(format, args));
            } catch (Throwable t) {
                logger.warn("bad format? " + t);
                logger.debug(format);
                t.printStackTrace();
            }
        }
    }
    
    
    public static String oneDigitDecimal(double x) {
        int tenX = (int) Math.round(x * 10);
        if ((tenX / 10) * 10 == tenX)
            return new Integer(tenX / 10).toString();
        else
            return new Float( ((float)tenX) / 10f ).toString();
    }

    /** @return a friendly looking string to represent the given number of bytes: e.g.: 120k, or 3.8M.
     * for values less than zero, returns ""
     */
    
    public static String abbrevBytes(long bytes) {
        if (bytes > 1024*1024)
            return String.format("%.1fM", (bytes/(1024.0*1024)));
        else if (bytes > 1024)
            return bytes/1024 + "k";
        else if (bytes >= 0)
            return "" + bytes;
        else
            return "";
    }

    /*
    public static String displayName(Class clazz) {
        if (clazz == null)
            return "()";

        final String name = clazz.getSimpleName();
        final StringBuffer buf = new StringBuffer(name.length() + 6);

//         if (true) {
//             String[] segs = name.split("\\p{Lower}\\p{Upper}");
//             for (int i = 0; i < segs.length; i++) {
//                 buf.append(segs[i]);
//                 buf.append(' ');
//             }
//             return buf.toString();
//         }
        

        int i = 0;
        while (i < name.length() && Character.isUpperCase(name.charAt(i)))
            buf.append(name.charAt(i++));

        if (i > 1)
            buf.insert(i - 1, ' ');

        int lastAdd = 0;
        boolean inUpper = true;
        for (; i < name.length(); i++) {
            final char c = name.charAt(0);
            if (inUpper && Character.isLowerCase(c)) {
                buf.append(name, lastAdd, i - 2);
                buf.append(' ');
                buf.append(name.charAt(i - 1));
                buf.append(c);
                //inUpper = false;
            } 
        }

        return name+"{"+buf.toString() +"}";
        //return buf.toString();
    }
    */
    
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


    /** encode the given String in some charset into easily persistable UTF-8 8-bit format */
    public static String encodeUTF(String s) {
        if (s == null) return null;
        try {
            String encoded = new String(s.getBytes("UTF-8"));
            //System.out.println("ENCODED [" + encoded + "]");
            return encoded;
        } catch (java.io.UnsupportedEncodingException e) {
            // should never happen
            e.printStackTrace();
            return s;
        }
    }

    public static String encodeASCII(String s) {
        if (s == null) return null;
        try {
            // unfortunately, this doesn't encode non-ascii chars as % codes, only '?'
            String encoded = new String(s.getBytes("ASCII"));
            return encoded;
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }
    
    /** decode the given String in 8-bit UTF-8 to the default java 16-bit unicode format
        (the platform default, which varies) for display */
    public static String decodeUTF(String s) {
        if (s == null) return null;
        try {
            String decoded = new String(s.getBytes(), "UTF-8");
            //System.out.println("DECODED [" + decoded + "]");
            return decoded;
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }

    private final static String NO_CLASS_FILTER = "";


    private static final StringWriter ExceptionLog = new StringWriter(1024);
    private static final PrintWriter LogPrintWriter = new PrintWriter(ExceptionLog);

    /**
     * @eturn the contents of the exception log.  This is not a copy, it's
     * the actual log, so don't modify the contents unless you really mean
     * to.
     */
    public static StringBuffer getExceptionLog() {
        return ExceptionLog.getBuffer();
    }

    /** @return the log writer for anyone else who might want to write to it */
    public static Writer getLogWriter() {
        return LogPrintWriter;
    }
    


    /** print stack trace items only from fully qualified class names that match the given prefix */
    public static void printClassTrace(Throwable t, String prefix, String message, java.io.PrintStream pst) {

        java.awt.Toolkit.getDefaultToolkit().beep();

        final PrintWriter log = LogPrintWriter;
        
        synchronized (System.out) {
        synchronized (System.err) {
        synchronized (pst) {

            pst.print(TERM_PURPLE);
            log.println();

            if (message != null) {
                pst.println(message);
                log.println(message);
            }
            
            final String head;
            if (t.getClass().getName().equals("java.lang.Throwable"))
                head = t.getMessage();
            else
                head = t.toString();
            if (prefix == null || prefix == NO_CLASS_FILTER) {
                pst.print(head + ";");
                log.print(head + ";");
            } else {
                pst.print(head + " (stack element prefix \"" + prefix + "\")");
                log.print(head + " (stack element prefix \"" + prefix + "\")");
            }

            final long now = System.currentTimeMillis();
            final String stamp = " in " + Thread.currentThread() + " at " + now + " " + new java.util.Date(now);

            pst.print(stamp);
            log.print(stamp);

            //pst.print(TERM_CLEAR);
            
            // if (prefix == null || prefix == NO_CLASS_FILTER)
            //     prefix = "!tufts.Util.print";

            StackTraceElement[] trace = t.getStackTrace();
            int skipped = 0;
            for (int i = 0; i < trace.length; i++) {
                if (includeInTrace(trace[i], prefix)) {
                    pst.print("\n\tat " + trace[i] + " ");
                    log.print("\n\tat " + trace[i] + " ");
                } else {
                    pst.print(".");
                    log.print(".");
                }
            }
            pst.println();
            log.println();

            Throwable cause = t.getCause();
            if (cause != null) {
                //ourCause.printStackTraceAsCause(s, trace);
                pst.print(TERM_RED);
                pst.print("    CAUSE: ");
                log.print("    CAUSE: ");
                pst.print(TERM_CLEAR);
                cause.printStackTrace(pst);
                cause.printStackTrace(log);
            }
            pst.println("END " + t + "\n");
            log.println("END " + t + "\n");

            pst.print(TERM_CLEAR);
            
            
        }
        }}

        //System.exit(-1); // e.g.: enable for debugging severe stack overflows
    }

    private static boolean includeInTrace(StackTraceElement trace, String prefix) {

        final String where = trace.getClassName() + "." + trace.getMethodName();
        
        if (where.startsWith("tufts.Util.print")) // hardcoded
            return false;
        
        if (prefix == null || prefix.length() == 0)
            return true;
        
        boolean matchIsIncluded = true;
        if (prefix.charAt(0) == '!') {
            prefix = prefix.substring(1);
            matchIsIncluded = false;
        }
        
        if (where.startsWith(prefix))
            return matchIsIncluded;
        else
            return !matchIsIncluded;
    }

    public static void printClassTrace(Throwable t, String prefix) {
        printClassTrace(t, prefix, null, System.err);
    }
    
    public static void printClassTrace(String prefix, String message) {
        printClassTrace(new Throwable(message), prefix, null, System.err);
    }
    
    public static void printClassTrace(String prefix) {
        printClassTrace(prefix, "*** STACK TRACE ***");
    }
    public static void printClassTrace(Class clazz, String message) {
        printClassTrace(clazz.getName(), message);
    }

    public static void printStackTrace() {
        printClassTrace(NO_CLASS_FILTER);
    }
    
    public static void printStackTrace(Throwable t) {
        printClassTrace(t, NO_CLASS_FILTER, null, System.err);
    }
    
    public static void printStackTrace(Throwable t, String message) {
        printClassTrace(t, NO_CLASS_FILTER, message, System.err);
    }
        
    public static void printStackTrace(String message) {
        printClassTrace(new Throwable(message), NO_CLASS_FILTER);
    }
    

    public static String tag(Object o) {
        if (o == null)
            return "null";
        else
            return String.format("%s@%08x", o.getClass().getName(), System.identityHashCode(o));
    }

    public static String quote(String s) {
        final StringBuilder buf = new StringBuilder(s == null ? 2 : s.length() + 2);
        buf.append('"');
        //buf.append('\u201C');
        ////buf.append('\u201F');
        if (s != null)
            buf.append(s);
        //buf.append('\u201D');
        buf.append('"');
        return buf.toString();
    }
    
    public static String quote(String s, String color) {
        final StringBuilder buf = new StringBuilder(s == null ? 2 : s.length() + 16);
        buf.append(color);
        buf.append('"');
        if (s != null)
            buf.append(s);
        buf.append('"');
        buf.append(TERM_CLEAR);
        return buf.toString();
    }
    
    public static String color(String s, String color) {

        if (s == null || s.length() == 0)
            return s;
        
        final StringBuilder buf = new StringBuilder(s.length() + 16);
        buf.append(color);
        if (s != null)
            buf.append(s);
        buf.append(TERM_CLEAR);
        return buf.toString();
    }
    
    // special case for strings: we dont care about hashCode / type -- just return quoted
    public static String tags(String s) {
        if (s == null)
            return "null";

        //return quote((String) o);
        //return TERM_RED + quote((String)s) + TERM_CLEAR;
        return TERM_BOLD + quote((String)s) + TERM_CLEAR;
    }
    
    /**
     * Produce a "package.class@idenityHashCode[toString]" debug tag to uniquely identify
     * arbitrary objects.  Allows for toString failure, and shortens the output if the
     * toString result already includes the type (class) @ identityHashCode information.
     */
    public static String tags(Object o) {
        if (o == null)
            return "null";

        if (o instanceof java.lang.String) {
            return tags((String) o);
        }
        if (o instanceof java.lang.Number) {
            // special case for strings: we dont care about hashCode / type -- just return quoted
            return o.toString() + " (" + o.getClass().getSimpleName() + ")";
            //return TERM_RED + '"' + o.toString() + '"' + TERM_CLEAR;
        }
        
        if (o instanceof int[]) {
            int[] ia = (int[]) o;
            return String.format("int[%d]=%s", ia.length, Arrays.toString(ia));
        }
        
        if (o instanceof String[]) {
            return Arrays.toString((String[]) o);
            // String[] sa = (String[]) o;
            // return String.format("String[%d]%s", sa.length, Arrays.toString(sa));
        }
            
        final String type = o.getClass().getName();
        final String simpleType = o.getClass().getSimpleName();
        final int ident = System.identityHashCode(o);
        String txt = null;

        try {
            if (o instanceof Collection) {
                final int size = ((Collection)o).size();
                if (size != 1)
                    txt = "size=" + size;
                final Object item;
                if (size == 0)
                    item = null;
                else if (o instanceof java.util.List)
                    item = ((java.util.List)o).get(0);
                else
                    item = ((Collection)o).toArray()[0];
                if (item != null) {
                    if (txt == null)
                        txt = "";
                    else
                        txt += "; ";
                    if (size == 1) 
                        txt += "only=" + tags(item);  // note: recursion
                    else
                        txt += "first=" + tags(item);  // note: recursion
//                     if (size == 1) 
//                         txt += "; " + tags(item);
//                     else
//                         txt += "type[0]=" + item.getClass().getName();
                }
            }
            else if (o instanceof java.awt.image.BufferedImage) {
                final BufferedImage bi = (BufferedImage) o;
                final int t = bi.getTransparency();
                return String.format("BufferedImage@%08x[%dx%d %s]", ident,
                                     bi.getWidth(),
                                     bi.getHeight(),
                                     transparency(t));
            }
            else if (o instanceof org.w3c.dom.Node) {
                final org.w3c.dom.Node n = (org.w3c.dom.Node) o;
                final Object value = n.getNodeValue();
                if (value == null)
                    return String.format("%s@%08x[%s]", simpleType, ident, n.getNodeName());
                else
                    return String.format("%s@%08x[%s=%s]", simpleType, ident, n.getNodeName(), tags(n.getNodeValue())); // note: recursion
            }
            else {
                txt = o.toString();
                
                final String stdShortTag = String.format("%s@%x", simpleType, ident); 
                final String stdShortTag0 = String.format("%s@%07x", simpleType, ident);
                final String stdLongTag = String.format("%s@%x", type, ident); // default java object toString
                final String stdLongTag0 = String.format("%s@%07x", type, ident);
                
                // remove redunant type info from toString:
                
                if (txt.startsWith(stdLongTag) || txt.startsWith(stdLongTag0)) {
                    return txt;
                } else if (txt.startsWith(stdShortTag)) {
                    txt = txt.substring(stdShortTag.length());
                } else if (txt.startsWith(stdShortTag0)) {
                    txt = txt.substring(stdShortTag0.length());
                } else if (txt.startsWith(simpleType)) {
                    txt = txt.substring(simpleType.length());
                } else if (txt.startsWith(type)) {
                    txt = txt.substring(type.length());
                }

            }
        } catch (Throwable t) {
            txt = t.toString();
        }

        final String s;
        if (txt.length() > 0) {
            if (txt.length() > 2 && txt.charAt(0) == '[' && txt.charAt(txt.length() - 1) == ']') {
                // skip redundant brackets
                s = String.format("%s@%08x%s", type, ident, txt);
            } else
                s = String.format("%s@%08x[%s]", type, ident, txt);
        } else
            s = String.format("%s@%08x", type, ident);
            
        return s;
    }

    private static String transparency(int i) {
        switch (i) {
        case Transparency.OPAQUE: return "OPAQUE";
        case Transparency.BITMASK: return "BITMASK";
        case Transparency.TRANSLUCENT: return "HASALPHA";
        }
        return "*UNKOWN-TRANSPARENCY*";
    }
    
    public static String objectTag(Object o) {
        return tag(o);
    }

    
    /** @deprecated -- very old -- use String.format with width specifiers instead */
    public static String pad(char c, int wide, String s, boolean alignRight) {
        if (s.length() >= wide)
            return s;
        int pad = wide - s.length();
        StringBuilder buf = new StringBuilder(wide);
        if (alignRight == false)
            buf.append(s);
        while (pad-- > 0)
            buf.append(c);
        if (alignRight)
            buf.append(s);
        return buf.toString();
    }

    public static String pad(char c, int wide, String s) {
        return pad(c, wide, s, false);
    }
    public static String pad(int wide, String s) {
        return pad(' ', wide, s, false);
    }

    public static String toBase2(byte b) {
        StringBuilder buf = new StringBuilder(8);
        buf.append((b & (1<<7)) == 0 ? '0' : '1');
        buf.append((b & (1<<6)) == 0 ? '0' : '1');
        buf.append((b & (1<<5)) == 0 ? '0' : '1');
        buf.append((b & (1<<4)) == 0 ? '0' : '1');
        buf.append((b & (1<<3)) == 0 ? '0' : '1');
        buf.append((b & (1<<2)) == 0 ? '0' : '1');
        buf.append((b & (1<<1)) == 0 ? '0' : '1');
        buf.append((b & (1<<0)) == 0 ? '0' : '1');
	return buf.toString();
    }
    
    
    

    /**
     * For now, this just determines if DNS is available, and assumes that if it is,
     * we have external network access.  Ultimately, pinging a known "permanent",
     * high-availability host (e.g., www.google.com), would be somewhat more accurate,
     * (tho also slignly risky, as whatever you pick may in fact someday not respoond)
     * but InetAddress.isReachable is a java 1.5 API call, and VUE isn't built
     * with that yet.
     *
     * TODO: This currently not actually helpful to us: only will tell you if DNS
     * is available the first time this is run in the Java VM (VUE was started),
     * after that, the result is cached.
     */
    public static boolean isInternetReachable() {
        try {
            InetAddress result = InetAddress.getByName("www.google.com");
            return result != null;
        } catch (Throwable t) {
            return false;
        }
    }

    

    public static void main(String args[])
        throws Exception
    {

        //System.out.println("System.in: " + tags(System.in));

        if (false) {
            openURL(makeQueryURL("MAILTO:foo@foobar.com",
                                 
                                 "subject", "VUE Log Report",
                                 
                                 //"attachment", "c:\\\\foo.txt"
                                 //,
                                 
                                 "body",
                                 "I am the body.  Spic & Span?"
                                 + "\nfoo@bar.com"
                                 + "\nfile://local/file/"
                                 + "\n\\\\.psf\\foobie\\"
                                 + "\ncolons:are:no:problem"


+ "\nVUE 2007-06-25 18:32:43,187 [main] INFO   Startup; build: June 25 2007 at 1523 by sfraize on Mac OS X 10.4.10 i386 JVM 1.5.0_07-164"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   Running in Java VM: 1.5.0_11-b03; MaxMemory(-Xmx)=381.1M, CurMemory(-Xms)=1.9M"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   VUE version: 2.0 alpha-x"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   Current Working Directory: \\\\.psf\\vue"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   User/host: Scott Fraize@null"
+ "\nVUE 2007-06-25 18:32:43,197 [main] DEBUG GUI init"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF  name: Windows"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF descr: The Microsoft Windows Look and Feel"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF class: class com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
+ "\nVUE 2007-06-25 18:32:47,523 [main] DEBUG  loading disk cache..."
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  Got cache directory: C:\\Documents and Settings\\Scott Fraize\\vue_2\\cache"
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  listing disk cache..."
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  listing disk cache: done; entries=21"
+ "\nVUE 2007-06-25 18:32:47,934 [main] DEBUG  loading disk cache: done"
+ "\nVUE 2007-06-25 18:32:48,064 [main] DEBUG  loading fonts."
+ "XXXXXXX"
+ "XXXXXXX"
+ "XXXXXXX"
+ "0"
+ "1" // 2064
+ "2" // 2065

                                 + "\n\nEnd.\n"
                                 ));
            System.exit(0);
        }
                         

    
        if (args.length > 0 && "network".equals(args[0])) {
            int tries = 1;
            for (int i = 0; i < tries; i++) {
                if (i > 0)
                    Thread.sleep(2000);
                System.err.print("Internet is reachable (DNS available): ");
                System.err.println("" + isInternetReachable());
            }
        
            Enumeration nie = NetworkInterface.getNetworkInterfaces();
            while (nie.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nie.nextElement();
                System.err.println("\nNetwork Interface[" + ni + "]");
                //Enumeration ie = ni.get
            
            }
            System.exit(0);
        }

        //System.err.println("ServerSocket: " + new ServerSocket(0)); // not sensitive to network availability
      
       if (false && args.length > 0 && args[0].startsWith("-")) {
            String host = args[0].substring(1);
            InetAddress[] ips = InetAddress.getAllByName(host);
            System.out.println(host + " IP's: " + Arrays.asList(ips));
            System.exit(0);
        }
        
        
        //System.out.println("cursor16 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(16,16));
        //System.out.println("cursor24 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(24,24));
        //System.out.println("cursor32 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(32,32));
                                       //.list(System.out);

        DEBUG = true;
        // TESTING CODE
        System.out.println("Default JVM character encoding for this platform: " +
                           (new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream())).getEncoding());
            
        if (args.length > 0 && args[0].equals("-charsets")) {
            Map cs = java.nio.charset.Charset.availableCharsets();
            //System.out.println("Charsets: " + cs.values());
            Iterator i = cs.values().iterator();
            while (i.hasNext()) {
                java.nio.charset.Charset o = (java.nio.charset.Charset) i.next();
                System.out.println(o
                                   + "\t" + o.aliases()
                                   //+ " " + o.getClass()
                                   );
            }
            System.exit(0);
        }

       String test = "test"; 
        if (args.length == 1) {
            if ("test".equals(args[0])) {
                execMacOpenURL(test);
                openURL(test);
            } else
                openURL(args[0]);
            //else
            //test_OpenURL();
        } else if (args.length == 2) {
            try {
                // Even tho we tried putting blackships.jar in two different places in java.library.path, it claimed it couldn't find it.
                System.loadLibrary("blackships.jar");
                java.net.URL url = new java.net.URL("blackships/large/02_010b_DutchFamily_l.jpg");
                System.out.println("URL: " + url);
                System.out.println("CONTENT: " + url.getContent());
                // cannot build a file object from a URL
                //java.net.URL url = new java.net.URL("jar:file:/VUE/src/VUE-core.jar!/tufts/vue/images/pathway_hide_on.gif");
                //java.net.URI uri = new java.net.URI(url.toString());
                //java.io.File f = new java.io.File(uri);
                //System.out.println("File " + f + " has length " + f.length() + " exists=" + f.exists());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (args.length == 3) {
            JarFile jar = new JarFile(args[0]);
            System.out.println("Got jar " + jar);
            Enumeration e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = (JarEntry) e.nextElement();
                long size = entry.getSize();
                System.out.println("Got entry " + entry + "  size=" + size);
                if (entry.getComment() != null)
                    System.out.println("\tcomment[" + entry.getComment() + "]");
                    byte[] extra = entry.getExtra();
                    if (extra != null) {
                        System.out.println("\textra len=" + extra.length + " [" + extra + "]");
                    }
                if (entry.getName().endsWith("MANIFEST.MF")) {
                    java.io.InputStream in = jar.getInputStream(entry);
                    byte[] data = new byte[(int)size];
                    in.read(data);
                    System.out.println("Contents[" + new String(data) + "]");
                }
            }
                
        } else {
            Hashtable props = System.getProperties();
            Enumeration e = props.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                //System.out.println("[1;36m" + key + "[m=" + props.get(key));
                System.out.println(TERM_PURPLE + key + TERM_CLEAR + " " + props.get(key));
            }
        }
        
        System.exit(0);
    }

    
    
}
