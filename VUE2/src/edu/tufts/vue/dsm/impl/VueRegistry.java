package edu.tufts.vue.dsm.impl;

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

public class VueRegistry
implements edu.tufts.vue.dsm.Registry
{
	private static edu.tufts.vue.dsm.Registry registry = new VueRegistry();
	private org.osid.registry.RegistryManager registryManager = null;
	
	/**
	*/
	public static edu.tufts.vue.dsm.Registry getInstance() {
		return registry;
	}
	
	/**
	*/
	private VueRegistry() {
		registryManager = VueOsidFactory.getInstance().getRegistryManagerInstance();
	}
	
	public org.osid.registry.Provider getProvider(org.osid.shared.Id providerId) {
		try {
			org.osid.registry.ProviderIterator providerIterator = registryManager.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.registry.Provider provider = providerIterator.nextProvider();
				if (providerId.isEqual(provider.getProviderId())) {
					return provider;
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"getting Provider in VueRegistry");
		}
		return null;
	}
	
	/**
	*/
	public org.osid.registry.Provider[] checkRegistryForNew(edu.tufts.vue.dsm.DataSource[] dataSources) {
		
		// find all the Provider Ids from the data sources passed in and check these agains what is registered		
		try {
			java.util.Vector idVector = new java.util.Vector();
			for (int i=0; i < dataSources.length; i++) {
				idVector.addElement(dataSources[i].getProviderId().getIdString());
			}
			
			org.osid.id.IdManager idManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance();
			java.util.Vector results = new java.util.Vector();
			org.osid.registry.ProviderIterator providerIterator = registryManager.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.registry.Provider provider = providerIterator.nextProvider();
				String providerIdString = provider.getProviderId().getIdString();
				
				int index = idVector.indexOf(providerIdString);
				if (index == -1) {
					//System.out.println("A new provider is available");
					results.addElement(provider);
				}
			}
			int size = results.size();
			org.osid.registry.Provider providers[] = new org.osid.registry.Provider[size];
			for (int i=0; i < size; i++) {
				providers[i] = (org.osid.registry.Provider)results.elementAt(i);
			}
			return providers;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"checking Reigstry for new in VueRegistry");
		}
		return null;
	}

	public org.osid.registry.Provider[] checkRegistryForUpdated(edu.tufts.vue.dsm.DataSource[] dataSources) {
		
		// find all the Provider Ids from the data sources passed in and check these agains what is registered		
		try {
			java.util.Vector idVector = new java.util.Vector();
			for (int i=0; i < dataSources.length; i++) {
				idVector.addElement(dataSources[i].getProviderId().getIdString());
			}
			
			java.util.Vector results = new java.util.Vector();
			org.osid.registry.ProviderIterator providerIterator = registryManager.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.registry.Provider provider = providerIterator.nextProvider();
				String providerIdString = provider.getProviderId().getIdString();
				
				int index = idVector.indexOf(providerIdString);
				if (index != -1) {
					// we have found a registered provider that matches one we already have; now check the dates
					java.util.Date registrationDate = edu.tufts.vue.util.Utilities.stringToDate(provider.getRegistrationDate());
					java.util.Date dataSourceDate = dataSources[index].getRegistrationDate();
					if (registrationDate.after(dataSourceDate)) {
						//System.out.println("The same provider with a newer date is available");
					} else {
						//System.out.println("The same provider already installed is available");
					}
					results.addElement(provider);
				}
			}
			int size = results.size();
			org.osid.registry.Provider providers[] = new org.osid.registry.Provider[size];
			for (int i=0; i < size; i++) {
				providers[i] = (org.osid.registry.Provider)results.elementAt(i);
			}
			return providers;
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"checking Reigstry for updated in VueRegistry");
		}
		return null;
	}
	
	/**
	*/
	public void download(edu.tufts.vue.dsm.DataSource dataSource) {
		
	}

	/**
	*/
	public org.osid.repository.Repository getRepository(org.osid.shared.Id providerId) {		
		try {
			// get the load key for this Registered provider
			org.osid.registry.ProviderIterator providerIterator = registryManager.getProviders();
			while (providerIterator.hasNextProvider()) {
				org.osid.registry.Provider provider = providerIterator.nextProvider();
				if (provider.getProviderId().isEqual(providerId)) {
					// get a Manager and then the appropriate Repository
					String loadKey = provider.getOsidLoadKey();
					org.osid.repository.RepositoryManager repositoryManager = 
						(org.osid.repository.RepositoryManager)VueOsidFactory.getInstance().getRepositoryManagerInstance(loadKey);
					org.osid.shared.Id repositoryId = edu.tufts.vue.util.Utilities.getRepositoryIdFromLoadKey(loadKey);
					org.osid.repository.Repository repository = repositoryManager.getRepository(repositoryId);
					return repository;
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Resolving a Repository from a load key");
		}
		return null;
	}	
}
