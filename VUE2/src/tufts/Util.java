 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts;

import java.util.*;
import java.util.jar.*;
import java.util.prefs.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Utility class.  Provides code for determining what platform we're on,
 * platform-specific code for opening URL's, as well as various convenience
 * functions.
 *
 * @version $Revision: 1.312 $ / $Date: 2005/11/27 16:17:17 $ / $Author: sfraize $ 
 */

public class Util
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean MacAquaLAF = false;
    private static boolean MacAquaLAF_set = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    
    private static int MacMRJVersion = -1;
    
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
            String mrj = System.getProperty("mrj.version");
            int i = 0;
            while (i < mrj.length()) {
                if (!Character.isDigit(mrj.charAt(i)))
                    break;
                i++;
            }

            try {
                MacMRJVersion = Integer.parseInt(mrj.substring(0, i));
            } catch (NumberFormatException e) {
                System.err.println("mrj.version: " + e);
            }
            System.out.println("Mac mrj.version: \"" + mrj + "\" = " + MacMRJVersion);
            
        } else if (osn.indexOf("WINDOWS") >= 0) {
            WindowsPlatform = true;
            System.out.println("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }
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

    public static void openURL(String platformURL)
        throws java.io.IOException
    {
        if (isMacPlatform())
            openURL_Mac(platformURL);
        else if (isUnixPlatform())
            openURL_Unix(platformURL);
        else // default is a windows platform
            openURL_Windows(platformURL);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url)
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
        url = decodeURL(url);
        url = decodeURL(url); // run twice in case any %2520 double encoded spaces
        if (url.toLowerCase().startsWith("file:")) {
            // below works, but we're doing a full conversion to windows path names now
            //url = url.replaceFirst("^file:/+", "file:");
            url = url.replaceFirst("^file:/+", "");
            url = url.replace('/', '\\');
            char c1 = 0;
            try { c1 = url.charAt(1); } catch (StringIndexOutOfBoundsException e) {}
            if (c1 == ':' || c1 == '|') {
                // Windows drive letter specification, e.g., "C:".
                // Also, I've seen D|/dir/file.txt in a shortcut file, so we allow that too.
                // In any case, we do noting: this string should be workable to url.dll
            } else {
                // if this does NOT start with a drive specification, assume
                // the first component is a host for a windows network
                // drive, and thus we have to pre-pend \\ to it, to get \\host\file
                url = "\\\\" + url; 
            }
            // at this point, "url" is really just a local windows file
            // in full windows syntax (backslashes, drive specs, etc)
        }

        String cmd = PC_OPENURL_CMD + " " + url;
        System.err.println("exec[" + cmd + "]");
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

    /** call a named static method with the given args */
    public static Object execute(String className, String methodName, Object[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalArgumentException,
               IllegalAccessException,
               java.lang.reflect.InvocationTargetException
    {
        Class clazz = Class.forName(className);
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i].getClass();
        }
                
        java.lang.reflect.Method method = clazz.getMethod(methodName, argTypes);

        Object result = method.invoke(null, args);

        //System.out.println("Sucessfully invoked " + className + "." + methodName + ", result=" + result);
        
        return result;
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

    private static java.lang.reflect.Method macOpenURL_Method = null;
    private static void openURL_Mac(String url)
        throws java.io.IOException
    {
        // In Mac OS X, you MUST have %20 and NO spaces in the URL's -- reverse of Windows.
        // (Actually, that may only be for local files?).
        
        url = url.replaceAll(" ", "%20");
        System.err.println("openURL_Mac  [" + url + "]");
        if ( (url.indexOf(':') < 0) && (!(url.startsWith("/"))) ) {
            // Hack to make relative references relative to user home directory.
            // OSX won't default to use current directory
            // for a relative reference, so we prepend
            // the current directory manually.
            url = "file://" + System.getProperty("user.dir") + "/" + url;
            System.err.println("openURL_Mac  [" + url + "]");
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
        // For now we just assume Netscape is installed.

        // todo: run down list of netscape, mozilla, konqueror (KDE
        // browser), gnome version?  KDE/Gnome may have their own
        // services for getting a default browser.
        
        // First, attempt to open the URL in a currently running session of Netscape
        Process process = Runtime.getRuntime().exec(new String[] { "netscape",
                                                                   "-remove",
                                                                   "'openURL('" + url + "')" });
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0)	// if Netscape was not open
                Runtime.getRuntime().exec(new String[] { "netscape", url });
        } catch (InterruptedException e) {
            java.io.IOException ioe =  new java.io.IOException();
            ioe.initCause(e);
            throw ioe;
        }
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
            } else
                buf.append(c);
        }
        if (true) {
            System.out.println("DECODED      [" + s + "]");
            System.out.println("             [" + buf + "]");
        }
        return buf.toString();
    }

    
    public static java.util.Iterator EmptyIterator = new java.util.Iterator() {
            public boolean hasNext() { return false; }
            public Object next() { throw new NoSuchElementException(); }
            public void remove() { throw new UnsupportedOperationException(); }
        };

    /** Convenience class: proivdes a single element iterator */
    public static class SingleIterator implements java.util.Iterator {
        Object object;
        public SingleIterator(Object o) {
            object = o;
        }
        public boolean hasNext() { return object != null; }
        public Object next() { if (object == null) throw new NoSuchElementException(); Object o = object; object = null; return o; }
        public void remove() { throw new UnsupportedOperationException(); }
    };
    
    /** GroupIterator allows you to construct a new iterator that
     * will aggregate an underlying set of Iterators and/or Collections */
    public static class GroupIterator extends java.util.ArrayList
        implements java.util.Iterator
    {
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator() {}
        
        /** All parameters must be either instances of Iterator or Collection */
        public GroupIterator(Object i1, Object i2)
        {
            this(i1, i2, null);
        }
        /** All parameters must be either instances of Iterator or Collection */
        public GroupIterator(Object i1, Object i2, Object i3)
        {
            super(3);
            if (i1 == null || i2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            add(i1);
            add(i2);
            if (i3 != null)
                add(i3);
        }

        /**
         * Add a new Iterator or Collection to this GroupIterator.  This can only be
         * done before iteration has started for this GroupIterator.
         * @param o an Iterator or Collection
         * @return result of super.add (ArrayList.add)
         */
        public boolean add(Object o) {
            if (!(o instanceof Collection) &&
                !(o instanceof Iterator))
                throw new IllegalArgumentException("Can only add Collection or Iterator: " + o);
            return super.add(o);
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
                    curIter = ((Collection)next).iterator();
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
            else
                throw new IllegalStateException(this + ": no underlying iterator");
        }
    }

    public static void dumpCollection(Collection c) {
        System.out.println("Collection of size: " + c.size() + " (" + c.getClass().getName() + ")");
        dumpIterator(c.iterator());
    }
    public static void dumpIterator(Iterator i) {
        int x = 0;
        while (i.hasNext()) {
            System.out.println((x<10?" ":"") + x + ": " + i.next());
            x++;
        }
    }


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

    /**
     * Size a normal Window to the maximum size usable in the current
     * platform & desktop configuration, <i>without</i> using the java
     * special full-screen mode, which can't have any other windows
     * on top of it, and changes the way user input events are handled.
     * On the PC, this will just be the whole screen (bug: probably
     * not good enough if they have non-hiding menu bar set to always-
     * on-top). On the Mac, it will be adjusted for the top menu
     * bar and the dock if it's visible.
     */
    // todo: test in multi-screen environment
    public static void setFullScreen(java.awt.Window window)
    {
        java.awt.Dimension screen = window.getToolkit().getScreenSize();
        if (isMacPlatform()) {
            // mac won't layer a regular window over the menu bar, so
            // we need to limit the size
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle desktop = ge.getMaximumWindowBounds();
            out("setFullScreen: mac maximum bounds  " + out(desktop));
            if (desktop.x > 0 && desktop.x <= 4) {
                // hack for smidge of space it attempts to leave if the dock is
                // at left and auto-hiding
                desktop.width += desktop.x;
                desktop.x = 0;
            } else {
                // dock at bottom & auto-hiding
                int botgap = screen.height - (desktop.y + desktop.height);
                if (botgap > 0 && botgap <= 4) {
                    desktop.height += botgap;
                } else {
                    // dock at right & auto-hiding
                    int rtgap = screen.width - desktop.width;
                    if (rtgap > 0 && rtgap <= 4)
                        desktop.width += rtgap;
                }
            }
            out("setFullScreen: mac adjusted bounds " + out(desktop));
            window.setLocation(desktop.x, desktop.y);
            window.setSize(desktop.width, desktop.height);
        } else {
            window.setLocation(0, 0);
            window.setSize(screen.width, screen.height);
        }
        out("setFullScreen: set to " + window);
    }

    /** set window to be as off screen as possible */
    public static void setOffScreen(java.awt.Window window)
    {
        java.awt.Dimension screen = window.getToolkit().getScreenSize();
        window.setLocation(screen.width - 1, screen.height - 1);
    }
    

    /** a JPanel that anti-aliases text */
    public static class JPanel_aa extends javax.swing.JPanel {
        public JPanel_aa(java.awt.LayoutManager layout) {
            super(layout, true);
        }
        public JPanel_aa() {}

        public void paint(java.awt.Graphics g) {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }
    
    /** This is for testing individual components. It will display the given component in frame
     * of the given size. */
    public static JFrame displayComponent(javax.swing.JComponent comp, int width, int height)
    {
        javax.swing.JFrame frame = new javax.swing.JFrame(comp.getClass().getName());
        comp.setSize(comp.getPreferredSize());
        frame.setContentPane(comp);
        if (width != 0 && height != 0)
            frame.setSize(width, height);
        else
            frame.pack();
        frame.validate();
        centerOnScreen(frame);
        frame.show();
        return frame;
    }
    
    /** This is for testing individual components. It will display the given component in frame. */
    public static JFrame displayComponent(javax.swing.JComponent comp) {
        return displayComponent(comp, 0, 0);
    }

    private static JColorChooser colorChooser;
    private static Dialog colorChooserDialog;
    private static boolean colorChosen;
    /** Convience method for running a JColorChooser and collecting the result */
    public static Color runColorChooser(String title, Color c, Component chooserParent)
    {
        if (colorChooserDialog == null) {
            colorChooser = new JColorChooser();
            //colorChooser.setDragEnabled(true);
            //colorChooser.setPreviewPanel(new JLabel("FOO")); // makes it dissapear entirely, W2K/1.4.2/Metal
            if (false) {
                final JPanel np = new JPanel();
                np.add(new JLabel("Text"));
                np.setSize(new Dimension(300,100)); // will be invisible otherwise
                np.setBackground(Color.red);
                //np.setBorder(new EmptyBorder(10,10,10,10));
                //np.setBorder(new EtchedBorder());
                np.setBorder(new LineBorder(Color.black));
                np.setOpaque(true);
                np.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            System.out.println("CC " + e.getPropertyName() + "=" + e.getNewValue());
                            if (e.getPropertyName().equals("foreground"))
                                np.setBackground((Color)e.getNewValue());
                        }});
                colorChooser.setPreviewPanel(np); // also makes dissapear entirely
            }
            /*
            JComponent pp = colorChooser.getPreviewPanel();
            System.out.println("CC Preview Panel: " + pp);
            for (int i = 0; i < pp.getComponentCount(); i++)
                System.out.println("#" + i + " " + pp.getComponent(i));
            colorChooser.getPreviewPanel().add(new JLabel("FOO"));
            */
            colorChooserDialog =
                JColorChooser.createDialog(chooserParent,
                                           "Color Chooser",
                                           true,  
                                           colorChooser,
                                           new ActionListener() { public void actionPerformed(ActionEvent e)
                                               { colorChosen = true; } },
                                           null);
        }
        if (c != null)
            colorChooser.setColor(c);
        if (title != null)
            colorChooserDialog.setTitle(title);

        colorChosen = false;
        // show() blocks until a color chosen or cancled, then automatically hides the dialog:
        colorChooserDialog.show();

        JComponent pp = colorChooser.getPreviewPanel();
        System.out.println("CC Preview Panel: " + pp + " children=" + Arrays.asList(pp.getComponents()));
        for (int i = 0; i < pp.getComponentCount(); i++)
            System.out.println("#" + i + " " + pp.getComponent(i));
        
        return colorChosen ? colorChooser.getColor() : null;
    }

    public static void screenToBlack() {
        if (!isMacPlatform())
            return;
        try {
            tufts.macosx.Screen.goBlack();    
        } catch (LinkageError e) {
            eout(e);
        }
    }
    
    public static void screenFadeFromBlack() {
        if (isMacPlatform()) {
            try {
                tufts.macosx.Screen.fadeFromBlack();
            } catch (LinkageError e) {
                eout(e);
            }
        }
    }

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
                tufts.macosx.Screen.adjustMacWindows(mainWindowTitleStart, ensureShown, ensureHidden, inFullScreenMode);
            } catch (LinkageError e) {
                eout(e);
            } catch (Exception e) {
                System.err.println("failed to handle mac window adjustment");
                e.printStackTrace();
            }
        }
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

    public static void out(Object o) {
        System.out.println((o==null?"null":o.toString()));
    }

    /*
    public static void out(String s) {
        System.out.println(s==null?"null":s);
    }
    */
    public static String out(Object[] o) {
        return Arrays.asList(o).toString();
    }
    
    public static String out(java.awt.geom.Point2D p) {
        return (float)p.getX() + "," + (float)p.getY();
    }

    public static String out(java.awt.Dimension d) {
        return d.width + "x" + d.height;
    }

    public static String out(java.awt.geom.Rectangle2D r) {
        return ""
            + (float)r.getX() + "," + (float)r.getY()
            + " "
            + (float)r.getWidth() + "x" + (float)r.getHeight()
            ;
    }

    public static String out(java.awt.Rectangle r) {
        return ""
            + r.width + "x" + r.height
            + " "
            + r.x + "," + r.y
            ;
    }
    
    public static String out(java.awt.geom.RectangularShape r) {
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
        return ""
            + (float)l.getX1() + "," + (float)l.getY1()
            + " -> "
            + (float)l.getX2() + "," + (float)l.getY2()
            ;
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
            return oneDigitDecimal(bytes/(1024.0*1024)) + "M";
        else if (bytes > 1024)
            return bytes/1024 + "k";
        else if (bytes >= 0)
            return "" + bytes;
        else
            return "";
    }
    
    

    public static void main(String args[])
        throws java.io.IOException
    {
        //System.out.println("cursor16 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(16,16));
        //System.out.println("cursor24 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(24,24));
        //System.out.println("cursor32 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(32,32));
                                       //.list(System.out);

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

        
        
        if (args.length == 1) {
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
                System.out.println(key + "=" + props.get(key));
            }
        }
        
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


    /** encode the given String in some charset into easily persistable UTF-8 8-bit format */
    public static String encodeUTF(String s) {
        if (s == null) return null;
        try {
            String encoded = new String(s.getBytes("UTF-8"));
            //System.out.println("ENCODED [" + encoded + "]");
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
        
    
}
