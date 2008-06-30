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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;


/**
 * A property editor panel for LWLink's.
 *
 * @version $Revision: 1.52 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * 
 */

public class LinkToolPanel extends ToolPanel
{
	/** stroke size selector menu **/   protected StrokeStyleButton mStrokeStyleButton;
	/** stroke size selector menu **/   protected StrokeMenuButton mStrokeButton;
	
	 private static class StrokeStyleButton extends ComboBoxMenuButton<LWComponent.StrokeStyle>
	    {
	        
	        public StrokeStyleButton() {
	            buildMenu(LWComponent.StrokeStyle.class);
	            setFont(tufts.vue.VueConstants.FONT_SMALL); // does this have any effect?  maybe on Windows?
	            // set the size of the icon that displays the current value:
	            //setButtonIcon(new LineIcon(ButtonWidth-18, 3)); // height really only needs to be 1 pixel
	            ComboBoxRenderer renderer= new ComboBoxRenderer();
	    		setRenderer(renderer);
	    		this.setMaximumRowCount(10);
	    		
	        }
	        
	        public void displayValue(LWComponent.StrokeStyle style) {
	            setToolTipText("Stroke Style: " + style);
	            mCurrentValue = style;
                    setSelectedIndex(style.ordinal());
	        }
	        
	        /** returns the physical size of the outer button */
	        //protected Dimension getButtonSize() {
	            // todo: the interaction between button size and icon/width height should
	            // be clearer, and automatic!
	            //return new Dimension(ButtonWidth,ButtonHeight);
	        //}
	        
	        /** factory for superclass buildMenu -- these are the icons that will appear in the pull-down menu */
                @Override
	        protected Icon makeIcon(LWComponent.StrokeStyle style) {

                    LineIcon li = new LineIcon(22, 13);
                    //li.setColor(Color.gray);
                    li.setStroke(style.makeStroke(1));
                    return new GUI.ProxyEnabledIcon(li, StrokeStyleButton.this);
                    
// 	            LineIcon li = new LineIcon(24, 3);
// 	            li.setStroke(style.makeStroke(1));
// 	            return li;
	        }
	        
                //class ComboBoxRenderer extends JLabel implements ListCellRenderer {
	        class ComboBoxRenderer extends DefaultListCellRenderer {
	        	
                    public ComboBoxRenderer() {
                        setHorizontalAlignment(CENTER);
                        setVerticalAlignment(CENTER);

                        if (GUI.isMacAqua())
                            setBorder(BorderFactory.createEmptyBorder(3,2,3,4));
                        else
                            setBorder(BorderFactory.createEmptyBorder(5,5,2,2));
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
                        LWComponent.StrokeStyle a = (LWComponent.StrokeStyle) value;
	        		
                        //Icon icon = getRenderIcon(a); // why create this every time we paint?
                        //setIcon(icon);
                        setIcon(getIconForValue(a));
	        		
                        return this;
                    }
                    
//                     protected Icon getRenderIcon(LWComponent.StrokeStyle style) {
//                         LineIcon li = new LineIcon(20, 13);
//                         li.setStroke(style.makeStroke(1));
//                         return new GUI.ProxyEnabledIcon(li, StrokeStyleButton.this);
//                     }
                    
                    /** @return new icon for the given shape */
                    //  protected Icon makeIcon(RectangularShape shape) {
                    //      return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
                    //  }
	            
	        }        
		 
	    }
	    


    public boolean isPreferredType(Object o) {
        return o instanceof LWLink;
    }
    
