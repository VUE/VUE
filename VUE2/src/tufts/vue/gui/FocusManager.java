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
import static tufts.Util.*;
import tufts.macosx.MacOSX;

import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.VueUtil;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.AWTEvent;
import java.awt.KeyboardFocusManager;
import java.awt.FocusTraversalPolicy;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.PaintEvent;
import java.awt.event.HierarchyEvent;

import javax.swing.SwingUtilities;

import java.util.logging.*;

import javax.swing.JPanel;      // for test harness
import javax.swing.JLabel;      // for test harness
import javax.swing.JButton;     // for test harness
import javax.swing.JTextField;  // for test harness

// Note: issues with FOCUS: If a Window has setFocusableState(false), it won't take
// focus, thus leaving main application Frame's ACTIVATED, and you can click on buttons
// and such, in the Window, but you CANNOT get focus in a text field.  Maybe can hack
// fix that with our own FocusManager.  ALSO: floating Windows with no PARENT can NEVER
// take focus anyway, so we're going to have to hack that if we want an MDI
// (multi-document interface), unless we have some kind of always visible Frame opened
// up -- the parent has to be visible, which is why w/out parent they can never take
// input, unless our hacking a FocusManager also gets around that.

// god knows what this will do if running in an applet context...
        
//-------------------------------------------------------
//-------------------------------------------------------
        
// The problem is this: we want a text field to get focus in a floating window WIHTOUT
// having the currently activated Frame go inactive.  The Frame is stopped from going
// inactive if the Window containg the text field is marked as
// setFocusableWindowState(false), but then of course the component won't get focus.

// So, is it possible to have it both ways?  While this is tolerable in VUE's current
// SDI, in an MDI you really don't want the Frame show itself inactive -- how you going
// to know which one your floating tool is effecting?  It would be gross and inadquate
// to add an indicator, and totally gross to manage all the damn decorations ourselves
// for all the platforms.

// As "The active Window is always either the focused Window, or the first Frame or
// Dialog that is an owner of the focused Window" (according to KeyboardFocusManager),
// maybe we could interrupt the code where it determines that.  Oh, forget it, on mac,
// even when this is the case in current VUE -- the ToolWindows are owned by VUE.frame,
// it still freakin goes deactivated.

//-------------------------------------------------------
//-------------------------------------------------------

/*========================================================================================

  Insurmountable problem with setAlwaysOnTop? (sigh)

  setAlwaysOnTop keeps DockWindow's on top of everything, including pop-up's
  (menus, rollovers, etc).  We could hack around and force such things on top,
  which mostly works, except on mac where it's going behind anyway as you
  start to mouse around on the menu, so we'd literally have to hide any
  overlapping DockWindow's when menus or even freakin rollovers pop up.

  But that's just the start: if the rollover or pop-up menu is LIGHTWEIGHT,
  there's no heavy weight window to set always on top, in which case our
  only recourse in the case would be to detect overlapping DockWindows
  and hide them completely -- which I suspect would be disgusting.

  As it is, Mac seems only to use heavy weight menu's, but PC is happy to
  use lightweight ones.

  Now the problem of course is if we do NOT use alwaysOnTop, then having an MDI, or
  perhaps more importantly, FULL-SCREEN, is either impossible or another set of
  very nasty and barely tolerable hacks.  The problem is the DockWindow's can
  only stay on top of their original parent -- so a second document window
  would have all the DockWindow's going behind it.

  Now, on the Mac, we could hack this like before by re-attaching the window's
  as focus changes -- messy but probably tolerable, assuming we can fix the
  full-screen bug there we never did.

  On the PC, either we're completely freakin out of luck, unless grand parentage works
  on the PC (windows staying on top of not just parents, but grandparents, which would
  allow us a whole family of windows and make everything nice).

  So at the moment it's looking like for full-screen, we'll need our mac hacks
  back, and either grand-parentage will save us on PC, or, GOD FORBID:

  We could have MULTIPLE COPIES of all the damn DockWindow's, a set of children
  for each document window.  This really only helps with full screen tho --
  i'd think the flashing for MDI would be intolerable.

  OH, well, there's always putting up with flashing; constantly toFronting the
  windows...

  Oh, and of course, we lose our forced focus advantage of not letting the
  main application go inactive on the mac, but that seem's hardly like
  a serious concern at this point...


  ***OR*** -- I suppose we could LIVE with Menu's going under DockWindows...


  Okay: even enforced toFronting isn't going to work: we appear to be
  DOA on the PC in this regard.

  UPDATE: Okay, we can totally deal with this on the mac by raising
  the NSWindow level, but we're stuck on the PC.


  ========================================================================================*/
  


/**
 * A focus manager that allows the focus to be "forced" to components
 * within a Window who's focusable state is actually false (getFocusableState() == false)
 * This is useful because keeping floating tool palette's / toolbars in Window's 
 * who's state is NOT focusable allows a main application frame to KEEP the
 * focus (and it's title bar stay active) even while editing a field in another
 * Window.
 *
 * Currently this allows forced focus only to Swing JTextComponent's and JComboBox's.
 *
 * Note: the implementation me be non-deterministic in future java releases
 * as we essentially "lie" about who has the active focus, by overriding
 * superclass getters and returning our own forced focusOwner and
 * focusCycleRoot when the KeyboardFocusManager is asked. We also
 * redispatch our own FocusEvents for transferring focus, which is the second
 * part of the magic that makes this work.
 *
 * @version $Revision: 1.27 $ / $Date: 2009-05-13 16:59:58 $ / $Author: sfraize $ 
 */

// todo: can also try calling the focus owner setters instead of lying -- that might work

// TODO: need to handle single-click in a JTable to activate edit (why doesn't
// it show cursor for single click in vanilla mode tho?)
// Also, e.g., outline view can take focus and you can use all kinds of
// focus traversal keys, and it tweaks selection as you do this!

// speaking of which, if we have MDI, a checkbox on the windo should be tracks
// selection, and as long as we're going to have a toolbar, might as well put
// forward/back in there, and then maybe zoom controls?

// If we do MDI, multiple views of the same map will need to be labled as "View 2", etc.

// For understanding entire AWT event chain start at:
// java.awt.EventQueue,dispatchEvent() and java.awt.Component.dispatchEvent()


