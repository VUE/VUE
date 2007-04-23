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
        List<OntType> types = new ArrayList<OntType>();
        m.read(ontUrl.toString());
        setBase(ontUrl.toString());
        ExtendedIterator iter;
        readOntTypes(m.listNamedClasses(),types,ontUrl.toString());
        readOntTypes( m.listOntProperties(),types,ontUrl.toString());
        setOntTypes(types);
    }
    
    public OWLLOntology(URL ontUrl,URL cssUrl) {
        List<OntType> types = new ArrayList<OntType>();
        m.read(ontUrl.toString());
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        ExtendedIterator iter = m.listOntProperties();
        setBase(ontUrl.toString());
        readOntTypes(iter,types,styleMap,ontUrl.toString());
         readOntTypes(m.listObjectProperties(),types,styleMap,ontUrl.toString());
        readOntTypes(m.listNamedClasses(),types,styleMap,ontUrl.toString());
        setOntTypes(types);
    }
    
    /**
     * @param args the command line arguments
     */
   public org.osid.shared.Type getType() {
       return OntologyType.OWL_TYPE;
   }
    
}
