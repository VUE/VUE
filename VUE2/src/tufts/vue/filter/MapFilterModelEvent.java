/*
 * MapFilterModelEvent.java
 *
 * Created on March 2, 2004, 9:40 PM
 */

package tufts.vue.filter;

/**
 *
 * @author  akumar03
 */
public class MapFilterModelEvent {
    
    public static int KEY_ADDED = 0;
    public static int KEY_DELETED = 1;
    
    Key key = null;
    int action;
    /** Creates a new instance of MapFilterModelEvent */
    public MapFilterModelEvent(Key key,int action) {
        this.key = key;
        this.action = action;
    }

    public Key getKey() {
        return this.key;
    }
    public int getAction(){
        return action;
    }
}
