 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package tufts.vue;

import tufts.vue.gui.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.RectangularShape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.tufts.vue.preferences.implementations.ColorPreference;

/**
 * This creates an editor panel for LWNode's
 *
 * @version $Revision: 1.15 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
< */
 
public class FillToolPanel extends ToolPanel implements ComponentListener
{
    /** fill button **/
    //protected ColorMenuButton mFillColorButton;
    static ColorMenuButton mFillColorButton; // static hack till all the format tool code in once place
    /** stroke color editor button **/
    protected ColorMenuButton mStrokeColorButton;
    private final Color [] fillColors = VueResources.getColorArray("fillColorValues");
  //   private final String [] fillColorNames = VueResources.getStringArray("fillColorNames");
     private final Color[] strokeColors = VueResources.getColorArray("strokeColorValues");
     private final String[] strokeColorNames = VueResources.getStringArray("strokeColorNames");
     private final JLabel fillLabel = new JLabel("Fill: ");
     private final JLabel lineLabel = new JLabel("Line: ");
     private final ColorPreference fillPrefColor = ColorPreference.create(
				edu.tufts.vue.preferences.PreferenceConstants.FORMATTING_CATEGORY,
				"fillColor", 
				"Fill Color", 
				"Remember Fill Color?",
				VueResources.getColor("defaultFillColor"),
				false);
     
     private final ColorPreference strokePrefColor = ColorPreference.create(
				edu.tufts.vue.preferences.PreferenceConstants.FORMATTING_CATEGORY,
				"strokeColor", 
				"Stroke Color", 
				"Remember Stroke Color?",
				VueResources.getColor("defaultStrokeColor"),
				false);
    public FillToolPanel() {

        //setBorder(BorderFactory.createLineBorder(Color.red));
    }
    
