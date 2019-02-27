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

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.MetaMap;
import tufts.vue.LWComponent;

import java.util.*;
    
/**
 * Mainly a collection of static methods for searching data-sets (Schemas)
 * and finding relationships among row-nodes and value-nodes.  Some
 * methods return instances of a Relation which say something about
 * how the data was related.
 *
 * @version $Revision: 1.8 $ / $Date: 2010-02-03 19:13:16 $ / $Author: mike $
 * @author Scott Fraize
 */
public final class Relation {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Relation.class);

    // relationship types
    public static final Object USER = "USER"; // user association
    public static final Object AUTOMATIC = "AUTO";
    public static final Object JOIN = "JOIN";
    /** essentially an intra-schema "join" */
    public static final Object COUNT = "COUNT";

    static final String ALL_VALUES = "*all-values*";

    private static final String NO_VALUE = "<no-value>";
    private static final boolean CROSS_SCHEMA = true;
        
    final Object type;
    final String key, value;
    final boolean isCrossSchema;
    int count = 1;

    private Relation(Object type, String k, String v, boolean crossSchema) {
        this.type = type;
        this.key = k;
        this.value = v;
        this.isCrossSchema = crossSchema;
    }
    private Relation(Object type, String k, String v) {
        this(type, k, v, false);
    }

    int getCount() {
        return count;
    }

    boolean isCrossSchema() {
        return isCrossSchema;
    }
    
//     private void setCrossSchema() {
//         isCrossSchema = true;
//     }

    String getDescription() {
        
        String s;
        
        if (type == JOIN || type == COUNT) {
            // value is empty, key is description
            s = String.format("%s: %s", type, key);
        } else {
            s = String.format("%s=%s", key, value);
        }

        if (count > 1) {
            s += String.format(" x %d", count);
        }

        return s;
    }
    
//     private Relation(Object type, String description) {
//         this(Relation.JOIN, true, description, NO_VALUE);
//         if (type != JOIN)
//             throw new Error("must be a join relation if only a description is provided");
//     }

    @Override public String toString() {
        return String.format("Relation[%s %s=%s #%d%s]", type, key, value, count, isCrossSchema ? " X" : "");
//         if (type == JOIN) {
//             // value is empty, key is description
//             return String.format("Relation[%-10s %s]", type, key);
//         } else {
//             return String.format("Relation[%-10s %s=%s]", type, key, value);
//         }
//         //return String.format("Relation[%-10s %s=%s %s]", type, key, value, isForward ? "->" : "<-");
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
        public boolean hasEntry(String key, CharSequence value);
        public String getString(String key);
        public Collection<String> getValues(String key);
        public Schema getSchema();
    }

    
//    /** @return null if none found, or the VALUE of what was the relation was based on otherwise */
//     static String getCrossSchemaRelation
//         (final Field field,
//          final MetaMap rowData,
//          final String fieldValue)
//     {
//         String relation = null;
        
//         int count = -1;
//         for (String joinedValue : getCrossSchemaJoinedValues(field, rowData, fieldValue)) {
//             count++;
//             if (count > 0) {
//                 Log.debug("IGNORING JOINED VALUE #" + count + " FOR LINK CREATION: " + joinedValue, new Throwable("HERE"));
//                 continue;
//             }
//             //final String relation = String.format("%s=\"%s\"\n%s=\"%s\"",
//             //indexKey, indexValue,
//             //extractKey, extractValue);
//             relation = String.format("matched joined value \"%s\"", joinedValue);
//         }

//         return relation;
//     }
    
    /** @return the first Relation found if any, null otherwise */
    static Relation getCrossSchemaRelation
        (final Field field,
         final MetaMap rowData,
         final String fieldValue)
    {
        Relation relation = null;
        
        int count = -1;
        for (Relation join : getCrossSchemaJoinedValues(field, rowData, fieldValue)) {
            count++;
            if (count > 0) {
                Log.debug("IGNORING JOINED VALUE #" + count + " FOR LINK CREATION: " + join.value, new Throwable("HERE"));
                continue;
            }
            relation = join;
        }

        return relation;
    }
    
