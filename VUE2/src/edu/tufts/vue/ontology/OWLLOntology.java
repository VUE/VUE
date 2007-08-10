/*
 * OWLLOntology.java
 *
 * Created on April 20, 2007, 12:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.ontology;

import edu.tufts.vue.style.*;

import java.util.*;
import java.net.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;

public class OWLLOntology extends Ontology {
    OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM, null);
    /** Creates a new instance of OWLLOntology */
    public OWLLOntology() {
        
    }
    public OWLLOntology(URL ontUrl) {
        m.read(ontUrl.toString());
        setBase(ontUrl.toString());
        readAllSupportedOntTypes();
    }
    
    public OWLLOntology(URL ontUrl,URL cssUrl) {
        this.cssUrl = cssUrl;
        m.read(ontUrl.toString());
        setBase(ontUrl.toString());
        readAllSupportedOntTypesWithCss();
    }
    public void readAllSupportedOntTypes() {
        readOntTypes(m.listNamedClasses());
        readOntTypes(m.listObjectProperties());
        readOntTypes( m.listOntProperties());
    }
    
    public void readAllSupportedOntTypesWithCss(){
        readOntTypes(m.listOntProperties(),cssUrl);
        readOntTypes(m.listObjectProperties(),cssUrl);
        readOntTypes(m.listNamedClasses(),cssUrl);
    }
    /**
     * @param args the command line arguments
     */
    public org.osid.shared.Type getType() {
        return OntologyType.OWL_TYPE;
    }
    
}
