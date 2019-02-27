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

package tufts.vue;

import tufts.vue.gui.GUI;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


public class FloatingZoomPanel extends JPanel
{
	//private final VueTool ArrowTool = VueToolbarController.getController().getTool("selectionTool");
	//private final VueTool HandTool = VueToolbarController.getController().getTool("handTool");
    //private final VueTool ZoomTool = VueToolbarController.getController().getTool("zoomTool");
    
	private String fullScreenTools = "fullScreenToolbarToolNames";
    /** the panel where the main tools are placed **/
    private JComponent mMainToolBar = null;	
	
    /** the button group used for tool selection **/
    private ButtonGroup mButtonGroup = null;
	
    /** the list of VueTools in the main tool panel **/
    private Vector mTools = new Vector();
	
    /** the current tool selection (TO DO:  remove this)  **/
    private VueTool mCurrentTool = null;
	
    /** a list of available tools **/
    private VueTool[] mVueTools = null;
    
    /** a map of PaletteButtons keyed off of the tool ID **/
    private Map mToolButtons = new HashMap();
	
	
    public FloatingZoomPanel() {
        mButtonGroup = new ButtonGroup();          
        
        GUI.applyToolbarColor(this);
		
        setLayout( new BorderLayout() );
        setOpaque(false);
        
        if (GUI.isMacBrushedMetal())
            setBorder(new EmptyBorder(0,3,2,10));//tlbr
        else
            setBorder(new EmptyBorder(0,3,0,10));//tlbr
        
        mMainToolBar = new Box(BoxLayout.X_AXIS);
        
        //addTool();
            
       // addSubZoomTool(ZoomTool);
       //ZoomTool zt = new ZoomTool();
       //zt.setID("zoomTool.zoomIn");
       //zt.initFromResources();
        
        //The floating toolbar is a subset of the toolbar so load tools from the instance map..
        mVueTools = VueToolUtils.loadToolsFromMap(fullScreenTools);
       
        for (int i = 0; i < mVueTools.length;i++)
        	addTool(mVueTools[i]);
       
        setAlignmentX( LEFT_ALIGNMENT);
        add(BorderLayout.WEST, mMainToolBar);

        VUE.addActiveListener(VueTool.class, this);
    }

    public JComponent getMainToolbar() {
        return mMainToolBar;
    }
	
	
    /**
     * addToolButton
     * This method adds a PaletteButton to the main tool panel as
     * a tool to be used in the set of main tools
     *
     * @param PaletteButton - the button to add
     **/
    public void addToolButton( PaletteButton pButton) {
         
        GUI.applyToolbarColor(pButton);
        mMainToolBar.add( pButton);
        mButtonGroup.add( pButton);
        if( mButtonGroup.getButtonCount() == 1) {
            pButton.setSelected( true);
        }
    }
		
	
    /**
     * addTool
     * This method adds a single VueTool to the main toolbar.
     * It creates a PaleteButton for the tool and adds it to the toolbar panel.
     *
     * #param VueTool - the tool to add.
     **/
    public void addTool( VueTool pTool) {
	
        if( mTools == null) {
            mTools = new Vector();
        }
        mTools.add(pTool);
        if (pTool.hasToolbarButton()) {
        	 if (pTool.hasSubTools()) {
        		 //create a button for each subtool
        		 Vector names = pTool.getSubToolIDs();
                 int numSubTools = names.size();

                 for(int i=0; i<numSubTools; i++) {
                     String name = (String) names.get(i);
                     VueTool subTool = pTool.getSubTool( name);
                     if( subTool != null) 
                     {
                    	 PaletteButton button = createPaletteButton(subTool);
                		 // save teh component in the button map
                		 mToolButtons.put( subTool.getID(), button);
                        
                		 // 	todo: setting this mnemonic doesn't appear to work
                		 //if (pTool.getShortcutKey() != 0)
                		 //button.setMnemonic(pTool.getShortcutKey());
                		 addToolButton( button);
                     }
                 }
                 
        	 }
        	 else
        	 {
        		 PaletteButton button = createPaletteButton(pTool);
        		 // save teh component in the button map
        		 mToolButtons.put( pTool.getID(), button);
                
        		 // 	todo: setting this mnemonic doesn't appear to work
        		 //if (pTool.getShortcutKey() != 0)
        		 //button.setMnemonic(pTool.getShortcutKey());
        		 addToolButton( button);
        	 }
        }
    }
	
