package tufts.vue;

import javax.swing.JPanel;

public class PathwayTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public PathwayTool() {
        super();
    }
    
    public boolean supportsSelection() { return true; }

    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return false; }
    
    public JPanel getContextualPanel() {
        if (sControlPanel == null)
            sControlPanel = new PathwayPanel.PlaybackToolPanel();
        return sControlPanel;
    }
    
}
