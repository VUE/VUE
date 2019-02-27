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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import tufts.vue.DEBUG;
import tufts.vue.LWPropertyChangeEvent;
import tufts.vue.VueAction;
import tufts.vue.VueResources;

/**
 * A button that supports a drop-down menu and changes state based on the
 * currently selected drop-down menu item.  
 * The menu can be initialized from either an array of property values, or actions.
 * Property change events or action events (depending on the initialization type)
 * are fired when the menu selection changes.
 *
 * Subclasses must implement LWEditor produce/display
 *
 * @version $Revision: 1.33 $ / $Date: 2010-02-03 19:15:47 $ / $Author: mike $
 * @author Scott Fraize
 *
 */

// as this class is now specialized to handle vue LWKey properties,
// it's no longer generic gui.  Can create subclass, VuePopupMenu,
// that does the LWPropertyHandler impl, and move that to the
// forthcoming tool package.

public abstract class MenuButton<T> extends JButton
    implements ActionListener, tufts.vue.LWEditor<T>
// todo: cleaner to get this to subclass from JMenu, and then cross-menu drag-rollover
// menu-popups would automatically work also.
{
    protected Object mPropertyKey;
    protected T mCurrentValue;
    
    protected JPopupMenu mPopup;
    protected JMenuItem mEmptySelection;
    private Icon mButtonIcon;

    //protected static final String ArrowText = "v ";
    private static boolean isButtonActionable = false;

    private boolean actionAreaClicked = false;

    private Insets insets(int x) { return new Insets(x,x,x,x); }


    public MenuButton()
    {
        if (false) {
            //setBorder(BorderFactory.createEtchedBorder());
            //setBorder(new CompoundBorder(BorderFactory.createRaisedBevelBorder(), new EmptyBorder(3,3,3,3)));
            //setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new LineBorder(Color.blue, 6)));
            //setBorder(BorderFactory.createRaisedBevelBorder());
        } else {
            //setOpaque(false);
            setContentAreaFilled(false);
            setBorder(null);
            //setBorder(new LineBorder(Color.blue, 2));
            //setBorder(new EmptyBorder(2,2,2,2));
        }

        //setBorderPainted(false);

        if (GUI.isMacAqua()) {
            // anything big makes them rounded & fit into their space.
            // We could make them square making them smaller, but will
            // need to force the toolbar row to small height, or compute
            // the insets needed for each button based on it's minimum size.
            setMargin(insets(22));
        }

        setFocusable(false);
        addActionListener(this);
        final int borderIndent = 2;
        addMouseListener(new tufts.vue.MouseAdapter(toString()) {
                public void mousePressed(MouseEvent e) {

                    if (!MenuButton.this.isEnabled())
                        return;
                    /*
                    if (//getText() == ArrowText &&
                        isButtonActionable && getIcon() != null && e.getX() < getIcon().getIconWidth() + borderIndent) {
                        actionAreaClicked = true;
                    } else {
                    */
                    actionAreaClicked = false;
                    Component c = e.getComponent();
                    getPopupMenu().show(c, 0, (int) c.getBounds().getHeight());
                }
            });
    }

    /** if the there's an immediate action area of the button pressed, fire the property setter
        right away instead of popping the menu (actionAreaClicked was determined in mousePressed) */
    public void actionPerformed(ActionEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " " + e);
        if (actionAreaClicked)
            firePropertySetter();
    }

    /**
     * Set the raw icon used for displaying as a button (may be additionally decorated).
     * Intended for use during initialization.
     **/
    public void setButtonIcon(Icon i) {
        if (DEBUG.BOXES||DEBUG.TOOL) System.out.println(this + " setButtonIcon " + i);
        //if (DEBUG.Enabled) new Throwable().printStackTrace();
        _setIcon(mButtonIcon = i);
    }

    
    /*Intended for use during initialization OR then later for value changes.
    public void setOrResetButtonIcon(Icon i) {
        System.out.println("MenuButton " + this + " setButtonIcon " + i);
        new Throwable().printStackTrace();
        if (mButtonIcon == null && i != null) {
            _setIcon(mButtonIcon = i);
        } if (i instanceof BlobIcon && mButtonIcon instanceof BlobIcon) {
            // in this case, just transfer color from the given icon to
            // our already existing color.
            ((BlobIcon)mButtonIcon).setColor(((BlobIcon)i).getColor());
        } else {
            System.out.println("MenuButton " + this + " setButtonIcon: INITIALIZED");
            mButtonIcon = i;
        }
    }
    */
    
    protected Icon getButtonIcon() {
        return mButtonIcon;
    }

    /* override of JButton so can catch blob-icon 
    public void XsetIcon(Icon i) {
        if (mButtonIcon != null) {
            if (i instanceof BlobIcon && mButtonIcon instanceof BlobIcon)
                ((BlobIcon)mButtonIcon).setColor(((BlobIcon)i).getColor());
        } else {
            System.out.println("MenuButton " + this + " setIcon " + i);
            _setIcon(i);
        }
    }
    */

    /** return the default button size for this type of button: subclasses can override */
    protected Dimension getButtonSize() {
        return new Dimension(32,22); // better at 22, but get clipped 1 pix at top in VueToolbarController! todo: BUG
    }
    
    private void _setIcon(Icon i) {
        /*
            super.setIcon(i);
            super.setRolloverIcon(new VueButtonIcon(i, VueButtonIcon.ROLLOVER));
        */
        /*
          final int pad = 7;
          Dimension d = new Dimension(i.getIconWidth()+pad, i.getIconHeight()+pad);
          if (d.width < 21) d.width = 21; // todo: config
          if (d.height < 21) d.height = 21; // todo: config
        */
        //if (DEBUG.BOXES||DEBUG.TOOL) System.out.println(this + " _setIcon " + i);
        Dimension d = getButtonSize();
        if (true || !GUI.isMacAqua()) {
            if (false)
                VueButtonIcon.installGenerated(this, i, d);
            else
                VueButtonIcon.installGenerated(this, new MenuProxyIcon(i), d);
            //System.out.println(this + " *** installed generated, setPreferredSize " + d);
        }
        setPreferredSize(d);
    }
    

    protected JPopupMenu getPopupMenu() {
        return mPopup;
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
        mPopup = new JPopupMenu();
			
        //final String valueKey = getPropertyName() + ".value"; // propertyName usually not set at this point!
            
        final ActionListener menuItemAction =
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleMenuSelection(e);
                }};
            
        for (int i = 0; i < values.length; i++) {
            JMenuItem item;
            T value;
            Icon icon = null;
            if (values[i] instanceof Action) {
                Action a = (Action) values[i];
                item = new JMenuItem((String) a.getValue(Action.NAME));
                value = (T) a.getValue(ValueKey);
                icon = (Icon) a.getValue(Action.SMALL_ICON);
            } else {
                item = new JMenuItem();
                value = (T) values[i];
            }
            item.putClientProperty(ValueKey, value);
            if (icon == null)
                icon = makeIcon(value);
            if (icon != null)
                item.setIcon(icon);
            if (names != null)
                item.setText(names[i]);
            item.addActionListener(menuItemAction);
            mPopup.add(item);
        }

        if (createCustom) {
            JMenuItem item = new JMenuItem(VueResources.getString("menubutton.custom")); // todo: more control over this item
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleValueSelection(runCustomChooser()); }});
            mPopup.add(item);
        }

        mEmptySelection = new JMenuItem();
        mEmptySelection.setVisible(false);
        mPopup.add(mEmptySelection);
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
    
		
    protected void handleMenuSelection(ActionEvent e) {

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
        handleValueSelection(((JComponent)e.getSource()).getClientProperty(ValueKey));
    }

    /** Simulate a user value selection */
    public void selectValue(Object value) {
        handleValueSelection(value);
    }
    
    protected void handleValueSelection(Object newPropertyValue) {
        if (DEBUG.TOOL) System.out.println(this + " handleValueSelection: newPropertyValue=" + newPropertyValue);
        // TODO: this is getting fired twice, once for ItemEvent stateChange=DESELECTED, and
        // then the one we really want, with itemState=SELECTED.  We want to ignore the former,
        // as it's generating extra property sets on the selection that are immediately
        // overriden by the SELECTED value.  This should actually be harmless, but
        // it's definitely unexpected internal behaviour.  -- SMF 2007-05-01 14:55.37
        
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
                    if (DEBUG.TOOL && (DEBUG.EVENTS || DEBUG.META)) System.out.println(this + " fires " + event + " to " + listeners[i]);
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
        /*
        if (true) setToolTipText(null);//tmp debug
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.black);
        final int arrowWidth = 5; // make sure is odd #
        int x = w - (arrowWidth + 3);
        int y = h / 2 - 1;
        for (int len = arrowWidth; len > 0; len -= 2) {
            g.drawLine(x,y,x+len,y);
            y++;
            x++;
        }
        */
    }
	
    public String toString() {
        return getClass().getName() + "[" + getPropertyKey() + "]";
    }
}



	
    /*
     * paint( Graphics g)
     * Overrides paint method and renders an additional icon ontop of
     * of the normal rendering to indicate if this button contains
     * a popup handler.
     *
     * param Graphics g the Graphics.
     **/
    /*

    // default offsets for drawing popup arrow via code
    public int mArrowSize = 3;
    public int mArrowHOffset  = -9;
    public int mArrowVOffset = -7;
	
    // the popup overlay icons for up and down states
    public Icon mPopupIndicatorIconUp = null;
    public Icon mPopupIndicatorDownIcon = null;

    
    public void paint( java.awt.Graphics pGraphics) {
        super.paint( pGraphics);
        if (true) return;
        Dimension dim = getPreferredSize();
        Insets insets = getInsets();
		
        // now overlay the popup menu icon indicator
        // either from an icon or by brute painting
        if(  (! false ) 
             &&  (mPopup != null) 
             && ( !mPopup.isVisible() ) ) {
            // draw popup arrow
            Color saveColor = pGraphics.getColor();
            pGraphics.setColor( Color.black);
			
            int w = getWidth();
            int h = getHeight();
			
            int x1 = w + mArrowHOffset;
            int y = h + mArrowVOffset;
            int x2 = x1 + (mArrowSize * 2) -1;
			
            for(int i=0; i< mArrowSize; i++) { 
                pGraphics.drawLine(x1,y,x2,y);
                x1++;
                x2--;
                y++;
            }
            pGraphics.setColor( saveColor);
        }
        else  // Use provided popup overlay  icons
            if(   (mPopup != null) && ( !mPopup.isVisible() ) ) {
			
                Icon overlay = null;
                if( isSelected() ) {
				
                    overlay = mPopupIndicatorDownIcon;
                }
                if( overlay != null) {
                    overlay.paintIcon( this, pGraphics, insets.top, insets.left);
                }
            }
    }
    */