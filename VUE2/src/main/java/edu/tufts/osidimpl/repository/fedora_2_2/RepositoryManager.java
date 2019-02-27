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
package  edu.tufts.osidimpl.repository.fedora_2_2;

public class RepositoryManager
        implements org.osid.repository.RepositoryManager {
    private org.osid.OsidContext context = null;
    private java.util.Map configuration = null;
    private org.osid.repository.Repository repository = null;
    private static final String REPOSITORY_ID_STRING = "6D6ABA4C-3B1F-4326-BBCD-A3417CCE4687-EC49D42B";
    
    public org.osid.OsidContext getOsidContext()
    throws org.osid.repository.RepositoryException {
        return this.context;
    }
    
    public void assignOsidContext(org.osid.OsidContext context)
    throws org.osid.repository.RepositoryException {
        this.context = context;
    }
    
    public void assignConfiguration(java.util.Properties configuration)
    throws org.osid.repository.RepositoryException {
        this.configuration = configuration;
        try {
			//System.out.println("Fedora 2.0 local configuration is " + configuration);
			
			Object displayname = configuration.getProperty("fedora22DisplayName");
//			Object address = configuration.getProperty("fedora22Address");
//			Object port = configuration.getProperty("fedora22Port");
//			Object username = configuration.getProperty("fedora22UserName");
//			Object password = configuration.getProperty("fedora22Password");
			
			if (displayname != null) {
//				String displaynameString = (String)displayname;
//				String addressString = (String)address;
//				String portString = (String)port;
//				String usernameString = (String)username;
//				String passwordString = (String)password;
				
//				this.repository = new Repository("fedora.xml",
//												 REPOSITORY_ID_STRING,
//												 displaynameString,
//												 addressString,
//												 portString,
//												 usernameString,
//												 passwordString);
                            this.repository  = new Repository(configuration,REPOSITORY_ID_STRING);
			}
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public org.osid.repository.Repository createRepository(String displayName
            , String description
            , org.osid.shared.Type repositoryType)
            throws org.osid.repository.RepositoryException {
        if ( (displayName == null) || (description == null) || (repositoryType == null) ) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public void deleteRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException {
        if (repositoryId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.RepositoryIterator getRepositories()
    throws org.osid.repository.RepositoryException {
        java.util.Vector result = new java.util.Vector();
        result.addElement(this.repository);
        return new RepositoryIterator(result);
    }
    
    public org.osid.repository.RepositoryIterator getRepositoriesByType(org.osid.shared.Type repositoryType)
    throws org.osid.repository.RepositoryException {
        if (repositoryType == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        java.util.Vector result = new java.util.Vector();
        result.addElement(this.repository);
        return new RepositoryIterator(result);
    }
    
    public org.osid.repository.Repository getRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException {
        if (repositoryId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        try {
            if (repositoryId.getIdString().equals(REPOSITORY_ID_STRING)) {
                return this.repository;
            }
        } catch (Throwable t) {
            
        }
        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }
    
    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id assetId
            , long date)
            throws org.osid.repository.RepositoryException {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        java.util.Vector result = new java.util.Vector();
        // insert code here to add elements to result vector
        try {
            return new LongValueIterator(result);
        } catch(org.osid.OsidException oex) {
            System.out.println(oex.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
    }
    
    public org.osid.repository.AssetIterator getAssetsBySearch(org.osid.repository.Repository[] repositories
            , java.io.Serializable searchCriteria
            , org.osid.shared.Type searchType
            , org.osid.shared.Properties properties)
            throws org.osid.repository.RepositoryException {
        if ( (repositories == null) || (searchCriteria == null) ) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        // just call get assets on each repository
        java.util.Vector result = new java.util.Vector();
        try {
            for (int i=0, length = repositories.length; i < length; i++) {
                org.osid.repository.AssetIterator assetIterator =
                        repositories[i].getAssetsBySearch(searchCriteria,searchType,null);
                while (assetIterator.hasNextAsset()) {
                    result.addElement(assetIterator.nextAsset());
                }
            }
        } catch(Exception ex) {
            System.out.println(ex.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return new AssetIterator(result);
    }
    
    public org.osid.shared.Id copyAsset(org.osid.repository.Repository repository
            , org.osid.shared.Id assetId)
            throws org.osid.repository.RepositoryException {
        if ( (repository == null) || (assetId == null) ) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.NULL_ARGUMENT);
        }
        
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public org.osid.shared.TypeIterator getRepositoryTypes()
    throws org.osid.repository.RepositoryException {
        try {
            java.util.Vector result = new java.util.Vector();
            result.addElement(new Type("tufts.edu","repository","fedoraImage"));
            return new TypeIterator(result);
        } catch (org.osid.OsidException oex) {
            System.out.println(oex.getMessage());
        }
        throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
    }
    
    public boolean supportsUpdate()
    throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public boolean supportsVersioning()
    throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public void osidVersion_2_0()
    throws org.osid.repository.RepositoryException {
    }
    
}
