package tufts.macosx;

import com.apple.cocoa.foundation.*;
import com.apple.cocoa.application.*;


import java.awt.*;

// NOTE: This will ONLY compile on Mac OS X (or, technically, anywhere
// you have the com.apple.cocoa.* class files available).  It is for
// generating a library to be put in the lib dir so VUE can build on
// any platform.
// Scott Fraize 2005-03-27

// $Header: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/maclib/Screen.java,v 1.4 2005-03-28 03:18:35 sfraize Exp $

/**
 * This class provides access to native Mac OS X functionality
 * for things such as fading the screen to black and forcing
 * child windows to stay attached to their parent.
 *
 * @author Scott Fraize
 */
public class Screen
{
    private static NSWindow sFullScreen;
    private static boolean DEBUG = false;

    static {
        if (System.getProperty("tufts.macosx.Screen.debug") != null)
            DEBUG = true;
    }

    public static void goBlack() {
        goBlack(getFullScreenWindow());
    }
    
    public static void goBlack(NSWindow w) {
        w.setAlphaValue(1);
        w.orderFrontRegardless();
        //w.orderFront(w);
    }
    
    public static void goClear() {
        getFullScreenWindow().setAlphaValue(0);
    }

    public static void fadeToBlack() {
        NSWindow w = getFullScreenWindow();
        w.setAlphaValue(0);
        w.orderFront(w);
        cycleAlpha(0, 1);
    }

    public static void fadeFromBlack() {
        fadeFromBlack(getFullScreenWindow());
    }
    
    public static void fadeFromBlack(NSWindow w) {
        goBlack(w);
        cycleAlpha(w, 1, 0);
        w.close();  // bus-error of we haven't called setReleasedWhenClosed(false)
    }

    private static void cycleAlpha(float start, float end) {
        cycleAlpha(getFullScreenWindow(), start, end, 32);
    }
    
    private static void cycleAlpha(NSWindow w, float start, float end) {
        cycleAlpha(w, start, end, 32);
    }
    
    private static void cycleAlpha(NSWindow w, float start, float end, final int steps) {
        if (DEBUG) System.out.println("cycleAlpha " + start + " -> " + end + " in " + steps + " steps");
        float alpha;
        float delta = end - start;
        float inc = delta / steps;
        for (int i = 0; i < steps; i++) {
            alpha = start + inc * i;
            w.setAlphaValue(alpha);
            //System.out.println("alpha=" + alpha);
            //try { Thread.sleep(10); } catch (Exception e) {} // give CPU a break
        }
        // if end value isn't 0, and you don't manuall close the window,
        // it will grab all mouse events, locking out everything below it!
        //if (DEBUG) { if (end == 0) end=.1f; }
        w.setAlphaValue(end);
        if (DEBUG) System.out.println("cycleAlpha complete");
    }

    private static NSWindow getFullScreenWindow() {
        if (sFullScreen == null) {
            Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            sFullScreen = new NSWindow(new NSRect(0,0,screen.width,screen.height),
                                       0,
                                       NSWindow.NonRetained,
                                       //NSWindow.Buffered,
                                       true);
            if (false&&DEBUG)
                sFullScreen.setBackgroundColor(NSColor.redColor());
            else
                sFullScreen.setBackgroundColor(NSColor.blackColor());
            sFullScreen.setLevel(NSWindow.ScreenSaverWindowLevel); // this allows it over the  mac menu bar
            sFullScreen.setHasShadow(false);
            sFullScreen.setIgnoresMouseEvents(true);
            sFullScreen.setReleasedWhenClosed(false);
            sFullScreen.setTitle("_mac_full_screen_fader"); // make sure starts with "_" (see keepWindowsOnTop)
        }
        return sFullScreen;
    }

    private static void showColorPicker() {
        //NSColorPanel cp = new NSColorPanel();
        NSColorPanel cp = NSColorPanel.sharedColorPanel();
        cp.setShowsAlpha(true);
        cp.orderFront(cp);
    }

    public static NSWindow getMainWindow() {
        return NSApplication.sharedApplication().mainWindow();
    }
    
    public static NSMenu getMainMenu() {
        return NSApplication.sharedApplication().mainMenu();
    }

