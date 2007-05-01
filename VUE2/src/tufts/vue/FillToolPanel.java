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

package tufts.vue;

import tufts.vue.gui.*;
import tufts.vue.beans.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.geom.RectangularShape;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.*;

/**
 * This creates an editor panel for LWNode's
 *
 * @version $Revision: 1.4 $ / $Date: 2007-05-01 22:36:54 $ / $Author: sfraize $
 */
 
public class FillToolPanel extends ToolPanel
{
	 /** fill button **/                 protected ColorMenuButton mFillColorButton;
	 /** stroke color editor button **/  protected ColorMenuButton mStrokeColorButton;
	 
    public FillToolPanel() {
    
        //setBorder(BorderFactory.createLineBorder(Color.red));
    }
    
    public void buildBox()
    {
  	  //-------------------------------------------------------
        // Fill Color menu
        //-------------------------------------------------------
        //TODO: need to come back here and move these tooltips into properties. -mikek         
        Color [] fillColors = VueResources.getColorArray("fillColorValues");
        String [] fillColorNames = VueResources.getStringArray("fillColorNames");
        mFillColorButton = new ColorMenuButton(fillColors, fillColorNames, true);
        mFillColorButton.setPropertyKey(LWKey.FillColor);
        mFillColorButton.setColor(VueResources.getColor("defaultFillColor"));
        mFillColorButton.setToolTipText("Fill Color");
        //mFillColorButton.addPropertyChangeListener(this); // always last or we get prop change events for setup
         
        //-------------------------------------------------------
        // Stroke Color menu
        //-------------------------------------------------------
        
        Color[] strokeColors = VueResources.getColorArray("strokeColorValues");
        String[] strokeColorNames = VueResources.getStringArray("strokeColorNames");
        mStrokeColorButton = new ColorMenuButton(strokeColors, strokeColorNames, true);
        mStrokeColorButton.setPropertyKey(LWKey.StrokeColor);
        mStrokeColorButton.setColor(VueResources.getColor("defaultStrokeColor"));
        //mStrokeColorButton.setButtonIcon(new LineIcon(16,16, 4, false));
        mStrokeColorButton.setToolTipText("Stroke Color");
        //mStrokeColorButton.addPropertyChangeListener(this);
       GridBagConstraints gbc = new GridBagConstraints();
     	gbc.insets = new Insets(3,3,5,3);    
        gbc.gridx = 0;
 		gbc.gridy = 0;    		
 		gbc.gridwidth = 1;
 		gbc.gridheight=1;
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.NORTHEAST;
 		
 		JLabel fillLabel = new JLabel("Fill: ");
                fillLabel.setLabelFor(mFillColorButton);
 		fillLabel.setForeground(new Color(51,51,51));
 		fillLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		getBox().add(fillLabel,gbc);
         
        gbc.gridx = 0;
 		gbc.gridy = 1;    		
 		gbc.gridwidth = 1; // next-to-last in row
 		gbc.gridheight=1;
 		gbc.insets = new Insets(8,3,1,3);
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.NORTHEAST;
 		JLabel lineLabel = new JLabel("Line: ");
                lineLabel.setLabelFor(mStrokeColorButton);
 		lineLabel.setForeground(new Color(51,51,51));
 		lineLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		getBox().add(lineLabel,gbc);
         
 		  if (addComponent(mFillColorButton))
 	        {
 			  	gbc.gridx = 1;
 			 	gbc.gridy = 0;    				
 	 			gbc.fill = GridBagConstraints.NONE; // the label never grows
 	 			gbc.insets = new Insets(1,2,4,3);
 	 			gbc.anchor = GridBagConstraints.SOUTHWEST;
 	    		getBox().add(mFillColorButton, gbc);
 	        }
 	        if (addComponent(mStrokeColorButton))
 	        {
 	        	gbc.gridx = 1;
 	      		gbc.gridy = 1;    				
 	      		gbc.fill = GridBagConstraints.NONE; // the label never grows
 	      		gbc.insets = new Insets(4,2,1,3);
	      		gbc.anchor = GridBagConstraints.SOUTHWEST;		
 	    		getBox().add(mStrokeColorButton, gbc); 	        	
 	        }
     	
        
         
        
                  
    }
    public boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    static class ShapeMenuButton extends VueComboMenu<RectangularShape>
    {
        public ShapeMenuButton() {
            super(LWKey.Shape, NodeTool.getTool().getShapeSetterActions());
            setToolTipText("Node Shape");
            ComboBoxRenderer renderer= new ComboBoxRenderer();
    		setRenderer(renderer);
    		this.setMaximumRowCount(10);
    		//setEnabled(false);
    		
        }

        protected Dimension getButtonSize() {
            return new Dimension(37,22);
        }

        /** @param o an instance of RectangularShape */
        public void displayValue(RectangularShape shape) {
            if (DEBUG.TOOL) System.out.println(this + " displayValue " + shape.getClass() + " [" + shape + "]");

            if (mCurrentValue == null || !mCurrentValue.getClass().equals(shape.getClass())) {
                mCurrentValue = shape;

                // This is inefficent in that we there are already shape icons out there (produced
                // in getShapeSetterActions()) that we could use, but doing it this way (creating a
                // new one every time) will allow for ANY rectangular shape to display properly in
                // the tool menu, even it is a deprecated shape or non-standard shape (not defined
                // as a standard from for the node tool in VueResources.properties).  (This is
                // especially in-effecient if you look at what setButtonIcon does in MenuButton: it
                // creates first a proxy icon, and then creates and installs a whole set of
                // VueButtonIcons for all the various states the button can take, for a totale of 7
                // objects every time we do this (1 for the clone, 1 for proxy, 5 via
                // VueButtonIcon.installGenerated)
                
                //setButtonIcon(makeIcon(shape));
            }
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
        		Action a = (Action) value;
                Icon icon = (Icon) a.getValue(Action.SMALL_ICON);
                value = a.getValue(ValueKey);
                if (icon == null)
                    icon = makeIcon(value);
                if (icon != null)
                    setIcon(new MenuProxyIcon(icon));
        		
             //   System.out.println("ICON SIZE NODE " + icon.getIconHeight() + " " + icon.getIconWidth());
                this.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

        		return this;
        	}
        	/** @return new icon for the given shape */
            protected Icon makeIcon(Object value) {
                RectangularShape shape = (RectangularShape) value;
                return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
            }
        	 /** @return new icon for the given shape */
          //  protected Icon makeIcon(RectangularShape shape) {
          //      return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
          //  }
            
        }        
	 
    }

    /*
    static class LinkMenuButton extends VueComboMenu<Object>
    {
        public LinkMenuButton() {
            super(LWKey.LinkCurves, LinkTool.getTool().getSetterActions());
            setToolTipText("Node Shape");
            ComboBoxRenderer renderer= new ComboBoxRenderer();
    		setRenderer(renderer);
    		this.setMaximumRowCount(10);
    		//setEnabled(false);
    		
        }

        protected Dimension getButtonSize() {
            return new Dimension(37,22);
        }

        public void displayValue(Object linkShape) {
           // if (DEBUG.TOOL) System.out.println(this + " displayValue " + shape.getClass() + " [" + shape + "]");

            //if (mCurrentValue == null || !mCurrentValue.getClass().equals(shape.getClass())) {
            //    mCurrentValue = shape;

                // This is inefficent in that we there are already shape icons out there (produced
                // in getShapeSetterActions()) that we could use, but doing it this way (creating a
                // new one every time) will allow for ANY rectangular shape to display properly in
                // the tool menu, even it is a deprecated shape or non-standard shape (not defined
                // as a standard from for the node tool in VueResources.properties).  (This is
                // especially in-effecient if you look at what setButtonIcon does in MenuButton: it
                // creates first a proxy icon, and then creates and installs a whole set of
                // VueButtonIcons for all the various states the button can take, for a totale of 7
                // objects every time we do this (1 for the clone, 1 for proxy, 5 via
                // VueButtonIcon.installGenerated)
                
                //setButtonIcon(makeIcon(shape));
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
                Action a = (Action)value;
                Icon icon = (Icon)a.getValue(Action.SMALL_ICON);
             //   System.out.println("ICON SIZE LINK " + icon.getIconHeight() + " " + icon.getIconWidth());
              //  if (icon != null)
                    //setIcon(new MenuProxyIcon(icon));
                this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
                setIcon(icon);
        		return this;
        	}
     
            
        }
	 
    }
  
    */

    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.init(args);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
}
