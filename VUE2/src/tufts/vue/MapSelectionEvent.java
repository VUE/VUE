package tufts.vue;

public class MapSelectionEvent
    extends EventRaiser
{
    private MapItem mapItem;
    
    public MapSelectionEvent(java.awt.Component c, MapItem mapItem)
    {
        super(c);
        this.mapItem = mapItem;
    }

    public Class getListenerClass()
    {
        return MapSelectionListener.class;
    }
    
    public MapItem getMapItem()
    {
        return mapItem;
    }

    public void dispatch(Object listener)
    {
        //System.err.println("dispatch to " + listener);
        ((MapSelectionListener)listener).eventRaised(this);
    }
    
}


