package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.LWComponent;

import java.util.Collection;
    
/**
 * Mainly a collection of static methods for searching data-sets (Schemas)
 * and finding relationships among row-nodes and value-nodes.  Some
 * methods return instances of a Relation which say something about
 * how the data was related.
 *
 * @version $Revision: 1.1 $ / $Date: 2009-07-06 15:39:48 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public final class Relation {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Relation.class);

    // relationship types
    public static final Object EXPLICIT = "rel-user";
    public static final Object AUTOMATIC = "rel-auto";
    public static final Object JOIN = "rel-join";
        
    final Object type;
    final String key, value;

    private Relation(Object type, boolean forward, String k, String v) {
        this.type = type;
        this.key = k;
        this.value = v;
    }
    private Relation(Object type, String k, String v) {
        this(type, true, k, v);
    }

    //========================================================================================
    // Can we do this: all the findMatching / getMatching association using code below
    // is converted to generic non-schema based code, that just works on lists of
    // MetaMaps -- then we could use it with both the list of DataRow's, as well as lists
    // of LWComonent meta-data?  The getField checks returning EMPTY_LIST couldn't be done
    // tho -- that would be a nice optimization to keep.  Or could we really keep a Schema
    // around which represents all the data on the map?  *adding* to that schema would be
    // easy, but *removing* data from it would be a problem -- schema's don't work that way.
    //========================================================================================

    /** an interface for any key-value data map.  may eventually be removed, but
     * useful for making the search routines work with anything that can provide
     * these basic functions or easily delegate to something that can */
    // todo: consider renaming to DataRow, and renaming the Schema.DataRow impl & making it private
    public interface Scannable {
        public boolean hasEntry(String key, String value);
        public String getString(String key);
        public Collection<String> getValues(String key);
        public Schema getSchema();
    }

    
    /** search the given Scannable's for the given Field=fieldValue, using association's, and add matches to results
     * This essentially does an "A.K.A" with the Field based on the user associations */
    static void searchDataWithField
        (final Field fieldKey,
         final String fieldValue,
         final Collection<? extends Scannable> searchSet,
         final Collection results)
    {
        final String fieldName = fieldKey.getName();
        
        searchData(fieldName, fieldValue, searchSet, results);

        if (DEBUG.Enabled) {
            Log.debug(String.format("searchDataWithField: %s='%s'\n\tsearchSet: %s",
                                    fieldKey, fieldValue, Util.tags(searchSet)));
            //Util.dump(Association.lookup(field));
        }
        
        for (Association a : Association.lookup(fieldKey)) {
            if (a.isEnabled())
                searchData(a.getPairedField(fieldKey).getName(),
                           fieldValue,
                           searchSet,
                           results);
        }
    }
    
    
    /**
     * Uses an entire row of data to do AKA searches looking for relationships
     * betweens rows from DIFFERENT schema's.
     *
     * @param rowKey - a data "row" - a bag of related key/value pairs
     * @param searchSet - a bag of rows to search for association based relationships
     * @param searchSchema - Schema for the rows in the searchSet, DIFFERENT from rowKey's schema
     * @param results - rows found in the searchSet that have a relationship to rowKey will be added here
     *
     * For example, in VUE, this would eventually be called after dropping "all rows" in
     * the DataTree from Schema-InTree, onto a node on the map from Schema-OnMap, and searching
     * all rows in Schema-InTree for nodes to add to the map that are related to the row from
     * Schema-OnMap.
     *
     */
    static void searchDataWithRow
        (final Scannable rowKey, // e.g., a MetaMap, from a DIFFERENT schema than the search-set
         final Collection<? extends Scannable> searchSet,
         final Schema searchSchema, // may be null if the searchSet contains more than one Schema
         final Collection results)
    {
        if (searchSchema == null)
            throw new UnsupportedOperationException("schema is null; variable searchSet schema's not implemented");
        
        // [edit] look auto-joins e.g., if there are ANY join between Faculty & Pubs (e.g.,
        // Name=Author), then these to schemas are in fact "joined", and can filter
        // based on that.
        //
        // So dropping Pubs onto a Faculty can find the joins (just the first joined?
        // priority to key fields?)  uses Faculty.Name to search through all Pubs for
        // matching Pubs.Author's, and pulls those records.
        //
        // Note that in this case, Faculty.Name happens to be a key field, but
        // Pubs.Author is NOT a key field.  (Pubs.Title is the key field there)

        final Schema keySchema = rowKey.getSchema();

        if (DEBUG.Enabled) {
            Log.debug("searchDataWithRow: "
                      + "\n      rowKey: " + rowKey
                      + "\n   searchSet: " + Util.tags(searchSet)
                      + "\nsearchSchema: " + searchSchema
                      );
        }

        if (keySchema == searchSchema)
            throw new Error("can only search a schema with a row from another schema: " + keySchema);
        
        for (Association a : Association.getBetweens(searchSchema, keySchema)) {
            Log.debug("searchDataWithRow: scanning for " + a);
            final String localKey = a.getKeyForSchema(searchSchema);
            final String remoteKey = a.getKeyForSchema(keySchema);
                
            // instead of data.getString, we really needs to search ALL the values for that key???
            // (e.g., multiple category values)
            
            searchData(localKey,
                       rowKey.getString(remoteKey), // TODO: handle multiple values
                       searchSet,
                       results);
        }
    
        for (Scannable s : searchSet) {
            if (isAutoRelated(rowKey, s))
                results.add(s);
        }
    }

    /** search the given Scannable's for the given key=value, and add matches to results */
    private static void searchData
        (final String key,
         final String value,
         final Collection<? extends Scannable> searchSet,
         final Collection results)
    {
        if (DEBUG.Enabled) Log.debug("searchData " + key + "=" + value + " in " + Util.tags(searchSet));
        
        for (Scannable row : searchSet) {
            if (row.hasEntry(key, value))
                results.add(row);
        }
    }
    

    private static Relation tryAutoRelate(Scannable row1, Scannable row2)
    {
        //-----------------------------------------------------------------------------
        // [note: logic was initially from makeCrossSchemaRowNodeLinks)
        // First, check for matching key field names, even if there isn't an explicit
        // association between the two.  This code checks for the presence of the key
        // field from one schema ANYWHERE else in the paired node -- not just as the key
        // field.  Essentially an automatic open-ended association based on key fields.
        // -----------------------------------------------------------------------------

        String relatedValue;
        
        //-------------------------------------------------------
        
        final Schema s1 = row1.getSchema();
        final String keyField1 = s1.getKeyFieldName();

        relatedValue = relatedBy(keyField1, row1, row2);
        if (relatedValue != null)
            return new Relation(AUTOMATIC, true, keyField1, relatedValue);

        //-------------------------------------------------------
        
        final Schema s2 = row2.getSchema();
        final String keyField2 = s2.getKeyFieldName();

        relatedValue = relatedBy(keyField2, row1, row2);
        if (relatedValue != null)
            return new Relation(EXPLICIT, false, keyField2, relatedValue);
        
        //-------------------------------------------------------

        // TODO: do open-ended relating based in any key fields that happen to have the same name
        // for performance, in the cases where we know the schemas up front, we can inspect
        // them for shared names and then just only ever look for those shared names.
        // Actually, could just compute & store that in every schema -- a map in each schema
        // by all other schemas of all field names shared by those other schemas.
        
        return null;
    }
    
    /** note: duplicate core logic of tryAutoRelate, trimmed for performance (it doesn't return a new Relation) */
    private static boolean isAutoRelated(Scannable row1, Scannable row2)
    {
        final Schema s1 = row1.getSchema();

        if (relatedBy(s1.getKeyFieldName(), row1, row2) != null)
            return true;
        
        final Schema s2 = row2.getSchema();

        if (relatedBy(s2.getKeyFieldName(), row1, row2) != null)
            return true;
        
        return false;
    }
    
    /** @return true if the two rows (from the same schema) are the "same" -- the have the same key field value */
    public static boolean isSameRow(final Scannable row1, final Scannable row2)
    {
        if (DEBUG.Enabled) {
            if (row1.getSchema() != row2.getSchema()) {
                //Log.debug("testing same row for different schemas");
                return false;
            }
        }
        
        return relatedBy(row1.getSchema().getKeyFieldName(),
                         row1,
                         row2) != null;
    }


    public static boolean isSameRow(final LWComponent c1, final LWComponent c2)
    {
        if (c1 == null || c2 == null)
            return false;
        return isSameRow(c1.getRawData(), c2.getRawData());
    }

    /** @return true if the two rows in the given schema are the "same" -- the have the same key field value */
    public static boolean isSameRow(final Schema schema, final Scannable row1, final Scannable row2)
    {
        if (DEBUG.Enabled) {
            if (row1.getSchema() != row2.getSchema())
                throw new Error("different schemas");
            if (row1.getSchema() != schema)
                throw new Error("schema mis-match");
        }
        
        return relatedBy(schema.getKeyFieldName(),
                         row1,
                         row2) != null;
    }
    
    /** @return the value that was found to match between the two
     * If more than one, we return the first for now.  E.g., each row may have multiple
     * "category" values, and more than one might match. [todo: currently only checks first value!]
     */
    private static String relatedBy
        (final String key,
         final Scannable row1,
         final Scannable row2)
    {
        //----------------------------------------------------------------------------------------
        // NOTE: this uses hasEntry instead of fetching & comparing
        // values, which will automatically check ALL values for the given key
        //----------------------------------------------------------------------------------------

        final String row1_value = row1.getString(key);  // TODO: handle multiple values

        //Log.debug(String.format("relatedBy: %s", key));
        
        //Log.debug(String.format("relatedBy0 %s='%s' in %s", key, row1_value, Util.tags(row2)));
        if (row2.hasEntry(key, row1_value)) {
            Log.debug(String.format("relatedBy: found %s='%s'", key, row1_value));
            return row1_value;
        }
            
        // The semantic reverse of the above case.  THE REASON WE DO TWO TESTS is only
        // for the case of multple values (e.g., 10 different category values).  There
        // was some test case I forget where I wanted this -- pretty sure it was a news
        // feed example, probably at some time when we attempted to auto-relate on all
        // fields in a row.  However, BUG: even doing this isn't enough: E.g., if key
        // was "category", we'd need to iterate through ALL the values for "category"
        // found in row1 (not just the first), and check it against ALL the values for
        // "category" found in row2.
        
        final String row2_value = row2.getString(key);  // TODO: handle multiple values

        //Log.debug(String.format("relatedBy1 %s='%s' in %s", key, row2_value, Util.tags(row1)));
        if (row1.hasEntry(key, row2_value)) {
            Util.printStackTrace("relatedBy: returning on 2nd value: " + key + "=" + row2_value
                                 + "\n\trow1: " + row1
                                 + "\n\trow2: " + row2
                                 );
            return row2_value;
        }

        return null;
    }


    // todo: if schema is flat (e.g., non-xml), we can do a much simpler/faster test */
    private static String relatedByAKA_multiValues
        (final String key1,
         final String key2,
         final Scannable row1,
         final Scannable row2)
    {
        int i;

        // TODO: case independence for values?

        i = 0;
        for (String row1value : row1.getValues(key1)) {
            if (row2.hasEntry(key2, row1value)) {
                Log.debug("relatedByAKA: 1st pass found match at row1 value #" + i + "; key=" + key2);
                return row1value;
            }
            i++;
        }
        
        i = 0;
        for (String row2value : row2.getValues(key2)) {
            if (row1.hasEntry(key1, row2value)) {
                Log.debug("relatedByAKA: 2nd pass found match at row1 value #" + i + "; key=" + key1);
                return row2value;
            }
            i++;
        }

        return null;
    }
    

    /** @return a Relation between the two rows if any can be found, null otherwise
     * The rows must be from different schemas.
     */
    public static Relation getRelation(Scannable row1, Scannable row2)
    {
        final Schema s1 = row1.getSchema();
        final Schema s2 = row2.getSchema();

        if (s1 == s2)
            throw new Error("same schema: " + s1);

        if (DEBUG.SCHEMA && DEBUG.META) Log.debug("getRelation;\n\trow1=" + row1 + "\n\trow2=" + row2);
        
        for (Association a : Association.getAll()) {

            if (a.isEnabled() && a.isBetween(s1, s2))
                ; // go ahead and apply this association
            else
                continue;

            final String key1 = a.getKeyForSchema(s1);
            final String key2 = a.getKeyForSchema(s2);
            
            //Log.debug("getRelation: applying key1=" + key1 + "; key2=" + key2 + " from " + a);

            // todo: if schema is flat (e.g., non-xml), we can do a much simpler/faster test
            String relatedValue = relatedByAKA_multiValues(key1, key2, row1, row2);

            if (relatedValue != null) {

                final String alsoKnownAsKey = String.format("%s=%s", key1, key2); // TWO keys equal this value
                
                return new Relation(EXPLICIT,
                                    alsoKnownAsKey,
                                    relatedValue);
            }
        }

        return tryAutoRelate(row1, row2); // will return null if none found
    }

// Old single-value-only relation code:            
//             String relatedValue = row1.getString(key1);
//             // TODO: handle multiple values -- use row.hasEntry like relatedBy
//             if (relatedValue != null && relatedValue.equals(row2.getString(key2))) { // TODO: case independence?
//                 final String alsoKnownAsKey = String.format("%s=%s", key1, key2); // TWO keys equals this value
//                 return new Relation(RELATION_JOIN,
//                                     alsoKnownAsKey,
//                                     relatedValue);
//             }
    

    @Override public String toString() {
        return String.format("Relation[%-10s %s=%s]", type, key, value);
        //return String.format("Relation[%-10s %s=%s %s]", type, key, value, isForward ? "->" : "<-");
    }
}
