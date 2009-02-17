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


package tufts.vue;

import tufts.Util;

import tufts.vue.ds.Schema;

import java.util.*;
import java.lang.ref.Reference;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Iterators;


/**
 * A general key/value map for storing values: e.g., meta-data.
 *
 * Uses an underlying Multimap impl, so multiple values for the same key are supported.
 *
 * The insertion order of each key/value is preserved, even for each use of
 * the same key with different values.
 *
 * @version $Revision: 1.6 $ / $Date: 2009-02-17 02:46:24 $ / $Author: sfraize $
 */

public class MetaMap implements TableBag, XMLUnmarshalListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaMap.class);

    // Note: LinkedHashMultimap preserves the order of all "KEY+VALUE" additions, not just the KEY --
    // this is good for preserving semantics of incoming data, as the order is often good information
    // (especially if the source is XML data).  We could potentially hack this impl to even preserve
    // some kind of decoration that allows duplicate key-value pairs (good for XML again).
    // e.g.: wrap the value in something that disguises it.
    
    // Performance: allow only creating a collection when the second value for
    // a given key is added.  Would need to either hack google collections for this,
    // or do as part of our own complete re-implementation.
    
    /** the backing Multimap: deliberately un-typed */
    private final Multimap mData = Multimaps.newLinkedHashMultimap();

    private volatile MapModel mTableModel;
    private boolean mHoldingChanges = false;
    private int mChanges;
    private List<TableBag.Listener> listeners;

    private static final Map<String,Key> KEY_MAP = new java.util.concurrent.ConcurrentHashMap();

    private static final boolean TEST_MODE = false;

    private Schema mSchema;
    
    public MetaMap() {}

    public void setSchema(Schema schema) {
        mSchema = schema;
    }
    
    public Schema getSchema() {
        //Log.debug("getSchema returns " + mSchema);
        return mSchema;
    }
    
