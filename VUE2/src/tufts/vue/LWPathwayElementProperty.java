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
    private transient LWComponent c;
    
    /** for persistance restores */
    public LWPathwayElementProperty()
    {
        ID = "no ID";
    }
    
    public LWPathwayElementProperty(LWComponent c) 
    {
        setComponent(c);
    }
    
    /*    
    public LWPathwayElementProperty(String ID, String notes)
    {
        this.ID = ID;
        this.notes = notes;
    }
    */
    
    /** for persistance */
    public String getElementID() {
        return ID;
    }
    
    /** for persistance */
    public void setElementID(String ID) {
        this.ID = ID;
    }

    void setComponent(LWComponent c) {
        this.c = c;
        this.ID = c.getID();
    }
    
    LWComponent getComponent() {
        return this.c;
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

    public String toString()
    {
        return "LWPathwayElementProperty[cid=" + getElementID() + " note="+notes + "]";
    }
}
