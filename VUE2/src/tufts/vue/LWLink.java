package tufts.vue;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * LWLink.java
 *
 * Draws a view of a Link on a java.awt.Graphics2D context,
 * and offers code for user interaction.
 *
 * Note that links have position (always their mid-point) only so that
 * there's a place to connect for another link and/or a place for
 * the label.  Having a size doesn't actually make much sense, tho
 * we inherit from LWComponent.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public class LWLink extends LWComponent
    implements Link
{
    //private static final float WEIGHT_RENDER_RATIO = 2f;
    //private static final float MAX_RENDER_WIDTH = 16f;
    
    // interface
    
    // used only during save
    public String getEndPoint1_ID()
    {
        //System.err.println("getEndPoint1_ID called for " + this);
        if (this.c1 == null)
            return this.endPoint1_ID;
        else
            return this.c1.getID();
    }
    // used only during save
    public String getEndPoint2_ID()
    {
        //System.err.println("getEndPoint2_ID called for " + this);
        if (this.c2 == null)
            return this.endPoint2_ID;
        else
            return this.c2.getID();
    }

    // used only during restore
    public void setEndPoint1_ID(String s)
    {
        this.endPoint1_ID = s;
    }
    // used only during restore
    public void setEndPoint2_ID(String s)
    {
        this.endPoint2_ID = s;
    }
    public boolean isOrdered()
    {
        return this.ordered;
    }
    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    public int getWeight()
    {
        return (int) (getStrokeWidth() + 0.5f);
    }
    public void setWeight(int w)
    {
        setStrokeWidth((float)w);
    }

    public void setStrokeWidth(float w)
    {
        if (w <= 0f)
            w = 0.1f;
        super.setStrokeWidth(w);
    }
    

    public int incrementWeight()
    {
        //this.weight += 1;
        //return this.weight;
        setStrokeWidth(getStrokeWidth()+1);
        return getWeight();
    }

    // impl
    
    private String endPoint1_ID; // used only during restore
    private String endPoint2_ID; // used only during restore
    
    //private int weight = 1;
    private boolean ordered = false;
    private int endPoint1Style = 0;
    private int endPoint2Style = 0;
    private LWComponent c1;
    private LWComponent c2;
    private Line2D line = new Line2D.Float();
    
    /**
     * Used ONLY for restore -- must be public so can be reflected
     */
    public LWLink() {}

    /**
     * Create a new link between two LWC's
     */
    public LWLink(LWComponent c1, LWComponent c2)
    {
        if (c1 == null || c2 == null)
            throw new IllegalArgumentException("LWLink: c1=" + c1 + " c2=" + c2);
        this.c1 = c1;
        this.c2 = c2;
        setSize(10,10);
        setFont(FONT_LINKLABEL);
        setTextColor(COLOR_LINK_LABEL);
        setEndPoint1(c1);
        setEndPoint2(c2);
        setStrokeWidth(2f);
        // todo: compute location now
        
        //manager = LWPathwayManager.getInstance();
    }
    
    protected void removeFromModel()
    {
        super.removeFromModel();
        c1.removeLinkRef(this);
        c2.removeLinkRef(this);
    }

    public boolean intersects(Rectangle2D rect)
    {
        return rect.intersectsLine(this.line);
    }

    public boolean contains(float x, float y)
    {
        //if (super.contains(x, y))
        //  return true;
        if (VueUtil.StrokeBug05) {
            x -= 0.5f;
            y -= 0.5f;
        }
        //float maxDist = (getWeight() * WEIGHT_RENDER_RATIO) / 2;
        float maxDist = getStrokeWidth() / 2;
        return line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1;
    }
    
    public LWComponent getComponent1()
    {
        return c1;
    }
    public LWComponent getComponent2()
    {
        return c2;
    }
    public MapItem getItem1()
    {
        return c1;
    }
    public MapItem getItem2()
    {
        return c2;
    }
    void setEndPoint1(LWComponent c)
    {
        if (c == null) throw new IllegalArgumentException(this + " attempt to set endPoint1 to null");
        this.c1 = c;
        //if (c == null) System.err.println(this + " endPoint1 set to null");
        //else
        c.addLinkRef(this);
        //System.out.println(this + " ep1 = " + c);
    }
    void setEndPoint2(LWComponent c)
    {
        if (c == null) throw new IllegalArgumentException(this + " attempt to set endPoint2 to null");
        this.c2 = c;
        //if (c == null) System.err.println(this + " endPointd2 set to null");
        //else
        c.addLinkRef(this);
        //System.out.println(this + " ep2 = " + c);
    }
    public java.util.Iterator getLinkEndpointsIterator()
    {
        java.util.List endpoints = new java.util.ArrayList(2);
        endpoints.add(getComponent1());
        endpoints.add(getComponent2());
        return new VueUtil.GroupIterator(endpoints,
                                         super.getLinkEndpointsIterator());
        
    }
    
    public java.util.List getAllConnectedComponents()
    {
        java.util.List list = new java.util.ArrayList(getLinkRefs().size() + 2);
        list.addAll(getLinkRefs());
        list.add(getComponent1());
        list.add(getComponent2());
        return list;
    }

    /*
    public void X_setLocation(float x, float y)
    {
        float dx = getX() - x;
        float dy = getY() - y;
        //System.out.println(this + " ("+x+","+y+") dx="+dx+" dy="+dy);
        // fixme: moving a link tween links sends
        // multiple move events to nodes at their
        // ends, causing them to move nlinks or more times
        // faster than we're dragging.
        // todo fixme: what if both are children? better
        // perhaps to actually have a child move it's parent
        // around here, yet we can't do generally in setLocation
        // or then we couldn't individually drag a parent
        if (!c1.isChild())
            c1.setLocation(c1.getX() - dx, c1.getY() - dy);
        if (!c2.isChild())
            c2.setLocation(c2.getX() - dx, c2.getY() - dy);
        super.setLocation(x,y);
    }
    */

    private static final int clearBorder = 4;
    private Rectangle2D box = new Rectangle2D.Float();
    public void draw(Graphics2D g)
    {
        // Draw the connecting line

        float startX, startY, endX, endY, locX, locY;
        startX = c1.getCenterX();
        startY = c1.getCenterY();
        endX = c2.getCenterX();
        endY = c2.getCenterY();

        if (false&&c1.isChild()) {
            /*
            Point2D p = c1.nearestPoint(endX, endY);
            startX = (float) p.getX();
            startY = (float) p.getY();
            */
            // nearest corner
            if (endX > startX)
                startX += c1.getWidth() / 2;
            else if (endX < startX)
                startX -= c1.getWidth() / 2;
            if (endY > startY)
                startY += c1.getHeight() / 2;
            else if (endY < startY)
                startY -= c1.getHeight() / 2;
        }
        if (false&&c2.isChild()) {
            /*
            Point2D p = c2.nearestPoint(startX, startY);
            endX = (float) p.getX();
            endY = (float) p.getY();
            */
            // nearest corner
            if (endX > startX)
                endX -= c2.getWidth() / 2;
            else if (endX < startX)
                endX += c2.getWidth() / 2;
            if (endY > startY)
                endY -= c2.getHeight() / 2;
            else if (endY < startY)
                endY += c2.getHeight() / 2;
        }
        locX = startX - (startX - endX) / 2;
        locY = startY - (startY - endY) / 2;
        
        // Set our location to the midpoint between
        // the nodes we're connecting.
        // todo: as this happens every paint for every link,
        // make sure we don't raise locations events
        // (override if we decide we LWComponent's normally
        // sending location events, which we don't now).
        //super.setLocation(locX - getWidth()/2,
        //                locY - getHeight()/2);
        //todo: eventually have LWComponent setLocation
        // tell all connected links to recompute themselves...
        setX(locX - getWidth()/2);
        setY(locY - getHeight()/2);
        
        if (VueUtil.StrokeBug05) {
            startX -= 0.5;
            startY -= 0.5;
            endX -= 0.5;
            endY -= 0.5;
        }

        //-------------------------------------------------------
        // Set the stroke line
        //-------------------------------------------------------
        // todo: compute & return this in getShape
        this.line.setLine(startX, startY, endX, endY);


        // Clip the node shape so the link doesn't draw into it.
        // We need to do this instead of just drawing links first
        // because SOME links need to be on top -- links to child links,
        // for instance, or maybe just a link you want on the top layer.
        // todo: this works, but it may be a big performance hit,
        // and it doesn't solve the problem of knowing the true
        // visible link length so we can properly center the label
        // todo: this will eventually be replace by links knowing
        // their exact endpoint at edge of the shape of each node --
        // we need to compute the intersection of a shape and a line segment
        //if ((c1.getShape() != null && !c1.isChild())
        //|| (c2.getShape() != null && !c2.isChild())) {
        if (c1.getShape() != null || c2.getShape() != null) {
            Area clipArea = new Area(g.getClipBounds());
            if (c1.getShape() != null /*&& !c1.isChild()*/)
                clipArea.subtract(new Area(c1.getShape()));
            if (c2.getShape() != null /*&& !c2.isChild()*/)
                clipArea.subtract(new Area(c2.getShape()));
            g.clip(clipArea);
        }

        
        /*
        // temporary: draw hit box
        // todo: make a handle?
        g.setColor(Color.lightGray);
        g.setStroke(STROKE_ONE);
        //g.drawRect((int)getX(), (int)getY(), (int)getWidth(), (int)getHeight());
        if (VueUtil.StrokeBug05)
            box.setRect(getX()-0.5, getY()-0.5, getWidth(), getHeight());
        else
            box.setRect(getX(), getY(), getWidth(), getHeight());
        g.draw(box);
        */

        //
        //strokeWidth = getWeight() * WEIGHT_RENDER_RATIO;
        //if (strokeWidth > MAX_RENDER_WIDTH)
        //    strokeWidth = MAX_RENDER_WIDTH;
        

        BasicStroke stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        if (c1.getScale() != 1f || c2.getScale() != 1f) {
            float strokeWidth = getStrokeWidth();
            if (c1.getScale() < c2.getScale())
                strokeWidth *= c1.getScale();
            else
                strokeWidth *= c2.getScale();
            //g.setStroke(new BasicStroke(strokeWidth));
            stroke = new BasicStroke(strokeWidth);
        } else {
            //g.setStroke(this.stroke);
            stroke = this.stroke;;
        }
    
        //-------------------------------------------------------
        // If selected or indicated, draw a standout stroke
        // bigger than the actual stroke first.
        //-------------------------------------------------------
        /*
        if (isIndicated() || isSelected()) {
            if (isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(COLOR_INDICATION);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 2));
            g.draw(this.line);
        }
        */
        
        //-------------------------------------------------------
        // Draw the stroke
        //-------------------------------------------------------
        if (isSelected())
            g.setColor(COLOR_SELECTION);
        else if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else
            g.setColor(getStrokeColor());

        //g.setColor(getStrokeColor());
        g.setStroke(stroke);
        g.draw(this.line);

        String label = getLabel();
        if (label != null && label.length() > 0) {
            g.setColor(getTextColor());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            float w = fm.stringWidth(label);
            g.drawString(label, locX - w/2, locY - (strokeWidth/2));
        }
       
        //g.drawLine((int)sx, (int)sy, (int)ex, (int)ey);
        //g.drawLine(sx, sy, ex, ey);

        /*
         * Draw the handle
         */
        /*
        //fixme
        g.setColor(Color.darkGray);
        int w = getWidth() - clearBorder * 2;
        int h = getHeight() - clearBorder * 2;
        g.fillRect(clearBorder, clearBorder, w, h);
        */
        
    }


    // these two to support a special dynamic link
    // which we use while creating a new link
    LWLink(LWComponent c2)
    {
        this.c2 = c2;
    }
    void setSource(LWComponent c1)
    {
        this.c1 = c1;
    }
    
    
}
