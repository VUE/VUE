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
import tufts.vue.NodeTool;

import javax.swing.Icon;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

/**
 * 
 * TODO: should probably be merged with ComboBoxMenuButton
 *
 * @version $Revision: 1.3 $ / $Date: 2007-05-02 02:55:55 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class VueComboMenu<T> extends ComboBoxMenuButton<T>
{
    public VueComboMenu(Object propertyKey, Object[] valuesOrActions)
    {
        setPropertyKey(propertyKey);
        buildMenu(valuesOrActions);
        displayValue(getMenuValueAt(0));
        setName(propertyKey.toString());
    }
}
