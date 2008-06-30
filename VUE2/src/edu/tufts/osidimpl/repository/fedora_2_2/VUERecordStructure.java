/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
package  edu.tufts.osidimpl.repository.fedora_2_2;

import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.io.*;
import java.util.*;

public class VUERecordStructure   implements org.osid.repository.RecordStructure {
    public static final String[] dcFields = {"title","creator","subject","date","type","format","identifier","collection","coverage"};
    public static final String  DC_NAMESPACE = "dc:";
    
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "VUE Specific Data";
    private String description = "Provides information to be used by VUE";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("tufts.edu","recordStructure","vue");
    private org.osid.repository.PartStructure sVUEDefaultViewPartStructure = null;
    
    protected VUERecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException {
        try {
            this.id = new PID("VUEInfoStructureId");
        } catch (org.osid.shared.SharedException sex) {
        }
        this.sVUEDefaultViewPartStructure = new URLPartStructure(this, repository);
        this.partsVector.add(this.sVUEDefaultViewPartStructure);
        for(int i=0;i<Repository.DC_FIELDS.length;i++) {
            org.osid.repository.PartStructure metadataElementPartStructure = new MetadataElementPartStructure(Repository.DC_FIELDS[i], this,repository,new Type("tufts.edu","partStructure",Repository.DC_FIELDS[i]));
            this.partsVector.add(metadataElementPartStructure);
        }
    }
    
    public String getDisplayName()
    throws org.osid.repository.RepositoryException {
        return this.displayName;
    }
    
    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public String getDescription()
    throws org.osid.repository.RepositoryException {
        return this.description;
    }
    
    public String getFormat()
    throws org.osid.repository.RepositoryException {
        return this.format;
    }
    
    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException {
        return this.id;
    }
    
    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException {
        return new PartStructureIterator(this.partsVector);
    }
    
    public String getSchema()
    throws org.osid.repository.RepositoryException {
        return this.schema;
    }
    
    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException {
        return this.type;
    }
    
    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException {
        return true;
    }
    
    public org.osid.repository.PartStructure getVUEDefaultViewPartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sVUEDefaultViewPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sVUEDefaultViewPartStructure;
    }
    
    public org.osid.repository.PartStructure getMetadataElementPartStructure(String element)
    throws org.osid.repository.RepositoryException {
        Iterator i = partsVector.iterator();
        try {
            while(i.hasNext()){
                org.osid.repository.PartStructure partStructure = (org.osid.repository.PartStructure)i.next();
                if(element.equals(partStructure.getId().getIdString()))
                    return partStructure;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
    }
    
    public static Record createVUERecord(String pid
            , VUERecordStructure recordStructure
            , Repository repository
            , PID objectId
            , FedoraObjectAssetType assetType)
            throws org.osid.repository.RepositoryException {
        Record record = null;
        try {
            record = new Record(new PID(pid),recordStructure);
            if(assetType.getKeyword().equals(repository.getFedoraProperties().getProperty("type.image"))) {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                        Utilities.formatObjectUrl(objectId.getIdString(),repository.getFedoraProperties().getProperty("assetDef.fullView"),repository));
            } else {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                        Utilities.formatObjectUrl(objectId.getIdString(),repository.getFedoraProperties().getProperty("assetDef.fullView"),repository));
            }
            if(!(assetType.getKeyword().equals(Repository.BDEF) || assetType.getKeyword().equals(Repository.BMECH))){
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                String url = Utilities.formatObjectUrl(objectId.getIdString(),repository.getFedoraProperties().getProperty("dissemination.dc"),repository);
                //System.out.println("DC URL:"+ url);
                URL dcUrl  = new URL(url);
                if(dcUrl !=null) {
                    InputStream dublinCoreInputStream = dcUrl.openStream();
                    Document document = factory.newDocumentBuilder().parse(dublinCoreInputStream);
                    for(int i=0;i<Repository.DC_FIELDS.length;i++) {
                        NodeList list = document.getElementsByTagName(Repository.DC_NAMESPACE+dcFields[i]);
                        if(list != null && list.getLength() != 0) {
                            // only picks the first element
                            if(list.item(0).getFirstChild() != null)
                                record.createPart(recordStructure.getMetadataElementPartStructure(Repository.DC_FIELDS[i]).getId(), list.item(0).getFirstChild().getNodeValue());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return record;
    }
    

    
    
}
