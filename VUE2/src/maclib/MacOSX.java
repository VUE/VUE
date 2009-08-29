package tufts.macosx;

import tufts.Util;

import apple.awt.CWindow;
    


import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;

import java.awt.*;
import java.util.*;
import java.awt.image.BufferedImage;

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
 * @version $Revision: 1.16 $ / $Date: 2009-08-29 21:35:36 $ / $Author: mike $
 * @author Scott Fraize
 */
public class MacOSX extends tufts.macosx.MacOSX16Safe
{
    
    protected static volatile com.apple.cocoa.application.NSWindow sFullScreen;
    

    private static int DefaultColorCycleSteps = 10; // old was 32, then 8

    private static final boolean NSGone;

    static {
        if (System.getProperty("tufts.macosx.debug") != null)
            DEBUG = true;
        // 64bit Java 6 VM
        if (Util.getJavaVersion() >= 1.6) // is this actually supported on Java 1.6 Tiger?
            NSGone = true;
        else
            NSGone = false;
        //HasNS = !"64".equals(System.getProperty("sun.arch.data.model"));
    }

    public static boolean supported() {
        return !NSGone;
    }

   
   
    
    // We allow non-threadsafe access to this map, as worst case
    // simply allocates some extra icons.
    private static final Map<String,Image> IconCache = new HashMap();
    
    public static Image getIconForExtension(String ext) {
        return getIconForExtension(ext, 128);
    }
        
    public static Image getIconForExtension(String ext, int sizeRequest) {
        try {
            return fetchIconForExtension(ext, sizeRequest);
        } catch (Throwable t) {
            out("Failed to find icon for extension [" + ext + "] size " + sizeRequest);
            t.printStackTrace();
        }
        return null;
    }
    
