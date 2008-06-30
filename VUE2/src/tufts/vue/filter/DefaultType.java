/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
 * DefaultType.java
 *
 * Created on February 13, 2004, 3:53 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
public class DefaultType implements tufts.vue.filter.Type {
    
    /** Creates a new instance of DefaultType */
    
    String displayName;
    protected List operatorList = new Vector();
    public DefaultType() {
    }
    
    public DefaultType(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return this.displayName;
    }
    
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public java.util.List getOperators() {
        return operatorList;
    }
    
    public void setOperators(List operatorList) {
        this.operatorList = operatorList;
    }
    
    public void addOperator(Operator operator) {
        operatorList.add(operator);
    }
    
    public void removeOperator(Operator operator) {
        operatorList.remove(operator);
    }
    
    public java.util.List getSettableOperators() {
        List settableOperatorList = new Vector();
        Iterator i = operatorList.iterator();
        while(i.hasNext()) {
            Operator operator = (Operator) i.next();
            if(operator.isSettable())
                settableOperatorList.add(operator);
        }
        return settableOperatorList;
    }
    /** first operator is the default operator.  This can be changed later.
     */
    public Operator getDefaultOperator() {
        if(operatorList.size() > 0)
            return (Operator) operatorList.get(0);
        else 
            throw new RuntimeException(displayName+": Type has no operators");
    }
    //todo allow only certain type of keys and values.  This is now checked while entering the data.
    public boolean isValidKey() {
        return true;
        
    } 
    public boolean isValidValue() {
        return true;
    }
    

    public String toString() {
        return displayName;
    }
    
     public boolean compare(Statement s1,Statement s2) {
         throw new RuntimeException("Not implemented");
     }
    
}
