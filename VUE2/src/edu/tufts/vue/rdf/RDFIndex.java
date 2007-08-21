/**
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 */

/**
 *
 * RDFIndex.java
 *
 * @author akumar03
 * @author Daniel J. Heller
 *
 */

package edu.tufts.vue.rdf;

import java.util.*;
import java.io.*;
import java.net.*;
import edu.tufts.vue.metadata.*;
import tufts.vue.*;

import edu.tufts.vue.ontology.*;
import edu.tufts.vue.metadata.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.sparql.core.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.query.*;

public class RDFIndex extends ModelCom {
    public static final int MAX_SIZE = VueResources.getInt("rdf.index.size");
    public static final boolean AUTO_INDEX= VueResources.getBool("rdf.index.auto");
    public static final String INDEX_FILE = VueUtil.getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file");
    public static final String ONT_SEPARATOR = "#";
    public static final String VUE_ONTOLOGY = Constants.ONTOLOGY_URL+ONT_SEPARATOR;
    com.hp.hpl.jena.rdf.model.Property idOf = createProperty(VUE_ONTOLOGY,Constants.ID);
    com.hp.hpl.jena.rdf.model.Property labelOf = createProperty(VUE_ONTOLOGY,Constants.LABEL);
    com.hp.hpl.jena.rdf.model.Property childOf = createProperty(VUE_ONTOLOGY,Constants.CHILD);
    com.hp.hpl.jena.rdf.model.Property authorOf = createProperty(VUE_ONTOLOGY,Constants.AUTHOR);
    com.hp.hpl.jena.rdf.model.Property hasTag = createProperty(VUE_ONTOLOGY,Constants.TAG);
    private static RDFIndex defaultIndex;
    private boolean isAutoIndexing = AUTO_INDEX;
    private  com.hp.hpl.jena.query.Query query;
    private QueryExecution qe;
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
    }
    
    public RDFIndex() {
        super(com.hp.hpl.jena.graph.Factory.createDefaultGraph());
        // creating dummy query object. to make the search faster;
         String queryString =
                "PREFIX vue: <"+VUE_ONTOLOGY+">"+
                "SELECT ?resource ?keyword " +
                "WHERE{" +
                "      ?resource ?x ?keyword  } ";
        query = QueryFactory.create(queryString);
        qe = QueryExecutionFactory.create(query, this);
    }
    public void index(LWMap map) {
        long t0 = System.currentTimeMillis();
        if(DEBUG.RDF)System.out.println("INDEX - begin index: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        
        com.hp.hpl.jena.rdf.model.Resource mapR = this.createResource(map.getURI().toString());
        if(DEBUG.RDF)System.out.println("INDEX - create resource for map: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        try {
            addProperty(mapR,idOf,map.getID());
            addProperty(mapR,authorOf,System.getProperty("user.name"));
            if(map.getLabel() != null){
                addProperty(mapR,labelOf,map.getLabel());
            }
            if(DEBUG.RDF) System.out.println("INDEX - added properties for map: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
            
            for(LWComponent comp: map.getAllDescendents())
                rdfize(comp,mapR);
            
            if(DEBUG.RDF)System.out.println("INDEX - after indexing all components: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
            
        } catch(Exception ex) {
            System.out.println("RDFIndex.index: "+ex);
        }
        if(DEBUG.RDF) System.out.println("Size of index:"+this.size());
    }
    
    public List<URI> search(String keyword) {
        long t0 = System.currentTimeMillis();
        if(DEBUG.RDF) System.out.println("SEARCH- beginning of search: "+(System.currentTimeMillis()-t0)+" Memory: "+Runtime.getRuntime().freeMemory());
        
        List<URI> r = new ArrayList<URI>();
        //System.out.println("Searching for: "+keyword+ " size of index:"+this.size());
        String queryString =
                "PREFIX vue: <"+VUE_ONTOLOGY+">"+
                "SELECT ?resource ?keyword " +
                "WHERE{" +
                "      ?resource ?x ?keyword FILTER regex(?keyword,\""+keyword+ "\",\"i\") } ";
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
    
    public void save() {
        
    }
    
    public void read() {
        
        
    }
    
    public void rdfize(LWComponent component,com.hp.hpl.jena.rdf.model.Resource mapR) {
        com.hp.hpl.jena.rdf.model.Resource r = this.createResource(component.getURI().toString());
        try {
            addProperty(r,idOf,component.getID());
            if(component.getLabel() != null){
                addProperty(r,labelOf,component.getLabel());
            }
            com.hp.hpl.jena.rdf.model.Statement statement = this.createStatement(r,childOf,mapR);
            
            addStatement(statement);
            List<VueMetadataElement> metadata = component.getMetadataList().getMetadata();
            Iterator<VueMetadataElement> i = metadata.iterator();
            while(i.hasNext()) {
                VueMetadataElement element = i.next();
                if(DEBUG.RDF) System.out.println("Resouece: "+r+" Element:"+element+" class of element:"+element.getClass());
                if(element.getObject() != null)
                    statement = this.createStatement(r,createPropertyFromKey(element.getKey()),element.getObject().toString());
                addStatement(statement);
            }
        } catch(Exception ex) {
            System.out.println("RDFIndex.rdfize: "+ex);
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
    
    public void startAutoIndexing() {
        isAutoIndexing = true;
    }
    
    public void stopAutoIndexing() {
        isAutoIndexing = false;
    }
    
    public void regenerate() {
        System.out.println("Before Indexing size: "+size());
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
        System.out.println("After Indexing size: "+size());
        
    }
    
    public  com.hp.hpl.jena.rdf.model.Property createPropertyFromKey(String key) throws Exception {
        String words[] = key.split(ONT_SEPARATOR);
        if(words.length < 2){
            throw new Exception("createPropertyFromKey: The key format is wrong. key - "+key);
        }
        return createProperty(words[0],words[1]);
    }
    
    private boolean compareStatements(com.hp.hpl.jena.rdf.model.Statement stmt1,com.hp.hpl.jena.rdf.model.Statement stmt2) {
        if(!stmt1.getSubject().toString().equals(stmt2.getSubject().toString())) {
            
            return false;
        }
        if(!stmt1.getObject().toString().equals(stmt2.getObject().toString())) {
            return false;
        }if(!stmt1.getPredicate().toString().equals(stmt2.getPredicate().toString())) {
            return false;
        }
        return true;
    }
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
    
}
