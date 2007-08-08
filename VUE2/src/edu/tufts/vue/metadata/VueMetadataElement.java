
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

import java.net.*;

/*
 * VueMetadataElement.java
 *
 * Created on June 25, 2007, 12:04 PM
 *
 * @author dhelle01
 */
public class VueMetadataElement {
    
   private String value;
   private URI key;
   private Object obj;
   private int type;
   
   public static final int TAG = 0;
   public static final int CATEGORY = 1;
   public static final int ONTO_TYPE = 2;
   
   public static final String VUE_ONT = "vue.tufts.edu/vue.rdfs";
   
   public Object getObject()
   {
       return obj;
   }
   
   public void setObject(Object obj)
   {
       this.obj = obj;
       if(obj instanceof String)
       {    
           type = TAG;
           value = (String)obj;
           try
           {
             key = new URI(VUE_ONT + "#TAG");
           }
           catch(URISyntaxException exc)
           {
             System.out.println("VueMetadataElement setObject: URISyntaxException: " + exc);
           }
       }
       else if(obj instanceof String[])
       {
           type = CATEGORY;
           value = ((String[])obj)[1];
           try
           {
             key = new URI(VUE_ONT + "#" + ((String[])obj)[2]);
           }
           catch(URISyntaxException exc)
           {
             System.out.println("VueMetadataElement setObject: URISyntaxException: " + exc);
           }
       }
       else
       {
           type = ONTO_TYPE;
           OntType type = (OntType)(obj);
           value = type.getBase() + "#" + type.getLabel();
           try
           {
             key = new URI(VUE_ONT+"#ontoType");
           }
           catch(URISyntaxException exc)
           {
             System.out.println("VueMetadataElement setObject: URISyntaxException: " + exc);
           }
       }
   }
   
   public String getValue()
   {
       return value;
   }
   
   public URI getKey()
   {
       return key;
   }
   
   public void setKey(URI key)
   {
       this.key = key;
       // how recreate obj?
   }
   
   public void setValue(String value)
   {
       this.value = value;
       // how recreate obj;
   }
    
}
