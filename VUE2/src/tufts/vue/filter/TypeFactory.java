/*
 * TypeFactory.java
 *
 * Created on February 13, 2004, 4:08 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
public class TypeFactory {
    
    /** Creates a new instance of TypeFactory */
    public static Type getIntegerType() {
        Operator eqOperator = new DefaultOperator("equal","=",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s1.getValue();
                if(!(value1 instanceof Integer) || !(value2 instanceof Integer))
                    return false;
                else {
                    int v1 = ((Integer)value1).intValue();
                    int v2 = ((Integer)value2).intValue();
                    if(v1 == v2) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Operator gtOperator = new DefaultOperator("greater",">",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s1.getValue();
                if(!(value1 instanceof Integer) || !(value2 instanceof Integer))
                    return false;
                else {
                    int v1 = ((Integer)value1).intValue();
                    int v2 = ((Integer)value2).intValue();
                    if(v1 > v2) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Operator ltOperator = new DefaultOperator("greater",">",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s1.getValue();
                if(!(value1 instanceof Integer) || !(value2 instanceof Integer))
                    return false;
                else {
                    int v1 = ((Integer)value1).intValue();
                    int v2 = ((Integer)value2).intValue();
                    if(v1 < v2) 
                        return true;
                    else 
                        return false;
                }
            }
        };
                      
        Type type = new DefaultType("integer");
        type.getOperators().add(eqOperator);   
        type.getOperators().add(gtOperator);
        type.getOperators().add(ltOperator);
        return type;
    }
    
    
    
    public static Type getStringType() {
       Operator eqOperator = new DefaultOperator("equals","equals",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s1.getValue();
                if(!(value1 instanceof String) || !(value2 instanceof String))
                    return false;
                else {
                    String v1 = (String) value1;
                    String v2 = (String) value2;
                    if(v1.equals(v2)) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Type type = new DefaultType("String");
        type.getOperators().add(eqOperator);   
        return type;
    }
    // currently returns only string and integer types;
    public static List getAllTypes() {
        List typeList = new Vector();
        typeList.add(getIntegerType());
        typeList.add(getStringType());
        return typeList;
    }
}
