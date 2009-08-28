/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import tufts.vue.gui.GUI;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JViewport;

/**
 * MapViewport -- a viewport that handles a dynamically sized extent.
 *
 * This would be better as a static inner class in MapViewer.java, but
 * it's here just to keep the code better organized.
 *
 * The viewport code is complicated to deal with the fact that we
 * operate on an infinite canvas and need to guess at something
 * reasonable to do in a bunch of different cases, and because
 * JScrollPane's/JViewport weren't designed to handle components that
 * may grow up/left as opposed to just down/right.
 *
 * In JViewport, The EXTENT is the physical, visible JPanel, through which
 * the contents of the VIEW (the MapViewer) are scrolled.  Here, we
 * call the view the CANVAS.
 *
 * @version $Revision: 1.17 $ / $Date: 2007/08/28 17:35:02 $ / $Author: sfraize $
 */

public class MapViewport extends JViewport
    implements VueConstants
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapViewport.class);
    
    private /*final*/ MapViewer viewer; // should be final, but can't due to JScrollPane init code
    private final javax.swing.JScrollPane scrollPane;

    //private Rectangle2D lastMapBounds = new Rectangle2D.Float();
    private Dimension lastCanvas = new Dimension();
    private Point2D lastMapLocationAtCanvasOrigin = new Point2D.Float();
    

    public MapViewport(javax.swing.JScrollPane scrollPane) {
//         if (viewer == null)
//             throw new NullPointerException("viewer is null");
//         this.viewer = viewer;
        this.scrollPane = scrollPane;
    }
    
//     public MapViewport(MapViewer viewer) {
//         setView(viewer);
//     }

