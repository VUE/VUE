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
import javax.swing.ImageIcon;

/**
 * @author  Daisuke Fujiwara
 * Todo: re-write this class with active map listener, and render right
 * from the node labels so all we have to do is repaint to refresh.
 * (still need to modify tree for hierarchy changes tho).
 */

/**A class that represents a tree structure which holds the outline view model*/
public class OutlineViewTree extends JTree implements LWComponent.Listener, TreeModelListener, LWSelection.Listener
{ 
    private LWContainer currentContainer = null;
    private tufts.oki.hierarchy.HierarchyNode selectedNode = null;
    private tufts.oki.hierarchy.OutlineViewHierarchyModel hierarchyModel = null;
     
    private ImageIcon  selectedIcon = null;
    private ImageIcon  nodeIcon = VueResources.getImageIcon("outlineIcon.node");
    private ImageIcon linkIcon = VueResources.getImageIcon("outlineIcon.link");
    private ImageIcon   mapIcon = VueResources.getImageIcon("outlineIcon.map");
    /** Creates a new instance of OverviewTree */
    public OutlineViewTree()
    {
         setModel(null);
         setEditable(true);
         getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
         setCellRenderer(new OutlineViewTreeRenderer());
        
         VUE.getSelection().addListener(this);
         
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
     
                        if (selectedComponent instanceof LWMap)
                          selectedIcon = mapIcon;
                        
                        else if (selectedComponent instanceof LWNode)
                          selectedIcon = nodeIcon;
                        
                        else if (selectedComponent instanceof LWLink)
                          selectedIcon = linkIcon;  
                            
                        //if the selected node is not an instance of LWMap
                        if(!(selectedComponent instanceof LWMap))
                            VUE.getSelection().setTo(selectedComponent);
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
            
            currentContainer.addLWCListener(this, LWKey.Label);
            currentContainer.addLWCListener(hierarchyModel, new Object[] { LWKey.ChildrenAdded, LWKey.ChildrenRemoved } );
        }
        
        else
            setModel(null);
    }
    
    /**A method that sets the current tree path to the one designated by the given LWComponent*/
    public void setSelectionPath(LWComponent component)
    {     
        //in case the node inspector's outline tree is not initalized
        if (hierarchyModel != null)
        {
            TreePath path = hierarchyModel.getTreePath(component);
            super.setSelectionPath(path);
            super.expandPath(path);
            super.scrollPathToVisible(path);
        }
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
        //when a label on a node was changed
        //Already label filtered. ???
        //hierarchyModel.updateNodeDisplayName(e.getComponent());
            
        //repaints the entire tree
        repaint();       
    }
    
    /** A method for handling LWSelection event **/
    public void selectionChanged(LWSelection selection)
    {
        //if it is not an empty selection, select the first element in the selection
        if (!selection.isEmpty())
          setSelectionPath(selection.first());
        
        //else deselect
        else 
          super.setSelectionPath(null);
    }
    
    /**A class that specifies the rendering method of the outline view tree*/
    private class OutlineViewTreeRenderer extends DefaultTreeCellRenderer
    { 
        //private ImageIcon nodeIcon = null, linkIcon = null, mapIcon = null;
        
        public OutlineViewTreeRenderer()
        {
            //retrieves the icons for nodes and links
           
        }
        
        public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) 
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (((DefaultMutableTreeNode)value).getUserObject() instanceof tufts.oki.hierarchy.HierarchyNode)
            {
                tufts.oki.hierarchy.HierarchyNode hierarchyNode = (tufts.oki.hierarchy.HierarchyNode)(((DefaultMutableTreeNode)value).getUserObject());
                LWComponent component = hierarchyNode.getLWComponent();
                
                if (((DefaultMutableTreeNode)getModel().getRoot()).getUserObject().equals(hierarchyNode) || component instanceof LWMap)
                  setIcon(mapIcon);
                
                else if (component instanceof LWNode)
                  setIcon(nodeIcon);
            
                else if (component instanceof LWLink)
                  setIcon(linkIcon);

                //setText(component.getDisplayLabel());
                // need to update size (but only if label has changed)
                //setPreferredSize(getPreferredSize());
                // doesn't appear to get right size if there's a '.' in the name!
            }
            
            else
            {
              if (selectedIcon == null ) System.err.println("OutlineViewTree's icon is null --- problem");
              setIcon(selectedIcon);
            }
            
            return this;
        }
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
}
