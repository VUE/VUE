package tufts.vue;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ColorMenuButton
 *
 * This class provides a popup radio button selector component.
 * It is used for the main tool bar tool
 *
 * @author csb
 * @author Scott Fraize
 * @version March 2004
 *
 */

public class ColorMenuButton extends MenuButton
{
    //static ImageIcon sIcon = TestResources.getImageIcon( "icon");
	
    /** FIX:  move this to Resources **/
    private static final String sPickerTitle = "Select Custom Color";
	
    //private static BlobIcon  sIcon = new BlobIcon( 16,16, new Color(1,1,244) );
    private BlobIcon  sIcon = new BlobIcon(20,16, true);

    /** The currently selected Color item--if any **/
    protected Color mCurColor = new Color(0,0,0);
			
    /** the BlobIcon for the swatch **/
    private BlobIcon mBlobIcon = null;

    private JMenuItem mNoneSelected = new ColorMenuItem(Color.black);
	
    /**
     * Constructor
     *
     *  Creates a new ColorMenuButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of ColorMenuButtonItems for the menu.
     **/
    public ColorMenuButton(Color [] pColors, String [] pMenuNames, boolean pHasCustom) {
        setIcon(sIcon);
        buildMenu(pColors, pMenuNames, pHasCustom);
    }
	
    public ColorMenuButton(Color [] pColors, String  [] pMenuNames) {
        this( pColors, pMenuNames, false);
    }
	
    public ColorMenuButton( Color [] pColors) {
        this( pColors, null, false);
    }
	
    /**
     * setContext
     * Sets a the current color.
     **/
    public void setColor(Color pColor) {
        mCurColor = pColor;
        if (mBlobIcon != null)
            mBlobIcon.setColor(pColor);
        if (pColor == null)
            mPopup.setSelected(mNoneSelected);
    }
	 
    /**
     * getColor
     * Gets teh current color
     **/
    public Color  getColor() {
        return mCurColor;
    }

    public void setPropertyValue(Object o) {
        setColor((Color)o);
    }
	 
    public Object getPropertyValue() {
        return getColor();
    }
	 
    public void setIcon( Icon pIcon) {
        super.setIcon(pIcon);
        if (pIcon instanceof BlobIcon)
            mBlobIcon = (BlobIcon) pIcon;
        else
            mBlobIcon = null;
    }
	
    /**
     * buildMenu
     * This method builds the list of colors for the popup
     *
     **/
    private void buildMenu( Color [] pColors, String [] pNames,  boolean pHasCustom) {
        super.mPopup = new JPopupMenu();
			
        if (pColors != null)
            addColors( pColors, pNames);

        mNoneSelected.setVisible(false);
        mPopup.add(mNoneSelected);

        if( pHasCustom ) {
            // add the last custom color editor
			 	
            ColorMenuItem item = new ColorMenuItem( true);
            mPopup.add( item);
        }
    }
		
    private void addColors( Color [] pColors, String [] pNames)
    {
        final String valueKey = getPropertyName() + ".value";
            
        ActionListener a = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleMenuSelection(((JComponent)e.getSource()).getClientProperty(valueKey));
                }};
            
        for (int i = 0; i < pColors.length; i++) {
            //ColorMenuItem item = null;
            //item = new ColorMenuItem( pColors[i] ) ;
            JMenuItem item = new JMenuItem();
            item.setIcon(new BlobIcon(16,16, pColors[i]));
            item.addActionListener(a);
            item.putClientProperty(valueKey, pColors[i]);
            if( pNames != null)
                item.setText( pNames[i]);
            mPopup.add( item);
        }
    }

    // should be able to replace this class with a putClientProperty on the JMenuItem's
    // for the color, etc, and a single action listener for them.  And if we make
    // the property general (e.g. "vuePropertyValue") we can put all the code in MenuButton.
    private class ColorMenuItem extends JMenuItem implements ActionListener {
	
        /** the color this item represents **/
        private Color mMenuColor = null;
		
        /** is this a custom color item? **/
        private boolean mIsCustomItem = false;
		
        /**
         * Constructor
         * @param the color for this item.
         **/
        public ColorMenuItem( Color pColor) {
            super();
            mMenuColor = pColor;
            setIcon(new BlobIcon( 16,16, pColor));
            addActionListener( this);
        }
        public ColorMenuItem( boolean pIsCustom) {
            super("Custom...");
            mIsCustomItem = pIsCustom;
            //setIcon(new BlobIcon( 16,16, null));
            addActionListener( this);
        }
		
        public void actionPerformed( ActionEvent pEvent)
        {
            Color oldColor = getColor();
            
            if( mIsCustomItem ) {
                Color newColor = JColorChooser.showDialog( this, sPickerTitle, oldColor );
                if( newColor != null  ) {
                    mMenuColor =  newColor;
                    handleMenuSelection(newColor);
                }
            }
            else {
                handleMenuSelection(mMenuColor);
            }
        }
    }

    ColorMenuButton() { this(sTestColors,  sTestNames, true); }
    private static String[] sTestNames = { "Black",
                                           "White",
                                           "Red",
                                           "Green",
                                           "Blue" }; 
    private static Color[] sTestColors = { new Color(0,0,0),
                                           new Color(255,255,255),
                                           new Color(255,0,0),
                                           new Color(0,255,0),
                                           new Color(0,0,255)  };
	
    
}