//     public MapViewport() {}
    
    @Override
    public void setView(Component view) {
        viewer = (MapViewer) view;
//         if (view != viewer)
//             throw new Error("view != viewer; " + view + " != " + viewer);
        super.setView(view);
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

    void setCanvasSize(Dimension d) {
        if (DEBUG.SCROLL) out("   setCanvasSize " + out(d));
        setViewSize(d);
        viewer.setPreferredSize(d);
        //viewer.setSize(d);
    }

    @Override
    public Dimension getViewSize() {
        return getCanvasSize();
    }

    private Point lastPosition;
    private boolean isAdjusting;
    
    @Override
    public void setViewPosition(Point p) {
        if (DEBUG.SCROLL) out("setViewPosition " + out(p));
        if (!p.equals(lastPosition)) {
            // for when scroll-bars are dragged
            viewer.fireViewerEvent(MapViewer.Event.PAN, "MapViewport:setViewPosition0");
        }
        lastPosition = p;
        super.setViewPosition(p);

        if (!isAdjusting())
            viewer.trackViewChanges("MapViewport:setViewPosition1");
    }

    public void setAdjusting(boolean b) {
        isAdjusting = b;
        //scrollPane.getHorizontalScrollBar().setValueIsAdjusting(b);
        //scrollPane.getVerticalScrollBar().setValueIsAdjusting(b);
    }

    public boolean isAdjusting() {

        if (DEBUG.SCROLL) Log.debug("isAdjusting=" + isAdjusting
                                    + ", horzAdjusting=" + scrollPane.getHorizontalScrollBar().getValueIsAdjusting()
                                    + ", vertAdjusting=" + scrollPane.getVerticalScrollBar().getValueIsAdjusting());
        
        return isAdjusting
            || scrollPane.getHorizontalScrollBar().getValueIsAdjusting()
            || scrollPane.getVerticalScrollBar().getValueIsAdjusting();
    }
        

    public void setVisibleCanvasCorner(Point2D p) {
        setCanvasPosition(new Point2D.Double(-p.getX(), -p.getY()));
    }
    
    public void zoomAdjust(Point2D mapAnchor, Point2D screenPositionOfMapAnchor)
    {
        final boolean centerZoomStyle = false;

        adjustCanvasSize(false, true, true, false, true);
        if (centerZoomStyle || screenPositionOfMapAnchor == null)
            placeMapLocationAtViewCenter(mapAnchor);
        else
            placeMapLocationAtViewLocation(mapAnchor, screenPositionOfMapAnchor);
    }

    public void placeMapLocationAtViewLocation(Point2D mapAnchor, Point2D viewLocation)
    {
        Point2D canvasAnchor = viewer.mapToScreenPoint2D(mapAnchor);
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS ANCHOR: " + out(canvasAnchor));
        Point2D.Double canvasOffset = new Point2D.Double();
        canvasOffset.x = canvasAnchor.getX() - viewLocation.getX();
        canvasOffset.y = canvasAnchor.getY() - viewLocation.getY();
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS OFFSET: " + out(canvasOffset));
        setVisibleCanvasCorner(canvasOffset);
    }
    
    public void placeMapLocationAtViewCenter(Point2D mapAnchor)
    {
        placeMapLocationAtViewLocation(mapAnchor, new Point2D.Double(getWidth() / 2, getHeight() / 2));
        /*
        Point2D canvasAnchor = viewer.mapToScreenPoint2D(mapAnchor);
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS ANCHOR: " + out(canvasAnchor));
        Point2D.Double canvasOffset = new Point2D.Double();
        canvasOffset.x = canvasAnchor.getX() - getWidth() / 2.0;
        canvasOffset.y = canvasAnchor.getY() - getHeight() / 2.0;
        if (DEBUG.SCROLL) System.out.println("  ZOOM CANVAS OFFSET: " + out(canvasOffset));
        setVisibleCanvasCorner(canvasOffset);
        */
    }
    
    void adjustSize() {
        adjustCanvasSize(false, true, true, true, false);
    }
    void adjustSize(boolean expand, boolean trimNorthWest, boolean trimSouthEast) {
        adjustCanvasSize(expand, trimNorthWest, trimSouthEast, true, false);
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
     * @param validate - call validate at end (e.g., for intermediate adjustments)
     * @param intermediate - this is an intermediate adjustment, and thus we don't
     * need to ensure that the canvas isn't smaller than the view (e.g., we're about
     * to call setCanvasPosition, which will then adjust our minimum size).
     *
     * todo: Expand is incompatable with the trims -- reorganize arguments.
     * todo: expand not used
     */
    
    private void adjustCanvasSize
        (boolean expand,
         boolean trimNorthWest,
         boolean trimSouthEast,
         boolean validate,
         boolean intermediate)
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
        
        final Rectangle2D mapBounds = getMap().getPaintBounds();
        if (DEBUG.SCROLL) out("---MAP BOUNDS: " + out(mapBounds)
                              + " expand="+expand
                              + " trimNorthWest="+trimNorthWest
                              + " trimSouthEast="+trimSouthEast
                              + " validate="+validate
                              + " intermediate="+intermediate
                              );
        if (DEBUG.SCROLL) out("  view position: " + out(getViewPosition()));

        // compute the size of the minumum canvas that can contain everything in the map
        Rectangle2D.Float mapCanvas = viewer.getContentBounds();
        if (DEBUG.SCROLL) out("     map canvas: " + out(mapCanvas));
        
        Point2D.Float mapLocationAtCanvasOrigin = getMapLocationAtCanvasOrigin();
        
        if (trimNorthWest) {
            
            // If we're collapsing, compress the canvas by moving the
            // origin to the upper left hand corner of all the
            // component bounds.  We "trim" the canvas of usused map
            // "whitespace" when we trimNorthWest.
            
            if (DEBUG.SCROLL) out("     old origin: " + out(viewer.mOffset));
            placeMapLocationAtCanvasOrigin(mapCanvas.x, mapCanvas.y);
            if (DEBUG.SCROLL) out("   reset origin: " + out(viewer.mOffset));
        } else {
            
            // add the current origin, otherwise everything would
            // always be jamming itself up against the upper left hand
            // corner.  This has no effect unless they've moved the
            // component with the smallest x/y (the farthest to the upper
            // left).
            
            if (DEBUG.SCROLL) out("     add offset: " + out(viewer.mOffset));
            if (DEBUG.SCROLL) out("     is map loc: " + out(mapLocationAtCanvasOrigin));
            mapCanvas.add(mapLocationAtCanvasOrigin);
            if (DEBUG.SCROLL) out("    +plusOrigin: " + out(mapCanvas));
        }

        // okay to call this mapToScreen while adjusting origin as we're
        // only interested in the zoom conversion for the size.
        final Dimension minCanvas = viewer.mapToScreenDim(mapCanvas);
        final Dimension curCanvas = getCanvasSize();
        final Dimension newCanvas = new Dimension(minCanvas);
        final Dimension curView = getSize(); // current view size

        //if (!trimNorthWest && lastCanvas.equals(canvas) && lastMapLocationAtCanvasOrigin.equals(mapLocationAtCanvasOrigin))
        //    return;
        
        lastCanvas = minCanvas;
        lastMapLocationAtCanvasOrigin = mapLocationAtCanvasOrigin;
        
        if (!trimNorthWest) {
            // If canvas is outside the the current map origin (that is,
            // something's been dragged off the left or top of the screen),
            // reset the origin to include the region where the components
            // were moved to.
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
        
        //if (curView.equals(newCanvas)) return;

        // unless this is an intermediate adjustment, never let new size be less than current view
        if (!intermediate) {
            if (newCanvas.width < curView.width)
                newCanvas.width = curView.width;
            if (newCanvas.height < curView.height)
                newCanvas.height = curView.height;
        }
            
        if (!trimSouthEast) {
            // don't let new size be less than current canvas
            if (newCanvas.width < curCanvas.width)
                newCanvas.width = curCanvas.width;
            if (newCanvas.height < curCanvas.height)
                newCanvas.height = curCanvas.height;
        }
        
        if (DEBUG.SCROLL) {
            Dimension vp = getSize();
            out("currnt viewport: " + out(vp));
            if (!vp.equals(curView))
            out("!!cur view size: " + out(curView)); // same as above
            out(" current canvas: " + out(curCanvas));
            if (!curCanvas.equals(viewer.getSize()))
            out("!!!!actual size: " + out(viewer.getSize())); // same as above
            out(" minimum canvas: " + out(minCanvas));
            out("     new canvas: " + out(newCanvas));
        }

        setCanvasSize(newCanvas);
        if (validate) {
            // until call validate, calls to setLocation were triggering reshape with old size
            // but now we call setViewSize manually in setCanvasSize, which get's it set
            // in JViewport w/out the revalidate.
            if (DEBUG.SCROLL) out("calling revalidate");
            revalidate();
        }
    }

    @Override
    public void reshape(int x, int y, int w, int h) {
        
        if (DEBUG.SCROLL||DEBUG.PAINT||DEBUG.EVENTS||DEBUG.FOCUS) {
            
            boolean ignore =
                getX() == x &&
                getY() == y &&
                getWidth() == w &&
                getHeight() == h;
            
            Log.debug("reshape: "
                      + (ignore?"(no change) ":"")
                      + w + " x " + h
                      + " "
                      + x + "," + y
                      + " " + this);
        }
                               

        super.reshape(x, y, w, h);

        //viewer.setFreshPaint();
        
        // This is a workaround for repaint bugs in TextBox/JTextPane when it get's taller
        // (when you insert newlines).  Why we get reshapes on the parent (ignoreable ones)
        // every time you type a key (or is it when the TextBox resizes?) I don't know,
        // or why we *must* do the repaint here -- e.g., detecting "enter" keypress
        // and repainting doesn't help in textbox.  Maybe resize/reshape override there would?
        if (viewer.isEditingLabel())
            repaint();
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
        
        
    void pan(int dx, int dy, boolean allowGrowth)
    {
        //Point location = viewport.getViewPosition();
        Point location = viewer.getLocation(); // both x/y should always be <= 0
        if (DEBUG.SCROLL) {
            out("-----------------------------------------------------------------------------");
            out("PAN: dx=" + dx + ", dy=" + dy + " allowGrowth="+allowGrowth);
            out("PAN: viewport start: " + out(location));
        }
        location.translate(-dx, -dy);
        if (DEBUG.SCROLL) out("PAN: viewport   end: " + out(location));

        /*
        if (!allowGrowth) {
            // If drag would take us beyond width or height of existing canvas,
            // clip to existing canvas.
            if (location.x + getWidth() > getCanvasWidth())
                location.x = getCanvasWidth() - getWidth();
            if (location.y + getHeight() > getCanvasHeight())
                location.y = getCanvasHeight() - getHeight();
        }
        */
        
        if (DEBUG.SCROLL) out("PAN: setViewPosition " + out(location));
        
        setCanvasPosition(location, allowGrowth);
        revalidate();
        //viewer.fireViewerEvent(MapViewer.Event.PAN, "MapViewport::pan");
    }
    
    
    private void setCanvasPosition(Point2D p) {
        setCanvasPosition(p, true);
    }
    
    private void setCanvasPosition(Point2D p, boolean allowGrowth) {
        if (DEBUG.SCROLL) out("setCanvasPosition " + out(p) + " allowGrowth=" + allowGrowth);
        
        Dimension canvas = getCanvasSize();
        Dimension view = getSize();

        if (DEBUG.SCROLL) {
            out("setCanvasPosition canvas: " + out(canvas));
            out("setCanvasPosition   view: " + out(view));
        }
        
        boolean grew = false;
        boolean moved = false;
        double ox = viewer.mOffset.x;
        double oy = viewer.mOffset.y;
        double grow;

        if (p.getX() > 0) {
            grow = p.getX();
            if (DEBUG.SCROLL) out("GROW LEFT " + grow);
            p.setLocation(0, p.getY());
            ox -= grow;
            moved = true;
            canvas.width += grow;
            grew = true;
        }
        if (p.getY() > 0) {
            grow = p.getY();
            if (DEBUG.SCROLL) out("GROW UP " + grow);
            p.setLocation(p.getX(), 0);
            oy -= grow;
            moved = true;
            canvas.height += grow;
            grew = true;
        }
        if (canvas.width + p.getX() < view.width) {
            grow = view.width - (canvas.width + p.getX());
            if (DEBUG.SCROLL) out("GROW RIGHT " + grow);
            canvas.width += grow;
            grew = true;
        }
        if (canvas.height + p.getY() < view.height) {
            grow = view.height - (canvas.height + p.getY());
            if (DEBUG.SCROLL) out("GROW DOWN " + grow);
            canvas.height += grow;
            grew = true;
        }

        double px = p.getX();
        double py = p.getY();
        int floorx = (int) px;
        int floory = (int) py;
        if (floorx != px || floory != py) {
            Point2D.Double decimal = new Point2D.Double();
            decimal.x = px - floorx;
            decimal.y = py - floory;
            if (DEBUG.SCROLL) out("compensating " + out(decimal));
            ox -= decimal.x;
            oy -= decimal.y;
            moved = true;
            if (!allowGrowth)
                out("setCanvasPosition: growth disallowed, losing compensation " + out(decimal));
        }

        if (allowGrowth) {
            if (grew)
                setCanvasSize(canvas);
            if (moved)
                viewer.setMapOriginOffset(ox, oy);
        }
            
        setViewPosition(new Point(-floorx, -floory));
        
        if (DEBUG.SCROLL) out("setCanvasPosition completed");
    }

    
        /*
        if (p.x > 0 || p.y > 0) {
            out("GROW ORIGIN");
            //placeMapLocationAtCanvasOrigin(mapCanvas.x, mapCanvas.y);
            //viewport.setViewPosition(p);
            //panScrollRegion(-p.x, -p.y, true);
        }

        */

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
        
        /* from from bottom of old pan:
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
        return "MapViewport[" + this.viewer + "]";
    }

    private void out(Object o) {
        Log.debug(viewer.getDiagName() + " " + (o==null?"null":o.toString()));
        //System.out.println(this + " " + (o==null?"null":o.toString()));
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



