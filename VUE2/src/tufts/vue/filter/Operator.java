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

