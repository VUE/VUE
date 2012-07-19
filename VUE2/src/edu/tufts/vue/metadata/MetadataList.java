
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Map;


/*
 * MetadataList.java
 *
 * The metadata associated with a generic LWComponent
 *
 * Created on June 25, 2007, 12:03 PM
 *
 * @author dhelle01
 *
 * S.Fraize 2012: attempt to clean up a bizzarely designed class, just to see clearly what it was
 * really attempting to do.  Fixed many bad bugs.  Todo: completely throw this out.
 */
public class MetadataList implements tufts.vue.XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetadataList.class);
    
    private final static boolean DEBUG_LOCAL = false;
    
    //todo: add to VueResources.properties
    public static final String MERGE_SOURCES_TITLE = "Merged from:";
    
    /** Note that this was a STATIC list of listeners, which is bad, crazy, bad, bad, crazy.
     * Or at least as long as it was firing for every single deserialization... */
    // private static List<MetadataListListener> listeners = new ArrayList<MetadataListListener>();
    
    private final CategoryFirstList dataList = new CategoryFirstList();

    /** these are all decorators for metadataList */
    private final SubsetList resourceList = new SubsetList(VueMetadataElement.RESOURCE_CATEGORY);
    private final SubsetList categoryList = new SubsetList(VueMetadataElement.CATEGORY);
    private final SubsetList ontologyList = new SubsetList(VueMetadataElement.ONTO_TYPE);
    private final SubsetList otherList =    new SubsetList(VueMetadataElement.OTHER);
    
    public MetadataList() {
        // todo: this sub-list thing by type is needlessly complicated, and
        // not needed at all.

        // Can only find one reference to RESOURCE_CATEGORY -- in MapInspectorPanel where
        // VUE sets dcCreator meta-data if the LWMap author is edited.
        
        // The existence ONTO_TYPE could explain meta-data node-icons only SOMETIMES showing,
        // as LWIcon only checks for ONTO_TYPE and OTHER.  ONTO_TYPE is created in tons of
        // places in the code tho (via the existence of class OntType).  OTHER is created
        // in LWMergeMap and ConnectivityMatrixList.

        // In ~/Maps I found 2 instances of resourceListSize -- one map with dc:creator, and
        // another with dc:description, which is what saved LWMap notes are using.

        // There are also #TAG elements.  Sometimes the keys in VME are RDF keys.
        // Actually it's usually, I think.  Makes for needlessly big save files.
    }
   
    /** horrible API still called all over the place */
    public List<VueMetadataElement> getMetadata() {
        return dataList;
    }
    
    /** is this *effectively* empty */
    public boolean isEmpty() {
        return dataList.size() <= 0 || (dataList.size() == 1 && dataList.get(0).isEmpty());
    }
    
    /** is there any data of the given type in here? */
    public boolean hasMetadata(int type) {
        // return getMetadataAsHTML(type).length() > 0 // Christ!
        return sizeForType(type) > 0;
    }

    private boolean unmarshalling = false;
    /** @see tufts.vue.XMLUnmarshalListener */
    public void XML_initialized(Object context) { unmarshalling = true; }
    public void XML_completed(Object context) { unmarshalling = false; }
    public void XML_fieldAdded(Object context, String name, Object child) {}
    public void XML_addNotify(Object context, String name, Object parent) {}

    
    /** for castor persistance only */
    public List<VueMetadataElement> getXMLdata() {
        if (isEmpty())
            return null;
        else
            return dataList;
    }
    
    /** for castor persistance only */
    public List<VueMetadataElement> getSetterXMLdata() {
        if (unmarshalling) {
            // allow old "metadata" named element name to initialize the list
            return dataList;
        } else {
            // prevent persistance of old "metadata" named element name
            return null;
        }
    }

    public boolean intersects(MetadataList remote) {
        final List remoteList = remote.getMetadata();
        for (VueMetadataElement e : dataList)
            if (remoteList.contains(e))
                return true;
        return false;
    }

    public static void addListener(MetadataListListener listener) {
        // Util.printStackTrace(MetadataList.class + ":STATIC:addListener:IGNORED " + Util.tags(listener));
        Log.info("listeners have been disabled, will not report to: " + Util.tags(listener));
        // listeners.add(listener);
    }
    
    // Oops -- this disable flag was static, which means meta-data list events from other threads
    // could tromp all over the updates from other threads (not that we really need this update at
    // all, fortunately)
    // private static boolean disableEvents = false;
    private static void fireListChanged(Object tag)
    {
        // if (DEBUG.Enabled) Log.info("fireListChanged: " + Util.tags(tag));
        
        // if (disableEvents)
        //     return;
        // for (MetadataListListener mdl : listeners) {
        //     try {
        //         mdl.listChanged();
        //     } catch (Throwable t) {
        //         Log.warn("listener update: " + Util.tags(mdl), t);
        //     }
        // }
    }
    
    /** attempt at high-performace bulk load that triggers no GUI updates -- WARNING: ONLY ADDS TO "CATEGORY" LIST */
    public void add(Iterable<Map.Entry> kvEntries) {
        // oddly, firing list changed events get much slower as amount of total [map] meta-data increases [NOW WE KNOW WHY]

        // the categoryList updates still trigger the updates, so we turn them off with a flag for now
        
        // disableEvents = true;
        try {
            for (Map.Entry e : kvEntries) {
                try {
                    categoryList.add(new VueMetadataElement(e.getKey().toString(), e.getValue().toString()));
                } catch (Throwable t) {
                    Log.error("add entry " + Util.tags(e), t);
                }
            }
        } catch (Throwable tx) {
            Log.error("add iterable " + Util.tags(kvEntries), tx);
        }
        // finally {
        //     disableEvents = false;
        // }
        fireListChanged("bulk-add");
    }
    
    /** add a new CATEGORY type */
    public void add(String key, String value) {
        addElement(new VueMetadataElement(key, value));
    }
    
    public void addElement(VueMetadataElement element)     {
        if (true||DEBUG_LOCAL) Log.debug("addElement " + Util.tags(element));
        dataList.add(element);
        fireListChanged("addElement");
    }
    
    public void remove(String key, String value)
    {
    	int found = -1;

        for (int i = 0; i < getCategoryListSize(); i++) {
            final VueMetadataElement vme = getCategoryListElement(i);
            if (vme.getKey().equals(key) && vme.getValue().equals(value)) {
                found = i;
                break;
            }
        }
        if (found != -1)
            remove(found);
    }

    public int size() {
    	return dataList.size();
    }

    private int getResourceListSize() {
        // what??? it's called "CategoryFirstList", yet it stores resources 1st?
        return dataList.getResourceEndIndex();
    }
    public int getCategoryListSize() {
        return
            dataList.getCategoryEndIndex() -
            dataList.getResourceEndIndex();
    }
    public int getOntologyListSize() { // publicly called
        return
            dataList.getOntologyEndIndex() -
            dataList.getCategoryEndIndex();
    }
    public int getOtherListSize() {
        return
            dataList.getOtherEndIndex() - 
            dataList.getOntologyEndIndex();
    }
    
    public void remove(int i) {
    	dataList.remove(i);
    	fireListChanged("remove");
    }
    public VueMetadataElement get(int i) {
    	return dataList.get(i);
    }

    // does this mean to report that the given element has changed?
    public void modify(VueMetadataElement element) {
      if(element.getType() == VueMetadataElement.CATEGORY) {
          int i = findCategory(element.getKey());
          if(i!=-1) {    
              dataList.set(i,element);
              fireListChanged("modify-cat");
          }
      }
      if(element.getType() == VueMetadataElement.RESOURCE_CATEGORY) {
          int i = findRCategory(element.getKey());
          if(i!=-1) {    
              dataList.set(i,element);
              fireListChanged("modify-res");
          }
      }
    }
    
    public VueMetadataElement get(String key) {
        int rcindex = findRCategory(key);
        int cindex = findCategory(key);
        if(rcindex != -1)
            return getResourceListElement(rcindex);
        if(cindex != -1)
            return getCategoryListElement(cindex);
        else
            return null;
    }
    
    /*public int categoryIndexOfFirstWithValueAndKey(String key,String value) { }*/

     /**
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

    // See MapInspectorPanel
    /*public*/ public int findRCategory(String key)
    {
        int foundAt = -1;
        for(int i=0;i<getResourceListSize();i++)
        {
            VueMetadataElement vme = getResourceListElement(i);
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
    private int findMostRecentCategory(String key)
    {
        int foundAt = -1;
        int startPoint = ((CategoryFirstList)dataList).getResourceEndIndex();
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
    
    // See Util.java
    /*public*/ public boolean containsOntologicalType(String ontType)
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

    /*public*/ public VueMetadataElement getCategoryListElement(int i)
    {
        int index = i;
        try
        {        
          if(getCategoryListSize() > 0 && index < dataList.size())
            return dataList.get(index+((CategoryFirstList)dataList).getResourceEndIndex());
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
    
    /*hide*/ private void setCategoryListElement(int i,VueMetadataElement ele)
    {
        int index = i;
        try
        {        
          if(getCategoryListSize() > 0 && index < dataList.size())
            dataList.set(index+ ((CategoryFirstList)dataList).getResourceEndIndex(),ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    /*hide*/ private VueMetadataElement getResourceListElement(int i)
    {
        int index = i;
        try
        {        
          if(getResourceListSize() > 0 && index < dataList.size())
            return dataList.get(index);
          else
            return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return new VueMetadataElement();
        }
    }
    
    /*hide*/ private void setResourceListElement(int i,VueMetadataElement ele)
    {
        int index = i;
        try
        {        
          if(getResourceListSize() > 0 && index < dataList.size())
            dataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    // See OntologicalMembershipPane
    /*public*/ public boolean hasOntologicalMetadata() {
        return (getOntologyListSize() > 0);
    }
    // See OntologicalMembershipPane
    /*public*/ public VueMetadataElement getOntologyListElement(int i)
    {
        CategoryFirstList cf = ((CategoryFirstList)dataList);
        int index = i+cf.getCategoryEndIndex();
        try
        {        
          if(getOntologyListSize() > 0 && index < dataList.size())
            return dataList.get(index);
          else
            return new VueMetadataElement();
        }
        catch(Exception e)
        {
            return new VueMetadataElement();
        }
    }

    // See Util.java -- note: was always persisted.  Now is never persisted.
    /*public*/ public String getOntologyListString()
    {
        String returnString = "";
        
        for(int i=0;i<getOntologyListSize();i++)
        {
            VueMetadataElement vme = getOntologyListElement(i);
            returnString += vme.getObject() + "|";
        }
        
        return returnString;
    }
    /*hide*/ private void setOntologyListElement(int i,VueMetadataElement ele)
    {
        CategoryFirstList cf = ((CategoryFirstList)dataList);
        int index = i+cf.getCategoryEndIndex();
        try
        {        
          if(getOntologyListSize() > 0 && index < dataList.size())
            dataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    /*hide*/ private VueMetadataElement getOtherListElement(int i)
    {
        int index = i+((CategoryFirstList)dataList).getOntologyEndIndex();
        try
        {        
          if(getOtherListSize() > 0 && index < dataList.size())
            return dataList.get(index);
          else
              return new VueMetadataElement(); // NOTE VME CREATION!
        }
        catch(Exception e)
        {
            return new VueMetadataElement(); // NOTE VME CREATION!
        }
    }
    
    /*hide*/ private void setOtherListElement(int i,VueMetadataElement ele)
    {
        int index = i+((CategoryFirstList)dataList).getOntologyEndIndex();
        try
        {        
          if(getOtherListSize() > 0 && index < dataList.size())
            dataList.set(index,ele);
          else
            return;
        }
        catch(Exception e)
        {
            return;
        }
    }
    
    /** note: as is from old impl: only checks for CAT/REST/ONTO, defaults to OTHER */
    private int sizeForType(int type) {
        if (type == VueMetadataElement.CATEGORY)
            return getCategoryListSize();
        else if (type == VueMetadataElement.RESOURCE_CATEGORY)
            return getResourceListSize();
        else if (type == VueMetadataElement.ONTO_TYPE)
            return getOntologyListSize();
        else
            return getOtherListSize();
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
                
                if (DEBUG_LOCAL) Log.debug("HTML loop -- value for " + i + " type: " + type + " value: " + value);

                if (value == null) {
                    if (DEBUG.Enabled) Log.debug("null meta-data value for " + mdList.get(i));
                    //Util.printStackTrace();
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
                       
                     value = " &nbsp; " + value;
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
    
    public SubsetList getCategoryList() { return categoryList; }
    public SubsetList getOntologyList() { return ontologyList; }
    public SubsetList getOtherList() { return otherList; }
    
    /** a decorator for the main list that makes it look like it only contains the given type */
    public class SubsetList
    {
        private final int type;
        public SubsetList(int type) { this.type = type; }

        private int getSizeForNonResourceType(final int t) {
           // [DAN] "not yet needed -- also add in contains.."
           //if(type == VueMetadataElement.RESOURCE_CATEGORY)
           //    size = getResourceListSize();

            if (t == VueMetadataElement.RESOURCE_CATEGORY) {
                Log.warn("impl never searched RESOURCE_CATEGORY");
                return 0;
            }
            if (t == VueMetadataElement.CATEGORY)
                return getCategoryListSize();
            else if (t == VueMetadataElement.ONTO_TYPE)
                return getOntologyListSize();
            else
                return getOtherListSize();
        }
        
        public int indexOf(VueMetadataElement vme) {
            final int size = getSizeForNonResourceType(type);
            for (int i=0; i<size; i++) {
                if (type == VueMetadataElement.CATEGORY && getCategoryListElement(i).equals(vme))
                    return i;
                else if (type == VueMetadataElement.ONTO_TYPE && getOntologyListElement(i).equals(vme))
                    return i;
                else if (getOtherListElement(i).equals(vme))
                    return i;
           }
           // Looks like the blocks were screwed up -- could only ever find CATEGORY
           // -- afraid to actually fix this...
           //  for(int i=0;i<size;i++) {
           //     if(type == VueMetadataElement.CATEGORY)
           //        if(getCategoryListElement(i).equals(vme))
           //            return i;
           //     else if(type == VueMetadataElement.ONTO_TYPE)
           //        if(getOntologyListElement(i).equals(vme))
           //            return i;
           //     else
           //        if(getOtherListElement(i).equals(vme))
           //            return i;
           // }
           //note: currently resource categories are not found.
           return -1;
        }
        
        public boolean contains(VueMetadataElement vme) {
            final int size = getSizeForNonResourceType(type);
            
            for(int i=0; i<size; i++) {
                if (type == VueMetadataElement.CATEGORY && getCategoryListElement(i).equals(vme))
                    return true;
                else if (type == VueMetadataElement.ONTO_TYPE && getOntologyListElement(i).equals(vme))
                    return true;
                else if (getOtherListElement(i).equals(vme))
                    return true;
            }
           // More screwed up blocking... again, afraid to fix this...
           // for(int i=0;i<size;i++) {
           //     if(type == VueMetadataElement.CATEGORY)
           //        if(getCategoryListElement(i).equals(vme))
           //            return true;
           //     else if(type == VueMetadataElement.ONTO_TYPE)
           //        if(getOntologyListElement(i).equals(vme))
           //            return true;
           //     else
           //        if(getOtherListElement(i).equals(vme))
           //            return true;
           // }
           // note: as in indexOf() currently resouerce categories are not found.
            return false;
        }
        
        public void add(VueMetadataElement vme) {
            ((CategoryFirstList)getMetadata()).add(vme);
        }
        
        public int size() { return sizeForType(type); }
        
        public VueMetadataElement get(int i)
        {
           if(type == VueMetadataElement.RESOURCE_CATEGORY)
               return getResourceListElement(i);
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
               setResourceListElement(i,ele);
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
    
    /* Only place this is still publicly referenced is OntologicalMembershipPane.java */
    public static class CategoryFirstList extends java.util.ArrayList<VueMetadataElement>
    {
        //private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CategoryFirstList.class);
        
        private int categoryEndIndex = 0;
        private int ontologyEndIndex = 0;
        private int otherEndIndex = 0;
        private int rCategoryEndIndex = 0; // RESOURCE_CATEGORY
      
        public int getCategoryEndIndex() { return categoryEndIndex; }
        private int getResourceEndIndex() { return rCategoryEndIndex; }
        private int getOntologyEndIndex() { return ontologyEndIndex; }
        private int getOtherEndIndex() { return otherEndIndex; }
      
        /** called via BOTH castor restore as well as runtime API */
        public boolean add(final VueMetadataElement vme)
        {
            if (DEBUG.CASTOR||DEBUG.XML||DEBUG.DATA) Log.debug(Util.tag(this) + ":add: " + Util.tags(vme));
          
            if (vme.getObject() == null) {
                if (DEBUG_LOCAL) Log.debug("VME object is null, (re)setting type..." + vme.getType());
                vme.setType(vme.getType()); // ick ick ick
            }
          
            if(DEBUG_LOCAL) 
                Log.debug("categoryFirstList add - catEnd,ontEnd,size(): "
                          + categoryEndIndex + "," + ontologyEndIndex + "," + size());
          
            if (vme.getType() == VueMetadataElement.ONTO_TYPE && vme.getObject() instanceof OntType) {
                otherEndIndex++;
                super.add(ontologyEndIndex++, vme);
            }
            else if (vme.getType() == VueMetadataElement.RESOURCE_CATEGORY && vme.getObject() instanceof String[]) {
                ontologyEndIndex++;
                categoryEndIndex++;
                otherEndIndex++;
                super.add(rCategoryEndIndex++, vme);
                if(DEBUG_LOCAL) Log.debug("rCategoryIndex is now: " + rCategoryEndIndex);
            }
            else if (vme.getType() == VueMetadataElement.CATEGORY && vme.getObject() instanceof String[]) {
                ontologyEndIndex++;
                otherEndIndex++;
                super.add(categoryEndIndex++, vme);
            }
            else {
                // WARNING: checks for getObject mean possible for a NON-OTHER type to wind up in other sub-list
                super.add(otherEndIndex++, vme);
            }

            // MAJOR ISSUE: THIS WAS FIRING DURING DESERIALIZATION, TO A GLOBALLY *STATIC* LIST OF
            // LISTENERS, WHICH MEANS CALLS TO AWT CODE FOR EVERY SINGLE VME IN ANY NODE IN ANY
            // MAP. Including one of the worse AWT calls: revalidate().  This could also sometimes
            // cause AWT hangs, often in somewhere down in cursor code(?)  We've now reduced all
            // MDL update events to diagnostics only -- turns out there's no place in VUE where
            // there are AWT components watching meta-data that may change outside the UI changing
            // them.
            fireListChanged("CFL-add");
            return true;
        }
      
        public VueMetadataElement remove(int i)
        {
            // Attempt the remove 1st so that if we get a RangeCheck exception,
            // we haven't adjusted our indicies.
            final VueMetadataElement removed = super.remove(i);
            
            if(i < rCategoryEndIndex) {
                rCategoryEndIndex --;
                if(categoryEndIndex > 0)
                    ontologyEndIndex--;
                if(ontologyEndIndex > 0)
                    ontologyEndIndex--;
                if(otherEndIndex > 0)
                    otherEndIndex--;
            }
            if(i < categoryEndIndex && i >= rCategoryEndIndex) {
                categoryEndIndex--;
                if(ontologyEndIndex > 0)
                    ontologyEndIndex--;
                if(otherEndIndex > 0)
                    otherEndIndex--;
            }
            else if(i >= categoryEndIndex && i < ontologyEndIndex) {
                ontologyEndIndex--;
                otherEndIndex--;
            }
            else
                otherEndIndex--;
          
            fireListChanged("CFL-remove");

            return removed;
          
            // BUG: if this index was out of range, we've already adjusted our indicies!
            // return super.remove(i);
        }
    }
    
    public interface MetadataListListener
    {
       public void listChanged();
    }
}
