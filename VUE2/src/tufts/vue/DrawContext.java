/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import tufts.Util;
import static tufts.Util.*;

import edu.tufts.vue.preferences.implementations.BooleanPreference;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

import java.awt.RenderingHints;
import static java.awt.RenderingHints.*;

import static java.lang.Boolean.*;

import com.google.common.collect.Multiset;


/**
 * Includes a Graphics2D context and adds VUE specific flags and helpers
 * for rendering a tree of LWComponents.
 *
 * @version $Revision: 1.69 $ / $Date: 2009-12-09 17:49:35 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public final class DrawContext
{
    public final Graphics2D g;
    public final double zoom;
    
    public final float offsetX;
    public final float offsetY;
    
    
    private int index;
    private int maxLayer = Short.MAX_VALUE; // don't draw layers above this level
    private boolean disableAntiAlias;
    private boolean isInteractive;
    private boolean isDraftQuality;
    private boolean isBlackWhiteReversed;
    private boolean isPresenting;
    private boolean isDrawingPathways = true;
    public final Rectangle frame; // if we have the pixel dimensions of the surface we're drawing on, they go here

    public final LWComponent focal;
    
    LWComponent focused;

    private float alpha = 1f;

    //private VueTool activeTool;

    private final Shape rawClip;
    private final AffineTransform rawTransform;
    private final AffineTransform mapTransform;

    //private AffineTransform lastTransform;
    
    private Rectangle2D masterClipRect; // for drawing map nodes
    private Color fillColor;

    public LWComponent skipDraw;

    private boolean isClipOptimized = true; // todo: rename isPaintOptimized, and make the default false
    private boolean isAnimating;

    // todo: consider including a Conatiner arg in here, for
    // MapViewer, etc.  And replace zoom with a getZoom
    // that grabs transform scale value.

    // todo: move coord mappers from MapViewer to here?

    public DrawContext(Graphics _g, double zoom, float offsetX, float offsetY, Rectangle frame, LWComponent focal, boolean absoluteLinks)
    {
        this.g = (Graphics2D) _g;
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.frame = frame;
        this.focal = focal;

        this.rawClip = g.getClip();
        //this.g.translate(frame.x, frame.y);
        this.rawTransform = g.getTransform();
        this.g.translate(offsetX, offsetY);
        this.g.scale(zoom, zoom);
        this.mapTransform = g.getTransform();
        setMasterClip(g.getClip());

        if (DEBUG.PAINT) out("CONSTRUCTED");

        
        //setMasterClip(rawClip = g.getClip());
        
        //this.drawAbsoluteLinks = absoluteLinks;
        //setPrioritizeSpeed(true);
        //disableAntiAlias(true);
    }
    
    public DrawContext(Graphics g, LWComponent focal)
    {
        this(g, 1.0, 0, 0, (Rectangle) null, focal, false);
        //setBackgroundFill(focal.getRenderFillColor(null)); // caller should do manually
    }
    
    public DrawContext(Graphics g, double zoom)
    {
        this(g, zoom, 0, 0, (Rectangle) null, (LWComponent) null, false);
    }
    public DrawContext(Graphics g)
    {
        this(g, 1.0);
    }

    private DrawContext lastPush;
    public DrawContext push() {
        if (lastPush != null)
            Util.printStackTrace("Unpopped DC: " + lastPush);
        return lastPush = create();
    }
    public void pop() {
        lastPush.dispose();
        lastPush = null;
    }
    public void dispose() {
        // in case we try to use it after this, this should ensure exceptions:
        isClipOptimized = true;
        masterClipRect = null;
        g.dispose();
    }
        

    public void fillBackground(Color c) {
        if (fillColor != null)
            Util.printStackTrace(this + " already filled with " + fillColor);
        setBackgroundFill(c);
        g.setColor(c);
        g.fill(g.getClipBounds());
    }

    /** Fill the given shape will the given color: will be a noop if the fill color matches the current BG fill */
    public void fillArea(final Shape shape, final Color fill) {
        if (fill == null || fill.equals(fillColor)) {
            if (DEBUG.PRESENT && DEBUG.META) Util.printStackTrace("skipping fillArea matching existing bg fill " + fill + " " + fmt(shape));
            return;
        }
        g.setColor(fill);
        g.fill(shape);
    }
    

    public Color getBackgroundFill() {
        return fillColor;
    }
    
    public void setBackgroundFill(Color c) {
        //if (DEBUG.IMAGE) out("setFill: " + c);
        fillColor = c;
    }
    

    /** set up for drawing a model: adjust to the current zoom and offset.
     * MapViewer, MapPanner, VueTool, etc, to use.*/
    // todo: change to single setMapDrawing(boolean)
    /*
    public void setMapDrawing() {
        if (rawTransform != null)
            throw new Error("DrawContext: map paramaters already established");
        
        //if (!inMapDraw) {
            rawTransform = g.getTransform();
            g.translate(offsetX, offsetY);
            g.scale(zoom, zoom);
            mapTransform = g.getTransform();
            setMasterClip(g.getClip());
            //System.out.println("DC SCALE TO " + zoom);
            //System.out.println("DC SCALE TO " + g.getTransform());
            //inMapDraw = true;
            //}
    }
    public void resetMapDrawing() {
        if (mapTransform != null)
            g.setTransform(mapTransform);
        else
            throw new Error("DrawContext: initial map transform not established");
    }
    public void setRawDrawing() {
        //if (inMapDraw) {
            if (rawTransform == null)
                throw new IllegalStateException("attempt to revert to raw draw in a derivative DrawContext");
            //System.out.println("DC REVER TO " + savedTransform);
            g.setTransform(rawTransform);
            //setMasterClip(rawClip);
            //            inMapDraw = false;
            //        }
    }
    
    */

    public boolean isAnimating() {
        return isAnimating;
    }
    public void setAnimating(boolean animating) {
        isAnimating = animating;
    }
    
    public void setMapDrawing() {
        isClipOptimized = true;
        g.setTransform(mapTransform);
    }
    public void setRawDrawing() {
        isClipOptimized = false;
        g.setTransform(rawTransform);
    }
    
    public void setFrameDrawing() {
        setRawDrawing();
        g.translate(frame.x, frame.y);
    }

    public void setClipOptimized(boolean clipOptimized) {
        isClipOptimized = clipOptimized;
    }

    
    /**
       
     * @return true if we can do clip bounds checking optimizations to
     * know if we can skip drawing an LWComponent entirely.  Only
     * works if we're drawing "proper" children of a current map.
     * This is as opposed to special decorations that happened to be
     * LWComponents that we want to draw no matter what what their
     * current location is (often, always 0,0).  E.g., a master slide
     * background, on-map slide icons, presentation navigation nodes,
     * etc.
     
     */
    public boolean isClipOptimized() {
        if (DEBUG.CONTAINMENT && DEBUG.META && DEBUG.WORK) // TODO: temporary weird case
            return false;
        else
            return isClipOptimized;
    }
        
    public void setMasterClip(Shape clip)
    {
        g.setClip(clip);
        if (clip instanceof Rectangle2D) {
            if (DEBUG.PAINT) out("SET MASTER CLIP RECT2D " + fmt(clip));
            masterClipRect = (Rectangle2D) clip;
            //masterClipRect = (Rectangle2D) ((Rectangle2D)clip).clone();
            //if (DEBUG.PAINT) out("SET MASTER CLIP RECT2D DONE " + fmt(masterClipRect));
        } else {
            // we've set the shaped clip in the gc, now extract the master clip rectangle from the gc
            masterClipRect = g.getClipBounds();
            if (DEBUG.PAINT || DEBUG.CONTAINMENT) {
                out("SET SHAPE CLIP: " + fmt(clip));
                //System.out.println("MASTER CLIP RECT2D: " + Util.out(masterClipRect));
            }
        }
//         if (DEBUG.PAINT || (DEBUG.CONTAINMENT&&DEBUG.META) || DEBUG.PRESENT)
//             out("SET MASTER CLIP RECT2D " + fmt(masterClipRect));
    }

    public Rectangle2D getMasterClipRect() {
        if (masterClipRect == null) {
            Util.printStackTrace(this + " null masterClipRect!");
            masterClipRect = this.g.getClipBounds();
        }
        //return (Rectangle2D) ((Rectangle2D)masterClipRect).clone();
        return masterClipRect;
    }

    public void setMaxLayer(int layer) {
        maxLayer = layer;
    }
    public int getMaxLayer() {
        return maxLayer;
        //return 99;
    }

    /*
    public setFrame(Rectangle frame) {
        this.frame = frame;
    }
    */
    public Rectangle getFrame() {
        return new Rectangle(frame);
    }

