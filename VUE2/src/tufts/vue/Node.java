package tufts.vue;

import java.awt.Point;
import java.awt.geom.Point2D;

public class Node extends MapItem
{
    private javax.swing.ImageIcon icon = null;
    private java.util.List children = new java.util.Vector();
    
    public Node() {
        super("Node");
    }
    public Node(String label)
    {
        super(label);
    }

    public Node(String label, Point2D p)
    {
        super(label);
        setPosition(p);
    }
    
    public Node(String label, Resource resource)
    {
        super(label);
        setResource(resource);
    }

    public Node(String label, Resource resource, Point2D p)
    {
        super(label);
        setResource(resource);
        setPosition(p);
    }

    public void addChild(Node node)
    {
        if (this.inNotify) return;
        this.children.add(node);
        notify("addChild");
    }
    
    public void removeChild(Node node)
    {
        if (this.inNotify) return;
        this.children.remove(node);
        notify("removeChild");
    }

    public java.util.Vector getChildList()
    {
        return (java.util.Vector) children;
    }
    
    public void setChildList(java.util.Vector children)
    {
        this.children = children;
    }

    public java.util.Iterator getChildIterator()
    {
        return this.children.iterator();
    }

    
    public String toString()
    {
        return super.toString() + " resource=" + getResource();
    }

    public void setIcon(javax.swing.ImageIcon icon)
    {
        this.icon = icon;
    }

    public javax.swing.ImageIcon getIcon()
    {
        return this.icon;
    }
    

}
