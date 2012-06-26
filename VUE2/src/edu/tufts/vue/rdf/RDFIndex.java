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

package edu.tufts.vue.rdf;

import java.util.*;
import java.io.*;
import java.net.*;

import tufts.vue.DEBUG;
import tufts.vue.LWComponent;
import tufts.vue.LWPathway;
import tufts.vue.LWMap;
import tufts.vue.VueResources;
import tufts.vue.VueUtil;
import edu.tufts.vue.metadata.*;

import edu.tufts.vue.ontology.*;
import edu.tufts.vue.metadata.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.sparql.core.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.query.*;

/**
 *
 * RDFIndex.java
 *
 * RDFIndex mainly makes use of Apache Jena for VUE map searching.  This RDF approach is overkill for
 * VUE's current search needs, but originally began as an effort to create larger, peristent multi-map
 * indicies.  That effort was not completed.  The resulting implementation that uses this
 * class creates a new RDFIndex for every new search (over one map or multiple maps), and then runs
 * the search on that fresh index.
 *
 * We do get one very nice feature for free -- the ability to write out a full .rdf file containing
 * a set map data, although the current impl below tosses out the the key names from OSID meta-data
 * -- just the values are written out, all called "property".
 *
 * -- Scott Fraize 2012-06-25
 *
 * @author akumar03
 * @author Daniel J. Heller
 *
 */

