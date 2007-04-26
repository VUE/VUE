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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import tufts.vue.LWComponent;


/**
 * StrokeMenuButton
 *
 * This class provides a popup button selector component for stroke widths.
 *
 * @author Scott Fraize
 * @version March 2004
 **/
public class StrokeMenuButton extends ComboBoxMenuButton<Float>
{
    static private int sIconWidth = 24;
    static private int sIconHeight = 16;
	
    /** The value of the currently selected stroke width item--if any **/
    protected float mStroke = 1;
			
    //protected ButtonGroup mGroup = new ButtonGroup();
    
    public StrokeMenuButton(float[] pValues, String [] pMenuNames, boolean pGenerateIcons, boolean pHasCustom)
    {
        Float[] values = new Float[pValues.length];
        // I really hope that java 1.5 auto-boxing will handle this type of thing for us...
        for (int i = 0; i < pValues.length; i++)
            values[i] = new Float(pValues[i]);

        buildMenu(values, pMenuNames, false);
        setFont(tufts.vue.VueConstants.FONT_SMALL);
        ComboBoxRenderer renderer= new ComboBoxRenderer();
		setRenderer(renderer);
		this.setMaximumRowCount(10);
	
    }

    public StrokeMenuButton() {}
	
    public void setStroke(float width) {
        setToolTipText("Stroke Width: " + width);
        mStroke = width;	 	   
    }
    public float getStroke() {
        return mStroke;
    }
    
    public void displayValue(Float f) {
        setStroke(f); // what does auto-box do if it's null???
        //setStroke(f == null ? 0f : f);
        //setStroke(o == null ? 0f : ((Float)o).floatValue());
    }
    public Float produceValue() {
        return getStroke();
    }

    /** factory for superclass buildMenu */
    protected Icon makeIcon(Float value) {
        return new LineIcon(sIconWidth, sIconHeight, value);
    }
    
    class ComboBoxRenderer extends JLabel implements ListCellRenderer {
    	
    	public ComboBoxRenderer() {
    		setOpaque(true);
    		setHorizontalAlignment(CENTER);
    		setVerticalAlignment(CENTER);

    	}

    
    	public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
    		
    		if (isSelected) {
    			setBackground(list.getSelectionBackground());
    			setForeground(list.getSelectionForeground());
    		} else {
    			setBackground(Color.white);
    			setForeground(list.getForeground());
    		}
    	     		
    		//Set the icon and text.  If icon was null, say so.        		
    		Float a = (Float) value;    		
            //Icon    icon = makeIcon(a);
    		setText(a.intValue() + "px");
            //setIcon(icon);
    		
          //  this.setBorder(BorderFactory.createEmptyBorder(0,0, 1, 1));
    		this.setBorder(BorderFactory.createEmptyBorder(4,1, 3, 3));
    		return this;
    	}
    	protected Icon makeIcon(Float value) {
            return new LineIcon(sIconWidth, sIconHeight, value);
        }	 
    }        
}

