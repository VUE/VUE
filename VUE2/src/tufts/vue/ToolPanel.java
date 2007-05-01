package tufts.vue;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Replacement for old LWCToolPanel, mainly for backward compat with some old code.
 * Ultimately, we probably don't need this class at all.  -- SMF 2007-04-30
 *
 * @version $Revision: 1.1 $ / $Date: 2007-05-01 04:38:07 $ / $Author: sfraize $  
 */

public class ToolPanel extends JPanel
{        
    protected JPanel mBox;
   
    public ToolPanel()
    {
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         
         setBorder(new EmptyBorder(2,1,2,1));//t,l,b,r
         
         setOpaque(false);
         mBox = new JPanel();
         mBox.setLayout(new java.awt.GridBagLayout());
         tufts.vue.gui.GUI.applyToolbarColor(mBox);
         buildBox();
         add(mBox);
    }

    protected void buildBox() {}
    
    protected javax.swing.JComponent getBox() {
        return mBox;
    }
    
    /** @deprecated */
    public boolean addComponent(java.awt.Component c) {
        System.out.println("ToolPanel:addComponent IGNORED " + c);
        return true;
    }
    
}