public class FocusManager extends java.awt.DefaultKeyboardFocusManager
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FocusManager.class);
    
    private static final Object FOCUS_FORWARD = "FORWARD";
    private static final Object FOCUS_BACKWARD = "BACKWARD";

    private static FocusManager Singleton;
    

    /** forced focus component */
    protected Component mForcedFocus = null;
    
    /** the copmonent that had focus before we entered forced-focus */
    protected Component mFocusOwnerBeforeForced = null;
    
    /** forced focus child -- the underlying component we may have clicked on */
    protected Component mForcedFocusClickedChild = null;
    
    /** focus cycle root of the forced focus component */
    protected Container mForcedFocusCycleRoot = null;
    
    /** Window that owns mForcedFocusCycleRoot -- may in fact be the same object */
    protected Window mForcedFocusWindow = null;

    /** true if we're currently forcing the focus */
    protected static boolean mForcingFocus = false;
    
    /** The last target of a MOUSE_PRESSED event  */
    protected Object mMousePressedTarget;
        
    private static final boolean UseForcedFocus = false;

    private final static boolean LieImpl = true; // impl style: lying v.s. setting (setting probably not possible)


    /**
     * Make this FocusManager active for all of AWT.
     * The first call create's and install a new KeyboardFocusManager.
     */
    public static synchronized void install() {
        if (Singleton == null) {
            Singleton = new FocusManager();
            KeyboardFocusManager.setCurrentKeyboardFocusManager(Singleton);
            if (DEBUG.INIT) System.out.println(Singleton + ": installed.");
            if (DEBUG.FOCUS && DEBUG.META) enableAWTFocusLogging();
            //UseForcedFocus = GUI.UseAlwaysOnTop
            //    || true; // Turn on for now so toolbars can have false focusableWindowState
            
        } else
            if (DEBUG.INIT) System.out.println(Singleton + ": already installed.");
    }

    public static Component getFocused() {
        return Singleton.getFocusOwner();
    }

    protected FocusManager() {
        //System.out.println("FocusManager constructed.");
    }