//     static Collection<String> getCrossSchemaJoinedValues
//         (final Field field,
//          final MetaMap rowData,
//          final String fieldValue) // return extract values that match, unless this is ALL_FIELD_VALUES
//     {
//         return getCrossSchemaJoinedValues(field, Collections.singletonList(rowData), fieldValue);
//     }


    //----------------------------------------------------------------------------------------
    // The first VUE3 Grant case, Rockwell Kent:
    //
    // The most complicated part is joining the Mediums.  We could just get away with
    // using row nodes for mediums, and our existing code would find relationships pretty
    // well, but the problem is that there are 42 paintings (records), with an eventual
    // enumerated set of only 12 medium's, and we'd like to be able to place just THOSE
    // TWELVE on the map, w/out having to add a new record for every painting, so we
    // could cluster the paintings by medium if we wish.
    //----------------------------------------------------------------------------------------

    /**
     * currently used in these ways:
     * 1 - if fieldValue is ALL_VALUES, use rowData to find all relating field values
     * 2 - if fieldValue is specific, we return only Relations found with that fieldValue
     * 3 - called repeatly across rows to find rows that have a matching fieldValue
     */
    static Collection<Relation> getCrossSchemaJoinedValues
        (final Field field,
         final MetaMap rowData,
         //final Collection<MetaMap> rowData,
         final String fieldValue) // return extract values that match, unless this is ALL_FIELD_VALUES
    {

        if (rowData == null) {
            //Util.printStackTrace("NULL ROW DATA AGAINST FIELD: " + field);
            return Collections.EMPTY_LIST;
        }
        
        //-----------------------------------------------------------------------------
        //
        // WE IMPLEMENT JOIN's HERE: E.g., if we drag Rockwell-Mediums.medium onto a
        // Rockwell-Paintings.<row-node> and Mediums has been joined to Paintings via
        // their key Field "titles" (tho they don't have to have the same name as per
        // Associations), then we search for all Mediums rows with a title that matches
        // the drop-target row-node "titles" (doesn't have to be same name), and those
        // will be the value nodes.

        // We could implement this elsewhere if we replace the above getValues call
        // with something like Field.getMatchingValues(<row-data>), which will
        // work like Schema.getMatchingValues (or put it right in the schema code).
        //
        //-----------------------------------------------------------------------------
        
        final Schema fieldSchema = field.getSchema();
        final Schema rowSchema = rowData.getSchema();

        //final Set<String> valuesSeen = new HashSet(); // to detect dups
        //final List<Relation> results = new ArrayList();
        final Map<String,Relation> results = new HashMap();
        
        if (DEBUG.SCHEMA || DEBUG.WORK) {
            Log.debug("getCrossSchemaJoinedValues:"
                      + "\n\tfieldSchema: " + fieldSchema
                      + "\n\t  rowSchema: " + rowSchema
                      + "\n\t    rowData: " + rowData
                      + "\n\t      field: " + quoteKV(field, fieldValue)
                      );
        }
        
        if (fieldSchema == rowSchema) {
//             Log.warn("JOIN: same schema, no joins possible for " + quoteKey(field) + "<=>" + rowData,
//                      new Throwable("HERE"));
            //Log.debug("JOIN: warning: same schema, no joins possible for " + field + "<=>" + rowData);
            //Util.printStackTrace("same schema: " + fieldSchema);

            
            //----------------------------------------------------------------------------------------
            // TODO: procuring these params this way is a total hack -- need uniform method of
            // pulling this info from a MetaMap
            //----------------------------------------------------------------------------------------
            final String fieldName = rowData.getString(LWComponent.EnumeratedValueKey);

            if (fieldName == null) {
                if (DEBUG.SCHEMA || DEBUG.WORK)
                    Log.debug("JOIN: same schema, no joins possible for " + quoteKey(field) + "<=>" + rowData);
                         //new Throwable("HERE"));
                return Collections.EMPTY_LIST;
            }
                
            final Field indexKey = rowSchema.getField(fieldName);
            final String indexValue = rowData.getString(fieldName);
            //----------------------------------------------------------------------------------------
            
            Log.debug("INTRA-SCHEMA JOIN: indexing " + quoteKV(indexKey, indexValue));
            
            runJoin(fieldSchema,
                    indexKey,
                    indexValue,
                    field,
                    fieldValue,
                    results, Relation.COUNT, false);

            return results.values();
            
            //return Collections.EMPTY_LIST;
        }

        int i = -1;
        //for (Association join : Association.getBetweens(fieldSchema, rowSchema)) {
        for (Association join : Association.getJoins(rowSchema, field)) {
            i++;

            //-----------------------------------------------------------------------------
            // THE CASE WE'RE MISSING IS THIS: (to handle IN getCrossSchemaRelation,
            // which should become a more generic getRelations) -- if the SEARCH field
            // (e.g., medium) is DIFFERENT than the field found in the Association, only
            // THEN do we need to attempt a join, otherwise, we can use a straight
            // Association.  Actually, not 100% sure where to put that -- we may want to
            // check for this in callers before we even get here, and/or go back to
            // having Association.getJoins(schema1, schema1, FIELD), which will only
            // return betweens that do NOT match the given field.
            //
            // [ABOVE IS NOW HANDLED VIA Association.getJoins, which implements the exclusion]
            //
            // We may need to revisit everywhere we check Associations in low-level
            // code and pull that up to higher level, or at least replace them
            // with a smarter association check that automatically handles the join
            // cases (in which case, joins might even auto-cascade across data-sets,
            // tho that could be dangerous, and recording the relationship would
            // become a recursive process).
            //-----------------------------------------------------------------------------
            
            if (DEBUG.Enabled) Log.debug("JOIN #" + i + ": " + join);
            
            // This works for the Rockwell-Mediums case, tho only for initial node
            // creation of course -- NEED TO GENERALIZE

            final Field indexKey = join.getFieldForSchema(fieldSchema);
            final String indexValue = rowData.getString(join.getKeyForSchema(rowSchema)); // todo: multi-values

            runJoin(fieldSchema, indexKey, indexValue, field, fieldValue, results, Relation.JOIN, true);

            // insert process-join here? (that can be re-usable for INTRA-schema "joins")

        }

        return results.size() > 0 ? results.values() : Collections.EMPTY_LIST;
    }

    /** @return passed in results for convenience */
    // call this something like "getUniqueRelatedValues"
    // this doesn't care if the searchKey is from the indexSchema or not (originally, this
    // was from code that would only work if the searchKey was from a DIFFERENT Schema)
    private static void runJoin
        (final Schema indexSchema,
         final Field indexKey,
         final String indexValue,
         final Field searchKey,
         final String searchValue,
         final Map<String,Relation> results,
         final Object joinType,
         final boolean isCrossSchema)
    {
        if (DEBUG.Enabled) {
            Log.debug("RUNJOIN " + quoteKV(indexKey, indexValue)
                      + " in " + indexSchema
                      + " for " + quoteKV(searchKey, searchValue));
        }

        final Collection<DataRow> matchingRows = indexSchema.getMatchingRows(indexKey, indexValue);

        if (DEBUG.Enabled) Log.debug("JOIN: found 1st pass rows: " + Util.tags(matchingRows));

        for (DataRow row : matchingRows) {
            // todo: use Schema.searchData?
            final Collection<String> joinedValues = row.getValues(searchKey);
            if (DEBUG.Enabled && joinedValues.size() > 1) Log.debug("JOIN: extracted " + Util.tags(joinedValues));
            for (String extractValue : joinedValues) {

                // most commonly, we will only iterate over one extractValue
                // (one value per key)

                //------------------------------------------------------------------
                // ** THE EXCLUSION CASE **                    

                if (searchValue == ALL_VALUES)
                    ; // proceed -- we're not filtering, accumulate all values
                else if (searchValue.equals(extractValue))
                    ; // proceed -- we've matched a value we're looking for
                else
                    continue; // stop: doesn't match what we're looking for
                    
                //------------------------------------------------------------------

                if (DEBUG.Enabled) {
                    String debug = String.format("%s=%s; %s=%s",
                                                 indexKey, Util.tags(indexValue),
                                                 searchKey, Util.tags(extractValue));
                    Log.debug(Util.TERM_YELLOW + "JOIN: relation " + debug);

                    // if we keep the code that is just looking to see if a relation
                    // exists at all (e.g., the link finding code), we can abort runJoin
                    // entirely as soon as the first one is found.
                }

                Relation r = results.get(extractValue);
                if (r == null) {
                    final String relation = String.format("%s=\"%s\"; %s=\"%s\"",
                                                          indexKey, indexValue,
                                                          searchKey, extractValue);
                        
                    r = new Relation(joinType, relation, extractValue, isCrossSchema);
                    Log.debug("JOIN: added " + r);
                    results.put(extractValue, r);
                } else {
                    r.count++;
                }
            }
        }
    }
    
    
