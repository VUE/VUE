package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JViewport;

/**
 * MapViewport -- a viewport that handles a dynamically sized extent.
 *
 * Conceptually this would be better as a static inner class
 * in MapViewer.java, but it's here just to keep the code
 * better organized.
 *
 * The viewport code is complicated to deal with the fact that
 * we operate on an infinite canvas and need to guess
 * at something reasonable to do in a bunch of different cases,
 * and because JScrollPane's/JViewport weren't designed
 * to handle components that may grow up/left as opposed
 * to just down/right.
 *
 * In JViewport, The EXTENT is the physical, visible JPanel, through which
 * the contents of the VIEW (the MapViewer) are scrolled.  Here, we
 * call the view the CANVAS.
 *
 */

class MapViewport extends JViewport
    implements VueConstants
{
    private final MapViewer viewer;

    private Rectangle2D lastMapBounds = new Rectangle2D.Float();
    private Dimension lastCanvas = new Dimension();
    private Point2D lastMapLocationAtCanvasOrigin = new Point2D.Float();
    

    public MapViewport(MapViewer viewer) {
        setView(this.viewer = viewer);
    }

    private LWMap getMap() {
        return viewer.getMap();
    }
    
    /**
     * Configures the viewer to display the given map coordinate in the
     * 0,0 location of the panel.  Note that if we're in a scroll
     * region, this results in setting what displays in the 0,0 of the
     * extent -- not what's actually on screen, unelss user happens to
     * be scrolled all the way up and to the left.
     *
     * E.g. -- to have map location 10,10 display in the upper left
     * hand corner of the extent (panel location 0,0) we use
     * setMapOriginoffset to position the 0,0 map offset position
     * at 10,10, thus when we draw, location 10,10 will be at
     * 0,0. This method is here to compensate for the zoom factor:
     * E.g., at a zoom of 200%, we actually have to set the map offset
     * to 20,20, as each map coordinate unit now takes up two pixels.
     *
     */
    private void placeMapLocationAtCanvasOrigin(float mapX, float mapY) {
        viewer.setMapOriginOffset((float) (mapX * viewer.mZoomFactor),
                                  (float) (mapY * viewer.mZoomFactor),
                                  false);
    }
    
    private void placeMapLocationAtCanvasOrigin(Point2D.Float p) {
        placeMapLocationAtCanvasOrigin(p.x, p.y);
    }
    
    private Point2D.Float getMapLocationAtCanvasOrigin() {
        return new Point2D.Float
            ((float) (viewer.mOffset.x * viewer.mZoomInverse),
             (float) (viewer.mOffset.y * viewer.mZoomInverse));
    }


    /** width of the "view" region we're scrolling over in a scroll pane. */
    private int getCanvasWidth() {
        return viewer.getPreferredSize().width;
        //return viewer.getWidth();
    }
    /** height of the extent region we're scrolling over in a scroll pane */
    private int getCanvasHeight() {
        return viewer.getPreferredSize().height;
        //return viewer.getHeight();
    }

    /** equivalent to JViewport.getViewSize() */
    private Dimension getCanvasSize() {
        return viewer.getPreferredSize();
        //return viewer.getSize();
    }

    private void setCanvasSize(Dimension d) {
        setViewSize(d);
        viewer.setPreferredSize(d);
        //viewer.setSize(d);
    }

    /** override of JViewport */
    public Dimension getViewSize() {
        return getCanvasSize();
    }
    /** override of JViewport */
    public void setViewPosition(Point p) {
        if (DEBUG.SCROLL) out("setViewPosition " + out(p));
        super.setViewPosition(p);
    }

    public void placeMapLocationAtViewCenter(Point2D.Float mapAnchor)
    {
        adjustSize(true, false, false, false);
        Point canvasAnchor = viewer.mapToScreenPoint(mapAnchor);
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS ANCHOR: " + out(canvasAnchor));
        Point canvasOffset = new Point(canvasAnchor);
        canvasOffset.x -= getWidth() / 2;
        canvasOffset.y -= getHeight() / 2;
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS OFFSET: " + out(canvasOffset));
        
        setVisibleCanvasCorner(canvasOffset);
        //setCanvasPosition(canvasOffset);
        
        //mViewport.setViewPosition(canvasOffset);
        //setLocation(-canvasOffset.x, -canvasOffset.y);
        //adjustCanvas(false, false);
    }
    
    public void setVisibleCanvasCorner(Point p) {
        setCanvasPosition(new Point(-p.x, -p.y));
    }
    

    /**
     * adjustSize -- adjust the size of the MapViewer canvas - the viewport view
     *
     * Called after changes to map bounds (drag or resize operations).
     *
     * Adjust the size of the canvas -- the size of the region being scrolled over.  This
     * changes as the size of the map bounds changes, and as we zoom in and out.  E.g., zooming
     * in from 100% to 200% will generally double the size of the canvs.. Zooming to less than
     * 100% will generally set the canvas to the same size as the viewport.  The canvas will never
     * be less than the size of the viewport.  What happens exactly on each adjustment depends
     * on where the user is currently panned to -- e.g., on zooms, we want to center on the
     * viewport, which means that besides resizing the canvas for the soom factor, if we're
     * zooming in on, say, the upper left of the canvas, and the soom would result in the upper
     * left of the canvas now being in the middle of the viewport, we have to grow the canvas
     * and reset the offset so that the upper left of the canvas is in the upper left of the
     * viewport so the focused region is in the actual center of the screen.  This is because
     * the canvas (the MapViewer JComponent), can never have a positive location -- it can never
     * be > 0,0, although if we scroll over it, it can take on negative location values as we scroll.
     *
     * @param expand -- automatically expand the canvas to cover the current map origin offset (mOffset) & viewport size
     * @param trimNorthWest -- trim north west corner of canvas to map bounds, and place map at upper left of display
     * @param trimSouthEst -- trim south west corner of canvas to the current on-screen viewport display size
     *
     * todo: Expand is incompatable with the trims -- reorganize arguments.
     */
    
    void adjustSize() {
        adjustSize(false, true, true, true);
    }
    void adjustSize(boolean expand, boolean trimNorthWest, boolean trimSouthEast) {
        adjustSize(false, true, true, true);
    }

    private void adjustSize(boolean expand, boolean trimNorthWest, boolean trimSouthEast, boolean validate)
    {
        if (DEBUG.SCROLL && DEBUG.META) new Throwable("adjustSize").printStackTrace();
        
        //------------------------------------------------------------------
        // Compute the canvas, which is going to be the new total size
        // of the region we're going to have available to scroll over.
        // We always include the bounds of every object, as well as
        // the current map origin -- so grows up & to the left are
        // "permanent" until a an adjustExtend with both trims sis called (currently
        // only via ZoomFit).
        //------------------------------------------------------------------
        
        Rectangle2D mapBounds = getMap().getBounds();
        if (DEBUG.SCROLL) out("---MAP BOUNDS: " + out(mapBounds)
                              + " expand="+expand
                              + " trimNorthWest="+trimNorthWest
                              + " trimSouthEast="+trimSouthEast
                              + " validate="+validate
                              );
        if (DEBUG.SCROLL) out("view position: " + out(getViewPosition()));

        // compute the size of the minumum canvas that can contain everything in the map
        Rectangle2D.Float mapCanvas = viewer.getContentBounds();
        if (DEBUG.SCROLL) out("   map canvas: " + out(mapCanvas));
        
        Point2D.Float mapLocationAtCanvasOrigin = getMapLocationAtCanvasOrigin();
        
        if (trimNorthWest) {
            
            // If we're collapsing, compress the canvas by moving the
            // origin to the upper left hand corner of all the
            // component bounds.  We "trim" the canvas of usused map
            // "whitespace" when we trimNorthWest.
            
            if (DEBUG.SCROLL) out("   old origin: " + out(viewer.mOffset));
            placeMapLocationAtCanvasOrigin(mapCanvas.x, mapCanvas.y);
            if (DEBUG.SCROLL) out(" reset origin: " + out(viewer.mOffset));
        } else {
            
            // add the current origin, otherwise everything would
            // always be jamming itself up against the upper left hand
            // corner.  This has no effect unless they've moved the
            // component with the smallest x/y (the farthest to the upper
            // left).
            
            if (DEBUG.SCROLL) out("   add offset: " + out(viewer.mOffset));
            if (DEBUG.SCROLL) out("   is map loc: " + out(mapLocationAtCanvasOrigin));
            mapCanvas.add(mapLocationAtCanvasOrigin);
            if (DEBUG.SCROLL) out("  +plusOrigin: " + out(mapCanvas));
        }

        // If canvas
        //if (expand) {
        
        //Point vPos = viewport.getViewPosition();
        /*
        if (panning) {
            Point vPos = viewport.getViewPosition();
            System.out.println("SCROLL: vp="+vPos);
            //canvas.add(vPos.x, vPos.y);
            //System.out.println(getMap().getLabel() + "plusViewerPos: " + canvas);
            canvas.add(vPos.x + viewport.getWidth(),
                       vPos.y + viewport.getHeight());
            System.out.println(getMap().getLabel() + "   plusCorner: " + canvas);
        }
         */
        
        
        // okay to call this mapToScreen while adjusting origin as we're
        // only interested in the zoom conversion for the size.
        Dimension canvas = viewer.mapToScreenDim(mapCanvas);
        if (DEBUG.SCROLL) out(" pixel canvas: " + out(canvas));
        //Rectangle vb = mapToScreenRect(mapCanvas);

        //Dimension curSize = viewer.getPreferredSize(); // get CURRENT size of the canvas
        Dimension curSize = getSize(); // current view size
        //int newWidth = curSize.width;
        //int newHeight = curSize.height;
        int newWidth = canvas.width;
        int newHeight = canvas.height;
        
        //if (!trimNorthWest && lastCanvas.equals(canvas) && lastMapLocationAtCanvasOrigin.equals(mapLocationAtCanvasOrigin))
        //    return;
        
        lastCanvas = canvas;
        lastMapLocationAtCanvasOrigin = mapLocationAtCanvasOrigin;
        /*
        if (canvas.width > newWidth)
            newWidth = canvas.width;
        if (canvas.height > newHeight)
            newHeight = canvas.height;
        */
        Dimension newSize = new Dimension(newWidth, newHeight);
        
        
        //------------------------------------------------------------------
        // If canvas is outside the the current map origin (that is,
        // something's been dragged off the left or top of the screen),
        // reset the origin to include the region where the components
        // were moved to.
        //------------------------------------------------------------------
        
        if (!trimNorthWest) {
            boolean originGrew = false;
            // mOffset is what?
            //float ox = mOffset.x;
            //float oy = mOffset.y;
            float ox = mapLocationAtCanvasOrigin.x;
            float oy = mapLocationAtCanvasOrigin.y;
            if (mapCanvas.x < mapLocationAtCanvasOrigin.x) {
                ox = mapCanvas.x;
                originGrew = true;
            }
            if (mapCanvas.y < mapLocationAtCanvasOrigin.y) {
                oy = mapCanvas.y;
                originGrew = true;
            }
            if (originGrew)
                placeMapLocationAtCanvasOrigin(ox, oy);
        }
        
        //if (curSize.equals(newSize))
        //return;

        if (!trimSouthEast) {
            // don't let new size be less than current size
            if (newSize.width < curSize.width)
                newSize.width = curSize.width;
            if (newSize.height < curSize.height)
                newSize.height = curSize.height;
        }
        
        if (DEBUG.SCROLL) {
            out(" cur ext size: " + out(curSize));
            out(" new ext size: " + out(newSize));
            out("  canvas size: " + out(getCanvasSize()));
            out("  actual size: " + out(viewer.getSize()));
            out("   vport size: " + out(getSize()));
        }
        
        setCanvasSize(newSize);
        if (true||validate) { // until call validate, calls to setLocation are triggering reshape with old size! (ok, we call setViewSize manually also now)
            if (DEBUG.SCROLL) out("calling revalidate");
            revalidate();
        }
    }

    void pan(int dx, int dy, boolean allowGrowth)
    {
        //Point location = viewport.getViewPosition();
        Point location = viewer.getLocation(); // both x/y should always be <= 0
        if (DEBUG.SCROLL) out("PAN: dx=" + dx + ", dy=" + dy + " allowGrowth="+allowGrowth);
        if (DEBUG.SCROLL) out("PAN: viewport start: " + out(location));
        location.translate(-dx, -dy);
        if (DEBUG.SCROLL) out("PAN: viewport   end: " + out(location));

        if (!allowGrowth) {
            // If drag would take us beyond width or height of existing canvas,
            // clip to existing canvas.
            if (location.x + getWidth() > getCanvasWidth())
                location.x = getCanvasWidth() - getWidth();
            if (location.y + getHeight() > getCanvasHeight())
                location.y = getCanvasHeight() - getHeight();
        }
        
        if (DEBUG.SCROLL) {
            out("PAN: setViewPosition " + out(location));
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }
        
        setCanvasPosition(location, allowGrowth);
        viewer.fireViewerEvent(MapViewerEvent.PAN);
        
        /*
        if (false) {
            Rectangle2D.Float canvas = viewer.getContentBounds();
            //Point vPos = viewport.getViewPosition();
            Point vPos = location;
            Rectangle2D.union(canvas, viewer.getVisibleMapBounds(), canvas);
            if (DEBUG.SCROLL) System.out.println(getMap().getLabel() + "   plusVISMAP: " + canvas);
            //canvas.add(mOffset);
            //System.out.println(getMap().getLabel() + "   plusOrigin: " + canvas);
            //System.out.println("SCROLL: vp="+vPos);
            // NOTE: Canvas is current a bunch of map coords...
            //canvas.add(vPos.x, vPos.y);
            //System.out.println(getMap().getLabel() + "plusViewerPos: " + canvas);
            //canvas.add(vPos.x + viewport.getWidth(), vPos.y + viewport.getHeight());
            //System.out.println(getMap().getLabel() + "   plusCorner: " + canvas);
              Dimension curSize = getSize();
              int newWidth = curSize.width;
              int newHeight = curSize.height;
              Rectangle canvasSize = mapToScreenRect(canvas);
              if (canvasSize.width > newWidth)
              newWidth = canvasSize.width;
              if (canvasSize.height > newHeight)
              newHeight = canvasSize.height;
              Dimension newSize = new Dimension(newWidth, newHeight);
              System.out.println("PAN: size to " + newSize);
              setPreferredSize(newSize);
            viewer.setPreferredSize(viewer.mapToScreenDim(canvas));
            revalidate();
        }
        */
    }
    
    
    void setCanvasPosition(Point p) {
        setCanvasPosition(p, true);
    }
    
    void setCanvasPosition(Point p, boolean allowGrowth) {
        if (DEBUG.SCROLL) {
            out("setCanvasPosition " + out(p));
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }

        Dimension canvas = getCanvasSize();
        Dimension view = getSize();

        if (DEBUG.SCROLL) {
            out("setCanvasPosition canvas: " + out(canvas));
            out("setCanvasPosition   view: " + out(view));
        }
        
        /*
        if (p.x > 0 || p.y > 0) {
            out("GROW ORIGIN");
            //placeMapLocationAtCanvasOrigin(mapCanvas.x, mapCanvas.y);
            //viewport.setViewPosition(p);
            //panScrollRegion(-p.x, -p.y, true);
        }

        */

        boolean grew = false;
        boolean moved = false;
        float ox = viewer.mOffset.x;
        float oy = viewer.mOffset.y;

        if (p.x > 0) {
            int grow = p.x;
            if (DEBUG.SCROLL) out("GROW LEFT " + grow);
            p.x = 0;
            ox -= grow;
            moved = true;
            canvas.width += grow;
            grew = true;
        }
        if (p.y > 0) {
            int grow = p.y;
            if (DEBUG.SCROLL) out("GROW UP " + grow);
            p.y = 0;
            oy -= grow;
            moved = true;
            canvas.height += grow;
            grew = true;
        }
        if (canvas.width + p.x < view.width) {
            int grow = view.width - (canvas.width + p.x);
            if (DEBUG.SCROLL) out("GROW RIGHT " + grow);
            canvas.width += grow;
            grew = true;
        }
        if (canvas.height + p.y < view.height) {
            int grow = view.height - (canvas.height + p.y);
            if (DEBUG.SCROLL) out("GROW DOWN " + grow);
            canvas.height += grow;
            grew = true;
        }

        if (allowGrowth) {
            if (grew)
                setCanvasSize(canvas);
            if (moved)
                viewer.setMapOriginOffset(ox, oy);
        }
            
        p.x = -p.x;
        p.y = -p.y;
        setViewPosition(p);
        
        if (DEBUG.SCROLL) {
            out("setCanvasPosition, setViewPosition completed");
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }
    }
    
    /*
        if (location.x < 0) {
            if (allowGrowth) {
                if (DEBUG.SCROLL) out("PAN: GROW X " + location.x);
                ox += location.x;
                originMoved = true;
                location.x = 0;
            } else {
                // if drag would take us to left of existing canvas, clip
                location.x = 0;
            }
        }
        if (location.y < 0) {
            if (allowGrowth) {
                if (DEBUG.SCROLL) out("PAN: GROW Y " + location.y);
                oy += location.y;
                originMoved = true;
                location.y = 0;
            } else {
                // if drag would take us above existing canvas, clip
                location.y = 0;
            }
        }
        if (originMoved) {
            // not working -- adjustCanvas should
            // handle setPreferredSize?
            //setMapOriginOffset(ox, oy);
            Dimension s = getPreferredSize();
            s.width += dx;
            s.height += dy;
            setCanvasSize(s);
        }
        out("setCanvasPosition, ASR:");
        try { Thread.sleep(1000); } catch (Exception e) {}
        adjustCanvas(false, false);
    */
    /*
    public void paint(Graphics g) {
        super.paint(g);
        out("paint");
        Point center = new Point(getWidth() / 2, getHeight() / 2);
        g.setColor(Color.blue);
        g.drawLine(-99999, center.y, 99999, center.y);
        g.drawLine(center.x, -99999, center.x, 99999);
    }
    public Dimension getViewSize() {
        //new Throwable("getViewSize").printStackTrace();
        return viewer.mapToScreenDim(viewer.getContentBounds());
    }
    void update() {
        fireStateChanged();
    }
    */

    public String toString() {
        return "MapViewport";
    }

    private void out(Object o) {
        System.out.println(this + " " + (o==null?"null":o.toString()));
    }

    private String out(Point2D p) { return (float)p.getX() + ", " + (float)p.getY(); }
    private String out(Rectangle2D r) { return ""
            + (float)r.getX() + ", " + (float)r.getY()
            + "  "
            + (float)r.getWidth() + " x " + (float)r.getHeight()
            ;
    }
    private String out(Dimension d) { return d.width + " x " + d.height; }
    
}



