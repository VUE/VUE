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
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;

/**
 * Includes a Graphics2D context and adds VUE specific flags and helpers
 * for rendering a tree of LWComponents.
 *
 * @version $Revision: 1.20 $ / $Date: 2006-10-18 17:29:34 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class DrawContext
{
    public final Graphics2D g;
    public final double zoom;
    public final boolean drawAbsoluteLinks;
    
    private final float offsetX;
    private final float offsetY;
    
    private int index;
    private boolean disableAntiAlias = false;
    private boolean isInteractive = false;
    private boolean isDraftQuality = false;
    private boolean isBlackWhiteReversed = false;
    private boolean isPresenting = false;
    private Rectangle frame;
    //private float mAlpha = 1f;

    private VueTool activeTool;

    // todo: consider including a Conatiner arg in here, for
    // MapViewer, etc.  And replace zoom with a getZoom
    // that grabs transform scale value.

    // todo: move coord mappers from MapViewer to here?

    public DrawContext(Graphics g, double zoom, float offsetX, float offsetY, Rectangle frame, boolean absoluteLinks)
    {
        this.g = (Graphics2D) g;
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.frame = frame;
        this.drawAbsoluteLinks = absoluteLinks;
    }
    
    public DrawContext(Graphics g, double zoom)
    {
        this(g, zoom, 0, 0, null, false);
    }
    public DrawContext(Graphics g)
    {
        this(g, 1.0);
    }

    /*
    public setFrame(Rectangle frame) {
        this.frame = frame;
    }
    */
    public Rectangle getFrame() {
        return new Rectangle(frame);
    }

    public void setActiveTool(VueTool tool) {
        activeTool = tool;
    }
    public VueTool getActiveTool() {
        return activeTool;
    }

    public void setAlpha(double alpha, int alphaRule) {
        //mAlpha = (float) alpha;
        if (alpha == 1)
            g.setComposite(AlphaComposite.Src);
        else
            g.setComposite(AlphaComposite.getInstance(alphaRule, (float) alpha));
    }

    public void setAlpha(double alpha) {
        setAlpha(alpha, AlphaComposite.SRC_OVER);
    }
    

    //public float getAlpha() { return mAlpha; }
    
    /**
     * Mark us as rendering for interactive usage: e.g., selection will be drawn.
     */
    public void setInteractive(boolean t) {
        isInteractive = t;
    }

    public boolean isInteractive() {
        return isInteractive;
    }

    public boolean isBlackWhiteReversed() {
        return isBlackWhiteReversed;
    }

    public void setBlackWhiteReversed(boolean t) {
        isBlackWhiteReversed = t;
    }

    public void setPresenting(boolean t) {
        isPresenting = t;
    }

    public boolean isPresenting() {
        return isPresenting;
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

    /* passthru to Graphcs.setColor.  This method available for override 
    public void setColor(Color c) {
        g.setColor(c);
    }*/
        
    /** Turn on or off text & shape anti-aliasing */
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

    // todo: make this safer
    public void setAbsoluteDrawing(boolean unZoom) {
        if (unZoom)
            g.scale(1/zoom, 1/zoom);
        else
            g.scale(zoom, zoom);
    }

    private boolean inMapDraw = false;
    private AffineTransform savedTransform;
    /** set up for drawing a model: adjust to the current zoom and offset.
     * MapViewer, MapPanner, VueTool, etc, to use.*/
    // todo: change to single setMapDrawing(boolean)
    public void setMapDrawing() {
        if (!inMapDraw) {
            savedTransform = g.getTransform();
            g.translate(offsetX, offsetY);
            g.scale(zoom, zoom);
            //System.out.println("DC SCALE TO " + zoom);
            //System.out.println("DC SCALE TO " + g.getTransform());
            inMapDraw = true;
        }
    }
    public void setRawDrawing() {
        if (inMapDraw) {
            if (savedTransform == null)
                throw new IllegalStateException("attempt to revert to raw draw in a derivative DrawContext");
            //System.out.println("DC REVER TO " + savedTransform);
            g.setTransform(savedTransform);
            inMapDraw = false;
        }
    }
    

    public DrawContext create()
    {
        return new DrawContext(this);
    }
    
    public DrawContext(DrawContext dc)
    {
        this.g = (Graphics2D) dc.g.create();
        this.zoom = dc.zoom;
        this.offsetX = dc.offsetX;
        this.offsetY = dc.offsetY;
        this.disableAntiAlias = dc.disableAntiAlias;
        this.index = dc.index;
        this.isInteractive = dc.isInteractive;
        this.isDraftQuality = dc.isDraftQuality;
        this.isBlackWhiteReversed = dc.isBlackWhiteReversed;
        this.isPresenting = dc.isPresenting;
        this.activeTool = dc.activeTool;
        this.inMapDraw = dc.inMapDraw;
        this.frame = dc.frame;
        this.drawAbsoluteLinks = dc.drawAbsoluteLinks;
        //this.mAlpha = dc.mAlpha;
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
