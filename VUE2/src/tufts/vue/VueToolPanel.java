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

import tufts.vue.gui.GUI;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
 *
 * The VueToolPanel is the component that holds the main VUE toolbar
 * and the contextual properties tools.
 *
 * @version $Revision: 1.24 $ / $Date: 2006-01-20 20:35:48 $ / $Author: sfraize $ 
 *
 **/
public class VueToolPanel extends JPanel
{
    /** the panel where the main tools are placed **/
    private JPanel mMainToolPanel = null;
	
    /** the panel where contextual tools are placed **/
    private JPanel mContextualToolPanel = null;
	
    /** the button group used for tool selection **/
    private ButtonGroup mButtonGroup = null;
	
    /** the list of VueTools in the main tool panel **/
    private Vector mTools = new Vector();
	
    /** the current tool selection (TO DO:  remove this)  **/
    private VueTool mCurrentTool = null;
	
    private Box mMainBox = null;
	
    /** a map of PaletteButtons keyed off of the tool ID **/
    private Map mToolButtons = new HashMap();
	
	
    /***
     * VueToolPanel()
     * The constructor that builds an initial VUE ToolPanel
     **/
    private static final boolean debug = false;
    public VueToolPanel() {
        super();
        mButtonGroup = new ButtonGroup();
        if (debug)
            setBackground(Color.blue);
        else
            GUI.applyToolbarColor(this);
		
        setLayout( new BorderLayout() );
        if (GUI.isMacBrushedMetal())
            setBorder(new EmptyBorder(0,3,2,10));//tlbr
        else
            setBorder(new EmptyBorder(1,3,2,10));//tlbr
		
        mMainToolPanel = new JPanel();
        mMainBox = Box.createHorizontalBox();
		
        BoxLayout boxLayout = new BoxLayout( mMainToolPanel, BoxLayout.X_AXIS);
        mMainToolPanel.setLayout(boxLayout);
		
        if (debug)
            mMainToolPanel.setBackground(Color.green);

        mContextualToolPanel = new JPanel();
        mContextualToolPanel.setAlignmentX(LEFT_ALIGNMENT);
        mContextualToolPanel.setLayout(new BoxLayout(mContextualToolPanel, BoxLayout.X_AXIS));

        if (debug)
            mContextualToolPanel.setBackground(Color.orange);
		
        setAlignmentX( LEFT_ALIGNMENT);
        add(BorderLayout.WEST, mMainToolPanel);
        add(BorderLayout.EAST, mContextualToolPanel);
        //add( BorderLayout.CENTER, mContextualToolPanel);
        //add( BorderLayout.EAST, Box.createHorizontalGlue());
    }
	
	
    /**
     * addToolButton
     * This method adds a PaletteButton to the main tool panel as
     * a tool to be used in the set of main tools
     *
     * @param PaletteButton - the button to add
     **/
    public void addToolButton( PaletteButton pButton) {
         
        if (debug)
            pButton.setBackground(Color.magenta);
        else
            GUI.applyToolbarColor(pButton);
        mMainToolPanel.add( pButton);
        mButtonGroup.add( pButton);
        if( mButtonGroup.getButtonCount() == 1) {
            pButton.setSelected( true);
        }
    }
	
	
    /**
     * addTools()
     * This method adds an array of VueTool items and creates
     * main toolbar buttons based on the VueTool.
     *
     * @param VueTool [] - the list of tools
     **/
    public void addTools( VueTool [] pTools) {
        for( int i=0; i<pTools.length; i++) {
            addTool( pTools[i] );
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
            PaletteButton button = createPaletteButton(pTool);
            // save teh component in the button map
            mToolButtons.put( pTool.getID(), button);
                
            // todo: setting this mnemonic doesn't appear to work
            //if (pTool.getShortcutKey() != 0)
            //button.setMnemonic(pTool.getShortcutKey());
            addToolButton( button);
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
	
	
    public void addContextual( Component pObj) {
        if( pObj != null) {
            mContextualToolPanel.add( pObj);
        }
    }
	
    /**
     * setContextualToolPanel
     * This method sets the contextual tool panel and removes
     * any components already displayed.
     **/
    private JPanel mPanelContent;
    public void setContextualToolPanel(JPanel pPanel) {
        if (mPanelContent == pPanel)
            return;
        if (DEBUG.TOOL) System.out.println(this + " LOADING " + pPanel);
        mContextualToolPanel.removeAll();        
        if (pPanel != null) {
            if (debug)
                pPanel.setBackground(Color.cyan);
            else
                GUI.applyToolbarColor(pPanel);
            mContextualToolPanel.add(pPanel);
            mPanelContent = pPanel;
        }
        validate();
        repaint();
    }
     
	
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
                    item.setIcon( subTool.getIcon() );
                    item.setSelectedIcon( subTool.getSelectedIcon() );
                    item.setDisabledIcon( subTool.getDisabledIcon() );
                    item.setRolloverIcon( subTool.getRolloverIcon() );
                    item.setPressedIcon( subTool.getDownIcon() );
                    item.setMenuItemIcon( subTool.getMenuItemIcon() );
                    item.setMenuItemSelectedIcon( subTool.getMenuItemSelectedIcon() );
                    item.setToolTipText( subTool.getToolTipText() );
                    item.setToolTipText( pTool.getToolTipText() );
                    item.addActionListener( subTool);
					
                    items[i] = item;

                    subTool.setLinkedButton(item);
                }
            }
            button = new PaletteButton( items );
            button.setPropertiesFromItem( items[0]);
            button.setOverlayIcons (pTool.getOverlayUpIcon(), pTool.getOverlayDownIcon() );
            button.setToolTipText( pTool.getToolTipText() );
        } else  {  // just a radio-like button, no popup items 
            button = new PaletteButton();
            button.setIcons( pTool.getIcon(), pTool.getDownIcon(), pTool.getSelectedIcon() ,
                             pTool.getDisabledIcon(), pTool.getRolloverIcon() );
            button.setToolTipText( pTool.getToolTipText() );
        }
        // set the user context to the VueTOol
        button.setContext( pTool);
        button.addActionListener( pTool);
        button.setName(pTool.getID());

        // store the button in the tool
        pTool.setLinkedButton(button);
        
        return button;
    }
	

    //  OLD CODE	
    //	public void setEnabled( boolean pState) {
    //	  // Manual override
    //	  super.setEnabled( true);
    //	}
	
	
    // DEBUG :
	
	
    protected void X_processMouseMontionEvent( MouseEvent pEvent) {
        debug("  VueToolPanel: processMouseMotionEvent "+pEvent.getID() );
        super.processMouseEvent( pEvent);
    }


    protected void X_processMouseEvent( MouseEvent pEvent) {
        debug("  processMouseEvent() "+ pEvent.getID()  );
        super.processMouseEvent( pEvent);
    }


    static private boolean sDebug = true;
    private void debug( String pStr) {
        if( sDebug)
            System.out.println( "VueToolPanel - "+pStr);
    }
}