//     public interface MouseInterceptor {
//         /** return true if the event should be consumed and not passed on to children */
//         public boolean interceptMousePress(java.awt.event.MouseEvent e);
//     }

    /**
     * Here we see all the KeyEvents and MouseEvents, within the entire VM context,
     * and Window/Component events related to focus.
     * 
     * @return <code>true</code> if this method dispatched the event;
     *         <code>false</code> otherwise
     */
    public boolean dispatchEvent(AWTEvent e) {
        final int id = e.getID();
        
        if (!DEBUG.META && id >= HierarchyEvent.HIERARCHY_FIRST && id <= HierarchyEvent.HIERARCHY_LAST)
            return super.dispatchEvent(e);

        // If this is a resize event, and the window is animating resize,
        // we do NOT want to dispatch any COMPONENT_RESIZED events: just
        // return true, claiming it's been handled.  Ah: but there's
        // no way to know it's animating: we get these events after
        // -- maybe we can stick something in the queue to let us know
        // when AWT is done?
        // See Component.dispatchEventImpl(AWTEvent e)

        switch (id) {
        case MouseEvent.MOUSE_MOVED:
        case MouseEvent.MOUSE_ENTERED:
        case MouseEvent.MOUSE_EXITED:        
        case MouseEvent.MOUSE_DRAGGED:
            //case ComponentEvent.COMPONENT_MOVED:
            //case ComponentEvent.COMPONENT_RESIZED:
            //case PaintEvent.PAINT:
            //case PaintEvent.UPDATE:
            // do don't report these.
        	
            if (DEBUG.META == false)
                break;
            
        default:
            //if (e instanceof WindowEvent || e instanceof FocusEvent)
            if (DEBUG.FOCUS 
                || (DEBUG.MOUSE && e instanceof MouseEvent)
                || (DEBUG.PAINT && e instanceof PaintEvent)
                || (DEBUG.KEYS && e instanceof KeyEvent)
                ) {
                if (!DEBUG.META && e instanceof MouseEvent && ((MouseEvent)e).getComponent().isShowing() == false) {
                    // don't report mouse-overs of invisible windows that may be scattered about
                    break;
                    //return true;
                }
                // especially highlight the start of input chains
                if (id == MouseEvent.MOUSE_PRESSED ||
                    id == KeyEvent.KEY_PRESSED ||
                    id == ComponentEvent.COMPONENT_SHOWN )
                    out(TERM_YELLOW + eventName(e) + TERM_CLEAR);
                else
                    out(TERM_RED + eventName(e) + TERM_CLEAR);
            }
        }

        Component c = (Component) e.getSource();
            
        switch (id) {
        case MouseEvent.MOUSE_PRESSED:

            MouseEvent me = (MouseEvent) e;
            
            // In some cases, we get two mouse presses & releases, such as clicking on a
            // combo-box in a JTable, where we get a press for the JTable, a press for
            // the combo-box, then a release for the JTable, then a release for the
            // combo-box.  We only want to take action on the last pressed object.  (Or
            // the combo pops up and then immediately goes away) Unforunately, this
            // means we can't shift the focus on MOUSE_PRESSED like regular focus
            // changes, unless we can figure another workaround for this, or specialize
            // for just the JTable case, or make a VueTable that provides us an
            // indicator for this situation.

            if (DEBUG.FOCUS && !UseForcedFocus)
                getFocusCycleRoot(c); // for diagnostic output

            mMousePressedTarget = c;
            
            Window parentWindow = getFocusCycleWindow(c);
            //if (parentWindow instanceof MouseInterceptor) {
            if (parentWindow instanceof DockWindow.Peer) {
                //if (DEBUG.FOCUS) out("AWTEvent intercepted by MouseInterceptor " + Util.tags(parentWindow));
                if (DEBUG.FOCUS) out("AWTEvent intercepted by: " + parentWindow);
                try {
                    //if (((MouseInterceptor)parentWindow).interceptMousePress(me))
                    if (((DockWindow.Peer)parentWindow).getDock().interceptMousePress(me))
                        return true;
                } catch (Throwable t) {
                    Log.error("MouseInterceptor failure: " + Util.tags(parentWindow) + "; " + me, t);
                }
            }
            
            break;

        case MouseEvent.MOUSE_RELEASED:
            if (UseForcedFocus && c == mMousePressedTarget) {
                inspectMouseEventForForcableFocusChange(e);
                mMousePressedTarget = null;
            }
            break;

            /*            
        case WindowEvent.WINDOW_ACTIVATED:
            
            // Window's java 1.5 doesn't give is COMPONENT_SHOWN
            // for dialogs -- we need to trap this here
            if (!GUI.UseAlwaysOnTop && Util.isWindowsPlatform()) {
                if (c instanceof Dialog)
                    ensureDialogStaysOnTop((Dialog)c);
            }
            break;
            */
            

// Skip dialog on-top code for now and see what breaks SMF 2006-06-03 15:02.36
/*
        case ComponentEvent.COMPONENT_SHOWN:
            //case PaintEvent.PAINT:


            // even if we're not using always on top in general,
            // we now always use it for 1.5 dialog's with no parent,
            // otherwise they go behind other windows!

            if (GUI.UseAlwaysOnTop == false) {
                if (c instanceof Dialog)
                    ensureDialogStaysOnTop((Dialog)c);
                // don't go any further if not UseAlwaysOnTop                
                break;
            }

            
            boolean forceOnTop = false;
            if (c instanceof java.awt.Dialog) {
                forceOnTop = true;
            } else {
                forceOnTop = c instanceof Window && isPopup(c);
            }

            if (forceOnTop) {
                final Window window = (Window) e.getSource();
                if (false //&& w.isAlwaysOnTop()
            ) {
                    out(name(window) + " toFront");
                    window.toFront();
                } else {
                    if (Util.isMacPlatform()) {
                        MacOSX.raiseToMenuLevel(window);
                    } else {
                        // This is experimental overkill for the PC to
                        // see what's possible worst case.  Apparently: not much.
                        GUI.setAlwaysOnTop(window, true);
                        window.toFront();
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            out(name(window) + " setAlwaysOnTop RUN");
                            GUI.setAlwaysOnTop(window, true);
                            window.toFront();
                        }});
                    }
                }
            }
            
            break;
*/
            
        case ComponentEvent.COMPONENT_HIDDEN:
            if (UseForcedFocus && c == mForcedFocusWindow) {
                // Be sure to clear forced focus if the Window that contains it hides
                clearForcedFocus(true);
            }
            break;
        }

        boolean dispatched = super.dispatchEvent(e);


        // We could process this in dispatchKeyEvent, but it is not
        // guaranteed to always be used, so we must catch this here.
        if (id == KeyEvent.KEY_PRESSED && !((KeyEvent)e).isConsumed()) {
            if (!dispatched) Util.printStackTrace("FAILED TO DISPATCH " + e);

            final KeyEvent ke = (KeyEvent) e;

            if (isLikelyRelayableActionKey(ke))
                relayUnconsumedKeyPress(ke);

            if (DEBUG.FOCUS) out("UNCONSUMED: " + e);
        }

        return dispatched;
    }

    private static final int MENU_BAR_MAYBE =
        InputEvent.CTRL_MASK |
        InputEvent.META_MASK |
        InputEvent.ALT_MASK |
        InputEvent.ALT_GRAPH_MASK;


    private static boolean isLikelyRelayableActionKey(KeyEvent ke) {

        //if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) Log.debug("SEEING BACK SPACE");
        
        return (ke.getModifiers() & MENU_BAR_MAYBE) != 0
            || ke.isActionKey()
            || ke.getKeyCode() == KeyEvent.VK_BACK_SPACE
            || ke.getKeyCode() == KeyEvent.VK_DELETE
            ;
        
    }

    private void ensureDialogStaysOnTop(Dialog dialog) {
        if (DEBUG.FOCUS) out("DIALOG PARENT: " + dialog.getParent());
        if (dialog.getParent() == GUI.HiddenDialogParent) {
            GUI.setAlwaysOnTop(dialog, true);
        }
    }
    
    /**
     * Hand off to the VueMenuBar any unconsumed key events so it
     * may process the KeyStroke for invoking a menu Action
     */
    private void relayUnconsumedKeyPress(KeyEvent e) {
        //if (e.getSource() == null) Util.printStackTrace("null source: " + e);
        //e = new VUEKeyEvent(e);
        if (DEBUG.FOCUS || DEBUG.KEYS) out(name(e) + " RELAYING");
        try {
            //VUE.getJMenuBar().doProcessKeyPressEventToBinding(e);
            VUE.getJMenuBar().doProcessKeyEvent(e);
        } catch (NullPointerException ex) {
            if (DEBUG.Enabled) out("relayUnconsumedKeyPress: no menu bar");
        }
    }

    public boolean dispatchKeyEvent(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE &&
            e.isControlDown() &&
            e.isAltDown() &&
            e.isShiftDown())
        {
            Log.info("Keyboard Abort Sequence: (Ctrl-Alt-Shift-ESCAPE); " + e);
            System.err.println("VUE: key sequence abort " + e);
            System.exit(0);
        } // debug abort

        // Note that KeyEvents typed while a forced focus is in place
        // will come in with a source of the currently active Frame,
        // not the force component.  We patch that up here.  If we
        // don't, the key events are sometimes ignored by the target.
        
        if (mForcingFocus) {
            /*
            if (mForcedFocus instanceof VueTextPane && e.getKeyCode() == KeyEvent.VK_TAB) {
                // This would be better handled by having the VueTextPane
                // install it's own additional focus traversal keys.
                if (e.getID() == KeyEvent.KEY_RELEASED) {
                    if (e.isShiftDown())
                        focusPreviousComponent();
                    else
                        focusNextComponent();
                }
                e.consume();
                return true;
            }
            */
            e.setSource(mForcedFocus);
        }

        if (DEBUG.FOCUS||DEBUG.KEYS) out(TERM_GREEN + eventName(e) + TERM_CLEAR);

        //boolean handled = super.dispatchKeyEvent(e);

        boolean handled = false;
        
        // A complete and total hack making use of a global TAB press:
        
        if (e.getKeyCode() == KeyEvent.VK_TAB
            && (e.getID() == KeyEvent.KEY_PRESSED || e.getID() == KeyEvent.KEY_RELEASED))
        {
            if (DEBUG.FOCUS || DEBUG.KEYS) out("inspecting TAB from " + e.getSource() + "; " + e.paramString());
            if (FullScreen.inNativeFullScreen()) {

                // never want to manually mess with the DockWindows in full screen mode anyway

                final tufts.vue.VueTool tool = tufts.vue.VueToolbarController.getActiveTool();

                if (tool instanceof tufts.vue.PresentationTool) {
                    if (DEBUG.FOCUS || DEBUG.KEYS) out("relaying TAB to " + tool);
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        tool.handleKeyPressed(e);
                    else //if (e.getID() == KeyEvent.KEY_RELEASED)
                        tool.handleKeyReleased(e);
                    handled = true;
                }
                
            } else if (e.getID() == KeyEvent.KEY_PRESSED) {
                //out("TAB: consumed=" + e.isConsumed() + " handled=" + handled);
                
                final Object src = e.getSource();
                
                if (src instanceof tufts.vue.MapViewer ||
                    src instanceof tufts.vue.gui.DockWindow.Peer ||
                    src instanceof tufts.vue.PathwayTable)
                {
                    if (DEBUG.FOCUS || DEBUG.KEYS) out("relaying TAB to " + DockWindow.class);
                    DockWindow.ToggleAllVisible();
                    handled = true;
                }
                
            }
        }

        if (handled)
            return true;
        else
            return super.dispatchKeyEvent(e);
    }

    public boolean XpostProcessKeyEvent(KeyEvent e) {
        super.postProcessKeyEvent(e);
        //if (DEBUG.FOCUS)
            out(name(e) + " postProcessKeyEvent");
        return true;
    }

    public void XprocessKeyEvent(Component focused, KeyEvent e) {

        if (DEBUG.FOCUS && DEBUG.META) out(name(e) + " processKeyEvent: focused=" + name(focused));
            
        //tufts.Util.printStackTrace("processKeyEvent " + focused + " " + e);
        super.processKeyEvent(focused, e);
    }

    protected void enqueueKeyEvents(long after, Component untilFocused) {

        if (!UseForcedFocus) {
            super.enqueueKeyEvents(after, untilFocused);
            return;
        }
        
        boolean special = mForcedFocus == untilFocused; // TODO clean later: hoping to see this case...
        if (DEBUG.FOCUS || special) {
            out("enqeueKeyEvents, after=" + after
                + "\n\tuntilFocused: " + name(untilFocused)
                + "\n\t  focusOwner: " + name(getFocusOwner())
                + "\n\t forcedFocus: " + name(mForcedFocus)
                );
            if (DEBUG.META||special)
                Util.printStackTrace();
        }

        //-----------------------------------------------------------------------------
        // THE PROBLEM:

        // What happens is after a forced-focus, the force focus is properly cleared
        // when the MapViewer grabs focus after it gets MOUSE_ENTERED, and KeyEvents are
        // dispatched to it in the regular ware through it's parent, the VueFrame, just
        // fine, until the MapViewer gets a MOUSE_PRESSED, at which point the native
        // MacOSX AWT impl is deciding to queue all KeyEvents until it thinks the
        // MapViewer is ready to get them, and only the VueFrame (it's parent) ends up
        // seeing them.  In this scenario, the next child of VueFrame to get the focus
        // (that isn't the MapViewer, e.g., a text field in the toolbar) gets all these
        // queued key events at once.  I don't know why the native impl doesn't think
        // the MapViewer is ready for KeyEvents, but I'm assuming it has to do with the
        // hacks we've employed with this FocusManager.

        // To get around this, we skip this call (which establishes the queue
        // for future KeyEvents) if we just clicked on what is already the focus
        // owner -- this should be pretty safe.  Just in case, we may want to discard
        // all pending key events for this component, although skipping the enqueue
        // call should prevent any queue from being established in the first place.

        // This appears only to be a issue in Java 1.5 on MacOSX, not Java 1.4.2.
        // In 1.4.2, we see a call to dequeueKeyEvents happen after the MapViewer
        // calls requestFocus after getting MOUSE_PRESSED, as opposed to the
        // problematic enqueueKeyEvents.

        // If you want to dive into this more, start in Sun's Java 1.5.0 source,
        // java.awt.KeyboardFocusManager line 2327, which is where enqueueKeyEvents
        // is called for this case.

        // Addendum: This can ALSO sometimes happen when the MapVewier gets
        // MOUSE_ENTERED: it requests focus, and we get a call here to queue key events
        // for the current forced focus before it's been cleared -- we ignore in that
        // case also.  I haven't been able to reproduce this at will, but I've seen it.

        // Addendum: can also happen when simply mousing out of the map over a
        // DockWindow then back again!  When MapViewer get's return MOUSE_ENTERED, it's
        // requesting focus, and again we're getting the enqueue -- so we need to skip
        // anytime untilFocused is the focus owner -- not just if mouse was pressed on
        // it.  This is reproduceable by clicking into a forced focus, mousing into the
        // MapViewer and it gets cleared, mousing out of the MapViewer and then back in.

        // SMF 2006-01-02
        //-----------------------------------------------------------------------------


        //boolean isPopup = untilFocused.getName() == GUI.POPUP_NAME ||
        //untilFocused instanceof tufts.vue.MapViewer; // TODO
        final boolean isPopup = false;

        // so we can either try stopping the dispatch of the FOCUS_GAINED + FOCUS_LOST,
        // and or turn off focusability of the right damn component and/or peer.

        if (untilFocused == mForcedFocus || untilFocused == getFocusOwner() || isPopup) {
            if (DEBUG.FOCUS||special) out("IGNORING enqueueKeyEvents for " + name(untilFocused));
        } else {
            if (DEBUG.FOCUS||special) out("allowing enqueueKeyEvents for " + name(untilFocused));
            super.enqueueKeyEvents(after, untilFocused);
        }
    }
    
    protected void dequeueKeyEvents(long after, Component untilFocused) {
        if (DEBUG.FOCUS && UseForcedFocus) {
            out("dequeueKeyEvents, after=" + after + " untilFocused=" + name(untilFocused));
            if (DEBUG.META)
                Util.printStackTrace();
        }
        super.dequeueKeyEvents(after, untilFocused);
    }

    protected void discardKeyEvents(Component c) {
        if (DEBUG.FOCUS && UseForcedFocus) {
            out("discardKeyEvents: " + name(c));
            //if (DEBUG.META) Util.printStackTrace();
        }
        super.discardKeyEvents(c);
    }

    private boolean isPopup(Component c) {

        return c.getClass().getDeclaringClass() == javax.swing.Popup.class;
        
        /*
        boolean pop = c.getClass().getDeclaringClass() == javax.swing.Popup.class;
        final String name = c.getName();
        boolean pop =
            name == GUI.POPUP_NAME ||
            (name != null
             && GUI.OVERRIDE_REDIRECT != name         // it's not one of OUR special overrides,
             && GUI.OVERRIDE_REDIRECT.equals(name));  // but it's an override,

        // out("ISPOPUP " + pop + " " + name(c));
        return pop;
        */
    }

    private void inspectMouseEventForForcableFocusChange(AWTEvent e) {

        final Component clicked = (Component) e.getSource();
        
        if (DEBUG.FOCUS) out(" inspectMouseEvent: " + name(clicked) + " " + e.paramString());
        
        if (clicked == mForcedFocus || clicked == mForcedFocusClickedChild) {
            // leave us force-focused as is        	
            return;
        }

        if (clicked == null) {
            Util.printStackTrace("null source in " + e);
            return;
        }
        
        //----------------------------------------------------------------------------- 
        // We've clicked on something that is anything but the currently
        // forced focus Component, if there is one.  Usually this means
        // we'll be canceling the existing forced focus, if if the new
        // component is force focusable, transfering focus to that.
        // But not if we're clicking in a pop-up window and/or one without
        // a Window at it's root (some menu pop-ups do this).
        //----------------------------------------------------------------------------- 
            
        final Container focusCycleRoot = getFocusCycleRoot(clicked);

        if (focusCycleRoot == null || isPopup(focusCycleRoot)) {
                
            // this happens in the case of pop-ups, in which case we don't need to do
            // anything to the currently forced focus.  In the case of a Menu, it'll run
            // and we can keep focus.  In the case of, say, the pop-up under a JComboBox
            // that IS the forced-focus component, it may contain all sorts of stuff,
            // like a scroll bar, and anything down in there we click on we want to
            // allow w/out moving the focus away from the JComboBox, or the whole pop-up
            // will dissapear;
                
            if (DEBUG.FOCUS) out("focusCycleRoot is " + name(focusCycleRoot) + ", no focus change for popups");
            return;
        }
            
        final boolean wasForcingFocus = mForcingFocus;

        // We've clicked on *something*, and it's not the currently
        // forced focus.  So if we're currently forcing focus, 
        // it needs to be cleared.
        
        final Component forceable;
        final Window focusCycleWindow = getFocusCycleWindow(focusCycleRoot);

        // The whole purpose of this FocusManager is to allow focus to Components
        // that are in Window's that are NOT focusable: so only activate
        // the forced focus if the root window is not focusable.
        // Note that if the window contains no focusable elements, even
        // if it's focusable window state is true, it will not be considered
        // a focusable window.
        if (focusCycleWindow.isFocusableWindow())
            forceable = null;
        else
            forceable = getMouseClickForceFocusableComponent(clicked);

        if (forceable == null) {

            // cases mentioned in comments below now handled in clearForcedFocus
            if (mForcingFocus)
                clearForcedFocus(true);

            /*
            
            // Neither the clicked component or it's immediate parent
            // was force-focusable: leave us to the regular focus system.

            // If we just cancelled force focus, make absolutely
            // sure the current "real" focus component knows it has
            // the focus back.
            if (wasForcingFocus && clicked == super.getFocusOwner()) {
                
                // When transferring focus from a forced focus component to regular
                // focus due to a mouse click, and the regular focused component had
                // the focus before the force focus component, it doesn't actually
                // getting the focus back, because the FocusManager thinks it
                // already *has* focus.  So we make sure it's told it has the focus
                // here.
                    
                // todo: we probably need our checking of immedate parent here also
                simulateFocusGained(clicked);
            }
            */
                
        } else {

            // If the new component is force focusable, force focus to it.
                
            //-----------------------------------------------------------------------------
            // Okay, now we know we're in a Window that is not normally focusable,
            // and we've clicked on something we want to allow the focus to be forced
            // to.  We may have actually clicked on an immediate child of such a component,
            // and in that case be sure to record both the parent and child so we
            // know if they click again in the future on either to do nothing to
            // change the focus.
            //-----------------------------------------------------------------------------
                
            if (forceable == clicked) {
                // we clicked directly on something that's forceable
                setForcedFocus(forceable, null, focusCycleRoot);
            } else {
                // we clicked on a child of what's forceable
                setForcedFocus(forceable, clicked, focusCycleRoot);
            }
        }
        
    }
    // end inspectMouseEventForForcableFocusChange


    private static boolean isForceFocusableType(Component c)
    {
        return
               c instanceof javax.swing.text.JTextComponent
            || c instanceof javax.swing.AbstractButton
            || c instanceof javax.swing.JComboBox
            || c instanceof javax.swing.JTree
            || c instanceof javax.swing.JTable
            ;
                             

        /*
        return (false
                || c instanceof Window
                || c instanceof javax.swing.JScrollPane
                || c instanceof javax.swing.JViewport
                || c instanceof javax.swing.JPanel
                ) == false;
        */
    }


    private static boolean isForceFocusableComponent(Component c) {
        //out("isFocusable: " + c.isFocusable() + " " + tufts.Util.objectTag(c));

        return c.isFocusable() && c.isEnabled() && isForceFocusableType(c);
    }

    /** if Component or it's immediate ancestor is focusable, return the first one that is focusable, otherise null */
    private Component getMouseClickForceFocusableComponent(Component c) {
        
        // The component we may click on, such as an AquaComboBoxButton (a JButton)
        // within a JComboBox may NOT be focusable, while the JComboBox is...
        // need to resolve that, and I don't think we want to force focus
        // on the JButton.
        
        if (isForceFocusableComponent(c))
            return c;
        
        Component parent = c.getParent();
        if (parent != null && parent.isFocusable() && isForceFocusableType(parent))
            return parent;
        else
            return null;
    }


    /**
     * Return the first parent Container of c for which isFocusCycleRoot() returns true.
     * @param c - component to find focus cycle root of
     */
    public static Container getFocusCycleRoot(final Component c)
    {
        if (c instanceof Container && ((Container)c).isFocusCycleRoot()) {
            if (DEBUG.FOCUS) out("  focusCycleRoot of " + name(c) + ": is it's own root.");
            return (Container) c;
        }

        Container parent = c.getParent();
        if (parent == null) {
            if (DEBUG.FOCUS) out("  focusCycleRoot of " + name(c) + ": has null parent, using self.");
            return (Container) c;
        }
        do {
            if (parent.isFocusCycleRoot()) {
                if (DEBUG.FOCUS) out("  focusCycleRoot of " + name(c) + " == " + name(parent));
                return parent;
            }
                if (DEBUG.FOCUS && DEBUG.META) out("  focusCycleRoot of " + name(c) + " != " + name(parent));
            parent = parent.getParent();
        } while (parent != null);

        if (DEBUG.FOCUS) out("failed to find focusCycleRoot for " + c);
        return null;
    }

    /**
     * If Container 'c' is Window, it is returned.  Otherwise the first parent that
     * is a Window is returned.
     * @param c - component to find focus cycle root of
     */
    public static Window getFocusCycleWindow(final Component c)
    {
        Container parent;

        if (c instanceof Container)
            parent = (Container) c;
        else
            parent = c.getParent();
                
        if (parent == null) {
            //tufts.Util.printStackTrace("getFocusCycleWindow: null component");
            if (DEBUG.FOCUS) out("focusCycleWindow: null parent");
            return null;
        }

        do {
            if (parent instanceof Window) {
                if (DEBUG.FOCUS) {
                    out("focusCycleWindow of " + name(c) + " == " + name(parent)
                        + " peer=" + name(parent.getPeer())
                        + " parent=" + name(parent.getParent())
                        + " focusable=" + parent.isFocusable()
                        + " focusableWin=" + ((Window)parent).isFocusableWindow()
                        );
                    //tufts.macosx.Screen.getWindow(parent).getParent();
                    //tufts.macosx.Screen.dumpWindows();
                }
                return (Window) parent;
            }
            parent = parent.getParent();
        } while (parent != null);

        if (DEBUG.FOCUS) out("failed to find focusCycleWindow for " + c);
        return null;
    }

    private synchronized void setForcedFocus(Component c) {
        setForcedFocus(c, null, getFocusCycleRoot(c));
    }
        
    /* child will usually be null */
    private synchronized void setForcedFocus(Component forceable, Component child, Container focusCycleRoot)
    {
        if (DEBUG.FOCUS) out(TERM_GREEN + "SETTING FORCED FOCUS " + name(forceable) + TERM_CLEAR);
        
        if (mForcingFocus) {

            // We're currently forced focus, so this is just
            // a focus-transfer among force-focusable components.
            // Clear the current forced focus, but not "permanently"

            if (mForcedFocus == forceable)
                return;
            else
                clearForcedFocus(false);

        } else {
        
            // We're beginning a new forced-focus ownership --
            // convince the real focus owner it's lost focus
            // with a simulated event, and remember it for
            // later focus return once the forced-focus is over.

            mFocusOwnerBeforeForced = super.getFocusOwner();
            if (mFocusOwnerBeforeForced != null)
                simulateFocusLost(mFocusOwnerBeforeForced, forceable);
        }
        
        mForcedFocus = forceable;
        mForcedFocusClickedChild = child;
        mForcedFocusCycleRoot = focusCycleRoot;
        mForcedFocusWindow = getFocusCycleWindow(focusCycleRoot);
        mForcingFocus = true;
        simulateFocusGained(mForcedFocus);
        
    }

    // TODO: if we're forced focus, and another component REQUESTS focus (such as the
    // MapViewer via MOUSE_ENTERED), the forced-focus component gets a REAL FOCUS_LOST
    // event (as getFocusOwner is overridden to claim it as real focus owner), and the
    // new component gets a REAL FOCUS_GAINED event, so our fake events are redundant in
    // that case.  Would be safer not to have the redundant events, but I don't think
    // it's going to hurt anything for now.  We could handle this by catching the
    // FOCUS_LOST event on our forced-focus, and clear forced focus then...

    private synchronized void clearForcedFocus(boolean permanent)
    {
        if (mForcingFocus == false)
            return;
            
        Component c = mForcedFocus;
                    
        // clear mForcedFocus before simulating focus loss
        // in case that triggers any calls to KeyboardFocusManager
        // asking about focus, and we don't want to be lying at
        // that point.
            
        mForcedFocus = null;
        mForcedFocusClickedChild = null;
        mForcedFocusCycleRoot = null;
        mForcingFocus = false;
        
        if (DEBUG.FOCUS) out(TERM_GREEN + "CLEARED FORCED FOCUS "
                             + name(c)
                             + " permanent=" + permanent
                             + " priorOwner=" + name(mFocusOwnerBeforeForced)
                             + TERM_CLEAR);

        simulateFocusLost(c, null);
        
        if (permanent) {
            if (mFocusOwnerBeforeForced != null) {

                final Component realFocusOwner = super.getFocusOwner();

                if (mFocusOwnerBeforeForced != realFocusOwner && realFocusOwner != null) {
                    Util.printStackTrace("incorrect focus return: "
                                         + mFocusOwnerBeforeForced
                                         + " real=" + realFocusOwner);
                    simulateFocusGained(realFocusOwner);
                } else {
                    simulateFocusGained(mFocusOwnerBeforeForced);
                }
                
                mFocusOwnerBeforeForced = null;
            }
        }
    }


    /**
     * Class simply for marking the FocusEvent's we simulate in case anyone ever
     * needs to know it's not a "real" focus event.
     */
    private static class FakeFocusEvent extends java.awt.event.FocusEvent {
        FakeFocusEvent(Component source, int id, boolean temporary, Component opposite) {
            super(source, id, temporary, opposite);
        }
    }

    private static final Component NullComponent = new java.awt.Canvas();
    
    private static class VUEKeyEvent extends KeyEvent {
        public VUEKeyEvent(KeyEvent e) {
            super(e.getComponent() == null ? NullComponent : e.getComponent(),
                  e.getID(),
                  e.getWhen(),
                  e.getModifiers(),
                  e.getKeyCode(),
                  e.getKeyChar(),
                  e.getKeyLocation());
            {
                setSource(e.getSource());
            }
        }

        public String paramString() {
            return "*VUE* " + super.paramString();
        }
    }

    /*
    private static classMarkedMouseEvent extends MouseEvent {
        public MarkedMouseEvent(MouseEvent e) {
            super(e.getComponent(),
                  e.getID(),
                  e.getWhen(),
                  e.getModifiers(),
                  e.getX(),
                  e.getY(),
                  99, //e.getClickCount(),
                  e.isPopupTrigger(),
                  e.getButton()); {}
        }
    }
    */

    private void simulateFocusGained(Component c)
    {
        if (LieImpl) {
            AWTEvent fakeEvent = new FakeFocusEvent(c, FocusEvent.FOCUS_GAINED, false, null);
            if (DEBUG.FOCUS) out("DELIVERING SIMULATED " + eventName(fakeEvent));
            super.redispatchEvent(c, fakeEvent);
        } else {
            super.setGlobalFocusOwner(c);
        }
    }

    private void simulateFocusLost(Component c, Component focusLostTo)
    {
        AWTEvent fakeEvent = new FakeFocusEvent(c, FocusEvent.FOCUS_LOST, false, focusLostTo);
        if (DEBUG.FOCUS) out("DELIVERING SIMULATED " + eventName(fakeEvent));
        super.redispatchEvent(c, fakeEvent);
    }


    private void trace(String s, Object arg) {
        if (false && DEBUG.FOCUS && DEBUG.META) {
            final String div = mForcingFocus ? ": (LIE) " : ": ";
            out(tufts.vue.VueUtil.pad(' ', 28, s, true) + div + name(arg));
        }
    }

    /**
     * In our override situation, because the Window that is the focus cycle root is not
     * a focusable Window (isFocusableWindow() returns false), it will not permit any
     * focus transfers (See java.awt.Component.requestFocusHelper).  We override the
     * focus transfer code here (focusNextComponent, focusPreviousComponent) to ignore
     * the focusability of the focus cycle root if we're in a forcing focus situation,
     * and manually transfer focus to the next available JTextComponent (skipping all
     * other components).  If we're not already forcing the focus, we do the default
     * behaviour by handing the request back up to the superclass.
     */
    
    private void forcedTransferFocus(Component currentFocus, Object direction)
    {
        FocusTraversalPolicy policy = mForcedFocusCycleRoot.getFocusTraversalPolicy();
        Component toFocus;
        if (direction == FOCUS_FORWARD)
            toFocus = policy.getComponentAfter(mForcedFocusCycleRoot, currentFocus);
        else
            toFocus = policy.getComponentBefore(mForcedFocusCycleRoot, currentFocus);
            
        while (isForceFocusableComponent(toFocus) == false) {
            if (DEBUG.FOCUS) System.out.println("\tNOT FORCE-FOCUSABLE: " + name(toFocus));
            if (direction == FOCUS_FORWARD)
                toFocus = policy.getComponentAfter(mForcedFocusCycleRoot, toFocus);
            else
                toFocus = policy.getComponentBefore(mForcedFocusCycleRoot, toFocus);
        }
        if (DEBUG.FOCUS)
            System.out.println("\tTRANSFER FOCUS " + direction
                               + " in " + name(mForcedFocusCycleRoot)
                               + " to " + name(toFocus));
        //if (toFocus == null)
                //toFocus = policy.getDefaultComponent(mForcedFocusCycleRoot);
        if (toFocus != null && toFocus != mForcedFocus)
            setForcedFocus(toFocus);
    }


    public void focusNextComponent(Component c) {
        if (DEBUG.FOCUS) trace("focusNextComponent", c);
        if (mForcingFocus)
            forcedTransferFocus(c, FOCUS_FORWARD);
        else
            super.focusNextComponent(c);
    }

    public void focusPreviousComponent(Component c) {
        if (DEBUG.FOCUS) trace("focusPreviousComponent", c);
        if (mForcingFocus)
            forcedTransferFocus(c, FOCUS_BACKWARD);
        else
            super.focusPreviousComponent(c);
    }
        

    public Component getFocusOwner() {
        Component c = super.getFocusOwner();
        if (LieImpl && mForcingFocus) c = mForcedFocus;
        if (DEBUG.FOCUS) trace("getFocusOwner", c);
        //tufts.Util.printStackTrace("getFocusOwner");
        return c;
    }

    public Component getGlobalFocusOwner() throws SecurityException {
        Component c = super.getGlobalFocusOwner();
        if (LieImpl && mForcingFocus) c = mForcedFocus;
        if (DEBUG.FOCUS) trace("getGlobalFocusOwner", c);
        return c;
    }

    public Component getPermanentFocusOwner() {
        Component c = super.getPermanentFocusOwner();
        if (LieImpl && mForcingFocus) c = mForcedFocus;
        if (DEBUG.FOCUS) trace("getPermanentFocusOwner", c);
        return c;
    }

    public Component getGlobalPermanentFocusOwner() {
        Component c = super.getGlobalPermanentFocusOwner();
        if (LieImpl && mForcingFocus) c = mForcedFocus;
        if (DEBUG.FOCUS) trace("getGlobalPermanentFocusOwner", c);
        return c;
    }

    protected void setGlobalPermanentFocusOwner(Component c) {
        if (DEBUG.FOCUS) trace("setGlobalPermanentFocusOwner", c);
        super.setGlobalPermanentFocusOwner(c);
    }
        
    public void clearGlobalFocusOwner() {
        if (DEBUG.FOCUS) trace("clearGlobalFocusOwner; force", mForcedFocus);
        if (DEBUG.FOCUS) out("KeyboardFocusManager.clearGlobalFocusOwner: honoring");
        
        //-------------------------------------------------------
        // We trust the DefaultKeyboardFocusManager in this case.
        // E.g., the entire application just lost focus.
        //-------------------------------------------------------

        // In this case, may be able to skip the simulated FOCUS_LOST
        // event, as one has sometimes just come through, even
        // for our forced focus component!  But we deliver it just
        // in case...  be on the lookout for problems related to
        // multiple FOCUS_LOST events tho.  We can fix this easily
        // if need be.
        
        clearForcedFocus(true);
        
        super.clearGlobalFocusOwner();
    }

    protected Window getGlobalFocusedWindow() throws SecurityException {
        Window w = super.getGlobalFocusedWindow();
        if (LieImpl && mForcingFocus) w = mForcedFocusWindow;
        if (DEBUG.FOCUS) trace("getGlobalFocusedWindow", w);
        return w;
    }

    protected void setGlobalFocusedWindow(Window w) {
        if (DEBUG.FOCUS) trace("setGlobalFocusedWindow", w);
        super.setGlobalFocusedWindow(w);
    }

    public Window getFocusedWindow() {
        Window w = super.getFocusedWindow();
        if (LieImpl && mForcingFocus) w = mForcedFocusWindow;;
        if (DEBUG.FOCUS) trace("getFocusedWindow", w);
        return w;
    }

    public Window getActiveWindow() {
        Window w = super.getActiveWindow();
        if (LieImpl && mForcingFocus) w = mForcedFocusWindow;
        if (DEBUG.FOCUS) trace("getActiveWindow", w);
        return w;
    }

    protected Window getGlobalActiveWindow() throws SecurityException {
        Window w = super.getGlobalActiveWindow();
        //if (mForcingFocus) w = mForcedFocusWindow;
        if (DEBUG.FOCUS) trace("getGlobalActiveWindow", w);
        return w;
    }

    protected void setGlobalActiveWindow(Window w) {
        if (DEBUG.FOCUS) trace("setGlobalActiveWindow", w);
        super.setGlobalActiveWindow(w);
    }

    public java.awt.Container getCurrentFocusCycleRoot() {
        java.awt.Container c = super.getCurrentFocusCycleRoot();
        if (LieImpl && mForcingFocus) c = mForcedFocusCycleRoot;
        if (DEBUG.FOCUS) trace("getCurrentFocusCycleRoot", c);
        return c;
    }
        
    protected java.awt.Container getGlobalCurrentFocusCycleRoot() throws SecurityException {
        java.awt.Container c = super.getGlobalCurrentFocusCycleRoot();
        if (LieImpl && mForcingFocus) c = mForcedFocusCycleRoot;
        if (DEBUG.FOCUS) trace("getGlobalFocusCycleRoot", c);
        return c;
    }
        
    private static void out(String s) {
        final String state;
        if (mForcingFocus)
            state = TERM_GREEN + "FORCING " + TERM_CLEAR;
        else
            state = null;
        Log.debug(state == null ? s : (state + s));
    }

