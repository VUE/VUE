
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
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
 *
 */

package edu.tufts.vue.metadata;

import edu.tufts.vue.ontology.*;
import edu.tufts.vue.rdf.RDFIndex;

import java.net.*;

/*
 * VueMetadataElement.java
 *
 * Created on June 25, 2007, 12:04 PM
 *
 * @author dhelle01
 */
public class VueMetadataElement {
    
   private static final boolean DEBUG_LOCAL = false; 
    
   private String value;
   private String key;
   private Object obj;
   private int type;
   
   public static final int TAG = 0;
   public static final int CATEGORY = 1;
   public static final int ONTO_TYPE = 2;
   public static final int SEARCH_STATEMENT = 3;
   public static final int OTHER = 4;
   
   public static final String ONT_SEPARATOR = "#";
   
   public static final String VUE_ONT = Constants.ONTOLOGY_URL;//+ONT_SEPARATOR; //"vue.tufts.edu/vue.rdfs";
   
   public Object getObject()
   {
       return obj;
   }
   
   public void setObject(Object obj)
   {
       
       if(DEBUG_LOCAL)
       {
           System.out.println("VueMetadataElement setObject -, key,value " + obj +"," + key + "," + value);
       }
       
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
           type = TAG;
       }
   }
   
   public String getValue()
   {
       return value;
   }
   
   public String getKey()
   {
       return key;
   }
   
   public void setKey(String key)
   {
       this.key = key;
       // object gets reset from persistence in setType
   }
   
   public void setValue(String value)
   {
       this.value = value;
       // object gets reset from persistence in setType
   }
   
    private static final boolean DEBUG = false;
   
   public void setType(int type)
   {
       
       if(DEBUG_LOCAL)
       {
           System.out.println("VueMetadataElement setType -, key,value " + type +"," + key + "," + value);
       }
       
       this.type = type;
       if( ( (type == CATEGORY) || (type == ONTO_TYPE) ) && (obj == null) )
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

   public int getType()
   {
       return type;
   }
   
   
    
}
