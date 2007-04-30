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
package edu.tufts.osidimpl.repository.sakai;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import javax.xml.namespace.QName;
import org.apache.axis.encoding.Base64;

public class Asset
implements org.osid.repository.Asset
{
    private String displayName = null;
    private String description = null;
    private org.osid.shared.Id assetId = null;
	private org.osid.shared.Type assetType = null;
    private org.osid.shared.Type collectionAssetType = new Type("org.sakaiproject","asset","siteCollection");
    private org.osid.shared.Type resourceAssetType =  new Type("org.sakaiproject","asset","resource");
    private org.osid.shared.Type uploadAssetType =  new Type("org.sakaiproject","asset","upload");
	private org.osid.repository.Repository repository = null;
	private org.osid.repository.Record record = null;
	private org.osid.shared.Id recordStructureId = null;
	private java.util.Vector partIdStringVector = null;
	private java.util.Vector partValueVector = null;	
	private String key = null;	
	private java.util.Vector assetVector = new java.util.Vector();
	
    protected Asset(String contentId,
                    org.osid.shared.Type assetType,
					org.osid.shared.Id repositoryId,
					org.osid.repository.Repository repository,
					String key)
    throws org.osid.repository.RepositoryException
    {
		this.assetType = assetType;
		this.repository = repository;
		this.key = key;
		try {
			this.assetId = Utilities.getIdManager().getId(contentId);
		} catch (Throwable t) {
		}
    }
	
	public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        if (displayName == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public String getDescription()
    throws org.osid.repository.RepositoryException
    {
        return this.description;
    }

    public void updateDescription(String description)
    throws org.osid.repository.RepositoryException
    {
        if (description == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.assetId;
    }

    public org.osid.shared.Id getRepository()
    throws org.osid.repository.RepositoryException
    {
        return Utilities.getRepositoryId();
    }

    public java.io.Serializable getContent()
    throws org.osid.repository.RepositoryException
    {
		if (this.assetType.isEqual(this.collectionAssetType)) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/*
		This operation is only supported for ????; otherwise UNKNOWN_TYPE is thrown.
		The content must be able to be cast as a java.io.File
	 */
    
	public void updateContent(java.io.Serializable content)
    throws org.osid.repository.RepositoryException
    {
		if (content == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
/*		if (this.assetType.isEqual(this.categoryAssetType)) {  */
	}

    public void addAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void removeAsset(org.osid.shared.Id assetId
                          , boolean includeChildren)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.AssetIterator getAssets()
    throws org.osid.repository.RepositoryException
    {
		java.util.Vector result = new java.util.Vector();

		// only site collection assets can have sub-assets
		if (!this.assetType.isEqual(this.collectionAssetType)) {
			return new AssetIterator(result);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
	}

    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        if (assetType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		if (!this.assetType.isEqual(this.collectionAssetType)) {
			throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }
		return getAssets();
    }

    public org.osid.repository.Record createRecord(org.osid.shared.Id recordStructureId)
    throws org.osid.repository.RepositoryException
    {
        if (recordStructureId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		try {
			if (recordStructureId.isEqual(this.recordStructureId)) {
				this.record = new Record();
				return this.record;
			}
		} catch (Throwable t) {
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }

    public void inheritRecordStructure(org.osid.shared.Id assetId
                                     , org.osid.shared.Id recordStructureId)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void copyRecordStructure(org.osid.shared.Id assetId
                                  , org.osid.shared.Id recordStructureId)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void deleteRecord(org.osid.shared.Id recordId)
    throws org.osid.repository.RepositoryException
    {
        if (recordId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.RecordIterator getRecords()
    throws org.osid.repository.RepositoryException
    {
		java.util.Vector result = new java.util.Vector();
		result.addElement(this.record);
        return new RecordIterator(result);
    }

    public org.osid.repository.RecordIterator getRecordsByRecordStructure(org.osid.shared.Id recordStructureId)
    throws org.osid.repository.RepositoryException
    {
        if (recordStructureId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		try {
			if (recordStructureId.isEqual(this.recordStructureId)) {
				return getRecords();
			}
		} catch (Throwable t) {
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
	}

    public org.osid.shared.Type getAssetType()
    throws org.osid.repository.RepositoryException
    {
        return this.assetType;
    }

    public org.osid.repository.RecordStructureIterator getRecordStructures()
    throws org.osid.repository.RepositoryException
    {
        return new RecordStructureIterator(new java.util.Vector());
    }

    public org.osid.repository.RecordStructure getContentRecordStructure()
    throws org.osid.repository.RepositoryException
    {
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.Record getRecord(org.osid.shared.Id recordId)
    throws org.osid.repository.RepositoryException
    {
        if (recordId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.Part getPart(org.osid.shared.Id partId)
    throws org.osid.repository.RepositoryException
    {
        if (partId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public java.io.Serializable getPartValue(org.osid.shared.Id partId)
    throws org.osid.repository.RepositoryException
    {
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.PartIterator getPartByPart(org.osid.shared.Id partStructureId)
    throws org.osid.repository.RepositoryException
    {
        if (partStructureId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		return new PartIterator(new java.util.Vector());    
    }

    public org.osid.shared.ObjectIterator getPartValueByPart(org.osid.shared.Id partStructureId)
    throws org.osid.repository.RepositoryException
    {
		try {
			return new ObjectIterator(new java.util.Vector());
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
    }

    public long getEffectiveDate()
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void updateEffectiveDate(long effectiveDate)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public long getExpirationDate()
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void updateExpirationDate(long expirationDate)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.ObjectIterator getPartValuesByPartStructure(org.osid.shared.Id partStructureId)
    throws org.osid.repository.RepositoryException
    {
        if (partStructureId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		try {
			return new ObjectIterator(new java.util.Vector());
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
    }

    public org.osid.repository.PartIterator getPartsByPartStructure(org.osid.shared.Id partStructureId)
    throws org.osid.repository.RepositoryException
    {
        if (partStructureId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		return new PartIterator(new java.util.Vector());            
    }

    public org.osid.repository.RecordIterator getRecordsByRecordStructureType(org.osid.shared.Type recordStructureType)
    throws org.osid.repository.RepositoryException
    {
        if (recordStructureType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        return new RecordIterator(new java.util.Vector());
    }
}
