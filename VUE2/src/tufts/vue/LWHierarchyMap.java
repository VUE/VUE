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
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;

public class LWHierarchyMap extends LWMap {
    
    private LWNode rootNode;
    private ArrayList nodesList, linksList;
    
    /** Creates a new instance of HierarchyMap */
    public LWHierarchyMap() 
    {
        rootNode = null;
        nodesList = new ArrayList();
        linksList = new ArrayList();
    }
    
    public LWHierarchyMap(LWNode node)
    {
        this();
        rootNode = (LWNode)node.duplicate();
    }
    
    public void setRootNode(LWNode node)
    {
        rootNode = (LWNode)node.duplicate();
    }
    
    public LWNode getRootNode()
    {
        return rootNode;
    }
    
    /**Dijkstra's theorem should be used here to compute the shortest path*/
    public void setupHierarchy()
    {
        Vector nodesVector = new Vector();
        nodesVector.add(rootNode);
        LWNode currentNode;
        //gotta set up the distance too
        
        while(!nodesVector.isEmpty())
        {
            currentNode = (LWNode)nodesVector.firstElement();
            
            for(Iterator i = currentNode.getLinkIterator(); i.hasNext();)
            {
                LWLink connectedLink = (LWLink)i.next();
                LWNode otherNode = null;
                
                if ((otherNode = (LWNode)connectedLink.getComponent1()) == currentNode)
                    otherNode = (LWNode)connectedLink.getComponent2();
                
                
            }
        }
    }
}
