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
    private MapViewer viewer;
    private double zoomFactor;
    private Point dragStart;
    
    public MapPanner(MapViewer viewer)
    {
        //setBorder(new javax.swing.border.TitledBorder("Panner"));
        setPreferredSize(new Dimension(100,100));
        setActiveViewer(viewer);
        setBackground(Color.white);
        // addMouseListener(this);
        // addMouseMotionListener(this);
    }

    public void mapViewerEventRaised(MapViewerEvent e)
    {
        if (e.getID() == MapViewerEvent.DISPLAYED
            || e.getID() == MapViewerEvent.PANZOOM) {
            setActiveViewer(e.getMapViewer());
            repaint();
        }
    }
    
    public void setActiveViewer(MapViewer viewer)
    {
        this.viewer = viewer;
    }
    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (viewer == null || viewer.isEmpty())
            return;
        
        //g.drawRect(0,0, getWidth()-1, getHeight()-1);
        Graphics2D g2 = (Graphics2D) g;
        Point2D offset = new Point2D.Double();
        zoomFactor = ZoomTool.computeZoomFit(getSize(),
                                             0, // todo: allow for gap w/out offsettig viewport
                                             viewer.getAllComponentBounds(),
                                             offset);

        Rectangle2D viewerRect = viewer.screenToMapRect(viewer.getBounds());
        
        g2.translate(-offset.getX(), -offset.getY());
        g2.scale(zoomFactor, zoomFactor);
        viewer.paintLWComponents(g2);
        g2.setColor(Color.red);
        g2.setStroke(STROKE_ONE);
        g2.draw(viewerRect);

        //g2.scale(1/zoomFactor, 1/zoomFactor);
        //g2.translate(offset.getX(), offset.getY());

        
    }

    public void mousePressed(MouseEvent e)
    {
        dragStart = e.getPoint();
    }
    public void mouseReleased(MouseEvent e)
    {
        dragStart = null;
        //System.err.println(e);
    }
    public void mouseDragged(MouseEvent e)
    {
        //System.out.println(e);
        Point p = e.getPoint();

        if (dragStart == null) {
            System.out.println("mouseDragged with no dragStart!");
            return;
        }
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
