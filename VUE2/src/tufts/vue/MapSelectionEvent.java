package tufts.vue;

// rename SelectionEvent
public class MapSelectionEvent
    extends EventRaiser
{
    java.util.List items;
    
    public MapSelectionEvent(java.awt.Component source, java.util.List list)
    {
        super(source);
        this.items = list;
    }

    public Class getListenerClass()
    {
        return MapSelectionListener.class;
    }
    
    public int count()
    {
        return items.size();
    }
    
    public java.util.Iterator getIterator()
    {
        return items.iterator();
    }
    
    public java.util.List getList()
    {
        return items;
    }

    public void dispatch(Object listener)
    {
        ((MapSelectionListener)listener).eventRaised(this);
    }
    
}