//     public int getSchemaID() {
//         if (mSchema == null)
//             return 0;
//         else
//             return mSchema.getMapLocalID();
//     }


    @Override
    public boolean equals(Object o) {
        if (o instanceof MetaMap == false)
            return false;
        return mData.equals( ((MetaMap)o).mData );
    }
    
    /**
     *
     * A decorator for String that will be object equivalent with other Key String decorators
     * regardless of case.  This permits *preserving* case within a given key, while ignoring it
     * for hash/equals.
     *
     * We could support object equivalence with actual Strings, but the deep down underlying
     * java.util.HashMap these keys are ultimately destined for doesn't do key compairson in the
     * order that would support that (e.g., we'd need Key.equals(String), it does
     * String.equals(Object))
     *
     */
    
    private static final class Key {
        final String name;
        final int hash;

        private Key(String s) {
            name = s;
            hash = name.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            //Log.debug(String.format("testing [%s]=[%s]", Util.tags(this), Util.tags(o)));

            if (o == this)
                return true;
            
            if (o.getClass() != Key.class) {
                Log.warn("IS NOT KEY: " + Util.tags(o));
                return false;
            }
            
            if (TEST_MODE && DEBUG.Enabled) {
                final boolean eq = name.equalsIgnoreCase( ((Key)o).name );
                if (eq && !name.equals(o.toString()))
                    Log.debug(String.format("matched mixed-case [%s] to [%s]", name, o));
                return eq;
            } else {
                return name.equalsIgnoreCase( ((Key)o).name );
            }
            
        }
        
        @Override
        public int hashCode() {
            return hash;
        }
        @Override
        public String toString() {
            return name;
        }

//         private static Object instance(String s) {
//             // old style impl: regular Strings as keys: all MetaMap.Key code ignored
//             return s;
//         }
        
        private static Key instance(String s) {
            
            // case independent key impl: all Key's cached and only
            // Key objects used in the Multimap, and all lookups require
            // an additional hash lookup for a Key object, or the creation
            // of a new one.

            final Key existing = KEY_MAP.get(s);
            if (existing != null)
                return existing;
            
            final Key newKey = new Key(s);
            KEY_MAP.put(s, newKey);

            return newKey;
        }
    }
    
    /** add the given key/value pair */
    public synchronized void put(final String key, final Object value)
    {
        final Object internalKey = Key.instance(key);
        
        if (DEBUG.DATA) Log.debug("put " + internalKey + " " + Util.tags(value));
        
        if (mData.put(internalKey, value))
            markChange();
    }

    /** will ensure the given key has ONLY the given value: any other values present will be removed */
    public synchronized void set(final String key, final Object value)
    {
        final Key internalKey = Key.instance(key);

        //mData.replaceValues(internalKey, Util.iterable(value));
        
        final Collection bag = mData.get(internalKey);

        if (bag.isEmpty()) {
            bag.add(value);
        } else {
            if (DEBUG.DATA)
                Log.debug("replacing " + internalKey + " " + Util.tags(bag) + " with " + Util.tags(value));
                      //+ "; got=" + Util.tags(getFirst(internalKey)));
            bag.clear();
            bag.add(value);
        }

        markChange();
    }
    
    /** Will add the String values of all key/value pairs */
    public synchronized void putAllStrings(final Iterable<Map.Entry> entries) {
        final boolean onHold = mHoldingChanges;
        if (!onHold)
            holdChanges();
        for (Map.Entry e : entries)
            put(e.getKey().toString(), e.getValue().toString());
        if (!onHold)
            releaseChanges();
    }

    
    /** backward compat for now: same as put */
    public void add(final String key, final Object value) {
        put(key, value);
    }

    /** @return true if the given value is considered "empty", e.g., a null,
     * 0 length string, or zero size collection */
    private static boolean isEmpty(Object v) {
        if (v == null)
            return true;
        else if (v.getClass() == String.class && v.toString().length() == 0)
            return true;
        else if (v instanceof Collection && ((Collection)v).size() == 0)
            return true;
        else
            return false;
    }
    
    /** add the given key/value, only if the value is considered "non-empty"
     * Empty values currently include null, zero length Strings, and empty Collections */
    public void putNonEmpty(String key, Object value) {
        if (!isEmpty(value))
            put(key, value);
        //else if (DEBUG.DATA) Log.debug("EMPTY: " + Util.tags(value));
    }

    /** set the given key/value, only if the value is considered "non-empty" */
    public void setNonEmpty(String key, Object value) {
        if (!isEmpty(value))
            set(key, value);
        //else if (DEBUG.DATA) Log.debug("EMPTY: " + Util.tags(value));
    }
    
    /** remove ALL values having the given key */
    public synchronized Object remove(Object key) {

        final Object prior = mData.removeAll(key);

        if (prior != null)
            markChange();

        return prior;
    }

    /** @return the first value found for the given key */
    public synchronized Object getFirst(String key) {
        return getFirst(Key.instance(key));
    }
    
    private synchronized Object getFirst(Key key) {
        return extractFirstValue(mData.get(key));
    }

    private static Object extractFirstValue(Object o) {
        if (o instanceof Collection) {
            // in practice, this is the case that always occurrs with the current google
            // collections impl
            
            // todo performance: this is a terrible waste much of the time.  When there
            // are no repeat values for a given key, we must construct and run an
            // iterator just to extract the single value.  Hope for an optimized version
            // of Multimaps from google that automatically handle this. SMF 2008-12-03
            
            //Log.debug("extracting from Collection "  + Util.tags(o));
            final Collection bag = (Collection) o;
            if (bag.isEmpty())
                return null;
            else
                return bag.iterator().next();
        }
        else if (o instanceof List) {
            //Log.debug("extracting from List "  + Util.tags(o));
            final List list = (List) o;
            return list.isEmpty() ? null : list.get(0);
        }
        else
            return o;
    }

    public Object get(String key) {
        return getFirst(key);
    }
    
    
    /** @return the property value for the given key, dereferened if an instanceof java.lang.ref.Reference is found */
    public Object getValue(String key) {

        Object o = getFirst(key);
            
        if (o instanceof Reference) {
            o = ((Reference)o).get();
            if (o == null) {
                if (DEBUG.Enabled) Log.debug("value was GC'd for key: " + key);
                //mData.remove(Key.instance(key), o); // don't need to see it again
            }
        }
        
        return o;
    }

    /** @return first value found for key as a string */
    public String getFirstString(String key) {
        final Object o = getFirst(key);
        return o == null ? null : o.toString();
    }
    
    /** @return first value found for key as a string: current impl identical to getFirstString */
    // if there are multiple values for the given key, this someday could return a string
    // that concatenates all the values with a delimiter
    public String getString(String key) {
        return getFirstString(key);
    }

    // TODO: make return types (Object v.s String) more consistent, or more clear as to result
    
