
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
import java.util.Iterator;
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
    
    private static List<MetadataListListener> listeners = new ArrayList<MetadataListListener>();
   
    public List<VueMetadataElement> getMetadata()
    {
      return metadataList;   
    }
    
    public static void addListener(MetadataListListener listener)
    {
        listeners.add(listener);
    }
    
    private static void fireListChanged()
    {
        Iterator<MetadataListListener> i = listeners.iterator();
        while(i.hasNext())
        {
            i.next().listChanged();
        }
    }
    
    public void addElement(VueMetadataElement element)
    {
      metadataList.add(element);
      fireListChanged();
    }
    
    public void setMetadata(List<VueMetadataElement> list)
    {
      metadataList = list;
      fireListChanged();
    }
    
    public boolean containsOntologicalType(String ontType)
    {
        for(int i=0;i<getOntologyListSize();i++)
        {
            VueMetadataElement vme = getOntologyListElement(i);
            OntType type = (OntType)vme.getObject();
            System.out.println("MetadataList - containsOntologicalType - vme.getValue() " +
                               vme.getValue() + " --- ontType from properties " + ontType);
            if(ontType.equals(vme.getValue()));
                return true;
        }
        return false;
    }
    
    public VueMetadataElement getOntologyListElement(int i)
    {
        int index = i+((CategoryFirstList)metadataList).getCategoryEndIndex();
        try
        {        
          if(getOntologyListSize() > 0 && index < metadataList.size())
            return metadataList.get(index);
          else
            return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return new VueMetadataElement();
        }
    }
    
    public int getOntologyListSize()
    {
        int size = ((CategoryFirstList)metadataList).getOntologyEndIndex() - ((CategoryFirstList)metadataList).getCategoryEndIndex();
        if(size < 0)
            return 0;
        //System.out.println("MetadataList getOntologyList size - " + size);
        return size;
    }
    
    public boolean hasOntologicalMetadata()
    {
        return (getOntologyListSize() > 0);
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
        
      public int getOntologyEndIndex()
      {
          return ontologyEndIndex;
      }
      
      public boolean add(E o)
      {
          VueMetadataElement vme = null;
          if(!(o instanceof VueMetadataElement))
              return false;
          else
              vme = (VueMetadataElement)o;
          
          if(vme.getObject() instanceof OntType)
          {
              otherEndIndex++;
              add(ontologyEndIndex++,(E)vme);
          }
          else
          if(vme.getObject() instanceof String[])
          {
              ontologyEndIndex++;
              otherEndIndex++;
              add(categoryEndIndex++,(E)vme);
          }
          else
              add(otherEndIndex++,(E)vme);
          
          fireListChanged();
          
          return true;
      }
      
      public E remove(int i)
      {
          if(i<0 || i >= size())
              return null;
          
          
          if(i < categoryEndIndex && i > 0)
          {
              categoryEndIndex--;
          }
          else if(i >= categoryEndIndex && i < ontologyEndIndex)
          {
              
              if(ontologyEndIndex > 0)
              {
                  ontologyEndIndex--;
              }
              
          }
          else
          {

          }
          
          fireListChanged();
          
          
          return super.remove(i);
      }
            
    }
    
    public interface MetadataListListener
    {
       public void listChanged();
    }
}
