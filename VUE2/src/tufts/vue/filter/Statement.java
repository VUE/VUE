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
        setValue(key.getDefaultValue());
        setOperator(key.getType().getDefaultOperator());
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

    public String toString() {
        return "Statement[" + key + " " + operartor + " '" + value + "']";
        //return "Statement[op=" + operartor + " key=" + key + " val=" + value + "]";
    }
        
}
