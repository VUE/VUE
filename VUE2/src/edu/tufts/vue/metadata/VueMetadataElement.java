
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
public class VueMetadataElement implements tufts.vue.XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueMetadataElement.class);

    static final public String ONTOLOGY_NONE = edu.tufts.vue.rdf.RDFIndex.VueTermOntologyNone;
    static final public String ONT_SEPARATOR = "#";
 // public static final String VUE_ONT = Constants.ONTOLOGY_URL;//+ONT_SEPARATOR; //"vue.tufts.edu/vue.rdfs";
    
    static final String KEY_TAG = Constants.ONTOLOGY_URL + "#TAG";
    static final String KEY_TAG_OLD = Constants.ONTOLOGY_URL + "#Tag";
    static final String KEY_ONTO_TYPE = Constants.ONTOLOGY_URL + "#ontoType";
    static final String KEY_SOURCE = Constants.ONTOLOGY_URL + "#source";

    int type;
    String key;
    String value;
    
    private Object obj;
    
    /** don't know how TAG originally meant to be used, but a bug spent years saving maps with that
     * meant to be OTHER, which was MERGE-SOURCE, as this */
    static final int TAG = 0;     /** access: MetadataList */

    public static final int CATEGORY = 1;
    /** public for LWIcon presence-query only */
    public static final int ONTO_TYPE = 2; // can barely find any old maps with this type set, lots of code refs that send us an OntType instance will cause it to be set tho
    public static final int SEARCH_STATEMENT = 3; // tag used be search code, ends up being alias for OTHER, tho we never have these in a MetadataList
    /** access: LWIcon */
    public static final int OTHER = 4; // Current impl code has hardcoded this to mean "merge source meta-data", tho it always ended up being turned into TAG
    public static final int RESOURCE_CATEGORY = 5; /// Only ever use for actual LWMap meta-data: e.g., dc:creator, set in MapInspectorPanel

    static final int VME_EMPTY_IGNORE = Integer.MIN_VALUE; // mark for ignoring in MetadataList.SubsetList.add during deserialize
    
    static final String[] _Types = {"TAG", "CAT", "ONTO", "SEARCH", "OTHER(Merge)", "ResCat" };
   
    /** an object to make most objects look okay to MetadataList (except OntoType) and
     * prevent it from resetting type -- we should never see this key/value anywhere. */
    private static final Object UniversalOkayObject = new String[] { "[keyPlaceHolder]", "[valuePlaceHolder]" };

    protected VueMetadataElement(int type, String key, String value, Object o) {
        this.type = type >= 0 ? type : VME_EMPTY_IGNORE;
        this.key = key;
        this.value = value;
        this.obj = o;
    }

    public VueMetadataElement() {}
    
    public VueMetadataElement(String key, String value) {
        setKey(key);
        setValue(value);
        setType(CATEGORY);
    }

    /** for merge-sources */
    public static VueMetadataElement createSourceTag(String sourceString) {
        final VueMetadataElement vme = new VueMetadataElement();
        vme.type = OTHER;
        vme.key = KEY_SOURCE;
        vme.value = sourceString;
        vme.obj = UniversalOkayObject; // note this means shared, but should never be editing these...
        // try leaving object empty: RDFIndex no longer cares, and this shouldnt show up in
        // interactive UI
        return vme;
    }
                                                         
   
    public int    getType() { return type; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public Object getObject() { return obj; }

    public boolean hasValue() { return value != null && value.length() > 0; }
    public boolean isEmpty() { return key == ONTOLOGY_NONE && !hasValue(); }
    public boolean isKeyNone() { return key == ONTOLOGY_NONE; }

    // Note that for all save files prior to 2012, the deserialize call order
    // is VALUE first, then KEY, then TYPE last.

    // For castor mapping file hacks -- getters always null to remove from future save files.
    // These names make more sense when reading the mapping file.
    /** for mapping backward compat*/public String      getSetterKey() { return null; }
    /** for mapping backward compat*/public String      getSetterValue() { return null; }
    /** for mapping backward compat*/public String      getSetterType() { return null; }
    /** for mapping backward compat*/public void        setSetterKey(String k) { setKey(k); }
    /** for mapping backward compat*/public void        setSetterValue(String v) { setValue(v); }
    /** for mapping backward compat*/public void        setSetterType(String t)
    {
        // This is marked as a type java.lang.String in mapping file so we have a real Object we
        // deal with, which we need in order to return null in the getter to remove from future
        // save files.
        if (t == null || t.length() < 1) {
            if (DEBUG.Enabled) Log.debug("empty type in old save file: " + Util.tags(t));
            this.type = 0;
        } else {
            // setType(t.charAt(0) - '0');
            this.type = t.charAt(0) - '0';
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
        
        if (key == null)
            key = KEY_TAG;

        boolean isGood = true;
        
        if (key == ONTOLOGY_NONE || key == KEY_TAG) {
            // these identity objects were established in setKey
            isGood = hasValue();
        } else if (key.equals(KEY_TAG_OLD)) {
            key = KEY_TAG;
            isGood = hasValue();
        }

        if (isGood)
            setType(this.type);
        else
            this.type = VME_EMPTY_IGNORE; // mark for ignoring in MetadataList.SubsetList.add

        //if (DEBUG.Enabled) Log.debug("      xml: " + this);
        //if (DEBUG.Enabled) Log.debug("completed: " + this);
    }
    
    public void XML_fieldAdded(Object context, String name, Object child) {}
    public void XML_addNotify(Object context, String name, Object parent) {}

    /** castor persist only */public void setXMLtype(int type) { this.type = type; }
    /** castor persist only */public int getXMLtype() { return type; }
   
   public void setKey(final String key) {
       if (DEBUG.PAIN) Log.debug(this + "; setKey " + Util.tags(key));
       // performance hack: esp. for deserializing, check for key constants and change them
       // to their identity object.  Todo: change impl entirely so VME members are
       // all final.  todo: shouldn't really need this after deserialize.
       if (key != null && key.startsWith("http://vue.")) {
                if (ONTOLOGY_NONE.equals(key))  this.key = ONTOLOGY_NONE;
           else if (KEY_SOURCE.equals(key))     this.key = KEY_SOURCE;
           else if (KEY_TAG.equals(key))        this.key = KEY_TAG;
           else 
               this.key = key;
       }
       else
           this.key = key;
   }
   
   public void setValue(String value) {
       if (DEBUG.PAIN) Log.debug(this + "; setValue " + Util.tags(value));
       this.value = value;
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
        this.type = type;
       
        if (obj == null && (type == CATEGORY || type == RESOURCE_CATEGORY || type == ONTO_TYPE)) {
            try {
                // old version before getBase modifications in OntType:
                // int len = (VUE_ONT + "#").length();
                // String[] pairedValue = {getKey().substring(len,getKey().length()),getValue()};
                //-----------------------------------------------------------------------------
                // Fix for files saved before 11/27/2007 -- file:/ prefix didn't work in
                // rdf search, so used http://vue.tufts.edu#custom.rdfs instead.
                if (key != null && key.indexOf("file") != -1 && key.indexOf("custom.rdfs") != -1) {
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
   
    /** a messy and pain inducing hack this was -- appears to rarely be called directly in VUE source, so elimination shouldn't be too bad */
    public void setObject(final Object obj)
    {
        if (DEBUG.Enabled) Log.warn("setObject->: " + this + "; obj=" + Util.color(Util.tags(obj), Util.TERM_GREEN));
       
        this.obj = obj;
        if (obj instanceof String) {
            type = TAG;
            value = (String) obj;
            key = KEY_TAG;
        }
        else if (obj instanceof String[]) {
            type = CATEGORY;
            value = ((String[])obj)[1];
            key = /*VUE_ONT  + "#" + */ ((String[])obj)[0];
        }
        else if (obj instanceof OntType) {
            // This does not look like it's for setting our key *based* on an OntType,
            // it look like it's for *representing* actual OntTypes for some kind
            // ontologies UI Dan might have worked on at some point.
            type = ONTO_TYPE;
            value = ((OntType)obj).getAsKey(); // value is the OntType http://global-part#local *key*
            key = KEY_ONTO_TYPE;
        }
        else {
            type = OTHER;
        }
        
        if (DEBUG.Enabled) {
            Log.warn("setObject=>: " + this);
            //if (DEBUG.PAIN && DEBUG.META)
                Util.printClassTrace("!java");
        }
    }
   
    @Override public int hashCode() {
        if (value == null)
            return key.hashCode();
        else
            return key.hashCode() ^ value.hashCode();
    }
    
    @Override public boolean equals(final Object vme) {
        if (vme instanceof VueMetadataElement) {
            final VueMetadataElement other = (VueMetadataElement) vme;
            if (this.key == other.key && this.value == other.value)
                return true;
            try {
                return other.key.equals(key) && other.value.equals(value);
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
            : ("type" + type + "?");

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
