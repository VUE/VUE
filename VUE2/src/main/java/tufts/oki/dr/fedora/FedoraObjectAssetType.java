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
 * Created on October 10, 2003, 5:23 PM
 */

package tufts.oki.dr.fedora;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

/**
 *
 * @author  akumar03
 */

public class FedoraObjectAssetType extends osid.shared.Type {   
    private Vector infoStructures = new Vector();
    private DR dr;
    private String id;
    private String type ="TUFTS_STD_IMAGE";
    private osid.dr.InfoStructure disseminationInfoStructure = null;
    private osid.dr.InfoStructure sVUEInfoStructure = null;
    /** Creates a new instance of FedoraObjectAssetType */
    public FedoraObjectAssetType(DR dr,String type) throws osid.dr.DigitalRepositoryException {
        super("Fedora_Asset","tufts.edu",type);
        this.dr = dr;
        this.type = type;
        this.id = type;
        loadInfoStructures();
    }
    
    public String getType() {
        return this.type;
    }
    
    public osid.dr.InfoStructureIterator getInfoStructures()
        throws osid.dr.DigitalRepositoryException
    {
        return (osid.dr.InfoStructureIterator) new InfoStructureIterator(infoStructures);
    }
    public osid.dr.InfoStructure getDissemiationInfoStructure()  throws osid.dr.DigitalRepositoryException  {
        if(this.disseminationInfoStructure == null) 
            throw new osid.dr.DigitalRepositoryException("Dissemination InfoStructure doesn't exist");
        return this.disseminationInfoStructure;
            
    }

     public osid.dr.InfoStructure getVUEInfoStructure()  throws osid.dr.DigitalRepositoryException  {
        if(this.sVUEInfoStructure == null) 
            throw new osid.dr.DigitalRepositoryException("VUE InfoStructure doesn't exist");
        return this.sVUEInfoStructure;
            
    }

    private void loadInfoStructures() throws osid.dr.DigitalRepositoryException {
        try {
            disseminationInfoStructure = new DisseminationInfoStructure(dr);
            sVUEInfoStructure = new VUEInfoStructure(dr);
            infoStructures.add(disseminationInfoStructure);
            infoStructures.add(sVUEInfoStructure);
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraObjecAssetType.loadInfoStructure  "+ex);
        }
    }
    public String toString() {
        return getClass().getName()+" id:"+id;
    }
        
}
