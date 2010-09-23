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
package edu.tufts.vue.fsm;

/**
 A Federated Search Manager returns UI controls that are included with VUE or provided
 by a third party.  A query adjuster allows a third party an opportunity to modify the 
 search criteria, type, or properties before a search is called.  A result set manager
 returns results of a search.
 */

public interface FederatedSearchManager
{
	public QueryEditor getQueryEditorForType(org.osid.shared.Type searchType);
	
	public QueryAdjuster getQueryAdjusterForRepository(org.osid.shared.Id repositoryId);
	
	public AssetViewer getAssetViewerForType(org.osid.shared.Type assetType);
	
	public ResultSetManager getResultSetManager(java.io.Serializable searchCriteria,
												org.osid.shared.Type searchType,
												org.osid.shared.Properties searchProperties);
}
