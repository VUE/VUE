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
public class HierarchyTreeModel implements TreeModelListener
{
    private DefaultTreeModel model;
    
    /** Creates a new instance of HierarchyTree */
    public HierarchyTreeModel(LWNode rootNode) 
    {
        model = new DefaultTreeModel(setUpHierarchy(rootNode, null));
        model.addTreeModelListener(this);
    }
    
    public DefaultTreeModel getModel()
    {   
        return model;  
    }
    
    public DefaultMutableTreeNode setUpHierarchy(LWNode node, LWNode parentNode)
    {   
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        
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
    
    public void treeNodesChanged(TreeModelEvent e) 
    {
        //verify this part
        
        DefaultMutableTreeNode treeNode;
        treeNode = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());
        
        try 
        {
            int index = e.getChildIndices()[0];
            treeNode = (DefaultMutableTreeNode)(treeNode.getChildAt(index));
        } 
        
        catch (NullPointerException exc) {}

        //not working
        LWNode node = (LWNode)treeNode.getUserObject();
        node.setLabel(treeNode.toString());
        
        System.out.println("changed to " + node.toString());
    }
    
    /**non used portion of the interface*/
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
}
