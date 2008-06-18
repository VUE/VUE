/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
package tufts.vue;

/**
 * @see tufts.vue.ActiveChangeSupport
 * @see tufts.vue.ActiveEvent
 * @author Scott Fraize
 * @version $Revision: 1.5 $ / $Date: 2008-06-18 02:38:04 $ / $Author: sfraize $
 */
public class ActiveEvent<T> {
    public final Class<T> type;
    public final Object source;
    public final T active;
    public final T oldActive;

    ActiveEvent(Class<T> type, Object source, T oldActive, T newActive) {
        this.type = type;
        this.source = source;
        this.oldActive = oldActive;
        this.active = newActive;
    }

//     public boolean hasSource(Object o) {
//         if (source == o)
//             return true;
//         else if (source instanceof ActiveEvent)
//             return ((ActiveEvent)source).hasSource(o);
//         else
//             return false;
//     }

//     public boolean hasSourceOfType(Class clazz) {
//         if (clazz.isInstance(source))
//             return true;
//         else if (source instanceof ActiveEvent)
//             return ((ActiveEvent)source).hasSourceOfType(clazz);
//         else
//             return false;
//     }

    /**

     * @return true if this represents some kind of (unknown) change event *on the
     * active item*, and thus listeners may want to refresh any state they determine
     * from the state of the active instance itself.  In this case, that active instance
     * has NOT changed. So this returns true if oldActive == active.
     
     */
    public boolean isRefresh() {
        // This may seem counterintuitive; see above method comment.
        return active == oldActive;
    }
    

    public String toString() {
        return "ActiveEvent<" + type.getName() + ">[src=" + source + "; active=" + active + "]";
    }
    
}
