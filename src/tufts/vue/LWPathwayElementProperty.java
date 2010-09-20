/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * LWPathwayElementProperty.java
 *
 * Created on January 28, 2004, 1:11 PM
 */

package tufts.vue;

/**
 * @author  Daisuke Fujiwara
 * todo: this should really be an inner class to LWPathway, but castor doesn't
 * support mapping to inner classes.
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
        return "PEP[id=" + getElementID() + " notes="+notes + "]";
    }
}
