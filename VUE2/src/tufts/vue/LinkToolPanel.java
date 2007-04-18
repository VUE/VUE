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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;


/**
 * A property editor panel for LWLink's.
 *
 * @version $Revision: 1.40 $ / $Date: 2007-04-18 21:06:23 $ / $Author: mike $
 * 
 */

public class LinkToolPanel extends LWCToolPanel
{
	/**
	 * This isn't here for good but until we do something else, this is here so we
	 * don't loose the functionality with the loss of the contextual menu.
	 * @author mkorcy01
	 *
	 */
	private static class ArrowStyleButton extends JComboBox
    {
        ImageIcon[] imageIcons;
        
        public ArrowStyleButton(Object[] a) {
        	super(a);
            setFont(tufts.vue.VueConstants.FONT_SMALL); // does this have any effect?  maybe on Windows?
            // set the size of the icon that displays the current value:
            //setButtonIcon(new LineIcon(ButtonWidth-18, 3)); // height really only needs to be 1 pixel
            ComboBoxRenderer renderer= new ComboBoxRenderer();
    		setRenderer(renderer);
    	      
    		imageIcons = new ImageIcon[3];    
            imageIcons[0] = (ImageIcon) VueResources.getIcon("leftarrow.raw");
            imageIcons[1] = (ImageIcon) VueResources.getIcon("rightarrow.raw");
            imageIcons[2] = (ImageIcon) VueResources.getIcon("botharrow.raw");                                               		
    		this.setMaximumRowCount(10);
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
        		
                //this.setBorder(BorderFactory.createEmptyBorder(1,4, 1, 4));

        		return this;
        	}
        	  protected Icon makeIcon(LWComponent.StrokeStyle style) {
                  LineIcon li = new LineIcon(24, 3);
                  li.setStroke(style.makeStroke(1));
                  return li;
              }
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
        
        Integer[] i = new Integer[4];
		i[0]= new Integer(0);
		i[1]= new Integer(1);
		i[2]= new Integer(2);
		i[3]= new Integer(3);
		final ArrowStyleButton arrowCombo = new ArrowStyleButton(i);
		
        AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkCurves, LinkTypeActions);
        linkTypeMenu.setToolTipText("Link Style");
        linkTypeMenu.addPropertyChangeListener(this);
        
        final LWPropertyHandler arrowPropertyHandler =
            new LWPropertyHandler<Integer>(LWKey.LinkArrows, LinkToolPanel.this) {
                public Integer produceValue() {
                    int arrowState = LWLink.ARROW_NONE;
                    if (arrowCombo.getSelectedIndex() == 0)
                    	return arrowState;
                    else if (arrowCombo.getSelectedIndex() ==1)
                        arrowState |= LWLink.ARROW_HEAD;
                    else if (arrowCombo.getSelectedIndex() ==2)
                        arrowState |= LWLink.ARROW_TAIL;
                    else if (arrowCombo.getSelectedIndex() ==3)
                    {
                    	arrowState |= LWLink.ARROW_HEAD;
                    	arrowState |= LWLink.ARROW_TAIL;
                    }
                    return arrowState;
                }
                public void displayValue(Integer i) {
                    int arrowState = i;
                    
                }

            /*    public void setEnabled(boolean enabled) {
                    mArrowStartButton.setEnabled(enabled);
                    mArrowEndButton.setEnabled(enabled);
                }*/
            };

            arrowCombo.addActionListener(arrowPropertyHandler);
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
        
        addComponent(linkTypeMenu);
        	
        mBox.setLayout(new GridBagLayout());
    	gbc.gridx = 0;
		gbc.gridy = 0;    		
		gbc.insets = new Insets(1,5,1,0);
		gbc.gridwidth=3;
		gbc.fill = GridBagConstraints.BOTH; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		JLabel linkLabel = new JLabel("Link");
		linkLabel.setForeground(Color.black);
		linkLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(linkLabel,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		JLabel strokeLabel = new JLabel("Stroke :");
		strokeLabel.setForeground(new Color(51,51,51));
		strokeLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(strokeLabel,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;    				
		gbc.gridwidth=1;
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		JLabel arrowLabel = new JLabel("Arrows :");
		arrowLabel.setForeground(new Color(51,51,51));
		arrowLabel.setFont(tufts.vue.VueConstants.SmallFont);
		mBox.add(arrowLabel,gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;    		
		gbc.insets = new Insets(1,1,1,1);
		gbc.gridwidth=1;
		gbc.gridheight=1;
		
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
//		gbc.ipadx=10;
//		gbc.ipady=0;
		
		mBox.add(mStrokeStyleButton,gbc);
		
//		gbc.ipadx=0;
//		gbc.ipady=4;
		gbc.gridx = 1;
		gbc.gridy = 2;    		
		gbc.insets = new Insets(1,1,1,1);
		gbc.gridwidth=1;
		gbc.gridheight=1;
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;
		JComboBox undefined = new JComboBox();
		undefined.setFont(tufts.vue.VueConstants.FONT_SMALL); 
		undefined.addItem("none");
		undefined.setEnabled(false);
		mBox.add(undefined,gbc);
		
		gbc.ipadx=0;
		gbc.ipady=0;
		gbc.gridx = 2;
		gbc.gridy = 2;    		
		gbc.insets = new Insets(1,1,1,1);
		gbc.gridwidth=1;
		gbc.gridheight=1;
		//gbc.ipadx=12;
		gbc.fill = GridBagConstraints.NONE; // the label never grows
		gbc.anchor = GridBagConstraints.WEST;		
		mBox.add(arrowCombo,gbc);
		
		gbc.ipadx=0;
        if (addComponent(mStrokeColorButton))
        {
        	gbc.gridx = 3;
    		gbc.gridy = 1;    		
    		gbc.insets = new Insets(1,1,1,1);
    		gbc.gridwidth=1;
    		gbc.gridheight=1;
    		gbc.fill = GridBagConstraints.NONE; // the label never grows
    		gbc.anchor = GridBagConstraints.WEST;
    		mBox.add(mStrokeColorButton,gbc);
        }
        
        if (addComponent(mStrokeButton))
        {
        	gbc.gridx = 2;
    		gbc.gridy = 1;    		
    	//	gbc.insets = new Insets(-6,5,-6,1);
    		gbc.gridwidth=1;
    	//	gbc.ipadx=10;
    		gbc.gridheight=1;
    		gbc.fill = GridBagConstraints.NONE; // the label never grows
    		gbc.anchor = GridBagConstraints.WEST;
    		mStrokeButton.setSelectedIndex(1);
    		mBox.add(mStrokeButton,gbc);
        }                
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
