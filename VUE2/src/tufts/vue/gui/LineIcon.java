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
import javax.swing.*;

/**
 * LineIcon Class
 *
 * This class extends the BlobIcon and is used to draw a line
 * of a dynamic size horizontally accross the icon.
 *
 * The LineIcon class is used by the StrokeMenuButton class to
 * generate visual stroke weights in a popup menu ands for autogeneration,
 * as well as the ColorMenuButton class for a colored stroke button
 *
 * @see tufts.vue.StrokeMenuButton
 * @see tufts.vue.ColorMenuButton
 *
 **/
public class LineIcon extends BlobIcon
{
    /** the weight of the stroke **/
    private float mWeight;	
    private boolean mDiagonal;
    private BasicStroke stroke = new BasicStroke();
	
    public LineIcon() {
        super();
    }
	
    public LineIcon( int pWidth,int  pHeight) {
        this( pWidth, pHeight, null, null);
    }
	
    public LineIcon( int pWidth, int pHeight, Color pColor) {
        this( pWidth, pHeight, pColor, null);
    }
	
    public LineIcon( int pWidth, int pHeight, float pWeight) {
        this(pWidth, pHeight, pWeight, false);
    }
	
    public LineIcon( int pWidth,int  pHeight, float pWeight, boolean diagonal) {
        this( pWidth, pHeight, null, null);
        setWeight(pWeight);
        mDiagonal = diagonal;
    }
	
	
    /**
     * This constrocutor makes a LineIcon with the 
     * @param pWidth - the icon width
     * @param pHeight - the icon height
     * @param pColor - the blob swatch color
     * @param pOverlay - an overlay icon to draw over the blob
     **/
    public LineIcon(int pWidth, int pHeight, Color pColor, Icon pOverlay) {
        super( pWidth, pHeight, pColor, pOverlay, false);
        mWeight = 0;
    }
	
    /** Sets the stroke weight **/
    public void setWeight(float pWeight) {
        mWeight = pWeight;
        stroke = new BasicStroke(mWeight);
    }
	
    /** Gets the stroke weight **/
    public float getWeight() {
        return mWeight;
    }
	
	
    /**
     * This paints a line icon with width of current weight,
     * in the current color if one is set, or black otherwise.
     * If an overlay icon is set, the overlay icon will be
     * painted on top.
     * @see java.awt.Icon
     **/
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        if (getWeight() > 0) {
            if (getColor() == null)
                g.setColor(Color.black);
            else
                g.setColor(getColor());
            int weight = (int) getWeight();
            if (mDiagonal) {
                //g.setStroke
                g.drawLine(x, getIconHeight(), getIconWidth(), y);
            } else {
                int y1 = y + (getIconHeight() - weight) / 2;
                g.fillRect(x, y1, getIconWidth(), weight );
            }
			
        }
        if (getOverlay() != null) {
            getOverlay().paintIcon( c, g, x, y);
            //g.drawImage( getOverlay().getImage(), x, y, null);
        }
    }

    public String paramString() {
        return super.paramString() + " weight=" + mWeight;
    }
    public String toString() {
        return "LineIcon[" + paramString() + "]";
    }
    //public String toString() {return "LineIcon[" + (mColor==null?"":(mColor + ", ")) + mWeight + "]";}
    
}