    private static Image fetchIconForExtension(String ext, int sizeRequest)
    {
        if (NSGone) return null;
        
        final String key = ext + '.' + sizeRequest;
        
        Image image = IconCache.get(key);
        if (image != null)
            return image;
        
        com.apple.cocoa.application.NSImage nsImage = null;
        
    	if ("dir".equals(ext))
            nsImage = com.apple.cocoa.application.NSWorkspace.sharedWorkspace().iconForFile("/bin");
    	else    		
            nsImage = com.apple.cocoa.application.NSWorkspace.sharedWorkspace().iconForFileType(ext);

        //System.out.println("fetching reps for " + ext);
        if (DEBUG) Log.debug(Util.TERM_CYAN + key + Util.TERM_CLEAR + "; image w/reps: " + nsImage);

        final com.apple.cocoa.foundation.NSArray reps = nsImage.representations();
        com.apple.cocoa.foundation.NSData nsData = null;

        try {
            for (int i = 0; i < reps.count(); i++) {
                final com.apple.cocoa.application.NSImageRep rep = (com.apple.cocoa.application.NSImageRep) reps.objectAtIndex(i);
                if (rep.pixelsHigh() == sizeRequest && rep instanceof com.apple.cocoa.application.NSBitmapImageRep) {
                    nsData = ((com.apple.cocoa.application.NSBitmapImageRep)rep).TIFFRepresentation();
                    break;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (nsData == null)
            nsData = nsImage.TIFFRepresentation();

        image = NStoJavaImage(nsData);

        // see http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
        // on how to do this better/faster -- happens very rarely on the Mac tho, but need to test PC.
        if (sizeRequest != image.getHeight(null)) {
            //if (DEBUG) Log.debug(Util.TERM_RED + "scaling for key " + key + ": "
            Log.info(Util.TERM_RED + "scaling for key " + key + ": "
                     + Util.tags(image)
                     + "; from " + image.getWidth(null) + "x" + image.getHeight(null)
                     + Util.TERM_CLEAR);
            image = image.getScaledInstance(sizeRequest, sizeRequest, 0); //Image.SCALE_SMOOTH);
        }

        if (image == null)
            out("Could not generate Icon for filetype : " + ext);
        else
            IconCache.put(key, image);
        
        return image;
    }

    private static Image NStoJavaImage(com.apple.cocoa.foundation.NSData tiffData)
    {
        final byte[] data = tiffData.bytes(0, tiffData.length());
        
        if (data.length == 0)
            return null;
        
        BufferedImage bim = null;
        
        try {        	        
            bim = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            // note: byte array input streams don't need to be closed
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return bim;
    }
    
    
    public static void goBlack() {
        if (NSGone) return;
        goBlack(getFullScreenWindow());
    }

    
    public static void goBlack(com.apple.cocoa.application.NSWindow w) {
        if (NSGone) return;
        if (DEBUG) out(w + " goBlack");
        w.setAlphaValue(1f);
        w.orderFrontRegardless();
        //w.orderFront(w);

        //out("Sleeping..."); try { Thread.currentThread().sleep(1000); } catch (Throwable t) {}

    }
    
    public static void hideFSW() {
        if (NSGone) return;
        final com.apple.cocoa.application.NSWindow w = getFullScreenWindow();
        if (DEBUG) out(w + " hiding (closing)");
        w.close();
        //getFullScreenWindow().setAlphaValue(0);
    }

    public static void fadeToBlack() {
        if (NSGone) return;
        com.apple.cocoa.application.NSWindow w = getFullScreenWindow();
        if (DEBUG) out(w + " fadeToBlack");
        //w.setAlphaValue(0);
        w.orderFrontRegardless();
        cycleAlpha(0, 1);
        //cycleAlpha(0, .5f);
        //cycleAlpha(1, 0);
    }

    public static void fadeFromBlack() {
        if (DEBUG) out("fadeFromBlack");
        if (NSGone) return;
        fadeFromBlack(getFullScreenWindow());
    }
    
    public static void fadeFromBlack(com.apple.cocoa.application.NSWindow w) {
        if (NSGone) return;
        if (DEBUG) out(w + " fadeFromBlack");
        goBlack(w);
        cycleAlpha(w, 1, 0);

        // calling close is how the window becomes fully hidden (not just invisible),
        // tho setReleasedWhenClosed(false) must have been called prior or we can get a bus-error
        // accessing this again, as it may have been free'd.
        w.close();
    }

    public static void cycleAlpha(Window w, float start, float end) {
        if (NSGone) return;
        cycleAlpha(getWindow(w), start, end, DefaultColorCycleSteps);
    }
    
    private static void cycleAlpha(float start, float end) {
        if (NSGone) return;
        cycleAlpha(getFullScreenWindow(), start, end, DefaultColorCycleSteps);
    }
    
    
    private static void cycleAlpha(com.apple.cocoa.application.NSWindow w, float start, float end) {
        if (NSGone) return;
        cycleAlpha(w, start, end, DefaultColorCycleSteps);
    }

    private static void cycleAlpha(com.apple.cocoa.application.NSWindow w, float start, float end, final int steps) {
        if (NSGone) return;
        if (DEBUG) out(w + " cycleAlpha: " + start + " -> " + end + " in " + steps + " steps");
        //new Throwable("CYCLEALPHA").printStackTrace();
        float alpha;
        float delta = end - start;
        float inc = delta / steps;
        for (int i = 0; i < steps; i++) {
            alpha = start + inc * i;
            w.setAlphaValue(alpha);
            if (DEBUG) {
                out("alpha=" + alpha);
                try { Thread.sleep(100); } catch (Exception e) {} // let us watch the reps
            }
            //Thread.yield();
            try { Thread.sleep(8); } catch (Exception e) {} // 8ms wait = 125fps with theoretical render time of 0

        }
        w.setAlphaValue(end);

        // NOTE: if end value is NOT zero (it's at least slightly visible), and you
        // don't manually call w.close() on the window to hide it, it will grab all
        // mouse events, locking out everything below it.  Callers of cycleAlpha are
        // currently responsible for sorting that ...
        
        if (DEBUG) {
            out("alpha=" + end + "; cycle complete");
            //java.awt.Toolkit.getDefaultToolkit().beep();
        }
            
    }

    private static com.apple.cocoa.application.NSWindow getFullScreenWindow() {
        
        if (NSGone) return null;
        
        if (sFullScreen == null) {
            if (DEBUG) out("creating FSW:");
            final Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            final com.apple.cocoa.foundation.NSRect size;
            if (DEBUG)
                size = new com.apple.cocoa.foundation.NSRect(200,200,screen.width/2,screen.height/2);
            else
                size = new com.apple.cocoa.foundation.NSRect(0,0,screen.width,screen.height);
            sFullScreen =
                new com.apple.cocoa.application.NSWindow(size,
                             0,
                             //NSWindow.Retained,
                             //NSWindow.NonRetained,
                             com.apple.cocoa.application.NSWindow.Buffered, // WINDOW IS ALWAYS WHITE UNLESS WE USED BUFFERED
                             true);
            if (DEBUG)
                sFullScreen.setBackgroundColor(com.apple.cocoa.application.NSColor.redColor());
            else
                sFullScreen.setBackgroundColor(com.apple.cocoa.application.NSColor.blackColor());
//            sFullScreen.setBackgroundColor(NSColor.redColor());
//            out(" RED COLOR " + NSColor.redColor());
//            out("FILL COLOR " + sFullScreen.backgroundColor());
//             if (DEBUG)
//                 sFullScreen.setBackgroundColor(NSColor.redColor());
//             else
//                 sFullScreen.setBackgroundColor(NSColor.blackColor());
            sFullScreen.setHasShadow(false);
            sFullScreen.setIgnoresMouseEvents(true);
            sFullScreen.setReleasedWhenClosed(false);
            sFullScreen.setTitle("_mac_full_screen_fader"); // make sure starts with "_" (see keepWindowsOnTop)
            sFullScreen.setLevel(com.apple.cocoa.application.NSWindow.ScreenSaverWindowLevel); // this allows it over the  mac menu bar
            if (DEBUG) out(sFullScreen + "; created");
        }
        return sFullScreen;
    }

    protected static void showColorPicker() {
        //NSColorPanel cp = new NSColorPanel();
    	com.apple.cocoa.application.NSColorPanel cp = com.apple.cocoa.application.NSColorPanel.sharedColorPanel();
        cp.setShowsAlpha(true);
        cp.orderFront(cp);
    }

    public static com.apple.cocoa.application.NSWindow getMainWindow() {
        
        if (NSGone) return null;
        
        return com.apple.cocoa.application.NSApplication.sharedApplication().mainWindow();
    }
    
    public static com.apple.cocoa.application.NSMenu getMainMenu() {
        
        if (NSGone) return null;
        
        return com.apple.cocoa.application.NSApplication.sharedApplication().mainMenu();
    }

    public static void setApplicationIcon(String imageFileName) {

        if (NSGone) return;

        try {
        	com.apple.cocoa.application.NSImage icon = new com.apple.cocoa.application.NSImage(imageFileName, false);
        	com.apple.cocoa.application.NSApplication.sharedApplication().setApplicationIconImage(icon);
        } catch (LinkageError e) {
            eout(e);
        } catch (Throwable t) {
            System.err.println(imageFileName + "; " + t);
        }
    }

    private static com.apple.cocoa.application.NSMenu firstMenu = null;
    private static com.apple.cocoa.application.NSMenu cloneMenu = null;
    public static void dumpMainMenu() {
    	com.apple.cocoa.application.NSMenu m = getMainMenu();
        dumpMenu(m);
        /*
        if (firstMenu == null) {
            firstMenu = m;
            try {
                cloneMenu = (NSMenu) m.clone();
                dumpMenu(cloneMenu);
            } catch (Exception ex) {
                System.err.println(ex);
            }
            NSApplication.sharedApplication().setMainMenu(cloneMenu);
        }
        */
        //if (m != firstMenu)
            //NSApplication.sharedApplication().setMainMenu(cloneMenu);
    }
    
    public static void dumpMenu(com.apple.cocoa.application.NSMenu m) {
        System.out.println("Mac Main Menu: " + m + " visible="+com.apple.cocoa.application.NSMenu.menuBarVisible() + " hash=" + m.hashCode());
    }

    private static com.apple.cocoa.application.NSWindow MainWindow = null;
    private static com.apple.cocoa.application.NSWindow FullWindow = null;
    /*
    public static void keepWindowsOnTop() {
        keepWindowsOnTop(null, false);
    }
    */
    
    /** This method make's sure any visible windows are made as
     * children of the application main window, which keeps them
     * on top of it.  As a side effect, this functionality on the
     * mac also moves the windows along with the main window
     * @param ensureWindow is for a window that may be in the
     * process of showing, and thus claims not to be visible
     * yet, even tho it is.  We make sure to process any window
     * with this title even it claims not to be visible.
     *
     * Note that making an NSWindow a child also brings it to the
     * front, and if it's NOT already at the front via Java AWT, java will have
     * no idea the window is visible, and it won't be pretty.
     *
     * This code is very specific to the frame titles used in VUE.
     */
    public static void adjustMacWindows(final String mainWindowTitleStart,
                                        final String ensureShown,
                                        final String ensureHidden,
                                        final boolean fullScreen)
    {
        if (DEBUG) dumpWindows();
        
        com.apple.cocoa.application.NSApplication a = com.apple.cocoa.application.NSApplication.sharedApplication();
        com.apple.cocoa.foundation.NSArray windows = a.windows();
        com.apple.cocoa.application.NSWindow w;
        
        // note that the only way at moment we can recognize the
        // main vue frame & full screen window is by title,
        // so be sure not to create any titles that start with
        // the below checked strings.  (getMainWindow() is
        // returning a different object that the main vue frame)
        if (true ||MainWindow == null || FullWindow == null) {
            for (int i = 0; i < windows.count(); i++) {
                w = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);
                if (w.title().startsWith("VUE-FULL-WORKING"))
                    FullWindow = w;
                else if (w.title().startsWith(mainWindowTitleStart))
                    MainWindow = w;
            }
        }
        if (DEBUG) System.out.println("tufts.macosx.Screen.adjustMacWindows:"
                                      + "\n\tmainTitleStart is \"" + mainWindowTitleStart + '"'
                                      + "\n\tMainWindow=" + MainWindow
                                      + "\n\tFullWindow=" + FullWindow
                                      );
        for (int i = 0; i < windows.count(); i++) {
            w = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);
            if (w == MainWindow || w == FullWindow || w.title().startsWith("_"))
                continue;
            // Ordering also forces the window visible! (and doesn't tell java, of course)
            // Even when checking if visible mac java impl is putting other frames them right back
            // behind as soon as the main frame gets activated...
            //if (w != MainWindow && MainWindow != null && w.isVisible()) {

            // todo: would be nice to bring forward the "Toolbar" from a dragged JToolBar,
            // but when we go full screen, it appears invisible (prob because it's a dialog
            // child of the vue frame, and it went invisible with it...)  We can do this,
            // but VUE will have to help us specifically.
            
            if (ensureHidden != null && w.title().equals(ensureHidden)) {
                if (DEBUG) System.out.println("--- REMOVE AS CHILD OF MAIN-WINDOW: #" + i + " [" + w.title() + "]");
                MainWindow.removeChildWindow(w);
                continue;
            }

            if ((ensureShown != null && ensureShown.equals(w.title())) ||
                (w.isVisible() && w.title().length() > 0))
            {
                if (fullScreen && FullWindow != null) {
                    if (DEBUG) System.out.println("+++ ADDING AS CHILD OF FULL-SCREEN: #" + i + " [" + w.title() + "]");
                    FullWindow.addChildWindow(w, com.apple.cocoa.application.NSWindow.Above);
                } else {
                    if (DEBUG) System.out.println("+++ ADDING AS CHILD OF MAIN-WINDOW: #" + i + " [" + w.title() + "]");
                    MainWindow.addChildWindow(w, com.apple.cocoa.application.NSWindow.Above);
                }
                //w.orderFront(w); // causes some flashing
            }
                
            //if (w != MainWindow && MainWindow != null && w.isVisible())
            //    w.orderWindow(NSWindow.Above, MainWindow.windowNumber());
            // Anothing above default level puts window over other apps
            //if (!w.title().startsWith("VUE"))
            //w.setLevel(NSWindow.FloatingWindowLevel);
                //w.setLevel(NSWindow.StatusWindowLevel);
            //w.setAlphaValue(0.75f);
            //w.orderFront(w);
        }
        // Having seen the below once, we're nulling just in case.
        // ObjCJava FATAL:
        // jobjc_lookupObjCObject(): returning garbage collected java ref for objc object of class NSWindow
        // ObjCJava Exit

        w = null;
        a = null;
        windows = null;
    }

    /*
     * This sets the parent pointer, but it doesn't "attach" it
     *
    public static boolean setParentWindow(Window parent, Window child) {
        final NSWindow NSparent = getWindow(parent);
        final NSWindow NSchild = getWindow(child);
        final boolean success;

        if (NSparent != null && NSchild != null) {
            NSchild.setParentWindow(NSparent);
            if (DEBUG) out("Set parent of " + child + " to " + parent);
            success = true;
        } else {
            if (DEBUG) out("Failed to set parent of " + child + " to " + parent);
            success = false;
        }

        if (DEBUG) dumpWindows();
        
        return success;
    }
    */
    
    /** order child over parent */
    public static void orderAbove(Window parent, Window child) {
        final com.apple.cocoa.application.NSWindow NSparent = getWindow(parent);
        final com.apple.cocoa.application.NSWindow NSchild = getWindow(child);

        if (NSparent == null || NSchild == null)
            return;

        //dumpWindows();

        if (true) {
            NSchild.orderWindow(com.apple.cocoa.application.NSWindow.Above, NSparent.windowNumber());
            NSchild.orderFront("source");
            if (DEBUG) out("Ordered " + child + " over " + parent);
        } else {
            // This doesn't help our last-window-detached-from-a-parent
            // sinks bug.
            NSparent.orderWindow(com.apple.cocoa.application.NSWindow.Below, NSchild.windowNumber());
            if (DEBUG) out("Ordered " + parent + " under " + child);
        }

        //dumpWindows();
        
    }
    
    /*
    public static void orderTop(Window w) {
        final NSWindow NSwindow = getWindow(w);

        // even doing ALL OF THIS doesn't prevent the last window
        // detached from it's parent from going behind the main
        // window in VUE

        NSwindow.orderFrontRegardless();
        NSwindow.orderWindow(NSWindow.Above, 0);
        NSwindow.setLevel(NSWindow.FloatingWindowLevel); // puts on top of other apps
        NSwindow.setLevel(NSWindow.NormalWindowLevel);
            
        if (DEBUG) out("Ordered " + w + " over all other windows");
    }
    */

    private static String name(Object o) {
        return Util.objectTag(o);
    }

    private static String name(Window w) {
        return Util.objectTag(w) + "(" + w.getName() + ")";
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
        try {
        	com.apple.cocoa.application.NSWindow nsw = getWindow(w);
            if (nsw != null) {
                nsw.setBackgroundColor(com.apple.cocoa.application.NSColor.blackColor().colorWithAlphaComponent(0.0f));
                //nsw.setBackgroundColor(NSColor.blackColor().colorWithAlphaComponent(0.1f));
                //nsw.setBackgroundColor(NSColor.whiteColor().colorWithAlphaComponent(0.5f));
                nsw.setOpaque(false);
            }
        } catch (LinkageError e) {
            eout(e);
        } catch (Throwable t) {
            System.err.println("setTransparent " + w + "; " + t);
        }
    }
    
    /**
     * Set the entire contents of the given window to
     * render with the given alpha, letting underlying
     * operating system windows bleed through.
     */
    public static void setWindowAlpha(Window w, float alpha) {
        if (DEBUG) out("setAlpha " + alpha + " on " + w);
        setNSAlpha(getWindow(w), alpha);
    }

    private static void setNSAlpha(com.apple.cocoa.application.NSWindow w, float alpha) {
        if (DEBUG) out(w + " setAlpha " + alpha);
        if (w != null)
            w.setAlphaValue(alpha);
    }
    

    /**
     * Set the title on the underlying NSWindow.  This
     * will have no effect on the java displayed window,
     * (unless it's a Frame), and is mainly for debug tracking.
     */
    public static void setTitle(Window w, String title) {

        if (NSGone) return;
        
        try {
        	com.apple.cocoa.application.NSWindow nsw = getWindow(w);
            if (nsw != null) {
                nsw.setTitle(title);
            }
        } catch (LinkageError e) {
            eout(e);
        }
    }
    
    
    public static void setShadow(Window w, boolean hasShadow) {

        if (NSGone) return;
        
        com.apple.cocoa.application.NSWindow nsw = getWindow(w);
        if (nsw != null) {
            //if (DEBUG) out("setShadow: " + name(w) + " " + hasShadow);
            nsw.setHasShadow(hasShadow);
        }
    }

    public static void raiseToMenuLevel(Window w) {
    	com.apple.cocoa.application.NSWindow nsw = getWindow(w);

        if (nsw != null) {
            if (DEBUG) out("raiseToMenuLevel: " + name(w));
            nsw.setLevel(com.apple.cocoa.application.NSWindow.MainMenuWindowLevel);
        }
        
    }
    
    public static boolean addChildWindow(Window parent, Window child) {
        final com.apple.cocoa.application.NSWindow NSparent = getWindow(parent);
        final com.apple.cocoa.application.NSWindow NSchild = getWindow(child);
        final boolean success;

        if (NSparent != null && NSchild != null) {
            if (DEBUG) out("Attaching " + child + " to parent " + parent);
            if (NSparent == NSchild) {
                out("attempting to attach to self: " + NSparent);
                return false;
            }
           NSparent.addChildWindow(NSchild, com.apple.cocoa.application.NSWindow.Above);
            //tufts.Util.printStackTrace("addChildWindow");
            success = true;
        } else {
            if (DEBUG) out("Failed to attach " + child + " to parent " + parent);
            success = false;
        }

        //if (DEBUG) dumpWindows();

        return success;
    }

    public static boolean removeChildWindow(Window parent, Window child) {
        final com.apple.cocoa.application.NSWindow NSparent = getWindow(parent);
        final com.apple.cocoa.application.NSWindow NSchild = getWindow(child);
        final boolean success;

        if (NSparent != null && NSchild != null) {
            NSparent.removeChildWindow(NSchild);
            if (DEBUG) out("Removed " + child + " from parent " + parent);
            //tufts.Util.printStackTrace("removeChildWindow");
            success = true;
        } else {
            if (DEBUG) out("Failed to remove " + child + " from " + parent);
            success = false;
        }
        
        //if (DEBUG) dumpWindows();

        return success;
    }
    

    private static boolean isSameWindow(com.apple.cocoa.application.NSWindow macWin, Window javaWin) {
        /*
          // mac coordinates place y=0 at bottom of screen, and the y corner
          // of the window is the LOWER left corner.
        Rectangle macBounds = macWin.frame().toAWTRectangle();
        out("  macFrame " + macWin.frame());
        out(" macBounds " + macBounds);
        out("javaBounds " + javaWin.getBounds());
        out("  javaPeer " + javaWin.getPeer().getClass().getName() + " " + javaWin.getPeer());
        out("-----------");
        */

        String javaName = javaWin.getName();

        if (javaName == null && javaWin instanceof Frame)
            javaName = ((Frame)javaWin).getTitle();
        else if (javaName == null && javaWin instanceof Dialog)
            javaName = ((Dialog)javaWin).getTitle();
        
        if (javaName == null || javaName.length() == 0)
            return false;
        else
            return javaName.equals(macWin.title());

    }

    private static java.util.Map WindowMap = new java.util.HashMap();
    
    // This should be private as we don't want to export NSWindow dependencies,
    // but is public for testing right now.
    public static com.apple.cocoa.application.NSWindow getWindow(Window javaWindow) {

        if (NSGone) return null;
        
        try {
            return findWindow(javaWindow);    
        } catch (LinkageError e) {
            eout(e);
        }
        return null;
    }
    
    private static com.apple.cocoa.application.NSWindow findWindow(Window javaWindow)
    {
        if (NSGone) return null;

        com.apple.cocoa.application.NSWindow macWindow = (com.apple.cocoa.application.NSWindow) WindowMap.get(javaWindow);
        //NSWindow macWindowCached = (NSWindow) WindowMap.get(javaWindow);
        //NSWindow macWindow = null;

        if (macWindow != null) {
            //if (DEBUG) out("found cached " + Util.objectTag(macWindow) + " for " + Util.objectTag(javaWindow));
            return macWindow;
        }
        
        com.apple.cocoa.application.NSApplication a = com.apple.cocoa.application.NSApplication.sharedApplication();
        com.apple.cocoa.foundation.NSArray windows = a.windows();

        for (int i = 0; i < windows.count(); i++) {
            macWindow = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);

            if (isSameWindow(macWindow, javaWindow)) {

                if (false && DEBUG) {
                    System.err.print("matched: "); dumpWindow(macWindow, -1);
                    com.apple.cocoa.foundation.NSArray subv = macWindow.contentView().subviews();
                    for (int x = 0; x < subv.count(); x++) {
                    	com.apple.cocoa.application.NSView v = (com.apple.cocoa.application.NSView) subv.objectAtIndex(x);
                        System.out.println("\tsubview: " + v);
                    }
                }
                break;
            } else
                macWindow = null;
        }

        if (macWindow == null)
            macWindow = findWindowUsingPeer(javaWindow);
        
        /*
        if (macWindowCached != null && macWindow != macWindowCached) {
            out("CACHE WAS WRONG:"
                + "\n\t    had: " + macWindowCached
                + "\n\tcurrent: " + macWindow);
            dumpWindows();
        }
        */

        if (macWindow != null) {
            //if (macWindowCached == null) out("loading cache for " + javaWindow + " with: " + macWindow);
            //out("loading cache for " + javaWindow + " with: " + macWindow);            
            WindowMap.put(javaWindow, macWindow);
        } else {
            out("failed to find NSWindow for: " + name(javaWindow));
            dumpWindows();
        }

        return macWindow;

    }

    // We can "more reliablaly" find the window by finding the the
    // NSWindow who's view == the viewPtr from the apple peer code:
    // it's a bit messy tho because the only way to get this ptr value
    // out of the NSView itself is from the value it returns from
    // toString().
    
    private static com.apple.cocoa.application.NSWindow findWindowUsingPeer(Window javaWindow)
    {
    	com.apple.cocoa.application.NSApplication a = com.apple.cocoa.application.NSApplication.sharedApplication();
        com.apple.cocoa.foundation.NSArray windows = a.windows();
        com.apple.cocoa.application.NSWindow macWindow;

        CWindow peer = (CWindow) javaWindow.getPeer();
        String javaViewPtr = null;
        
        if (peer != null) {
            long peerViewPtr = peer.getViewPtr();
            javaViewPtr = Long.toHexString(peerViewPtr);
            //if (DEBUG) out(javaWindow + " peer.getViewPtr=" + javaViewPtr);

            for (int i = 0; i < windows.count(); i++) {
                macWindow = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);
                
                com.apple.cocoa.application.NSView view = macWindow.contentView();
                String viewPtr = view.toString();
                
                //out("matching " + javaViewPtr + " against " + viewPtr);
                
                if (viewPtr != null && viewPtr.indexOf(javaViewPtr) >= 0) {
                    if (DEBUG)
                        out("matched java peer pointer against NSView pointer: "
                            + javaViewPtr + " == " + viewPtr + " for " + Util.objectTag(javaWindow));
                    return macWindow;
                }
            }
        }
        
        if (DEBUG) {

            if (javaViewPtr == null) {
                out("can't lookup NSWindow: java window doesn't have a peer yet: " + name(javaWindow));
            } else {
                out("failed to find NSWindow with matching view pointer for:"
                    + "\n\t               javaWindow: " + name(javaWindow)
                    + "\n\tapple.awt.CWindow viewPtr: " + javaViewPtr);
            }
        }
        
        return null;
    }
    

