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

import java.util.Vector;
public class ConceptMap extends MapItem
{
    private long nextID = 1;
    
    class CVector extends java.util.Vector {
        // Used to set parent refs & ID's on child nodes.
        // During restore operations, the ID
        // should already be set, and we're just
        // checking it to make sure nextID will
        // skip over any existing values
        public boolean add(Object obj) {
            super.addElement(obj);
            MapItem mi = (MapItem) obj;
            mi.setParent(ConceptMap.this);
            if (mi.getID() == null) {
                mi.setID(""+nextID);
                nextID++;
            } else {
                long id = -1;
                try {
                    id = Integer.parseInt(mi.getID());
                    if (id >= nextID)
                        nextID = id + 1;
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            System.out.println("added " + obj);
            return true;
        }
        public void addElement(Object obj)
        {
            add(obj);
        }
    }
    
    private java.util.List nodeList = new CVector();
    private java.util.List linkList = new CVector();
    private java.util.List pathwayList = new CVector();
    
    private java.util.List listeners = new java.util.Vector();

    private float originX;
    private float originY;
    
    // only to be used during a restore from persisted
    public ConceptMap() {   
        super("<untitled map>");
    }

    public ConceptMap(String label)
    {
        super(label);
        setID("0");
    }

    /**
     * To be called once after a persisted map
     * is restored.
     */
    public void resolvePersistedLinks()
    {
        java.util.Iterator i = getLinkIterator();
        while (i.hasNext()) {
            Link l = (Link) i.next();
            l.setItem1(findItemByID(l.getItem1_ID()));
            l.setItem2(findItemByID(l.getItem2_ID()));
        }
        // todo: throw exception if this called again &
        // clear out link item id strings
    }

    private MapItem findItemByID(String ID)
    {
        java.util.Iterator i = new VueUtil.GroupIterator(nodeList, linkList, pathwayList);
        while (i.hasNext()) {
            MapItem mi = (MapItem) i.next();
            if (mi.getID().equals(ID))
                return mi;
        }
        System.out.println("failed to locate a MapItem with id [" + ID + "] in " + this);
        return null;
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
        if (listeners == null) {
            //System.out.println("impossible condition: listeners is null");
            return;
        }
            
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

    // override of MapItem notify to convert to Map event
    public void notifyChangeListeners(MapItemEvent e)
    {
        notifyMapListeners(new MapEvent(this, e.getSource(), MapEvent.CHANGE));
    }


    public Node addNode(Node node)
    {
        nodeList.add(node);
        notifyMapListeners(new MapEvent(this, node, MapEvent.ADD));
        return node;
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

    public java.util.Vector getNodeList() {
        return (java.util.Vector)nodeList;
    }
    
 
    public void setNodeList(Vector nodeList) {
        this.nodeList = (java.util.List)nodeList;
    }
       
    public java.util.Vector getLinkList() {
        return (java.util.Vector)linkList;
    }
    
    public void setLinkList(Vector linkList) {
        this.linkList = (java.util.List)linkList;
    }
     
    public java.util.Vector getPathwayList() {
        return (java.util.Vector) pathwayList;
    }
    
    public void setPathwayList(Vector pathwayList) {
        this.pathwayList = (java.util.List)pathwayList;
    }
    
    
}
