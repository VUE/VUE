package edu.tufts.vue.dsm.impl;

import junit.framework.*;

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

public class edsmTest extends TestCase
{
	private org.osid.OsidContext osidContext = new org.osid.OsidContext();
	private java.util.Properties properties = new java.util.Properties();
	private org.osid.registry.RegistryManager registryManager = null;	
	private String registryImplementation = null;
	private String idImplementation = null;
	private String providerIdString = "1ff1ebc5801080002e782010000102";

	public void testCreateProvider() {
		try {
			org.osid.id.IdManager idManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance();
			org.osid.registry.RegistryManager registryManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRegistryManagerInstance();
			
			java.util.Vector keywordVector = new java.util.Vector();
			keywordVector.addElement("Japan");
			keywordVector.addElement("Perry");
			
			java.util.Vector categoryVector = new java.util.Vector();
			categoryVector.addElement("Image Archive");
			
			java.util.Vector categoryTypeVector = new java.util.Vector();
			categoryTypeVector.addElement(new edu.tufts.vue.util.Type("edu.mit","providerCategory","image"));
			
			java.util.Vector rightVector = new java.util.Vector();
			rightVector.addElement("fair use");
			
			java.util.Vector rightTypeVector = new java.util.Vector();
			rightTypeVector.addElement(new edu.tufts.vue.util.Type("edu.mit","rights","fairUse"));
			
			java.util.Vector filenameVector = new java.util.Vector();
			filenameVector.addElement("VisualizingCultures.zip");
			
			java.util.Vector fileDisplayNameVector = new java.util.Vector();
			fileDisplayNameVector.addElement("Mac OSX Download");
			
			org.osid.shared.Id providerId = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(this.providerIdString);
			org.osid.shared.Id repositoryId = idManager.getId("VC.001");
/*			
			org.osid.registry.Provider provider = registryManager.createProvider(providerId,
																				 "Repository",
																				 2,
																				 0,
																				 "edu.mit.visualizingcultures.repository.blackships@VC.001",
																				 "Black Ships and Samurai",
																				 "Visualizing Cultures Content",
																				 keywordVector,
																				 categoryVector,
																				 categoryTypeVector,
																				 "Visualizing Cultures Image Database Project",
																				 "Massachusetts Institute of Technology",
																				 "www.blackshipsandsamurai.com",
																				 1,
																				 0,
																				 "2005-12-21 00:00:00:000 EST",
																				 "Jeff Kahn",
																				 "678.339.0249",
																				 "jeffkahn@mit.edu",
																				 "My license is here",
																				 rightVector,
																				 rightTypeVector,
																				 
																				 "Read this to know what is here",
																				 "Java 1.4",
																				 true,
																				 repositoryId,
																				 null,
																				 null,
																				 filenameVector,
																				 fileDisplayNameVector);
*/		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public void testEquals()
	{
		try {
			org.osid.shared.Id createProviderId = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getIdManagerInstance().getId(this.providerIdString);
			Assert.assertTrue(createProviderId != null);
			
			org.osid.registry.RegistryManager registryManager = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance().getRegistryManagerInstance();
			Assert.assertTrue(registryManager != null);
			
			org.osid.shared.Id newProviderId = registryManager.getProvider(createProviderId).getProviderId();
			Assert.assertTrue(newProviderId != null);
			
			Assert.assertTrue(createProviderId.isEqual(newProviderId));
			
/*
			edu.tufts.vue.dsm.DataSourceManager dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
			edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
			for (int i=0; i < dataSources.length; i++) {
				System.out.println("Data Source Found for Provider " + dataSources[i].getProviderDisplayName());
			}
*/
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
	}

	public static Test suite()
	{
		return new TestSuite(edsmTest.class);
	}
	
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}	
}