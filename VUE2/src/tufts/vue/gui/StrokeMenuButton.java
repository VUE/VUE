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
 * @version $Revision: 1.15 $ / $Date: 2010-02-03 19:15:47 $ / $Author: mike $
 *
 **/
public class StrokeMenuButton extends ComboBoxMenuButton<Float>
{
    static private int sIconWidth = 24;
    static private int sIconHeight = 16;
	
    public StrokeMenuButton(float[] pValues, String [] pMenuNames, boolean pGenerateIcons, boolean pHasCustom)
    {
        Float[] values = new Float[pValues.length];
        for (int i = 0; i < pValues.length; i++)
            values[i] = new Float(pValues[i]);

        buildMenu(values, pMenuNames, false);
        setFont(tufts.vue.VueConstants.FONT_SMALL);
        setRenderer(new ComboBoxRenderer());
        this.setMaximumRowCount(10);
        mCurrentValue = new Float(1);
    }

    public StrokeMenuButton() {
        mCurrentValue = new Float(1);
    }
	
    public void displayValue(Float width) {
        setToolTipText("Stroke Width: " + width);
        if (width == null)
            mCurrentValue = new Float(1);
        else
            mCurrentValue = width;
        setSelectedItem(mCurrentValue);
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
    		Color bg = GUI.getTextHighlightColor();
    		if (isSelected) {
    			setBackground(bg);
    			setForeground(Color.black);
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
    }        
}

