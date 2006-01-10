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
 The Registry OSID has a Provider object.  This simplified registry hides the Registry
 OSID implementation and offers only what is needed by the VUE UI or other code.
*/

public interface Registry
{
	/**
	 A new provider is one that is registered by the O.K.I. Community but not currently
	 installed in the user's VUE.
	*/
	public org.osid.registry.Provider[] checkRegistryForNew(DataSource[] dataSources);

	/**
	 An updated provider is one that is both registered by the O.K.I. Community and 
	 installed in the user's VUE.  The provider has a release date after the date of the
	 version in VUE.
	 */
	public org.osid.registry.Provider[] checkRegistryForUpdated(DataSource[] dataSources);

	/**
	 Downlaoding a data source may involve different operations depending on the user's
	 environment and the type of provider.
	 */
	public void download(DataSource dataSource);
	
	public org.osid.registry.Provider getProvider(org.osid.shared.Id providerId);
	
	public org.osid.repository.Repository getRepository(org.osid.shared.Id repositoryId);
}
