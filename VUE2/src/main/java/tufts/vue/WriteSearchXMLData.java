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

package tufts.vue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class WriteSearchXMLData {
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(WriteSearchXMLData.class);
	List data;

	Document dom;

	public WriteSearchXMLData(List dataList) {

		// create a list to hold the data

		data = new ArrayList();
		data = dataList;		
		// Get a DOM object

		createDocument();

	}

	public void runSearchWriteToFile() {

		Log.info("Started .. ");
		File file = new File("Search.xml");
		Log.info("file.exists()::"+file.exists());
		if(file.exists()){
			readExistingXMLFile(file);
		}else{
			createDOMTree();	
			printToFile();
		}

		Log.info("Generated file successfully in your workspace.");

	}	
    
	public List getData(){
		return data;
	}
	private void createDocument() {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			dom = db.newDocument();

		} catch (ParserConfigurationException pce) {

			Log.info("Error while trying to instantiate DocumentBuilder "
							+ pce);

			System.exit(1);

		}

	}

	private void createDOMTree() {

		Element rootElement = dom.createElement("SearchData");

		dom.appendChild(rootElement);

		Iterator it = data.iterator();

		while (it.hasNext()) {

			SearchData search = (SearchData) it.next();

			Element searchElement = createSearchElement(search);

			rootElement.appendChild(searchElement);

		}

	}

	private Element createSearchElement(SearchData search) {

		Element searchElement = dom.createElement("Search");		
		Element searchTypeElement = dom.createElement("SearchType");
		Text searchTypeText = dom.createTextNode(search.getSearchType());		
		searchTypeElement.appendChild(searchTypeText);
		searchElement.appendChild(searchTypeElement);

		Element mapElement = dom.createElement("MapType");
		Text mapText = dom.createTextNode(search.getMapType());
		mapElement.appendChild(mapText);
		searchElement.appendChild(mapElement);
		

		Element resultElement = dom.createElement("Result");

		Text resultText = dom.createTextNode(search.getResultType());

		resultElement.appendChild(resultText);

		searchElement.appendChild(resultElement);
		
		Element andOrElement = dom.createElement("AndOr");

		Text andOrText = dom.createTextNode(search.getAndOrType());

		andOrElement.appendChild(andOrText);

		//andOrElement.appendChild(andOrElement);
		
		searchElement.appendChild(andOrElement);

		return searchElement;

	}

	private void printToFile() {


		try {

			final Transformer transformer = TransformerFactory.newInstance().newTransformer();

			final FileOutputStream fileOutputStream = new FileOutputStream(
					new File("Search.xml"));
			final StreamResult streamResult = new StreamResult(fileOutputStream);
			transformer.transform(new DOMSource(dom), streamResult);

			fileOutputStream.close();

		} catch (IOException | TransformerException ie) {

			ie.printStackTrace();

		}

	}
	
	public void readExistingXMLFile(File file){
		try {			  
			  DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			  DocumentBuilder db = dbf.newDocumentBuilder();
			  Document doc = db.parse(file);
			  doc.getDocumentElement().normalize();
			  Log.info("Root element " + doc.getDocumentElement().getNodeName());
			  NodeList nodeLst = doc.getElementsByTagName("SearchData");
			  Log.info("Information of all Search Data"+nodeLst.getLength());			  
			  
			  for (int iCount = 0; iCount < nodeLst.getLength(); iCount++) {

			    Node fstNode = nodeLst.item(iCount);
			    Element childSearchElmnt = (Element) fstNode;
				NodeList searchChildLst = childSearchElmnt.getElementsByTagName("Search");
				Log.info("Information of all Search Data>>>>"+searchChildLst.getLength());
				
				for (int iChildCount = 0; iChildCount < searchChildLst.getLength(); iChildCount++) {
					
					Node childNode = searchChildLst.item(iChildCount);
				    if (childNode.getNodeType() == Node.ELEMENT_NODE) {				    	
				      Element searchTypeElmnt = (Element) childNode;
				      NodeList searchTypeNmElmntLst = searchTypeElmnt.getElementsByTagName("SearchType");
				      Element searchTypeNmElmnt = (Element) searchTypeNmElmntLst.item(0);
				      NodeList searchTypeNm = searchTypeNmElmnt.getChildNodes();
				      Log.info("Search : "  + ((Node) searchTypeNm.item(0)).getNodeValue());
				      
				      NodeList mapTypeNmElmntLst = searchTypeElmnt.getElementsByTagName("MapType");
				      Element mapTypNmElmnt = (Element)  mapTypeNmElmntLst.item(0);
				      NodeList mapTypNm = mapTypNmElmnt.getChildNodes();
				      Log.info("MapType : " + ((Node) mapTypNm.item(0)).getNodeValue());
				      
				      NodeList resultNmElmntLst = searchTypeElmnt.getElementsByTagName("Result");
				      Element resultNmElmnt = (Element) resultNmElmntLst.item(0);
				      NodeList resultNm = resultNmElmnt.getChildNodes();
				      Log.info("Result : " + ((Node) resultNm.item(0)).getNodeValue());
				      
				      NodeList andOrNmElmntLst = searchTypeElmnt.getElementsByTagName("AndOr");
				      Element andOrNmElmnt = (Element) andOrNmElmntLst.item(0);
				      NodeList andOrNm = andOrNmElmnt.getChildNodes();
				      //Log.info("AndOr : " + ((Node) andOrNm.item(0)).getNodeValue());
				    }
				}

			  }
			  } catch (Exception e) {
			    e.printStackTrace();
			  }
	}
}
