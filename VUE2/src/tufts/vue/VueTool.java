package tufts.vue;

public abstract class VueTool
{
    protected MapViewer mapView;

    VueTool(MapViewer mapViewer)
    {
        this.mapView = mapViewer;
    }

    public abstract String getToolName();
    public abstract boolean handleKeyPressed(java.awt.event.KeyEvent e);
}
