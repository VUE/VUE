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
 */

public class ToolWindow
    extends JWindow
    implements MouseListener, MouseMotionListener
{
    final int TitleHeight = 11;
    final int ResizeCornerSize = 14;
    
    public static void main(String args[])
    {
        ToolWindow tw = new ToolWindow("Title", null);
        //tw.setSize(1024,768);// todo fixme hack: why Graphics being limted to first size??
        tw.setSize(200,200);
        tw.show();
    }
    
    private Point dragStart;
    private Dimension dragSizeStart;

    class ToolPanel extends JPanel
    {
        JPanel titlePanel = new JPanel();
        JPanel contentPanel = new JPanel();
        
        //Component hideButton = new Button();
        //JButton hideButton = new JButton("X");
            
        
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
            
            /*
            hideButton.setFont(new Font("SansSerf", Font.BOLD, 7));
            hideButton.setMargin(new Insets(0,1,0,1));
            hideButton.setDefaultCapable(false);
            titlePanel.add(hideButton);
            */
            
            if (VueUtil.isMacPlatform()) {
                //titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                add(titlePanel, BorderLayout.NORTH);
                contentPanel.setBorder(new LineBorder(Color.gray));
            } else {
                //hideButton.setBackground(SystemColor.activeCaption);
                //titlePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                titlePanel.setBackground(SystemColor.activeCaption);
                titlePanel.setBorder(new LineBorder(SystemColor.activeCaptionBorder));
                contentPanel.setBackground(SystemColor.control);
                contentPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
                contentPanel.add(titlePanel, BorderLayout.NORTH);
            }

            if (title != null) {
                //titlePanel.setLayout(new FlowLayout());
                titlePanel.setLayout(null);
                JLabel l = new JLabel(title);
                l.setFont(new Font("SansSerf", Font.PLAIN, 9));
                //l.setForeground(SystemColor.activeCaptionText);
                l.setForeground(Color.darkGray);
                l.setSize(l.getPreferredSize());
                titlePanel.add(l);
                //System.out.println("lh=" + l.getHeight());
                int y = ((TitleHeight - l.getHeight())+1) / 2;
                if (VueUtil.isMacPlatform())
                    y++;
                l.setLocation(2, y);
            }

            add(contentPanel, BorderLayout.CENTER);
        }

        public void X_addNotify()
        {
            super.addNotify();
            //System.out.println("hideButton=" + hideButton);
            //System.out.println("peer="+hideButton.getPeer());

        }
    }
    
    private ToolPanel toolPanel;
    private String title;
    public ToolWindow(String title, Frame owner)
    {
        //setUndecorated(true);
        super(owner);
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(SystemColor.control);
        this.title = title;
        setContentPane(toolPanel = new ToolPanel(title));
        pack();
        //setLocationRelativeTo(owner);
        //setFocusableWindowState(false); // nothing can get input
    }
    
    class DisplayAction extends AbstractAction
    {
        public DisplayAction(String label)
        {
            super(label);
        }
        public void actionPerformed(ActionEvent e)
        {
            AbstractButton btn = (AbstractButton) e.getSource();
            setVisible(btn.isSelected());
        }
    }

    Action displayAction;
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction(this.title);
        return displayAction;
    }

    public void addTool(Component c)
    {
        // todo: make it so can add more than one tool
        // -- probably use BoxLayout
        toolPanel.contentPanel.add(c, BorderLayout.CENTER);
        setBackground(c.getBackground());
        pack();
        // double-buffering isn't happening on the PC during
        // resize for some reason -- keeping the background
        // color in sync here reduces the repaint clutter
        // a bit until/if we fix it.
    }

    public void paint(Graphics g)
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
    
    public void mousePressed(MouseEvent e)
    {
        dragStart = e.getPoint();
        // todo: will need to check this on the glass pane
        // in case underlying panel is also grabbing
        // mouse events (and then redispatch)
        if (e.getX() > getWidth() - ResizeCornerSize &&
            e.getY() > getHeight() - ResizeCornerSize)
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
    
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseExited(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseMoved(MouseEvent e) {}

}
