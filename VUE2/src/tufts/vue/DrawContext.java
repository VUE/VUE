package tufts.vue;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DrawContext
{
    public final Graphics2D g;
    private double zoom = 1.0;
    private int index;
    private boolean disableAntiAlias = false;
    private boolean isPrinting = false;
    private boolean isDraftQuality = false;
    // todo: consider including a Conatiner arg in here, for
    // MapViewer, etc.  And replace zoom with a getZoom
    // that grabs transform scale value.
    
    public DrawContext(Graphics2D g, double zoom)
    {
        this.g = g;
        this.zoom = zoom;
    }
    public DrawContext(Graphics2D g)
    {
        this(g, 1.0);
    }
    
    /**
     * Mark us rendering for printing.  Note that
     * rendering any transparency whatsoever during
     * a print render appears to cause at least the
     * print preview to fail on Mac OSX (the Preview app)
     */
    public void setPrinting(boolean t) {
        isPrinting = t;
    }

    public boolean isPrinting() {
        return isPrinting;
    }

    public void setDraftQuality(boolean t) {
        isDraftQuality = t;
    }

    public boolean isDraftQuality() {
        return isDraftQuality;
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

    /** set a stroke width that stays constant on-screen at given
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
        return new DrawContext(this);
    }
    
    public DrawContext(DrawContext dc)
    {
        this.g = (Graphics2D) dc.g.create();
        this.zoom = dc.zoom;
        this.disableAntiAlias = dc.disableAntiAlias;
        this.index = dc.index;
        this.isPrinting = dc.isPrinting;
        this.isDraftQuality = dc.isDraftQuality;
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
