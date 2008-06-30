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

package edu.tufts.osidimpl.repository.localfiles;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.*;
import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;
import tufts.vue.*;

public class Repository
implements org.osid.repository.Repository
{
    private org.osid.shared.Id repositoryId = null;
	private org.osid.shared.Type repositoryType = null;
	private org.osid.shared.Type assetType = new Type("edu.tufts","asset","file");
    private String displayName = null;
    private String description = null;
    private java.util.Vector searchTypeVector = null;
	
    protected Repository(String displayName,
						 String description,
						 org.osid.shared.Id repositoryId,
						 org.osid.shared.Type repositoryType,
						 java.util.Vector searchTypeVector)
		throws org.osid.repository.RepositoryException
    {
        this.displayName = displayName;
        this.description = description;
		this.repositoryId = repositoryId;
        this.repositoryType = repositoryType;
        this.searchTypeVector = searchTypeVector;
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
		//recurrsive descent occurse in each Asset
		Vector result = new Vector();
		try {			
			Vector cabVector = new Vector();
			installDesktopFolders(cabVector);
			
			Iterator iterator = cabVector.iterator();
			while (iterator.hasNext()) {
				LocalCabinet localCabinet = (LocalCabinet)iterator.next();
				result.addElement(new Asset(localCabinet, this.repositoryId));
				
				CabinetEntryIterator i = localCabinet.entries();				
				while (i.hasNext()) {
					CabinetEntry ce = i.next();
					result.addElement(new Asset(ce, this.repositoryId));
				}
			}
		} catch (Throwable t) {
			Utilities.log(t);
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		return new AssetIterator(result);
	}
			
	private void installDesktopFolders(Vector cabVector)
	{
		osid.shared.Agent agent = null; //  This may cause problems later.
		
		File home = new File(VUE.getSystemProperty("user.home"));
		if (home.exists() && home.canRead()) {
			// This might be better handled via addRoot on the LocalFilingManager, but
			// we can't set the label (title) for it that way. -- SMF
			String[] dirs = { "Desktop", "My Documents", "Documents", "Pictures", "My Pictures", "Photos", "My Photos"};
			int added = 0;
			for (int i = 0; i < dirs.length; i++) {
				File dir = new File(home, dirs[i]);
				if (dir.exists() && dir.canRead()) {
					//					CabinetResource r = new CabinetResource(new LocalCabinet(dir.getPath(), agent, null));
					//					r.setTitle(dirs[i]);
					//					cabVector.add(r);
					cabVector.add(new LocalCabinet(dir.getPath(),agent,null));
					added++;
				}
			}
			if (added == 0 || tufts.Util.isWindowsPlatform() == false) {
                            Resource r = Resource.getFactory().get(new LocalCabinet(home.getPath(), agent, null));
                            String title = "Home";
                            String user = VUE.getSystemProperty("user.name");
                            if (user != null)
                                title += " (" + user + ")";
                            r.setTitle(title);
                            //				cabVector.add(r);
                            cabVector.add(new LocalCabinet(home.getPath(), agent, null));
			}
		}
		boolean gotSlash = false;
		File volumes = null;
		if (tufts.Util.isMacPlatform()) {
			volumes = new File("/Volumes");
		} else if (tufts.Util.isUnixPlatform()) {
			volumes = new File("/mnt");
		}
		if (volumes != null && volumes.exists() && volumes.canRead()) {
			File[] vols = volumes.listFiles();
			for (int i = 0; i < vols.length; i++) {
				File v = vols[i];
				if (!v.canRead() || v.getName().startsWith("."))
					continue;
				Resource r = Resource.getFactory().get(new LocalCabinet(v.getPath(), agent, null));
				r.setTitle(v.getName());
				try {
					//r.setTitle(v.getName() + " (" + v.getCanonicalPath() + ")");
					if (v.getCanonicalPath().equals("/"))
						gotSlash = true;
				} catch (Exception e) {
					System.err.println(e);
				}
				//				cabVector.add(r);
				cabVector.add(new LocalCabinet(v.getPath(), agent, null));
			}
		}
		
		try {
			final FileSystemView fsview = FileSystemView.getFileSystemView();
			final LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
			
			LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
			while(rootCabs.hasNext()){
				LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
				Resource res = Resource.getFactory().get(rootNode);
				if (rootNode instanceof LocalCabinet) {
					File f = ((LocalCabinet)rootNode).getFile();
					try {
						if (f.getCanonicalPath().equals("/") && gotSlash)
							continue;
					} catch (Exception e) {
						System.err.println(e);
					}
					String sysName = fsview.getSystemDisplayName(f);
					if (sysName != null) {
						res.setTitle(sysName);
					}
				}
				//				cabVector.add(res);
				cabVector.add(rootNode);
			}
		} catch (Exception ex) {
			Utilities.log(ex);
		}
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
				
		String criteria = ((String)searchCriteria).toLowerCase();
		java.util.Vector result = new java.util.Vector();
        try
        {
			// get all assets and look for matches
			org.osid.repository.AssetIterator ai = getAssets();
			while (ai.hasNextAsset()) {
				org.osid.repository.Asset a = ai.nextAsset();
				if (a.getDisplayName().toLowerCase().indexOf(criteria) != -1) {
					result.addElement(a);
				}
			}
			return new AssetIterator(result);
        }
        catch (Throwable t)
        {
            Utilities.log(t);
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
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
        if (recordStructureType.isEqual(new Type("edu.tufts","recordStructure","artifact")))
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
