package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JViewport;
import javax.swing.border.*;

/**
 * MapPanner.java
 *
 * Implements a panel for displaying a map overview, including
 * the currently visible viewport, and moving (panning) the currently
 * visible viewport.
 *
 * @author Scott Fraize
 * @version 3/27/03
 */
public class MapPanner extends javax.swing.JPanel
    implements VueConstants,
               MapViewer.Listener,
               VUE.ActiveViewerListener,
               LWComponent.Listener,
               MouseListener,
               MouseMotionListener
{
    private MapViewer mapViewer; // active MapViewer
    private double zoomFactor; // zoomFactor that will fit entire map in the panner
    private Point dragStart; // where mouse was at mouse press
    private Point lastDrag; // where mouse was at last drag
    private Point2D mapStart; // where map origin was at mouse press
    private LWMap map; // active map

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

        VUE.addActiveViewerListener(this);
    }

    
    /**
     * All instances of MapViewer raise MapViewer events
     * as the act, and the MapPanner hears all of them
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
            this.map.addLWCListener(this, LWKey.UserActionCompleted);
        }
    }

    public void LWCChanged(LWCEvent e) {
        // we only see UserActionCompleted events
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
        //mapViewer.adjustScrollRegion();// for now
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
            if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
                lastDrag = e.getPoint();
                return;
            }
            int dx = x - lastDrag.x;
            int dy = y - lastDrag.y;
            double factor = mapViewer.getZoomFactor() / this.zoomFactor;

            dx = (int) (dx * factor + 0.5);
            dy = (int) (dy * factor + 0.5);
            
            if (DEBUG.SCROLL) out("dx="+dx + " dy="+dy);
            
            mapViewer.panScrollRegion(dx, dy);
            lastDrag = e.getPoint();
            
        } else {
            // hack till we disallow the maprect from going beyond edge
            if (x < 0) x = 0;
            else if (x > getWidth()-2) x = getWidth()-2;
            if (y < 0) y = 0;
            else if (y > getHeight()-2) y = getHeight()-2;
        
            double factor = this.zoomFactor / mapViewer.getZoomFactor();
            double dragOffsetX = (x - dragStart.getX()) / factor;
            double dragOffsetY = (y - dragStart.getY()) / factor;
            mapViewer.setMapOriginOffset(mapStart.getX() + dragOffsetX,
                                         mapStart.getY() + dragOffsetY);
            mapViewer.repaint();
            repaint();
        }

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

        Rectangle2D allComponentBounds = mapViewer.getAllComponentBounds();
        Rectangle   viewerBounds = new Rectangle(mapViewer.getVisibleWidth()-1, mapViewer.getVisibleHeight()-1);

        if (viewerBounds.isEmpty()) {
            out("nothing to paint");
            return;
        }
        
        //Rectangle2D.Float mapViewerRect = (Rectangle2D.Float) mapViewer.screenToMapRect(viewerBounds);
        Rectangle2D mapViewerRect = mapViewer.getVisibleMapBounds();
        /*
        if (mapViewer.getParent() instanceof JViewport) {
            JViewport vp = (JViewport) mapViewer.getParent();
            Point vpos = vp.getViewPosition();
            mapViewerRect.x = vpos.x;
            mapViewerRect.y = vpos.y;
        }
        */
        Rectangle2D pannerRect = mapViewerRect.createUnion(allComponentBounds);

        Dimension pannerViewportSize = getSize();
        pannerViewportSize.width -= 1;
        pannerViewportSize.height -= 1;
        
        Graphics2D g2 = (Graphics2D) g;
        Point2D offset = new Point2D.Double();
        
        zoomFactor = ZoomTool.computeZoomFit(pannerViewportSize,
                                             0,
                                             pannerRect,
                                             offset);
                                            

        g2.translate(-offset.getX(), -offset.getY());
        g2.scale(zoomFactor, zoomFactor);
        //g2.setColor(Color.white);
        g2.setColor(mapViewer.getBackground());
        g2.fill(mapViewerRect);

        /*
         * Construct a DrawContext to use in painting the entire
         * map on the panner window.
         */

        DrawContext dc = new DrawContext(g2, zoomFactor);
        //dc.setAntiAlias(true);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, mapViewer.AA_ON);//pickup MapViewer AA state for debug
        dc.setPrioritizeSpeed(true);
        dc.setFractionalFontMetrics(false);
        dc.setPrinting(true); // what we want on panner draw same as printing -- really a "non-interactive" flag
        dc.setDraftQuality(true); // okay to skimp in rendering of panner image -- it's usually so tiny

        /*
         * Now tell the active LWMap to draw itself here on the panner.
         */
        mapViewer.getMap().draw(dc);
        
        g2.setColor(Color.red);
        // todo: de-scale us before drawing -- actually -- do on a glass pane as we're
        // very expensively rederawing the whole map here...
        if (!VueUtil.isMacPlatform()) {
            dc.setAntiAlias(false);
            //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setStroke(STROKE_ONE);
        } else {
            g2.setStroke(new BasicStroke((float)(1/this.zoomFactor)));
        }
        g2.draw(mapViewerRect);

        //System.out.println(pannerRect);
        //System.out.println(mapViewerRect);

        g2.scale(1/zoomFactor, 1/zoomFactor);
        g2.translate(offset.getX(), offset.getY());
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
