package tufts.vue;

public class MapViewerEvent
    extends EventRaiser
{
    public static final int DISPLAYED = 1;
    public static final int HIDDEN = 2;
    public static final int PAN = 4;
    public static final int ZOOM = 8;
    
    private int id;
    
    public MapViewerEvent(MapViewer mapViewer, int id)
    {
        super(mapViewer);
        this.id = id;
    }

    public int getID()
    {
        return id;
    }

    public Class getListenerClass()
    {
        return MapViewerListener.class;
    }
    
    public MapViewer getMapViewer()
    {
        return (MapViewer) getSource();
    }

    public void dispatch(Object listener)
    {
        ((MapViewerListener)listener).mapViewerEventRaised(this);
    }
    
}


