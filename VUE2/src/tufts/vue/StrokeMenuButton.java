package tufts.vue;

import java.awt.Color;
import javax.swing.Icon;

/**
 * StrokeMenuButton
 *
 * This class provides a popup button selector component for stroke widths.
 *
 * @author Scott Fraize
 * @version March 2004
 **/
public class StrokeMenuButton extends MenuButton
{
    static private int sIconWidth = 24;
    static private int sIconHeight = 16;
	
    /** The value of the currently selected stroke width item--if any **/
    protected float mStroke = 1;
			
    //protected ButtonGroup mGroup = new ButtonGroup();
    
    public StrokeMenuButton(float [] pValues, String [] pMenuNames, boolean pGenerateIcons, boolean pHasCustom)
    {
        Float[] values = new Float[pValues.length];
        // I really hope this is one if the things java 1.5 has automatic support for...
        for (int i = 0; i < pValues.length; i++)
            values[i] = new Float(pValues[i]);

        
        buildMenu(values, pMenuNames, false);
    }

    public StrokeMenuButton() {}
	
    public void setStroke(float width) {
        setToolTipText("Stroke Width: " + width);
        mStroke = width;
	 	
        // if we are using a LineIcon, update it
        if (getIcon() instanceof LineIcon ) {
            LineIcon icon = (LineIcon) getIcon();
            icon.setWeight(mStroke);
        }
    }
    public float getStroke() {
        return mStroke;
    }
    public void setPropertyValue(Object o) {
        setStroke(o == null ? 0f : ((Float)o).floatValue());
    }
    public Object getPropertyValue() {
        return new Float(getStroke());
    }

    /** factory for superclass buildMenu */
    protected Icon makeIcon(Object value) {
        return new LineIcon(sIconWidth, sIconHeight, ((Float)value).floatValue());
    }
    
    /*
      Will need to enhance MenuButton to have an overridable addMenuItem or somethign
      if we still want to support the button group.
      mGroup.add( item);
      As WELL as an item menu factory, so it could return JRadioButtonMenuItem's
      instead (and in that method we could add it to the group for us here).
    */

}

