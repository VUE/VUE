/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package tufts.vue.ds;

import tufts.Util;
import tufts.vue.MetaMap;

import java.util.*;
import java.io.*;
import java.net.URL;

import tufts.vue.Resource;
import tufts.vue.LWComponent;

import org.xml.sax.InputSource;

import com.google.common.collect.*;


/**
 * @version $Revision: 1.15 $ / $Date: 2008-12-17 23:13:36 $ / $Author: sfraize $
 * @author Scott Fraize
 */


// todo: create a DataSet object, which is a combination of a Schema,
// the DataSet source (use a Resource?), and the holder of the actual
// row data.

public class Schema {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Schema.class);

    static final boolean DEBUG = true;

    protected final Map<String,Field> mFields = new LinkedHashMap(); // "columns"
    private Field mKeyField;

    private final List<DataRow> mRows = new ArrayList();

    private Object mSource;
    private Resource mResource;
    
    protected int mLongestFieldName = 10;

    private String mName;

    private LWComponent mStyleNode;

    private final String UUID;

//     /** construct an empty schema */
//     public Schema() {}
    
    public Schema(Object source) {
        // would be very handy if source was a Resource and Resources had IO methods
        setSource(source);
        UUID = edu.tufts.vue.util.GUID.generate();
    }

    public void annotateFor(Collection<LWComponent> nodes) {

        for (Field field : mFields.values())
            field.annotateIncludedValues(nodes);

        // is probably a faster way to track this by handling the key field specially
        // during annotateIncludedValues above and process the rows v.s. the values,
        // or just refactoring the whole process to go DataRow by DataRow.

        final Field keyField = getKeyField();
        final String keyFieldName = keyField.getName();

//         // crap: this is where we should be comparing the actual data sourced schema.
//         for (LWComponent node : nodes) {
//             if (node.hasDataKey(keyFieldName) && !node.isSchematicField()) {
//                 final DataRow row = findRow(keyField, node.getDataValue(keyFieldName));
//                 if (row == null) {
//                     Log.warn("no raw data found for node " + node);
//                     continue;
//                 }
//                 final MetaMap rawData = row.getData();
//                 final MetaMap mapData = node.getRawData();

                
//             }
//         }
        
        for (DataRow row : mRows) {

            final String rowKey = row.getValue(keyField);

            for (LWComponent node : nodes) {

                if (!node.hasDataValue(keyFieldName, rowKey))
                    continue;

                final MetaMap rawData = row.getData();
                final MetaMap mapData = node.getRawData();
                //Log.debug("comparing:\n" + rawData.values() + " to:\n" + mapData.values());
                row.setContextChanged(!rawData.equals(mapData));
                

                // test if this node is a row node from this schema -- currently an imperfect test: only
                // checks for presence of the same key field.
                //if (node.hasDataKey(keyFieldName) && !node.isSchematicField()) {

//                 if (node.hasDataValue(keyFieldName) && !node.isSchematicField()) {
//                     final MetaMap rawData = row.getData();
//                     final MetaMap mapData = node.getRawData();
//                     Log.debug("comparing:\n" + rawData.values() + " to:\n" + mapData.values());
//                     row.setContextChanged(!rawData.equals(mapData));
//                 } else {
//                     // could set to a "present" status -- context changed state is now technically undefined...
//                     row.setContextChanged(false);
//                 }
            }
        }
        
    }

    public void flushData() {
        Log.debug("flushing " + this);
        mRows.clear();
        mLongestFieldName = 10;
        for (Field f : mFields.values()) {
            f.flushStats(); // flush data / enums, but keep any style
        }
    }

    public DataRow findRow(Field field, String value) {
        for (DataRow row : mRows)
            if (row.contains(field, value))
                return row;
        return null;
    }

    @Override
    public String toString() {
        //return getName() + "; " + getSource() + "; " + UUID;
        //return getName() + "; " + getResource() + "; " + UUID;
        return getName();
    }

    public void setStyleNode(LWComponent style) {
        mStyleNode = style;
    }
        
    public LWComponent getStyleNode() {
        return mStyleNode;
    }
    
    public Field getKeyField() {
        if (mKeyField == null)
            mKeyField = getKeyFieldGuess();
        return mKeyField;
    }
    
    public void setKeyField(Field f) {
        //Log.debug("setKeyField " + Util.tags(f));
        mKeyField = f;
    }
    
    public void setKeyField(String name) {
        setKeyField(getField(name));
    }
        
    public Object getSource() {
        return mSource;
    }
    
    public void setSource(Object src) {
        mSource = src;

        try {
            setResource(src);
        } catch (Throwable t) {
            Log.warn(t);
        }
    }

    private void setResource(Object r) {
    
        if (r instanceof Resource)
            mResource = (Resource) r;
        else if (r instanceof InputSource)
            mResource = Resource.instance(((InputSource)r).getSystemId());
        else if (r instanceof File)
            mResource = Resource.instance((File)r);
        else if (r instanceof URL)
            mResource = Resource.instance((URL)r);
        else if (r instanceof String)
            mResource = Resource.instance((String)r);
    }

    public void setResource(Resource r) {
        mResource = r;
    }
    public Resource getResource() {
        return mResource;
    }
    
    public int getRowCount() {
        return mRows.size();
    }

    public void setName(String name) {
        mName = name;
    }

    public Field getField(String name) {
        //return mFields.get(name);
        Field f = mFields.get(name);
        if (f == null) Log.debug(String.format("%s; no field named '%s' in %s", this, name, mFields.keySet()));
        return f;
    }

    public boolean hasField(String name) {
        return mFields.containsKey(name);
    }
    
    
    public String getName() {

        if (mName != null)
            return mName;
        else
            return getSource().toString();
        
//         String s = getSource().toString();
//         int i = s.lastIndexOf('/');
//         if (i < s.length() - 2)
//             return s.substring(i+1);
//         else
//             return s;
//         Object o = getSource();
//         if (o instanceof File)
//             return ((File)o).getName();
//         else if (o instanceof URL)
//             return ((URL)o).getFile();
//         else
//             return o.toString();
    }

    public void ensureFields(String[] names) {
        for (String name : names) {
            if (!mFields.containsKey(name)) {
                final Field f = new Field(name, this);
                // note: Field may have trimmed the name: refetch
                mFields.put(f.getName(), f);
            }
        }
    }
    
    public void ensureFields(int count) {
        int curFields = mFields.size();
        if (count <= curFields)
            return;
        for (int i = curFields; i < count; i++) {
            String name = "Column " + (i+1);
            mFields.put(name, new Field(name, this));
        }
    }
    
