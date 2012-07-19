
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

package edu.tufts.vue.metadata;

import edu.tufts.vue.ontology.*;
import tufts.Util;
import tufts.vue.DEBUG;

import java.net.*;

/*
 * VueMetadataElement.java
 *
 * Created on June 25, 2007, 12:04 PM
 *
 * @author dhelle01
 */
public final class VueMetadataElement implements tufts.vue.XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueMetadataElement.class);

    public final static String ONTOLOGY_NONE = edu.tufts.vue.rdf.RDFIndex.VueTermOntologyNone;
    
    private static final boolean _DEBUG = false; 
    
    private String value;
    private String key;
    private Object obj;
    private int type;
   
    public static final int TAG = 0;
    public static final int CATEGORY = 1;
    public static final int ONTO_TYPE = 2;
    public static final int SEARCH_STATEMENT = 3;
    public static final int OTHER = 4;
    public static final int RESOURCE_CATEGORY = 5;

    private static final String[] _Types = {"TAG", "CAT", "ONTO", "SEARCH", "OTHER", "ResCat" };
   
    public static final String ONT_SEPARATOR = "#";
   
    public static final String VUE_ONT = Constants.ONTOLOGY_URL;//+ONT_SEPARATOR; //"vue.tufts.edu/vue.rdfs";

    public VueMetadataElement() {}
    
    public VueMetadataElement(String key, String value) {
        setKey(key);
        setValue(value);
        setType(CATEGORY);
    }
                                                         
   
    public int    getType() { return type; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public Object getObject() { return obj; }

    public boolean hasValue() { return value != null && value.trim().length() > 0; }
    public boolean isEmpty() { return key == ONTOLOGY_NONE && !hasValue(); }

    // For castor mapping file hacks -- getters always null to remove from future save files.
    // These sames make more sense when reading the mapping file.
    /** for mapping backward compat*/public String      getSetterKey() { return null; }
    /** for mapping backward compat*/public String      getSetterValue() { return null; }
    /** for mapping backward compat*/public String      getSetterType() { return null; }
    /** for mapping backward compat*/public void        setSetterKey(String k) { setKey(k); }
    /** for mapping backward compat*/public void        setSetterValue(String v) { setValue(v); }
    /** for mapping backward compat*/public void        setSetterType(String t) {
        // This is marked as string persistance so we can always returl null
        // in getter to remove from future save files.
        if (t == null || t.length() < 1) {
            System.out.println("VME: empty type in old save file: " + Util.tags(t));
            setType(0);
        } else {
            setType(t.charAt(0) - '0');
        }
    }

    // Note: setType(type) was always called LAST in the old mapping, which was importat as it has
    // side effects to this.obj / this.key that *depended* on type.  As we want type to be 1st, not
    // last, in the mapping / XML output, we handle that here via an XMLUnmarshalListener and
    // vanilla get/setXMLtype.
    
    /** @see tufts.vue.XMLUnmarshalListener */
    public void XML_initialized(Object context) {}
    public void XML_completed(Object context) {
        // This will check for applying old key patches, and initialize
        // the horrible hack of the this.obj member.
        setType(this.type);
    }
    public void XML_fieldAdded(Object context, String name, Object child) {}
    public void XML_addNotify(Object context, String name, Object parent) {}

    /** castor persist only */public void setXMLtype(int type) { this.type = type; }
    /** castor persist only */public int getXMLtype() { return type; }
   
   public void setKey(final String newKey) {
       if (ONTOLOGY_NONE.equals(newKey)) {
           this.key = ONTOLOGY_NONE;
           // performance hack: on deserialize, make objects have same identity (also allows
           // tossing this input string) todo: do for #TAG also (merge maps create lots of these)
       } else {
           this.key = newKey;
       }
       // object gets reset from persistence in setType
   }
   
   public void setValue(String value) {
       this.value = value;
       // object gets reset from persistence in setType
   }

   
    /**
     * warning: this has significant side effects -- it might change this.key and this.obj Possibly
     * changing the key is part of an old patch for bad key names.  This used to mean that persistance was
     * important: type had to come last, tho we fixed this.  This still means, however, that in using
     * the API for this class, call order makes a difference: setType calls that happened last
     * are different than those that happen first.
     */
    public void setType(int type)
    {
        if (_DEBUG) Log.debug("setType " + type + " on: " + this);
        this.type = type;
       
        if (obj == null && (type == CATEGORY || type == RESOURCE_CATEGORY || type == ONTO_TYPE)) {
            try {
                // old version before getBase modifications in OntType:
                // int len = (VUE_ONT + "#").length();
                // String[] pairedValue = {getKey().substring(len,getKey().length()),getValue()};
                //-----------------------------------------------------------------------------
                // Fix for files saved before 11/27/2007 -- file:/ prefix didn't work in
                // rdf search, so used http://vue.tufts.edu#custom.rdfs instead.
                if (key.indexOf("file") != -1 && key.indexOf("custom.rdfs") != -1) {
                    if (key.indexOf("#") != -1) {
                        Log.info("patching key in: " + key + "=" + value);
                        this.key = "http://vue.tufts.edu/custom.rdfs" + key.substring(key.indexOf("#"), key.length());
                        Log.info(" patched key to: " + key + "=" + value);
                    }
                }
                this.obj = new String[] { this.key, this.value }; // ouch -- can we say redundant?
            } catch (Throwable t) {
                Log.warn(this + "; setType(" + type + ");", t);
            }
        }
    }
   
    /** a messy and pain inducing hack this was */
    public void setObject(final Object obj)
    {
        if (DEBUG.Enabled) Log.warn("setObject->: " + this + "; obj=" + Util.color(Util.tags(obj), Util.TERM_GREEN));
       
        this.obj = obj;
        if (obj instanceof String) {
            type = TAG;
            value = (String) obj;
            key = VUE_ONT + "#TAG";
        }
        else if (obj instanceof String[]) {
            type = CATEGORY;
            value = ((String[])obj)[1];
            key = /*VUE_ONT  + "#" + */ ((String[])obj)[0];
        }
        else if (obj instanceof OntType) {
            type = ONTO_TYPE;
            OntType type = (OntType)(obj);
            value = type.getBase() + "#" + type.getLabel();
            key = VUE_ONT+"#ontoType";
        }
        else {
            type = OTHER;
        }
        
        if (DEBUG.Enabled) Log.warn("setObject=>: " + this);
        // Util.printClassTrace("!java");
    }
   
   public boolean equals(final Object other) {
       if (other instanceof VueMetadataElement) {
           final VueMetadataElement vme = (VueMetadataElement) other;
           try {
               return vme.key.equals(key) && vme.value.equals(value);
           } catch (Throwable t) {
               return false;
           }
       } else 
           return false;
   }

    public String toString() {
        final String tname =
            (type >= 0 && type < _Types.length)
            ? _Types[type]
            : ("Type" + type + "?");

        final String id = String.format("%08x", System.identityHashCode(this)).substring(4);
        return String.format("Vme_%s[%s %s=%s, o=%s]", id, tname, key, Util.tags(value), Util.tags(obj));
    }
   
   public static VueMetadataElement getNewCategoryElement()
   {
      VueMetadataElement vme = new VueMetadataElement();
      String[] emptyEntry = {ONTOLOGY_NONE,""};
      vme.setObject(emptyEntry);
      vme.setType(VueMetadataElement.CATEGORY);
      return vme;
   }
    
}
