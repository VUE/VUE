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

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ToolWindow
 *
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
 */

//public class ToolWindow extends JWindow
public class ToolWindow extends JFrame // will need on mac version, so convert to factory
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

    private final boolean managedTitleBar;
    
    public ToolWindow(String title, Frame owner)
    {
        //super(owner);
        managedTitleBar = true;
        setUndecorated(managedTitleBar);
        //if (((Object)this) instanceof JWindow)
        //managedTitleBar = true;
        //if (owner instanceof JFrame) setRootPane(((JFrame)owner).getRootPane()); // no help getting menu bars shared on mac
        
        this.mTitle = title;
        setName(title);
        if (managedTitleBar) {
            addMouseListener(this);
            addMouseMotionListener(this);
            addKeyListener(this);
        }
        //setFocusable(false);
        addFocusListener(this); // should never see...
        if (debug) out("contentPane=" + getContentPane());
        mContentPane = new ContentPane(mTitle);
        //mContentPane.setFocusable(false);
        //getContentPane().add(mContentPane);
        setContentPane(mContentPane);
        // todo checkout: setting content-pane v.s. adding to it may affect glass pane?
        // seems to be working fine now...

        //---------------------------------
        // set up glass pane
        Component gp = new GlassPane();
        setGlassPane(gp);
        gp.setVisible(true);


        pack();
        if (DEBUG.Enabled) out("constructed.");
        
        //setLocationRelativeTo(owner);
        //setFocusableWindowState(false); // nothing can get input at all...
        //setFocusable(false); doesn't appear to do anything; todo: play with later & sub-panels

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
    }

    // todo: problem: we can see this, but the global Command-W action is ALSO
    // getting activated!
    public void keyPressed(KeyEvent e)
    {
        if (DEBUG.KEYS) out("[" + e.paramString() + "]");

        if (e.getKeyCode() == KeyEvent.VK_W && (e.getModifiers() & Actions.COMMAND) != 0)
            hide();
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
    public void setVisible(boolean show) {
        if (DEBUG.FOCUS) out("setVisible " + show);
        if (show && isRolledUp())
            setRolledUp(false);
        else if (!show) {
            if (focusReturn != null) {
                if (DEBUG.FOCUS) out("returning focus to " + focusReturn);
                focusReturn.requestFocus();
            }
        }
        super.setVisible(show);
    }

    /** look for a tabbed pane within us with the given title, and select it */
    public void showTab(final String name) {
        // event raiser wasn't designed for this, but turns out
        // to be very convienent for it.
        EventRaiser e = new EventRaiser(this) {
                public Class getListenerClass() { return JTabbedPane.class; }
                void dispatch(Object pTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) pTabbedPane;
                    int i = tabbedPane.indexOfTab(name);
                    if (i >= 0)
                        tabbedPane.setSelectedIndex(i);
                }
            };
        e.deliverToChildren(this);
        setVisible(true);
    }

    public void addTool(JComponent c) {
        addTool(c, false);
    }
    
    public void addTool(JComponent c, boolean addBorder)
    {
        // todo: make it so can add more than one tool
        // -- probably use BoxLayout
        mContentPane.contentPanel.add(c, BorderLayout.CENTER);
        
        // this is hack till glass pane can redispatch mouse events so
        // that mouse listening tools don't disable the resize corner
        MouseListener[] ml = c.getMouseListeners();
        if (DEBUG.Enabled) out("added " + c + " mouseListeners=" + ml.length);
        if (addBorder || ml.length > 0) {
            if (DEBUG.Enabled)
                mContentPane.contentPanel.setBorder(new LineBorder(Color.lightGray, 5));
            else
                mContentPane.contentPanel.setBorder(new EmptyBorder(5,5,5,5));
        }
        pack();
    }

    public void add(JComponent c) {
        addTool(c);
    }
        

    /*
    public void X_paint(Graphics g)
    {
        // todo: better to actually NOT paint the title
        // as a jcomponent, in case our content panel
        // has an exception during rendering, we can
        // catch it here (around super.paint()) and
        // then still decorate the window.
        super.paint(g);
        int bottom = getHeight() - 1;
        int right = getWidth() - 1;
        int x = getWidth() - ResizeCornerSize;
        int y = getHeight() - ResizeCornerSize;
        g.setColor(Color.gray);
        for (int i = 0; i < ResizeCornerSize/2; i++) {
            g.drawLine(x,bottom, right,y);
            x += 2;
            y += 2;
        }
        g.setColor(SystemColor.control);
    }
    */

    public String getTitle() {
        return mTitle;
    }
    
    public void setSize(int width, int height)
    {
        //System.out.println("setSize " + width + "x" + height);
        if (width < ResizeCornerSize * 3)
            width = ResizeCornerSize * 3;
        if (height < TitleHeight + ResizeCornerSize)
            height = TitleHeight + ResizeCornerSize;
        super.setSize(width, height);
        validate();
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
    
    private void setRolledUp(boolean t) {
        if (isRolledUp == t)
            return;
        isRolledUp = t;
        if (isRolledUp) {
            savedSize = getSize();
            setSize(getWidth(), 0);
        } else {
            setSize(savedSize);
            //setSize(getPreferredSize());
        }
    }

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
        requestFocus(); // must do this to get key input
        //System.out.println(e);
        dragStart = e.getPoint();
        // todo: will need to check this on the glass pane
        // in case underlying panel is also grabbing
        // mouse events (and then redispatch)
        if (closeCornerHit(e))
            hide();
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
            setLocation(p);
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



    private class ContentPane extends JPanel
    {
        JPanel titlePanel;
        JPanel contentPanel = new JPanel();
        
        //JButton hideButton = null;
            
        public ContentPane(String title)
        {
            super(true);
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

            if (managedTitleBar)
                installTitlePanel(title);

            add(contentPanel, BorderLayout.CENTER);
        }

        private void installTitlePanel(String title)
        {
            /*
            if (hideButton != null) {
                hideButton.setFont(new Font("SansSerf", Font.BOLD, 7));
                hideButton.setMargin(new Insets(0,1,0,1));
                hideButton.setDefaultCapable(false);
                titlePanel.add(hideButton);
            }
            */
            titlePanel = new JPanel();
            titlePanel.setPreferredSize(new Dimension(0, TitleHeight));
            if (VueUtil.isMacAquaLookAndFeel()) {
                // Mac OS X Aqua L&F
                //titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                /*
                if (VueTheme.isMacMetalLAF()) {
                    // this doesn't work unless we're subclassed from a JFrame,
                    // and in that case we don't need to do it anyway.
                    // Bottom line: mac brushed metal look only applies to proper Frame's
                    titlePanel.setBackground(SystemColor.window);
                }
                */
                add(titlePanel, BorderLayout.NORTH);
                contentPanel.setBorder(new LineBorder(Color.gray));
            } else {
                setBorder(new BevelBorder(BevelBorder.RAISED));

                //hideButton.setBackground(SystemColor.activeCaption);
                //titlePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                //System.out.println("ActiveCaption=" + out(SystemColor.activeCaption));
                
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

            /*
            titlePanel.setLayout(new BorderLayout());
            if (title != null) {
                JLabel l = new JLabel(title);
                l.setFont(new Font("SansSerf", Font.PLAIN, 9));
                l.setForeground(SystemColor.activeCaptionText);
                l.setSize(l.getPreferredSize());
                titlePanel.add(l, BorderLayout.WEST);

                /*
                //System.out.println("lh=" + l.getHeight());
                int y = ((TitleHeight - l.getHeight())+1) / 2;
                if (VueUtil.isMacPlatform())
                    y++;
                l.setLocation(2, y);
                *
            }

            JButton b = new JButton(new CloseIcon());
            b.setPressedIcon(new CloseIcon(Color.gray));
            b.setRolloverIcon(new CloseIcon(Color.red));
            titlePanel.add(b, BorderLayout.EAST);
        */
            
                //b.setLocation(getWidth() - b.getWidth(), 0);
            
            if (title != null) {
                //titlePanel.setLayout(new FlowLayout());
                titlePanel.setLayout(null); // for manual layout
                JLabel l = new JLabel(title);
                l.setFont(TitleFont);
                l.setForeground(SystemColor.activeCaptionText);
                //l.setForeground(Color.darkGray);
                l.setSize(l.getPreferredSize());
                titlePanel.add(l);
                //System.out.println("lh=" + l.getHeight());
                int y = ((TitleHeight - l.getHeight())+1) / 2;
                if (VueUtil.isMacAquaLookAndFeel())
                    y++;
                l.setLocation(2, y);
                //if (hideButton != null)
                //hideButton.setLocation(50,0);
            }
        }

        private int iconSize;
        private int yoff;
        private int edgeInset;
        
        public void addNotify()
        {
            if (iconSize == 0) {
                iconSize = TitleHeight - (VueUtil.isMacAquaLookAndFeel() ? 4 : 5);
                yoff = VueUtil.isMacAquaLookAndFeel() ? 2 : 4;
                edgeInset = VueUtil.isMacAquaLookAndFeel() ? TitleHeight : TitleHeight+1;
            }
            super.addNotify();
            //System.out.println("hideButton=" + hideButton);
            //System.out.println("peer="+hideButton.getPeer());
        }

        public void paint(Graphics g) {
            //System.out.println("painting " + this);
            super.paint(g);
            if (managedTitleBar) {
                int xoff = getWidth() - edgeInset;
                if (false && VueUtil.isMacAquaLookAndFeel()) {
                    //macWindowClose.paintIcon(this, g, xoff, yoff);
                } else {
                    g.setColor(SystemColor.activeCaption);
                    if (!VueTheme.isMacMetalLAF())
                        g.fillRect(xoff, yoff, iconSize,iconSize);
                    g.setColor(SystemColor.activeCaptionText);
                    //g.setColor(SystemColor.activeCaption.brighter().brighter());
                    g.drawRect(xoff, yoff, iconSize,iconSize);
                    g.drawLine(xoff, yoff, xoff+iconSize,yoff+iconSize);
                    g.drawLine(xoff, yoff+iconSize, xoff+iconSize,yoff);
                }
            }
        }

        private String out(Color c) {
            return "color[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
        }

        /*
        class CloseIcon implements Icon
        {
            private Color color = Color.BLACK;
            private final int height = 5;
            private final int width = 5;
    
            public CloseIcon() {}
            public CloseIcon(Color color) { this.color = color; }
            public int getIconWidth() { return width; }
            public int getIconHeight() { return height; }
    
            public void paintIcon(Component c, Graphics g, int x, int y) {
                //Graphics2D g2d = (Graphics2D) g;
                g.setColor(color);
                x=y=0;
                g.drawRect(x, y, x+width, y+height);
            }
        }
        */
    }
    


    
    /**
     * We provide our own glass pane so that it can paint the resize corner.
     */
    private class GlassPane extends JComponent
    {
        public void paint(Graphics g) {
            if (!isRolledUp())
                paintResizeCorner((Graphics2D)g);
        }

        private void paintResizeCorner(Graphics2D g)
        {
            int w = getWidth();
            int h = getHeight();
            int right = w - 1;
            int bottom = h - 1;
            int x = w - ResizeCornerSize;
            int y = h - ResizeCornerSize;
            Color c = g.getColor();
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
            g.setColor(c);
            
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

          /*public MyGlassPane(AbstractButton aButton, 
                           JMenuBar menuBar,
                           Container contentPane) {
            CBListener listener = new CBListener(aButton, menuBar,
                                                 this, contentPane);
            addMouseListener(listener);
            addMouseMotionListener(listener);
        }*/
    }

    private static boolean debug = false;
    public static void main(String args[]) {
        debug=true;
        DEBUG.BOXES=true;
        DEBUG.KEYS=true;
        DEBUG.MOUSE=true;
        ToolWindow tw = new ToolWindow("Title", null);
        JPanel p = new JPanel();
        p.setBorder(new TitledBorder("Yippity Vue Tool"));
        JLabel l = new JLabel("I am a label");
        p.add(l);
        JTextField tf = new JTextField(5);
        tf.setText("text");
        tf.setEditable(true);//no need
        p.add(tf);
        tw.addTool(p);
        tw.setSize(200,200);
        tw.show();

        // why can't we get the tex field to respond to mouse??
        // must it have an action listener?
        tw.setFocusable(true);
        p.setFocusable(true);
        tf.setFocusable(true);
        
    }
    
    
}
