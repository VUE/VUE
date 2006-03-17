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
 * @version $Revision: 1.3 $ / $Date: 2006-03-17 15:38:10 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class WidgetStack extends JPanel
{
    private final JPanel mGridBag;
    private final GridBagConstraints _gbc = new GridBagConstraints();
    private final Insets ExpandedTitleBarInsets = GUI.EmptyInsets;
    private final Insets CollapsedTitleBarInsets = new Insets(0,0,1,0);
    private final GridBagLayout mLayout;
    private final JComponent mDefaultExpander;

    private int mExpandersOpen = 0;

    public WidgetStack() {
        mLayout = new GridBagLayout();
        mGridBag = this;
        setLayout(mLayout);

        _gbc.gridwidth = GridBagConstraints.REMAINDER; // last in line as only one column
        _gbc.anchor = GridBagConstraints.NORTH;
        _gbc.weightx = 1;                              // always expand horizontally
        _gbc.gridx = 0;
        _gbc.gridy = 0;

        // We now add a component "guaranteed" to be at the bottom of the stack
        // (gridy=64), that starts invisible, but when all other vertical expanders
        // become invisible (are closed), this component will be set to visible (tho it
        // will display nothing), and will eat up any leftover vertical space at the
        // bottom of gridbag (it has a non-zero weighty), so if everything is collapsed,
        // the titleBar's all go to the top, instead of the middle.  (In a grid bag,
        // there must always be at least one visible component with a non-zero gridy
        // value, or everything is clumped in the center of the display area).
        
        GridBagConstraints c = (GridBagConstraints) _gbc.clone();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.SOUTH;
        c.weighty = 1;
        c.gridy = 64; 
        mDefaultExpander = new JPanel();
        mDefaultExpander.setVisible(false);
        add(mDefaultExpander, c);
    }


    /**
     * At least one pane MUST have a non-zero vertical expansion
     * weight (usually values between 0.0 and 1.0: meaninful only
     * relative to each other), otherwise the panes will all clump
     * together in the middle.
     */
    
    public void addPane(String title, JComponent widget, float verticalExpansionWeight) {
        //verticalExpansionWeight=0.0f;
        boolean isExpander = (verticalExpansionWeight != 0.0f);
        WidgetTitle titleBar = new WidgetTitle(title, widget, isExpander);

        if (isExpander)
            mExpandersOpen++;

        _gbc.weighty = 0;
        _gbc.fill = GridBagConstraints.HORIZONTAL;
        mGridBag.add(titleBar, _gbc);
        _gbc.gridy++;
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.weighty = verticalExpansionWeight;
        mGridBag.add(widget, _gbc);

        _gbc.gridy++;
        
    }

    public void addPane(String title, JComponent widget) {
        addPane(title, widget, 1f);
    }

    public void addNotify() {
        super.addNotify();
        if (mExpandersOpen == 0)
            tufts.Util.printStackTrace("warning: no vertical expanding panes; WidgetStack will not layout properly");
    }
    

    private static final int TitleHeight = 20;
    private static final Color TopGradient = new Color(79,154,240);
    private static final Color BottomGradient = new Color(0,133,246);
    private static final GradientPaint Gradient
        = new GradientPaint(0,           0, TopGradient,
                            0, TitleHeight, BottomGradient);

    private static final char RightArrowChar = 0x25B6; // unicode "black right pointing triangle"
    private static final char DownArrowChar = 0x25BC; // unicode "black down pointing triangle"

    private static final boolean isMac = tufts.Util.isMacPlatform();

    class WidgetTitle extends Box {

        private final JLabel mTitle;
        private final JComponent mWidget;
        private final GUI.IconicLabel mIcon;

        private final boolean isExpander;
        
        private boolean mExpanded = true;

        public WidgetTitle(String label, JComponent widget, boolean isExpander) {
            super(BoxLayout.X_AXIS);
            this.isExpander = isExpander;
            setName(label);
            setOpaque(true);
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
            if (mExpanded) {
                mIcon.setText("" + DownArrowChar);
                if (isExpander)
                    mExpandersOpen++;
            } else {
                mIcon.setText("" + RightArrowChar);
                if (isExpander)
                    mExpandersOpen--;
            }
            if (mExpandersOpen == 0)
                mDefaultExpander.setVisible(true);
            else
                mDefaultExpander.setVisible(false);
                
            mWidget.setVisible(expanded);
            GridBagConstraints c = mLayout.getConstraints(this);
            c.insets = expanded ? ExpandedTitleBarInsets : CollapsedTitleBarInsets;
            mLayout.setConstraints(this, c);
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

}    

