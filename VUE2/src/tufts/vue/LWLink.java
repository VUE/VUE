package tufts.vue;

import java.awt.*;
import java.awt.geom.Line2D;
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
        if (super.contains(x, y))
            return true;
        if (VueUtil.StrokeBug05) {
            x -= 0.5f;
            y -= 0.5f;
        }
        float maxDist = (link.getWeight() * WEIGHT_RENDER_RATIO) / 2;
        return line.ptSegDistSq(x, y) <= (maxDist * maxDist);
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

    public void setLocation(float x, float y)
    {
        float dx = getX() - x;
        float dy = getY() - y;
        //System.out.println(this + " ("+x+","+y+") dx="+dx+" dy="+dy);
        // fixme: moving a link tween links sends
        // multiple move events to nodes at their
        // ends, causing them to move nlinks or more times
        // faster than we're dragging.
        c1.setLocation(c1.getX() - dx, c1.getY() - dy);
        c2.setLocation(c2.getX() - dx, c2.getY() - dy);
        super.setLocation(x,y);
    }
    
    private static final int clearBorder = 4;
    private Rectangle2D box = new Rectangle2D.Float();
    public void draw(Graphics2D g)
    {
        super.draw(g);
        // Draw the connecting line
        float sx = c1.getX() + c1.getWidth() / 2;
        float sy = c1.getY() + c1.getHeight() / 2;
        float ex = c2.getX() + c2.getWidth() / 2;
        float ey = c2.getY() + c2.getHeight() / 2;
        float lx = sx - (sx - ex) / 2;
        float ly = sy - (sy - ey) / 2;
        
        /*
         * Set our location to the midpoint between
         * the nodes we're connecting.
         */
        super.setLocation(lx-getWidth()/2, ly-getHeight()/2);
        

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

        /*
         * Draw the link
         */
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else if (isSelected())
            g.setColor(COLOR_SELECTION);
        else
            g.setColor(COLOR_DEFAULT);
        
        float strokeWidth = 0f;
        if (this.link == null) {
            // possible while dragging out a new link
            g.setStroke(STROKE_TWO);
        } else {
            strokeWidth = this.link.getWeight() * WEIGHT_RENDER_RATIO;
            if (strokeWidth > MAX_RENDER_WIDTH)
                strokeWidth = MAX_RENDER_WIDTH;
            g.setStroke(new BasicStroke(strokeWidth));
        }
        if (VueUtil.StrokeBug05) {
            sx -= 0.5;
            sy -= 0.5;
            ex -= 0.5;
            ey -= 0.5;
        }
        this.line.setLine(sx, sy, ex, ey);
        g.draw(this.line);

        MapItem mi = getMapItem();
        if (mi != null) {
            String label = mi.getLabel();
            if (label != null && label.length() > 0) {
                g.setColor(COLOR_LINK_LABEL);
                g.setFont(LinkLabelFont);
                FontMetrics fm = g.getFontMetrics();
                float w = fm.stringWidth(label);
                g.drawString(label, lx - w/2, ly-(strokeWidth/2));
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
