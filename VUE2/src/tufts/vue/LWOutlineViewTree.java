/*
 * LWOutlineViewTree.java
 *
 * Created on September 23, 2003, 5:18 PM
 */

package tufts.vue;

import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

/**A class which displays the outline view of a given LWMap in a tree form*/
public class LWOutlineViewTree extends InspectorWindow implements LWComponent.Listener, TreeModelListener
{    
    private DisplayAction displayAction = null;
    private JTree tree;
    private LWMap currentMap;
    private LWComponent selectedComponent;
    
    /** Creates a new instance of LWOverviewTree */
    public LWOutlineViewTree(JFrame parent) 
    {
        super(parent, "Outline View");
        setSize(500, 300);
     
        currentMap = null;
        selectedComponent = null;
        
        //initializing the tree with appropriate listener and models
        tree = new JTree();
        tree.setModel(null);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        //tree selection listener to keep track of the selected node 
        tree.addTreeSelectionListener(
            new TreeSelectionListener() 
            {
                public void valueChanged(TreeSelectionEvent e) 
                {
                    LWTreeNode treeNode = (LWTreeNode)tree.getLastSelectedPathComponent();
        
                    if (treeNode == null || treeNode.getUserObject() instanceof String) 
                      selectedComponent = null;

                    else
                      selectedComponent = (LWComponent)treeNode.getUserObject();
                }
            }
        );
        
        JScrollPane scrollPane = new JScrollPane(tree);
        
        getContentPane().add(scrollPane);
        getContentPane().setBackground(Color.white);
        
        super.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e) 
                {
                    displayAction.setButton(false);
                }
            }
        );
    }
    
    public LWOutlineViewTree(JFrame parent, LWMap map)
    {
        this(parent);
        setMap(map.getChildList(), map.getLabel());
    }
    
    /**A method which switches the displayed map*/
    public void switchMap(LWMap newMap)
    {
        //removes itself from the old map's listener list
        if (currentMap != null)
          currentMap.removeLWCListener(this);
        
        //adds itself to the new map's listener list
        currentMap = newMap;
        currentMap.addLWCListener(this);
        
        setMap(currentMap.getChildList(), currentMap.getLabel());
    }
    
    /**A method which sets up the tree for the given map*/
    public void setMap(ArrayList childList, String label)
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
        
        tree.setModel(new DefaultTreeModel(rootNode));
        tree.getModel().addTreeModelListener(this);
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
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent, true);
            
            LWTreeNode addedChildTreeNode = new LWTreeNode(addedChild);
            
            //adds the tree node
            parentTreeNode.add(addedChildTreeNode); 
               
            //for each link associated with the added LWNode, add to the tree node
            for(Iterator i = addedChild.getLinks().iterator(); i.hasNext();)
            {
                LWLink link = (LWLink)i.next();
                addedChildTreeNode.add(new LWTreeNode(link));
            }
               
            //updates the tree
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
        }
             
        //if it is a LWLink
        else if (addedChild instanceof LWLink)
        {
            LWTreeNode linkedTreeNode1, linkedTreeNode2;
            LWLink link = (LWLink)addedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            //finds the tree nodes associated with the two components and adds the tree node representing the link to
            //the two tree nodes
            linkedTreeNode1 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component1, true);
            linkedTreeNode2 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component2, true);
            linkedTreeNode1.add(new LWTreeNode(link));
            linkedTreeNode2.add(new LWTreeNode(link));
            
            //updates the tree
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode1);
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode2);
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
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent, true);
            
            //finds the tree node representing the deleted child
            deletedChildTreeNode = findLWTreeNode(parentTreeNode, deletedChild, false);
             
            //removes from the tree and updates the tree
            parentTreeNode.remove(deletedChildTreeNode);
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
        }
         
        //if it is a LWLink
        else if (deletedChild instanceof LWLink)
        {
            LWTreeNode linkedTreeNode1, linkedTreeNode2;
            LWLink link = (LWLink)deletedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            linkedTreeNode1 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component1, true);
            linkedTreeNode2 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component2, true);
              
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
              
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode1);
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode2);
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
        
        //when children are removed from the map
        else if (message.equals("childrenRemoved"))
        {
            ArrayList childrenList = e.getComponents();
            
            for (Iterator i = childrenList.iterator(); i.hasNext();)
              deleteLWTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        
        //when a label on a node was changed
        else if (message.equals("label"))
          tree.repaint();      
    }
    
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
       
        if(treeNode != (LWTreeNode)tree.getModel().getRoot())
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
    
    /**A method used by VUE to display the tree*/
    public Action getDisplayAction()
    {
        if (displayAction == null)
            displayAction = new DisplayAction("Outline View");
        
        return (Action)displayAction;
    }
    
    /**A class which controls the visibility of the tree */
    private class DisplayAction extends AbstractAction
    {
        private AbstractButton aButton;
        
        public DisplayAction(String label)
        {
            super(label);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            aButton = (AbstractButton) e.getSource();
            setVisible(aButton.isSelected());
        }
        
        public void setButton(boolean state)
        {
            aButton.setSelected(state);
        }
    }
}
