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
package edu.tufts.vue.dsm;

/**
 At several junctures, VUE needs to get an instance of the Registry Manager, the
 Id Manager, or a particular Repository Manager.  An implementation of this interface
 hides the detail of instance loading, caching, etc.  Note that the OsidLoader that
 was included with the O.K.I. OSID v2.0, made certain assumptions.  Introducing this
 factory allows the implementation to use the v2.0 OsidLoader as is, or another
 loader.
 
 The single argument form of getRepositoryManagerInstance() uses an empty OsidContext
 and Properties.
 */

public interface OsidFactory
{
	public org.osid.id.IdManager getIdManagerInstance()
	throws org.osid.OsidException;
	
	public org.osid.provider.Provider getProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException;

	public String getResourcePath(String resourceName)
		throws org.osid.provider.ProviderException;

	public org.osid.provider.ProviderIterator getProviders()
		throws org.osid.provider.ProviderException;

	public org.osid.provider.Provider getInstalledProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException;

	public org.osid.provider.ProviderIterator getInstalledProviders()
		throws org.osid.provider.ProviderException;
	
	public org.osid.provider.ProviderIterator getProvidersNeedingUpdate()
		throws org.osid.provider.ProviderException;
	
	public org.osid.provider.Provider[] checkRegistryForNew(DataSource[] dataSources)
		throws org.osid.provider.ProviderException;

	public org.osid.provider.Provider[] checkRegistryForUpdates(DataSource[] dataSources)
		throws org.osid.provider.ProviderException;

	public void installProvider(org.osid.shared.Id providerId)
        throws org.osid.provider.ProviderException;
	
	public void updateProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException;

	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey);
	
	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey,
																			  org.osid.OsidContext context,
																			  java.util.Properties properties);
}
