package edu.tufts.vue.fsm;

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
