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
 * OsidAssetResource.java
 *
 * Created on March 24, 2004, 11:21 AM
 */

package tufts.vue;

/** A wrapper for an implementation of the Repository OSID.  A osid.dr.Asset which can be used as the user 
 *  object in a DefaultMutableTreeNode.  It implements the Resource interface specification.
 */

public class OsidAssetResource extends MapResource
{
    public static final String VUE_INTEGRATION_RECORD = "VUE_Integration_Record";
    private osid.shared.SharedManager sharedManager = null;
    private osid.OsidOwner owner = null;
    private osid.OsidContext context = null;
    private osid.repository.Asset asset20 = null;
    private osid.dr.Asset asset10 = null;

//    private osid.dr.Asset asset;
//    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.

    public OsidAssetResource(osid.repository.Asset asset, osid.OsidContext context) throws osid.repository.RepositoryException 
    {
        super();
        try
        {
            this.owner = owner;
            this.asset20 = asset;
            setAsset(asset20);
        }
        catch (Throwable t)
        {
            System.out.println(t.getMessage());
        }
    }
    
    public OsidAssetResource(osid.dr.Asset asset, osid.OsidOwner owner) throws osid.dr.DigitalRepositoryException 
    {
        super();
        try
        {
            this.owner = owner;
            this.asset10 = asset;
            setAsset(asset10);
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

    public void setAsset(osid.repository.Asset asset) throws osid.repository.RepositoryException 
    {
        this.asset20 = asset;
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
                osid.repository.Record record = asset.getRecord(sharedManager.getId(VUE_INTEGRATION_RECORD));
                // replace the above with integration by type, if added to OSID 2.0
                if (record != null)
                {
                    osid.repository.PartIterator partIterator = record.getParts();
                    while (partIterator.hasNextPart())
                    {
                        osid.repository.Part part = partIterator.nextPart();
                        osid.repository.PartStructure partStructure = part.getPartStructure();
                        String dname = partStructure.getDisplayName();
                        mProperties.put(part.getPartStructure().getDisplayName(),part.getValue());
                    }
                }
            }
            catch (Exception ex) 
            {
                setSpec((String)asset.getContent());            
                System.out.println("No VUE integration record.  Fetching Asset's content " + getSpec());
            }
            Object o = mProperties.get("spec");
            if (getSpec() == null)
            {
                setSpec( (o != null) ? (String)o : asset.getDisplayName() );
            }
        }
        catch (Exception ex)
        {
            setSpec(asset.getDisplayName());
        }
    }

    public void setAsset(osid.dr.Asset asset) throws osid.dr.DigitalRepositoryException 
    {
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
                osid.dr.InfoRecord record = asset10.getInfoRecord(sharedManager.getId(VUE_INTEGRATION_RECORD));
                // replace the above with integration by type, if added to OSID 2.0
                if (record != null)
                {
                    osid.dr.InfoFieldIterator partIterator = record.getInfoFields();
                    while (partIterator.hasNext())
                    {
                        osid.dr.InfoField part = partIterator.next();
                        osid.dr.InfoPart partStructure = part.getInfoPart();
                        String dname = partStructure.getDisplayName();
                        mProperties.put(part.getInfoPart().getDisplayName(),part.getValue());
                    }
                }
            }
            catch (Exception ex) 
            {
                setSpec( (String)asset10.getContent() );            
                System.out.println("No VUE integration record.  Fetching Asset's content " + getSpec());
            }
            Object o = mProperties.get("spec");
            if (getSpec() == null)
            {
                setSpec( (o != null) ? (String)o : asset10.getDisplayName() );
            }
        }
        catch (Exception ex)
        {
            setSpec(asset10.getDisplayName());
        }
    }

/*
    public osid.repository.Asset getAsset() 
    {
        return this.asset20;
    }    
*/
    public osid.dr.Asset getAsset() 
    {
        return this.asset10;
    }    
}