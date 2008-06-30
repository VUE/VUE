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
package edu.tufts.vue.dsm.impl;

public class VueAssetReference
implements edu.tufts.vue.dsm.AssetReference
{
	private String osidLoadKey = null;
	private String assetIdString = null;
	
	public VueAssetReference(String osidLoadKey,
							 String assetIdString) {

		this.osidLoadKey = osidLoadKey;
		this.assetIdString = assetIdString;
	}

	/**
	*/
	public String getOsidLoadKey() {
		return null;
	}

	/**
	*/
	public String getAssetIdString() {
		return null;
	}
}
