
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
import static edu.tufts.vue.metadata.VueMetadataElement.*;
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

        // Can only find one code reference to RESOURCE_CATEGORY -- in MapInspectorPanel where
        // VUE sets dcCreator meta-data if the LWMap author is edited.
        // In ~/Maps I found 2 instances of type RESOURCE_CATEGORY -- one map with dc:creator, and
        // another with dc:description, which is what saved LWMap notes are using.

        // Both #none and #TAG ARE handled in the ui, but ONLY when they're type is 1 (CATEGORY) or
        // 2 (ONTO_TYPE) -- the UI only pulls by TYPE, not key.  Of course, the combo-box no longer
        // supports TAG, and the tag in this instance is "#Tag" not "#TAG" -- so the UI pulls up
        // whatever is after the # if it's of a VME this.type it handles.
    }
   
    /** horrible API still called all over the place */
    public List<VueMetadataElement> getMetadata() { return dataList; }
    public List<VueMetadataElement> getAllTypes() { return dataList; }
    
    /** is this *effectively* empty */
    public boolean isEmpty() {
        return dataList.size() <= 0 || (dataList.size() == 1 && dataList.get(0).isEmpty());
    }
    
    /** is there any data of the given type in here? this is a special call for the LWIcon UI */
    public boolean hasMetadata(int type) {
        return sizeForTypeHistorical(type) > 0;
        // if (true) return getOldMetadataHTMLAsPresenceTest(type).length() > 0;
        // final int count = countType(type);
        // final int size = sizeForType(type);
        // if (count != size) Log.warn("count != size for type " + type + ": " +count + " != " + size); // they don't match!!!
        // return size > 0 && !isEmpty();
    }

    private int countType(int type) {
        int count = 0;
        for (VueMetadataElement vme : dataList)
            if (vme.getType() == type)
                count++;
        return count;
    }
    
    /** note: as is from old impl: only checks for CAT/REST/ONTO, defaults to OTHER */
    private int sizeForType(int type) {
        return sizeForTypeHistorical(type);

    }
    
    private int sizeForTypeHistorical(int type) {
        switch (type) {
        case CATEGORY:          return getCategoryListSize();
        case ONTO_TYPE:         return getOntologyListSize();
        case RESOURCE_CATEGORY: return getResourceListSize();
        case OTHER:             
        case SEARCH_STATEMENT:  
        case TAG:
        default:
            return getOtherListSize();
        }
    }

    private int sz(int type) { return sizeForTypeActual(type); }
    
    private int sizeForTypeActual(int type) {
        switch (type) {
        case TAG:               return -1;
        case CATEGORY:          return getCategoryListSize();
        case ONTO_TYPE:         return getOntologyListSize();
        case SEARCH_STATEMENT:  return -1;
        case OTHER:             return getOtherListSize();
        case RESOURCE_CATEGORY: return getResourceListSize();
        }
        return Integer.MIN_VALUE;
    }
    

    private boolean unmarshalling = false;
    /** @see tufts.vue.XMLUnmarshalListener */
    public void XML_initialized(Object context) { unmarshalling = true; }
    public void XML_completed(Object context) { unmarshalling = false; }
    public void XML_fieldAdded(Object context, String name, Object child) {}
    public void XML_addNotify(Object context, String name, Object parent) {}

    
    /** for castor persistance only */
    public List<VueMetadataElement> getXMLdata() {
        // TODO: FIX
        // if (isEmpty())
        //     return null;
        // else
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
    public int getCategoryListSize() { // publicly called InspectorPane
        return
            dataList.getCategoryEndIndex() -
            dataList.getResourceEndIndex();
    }
    public int getOntologyListSize() { // publicly called
        return
            dataList.getOntologyEndIndex() -
            dataList.getCategoryEndIndex();
    }
    /*hide*/ private int getOtherListSize() {
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
    public void replaceWith(VueMetadataElement newItem) {
      if(newItem.getType() == VueMetadataElement.CATEGORY) {
          int i = findCategory(newItem.getKey());
          if(i!=-1) {    
              dataList.set(i,newItem);
              fireListChanged("modify-cat");
          }
      }
      if(newItem.getType() == VueMetadataElement.RESOURCE_CATEGORY) {
          int i = findRCategory(newItem.getKey());
          if(i!=-1) {    
              dataList.set(i,newItem);
              fireListChanged("modify-res");
          }
      }
    }
    
    /*public int categoryIndexOfFirstWithValueAndKey(String key,String value) { }*/

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
     /** finds the first entered (last in order) category element with the supplied key
     * @return -1 if not found. **/
    /*hide*/ private int findCategory(String key) {
        VueMetadataElement vme = null;
        int i = -1;
        try {
            for (i=0;i<getCategoryListSize();i++) {
                vme = getCategoryListElement(i);
                if (key.equals(vme.key))
                    return i;
            }
        } catch (Throwable t) {
            Log.error("searching for " + Util.tags(key) + " at index " + i + " with " + vme, t);
        }
        return -1;
    }
    // See MapInspectorPanel
    /*public*/ public int findRCategory(String key)
    {
        for(int i=0;i<getResourceListSize();i++) {
            VueMetadataElement vme = getResourceListElement(i);
            if(vme.getKey().equals(key))
                return i;
        }
        return -1;
    }
    

    public boolean contains(VueMetadataElement vme) {
        return contains(vme.getKey(), vme.getValue());
    }
    
    public boolean contains(String key, String value) 
    {
        for(int i=0;i<getCategoryListSize();i++) {     
            VueMetadataElement vme = getCategoryListElement(i);
            if(vme.getKey().equals(key) && vme.getValue().equals(value))
                return true;
        }
        return false;
    }

    // /**
    //  * finds the most recently entered (last in order)
    //  * category element with the supplied key
    //  * @return -1 if not found. **/
    // private int findMostRecentCategory(String key)
    // {
    //     int foundAt = -1;
    //     int startPoint = ((CategoryFirstList)dataList).getResourceEndIndex();
    //     for(int i=startPoint;i<getCategoryListSize();i++) {
    //         VueMetadataElement vme = getCategoryListElement(i);
    //         if(vme.getKey().equals(key))
    //             foundAt = i;
    //     }
    //     return foundAt;
    // }
    
    // See Util.java old code
    /*hide*/ private boolean containsOntologicalType(String ontType)
    {
        for(int i=0;i<getOntologyListSize();i++) {
            VueMetadataElement vme = getOntologyListElement(i);
            OntType type = (OntType)vme.getObject();
            if(DEBUG_LOCAL) Log.debug("containsOntologicalType - vme.getValue() " + vme.getValue() + " --- ontType from properties " + ontType);
            if(ontType.equals(vme.getValue()));
                return true;
        }
        return false;
    }

    /*public*/ public VueMetadataElement getCategoryListElement(int index)
    {
        try {
            if(getCategoryListSize() > 0 && index < dataList.size())
                return dataList.get(index+((CategoryFirstList)dataList).getResourceEndIndex());
          //else
            //return new VueMetadataElement(); // SMF?
        }
        catch(Exception e) {
            Log.warn(e);
            return null;
            //return new VueMetadataElement(); 
        }
        return null;
    }
    /*hide*/ private VueMetadataElement getResourceListElement(int index) {
        try {        
            if (getResourceListSize() > 0 && index < dataList.size())
                return dataList.get(index);
            else
                return new VueMetadataElement();
        }
        catch(Exception e) {
            Log.warn(e);
            // WHAT???  This looks like a bunch of crap so that the MetadataEditor
            // could be stupid & lazy.
            return new VueMetadataElement();
        }
    }
    /*hide*/ private void setCategoryListElement(int index,VueMetadataElement vme) {
        try {        
            if (getCategoryListSize() > 0 && index < dataList.size())
                dataList.set(index+ ((CategoryFirstList)dataList).getResourceEndIndex(),vme);
            else
                return;
        }
        catch(Exception e) {
            Log.warn(e);
            return;
        }
    }
    /*hide*/ private void setResourceListElement(int index, VueMetadataElement ele)
    {
        try {        
            if(getResourceListSize() > 0 && index < dataList.size())
                dataList.set(index,ele);
            else
                return;
        }
        catch(Exception e) {
            Log.warn(e);
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

    // See Util.getMergeProperty -- note: was always persisted.  Now is never persisted.
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
    
    public String getMetadataAsHTML(int type) {
        if (DEBUG.DATA || DEBUG.DR) // DR so can turn on with '^' key
            return buildDebugHTML(type).toString();
        else
            return buildHTMLForType(type);
    }

    private StringBuilder buildDebugHTML(int type) {
        final StringBuilder b = new StringBuilder();

        // Old (still, actually) source-map meta-data that is put into nodes on merged-maps was
        // supposed to be of type 4/OTHER, however apparently that never worked, and it was put in
        // and saved out as type 0/TAG (E.g. ForcesAlicePulman.vue).  However, a bug or intentional
        // very confusing hack had 0/TAG meta-data pulled for HTML even when meta-data of type
        // 4/OTHER was requested.  What a mess.  Actually, the real problem may be that
        // the TAG data actually exists in the "getOtherList()" decorator.
        
        final String typeName;
        if (type >= VueMetadataElement._Types.length)
            typeName = "<unknown:" + type + ">";
        else
            typeName = VueMetadataElement._Types[type];
      //b.append("<font face=Menlo size=-2><b>"); // fixed witdth font
        b.append("<font face=Menlo size=-0><b>"); // fixed witdth font
        b.append("Type requested: ").append(type).append(" / ").append(typeName);
        b.append("<br>");
        b.append("real-sizes: ");
        for (int i = 0; i <= 5; i++) {
            final int sz = sizeForTypeActual(i);
            if (sz >= 0)
                b.append(VueMetadataElement._Types[i].toLowerCase()) .append('=') .append(sz) .append(" ");
        }
        b.append("<br>");
        b.append("fake-sizes: ");
        for (int i = 0; i <= 5; i++) {
            final int sz = sizeForTypeHistorical(i);
            if (sz >= 0)
                b.append(VueMetadataElement._Types[i].toLowerCase()) .append('=') .append(sz) .append(" ");
        }
        int count = 0;
        for (VueMetadataElement md: dataList) {
            count++;
            b.append("<br>#");
            if (count < 10) b.append(' ');
            b.append(count).append("  ");
            b.append(md.getType()).append(':');
            //b.append(String.format("%14s", md.getKey())); // not in HTML
            final String key = md.getKey();
            final String val = md.getValue();
            
            if (dataList.size() > 1)
                for (int i = key.length(); i < 22; i++) b.append("&nbsp;");
            
            b.append(key).append(": ");
            if (val == null) {
                b.append("<font color=blue>");
                b.append("null");
                final Object o = md.getObject();
                if (o != null) {
                    boolean dumpObj = true;
                    if (o instanceof String[]) {
                        String s[] = (String[]) o;
                        if (s.length == 2 && s[0] == key && s[1] == val)
                            dumpObj = false;
                    }
                    if (dumpObj)
                        b.append(" ") .append(Util.tags(o)); // [nice to pass in a "StringPainter" to tags]
                }
            } else {
                b.append("<font color=red>");
                b.append('"');
                if (val.length() > 64)
                    b.append(val.substring(0,64)) .append("\"...x") .append(val.length());
                else
                    b.append(val) .append('"');
            }
            b.append("</font>");
        }
        return b;
    }

    private static final char SPACE = ' ', SLASH = '/', NEWLINE = '\n';

    private String buildHTMLForType(final int typeRequest)
    {
        // Util.printStackTrace("buildHTMLForType " + typeRequest);
        
        final StringBuilder b = new StringBuilder(32);
        int startSize = 0;
        
        if (typeRequest == OTHER) {
            b.append("&nbsp;<b><font color=gray>Merge sources:</font></b>");
            startSize = b.length();
            buildHTMLForMergeSources(b);
        } else {
            buildHTMLFilteredAndOrganized(b, typeRequest);
            // attempting to force a tiny last-line height to push rollover bottom edge down by a few pixels:
            // b.append("<font size=-14 color=gray>---");  // can't get small enough: font size has floor on it
        }
        Log.debug("HTMLforType " + typeRequest + " [" + b + "]");
        
        if (DEBUG.Enabled && b.length() == startSize)
            b.append(buildDebugHTML(typeRequest));
        
        return b.toString();
    }

    /* don't include empty values, and put keyless data at the end */
    private void buildHTMLFilteredAndOrganized(final StringBuilder b, final int typeRequest)
    {
        final List<VueMetadataElement> haveKey = new ArrayList(dataList.size());
        final List<VueMetadataElement> haveOnlyValue = new ArrayList(dataList.size());
            
        for (VueMetadataElement md : dataList) {
            // "<missing>" is a special value produced by the Schema code.  It can't be displayed
            // for the same reason that <anythingInAngleBrackets> wont show up in the HTML, and for
            // rollovers it's also a good indicator of a non-interesting value.
            if (md.type != typeRequest || !md.hasValue() || "<missing>".equals(md.value))
                continue;
            if (md.key == null || md.key == ONTOLOGY_NONE || md.key == KEY_TAG)
                haveOnlyValue.add(md);
            else
                haveKey.add(md);
        }
        if (haveKey.size() == 1 && haveOnlyValue.size() == 0) {
            // If single key & value, display it specially.  We may want to reserve this for
            // the @ValueOf cases (enumerated values from a Schema), but we just do it for
            // everything now.
            b.append(" &nbsp; <b><font color=gray>"); 
            b.append(shortKey(haveKey.get(0).key));
            b.append("</font></b>: &nbsp; <br> &nbsp; <font size=+0>"); // odd -- this still makes it bigger
            appendBold(b, haveKey.get(0).value);
            b.append("</font> &nbsp; "); // to align, nbsp's must happen at same font size
            return;
        }
        
        if (haveKey.size() > 0) {
            b.append("<b>");
            for (VueMetadataElement md : haveKey) {
                if (DEBUG.Enabled) b.append(NEWLINE); // newline for debugging -- watch whitespace impact
                b.append("&nbsp;<font color=gray>");
                b.append(truncate(shortKey(md.key)));
                b.append(":</font> ");
                b.append(truncate(md.value));
                b.append("&nbsp;<br>");
            }
            b.append("</b>");
        }
        
        if (haveOnlyValue.size() > 0) {
            for (VueMetadataElement md : haveOnlyValue) {
                if (DEBUG.Enabled) b.append(NEWLINE); // newline for debugging -- watch whitespace impact
                b.append("&nbsp;&thinsp;&bull; ");
                appendBold(b, truncate(md.value));
                b.append(" &nbsp; <br>");
            }
        }

        if (haveKey.size() + haveOnlyValue.size() > 0)
            return;

        // Below is old complex code that should just be dumped, but it works okay as a failsafe to
        // handle case of only having key(s) that have no value;
        
        final boolean isSingle = (dataList.size() == 1);
        for (VueMetadataElement md : dataList) {
            if (md.type != typeRequest)
                continue;
            if (DEBUG.Enabled) b.append(NEWLINE); // newline only for debugging the output string
            final boolean hasKey = !(md.key == null || md.key == ONTOLOGY_NONE || md.key == KEY_TAG);

            b.append("&nbsp;");
            if (isSingle)
                b.append("<font size=+0>"); // odd -- this still makes it bigger
            else if (hasKey)
                b.append("<font color=gray>");

            if (hasKey)
                b.append(shortKey(md.key)).append(':');
            else
                b.append("&bull;");
            if (hasKey && !isSingle) b.append("</font>");
            b.append(SPACE);
            if (md.value != null) {
                if (!hasKey || isSingle)
                    appendBold(b, truncate(md.value));
                else
                    b.append(truncate(md.value));
            }
            b.append("&nbsp;<br>");
        }
    }

    private void buildHTMLForMergeSources(final StringBuilder b)
    {
        for (VueMetadataElement md : dataList) {
            if (md.type == OTHER || md.type == TAG) {
                if (md.value == null)
                    continue;
                b.append("<br>&nbsp;");
                final int startLine = b.length();
                final String[] part;
                if (md.value != null && md.value.startsWith("source:")) {
                    // handle old style pre-summer-2012 merge-map meta-data annotations
                    part = md.value.substring(8).split(","); // format: map-file-name,label
                    b.append("&thinsp;&bull; "); 
                    if (part.length > 1) {
                        b.append(part[0]).append(SLASH);
                        appendItalic(b, truncate(part[1].trim()));
                    }
                } else {
                    part = md.value.split("/"); // format: ID/typeChar/map-file-name/label
                    if (part.length > 3) {
                        if (part[1].charAt(0) == 'L') // link (also: N=node, I=image, etc)
                            b.append("&harr; "); // horizontal double-ended arrow                            
                        else
                            b.append("&thinsp;&bull; "); // bullet: align under 'M' of "Merge sources"
                        b.append(part[2]).append(SLASH); // map name
                        appendItalic(b, truncate(part[3])); // node/component name
                    }
                }
                if (b.length() == startLine)
                    b.append(md.value); // dump all if didn't parse
                b.append("&nbsp;");
            }
        }
    }

    private static StringBuilder appendItalic(StringBuilder b, String s) {
        return b.append("<i>").append(s).append("</i>");
    }
    private static StringBuilder appendBold(StringBuilder b, String s) {
        return b.append("<b>").append(s).append("</b>");
    }

    private static String shortKey(String k) {
        final int localPart = k.indexOf('#');
        if (localPart > 1 && localPart < (k.length() - 1))
            return k.substring(localPart + 1);
        else
            return k;
    }
    private static String truncate(String s) { return truncate(s, 50); }
    private static String truncate(String s, final int max) {
        // Re: truncation: Notes rollover wraps text via JTextArea, which won't give us HTML.  If
        // we want text wrapping, we may be able to do it using a table.
        if (s.length() > max)
            return s.substring(0,max) + "&hellip;";
        else
            return s;
    }


    private StringBuilder getOldMetadataHTML(int type)
    {
        final StringBuilder b = new StringBuilder();
        final SubsetList mdList;
        
        if(type == VueMetadataElement.CATEGORY)
            mdList = getCategoryList();
        else if(type == VueMetadataElement.ONTO_TYPE)
            mdList = getOntologyList();
        else
            mdList = getOtherList();

        final int size = mdList.size();
        
        if (size <= 0)
            return b;
        
        for (int i = 0; i < size; i++) {
            final String value = mdList.get(i).getValue();
                
            if (DEBUG_LOCAL) Log.debug("HTML loop -- value for " + i + " type: " + type + " value: " + value);

            if (value == null) {
                if (DEBUG.MEGA) Log.debug("GET-AS-HTML: null meta-data value for " + mdList.get(i));
                //Util.printStackTrace();
                continue;
            }
                
            if (value.length() > 0) {    
                b.append("<br>");
                if (type == VueMetadataElement.ONTO_TYPE) {
                    final int nameLocation = value.indexOf(VueMetadataElement.ONT_SEPARATOR);
                    if(nameLocation > -1 && value.length() > nameLocation + 1) {
                        b.append(value, nameLocation + 1, value.length()); // todo check: off by 1 anywhere?
                        //value = value.substring(nameLocation + 1);
                    } else
                        b.append(value);
                }
                else if(type == VueMetadataElement.OTHER) {
                    b.append(" &nbsp; ");
                    // This looks like it's expecting a specific format from somewhere...
                    final int colon = value.indexOf(":");
                    String cursor = value;
                  // Based on indention, doesn't look like that semicolon was on purpose -- so this never worked right.
                  // Tho again, this is another bug I'm afraid to fix as something else in this spaghetti fest might be relying on it...
                  //if (cLocation > -1 && value.length() > cLocation + 1);
                  //    value = value.substring(cLocation + 1);  
                    if (colon > -1 && value.length() > colon + 1)
                        cursor = value.substring(colon + 1);
                    final int dot = cursor.lastIndexOf(".");
                    if (dot != -1) {
                        final int comma = cursor.indexOf(",");
                        String endPart = "";
                        if (comma != -1 && comma > dot) {
                            endPart = cursor.substring(comma); // todo: factor down to append
                        } 
                        cursor = cursor.substring(0,dot) + endPart;
                    }
                    b.append(cursor);
                } else
                    b.append(value);
            }
        }
            
        if(b.length() > 0) {
            if(type == CATEGORY)
                b.insert(0, "Keywords: ");
            else if(type == ONTO_TYPE)
                b.insert(0, "Ontological Membership: ");
            else if(type == OTHER)
                b.insert(0, "Merged from: ");
                // So OTHER meta-data is, in fact, hardcoded for the purpose of merge-sources.
        }
        return b;
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
           if (type == VueMetadataElement.RESOURCE_CATEGORY)
               return getResourceListElement(i);
           else if(type == VueMetadataElement.CATEGORY)
               return getCategoryListElement(i);
           else if(type == VueMetadataElement.ONTO_TYPE)
               return getOntologyListElement(i);
           else
               return getOtherListElement(i);            
        }
        
        // Used in MetadataEdtior.java
        public void set(int i,VueMetadataElement ele)
        {
           if (type == VueMetadataElement.RESOURCE_CATEGORY)
               setResourceListElement(i,ele);
           else if(type == VueMetadataElement.CATEGORY)
               setCategoryListElement(i,ele);
           else if(type == VueMetadataElement.ONTO_TYPE)
               setOntologyListElement(i,ele);
           else
               setOtherListElement(i,ele);            
        }
        
        public java.util.List getAsList()
        {
            final java.util.List returnList = new java.util.ArrayList();
            final int size = size();
            for(int i=0; i < size; i++)
                returnList.add(get(i));
            return returnList;
        }
    }
    
    /* Only place this is still publicly referenced is OntologicalMembershipPane.java */
    public class CategoryFirstList extends java.util.ArrayList<VueMetadataElement>
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
        @Override public boolean add(final VueMetadataElement vme)
        {
            if (DEBUG.Enabled) {
                if (DEBUG.CASTOR||DEBUG.XML||DEBUG.DATA||DEBUG.PAIN) Log.debug(Util.tag(this) + ":add: " + Util.tags(vme));
            }

            if (vme.type == VME_EMPTY_IGNORE) { 
                if (!unmarshalling) Log.info("ignoring " + vme);
                return true;
            }
          
            if (vme.getObject() == null) {
                if (DEBUG_LOCAL) Log.debug("VME object is null, (re)setting type..." + vme.getType());
                vme.setType(vme.getType()); // ick ick ick
            }
          
            if(DEBUG_LOCAL) 
                Log.debug("categoryFirstList add - catEnd,ontEnd,size(): "
                          + categoryEndIndex + "," + ontologyEndIndex + "," + size());
          
            if (vme.type == VueMetadataElement.ONTO_TYPE && vme.getObject() instanceof OntType) {
                otherEndIndex++;
                super.add(ontologyEndIndex++, vme);
            }
            else if (vme.type == VueMetadataElement.RESOURCE_CATEGORY && vme.getObject() instanceof String[]) {
                ontologyEndIndex++;
                categoryEndIndex++;
                otherEndIndex++;
                super.add(rCategoryEndIndex++, vme);
                if(DEBUG_LOCAL) Log.debug("rCategoryIndex is now: " + rCategoryEndIndex);
            }
            else if (vme.type == VueMetadataElement.CATEGORY && vme.getObject() instanceof String[]) {
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
      
        @Override public VueMetadataElement remove(int i)
        {
            // Attempt the remove 1st so that if we get a RangeCheck exception,
            // we haven't adjusted our indicies.
            final VueMetadataElement removed = super.remove(i);
            
            if (DEBUG.PAIN) Log.debug(Util.tag(this) + ": remove index " + i + ": " + removed);
            
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
    
    public static void addListener(MetadataListListener listener) {
        // Util.printStackTrace(MetadataList.class + ":STATIC:addListener:IGNORED " + Util.tags(listener));
        if (DEBUG.Enabled) Log.debug("listeners no longer allowed, will not report to: " + Util.tags(listener));
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
    
    public interface MetadataListListener { public void listChanged(); }
}
