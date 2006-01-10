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
	public org.osid.registry.RegistryManager getRegistryManagerInstance();
	
	public org.osid.id.IdManager getIdManagerInstance();
	
	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey);
	
	public org.osid.repository.RepositoryManager getRepositoryManagerInstance(String osidLoadKey,
																			  org.osid.OsidContext context,
																			  java.util.Properties properties);
}
