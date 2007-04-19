/*
 * OntManager.java
 *
 * Created on March 12, 2007, 10:59 AM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
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
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.ontology;

import java.util.*;
import edu.tufts.vue.style.*;
import java.net.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;

public class OntManager {
    public static final int RDFS = 0;
    public static final int OWL = 1;
    /** Creates a new instance of OntManager */
    List<Ontology> ontList = Collections.synchronizedList(new ArrayList<Ontology>());
    static OntManager ontManager;
    public OntManager() {
    }
    
    
    public  Ontology readOntologyWithStyle(URL ontUrl,URL cssUrl,int ontType) {
        
        switch (ontType) {
            case RDFS:
                Ontology ontR = readRDFSOntologyWithStyle(ontUrl,cssUrl);
                ontList.add(ontR);
                return ontR;
            case OWL:
                Ontology ontO = readOWLOntolgyWithStyle(ontUrl,cssUrl);
                ontList.add(ontO);
                return  ontO;
        }
        return null;
        
    }
    
    private Ontology readRDFSOntologyWithStyle(URL ontUrl, URL cssUrl) {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM,null);
        List<OntType> types = new ArrayList<OntType>();
        m.read(ontUrl.toString());
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        ExtendedIterator iter = m.listOntProperties();
        StyleReader.readCSS(cssUrl);
        Ontology ont = new Ontology();
        ont.setBase(ontUrl.toString());
        readOntTypes(iter,types,styleMap,ontUrl.toString());
        readOntTypes(m.listNamedClasses(),types,styleMap,ontUrl.toString());
        ont.setOntTypes(types);
        return ont;
    }
    private Ontology readOWLOntolgyWithStyle(URL ontUrl,URL cssUrl) {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM, null);
        List<OntType> types = new ArrayList<OntType>();
        ExtendedIterator iter;
        Ontology ont = new Ontology();
        ont.setBase(ontUrl.toString());
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        m.read(ontUrl.toString());
        readOntTypes(m.listNamedClasses(),types,styleMap,ontUrl.toString());
        readOntTypes(m.listObjectProperties(),types,styleMap,ontUrl.toString());
        readOntTypes( m.listOntProperties(),types,styleMap,ontUrl.toString());
        
        ont.setOntTypes(types);
        return ont;
    }
   
    private void readOntTypes(ExtendedIterator iter, List<OntType> types,Map<String,Style> styleMap, String ontUrl) {
        while(iter.hasNext()) {
            OntResource c = (OntResource) iter.next();
            OntType type = new OntType();
            type.setId(c.getLocalName());
            if(c.getLabel(null) == null) {
                type.setLabel(c.getLocalName());
            }else {
                type.setLabel(c.getLabel(null));
            }
            type.setBase(ontUrl.toString());
            type.setComment(c.getComment(null));
            type.setStyle(Style.getStyle(c.getLocalName(),styleMap));
            types.add(type);
        }
        
    }
    public List<Ontology> getOntList() {
        return ontList;
    }
    public Ontology getOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                return ont;
            }
        }
        return null;
    }
    
    public void removeOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                ontList.remove(ont);
                return;
            }
        }
    }
    
    public Ontology applyStyle(URL ontUrl, URL cssUrl) {
        return null;
    }
    public  static OntManager getOntManager() {
        if(ontManager == null) {
            ontManager = new OntManager();
        }
        return ontManager;
    }
    
}
