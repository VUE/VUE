 /*
 * Copyright 2003-2007 Tufts University  Licensed under the
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


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import tufts.vue.gui.*;
import tufts.vue.filter.*;

/**
 *
 * @deprecated -- no longer in use
 *
 * The Object Inspector Panel!
 *
 * @version $Revision: 1.1 $ / $Date: 2008-06-19 03:46:58 $ / $Author: sfraize $ 
 *
 */
public class ObjectInspectorPanel extends JPanel
    implements LWSelection.Listener
{
    private static final boolean WidgetStackImpl = false;

    public static final int INFO_TAB = 0;
    public static final int NOTES_TAB = 1;
    public static final int TREE_TAB = 2;
    public static final int FILTER_TAB = 3;
    
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
    
    public ObjectInspectorPanel()
    {
        setMinimumSize( new Dimension( 240,200) );
        
        //mInfoPanel = new InfoPanel();
        mTreePanel = new TreePanel();
        mNotePanel = new NotePanel();
        mNodeFilterPanel = new NodeFilterPanel();
            
        setLayout( new BorderLayout() );
        
        if (WidgetStackImpl) {
            WidgetStack stack = new WidgetStack();
            stack.addPane(mNotePanel);
            stack.addPane(mNodeFilterPanel);
            stack.addPane(mTreePanel);
            add(stack);
            Widget.setExpanded(mTreePanel, false);
        } else {
            setOpaque(false);
            setBorder( new EmptyBorder( 5,5,5,5) );
            mTabbedPane = new JTabbedPane();
            VueResources.initComponent( mTabbedPane, "tabPane");
        
            //mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
            mTabbedPane.addTab( mTreePanel.getName(),  mTreePanel);
            mTabbedPane.addTab( mNotePanel.getName(), mNotePanel);
            mTabbedPane.addTab(mNodeFilterPanel.getName(),mNodeFilterPanel);
            add( BorderLayout.CENTER, mTabbedPane );
        }

        VUE.getSelection().addListener(this);
        
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
        //mInfoPanel.updatePanel( mComponent);
        mTreePanel.updatePanel( mComponent);
        //mNotePanel.updatePanel( mComponent);
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
        JLabel objectLabel;
        //JPanel previewPanel = new JPanel(new BorderLayout());
        JComponent mPreview = null;
        LWComponent mComponent;
        public InfoPanel() {
            setLayout( new BorderLayout() );
            setName("infoPanel");
            setOpaque(false);
            
            //setBorder( BorderFactory.createEmptyBorder(10,10,10,6));
            
            objectLabel = new JLabel("Node");
            objectLabel.setFont(VueConstants.FONT_MEDIUM_BOLD);
            objectLabel.setHorizontalTextPosition(JLabel.LEFT);
            objectLabel.setVerticalAlignment(JLabel.TOP);
            labelPanel = new JPanel(new BorderLayout());
            labelPanel.setOpaque(false);
            labelPanel.setBorder( BorderFactory.createEmptyBorder(0,0,5,0));
            labelPanel.add(objectLabel,BorderLayout.WEST);
            //previewPanel.setPreferredSize(new Dimension(75,75));
            //previewPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            //previewPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            //labelPanel.add(previewPanel,BorderLayout.EAST);
            
            //setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
            setBorder(BorderFactory.createEmptyBorder(3,4,0,4));
            //setBorder(BorderFactory.createLineBorder(Color.red, 1));
            //setBorder(null);
            
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
            mComponent = pComponent;
            if (DEBUG.Enabled)
                objectLabel.setText(mComponent.getUniqueComponentTypeLabel());
            else
                objectLabel.setText(mComponent.getComponentTypeLabel());
            // To get the preview, the resource may have to do an initial
            // load of the content, which may take a while, so we do this
            // in a thread.
            /// Remove Preview
            /**
            new Thread() {
                public void run() {
                    JComponent preview = null;
                    if (mComponent.hasResource())
                        preview = mComponent.getResource().getPreview();

                    if (mPreview != preview) {
                        // the preview JComponent is diferrent: change display
                        if (mPreview != null)
                            previewPanel.remove(mPreview);
                        mPreview = preview;
                        if (mPreview != null)
                            previewPanel.add(mPreview, BorderLayout.CENTER);
                        // panel doesn't always update (probably threading issue)
                        // so we throw alot out and hope it sticks:
                        previewPanel.repaint();
                        validate();
                        repaint();
                    }
                }
            }.start();
             */
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
            
            setName("treePanel");
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
            
            setName("nodeFilterPanel");
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
                if (VUE.getActiveMap() != null && pComponent.getNodeFilter() != null) {
                    // NodeFilter bombs entirely if no active map, so don't let
                    // it mess us up if there isn't one.
                    nodeFilterEditor = new NodeFilterEditor(pComponent.getNodeFilter(),true);
                    add(nodeFilterEditor,BorderLayout.CENTER);
                }
            }
            validate();
        }
    }
    
    
    /**
     * setTab
     * Sets the selected Tab for teh panel to the specifided ObjectInspector panel key
     **/
    public void setTab( int pTabKey) {
        JComponent picked = null;
        if (pTabKey == NOTES_TAB ) {
            picked = mNotePanel;
        } else if (pTabKey == INFO_TAB ) {
            picked = mInfoPanel;
        } else if (pTabKey == TREE_TAB ) {
            picked = mTreePanel;
        } else if(pTabKey == FILTER_TAB) {
            picked = mNodeFilterPanel;
        }
        if (picked != null) {
            if (WidgetStackImpl)
                Widget.setExpanded(picked, true);
            else
                mTabbedPane.setSelectedComponent(picked);
        }
        Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (w != null)
            w.setVisible(true);
    }

    public static void main(String args[]) {
        VUE.parseArgs(args);
        VUE.initUI();
        new Frame("A Frame").setVisible(true);
        ObjectInspectorPanel p = new ObjectInspectorPanel();
        //LWMap map = new LWMap("TestMap");
        LWComponent node = new LWNode("Test Node");
        node.setNotes("I am a note.");
        node.setResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        Resource r = node.getResource();
        for (int i = 10; i < 31; i++)
            r.setProperty("field_" + i, "value_" + i);
        //map.addLWC(node);
        tufts.vue.gui.DockWindow w = tufts.vue.gui.GUI.createDockWindow("Object Inspector");
        w.setContent(p);
        w.setVisible(true);
        p.setLWComponent(node);
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this
    }
}



