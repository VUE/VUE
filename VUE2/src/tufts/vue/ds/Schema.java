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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * A Schema is combination data definition, and when loaded with data, serves as a
 * minature, searchable data-base with automatic up-front analyitics.  Its "rows" of
 * data are all key-value paired, so not all rows have to contain all fields, which is
 * important for treating repeated XML fragments as "rows" (e.g., RSS feed items).
 * The data analysis performed looks at each column (Field) and if the values are
 * generally "short" enough, it will enumerate all the unique values found in that
 * column.
 *
 * @version $Revision: 1.46 $ / $Date: 2009-09-28 18:59:50 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class Schema implements tufts.vue.XMLUnmarshalListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Schema.class);

    /** A map of all Fields from all schemas for auto-discovering associations based on the field name */

//     private static final Multimap<String,Field> AllFieldsByLowerName = Multimaps.synchronizedMultimap
//         ((Multimap<String,Field>)Multimaps.newHashMultimap()); // WTF?  Strange javac complaint...
    private static final Multimap<String,Field> AllFieldsByLowerName = Multimaps.newHashMultimap();
    
    /** All possible column's in this Schema */
    private final Map<String,Field> mFields = new HashMap();
    //private final Map<String,Field> mFields = new LinkedHashMap();
    /** Ordered list of column's in this Schema -- same contents as mFields */
    private final List<Field> mFieldList = new ArrayList();

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

    ///** contains "empty" (no data) schemas, which are retained as handles to be replaced if actual data schemas arrive */
    //private static final Collection<Schema> SchemaHandles = Collections.synchronizedList(new ArrayList());
    private static final Map<Resource,Schema> _SchemaByResource = Collections.synchronizedMap(new HashMap());
    private static final Map<String,Schema> _SchemaByDSGUID = Collections.synchronizedMap(new HashMap());
    
    public static Schema newInstance(Resource r, String dsGUID) {

        Schema prev;

        final Schema s = new Schema();
        
         // may want to wait to do registrations until we actually load the rows...
        
//         if (dsGUID != null)
//             registerByDSGUID(s, dsGUID);
//         registerByResource(s, r);

        s.setResource(r);
        s.setDSGUID(dsGUID);
        s.setGUID(edu.tufts.vue.util.GUID.generate());
        //if (DEBUG.SCHEMA) Log.debug("INSTANCED SCHEMA " + s + "\n");
        if (DEBUG.SCHEMA) Log.debug(s + "; INSTANCED");

        if (dsGUID != null)
            registerByDSGUID(s, dsGUID);
        registerByResource(s, r);
        
        if (DEBUG.SCHEMA) Log.debug(s + "; REGISTERED");
        
        return s;
    }

    public static Set<Map.Entry<String,Schema>> getAllByDSGUID() {
        return _SchemaByDSGUID.entrySet();
    }
    public static Set<Map.Entry<Resource,Schema>> getAllByResource() {
        return _SchemaByResource.entrySet();
    }

    private static void registerByDSGUID(Schema s, String dsGUID) {
        Schema prev = _SchemaByDSGUID.put(dsGUID, s);
        if (prev != null) {
            Log.warn("REPLACING EXISTING DSGUID; orphaning: " + prev, new Throwable("HERE"));
        }
        Log.info(s + "; recorded as AUTHORITATIVE for DSGUID");
        if (s.getDSGUID() != dsGUID)
            Log.warn(new Throwable("INCONSISTENT DSGUID STATE"));
    }
    
    private static void registerByResource(Schema s, Resource r) {
        Schema prev = _SchemaByResource.put(r, s);
        if (prev != null) {
            Log.warn("REPLACING EXISTING byRESOURCE; orphaning" + prev, new Throwable("HERE"));
        }
        Log.info(s + "; recorded as AUTHORITATIVE for Resource: " + r);
        if (s.getResource() != r)
            Log.warn(new Throwable("INCONSISTENT RESOURCE STATE"));
    }

    /** If an existing schema exists matching the DSGUID, return that, otherwise, a new schema instance */
    // TODO: factor in looking up based on the resource if the DSGUID doesn't match for some reason
    public static Schema getInstance(Resource r, String dataSourceGUID) {
        Schema s = _SchemaByDSGUID.get(dataSourceGUID);
        if (s == null) {
            if (DEBUG.SCHEMA) Log.debug("fetch: dsGUID " + Util.tags(dataSourceGUID) + " not found; instancing new");
            return newInstance(r, dataSourceGUID);
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
    public static Schema lookupAuthority(final Schema schema) {
        if (DEBUG.SCHEMA && DEBUG.META) Log.debug("LOOKUP SCHEMA " + schema);
        if (schema == null)
            return null;
        if (schema.isLoaded())
            return schema;

        final Schema authoritativeSchema = lookForReplacement(schema);

        if (authoritativeSchema != null)
            return authoritativeSchema;
        else
            return schema;
        
    }

    private static Schema lookForReplacement(final Schema schemaHandle)
    {
        final Resource resource = schemaHandle.getResource();
        Schema authority = _SchemaByResource.get(resource);
        String matchedBy = "RESOURCE";
        Object matchData = resource;
        if (authority == null) {
            authority = _SchemaByDSGUID.get(matchData = schemaHandle.getDSGUID());
            matchedBy = "DSGUID";
        }
        
        // also, even if resource & DSGUID doesn't match, look up based on GUID, as a url may have slightly changed.

        if (authority == schemaHandle)
            return null;
        else if (authority != null) {
            Log.debug(String.format("MATCHED BY %s=%s:\n\t   handle: %s\n\tauthority: %s",
                                    matchedBy,
                                    matchData,
                                    schemaHandle,
                                    authority));
            return authority;
        } else
            return null;
    }


//     private static Schema lookForMatching(final Schema schemaHandle)
//     {
//         final Resource resource = schemaHandle.getResource();
//         Schema loaded = SchemaByResource.get(resource);
//         Object matched = resource;
//         if (loaded == null) {
//             loaded = SchemaByGUID.get(matched = schemaHandle.getDSGUID());
//             if (DEBUG.SCHEMA /*&& DEBUG.META*/ && loaded != null) {
//                 Log.debug("MATCHED BY GUID: " + matched + " " + loaded);
//             }
//         } else {
//             Log.debug("MATCHED BY " + resource);
//         }

//         // now, even if resource & DSGUID doesn't match, look up based on GUID, as a url may have slightly changed.
        
//         if (loaded != null) {
//             //if (DEBUG.SCHEMA /*&& DEBUG.DATA*/) Log.debug("MATCHED EMPTY SCHEMA " + matched + " to " + loaded);
//             return loaded;
//         } else
//             return null;
        
// //         final String dataSourceGUID = schemaHandle.DSGUID;
// //         Schema existing = SchemaByGUID.get(dataSourceGUID);
// //         return existing;
//     }
    
    /** find all schema handles in all nodes that match the new schema
     * and replace them with pointers to the live data schema */
    public static void updateAllSchemaReferences(final Schema newlyLoadedSchema,
                                                  final Collection<tufts.vue.LWMap> maps)
    {
        if (DEBUG.Enabled) {
            Log.debug("updateAllSchemaReferences; " + newlyLoadedSchema + "; maps: " + maps);
            Util.dump(_SchemaByResource, "BY-RESOURCE");
            Util.dump(_SchemaByDSGUID, "BY-DSGUID");
        }
        
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
            final Collection<LWComponent> nodes = map.getAllDescendents(LWComponent.ChildKind.ANY);
            for (LWComponent c : nodes) {
                final MetaMap data = c.getRawData();
                if (data == null)
                    continue;
                final Schema curSchema = data.getSchema();
                if (curSchema != null) {
                    final Schema newSchema = Schema.lookupAuthority(curSchema);
//                     Log.debug("curs: " + newSchema);
//                     Log.debug("auth: " + newSchema);
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

    private static void replaceSchemaReferences
        (final Schema oldSchema,
         final Schema newSchema,
         final Collection<LWComponent> nodes,
         final tufts.vue.LWMap sourceMap)
    {
        int updateCount = 0;

        newSchema.setRowNodeStyle(oldSchema.getRowNodeStyle()); // ideally, only when the existing isn't already used anywhere

        for (LWComponent c : nodes) {
            final MetaMap data = c.getRawData();
            if (data == null)
                continue;
            if (data.getSchema() == oldSchema) {
                data.setSchema(newSchema);
                updateCount++;
            }
//             else if (DEBUG.Enabled) {
//                 Log.debug("keeping schema: " + data.getSchema());
//             }
        }

        Log.info(String.format("updated %d schema handle references in %s", updateCount, sourceMap));

    }


//     /** interface {@link XMLUnmarshalListener} -- track us */
//     public void XML_completed(Object context) {
//         //SchemaHandles.add(this);
//         // As the resource isn't in the LWComponent hierarchy, it won't be updated in the map.
//         // Too bad actually -- then it could make use of the relative path code -- would be
//         // a good idea to move these to the map.
//         ((tufts.vue.URLResource)getResource()).XML_completed("SCHEMA-MANUAL-INIT");
//         DSGUID = getResource().getProperty("@DSGUID");
//         if (DSGUID != null) {
//             Schema prev = SchemaByGUID.put(DSGUID, this);
//             if (prev != null)
//                 Log.warn("BLEW AWAY PRIOR SCHEMA " + prev, new Throwable("HERE"));
//         }
//         //Log.debug("LOAD FIELDS WITH " + mPersistFields);
//         for (Field f : mPersistFields) {
//             final LWComponent style = f.getStyleNode();
//             f.setSchema(this);
//             if (DEBUG.Enabled) Log.debug(String.format("loading field %s%-20s%s style=%s",
//                                                        Util.TERM_GREEN, f, Util.TERM_CLEAR, style));
//             initStyleNode(style);
//             addField(f);
//         }
//         mXMLRestoreUnderway = false;
//         createAttachableAssociations();
//         if (DEBUG.Enabled) Log.debug(this + " RESTORED; DSGUID=" + DSGUID + "\n");
//     }

    
    /** interface {@link XMLUnmarshalListener} -- track us */
    public void XML_completed(Object context) {
        // As the resource isn't in the LWComponent hierarchy, it won't be updated in
        // the map.  TODO: too bad actually -- then it could make use of the
        // relative-to-map file path code -- would be a good idea to move these to the
        // map.
        ((tufts.vue.URLResource)getResource()).XML_completed("SCHEMA-MANUAL-INIT");
        
        this.DSGUID = getResource().getProperty("@DSGUID");

        for (Field field : mPersistFields) {
            field.setSchema(this);
            if (DEBUG.Enabled) Log.debug(String.format("seen field %s%-20s%s",
                                                       Util.TERM_YELLOW, field, Util.TERM_CLEAR));
        }
        
//         // Problem: doing this now, at deserialize time, means we wont see any Schema's
//         // that have yet to deserialize
//         createAttachableAssociations(mPersistFields);
        
        mXMLRestoreUnderway = false;
        if (DEBUG.Enabled) Log.debug(this + " RESTORED; DSGUID=" + DSGUID + "\n");
    }


    /** to be called on each schema at the end of a map load */
    public void syncToGlobalModel(tufts.vue.LWMap sourceMap, Collection<LWComponent> allRestored)
    {
        Log.debug(this + "; SYNC TO GLOBAL MODEL");
        
        boolean replacedByExistingSchema = false;
        if (getDSGUID() != null) {
            Schema existing = lookForReplacement(this);
            if (existing != null) {
                Log.debug("DUMPING THIS SCHEMA FOR EXISTING:\n\t  dumped: " + this + "\n\texisting: " + existing);
                replaceSchemaReferences(this, existing, allRestored, sourceMap);
                replacedByExistingSchema = true;
            } else {
                registerByDSGUID(this, getDSGUID());
            }
        }
        
        //Log.debug("LOAD FIELDS WITH " + mPersistFields);
        
        if (!replacedByExistingSchema) {
            for (Field f : mPersistFields) {

                final LWComponent style = f.getStyleNode();
                if (DEBUG.Enabled) Log.debug(String.format("keeping field %s%-23s%s style=%s",
                                                           Util.TERM_GREEN, f, Util.TERM_CLEAR, style));
                initStyleNode(style);
                addField(f);
            }
            createAttachableAssociations(mPersistFields);
            if (DEBUG.Enabled) Log.debug(this + " IS THE AUTHORITY; DSGUID=" + getDSGUID() + "\n");
            
        }
    }
    

    private void createAttachableAssociations(Collection<Field> restoredFields) {

        if (DEBUG.SCHEMA) Log.debug(this + "; createAttachableAssociations");

        for (Field field : restoredFields) {
            for (Field.PersistRef ref : field.getRelatedFieldRefs()) {
                if (DEBUG.SCHEMA) Log.debug(this + "; persisted relation for " + field + ": " + ref);
                for (Field possibleMatch : AllFieldsByLowerName.get(ref.fieldName.toLowerCase())) {
                    final String guid = possibleMatch.getSchema().getGUID();
                    Log.debug("Checking GUID " + guid);
                    if (ref.schemaGuid.equals(guid)) {
                        final Field match = possibleMatch;
                        Log.debug("found live field to match ref: " + ref + " = " + Util.tags(match));
                        Association.add(field, possibleMatch);
                    }
                }
            }
        }
    }
    
//     private void createAttachableAssociations() {

//         if (DEBUG.SCHEMA) Log.debug(this + "; createAttachableAssociations");

//         for (Field field : mFields.values()) {
//             for (Field.PersistRef ref : field.getRelatedFieldRefs()) {
//                 if (DEBUG.SCHEMA) Log.debug(this + "; persisted relation for " + field + ": " + ref);
//                 for (Field possibleMatch : AllFieldsByLowerName.get(ref.fieldName.toLowerCase())) {
//                     if (ref.schemaGuid.equals(possibleMatch.getSchema().getGUID())) {
//                         final Field match = possibleMatch;
//                         Log.debug("found live field to match ref: " + ref + " = " + Util.tags(match));
//                         Association.add(field, possibleMatch);
//                     }
//                 }
//             }
//         }
//     }

    //private static tufts.vue.LWContainer FalseStyleParent = new tufts.vue.LWNode("FalseStyleParent");

    private Field addField(final Field newField) {
        return addField(newField, null, false);
    }
        
    private synchronized Field addField(final Field newField, final Field nearField, boolean before)
    {
        newField.setSchema(this);

        // note: for new Fields, constructor may have trimmed the name,
        // so we should always fetch via f.getName() just in case.
        // (instead of ever passing in the name of the new field)
        final String name = newField.getName();
        
        mFields.put(name, newField);
        if (nearField != null)
            mFieldList.add(mFieldList.indexOf(nearField) + (before?0:1), newField);
        else
            mFieldList.add(newField);
        
        final String keyName = name.toLowerCase();
        // This will auto-add associations for fields of the same name -- too aggressive tho
        //Association.addByAll(newField, AllFieldsByLowerName.get(keyName));
        AllFieldsByLowerName.put(keyName, newField);

        return newField;
    }
    
    Field addFieldAfter(Field afterField, String name) {
        return addField(new Field(name.trim(), this), afterField, false);
    }
    Field addFieldBefore(Field beforeField, String name) {
        return addField(new Field(name.trim(), this), beforeField, true);
    }
    
    protected Field addField(String name) {
        return addField(new Field(name.trim(), this));
    }
    
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
        Log.debug(this + "; CONSTRUCTED FOR CASTOR");
    }

    private static String nextLocalID() {
        // must differentiate this from the the codes used for persisting nodes (just integer strings),
        // as castor apparently uses a global mapping for all object types, instead of an id reference
        // cache per-type (class).
        return String.format("S%d", NextLocalId.incrementAndGet());
    }

    /** for persistance */
    public final String getMapLocalID() {
        return mLocalID;
    }

    /** @return true if this Schema contains data.  Persisted Schema's are just references and do not contain data */
    public boolean isLoaded() {
        return mRows.size() > 0;
    }
    
    /** only debug during castor deserialization */
    public final void setMapLocalID(String id) {
        // we can safely ignore this -- a new current runtime map-local-id has already
        // been set -- the existing one is purely for castor's use in assigning references
        // from MetaMap's in the save file to this schema instance.
        Log.debug(this + "; safely ignoring persisted map-id [" + id + "]");
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

        for (Field field : getFields())
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
        for (Field f : getFields()) {
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
        try {
            String extra = "";
            if (DEBUG.META) extra = " " + DSGUID + "/" + mResource;
            return String.format("Schema@%07x[%s/%s; #%dx%d %s k=%s%s]",
                                 System.identityHashCode(this),
                                 ""+mLocalID,
                                 //""+GUID,
                                 ""+DSGUID,
                                 mFields.size(),
                                 mRows.size(),
                                 Util.color(mName, Util.TERM_PURPLE),
                                 Relation.quoteKey(mKeyField),
                                 extra);
        } catch (Throwable t) {
            t.printStackTrace();
            return "Schema[" + t + "]";
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
        if (DEBUG.SCHEMA) Log.debug(this + "; setDSGUID: " + s);
        DSGUID = s;
        getResource().setProperty("@DSGUID", s);
//         if (DSGUID != null)
//             registerByDSGUID(this, DSGUID);
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
        if (name == null) {
            Log.warn("searching for null Field name in " + this, new Throwable("HERE"));
            return null;
        }
        final Field f = findField(name);
        if (f == null && DEBUG.Enabled) Log.debug(String.format("%s; no field named '%s' in %s",
                                                                this,
                                                                name,
                                                                //mFields.keySet()
                                                                Util.tags(mFields.keySet())
                                                                ));
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

    // todo: doesn't use same case indepence as findField
    public boolean hasField(Field f) {
        // todo: more performant (keep a set of just hashed Fields)
        // could just check f.getSchema() == this, tho that may not work at init time?
        return mFields.containsKey(f.getName());
    }
    
    
    public String getName() {

        if (mName != null) {
            return mName;
        } else {
            // for giving us something informative during construction
            final Resource r = getResource();
            String s = r.getTitle();
            if (s == null)
                return r.getSpec();
            else
                return s;
        }

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
                addField(new Field(name, this));
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
            addField(new Field(name, this));
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

        // todo: prioritize Fields who's values are all integers
            
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
        for (Field f : getFields()) {
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

    /** called by schema loaders to notify the Schema that all the rows have been loaded (and any final analysis can be completed) */
    void notifyAllRowsAdded() {
        
        for (Field f : Util.toArray(getFields(), Field.class)) { // must dupe as will may be adding new fields (quartiles)
            Log.debug("field analysis " + Relation.quoteKey(f.getName()) + ": type " + Util.tags(f.getType()));
            f.setStyleNode(DataAction.makeStyleNode(f));
            try {
                f.performFinalAnalysis();
            } catch (Throwable t) {
                Log.error("analysis failed on " + f, t);
            }
        }
    }

    public List<DataRow> getRows() {
        return mRows;
    }

//     /** @return rows that match the given key/value pair, allowing for user specified key associations */
//     public Collection<DataRow> getMatchingRows(Field field, String fieldValue)
//     {
//         if (!hasField(field)) {
//             Log.debug("getMatchingRows: immediate return, field not in Schema: " + field, new Throwable("HERE"));
//             return Collections.EMPTY_LIST;
//         }
        
//         final Collection<DataRow> results = new HashSet();
//         // todo: more performant than HashSet?  perhaps just scan at end for dupes
        
//         Relation.searchDataWithField(field, fieldValue, getRows(), results);
        
//         return results;
//     }
    
    /** @return rows that match the given key/value pair, allowing for user specified key associations */
    public Collection<DataRow> getMatchingRows(Field field, String fieldValue)
    {
        if (DEBUG.Enabled) Log.debug("getMatchingRows: " + Relation.quoteKV(field, fieldValue));
        
        final Collection<DataRow> results = new HashSet();
        // todo: more performant than HashSet?  perhaps just scan at end for dupes
        
        if (hasField(field)) {
            
            Relation.searchDataWithField(field, fieldValue, getRows(), results);
            
        } else {


            // todo performance: getCrossSchemaRelation is recomputing all sorts of
            // stuff each time that we could do faster if we unrolled in one place --
            // e.g., make it take a collection of row-data (all from the same schema, so
            // maybe actually hand it a schema), and it could handle it there.  Plus,
            // even if NO join's were found, it will still check all rows!

            //if (Relation.getCrossSchemaRelation(field, row.getData(), fieldValue) != null)

            // NOTE: WE RISK RECURSION HERE!  getCrossSchemaRelation calls getMatchingRows...
            // will that somehow auto-jump any length of schema relation joins?

            //-----------------------------------------------------------------------------
            // SHIT!  Do we actually need the full getCrossSchemaRelation checks (for Mediums case tho, right??
            // But don't we want to check straight Associations first?  This is overkill for those cases.
            //
            // THE CASE WE'RE MISSING IS THIS: (to handle IN getCrossSchemaRelation,
            // which should become a more generic getRelations) -- if the SEARCH field
            // (e.g., medium) is DIFFERENT than the field found in the Association,
            // only THEN do we need to attempt a join, otherwise, we can use a straight
            // Association.
            // 
            //-----------------------------------------------------------------------------

//             if (Association.hasAliases(this, field)) {
//             }
            
            if (Association.hasJoins(this, field)) {
                for (DataRow row : getRows()) {
                    if (Relation.getCrossSchemaRelation(field, row.getData(), fieldValue) != null)
                        results.add(row);
                }
            }
            
        }
        
        
        return results;
    }

    /** @param data: a MetaMap from another schema.  Heuristics will be used,
     * including looking at associations, to determine what should match.
     */
    public Collection<DataRow> getMatchingRows(MetaMap searchKeys)
    {
        if (DEBUG.Enabled) Log.debug("getMatchingRows: " + Util.tags(searchKeys));
        
        final Collection<DataRow> matching = new HashSet();
        // we use a HashSet to prevent duplicates, which could happen through
        // duplicate associations, or associations that are duped by an auto-join

        Relation.searchDataWithRow(searchKeys, getRows(), this, matching);

        return matching;
    }

//     public Collection<DataRow> getJoinedMatchingRows(Association join, Schema joiningSchema)
//     {
//         // should we even try this? something like the below: cut & pasted from DataAction
//         final Field indexKey = join.getFieldForSchema(dragSchema);
//         final String indexValue = onMapRowData.getString(join.getKeyForSchema(dropSchema)); // todo: multi-values

//         Log.debug("JOIN: indexKey=" + indexKey + "; indexValue=" + indexValue);

//         final Collection<DataRow> matchingRows = getMatchingRows(indexKey, indexValue);

//         Log.debug("found rows: " + Util.tags(matchingRows));

//         return matchingRows;
//     }
    
    
    public Collection<Field> getFields() {
        //return mFields.values();
        return mFieldList;
    }

    public Collection<Field> getXMLFields() {
        if (mXMLRestoreUnderway) {
            // return the list to be *loaded* by castor
            return mPersistFields;
        } else
            return getFields();
    }
    
    public int getFieldCount() {
        return getFields().size();
        //return mFields.size();
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
final class DataRow implements Relation.Scannable {

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
        

    /** add the given value, and track for analysis */
    void addValue(Field f, String value) {
        f.trackValue(takeValue(f, value));
    }
    
    /** add, but DO NOT track the value -- return the actual value added (which may have been trimmed or be Field.EMPTY_VALUE) */
    String takeValue(Field f, String value) {
        value = value.trim();
        
//         final String existing = values.put(f, value);
//         if (existing != null && Schema.DEBUG)
//             Log.debug("ROW SUB-KEY COLLISION " + f);
        //super.put(f.getName(), value);
        
        if (value.length() == 0)
            value = Field.EMPTY_VALUE;
        mmap.put(f.getName(), value);
        return value;
    }

    Iterable<Map.Entry> dataEntries() {
        return mmap.entries();
    }

    public String getValue(String key) {
        return mmap.getString(key);
    }
    
    /** interface Scannable */
    public Collection<String> getValues(String key) {
        return mmap.getValues(key);
    }
    
    public Collection<String> getValues(Field f) {
        return mmap.getValues(f.getName());
    }
    
    /** interface Scannable */
    public Schema getSchema() {
        return mmap.getSchema();
    }

    /** interface Scannable */
    public String getString(String key) {
        return mmap.getString(key);
    }

    /** interface Scannable */
    public boolean hasEntry(String key, CharSequence value) {
        return mmap.hasEntry(key, value);
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
