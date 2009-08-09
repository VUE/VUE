package edu.tufts.vue.zotero;


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
import java.beans.Statement;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tufts.Util;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.LWNode;
import tufts.vue.MapViewer;
import tufts.vue.Resource;
import tufts.vue.VUE;

import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.util.FileManager;


public class ZoteroAction {

	private static String ONTOLOGY_SEPERATOR = "#";
	private static String ZOTERO_ONTOLOGY="http://www.zotero.org/namespaces/export";	
	private static String DC_TERMS ="http://purl.org/dc/terms/";
	private static String DC_ELEMENTS = "http://purl.org/dc/elements/1.1/";
	private static String RDF_NAMESPACE="http://www.w3.org/1999/02/22-rdf-syntax-ns";
	private static String BIBLIO = "http://purl.org/net/biblio";
	private final static String RANDOM = "random";
	private final static String SEARCH = "search";
	
	public static void importZotero(File file) {
		java.util.List<LWComponent> comps = new ArrayList<LWComponent>();		
		LWMap map = VUE.getActiveMap();
		MapViewer viewer = VUE.getActiveViewer();
		
		 // create an empty model
		 Model model = ModelFactory.createDefaultModel();

		 // use the FileManager to find the input file
		 InputStream in = FileManager.get().open(file.getAbsolutePath());
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + file.getAbsolutePath() + " not found");
		}

		// read the RDF/XML file
		model.read(in, "http://www.zotero.org/namespaces/export");
				
		// write it to standard out
		//model.write(System.out);
				
		List<ZoteroItem> zoteroItems = getZoteroItems(model);
		
		//scatter no subject items on map.
		scatterKeywordlessItems(comps,zoteroItems);
		
		addComponentsToMap(comps,RANDOM);
		
		ResultSet results = getKeywordNodes(model);
			
		
	/*	while(results.hasNext())  
		{
		       QuerySolution qs = results.nextSolution();		   
		       String subject = qs.get("subject").toString();
		       //System.out.println(qs.toString());
		      if (subject !=null && subject.length() > 0)
		      {  	//System.out.println(resourceList.get(p).toString());	
		    	  	comps.clear();
		        	tufts.vue.LWNode keywordNode = new tufts.vue.LWNode(subject);

		        	String label = keywordNode.getLabel();

		            label = Util.formatLines(label, Util.getMaxLabelLineLength());
		            keywordNode.setLabel(label);		            		        	
		        	keywordNode.layout();
		        	comps.add(keywordNode);
		        	
		        	addComponentsToMap(comps,RANDOM);
					
		        	//recreated for next layout
					comps.clear();
		        	for (int i = 0; i < zoteroItems.size(); i++)
		        	{
		        		//System.out.println("NEXT " + i + "," + zoteroItems.get(i).getSubject());
		        		if (zoteroItems.get(i).getSubject() !=null && zoteroItems.get(i).getSubject().equals(subject))
		        		{
		        			//System.out.println("ADD : " + subject);
		        			String itemLabel = Util.formatLines(zoteroItems.get(i).getTitle(), Util.getMaxLabelLineLength());
		        			LWNode itemNode = new LWNode(itemLabel);

		        			String ref = zoteroItems.get(i).getReference();
		        			String item = zoteroItems.get(i).getItem();
		        			String notes = null;
		        			if (ref !=null)
		        			  notes = getZoteroNotes(ref,item, model);
		        			
		        			Resource r = Resource.instance(item);
		        			
		        			itemNode.setResource(r);
		        			itemNode.setNotes(notes);
		        			itemNode.setLocation(keywordNode.getLocation());
		        			comps.add(itemNode);
		        			LWLink link = new LWLink(keywordNode,itemNode);
		        			comps.add(link);
		        		}
		        	}

		        	addComponentsToMap(comps,SEARCH);		        			        			        	
		       	}		      
		}//end while results
		*/
		//}
		/*
 *CODE TO DUMP TRIPLES TO A FILE.
 *
 */
		
 	StmtIterator stmt = model.listStatements();

	try {
    	BufferedWriter out = new BufferedWriter(new FileWriter("/Users/mkorcy01/Desktop/triples.txt"));
   

		while (stmt.hasNext())
		{
			com.hp.hpl.jena.rdf.model.Statement s = stmt.nextStatement();
			out.write(s.toString()+"\n");
		}

		out.close();
		} catch (IOException e) { e.printStackTrace();
		}
	
	}
	
	private static void addComponentsToMap(List<LWComponent> comps,
			String layout) {
		
		LWMap map = VUE.getActiveMap();
		
		if (map!=null)
		{
			map.addChildren(comps);
			if (layout.equals(RANDOM))
				tufts.vue.LayoutAction.random.act(comps);
			else if (layout.equals(SEARCH))
				tufts.vue.LayoutAction.search.act(comps);
		}
		
	}

	private static ResultSet getKeywordNodes(Model model) {
		String queryString = 	
			"PREFIX z: <"+ ZOTERO_ONTOLOGY + ONTOLOGY_SEPERATOR+">"+
			"PREFIX dcterms: <"+DC_TERMS+">"+
			"PREFIX dcelements: <"+DC_ELEMENTS+">"+
			"PREFIX rdf: <"+RDF_NAMESPACE + ONTOLOGY_SEPERATOR+">"+
			"PREFIX biblio: <"+BIBLIO + ONTOLOGY_SEPERATOR+">"+
			"SELECT DISTINCT ?subject " +
			"WHERE { ?item dcelements:subject ?subject}";
		
			ResultSet results = SparqlQuery.executeQuery(queryString, model);
			return results;
	}

	private static void scatterKeywordlessItems(List<LWComponent> comps,
			List<ZoteroItem> zoteroItems) {
		
		comps.clear();
		for (int i = 0; i < zoteroItems.size(); i++)
    	{
    		//System.out.println("NEXT " + i + "," + zoteroItems.get(i).getSubject());
    	/*	if (zoteroItems.get(i).getSubject() ==null)
    		{
    			//System.out.println("ADD : " + subject);
    			String itemLabel = Util.formatLines(zoteroItems.get(i).getTitle(), Util.getMaxLabelLineLength());
    			LWNode itemNode = new LWNode(itemLabel);
    			comps.add(itemNode);
    		}*/
    	}			
		
	}

	private static String getZoteroNotes(String item, String itemId, Model model) {
		//[http://www.zotero.org/namespaces/export#item_46, http://www.w3.org/1999/02/22-rdf-syntax-ns#value, "<p>Some NOtes</p>"]
		//	[http://www.zotero.org/namespaces/export#item_46, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://purl.org/net/biblio#Memo]
//[http://vue.tufts.edu/, http://purl.org/dc/terms/isReferencedBy, http://www.zotero.org/namespaces/export#item_46]
//		[http://www.zotero.org/namespaces/export#item_46, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://purl.org/net/biblio#Memo]

		String queryString = 	
			"PREFIX z: <"+ ZOTERO_ONTOLOGY + ONTOLOGY_SEPERATOR+">"+
			"PREFIX dcterms: <"+DC_TERMS+">"+
			"PREFIX dcelements: <"+DC_ELEMENTS+">"+
			"PREFIX rdf: <"+RDF_NAMESPACE + ONTOLOGY_SEPERATOR+">"+
			"PREFIX biblio: <"+BIBLIO + ONTOLOGY_SEPERATOR+">"+
			"SELECT  ?notes " +
			"WHERE {<"+item+"> rdf:value ?notes ." +
			" 		<"+item+"> rdf:type biblio:Memo . "+
			" 		<"+itemId+"> dcterms:isReferencedBy <"+item+">}"; 
		//System.out.println("QS : " + queryString);
		//System.out.println("ITEM ID : " + itemId);	
    	ResultSet results2 = SparqlQuery.executeQuery(queryString, model);
	
//    	List<ZoteroItem> l = new ArrayList();

    	String memo = null;
    	
    	if (results2.hasNext())
    	{
    		QuerySolution qs2 = results2.nextSolution();
    		//System.out.println("QS2 : " + qs2.toString());
    		memo = qs2.getLiteral("notes").toString();
    	}
    		
    	//System.out.println("memo : " + memo);
		return memo;
	}
	private static List getZoteroItems(Model model) {
		String queryString = 	
			"PREFIX z: <"+ ZOTERO_ONTOLOGY + ONTOLOGY_SEPERATOR+">"+
			"PREFIX dcterms: <"+DC_TERMS+">"+
			"PREFIX dcelements: <"+DC_ELEMENTS+">"+
			"PREFIX rdf: <"+RDF_NAMESPACE + ONTOLOGY_SEPERATOR+">"+
			"PREFIX biblio: <"+BIBLIO + ONTOLOGY_SEPERATOR+">"+
			"SELECT  ?item ?title ?subject ?ref " +
			"WHERE {?item rdf:type ?docType ." +
			" 		?item dcelements:title ?title ." +
			"OPTIONAL {?item dcelements:subject ?subject} " +
			"OPTIONAL {?item dcterms:isReferencedBy ?ref}}";
//			"	   ?item dcelements:subject ?"+subject+"}";

    	ResultSet results2 = SparqlQuery.executeQuery(queryString, model);
	
    	List<ZoteroItem> l = new ArrayList();
    	while(results2.hasNext())  
    	{
    					
	       QuerySolution qs2 = results2.nextSolution();
	       String title = qs2.get("title").toString();  
	       System.out.println(qs2.toString());    	
           String subject = null;
           if (qs2.getLiteral("subject") !=null)
        	  subject= qs2.getLiteral("subject").toString();
	       String item = qs2.get("item").toString();

	       String reference = null;
	       
	       if (qs2.get("ref") != null) 
	       		reference = qs2.get("ref").toString();
	       
	       //System.out.println("ref : " + reference);
	       l.add(new ZoteroItem());
           //System.out.println(qs2.toString());
	    }
		return l;
	}

	public static void main(String[] args)
	{
		importZotero(new File("/Users/mkorcy01/Desktop/Simple.rdf"));
//		importZotero(new File("/Users/mkorcy01/Desktop/My Library.rdf"));
	}

}
