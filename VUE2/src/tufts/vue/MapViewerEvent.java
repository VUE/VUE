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
        return MapViewer.Listener.class;
    }
    
    public MapViewer getMapViewer()
    {
        return (MapViewer) getSource();
    }

    public void dispatch(Object listener)
    {
        ((MapViewer.Listener)listener).mapViewerEventRaised(this);
    }

    public String toString()
    {
        String name = null;
        if (id == DISPLAYED) name = "DISPLAYED";
        else if (id == HIDDEN) name = "HIDDEN";
        else if (id == PAN) name = "PAN";
        else if (id == ZOOM) name = "ZOOM";
        return getClass().getName() + "[" + name + " src=" + getSource() + "]";
    }
    
    
}


