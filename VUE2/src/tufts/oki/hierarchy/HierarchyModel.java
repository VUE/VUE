/*
 * HierarchyModel.java
 *
 * Created on October 2, 2003, 10:56 AM
 */

package tufts.oki.hierarchy;

import java.util.Vector;
import java.util.HashMap;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Iterator;
import java.util.Enumeration;

import tufts.vue.LWNode;
import tufts.vue.HierarchyTreeModel;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class HierarchyModel implements osid.hierarchy.Hierarchy
{
    private String description, name;
    private osid.shared.Id ID;
  
    boolean multipleParents = false;
    boolean recursion = true;
    
    private HashMap map;
    private Vector availableTypes;
    private DefaultTreeModel treeModel;
    
    private int nextID = 1;
     
    private String getNextID()
    {
        return Integer.toString(nextID++, 10);
    }
    
    /** Creates a new instance of HierarchyModel */
    public HierarchyModel(LWNode rootNode)
    {
        map = new HashMap();
        
        try 
        {
            ID = new tufts.oki.shared.Id(getNextID());
            
        }
        
        catch (osid.shared.SharedException se)
        {
            System.out.println("shared exception");
        }
            
        name = "no name";
        description = "no description";
        
        availableTypes = new Vector();
        
        setUpHierarchyNodes(rootNode, null);
    }
    
    public HierarchyModel(LWNode rootNode, String name, String description) 
    {
        this(rootNode);
        this.name = name;
        this.description = description;
    }
    
    public void addNodeType(osid.shared.Type type) throws osid.hierarchy.HierarchyException 
    {
        if (type == null) 
          //throw new osid.hierarchy.HierarchyException(osid.hierarchy.HierarchyException.NULL_ARGUMENT);
          throw new osid.hierarchy.HierarchyException("type can't be null");
        
        if(!availableTypes.contains(type))
          availableTypes.add(type);
        
        else
          throw new osid.hierarchy.HierarchyException("type already exists");
    }
    
    public void removeNodeType(osid.shared.Type nodeType) throws osid.hierarchy.HierarchyException 
    {
        if (nodeType == null) 
          throw new osid.hierarchy.HierarchyException("type can't be null");
        
        try 
        {
            // check no Node has using this NodeType
            for (osid.hierarchy.NodeIterator nodeIterator = getAllNodes(); nodeIterator.hasNext();) 
            {
                if (nodeType.isEqual(nodeIterator.next().getType())) 
                  throw new osid.hierarchy.HierarchyException("node is being used");
            }

            for (Iterator i = availableTypes.iterator(); i.hasNext();)
            {
                osid.shared.Type type = (osid.shared.Type)i.next();
                if (nodeType.isEqual(type))
                {
                    i.remove();
                    break;
                }
            }
        } 
        
        catch (osid.hierarchy.HierarchyException oex) 
        {
            throw new osid.hierarchy.HierarchyException(oex.getMessage());
        }

        //throw new osid.hierarchy.HierarchyException(osid.hierarchy.HierarchyException.NODE_TYPE_NOT_FOUND);
    }

    public boolean allowsMultipleParents() throws osid.hierarchy.HierarchyException 
    {
        return multipleParents;
    }
    
    public boolean allowsRecursion() throws osid.hierarchy.HierarchyException 
    {
        return recursion;
    }
    
    public osid.hierarchy.Node createNode(osid.shared.Id nodeId, osid.shared.Id parentId, osid.shared.Type type, String name, String description) throws osid.hierarchy.HierarchyException 
    {
        if ((nodeId == null) || (parentId == null) || (name == null) || (type == null) || (description == null)) 
          throw new osid.hierarchy.HierarchyException("arguments are null");
        
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
        HierarchyNode node = new HierarchyNode(nodeId, treeNode);
        
        node.setType(type);
        node.updateDisplayName(name);
        node.updateDescription(description);
        treeNode.setUserObject(node);
        
        //can we implement this in other way using the node interface?
        try
        {
            HierarchyNode parentNode = (HierarchyNode)map.get(parentId.getIdString());
            DefaultMutableTreeNode parentTreeNode = parentNode.getTreeNode(); 
        
            if (parentTreeNode != null)
              parentTreeNode.add(treeNode);
        
            else
              throw new osid.hierarchy.HierarchyException("the parent node is null");
            
            map.put(nodeId.getIdString(), node);
            return (osid.hierarchy.Node)node;
        }
        
        catch (osid.shared.SharedException se)
        {
            throw new osid.hierarchy.HierarchyException("exception");
        }
    }
    
    public osid.hierarchy.Node createRootNode(osid.shared.Id nodeId, osid.shared.Type type, String name, String description) throws osid.hierarchy.HierarchyException 
    {
        if ((nodeId == null) || (name == null) || (type == null) || (description == null)) 
          throw new osid.hierarchy.HierarchyException("arguments are null");
        
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
        HierarchyNode rootNode = new HierarchyNode(nodeId, rootTreeNode);
       
        rootNode.setType(type);
        rootNode.updateDisplayName(name);
        rootNode.updateDescription(description);
        rootTreeNode.setUserObject(rootNode);
        
        try 
        {
            map.put(nodeId.getIdString(), rootNode);
        }
        
        catch (osid.shared.SharedException se)
        {
            throw new osid.hierarchy.HierarchyException("Exception");
        }
        
        treeModel = new DefaultTreeModel(rootTreeNode);
        
        return (osid.hierarchy.Node)rootNode;
    }
    
    public void deleteNode(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        try
        {
            HierarchyNode node = (HierarchyNode)map.get(nodeId.getIdString());
        
            if (node != null && !node.isRoot())
            {   
                DefaultMutableTreeNode treeNode = node.getTreeNode();
            
                if (treeNode != null)
                  treeModel.removeNodeFromParent(treeNode);
            
                map.remove(nodeId.getIdString());
            }
        
            else
              throw new osid.hierarchy.HierarchyException("can't delete the node");
        }
        
        catch (osid.shared.SharedException se)
        {
            throw new osid.hierarchy.HierarchyException("Exception");
        }
    }
    
    public osid.hierarchy.NodeIterator getAllNodes() throws osid.hierarchy.HierarchyException 
    {
        Vector list = new Vector();
        
        for (Iterator i = map.values().iterator(); i.hasNext();)
           list.add((osid.hierarchy.Node)i.next());
        
        return (osid.hierarchy.NodeIterator)(new HierarchyNodeIterator(list));
    }
    
    public String getDescription() throws osid.hierarchy.HierarchyException 
    {
        return description;
    }
    
    public String getDisplayName() throws osid.hierarchy.HierarchyException 
    {
        return name;
    }
    public osid.shared.Id getId() throws osid.hierarchy.HierarchyException 
    {
        return ID;
    }
    
    public void updateDescription(java.lang.String description) throws osid.hierarchy.HierarchyException 
    {
        this.description = description;
    }
    
    //hashing could be useful
    public osid.hierarchy.Node getNode(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        try
        {
            osid.hierarchy.Node node = (osid.hierarchy.Node)map.get(nodeId.getIdString());
            return node;
        }
        
        catch (osid.shared.SharedException se)
        {
            throw new osid.hierarchy.HierarchyException("Exception");
        }
    }
    
    public osid.shared.TypeIterator getNodeTypes() throws osid.hierarchy.HierarchyException 
    {
        return (osid.shared.TypeIterator) (new tufts.oki.shared.TypeIterator(availableTypes));
    }
    
    public osid.hierarchy.NodeIterator getRootNodes() throws osid.hierarchy.HierarchyException 
    {
        Vector rootNodes = new Vector();
     
        for (Iterator i = map.values().iterator(); i.hasNext();)
        {
            osid.hierarchy.Node nextNode = (osid.hierarchy.Node)i.next();
            
            if (nextNode.isRoot())
              rootNodes.add(nextNode);
        }
        
        return (osid.hierarchy.NodeIterator)(new HierarchyNodeIterator(rootNodes));
    }
    
    public osid.hierarchy.TraversalInfoIterator traverse(osid.shared.Id startId, int mode, int direction, int levels) throws osid.hierarchy.HierarchyException 
    {
        if ((mode != osid.hierarchy.Hierarchy.TRAVERSE_MODE_DEPTH_FIRST) && (mode != osid.hierarchy.Hierarchy.TRAVERSE_MODE_BREADTH_FIRST)) 
          throw new osid.hierarchy.HierarchyException("unknown mode");
        
        if ((direction != osid.hierarchy.Hierarchy.TRAVERSE_DIRECTION_UP) && (direction != osid.hierarchy.Hierarchy.TRAVERSE_DIRECTION_DOWN)) 
          throw new osid.hierarchy.HierarchyException("unknown direction");
        
        Vector traversalInfoList = new Vector();
        HierarchyNode startingNode = (HierarchyNode)getNode(startId);
        
        //osid.hierarchy.Node startingNode = getNode(startId);
        //traversalInfoList.add(new HierarchyTraversalInfo(startingNode.getId(), startingNode.getName(), levels));
        
        if (startingNode == null) 
          throw new osid.hierarchy.HierarchyException("unknown node");
        
        else
        {
            Enumeration e;
            DefaultMutableTreeNode startingTreeNode = startingNode.getTreeNode();
            
            if (direction == osid.hierarchy.Hierarchy.TRAVERSE_DIRECTION_DOWN)
            {
                if (mode == osid.hierarchy.Hierarchy.TRAVERSE_MODE_DEPTH_FIRST) 
                  e = startingTreeNode.depthFirstEnumeration();
             
                else 
                  e = startingTreeNode.breadthFirstEnumeration();
                
                int maxLevel;
                int currentLevel = startingTreeNode.getLevel();
                int startLevel = currentLevel;

                if (levels >= 0) 
                  maxLevel = currentLevel + levels;
               
                else 
                  maxLevel = Integer.MAX_VALUE;
                
                while (e.hasMoreElements()) 
                {
                    DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode)e.nextElement();

                    if (nextNode.getLevel() > maxLevel) 
                        break;

                    osid.hierarchy.Node node = (osid.hierarchy.Node)nextNode.getUserObject();
                    traversalInfoList.add((osid.hierarchy.TraversalInfo) (new HierarchyTraversalInfo(
                        node.getId(), node.getDisplayName(),
                        currentLevel - startLevel)));
                }
            }
            
            else
            {
                HierarchyNode rootNode = (HierarchyNode)getRootNodes().next();
                DefaultMutableTreeNode rootTreeNode = rootNode.getTreeNode();
                
                if (mode == osid.hierarchy.Hierarchy.TRAVERSE_MODE_DEPTH_FIRST) 
                  e = rootTreeNode.depthFirstEnumeration();
                
                else 
                  e = rootTreeNode.breadthFirstEnumeration();
                
                int maxLevel;
                int currentLevel = startingTreeNode.getLevel();
                int startLevel = currentLevel + levels;

                if (levels >= 0) 
                  maxLevel = currentLevel;
                    
                else 
                  maxLevel = Integer.MAX_VALUE;
            
                // make a TraversalInfo for each node found
                while (e.hasMoreElements()) 
                {
                    DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) e.nextElement();

                    if (nextNode.getLevel() >= startLevel) 
                    {
                        osid.hierarchy.Node node = (osid.hierarchy.Node)nextNode.getUserObject();
                        traversalInfoList.add((osid.hierarchy.TraversalInfo) (new HierarchyTraversalInfo(
                            node.getId(), node.getDisplayName(),
                            currentLevel + startLevel)));
                    }

                    if (nextNode == startingTreeNode) 
                      break;
                }
                
                Vector reverseVector = new Vector();

                for (int j = traversalInfoList.size(); j >= 0; j--)
                    reverseVector.add(traversalInfoList.elementAt(j));

                traversalInfoList = reverseVector;
            }
        }
        
        HierarchyTraversalInfoIterator iterator = new HierarchyTraversalInfoIterator(traversalInfoList);
        return (osid.hierarchy.TraversalInfoIterator)iterator;
    }
    
    /**custom methods*/
    
    public void setUpHierarchyNodes(LWNode node, osid.hierarchy.Node parentNode)
    {
        try
        {
            osid.hierarchy.Node hierarchyNode;
        
            if (parentNode == null)
              hierarchyNode = createRootNode(new tufts.oki.shared.Id(getNextID()), new tufts.oki.shared.VueType(), 
                                                                    node.getLabel(), node.getNotes());
        
            else
              hierarchyNode = createNode(new tufts.oki.shared.Id(getNextID()), parentNode.getId(), new tufts.oki.shared.VueType(), 
                                                                 node.getLabel(), node.getNotes());
            
            ((HierarchyNode)hierarchyNode).setLWNode(node);
         
            //do it recursively
            for (Iterator i = node.getNodeIterator(); i.hasNext();)
            {
                LWNode nextNode = (LWNode)i.next();
                setUpHierarchyNodes(nextNode, hierarchyNode);
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
    
    /*
    public Vector traverseUpDepthFirst(osid.hierarchy.Node node, int level)
    {
        Vector list = new Vector();
        
        for(osid.hierarchy.NodeIterator iterator = node.getParents(); iterator.hasNext();)
        {
            osid.hierarchy.Node parent = iterator.next();
            list.add(new HierarchyTraversalInfo(parent.getId(), parent.getName(), level));
            
            if(!parent.isRoot() && level != 0)
              list.add(traverseUpDepthFirst(parent, level - 1));
        }
        
        return list;
    }
    
    public Vector traverseUpBreadthFirst(osid.hierarchy.Node node, int level)
    {
        Vector list = new Vector();
        
        for(osid.hierarchy.NodeIterator iterator = node.getParents(); iterator.hasNext();)
        {
            osid.hierarchy.Node parent = iterator.next();
            list.add(new HierarchyTraversalInfo(parent.getId(), parent.getName(), level));
        }
        //how to do ?
        return list;
    }
    
    public Vector traverseDownDepthFirst(osid.hierarchy.Node node, int level)
    {
        Vector list = new Vector();
        
        for(osid.hierarchy.NodeIterator iterator = node.getChildren(); iterator.hasNext();)
        {
            osid.hierarchy.Node child = iterator.next();
            traversalInfoList.add(new HierarchyTraversalInfo(child.getId(), child.getName(), level));
            
            if(!child.isLeaf() && level != 0)
              list.add(traverseDownDepthFirst(child, level - 1));
        }
    }
    
    public Vector traverseDownBreadthFirst(osid.hierarchy.Node node, int level)
    {
        for(osid.hierarchy.NodeIterator iterator = node.getChildren(); iterator.hasNext();)
        {
            osid.hierarchy.Node child = iterator.next();
            list.add(new HierarchyTraversalInfo(child.getId(), child.getName(), levels));
        }
        
        return list;
    }
    */
    
}