//     public String getAny(String... keys) {
//         // todo: could also handle some case hacking until we support case independent keys
//         for (String k : keys) {
//             final String value = getFirstValue(k);
//             if (value != null)
//                 return value;
//         }
//         return null;
//     }
    

    /** TableBag impl */
    public int size() {
        return mData.size();
    }
    
    public boolean containsKey(String key) {
        // can we optimize if the key-cache finds to key at all to look up?
        // I think only if we also case-fold the key-cache.
        return mData.containsKey(Key.instance(key));
        //return mData.containsKey(key);
    }
    public boolean containsEntry(String key, String value) {
        return mData.containsEntry(Key.instance(key), value);
        //return mData.containsEntry(key, value);
    }
    
//     public Set keySet() {
//         return mData.keySet();
//     }
    
//     public Map<String,?> asMap() {
//         return mData.asMap();
//     }

//     /** @return a set of entries that presents a flattened view of all key-value pairs */
//     public Collection<Map.Entry<String,Object>> entrySet() {
//         //return mData.entries();

//         // here we need the a flattening Collection forwarder, or
//         // replace entrySet with a special flattening iterator
//         //return mData.entrySet();
//     }
    
//     private Collection<Map.Entry<String,Collection<?>>> collectionEntrySet() {
//         return mData.asMap().entrySet();
//     }
    
    /** @return a flat collection of key-value pairs -- modifications to the collection will modify the map */
    //public Collection<Map.Entry<Key,Object>> entries() {
    public Collection<Map.Entry> entries() {
        return mData.entries();
    }
    
    public Collection values() {
        return mData.values();
    }

    private Collection<Map.Entry<String,Object>> mPersistEntries;
    
    /** for castor persistance of key/value pairs -- returns mappable PropertyEntry instances */
    //public Iterator<Map.Entry<String,Object>> iterateEntries() // could try castor iterate/add patttern
    public Collection<Map.Entry<String,Object>> getPersistEntries() {
        if (mPersistEntries == null)
            mPersistEntries = new ArrayList();
        
        if (mData.size() > 0) {
            mPersistEntries.clear();
            if (DEBUG.XML) Log.debug("LOADING ENTRIES n=" + mData.size() + "; in " + Util.tag(this));

            // NOTE: at runtime, we sometimes put the actual object in the property map
            // E.g., URLResource will put a URI in the Resource properties for
            // @file.relative -- which has worked for us as it persists the toString()
            // version.  But on deserialization, a Multimap will start with the String
            // only version of @file.relative, and then add the object version,
            // resulting in the value appearing in there twice (both are normally hidden
            // except for debug tho).  But when saved again, @file.relative will then
            // appear in the save file twice -- once for the original String, once for
            // the current runtime URI, stringified at persist time.  This is okay -- on
            // deserialization again, they'll both be encoundered as Strings again and
            // be combined into a single entry.  The one problem this could potentially
            // cause is if our code were to ever rely on finding the original URI object
            // in the resource properties.  This currently isn't done -- we only look for
            // String values, and the original object is in there only for debugging.
            // Just be aware of this.
            //
            // [ update: Resource setProperty now uses MetaMap.set v.s. MetaMap.put,
            // so repeats should no longer be possible ]
            
            for (Map.Entry e : entries())
                mPersistEntries.add(new PropertyEntry(e));
        }
        
        return mPersistEntries;
    }

    public void XML_initialized(Object context) {}
    
    public void XML_completed(Object context) {
        if (DEBUG.XML) Log.debug("UNPACKING ENTRIES IN " + this + " context=" + context);
        for (Map.Entry<String,?> e : mPersistEntries) {
            if (DEBUG.XML) Log.debug("UNPACKING " + e);
            
            // NOTE: We could use String.intern() for the keys.  This is a debatable
            // performance issue -- use of intern() will slow down all future String
            // creations.  But if we don't do this or something like it, every key
            // object is a unique String instance after deserializing, which can be a
            // ton of needless objects when spread across hundreds of meta-maps, each
            // with dozens of keys.  A beter impl would probably be to have a static
            // MetaMap internal global key hash.

            // put(e.getKey().intern(), e.getValue());
            
            put(e.getKey(), e.getValue());
        }
        if (DEBUG.XML) Log.debug("UNPACKED " + this);
    }
    
    public void XML_fieldAdded(Object context, String name, Object child) {}
    public void XML_addNotify(Object context, String name, Object parent) {}

    
