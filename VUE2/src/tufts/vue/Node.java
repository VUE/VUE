package tufts.vue;

import java.awt.Point;
import java.awt.geom.Point2D;

public class Node extends MapItem
{
    private Resource resource = null;
    // using awt Point for now
    private Point2D position = new Point2D.Float(0,0);
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

    public void setPosition(Point2D position)
    {
        this.position = position;
    }

    public void setPosition(float x, float y)
    {
        this.position = new Point2D.Float(x, y);
    }

    public Point2D getPosition()
    {
        return this.position;
    }

    public float getX()
    {
        return (float) this.position.getX();
    }
    public float getY()
    {
        return (float) this.position.getY();
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
