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

public class Osid2AssetResource extends MapResource
{
    private osid.OsidOwner owner = null;
    private org.osid.OsidContext context = null;
    private org.osid.repository.Asset asset = null;
	private org.osid.shared.Type thumbnailPartType1 = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
	private org.osid.shared.Type urlPartType1 = new edu.tufts.vue.util.Type("mit.edu","partStructure","URL");
	private String icon = null;
	
	//    private osid.dr.Asset asset;
	//    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
	
	// default constructor needed for Castor
	public Osid2AssetResource()
	{
	}
	
	public String getLoadString()
	{
		return getTitle();
	}
	
	public void setLoadString()
	{
	}
	
    public Osid2AssetResource(org.osid.repository.Asset asset, org.osid.OsidContext context) throws org.osid.repository.RepositoryException 
    {
        super();
        try {
            this.context = context;
            this.asset = asset;
            getProperties().holdChanges();
            setAsset(asset);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        } finally {
            getProperties().releaseChanges();
        }
    }
    
    /**
        The Resoource Title maps to the Asset DisplayName.
	 The Resource Spec maps to the value in an info field with a published Id.  This should be changed
	 to a field with a published name and a published InfoStructure Type after the OSID changes.
	 */
	
    public void setAsset(org.osid.repository.Asset asset) throws org.osid.repository.RepositoryException 
    {
        this.asset = asset;
        try {
            java.util.Properties osid_registry_properties = new java.util.Properties();
			
            setType(Resource.ASSET_OKIREPOSITORY);
            String displayName = asset.getDisplayName();
            setTitle(displayName);
            setProperty("title", displayName);
			org.osid.repository.RecordIterator recordIterator = asset.getRecords();
			while (recordIterator.hasNextRecord()) {
				org.osid.repository.PartIterator partIterator = recordIterator.nextRecord().getParts();
				while (partIterator.hasNextPart()) {
					org.osid.repository.Part part = partIterator.nextPart();
					org.osid.repository.PartStructure partStructure = part.getPartStructure();
					org.osid.shared.Type partStructureType = partStructure.getType();
					java.io.Serializable ser = part.getValue();
					
					// metadata discovery
					addProperty(partStructureType.getKeyword(),ser);
					if (partStructureType.isEqual(this.urlPartType1)) {
						String s = (String)part.getValue();
						setSpec(s);
						//setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon(new java.net.URL(s))));
						this.icon = s;
					}
					
					// preview should be a URL or an image
					if (partStructureType.isEqual(this.thumbnailPartType1)) {
						if (ser instanceof String) {
							//setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon(new java.net.URL((String)ser))));
							this.icon = (String)ser;
						} else {
							//setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon((java.awt.Image)ser)));
							//this.icon = new javax.swing.ImageIcon((java.awt.Image)ser);
						}
					}
				}
			}
		}
		catch (Throwable t) 
		{
			t.printStackTrace();
		}
		
		if ((getSpec() == null) || (getSpec().trim().length() == 0)) {
			setSpec( asset.getDisplayName() );
		}
	}

    public org.osid.repository.Asset getAsset() 
    {
        return this.asset;
    }    
	
	public String getImageIcon()
	{
		return this.icon;
	}	
}