    protected void buildBox()
    {
        //-------------------------------------------------------
        // Stroke Width menu
        //-------------------------------------------------------
        
        float[] strokeValues = VueResources.getFloatArray("strokeWeightValues");
        String[] strokeMenuLabels = VueResources.getStringArray("strokeWeightNames");
        mStrokeButton = new StrokeMenuButton(strokeValues, strokeMenuLabels, true, false);
        mStrokeButton.setPropertyKey(LWKey.StrokeWidth);
        //mStrokeButton.setButtonIcon(new LineIcon(16,16));
        mStrokeButton.setToolTipText("Stroke Width");
        //mStrokeButton.addPropertyChangeListener(this);
        
    	//-------------------------------------------------------
        // Stroke Style menu
        //-------------------------------------------------------
        
        mStrokeStyleButton = new StrokeStyleButton();
        mStrokeStyleButton.setPropertyKey(LWKey.StrokeStyle);
        //mStrokeStyleButton.addPropertyChangeListener(this);
        
        
        final Action[] LinkTypeActions = new Action[] { 
            Actions.LinkMakeStraight,
            Actions.LinkMakeQuadCurved,
            Actions.LinkMakeCubicCurved
        };
        
        Integer[] i = new Integer[4];
		i[0]= new Integer(0);
		i[1]= new Integer(1);
		i[2]= new Integer(2);
		i[3]= new Integer(3);
		
        AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkShape, LinkTypeActions);
        linkTypeMenu.setToolTipText("Link Style");
        //linkTypeMenu.addPropertyChangeListener(this);
        
        //LWCToolPanel.InstallHandler(mArrowStartButton, arrowPropertyHandler);
        //LWCToolPanel.InstallHandler(mArrowEndButton, arrowPropertyHandler);

        // We can't just rely on the each handler hanging free without knowing about it.
        // It works when the editor activates -- we can find which tool panel it's in
        // (up the AWT chain), and could find the right default state to work with
        // (node/link/text, etc).  But when a selection happens and the tool panel needs
        // to LOAD UP all these property editors, this is the only way we can know about
        // it...  Otherwise, we'd have to make every LWPropertyHandler a selection
        // listener in it's own right (tho this wouldn't be instance, given that every
        // single action in the system is also a selection listener!)
        
        //super.addEditor(arrowPropertyHandler);

        //mArrowStartButton.addActionListener(arrowPropertyHandler);
        //mArrowEndButton.addActionListener(arrowPropertyHandler);
        //mArrowStartButton.addItemListener(arrowPropertyHandler);
        //mArrowEndButton.addItemListener(arrowPropertyHandler);

        //mArrowStartButton.addItemListener(arrowPropertyHandler);
        //mArrowEndButton.addItemListener(arrowPropertyHandler);
        GridBagConstraints gbc = new GridBagConstraints();
        
        //addComponent(linkTypeMenu);
        	
        mBox.setLayout(new GridBagLayout());
    	
        gbc.insets = new Insets(1,3,1,1);
		gbc.gridx = 0;
		gbc.gridy = 0;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
		gbc.anchor = GridBagConstraints.EAST;
		JLabel strokeLabel = new JLabel("Stroke :");
		strokeLabel.setLabelFor(mStrokeStyleButton);
		strokeLabel.setForeground(new Color(51,51,51));
		strokeLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(strokeLabel,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
		gbc.anchor = GridBagConstraints.EAST;
		JLabel weightLabel = new JLabel("Weight :");
                weightLabel.setLabelFor(mStrokeButton);
		weightLabel.setForeground(new Color(51,51,51));
		weightLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(weightLabel,gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;    		
		gbc.insets = new Insets(1,1,1,3);
	//	gbc.gridwidth=1;
	//	gbc.gridheight=1;		
		//gbc.ipady=8;
		//gbc.ipadx=5;
		gbc.fill = GridBagConstraints.BOTH; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		mBox.add(mStrokeStyleButton,gbc);
		
		       

        	//gbc.ipady=2;
        	//gbc.ipadx=4;
        	gbc.gridx = 1;
    		gbc.gridy = 1;    		
    	//	gbc.gridwidth=1;
    	//	gbc.gridheight=1;
    		gbc.fill = GridBagConstraints.BOTH; // the label never grows
    		gbc.anchor = GridBagConstraints.WEST;
    		mStrokeButton.setSelectedIndex(1);
    		mBox.add(mStrokeButton,gbc);

    }
     
    //protected VueBeanState getDefaultState() { return VueBeans.getState(LWLink.setDefaults(new LWLink())); }
    protected LWComponent createDefaultStyle() {
        LWLink l = new LWLink();
        l.setLabel("defaultLinkStyle");
        return LWLink.SetDefaults(l);
    }
            
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.init(args);
        VueUtil.displayComponent(new LinkToolPanel());
    }
}