//     private static void out(String s) {
//         String name;
//         if (mForcingFocus)
//             name = TERM_GREEN + "FocusManager " + TERM_CLEAR;
//         else
//             name = "FocusManager ";
//         System.err.println(name
//                            + (""+System.currentTimeMillis()).substring(9)
//                            + " " + s);
//     }
    
    private static String name(Object c) {
        return GUI.name(c);
    }
    private static String name(AWTEvent e) {
        return GUI.eventName(e);
    }
    private static String eventName(AWTEvent e) {
        return GUI.eventName(e);
    }

    public static void log_test_main(String args[]) {
        
        VUE.init(args);
        
        Logger focusLog = Logger.getLogger("java.awt.focus.Component");
        System.out.println("GOT LOGGER " + focusLog);
        focusLog.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        focusLog.addHandler(handler);
        focusLog.finest("HELLO, THIS IS LOGGER " + focusLog);
        
    }

    public static void enableAWTFocusLogging() {
        Logger kbdFocusLog = Logger.getLogger("java.awt.focus.KeyboardFocusManager");
        Logger focusLog = Logger.getLogger("java.awt.focus.Component");
        focusLog.setLevel(Level.ALL);
        kbdFocusLog.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        focusLog.addHandler(handler);
        kbdFocusLog.addHandler(handler);
    }

    /*
    public static void main_ToolWindow(String args[]) {

        //enableAWTFocusLogging();
        
        VUE.init(args);
        
        DEBUG.SELECTION=true; // for testing Command-A text-select pass up to VueMenuBar

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-trace")) { 
                UseForcedFocus = false;
            }
        }

        VueFrame Doc0 = new VueFrame("Document");
        
        //ToolWindow Doc0 = new ToolWindow("Document Zero", null, true, true);
        ToolWindow Doc1 = new ToolWindow("Document One", null, true, true);
        ToolWindow Doc2 = new ToolWindow("Has Focusable", null, true, true);

        
        ToolWindow tw = new ToolWindow("Interactive", null);
        //ToolWindow tw = new ToolWindow("Ya Biggie Title", twDoc0.mWindow);
        JPanel panel = new JPanel();
        JLabel l = new JLabel("I am a label");
        panel.add(l);
        VueTextField textField0 = new VueTextField("text");
        VueTextField textField1 = new VueTextField("more text");
        panel.add(textField0);
        if (true) {
            panel.add(textField1);
            panel.add(new JButton("foo"));
            panel.add(new tufts.vue.FontEditorPanel());
        }

        Doc2.addComponent(new VueTextField("field one"));
        Doc2.addComponent(new VueTextField("field two"));

        /*
        VueTextPane textPane = new VueTextPane("textPane");
        textPane.setText("This is a test");
        textPane.setMinimumSize(new java.awt.Dimension(180, 60));
        p.add(textPane);
        *

        tw.addTool(panel);

        // Okay, the only way to ensure something stays on top is
        // to have the children be WINDOWS (not frames), and all
        // parent to the SAME root parent.  Hierarchical
        // keep-on-top doesn't seem to work, and children
        // who are frames will not stay on top.

        //ToolWindow twDoc0 = new ToolWindow("Document Zero", null, true, true);
        //ToolWindow tw0 = new ToolWindow("ToolWindow 0", null);
        ToolWindow tw0 = tw;
        // ToolWindow contents won't take focus if they have no parent?
        //ToolWindow tw0 = new ToolWindow("Inspector", twDoc0.mWindow);
        //tw0.addTool(new LWCInspector());
        ToolWindow tw1 = new ToolWindow("Other", null);

        if (!UseForcedFocus)
            tw0.getWindow().setFocusableWindowState(true);
        
//        tw1.getWindow().setFocusableWindowState(false);

        // everyone teels to be focusable, otherwise we never get an
        // event (e.g., activation loss as an opposite, or focus
        // gained event) that we can get to keep track of stacking
        // order.  Actually tho, in our whole-hog impl, we kind of
        // control that (in the vertical stack) -- but no -- there are
        // still going to be other floating windows.
        // [Okay, we can work around this by catching any mouse-pressed
        // event on a floating window]
        
        JPanel p = new JPanel();
        p.setBackground(java.awt.Color.orange);
        javax.swing.JScrollPane sp =
            new javax.swing.JScrollPane(p,
                                        javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                        javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Doc0.getContentPane().add(sp);

        
        Doc0.setSize(200,100);
        Doc0.setVisible(true);
        Doc1.setVisible(true);
        Doc2.setVisible(true);
        
        tw1.setVisible(true);
        tw0.setVisible(true);


        /*
        Window w = new Window(ToolWindow.getHiddenFrame());
        w.setName("###overrideRedirect###");        
        w.setSize(100,15);
        //w.setBackground(java.awt.SystemColor.window);
        w.show();
        *

        
    }
        */




