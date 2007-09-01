package tufts.vue.gui;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.DEBUG;
import tufts.vue.MapViewer;
import tufts.vue.VueTool;
import tufts.vue.VueToolbarController;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JWindow;
import org.apache.log4j.NDC;

/**
 * Code for providing, entering and exiting VUE full screen modes.
 *
 * @version $Revision: 1.7 $ / $Date: 2007-09-01 16:18:42 $ / $Author: sfraize $
 *
 */

// This code is pretty messy as it's full of old commented out experimental workarounds
// for java limitations relating to window parentage, visibility & z-order.  The current
// code seems to be the only thing that actually works, but lots of old code is left in
// all over the place in case we have problems with this again.  I've submitted an RFC
// for Java with Sun Micro on this, which was accepted, but the desired clean
// functionality won't be in there till Java 6 at the earliest, maybe not till Java 7.
// -- SMF 2007-05-22

public class FullScreen
{
    private static boolean fullScreenMode = false;
    private static boolean fullScreenNative = false; // using native full-screen mode, that hides even mac menu bar?
    private static boolean nativeModeHidAllDockWindows;
    //private static Window fullScreenWindow;
    //private static Window cachedFSW;
    private static FSWindow FullScreenWindow;
    private static MapViewer FullScreenViewer;
    private static MapViewer FullScreenLastActiveViewer;
    //private static Container fullScreenOldParent = null;
    //private static Point fullScreenOldVUELocation;
    //private static Dimension fullScreenOldVUESize;
    //private static javax.swing.JComponent fullScreenContent;

    private static final String FULLSCREEN_NAME = "*FULLSCREEN*";

    public static final String VIEWER_NAME = "FULL";

    //private static Frame cachedFSWnative = null;


    /**
     * The special full-screen window for VUE -- overrides setVisible for special handling
     */
    public static class FSWindow extends javax.swing.JWindow
    {
        private VueMenuBar mainMenuBar;
        
        FSWindow() {
            super(VUE.getApplicationFrame());
            if (GUI.isMacBrushedMetal())
                setName(GUI.OVERRIDE_REDIRECT); // so no background flashing of the texture
            else
                setName(FULLSCREEN_NAME);

            GUI.setRootPaneNames(this, FULLSCREEN_NAME);
            GUI.setOffScreen(this);
            
            if (Util.isMacPlatform()) {
                // On the Mac, this must be shown before any DockWindows display,
                // or they won't stay on top of their parents (this is a mac bug;
                // as of 2006 anyway -- don't know if we still need this,
                // but best to leave it in -- SMF 2007-05-22).
                setVisible(true);
                setVisible(false);
            }
            setBackground(Color.black);
            VUE.addActiveListener(tufts.vue.LWMap.class, FSWindow.this);
        }

        public void activeChanged(tufts.vue.ActiveEvent e, tufts.vue.LWMap map) {
            if (fullScreenMode)
                FullScreenViewer.loadFocal(map);
        }

        private javax.swing.JMenuBar getMainMenuBar() {
            if (mainMenuBar == null) {
                // fyi, if this is ever called before all the DockWindow's have
                // been constructed, the Windows menu in the VueMenuBar
                // will be awfully sparse...
                mainMenuBar = new VueMenuBar();
            }
            return mainMenuBar;
        }

        /**
           
         * Allow a menu bar at the top for full-screen working mode on non-mac
         * platforms.  (Mac always has a menu bar at the top in any working mode, as
         * opposed to full-screen native mode).  For Windows full-screen "native" mode,
         * we need to make sure this is NOT enabled, or it will show up at the top of
         * the full-screen window during a presentation.
         
         */
        
        private void setMenuBarEnabled(boolean enabled) {
            if (GUI.isMacAqua()) {
                // we never need to do this in Mac Aqua, as the application
                // always has the Mac menu bar at the top of the screen
                return;
            }
            
            if (enabled)
                getRootPane().setJMenuBar(getMainMenuBar());
            else
                getRootPane().setJMenuBar(null);
        }
        
