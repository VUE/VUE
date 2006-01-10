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