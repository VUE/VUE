package tufts.vue;

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
	
    public LineIcon() {
        super();
    }
	
    public LineIcon( int pWidth,int  pHeight) {
        this( pWidth, pHeight, null, null);
    }
	
    public LineIcon( int pWidth,int  pHeight, int pWeight) {
        this( pWidth, pHeight, null, null);
        setWeight(pWeight);
    }
	
    public LineIcon( int pWidth, int pHeight, Color pColor) {
        this( pWidth, pHeight, pColor, null);
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
    }
	
    /** Gets the stroke weight **/
    public float getWeight() {
        return mWeight;
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
    public void paintIcon( Component c, Graphics g, int x, int y)
    {
        /*
        Color color = getColor();
        if (color == null)
            color = c.getBackground();
        g.setColor( color);
        g.fillRect(x,y, getIconWidth(), getIconHeight());
        */
			
        if (getWeight() > 0 ) {
            g.setColor(getColor());
            int weight = (int) getWeight();
            int y1 =  y+ ( (getIconHeight() - weight) /2);
            g.fillRect( x, y1, getIconWidth(), weight );
			
        }
        if (getOverlay() != null) {
            getOverlay().paintIcon( c, g, x, y);
            //g.drawImage( getOverlay().getImage(), x, y, null);
        }
    }
}
