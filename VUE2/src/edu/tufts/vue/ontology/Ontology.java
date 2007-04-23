/*
 * Ontology.java
 *
 * Created on March 12, 2007, 3:08 PM
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

import edu.tufts.vue.style.*;

import java.util.*;
import java.net.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;

public class Ontology {
    private List<OntType> types = new ArrayList<OntType>();
    private String base;
    /** Creates a new instance of Ontology */
    public Ontology() {
    }
    public List<OntType> getOntTypes() {
        return this.types;
    }
    public String getBase() {
        return this.base;
    }
    public void setBase(String base) {
        this.base = base;
    }
    public void setOntTypes(List<OntType> types) {
        this.types = types;
    }
    
    public void applyStyle(URL cssUrl) {
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        for(OntType type:types) {
            type.setStyle(Style.getStyle(type.getId(),styleMap));
        }
    }
    public String toString() {
        String s = new String();
        s = "Base: "+base;
        for(OntType o: types) {
            s += o;
        }
        return s;
    }
    
    public  void readOntTypes(ExtendedIterator iter, List<OntType> types, String ontUrl) {
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
            types.add(type);
        }
        
    }
    
    public void readOntTypes(ExtendedIterator iter, List<OntType> types,Map<String,Style> styleMap, String ontUrl) {
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
}
