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
 **  NodeInspectorPanel.java
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


import tufts.vue.filter.*;

/**
 * NodeInspectorPanel
 *
 * The Node  Inspector Panel!
 *
 * \**/
public class NodeInspectorPanel  extends JPanel implements ObjectInspectorPanel.InspectorCard {
    
    
    /////////////
    // Fields
    //////////////
    
    /** The tabbed panel **/
    JTabbedPane mTabbedPane = null;
    
    /** The node we are inspecting **/
    LWNode mNode = null;
    
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
    
    public NodeInspectorPanel() {
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
    }
    
    
    
    ////////////////////
    // Methods
    ///////////////////
    
    
    /**
     * setNode
     * Sets the LWMap component and updates teh display
     *
     * @param pNode - the LWMap to inspect
     **/
    public void setNode( LWNode pNode) {
        
        // if we have a change in maps...
        if( pNode != mNode) {
            mNode = pNode;
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
        
        mInfoPanel.updatePanel( mNode);
        mTreePanel.updatePanel( mNode);
        mNotePanel.updatePanel( mNode);
        mNodeFilterPanel.updatePanel(mNode);
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
        
            JLabel nodeLabel = new JLabel("Node");
            nodeLabel.setFont(VueConstants.FONT_MEDIUM_BOLD);
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.setBorder( BorderFactory.createEmptyBorder(0,0,5,0));
            labelPanel.add(nodeLabel);
            // DEMO FIXX:  Demo hack
            mInfoBox.add(labelPanel);
            mInfoBox.add( new LWCInfoPanel() );
       
            mInfoScrollPane = new JScrollPane();
            mInfoScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            mInfoScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mInfoScrollPane.setLocation(new Point(8, 9));
            mInfoScrollPane.setVisible(true);
            mInfoScrollPane.getViewport().add(mInfoBox);
            mInfoScrollPane.setBorder(BorderFactory.createEmptyBorder());
            //mInfoScrollPane.getViewport().add(new JPanel());
             
            setBorder( BorderFactory.createEmptyBorder(10,10,0,6));
            add(mInfoScrollPane,BorderLayout.NORTH );
            
            //mInfoBox.add( new JLabel("Node Info") );
            // mInfoBox.add( new PropertyPanel() );;
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
        public void updatePanel( LWNode pNode) {
            // update the display
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
                name = "Node Tree";
            }
            return name;
        }
        
        
        /**
         * updatePanel
         * This updates the Panel display based on a new LWMap
         *
         **/
        public void updatePanel( LWNode pNode) {
            // update display based on the LWNode
            
            //if the tree is not intiliazed, hidden, or doesn't contain the given node,
            //then it switches the model of the tree using the given node
            if (!tree.isInitialized() || !this.isVisible() || !tree.contains(pNode)) {
                //panelLabel.setText("Node: " + pNode.getLabel());
                tree.switchContainer(pNode);
            }
            
            //if the node is in the model and the panel is visible and intialized,
            //then it sets the selected path to the one which ends with the given node
            else
                tree.setSelectionPath(pNode);
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
        public void updatePanel( LWNode node) {
            // update the display
            if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + node);
            if(nodeFilterEditor!= null) {
                nodeFilterEditor.setNodeFilter(node.getNodeFilter());
            }else {
                nodeFilterEditor = new NodeFilterEditor(node.getNodeFilter(),true);
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
        if (pTabKey == ObjectInspectorPanel.NOTES_TAB ) {
            mTabbedPane.setSelectedComponent( mNotePanel);
        } else if (pTabKey == ObjectInspectorPanel.INFO_TAB ) {
            mTabbedPane.setSelectedComponent( mInfoPanel );
        } else if (pTabKey == ObjectInspectorPanel.TREE_TAB ) {
            mTabbedPane.setSelectedComponent( mTreePanel );
        } else if(pTabKey == ObjectInspectorPanel.FILTER_TAB) {
            mTabbedPane.setSelectedComponent(mNodeFilterPanel);
        }
    }
    
    
}





