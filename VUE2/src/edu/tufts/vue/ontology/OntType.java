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

import tufts.vue.VueResources;
import edu.tufts.vue.style.*;


public class OntType implements java.io.Serializable {
    private String id;
    private String label;
    private String base;
    private String comment;
    private Style style;
    
    // for VUE2.0, unspecified styles always default to Node
    // todo: create vue resource for this choice
    // todo2: guess some reasonable defaults from ontology and save
    // information here
    private boolean defaultsToNode = true;
    /** Creates a new instance of OntType */
    public OntType() {
        
    }
   
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
        if(this.base.indexOf("/edu/tufts/vue/metadata/dces_1_1.rdf") != -1)
            return VueResources.getString("metadata.dublincore.url");
        //if( this.base.indexOf(edu.tufts.vue.metadata.CategoryModel.CUSTOM_METADATA_FILE) != -1)
        if( this.base.indexOf("edu/tufts/vue/metadata/categories.rdf") != -1)
        {
            return VueResources.getString("metadata.vue.url");
        }
        if( this.base.indexOf("custom.rdfs") != -1)
        {    
        //    return "file:///Users/dhelle01/.vue_2/custom.rdfs";
              return "http://vue.tufts.edu/custom.rdfs";
        }
        return this.base;
    }
    
    public void setBase(String base) {
        this.base = base;
    }
    
    public Style getStyle() {
        return this.style;
    }
    
    public void setStyle(Style style) {
        this.style = style;
    }
    
    public String toString() {
        return "Base: "+base+" name: "+ id+" Style: "+style;
    }
            
}
