/*
 * HierarchyNode.java
 *
 * Created on October 2, 2003, 11:11 AM
 */

package tufts.oki.hierarchy;

import java.util.Vector;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;

import tufts.vue.LWComponent;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class HierarchyNode implements osid.hierarchy.Node
{
    private osid.shared.Type type;
    private osid.shared.Id id;
    private String name;
    private String description;
    private DefaultMutableTreeNode treeNode;
    private LWComponent component;
    
    /** Creates a new instance of HierarchyNode */
    public HierarchyNode(osid.shared.Id id, DefaultMutableTreeNode treeNode) 
    { 
        type = null;
        this.id = id;
        name = null;
        description = null;
        component = null;
        
        this.treeNode = treeNode;
    }
    
    public osid.hierarchy.NodeIterator getChildren() throws osid.hierarchy.HierarchyException 
    {   
        if (treeNode == null)
         throw new osid.hierarchy.HierarchyException("tree node is null");
        
        Vector children = new Vector();
        
        for (Enumeration e = treeNode.children(); e.hasMoreElements();)
        {
            DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) e.nextElement();
            children.addElement((osid.hierarchy.Node) (childTreeNode.getUserObject()));
            //osid.hierarchy.Node childNode = (osid.hierarchy.Node) e.nextElement();
            //children.addElement(childNode);
        }

        return (osid.hierarchy.NodeIterator) (new HierarchyNodeIterator(children));
    }
    
    public osid.shared.Id getId() throws osid.hierarchy.HierarchyException 
    {
        return id;
    }
    
    public String getDisplayName() throws osid.hierarchy.HierarchyException 
    {
        return name;
    }
    
    public void updateDisplayName(java.lang.String name) throws osid.hierarchy.HierarchyException 
    {
        if (name == null) 
          throw new osid.hierarchy.HierarchyException("display name is null");
        
        this.name = name;
    }
    
    public String getDescription() throws osid.hierarchy.HierarchyException 
    {
        return this.description;
    }
    
    public void updateDescription(java.lang.String description) throws osid.hierarchy.HierarchyException 
    {
        if (description == null) 
          throw new osid.hierarchy.HierarchyException("description is null");
        
        this.description = description;
    }
    
    public osid.hierarchy.NodeIterator getParents() throws osid.hierarchy.HierarchyException 
    {   
        if (treeNode == null)
         throw new osid.hierarchy.HierarchyException("tree node is null");
        
        Vector parents = new java.util.Vector();
        DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) treeNode.getParent();

        if (parentTreeNode != null) 
          parents.addElement((osid.hierarchy.Node) (parentTreeNode.getUserObject()));
          //parents.addElement(parentNode);
        
        //maybe not needed, since you can check for this one level up
        else
          throw new osid.hierarchy.HierarchyException("this node doesn't have a parent");
        
        return (osid.hierarchy.NodeIterator) (new HierarchyNodeIterator(parents));
    }
    
    public void setType(osid.shared.Type type) throws osid.hierarchy.HierarchyException
    {
        this.type = type; 
    }
    
    public osid.shared.Type getType() throws osid.hierarchy.HierarchyException 
    {
        return type;
    }
    
    public boolean isLeaf() throws osid.hierarchy.HierarchyException 
    {
        return treeNode.isLeaf();
    }
    
    public boolean isRoot() throws osid.hierarchy.HierarchyException 
    {
        return treeNode.isRoot();
    }
    
    public void addParent(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        //unimplmented
    }
    
    public void removeParent(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        //unimplemented
    }
    
    public void changeParent(osid.shared.Id first, osid.shared.Id second) throws osid.hierarchy.HierarchyException
    {
        //unimplemented
    }
    
    /** custom methods */
    public DefaultMutableTreeNode getTreeNode() 
    {
        return treeNode;
    }
    
    public void changeLWComponentLabel(String label) throws osid.hierarchy.HierarchyException
    {
        component.setLabel(label);
        updateDisplayName(label);
    }
    
    public void setLWComponent(LWComponent component)
    {
        this.component = component;
    }
    
    public LWComponent getLWComponent()
    {
        return component;
    }
    
    public String toString()
    {
        return name;
    }
}
