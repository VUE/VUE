/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
