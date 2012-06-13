package tufts.macosx;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;

import java.awt.*;
import java.util.*;

import tufts.Util;

// EMPTY STUB 2012: SMF 2012-05-16 13:31.37 Wednesday SFAir.local

// NOTE: This will ONLY compile on Mac OS X (or, technically, anywhere
// you have the com.apple.cocoa.* class files available).  It is for
// generating a library to be put in the lib dir so VUE can build on
// any platform.
// Scott Fraize 2005-03-27

/**
 * This class provides access to native Mac OS X functionality
 * for things such as fading the screen to black and forcing
 * child windows to stay attached to their parent.
 *
 * @version $Revision: 1.17 $ / $Date: 2009-09-09 19:43:19 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class MacOSX extends tufts.macosx.MacOSX16Safe
{
    static {
        if (System.getProperty("tufts.macosx.debug") != null)
            DEBUG = true;
    }

    public static boolean supported() {
        return false;
    }
    
    public static Image getIconForExtension(String ext) {
        return null;
    }
        
    public static Image getIconForExtension(String ext, int sizeRequest) {
        return null;
    }
    
    public static void goBlack() {}
    
    public static void hideFSW() {}

    public static void fadeToBlack() {}

    public static void fadeFromBlack() {}
    
    public static void cycleAlpha(Window w, float start, float end) {
        
    }
    
    private static void cycleAlpha(float start, float end) {
    }
    
    
    /**
     * Make the given window transparent.  Unlike setAlpha, This makes the window
     * container entirely transparent, and dos not affect window contents, which
     * are displayed normally.  This is achived by setting the NSWindow to
     * non-opaque, and setting it's background color to a color with a 100% alpha
     * value.  Note that if the java content of the window paints a background,
     * this will have no effect.  Note also that 100% transparent mac NSWindow's
     * ignore mouse clicks where they are transparent.  Also, they will generate
     * no shadow.
     */
    public static void setTransparent(Window w) {
    }
    
    /**
     * Set the entire contents of the given window to
     * render with the given alpha, letting underlying
     * operating system windows bleed through.
     */
    public static void setWindowAlpha(Window w, float alpha) {
        if (DEBUG) out("setAlpha " + alpha + " on " + w);
    }


    /**
     * Set the title on the underlying NSWindow.  This
     * will have no effect on the java displayed window,
     * (unless it's a Frame), and is mainly for debug tracking.
     */
    public static void setTitle(Window w, String title) {
    }
    
    
    public static void setShadow(Window w, boolean hasShadow) {
    }

    public static void raiseToMenuLevel(Window w) {
    }
    
    public static boolean addChildWindow(Window parent, Window child) {
        return false;
    }

    public static boolean removeChildWindow(Window parent, Window child) {
        return false;
    }
    

    public static void makeMainInvisible() {}

    // public static boolean isMainInvisible() {
    //     if (NSGone) return false;
    //     return getMainWindow().alphaValue() == 0;
    // }
    
    public static void fadeUpMainWindow() {}
    
    // public static void setMainAlpha(float alpha) {
    //     if (NSGone) return;
    //     final com.apple.cocoa.application.NSWindow w = getMainWindow();
    //     if (DEBUG) out(w + " setMainAlpha " + alpha);
    //     w.setAlphaValue(alpha);
    // }

    public static void adjustMacWindows(final String mainWindowTitleStart,
                                        final String ensureShown,
                                        final String ensureHidden,
                                        final boolean fullScreen) {
    }
    
     private static void eout(LinkageError e) {
        if (e instanceof NoSuchMethodError)
            eout((NoSuchMethodError)e);
        else if (e instanceof NoClassDefFoundError)
            eout((NoClassDefFoundError)e);
        else {
            errout(e + ": problem locating MacOSX Java/Cocoa supper code");
        }
    }
    
    private static void eout(NoSuchMethodError e) {
        // If tufts.macosx.MacOSX get's out of date, or
        // it's library is not included in the build, we'll
        // get a NoSuchMethodError
        errout(e + "; tufts.macosx.MacOSX needs rebuild and/or VUE-MacOSX.jar needs updating");
        e.printStackTrace();
    }
    private static void eout(NoClassDefFoundError e) {
        // We'll get this if /System/Library/Java isn't in the classpath
        errout(e + ": Not MacOSX Platform or /System/Library/Java not in classpath");
        e.printStackTrace();
    }        
}