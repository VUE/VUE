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

package tufts.vue.ds;

import tufts.vue.DEBUG;
import tufts.Util;
import tufts.vue.EventHandler;
import static tufts.vue.ds.Relation.*;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * A list of relationships between Fields.  Each association names two Field's,
 * which must be from different Schema's.  How VUE makes meaning of these associations
 * is generally determined elsewhere, with the exeption of an Association between
 * the key field of two Schema's, which is considered to be a join in the classic
 * database sense.
 *
 * @version $Revision: 1.16 $ / $Date: 2010-02-03 19:13:16 $ / $Author: mike $
 * @author Scott Fraize
 */

// bug: associations are not persisting unless at least one node using the association is on the map
public final class Association
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Association.class);
    
    private static final Multimap<Field,Association> AllByField = Multimaps.newHashMultimap();
    private static final List<Association> AllPairsList = new ArrayList();
        
    final Field field1;
    final Field field2;
    final Schema schema1;
    final Schema schema2;
    
    boolean enabled;
    public static final class Event {
        public static final Object ADDED = "added";
        public static final Object REMOVED = "removed";
        public static final Object CHANGED = "changed";
        
        final Object id;
        final Association a;
        private Event(Association a, Object id) {
            this.a = a;
            this.id = id;
        }
        @Override public String toString() {
            return String.format("%s[%s%s]", Util.tag(this), id, a == null ? "" : (" " + a));
        }
    }
    public interface Listener extends EventHandler.Listener<Event> {}

    private static final EventHandler<Event> EventSource = EventHandler.getHandler(Event.class);

    private Association(Field f1, Field f2, boolean isOn) {
        if (f1 == f2)
            throw new IllegalArgumentException("field can't associate to itself: " + f1);
        if (f1 == null || f2 == null)
            throw new IllegalArgumentException("null field: " + f1 + "; " + f2);
        field1 = f1;
        field2 = f2;
        schema1 = field1.getSchema();
        schema2 = field2.getSchema();
        if (DEBUG.SCHEMA)
            Log.debug("instancing:"
                      + "\n\tfield 1: " + quoteKey(f1) + " " + Util.tags(f1) + " " + schema1
                      + "\n\tfield 2: " + quoteKey(f2) + " " + Util.tags(f2) + " " + schema2);
        enabled = isOn;
        if (schema1 == schema2)
            throw new IllegalArgumentException("can't associate fields from the same schema: " + f1 + "; " + f2 + "; " + schema1);
        
    }

    private static final int INDEX_DEFAULT = -1;

    /** add a global association between the given two fields */
    public static void add(Field f1, Field f2) {

        Association a = addImpl(f1, f2, INDEX_DEFAULT);
        
        if (a != null)
            EventSource.raise(Association.class, new Event(a, Event.ADDED));
    }

    private static synchronized Association addImpl(Field f1, Field f2, int index) {

        boolean alreadyExists = false;
        for (Association scan : AllPairsList) {
            if ((scan.field1 == f1 && scan.field2 == f2) ||
                (scan.field1 == f2 && scan.field2 == f1)) {
                alreadyExists = true;
                if (DEBUG.SCHEMA) Log.debug("an association already exists between " + f1 + " and " + f2);
                break;
            }
        }
        
        if (alreadyExists) {
            return null;
        } else {
            Association a = new Association(f1, f2, true);
            if (DEBUG.SCHEMA) Log.debug("ADDING " + a + " at index " + index);
            AllByField.put(f1, a);
            AllByField.put(f2, a);
            if (index >= 0)
                AllPairsList.add(index, a);
            else
                AllPairsList.add(a);
            if (DEBUG.SCHEMA) {
                Log.debug("all associations:");
                Util.dump(AllPairsList);
            }
            return a;
        }
    }
    
    public static void remove(Association a) {
        removeImpl(a);
        EventSource.raise(Association.class, new Event(a, Event.REMOVED));
    }
    
    private static synchronized void removeImpl(Association a) {
        if (DEBUG.SCHEMA) Log.debug("REMOVE " + a);
        AllPairsList.remove(a);
        AllByField.remove(a.getLeft(), a);
        AllByField.remove(a.getRight(), a);
    }
    
    static void updateForNewAuthoritativeSchema(final Schema newAuthority)
    {
        if (verifyAll(newAuthority))
            EventSource.raise(Association.class, new Event(null, Event.CHANGED));
    }

    private static synchronized boolean verifyAll(final Schema newAuthority)
    {
        boolean changed = false;
        for (Association a : Util.copy(AllPairsList)) { // list may change during iteration
            
            // Technically, we should only really need to look for Associations that
            // contain Schema's that can be replaced by newAuthority.  We could also
            // just scan for Schema's that have been discarded, which we shouldn't be
            // seeing any of in the global list.  But just in case, we end up doing all
            // of the above and just scan every association for conditions that don't
            // appear to be in sync.
            
            try {
                if (verifyAndUpdate(a, newAuthority))
                    changed = true;
            } catch (Throwable t) {
                Log.error("verifying association " + Util.tags(a) + " with newAuthority " + newAuthority, t);
            }
        }
        return changed;
    }

    private static boolean verifyAndUpdate(final Association a, final Schema newAuthority) 
    {
        final Schema s1 = replaceSchema(a, a.schema1, newAuthority);
        final Schema s2 = replaceSchema(a, a.schema2, newAuthority);

        if (s1 == a.schema1 && s2 == a.schema2) {
            // doesn't need updating
            return false;
        }
        
        final Field f1, f2;

        if (s1 == a.schema1)
            f1 = a.field1;
        else
            f1 = replaceField(a.field1, s1);
        
        if (s2 == a.schema2)
            f2 = a.field2;
        else
            f2 = replaceField(a.field2, s2);

        final Association replacement;
        synchronized (Association.class) {
            // todo: we want to replace the exact location in the AllPairsList
            // so the UI won't re-order
            int index = AllPairsList.indexOf(a);
            removeImpl(a); // toss the old one completely
            a.enabled = false; // just in case, tho nothing should be referencing
            replacement = addImpl(f1, f2, index);
        }
        return true;
        //EventSource.raise(Association.class, new Event(replacement, Event.CHANGED));
        
    }

    private static Schema replaceSchema(Association a, final Schema old, final Schema newAuthority) 
    {
        final Schema s = Schema.lookupAuthority(old);
        if (s != old) {
            if (s != newAuthority) {
                Log.warn("ASSOCIATION NEEDED PATCHING UNRELATED TO NEW AUTHORITY REPORT;"
                         + "\n\t    old: " + old
                         + "\n\t lookup: " + s
                         + "\n\tnewAuth: " + newAuthority
                         + "\n\t  assoc: " + a
                         ,new Throwable("HERE"));
            }
            if (!old.isDiscarded()) {
                Log.warn("REPLACED SCHEMA WASN'T DISCARDED; "
                         + "\n\t    old: " + old
                         + "\n\t lookup: " + s
                         + "\n\tnewAuth: " + newAuthority
                         + "\n\t  assoc: " + a
                         ,new Throwable("HERE"));

            }
        }
        return s;
    }
    
    private static Field replaceField(Field oldField, Schema newSchema) 
    {
        return newSchema.getField(oldField.getName());
    }

