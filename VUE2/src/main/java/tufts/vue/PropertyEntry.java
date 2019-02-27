 /*
* Copyright 2003-2010 Tufts University  Licensed under the
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
 * PropertyEntry.java
 *
 * Created on February 5, 2004, 6:39 PM
 */

package tufts.vue;

import java.util.Map;


/**
 * Basic {@link Map.Entry} implmentation.
 */
class KVEntry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    
    public KVEntry(K key, V value) {
        this.key   = key;
        this.value = value;
    }

    public KVEntry(Map.Entry<K,V> e) {
        this.key   = e.getKey();
        this.value = e.getValue();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public int hashCode() {
        return ((key   == null)   ? 0 :   key.hashCode()) ^
               ((value == null)   ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        final Map.Entry e = (Map.Entry) o;
        final Object k1 = getKey();
        final Object k2 = e.getKey();
        if (k1 == k2 || (k1 != null && k1.equals(k2))) {
            final Object v1 = getValue();
            final Object v2 = e.getValue();
            if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                return true;
        }
        return false;
    }

    /** must have for persistance */
    public KVEntry() {}
    
}


/**
 * Class to save and restore properties (used with Castor XML persistance)
 *
 * @author  akumar03
 */
public final class PropertyEntry extends KVEntry<String,Object> {
    
    /** will convert the key and value to their String values */
    public PropertyEntry(Map.Entry e) {
        this(e.getKey().toString(), e.getValue().toString());
    }
    /** will currently convert the Object value to a String value, or empty string if null */
    public PropertyEntry(String key, Object value) {
        super(key, value == null ? "" : value.toString());
    }
    /** will currently convert the key and value Objects to String values, or empty string if null */
    public PropertyEntry(Object key, Object value) {
        super(key.toString(), value == null ? "" : value.toString());
    }
    /** must have for persistance */
    public PropertyEntry() {}
    
    public String getEntryKey() {
        return getKey();
    }
    
    public void setEntryKey(String entryKey) {
        super.key = entryKey;
    }
    
    public Object getEntryValue() {
        return getValue();
    }
    
    public void setEntryValue(Object entryValue) {
        
//         if (DEBUG.Enabled && entryValue instanceof String) {
//             super.setValue(stripBrackets((String)entryValue));
//             return;
//         }
            
        super.setValue(entryValue);
    }

    // for debug: some property lists were wrapped in one or more '[]' bracket pairs --
    // each time they'd been persisted as a collection
    private static String stripBrackets(String s) {

        if (s == null || s.length() < 2 || s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']')
            return s;

        while (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            System.err.println("stripping " + s);
            s = s.substring(1, s.length() - 1);
        }
        
        System.err.println("strippedTo " + s);
        return s;
    }

    @Override
    public String toString() {
        return "PropertyEntry[" + key + "=" + value + "]";
    }

    /** for backward-compat castor hacks */
    public Object getNull() { return null; }

}
