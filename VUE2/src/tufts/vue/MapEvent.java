package tufts.vue;

public class MapEvent
{
    public static final int ADD = 1;
    public static final int REMOVE = 2;
    public static final int CHANGE = 3;

    private int id;
    private ConceptMap source;
    private MapItem item;

    public MapEvent(ConceptMap map, MapItem item, int id)
    {
        this.source = map;
        this.item = item;
        this.id = id;
    }

    public int getID()
    {
        return id;
    }
    
    public ConceptMap getSource()
    {
        return source;
    }
    
    public MapItem getItem()
    {
        return item;
    }
}

