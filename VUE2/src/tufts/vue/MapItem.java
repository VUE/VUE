package tufts.vue;

/**
 * MapItem.java
 *
 * Base abstract class for elements in the map model.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
public abstract class MapItem
{
    private static final String EMPTY = "";
    
    private String ID = null;
    private String label = null;
    private String notes = EMPTY;
    private String metaData = EMPTY;
    private String category = EMPTY;
    private float x;
    private float y;

    public MapItem()
    {
        this.ID = super.toString(); // temporary
    }
    public MapItem(String label)
    {
        this();
        setLabel(label);
    }

    private java.util.List listeners;
    public void addChangeListener(MapItemChangeListener listener)
    {
        if (listeners == null)
            listeners = new java.util.ArrayList();
        listeners.add(listener);
    }
    public void removeChangeListener(MapItemChangeListener listener)
    {
        if (listeners == null)
            return;
        listeners.remove(listener);
    }
    public void notifyChangeListeners(MapItemChangeEvent e)
    {
        if (listeners == null)
            return;
        this.inNotify = true;
        try {
            java.util.Iterator i = listeners.iterator();
            while (i.hasNext())
                ((MapItemChangeListener)i.next()).mapItemChanged(e);
        } finally {
            this.inNotify = false;
        }
    }
    
    private boolean inNotify = false;
    private void notify(String what)
    {
        notifyChangeListeners(new MapItemChangeEvent(this, what));
    }
    
    public float getX()
    {
        return this.x;
    }
    public float getY()
    {
        return this.y;
    }

    public void setPosition(java.awt.geom.Point2D p)
    {
        setPosition((float)p.getX(), (float)p.getY());
    }
    
    public java.awt.geom.Point2D getPosition()
    {
        return new java.awt.geom.Point2D.Float(this.x, this.y);
    }

    public void setPosition(float x, float y)
    {
        if (this.inNotify) return;
        this.x = x;
        this.y = y;
        notify("position");
    }
    public void setID(String ID)
    {
        if (this.inNotify) return;
        this.ID = ID;
        notify("ID");
    }

    public void setLabel(String label)
    {
        if (this.inNotify) return;
        this.label = label;
        notify("label");
    }

    public void setNotes(String notes)
    {
        if (this.inNotify) return;
        this.notes = notes;
        notify("notes");
    }

    public void setMetaData(String metaData)
    {
        if (this.inNotify) return;
        this.metaData = metaData;
        notify("meta-data");
    }

    public void setCategory(String category)
    {
        if (this.inNotify) return;
        this.category = category;
        notify("category");
    }
    
    public String getCategory()
    {
        return this.category;
    }
    
    public String getID()
    {
        return this.ID;
    }

    public String getLabel()
    {
        return this.label;
    }

    public String getNotes()
    {
        return this.notes;
    }

    public String getMetaData()
    {
        return this.metaData;
    }

    public String toString()
    {
        return getClass().getName() + "[id=" + getID() + " " + getLabel() + "]";
    }
    
    
}
