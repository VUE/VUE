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


package tufts.vue;

import tufts.vue.gui.GUI;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Our own floating window class so we can do things like control
 * decorations, add features like double-click to roll-up, make
 * sticky to other windows when dragged, etc.
 *
 * FYI: this class file is still a bit of an experimental mess.
 * A ToolWindow is ultimately meant to be contain multiple tools,
 * although it's really only ready to go for one tool per window
 * in it's current state.
 *
 * Focus handling for these is quite imperfect right now,
 * so Command/Ctrl-W to close a ToolWindow only sometimes works.
 *
 * @deprecated -- use tufts.vue.gui.DockWindow -- remove this file
 * @version $Revision: 1.53 $ / $Date: 2006-01-20 20:11:52 $ / $Author: sfraize $ 
 */

public class ToolWindow 
    implements MouseListener, MouseMotionListener, KeyListener, FocusListener
{
    private final static int TitleHeight = 14;
    private final static Font TitleFont = new Font("SansSerf", Font.PLAIN, 10);
    private final static int ResizeCornerSize = 14;
    //private final static Icon macWindowClose = VueResources.getIcon("macWindowClose");
    
    private final String mTitle;
    protected final ContentPane mContentPane;
    
    private Point dragStart;
    private Dimension dragSizeStart;

    private boolean isRolledUp = false;
    private Dimension savedSize;

    private final boolean mCreateTitleBar;

    private static int CollapsedHeight = 0;

    private final Window mWindow;
    private final Delegate mDelegate;

    private final boolean isMacAqua;
    private final boolean isMacMetal;

    /**
     * Interface for our Window or Frame delegate.
     * Implementors must also be RootPaineContainer's (e.g., JWindow, JFrame)
     */
    private static interface Delegate extends RootPaneContainer {
        public void setSuperSize(int w, int h);
        public void setSuperVisible(boolean t);
    }

    /**
     * FrameDelegate: For Mac OS X
     *
     * Reasons we use a Frame on Mac OS X:
     *
     * 1: It's the only way to keep the VUE menu bar from going away when we get focus.
     *    Frame's allow us to attach a JMenuBar, and Window's do not.
     *    On the Mac, VUE attaches a duplicate of the main menu bar to every
     *    created Frame, so when Mac OS X forcably switches to the
     *    JMenuBar attached to a Frame when it get's focus (which it does
     *    even if NO menu bar has been defined), it looks like nothing happened.
     *    [NO LONGER TRUE IN MOST RECENT 1.4.2 and 1.5 JVM's 2005-12-02]
     *
     * 2: If using the brushed metal look, Frame's pick it up, but Window's don't.
     *    [Not fully true or no longer true: JIDE floating toolbars, which are windows, pick it up]
     *    [NO LONGER TRUE IN MOST RECENT 1.4.2 and 1.5 JVM's 2005-12-02]
     *
     * There are drawbacks, however:
     *
     * -1: See WindowDisplayAction in VUE.java and tufts.macosx.Screen.java for a frightening
     *     array of hacks to make the Frame windows behave.  (Actually, we'd
     *     probably need these even if we weren't using Frame's).
     *
     * -2: The Frame's "roll-up" size isn't a small as we'd like: they're
     *     forced by the Mac OS X to have a minimum size that's bigger
     *     than we need. (and in the metal look under most recent JVM's, even bigger -- SMF 2005-11-09)
     *     [THIS IS A RESULT OF MAC BRUSHED METAL LOOK, NOT FRAME, AND HAPPENS ON WINDOWS TOO IN THAT CASE]
     *
     * -3: We can't override setLocation on a Frame, to make the window's
     * "sticky" to the edge's of the main window.
     *
     * FYI, if we use Frames on the PC, they can go behind the
     * the main application, which we don't want.
     *
     * Note: as of java 1.5, there is a way to force a window to be on top,
     * which would allow us to use windows on the mac if we can solve the
     * menu bar problem (tho we may still have the missing brushed-metal
     * look problem).   [No longer applies: windows can be used, and stay on top of parent]
     *
     *
     * 2005-12-06 UPDATE: Using a Dialog is nice in that it can be parented
     * to another frame, which, for instance, has the root menu installed.
     * But Dialog's, at least on mac, won't display an iconification button...
     *
     */

    private class FrameDelegate extends JFrame implements Delegate {
    //private class FrameDelegate extends JDialog implements Delegate {
    //private class FrameDelegate extends JWindow implements Delegate {
        FrameDelegate(String title) {
            this(title, null);
        }
        
        FrameDelegate(String title, Frame owner) {
            super(title);               // Frame
            //super(owner, title);        // Dialog
            //super(owner);               // Window

            //-------------------------------------------------------
            // It is important to call setTitle even if we're undecorated for
            // our tufts.macosx.Screen hacks to work.
            //-------------------------------------------------------
            setTitle(title);
            
            //setUndecorated(true);
            setResizable(true);

            setJMenuBar(new tufts.vue.gui.VueMenuBar());


            if (false && VueUtil.isMacPlatform()) {
                addComponentListener(new ComponentAdapter() {
                        public void componentShown(ComponentEvent e) { handleShown(); }
                        public void componentHidden(ComponentEvent e) { handleHidden(); }
                    });
            }

        }

        public void setVisible(boolean show) { ToolWindow.this.setVisible(show); }
        public void setSize(int width, int height) { ToolWindow.this.setSize(width, height); }
        public void setSuperSize(int width, int height) { super.setSize(width, height); }
        public void setSuperVisible(boolean show) {
            if (DEBUG.TOOL) out("setSuperVisible [" + getTitle() + "] (detach immediate)");
            if (show == false) {
                // if going invisible, must be sure to detach from a Cocoa established parent or
                // parent will going invisible also!
                // detachFromMainWindow(getTitle()); TOOLWINDOW-NEW
            }
            super.setVisible(show);
        }

        private void handleShown() {
            if (DEBUG.TOOL) out("handleShown [" + getTitle() + "] (attach later)");
            //attachToMainWindow(getTitle()); // TOOLWINDOW-NEW
        }
        private void handleHidden() {
            if (DEBUG.TOOL) out("handleHidden [" + getTitle() + "] (noop)");

            // As of Mac OS X Tiger (at least 10.4.2), hiding a window
            // (via java setVisible) that has been attached to a
            // parent (via Cocoa NSWindow.addChildWIndow) ALSO hides
            // the parent window!  So we need to detach it from the
            // parrent immediately when it's requested to go
            // invisible, so it's detached by the time the
            // setVisible(false) goes through.  Thus we can't handle
            // this here anymore in this after-the-fact event handler.
            // So it's now handled above in setSuperVisible.
            
            //detachFromMainWindow(getTitle());
        }

        protected void processEvent(AWTEvent e) {
            if (DEBUG.TOOL && (e instanceof MouseEvent == false || DEBUG.META))
                out("processEvent " + e);
            super.processEvent(e);
        }

        public void setContentPane(Container contentPane) {
            if (DEBUG.TOOL) out("setContentPane " + contentPane);
            super.setContentPane(contentPane);
        }

        public String toString() {
            return getClass().getName() + "[" + getName() + "]";
        }
    
    }

    private static void attachToMainWindow(String title) {
        adjustMacWindows(title, null, false);
    }
    // This happens IMMEDIATELY, as opposed to attach, which happens after AWT
    private static void detachFromMainWindow(String title) {
        adjustMacWindows(null, title, true);
    }

    private static void adjustMacWindows(final String ensureShown, final String ensureHidden, final boolean immediate) {
        if (VueUtil.isMacPlatform() && !VUE.inNativeFullScreen()) {
            if (immediate) {
                tufts.Util.adjustMacWindows(VUE.getName() + ":", ensureShown, ensureHidden, VUE.inFullScreen());
            } else {
                VUE.invokeAfterAWT(new Runnable() {
                        public void run() {
                            tufts.Util.adjustMacWindows(VUE.getName() + ":", ensureShown, ensureHidden, VUE.inFullScreen());
                        }
                    });
            }
        }
    }
    static void adjustMacWindows() {
        adjustMacWindows(null, null, false);
    }
    
    
    /**
     * WindowDelegate: The default delegate, a standard window, For PC, etc (all non-mac)
     *
     * One drawback: (todo: bug) The window doesn't inherit all the
     * action command keys from the VUE JMenuBar, so, for example,
     * "Command-1" can make the ObjectInspector appear, but it usually
     * get's focus right away, and then "Command-1" no longer works to
     * make it dissapear until you click back on the main VUE window.
     * May be able to address this bug by creating an invisible frame
     * with a menu-bar attached that get's focus when this window
     * gets focus, but not sure of feasability of handing off focus.
     * But window's aren't focusable until their parents are visible on screen,
     * so maybe the frame can be made "invisible" if moved off screen.
     * If this works, all floating windows could use this "invisible"
     * frame as their parent (i think this is similar to the method
     * net.roydesign / MRJAdapter users).
     *
     * Note: as of java 1.5, there is a way to force a window to be on top,
     * which would allow us to use frame's on the PC if that need should
     * arise, although we ideally want to be moving in the opposite direction
     * and have everything just be windows.
     */
    private class WindowDelegate extends JWindow implements Delegate {
        WindowDelegate(String title, Window owner) {
            super(owner);

            // This identifies it as a palette style window, leaving focus
            // with application Frame's as we'd like.  Unfortunately,
            // this also means that any text widgets that need focus
            // in order to take key input won't, by default, get it anymore.
            // Installing our own KeyboardFocusManager could work around this.
            
            setFocusableWindowState(false);

            // Popup's use this special name, I think to avoid taking focus?
            // It also has a side effect on Mac such that even if brushed metal
            // look is installed, the window won't pick it up (which of course
            // you wouldn't want on a tool-tip pop-up window, or a menu for that
            // matter)
            //setName("###overrideRedirect###");

            setName(title);
            //setName(title + "^(" + tufts.vue.gui.FocusManager.name(owner) + ")");

            // Just to confirm: even in 1.5, Window's that are children of
            // a Frame still make that Frame go inactive if they get focus.
            
            if (DEBUG.TOOL) out("constructed WindowDelegate, child of " + owner);
        }
        public void setVisible(boolean show) { ToolWindow.this.setVisible(show); }
        public void setSize(int width, int height) { ToolWindow.this.setSize(width, height); }
        public void setSuperVisible(boolean show) { super.setVisible(show); }
        public void setSuperSize(int width, int height) { super.setSize(width, height); }
        
        protected void processEvent(AWTEvent e) {
            if (DEBUG.TOOL && (e instanceof MouseEvent == false || DEBUG.META))
                out("processEvent " + e);
            super.processEvent(e);
        }
        
        private static final int StickyDist = 10;
        private static final int ReleaseDist = 100;

        public void setLocation(int x, int y)
        {
            // todo: this is kind of a cheap method, but it works, & allows
            // user override by moving the toolwindow fast ("slamming" it
            // past the sticky edge), or dragging it away
            //out("setLocation0 " + x + "," + y);
            Component c = getParent();
            if (c != null) {
                Rectangle parent = getParent().getBounds();
                //System.out.println("parent at " + parent);
                if (getY() < parent.y + parent.height &&
                    getY() + getHeight() > parent.y) {
                    // We're vertically in the parent plane
                    if (x > getX()) {
                        int toolRightEdge = getX() + getWidth();
                        int reDist = parent.x - toolRightEdge;
                        // mouseDist: distance between would-be lockdown & requested
                        int mouseDist = x - (parent.x - getWidth());
                        if (reDist >= 0 && reDist <= StickyDist && mouseDist < ReleaseDist) {
                            x = parent.x - getWidth();
                            //out("gap " + reDist + ", mouse " + mouseDist);
                        }
                    } else if (x < getX()) {
                        int reDist = getX() - (parent.x + parent.width);
                        // mouseDist: distance between would-be lockdown & requested
                        int mouseDist = (parent.x + parent.width) - x;
                        if (reDist >= 0 && reDist <= StickyDist && mouseDist < ReleaseDist) {
                            x = parent.x + parent.width;
                            //out("gap " + reDist + ", mouse " + mouseDist + ": sticking");
                        }
                    }
                }
            }
            //out("setLocation1 " + x + "," + y);
            super.setLocation(x, y);
        }

        public void setContentPane(Container contentPane) {
            if (DEBUG.TOOL) out("setContentPane " + contentPane);
            super.setContentPane(contentPane);
        }

        public String toString() {
            return getClass().getName() + "[" + getName() + "]";
        }
        
        
    }

    
    public void setVisible(boolean show)
    {
        /*
        java.awt.peer.ComponentPeer peer = getPeer();
        out("PEER=" +peer.getClass() + " " + peer);
        if (peer instanceof apple.awt.CWindow) {
            apple.awt.CWindow cWindow = (apple.awt.CWindow)peer;
            cWindow.setAlpha(0.5f);
        }
        */

        //new Throwable(this + "SET VISIBLE " + show).printStackTrace();

        if (DEBUG.FOCUS || DEBUG.TOOL) out("setVisible " + show);
        if (show && isRolledUp()) {
            setRolledUp(false);
        } else if (!show) {
            if (focusReturn != null) {
                if (DEBUG.FOCUS) out("ToolWindow returning focus to " + focusReturn);
                focusReturn.requestFocus();
            } else {
                if (VueUtil.isMacPlatform() == false) {

                    // We need to do this to ensure VUE still has the
                    // focus after hiding a ToolWindow, which if done
                    // via accellerater keys, often leaves the focus
                    // somewhere in outer space...

                    // We don't need to do it on the mac because we
                    // don't need to manually re-route key events back up
                    // to the JMenuBar for checking against accelerators,
                    // as they already have JMenuBar's active on every ToolWindow.
                    
                    VUE.getActiveViewer().requestFocus();
                }
            }
        }
        //if (DEBUG.TOOL) out("delegate setSuperVisible " + show + " " + mDelegate);
        mDelegate.setSuperVisible(show);
    }

    public ToolWindow(String title, Window owner) {
        this(title, owner, true);
    }
    /*
    private ToolWindow(String title, ToolWindow owner) {
        this(title, owner.mWindow, true);
    }
    */
        
    public ToolWindow(String title, Window owner, boolean installTitleBar) {
        this(title, owner, installTitleBar, false);
    }

    private static JFrame HiddenParentFrame;
    public static Frame getHiddenFrame() {
        if (HiddenParentFrame == null) {
            HiddenParentFrame = new JFrame() {
                    public void show() {}

                    // so isFocusableWindow in children can return true even tho we're invisible
                    public boolean isShowing() { return true; }

                    // make sure is never preferred window group for handing focus to
                    // CANNOT do this or an installed menu-bar won't work
                    //public boolean getFocusableWindowState() { return false; } // doesn't help
                    
                    public String toString() { return getName(); }
                };
            HiddenParentFrame.setName("(VUE Hidden)");

            // If we have a menu-bar attached, it must be focusable for the menu bar to work.
            //HiddenParentFrame.setFocusableWindowState(false);
            
            // Don't need to attach MenuBar now that ToolWindow's do NOT "officially" take the focus at all --
            // the active frame with it's attached menu bar stays active.
            //HiddenParentFrame.setJMenuBar(new tufts.vue.gui.VueMenuBar());
            
            // fortunately, does NOT need to be visible for menu bar to work
            HiddenParentFrame.setVisible(false);
        }

        return HiddenParentFrame;
    }
    

    public ToolWindow(String title, Window owner, boolean installTitleBar, boolean frameDelegate)
    {
        mTitle = title;

        if (owner == null) {
            owner = getHiddenFrame();
            //out("using HiddenParentFrame");
        }
        
        //if (false && VueUtil.isMacPlatform()) { // we no longer need FrameDelegate -- mac fixed the bugs
        if (frameDelegate) {
            mWindow = new FrameDelegate(title, (Frame)owner);
        } else {
            // TODO: if owner is null, use our own offscreen owner so can take
            // focus, or hack up our own KeyboardFocusManager?
            // Could have a Frame who's show is overriden to do nothing
            // (like SwingUtility shared parent), yet we could also
            // have isShowing return true, so that isFocusableWindow
            // in a child will think it's parent is visible on-screen.
            mWindow = new WindowDelegate(title, owner);
        }
        //sWindowTracker.add(mWindow);
        
        mDelegate = (Delegate) mWindow;
        isMacAqua = GUI.isMacAqua();
        isMacMetal = GUI.isMacBrushedMetal();
        
        mCreateTitleBar = installTitleBar;
        //mCreateTitleBar = true;
        if (true /*mCreateTitleBar*/) {
            // we're not using OS title-bars at all
            mWindow.addMouseListener(this);
            mWindow.addMouseMotionListener(this);
            mWindow.addKeyListener(this);
        }

        //if (debug) out("contentPane=" + mWindow.getContentPane());
        mContentPane = new ContentPane(mTitle);
        //mContentPane.setFocusable(false);
        //getContentPane().add(mContentPane);

        // createg lass pane for painting resize corner
        Component glassPane = new GlassPane();

        if (mWindow instanceof JFrame) {
            if (mCreateTitleBar)
                ((JFrame)mWindow).setContentPane(mContentPane);
            ((JFrame)mWindow).setGlassPane(glassPane);
        } else if (mWindow instanceof JDialog) {
            if (mCreateTitleBar)
                ((JDialog)mWindow).setContentPane(mContentPane);
            ((JDialog)mWindow).setGlassPane(glassPane);
        } else { // is JWindow
            if (mCreateTitleBar)
                ((JWindow)mWindow).setContentPane(mContentPane);
            ((JWindow)mWindow).setGlassPane(glassPane);
        }
        glassPane.setVisible(true);

        // todo checkout: setting content-pane v.s. adding to it may affect glass pane?
        // seems to be working fine now...

        mWindow.pack();
        
        if (DEBUG.INIT) out("constructed.");
        
        //setLocationRelativeTo(owner);
        //setFocusableWindowState(false); // nothing can get input at all...
        //setFocusable(false); doesn't appear to do anything; todo: play with later & sub-panels
        //setFocusable(false);
        //getContentPane().setFocusable(false);
        //addFocusListener(this); // should never see...


        /*
          // Window open & close only come to us the first time the window happens
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {System.out.println(e); setButtonState(false); }
                public void windowOpened(WindowEvent e) {System.out.println(e); setButtonState(true); }
            });
        addWindowStateListener(new WindowStateListener() {
                public void windowStateChanged(WindowEvent e) {
                    System.out.println("ToolWindow: " + e);
                }
            });
        */

        if (CollapsedHeight == 0) {
            CollapsedHeight = TitleHeight;
            if (isMacAqua == false)
                CollapsedHeight += 4;;
        }

        // set a default size
        setSize(180,100);
        
    }

    public Window getWindow() {
        return mWindow;
    }

    public RootPaneContainer getRootPaneContainer() {
        return mDelegate;
    }

    public JComponent getContentPane() {
        return mContentPane;
    }

    public JRootPane getRootPane() {
        if (mWindow instanceof JWindow)
            return ((JWindow)mWindow).getRootPane();
        else
            return ((JFrame)mWindow).getRootPane();
    }


    // todo: problem: we can see this, but the global Command-W action is ALSO
    // getting activated!
    public void keyPressed(KeyEvent e)
    {
        if (DEBUG.KEYS) out("[" + e.paramString() + "]");

        if (e.getKeyCode() == KeyEvent.VK_W && (e.getModifiers() & Actions.COMMAND) != 0)
            setVisible(false);
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    
    public void focusLost(FocusEvent e)
    {
        if (DEBUG.FOCUS) out("focusLost to " + e.getOppositeComponent());
        Actions.CloseMap.setEnabled(true); // hack
    }
    
    private Component focusReturn;
    public void focusGained(FocusEvent e)
    {
        if (DEBUG.FOCUS) out("focusGained from " + e.getOppositeComponent());
        focusReturn = e.getOppositeComponent();
        Actions.CloseMap.setEnabled(false); // hack
    }

    /** look for a tabbed pane within us with the given title, and select it */
    public void showTab(final String name) {
        // event raiser wasn't designed for this, but turns out
        // to be very convienent for it.
        EventRaiser e = new EventRaiser(mWindow) {
                public Class getTargetClass() { return JTabbedPane.class; }
                public void dispatch(Object pTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) pTabbedPane;
                    int i = tabbedPane.indexOfTab(name);
                    if (i >= 0)
                        tabbedPane.setSelectedIndex(i);
                }
            };
        e.raiseStartingAt(mWindow);
        setVisible(true);
    }

    public void addTool(JComponent c) {
        addTool(c, false);
    }

    public void addComponent(JComponent c) {
        getContentPanel().setLayout(new FlowLayout());
        getContentPanel().add(c);
    }

    public void addTool(JComponent c, boolean addBorder)
    {
        if (DEBUG.INIT) out("adding " + c.getClass());
        // todo: make it so can add more than one tool
        // -- probably use BoxLayout
        getContentPanel().add(c, BorderLayout.CENTER);
        
        // this is hack till glass pane can redispatch mouse events so
        // that mouse listening tools don't disable the resize corner
        MouseListener[] ml = c.getMouseListeners();
        if (addBorder || ml.length > 0) {
            if (DEBUG.TOOL)
                getContentPanel().setBorder(new LineBorder(Color.lightGray, 5));
            else
                getContentPanel().setBorder(new EmptyBorder(5,5,5,5));
        }
        mWindow.pack();
        if (DEBUG.INIT) out("added " + c + " mouseListeners=" + ml.length);
    }

    private JPanel getContentPanel() {
        return mContentPane.contentPanel;
    }

    public void add(JComponent c) {
        addTool(c);
    }
        
    public String getTitle() {
        return mTitle;
    }
    
    public void setSize(int width, int height)
    {
        //System.out.println("setSize " + width + "x" + height);
        if (width < ResizeCornerSize * 3)
            width = ResizeCornerSize * 3;
        if (isRolledUp()) {
            if (height < CollapsedHeight)
                height = CollapsedHeight;
        } else {
            if (height < TitleHeight + ResizeCornerSize)
                height = TitleHeight + ResizeCornerSize;
        }
        /*
        Dimension min = getMinimumSize();
        if (width < min.width)
            width = min.width;
        if (height < min.height)
            height = min.height;
        */
        mDelegate.setSuperSize(width, height);
        mWindow.validate();
    }

    
    public void suggestLocation(int x, int y) {
        Rectangle limit = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        limit.width += limit.x;
        limit.height += limit.y;
        //out("suggesting location " + x + "," + y + " in limits " + limit);
        if (x + getWidth() > limit.width)
            x = limit.width - getWidth();
        if (x < limit.x)
            x = limit.x;
        if (y + getHeight() > limit.height)
            y = limit.height - getHeight();
        if (y < limit.y)
            y = limit.y;
        mWindow.setLocation(x, y);
    }
    
    private void setRolledUp(boolean t) {
        if (isRolledUp == t)
            return;
        isRolledUp = t;
        if (isRolledUp) {
            savedSize = mWindow.getSize();
            setSize(getWidth(), 0);
            getContentPanel().setVisible(false);
        } else {
            setSize(savedSize.width, savedSize.height);
            //setSize(getPreferredSize());
            getContentPanel().setVisible(true);
        }
    }

    public int getWidth() { return mWindow.getWidth(); }
    public int getHeight() { return mWindow.getHeight(); }
    public int getX() { return mWindow.getX(); }
    public int getY() { return mWindow.getY(); }
    public Dimension getSize() { return mWindow.getSize(); }

    public boolean isRolledUp() {
        return isRolledUp;
    }
    
    protected boolean resizeCornerHit(MouseEvent e) {
        return
            e.getX() > getWidth() - ResizeCornerSize &&
            e.getY() > getHeight() - ResizeCornerSize
            ||
            e.getX() > getWidth() - 25 && e.getY() < TitleHeight; // tmp hack extra resize
        
    }

    protected boolean closeCornerHit(MouseEvent e) {
        return e.getX() > getWidth() - TitleHeight && e.getY() < TitleHeight;
        
    }
    
    public void mousePressed(MouseEvent e)
    {
        if (DEBUG.MOUSE) out(e);
        mWindow.requestFocus(); // must do this to get key input
        //System.out.println(e);
        dragStart = e.getPoint();
        // todo: will need to check this on the glass pane
        // in case underlying panel is also grabbing
        // mouse events (and then redispatch)
        if (closeCornerHit(e))
            setVisible(false);
        else if (!isRolledUp() && resizeCornerHit(e))
            dragSizeStart = getSize();
    }
    
    public void mouseReleased(MouseEvent e)
    {
        if (DEBUG.MOUSE) out(e);
        dragStart = null;
        dragSizeStart = null;
        //System.err.println(e);
    }
    public void mouseDragged(MouseEvent e)
    {
        //System.out.println(e);
        Point p = e.getPoint();

        if (dragSizeStart != null) {
            // resizing
            int newWidth = dragSizeStart.width + (e.getX() - dragStart.x);
            int newHeight = dragSizeStart.height + (e.getY() - dragStart.y);
            setSize(newWidth, newHeight);
        } else {
            if (dragStart == null) {
                out("mouseDragged with no dragStart!");
                return;
            }
            // moving the window
            p.x += this.getX();
            p.y += this.getY();
            // now we have the absolute screen location
            p.x -= dragStart.x;
            p.y -= dragStart.y;
            mWindow.setLocation(p);
        }
        //System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
    }
    
    public void mouseClicked(MouseEvent e) {
        if (DEBUG.MOUSE) out(e);
        if (e.getClickCount() == 2)
            setRolledUp(!isRolledUp());
    }
    
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    private void out(Object o) {
        System.out.println(this + " " + (o==null?"null":o.toString()));
    }

    public String toString() {
        return "ToolWindow[" + mTitle + "]";
    }


    private boolean processKeyBindingsToMenuBar = true;
    void setProcessKeyBindingsToMenuBar(boolean t) {
        processKeyBindingsToMenuBar = t;
    }
    

    protected class ContentPane extends JPanel
    {
        JPanel titlePanel;
        JPanel contentPanel = new JPanel();
        
        protected void processEvent(AWTEvent e) {
            if (DEBUG.TOOL) System.out.println(this + " processEvent " + e);
            super.processEvent(e);
        }

        protected void processKeyEvent(KeyEvent e) {
            if (DEBUG.TOOL) System.out.println(this + " processKeyEvent " + e);
            super.processKeyEvent(e);
        }

        /** we need this to hand off command keystrokes to the VUE menu bar */
        /*
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
        {
            // We want to ignore vanilla typed text as a quick-reject culling mechanism.
            // So only if any modifier bits are on (except SHIFT), do we attempt to
            // send the KeyStroke to the JMenuBar to check against any accelerators there.
            final int PROCESS_MASK = InputEvent.CTRL_MASK | InputEvent.META_MASK | InputEvent.ALT_MASK | InputEvent.ALT_GRAPH_MASK;

            // On Mac, we've already installed JMenuBar's on our JFrame
            // so they appear at the top of the screen, and they'll
            // handle the key events.  On PC, we have to manually
            // redirect the key events to the main VueMenuBar.
            
            //if (getRootPane().getJMenuBar() != null)
            if (processKeyBindingsToMenuBar == false)
                return super.processKeyBinding(ks, e, condition, pressed);

            if ((e.getModifiers() & PROCESS_MASK) == 0 && !e.isActionKey())
                return super.processKeyBinding(ks, e, condition, pressed);
                

            if (DEBUG.TOOL||DEBUG.FOCUS) System.out.println("TWCP PKB " + ks + " condition="+condition);
            // processKeyBinding never appears to return true, and t his damn key event never
            // gets marked as consumed (I guess we'd have to listen for that in our text fields
            // and consume it), so we're always passing the damn character up the tree...
            if (super.processKeyBinding(ks, e, condition, pressed) || e.isConsumed())
                return true;
            // Condition usually comes in as WHEN_ANCESTOR_OF_FOCUSED_COMPONENT (1), which doesn't do it for us.
            // We need condition (2): WHEN_IN_FOCUSED_WINDOW
            int newCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;
            if (DEBUG.TOOL||DEBUG.FOCUS) System.out.println("     PKB " + ks + " handing to VueMenuBar w/condition=" + newCondition);
            try {
                return VUE.getJMenuBar().doProcessKeyBinding(ks, e, newCondition, pressed);
            } catch (NullPointerException ex) {
                System.err.println("ToolWindow: no menu bar: " + ex);
                return false;
            }
        }
        */
        
        public ContentPane(String title)
        {
            super(true); // requesting double-buffering doesn't seem to do much to help title flashing
            
            // todo -- need to have at least title click-able
            // without this window grabbing focus so you can
            // at least drag these windows without main
            // losing focus.  Also, should be able to make
            // it so you can get focus and not have the parent
            // window go inactiveat all, which is happening
            // on OSX.

            setLayout(new BorderLayout());
            contentPanel.setLayout(new BorderLayout());
            addKeyListener(ToolWindow.this);

            if (mCreateTitleBar) {
                installTitlePanel(title);
                add(contentPanel, BorderLayout.CENTER);
            } else {
                setBorder(new LineBorder(Color.red)); // not seeing in ToolPalette
                // is it JToolBar SETTING the content pane or adding
                // to it?  I think the latter, and if so should be able to add border
                // to content pane easily enough, right?
            }
        }

        private void installTitlePanel(String title)
        {
            titlePanel = new JPanel();
            titlePanel.setPreferredSize(new Dimension(0, TitleHeight));
            if (isMacAqua) {
                add(titlePanel, BorderLayout.NORTH);
                contentPanel.setBorder(new LineBorder(Color.gray));
            } else {
                setBorder(new BevelBorder(BevelBorder.RAISED));

                if (VueUtil.isMacPlatform())
                    titlePanel.setBackground(SystemColor.control);
                else
                    titlePanel.setBackground(SystemColor.activeCaption);
                titlePanel.setForeground(SystemColor.activeCaptionText);
                titlePanel.setBorder(new LineBorder(SystemColor.activeCaptionBorder));

                //contentPanel.setBorder(new LineBorder(Color.green, 5));

                add(titlePanel, BorderLayout.NORTH);
                //contentPanel.add(titlePanel, BorderLayout.NORTH);
            }

            
            if (title != null) {
                titlePanel.setLayout(new BorderLayout());
                JLabel l = new JLabel(title);
                int topPad = isMacAqua ? 2 : 1;
                l.setBorder(new EmptyBorder(topPad,2,0,0));
                l.setFont(TitleFont);
                l.setForeground(SystemColor.activeCaptionText);
                l.setSize(l.getPreferredSize());
                titlePanel.add(l, BorderLayout.WEST);
                titlePanel.add(new JLabel(new CloseIcon()), BorderLayout.EAST);
            }
        }
        private class CloseIcon implements Icon {
            private final int iconSize;
            private final int iconWidth;
            private final int iconHeight;
            private final java.awt.BasicStroke X_STROKE;
            
            public CloseIcon() {
                X_STROKE = new java.awt.BasicStroke(1.3f);
                iconSize = TitleHeight - 5;
                if (isMacAqua) {
                    iconWidth = iconSize + 3;
                    iconHeight = iconSize + 1;
                } else {
                    iconWidth = iconSize + 2;
                    if (VueUtil.isMacPlatform())
                        iconHeight = iconSize + 3;
                    else
                        iconHeight = iconSize + 2;
                }
            }
            
            public int getIconWidth() { return iconWidth; }
            public int getIconHeight() { return iconHeight; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                int xoff = x;
                int yoff = y;
                if (isMacMetal == false) {
                    //g.setColor(SystemColor.activeCaption);
                    // we fill in case window is so narrow that title text
                    // would appear under this icon: we don't want to see that.
                    g.setColor(Color.white);
                    g.fillRect(xoff, yoff, iconSize,iconSize);
                }
                g.setColor(Color.darkGray);
                g.drawRect(xoff, yoff, iconSize,iconSize);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                ((Graphics2D)g).setStroke(X_STROKE);
                //g.setColor(Color.red);
                int inset = 2;
                int len = iconSize - inset;
                // UL to LR
                g.drawLine(xoff+inset, yoff+inset, xoff+len, yoff+len);
                // LL to UR
                g.drawLine(xoff+inset, yoff+len, xoff+len, yoff+inset);
            }
            
        }

        private String out(Color c) {
            return "color[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
        }
    }
    


    
    /**
     * We provide our own glass pane so that it can paint the resize corner.
     */
    private class GlassPane extends JComponent
    {
        // todo: because this is a glass pane, it is called every single time
        // a cursor flashes on any component in the ToolWindow.  Note however
        // that the paint code won't actually write bits to the screen unless
        // it's within getClipBounds().
        public void paintComponent(Graphics g) {
            if (!isRolledUp())
                paintResizeCorner((Graphics2D)g);
        }

        private void paintResizeCorner(Graphics2D g)
        {
            if (DEBUG.PAINT) System.err.println(mTitle + " GlassPane.paintResizeCorner " + g.getClipBounds());
            
            int w = ToolWindow.this.getWidth();
            int h = ToolWindow.this.getHeight();
            int x = w - ResizeCornerSize;
            int y = h - ResizeCornerSize;

            int right = w - 1;
            int bottom = h - 1;
            final Color oldColor = g.getColor();
            g.setColor(Color.gray);
            for (int i = 0; i < ResizeCornerSize/2; i++) {
                g.drawLine(x,bottom, right,y);
                x += 2;
                y += 2;
            }
            if (DEBUG.BOXES) {
                g.setColor(Color.green);
                g.drawRect(w-ResizeCornerSize,h-ResizeCornerSize,w,h);
            }
            g.setColor(oldColor);
            
        }

        public GlassPane() {
            // Adding a mouse listener on the glass pane grabs
            // all mouse events -- none ever get to the contents.
            // We'll have to retarget the mouse events if want to
            // ability to override for resize corner hit detection.
            //addMouseListener(new MouseAdapter("GlassPane"));
            
            // test: fyi, this doesn't show up:
            //setLayout(new FlowLayout());
            //add(new JLabel("foobie"));
        }
    }

    public static void main(String args[]) {
        VUE.init(args);
        DEBUG.BOXES=true;
        DEBUG.KEYS=true;
        
        ToolWindow twDoc0 = new ToolWindow("Document Zero", null, true, true);
        
        ToolWindow tw = new ToolWindow("Ya Biggie Title", null);
        //ToolWindow tw = new ToolWindow("Ya Biggie Title", twDoc0.mWindow);
        JPanel p = new JPanel();
        p.setBorder(new TitledBorder("Yippity Vue Tool"));
        JLabel l = new JLabel("I am a label");
        p.add(l);
        JTextField tf = new JTextField(5);
        tf.setText("text");
        tf.setEditable(true);//no need
        tf.setFocusable(true);//no need
        p.add(tf);
        p.add(new JButton("foo"));
        tw.addTool(p);
        //tw.setSize(240,200);
        
        //tw.setVisible(true);

        // Okay, the only way to ensure something stays on top is
        // to have the children be WINDOWS (not frames), and all
        // parent to the SAME root parent.  Hierarchical
        // keep-on-top doesn't seem to work, and children
        // who are frames will not stay on top.

        if (true) {
            //-------------------------------------------------------
            // java 1.5 test of setAlwaysOnTop
            //-------------------------------------------------------
            // This works perfectly.  Damn, we really need to switch to java 1.5.
            // And we wouldn't need to deal with root windows.
            // Okay, not PERFECTLY: alwaysOnTop on mac sets it to even be
            // on top of all other APPLICATIONS!


            //ToolWindow twDoc0 = new ToolWindow("Document Zero", null, true, true);
            ToolWindow twDoc1 = new ToolWindow("Document One", null, true, true);
            //ToolWindow tw0 = new ToolWindow("ToolWindow 0", null);
            ToolWindow tw0 = tw;
            // ToolWindow contents won't take focus if they have no parent?
            //ToolWindow tw0 = new ToolWindow("Inspector", twDoc0.mWindow);
            //tw0.addTool(new LWCInspector());
            ToolWindow tw1 = new ToolWindow("ToolWindow 1", null);

            twDoc0.setVisible(true);
            twDoc1.setVisible(true);

            tw0.setVisible(true);
            tw1.setVisible(true);
            
            //tw0.mWindow.setAlwaysOnTop(true);
            //tw1.mWindow.setAlwaysOnTop(true);

            // called indirectly so this compiles in java 1.4
            //tufts.Util.invoke(tw0.mWindow, "setAlwaysOnTop", Boolean.TRUE);
            //tufts.Util.invoke(tw1.mWindow, "setAlwaysOnTop", Boolean.TRUE);
            
        } else {

            ToolWindow root = new ToolWindow("ROOT-0", null, true, true);
            //ToolWindow root1 = new ToolWindow("ROOT-1", null, true, false);
            ToolWindow docRoot = new ToolWindow("DOCUMENT ROOT", root.mWindow, true, false);
                       twDoc0 = new ToolWindow("Document Zero", docRoot.mWindow, true, true);
            ToolWindow twDoc1 = new ToolWindow("Document One", docRoot.mWindow, true, true);
            ToolWindow tw0 = new ToolWindow("ToolWindow 0", root.mWindow);
            ToolWindow tw1 = new ToolWindow("ToolWindow 1", root.mWindow);
            //ToolWindow tw2 = new ToolWindow("ToolWindow 2", root);
            
            root.setVisible(true);
            //root1.setVisible(true);
            docRoot.setVisible(true);
            twDoc0.setVisible(true);
            twDoc1.setVisible(true);
            tw0.setVisible(true);
            tw1.setVisible(true);
        }

        
        // why can't we get the tex field to respond to mouse??
        // must it have an action listener?
        /*
        tw.setFocusable(true);
        p.setFocusable(true);
        tf.setFocusable(true);
        */
    }

    
    
}
