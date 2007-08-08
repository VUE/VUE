
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
   private String key;
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
           key = VUE_ONT + "#TAG";
       }
       else if(obj instanceof String[])
       {
           type = CATEGORY;
           value = ((String[])obj)[1];
           key = VUE_ONT + "#" + ((String[])obj)[2];
       }
       else
       {
           type = ONTO_TYPE;
           OntType type = (OntType)(obj);
           value = type.getBase() + "#" + type.getLabel();
           key = VUE_ONT+"#ontoType";
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
       // object does not currently get recreated from persistence, 
       // gui handles this
   }
   
   public void setValue(String value)
   {
       this.value = value;
       // object does not currently get recreated from persistence, 
       // gui handles this
   }
   
   public void setType(int type)
   {
       this.type = type;
   }
   
   public int getType()
   {
       return type;
   }
    
}
