package tufts.vue;

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
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean displayed = true;
    protected boolean selected = false;
    protected boolean indicated = false;

    protected LWComponent() {}

    public MapItem getMapItem()
    {
        return null;
    }

    public void setLocation(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    public void setLocation(java.awt.Point p)
    {
        this.x = p.x;
        this.y = p.y;
    }
    
    public void setSize(int w, int h)
    {
        this.width = w;
        this.height = h;
    }

    public int getX() { return this.x; }
    public int getY() { return this.y; }
    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    
    public boolean contains(int x, int y)
    {
        return x >= this.x && x <= (this.x+width)
            && y >= this.y && y <= (this.y+height);
    }
    
    public void draw(java.awt.Graphics g)
    {
        //System.err.println("drawing " + this);
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
        return getClass().getName() + "["+x+","+y + " " + width + "x" + height + "]";
    }
    
}
