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
package tufts.vue;

/**
 * @see tufts.vue.ActiveChangeSupport
 * @see tufts.vue.ActiveEvent
 *
 * If a particular class impl is only interested in a single type of activeChanged,
 * it can use the type information in it's implementation.  If it is interested
 * in updates on more than one type of active object, it can implement
 * the generic version and check the type information itself in the callback.
 *
 * @author Scott Fraize
 * @version $Revision: 1.4 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 */
public interface ActiveListener<T> extends java.util.EventListener {
    public void activeChanged(ActiveEvent<T> e);
}
    
        
