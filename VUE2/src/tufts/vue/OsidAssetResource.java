/*
 * OsidAssetResource.java
 *
 * Created on March 24, 2004, 11:21 AM
 */

package tufts.vue;

/**
 *
 * @author  jkahn
 */

/*
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
*/

/** A wrapper for an implementation of the Repository OSID.  A osid.dr.Asset which can be used as the user 
 *  object in a DefaultMutableTreeNode.  It implements the Resource interface specification.
 */

public class OsidAssetResource extends MapResource
{
    public static final String VUE_INTEGRATION_RECORD = "VUE_Integration_Record";
    private osid.shared.SharedManager sharedManager = null;
    private osid.OsidOwner owner = null;
    private osid.dr.Asset asset = null;

//    private osid.dr.Asset asset;
//    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.

    public OsidAssetResource(osid.dr.Asset asset, osid.OsidOwner owner) throws osid.dr.DigitalRepositoryException 
    {
        super();
        try
        {
            this.owner = owner;
            this.asset = asset;
            setAsset(asset);
        }
        catch (Throwable t)
        {
            System.out.println(t.getMessage());
        }
    }
    
    /**
        The Resoource Title maps to the Asset DisplayName.
        The Resource Spec maps to the value in an info field with a published Id.  This should be changed
        to a field with a published name and a published InfoStructure Type after the OSID changes.
    */

    public void setAsset(osid.dr.Asset asset) throws osid.dr.DigitalRepositoryException,osid.OsidException 
    {
        this.asset = asset;
        try
        {
            java.util.Properties osid_registry_properties = new java.util.Properties();
            osid_registry_properties.load(new java.io.FileInputStream("osid_registry.properties"));
            String sharedImplementation = osid_registry_properties.getProperty("Shared_Implementation");
            this.sharedManager = (osid.shared.SharedManager)osid.OsidLoader.getManager(
                "osid.shared.SharedManager",
                sharedImplementation,
                this.owner);

            setType(Resource.ASSET_OKIDR);
            String displayName = asset.getDisplayName();
            setTitle(displayName);
            mProperties.put("title",displayName);
            try
            {
                osid.dr.InfoRecord infoRecord = asset.getInfoRecord(sharedManager.getId(VUE_INTEGRATION_RECORD));
                if (infoRecord != null)
                {
                    osid.dr.InfoFieldIterator infoFieldIterator = infoRecord.getInfoFields();
                    while (infoFieldIterator.hasNext())
                    {
                        osid.dr.InfoField infoField = infoFieldIterator.next();
                        mProperties.put(infoField.getInfoPart().getDisplayName(),infoField.getValue());
                    }
                }
            }
            catch (Exception ex) 
            {
                this.spec = (String)asset.getContent();            
                System.out.println("No VUE integration record.  Fetching Asset's content " + this.spec);
            }
            Object o = mProperties.get("spec");
            if (this.spec == null)
            {
                this.spec = (o != null) ? (String)o : asset.getDisplayName();
            }
        }
        catch (Exception ex)
        {
            this.spec = asset.getDisplayName();
        }
    }

    public osid.dr.Asset getAsset() 
    {
        return this.asset;
    }
    
/*
    public void setCastorFedoraObject(CastorFedoraObject castorFedoraObject) throws osid.dr.DigitalRepositoryException,osid.OsidException 
    {
        this.castorFedoraObject = castorFedoraObject;
        this.asset = this.castorFedoraObject.getFedoraObject();
        this.spec =   asset.getInfoField(new tufts.oki.dr.fedora.PID(VUE_DEFAULT_VIEW_ID)).getValue().toString();
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
*/
}