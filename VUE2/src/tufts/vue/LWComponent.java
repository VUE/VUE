package tufts.vue;

import java.awt.Shape;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
    implements VueConstants,
               MapItemListener,
               LWCListener
// todo: consider subclassing the abstract RectangularShape?
{
    private float x;
    private float y;
    protected float width;
    protected float height;
    protected Color fillColor = COLOR_FILL;
    protected Color textColor = COLOR_TEXT;
    protected Color strokeColor = COLOR_STROKE;
    protected float strokeWidth = 1f;
    protected Font font = FONT_DEFAULT;
    protected boolean isAutoSized = true;

    protected boolean displayed = true;
    protected boolean selected = false;
    protected boolean indicated = false;

    protected LWComponent parent = null;

    protected MapItem mapItem;
    
    private java.util.List links = new java.util.ArrayList();
    private java.util.List children = new java.util.ArrayList();

    // Scale exists ONLY to support the child-node
    // convenience feature.
    protected float scale = 1.0f;
    protected final float ChildScale = 0.75f;

    private java.util.List lwcListeners;

    protected LWComponent() {}
    protected LWComponent(MapItem mapItem)
    {
        if (mapItem == null)
            throw new java.lang.IllegalArgumentException("LWNode: node is null");
        this.mapItem = mapItem;
        this.mapItem.addChangeListener(this);
    }

    // If the component has an area, it should
    // implement getShape().  Links, for instance,
    // don't need to implement this.
    public Shape getShape()
    {
        return null;
    }

    public boolean isAutoSized()
    {
        return this.isAutoSized;
    }
    public boolean setAutoSized(boolean tv)
    {
        return this.isAutoSized = tv;
    }
    public Color getFillColor()
    {
        return this.fillColor;
    }
    public void setFillColor(Color color)
    {
        this.fillColor = color;
    }
    public Color getTextColor()
    {
        return this.textColor;
    }
    public void setTextColor(Color color)
    {
        this.textColor = color;
    }
    public Color getStrokeColor()
    {
        return this.strokeColor;
    }
    public void setStrokeColor(Color color)
    {
        this.strokeColor = color;
    }
    public float getStrokeWidth()
    {
        return this.strokeWidth;
    }
    public void setStrokeWidth(float w)
    {
        this.strokeWidth = w;
    }
    public boolean isManagedColor()
    {
        return getFillColor() == COLOR_NODE_DEFAULT
            || getFillColor() == COLOR_NODE_INVERTED;
    }
    public Font getFont()
    {
        return this.font;
    }
    public void setFont(Font font)
    {
        this.font = font;
        layout();
    }
    /**
     * If this item supports children,
     * lay them out.
     */
    protected void layout() {}
    
    public void mapItemChanged(MapItemEvent e)
    {
        System.out.println(e);
        //MapItem mi = e.getSource();
        //setLocation(mi.getPosition());
    }

    /**
     * does this component paint on the map as a whole?
     * (e.g., links do this) -- If not, translate
     * into component coord space before it paints.
     */
    public boolean absoluteDrawing()
    {
        return false;
    }

    public MapItem getMapItem()
    {
        return this.mapItem;
    }

    public float getLabelX()
    {
        return getCenterX();
    }
    public float getLabelY()
    {
        return getCenterY();
    }
    
    public void addLink(LWLink link)
    {
        this.links.add(link);
    }
    
    public void removeLink(LWLink link)
    {
        this.links.remove(link);
    }

    public java.util.List getLinks()
    {
        return this.links;
    }

    /**
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it will include it's
     * own endpoints in the list.
     */
    public java.util.Iterator getLinkEndpointsIterator()
    {
        return
            new java.util.Iterator() {
                java.util.Iterator i = getLinks().iterator();
                public boolean hasNext() {return i.hasNext();}
		public Object next()
                {
                    LWLink lwl = (LWLink) i.next();
                    if (lwl.getComponent1() == LWComponent.this)
                        return lwl.getComponent2();
                    else
                        return lwl.getComponent1();
                }
		public void remove() {
		    throw new UnsupportedOperationException();
                }
            };
    }

    public boolean isChild()
    {
        return this.parent != null;
    }
    public LWComponent getParent()
    {
        return this.parent;
    }

    public void addChild(LWComponent c)
    {
        this.children.add(c);
        c.parent = this;
    }
    public void removeChild(LWComponent c)
    {
        this.children.remove(c);
        c.parent = null;
    }

    public boolean hasChildren()
    {
        return this.children.size() > 0;
    }

    public java.util.List getChildList()
    {
        return this.children;
    }
    public java.util.Iterator getChildIterator()
    {
        return this.children.iterator();
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

    public void setScale(float scale)
    {
        this.scale = scale;
        //System.out.println("Scale set to " + scale + " in " + this);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // todo: temporary hack color change for children
            if (c.isManagedColor()) {
                if (getFillColor() == COLOR_NODE_DEFAULT)
                    c.setFillColor(COLOR_NODE_INVERTED);
                else
                    c.setFillColor(COLOR_NODE_DEFAULT);
            }
            c.setScale(scale * ChildScale);
        }
    }
    
    public float getScale()
    {
        //return 1f;
        return this.scale;
    }
    public float getLayer()
    {
        return this.scale;
    }
    public void setLocation(float x, float y)
    {
        //System.out.println(this + " setLocation("+x+","+y+")");
        this.x = x;
        this.y = y;
        notify("location");
    }
    
    public void setLocation(double x, double y)
    {
        setLocation((float) x, (float) y);
    }

    public void setLocation(Point2D p)
    {
        setLocation((float) p.getX(), (float) p.getY());
    }
    
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
    public float getWidth() { return this.width * getScale(); }
    public float getHeight() { return this.height * getScale(); }
    public float getCenterX() { return this.x + getWidth() / 2; }
    public float getCenterY() { return this.y + getHeight() / 2; }

    public Rectangle2D getBounds()
    {
        return new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
    }
    
    /**
     * Default implementation: checks bounding box
     */
    public boolean contains(float x, float y)
    {
        return x >= this.x && x <= (this.x+getWidth())
            && y >= this.y && y <= (this.y+getHeight());
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        return rect.intersects(getX(), getY(), getWidth(), getHeight());
    }

    /**
     * Does x,y fall within the selection target for this component.
     * This default impl adds a 30 pixel swath to bounding box.
     */
    public boolean targetContains(float x, float y)
    {
        final int swath = 30; // todo: preference
        float sx = this.x - swath;
        float sy = this.y - swath;
        float ex = this.x + getWidth() + swath;
        float ey = this.y + getHeight() + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }

    /**
     * We divide area around the bounding box into 8 regions -- directly
     * above/below/left/right can compute distance to nearest edge
     * with a single subtract.  For the other regions out at the
     * corners, do a distance calculation to the nearest corner.
     * Behaviour undefined if x,y are within component bounds.
     */
    public float distanceToEdgeSq(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();

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
            return dx*dx + dy*dy;
        }
    }

    public Point2D nearestPoint(float x, float y)
    {
        float ex = this.x + getWidth();
        float ey = this.y + getHeight();
        Point2D.Float p = new Point2D.Float(x, y);

        if (x >= this.x && x <= ex) {
            // we're directly above or below this component
            if (y < this.y)
                p.y = this.y;
            else
                p.y = ey;
        } else if (y >= this.y && y <= ey) {
            // we're directly to the left or right of this component
            if (x < this.x)
                p.x = this.x;
            else
                p.x = ex;
        } else {
            // This computation only makes sense following the above
            // code -- we already know we must be closest to a corner
            // if we're down here.
            float nearCornerX = x > ex ? ex : this.x;
            float nearCornerY = y > ey ? ey : this.y;
            p.x = nearCornerX;
            p.y = nearCornerY;
        }
        return p;
    }

    public float distanceToEdge(float x, float y)
    {
        return (float) Math.sqrt(distanceToEdgeSq(x, y));
    }

    
    /**
     * Return the square of the distance from x,y to the center of
     * this components bounding box.
     */
    public float distanceToCenterSq(float x, float y)
    {
        float cx = getCenterX();
        float cy = getCenterY();
        float dx = cx - x;
        float dy = cy - y;
        return dx*dx + dy*dy;
    }
    
    public float distanceToCenter(float x, float y)
    {
        return (float) Math.sqrt(distanceToCenterSq(x, y));
    }
    
    public void draw(java.awt.Graphics2D g)
    {
        // System.err.println("drawing " + this);
    }
    

    public void LWCChanged(LWCEvent e)
    {
        if (e.getSource() == this)
            return;
        System.out.println(e);
        //MapItem mi = e.getSource();
        //setLocation(mi.getPosition());
    }
    
    public void addLWCListener(LWCListener listener)
    {
        if (lwcListeners == null)
            lwcListeners = new java.util.ArrayList();
        lwcListeners.add(listener);
    }
    public void removeLWCListener(LWCListener listener)
    {
        if (lwcListeners == null)
            return;
        lwcListeners.remove(listener);
    }
    public void notifyLWCListeners(LWCEvent e)
    {
        if (lwcListeners == null)
            return;
        java.util.Iterator i = lwcListeners.iterator();
        while (i.hasNext())
            ((LWCListener)i.next()).LWCChanged(e);
        //if (parent != null)
        //parent.notifyLWCListeners(e);
    }
    protected void notify(String what)
    {
        notifyLWCListeners(new LWCEvent(this, this, what));
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
