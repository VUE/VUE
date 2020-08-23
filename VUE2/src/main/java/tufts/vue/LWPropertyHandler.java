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

import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Helper class for creating an LWPropertyProducer.
 * Handles an action-event, & firing a property change event with
 * after producing the new property value (via getPropertyValue()).
 *
 * Subclass implementors must provide produceValue/displayValue from LWEditor.
 */

// LWEditorChangeHandler?
public abstract class LWPropertyHandler<T>
    implements LWEditor<T>, java.awt.event.ActionListener
{
    private final Object key;
    private final java.awt.Component[] gui;
    //private final PropertyChangeListener changeListener;
    
    //public LWPropertyHandler(Object propertyKey, PropertyChangeListener listener, java.awt.Component gui) {
    public LWPropertyHandler(Object propertyKey, java.awt.Component... gui) {
        this.key = propertyKey;
        //this.changeListener = listener;
        this.gui = gui;
        // as this may not be in the AWT hierarchy to be found by the manager,
        // we register it manually:
        EditorManager.registerEditor(this);
    }

    /*
    public LWPropertyHandler(Object propertyKey, java.awt.Component gui) {
        this(propertyKey, null, gui);
    }
    */
    public LWPropertyHandler(Object propertyKey) {
        this(propertyKey, (java.awt.Component) null);
        //this(propertyKey, null, null);
    }
    
    public Object getPropertyKey() { return key; }

    public void setEnabled(boolean enabled) {
        if (gui != null) {
            for (java.awt.Component c : gui) {
                c.setEnabled(enabled);
            }
            //gui.setEnabled(enabled);
        } else
            throw new UnsupportedOperationException(this + ": provide a gui component, or subclass should override setEnabled");
    }

    //public void itemStateChanged(java.awt.event.ItemEvent e) {
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (DEBUG.TOOL) System.out.println(this + " actionPerformed " + e.paramString());
        EditorManager.firePropertyChange(this, e.getSource());
    }

    public String toString() {
        Object value = null;
        try {
            value = produceValue();
        } catch (Throwable t) {
            value = t.toString();
        }
        return getClass().getName() + ":LWPropertyHandler[" + getPropertyKey() + "=" + value + "]";
    }
}
