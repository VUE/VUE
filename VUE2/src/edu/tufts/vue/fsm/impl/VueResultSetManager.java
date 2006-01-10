package edu.tufts.vue.fsm.impl;

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
 This manager offers the raw material for a caller to package results for a user.
 */

public class VueResultSetManager
implements edu.tufts.vue.fsm.ResultSetManager
{
	//TO DO: how do we handle searches in progress
	private edu.tufts.vue.fsm.SearchEngine searchEngine = null;
	private org.osid.repository.AssetIterator assetIterator = null;
	
	private java.util.Vector assetVector = new java.util.Vector();
	private java.util.Vector assetIdStringVector = new java.util.Vector();
	
	protected VueResultSetManager(edu.tufts.vue.fsm.SearchEngine searchEngine)
	{
		this.searchEngine = searchEngine;
	}

	public java.awt.Image getPreview(org.osid.shared.Id assetId) {
		return null;
	}

	public org.osid.repository.AssetIterator getAssets()
	{
		AddingAssetIterator ai = new AddingAssetIterator();
		int numSearches = this.searchEngine.getNumSearches();
		for (int i=0; i < numSearches; i++) {
			ai.add(searchEngine.getAssetIterator(i));
		}
		return ai;
	}

	public void addAsset(org.osid.repository.Asset asset)
	{
		try {
			this.assetVector.addElement(asset);
			this.assetIdStringVector.addElement(asset.getId());
		} catch (Throwable t) {
			
		}
	}
	
	public boolean isComplete()
	{
		return this.searchEngine.isComplete();
	}
	
	public void removeAsset(org.osid.repository.Asset assetId)
	{
		
	}
	
	public void clearResults()
	{
		this.searchEngine = null;
		this.assetVector.removeAllElements();
		this.assetIdStringVector.removeAllElements();
	}
}
