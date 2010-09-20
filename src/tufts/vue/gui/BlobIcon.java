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

import tufts.vue.DEBUG;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * BlobIcon Class
 *  This clas is used for creating dynamic color blobs with or without
 * an overlay image.  It can be used to create swatches of color for things
 * like menu icons and buttons.  It is useful in color menus for selecting
 * custom colors where you require an icon in a UI widget.
 *
 **/

public class BlobIcon implements Icon
{
    /** the icon height **/
    private int mHeight = 0;
	
    /** the icon width **/
    private int mWidth = 0;
	
    /** the blob color **/
    protected Color mColor = null;
	
    /** the overlay icon, if any **/
    private Icon mOverlay = null;
	
    /** paint a darkened color border? */
    private boolean mPaintBorder = true;
    
    public BlobIcon() {}
    
    public BlobIcon( int pWidth,int  pHeight) {
        this( pWidth, pHeight, null, null, true);
    }
    
    public BlobIcon( int pWidth,int  pHeight, boolean paintBorder) {
        this( pWidth, pHeight, null, null, paintBorder);
    }
	
    public BlobIcon( int pWidth, int pHeight, Color pColor) {
        this( pWidth, pHeight, pColor, null, true);
    }
	
	
    /**
     * This constrocutor makes a BlobIcon with the 
     * @param pWidth - the icon width
     * @param pHeight - the icon height
     * @param pColor - the blob swatch color
     * @param pOverlay - an overlay icon to draw over the blob
     **/
    public BlobIcon(int pWidth, int pHeight, Color pColor, Icon pOverlay, boolean paintBorder) {
        mWidth = pWidth;
        mHeight = pHeight;
        mColor = pColor;
        mOverlay = pOverlay;
        mPaintBorder = paintBorder;
    }
	
	
	
    //////////////
    // Methods
    ///////////////
	
	
    /**
     * getIconHeight
     * Implementation of Icon interface
     **/
    public int getIconHeight() {
        return mHeight;
    }
	
    /**
     * getIconWidth
     * Implementation of Icon interface.
     * @see java.awt.Icon
     **/
    public int getIconWidth() {
        return mWidth;
    }
	
    /**
     * setIconSize
     * A convenience method to set the height an dwidth
     **/
    public void setIconSize( int pWidth, int pHeight) {
        mWidth = pWidth;
        mHeight = pHeight;
    }
	
    public void setColor(Color c) {
        if (DEBUG.TOOL) System.out.println(this + " setColor " + c);
        mColor = c;
    }
	
    public Color getColor() {
        return mColor;
    }
	
	
    /**
     * setIconHeight
     * Sets the icon height
     **/
    public void setIconHeight( int pHeight) {
        mHeight = pHeight;
    }
	
    /**
     * setIconWidth
     * Sets the icon's width.
     **/
    public void setIconWidth( int pWidth) {
        mWidth = pWidth;
    }
	
	
    /**
     * setOverlya
     * Sets the overlay icon to draw an overlay over the color blob
     * This is a convienience for transparent icons.
     * Also, the BlobICon will resize to size of the overlay
     * icon.  Note, if the overlay has no transparent pixels,
     * the BlobIcon is nothing more than a normal icon with
     * the overlay icon being painted.
     *
     * @param pOverlay - the overlay icon (should have transparent pixels).
     **/
    public void setOverlay( Icon pOverlay) {
        mOverlay = pOverlay;
        if( pOverlay != null) {
            setIconSize( pOverlay.getIconWidth(), pOverlay.getIconHeight() );
        }
    }
	
    /** 
     * getOverlay()
     * Gets teh overlay icon.
     **/
    public Icon getOverlay() {
        return mOverlay;
    }
	
    /**
     * paintIcon
     * Implementation of Icon interface
     * This paints a color blob of the icon size at the 
     * specified coords in the specified graphics.
     * If an overlay icon is set, the overlay icon will be
     * painted on top of the blob, thus providing a framing
     * system.
     * @see java.awt.Icon
     **/

    private static final RoundRectangle2D BlobShape = new RoundRectangle2D.Float();
    private static final BasicStroke BlobStroke = new BasicStroke(0.75f);
    public void paintIcon(Component c, Graphics _g, int x, int y) {
        //if (DEBUG.TOOL) System.out.println(this + " PAINT on " + c);

        final Graphics2D g = (Graphics2D) _g;
		
        final Color oldColor = g.getColor();
        final Color color;
        final boolean isNoFill = (mColor == null || mColor.getAlpha() == 0);
        if (isNoFill)
            color = c.getBackground();
        else
            color = mColor;

        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        BlobShape.setRoundRect(x, y, mWidth-1, mHeight-1, 7, 7);
        
        if (isNoFill == false) {
            g.setColor(color);
            g.fill(BlobShape);
            //g.fillRect(x,y, mWidth, mHeight);
        }
        if (mPaintBorder) {
            if (isNoFill)
                g.setColor(Color.black);
            else
                g.setColor(color.darker().darker());
            g.setStroke(BlobStroke);
            g.draw(BlobShape);
            //g.drawRect(x,y, mWidth-1, mHeight-1);
        }
        if (isNoFill) {
            g.setColor(Color.black);
            g.drawLine(x+2,y + mHeight-3,  x+mWidth-3, y+2);
            //g.drawLine(x,y, x+mWidth-1, y+mHeight-1);
        }
            
        if( mOverlay != null) {
            mOverlay.paintIcon( c, g, x, y);
            //g.drawImage( mOverlay.getImage(), x, y, null);
        }
        g.setColor(oldColor);
    }

    public String paramString() {
        String s = mWidth + "x" + mHeight;
        if (mColor != null) 
            s += " rgb=" + mColor.getRed()
                + "," + mColor.getGreen()
                + "," + mColor.getBlue();
        return s;
    }

    public String toString() {
        return getClass().getName() + "[" + paramString() + "]";
        //return "BlobIcon[" + paramString() + "]";
    }
}
