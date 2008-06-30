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
    Object sKey;
    Object defaultValue;
    public Key() {
    }
    
    public Key(String key,Type type) {
        this.type = type;
        this.sKey = key;
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
        this.sKey = key;
    }
    
    public Object getKey() {
        return this.sKey;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String toString() {
        return sKey.toString();
    }

    /*
    public String toString() {
        return "Key[type="+type + " key=" + sKey + " default=" + defaultValue + "]";
    }
    */
}
