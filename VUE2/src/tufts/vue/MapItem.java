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

    public MapItem()
    {
        this.ID = super.toString(); // temporary
    }
    public MapItem(String label)
    {
        this();
        setLabel(label);
    }

    public String toString()
    {
        return "MapItem[id=" + getID() + " label=" + getLabel() + "]";
    }
    
    public void setID(String ID)
    {
        this.ID = ID;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public void setMetaData(String metaData)
    {
        this.metaData = metaData;
    }

    public void setCategory(String category)
    {
        this.category = category;
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
}
