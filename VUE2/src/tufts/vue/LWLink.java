package tufts.vue;

import java.awt.*;
import java.awt.geom.Line2D;

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

    //private Line2D.Float line = new Line2D.Float(0,0,0,0);
    
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

    public void setLocation(int x, int y)
    {
        int dx = getX() - x;
        int dy = getY() - y;
        //System.out.println(this + " ("+x+","+y+") dx="+dx+" dy="+dy);
        // fixme: moving a link tween links sends
        // multiple move events to nodes at their
        // ends, causing them to move nlinks or more times
        // faster than we're dragging.
        c1.setLocation(c1.getX() - dx, c1.getY() - dy);
        c2.setLocation(c2.getX() - dx, c2.getY() - dy);
        super.setLocation(x,y);
    }
    
    private static final Font linkFont = new Font("SansSerif", Font.PLAIN, 11);
    private static final int clearBorder = 4;

    public void draw(Graphics2D g)
    {
        super.draw(g);
        // Draw the connecting line
        int sx = c1.getX() + c1.getWidth() / 2;
        int sy = c1.getY() + c1.getHeight() / 2;
        int ex = c2.getX() + c2.getWidth() / 2;
        int ey = c2.getY() + c2.getHeight() / 2;
        int lx = sx - (sx - ex) / 2;
        int ly = sy - (sy - ey) / 2;
        
        /*
         * Set our location to the midpoint between
         * the nodes we're connecting.
         */
        super.setLocation(lx-getWidth()/2, ly-getHeight()/2);
        

        // temporary: draw hit box
        g.setColor(Color.lightGray);
        g.setStroke(STROKE_ONE);
        g.drawRect(this.x, this.y, getWidth(), getHeight());

        /*
         * Draw the link
         */
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else if (isSelected())
            g.setColor(COLOR_SELECTION);
        else
            g.setColor(COLOR_DEFAULT);
        
        if (this.link == null) {
            // possible while dragging out a new link
            g.setStroke(STROKE_TWO);
        } else {
            float width = this.link.getWeight() * WEIGHT_RENDER_RATIO;
            if (width > MAX_RENDER_WIDTH)
                width = MAX_RENDER_WIDTH;
            g.setStroke(new BasicStroke(width));
        }
        g.drawLine(sx, sy, ex, ey);

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
        
        /*
        g.setFont(linkFont);
        FontMetrics fm = g.getFontMetrics();
        String name = "Link"+hashCode();
        int w = fm.stringWidth(name);
        g.drawString(name, lx - w/2, ly);
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
