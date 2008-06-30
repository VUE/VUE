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

/*
 * AssetResource.java
 *
 * Created on January 23, 2004, 11:21 AM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import osid.dr.*;
import tufts.oki.dr.fedora.*;
import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import fedora.server.types.gen.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

/** *  A wrapper for Fedora Object implementation of osid.dr.Asset which can be used as the user object in a 
 *  DefaultMutableTreeNode.  It implements the Resource interface specification.
 */

public class AssetResource extends URLResource {
    
    /** Creates a new instance of AssetResource */
    public static final int DC_NAMESPACE_LENGTH = 3 ;// (dc:) the namespacepresent in metadata fields. Beginning is chopped off for clean rendering
    public static final String DISSEMINATION_ID = "getDublinCore";
    public static final String DC_FIELD_ID = "fedora.disseminationURL";
    public static final String DISSEMINATION_INFOSTRUCTURE_ID = "fedora.dissemination";
    public static final String VUE_INFOSTRUCTURE_ID = "fedora.vue";
    public static final String VUE_DEFAULT_VIEW_ID = "fedora.vue.defaultView";
    public static final String[] dcFields = tufts.oki.dr.fedora.DR.DC_FIELDS;
    public static final String  DC_NAMESPACE = tufts.oki.dr.fedora.DR.DC_NAMESPACE;
    
    private Asset asset;
    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
    public AssetResource() {
        super();
        setClientType(Resource.ASSET_FEDORA);
    }
    public AssetResource(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this();
        setAsset(asset);
    }
    
    public void setAsset(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.asset = asset;
        setPropertiesByAsset();
        setSpec(asset.getInfoField(new tufts.oki.dr.fedora.PID(VUE_DEFAULT_VIEW_ID)).getValue().toString());
        setClientType(Resource.ASSET_FEDORA);
        setTitle(asset.getDisplayName());
        this.castorFedoraObject = new CastorFedoraObject((FedoraObject)asset);
    }
    
    public Asset getAsset() {
        return this.asset;
    }
    private void setPropertiesByAsset() {
        
        try {
            if(!(asset.getAssetType().getKeyword().equals("fedora:BDEF") || asset.getAssetType().getKeyword().equals("fedora:BMECH"))){
                
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                //InputStream dublinCoreInputStream = new ByteArrayInputStream(((MIMETypedStream)(asset.getInfoField(new PID("getDublinCore")).getValue())).getStream());
                URL dcUrl  = getDCUrl();
                if(dcUrl !=null) {
                    InputStream dublinCoreInputStream = dcUrl.openStream();
                    Document document = factory.newDocumentBuilder().parse(dublinCoreInputStream);
                    for(int i=0;i<dcFields.length;i++) {
                        NodeList list = document.getElementsByTagName(DC_NAMESPACE+dcFields[i]);
                        if(list != null && list.getLength() != 0) {
                            // only picks the first element
                            if(list.item(0).getFirstChild() != null)
                                setProperty(dcFields[i], list.item(0).getFirstChild().getNodeValue());
                        }
                    }
                }
            }
        } catch (Exception ex)  {ex.printStackTrace();}
        
    }
    
    private URL getDCUrl() {
        URL url = null;
        try {
            boolean flag = true;
            osid.dr.InfoFieldIterator i = asset.getInfoRecord(new PID(DISSEMINATION_ID)).getInfoFields();
            while(i.hasNext() && flag) {
                osid.dr.InfoField infoField = i.next();
                if(infoField.getInfoPart().getId().getIdString().equals(DC_FIELD_ID))  {
                    url = new URL(infoField.getValue().toString());
                }
            }
        } catch (Exception ex)  {ex.printStackTrace();}
        return url;
    }
    
    public void setCastorFedoraObject(CastorFedoraObject castorFedoraObject) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.castorFedoraObject = castorFedoraObject;
        this.asset = this.castorFedoraObject.getFedoraObject();
        setSpec(asset.getInfoField(new tufts.oki.dr.fedora.PID(VUE_DEFAULT_VIEW_ID)).getValue().toString());
        setClientType(Resource.ASSET_FEDORA);
        setPropertiesByAsset();
    }
    
    public CastorFedoraObject getCastorFedoraObject() {
        return this.castorFedoraObject;
    }
    
    //todo:menu will be generated in AssetResource.
    public static AbstractAction getFedoraAction(osid.dr.InfoRecord infoRecord,osid.dr.DigitalRepository dr) throws osid.dr.DigitalRepositoryException {
        final DR mDR = (DR)dr;
        final tufts.oki.dr.fedora.InfoRecord mInfoRecord = (tufts.oki.dr.fedora.InfoRecord)infoRecord;
        
        try {
            AbstractAction fedoraAction = new AbstractAction(infoRecord.getId().getIdString()) {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    try {
                        //String fedoraUrl = mDR.getFedoraProperties().getProperty("url.fedora.get","http://vue-dl.tccs..tufts.edu:8080/fedora/get");
                        String fedoraUrl = mInfoRecord.getInfoField(new PID(FedoraUtils.getFedoraProperty(mDR, "DisseminationURLInfoPartId"))).getValue().toString();
                        URL url = new URL(fedoraUrl);
                        URLConnection connection = url.openConnection();
                        System.out.println("FEDORA ACTION: Content-type:"+connection.getContentType()+" for url :"+fedoraUrl);
                        
                        VueUtil.openURL(fedoraUrl);
                    } catch(Exception ex) {  }
                }
                
            };
            return fedoraAction;
        } catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("FedoraUtils.getFedoraAction "+ex.getMessage());
        }
    }
}
