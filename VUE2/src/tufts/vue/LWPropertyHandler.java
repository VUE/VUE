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

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Helper class for creating an LWPropertyProducer.
 * Handles an action-event, & firing a property change event with
 * after producing the new property value (via getPropertyValue()).
 */

public abstract class LWPropertyHandler
    implements LWPropertyProducer, ActionListener
{
    private final Object mPropertyKey;
    private final PropertyChangeListener mChangeListener;
    
    public LWPropertyHandler(Object propertyKey, PropertyChangeListener listener) {
        mPropertyKey = propertyKey;
        mChangeListener = listener;
    }
    public Object getPropertyKey() { return mPropertyKey; }
    public abstract Object getPropertyValue();
    /** load the property value into the property producer */
    public abstract void setPropertyValue(Object value);
    
    public void actionPerformed(ActionEvent ae) {
        Object newValue = getPropertyValue();
        mChangeListener.propertyChange
            (new LWPropertyChangeEvent(ae.getSource(),
                                       mPropertyKey,
                                       null, // no old value for now
                                       newValue));
    }
}
