/*
 * CastorDR.java
 *
 * Created on October 21, 2003, 6:21 PM
 */

package  tufts.vue;

/**
 *
 * @author  akumar03
 */
import osid.dr.*;
import tufts.dr.fedora.*;

public class CastorDR {
    DR dr;
    String id;
    String displayName;
    String description;
    
    public CastorDR() {
    }
    
    public CastorDR(DR dr) {
        try {
            this.id = dr.getId().getIdString();
            this.displayName = dr.getDisplayName();
            this.description = dr.getDescription();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public DR getDR() {
        try {
            dr = new DR(id, displayName, description);
            return dr;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public void setId(String id) {
        this.id  = id;
    }
    public String getId() {
        return this.id;
    }
    public void setdisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName(){
        return this.displayName;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription(){
        return this.description;
        
    }
}