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

public class AssetResource extends MapResource {
    
    /** Creates a new instance of AssetResource */
    static final String[] dcFields = tufts.oki.dr.fedora.DR.DC_FIELDS;
    
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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            InputStream dublinCoreInputStream = new ByteArrayInputStream(((MIMETypedStream)(asset.getInfoField(new PID("getDublinCore")).getValue())).getStream());
            Document document = factory.newDocumentBuilder().parse(dublinCoreInputStream);
            for(int i=0;i<dcFields.length;i++) {
                NodeList list = document.getElementsByTagName(dcFields[i]);
                if(list != null && list.getLength() != 0) {
                     // only picks the first element 
                    if(list.item(0).getFirstChild() != null) 
                        mProperties.put(dcFields[i], list.item(0).getFirstChild().getNodeValue());
                }
             }
    
        } catch (Exception ex)  {ex.printStackTrace();}
        
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
