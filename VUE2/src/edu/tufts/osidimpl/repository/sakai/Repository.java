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

public class Repository
implements org.osid.repository.Repository
{
    private java.util.Vector assetTypeVector = new java.util.Vector();
    private java.util.Vector searchTypeVector = new java.util.Vector();
	private org.osid.shared.Id repositoryId = null;
	private String repositoryIdPrefixString = "E89F7F92-8C23-481B-AF8C-7AE169699F34-2595-000008D778AFEB53";
	private org.osid.shared.Type repositoryType = new Type("sakaiproject.org","repository","contentHosting");
	private org.osid.shared.Type keywordSearchType = new Type("mit.edu","search","keyword");
	private org.osid.shared.Type titleSearchType = new Type("mit.edu","search","title");
	private String displayName = null;
	private String key = null;
	private String sessionId = null;
	private RepositoryManager manager = null;
 
    protected Repository(String displayName, String key, RepositoryManager manager)
		    throws org.osid.repository.RepositoryException
    {
		this.key = key;
		this.sessionId = Utilities.getSessionId(key);
		this.displayName = displayName;
		this.searchTypeVector.addElement(this.keywordSearchType);
		this.searchTypeVector.addElement(this.titleSearchType);		
		this.assetTypeVector.addElement(Utilities.getCollectionAssetType());
		this.assetTypeVector.addElement(Utilities.getResourceAssetType());
		this.repositoryId = Utilities.getRepositoryId();
		this.manager = manager;
/*		try {
			System.out.println("Repository id is " + this.repositoryId.getIdString());
		} catch (Throwable t) {
		}
*/			
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
        return this.displayName;
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
		return this.repositoryId;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return this.repositoryType;
    }

    public org.osid.repository.Asset createAsset(String displayName
                                               , String description
                                               , org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        if ( (displayName == null ) || (description == null) || (assetType == null) )
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void deleteAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.AssetIterator getAssets()
    throws org.osid.repository.RepositoryException
    {
		java.util.Vector result = new java.util.Vector();
		this.key = this.manager.pingForValidSession();
		this.sessionId = Utilities.getSessionId(this.key);
		
		try {
			String endpoint = Utilities.getEndpoint();
			//System.out.println("Endpoint " + endpoint);
			String address = Utilities.getAddress();
			//System.out.println("Address " + address);
			
			Service  service = new Service();
			
			//	Get the virtual root.
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "getVirtualRoot"));
			String virtualRootId = (String) call.invoke( new Object[] {sessionId} );
			//System.out.println("Sent ContentHosting.getVirtualRoot(sessionId), got '" + virtualRootId + "'");
			
			//	Get the list of root collections from virtual root.
			call = (Call) service.createCall();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "getResources"));
			String siteString = (String) call.invoke( new Object[] {sessionId, virtualRootId} );
			//System.out.println("Sent ContentHosting.getAllResources(sessionId,virtualRootId), got '" + siteString + "'");

			return new AssetIterator(siteString,this.key,siteString);			
		} catch (Throwable t) {
			Utilities.log(t);
			throw new org.osid.repository.RepositoryException(t.getMessage());
		}
    }

    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        if (assetType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		if ( !(assetType.isEqual(Utilities.getCollectionAssetType()) ||
			   assetType.isEqual(Utilities.getResourceAssetType())) )
        {
			throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
        }

		java.util.Vector result = new java.util.Vector();
		try {
			org.osid.repository.AssetIterator ai = getAssets();
			while (ai.hasNextAsset()) {
				org.osid.repository.Asset a = ai.nextAsset();
				//System.out.println("getting asset ... " + a.getDisplayName());
				if (a.getAssetType().isEqual(assetType)) {
					result.addElement(a);
				}
			}
		} catch (Throwable t) {
			Utilities.log(t);
			throw new org.osid.repository.RepositoryException(t.getMessage());
		}
		return new AssetIterator(result);
	}
		
    public org.osid.shared.TypeIterator getAssetTypes()
    throws org.osid.repository.RepositoryException
    {
        try
        {
            return new TypeIterator(this.assetTypeVector);
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.repository.RecordStructureIterator getRecordStructures()
    throws org.osid.repository.RepositoryException
    {
        return new RecordStructureIterator(new java.util.Vector());
    }

    public org.osid.repository.RecordStructureIterator getMandatoryRecordStructures(org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        if (assetType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
//		if (assetType.isEqual(this.categoryAssetType))
        {
            return new RecordStructureIterator(new java.util.Vector());
        }
//        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
    }

    public org.osid.shared.TypeIterator getSearchTypes()
    throws org.osid.repository.RepositoryException
    {
        try
        {
            return new TypeIterator(this.searchTypeVector);
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.shared.TypeIterator getStatusTypes()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
        try
        {
            results.addElement(new Type("mit.edu","asset","valid"));
            return new TypeIterator(results);
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.shared.Type getStatus(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        return new Type("mit.edu","asset","valid");
    }

    public boolean validateAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public void invalidateAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		try {
			String endpoint = Utilities.getEndpoint();
			//System.out.println("Endpoint " + endpoint);
			String address = Utilities.getAddress();
			//System.out.println("Address " + address);
			
			Service  service = new Service();
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress (new java.net.URL(endpoint) );
			call.setOperationName(new QName(address, "getInfo"));
			String assetIdString = assetId.getIdString();
			String siteString = (String) call.invoke( new Object[] {sessionId, assetIdString} );
			//System.out.println("Sent ContentHosting.getInfo(sessionId,assetId), got '" + siteString + "'");
			return new Asset(this.key,siteString);			
		} catch (Throwable t) {
			Utilities.log(t);
			throw new org.osid.repository.RepositoryException(t.getMessage());
		}
    }

    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id assetId
                                                  , long date)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	public org.osid.repository.AssetIterator getAssetsBySearch(java.io.Serializable searchCriteria
                                                             , org.osid.shared.Type searchType
                                                             , org.osid.shared.Properties searchProperties)
    throws org.osid.repository.RepositoryException
    {
        if (searchCriteria == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        if (searchType == null) 
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        if (!(searchCriteria instanceof String))
        {
            // maybe change this to a new exception message
            Utilities.log("invalid criteria");
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
		java.util.Vector result = new java.util.Vector();
		boolean knownType = false;
		for (int i=0, size = this.searchTypeVector.size(); i < size; i++) {
			if (searchType.isEqual((org.osid.shared.Type)this.searchTypeVector.elementAt(i))) {
				knownType = true;
			}
		}
		if (!knownType)
		{
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}

		String criteria = ((String)searchCriteria).toLowerCase();

		try {
			// note this is a name match NOT a site search
			org.osid.repository.AssetIterator assetIterator = getAssets();
			while (assetIterator.hasNextAsset()) {
				result = match(assetIterator.nextAsset(), result, criteria);
			}
			
		} catch (Throwable t) {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(t.getMessage());
        }
		return new AssetIterator(result);
    }
	
	private java.util.Vector match(org.osid.repository.Asset asset, java.util.Vector result, String criteria)
	{
		try {
			// descend to resources, if appropriate
			if (asset.getAssetType().isEqual(Utilities.getCollectionAssetType())) {
				org.osid.repository.AssetIterator subAssetIterator = asset.getAssets();
				while (subAssetIterator.hasNextAsset()) {
					result = match(subAssetIterator.nextAsset(), result, criteria);
				}
			} else {
				if (asset.getDisplayName().toLowerCase().indexOf(criteria) != -1) {
					result.addElement(asset);
				}
			}
		} catch (Throwable t) {
			
		}
		return result;
	}

    public org.osid.shared.Id copyAsset(org.osid.repository.Asset asset)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.RecordStructureIterator getRecordStructuresByType(org.osid.shared.Type recordStructureType)
    throws org.osid.repository.RepositoryException
    {
        if (recordStructureType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.PropertiesIterator getProperties()
    throws org.osid.repository.RepositoryException
    {
        try
        {
            return new PropertiesIterator(new java.util.Vector());
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }        
    }

    public org.osid.shared.Properties getPropertiesByType(org.osid.shared.Type propertiesType)
    throws org.osid.repository.RepositoryException
    {
        if (propertiesType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        return new Properties();
    }

    public org.osid.shared.TypeIterator getPropertyTypes()
    throws org.osid.repository.RepositoryException
    {
        try
        {
            return new TypeIterator(new java.util.Vector());
        }
        catch (Throwable t)
        {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }        
    }

    public boolean supportsUpdate()
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public boolean supportsVersioning()
    throws org.osid.repository.RepositoryException
    {
        return true;
    }
}
