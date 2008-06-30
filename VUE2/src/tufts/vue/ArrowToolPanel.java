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
 * @version $Revision: 1.10 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * 
 */

public class ArrowToolPanel extends ToolPanel
{
	int headArrowState = LWLink.ARROW_NONE;
	int tailArrowState = LWLink.ARROW_TAIL;
	/**
	 * This isn't here for good but until we do something else, this is here so we
	 * don't loose the functionality with the loss of the contextual menu.
	 * @author mkorcy01
	 *
	 */
	private static class ArrowStyleButton extends JComboBox
    {
        Icon[] imageIcons;
        
        public ArrowStyleButton(Object[] a, boolean isHead) {
        	super(a);
            setFont(tufts.vue.VueConstants.FONT_SMALL); // does this have any effect?  maybe on Windows?
            // set the size of the icon that displays the current value:
            //setButtonIcon(new LineIcon(ButtonWidth-18, 3)); // height really only needs to be 1 pixel
            ComboBoxRenderer renderer= new ComboBoxRenderer();
    		setRenderer(renderer);
    	      
    		imageIcons = new Icon[1];
    		if (isHead)
                    imageIcons[0] = new GUI.ProxyEnabledIcon((Icon) VueResources.getIcon("leftarrow.raw"), this);
    		else
                    imageIcons[0] = new GUI.ProxyEnabledIcon((Icon) VueResources.getIcon("rightarrow.raw"), this);
            //imageIcons[2] = (ImageIcon) VueResources.getIcon("botharrow.raw");                                               		
    		this.setMaximumRowCount(10);
        }
                
        
        //class ComboBoxRenderer extends JLabel implements ListCellRenderer {
        class ComboBoxRenderer extends DefaultListCellRenderer {
        	
            public ComboBoxRenderer() {
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(CENTER);		
                setBorder(BorderFactory.createEmptyBorder(3,1, 3,1));
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
                
                int val = ((Integer)value).intValue();
                if (val ==0)
                    {
                	setText("none");
                	setIcon(null);
                    }
                else
                    {	val--;
                	setText("");
                	setIcon(imageIcons[val]);
                    }
                //System.out.println(this.getPreferredSize());

                return this;
            }
            
//             protected Icon makeIcon(LWComponent.StrokeStyle style) {
//                 LineIcon li = new LineIcon(24, 3);
//                 li.setStroke(style.makeStroke(1));
//                 return li;
//             }
            
        }        
	 
    }
    

    public boolean isPreferredType(Object o) {
        return o instanceof LWLink;
    }
    
