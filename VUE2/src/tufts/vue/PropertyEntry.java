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
    public PropertyEntry() {
    }
    
    Object entryKey;
    Object entryValue;
    
    public Object getEntryKey() {
        return this.entryKey;
    }
    
    public void setEntryKey(Object entryKey) {
        this.entryKey = entryKey;
    }
    
     public Object getEntryValue() {
        return this.entryValue;
    }
    
    public void setEntryValue(Object entryValue) {
        this.entryValue = entryValue;
    }
    
    
    
}
