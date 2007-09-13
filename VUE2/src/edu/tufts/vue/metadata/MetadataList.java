
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * MetadataList.java
 *
 *
 * The metadata associated with a generic LWComponent
 *
 * Created on June 25, 2007, 12:03 PM
 *
 * @author dhelle01
 */
public class MetadataList {
    
    private List<VueMetadataElement> metadataList = new CategoryFirstList<VueMetadataElement>();
   
    public List<VueMetadataElement> getMetadata()
    {
      return metadataList;   
    }
    
    public void addElement(VueMetadataElement element)
    {
      metadataList.add(element);
    }
    
    public void setMetadata(List<VueMetadataElement> list)
    {
      metadataList = list;
    }
    
    public class CategoryFirstList<E> extends java.util.ArrayList<E>
    {
        
      int categoryEndIndex = 0;
      int ontologyEndIndex = 0;
      int otherEndIndex = 0;
      
      public int getCategoryEndIndex()
      {
          return categoryEndIndex;
      }
        
      public boolean add(E o)
      {
          VueMetadataElement vme = null;
          if(!(o instanceof VueMetadataElement))
              return false;
          else
              vme = (VueMetadataElement)o;
          
          if(vme.getObject() instanceof Ontology)
          {
              otherEndIndex++;
              add(ontologyEndIndex++,(E)vme);
          }
          if(vme.getObject() instanceof String[])
          {
              ontologyEndIndex++;
              otherEndIndex++;
              add(categoryEndIndex++,(E)vme);
          }
          else
              add(otherEndIndex++,(E)vme);
              
          return true;
      }
      
      public E remove(int i)
      {
          if(i < categoryEndIndex)
          {
              categoryEndIndex--;
          }
          else if(i >= categoryEndIndex && i < ontologyEndIndex)
          {
              categoryEndIndex--;
              ontologyEndIndex--;
          }
          else
          {
              categoryEndIndex--;
              ontologyEndIndex--;
          }
          return super.remove(i);
      }
            
    }
}
