/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import tufts.vue.VueResources;
import edu.tufts.vue.style.*;


public class OntType implements java.io.Serializable {
    private String id;
    private String label;
    private String base;
    private String comment;
    private Style style;

    private String baseURL;
    
    // for VUE2.0, unspecified styles always default to Node
    // todo: create vue resource for this choice
    // todo2: guess some reasonable defaults from ontology and save
    // information here
    private boolean defaultsToNode = true;
    /** Creates a new instance of OntType */
    public OntType() { }
   
    public void setDefaultsToNode(boolean set)
    {
        defaultsToNode = set;
    }
    
    public boolean defaultsToNode()
    {
        return defaultsToNode;
    }
   
    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return this.id;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getLabel() {
        return this.label;
    }
   
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getComment() {
        return this.comment;
    }
    
    public String getBase() {
        return this.baseURL;
    }

    public String getAsKey() {
        final StringBuilder b = new StringBuilder(baseURL.length() + 2 + label.length());
        b.append(baseURL);
        b.append('#');
        b.append(label);
        return b.toString();
    }
    

    /** the given url key must end with an exact case-sensitive match to this.label -- this.id is ignored */
    public boolean matchesKey(String key) {
        if (!key.startsWith(baseURL))
            return false;
        final int localPart = key.indexOf('#');
        return
            localPart > 0 &&
            localPart < key.length() - 1 &&
            key.substring(localPart + 1).equals(label);
    }
    
    public void setBase(final String base) {
        this.base = base;
        // NOTE: this has never done anything for vra_core_3.rdf, so the base ends up being the full filename to
        // the jar in the application install -- is that okay?
        if (base.indexOf("/edu/tufts/vue/metadata/dces_1_1.rdf") != -1)
            baseURL = VueResources.local("metadata.dublincore.url");
     // else if (base.indexOf(edu.tufts.vue.metadata.CategoryModel.CUSTOM_METADATA_FILE) != -1)
        else if (base.indexOf("edu/tufts/vue/metadata/categories.rdf") != -1)
            baseURL = VueResources.local("metadata.vue.url");
        else if (base.indexOf("custom.rdfs") != -1)
            // return "file:///Users/dhelle01/.vue_2/custom.rdfs";
            baseURL = "http://vue.tufts.edu/custom.rdfs";
        else
            baseURL = base;
    }
    
    public Style getStyle() {
        return this.style;
    }
    
    public void setStyle(Style style) {
        this.style = style;
    }
    
    public String toString() {
        final String xid = (id == null ? "" : id);
        if (baseURL != base)
            return baseURL+" for "+base+" id="+ id + (!xid.equals(label) ? (" label=" + label) : "");
        else
            return base+" id="+ id + (!xid.equals(label) ? (" label=" + label) : "");
        //return "Base: "+base+" name: "+ id+" Style: "+style;
    }
            
}
