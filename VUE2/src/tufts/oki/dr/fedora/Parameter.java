/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * Parameter.java
 *
 * Created on October 13, 2003, 1:18 PM
 */

package tufts.oki.dr.fedora;

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
