/*
 * OntType.java
 *
 * Created on March 12, 2007, 2:48 PM
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

import tufts.vue.VueResources;
import edu.tufts.vue.style.*;


public class OntType implements java.io.Serializable {
    private String id;
    private String label;
    private String base;
    private String comment;
    private Style style;
    /** Creates a new instance of OntType */
    public OntType() {
        
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
        if(( this.base.indexOf("edu/tufts/vue/metadata/categories.rdfs") != -1) 
             && (getLabel().equals("Tag")))
        {
            return edu.tufts.vue.metadata.ui.MetadataEditor.TAG_ONT;
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
