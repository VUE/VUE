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

public class RDFIndex extends ModelCom {
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
    }
    public void index(LWMap map) {
        
    }
    
    public List search(String keyword) {        
        return null;
    }
    
    public void save() {
        
    }
    
    public void read() {
        
    }
    
}