    protected void buildBox()
    {
       // final AbstractButton mArrowStartButton = new VueButton.Toggle("link.button.arrow.start");
       // final AbstractButton mArrowEndButton = new VueButton.Toggle("link.button.arrow.end");
        
        //setting up tooltips for link specific buttons.
        //mArrowStartButton.setToolTipText(VueResources.getString("linkToolPanel.startArrow.toolTip"));
        //mArrowEndButton.setToolTipText(VueResources.getString("linkToolPanel.endArrow.toolTip"));
        
        final Action[] LinkTypeActions = new Action[] { 
            Actions.LinkMakeStraight,
            Actions.LinkMakeQuadCurved,
            Actions.LinkMakeCubicCurved
        };
        
        Integer[] i = new Integer[2];
		i[0]= new Integer(0);
		i[1]= new Integer(1);
		
		final ArrowStyleButton arrowHeadCombo = new ArrowStyleButton(i,true);
		final ArrowStyleButton arrowTailCombo = new ArrowStyleButton(i,false);
		
                //AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkCurves, LinkTypeActions);
                //        linkTypeMenu.setToolTipText("Link Style");
                //linkTypeMenu.addPropertyChangeListener(this);
        
        

        /*
        final LWPropertyHandler arrowHeadPropertyHandler =
            new LWPropertyHandler<Integer>(LWKey.LinkArrows, ArrowToolPanel.this) {
                public Integer produceValue() {
                    
                    if (arrowHeadCombo.getSelectedIndex() == 0)
                    	headArrowState =LWLink.ARROW_NONE;
                    else if (arrowHeadCombo.getSelectedIndex() ==1)
                        headArrowState = LWLink.ARROW_HEAD;
                 //  System.out.println(headArrowState + tailArrowState);
                    return headArrowState + tailArrowState;
                }
                public void displayValue(Integer i) {
                    //int arrowState = i;
                    
                }

            
            };

            arrowHeadCombo.addActionListener(arrowHeadPropertyHandler);
            final LWPropertyHandler arrowTailPropertyHandler =
                new LWPropertyHandler<Integer>(LWKey.LinkArrows, ArrowToolPanel.this) {
                    public Integer produceValue() {
                        
                        if (arrowTailCombo.getSelectedIndex() == 0)
                        	 tailArrowState =LWLink.ARROW_NONE;
                        else if (arrowTailCombo.getSelectedIndex() ==1)
                            tailArrowState = LWLink.ARROW_TAIL;
                        //System.out.println(headArrowState + tailArrowState);
                        return headArrowState +tailArrowState;
                    }
                    public void displayValue(Integer i) {
                      //  int arrowState = i;
                        
                    }

                
                };

                arrowTailCombo.addActionListener(arrowTailPropertyHandler);
        */

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
    	
        gbc.insets=new Insets(1,3,1,1);
		gbc.gridx = 0;
		gbc.gridy = 0;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
		gbc.anchor = GridBagConstraints.EAST;
		final JLabel headLabel = new JLabel("Start :");
                headLabel.setLabelFor(arrowHeadCombo);
		headLabel.setForeground(new Color(51,51,51));
		headLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(headLabel,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
		gbc.anchor = GridBagConstraints.EAST;
		final JLabel tailLabel = new JLabel("End :");
                tailLabel.setLabelFor(arrowTailCombo);
		tailLabel.setForeground(new Color(51,51,51));
		tailLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(tailLabel,gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;    		
		//gbc.insets = new Insets(1,1,1,1);
		gbc.gridwidth=1;
		gbc.gridheight=1;
		gbc.insets = new Insets(1,1,1,3);
		gbc.fill = GridBagConstraints.BOTH; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		mBox.add(arrowHeadCombo,gbc);
		
		       
       	gbc.gridx = 1;
   		gbc.gridy = 1;    		
   		gbc.gridwidth=1;
   		gbc.gridheight=1;
   		gbc.insets = new Insets(1,1,1,3);
   		gbc.fill = GridBagConstraints.BOTH; // the label never grows
   		gbc.anchor = GridBagConstraints.WEST;
   		arrowTailCombo.setSelectedIndex(1);
   		mBox.add(arrowTailCombo,gbc);




        final LWPropertyHandler arrowPropertyHandler =
            new LWPropertyHandler<Integer>(LWKey.LinkArrows, arrowHeadCombo, arrowTailCombo) {
                public Integer produceValue() {
                    int arrowState = 0;
                    if (arrowHeadCombo.getSelectedIndex() > 0)
                    	arrowState |= LWLink.ARROW_HEAD;
                    if (arrowTailCombo.getSelectedIndex() > 0)
                        arrowState |= LWLink.ARROW_TAIL;
                    return arrowState;
                }
                public void displayValue(Integer arrowState) {
                    if ((arrowState & LWLink.ARROW_HEAD) == 0)
                        arrowHeadCombo.setSelectedIndex(0);
                    else
                        arrowHeadCombo.setSelectedIndex(1);
                    if ((arrowState & LWLink.ARROW_TAIL) == 0)
                        arrowTailCombo.setSelectedIndex(0);
                    else
                        arrowTailCombo.setSelectedIndex(1);
                }

                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    headLabel.setEnabled(enabled);
                    tailLabel.setEnabled(enabled);
                }

            };

        arrowHeadCombo.addActionListener(arrowPropertyHandler);
        arrowTailCombo.addActionListener(arrowPropertyHandler);

                
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
