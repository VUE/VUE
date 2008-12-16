/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
package tufts.vue.gui;

import tufts.Util;
import tufts.vue.Actions;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.DEBUG;
import tufts.vue.MapViewer;
import tufts.vue.VueTool;
import tufts.vue.VueToolbarController;
import tufts.vue.ZoomTool;

import tufts.macosx.MacOSX;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JWindow;
import org.apache.log4j.NDC;

/**
 * Code for providing, entering and exiting VUE full screen modes.
 *
 * @version $Revision: 1.39 $ / $Date: 2008-12-16 23:14:30 $ / $Author: sfraize $
 *
 */

// This code is pretty messy as it's full of old commented out experimental workarounds
// for java limitations relating to window parentage, visibility & z-order.  The current
// code seems to be the only thing that actually works, but lots of old code is left in
// all over the place in case we have problems with this again.  I've submitted an RFC
// for Java with Sun Micro on this, which was accepted, but the desired clean
// functionality won't be in there till Java 6 at the earliest, maybe not till Java 7.
// -- SMF 2007-05-22
// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6373178

public class FullScreen
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FullScreen.class);
    
    private static boolean fullScreenWorking = false;
    private static boolean fullScreenNative = false; // using native full-screen mode, that hides even mac menu bar?
    private static boolean nativeModeHidAllDockWindows;
    private static FSWindow FullScreenWindow;
    private static MapViewer FullScreenViewer;
    private static MapViewer FullScreenLastActiveViewer;

    private static final String FULLSCREEN_NAME = "*FULLSCREEN*";

    public static final String VIEWER_NAME = "FULL";

    //private static final boolean ExtraDockWindowHiding = !Util.isMacPlatform(); // must be done on WinXP

    // this "must be done" on XP -- need to recall why

    // It also is nice to be done on Mac OS X or the DockWindows appear over the temporary "working"
    // full screen window during a presentation (as would be expected), when we drop into that mode
    // to allow the display of an external app for content display (e.g., a web browser)

    // This may also end up being required for Leopard, tho it's anyones guess what that might
    // take at this point...
    private static final boolean ExtraDockWindowHiding = true;
    

    /**
     * The special full-screen window for VUE -- overrides setVisible for special handling
     */
    public static class FSWindow extends javax.swing.JWindow
    {
        private VueMenuBar mainMenuBar;
        private boolean isHidden = false;
        private boolean screenBlacked = false;
        
        FSWindow() {
            super(VUE.getApplicationFrame());
            if (GUI.isMacBrushedMetal())
                setName(GUI.OVERRIDE_REDIRECT); // so no background flashing of the texture
            else
                setName(FULLSCREEN_NAME);

            GUI.setRootPaneNames(this, FULLSCREEN_NAME);
            

            if (DEBUG.DOCK) {
                
                Util.printStackTrace("creating FSWindow");
                setOffScreen();

                if (Util.isMacPlatform() && DockWindow.useManagedWindowHacks()) {
                    setVisibleImpl(true, true);
                    setVisibleImpl(false, true);
                } else {
                    super.setVisible(true);
                    setFocusableWindowState(false);
                }
                
            } else {
                setOffScreen();
                setBackground(Color.black);
                
                if (Util.isMacPlatform() && DockWindow.useManagedWindowHacks()) {

                    //=============================================================================
                    
                    // On the Mac, this window *MUST* be shown before any DockWindows display, or they
                    // won't stay on top of their parents (On top of the main VUE window).  This is a
                    // mac bug; as of 2006 anyway -- SMF 2007-05-22.
                    
                    // SMF UPDATE 2008-04-21: Still needed on Mac OS X Tiger (10.4.x) for this to work, and
                    // we must call our setVisible impl, not just super.setVisible().  Unfortunately,
                    // this is NO LONGER WORKING to keep DockWindow's on top on Mac OSX Leopard
                    // (10.5+), however, there is SOME way to get it to happen (iconifying the whole
                    // app and de-iconifying seems to do it), but I don't know if we can find another
                    // workaround...
                    
                    //=============================================================================
                    
                    setVisibleImpl(true, true);
                    setVisibleImpl(false, true);
                    
                }
            }

            VUE.addActiveListener(tufts.vue.LWMap.class, FSWindow.this);
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
        public void setVisible(boolean show) {
            if (DEBUG.DOCK) Log.debug("FSW setVisible " + show);
            setVisibleImpl(show, false);
        }

        private void setOffScreen() {
            if (DEBUG.DOCK) {
                // instead of hiding it, keep it around so we can observe it and
                // it's layering relative to other VUE windows (it should always
                // be OVER the main window, and UNDER all DockWindow's)

                // we set the color so we notice this window in its debug state, but it
                // turns out it's also important on Leopard for this to work at all (see
                // comment below)
                setBackground(Color.red);
                
                setSize(GUI.getScreenWidth() / 10, GUI.getScreenHeight() / 10);
                
            } else {
                GUI.setOffScreen(this);
                
                if (Util.isMacLeopard()) {

                    //------------------------------------------------------------------
                    
                    // This odd workaround, which I was lucky to stumble upon, is to
                    // allow us to enter native full screen mode on Leopard more than
                    // once.  Forcing an actual color change through to the peer is
                    // changing some kind of state such that the bug, which normally
                    // leaves the full screen window entirely blank the second time it's
                    // set in place appears to go away.  Note: the resulting bad case
                    // behavior of this bug is similar to the other bug that happens if
                    // any other windows try to appear/get focus while in native full
                    // screen -- we get a blank screen, except at least in this case, it
                    // doesn't lock up the entire Mac operating system event loop.
                    //
                    // -- SMF 2008-04-21
                    
                    //------------------------------------------------------------------
                    
                    Color c = getBackground();
                    if (Color.black.equals(c))
                        setBackground(Color.darkGray);
                    else
                        setBackground(Color.black);
                }
            }
        }

        private void setVisibleImpl(boolean show, boolean init)
        {
            setFocusableWindowState(show);
            
            // if set we allow it to actually go invisible
            // all children will hide (hiding all the DockWindow's)
            
            if (show) {
                super.setVisible(true);
            } else {
                setOffScreen();
            }

// Let this be handled by our callers -- was important before ExtraDockWindowHiding, which is
// pretty much mandatory now.
//             if (!init && show && !inNativeFullScreen())  {
//                 // just in case
//                 //if (DEBUG.Enabled) Util.printStackTrace("FS DW RAISE ALL");
//                 DockWindow.raiseAll(); 
//             }
        }
        
        public void activeChanged(tufts.vue.ActiveEvent e, tufts.vue.LWMap map) {
            if (fullScreenWorking) {
                if (VUE.multipleMapsVisible()) {
                    
                    // TODO:
                    // bug when multiple maps visible: if right viewer ever becomes
                    // the active viewer, then it sets it's map to the active map,
                    // and then we thing the map has changed here, and we
                    // open up the full screen viewer with the wrong map!
                    
                    // This is a convenience feature anyway: the only
                    // time this can be used (in production) is if
                    // the recently opened files menu is used while in working
                    // full screen.

                    // Ideally, we would be best to completely ignore these events
                    // while we're entering/exiting full-screen mode.
                } else {
                    FullScreenViewer.loadFocal(map);
                }
            }
        }

        @Override
        public void paint(Graphics g) {

            boolean offscreen = getX() < 0 || getY() < 0;
            
            if (DEBUG.Enabled) Log.debug("paint " + this + ";  hidden=" + isHidden + "; offscreen=" + offscreen);

            // don't paint if we're offscreen.  This is important for the presentation tool,
            // as it actually configures some of it's special button locations when it
            // paints, so only one viewer at a time should be painting, otherwise the
            // buttons could get the wrong location for the visible viewer.
            if (offscreen)
                return;
            
            super.paint(g);

            if (isHidden) {
                // wait for paint to finish, then fade us up
                fadeUp();
            }

            if (screenBlacked) {
                fadeFromBlack();
                screenBlacked = false;
            }
        }

        void screenToBlack() {
            screenBlacked = true;
            goBlack();
        }


        private void fadeUp()
        {
            isHidden = false;
            if (Util.isMacPlatform()) {
                if (DEBUG.Enabled) Log.debug("requesting fadeup");
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    doFadeUp();
                }});
            }
        }
        
        private void doFadeUp()
        {
            if (DEBUG.Enabled) Log.debug("fadeup invoked");
            if (Util.isMacPlatform()) {
                try {
                    //if (MacOSX.isMainInvisible())
                    MacOSX.fadeUpMainWindow();
                } catch (Throwable t) {
                    Log.error(t);
                }
            }
            isHidden = false;
        }

        void makeInvisible() {
            if (!isHidden && Util.isMacPlatform() && !VUE.isApplet()) {
                isHidden = true;
                MacOSX.setWindowAlpha(this, 0);
            }
        }

