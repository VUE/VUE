/*
 * OutlineViewTree.java
 *
 * Created on December 1, 2003, 1:07 AM
 */

package tufts.vue;

import java.awt.*;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class that represents a tree structure which holds the outline view model*/
public class OutlineViewTree extends JTree implements LWComponent.Listener, TreeModelListener
{ 
    private LWContainer currentContainer = null;
    private tufts.oki.hierarchy.HierarchyNode selectedNode = null;
    private tufts.oki.hierarchy.OutlineViewHierarchyModel hierarchyModel = null;

    /** Creates a new instance of OverviewTree */
    public OutlineViewTree()
    {
         setModel(null);
         setEditable(true);
         getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
         //tree selection listener to keep track of the selected node 
         addTreeSelectionListener(
            new TreeSelectionListener() 
            {
                public void valueChanged(TreeSelectionEvent e) 
                {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)getLastSelectedPathComponent();
                    
                    //if there is no selected node
                    if (treeNode == null)
                      selectedNode = null;
                                
                    else 
                    {
                        //retrieves the LWComponent associated with the selected tree node
                        selectedNode = (tufts.oki.hierarchy.HierarchyNode)treeNode.getUserObject();
                        LWComponent selectedComponent = selectedNode.getLWComponent();
                        
                        //if the selected node is not an instance of LWMap
                        if(!(selectedComponent instanceof LWMap))
                          VUE.ModelSelection.setTo(selectedComponent);
                    }
                }
            }
        );
    }
    
    public OutlineViewTree(LWContainer container)
    {
        this(); 
        switchContainer(container);
    }
    
    /**A method which switches the displayed container*/
    public void switchContainer(LWContainer newContainer)
    {
        //removes itself from the old container's listener list
        if (currentContainer != null)
        {
          currentContainer.removeLWCListener(this);
          currentContainer.removeLWCListener(hierarchyModel);
        }
        
        //adds itself to the new container's listener list
        if (newContainer != null)
        {
            currentContainer = newContainer;
            
            //creates the new model for the tree with the given new LWContainer
            hierarchyModel = new tufts.oki.hierarchy.OutlineViewHierarchyModel(newContainer);
            DefaultTreeModel model = hierarchyModel.getTreeModel();
            
            model.addTreeModelListener(this);
            setModel(model);
            
            currentContainer.addLWCListener(this);
            currentContainer.addLWCListener(hierarchyModel);
        }
        
        else
            setModel(null);
    }
    
    public void setSelectionPath(LWComponent component)
    {
        TreePath path = hierarchyModel.getTreePath(component);
        super.setSelectionPath(path);
        super.expandPath(path);
        super.scrollPathToVisible(path);
    }
    
    /**A wrapper method which determines whether the underlying model contains a node with the given component*/
    public boolean contains(LWComponent component)
    {
       return hierarchyModel.contains(component);
    }
    
    /**A method which returns whether the model has been intialized or not*/
    public boolean isInitialized()
    {
        if (hierarchyModel != null)
          return true;
        
        else
          return false;
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
        if(treeNode != (DefaultMutableTreeNode)getModel().getRoot())
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
    
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        String message = e.getWhat();
        
        //when a label on a node was changed
        if (message.equals("label"))
          repaint();      
    }
}
