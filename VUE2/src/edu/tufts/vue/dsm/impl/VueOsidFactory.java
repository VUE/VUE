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

/**
 This class keeps a single copy of a Provider Manager and an Id Manager.  The
 specific packagename for the implementations is drawn from VueResource properties.
 */

import java.util.Properties;
import org.osid.*;
import org.osid.provider.*;
import org.osid.shared.*;

public class VueOsidFactory
implements edu.tufts.vue.dsm.OsidFactory
{
	private static org.osid.OsidContext osidContext = new org.osid.OsidContext();
	private static java.util.Properties properties = new java.util.Properties();
	private static java.util.Vector keyVector = new java.util.Vector();
	private static java.util.Vector managerVector = new java.util.Vector();
    private static ProviderControlManager providerControlManager;
    private static ProviderInvocationManager providerInvocationManager;
    private static ProviderInstallationManager providerInstallationManager;
    private static ProviderLookupManager providerLookupManager;
	private static org.osid.id.IdManager idManager = null;
	private static org.osid.registry.RegistryManager registryManager = null;
	
	private static edu.tufts.vue.dsm.OsidFactory osidFactory = new VueOsidFactory();
	
	public static edu.tufts.vue.dsm.OsidFactory getInstance()
		throws org.osid.provider.ProviderException
	{
		if (providerControlManager == null) {
			try {
				osidContext.assignContext("com.harvestroad.authentication.username","vue");
				osidContext.assignContext("com.harvestroad.authentication.password","vue");
				osidContext.assignContext("com.harvestroad.authentication.host","bazzim.mit.edu");
				osidContext.assignContext("com.harvestroad.authentication.port","80");
			} catch (OsidException e) {
				edu.tufts.vue.util.Logger.log("Assigning to context: this error should never happen");
				throw new org.osid.provider.ProviderException(org.osid.OsidException.CONFIGURATION_ERROR);
			}
			try {
				providerControlManager = (ProviderControlManager) edu.mit.osidimpl.OsidLoader.getManager("org.osid.provider.ProviderControlManager", 
																										 "edu.mit.osidimpl.provider.repository",
																										 osidContext, 
																										 new Properties());
				providerInvocationManager = providerControlManager.getProviderInvocationManager();
				providerLookupManager = providerControlManager.getProviderLookupManager();
				providerInstallationManager = providerControlManager.getProviderInstallationManager();
			} catch (OsidException e) {
				edu.tufts.vue.util.Logger.log("Cannot load ProviderInvocationManager: " + e.getMessage());
				throw new org.osid.provider.ProviderException(org.osid.OsidException.CONFIGURATION_ERROR);
			}
		}
		return osidFactory;
	}
	
	public static void setOsidLoaderOsidContext(org.osid.OsidContext context)
	{
		osidContext = context;
	}
	
	public static void setOsidLoaderProperties(java.util.Properties props)
	{
		properties = props;
	}

	public void installProvider(org.osid.shared.Id providerId)
        throws org.osid.provider.ProviderException
	{
		providerInstallationManager.installProvider(providerId);
	}
	
	public void updateProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException
	{
		providerInstallationManager.updateInstalledProvider(providerId);
	}

	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey)
	{
		int index = keyVector.indexOf(osidLoadKey);
		if (index != -1) {
			//return (org.osid.repository.RepositoryManager)managerVector.elementAt(index);
		}
		
		org.osid.repository.RepositoryManager manager = null;
		String managerImplementation = null;
		try {
			managerImplementation = edu.tufts.vue.util.Utilities.getManagerStringFromLoadKey(osidLoadKey);
			//System.out.println("Manager implementation is " + managerImplementation);
			manager = (org.osid.repository.RepositoryManager)providerInvocationManager.getManager("org.osid.repository.RepositoryManager",
																								  managerImplementation,
																								  osidContext,
																								  properties);
			managerVector.addElement(manager);
			keyVector.addElement(osidLoadKey);																				  
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to load Repository Manager in factory with key " + osidLoadKey);
		}
		return manager;
	}

	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey,
																			  org.osid.OsidContext context,
																			  java.util.Properties props)
	{
		String managerImplementation = edu.tufts.vue.util.Utilities.getManagerStringFromLoadKey(osidLoadKey);
		int index = keyVector.indexOf(osidLoadKey);
		if (index != -1) {
			//return (org.osid.repository.RepositoryManager)managerVector.elementAt(index);
		}
		org.osid.repository.RepositoryManager manager = null;
		try {
			manager = (org.osid.repository.RepositoryManager)providerInvocationManager.getManager("org.osid.repository.RepositoryManager",
																								  managerImplementation,
																								  context,
																								  props);
			managerVector.addElement(manager);
			keyVector.addElement(osidLoadKey);
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to load Repository Manager in factory with key " + osidLoadKey);
		}
		return manager;
	}
	
	public org.osid.provider.Provider getProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException
	{
		return providerLookupManager.getProvider(providerId);
	}
	
	public String getResourcePath(String resourceName)
		throws org.osid.provider.ProviderException
	{
		return providerInvocationManager.getResourcePath(resourceName);
	}
	
	public ProviderIterator getProviders()
		throws org.osid.provider.ProviderException
	{
		return providerLookupManager.getProviders();
	}
	
	public Provider getInstalledProvider(org.osid.shared.Id providerId)
		throws org.osid.provider.ProviderException
	{
		return providerInstallationManager.getInstalledProvider(providerId);
	}

	public ProviderIterator getInstalledProviders()
		throws org.osid.provider.ProviderException
	{
		return providerInstallationManager.getInstalledProviders();
	}
	
	public org.osid.provider.ProviderIterator getProvidersNeedingUpdate()
		throws org.osid.provider.ProviderException
	{
		return providerInstallationManager.getInstalledProvidersNeedingUpdate();
	}
	
	
	public org.osid.provider.Provider[] checkRegistryForNew(edu.tufts.vue.dsm.DataSource[] dataSources)
		throws org.osid.provider.ProviderException
	{
		java.util.Vector results = new java.util.Vector();
		try {
			java.util.Vector idVector = new java.util.Vector();
			for (int i=0; i < dataSources.length; i++) {
				idVector.addElement(dataSources[i].getProviderId().getIdString());
			}

			ProviderIterator providerIterator = providerLookupManager.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				String providerIdString = nextProvider.getId().getIdString();
				
				int index = idVector.indexOf(providerIdString);
				if (index == -1) {
					System.out.println("A new provider is available " + nextProvider.getDisplayName());
					results.addElement(nextProvider);
				}
			}		
			int size = results.size();
			org.osid.provider.Provider providers[] = new org.osid.provider.Provider[size];
			for (int i=0; i < size; i++) {
				providers[i] = (org.osid.provider.Provider)results.elementAt(i);
			}
			return providers;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
			throw new org.osid.provider.ProviderException(t.getMessage());
		}
	}
	
	public org.osid.provider.Provider[] checkRegistryForUpdates(edu.tufts.vue.dsm.DataSource[] dataSources)
		throws org.osid.provider.ProviderException
	{
		java.util.Vector results = new java.util.Vector();
		try {
			java.util.Vector idVector = new java.util.Vector();
			for (int i=0; i < dataSources.length; i++) {
				idVector.addElement(dataSources[i].getProviderId().getIdString());
			}
			
			ProviderIterator providerIterator = providerInstallationManager.getInstalledProvidersNeedingUpdate();
			while (providerIterator.hasNextProvider()) {
				org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				String providerIdString = nextProvider.getId().getIdString();
				
				int index = idVector.indexOf(providerIdString);
				if (index == -1) {
					System.out.println("A provider update is available " + nextProvider.getDisplayName());
					results.addElement(nextProvider);
				}
			}		
			int size = results.size();
			org.osid.provider.Provider providers[] = new org.osid.provider.Provider[size];
			for (int i=0; i < size; i++) {
				providers[i] = (org.osid.provider.Provider)results.elementAt(i);
			}
			return providers;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t);
			throw new org.osid.provider.ProviderException(t.getMessage());
		}
	}
	
	public org.osid.id.IdManager getIdManagerInstance()
		throws org.osid.OsidException
	{
		if (idManager == null) {
			String idImplementation = null;
			idImplementation = tufts.vue.VueResources.getString("OSIDIdManager-2.0");
			if (idImplementation != null) {
				idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																				  idImplementation,
																				  osidContext,
																				  properties);
			}
		}		
		return idManager;
	}	
}
