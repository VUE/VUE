/*
 * Parameter.java
 *
 * Created on October 13, 2003, 1:18 PM
 */

package tufts.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class Parameter implements osid.dr.InfoField {
    //private osid.OsidOwner owner = null;
    //private java.util.Map configuration = null;
    private java.util.Vector infoFieldVector = new java.util.Vector();
    private osid.dr.InfoStructure infoStructure = null;
    private osid.shared.Id id = null;
    private String  value = null;
    private osid.dr.InfoPart infoPart = null;
    /** Creates a new instance of Parameter */
    public Parameter() {
    }
    
    public osid.dr.InfoField createInfoField(osid.shared.Id infoPartId, java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public void deleteInfoField(osid.shared.Id infoFieldId) throws osid.dr.DigitalRepositoryException {
    }
    
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return this.id;
    }
    
    public osid.dr.InfoFieldIterator getInfoFields() throws osid.dr.DigitalRepositoryException {
        return null;
    }
    
    public osid.dr.InfoPart getInfoPart() throws osid.dr.DigitalRepositoryException {
       return  this.infoPart;
    }
    
    public java.io.Serializable getValue() throws osid.dr.DigitalRepositoryException {
        return this.value;
    }
    
    public void updateValue(java.io.Serializable value) throws osid.dr.DigitalRepositoryException {
        this.value = (String) value;
    }
    
}