//     public void createFields(String[] names) {
//         for (String name : names)
//             mFields.put(name, new Field(name, this));
//     }
    
//     public void createFields(int count) {
//         for (int i = 0; i < count; i++) {
//             String name = "Column " + (i+1);
//             mFields.put(name, new Field(name, this));
//         }
//     }

    private static boolean isUnlikelyKeyField(Field f) {
        // hack for dublin-core fields (e.g., dc:creator), which may often
        // all be unique (e.g., short RSS feed), but are unlikely to be useful keys.
        return f.isSingleValue() || (f.getName().startsWith("dc:") && !f.getName().equals("dc:identifier"));
    }
    
    /** look at all the Fields and make a guess as to which is the most likely key field
     * This currently will always return *some* field, even if it's not a possible key field. */
    public Field getKeyFieldGuess() {

        // Many RSS feeds can be covered by checking "guid" and "link"
        
        Field f;
        if ((f = getField("guid")) != null && f.isPossibleKeyField())
            return f;
        if ((f = getField("key")) != null && f.isPossibleKeyField())
            return f;
        //if ((f = getField("link")) != null && f.isPossibleKeyField()) // some rss news feeds have dupe entries
        if ((f = getField("link")) != null && !f.isSingleValue())
            return f;

        // todo: identifying the shortest field isn't such a good strategy
            
        Field firstField = null;
        Field shortestField = null;
        int shortestFieldLen = Integer.MAX_VALUE;

        for (Field field : getFields()) {
            if (firstField == null)
                firstField = field;
            if (field.isPossibleKeyField() && !isUnlikelyKeyField(field)) {
                if (field.getMaxValueLength() < shortestFieldLen) {
                    shortestField = field;
                    shortestFieldLen = field.getMaxValueLength();
                }
            }
        }

//         if (shortestField == null) {
//             for (Field field : getFields()) {
//                 if (field.getMaxValueLength() < shortestFieldLen) {
//                     shortestField = field;
//                     shortestFieldLen = field.getMaxValueLength();
//                 }
//             }
//         }

        return shortestField == null ? firstField : shortestField;
    }
        
    public void dumpSchema(PrintStream ps) {
        dumpSchema(new PrintWriter(new OutputStreamWriter(ps)));
    }
        
    public void dumpSchema(PrintWriter ps) {
        ps.println(getClass().getName() + ": ");
        final int pad = -mLongestFieldName;
        final String format = "%" + pad + "s: %s\n";
        for (Field f : mFields.values()) {
            ps.printf(format, f.getName(), f.valuesDebug());
        }
        //ps.println("Rows collected: " + rows.size());
    }

    public String getDump() {
        StringWriter debug = new StringWriter();
        dumpSchema(new PrintWriter(debug));
        return debug.toString();
    }
    

    protected void addRow(DataRow row) {
        mRows.add(row);
    }
    
    protected void addRow(String[] values) {

        DataRow row = new DataRow();
        int i = 0;
        for (Field field : getFields()) {
            final String value = values[i++];
            row.addValue(field, value);
        }
        addRow(row);
    }
    
    public List<DataRow> getRows() {
        return mRows;
    }

    public Collection<Field> getFields() {
        return mFields.values();
    }
    
    public int getFieldCount() {
        return mFields.size();
    }

    /** if a singleton exists with the given name, return it's value, otherwise null */
    public String getSingletonValue(String name) {
        Field f = mFields.get(name);
        if (f != null && f.isSingleton())
            return f.getValueSet().iterator().next();
        else
            return null;
    }
    

    // todo: factor out XML impl reference
    public boolean isXMLKeyFold() {
        return false;
    }

    

}

