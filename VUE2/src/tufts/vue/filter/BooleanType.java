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
 * BooleanType.java
 *
 * Created on February 28, 2004, 5:59 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class BooleanType extends DefaultType {
    
    public static String OP_EQUAL = "equals";
    public static String OP_NOT_EQUAL = "not equals";
    /** Creates a new instance of BooleanType */
    public BooleanType() {
        super(Type.BOOLEAN_TYPE);
        Operator eqOperator = new DefaultOperator(OP_EQUAL ,"is",true) ;
        Operator neOperator = new DefaultOperator(OP_NOT_EQUAL,"is not",false);
        operatorList.add(eqOperator);
        operatorList.add(neOperator);
    }
    public boolean compare(Statement s1,Statement s2) {
        Object value1  = s1.getValue();
        Object value2 = s2.getValue();
        if(!(value1 instanceof String) || !(value2 instanceof String))
            return false;
        boolean v1 = ((Boolean)value1).booleanValue();
        boolean v2 = ((Boolean)value2).booleanValue();
        if(s1.getOperator().getDisplayName().equals(OP_EQUAL)){
            if(s2.getOperator().getDisplayName().equals(OP_EQUAL)) {
                return v1 && v2;
            } else if (s2.getOperator().getDisplayName().equals(OP_NOT_EQUAL)) {
                return v1 && !v2;
            }
        }
        throw new RuntimeException("Not Implemented");
    }
    
}