//     public static void main(String args[]) {

//         VUE.init(args);
        
//         DEBUG.SELECTION=true; // for testing Command-A text-select pass up to VueMenuBar

//         /*
//         for (int i = 0; i < args.length; i++) {
//             if (args[i].equals("-trace")) { 
//                 UseForcedFocus = false;
//             }
//         }
//         */

        
//         if (true) { // MAYBE USEFUL

//             // You'd think we could at LEAST use chaining for full-screen mode?  The
//             // last in the chain could be the full screen window, which is normally not
//             // visible (tho that would prob hide it's children).  Maybe it could be
//             // small and off-screen...

//             // Okay this "works" -- a possibly useful possibility: could enable having a
//             // at least a, full-screen window, but no MDI interface, tho the FSW would
//             // have to be placed "out" so it wouldn't be seen on screen, or we might try
//             // and have it lie about it's visibility.

//             // I do notice we used a Frame, and not a Window, for our old
//             // full screen impl -- i hope that wasn't needed, otherwise this
//             // is also no good: a frame can't be a child of any other window/frame.
//             // Oh, crap: we needed it so there was a menu-bar...  but I don't
//             // think we need that anymore...

//             Frame doc1 = new javax.swing.JFrame("Document");
//             Window doc2 = new DockWindow("Full-Screen", doc1);