//             if (DEBUG.Enabled) {
//                 Log.debug("JOIN #" + i
//                           + ": indexKey=" + quoteKey(indexKey)
//                           + "; indexValue=" + quoteVal(indexValue));
//             }

//             final Collection<DataRow> matchingRows = fieldSchema.getMatchingRows(indexKey, indexValue);

//             if (DEBUG.Enabled) Log.debug("JOIN: found rows: " + Util.tags(matchingRows));

//             final Field extractKey = field;

//             for (DataRow row : matchingRows) {
//                 // todo: use Schema.searchData?
//                 final Collection<String> joinedValues = row.getValues(extractKey);
//                 if (DEBUG.Enabled) Log.debug("JOIN: extracted " + Util.tags(joinedValues));
//                 for (String extractValue : joinedValues) {

//                     //------------------------------------------------------------------
//                     // ** THE EXCLUSION CASE **                    

//                     if (fieldValue == ALL_VALUES)
//                         ; // proceed -- we're not filtering, accumulate all values
//                     else if (fieldValue.equals(extractValue))
//                         ; // proceed -- we've matched a value we're looking for
//                     else
//                         continue; // stop: doesn't match what we're looking for
                    
//                      //------------------------------------------------------------------

//                     if (DEBUG.Enabled) {
//                         String debug = String.format("%s=%s; %s=%s",
//                                                         indexKey, Util.tags(indexValue),
//                                                         extractKey, Util.tags(extractValue));
//                         Log.debug(Util.TERM_YELLOW + "JOIN: FOUND RELATION " + debug);
//                     }

