/*
 * HierarchyTreeModel.java
 *
 * Created on September 9, 2003, 4:05 PM
 */

package tufts.vue;

import javax.swing.JTree;
import javax.swing.tree.*;
import java.util.Iterator;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class which represents a model for the hierarchy tree*/
public class HierarchyTreeModel implements TreeModelListener
{
    private DefaultTreeModel model;
    private LWNode selectedNode = null;
    
    /** Creates a new instance of HierarchyTree */
    public HierarchyTreeModel(LWNode rootNode) 
    {
        model = new DefaultTreeModel(setUpHierarchy(rootNode, null));
        model.addTreeModelListener(this);
    }
    
    /**Returns the model in the form the tree can use*/
    public DefaultTreeModel getModel()
    {   
        return model;  
    }
    
    /**Sets the currently selected node to the given node*/
    public void setSelectedNode(LWNode node)
    {
        selectedNode = node;
    }
    
    /**Sets up the tree hierarchy in a recursive fashion
       As arguments it takes the current node and its parent node*/
    public LWTreeNode setUpHierarchy(LWNode node, LWNode parentNode)
    {   
        LWTreeNode treeNode = new LWTreeNode(node);
        
        //calls itself on the children nodes
        for (Iterator i = node.getLinks().iterator(); i.hasNext();)
        {
            LWLink link = (LWLink)i.next();
            LWNode nextNode = null;
            
            if ((nextNode = (LWNode)link.getComponent1()) == node)
              nextNode = (LWNode)link.getComponent2();
            
            if (!nextNode.equals(parentNode))
              treeNode.add(setUpHierarchy(nextNode, node));
        }
        
        return treeNode;
    }
    
    /**A method which is invoked when the tree node is modified*/
    public void treeNodesChanged(TreeModelEvent e) 
    {
        //retrieves the selected node
        LWTreeNode treeNode = (LWTreeNode)(e.getTreePath().getLastPathComponent());
        
        //if appropriate retrieves the child of the selected node
        try 
        {
            int index = e.getChildIndices()[0];
            treeNode = (LWTreeNode)(treeNode.getChildAt(index));
        } 
        
        catch (NullPointerException exc) {}
        
        //changes the node's label and sets it as a new object for the tree node
        selectedNode.setLabel(treeNode.toString());
        treeNode.setUserObject(selectedNode);
    }
    
    /**unused portion of the interface*/
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
}
