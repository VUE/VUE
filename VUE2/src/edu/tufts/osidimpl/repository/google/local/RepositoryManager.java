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

package edu.tufts.osidimpl.repository.google.local;

public class RepositoryManager
implements org.osid.repository.RepositoryManager
{
    private org.osid.OsidContext context = null;
    private java.util.Properties configuration = null;
    private org.osid.repository.Repository repository = null;
	private org.osid.shared.Type repositoryType = null;
	private org.osid.shared.Id repositoryId = null;
    private java.util.Vector repositoryVector = new java.util.Vector();
    private java.util.Vector searchTypeVector = new java.util.Vector();
	private boolean firstTime = true;
	
    public void osidVersion_2_0()
		throws org.osid.repository.RepositoryException
    {
    }
	
    public org.osid.OsidContext getOsidContext()
		throws org.osid.repository.RepositoryException
    {
        return context;
    }
	
    public void assignOsidContext(org.osid.OsidContext context)
		throws org.osid.repository.RepositoryException
    {
        this.context = context;
    }
	
    public void assignConfiguration(java.util.Properties configuration)
    throws org.osid.repository.RepositoryException
    {
		this.configuration = configuration;
		
		if (firstTime) {
			try {
				org.osid.logging.LoggingManager loggingManager = (org.osid.logging.LoggingManager)org.osid.OsidLoader.getManager("org.osid.logging.LoggingManager",
																																 "comet.osidimpl.logging.plain",
																																 this.context,
																																 new java.util.Properties());
				
				org.osid.logging.WritableLog log = null;
				try {
					log = loggingManager.getLogForWriting("Google Local");
				} catch (org.osid.logging.LoggingException lex) {
					log = loggingManager.createLog("Google Local");
				}
				log.assignFormatType(new Type("mit.edu","logging","plain"));
				log.assignPriorityType(new Type("mit.edu","logging","info"));
				Utilities.setLog(log);			
				
				org.osid.id.IdManager idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																										"comet.osidimpl.id.no_persist",
																										this.context,
																										new java.util.Properties());
				Utilities.setIdManager(idManager);
				
				/*
				 Make on Repository
				 */
				this.repositoryType = new Type("tufts.edu","repository","google");
				this.repositoryId = Utilities.getIdManager().getId("4C728514-3135-43EC-95CA-F588F4668C5B-304-0000001A73B8246E");
				this.searchTypeVector.addElement(new Type("mit.edu","search","keyword"));
				this.repository = new Repository("Google Enterprise",
												 "Google (not Global).  This implementation requires the user to have a URL, site, and client.",
												 this.repositoryId,
												 this.repositoryType,
												 this.searchTypeVector);
				this.repositoryVector.addElement(this.repository);
			} catch (Throwable t) {
				Utilities.log(t);
				if (t instanceof org.osid.repository.RepositoryException) {
					throw new org.osid.repository.RepositoryException(t.getMessage());
				} else {                
					throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
				}
			}
		}
		firstTime = false;

//		System.out.println("google local configuration is " + configuration);
		
		// update the URL, if it has been passed with the configuration
		Object urlObject = configuration.getProperty("googleLocalURL");
		Object siteObject = configuration.getProperty("googleLocalSite");
		Object clientObject = configuration.getProperty("googleLocalClient");

		String url = (String)urlObject;
		String site = (String)siteObject;
		String client = (String)clientObject;

//		System.out.println(url);
//		System.out.println(site);
//		System.out.println(client);
		
		if ( (url != null) && (url instanceof String) && (site != null) && (site instanceof String)  && (client != null) && (client instanceof String) ) {
			
			if((url != null) ) {
				if((url.indexOf("http://") >=0 && url.length() > 7))
					url = url.substring(7);
			} else {
				url ="";
			}

			String address = "http://"+url+"/search?site="+site+"&client="+client+"&output=xml_no_dtd";
			
			((Repository)this.repository).setGoogleAddress(address);
			Utilities.log("Google address set to " + address);
		}
	}

    public org.osid.repository.Repository createRepository(String displayName
                                                         , String description
                                                         , org.osid.shared.Type repositoryType)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public void deleteRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.repository.RepositoryIterator getRepositories()
    throws org.osid.repository.RepositoryException
    {
        return new RepositoryIterator(this.repositoryVector);
    }

    public org.osid.repository.RepositoryIterator getRepositoriesByType(org.osid.shared.Type repositoryType)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryType == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        java.util.Vector result = new java.util.Vector();
        org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
        while (repositoryIterator.hasNextRepository())
        {
            org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
            if (nextRepository.getType().isEqual(repositoryType))
            {
                result.addElement(nextRepository);
            }
        }
        return new RepositoryIterator(result);
    }

    public org.osid.repository.Repository getRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try
        {
            org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
            while (repositoryIterator.hasNextRepository())
            {
                org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
                if (nextRepository.getId().isEqual(repositoryId))
                {
                    return nextRepository;
                }
            }
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try
        {
            org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
            while (repositoryIterator.hasNextRepository())
            {
                org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
                try
                {
                    org.osid.repository.Asset asset = nextRepository.getAsset(assetId);
                    return asset;
                }
                catch (Throwable t) {}
            }
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }

    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id assetId
                                                  , long date)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try
        {
            org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
            while (repositoryIterator.hasNextRepository())
            {
                org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
                try
                {
                    org.osid.repository.Asset asset = nextRepository.getAssetByDate(assetId,date);
                    return asset;
                }
                catch (Throwable t) {}
            }
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
        throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }

    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        java.util.Vector result = new java.util.Vector();
        try
        {
            org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
            while (repositoryIterator.hasNextRepository())
            {
                org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
                org.osid.shared.LongValueIterator longValueIterator = repository.getAssetDates(assetId);
                while (longValueIterator.hasNextLongValue())
                {
                    result.addElement(new Long(longValueIterator.nextLongValue()));
                }
            }
            return new LongValueIterator(result);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.repository.AssetIterator getAssetsBySearch(org.osid.repository.Repository[] repositories
                                                             , java.io.Serializable searchCriteria
                                                             , org.osid.shared.Type searchType
                                                             , org.osid.shared.Properties searchProperties)
    throws org.osid.repository.RepositoryException
    {
        if (repositories == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try
        {
            java.util.Vector results = new java.util.Vector();
            for (int j=0; j < repositories.length; j++)
            {
                org.osid.repository.Repository nextRepository = repositories[j];
                //optionally add a separate thread here
                try
                {
                    org.osid.repository.AssetIterator assetIterator =
                        nextRepository.getAssetsBySearch(searchCriteria,searchType,searchProperties);
                    while (assetIterator.hasNextAsset())
                    {
                        results.addElement(assetIterator.nextAsset());
                    }
                }
                catch (Throwable t) 
                {
                    // log exceptions but don't stop searching
                    Utilities.log(t);
                }
            }
            return new AssetIterator(results);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

    public org.osid.shared.Id copyAsset(org.osid.repository.Repository repository
                                      , org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if ((repository == null) || (assetId == null))
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public org.osid.shared.TypeIterator getRepositoryTypes()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
        try
        {
            results.addElement(this.repositoryType);
            return new TypeIterator(results);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }
}