//     public static void addByAll(Field newField, Collection<Field> existingFields) {

//         if (existingFields.size() == 0)
//             return;

//         for (Field f : existingFields)
//             new Association(newField, f, false);

// //         if (DEBUG.Enabled) {
// //             Log.debug("CURRENT ASSOC:");
// //             for (Association a : AllPairsList) {
// //                 System.out.println("\t" + a);
// //             }
// //         }
        
//         EventSource.raise(newField, new Event(null, Event.ADDED));
//     }

    /** @return all Associations */
    public static List<Association> getAll() {
        return AllPairsList;
    }

    public static int getCount() {
        return AllPairsList.size();
    }

    public static Association get(int n) {
        if (n < 0 || n > getCount())
            return null;
        else
            return AllPairsList.get(n);
    }
    

    /** @return All enabled associations between the two given schemas */
    public static List<Association> getBetweens(Schema s1, Schema s2) {
        final List<Association> betweens = new ArrayList();
            
        for (Association a : getAll()) {
            if (a.isEnabled() && a.isBetween(s1, s2))
                betweens.add(a);
        }
        //if (DEBUG.SCHEMA && DEBUG.META)
        if (DEBUG.Enabled)
            dumpSmart(betweens, "betweens for schemas: " + s1.getName() + " x " + s2.getName());
        return betweens;
    }
    
    /** @return all enabled JOIN's between the two given schemas given the field of interest
     * A JOIN is an any association that does NOT involve the given Field.
     */
    public static List<Association> getJoins(Schema s1, Field field) {
        final List<Association> betweens = new ArrayList();
        final Schema s2 = field.getSchema();

        if (s1 == null)
            throw new NullPointerException("null Schema");
        if (field == null)
            throw new NullPointerException("null Field");
            
        for (Association a : getAll()) {
            if (a.isEnabled() && a.isBetween(s1, s2) && !a.contains(field))
                betweens.add(a);
        }
        if (DEBUG.Enabled) {
            dumpSmart(betweens, "joins for schemas: " + s1.getName() + " x " + s2.getName()
                      + "; excluding-field: " + Relation.quoteKey(field));
        }
        return betweens;
    }

    private static void dumpSmart(List list, String msg) {
        String s = "";
        if (list.size() <= 1) {
            if (list.size() == 1)
                s = ": " + list.get(0).toString();
            else
                s = ": none";
            //s = ": " + Util.tags(list);
        } 
        Log.debug(msg + s);
        if (list.size() > 1)
            Util.dump(list);
    }

    /** @return true if there are any associations between the two schemas that do NOT contain the given Field,
     * which means we'll need to do a join-extract to look for relationships */
    public static boolean hasJoins(Schema s1, Field field) {
        final Schema s2 = field.getSchema();
        for (Association a : getAll()) 
            if (a.isEnabled() && a.isBetween(s1, s2) && !a.contains(field))
                return true;
        return false;
    }
