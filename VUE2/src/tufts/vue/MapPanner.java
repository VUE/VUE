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
               MapViewerListener,
               MouseListener,
               MouseMotionListener
{
    private MapViewer mapViewer;
    private double zoomFactor;
    private Point dragStart;
    
    public MapPanner(MapViewer mapViewer)
    {
        //setBorder(new javax.swing.border.TitledBorder("Panner"));
        setPreferredSize(new Dimension(100,100));
        setViewer(mapViewer);
        setBackground(SystemColor.control);
        
        //addMouseListener(this);
        //addMouseMotionListener(this);

        // VUE.addEventListener(this, MapViewerEvent.class);
    }

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
    
    public void setViewer(MapViewer mapViewer)
    {
        this.mapViewer = mapViewer;
    }
    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (mapViewer == null)
            return;

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
        g2.setColor(Color.white);
        g2.fill(mapViewerRect);
        mapViewer.getMap().draw(g2);
        g2.setColor(Color.red);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setStroke(STROKE_ONE);
        g2.draw(mapViewerRect);

        //g2.scale(1/zoomFactor, 1/zoomFactor);
        //g2.translate(offset.getX(), offset.getY());

        
    }

    public void mousePressed(MouseEvent e)
    {
        dragStart = e.getPoint();
        repaint();
    }
    public void mouseReleased(MouseEvent e)
    {
        dragStart = null;
        //System.err.println(e);
    }
    public void mouseDragged(MouseEvent e)
    {
        if (dragStart == null)
            return;
        
        //System.out.println(e);
        Point p = e.getPoint();

        // move the panner
        /*
            // moving the window
            p.x += this.getX();
            p.y += this.getY();
            // now we have the absolute screen location
            p.x -= dragStart.x;
            p.y -= dragStart.y;
            setLocation(p);

        //System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
        */
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseExited(MouseEvent e) { /*System.err.println(e);*/ }
    public void mouseMoved(MouseEvent e) {}
    
}
