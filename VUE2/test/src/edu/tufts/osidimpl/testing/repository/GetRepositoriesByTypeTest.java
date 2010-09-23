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

package edu.tufts.osidimpl.testing.repository;

import junit.framework.TestCase;

public class GetRepositoriesByTypeTest extends TestCase
{
	public GetRepositoriesByTypeTest(org.osid.repository.RepositoryManager repositoryManager, org.w3c.dom.Document document)
		throws org.osid.repository.RepositoryException, org.xml.sax.SAXParseException
	{
		// are their repositories to test for?
		org.w3c.dom.NodeList repositoriesNodeList = document.getElementsByTagName(OsidTester.REPOSITORIES_BY_TYPE_TAG);
		int numRepositories = repositoriesNodeList.getLength();
		for (int i=0; i < numRepositories; i++) {
			org.w3c.dom.Element repositoryElement = (org.w3c.dom.Element)repositoriesNodeList.item(i);
			String autoString = repositoryElement.getAttribute(OsidTester.AUTO_ATTR);
			
			// currently only supports the auto case
			String idString = null;
			if (autoString.trim().toLowerCase().equals("true")) {
				// store repositories and their types, and the unique set of types
				org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
				java.util.Vector repositoryIdStringVector = new java.util.Vector();
				java.util.Vector repositoryTypeVector = new java.util.Vector();
				java.util.Vector distinctRepositoryTypeVector = new java.util.Vector();
				while (repositoryIterator.hasNextRepository()) {
					org.osid.repository.Repository repository = repositoryIterator.nextRepository();
					try {
						idString = repository.getId().getIdString();
						repositoryIdStringVector.addElement(idString);
					} catch (Throwable t) {
					}
					String typeString = Utilities.typeToString(repository.getType());
					repositoryTypeVector.addElement(typeString);
					if (!distinctRepositoryTypeVector.contains(typeString)) {
						distinctRepositoryTypeVector.addElement(typeString);
					}
				}
				
				// test for each type that 
				for (int j=0, size = distinctRepositoryTypeVector.size(); j < size; j++) {
					String typeString = (String)distinctRepositoryTypeVector.elementAt(j);
					
					// what ids do we expect?
					java.util.Vector idStringVector = new java.util.Vector();
					int startIndex = 0;
					int index = 0;
					while ( (index = repositoryTypeVector.indexOf(typeString,startIndex)) != -1 ) {
						idStringVector.addElement(repositoryIdStringVector.elementAt(index));
						startIndex = index + 1;												  
					}
					
					// what ids do we find?
					org.osid.shared.Type type = Utilities.stringToType(typeString);
					org.osid.repository.RepositoryIterator repositoryIterator2 = repositoryManager.getRepositoriesByType(type);
					while (repositoryIterator2.hasNextRepository()) {
						org.osid.repository.Repository repository = repositoryIterator2.nextRepository();
						try {
							idString = repository.getId().getIdString();
							if (!repositoryIdStringVector.contains(idString)) {
								fail("No repository with id " + idString + " found when getting repositories by type " + typeString);
							}
						} catch (Throwable t) {
						}
					}
					System.out.println("PASSED: Repository By Type " + typeString);
				}
			}
		}
	}
}