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