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
    /** The currently selected Color item--if any **/
    protected Color mCurColor = new Color(0,0,0);
			
    /**
     *  Creates a new ColorMenuButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of ColorMenuButtonItems for the menu.
     **/
    public ColorMenuButton(Color[] pColors, String[] pMenuNames, boolean pHasCustom) {
        // create default color swatch icon: override with setButtonIcon if want different
        setButtonIcon(new BlobIcon(10,10, true)); // can we live with no default? clean up init style...
        buildMenu(pColors, pMenuNames, pHasCustom);
    }
	
    public ColorMenuButton(Color[] pColors, String [] pMenuNames) {
        this(pColors, pMenuNames, false);
    }
	
    public ColorMenuButton(Color[] pColors) {
        this(pColors, null, false);
    }
	
    /**
     * Sets a the current color.
     */
    public void setColor(Color c) {
        if (DEBUG.TOOL) System.out.println(this + " setColor " + c);
        mCurColor = c;
        Icon i = getButtonIcon();
        if (i instanceof BlobIcon)
            ((BlobIcon)i).setColor(c);
        if (c == null)
            mPopup.setSelected(super.mEmptySelection);
        repaint();
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
	
    /** factory for superclass buildMenu */
    protected Icon makeIcon(Object value) {
        return new BlobIcon(16,16, (Color) value);
    }

    protected Object runCustomChooser() {
        return tufts.vue.VueUtil.runColorChooser("Select Custom Color", getColor(), tufts.vue.VUE.getFrame());
        // todo: set up own listeners for color change in chooser
        // --that way way can actually tweak color on map as they play
        // with it in the chooser
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

