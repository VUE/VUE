package tufts.vue;

import java.awt.Color;
import javax.swing.Icon;

/**
 * ColorMenuButton
 *
 * This class provides a popup menu of items that supports named color values
 * with a corresponding color swatch.
 *
 * @author csb
 * @author Scott Fraize
 * @version March 2004
 *
 */

public class ColorMenuButton extends MenuButton
{
    //private static BlobIcon  sIcon = new BlobIcon( 16,16, new Color(1,1,244) );
    private BlobIcon  sIcon = new BlobIcon(20,16, true);

    /** The currently selected Color item--if any **/
    protected Color mCurColor = new Color(0,0,0);
			
    /** the BlobIcon for the swatch **/
    private BlobIcon mBlobIcon = null;

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
     * Sets a the current color.
     */
    public void setColor(Color pColor) {
        mCurColor = pColor;
        if (mBlobIcon != null)
            mBlobIcon.setColor(pColor);
        if (pColor == null)
            mPopup.setSelected(super.mEmptySelection);
    }
	 
    /**
     * Gets the current color
     */
    public Color getColor() {
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
	
    /** factory for superclass buildMenu */
    protected Icon makeIcon(Object value) {
        return new BlobIcon(16,16, (Color) value);
    }

    protected Object runCustomChooser() {
        return javax.swing.JColorChooser.showDialog( this, "Select Custom Color", getColor());
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