//     public static boolean hasBetweens(Schema s1, Schema s2) {
//         for (Association a : getAll()) 
//             if (a.isEnabled() && a.isBetween(s1, s2))
//                 return true;
//         return false;
//     }
    
    

    /** @return all associations containing the given Field as one side of the association */
    public static Collection<Association> getAliases(Field f) {
        final Collection<Association> got = AllByField.get(f);

        if (DEBUG.Enabled) {
            final Object gots;
            if (got.size() == 0)
                gots = null;
            else if (got.size() == 1)
                gots = Util.getFirst(got);
            else
                gots = "";
            //Log.debug("found aliases for " + f + ": " + gots);
            if (got.size() > 1) {
                Util.dump(got);
                Log.debug("found aliases for " + f + ": " + gots);
            }
        }
        
        return got;
    }

    /** @return all opposite Fields mentioned in associations containing the given Field */
    public static Collection<Field> getPairedFields(Field field) {
        final Collection<Field> pairedTo = new HashSet();
        for (Association a : Association.getAliases(field)) {
            pairedTo.add(a.getPairedField(field));
        }
        return pairedTo;
    }
    

    public boolean contains(Field f) {
        return f == field1 || f == field2;
    }

    public Field getLeft() {
        return field1;
    }
    public Field getRight() {
        return field2;
    }
        
    public Field getPairedField(Field f) {
        if (f == field1)
            return field2;
        else if (f == field2)
            return field1;
        else
            throw new Error("field not in association: " + f);
    }
        
    public Field getFieldForSchema(Schema s) {
        if (s == schema1)
            return field1;
        else if (s == schema2)
            return field2;
        else
            throw new Error("field for schema not in association: " + s);
    }
        
    public String getKeyForSchema(Schema s) {
        return getFieldForSchema(s).getName();
    }
    
    public boolean isBetween(Schema s1, Schema s2) {
        if (schema1 == s1 && schema2 == s2)
            return true;
        else if (schema1 == s2 && schema2 == s1)
            return true;
        else
            return false;
    }
        
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean on) {
        enabled = on;
    }

//     /** @return true if the two Schema's are joined -- their key fields are associated */
//     public static boolean isJoined(Schema s1, Schema s2) {
//         return getJoin(s1, s2) != null;
//     }
    
//     /** @return true if this association is a DOUBLE JOIN -- an association between
//      * the key fields of two schemas.
//      */
//     public boolean isDoubleJoin() {
//         return schema1.getKeyField() == field1
//             && schema2.getKeyField() == field2;
//     }
//     /** @return true if this association is a JOIN -- an association between
//      * at least one key field of one of the Schema's
//      */
//     public boolean isJoin() {
//         return schema1.getKeyField() == field1
//             || schema2.getKeyField() == field2;
//     }

//     /** @return the Association found if the two Schema's are joined (their key fields are associated) */
//     public static Association getJoin(Schema s1, Schema s2) {

//         if (s1 == s2)
//             return null;

//         // if we need more performance here, maintain this via a special map so we
//         // can instantly look it up -- or even store it right in each Schema, which
//         // would probably be the fastest.  We'll need to handle updating those maps
//         // tho for Schema's that reload after a user has, god forbid change their
//         // key field.

//         for (Association a : getAll())
//             if (a.isEnabled() && a.isBetween(s1, s2) && a.isJoin())
//                 return a;

//         return null;
//     }

    @Override public String toString() {
        return String.format("Association[%s%s=%s]",
                             enabled ? "" : "DISABLED ",
                             quoteKey(field1),
                             quoteKey(field2));
    }
}
