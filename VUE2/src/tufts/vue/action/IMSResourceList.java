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

package tufts.vue.action;

import tufts.vue.*;

/*
 The goal of this code is to save a map as an XML file conforming to the standard IMS RLI.
 
 This is really a degenerate case.  We will use:
 
 <xml version "1.0" encoding="iso-8859-1">
 <resourceList>	
 <resourceListMetadata>
 <title>map name</title>
 </resourceListMetadata>
 <resource>
 <resourceMetadata>
 <title>resoure title></title>
 <location>
 <locator>resource spec</locator>
 </location>
 </resourceMetadata>
 </resource>
 </resourceList>
 */

public class IMSResourceList
{
	public IMSResourceList() 
	{		
	}
	
	public void convert(LWMap map,
						java.io.File file)
	{
		try {
			System.out.println("Saving IMS RLI...");
			javax.xml.parsers.DocumentBuilderFactory dbf = null;
			javax.xml.parsers.DocumentBuilder db = null;
			org.w3c.dom.Document document = null;
			org.w3c.dom.Element element = null;
			
			dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db.newDocument();
			org.w3c.dom.Element resourceListElement = document.createElement("resourceList");
			org.w3c.dom.Element resourceListMetadataElement = document.createElement("resourceListMetadata");
			org.w3c.dom.Element titleElement = document.createElement("title");
			
			titleElement.setNodeValue(VUE.getName());
			resourceListMetadataElement.appendChild(titleElement);
			resourceListElement.appendChild(resourceListMetadataElement);
			
			//java.util.List descendents = map.getAllDescendents();
			//System.out.println("size: " + descendents.size());
			//for (int i=0; i < descendents.size(); i++) {
			//	LWComponent lwc = (LWComponent)descendents.get(i);
			for (LWComponent lwc : map.getAllDescendents(tufts.vue.LWComponent.ChildKind.ANY)) {
				if (!(lwc instanceof LWImage)) {
					Resource r = lwc.getResource();
					if (r != null) {
						org.w3c.dom.Element resourceElement = document.createElement("resource");
						org.w3c.dom.Element resourceMetadataElement = document.createElement("resourceMetadata");
						org.w3c.dom.Element resourceTitleElement = document.createElement("title");
						org.w3c.dom.Element locationElement = document.createElement("location");
						
						resourceTitleElement.appendChild(document.createTextNode(r.getTitle()));
						locationElement.appendChild(document.createTextNode(r.getSpec()));
						//System.out.println("saving resource " + r.getTitle());
						
						resourceMetadataElement.appendChild(resourceTitleElement);
						resourceMetadataElement.appendChild(locationElement);
						resourceElement.appendChild(resourceMetadataElement);
						resourceListElement.appendChild(resourceElement);
					}
				}
			}				
			document.appendChild(resourceListElement);
			
			javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
			javax.xml.transform.Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty("indent","yes");
			javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
			String filename = file.getAbsolutePath();
			javax.xml.transform.stream.StreamResult result = 
				new javax.xml.transform.stream.StreamResult (filename);
			transformer.transform(domSource,result);
			System.out.println("IMS RLI Save Complete");
		} catch (Exception ex) {
			System.out.println("IMS RLI writing failed: " + ex.getMessage());
		}
	}
}
