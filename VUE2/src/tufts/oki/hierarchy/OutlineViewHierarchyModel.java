/*
 * OutlineViewHierarchyModel.java
 *
 * Created on December 20, 2003, 11:41 PM
 */

package tufts.oki.hierarchy;

import java.util.Iterator;
import java.util.ArrayList;
import javax.swing.tree.TreePath;

import tufts.vue.LWComponent;
import tufts.vue.LWContainer;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWCEvent;
import tufts.vue.LWKey;

/**
 *
 * @author  Daisuke Fujiwara
 */

/**A class that represents the hierarchy model used for the outline view*/
public class OutlineViewHierarchyModel extends HierarchyModel implements LWComponent.Listener 
{
    
    /** Creates a new instance of OutlineViewHierarchyModel */
    public OutlineViewHierarchyModel(LWContainer container) 
    {
        super();
        setUpOutlineView(container, null);
    }
    
    public OutlineViewHierarchyModel(LWContainer container, String name, String description) 
    {
        super(name, description);
        setUpOutlineView(container, null);
    }
    
    /**A method that sets up the hierarchy structure of the outline view*/
    public void setUpOutlineView(LWContainer container, HierarchyNode parentNode)
    {
        try
        {
            HierarchyNode hierarchyNode;
        
            //if a node to be created is a root node
            if (parentNode == null)
            {
              String label, description;
            
              if ((label = container.getLabel()) == null)   
                label = new String("Container:" + container.getID());
              
              if ((description = container.getNotes()) == null)
                description = new String("No description for " + label);
              
              hierarchyNode = (HierarchyNode)createRootNode(new tufts.oki.shared.Id(getNextID()), new tufts.oki.shared.VueType(), 
                                             label, description);
              hierarchyNode.setLWComponent(container);
            }
            
            //if it is a non root node
            else
              hierarchyNode = createHierarchyNode(parentNode, container);
            
            //tricky with the map.. must pay attention for debugging
            for (Iterator li = container.getLinks().iterator(); li.hasNext();)
            {
                LWLink link = (LWLink)li.next();
                HierarchyNode linkNode = createHierarchyNode(hierarchyNode, link);
            }
            
            //do it recursively
            //just get the nodelist?
            for(Iterator i = container.getChildList().iterator(); i.hasNext();)
            {
                LWComponent component = (LWComponent)i.next();
            
                if (component instanceof LWNode)
                  setUpOutlineView((LWNode)component, hierarchyNode);
            } 
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.err.println("hierarchy exception");
            System.err.println(he.getMessage());
        }
        
        catch (osid.shared.SharedException se)
        {
            System.err.println("shared exception");
        }
    }
    
    /**A method which finds a tree node representing the given component under the given tree node
       A boolean flag is used to determine whether to search for the node recursively in sub-levels
     */
    public HierarchyNode findHierarchyNode(HierarchyNode hierarchyNode, LWComponent component, boolean recursive)
    {   
        HierarchyNode foundNode = null;
        
        if (component == null)
        {
            System.err.println("the component is null in findHierarchyNode method");
            return null;
        }
        
        try
        {
            for (osid.hierarchy.NodeIterator i = hierarchyNode.getChildren(); i.hasNext();)
            {
                HierarchyNode childNode = (HierarchyNode)i.next();
            
                if(childNode.getLWComponent() == component)
                {
                    foundNode = childNode;
                    break;
                }
            
                else if (recursive)
                {
                    childNode = findHierarchyNode(childNode, component, true);
                
                    //redundant?
                    if(childNode != null && childNode.getLWComponent() == component)
                    {
                        foundNode = childNode;
                        break;
                    }
                }
            }
        }
        
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println("hierarchy exception");
        }
        