//         void makeVisible() {
//             if (isHidden) {
//                 isHidden = false;
//                 //MacOSX.cycleAlpha(this, 0f, 1f);
//                 MacOSX.setAlpha(this, 1f);
//             }
//         }

//         void makeVisibleLater() {
//             if (isHidden) {
//                 GUI.invokeAfterAWT(new Runnable() { public void run() {
//                     makeVisible();
//                 }});
//             }
//         }
        
        

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
        
    }

    

    private static void goBlack() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.goBlack();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }

    private static void goClear() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.hideFSW();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }
    

    private static void fadeFromBlack() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.fadeFromBlack();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }
    
//     public static void fadeToBlack() {
//         // None of the MacOSX utils that use a separate NSWindow drawn
//         // black on top of everything else work in full-screen native
//         // mode -- the full screen window itself always stays on-top,
//         // even if we order the special NSWindow to the front.
        
// //         if (Util.isMacPlatform()) {
// //             try {
// //                 MacOSX.fadeToBlack();
// //             } catch (Error e) {
// //                 Log.error(e);
// //             }
// //         }
//     }

    

    
    
    public static boolean inFullScreen() {
        return fullScreenWorking || fullScreenNative;
    }

    public static boolean inWorkingFullScreen() {
        return fullScreenWorking;
    }
    public static boolean inNativeFullScreen() {
        return fullScreenNative;
        //return fullScreenNative || (DEBUG.PRESENT && inWorkingFullScreen());
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }

