package tufts.vue;

public class MapItemEvent
{
    private MapItem source;
    private String what;

    public MapItemEvent(MapItem item, String what)
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
        return "MapItemEvent[" + what + " item=" + source + "]";
    }
}