        return foundNode;
    }
    
    /**Adds a new hierarchy node*/
    public void addHierarchyTreeNode(LWContainer parent, LWComponent addedChild)
    {      
        //if it is a LWNode
        if (addedChild instanceof LWNode)
        {
            HierarchyNode parentNode = null, childNode;
            
            //finds the parent hierarchy node
            try
            {
                if (parent instanceof LWMap)
                  parentNode = getRootNode();
            
                else
                  parentNode = findHierarchyNode(getRootNode(), parent, true);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {
                System.err.println("couldn't find root");
            }
            
            if (parentNode == null) {
                System.err.println("*** NULL parentNode when adding a hierarchy node in OutlineViewHierarchyModel");
                // don't know what right thing to do here, but this exception
                // was driving me crazy -- SMF 2003-11-13 18:19.04
                return;
            }

            //creates the hierarchy node as a child of the parent node    
            childNode = createHierarchyNode(parentNode, addedChild);
            
            //for each link associated with the added LWNode, add to the parent hierarchy node
            for (Iterator i = addedChild.getLinks().iterator(); i.hasNext();)
            {
                LWLink link = (LWLink)i.next();       
                HierarchyNode linkNode = createHierarchyNode(childNode, link);
            }
               
            //adds anything that is contained in the added LWNode
            for (Iterator nodeIterator = ((LWNode)addedChild).getNodeIterator(); nodeIterator.hasNext();)
            {
                
                addHierarchyTreeNode((LWNode)addedChild, (LWNode)nodeIterator.next());
                
                /*
                LWNode subChild = (LWNode)nodeIterator.next();
                HierarchyNode subChildNode = createHierarchyNode(childNode, subChild);
                
                for (Iterator linkIterator = subChild.getLinks().iterator(); linkIterator.hasNext();)
                {
                    LWLink link = (LWLink)linkIterator.next();
                    HierarchyNode linkNode = createHierarchyNode(subChildNode, link);
                }
                 */
               
            }
            
            //updates the tree
            reloadTreeModel(parentNode);
            //scrollPathToVisible(new TreePath(parentTreeNode.getPath()));
        }
             
        //if it is a LWLink
        else if (addedChild instanceof LWLink)
        {
            HierarchyNode linkedNode1 = null;
            HierarchyNode linkedNode2 = null;
            LWLink link = (LWLink)addedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            //finds the hierarchy nodes associated with the two components and adds a hierarchy node representing the link to
            //the two hierarchy nodes
            if (component1 != null) 
            {
                try
                {
                    linkedNode1 = findHierarchyNode(getRootNode(), component1, true);
                    
                    if (linkedNode1 != null)
                    { 
                        HierarchyNode linkNode = createHierarchyNode(linkedNode1, link);
                        reloadTreeModel(linkedNode1);
                    }
                }
                
                catch (osid.hierarchy.HierarchyException he1)
                {
                    System.err.println("couldn't find the root");
                }
            }
            
            if (component2 != null) 
            {
                try
                {
                    linkedNode2 = findHierarchyNode(getRootNode(), component2, true);
                    
                    if (linkedNode2 != null) 
                    {
                        HierarchyNode linkNode = createHierarchyNode(linkedNode2, link);
                        reloadTreeModel(linkedNode2);
                    }
                }
                
                catch (osid.hierarchy.HierarchyException he2)
                {
                    System.err.println("couldn't find the root");
                }
            }
        }
    }
    
    /**Deletes a hierarchy node*/
    public void deleteHierarchyTreeNode(LWContainer parent, LWComponent deletedChild)
    {    
        //if it is a LWNode
        if (deletedChild instanceof LWNode)
        {
            HierarchyNode parentNode = null, deletedChildNode = null;
            
            //finds the parent hierarchy node
            try
            {
                if (parent instanceof LWMap)
                  parentNode = getRootNode();
            
                else
                  parentNode = findHierarchyNode(getRootNode(), parent, true);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {
                System.err.println("couldn't get the root");
            }
             
            if (parentNode == null) 
            {
                System.err.println("*** NULL parentNode when deleting a hierarchy node in OutlineViewHierarchyModel");
                // don't know what right thing to do here, but this exception
                // was driving me crazy -- SMF 2003-11-13 18:19.04
                return;
            }
            
            //finds the tree node representing the deleted child
            deletedChildNode = findHierarchyNode(parentNode, deletedChild, false);
             
            //removes from the hierarch model
            deleteHierarchyNode(parentNode, deletedChildNode);
        }
         
        //if it is a LWLink
        else if (deletedChild instanceof LWLink)
        {
            HierarchyNode linkedNode1 = null, linkedNode2 = null;
            LWLink link = (LWLink)deletedChild;
            
            //gets two components the link connects to
            LWComponent component1 = link.getComponent1();
            LWComponent component2 = link.getComponent2();
         
            try
            {
                linkedNode1 = findHierarchyNode(getRootNode(), component1, true);
                linkedNode2 = findHierarchyNode(getRootNode(), component2, true);
            }
            
            catch (osid.hierarchy.HierarchyException he)
            {
                System.err.println("couldn't find the root");
            }
            
            //finds the hierarchy nodes associated with the two components and deletes the tree node representing the link to
            //the two tree nodes
            //must check to see if the parent wasn't deleted in the process
            if (linkedNode1 != null)
            {
                HierarchyNode linkNode1 = findHierarchyNode(linkedNode1, link, false);
                deleteHierarchyNode(linkedNode1, linkNode1);
            }
              
            if (linkedNode2 != null)
            {
                HierarchyNode linkNode2 = findHierarchyNode(linkedNode2, link, false); 
                deleteHierarchyNode(linkedNode2, linkNode2);
            }
        }
    }
    
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        String message = e.getWhat();
        
        //old events
        //if (message == LWKey.ChildAdded)
        //  addHierarchyTreeNode((LWContainer)e.getSource(), e.getComponent());
        //else if (message == LWKey.ChildRemoved)
        //deleteHierarchyTreeNode((LWContainer)e.getSource(), e.getComponent());

        // TODO: also needs to generally handle HierachyChanging events, which
        // is all we get on undo's

        if (message == LWKey.ChildrenAdded)
        {
            ArrayList childrenList = e.getComponents();
            for (Iterator i = childrenList.iterator(); i.hasNext();)
                addHierarchyTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        else if (message == LWKey.ChildrenRemoved)
        {
            ArrayList childrenList = e.getComponents();
            for (Iterator i = childrenList.iterator(); i.hasNext();)
                deleteHierarchyTreeNode((LWContainer)e.getSource(), (LWComponent)i.next());
        }
        else if (message == LWKey.HierarchyChanged)
        {
            System.err.println(this + " needs to rebuild child list from scratch for " + e.getSource());
        }
    }
    
    /**A method that creates a hierarch node with a given parent and the given LWComponent*/
    private HierarchyNode createHierarchyNode(HierarchyNode parentNode, LWComponent component) 
    {   
        HierarchyNode node = null;
        
        try
        {
            String label, description;
            
            //if there is no label associated with the given component
            if ((label = component.getLabel()) == null)
            {
                if (component instanceof LWLink)
                {
                  LWLink link = (LWLink)component;
                  String connectedNodeLabel;
                  
                  //gets the name of the component that the link connects to
                  if ((connectedNodeLabel = link.getComponent1().getLabel()).equals(parentNode.getDisplayName()))
                    connectedNodeLabel = link.getComponent2().getLabel();
                  
                  label = new String("Link: to " + connectedNodeLabel);
                }
                
                else if (component instanceof LWNode)   
                  label = new String("Node:" + component.getID());
            }
            
            if ((description = component.getNotes()) == null)
              description = new String("No description for " + label);
              
            //creates a hierarchy node and sets its LWcomponent to the given one
            node = (HierarchyNode)createNode(new tufts.oki.shared.Id(getNextID()), parentNode.getId(), new tufts.oki.shared.VueType(), 
                                             label, description);
            node.setLWComponent(component);
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.err.println("exception creating a node");
        }
        
        catch (osid.shared.SharedException se)
        {
            System.err.println("exception creating a node from shared");
        }
        
        catch (Exception e)
        {
           //possible null pointer 
            System.err.println(this + " createHierarchyNode " + e);
            //e.printStackTrace();
        }
        
        return node;
    }
    
    /**A method that deletes the given node*/
    private void deleteHierarchyNode(HierarchyNode parentNode, HierarchyNode childNode)
    {
        try
        {  
            deleteNode(childNode.getId());
            reloadTreeModel(parentNode);
        }
              
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println("deleting node bug");  
        }
        
        catch(Exception e)
        {
            System.err.println(this + " deleteHierarchyNode " + e);
            //e.printStackTrace();
        }
    }
    
    public boolean contains(LWComponent component)
    {
        boolean result = false;
        
        try
        {
            if (findHierarchyNode(getRootNode(), component, true) != null)
              result = true;
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.err.println("contains method didn't work");
        }
        
        return result;
    }
    
    public TreePath getTreePath(LWComponent component)
    {
        TreePath path = null;
        
        try
        {
            HierarchyNode node = findHierarchyNode(getRootNode(), component, true);
            path = new TreePath(node.getTreeNode().getPath());
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.err.println("Hierarchy exception in the tree path method");
        }
        
        catch (Exception e)
        {
            System.err.println("Exception in the tree path method: " + e.getMessage());
            path = null;
        }
        
        return path;
    }
    
    /**A method which gets called when a component on a map changed its label so the update is reflected
       to the hierarchy node associated with the component*/
    public void updateNodeDisplayName(LWComponent component)
    {   
        System.out.println("the component to update is " + component);
        
        try
        {
            if (component == null)
              return;
            
            //find the hierarchy node
            HierarchyNode node = findHierarchyNode(getRootNode(), component, true);
        
            //switch to the new name
            if (node != null)
            {
              node.updateDisplayName(component.getLabel());
              System.out.println("update completed");
            }
            
            else
              System.err.println("node was not found in updateNodeDisplayName");
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.err.println("Hierarchy exception doing the updateNodeDisplayName method");
        }
        
        catch (Exception e)
        {
            System.err.println("General exception doing the updateNodeDisplayName method");
            e.printStackTrace();
        }
    }
}
