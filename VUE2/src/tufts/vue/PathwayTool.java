package tufts.vue;

import java.awt.Color;
import javax.swing.*;
import javax.swing.border.*;

public class PathwayTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public PathwayTool() {
        super();
    }
    
    public boolean supportsSelection() { return true; }

    // todo: need selection, but no drag, and click-to-deselect still working
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return false; }

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
        public void addNotify() {
            super.addNotify();

            // this is a bit of a side-effect, but it works given the
            // way the VueToolPanel manages it's content:
            VUE.sMapInspector.showTab("Pathway");
        }
    };
    
}
