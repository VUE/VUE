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
package tufts.vue;

public class SakaiCollection
{
//	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("org.sakaiproject","asset","siteCollection");
	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("com.harvestroad","asset","category");
	private org.osid.repository.Asset _asset = null;
	
	public SakaiCollection(org.osid.repository.Asset asset)
	{
		_asset = asset;
	}

	// if array is empty, configuration was incomplete, server was not responding, permission was denied
	public String getDisplayName()
		throws org.osid.repository.RepositoryException
	{
		return _asset.getDisplayName();
	}
	
	public SakaiCollection[] getCollections()
		throws org.osid.repository.RepositoryException
	{
		java.util.Vector collectionVector = new java.util.Vector();
		SakaiCollection collection[] = new SakaiCollection[0];
		
		// get all the collections and their ids
		org.osid.repository.AssetIterator assetIterator = _asset.getAssetsByType(_collectionAssetType);
		while (assetIterator.hasNextAsset()) {
			collectionVector.addElement(new SakaiCollection(assetIterator.nextAsset()));
		}
		// convert to array for return
		int size = collectionVector.size();
		collection = new SakaiCollection[size];
		for (int i=0; i < size; i++) {
			collection[i] = (SakaiCollection)collectionVector.elementAt(i);
		}
		return collection;
	}

	public boolean hasChildren()
	{
		// query for any sub-Assets
		try {
			org.osid.repository.AssetIterator assetIterator = _asset.getAssetsByType(_collectionAssetType);
			return assetIterator.hasNextAsset();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
	
	// failure's cause is logged
	public boolean save(java.io.File file)
	{
		try {
			_asset.updateContent(file);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	} 
}
