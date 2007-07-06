/*
 * RDFIndex.java
 *
 * Created on June 25, 2007, 12:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */

package edu.tufts.vue.rdf;

import java.util.*;
import java.io.*;
import tufts.vue.*;

import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.graph.*;

public class RDFIndex extends ModelCom {
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
    }
    public void index(LWMap map) {
        for(LWComponent comp: map.getAllDescendents()) {
            rdfize(comp);
        }
         System.out.println("Size of index:"+this.size());
    }
    
    public List search(String keyword) {        
        return null;
    }
    
    public void save() {
        
    }
    
    public void read() {
        
    }
    
    public void rdfize(LWComponent component) {
        com.hp.hpl.jena.rdf.model.Resource r = this.createResource("vue://"+component.getURI().toString());
        com.hp.hpl.jena.rdf.model.Property idOf = this.createProperty("vue://","id");
        com.hp.hpl.jena.rdf.model.Property labelOf = this.createProperty("vue://","label");
        r.addProperty(idOf,component.getID());
        if(component.getLabel() != null){
            r.addProperty(labelOf,component.getLabel());
        }
    }
    public static String getUniqueId() {
        return edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex createDefaultIndex() {
        return new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
    }
    
}
