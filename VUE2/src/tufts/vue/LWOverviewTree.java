/*
 * LWOverviewTree.java
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
public class LWOverviewTree extends InspectorWindow implements LWComponent.Listener, TreeModelListener
{    
    private DisplayAction displayAction = null;
    private JTree tree;
    private LWMap currentMap;
    private LWNode selectedNode;
    
    /** Creates a new instance of LWOverviewTree */
    public LWOverviewTree(JFrame parent) 
    {
        super(parent, "Overview Tree");
        setSize(500, 300);
     
        currentMap = null;
        selectedNode = null;
        
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
                      selectedNode = null;

                    else
                      selectedNode = (LWNode)treeNode.getUserObject();
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
    
    public LWOverviewTree(JFrame parent, LWMap map)
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
    
    public void LWCChanged(LWCEvent e)
    {
        String message = e.getWhat();
        
        if (message.equals("childAdded")) 
        {
            LWContainer parent = (LWContainer)e.getSource();
            LWTreeNode parentTreeNode;
            
            if(parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent);
         
            //no need to check what type??
            parentTreeNode.add(new LWTreeNode(e.getComponent()));         
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
            
            //LWMap map = VUE.getActiveMap();
            //setMap(map.getChildList(), map.getLabel());
        }
        
        else if (message.equals("childRemoved"))
        {
            LWContainer parent = (LWContainer)e.getSource();
            LWComponent deletedChild = e.getComponent();
            
            LWTreeNode parentTreeNode, deletedChildTreeNode = null;
            
            if(parent instanceof LWMap)
              parentTreeNode = (LWTreeNode)tree.getModel().getRoot();
            
            else
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent);
            
            for (Enumeration enum = parentTreeNode.children(); enum.hasMoreElements();)
            {
                LWTreeNode treeNode = (LWTreeNode)enum.nextElement();
                
                //??
                if (treeNode.getUserObject().equals(deletedChild))
                {
                    deletedChildTreeNode = treeNode;
                    break;
                }   
            }
         
            //no need to check what type??
            parentTreeNode.remove(deletedChildTreeNode);         
            ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
        }
        
        else if (message.equals("childrenRemoved"))
        {
            
            LWContainer parent = (LWContainer)e.getSource();
            LWTreeNode parentTreeNode;
            
            if(parent instanceof LWMap)
              setMap(parent.getChildList(), parent.getLabel());
            
            else
            {
              parentTreeNode = findLWTreeNode((LWTreeNode)tree.getModel().getRoot(), parent);
              
              parentTreeNode.removeAllChildren();
              
              for (Iterator i = parent.getChildIterator(); i.hasNext();)
              {
                LWComponent component = (LWComponent)i.next();
            
                if (component instanceof LWNode)
                  parentTreeNode.add(setUpOverview((LWNode)component));
              }
              
              ((DefaultTreeModel)tree.getModel()).reload(parentTreeNode);
            }
        }
        
        else if (message.equals("label"))
          tree.repaint();    
        
    }
    
    public LWTreeNode findLWTreeNode(LWTreeNode treeNode, LWComponent component)
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
            
            else
            {
                childNode = findLWTreeNode(childNode, component);
                
                if(childNode != null && childNode.getUserObject().equals(component))
                {
                    foundTreeNode = childNode;
                    break;
                }
            }
        }
        
        return foundTreeNode;
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
        selectedNode.setLabel(treeNode.toString());
        treeNode.setUserObject(selectedNode);
        
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
            displayAction = new DisplayAction("Overview Tree");
        
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
