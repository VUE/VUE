package tufts.vue;

import java.awt.Graphics;
import java.awt.Graphics2D;
public class DrawContext
{
    Graphics2D g;
    double zoom = 1.0;
    
    public DrawContext(Graphics2D g, double zoom)
    {
        this.g = g;
        this.zoom = zoom;
    }
    public DrawContext(Graphics2D g)
    {
        this(g, 1.0);
    }
    
    public DrawContext(DrawContext dc)
    {
        this.g = dc.g;
        this.zoom = dc.zoom;
        //this.scale = dc.scale;
    }

    /*
    public DrawContext createScaled(float scale)
    {
        DrawContext dc = new DrawContext(this);
        dc.scale *= scale;
        return dc;
    }
    */
}
