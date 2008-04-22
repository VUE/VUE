/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
import tufts.macosx.MacOSX;

import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.VueUtil;
import tufts.vue.EventRaiser;
import tufts.vue.VueResources;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import edu.tufts.vue.preferences.implementations.WindowPropertiesPreference;

// Just to confirm: even in 1.5, Window's that are children of
// a Frame still make that Frame go inactive if they get focus.

/**
   
 * Our own floating window class so we can do things like control decorations, add
 * features like double-click to roll-up, make sticky to other windows when dragged,
 * etc.

 * Instances of this class, if given no parent Window, turn off focusable window state,
 * which effectively identifies it as a palette style window that always leave focus
 * with application Frame's as we'd like.  Unfortunately, this also means that any
 * components that need focus in order to take key input (such a text field) won't be
 * able to get it (mouse events can still get through).  So if you want text input for
 * anything in one of these windows, this class requires installing your
 * KeyboardFocusManager that can temporarily force the keyboard focus to components that
 * want it within these Windows.  Another side effect is that the cursor can't be
 * changed anywhere in the Window when it's focusable state is false.

 * @version $Revision: 1.125 $ / $Date: 2008-04-22 07:46:20 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class DockWindow extends javax.swing.JWindow
    implements MouseListener
               , MouseMotionListener
               , FocusManager.MouseInterceptor
               , java.beans.PropertyChangeListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DockWindow.class);
    
    public static final int DefaultWidth = 300;
    private static boolean AllVisible = true;
    
    final static LinkedList<DockWindow> AllWindows = new LinkedList();
    final static char RightArrowChar = 0x25B8; // unicode
    final static char DownArrowChar = 0x25BE; // unicode
    //final static String RightArrow = "" + RightArrowChar;
    //final static String DownArrow = "" + DownArrowChar;

    public final static int ToolbarHeight = VueResources.getInt("gui.dockToolbar.height", 70);
    private final static boolean MacWindowShadowEnabled = false;
    private static Border WindowBorder;
    private static Border ContentBorder;
    private static Border ContentBorderInset;
    
    static DockRegion TopDock;
    static DockRegion BottomDock;
    static DockRegion MainDock;

    private static final Color
        TopGradientColor = VueResources.getColor("gui.dockWindow.title.background.top"),
        BottomGradientColor = VueResources.getColor("gui.dockWindow.title.background.bottom");
    
    
    private final static String NORTH_WEST = "northWest";
    private final static String NORTH_EAST = "northEast";
    private final static String SOUTH_WEST = "southWest";
    private final static String SOUTH_EAST = "southEast";
    
    private final static int TitleHeight = VueResources.getInt("gui.dockWindow.title.height", 19);
    private final static int ResizeCornerSize = VueResources.getInt("gui.dockWindow.resizeCorner.size", 12);
    private final static Font TitleFont = VueResources.getFont("gui.dockWindow.title.font", tufts.vue.VueConstants.FONT_MEDIUM);
    private final static int MacAquaMetalMinHeight = 37; // Mac Aqua Brushed Metal window size bug
    
    private final ContentPane mContentPane;
    private JComponent mResizeCorner;
    private JComponent mResizeCorner2;
    //private Action[] mMenuActions;
    
    private String mTitleName;
    private String mMenuName; // if non-null, will be used for name in menus
    private final String mBaseTitle;
    private int mTitleWidth;
    private int mMinTitleWidth;
    private boolean showCloseBtn=true;
    
    private DockWindow mChild;
    private DockWindow mParent;
    private DockWindow mChildWhenHidden;
    private DockWindow mParentWhenHidden;
    private DockRegion mDockRegion;
    private DockWindow mDockNext; // next (right or below) window in DockRegion
    private DockWindow mDockPrev; // previous (left or above) window in DockRegion

    /** point in window mouse was at when drag started */
    private Point mDragStart;
    /** absolute point on screen mouse was at when drag started */
    private Point mDragStartScreen;
    private Dimension mMinContentSize = new Dimension(0,0);
    
    private Dimension mDragSizeStart;
    private boolean mMouseWasPressed;
    private boolean mMouseWasDragged;
    private boolean mWindowDragUnderway;
    private int mMovingStackHeight;

    private boolean isResizeEnabled;
    private boolean isRolledUp;
    private boolean isStackOwner;
    private Rectangle mSavedShape;
    /** set to false as soon as we begin to go invisible */
    private boolean mShowing;
    private boolean mWasVisible;
    private boolean mAnimatingReshape;
    private boolean mStickingRight;
    private boolean mWasStickingRight;
    private boolean mHasWindowShadow = true;
    private boolean isToolbar;

    static int CollapsedHeight = 0;
    /** visible exposed height of parent DockWindow that child window shouldn't overlap */
    private static int CollapsedHeightVisible;
    
            static boolean isMac;
    private static boolean isMacAqua;
    private static boolean isMacAquaMetal;
    private static boolean isWindows;
    private static boolean isDarkTitleBar;
    private static boolean isGradientTitle;

    // This override trick only works on MacOSX Java 1.5
    private static final boolean OverrideMacAquaBrushedMetal = false;

    private static boolean SidewaysRollup = false; // experimental

    private static JFrame HiddenParentFrame;

    private WindowPropertiesPreference wpp = null;
    /**
     * Create a new DockWindow.  You should use GUI.createDockWindow for creating
     * instances of DockWindow for VUE.
     */
    public DockWindow(String title, Window owner, JComponent content, boolean asToolbar,boolean showCloseButton)
    {
        super(owner == null ? getHiddenFrame() : owner);
        /* Black ghosts on windows...
         * This is fixed in 1.6 as far as I can tell, but this has become an annoying
         * problem on 1.5 that multiple people on the team have complained about.  The
         * problem seems to be related to this issue:
         * http://mindprod.com/jgloss/contentpane.html
         * On a mac if you look closely you really do see the same problem as windows
         * (at least on my pokey mac) but the background color by default on the mac 
         * is light gray so its not as noticable.  
         */
        if (Util.isWindowsPlatform() && Util.getJavaVersion() < 1.6)
        	setBackground(Color.lightGray);
        
        if (CollapsedHeight == 0)
            staticInit();
        
        /*
        if (getParent() == HiddenParentFrame) {
            // We use this in java 1.5
            setFocusableWindowState(false);
            // TODO: Try enableInputMethods(false) to DISCONNECT from the focus management system
            // and take keys directly?
            // enableInputMethods(false); // can't see the change
        }
        */

        wpp = WindowPropertiesPreference.create(
        		"windows",
        		"window" + title.replace(" ", ""),
        		title, 
        		"Remember size and position of window",
        		false);
        
        showCloseBtn = showCloseButton;
        
        if (asToolbar) {
            
            // SMF: even tho you can't have any tool-tips without window
            // being enabled, combo box pop-ups dissapear way
            // to easily unless you do this (as soon as you mouse
            // into the toolbar, they dismiss)
            
            // MK: I'll have to check this against a mac but on windows
            //this behaves better without this set to false.  you can
            //actually bring the window forward in the z-order when 
            //you click on its borderbar, maybe this was a problem in
            //an old java verison since the dockwinow hadn't been used 
            //in toolbar mode in a while? -MK

            // SMF 2008-04-21: The ongoing saga.  See interceptMousePress
            // for more on this.  All toolbars are henceforth not
            // focusable, except for Linux, where I haven't tested this.
            
            if (!Util.isUnixPlatform()) {
                
                // SMF: seems better on mac -- re-enabled for mac 2007-10-29 In
                // particular, very narrow drop-downs are problematic w/out doing this:
                // as soon as you roll off them, they dissapear.  E.g., makes changing
                // the font via the font-size drop-down very problematic.  Worth not
                // having rollovers for this.
                
                setFocusableWindowState(false); 
            }

        }
        mBaseTitle = title;
        setTitle(title);
        
        isToolbar = asToolbar;

        mContentPane = new ContentPane(title, asToolbar);

        if (true)
            setContentPane(mContentPane);
        else
            setContentPane(new Box(BoxLayout.Y_AXIS));
                               
        setResizeEnabled(!isToolbar);

        if (DEBUG.INIT || DEBUG.DOCK) out("constructed (child of " + GUI.name(owner) + ")");

        if (!isToolbar) {
            // set a default size
   
        		setSize(DefaultWidth,150);
            //setMinimumSize(new Dimension(180,100)); // java 1.5 only
            //setPreferredSize(new Dimension(300,150)); // interferes with height
        }
       
        if (content != null) {
            setContent(content);
        } else
            ;//pack(); // ensure peer's created
        
        // add us to the static list of all DockWindow's
        synchronized (AllWindows) {
            AllWindows.add(this);
        }

        /* WAIT-CURSOR DEBUG
           setMenuActions(new Action[] {
           new AbstractAction("Show Wait Cursor") {public void actionPerformed(ActionEvent ae) {
           GUI.activateWaitCursor();
           }},
           new AbstractAction("Clear Wait Cursor") {public void actionPerformed(ActionEvent ae) {
           GUI.clearWaitCursor();
           }},
           });} */
        setFocusable(true);
    }
   
    public void scrollToTop()
    {
    	JScrollPane jsp = this.mContentPane.getScroller();
    	if ( jsp != null)
    	{
    		jsp.getVerticalScrollBar().setValue(0);
    		jsp.getVerticalScrollBar().setValueIsAdjusting(false);    		    		
    	}    	
    }
    public DockWindow(String title, Window owner, JComponent content, boolean asToolbar)
    {
    	this(title,owner,content,false,true);
    }
    public DockWindow(String title, Window owner) {
        this(title, owner, null, false,true);
    }
    
    public DockWindow(String title) {
        this(title, null, null, false,true);
    }
    
    public DockWindow(String title, JComponent content) {
        this(title, null, content, false,true);
    }

    public void setContent(JComponent c) {
        if (DEBUG.DOCK || DEBUG.WIDGET || DEBUG.INIT) out("adding " + GUI.name(c) + " to " + GUI.name(getContentPanel()));

        boolean hadContent = getContent() != null;

        /*if (c.getBorder() == null) {
            // enforce some kind of border so that a mouse click on at least the bottom
            // pixel in the window get's to us, so if it's at the bottom of the screen,
            // we can detect it for un-rolling.
            if (DEBUG.BOXES)
                getContentPanel().setBorder(new MatteBorder(0,0,1,0, Color.green));
            else
                getContentPanel().setBorder(new EmptyBorder(0,0,1,0));
        } else {
            if (DEBUG.BOXES)
                getContentPanel().setBorder(new MatteBorder(0,0,1,0, Color.green));
            else
                getContentPanel().setBorder(new EmptyBorder(0,0,1,0));
                }*/
        
        if (hadContent)
            getContent().removePropertyChangeListener(this);

        mMinContentSize = c.getMinimumSize();
        if (DEBUG.DOCK) GUI.dumpSizes(c, this + ":setContent");
        mContentPane.setWidget(c, Widget.wantsScroller(c),Widget.wantsScrollerAsNeeded(c));

        Component toListen = null;
        if (c instanceof JScrollPane)
            toListen = ((JScrollPane)c).getViewport().getView();
        if (toListen == null)
            toListen = c;

        toListen.addPropertyChangeListener(this);
        if (DEBUG.DOCK) out("addPropertyChangeListener: " + GUI.name(toListen));
        
        if (!hadContent || !isDisplayable()) {
            pack();
            if (isToolbar)
            	;//setSize(620,54);
            else
                setSize(DefaultWidth, getHeight());
        } else {
            validate();
        }

        
        
        //int width = minUnrolledWidth(getWidth());
        //if (width < 300) width = 300;

        /*
        boolean neverDisplayed = false;
        int minHeight = getHeight();
        
        if (!isDisplayable()) { // has never been displayed (or pack() called)
            neverDisplayed = true;
            int minWidth = 300;
            Dimension ps = c.getPreferredSize();
            Dimension ms = c.getMinimumSize();
            if (DEBUG.DOCK) out("content  minSize " + ms);
            if (DEBUG.DOCK) out("content prefSize " + ps);
            //int minHeight = ps.height > ms.height ? ps.height : ms.height;
            minHeight = ms.height;
            setSize(minWidth, minHeight);
        }
        
        pack();

        if (neverDisplayed)
            setSize(300, minHeight);
        */
        
    }

    /** this is overriden from Container just in case it is accidentally called.  An Error is thrown if this is called. */
    public Component add(Component c) {
        throw new Error("can't add component's directly to the DockWindow");
    }

    private JPanel getContentPanel() {
        return mContentPane.mContent;
    }

    /** Set the size of DockWindow such that content will ultimately have the given size.
     * (Set the size ot the DockWindow to this size plus the size of our title bar and borders).
     */
    public void setContentSize(int width, int height) {
        Dimension winBorder = getBorderSize();
        
        setSize(width + winBorder.width, height + CollapsedHeight);
    }

    public JComponent getContent() {
        if (mContentPane.mContent.getComponentCount() > 0)
            return (JComponent) mContentPane.mContent.getComponent(0);
        else
            return null;
    }
    
    /** interface java.beans.PropertyChangeListener for contained component */
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        
        final String key = e.getPropertyName();

        if (DEBUG.DOCK /*&& !key.equals("ancestor")*/) {
            out("Widget property change key(" + key + ") value=[" + e.getNewValue() + "]");
            //GUI.messageAfterAWT("after awt for property change " + key);
        }

        if (key == Widget.EXPANSION_KEY) {

            if (!mWasVisible) {
                boolean expand = ((Boolean) e.getNewValue()).booleanValue();
                setRolledUp(!expand, isDisplayable(), true);
            }
            
        } else if (key == Widget.HIDDEN_KEY) {

            if (!mWasVisible) {
                boolean hide = ((Boolean) e.getNewValue()).booleanValue();
                if (hide)
                    dismiss();
                else
                    setRolledUp(false, isVisible(), true);
            }
            
        } else if (key == Widget.MENU_ACTIONS_KEY) {
            setMenuActions((Action[]) e.getNewValue());
                
        } else if (key.equals("TITLE-INFO")) {
            
            String auxTitle = auxTitle = (String) e.getNewValue();
            setAuxTitle(auxTitle);
            
        }
    }

    private void setAuxTitle(String suffix) {
        String newTitle;
        if (suffix == null) {
            newTitle = mBaseTitle;
        } else {
            newTitle = mBaseTitle + " (" + suffix + ")";
        }
        setTitle(newTitle);
        //if (DEBUG.Enabled) System.out.println("setAuxTitle(" + newTitle + ")");
    }


    /** All DockWindow's in this DockWindow's stack show and hide with it */
    public void setStackOwner(boolean t) {
        isStackOwner = true;
    }

    public void setMenuActions(Action[] actions) {
        //mMenuActions = actions;
        mContentPane.mTitle.setMenuActions(actions);
    }
        
    public void setMenuActions(java.util.List actions) {
        setMenuActions( (Action[]) actions.toArray(new Action[actions.size()]));
    }
        
    private static void staticInit() {
        //-------------------------------------------------------
        // INIT STATIC'S
        //-------------------------------------------------------
        
        isMac = VueUtil.isMacPlatform();
        isMacAqua = GUI.isMacAqua();
        isMacAquaMetal = GUI.isMacBrushedMetal();
        isWindows = VueUtil.isWindowsPlatform();

        isDarkTitleBar = isMacAquaMetal;
        //isGradientTitle = isMac;
        isGradientTitle = true;

        if (OverrideMacAquaBrushedMetal && Util.getJavaVersion() >= 1.5f)
            isMacAquaMetal = false;
        
        if (isMacAquaMetal) {
            SidewaysRollup = false; // can't work in 1.4 brushed metal due to mac java bug
            CollapsedHeight = MacAquaMetalMinHeight;
        } else {
            CollapsedHeight = TitleHeight;
        }

        CollapsedHeightVisible = TitleHeight;

        WindowBorder = makeWindowBorder();
        
        if (WindowBorder != null) {
            Insets bi = WindowBorder.getBorderInsets(null);
            CollapsedHeightVisible += bi.top + bi.bottom;
            if (isMacAqua) // overlap by one pixel
                CollapsedHeightVisible -= 1;
            else // overlap by 2 pixels
                ; // not till we can fix up border overlap flashing; CollapsedHeightVisible -= 2;
            if (!isMacAquaMetal)
                CollapsedHeight += bi.top + bi.bottom;
        }

        // Simulate Windows;
        // isWindows=true; isMac=isMacAqua=isMacAquaMetal=false;
        // Simulate Linux;
        // isWindows=false; isMac=isMacAqua=isMacAquaMetal=false;

        /*
        if (isMac)  {
            TitleFont = VueAquaLookAndFeel.SmallSystemFont12;
        } else {
            Object desktopFrameFont = Toolkit.getDefaultToolkit().getDesktopProperty("win.frame.captionFont");
            if (desktopFrameFont instanceof Font) {
                Font font = (Font) desktopFrameFont;
                //System.out.println("GOT TITLE FONT " + font);
                TitleFont = font.deriveFont(10.0f);
                //System.out.println("GOT TITLE FONT " + TitleFont);
            } else {
                TitleFont = new Font("SansSerf", Font.BOLD, 10);
            }
        }
        */

        //sBottomGradientColor = isDarkTitleBar ? new Color(112,112,112) : Color.lightGray;

        //int midColor = (sBottomGradientColor.getRed() + sTopGradientColor.getRed()) / 2;
        //sMidGradientColor = new Color(midColor, midColor, midColor);

        refreshScreenInfo(null);

        //TopDock = new DockRegion(GUI.GInsets.top, DockRegion.TOP, "TopScreen");
        //BottomDock = new DockRegion(GUI.GScreenHeight - GUI.GInsets.bottom, DockRegion.BOTTOM, "BotScreen");

        // Be sure to create MainDock last, so the other docks
        // with fixed locations will take priority for membership.
        //MainDock = new DockRegion(-99, DockRegion.BOTTOM, "AppWinTop"); // top of app window
    }

    public static boolean isTopDockEmpty() {
        return TopDock == null ? true : TopDock.isEmpty();
    }
    
    public static DockRegion getTopDock() {
        if (CollapsedHeight == 0)
            staticInit();
        return TopDock;
    }
    public static DockRegion getMainDock() {
        if (CollapsedHeight == 0)
            staticInit();
        return MainDock;
    }

    public static int getCollapsedHeight() {
        if (CollapsedHeight == 0)
            staticInit();
        return CollapsedHeight;
    }

    public static int getMaxContentHeight() {
        return GUI.getMaxWindowHeight() - (getCollapsedHeight() + GUI.GInsets.bottom + 50);
    }

    /** @return a border, if any, for the entire DockWindow (null if none) */
    private Border getWindowBorder() {
        if (isToolbar && isMacAqua)
            return null;
        else
            return WindowBorder;
    }


    /** @return a border, if any, for the entire DockWindow (null if none) */
    private Border getContentBorder(JComponent c) {
        if (isToolbar)
            return null;

        if (ContentBorder == null) {
            if (DEBUG.BOXES)
                return ContentBorder = new LineBorder(Color.orange, 4);
            else
                return null;
            // ContentBorder = new CompoundBorder(new MatteBorder(3,2,3,2, new Color(235,235,235)),
            //new LineBorder(new Color(102,102,102)));
            //ContentBorderInset = new CompoundBorder(ContentBorder, GUI.WidgetInsetBorder);

        }

        return ContentBorder;

        /*
        if (c instanceof WidgetStack || firstChild(c) instanceof WidgetStack || c instanceof JScrollPane)
            return ContentBorder;
        else
            return ContentBorderInset;
        */
    }

    private Dimension mBorderSize;
    private Dimension getBorderSize() {
        if (mBorderSize == null) {
            mBorderSize = new Dimension();
            if (getWindowBorder() != null) {
                Insets wb = getWindowBorder().getBorderInsets(null);
                mBorderSize.width += wb.left + wb.right;
                mBorderSize.height += wb.top + wb.bottom;
            }
            if (getContentBorder(null) != null) {
                Insets cb = getContentBorder(null).getBorderInsets(null);
                mBorderSize.width += cb.left + cb.right;
                mBorderSize.height += cb.top + cb.bottom;
            }
        }
        return mBorderSize;
    }
    

    /** @return first child of component, if it is a Container and has children, otherwise null */
    private static Component firstChild(Component component) {
        if (component instanceof Container) {
            Container c = (Container) component;
            if (c.getComponentCount() > 0)
                return c.getComponent(0);
        }
        return null;
            
    }
    
    private static Border makeWindowBorder() {

        if (isMacAqua && (MacWindowShadowEnabled || isMacAquaMetal)) {
            return null; // no border on MacOSX at all for now: rely on native shadowing
        } else {
            if (DEBUG.BOXES) {
                return new LineBorder(Color.green);
            } else {
                return new CompoundBorder(new MatteBorder(0,1,1,1, new Color(204,204,204)),
                                          new LineBorder(new Color(137,137,137)));
                //return new CompoundBorder(new LineBorder(new Color(204,204,204)),
                //                          new LineBorder(new Color(137,137,137)));
                //return new LineBorder(new Color(51,51,51));
            }
            
            /*
            if (isMacAqua) {
                if (DEBUG.BOXES)
                    return new LineBorder(Color.green);
                else
                    return new LineBorder(sBottomEdgeColor);
                //return new BevelBorder(BevelBorder.RAISED, Color.lightGray, Color.gray);
            } else {

                // For Windows:

                Color base = GUI.getVueColor();
                
                return new BevelBorder(BevelBorder.RAISED,
                                       base,
                                       Util.brighterColor(base),
                                       //base.brighter(),
                                       base.darker().darker(),
                                       base.darker());
                
                //return BorderFactory.createRaisedBevelBorder();
            }
            */
        }
    }
    

    public void setResizeEnabled(boolean canResize) {
        isResizeEnabled = canResize;
        if (canResize) {
            if (mResizeCorner == null) {
                mResizeCorner = new ResizeCorner(this, SOUTH_EAST);
                mResizeCorner2 = new ResizeCorner(this, SOUTH_WEST);
                getLayeredPane().add(mResizeCorner, JLayeredPane.PALETTE_LAYER);
                getLayeredPane().add(mResizeCorner2, JLayeredPane.PALETTE_LAYER);
                // todo: need to handle window reshape (it moves) v.s. just resize for this
            }
        } else {
            if (mResizeCorner != null) {
                getLayeredPane().remove(mResizeCorner);
                getLayeredPane().remove(mResizeCorner2);
                mResizeCorner = null;
                mResizeCorner2 = null;
            }
        }
    }


    private static void dumpGC(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        sun.java2d.SurfaceData sd = null;
        if (g instanceof sun.java2d.SunGraphics2D)
            sd = ((sun.java2d.SunGraphics2D)g).surfaceData;
        
        System.out.println("\t" +
                           Util.objectTag(g) + " surface=" + sd
                           + "\n\t      clip " + g.getClip()
                           + "\n\tclipBounds " + g.getClipBounds()
                           //+ "\n\t     hints " + g2.getRenderingHints()
                           //+ "\n\tbackground " + g2.getBackground()
                           );
    }


    public void Xupdate(Graphics g) {
        //g.drawString("Hello", 10, 20);
        //if (DEBUG.DOCK || DEBUG.PAINT) out("update");
        Util.printClassTrace("!java.awt.EventDispatchThread", "update");
        dumpGC(g);
        super.update(g);
    }


    public void paint(Graphics g) {
        //Util.printClassTrace("!java.awt.EventDispatchThread", "paint");
        
        if (DEBUG.PAINT) out("paint");

        // For subclassing Window impl:
        // if (!isRolledUp()) paintResizeCorner((Graphics2D)g);

        super.paint(g);
    }

    public void paintAll(Graphics g) {
        Util.printStackTrace("paintAll");
        super.paintAll(g);
    }


    public void Xreshape(int x, int y, int w, int h) {
        if (DEBUG.DOCK) out("reshape");

        //super.reshape(x, y, w, h);

        if (DEBUG.DOCK) out("reshape returns");

        // Mac frames that are DECORATED refresh beautifully, but unless the reshape
        // request is coming from MacOSX, we get flashing for any other reshape (from
        // java) calls.

        //-----------------------------------------------------------------------------
        //
        // The problem is that the call to peer.setBounds in reshape is ultimately
        // causing a clearRect on the the entire graphics context, no matter what we do,
        // even if we manually turn off peerFlushing (either it doesn't handle that
        // case, or it's getting turned back on).  There is special code in the apple
        // peers that knows how handle the COMPONENT_RESIZED (maybe that event is
        // special) when it comes from native CFrame drags, that eliminates flashing,
        // but for whatever reason they're just not doing for this case.
        //
        //-----------------------------------------------------------------------------
    }

    public void validate() {

        // Util.printClassTrace("!java.awt.EventDispatchThread", "validate");

        if (mAnimatingReshape) {
            // this no help in preventing occasional mac double-redraw's on show
            if (DEBUG.DOCK && DEBUG.META) out("validate: skipping");
        } else {
            if (DEBUG.DOCK && DEBUG.META) out("validate");
            //Util.printStackTrace("validate " + this);
            super.validate();
            
            if (mResizeCorner != null) {
                int width = getWidth();
                int height = getHeight();

                // south east
                mResizeCorner.setLocation(width - ResizeCornerSize,
                                          height - ResizeCornerSize);

                // south west
                mResizeCorner2.setLocation(0, height - ResizeCornerSize/2);

            }
        }
    }
    
    public void invalidate() {

        //Util.printClassTrace("!java.awt.EventDispatchThread", "invalidate");
        
        if (mAnimatingReshape) {
            if (DEBUG.DOCK && DEBUG.META) out("invalidate: skipping");
            
            //Util.printStackTrace("invalidate " + this);
            
            // This doesn't help unless we make sure we don't clear mAnimatingReshape
            // until the AWT EventQueue is cleared after we're done animating, as a
            // COMPONENT_RESIZED event for each resize during animation is being posted
            // to the EventQueue, which then all come, one after the other, after we're
            // done animating, at least on the mac (i suspect it's smarter on PC?)  When
            // they get to Window.dispatchEventImpl, it issues a paired call
            // validate/invalidate for each event.

            // So Component.reshape is calling invalidate on resize's, and then posting
            // a COMPONENT_RESIZED event each time also.

            // Altho all the extra invalidate/validate's don't appear to be causing
            // multiple repaints in this case, we're skipping these as they're probably
            // slowing things down.  Actually, at the moment on my PowerBook 1.5Ghz, I
            // can detect no difference at all...
            
        } else {
            if (DEBUG.DOCK && DEBUG.META) out("invalidate");
            super.invalidate();
        }
    }

    public void addNotify()
    {
        if (OverrideMacAquaBrushedMetal && GUI.isMacBrushedMetal()) {

            // This trick only works on MacOSX Java 1.5:
            
            // If we have this special name when the peer is created, the Window will
            // NOT be MacOSX brushed metal, even if that's what we're running under.
            // This is the name java uses for popup windows such as menus and tool
            // tips, which don't appear as brushed metal under 1.5 (they do under 1.4).
            // This is also a way to get around the minimum size bug of Windows when
            // using Mac Aqua Brushed Metal.
            
            setName(GUI.OVERRIDE_REDIRECT);

            // Note that we can re-set the name for debugging purposes after
            // the peer has been created (super.addNotify())
        }

        super.addNotify();

        updateWindowShadow();
        
        addMouseListener(this);
        addMouseMotionListener(this);
        
        // make sure peer has title for a native MacOSX code, and re-set name if it was ###override
        setTitle(mTitleName);

        if (isToolbar) {
            // enforced a fixed height on toolbars
            mContentPane.setPreferredSize(new Dimension(getPreferredSize().width,
                                                        ToolbarHeight));
        }

    }

    private DockWindow getFirstVisibleParent() {
        if (isVisible())
            return this;
        else if (mParentWhenHidden != null)
            return mParentWhenHidden.getFirstVisibleParent();
        else
            return null;
    }
    
    private boolean _firstDisplay = true;
    private void superSetVisible(final boolean show) {
        if (DEBUG.DOCK) out("superSetVisible " + show);
        mShowing = show;

        if (_firstDisplay) {
            _firstDisplay = false;
            keepOnScreen();
        }
        
        super.setVisible(show);

        /*
        if (isMacAqua && show && !isStacked())
            setWindowShadow(true);
        
        super.setVisible(show);
        
        if (isMacAqua && !show) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                setWindowShadow(false);
            }});
        }
        */
    }

    public static void ToggleAllVisible() {
        if (!AllVisible)
            ShowPreviouslyHiddenWindows();
        else
            HideAllWindows();
    }

    public static boolean AllWindowsHidden() {
        return !AllVisible;
    }

    public synchronized static void HideAllWindows() {
        if (DEBUG.Enabled) Log.debug("hide all");
        AllVisible = false;
        for (DockWindow dw : AllWindows) {
            dw.mWasVisible = dw.isVisible();
            if (dw.mWasVisible)
                dw.superSetVisible(false);
        }
        // TODO: when prophylactically hiding these for full-screen mode,
        // this is overkill / could even be problematic.
        ensureViewerHasFocus(); 
    }

    public void toFront() {
        //tufts.Util.printClassTrace(DockWindow.class, "RAISING");
        if (isVisible()) {
            if (DEBUG.DOCK||DEBUG.WORK) out("toFront");
            super.toFront();
        } else {
            // Window.toFront does nothing if not visible anyway
            //if (DEBUG.DOCK) out("(toFront)");
        }
    }

    public synchronized static void ShowPreviouslyHiddenWindows() {
        if (DEBUG.Enabled) Log.debug("ShowPreviouslyHiddenWindows");
        if (VUE.inNativeFullScreen()) {
            Log.debug("Ignoring show all windows: in native full screen");
            // don't touch windows if in native full screen, as can
            // completely hang us on Mac OS X
            return;
        }

        if (Util.isMacLeopard()) {

            // Okay, this is seriously messed.  If we have only ONE DockWindow visible,
            // we can hide/show all ONCE, and it stays on top, but the SECOND time
            // we hide/show (or enter/exit full screen mode), it starts going behind!
            // Showing a second DockWindow then hiding/showing all once or twice
            // seems to put things back in order.  Delaying the toFront to be
            // invoked later on the AWT thread appears to offer no help, nor does
            // just raising them all at the end.
            
            for (DockWindow dw : AllWindows) {
                if (dw.mWasVisible) {
                    dw.superSetVisible(true);
                    dw.toFront();

                    //dw.setAlwaysOnTop(true);
//                     final DockWindow d = dw;
//                     GUI.invokeAfterAWT(new Runnable() { public void run() {
//                         d.toFront(); // 2008-04-21 required for Mac OSX Leopard to keep them on top
//                     }});
                }
                
                dw.mWasVisible = false;
            }

//             // hope this helps the random Leopard cases that still break...
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                 DockWindow.raiseAll();
//             }});
            
            
        } else {
            
            for (DockWindow dw : AllWindows) {
                if (dw.mWasVisible)
                    dw.superSetVisible(true); // non-Leopard platforms automatically toFront on this
                dw.mWasVisible = false;
            }
        }

        ensureViewerHasFocus();
        AllVisible = true;
    }
    
    private static void ensureViewerHasFocus() {
        
        // The give the focus back to the viewer, which can lose it it
        // when they go visible or invisible.  E.g., on WinXP, even
        // the hidden FullScreen.FSWindow is somes getting focus,
        // which it never should, given that it's focusable state is
        // false.
        
        if (!Util.isMacLeopard()) // still having problems: too risky
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            final tufts.vue.MapViewer viewer = VUE.getActiveViewer();
            if (viewer != null) {
                if (DEBUG.DOCK) Log.debug("focusRequest to return focus to " + viewer);
                viewer.requestFocus();
                //VUE.getActiveViewer().grabVueApplicationFocus(DockWindow.class.getName(), null);
            }}});
    }



    public synchronized static void ImmediatelyRepaintAllWindows() {
        if (DEBUG.Enabled) Log.debug("ImmediatelyRepaintAllWindows");
        for (DockWindow dw : AllWindows) {
            if (dw.isVisible()) {
                Rectangle bounds = dw.getContentPanel().getBounds();
                // adjust bounds to force repainting of the entire window contents
                bounds.x = -50;
                bounds.y = -50;
                bounds.width += 100;
                bounds.height += 100;
                if (DEBUG.PAINT) Log.debug("repaint " + dw + "; " + bounds);
                dw.getContentPanel().paintImmediately(bounds);
            }
        }
    }
    
    
    /** keep the bottom of the window from going below the bottom screen edge */
    private void keepOnScreen() {
        Rectangle r = getBounds();
        if (keepOnScreen(r))
            setSize(r.width, r.height);
    }
            
    /** @return true of bounds were modified */
    private boolean keepOnScreen(Rectangle r) {
        int bottom = r.y + r.height;
        int maxBottom = GUI.GScreenHeight - GUI.GInsets.bottom;
        //out("        y="+r.y);
        //out("   bottom="+bottom);
        //out("maxBottom="+maxBottom);
        if (bottom > maxBottom) {
            r.height = Math.max(TitleHeight + 1, maxBottom - r.y);
            return true;
        } else
            return false;

        /*
          Rectangle max = GUI.GMaxWindowBounds;
          if (DEBUG.Enabled) out("maxwinbounds: " + max);
          if (mSavedShape.width > max.width)
          mSavedShape.width = max.width;
          if (mSavedShape.height > max.height)
          mSavedShape.height = max.height;
        */
    }

    /** for use during application startup */
    public void showRolledUp() {
        if (DEBUG.DOCK || DEBUG.INIT) out("showRolledUp");
        setRolledUp(true, false);
        //updateWindowShadow();
        superSetVisible(true);
    }

    /** normally can only be in a DockRegion if visible: this allows pre-assignment during startup */
    public void setDockTemporary(DockRegion region) {
        if (DEBUG.DOCK) out("setDockTemporary: " + region);
        mDockRegion = region;
        updateWindowShadow();
    }
    
    public static void assignAllDockRegions() {
        DockRegion.assignAllMembers();
    }
    
    public void setStackVisible(boolean show) {

        // If showing, show us first, then children.
        // If hiding, do reverse.
        
        if (show) {
            // Invoking later helps ensure DockWindow's that 
            // are set visible last are on the top of the z-order,
            // which is important for MacOSX window shadow.
            // Unfrotunately, this is not full-proof, but
            // adding a call to toFront seems to have fixed this?
            if (isMacAqua && MacWindowShadowEnabled) {
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    superSetVisible(true);
                    toFront();
                }});
            } else {
                superSetVisible(true);
            }
        }
        if (mChild != null)
            mChild.setStackVisible(show);
        if (!show) {
            superSetVisible(false);
        }
    }
    
    @Override
    public void setVisible(boolean show) {
        setVisible(show, true);
    }

    private void raiseChildrenLater() {
        // apparently, sometimes, we must raise the children later for toFront to work
        GUI.invokeAfterAWT(new Runnable() { public void run() { raiseChildren(); }});
    }
    
    protected void setVisible(boolean show, boolean autoUnrollOnShow)
    {
        mShowing = show;

        if (DEBUG.FOCUS || DEBUG.DOCK) {
            out("setVisible " + show);
//             if (!show && !AllVisible && mWasVisible) {
//                 out("ignoring visibility request: all are hidden");
//                 return;
//             }
        }

        if (isStackOwner && mChild != null) {
            setStackVisible(mShowing);
            //if (isMac && mShowing) raiseChildrenLater();
            return;
        }

        if (show) {
            if (autoUnrollOnShow && isRolledUp())
                setRolledUp(false);
            else if (false && mSavedShape != null)
                // need to show before we do this!  Will need to tweak us so that's okay to do.
                setShapeAnimated(getX(), getY(), mSavedShape.width, mSavedShape.height);
            
        } else if (false) {
            if (!isRolledUp())
                mSavedShape = getBounds();
            setShapeAnimated(getX(), getY(), getWidth(), 0);
        }
            
        if (isVisible() == mShowing)
            return;
        
        updateOnVisibilityChange();
        superSetVisible(show);

        if (show) {
            boolean windowStackChanged = false;
            if (mParentWhenHidden != null) {
                DockWindow visibleParent = mParentWhenHidden.getFirstVisibleParent();
                if (visibleParent != null) {
                    visibleParent.setChild(this);
                    windowStackChanged = true;
                }
            } else if (mChildWhenHidden != null &&
                       mChildWhenHidden.mParent == this &&
                       mChildWhenHidden.isVisible()) {
                setChild(mChildWhenHidden);
                windowStackChanged = true;
            }
            mChildWhenHidden = null;
            mParentWhenHidden = null;
            
            if (isMac && true || windowStackChanged) {
                raiseChildrenLater();
            }
        }

        DockRegion.assignAllMembers();
    }

    private void updateOnVisibilityChange() {
        if (DEBUG.DOCK) out("updateOnVisibilityChange; mShowing=" + mShowing + " visible=" + isVisible());

        if (mShowing == false) {
            mChildWhenHidden = mChild;
            mParentWhenHidden = mParent;
            if (mChild != null) {
                if (mParent != null)
                    mParent.setChild(mChild);
                else
                    removeChild();
            } else if (mParent != null) {
                mParent.removeChild();
            }
        } else {
            /*
            if (mParentWhenHidden != null && mParentWhenHidden.isVisible())
                mParentWhenHidden.setChild(this);
            else if (mChildWhenHidden != null && mChildWhenHidden.isVisible())
                setChild(mChildWhenHidden);
            mChildWhenHidden = null;
            mParentWhenHidden = null;
            */
        }
    }

    private void dismiss() {
        if (DEBUG.DOCK) out("DISMISS");
        //setShapeAnimated(getX(), getY(), getWidth(), 0);
        // oops: this never handled showing us again anyway :)
       
        setVisible(false);
    }
    
    public void saveWindowProperties()
    {   
     	Dimension size = null;
     	
     	if (isRolledUp)
     		size = new Dimension((int)mSavedShape.getWidth(),(int)mSavedShape.getHeight());
     	else
     		size = getSize();
     	Point p;
     	
     	if (isShowing())
     		p = getLocationOnScreen();
     	else
     		p = new Point(-1,-1);
     	
     	wpp.updateWindowProperties(isShowing(), (int)size.getWidth(), (int)size.getHeight(), (int)p.getX(), (int)p.getY(),isRolledUp);
    }

    public void positionWindowFromProperties()
    {
    	if (wpp.isEnabled() && (wpp.isWindowVisible() || wpp.isRolledUp())) {
            Point p = wpp.getWindowLocationOnScreen();
            Dimension size = wpp.getWindowSize();
            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            if (((int)p.getX()) > -1 && isPointFullyOnScreen(p,size,screenSize)) {
                
                if (isToolbar) // ignore size on toolbars: they always get their designed size
                    setLocation((int)p.getX(),(int)p.getY());
                else
                    setBounds((int)p.getX(),(int)p.getY(),(int)size.getWidth(),(int)size.getHeight());
    		
                if (wpp.isRolledUp())
                    {
                        mSavedShape = new Rectangle((int)p.getX(),(int)p.getY(),(int)size.getWidth(),(int)size.getHeight());
                        showRolledUp();
                    }
                else
                    setVisible(wpp.isWindowVisible());    		
            }    	
            else
                {
                    if (wpp.isWindowVisible())
                        {
                            //System.out.println("OTHER");
                            suggestLocation((int)p.getX(),(int)p.getY());

                            if (wpp.isRolledUp())
                                {        			
                                    mSavedShape = new Rectangle((int)p.getX(),(int)p.getY(),(int)size.getWidth(),(int)size.getHeight());
                                    showRolledUp();
                                }
                            else
                                setVisible(wpp.isWindowVisible());
                        }    	
    			
                }
        }    	
    }
    
    private boolean isPointFullyOnScreen(Point p, Dimension size, Dimension screenSize)
    {
    	int rightCorner =  (int)p.getX() + (int)size.getWidth();
    	int bottomCorner = (int)p.getY() + (int)size.getHeight();
    	
    	if ((rightCorner <= screenSize.getWidth()) && (bottomCorner <= screenSize.getHeight()))
    		return true;
    	else 
    		return false;
    }
    public WindowPropertiesPreference getWindowProperties()
    {
    	return wpp;
    }

    /** look for a tabbed pane within us with the given title, and select it */
    public void showTab(final String name) {
        
        new EventRaiser<JTabbedPane>(this, JTabbedPane.class) {
            public void dispatch(JTabbedPane tabbedPane) {
                int i = tabbedPane.indexOfTab(name);
                if (i >= 0) {
                    tabbedPane.setSelectedIndex(i);
                    EventRaiser.stop();
                }
            }
        }.raiseStartingAt(this);
        
        setVisible(true);
    }

    public void setTitle(String title) {

        if (mTitleName != null && mTitleName.equals(title))
            return;

        // TODO: don't update the root title if the aux title changes
        
        mTitleName = title;
        mTitleWidth = GUI.stringLength(TitleFont, title);
        mMinTitleWidth = mTitleWidth + 4;
        setName(title);

        GUI.setRootPaneNames(this, title);

        if (isMac && isDisplayable()) {
            // isDisplayable true if we have a peer, which we need before MacOSX lib calls
            try {
                MacOSX.setTitle(this, title);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        if (mContentPane != null && mContentPane.mTitle != null) {
            mContentPane.mTitle.setTitle(title);
            // hackish to add this to existing title width, put produces sane numbers
            mMinTitleWidth += mContentPane.mTitle.getMinimumSize().width;
        }
        repaint();
    }
    
    public String getTitle() {
        return mTitleName;
    }
    
    /** Return the name to use in a menu action to refer to this window: defaults to title */
    public String getMenuName() {
        return mMenuName == null ? getTitle() : mMenuName;
    }
    
    /** Set a separate menu name: if null, will default to title */
    public void setMenuName(String s) {
        mMenuName = s;
    }
    
    private int minUnrolledHeight(int height) {
        int absoluteMin =
            TitleHeight
            + getBorderSize().height
            + mMinContentSize.height;

        if (absoluteMin < TitleHeight + ResizeCornerSize)
            absoluteMin = TitleHeight + ResizeCornerSize;
        
        if (height < absoluteMin)
            return absoluteMin;
        else
            return height;
    }

    private int minUnrolledWidth(int requestedWidth)
    {
        final DockWindow stackTop = getStackTop();
        
        int absoluteMin = getBorderSize().width + mMinContentSize.width;

        if (absoluteMin < mMinTitleWidth)
            absoluteMin = mMinTitleWidth;
        
        if (stackTop.isStackOwner && stackTop != this) {
            if (absoluteMin < stackTop.getWidth())
                absoluteMin = stackTop.getWidth();
        }
        
        if (requestedWidth < absoluteMin)
            return absoluteMin;
        else
            return requestedWidth;
    }
    
    public void setBounds(int x, int y, int width, int height)
    {
        if (DEBUG.DOCK) out("setBounds " + x+","+y + " " + width+"x"+height);

        if (DEBUG.Enabled && width == 0 && mTitleName != null) // mTitle only null during <init>
            Util.printStackTrace(this + " zero width setBounds " + x+","+y + " " + width+"x"+height);
        
        /*
          // no way to erase minimum brushed metal window size...
          
        apple.awt.CWindow peer = (apple.awt.CWindow) getPeer();
        if (peer != null) {
            invalidate();
            out(" awt    minSize: " +  getMinimumSize());
            out("peer getMinSize: " +  peer.getMinimumSize());
            out("peer    minSize: " +  peer.minimumSize());
            //peer.setBackground(Color.blue);
            //setMinimumSize(new Dimension(1,1));
            //peer.setBounds(x, y, width, height);
            peer.reshape(x, y, width, height);
            return;
        }
        */

        /*
        float mAspect = 3f / 4f;

        System.out.println("aspect=" + mAspect);
        if (mAspect > 0) {
            if (width <= 0) width = 1;
            if (height <= 0) height = 1;
            double newAspect = width / height;
            if (newAspect < mAspect)
                height = (int) (width / mAspect);
            else if (newAspect > mAspect)
                width = (int) (height * mAspect);
        }
        */
        

        int curHeight = getHeight();

        if (height != curHeight) {

            // If we're shrinking, move up our child before we shrink up,
            // otherwise, move down our child after we grow.  This keeps
            // whatever is under the window stack from peeking through as
            // the stack adjusts.

            if (height < curHeight) {
                updateAllChildLocations(height, getY());
                super.setBounds(x, y, width, height);
            } else {
                super.setBounds(x, y, width, height);
                updateAllChildLocations(height, getY());
            }
        
        } else
            super.setBounds(x, y, width, height);

        if (!isMacAqua) {
            // needed for Java Metal L&F                
            validate(); 
        }
        


    }

    private boolean wantsSidewaysRollup() {
        return SidewaysRollup
            && atScreenLeft()
            && !atScreenTop()
            && !atScreenBottom()
            && !isStacked();
    }
    

    private int minRolledHeight() {
        if (wantsSidewaysRollup())
            return getWidth();
        else
            return CollapsedHeight;
    }


    private int minRolledWidth()
    {
        if (mDockRegion != null)
            return mDockRegion.getRolledWidth(this);
        else
            return minUnrolledWidth(getWidth());
    }
        
    private int XminRolledWidth()
    {
        if (mDockRegion != null)
            return mDockRegion.getRolledWidth(this);
            
        if (isStacked() == false) {
            if (wantsSidewaysRollup()) {
                return CollapsedHeight;
                //} else if (mDockRegion != null) {
                //return mDockRegion.getRolledWidth(this);
            } else {
                // conserve width if at sides
                return getWidth() > 180 ? 180 : getWidth();
            }
        }
        
        // choose the widest of either parent or child's stacked {unrolled,rolled} size

        // TODO: need more work: if parent or child rolled-up, their width may even have
        // been stretched, in which case we'll want to shrink the rolled-up size to,
        // say, our width if we're narrow and there are no other wider windows.

        //final int parentWidth = mParent == null ? 0 : mParent.getStackedWidth();
        //final int childWidth = mChild == null ? 0 : mChild.getStackedWidth();
        
        final int parentWidth = mParent == null ? Integer.MAX_VALUE : mParent.getWidth();
        final int childWidth = mChild == null ? Integer.MAX_VALUE : mChild.getWidth();

        // smallest of the two:
        return minUnrolledWidth(parentWidth < childWidth ? parentWidth : childWidth);
    }

    private int XgetRolledWidth() {
        if (mParent == null && mChild == null)
            return 180;
        
        // choose the widest of either parent or child

        final int parentWidth = mParent == null ? 0 : mParent.getWidth();
        final int childWidth = mChild == null ? 0 : mChild.getWidth();
        
        return parentWidth > childWidth ? parentWidth : childWidth;
    }
    
    

    /** return width in stack: the smaller of current rolled width or unrolled width */
    private int getStackedWidth() {
        final int curWidth = getWidth();
        if (mSavedShape != null)
            return mSavedShape.width < curWidth ? mSavedShape.width : curWidth;
        else
            return curWidth;
    }

    /*
    private void updateRolledUpSize(DockWindow notifier)
    {
        if (isRolledUp()) {
            setSizeAnimated(getRolledWidth(), getHeight());
        }
    }
    */

    public boolean atScreenTop() {
        //if (DEBUG.Enabled) out("atScreenTop: y=" + getY() + " <= " + GUI.GInsets.top);
        return getY() <= GUI.GInsets.top;
    }
    
    public boolean atScreenLeft() {
        return getX() == 0;
        //return getX() <= GUI.GInsets.left;
    }
    
    public boolean atScreenRight() {
        return (getX() + getWidth()) == GUI.GScreenWidth;
        /*
        // will need to align to the PARENT, not the screen, if to support this.
        
        if (GUI.GScreenWidth == 0)
            return false;
        else
            return getX() > GUI.GScreenWidth / 2;
        */
    }
    
    public boolean atScreenBottom() {
        int bottomEdge = getY() + getHeight();

        // don't allow this for anything with a top in the upper half of the screen
        if (getY() < GUI.GScreenHeight / 2)
            return false;

        if (GUI.GInsets.bottom > 0) {

            return bottomEdge <= GUI.GScreenHeight
                && bottomEdge >= GUI.GScreenHeight - GUI.GInsets.bottom;
                
        } else {
            return bottomEdge == GUI.GScreenHeight;
        }
    }


    /**
     * Set this DockWindow to it's "rolled-up" state: just the title
     * bar is showing.  If Component.isDisplayable() is true (it
     * has a peer / has been shown on screen at least once), then
     * the transition will be animated.
     */
    public void setRolledUp(boolean rollup) {
        setRolledUp(rollup, isDisplayable());
    }
    
    public void setRolledUp(boolean makeRolledUp, boolean animate) {
        setRolledUp(makeRolledUp, animate, false);
    }
    
    private void setRolledUp(boolean makeRolledUp, boolean animate, boolean propertyChangeEvent) {
        if (DEBUG.DOCK) out("setRolledUp " + makeRolledUp + " animate=" + animate + " propertyChangeEvent="+propertyChangeEvent);
        if (isRolledUp == makeRolledUp || isToolbar)
            return;

        /*
         * i can't find a case where removing this breaks something.  It would seem
         * that maybe it wasn't considering that the Content could be a widgetstack, and
         * so this bug was missed.  just calling widget.setExpanded here is not going to
         * collapse the dockwindow when the content is a widgetstack -MK
         */
        /*if (!propertyChangeEvent && Widget.isWidget(getContent())) {
            // ensure the Widget property value is set.
            Widget.setExpanded(getContent(), !makeRolledUp);
            return;
        }
		*/
        // need to mark us as rolled up now for forthcoming
        // size computations to work.
        isRolledUp = makeRolledUp;

        // updateWindowShadow();
        
        if (makeRolledUp) {

            // make us rolled up

            mSavedShape = getBounds();
            //out("shape " + mSavedShape);

            // Okay, two cases: one where we're getting our "future" rolled up width
            // now, adjusting for future adjustments in parent & children, who may only
            // have been rolled up wider becuase we are wide.

            // The other is "immediate": my parent or child just changed sized, so now
            // what do I do?  Of course, in this case, we do have a cascading
            // dependency...  my parent may have been wider because of me, but now it
            // *still* wants to be wider rolled becuase if *it's* parent, tho maybe not
            // as wide as it is now, and *WE* need to know that information...

            // So probably should just iterate through the whole damn stack each time
            // (instead of this recursive confusion), starting from the widest unrolled
            // size, and updating everyone that way -- which would allow us to also do a
            // paralell call on all the setSizes.

            int rolledWidth = minRolledWidth();
            int rolledHeight = minRolledHeight();
            int rolledX = getX();
            int rolledY = getY();
            
            if (atScreenBottom() && mParent == null  && !atScreenTop()// don't roll-down in stacks for now
                //|| (isDocked() && mDockRegion.mGravity == DockRegion.BOTTOM)
                ) {
                rolledY = getY() + getHeight() - rolledHeight;
            } else if (atScreenRight()) {
                mStickingRight = true;
                rolledX = GUI.GScreenWidth - rolledWidth;
            }
            
            if (animate)
                setShapeAnimated(rolledX, rolledY, rolledWidth, rolledHeight);
            else
                setBounds(rolledX, rolledY, rolledWidth, rolledHeight);
            
            getContentPanel().setVisible(false);

            if (wantsSidewaysRollup())
                mContentPane.setVerticalTitle(true);

            // Pretend like we've been hidden so that VueMenuBar.WindowDisplayAction
            // will treat another display request as an un-roll request in-line subclass
            // so is marked for tracking.  I think this also is making sure we give up
            // the kbd focus if one of our children had it.

            if (animate) // only during normal use: not during init
                GUI.postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN) {} );
                                                                        
            
        } else {

            if (SidewaysRollup) // ensure vertical cleared
                mContentPane.setVerticalTitle(false);
            
            mStickingRight = false;

            Rectangle newShape = new Rectangle(mSavedShape);
            
            if (atScreenBottom() && mParent == null
                //|| (isDocked() && mDockRegion.mGravity == DockRegion.BOTTOM)
                ) {

                newShape.x = getX();
                newShape.y = getY() + getHeight() - mSavedShape.height;
                
            } else if (atScreenRight()) {

                newShape.x = GUI.GScreenWidth - mSavedShape.width;
                newShape.y = getY();
                mStickingRight = true;

            } else {

                newShape.x = getX();
                newShape.y = getY();
            }
            
            keepOnScreen(newShape);

            setShapeAnimated(newShape.x, newShape.y, newShape.width, newShape.height);
                
            getContentPanel().setVisible(true);
            mSavedShape = null;

            // pretend like we've been shown so that VueMenuBar.WindowDisplayAction
            // will know we're visible again.
            GUI.postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_SHOWN));
            
        }

        mContentPane.mTitle.showAsOpen(!isRolledUp);

        if (mResizeCorner != null) {
            mResizeCorner.setVisible(!isRolledUp);
            mResizeCorner2.setVisible(!isRolledUp);
        }
            
        updateWindowShadow();

        if (mDockPrev != null)
            mDockPrev.repaintTitle();
        if (mDockNext != null)
            mDockNext.repaintTitle();
        

        /*
        if (mParent != null)
            mParent.updateRolledUpSize(this);
        if (mChild != null)
            mChild.updateRolledUpSize(this);
        */

        /*
        
        if (mParent != null && mParent.isRolledUp())
            mParent.setSizeAnimated(getWidth(), mParent.getHeight());
        if (mChild != null && mChild.isRolledUp())
            mChild.setSizeAnimated(getWidth(), mChild.getHeight());

        */
    }


    static class Interpolator {
        final float increment;
        private float current;
        public Interpolator(int steps, int start, int end, String name) {
            float delta = end - start;
            increment = delta / steps;
            this.current = start;
            if (false)
                System.out.println("Interpolate " + name + " from " + start + " to " + end
                                   + " in " + steps + " steps: "
                                   + "range=" + delta + " inc=" + increment);
            
        }
        public Interpolator(int steps, int start, int end) {
            this(steps, start, end, "");
        }
        public int next() {
            current += increment;
            return (int) current;
        }
    }

    public void setWidth(int width) {
        setSize(width, getHeight());
    }
    public void setHeight(int height) {
        setSize(getWidth(), height);
    }

    private void XsetSizeAnimated(int width, int height) {
        super.setSize(width, height);
    }

    private void setShapeAnimated(int x, int y, int width, int height) {

        if (DEBUG.DOCK) out("setShapeAnimated");

        // christ: JVM 1.5, on an old WIN2K box, animates a resize easily
        // 10 times faster than the mac
        final int steps = isWindows ? 16 : 4;
        
        final boolean moved = (x != getX() || y != getY());
        final boolean resized = (width != getWidth() || height != getHeight());
        
        Interpolator ix, iy, iw, ih;

        ix = iy = iw = ih = null;
        
        if (moved) {
            ix = new Interpolator(steps, getX(), x, "x");
            iy = new Interpolator(steps, getY(), y, "y");
        }
        if (resized) {
            iw = new Interpolator(steps, getWidth(), width, "width");
            ih = new Interpolator(steps, getHeight(), height, "height");
        }

        mAnimatingReshape = true;
        //setIgnoreRepaint(true); // works on W2K jvm 1.5
        try {
            if (moved && resized) {
                for (int i = 1; i < steps; i++)
                    setBounds(ix.next(), iy.next(), iw.next(), ih.next());
            } else if (resized) {
                for (int i = 1; i < steps; i++)
                    setBounds(x, y, iw.next(), ih.next());
            } else if (moved) {
                for (int i = 1; i < steps; i++)
                    setBounds(ix.next(), iy.next(), width, height);
            }
            setBounds(x, y, width, height);
        } finally {
            GUI.invokeAfterAWT(new Runnable() {
                    public void run() {
                        mAnimatingReshape = false;
                        if (DEBUG.DOCK) out("setShapeAnimated: AWT eventQueue cleared, animating stopped.");
                        
                        // You might thinking calling the final setBounds in here would
                        // be cleaner than the manual invalidate/validate but what you get
                        // is a much messier result on screen.

                        if (resized) {
                            invalidate();
                            validate();
                        }
                    }
                });
            
            //setIgnoreRepaint(false); // can apparently never recover from this on Mac
            //repaint();
        }

        if (DEBUG.DOCK) out("setShapeAnimated: returning");
        
    }
    
    public boolean isRolledUp() {
        return isRolledUp;
    }
    
    private void dragToConstrained(int x, int y, boolean relaxed)
    {
        final Point p;

        /*
           // can't drag up into top dock region if we do this.
          
        if (y < GUI.GInsets.top) {
            // HARD CONSTRAINT: never above the top screen inset.
            // don't allow us above an OS menu bar on top of the screen,
            // such as on the Mac.  We compenstate for this here
            // as it makes later constraint adjustments saner.
            y = GUI.GInsets.top;
        }
        */

        if (relaxed) {
            if (DEBUG.EDGE) getConstrainedLocation(x, y); // for debug output
            p = new Point(x, y);
        } else
            p = getConstrainedLocation(x, y);

        if (p.y < GUI.GInsets.top)
            p.y = GUI.GInsets.top;
        else if (p.y > GUI.GScreenHeight - CollapsedHeightVisible)
            p.y = GUI.GScreenHeight - CollapsedHeightVisible;

        
        if (DEBUG.DOCK) out("dragToConstrained " + x + "," + y + ((p.x == x && p.y == y) ? "" : " = " + Util.out(p)));
        
        dragSetLocation(p.x, p.y);
    }
    
    private void superSetLocation(int x, int y)
    {
        if (DEBUG.DOCK && DEBUG.META) out("superSetLocation " + x + "," + y);
        super.setLocation(x, y);
        mStickingRight = atScreenRight();
    }
    
    public void dragSetLocation(int x, int y)
    {
        if (DEBUG.DOCK && DEBUG.META) out("dragSetLocation " + x + "," + y);

        superSetLocation(x, y);
        
        if (isMac == false && mChild != null) {
            
            // Manually move all of our children (and their children).  This works
            // beauftifly smoothly on PC, and gets terribly behind on Mac unless we set
            // up the native OSX to handle it.

            updateAllChildLocations();
        }
    }
    

    public void setLocation(int x, int y)
    {
        if (DEBUG.DOCK && DEBUG.META) out("setLocation " + x + "," + y);

        superSetLocation(x, y);
        updateAllChildLocations();
    }

    
    private static class Edge {
        static final int LIP_LEFT = -1;
        static final int LIP_RIGHT = 1;
        static final int LIP_UP = -1;
        static final int LIP_DOWN = 1;

        
        final int axis; // x or y value
        final int min;  // if axis is x, min y, if axis is y, min x
        final int max;  // if axis is x, max y, if axis is y, max x

        private Edge lip;
        private int lipDir = 0;

        final boolean isLip;

        Edge(int axis, int oppositeAxisStart, int oppositeAxisEnd) {
            this.axis = axis;
            if (oppositeAxisStart < oppositeAxisEnd) {
                this.min = oppositeAxisStart;
                this.max = oppositeAxisEnd;
            } else {
                this.min = oppositeAxisEnd;
                this.max = oppositeAxisStart;
            }
            isLip = max - min < 2;            
        }
        Edge(int axis) {
            this(axis, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        boolean inRangeOf(Edge e) {
            if (false && isLip)
                return max > e.min && e.max > min && e.min >= min; 
            else
                return max > e.min && e.max > min;
        }

        // currenly, lips only happen at the MIN value, which means left or top
        // of an edge.  They're a catch along the OPPOSITE axis.
        void addLip(int direction) {
            this.lip = new Edge(min, min, min /*+ direction*/  );
            this.lip.lipDir = direction;
            if (DEBUG.EDGE) System.out.println(this + "\n\tadded lip: " + lip);
        }

        public String toString() {
            String s = "[" + axis;
            if (min == Integer.MIN_VALUE)
                return s + "]";
            else if (isLip || lipDir != 0)
                return s + " LIP " + min + "+" + lipDir + "]";
            else
                return s + " len " + (max-min) + ":" + min + "-" + max + "]";
        }
        
    }

    private static class EdgeBox {
        
        public Edge top, left, right, bottom;
        public final int width, height;
        
        public EdgeBox(final Component given) {
            if (given == null) {
                top = left = right = bottom = null;
                width = height = 0;
            } else {
                Component c = given;

                //if (c.getParent() instanceof JTabbedPane)
                //c = (JTabbedPane) c.getParent();
                if (c.getParent() instanceof JViewport)
                    c = (JViewport) c.getParent();
                //if (c.getParent() instanceof JScrollPane)
                //c = (JScrollPane) c.getParent();

                Point loc = new Point(c.getX(), c.getY());

                if (c instanceof Window == false)
                    SwingUtilities.convertPointToScreen(loc, c);
                    
                int TOP = loc.y;
                int LEFT = loc.x;
                int RIGHT = LEFT + c.getWidth();
                int BOTTOM = TOP + c.getHeight();

                this.top =      new Edge(TOP,    LEFT, RIGHT);
                this.bottom =   new Edge(BOTTOM, LEFT, RIGHT);
                this.left =     new Edge(LEFT,   TOP, BOTTOM);
                this.right =    new Edge(RIGHT,  TOP, BOTTOM);

                this.width = RIGHT - LEFT;
                this.height = BOTTOM - TOP;
                
                if (DEBUG.EDGE)
                    System.out.println(this + " for " + GUI.name(c)
                                       + " parent=" + GUI.name(c.getParent())
                                       + " from " + given
                                       );
            }
        }


        public EdgeBox(Rectangle r) {
            int TOP = r.y;
            int LEFT = r.x;
            int RIGHT = LEFT + r.width;
            int BOTTOM = TOP + r.height;

            this.top =      new Edge(TOP,    LEFT, RIGHT);
            this.bottom =   new Edge(BOTTOM, LEFT, RIGHT);
            this.left =     new Edge(LEFT,   TOP, BOTTOM);
            this.right =    new Edge(RIGHT,  TOP, BOTTOM);
            
            this.width = RIGHT - LEFT;
            this.height = BOTTOM - TOP;
        }
        

        public String toString() {
            return "EdgeBox[top" + top + " left" + left + " bottom" + bottom + " right" + right + "]";
        }
    }

    private static class EdgeArray {
        final String name;
        final Edge[] array = new Edge[64];
        int length = 0;

        EdgeArray(String name) {
            this.name = name;
        }

        void add(Edge e) {
            if (e != null) {
                if (DEBUG.EDGE && DEBUG.META) System.out.println("Added " + name + "\tedge" + e);
                array[length++] = e;
            }
        }
        
        void add(int i) {
            add(new Edge(i));
        }

        void reset() {
            length = 0;
        }
    }

    private static void addEdges(EdgeBox b, DockWindow dw) {

        //if (DEBUG.DOCK) System.out.println("addEdges " + e);
        
        StickyLeftEdges.add(b.right);
        StickyRightEdges.add(b.left);

        // Don't add top or bottom edges of mid-stack DockWindow's.
        // That is, ignore the middle of the stack.

        // This would also ignore the DockWindow parent we just pulled
        // away from if we were stacked, as it appears as if its bottom
        // edge is in the "middle", which is why we check mMouseWasPressed
        // on the child.
        
        // TODO-FYI: currently double impl: dw may always be null: see addEdges(DockWindow)

        if (dw == null || dw.mChild == null || dw.getWidth() != dw.mChild.getWidth() || dw.mChild.mMouseWasPressed)
            StickyTopEdges.add(b.bottom);
        
        if (dw == null || dw.mParent == null || dw.getWidth() != dw.mParent.getWidth())
            StickyBottomEdges.add(b.top);

        // add a "lip" at the top and bottom LEFT edge for anything sliding along the top
        // or bottom of a window to catch on just before it goes beyond the left edgb.
        // We don't do this for the right edge.

        if (b.top != null)
            //e.top.addLip(b.left.axis);
            StickyLeftEdges.add(new Edge(b.left.axis, b.top.axis, b.top.axis - 1));

        if (b.bottom != null)
            StickyLeftEdges.add(new Edge(b.left.axis, b.bottom.axis, b.bottom.axis + 1));

        // also add lips to left and right at the top for top alignment
        if (b.top != null) {
            if (false) {
                StickyTopEdges.add(new Edge(b.top.axis, b.left.axis, b.left.axis - 1));
                StickyTopEdges.add(new Edge(b.top.axis, b.right.axis, b.right.axis + 1));
            } else {
                b.left.addLip(Edge.LIP_LEFT);
                b.right.addLip(Edge.LIP_RIGHT);
            }
        }
        
    }
    
    private static void addEdges(Component c) {
        if (c != null)
            addEdges(new EdgeBox(c), null);
    }
    
    private static void addEdges(DockWindow dw) {
        if (false) {

            // get rid of all inter-stack edges, but keep inter-stack outside lips
            
            addEdges(new EdgeBox(dw), dw);

        } else {

            // get rid of all inter-stack edges, including outside lips

            EdgeBox edges = new EdgeBox(dw);
            
            if (dw.mParent != null)
                edges.top = null;
            
            // don't add our bottom if we have a child, unless it's about to be the mover

            // Also, would nice if dragging a wide top window with a narrow
            // child window (e.g., current tester Font on top of tester Link),
            // that the wider Font also adds it's bottom for sticking.
            
            if (dw.mChild != null && !dw.mChild.mMouseWasPressed)
                edges.bottom = null;
            
            addEdges(edges, null);
        }
    }

    /*
    static void setAllOnTop(boolean onTop) {
        Iterator i = sAllWindows.iterator();
        while (i.hasNext()) {
            DockWindow dw = (DockWindow) i.next();
            dw.setAlwaysOnTop(onTop);
            dw.toFront();
        }
    }
    */
    
    public static void raiseAll() {
        if (DEBUG.DOCK||DEBUG.WORK) Log.debug("raiseAll");
        for (DockWindow dw : AllWindows)
            dw.toFront();
    }
        
    private static void refreshScreenInfo(DockWindow mover)
    {
        GUI.refreshGraphicsInfo();

        StickyLeftEdges.reset();
        StickyRightEdges.reset();
        StickyTopEdges.reset();
        StickyBottomEdges.reset();
        
        if (GUI.GInsets.left <= 4) {
            StickyLeftEdges.add(0);
        } else {
            StickyLeftEdges.add(GUI.GInsets.left);
        }

        StickyRightEdges.add(GUI.GScreenWidth);
        StickyBottomEdges.add(GUI.GScreenHeight - GUI.GInsets.bottom);

        StickyRightEdges.add(0); // in case multi-monitor
        StickyLeftEdges.add(GUI.GScreenWidth); // in case multi-monitor

        // todo: add bottom edge of other displays in case resolution
        // is different: should really only take effect if on that
        // screen tho, not across entire virtual desktop.
        
        //if (GUI.GInsets.bottom > 0)
        //    StickyBottomEdges.add(GUI.GScreenHeight - GUI.GInsets.bottom);

        if (MainDock != null) {
            if (MainDock.mGravity == DockRegion.BOTTOM)
                StickyBottomEdges.add(MainDock.getY());
            else
                StickyTopEdges.add(MainDock.getY());
        }
        if (TopDock != null) {
            TopDock.moveToY(GUI.GInsets.top);
            BottomDock.moveToY(GUI.GScreenHeight - GUI.GInsets.bottom);
        }
        
        if (mover == null)
            return;
        
        addEdges(VUE.getMainWindow());
        //addEdges(VUE.getActiveViewer());

        
        for (DockWindow dw : AllWindows) {
            //if (dw == mover || !dw.isVisible() || dw.inSameStack(mover))
            if (dw == mover || !dw.isVisible() || mover.hasDescendant(dw))
                continue;

            addEdges(dw);
        }
        
    }

    private DockWindow getStackTop() {
        if (mParent == null)
            return this;
        else
            return mParent.getStackTop();
    }
    
    private DockWindow getStackBottom() {
        if (mChild == null)
            return this;
        else
            return mChild.getStackBottom();
    }

    public boolean isStackTop() {
        return mParent == null && mChild != null;
    }

    public boolean isStacked() {
        return mChild != null || mParent != null;
    }
    
    public boolean isDocked() {
        return mDockRegion != null;
    }
    
    public boolean inSameStack(DockWindow dw) {
        DockWindow ourTop = getStackTop();
        return ourTop != null && ourTop == dw.getStackTop();
    }

    public boolean hasDescendant(DockWindow child) {
        if (mChild == null)
            return false;
        else if (mChild == child) {
            if (DEBUG.DOCK) out("found descendant " + child);
            return true;
        } else
            return mChild.hasDescendant(child);
    }
    

    /** @return total stack height BELOW us, or just our height if no children under us */
    public int getStackHeight() {
        if (mChild == null)
            return getVisibleHeight();
        else
            return getVisibleHeight() + mChild.getStackHeight();
    }
    
    private static final int StickyDistance = 15;

    /** columns sticky on their right side (window left edges stick to them) */
    // the names are reversed so they make sense when read from the perpective of
    // Window edges, as opposed to screen edges.
    private static EdgeArray StickyLeftEdges = new EdgeArray("RIGHT");
    private static EdgeArray StickyRightEdges = new EdgeArray("LEFT");
    private static EdgeArray StickyTopEdges = new EdgeArray("BOTTOM");
    private static EdgeArray StickyBottomEdges = new EdgeArray("TOP");

    private Point getConstrainedLocation(int x, int y)
    {
        Rectangle movingBounds = getBounds();
        movingBounds.x = x;
        movingBounds.y = y;

        if (mMovingStackHeight > 0)
            movingBounds.height = mMovingStackHeight;
        
        // out("movingBounds " + Util.out(movingBounds));
            
        // moving bounds is now the would-be bounds of what's
        // moving (a DockWindow or a DockWindow stack) if we
        // didn't constrain the movement at all.
        
        EdgeBox movingBox = new EdgeBox(movingBounds);

        return getConstrainedXY(x, y, movingBox);
        //return new Point(getConstrainedX(x, movingBox), getConstrainedY(y, movingBox));
    }

    private static boolean computeClosestEdge(Edge movingEdge,
                                              EdgeArray stickyEdges,
                                              EdgeHit result)
    {
        boolean hitResult = false;
            
        for (int i = 0; i < stickyEdges.length; i++) {

            hitResult |= mergeWithResult(stickyEdges.array[i], movingEdge, result);

            /*
            Edge edge = stickyEdges.array[i];
            if (edge.axis < 0)
                continue;

            if (!edge.inRangeOf(movingEdge))
                continue;

            int rawDelta = edge.axis - movingEdge.axis;
            int delta = Math.abs(rawDelta);
            
            if (delta < result.delta) {
                result.rawDelta = rawDelta;
                result.delta = delta;
                result.edge = edge;
                hitResult = true;
            }
            */
        }
        
        return hitResult;
    }

    /** @return true if changed result */
    private static boolean mergeWithResult(Edge edge, Edge movingEdge, EdgeHit result)
    {
        if (edge.axis < 0)
            return false;

        if (!edge.inRangeOf(movingEdge))
            return false;

        int rawDelta = edge.axis - movingEdge.axis;
        int delta = Math.abs(rawDelta);
            
        if (delta < result.delta) {
            result.rawDelta = rawDelta;
            result.delta = delta;
            result.edge = edge;
            return true;
        }
        
        return false;
    }

    private static class EdgeHit {

        int rawDelta = 0;

        /** smallest delta found (absolute value of rawDelta) */
        int delta = Integer.MAX_VALUE;

        /** the closest edge (had smallest delta) */
        Edge edge;

        void reset() {
            rawDelta = 0;
            delta = Integer.MAX_VALUE;
            edge = null;
        }

        public String toString() {
            return " delta " + rawDelta + " from " + edge;
        }

    }


    private Point getConstrainedXY(int x, int y, EdgeBox movingBox)
    {
        final EdgeHit Xresult = new EdgeHit();
        final EdgeHit Yresult = new EdgeHit();

        // COMPUTE X
        
        computeClosestEdge(movingBox.left, StickyLeftEdges, Xresult);
        boolean matchedRight =
            computeClosestEdge(movingBox.right, StickyRightEdges, Xresult);
        
        if (DEBUG.EDGE) {
            Edge movingEdge = matchedRight ? movingBox.right : movingBox.left;
            out("X " + movingEdge + " sees " + (matchedRight?"RIGHT":"LEFT") + Xresult);
        }

        // COMPUTE Y

        computeClosestEdge(movingBox.top, StickyTopEdges, Yresult);
        boolean matchedBottom =
            computeClosestEdge(movingBox.bottom, StickyBottomEdges, Yresult);

        if (DEBUG.EDGE) {
            Edge movingEdge = matchedBottom ? movingBox.bottom : movingBox.top;
            out("Y " + movingEdge + " sees " + (matchedBottom?"BOTTOM":"TOP") + Yresult);
        }

        // ADJUST RESULTS FOR LIPS
        
        if (Xresult.delta < StickyDistance) {
            if (matchedRight)
                x = Xresult.edge.axis - movingBox.width;
            else
                x = Xresult.edge.axis;

            Edge lip = Xresult.edge.lip;
            // lips only at top and left right now
            if (lip != null) {
                //computeClosestEdge(movingBox.top, new Edge[] { lip }, Yresult);
            }
        }

        if (Yresult.delta < StickyDistance) {
            if (matchedBottom)
                y = Yresult.edge.axis - movingBox.height;
            else
                y = Yresult.edge.axis;
        }

        return new Point(x, y);
    }

    /*
      
    private int getConstrainedX(int x, EdgeBox movingBox)
    {
        final EdgeHit result = new EdgeHit();

        computeClosestEdge(movingBox.left, StickyLeftEdges, result);
        boolean matchedRight =
            computeClosestEdge(movingBox.right, StickyRightEdges, result);
        
        if (DEBUG.DOCK) {
            Edge movingEdge = matchedRight ? movingBox.right : movingBox.left;
            out("X " + movingEdge + " sees " + (matchedRight?"RIGHT":"LEFT") + result);
        }

        if (result.delta < StickyDistance) {
            if (matchedRight)
                return result.edge.axis - movingBox.width;
            else
                return result.edge.axis;
        } else
            return x;
    }

    private int getConstrainedY(int y, EdgeBox movingBox)
    {
        final EdgeHit result = new EdgeHit();

        computeClosestEdge(movingBox.top, StickyTopEdges, result);
        boolean matchedBottom =
            computeClosestEdge(movingBox.bottom, StickyBottomEdges, result);

        if (DEBUG.DOCK) {
            Edge movingEdge = matchedBottom ? movingBox.bottom : movingBox.top;
            out("Y " + movingEdge + " sees " + (matchedBottom?"BOTTOM":"TOP") + result);
        }

        if (result.delta < StickyDistance) {
            if (matchedBottom)
                return result.edge.axis - movingBox.height;
            else
                return result.edge.axis;
        } else
            return y;
    }

    */

    /*
    private int getStickyAdjustment(int actual, int stickyAxis, EdgeHit hit)
    {
        if (true) return stickyAxis;

        if (hit.delta > 5) {
            float mag = hit.mag;
            float delta = hit.delta;
            float maxDist = StickyDistance;
            float ratioA = delta / maxDist;
            float ratioB = ratioA * ratioA;
            float adj = mag - mag * ratioB;
            //int adj = (int) ((float)hit.mag / ((float)hit.delta / (float)StickyDistance));
            out("raw adj " + hit.mag + " ratioA " + ratioA + " ratioB " + ratioB + " soft adj " + adj);
            return actual + (int) adj;
        } else {
            return actual + hit.mag;
        }
    }
    */
    
    
    
    /*
    public void setLocationAnimated(int x, int y)
    {
        final int steps = 40;
        Interpolator ix = new Interpolator(steps, getX(), x, "x");
        Interpolator iy = new Interpolator(steps, getY(), y, "y");

        for (int i = 1; i < steps; i++)
            placeWindow(ix.next(), iy.next());
        
        placeWindow(x, y);
    }
    */
    

    private DockWindow getNearWindow(int localMouseX, int localMouseY) {
        int winX = getX();
        int winY = getY();

        // convert from window to screen mouse coords        
        int mouseX = localMouseX + winX;
        int mouseY = localMouseY + winY;
        
        for (DockWindow dw : AllWindows) {

            if (!dw.isVisible() || dw == this)
                continue;

            // only dock like windows together for now
            if (isToolbar != dw.isToolbar)
                continue;


            // For now, only find a near window if the DockWindow
            // is exactly along the bottom of the desired parent,
            // presumably due to stickiness.

            int left = dw.getX();
            int right = dw.getX() + dw.getWidth();
            int bottom = dw.getY() + dw.getHeight();
            int winRight = winX + getWidth();

            if (winY == bottom &&
                ((winX >= left && winX < right) ||
                 (winRight > left && winRight < right)))
                
                return dw;


            /*
             * This is too agressive: from now only, ONLY attach if the window is
             * already just below the parent.
             *
             
            Rectangle bounds = dw.getBounds();
            
            if (bounds.contains(mouseX, mouseY)) {
                
                if (hasDescendant(dw)) {
                    // This can happen if stickiness has moved as stack up,
                    // and the mouse is now over a child of the mover.
                    ; // -- don't allow attachment
                } else {
                    return dw;
                }
            }
            // if our window is also sitting just below the
            // other window (probably via stickyness), also
            // attach to it.

            bounds.height++;
            if (bounds.contains(winX, winY) ||
                bounds.contains(winX + getWidth()-1, winY))
                return dw;
            */

        }

        return null;
    }

    protected boolean titleBarHit(MouseEvent e) {
        return e.getY() < CollapsedHeightVisible
            || (SidewaysRollup && mContentPane.mTitle.contains(e.getX(), e.getY()));
    }
    
    /** interface FocusManager.MouseInterceptor impl */
    public boolean interceptMousePress(MouseEvent e) {

        // okay, no good: if, say, this press opens a menu, the child DockWindow
        // is being raised over the menu when we're in setAlwaysOnTop mode!

        // However, we're still stuck: clicking most anywhere in window has mac send the
        // OSX window some kind of message to raise itself (hey -- maybe we can turn
        // that off?) -- so turning this off means we see our menus as we need, but now
        // children are going under parent DockWindows when shadow is on.

        // TODO: okay, maybe stay with brushed metal in 1.5 for it's nice look and nice
        // look not having a shadow, and when rolled up, swap in a whole new freakin
        // WINDOW that is override-redirected so it can be small?  Of course, this still
        // forces us into java 1.5.

        // FYI, we need this on the PC: Windows has no special OS window raising code.
        // (It has no native window shadow's, so it doesn't need any)

        // It's okay to do this in java 1.4, as we can't be alwaysOnTop, which
        // is why this is a problem.
        
        // 2008-04-21 SMF: On Mac Leopard apparently windows are no longer automatically
        // raised when they get focus / are click on, so we also always do it in that case.

        // 2008-04-21 SMF: We always auto-raise on any mouse click when on Windows now
        // too.  Toolbars must not be focusable or they can steal key events and the
        // user doesn't know where the input is going.  This is why all toolbars are
        // setFocusableWindowState(false).  This creates problems elsewhere tho: windows
        // won't always auto-raise when clicked on if they don't have focusable window
        // state.  E.g., the Format toolbar raises just fine when when clicked on and not
        // focusable, but for some reason, the FloatingZoomPanel does not.

        // 2008-04-21 SMF: Oh, and now that all toolbars are not focusable, we want to
        // do this for all mac platforms, not just Leopard, tho note that this code is
        // MUCH more important for Leopard than it is for Tiger.

        if (!Util.isUnixPlatform())
            raiseStack();

//         if (Util.isMacLeopard()
//             || Util.isWindowsPlatform()
//             || (MacWindowShadowEnabled && (!GUI.UseAlwaysOnTop || !isMac)))
//             {
//                 raiseStack();
//             }

        if (AllWindows.getLast() != this) {
            // Maintain stacking order: last one in list will be last to show/toFront,
            // so will be on top.
            
            // TODO: safer: simply added an index we can increment every time for
            // the top window based on a global static count, then sort
            // based on the index for traversals that need to pay addention to order.
            
            synchronized (AllWindows) {
                AllWindows.remove(this);
                AllWindows.addLast(this);
            }

        }
                
        return false;
    }

    private void raiseStack() {
        if (DEBUG.DOCK) out("toFront my stack");
        toFront();
        raiseChildren();
    }

    
    public void mousePressed(MouseEvent e)
    {
        mMouseWasPressed = true;
        
        if (DEBUG.MOUSE && DEBUG.META) out(e);

        if (e.getSource().getClass() == ResizeCorner.class) {
            if (!isRolledUp())
                mDragSizeStart = getSize(); // we'll be resizing the window

            e = SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
        }

        mDragStart = e.getPoint();
        mDragStartScreen = e.getPoint();
        mDragStartScreen.translate(getX(), getY());
        mStickingRight = atScreenRight();
        mWasStickingRight = mStickingRight;

        if (DEBUG.MOUSE) out("mDragStartScreen=" + mDragStartScreen + " mDragStart=" + mDragStart);
        
        if (false && isMac) { // don't shadow child
            // better: don't raise us up if we're rolling up, but then would
            // have to do this on mouseReleased
            // TODO: is this getting repeated after interceptMousePress?
            raiseStack();
        }

        // update screen size, insets, etc for window dragging constraints.
        refreshScreenInfo(this);
    }
    
    public void mouseDragged(MouseEvent e)
    {
        if (mDragStart == null) {
            out("mouseDragged with no mousePressed (mDragStart is null)");
            return;
        }

        if (e.getSource().getClass() == ResizeCorner.class)
            e = SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
        
        mMouseWasDragged = true;
        
        if (mDragSizeStart != null) {
            dragResizeWindow(e);
        } else {
            dragMoveWindow(e);
        }
    }

    private void dragResizeWindow(MouseEvent e)
    {
        mWindowDragUnderway = true;

        int newWidth = mDragSizeStart.width + (e.getX() - mDragStart.x);
        int newHeight = mDragSizeStart.height + (e.getY() - mDragStart.y);

        newWidth = minUnrolledWidth(newWidth);
        newHeight = minUnrolledHeight(newHeight);
            
        setSize(newWidth, newHeight);

    }
    
    private void dragMoveWindow(MouseEvent e)
    {
        if (mWindowDragUnderway == false) {
            if (!dragStartWindowMove(e))
                return;
        }

        int x = e.getX() + getX() - mDragStart.x;
        int y = e.getY() + getY() - mDragStart.y;
        
        boolean relaxed = e.isShiftDown();

        dragToConstrained(x, y, relaxed);
    }

    /** @return true if the drag has actually started */
    private boolean dragStartWindowMove(MouseEvent e)
    {
        //---------------------------------
        // We're just starting the drag
        //---------------------------------
            
        Point screen = e.getPoint();
        screen.translate(getX(), getY());
            
        int dx = screen.x - mDragStartScreen.x;
        int dy = screen.y - mDragStartScreen.y;
            
        if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
            if (DEBUG.MOUSE) out("delaying drag start with dx="+dx + " dy="+dy + " screen=" + screen);
            return false;
        }
            
        if (isMac) {
            // Make sure we're all on top, otherwise can get wierd effects such as free-floating
            // DockWindow's "slicing" in between our stack windows if it happens to have a MacOSX
            // z-order in between two of our children.

            // Children can go behind VUE window as soon as mouse goes down, which should
            // be impossible... This is because of our MacOSX child window associations during
            // drag.  So when a stack as dropped, we always raise it after the windows
            // are MacOSX dissasociated.

            // We need to raise them again here, because if some OTHER DockWindow was MacOSX
            // "activated" (clicked on), it's z-order may just happen to wind up smack in the
            // middle of our stack, which will show up as slicing thru our stack during the drag.
                
            raiseStack();
        }

        mWindowDragUnderway = true;

        repaintTitle();

        if (isStackTop())
            mMovingStackHeight = getStackHeight();
        else
            mMovingStackHeight = 0;

        //if (isMacAqua) MacOSX.setAlpha(this, 0.75f);

        if (e.isShiftDown()) {
            // remove from any stack it's in and drag free
            if (mParent != null)
                mParent.removeChild();
            if (mChild != null)
                setChild(null);
        } else {
            if (isMac) {
                // window drag begins: attach (via OSX) all children to the parent
                // being dragged if we're on the mac
                attachChildrenForMoving(this);
                    
                if (e.isAltDown())
                    attachSiblingsForMoving();

            }
        }

        return true;
    }
    
    
    // TODO: okay, if we get a damn click-count in here and as long as drag start was still
    // delayed, do mouseClicked roll-up handling in here instead of mouseClicked -- too easy
    // to miss clicks when mouse is moving too fast (prob mainly a trackpad problem)
    
    public void mouseReleased(MouseEvent e)
    {
        if (DEBUG.MOUSE && DEBUG.META) out(e);

        if (e.getSource().getClass() == ResizeCorner.class) // should never be needed, but just in case
            e = SwingUtilities.convertMouseEvent(mResizeCorner, e, this);
        
        if (mWindowDragUnderway)
            dropWindow(e);
        else if (!isToolbar)
            handleMouseClicked(e); // treat a delayed drag start as a click

        mDragStart = null;
        mDragSizeStart = null;
        mMouseWasDragged = false;
        mWindowDragUnderway = false;
        mMouseWasPressed = false;

        if (mWasStickingRight != mStickingRight)
            updateAllChildLocations();
        
    }
    
    private void handleMouseClicked(MouseEvent e) {
        if (DEBUG.MOUSE && DEBUG.META) out(e);
        
        if (e.getSource().getClass() == ResizeCorner.class)
            return;
         
        //On windows the click count kept growing if i I didn't move the mouse slightly between clicks so I put
        //an extra test in here for clicks > 2 % 2 to test for additional double clicks seems to work
        //well on windows i'll double check with mac.
        if (/*e.getClickCount() != 0 &*/ (e.getClickCount() == 2 || (e.getClickCount() > 2 && (e.getClickCount() % 2 ==0))) && titleBarHit(e)) {
            // clickCount != 0 prevents action with long mouse down
            // clickCount != 2 allows double-click not undo what just happened on single click,
            // but rapid clicking for testing (clickCount keeps climbing) is allowed.
        	
            setRolledUp(!isRolledUp());

        } else {
            // if we click on the bottom pixels when we're at the screen bottom,
            // treat it as a roll-up request

            // So this can always work, we enforce at least a 1 pixel empty bottom
            // border around the content pane for any tool component that has no
            // internal border of it's own (such as the current MapPanner).
        
            if (e.getY() > getHeight() - 5 && !isRolledUp() /*&& atScreenBottom() */ ) {
                setRolledUp(true);
            }
        }
    }


    /*
    private int getDockRegionRolledWidth() {
        DockWindow rightSibling = getSiblingToRight();
        if (rightSibling == null)
            return getWidth();
        else
            return rightSibling.getX() - getX();
    }
    private DockWindow getSiblingToRight() {
        Iterator i = sAllWindows.iterator();
        DockWindow closest = null;
        int minDeltaX = Integer.MAX_VALUE;

        int myTop = getY();
        int myLeft = getX();
        
        while (i.hasNext()) {
            DockWindow dw = (DockWindow) i.next();

            if (!dw.isVisible())
                continue;

            if (dw.getY() == myTop && dw.getX() > myLeft) {
                int deltaX = dw.getX() - myLeft;
                if (deltaX < minDeltaX) {
                    minDeltaX = deltaX;
                    closest = dw;
                }
            }
        }
        return closest;
    }
    */
    
    

    /** @param MouseEvent should be the MOUSE_RELEASED event where the window was dropped*/
    private void dropWindow(MouseEvent e)
    {
        if (DEBUG.DOCK) out("dropWindow: curBounds " + getBounds());

        //if (isMacAqua) MacOSX.setAlpha(this, 1f);

        GUI.refreshGraphicsInfo();
        
        DockWindow near;
        DockRegion dockRegion = DockRegion.findRegion(this);

        detachSiblingsForMoving();
            
        if (dockRegion != null) {
            near = null;
        } else if (e.isShiftDown()) { // don't allow attachment if relaxed movement
            near = null;
        } else {
            near = getNearWindow(e.getX(), e.getY());

            // don't attach a child below something that's already
            // at the bottom of the screen!
            
            if (near != null && near.atScreenBottom())
                near = null;
        }
        
        if (DEBUG.DOCK) out("dropWindow: near " + near + ", region " + dockRegion);

        // if (isMac) detachChildrenForMoving(this);

        if (near != null) {

            if (hasDescendant(near)) {
                Log.error("can't drop a parent onto a descendant: " + near);
            } else {
                near.setChild(this);
            }
                
        } else if (mParent != null) {

            // If we had a parent, detach from it.

            mParent.removeChild();
        }

        /*
        if (dockRegion != null) {
            dockRegion.assignMembers();
        } else if (mDockRegion != null) {
            mDockRegion.remove(this);
        }
        */

        DockRegion.assignAllMembers();

        if (isMac) {
                
            // do this as late as possible or sometimes the setLocation from updateChild is
            // actually *failing* (no error or exception) for the child of a window that was
            // just reparented.  Must be related to using Mac NSWindow code w/java.

            // TODO: Christ, but doing it down here causes toFront() to freakin fail!  Okay, is
            // possible to solve by interjecting this into setChild to do after the
            // setLocation's, but before the raises.  failure case: drag bottom two windows up
            // to top in stack, "inserting" them there. Second window will be on top of it's
            // child, the third window, yet they were all toFront'ed in proper order.

            // Very rarely, still seeing a window (child of moved usually?) get the wrong
            // location after a reparenting -- this I'm guessing is due to some location data
            // in java not being up to date w/mac -- notice that we get no setLocation events
            // on children that are moving while their parent is being moved -- maybe we call
            // setLocation just for good measure? always or at end?  oh, i think at end is
            // where it's happening.  Or hey, maybe *that* setLocation is screwing us up,
            // because java thinks it needs to be moved when it really doesn't?
                
            detachChildrenForMoving(this);
            
            // This is CRUCIAL to restore z-ordering based on proper java window parentage
            raiseStack();
        }

        updateWindowShadow();
        repaintTitle();
        
    }

    void assignDockRegion(DockRegion region) {
        if (mDockRegion != region) {
            mDockRegion = region;
            if (DEBUG.DOCK) out("assignDockRegion: " + region);

            // if we just dragged an unrolled window to the bottom with
            // just the title bar showing (we dragged the visible part off
            // the bottom of the screen), auto roll-up the DockWindow
            // so when it's clicked on again it will roll upwards.
            
            if (region == BottomDock && !isRolledUp() && getY() + getHeight() > GUI.GScreenHeight)
                setRolledUp(true);
        }
    }
    void assignDockSiblings(DockWindow prev, DockWindow next) {
        boolean changed = false;
        if (mDockPrev != prev) {
            mDockPrev = prev;
            changed = true;
        }
        if (mDockNext != next) {
            mDockNext = next;
            changed = true;
        }
        if (changed)
            repaintTitle();
    }

    private void repaintTitle() {
        //if (DEBUG.DOCK) out("repaintTitle");
        mContentPane.mTitle.repaint();
    }



    public void mouseClicked(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {
        // We're trying this to make sure any rollover's in the DockWindow will always work.
        requestFocus();
    }
    
    public void mouseExited(MouseEvent e) {}
    

    private void updateWindowShadow() {
        if (isMac) {

            if (!MacWindowShadowEnabled) {
                setWindowShadow(false);
                return;
            } 
            
            if (DEBUG.DOCK) out("updateWindowShadow: docked=" + isDocked() + " rolled=" + isRolledUp());

            boolean hideShadow =
                (isToolbar /*&& isDarkTitleBar*/) ||
                (isDocked() && isRolledUp()
                 || (MainDock != null && MainDock.getY() == getY() + getHeight()))
                ;

            setWindowShadow(!hideShadow);
        }
    }

    private void setWindowShadow(boolean shadow) {
        if (mHasWindowShadow != shadow && isDisplayable()) {
            mHasWindowShadow = shadow;
            if (DEBUG.DOCK) out("setShadow " + shadow);
            try {
                MacOSX.setShadow(this, shadow);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void removeChild() {
        if (mChild != null) {
            if (DEBUG.DOCK) out("removeChild " + mChild);
            //tufts.macosx.Screen.removeChildWindow(this, mChild);
            mChild.setParent(null);
            mChild = null;
            updateWindowShadow();
        }
    }

    /** set the child of this DockWindow to the given child */
    public void setChild(DockWindow newChild) {
        setChild(newChild, true);
    }


    /** add a child to the bottom of this stack
     * @return "this" DockWindow, for chaining calls to addChild
     */
    public DockWindow addChild(DockWindow newChild) {
        getStackBottom().setChild(newChild, true);
        return this;
    }
    

    private void setChild(DockWindow newChild, boolean updateChildren)
    {
        if (DEBUG.DOCK) out("setChild " + newChild);

        if (newChild == null) {
            removeChild();
            return;
        }

        if (newChild == mParent)
            throw new Error(this + " cannot setChild on parent " + mParent);

        if (newChild.mParent != null && newChild.mParent != this) {
            //tufts.macosx.Screen.removeChildWindow(newChild.mParent, newChild);
            newChild.mParent.removeChild();
        }

        if (mChild == newChild) {
            // it's already our child
            if (newChild.mParent != this)
                throw new Error("DockWindow child/parent inconsistency");
            if (isMac)
                updateChildLocation();
            else
                updateAllChildLocations();
        } else {

            boolean reparented = false;

            if (mChild != null) {
                if (mChild.mShowing) {
                    // if we already have a child, and it's not in the process
                    // of going invisible, set it as child of our new child,
                    // but don't let it move it's children yet, as it's
                    // new parent isn't in place yet.
                    reparented = true;
                    newChild.setChild(mChild, false);
                } else {
                    removeChild();
                }
            }

            if (hasDescendant(newChild)) {
                Util.printStackTrace("is already descendent of " + this + " " + newChild);
                return;
            }
            
            mChild = newChild;
            mChild.setParent(this);
            //tufts.macosx.Screen.addChildWindow(getTopParent(), mChild);
            if (updateChildren) {
                
                if (isMac) {
                    // If on mac, child window's of the window just dropped on us
                    // are "attached" to it, and we only need to set the location
                    // of this new immediate child to set all their locations.
                    // And in fact, if we *do* attempt to set the location,
                    // occasionaly java & the underlying NSWindow in Objective-C
                    // code aren't quite in sync yet, and the window can get
                    // placed wrong.
                    if (reparented)
                        updateAllChildLocations();
                    else
                        updateChildLocation();
                    raiseChildren(); // prevent parent shadowing child
                } else {
                    updateAllChildLocations();
                }
                
            }

            if (!newChild.isRolledUp())
                newChild.keepOnScreen();
        }

        updateWindowShadow();
    }

    private void setParent(DockWindow parent) {
        if (mParent == parent)
            return;
        
        /*
          // This doesn't help window shadow at all...
        if (isMac) {
            if (mParent != null)
                MacOSX.removeChildWindow(mParent, this);
            if (parent != null) {
                MacOSX.addChildWindow(parent, this);
                MacOSX.orderAbove(parent, this);
            }
        }
        */
        
        mParent = parent;
        //mContentPane.setCloseButtonVisible(parent == null || !parent.getStackTop().isStackOwner);
        mContentPane.setCloseButtonVisible(!isStacked() || isStackTop());
    }


    /** recursively move all children under us, using given height & y coord instead of current height & y coord */
    private void updateAllChildLocations(int newHeight, int newY) {
        updateChildLocation(true, newHeight, newY);
    }
    
    /** recursively move all children under us */
    private void updateAllChildLocations() {
        updateChildLocation(true, getHeight(), getY());
    }
    
    /** move just the first child under us -- used when on the mac and the windows are "attached" */
    private void updateChildLocation() {
        updateChildLocation(false, getHeight(), getY());
    }
    
    // if last in chain and is Aqua Brushed Metal, could move UP to obscure the extra
    // window size, tho then we'd have to set it's title to BorderLayout.SOUTH, (easy)
    // and more problematically, keep it stacked BELOW it's parent, which would be fine
    // as long as the parent was open, but as soon as it rolled up, we'd have the
    // problem again.
    
    /**
     * Set location of an attached child (if we have one) based on
     * our current location and size.
     *
     * We pass in height & y value arguments as we may call this routine just *before*
     * we (the parent) resizes or repositions, to move the child before we do, as a
     * method of preventing whatever is on the desktop under the stack from briefly
     * flashing through as the stack adjusts.
     *
     * This method recursively call's itself on any children.
     *
     * @param allChildren - recursively update all children, otherwise just the immediate child
     * @param newParentHeight - our height or the one we may be about to take on
     * @param newParentY - our Y location or the value it may be about to take on
     *
     */
    private void updateChildLocation(boolean allChildren, int upcomingHeight, int upcomingY) {
        try {
            _updateChildLocation(allChildren, upcomingHeight, upcomingY);
        } catch (LoopError e) {
            Util.printStackTrace(e);
        }
    }

    private int StackDepth = -1;
    private void _updateChildLocation(boolean allChildren, int upcomingHeight, int upcomingY)
    {
        if (StackDepth-1 > AllWindows.size())
            throw new LoopError("updateChildLocation");

        StackDepth++;
        
        if (mChild != null) {

            if (DEBUG.DOCK)
                out("updateChildLocation, child=" + mChild
                    + " soonH=" + upcomingHeight + " soonY=" + upcomingY + " all=" + allChildren);

            int x;
            int y = upcomingY + upcomingHeight;

            if (mStickingRight) {
                x = GUI.GScreenWidth - mChild.getWidth();
                mChild.mStickingRight = true;
            } else {
                x = getX();
                mChild.mStickingRight = false;
            }
            
            if (CollapsedHeight != CollapsedHeightVisible) {
                // see getVisibleHeight for repeat of this logic
                if (isRolledUp())
                    y -= (CollapsedHeight - CollapsedHeightVisible);
                else
                    y--;
            }

            // To prevent background desktop from flashing thru: If the stack is moving
            // down, move us down first, then our child.  If the stack is moving up,
            // pull up the child first, then move us up.
            
            if (y >= mChild.getY()) {
                mChild.superSetLocation(x, y);
                if (allChildren)
                    mChild.updateAllChildLocations(); // recurse down the chain
            } else {
                if (allChildren)
                    mChild.updateAllChildLocations(mChild.getHeight(), y); // recurse down the chain
                mChild.superSetLocation(x, y);
            }
        }
        
        StackDepth--;
    }

    /** @return visible height -- adjusts for stacked windows below us that may be slightly overlapping us */
    public int getVisibleHeight() {
        if (mChild != null && CollapsedHeight != CollapsedHeightVisible) {
            if (isRolledUp())
                return getHeight() - (CollapsedHeight - CollapsedHeightVisible);
            else
                return getHeight() - 1;
        } else
            return getHeight();
    }

    private class LoopError extends Error {
        LoopError(String where) {
            super("StackDepth=" + StackDepth
                  + " #DockWindows=" + AllWindows.size()
                  + "\n" + where + " DockWindow" + DockWindow.this
                  + "\n\tparent=" + mParent
                  + "\n\t child=" + mChild
                  );
        }
    }

    // *still* occasionally seeing the bottom window in stack go under it's
    // parent -- not consistently, but sometimes when switching focus to
    // the MapViewer
    private void raiseChildren() {
        //GUI.invokeAfterAWT(new Runnable() { public void run() { _raiseChildren(); }});
        // if we *always* do this, it happens too slowly sometimes & causes flashing

        try {
            _raiseChildren();
        } catch (LoopError e) {
            Util.printStackTrace(e);
        }
    }
    
    /** recursive down children */
    private void _raiseChildren()
    {
        if (StackDepth-1 > AllWindows.size())
            throw new LoopError("raiseChildren");

        StackDepth++;
        
        if (mChild != null) {
            mChild.toFront();
            mChild._raiseChildren();
        }
        
        if (isMac && MacWindowShadowEnabled)
            GUI.invokeAfterAWT(new Runnable() { public void run() { updateWindowShadow(); }});

        StackDepth--;
    }
    
    // either move to DockRegion, or make this attach only
    // everyone who's to our right...
    private void attachSiblingsForMoving() {
        if (isMac && isDocked()) {
            Iterator i = mDockRegion.getDockedWindows().iterator();
            while (i.hasNext()) {
                DockWindow dw = (DockWindow) i.next();
                if (dw != this) {
                    if (DEBUG.DOCK) out("attaching sibling " + dw);
                    MacOSX.addChildWindow(this, dw);
                }
            }
        }
    }

    private void detachSiblingsForMoving() {
        if (isMac && isDocked()) {
            Iterator i = mDockRegion.getDockedWindows().iterator();
            while (i.hasNext()) {
                DockWindow dw = (DockWindow) i.next();
                if (dw != this) {
                    if (DEBUG.DOCK) out("detaching sibling " + dw);
                    MacOSX.removeChildWindow(this, dw);
                    //dw.toFront();
                }
            }

            // prevent from going behind main after detachment
            i = mDockRegion.getDockedWindows().iterator();
            while (i.hasNext()) {
                DockWindow dw = (DockWindow) i.next();
                dw.toFront();
            }
        }
    }

    /*
     * There are several constraints with the Mac parent/child window relationships.
     * The biggest is that apparently if the parent/child links go more then three deep,
     * (three windows), the fourth in the chain gets left out of move's by the parent.
     * If this weren't the case, we'd keep all the windows in a stack attach all the
     * time.  Since it isn't, it's easist just to create a temporary set of children,
     * all parented to the top-most window in the stack at the start of moves, and clear
     * it at the end of the move.  (Actually, we could optimize and only clear it if
     * they pull out a child at some point -- then we'd have less of the minor toFront()
     * flashing we see to keep the shadow's of the parents obscuring the children, which
     * we don't see when a child is attached to it's parent -- tho with them all chained
     * to the TOP window, this would only actually fix the first window under the top).
     */
    private void attachChildrenForMoving(DockWindow topOfWindowStack) {
        if (isMac && mChild != null) {
            MacOSX.addChildWindow(topOfWindowStack, mChild);
            mChild.attachChildrenForMoving(topOfWindowStack);
        }
    }
    private void detachChildrenForMoving(final DockWindow topOfWindowStack) {
        if (isMac && mChild != null) {
            //GUI.invokeAfterAWT(new Runnable() { public void run() {
                MacOSX.removeChildWindow(topOfWindowStack, mChild);
                //if (MacWindowShadowEnabled)
                // BE SURE TO RESTORE Z-ORDER OVER PROPER JAVA  PARENT
                // (also keeps on top of shadow)
                //mChild.toFront(); 
                //}});
                // Don't do this here, as this only hits children
                // in stack: we need the whole stack, including top.
            
            
            mChild.detachChildrenForMoving(topOfWindowStack);

            // Need to keep last one detached over it's parent if there is one --
            // it tends to go behind it after detachment.

            // Okay, if we stay with java 1.4.2 and can't use setAlwaysOnTop to fix
            // this, we can call NSWindow.setLevel to raise it up over all other
            // applications, but will need a WindowManager to put it back in place
            // once it loses focus...  Actually, we'll need that WindowManager
            // for 1.5 w/setAlwaysOnTop ANYWAY, so either way we need the WindowManager
            // (to know when the application has lost focus and should hide/lower floating
            // palettes).

            // Okay -- in java 1.5, we get same bug, tho when we use
            // setAlwaysOnTop, it doesn't actually go behind it's java parent
            // (the main Frame), but this is the apparently the same bug
            // where it DOES go under it's DockWindow parent shadow -- even
            // if we do damn orderAbove...


            if (false && mChild.mChild == null) {
                // workaround for above bug: be sure to raise *this* window,
                // then our child: apparently the parent is stepping entirely
                // out of the order somehow?  W/out our toFront first, the
                // child won't actually stay on top of us all the time.
                toFront();
                mChild.toFront();
            }
        }
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
        setLocation(x, y);
    }

    /** set location by the upper right cornet instead of the (normal) upper left corner */
    public void setUpperRightCorner(int rightX, int y) {
        setLocation(rightX - getWidth(), y);
    }

    public void setLowerLeftCorner(int x, int lowerY) {
        setLocation(x, lowerY - getHeight());
    }

    public void setLowerRightCorner(int lowerX, int lowerY) {
        setLocation(lowerX - getWidth(), lowerY - getHeight());
    }
    
    private void out(Object o) {
        Log.debug("(" + mTitleName + ") " + o);
//         String s = "DockWindow " + (""+System.currentTimeMillis()).substring(9);
//         s += " [" + mTitle + "]";
        
//         System.err.println(s + " " + (o==null?"null":o.toString()));
    }

    public String toString() {
        //String s = "DockWindow[" + mTitle;
        String s = "[" + mTitleName;
        if (true || mChild == null)
            return s + "]";
        else
            return s + " ->" + mChild.mTitleName + "]";
    }

    private static class ScrollableWidthTracker extends JPanel implements Scrollable {
        private JComponent tracked;
        
        ScrollableWidthTracker(JComponent wrapped, JComponent tracked) {
            super(new BorderLayout());
            if (DEBUG.BOXES) setBorder(new LineBorder(Color.red, 4));
            add(wrapped);
            this.tracked = tracked;
        }

        public Dimension getPreferredScrollableViewportSize() {
            Dimension d = tracked.getSize(); // width is always same as tracked width
            d.height = getHeight(); // height is always whatever we've been laid out to (preferred size)
            //Dimension d = getSize();
            //d.height = tracked.getHeight();
            if (DEBUG.DOCK || DEBUG.SCROLL) Log.debug(GUI.name(tracked) + " viewportSize " + GUI.name(d));
            return d;
        }

        /** clicking on the up/down arrows of the scroll bar use this */
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 8;
        }
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }
    
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    
    /** The content-pane for the Window: has the window border, contains
        the title and the widget content panel (which holds the widget border) */
    private class ContentPane extends JPanel
    {
        private JPanel mContent = new JPanel(true);
        private TitlePanel mTitle;
        private boolean isVertical = false;

        private boolean gradientBG = false;

        private Object titleConstraints = BorderLayout.NORTH;
        private JScrollPane mScroller;

        private Object contentConstraints;
        
        public ContentPane(String title, boolean asToolbar)
        {
            mContent.setName(title + ".dockContent");
            if (true||asToolbar) {
                setLayout(new BorderLayout());

                if (asToolbar)
                    titleConstraints = BorderLayout.WEST;
                else
                    titleConstraints = BorderLayout.NORTH;
                contentConstraints = BorderLayout.CENTER;
            } else {
                setLayout(new GridBagLayout());
                
                GridBagConstraints c = new GridBagConstraints();
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                c.gridx = 0;
                
                c.gridy = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                titleConstraints = c.clone();

                c.gridy = 1;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 1;
                contentConstraints = c;
            }
            
            // Apparently, max bounds not respected by BorderLayout: try GridBag
            // pref size is respected, but then it sets *everything* to max size.
            // Okay, BoxLayout and not even freakin GridBag is handling this...
            
//             Rectangle max = GUI.getMaximumWindowBounds();
//             mContent.
//                 setMaximumSize(new Dimension(max.width, max.height-100));
//                 setMaximumSize(new Dimension(max.width, max.height-100));
                //setPreferredSize(new Dimension(max.width, max.height-100));

            if (isMac && isToolbar)
            	setBorder(BorderFactory.createLineBorder(Color.black));
            else
            	setBorder(getWindowBorder());
            
            installTitlePanel(title, asToolbar);
            add(mContent, contentConstraints);

            mContent.setLayout(new BorderLayout());

            // need to make sure the background is set
            // so that if there is a bevel-border, it'll
            // do the right thing.
            //setBackground(GUI.getVueColor());

            //mContent.setBackground(Color.green);
            mContent.setOpaque(false);
        }
		public JScrollPane getScroller()
		{
			return mScroller;
		}
        //public void validate() { out("validate"); super.validate(); }
        public void doLayout() {


            if (false && !mWindowDragUnderway && !isRolledUp()) {
                if (DEBUG.DOCK && DEBUG.SCROLL) GUI.dumpSizes(this, "doLayout");
                
                int height = getHeight();
                int prefHeight = Math.max(getPreferredSize().height, getMinimumSize().height);
                prefHeight = Math.min(prefHeight, GUI.GScreenHeight);
                
                if (height != prefHeight)
                    setHeight(prefHeight);
            }
            
            super.doLayout();
        }

        /*
        public _ContentPane(String title, boolean asToolbar)
        {
            // requesting double-buffering doesn't do squat to stop resize flashing on MacOSX
            super(true);
            mContent.setName(title + ".dockContent");
            setLayout(new BorderLayout());
            setBorder(getWindowBorder());
            installTitlePanel(title, asToolbar);
            add(mContent, BorderLayout.CENTER);
            mContent.setLayout(new BorderLayout());

            // need to make sure the background is set
            // so that if there is a bevel-border, it'll
            // do the right thing.
            //setBackground(GUI.getVueColor());

            //mContent.setBackground(Color.green);
            mContent.setOpaque(false);
        }
        */

        void setCloseButtonVisible(boolean visible) {
            if (mTitle != null)
                mTitle.setCloseButtonVisible(visible);
        }

        private void changeAll(JComponent root) {
            
            new EventRaiser<JComponent>(this, JComponent.class) {
                protected void visit(Component c) {

                    if (DEBUG.INIT) {
                        if (targetClass.isInstance(c)) {
                            System.out.format("\tDockWindow(%s) making transparent: ", DockWindow.this.mTitleName);
                            dispatchSafely(c);
                        } else
                            System.out.print("              ");
                        eoutln(GUI.name(c));
                    } else {
                        super.visit(c);
                    }
                }
                public void dispatch(JComponent c) {
                    if (c instanceof javax.swing.text.JTextComponent)
                        return;
                    //if (target instanceof javax.swing.AbstractButton)
                    //return;

                    // apparently can't make a JTabbedPane transparent...
                    
                    c.setBackground(null);
                    c.setOpaque(false);
                    //c.setBackground(Color.red);
                }
            }.raiseStartingAt(root);
            
        }

        void setWidget(JComponent widget, boolean scrolled, boolean scrollAlways) {
            mContent.removeAll();

            //if (GUI.isMacBrushedMetal() && isToolbar)
            if (isToolbar) {
                //gradientBG = true;
                changeAll(widget);
            }

            mContent.setBorder(getContentBorder(widget));

            if (scrolled && mScroller == null) {
                mScroller = new JScrollPane(null,
                                            scrollAlways ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                mScroller.setOpaque(false);
                mScroller.getViewport().setOpaque(false);
                mScroller.setBorder(null);
                mContent.add(mScroller, BorderLayout.CENTER);
                if (DEBUG.BOXES) mScroller.setBorder(new LineBorder(Color.green, 4));
            }

            if (mScroller != null) {

                mScroller.setViewportView(new ScrollableWidthTracker(widget, mContent));
                //mScroller.setViewportView(widget);
                
                //JPanel p = new JPanel(new BorderLayout());
                //p.add(widget);
                //p.setBorder(new LineBorder(Color.red));
                //mScroller.setViewportView(p);
                //mScroller.setViewportView(widget);
            } else {
                mContent.add(widget, BorderLayout.CENTER);
            }

        }

        public void XaddNotify() {
            changeAll(mContent);
            super.addNotify();
            changeAll(mContent);
        }
            
        

        public void Xpaint(Graphics g) {
            //out("paint");
            //setOpaque(true);
            changeAll(mContent);
            super.paint(g);
        }

        public void paintComponent(Graphics g) {
            if (gradientBG)
                paintBackgroundGradient(g);
            else
                super.paintComponent(g);
        }
        
        private void paintBackgroundGradient(Graphics g) {
            //out("paintComponent");
            GradientPaint gp;
            if (false)
            gp = new GradientPaint(0,             0, TopGradientColor,
                                   0, getHeight()/2, BottomGradientColor, true);
            gp = new GradientPaint(0,             0, TopGradientColor,
                                   0,   getHeight(), BottomGradientColor);

                
            ((Graphics2D)g).setPaint(gp);
            //g.setColor(Color.blue);
            g.fillRect(0,0, getWidth(), getHeight());
            //super.paintComponent(g);

            if (false && isToolbar) {
                g.setColor(Color.lightGray);
                g.drawLine(0, 0, getWidth(),0);
            }
            
        }

        public void setVerticalTitle(boolean vertical) {
            if (mTitle == null)
                return;

            if (isVertical == vertical)
                return;

            isVertical = vertical;
            
            if (vertical) {
                //setBorder(new LineBorder(Color.gray));
                setBorder(new LineBorder(Color.lightGray));
                //setBorder(null);
                mTitle.setVertical(true);
                add(mTitle, BorderLayout.CENTER);
            } else {
                setBorder(getWindowBorder());
                remove(mTitle);
                mTitle.setVertical(false);
                add(mTitle, BorderLayout.NORTH);
            }
        }
            
        

        private void installTitlePanel(String title, boolean asToolbar) {
            mTitle = new TitlePanel(title);
            if (asToolbar)
                add(new Gripper(), BorderLayout.WEST);
            else
                add(mTitle, titleConstraints);
        }

        /*
        public boolean processKeyBindingUp(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            return super.processKeyBinding(ks, e, condition, pressed);
        }

        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            return GUI.processKeyBindingToMenuBar(this, ks, e, condition, pressed);
        }
        */
        
        
    }

    //private class Gripper extends javax.swing.JPanel {
    private class Gripper extends javax.swing.Box {
        private JComponent closeButton;
        
        Gripper() {
            super(BoxLayout.Y_AXIS);
            //super(new BorderLayout()); // close-button expands to fill whole gripper...
            //super(null);
            setName(DockWindow.this.getName());
            setPreferredSize(new Dimension(16,-1));
            if (true)
                setOpaque(false);
            else
                setBackground(Color.darkGray); // looks like crap
            //setBackground(sMidGradientColor); // looks like crap

            closeButton = new CloseButton(DockWindow.this);

            closeButton.setBorder(new EmptyBorder(2,1,2,0));

            //add(Box.createVerticalGlue());
            if (showCloseBtn)
            	add(closeButton);
            //closeButton.setLocation(1,1);
            //add(Box.createVerticalGlue());
            //add(closeButton, BorderLayout.CENTER);

            /*

              // Without an opposite component on mouse enter/exit events,
              // we've no idea if on exit it's actually entering the closeButton,
              // in which case we do NOT want to make it go invisible.
              // We could make this work by using invokeLater on the
              // exit, and if when it runs, the close button has gotten
              // a mouse-enter, then don't make it invisible.

            closeButton.setVisible(false);
            addMouseListener(new tufts.vue.MouseAdapter(this) {
                    public void mouseEntered(MouseEvent e) {
                        //closeButton.setVisible(true);
                    }
                    public void mouseExited(MouseEvent e) {
                        //closeButton.setVisible(false);
                    }
                });
            */
               
        }

        public void paintComponent(Graphics g) {
            if (DEBUG.BOXES) {
                g.setColor(Color.green);
                g.drawRect(0,0, getWidth()-1, getHeight()-1);
            }
            if (true)
                paintGripper((Graphics2D)g);
            else
                super.paintComponent(g);
        }

        private void paintGripper(Graphics2D g)
        {
        	final int height = getHeight();
        	final int left = 0;
            final int right = getWidth();
        	final int width = right - left;
        	GradientPaint mGradient = new GradientPaint(0, 0,BottomGradientColor,
                     width,           0, TopGradientColor  );         
         
        	
            ((Graphics2D)g).setPaint(mGradient);

            g.fillRect(0,0, width, height);
            g.setColor(new Color(128,128,128));
            g.drawLine(right-1, 0, right-1, height);
            /*final int height = getHeight();
            final int left = 0;
            final int right = getWidth();
            final int width = right - left;

            final int yinc = 5; // if divides evenly into ToolbarHeight, will texture when stacked

            for (int i = 0, y = 0; i < height; i++) {
                g.setColor(Color.black);
                g.drawLine(left,y-width, right,y);
                g.setColor(Color.white);
                g.drawLine(left,(y-width)-1, right,y-1);
                y += yinc;
            }*/
            /*
            for (int i = 0, y = 0; i < height; i++) {
                g.setColor(Color.black);
                g.drawLine(left,y, right,y-width);
                g.setColor(Color.white);
                g.drawLine(left,y-1, right,(y-width)-1);
                y += yinc;
            }
            */
            /*
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0, y = 0; i < height; i++) {
                g.setColor(Color.black);
                g.drawLine(left,y-width, right,y);
                //g.setColor(Color.white);
                //g.drawLine(left,(y-width)-1, right,y-1);
                y += yinc;
            }
            for (int i = 0, y = 0; i < height; i++) {
                g.setColor(Color.black);
                g.drawLine(left,y, right,y-width);
                //g.setColor(Color.white);
                //g.drawLine(left,y-1, right,(y-width)-1);
                y += yinc;
            }
            */
         /*   if (false) {
                //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // draw left & right border
                g.setColor(Color.lightGray);
                g.drawLine(0,0, 0,height);
                g.drawLine(width-1,0, width-1,height);
            }*/
        }
        
    }
    

    // TODO: allow for info in the title panel: e.g., Panner shows zoom or
    // Font shows current selected font v.s. the font we'll "apply"???

    private class TitlePanel extends javax.swing.Box {
        
        private CloseButton mCloseButton;
        private GradientPaint mGradient;
        private JLabel mLabel;
     //   private JLabel mOpenLabel; // for open/close icon
        private boolean isVertical = false;
        private MenuButton mMenuButton; // null of has no menu

        //private final Icon DownArrow = GUI.getIcon("DockDownArrow.gif");
        //private final Icon RightArrow = GUI.getIcon("DockRightArrow.gif");
        
        TitlePanel(String title)
        {
            //setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            super(BoxLayout.X_AXIS);
            setName(title);
            setOpaque(true);
            setPreferredSize(new Dimension(0, TitleHeight));

            /*
            if (!isMac) {
                //if (isMac) setBackground(SystemColor.control);
                setBackground(SystemColor.activeCaption);
                //setBorder(new LineBorder(SystemColor.activeCaptionBorder));
             }
            */
            //if (!isMacAqua) setBorder(new LineBorder(GUI.getVueColor()));

             if (title == null)
                 title = "";

             mLabel = new tufts.Util.JLabelAA(title);
             //if (TitleFont.getSize() < 11)
             //mLabel.setBorder(new EmptyBorder(2,0,0,0)); // t,l,b,r
             //else
             //mLabel.setBorder(new EmptyBorder(1,0,0,0)); // t,l,b,r
             // FYI, raise the label raises the icon also...
             mLabel.setFont(TitleFont);
             mLabel.setForeground(VueResources.getColor("gui.dockWindow.title.foreground",
                                                        SystemColor.activeCaptionText));

             Color iconColor = VueResources.getColor("gui.dockWindow.title.disclosureIcon.color",
                                                     isMacAqua ? Color.darkGray : SystemColor.activeCaptionText);

             mCloseButton = new CloseButton(DockWindow.this);

          /*   mOpenLabel = new GUI.IconicLabel(DownArrowChar,
                                              16, // point-size
                                              iconColor,
                                              15, // fixed width
                                              TitleHeight); // fixed height
            */         
             //if (isMacAqua)
           //      mOpenLabel.setBorder(new EmptyBorder(0,0,1,0)); // t,l,b,r
             
             if (DEBUG.BOXES) {
                 mLabel.setBackground(Color.yellow);
                 mLabel.setOpaque(true);
             }
             
             //JLabel helpButton = new JLabel(GUI.getIcon("btn_help_top.gif"));
             // todo for Melanie: new icons should be appearing in gui/icons
             VueLabel helpButton = new VueLabel(VueResources.getImageIconResource("/tufts/vue/images/btn_help_top.gif"));
             //helpButton.setToolTipText("Help Text");
             
             String helpText = VueResources.getString("dockWindow." + getName().replaceAll(" ","") + ".helpText");
             if (helpText != null)
                 helpButton.setToolTipText(helpText);
                          
             if (isMacAqua) {
                 // close button at left
                 add(Box.createHorizontalStrut(6));
                 add(mCloseButton);
                 add(Box.createHorizontalStrut(4));
               //  add(mOpenLabel);
                 add(mLabel);
                 add(Box.createGlue());
                 if (helpText != null)
                	 add(helpButton);
             } else {
                 // close button at right
                 add(Box.createHorizontalStrut(8));
            //     add(mOpenLabel);
              //   add(Box.createHorizontalStrut(2));
                 add(mLabel);
                 add(Box.createGlue());
                 if (helpText != null)
                	 add(helpButton);
                 add(Box.createHorizontalStrut(3));
                 add(mCloseButton);
             }

             add(Box.createHorizontalStrut(2));
             
             if (isGradientTitle)
                 installGradient(false);
        }

        void setTitle(String title) {
            mLabel.setText(title);
        }

        void showAsOpen(boolean open) {
            //mOpenLabel.setIcon(open ? DownArrow : RightArrow);
            //mOpenLabel.setText(open ? DownArrow : RightArrow);
         //   mOpenLabel.setText(open ? ""+DownArrowChar : ""+RightArrowChar);
        }

        void setCloseButtonVisible(boolean visible) {
            mCloseButton.setHidden(!visible);
        }

        void setMenuActions(Action[] actions) {
            int count = 0;
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    if (actions[i] != null)
                        count++;
                }
            }

            if (count == 0) {
                if (mMenuButton != null) {
                    remove(mMenuButton);
                    mMenuButton = null;
                }
            } else {
                if (mMenuButton == null) {
                    mMenuButton = new MenuButton(actions);
                    add(mMenuButton, -1);
                } else
                    mMenuButton.setMenuActions(actions);
            }
        }

        void setVertical(boolean vertical) {
            if (isGradientTitle)
                installGradient(vertical);
            mCloseButton.setVisible(!vertical);
            isVertical = vertical;
        }
        
        private void installGradient(boolean vertical) {
            if (vertical)
                mGradient = new GradientPaint(getHeight(), 0, TopGradientColor,
                                              0,           0, false ? Color.gray : BottomGradientColor);
            else
                mGradient = new GradientPaint(0,           0, TopGradientColor,
                                              0, TitleHeight, BottomGradientColor);

            // reversed gradient
            if (false) mGradient = new GradientPaint(0,           0, BottomGradientColor,
                                                     0, TitleHeight, TopGradientColor);
        }

        public void paint(Graphics g) {
            if (!isMac) {
                // this is on by default on the mac
                Graphics2D g2 = (Graphics2D) g;
                //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            super.paint(g);
        }

        public void paintComponent(Graphics g)
        {
            if (DEBUG.PAINT) out("TitlePanel.paintComponent");
            //if (!isMac || !DEBUG.DOCK) // for tufts.macosx.MacTest
            paintGradientEtc(g);
        }
        
        private void paintGradientEtc(Graphics g)
        {
            final int width = getWidth();
            final int height = getHeight();

            if (isGradientTitle)
                ((Graphics2D)g).setPaint(mGradient);
            else
                g.setColor(getBackground());

            g.fillRect(0,0, width, height);

            // This add's left + right edge border when docked if we want.
            if (!mHasWindowShadow && isDocked() && getWindowBorder() == null) {
                //g.setColor(sBottomEdgeColor);
                g.setColor(BottomGradientColor);
                if (false) {
                    // draw left edge
                    g.drawLine(0, 0, 0, height-1);
                }
                //if (mWindowDragUnderway || mDockNext == null || !mDockNext.isRolledUp()) {
                if (mDockNext == null) {
                    // If right (next) sibling is unrolled, it will have a shadow
                    // that could serve as the visual border. However, if the
                    // the left (prev) window is above it, it will obscure
                    // the shadow and the window's will "merge" at the title bar.
                    // [NO LONGER TRUE: left component now always shrinks, and
                    // unrolled always have shadow ]
                    g.drawLine(width-1, 0, width-1, height-1);
                }
            }

            /*
              This looks great, but more so during testing when no content in the DockWindow.
              
            if (isMac && !isVertical && !isMacAquaMetal && !isRolledUp()) {
                g.setColor(sBottomEdgeColor);
                g.drawLine(0, height-1, width, height-1);
            }
            */


            if (isVertical) {
                g.setColor(mLabel.getForeground());
                g.setFont(mLabel.getFont());
                ((Graphics2D)g).rotate(Math.PI / 2);
                g.drawString(mLabel.getText(), 4, -4);
            }
        }
        
    }


    private class MenuButton extends JLabel implements MouseListener 
    {


        MenuButton(Action[] actions) 
        {
        	super();        	
        	setFocusable(true);
        	setIcon(VueResources.getIcon("dockWindow.panner.menu.raw"));        	
        
            setName(DockWindow.this.getName());
            setFont(new Font("Arial", Font.PLAIN, 18));
        
            Insets borderInsets = new Insets(1,1,1,1);
        
            if (DEBUG.BOXES) {
                setBorder(new MatteBorder(borderInsets, Color.orange));
                setBackground(Color.red);
                setOpaque(true);
            } else {
                // using an empty border allows mouse over the border gap
                // to also activate us for rollover
                setBorder(new EmptyBorder(borderInsets));
            }

            if (false) setMaximumSize(new Dimension(30,TitleHeight));

            setMenuActions(actions);
        }
            
            
    		public void mouseEntered(MouseEvent arg0) {    			
    			setIcon(VueResources.getImageIcon("dockWindow.panner.menu.hover"));
    		}

    		public void mouseExited(MouseEvent arg0) {
    			setIcon(VueResources.getImageIcon("dockWindow.panner.menu.raw"));    			
    			
    		}       


        void setMenuActions(Action[] actions)
        {
            clearMenuActions();
            addMouseListener(this);
            new GUI.PopupMenuHandler(this, GUI.buildMenu(actions)) {
                /*
                  public void mouseEntered(MouseEvent e) {
                  setForeground(isMacAqua ? Color.black : Color.white);
                  }
                  public void mouseExited(MouseEvent e) {
                  //setForeground(isMacAqua ? Color.gray : SystemColor.activeCaption.brighter());
                        setForeground(inactiveColor);
                        }
                */
                
                public int getMenuX(Component c) { return c.getWidth(); }
                public int getMenuY(Component c) { return -getY(); } // 0 in parent
            };

            repaint();
        }

        private void clearMenuActions() {
            MouseListener[] ml = getMouseListeners();
            for (int i = 0; i < ml.length; i++) {
                if (ml[i] instanceof GUI.PopupMenuHandler)
                    removeMouseListener(ml[i]);
            }
        }


		public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}


		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}


		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}	

    }
    
    private static class CloseButton extends JLabel {
            
        private final Icon iconClose;
        private final Icon iconOver;

        private boolean visible = true;
        
        public CloseButton(final DockWindow dockWindow) {

            setName(dockWindow.getName());

            if (dockWindow.isToolbar) {
                iconClose = VueResources.getIcon("gui.dockWindow.closeIcon");
                iconOver = VueResources.getIcon("gui.dockWindow.closeIcon.over");
            } else {
                iconClose = VueResources.getIcon("gui.dockWindow.closeIcon");
                iconOver = VueResources.getIcon("gui.dockWindow.closeIcon.over");
            }

            setIcon(iconClose);
            
//             if (isMacAqua)
//                 setIcon(iconBlank);
//             else
//                 setIcon(new SquareCloseIcon());

            addMouseListener(new tufts.vue.MouseAdapter(getClass()) {
                    public void mouseEntered(MouseEvent e) { setRollover(true); }
                    public void mouseExited(MouseEvent e) { setRollover(false); }
                    public void mouseClicked(MouseEvent e) {
                        if (visible && e.getClickCount() != 0 && e.getClickCount() != 2)
                            dockWindow.dismiss();
                    }
                });

        }

        public void paint(Graphics g) {
            if (visible)
                super.paint(g);
        }

        public void setHidden(boolean hidden) {
            this.visible = !hidden;
            //setEnabled(this.visible);
        }

        private void setRollover(boolean lit) {
            setIcon(lit ? iconOver : iconClose);
//             if (isMacAqua)
//                 setIcon(lit ? iconClose : iconBlank);
//             else
//                 ((SquareCloseIcon)getIcon()).setRollover(lit);
        }

        private class SquareCloseIcon implements Icon {
            private final int iconSize;
            private final int iconWidth;
            private final int iconHeight;
            private final java.awt.BasicStroke X_STROKE;

            private boolean isRollover = false;

            private final Border border = BorderFactory.createRaisedBevelBorder();
            
            public SquareCloseIcon() {
                X_STROKE = new java.awt.BasicStroke(1.3f);
                iconSize = TitleHeight - 5;

                iconWidth = iconSize + 1;
                iconHeight = iconSize + 1;
                
                /*
                if (isMacAqua) {
                    iconWidth = iconSize + 3;
                    iconHeight = iconSize + 1;
                } else {
                    iconWidth = iconSize + 2;
                    if (isMac)
                        iconHeight = iconSize + 3;
                    else
                        iconHeight = iconSize + 2;
                }
                */
            }

            void setRollover(boolean t) {
                isRollover = t;
                repaint();
            }
            
            public int getIconWidth() { return iconWidth; }
            public int getIconHeight() { return iconHeight; }
            
            public void paintIcon(Component c, Graphics g, int x, int y) {
                int xoff = x-1;
                int yoff = y+1;
                if (false && isMacAquaMetal == false) {
                    //g.setColor(SystemColor.activeCaption);
                    // we fill in case window is so narrow that title text
                    // would appear under this icon: we don't want to see that.
                    //g.setColor(c.getBackground().brighter());
                    g.setColor(SystemColor.control);
                    //g.setColor(Color.green);
                    g.fillRect(xoff, yoff, iconSize,iconSize);
                }
                //g.setColor(Color.black);
                //g.drawRect(xoff, yoff, iconSize,iconSize);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                                 RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isRollover ? Color.red : Color.lightGray);
                //g.drawOval(xoff, yoff, iconSize,iconSize);
                ((Graphics2D)g).setStroke(X_STROKE);
                int inset = 2;
                int len = iconSize - (inset+1);
                // UL to LR
                g.drawLine(xoff+inset, yoff+inset, xoff+len, yoff+len);
                // LL to UR
                g.drawLine(xoff+inset, yoff+len, xoff+len, yoff+inset);

                //border.paintBorder(c, g, x, y, iconSize, iconSize);
            }
            
        }

        private String out(Color c) {
            return "color[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
        }
            

    }
    
    private static class ResizeCorner extends javax.swing.JComponent {
        final Object mCorner;
        ResizeCorner(DockWindow mouseListener, Object corner) {

            mCorner = corner;
            setName(corner.toString());

            if (mCorner == SOUTH_EAST)
                setSize(ResizeCornerSize, ResizeCornerSize);
            else
                setSize(ResizeCornerSize/2, ResizeCornerSize/2);
            
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseListener);

            int cursorID = Cursor.SE_RESIZE_CURSOR;

            if (mCorner == NORTH_EAST)
                cursorID = Cursor.NE_RESIZE_CURSOR;
            else if (mCorner == SOUTH_WEST)
                cursorID = Cursor.SW_RESIZE_CURSOR;
            else if (mCorner == NORTH_WEST)
                cursorID = Cursor.NW_RESIZE_CURSOR;

            // This only works if Window focusable state is true, which
            // will not be true if we're using java 1.5 alwaysOnTop w/forced focus.
            setCursor(Cursor.getPredefinedCursor(cursorID));

            if (isMacAqua && !isMacAquaMetal)
                setForeground(Color.lightGray);
            else
                setForeground(Color.gray);
        }


        public void paintComponent(Graphics g) {
            
            if (mCorner == SOUTH_EAST)
                paintResizeCorner((Graphics2D)g);
            
            if (DEBUG.BOXES) {
                g.setColor(Color.green);
                g.drawRect(0,0, getWidth()-1, getHeight()-1);
            }
        }

        private void paintResizeCorner(Graphics2D g)
        {
            if (DEBUG.PAINT) System.out.println("ResizeCorner paint " + g.getClipBounds());
            
            int width = getWidth();
            int right = width - 1;
            int bottom = getHeight() - 1;

            int x = 0, y = 0;
            
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.setColor(getForeground());
            for (int i = 0; i < width/2; i++) {
                g.drawLine(x,bottom, right,y);
                x += 2;
                y += 2;
            }
        }
        
    }
    
    public static Frame getHiddenFrame() {
        Util.printStackTrace();
        if (HiddenParentFrame == null) {
            HiddenParentFrame = new JFrame() {
                    public void show() {}

                    // so isFocusableWindow in children can return true even tho we're invisible
                    public boolean isShowing() { return true; }

                    // An isVisible() that returns true is the key to allowing this to
                    // serve as a top-level focusable parent.  Otherwise, if this is the
                    // ONLY frame "on screen", keyboard input is not permitted anywhere,
                    // even to forced focus components (we get system beeps).  Calling
                    // setVisible(true) won't work, because it won't actually go visible
                    // if show() is doing nothing.
                    // UNFORUNATELY, THIS ALSO MAKES IT VISIBLE ON THE SCREEN!
                    //public boolean isVisible() { return true; }

                    // make sure is never preferred window group for handing focus to
                    // CANNOT do this or an installed menu-bar won't work
                    //public boolean getFocusableWindowState() { return false; } // doesn't help
                    
                    //public boolean isFocusable() { return true; }
                    //public boolean getFocusableWindowState() { return true; }
                    
                    public String toString() { return getName(); }
                };
            HiddenParentFrame.setName("(VUE Hidden Dock Parent)");

            // If we have a menu-bar attached, it must be focusable for the menu bar to work.
            // (perhaps also now that it's returning isVisible() == true?
            //HiddenParentFrame.setFocusableWindowState(true);
            
            // Don't need to attach MenuBar now that ToolWindow's do NOT "officially" take the focus at all --
            // the active frame with it's attached menu bar stays active.
            //HiddenParentFrame.setJMenuBar(new tufts.vue.gui.VueMenuBar());
            
            // fortunately, does NOT need to be visible for menu bar to work
            //HiddenParentFrame.setVisible(true);
            HiddenParentFrame.setVisible(false);
        }

        return HiddenParentFrame;
    }
    

    
    public static DockWindow getTestWindow() {
        DockWindow dw = new DockWindow("Interactive");
        JPanel p = new JPanel();
        JLabel l = new JLabel("I am a label");
        p.add(l);
        JTextField tf = new JTextField(5);
        tf.setText("text");
        p.add(tf);
        p.add(new JButton("foo"));
        dw.add(p);
        dw.setVisible(true);
        return dw;
    }
    
    public static void main(String args[]) {

        VUE.init(args);
        //DEBUG.BOXES=true;
        //DEBUG.KEYS=true;
        
        
        if (false) {

            //new Frame("A Frame").show();
            
            DockWindow dw = getTestWindow();
            
            dw.setLocationRelativeTo(null);
        }

        Window owner = null;
        if (true) {
            owner = new Frame("A Frame");
            owner.setVisible(true);
        }

        //new Frame("Frame Two").show();

        //owner = null;

        final DockWindow win1 = new DockWindow("Dock 1", owner, null, false);
        //win1.add(new FontPropertyPanel());
        //win1.setLocationRelativeTo(null); // center's on screen
        
        win1.setMenuActions(new Action[] {
                new tufts.vue.VueAction("Test 1"),
                null,
                new tufts.vue.VueAction("Activate Wait Cursor") {
                    public void act() {
                        System.out.println("wait cursor");
                        JRootPane root = SwingUtilities.getRootPane(win1);
                        System.out.println("JRootPane: " + root);
                        root.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        //win1.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    }
                    public boolean enabled() { return true; }
                },
                new tufts.vue.VueAction("Clear Wait Cursor") {
                    public void act() {
                        System.out.println("clear wait cursor");
                        win1.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                    public boolean enabled() { return true; }
                },
            });
        
        //win1.add(new WidgetBox("Folders", new JLabel("Hello World")));

        WidgetStack stack = new WidgetStack();
        stack.addPane("TUFTS Digital Library");
        stack.addPane("ArtStor");
        stack.addPane("My Computer");
        stack.addPane("My Picture", new JLabel(new ImageIcon(VueResources.getURL("splashScreen"))));

         if (false) {
            JScrollPane sp = new JScrollPane(stack);
            //sp.setBorder(new LineBorder(Color.red));
            // clear default 1 pixel white border
            sp.setBorder(null);
            win1.setContent(sp);
         } else {
             //   win1.setContent(stack);
             win1.setContent(new JLabel("foo"));
         }
        
        win1.setVisible(true);
        
        DockWindow win2 = new DockWindow("Dock 2", owner);
        //win2.add(new FontPropertyPanel());
        win2.setLocationRelativeTo(null); // center's on screen
        win2.setFocusableWindowState(true);
        win2.setVisible(true);
                
        /*
        if (false) {

            DockWindow win3 = new DockWindow("Dock 3", owner);
            DockWindow win4 = new DockWindow("Dock 4", owner);
            //DockWindow win3 = new DockWindow("King Objumpy");
            //DockWindow win4 = new DockWindow("Canvas");
            
            win1.setChild(win2);
            //win2.setChild(win3);
            win3.setChild(win4);
            
            //win2.setVisible(true);
            win3.setVisible(true);
            win4.setVisible(true);
            
        }
        */
            
        // called indirectly so this compiles in java 1.4
        //tufts.Util.invoke(tw0.mWindow, "setAlwaysOnTop", Boolean.TRUE);
        //tufts.Util.invoke(tw1.mWindow, "setAlwaysOnTop", Boolean.TRUE);
            

    }

    
    
}
