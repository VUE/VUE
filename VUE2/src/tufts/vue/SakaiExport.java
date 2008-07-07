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
package tufts.vue;

public class SakaiExport
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SakaiExport.class);
    
	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("sakaiproject.org","asset","siteCollection");
	private org.osid.shared.Type _sakaiRepositoryType = new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
	private edu.tufts.vue.dsm.DataSourceManager _dsm = null;

	public SakaiExport(edu.tufts.vue.dsm.DataSourceManager dsm)
	{
		_dsm = dsm;
	}
	
	public edu.tufts.vue.dsm.DataSource[] getSakaiDataSources()
		throws org.osid.repository.RepositoryException
	{
		java.util.Vector dataSourceVector = new java.util.Vector();
		edu.tufts.vue.dsm.DataSource result[] = new edu.tufts.vue.dsm.DataSource[0];
		
		edu.tufts.vue.dsm.DataSource dataSources[] = _dsm.getDataSources();
		for (int i=0; i < dataSources.length; i++) {
                    
                    final org.osid.repository.Repository repository = dataSources[i].getRepository();
                    final String rname = (repository == null ? "<null!>" : repository.getDisplayName());
                    if (DEBUG.DR) Log.debug("Examining data source: " + dataSources[i] + "; repository=" + rname);
                    
//                     final String name = (repository == null ? "<null-repository>" : repository.getDisplayName());
//                     if (DEBUG.DR) Log.debug(" Which has repository: " + repository + "; name=" + name);
                    
                    if (dataSources[i].supportsUpdate() && repository != null) {
                        if (DEBUG.DR) Log.info("Supports Update, Now Checking Type");
                        if (DEBUG.DR) Log.info("checking type " + repository.getType().getAuthority() );
                        if (repository.getType().isEqual(_sakaiRepositoryType)) {
                            if (DEBUG.DR) Log.info("checking type worked" );
                            dataSourceVector.addElement(dataSources[i]);
                        }
                    }
		}
		// convert to array for return
		int size = dataSourceVector.size();
		result = new edu.tufts.vue.dsm.DataSource[size];
		for (int i=0; i < size; i++) {
			result[i] = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
		}
		return result;
	}

	// if array is empty, configuration was incomplete, server was not responding, permission was denied
	public SakaiCollection[] getCollections(edu.tufts.vue.dsm.DataSource dataSource)
		throws org.osid.repository.RepositoryException
	{
		java.util.Vector collectionVector = new java.util.Vector();
		SakaiCollection collection[] = new SakaiCollection[0];
		
		// get all the collections
		if (dataSource.supportsUpdate()) {
			org.osid.repository.Repository repository = dataSource.getRepository();
			org.osid.repository.AssetIterator assetIterator = repository.getAssetsByType(_collectionAssetType);
			while (assetIterator.hasNextAsset()) {
				collectionVector.addElement(new SakaiCollection(assetIterator.nextAsset()));
			}
			// convert to array for return
			int size = collectionVector.size();
			collection = new SakaiCollection[size];
			for (int i=0; i < size; i++) {
				collection[i] = (SakaiCollection)collectionVector.elementAt(i);
			}
		}
		return collection;
	}
}
