package tufts.vue;

public interface MapChangeListener
    extends java.util.EventListener
{
    void mapItemAdded(MapChangeEvent e);
    void mapItemRemoved(MapChangeEvent e);
    void mapItemChanged(MapChangeEvent e);
}

