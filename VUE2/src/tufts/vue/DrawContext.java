package tufts.vue;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DrawContext
{
    Graphics2D g;
    double zoom = 1.0;
    int index;
    private boolean disableAntiAlias = false;
    
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
        this.disableAntiAlias = dc.disableAntiAlias;
        this.index = dc.index;
    }

    public void disableAntiAlias(boolean tv)
    {
        this.disableAntiAlias = tv;
        if (tv)
            setAntiAlias(false);
    }
        
    public void setAntiAlias(boolean on)
    {
        if (disableAntiAlias)
            on = false;
        
        Object shapeVal = on ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        Object textVal = on ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        this.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, shapeVal);
        this.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textVal);
    }

    public void setPrioritizeQuality(boolean on)
    {
        this.g.setRenderingHint(RenderingHints.KEY_RENDERING, on
                                ? RenderingHints.VALUE_RENDER_QUALITY
                                : RenderingHints.VALUE_RENDER_SPEED);
    }

    public void setPrioritizeSpeed(boolean on)
    {
        setPrioritizeQuality(!on);
    }
    
    public void setFractionalFontMetrics(boolean on)
    {
        this.g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, on
                                ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    /** set a stroke with that stays constant on-screen at given
     * width independent of any current scaling (presuming
     * scaling is same in X/Y direction's -- only tracks X scale factor).
     */
    public void setAbsoluteStrokeWidth(float width)
    {
        this.g.setStroke(new java.awt.BasicStroke(width / (float) this.g.getTransform().getScaleX()));
    }

    /**
     * An arbitrary counter that can be set & read.
     */
    public void setIndex(int i) {
        this.index = i;
    }
    /**
     * An arbitrary counter that can be set & read.
     */
    public int getIndex() {
        return this.index;
    }

    public DrawContext create()
    {
        DrawContext dc = new DrawContext(this);
        dc.g = (Graphics2D) this.g.create();
        return dc;
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
