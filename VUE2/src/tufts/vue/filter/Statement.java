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
        return getKey().getType().compare(this, statement2);
    }
        
}
