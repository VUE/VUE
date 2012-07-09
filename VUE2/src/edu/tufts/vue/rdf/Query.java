
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

/**
 * A Class to support coplex queries throught sparql
 * It supports boolean AND / OR operation
 * and REGEX MODE
 * By default the query is limitimg, which means it returns results that match
 * all the criteria
 * @author akumar03
 *
 */

package edu.tufts.vue.rdf;

import java.util.*;
import tufts.Util;
import tufts.vue.DEBUG;

/**
 * Query utility class for VUE to provide a simple API for building up a SPARQL query.
 * Could probably skip this and just an existing impl of com.hp.hpl.jena.query.Query
 *
 * Note that this class is poorly encapsulated: the literal values in the query it constructs:
 * e.g., "resource" / "keyword", or "rid" / "val", or whatever they are, have to be hardcoded
 * elsewhere in processing the results of QuerySolution(s).
 *
 * This class might better be called something like QueryBuilder or SPARQLBuilder
 * -- can cause confusion with com.hp.hpl.jena.query.Query.
 */
public class Query  {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Query.class);

    public enum Qualifier { STARTS_WITH, CONTAINS,MATCH, WITHOUT,MATCH_CASE };

    // These appear to have never been used.
    //public enum Operation { AND , OR }
    //private boolean regex = true;
    //private Operation oper  = Operation.AND;

    private final List<Criteria> criteriaList = new ArrayList<Criteria>();
    
    /** Creates a new instance of Query */
    public Query() {}
    
    public void addCriteria(String key,String value) {
        if (DEBUG.SEARCH) Log.debug("addCriteria " + key + "=" + Util.tags(value));
        Criteria criteria = new Criteria(key,value);
        criteriaList.add(criteria);
    }
    
    public void addCriteria(String key, String value, String condition){
        if (DEBUG.SEARCH) Log.debug("addCriteria " + key + "=" + Util.tags(value) + " cond=" + Util.tags(condition));
        Criteria criteria = new Criteria(key,value,Qualifier.valueOf(condition));
        criteriaList.add(criteria);
    }

    //private static final String QueryExprFmt = "?resource <%s> ?keyword%d FILTER regex(?keyword%d, \"%s%s\", \"i\") .";
    //private static final String QueryExprFmt = "?rid <%s> ?val%d FILTER regex(?val%d, \"%s%s\", \"i\") .";
    
    private static final String QueryStart = "PREFIX vue: <"+RDFIndex.VUE_ONTOLOGY+">" + "\nSELECT ?rid ?val WHERE { ";
    private static final String QueryForProperty = "?rid <%s> ?val FILTER regex(?val, \"%s%s\", \"i\") .";

    // "i" means case-independent
    // Todo: would be nice to be able to handle stuff like: FILTER (?age >= 24)
    
    public String createSPARQLQuery() {
        final StringBuilder query = new StringBuilder(QueryStart.length() + QueryForProperty.length() + 16);
        final boolean multiLine = true; // criteriaList.size() > 1; // for diagnostic readability
        query.append(QueryStart);
        if (multiLine) query.append('\n');
        int i = 0;
        for (Criteria criteria : criteriaList) {
            i++;
            final String encodedPropertyKey = RDFIndex.getEncodedKey(criteria.key); // better done at critera construct time?
            final String regexPrefix;

            // if (encodedKey.startsWith(RDFIndex.VUE_ONTOLOGY))
            //     encodedKey = "vue:" + encodedKey.substring(RDFIndex.VUE_ONTOLOGY.length()); // remove angle brackets as well?
            
            switch (criteria.qualifier) {
                case STARTS_WITH: regexPrefix = "^"; break;
                default:          regexPrefix = "";
            }
            if (multiLine) query.append("  ");
            query.append(String.format(QueryForProperty, encodedPropertyKey, regexPrefix, criteria.value));
          //query.append(String.format(QueryExprFmt,encodedPropertyKey, i,i, regexPrefix, criteria.value));
          // Sequentially naming the query vars is just another way to distinguish what was found in the results.
            if (multiLine) query.append('\n');
        }
        query.append('}');
        return query.toString();
    }
    
    class Criteria {
        String key;
        Qualifier qualifier;
        String value;
        public Criteria(String key, String value) {
            this(key,value,Qualifier.CONTAINS);
        }
        
        public Criteria(String key, String value, Qualifier qualifier) {
            this.key = key;
            this.value = value;
            this.qualifier  =     qualifier;
        }

        // public String toString() {
        //     return "Criteria[" + key + "=" + value + "]; qual=" + qualifier;
        // }
    }
    
}
