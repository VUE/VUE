/*
 * CastorFedoraObject.java
 *
 * Created on October 21, 2003, 6:13 PM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */
// this object exists only for saving and restoring assets
import osid.dr.*;
import tufts.oki.dr.fedora.*;

public class CastorFedoraObject {
    String id;
    String type;
    CastorDR castorDR;
    DR dr;
    FedoraObject object;
    FedoraObjectAssetType fedoraObjectAssetType;
    String displayName;
    
    public CastorFedoraObject() {
    }
    
    public CastorFedoraObject(FedoraObject object) {
        try {
            this.id = object.getId().getIdString();
            this.displayName = object.getDisplayName();
            this.castorDR = new CastorDR(object.getDR());
            this.type = ((FedoraObjectAssetType)object.getAssetType()).getType();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public FedoraObject getFedoraObject() {
        try  {
            dr = castorDR.getDR();
            fedoraObjectAssetType =  dr.createFedoraObjectAssetType(type);
            object = new FedoraObject(dr,id,displayName,fedoraObjectAssetType);
            return object;
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
    
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return this.type;
    }
    public void setCastorDR(CastorDR castorDR)  {
        this.castorDR = castorDR;
    }
    public CastorDR getCastorDR() {
        return this.castorDR;
    }
    public void setdisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName(){
        return this.displayName;
    }
}
