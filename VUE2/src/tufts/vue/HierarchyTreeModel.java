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
        model = new DefaultTreeModel(setUpHierarchy(rootNode));
        model.addTreeModelListener(this);
    }
    
    public DefaultTreeModel getModel()
    {   
        return model;  
    }
    
    public DefaultMutableTreeNode setUpHierarchy(LWNode node)
    {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        
        for(Iterator i = node.getNodeIterator(); i.hasNext();)
        {
            LWNode nextNode = (LWNode)i.next();
            treeNode.add(setUpHierarchy(nextNode));
        }
        
        return treeNode;
    }
    
    public void treeNodesChanged(TreeModelEvent e) 
    {
        //verify this part
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());
    }
    
    /**non used portion of the interface*/
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
}
