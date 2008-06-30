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
        return new IntegerType();
        /**
        Operator eqOperator = new DefaultOperator("equal","=",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s2.getValue();
                System.out.println("COMPARING "+ value1+ " AND " + value2+ "CLASSES ="+value1.getClass()); 
                if(!(value1 instanceof Integer) || !(value2 instanceof Integer))
                    return false;
                else {
                    int v1 = ((Integer)value1).intValue();
                    int v2 = ((Integer)value2).intValue();
                    System.out.println("COMPARING "+ v1+ " AND " + v2);
                
                    if(v1 == v2) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Operator gtOperator = new DefaultOperator("smaller","<",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s2.getValue();
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
                Object value2 = s2.getValue();
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
                      
        Type type = new DefaultType(Type.INTEGER_TYPE);
        type.getOperators().add(eqOperator);   
        type.getOperators().add(gtOperator);
        type.getOperators().add(ltOperator);
        return type;
         */
    }
    
    
    
    public static Type getStringType() {
        return new StringType();
        /**
       Operator eqOperator = new DefaultOperator("equals","equals",true) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s2.getValue();
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
        Operator stOperator = new DefaultOperator("starts","starts with",false) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s2.getValue();
                if(!(value1 instanceof String) || !(value2 instanceof String))
                    return false;
                else {
                    String v1 = (String) value1;
                    String v2 = (String) value2;
                    if(v1.startsWith(v2)) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Operator endOperator = new DefaultOperator("ends","ends with",false) {
            public boolean compare(Statement s1,Statement s2) {
                Object value1  = s1.getValue();
                Object value2 = s2.getValue();
                if(!(value1 instanceof String) || !(value2 instanceof String))
                    return false;
                else {
                    String v1 = (String) value1;
                    String v2 = (String) value2;
                    if(v1.endsWith(v2)) 
                        return true;
                    else 
                        return false;
                }
            }
        };
        Type type = new DefaultType(Type.STRING_TYPE);
        type.getOperators().add(eqOperator);  
        type.getOperators().add(stOperator);
        type.getOperators().add(endOperator);
        return type;
         */
    }
    public static Type getBooleanType() {
        return new BooleanType();
    }
    // currently returns only string and integer types;
    public static List getAllTypes() {
        List typeList = new Vector();
        typeList.add(getIntegerType());
        typeList.add(getStringType());
        //typeList.add(getBooleanType());
        return typeList;
    }
}
