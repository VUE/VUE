package tufts.vue;

public class MapItemChangeEvent
{
    private MapItem source;
    private String what;

    public MapItemChangeEvent(MapItem item, String what)
    {
        this.source = item;
        this.what = what;
    }

    public MapItem getSource()
    {
        return this.source;
    }
    
    public String getWhat()
    {
        return this.what;
    }

    public String toString()
    {
        return "MapItemChangeEvent[" + what + " item=" + source + "]";
    }
}

