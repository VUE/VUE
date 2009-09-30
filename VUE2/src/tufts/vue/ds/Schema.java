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
 * @version $Revision: 1.52 $ / $Date: 2009-09-30 23:12:19 $ / $Author: sfraize $
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
    
    protected int mLongestFieldName = 10; // for debug

    private String mName;
    private Field mImageField;

    private LWComponent mStyleNode;

    private String GUID;
    private String DSGUID;

    private boolean isUserStyled;
    private boolean isDiscarded;

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
    
    private static Schema newAuthorityInstance(Resource r, String dsGUID) {

        final Schema s = new Schema();
        
        s.setResource(r);
        s.setDSGUID(dsGUID);
        s.setGUID(edu.tufts.vue.util.GUID.generate());
        //if (DEBUG.SCHEMA) Log.debug("INSTANCED SCHEMA " + s + "\n");
        if (DEBUG.SCHEMA) Log.debug(s + "; INSTANCED");

        registerAsAuthority(s);
        // would we want to wait to do registrations until we actually load the rows?
        
        if (DEBUG.SCHEMA) Log.debug(s + "; REGISTERED");
        
        if (DEBUG.SCHEMA) dumpAuthorities();
        
        return s;
    }

// TODO: something like this would probably make all sorts of stuff so much easier,
// tho we'd have to re-check tons of code.  Would help with nodes that have
// "discarded" schema references in the Undo queue or cut/paste buffer, as
// well as associations Fields (we'd need a similar scheme for Fields).
//     @Override public boolean equals(Object o) {
//         return o instanceof Schema && ((Schema)o).DSGUID.equals(DSGUID);
//     }


// We could establish fast similiarity based on the following rules:
// If the the FILE exists (Resource, but what if a network source?)
// Then each existing file gets an absolute unique ID, overriding
// DSGUID.  If the file no longer exists, then map missing files
// by found DSGUID's.  Tho, what if a user changes the file in a DSGUID,
// and then expects prior maps pointing to that DSGUID to use the
// new file?  So actually, DSGUID does neet to take priority.
    