    /**
     * getSelectedTool
     * This method returns the selected tool based on the radio group
     **/
    public VueTool getSelectedTool() {
	 	
        Enumeration e = mButtonGroup.getElements();
        PaletteButton cur;
        while( e.hasMoreElements() ) {
            cur = (PaletteButton) e.nextElement();
            if( cur.isSelected() ) {
                return  ((VueTool) cur.getContext()) ;
            }
        }
        return null;	 	
    }
	
	
    /**
     * setSelectedTool
     *This method attempts to set the currenlty selected tool
     * in the main tool bar by looking for the TVueTool's PaletteButton
     * that's in the radio group.  If found, it selectes the button
     * in the radio group and causes an repaint.  
     *
     * @param VueTool - the new tool to select
     **/
    public void setSelectedTool( VueTool pTool) {
        if( pTool != null) {
            PaletteButton button = (PaletteButton) mToolButtons.get( pTool.getID() );
            if( button != null) {
                mButtonGroup.setSelected( button.getModel(), true);
            }
        }
    }


    public void activeChanged(ActiveEvent e, VueTool tool) {
        setSelectedTool(tool);
    }

    
	
    /**
     * setContextualToolPanel
     * This method sets the contextual tool panel and removes
     * any components already displayed.
     **/
    private JPanel mPanelContent;
     
	
    /**
     * removeTool()
     * This method removes a tool from the VueToolPanel
     * @param VueTool the tool to remove
     **/
    public void removeTool( VueTool pTool) {
		
        PaletteButton button = (PaletteButton) mToolButtons.get( pTool.getID() );
        mToolButtons.remove( pTool.getID() );
		
        mTools.remove( pTool);
        // FFIX:  tbd we might not need to ever remove, only disable.
        // removeToolButton( pTool.getName() );
    }
	
	
	
	
    /**
     * createPaletteButton
     * This method creates a GUI PaleteeButton control from the
     * a VueTool.
     * 
     * @param pTool -= the tool to map to aPaletteButton
     * @return PaletteButton - a PaletteButton with properties based on the VueTool
     **/
    protected PaletteButton createPaletteButton(VueTool pTool)
    {
        PaletteButton button = null;
        /*
        if (pTool.hasSubTools()) {
            // create button items
            Vector names = pTool.getSubToolIDs();
            int numSubTools = names.size();
            PaletteButtonItem items [] = new PaletteButtonItem[numSubTools];
            for(int i=0; i<numSubTools; i++) {
                String name = (String) names.get(i);
                VueTool subTool = pTool.getSubTool( name);
                if( subTool != null) {
                    PaletteButtonItem item = new PaletteButtonItem();
                    item.setName(name);
                    item.setIcon( subTool.getIcon() );
                    item.setSelectedIcon( subTool.getSelectedIcon() );
                    item.setDisabledIcon( subTool.getDisabledIcon() );
                    item.setRolloverIcon( subTool.getRolloverIcon() );
                    item.setPressedIcon( subTool.getDownIcon() );
                    item.setMenuItemIcon( subTool.getMenuItemIcon() );
                    item.setMenuItemSelectedIcon( subTool.getMenuItemSelectedIcon() );
                    //item.setToolTipText( subTool.getToolTipText() );
                    //item.setToolTipText( pTool.getToolTipText() );
                    item.addActionListener( subTool);
					
                    items[i] = item;

                    subTool.setLinkedButton(item);
                }
            }
            
            button = new PaletteButton( items );
            button.setPropertiesFromItem( items[0]);
            button.setOverlayIcons (pTool.getOverlayUpIcon(), pTool.getOverlayDownIcon() );
        } else  {  // just a radio-like button, no popup items
        	*/
            button = new PaletteButton();
            button.setIcons( pTool.getIcon(), pTool.getDownIcon(), pTool.getSelectedIcon() ,
                             pTool.getDisabledIcon(), pTool.getRolloverIcon() );
        //}
        
        button.setToolTipText( pTool.getToolTipText() );
        // set the user context to the VueTOol
        button.setContext( pTool);
        button.addActionListener( pTool);
        button.setName(pTool.getID());

        // store the button in the tool
        pTool.setLinkedButton(button);
        
        return button;
    }
}
