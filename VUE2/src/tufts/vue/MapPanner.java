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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Implements a panel for displaying a map overview, including
 * the currently visible viewport, and moving (panning) the currently
 * visible viewport.
 *
 * @version $Revision: 1.49 $ / $Date: 2006-03-20 18:15:16 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
// TODO: fix our aspect to that of canvas (if that's what we tracking, the whole map otherwise);
public class MapPanner extends javax.swing.JPanel
    implements VueConstants,
               MapViewer.Listener,
               VUE.ActiveViewerListener,
               LWComponent.Listener,
               MouseListener,
               MouseMotionListener,
               MouseWheelListener
{
    private MapViewer mapViewer; // active MapViewer
    private double zoomFactor; // zoomFactor that will fit entire map in the panner
    private Point dragStart; // where mouse was at mouse press
    private Point lastDrag; // where mouse was at last drag
    private Point2D mapStart; // where map origin was at mouse press
    private LWMap map; // active map

    // Enable this to keep viewport always visible in panner: (it causes while-you-drag
    // zoom adjusting tho, which can be a bit disorienting)
    private static final boolean ViewerAlwaysVisible = true;
    private static final boolean ShowFullCanvas = true;
    private static final int MapMargin = 0;
    //private static final int MapMargin = ViewerAlwaysVisible ? 5 : 50;
    
    /**
     * Get's global (thru AWT hierarchy) MapViewerEvent's
     * to know what to display & when to update.  It's
     * intended that there only be one MapPanner in the
     * application at a time.
     */
    public MapPanner()
    {
        setPreferredSize(new Dimension(150,100));
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        VUE.addActiveViewerListener(this);
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
        if (e.isActivationEvent() && mapViewer == null) {
            setViewer(e.getMapViewer());
        } else if (e.getSource() == this.mapViewer
            && (e.getID() == MapViewerEvent.PAN ||
                e.getID() == MapViewerEvent.ZOOM)) {
            repaint();
            if (e.getID() == MapViewerEvent.ZOOM) {
                putClientProperty("TITLE-INFO", ""+this.mapViewer.getZoomFactor());
            }
        }
    }

    public void activeViewerChanged(MapViewer viewer) {
        setViewer(viewer);
    }
    
    private void setViewer(MapViewer mapViewer)
    {
        if (DEBUG.FOCUS) out("setViewer " + mapViewer);
        if (this.mapViewer != mapViewer) {
            this.mapViewer = mapViewer;
            if (mapViewer != null)
                setMap(mapViewer.getMap());
            repaint();
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
        final Object key = e.getWhat();
        if (DEBUG.DYNAMIC_UPDATE || key == LWKey.UserActionCompleted || key == LWKey.RepaintAsync)
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
            int dx = x - lastDrag.x;
            int dy = y - lastDrag.y;
            double factor = mapViewer.getZoomFactor() / this.zoomFactor;

            dx = (int) (dx * factor + 0.5);
            dy = (int) (dy * factor + 0.5);
            
            if (DEBUG.SCROLL) out("dx="+dx + " dy="+dy);
            
            mapViewer.panScrollRegion(dx, dy, true);
            lastDrag = e.getPoint();
            
        } else {
            
            if (ViewerAlwaysVisible) {
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
        if (mapViewer != null)
            mapViewer.getMouseWheelListener().mouseWheelMoved(e);
    }
    
    public void paintComponent(Graphics g)
    {
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
        paintViewerIntoRectangle(this, g, this.mapViewer, pannerSize);
    }

    public static void paintViewerIntoRectangle(Graphics g, final MapViewer viewer, final Rectangle pannerSize) {
        paintViewerIntoRectangle(null, g, viewer, pannerSize);
    }
    
    static void paintViewerIntoRectangle(MapPanner panner, Graphics g, final MapViewer viewer, final Rectangle paintRect)
    {

        if (viewer.getVisibleWidth() < 1 || viewer.getVisibleHeight() < 1) {
            if (DEBUG.Enabled)
                System.out.println("MapPanner: paintViewerIntoRectangle: nothing to paint; visible size="
                                   + viewer.getVisibleSize()
                                   + " in " + viewer);
            return;
        }

        final LWMap map = viewer.getMap();
        
        final Rectangle2D allComponentBounds = map.getBounds();
        final Rectangle2D canvasRect = viewer.getCanvasMapBounds();
        final Rectangle2D viewerRect = viewer.getVisibleMapBounds();
        final Rectangle2D pannerRect;

        if (ViewerAlwaysVisible && viewer.inScrollPane()) {
            if (ShowFullCanvas)
                // the fudgey margins go away with show full canvas -- which indicates
                // the problem w/out the canvas is obviously because we can *drag* to
                // edge of full canvas, but if not computing zoom with it, we'll
                // get zoomed out when we go off edge of map bounds to edge of canvas bounds.
                pannerRect = canvasRect.createUnion(allComponentBounds);
            else
                pannerRect = viewerRect.createUnion(allComponentBounds);
        } else
            pannerRect = allComponentBounds;

        
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

        final DrawContext dc = new DrawContext(g, zoomFactor, -offset.x, -offset.y, null, false);

        dc.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, viewer.AA_ON);//pickup MapViewer AA state for debug
        dc.setPrioritizeSpeed(true);
        dc.setFractionalFontMetrics(false);
        dc.setDraftQuality(true); // okay to skimp in rendering of panner image -- it's usually so tiny
        dc.setMapDrawing();

        /*
         * Fill the background representing the currently active canvas region.
         * If the viewer is in a scroll-region, this will be the total area
         * it's scrolling over -- a large canvas.  If not, it will simply be
         * the visible viewer canvas, which virtually "pan's" over the infinite
         * coordinate space the map lies in.
         */
        
        // need to offset fill, so can't just use existing canvasRect
        final Rectangle2D canvas = viewer.screenToMapRect(new Rectangle(1,1, viewer.getWidth(), viewer.getHeight()));

        dc.g.setColor(map.getFillColor());
        // round size of canvas down...
        dc.g.fill(canvas);
        
        /*
         * Now tell the active LWMap to draw itself here on the panner.
         */
        
        map.draw(dc);
        
        /*
         * Show where the edge of the *visible* viewer region overlaps the map
         */
        
        dc.setAntiAlias(false);
        dc.setAbsoluteStroke(1);
        dc.g.setColor(Color.red);
        dc.g.draw(viewerRect);
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
