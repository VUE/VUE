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
 * MapFilterModelEvent.java
 * MapFilterModelEvent is generated when a Key is added or deleted from MapFilterModel.
 * It stores the MapFilterModel that generated the event, the Key and integer to 
 * identify addition or deletion
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
    MapFilterModel model = null;
    int action;
    /** Creates a new instance of MapFilterModelEvent */
    public MapFilterModelEvent(MapFilterModel model,Key key,int action) {
        this.key = key;
        this.action = action;
        this.model = model;
    }

    public Key getKey() {
        return this.key;
    }
    public int getAction(){
        return action;
    }
    
    public MapFilterModel getMapFilterModel() {
        return this.model;
    }
}