    // getting by name only works for frames -- Window's don't set a title
    private static com.apple.cocoa.application.NSWindow getFrame(String title)
    {
    	com.apple.cocoa.application.NSApplication a = com.apple.cocoa.application.NSApplication.sharedApplication();
        com.apple.cocoa.foundation.NSArray windows = a.windows();
        com.apple.cocoa.application.NSWindow w;
        
        for (int i = 0; i < windows.count(); i++) {
            w = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);
            if (title.equals(w.title())) {
                if (DEBUG) out("found NSWindow titled \"" + title + '"');
                return w;
            }
        }
        if (DEBUG) out("failed to find NSWindow titled \"" + title + '"');
        return null;
    }

    
    public static void dumpWindows() {
    	com.apple.cocoa.application.NSApplication a = com.apple.cocoa.application.NSApplication.sharedApplication();
        com.apple.cocoa.foundation.NSArray windows = a.windows();
        for (int i = 0; i < windows.count(); i++) {
        	com.apple.cocoa.application.NSWindow w = (com.apple.cocoa.application.NSWindow) windows.objectAtIndex(i);

            dumpWindow(w, i);
            
            if (false && w.minSize().height() == 37) {
                com.apple.cocoa.foundation.NSSize minSize = new com.apple.cocoa.foundation.NSSize(w.minSize().width(), 5);
                out("\tadjusting min size to " + minSize);
                w.setMinSize(minSize);
            }
            
        }
    }

    private static void dumpWindow(com.apple.cocoa.application.NSWindow w) {
        dumpWindow(w, -1);
    }
    
    private static void dumpWindow(com.apple.cocoa.application.NSWindow w, int idx) {
    	com.apple.cocoa.application.NSView view = w.contentView();
        System.out.println("#" + (idx>9?"":" ") + idx + ": "
                           //+ " visible=" + (w.isVisible()?"true":"    ")
                           + (w.isVisible()? "visible":"       ")
                           + tufts.Util.pad(20, " [" + w.title() + "]")
                           + tufts.Util.pad(27, w.toString())
                           //+ " level=[" + w.level() + "] "
                           //+ " " + w.getClass() // always NSWindow
                           + tufts.Util.pad(32, w.frame().toString())
                           //+ " " + w.frame()
                           //+ " min=" + w.minSize()
                           //+ " contentMin=" + w.contentMinSize()
                           + Util.objectTag(view) + view //+ " super=" + view.superview()
                           // + " " + w.contentView().frame() // view frame 0 based
                           + " parent=" + w.parentWindow()
                           //+ " " + w.contentView().getClass() + w.contentView()
                           // class is always com.apple.cocoa.application.NSView
                           );
    }
    
    public static void makeMainInvisible() {
        if (NSGone) return;
        final com.apple.cocoa.application.NSWindow w = getMainWindow();
        if (DEBUG) out(w + " making main invisible");
        w.setAlphaValue(0);
    }

    public static boolean isMainInvisible() {
        if (NSGone) return false;
        return getMainWindow().alphaValue() == 0;
    }
    
    public static void fadeUpMainWindow() {
        if (NSGone) return;
        final com.apple.cocoa.application.NSWindow w = getMainWindow();
        if (DEBUG) out(w + " fadeUp main window");
        cycleAlpha(w, 0, 1);
    }
    
    public static void setMainAlpha(float alpha) {
        if (NSGone) return;
        final com.apple.cocoa.application.NSWindow w = getMainWindow();
        if (DEBUG) out(w + " setMainAlpha " + alpha);
        w.setAlphaValue(alpha);
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