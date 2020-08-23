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
 * HierarchyNode.java
 *
 * Created on October 2, 2003, 11:11 AM
 */

package tufts.oki.hierarchy;

import java.util.Vector;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;

import tufts.vue.LWComponent;
import tufts.vue.VUE;

/**
 *
 * @author  Daisuke Fujiwara
 * todo: re-implement so doesn't cache a copy of the node name?
 *      or: make a label change listener on the LWComponent
 */

/**A class that represents a node of a hierarchy structure*/
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
    
    /**A method that retrieves the children of the node*/
    public osid.hierarchy.NodeIterator getChildren() throws osid.hierarchy.HierarchyException 
    {   
        if (treeNode == null)
         throw new osid.hierarchy.HierarchyException("tree node is null");
        
        Vector children = new Vector();
        
        for (Enumeration e = treeNode.children(); e.hasMoreElements();)
        {
            Object nextElement = e.nextElement();
            //for each child node, retrieves the hierarchy node associated with it and adds to the node vector
            
            DefaultMutableTreeNode childTreeNode = (DefaultMutableTreeNode) nextElement; 
            children.addElement((osid.hierarchy.Node) (childTreeNode.getUserObject()));
        }

        return (osid.hierarchy.NodeIterator) (new HierarchyNodeIterator(children));
    }
    
    /**A method that returns the node's ID*/
    public osid.shared.Id getId() throws osid.hierarchy.HierarchyException 
    {
        return id;
    }
    
    /**A method that returns the node's display name*/
    public String getDisplayName() throws osid.hierarchy.HierarchyException 
    {
        //return component.getDisplayLabel();
        return name;
    }
    
    /**A method that modifies the node's display name*/
    public void updateDisplayName(java.lang.String name) throws osid.hierarchy.HierarchyException 
    {
        if (name == null) 
          throw new osid.hierarchy.HierarchyException("display name is null");
        
        this.name = name;
    }
    
    /**A method that returns the node's description*/
    public String getDescription() throws osid.hierarchy.HierarchyException 
    {
        return this.description;
    }
    
    /**A method that modifies the node's description*/
    public void updateDescription(java.lang.String description) throws osid.hierarchy.HierarchyException 
    {
        if (description == null) 
          throw new osid.hierarchy.HierarchyException("description is null");
        
        this.description = description;
    }
    
    /**A method that retrieves the parents of the node*/
    public osid.hierarchy.NodeIterator getParents() throws osid.hierarchy.HierarchyException 
    {   
        if (treeNode == null)
         throw new osid.hierarchy.HierarchyException("tree node is null");
        
        Vector parents = new java.util.Vector();
        DefaultMutableTreeNode parentTreeNode = (DefaultMutableTreeNode) treeNode.getParent();

        //this ONLY works for the single parent structure
        if (parentTreeNode != null) 
          parents.addElement((osid.hierarchy.Node) (parentTreeNode.getUserObject()));
        
        //maybe not needed, since you can check for this one level up
        else
          throw new osid.hierarchy.HierarchyException("this node doesn't have a parent");
        
        return (osid.hierarchy.NodeIterator) (new HierarchyNodeIterator(parents));
    }
    
    /**A method that sets the type of the node*/
    public void setType(osid.shared.Type type) throws osid.hierarchy.HierarchyException
    {
        this.type = type; 
    }
    
    /**A method that gets the type of the node*/
    public osid.shared.Type getType() throws osid.hierarchy.HierarchyException 
    {
        return type;
    }
    
    /**A method that indicates whether the node is a leaf or not*/
    public boolean isLeaf() throws osid.hierarchy.HierarchyException 
    {
        return treeNode.isLeaf();
    }
    
    /** method that indicates whether the node is a root or not*/
    public boolean isRoot() throws osid.hierarchy.HierarchyException 
    {
        return treeNode.isRoot();
    }
    
    /**A method that adds a parent to the node*/
    public void addParent(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        //unimplmented
    }
    
    /**A method that removes a parent from the node*/
    public void removeParent(osid.shared.Id nodeId) throws osid.hierarchy.HierarchyException 
    {
        //unimplemented
    }
    
    /**A method that changes a parent to another parent*/
    public void changeParent(osid.shared.Id first, osid.shared.Id second) throws osid.hierarchy.HierarchyException
    {
        //unimplemented
    }
    
    /** custom methods */
    
    /**A method that returns the tree node associated with the hierarchy node*/
    public DefaultMutableTreeNode getTreeNode() 
    {
        return treeNode;
    }
    
    /**A method that changes the label associated with the LWComponent*/
    public void changeLWComponentLabel(String label) throws osid.hierarchy.HierarchyException
    {
        if (!component.getDisplayLabel().equals(label)) {
            component.setLabel(label);
            updateDisplayName(label);
            //creating an undo action
            VUE.getUndoManager().mark();
        }
    }
    
    /**A method that sets the LWComponent associated with the hierarchy node*/
    public void setLWComponent(LWComponent component)
    {
        this.component = component;
    }
    
    /**A method that gets the LWComponent associated with the hierarchy node*/
    public LWComponent getLWComponent()
    {
        return component;
    }
    
    /**A method that returns the string representation of the node*/
    public String toString()
    {
        return name;
    }
}
