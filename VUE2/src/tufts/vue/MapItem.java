package tufts.vue;

/**
 * MapItem.java
 *
 * @author Scott Fraize
 * @version 6/7/03
 */
public interface MapItem
{
    public float getX();
    public void setX(float x);
    public float getY();
    public void setY(float y);
    
    public void setID(String ID);
    public void setLabel(String label);
    public void setNotes(String notes);
    public void setMetaData(String metaData);
    public void setCategory(String category);
    public void setResource(Resource resource);
    
    public Resource getResource();
    public String getCategory();
    public String getID();
    public String getLabel();
    public String getMetaData();
    
    //added by Daisuke
    public String getNotes();
}
