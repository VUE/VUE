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
	private String assetIdString = null;
	private org.osid.shared.Type assetType = null;
    private org.osid.shared.Type mimeType =  new Type("mit.edu","asset","MIME");
	private org.osid.repository.Repository repository = null;
	private org.osid.repository.Record record = null;
	private org.osid.shared.Id recordStructureId = null;
	private java.util.Vector partIdStringVector = null;
	private java.util.Vector partValueVector = null;	
	private String key = null;	
	private String sessionId = null;
	private java.util.Vector assetVector = new java.util.Vector();
	
	public static final String LIST_TAG = "list";
	public static final String RESOURCE_TAG = "resource";
	public static final String ID_TAG = "id";
	public static final String NAME_TAG = "name";
	public static final String TYPE_TAG = "type";
	public static final String URL_TAG = "url";
	
    protected Asset(String assetIdString,
                    org.osid.shared.Type assetType,
					String key,
					String displayName,
					String url)
    {
		this.assetIdString = assetIdString;
		this.assetType = assetType;
		this.key = key;
		try {
			this.sessionId = Utilities.getSessionId(key);
			this.assetId = Utilities.getIdManager().getId(assetIdString);
			this.recordStructureId = RecordStructure.getInstance().getId();
			if (url != null) {
				org.osid.repository.Record record = createRecord(this.recordStructureId);
				record.createPart(URLPartStructure.getInstance().getId(),url);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		this.displayName = displayName;
    }
	
	protected Asset(String key,
					String xml)
	{
		try {
			this.recordStructureId = RecordStructure.getInstance().getId();
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			org.w3c.dom.Document document = db.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
			
			org.w3c.dom.NodeList nl = document.getElementsByTagName(LIST_TAG);
			org.w3c.dom.Element listElement = (org.w3c.dom.Element)nl.item(0);
			nl = document.getElementsByTagName(RESOURCE_TAG);
			int numResources = nl.getLength();
			for (int i=0; i < numResources; i++) {
				org.w3c.dom.Element resourceElement = (org.w3c.dom.Element)nl.item(i);
				String id = Utilities.expectedValue(resourceElement,ID_TAG);
				String name = Utilities.expectedValue(resourceElement,NAME_TAG);
				String type = Utilities.expectedValue(resourceElement,TYPE_TAG);
				String url = Utilities.expectedValue(resourceElement,URL_TAG);
				if (url != null) {
					org.osid.repository.Record record = createRecord(this.recordStructureId);
					record.createPart(URLPartStructure.getInstance().getId(),url);
				}
				/*
				System.out.println("Next Resource");
				System.out.println("\tId: " + id);
				System.out.println("\tName: " + name);
				System.out.println("\tType: " + type);
				System.out.println("\tURL: " + url);
				*/
				org.osid.shared.Type assetType = null;
				if (type.equals("collection")) assetType = Utilities.getCollectionAssetType();
				if (type.equals("resource")) assetType = Utilities.getResourceAssetType();
				
				this.assetIdString = id;
				this.assetType = assetType;
				this.key = key;
				this.sessionId = Utilities.getSessionId(key);
				try {
					this.assetId = Utilities.getIdManager().getId(assetIdString);
				} catch (Throwable t) {
				}
				this.displayName = name;
			}
		} catch (Throwable t) {
			t.printStackTrace();
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
		if (this.assetType.isEqual(Utilities.getCollectionAssetType())) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}
		try {
			// OBA assumes serializable is a byte array
			Service  service = new Service();
			Call call = (Call) service.createCall();
			call = (Call) service.createCall();
			String endpoint = Utilities.getEndpoint();
			String address = Utilities.getAddress();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "getContentData"));
			String result = (String) call.invoke( new Object[] {sessionId, this.assetIdString} );
			
			SakaiContentObject obj = new SakaiContent();
			obj.setDisplayName(getDisplayName());
			obj.setDescription(getDescription());
			if (getAssetType().isEqual(this.mimeType)) {
				obj.setMIMEType(getAssetType().getKeyword());
			}
			obj.setBytes(org.apache.axis.encoding.Base64.decode(result));
			return obj;
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
	}	
    
	public void updateContent(java.io.Serializable content)
    throws org.osid.repository.RepositoryException
    {
		if (content == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		if (!(this.assetType.isEqual(Utilities.getCollectionAssetType()))) {
			System.out.println("Not a collection type " + Utilities.typeToString(this.assetType));
			throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}
		if (!(content instanceof SakaiContentObject)) {
			System.out.println("Not a Sakai Content Object");
			throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}

		try {
			// OBA assumes serializable is a SakaiContentUploadObject
			SakaiContentObject upload = (SakaiContentObject)content;
			String name = upload.getDisplayName();
			String description = upload.getDescription();
			String type = upload.getMIMEType();
			byte[] data = upload.getBytes();
			String encodedContent = Base64.encode(data);
			
			Service  service = new Service();
			Call call = (Call) service.createCall();
			String endpoint = Utilities.getEndpoint();
			String address = Utilities.getAddress();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "createContentItem"));
			String result = (String) call.invoke( new Object[] {sessionId, name, this.assetIdString, encodedContent, description, type, new Boolean(true)} );
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
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
		// only site collection assets can have sub-assets
		if (this.assetType.isEqual(Utilities.getCollectionAssetType())) {

			try {
				String endpoint = Utilities.getEndpoint();
				//System.out.println("Endpoint " + endpoint);
				String address = Utilities.getAddress();
				//System.out.println("Address " + address);
				
				Service  service = new Service();
				
				//	Get the list of root collections from virtual root.
				Call call = (Call) service.createCall();
				call = (Call) service.createCall();
				call.setTargetEndpointAddress (new java.net.URL(endpoint) );
				call.setOperationName(new QName(address, "getResources"));
				String siteString = (String) call.invoke( new Object[] {sessionId, assetIdString} );
				//System.out.println("Sent ContentHosting.getAllResources(sessionId,collectionId), got '" + siteString + "'");
				
				return new AssetIterator(siteString,this.key,siteString);			
			} catch (Throwable t) {
				Utilities.log(t);
				throw new org.osid.repository.RepositoryException(t.getMessage());
			}
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
		if (!this.assetType.isEqual(Utilities.getCollectionAssetType())) {
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
