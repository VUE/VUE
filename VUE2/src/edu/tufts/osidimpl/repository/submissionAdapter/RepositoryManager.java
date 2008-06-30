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

package edu.tufts.osidimpl.repository.submissionAdapter;

public class RepositoryManager
implements org.osid.repository.RepositoryManager
{
    private org.osid.OsidContext context = null;
    private java.util.Properties configurationProperties = null;
	private java.util.Vector repositoryVector = new java.util.Vector();

    // threaded federated search support
	private ThreadGroup mSearchThreadGroup;
	private java.util.Vector assetIteratorVector = new java.util.Vector();
	private int searchCount = 0;
	private int searchTimeout = 0;
	
    public org.osid.OsidContext getOsidContext()
		throws org.osid.repository.RepositoryException
    {
        return context;
    }
	
    public void assignOsidContext(org.osid.OsidContext context)
		throws org.osid.repository.RepositoryException
    {
        this.context = context;
		
		// OBA
		// assume we have zero or more RepositoryManagers passed in
		
		try {
			int i = 0;
			java.io.Serializable ser = null;
			String key = "submissionAdapter_" + i++;
			while ( (ser = context.getContext(key)) != null) {
				key = "submissionAdapter_" + i++;
				org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)ser;
				org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
				while (repositoryIterator.hasNextRepository()) {
					org.osid.repository.Repository repository = repositoryIterator.nextRepository();
					// here is where we filter for repositories that allow submission
					if (repository.supportsUpdate()) {
						this.repositoryVector.addElement(repository);
					}
				}
			}
		} catch (org.osid.repository.RepositoryException rex) {
			throw new org.osid.repository.RepositoryException(rex.getMessage());
		} catch (Throwable t) {
			System.out.println(t.getMessage());
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
    }
	
    public void assignConfiguration(java.util.Properties configurationProperties)
		throws org.osid.repository.RepositoryException
    {
        this.configurationProperties = configurationProperties;
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
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
	
	/**
		Returns currently configured set of repositories.
	 */
    public org.osid.repository.RepositoryIterator getRepositories()
		throws org.osid.repository.RepositoryException
    {
		return new RepositoryIterator(this.repositoryVector);
    }
	
	/**
	 Uses currently configured set of repositories. 
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the repositoryType argument is null.
	 This method can throw org.osid.shared.SharedException.UNKNOWN_TYPE.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.repository.RepositoryIterator getRepositoriesByType(org.osid.shared.Type repositoryType)
		throws org.osid.repository.RepositoryException
    {
        if (repositoryType == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		try {
			java.util.Vector result = new java.util.Vector();
			
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				if (repository.getType().isEqual(repositoryType)) {
					result.addElement(repository);
				}
			}
			if (result.size() > 0) {
				return new RepositoryIterator(result);
			}
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
    }
	
	/**
	 Uses currently configured set of repositories. 
	 <p>
	 This implementation assumes repository ids are unique and returns the first repository whose id matches the criterion.
	 <p>
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the repositoryType argument is null.
	 This method throws org.osid.shared.SharedException.UNKNOWN_ID.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.repository.Repository getRepository(org.osid.shared.Id repositoryId)
		throws org.osid.repository.RepositoryException
    {
        if (repositoryId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.NULL_ARGUMENT);
        }
		try {
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				if (repository.getId().isEqual(repositoryId)) {
					return repository;
				}
			}
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }
	
	/**
  	 Uses currently configured set of repositories.
	 <p>
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the assetId argument is null.
	 This method throws org.osid.shared.SharedException.UNKNOWN_ID.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId)
		throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.NULL_ARGUMENT);
        }
		try {
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				try {
					return repository.getAsset(assetId);
				} catch (Throwable t1) {
					// ignore and continue
				}
			}
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }
	
	/**
	 Uses currently configured set of repositories.
	 <p>
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the assetId argument is null.
	 This method throws org.osid.shared.SharedException.UNKNOWN_ID.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id assetId,
													long date)
		throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.NULL_ARGUMENT);
        }
		try {
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				try {
					return repository.getAssetByDate(assetId, date);
				} catch (Throwable t1) {
					// ignore and continue
				}
			}
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }
	
	/**
	 Uses currently configured set of repositories.
	 <p>
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the assetId argument is null.
	 This method throws org.osid.shared.SharedException.UNKNOWN_ID.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId)
		throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.NULL_ARGUMENT);
        }
		try {
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				try {
					return repository.getAssetDates(assetId);
				} catch (Throwable t1) {
					// ignore and continue
				}
			}
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }
	
	/**
	 Uses currently configured set of repositories.
	 <p>
	 If an exception is thrown by a call to a manager, the exception is logged and processing continues.
	 This method throws org.osid.shared.SharedException.NULL_ARGUMENT if the assetId argument is null.
	 This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
    public org.osid.repository.AssetIterator getAssetsBySearch(org.osid.repository.Repository[] repositories
															   , java.io.Serializable searchCriteria
															   , org.osid.shared.Type searchType
															   , org.osid.shared.Properties searchProperties)
		throws org.osid.repository.RepositoryException
    {
        if (repositories == null) {
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.NULL_ARGUMENT);
        }
		
		try {
			java.util.Vector result = new java.util.Vector();
			assetIteratorVector.removeAllElements();
			searchCount = 0;
			
			mSearchThreadGroup = new ThreadGroup("SearchParent");
			performParallelSearches(repositories,searchCriteria,searchType,searchProperties);

			int maxSearches = repositories.length;
			
			// allow for overrides through configuration
			if (searchTimeout < 1) searchTimeout = 120;
			int maxChecks = 10;
			long sleep = (searchTimeout * 100);
			int checkCount = 0;
			
			while (checkCount++ <= maxChecks) {
				Thread.sleep(sleep);
				if (searchCount == maxSearches) {
					break;
				}
			}
			
			for (int i=0; i < assetIteratorVector.size(); i++) {
				org.osid.repository.AssetIterator assetIterator = (org.osid.repository.AssetIterator)assetIteratorVector.elementAt(i);
				while (assetIterator.hasNextAsset()) {
					result.addElement(assetIterator.nextAsset());
				}
			}
			return new AssetIterator(result);
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
	}
	
	/* 			

		Threaded federated search support begins

	*/	
    private synchronized void performParallelSearches(org.osid.repository.Repository[] repositories,
													  java.io.Serializable searchCriteria,
													  org.osid.shared.Type searchType,
													  org.osid.shared.Properties searchProperties) 
	{
        mSearchThreadGroup.interrupt();        
        final Thread[] threads = new Thread[repositories.length];
        
        for (int i = 0; i < repositories.length; i++) {
            final org.osid.repository.Repository repository = repositories[i];
            
            SearchThread searchThread = null;
            try {                
                searchThread = new SearchThread(repository, searchCriteria, searchType, searchProperties);
            } catch (Throwable t) {
            }
            
            threads[i] = searchThread;
        }
        
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
		}
    }
	
	
    private  class SearchThread extends Thread 
	{
        private final org.osid.repository.Repository mRepository;
        private java.io.Serializable mSearchCriteria;
        private org.osid.shared.Type mSearchType;
        private org.osid.shared.Properties mSearchProperties;
        
        public SearchThread(org.osid.repository.Repository r,
							java.io.Serializable searchCriteria,
							org.osid.shared.Type searchType,
							org.osid.shared.Properties searchProperties)
			throws org.osid.repository.RepositoryException {
				super(mSearchThreadGroup,"Search");
				setDaemon(true);
				
				mRepository = r;
				mSearchCriteria = searchCriteria;
				mSearchType = searchType;
				mSearchProperties = searchProperties;
			}
        
        public void run() 
		{            
            if (stopped())
                return;
            
            try {
                if (stopped())
                    return;
                
                // TODO: ultimately, the repository will need some kind of callback
                // architecture, so that a search can be aborted even while waiting for
                // the server to come back, tho it'll probably need to use channel based
                // NIO to really get that working.  Should that day come, the federated
                // search manager could handle this full threading and calling us back
                // as results come in, so we could skip our threading code here, and so
                // other GUI's could take advantage of the fully parallel search code.
                
                assetIteratorVector.addElement(mRepository.getAssetsBySearch(mSearchCriteria, mSearchType, mSearchProperties));
            } catch (Throwable t) {
				String msg = null;
				try {
					msg = "Repository " + mRepository.getDisplayName() + " reported exception: " + t.getMessage();
				} catch (Throwable t1) {
				}
                if (stopped())
                    return;
            }
			searchCount++;
        }
                
        private boolean stopped() {
            if (isInterrupted()) {
                return true;
            } else
                return false;
        }
	}
        
	/**
		Unimplemented method
	 */
    public org.osid.shared.Id copyAsset(org.osid.repository.Repository repository
										, org.osid.shared.Id assetId)
		throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
	
	/**
		Uses currently configured set of repositories.
		This method can throw org.osid.OsidException.OPERATION_FAILED.
	 */
	public org.osid.shared.TypeIterator getRepositoryTypes()
		throws org.osid.repository.RepositoryException
	{
		try {
			java.util.Vector result = new java.util.Vector();
			java.util.Vector typeStringVector = new java.util.Vector();
			for (int i=0, size = this.repositoryVector.size(); i < size; i++) {
				org.osid.repository.Repository repository = (org.osid.repository.Repository)this.repositoryVector.elementAt(i);
				org.osid.shared.Type type = repository.getType();
				String typeString = typeToString(type);
				if (!typeStringVector.contains(typeString)) {
					typeStringVector.addElement(typeString);
					result.addElement(type);
				}
			}
			return new TypeIterator(result);
		} catch (Throwable t) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
	}
	
    public void osidVersion_2_0()
		throws org.osid.repository.RepositoryException
    {
    }
	
	private String typeToString(org.osid.shared.Type type)
	{
		return type.getDomain() + "/" + type.getKeyword() + "@" + type.getAuthority();
	}
	
}