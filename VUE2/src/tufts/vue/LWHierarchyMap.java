/*
 * HierarchyMap.java
 *
 * Created on August 1, 2003, 2:51 PM
 */

package tufts.vue;

/**
 *
 * @author Daisuke Fujiwara
 */
import java.util.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**a class which creates a hierarchy view of a map from a given node*/
public class LWHierarchyMap extends LWMap
{
    //a root node where the hierarchy should start from
    private LWNode rootNode;
    
    //hash map which stores information of the hierarchy map data
    //node hashtable serves the purpose of mapping duplicates to the original nodes
    private HashMap hierarchyHash;
    private Hashtable nodeHash;
    
    /** Creates a new instance of HierarchyMap */
    public LWHierarchyMap() 
    {
        super("test");
        
        rootNode = null;
        hierarchyHash = new HashMap();
        nodeHash = new Hashtable();
    }
    
    public LWHierarchyMap(LWNode node)
    {
        this();
        rootNode = node;
    }
    
    public void setRootNode(LWNode node)
    {
        rootNode = node;
    }
    
    public LWNode getRootNode()
    {
        return rootNode;
    }
    
    /*** create the duplicates of the nodes and map them to the original nodes
         using the node hashtable
     */
    public void duplicateNode(LWNode node)
    {
        LWNode copy = (LWNode)node.duplicate();
        addNode(copy);
        nodeHash.put(node, copy);
    }
    
    /**Dijkstra's theorem is used here to compute the shortest path between nodes*/
    public void computeShortestPath()
    {
        //a vector used for the Dijkstra's theorem
        Vector nodesVector = new Vector();
        
        //an arraylist which holds all the nodes that were connected from the root node
        ArrayList originalNodes = new ArrayList();
         
        //initial set up for the computation for the shortest path
        nodesVector.add(rootNode); 
        originalNodes.add(rootNode);
        duplicateNode(rootNode);
        
        //stores default values to the hierarchy hashmap
        hierarchyHash.put((LWNode)nodeHash.get(rootNode), new HierarchyData(null, 0));
        
        //Dijkstra's theorem (shortest path)
        while(!nodesVector.isEmpty())
        {   
            //removes the first element in the vector as it is a queue
            LWNode currentNode = (LWNode)nodesVector.remove(0);
            LWNode currentNodeCopy = (LWNode)nodeHash.get(currentNode);
            
            //retrieves the current shortest distance to get to the given node from the root node
            int totalDistance = ((HierarchyData)hierarchyHash.get(currentNodeCopy)).getDistance();

            //iterates through nodes that are connected to the given node
            for (Iterator i = currentNode.getLinks().iterator(); i.hasNext();)
            {   
                LWLink connectedLink = (LWLink)i.next();
                LWNode nextNode = null;
                
                //calculates the distance to adjacent nodes from the root node passing through the given node
                //could lose the precision..
                int length = (int)connectedLink.getLine().getP1().distance(connectedLink.getLine().getP2());
                totalDistance += length;
             
                //gets the component associated with the given link
                if ((nextNode = (LWNode)connectedLink.getComponent1()) == currentNode)
                    nextNode = (LWNode)connectedLink.getComponent2();
                
                //keep track of nodes that are connected 
                if(!originalNodes.contains(nextNode))
                {
                  originalNodes.add(nextNode);
                  duplicateNode(nextNode);
                }
                
                LWNode nextNodeCopy = (LWNode)nodeHash.get(nextNode);
                
                //if it is the first time traversing through this node or if the calculated distance is shorter
                //than the shortest distance associated with the adjacent node
                if (!hierarchyHash.containsKey(nextNodeCopy) || 
                   totalDistance < ((HierarchyData)hierarchyHash.get(nextNodeCopy)).getDistance())
                {  
                    //updates the distance and parent hashtables and adds to the vector
                    nodesVector.add(nextNode);
                    hierarchyHash.put(nextNodeCopy, new HierarchyData(currentNodeCopy, totalDistance));
                }
            }
        }
          
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
        for (Iterator i = getNodeIterator(); i.hasNext();)
        {
            LWNode child = (LWNode)i.next();
            LWNode parent = ((HierarchyData)hierarchyHash.get(child)).getParent();
            
            //if the node has a parent
            if (parent != null)
            { 
              LWLink link = new LWLink(parent, child);
              addLink(link);
              
              //how about the link label?
            }
        }
    }
    
    /**organizes the nodes in a hierarchy in a recursive fashion*/
    public void layout(LWNode currentNode, LWNode previousNode)
    {   
        //System.out.println("laying out: " + currentNode.toString()) ;
        LWNode parentNode = ((HierarchyData)hierarchyHash.get(currentNode)).getParent();
        
        //x and y values which specify the new location of the current node
        //they are initialized to the root node position (default)
        float x = 200f, y = 0;
        
        if (previousNode != null)
        {
            x = previousNode.getX() + previousNode.getBoundsWidth() + 2;
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
    
    /**biggest question is whether this should inherit LWMap or return LWMap as a product of a method*/
    public void createHierarchy()
    {    
        computeShortestPath();
        createLinks();
        
        LWNode newRootNode = (LWNode)nodeHash.get(rootNode);
        
        layout(newRootNode, null);
        VUE.getHierarchyTree().setTree((new HierarchyTreeModel(newRootNode)).getModel());
    }
    
    private class HierarchyData
    {
        private LWNode parentNode;
        private int distance;
        
        public HierarchyData(LWNode parentNode, int distance)
        {
          this.parentNode = parentNode;
          this.distance = distance;
        }
        
        public int getDistance()
        {
          return distance;
        }
        
        public LWNode getParent()
        {
          return parentNode;
        }
    }
}
