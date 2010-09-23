
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

public class Query  {
    public enum Operation { AND , OR }
    public enum Qualifier {STARTS_WITH, CONTAINS,MATCH, WITHOUT,MATCH_CASE};
    private boolean regex = true;
    private Operation oper  = Operation.AND;
    //private Map<String,String> criteria = new HashMap<String,String>();
    private  List<Criteria> criteriaList = new ArrayList<Criteria>();
    /** Creates a new instance of Query */
    public Query() {
    }
    
    public void addCriteria(String key,String value) {
        Criteria criteria = new Criteria(key,value);
        criteriaList.add(criteria);
    }
    
    public void addCriteria(String key,String value,String condition){
        Criteria criteria = new Criteria(key,value,Qualifier.valueOf(condition));
        criteriaList.add(criteria);
    }
    
    public String createSPARQLQuery() {
        String query = new String();
        query =  "PREFIX vue: <"+RDFIndex.VUE_ONTOLOGY+"> "+
                "SELECT ?resource " +
                "WHERE{";
        int i =0;
        String keyword = "keyword";
        for(Criteria criteria: criteriaList) {
             i++;
            keyword ="keyword"+i;
            
            String encodedKey = RDFIndex.getEncodedKey(criteria.key);
            
            switch(criteria.qualifier)  {
                case CONTAINS:
                    query +=  "?resource <"+ encodedKey +"> ?"+keyword+" FILTER regex(?"+keyword+",\""+criteria.value+ "\",\"i\") . ";
                    break;
                case STARTS_WITH:
                    query +=  "?resource <"+ encodedKey +"> ?"+keyword+" FILTER regex(?"+keyword+",\"^"+criteria.value+ "\",\"i\") . ";
                    break;                 
            }
        }
        query  += "}";
        return query;
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
    }
    
}
