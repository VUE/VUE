/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DrawContext
{
    // consider getting rid of all the methods and make all the members final.
    public final Graphics2D g;
    public final double zoom;
    private int index;
    private boolean disableAntiAlias = false;
    private boolean isPrinting = false;
    private boolean isDraftQuality = false;
    private boolean isBlackWhiteReversed = false;

    private VueTool activeTool;
    // tracking the active tool for conditional drawing would probably be
    // better handled through a more comprehensive tool architecture,
    // that gave the active tool chances to draw whatever it wants
    // at 4 different points: under everything, over everything,
    // under the selected object and over the selected object.

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

    public void setActiveTool(VueTool tool) {
        activeTool = tool;
    }
    public VueTool getActiveTool() {
        return activeTool;
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

    public boolean isBlackWhiteReversed() {
        return isBlackWhiteReversed;
    }

    public void setBlackWhiteReversed(boolean t) {
        isBlackWhiteReversed = t;
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

    /** passthru to Graphcs.setColor.  This method available for override */
    public void setColor(Color c) {
        g.setColor(c);
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
    public void setAbsoluteStroke(double width)
    {
        this.g.setStroke(new java.awt.BasicStroke((float) (width / this.g.getTransform().getScaleX())));
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
        this.isBlackWhiteReversed = dc.isBlackWhiteReversed;
        this.activeTool = dc.activeTool;
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
