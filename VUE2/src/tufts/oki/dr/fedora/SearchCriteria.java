/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * SearchCriteria.java
 *
 * Created on November 1, 2003, 9:48 AM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */
import fedora.server.types.gen.*;

public class SearchCriteria implements java.io.Serializable {
    public static final String FIND_OBJECTS = "findObjects";
    public static final String RESUME_FIND_OBJECTS = "resumeFindObjects";
    
    private String keywords = null;
    private String maxReturns = null;
    private Condition[]  conditions = null;
    private String searchOperation = null; // field to browse through results
    private String token = null; // field to track session(its session id)
    private int results = 0;
    
    /** Creates a new instance of SearchCriteria */
    public SearchCriteria() {
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    public String getKeywords() {
        return this.keywords;
    }
    
    public void setMaxReturns(String maxReturns) {
        this.maxReturns = maxReturns;
    }
    public String getMaxReturns() {
        return this.maxReturns;
    }
    
    
    public void setSearchOperation(String searchOperation) {
        this.searchOperation = searchOperation;
    }
    public String getSearchOperation() {
        return this.searchOperation;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return this.token;
    }
    
    public void setConditions(Condition[] conditions) {
        this.conditions = conditions;
    }
    
    public Condition[] getConditions() {
        return this.conditions;
    }
    public void setResults(int results) {
        this.results = results;
    }
    
    public int getResults() {
        return this.results;
    }
    
}
