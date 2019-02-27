/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
 * A class that represents the hierarchy model used for the outline view.
 *
 * This presents a transformed view of the model presented by the LWMap containment hierarchy.
 * In the LWMap model, nodes & links are all children of the map, except nodes that can be parented
 * to other nodes, in which case they are, of course, children of their parent nodes.  Here, we
 * present a different view.  We make it look as if every link to any component is also
 * a child of that component.  Thus links, if both ends are connected, appear twice in
 * the model: once in each parent.  If only one endpoint is connected, they'll appear once,
 * and if NO endpoints of the link are connected, they won't appear in the model at all.
 * This view is similar to the LWMap hierarchy in that we present nodes that are children of other nodes
 * in the same way.
 *
 * @author  Daisuke Fujiwara
 */

public class OutlineViewHierarchyModel extends HierarchyModel implements LWComponent.Listener
{    
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(OutlineViewHierarchyModel.class);

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
        try {
            _setUpOutlineView(container, parentNode);
        } catch (osid.hierarchy.HierarchyException he) {
            System.err.println("hierarchy exception: " + he.getMessage());
            he.printStackTrace();
        } catch (osid.shared.SharedException se) {
            System.err.println("shared exception: " + se.getMessage());
            se.printStackTrace();
        }
    }
    
    private void _setUpOutlineView(LWContainer container, HierarchyNode parentNode)
        throws osid.hierarchy.HierarchyException, osid.shared.SharedException
    {
        HierarchyNode hierarchyNode;
        
        //if a node to be created is a root node
        if (parentNode == null) {
            String label, description;
            
            if ((label = container.getLabel()) == null)   
                label = new String("Container:" + container.getID());
              
            if ((description = container.getNotes()) == null)
                description = new String("No description for " + label);
              
            hierarchyNode = (HierarchyNode)createRootNode(new tufts.oki.shared.Id(getNextID()),
                                                          new tufts.oki.shared.VueType(), 
                                                          label, description);
            hierarchyNode.setLWComponent(container);
        } else {
            //if it is a non root node
            hierarchyNode = createHierarchyNode(parentNode, container);
        }
            
        //tricky with the map.. must pay attention for debugging
        for (Iterator li = container.getLinks().iterator(); li.hasNext();) {
            LWLink link = (LWLink)li.next();
            HierarchyNode linkNode = createHierarchyNode(hierarchyNode, link);
        }
            
        //do it recursively
        for (Iterator i = container.getChildIterator(); i.hasNext();) {
            LWComponent component = (LWComponent)i.next();
            
            if (component instanceof LWLink)
                ; // ignore
            else if (component instanceof LWContainer)
                setUpOutlineView((LWContainer)component, hierarchyNode);
            else
                createHierarchyNode(hierarchyNode, component);
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
        if (parentNode == null || id == null)
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
            ArrayList nodes = new ArrayList();
            HierarchyNode rootNode = getRootNode();
            
            //checks to see if the id belongs to the root node, and if it does, adds to the arraylist
            if (rootNode.getLWComponent().getID().equals(id))
                nodes.add(rootNode);
                   
            nodes.addAll(findHierarchyNodeByComponentID(rootNode, id));
            
            if (nodes == null) {
                System.out.println("OutlineViewHierarchyModel: unhandled case, nodes is null");
                return;
            }

            for (Iterator i = nodes.iterator(); i.hasNext();)
            {
                HierarchyNode hierarchyNode = (HierarchyNode)i.next();
                
                if (newLabel == null)
                {
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
        //if it is a LWLink
        if (addedChild instanceof LWLink)
        {
            LWLink link = (LWLink) addedChild;
            addLinkConnection(link.getComponent1(), link);
            addLinkConnection(link.getComponent2(), link);
            // We aren't using this code anymore: handled via the LinkAdded event.
            // However, if the model ever changes to include fully disconnected links
            // at the top level of the tree, we'll need to do something here.  (Instead
            // of calling addLinkConnection above, we need to just add a new child leaf
            // node to the given parent)
            new Throwable(this + " FYI: deprecated use of addHierarchyTreeNode for " + addedChild).printStackTrace();
        }
        //if it is a LWNode, LWImage, LWGroup, etc
        else if (addedChild instanceof LWComponent)
        {
            HierarchyNode parentNode = null, childNode;
            
            //finds the parent hierarchy node
            if (parent instanceof LWMap)
                parentNode = getRootNode();
            else
                parentNode = findHierarchyNode(getRootNode(), parent, true);
            
            if (parentNode == null) {
                if (Log.isDebugEnabled()) Log.debug("null parentNode when adding hierarchy node: " + addedChild);
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
               
            /*
            //adds anything that is contained in the added LWNode
            for (Iterator nodeIterator = ((LWContainer)addedChild).getNodeIterator(); nodeIterator.hasNext();)
                addHierarchyTreeNode((LWContainer)addedChild, (LWNode)nodeIterator.next());
            */
            
            if (addedChild.hasChildren()) {
                for (Iterator childIterator = ((LWContainer)addedChild).getChildIterator(); childIterator.hasNext();) {
                    LWComponent child = (LWComponent)childIterator.next();
                    if (child instanceof LWContainer)
                        addHierarchyTreeNode((LWContainer)addedChild, (LWContainer)child);
                }
            }
        }
    }
    
    /** Deletes a hierarchy node. */
    public void deleteHierarchyTreeNode(LWContainer parent, LWComponent deletedChild) throws osid.hierarchy.HierarchyException
    {    
        // if it is a LWLink
        if (deletedChild instanceof LWLink)
        {
            LWLink link = (LWLink)deletedChild;
            removeLinkConnection(link.getComponent1(), link);
            removeLinkConnection(link.getComponent2(), link);
            // We aren't using this code anymore: handled via the LinkRemoved event.
            // However, if the model ever changes to include fully disconnected links
            // at the top level of the tree, we'll need this -- in that case, comment out this warning.
            new Throwable(this + " FYI: unexpected use of deleteHierarchyTreeNode for " + deletedChild).printStackTrace();
        }
        //if it is anything except a link
        else if (deletedChild instanceof LWComponent)
        {
            HierarchyNode parentNode = null, deletedChildNode = null;
            
            //finds the parent hierarchy node
            if (parent instanceof LWMap)
                parentNode = getRootNode();
            else
                parentNode = findHierarchyNode(getRootNode(), parent, true);
                
            if (parentNode == null) {
                System.err.println(this + " *** NULL parentNode when deleting a hierarchy node");
                return;
            }
            
            //finds the tree node representing the deleted child
            deletedChildNode = findHierarchyNode(parentNode, deletedChild, false);  
                
            //removes from the hierarch model
            if (deletedChildNode != null)
            {
                deleteHierarchyNode(deletedChildNode);
            }
        }
        //validateHierarchyNodeLinkLabels();
    }
    
    private void removeLinkConnection(LWComponent linkEndpoint, LWLink link)
        throws osid.hierarchy.HierarchyException        
    {
        if (linkEndpoint == null)
            return;
        HierarchyNode hierParent = findHierarchyNode(getRootNode(), linkEndpoint, true);
        if (hierParent != null) {
            HierarchyNode hierLink = findHierarchyNode(hierParent, link, false);
            // in case this LWKey.LinkRemoved is result of the link being deleted,
            // it will already have been taken out of the model, so we need
            // to check for null first.
            if (hierLink != null) 
            {
                deleteHierarchyNode(hierLink);
            }
        }
    }

    private void addLinkConnection(LWComponent linkEndpoint, LWLink link)
        throws osid.hierarchy.HierarchyException        
    {
        if (linkEndpoint == null)
            return;
        HierarchyNode hierParent = findHierarchyNode(getRootNode(), linkEndpoint, true);
        if (hierParent != null) {
            HierarchyNode hierLink = createHierarchyNode(hierParent, link);
        }
    }
    
    /**A method for handling a LWC event*/
    public void LWCChanged(LWCEvent e)
    {
        Object message = e.key;
        try
        {
            if (message == LWKey.LinkAdded)
            {
                addLinkConnection((LWComponent)e.getSource(), (LWLink)e.getComponent());
            }
            else if (message == LWKey.LinkRemoved)
            {
                removeLinkConnection((LWComponent)e.getSource(), (LWLink)e.getComponent());
            }
            else if (message == LWKey.ChildrenAdded)
            {
                for (LWComponent c : e.getComponents()) {
                    if (c instanceof LWLink)
                        continue; // will be handled by a LinkAdded event
                    addHierarchyTreeNode((LWContainer)e.getSource(), c);
                }
            }
            else if (message == LWKey.ChildrenRemoved)
            {
                for (LWComponent c : e.getComponents()) {
                     if (c instanceof LWLink)
                        continue; // will be handled by a LinkRemoved event
                     deleteHierarchyTreeNode((LWContainer)e.getSource(), c);
                }
            }
            /*
            else if (message == LWKey.HierarchyChanged)
            {   
                //System.err.println(this + " needs to rebuild child list from scratch for " + e.getSource());
                LWContainer container = (LWContainer)e.getSource();  
                System.out.println("the container it needs to change is " + container.toString());
                
                ArrayList nodes = new ArrayList();
                HierarchyNode rootNode = getRootNode();
                
                if (container instanceof LWMap)
                {    
                    //rootNode.setLWComponent(container);
                    nodes.add(rootNode);
                }
                
                nodes.addAll(findHierarchyNodeByComponentID(rootNode, container.getID()));
                
                for (Iterator i = nodes.iterator(); i.hasNext();)
                { 
                    HierarchyNode hierarchyNode = (HierarchyNode)i.next();
                    
                    for (osid.hierarchy.NodeIterator ni = hierarchyNode.getChildren(); ni.hasNext();)
                    {
                      HierarchyNode node = (HierarchyNode)ni.next();
                      
                      System.out.println("deleting the node:" + node.getDisplayName());
                      deleteHierarchyNode(node); 
                    }
                    
                    for (Iterator li = container.getLinks().iterator(); li.hasNext();)
                    {
                        LWLink link = (LWLink)li.next();       
                        addHierarchyTreeNode(container, link);
                    }
                    
                    for (Iterator ci = container.getChildIterator(); ci.hasNext();)
                    {
                        LWComponent component = (LWComponent)ci.next();
                        addHierarchyTreeNode(container, component);
                    }
                }
            }
            */
        }
        
        catch(osid.hierarchy.HierarchyException he)
        {
            System.err.println(he.getMessage());
            he.printStackTrace();
        }
    }
    
    /*
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
    */
    
    /**A method that creates a hierarch node with a given parent and the given LWComponent*/
    private HierarchyNode createHierarchyNode(HierarchyNode parentNode, LWComponent component) throws osid.hierarchy.HierarchyException
    {   
        HierarchyNode node = null;
        
        try
        {  
            String label, description;
         
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
    private void deleteHierarchyNode(HierarchyNode childNode) throws osid.hierarchy.HierarchyException
    {
        try
        {  
            for (osid.hierarchy.NodeIterator ni = childNode.getChildren(); ni.hasNext();)
            {
                HierarchyNode node = (HierarchyNode)ni.next();
                deleteNode(node.getId());
            }
            
            deleteNode(childNode.getId());
        }
               
        catch(Exception e)
        {
            System.err.println(this + " deleteHierarchyNode " + e);
            e.printStackTrace();
        }
    }
    
    /**A method which searches the tree model for a hierarchy node which represents the given LWComponent*/
    public boolean contains(LWComponent component)
    {
        boolean result = false;
        
        try
        {
            if (getRootNode().getLWComponent() == component || findHierarchyNode(getRootNode(), component, true) != null)
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
    
    /**A method which returns the appropriate node label for the hierarchy node of the given LWComponent*/
    public String getNodeLabel(LWComponent component)
    {
        String label;
        
        //if there is no label associated with the given component
        if ((label = component.getLabel()) == null)
        {
            if ((label = component.getDisplayLabel()) == null)
            {
                if (component instanceof LWLink)
                  label = new String("Link-ID# " + component.getID());
              
                else if (component instanceof LWNode)   
                  label = new String("Node-ID# " + component.getID());
                
                else
                  label = "no label";
            }
        }
            
        return label;
    }
}
