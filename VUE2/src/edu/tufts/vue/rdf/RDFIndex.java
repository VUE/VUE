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

import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.sparql.core.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.query.*;

public class RDFIndex extends ModelCom {
    
    public static final String INDEX_FILE = VueUtil.getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file");
    public static final String VUE_BASE = "vue-index://";
    com.hp.hpl.jena.rdf.model.Property idOf = createProperty("vue://vue#","id");
    com.hp.hpl.jena.rdf.model.Property labelOf = createProperty("vue://vue#","label");
    com.hp.hpl.jena.rdf.model.Property childOf = createProperty("vue://vue#","child");
    com.hp.hpl.jena.rdf.model.Property authorOf = createProperty("vue://vue#","author");
    com.hp.hpl.jena.rdf.model.Property hasTag = createProperty("user://user#","tag");
    
    
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
    }
    public void index(LWMap map) {
        com.hp.hpl.jena.rdf.model.Resource mapR = this.createResource("vue://"+map.getURI().toString());
        mapR.addProperty(idOf,map.getID());
        mapR.addProperty(authorOf,System.getProperty("user.name"));
        if(map.getLabel() != null){
            mapR.addProperty(labelOf,map.getLabel());
        }
        for(LWComponent comp: map.getAllDescendents()) {
            rdfize(comp,mapR);
            
            
        }
        System.out.println("Size of index:"+this.size());
    }
    
    public List<URI> search(String keyword) {
        List<URI> r = new ArrayList<URI>();
        System.out.println("Searching for: "+keyword+ " size of index:"+this.size());
        System.out.println("INDEX:"+this);
        String queryString =
                "PREFIX vue: <vue://vue#>"+
                "SELECT ?id ?label ?resource " +
                "WHERE {" +
                "      ?resource vue:label \""+keyword+ "\""+
                "      }";
        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.create(query, this);
        ResultSet results = qe.execSelect();
        while(results.hasNext())  {
            Object o = results.next();
            ResultBinding res = (ResultBinding)o ;
            try {
                r.add(new URI(res.get("resource").toString()));
            }catch(Throwable t) {
                t.printStackTrace();
            }
            System.out.println("Resource :"+res.get("resource"));
        }
        ResultSetFormatter.out(System.out, results, query);
        
        qe.close();
        return r;
    }
    
    public void save() {
        
    }
    
    public void read() {
        
        
    }
    
    public void rdfize(LWComponent component,com.hp.hpl.jena.rdf.model.Resource mapR) {
        com.hp.hpl.jena.rdf.model.Resource r = this.createResource("vue://"+component.getURI().toString());
        r.addProperty(idOf,component.getID());
        if(component.getLabel() != null){
            r.addProperty(labelOf,component.getLabel());
        }
        com.hp.hpl.jena.rdf.model.Statement statement = this.createStatement(r,childOf,mapR);
        this.add(statement);
        List<VueMetadataElement> metadata = component.getMetadataList().getMetadata();
        Iterator<VueMetadataElement> i = metadata.iterator();
        while(i.hasNext()) {
            VueMetadataElement element = i.next();
            statement = this.createStatement(r,hasTag,element.getObject().toString());
            this.add(statement);
        }
    }
    
    public static String getUniqueId() {
        
        return edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex getDefaultIndex() {
        RDFIndex index = new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
        try {
            File indexFile = new File(INDEX_FILE);
            if(indexFile.exists()) {
                index.read(new FileReader(indexFile),VUE_BASE);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        index.search("one");
        return index;
    }
    
    @Deprecated
    public static RDFIndex createDefaultIndex() {
        
        return new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
    }
    
}
