package tufts.vue;

/**
 * ConceptMap.java
 *
 * This is the core VUE model class.
 * todo: persistance! particularly, XML.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
public class ConceptMap extends MapItem
{
    private java.util.List nodeList = new java.util.Vector();
    private java.util.List linkList = new java.util.Vector();
    private java.util.List pathwayList = new java.util.Vector();
    
    private java.util.List listeners = new java.util.Vector();

    private float originX;
    private float originY;

    ConceptMap(String label)
    {
        super(label);
    }

    public void setOrigin(float x, float y)
    {
        this.originX = x;
        this.originY = y;
    }

    public float getOriginX()
    {
        return this.originX;
    }
    public float getOriginY()
    {
        return this.originY;
    }
    public java.awt.geom.Point2D getOrigin()
    {
        return new java.awt.geom.Point2D.Float(this.originX, this.originY);
    }


    public void addMapListener(MapChangeListener cl)
    {
        listeners.add(cl);
    }

    public void notifyMapListeners(MapChangeEvent e)
    {
        java.util.Iterator i = listeners.iterator();
        int id = e.getID();
        while (i.hasNext()) {
            MapChangeListener mcl = (MapChangeListener) i.next();
            switch (id) {
            case MapChangeEvent.ADD:
                mcl.mapItemAdded(e);
                break;
            case MapChangeEvent.REMOVE:
                mcl.mapItemRemoved(e);
                break;
            case MapChangeEvent.CHANGE:
                mcl.mapItemChanged(e);
                break;
            }
        }
    }
    public void removeMapListener(MapChangeListener mcl)
    {
        listeners.remove(mcl);
    }
    public void removeAllMapListeners()
    {
        listeners.clear();
    }

    public void addNode(Node node)
    {
        nodeList.add(node);
        notifyMapListeners(new MapChangeEvent(this, node, MapChangeEvent.ADD));
    }

    public void removeNode(Node node)
    {
        nodeList.remove(node);
        notifyMapListeners(new MapChangeEvent(this, node, MapChangeEvent.REMOVE));
    }

    public void addLink(Link link)
    {
        linkList.add(link);
        notifyMapListeners(new MapChangeEvent(this, link, MapChangeEvent.ADD));
    }

    public void removeLink(Link link)
    {
        linkList.remove(link);
        notifyMapListeners(new MapChangeEvent(this, link, MapChangeEvent.REMOVE));
    }

    public void addPathway(Pathway pathway)
    {
        pathwayList.add(pathway);
        notifyMapListeners(new MapChangeEvent(this, pathway, MapChangeEvent.ADD));
    }

    public void removePathway(Pathway pathway)
    {
        pathwayList.remove(pathway);
        notifyMapListeners(new MapChangeEvent(this, pathway, MapChangeEvent.REMOVE));
    }

    public java.util.Iterator getNodeIterator()
    {
        return nodeList.iterator();
    }

    public java.util.Iterator getLinkIterator()
    {
        return linkList.iterator();
    }

    public java.util.Iterator getPathwayIterator()
    {
        return pathwayList.iterator();
    }


    
}
