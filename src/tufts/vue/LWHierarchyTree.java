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

/*
 * HierarchyTree.java
 *
 * Created on September 12, 2003, 12:55 AM
 */

package tufts.vue;

import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class that displays the hierarchy of nodes in a tree*/
//public class LWHierarchyTree extends InspectorWindow implements TreeModelListener
public class LWHierarchyTree extends JPanel implements TreeModelListener
{
    //private DisplayAction displayAction = null;
    private JTree tree;
    private tufts.oki.hierarchy.HierarchyNode selectedNode = null;
    
    /** Creates a new instance of HierarchyTreeWindow */
    //    public LWHierarchyTree(JFrame parent) 
    //    {
    //  super(parent, "Hierarchy Tree");

    public LWHierarchyTree() 
    {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, 300));
        
        /**creating a hierarchy tree*/
        tree = new JTree();
        tree.setModel(null);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        /*currently commented out due to the interface complication
        //mouse listener to let the user open up the resource associated with the selected tree node
        tree.addMouseListener
        (
            new MouseAdapter()
            {
                public void mouseClicked(MouseEvent me)
                {
                    //determines the row and the path of the selected node
                    int selRow = tree.getRowForLocation(me.getX(), me.getY());
                    TreePath selPath = tree.getPathForLocation(me.getX(), me.getY());
                
                    //if there is a selected row
                    if(selRow != -1) 
                    {       
                        //if the mouse click is a double click, then display the resource associated with the node
                        if(me.getClickCount() == 2) 
                        {
                            LWNode clickedNode = 
                               (LWNode)((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
                            clickedNode.getResource().displayContent();
                        }
                    }
                }
            }
        );
         */
        
        //tree selection listener to keep track of the selected node 
        tree.addTreeSelectionListener(
            new TreeSelectionListener() 
            { 
                public void valueChanged(TreeSelectionEvent e) 
                {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                    
                    //if there is no selected node
                    if (treeNode == null)
                      selectedNode = null;
                                
                    else 
                    {
                        //retrieves the LWComponent associated with the selected tree node
                        selectedNode = (tufts.oki.hierarchy.HierarchyNode)treeNode.getUserObject();
                        LWComponent selectedComponent = selectedNode.getLWComponent();
                        
                        //if the selected node is not an instance of LWMap
                        //if(!(selectedComponent instanceof LWMap))
                          //VUE.getSelection().setTo(selectedComponent);
                    }
                }
            }
        );

        JScrollPane scrollPane = new JScrollPane(tree);
        
        //getContentPane().add(scrollPane);
        //getContentPane().setBackground(Color.white);
        add(scrollPane);
        setBackground(Color.white);
    }
    
     /**A method that deals with dynamic changes to the tree element*/
    public void treeNodesChanged(TreeModelEvent e)
    {
        //retrieves the selected node
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());
        
        //it appropriate retrieves the child of the selected node
        try 
        {
            int index = e.getChildIndices()[0];
            treeNode = (DefaultMutableTreeNode)(treeNode.getChildAt(index));
        } 
        
        catch (NullPointerException exc) {}
       
        //might want to come up with an exception
        if(treeNode != (DefaultMutableTreeNode)tree.getModel().getRoot())
        {
            //changes the node's label and sets it as a new object of the tree node
            try
            {
                selectedNode.changeLWComponentLabel(treeNode.toString());
                treeNode.setUserObject(selectedNode);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {
                //resets the change to the previous one
                treeNode.setUserObject(selectedNode);
            }
        }
    }
    
    /**unused portion of the interface*/
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
    
    /**Sets the model of the tree to the given hierarchy tree model
       Also stores the reference to the model*/
    public void setHierarchyModel(tufts.oki.hierarchy.HierarchyViewHierarchyModel hierarchyModel)
    {
        if (hierarchyModel != null)
        {
            DefaultTreeModel model = hierarchyModel.getTreeModel();
            model.addTreeModelListener(this);
            tree.setModel(model);
        }
        
        else
          tree.setModel(null);
    } 
}
