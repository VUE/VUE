package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
               MouseListener,
               MouseMotionListener
{
    private MapViewer mapViewer; // active MapViewer
    private double zoomFactor; // zoomFactor that will fit entire map in the panner
    private Point dragStart; // where mouse was at mouse press
    private Point2D mapStart; // where map origin was at mouse press

    /**
     * Get's global (thru AWT hierarchy) MapViewerEvent's
     * to know what to display & when to update.  It's
     * intended that there only be one MapPanner in the
     * application at a time.
     */
    public MapPanner()
    {
        //setBorder(new javax.swing.border.TitledBorder("Panner"));
        setPreferredSize(new Dimension(100,100));
        setBackground(SystemColor.control);
        
        addMouseListener(this);
        addMouseMotionListener(this);

        // VUE.addEventListener(this, MapViewerEvent.class);
    }

    /**
     * All instances of MapViewer raise MapViewer events
     * as the act, and the MapPanner hears all of them
     * here.
     */
    public void mapViewerEventRaised(MapViewerEvent e)
    {
        if (e.getID() == MapViewerEvent.DISPLAYED) {
            setViewer(e.getMapViewer());
            repaint();
        } else if (e.getSource() == this.mapViewer
                   && (e.getID() == MapViewerEvent.PAN ||
                       e.getID() == MapViewerEvent.ZOOM)) {
            repaint();
        }
    }
    
    private void setViewer(MapViewer mapViewer)
    {
        this.mapViewer = mapViewer;
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
        Rectangle   viewerBounds = new Rectangle(mapViewer.getWidth()-1, mapViewer.getHeight()-1);

        if (viewerBounds.isEmpty())
            return;
        
        Rectangle2D mapViewerRect = mapViewer.screenToMapRect(viewerBounds);
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

        //g2.scale(1/zoomFactor, 1/zoomFactor);
        //g2.translate(offset.getX(), offset.getY());

        
    }

    protected void paintBorder(Graphics g) {
        // panner disables any border
    }

    public void mousePressed(MouseEvent e)
    {
        dragStart = e.getPoint();
        mapStart = mapViewer.getOriginLocation();
        repaint();
    }
    public void mouseReleased(MouseEvent e)
    {
        dragStart = null;
    }

    public void mouseDragged(MouseEvent e)
    {
        if (dragStart == null)
            return;

        int x = e.getX();
        int y = e.getY();

        // hack till we disallow the maprect from going beyond edge
        if (x < 0) x = 0;
        else if (x > getWidth()-2) x = getWidth()-2;
        if (y < 0) y = 0;
        else if (y > getHeight()-2) y = getHeight()-2;
        
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
        
        double factor = this.zoomFactor / mapViewer.getZoomFactor();
        double dragOffsetX = (x - dragStart.getX()) / factor;
        double dragOffsetY = (y - dragStart.getY()) / factor;

        /*
         * Reposition the active MapViewer -- this will generate an
         * event that we'll get, a which point the panner will
         * then repaint itself.  (We then tell the mapviewer
         */
        mapViewer.setMapOriginOffset(mapStart.getX() + dragOffsetX,
                                     mapStart.getY() + dragOffsetY);
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseExited(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseMoved(MouseEvent e) {}
    
}
