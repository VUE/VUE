package tufts.vue;

/**
 * Vue2DMap.java
 *
 * This is the core VUE model class -- used for saving
 * and restoring maps to XML.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */

import java.util.Vector;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Vue2DMap extends LWContainer
    implements ConceptMap
{
    public static final String CASTOR_XML_MAPPING = "vue2d_map.xml";
    
    // these for persistance use only
    private float userOriginX;
    private float userOriginY;
    private double userZoom = 1;
    
    /*
    class CVector extends java.util.Vector {
        // Used to set parent refs & ID's on child nodes.
        // During restore operations, the ID
        // should already be set, and we're just
        // checking it to make sure nextID will
        // skip over any existing values
        public boolean add(Object obj) {
            super.addElement(obj);
            
            LWComponent lwc = (LWComponent) obj;
            lwc.setParent(Vue2DMap.this);
            if (lwc.getID() == null) {
                lwc.setID(""+nextID);
                nextID++;
            } else {
                long id = -1;
                try {
                    id = Integer.parseInt(lwc.getID());
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
    */
    
    private long nextID = 1;
    public void addChild(LWComponent c)
    {
        super.addChild(c);
        // todo: no point doing this here: need to do in LWGroup via CList --
        // restore code fetches list object and does the add itself!
        if (c.getID() == null) {
            c.setID(""+nextID);
            nextID++;
        }
    }
    
    // only to be used during a restore from persisted
    public Vue2DMap()
    {   
        setLabel("<untitled map>");
    }

    public Vue2DMap(String label)
    {
        setLabel(label);
        setID("0");
        setFillColor(java.awt.Color.white);
        setTextColor(COLOR_TEXT);
        setStrokeColor(COLOR_STROKE);
        setFont(FONT_DEFAULT);
    }

    

    /** for viewer to report user origin sets via pan drags */
    void setUserOrigin(float x, float y)
    {
        this.userOriginX = x;
        this.userOriginY = y;
    }
    /** for persistance */
    public Point2D getUserOrigin()
    {
        return new Point2D.Float(this.userOriginX, this.userOriginY);
    }
    /** for persistance */
    public void setUserOrigin(Point2D p)
    {
        setUserOrigin((float) p.getX(), (float) p.getY());
    }
    /** for persistance */
    public void setUserZoom(double zoom)
    {
        this.userZoom = zoom;
    }
    /** for persistance */
    public double getUserZoom()
    {
        return this.userZoom;
    }
      

    /**
     * To be called once after a persisted map
     * is restored.
     */
    public void resolvePersistedLinks()
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (!(c instanceof LWLink))
                continue;
            LWLink l = (LWLink) c;
            l.setEndPoint1(findItemByID(l.getEndPoint1_ID()));
            l.setEndPoint2(findItemByID(l.getEndPoint2_ID()));
        }
        // todo: throw exception if this called again &
        // clear out link item id strings
    }

    private LWComponent findItemByID(String ID)
    {
        //java.util.Iterator i = new VueUtil.GroupIterator(nodeList, linkList, pathwayList);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent lwc = (LWComponent) i.next();
            if (lwc.getID().equals(ID))
                return lwc;
        }
        System.out.println("failed to locate a LWC with id [" + ID + "] in " + this);
        return null;
    }
    
    public LWComponent findLWComponentAt(float mapX, float mapY)
    {
        LWComponent c = super.findLWComponentAt(mapX, mapY);
        return c == this ? null : c;
    }

    public LWComponent findLWSubTargetAt(float mapX, float mapY)
    {
        LWComponent c = super.findLWSubTargetAt(mapX, mapY);
        return c == this ? null : c;
    }

    LWNode addNode(LWNode c)
    {
        addChild(c);
        return c;
    }
    LWLink addLink(LWLink c)
    {
        addChild(c);
        return c;
    }
    
    LWPathway addPathway(LWPathway c)
    {
        addChild(c);
        return c;
    }
    
    LWComponent addLWC(LWComponent c)
    {
        addChild(c);
        return c;
    }
    private void removeLWC(LWComponent c)
    {
        removeChild(c);
    }

    /*
    public void addNode(Node n) { addLWC(n); }
    public void addLink(Link l) { addLWC(l); }
    public void addPathway(Pathway p) {}
    public void removeNode(Node n) { removeLWC(n); }
    public void removeLink(Link l) { removeLWC(l); }
    public void removePathway(Pathway p) {}
    */
    /*
    public java.util.Iterator getPathwayIterator()
    {
        return null;
    }*/

    public java.awt.geom.Rectangle2D getBounds()
    {
        System.out.println("Vue2DMap getbounds");
        return Vue2DMap.getBounds(getChildIterator());
    }
    
    /**
     * return the bounds for all LWComponents in the iterator
     */
    public static Rectangle2D getBounds(java.util.Iterator i)
    {
        float xMin = Float.POSITIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY;
        float xMax = Float.NEGATIVE_INFINITY;
        float yMax = Float.NEGATIVE_INFINITY;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float x = c.getX();
            float y = c.getY();
            float mx = x + c.getWidth();
            float my = y + c.getHeight();
            if (x < xMin) xMin = x;
            if (y < yMin) yMin = y;
            if (mx > xMax) xMax = mx;
            if (my > yMax) yMax = my;
        }

        // In case there's nothing in there
        if (xMin == Float.POSITIVE_INFINITY) xMin = 0;
        if (yMin == Float.POSITIVE_INFINITY) yMin = 0;
        if (xMax == Float.NEGATIVE_INFINITY) xMax = 0;
        if (yMax == Float.NEGATIVE_INFINITY) yMax = 0;

        return new Rectangle2D.Float(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    
}
