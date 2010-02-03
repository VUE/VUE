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
 * ParameterInfoPart.java
 *
 * Created on October 13, 2003, 12:14 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
public class ParameterInfoPart implements osid.dr.InfoPart {
    java.util.Vector infoPartsVector = new java.util.Vector();
    osid.dr.InfoStructure infoStructure = null;
    //private osid.OsidOwner owner = null;
    //private java.util.Map configuration = null;
    private String displayName = "Parameter InfoPart";
    private String description = "Used to set parameters of disemmination. Parameters are optional in Dissemination and they can be single or multivalued.";
    private osid.shared.Id id = null;
    private boolean populatedByDR = false;
    private boolean mandatory = false;
    private boolean repeatable = false;
     
    /** Creates a new instance of ParameterInfoPart */
    public ParameterInfoPart() {
    }
    
    public ParameterInfoPart(osid.dr.InfoStructure infoStructure,DR dr) throws osid.dr.DigitalRepositoryException,osid.shared.SharedException {  
        this.infoStructure = infoStructure;
        this.id = new PID(FedoraUtils.getFedoraProperty(dr, "ParameterInfoPartId"));
        repeatable = true;
    }
    
    public String getDescription() throws osid.dr.DigitalRepositoryException {
        return  this.description;
    }
    
    public String getDisplayName() throws osid.dr.DigitalRepositoryException {
        return this.displayName;
    }
    
    public osid.shared.Id getId() throws osid.dr.DigitalRepositoryException {
        return this.id;
    }
    
    public osid.dr.InfoPartIterator getInfoParts() throws osid.dr.DigitalRepositoryException {
        return new InfoPartIterator(this.infoPartsVector);
    }
    
    public osid.dr.InfoStructure getInfoStructure() throws osid.dr.DigitalRepositoryException {
        return this.infoStructure;
    }
    
    public boolean isMandatory() throws osid.dr.DigitalRepositoryException  {
        return this.mandatory;
    }
    
    public boolean isPopulatedByDR() throws osid.dr.DigitalRepositoryException  {
        return this.populatedByDR;
    }
    
    public boolean isRepeatable() throws osid.dr.DigitalRepositoryException {
        return this.repeatable;
    }
    
    public boolean validateInfoField(osid.dr.InfoField infoField) throws osid.dr.DigitalRepositoryException {
        return false;
    }
    
}