        /**
           
         * When set to hide, this instead sets us off-screen and forces us to be
         * non-focusable (we don't want an "invisible" window accidentally having the
         * focus).
         
         * We need to do this because all the DockWindows are children of this window,
         * (so they always stay on top of it in full-screen working mode, which is
         * currently the only way to ensure this), and as children, if we actually hid
         * this window, they'd all hide with it.

         * And when shown, we prophylacticly raise all the DockWindows, in case one of
         * the various window hierarchy display bugs in the various java platform
         * implementations left them behind something.
         
         */
        
        @Override
        public void setVisible(boolean show)
        {
            setFocusableWindowState(show);
            
            // if set we allow it to actually go invisible
            // all children will hide (hiding all the DockWindow's)
            
            if (show)
                super.setVisible(true);
            else 
                GUI.setOffScreen(this);

            if (show) // just in case
                DockWindow.raiseAll();
        }
        
    }
    
    public static boolean inFullScreen() {
        return fullScreenMode || fullScreenNative;
    }

    public static boolean inWorkingFullScreen() {
        return fullScreenMode;
    }
    public static boolean inNativeFullScreen() {
        return fullScreenNative;
        //return fullScreenNative || (DEBUG.PRESENT && inWorkingFullScreen());
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }
    
    public static synchronized void toggleFullScreen(final boolean goNative)
    {
        // TODO: getMapAt in MapTabbedPane fails returning null when, of course, MapViewer is parented out!
                
        // On the mac, the order in which the tool windows are shown (go from hidden to visible) is the
        // z-order, with the last being on top -- this INCLUDES the full-screen window, so when it get's
        // shown, all the tool windows will always go below it, (including the main VUE frame) so we have
        // have to hide/show all the tool windows each time so they come back to the front.

        // If the tool window was open when fs popped, you can get it back by hitting it's shortcuut
        // twice, hiding it then bringing it back, tho it appeared on mac that this didn't always work.

        // More Mac Problems: We need the FSW (full screen window) to be a frame so we can set a
        // jmenu-bar for the top (MRJAdapter non-active jmenu bar won't help: it's for only for when
        // there's NO window active).  But then as a sibling frame, to VUE.frame instead ofa child to it,
        // VUE.frame can appear on top of you Option-~.  Trying to move VUE.frame off screen doesn't
        // appear to be working -- maybe we could set it to zero size?  Furthermore, all the tool
        // Windows, which are children to VUE.frame, won't stay on top of the FSW after it takes focus.
        // We need to see what happens if they're frames, as they're going to need to be anyway.  (There
        // is the nice ability to Option-~ them all front/back at once, as children of the VUE.frame, if
        // they're windows tho...)

        // What about using a JDialog instead of a JFrame?  JDialog's can have
        // a parent frame AND a JMenuBar...

        boolean doBlack = (goNative || inNativeFullScreen());
        if (doBlack)
            tufts.Util.screenToBlack();

        if (fullScreenMode) {
            if (goNative && !inNativeFullScreen())
                enterFullScreenMode(true);
            else           
                exitFullScreenMode();
        } else {
            enterFullScreenMode(goNative);
        }

        if (!inWorkingFullScreen())
        {
            tufts.vue.VueToolbarController.getController()
                .setSelectedTool(tufts.vue.VueToolbarController.getController().getSelectedTool());

        }
        else if (!goNative)
        {
            tufts.vue.VueToolbarController.getController()
                .setSelectedTool(VUE.getFloatingZoomPanel().getSelectedTool());
            // Is causing event loop:
            //VUE.setActive(VueTool.class, FullScreen.class, VUE.getFloatingZoomPanel().getSelectedTool());
            
        	
        }
        
        //VUE.getActiveViewer().requestFocus();
        ////ToolWindow.adjustMacWindows();
        VueMenuBar.toggleFullScreenTools();

        if (doBlack)
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() { tufts.Util.screenFadeFromBlack();}
                });
    }

    private synchronized static void enterFullScreenMode(final boolean goNative)
    {
        NDC.push("[FS->]");

        final LWMap activeMap = VUE.getActiveMap();

        FullScreenLastActiveViewer = VUE.getActiveViewer();
        FullScreenLastActiveViewer.setFocusable(false);
        
        if (goNative) {
            // Can't use heavy weights, as they're windows that can't be seen,
            // and actually the screen goes blank on Mac OS X trying to handle this.
            javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
            // also: appear to need to do this before anything else
            // to have it property take effect?
        }

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final VueTool activeTool = VueToolbarController.getActiveTool();
        
        //out("Native full screen support available: " + device.isFullScreenSupported());
        // todo: crap: if the screen resolution changes, we'll need to resize the full-screen window


        VUE.Log.debug("Entering full screen mode; goNative=" + goNative);
        if (FullScreenWindow == null) {
            FullScreenWindow = (FSWindow) GUI.getFullScreenWindow();
            FullScreenWindow.getContentPane().add(FullScreenViewer = new MapViewer(null, VIEWER_NAME));
            //fullScreenWindow.pack();
        }
                
        FullScreenWindow.setMenuBarEnabled(!goNative);
        // FullScreenViewer.loadFocal(VUE.getActiveMap()); // can't do till we're sure it has a size!
        fullScreenMode = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;


        if (goNative) {

            // Try and prevent us from flashing a big white screen while we load.
            // Tho immediately loading the focal below will quickly override this...
            FullScreenWindow.setBackground(Color.black);
            FullScreenViewer.setBackground(Color.black);
            
            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            device.setFullScreenWindow(FullScreenWindow);

            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.

            if (!DockWindow.AllWindowsHidden()) {
                nativeModeHidAllDockWindows = true;
                DockWindow.HideAllWindows();
            }

        } else {
            
            GUI.setFullScreenVisible(FullScreenWindow);
        }
                
        FullScreenViewer.loadFocal(activeMap);
        FullScreenViewer.grabVueApplicationFocus("FullScreen.enter", null);
        
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             FullScreenViewer.grabVueApplicationFocus("FullScreen.enter", null);
//         }});
        
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             FullScreenViewer.loadFocal(activeMap);
//         }});
        
        //FullScreenViewer.grabVueApplicationFocus("FullScreen.enter", null);
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             FullScreenViewer.grabVueApplicationFocus("FullScreen.enter", null);
//             //FullScreenViewer.popToMapFocal();
//         }});
        
        
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             if (DEBUG.PRESENT) VUE.Log.debug("AWT thread full-screen viewer loading map " + activeMap);
//             FullScreenViewer.loadFocal(activeMap);
//         }});
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (DEBUG.PRESENT) VUE.Log.debug("AWT thread activeTool.handleFullScreen for " + activeTool);
            activeTool.handleFullScreen(true, goNative);
            NDC.pop();
        }});
        
    }

    
    
    private synchronized static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();
        
        VUE.Log.debug("Exiting full screen mode; inNative=" + wasNative);

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            VUE.Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        FullScreenWindow.setVisible(false);
        FullScreenViewer.loadFocal(null);
        fullScreenMode = false;
        fullScreenNative = false;

        if (nativeModeHidAllDockWindows) {
            nativeModeHidAllDockWindows = false;
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                DockWindow.ShowPreviouslyHiddenWindows();
            }});
        }
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            FullScreenLastActiveViewer.grabVueApplicationFocus("FullScreen.exit", null);
        }});
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            VueToolbarController.getActiveTool().handleFullScreen(false, wasNative);
            //VUE.Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
            //VUE.getActiveViewer().popToMapFocal(); // old active viewer shouldn't have changed...
            NDC.pop();
        }});

    }


    /*
    private static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();
        
        VUE.Log.debug("Exiting full screen mode, inNative=" + wasNative);

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            VUE.Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        fullScreenWindow.setVisible(false);
        fullScreenMode = false;
        fullScreenNative = false;
        if (fullScreenWindow != VUE.getMainWindow()) {
            VUE.Log.debug("re-attaching prior extracted viewer content " + fullScreenContent);
            fullScreenOldParent.add(fullScreenContent);
            //fullScreenOldParent.add(VUE.getActiveViewer());
        }
        if (VUE.getMainWindow() != null) {
//             if (fullScreenOldVUELocation != null)
//                 VUE.getMainWindow().setLocation(fullScreenOldVUELocation); // mac window manger not allowing
//             if (fullScreenOldVUESize != null)
//                 VUE.getMainWindow().setSize(fullScreenOldVUESize); // mac window manager won't go to 0
            //VUE.getMainWindow.setExtendedState(Frame.NORMAL); // iconifies but only until an Option-TAB switch-back
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                VUE.Log.debug("showing main window " + VUE.getMainWindow());
                VUE.getMainWindow().setVisible(true);
            }});
        }

        if (nativeModeHidAllDockWindows) {
            nativeModeHidAllDockWindows = false;
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                DockWindow.ShowPreviouslyHiddenWindows();
            }});
        }

        
        GUI.invokeAfterAWT(new Runnable() {
                public void run() {
                    //VUE.Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
                    VueToolbarController.getActiveTool().handleFullScreen(false, wasNative);
                    NDC.pop();
                }});

        //NDC.pop();
        
    }
    */      


    /*
    private static void enterFullScreenMode(boolean goNative)
    {
        NDC.push("[FS->]");

        //goNative = false; // TODO: TEMP DEBUG

        if (goNative) {
            // Can't use heavy weights, as they're windows that can't be seen,
            // and actually the screen goes blank on Mac OS X trying to handle this.
            javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
            // also: appear to need to do this before anything else
            // to have it property take effect?
        }

        
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final MapViewer viewer = VUE.getActiveViewer();
        final VueTool activeTool = VueToolbarController.getActiveTool();
        
        //out("Native full screen support available: " + device.isFullScreenSupported());
        //  setFullScreenWindow(SwingUtilities.getWindowAncestor(this));
        //VUE.frame.setVisible(false); // this also hids all children of the frame, including the new fs window.
        // mac bug: if don't create window every time, subsequent full-screen
        // modes won't extend to the the bottom of the screen (probably related to mac dock being there)
        // Very odd even if not running as a native window...  The window reports proper location, but it
        // show's up placed at a negative y.

        // todo: crap: if the screen resolution changes, we'll need to resize the full-screen window


        VUE.Log.debug("enterFullScreenMode: goingNative=" + goNative);
//         if (false&&goNative) {
//             if (VueUtil.isMacPlatform() || cachedFSWnative == null) {
//                 // have to create full screen native win on mac every time or it comes
//                 // back trying to avoid the dock??
//                 if (cachedFSWnative != null)
//                     cachedFSWnative.setTitle("_old-mac-full-native"); // '_' important for macosx hacks
//                 cachedFSWnative = GUI.createFrame("VUE-FULL-NATIVE");
//                 cachedFSWnative.setUndecorated(true);
//                 cachedFSWnative.setLocation(0,0);
//             }
//             fullScreenWindow = cachedFSWnative;
//         } else {
        if (cachedFSW == null) {
            cachedFSW = GUI.getFullScreenWindow();
            //cachedFSW = GUI.createFrame("VUE-FULL-WORKING");
            //cachedFSW.setUndecorated(true);
        }
        fullScreenWindow = cachedFSW;
            //}
            
//         if (false) {
//             fullScreenWindow = VUE.getMainWindow();
//         } else if (VueUtil.isMacPlatform() || fullScreenWindow == null) {
//             //} else if (fullScreenWindow == null) {
//             // Terribly wasteful to have to re-create this on the mac all the time..
//             if (false) {
//                 fullScreenWindow = VUE.createWindow(); // if VUE.frame is parent, it will stay on top of it
//             } else {
//                 if (fullScreenWindow != null)
//                     ((Frame)fullScreenWindow).setTitle("OLD-FULL-FRAME");
//                 fullScreenWindow = VUE.createFrame("VUE-FULL-FRAME");
//                 // but we need a Frame in order to have the menu-bar on the mac!
//                 ((Frame)fullScreenWindow).setUndecorated(true);
//             }
//             //fullScreenWindow.setName("VUE-FULL-SCREEN");
//             //fullScreenWindow.setBackground(Color.RED);
//         }

                
        if (fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
            //javax.swing.JComponent fullScreenContent = viewer;
            fullScreenContent = viewer;
            //fullScreenContent = new JLabel("TEST");
            fullScreenOldParent = viewer.getParent();

            VUE.Log.debug("adding content to FSW:"
                          + "\n\tCONTENT: "+ fullScreenContent
                          + "\n\t    FSW: "+ fullScreenWindow
                          );
            
            if (fullScreenWindow instanceof DockWindow) {
                ((DockWindow)fullScreenWindow).add(fullScreenContent);
            } else if (fullScreenWindow instanceof JFrame) {
                //((JFrame)fullScreenWindow).setContentPane(fullScreenContent);
                ((JFrame)fullScreenWindow).getContentPane().add(fullScreenContent);
            } else if (fullScreenWindow instanceof JWindow) {
                //((JWindow)fullScreenWindow).setContentPane(fullScreenContent);
                // adding to the contentPane instead of setting as allows
                // a JMenuBar to be added to the top and the viewer then
                // appears under it (instead of the menu overlapping it at the top)
                ((JWindow)fullScreenWindow).getContentPane().add(fullScreenContent);
            } else // is Window
                fullScreenWindow.add(fullScreenContent);

            fullScreenWindow.pack();

            //getMap().setFillColor(Color.BLACK);
            //fullScreenWindow.getContentPane().add(MapViewer.this.getParent().getParent()); // add with scroll bars
        }
                
        fullScreenMode = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;
        
//         if (VUE.getMainWindow() != null) {
//             //fullScreenOldVUELocation = VUE.getMainWindow().getLocation();
//             //fullScreenOldVUESize = VUE.getMainWindow().getSize();
//             //if (fullScreenWindow != VUE.getMainWindow())
//             //VUE.getMainWindow().setVisible(false);
//         }

        if (fullScreenWindow instanceof FSWindow) {
            ((FSWindow)fullScreenWindow).setMenuBarEnabled(!goNative);
        }

        if (goNative) {

            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            //tufts.macosx.Screen.goBlack();

            device.setFullScreenWindow(fullScreenWindow);
                
//             if (VueUtil.isMacPlatform()) {
//             try {
//             tufts.macosx.Screen.makeMainInvisible();
//             } catch (Exception e) {
//             System.err.println(e);
//             }
//             }

            //if (DEBUG.Enabled) out("fsw=" + fullScreenWindow.getPeer().getClass());
                    
            //fullScreenWindow.addKeyListener(inputHandler);
            //w.enableInputMethods(true);
            //enableInputMethods(true);
                            
            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.

            if (!DockWindow.AllWindowsHidden()) {
                nativeModeHidAllDockWindows = true;
                DockWindow.HideAllWindows();
            }
            
                            
        } else {
            GUI.setFullScreenVisible(fullScreenWindow);
        }
                
        activeTool.handleFullScreen(true, goNative);
                    
//         if (false && fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
//             VUE.getMainWindow().setVisible(false);

//             //VUE.getMainWindow().setSize(0,0);
//             //tufts.Util.setOffScreen(VUE.getMainWindow());
                
//             //VUE.getMainWindow().setLocation(0,0);
//             //VUE.getMainWindow().setLocation(3072,2048);
//             //VUE.getMainWindow().setExtendedState(Frame.ICONIFIED);
//         }

        NDC.pop();
    }
*/      

    private static void out(String s) {
        System.out.println("VUE FullScreen: " + s);
    }
    
}