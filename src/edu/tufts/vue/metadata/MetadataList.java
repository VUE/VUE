
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Map;

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
    
    private final static boolean DEBUG_LOCAL = false;
    
    //todo: add to VueResources.properties
    public static final String MERGE_SOURCES_TITLE = "Merged from:";
    
    private List<VueMetadataElement> metadataList = new CategoryFirstList<VueMetadataElement>();
    
    private static List<MetadataListListener> listeners = new ArrayList<MetadataListListener>();
    
    private SubsetList rCategoryList;
    private SubsetList categoryList;
    private SubsetList ontologyList;
    private SubsetList otherList;
    
    public MetadataList()
    {
        rCategoryList = new SubsetList(VueMetadataElement.RESOURCE_CATEGORY);
        categoryList = new SubsetList(VueMetadataElement.CATEGORY);
        ontologyList = new SubsetList(VueMetadataElement.ONTO_TYPE);
        otherList = new SubsetList(VueMetadataElement.OTHER);
    }
   
    public List<VueMetadataElement> getMetadata()
    {
        return metadataList;
    }

    public boolean intersects(MetadataList remote) {
        final List remoteList = remote.getMetadata();
        for (VueMetadataElement e : metadataList)
            if (remoteList.contains(e))
                return true;
        return false;
    }

    public static void addListener(MetadataListListener listener)
    {
        listeners.add(listener);
    }
    
    private static boolean disableEvents = false;
    private static void fireListChanged()
    {
        if (disableEvents)
            return;
        Iterator<MetadataListListener> i = listeners.iterator();
        while(i.hasNext())
        {
            i.next().listChanged();
        }
    }
    
    /** attempt at high-performace bulk load that triggers no GUI updates */
    public void add(Iterable<Map.Entry> kvEntries) {

        disableEvents = true;
        // oddly, firing list changed events get much slower as amount of total meta-data increases        

        // the categoryList updates still trigger the updates, so we turn them off with
        // a flag for now
        
        for (Map.Entry e : kvEntries)
            categoryList.add(new VueMetadataElement(e.getKey().toString(), e.getValue().toString()));
        disableEvents = false;
        fireListChanged();
        
        //metadataList.add(new VueMetadataElement(e.getKey(), e.getValue().toString()));
        
    }
    
    public void add(String key, String value) {
        addElement(new VueMetadataElement(key, value));
    }
    
    public void addElement(VueMetadataElement element)
    {
      if(DEBUG_LOCAL)
      {
          System.out.println("MetadataList addElement - " + element);
      }
        
      metadataList.add(element);
      fireListChanged();
    }
    
    public void remove(String key, String value)
    {
    	int found = -1;

        for (int i = 0; i < getCategoryListSize(); i++)
        {
            VueMetadataElement vme = getCategoryListElement(i);

            if (vme.getKey().equals(key) && vme.getValue().equals(value))
            {
                found = i;
                break;
            }
        }

        if (found != -1)
        {
        	remove(found);
        }
    }

     public int size()
    {
    	return metadataList.size();

    }
    
    public void remove(int i)
    {
    	metadataList.remove(i);
    	fireListChanged();
    }
    
    
    public VueMetadataElement get(int i)
    {
    	return metadataList.get(i);
    }

    public void modify(VueMetadataElement element)
    {
      if(element.getType() == VueMetadataElement.CATEGORY)
      {
          int i = findCategory(element.getKey());
          if(i!=-1)
          {    
            metadataList.set(i,element);
            fireListChanged();
          }
      }
      if(element.getType() == VueMetadataElement.RESOURCE_CATEGORY)
      {
          int i = findRCategory(element.getKey());
          if(i!=-1)
          {    
            metadataList.set(i,element);
            fireListChanged();
          }
      }
    }
    
    public VueMetadataElement get(String key)
    {
        int rcindex = findRCategory(key);
        int cindex = findCategory(key);
        if(rcindex != -1)
            return getRCategoryListElement(rcindex);
        if(cindex != -1)
            return getCategoryListElement(cindex);
        else
            return null;
    }
    
    /*public int categoryIndexOfFirstWithValueAndKey(String key,String value)
    {
        
    }*/

     /**
     *
     * finds the first entered (last in order)
     * category element with the supplied key
     *
     * returns -1 if not found.
     * 
     **/
    public int findCategory(String key)
    {
        int foundAt = -1;
        for(int i=0;i<getCategoryListSize();i++)
        {
            VueMetadataElement vme = getCategoryListElement(i);
            if(vme.getKey().equals(key) && foundAt == -1)
            {
                foundAt = i;
            }
        }
        
        return foundAt;
    }

    public boolean contains(VueMetadataElement vme) {
        return contains(vme.getKey(), vme.getValue());
    }
    
    public boolean contains(String key, String value) 
    {
        int foundAt = -1;
        for(int i=0;i<getCategoryListSize();i++)
        {
            VueMetadataElement vme = getCategoryListElement(i);
            if(vme.getKey().equals(key) && vme.getValue().equals(value))
                return true;
        }
        return false;
    }

   public int findRCategory(String key)
    {
        int foundAt = -1;
        for(int i=0;i<getRCategoryListSize();i++)
        {
            VueMetadataElement vme = getRCategoryListElement(i);
            if(vme.getKey().equals(key) && foundAt == -1)
            {
                foundAt = i;
            }
        }
        
        return foundAt;
    }
    
    /**
     *
     * finds the most recently entered (last in order)
     * category element with the supplied key
     *
     * returns -1 if not found.
     * 
     **/
    public int findMostRecentCategory(String key)
    {
        int foundAt = -1;
        int startPoint = ((CategoryFirstList)metadataList).getRCategoryEndIndex();
        for(int i=startPoint;i<getCategoryListSize();i++)
        {
            VueMetadataElement vme = getCategoryListElement(i);
            if(vme.getKey().equals(key))
            {
                foundAt = i;
            }
        }
        
        return foundAt;
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
            if(DEBUG_LOCAL)
            {
              System.out.println("MetadataList - containsOntologicalType - vme.getValue() " +
                               vme.getValue() + " --- ontType from properties " + ontType);
            }
            if(ontType.equals(vme.getValue()));
                return true;
        }
        return false;
    }

    public VueMetadataElement getCategoryListElement(int i)
    {
        int index = i;
        try
        {        
          if(getCategoryListSize() > 0 && index < metadataList.size())
            return metadataList.get(index+((CategoryFirstList)metadataList).getRCategoryEndIndex());
          //else
            //return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return null;
            //return new VueMetadataElement(); 
        }
        
        return null;
    }
    
    public void setCategoryListElement(int i,VueMetadataElement ele)
    {
        int index = i;
        try
        {        
          if(getCategoryListSize() > 0 && index < metadataList.size())
            metadataList.set(index+ ((CategoryFirstList)metadataList).getRCategoryEndIndex(),ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    public int getRCategoryListSize()
    {
        CategoryFirstList cf = ((CategoryFirstList)metadataList);
        return cf.getRCategoryEndIndex();
    }
    
    public VueMetadataElement getRCategoryListElement(int i)
    {
        int index = i;
        try
        {        
          if(getRCategoryListSize() > 0 && index < metadataList.size())
            return metadataList.get(index);
          else
            return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return new VueMetadataElement();
        }
    }
    
    public void setRCategoryListElement(int i,VueMetadataElement ele)
    {
        int index = i;
        try
        {        
          if(getRCategoryListSize() > 0 && index < metadataList.size())
            metadataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    public int getCategoryListSize()
    {
        CategoryFirstList cf = ((CategoryFirstList)metadataList);
        return cf.getCategoryEndIndex()-cf.getRCategoryEndIndex();
    }
    
    public VueMetadataElement getOntologyListElement(int i)
    {
        CategoryFirstList cf = ((CategoryFirstList)metadataList);
        int index = i+cf.getCategoryEndIndex();
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
    
    public void setOntologyListElement(int i,VueMetadataElement ele)
    {
        CategoryFirstList cf = ((CategoryFirstList)metadataList);
        int index = i+cf.getCategoryEndIndex();
        try
        {        
          if(getOntologyListSize() > 0 && index < metadataList.size())
            metadataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    public int getOntologyListSize()
    {
        int size = ((CategoryFirstList)metadataList).getOntologyEndIndex() - ((CategoryFirstList)metadataList).getCategoryEndIndex();
        
        // produces a lot of output..
        /*if(DEBUG_LOCAL)
        {
            System.out.println("MetadataList - getOntologyListSize: " + size);
        }*/
        
        return size;
    }
    
    public VueMetadataElement getOtherListElement(int i)
    {
        int index = i+((CategoryFirstList)metadataList).getOntologyEndIndex();
        try
        {        
          if(getOtherListSize() > 0 && index < metadataList.size())
            return metadataList.get(index);
          else
            return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return new VueMetadataElement();
        }
    }
    
    public void setOtherListElement(int i,VueMetadataElement ele)
    {
        int index = i+((CategoryFirstList)metadataList).getOntologyEndIndex();
        try
        {        
          if(getOtherListSize() > 0 && index < metadataList.size())
            metadataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    public int getOtherListSize()
    {

        
        int size = ((CategoryFirstList)metadataList).getOtherEndIndex() - ((CategoryFirstList)metadataList).getOntologyEndIndex();

        if(DEBUG_LOCAL)
        {
            System.out.println("Metadatalist getOtherListSize() " + size );
        }
        
        return size;
    }
    
    public boolean hasOntologicalMetadata()
    {
        return (getOntologyListSize() > 0);
    }
    
    public String getOntologyListString()
    {
        String returnString = "";
        
        for(int i=0;i<getOntologyListSize();i++)
        {
            VueMetadataElement vme = getOntologyListElement(i);
            returnString += vme.getObject() + "|";
        }
        
        return returnString;
    }
    
    public boolean hasMetadata(int type)
    {
        return getMetadataAsHTML(type).length() > 0;
    }
    
    public String getMetadataAsHTML(int type)
    {
        
        SubsetList mdList = null;
        
        if(type == VueMetadataElement.CATEGORY)
        {
            mdList = getCategoryList();
        }
        else if(type == VueMetadataElement.ONTO_TYPE)
        {
            mdList = getOntologyList();
        }
        else
        {
            mdList = getOtherList();
        }
        
        if(mdList.size() > 0) {
            String txt = "";
            for (int i=0;i<mdList.size();i++) {
                String value = mdList.get(i).getValue();
                
                if(DEBUG_LOCAL)
                {
                    System.out.println("Metadatalist -- getMetadataAsHTML loop -- value for " + i + " type: " + type + " value: " + value);
                }

                if (value == null) {
                    System.out.println("null meta-data value for " + mdList.get(i));
                    continue;
                }
                
                if(value.length() > 0)
                {    
                  if(type == VueMetadataElement.ONTO_TYPE)
                  {
                      int nameLocation = value.indexOf(VueMetadataElement.ONT_SEPARATOR);
                      if(nameLocation > -1 && value.length() > nameLocation + 1)
                      {
                          value = value.substring(nameLocation + 1);
                      }
                  }
                  
                  if(type == VueMetadataElement.OTHER)
                  {
                     int cLocation = value.indexOf(":");
                     if(cLocation > -1 && value.length() > cLocation + 1);
                       value = value.substring(cLocation + 1);  
                       
                     int dotLocation = value.lastIndexOf(".");
                     if(dotLocation != -1)
                     {
                         int commaLocation = value.indexOf(",");
                         
                         String endPart = "";
                         
                         if(commaLocation != -1 && commaLocation > dotLocation)
                         {
                             endPart = value.substring(commaLocation);
                         }
                         
                         value = value.substring(0,dotLocation) + endPart;
                     }
                       
                     value = "-" + value;
                  }    
                  
                  txt += "<br>" + value;
                }
            }
            
            if(txt.length() > 0)
            {
                if(type == VueMetadataElement.CATEGORY)
                {    
                  txt = "Keywords: " + txt;
                }
                else if(type == VueMetadataElement.ONTO_TYPE)
                {
                  txt = "Ontological Membership: " + txt;
                }
                else if(type == VueMetadataElement.OTHER)
                {
                  //int dotLocation = txt.indexOf(".");
                  //if(dotLocation > 0)
                  //    txt = txt.substring(0,dotLocation);
                  txt = MERGE_SOURCES_TITLE + txt;
                }
            }
            
            return txt;
        } 
        else 
        {
            return "";
        }
    }
    
    public SubsetList getCategoryList()
    {
        return categoryList;
    }
    
    public SubsetList getOntologyList()
    {
        return ontologyList;
    }
    
    public SubsetList getOtherList()
    {
        return otherList;
    }
    public class SubsetList
    {
        private int type;
        
        public SubsetList(int type)
        {
            this.type = type;
        }
        
        public int indexOf(VueMetadataElement vme)
        {   
           int size = 0;
           // not yet needed -- also add in contains..
           //if(type == VueMetadataElement.RESOURCE_CATEGORY)
           //    size = getRCategoryListSize();
           if(type == VueMetadataElement.CATEGORY)
               size = getCategoryListSize();
           else if(type == VueMetadataElement.ONTO_TYPE)
               size = getOntologyListSize();
           else
               size = getOtherListSize();
           
           for(int i=0;i<size;i++)
           {
               if(type == VueMetadataElement.CATEGORY)
                  if(getCategoryListElement(i).equals(vme))
                      return i;
               else if(type == VueMetadataElement.ONTO_TYPE)
                  if(getOntologyListElement(i).equals(vme))
                      return i;
               else
                  if(getOtherListElement(i).equals(vme))
                      return i;
           }
           
           //note: currently resource categories are not found.
           return -1;
        }
        
        public boolean contains(VueMetadataElement vme)
        {  
           int size = 0;
           if(type == VueMetadataElement.CATEGORY)
               size = getCategoryListSize();
           else if(type == VueMetadataElement.ONTO_TYPE)
               size = getOntologyListSize();
           else
               size = getOtherListSize();
           
           for(int i=0;i<size;i++)
           {
               if(type == VueMetadataElement.CATEGORY)
                  if(getCategoryListElement(i).equals(vme))
                      return true;
               else if(type == VueMetadataElement.ONTO_TYPE)
                  if(getOntologyListElement(i).equals(vme))
                      return true;
               else
                  if(getOtherListElement(i).equals(vme))
                      return true;
           }
           
           // note: as in indexOf() currently resouerce categories are not found.
           return false;
        }
        
        public void add(VueMetadataElement vme)
        {
            ((CategoryFirstList)getMetadata()).add(vme);
        }
        
        
        public int size()
        {
           if(type ==  VueMetadataElement.RESOURCE_CATEGORY)
           {
               return getRCategoryListSize();
           }
           else
           if(type == VueMetadataElement.CATEGORY)
               return getCategoryListSize();
           else if(type == VueMetadataElement.ONTO_TYPE)
               return getOntologyListSize();
           else
               return getOtherListSize();
        }
        
        public VueMetadataElement get(int i)
        {
           if(type == VueMetadataElement.RESOURCE_CATEGORY)
               return getRCategoryListElement(i);
           else if(type == VueMetadataElement.CATEGORY)
               return getCategoryListElement(i);
           else if(type == VueMetadataElement.ONTO_TYPE)
               return getOntologyListElement(i);
           else
               return getOtherListElement(i);            
        }
        
        public void set(int i,VueMetadataElement ele)
        {
           if(type == VueMetadataElement.RESOURCE_CATEGORY)
           {
               setRCategoryListElement(i,ele);
           } else
           if(type == VueMetadataElement.CATEGORY)
               setCategoryListElement(i,ele);
           else if(type == VueMetadataElement.ONTO_TYPE)
               setOntologyListElement(i,ele);
           else
               setOtherListElement(i,ele);            
        }
        
        public java.util.List getList()
        {
            java.util.List returnList = new java.util.ArrayList();
            for(int i=0;i<size();i++)
            {
                returnList.add(get(i));
            }
            return returnList;
        }
    }
    
    public class CategoryFirstList<E> extends java.util.ArrayList<E>
    {
        
      int categoryEndIndex = 0;
      int ontologyEndIndex = 0;
      int otherEndIndex = 0;
      int rCategoryEndIndex = 0; // RESOURCE_CATEGORY
      
      public int getRCategoryEndIndex()
      {
          return rCategoryEndIndex;
      }
      
      public int getCategoryEndIndex()
      {
          return categoryEndIndex;
      }
        
      public int getOntologyEndIndex()
      {
          return ontologyEndIndex;
      }
  
      public int getOtherEndIndex()
      {
          return otherEndIndex;
      }
      
      public boolean add(E o)
      {
          
          if(DEBUG_LOCAL)
          {
            if(o instanceof VueMetadataElement)
            {    
              VueMetadataElement e = (VueMetadataElement)o;
              System.out.println("MetadataList adding object -- o.getObject() type " + e.getType());
            }
            //else
            //{
              //System.out.println("MetadataList non vme added to category first list " + o.getClass());
            //}
              
            //System.out.println("MetadataList categoryFirstList add - categoryEndIndex, ontologyEndIndex, size - " + o +"," +
            //      categoryEndIndex + "," + ontologyEndIndex + "," + size());
          }
          
          VueMetadataElement vme = null;
          if(!(o instanceof VueMetadataElement))
              return false;
          else
              vme = (VueMetadataElement)o;
          
          if(vme.getObject() == null)
          {
              
              if(DEBUG_LOCAL)
              {
                  System.out.println("Metadatalist categoryfirstindex add - setting type " + vme.getType());
              }
              
              vme.setType(vme.getType());
          }
          
          if(DEBUG_LOCAL)
          {
             
            System.out.println("MetadataList - categoryfirstList add - after set type check " );  
              
            if(o instanceof VueMetadataElement)
            {    
              VueMetadataElement e = (VueMetadataElement)o;
              System.out.println("MetadataList adding object -- o.getObject() (may need set type) " + e.getObject()); 
              System.out.println("MetadataList adding object -- o.getObject() type " + e.getType());
            }
            else
            {
              System.out.println("MetadataList non vme added to category first list " + o.getClass());
            }
              
            System.out.println("MetadataList categoryFirstList add - categoryEndIndex, ontologyEndIndex, size - " + o +"," +
                  categoryEndIndex + "," + ontologyEndIndex + "," + size());
          }
          
          if(vme.getObject() instanceof OntType || vme.getType() == VueMetadataElement.ONTO_TYPE ) 
          {
              otherEndIndex++;
              add(ontologyEndIndex++,(E)vme);
          }
          else
          if(vme.getType() == VueMetadataElement.RESOURCE_CATEGORY && vme.getObject() instanceof String[] )
          {
              if(DEBUG_LOCAL)
              {
                  System.out.println("MetadataList adding RESOURCE_CATEGORY " + vme.getValue());
              }
              
              ontologyEndIndex++;
              categoryEndIndex++;
              otherEndIndex++;
              add(rCategoryEndIndex++,(E)vme);
              
              if(DEBUG_LOCAL)
              {
                  System.out.println("MetadataList adding RESOURCE_CATEGORY rCategoryIndex is now: " + rCategoryEndIndex);
              }
              
          }
          else
          if(vme.getObject() instanceof String[] && vme.getType() == VueMetadataElement.CATEGORY )
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
          if(i < rCategoryEndIndex)
          {
              rCategoryEndIndex --;
              if(categoryEndIndex > 0)
              {    
                ontologyEndIndex--;
              }
              if(ontologyEndIndex > 0)
              {    
                ontologyEndIndex--;
              }
              if(otherEndIndex > 0)
              {
                otherEndIndex--;
              }
          }
          if(i < categoryEndIndex && i >= rCategoryEndIndex)
          {
              categoryEndIndex--;
              if(ontologyEndIndex > 0)
              {    
                ontologyEndIndex--;
              }
              if(otherEndIndex > 0)
              {
                otherEndIndex--;
              }
          }
          else if(i >= categoryEndIndex && i < ontologyEndIndex)
          {
              ontologyEndIndex--;
              otherEndIndex--;
          }


          else
          {
              otherEndIndex--;
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
