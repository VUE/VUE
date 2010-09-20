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
 This class chains asset iterators together so they appear as one to the caller.
 */

public class AddingAssetIterator
implements org.osid.repository.AssetIterator
{
	private java.util.Vector iteratorVector = new java.util.Vector();
	private int iteratorIndex = 0;
	
	protected AddingAssetIterator()
	{
		
	}
	
	protected void add(org.osid.repository.AssetIterator assetIterator)
	{
		this.iteratorVector.addElement(assetIterator);
	}

	protected void reset()
	{
		iteratorIndex = 0;
	}
	
	public boolean hasNextAsset()
		throws org.osid.repository.RepositoryException
	{
		int numIterators = this.iteratorVector.size();
		for (int i = this.iteratorIndex; i < numIterators; i++) {
			org.osid.repository.AssetIterator assetIterator = (org.osid.repository.AssetIterator)this.iteratorVector.elementAt(iteratorIndex);
			if (assetIterator.hasNextAsset()) {
				return true;
			} else {
				iteratorIndex++;
			}
		}
		return false;
	}
	
	public org.osid.repository.Asset nextAsset()
		throws org.osid.repository.RepositoryException
	{
		org.osid.repository.AssetIterator assetIterator = (org.osid.repository.AssetIterator)this.iteratorVector.elementAt(iteratorIndex);
		return assetIterator.nextAsset();
	}
}