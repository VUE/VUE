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
    com.hp.hpl.jena.rdf.model.Property idOf = createProperty("vue://","id");
    com.hp.hpl.jena.rdf.model.Property labelOf = createProperty("vue://","label");
    com.hp.hpl.jena.rdf.model.Property childOf = createProperty("vue://","child");
     com.hp.hpl.jena.rdf.model.Property authorOf = createProperty("vue://","author");
    
    
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
    
    public List search(String keyword) {
        return null;
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
    }
    public static String getUniqueId() {
        return edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex createDefaultIndex() {
        return new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
    }
    
}