//                     if (valuesSeen.add(extractValue)) {

//                         final String relation = String.format("%s=\"%s\"; %s=\"%s\"",
//                                                           indexKey, indexValue,
//                                                           extractKey, extractValue);
                        
//                         Relation r = new Relation(Relation.JOIN, relation, extractValue);
//                         results.add(r);
//                     }
//                 }
//             }
    
    //----------------------------------------------------------------------------------------
    // todo: all these methods take DataRows, which are really a wrapper of a MetaMap --
    // should chage all this stuff to work from MetaMaps (which actually have the
    // darn schema in them now anyway!)  And then we could also use some of the util
    // functions / link-creating funtions based on LWComponent to use MetaMap's as well.

    // TODO: need to move isDataValueNode / isDataRowNode to MetaMap
    //----------------------------------------------------------------------------------------
    /**
     * @param schema - the schema to search through
     * @param filterNode - the data-node (either a data-row-node or a data-value-node),
     * to use as a basis for searching the schema for related rows
     */

    static Collection<DataRow> findRelatedRows(Schema schema, LWComponent filterNode) {

        if (DEBUG.Enabled) {
            Log.debug("\n\nfindRelatedRows:"
                      + "\n\t schema: " + schema
                      + "\n\t   node: " + filterNode
                      + "\n\t   data: " + filterNode.getRawData()
                      );
        }

        if (filterNode.isDataValueNode()) {

            if (DEBUG.Enabled) Log.debug("findRelatedRows: VALUE node: " + filterNode);
            
            //-----------------------------------------------------------------------------
            // TODO: Support "merged" or compound value nodes.  A node that either has multiple
            // values, or multiple keys and values.  This will complicate this code, tho
            // maybe it will actually all become more similar in some respects as it
            // will all behave like a row-based search?  Actually, all we need to do is
            // get all (non-internal) key/values from the filterNode, and accumulate the
            // matching rows here, or impl that in getMatchingRows.
            //
            // Compound nodes would make the HIERARCHY use case much easier.  E.g.,
            // in ClubZora test set, drop "Role" onto "Latin America", where "Latin America"
            // is surrounded by it's row nodes, and we simply create compound value
            // nodes like this:
            //  Compound 1: Region="Latin America", Role="user"
            //  Compound 2: Region="Latin America", Role="mentor"
            //  Compound 3: Region="Latin America", Role="admin"
            //  etc...
            //
            // And then we'd cluster these compounds around Latin America, with extra
            // distance for the 2nd tier clustering, and then re-cluster the row nodes
            // around each of the compound nodes.  The relationship from each compound
            // node to it's set of row nodes would naturally be found by the search
            // routines once their modified to handle compound matching, which shouldn't
            // be too hard.  In this particular use case, the links from "Latin America"
            // to all the row nodes may become pretty messy/noisy tho -- need helper
            // actions for cleaning those out.
            //
            // Or, could hack this at the map level by MakeCluster on a parent node
            // w/no links does a joined cluster from all data-value children.
            // OR even just by putting them in a group.  Cluster nodes could be made
            // smart to prefer clustering for each of the grouped relations
            // in the "pie-slice" closest / most facing the appropriate node, tho
            // that would only really work with 2 nodes when stacked vertically,
            // more nodes than that would work best laid out in a row (as nodes are
            // usually wider than they are tall, especially value nodes).
            // -----------------------------------------------------------------------------
        
            final Field filterField = filterNode.getDataValueField();
            if (filterField == null)
                throw new NullPointerException("node has no field: " + filterNode);
            final String filterValue = filterNode.getDataValue(filterField.getName());

            return schema.getMatchingRows(filterField, filterValue);
        }
        else if (filterNode.isDataRowNode()) {

            if (DEBUG.Enabled) Log.debug("findRelatedRows: ROW node: " + filterNode);
            //Log.debug("FINDING ROWS MATCHING KEY FIELD OF FILTERING ROW NODE: " + filterNode);
            
            final Schema filterSchema = filterNode.getDataSchema();
            
            if (filterSchema == schema)
                throw new Error("can't do row-node filter from same schema");

            return schema.getMatchingRows(filterNode.getRawData());
            
        } else {
            throw new Error("unhandled filter case: " + filterNode);
        }

    }

    /** search the given Scannable's for the given Field=fieldValue, using association's, and add matches to results
     * This essentially does an "A.K.A" with the Field based on the user associations */
    
    //---------------------------------------------------------------------------------------------------
    // TODO: currently only being used with schema rows as searchSet -- meant to also use with
    // LWComponent's to normalize data-search code.
    // ---------------------------------------------------------------------------------------------------
    
    static void searchDataWithField
        (final Field fieldKey,
         final String fieldValue,
         final Collection<? extends Scannable> searchSet,
         final Collection results)
    {
        final String fieldName = fieldKey.getName();
        
        if (DEBUG.Enabled) {
            Log.debug(String.format("searchDataWithField: %s=%s",
                                    quoteKey(fieldKey),
                                    Util.tags(fieldValue)));
//             Log.debug(String.format("searchDataWithField: %s='%s'\n\tsearchSet: %s",
//                                     fieldKey, fieldValue, Util.tags(searchSet)));
//             //Util.dump(Association.lookup(field));
        }
        
        searchData(fieldName, fieldValue, searchSet, results);

        for (Association a : Association.getAliases(fieldKey)) {
            if (a.isEnabled()) {
                final String relatedField = a.getPairedField(fieldKey).getName();
                if (fieldName.equals(relatedField))
                    continue; // already searched above
                searchData(relatedField,
                           fieldValue,
                           searchSet,
                           results);
            }
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
    
    //---------------------------------------------------------------------------------------------------
    // TODO: currently only being used with schema rows as searchSet -- meant to also use with
    // LWComponent's to normalize data-search code.
    // ---------------------------------------------------------------------------------------------------
    
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
            if (DEBUG.Enabled) Log.debug("searchDataWithRow: scanning for " + a);
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
        if (DEBUG.Enabled) Log.debug("searchData: " + quoteKV(key, value) + " in " + Util.tags(searchSet));
        
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
            return new Relation(AUTOMATIC, keyField1, relatedValue);

        //-------------------------------------------------------
        
        final Schema s2 = row2.getSchema();
        final String keyField2 = s2.getKeyFieldName();

        relatedValue = relatedBy(keyField2, row1, row2);
        if (relatedValue != null)
            return new Relation(AUTOMATIC, keyField2, relatedValue);
        
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
    /** if the two rows are related by the given keys, return the value they're related by, null otherwise */
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
                Log.debug("relatedByAKA: 1st pass found match at row1 value #" + i + " " + quoteKV(key2, row1value));
                return row1value;
            }
            i++;
        }
        
        i = 0;
        for (String row2value : row2.getValues(key2)) {
            if (row1.hasEntry(key1, row2value)) {
                Log.debug("relatedByAKA: 2nd pass found match at row1 value #" + i + " " + quoteKV(key1, row2value));
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
        final Schema schema1 = row1.getSchema();
        final Schema schema2 = row2.getSchema();

        if (schema1 == schema2)
            throw new Error("same schema: " + schema1);

        if (DEBUG.SCHEMA && DEBUG.META) Log.debug("getRelation;\n\trow1=" + row1 + "\n\trow2=" + row2);
        
        for (Association a : Association.getAll()) {

            if (a.isEnabled() && a.isBetween(schema1, schema2))
                ; // go ahead and apply this association
            else
                continue;

            final String key1 = a.getKeyForSchema(schema1);
            final String key2 = a.getKeyForSchema(schema2);
            
            //Log.debug("getRelation: applying key1=" + key1 + "; key2=" + key2 + " from " + a);

            // todo: if schema is flat (e.g., non-xml), we can do a much simpler/faster test
            String relatedValue = relatedByAKA_multiValues(key1, key2, row1, row2);

            if (relatedValue != null) {

                final String alsoKnownAsKey = String.format("%s=%s", key1, key2); // TWO keys equal this value
                
                return new Relation(USER,
                                    alsoKnownAsKey,
                                    relatedValue,
                                    CROSS_SCHEMA);
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
    


    
    /** debug */ static String quoteKey(String s)          { return Util.TERM_GREEN + s + Util.TERM_CLEAR; }
    /** debug */ static String quoteKey(Field f)           { return Util.TERM_GREEN + f + Util.TERM_CLEAR; }
    /** debug */ static String quoteVal(String s)          { return Util.quote(s, Util.TERM_RED); }
    /** debug */ static String quoteKV(String k, String v) { return quoteKey(k) + "=" + quoteVal(v); }
    /** debug */ static String quoteKV(Field f, String v)  { return quoteKey(f) + "=" + quoteVal(v); }
    
    
}
