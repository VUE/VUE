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
        
                    if (treeNode == null) 
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
    
    public void switchMap(LWMap newMap)
    {
        if (currentMap != null)
          currentMap.removeLWCListener(this);
        
        currentMap = newMap;
        currentMap.addLWCListener(this);
        
        setMap(currentMap.getChildList(), currentMap.getLabel());
    }
    
    public void setMap(ArrayList childList, String label)
    {
        LWTreeNode rootNode = new LWTreeNode(label);
       
        for(Iterator i = childList.iterator(); i.hasNext();)
        {
            LWComponent component = (LWComponent)i.next();
            
            if (component instanceof LWNode)
              rootNode.add(setUpOverview((LWNode)component));
            
            /*commented out because this was for different implementation
            else if (component instanceof LWLink)
              rootNode.add(new LWTreeNode((LWLink)component));
             */
        }
        
        tree.setModel(new DefaultTreeModel(rootNode));
        tree.getModel().addTreeModelListener(this);
    }
    
    public LWTreeNode setUpOverview(LWNode node)
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
                  treeNode.add(setUpOverview((LWNode)component));
            } 
        }
        
        return treeNode;
    }
    
    public void addLWTreeNode(LWContainer parent, LWComponent addedChild)
    {      
        if (addedChild instanceof LWNode)
        {
            LWTreeNode parentTreeNode;
            
            if (parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent, true);
            
            LWTreeNode addedChildTreeNode = new LWTreeNode(addedChild);
            
            parentTreeNode.add(addedChildTreeNode); 
               
            for(Iterator i = addedChild.getLinks().iterator(); i.hasNext();)
            {
                LWLink link = (LWLink)i.next();
                addedChildTreeNode.add(new LWTreeNode(link));
            }
               
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
        }
             
        else if (addedChild instanceof LWLink)
        {
            LWTreeNode linkedTreeNode1, linkedTreeNode2;
            LWLink link = (LWLink)addedChild;
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            linkedTreeNode1 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component1, true);
            linkedTreeNode2 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component2, true);
            linkedTreeNode1.add(new LWTreeNode(link));
            linkedTreeNode2.add(new LWTreeNode(link));
            
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode1);
            ((DefaultTreeModel)tree.getModel()).reload(linkedTreeNode2);
        }
    }
    
    public void deleteLWTreeNode(LWContainer parent, LWComponent deletedChild)
    {    
        if (deletedChild instanceof LWNode)
        {
            LWTreeNode parentTreeNode, deletedChildTreeNode = null;
            
            if (parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent, true);
            
            deletedChildTreeNode = findLWTreeNode(parentTreeNode, deletedChild, false);
             
            parentTreeNode.remove(deletedChildTreeNode);
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
          }
            
          else if (deletedChild instanceof LWLink)
          {
              LWTreeNode linkedTreeNode1, linkedTreeNode2;
              LWLink link = (LWLink)deletedChild;
              LWComponent component1 = link.getComponent1();
              LWComponent component2 = link.getComponent2();
         
              linkedTreeNode1 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component1, true);
              linkedTreeNode2 = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), component2, true);
              
              //one of them is a null for sure
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
     
    public void LWCChanged(LWCEvent e)
    {
        String message = e.getWhat();
        
        if (message.equals("childAdded")) 
          addLWTreeNode((LWContainer)e.getSource(), e.getComponent());
            
        
        else if (message.equals("childRemoved"))
          deleteLWTreeNode((LWContainer)e.getSource(), e.getComponent());
        
        else if (message.equals("childrenRemoved"))
        {
            ArrayList childrenList = e.getComponents();
            
            for (Iterator i = childrenList.iterator(); i.hasNext();)
              deleteLWTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        
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
       
        //changes the node's label and sets it as a new object for the tree node
        selectedComponent.setLabel(treeNode.toString());
        treeNode.setUserObject(selectedComponent);
        
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
