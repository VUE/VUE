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

import javax.swing.*;
import javax.swing.border.*;

/**
 * Replacement for old LWCToolPanel, mainly for backward compat with some old code.
 * Ultimately, we probably don't need this class at all.  -- SMF 2007-04-30
 *
 * @version $Revision: 1.7 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $  
 */

public class ToolPanel extends JPanel
{        
    protected JPanel mBox;
   
    public ToolPanel()
    {
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         setBorder(new EmptyBorder(0,2,0,3));//t,l,b,r
         
         setOpaque(false);
         mBox = new JPanel();
         mBox.setLayout(new java.awt.GridBagLayout());
         //tufts.vue.gui.GUI.applyToolbarColor(mBox); // was only for the old perma-docked toolbar
         //buildBox();
         //add(mBox);
    }

    public void addNotify() {
        buildBox();
        add(mBox);
        super.addNotify();
    }

    protected void buildBox() {}
    
    protected javax.swing.JComponent getBox() {
        return mBox;
    }
    
    /** @deprecated */
    public boolean addComponent(java.awt.Component c) {
        tufts.Util.printStackTrace("ToolPanel:addComponent IGNORED " + c);
        return true;
    }
    
}
