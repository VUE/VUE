/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Implements a panel for displaying a map overview, including
 * the currently visible viewport, and moving (panning) the currently
 * visible viewport.
 *
 * @version $Revision: 1.65 $ / $Date: 2008-05-20 21:46:37 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
// TODO: fix our aspect to that of canvas (if that's what we tracking, the whole map otherwise);
public class MapPanner extends javax.swing.JPanel
    implements VueConstants,
               MapViewer.Listener,
               LWComponent.Listener,
               MouseListener,
               MouseMotionListener,
               MouseWheelListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapPanner.class);
    
    private MapViewer mapViewer; // active MapViewer
    private double zoomFactor; // zoomFactor that will fit entire map in the panner
    private Point dragStart; // where mouse was at mouse press
    private Point lastDrag; // where mouse was at last drag
    private Point2D mapStart; // where map origin was at mouse press
    private LWMap map; // active map

    // Enable this to keep viewport always visible in panner: (it causes while-you-drag
    // zoom adjusting tho, which can be a bit disorienting)
    private static final boolean ViewerViewportAlwaysVisible = true;

    // If false, map in panner will constantly resize to fit
    // as large a visible area as possible, tho not fully supported yet,
    // as dragging is funky if ViewerViewportAlwaysVisible is true,
    // and the viewport is dragged outside the total LWMap bounds (onto empty canvas).
    private static final boolean FullScrollCanvasAlwaysVisible = false;

    // false not fully supported yet (dragging wierd once hit edges: not absolute based dragged)
    // Also: at bottom and right, MapViewer jitters.  What really want is ability to allow
    // this, but say, not leave the at least a corner on the existing LWMap bounds.
    private static final boolean AutomaticallyGrowScrollRegions = true;
    
    private static final int MapMargin = 0;
    //private static final int MapMargin = ViewerViewportAlwaysVisible ? 5 : 50;
    
    /**
     * Get's global (thru AWT hierarchy) MapViewerEvent's
     * to know what to display & when to update.  It's
     * intended that there only be one MapPanner in the
     * application at a time.
     */
    public MapPanner()
    {
        //setPreferredSize(new Dimension(150,100));
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        setMinimumSize(new Dimension(200,125));

        VUE.addActiveListener(MapViewer.class, this);
    }

    public void addNotify()
    {
        super.addNotify();
        if (getParent() instanceof Window)
            ((Window)getParent()).setFocusableWindowState(false);
    }

    
    /**
     * All instances of MapViewer raise MapViewer events
     * as they act, and the MapPanner hears all of them
     * here.
     */
    public void mapViewerEventRaised(MapViewerEvent e)
    {
        if (VUE.inNativeFullScreen())
            return;
        
        if (e.isActivationEvent() && mapViewer == null) {
            setViewer(e.getMapViewer());
        } else if (e.getSource() == this.mapViewer
            && (e.getID() == MapViewerEvent.PAN ||
                e.getID() == MapViewerEvent.ZOOM)) {
            repaint();
            if (e.getID() == MapViewerEvent.ZOOM)
                updateZoomTitle();
        }
    }

    private void updateZoomTitle() {
        String titleInfo = null;
        if (this.mapViewer != null)
            titleInfo = ZoomTool.prettyZoomPercent(this.mapViewer.getZoomFactor());
        putClientProperty("TITLE-INFO", titleInfo);
    }

    public void activeChanged(ActiveEvent e, MapViewer viewer) {
        setViewer(viewer);
    }
    
    private void setViewer(MapViewer mapViewer)
    {
        if (VUE.inNativeFullScreen())
            return;
        
        if (DEBUG.FOCUS) out("setViewer " + mapViewer);
        if (this.mapViewer != mapViewer) {
            this.mapViewer = mapViewer;
            if (mapViewer != null)
                setMap(mapViewer.getMap());
            repaint();
            updateZoomTitle();
        }
    }

    private void setMap(LWMap map) {
        if (DEBUG.FOCUS) out("setMap " + map);
        if (this.map != map) {
            if (this.map != null)
                this.map.removeLWCListener(this);
            this.map = map;
            if (DEBUG.Enabled)
                this.map.addLWCListener(this);
            else
                this.map.addLWCListener(this, LWKey.UserActionCompleted);
        }
    }

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.DYNAMIC_UPDATE || e.key == LWKey.UserActionCompleted || e.key == LWKey.RepaintAsync)
            repaint();
    }

    public void mousePressed(MouseEvent e)
    {
        if (DEBUG.MOUSE) out(e);
        dragStart = lastDrag = e.getPoint();
        mapStart = mapViewer.getOriginLocation();
        repaint();
    }
    public void mouseReleased(MouseEvent e)
    {
        if (DEBUG.MOUSE) out(e);
        dragStart = lastDrag = null;
    }

    // TODO: What should remain constant is the absolute PANNING AMOUNT across the MAP:
    // nothing to do with the panner viewport itself: then we can have constant mouse
    // response on dragging, even if panner display is changing zoom...  You're really
    // dragging the MAP, not the panner reticle...  Also, this could allow for making
    // the map drag-unit much less chunky...  Shit, tho this may FEEL right, it would
    // look funny, as the mouse would still drift from it's location relative to the
    // reticle...  But at least it would REDUCE this drift, and get rid of the crazy
    // accelleration experiences as you drag reticle further off map, creating huge
    // canvas, which makes each mouse move look bigger...

    // Also: best guess constraint to address Melanie's concern about creating more
    // map canvas: the corners of the map can't go further out than the center
    // of the reticle...

    // Or if wanted to do SIMPLE: Panner uses it's own virtual canvas, which
    // is min canvas size (so union with actual scroll-pane generated canvas),
    // which is defined my map bounds plus 1/2 viewport dimensions, then
    // we never have dynamic scaling, and mouse response can be perfect.
    // Tho when zooming in/out, that means not only reticle changes, but
    // the panner displayed map gets bigger/smaller...  So maybe something
    // else we can bas it on: how about the MapViewer viewport itself:
    // half of that?  Crap, same problem: maybe half a viewport at 100% zoom?
    

    public void mouseDragged(MouseEvent e)
    {
        if (DEBUG.MOUSE) out(e);
        if (dragStart == null)
            return;

        /*
        Rectangle   viewerBounds = new Rectangle(mapViewer.getWidth()-1, mapViewer.getHeight()-1);
        if (viewerBounds.isEmpty())
            return;
        Rectangle2D mapViewerRect = mapViewer.screenToMapRect(viewerBounds);
        boolean keepx = false;
        if (mapViewerRect.getX() <= pannerMinX)
            keepx = true;
        // No good -- need to pre-compute this!
        //
        // It's surprisingly complex to try and figure out in advance if repositioning the map
        // change the panner zoom offset...  (And we still need to support being "off the grid"
        // in any case because the user can always manually drag the main view into outer-space)
        */

        if (DEBUG.SCROLL) out("mouse " + e.getPoint());
        int x = e.getX();
        int y = e.getY();

        if (mapViewer.inScrollPane()) {
            /*
            if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
                lastDrag = e.getPoint();
                return;
            }
            */
            // TODO: panScrollRegion needs dx,dy to work, but then we give up absolute
            // mouse delta tracking from drag start, which makes for poor correlation
            // between mouse movements and panner movements if the scale starts changing
            // (e.g., we go outside the LWMap bounds, and start auto-growing the
            // canvas).
            
            int dx = x - lastDrag.x;
            int dy = y - lastDrag.y;
            double factor = mapViewer.getZoomFactor() / this.zoomFactor;

            dx = (int) (dx * factor + 0.5);
            dy = (int) (dy * factor + 0.5);
            
            if (DEBUG.SCROLL) out("dx="+dx + " dy="+dy);
            
            mapViewer.panScrollRegion(dx, dy, AutomaticallyGrowScrollRegions);
            lastDrag = e.getPoint();
            
        } else {
            
            if (ViewerViewportAlwaysVisible) {
                // hack till we disallow the maprect from going beyond edge
                if (x < 0) x = 0;
                else if (x > getWidth()-2) x = getWidth()-2;
                if (y < 0) y = 0;
                else if (y > getHeight()-2) y = getHeight()-2;
            }
                
            double factor = this.zoomFactor / mapViewer.getZoomFactor();
            double dragOffsetX = (x - dragStart.getX()) / factor;
            double dragOffsetY = (y - dragStart.getY()) / factor;
            mapViewer.setMapOriginOffset(mapStart.getX() + dragOffsetX,
                                         mapStart.getY() + dragOffsetY);

            //if (mapViewer.inScrollPane())
            //    mapViewer.adjustExtent();

            mapViewer.repaint();
            repaint();
        }

    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        /*
        if (mapViewer != null)
            mapViewer.getMouseWheelListener().mouseWheelMoved(e);
        */
        int rotation = e.getWheelRotation();
        if (rotation > 0)
            tufts.vue.ZoomTool.setZoomSmaller(null);
        else if (rotation < 0)
            tufts.vue.ZoomTool.setZoomBigger(null);
    }
    
    public void paintComponent(Graphics g)
    {
        if (VUE.inNativeFullScreen())
            return;
        
        if (DEBUG.PAINT) System.out.println("\nPANNER PAINTING");
        super.paintComponent(g);
        
        if (mapViewer == null) {
            setViewer(VUE.getActiveViewer());//todo: remove
            // problem is at startup, somehow we no longer get an active viewer event
            // -- something got broke
            if (mapViewer == null)
                return;
        }

        final Rectangle pannerSize = new Rectangle(getSize());
        pannerSize.width -= 1;
        pannerSize.height -= 1;
        paintViewerIntoRectangle(this, g, this.mapViewer, pannerSize, true);
    }

    public static DrawContext paintViewerIntoRectangle(Graphics g, final MapViewer viewer, final Rectangle pannerSize) {
        return paintViewerIntoRectangle(null, g, viewer, pannerSize, true);
    }
    
    // TODO: take a DrawContext, not a viewer
    /**
     * @return the DrawContext that was used to draw the viewer contents into the given paintRect (to provide zoom/offset for picking)
     * Note that the x/y location of paintRect is ignored.
     */
    static DrawContext paintViewerIntoRectangle(final MapPanner panner,
                                                final Graphics g,
                                                final MapViewer viewer,
                                                final Rectangle paintRect,
                                                final boolean drawViewerReticle)
    {
        if (viewer.getVisibleWidth() < 1 || viewer.getVisibleHeight() < 1) {
            if (DEBUG.Enabled)
                System.out.println("MapPanner: paintViewerIntoRectangle: nothing to paint; visible size="
                                   + viewer.getVisibleSize()
                                   + " in " + viewer);
            return null;
        }

        final LWMap map = viewer.getMap();

        if (map == null) {
            Log.error("null map for viewer " + viewer);
            return null;
        }
        
        final Rectangle2D allComponentBounds = map.getBounds();
        final Rectangle2D canvasRect = viewer.getCanvasMapBounds();
        final Rectangle2D viewerRect = viewer.getVisibleMapBounds();
        final Rectangle2D pannerRect;

        if (ViewerViewportAlwaysVisible && viewer.inScrollPane()) {
            if (FullScrollCanvasAlwaysVisible)
                // the fudgey margins go away with show full canvas -- which indicates
                // the problem w/out the canvas is obviously because we can *drag* to
                // edge of full canvas, but if not computing zoom with it, we'll
                // get zoomed out when we go off edge of map bounds to edge of canvas bounds.
                pannerRect = canvasRect.createUnion(allComponentBounds);
            else
                pannerRect = viewerRect.createUnion(allComponentBounds);
        } else
            pannerRect = allComponentBounds;

        //if (DEBUG.WORK) Log.debug("pannerRect: " + tufts.Util.fmt(pannerRect));
        
        /*
         * Compute the zoom required to fit everything in the size of the
         * current panner tool window.
         */

        final Point2D.Float offset = new Point2D.Float();
        double zoomFactor = ZoomTool.computeZoomFit(paintRect.getSize(),
                                                    DEBUG.MARGINS ? 0 : MapMargin,
                                                    pannerRect,
                                                    offset);
        if (panner != null)
            panner.zoomFactor = zoomFactor;
                                            
        /*
         * Construct a DrawContext to use in painting the entire
         * map on the panner window.
         */

        final DrawContext dc = new DrawContext(g, zoomFactor, -offset.x, -offset.y, null, map, false);

        dc.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, viewer.AA_ON);//pickup MapViewer AA state for debug
        dc.setPrioritizeSpeed(true);
        dc.setFractionalFontMetrics(false);
        dc.setDraftQuality(true); // okay to skimp in rendering of panner image -- it's usually so tiny
        //dc.setMapDrawing(); // no longer needed: the default at dc init

        /*
         * Fill the background representing the currently active canvas region.
         * If the viewer is in a scroll-region, this will be the total area
         * it's scrolling over -- a large canvas.  If not, it will simply be
         * the visible viewer canvas, which virtually "pan's" over the infinite
         * coordinate space the map lies in.
         */
        // need to offset fill, so can't just use existing canvasRect
        //final Rectangle2D fillCanvas = viewer.screenToMapRect(new Rectangle(1,1, viewer.getWidth(), viewer.getHeight()));


        if (drawViewerReticle) {
            dc.g.setColor(map.getFillColor());
            // round size of canvas down...
            //dc.g.fill(canvas);
            // now we only fill visible on-screen area:
            dc.g.fill(viewerRect);
        }
        
        /*
         * Now tell the active LWMap to draw itself here on the panner.
         */
        
        map.draw(dc);

        if (drawViewerReticle) {
            /*
             * Show where the edge of the *visible* viewer region overlaps the map
             */
            dc.setAntiAlias(false);
            if (panner == null)
                dc.setAbsoluteStroke(3);
            else
                dc.setAbsoluteStroke(1);
            dc.g.setColor(Color.red);
            dc.g.draw(viewerRect);
        }

        return dc;
    }

    

    public void mouseClicked(MouseEvent e) { if (DEBUG.MOUSE) out(e); }
    public void mouseEntered(MouseEvent e) { if (DEBUG.MOUSE) out(e); }
    public void mouseExited(MouseEvent e) { if (DEBUG.MOUSE) out(e); }
    public void mouseMoved(MouseEvent e) {}

    private void out(Object o) {
        System.out.println("\t*** " + this + " " + (o==null?"null":o.toString()));
    }
    
    public String toString() {
        return "MapPanner[" + mapViewer + "]";
    }
    
}
