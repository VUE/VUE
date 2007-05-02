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
package tufts.vue;

public class SakaiExport
{
	private org.osid.repository.Repository _repository = null;
	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("org.sakaiproject","asset","siteCollection");
	private String _ids[] = new String[0];
	private String _subIds[] = new String[0];
	private org.osid.id.IdManager _idManager = null;

	// if array is empty, configuration was incomplete, server was not responding, permission was denied
	public String[] getCollectionNames()
	{
		String names[] = new String[0];
		_ids = new String[0];
		
		initRepository();
		
		try {
			// get all the collections and their ids
			org.osid.repository.AssetIterator assetIterator = _repository.getAssetsByType(_collectionAssetType);
			java.util.Vector nameVector = new java.util.Vector();
			java.util.Vector idVector = new java.util.Vector();
			while (assetIterator.hasNextAsset()) {
				org.osid.repository.Asset asset = assetIterator.nextAsset();
				nameVector.addElement(asset.getDisplayName());
				idVector.addElement(asset.getId().getIdString());			 
			}
			// convert to string arrays for return
			int size = nameVector.size();
			names = new String[size];
			_ids = new String[size];
			for (int i=0; i < size; i++) {
				names[i] = (String)nameVector.elementAt(i);
				_ids[i] = (String)idVector.elementAt(i);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return names;
	}
	
	// if array is empty, configuration was incomplete, server was not responding, permission was denied
	public String[] getCollectionIds()
	{
		getCollectionNames();
		return _ids;
	}

	public boolean hasChildren(String collectionId)
	{
		// query for any sub-Assets
		try {
			initRepository();
			org.osid.repository.Asset asset = _repository.getAsset(_idManager.getId(collectionId));
			org.osid.repository.AssetIterator assetIterator = asset.getAssetsByType(_collectionAssetType);
			return assetIterator.hasNextAsset();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
	
	// empty array means no children
	public String[] getCollectionNames(String collectionId)
	{
		// Query for sub-Assets
		String names[] = new String[0];
		_subIds = new String[0];
			
		initRepository();
		
		try {
			// get all the collections and their ids
			org.osid.repository.Asset asset = _repository.getAsset(_idManager.getId(collectionId));
			org.osid.repository.AssetIterator assetIterator = asset.getAssetsByType(_collectionAssetType);
			java.util.Vector nameVector = new java.util.Vector();
			java.util.Vector idVector = new java.util.Vector();
			while (assetIterator.hasNextAsset()) {
				org.osid.repository.Asset a = assetIterator.nextAsset();
				nameVector.addElement(a.getDisplayName());
				idVector.addElement(a.getId().getIdString());			 
			}
			// convert to string arrays for return
			int size = nameVector.size();
			names = new String[size];
			_subIds = new String[size];
			for (int i=0; i < size; i++) {
				names[i] = (String)nameVector.elementAt(i);
				_subIds[i] = (String)idVector.elementAt(i);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return names;
	}

	// empty array means no children
	public String[] getCollectionIds(String collectionId)
	{
		// Query for sub-Assets
		getCollectionNames();
		return _subIds;
	}
	
	// failure's cause is logged
	public boolean saveToCollection(java.io.File file, String collectionId)
	{
		initRepository();
		try {
			org.osid.repository.Asset asset = _repository.getAsset(_idManager.getId(collectionId));
			asset.updateContent(file);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	} 

	private void initRepository()
	{
		try {
			org.osid.repository.RepositoryManager repositoryManager = null;
			if (_repository == null) {
				java.util.Properties properties = new java.util.Properties();
				properties.load(new java.io.FileInputStream("sakai.properties"));
				try {
					repositoryManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRepositoryManagerInstance("edu.tufts.osidimpl.repository.sakai@foo",
																														  new org.osid.OsidContext(),
																														  properties);
				} catch (Throwable t) {
					repositoryManager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																											   "edu.tufts.osidimpl.repository.sakai",
																											   new org.osid.OsidContext(),
																											   properties);
				}
				
			}
			org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
			if (repositoryIterator.hasNextRepository()) {
				_repository = repositoryIterator.nextRepository();
			}
			
			if (_idManager == null) {
				try {
					_idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																					   "comet.osidimpl.id.no_persist",
																					   new org.osid.OsidContext(),
																					   new java.util.Properties());
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
