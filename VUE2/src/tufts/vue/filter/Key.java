/*
 * Key.java
 *
 * Created on February 14, 2004, 6:38 AM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class Key {
    
    /** Creates a new instance of Key */
    Type type;
    Object key;
    Object defaultValue;
    public Key() {
    }
    
    public Key(String key,Type type) {
        this.type = type;
        this.key = key;
        if(type.getDisplayName().equals(Type.INTEGER_TYPE))
            defaultValue = new Integer(0);
        else if(type.getDisplayName().equals(Type.BOOLEAN_TYPE))
            defaultValue = new Boolean(true);
        else 
            defaultValue = new String("");
    }
     
    public void setType(Type type) {
        this.type= type;
    }
    
    public Type getType() {
        return this.type;
    }
    
     public void setKey(Object key) {
        this.key = key;
    }
    
    public Object getKey() {
        return this.key;
    }
    
    public String toString() {
        return key.toString();
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
}
