/*
 * LWPathwayElementProperty.java
 *
 * Created on January 28, 2004, 1:11 PM
 */

package tufts.vue;

/**
 *
 * @author  Daisuke Fujiwara
 */
public class LWPathwayElementProperty 
{
    private String ID = null;
    private String notes = null;
    
    /** Creates a new instance of LWPathwayElement */
    public LWPathwayElementProperty()
    {
        ID = "no ID";
        //notes = "no pathway notes";
    }
    
    public LWPathwayElementProperty(String ID) 
    {
        this.ID = ID;
        //notes = "no pathway notes";
    }
    
    public LWPathwayElementProperty(String ID, String notes)
    {
        this.ID = ID;
        this.notes = notes;
    }
    
    public String getElementID()
    {
        return ID;
    }
    
    public void setElementID(String ID)
    {
        this.ID = ID;
    }
    
    public String getElementNotes()
    {
        if (notes != null && notes.length() < 1)
            this.notes = null;
        return notes;
    }
    
    public void setElementNotes(String notes)
    {
        if (notes != null && notes.trim().length() < 1)
            this.notes = null;
        else
            this.notes = notes;
    }
}
