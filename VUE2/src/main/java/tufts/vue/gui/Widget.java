/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import tufts.vue.DEBUG;
import tufts.vue.DRBrowser;
import tufts.vue.VUE;

import javax.swing.Icon;
import javax.swing.JComponent;

//import com.sun.xml.rpc.processor.modeler.j2ee.xml.javaXmlTypeMappingType;


/**

 * A convenience class for providing a wrapper for JComponents to go in
 * WidgetStack's or DockWindow's.

 * Most usefully this actually provides static methods so that any
 * existing JComponent can be treated as a Widget (it just needs to be
 * parented to a WidgetStack or DockWindow) without having to wrap it
 * in a Widget / make it a subclass.  This is doable because we need
 * only add a few properties to the JComponent (e.g., a title,
 * title-suffix) and change events can be issued to the parent via AWT
 * PropertyChangeEvents (e.g., expand/collapse, hide/show).
 
 *
 * @version $Revision: 1.23 $ / $Date: 2010-02-03 19:15:47 $ / $Author: mike $
 * @author Scott Fraize
 */
public class Widget extends javax.swing.JPanel
{
    static final String EXPANSION_KEY = "widget.expand";
    static final String HIDDEN_KEY = "widget.hide";
    static final String MENU_ACTIONS_KEY = "widget.menuActions";
    static final String MISC_ACTION_KEY = "widget.miscAction";
    static final String MISC_ICON_KEY = "widget.miscIcon";
    static final String HELP_ACTION_KEY = "widget.helpAction";
    static final String REFRESH_ACTION_KEY = "widget.refreshAction";
    static final String WANTS_SCROLLER_KEY = "widget.wantsScroller";
    static final String WANTS_SCROLLERALWAYS_KEY = "widget.wantsScrollerAlways";
    static final String TITLE_HIDDEN_KEY = "widget.titleHidden";
    static final String LOADING_KEY = "widget.loading";

    public static void setTitle(JComponent c, String title) {
        c.setName(title);
    }
    
    public static void setTitleHidden(JComponent c, boolean hidden) {
        setBoolean(c, TITLE_HIDDEN_KEY, hidden);
    }
    
    public static boolean isHidden(JComponent c)
    {
    	Boolean currentProp = (Boolean)c.getClientProperty(HIDDEN_KEY);
    	boolean current = currentProp == null ? false : currentProp.booleanValue();
    	return current;
    }
    /** Hide the entire widget, including it's title.  Do not affect expansion state. */
    public static void setHidden(JComponent c, boolean hidden) {
        // make sure instance method called in case it was overridden
        if (c instanceof Widget)
            ((Widget)c).setHidden(hidden);
        else
            setHiddenImpl(c, hidden);
    }
    
    public static void show(JComponent c) {
        setHidden(c, false);
    }
    public static void hide(JComponent c) {
        setHidden(c, true);
    }
    
