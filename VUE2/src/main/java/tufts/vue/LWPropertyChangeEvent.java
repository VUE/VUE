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
 * A marker class for VUE property change events, so we can distinguish them
 * from regular AWT/Swing property change events, which we need to do when
 * we make use of the property change support built into java.awt.Component.
 */

// rename LWEditorChangeEvent?
public class LWPropertyChangeEvent extends java.beans.PropertyChangeEvent
{
    public final Object key;
    
    //    public LWPropertyChangeEvent(Object source, Object propertyKey, Object oldValue, Object newValue) {
    //    super(source, propertyKey.toString(), oldValue, newValue);
    //}
    public LWPropertyChangeEvent(Object source, Object propertyKey, Object newValue) {
        this(source, propertyKey, null, newValue);
    }
    public LWPropertyChangeEvent(Object source, Object propertyKey, Object oldValue, Object newValue) {
        super(source, propertyKey.toString(), oldValue, newValue);
        this.key = propertyKey;
    }

    public String toString() {
        return "LWPropertyChangeEvent[" + key + " src=" + source + "]";
    }
}