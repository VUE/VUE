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

/*******
 **  LinkInspectorPanel.java
 **
 **
 *********/

package tufts.vue;


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
 * LinkInspectorPanel
 *
 * The Link  Inspector Panel!
 *
 * \**/
public class LinkInspectorPanel  extends JPanel implements ObjectInspectorPanel.InspectorCard {
    
    
    /////////////
    // Fields
    //////////////
    
    /** The tabbed panel **/
    JTabbedPane mTabbedPane = null;
    
    /** The link we are inspecting **/
    LWLink mLink = null;
    
    /** info tab panel **/
    InfoPanel mInfoPanel = null;
    
    /** filter panel **/
    NotePanel mNotePanel = null;
    
    ///////////////////
    // Constructors
    ////////////////////
    
    public LinkInspectorPanel() {
        super();
        
        setMinimumSize( new Dimension( 150,200) );
        setLayout( new BorderLayout() );
        setBorder( new EmptyBorder( 5,5,5,5) );
        mTabbedPane = new JTabbedPane();
        VueResources.initComponent( mTabbedPane, "tabPane");
        
        mInfoPanel = new InfoPanel();
        mNotePanel = new NotePanel();
        
        mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
        mTabbedPane.addTab( mNotePanel.getName(), mNotePanel);
        
        add( BorderLayout.CENTER, mTabbedPane );
        validate();
        setVisible(true);
    }
    
    
    
    ////////////////////
    // Methods
    ///////////////////
    
    
    /**
     * setLink
     * Sets the LWMap component and updates teh display
     *
     * @param pLink - the LWLink to inspect
     **/
    public void setLink( LWLink pLink) {
        
        // if we have a change in maps...
        if( pLink != mLink) {
            mLink = pLink;
            updatePanels();
        }
    }
    
    
    /**
     * updatePanels
     * This method updates the panel's content pased on the selected
     * Map
     *
     **/
    public void updatePanels() {
        
        mInfoPanel.updatePanel( mLink);
        mNotePanel.updatePanel( mLink);
    }
    
    //////////////////////
    // OVerrides
    //////////////////////
    
    
    public Dimension getPreferredSize()  {
        Dimension size =  super.getPreferredSize();
        if( size.getWidth() < 200 ) {
            size.setSize( 200, size.getHeight() );
        }
        if( size.getHeight() < 250 ) {
            size.setSize( size.getWidth(), 250);
        }
        return size;
    }
    
    
    
    
    
    
    
    /////////////////
    // Inner Classes
    ////////////////////
    
    
    
    
    /**
     * InfoPanel
     * This is the tab panel for displaying Map Info
     *
     **/
    public class InfoPanel extends JPanel {
        
        JScrollPane mInfoScrollPane = null;
        
        Box mInfoBox = null;
        
        public InfoPanel() {
            
            setLayout( new BorderLayout() );
            setBorder( BorderFactory.createEmptyBorder(10,10,10,6));
            mInfoBox = Box.createVerticalBox();
            JLabel linkLabel = new JLabel("Link");
            linkLabel.setFont(VueConstants.FONT_MEDIUM_BOLD);
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.setBorder( BorderFactory.createEmptyBorder(0,0,5,0));
            labelPanel.add(linkLabel);
            // DEMO FIX:  adding hack for demo
            mInfoBox.add(labelPanel,BorderLayout.WEST );
            mInfoBox.add( new  LWCInfoPanel() );
            
            mInfoScrollPane = new JScrollPane();
            mInfoScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            mInfoScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mInfoScrollPane.setLocation(new Point(8, 9));
            mInfoScrollPane.setVisible(true);
            mInfoScrollPane.getViewport().add( mInfoBox);
            mInfoScrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            add( BorderLayout.NORTH, mInfoScrollPane );
        }
        
        
        public String getName() {
            return VueResources.getString("mapInfoTabName") ;
        }
        
        
        
        /**
         * updatePanel
         * Updates the Link info panel
         * @param LWMap the map
         **/
        public void updatePanel( LWLink pLink) {
            // fill in the data here
        }
    }
    
    
    /**
     * setTab
     * Sets the selected Tab for teh panel to the specifided ObjectInspector panel key
     **/
    public void setTab( int pTabKey) {
        if( pTabKey == ObjectInspectorPanel.NOTES_TAB ) {
            mTabbedPane.setSelectedComponent( mNotePanel);
        }
        else
            if( pTabKey == ObjectInspectorPanel.INFO_TAB ) {
                mTabbedPane.setSelectedComponent( mInfoPanel );
            }
    }
    
    
    
}





