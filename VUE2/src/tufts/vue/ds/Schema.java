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
import tufts.vue.DEBUG;
import tufts.vue.MetaMap;

import java.util.*;
import java.io.*;
import java.net.URL;

import tufts.vue.Resource;
import tufts.vue.LWComponent;

import org.xml.sax.InputSource;

import com.google.common.collect.*;


/**
 * @version $Revision: 1.37 $ / $Date: 2009-06-10 16:03:33 $ / $Author: sfraize $
 * @author Scott Fraize
 */


// todo: create a DataSet object, which is a combination of a Schema,
// the DataSet source (use a Resource?), and the holder of the actual
// row data.

public class Schema implements tufts.vue.XMLUnmarshalListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Schema.class);

    protected final Map<String,Field> mFields = new LinkedHashMap(); // "columns"

    private final Collection<Field> mPersistFields = new ArrayList();
    private boolean mXMLRestoreUnderway;
        
    private Field mKeyField;

    private final List<DataRow> mRows = new ArrayList();

    //private Object mSource;
    private Resource mResource;
    
    protected int mLongestFieldName = 10;

    private String mName;
    private Field mImageField;

    private LWComponent mStyleNode;

    private String GUID;
    private String DSGUID;

    boolean mKeyFold;
        
    

//     /** construct an empty schema */
//     public Schema() {}

    /** id used for within-map reference for persisting the relationship between a MetaMap and a schema */
    private final String mLocalID;

    private int mContextRowNodeCount;

    private static final java.util.concurrent.atomic.AtomicInteger NextLocalId = new java.util.concurrent.atomic.AtomicInteger();

    ///** map of locations/addresses to schema instances */
    //private static final Map<String,Schema> SchemaMap = new java.util.concurrent.ConcurrentHashMap();

    /** contains "empty" (no data) schemas, which are retained as handles to be replaced if actual data schemas arrive */
    private static final Collection<Schema> SchemaHandles = Collections.synchronizedList(new ArrayList());
    private static final Map<Resource,Schema> SchemaByResource = Collections.synchronizedMap(new HashMap());
    private static final Map<String,Schema> SchemaByGUID = Collections.synchronizedMap(new HashMap());
    
    public static Schema instance(Resource r, String dataSourceGUID) {
        final Schema s = new Schema();
        s.setResource(r);
        s.setDSGUID(dataSourceGUID);
        s.setGUID(edu.tufts.vue.util.GUID.generate());
        if (DEBUG.SCHEMA) Log.debug("INSTANCED SCHEMA " + s + "\n");

        // may want to wait to do this until we actually load the rows...        
        SchemaByResource.put(r, s);
        if (dataSourceGUID != null)
            SchemaByGUID.put(dataSourceGUID, s);
            
        return s;
    }

    /** If an existing schema exists matching the GUID, return that, otherwise, a new schema instance */
    // TODO: factor in looking up based on the resource if the DSGUID doesn't match for some reason
    public static Schema fetch(Resource r, String dataSourceGUID) {
        Schema s = SchemaByGUID.get(dataSourceGUID);
        if (s == null) {
            if (DEBUG.SCHEMA) Log.debug("fetch: GUID " + Util.tags(dataSourceGUID) + " not found; instancing new");
            return instance(r, dataSourceGUID);
        } else {
            if (DEBUG.SCHEMA) {
                Log.debug("fetch: found by GUID " + s);
                Log.debug("fetch: existing fields:");
                Util.dump(s.mFields);
            }
            return s;
        }
    }

    /** for looking up a loaded (data-containing) schema from a an empty schema-handle.
     * If the given schema is already loaded, or if no matching loaded schema can be found,
     * the passed in schema is returned.
     */
    public static Schema lookup(Schema schema) {
        if (DEBUG.SCHEMA && DEBUG.META) Log.debug("LOOKUP SCHEMA " + schema);
        if (schema == null)
            return null;
        if (schema.isLoaded())
            return schema;
        final Resource resource = schema.getResource();
        Schema loaded = SchemaByResource.get(resource);
        Object matched = resource;
        if (loaded == null) {
            loaded = SchemaByGUID.get(matched = schema.getDSGUID());
            if (DEBUG.SCHEMA && DEBUG.META && loaded != null) {
                Log.debug("MATCHED SCHEMA BY GUID: " + matched + " " + loaded);
            }
        }
        if (loaded != null) {
            if (DEBUG.SCHEMA && DEBUG.DATA) Log.debug("MATCHED EMPTY SCHEMA " + matched + " to " + loaded);
            return loaded;
        } else
            return schema;

        // now, even if resource doesn't match, look up based on GUID, as a url may have slightly changed.
    }


    /** find all schema handles in all nodes that match the new schema
     * and replace them with pointers to the live data schema */
    public static void updateAllSchemaReferences(final Schema newlyLoadedSchema,
                                                  final Collection<tufts.vue.LWMap> maps)
    {
        if (DEBUG.Enabled) Log.debug("updateAllSchemaReferences; " + newlyLoadedSchema + "; maps: " + maps);
        
        if (!newlyLoadedSchema.isLoaded()) {
            Log.warn("newly loaded schema is empty: " + newlyLoadedSchema, new Throwable("FYI"));
            return;
        }
        
        int updateCount = 0;
        int mapUpdateCount = 0;

        // todo: if this ever gets slow, could improve performance by pre-computing a
        // lookup map of all schema handles that map to the new schema (which will
        // usually contain only a single schema mapping) then we only have to check
        // every schema reference found against the pre-computed lookups instead of
        // doing a Schema.lookup against all loaded Schema's.
        
        for (tufts.vue.LWMap map : maps) {
            final int countAtMapStart = updateCount;
            Collection<LWComponent> nodes = map.getAllDescendents();
            for (LWComponent c : nodes) {
                final MetaMap data = c.getRawData();
                if (data == null)
                    continue;
                final Schema curSchema = data.getSchema();
                if (curSchema != null) {
                    final Schema newSchema = Schema.lookup(curSchema);
                    if (newSchema != curSchema) {
                        data.setSchema(newSchema);
                        updateCount++;
                        if (newSchema != newlyLoadedSchema) {
                            Log.warn("out of date schema in " + c + "; oldSchema=" + curSchema + "; replaced with " + newSchema);
                        } else {
                            //if (DEBUG.SCHEMA) Log.debug("replaced schema handle in " + c);
                        }

                    }
                }
            }
            if (updateCount > countAtMapStart)
                mapUpdateCount++;
        }
        Log.info(String.format("updated %d schema handle references in %d maps", updateCount, mapUpdateCount));
    }
    

    /** interface {@link XMLUnmarshalListener} -- track us */
    public void XML_completed(Object context) {
        SchemaHandles.add(this);
        // As the resource isn't in the LWComponent hierarchy, it won't be updated in the map.
        // Too bad actually -- then it could make use of the relative path code -- would be
        // a good idea to move these to the map.
        ((tufts.vue.URLResource)getResource()).XML_completed("SCHEMA-MANUAL-INIT");
        DSGUID = getResource().getProperty("@DSGUID");
        if (DEBUG.Enabled) Log.debug("RESTORED SCHEMA " + this + "; DSGUID=" + DSGUID);
        if (DSGUID != null) {
            Schema prev = SchemaByGUID.put(DSGUID, this);
            if (prev != null)
                Log.warn("BLEW AWAY PRIOR SCHEMA " + prev, new Throwable("HERE"));
        }
        //Log.debug("LOAD FIELDS WITH " + mPersistFields);
        for (Field f : mPersistFields) {
            final LWComponent style = f.getStyleNode();
            if (DEBUG.Enabled) Log.debug("loading field " + f + "; style:" + style);
            initStyleNode(style);
            mFields.put(f.getName(), f);
        }
        mXMLRestoreUnderway = false;
    }

    //private static tufts.vue.LWContainer FalseStyleParent = new tufts.vue.LWNode("FalseStyleParent");

    private static void initStyleNode(LWComponent style) {
        if (style != null) {
            // the INTERNAL flag permits the style to operate (deliver events) w/out a parent
            //style.setFlag(LWComponent.Flag.INTERNAL);
            style.clearFlag(LWComponent.Flag.INTERNAL); // no longer used to deliver events
            // the DATA_STYLE bit is not persisted, must restore this bit manually:
            style.setFlag(LWComponent.Flag.DATA_STYLE);

            // as the style objects aren't proper children of the map, they never get this
            // cleared, and we have to do it here manually to make sure
            style.markAsRestored();

            // hack to give the style a parent so it is considered "alive" and can deliver events
            //style.setParent(FalseStyleParent);
        }
    }

    
    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_initialized(Object context) { mXMLRestoreUnderway = true; }
    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_fieldAdded(Object context, String name, Object child) {}
    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_addNotify(Object context, String name, Object parent) {}
    

