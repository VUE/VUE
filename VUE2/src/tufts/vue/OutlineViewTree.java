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
public class OutlineViewTree extends JTree implements LWComponent.Listener, TreeModelListener
{ 
    private LWContainer currentContainer = null;
    private LWComponent selectedComponent = null;
    
    /** Creates a new instance of LWOverviewTree */
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
                    LWTreeNode treeNode = (LWTreeNode)getLastSelectedPathComponent();
        
                    if (treeNode == null || treeNode.getUserObject() instanceof String) 
                       selectedComponent = null;
                                
                    else 
                    {
                        selectedComponent = (LWComponent)treeNode.getUserObject();
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
    
    /**A method which switches the displayed map*/
    public void switchContainer(LWContainer newContainer)
    {
        //removes itself from the old map's listener list
        if (currentContainer != null)
        {
          currentContainer.removeLWCListener(this);
          currentContainer.removeLWCListener((OutlineViewTreeModel)getModel());
        }
        
        //adds itself to the new map's listener list
        if (newContainer != null)
        {
            currentContainer = newContainer;
            
            OutlineViewTreeModel model = new OutlineViewTreeModel(newContainer);
            model.addTreeModelListener(this);
            setModel(model);
            
            currentContainer.addLWCListener(this);
            currentContainer.addLWCListener((OutlineViewTreeModel)getModel());
        }
        
        else
            setModel(null);
    }
    
    public void treeNodesChanged(TreeModelEvent e)
    {
        //retrieves the selected node
        LWTreeNode treeNode = (LWTreeNode)(e.getTreePath().getLastPathComponent());
        
        //it appropriate retrieves the child of the selected node
        try 
        {
            int index = e.getChildIndices()[0];
            treeNode = (LWTreeNode)(treeNode.getChildAt(index));
        } 
        
        catch (NullPointerException exc) {}
       
        //might want to come up with an exception
        if(treeNode != (LWTreeNode)getModel().getRoot())
        {
            //changes the node's label and sets it as a new object of the tree node
            selectedComponent.setLabel(treeNode.toString());
            treeNode.setUserObject(selectedComponent);
        }
        
        //VUE.getActiveViewer().repaint();
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
