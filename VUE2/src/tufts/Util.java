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
 */

public class Util
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean MacAquaLAF = false;
    private static boolean MacAquaLAF_set = false;
    private static boolean UnixPlatform = false;
    private static float javaVersion = 1.0f;
    
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
        System.out.println("openURL_Windows[" + url + "]");

        // On at least Windows 2000, %20's don't work when given to url.dll FileProtocolHandler
        
        // Also at least Win2K: file:///C:/foo/bar.html works, but start that with file:/ or file://
        // and it DOES NOT work -- you MUST have the three slashes, OR, you can have NO SLASHES,
        // and it will work... (file:C:/foo/bar.html)
        // ALSO, file:// or // file:/// works BY ITSELF (file:/ by itself still doesn't work).
        
        url = url.replaceAll("%20", " ");
        url = url.replaceFirst("^file:/+", "file:");
        String cmd = PC_OPENURL_CMD + " " + url;
        System.out.println("exec[" + cmd + "]");
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
        if ( (url.indexOf(':') < 0) && (!(url.startsWith("/"))) ) {
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
        } catch (NoSuchMethodError e) {
            eout(e);
        } catch (NoClassDefFoundError e) {
            eout(e);
        }
    }
    
    public static void screenFadeFromBlack() {
        if (!isMacPlatform())
            return;

        try {
            tufts.macosx.Screen.fadeFromBlack();
        } catch (NoSuchMethodError e) {
            eout(e);
        } catch (NoClassDefFoundError e) {
            eout(e);
        }
    }

    private static void eout(NoSuchMethodError e) {
        // If tufts.macosx.Screen get's out of date, or
        // it's library is not included in the build, we'll
        // get a NoSuchMethodError
        System.err.println(e + ": tufts.macosx.Screen needs rebuild or VUE-MacOSX.jar library not in classpath");
    }
    private static void eout(NoClassDefFoundError e) {
        // We'll get this is /System/Library/Java isn't in the classpath
        System.err.println(e + ": Not Mac OS X Platform or /System/Library/Java not in classpath");
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
        //else
            //test_OpenURL();
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

               
        
    
}
