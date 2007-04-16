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

package tufts.vue.gui;

import tufts.vue.DEBUG;

import javax.swing.Icon;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

/**
 * An exension of MenuButton that specifically handles pop-up menu
 * for a specific VUE LWComponent property (LWKey property)
 *
 * @version $Revision: 1.18 $ / $Date: 2006/01/20 17:17:29 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class VuePopupMenu<T> extends MenuButton<T>
{
    public VuePopupMenu(Object propertyKey, Object[] valuesOrActions)
    {
        setPropertyKey(propertyKey);
        buildMenu(valuesOrActions);
        displayValue(getMenuValueAt(0));
        setName(propertyKey.toString());
    }

    public T getMenuValueAt(int index) {
        JComponent c = (JComponent) mPopup.getComponent(0);
        return (T) c.getClientProperty(ValueKey);
    }

    public void displayValue(T newValue) {
        if (DEBUG.TOOL) System.out.println(this + " displayValue " + newValue);
        if (mCurrentValue == null || !mCurrentValue.equals(newValue)) {
            mCurrentValue = newValue;
            Icon i = getIconForPropertyValue(newValue);
            if (i != null)
                setButtonIcon(i);
        }
    }

    public Icon getIconForPropertyValue(T value) {
        int count = mPopup.getComponentCount();
        for (int i = 0; i < count; i++) {
            JComponent c = (JComponent) mPopup.getComponent(i);
            if (c instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) c;
                if (mi.getClientProperty(ValueKey).equals(value))
                    return mi.getIcon();
            }
        }
        return null;
    }
    
    

}