//     public Iterable<Map.Entry<String,Object>> entries() {
//         return mData.entries();
//     }
    
//     public Util.Itering<Map.Entry<String,?>> entries() {
//         return new FlatteningIterator(mData);
//     }
    
    private static Util.Itering<Map.Entry<String,?>> entries(Map<String,Collection> map) {
        return new FlatteningIterator(map);
    }

    /** Flatten's a Map who's values are collections: returns a key/value for each value in each collection */
    private static final class FlatteningIterator<K,V> extends Util.AbstractItering<Map.Entry<K,V>> {

        final Iterator<Map.Entry<Object,Collection>> keyIterator;
        
        /** this object returned as the result every time */
        final KVEntry entry = new KVEntry();
        
        Iterator valueIterator;

        public FlatteningIterator(Map<Object,Collection> map) {
            keyIterator = map.entrySet().iterator();
            if (keyIterator.hasNext()) {
                findValueIteratorAndKey();
            } else {
                valueIterator = Iterators.emptyIterator();
            }
        }

        void findValueIteratorAndKey() {
            final Map.Entry e = keyIterator.next();
            entry.key = e.getKey();
            Collection collection = (Collection) e.getValue();
            valueIterator = collection.iterator();
        }

        public boolean hasNext() {
            return valueIterator.hasNext() || keyIterator.hasNext();
        }

        /** note: current impl will always return the same Map.Entry object */
        public Map.Entry next() {
            if (!hasNext()) throw new NoSuchElementException();
            
            if (!valueIterator.hasNext())
                findValueIteratorAndKey();

            entry.value = valueIterator.next();
            
            return entry;
        }
    }

    private void markChange() {
        if (mHoldingChanges)
            mChanges++;
        else if (mTableModel != null)
            mTableModel.reload();
    }
    
    /** No listeners will be updated until releaseChanges is called.  Multiple
     * overlapping holds are okay. */
    public synchronized void holdChanges() {
        if (DEBUG.RESOURCE && DEBUG.META) out("holding changes");
        mHoldingChanges = true;
    }

    /**
     * If any changes made since holdChanges, listeners will be notified.
     * It is crucial this is called at some point after holdChanges,
     * or listeners may never get updated. Tho ideally only one call to
     * this per holdChanges is made, Extra calls to this are okay 
     * -- it will at worst degrade update performance with rapid updates.
     */
    public synchronized void releaseChanges() {
        mHoldingChanges = false;
        if (mChanges > 0 && mTableModel != null) {
            if (DEBUG.RESOURCE && DEBUG.IMAGE) out("releasing changes " + mChanges);
            mTableModel.reload();
        }
        if (DEBUG.RESOURCE && DEBUG.IMAGE) out("released changes " + mChanges);
        mChanges = 0;
    }

    public synchronized java.util.Properties asProperties() {
        final java.util.Properties props = new java.util.Properties();

        for (Map.Entry e : entries())
            props.put(e.getKey().toString(), e.getValue().toString());

        return props;
    }


    /* Do NOT synchronize getTableModel with the rest of the methods: this is because
     * when listeners get notified, one of the first things they're likely to do is ask
     * for the table model, but if two different threads are active on the other end,
     * we'll dead-lock.  (E.g., an ImageLoader thread and the AWT thread (via a user
     * LWSelection change) have both changed the current resource selection, and so the
     * meta-data pane is doing two synchronized updates (one from each thread) back to
     * back, but during one of the updates, it has to call back into the PropertyMap
     * here, which may already be locked on one of the threads, because it was from
     * there that a propertyMapChange was called.
     *
     * Eventually, the table model will probably want to move to the GUI and we can
     * avoid this special case.
     */

    // todo: this doesn't need to be so complicated as getTableModel()
    // anymore: could replace with a getEntries() that returns a new
    // array of all entries, fully iterable in a thread-safe manner,
    // and non-modifiable.
    
    public javax.swing.table.TableModel getTableModel() {
        // mTableModel is volatile, so as of Java 1.5, the double-checked locking
        // idiom used here should work.
        if (mTableModel == null) {
            synchronized (mData) { // sync on anything other than "this" just in case
                if (mTableModel == null)
                    mTableModel = new MapModel();
            }
        }
        return mTableModel;
    }

    public synchronized void addListener(TableBag.Listener l) {
        if (listeners == null)
            listeners = new java.util.ArrayList();
        listeners.add(l);
    }
    public synchronized void removeListener(TableBag.Listener l) {
            
        // Note: we can DEADLOCK here in AWT obtaining the method entry lock if this
        // MetaMap instance is already locked by an ImageLoader thread.
            
        if (listeners != null)
            listeners.remove(l);
    }

    private static int notifyCount = 0;
    
    public synchronized void notifyListeners()
    {
        if (listeners != null && listeners.size() > 0) {
            notifyCount++;
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifyListeners " + listeners.size() + " of " + super.toString());
            Iterator i = listeners.iterator();
            while (i.hasNext()) {
                Listener l = (Listener) i.next();
                if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifying: " + tufts.vue.gui.GUI.namex(l));
                
                // Note: we can DEADLOCK in MetaDataPane.tableBagChanged (if it's
                // synchronized) waiting against AWT which is already locking
                // MetaDataPane, and waiting to lock this object (locked by an
                // ImageLoader thread) to enter removeListener above.
                
                l.tableBagChanged(this); 
            }
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifyListeners completed " + listeners.size());
        }
    }

    private void out(Object o) {

        Log.debug(String.format("@%x (#%d): %s",
                                System.identityHashCode(this),
                                notifyCount,
                                (o==null?"null":o.toString())));
    }

    @Override
    public String toString() {
        return String.format("MetaMap@%x[n=%d]", System.identityHashCode(this), size());
    }

    @Override
    public MetaMap clone() {
        final MetaMap clone = new MetaMap();
        clone.mData.putAll(mData);
        clone.mSchema = mSchema;
        return clone;
    }
    
    private class MapModel extends javax.swing.table.AbstractTableModel {

        // TODO: we can get get rid of this now -- we're not doing the sort any more, as
        // the order is being preserved in the data-map itself.  The MetaDataPane viewer
        // can provide differently sorted views on its own if it likes.  HOWEVER, we may
        // still want to keep some delegate like this to prevent concurrent mod
        // exceptions while iterating in the MetaDataPane in case of changes while
        // loading (e.g., additional image data has arrived) We may even want to leave
        // the table model in case we ever want to show this in a table,

        private final ArrayList<Map.Entry> mEntries = new ArrayList();
        
        MapModel() {
            if (DEBUG.RESOURCE) out("new " + getClass());
            reload();
        }

        private void reload() {

            mEntries.clear();
            mEntries.addAll(MetaMap.this.entries());
            if (DEBUG.RESOURCE) out("MapModel reload " + mEntries.size() + " items");
            
//             final int totalRows = MetaMap.this.size(); // one row per key: includes repeated key's
//             if (mEntries == null || mEntries.length != totalRows)
//                 mEntries = new KVEntry[totalRows];
//             for (Map.Entry<String,?> e : MetaMap.this.entries())
//                 mEntries[ei++] = new KVEntry(e);
// //             Arrays.sort(mEntries, new Comparator() {
// //                     public int compare(Object o1, Object o2) {
// //                         String k1 = (String) ((Map.Entry)o1).getKey();
// //                         String k2 = (String) ((Map.Entry)o2).getKey();
// //                         return k1.compareTo(k2);
// //                     }});
                            
            if (DEBUG.RESOURCE) {
                out("loaded " + mEntries.size() + " entries");
                if (DEBUG.META)
                    out("model loaded " + mEntries);
            }
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("fireTableDataChanged...");
            fireTableDataChanged();
            MetaMap.this.notifyListeners();
        }
        
        public int getRowCount() {
            return mEntries.size();
        }
        
        public Object getValueAt(int row, int col) {
//             if (row >= mEntries.size()) {
//                 Util.printStackTrace(getClass() + " has only " + mEntries.size() + " entries, attempt to access row " + row);
//                 return "<empty>";
//             }

            final Map.Entry entry = mEntries.get(row);

            if (entry == null) {
                Log.warn(getClass().getName(), new Throwable("FYI: null entry at row " + row + "; col-request=" + col));
                return null;
            } else if (col == 0)
                return entry.getKey();
                //return String.format("%s@%x", entry.getKey(), System.identityHashCode(entry.getKey()));
            else
                return entry.getValue();
        }

        public int getColumnCount() { return 2; }

        public String getColumnName(int col) {
            //return col == 0 ? "Field" : "Value";
            return null;
        }

        public Class getColumnClass(int colIndex) {
            return colIndex == 0 ? String.class : Object.class;
        }

        
        
    }
}

