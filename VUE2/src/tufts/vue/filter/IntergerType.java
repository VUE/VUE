/*
 * IntergerType.java
 *
 * Created on February 28, 2004, 5:38 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class IntergerType extends DefaultType {
    public static String OP_EQUAL = "equals";
    public static String OP_GREATER = "greater";
    public static String OP_SMALLER = "smaller";
    /** Creates a new instance of IntergerType */
    public IntergerType() {
        super(Type.INTEGER_TYPE);
        Operator eqOperator = new DefaultOperator(OP_EQUAL,"=",true);
        Operator gtOperator = new DefaultOperator(OP_SMALLER,"<",true);
        Operator ltOperator = new DefaultOperator(OP_GREATER,">",true);
        operatorList.add(eqOperator);
        operatorList.add(gtOperator);
        operatorList.add(ltOperator);
    }
    
    public boolean compare(Statement s1,Statement s2) {
        Object value1  = s1.getValue();
        Object value2 = s2.getValue();
        if(!(value1 instanceof Integer) || !(value2 instanceof Integer))
            return false;
        int v1 = ((Integer)value1).intValue();
        int v2 = ((Integer)value2).intValue();
        if(s1.getOperator().getDisplayName().equals(OP_EQUAL)) {
             if(s2.getOperator().getDisplayName().equals(OP_EQUAL)) {
                 return v1 == v2;
             } else if(s2.getOperator().getDisplayName().equals(OP_GREATER)) {
                 return v1> v2;
             } else if(s2.getOperator().getDisplayName().equals(OP_SMALLER)) {
                 return v1 < v2;
             }
        } else if(s1.getOperator().getDisplayName().equals(OP_GREATER)) {
             if(s2.getOperator().getDisplayName().equals(OP_EQUAL)) {
                 return v1 > v2;
             } else if(s2.getOperator().getDisplayName().equals(OP_GREATER)) {
                 return v1 >= v2;
             } else if(s2.getOperator().getDisplayName().equals(OP_SMALLER)) {
                 return false;
             }
        }else if(s1.getOperator().getDisplayName().equals(OP_SMALLER)) {
             if(s2.getOperator().getDisplayName().equals(OP_EQUAL)) {
                 return v1 < v2;
             } else if(s2.getOperator().getDisplayName().equals(OP_GREATER)) {
                 return false;
             } else if(s2.getOperator().getDisplayName().equals(OP_SMALLER)) {
                 return v1 <=v2;
             }
        }
        throw new RuntimeException("Not Implemented");
    }
}
