package tufts.vue;

import java.awt.Point;
import java.awt.geom.Point2D;

public class Node extends MapItem
{
    private Resource resource = null;
    // using awt Point for now
    private javax.swing.ImageIcon icon = null;

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

    public String toString()
    {
        return super.toString() + " resource=" + getResource();
    }

    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    public void setIcon(javax.swing.ImageIcon icon)
    {
        this.icon = icon;
    }

    public Resource getResource()
    {
        return this.resource;
    }

    public javax.swing.ImageIcon getIcon()
    {
        return this.icon;
    }
    

}
