package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * We can use this class to create our own fancy floating tool windows.
 * Howver, this will mean providing & handling all our own decorations
 * -- can we somehow query the LookAndFeel to we can do something
 * compatible?
 * todo: put in layer that is BELOW pop-up menu layer -- this is appearing over menus!
 */

public class ToolWindow
    extends JWindow
    implements MouseListener, MouseMotionListener
{
    //final int TitleHeight = VueUtil.isMacPlatform() ? 11 : 13;
    //final int TitleHeight = 44;
    final int TitleHeight = 14;
    final Font TitleFont = new Font("SansSerf", Font.PLAIN, 10);
    final int ResizeCornerSize = 14;
    
    public static void main(String args[])
    {
        ToolWindow tw = new ToolWindow("Title", null);
        //tw.setSize(1024,768);// todo fixme hack: why Graphics being limted to first size??
        tw.setSize(200,200);
        tw.show();
    }
    
    private Point dragStart;
    private Dimension dragSizeStart; // tmp public hack

    /**handles opening and closing inspector*/
    private AbstractButton mDisplayButton = null;

    private boolean isRolledUp = false;
    private Dimension savedSize;

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

    class ToolPanel extends JPanel
    {
        JPanel titlePanel = new JPanel();
        JPanel contentPanel = new JPanel();
        
        //Component hideButton = new Button();
        //JButton hideButton = new JButton("X");
        JButton hideButton = null;
            
        
        String ctos(Color c)
        {
            return "color[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
        }


        public class CloseIcon implements Icon
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
            
        ToolPanel(String title)
        {
            // todo -- need to have at least title click-able
            // without this window grabbing focus so you can
            // at least drag these windows without main
            // losing focus.  Also, should be able to make
            // it so you can get focus and not have the parent
            // window go inactiveat all, which is happening
            // on OSX.

            setLayout(new BorderLayout());
            titlePanel.setPreferredSize(new Dimension(0, TitleHeight));
            contentPanel.setLayout(new BorderLayout());
            
            if (hideButton != null) {
                hideButton.setFont(new Font("SansSerf", Font.BOLD, 7));
                hideButton.setMargin(new Insets(0,1,0,1));
                hideButton.setDefaultCapable(false);
                titlePanel.add(hideButton);
            }
            
            if (VueUtil.isMacAquaLookAndFeel()) {
                // Mac OS X Aqua L&F
                //titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                add(titlePanel, BorderLayout.NORTH);
                contentPanel.setBorder(new LineBorder(Color.gray));
            } else {
                //hideButton.setBackground(SystemColor.activeCaption);
                //titlePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                //System.out.println("ActiveCaption=" + ctos(SystemColor.activeCaption));
                titlePanel.setBackground(SystemColor.activeCaption);
                titlePanel.setForeground(SystemColor.activeCaptionText);
                titlePanel.setBorder(new LineBorder(SystemColor.activeCaptionBorder));
                contentPanel.setBackground(SystemColor.control);
                //contentPanel.setBorder(new LineBorder(Color.green, 5));
                contentPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
                contentPanel.add(titlePanel, BorderLayout.NORTH);
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
                if (hideButton != null)
                    hideButton.setLocation(50,0);
            }

            add(contentPanel, BorderLayout.CENTER);
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
            int xoff = getWidth() - edgeInset;
            g.setColor(SystemColor.activeCaption);
            g.fillRect(xoff, yoff, iconSize,iconSize);
            g.setColor(SystemColor.activeCaptionText);
            g.drawRect(xoff, yoff, iconSize,iconSize);
            g.drawLine(xoff, yoff, xoff+iconSize,yoff+iconSize);
            g.drawLine(xoff, yoff+iconSize, xoff+iconSize,yoff);
        }
    }
    
    
    /**
     * We provide our own glass pane so that it can paint the resize corner.
     */
    class GlassPane extends JComponent {
    //class GlassPane extends Component {
        
        public void paint(Graphics g)
        {
            paintResizeCorner((Graphics2D)g);
        }
        
        public void X_paint(Graphics g)
        {
            g.setColor(Color.red);
            int w = getWidth();
            int h = getHeight();
            g.drawRect(w-10,h-10,w,h);
        }

        private void paintResizeCorner(Graphics2D g)
        {
            int w = getWidth();
            int h = getHeight();
            int right = w - 1;
            int bottom = h - 1;
            int x = w - ResizeCornerSize;
            int y = h - ResizeCornerSize;
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            //g.setStroke(VueConstants.STROKE_HALF);
            // attempt to deal with mac double-stroke(?) bug on last line drawn
            // could it possibly happen if our size is an odd integer?
            g.setColor(Color.gray);
            for (int i = 0; i < ResizeCornerSize/2; i++) {
                g.drawLine(x,bottom, right,y);
                x += 2;
                y += 2;
            }
            g.setColor(SystemColor.control);
        }

        public GlassPane()
        {
            //addMouseListener(new tufts.vue.MouseAdapter("GlassPane"));
            //setLayout(new FlowLayout());
            //add(new JLabel("foobie"));
        }

        /*
        public MyGlassPane(AbstractButton aButton, 
                           JMenuBar menuBar,
                           Container contentPane) {
            CBListener listener = new CBListener(aButton, menuBar,
                                                 this, contentPane);
            addMouseListener(listener);
            addMouseMotionListener(listener);
        }
        */
    }

    private ToolPanel toolPanel;
    private String mTitle;
    public ToolWindow(String title, Frame owner)
    {
        //setUndecorated(true);
        super(owner);
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(SystemColor.control);
        this.mTitle = title;
        if (false) System.out.println(this + " cp=" + getContentPane());
        toolPanel = new ToolPanel(mTitle);
        getContentPane().add(toolPanel);
        //setContentPane(toolPanel);
        Component gp = new GlassPane();
        setGlassPane(gp);
        // FYI: I don't know if a glass pane is going to
        // work if we override the content-pane -- see
        // JRootPane custom layout mgr that handles glass
        // pane layout for the *default* content-pane.
        gp.setVisible(true);
        pack();
        //setLocationRelativeTo(owner);
        //setFocusableWindowState(false); // nothing can get input

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

        System.out.println(this + " created.");
    }

    public String toString() {
        return "ToolWindow[" + mTitle + "]";
    }

    public ToolWindow(Frame owner, String title) {
        this(title, owner);
    }
    

    public String getTitle()
    {
        return mTitle;
    }

    public void addTool(JComponent c)
    {
        // todo: make it so can add more than one tool
        // -- probably use BoxLayout
        toolPanel.contentPanel.add(c, BorderLayout.CENTER);
        //c.setBorder(new LineBorder(Color.red, 5));
        //c.setBorder(new LineBorder(SystemColor.control, 5));
        if (c.getBackground().equals(SystemColor.control))
            c.setBorder(new LineBorder(SystemColor.control, 5));
        else
            c.setBorder(new LineBorder(Color.lightGray, 5));
        //c.setBorder(new LineBorder(c.getBackground(), 5));
        setBackground(c.getBackground());
        pack();
        // double-buffering isn't happening on the PC during
        // resize for some reason -- keeping the background
        // color in sync here reduces the repaint clutter
        // a bit until/if we fix it.
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

    final int stickyDist = 10;
    public void setLocation(int x, int y)
    {
        // todo: finish
        Component c = getParent();
        if (c != null) {
            Rectangle parent = getParent().getBounds();
            //System.out.println("parent at " + parent);
            if (getY() < parent.y + parent.height
                && getY() + getHeight() > parent.y
                && x > getX())
            {
                // We're vertically in the parent plane
                int toolRightEdge = getX() + getWidth();
                int reDist = parent.x - toolRightEdge;
                if (reDist >= 0 && reDist <= stickyDist)
                    x = parent.x - getWidth();
            }
        }
        super.setLocation(x, y);
    }


    protected boolean resizeCornerHit(MouseEvent e)
    {
        return
            e.getX() > getWidth() - ResizeCornerSize &&
            e.getY() > getHeight() - ResizeCornerSize
            ||
            e.getX() > getWidth() - 25 && e.getY() < TitleHeight; // tmp hack extra resize
        
    }

    protected boolean closeCornerHit(MouseEvent e)
    {
        return e.getX() > getWidth() - TitleHeight && e.getY() < TitleHeight;
        
    }
    
    public void mousePressed(MouseEvent e)
    {
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
                System.out.println("mouseDragged with no dragStart!");
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
        if (e.getClickCount() == 2)
            setRolledUp(!isRolledUp());
    }
    
    public void mouseEntered(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseExited(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseMoved(MouseEvent e) {}

}
