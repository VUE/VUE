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
    private Hashtable distanceHash, parentHash, nodeHash;
    
    //an arraylist which holds all the nodes that were connected from the root node
    private ArrayList originalNodes;
    
    /** Creates a new instance of HierarchyMap */
    public LWHierarchyMap() 
    {
        super("test");
        
        rootNode = null;
        originalNodes = new ArrayList();
        distanceHash = new Hashtable();
        parentHash = new Hashtable();
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
        
        //stores default values to the distance and parent hashtables
        distanceHash.put(rootNode.toString(), new Integer(0));
        parentHash.put(rootNode.toString(), "none");
        
        //Dijkstra's theorem (shortest path)
        while(!nodesVector.isEmpty())
        {   
            //removes the first element in the vector as it is a queue
            LWNode currentNode = (LWNode)nodesVector.remove(0);
            
            //retrieves the current shortest distance to get to the given node from the root node
            int totalDistance = ((Integer)distanceHash.get(currentNode.toString())).intValue();
            
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
                if (!distanceHash.containsKey(nextNode.toString()) || 
                   totalDistance < ((Integer)distanceHash.get(nextNode.toString())).intValue())
                {  
                    //updates the distance and parent hashtables and adds to the vector
                    nodesVector.add(nextNode);
                    distanceHash.put(nextNode.toString(), new Integer(totalDistance));
                    parentHash.put(nextNode.toString(), currentNode.toString());                    
                }
                
                //keep track of nodes that are connected 
                if(!originalNodes.contains(nextNode))
                  originalNodes.add(nextNode);
            }
        }
          
        //debugging
        for (Enumeration e = parentHash.keys(); e.hasMoreElements();)
        {
          Object a = e.nextElement();
          System.out.println("key " + a);
          System.out.println("value " + parentHash.get(a) + "\n\n");
        }
    }
    
    /**creates the elements for the hierarchy map including duplicated nodes
       and links between them according to the shortest path*/
    public void createMap()
    {
        //create the duplicates of the nodes and map them to the original nodes
        //using the node hashtable
        for (Iterator i = originalNodes.iterator(); i.hasNext();)
        {
            LWNode node = (LWNode)i.next();
            LWNode copy = (LWNode)node.duplicate();
            addNode(copy);
            
            //maps the orginal node to the duplicated one using the nodes' toString method
            nodeHash.put(node.toString(), copy.toString());
        }
        
        //verify a path between nodes and create a link between the nodes
        for (Iterator i = originalNodes.iterator(); i.hasNext();)
        {
            String childLabel = ((LWNode)i.next()).toString(); 
            String parentLabel = (String)parentHash.get(childLabel);
            
            //if the node has a parent
            if (!parentLabel.equals("none"))
              //creates the link using the mapped duplicates of the original nodes
              createLink((String)nodeHash.get(parentLabel), (String)nodeHash.get(childLabel));
        }
    }
    
    /**creates a link between two given nodes*/
    public void createLink(String parentLabel, String childLabel)
    {        
        LWNode parent = null, child = null;
         
        //searches for the parent and child nodes
        for (Iterator i = getNodeIterator(); i.hasNext();)
        {
          LWNode node = (LWNode)i.next();
          
          if (node.toString().equals(parentLabel))
            parent = node;
          
          else if (node.toString().equals(childLabel))
            child = node;
          
          //if both parent node and child node are found, break out of the loop
          if (parent != null && child != null)
            break;
        }
        
        //creates the link and adds it to the map
        LWLink link = new LWLink(parent, child);
        addLink(link);
    }
    
    /**organizes the nodes in a hierarchical manner*/
    public void layout()
    {
        //has to set the location of each node in a hierarchical manner
        for (Iterator i = getNodeIterator(); i.hasNext();)
        {
            LWNode currentNode = (LWNode)i.next();
            
            //how to set up the hierarchy??
        }
    }
    
    /**biggest question is whether this should inherit LWMap or return LWMap as a product of a method*/
    public void createHierarchy()
    {    
        computeShortestPath();
        createMap();
        layout();
    }
}
