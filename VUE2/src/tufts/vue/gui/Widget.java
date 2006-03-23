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

import javax.swing.JComponent;


/**

 * A convenice class for providing a wrapper for JComponents to go in
 * WidgetStack's or DockWindow's.

 * Most usefully this actually provides static methods so that any
 * existing JComponent can be treated as a Widget (it just needs to be
 * parented to a WidgetStack or DockWindow) without having to wrap it
 * in a Widget / make it a subclass.  This is doable because we need
 * only add a few properties to the JComponent (e.g., a title,
 * title-suffix) and change events can be iss issued to the parent via AWT
 * PropertyChangeEvents (e.g., expand/collapse, hide/show).
 
 *
 * @version $Revision: 1.3 $ / $Date: 2006-03-23 20:31:31 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Widget extends javax.swing.JPanel
{
    static final String EXPANSION_KEY = "widget.expand";
    static final String HIDDEN_KEY = "widget.hide";
    static final String MENU_ACTIONS_KEY = "widget.menuActions";

    public static void setTitle(JComponent c, String title) {
        c.setName(title);
    }
    
    /** Hide the entire widget, including it's title.  Do not affect expansion state. */
    public static void setHidden(JComponent c, boolean hidden) {
        if (DEBUG.WIDGET) System.out.println("Widget.setHidden " + GUI.name(c) + " " + hidden);
        c.putClientProperty(HIDDEN_KEY, hidden ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /** Make sure the Widget is expanded (visible).  Containing java.awt.Window
     * will be made visible if it's not */
    public static void setExpanded(JComponent c, boolean expanded) {
        if (DEBUG.WIDGET) System.out.println("Widget.setExpanded " + GUI.name(c) + " " + expanded);
        c.putClientProperty(EXPANSION_KEY, expanded ? Boolean.TRUE : Boolean.FALSE);
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(c);
        if (w != null && !w.isVisible() && !tufts.vue.VUE.isStartupUnderway())
            w.setVisible(true);        
        //c.firePropertyChange("TESTPROPERTY", false, true);
    }

    public static void setMenuActions(JComponent c, javax.swing.Action[] actions)
    {
        if (DEBUG.WIDGET) System.out.println("Widget.setMenuActions " + GUI.name(c) + " " + java.util.Arrays.asList(actions));
        c.putClientProperty(MENU_ACTIONS_KEY, actions);
    }

    public static boolean isWidget(JComponent c) {
        return c instanceof Widget || c.getClientProperty(EXPANSION_KEY) != null;
    }
    
    // instance methods for when used as a subclassed wrapper of JPanel:
    
    /** Create a new empty Widget JPanel, with a default layout of BorderLayout */
    public Widget(String title) {
        super(new java.awt.BorderLayout());
        setName(title);
    }

    public void setTitle(String title) {
        setTitle(this, title);
    }
    
    public void setExpanded(boolean expanded) {
        setExpanded(this, expanded);
    }

    public void setMenuActions(javax.swing.Action[] actions) {
        setMenuActions(this, actions);
    }
    
    
}