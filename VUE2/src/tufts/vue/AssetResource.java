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
public class AssetResource extends MapResource{
    
    /** Creates a new instance of AssetResource */
    static final int DC_NAMESPACE_LENGTH = 3 ;// (dc:) the namespacepresent in metadata fields. Beginning is chopped off for clean rendering
    static final String DISSEMINATION_ID = "getDublinCore";
    static final String DC_FIELD_ID = "fedora.disseminationURL";
    static final String[] dcFields = tufts.oki.dr.fedora.DR.DC_FIELDS;
    static final String  DC_NAMESPACE = tufts.oki.dr.fedora.DR.DC_NAMESPACE;
    
    private Asset asset;
    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
    public AssetResource() {
        super();
        setType(Resource.ASSET_FEDORA);
    }
    public AssetResource(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this();
        setAsset(asset);
    }
    
    public void setAsset(Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException {
        this.asset = asset;
        setPropertiesByAsset();
        this.spec = ((FedoraObject)asset).getDefaultViewURL();
        setTitle(((FedoraObject)asset).getDisplayName());
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
                                mProperties.put(dcFields[i], list.item(0).getFirstChild().getNodeValue());
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
        this.spec =  ((FedoraObject)this.castorFedoraObject.getFedoraObject()).getDefaultViewURL();
    }
    
    public CastorFedoraObject getCastorFedoraObject() {
        return this.castorFedoraObject;
    }
    
}
