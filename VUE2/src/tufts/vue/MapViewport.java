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
    private final JViewport viewport; // for now: this

    private Rectangle2D lastMapBounds = new Rectangle2D.Float();
    private Dimension lastExtent = new Dimension();
    private Point2D lastMapLocationAtCanvasOrigin = new Point2D.Float();
    

    public MapViewport(MapViewer viewer) {
        setView(this.viewer = viewer);
        this.viewport = this;
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
        return viewer.getWidth();
    }
    /** height of the extent region we're scrolling over in a scroll pane */
    private int getCanvasHeight() {
        return viewer.getHeight();
    }

    /** equivalent to JViewport.getViewSize() */
    private Dimension getCanvasSize() {
        return viewer.getSize();
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
     * Adjust the size of the "extent" -- the size of the region being scrolled over.  This
     * changes as the size of the map bounds changes, and as we zoom in and out.  E.g., zooming
     * in from 100% to 200% will generally double the size the extent. Zooming to less than
     * 100% will generally set the extent to the same size as the viewport.  The extent will never
     * be less than the size of the viewport.  What happens exactly on each adjustment depends
     * on where the user is currently panned to -- e.g., on zooms, we want to center on the
     * viewport, which means that besides resizing the extent for the soom factor, if we're
     * zooming in on, say, the upper left of the extent, and the soom would result in the upper
     * left of the extent now being in the middle of the viewport, we have to grow the extent
     * and reset the offset so that the upper left of the extent is in the upper left of the
     * viewport so the focused region is in the actual center of the screen.  This is because
     * the extent (the MapViewer JComponent), can never have a positive location -- it can never
     * be > 0,0, although if we scroll over it, it can take on negative location values as we scroll.
     *
     * @param expand -- automatically expand the extent to cover the current map origin offset (mOffset) & viewport size
     * @param trimNorthWest -- trim north west corner of extent to map bounds, and place map at upper left of display
     * @param trimSouthEst -- trim south west corner of extent to the current on-screen viewport display size
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
        if (DEBUG.SCROLL && DEBUG.META)
            new Throwable("ADJUST-SCROLL-REGION").printStackTrace();
        
        //------------------------------------------------------------------
        // Compute the extent, which is going to be the new total size
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
        if (DEBUG.SCROLL) out("view position: " + out(viewport.getViewPosition()));

        // compute the size of the minumum extent that can contain everything in the map
        Rectangle2D.Float mapExtent = viewer.getContentBounds();
        if (DEBUG.SCROLL) out("   map extent: " + out(mapExtent));
        
        Point2D.Float mapLocationAtCanvasOrigin = getMapLocationAtCanvasOrigin();
        
        if (trimNorthWest) {
            
            // If we're collapsing, compress the extent by moving the
            // origin to the upper left hand corner of all the
            // component bounds.  We "trim" the extent of usused map
            // "whitespace" when we trimNorthWest.
            
            if (DEBUG.SCROLL) out("   old origin: " + out(viewer.mOffset));
            placeMapLocationAtCanvasOrigin(mapExtent.x, mapExtent.y);
            if (DEBUG.SCROLL) out(" reset origin: " + out(viewer.mOffset));
        } else {
            
            // add the current origin, otherwise everything would
            // always be jamming itself up against the upper left hand
            // corner.  This has no effect unless they've moved the
            // component with the smallest x/y (the farthest to the upper
            // left).
            
            if (DEBUG.SCROLL) out("   add offset: " + out(viewer.mOffset));
            if (DEBUG.SCROLL) out("   is map loc: " + out(mapLocationAtCanvasOrigin));
            mapExtent.add(mapLocationAtCanvasOrigin);
            if (DEBUG.SCROLL) out("  +plusOrigin: " + out(mapExtent));
        }

        // If extent
        //if (expand) {
        
        //Point vPos = viewport.getViewPosition();
        /*
        if (panning) {
            Point vPos = viewport.getViewPosition();
            System.out.println("SCROLL: vp="+vPos);
            //extent.add(vPos.x, vPos.y);
            //System.out.println(getMap().getLabel() + "plusViewerPos: " + extent);
            extent.add(vPos.x + viewport.getWidth(),
                       vPos.y + viewport.getHeight());
            System.out.println(getMap().getLabel() + "   plusCorner: " + extent);
        }
         */
        
        
        // okay to call this mapToScreen while adjusting origin as we're
        // only interested in the zoom conversion for the size.
        Dimension extent = viewer.mapToScreenDim(mapExtent);
        if (DEBUG.SCROLL) out(" pixel extent: " + out(extent));
        //Rectangle vb = mapToScreenRect(mapExtent);

        Dimension curSize = viewer.getPreferredSize(); // get CURRENT size of the extent
        //int newWidth = curSize.width;
        //int newHeight = curSize.height;
        int newWidth = extent.width;
        int newHeight = extent.height;
        
        //if (!trimNorthWest && lastExtent.equals(extent) && lastMapLocationAtCanvasOrigin.equals(mapLocationAtCanvasOrigin))
        //    return;
        
        lastExtent = extent;
        lastMapLocationAtCanvasOrigin = mapLocationAtCanvasOrigin;
        /*
        if (extent.width > newWidth)
            newWidth = extent.width;
        if (extent.height > newHeight)
            newHeight = extent.height;
        */
        Dimension newSize = new Dimension(newWidth, newHeight);
        
        
        //------------------------------------------------------------------
        // If extent is outside the the current map origin (that is,
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
            if (mapExtent.x < mapLocationAtCanvasOrigin.x) {
                ox = mapExtent.x;
                originGrew = true;
            }
            if (mapExtent.y < mapLocationAtCanvasOrigin.y) {
                oy = mapExtent.y;
                originGrew = true;
            }
            if (originGrew)
                placeMapLocationAtCanvasOrigin(ox, oy);
        }
        
        //viewport.setViewSize(d);
        // extent.x is what we want to normalize to 0,
        // or the current position on screen
        /*
          if (extent.x < getX()) {
          System.out.println("Moving viewport back from " + getX() + " to " + extent.x);
          viewport.setViewPosition(new Point(-extent.x, getY()));
          int dx = getX() - extent.x;
          setMapOriginOffset(mOffset.x+dx, mOffset.y);
          }
         */
        //if (trimNorthWest)
        //if (DEBUG.SCROLL) out(" setting size: " + out(newSize));
        //setSize(newSize); // does this tract preferred size at all?  -- is called thru the revalidate.

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
            out("  actual size: " + out(getSize()));
            out("   vport size: " + out(viewport.getSize()));
        }
        
        viewer.setPreferredSize(newSize);
        if (validate) {
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
        location.translate(dx, dy);
        if (DEBUG.SCROLL) out("PAN: viewport   end: " + out(location));

        if (!allowGrowth) {
            // If drag would take us beyond width or height of existing extent,
            // clip to existing extent.
            if (location.x + viewport.getWidth() > getCanvasWidth())
                location.x = getCanvasWidth() - viewport.getWidth();
            if (location.y + viewport.getHeight() > getCanvasHeight())
                location.y = getCanvasHeight() - viewport.getHeight();
        }
        
        if (DEBUG.SCROLL) {
            out("PAN: setViewPosition " + out(location));
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }
        
        // Okay -- before we set the view position, increase the size if need be

        setCanvasPosition(location);
        viewer.fireViewerEvent(MapViewerEvent.PAN);
        
        //viewport.setViewPosition(location);
        
        /*
        if (DEBUG.SCROLL) {
            out("PAN: adjustExtent");
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }
        //adjustExtent(true, false, false);
        if (false) {
            Rectangle2D.Float extent = viewer.getContentBounds();
            
            //Point vPos = viewport.getViewPosition();
            Point vPos = location;
            
            Rectangle2D.union(extent, viewer.getVisibleMapBounds(), extent);
            if (DEBUG.SCROLL) System.out.println(getMap().getLabel() + "   plusVISMAP: " + extent);
            
            //extent.add(mOffset);
            //System.out.println(getMap().getLabel() + "   plusOrigin: " + extent);
            //System.out.println("SCROLL: vp="+vPos);
            // NOTE: Extent is current a bunch of map coords...
            //extent.add(vPos.x, vPos.y);
            //System.out.println(getMap().getLabel() + "plusViewerPos: " + extent);
            //extent.add(vPos.x + viewport.getWidth(), vPos.y + viewport.getHeight());
            //System.out.println(getMap().getLabel() + "   plusCorner: " + extent);
            
              Dimension curSize = getSize();
              int newWidth = curSize.width;
              int newHeight = curSize.height;
              
              Rectangle canvasSize = mapToScreenRect(extent);
              
              if (canvasSize.width > newWidth)
              newWidth = canvasSize.width;
              if (canvasSize.height > newHeight)
              newHeight = canvasSize.height;
              Dimension newSize = new Dimension(newWidth, newHeight);
              System.out.println("PAN: size to " + newSize);
              setPreferredSize(newSize);
            
            viewer.setPreferredSize(viewer.mapToScreenDim(extent));
            revalidate();
        }
        */
        
    }
    
    
    void setCanvasPosition(Point p) {
        if (DEBUG.SCROLL) {
            out("setCanvasPosition " + out(p));
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }

        Dimension extent = getViewSize();
        Dimension view = viewport.getSize();

        if (DEBUG.SCROLL) {
            out("setCanvasPosition extent: " + out(extent));
            out("setCanvasPosition   view: " + out(view));
        }
        
        boolean grew = false;

        /*
        if (p.x > 0 || p.y > 0) {
            out("GROW ORIGIN");
            //placeMapLocationAtCanvasOrigin(mapExtent.x, mapExtent.y);
            //viewport.setViewPosition(p);
            //panScrollRegion(-p.x, -p.y, true);
        }
        float ox = mOffset.x;
        float oy = mOffset.y;
        boolean originMoved = false;
        if (location.x < 0) {
            if (allowGrowth) {
                if (DEBUG.SCROLL) out("PAN: GROW X " + location.x);
                ox += location.x;
                originMoved = true;
                location.x = 0;
            } else {
                // if drag would take us to left of existing extent, clip
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
                // if drag would take us above existing extent, clip
                location.y = 0;
            }
        }
        if (originMoved) {
            // not working -- adjustExtent should
            // handle setPreferredSize?
            //setMapOriginOffset(ox, oy);
            Dimension s = getPreferredSize();
            s.width += dx;
            s.height += dy;
            setExtentSize(s);
        }

        if (p.x > 0) {
            int grow = p.x;
            p.x = 0;
        }
        */
            

        if (extent.width + p.x < view.width) {
            int grow = view.width - (extent.width + p.x);
            if (DEBUG.SCROLL) out("GROW RIGHT " + grow);
            extent.width += grow;
            grew = true;
        }
        if (extent.height + p.y < view.height) {
            int grow = view.height - (extent.height + p.y);
            if (DEBUG.SCROLL) out("GROW DOWN " + grow);
            extent.height += grow;
            grew = true;
        }

        //if (grew) setExtentSize(extent);
        
        p.x = -p.x;
        p.y = -p.y;
        viewport.setViewPosition(p);

        
        if (DEBUG.SCROLL) {
            out("setCanvasPosition, setViewPosition completed");
            if (DEBUG.META) try { Thread.sleep(1000); } catch (Exception e) {}
        }
        /*
        out("setCanvasPosition, ASR:");
        try { Thread.sleep(1000); } catch (Exception e) {}
        adjustExtent(false, false);
        */
    }

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



