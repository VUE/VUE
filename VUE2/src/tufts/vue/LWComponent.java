package tufts.vue;

import java.awt.geom.Point2D;

/**
 * LWComponent.java
 * 
 * Light-weight component base class for creating components to be
 * rendered by the ConceptMapView class, or anyone who wants to do
 * their own rendering and hit-detection.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */

class LWComponent
{
    public static final java.awt.Stroke STROKE_ONE = new java.awt.BasicStroke(1f);
    public static final java.awt.Stroke STROKE_TWO = new java.awt.BasicStroke(2f);
    public static final java.awt.Stroke STROKE_DEFAULT = STROKE_ONE;

    public static final java.awt.Color COLOR_SELECTION = java.awt.Color.blue;
    public static final java.awt.Color COLOR_INDICATION = java.awt.Color.red;
    public static final java.awt.Color COLOR_DEFAULT = java.awt.Color.black;
    public static final java.awt.Color COLOR_FAINT = java.awt.Color.lightGray;
    
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean displayed = true;
    protected boolean selected = false;
    protected boolean indicated = false;

    private java.util.List links = new java.util.ArrayList();

    protected LWComponent() {}

    public void addLink(LWLink link)
    {
        this.links.add(link);
    }
    
    public void removeLink(LWLink link)
    {
        this.links.remove(link);
    }

    public LWLink getLinkTo(LWComponent c)
    {
        java.util.Iterator i = this.links.iterator();
        while (i.hasNext()) {
            LWLink lwl = (LWLink) i.next();
            if (lwl.getComponent1() == c || lwl.getComponent2() == c)
                return lwl;
        }
        return null;
    }

    public boolean hasLinkTo(LWComponent c)
    {
        return getLinkTo(c) != null;
    }

    public MapItem getMapItem()
    {
        return null;
    }

    public void setLocation(float x, float y)
    {
        //System.out.println(this + " setLocation("+x+","+y+")");
        this.x = x;
        this.y = y;
    }

    /*
    public void setLocation(Point2D p)
    {
        this.x = (float) p.getX();
        this.y = (float) p.getY();
        }*/
    
    public Point2D getLocation()
    {
        return new Point2D.Float(this.x, this.y);
    }
    
    public void setSize(float w, float h)
    {
        this.width = w;
        this.height = h;
    }

    public float getX() { return this.x; }
    public float getY() { return this.y; }
    public float getWidth() { return this.width; }
    public float getHeight() { return this.height; }
    
    /**
     * Default implementation: checks bounding box
     */
    public boolean contains(float x, float y)
    {
        return x >= this.x && x <= (this.x+width)
            && y >= this.y && y <= (this.y+height);
    }

    /**
     * Does x,y fall within the selection target for this component.
     * This default impl adds a 30 pixel swath to bounding box.
     */
    public boolean targetContains(int x, int y)
    {
        final int swath = 30; // todo: preference
        float sx = this.x - swath;
        float sy = this.y - swath;
        float ex = this.x + width + swath;
        float ey = this.y + height + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }

    /**
     * We divide area around the bounding box into 8 regions -- directly
     * above/below/left/right can compute distance to nearest edge
     * with a single subtract.  For the other regions out at the
     * corners, do a distance calculation to the nearest corner.
     * Behaviour undefined if x,y are within component bounds.
     */
    public float distanceToEdge(float x, float y)
    {
        float ex = this.x + width;
        float ey = this.y + height;

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            return y < this.y ? this.y - y : y - ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            return x < this.x ? this.x - x : x - ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            float dx = nearCornerX - x;
            float dy = nearCornerY - y;
            return (float) java.lang.Math.sqrt(dx*dx + dy*dy);
        }
    }
    
    /**
     * Return the distance from x,y to the center of
     * this components bounding box.
     */
    public float distanceToCenter(float x, float y)
    {
        float cx = this.x + width / 2;
        float cy = this.y + height / 2;
        float dx = cx - x;
        float dy = cy - y;
        return (float) java.lang.Math.sqrt(dx*dx + dy*dy);
    }
    
    public void draw(java.awt.Graphics2D g)
    {
        // System.err.println("drawing " + this);
    }
    
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
    public boolean isSelected()
    {
        return this.selected;
    }
    
    public void setDisplayed(boolean displayed)
    {
        this.displayed = displayed;
    }
    public boolean isDisplayed()
    {
        return this.displayed;
    }

    public void setIndicated(boolean indicated)
    {
        this.indicated = indicated;
    }
    
    public boolean isIndicated()
    {
        return this.indicated;
    }
    
    public String toString()
    {
        return super.toString() + "["+x+","+y + " " + width + "x" + height + " " + getMapItem() + "]";
    }
    
}
