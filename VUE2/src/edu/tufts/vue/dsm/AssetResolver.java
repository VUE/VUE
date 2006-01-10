package edu.tufts.vue.dsm;

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
Repository OSID Assets may be present in VUE maps or other constructs.  Since the
 asset may have been fetched from a repository that required controlled access, we
 store information about how to fetch the asset again rather than the asset itself.
 Each asset has a unique id.  The load key helps the VUE OsidFactory or a similar
 utility in another application load the Repository that manages the Asset.
 
 A resolver converts from an Asset's id and its Repository's id to a reference.  The
 reference has string values and so is easy to store.  Going in reverse, the resolver
 converts references to assets.  Note that while we can make a reference, there is
 no guarantee that we can make an asset either because the asset or its repository are
 no longer available or because the user has insufficient permission.
 */

public interface AssetResolver
{
	public AssetReference getAssetReference(org.osid.shared.Id assetId,
											org.osid.shared.Id repositoryId);

	public org.osid.repository.Asset getAsset(String osidLoadKey,
											  String assetIdString)
		throws org.osid.repository.RepositoryException;
}
