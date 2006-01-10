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
 Each type of asset may need its own viewer.  Third parties can provide additional
 viewers.  To have a viewer mapped to a type, make an entry in the extensions in the
 users VUE folder.  The folder is within the user's home directory.  On Windows this 
 is vue, on Mac this is .vue.
 */

public interface AssetViewer
{
	public java.awt.Component viewAsset(org.osid.repository.Asset asset);

	public java.awt.Component viewAssets(org.osid.repository.AssetIterator assetIterator);
}
