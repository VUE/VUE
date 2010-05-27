/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

package edu.tufts.osidimpl.repository.artifact;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import edu.tufts.osidimpl.repository.fedora_2_2.PID;

import java.io.*;
import java.util.Properties;

public class Repository
implements org.osid.repository.Repository
{
	public static final String DISPLAY_NAME = "Artifact";
    private org.osid.shared.Id repositoryId = null;
	private org.osid.shared.Type repositoryType = null;
	private org.osid.shared.Type assetType = new Type("tufts.edu","asset","artifact");
	private org.osid.shared.Type artifactSearchType = new Type("tufts.edu","search","artifact");
        private org.osid.shared.Type keywordSearchType = new Type("mit.edu","search","keyword");
        private org.osid.shared.Type artifactMultiFieldSearchType = new Type("tufts.edu","search","artifact-multifield");
    private String displayName =  DISPLAY_NAME;
    private String description = null;
    private String address = null;
    private String searchKeywordTypePrefix = "searchType=Any";
    private java.util.Vector searchTypeVector = new java.util.Vector();
    private java.util.Properties configuration;
    private static final String DEFAULT_ADDRESS = "http://artifact.tufts.edu/";
	private static final String SEARCH_URL_PREFIX = DEFAULT_ADDRESS+"vue_xml/search2.asp?searchType=Any&max_return=10&query=";
	private static final String SEARCH_URL_SUFFIX = "&Submit=Submit";
	private int maxReturn = 10;
    protected Repository(String displayName,
						 String description,
						 String address,
						 String maxReturn,
						 org.osid.shared.Id repositoryId,
						 org.osid.shared.Type repositoryType,
						 java.util.Vector searchTypeVector)
		throws org.osid.repository.RepositoryException
    {
        this.displayName = displayName;
        this.description = description;
        this.address = address;
		this.repositoryId = repositoryId;
        this.repositoryType = repositoryType;
        this.searchTypeVector = searchTypeVector;
        searchKeywordTypePrefix  += "&max_return="+maxReturn+"&query=";
    }
    
     

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
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
        if (!assetType.isEqual(this.assetType))
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
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
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
	}

    public org.osid.repository.AssetIterator getAssetsByType(org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.TypeIterator getAssetTypes()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
        try
        {
            results.addElement(this.assetType);
            return new TypeIterator(results);
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
        java.util.Vector results = new java.util.Vector();
        results.addElement(RecordStructure.getInstance());
        return new RecordStructureIterator(results);
    }

    public org.osid.repository.RecordStructureIterator getMandatoryRecordStructures(org.osid.shared.Type assetType)
    throws org.osid.repository.RepositoryException
    {
        if (assetType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        if (assetType.isEqual(this.assetType))
        {
            java.util.Vector results = new java.util.Vector();
            results.addElement(RecordStructure.getInstance());
            return new RecordStructureIterator(results);
        }
        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
    }

    public org.osid.shared.TypeIterator getSearchTypes()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
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
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
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

        boolean knownType = false;
		for (int searchTypeNum = 0, size = this.searchTypeVector.size(); searchTypeNum < size; searchTypeNum++)
		{
			org.osid.shared.Type type = (org.osid.shared.Type)(this.searchTypeVector.elementAt(searchTypeNum));
			if (type.isEqual(searchType))
			{
				knownType = true;
			}
		}
		if (!knownType) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}
				
		java.util.Vector results = new java.util.Vector();
        try
        {
            String criteria = (String)searchCriteria;
			String query = null;
                        System.out.println("Artifact Search: Type "+ searchType.getKeyword()+" auth ="+searchType.getAuthority());
                        System.out.println(criteria);
			if (searchType.isEqual(this.artifactSearchType)) {
				query = criteria;
			} else if(searchType.isEqual(this.keywordSearchType) && this.address !=null) {
					 
                                query = this.address+searchKeywordTypePrefix+criteria;
                        }  else {
                        
				query = SEARCH_URL_PREFIX +  criteria  + SEARCH_URL_SUFFIX;
			}
			
			Utilities.log("Artifact Query " + query);
			System.out.println("Address:"+address);
			System.out.println("Query:" +query);
 
			return new AssetIterator(query,this.repositoryId);
        }
        catch (Throwable t)
        {
        	t.printStackTrace();
            Utilities.log(t);
			if (t.getMessage().equals(org.osid.OsidException.PERMISSION_DENIED)) {
				t.printStackTrace();
				throw new org.osid.repository.RepositoryException(org.osid.OsidException.PERMISSION_DENIED);
			}
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }
    
    private String xmlCriteriaToStringUrl(String criteria) throws Exception {
        String urlPart = new String();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new StringBufferInputStream(criteria));
        NodeList list = doc.getElementsByTagName("field");
        for(int i =0; i <list.getLength(); i++) {
             Element fieldElement = (Element)list.item(i);
                 Node typeNode =  fieldElement.getElementsByTagName("type").item(0);
                 Node valueNode  = fieldElement.getElementsByTagName("value").item(0);
                 String key  =  new String();
                 String value = new String();
                 if(typeNode.getFirstChild() instanceof Text) {
                     String s = typeNode.getFirstChild().getNodeValue();
                     key = s.substring(s.indexOf("/")+1,s.indexOf("@"));
                     if(key.equalsIgnoreCase("keywords")) key = "";
                     else key = "&"+key+"=";
                 }
                 if(valueNode.getFirstChild() instanceof Text) {
                     value = valueNode.getFirstChild().getNodeValue();
                         }
                 urlPart = key+value;
        }
        
        return urlPart;
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
        if (recordStructureType.isEqual(new Type("tufts.edu","recordStructure","artifact")))
        {
            java.util.Vector results = new java.util.Vector();
            results.addElement(RecordStructure.getInstance());
            return new RecordStructureIterator(results);
        }
        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
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
        return (org.osid.shared.Properties) new Properties();
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

    protected void addAsset(org.osid.repository.Asset asset)
    throws org.osid.repository.RepositoryException
    {
        if (asset == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);    
	}

    public boolean supportsUpdate()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean supportsVersioning()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }
    

}
