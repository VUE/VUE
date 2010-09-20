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

/*
 * DefaultOperator.java
 *
 * Created on February 13, 2004, 3:43 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class DefaultOperator implements Operator {
    
    /** Creates a new instance of DefaultOperator */
    
    String displayName = "equals";
    String symbol = "=";
    boolean settable = true;;
    public DefaultOperator() {
    }
    
    public DefaultOperator(String displayName,String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }
    public DefaultOperator(String displayName,String symbol,boolean settable) {
       this(displayName, symbol);
       this.settable = settable;
    }
    
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public String getSymbol() {
        return this.symbol;
    }
    
    /*
     * compares statement1 and statment 2 and returns true iff statement2 is true given statement 1
     */
    
    public boolean compare(Statement s1,Statement s2) {
        return s1.equals(s2);
    }
    
    /*
     * returns true if operator can be set by  user.  Some operators like contains, matches  
     * may be used only  for filtering.  The default value shall be false.
     *
     */
    
    public boolean isSettable(){
        return settable;
    }
    
    public void setSettable(boolean settable) {
        this.settable = settable;
    }
    
    public String toString() {
        return symbol;
    }
}
