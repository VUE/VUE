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


package tufts.vue.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * A vertical stack of collapsable/expandable regions containing arbitrary JComponent's.
 *
 * Note that the ultimate behaviour of the stack will be very dependent on the
 * the preferredSize/maximumSize/minimumSize settings on the contained JComponent's.
 *
 * @version $Revision: 1.2 $ / $Date: 2006-03-17 07:47:57 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class WidgetStack extends JPanel
{
    private final JPanel mGridBag;
    private final GridBagConstraints _gbc = new GridBagConstraints();

    private boolean mExpanderWasAdded = false;

    public WidgetStack() {
        //super(new BorderLayout());
        //setBorder(new LineBorder(Color.red));
        if (true) {
            mGridBag = this;
            setLayout(new GridBagLayout());
        } else {
            mGridBag = new JPanel(new GridBagLayout());
            add(mGridBag, BorderLayout.NORTH);
            // add something to eat extra vertical space
            // (easier than doing via the grid bag itself)
            JLabel empty = new JLabel("empty space");
            //empty.setPreferredSize(new Dimension(0,0));
            add(empty, BorderLayout.CENTER);
        }

        _gbc.gridwidth = GridBagConstraints.REMAINDER;
        _gbc.anchor = GridBagConstraints.NORTH;
        _gbc.weightx = 1;
        
    }

    public void addPane(String title, JComponent widget, int verticalExpansionWeight) {
        //WidgetBox box = new WidgetBox(title, widget);
        WidgetTitle titleBar = new WidgetTitle(title, widget);

        if (verticalExpansionWeight != 0)
            mExpanderWasAdded = true;

        _gbc.weighty = 0;
        _gbc.fill = GridBagConstraints.HORIZONTAL;
        mGridBag.add(titleBar, _gbc);
        
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.weighty = verticalExpansionWeight;
        mGridBag.add(widget, _gbc);
    }

    public void addPane(String title, JComponent widget) {
        addPane(title, widget, 0);
    }
    

    public void addNotify() {
        super.addNotify();

        // we need this all the time as it's now possible for
        // everything to be collpased, even if there was an
        // expander, now that we've done away with WidgetBox
        //if (!mExpanderWasAdded)
        
        // Add a component that is invisible (min size = 0), but will
        // eat up any leftover vertical space in the gridbag, so if
        // everything is collapsed, it will all go to the top, instead
        // of the middle.  Note: if we end up adding panes
        // dynamically, we'll need to manage this, or try going back
        // to handling with a borderlayout or something.
        
        GridBagConstraints c = (GridBagConstraints) _gbc.clone();
        c.fill = GridBagConstraints.CENTER;
        c.weighty = 1;
        JLabel empty = new JLabel("empty space");
        empty.setPreferredSize(new Dimension(0,0));
        empty.setMinimumSize(new Dimension(0,0));
        add(empty, c);
    }

}

class WidgetTitle extends Box {
    private static final int TitleHeight = 20;
    private static final Color TopGradient = new Color(79,154,240);
    private static final Color BottomGradient = new Color(0,133,246);
    private static final GradientPaint Gradient
        = new GradientPaint(0,           0, TopGradient,
                            0, TitleHeight, BottomGradient);

    private static final char RightArrowChar = 0x25B6; // unicode "black right pointing triangle"
    private static final char DownArrowChar = 0x25BC; // unicode "black down pointing triangle"

    private final JLabel mTitle;
    private final JComponent mWidget;
    //private final WidgetBox mWidgetBox;
    private final GUI.IconicLabel mIcon;

    private boolean mExpanded = true;

    private static final boolean isMac = tufts.Util.isMacPlatform();

    
    //public WidgetTitle(String label, WidgetBox box) {
    public WidgetTitle(String label, JComponent widget) {
        super(BoxLayout.X_AXIS);
        setName(label);
        setOpaque(true);
        //mWidgetBox = box;
        mWidget = widget;
        mTitle = new JLabel(label);
        mTitle.setForeground(Color.white);
        mTitle.setFont(new Font("Lucida Grande", Font.BOLD, 12));
        add(Box.createHorizontalStrut(9));
        int iconHeight = 10;
        int iconWidth = 9;
        int fontSize = 9;
        mIcon = new GUI.IconicLabel(DownArrowChar, fontSize, Color.white, iconWidth, iconHeight);
        add(mIcon);
        add(Box.createHorizontalStrut(4));
        add(mTitle);
        setPreferredSize(new Dimension(-1, TitleHeight));
        setMaximumSize(new Dimension(Short.MAX_VALUE, TitleHeight));
        setMinimumSize(new Dimension(50, TitleHeight));


        addMouseListener(new tufts.vue.MouseAdapter(label) {
                public void mouseClicked(MouseEvent e) {
                    setExpanded(!mExpanded);
                }
                public void mousePressed(MouseEvent e) {
                    mIcon.setForeground(TopGradient.brighter());
                }
                public void mouseReleased(MouseEvent e) {
                    mIcon.setForeground(Color.white);
                }
            });
        
    }
    
    private void setExpanded(boolean expanded) {
        mExpanded = expanded;
        if (mExpanded)
            mIcon.setText("" + DownArrowChar);
        else
            mIcon.setText("" + RightArrowChar);
        mWidget.setVisible(expanded);
        //mWidgetBox.setExpanded(mExpanded);
    }
    

    public void paint(Graphics g) {
        if (!isMac) {
            // this is on by default on the mac
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        super.paint(g);
    }

    public void paintComponent(Graphics g) {
        paintGradient((Graphics2D)g);
        super.paintComponent(g);
    }

    private void paintGradient(Graphics2D g)
    {
        g.setPaint(Gradient);
        g.fillRect(0, 0, getWidth(), TitleHeight);
    }

    
}

/*
class WidgetBox extends JPanel {

    private final JComponent mWidget;
    private GridBagConstraints c = new GridBagConstraints();

    WidgetBox(String title, JComponent widget) {
        super(new BorderLayout());
        //super(new GridLayout(2, 1));
        //super(new GridBagLayout());
        //setBorder(new LineBorder(Color.green));
        
        setName(title);
        JComponent titleBar = new WidgetTitle(title, this);
        if (widget == null)
            mWidget = new JTextArea(title + ":\nfoo\nbar\nbaz\boobie\nbazzie");
        else
            mWidget = widget;

        //mWidget.setBorder(new LineBorder(Color.blue));
        
        add(titleBar, BorderLayout.NORTH);
        add(mWidget, BorderLayout.CENTER);
        
        /*
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add(titleBar, c);
        //c.fill = GridBagConstraints.CENTER;
        c.weighty = 1;
        add(mWidget, c);
        *
    }

    void setExpanded(boolean expanded) {
        mWidget.setVisible(expanded);
        //validate();
    }

}

*/