//     public static void setFullScreenWorkingMode() {
//         dropFromNativeToWorking();
//     }
    
    public static void dropFromNativeToWorking() {

        // TODO: merge this code with exitFullScreen

        FullScreenWindow.screenToBlack(); // will auto-fade-up next time it completes a paint

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
        }
        FullScreenWindow.setMenuBarEnabled(true);
        VUE.getActiveTool().handleFullScreen(true, false);
        GUI.setFullScreenVisible(FullScreenWindow);
        fullScreenWorking = true;
        fullScreenNative = false;

        //FullScreenViewer.grabVueApplicationFocus("FullScreen.nativeDrop", null);

//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             VUE.getActiveTool().handleFullScreen(true, false);
//         }});
        
        
    }
    
    public static void dropFromNativeToFrame() {
        Actions.ToggleFullScreen.act();
    }
    
    public static synchronized void toggleFullScreen(boolean goNative)
    {
        //agoNative=false;
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

//         final boolean doBlack = (goNative || inNativeFullScreen());
//         if (doBlack)
//             goBlack();

        if (fullScreenWorking) {
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

            // TODO: this is problematic: this will unexepectly switch the user to a
            // different tool when they enter full screen mode.  The floating tool panel is
            // not being synced with the main VUE tool panel (it may not be capable of this
            // -- it has it's own smaller sub-set of tools on it).
            
            tufts.vue.VueToolbarController.getController()
                .setSelectedTool(VUE.getFloatingZoomPanel().getSelectedTool());
            // Is causing event loop:
            //VUE.setActive(VueTool.class, FullScreen.class, VUE.getFloatingZoomPanel().getSelectedTool());
            
        	
        }
        
        //VUE.getActiveViewer().requestFocus();
        ////ToolWindow.adjustMacWindows();
        VueMenuBar.toggleFullScreenTools();

//         if (doBlack)
//             VUE.invokeAfterAWT(new Runnable() {
//                     public void run() { tufts.Util.screenFadeFromBlack();}
//                 });
    }

    private static LWMap activeMap = null;
    private static MapViewer activeViewer= null;
    
    public static LWMap getObscuredMap()
    {
    	return activeMap;
    }
    
    public static MapViewer getObscuredViewer()
    {
    	return activeViewer;
    }
    
    private synchronized static void enterFullScreenMode(final boolean goNative)
    {
        NDC.push("[FS->]");

        boolean wentBlack = false;
        if (goNative && inWorkingFullScreen()) {
            // as the working full-screen window is the same window as the native
            // full-screen window, we need to show the black window when transitioning
            // from working full-screen to native full-screen, otherwise we see the
            // working full-screen tear-down and the underlying VUE app/desktop before
            // seeing the black screen of the initial native window at 0 alpha.
            // Okay to leave this up while in native, as it'll be torn down automatically
            // on exit, tho it safer to tear it down just in case.
            goBlack();
            wentBlack = true;
        }

        activeMap = VUE.getActiveMap();
        activeViewer = VUE.getActiveViewer();
        tufts.vue.LWComponent activeFocal = null;

        if (activeViewer != null)
            activeFocal = activeViewer.getFocal();
        if (activeFocal == null && activeMap != null)
            activeFocal = activeMap;

        if (activeViewer != FullScreenViewer) {
            FullScreenLastActiveViewer = activeViewer;
            activeViewer.setFocusable(false);
        }
        
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


        Log.debug("Entering full screen mode; goNative=" + goNative);
        if (FullScreenWindow == null) {
            FullScreenWindow = (FSWindow) GUI.getFullScreenWindow();
            FullScreenWindow.getContentPane().add(FullScreenViewer = new MapViewer(null, VIEWER_NAME));
            //fullScreenWindow.pack();
        }
                
        FullScreenWindow.setMenuBarEnabled(!goNative);
        // FullScreenViewer.loadFocal(VUE.getActiveMap()); // can't do till we're sure it has a size!
        fullScreenWorking = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;

        if (!Util.isWindowsPlatform() && goNative) {

            // Try and prevent us from flashing a big white screen while we load.
            // Tho immediately loading the focal below will quickly override this...
            FullScreenWindow.setBackground(Color.black);
            FullScreenViewer.setBackground(Color.black);
            
            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            FullScreenWindow.makeInvisible();
            device.setFullScreenWindow(FullScreenWindow);

            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.

            if (ExtraDockWindowHiding && !DockWindow.AllWindowsHidden()) {
                nativeModeHidAllDockWindows = true;
                DockWindow.HideAllWindows();
            }

        } else {
            if (goNative) {
                if (ExtraDockWindowHiding && !DockWindow.AllWindowsHidden()) {
                    nativeModeHidAllDockWindows = true;
                    DockWindow.HideAllWindows();
                }
            }
            GUI.setFullScreenVisible(FullScreenWindow);
        }
                
        FullScreenViewer.loadFocal(activeFocal);
        
        if (!goNative && activeViewer != null && activeMap != null && !(VUE.getActiveViewer().getFocal() instanceof tufts.vue.LWSlide))        
        {
        	//if (activeViewer.getWidth() != activeViewer.getVisibleWidth() ||activeViewer.getHeight() != activeViewer.getVisibleHeight())
        		ZoomTool.setZoomFitRegion(FullScreenViewer,activeViewer.getVisibleMapBounds());
        //	else
        	//	ZoomTool.setZoomFitRegion(FullScreenViewer,activeViewer.getMap().getMapBounds(),activeViewer.getMap().getFocalMargin(),false);
        		
        }

        if (!goNative && (Util.isUnixPlatform() || (DockWindow.useManagedWindowHacks() && Util.isMacPlatform()))) {

            // need this on Leopard for sure, and seems also to help on Linux
            
            // Pretty sure this only needs to be done the first
            // time (yet another java / java mac impl bug), but
            // can't hurt to do it always.
            DockWindow.raiseAll();
        }
        
        FullScreenViewer.grabVueApplicationFocus("FullScreen.enter-1", null);
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (DEBUG.PRESENT) Log.debug("AWT thread activeTool.handleFullScreen for " + activeTool);
            activeTool.handleFullScreen(true, goNative);
        }});

        if (wentBlack) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // shouldn't ever be able to see this happen, as the native full-screen should be
                // on top of us, but just in case we make sure to clear it out behind us...
                goClear();
            }});
        }

        GUI.invokeAfterAWT(new Runnable() { public void run() {

            // The initial request for focus sometimes happens while the
            // FullScreenViewer already has focus, and immediately after that it
            // completely loses focus (to null), so we request again here to be
            // absolutely sure we have keyboard focus.

            FullScreenViewer.grabVueApplicationFocus("FullScreen.enter-2", null);
            NDC.pop();
        }});
        
    }

    
    
    private synchronized static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();
        final tufts.vue.LWComponent fullScreenFocal = FullScreenViewer.getFocal();
        final tufts.vue.LWMap fullScreenMap = FullScreenViewer.getMap();
        final Rectangle2D fullScreenVisibleMapBounds = FullScreenViewer.getVisibleMapBounds();
        //final Rectangle2D fullScreenMapBounds = fullScreenMap.getBounds();

        Log.debug("Exiting full screen mode; inNative=" + wasNative);

        final boolean fadeBack = wasNative && Util.isMacPlatform();

        if (fadeBack) {
            // For Mac: this won't be visible till full-screen is torn down (nothing
            // can go over the FSW), but it will be there all full and black once
            // the java FSW *is* torn down, then we can fade that out to reveal
            // the desktop.
            goBlack();
        }

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        
        fullScreenWorking = false;
        fullScreenNative = false;
        FullScreenWindow.setVisible(false);
        FullScreenViewer.loadFocal(null);

        // Note: several of these invokeAfterAWT calls are probably overkill,
        // but the focus issues we're dealing with here can be very delicate,
        // so be careful in here...

        if (ExtraDockWindowHiding && nativeModeHidAllDockWindows) {
            nativeModeHidAllDockWindows = false;
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                DockWindow.ShowPreviouslyHiddenWindows();
                if (Util.isMacPlatform()) {

                    // This will ensure the windows are fully painted when we return
                    // from fade-back.
                    
                    // Only need to do this on mac, as we can't hide what's happening on
                    // WinXP by fading to/from black anyway.
                    
                    DockWindow.ImmediatelyRepaintAllWindows();
                }
            }});
        }

        if (FullScreenLastActiveViewer != FullScreenViewer) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // todo: we do NOT want to do this if the active map has changed while in
                // full-screen mode: we want to activate the viewer for the map currently
                // displayed
                FullScreenLastActiveViewer.grabVueApplicationFocus("FullScreen.exit", null);
            }});
        }
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            VueToolbarController.getActiveTool().handleFullScreen(false, wasNative);
            //Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
            //VUE.getActiveViewer().popToMapFocal(); // old active viewer shouldn't have changed...
        }});


        GUI.invokeAfterAWT(new Runnable() { public void run() {

            // Do not now ask for the currently active viewer: use the reference to the one that
            // was active when we entered full-screen mode: the java focus system can be very
            // unpredicable -- we can't be sure who is getting the focus back.  E.g., the active
            // viewer at this point may even still be the full-screen viewer, with a null
            // map, as it's been unloaded at this point.

            // Todo: if the active map has changed while in full screen mode, we should actually
            // return to the open viewer that has the same map.  Checking the ActiveViewer almost
            // works for this, but the corner cases need handling.  Easiest approach may be to have
            // FullScreenLastActiveViewer actually repoint to the standard viewer for the active
            // map if the active map changes.
            
            final MapViewer returnViewer = FullScreenLastActiveViewer;
            //final MapViewer returnViewer = VUE.getActiveViewer();

            if (DEBUG.Enabled) Log.debug("exit FS cleanup;"
                                         + "\n\tlastActiveViewer: " + FullScreenLastActiveViewer
                                         + "\n\t nowActiveViewer: " + VUE.getActiveViewer()
                                         + "\n\t    returnViewer: " + returnViewer
                                         + "\n\t fullScreenFocal: " + fullScreenFocal
                                         );

            try {
                if (!wasNative && returnViewer != null) {
                    
                    // If the focal has changed while in full screen mode, and it's still a
                    // focal from the same map that's in the viewer we're returning back to,
                    // swap load the same focal into the returning viewer.  (e.g., a slide
                    // was being edited in full screen mode: make the same slide visible in
                    // the regular viewer when we return to it).

                    if (fullScreenMap == returnViewer.getMap()) {
                        if (returnViewer.getFocal() != fullScreenFocal)
                            returnViewer.loadFocal(fullScreenFocal);
                    }
            		
                    // have the returning viewer zoom to the same region
                    // that is visible in the full-screen viewer
                    // problem with this method: the region keeps shrinking on repeated in/out of working FS mode
                    // ZoomTool.setZoomFitRegion(returnViewer, fullScreenVisibleMapBounds, 0, false, false);

                    // Current logic: this seems to keep the same region visible if the region is
                    // "zoomed in" -- there are parts of the map visible off screen, or just force
                    // a zoom-fit otherwise.

                    if (returnViewer.getWidth() != returnViewer.getVisibleWidth() ||
                        returnViewer.getHeight() != returnViewer.getVisibleHeight()) {
                        if (DEBUG.Enabled) Log.debug("size mismatch (viewer probably scroll-enabled) re-zoom");
                        ZoomTool.setZoomFitRegion(returnViewer,
                                                  returnViewer.getVisibleMapBounds());
                    } else {
                        if (DEBUG.Enabled) Log.debug("default re-zoom");
                        ZoomTool.setZoomFitRegion(returnViewer,
                                                  returnViewer.getMap().getMapBounds(),
                                                  returnViewer.getMap().getFocalMargin(),
                                                  false);
                    }

                }
            } finally {
                if (!fadeBack) NDC.pop();
            }
            
        }});


// This works to have the toolbar painted, vut the tabbed pane and viewer apparently aren't ready yet (just shows dark gray)
//         if (Util.isMacPlatform())
//             ((javax.swing.JComponent)VUE.getApplicationFrame().getContentPane()).paintImmediately(new Rectangle(-256,-256,4096,4096));
        
        
        if (fadeBack) {
            if (DEBUG.Enabled) Log.debug("requesting fadeFromBlack");
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                fadeFromBlack();
                NDC.pop();
            }});
        }

        

    
    }





    /*
    private static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();
        
        Log.debug("Exiting full screen mode, inNative=" + wasNative);

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        fullScreenWindow.setVisible(false);
        fullScreenWorking = false;
        fullScreenNative = false;
        if (fullScreenWindow != VUE.getMainWindow()) {
            Log.debug("re-attaching prior extracted viewer content " + fullScreenContent);
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
                Log.debug("showing main window " + VUE.getMainWindow());
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
                    //Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
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


        Log.debug("enterFullScreenMode: goingNative=" + goNative);
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

            Log.debug("adding content to FSW:"
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
                
        fullScreenWorking = true; // we're in the mode as soon as the add completes (no going back then)
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