/** a row impl that handles flat tables as well as Xml style variable "rows" or item groups */
//class DataRow extends tufts.vue.MetaMap {
final class DataRow {

    final tufts.vue.MetaMap mmap = new tufts.vue.MetaMap();

    boolean isContextChanged;

    void setContextChanged(boolean t) {
        isContextChanged = t;
    }

    boolean isContextChanged() {
        return isContextChanged;
    }
        

    void addValue(Field f, String value) {
        value = value.trim();
        
//         final String existing = values.put(f, value);
//         if (existing != null && Schema.DEBUG)
//             Log.debug("ROW SUB-KEY COLLISION " + f);
            
        //super.put(f.getName(), value);
        mmap.put(f.getName(), value);
        f.trackValue(value);
    }

    Iterable<Map.Entry> dataEntries() {
        return mmap.entries();
    }

    //@Override public String getValue(String key) {
    public String getValue(String key) {
        return mmap.getString(key);
        //return super.getString(key);
    }

    String getValue(Field f) {
        return mmap.getString(f.getName());
        //return super.getString(f.getName());
    }

    boolean contains(Field field, Object value) {
        return value != null && value.equals(getValue(field));
    }

    @Override public String toString() {
        return mmap.values().toString();
    }

    tufts.vue.MetaMap getData() {
        return mmap;
    }
    
    
}

// /** a row impl that handles flat tables as well as Xml style variable "rows" or item groups */
// class DataRow {

//     private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataRow.class);
    
//     // this impl is overkill for handling flat tabular data

//     final Map<Field,String> values; // isn't this just duplicated in mData???
//     final Multimap mData = Multimaps.newLinkedHashMultimap();

//     DataRow() {
//         values = new HashMap();
//     }

//     void addValue(Field f, String value) {
//         value = value.trim();
//         final String existing = values.put(f, value);
//         if (existing != null && Schema.DEBUG)
//             Log.debug("ROW SUB-KEY COLLISION " + f);
            
//         mData.put(f.getName(), value);
//         f.trackValue(value);
//     }

//     //Iterable<Map.Entry<String,String>> entries() {
//     Iterable<Map.Entry> dataEntries() {
//         return mData.entries();
//     }
    
//     String getValue(Field f) {
//         return values.get(f);
//     }
//     String getValue(String key) {
//         Object o = mData.get(key);
//         if (o instanceof Collection) {
//             final Collection bag = (Collection) o;
//             if (bag.size() == 0)
//                 return null;
//             else if (bag instanceof List)
//                 return (String) ((List)bag).get(0);
//             else
//                 return (String) Iterators.get(bag.iterator(), 0);
//         }
//         else
//             return (String) o;
//         //return (String) Iterators.get(mData.get(key).iterator(), 0);
//         //return (String) asText.get(key);
//     }
        
// }







    // temp hack: if we keep this, move to elsewhere and/or have it populate an existing container
//     public LWMap.Layer createSchematicLayer() {

//         final LWMap.Layer layer = new LWMap.Layer("Schema: " + getName());

//         Field keyField = null;

//         for (Field field : getFields()) {
//             if (field.isPossibleKeyField() && !field.isLenghtyValue()) {
//                 keyField = field;
//                 break;
//             }
//         }
//         // if didn't find a "short" key field, find the shortest
//         if (keyField == null) {
//             for (Field field : getFields()) {
//                 if (field.isPossibleKeyField()) {
//                     keyField = field;
//                     break;
//                 }
//             }
//         }

//         boolean labelGuessed = false;

//         LWNode keyNode = null;
//         if (keyField != null) {
//             keyNode = new LWNode("itemNode");
//             keyNode.setShape(java.awt.geom.Ellipse2D.Float.class);
//             keyNode.setProperty(tufts.vue.LWKey.FontSize, 32);
//             keyNode.setNotes(getDump());
//         }

//         int y = Short.MAX_VALUE;
//         for (Field field : Util.reverse(getFields())) { // reversed to preserve on-map stacking order
//             if (field.isSingleton())
//                 continue;
//             LWNode colNode = new LWNode(field.getName());
//             colNode.setLocation(0, y--);
//             if (field.isPossibleKeyField()) {
//                 if (keyNode != null && !labelGuessed) {
//                     keyNode.setLabel("${" + field.getName() + "}");
//                     labelGuessed = true;
//                 }
//                 colNode.setFillColor(java.awt.Color.red);
//             } else
//                 colNode.setFillColor(java.awt.Color.gray);
//             colNode.setShape(java.awt.geom.Rectangle2D.Float.class);
//             colNode.setNotes(field.valuesDebug());
//             layer.addChild(colNode);

//             if (keyNode != null)
//                 layer.addChild(new tufts.vue.LWLink(keyNode, colNode));
            
//         }

//         if (keyNode != null) {
//             layer.addChild(keyNode);
//             //tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//             tufts.vue.Actions.MakeCircle.act(Collections.singletonList(keyNode));
//         } else {
//             tufts.vue.Actions.MakeColumn.act(layer.getChildren());
//         }

//         return layer;
//     }