    public void buildBox()
    {
    	 //-------------------------------------------------------
        // Fill Color menu
        //-------------------------------------------------------
        //TODO: need to come back here and move these tooltips into properties. -mikek         
        mFillColorButton = new ColorMenuButton(fillColors, true);
        mFillColorButton.setPropertyKey(LWKey.FillColor);
        mFillColorButton.setColor((Color)fillPrefColor.getValue());
        mFillColorButton.setToolTipText("Fill Color");
        //mFillColorButton.addPropertyChangeListener(this); // always last or we get prop change events for setup
        mFillColorButton.addPropertyChangeListener(new PropertyChangeListener() {	
                public void propertyChange(PropertyChangeEvent e) {
                    if (e instanceof LWPropertyChangeEvent)
                        fillPrefColor.setValue(mFillColorButton.getColor());
                }        	
        });
        //-------------------------------------------------------
        // Stroke Color menu
        //-------------------------------------------------------
        
        mStrokeColorButton = new ColorMenuButton(strokeColors, true);
        mStrokeColorButton.setPropertyKey(LWKey.StrokeColor);
        mStrokeColorButton.setColor((Color)strokePrefColor.getValue());
        //mStrokeColorButton.setButtonIcon(new LineIcon(16,16, 4, false));
        mStrokeColorButton.setToolTipText("Stroke Color");
        //mStrokeColorButton.addPropertyChangeListener(this);
        mStrokeColorButton.addPropertyChangeListener(new PropertyChangeListener() {	
                public void propertyChange(PropertyChangeEvent e) {
                    if (e instanceof LWPropertyChangeEvent)
                        strokePrefColor.setValue(mStrokeColorButton.getColor());							
                }        	
        });

        lineLabel.addMouseListener(new MouseAdapter() {
                // double-click on stroke color label swaps in with fill color
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() > 1 && e.getClickCount() % 2 == 0) {
                        final Color fill = mFillColorButton.getColor();
                        final Color stroke = mStrokeColorButton.getColor();
                        mFillColorButton.selectValue(stroke);
                        mStrokeColorButton.selectValue(fill);
                        
                    }
                }
            });
        
        fillLabel.setLabelFor(mFillColorButton);
 		fillLabel.setForeground(new Color(51,51,51));
 		fillLabel.setFont(tufts.vue.VueConstants.SmallFont);

 		lineLabel.setLabelFor(mStrokeColorButton);
 		lineLabel.setForeground(new Color(51,51,51));
 		lineLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		
 		if (tufts.Util.isMacPlatform())
    		buildBoxMac();
    	else
    		buildBoxWin();
 		
    	VUE.getFormatDock().addComponentListener(this);
    }
    public void buildBoxWin()
    {
  	    GridBagConstraints gbc = new GridBagConstraints();
     	gbc.insets = new Insets(6,3,0,0);    
        gbc.gridx = 0;
 		gbc.gridy = 0;    		
 		gbc.gridwidth = 1;
 		//gbc.gridheight=1;
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.EAST;
 		
 		
 		getBox().add(fillLabel,gbc);
         
        gbc.gridx = 0;
 		gbc.gridy = 1;    		
 		gbc.gridwidth = 1; // next-to-last in row
 	//	gbc.gridheight=1;
 		gbc.insets = new Insets(0,3,6,0);
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.EAST;
 		
        
 		getBox().add(lineLabel,gbc);
         
 	  	gbc.gridx = 1;
 	 	gbc.gridy = 0;    				
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.insets = new Insets(5,0,0,0);
 	 			
 		gbc.anchor = GridBagConstraints.WEST;
 		getBox().add(mFillColorButton, gbc);
 	
 		gbc.gridx = 1;
 	    gbc.gridy = 1;    				
 	    gbc.fill = GridBagConstraints.NONE; // the label never grows
 	    gbc.insets = new Insets(0,0,5,0);
	    gbc.anchor = GridBagConstraints.WEST;		
 	    getBox().add(mStrokeColorButton, gbc); 	        	 	         	                                           
    }
    public void buildBoxMac()
    {
  	   GridBagConstraints gbc = new GridBagConstraints();
     	gbc.insets = new Insets(3,3,5,3);    
        gbc.gridx = 0;
 		gbc.gridy = 0;    		
 		gbc.gridwidth = 1;
 		gbc.gridheight=1;
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.NORTHEAST; 		
 		getBox().add(fillLabel,gbc);
         
        gbc.gridx = 0;
 		gbc.gridy = 1;    		
 		gbc.gridwidth = 1; // next-to-last in row
 		gbc.gridheight=1;
 		gbc.insets = new Insets(8,3,1,3);
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.NORTHEAST; 		        
 		getBox().add(lineLabel,gbc);
         
	  	gbc.gridx = 1;
	 	gbc.gridy = 0;    				
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.insets = new Insets(1,2,4,3);
		gbc.anchor = GridBagConstraints.SOUTHWEST;
   		getBox().add(mFillColorButton, gbc);

   		gbc.gridx = 1;
 	    gbc.gridy = 1;    				
 	    gbc.fill = GridBagConstraints.NONE; // the label never grows
 	    gbc.insets = new Insets(4,2,1,3);
	    gbc.anchor = GridBagConstraints.SOUTHWEST;		
 	    getBox().add(mStrokeColorButton, gbc); 	        	     	                                           
    }
    
    public boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    /*
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

        /** @param o an instance of RectangularShape *
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
        	 /* @return new icon for the given shape *
          //  protected Icon makeIcon(RectangularShape shape) {
          //      return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
          //  }
            
        }        
	 
    }
*/

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

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentMoved(ComponentEvent arg0) {
		   if ((mFillColorButton != null) && (mFillColorButton.getPopupWindow().isVisible()))
			   mFillColorButton.getPopupWindow().setVisible(false);
		   
		   if ((mStrokeColorButton != null) && (mStrokeColorButton.getPopupWindow().isVisible()))
			   mStrokeColorButton.getPopupWindow().setVisible(false);		    		    		    		
	}

	public void componentResized(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}