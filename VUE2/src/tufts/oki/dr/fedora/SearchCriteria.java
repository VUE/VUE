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
    
    private String keywords = null;
    private String maxReturns = null;
    private Condition[]  conditions = null;
    
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
    
    public void setConditions(Condition[] conditions) {
        this.conditions = conditions;
    }
    
    public Condition[] getConditions() {
        return this.conditions;
    }
    
}