//     public boolean same(Schema s) {
//         return s != null && s.mContentID == this.mContentID;
//     }

    public static Set<Map.Entry<String,Schema>> getAllByDSGUID() {
        return _SchemaByDSGUID.entrySet();
    }
    public static Set<Map.Entry<Resource,Schema>> getAllByResource() {
        return _SchemaByResource.entrySet();
    }

    private static synchronized void registerAsAuthority(Schema schema) {
        if (schema.DSGUID != null)
            registerByDSGUID(schema, schema.DSGUID);
        registerByResource(schema, schema.getResource());
    }

    private static synchronized void registerByDSGUID(Schema s, String dsGUID) {
        Schema prev = _SchemaByDSGUID.put(dsGUID, s);
        if (prev != null && prev != s) {
            Log.warn("REPLACING EXISTING DSGUID; orphaning: " + prev, new Throwable("HERE"));
        }
        if (prev == s)
            Log.info(s + "; was already AUTHORITATIVE for DSGUID " + dsGUID);
        else
            Log.info(s + "; recorded as AUTHORITATIVE for DSGUID " + dsGUID);
        if (s.getDSGUID() != dsGUID)
            Log.warn(""+s, new Throwable("INCONSISTENT DSGUID STATE; " + dsGUID));
    }
    
    private static synchronized void registerByResource(Schema s, Resource r) {
        Schema prev = _SchemaByResource.put(r, s);
        if (prev != null) {
            Log.warn("REPLACING EXISTING byRESOURCE; orphaning" + prev, new Throwable("HERE"));
        }
        if (prev == s)
            Log.info(s + "; was already AUTHORITATIVE for Resource: " + r);
        else
            Log.info(s + "; recorded as AUTHORITATIVE for Resource: " + r);
        if (s.getResource() != r)
            Log.warn(""+s, new Throwable("INCONSISTENT RESOURCE STATE " + r + " != " + s.getResource()));
    }


    private synchronized void trackUserStyles(Schema s) {
        if (!isUserStyled) {
            copyStyles(s);
            isUserStyled = true;
        }
    }
    
    private void copyStyles(Schema s) {
        if (s.mPersistFields.size() > 0) {
            if (DEBUG.SCHEMA) Log.debug("copyStyles; from persisted handles:"
                                        + "\n\tsource: " + s
                                        + "\n\ttarget: " + this);
            copyStyles(s.mPersistFields);
        } else {
            if (DEBUG.SCHEMA) Log.debug("copyStyles; from live Fields:"
                                        + "\n\tsource: " + s
                                        + "\n\ttarget: " + this);
            copyStyles(s.mFieldList);
        }
        setRowNodeStyle(s.getRowNodeStyle());
    }
    
    private void copyStyles(Collection<Field> fields) {
        for (Field src : fields) {
            Field target = getField(src.getName());
            if (target != null)
                target.setStyleNode(runtimeInitStyleNode(src.getStyleNode()));
        }
    }

    /** mark this Schema as discarded.
     * @param authority the authoritative Schema this one is being discard for -- if
     * it hasn't been user styled yet, it will be user-styled based on this Schema
     * (the one being discarded).
     */
    private synchronized void discardFor(Schema authority) {
        if (!isDiscarded) {
            if (DEBUG.SCHEMA) Log.info(this + ": DISCARDED");
            isDiscarded = true;
            authority.trackUserStyles(this);
        }
    }
    synchronized boolean isDiscarded() {
        return isDiscarded;
    }

    public synchronized static void dumpAuthorities() {
        Log.debug("Current VUE authoritative schemas by DSGUID:");
        //Util.dump(Schema.getAllByDSGUID());
        Util.dump(_SchemaByDSGUID, "BY-DSGUID");
        Log.debug("Current VUE authoritative schemas by RESOURCE:");
        //Util.dump(Schema.getAllByResource());
        Util.dump(_SchemaByResource, "BY-RESOURCE");
    }
    
    private static final boolean SCHEMA_AUTHORITY = true;
    private static final boolean SCHEMA_REFERENCE = false;

    public static Schema getInstance(Resource r, String dsGUID) {
        return getInstance(r, dsGUID, null, SCHEMA_REFERENCE);
    }
    
    public static Schema getNewAuthorityInstance(Resource r, String dsGUID, String displayName) {

        return getInstance(r, dsGUID, displayName, SCHEMA_AUTHORITY);
    }
    
    private static synchronized Schema getInstance
        (Resource r,
         String dsGUID,
         String displayName,
         boolean infoIsAuthority)
    {
        if (true) {
            return getInstanceAuthoritiesAreNew(r, dsGUID, displayName, infoIsAuthority);
        } else {
            return getInstanceAuthoritiesAreReloaded(r, dsGUID, displayName, infoIsAuthority);
        }
        
    }
    private static Schema getInstanceAuthoritiesAreNew
        (Resource r,
         String dsGUID,
         String displayName,
         boolean infoIsAuthority)
    {
        if (infoIsAuthority) {
            return newAuthorityInstance(r, dsGUID);
        } else {
            return lookup(dsGUID, r, displayName);
        }
        
    }
    
    /** If an existing schema exists matching the given data, return that, otherwise, a new schema instance */
    // Not ideal: it's safer to guarante a new fresh Schema instance here
    // every time we have authoritative information and to only copy FIELD
    // STYLE info over if there is an existing matching schema.
    private static Schema getInstanceAuthoritiesAreReloaded
        (Resource r,
         String dsGUID,
         String displayName,
         boolean infoIsAuthority)
    {
        Schema s = lookup(dsGUID, r, displayName);

        if (s == null) {
            return newAuthorityInstance(r, dsGUID);
        } else {
            
            if (infoIsAuthority) {

                // this is the codepath from XmlDataSource to load the actual DataSource schema, which has the
                // ultime authority info -- if we're keeping an existing schema, it's only for the styles.
                
                if (!r.equals(s.getResource())) {
                    Log.info("updating Resource in authority schema:"
                             + "\n\told: " + s.getResource()
                             + "\n\tnew: " + r);
                    s.setResource(r);
                }
                if (dsGUID != null && !dsGUID.equals(s.getDSGUID())) {
                    Log.info("updating DSGUID in authority schema", new Throwable("HERE"));
                    s.setDSGUID(dsGUID);
                }
                if (displayName != null && !displayName.equals(s.mName)) {
                    Log.info("updating Name in authority schema");
                    s.setName(displayName);
                }

                // re-register in case Resource or DSGUID has changed -- not this could allow
                // multiple Resource's / DSGUID's to map to the same Schema, which is fine.
                registerAsAuthority(s); 
            }
            
            return s;
        }
        
    }
    
    private static synchronized Schema lookup
        (final String dsGUID,
         final Resource r,
         final String name)
    {
        Schema s = _SchemaByDSGUID.get(dsGUID);
        if (s == null) {
            s = _SchemaByResource.get(r);
            if (s != null) {
                // actually, looking by Resource may not be correct: the same resource could
                // be used with a differet configuration (esp XML, tho could apply to CSV as well),
                // generating schema's that are actually different.
                if (DEBUG.SCHEMA) Log.debug("lookup: found by RESOURCE " + s + "; " + r);
            }
        } else {
            if (DEBUG.SCHEMA&&DEBUG.META) {
                Log.debug("lookup: found by DSGUID " + s);
//                 Log.debug("fetch: existing fields:");
//                 Util.dump(s.mFields);
            }
        }
        //if (DEBUG.SCHEMA) Log.debug("lookup: returning " + s);
        return s;
    }

    private static Schema lookup(Schema s)
    {
        synchronized (s) {
            return lookup(s.getDSGUID(), s.getResource(), s.getName());
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
        
//         if (schema.isLoaded())
//             return schema;

        final Schema authoritativeSchema = lookup(schema);

        if (authoritativeSchema != null && authoritativeSchema != schema) {
            if (schema.isLoaded() && DEBUG.Enabled) {
                Util.printStackTrace("replacing loaded schema " + schema + " with authority " + authoritativeSchema);
            }
            return authoritativeSchema;
        }
        else
            return schema;
        
    }

    
    /** interface {@link XMLUnmarshalListener} -- track us */
    public synchronized void XML_completed(Object context) {
        // As the resource isn't in the LWComponent hierarchy, it won't be updated in
        // the map.  TODO: too bad actually -- then it could make use of the
        // relative-to-map file path code -- would be a good idea to move these to the
        // map.
        ((tufts.vue.URLResource)getResource()).XML_completed("SCHEMA-MANUAL-INIT");
        
        this.DSGUID = getResource().getProperty("@DSGUID");

        for (Field field : mPersistFields) {
            field.setSchema(this);
            if (DEBUG.SCHEMA) Log.debug(String.format("seen field %s%-20s%s",
                                                      Util.TERM_YELLOW, field, Util.TERM_CLEAR));
        }
        
        // We can't do this now, at deserialize time, as we wont see any Schema's
        // that have yet to deserialize.
        // createAttachableAssociations(mPersistFields);
        
        mXMLRestoreUnderway = false;
        if (DEBUG.Enabled) Log.debug(this + " RESTORED; DSGUID=" + DSGUID + "\n");
    }

    /**
     * Any deserialized Schema's saved with the map that match any already loaded
     * authoritative Schema's will be replaced in the set of restored nodes, and and all
     * Field's found will be scanned for associations to be added.
     */
    public static synchronized void restoreSavedMapSchemas
        (final tufts.vue.LWMap restoredMap,
         final Collection<Schema> restoredSchemaHandles,
         final Collection<LWComponent> allRestored)
    {
        if (DEBUG.SCHEMA) {
            Log.debug("SCHEMA's in map " + restoredMap + ":");
            Util.dump(restoredSchemaHandles);
            Schema.dumpAuthorities();
        }

        // FIRST attempt all authority replacements:

        for (Schema schema : restoredSchemaHandles) {
            try {
                if (DEBUG.SCHEMA) Log.debug(schema + "; RESTORE AND REPLACE");
                schema.restoreFields();
                updateRestoredMapToAuthorities(schema, restoredMap, allRestored);
            } catch (Throwable t) {
                Log.error("error updating schema references for " + schema, t);
            }
        }

        // THEN scan for associations, so we can create associations based on all new authoritative references:
        
        for (Schema schema : restoredSchemaHandles) {
            try {
                //if (DEBUG.SCHEMA) Log.debug(schema + "; CREATE ASSOCIATIONS");
                createAttachableAssociations(schema, schema.mPersistFields); // note that Fields may have just been discarded
            } catch (Throwable t) {
                Log.error("error scanning for associations in " + schema, t);
            }
        }
    }

    private synchronized void restoreFields() {
        for (Field f : mPersistFields) {
            final LWComponent style = f.getStyleNode();
            if (DEBUG.SCHEMA) Log.debug(String.format("restore field %s%-23s%s style=%s",
                                                      Util.TERM_GREEN, f, Util.TERM_CLEAR, style));
            runtimeInitStyleNode(style);
            addField(f); // must do this to get all field names in the global multi-map for association lookups
        }
    }

    private static synchronized boolean updateRestoredMapToAuthorities
        (final Schema handle,
         final tufts.vue.LWMap restoredMap,
         final Collection<LWComponent> allRestored)
    {
        Schema existing = lookup(handle);
        if (existing != null) {
            if (DEBUG.Enabled) Log.debug("DUMPING THIS SCHEMA FOR EXISTING:\n\t  dumped: " + handle + "\n\texisting: " + existing);
            handle.discardFor(existing);
            replaceSchemaReferences(handle, existing, allRestored, restoredMap);
            
            // HOLY CRAP: we also need to update association Field references!
            // As currently an association can only be USED once the data-source is loaded,
            // lets just freakin NOT scan for associations until the actual data-source itself is loaded?
            // Or do we first want to see what it would take to actually turn associations on and off?

            // For assoc replacement keep it simple: be able to re-scan associations and create new ones
            // whenever a new authoritative schema is identified, and when added as a new association,
            // always scan and dump any associations that include references to orphaned schema's
            // (which will need to be marked as such).

            
            return true;
        } else
            return false;
    }
    
    private static synchronized void replaceSchemaReferences
        (final Schema oldSchema,
         final Schema newSchema,
         final Collection<LWComponent> nodes,
         final tufts.vue.LWMap sourceMap)
    {
        int updateCount = 0;

        //newSchema.setRowNodeStyle(oldSchema.getRowNodeStyle()); // ideally, only when the existing isn't already used anywhere

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

    // codepath: called in XmlDataSource anytime we load a new authoritative, "real" schema -- as such
    // couldn't we do this automatically?
    public static synchronized void reportNewAuthoritativeSchema
        (final Schema newAuthority,
         final Collection<tufts.vue.LWMap> maps)
    {
        tufts.vue.gui.GUI.invokeOnEDT(new Runnable() { public void run() {
            // safest to ensure all this happs on the AWT thread, so later association data
            // fetches (data search & filter actions) don't need synchronized read access
            makeSchemaReferencesAuthoritative(newAuthority, maps);
            Association.updateForNewAuthoritativeSchema(newAuthority);
        }});
    }
    
    /** find all schema handles in all nodes that match the new schema
     * and replace them with pointers to the live data schema */
    private static synchronized void makeSchemaReferencesAuthoritative
        (final Schema newAuthority,
         final Collection<tufts.vue.LWMap> maps)
    {
        if (DEBUG.Enabled) {
            Log.debug("makeSchemaReferencesAuthoritative; " + newAuthority + "; maps: " + maps);
            dumpAuthorities();
        }
        
        if (!newAuthority.isLoaded()) {
            Log.warn("newly loaded schema is empty: " + newAuthority, new Throwable("FYI"));
            return;
        }
        
        int scanCount = 0;
        int scanMapCount = 0;
        int updateCount = 0;
        int updateMapCount = 0;

        // todo: if this ever gets slow, could improve performance by pre-computing a
        // lookup map of all schema handles that map to the new schema (which will
        // usually contain only a single schema mapping) then we only have to check
        // every schema reference found against the pre-computed lookups instead of
        // doing a Schema.lookup against all loaded Schema's. E.g., we'd make a
        // a series of calls to replaceSchemaReferences.
        
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
                    scanCount++;
                    if (newSchema != curSchema) {
                        curSchema.discardFor(newSchema);
                        data.setSchema(newSchema);
                        updateCount++;
                        if (newSchema != newAuthority) {
                            Log.warn("out of date schema in " + c + "; oldSchema=" + curSchema + "; replaced with " + newSchema);
                        } else {
                            //if (DEBUG.SCHEMA) Log.debug("replaced schema handle in " + c);
                        }

                    }
                }
            }
            if (updateCount > countAtMapStart)
                updateMapCount++;
            scanMapCount++;
        }
        Log.info(String.format("scanned %d schema references in %d maps", scanCount, scanMapCount));
        Log.info(String.format("replaced %d schema references in %d maps", updateCount, updateMapCount));
    }
    

    private static synchronized void createAttachableAssociations(Schema schema, Collection<Field> restoredFields) {
        if (DEBUG.SCHEMA) Log.debug(schema + "; createAttachableAssociations");
        for (final Field field : restoredFields) {
            for (Field.PersistRef ref : field.getRelatedFieldRefs()) {
                if (DEBUG.SCHEMA) Log.debug(schema + "; persisted relation for " + field + ": " + ref);
                for (Field possibleMatch : AllFieldsByLowerName.get(ref.fieldName.toLowerCase())) {
                    final String guid = possibleMatch.getSchema().getGUID();
                    //Log.debug("Checking GUID " + guid);
                    if (ref.schemaGuid.equals(guid)) {
                        final Field match = possibleMatch;
                        if (match.getSchema().isDiscarded()) {
                            Log.debug("ignoring association match for discarded schema: " + match);
                        } else {
                            Log.debug("found live field to match ref: " + ref + " = " + Util.tags(match));
                            //tufts.vue.gui.GUI.invokeOnEDT(new Runnable() { public void run() {
                                // safest to ensure all this happs on the AWT thread, so later association data
                                // fetches (data search & filter actions) don't need synchronized read access
                                Association.add(field, match);
                                // }});
                        }
                    }
                }
            }
        }
    }
    
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
    
    private static LWComponent runtimeInitStyleNode(LWComponent style) {
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
        return style;
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

    private static synchronized String nextLocalID() {
        // must differentiate this from the the codes used for persisting nodes (just integer strings),
        // as castor apparently uses a global mapping for all object types, instead of an id reference
        // cache per-type (class).
        return String.format("S%d", NextLocalId.incrementAndGet());
    }

    /** for persistance */
    public final synchronized String getMapLocalID() {
        return mLocalID;
    }

    /** @return true if this Schema contains data.  Persisted Schema's are just references and do not contain data */
    public synchronized boolean isLoaded() {
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

    public synchronized void flushData() {
        if (DEBUG.Enabled) Log.debug("flushing " + this);
        mRows.clear();
        mLongestFieldName = 10; // for debug
        for (Field f : getFields()) {
            f.flushStats(); // flush data / enums, but keep any style
        }
    }

    public synchronized DataRow findRow(Field field, String value) {
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
            return String.format("Schema@%08x[%s/%s; #%dx%d %s%s k=%s%s]",
                                 System.identityHashCode(this),
                                 ""+mLocalID,
                                 //""+GUID,
                                 ""+DSGUID,
                                 mFields.size(),
                                 mRows.size(),
                                 Util.color(mName, Util.TERM_PURPLE),
                                 isDiscarded ? Util.color(" DISCARDED", Util.TERM_RED) : "",
                                 Relation.quoteKey(mKeyField),
                                 extra);
        } catch (Throwable t) {
            t.printStackTrace();
            return "Schema[" + t + "]";
        }
    }

    public synchronized String getGUID() {
        return GUID;
    }

    public synchronized void setGUID(String s) {
        GUID = s;
    }

    synchronized void setDSGUID(String s) {
        //if (DEBUG.Enabled) Util.printStackTrace("setDSGUID [" + s + "]");
        if (DEBUG.SCHEMA) Log.debug(this + "; setDSGUID: " + s);
        DSGUID = s;
        getResource().setProperty("@DSGUID", s);
//         if (DSGUID != null)
//             registerByDSGUID(this, DSGUID);
    }
    
    synchronized String getDSGUID() {
        return DSGUID;
    }
    
    /** set the node style object used for record/row nodes */
    public synchronized void setRowNodeStyle(LWComponent style) {
        if (DEBUG.SCHEMA) Log.debug("setRowNodeStyle: " + style);
        runtimeInitStyleNode(style);
        mStyleNode = style;
    }
        
    /** @return the node style used for record/row nodes */
    public synchronized LWComponent getRowNodeStyle() {
        return mStyleNode;
    }
    
    public synchronized Field getKeyField() {
        if (mKeyField == null)
            mKeyField = getKeyFieldGuess();
        return mKeyField;
    }

    public synchronized String getKeyFieldName() {
        return getKeyField().getName();
    }
    
    public synchronized void setKeyField(Field f) {
        //Log.debug("setKeyField " + Util.tags(f));
        mKeyField = f;
    }
    
    public synchronized void setKeyField(String name) {
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

    public synchronized void setResource(Resource r) {
        Log.debug(this + "; setResource " + r);
        mResource = r;
    }
    public synchronized Resource getResource() {
        return mResource;
    }
    
    public synchronized int getRowCount() {
        return mRows.size();
    }

    public synchronized void setName(String name) {
        mName = name;
    }

    public synchronized Field getField(String name) {
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
    public synchronized Field findField(String name) {
        Field f = mFields.get(name);
        if (f == null && name != null && name.length() > 0 && Character.isUpperCase(name.charAt(0))) {
            f = mFields.get(name.toLowerCase());
            if (DEBUG.SCHEMA && f != null) Log.debug("found lowerized [" + name + "]");
        }
        return f;
    }

    // todo: doesn't use same case indepence as findField
    public synchronized boolean hasField(String name) {
        return mFields.containsKey(name);
    }

    // todo: doesn't use same case indepence as findField
    public synchronized boolean hasField(Field f) {
        // todo: more performant (keep a set of just hashed Fields)
        // could just check f.getSchema() == this, tho that may not work at init time?
        return mFields.containsKey(f.getName());
    }
    
    
    public synchronized String getName() {

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

    public synchronized void ensureFields(String[] names) {

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
    
    public synchronized void ensureFields(int count) {
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
    public synchronized Vector<String> getPossibleKeyFieldNames() {
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
    public synchronized Vector<String> getFieldNames() {
        final Vector<String> names = new Vector();

        for (Field field : getFields())
            names.add(field.getName());

        return names;
    }
    
    
    
    /** look at all the Fields and make a guess as to which is the most likely key field
     * This currently will always return *some* field, even if it's not a possible key field. */
    public synchronized Field getKeyFieldGuess() {

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
    synchronized void notifyAllRowsAdded() {
        
        for (Field f : Util.toArray(getFields(), Field.class)) { // must dupe as will may be adding new fields (quartiles)
            if (DEBUG.Enabled) Log.debug("field analysis " + Relation.quoteKey(f.getName()) + ": type " + Util.tags(f.getType()));
            f.setStyleNode(DataAction.makeStyleNode(f));
            try {
                f.performFinalAnalysis();
            } catch (Throwable t) {
                Log.error("analysis failed on " + f, t);
            }
        }
    }

    public synchronized List<DataRow> getRows() {
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
    
    
    public synchronized Collection<Field> getFields() {
        //return mFields.values();
        return mFieldList;
    }

    public synchronized Collection<Field> getXMLFields() {
        if (mXMLRestoreUnderway) {
            // return the list to be *loaded* by castor
            return mPersistFields;
        } else
            return getFields();
    }
    
    public synchronized int getFieldCount() {
        return getFields().size();
        //return mFields.size();
    }

    /** if a singleton exists with the given name, return it's value, otherwise null */
    public synchronized String getSingletonValue(String name) {
        Field f = mFields.get(name);
        if (f != null && f.isSingleton())
            return f.getValueSet().iterator().next();
        else
            return null;
    }
    

    public synchronized boolean isXMLKeyFold() {
        return mKeyFold;
    }
    public synchronized void setXMLKeyFold(boolean keyFold) {
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
