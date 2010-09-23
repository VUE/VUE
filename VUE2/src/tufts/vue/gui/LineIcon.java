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
        stroke = new BasicStroke(mWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    }
	
    /** Gets the stroke weight **/
    public float getWeight() {
        return mWeight;
    }

    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
        mWeight = stroke.getLineWidth();
    }
	
	
    /**
     * This paints a line icon with width of current weight,
     * in the current color if one is set, or black otherwise.
     * If an overlay icon is set, the overlay icon will be
     * painted on top.
     * @see java.awt.Icon
     **/
    public void paintIcon(Component c, Graphics _g, int x, int y)
    {
        final Graphics2D g = (Graphics2D) _g;
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        if (getWeight() > 0 || stroke != null) {
            if (getColor() == null)
                g.setColor(Color.black);
            else
                g.setColor(getColor());
            int weight = (int) getWeight();
            if (mDiagonal) {
                g.drawLine(x, getIconHeight(), getIconWidth(), y);
            } else {
                if (weight < 1 && getWeight() > 0) {
                    // todo: hack for stroke between 0 & 1
                    g.setColor(Color.gray);
                    weight = 1;
                }
                //int y1 = y + (getIconHeight() - weight) / 2;
                //g.fillRect(x, y1, getIconWidth(), weight );
                if (mWeight > 0) {
                    float midY = y + getIconHeight() / 2;
                    java.awt.geom.Line2D line = new java.awt.geom.Line2D.Float(x, midY, x + getIconWidth(), midY);
                    g.setStroke(this.stroke);
                    g.draw(line);
                }
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
    //public String toString() { return "LineIcon[" + paramString() + "]"; }
    
}
