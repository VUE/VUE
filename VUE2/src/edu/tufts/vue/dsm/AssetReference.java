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
package edu.tufts.vue.dsm;

/**
 Repository OSID Assets may be present in VUE maps or other constructs.  Since the
 asset may have been fetched from a repository that required controlled access, we
 store information about how to fetch the asset again rather than the asset itself.
 Each asset has a unique id.  The load key helps the VUE OsidFactory or a similar
 utility in another application load the Repository that manages the Asset.
 */

public interface AssetReference
{
	public String getOsidLoadKey();

	public String getAssetIdString();
}
