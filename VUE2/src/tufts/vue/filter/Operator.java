/*
 * Class.java
 *
 * Created on February 13, 2004, 2:36 PM
 */

package tufts.vue.filter;

/**
 * 
 * @author  akumar03
 *
 *An operator is used to compare metadata statements.  It has a displayName, Symbol 
 *and compare function. A symbol is short representation of for the operator. It could be =, <, >, ..
 * or eq, gt,lt, ....
 */


public interface Operator {
    
    /** Creates a new instance of Class */
    public void setDisplayName(String displayName);
    public String getDisplayName();
    public void setSymbol(String symbol);
    public String getSymbol();
    
    /*
     * compares statement1 and statment 2 and returns true iff statement2 is true given statement 1
     */
    
    public boolean compare(Statement s1,Statement s2);
    
    /*
     * returns true if operator can be set by  user.  Some operators like contains, matches  
     * may be used only  for filtering.  The default value shall be false.
     *
     */
    
    public boolean isSettable();
}

