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
package edu.tufts.vue.fsm.impl;

/**
 This Federated Search Manager works with data in the users extensions file to
 return type-specific UI controls and adjusters.  The result set manager is handed
 a set of queries.  That manager calls the search engine.
 */

public class VueFederatedSearchManager
implements edu.tufts.vue.fsm.FederatedSearchManager
{
	private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager = VueSourcesAndTypesManager.getInstance();
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueFederatedSearchManager.class);
	
	private static final String FILE_NOT_FOUND_MESSAGE = "Cannot find or open ";
	private static final String EXTENSIONS_TAG = "extensions";
	private static final String QUERY_EDITORS_TAG = "queryeditors";
	private static final String QUERY_EDITOR_TAG = "queryeditor";
	private static final String SEARCH_TYPE_TAG = "searchtype";
	private static final String CLASS_NAME_TAG = "classname";
	private static final String ASSET_VIEWERS_TAG = "assetviewers";
	private static final String ASSET_VIEWER_TAG = "assetviewer";
	private static final String ASSET_TYPE_TAG = "assettype";
	private static final String QUERY_ADJUSTERS_TAG = "queryadjusters";
	private static final String QUERY_ADJUSTER_TAG = "queryadjuster";
	private static final String REPOSITORY_ID_TAG = "repositoryid";
	
	private static final String DEFAULT_SEARCH_TYPE = "search/keyword@mit.edu";
	private static final String DEFAULT_CLASS_NAME = "edu.tufts.vue.ui.DefaultQueryEditor";

	private static final String ARTIFACT_SEARCH_TYPE = "search/artifact@tufts.edu";
	private static final String ARTIFACT_CLASS_NAME = "edu.tufts.artifact.ui.ArtifactQueryEditor";
	
	
	private static edu.tufts.vue.fsm.FederatedSearchManager federatedSearchManager = new VueFederatedSearchManager();
	
	private java.util.Vector queryEditorTypeVector = new java.util.Vector();
	private java.util.Vector queryEditorClassNameVector = new java.util.Vector();
	private java.util.Vector queryAdjusterRepositoryIdStringVector = new java.util.Vector();
	private java.util.Vector queryAdjusterClassNameVector = new java.util.Vector();
	private java.util.Vector assetViewerTypeVector = new java.util.Vector();
	private java.util.Vector assetViewerClassNameVector = new java.util.Vector();

	private static String xmlFilename = null;

	public static edu.tufts.vue.fsm.FederatedSearchManager getInstance() {
		return federatedSearchManager;
	}
	
	/*
	 Look in directories beneath the install directory for an extensions file.
	 */
	
	private String[] getXMLFilenames()
	{
		java.util.Vector filenameVector = new java.util.Vector();
		try {
			String targetFilename = tufts.vue.VueResources.getString("extensionsSaveToXmlFilename");
			//System.out.println("target filename " + targetFilename);
			String installDirectory = tufts.vue.VueResources.getString("dataSourceInstallDirectory");
			//System.out.println("install Directory " + installDirectory);
                        Log.info("listing " + installDirectory + " to find \"" + targetFilename + "\"");
			java.io.File root = new java.io.File(installDirectory);
			java.io.File[] files = root.listFiles();
			if (files != null) {
				for (int i=0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						java.io.File[] subfiles = files[i].listFiles();
						for (int j=0; j < subfiles.length; j++) {
							if (subfiles[j].getName().equals(targetFilename)) {
								filenameVector.addElement(subfiles[j].getAbsolutePath());
								//System.out.println("added " + filenameVector.lastElement());
							}
						}
					}
				}			
			}
		} catch (Exception ex) {
                    Log.warn(ex);
                    edu.tufts.vue.util.Logger.log(ex);
		}

		// add the default file in the user's home folder
		java.io.File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
		filenameVector.addElement(userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("extensionsSaveToXmlFilename"));
		
		int size = filenameVector.size();
		String result[] = new String[size];
		for (int i=0; i < size; i++) {
			result[i] = (String)filenameVector.elementAt(i);
		}
		return result;
	}

	private VueFederatedSearchManager() {
		try {
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			org.w3c.dom.Document document = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();

			java.io.File userFolder = tufts.vue.VueUtil.getDefaultUserFolder();
			if (!userFolder.exists()) {
				userFolder.mkdir();
			}
			this.xmlFilename = userFolder.getAbsolutePath() + "/" + tufts.vue.VueResources.getString("extensionsSaveToXmlFilename");
			java.io.File file = new java.io.File(this.xmlFilename);
            if (!file.exists()) {
				// make a minmal one
				document = db.newDocument();				
				
				org.w3c.dom.Element extensions = document.createElement(EXTENSIONS_TAG);
				org.w3c.dom.Element editors = document.createElement(QUERY_EDITORS_TAG);
				
				org.w3c.dom.Element editor = document.createElement(QUERY_EDITOR_TAG);
				org.w3c.dom.Element searchtype = document.createElement(SEARCH_TYPE_TAG);
				searchtype.appendChild(document.createTextNode(DEFAULT_SEARCH_TYPE));
				org.w3c.dom.Element classname = document.createElement(CLASS_NAME_TAG);
				classname.appendChild(document.createTextNode(DEFAULT_CLASS_NAME));
				editor.appendChild(classname);
				editor.appendChild(searchtype);
				editors.appendChild(editor);
				
				org.w3c.dom.Element artifactEditor = document.createElement(QUERY_EDITOR_TAG);
				org.w3c.dom.Element artifactSearchtype = document.createElement(SEARCH_TYPE_TAG);
				artifactSearchtype.appendChild(document.createTextNode(ARTIFACT_SEARCH_TYPE));
				org.w3c.dom.Element artifactClassname = document.createElement(CLASS_NAME_TAG);
				artifactClassname.appendChild(document.createTextNode(ARTIFACT_CLASS_NAME));
				artifactEditor.appendChild(classname);
				artifactEditor.appendChild(searchtype);
				editors.appendChild(artifactEditor);
				
				extensions.appendChild(editors);				
				document.appendChild(extensions);
				
				javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
				javax.xml.transform.Transformer transformer = tf.newTransformer();
				java.util.Properties properties = new java.util.Properties();
				properties.put("indent","yes");
				transformer.setOutputProperties(properties);
				javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
				javax.xml.transform.stream.StreamResult result = 
					new javax.xml.transform.stream.StreamResult (this.xmlFilename);
				transformer.transform(domSource,result);
            }
			
			String files[] = getXMLFilenames();
			for (int f=0; f < files.length; f++) 
			{
				java.io.InputStream istream = new java.io.FileInputStream(files[f]);
				document = db.parse(istream);
				
				org.w3c.dom.NodeList queryEditors = document.getElementsByTagName(QUERY_EDITORS_TAG);
				int numQueryEditors = queryEditors.getLength();
				for (int i=0; i < numQueryEditors; i++) {
					org.w3c.dom.Element queryEditor = (org.w3c.dom.Element)queryEditors.item(i);
					
					org.w3c.dom.NodeList typeNodeList = queryEditor.getElementsByTagName(SEARCH_TYPE_TAG);
					int numTypes = typeNodeList.getLength();
					
					org.w3c.dom.NodeList classNameNodeList = queryEditor.getElementsByTagName(CLASS_NAME_TAG);
					int numClassNames = classNameNodeList.getLength();
					
					for (int j=0; j < numTypes; j++) {
						org.w3c.dom.Element typeElement = (org.w3c.dom.Element)typeNodeList.item(j);
						if (typeElement.hasChildNodes()) {
							queryEditorTypeVector.addElement(typeElement.getFirstChild().getNodeValue());
							
							// ensure there is some entry in the vector
							if (numClassNames >= j) {
								org.w3c.dom.Element classNameElement = (org.w3c.dom.Element)classNameNodeList.item(j);
								if (classNameElement.hasChildNodes()) {
									queryEditorClassNameVector.addElement(classNameElement.getFirstChild().getNodeValue());
								} else {
									queryEditorClassNameVector.addElement(null);
								}
							} else {
								queryEditorClassNameVector.addElement(null);
							}
						}
					}
				}
				
				org.w3c.dom.NodeList queryAdjusters = document.getElementsByTagName(QUERY_ADJUSTERS_TAG);
				int numQueryAdjusters = queryAdjusters.getLength();
				for (int i=0; i < numQueryAdjusters; i++) {
					org.w3c.dom.Element queryAdjuster = (org.w3c.dom.Element)queryAdjusters.item(i);
					
					org.w3c.dom.NodeList repositoryIdNodeList = queryAdjuster.getElementsByTagName(REPOSITORY_ID_TAG);
					int numRepositoryIds = repositoryIdNodeList.getLength();
					
					org.w3c.dom.NodeList classNameNodeList = queryAdjuster.getElementsByTagName(CLASS_NAME_TAG);
					int numClassNames = classNameNodeList.getLength();
					
					for (int j=0; j < numRepositoryIds; j++) {
						org.w3c.dom.Element repositoryIdElement = (org.w3c.dom.Element)repositoryIdNodeList.item(j);
						if (repositoryIdElement.hasChildNodes()) {
							queryAdjusterRepositoryIdStringVector.addElement(repositoryIdElement.getFirstChild().getNodeValue());
							
							// ensure there is some entry in the vector
							if (numClassNames >= j) {
								org.w3c.dom.Element classNameElement = (org.w3c.dom.Element)classNameNodeList.item(j);
								if (classNameElement.hasChildNodes()) {
									queryAdjusterClassNameVector.addElement(classNameElement.getFirstChild().getNodeValue());
								} else {
									queryAdjusterClassNameVector.addElement(null);
								}
							} else {
								queryAdjusterClassNameVector.addElement(null);
							}
						}
					}
				}
				
				org.w3c.dom.NodeList assetViewers = document.getElementsByTagName(ASSET_VIEWERS_TAG);
				int numAssetViewers = assetViewers.getLength();
				for (int i=0; i < numAssetViewers; i++) {
					org.w3c.dom.Element assetViewer = (org.w3c.dom.Element)assetViewers.item(i);
					
					org.w3c.dom.NodeList typeNodeList = assetViewer.getElementsByTagName(ASSET_TYPE_TAG);
					int numTypes = typeNodeList.getLength();
					
					org.w3c.dom.NodeList classNameNodeList = assetViewer.getElementsByTagName(CLASS_NAME_TAG);
					int numClassNames = classNameNodeList.getLength();
					
					for (int j=0; j < numTypes; j++) {
						org.w3c.dom.Element typeElement = (org.w3c.dom.Element)typeNodeList.item(j);
						if (typeElement.hasChildNodes()) {
							assetViewerTypeVector.addElement(typeElement.getFirstChild().getNodeValue());
							
							// ensure there is some entry in the vector
							if (numClassNames >= j) {
								org.w3c.dom.Element classNameElement = (org.w3c.dom.Element)classNameNodeList.item(j);
								if (classNameElement.hasChildNodes()) {
									assetViewerClassNameVector.addElement(classNameElement.getFirstChild().getNodeValue());
								} else {
									assetViewerClassNameVector.addElement(null);
								}
							} else {
								assetViewerClassNameVector.addElement(null);
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"parsing " + this.xmlFilename);
		}
	}
	
	/*
	 Look in a specific XML file for the query editors element.  Within this are query editor elements with a search type and 
	 a class name.  If the search type matchs, try and load and return a class from the class name.
	 */
	public edu.tufts.vue.fsm.QueryEditor getQueryEditorForType(org.osid.shared.Type searchType) {
		try {
			if (searchType == null) {
				// load a default
				Class c = Class.forName("edu.tufts.vue.ui.DefaultQueryEditor");
				return (edu.tufts.vue.fsm.QueryEditor)c.newInstance();			
			} else {
				String searchTypeString = edu.tufts.vue.util.Utilities.typeToString(searchType);
				int startIndex = 0;
				int index = -1;
				while ( (index = queryEditorTypeVector.indexOf(searchTypeString,startIndex)) != -1) {
					String className = (String)queryEditorClassNameVector.elementAt(index);
					//TODO: we need to find this via Provider
					try {
						Class c = Class.forName(className);
						return (edu.tufts.vue.fsm.QueryEditor)c.newInstance();
					} catch (Throwable t) {
						edu.tufts.vue.util.Logger.log(t,"failed to load class " + className);
						// keep looking
						startIndex = index + 1;
					}
				}
				
				// load a default
				Class c = Class.forName("edu.tufts.vue.ui.DefaultQueryEditor");
				return (edu.tufts.vue.fsm.QueryEditor)c.newInstance();			
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"looking for a query editor");
		}
		return null;
	}
	
	public edu.tufts.vue.fsm.QueryAdjuster getQueryAdjusterForRepository(org.osid.shared.Id repositoryId)
	{
		try {
			String repositoryIdString = repositoryId.getIdString();
			int startIndex = 0;
			int index = -1;
			while ( (index = queryAdjusterRepositoryIdStringVector.indexOf(repositoryIdString,startIndex)) != -1) {
				String className = (String)queryAdjusterClassNameVector.elementAt(index);
				//TODO: we need to find this via Provider
				try {
					Class c = Class.forName(className);
					return (edu.tufts.vue.fsm.QueryAdjuster)c.newInstance();
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t,"failed to load class " + className);
					// keep looking
					startIndex = index + 1;
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"looking for a query adjuster");
		}
		return null;
	}
	
	public edu.tufts.vue.fsm.AssetViewer getAssetViewerForType(org.osid.shared.Type assetType) {
		try {
			String assetTypeString = edu.tufts.vue.util.Utilities.typeToString(assetType);
			int startIndex = 0;
			int index = -1;
			
			while ( (index = assetViewerTypeVector.indexOf(assetTypeString,startIndex)) != -1) {
				String className = (String)assetViewerClassNameVector.elementAt(index);
				//TODO: we need to find this via Provider
				try {
					Class c = Class.forName(className);
					return (edu.tufts.vue.fsm.AssetViewer)c.newInstance();
				} catch (Throwable t) {
					edu.tufts.vue.util.Logger.log(t,"failed to load class " + className);
					// keep looking
					startIndex = index + 1;
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"looking for a asset editor");
		}
		return null;
	}
	
	public edu.tufts.vue.fsm.ResultSetManager getResultSetManager(java.io.Serializable searchCriteria,
																  org.osid.shared.Type searchType,
																  org.osid.shared.Properties searchProperties) {
		try {
			edu.tufts.vue.fsm.SearchEngine searchEngine = new VueSearchEngine();

			org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
			edu.tufts.vue.dsm.DataSource[] dataSources = sourcesAndTypesManager.getDataSourcesToSearch(); // will be same length
			edu.tufts.vue.fsm.Query queries[] = new edu.tufts.vue.fsm.Query[repositories.length];
			for (int i=0; i < repositories.length; i++) {
				org.osid.repository.Repository repository = repositories[i];

				// We need to figure out how this should work under Provider
				edu.tufts.vue.fsm.QueryAdjuster adjuster = getQueryAdjusterForRepository(repository.getId());		
				if (adjuster != null) {
					queries[i] = adjuster.adjustQuery(repository,
													  searchCriteria,
													  searchType,
													  searchProperties);
				} else {

//				System.out.println("Creating query for foreign id " + dataSources[i].getId().getIdString());
				queries[i] = new VueQuery(dataSources[i].getId().getIdString(),
										  repository,
										  searchCriteria,
										  searchType,
										  searchProperties);
				}
			}
			searchEngine.search(queries);
			return new VueResultSetManager(searchEngine);			
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"preparing ResultSetManager");
		}
		return null;
	}	
}
