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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.tufts.osidimpl.testing.sakai;

public class SakaiRMLoader implements edu.tufts.osidimpl.testing.ContextObjectGetter
{
	public java.io.Serializable getContextObject()
	{
		try {
			java.util.Properties properties = new java.util.Properties();
			properties.setProperty("sakaiUsername","admin");
			properties.setProperty("sakaiPassword","admin");
			properties.setProperty("sakaiHost","http://localhost");
			properties.setProperty("sakaiPort","8080");
			
			org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager(
				"org.osid.repository.RepositoryManager",
				"edu.tufts.osidimpl.repository.sakai",
				new org.osid.OsidContext(),
				properties);
			System.out.println("Sakai Repository OSID Impl Loaded");
			return repositoryManager;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
}
