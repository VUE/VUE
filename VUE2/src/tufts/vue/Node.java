package tufts.vue;

import java.awt.Point;

public class Node extends MapItem
{
    private Resource resource = null;
    // using awt Point for now
    private Point position = new Point(0,0);
    private javax.swing.ImageIcon icon = null;

    public Node(String label)
    {
        super(label);
    }

    public Node(String label, Point p)
    {
        super(label);
        setPosition(p);
    }
    
    public Node(String label, Resource resource)
    {
        super(label);
        setResource(resource);
    }

    public Node(String label, Resource resource, Point p)
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

    public void setPosition(java.awt.Point position)
    {
        this.position = position;
    }

    public void setPosition(int x, int y)
    {
        this.position = new Point(x, y);
    }

    public void setIcon(javax.swing.ImageIcon icon)
    {
        this.icon = icon;
    }

    public Resource getResource()
    {
        return this.resource;
    }

    public java.awt.Point getPosition()
    {
        return this.position;
    }

    public javax.swing.ImageIcon getIcon()
    {
        return this.icon;
    }
    

}
