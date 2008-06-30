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
 * PropertyEntry.java
 *
 * Created on February 5, 2004, 6:39 PM
 */

package tufts.vue;

/**
 * Class to save and restore properties
 * @author  akumar03
 */
public class PropertyEntry {
    
    /** Creates a new instance of PropertyEntry */
    public PropertyEntry() {}
    public PropertyEntry(String key, Object value) {
        entryKey = key;
        entryValue = value;
    }
    
    String entryKey;
    Object entryValue;
    
    public String getEntryKey() {
        return this.entryKey;
    }
    
    public void setEntryKey(String entryKey) {
        this.entryKey = entryKey;
    }
    
     public Object getEntryValue() {
        return this.entryValue;
    }
    
    public void setEntryValue(Object entryValue) {
        this.entryValue = entryValue;
    }

    public String toString() {
        return "PropertyEntry[" + entryKey + "=" + entryValue + "]";
    }

    // this for castor hacks
    public Object getNull() { return null; }
    
    
    
}
