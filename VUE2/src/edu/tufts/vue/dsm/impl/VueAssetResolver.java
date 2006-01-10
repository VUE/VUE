package edu.tufts.vue.dsm.impl;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

public class VueAssetResolver
implements edu.tufts.vue.dsm.AssetResolver
{
	/**
	*/
	public edu.tufts.vue.dsm.AssetReference getAssetReference(org.osid.shared.Id assetId,
											org.osid.shared.Id repositoryId) {
		try {
			edu.tufts.vue.dsm.DataSourceManager dsm = VueDataSourceManager.getInstance();
			edu.tufts.vue.dsm.DataSource[] dataSources = dsm.getDataSources();
			for (int i=0; i < dataSources.length; i++) {
				if (dataSources[i].getRepositoryId().isEqual(repositoryId)) {
					return new VueAssetReference(dataSources[i].getOsidLoadKey(),
												 assetId.getIdString());
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"getting reference for Asset");
		}
		return null;
	}

	/**
	*/
	public org.osid.repository.Asset getAsset(String osidLoadKey,
											  String assetIdString)
	throws org.osid.repository.RepositoryException {
		try {
			org.osid.id.IdManager idManager = VueOsidFactory.getInstance().getIdManagerInstance();
			org.osid.repository.RepositoryManager repositoryManager = VueOsidFactory.getInstance().getRepositoryManagerInstance(osidLoadKey);
			return repositoryManager.getAsset(idManager.getId(assetIdString));
		} catch (org.osid.repository.RepositoryException rex) {
			edu.tufts.vue.util.Logger.log("getting Asset from reference");
			throw rex;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"getting Asset from reference");
		}
		return null;
	}
}
