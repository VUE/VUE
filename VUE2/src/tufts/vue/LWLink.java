package tufts.vue;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * LWLink.java
 *
 * Draws a view of a Link on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
class LWLink extends LWComponent
{
    private static final float WEIGHT_RENDER_RATIO = 2f;
    private static final float MAX_RENDER_WIDTH = 16f;
    
    private Link link;
    private LWComponent c1;
    private LWComponent c2;

    private Line2D line = new Line2D.Float();
    
    public LWLink(Link link, LWComponent c1, LWComponent c2)
    {
        if (link == null || c1 == null || c2 == null)
            throw new java.lang.IllegalArgumentException("LWLink: link=" + link + " c1=" + c1 + " c2=" + c2);
        this.link = link;
        this.c1 = c1;
        this.c2 = c2;
        setSize(10,10);
        setFont(FONT_LINKLABEL);
        setTextColor(COLOR_LINK_LABEL);
        c1.addLink(this);
        c2.addLink(this);
    }

    public boolean absoluteDrawing()
    {
        return true;
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
        float maxDist = (link.getWeight() * WEIGHT_RENDER_RATIO) / 2;
        return line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1;
    }
    
    public MapItem getMapItem()
    {
        return link;
    }
    public Link getLink()
    {
        return link;
    }

    public LWComponent getComponent1()
    {
        return c1;
    }
    public LWComponent getComponent2()
    {
        return c2;
    }
    public java.util.Iterator getLinkEndpointsIterator()
    {
        java.util.List endpoints = new java.util.ArrayList(2);
        endpoints.add(getComponent1());
        endpoints.add(getComponent2());
        return new VueUtil.GroupIterator(endpoints,
                                         super.getLinkEndpointsIterator());
        
    }

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
    
    private static final int clearBorder = 4;
    private Rectangle2D box = new Rectangle2D.Float();
    public void draw(Graphics2D g)
    {
        super.draw(g);
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
        
        /*
         * Set our location to the midpoint between
         * the nodes we're connecting.
         */
        super.setLocation(locX - getWidth()/2,
                          locY - getHeight()/2);
        

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

        /*
         * Draw the link
         */
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else if (isSelected())
            g.setColor(COLOR_SELECTION);
        else
            g.setColor(getStrokeColor());
        
        float strokeWidth = 0f;
        if (this.link == null) {
            // possible while dragging out a new link
            g.setStroke(STROKE_TWO);
        } else {
            // set the stroke width
            strokeWidth = this.link.getWeight() * WEIGHT_RENDER_RATIO;
            if (strokeWidth > MAX_RENDER_WIDTH)
                strokeWidth = MAX_RENDER_WIDTH;
            // If either end of this link is scaled, scale stroke
            // to smallest of the scales (even better: render the stroke
            // in a variable width narrowing as it went...)
            if (c1.getScale() != 1f || c2.getScale() != 1f) {
                if (c1.getScale() < c2.getScale())
                    strokeWidth *= c1.getScale();
                else
                    strokeWidth *= c2.getScale();
            }
            g.setStroke(new BasicStroke(strokeWidth));
        }
        if (VueUtil.StrokeBug05) {
            startX -= 0.5;
            startY -= 0.5;
            endX -= 0.5;
            endY -= 0.5;
        }
        this.line.setLine(startX, startY, endX, endY);

        // Clip the node shape so the link doesn't draw into it.
        // We need to do this instead of just drawing links first
        // because SOME links need to be on top -- links to child links,
        // for instance, or maybe just a link you want on the top layer.
        // todo: this works, but it may be a big performance hit,
        // and it doesn't solve the problem of knowing the true
        // visible link length so we can properly center the label
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
        g.draw(this.line);

        MapItem mi = getMapItem();
        if (mi != null) {
            String label = mi.getLabel();
            if (label != null && label.length() > 0) {
                g.setColor(getTextColor());
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                float w = fm.stringWidth(label);
                g.drawString(label, locX - w/2, locY - (strokeWidth/2));
            }
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
