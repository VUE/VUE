/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
 * @version $Revision: 1.5 $ / $Date: 2008-06-30 20:53:05 $ / $Author: mike $
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
