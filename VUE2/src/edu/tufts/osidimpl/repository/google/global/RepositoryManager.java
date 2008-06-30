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

package edu.tufts.osidimpl.repository.google.global;

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
					log = loggingManager.getLogForWriting("Google Global");
				} catch (org.osid.logging.LoggingException lex) {
					log = loggingManager.createLog("Google Global");
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
				this.repositoryType = new Type("edu.tufts","repository","google");
				this.repositoryId = Utilities.getIdManager().getId("49734F17-AD8B-450B-8841-BCBF8F3454B3-758-000002113FA638E4");
				this.searchTypeVector.addElement(new Type("mit.edu","search","keyword"));
				this.repository = new Repository("Google",
												 "Google (not local).  This implementatin requires the user to have a key.  Keys are obtained directly from Google.",
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

		// update the Google personal key, if it has been passed with the configuration
		Object key = configuration.getProperty("GoogleGlobalLicenseKey");
		if ((key != null) && (key instanceof String)) {
			((Repository)this.repository).setGoogleKey((String)key);
			//Utilities.log("Google key set to " + key);
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
