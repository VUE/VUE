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

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;

public class PathwayTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public PathwayTool() {
        super();
    }
    
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (DEBUG.TOOL) System.out.println("PATHWAYTOOL " + e);
        VUE.sMapInspector.showTab("Pathway");
    }


    public boolean supportsSelection() { return true; }

    // todo: need selection, but no drag, and click-to-deselect still working
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }

    public JPanel getContextualPanel() {
        if (sControlPanel == null)
            sControlPanel = new PathwayToolPanel();
        return sControlPanel;
    }
    
    private static class  PathwayToolPanel extends VueUtil.JPanel_aa {
        public PathwayToolPanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel label = new JLabel("Pathway playback:  ");
            label.setBorder(new EmptyBorder(3,0,0,0));
            add(label);
            JPanel controls = new PathwayPanel.PlaybackToolPanel();
            //controls.setBackground(Color.red);
            controls.setOpaque(false); // so we use parents background fill color
            add(controls);
            //add(Box.createHorizontalGlue());
            add(Box.createHorizontalStrut(22));
        }
    };
    
}