//     public void setActiveTool(VueTool tool) {
//         activeTool = tool;
//     }
//     public VueTool getActiveTool() {
//         return activeTool;
//     }

    public void setAlpha(double p_alpha, int alphaRule) {
        this.alpha = (float) p_alpha;
        if (alpha == 1f)
            g.setComposite(AlphaComposite.Src);
        else
            g.setComposite(AlphaComposite.getInstance(alphaRule, this.alpha));
        // todo: cache the alpha instance
    }

    public void setAlpha(double alpha) {
        setAlpha(alpha, AlphaComposite.SRC_OVER);
    }

    public float getAlpha() {
        return this.alpha;
    }
    

    public static final String[] AlphaRuleNames = {
        // Index based on coded values in java.awt.AlphaComposite.java @version 10 Feb 1997:
        "<none>",
        "CLEAR", "SRC", "SRC_OVER", "DST_OVER", "SRC_IN", "DST_IN", "SRC_OUT", "DST_OUT", "DST", "SRC_ATOP", "DST_ATOP", "XOR"
    };
    

    public void checkComposite(LWComponent c) {
        if (false&&alpha != 1f) {
            
            // if we're going to do a non-opaque fill during a general transparent
            // rendering situation, change temporarily to the SRC_OVER rule instead of
            // SRC, so that what's under the translucent node will show through.  If we
            // just left it SRC, color values that had an alpha channel would end up
            // blowing away what's underneath them.

            final Color fill = c.getRenderFillColor(this);
            
            // TODO: images with transparency in them may need special handling
            // At the moment, any time we draw with a global transparency,
            // images with transparency are filling their background with black!

            if (c instanceof LWImage) {
                // not perfect, but helps sometimes:
                setAlpha(alpha, AlphaComposite.SRC_ATOP);
                //System.out.println("IMAGE COMPOSITE " + c);
            } else if (fill != null && fill.getAlpha() != 255) {
                // merge with background:
                setAlpha(alpha, AlphaComposite.SRC_OVER);
                //System.out.println("COMPOSITE: SRC_OVER (has fill with partial transparency) " + c);
            } else {
                // draw on top of background:
                //System.out.println("COMPOSITE: SRC (has no fill or opaque fill)              " + c);
                setAlpha(alpha, AlphaComposite.SRC);
            }
        }
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

    /** @return true for special temporary state alternate indications */
    public void setIndicated(LWComponent c) {
        focused = c; // using focused field for now (overloaded)
    }

    public boolean isIndicated(LWComponent c) {
        return focused == c;
    }

    public boolean hasIndicated() {
        return focused != null;
    }

    public LWComponent getIndicated() {
        return focused;
    }
    

    public boolean drawPathways() {
        return isDrawingPathways;
        //return !isPresenting() && focal instanceof LWMap;
        //    return !isFocused && !isPresenting();
    }

    public void setDrawPathways(boolean drawPathways) {
        isDrawingPathways = drawPathways;
    }

    public boolean isFocused() {
        return focal instanceof LWMap == false;
    }

    
    /** @return true of Level-Of-Detail rendering is enabled/permitted */
    public boolean isLODEnabled() {
        // todo: this is inferred -- should have a separate bit for this
        return isInteractive() || isDraftQuality();
    }

    public void disableAntiAlias(boolean disable)
    {
        this.disableAntiAlias = disable;
        if (disable)
            setAntiAlias(false);
    }

    /* passthru to Graphcs.setColor.  This method available for override 
    public void setColor(Color c) {
        g.setColor(c);
    }*/
        
    /** Turn on or off text & shape anti-aliasing */
    public void setAntiAlias(boolean on)
    {
        if (disableAntiAlias) {
            setAliasQuality(g, FALSE);
            setAliasTextQuality(g, FALSE);
        } else {
            setAliasQuality(g, on);
            setAliasTextQuality(g, on);
        }
    }

    public static final Boolean DEFAULT = new Boolean(false); // true/false not relevant
    public static final Boolean QUALITY = Boolean.TRUE;
    public static final Boolean SPEED = Boolean.FALSE;

    public static final Boolean INTERPOLATION_SPEED = SPEED;
    public static final Boolean INTERPOLATION_BETTER = DEFAULT;
    public static final Boolean INTERPOLATION_BEST = QUALITY;

    /** "normal" quality */
    public void setInteractiveQuality() {

        setImageQuality(g, isImageQualityRequested() ? QUALITY : SPEED);

        //setImageQuality(g, QUALITY);
        //setImageQuality(g, DEFAULT);
        // even best quality under Java 1.6 appears to be crappy compared to best under 1.5 on Mac OS X 10.5.8
        //setInterpolation(g, INTERPOLATION_SPEED);
        //setInterpolation(g, INTERPOLATION_BETTER);
        //setInterpolation(g, INTERPOLATION_BEST);
        
        setAlphaQuality(g, DEFAULT);
        setColorQuality(g, DEFAULT);
        setFontQuality(g, DEFAULT);
        setAntiAlias(true);
        //setDitherQuality(g, TRUE);
        isDraftQuality = false;
    }

    public boolean isDraftQuality() {
        return isDraftQuality;
    }

    /** fast rendering with bit set for renderers to check */
    public void setDraftQuality() {
        setFastQuality();
        isDraftQuality = true;
    }
    
    /** "fast" quality */
    private void setFastQuality() {
        setInterpolation(g, INTERPOLATION_SPEED);
        setImageQuality(g, FALSE);
        setAlphaQuality(g, FALSE);
        setColorQuality(g, FALSE);
        setFontQuality(g, FALSE);
        setAntiAlias(true); // never turn off anti-alias
    }

    public void setPrintQuality()
    {
        setInterpolation(g, INTERPOLATION_BEST);
        setImageQuality(g, TRUE);
        setAlphaQuality(g, TRUE);
        setColorQuality(g, TRUE);
        setFontQuality(g, TRUE);
        setAntiAlias(true);
    }
    
    public void setAnimatingQuality()
    {
        setFastQuality();
    }
    
    public void setFractionalFontMetrics(boolean on)
    {
        setFontQuality(g, on);
    }
    
//     public void setPrioritizeSpeed(boolean speed)
//     {
//         setPrioritizeQuality(!speed);
//     }
        

    /** set a stroke width that stays constant on-screen at given
     * width independent of any current scaling (presuming
     * scaling is same in X/Y direction's -- only tracks X scale factor).
     * NOTE: doesn't take into account stroke style (dashing) -- always produces
     * solid stroke.
     */
    public void setAbsoluteStroke(double width)
    {
//         double scale = getAbsoluteScale();
//         if (scale <= 0)
//             scale = 1;
//         this.g.setStroke(new java.awt.BasicStroke((float) (width / scale)));
        setAbsoluteStroke(this.g, width);
    }

    public static void setAbsoluteStroke(Graphics2D g, double width)
    {
        double scale = getAbsoluteScale(g);
        if (scale <= 0)
            scale = 1;
        g.setStroke(new java.awt.BasicStroke((float) (width / scale)));
    }
    

    public double getAbsoluteScale() {
        return this.g.getTransform().getScaleX();
    }
    
    public static double getAbsoluteScale(Graphics2D g) {
        return g.getTransform().getScaleX();
    }


    public void setAbsoluteScale(double absScale)
    {
        final double adjScale = (1 / g.getTransform().getScaleX()) * absScale;
        if (adjScale > 0)
            g.scale(adjScale, adjScale);
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


    public DrawContext create()
    {
        return new DrawContext(this);
    }

    public String toString() {
        //return String.format("DrawContext@%x[zoom=%.2f mapOffset=%.1f,%.1f focal=%s]",
        return String.format("DrawContext@%06X[%.1f%% %s %s%s]",
                             hashCode(),
                             zoom * 100,
                             //offsetX, offsetY,
                             focal == null ? "null-focal" : focal.getUniqueComponentTypeLabel(),
                             fmt(masterClipRect),
                             isClipOptimized
                             ? " CLIPPING"
                             : " DRAW-ALL"
                             );
        
    }

    public static void setImageQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_RENDERING, on,
                   VALUE_RENDER_DEFAULT,
                   VALUE_RENDER_QUALITY,
                   VALUE_RENDER_SPEED);
    }
    
    public static void setInterpolation(Graphics g, Boolean on) {
        setQuality(g,KEY_INTERPOLATION, on,
                   VALUE_INTERPOLATION_BILINEAR, // better ("default")
                   VALUE_INTERPOLATION_BICUBIC, // best & slowest ("quality")
                   VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // fastest ("speed")
    }

    public static void setAlphaQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_ALPHA_INTERPOLATION, on,
                   VALUE_ALPHA_INTERPOLATION_DEFAULT,
                   VALUE_ALPHA_INTERPOLATION_QUALITY,
                   VALUE_ALPHA_INTERPOLATION_SPEED);
    }
    
    public static void setColorQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_COLOR_RENDERING, on,
                   VALUE_COLOR_RENDER_DEFAULT,
                   VALUE_COLOR_RENDER_QUALITY,
                   VALUE_COLOR_RENDER_SPEED);
    }

    public static void setFontQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_FRACTIONALMETRICS, on,
                   VALUE_FRACTIONALMETRICS_DEFAULT,
                   VALUE_FRACTIONALMETRICS_ON,
                   VALUE_FRACTIONALMETRICS_OFF);
    }

    public static void setAliasQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_ANTIALIASING, on,
                   VALUE_ANTIALIAS_DEFAULT,
                   VALUE_ANTIALIAS_ON,
                   VALUE_ANTIALIAS_OFF);
    }

    public static void setAliasTextQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_TEXT_ANTIALIASING, on,
                   VALUE_TEXT_ANTIALIAS_DEFAULT,
                   VALUE_TEXT_ANTIALIAS_ON,
                   VALUE_TEXT_ANTIALIAS_OFF);
    }


    public static void setDitherQuality(Graphics g, Boolean on) {
        setQuality(g,KEY_DITHERING, on,
                   VALUE_DITHER_DISABLE,
                   VALUE_DITHER_ENABLE,
                   VALUE_DITHER_DEFAULT);
    }
    

    private static void setQuality
        (final Graphics g,
         final RenderingHints.Key key,
         final Boolean on,
         final Object defaultValue,
         final Object qualityValue,
         final Object speedValue)
    {
        final Object hint;
        
        if (on == DEFAULT)
            hint = defaultValue;
        else if (on)
            hint = qualityValue;
        else
            hint = speedValue;
        
        if (DEBUG.PAINT) {
            System.out.format(TERM_PURPLE + "%s %7s for %-41s = %s\n" + TERM_CLEAR, Util.tag(g),
                              on==DEFAULT?"DEFAULT": (on ? "quality" : "speed"),
                              '[' + key.toString() + ']',
                              Util.tags(hint.toString()));
        }
        
        ((Graphics2D)g).setRenderingHint(key, hint);
    }
    
    
    protected void out(String s) {
        System.err.println(Util.TERM_PURPLE
                           + this
                           + Util.TERM_CLEAR
                           + " " + s);
    }

    private static void msg(String s) {
        System.err.println(Util.TERM_PURPLE
                           + s
                           + Util.TERM_CLEAR);
    }
    
    
    
    public DrawContext(DrawContext dc)
    {
        this(dc, dc.focal);
    }
        
    // todo: replace with a faster clone op?
    public DrawContext(DrawContext dc, LWComponent newFocal) {
        //System.out.println("transform before dupe: " + dc.g.getTransform());
        this.g = (Graphics2D) dc.g.create();
        //this.g = dc.g;

        // This helps itext (tho is not a workaround) -- it looks like the GC provided by
        // itext doesn't handle create properly -- it almost looks as if a fill is required
        // every time a new GC is created for fill's in subsequent child fill's or even text to work
        // (tho stroking does seem to work) -- e.g., a group with no fill at all will
        // not fill any of it's children, or draw any text -- you only see strokes.
        // Setting a fill doesn't always guarantee to fix this tho...  vanilla
        // side (non map-view) content seems to be the worst.
        
        //g.setColor(new java.awt.Color(4,4,4,4)); // helps
        //g.setColor(new java.awt.Color(0,0,0,0)); // less help
        //g.fillRect(-Short.MAX_VALUE/2, -Short.MAX_VALUE/2, Short.MAX_VALUE, Short.MAX_VALUE);
        
                
        //System.out.println("transform after  dupe: " + g.getTransform());
        this.zoom = dc.zoom;
        this.offsetX = dc.offsetX;
        this.offsetY = dc.offsetY;
        this.disableAntiAlias = dc.disableAntiAlias;
        this.index = dc.index;
        this.isInteractive = dc.isInteractive;
        this.isDraftQuality = dc.isDraftQuality;
        this.isBlackWhiteReversed = dc.isBlackWhiteReversed;
        this.isPresenting = dc.isPresenting;


        this.isDrawingPathways = dc.isDrawingPathways;
        //this.activeTool = dc.activeTool;
        //this.inMapDraw = dc.inMapDraw;
        this.mapTransform = dc.mapTransform;
        this.frame = dc.frame;
        this.focal = newFocal;
        this.alpha = dc.alpha;
        //this.drawAbsoluteLinks = dc.drawAbsoluteLinks;
        this.maxLayer = dc.maxLayer;
        this.rawClip = dc.rawClip;
        this.rawTransform = dc.rawTransform;
        this.masterClipRect = dc.masterClipRect;
        this.skipDraw = dc.skipDraw;
        this.fillColor = dc.fillColor;
        this.isClipOptimized = dc.isClipOptimized;
        this.isAnimating = dc.isAnimating;
        this.focused = dc.focused;

        if (DEBUG.PAINT&&DEBUG.META) out("CLONE of " + dc);
        //out("CLONED: " + Util.tag(masterClipRect) + " from " + dc);
        //Util.printClassTrace("tufts.vue", "CLONE " + this);        
        
        //this.mAlpha = dc.mAlpha;
    }


    private static final Multiset DebugRecording = com.google.common.collect.HashMultiset.create();

    public static void recordDebug(Object value) {
        DebugRecording.add(value);
    }

    public static void recordDebug(LWComponent c) {
        //final String type = c.getClass().getName();
        final String type = c.getComponentTypeLabel();
        if (c.getTypeToken() != null && c.getTypeToken() != c.getClass())
            recordDebug(type + ":" + c.getTypeToken());
        else
            recordDebug(type);
    }
    
    
    public static String getDebug() {
        return DebugRecording.toString();
    }
    
    public static void clearDebug() {
        DebugRecording.clear();
    }

    private final static boolean PlatformQualityIsSlow;
    
    public static boolean drawingMayBeSlow(final LWComponent focal) {
        // could check focal for the presence of images
        //if (true) return false;
        if (PlatformQualityIsSlow && isImageQualityRequested())
            return focal instanceof LWMap;
        else
            return false;
    }

    //private final static BooleanPreference ImageQualityPreference;
    
    public static boolean isImageQualityRequested() {
        //return ImageQualityPreference != null && ImageQualityPreference.isTrue();
        //return ImageQualityPreference.isTrue();
        return true;
    }

    static {
        if (Util.isMacLeopard() && Util.getJavaVersion() < 1.6f)
            PlatformQualityIsSlow = true; // slow, but the only one truly effective!
        else
            PlatformQualityIsSlow = false;
        
//         ImageQualityPreference = BooleanPreference.create
//                 (edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
//                  "imageQuality", 
//                  VueResources.getString("preference.imageQuality.title", "Image Quality"), 
//                  VueResources.getString("preference.imageQuality.description",
//                                         "Disabling this will make VUE faster when working on maps with images"
//                                         ),
//                  Boolean.TRUE,
//                  true);


    }

//     private final static BooleanPreference ImageQualityPreference = BooleanPreference.create(
// 			edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
// 			"imageQuality", 
// 			VueResources.getString("preference.imageQuality.title", "Image Quality"), 
// 			VueResources.getString("preference.imageQuality.description",
//                                                "Disabling this will make VUE faster when working on maps with images"
//                                                ),
// 			Boolean.TRUE,
// 			true);
    


//     @Override
//     public final Object clone() {
// //         final DrawContext dc = new DrawContext(g);
// //         return dc;
//         try {
//             final DrawContext dc = (DrawContext) super.clone();
//             dc.focal = null;
//             return dc;
//         } catch (CloneNotSupportedException e) {
//             e.printStackTrace();
//         }
//         return null;
//     }

}
