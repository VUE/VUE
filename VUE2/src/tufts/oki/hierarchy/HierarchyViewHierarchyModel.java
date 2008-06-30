/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
 * HierarchyViewHierarchyModel.java
 *
 * Created on December 20, 2003, 11:44 PM
 */

package tufts.oki.hierarchy;

import java.util.*;

import tufts.vue.LWComponent;
import tufts.vue.LWContainer;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWHierarchyMap;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class HierarchyViewHierarchyModel extends HierarchyModel {
    
    //hash map which stores information of the hierarchy map data
    //node hashtable serves the purpose of mapping duplicates to the original nodes
    private HashMap hierarchyHash;
    private Hashtable nodeHash;
    
    //an arraylist which holds all the nodes that were connected from the root node
    private ArrayList originalNodes;
    
    private LWHierarchyMap hierarchyMap = null;
    //problems:
    //fix layout (where to call)
    //duplicate problem
    
    /** Creates a new instance of HierarchyViewHierarchyModel */
    public HierarchyViewHierarchyModel(LWNode node, LWHierarchyMap hierarchyMap) 
    {
        super();
        
        hierarchyHash = new HashMap();
        nodeHash = new Hashtable();
        originalNodes = new ArrayList();
        this.hierarchyMap = hierarchyMap;
        
        computeShortestPath(node);
        createLinks();
        layout(getDuplicatedNode(node), null);
        setUpHierarchyNodes(getDuplicatedNode(node), null);
    }
    
    public HierarchyViewHierarchyModel(LWNode node, LWHierarchyMap hierarchyMap, String name, String description) 
    {
        super(name, description);
        
        hierarchyHash = new HashMap();
        nodeHash = new Hashtable();
        originalNodes = new ArrayList();
        this.hierarchyMap = hierarchyMap;
        
        computeShortestPath(node);
        System.out.println("beginning of links");
        createLinks();
        layout(getDuplicatedNode(node), null);
        System.out.println("end of links");
        setUpHierarchyNodes(getDuplicatedNode(node), null);
    }
    
    /*** create the duplicates of the nodes and map them to the original nodes
         using the node hashtable
     */
    public LWNode duplicateNode(LWNode node, LWContainer parent)
    {
        LWNode copy = (LWNode)node.duplicate();
        //copy.setParent(parent);
        
        /*
        for (Iterator i = node.getNodeIterator(); i.hasNext();)
        {
            LWNode childCopy = duplicateNode((LWNode)i.next(), copy);
            copy.addChild(childCopy);
        }
        */
        
        nodeHash.put(node, copy);
        
        return copy;
    }
     
    public LWNode getDuplicatedNode(LWNode node)
    {
        return (LWNode)nodeHash.get(node);
    }
    
     /**Dijkstra's theorem is used here to compute the shortest path between nodes*/
    public void computeShortestPath(LWNode rootNode)
    {
        //a vector used for the Dijkstra's theorem
        Vector nodesVector = new Vector();
         
        //initial set up for the computation for the shortest path
        nodesVector.add(rootNode); 
        originalNodes.add(rootNode);
        duplicateNode(rootNode, hierarchyMap);
       
        //stores default values to the hierarchy hashmap
        hierarchyHash.put((LWNode)nodeHash.get(rootNode), new ShortestPathData(null, 0));
        
        //Dijkstra's theorem (shortest path)
        while(!nodesVector.isEmpty())
        {   
            //removes the first element in the vector as it is a queue
            LWNode currentNode = (LWNode)nodesVector.remove(0);
            LWNode currentNodeCopy = (LWNode)nodeHash.get(currentNode);
            
            //retrieves the current shortest distance to get to the given node from the root node
            int totalDistance = ((ShortestPathData)hierarchyHash.get(currentNodeCopy)).getDistance();

            //iterates through nodes that are connected to the given node
            for (Iterator i = currentNode.getLinks().iterator(); i.hasNext();)
            {   
                LWLink connectedLink = (LWLink)i.next();
                LWNode nextNode = null;
                
                //calculates the distance to adjacent nodes from the root node passing through the given node
                //could lose the precision..
                // todo: how to handle curved links?
                int length = (int)connectedLink.getPoint1().distance(connectedLink.getPoint2());
                totalDistance += length;
             
                //gets the component associated with the given link
                if ((nextNode = (LWNode)connectedLink.getComponent1()) == currentNode)
                    nextNode = (LWNode)connectedLink.getComponent2();
                
                //keep track of nodes that are connected 
                if(!originalNodes.contains(nextNode))
                {
                  originalNodes.add(nextNode);
                  duplicateNode(nextNode, hierarchyMap);
                }
                
                LWNode nextNodeCopy = (LWNode)nodeHash.get(nextNode);
                
                //if it is the first time traversing through this node or if the calculated distance is shorter
                //than the shortest distance associated with the adjacent node
                if (!hierarchyHash.containsKey(nextNodeCopy) || 
                   totalDistance < ((ShortestPathData)hierarchyHash.get(nextNodeCopy)).getDistance())
                {  
                    //updates the distance and parent hashtables and adds to the vector
                    nodesVector.add(nextNode);
                    hierarchyHash.put(nextNodeCopy, new ShortestPathData(currentNodeCopy, totalDistance));
                }
            }
        }
        
        //clears the arraylist contents
        originalNodes.clear();
        
        /*
        for (Iterator ii = hierarchyHash.keySet().iterator(); ii.hasNext();)
        {
          LWNode a = (LWNode)ii.next();
          System.out.println("key " + a.toString());
          
          if(((HierarchyData)hierarchyHash.get(a)).getParent() != null)
            System.out.println("value " + ((LWNode)((HierarchyData)hierarchyHash.get(a)).getParent()).toString() + "\n\n");
          
          else
            System.out.println("value null" + "\n\n");
        }
         **/
    }
    
    /**creates the links for the hierarchy map according to the shortest path*/
    public void createLinks()
    {
        //verify a path between nodes and create a link between the nodes
        for (Iterator i = nodeHash.values().iterator(); i.hasNext();)
        {
            LWNode child = (LWNode)i.next();
            LWNode parent = ((ShortestPathData)hierarchyHash.get(child)).getParent();
            
            //if the node has a parent, then create a link
            if (parent != null)
            { 
              LWLink link = new LWLink(parent, child);
              
              //how about the link label?
            }
        }
    }
    
     /**organizes the nodes in a hierarchy in a recursive fashion*/
    public void layout(LWNode currentNode, LWNode previousNode)
    {   
        //System.out.println("laying out: " + currentNode.toString()) ;
        LWNode parentNode = ((ShortestPathData)hierarchyHash.get(currentNode)).getParent();
        
        //x and y values which specify the new location of the current node
        //they are initialized to the root node position (default)
        float x = 200f, y = 0;
        
        if (previousNode != null)
        {
            x = previousNode.getX() + previousNode.getLocalBorderWidth() + 2;
            y = previousNode.getY();
        }
        
        //if there is no previous node and the parent node exists 
        else if(parentNode != null)
        {    
          //determines the farthest left node's x location
          int xRange = 200;
          
          x = parentNode.getX() - (xRange / 2);
          y = parentNode.getY() + 60;
        }
            
        currentNode.setLocation(x, y);
        
        //children from here
        LWNode node = null;
        
        //must come up with another algorithm if left to right has some meaning
        for (Iterator i = currentNode.getLinks().iterator(); i.hasNext();)
        {
            //links to nodes
            LWLink link = (LWLink)i.next();
            LWNode nextNode = null;
            
            if ((nextNode = (LWNode)link.getComponent1()) == currentNode)
              nextNode = (LWNode)link.getComponent2();
        
            if(!nextNode.equals(parentNode))
            {
              layout(nextNode, node);
              node = nextNode;
            }
        }
        
        //return currentNode;
    }
    
    //only adds LWNode
    public void setUpHierarchyNodes(LWNode node, HierarchyNode parentNode)
    {
        try
        {
            HierarchyNode hierarchyNode;
         
            originalNodes.add(node);
            
            //if a node to be created is a root node
            if (parentNode == null)
            {
              String label, description;
            
              if ((label = node.getLabel()) == null)   
                label = new String("Node:" + node.getID());
              
              if ((description = node.getNotes()) == null)
                description = new String("No description for " + label);
              
              hierarchyNode = (HierarchyNode)createRootNode(new tufts.oki.shared.Id(getNextID()), new tufts.oki.shared.VueType(), 
                                             label, description);
              hierarchyNode.setLWComponent(node);
            }
            
            //if it is a non root node
            else
              hierarchyNode = createHierarchyNode(parentNode, node);
            
            //do it recursively
            for (Iterator i = node.getLinks().iterator(); i.hasNext();)
            {
                LWLink link = (LWLink)i.next();
                LWComponent nextNode = null;
                
                if (link.getComponent1() != node)
                  nextNode = link.getComponent1();
                else if (link.getComponent2() != node) 
                  nextNode = link.getComponent2();
                
                if(nextNode != null && nextNode instanceof LWNode && !originalNodes.contains(nextNode))
                  setUpHierarchyNodes((LWNode)nextNode, hierarchyNode);
            }
        }
        
        catch (osid.hierarchy.HierarchyException he)
        {
            System.out.println("hierarchy exception");
        }
        
        catch (osid.shared.SharedException se)
        {
            System.out.println("shared exception");
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
                  label = new String("Link:" + component.getID());
                
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
            e.printStackTrace();
        }
        
        return node;
    }
    
    /**A method that deletes the given node*/
    private void deleteHierarchyNode(HierarchyNode parentNode, HierarchyNode childNode)
    {
        try
        {  
            deleteNode(childNode.getId());
            //reloadTreeModel(parentNode);
        }
              
        catch(osid.hierarchy.HierarchyException he)
        {
            System.out.println("deleting node bug");  
        }
        
        catch(Exception e)
        {
            System.err.println(this + " deleteHierarchyNode " + e);
            e.printStackTrace();
        }
    }
    
    /**A class which stores the information of the hierarchy*/
    private class ShortestPathData
    {
        private LWNode parentNode;
        private int distance;
        
        public ShortestPathData(LWNode parentNode, int distance)
        {
          this.parentNode = parentNode;
          this.distance = distance;
        }
        
        //gets the distance associated with the given node
        public int getDistance()
        {
          return distance;
        }
        
        //gets the parent node associated with the given node
        public LWNode getParent()
        {
          return parentNode;
        }
    }
}
