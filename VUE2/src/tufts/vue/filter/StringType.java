/*
 * StringType.java
 *
 * Created on February 27, 2004, 6:30 PM
 */

package  tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class StringType extends DefaultType {
    public static String OP_EQUAL = "equals";
    public static String OP_CONTAIN = "contains";
    public static String OP_START = "starts";
    public static String OP_END = "ends";
    /** Creates a new instance of StringType */
    public StringType() {
        super(Type.STRING_TYPE);
        Operator eqOperator = new DefaultOperator(OP_EQUAL ,"is",true) ;
        Operator containOperator = new DefaultOperator(OP_CONTAIN,"contains",false);
        Operator stOperator = new DefaultOperator(OP_START,"starts with",false);
        Operator endOperator = new DefaultOperator(OP_END,"ends with",false);
        operatorList.add(eqOperator);
        operatorList.add(containOperator);
        operatorList.add(stOperator);
        operatorList.add(endOperator);
    }
    
    public boolean compare(Statement s1,Statement s2) {
        Object value1  = s1.getValue();
        Object value2 = s2.getValue();
        if(!(value1 instanceof String) || !(value2 instanceof String))
            return false;
        String v1 = ((String) value1).toLowerCase();
        String v2 = ((String) value2).toLowerCase();
        if(s1.getOperator().getDisplayName().equals(OP_EQUAL)){
            if(s2.getOperator().getDisplayName().equals(OP_EQUAL)) {
                return v1.equals(v2);
            } else if (s2.getOperator().getDisplayName().equals(OP_CONTAIN)) {
                return v1.indexOf(v2) != -1;
            }else if(s2.getOperator().getDisplayName().equals(OP_START)) {
                return v1.startsWith(v2);
            } else if(s2.getOperator().getDisplayName().equals(OP_END)) {
                return v1.endsWith(v2);
            }
        }
        throw new RuntimeException("Not Implemented");
    }
}