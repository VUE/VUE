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
 **  ObjectInspectorPanel
 **
 **
 *********/


package tufts.vue;


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import tufts.vue.filter.*;


/**
 * ObjectInspectorPanel
 *
 * The Object Inspector Panel!
 *
 * \**/
public class ObjectInspectorPanel  extends JPanel
implements LWSelection.Listener {
    
    
    /////////////
    // Statics
    //////////////
    
    // these 3 constants want to be elsewhere
    public static final int INFO_TAB = 0;
    public static final int NOTES_TAB = 1;
    public static final int TREE_TAB = 2;
    public static final int FILTER_TAB = 3;
    
    
    /////////////
    // Fields
    //////////////
    
    /** The tabbed panel **/
    JTabbedPane mTabbedPane = null;
    
    /** The node we are inspecting **/
    LWComponent mComponent = null;
    
    /** info tab panel **/
    InfoPanel mInfoPanel = null;
    
    /** pathways panel **/
    TreePanel mTreePanel = null;
    
    /** notes panel **/
    NotePanel mNotePanel = null;
    
    /** filter panel **/
    
    NodeFilterPanel mNodeFilterPanel = null;
    ///////////////////
    // Constructors
    ////////////////////
    
    public ObjectInspectorPanel() {
        super();
        
        setMinimumSize( new Dimension( 200,200) );
        setLayout( new BorderLayout() );
        setBorder( new EmptyBorder( 5,5,5,5) );
        mTabbedPane = new JTabbedPane();
        VueResources.initComponent( mTabbedPane, "tabPane");
        
        mInfoPanel = new InfoPanel();
        mTreePanel = new TreePanel();
        mNotePanel = new NotePanel();
        mNodeFilterPanel = new NodeFilterPanel();
        
        mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
        mTabbedPane.addTab( mTreePanel.getName(),  mTreePanel);
        mTabbedPane.addTab( mNotePanel.getName(), mNotePanel);
        mTabbedPane.addTab(mNodeFilterPanel.getName(),mNodeFilterPanel);
        add( BorderLayout.CENTER, mTabbedPane );
        if (DEBUG.INIT) System.out.println("Created " + this);
    }
    
    
    
    ////////////////////
    // Methods
    ///////////////////
    
    
    /**
     * setComponent
     * Sets the LWMap component and updates the display
     *
     * @param pComponet - the LWMap to inspect
     **/
    public void setLWComponent(LWComponent pComponent) {
        
        // if we have a change in maps...
        if( pComponent != mComponent) {
            mComponent = pComponent;
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
        mInfoPanel.updatePanel( mComponent);
        mTreePanel.updatePanel( mComponent);
        mNotePanel.updatePanel( mComponent);
        mNodeFilterPanel.updatePanel(mComponent);
    }
    
    //////////////////////
    // OVerrides
    //////////////////////
    
    
    public Dimension getPreferredSize()  {
        Dimension size =  super.getPreferredSize();
        if( size.getWidth() < 300 ) {
            size.setSize( 300, size.getHeight() );
        }
        if( size.getHeight() < 250 ) {
            size.setSize( size.getWidth(), 250);
        }
        return size;
    }
    
    
    public void selectionChanged( LWSelection pSelection) {
        LWComponent lwc = null;
        if( pSelection.size() == 1 )  {
            // debug( "Object Inspector single selection");
            lwc = pSelection.first();
            setLWComponent(lwc);
        }
        else if (pSelection.size() == 0) {
            mTreePanel.updatePanel();
        }else {
            
            // debug("ObjectInspector item selection size is: "+ pSelection.size() );
        }
        // setLWComponent(lwc);
        
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
        JPanel labelPanel;
        JLabel nodeLabel = null;
        JComponent iconPanel = new JPanel();
        LWComponent sComponent;
        public InfoPanel() {
            setLayout( new BorderLayout() );
            setBorder( BorderFactory.createEmptyBorder(10,10,10,6));
            nodeLabel = new JLabel("Node");
            nodeLabel.setFont(VueConstants.FONT_MEDIUM_BOLD);
            nodeLabel.setHorizontalTextPosition(JLabel.LEFT);
            nodeLabel.setVerticalAlignment(JLabel.TOP);
            labelPanel = new JPanel(new BorderLayout());
            labelPanel.setBorder( BorderFactory.createEmptyBorder(0,0,5,0));
            labelPanel.add(nodeLabel,BorderLayout.WEST);
            labelPanel.add(iconPanel,BorderLayout.EAST);
            setBorder( BorderFactory.createEmptyBorder(10,10,0,6));
            add(labelPanel,BorderLayout.NORTH);
            add(new LWCInfoPanel(),BorderLayout.CENTER);
        }
        
        
        public String getName() {
            String name = VueResources.getString("mapInfoTabName") ;
            
            if( name == null) {
                name = "Info";
            }
            return name;
        }
        
        
        
        /**
         * updatePanel
         * Updates the Map info panel
         * @param LWMap the map
         **/
        public void updatePanel( LWComponent pComponent) {
            sComponent = pComponent;
            if(pComponent instanceof LWLink)
                nodeLabel.setText("Link");
            else
                nodeLabel.setText("Node");
            Thread t = new Thread() {
                public void run() {
                    labelPanel.remove(iconPanel);
                    if(sComponent.getResource()!= null && sComponent.getResource().getPreview() != null) {
                        iconPanel = sComponent.getResource().getPreview();
                        
                    } else {
                        iconPanel = new JPanel();
                    }
                    iconPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    //((Graphics2D)iconPanel.getGraphics()).scale(0.2,0.2);
                    iconPanel.setPreferredSize(new Dimension(75,75));
                    labelPanel.add(iconPanel,BorderLayout.EAST);
                    
                    labelPanel.validate();
                    // update the display
                }
            };
            t.start();
        }
    }
    
    
    /**
     * This is the Pathway Panel for the Map Inspector
     *
     **/
    public class TreePanel extends JPanel {
        
        /** the path scroll pane **/
        JScrollPane mTreeScrollPane = null;
        
        /** the tree to represented in the scroll pane **/
        OutlineViewTree tree = null;
        
        /** the header of the panel**/
        //JLabel panelLabel = null;
        
        /**
         * TreePanel
         * Constructs a pathway panel
         **/
        public TreePanel() {
            
            //fix the layout?
            setLayout( new BorderLayout() );
            setBorder( new EmptyBorder(4,4,4,4) );
            
            
            tree = new OutlineViewTree();
            tree.setBorder(new EmptyBorder(4,4,4,4));
            
            //panelLabel = new JLabel();
            
            mTreeScrollPane = new JScrollPane(tree);
            mTreeScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            mTreeScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mTreeScrollPane.setLocation(new Point(8, 9));
            mTreeScrollPane.setVisible(true);
            
            //add(panelLabel, BorderLayout.NORTH);
            add(mTreeScrollPane, BorderLayout.CENTER);
        }
        
        
        public String getName() {
            String name = VueResources.getString("nodeTreeTabName") ;
            if( name == null) {
                name = "Tree";
            }
            return name;
        }
        
        
        /**
         * updatePanel
         * This updates the Panel display based on a new LWMap
         *
         **/
        public void updatePanel( LWComponent pComponent) {
            // update display based on the LWNode
            
            //if the tree is not intiliazed, hidden, or doesn't contain the given node,
            //then it switches the model of the tree using the given node
            
            if (!tree.isInitialized() || !this.isVisible() || !tree.contains(pComponent)) {
                //panelLabel.setText("Node: " + pNode.getLabel());
                if(pComponent instanceof LWContainer)
                    tree.switchContainer((LWContainer)pComponent);
                
                else if (pComponent instanceof LWLink)
                    tree.switchContainer(null);
            }
            
            //if the node is in the model and the panel is visible and intialized,
            //then it sets the selected path to the one which ends with the given node
            //else
            //tree.setSelectionPath(pComponent);
        }
        
        public void updatePanel() {
            tree.switchContainer(null);
        }
    }
    
    public class NodeFilterPanel extends JPanel implements ActionListener{
        NodeFilterEditor nodeFilterEditor = null;
        
        public NodeFilterPanel() {
            
            setLayout(new BorderLayout());
            setBorder( BorderFactory.createEmptyBorder(10,10,10,6));
            
            // todo in VUE to create map before adding panels or have a model that
            // has selection loaded when map is added.
            // nodeFilterEditor = new NodeFilterEditor(mNode.getNodeFilter(),true);
            // add(nodeFilterEditor);
        }
        
        
        public void actionPerformed(ActionEvent e) {
        }
        public String getName() {
            return "Custom Metadata"; // this should come from VueResources
        }
        public void updatePanel( LWComponent pComponent) {
            // update the display
            if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + pComponent);
            if(nodeFilterEditor!= null) {
                nodeFilterEditor.setNodeFilter(pComponent.getNodeFilter());
            }else {
                nodeFilterEditor = new NodeFilterEditor(pComponent.getNodeFilter(),true);
                add(nodeFilterEditor,BorderLayout.CENTER);
            }
            validate();
        }
    }
    
    
    /**
     * setTab
     * Sets the selected Tab for teh panel to the specifided ObjectInspector panel key
     **/
    public void setTab( int pTabKey) {
        if (pTabKey == NOTES_TAB ) {
            mTabbedPane.setSelectedComponent( mNotePanel);
        } else if (pTabKey == INFO_TAB ) {
            mTabbedPane.setSelectedComponent( mInfoPanel );
        } else if (pTabKey == TREE_TAB ) {
            mTabbedPane.setSelectedComponent( mTreePanel );
        } else if(pTabKey == FILTER_TAB) {
            mTabbedPane.setSelectedComponent(mNodeFilterPanel);
        }
    }
    
    
}



