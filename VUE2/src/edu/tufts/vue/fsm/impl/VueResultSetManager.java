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
package edu.tufts.vue.fsm.impl;

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
	
	private AddingAssetIterator ai = null;
	
	protected VueResultSetManager(edu.tufts.vue.fsm.SearchEngine searchEngine)
	{
		this.searchEngine = searchEngine;
	}

	public java.awt.Image getPreview(org.osid.shared.Id assetId) {
		return null;
	}

	public org.osid.repository.AssetIterator getAssets(String foreignIdString)
	{
		ai = new AddingAssetIterator();
		ai.add(searchEngine.getAssetIterator(foreignIdString));
		return ai;
	}
	
	public org.osid.repository.AssetIterator getAssets()
	{
		if (ai == null) {
			ai = new AddingAssetIterator();
			int numSearches = this.searchEngine.getNumSearches();
			for (int i=0; i < numSearches; i++) {
				ai.add(searchEngine.getAssetIterator(i));
			}
		} else {
			ai.reset();
		}
		return ai;
	}

	public String getExceptionMessage(int index)
	{
		return searchEngine.getExceptionMessage(index);
	}
	
	public org.osid.repository.AssetIterator getAsset(int i)
	{
		return searchEngine.getAssetIterator(i);
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
