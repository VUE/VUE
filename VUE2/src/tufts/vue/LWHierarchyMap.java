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

public class LWHierarchyMap extends LWMap
{
    //a root node where the hierarchy should start from
    private LWNode rootNode;
    
    //hash tables which stores information of the hierarchy map
    //distance hashtable holds the shortest distance to reach a node from the root node
    //parent hashtable holds nodes' parent information
    //node hashtable serves the purpose of mapping duplicates to the original nodes
    private HashMap distanceHash, parentHash;
    private Hashtable nodeHash;
    
    //an arraylist which holds all the nodes that were connected from the root node
    private ArrayList originalNodes;
    
    /** Creates a new instance of HierarchyMap */
    public LWHierarchyMap() 
    {
        super("test");
        
        rootNode = null;
        originalNodes = new ArrayList();
        distanceHash = new HashMap();
        parentHash = new HashMap();
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
    
    /**Dijkstra's theorem is used here to compute the shortest path between nodes*/
    public void computeShortestPath()
    {
        //a vector used for the Dijkstra's theorem
        Vector nodesVector = new Vector();
        
        //initial set up for the computation for the shortest path
        nodesVector.add(rootNode); 
        originalNodes.add(rootNode);
        LWNode copy = (LWNode)rootNode.duplicate();
        addNode(copy);
            
        //maps the orginal node to the duplicated one using the nodes' toString method
        nodeHash.put(rootNode, copy);
        
        //stores default values to the distance and parent hashtables
        distanceHash.put(rootNode, new Integer(0));
        parentHash.put(rootNode, null);
        
        //Dijkstra's theorem (shortest path)
        while(!nodesVector.isEmpty())
        {   
            //removes the first element in the vector as it is a queue
            LWNode currentNode = (LWNode)nodesVector.remove(0);
            
            //retrieves the current shortest distance to get to the given node from the root node
            int totalDistance = ((Integer)distanceHash.get(currentNode)).intValue();

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
                
                //if it is the first time traversing through this node or if the calculated distance is shorter
                //than the shortest distance associated with the adjacent node
                if (!distanceHash.containsKey(nextNode) || 
                   totalDistance < ((Integer)distanceHash.get(nextNode)).intValue())
                {  
                    //updates the distance and parent hashtables and adds to the vector
                    nodesVector.add(nextNode);
                    distanceHash.put(nextNode, new Integer(totalDistance));
                    parentHash.put(nextNode, currentNode);                    
                }
                
                //keep track of nodes that are connected 
                if(!originalNodes.contains(nextNode))
                {
                  originalNodes.add(nextNode);
                    
                  //create the duplicates of the nodes and map them to the original nodes
                  //using the node hashtable
                  copy = (LWNode)nextNode.duplicate();
                  addNode(copy);
            
                  //maps the orginal node to the duplicated one using the nodes' toString method
                  nodeHash.put(nextNode, copy);
                }
            }
        }
          
        //debugging
        for (Iterator ii = parentHash.keySet().iterator(); ii.hasNext();)
        {
          LWNode a = (LWNode)ii.next();
          System.out.println("key " + a.toString());
          
          if(parentHash.get(a) != null)
            System.out.println("value " + ((LWNode)parentHash.get(a)).toString() + "\n\n");
          
          else
            System.out.println("value null" + "\n\n");
        }
    }
    
    /**creates the elements for the hierarchy map including duplicated nodes
       and links between them according to the shortest path*/
    public void createMap()
    {
        //verify a path between nodes and create a link between the nodes
        for (Iterator i = originalNodes.iterator(); i.hasNext();)
        {
            LWNode child = (LWNode)i.next();
            LWNode parent = (LWNode)parentHash.get(child);
            
            //if the node has a parent
            if (parent != null)
            {
              //creates the link using the mapped duplicates of the original nodes
              LWNode childCopy = (LWNode)nodeHash.get(child);
              LWNode parentCopy = (LWNode)nodeHash.get(parent);
              
              LWLink link = new LWLink(parentCopy, childCopy);
              addLink(link);
            }
        }
    }
    
    /**organizes the nodes in a hierarchical manner*/
    public void layout(LWNode currentNode, int number, int total)
    {   
        System.out.println("calling layout() on " + currentNode.toString());
        originalNodes.add(currentNode);
        
        LWNode copyNode = (LWNode)nodeHash.get(currentNode);
        LWNode parentNode = (LWNode)parentHash.get(currentNode);
            
        if(parentNode != null)
          {
            //set the location
            Point2D point = parentNode.getLocation();
            float x = (float)point.getX();
            float y = (float)point.getY();
            
            //location for the node
            int xIncrement = 50 / total;
            int xOffSet = number * xIncrement;
            
            x = x - 25 + xOffSet;
            y += 60;
            
            copyNode.setLocation(x, y);
          }
            
        //if it is the rootnode
        else
          {
            copyNode.setLocation(200f, 0f);
          }
            
        int nextNumber = 0;
        int nextTotal = currentNode.getLinks().size() - 1; //taking out the parent node
        
        for (Iterator i = currentNode.getLinks().iterator(); i.hasNext();)
        {
            //links to nodes
            LWLink link = (LWLink)i.next();
            LWNode nextNode = null;
            
            if ((nextNode = (LWNode)link.getComponent1()) == currentNode)
              nextNode = (LWNode)link.getComponent2();
            
            if(!originalNodes.contains(nextNode))
            {
              layout(nextNode, nextNumber, nextTotal);
              nextNumber++;
            }
        }
    }
    
    /**biggest question is whether this should inherit LWMap or return LWMap as a product of a method*/
    public void createHierarchy()
    {    
        computeShortestPath();
        createMap();
        
        originalNodes.clear();
        layout(rootNode, 0, 0);
    }
}
