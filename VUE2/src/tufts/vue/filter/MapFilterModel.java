/*
 * MapMetadataModel.java
 *
 * Created on February 14, 2004, 2:40 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
public class MapFilterModel extends Vector {
    
    /** Creates a new instance of MapMetadataModel */
    
    public MapFilterModel() {
    }
    public synchronized void add(Key key) {
        super.add(key);
    }
    
    public boolean add(Object o) {
        throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    }
}