    protected static void setHiddenImpl(JComponent c, boolean hidden) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setHidden " + hidden);
        setBoolean(c, HIDDEN_KEY, hidden);
    }

    public static void setWantsScroller(JComponent c, boolean scroller) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setWantsScroller " + scroller);
        setBoolean(c, WANTS_SCROLLER_KEY, scroller);
    }
    
    public static void setWantsScrollerAlways(JComponent c, boolean scroller) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setWantsScrollerAlways " + scroller);
        setBoolean(c, WANTS_SCROLLERALWAYS_KEY, scroller);
    }
    
    
    /** Make sure the Widget is expanded (visible).  Containing java.awt.Window
     * will be made visible if it's not */
    public static void setExpanded(JComponent c, boolean expanded) {
        // make sure instance method called in case it was overridden
        if (c instanceof Widget)
            ((Widget)c).setExpanded(expanded);
        else
            setExpandedImpl(c, expanded);
    }
    
    public static boolean isExpanded(JComponent c)
    {
    	Boolean currentProp = (Boolean)c.getClientProperty(EXPANSION_KEY);
    	boolean current = currentProp == null ? false : currentProp.booleanValue();
    	return current;
    }
    protected static void setExpandedImpl(JComponent c, boolean expand) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setExpanded " + expand);
        //c.putClientProperty(EXPANSION_KEY, expanded ? Boolean.TRUE : Boolean.FALSE);

        setBoolean(c, EXPANSION_KEY, expand);
        
        // We do NOT auto-display the containing window if startup is
        // underway, otherwise all sorts of stuff will show while the
        // windows are being pre-configured.
        
        if (expand && !tufts.vue.VUE.isStartupUnderway()) {
            if (isBooleanTrue(c, HIDDEN_KEY) || !c.isVisible())
                setHidden(c, false);
            if (!DockWindow.AllWindowsHidden() && !tufts.vue.VUE.inNativeFullScreen() &&
            		!isBooleanTrue(c, LOADING_KEY)) {
                // make sure the parent window containing us is visible:
                GUI.makeVisibleOnScreen(c);
            }
        }

        //c.firePropertyChange("TESTPROPERTY", false, true);
    }
    
    public static void setHelpAction(JComponent c, String action)
    {
    //	if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuAction " + action.toString());
        c.putClientProperty(HELP_ACTION_KEY, action);
    }
    
    public static void setRefreshAction(JComponent c, MouseListener action)
    {
    //	if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuAction " + action.toString());
        c.putClientProperty(REFRESH_ACTION_KEY, action);
    }
    
    public static void setMiscAction(JComponent c, MouseListener action, String icon)
    {
    //	if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuAction " + action.toString());
        c.putClientProperty(MISC_ACTION_KEY, action);
        c.putClientProperty(MISC_ICON_KEY, icon);
    }
    public static void setMenuActions(JComponent c, javax.swing.Action[] actions)
    {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuActions " + java.util.Arrays.asList(actions));
        c.putClientProperty(MENU_ACTIONS_KEY, actions);
    }

    public static boolean isWidget(JComponent c) {
        return c != null && (c instanceof Widget || c.getClientProperty(EXPANSION_KEY) != null);
    }

    public static boolean wantsScroller(JComponent c) {
        return isBooleanTrue(c, WANTS_SCROLLER_KEY);
    }
    
    public static boolean wantsScrollerAlways(JComponent c) {
        return isBooleanTrue(c, WANTS_SCROLLERALWAYS_KEY);
    }

    /** only set property if not already set
     * @return true if the value changed
     */
    protected static boolean setBoolean(JComponent c, String key, boolean newValue) {
        Boolean currentProp = (Boolean) c.getClientProperty(key);
        // default for property value not there is false:
        boolean current = currentProp == null ? false : currentProp.booleanValue();
        if (current != newValue) {
            if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " WIDGET-CHANGE " + key + "=" + newValue);
            c.putClientProperty(key, newValue ? Boolean.TRUE : Boolean.FALSE);
            return true;
        } else
            return false;
    }

    protected static boolean isBooleanTrue(JComponent c, Object key) {
        Boolean boolProp = (Boolean) c.getClientProperty(key);
        return boolProp != null && boolProp.booleanValue();
    }
    
  
    // instance methods for when used as a subclassed wrapper of JPanel:
    
    /** Create a new empty Widget JPanel, with a default layout of BorderLayout, with the given content in center */
    public Widget(String title, JComponent content) {
        super(new java.awt.BorderLayout());
        setName(title);
        if (content != null)
            add(content);
        if (DEBUG.BOXES) setBorder(new javax.swing.border.LineBorder(java.awt.Color.blue, 4));
    }

    /** Create a new empty Widget JPanel, with a default layout of BorderLayout */
    public Widget(String title) {
        this(title, null);
    }        
    
    
    public Widget(String title, javax.swing.JButton blah) {
        super(new java.awt.BorderLayout());
        setName(title);        
        if (DEBUG.BOXES) setBorder(new javax.swing.border.LineBorder(java.awt.Color.blue, 4));
    }

    public final void setTitle(String title) {
        setTitle(this, title);
    }
    
    public void setExpanded(boolean expanded) {
        setExpandedImpl(this, expanded);
    }

    public void setHidden(boolean hidden) {
        setHiddenImpl(this, hidden);
    }

    public final void setTitleHidden(boolean hidden) {
        setTitleHidden(this, hidden);
    }

    public final void setMenuActions(javax.swing.Action[] actions) {
        setMenuActions(this, actions);
    }
    
    public final void setWantsScroller(boolean scroller) {
        setWantsScroller(this, scroller);
    }	    

    public static boolean isLoading(JComponent c)
    {
    	Boolean currentProp = (Boolean)c.getClientProperty(LOADING_KEY);
    	boolean current = currentProp == null ? false : currentProp.booleanValue();
    	return current;
    }

    public static void setLoading(JComponent c, boolean loading) {
        // make sure instance method called in case it was overridden
        if (c instanceof Widget)
            ((Widget)c).setLoading(loading);
        else
            setLoadingImpl(c, loading);
    }

    protected static void setLoadingImpl(JComponent c, boolean loading) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setLoading " + loading);
        setBoolean(c, LOADING_KEY, loading);
    }

    public void setLoading(boolean loading) {
        setLoadingImpl(this, loading);
    }

}