//     @Override
//     public boolean equals(Object o) {
//         if (this == o)
//             return true;
//         else if (o == null)
//             return false;
//         else {
//             final Schema s;
//             try {
//                 s = (Schema) o;
//             } catch (ClassCastException e) {
//                 return false;
//             }
//             return mLocalID.equals(s.mLocalID);
//         }
//     }
    
    
//     private Schema(Object source) {
//         // would be very handy if source was a Resource and Resources had IO methods
//         setSource(source);
//         setGUID(edu.tufts.vue.util.GUID.generate());
//         mLocalID = nextLocalID();
//     }

    /** for castor de-serialization only */
    public Schema() {
        mLocalID = nextLocalID();
        Log.debug("CONSTRUCTED SCHEMA " + this + "\n");
    }

    private static String nextLocalID() {
        // must differentiate this from the the codes used for persisting nodes (just integer strings),
        // as castor apparently uses a global mapping for all object types, instead of an id reference
        // cache per-type (class).
        return String.format("S%d", NextLocalId.incrementAndGet());
    }

    public final String getMapLocalID() {
        return mLocalID;
    }

    /** @return true if this Schema contains data.  Persisted Schema's are just references and do not contain data */
    public boolean isLoaded() {
        return mRows.size() > 0;
    }
    
    public final void setMapLocalID(String id) {
        Log.debug("SCHEMA ID WAS PERSISTED AS " + id + "; " + this);
        //mLocalID.set(i);
    }

    public void setImageField(String name) {
        if (name == null)
            mImageField = null;
        else
            mImageField = findField(name);
    }
    public Field getImageField() {
        return mImageField;
    }

    public synchronized void annotateFor(Collection<LWComponent> nodes) {

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

        mContextRowNodeCount = 0;

        for (DataRow row : mRows) {

            final String rowKey = row.getValue(keyField);

            row.mContextCount = 0;
            row.setContextChanged(false);

            for (LWComponent node : nodes) {

                if (!node.isDataRow(this))
                    continue;

                mContextRowNodeCount++; // todo: is wildly overcounting -- need to total at end by adding all final row.mContextCount's

                if (!node.hasDataValue(keyFieldName, rowKey))
                    continue;

                row.mContextCount++;
                
                final MetaMap rawData = row.getData();
                final MetaMap mapData = node.getRawData();
                //Log.debug("comparing:\n" + rawData.values() + " to:\n" + mapData.values());
                if (rawData != mapData) {
                    // todo: would be nice to tag each field to see what changed, tho
                    // that adds another bit for every single value in a data-set, and
                    // we have no per-value meta-data in the DataRow at the moment
                    row.setContextChanged(!rawData.equals(mapData));
                }

                

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

    public int getContextRowNodeCount() {
        return mContextRowNodeCount;
    }

    public void flushData() {
        if (DEBUG.Enabled) Log.debug("flushing " + this);
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
        //return getName();
        try {
            return String.format("Schema@%x[%s;%s #%d \"%s\" %s]",
                                 System.identityHashCode(this),
                                 getMapLocalID(),
                                 DSGUID == null ? "" : (" " + DSGUID + "; "),
                                 mFields.size(), getName(), getResource());
        } catch (Throwable t) {
            return String.format("Schema@%x[%s; %s; #%d \"%s\" %s]",
                                 System.identityHashCode(this),
                                 ""+mLocalID,
                                 DSGUID,
                                 mFields.size(), ""+mName, ""+mResource);
        }
    }

    public String getGUID() {
        return GUID;
    }

    public void setGUID(String s) {
        GUID = s;
    }

    void setDSGUID(String s) {
        //if (DEBUG.Enabled) Util.printStackTrace("setDSGUID [" + s + "]");
        DSGUID = s;
        getResource().setProperty("@DSGUID", s);
        if (DSGUID != null)
            SchemaByGUID.put(DSGUID, this);        
    }
    
    String getDSGUID() {
        return DSGUID;
    }
    
    /** set the node style object used for record/row nodes */
    public void setRowNodeStyle(LWComponent style) {
        if (DEBUG.SCHEMA) Log.debug("setRowNodeStyle: " + style);
        initStyleNode(style);
        mStyleNode = style;
    }
        
    /** @return the node style used for record/row nodes */
    public LWComponent getRowNodeStyle() {
        return mStyleNode;
    }
    
    public Field getKeyField() {
        if (mKeyField == null)
            mKeyField = getKeyFieldGuess();
        return mKeyField;
    }

    public String getKeyFieldName() {
        return getKeyField().getName();
    }
    
    public void setKeyField(Field f) {
        //Log.debug("setKeyField " + Util.tags(f));
        mKeyField = f;
    }
    
    public void setKeyField(String name) {
        setKeyField(getField(name));
    }
        
//     public Object getSource() {
//         return mSource;
//     }
    
//     public void setSource(Object src) {
//         mSource = src;

//         try {
//             setResource(src);
//         } catch (Throwable t) {
//             Log.warn(t);
//         }
//     }

//     private void setResource(Object r) {
    
//         if (r instanceof Resource)
//             mResource = (Resource) r;
//         else if (r instanceof InputSource)
//             mResource = Resource.instance(((InputSource)r).getSystemId());
//         else if (r instanceof File)
//             mResource = Resource.instance((File)r);
//         else if (r instanceof URL)
//             mResource = Resource.instance((URL)r);
//         else if (r instanceof String)
//             mResource = Resource.instance((String)r);
//     }

    public void setResource(Resource r) {
        Log.debug(this + "; setResource " + r);
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
        final Field f = findField(name);
        if (f == null) Log.debug(String.format("%s; no field named '%s' in %s", this, name, mFields.keySet()));
        return f;
    }
    
    /** will quietly return null if not found */
    public Field findField(String name) {
        Field f = mFields.get(name);
        if (f == null && name != null && name.length() > 0 && Character.isUpperCase(name.charAt(0))) {
            f = mFields.get(name.toLowerCase());
            if (DEBUG.SCHEMA && f != null) Log.debug("found lowerized [" + name + "]");
        }
        return f;
    }

    // todo: doesn't use same case indepence as findField
    public boolean hasField(String name) {
        return mFields.containsKey(name);
    }
    
    
    public String getName() {

        if (mName != null)
            return mName;
        else
            return getResource().getTitle();

        //return getSource().toString();
        
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

        if (DEBUG.SCHEMA) {
            Log.debug("ensureFields:");
            Util.dump(names);
            Log.debug("ensureFields; existingFields:");
            Util.dump(mFields);
        }
        
        for (String name : names) {
            name = name.trim();
            if (!mFields.containsKey(name)) {
                final Field f = new Field(name, this);
                // note: Field may have trimmed the name: we refetch just in case
                mFields.put(f.getName(), f);
            }
        }

        // TODO: if any fields already exists and are NOT named in names, we at least
        // need debug here: the schema has changed!  (which may be normal for XML --
        // e.g., a news feed) What to do?  If we don't clear them out they'll remain
        // as empty fields in the data-tree.
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

    // this returns a Vector so can be fed directly to a JComboBox if desired
    public Vector<String> getPossibleKeyFieldNames() {
        final Vector<String> possibleKeyFields = new Vector();

        for (Field field : getFields())
            if (field.isPossibleKeyField())
                possibleKeyFields.add(field.getName());

        if (possibleKeyFields.size() == 0) {
            // add them all: some data rows are probably duplicates, and we can't
            // identify a key field
            for (Field field : getFields())
                if (!field.isSingleton())
                    possibleKeyFields.add(field.getName());
        }

        return possibleKeyFields;
    }

    // this returns a Vector so can be fed directly to a JComboBox if desired
    public Vector<String> getFieldNames() {
        final Vector<String> names = new Vector();

        for (Field field : getFields())
            names.add(field.getName());

        return names;
    }
    
    
    
    /** look at all the Fields and make a guess as to which is the most likely key field
     * This currently will always return *some* field, even if it's not a possible key field. */
    public Field getKeyFieldGuess() {

        if (getFieldCount() == 0)
            throw new Error("no fields in " + this);

        // Many RSS feeds can be covered by checking "guid" and "link"
        
        Field f;
        if ((f = findField("guid")) != null && f.isPossibleKeyField())
            return f;
        if ((f = findField("key")) != null && f.isPossibleKeyField())
            return f;
        //if ((f = getField("link")) != null && f.isPossibleKeyField()) // some rss news feeds have dupe entries
        if ((f = findField("link")) != null && !f.isSingleValue())
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

        final Field guess = shortestField == null ? firstField : shortestField;

        if (guess == null) {
            //Log.warn("no key field: " + this, new Throwable("FYI"));
//             Log.warn("key field guess failed in: " + this);
//             dumpSchema(System.err);
            throw new Error("no key field found in " + this);
        }

        return guess;
    }
        
    public void dumpSchema(PrintStream ps) {
        dumpSchema(new PrintWriter(new OutputStreamWriter(ps)));
    }
        
    public void dumpSchema(PrintWriter ps) {
        ps.println(getClass().getName() + ": " + toString());
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

        DataRow row = new DataRow(this);
        int i = 0;
        for (Field field : getFields()) {
            final String value;
            try {
                value = values[i++];
            } catch (IndexOutOfBoundsException e) {
                Log.warn("missing value at index " + (i-1) + " for field " + field + " in " + Arrays.asList(values));
                Util.dumpArray(values);
                row.addValue(field, "<missing>");
                continue;
            }
                    
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

    public Collection<Field> getXMLFields() {
        if (mXMLRestoreUnderway) {
            // return the list to be *loaded* by castor
            return mPersistFields;
        } else
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
    

    public boolean isXMLKeyFold() {
        return mKeyFold;
    }
    public void setXMLKeyFold(boolean keyFold) {
        mKeyFold = keyFold;
    }
    

}

/** a row impl that handles flat tables as well as Xml style variable "rows" or item groups */
//class DataRow extends tufts.vue.MetaMap {
final class DataRow {

    final tufts.vue.MetaMap mmap = new tufts.vue.MetaMap();

    boolean isContextChanged;
    int mContextCount;

    DataRow(Schema s) {
        mmap.setSchema(s);
    }

    void setContextChanged(boolean t) {
        isContextChanged = t;
    }

    boolean isContextChanged() {
        return isContextChanged;
    }

    public int getContextCount() {
        return mContextCount;
    }
        

    void addValue(Field f, String value) {
        value = value.trim();
        
//         final String existing = values.put(f, value);
//         if (existing != null && Schema.DEBUG)
//             Log.debug("ROW SUB-KEY COLLISION " + f);
            
        //super.put(f.getName(), value);
        if (value.length() == 0)
            value = Field.EMPTY_VALUE;
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
