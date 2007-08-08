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
import org.w3c.dom.stylesheets.LinkStyle;

public class Ontology {
    public static final String ONT_CLASS_NODE = "node.OntClass";
    public static final String DEFAULT_NODE = "node.default";
    public static final String ONT_PROPERTY_LINK = "link.OntProperty";
    public static final String DEFAULT_LINK = "link.default";
    public static final String DEFAULT_ONT_LABEL = "Ontology ";
    protected List<OntType> types = new ArrayList<OntType>();
    static int ONT_COUNTER  =0;
    URL cssUrl;
    OntModel m;
    private String base;
    private String label;
    /** Creates a new instance of Ontology */
    public Ontology() {
        setLabel();
    }
    public List<OntType> getOntTypes() {
        return this.types;
    }
    public String getBase() {
        return this.base;
    }
    public void setBase(String base) {
        this.base = base;
        this.label = getLabelFromUrl(base);
    }
    public String getLabel() {
        return this.label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public void setLabel() {
        ONT_COUNTER++;
        label = DEFAULT_ONT_LABEL+ONT_COUNTER;
    }
    public void setOntTypes(List<OntType> types) {
        this.types = types;
    }
    
    public static String getLabelFromUrl(String url) {
        int start = Math.max(0,url.lastIndexOf("/")+1);
        int stop =url.length()-1;
        if(url.lastIndexOf(".")>0) {
            stop = url.lastIndexOf(".");
        }
        return url.substring(start,stop);
    }
    public void applyStyle(URL cssUrl) {
        this.cssUrl = cssUrl;
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        for(OntType type:types) {
            type.setStyle(Style.getStyle(type.getId(),styleMap));
        }
    }
    
    public URL getStyle() {
        return this.cssUrl;
    }
    public void setCssFileName(String fileName) {
        try {
            cssUrl = new URL(fileName);
        } catch(Exception ex)  {
            System.out.println("Ontology.cssFileName" + ex);
        }
    }
    public String getCssFileName() {
        return cssUrl.toString();
    }
    public String toString() {
        String s = new String();
        s = "Base: "+base;
        for(OntType o: types) {
            s += o;
        }
        return s;
    }
    
    public  void readOntTypes(ExtendedIterator iter) {
        while(iter.hasNext()) {
            OntResource c = (OntResource) iter.next();
            OntType type = new OntType();
            type.setId(c.getLocalName());
            if(c.getLabel(null) == null) {
                type.setLabel(c.getLocalName());
            }else {
                type.setLabel(c.getLabel(null));
            }
            type.setBase(base);
            type.setComment(c.getComment(null));
            Style style;
            if(c instanceof OntClass)
                style = NodeStyle.DEFAULT_NODE_STYLE;
            else
                style = edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE;
            type.setStyle(style);
            types.add(type);
            
        }
        
    }
    
    public void readOntTypes(ExtendedIterator iter,URL cssUrl) {
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        while(iter.hasNext()) {
            OntResource c = (OntResource) iter.next();
            OntType type = new OntType();
            type.setId(c.getLocalName());
            if(c.getLabel(null) == null) {
                type.setLabel(c.getLocalName());
            }else {
                type.setLabel(c.getLabel(null));
            }
            type.setBase(base);
            type.setComment(c.getComment(null));
            Style style = Style.getStyle(c.getLocalName(),styleMap);
            if(style == edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE){
                if(c instanceof OntClass) {
                    style = Style.getStyle(ONT_CLASS_NODE,styleMap);
                } else {
                    style = Style.getStyle(ONT_PROPERTY_LINK,styleMap);
                }
            }
            if(style == edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE) {
                if(c instanceof OntClass) {
                    style = Style.getStyle(DEFAULT_NODE,styleMap);
                } else {
                    style = Style.getStyle(DEFAULT_LINK,styleMap);
                }
            }
            if((c instanceof OntClass) && (style == edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE)) {
                style = NodeStyle.DEFAULT_NODE_STYLE;
            }
            type.setStyle(style);
            types.add(type);
        }
        
    }
    
    
}