    private static NSMenu firstMenu = null;
    private static NSMenu cloneMenu = null;
    public static void dumpMainMenu() {
        NSMenu m = getMainMenu();
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
    
    public static void dumpMenu(NSMenu m) {
        System.out.println("Mac Main Menu: " + m + " visible="+m.menuBarVisible() + " hash=" + m.hashCode());
    }

    private static NSWindow MainWindow = null;
    private static NSWindow FullWindow = null;
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
        
        NSApplication a = NSApplication.sharedApplication();
        NSArray windows = a.windows();
        NSWindow w;
        
        // note that the only way at moment we can recognize the
        // main vue frame & full screen window is by title,
        // so be sure not to create any titles that start with
        // the below checked strings.  (getMainWindow() is
        // returning a different object that the main vue frame)
        if (true ||MainWindow == null || FullWindow == null) {
            for (int i = 0; i < windows.count(); i++) {
                w = (NSWindow) windows.objectAtIndex(i);
                if (w.title().startsWith("VUE-FULL-WORKING"))
                    FullWindow = w;
                else if (w.title().startsWith(mainWindowTitleStart))
                    MainWindow = w;
            }
        }
        for (int i = 0; i < windows.count(); i++) {
            w = (NSWindow) windows.objectAtIndex(i);
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
            
            //if (w.title().startsWith("@")) {
            if (ensureHidden != null && w.title().equals(ensureHidden)) {
                //EXPERIMENTAL
                if (DEBUG) System.out.println("--- REMOVE AS CHILD OF MAIN-WINDOW: #" + i + " [" + w.title() + "]");
                // todo: clear the @ from the title now?  What if we already removed it once?
                MainWindow.removeChildWindow(w);
                continue;
            }

            if ((ensureShown != null && ensureShown.equals(w.title())) ||
                (w.isVisible() && w.title().length() > 0))
            {
                if (fullScreen && FullWindow != null) {
                    if (DEBUG) System.out.println("+++ ADDING AS CHILD OF FULL-SCREEN: #" + i + " [" + w.title() + "]");
                    FullWindow.addChildWindow(w, NSWindow.Above);
                } else {
                    if (DEBUG) System.out.println("+++ ADDING AS CHILD OF MAIN-WINDOW: #" + i + " [" + w.title() + "]");
                    MainWindow.addChildWindow(w, NSWindow.Above);
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
    
    public static void dumpWindows() {
        NSApplication a = NSApplication.sharedApplication();
        NSArray windows = a.windows();
        for (int i = 0; i < windows.count(); i++) {
            NSWindow w = (NSWindow) windows.objectAtIndex(i);
            System.out.println("Window #" + (i>9?"":" ") + i + ": "
                               + " visible=" + (w.isVisible()?"true":"    ")
                               + " [" + w.title() + "]\t"
                               + w
                               //+ " level=[" + w.level() + "] "
                               //+ " " + w.getClass() // always NSWindow
                               + " frame=" + w.frame()
                               + " NSView=" + w.contentView()
                               //+ " " w.contentView().getClass() // is always NSView
                               );
        }

    }
    
    public static void makeMainInvisible() {
        getMainWindow().setAlphaValue(0);
    }

    public static boolean isMainInvisible() {
        return getMainWindow().alphaValue() == 0;
    }
    
    public static void fadeUpMainWindow() {
        cycleAlpha(getMainWindow(), 0, 1);
    }
    
    public static void setMainAlpha(float alpha) {
        getMainWindow().setAlphaValue(alpha);
    }


    public static void main(String args[])
    {
        DEBUG=true;
        Frame w = new Frame("invisible");
        fadeToBlack();
        if (args.length > 0)
            w.show(); // leaves us in AWT event loop at end of main instead of exiting
        //goBlack();
        showColorPicker();
        NSOpenPanel.openPanel().orderFront(null);
        try { Thread.sleep(500); } catch (Exception e) {}
        NSWindow main = NSApplication.sharedApplication().mainWindow();
        System.out.println("mainWindow="+main);
        main.setBackgroundColor(NSColor.redColor());
        main.setAlphaValue(0.5f);
        fadeFromBlack();
        //try { Thread.sleep(5000); } catch (Exception e) {}
    }

    public static void x_main(String args[])
    {
        System.out.println("MacTest - Cocoa application & foundation classes");

        Frame w = new Frame("Hello");
        // if we don't create a Java frame or window of some kind first, we get the following
        // error (note that we don't even have to show it):
        /*
        Exception in thread "main" NSInternalInconsistencyException: Error (1002) creating CGSWindow
        at com.apple.cocoa.application.NSWindow.orderFrontRegardless(Native Method)
        at MacTest.main(MacTest.java:22)
        */

        final NSWindow nsw =
            new NSWindow(new NSRect(0,0,1600,1024),
                         //NSWindow.TitledWindowMask +
                         //NSWindow.ClosableWindowMask +
                         //NSWindow.ResizableWindowMask +
                         //NSWindow.MiniaturizableWindowMask +
                         //NSWindow.TexturedBackgroundWindowMask +
                         0,
                         NSWindow.Buffered,
                         true);
        //nsw.center();
        nsw.setTitle("My Apple Window");
        //nsw.setAlphaValue(0.75f);
        //nsw.setBackgroundColor(NSColor.brownColor());
        nsw.setBackgroundColor(NSColor.blackColor());
        //nsw.setShowsResizeIndicator(true);
        nsw.setLevel(NSWindow.ScreenSaverWindowLevel); // this allows it over the  mac menu bar
        nsw.setAlphaValue(0);
        nsw.orderFront(nsw);
        //nsw.orderFrontRegardless();
        //nsw.display();
        
        System.out.println("NSWindow=" + nsw);

        
        new Thread() {
            public void run() {
                // note that the mac conveniently does not return from the setAlphaValue
                // until it's been set and rendered onto the screen.  (So this is too
                // fast for a small window w/out big delay, but if you maximize it, it's
                // smooth).
                final boolean cycle = false;
                boolean black = false;
                while (true) {
                    if (cycle) {
                        if (black)
                            nsw.setBackgroundColor(NSColor.whiteColor());
                        else 
                            nsw.setBackgroundColor(NSColor.blackColor());
                        nsw.setAlphaValue(0);
                        nsw.display(); // must call display for a background color change to take effect
                        black = !black;
                    }
                    // for max effect, especially when fading to full white, take into
                    // account that brightness sensitivity is non-linear -- e.g.,
                    // fade to white ramps too fast in the beginning.
                    double alpha;
                    for (alpha = 0.1; alpha <= 1; alpha += 0.02) {
                        nsw.setAlphaValue((float)alpha);
                        //try { sleep(10); } catch (Exception e) {} // give CPU a break
                    }
                    System.out.println("alpha="+(float)alpha);
                    for (alpha = 1; alpha >= 0; alpha -= 0.02) {
                        if (alpha < 0.01)
                            alpha = 0;
                        nsw.setAlphaValue((float)alpha);
                        //try { sleep(10); } catch (Exception e) {}
                    }
                    System.out.println("alpha="+(float)alpha);
                    if (!cycle)
                        return;
                }
            }
        }.start();
    }

    
}
    
