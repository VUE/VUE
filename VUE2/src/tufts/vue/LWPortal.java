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

import tufts.Util;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;


/**
 * Special map portal.
 *
 * We need this for now to subclass LWNode just to support the shape property.
 * If we were to move the shape key into LWComponent, we could do away with
 * this class, and just use an LWComponent with dynamically disabled properies
 * as we see fit...
 *
 * @version $Revision: 1.13 $ / $Date: 2007-08-31 01:11:42 $ / $Author: sfraize $ 
 */

public class LWPortal extends LWNode
{
    private static final Color DarkFill = new Color(0,0,0,64);
    private static final Color LightFill = new Color(255,255,255,64);
    private static final Color DebugFill = new Color(0,255,0,128);
    private static final Color DefaultFill = new Color(128,128,128,128);
    
    public LWPortal() {
        disablePropertyTypes(KeyType.STYLE);
        //enableProperty(LWKey.StrokeWidth);
        //enableProperty(LWKey.StrokeColor);
        enableProperty(LWKey.Shape);
        //disableProperty(LWKey.Label);
    }

    public static LWPortal create() {
        final LWPortal p = new LWPortal();
        p.setStrokeWidth(0);
        p.setSize(LWSlide.SlideWidth / 4, LWSlide.SlideHeight / 4);
        //setAspect(LWSlide.SlideAspect);
        p.setLabel("Presentation Portal");
        return p;
    }


    /* override to do nothing so we aren't constrainted by LWNode's minimum size*/
    //@Override protected void layoutImpl(Object triggerKey) {}
    //@Override protected void layout(Object triggerKey, Size curSize, Size request) {} // overkill: shrinks to nothing?
    /** override to so we aren't constrainted by LWNode's minimum size */
    @Override
    protected Size getTextSize() { return Size.None; }

    @Override
    protected void userSetSize(float width, float height, MapMouseEvent e)
    {
        if (e.isShiftDown()) {
            // Allow constraining to slide aspect:
            Size newSize = ConstrainToAspect(LWSlide.SlideAspect, width, height);
            super.setSize(newSize.width, newSize.height);
        } else {
            super.setSize(width, height);
        }
    }

    @Override
    public boolean supportsUserLabel() {
        return false;
    }

    /** @return false: portals can never have slides of their own */
    @Override
    public final boolean supportsSlide() {
        return false;
    }
    
    @Override
    public int getFocalMargin() {
        return 0;
    }

    @Override
    protected boolean containsImpl(final float x, final float y, PickContext pc) {
        if (pc.isZoomRollover)
            return false; // allow picking through the portal -- never zoom portals
        else
            return super.containsImpl(x, y, pc);
    }
    
    @Override
    public Color getRenderFillColor(DrawContext dc) {
        if (false&&dc != null && dc.focal != null)
            return dc.focal.mFillColor.brightness() > 0.5 ? DarkFill : LightFill;
        else
            return getMap().mFillColor.brightness() > 0.5 ? DarkFill : LightFill;
    }

    @Override
    protected void drawImpl(DrawContext dc)
    {
        if (dc.skipDraw == this)
            return;

        //if (dc.focal instanceof LWPortal || !dc.isInteractive()) {
        if (dc.focal == this) {

            final AffineTransform zeroTransform = DEBUG.CONTAINMENT ? dc.g.getTransform() : null;

//             dc.setAbsoluteStroke(5);
//             dc.g.setColor(Color.red);
//             dc.g.fill(getZeroShape());
//             //dc.g.draw(getZeroShape());
            
            // Okay, I think this is really best handled in SlideViewer & PresentationTool, tho
            // would be nice if that code could be merged...
            //dc.g.setColor(getRenderFillColor(dc));
            //dc.g.fill(getZeroShape());
            dc.setMapDrawing();
            dc.setDrawPathways(false);
            dc.skipDraw = this;
            if (DEBUG.CONTAINMENT) {
                final DrawContext alphaDC = dc.create();
                alphaDC.setAlpha(0.1, AlphaComposite.SRC);
                getParent().draw(alphaDC);
                alphaDC.dispose();

                final DrawContext clipDC = dc.create();
                clipDC.setMasterClip(getMapShape());
                getParent().draw(clipDC);
                clipDC.dispose();

                dc.g.setTransform(zeroTransform);
                dc.setAbsoluteStroke(1);
                dc.g.setColor(Color.red);
                dc.g.draw(getZeroShape());
            } else {
                dc.setMasterClip(getMapShape());
                getParent().draw(dc);
            }
            
        } else if (dc.focal instanceof LWPortal) {
            // no fill: don't show the portal fill if we, or any
            // other portal, is currently the focal
        } else if (hasEntries()) {
//             final LWPathway exclusive = getExclusiveVisiblePathway();
//             if (exclusive != null)
//                 dc.g.setColor(exclusive.getColor());
//             else if (inPathway(VUE.getActivePathway()))
//                 dc.g.setColor(VUE.getActivePathway().getColor());
//             else 
//                 dc.g.setColor(getRenderFillColor(dc));

            final Color fill = getPriorityPathwayColor(dc);
            if (fill == null)
                dc.g.setColor(getRenderFillColor(dc));
            else
                dc.g.setColor(fill);

            //dc.g.setColor(DefaultFill);
            if (dc.zoom > PathwayOnTopZoomThreshold) {
                dc.g.setStroke(new java.awt.BasicStroke(LWPathway.PathBorderStrokeWidth));
                dc.g.draw(getZeroShape());
            } else {
                dc.g.fill(getZeroShape());
            }
        } else {
            // Show the portal region:
            dc.g.setColor(getRenderFillColor(dc));
            dc.g.fill(getZeroShape());
        }


        
//         if (DEBUG.BOXES || DEBUG.CONTAINMENT) {
//             dc.g.setColor(DebugFill);
//             dc.g.fill(getZeroShape());
//         } else if (dc.focal instanceof LWPortal || !dc.isInteractive()) {
//             // no fill: don't show the portal fill if we, or any
//             // other portal, is currently the focal
//             if (false) {
//                 dc.g.setColor(Color.blue);
//                 dc.g.setStroke(VueConstants.STROKE_TWO);
//                 dc.g.draw(getZeroShape());
//             }
//         } else {
//             // Show the portal region:
//             dc.g.setColor(getRenderFillColor(dc));
//             dc.g.fill(getZeroShape());
//         }
    }

    /*
    private boolean wasVisible = true;
    @Override public boolean isVisible() {
        // TODO: handle this in LWPathway and actually set a hidden bit...
        boolean visible;
        final java.util.Collection pathways = getPathways();
        if (pathways.size() > 0) {
            visible = false;
            for (LWPathway p : getPathways())
                if (p.isVisible())
                    visible = true;
        } else
            visible = true;

        if (wasVisible != visible) {
            wasVisible = visible;
            notify(LWKey.Hidden);
        }
        return visible;
    }
    */
    

    @Override
    public boolean hasLabel() { // so LWNode won't draw it
        return false;
    }
    
    @Override
    public boolean supportsChildren() {
        return false;
    }
    @Override
    public boolean supportsReparenting() {
        return false;
    }

    @Override
    protected boolean iconShowing() {
        return false;
    }

}
    
