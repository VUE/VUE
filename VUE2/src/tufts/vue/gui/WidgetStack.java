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

import tufts.vue.DEBUG;

import java.beans.PropertyChangeEvent;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A vertical stack of collapsable/expandable regions containing arbitrary JComponent's.
 *
 * Note that the ultimate behaviour of the stack will be very dependent on the
 * the preferredSize/maximumSize/minimumSize settings on the contained JComponent's.
 *
 * @version $Revision: 1.10 $ / $Date: 2006-04-11 05:48:26 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class WidgetStack extends JPanel
{
    private final JPanel mGridBag;
    private final GridBagConstraints _gbc = new GridBagConstraints();
    private final Insets ExpandedTitleBarInsets = GUI.EmptyInsets;
    private final Insets CollapsedTitleBarInsets = new Insets(0,0,1,0);
    //private final Insets TitleBarInsets = new Insets(1,0,0,0);
    private final GridBagLayout mLayout;
    private final JComponent mDefaultExpander;

    private int mExpanderCount = 0;
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

        // todo: need to set min size on whole stack (nitems * title height)
        // and have DockWindow respect this.
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

        if (isExpander)
            mExpanderCount++;
        
        WidgetTitle titleBar = new WidgetTitle(title, widget, isExpander);

        
        if (DEBUG.WIDGET) {
            out("addPane:"
                + " expansionWeight=" + verticalExpansionWeight
                //+ " expanderCnt=" + mExpanderCount
                //+ " isExpander=" + isExpander
                + " [" + title + "] containing " + GUI.name(widget));
            dumpSizeInfo(widget);
        }


        _gbc.weighty = 0;
        _gbc.fill = GridBagConstraints.HORIZONTAL;
        _gbc.insets = ExpandedTitleBarInsets;
        mGridBag.add(titleBar, _gbc);
        
        _gbc.gridy++;
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.weighty = verticalExpansionWeight;
        _gbc.insets = GUI.EmptyInsets;
        mGridBag.add(widget, _gbc);

        _gbc.gridy++;

        //if (!widget.isPreferredSizeSet()) {// note: a java 1.5 api call only
        //if (!isExpander && !widget.isMinimumSizeSet())
        //    widget.setMinimumSize(widget.getPreferredSize());
        
    }

    /**

     * The given widget *must* already have it's name set to be used as the title.

     * Note that if verticalExpansionWeight is zero, it is also important that the given
     * JComponent provides a reasonable preferredSize or minimumSize. Most Swing
     * components provide a reasonable preferredSize automatically, but pay attention to
     * layout managers that might not do such a good job of passing up this information.
     * Also of particular note are JScrollPanes, which by default usually will collapse
     * down to about nothing unless you manually set the pref or min sizes them (or, say a
     * container they're laid out in that they're expanding to fill).
     
     **/
    public void addPane(JComponent widget, float verticalExpansionWeight) {
        addPane(widget.getName(), widget, verticalExpansionWeight);
    }
    

    public void addPane(String title, JComponent widget) {
        addPane(title, widget, 1.0f);
    }

    public Widget addPane(String title) {
        Widget w = new Widget(title);
        addPane(w, 1.0f);
        return w;
    }

    /** @param c must have name set (setName) */
    public void addPane(JComponent c) {
        addPane(c, 1f);
    }


    private void dumpSizeInfo(Component c) {
        if (DEBUG.META) tufts.Util.printStackTrace("java 1.5 only");
        //out("\tprefSize " + (c.isPreferredSizeSet()?" SET ":"     ") + c.getPreferredSize());
        //out("\t minSize " + (c.isMinimumSizeSet()?" SET ":"     ") + c.getMinimumSize());
        //out("\t maxSize " + (c.isMaximumSizeSet()?" SET ":"     ") + c.getMaximumSize());
    }

    public void addNotify() {
        updateDefaultExpander();
        super.addNotify();
        if (mExpanderCount == 0)
            if (DEBUG.Enabled) out("warning: no vertical expanders");
            //tufts.Util.printStackTrace("warning: no vertical expanding panes; WidgetStack will not layout properly");
        setName("in " + GUI.name(getParent()));
        //setName("in " + getParent().getName());
        //setName("WidgetStack:" + getParent().getName());
    }
    
    private void updateDefaultExpander() {
        //System.out.println("EXPANDERS OPEN: " + mExpandersOpen);
        if (mExpandersOpen == 0)
            mDefaultExpander.setVisible(true);
        else
            mDefaultExpander.setVisible(false);
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

    private static final char Chevron = 0xBB; // unicode "right-pointing double angle quotation mark"

    class WidgetTitle extends Box implements java.beans.PropertyChangeListener {

        private final JLabel mTitle;
        private final MenuButton mMenuButton;
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

            add(Box.createGlue());
            mMenuButton = new MenuButton(Chevron, null);
            add(mMenuButton);

            
            setPreferredSize(new Dimension(50, TitleHeight));
            setMaximumSize(new Dimension(Short.MAX_VALUE, TitleHeight));
            setMinimumSize(new Dimension(50, TitleHeight));

            addMouseListener(new tufts.vue.MouseAdapter(label) {
                    public void mouseClicked(MouseEvent e) { Widget.setExpanded(mWidget, !mExpanded); }
                    public void mousePressed(MouseEvent e) { mIcon.setForeground(TopGradient.brighter()); }
                    public void mouseReleased(MouseEvent e) { mIcon.setForeground(Color.white); }
                });

            // Check for property values set on the JComponent before being added to the
            // WidgetStack.  Important to handle EXPANSION_KEY before HIDDEN_KEY
            // (changing the expansion of something that is hidden is currently
            // undefined).  If the property is already set, we handle it via a fake
            // propertyChangeEvent.  If it isn't set, we set the default, which won't
            // trigger a recursive propertyChangeEvent as we haven't added us as a
            // property change listener yet.
            
            Object expanded = widget.getClientProperty(Widget.EXPANSION_KEY);
            if (expanded != null) {
                propertyChange(new PropertyChangeEvent(this, Widget.EXPANSION_KEY, null, expanded));
            } else {
                // expanded by default                
                widget.putClientProperty(Widget.EXPANSION_KEY, Boolean.TRUE);
                handleWidgetDisplayChange(true); 
            }
                
            Object hidden = widget.getClientProperty(Widget.HIDDEN_KEY);
            if (hidden != null) {
                propertyChange(new PropertyChangeEvent(this, Widget.HIDDEN_KEY, null, hidden));
            } else {
                // not hidden by default
                widget.putClientProperty(Widget.HIDDEN_KEY, Boolean.FALSE);
            }
            
            widget.addPropertyChangeListener(this);
            
        }
    
        /** interface java.beans.PropertyChangeListener for contained component */
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            final String key = e.getPropertyName();
        
            if (DEBUG.WIDGET && !key.equals("ancestor"))
                out(GUI.name(e.getSource()) + " property \"" + key + "\" newValue=[" + e.getNewValue() + "]");

            if (key == Widget.EXPANSION_KEY) {
                setExpanded( ((Boolean)e.getNewValue()).booleanValue() );
                
            } else if (key == Widget.HIDDEN_KEY) {
                setHidden( ((Boolean)e.getNewValue()).booleanValue() );

            } else if (key == Widget.MENU_ACTIONS_KEY) {
                mMenuButton.setMenuActions((Action[]) e.getNewValue());
                
            } else if (key.equals("name")) {
                mTitle.setText((String) e.getNewValue());
                
            } else if (key.endsWith("Size")) {

                Component src = (Component) e.getSource();
            
                if (DEBUG.WIDGET) dumpSizeInfo(src);
                if (DEBUG.WIDGET) out("revalidate on size property change");
                revalidate();
                if (DEBUG.WIDGET) dumpSizeInfo(src);
            }
        }

        /**
         * This method only does something if isExpander is true: track how many
         * expanding Widgets (in the GridBagLayout) are visible, beacuse when we get
         * down to 0, we need to add a default expander to take up the remaining space.
         */
        private void handleWidgetDisplayChange(boolean visible) {
            if (!isExpander)
                return;

            if (visible)
                mExpandersOpen++;
            else
                mExpandersOpen--;

            if (DEBUG.WIDGET) out("VISIBLE EXPANDER COUNT: " + mExpandersOpen);
            
            if (mExpandersOpen < 0 || mExpandersOpen > mExpanderCount)
                throw new IllegalStateException("WidgetStack: expanders claimed open: "
                                                + mExpandersOpen
                                                + ", expander count=" + mExpanderCount);

            // Could do: if only one widget open, change constraints on THAT guy to
            // expand...  Or, as soon as all expanders close, set remaining ones to
            // expand equally?  That could just get ugly tho (titles keep moving down
            // out from under your mouse -- this currently happens only on our last item
            // in the stack which isn't so bad the way we're using it, but for
            // everything?) Really need second tier expander marks for stuff
            // w/scroll-panes (use negative expansion weights?)  Subclassing
            // GridBagLayout might make this easier also.

            // new: if last expander open, and we close it,
            // open the next visible expander below, or if none, the
            // one above, or none, don't allow this to collapse.

            WidgetStack.this.updateDefaultExpander();
                
        }

        private void setHidden(boolean hide) {
            if (DEBUG.WIDGET) out("setHidden " + hide);

            if (isVisible() == !hide)
                return;

            setVisible(!hide);
            if (hide) {
                if (mExpanded) {
                    mWidget.setVisible(false);
                    handleWidgetDisplayChange(false);
                }
            } else if (mExpanded) {
                mWidget.setVisible(true);
                handleWidgetDisplayChange(true);
            }
        }
        

        private void setExpanded(boolean expanded) {
            if (DEBUG.WIDGET) out("setExpanded " + expanded);
            if (mExpanded == expanded)
                return;
            mExpanded = expanded;
            if (mExpanded) {
                mIcon.setText("" + DownArrowChar);
            } else {
                mIcon.setText("" + RightArrowChar);
            }

            handleWidgetDisplayChange(mExpanded);

            GridBagConstraints c = mLayout.getConstraints(this);
            c.insets = expanded ? ExpandedTitleBarInsets : CollapsedTitleBarInsets;
            mLayout.setConstraints(this, c);

            mWidget.setVisible(expanded);

            revalidate();
            
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


        private void out(Object o) {
            System.err.println(GUI.name(this) + " " + (o==null?"null":o.toString()));
        }

    
    }
    
    private void out(Object o) {
        System.err.println(GUI.name(this) + " " + (o==null?"null":o.toString()));
    }

    static class MenuButton extends GUI.IconicLabel {
        private static final Color defaultColor = GUI.isMacAqua() ? Color.white : Color.black;
        private static final Color activeColor = GUI.isMacAqua() ? TopGradient.brighter() : Color.white;

        MenuButton(char iconChar, Action[] actions) {
            super(iconChar, 18, defaultColor, TitleHeight, TitleHeight);
            setAlignmentY(0.5f);
            // todo: to keep manually picking a height and a bottom pad to get this
            // middle aligned is no good: will eventually want to use a TextLayout to
            // get precise bounds for center, and create as a real Icon
            setBorder(new javax.swing.border.EmptyBorder(0,0,3,0));
            setMenuActions(actions);

            /*
            new Action[] {
                new tufts.vue.VueAction("foo"),
                new tufts.vue.VueAction("bar"),
            });
            */
            
        }

        void setMenuActions(Action[] actions) {
            clearMenuActions();

            if (actions == null) {
                setVisible(false);
                return;
            }
            setVisible(true);

            new GUI.PopupMenuHandler(this, GUI.buildMenu(actions)) {
                public void mouseEntered(MouseEvent e) { setForeground(activeColor); }
                public void mouseExited(MouseEvent e) { setForeground(defaultColor); }
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

        
    }

    public static void main(String args[])
    {
        // todo: appears to be a bug in GridBagLayout where if ALL
        // components are expanders, in can sometimes add a pixel
        // at the top of the freakin layout.  This example
        // was all weights of 1.0.  The pixel can come in and
        // out even during resizes: some sizes just trigger it...
        // Okay, even if NOT all are expanders it can fail.
        // Christ.  Yet our ResourcePanel stack and DRBrowser
        // stack work fine... Okay, thoes have only ONE expander...
        
        tufts.vue.VUE.init(args);
        
        WidgetStack s = new WidgetStack();

        String[] names = new String[] { "One",
                                        "Two",
                                        "Three",
                                        "Four",
        };

        for (int i = 0; i < names.length; i++) {
            s.addPane(names[i], new JLabel(names[i], SwingConstants.CENTER), 1f);
        }
        //s.addPane("Fixed", new JLabel("fixed"), 0f);

        GUI.createDockWindow("WidgetStack Test", s).setVisible(true);
    }
        
}    

