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
    
    public OWLLOntology(String ontUrl)
    {
       setBase(ontUrl); 
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
    
    public static void populateExistingOWLOntology(URL ontUrl,
                                                   OWLLOntology owlLOntology)
    {
        owlLOntology.m.read(ontUrl.toString());
        //owlLOntology.setBase(ontUrl.toString());
        owlLOntology.readAllSupportedOntTypes();
    }
    
    public static void populateExistingOWLOntology(URL ontUrl,URL cssUrl,
                                                   OWLLOntology owlLOntology)
    {
        owlLOntology.cssUrl = cssUrl;
        owlLOntology.m.read(ontUrl.toString());
        //owlLOntology.setBase(ontUrl.toString());
        owlLOntology.readAllSupportedOntTypesWithCss();
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
