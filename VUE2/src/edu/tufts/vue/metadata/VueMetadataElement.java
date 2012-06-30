
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

import java.net.*;

/*
 * VueMetadataElement.java
 *
 * Created on June 25, 2007, 12:04 PM
 *
 * @author dhelle01
 */
public final class VueMetadataElement {
    
    //public final static String NONE_ONT = "http://vue.tufts.edu/vue.rdfs#none"; 
    public final static String NONE_ONT = edu.tufts.vue.rdf.RDFIndex.VueTermOntologyNone;
    
   private static final boolean DEBUG = false; 
    
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
                                                         
   
   public Object getObject() {
       return obj;
   }
   
   public void setObject(Object obj)
   {
       if (DEBUG) System.out.println("VueMetadataElement setObject -, key,value " + obj +"," + key + "," + value);
       
       this.obj = obj;
       if(obj instanceof String)
       {    
           type = TAG;
           value = (String)obj;
           key = VUE_ONT + "#TAG";
       }
       else if(obj instanceof String[])
       {
           type = CATEGORY;
           value = ((String[])obj)[1];
           key = /*VUE_ONT  + "#" + */ ((String[])obj)[0];
       }
       else if(obj instanceof OntType)
       {
           type = ONTO_TYPE;
           OntType type = (OntType)(obj);
           value = type.getBase() + "#" + type.getLabel();
           key = VUE_ONT+"#ontoType";
       }
       else
       {
           type = OTHER;
       }
   }
   
   public String getValue() {
       return value;
   }
   
   public String getKey() {
       return key;
   }
   
   public void setKey(final String newKey) {
       if (NONE_ONT.equals(newKey)) {
           this.key = NONE_ONT;
           // performance hack: on deserialize, make objects have same identity
           // (also allows tossing this input string)
           // todo someday: do for schema's in general...
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
     * warning: this has significant side effects -- it might change this.key and this.obj!
     * This may be a persistance hack?
     */
    public void setType(int type)
   {
       if (DEBUG) { System.out.println("VueMetadataElement setType -, key,value " + type +"," + key + "," + value); }
       this.type = type;
       
       if (obj == null && (type == CATEGORY || type == RESOURCE_CATEGORY || type == ONTO_TYPE))
       {
           int len = (VUE_ONT + "#").length();
           if (DEBUG) System.out.println("VueMetadataElement setType -- getKey, getValue: " + getKey() + "," + getValue());
           try {
               //old version before getBase modifications in OntType
               //String[] pairedValue = {getKey().substring(len,getKey().length()),getValue()};
               
               // fix for files saved before 11/27/2007 -- file:/ prefix didn't work in
               // rdf search, so used http://vue.tufts.edu#custom.rdfs instead.
               if(getKey().indexOf("file") != -1 && getKey().indexOf("custom.rdfs") != -1)
               {
                   if(getKey().indexOf("#") != -1)
                   {
                       key = "http://vue.tufts.edu/custom.rdfs" + getKey().substring(getKey().indexOf("#"),getKey().length());
                   }    
               }
               
               String[] pairedValue = {getKey(),getValue()};
               obj = pairedValue;
               if (DEBUG) System.out.println("recover from: " + pairedValue[0] + "," + pairedValue[1]);
           } catch (Throwable t) {
               //tufts.Util.printStackTrace(t, this+":setType(" + type + ") key["+getKey() + "] value=[" + getValue() + "]");
               System.err.println(this+": " + t + "; setType(" + type + ") key["+getKey() + "] value=[" + getValue() + "]");
           }
       }
   }

   public int getType() {
       return type;
   }
   
   public boolean equals(Object compare)
   {
       if(compare instanceof VueMetadataElement)
       {
           VueMetadataElement vme = (VueMetadataElement)compare;
           if(vme.getKey().equals(getKey()) && vme.getValue().equals(getValue()))
               return true;
           else
               return false;
       }
       else
       {
           return false;
       }
   }

    public String toString() {
        final String tname =
            (type >= 0 && type < _Types.length)
            ? _Types[type]
            : ("Type" + type + "?");
        
        return "Vme[" + tname + " " + key + "=" + Util.tags(value) + ", o=" + Util.tags(obj) + "]";
            
    }
   
   public static VueMetadataElement getNewCategoryElement()
   {
      VueMetadataElement vme = new VueMetadataElement();
      String[] emptyEntry = {NONE_ONT,""};
      vme.setObject(emptyEntry);
      vme.setType(VueMetadataElement.CATEGORY);
      return vme;
   }
    
}
