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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tufts.vue.VueResources;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.tufts.vue.style.CSSParser;
import edu.tufts.vue.style.NodeStyle;
import edu.tufts.vue.style.Style;

abstract public class Ontology {
    public static final String ONT_CLASS_NODE = "node.OntClass";
    public static final String DEFAULT_NODE = "node.default";
    public static final String ONT_PROPERTY_LINK = "link.OntProperty";
    public static final String DEFAULT_LINK = "link.default";
    public static final String DEFAULT_ONT_LABEL = VueResources.getString("ontology.label");
    private boolean enabled = true;
    
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
    
    public void setEnabled(boolean enable)
    {
    	this.enabled=enable;
    }
    
    public boolean isEnabled()
    {
    	return enabled;
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
        
        int start = url.lastIndexOf("/");
        
        if(start == -1 || (start == url.length() -1) )
            return url;
        else
            url = url.substring(start + 1);
        
        int stop = url.lastIndexOf(".");
        
        if(stop == -1)
            return url;
        
        return url.substring(0,stop);
        
 
    }
    
    public void applyStyle(URL cssUrl) {
        this.cssUrl = cssUrl;
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        for(OntType type:types) {
            type.setStyle(Style.getStyle(type.getId(),styleMap));
        }
    }
    
    public void applyDefaultStyle()
    {
        for(OntType type:types) {
            
            // todo: if default ever changes back to mix of nodes and links then
            // may want to use this approach:
            /*if(type.defaultsToNode)
            {
                type.setStyle(NodeStyle.DEFAULT_NODE_STYLE);
            }
            else
            {
                type.setStyle(edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE);
            }*/
            // for now, don't override link/node info from previous loaded style
            // todo: strictly speaking, since type of component is part of the
            // style should go back to "all nodes" for default. However, there
            // is currently no global property that determines this, its
            // just the intializer in OntType (see todo: note in OntType)
            // so be careful about overriding based on node/link type and/or
            // add global property in future
            if(type.getStyle() instanceof NodeStyle)
            {
                type.setStyle(NodeStyle.DEFAULT_NODE_STYLE);
            }
            else
            {
                type.setStyle(edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE);
            }
        }
            
    }
    
    public URL getStyle() {
        return this.cssUrl;
    }
    public void setCssFileName(String cssFileName) {
        try {
            cssUrl = new URL(cssFileName);
        } catch(Exception ex)  {
            System.out.println("Ontology.cssFileName" + ex);
        }
    }
    public String getCssFileName() {
        if(cssUrl == null) {
            return null;
        }else
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
    
    public boolean equals(Object o) {
        if(o instanceof Ontology) {
            Ontology ontology = (Ontology)o;
            
            /*if(this.base == null || this.label == null)
                return false;
            if(ontology.getBase() == null || ontology.getLabel() == null)
                return false;*/
            
            if(this.base.equals(ontology.getBase()) && this.label.equals(ontology.getLabel())) {
                return true;
            } else {
                return false;
            }
        }else {
            return false;
        }
    }
    public  void readOntTypes(ExtendedIterator iter) {
        while(iter.hasNext()) {
            OntResource c = (OntResource) iter.next();
            
            // getLocalName only gets the value passed any spaces
            // todo: find documentation -- is it a bug in Jena or a feature?
            String fullString = c.toString();
            int dividerIndex = fullString.indexOf("#");
            String name = "";
            if(dividerIndex != -1 && dividerIndex < fullString.length() - 2)
            {
               name = fullString.substring(dividerIndex+1);                
            }
            
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
            
            // see comment about getLocalName() above
            if(!name.equals(""))
            {
                type.setId(name);
                type.setLabel(name);
            }
            
            // disable for now, see VUE-866 reapplying style without rereading
            // ontology loses this styling information
            // what we really need to do, is to reapply the node/link choice
            // from load time for nodes or links that are not specifically 
            // styled by the css style sheet
            /*if(c instanceof OntClass)
                style = NodeStyle.DEFAULT_NODE_STYLE;
            else
                style = edu.tufts.vue.style.LinkStyle.DEFAULT_LINK_STYLE;*/
            
            // just default to node for now (as is done in case of no style
            // found for consistency --- again see VUE-866)
            style = NodeStyle.DEFAULT_NODE_STYLE;
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
                type.setDefaultsToNode(true);
            }
            type.setStyle(style);
            types.add(type);
        }
        
    }
    
    abstract public void readAllSupportedOntTypes();
    
    abstract public void readAllSupportedOntTypesWithCss();
    
    
}
