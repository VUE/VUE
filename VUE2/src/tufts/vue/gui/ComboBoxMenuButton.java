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
import tufts.vue.VueAction;
import tufts.vue.LWComponent;
import tufts.vue.LWPropertyChangeEvent;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A button that supports a drop-down menu and changes state based on the
 * currently selected drop-down menu item.  
 * The menu can be initialized from either an array of property values, or actions.
 * Property change events or action events (depending on the initialization type)
 * are fired when the menu selection changes.
 *
 * Subclasses must implement LWEditor produce/display
 *
 * @version $Revision: 1.1 $ / $Date: 2007-04-18 13:29:11 $ / $Author: mike $
 * @author Scott Fraize
 *
 */

// as this class is now specialized to handle vue LWKey properties,
// it's no longer generic gui.  Can create subclass, VuePopupMenu,
// that does the LWPropertyHandler impl, and move that to the
// forthcoming tool package.

public abstract class ComboBoxMenuButton<T> extends JComboBox
    implements ActionListener, tufts.vue.LWEditor<T>
// todo: cleaner to get this to subclass from JMenu, and then cross-menu drag-rollover
// menu-popups would automatically work also.
{
    protected Object mPropertyKey;
    protected T mCurrentValue;
    
    //protected JPopupMenu mPopup;
    //protected JMenuItem mEmptySelection;
    private Icon mButtonIcon;

    //protected static final String ArrowText = "v ";
    private static boolean isButtonActionable = false;

    private boolean actionAreaClicked = false;

    private Insets insets(int x) { return new Insets(x,x,x,x); }


    public ComboBoxMenuButton()
    {
    	super();
        setFocusable(false);
        addActionListener(this);
      	
    }

    /** if the there's an immediate action area of the button pressed, fire the property setter
        right away instead of popping the menu (actionAreaClicked was determined in mousePressed) */
    public void actionPerformed(ActionEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " " + e);
        if (actionAreaClicked)
            firePropertySetter();
    }

       /** return the default button size for this type of button: subclasses can override */
    protected Dimension getButtonSize() {
        return new Dimension(32,22); // better at 22, but get clipped 1 pix at top in VueToolbarController! todo: BUG
    }
    
   
    public void setPropertyKey(Object key) {
        mPropertyKey = key;
    }
 	
    public Object getPropertyKey() {
        return mPropertyKey;
    }

    /** factory method for subclasses -- build's an icon for menu items */
    protected Icon makeIcon(T value) {
        return null;
    }
    
    /** override if there is a custom menu item */
    protected Object runCustomChooser() {
        return null;
    }
    
    /** @param values can be property values or actions (or even a mixture) */
    protected void buildMenu(Object[] valuesOrActions)
    {
        buildMenu(valuesOrActions, null, false); 
    }

    /** Key for JMenuItem's: a place to store a property value for this menu item */
    public static final String ValueKey = "property.value";
    
    /**
     * @param values can be property values or actions
     * @param names is optional
     * @param createCustom - add a "Custom" menu item that calls runCustomChooser
     *
     * The ability to pass in an array of actions is a convenience to create
     * the needed JMenuItem's using the Action.NAME & Action.SMALL_ICON & MenuButton.ValueKey
     * values stored in the action. The action is not actually fired when the menu
     * item is selected (this used to be the case, but no longer).
     * The action's will be expected to hava a value under the key
     * MenuButton.ValueKey representing the value of the object.  (This only
     * works for actions that set specific values every time they fire).
     *
     * OLD:
     * If values are actions, the default handleValueSelection won't ever
     * do anything as a value wasn't set on the JMenuItem -- it's assumed
     * that the action is handling the value change.  In this case override
     * handleMenuSelection to change the buttons appearance after a selection change.
     */
    protected void buildMenu(Object[] values, String[] names, boolean createCustom)
    {
        //mPopup = new JPopupMenu();
			
        //final String valueKey = getPropertyName() + ".value"; // propertyName usually not set at this point!
            
       
            
        this.addItemListener(new ItemListener(){
        	public void itemStateChanged(ItemEvent e)
        	{
        		handleMenuSelection(e);
        	}
        });
        for (int i = 0; i < values.length; i++) {
           // JMenuItem item;
           // T value;
           // Icon icon = null;
           // if (values[i] instanceof Action) {
           //     Action a = (Action) values[i];
           //     item = new JMenuItem((String) a.getValue(Action.NAME));
           //     value = (T) a.getValue(ValueKey);
           //     icon = (Icon) a.getValue(Action.SMALL_ICON);
           // } else {
           //     item = new JMenuItem();
           //     value = (T) values[i];
           // }
           // item.putClientProperty(ValueKey, value);
           // if (icon == null)
           //     icon = makeIcon(value);
           // if (icon != null)
           //     item.setIcon(icon);
           // if (names != null)
           //     item.setText(names[i]);
       // 	((Component)values[i]).addActionListener(menuItemAction);
        //	this.addActionListener(menuItemAction);
          //  mPopup.add(item);
        	addItem(values[i]);
        }

  /*      if (createCustom) {
            JMenuItem item = new JMenuItem("Custom..."); // todo: more control over this item
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleValueSelection(runCustomChooser()); }});
            mPopup.add(item);
        }

        mEmptySelection = new JMenuItem();
        mEmptySelection.setVisible(false);
        mPopup.add(mEmptySelection);*/
        
    }

    protected void buildMenu(Class<T> enumType)
    {
        T[] values = enumType.getEnumConstants();
        if (values == null)
            throw new Error("no enum constants for (not an enum?) " + enumType);
        String[] names = new String[values.length];
        int i = 0;
        for (T e : values)
            names[i++] = e.toString();
        buildMenu(values, names, false);
    }
    
		
    protected void handleMenuSelection(ItemEvent e) {

        // Note: based on the action event, if the source JMenuItem had an icon set,
        // we could automatically set the displayed button icon to the same here.
        // This doesn't help us tho, as MenuButtons need to be able to handle
        // property value changes happening outside of this component, and then
        // reflecting that value, which means in the LWPropertyProducer.setPropertyValue,
        // we have to provide a specific mapping from the given property value to the
        // selected menu item anyway.  (Altho: if all our JMenuItems had a "property.value"
        // set in them, we could search thru them every time to figure out which icon
        // to set as a default...)

        if (DEBUG.TOOL) System.out.println("\n" + this + " handleMenuSelection " + e);
        if (e.getItem() instanceof Action)
        	handleValueSelection(((Action)e.getItem()).getValue(ValueKey));
        else
        	handleValueSelection(e.getItem());
    }
    
    protected void handleValueSelection(Object newPropertyValue) {
        if (DEBUG.TOOL) System.out.println(this + " handleValueSelection: newPropertyValue=" + newPropertyValue);
        if (newPropertyValue == null) // could be result of custom chooser
            return;
        // even if we were build from actions, in which case the LWComponents
        // have already been changed via that action, call setPropertyValue
        // here so any listening LWCToolPanels can update their held state,
        // and so subclasses can update their displayed selected icons

        // Okay, do NOT call this with the action?  But what happens if nothing is selected?
        if (newPropertyValue instanceof Action) {
            System.out.println("Skipping setPropertyValue & firePropertyChanged for Action " + newPropertyValue);
        } else {
            Object oldValue = produceValue();
            displayValue((T)newPropertyValue);
            firePropertyChanged(oldValue, newPropertyValue);
        }
        repaint();
    }

    /** @return the currently selected value (interface LWEditor) */
    public T produceValue() {
        if (DEBUG.TOOL) System.out.println(this + " produceValue " + mCurrentValue);
        return mCurrentValue;
    }

    /** Sub-classes can set the current value (mCurrentValue), and must update the display (icon, etc) and repaint (interface LWEditor) */
    public abstract void displayValue(T value);
	
    /** fire a property change event even if old & new values are the same */
    // COULD USE Component.firePropertyChange!  all this is getting us is diagnostics!
    protected void firePropertyChanged(Object oldValue, Object newValue)
    {
        if (getPropertyKey() != null) {
            PropertyChangeListener[] listeners = getPropertyChangeListeners();
            if (listeners.length > 0) {
                PropertyChangeEvent event = new LWPropertyChangeEvent(this, getPropertyKey(), oldValue, newValue);
                for (int i = 0; i< listeners.length; i++) {
                    if (DEBUG.TOOL) System.out.println(this + " fires " + event + " to " + listeners[i]);
                    listeners[i].propertyChange(event);
                }
            }
        }
    }
    protected void firePropertySetter() {
        Object o = produceValue();
        if (DEBUG.TOOL) System.out.println(this + " firePropertySetter " + o);
        if (o instanceof Action) {
            if (o instanceof VueAction)
                ((VueAction)o).fire(this);
            else {
                Action a = (Action) o;
                a.actionPerformed(new ActionEvent(this, 0, (String) a.getValue(Action.NAME)));
            }
        } else {
            firePropertyChanged(o, o);
        }
    }
	
    public void paint(java.awt.Graphics g) {
        ((java.awt.Graphics2D)g).setRenderingHint
            (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }
	
    
  
    
    public String toString() {
        return getClass().getName() + "[" + getPropertyKey() + "]";
    }
}