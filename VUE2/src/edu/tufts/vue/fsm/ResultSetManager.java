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
 This interface provides all the raw material a caller needs to present results to
 a user.
 */

public interface ResultSetManager
{
	public java.awt.Image getPreview(org.osid.shared.Id assetId);

	public org.osid.repository.AssetIterator getAssets();
	
	public org.osid.repository.AssetIterator getAsset(int i);

	public void addAsset(org.osid.repository.Asset asset);
	
	public boolean isComplete();

	public void removeAsset(org.osid.repository.Asset assetId);
	
	public void clearResults();
}
