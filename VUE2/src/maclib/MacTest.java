package tufts.macosx;

import com.apple.cocoa.foundation.*;
import com.apple.cocoa.application.*;

import com.apple.eawt.*;

import java.awt.*;
import java.net.*;

// NOTE: This will ONLY compile on Mac OS X (or, technically, anywhere
// you have the com.apple.cocoa.* class files available).  It is for
// generating a library to be put in the lib dir so VUE can build on
// any platform.
// Scott Fraize 2005-03-27

/**
 * Mac OSX Test Code
 * @version $Revision: 1.6 $ / $Date: 2008-05-27 23:55:58 $ / $Author: sfraize $
 */
public class MacTest extends MacOSX
{
    public static void main(String args[]) {

        MacOSX.DEBUG = true;

        tufts.vue.gui.GUI.parseArgs(args);
        tufts.vue.gui.GUI.init();

        //test_fadeScreen();
        test_colorPicker(args);
        //test_movie(args);
        //test_macPanel(args);
        
    }
    
    // Okay, NSMovieView is just broken crap.  Doesn't even work in
    // an NSPanel, never mind a CocoaComponent.  Need to use
    // the quicktime.* classes -- see PlayMovie demo in QTJavaDemos.
        
    static void test_movie(String args[])
    {
        URL u = null;
        try {
            u = new URL(args[0]);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        final URL url = u;

        NSMovie movie = new NSMovie(url, false);
        out("got movie " + movie + " url=" + movie.URL());
        final NSMovieView movieView = new NSMovieView();
        movieView.setMovie(movie);
        out("got movie view " + movieView);


        String viewStr = movieView.toString();
        String viewHex = viewStr.substring(viewStr.indexOf("0x")+2, viewStr.length() - 1);
        
        out("got movie hashCode " + movieView.hashCode());
        out("got movie view hex " + viewHex);
        final long viewPtr = Long.parseLong(viewHex, 16);
        out("got movie view ptr " + viewPtr);

        NSSize movieSize = movieView.sizeForMagnification(1.0F);
        final Dimension csize = movieSize.toAWTDimension();


        //movieView.showController(false, false);
        movieView.setEditable(false);           // changes controller to simpler/standard
        movieView.setPlaysSelectionOnly(false); // can't see effect
        //movieView.setPlaysEveryFrame(true);     // won't even start, or stops after shorter bursts
        //movieView.setRate(0.5F);
        
        out("got movie size " + csize);
        out("got movie rate " + movieView.rate());
        out("isControllerVisible=" + movieView.isControllerVisible());
        
        
        //Canvas movieCanvas = new quicktime.app.display.QTCanvas() { // apparenly ONLY java 1.3!!!
        //Canvas movieCanvas = new quicktime.app.view.QTJavaCocoaCanvas() { // not accessable

        Canvas movieCanvas = new CocoaComponent() {

                {
                    out("constructed " + this);
                }

                public int createNSView() {
                    // doesn't get called, but just in case
                    return (int) createNSViewLong();
                }

                public void addNotify() {
                    out("addNotify " + this);
                    super.addNotify();
                }

                protected void processEvent(AWTEvent e) {
                    out("processEvent " + e);
                    super.processEvent(e);
                }
                
                // if this doesn't return something, createNSView will be called
                public long createNSViewLong() {
                    long ptr = viewPtr;
                    out("createNSViewLong returns " + ptr);
                    return ptr;
                }

                public void paint(Graphics g) {
                    out("paint");
                    super.paint(g);
                }
                public void update(Graphics g) {
                    out("update");
                    super.update(g);
                }

                public java.awt.Dimension getMaximumSize() { return csize; }
                public java.awt.Dimension getMinimumSize() { return csize; }
                public java.awt.Dimension getPreferredSize() { return csize; }
                
            };


        out("got canvas " + movieCanvas);

        //NSWindow main = NSApplication.sharedApplication().mainWindow();
        //out("NSApplication.mainWindow="+main);
        

        Frame frame =
            new Frame("Movie: " + url) {
                {
                    setBackground(Color.orange);
                    setLayout(new FlowLayout());
                    add(new Label("Movie: " + url));
                }
                public void paint(Graphics g) {
                    out("frame paint");
                    super.paint(g);
                }
                public void update(Graphics g) {
                    out("frame update");
                    super.update(g);
                }
                protected void processEvent(AWTEvent e) {
                    out("frame processEvent " + e);
                    super.processEvent(e);
                }
            };


        if (true) {
            frame.add(movieCanvas);
        } else {
            // Wow: NSMovieView doesn't even work in a real NSPanel
            NSPanel p = new MacPanel(true);
            p.setTitle("Movie: " + url);
            p.setContentView(movieView);
            p.orderFrontRegardless();
            KEEP.add(p);
        }

        frame.pack();
        frame.show();
        
        //NSWindow main = NSApplication.sharedApplication().mainWindow();
        //out("NSApplication.mainWindow="+main);

        KEEP.add(movie);
        KEEP.add(movieView);
        KEEP.add(movieCanvas);

        // So I bet this would work fine in an NSPanel.
        // It doesn't seem to like being in a java Frame.

        dumpWindows();

        //movieView.start(null);
        
    }
    
    static void test_colorPicker(String args[])
    {
        DEBUG=true;
        showColorPicker();
        try { Thread.sleep(5000); } catch (Exception e) {}
        
        /*
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
        if (main == null) {
            System.out.println("COULDN'T FIND MAIN WINDOW!");
        } else {
            main.setBackgroundColor(NSColor.redColor());
            main.setAlphaValue(0.5f);
        }
        fadeFromBlack();
        //try { Thread.sleep(5000); } catch (Exception e) {}
        */
    }

        
    static void test_fadeScreen()
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
                        out("alpha="+(float)alpha);
                        nsw.setAlphaValue((float)alpha);
                        try { sleep(10); } catch (Exception e) {} // give CPU a break
                    }
                    out("alpha="+(float)alpha);
                    for (alpha = 1; alpha >= 0; alpha -= 0.02) {
                        if (alpha < 0.01)
                            alpha = 0;
                        out("alpha="+(float)alpha);
                        nsw.setAlphaValue((float)alpha);
                        try { sleep(10); } catch (Exception e) {}
                    }
                    if (!cycle)
                        return;
                }
            }
        }.start();
    }




    //private static NSPanel NSPanelDontGarbageCollectMe;
    private static java.util.Collection KEEP = new java.util.ArrayList(); // array of object to not GC

    public static class NoticeListener {
        //private static NSSelector selector = new NSSelector("notice", new Class[] { new String().getClass() });
        private static NSSelector selector = new NSSelector("notice", new Class[] { NSNotification.class });
        public static NSSelector getSelector() {
            return selector;
            //return new NSSelector("notice", new Class[] { new String().getClass() });
        }
        public static void addObserver(Object toWatch) {
            addObserver(toWatch, null);
        }
        public static void addObserver(Object toWatch, String message) {
            System.err.println("Adding observer of " + toWatch + " for messages: " + message);
            Object listener = new NoticeListener();
            KEEP.add(listener);
            NSNotificationCenter.defaultCenter().addObserver(listener, getSelector(), message, toWatch);
            //NSNotificationCenter.defaultCenter().addObserver(null, getSelector(), null, toWatch);
            //System.err.println("Added observer of " + toWatch);
        }
        public void notice(NSNotification n) {
            if (!n.name().equals(NSWindow.WindowDidUpdateNotification))
                System.out.println(n);
        }

        public static void test() {
            try {
                System.out.println("NSSelector: " + selector);
                //selector.invoke(new NoticeListener(), new Object[] { "test message" });
                selector.invoke(new NoticeListener(), new Object[] { new NSNotification("TestNotice", "AnObject") });
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    public static class MacPanel extends NSPanel {

        public Panel awtPanel;
        private Window awtWindow;

        private static int cnt = 0;
        
        MacPanel(boolean textured) {
            super(new NSRect(100*cnt,100*cnt, 200,100),
                  NSWindow.TitledWindowMask +
                  NSWindow.ClosableWindowMask +
                  //NSWindow.MiniaturizableWindowMask + // apparently not allowed w/UtilityWindows
                  NSWindow.ResizableWindowMask +
                  (textured ? NSWindow.TexturedBackgroundWindowMask : 0) +
                  //NSWindow.UnifiedTitleAndToolbarWindowMask  + // kills all deco on utility
                  NSPanel.UtilityWindowMask +
                  0,
                  NSWindow.Buffered,
                  true);

            cnt++;
            
            NSButton close = standardWindowButton(NSWindow.CloseButton);
            NSButton iconify = standardWindowButton(NSWindow.MiniaturizeButton);
            NSButton zoom = standardWindowButton(NSWindow.ZoomButton);

            
            if (textured) {
            
                if (false) {
                    // tends to lead to Bus Error / Seg Fault
                    // maybe if we tried after construction?
                    // Anyway to get the view to repack so
                    // doesn't have empty space left over?
                    iconify.removeFromSuperview();
                    zoom.removeFromSuperview();
                } else {
                    iconify.setHidden(true);
                    zoom.setHidden(true);
                }
                return;
            }

            setBackgroundColor(NSColor.brownColor().colorWithAlphaComponent(0.5f));
            //setAlphaValue(0.5f);
            setOpaque(false);
            
            //System.out.println("got button " + b + " w/image " + b.image());
            setIgnoresMouseEvents(false);
            setAcceptsMouseMovedEvents(true);
            awtWindow = tufts.vue.gui.DockWindow.getTestWindow();
            NSWindow awtNS = getWindow(awtWindow);

            awtNS.setOpaque(false);
            awtNS.setBackgroundColor(NSColor.whiteColor().colorWithAlphaComponent(0.02f));
            //awtNS.setBackgroundColor(NSColor.brownColor().colorWithAlphaComponent(0.0f));


            if (true) {
                awtWindow = null; // detach from the movement stuff
                awtNS.setHasShadow(true);
                awtNS.setLevel(NSWindow.ScreenSaverWindowLevel);
            } else {
                //awtNS.setPreservesContentDuringLiveResize(true); // does nothing for AWT
                //awtNS.setAlphaValue(0.5f);
                awtNS.setHasShadow(false);
                addChildWindow(awtNS, NSWindow.Above);
            }


        }

        public void test(int msg) { System.out.println("test " + msg); }
        public void test(String msg) { System.out.println("test " + msg); }

        public void testInvoke() {
            NSSelector method1 = new NSSelector("test", new Class[] {int.class});
            NSSelector method2 = new NSSelector("test", new Class[] {new String().getClass()});
            NSSelector method3 = new NSSelector("windowNotice", new Class[] { new String().getClass() });
            // using "String.class" to define arg list gets us a wierd mac exception
            try {
                method1.invoke(this, new Object[] { new Integer(3) });
                method2.invoke(this, new Object[] { "foo" });
                method3.invoke(this, new Object[] { "hello notification" });
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }


        public void sendEvent(com.apple.cocoa.application.NSEvent e) {
            System.out.println(e);
            super.sendEvent(e);
        }

        public void postEvent(com.apple.cocoa.application.NSEvent e, boolean tv) {
            System.out.println("post " + e + " " + tv);
            super.postEvent(e, tv);
        }

        public void setFrame(com.apple.cocoa.foundation.NSRect rect, boolean display, boolean animate) {
            System.out.println(rect);
            super.setFrame(rect, display, animate);
            // not seeing
        }

        public void setFrame(final com.apple.cocoa.foundation.NSRect r, boolean display) {
            System.out.println("setFrame " + r + " display=" + display);
            super.setFrame(r, display);
            if (awtPanel != null) {
                awtPanel.setLocation(0,0);
                awtPanel.setSize((int)r.width()-10, (int)r.height()-20);
            } else if (awtWindow != null) {
                // Must sync with AWT or java crashes in no time
                tufts.vue.VUE.invokeAfterAWT(new Runnable() {
                        public void run() {
                            awtWindow.setLocation((int)r.x(), (int)r.y());
                            awtWindow.setSize((int)r.width()-10, (int)r.height()-20);
                        }
                    });
            }
        }

        public void setContentSize(com.apple.cocoa.foundation.NSSize size) {
            System.out.println(size);
            super.setContentSize(size);
        }
    }

    public static void test_macPanel(String args[])
    {
        NoticeListener.test();
        System.out.println("MacTest - Cocoa application & foundation classes");

        tufts.vue.DEBUG.DOCK = true;
        tufts.vue.DEBUG.BOXES = true;

        /*
        javax.swing.JFrame frame = new javax.swing.JFrame("Swing Frame");
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.add(new javax.swing.JLabel("Label"));
        panel.add(new javax.swing.JButton(new tufts.vue.VueAction("Action")));
        frame.getContentPane().add(panel);
        frame.setSize(200,100);
        frame.show();
        */

        // Well, we can get AWT panels & components actually responding,
        // laying out, clicking, etc, inside an NSWindow, but not Swing components.
        // Actually, we can get a JButton to be placed & repsond to clicks, but it won't paint...


        Frame frame = new Frame("AWT Frame");
        final Panel panel = new Panel();
        //final javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.add(new Label("Label"));
        panel.setBackground(java.awt.Color.red);
        Button button = new Button("Button");
        button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed: " + e);
                    panel.setLocation(panel.getX() + 1, panel.getY() + 1);
                    panel.setSize(panel.getWidth() + 1, panel.getHeight() + 1);
                    panel.layout();
                }
            });

        panel.add(button);
        panel.add(new javax.swing.JButton(new tufts.vue.VueAction("Action")));
        frame.add(panel);
        frame.setSize(200,100);
        frame.show();

        
        // if we don't create a Java frame or window of some kind first, we get the following
        // error (note that we don't even have to show the frame):
        /*
        Exception in thread "main" NSInternalInconsistencyException: Error (1002) creating CGSWindow
        at com.apple.cocoa.application.NSWindow.orderFrontRegardless(Native Method)
        at MacTest.main(MacTest.java:22)
        */

        final NSWindow nsw =
            new NSWindow(new NSRect(100,100,200,100),
                         NSWindow.TitledWindowMask +
                         NSWindow.ClosableWindowMask +
                         NSWindow.ResizableWindowMask +
                         NSWindow.UnifiedTitleAndToolbarWindowMask +
                         //NSWindow.MiniaturizableWindowMask +
                         //NSWindow.TexturedBackgroundWindowMask +
                         0,
                         NSWindow.Buffered,
                         true);
        nsw.center();
        nsw.setTitle("An NSWindow");
        //nsw.setBackgroundColor(NSColor.brownColor());
        //nsw.setBackgroundColor(NSColor.blackColor());
        //nsw.setShowsResizeIndicator(true);
        //nsw.orderFront(nsw);
        nsw.display();
        nsw.orderFrontRegardless();

        MacPanel p = new MacPanel(false);
        NoticeListener.addObserver(p, null);

        //p.testInvoke();
        
        //p.setBackgroundColor(NSColor.brownColor());
        p.setTitle("An NSPanel");
        p.setShowsResizeIndicator(true);
        p.display();
        p.orderFrontRegardless();
        
        MacPanel textured = new MacPanel(true);
        textured.setTitle("NSPanel Textured");
        textured.orderFrontRegardless();
        
        //NSButton b = textured.standardWindowButton(NSWindow.CloseButton);
        //System.out.println("got button " + b + " w/image " + b.image());

        System.out.println("\n");
            
        dumpWindows();

        NSWindow frameWin = getWindow(frame);
        NSView frameView = frameWin.contentView();

        System.out.println("got NSView " + frameView);

        if (false) {
            frameWin.setContentView(null);
            p.setContentView(frameView);
            frame.hide();
        }

        p.makeKeyWindow();

        dumpWindows();



        KEEP.add(p);
        KEEP.add(textured);
        
        //NSPanelDontGarbageCollectMe = p;
        // This prevent's GC.  I think since main is exiting, these ref's are being GC'd, which
        // eventually closes out any NSWindows w/out a non-stack pointer (e.g., there's no
        // AWT like system that keeps references to everything).

        
    }

    

    
}
    
