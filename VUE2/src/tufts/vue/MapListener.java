package tufts.vue;

public interface MapListener
    extends java.util.EventListener
{
    void mapItemAdded(MapEvent e);
    void mapItemRemoved(MapEvent e);
    void mapItemChanged(MapEvent e);
}

