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
package tufts.vue.gui;

import tufts.vue.Actions;
import tufts.vue.action.*;
import javax.swing.Box;
import javax.swing.JButton;

/**
 * Toolbar for general VUE actions.
 *
 * @version $Revision: 1.4 $ / $Date: 2010-02-03 19:15:47 $ / $Author: mike $ 
 */
public class VueActionBar extends javax.swing.Box
{
    public VueActionBar()
    {
	super(javax.swing.BoxLayout.X_AXIS);
        add(Actions.NewMap);
        add(new OpenAction());
        add(new SaveAction());
        add(new PrintAction()); // deal with print singleton issue / getactioncommand is null here
        //addSeparator(); // not doing much
        add(Actions.Undo);
        add(Actions.Redo);
        //add(Actions.Group);
        //add(Actions.Ungroup);
        add(Actions.ZoomIn);
        add(Actions.ZoomOut);
        add(Actions.ZoomFit);
        add(Actions.Delete);
        add(Box.createHorizontalStrut(3));
    }

    public JButton add(javax.swing.Action a) {
        JButton b = makeButton(a);
        super.add(b);
        return b;
    }

    private static JButton makeButton(javax.swing.Action a) {
        VueButton b = new VueButton(a);
        b.setAsToolbarButton(true);
        return b;
    }
}

