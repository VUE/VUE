package tufts.vue;

import java.awt.*;

/**
 * LinkView.java
 *
 * Draws a view of a Link on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
class LinkView extends LWComponent
{
    private LWComponent c1;
    private LWComponent c2;
    private Link link;
    
    public LinkView(Link link, LWComponent c1, LWComponent c2)
    {
        if (link == null || c1 == null || c2 == null)
            throw new java.lang.IllegalArgumentException("LinkView: link=" + link + " c1=" + c1 + " c2=" + c2);
        this.link = link;
        this.c1 = c1;
        this.c2 = c2;
        setSize(10,10);
    }

    public MapItem getMapItem()
    {
        return getLink();
    }

    public Link getLink()
    {
        return link;
    }

    public void setLocation(int x, int y)
    {
        int dx = getX() - x;
        int dy = getY() - y;
        super.setLocation(x,y);
        c1.setLocation(c1.getX() - dx, c1.getY() - dy);
        c2.setLocation(c2.getX() - dx, c2.getY() - dy);
    }
    
    private static final Font linkFont = new Font("SansSerif", Font.PLAIN, 11);
    private static final int clearBorder = 4;

    public void draw(Graphics g)
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

        g.setColor(Color.lightGray);
        g.drawRect(this.x, this.y, getWidth(), getHeight());

        /*
         * Draw the link
         */
        if (isSelected())
            g.setColor(Color.blue);
        else
            g.setColor(Color.black);
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
    LinkView(LWComponent c2)
    {
        this.c2 = c2;
    }
    void setSource(LWComponent c1)
    {
        this.c1 = c1;
    }
    
    
}
