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
    
    public ConceptMap() {   
        super("Map");
    }

    public ConceptMap(String label)
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


    public void addMapListener(MapListener cl)
    {
        listeners.add(cl);
    }

    public void notifyMapListeners(MapEvent e)
    {
        java.util.Iterator i = listeners.iterator();
        int id = e.getID();
        while (i.hasNext()) {
            MapListener mcl = (MapListener) i.next();
            switch (id) {
            case MapEvent.ADD:
                mcl.mapItemAdded(e);
                break;
            case MapEvent.REMOVE:
                mcl.mapItemRemoved(e);
                break;
            case MapEvent.CHANGE:
                mcl.mapItemChanged(e);
                break;
            }
        }
    }
    public void removeMapListener(MapListener mcl)
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
        notifyMapListeners(new MapEvent(this, node, MapEvent.ADD));
    }

    public void removeNode(Node node)
    {
        nodeList.remove(node);
        notifyMapListeners(new MapEvent(this, node, MapEvent.REMOVE));
    }

    public void addLink(Link link)
    {
        linkList.add(link);
        notifyMapListeners(new MapEvent(this, link, MapEvent.ADD));
    }

    public void removeLink(Link link)
    {
        linkList.remove(link);
        notifyMapListeners(new MapEvent(this, link, MapEvent.REMOVE));
    }

    public void addPathway(Pathway pathway)
    {
        pathwayList.add(pathway);
        notifyMapListeners(new MapEvent(this, pathway, MapEvent.ADD));
    }

    public void removePathway(Pathway pathway)
    {
        pathwayList.remove(pathway);
        notifyMapListeners(new MapEvent(this, pathway, MapEvent.REMOVE));
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