public class RDFIndex extends com.hp.hpl.jena.rdf.model.impl.ModelCom
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(RDFIndex.class);
    
    public static final int MAX_SIZE = VueResources.getInt("rdf.index.size");
    //public static final boolean AUTO_INDEX= VueResources.getBool("rdf.index.auto"); // never implememnted
    public static final String INDEX_FILE = VueUtil.getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file");
    public static final String ONT_SEPARATOR = "#";
    public static final String VUE_ONTOLOGY = Constants.ONTOLOGY_URL+ONT_SEPARATOR;
    
    final com.hp.hpl.jena.rdf.model.Property idOf = createProperty(VUE_ONTOLOGY,Constants.ID);
    final com.hp.hpl.jena.rdf.model.Property labelOf = createProperty(VUE_ONTOLOGY,Constants.LABEL);
    final com.hp.hpl.jena.rdf.model.Property childOf = createProperty(VUE_ONTOLOGY,Constants.CHILD);
    final com.hp.hpl.jena.rdf.model.Property authorOf = createProperty(VUE_ONTOLOGY,Constants.AUTHOR);
    final com.hp.hpl.jena.rdf.model.Property colorOf = createProperty(VUE_ONTOLOGY,Constants.COLOR);
    final com.hp.hpl.jena.rdf.model.Property notesOf = createProperty(VUE_ONTOLOGY,Constants.NOTES);
    final com.hp.hpl.jena.rdf.model.Property contentPropertyOf = createProperty(VUE_ONTOLOGY,Constants.CONTENT_INFO_PROPERTY);
    final com.hp.hpl.jena.rdf.model.Property hasTag = createProperty(VUE_ONTOLOGY,Constants.TAG);
    
    private static RDFIndex defaultIndex;
    private  com.hp.hpl.jena.query.Query query;
    private QueryExecution qe;
    // private boolean isAutoIndexing = AUTO_INDEX; // never implemented
    
    // could also index objects in LWComponent when get ID
    private static boolean INDEX_OBJECTS_HERE = true;
    //private boolean shouldClearMap = true;
    
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
        if (DEBUG.RDF || DEBUG.SEARCH) Log.debug("instanced from " + tufts.Util.tags(base));
    }

    private static final String DefaultVueQuery =
          "PREFIX vue: <" + VUE_ONTOLOGY + ">"
        + "SELECT ?resource ?keyword "
        + "WHERE{"
        + " ?resource ?x ?keyword } ";
    
    public RDFIndex() {
        super(com.hp.hpl.jena.graph.Factory.createDefaultGraph());
        // creating dummy query object. to make the search faster;
        query = QueryFactory.create(DefaultVueQuery);
        qe = QueryExecutionFactory.create(query, this);
        if (DEBUG.RDF || DEBUG.SEARCH) Log.debug("instanced w/default VUE query[" + DefaultVueQuery + "]");
    }
    
    public void index(LWMap map) {
        index(map,false,false,true);
    }
    public void index(LWMap map,boolean metadataOnly) {
        index(map,metadataOnly,false,true);
    }
    
    public void index(LWMap map,boolean metadataOnly,boolean searchEverything,boolean shouldClearMap)
    {
        if (INDEX_OBJECTS_HERE && shouldClearMap)
            VueIndexedObjectsMap.clear();
        
        if (DEBUG.RDF) Log.debug("index: begin; freeMem=: "+Runtime.getRuntime().freeMemory());
        
        final com.hp.hpl.jena.rdf.model.Resource mapR = this.createResource(map.getURI().toString());
        
        if (DEBUG.RDF) Log.debug("index: create resource for map; freeMem=: "+Runtime.getRuntime().freeMemory());
        
        try {
            addProperty(mapR, idOf, map.getID());
            addProperty(mapR, authorOf, System.getProperty("user.name"));
            if (map.hasLabel())
                addProperty(mapR,labelOf,map.getLabel());

            if (DEBUG.RDF) Log.debug("index: added properties for map; freeMem="+Runtime.getRuntime().freeMemory());
            
            final Collection<LWComponent> searchSet;
            
            if (searchEverything) {
                // E.g., this will search everything incuding Slides
                searchSet = map.getAllDescendents(LWComponent.ChildKind.ANY);
            } else {
                searchSet = map.getAllDescendents();
            }
            
            for (LWComponent c : searchSet) {

                if (c instanceof LWPathway || c instanceof LWMap.Layer)
                    continue;
                
                if (INDEX_OBJECTS_HERE)
                    VueIndexedObjectsMap.setID(c.getURI(), c);
                
                load_VUE_component_to_RDF_index(c, mapR, metadataOnly);

                if (size() > MAX_SIZE) {
                    Log.warn("Maximum fail-safe search capacity reached: not all nodes will be searchable. (See property rdf.index.size)");
                    break;
                }
            }    
            
            if (DEBUG.RDF) Log.debug("index: after indexing all components; freeMem="+Runtime.getRuntime().freeMemory());
            
        } catch(Exception ex) {
            Log.error("index", ex);
        }
        if(DEBUG.RDF) Log.debug("index: done -- size="+this.size());
    }
    
    /***
     * 
     *  todo: factor this code correctly in with similar code
     *  in single argument search(String)
     * 
     * */
    public List<URI> search(String keyword,String queryString)
    {
        long t0 = System.currentTimeMillis();
        if(DEBUG.RDF) System.out.println("SEARCH- beginning of search: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        List<URI> r = new ArrayList<URI>();
        if(DEBUG.RDF) System.out.println("SEARCH- created arraylist and query string: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        query = QueryFactory.create(queryString);
        qe = QueryExecutionFactory.create(query, this);
        if(DEBUG.RDF) System.out.println("SEARCH- created query "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        ResultSet results = qe.execSelect();
        if(DEBUG.RDF) System.out.println("SEARCH- executed query: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        while(results.hasNext())  {
            QuerySolution qs = results.nextSolution();
            try {
                
                String fullKeyword = qs.getLiteral("keyword").toString();
                
                int slashLocation = fullKeyword.indexOf("#");
                int keywordLocation = fullKeyword.toString().toLowerCase().
                                          lastIndexOf(keyword.toString().toLowerCase());
                
                if(keywordLocation > slashLocation)
                {
                  r.add(new URI(qs.getResource("resource").toString()));
                }
            }catch(Throwable t) {
                t.printStackTrace();
            }
        }
        qe.close();
        return r;
   
    }
    
    public List<URI> search(String queryString)
    {
        
        long t0 = System.currentTimeMillis();
        if(DEBUG.RDF) System.out.println("SEARCH- beginning of search: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        List<URI> r = new ArrayList<URI>();
        if(DEBUG.RDF) System.out.println("SEARCH- created arraylist and query string: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        
        query = QueryFactory.create(queryString);
        qe = QueryExecutionFactory.create(query, this);
        if(DEBUG.RDF) System.out.println("SEARCH- created query "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        ResultSet results = qe.execSelect();
        if(DEBUG.RDF) System.out.println("SEARCH- executed query: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        while(results.hasNext())  {
            QuerySolution qs = results.nextSolution();
            try {
                r.add(new URI(qs.getResource("resource").toString()));
            }catch(Throwable t) {
                t.printStackTrace();
            }
        }
        qe.close();
        return r;
    }
    
    
    
    public List<URI> searchAllResources(String keyword) {
        
        String queryString =
                "PREFIX vue: <"+VUE_ONTOLOGY+">"+
                "SELECT ?resource ?keyword " +
                "WHERE{" +
                "      ?resource ?x ?keyword FILTER regex(?keyword,\""+keyword+ "\",\"i\") } ";
        
        return search(keyword,queryString);
    }
    
    public List<URI> search(Query query) 
    {
        String queryString = query.createSPARQLQuery();
        return search(queryString);
    }
    
    public void save() { }
    public void read() { }
    
    /* was public "rdfize" */
    private void load_VUE_component_to_RDF_index(LWComponent component,com.hp.hpl.jena.rdf.model.Resource mapR) {
        //rdfize(component,mapR,false);
        load_VUE_component_to_RDF_index(component, mapR, false);
    }

    private static final boolean RDFIZE_COLOR = VueResources.getString("rdf.rdfize.color").equals("TRUE");
    
    /* was public "rdfize" */
    
    /** Extract relevant data of interest from the given VUE component,
     // loading it into a new jena.rdf.model.Resource, and add that to
     // the RDF property tree as a child of the given mapRootResource */
    
    private void load_VUE_component_to_RDF_index
        (tufts.vue.LWComponent component,
         com.hp.hpl.jena.rdf.model.Resource mapRootResource,
         boolean metadataOnly)
    {
        final com.hp.hpl.jena.rdf.model.Resource r = this.createResource(component.getURI().toString());
        try {
            if (!metadataOnly) {    
                // addProperty(r,idOf,component.getID());

                if (component.hasLabel())
                    addProperty(r, labelOf, component.getLabel());

                if (component.hasNotes())
                    addProperty(r, notesOf, component.getNotes());

                if (RDFIZE_COLOR && component.getXMLfillColor() != null) 
                    addProperty(r, colorOf, component.getXMLfillColor());

                final tufts.vue.Resource res = component.getResource();
                if (res != null) {
                    // for (Map.Entry e : res.getProperties().entries())
                    // Note that we do not currently have a way to search on Resource keys
                    // -- these are never accessed -- just values.
                    
                    for (Object value : res.getProperties().values()) {
                        if (value != null)
                            addProperty(r, contentPropertyOf, value.toString());
                    }
                    // final javax.swing.table.TableModel model = res.getProperties().getTableModel();
                    // for(int i=0;i<model.getRowCount();i++) // note: column 0 is key, column 1 is value
                    //     addProperty(r,contentPropertyOf,"" + model.getValueAt(i, 1));
                }
            }
            
            com.hp.hpl.jena.rdf.model.Statement statement = this.createStatement(r, childOf, mapRootResource);
            
            addStatement(statement);
            
            final List<VueMetadataElement> metadata = component.getMetadataList().getMetadata();
            for (VueMetadataElement element : metadata) {
                if (DEBUG.RDF && DEBUG.META) Log.debug(r + " " + tufts.Util.tags(element));
                if (element.getObject() != null) {
                    final String encodedKey = getEncodedKey(element.getKey());
                    statement = this.createStatement(r, createPropertyFromKey(encodedKey), element.getValue());
                    addStatement(statement);
                } else {
                    Log.warn(r + ": null element object: no statement");
                    // the old code would just re-add whatever "statement" was last set to!
                }
                //statement = this.createStatement(r,createPropertyFromKey(element.getKey()),element.getObject().toString());
                //addStatement(statement);
            }
            // Iterator<VueMetadataElement> i = metadata.iterator();
            // while(i.hasNext()) {
            //     VueMetadataElement element = i.next();
            //     if(DEBUG.RDF) Log.debug(r + " " + tufts.Util.tags(element));
            //     if(element.getObject() != null) {
            //         String encodedKey = getEncodedKey(element.getKey());
            //         statement = this.createStatement(r,createPropertyFromKey(encodedKey),element.getValue());
            //     }
            //     //statement = this.createStatement(r,createPropertyFromKey(element.getKey()),element.getObject().toString());
            //     addStatement(statement);
            // }
        } catch(Exception ex) {
            Log.warn("rdfize " + ex);
            ex.printStackTrace();
        }
    }
    
    public void addStatement(com.hp.hpl.jena.rdf.model.Statement statement)  throws Exception {
        if(size() < MAX_SIZE) {
            super.add(statement);
        } else {
            throw new Exception("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    
    public void addProperty(com.hp.hpl.jena.rdf.model.Resource r, com.hp.hpl.jena.rdf.model.Property p,String value) throws Exception {
        if(size() <MAX_SIZE) {
            r.addProperty(p,value);
        } else {
            throw new Exception("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    public static String getUniqueId() {
        return Constants.RESOURCE_URL+edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex getDefaultIndex() {
        if(defaultIndex == null) {
            return createDefaultIndex();
        } else {
            return defaultIndex;
        }
    }
    
    // A concept of auto-indexing was never implemented -- SMF 2012-06-25
    // public void startAutoIndexing() {
    //     isAutoIndexing = true;
    // }
    // public void stopAutoIndexing() {
    //     isAutoIndexing = false;
    // }
    
    // don't see this called anywhere...
    public void regenerate() {
        Log.info("regenerate: before Indexing size: "+size());
        List stmtList  = listStatements().toList();
        for(int i = 0;i<stmtList.size();i++) {
            com.hp.hpl.jena.rdf.model.Statement statementI = (com.hp.hpl.jena.rdf.model.Statement)stmtList.get(i);
            for(int j = i+1;j<stmtList.size();j++) {
                com.hp.hpl.jena.rdf.model.Statement statementJ = (com.hp.hpl.jena.rdf.model.Statement) stmtList.get(j);
                if(compareStatements(statementI,statementJ)) {
                    remove(statementJ);
                }
            }
        }
        Log.info("regenerate: after Indexing size: "+size());
    }
    // don't see this called anywhere except above...
    private boolean compareStatements(com.hp.hpl.jena.rdf.model.Statement stmt1,com.hp.hpl.jena.rdf.model.Statement stmt2) {
             if (!stmt1.getSubject().toString().equals(stmt2.getSubject().toString()))
            return false;
        else if (!stmt1.getObject().toString().equals(stmt2.getObject().toString()))
            return false;
        else if (!stmt1.getPredicate().toString().equals(stmt2.getPredicate().toString()))
            return false;
        else
            return true;
    }
    
    public  com.hp.hpl.jena.rdf.model.Property createPropertyFromKey(String key) throws Exception {
        String words[] = key.split(ONT_SEPARATOR);
        if (words.length == 1) {
            return createProperty(VUE_ONTOLOGY+"#",key);
        } else if(words.length < 1){
            
            throw new Exception("createPropertyFromKey: The key format is wrong. key - "+key);
        }
        return createProperty(words[0]+"#",words[1]);
    }
    
    // Note that while VUE.java currently has code to call this, it's never used.  SMF 2012-06-25
    private static RDFIndex createDefaultIndex() {
        defaultIndex = new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
        try {
            File indexFile = new File(INDEX_FILE);
            if(indexFile.exists()) {
                defaultIndex.read(new FileReader(indexFile),Constants.RESOURCE_URL);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return defaultIndex;
    }
    
    public static String getEncodedKey(String key)
    {
       String encodedKey = key;
       
       String s = ONT_SEPARATOR;
       
       try
       {
         if(key.indexOf(s) != -1)
         {    
           String prefix = key.substring(0,key.indexOf(s));
           String end = key.substring(key.indexOf(s)+1,key.length());
           encodedKey = prefix + s + java.net.URLEncoder.encode(end,"UTF-8");
         }
         else
         {
           encodedKey = java.net.URLEncoder.encode(key,"UTF-8");  
         }
       }
       catch(java.io.UnsupportedEncodingException uee)
       {
         System.out.println("Query: attempting to encode criteria.key -- " + uee);  
       }
       return encodedKey;
    }
    
}
