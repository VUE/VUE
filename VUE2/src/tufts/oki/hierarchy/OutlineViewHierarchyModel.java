/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

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
            he.printStackTrace();
        }
        
        catch (osid.shared.SharedException se)
        {
            System.err.println("shared exception");
            se.printStackTrace();
        }
    }
    
    /**A method which finds a tree node representing the given component under the given tree node
       A boolean flag is used to determine whether to search for the node recursively in sub-levels
     */
    public HierarchyNode findHierarchyNode(HierarchyNode hierarchyNode, LWComponent component, boolean recursive)
        throws osid.hierarchy.HierarchyException
    {   
        HierarchyNode foundNode = null;
        
        if (component == null || hierarchyNode == null)
        {
            System.err.println("the component is null in findHierarchyNode method");
            return null;
        }
             
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
        
        return foundNode;
    }
    
    /**A method which finds tree nodes representing the given component ID under the given tree node
     */
    public ArrayList findHierarchyNodeByComponentID(HierarchyNode parentNode, String id) throws osid.hierarchy.HierarchyException
    {
        if (parentNode == null ||id == null)
        {
            System.err.println("null in findHierarchyNodebyID method");
            return null;
        }
        
        ArrayList nodes = new ArrayList();
       
        for (osid.hierarchy.NodeIterator i = parentNode.getChildren(); i.hasNext();)
        {
            HierarchyNode childNode = (HierarchyNode)i.next();
            
            if(childNode.getLWComponent().getID().equals(id))
            {
                nodes.add(childNode);
            }
            
            nodes.addAll(findHierarchyNodeByComponentID(childNode, id));
        }
        
        return nodes;
    }
    
    /**A method which updates the hierarchy node with the given componenet ID to the given label
     */
    public void updateHierarchyNodeLabel(String newLabel, String id)
    {
        try
        {
            ArrayList nodes = findHierarchyNodeByComponentID(getRootNode(), id);
            if (nodes == null) {
                System.out.println("OutlineViewHierarchyModel: unhandled case, nodes is null");
                return;
            }

            for (Iterator i = nodes.iterator(); i.hasNext();)
            {
                HierarchyNode hierarchyNode = (HierarchyNode)i.next();
                
                if (newLabel == null)
                {   
                    /*
                    String parentDisplayName = "";
                    
                    for (osid.hierarchy.NodeIterator pi = hierarchyNode.getParents(); pi.hasNext();)
                    {
                        HierarchyNode parentNode = (HierarchyNode)pi.next();
                        parentDisplayName = parentNode.getDisplayName();
                        
                        System.out.println("parent name is " + parentDisplayName);
                    }
                    */
                    
                    newLabel = getNodeLabel(hierarchyNode.getLWComponent());
                }
                
                hierarchyNode.updateDisplayName(newLabel);
                revalidateTree(hierarchyNode);
            }
        }
        
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println(he.getMessage());
            he.printStackTrace();
        }
        
        catch(Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
      
    /**Adds a new hierarchy node*/
    public void addHierarchyTreeNode(LWContainer parent, LWComponent addedChild) throws osid.hierarchy.HierarchyException
    {      
        //if it is a LWNode
        if (addedChild instanceof LWNode)
        {
            HierarchyNode parentNode = null, childNode;
            
            //finds the parent hierarchy node
            if (parent instanceof LWMap)
              parentNode = getRootNode();
            
            else
              parentNode = findHierarchyNode(getRootNode(), parent, true);
            
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
                linkedNode1 = findHierarchyNode(getRootNode(), component1, true);
                    
                if (linkedNode1 != null)
                { 
                    HierarchyNode linkNode = createHierarchyNode(linkedNode1, link);
                    reloadTreeModel(linkedNode1);
                }
            }
            
            if (component2 != null) 
            {   
                linkedNode2 = findHierarchyNode(getRootNode(), component2, true);
                    
                if (linkedNode2 != null) 
                {
                    HierarchyNode linkNode = createHierarchyNode(linkedNode2, link);
                    reloadTreeModel(linkedNode2);
                }  
            }
        }
    }
    
    /**Deletes a hierarchy node*/
    public void deleteHierarchyTreeNode(LWContainer parent, LWComponent deletedChild) throws osid.hierarchy.HierarchyException
    {    
        //if it is a LWNode
        if (deletedChild instanceof LWNode)
        {
            HierarchyNode parentNode = null, deletedChildNode = null;
            
            //finds the parent hierarchy node
            if (parent instanceof LWMap)
              parentNode = getRootNode();
            
            else
              parentNode = findHierarchyNode(getRootNode(), parent, true);
                
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
       
            linkedNode1 = findHierarchyNode(getRootNode(), component1, true);
            linkedNode2 = findHierarchyNode(getRootNode(), component2, true);
                
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
        
        //validateHierarchyNodeLinkLabels();
    }
    
    
    public void validateHierarchyNodeLinkLabels() throws osid.hierarchy.HierarchyException
    {
        for(osid.hierarchy.NodeIterator i = getAllNodes(); i.hasNext();)
        {
            HierarchyNode node = (HierarchyNode)i.next();
            LWComponent component = node.getLWComponent();
            
            if (component instanceof LWLink)
            {  
               System.out.println("validating: " + component.getID());
               
               LWLink link = (LWLink)component;
               
               if(node.getDisplayName().equals(link.getLabel()))
               {
                 continue;
               }
               
               if (link.getComponent1() == null || link.getComponent2() == null)    
               {
                 node.updateDisplayName("Link ID# " + link.getID() + " : to nothing");               
               }
               
               else
               {
                   System.out.println("the connected nodes are " + link.getComponent1().getLabel() + ", " + link.getComponent2().getLabel());
               }
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

        try
        {
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
        
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println(he.getMessage());
            he.printStackTrace();
        }
    }
    
    /**A method that creates a hierarch node with a given parent and the given LWComponent*/
    private HierarchyNode createHierarchyNode(HierarchyNode parentNode, LWComponent component) throws osid.hierarchy.HierarchyException
    {   
        HierarchyNode node = null;
        
        try
        {  
            String label, description;
         
            //label = getNodeLabel(component, parentNode.getDisplayName()); 
            label = getNodeLabel(component);
            
            if ((description = component.getNotes()) == null)
              description = new String("No description for " + label);
              
            //creates a hierarchy node and sets its LWcomponent to the given one
            node = (HierarchyNode)createNode(new tufts.oki.shared.Id(getNextID()), parentNode.getId(), new tufts.oki.shared.VueType(), 
                                             label, description);
            node.setLWComponent(component);
        }
      
        catch (osid.shared.SharedException se)
        {
            System.err.println("exception creating a node from shared");
        }
        
        catch (Exception e)
        {
           //possible null pointer 
            System.err.println(this + " createHierarchyNode " + e);
            e.printStackTrace();
        }
        
        return node;
    }
    
    /**A method that deletes the given node*/
    private void deleteHierarchyNode(HierarchyNode parentNode, HierarchyNode childNode) throws osid.hierarchy.HierarchyException
    {
        try
        {  
            deleteNode(childNode.getId());
            reloadTreeModel(parentNode);
        }
               
        catch(Exception e)
        {
            System.err.println(this + " deleteHierarchyNode " + e);
            e.printStackTrace();
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
            //System.err.println("Exception in the tree path method: " + e.getMessage());
            path = null;
        }
        
        return path;
    }
    
    //public String getNodeLabel(LWComponent component, String parentNodeDisplayName)
    public String getNodeLabel(LWComponent component)
    {
        String label;
        
        //if there is no label associated with the given component
        if ((label = component.getLabel()) == null)
        {
            if (component instanceof LWLink)
            {
                LWLink link = (LWLink)component;
                /*
                String connectedNodeLabel = "nothing";
                  
                //gets the name of the component that the link connects to
                if (link.getComponent1() != null && link.getComponent1().getLabel() != null)
                  connectedNodeLabel = link.getComponent1().getLabel();
                     
                //if (connectedNodeLabel.equals(parentNode.getDisplayName()))
                if (connectedNodeLabel.equals(parentNodeDisplayName))
                {
                    if (link.getComponent2() !=null && link.getComponent2().getLabel() != null)
                      connectedNodeLabel = link.getComponent2().getLabel();
                   
                    else
                      connectedNodeLabel = "nothing"; 
                }
                  
                label = new String("Link ID# " + link.getID() + ": to " + connectedNodeLabel);
                */
                
                label = new String("Link ID# " + link.getID());
            }
                
            else if (component instanceof LWNode)   
              label = new String("Node ID# " + component.getID());
        }
        
        return label;
    }
}
