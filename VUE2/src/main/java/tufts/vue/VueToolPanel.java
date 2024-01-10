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


/**
 *
 * The VueToolPanel is the component that holds the main VUE toolbar
 * and the contextual properties tools.
 *
 * @version $Revision: 1.40 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $ 
 *
 **/
public class VueToolPanel extends JPanel
{
    // when we can set this to false, we can get rid of this class.
    public static final boolean IS_CONTEXTUAL_TOOLBAR_ENABLED = false;
    
    /** the panel where the main tools are placed **/
  //  private JComponent mMainToolBar = null;	
	
    /** the button group used for tool selection **/
    private ButtonGroup mButtonGroup = null;
	
    /** the list of VueTools in the main tool panel **/
    private Vector mTools = new Vector();
	
    /** the current tool selection (TO DO:  remove this)  **/
    private VueTool mCurrentTool = null;
	
    //private Box mMainBox = null;
	
    /** a map of PaletteButtons keyed off of the tool ID **/
    private Map mToolButtons = new HashMap();
	
	
    /***
     * VueToolPanel()
     * The constructor that builds an initial VUE ToolPanel
     **/
    private static final boolean debug = false;
    public VueToolPanel() {
        mButtonGroup = new ButtonGroup();
        if (debug)
            setBackground(Color.blue);
        else
            GUI.applyToolbarColor(this);
		
        setLayout( new BoxLayout(this,BoxLayout.X_AXIS) );
        setOpaque(false);
        if (GUI.isMacBrushedMetal())
            setBorder(new EmptyBorder(0,3,2,10));//tlbr
        else
            setBorder(new EmptyBorder(0,3,0,10));//tlbr
        //setBorder(new EmptyBorder(1,3,2,10));//tlbr
		
    //    mMainToolBar = new Box(BoxLayout.X_AXIS);
        //mMainBox = Box.createHorizontalBox();
		
   //     if (debug)
    //        mMainToolBar.setBackground(Color.green);

       // setAlignmentX( LEFT_ALIGNMENT);
    //    add(mMainToolBar);
        //add( BorderLayout.CENTER, mContextualToolPanel);
        //add( BorderLayout.EAST, Box.createHorizontalGlue());
    }

    public JComponent getMainToolbar() {
        return this;
    }
	
    class JLineSeparator extends JSeparator
    {
         private Dimension dim;

         public JLineSeparator(int orient, int w, int h)
         {
             super(orient);
             this.setForeground(VueResources.getColor("dividerBarColor"));
             dim = new Dimension(w, h);
         }

         public Dimension getPreferredSize()
         {
             return dim;
         }

         public Dimension getMaximumSize()
         {
             return getPreferredSize();
         }

    } 
    public void addSeparator()
    {
    	JLineSeparator jsp = new JLineSeparator(SwingConstants.VERTICAL,3,18);
    	jsp.setBorder(BorderFactory.createEmptyBorder());
    	add(jsp);
    }
	
    /**
     * createToolButton
     * This method creates a PaletteButton and stores it in the button group
     * however it may or may not get added to the main toolbar.
     *
     * @param PaletteButton - the button to add
     * @param addToMainToolbar - whether or not to put it on the main toolbar.
     **/
	private static final JPanel p= new JPanel();

    public void createToolButton( PaletteButton pButton, boolean addToMainToolbar) {
         
        if (debug)
            pButton.setBackground(Color.magenta);
        else if (!VUE.isApplet())
            GUI.applyToolbarColor(pButton);
        else
        {
        	//For the life of me I can't figure out what's wrong with L&F on applets but 
        	//buttons are not getting the same background properties as panels, weird. Hack
        	//for now.
        	pButton.setBackground(p.getBackground());
        }
        if (addToMainToolbar)
        	add( pButton);
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
    public void addTools( VueTool [] pTools, int[] separators) {
    	int separatorIndex =0;
        for( int i=0; i<pTools.length + separators.length; i++) {
        	if ((separatorIndex < separators.length) && i==separators[separatorIndex])
        	{
        		addSeparator();
        		separatorIndex++;
        	}
        	else
        		addTool( pTools[i-separatorIndex],true );
        }
    }
	
    public void addTool( VueTool pTool) {
    	addTool(pTool,false);
    }
    /**
     * addTool
     * This method adds a single VueTool to the main toolbar.
     * It creates a PaleteButton for the tool and adds it to the toolbar panel.
     *
     * #param VueTool - the tool to add.
     **/
    public void addTool( VueTool pTool,boolean addToMainToolbar) {
	
        if( mTools == null) {
            mTools = new Vector();
        }
        mTools.add(pTool);
        if (pTool.hasToolbarButton()) {
            PaletteButton button = createPaletteButton(pTool);
            // save teh component in the button map
            if (pTool.hasSubTools())
            {
            	Vector v = pTool.getSubToolIDs();
            	for (int i = 0; i < v.size(); i++)
            	{
            		VueTool tool = pTool.getSubTool((String)v.get(i));
            		addTool(tool,false);
            	}
            }
            mToolButtons.put( pTool.getID(), button);
                
            // todo: setting this mnemonic doesn't appear to work
            //if (pTool.getShortcutKey() != 0)
            //button.setMnemonic(pTool.getShortcutKey());
            createToolButton( button,addToMainToolbar);
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
        	if (pTool.getParentTool() != null)
        	{        		
        		//you've selected a subtool...do somethign else.
        		//select the parent.
        		VueTool parentTool = pTool.getParentTool();
        		PaletteButton button = (PaletteButton) mToolButtons.get( parentTool.getID() );
        		
        		
        		//PaletteButtonItem[] items = button.getPaletteButtonItems();
        		button.setPropertiesFromItem((PaletteButton) mToolButtons.get(pTool.getID()));
        		
        		parentTool.setSelectedSubTool(pTool);
        		//button.setContext( pTool);
        		if( button != null) 
        			mButtonGroup.setSelected( button.getModel(), true);
        		
        		//button.addActionListener( pTool);
        		//button.setSelectedIcon(pTool.getSelectedIcon());        		
        		//button.setIcon(pTool.getIcon());        		
        	}
        	else
        	{        		
        		PaletteButton button = (PaletteButton) mToolButtons.get( pTool.getID() );
        		if( button != null) 
        			mButtonGroup.setSelected( button.getModel(), true);
            }
        }
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
	
	public Map getToolButtons()
	{
		return  mToolButtons;
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
                    item.setName(name);
                    item.setIcon( subTool.getIcon() );
                    item.setSelectedIcon( subTool.getSelectedIcon() );
                    item.setDisabledIcon( subTool.getDisabledIcon() );
                    item.setRolloverIcon( subTool.getRolloverIcon() );
                    item.setPressedIcon( subTool.getDownIcon() );
                    item.setMenuItemIcon( subTool.getMenuItemIcon() );
                    item.setMenuItemSelectedIcon( subTool.getMenuItemSelectedIcon() );
                    item.setToolTipText( subTool.getToolTipText() );
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
            button = new PaletteButton();
            button.setIcons( pTool.getIcon(), pTool.getDownIcon(), pTool.getSelectedIcon() ,
                             pTool.getDisabledIcon(), pTool.getRolloverIcon() );
        }
        
        button.setToolTipText( pTool.getToolTipText() );
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
