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

import java.awt.Container;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;

/**
 *
 * Zoom tool handler for MapViewer, and static code for computing
 * zoom needed to display an arbitraty map region into an arbitrary
 * pixel region.
 *
 * @version $Revision: 1.49 $ / $Date: 2007-04-10 19:51:46 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class ZoomTool extends VueTool
    implements VueConstants
{
    static private final int ZOOM_MANUAL = -1;
    static private final double[] ZoomDefaults = {
        1.0/100, 1.0/64, 1.0/48, 1.0/32, 1.0/24, 1.0/16, 1.0/12, 1.0/8, 1.0/6, 1.0/5, 1.0/4, 1.0/3, 1.0/2, 2.0/3, 0.75,
        1.0,
        1.25, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 128
        //, 96, 128, 256, 384, 512
    };
    static private final int ZOOM_FIT_PAD = 20; // make this is > SelectionStrokeWidth & SelectionHandleSize
    static private final double MaxZoom = ZoomDefaults[ZoomDefaults.length - 1];

    
    public ZoomTool() {
        super();
    }
	
    public JPanel getContextualPanel() {
        return VueToolbarController.getController().getSuggestedContextualPanel();
    }

    private static final Color SelectorColor = Color.red;
    private static final Color SelectorColorInverted = new Color(0,255,255); // inverse of red
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        /*
        if (VueUtil.isMacPlatform())
            g.setXORMode(SelectorColorInverted);
        else
            g.setXORMode(SelectorColor);
        */
        g.setColor(Color.red);
        super.drawSelector(g, r);
    }

    public boolean usesRightClick()
    {
        return true;
    }

    public boolean isZoomOutMode() {
        return getSelectedSubTool().getID().equals("zoomTool.zoomOut");
    }

    public boolean supportsSelection() { return false; }

    public boolean supportsDraggedSelector(MapMouseEvent e)
    {
        if (false && e.getPicked() != null) // is causing a pick traversal for ever mouse drag event: too slow
            return false;
        
        // todo: if zoom level on viewer == MaxZoom, return false
        
        // This is so that if they RIGHT click, the dragged selector doesn't appear --
        // because right click in zoom does a zoom out, and it makes less sense to
        // zoom out on a particular region.
        // Need to recognize button 1 on a drag, where getButton=0, or a release, where modifiers 0 but getButton=1
        return !isZoomOutMode() &&
            (e.getButton() == MouseEvent.BUTTON1 || (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0);
    }
    
    private boolean pickedZoom = false;
    private boolean ignoreRelease = false;
    public boolean handleMousePressed(MapMouseEvent e) {
        super.handleMousePressed(e);

        final MapViewer viewer = e.getViewer();

        if (pickedZoom && !e.isShiftDown()) {
            setZoomFit(viewer, true);
            pickedZoom = false;
            ignoreRelease = true;
            return true;
        } else if (e.getPicked() != null)  {
            if (e.getPicked() instanceof LWSlide) {
                final LWSlide slide = (LWSlide) e.getPicked();
                setZoomFitRegion(viewer,
                                 slide.getSourceNode().getSlideIconBounds(),
                                 0,
                                 true);

                tufts.vue.gui.GUI.invokeAfterAWT(new Runnable() {
                        public void run() {
                            viewer.loadFocal(slide);
                            //setZoomFitRegion(viewer, slide.getBounds(), 0, false);
                        }});
                
            } else {
                setZoomFitRegion(viewer,
                                 e.getPicked().getBounds(),
                                 0,
                                 true);
            }
            pickedZoom = true;
            ignoreRelease = true;
            return true;
        } else
            return false;
    }
    
    public boolean handleMouseReleased(MapMouseEvent e)
    {
        if (DEBUG.TOOL) System.out.println(this + " handleMouseReleased " + e);

        if (ignoreRelease) {
            ignoreRelease = false;
            return true;
        }

        //Point p = e.getPoint();
        Point2D p = e.getMapPoint();
        
        if (e.isShiftDown() || e.getButton() != MouseEvent.BUTTON1
            //|| toolKeyEvent != null && toolKeyEvent.isShiftDown()
            ) {
            if (isZoomOutMode())
                setZoomBigger(p);
            else
                setZoomSmaller(p);
        } else {
            Rectangle box = e.getSelectorBox();
            if (box != null && box.width > 10 && box.height > 10) {
                setZoomFitRegion(e.getMapSelectorBox());
            } else {
                if (isZoomOutMode())
                    setZoomSmaller(p);
                else
                    setZoomBigger(p);
            }
        }
        return true;
    }
    
    public boolean handleKeyPressed(KeyEvent e){return false;}
    
    public static boolean setZoomBigger(Point2D focus)
    {
        double curZoom = VUE.getActiveViewer().getZoomFactor();
        for (int i = 0; i < ZoomDefaults.length; i++) {
            if (ZoomDefaults[i] > curZoom) {
                setZoom(ZoomDefaults[i], focus);
                return true;
            }
        }
        return false;
    }
    
    public static boolean setZoomSmaller(Point2D focus)
    {
        double curZoom = VUE.getActiveViewer().getZoomFactor();
        for (int i = ZoomDefaults.length - 1; i >= 0; i--) {
            if (ZoomDefaults[i] < curZoom) {
                setZoom(ZoomDefaults[i], focus);
                return true;
            }
        }
        return false;
    }

    /** special zoom focus marker: use center of view as zoom anchor */
    private static final Point2D CENTER_FOCUS = new Point2D.Float();
    /** special zoom focus marker: don't adjust around a focal point: just adjust the zoom */
    private static final Point2D DONT_FOCUS = new Point2D.Float();
    
    public static void setZoom(double zoomFactor)
    {
        setZoom(VUE.getActiveViewer(), zoomFactor, true, CENTER_FOCUS, false);
    }
    public static void setZoom(double zoomFactor, Point2D focus)
    {
        setZoom(VUE.getActiveViewer(), zoomFactor, true, focus, false);
    }

    /**
     * @param focus - map location to anchor the zoom at (keep at same screen location)
     */
    private static void setZoom(MapViewer viewer, double newZoomFactor, boolean adjustViewport, Point2D focus, boolean reset)
    {
        // this is much simpler as the viewer now handles adjusting for the focal point
        if (focus == DONT_FOCUS)
            focus = null;
        //else if (focus instanceof Point) {
            // if a Point and not just a Point2D, it was a screen coordinate from setZoomBigger/Smaller
            //focus = viewer.screenToMapPoint((Point)focus);
        //}
        else if (adjustViewport && (focus == null || focus == CENTER_FOCUS)) {
            // If no user selected zoom focus point, zoom in to
            // towards the map location at the center of the
            // viewport.
            if (DEBUG.SCROLL) System.out.println("VISIBLE CENTER " + viewer.getVisibleCenter());
            focus = viewer.screenToMapPoint2D(viewer.getVisibleCenter());
        }

        // If zooming in, anchor to the click point.  If zooming out, always
        // zoom out from the center.
        if (newZoomFactor > viewer.mZoomFactor)
            viewer.setZoomFactor(newZoomFactor, reset, focus, false);
        else
            viewer.setZoomFactor(newZoomFactor, reset, null, true);
    }
    
    /** @param currently only works if NOT in a scroll pane */
    public static void setZoomFitRegion(MapViewer viewer, Rectangle2D mapRegion, int edgePadding, boolean animate)
    {
        if (mapRegion == null) {
            new Throwable("setZoomFitRegion: mapRegion is null for " + viewer).printStackTrace();
            return;
        }
        Point2D.Double offset = new Point2D.Double();
        double newZoom = computeZoomFit(viewer.getVisibleSize(),
                                        edgePadding,
                                        mapRegion,
                                        offset);
        
        if (viewer.inScrollPane()) {
            Point2D center = new Point2D.Double(mapRegion.getCenterX(), mapRegion.getCenterY());
            if (newZoom > MaxZoom)
                newZoom = MaxZoom;

            viewer.setZoomFactor(newZoom, false, center, true);
            
        } else {
            if (newZoom > MaxZoom) {
                setZoom(viewer, MaxZoom, true, CENTER_FOCUS, true);
                Point2D mapAnchor = new Point2D.Double(mapRegion.getCenterX(), mapRegion.getCenterY());
                Point focus = new Point(viewer.getVisibleWidth()/2, viewer.getVisibleHeight()/2);
                double offsetX = (mapAnchor.getX() * MaxZoom) - focus.getX();
                double offsetY = (mapAnchor.getY() * MaxZoom) - focus.getY();
                viewer.setMapOriginOffset(offsetX, offsetY);
                //viewer.resetScrollRegion();
            } else {

                if (animate) {
                    animatedZoomTo(viewer, newZoom, offset);
                    //if (DEBUG.Enabled) System.out.println("zoomFinal " + newZoom);
                }
                
                setZoom(viewer, newZoom, false, DONT_FOCUS, true);
                viewer.setMapOriginOffset(offset.getX(), offset.getY());
            }
        }
    }

    /** Animate all but the last step of a zoom to the given given zoom and offset.   Caller must provide the final calls. */
    private static void animatedZoomTo(MapViewer viewer, double newZoom, Point2D offset)
    {
        // This will currenly only work on a viewer that's NOT
        // in a scroll-pane (so ony full-screen windows for now)
        // as the repaint does nothing to adjust the scrolling
        // viewport.
        
        if (viewer.inScrollPane())
            return;
        
        final int frames = 4; // will do frame-1 intermediate frames: last is left to caller for the exact final value

        double cz = viewer.getZoomFactor();
        double cx = viewer.getOriginX();
        double cy = viewer.getOriginY();
                
        double dz = newZoom - cz;
        double dx = offset.getX() - cx;
        double dy = offset.getY() - cy;

        double iz = dz/frames;
        double ix = dx/frames;
        double iy = dy/frames;

        for (int i = 1; i < frames; i++) {
            double zoom = cz + iz*i;
            setZoom(viewer, zoom, false, DONT_FOCUS, true);
            viewer.setMapOriginOffset(cx + ix*i, cy + iy*i);
            viewer.paintImmediately();
            //if (DEBUG.Enabled) System.out.println("zoomAnimate " + zoom);
        }
    }
    
    public static void setZoomFitRegion(Rectangle2D mapRegion, int edgePadding)
    {
        setZoomFitRegion(VUE.getActiveViewer(), mapRegion, edgePadding, false);
    }
    
    public static void setZoomFitRegion(Rectangle2D mapRegion)
    {
        setZoomFitRegion(mapRegion, 0);
    }
    
    /** fit all of the map contents for the given viewer to be visible */
    public static void setZoomFit(MapViewer viewer) {
        setZoomFit(viewer, false);
    }
        
    /** fit all of the map contents for the given viewer to be visible */
    public static void setZoomFit(MapViewer viewer, boolean animate)
    {
        // if don't want this to vertically center map in viewport, will need
        // to tell setZoomFitRegion above to compute center using mapRegion.getY()
        // instead of mapRegion.getCenterY()
        setZoomFitRegion(viewer, viewer.getDisplayableMapBounds(), DEBUG.MARGINS ? 0 : ZOOM_FIT_PAD, animate);
        //setZoomFitRegion(viewer, viewer.getMap().getBounds(), DEBUG.MARGINS ? 0 : ZOOM_FIT_PAD, false);
        // while it would be nice to call getActiveViewer().getContentBounds()
        // as a way to get bounds with max selection edges, etc, it computes some
        // of it's size based on current zoom, which we're about to change, so
        // we can't use it as our zoom fit becomes a circular, cycling computation.
    }
    
    /** fit everything in the current map into the current viewport */
    public static void setZoomFit() {
        setZoomFit(VUE.getActiveViewer());
    }
    
    
    public static double computeZoomFit(Dimension viewport, int borderGap, Rectangle2D bounds, Point2D offset) {
        return computeZoomFit(viewport, borderGap, bounds, offset, true);
    }
    
    /**
     * Compute two items: the zoom factor that will fit everything
     * within the given map bounds into the given viewport, and put
     * into @param offset the offset to place the viewport at. Used to
     * figure out how to fit everything within a map on the screen and
     * where to pan to so you can see it all.
     *
     * @param outgoingOffset may be null (not interested in result)
     */
    public static double computeZoomFit(java.awt.Dimension viewport,
                                        int borderGap,
                                        java.awt.geom.Rectangle2D bounds,
                                        java.awt.geom.Point2D outgoingOffset,
                                        boolean centerSmallerDimensionInViewport)
    {
        int viewWidth = viewport.width - borderGap * 2;
        int viewHeight = viewport.height - borderGap * 2;
        double vertZoom = (double) viewHeight / bounds.getHeight();
        double horzZoom = (double) viewWidth / bounds.getWidth();
        boolean centerVertical;
        double newZoom;
        if (horzZoom < vertZoom) {
            newZoom = horzZoom;
            centerVertical = true;
        } else {
            newZoom = vertZoom;
            centerVertical = false;
        }

        // Now center the components within the dimension
        // that had extra room to scale in.
                    
        if (outgoingOffset != null) {
            double offsetX = bounds.getX() * newZoom - borderGap;
            double offsetY = bounds.getY() * newZoom - borderGap;
            
            if (centerSmallerDimensionInViewport) {
                if (centerVertical)
                    offsetY -= (viewHeight - bounds.getHeight()*newZoom) / 2;
                else // center horizontal
                    offsetX -= (viewWidth - bounds.getWidth()*newZoom) / 2;
            }
            outgoingOffset.setLocation(offsetX, offsetY);
        }
            
        return newZoom < ZoomDefaults[0] ? 1.0 : newZoom; // if less than 1%, go to 100% -- assume nothing in map
    }


    public static String prettyZoomPercent(double zoom) {
        double zoomPct = zoom * 100;
        if (zoomPct < 10) {
            // if < 10% zoom, show with 1 digit of decimal value if it would be non-zero
            return VueUtil.oneDigitDecimal(zoomPct) + "%";
        } else {
            //title += (int) Math.round(zoomPct);
            return ((int)Math.floor(zoomPct + 0.49)) + "%";
        }
    }
    
    
}
