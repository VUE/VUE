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

/**
 This class keeps a single copy of a Registry Manager and an Id Manager.  The
 specific packagename for the implementations is drawn from VueResource properties.
 
 To return an instance of a Repository, we also needs its Manager.  The Osid Load
 Key contains both.  Both Managers and Repositories are cache in this singleton so 
 that we only instantiate a particular instance once.

 Note that we are using a custom OsidLoader (not what was released by O.K.I for v2.0).
 This laoder looks for jars, etc in a special set of directories on the user's machine 
 rather that one the VUE classpath.
 */

public class VueOsidFactory
implements edu.tufts.vue.dsm.OsidFactory
{
	private static org.osid.OsidContext osidContext = new org.osid.OsidContext();
	private static java.util.Properties properties = new java.util.Properties();
	private static java.util.Vector keyVector = new java.util.Vector();
	private static java.util.Vector managerVector = new java.util.Vector();
	private static org.osid.registry.RegistryManager registryManager = null;
	private static org.osid.id.IdManager idManager = null;
	
	private static edu.tufts.vue.dsm.OsidFactory osidFactory = new VueOsidFactory();
	
	public static edu.tufts.vue.dsm.OsidFactory getInstance()
	{
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
	
	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey)
	{
		int index = keyVector.indexOf(osidLoadKey);
		if (index != -1) {
			return (org.osid.repository.RepositoryManager)managerVector.elementAt(index);
		}
		
		org.osid.repository.RepositoryManager manager = null;
		String managerImplementation = null;
		try {
			managerImplementation = edu.tufts.vue.util.Utilities.getManagerStringFromLoadKey(osidLoadKey);
			manager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																							managerImplementation,
																							osidContext,
																							properties);
			managerVector.addElement(manager);
			keyVector.addElement(osidLoadKey);																				  
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to load Repository Manager in factory with key " + osidLoadKey);
			try {
				manager = (org.osid.repository.RepositoryManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																										  managerImplementation,
																										  osidContext,
																										  properties);
				managerVector.addElement(manager);
				keyVector.addElement(osidLoadKey);				
			} catch (Throwable t1) {
				edu.tufts.vue.util.Logger.log(t1,"Trying to load (alternate) Repository Manager in factory with key " + osidLoadKey);
			}
		}
		return manager;
	}

	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey,
																			  org.osid.OsidContext context,
																			  java.util.Properties props) {
		String managerImplementation = edu.tufts.vue.util.Utilities.getManagerStringFromLoadKey(osidLoadKey);
		int index = keyVector.indexOf(osidLoadKey);
		if (index != -1) {
			return (org.osid.repository.RepositoryManager)managerVector.elementAt(index);
		}
		org.osid.repository.RepositoryManager manager = null;
		try {
			manager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																							managerImplementation,
																							context,
																							props);
			managerVector.addElement(manager);
			keyVector.addElement(osidLoadKey);
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"Trying to load Repository Manager in factory with key " + osidLoadKey);
			try {
				manager = (org.osid.repository.RepositoryManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.repository.RepositoryManager",
																										  managerImplementation,
																										  osidContext,
																										  properties);
				managerVector.addElement(manager);
				keyVector.addElement(osidLoadKey);				
			} catch (Throwable t1) {
				edu.tufts.vue.util.Logger.log(t,"Trying to load (alternate) Repository Manager in factory with key " + osidLoadKey);
			}
		}
		return manager;
	}
	
	public org.osid.registry.RegistryManager getRegistryManagerInstance()
	{
		if (registryManager == null) {
			String registryImplementation = null;
			try {
				registryImplementation = tufts.vue.VueResources.getString("OSIDRegistryManager-2.0");
				if (registryImplementation != null) {
					registryManager = (org.osid.registry.RegistryManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.registry.RegistryManager",
																												  registryImplementation,
																												  osidContext,
																												  properties);
				}
			} catch (Throwable t) {
				edu.tufts.vue.util.Logger.log(t,"Trying to load Registry Manager in factory");
				try {
					registryManager = (org.osid.registry.RegistryManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.registry.RegistryManager",
																												  registryImplementation,
																												  osidContext,
																												  properties);
				} catch (Throwable t1) {
					edu.tufts.vue.util.Logger.log(t,"Trying to load (alternate) Registry Manager in factory");
				}
			}
		}		
		return registryManager;
	}

	public org.osid.id.IdManager getIdManagerInstance()
	{
		if (idManager == null) {
			String idImplementation = null;
			try {
				idImplementation = tufts.vue.VueResources.getString("OSIDIdManager-2.0");
				if (idImplementation != null) {
					idManager = (org.osid.id.IdManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.id.IdManager",
																								idImplementation,
																								osidContext,
																								properties);
				}
			} catch (Throwable t) {
				edu.tufts.vue.util.Logger.log(t,"Trying to load Registry Manager in factory");
				try {
					idManager = (org.osid.id.IdManager)edu.tufts.vue.util.OsidLoader.getManager("org.osid.id.IdManager",
																								idImplementation,
																								osidContext,
																								properties);
				} catch (Throwable t1) {
					edu.tufts.vue.util.Logger.log(t,"Trying to load (alternate) Id Manager in factory");
				}
			}
		}		
		return idManager;
	}	
}