// TODO: allow specifing a prefix, and maybe some special handling for
// nesting MetaMap's hierarchically.  Would still need a flattened
// outer decoration of all the contained property maps, with combined
// dotted keys.  This would be perfect for mapping XML data.

// TODO: handle case-independence in keys.  An easy impl would create a ton of key
// objects instead of using String's as keys.  An ideal impl would provavly involve
// hacking google collections to allow the use of an underlying case-independent
// hash-set impl (to be written) instead of java.util.HashSet

// What if we had an introspecting PropertyMap impl?  (or Map impl, or Multimap impl, or
// whatever) could specify which fields you want bound to what to use just a sub-set
// (e.g., LWComponent properties) Or in meantime, could just make use of our existing
// LWComponent.Key class to make it even easier.  Might NOT want that to just be called
// the "node." sub-set: maybe something more semantic like "ui." or "display."

// TODO: this type of map should not be used for internal runtime properties that want
// to be set and re-set, for possibly including SoftReference'd properties, etc -- to
// switch to this impl, would need to add a class similar to old PropertyMap (w/out the
// addProperty / unique key stuff, e.g., a DataMap), but with the special Reference
// handling, Class property handling, etc.  Ideally have it impl a ClientData interface
// that any object can impl to use to pass through to the DataMap (class Resouce and
// class LWComponent could even subclass this, as their currently top level objects)

