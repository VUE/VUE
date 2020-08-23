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
 * FedoraObjectAssetType.java
 *
 * Created on April 7, 2006, 5:23 PM
 */
package  edu.tufts.osidimpl.repository.fedora_2_2;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

/**
 *
 * @author  akumar03
 */

public class FedoraObjectAssetType extends org.osid.shared.Type {
    private Vector recordStructures = new Vector();
    private Repository repository;
    private String id;
    private String type ="tufts/image/archival";
    private org.osid.repository.RecordStructure disseminationRecordStructure = null;
    private org.osid.repository.RecordStructure sImageRecordStructure = null;
    private org.osid.repository.RecordStructure sDefaultRecordStructure = null;
    
    /** Creates a new instance of FedoraObjectAssetType */
    public FedoraObjectAssetType(Repository repository,String type) throws org.osid.repository.RepositoryException {
        super("tufts.edu","asset",type);
        this.repository = repository;
        this.type = type;
        this.id = type;
        loadRecordStructures();
    }
    
    public String getType() {
        return this.type;
    }
    
    public org.osid.repository.RecordStructureIterator getRecordStructures()
    throws org.osid.repository.RepositoryException {
        return (org.osid.repository.RecordStructureIterator) new RecordStructureIterator(recordStructures);
    }
    
    public org.osid.repository.RecordStructure getDissemiationRecordStructure()  throws org.osid.repository.RepositoryException  {
        if(this.disseminationRecordStructure == null)
            throw new org.osid.repository.RepositoryException("Dissemination RecordStructure doesn't exist");
        return this.disseminationRecordStructure;
        
    }
    
    public org.osid.repository.RecordStructure getImageRecordStructure()  throws org.osid.repository.RepositoryException  {
        if(this.sImageRecordStructure == null)
            throw new org.osid.repository.RepositoryException("Image RecordStructure doesn't exist");
        return this.sImageRecordStructure;
        
    }
    public org.osid.repository.RecordStructure getDefaultRecordStructure()  throws org.osid.repository.RepositoryException  {
        if(this.sDefaultRecordStructure == null)
            throw new org.osid.repository.RepositoryException("Default RecordStructure doesn't exist");
        return this.sDefaultRecordStructure;
        
    }
    private void loadRecordStructures() throws org.osid.repository.RepositoryException {
        try {
            disseminationRecordStructure = new DisseminationRecordStructure(repository);
            sImageRecordStructure = new ImageRecordStructure(repository);
            sDefaultRecordStructure = new DefaultRecordStructure(repository);
            recordStructures.add(disseminationRecordStructure);
            recordStructures.add(sImageRecordStructure);
             recordStructures.add(sDefaultRecordStructure);
        }catch(Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraObjecAssetType.loadRecordStructure  "+ex);
        }
    }
    public String toString() {
        return getClass().getName()+" id:"+id;
    }
    
}
