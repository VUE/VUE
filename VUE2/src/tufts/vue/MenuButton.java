package tufts.vue;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * MenuButton
 *
 * @author Scott Fraize
 * @version March 2004
 *
 */

public abstract class MenuButton extends JButton
// todo: cleaner to get this to subclass from JMenu, and then cross-menu drag-rollover
// menu-popups would automatically work also.  Actualy, this should probably
// either just be a JComboBox or subclass if JComboBox can handle arbitrary contents.
// (that's essentailly what this is: a combo box that supports non string types)
{
    protected String mPropertyName;
    protected JPopupMenu mPopup;
    protected JMenuItem mEmptySelection;

    public MenuButton()
    {
        //setIcon(sIcon);
        //mPopup = getPopupMenu();
        //buildMenu(pColors, pMenuNames, pHasCustom);
        //setBorder(new CompoundBorder(BorderFactory.createRaisedBevelBorder(), new EmptyBorder(3,3,3,3)));
        //setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new LineBorder(Color.blue, 6)));
        //setBorder(BorderFactory.createRaisedBevelBorder());
        
        setBorder(BorderFactory.createEtchedBorder());
        //setBorder(new EmptyBorder(2,2,2,2));
        setFont(VueConstants.FONT_ICONIC);
        setText("v ");
        setFocusable(false);
        setContentAreaFilled(false);
        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    Component c = e.getComponent(); 	
                    getPopupMenu().show(c, 0, (int) c.getBounds().getHeight());
                }
            });
    }

    protected JPopupMenu getPopupMenu() {
        return mPopup;
    }
    
    public void setPropertyName(String pName) {
        mPropertyName = pName;
    }

    public String getPropertyName() {
        return mPropertyName;
    }

    public abstract void setPropertyValue(Object propertyValue);
    public abstract Object getPropertyValue();

    public void loadPropertyValue(tufts.vue.beans.VueBeanState state) {
        setPropertyValue(state.getPropertyValue(getPropertyName()));
    }
	
    /** factory method for subclasses */
    protected Icon makeIcon(Object value) {
        return null;
    }
    
    /** override if there is a custom menu item */
    protected Object runCustomChooser() {
        return null;
    }
    
    /** @param values can be property values or actions (or even a mixture) */
    protected void buildMenu(Object[] values)
    {
        buildMenu(values, null, false); 
    }

    private final String mValueKey = "prop.value";
    
    /**
     * @param values can be property values or actions
     * @param names is optional
     * @param createCustom - add a "Custom" menu item that calls runCustomChooser
     */
    protected void buildMenu(Object[] values, String[] names, boolean createCustom)
    {
        mPopup = new JPopupMenu();
			
        //final String valueKey = getPropertyName() + ".value"; // todo: getPropertyName usually not set at this point!
            
        ActionListener a = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleMenuSelection(e);
                }};
            
        for (int i = 0; i < values.length; i++) {
            JMenuItem item;
            if (values[i] instanceof Action)
                item = new JMenuItem((Action)values[i]);
            else
                item = new JMenuItem();
            Icon icon = makeIcon(values[i]);
            if (icon != null)
                item.setIcon(makeIcon(values[i]));
            item.putClientProperty(mValueKey, values[i]);
            if (names != null)
                item.setText(names[i]);
            item.addActionListener(a);
            mPopup.add(item);
        }

        if (createCustom) {
            JMenuItem item = new JMenuItem("Custom..."); // todo: more control over this item
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleMenuSelection(runCustomChooser()); }});
            mPopup.add(item);
        }

        mEmptySelection = new JMenuItem();
        mEmptySelection.setVisible(false);
        mPopup.add(mEmptySelection);
    }
		
    protected void handleMenuSelection(ActionEvent e) {
        handleMenuSelection(((JComponent)e.getSource()).getClientProperty(mValueKey));
    }
    
    protected void handleMenuSelection(Object newPropertyValue) {
        if (newPropertyValue == null) // could be result of custom chooser
            return;
        Object oldValue = getPropertyValue();
        setPropertyValue(newPropertyValue);
        firePropertyChanged(oldValue, newPropertyValue);
        repaint();
    }
	
    /** fire a property change event even if old & new values are the same */
    protected void firePropertyChanged(Object oldValue, Object newValue)
    {
        PropertyChangeListener[] listeners = getPropertyChangeListeners();
        if (listeners.length > 0) {
            PropertyChangeEvent event = new PropertyChangeEvent(this, mPropertyName, oldValue, newValue);
            for (int i = 0; i< listeners.length; i++) {
                listeners[i].propertyChange(event);
            }
        }
    }
	
    public void paint(java.awt.Graphics g) {
        ((java.awt.Graphics2D)g).setRenderingHint
            (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }
	
    public String toString() {
        return getClass().getName() + "[" + mPropertyName + "]";
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