// TODO: may want to create this as a generic with Object as key (and maybe even value
// as Object v.s. a Collection, or some wrapper class that handles
// collection/singleton/meta-map), and handle in that any hierarchy encapsulation we end
// up wanting: e.g., a value could actually be another meta-map, and the flatting
// iterator could build up the key each time it descends.  Would make for very noisy
// persistance (fat keys) when flattened then serialized, tho would ideally create a
// specialized castor mapping that would beautifully handle the nesting, (hopefully
// castor's limitiations wouldn't get in our way).

// Don't forget that the main reason to using hashing collections for all of this is not
// for presenting a sorted order (that could be produced once), but to provide fast
// hashed lookups / searches / filters - tho currently all our searches are value based,
// we'll be adding keyword based searches.  (e.g. styling)  E.g., a TreeMap is not
// only a map that maintains sorted order, but uses the red-black tree as a hashing
// looked mechanism based on the searched for key.  Still, technically, I suppose
// if you know a "done/loaded" time for your collection, at which you could perform
// your sort once, you could use a LinkedHashMap all the way through, just
// performing the sort on it once (or a TreeMap to start, switching over to LinkedHashMap
// later)   Has anyone created a crazy run-time-polymorphic collection set such as this?

// *IDEALLY*, the end-all-be-all version, would actually maintain insertion order over
// time, as for repository meta-data this could be semantically relevant, but then be
// SWAPPABLE betwee that and sorted at any given time (e.g., a LinkedHashTreeMap).  Tho
// this may finally be an argument for just going ahead and use a LinkedHashMultiMap,
// which preserves everything, and then do any desired sorting at the UI level.  <*>
// This could be achieved simply by a linkedhashmap/set that provides a sorted view
// fetcher / sorted iterator fetcher, which caches the result (soft-reference even),
// and flushes cache should the collection be modified.

// Note: HashMap double-hashes: so must use IdentityHashMap if want to truly
// take advantage of identity hashing.  Could use String.intern() for
// creating all keys, tho apparenly this is intern() is quite slow --
// better off doing on your own.  This is a place a Schema could come
// in handy, providing unique Key objects.


