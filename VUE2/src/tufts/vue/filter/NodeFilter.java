/*
 * NodeFilter.java
 *
 * Created on February 14, 2004, 2:55 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */

import java.util.Vector;
public class NodeFilter extends Vector {
    
    /** Creates a new instance of NodeFilter */
    public NodeFilter() {
    }
    
     public synchronized void add(Statement statement) {
        super.add(statement);
    }
    
    public boolean add(Object o) {
        throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    }
}