//             DockWindow tool = new DockWindow("Tool", doc2);
            
//             doc1.setVisible(true);
//             doc2.setVisible(true);
//             tool.setVisible(true);
//         }

//         if (false) {  // SLIGHT CHANCE USEFUL

//             // chaining experiment (would need to pre-alloc a fixed max # of doc windows)

//             // Okay, on mac, a surprise: tool on top of both (some grand-parentage
//             // there), but of course, doc1 can never come to top over doc2 (which is
//             // also not iconifiable, since it's a dialog, tho we could maybe NSWindow
//             // force that back on)

//             // This only useful if document (map) windows were always tiled
//             // and never overlapped (cause a single order would always be
//             // enforced)

//             Frame doc1 = new javax.swing.JFrame("Document 1");
//             Window doc2 = new javax.swing.JDialog(doc1, "Document 2");

//             DockWindow tool = new DockWindow("Tool", doc2);
            
//             doc1.setVisible(true);
//             doc2.setVisible(true);
//             tool.setVisible(true);
//         }
        
//         if (false) {

//             // This is supposedly the vanilla case that we could
//             // try hacking later (everyone's a sibling), but
//             // we get a problem on the PC!
            
//             // Okay, in this wierd case, on the PC, the tool can be over
//             // only the INACTIVE document, never the active one!
//             // On the Mac, it's just another sibling.
            
