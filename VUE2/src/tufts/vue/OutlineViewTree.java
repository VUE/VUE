/*
 * OutlineViewTree.java
 *
 * Created on December 1, 2003, 1:07 AM
 */

package tufts.vue;

import java.awt.*;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class OutlineViewTree extends JTree implements LWComponent.Listener, TreeModelListener
{ 
    private LWContainer currentContainer = null;
    private DefaultTreeModel treeModel = null;
    private LWComponent selectedComponent = null;
    
    /** Creates a new instance of LWOverviewTree */
    public OutlineViewTree(LWContainer container)
    {
         System.out.println("creating");
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
                    
        currentContainer = container;
        currentContainer.addLWCListener(this);
        setUpTree(container.getChildList(), container.getLabel());
        
    }
    
    /**A method which switches the displayed map*/
    public void switchContainer(LWContainer newContainer)
    {
        //removes itself from the old map's listener list
        if (currentContainer != null)
          currentContainer.removeLWCListener(this);
        
        //adds itself to the new map's listener list
        
        if (newContainer != null)
        {
            currentContainer = newContainer;
            currentContainer.addLWCListener(this);
        
            setUpTree(currentContainer.getChildList(), currentContainer.getLabel());
        }
        
        else
            setModel(null);
    }
    
    /**A method which sets up the tree for the given container*/
    public void setUpTree(ArrayList childList, String label)
    {
        LWTreeNode rootNode = new LWTreeNode(label);
       
        for(Iterator i = childList.iterator(); i.hasNext();)
        {
            LWComponent component = (LWComponent)i.next();
            
            if (component instanceof LWNode)
              rootNode.add(setUpTreeNode((LWNode)component));
            
            /*commented out because this was for different implementation
            else if (component instanceof LWLink)
              rootNode.add(new LWTreeNode((LWLink)component));
             */
        }
        
        setModel(new DefaultTreeModel(rootNode));
        getModel().addTreeModelListener(this);
    }
    
    
    /**A method which sets up the tree node*/
    public LWTreeNode setUpTreeNode(LWNode node)
    {   
        LWTreeNode treeNode = new LWTreeNode(node);
        
        for (Iterator li = node.getLinks().iterator(); li.hasNext();)
        {
            LWLink link = (LWLink)li.next();
            treeNode.add(new LWTreeNode(link));
        }
        
        if (node.getChildList().size() > 0)
        {
            //calls itself on the children nodes
            for (Iterator i = node.getChildIterator(); i.hasNext();)
            {
                LWComponent component = (LWComponent)i.next();
            
                if (component instanceof LWNode)
                  treeNode.add(setUpTreeNode((LWNode)component));
            } 
        }
        
        return treeNode;
    }
    
    /**Adds a new tree node*/
    public void addLWTreeNode(LWContainer parent, LWComponent addedChild)
    {      
        //if it is a LWNode
        if (addedChild instanceof LWNode)
        {
            LWTreeNode parentTreeNode;
            
            //finds the parent tree node
            if (parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)getModel().getRoot(), parent, true);
            
            LWTreeNode addedChildTreeNode = new LWTreeNode(addedChild);
            
            if (parentTreeNode == null) {
                System.err.println("*** NULL parentTreeNode in LWOutlineViewTree");
                // don't know what right thing to do here, but this exception
                // was driving me crazy -- SMF 2003-11-13 18:19.04
                return;
            }

            //adds the tree node
            parentTreeNode.add(addedChildTreeNode); 
               
            //for each link associated with the added LWNode, add to the tree node
            for (Iterator i = addedChild.getLinks().iterator(); i.hasNext();)
            {
                LWLink link = (LWLink)i.next();
                addedChildTreeNode.add(new LWTreeNode(link));
            }
               
            //adding anything that the added node contains
            for (Iterator nodeIterator = ((LWNode)addedChild).getNodeIterator(); nodeIterator.hasNext();)
            {
                LWNode subNode = (LWNode)nodeIterator.next();
                LWTreeNode subTreeNode = new LWTreeNode(subNode);
                
                addedChildTreeNode.add(subTreeNode);
                
                for (Iterator linkIterator = subNode.getLinks().iterator(); linkIterator.hasNext();)
                {
                    LWLink link = (LWLink)linkIterator.next();
                    subTreeNode.add(new LWTreeNode(link));
                }
            }
            
            //updates the tree
            ((DefaultTreeModel)getModel()).reload(parentTreeNode);
            scrollPathToVisible(new TreePath(parentTreeNode.getPath()));
        }
             
        //if it is a LWLink
        else if (addedChild instanceof LWLink)
        {
            LWTreeNode linkedTreeNode1 = null;
            LWTreeNode linkedTreeNode2 = null;
            LWLink link = (LWLink)addedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            //finds the tree nodes associated with the two components and adds the tree node representing the link to
            //the two tree nodes
            if (component1 != null) {
                linkedTreeNode1 = findLWTreeNode((LWTreeNode)getModel().getRoot(), component1, true);
                if (linkedTreeNode1 != null) {
                    linkedTreeNode1.add(new LWTreeNode(link));
                    ((DefaultTreeModel)getModel()).reload(linkedTreeNode1);
                }
            }
            if (component2 != null) {
                linkedTreeNode2 = findLWTreeNode((LWTreeNode)getModel().getRoot(), component2, true);
                if (linkedTreeNode2 != null) {
                    linkedTreeNode2.add(new LWTreeNode(link));
                    ((DefaultTreeModel)getModel()).reload(linkedTreeNode2);
                }
            }
            
            //updates the tree

            if (linkedTreeNode1 != null)
                scrollPathToVisible(new TreePath(linkedTreeNode1.getPath()));
            if (linkedTreeNode2 != null)
                scrollPathToVisible(new TreePath(linkedTreeNode2.getPath()));
        }
    }
    
    /**Deletes a tree node*/
    public void deleteLWTreeNode(LWContainer parent, LWComponent deletedChild)
    {    
        //if it is a LWNode
        if (deletedChild instanceof LWNode)
        {
            LWTreeNode parentTreeNode, deletedChildTreeNode = null;
            
            //finds the parent tree node
            if (parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)getModel().getRoot(), parent, true);
            
            //finds the tree node representing the deleted child
            deletedChildTreeNode = findLWTreeNode(parentTreeNode, deletedChild, false);
             
            //removes from the tree and updates the tree
            parentTreeNode.remove(deletedChildTreeNode);
            ((DefaultTreeModel)getModel()).reload(parentTreeNode);
        }
         
        //if it is a LWLink
        else if (deletedChild instanceof LWLink)
        {
            LWTreeNode linkedTreeNode1, linkedTreeNode2;
            LWLink link = (LWLink)deletedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            linkedTreeNode1 = findLWTreeNode((LWTreeNode)getModel().getRoot(), component1, true);
            linkedTreeNode2 = findLWTreeNode((LWTreeNode)getModel().getRoot(), component2, true);
              
            //finds the tree nodes associated with the two components and deletes the tree node representing the link to
            //the two tree nodes
            //must check to see if the parent wasn't deleted in the process
            if (linkedTreeNode1 != null)
            {
              LWTreeNode linkNode1 = findLWTreeNode(linkedTreeNode1, link, false);
              linkedTreeNode1.remove(linkNode1);
            }
              
            if (linkedTreeNode2 != null)
            {
              LWTreeNode linkNode2 = findLWTreeNode(linkedTreeNode2, link, false);
              linkedTreeNode2.remove(linkNode2);
            }
              
            ((DefaultTreeModel)getModel()).reload(linkedTreeNode1);
            ((DefaultTreeModel)getModel()).reload(linkedTreeNode2);
        }
    }
    
    /**A method which finds a tree node representing the given component under the given tree node
       A boolean flag is used to determine whether to search for the node recursively in sub-levels
     */
    public LWTreeNode findLWTreeNode(LWTreeNode treeNode, LWComponent component, boolean recursive)
    {   
        LWTreeNode foundTreeNode = null;
        
        for (Enumeration enum = treeNode.children(); enum.hasMoreElements();)
        {
            LWTreeNode childNode = (LWTreeNode)enum.nextElement();
            
            if(childNode.getUserObject().equals(component))
            {
                foundTreeNode = childNode;
                break;
            }
            
            else if (recursive)
            {
                childNode = findLWTreeNode(childNode, component, true);
                
                if(childNode != null && childNode.getUserObject().equals(component))
                {
                    foundTreeNode = childNode;
                    break;
                }
            }
        }
        
        return foundTreeNode;
    }
     
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        String message = e.getWhat();
        
        //when a child is added to the map
        if (message.equals("childAdded")) 
          addLWTreeNode((LWContainer)e.getSource(), e.getComponent());
            
        //when a child is removed from the map
        else if (message.equals("childRemoved"))
          deleteLWTreeNode((LWContainer)e.getSource(), e.getComponent());

        //when children added to the map
        else if (message.equals("childrenAdded"))
        {
            ArrayList childrenList = e.getComponents();
            for (Iterator i = childrenList.iterator(); i.hasNext();)
                addLWTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        //when children are removed from the map
        else if (message.equals("childrenRemoved"))
        {
            ArrayList childrenList = e.getComponents();
            for (Iterator i = childrenList.iterator(); i.hasNext();)
                deleteLWTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        
        //when a label on a node was changed
        else if (message.equals("label"))
          repaint();      
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
}
