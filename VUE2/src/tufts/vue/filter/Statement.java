/*
 * Statement.java
 *
 * Created on February 13, 2004, 3:31 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class Statement {
    
    /** Creates a new instance of Statement */
    //Type type;
    Operator operartor;
    Key key;
    Object value;
    public Statement() {
    }
    
    public Statement(String key,String value) {
        setKey(new Key(key, TypeFactory.getStringType()));
        setOperator(TypeFactory.getStringType().getDefaultOperator());
        setValue(value);
    }
    
    /**
    public void setType(Type type) {
        this.type= type;
    }
    
    public Type getType() {
        return this.type;
    }
     */
    
    public void setOperator(Operator operator) {
        this.operartor = operator;
    }
    
    public Operator getOperator() {
        return this.operartor;
    }
    
    public void setKey(Key key) {
        this.key = key;
    }
    
    public Key getKey() {
        return this.key;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public Object getValue() {
       return this.value;
    }
    
  
    public boolean compare(Statement statement2) {
        return getOperator().compare(this, statement2);
    }
        
}