//             Frame toolParent = new javax.swing.JFrame("Hidden Tool Parent");

//             Window doc1 = new javax.swing.JFrame("Document 1");
//             Window doc2 = new javax.swing.JFrame("Document 2");

//             DockWindow tool = new DockWindow("Tool", toolParent);
            
//             toolParent.setVisible(true);
//             doc1.setVisible(true);
//             doc2.setVisible(true);
//             tool.setVisible(true);
//         }
        
//         if (false) {
        
//             // In this scenario, child stays on top of both parent &
//             // grandparent on PC, but only on top of parent on mac.
            
//             Frame top = new Frame("Grand Parent");
//             Window parent = new DockWindow("Parent", top);
//             parent.add(new JTextField());
            
//             top.setVisible(true);
//             parent.setVisible(true);
            
//             DockWindow child = new DockWindow("Child", parent);
//             child.add(new JTextField());
            
//             child.setVisible(true);
//         }
        
//         if (false) {

//             // useless on mac (tool always bottom), and enforces parentage
//             // with flashing on the PC...
            
//             Frame docParent = new Frame("Hidden Full Screener");

//             Window doc1 = new javax.swing.JDialog(docParent, "Document 1");
//             Window doc2 = new javax.swing.JDialog(docParent, "Document 2");

//             DockWindow tool = new DockWindow("Tool", doc1);
            
//             docParent.setVisible(true);
//             doc1.setVisible(true);
//             doc2.setVisible(true);
//             tool.setVisible(true);
//         }
        
//         if (false) {
            
//             // Crap: even on PC, tool only stays on top of master

//             Frame top = new javax.swing.JFrame("Master");
//             Frame docParent = new javax.swing.JFrame("Documents");

//             Window doc1 = new javax.swing.JDialog(docParent, "Document 1");
//             Window doc2 = new javax.swing.JDialog(docParent, "Document 2");
//             //Window doc1 = new java.awt.Dialog(docParent, "Document 1");
//             //Window doc2 = new java.awt.Dialog(docParent, "Document 2");
            
//             Window tool = new DockWindow("Tool", top);
//             //tool.add(new JTextField());
            
//             top.setVisible(true);
//             docParent.setVisible(true);
//             doc1.setVisible(true);
//             doc2.setVisible(true);
//             tool.setVisible(true);
//         }
        

        
//     }

    
}
