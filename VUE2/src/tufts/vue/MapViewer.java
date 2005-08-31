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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Iterator;
import javax.swing.*;

import tufts.oki.dr.fedora.*;
import tufts.vue.shape.*;

import osid.dr.*;

/**
 * Implements a component for displaying & interacting with
 * an instance of LWMap.  Handles drawing the LWSelection &
 * providing interaction with it.  Provides for moving LWNode's
 * around, dropping them on other LWNodes as children.  Provides
 * context menus. Defers to the active tool for the current
 * cursor, as well as what to when dragging out a selector-box.
 *
 * Implemented as a swing JComponent to be sure to get
 * double-buffering on the PC (is automatic on Mac),
 * and because of course the rest of VUE uses Swing.
 *
 * Note that all the mapToScreen & screenToMap conversion routines
 * would have been more aptly name canvasToMap and mapToCanvas,
 * as they no longer represent actualy on-screen (Panel) locations
 * once the viewer has been put into a JScrollPane.  (If not
 * running in a scroll-pane, they original semantics still apply).
 *
 * @author Scott Fraize
 */

// Note: you'll see a bunch of code for repaint optimzation, which was never 100% completed,
// and is not turned on.
// todo: rename this class LWCanvas & break parts of it out so file size is smaller
public class MapViewer extends javax.swing.JComponent
    implements VueConstants
               , FocusListener
               , LWComponent.Listener
               , LWSelection.Listener
               , VueToolSelectionListener
               , VUE.ActiveViewerListener
{
    static final int RolloverAutoZoomDelay = VueResources.getInt("mapViewer.rolloverAutoZoomDelay");
    static final int RolloverMinZoomDeltaTrigger_int = VueResources.getInt("mapViewer.rolloverMinZoomDeltaTrigger", 10);
    static final float RolloverMinZoomDeltaTrigger = RolloverMinZoomDeltaTrigger_int > 0 ? RolloverMinZoomDeltaTrigger_int / 100f : 0f;
    
    private Rectangle2D.Float RepaintRegion = null; // could handle in DrawContext
    private Rectangle paintedSelectionBounds = null;
    
    public interface Listener extends java.util.EventListener {
        public void mapViewerEventRaised(MapViewerEvent e);
    }

    protected LWMap map;                   // the map we're displaying & interacting with
    private TextBox activeTextEdit;          // Current on-map text edit
    
    // todo make a "ResizeControl" -- a control abstraction that's
    // less than a whole VueTool -- it depends on the current selection,
    // but can still do some drawing on the map while active --
    // (generically, something like a SelectionController -- provides ControlPoints)
    //private LWSelection.ControlPoint[] resizeHandles = new LWSelection.ControlPoint[8];
    //private boolean resizeHandlesActive = false;
    private ResizeControl resizeControl = new ResizeControl();
    
    //-------------------------------------------------------
    // Selection support
    //-------------------------------------------------------
    
    /** an alias for the global selection, reset to null when we're not the active map */
    protected LWSelection VueSelection = null;
    /** a group that contains everything in the current selection.
     *  Used for doing operations on the entire group (selection) at once */
    protected LWGroup draggedSelectionGroup = LWGroup.createTemporary(VUE.ModelSelection);
    /** the currently dragged selection box */
    protected Rectangle draggedSelectorBox;
    /** the last selector box drawn -- for repaint optimization */
    protected Rectangle lastPaintedSelectorBox;
    /** are we currently dragging a selection box? */
    protected boolean isDraggingSelectorBox;
    /** are we currently in a drag of any kind? (mouseDragged being called) */
    protected static boolean sDragUnderway;
    //protected Point2D.Float dragPosition = new Point2D.Float();
    
    protected LWComponent indication;   // current indication (drag rollover hilite)
    protected LWComponent rollover;   // current rollover (mouse rollover hilite)
    
    //-------------------------------------------------------
    // Pan & Zoom Support
    //-------------------------------------------------------
    // package-private for MapViewport class
    double mZoomFactor = 1.0;
    double mZoomInverse = 1/mZoomFactor;
    Point2D.Float mOffset = new Point2D.Float();
    
    //-------------------------------------------------------
    // VueTool support
    //-------------------------------------------------------
    
    /** The currently selected tool **/
    private VueTool activeTool;
    
    private final VueTool ArrowTool = VueToolbarController.getController().getTool("arrowTool");
    private final VueTool HandTool = VueToolbarController.getController().getTool("handTool");
    private final VueTool ZoomTool = VueToolbarController.getController().getTool("zoomTool");
    private final NodeTool NodeTool = (NodeTool) VueToolbarController.getController().getTool("nodeTool");
    private final VueTool LinkTool = VueToolbarController.getController().getTool("linkTool");
    private final VueTool TextTool = VueToolbarController.getController().getTool("textTool");
    private final VueTool PathwayTool = VueToolbarController.getController().getTool("pathwayTool");

    
    //-------------------------------------------------------
    // Scroll-pane support
    //-------------------------------------------------------
    
    private boolean inScrollPane = false;
    private MapViewport mViewport;
    private boolean isFirstReshape = true;
    private boolean didReshapeZoomFit = false;
    private static final boolean scrollerCoords = false;// get rid of this

    private final static boolean UseMacFocusBorder = false;
    
    public MapViewer(LWMap map) {
        this(map, "");
    }
    
    private String instanceName;
    MapViewer(LWMap map, String instanceName)
    {
        this.instanceName = instanceName;
        this.activeTool = VueToolbarController.getActiveTool();
        if (activeTool == null)
            activeTool = ArrowTool;
        this.mapDropTarget = new MapDropTarget(this); // new CanvasDropHandler
        this.setDropTarget(new java.awt.dnd.DropTarget(this, mapDropTarget));
    
        setOpaque(true);
        setLayout(null);
        if (map.getFillColor() != null)
            setBackground(map.getFillColor());
        loadMap(map);
        
        //-------------------------------------------------------
        // If this map was just restored, there might
        // have been an existing userZoom or userOrigin
        // set -- we honor that last user configuration here.
        //-------------------------------------------------------
        if (getMap().getUserZoom() != 1.0)
            setZoomFactor(getMap().getUserZoom(), false, null, false);

        VUE.ModelSelection.addListener(this);
        VUE.addActiveViewerListener(this);
        
        // draggedSelectionGroup is always a selected component as
        // it's only used when it IS the selection
        // There was some reason we need to have the set -- what was it?
        draggedSelectionGroup.setSelected(true);
        
        // TODO: need to remove us as listener for this & VUE selection this map is closed.
        // listen to tool selection events
        VueToolbarController.getController().addToolSelectionListener(this);

        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler); // have zoom tool do this?
        if (DEBUG.INIT||DEBUG.FOCUS) out("CONSTRUCTED.");
    }
    
    boolean inScrollPane() {
        return inScrollPane;
    }

    private JComponent mFocusIndicator = new JPanel(); // make sure is never null
    private InputHandler inputHandler = new InputHandler(this);
    private boolean mAddNotifyUnderway = false;

    // THIS IS BREAKING US IN JAVA 1.5.0 ON MAC
    public void addNotify()
    {
        super.addNotify();
        if (mAddNotifyUnderway) {
            if (DEBUG.INIT) out("(bootstrapped add-notify for viewport)");
            return;
        }
        mAddNotifyUnderway = true;
        inScrollPane = (getParent() instanceof JViewport);
        boolean bootstrapped = false;
        if (getParent() instanceof MapViewport) {
            bootstrapped = true;
            // we're already in a MapViewport -- happens after
            // reparenting back from a full-screen display.
            mViewport = (MapViewport) getParent();
        } else if (inScrollPane) {
            JScrollPane sp = (JScrollPane) getParent().getParent();

            if (true) {
                if (DEBUG.INIT) out("creating viewport");
                mViewport = new MapViewport(this);
                if (DEBUG.INIT) out("installing our own viewport on JScrollPane");
                sp.setViewport(mViewport);
                if (DEBUG.INIT) out("back from installing our own viewport on JScrollPane");
            } else {
                //mViewport = (JViewport) getParent();
                // todo perf: auto-scroll is slowing down operations that
                // don't need it whenever the mouse is dragged just beyond
                // the edge of the map (still?)
            }
            
            setAutoscrolls(true);
            //scrollerCoords = true;
            // don't know if this every worked: weren't
            // able to even get focus listening to the viewport!

            mFocusIndicator = new FocusIndicator();
            sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, mFocusIndicator);
            //sp.setViewportBorder(BorderFactory.createLineBorder(Color.red, 1));
            //sp.setBorder(BorderFactory.createLineBorder(Color.red, 1));
            
            if (UseMacFocusBorder) {
                // Leave default installed special mac focus border.
                // The Mac Aqua focus border looks fantastic, but we have to
                // repaint the whole map every time the focus changes to
                // another map, which is slow.
            } else if (VueUtil.isMacAquaLookAndFeel()) {
                if (VueTheme.isMacMetalLAF())
                     // use same color as mac brushed metal inactive border
                    sp.setBorder(BorderFactory.createLineBorder(new Color(155,155,155), 1));
                else
                    sp.setBorder(null); // no border at all for now for default mac look
            }

        } else {
            //scrollerCoords = false;
            mViewport = null;
        }
        
        /*
        if (inScrollPane) {
            JScrollPane sp = (JScrollPane) mViewport.getParent();
            //System.out.println("vpParent="+p);
            hsb = sp.getHorizontalScrollBar();
            System.out.println("hsb="+hsb);
            System.out.println("model="+hsb.getModel());
            hsb.setModel(new ScrollModel(hsb.getModel()));
        }
         */
        
        // if nothing visible, do a zoom fit
        /*
          // can't do this here: nothing visible yet
        if (computeSelection(getVisibleMapBounds(), null).size() == 0) {
            out("***GET VISIBLE BOUNDS " + getVisibleBounds());
            out("***GET SIZE " + getSize());
            out("***GET PREF SIZE " + getPreferredSize());
            //tufts.vue.ZoomTool.setZoomFit(this);
            DEBUG.FOCUS=true;
        }
        */
        
        //addKeyListener(inputHandler);
        if (!bootstrapped)
            addFocusListener(this);
        
        if (inScrollPane && !bootstrapped) {
            //mViewport.addFocusListener(this); // do we need this?
            mViewport.getParent().addFocusListener(this);
        }
        
        if (false&&inScrollPane) {
            Rectangle2D mb = getContentBounds();
            setMapOriginOffset(mb.getX(), mb.getY());
        } else {
            Point2D p = getMap().getUserOrigin();
            setMapOriginOffset(p.getX(), p.getY());
        }

        if (scrollerCoords) {
            // Do this is you want mouse events to
            // come to us in view as opposed to canvas
            // coordinates when in scroll pane.
            mViewport.addMouseListener(inputHandler);
            mViewport.addMouseMotionListener(inputHandler);
            //mViewport.getParent().addMouseListener(inputHandler);
            //mViewport.getParent().addMouseMotionListener(inputHandler);
        } else {
            //addMouseListener(inputHandler);
            //addMouseMotionListener(inputHandler);
        }

        requestFocus();
        mAddNotifyUnderway = false;
        //VUE.invokeAfterAWT(new Runnable() { public void run() { ensureMapVisible(); }});
    }



    private static final Color AquaFocusBorderLight = new Color(157, 191, 222);
    private static final Color AquaFocusBorderDark = new Color(137, 170, 201);
    
    /** a little box for the lower right of a JScrollPane indicating this viewer's focus state */
    private class FocusIndicator extends JComponent {
        final Color fill;
        final Color line;
        final static int inset = 4;
        
        FocusIndicator() {
            if (VueUtil.isMacAquaLookAndFeel()) {
                fill = AquaFocusBorderLight;
                line = AquaFocusBorderLight.darker();
                //line = AquaFocusBorderDark;
            } else {
                fill = VueTheme.getToolbarColor();
                line = fill.darker();
            }
        }
        
        public void paintComponent(Graphics g) {
            //if (VUE.multipleMapsVisible() || DEBUG.Enabled || DEBUG.FOCUS)
            paintIcon(g);
        }
        
        void paintIcon(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            
            // no effect on muddling with mac aqua JScrollPane focus border
            //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            
            // fill a block if we own the VUE application focus (Actions apply here)
            if (VUE.getActiveViewer() == MapViewer.this) {
                g.setColor(fill);
                g.fillRect(inset, inset, w-inset*2, h-inset*2);
            }
            
            // Draw a box if we own the KEYBOARD focus, which will
            // appear as a border to the above block assuming we have
            // VUE app focus.  Keyboard focus effects special cases
            // such as holding down the space-bar to trigger the
            // hand/pan tool, which is not an action, but a key
            // detected right on the MapViewer.  Also, e.g., the
            // viewer loses keyboard focus when there is an
            // activeTextEdit, while keeping VUE app focus.
            
            if (MapViewer.this.isFocusOwner()) {
                g.setColor(line);
                w--; h--;
                g.drawRect(inset, inset, w-inset*2, h-inset*2);
            }
            //if (DEBUG.FOCUS) out("painted focusIndicator");
        }
    }
    
    // todo: temporary till break processTransferable out of MapDropTarget
    // (for paste action)
    private MapDropTarget mapDropTarget;
    MapDropTarget getMapDropTarget() {
        return mapDropTarget;
    }

    
    /**
     * getCurrentTool()
     * Gets the current VueTool that is selected.
     * @return the slected VueTool
     **/
    public VueTool getCurrentTool() {
        return activeTool;
    }
    
    
    /**
     * Sets the current VueTool for the map viewer.
     * Updates any selection or state issues pased on the tool
     * @param pTool - the new tool
     **/
    
    public void toolSelected(VueTool pTool) {
        if (DEBUG.FOCUS && VUE.getActiveViewer() == this) out("toolSelected: " + pTool.getID());
        
        if (pTool == null) {
            System.err.println(this + " *** toolSelected: NULL TOOL");
            return;
        }
        if (pTool.getID() == null) {
            System.err.println(this + " *** toolSelected: NULL ID IN TOOL!");
            return;
        }
        
        VueTool oldTool = activeTool;
        activeTool = pTool;
        setMapCursor(activeTool.getCursor());
        
        if (isDraggingSelectorBox) // in case we change tool via kbd shortcut in the middle of a drag
            repaint();
        else if (oldTool != null && oldTool.hasDecorations() || pTool.hasDecorations())
            repaint();
    }
    
    private void setMapCursor(Cursor cursor) {
        //SwingUtilities.getRootPane(this).setCursor(cursor);
        // could compute cursor-set pane in addNotify
        setCursor(cursor);
        // todo: also set this on the VueToolPanel so you can see cursor change
        // when selecting new tool -- actually, VueToolPanel should
        // do this itself as we're going to put the cursors right in
        // the tool
        
    }
    
    public double getZoomFactor() {
        return mZoomFactor;
    }
    
    void fireViewerEvent(int id) {
        if (/*!sDragUnderway &&*/ (id == MapViewerEvent.HIDDEN || VUE.getActiveViewer() == this)
            || (id == MapViewerEvent.ZOOM && VUE.multipleMapsVisible())) // todo: good enough for presentation mode viewer
            new MapViewerEvent(this, id).raise();
    }
    
    void resetScrollRegion() {
        adjustCanvasSize(false, true, true);
    }
    
    private void adjustCanvasSize() {
        adjustCanvasSize(true, false, false);
    }
    
    private void adjustCanvasSize(boolean expand, boolean trimNorthWest, boolean trimSouthEast)
    {
        if (inScrollPane)
            mViewport.adjustSize(expand, trimNorthWest, trimSouthEast);
    }
    
    /**
     * @param pZoomFactor -- the new zoom factor
     * @param pReset -- completely reset the scrolling region to the map bounds
     * @param pMapAnchor

     * -- the on screen focus point, in panel canvas
     * coordinates when in scroll pane, so upper left may be > 0,0.
     * Mouse events are given to us in these panel coordinates, but
     * if, say, you wanted to zoom in on the center of the *visible*
     * area, accounting for scrolled state, you'll need to find the
     * panel location in the center of viewport first.  The map
     * location under the focus location should be the same after the
     * zoom as it was before the zoom.  Can be null if don't want to
     * make this adjustment.

     */
    
    void setZoomFactor(double pZoomFactor, boolean pReset, Point2D mapAnchor, boolean centerOnAnchor) {
        if (DEBUG.SCROLL) out("ZOOM: reset="+pReset + " Z="+pZoomFactor + " focus="+mapAnchor);
        
        if (mapAnchor == null && !pReset) {
            mapAnchor = screenToMapPoint2D(getVisibleCenter());
            if (DEBUG.SCROLL) out("ZOOM MAP CENTER: " + out(mapAnchor));
        } else {
            if (DEBUG.SCROLL) out("ZOOM MAP ANCHOR: " + out(mapAnchor));
        }
        
        Point2D.Float offset = null; // offset for non-scrol-region zoom
        Point2D screenPositionOfMapAnchor = null; // offset 
        
        if (!pReset) {
            if (inScrollPane) {
                if (!centerOnAnchor) {
                    Point2D canvasPosition = mapToScreenPoint2D(mapAnchor);
                    Point canvasOffset = getLocation(); // scrolled offset of our canvas in the scroll-pane
                    screenPositionOfMapAnchor = new Point2D.Double(canvasPosition.getX() + canvasOffset.x,
                                                                   canvasPosition.getY() + canvasOffset.y);
                }
            } else {
                // This is for non-scroll map viewer: it works to keep the focus
                // location at the same point on the screen.  
                //if (DEBUG.SCROLL) System.out.println(" ZOOM VIEWPORT FOCUS: " + out(pFocus));
                Point2D focus = mapToScreenPoint2D(mapAnchor);
                offset = new Point2D.Float();
                offset.x = (float) ((mapAnchor.getX() * pZoomFactor) - focus.getX());
                offset.y = (float) ((mapAnchor.getY() * pZoomFactor) - focus.getY());
                //if (DEBUG.SCROLL) System.out.println("   ZOOM FOCUS OFFSET: " + out(offset));
                setMapOriginOffset(offset.x, offset.y, false);
            }
        }

        //------------------------------------------------------------------
        // Set the new zoom factor: everything immediately "moves"
        // it's on screen position when you do this as all the the
        // map/screen conversion methods that compute with the zoom
        // factor start returning different values (with single
        // exception of map coordinate value 0,0 if it happens to be
        // on screen)
        // ------------------------------------------------------------------

        mZoomFactor = pZoomFactor;
        mZoomInverse = 1.0 / mZoomFactor;

        if (DEBUG.SCROLL) out("ZOOM FACTOR set to " + mZoomFactor);
        
        // record zoom factor in map for saving
        getMap().setUserZoom(mZoomFactor);
        
        //------------------------------------------------------------------
        
        if (inScrollPane) {
            if (mapAnchor != null && !pReset) {
                mViewport.zoomAdjust(mapAnchor, screenPositionOfMapAnchor);
            } else {
                adjustCanvasSize(false, true, true);
            }
        } else {
            if (mapAnchor != null)
                setMapOriginOffset(offset.x, offset.y);
        }
        
        repaint();
        fireViewerEvent(MapViewerEvent.ZOOM);
    }
    

    void panScrollRegion(int dx, int dy) {
        panScrollRegion(dx, dy, true);
    }
    
    void panScrollRegion(int dx, int dy, boolean allowGrowth) {
        if (inScrollPane) {
            mViewport.pan(dx, dy, allowGrowth);
        } else {
            setMapOriginOffset(mOffset.x + dx, mOffset.y + dy);
        }
    }
    
    public void setPreferredSize(Dimension d) {
        if (DEBUG.SCROLL) out("setPreferred: " + out(d));
        super.setPreferredSize(d);
    }
    /*
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (DEBUG.SCROLL) out("getPreferred: " + out(d));
        return d;
    }
    */

    public void setSize(Dimension d) {
        if (DEBUG.SCROLL) out("     setSize: " + out(d));
        // new Throwable("SETSIZE " + out(d)).printStackTrace();
        super.setSize(d);
    }
    
    
    /**
     * The given PIXEL offset is the pixel location that the
     * 0,0 map coordinate will appear on screen/or in the canvas.
     * Values < 0 or greater the the view size mean the
     * 0,0 map location will not be visible.
     *
     * The floating precision is due to possibility of zooming,
     * and needing to represent partial pixel values.
     */
    
    void setMapOriginOffset(float panelX, float panelY, boolean update) {
        if (DEBUG.SCROLL) out("setMapOriginOffset old:" + out(mOffset));
        if (DEBUG.SCROLL) out("setMapOriginOffset new:" + panelX + ", " + panelY);
        mOffset.x = panelX;
        mOffset.y = panelY;
        // todo: when in scroll region, user origin being offset 12 or so pixels
        // (probably width of scroll bar) -- would be nice to keep normalized to 0
        // so doesn't always offset it (will it do that cumulative every time we start??)
        if (VUE.getActiveViewer() == this)
            getMap().setUserOrigin(panelX, panelY);
        if (!inScrollPane && update) {
            repaint();
            fireViewerEvent(MapViewerEvent.PAN);
        }
    }
    
    public void setMapOriginOffset(float panelX, float panelY) {
        setMapOriginOffset(panelX, panelY, true);
    }
    
    public void setMapOriginOffset(double panelX, double panelY) {
        setMapOriginOffset((float) panelX, (float) panelY);
    }
    
    
    public Point2D.Float getOriginLocation() {
        return new Point2D.Float(getOriginX(), getOriginY());
    }
    public float getOriginX() {
        //return inScrollPane ? -getX() : mOffset.x;
        return mOffset.x;
    }
    public float getOriginY() {
        //return inScrollPane ? -getY() : mOffset.y;
        return mOffset.y;
    }

    //------------------------------------------------------------------
    // The core conversion routines: todo: rename "screen" to "canvas",
    // as "screen" no longer accurate if we're in a scroll-pane.
    //------------------------------------------------------------------
    float screenToMapX(float x) {
        //if (scrollerCoords) return (float) ((x + getOriginX()) * mZoomInverse) + getX();
        return (float) ((x + getOriginX()) * mZoomInverse);
    }
    float screenToMapY(float y) {
        //if (scrollerCoords) return (float) ((y + getOriginX()) * mZoomInverse) + getY();
        return (float) ((y + getOriginY()) * mZoomInverse);
    }
    float screenToMapX(double x) {
        return (float) ((x + getOriginX()) * mZoomInverse);
    }
    float screenToMapY(double y) {
        return (float) ((y + getOriginY()) * mZoomInverse);
    }
    int mapToScreenX(double x) {
        //if (scrollerCoords) return (int) (0.5 + ((x * mZoomFactor) - getOriginX())) - getX();
        return (int) (0.5 + ((x * mZoomFactor) - getOriginX()));
    }
    int mapToScreenY(double y) {
        //if (scrollerCoords) return (int) (0.5 + ((y * mZoomFactor) - getOriginY())) - getY();
        return (int) (0.5 + ((y * mZoomFactor) - getOriginY()));
    }
    double mapToScreenX2D(double x) {
        return x * mZoomFactor - getOriginX();
    }
    double mapToScreenY2D(double y) {
        return y * mZoomFactor - getOriginY();
    }
    
    //------------------------------------------------------------------
    // Convenience conversion routines
    //------------------------------------------------------------------
    float screenToMapX(int x) {
        return screenToMapX((float)x);
        //return (float) ((x + getOriginX()) * mZoomInverse);
    }
    float screenToMapY(int y) {
        return screenToMapY((float)y);
        //return (float) ((y + getOriginY()) * mZoomInverse);
    }
    float screenToMapDim(int dim) {
        return (float) (dim * mZoomInverse);
    }
    Point2D.Float screenToMapPoint2D(Point2D p) {
        return new Point2D.Float(screenToMapX(p.getX()), screenToMapY(p.getY()));
    }
    Point2D.Float screenToMapPoint(Point p) {
        return screenToMapPoint(p.x, p.y);
    }
    Point2D.Float screenToMapPoint(int x, int y) {
        return new Point2D.Float(screenToMapX(x), screenToMapY(y));
    }
    
    Point mapToScreenPoint(Point2D p) {
        return new Point(mapToScreenX(p.getX()), mapToScreenY(p.getY()));
    }
    Point2D mapToScreenPoint2D(Point2D p) {
        return new Point2D.Double(mapToScreenX2D(p.getX()), mapToScreenY2D(p.getY()));
    }
    int mapToScreenDim(double dim) {
        if (dim > 0)
            return (int) (0.5 + (dim * mZoomFactor));
        else
            return (int) (0.5 + (-dim * mZoomFactor));
    }
    Rectangle mapToScreenRect(Rectangle2D mapRect) {
        //if (mapRect.getWidth() < 0 || mapRect.getHeight() < 0)
        //    throw new IllegalArgumentException("mapDim<0");
        Rectangle screenRect = new Rectangle();
        // Make sure we round out to the largest possible pixel rectangle
        // that contains all map coordinates
        
        if (scrollerCoords) {
            screenRect.x = mapToScreenX(mapRect.getX());
            screenRect.y = mapToScreenY(mapRect.getY());
        } else {
            screenRect.x = (int) Math.floor(mapRect.getX() * mZoomFactor - getOriginX());
            screenRect.y = (int) Math.floor(mapRect.getY() * mZoomFactor - getOriginY());
        }
        screenRect.width = (int) Math.ceil(mapRect.getWidth() * mZoomFactor);
        screenRect.height = (int) Math.ceil(mapRect.getHeight() * mZoomFactor);
        
        //screenRect.x = (int) Math.round(mapRect.getX() * mZoomFactor - getOriginX());
        //screenRect.y = (int) Math.round(mapRect.getY() * mZoomFactor - getOriginY());
        //screenRect.width = (int) Math.round(mapRect.getWidth() * mZoomFactor);
        //screenRect.height = (int) Math.round(mapRect.getHeight() * mZoomFactor);
        return screenRect;
    }
    
    Dimension mapToScreenDim(Rectangle2D mapRect) {
        Rectangle screenRect = mapToScreenRect(mapRect);
        return new Dimension(screenRect.width, screenRect.height);
    }
    
    Rectangle2D.Float mapToScreenRect2D(Rectangle2D mapRect) {
        if (mapRect.getWidth() < 0 || mapRect.getHeight() < 0)
            throw new IllegalArgumentException("mapDim<0");
        Rectangle2D.Float screenRect = new Rectangle2D.Float();
        if (scrollerCoords) {
            screenRect.x = (float) mapToScreenX(mapRect.getX());
            screenRect.y = (float) mapToScreenY(mapRect.getY());
        } else {
            screenRect.x = (float) (mapRect.getX() * mZoomFactor - getOriginX());
            screenRect.y = (float) (mapRect.getY() * mZoomFactor - getOriginY());
        }
        screenRect.width = (float) (mapRect.getWidth() * mZoomFactor);
        screenRect.height = (float) (mapRect.getHeight() * mZoomFactor);
        return screenRect;
    }
    Rectangle2D screenToMapRect(Rectangle screenRect) {
        if (screenRect.width < 0 || screenRect.height < 0)
            throw new IllegalArgumentException("screenDim<0 " + screenRect + " in " + this);
        Rectangle2D mapRect = new Rectangle2D.Float();
        mapRect.setRect(screenToMapX(screenRect.x),
        screenToMapY(screenRect.y),
        screenToMapDim(screenRect.width),
        screenToMapDim(screenRect.height));
        return mapRect;
    }
    
    public int getVisibleWidth() {
        return inScrollPane ? mViewport.getWidth() : getWidth();
    }
    public int getVisibleHeight() {
        return inScrollPane ? mViewport.getHeight() : getHeight();
    }
    public Dimension getVisibleSize() {
        Dimension d = new Dimension(getVisibleWidth(), getVisibleHeight());
        if (DEBUG.SCROLL) out(" visible size: " + out(d));
        return d;
    }
    
    /**
     * When in a JScrollPane, the currently visible portion of the
     * MapViewer canvas.  When not in a scroll pane, it's just
     * the size of the component (and x=y=0);
     */
    private Rectangle getVisibleBounds() {
        if (inScrollPane) {
            // In scroll pane, location of this panel goes negative
            // as it's scrolled off to the left.
            return new Rectangle(-getX(), -getY(), mViewport.getWidth(), mViewport.getHeight());
        } else {
            return new Rectangle(0, 0, getWidth(), getHeight());
        }
    }
    
    /** @return the coordinate of this JComponent (the canvas coordinate) currently
     * visible in the center the viewport.  This is the same x/y value you'd get
     * from a mouse event clicked exactly in the middle of the displayed viewport,
     * which if scroll all the way up-left, will be same as canvas coords, but if not,
     * will be offset by scrolled amount.
     */
    public Point2D getVisibleCenter() {
        return viewportToCanvasPoint(getVisibleWidth() / 2.0, getVisibleHeight() / 2.0);
    }
    
    private Point2D viewportToCanvasPoint(double x, double y) {
        if (inScrollPane)
            return new Point2D.Double(x - getX(), y - getY());
        else
            return new Point2D.Double(x, y);
    }
    
    /**
     * Return the bounds of the map region that can actually be seen
     * in the display at this moment, accouting for any scrolled
     * state within the JViewport of a JScrollPane, zoom state, etc.
     * This could be a blank area of the map -- it's just where we
     * happen to be panned to and displaying at the moment.
     */
    public Rectangle2D getVisibleMapBounds() {
        return screenToMapRect(getVisibleBounds());
    }
    
    /**
     * @return in map coordinate space, the bounds represented by the
     * size of the total canvas.  When not in a JScrollPane, this is
     * the same as getVisibleMapBounds.  When in a scroll pane (a
     * JViewport) The size of the canvas may be arbitrarily large
     * depending on where the user has panned or dragged out to --
     * e.g., it may be much bigger than the bounds of the map
     * components.
     */
    public Rectangle2D getCanvasMapBounds() {
        return screenToMapRect(new Rectangle(0,0, getWidth(), getHeight()));
    }

    /**
     * Return, in Map coords, a bounding box for all the LWComponents in the
     * displayed map, including room for possible selection handles or
     * rendered selection highlights.  Will in effect be just a bit
     * bigger than getMap().getBounds(). todo: account for zoom?
     */
    
    private final static float SelectionStrokeMargin = SelectionStrokeWidth/2;
    public Rectangle2D.Float getContentBounds() {
        Rectangle2D.Float r = (Rectangle2D.Float) getMap().getBounds().clone();
        // because the selection stroke is rendered at scale (gets bigger
        // as we zoom in) we account for it here in the total bounds
        // needed to see everything on the map.
        if (!DEBUG.MARGINS) {
            r.x -= SelectionStrokeMargin;
            r.y -= SelectionStrokeMargin;
            r.width += SelectionStrokeWidth;
            r.height += SelectionStrokeWidth;
        }
        Rectangle2D.Float rr = growForSelection(r); // now grow it for the selection handles
        //if (DEBUG.SCROLL) out("getContentBounds " + rr);
        return rr;
    }
    
    protected void processEvent(AWTEvent e) {
        if (e instanceof MouseEvent) {
            super.processEvent(e);
            return;
        }
        if (DEBUG.VIEWER) out("MAPVIEWER: processEvent " + e);
        super.processEvent(e);
    }
    
    public void reshape(int x, int y, int w, int h) {
        boolean ignore =
            getX() == x &&
            getY() == y &&
            getWidth() == w &&
            getHeight() == h;

        // We get reshape events during text edits with no change
        // in size, yet are crucial for repaint update (thus: no ignore if activeTextEdit)
        
        if (DEBUG.SCROLL||DEBUG.PAINT||DEBUG.EVENTS||DEBUG.FOCUS)
            out("     reshape: "
                + w + " x " + h
                + " "
                + x + "," + y
                + (ignore?" (IGNORING)":""));

        /*
        if (w > 1 && h > y) {
            if (isFirstReshape || VUE.isStartupUnderway()) {
                isFirstReshape = false;
                // if nothing visible, do a zoom-fit
                out("*******REZOOMING******");
            }
        }
        */

        super.reshape(x,y, w,h);

        if (DEBUG.VIEWER || ignore && activeTextEdit != null)
            // if active text is transparent, we'll need this to draw under blinking cursor
            repaint(); 

        if (!ignore)
            fireViewerEvent(MapViewerEvent.PAN);
    }

    /** at startup make sure the contents of the map are visible in the viewport */
    private void ensureMapVisible()
    {
        if (getMap().hasChildren()) {
            int count = computeSelection(getVisibleMapBounds(), null).size();
            if (DEBUG.INIT || DEBUG.VIEWER) out("i see " + count + " components in visible map bounds " + getVisibleMapBounds());
            if (count == 0)
                tufts.vue.ZoomTool.setZoomFit(this);
        }
    }
    /*
    private boolean isAnythingCurrentlyVisible()
    {
        Rectangle mapRect = mapToScreenRect(getMap().getBounds());
        Rectangle viewerRect = getBounds(null);
        return mapRect.intersects(viewerRect);
    }
     */
    
    
    private boolean isDisplayed() {
        if (!isShowing())
            return false;
        if (inScrollPane) {
            System.out.println("parent=" + getParent());
            return getParent().getWidth() > 0 && getParent().getHeight() > 0;
        } else
            return getWidth() > 0 && getHeight() > 0;
    }
    
    
    private int lastMouseX;
    private int lastMouseY;
    private int lastMousePressX;
    private int lastMousePressY;
    private void setLastMousePressPoint(int x, int y) {
        lastMousePressX = x;
        lastMousePressY = y;
        setLastMousePoint(x,y);
    }
    /** last place mouse pressed */
    Point getLastMousePressPoint() {
        return new Point(lastMousePressX, lastMousePressY);
    }
    private void setLastMousePoint(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
    }
    /** last place mouse was either pressed or released */
    Point getLastMousePoint() {
        return new Point(lastMouseX, lastMouseY);
    }
    
    public LWMap getMap() {
        return this.map;
    }
    
    private void unloadMap() {
        this.map.removeLWCListener(this);
        this.map = null;
    }
    
    private void loadMap(LWMap map) {
        if (map == null)
            throw new IllegalArgumentException(this + " loadMap: null LWMap");
        if (this.map != null)
            unloadMap();
        this.map = map;
        this.map.addLWCListener(this);
        if (this.map.getUndoManager() == null) {
            if (map.isModified()) {
                out("Note: this map has modifications undo will not see");
                //VueUtil.alert(this, "This map has modifications undo will not see.", "Note");
            }
            this.map.setUndoManager(new UndoManager(this.map));
        }
        repaint();
    }

    void paintImmediately() {
        paintImmediately(getVisibleBounds());
    }
    
    private void RR(Rectangle r) {
        if (OPTIMIZED_REPAINT)
            super.repaint(0,r.x,r.y,r.width,r.height);
        else
            super.repaint();
    }
    
    private Rectangle mapRectToPaintRegion(Rectangle2D mapRect) {
        // todo: is this taking into account the current zoom?
        Rectangle r = mapToScreenRect(mapRect);
        r.width++;
        r.height++;
        // mac leaving trailing borders at right & bottom: todo: why?
        r.width++;
        r.height++;
        return r;
    }
    
    private boolean paintingRegion = false;
    private void repaintMapRegion(Rectangle2D mapRect) {
        if (OPTIMIZED_REPAINT) {
            paintingRegion = true;
            repaint(mapRectToPaintRegion(mapRect));
        }
        else
            repaint();
    }
    
    private void repaintMapRegionGrown(Rectangle2D mapRect, float growth) {
        if (OPTIMIZED_REPAINT) {
            mapRect.setRect(mapRect.getX() - growth/2,
            mapRect.getY() - growth/2,
            mapRect.getWidth() + growth,
            mapRect.getHeight() + growth);
            repaint(mapRectToPaintRegion(mapRect));
        } else
            repaint();
    }
    
    /** repaint region adjusting for presence of selection handles
     * outside the edges of what's selected */
    private void repaintMapRegionAdjusted(Rectangle2D mapRect) {
        if (OPTIMIZED_REPAINT)
            repaint(growForSelection(mapToScreenRect(mapRect)));
        else
            repaint();
    }
    
    // We grow the bounds here to include for the possability of any selection
    // handles that may be need to be drawn around components.
    private Rectangle growRect(Rectangle r, int pad) {
        if (!DEBUG.MARGINS) {
            final int margin = pad;
            final int adjust = margin * 2;
            r.x -= margin;
            r.y -= margin;
            r.width += adjust;
            r.height += adjust;
        }
        return r;
    }
    
    /*
    public void repaint() { // heavy-duty debug
        new Throwable().printStackTrace();
        super.repaint();
    }
     */
    
    /*
    private Rectangle growForSelection(Rectangle r, int pad)
    {
        final int margin = SelectionHandleSize;
        final int adjust = margin * 2;
        r.x -= margin;
        r.y -= margin;
        r.width += adjust + 1;
        r.height += adjust + 1;
        // adding 2 to SHS at moment due to Mac bugs
        //int adjust = SelectionHandleSize + 2;
        return r;
    }
     */
    
    private Rectangle growForSelection(Rectangle r) { return growRect(r, SelectionHandleSize); }
    private Rectangle growForSelection(Rectangle r, int pad) { return growRect(r, SelectionHandleSize+pad); }
    
    // same as grow for selection, but operates on map coordinates
    private Rectangle2D.Float growForSelection(Rectangle2D.Float r) {
        if (!DEBUG.MARGINS) {
            float margin = (float) (SelectionHandleSize * mZoomInverse);
            float adjust = margin * 2;
            r.x -= margin;
            r.y -= margin;
            r.width += adjust + 1;
            r.height += adjust + 1;
        }
        return r;
    }
    
    public void selectionChanged(LWSelection s) {
        //System.out.println("MapViewer: selectionChanged");
        activeTool.handleSelectionChange(s);
        if (VUE.getActiveMap() != this.map)
            VueSelection = null; // insurance: nothing should be happening here if we're not active
        else {
            if (VueSelection != VUE.ModelSelection) {
                if (DEBUG.FOCUS) out("*** Pointing to selection");
                VueSelection = VUE.ModelSelection;
            }
        }
        repaintSelection();
    }
    
    /** update the regions of both the old selection & the new selection */
    public void repaintSelection() {
        if (paintedSelectionBounds != null)
            RR(paintedSelectionBounds);
        if (VueSelection != null) {
            Rectangle2D newBounds = VueSelection.getBounds();
            if (newBounds != null)
                repaintMapRegionAdjusted(newBounds);
        }
    }
    
    private boolean isBoundsEvent(String k) {
        return k == LWKey.Size
            || k == LWKey.Location
            || k == LWKey.Frame
            || k == LWKey.Scale
            || k == LWKey.Hidden
            || k == LWKey.Created
            || k == LWKey.StrokeWidth  // because size events special off for this!
            || k == LWKey.Font // because size events special off for this!
            || k == LWKey.Label // because size events special off for this!
            //|| k == LWKey.Deleted // not currently being issued!
            //|| k == LWKey.Filtered // no such event yet
            ;
    }
    
    /**
     * Handle events coming off the LWMap we're displaying.
     * For most actions this repaints.  It tracks deletiions
     * for updating the current rollover zoom.
     */
    public void LWCChanged(LWCEvent e) {
        if (DEBUG.EVENTS && DEBUG.META) out("got " + e);
        
        final Object key = e.getKey();

        if (DEBUG.DYNAMIC_UPDATE == false) {
            if (key == LWKey.RepaintAsync) {
                repaint();
                return;
            } else if (VUE.getActiveViewer() != this) {
                // this prevents other viewers of same map from updating until an
                // action is completed in the active viewer.
                if (sDragUnderway || key != LWKey.UserActionCompleted)
                    return;
            } else {
                // The ACTIVE viewer can ignore these events,
                // because we've been repainting all the updates
                // due to events as they've been happening.
                if (key == LWKey.UserActionCompleted)
                    return;
            }
        }
        
        // ? todo: optimize -- we get lots of extra location events
        // when dragging if there are children of the dragged
        // object (still true?)
        
        //if (isBoundsEvent(key))
            adjustCanvasSize();
        
        if (key == LWKey.Deleting) {
            if (rollover == e.getComponent())
                clearRollover();
        } else if (key == LWKey.FillColor && e.getComponent() == this.map) {
            setBackground(this.map.getFillColor());
        }
        
        if (e.getKey() == LWKey.RepaintComponent) {
            Rectangle r = mapToScreenRect(e.getComponent().getBounds());
            super.paintImmediately(r);
            //super.repaint(0,r.x,r.y,r.width,r.height);            
        } else if (OPTIMIZED_REPAINT == false) {
            repaint();
        } else {
            if (paintedSelectionBounds != null) {
                // this will handle any size shrinkages -- old selection bounds
                // will still include the old size (this depends on fact that
                // we can only change the properties of a selected component)
                RR(paintedSelectionBounds);
            }
            if (e.getComponents() != null) {
                // todo: more than one component is in this event (e.g., it's a group add/remove)
                // for full repaint optimization, will want to repaint the bounds of all those children.
                repaint();
            } else {
                repaintMapRegionAdjusted(e.getComponent().getBounds());
            }
        }
    }
    
    /**
     * [TODO: changed] By default, add all nodes hit by this box to a list for doing selection.
     * If NO nodes are in the list, search for links within the region
     * instead.  Of @param onlyLinks is true, only search for links.
     */
    private java.util.List computeSelection(Rectangle2D mapRect, boolean onlyLinks) {
        java.util.List hits = new java.util.ArrayList();
        java.util.Iterator i = getMap().getChildIterator();
        // todo: if want nested children to get seleced, will need a descending iterator
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            
            if (!c.isDrawn())
                continue;
            
            boolean isLink = c instanceof LWLink;
            //if (!onlyLinks && isLink) // DISABLED IGNORING OF LINKS FOR NOW
            //  continue;
            if (onlyLinks && !isLink)
                continue;
            if (c.intersects(mapRect))
                hits.add(c);
        }
        if (onlyLinks)
            return hits;
        
        // if found nothing but links, now grab the links
        if (hits.size() == 0) {
            i = getMap().getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (!(c instanceof LWLink))
                    continue;
                if (c.intersects(mapRect))
                    hits.add(c);
            }
        }
        return hits;
    }
    
    // TODO: consolodate this into a single LWContainer tree descent
    // code -- allow for traversals that can hit a point (for clicks)
    // or a region (for selection) and handle all the hidden/filter
    // cases, as well as allowing for only selecting certian types
    // (for various selection types, including new ones like select
    // children, etc) -- the traversal is essentially dynamic search,
    // and if we're heavy duty enough even the filter code could use
    // it (tho performance may be an issue at that point).  (So, we
    // might have a "Traversal" object that does the search).
    
    private java.util.List computeSelection(Rectangle2D mapRect, Class selectionType) {
        java.util.List hits = new java.util.ArrayList();
        java.util.Iterator i = getMap().getChildIterator();
        // todo: if want nested children to get seleced, will need a descending iterator
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            
            if (!c.isDrawn())
                continue;
            
            if (selectionType != null && !selectionType.isInstance(c))
                continue;
            
            if (c.intersects(mapRect))
                hits.add(c);
        }
        return hits;
    }
    
    
    public LWComponent findClosestEdge(java.util.List hits, float x, float y) {
        return findClosest(hits, x, y, true);
    }
    
    /*
    public LWComponent findClosestCenter(java.util.List hits, float x, float y)
    {
        return findClosest(hits, x, y, false);
        }*/
    
    // todo: we probably need to abandon this whole closest thing, which was neat,
    // and just go for the cleaner, more traditional layer approach (uppermost
    // layer is always hit first).
    // also, architecturally, does this belong somewhere else?
    protected LWComponent findClosest(java.util.List hits, float x, float y, boolean toEdge) {
        if (hits.size() == 1)
            return (LWComponent) hits.get(0);
        else if (hits.size() == 0)
            return null;
        
        java.util.Iterator i;
        /*
        java.util.List topLayer = new java.util.ArrayList();
        if (!toEdge) {
            // scale is a proxy for the layers created by parent/child relationships
            // todo: perhaps do real layering, or have parent handle hit detection...
            float smallestScale = 99f;
            i = hits.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.getLayer() < smallestScale)
                    smallestScale = c.getLayer();
            }
            i = hits.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.getLayer() == smallestScale)
                    topLayer.add(c);
            }
            if (topLayer.size() == 1)
                return (LWComponent) topLayer.get(0);
        } else {
            topLayer = hits;
            }*/
        
        float shortest = Float.MAX_VALUE;
        float distance;
        LWComponent closest = null;
        //i = topLayer.iterator();
        i = hits.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (toEdge)
                distance = c.distanceToEdgeSq(x, y);
            else
                distance = c.distanceToCenterSq(x, y);
            if (distance < shortest) {
                shortest = distance;
                closest = c;
            }
        }
        return closest;
    }
    
    
    
    void setIndicated(LWComponent c) {
        if (indication != c) {
            clearIndicated();
            indication = c;
            if (DEBUG.PARENTING) out("indication  set  to " + c);
            //c.setIndicated(true);
            if (indication.getStrokeWidth() < STROKE_INDICATION.getLineWidth())
                repaintMapRegionGrown(indication.getBounds(), STROKE_INDICATION.getLineWidth());
            else
                repaintMapRegion(indication.getBounds());
        }
    }
    void clearIndicated() {
        if (indication != null) {
            //indication.setIndicated(false);
            if (indication.getStrokeWidth() < STROKE_INDICATION.getLineWidth())
                repaintMapRegionGrown(indication.getBounds(), STROKE_INDICATION.getLineWidth());
            else
                repaintMapRegion(indication.getBounds());
            if (DEBUG.PARENTING) out("clearing indication " + indication);
            indication = null;
        }
    }
    LWComponent getIndication() { return indication; }
    
    private Timer rolloverTimer = new Timer();
    private TimerTask rolloverTask = null;
    private void runRolloverTask() {
        //System.out.println("task run " + this);
        float mapX = screenToMapX(lastMouseX);
        float mapY = screenToMapY(lastMouseY);
        // use deepest to penetrate into groups
        LWComponent hit = getMap().findDeepestChildAt(mapX, mapY);
        //LWComponent hit = getMap().findChildAt(mapX, mapY);
        if (DEBUG.ROLLOVER) System.out.println("RolloverTask: hit=" + hit);
        //if (hit != null && VueSelection.size() <= 1)
        if (hit != null)
            setRollover(hit);
        else
            clearRollover();
        
        rolloverTask = null;
    }
    
    class RolloverTask extends TimerTask {
        public void run() {
            runRolloverTask();
        }
    }
    
    private float mZoomoverOldScale;
    private Point2D mZoomoverOldLoc = null;
    void setRollover(LWComponent c) {
        //if (rollover != c && (c instanceof LWNode || c instanceof LWLink)) {
        // link labels need more work to be zoomable
        if (rollover != c && (c instanceof LWNode)) {
            clearRollover();
            // for moment rollover is really setTemporaryZoom
            //rollover = c;
            //c.setRollover(true);
            mZoomoverOldScale = c.getScale();
            
            double curZoom = getZoomFactor();
            
            //double newScale = mZoomoverOldScale / curZoom;
            double newScale = 1.0 / curZoom;
            
            //if (newScale < 1.0) newScale = 1.0;
            
            //if (true||mZoomoverOldScale != 1f) {
            if (newScale > mZoomoverOldScale &&
            newScale - mZoomoverOldScale > RolloverMinZoomDeltaTrigger) {
                //c.setScale(1f);
                rollover = c;
                if (DEBUG.ROLLOVER) System.out.println("setRollover: " + c);
                c.setRollover(true);
                c.setZoomedFocus(true);
                if (false&&c instanceof LWNode) {
                    // center the zoomed node on it's original center
                    mZoomoverOldLoc = c.getLocation();
                    Point2D oldCenter = c.getCenterPoint();
                    c.setScale((float)newScale);
                    c.setCenterAtQuietly(oldCenter);
                } else
                    c.setScale((float)newScale);
                
                repaintMapRegion(rollover.getBounds());
            }
        }
    }
    void clearRollover() {
        if (rollover != null) {
            if (DEBUG.ROLLOVER) System.out.println("clrRollover: " + rollover);
            if (rolloverTask != null) {
                rolloverTask.cancel();
                rolloverTask = null;
            }
            Rectangle2D bigBounds = rollover.getBounds();
            rollover.setRollover(false);
            rollover.setZoomedFocus(false);
            if (true||mZoomoverOldScale != 1f) {
                
                // If deleted, don't put scale back or will throw
                // zombie event exception (should be okay to leave
                // scale in intermediate state on deleted node -- on
                // restore it should have it's scale set back thru
                // reparenting... if not, we'll need to clear rollover
                // on nodes b4 they're deleted, or allow the setScale
                // on a deleted node in LWComponent.
                
                if (!rollover.isDeleted())
                    rollover.setScale(mZoomoverOldScale);
                
                //if (rollover.getParent() instanceof LWNode)
                // have the parent put it back in place
                //rollover.getParent().layoutChildren();
                //else
                
                // todo? also need to do this setLocation quietly: if they
                // move mouse back and forth tween two link endpoints
                // when no delay is on (easier to see in big curved link)
                // we're seeing the connection point change (still seeing this?)
                if (mZoomoverOldLoc != null) {
                    rollover.setLocation(mZoomoverOldLoc);
                    mZoomoverOldLoc = null;
                }
            }
            repaintMapRegion(bigBounds);
            rollover = null;
        }
    }
    
    private static JComponent sTipComponent;
    private static Popup sTipWindow;
    private static LWComponent sMouseOver;
    
    /**
     * Pop a tool-tip near the given LWComponent.
     *
     * @param pJComponent - the JComponent to display in the tool-tip window
     * @param pAvoidRegion - the region to avoid (usually LWComponent bounds)
     * @param pTipRegion - the region, in map coords, that triggered this tool-tip
     */
    //    void setTip(LWComponent pLWC, JComponent pJComponent, Rectangle2D pTipRegion)
    void setTip(JComponent pJComponent, Rectangle2D pAvoidRegion, Rectangle2D pTipRegion) {
        if (pJComponent != sTipComponent && pJComponent != null) {
            
            if (sTipWindow != null)
                sTipWindow.hide();
            
            // since we're not using the regular tool-tip code, just the swing pop-up
            // factory, we have to set these properties ourselves:
            pJComponent.setOpaque(true);
            pJComponent.setBackground(COLOR_TOOLTIP);
            pJComponent.setBorder(javax.swing.border.LineBorder.createBlackLineBorder());
            
            //c.setIcon(new LineIcon(10,10, Color.red, null));//test -- icons w/tex work
            //System.out.println("    size="+c.getSize());
            //System.out.println("prefsize="+c.getPreferredSize());
            //System.out.println(" minsize="+c.getMinimumSize());
            
            //------------------------------------------------------------------
            // PLACE THE TOOL-TIP POP-UP WINDOW
            //
            // Try left of component first, then top, then right
            //------------------------------------------------------------------
            
            // always add the tip region to the avoid region
            // (need for links, and for nodes in case icon somehow outside bounds)
            Rectangle2D.union(pTipRegion, pAvoidRegion, pAvoidRegion);
            // For the total avoid region, limit to what's visible in the window,
            // as we never to "avoid" anything that's off-screen (not visible in the viewer).
            
            //Rectangle viewer = new Rectangle(0,0, getVisibleWidth(), getVisibleHeight()); // FIXME: SCROLLBARS (what's 0,0??)
            Rectangle viewer = getVisibleBounds();
            Box avoid = new Box(viewer.intersection(mapToScreenRect(pAvoidRegion)));
            Box trigger = new Box(mapToScreenRect(pTipRegion));
            
            SwingUtilities.convertPointToScreen(avoid.ul, this);
            SwingUtilities.convertPointToScreen(avoid.lr, this);
            SwingUtilities.convertPointToScreen(trigger.ul, this);
            //SwingUtilities.convertPointToScreen(trigger.lr, this); // unused
            
            Dimension tip = pJComponent.getPreferredSize();
            
            // Default placement starts from left of component,
            // at same height as the rollover region that triggered us
            // in the component.
            Point glass = new Point(avoid.ul.x - tip.width,  trigger.ul.y);
            
            if (glass.x < 0) {
                // if would go off left of screen, try placing above the component
                glass.x = avoid.ul.x;
                glass.y = avoid.ul.y - tip.height;
                keepTipOnScreen(glass, tip);
                // if too tall and would then overlap rollover region, move to right of component
                //if (glass.y + tip.height >= placementLeft.y) {
                // if too tall and would then overlap component, move to right of component
                if (glass.y + tip.height > avoid.ul.y) {
                    glass.x = avoid.lr.x;
                    glass.y = trigger.ul.y;
                }
                // todo: consider moving tall tips from tip to right
                // of component -- looks ugly having all that on top.
                
                // todo: consider a 2nd pass to ensure not overlapping
                // the rollover region, to prevent window-exit/enter loop.
                // (flashes the rollover till mouse moved away)
            }
            keepTipOnScreen(glass, tip);
            
            // todo java bug: there are some java bugs, perhaps in the
            // Popup caching code (happens on PC & Mac both), where
            // the first time a pop-up appears (actually only seeing
            // with tall JTextArea's), it's height is truncated.
            // Sometimes even first 1 or 2 times it appears!  If
            // intolerable, just implement our own windows and keep
            // them around as a caching scheme -- will use alot more
            // memory but should work (use WeakReferences to help)
            
            PopupFactory popupFactory = PopupFactory.getSharedInstance();
            sTipWindow = popupFactory.getPopup(this, pJComponent, glass.x, glass.y);
            sTipWindow.show();
            sTipComponent = pJComponent;
        }
        
    }
    
    private void keepTipOnScreen(Point glass, Dimension tip) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        // if would go off bottom, move up
        if (glass.y + tip.height >= screen.height)
            glass.y = screen.height - (tip.height + 1);
        // if would go off top, move back down
        if (glass.y < 0)
            glass.y = 0;
        // if would go off right, move back left
        if (glass.x + tip.width > screen.width)
            glass.x = screen.width - tip.width;
        // if would go off left, just put at left
        if (glass.x < 0)
            glass.x = 0;
    }
    
    void clearTip() {
        sTipComponent = null;
        if (sTipWindow != null) {
            sTipWindow.hide();
            sTipWindow = null;
        }
    }
    
    
    /**
     * Render all the LWComponents on the panel
     */
    // java bug: Do NOT create try and create an axis using Integer.MIN_VALUE or Integer.MAX_VALUE
    // -- this triggers line rendering bugs in PC Java 1.4.1 (W2K) -- same for floats.
    private static final int MinCoord = -10240;
    private static final int MaxCoord = 10240;
    private static final Line2D Xaxis = new Line2D.Float(MinCoord, 0, MaxCoord, 0);
    private static final Line2D Yaxis = new Line2D.Float(0, MinCoord, 0, MaxCoord);
    
    //public boolean isOpaque() {return false;}
    
    private int paints=0;
    private boolean redrawingSelector = false;
    public void paint(Graphics g) {
        long start = 0;
        if (DEBUG.PAINT) {
            System.out.print("paint " + paints + " " + g.getClipBounds()+" "); System.out.flush();
            start = System.currentTimeMillis();
        }
        try {
            // This a special speed optimization for the selector box -- not sure it helps anymore tho...
            if (redrawingSelector && draggedSelectorBox != null && activeTool.supportsXORSelectorDrawing()) {
                redrawSelectorBox((Graphics2D)g);
                redrawingSelector = false;
            } else
                super.paint(g);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("*paint* Exception painting in: " + this);
            System.err.println("*paint* VueSelection: " + VueSelection + ", first=" + VueSelection.first());
            System.err.println("*paint* Graphics: " + g);
            System.err.println("*paint* Graphics transform: " + ((Graphics2D)g).getTransform());
        }
        if (paints == 0) {
            if (inScrollPane)
                adjustCanvasSize(); // need for intial scroll-bar sizes if bigger than viewport on startup
            VUE.invokeAfterAWT(new Runnable() { public void run() { ensureMapVisible(); }});
        }
        if (DEBUG.PAINT) {
            long delta = System.currentTimeMillis() - start;
            long fps = delta > 0 ? 1000/delta : -1;
            System.out.println("paint #" + paints + " " + this + ": "
                               + delta
                               + "ms (" + fps + " fps)");
        }
        paints++;
        RepaintRegion = null;
    }
    
    /**
     * Java Swing JComponent.paintComponent -- paint the map on the map viewer canvas
     */
    //private static final Color rrColor = new Color(208,208,208);
    private static final Color rrColor = Color.yellow;
    
    public void paintComponent(Graphics g)
    {
        //isScaleDraw = false;
        Graphics2D g2 = (Graphics2D) g;
        
        /*
        Rectangle cb = g.getClipBounds();
        //if (DEBUG.PAINT && !OPTIMIZED_REPAINT && (cb.x>0 || cb.y>0))
        //out("paintComponent: clipBounds " + cb);
        */
        
        if (OPTIMIZED_REPAINT) {
            // debug: shows the repaint region
            if (DEBUG.PAINT && (RepaintRegion != null || paintingRegion)) {
                paintingRegion = false;
                g2.setColor(rrColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.black);
                g2.setStroke(STROKE_ONE);
                Rectangle r = g.getClipBounds();
                r.width--;
                r.height--;
                g2.draw(r);
            }
        }
        
        DrawContext dc = new DrawContext(g2, getZoomFactor(), -getOriginX(), -getOriginY(), getVisibleBounds(), true);
        dc.setAntiAlias(DEBUG_ANTI_ALIAS);
        dc.setPrioritizeQuality(DEBUG_RENDER_QUALITY);
        dc.setFractionalFontMetrics(DEBUG_FONT_METRICS);
        dc.disableAntiAlias(DEBUG_ANTI_ALIAS == false);
        dc.setActiveTool(getCurrentTool());
        
        //-------------------------------------------------------
        // adjust GC for pan & zoom
        //-------------------------------------------------------
        
        //setScaleDraw(g2);
        dc.setMapDrawing();

        //-------------------------------------------------------
        // DRAW THE MAP
        //
        // The active tool draws the map.  Most will use the default
        // handleDraw of VueTtool, which fills the background and
        // then just draws the map. Some, like PresentationTool,
        // may do something dramatically different.
        //
        // That will draw all the nodes, links, etc.  LWContainer's
        // such as LWNode's & LWGroup's are responsible for painting
        // their children, etc down the line.
        //-------------------------------------------------------
        
        activeTool.handleDraw(dc, this.map);
        
        dc.setMapDrawing();
        if (DEBUG_SHOW_ORIGIN) {
            out("DRAWING ORIGIN");
            // why isn't this working anymore?  was just after fill, but
            // now that tool does fill, we have to do on top, but can't
            // see it...
            g2.setStroke(STROKE_ONE);
            g2.setColor(Color.lightGray);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
            if (mZoomFactor >= 6.0) {
                dc.setAbsoluteStroke(1);
                g2.setColor(Color.black);
                g2.draw(Xaxis);
                g2.draw(Yaxis);
            }
        }
        
        /*
        if (dragComponent != null) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            dragComponent.draw(g2);
        }
         */
        
        /*
        if (true||!VueUtil.isMacPlatform()) // try aa selection on mac for now (todo)
            //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);
            dc.setAntiAlias(true);
        */
        
        //-------------------------------------------------------
        // DRAW THE SELECTION DECORATIONS - if anything is selected
        //-------------------------------------------------------
        
        //if (VueSelection != null && !VueSelection.isEmpty() && activeTool != PathwayTool)
        // todo: currently, the selection is application wide, and has no idea what map
        // it's contents come from, so it's theoretically possible for us to be drawing
        // a selection of a component that's actually from another map.  Maybe have
        // a per-map selection (there is a selection bit in LWComponents after all)
        // We currently prevent this by setting local VueSelection to null if we're
        // not the active map, but if we miss doing that for any reason...
        if (VueSelection != null && !VueSelection.isEmpty() && activeTool.supportsResizeControls())
            drawSelection(dc);
        else
            resizeControl.active = false;

        //-------------------------------------------------------
        // DRAW THE CURRENT INDICATION, if any (for targeting during drags)
        //-------------------------------------------------------

        if (indication != null)
            drawIndication(dc);

        dc.setRawDrawing();
        //setRawDraw(dc.g);
        
        //-------------------------------------------------------
        // draw the dragged selector box
        //-------------------------------------------------------
        
        //if (draggedSelectorBox != null && activeTool.supportsDraggedSelector(null)) {
        if (draggedSelectorBox != null) {
            // todo: box should already be null of tool doesn't support selector
            drawSelectorBox(g2, draggedSelectorBox);
            //if (VueSelection != null && !VueSelection.isEmpty())
            //    new Throwable("selection box while selection visible").printStackTrace();
            // totally reasonable if doing a shift-drag for SELECTION TOGGLE
        }
        
        if (DEBUG.VIEWER) {
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.drawLine(_mouse.x,_mouse.y, _mouse.x+1,_mouse.y+1);
            
            int iX = (int) (screenToMapX(_mouse.x) * 100);
            int iY = (int) (screenToMapY(_mouse.y) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            Point2D mapCoords = new Point2D.Float(mapX, mapY);
            Point canvas = getLocation();
            Point2D screen = new Point2D.Float(_mouse.x + canvas.x, _mouse.y + canvas.y);
                                                     
            
            g2.setFont(VueConstants.FixedFont);
            int x = -getX() + 10;
            int y = -getY();
            //g2.drawString("screen(" + mouse.x + "," +  mouse.y + ")", 10, y+=15);
            if (true) {
                g2.drawString("     origin at: " + out(getOriginLocation()), x, y+=15);
                g2.drawString("     map mouse: " + out(mapCoords), x, y+=15);
                g2.drawString("  canvas mouse: " + out(_mouse), x, y+=15);
                g2.drawString(" ~screen mouse: " + out(screen), x, y+=15);
                /*if (inScrollPane){
                Point extent = viewportToCanvasPoint(mouse);
                Point2D map = extentToMapPoint(extent);
                g2.drawString("  extent point: " + out(extent), x, y+=15);
                g2.drawString("     map point: " + out(map), x, y+=15);
                }*/
                g2.drawString("canvas-location " + out(canvas), x, y+= 15);
                if (inScrollPane){
                g2.drawString("viewport----pos " + out(mViewport.getViewPosition()), x, y+=15);
                }
                g2.drawString("map-canvas-size " + out(mapToScreenDim(getMap().getBounds())), x, y+=15);
                g2.drawString("map-canvas-adju " + out(mapToScreenDim(getContentBounds())), x, y+=15);
                g2.drawString("    canvas-size " + out(getSize()), x, y+=15);
            }
            if (inScrollPane) {
                g2.drawString("  viewport-size " + out(mViewport.getSize()), x, y+=15);
            }
            g2.drawString("zoom " + getZoomFactor(), x, y+=15);
            g2.drawString("anitAlias " + DEBUG_ANTI_ALIAS, x, y+=15);
            g2.drawString("renderQuality " + DEBUG_RENDER_QUALITY, x, y+=15);
            g2.drawString("fractionalMetrics " + DEBUG_FONT_METRICS, x, y+=15);
            //g2.drawString("findParent " + !DEBUG_FINDPARENT_OFF, x, y+=15);
            g2.drawString("optimizedRepaint " + OPTIMIZED_REPAINT, x, y+=15);

            Point2D center = getVisibleCenter();
            dc.setAbsoluteStroke(1);
            // easily gets lost when way zoomed in because coords > MaxCoord
            //g2.draw(new Line2D.Double(center.getX(), MinCoord, center.getX(), MaxCoord));
            //g2.draw(new Line2D.Double(MinCoord, center.getY(), MaxCoord, center.getY());
            g2.drawLine(-99999, (int) Math.round(center.getY()), 99999, (int) Math.round(center.getY()));
            g2.drawLine((int) Math.round(center.getX()), -99999, (int) Math.round(center.getX()), 99999);
        }

        /*
        if (DEBUG_SHOW_ORIGIN && mZoomFactor >= 6.0) {
            //g2.setComposite(java.awt.AlphaComposite.Xor);
            g2.translate(-getOriginX(), -getOriginY());
            g2.setStroke(STROKE_ONE);
            g2.setColor(Color.black);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }
        */
        
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_ON);
        dc.setAntiAlias(true);
        
        /*
        if (false&&mTipMessage != null) {
            g2.setFont(FONT_MEDIUM);
            TextRow msg = new TextRow(mTipMessage, g2);
            int x = mTipPoint.x - (int) msg.getWidth();
            int y = mTipPoint.y - (int) (msg.getHeight() / 2f);
            x -= 2;
            if (VueUtil.isMacPlatform())
                g2.setColor(COLOR_TOOLTIP);
            else
                g2.setColor(SystemColor.info);
            int p=3;
            g2.fillRect(x-p, y-p, (int)msg.getWidth()+p*2, (int)msg.getHeight()+p*2-1);
            g2.setColor(Color.black);
            g2.setStroke(STROKE_ONE);
            g2.drawRect(x-p, y-p, (int)msg.getWidth()+p*2, (int)msg.getHeight()+p*2-1);
            g2.setColor(SystemColor.infoText);
            msg.draw(x, y);
        }
         */
        
        //setOpaque(false);
        if (activeTextEdit != null)     // This is a real Swing JComponent
            super.paintChildren(g2);
        //setOpaque(true);
    }
    
    /** This paintChildren is a no-op.  super.paint() will call this,
     * and we want it to do nothing because we need to invoke this
     * ourself at a time later than it normally would (we call
     * super.paintChildren directly, only if there is an activeTextEdit,
     * at the bottom of paintComponent()).
     */
    protected void paintChildren(Graphics g) {}
    
    /** overriden only to catch when the activeTextEdit is being
     * removed from the panel */
    public void remove(Component c) {
        try {
            super.remove(c);
        } finally {
            if (c == activeTextEdit) {
                activeTextEdit = null;
                try {
                    repaint();
                    if (VUE.getActiveViewer() == this)
                        requestFocus();
                } finally {
                    VueAction.setAllIgnored(false);
                }
            }
        }
    }
    
    void cancelLabelEdit() {
        if (activeTextEdit != null)
            remove(activeTextEdit);
    }
    
    boolean isEditingLabel() {
        return activeTextEdit != null;
    }
    
    /**
     * Enable an interactive label edit box (TextBox) for the given LWC.
     * Only one of these should be active at a time.
     *
     * Important: This actually add's the component to the Container
     * (MapViewer) in order to get events (key, mouse, etc).
     * super.paintChildren is called in MapViewer.paintComponent only
     * to handle the case where a Component like this is active on the
     * MapViewer panel.  Note that this component only simulates zoom
     * by scaling it's font, so we must not zoom the panel while this
     * component is active, and other actions are probably not very
     * safe, thus, we ignore all action events while this is active.
     * When the edit is done (determined via focus loss) the Component
     * is removed from the panel and returns to being drawn through
     * our own LWC draw hierarchy.
     *
     * @see tufts.vue.TextBox
     */
    
    void activateLabelEdit(LWComponent lwc) {
        if (activeTextEdit != null && activeTextEdit.getLWC() == lwc)
            return;
        if (!lwc.supportsUserLabel())
            return;
        if (activeTextEdit != null)
            remove(activeTextEdit);
        // todo robust: make sure can never accidentally happen on a
        // closed map viewer, or all actions will go off and never
        // come back on again, because the textbox will never get
        // focus so it can lose it and turn them back on.
        VueAction.setAllIgnored(true);
        activeTextEdit = lwc.getLabelBox();
        activeTextEdit.saveCurrentText();
        if (activeTextEdit.getText().length() < 1)
            activeTextEdit.setText("label");
        
        // todo: this is a tad off at high scales...
        float cx = lwc.getLabelX();
        float cy = lwc.getLabelY();
        //cx--; // to compensate for line border inset?
        //cy--; // to compensate for line border inset?
        // todo: if is child (scaled) node, this location
        // is wrong -- it's shifted down/right
        activeTextEdit.setLocation(mapToScreenX(cx), mapToScreenY(cy)-1);
        
        activeTextEdit.selectAll();
        add(activeTextEdit);
        activeTextEdit.requestFocus();
    }
    
    private void drawSelectorBox(Graphics2D g2, Rectangle r) {
        // Setting XOR mode before setting the stroke actually
        // changes the behaviour of what happens on the painted-over
        // GC, and what happens appears pretty unpredicatable, thus
        // I think XOR drawing for speed is no longer viable --
        // Both pc's AND mac's now show garbage in GC sometimes also.
        // Note that is POSSIBLE to get this do something useful
        // on the Mac, except that it fills the whole selector region
        // instead draw's bounds, which doesn't really look that bad,
        // and actually looks great when you use a color other than
        // gray, however we can't predict how to get that working...
        //g2.setXORMode(COLOR_SELECTION_DRAG);
        //g2.setStroke(STROKE_SELECTION_DYNAMIC);
        //activeTool.drawSelector(g2, r);
        
        // todo opt: would this be any faster done on a glass pane?
        g2.setStroke(STROKE_SELECTION_DYNAMIC);
        if (activeTool.supportsXORSelectorDrawing())
            g2.setXORMode(COLOR_SELECTION_DRAG);// using XOR may also be working around below clip-edge bug
        else
            g2.setColor(COLOR_SELECTION_DRAG);
        activeTool.drawSelector(g2, r);
    }
    
    /*
    // redraw the selection box being dragged by the user
    // (erase old box, draw new box)
    private void redrawSelectorBox_OLD(Graphics2D g2)
    {
        //if (DEBUG.PAINT) System.out.println(g2);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);
        g2.setXORMode(COLOR_SELECTION_DRAG);
        g2.setStroke(STROKE_SELECTION_DYNAMIC);
        // first, erase last selector box if it was there (XOR redraw = undo)
        if (lastPaintedSelectorBox != null)
            g2.draw(lastPaintedSelectorBox);
        // now, draw the new selector box
        if (draggedSelectorBox == null)
            throw new IllegalStateException("null selectorBox!");
        g2.draw(draggedSelectorBox);
        lastPaintedSelectorBox = new Rectangle(draggedSelectorBox);
    }
     */
     
    private void redrawSelectorBox(Graphics2D g2)
    {
        //throw new UnsupportedOperationException("XOR redraw no longer supported");
        //if (DEBUG.PAINT) System.out.println(g2);
        g2.setStroke(STROKE_SELECTION_DYNAMIC);
     
        if (activeTool.supportsXORSelectorDrawing()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);
            g2.setXORMode(COLOR_SELECTION_DRAG);
            // In XOR mode, first erase last selector box if it was there (XOR redraw over same == undo)
            if (lastPaintedSelectorBox != null)
                activeTool.drawSelector(g2, lastPaintedSelectorBox);
        }
     
        // now, draw the new selector box
        if (draggedSelectorBox == null)
            throw new IllegalStateException("null selectorBox!");
        activeTool.drawSelector(g2, draggedSelectorBox);
        lastPaintedSelectorBox = new Rectangle(draggedSelectorBox); // for XOR mode: save to erase
    }
    
    /* Java/JVM 1.4.1 PC (Win32) Graphics Bugs
     
    #1: bottom edge clip-region STROKE ERASE BUG
    #2: clip-region (top edge?) TEXT WIGGLE BUG
     
    Can only see these bugs with repaint opt turned on -- where a clip
    region smaller than the whole panel is used during painting.
     
    #1 appears to go away when using XOR erase/redraw of selector box
    (currently a mac only option).
     
    Diagnosis 4: XOR selector erase/redraw seems to be a workaround
    for #1.  Can still reliably produce using below trigger method
    plus dragging a LINKED node with repaint optimization on -- watch
    what happens to links as the bottom edge of the clip region passes
    over them.  ANOTHER CLUE: unlinked nodes ("simple" clip region)
    don't cause it, but a linked node, generating a compound repaint
    region during optimized repaint, is where it's happening.  This
    explains why it did it for some links (those at bottom edge of
    COMPOUND clipping region) and not others (anyone who was
    surrounded in repaint region)
     
    Diagnosis 3: Doesn't seem to happen right away either -- have to
    zoom in/out some and/or pan the map around first Poss requirement:
    change zoom (in worked), then PAN while at the zoom level, then
    zoom back to 100% this seems to do it for the text shifting anyway
    -- shifting of everything takes something else I guess.
     
    Diagnosis 2: doesn't appear to be anti-alias or fractional-metrics
    related for the text, tho switchin AA off stops it when the whole
    node is sometimes slightly streteched or compressed off to the
    right.
     
    Diagnosis 1: pixels seems to subtly shift for SOME nodes as
    they pass in and out of the drag region on PC JVM 1.4 -- doesn't
    depend on region on screen -- actually the node!!  Happens to text
    more often -- somtimes text & strokes.  Happens much more
    frequently at zoom exactly 100%.
     
     */
    
    // todo: move all this code to LWSelection?
    // todo perf: way too much calculation here for every draw: do some on selection change?
    private void drawSelection(DrawContext dc) {
        Graphics2D g2 = dc.g;
        g2.setColor(COLOR_SELECTION);
        //g2.setXORMode(Color.black);
        g2.setStroke(STROKE_SELECTION);
        java.util.Iterator it;
        
        // draw bounding boxes -- still want to bother with this?
        /*
        it = VueSelection.iterator();
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if (!(c instanceof LWLink))
                drawComponentSelectionBox(g2, c);
        }
         */
        
        //-------------------------------------------------------
        // draw ghost shapes
        //-------------------------------------------------------

        //setScaleDraw(g2);
        dc.setMapDrawing();

        it = VueSelection.iterator();
        //g2.setStroke(new BasicStroke((float) (STROKE_HALF.getLineWidth() * mZoomInverse)));
        //g2.setStroke(STROKE_ONE);
        dc.setAbsoluteStroke(0.5);
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if (sDragUnderway || c.getStrokeWidth() == 0 || c instanceof LWLink) {
                // todo: the ideal is to always draw the ghost (not just when
                // dragging) but figure out a way not to uglify the border if
                // it's visible with the blue streak -- may XOR draw to the border
                // color? (or it's inverse)
                //g2.setColor(c.getStrokeColor());
                Shape shape = c.getShape();
                g2.draw(shape);
                if (shape instanceof RectangularPoly2D) {
                    if (((RectangularPoly2D)shape).getSides() > 4) {
                        Ellipse2D inscribed = new Ellipse2D.Float();
                        if (DEBUG.BOXES) {
                            inscribed.setFrame(shape.getBounds());
                            g2.draw(inscribed);
                        }
                        inscribed.setFrame(c.getX(),
                        c.getY()+(c.getHeight()-c.getWidth())/2,
                        c.getWidth(),
                        c.getWidth());
                        g2.draw(inscribed);
                    }
                }
            }
        }
        
        dc.setRawDrawing();
        //setRawDraw(g2);
        
        g2.setStroke(STROKE_SELECTION);
        //g2.setComposite(AlphaComposite.Src);
        g2.setColor(COLOR_SELECTION);
        
        //if (!VueSelection.isEmpty() && (!sDragUnderway || isDraggingSelectorBox)) {
        
        // todo opt?: don't recompute bounds here every paint ---
        // can cache in draggedSelectionGroup (but what if underlying objects resize?)
        Rectangle2D selectionBounds = VueSelection.getBounds();
        /*
          bounds cache hack
          if (VueSelection.size() == 1)
          selectionBounds = VueSelection.first().getBounds();
          else
          selectionBounds = draggedSelectionGroup.getBounds();
         */
        //System.out.println("mapSelectionBounds="+selectionBounds);
        Rectangle2D.Float mapSelectionBounds = mapToScreenRect2D(selectionBounds);
        paintedSelectionBounds = mapToScreenRect(selectionBounds);
        growForSelection(paintedSelectionBounds);
        //System.out.println("screenSelectionBounds="+mapSelectionBounds);
        
        // todo: this check is a hack: need to check if any in selection return true for supportsUserResize
        if (//VueSelection.countTypes(LWNode.class) + VueSelection.countTypes(LWImage.class) <= 0
            //||
            //(VueSelection.size() == 1 && VueSelection.first() instanceof LWNode && ((LWNode)VueSelection.first()).isTextNode())
            VueSelection.allOfType(LWLink.class)
            ) {
            // todo: also alow groups to resize (make selected group resize
            // re-usable for a group -- perhaps move to LWGroup itself &
            // also use draggedSelectionGroup for this?)
            if (DEBUG.BOXES || VueSelection.size() > 1 /*|| !VueSelection.allOfType(LWLink.class)*/)
                g2.draw(mapSelectionBounds);
            // no resize handles if only links or groups
            resizeControl.active = false;
        } else {
            if (VueSelection.size() > 1) {
                g2.draw(mapSelectionBounds);
            } else {
                // Only one in selection:
                // SPECIAL CASE to keep control handles out of way of node icons
                // when node is scaled way down:
                if (VueSelection.first().getScale() < 0.6) {
                    final float grow = SelectionHandleSize/2;
                    mapSelectionBounds.x -= grow;
                    mapSelectionBounds.y -= grow;
                    // for purposes here, don't need to make bigger at right,
                    // or even do the height at all, but lets at least keep it
                    // symmetrical around the node or will look off.
                    mapSelectionBounds.width += grow*2;
                    mapSelectionBounds.height += grow*2;
                }
            }
            //if (!sDragUnderway)
            //drawSelectionBoxHandles(g2, mapSelectionBounds);
            
            boolean groupies = VueSelection.allHaveSameParentOfType(LWGroup.class);
            
            setSelectionBoxResizeHandles(mapSelectionBounds);
            resizeControl.active = true;
            for (int i = 0; i < resizeControl.handles.length; i++) {
                LWSelection.ControlPoint cp = resizeControl.handles[i];
                drawSelectionHandleCentered(g2, cp.x, cp.y,
                                            groupies ? COLOR_SELECTION : cp.getColor());
            }
        }
        
        //if (sDragUnderway) return;
        
        //-------------------------------------------------------
        // draw LWComponent requested control points
        //-------------------------------------------------------
        
        //if (activeTool != PathwayTool) {
        it = VueSelection.getControlListeners().iterator();
        while (it.hasNext()) {
            LWSelection.ControlListener cl = (LWSelection.ControlListener) it.next();
            LWSelection.ControlPoint[] ctrlPoints = cl.getControlPoints();
            for (int i = 0; i < ctrlPoints.length; i++) {
                LWSelection.ControlPoint cp = ctrlPoints[i];
                if (cp == null)
                    continue;
                drawSelectionHandleCentered(g2,
                                            mapToScreenX(cp.x),
                                            mapToScreenY(cp.y),
                                            cp.getColor());
            }
        }
        //}
        
        if (DEBUG.VIEWER) resizeControl.draw(dc); // debug
        
        /*
        it = VueSelection.iterator();
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
         
            //if (!(c instanceof LWLink))
            //  drawComponentSelectionBox(g2, c);
         
            if (c instanceof LWSelection.ControlListener) {
                LWSelection.ControlListener cl = (LWSelection.ControlListener) c;
                //Point2D.Float[] ctrlPoints = cl.getControlPoints();
                LWSelection.ControlPoint[] ctrlPoints = cl.getControlPoints();
                for (int i = 0; i < ctrlPoints.length; i++) {
                    //Point2D.Float cp = ctrlPoints[i];
                    LWSelection.ControlPoint cp = ctrlPoints[i];
                    if (cp == null)
                        continue;
                    drawSelectionHandleCentered(g2,
                                                mapToScreenX(cp.x),
                                                mapToScreenY(cp.y),
                                                cp.getColor());
                }
            }
        }
         */
    }

    // Helper methods for keeping us scaled the way we want.
    // Assumes we only ever work with a single GC per cycle.
    // todo: cleaner: do with saving & restoring current transform
    /*
    private boolean isScaleDraw = false;
    private AffineTransform savedTransform;
    private void setScaleDraw(Graphics2D g) {
        if (!isScaleDraw) {
            savedTransform = g.getTransform();
            g.translate(-getOriginX(), -getOriginY());
            g.scale(mZoomFactor, mZoomFactor);
            isScaleDraw = true;
        }
    }
    private void setRawDraw(Graphics2D g) {
        if (isScaleDraw) {
            g.setTransform(savedTransform);
            //g.scale(1.0/mZoomFactor, 1.0/mZoomFactor);
            //g.translate(getOriginX(), getOriginY());
            isScaleDraw = false;
        }
    }
    */

    private void drawIndication(DrawContext dc)
    {
        if (indication == null)
            return;
        
        dc.setMapDrawing();
        //setScaleDraw(dc.g);
        double minStroke = STROKE_SELECTION.getLineWidth() * 2 * mZoomInverse;
        if (indication.getStrokeWidth() > minStroke)
            dc.g.setStroke(new BasicStroke(indication.getStrokeWidth()));
        else
            dc.g.setStroke(new BasicStroke((float) minStroke));
        //dc.g.setStroke(new BasicStroke((float) (STROKE_SELECTION.getLineWidth() * 2 * mZoomInverse)));
        dc.g.setColor(COLOR_INDICATION);
        dc.g.draw(indication.getShape());
        dc.g.setColor(COLOR_SELECTION);
        // really, only the dragComponent should be transparent...
        //dc2.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        //indication.draw(dc2);
        //g2.setComposite(AlphaComposite.Src);
    }
    
    
    // exterior drawn box will be 1 pixel bigger
    static final int SelectionHandleSize = VueResources.getInt("mapViewer.selection.handleSize"); // fill size
    static final int CHS = VueResources.getInt("mapViewer.selection.componentHandleSize"); // fill size
    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final Rectangle2D ComponentHandle = new Rectangle2D.Float(0,0,0,0);
    //static final int SelectionMargin = SelectionHandleSize > SelectionStrokeWidth/2 ? SelectionHandleSize : SelectionStrokeWidth/2;
    // can't combine these: one rendered at scale and one not!
    
    private void drawSelectionHandleCentered(Graphics2D g, float x, float y, Color fillColor) {
        x -= SelectionHandleSize/2;
        y -= SelectionHandleSize/2;
        drawSelectionHandle(g, x, y, fillColor);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y) {
        drawSelectionHandle(g, x, y, COLOR_SELECTION_HANDLE);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y, Color fillColor) {
        //x = Math.round(x);
        //y = Math.round(y);
        SelectionHandle.setFrame(x, y, SelectionHandleSize, SelectionHandleSize);
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fill(SelectionHandle);
        }
        // todo: if fillColor == COLOR_SELECTION, then this control point
        // will have poor to no contrast if it's over the selection color --
        // e.g., a link connection point at the edge of node who at the moment
        // happens to be selected and has a border.
        if (!COLOR_SELECTION.equals(fillColor)) {
            g.setColor(COLOR_SELECTION);
            g.draw(SelectionHandle);
        }
    }
    
    static final float sMinSelectEdge = SelectionHandleSize * 2;
    private void setSelectionBoxResizeHandles(Rectangle2D.Float r) {
        // don't let control boxes overlap:
        if (r.width < sMinSelectEdge) {
            r.x -= (sMinSelectEdge - r.width)/2;
            r.width = sMinSelectEdge;
        }
        if (r.height < sMinSelectEdge) {
            r.y -= (sMinSelectEdge - r.height)/2;
            r.height = sMinSelectEdge;
        }
        
        // set the 4 corners
        resizeControl.handles[0].setLocation(r.x, r.y);
        resizeControl.handles[2].setLocation(r.x + r.width, r.y);
        resizeControl.handles[4].setLocation(r.x + r.width, r.y + r.height);
        resizeControl.handles[6].setLocation(r.x, r.y + r.height);
        // set the midpoints
        resizeControl.handles[1].setLocation(r.x + r.width/2, r.y);
        resizeControl.handles[3].setLocation(r.x + r.width, r.y + r.height/2);
        resizeControl.handles[5].setLocation(r.x + r.width/2, r.y + r.height);
        resizeControl.handles[7].setLocation(r.x, r.y + r.height/2);
    }
    
    
    /* draw the 8 resize handles for the selection */
    private void old_drawSelectionBoxHandles(Graphics2D g, Rectangle2D.Float r) {
        // offset so are centered on line
        r.x -= SelectionHandleSize/2;
        r.y -= SelectionHandleSize/2;
        
        //r.x = Math.round(r.x);
        //r.y = Math.round(r.y);
        //r.width = Math.round(r.width);
        //r.height = Math.round(r.height);
        //g.draw(r);
        //r.x -= SelectionHandleSize/2;
        //r.y -= SelectionHandleSize/2;
        
        // Draw the four corners
        drawSelectionHandle(g, r.x, r.y);
        drawSelectionHandle(g, r.x, r.y + r.height);
        drawSelectionHandle(g, r.x + r.width, r.y);
        drawSelectionHandle(g, r.x + r.width, r.y + r.height);
        // Draw the midpoints
        drawSelectionHandle(g, r.x + r.width/2, r.y);
        drawSelectionHandle(g, r.x, r.y + r.height/2);
        drawSelectionHandle(g, r.x + r.width, r.y + r.height/2);
        drawSelectionHandle(g, r.x + r.width/2, r.y + r.height);
    }
    
    // todo: if move this to LWComponent as a default, LWLink could
    // override with it's own, and ultimately users of our API could
    // implement their own selection rendering -- tho that would also
    // mean having an api for what happens when they drag the selection,
    // or even how they hit the selection handles in he first place.
    void drawComponentSelectionBox(Graphics2D g, LWComponent c) {
        g.setColor(COLOR_SELECTION);
        Rectangle2D.Float r = mapToScreenRect2D(c.getShapeBounds());
        g.draw(r);
        r.x -= (CHS-1)/2;
        r.y -= (CHS-1)/2;
        if (CHS % 2 == 0) {
            // if box size is even, bias to inside the selection border
            r.height--;
            r.width--;
        }
        ComponentHandle.setFrame(r.x, r.y , CHS, CHS);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x, r.y + r.height, CHS, CHS);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x + r.width, r.y, CHS, CHS);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x + r.width, r.y + r.height, CHS, CHS);
        g.fill(ComponentHandle);
    }
    
    protected void selectionAdd(LWComponent c) {
        VueSelection.add(c);
    }
    protected void selectionAdd(java.util.Iterator i) {
        VueSelection.add(i);
    }
    protected void selectionRemove(LWComponent c) {
        VueSelection.remove(c);
    }
    protected void selectionSet(LWComponent c) {
        VueSelection.setTo(c);
    }
    protected void selectionClear() {
        VueSelection.clear();
    }
    protected void selectionToggle(LWComponent c) {
        if (c.isSelected())
            selectionRemove(c);
        else
            selectionAdd(c);
    }
    protected void selectionToggle(java.util.Iterator i) {
        VueSelection.toggle(i);
    }
    
    private static Map sLinkMenus = new HashMap();
    private JMenu getLinkMenu(String name) {
        Object menu = sLinkMenus.get(name);
        if (menu == null) {
            JMenu linkMenu = new JMenu(name);
            for (int i = 0; i < Actions.LINK_MENU_ACTIONS.length; i++) {
                Action a = Actions.LINK_MENU_ACTIONS[i];
                if (a == null)
                    linkMenu.addSeparator();
                else
                    linkMenu.add(a);
            }
            sLinkMenus.put(name, linkMenu);
            return linkMenu;
        } else {
            return (JMenu) menu;
        }
    }
    
    private static Map sNodeMenus = new HashMap();
    private JMenu getNodeMenu(String name) {
        Object menu = sNodeMenus.get(name);
        if (menu == null) {
            JMenu nodeMenu = new JMenu(name);
            for (int i = 0; i < Actions.NODE_MENU_ACTIONS.length; i++) {
                Action a = Actions.NODE_MENU_ACTIONS[i];
                if (a == null)
                    nodeMenu.addSeparator();
                else
                    nodeMenu.add(a);
            }
            nodeMenu.addSeparator();
            //nodeMenu.add(new JMenuItem("Set shape:")).setEnabled(false);
            nodeMenu.add(new JLabel("   Set shape:"));
            Action[] shapeActions = NodeTool.getShapeSetterActions();
            for (int i = 0; i < shapeActions.length; i++) {
                nodeMenu.add(shapeActions[i]);
            }
            sNodeMenus.put(name, nodeMenu);
            return nodeMenu;
        } else {
            return (JMenu) menu;
        }
    }
    /*
    private static JMenu sArrangeMenu;
    private JMenu getArrangeMenu() {
        if (sArrangeMenu == null)
            sArrangeMenu = buildMenu(new JMenu("Arrange"), Actions.ARRANGE_MENU_ACTIONS);
        return sArrangeMenu;
    }
     */
    
    private static JMenu buildMenu(String name, Action[] actions) {
        return buildMenu(new JMenu(name), actions);
    }
    private static JMenu buildMenu(JMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        return menu;
    }
    
    private JPopupMenu buildMultiSelectionPopup() {
        JPopupMenu m = new JPopupMenu("Multi-Component Menu");
        
        m.add(getNodeMenu("Nodes"));
        m.add(getLinkMenu("Links"));
        m.add(buildMenu("Arrange", Actions.ARRANGE_MENU_ACTIONS));
        m.addSeparator();
        m.add(Actions.Duplicate);
        m.add(Actions.Group);
        m.add(Actions.Ungroup);
        m.addSeparator();
        m.add(Actions.BringToFront);
        m.add(Actions.BringForward);
        m.add(Actions.SendToBack);
        m.add(Actions.SendBackward);
        m.addSeparator();
        m.add(Actions.DeselectAll);
        m.add(Actions.Delete);
        return m;
    }
    
    private static JMenuItem sNodeMenuItem;
    private static JMenuItem sLinkMenuItem;
    private static JMenuItem sUngroupItem;
    private static Component sPathAddItem;
    private static Component sPathRemoveItem;
    private static Component sPathSeparator;
    private JPopupMenu buildSingleSelectionPopup() {
        JPopupMenu m = new JPopupMenu("Component Menu");
        
        sNodeMenuItem = getNodeMenu("Node");
        sLinkMenuItem = getLinkMenu("Link");
        sUngroupItem = new JMenuItem(Actions.Ungroup);
        
        m.add(sNodeMenuItem);
        m.add(sLinkMenuItem);
        m.add(sUngroupItem);
        m.add(Actions.Rename);
        m.add(Actions.Duplicate);
        m.addSeparator();
        m.add(Actions.BringToFront);
        m.add(Actions.BringForward);
        m.add(Actions.SendToBack);
        m.add(Actions.SendBackward);
        m.addSeparator();
        m.add(Actions.DeselectAll);
        m.add(Actions.Delete);
        m.addSeparator();
        sPathAddItem = m.add(Actions.AddPathwayItem);
        sPathRemoveItem = m.add(Actions.RemovePathwayItem);
        sPathSeparator = m.add(new JPopupMenu.Separator());
        m.add(Actions.HierarchyView);
        m.add(buildMenu("Nudge", Actions.ARRANGE_SINGLE_MENU_ACTIONS));
        sAssetMenu = new JMenu("Disseminators");
        // todo: special add-to selection action that adds
        // hitComponent to selection so have way other
        // than shift-click to add to selection (so you
        // can do it all with the mouse)
        m.add(new VueAction("Show Object Inspector") {
                void act() { VUE.objectInspector.setVisible(true); }
            });
        
        return m;
    }
    
    private static JMenu sAssetMenu;
    private static JPopupMenu sSinglePopup;
    private JPopupMenu getSingleSelectionPopup(LWComponent c) {
        if (c == null)
            c = VueSelection.first(); // should be only thing in selection
        
        if (sSinglePopup == null)
            sSinglePopup = buildSingleSelectionPopup();
        
        if (c instanceof LWNode) {
            sNodeMenuItem.setVisible(!((LWNode)c).isTextNode());
            sLinkMenuItem.setVisible(false);
            Actions.HierarchyView.setEnabled(true);
            
            LWNode n = (LWNode) c;
            Resource r = n.getResource();
            if (r != null && r.getType() == Resource.ASSET_FEDORA) {
                Asset a = r == null ? null :((AssetResource)r).getAsset();
                if (a != null && sAssetMenu == null) {
                    buildAssetMenu(a);
                    sSinglePopup.add(sAssetMenu);
                } else if (a != null) {
                    sSinglePopup.remove(sAssetMenu);
                     buildAssetMenu(a);
                    sSinglePopup.add(sAssetMenu);
                } else if (a == null && sAssetMenu != null) {
                    sSinglePopup.remove(sAssetMenu);
                }
            }else {
                    sSinglePopup.remove(sAssetMenu);
            }
            
        } else {
            sLinkMenuItem.setVisible(c instanceof LWLink);
            sNodeMenuItem.setVisible(false);
            Actions.HierarchyView.setEnabled(false);
        }
        
        if (getMap().getPathwayList().getActivePathway() == null) {
            sPathAddItem.setVisible(false);
            sPathRemoveItem.setVisible(false);
            sPathSeparator.setVisible(false);
        } else {
            sPathAddItem.setVisible(true);
            sPathRemoveItem.setVisible(true);
            sPathSeparator.setVisible(true);
        }
        
        if (c instanceof LWGroup) {
            sUngroupItem.setVisible(true);
            sSinglePopup.getComponent(4).setVisible(false); // hide rename
        } else {
            sUngroupItem.setVisible(false);
            sSinglePopup.getComponent(4).setVisible(true); // show rename
        }
        
        return sSinglePopup;
    }

    private void dumpAsset(Asset asset)
        throws osid.dr.DigitalRepositoryException
    {
        System.out.println("DUMP: Asset " + asset.getClass().getName() + "[" + asset + "]"
                           + " displayName=[" + asset.getDisplayName() + "]"
                           + " description=[" + asset.getDescription() + "]"
                           );
        osid.dr.InfoRecordIterator i = asset.getInfoRecords();
        while (i.hasNext()) {
            System.out.print("\t");
            dumpInfoRecord((osid.dr.InfoRecord) i.next());
            //osid.dr.InfoRecord r = (osid.dr.InfoRecord) i.next();
        }
    }

    private void dumpInfoRecord(osid.dr.InfoRecord r)
        throws osid.dr.DigitalRepositoryException
    {
        System.out.println(r);
        osid.dr.InfoFieldIterator i = r.getInfoFields();
        while (i.hasNext()) {
            osid.dr.InfoField f = i.next();
            System.out.println("\t\t" + f);
        }
    }
            
    
    private void buildAssetMenu(Asset asset) {
        sAssetMenu.removeAll();
        osid.dr.InfoRecordIterator i;
        try {
            if (DEBUG.DR) dumpAsset(asset);
            i = asset.getInfoRecordsByInfoStructure(new PID(AssetResource.DISSEMINATION_INFOSTRUCTURE_ID));
            while(i.hasNext()) {
                osid.dr.InfoRecord infoRecord = i.next();
                sAssetMenu.add(FedoraUtils.getFedoraAction(infoRecord,((FedoraObject)asset).getDR()));
            }
        } catch (Exception ex) {
            System.out.println("MapViewer.getAssetMenu"+ex);
            ex.printStackTrace();
        }
    }
    
    private static JPopupMenu sMultiPopup;
    private JPopupMenu getMultiSelectionPopup() {
        if (sMultiPopup == null)
            sMultiPopup = buildMultiSelectionPopup();
        
        /*
        if (VueSelection.allOfType(LWLink.class))
            multiPopup.add(getLinkMenu());
        else
            multiPopup.remove(getLinkMenu());
         */
        
        return sMultiPopup;
    }
    
    private static JPopupMenu sMapPopup;
    private JPopupMenu getMapPopup() {
        if (sMapPopup == null) {
            sMapPopup = new JPopupMenu("Map Menu");
            sMapPopup.addSeparator();
            sMapPopup.add(Actions.NewNode);
            sMapPopup.add(Actions.NewText);
            sMapPopup.addSeparator();
            sMapPopup.add(Actions.ZoomFit);
            sMapPopup.add(Actions.ZoomActual);
            sMapPopup.add(Actions.ToggleFullScreen);
            sMapPopup.addSeparator();
            sMapPopup.add(Actions.SelectAll);
            sMapPopup.add(new VueAction("Show Map Inspector") {
                    void act() { VUE.sMapInspector.setVisible(true); }
                });
        }
        return sMapPopup;
    }
    
    
    
    static final int RIGHT_BUTTON_MASK =
          java.awt.event.InputEvent.BUTTON2_MASK
        | java.awt.event.InputEvent.BUTTON3_MASK;
    static final int ALL_MODIFIER_KEYS_MASK =
          java.awt.event.InputEvent.SHIFT_MASK
        | java.awt.event.InputEvent.CTRL_MASK
        | java.awt.event.InputEvent.META_MASK
        | java.awt.event.InputEvent.ALT_MASK;
    
    
    // toolKeyDown: a key being held down to temporarily activate
    // a particular tool;
    private int toolKeyDown = 0;
    private VueTool toolKeyOldTool;
    private boolean toolKeyReleased = false;
    //private KeyEvent toolKeyEvent = null; // to get at kbd modifiers active at time of keypress
    
    // temporary tool activators (while the key is held down)
    // They require a further mouse action to actually
    // do anythiing.
    static final int KEY_TOOL_PAN   = KeyEvent.VK_SPACE;
    static final int KEY_TOOL_ZOOM  = KeyEvent.VK_BACK_QUOTE;
    static final int KEY_TOOL_LINK = VueUtil.isMacPlatform() ? KeyEvent.VK_ALT : KeyEvent.VK_CONTROL;
    static final int KEY_TOOL_ARROW = KeyEvent.VK_Q;
    // Mac overrides CONTROL-MOUSE to look like right-click (context menu popup) so we can't
    // use CTRL wih mouse drag -- todo: change to ALT for PC too -- might as well be consistent.
    static final int KEY_ABORT_ACTION = KeyEvent.VK_ESCAPE;
    
    
    private void revertTemporaryTool() {
        if (toolKeyDown != 0) {
            toolKeyDown = 0;
            //toolKeyEvent = null;
            toolSelected(toolKeyOldTool); // restore prior cursor
            toolKeyOldTool = null;
        }
    }


    MouseWheelListener getMouseWheelListener() {
        return inputHandler;
    }
    
    // todo: if java ever supports moving an inner class to another file,
    // move the InputHandler out: this file has gotten too big.
    private class InputHandler extends tufts.vue.MouseAdapter
        implements java.awt.event.KeyListener, java.awt.event.MouseWheelListener
    {
        LWComponent dragComponent;//todo: RENAME dragGroup -- make a ControlListener??
        LWSelection.ControlListener dragControl;
        //boolean isDraggingControlHandle = false;
        int dragControlIndex;
        boolean mouseWasDragged = false;
        LWComponent justSelected;    // for between mouse press & click
        boolean hitOnSelectionHandle = false; // we moused-down on a selection handle

        MapViewer viewer; // getting ready to move this to another file.
        InputHandler(MapViewer viewer) {
            this.viewer = viewer;
        }
        
        /**
         * dragStart: screen location (within this java.awt.Container)
         * of mouse-down that started this drag. */
        Point dragStart = new Point();
        
        /**
         * dragOffset: absolute map distance mouse was from the
         * origin of the current dragComponent when the mouse was
         * pressed down. */
        Point2D.Float dragOffset = new Point2D.Float();
        
        private int kk = 0;
        private ToolWindow debugInspector;
        private ToolWindow debugPanner;
        public void keyPressed(KeyEvent e) {
            if (DEBUG.KEYS) out("[" + e.paramString() + "] consumed=" + e.isConsumed());
            
            viewer.clearTip();
            
            // FYI, Java 1.4.1 sends repeat key press events for
            // non-modal keys that are being held down (e.g. not for
            // shift, buf for spacebar)
            
            // Check for temporary tool activation via holding
            // a key down.  Only one can be active at a time,
            // so this is ignored if anything is already set.
            
            // todo: we'll probably want to change this to
            // a general tool-activation scheme, and the active
            // tool class will handle setting the cursor.
            // e.g., dispatchToolKeyPress(e);
            
            int key = e.getKeyCode();
            char keyChar = e.getKeyChar();
            
            /*
            if (key == KeyEvent.VK_F2 && lastSelection instanceof LWNode) {//todo: handle via action only
                Actions.Rename.actionPerformed(new ActionEvent(this, 0, "Rename-via-viewer-key"));
                //activateLabelEdit(lastSelection);
                return;
                }*/
            boolean handled = true;
            
            if (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE) {
                // todo: can't we add this to a keymap for the MapViewer JComponent?
                if (!e.isConsumed())
                    Actions.Delete.fire(this);
            } else if (key == KEY_ABORT_ACTION) {
                if (dragComponent != null) {
                    double oldX = viewer.screenToMapX(dragStart.x) + dragOffset.x;
                    double oldY = viewer.screenToMapY(dragStart.y) + dragOffset.y;
                    dragComponent.setLocation(oldX, oldY);
                    //dragPosition.setLocation(oldX, oldY);
                    dragComponent = null;
                    activeTool.handleDragAbort();
                    mouseWasDragged = false;
                    clearIndicated(); // incase dragging new link
                    // TODO: dragControl not abortable...
                    repaint();
                }
                if (draggedSelectorBox != null) {
                    // cancel any drags
                    draggedSelectorBox = null;
                    isDraggingSelectorBox = false;
                    repaint();
                }
                if (VUE.inFullScreen())
                    VUE.toggleFullScreen();
            } else if (e.isShiftDown() && VueSelection.isEmpty()) {
                // this is mainly for debug.
                     if (key == KeyEvent.VK_UP)    viewer.panScrollRegion( 0,-1);
                else if (key == KeyEvent.VK_DOWN)  viewer.panScrollRegion( 0, 1);
                else if (key == KeyEvent.VK_LEFT)  viewer.panScrollRegion(-1, 0);
                else if (key == KeyEvent.VK_RIGHT) viewer.panScrollRegion( 1, 0);
                else
                    handled = false;
            }
            else if (key == KeyEvent.VK_BACK_SLASH || key == KeyEvent.VK_F11) {
                VUE.toggleFullScreen(e.isShiftDown());
            }
            else if (activeTool.handleKeyPressed(e)) {
                ;
            } else {
                handled = false;
            }

            if (handled) {
                e.consume();
                return;
            }
            
            /*if (VueUtil.isMacPlatform() && toolKeyDown == KEY_TOOL_PAN) {
                // toggle cause mac auto-repeats space-bar screwing everything up
                // todo: is this case only on my G4 kbd or does it happen on
                // USB kbd w/external screen also?

                toolKeyEvent = null;
                setCursor(CURSOR_DEFAULT);
                return;
                }*/
            
            // If any modifier keys down, may be an action command.
            // Is actually okay if a mouse is down while we do this tho.
            if ((e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0 && (!sDragUnderway || isDraggingSelectorBox)) {
                VueTool[] tools =  VueToolbarController.getController().getTools();
                for (int i = 0; i < tools.length; i++) {
                    VueTool tool = tools[i];
                    if (tool.getShortcutKey() == keyChar) {
                        VueToolbarController.getController().setSelectedTool(tool);
                        return;
                    }
                }
            }
            

            if (toolKeyDown == 0 && !isDraggingSelectorBox && !sDragUnderway) {
                // todo: handle via resources
                VueTool tempTool = null;
                if      (key == KEY_TOOL_PAN) tempTool = HandTool;
                else if (key == KEY_TOOL_ZOOM) tempTool = ZoomTool;
                else if (key == KEY_TOOL_LINK) tempTool = LinkTool;
                else if (key == KEY_TOOL_ARROW) tempTool = ArrowTool;
                if (tempTool != null) {
                    toolKeyDown = key;
                    //toolKeyEvent = e;
                    toolKeyOldTool = activeTool;
                    if (tempTool != LinkTool) {
                        // the temporary linktool needs mousepressed before fully selected
                        // because it's CTRL, which is too generally used to change the cursor
                        // for every time we hold it down.
                        toolSelected(tempTool);
                    }
                }
            }
            
            
            //-------------------------------------------------------
            // DEBUGGING
            //-------------------------------------------------------
            
            if (/*e.isShiftDown() &&*/ !e.isControlDown()) {
                char c = e.getKeyChar();
                if ("DEBUG".charAt(kk++) == c) {
                    if (kk == 5) {
                        DEBUG.Enabled = !DEBUG.Enabled;
                        java.awt.Toolkit.getDefaultToolkit().beep();
                        System.out.println("debug: " + DEBUG.Enabled);
                        if (!DEBUG.Enabled) {
                            DEBUG.setAllEnabled(false);
                            repaint();
                        }
                        kk = 0;
                        return;
                    }
                } else
                    kk = 0;
                
                boolean did = true;
                if (!DEBUG.Enabled)
                    did = false;
                else if (c == 'A') {
                    DEBUG_ANTI_ALIAS = !DEBUG_ANTI_ALIAS;
                    if (DEBUG_ANTI_ALIAS)
                        AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;
                    else AA_ON = RenderingHints.VALUE_ANTIALIAS_OFF;
                }
                else if (c == 'B') { DEBUG.BOXES = !DEBUG.BOXES; }
                else if (c == 'C') { DEBUG.CONTAINMENT = !DEBUG.CONTAINMENT; }
                else if (c == 'D') { DEBUG.DYNAMIC_UPDATE = !DEBUG.DYNAMIC_UPDATE; }
                else if (c == 'E') { DEBUG.EVENTS = !DEBUG.EVENTS; }
                else if (c == 'F') { DEBUG.FOCUS = !DEBUG.FOCUS; }
                //else if (c == 'F') { DEBUG_FINDPARENT_OFF = !DEBUG_FINDPARENT_OFF; }
                else if (c == 'I') { DEBUG.IMAGE = !DEBUG.IMAGE; }
                else if (c == 'K') { DEBUG.KEYS = !DEBUG.KEYS; }
                else if (c == 'L') { DEBUG.LAYOUT = !DEBUG.LAYOUT; }
                else if (c == 'M') { DEBUG.MOUSE = !DEBUG.MOUSE; }
                else if (c == 'm') { DEBUG.MARGINS = !DEBUG.MARGINS; }
                else if (c == 'O') { DEBUG_SHOW_ORIGIN = !DEBUG_SHOW_ORIGIN; }
                else if (c == 'P') { DEBUG.PAINT = !DEBUG.PAINT; }
                else if (c == 'Q') { DEBUG_RENDER_QUALITY = !DEBUG_RENDER_QUALITY; }
                else if (c == 'R') { OPTIMIZED_REPAINT = !OPTIMIZED_REPAINT; }
                else if (c == 'r') { DEBUG_TIMER_ROLLOVER = !DEBUG_TIMER_ROLLOVER; }
                else if (c == 'S') { DEBUG.SELECTION = !DEBUG.SELECTION; }
                else if (c == 'T') { DEBUG.TOOL = !DEBUG.TOOL; }
                else if (c == 'U') { DEBUG.UNDO = !DEBUG.UNDO; }
                else if (c == 'V') { DEBUG.VIEWER = !DEBUG.VIEWER; }
                else if (c == 'W') { DEBUG.ROLLOVER = !DEBUG.ROLLOVER; }
                else if (c == 'Z') { resetScrollRegion(); }
                
                //else if (c == '|') { DEBUG_FONT_METRICS = !DEBUG_FONT_METRICS; }
                else if (c == '^') { DEBUG.DR = !DEBUG.DR; }
                else if (c == '+') { DEBUG.META = !DEBUG.META; }
                else if (c == '?') { DEBUG.SCROLL = !DEBUG.SCROLL; }
                else if (c == '{') { DEBUG.PATHWAY = !DEBUG.PATHWAY; }
                else if (c == '}') { DEBUG.PARENTING = !DEBUG.PARENTING; }
                else if (c == '>') { DEBUG.DND = !DEBUG.DND; }
                else if (c == '(') { DEBUG.setAllEnabled(true); }
                else if (c == ')') { DEBUG.setAllEnabled(false); }
                else if (c == '*') { tufts.vue.action.PrintAction.getPrintAction().fire(this); }
                //else if (c == '&') { tufts.macosx.Screen.fadeFromBlack(); }
                //else if (c == '@') { tufts.macosx.Screen.setMainAlpha(.5f); }
                //else if (c == '$') { tufts.macosx.Screen.setMainAlpha(1f); }
                else if (c == '~') { System.err.println("MapViewer debug abort."); System.exit(-1); }
                else if (c == '\\') {
                    VUE.toggleFullScreen();
                }
                else if (c == '|') {
                    VUE.toggleFullScreen(true); // native full screen mode
                }
                else if (c == '!') {
                    if (debugInspector == null) {
                        debugInspector = new ToolWindow("Inspector", VUE.getRootFrame() == null ? debugFrame : VUE.getRootFrame());
                        debugInspector.addTool(new LWCInspector());
                    }
                    debugInspector.setVisible(true);
                } else if (c == '@') {
                    if (debugPanner == null) {
                        debugPanner = new ToolWindow("Panner", VUE.getRootFrame() == null ? debugFrame : VUE.getRootFrame());
                        debugPanner.addTool(new MapPanner());
                    }
                    debugPanner.setVisible(true);
                } else
                    did = false;
                if (did) {
                    System.err.println("*** diagnostic '" + c + "' toggled (input=" + viewer + ")");
                    repaint();
                }
            }
        }

        
        
        public void keyReleased(KeyEvent e) {
            if (DEBUG.KEYS) out("[" + e.paramString() + "]");
            
            if (activeTool.handleKeyReleased(e))
                return;

            if (toolKeyDown == e.getKeyCode()) {
                // Don't revert tmp tool if we're in the middle of a drag
                if (sDragUnderway)
                    toolKeyReleased = true;
                else
                    revertTemporaryTool();
            }
            
            /*
            if (toolKeyDown == e.getKeyCode()) {
                //if (! (VueUtil.isMacPlatform() && toolKeyDown == KEY_TOOL_PAN)) {
                revertTemporaryTool();
                //}
            }
             */
        }
        
        public void keyTyped(KeyEvent e) // not very useful -- has keyChar but no key-code
        {
            // System.err.println("[" + e.paramString() + "]");
        }
        
        
        
        /** check for hits on control point -- pick one up and return
         *  true if we hit one -- false otherwise
         */
        private boolean checkAndHandleControlPointPress(MapMouseEvent e) {
            Iterator icl = VueSelection.getControlListeners().iterator();
            while (icl.hasNext()) {
                if (checkAndHandleControlListenerHits((LWSelection.ControlListener)icl.next(), e, true))
                    return true;
            }
            if (resizeControl.active) {
                if (checkAndHandleControlListenerHits(resizeControl, e, false))
                    return true;
            }
            return false;
        }
        
        private boolean checkAndHandleControlListenerHits(LWSelection.ControlListener cl, MapMouseEvent e, boolean mapCoords) {
            final int screenX = e.getX();
            final int screenY = e.getY();
            final int slop = 1; // a near-miss still grabs a control point
            
            float x = 0;
            float y = 0;
            
            Point2D.Float[] ctrlPoints = cl.getControlPoints();
            for (int i = 0; i < ctrlPoints.length; i++) {
                Point2D.Float cp = ctrlPoints[i];
                if (cp == null)
                    continue;
                if (mapCoords) {
                    x = mapToScreenX(cp.x) - SelectionHandleSize/2;
                    y = mapToScreenY(cp.y) - SelectionHandleSize/2;
                } else {
                    x = cp.x - SelectionHandleSize/2;
                    y = cp.y - SelectionHandleSize/2;
                }
                if (screenX >= x-slop &&
                    screenY >= y-slop &&
                    screenX <= x + SelectionHandleSize+slop &&
                    screenY <= y + SelectionHandleSize+slop)
                {
                    clearRollover(); // must do now to make sure bounds are set back to small
                    // TODO URGENT: need to translate map mouse event to location of
                    // control point on shrunken back (regular scale) node -- WHAT A HACK! UGH!
                    if (DEBUG.MOUSE||DEBUG.LAYOUT) System.out.println("hit on control point " + i + " of controlListener " + cl);
                    dragControl = cl;
                    dragControlIndex = i;
                    dragControl.controlPointPressed(i, e);
                        /*
                        // dragOffset only used when dragComponent != null
                        dragOffset.setLocation(cp.x - e.getMapX(),
                        cp.y - e.getMapY());
                         */
                    return true;
                }
            }
            return false;
        }
        
        
        
        private LWComponent hitComponent = null;
        private Point2D originAtDragStart;
        private Point viewportAtDragStart;
        private boolean mLabelEditWasActiveAtMousePress;
        public void mousePressed(MouseEvent e) {
            boolean wasFocusOwner = isFocusOwner(); // not doing it
            
            if (DEBUG.MOUSE) {
                System.out.println("-----------------------------------------------------------------------------");
                out("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "] focusOwner=" + wasFocusOwner);
            }

            mLabelEditWasActiveAtMousePress = (activeTextEdit != null);
            if (DEBUG.FOCUS) System.out.println("\tmouse-pressed active text edit="+mLabelEditWasActiveAtMousePress);
            // TODO: if we didn' HAVE focus, don't change the selection state --
            // only use the mouse click to gain focus.
            viewer.clearTip();
            grabVueApplicationFocus("mousePressed", e);//requestFocus();

            if (wasFocusOwner == false) {
                if (DEBUG.FOCUS) out("ignoring click on viewer focus gain");
                e.consume();
                return;
            }
            
            dragStart.setLocation(e.getX(), e.getY());
            if (DEBUG.MOUSE) System.out.println("dragStart set to " + dragStart);
            
            if (activeTool == HandTool) {
                originAtDragStart = getOriginLocation();
                if (inScrollPane)
                    viewportAtDragStart = mViewport.getViewPosition();
                else
                    viewportAtDragStart = null;
                return;
            }
            
            setLastMousePressPoint(e.getX(), e.getY());
            
            dragComponent = null;
            
            //-------------------------------------------------------
            // Check for hits on selection control points
            //-------------------------------------------------------
            
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            
            MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, null, null);
            
            if (activeTool.handleMousePressed(mme))
                return;
            
            if (e.getButton() == MouseEvent.BUTTON1 && activeTool.supportsSelection()) {
                hitOnSelectionHandle = checkAndHandleControlPointPress(mme);
                if (hitOnSelectionHandle) {
                    return;
                }
            }
            
            //-------------------------------------------------------
            // Check for hits on map LWComponents
            //-------------------------------------------------------
            
            //if (activeTool.supportsSelection() || activeTool.supportsClick()) {
            // Change to supportsComponentSelection?
            if (activeTool.supportsSelection()) {
                hitComponent = activeTool.findComponentAt(getMap(), mapX, mapY);
                if (DEBUG.MOUSE && hitComponent != null)
                    System.out.println("\t    on " + hitComponent + "\n" +
                    "\tparent " + hitComponent.getParent());
                mme.setHitComponent(hitComponent);
            } else {
                hitComponent = null;
            }
            
            //int mods = e.getModifiers();
            //e.isPopupTrigger()
            // java 1.4.0 bug on PC(w2k): isPopupTrigger isn't true for right-click!
            //if ((mods & RIGHT_BUTTON_MASK) != 0 && (mods & java.awt.Event.CTRL_MASK) == 0)
            
            //if ((mods & RIGHT_BUTTON_MASK) != 0 && !e.isControlDown() && !activeTool.usesRightClick())
            //    && !e.isControlDown()
            //    && !activeTool.usesRightClick())
            if ((e.isPopupTrigger() || isRightClickEvent(e)) && !activeTool.usesRightClick()) {
                if (hitComponent != null && !hitComponent.isSelected())
                    selectionSet(justSelected = hitComponent);
                
                //-------------------------------------------------------
                // MOUSE: We've pressed the right button down, so pop
                // a context menu depending on what's in selection.
                //-------------------------------------------------------
                displayContextMenu(e, hitComponent);
            }
            else if (hitComponent != null) {
                // special case handling for KEY_TOOL_LINK which
                // doesn't want to be fully activated till the
                // key is down (ctrl) AND the left mouse has been
                // pressed over a component to drag a link off.
                if (toolKeyDown == KEY_TOOL_LINK)
                    toolSelected(LinkTool);
                
                //-------------------------------------------------------
                // MOUSE: We've pressed the left (normal) mouse on SOME LWComponent
                //-------------------------------------------------------
                
                activeTool.handleComponentPressed(mme);
                
                if (mme.getDragRequest() != null) {
                    dragComponent = mme.getDragRequest(); // TODO: okay, at least HERE, dragComponent CAN be a real component...
                    //dragOffset.setLocation(0,0); // todo: want this? control poins also need dragOffset
                }
                else if (e.isShiftDown()) {
                    //-------------------------------------------------------
                    // Shift was down: TOGGLE SELECTION STATUS
                    //-------------------------------------------------------
                    selectionToggle(hitComponent);
                    
                }
                else {
                    //-------------------------------------------------------
                    // Vanilla mouse press:
                    //          (1) SET SELECTION
                    //          (2) GET READY FOR A POSSIBLE UPCOMING DRAG
                    // Clear any existing selection, and set to hitComponent.
                    // Also: mark drag start in case they start dragging
                    //-------------------------------------------------------
                    
                    // TODO: don't do this unless current tool willing to select this object
                    if (!hitComponent.isSelected())
                        selectionSet(justSelected = hitComponent);
                    
                    //-------------------------------------------------------
                    // Something is now selected -- get prepared to drag
                    // it in case they start dragging.  If it's a mult-selection,
                    // set us up for a group drag.
                    //-------------------------------------------------------
                    // Okay, ONLY drag even a single object via the selection
                    //if (VueSelection.size() > 1) {
                    // pick up a group selection for dragging
                    draggedSelectionGroup.useSelection(VueSelection);
                    dragComponent = draggedSelectionGroup;
                    //} else {
                    // [ We never drag just single components anymore --
                    // just the entire selection ]
                    // just pick up the single component
                    //dragComponent = hitComponent;
                    //}
                    
                }
            } else {
                //-------------------------------------------------------
                // hitComponent was null
                //-------------------------------------------------------
                
                // SPECIAL CASE for dragging the entire selection
                if (activeTool.supportsSelection()
                    && noModifierKeysDown(e)
                    //&& VueSelection.size() > 1
                    && VueSelection.contains(mapX, mapY))
                {
                    //-------------------------------------------------------
                    // PICK UP A GROUP SELECTION FOR DRAGGING
                    //
                    // If we clicked on nothing, but are actually within
                    // the bounds of an existing selection, pick it
                    // up for dragging.
                    //-------------------------------------------------------
                    draggedSelectionGroup.useSelection(VueSelection);
                    dragComponent = draggedSelectionGroup;
                } else if (!e.isShiftDown() && activeTool.supportsSelection()) {
                    //-------------------------------------------------------
                    // CLEAR CURRENT SELECTION & START DRAGGING FOR A NEW ONE
                    //
                    // If we truly clicked on nothing, clear the selection,
                    // unless shift was down, which is easy to accidentally
                    // have happen if user is toggling the selection.
                    //-------------------------------------------------------
                    selectionClear();
                    repaint(); // if selection handles not on, we need manual repaint here
                }
                if (activeTool.supportsDraggedSelector(e))
                    isDraggingSelectorBox = true;
                else
                    isDraggingSelectorBox = false;// todo ??? this was true?
            }
            
            if (dragComponent != null)
                dragOffset.setLocation(dragComponent.getX() - mapX,
                dragComponent.getY() - mapY);
            
        }
        
        private void displayContextMenu(MouseEvent e, LWComponent hitComponent) {

            if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
                // on mac, attempt to pop a menu in true full-screen mode
                // put's us to black screen and leaves us there!
                return;
            }
            
            if (VueSelection.isEmpty()) {
                getMapPopup().show(e.getComponent(), e.getX(), e.getY());
            } else if (VueSelection.size() == 1) {
                getSingleSelectionPopup(hitComponent).show(e.getComponent(), e.getX(), e.getY());
            } else {
                getMultiSelectionPopup().show(e.getComponent(), e.getX(), e.getY());
            }
        }
        
        
        private Point lastDrag = new Point();
        private void dragRepositionViewport(Point mouse) {
            if (DEBUG.MOUSE) {
                System.out.println(" lastDragLoc " + out(lastDrag));
                System.out.println(" newMouseLoc " + out(mouse));
            }
            if (inScrollPane) {
                int dx = lastDrag.x - mouse.x;
                int dy = lastDrag.y - mouse.y;
                panScrollRegion(dx, dy);
            } else {
                int dx = dragStart.x - mouse.x;
                int dy = dragStart.y - mouse.y;
                setMapOriginOffset(originAtDragStart.getX() + dx,
                                   originAtDragStart.getY() + dy);
            }
        }
        
        
        /** mouse has moved while dragging out a selector box -- update
         * selector box shape & repaint */
        private void dragResizeSelectorBox(int screenX, int screenY) {
            // Set repaint-rect to where old selection is
            Rectangle repaintRect = null;
            if (draggedSelectorBox != null)
                repaintRect = draggedSelectorBox;
            
            // Set the current selection box
            int sx = dragStart.x < screenX ? dragStart.x : screenX;
            int sy = dragStart.y < screenY ? dragStart.y : screenY;
            draggedSelectorBox = new Rectangle(sx, sy,
            Math.abs(dragStart.x - screenX),
            Math.abs(dragStart.y - screenY));
            
            // Now add to repaint-rect the new selection
            if (repaintRect == null)
                repaintRect = new Rectangle(draggedSelectorBox);
            else
                repaintRect.add(draggedSelectorBox);
            
            repaintRect.width++;
            repaintRect.height++;
            if (DEBUG.PAINT && redrawingSelector)
                System.out.println("dragResizeSelectorBox: already repainting selector");
            
            // XOR drawing simply keeps repainting on an existing graphics context,
            // which is extremely fast (because we can just XOR erase the previous
            // paint by redrawing it again) but the PC graphics context gets
            // polluted with garbage when left around, and now it looks like on mac too?
            // 2004-11-23 03:51.31 Tuesday -- Mac okay now, but appears no faster!
            // And still misses clearing old frames sometimes if window is huge
            //if (VueUtil.isMacPlatform())
            //redrawingSelector = true;
            
            if (OPTIMIZED_REPAINT)
                //paintImmediately(repaintRect);
                repaint(repaintRect);
            // todo: above helps alot, except that the outside halves of strokes are being erased
            // because our node.intersects is failing to take into account stroke width...
            // we're going to need to fix that anyway tho
            else
                repaint();
            
            // might need paint immediately or might miss cleaning up some old boxes
            // (RepaintManager coalesces repaint requests that are close temporally)
            // We use an explicit XOR re-draw to erase old and then draw the new
            // selector box.
        }
        
        /*
        private void dragResizeSelectorBox(int screenX, int screenY)
        {
            // Set repaint-rect to where old selection is
            Rectangle repaintRect = null;
            if (OPTIMIZED_REPAINT) {
                if (draggedSelectorBox != null) {
                    repaintRect = new Rectangle(draggedSelectorBox);
                    lastPaintedSelectorBox = draggedSelectorBox;
                }
            }
         
            // Set the current selection box
            int sx = dragStart.x < screenX ? dragStart.x : screenX;
            int sy = dragStart.y < screenY ? dragStart.y : screenY;
            draggedSelectorBox = new Rectangle(sx, sy,
                                                Math.abs(dragStart.x - screenX),
                                                Math.abs(dragStart.y - screenY));
         
            if (OPTIMIZED_REPAINT) {
                // Now add to repaint-rect the new selection
                if (repaintRect == null)
                    repaintRect = new Rectangle(draggedSelectorBox);
                else
                    repaintRect.add(draggedSelectorBox);
                //repaintRect.grow(4,4);
                // todo java bug: antialiased bottom or right edge of a stroke
                // (a single pixel's worth) is erased by the dragged selection box
                // when it passes exactly along/thru the edge in a 1-pixel increment.
                // No amount of growing the region will help because the bug
                // happens along the edge of whatever the repaint-region is itself,
                // so all you can do is move around where the bug happens relative
                // to dragged selection box.
                repaintRect.width++;
                repaintRect.height++;
                redrawingSelector = true;
                paintImmediately(repaintRect);
                //repaint(repaintRect);
            } else {
                repaint();
            }
        }
         */

        //private long lastRotationTime = 0;
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (DEBUG.MOUSE) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            /*
            long now = System.currentTimeMillis();
            if (now - lastRotationTime < 50) { // todo: preference
                if (DEBUG.MOUSE) System.out.println("ignoring speedy wheel event");
                return;
            }
            */
            int rotation = e.getWheelRotation();
            if (rotation > 0)
                tufts.vue.ZoomTool.setZoomSmaller(null);
            else if (rotation < 0)
                tufts.vue.ZoomTool.setZoomBigger(null);
            //lastRotationTime = System.currentTimeMillis();
        }
        
        public void mouseMoved(MouseEvent e) {
            if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            
            
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            // use deepest to penetrate into groups
            LWComponent hit = getMap().findDeepestChildAt(mapX, mapY);
            //LWComponent hit = getMap().findChildAt(mapX, mapY);
            if (DEBUG.ROLLOVER) System.out.println("  mouseMoved: hit="+hit);
            
            if (hit != sMouseOver) {
                if (sMouseOver != null) {
                    viewer.clearTip(); // in case it had a tip displayed
                    if (sMouseOver == rollover)
                        clearRollover();
                    MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, hit, null);
                    sMouseOver.mouseExited(mme);
                }
            }
            if (hit != null) {
                MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, hit, null);
                if (hit == sMouseOver)
                    hit.mouseMoved(mme);
                else
                    hit.mouseEntered(mme);
            } else
                viewer.clearTip(); // if over nothing, always make sure no tip displayed
            
            sMouseOver = hit;
            
            if (DEBUG.VIEWER) {
                _mouse.x = lastMouseX;
                _mouse.y = lastMouseY;
                repaint();
            }
            
            // Workaround for known Apple Mac OSX Java 1.4.1 bug:
            // Radar #3164718 "Control-drag generates mouseMoved, not mouseDragged"
            //if (dragComponent != null && VueUtil.isMacPlatform()) {
            //    if (DEBUG_MOUSE_MOTION) System.out.println("manually invoking mouseDragged");
            //    mouseDragged(e);
            //}
            
            //if (VUE.Prefs.doRolloverZoom() && RolloverAutoZoomDelay >= 0) {
            if (RolloverAutoZoomDelay >= 0) {
                if (DEBUG_TIMER_ROLLOVER && !sDragUnderway && !(activeTextEdit != null)) {
                    if (RolloverAutoZoomDelay > 10) {
                        if (rolloverTask != null)
                            rolloverTask.cancel();
                        rolloverTask = new RolloverTask();
                        try {
                            rolloverTimer.schedule(rolloverTask, RolloverAutoZoomDelay);
                        } catch (IllegalStateException ex) {
                            // don't know why this happens somtimes...
                            System.out.println(ex + " (fallback: creating new timer)");
                            rolloverTimer = new Timer();
                            rolloverTimer.schedule(rolloverTask, RolloverAutoZoomDelay);
                        }
                    } else {
                        runRolloverTask();
                    }
                }
            }
        }
        
        public void mouseEntered(MouseEvent e) {
            if (DEBUG.MOUSE||DEBUG.ROLLOVER) System.out.println(e);
            if (sMouseOver != null) {
                sMouseOver.mouseExited(new MapMouseEvent(e));
                sMouseOver = null;
            }
            grabVueApplicationFocus("mouseEntered", e);//requestFocus();
        }
        
        public void mouseExited(MouseEvent e) {
            if (DEBUG.MOUSE||DEBUG.ROLLOVER) System.out.println(e);
            if (sMouseOver != null && sMouseOver == rollover)
                clearRollover();
            if (false&&sMouseOver != null) {
                sMouseOver.mouseExited(new MapMouseEvent(e));
                sMouseOver = null;
            }
            
            // If you roll the mouse into a tip window, the MapViewer
            // will get a mouseExited -- we clear the tip if this
            // happens as we never want the tip to obscure anything.
            // This is slighly dangerous in that if for some reason
            // the tip has been placed over it's own activation
            // region, and you put the mouse over the intersection
            // area of the tip and the activation region, we'll enter
            // a show/hide loop: mouse into trigger region pops tip
            // window, which comes up under where the mouse is already
            // at, immediately triggering a mouseExited on the
            // MapViewer, which bring us here in mouseExited to clear
            // the tip, and when it clears, the mouse enters the map
            // again, and triggers the tip window again, looping for
            // as long as you leave the mouse there (because you can
            // still move the mouse away this isn't a fatal error).
            // But since this is still very undesirable, we take great
            // pains in placing the tip window to never overlap the
            // trigger region. (see setTip)
            
            viewer.clearTip();
            
            // Is still nice to do this tho because we get a mouse
            // exited when you rollover the tip-window itself, and if
            // it's right at the edge of the node and you're going for
            // the resize-control, better to have the note clear out
            // so you don't accidentally hit the tip when going for
            // the control.
        }
        
        private void scrollToMouse(MouseEvent e) {
            if (DEBUG.SCROLL) out("scrollToMouse " + out(e.getPoint()));
            scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1,1));
        }
        
        private void scrollToVisible(LWComponent c, int pad) {
            Rectangle r = growForSelection(mapToScreenRect(c.getBounds()), pad);
            // [turned back on now that we only scroll to mouse during drags]
            // turned off growth: don't scroll till really at edge:
            // todo: follow-on adjustscrollregion is adjusting to content-bounds
            // of map, which is still giving us a big margin when against edge
            // of canvas, as opposed to when against edge of viewport on bigger
            // canvas.
            //Rectangle r = mapToScreenRect(c.getBounds());

            // TODO: if component was off screen or is bigger than
            // screen, need to know what direction we're dragging
            // it in before we scroll to visible.
            //r = r.intersection(getVisibleBounds());
            // if we add a pixel in the drag direction to above
            // intersected rect, it will now be off screen in the
            // direction we're moving, so we'll scroll there...
            
            // we can pad it a bit to be sure we'll totally come up against
            // the edge of the max scroll region, or so we don't bother
            // auto-scrolling unless we really need to.
            if (r.width < getVisibleWidth() / 2 && r.height < getVisibleHeight() / 2) {
                if (DEBUG.SCROLL) out("scrollToComponent " + c);
                scrollRectToVisible(r);
            }
        }
        private void scrollToVisible(LWComponent c) {
            scrollToVisible(c, -2); // don't give big margin during auto-scroll
        }
        
        //private int drags=0;
        public void mouseDragged(MouseEvent e) {
            sDragUnderway = true;
            clearRollover();
            //System.out.println("drag " + drags++);
            if (mouseWasDragged == false) {
                // dragStart
                // we're just starting this drag
                //if (inScrollPane || dragComponent != null || dragControl != null) always set mousewasdragged
                if (dragComponent == null && dragControl == null)
                    viewer.setAutoscrolls(false);
                mouseWasDragged = true;
                lastDrag.setLocation(dragStart);
                if (DEBUG.MOUSE)
                    System.out.println(" lastDragSet " + out(lastDrag));
                // if we pan, our canvas location might change, offsetting mouse coord each time
                if (inScrollPane)
                    lastDrag = SwingUtilities.convertPoint(MapViewer.this, lastDrag, getParent());
            }
            
            if (DEBUG.VIEWER) _mouse = e.getPoint();
            if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            
            int screenX = e.getX();
            int screenY = e.getY();
            Point currentMousePosition = e.getPoint();
            
            if (activeTool == HandTool) {
                // drag the entire map
                if (originAtDragStart != null) {
                    // if we pan, our canvas location might change, offsetting mouse coord each time
                    if (inScrollPane)
                        currentMousePosition = SwingUtilities.convertPoint(MapViewer.this, currentMousePosition, getParent());
                    dragRepositionViewport(currentMousePosition);
                    lastDrag.setLocation(currentMousePosition);
                } else
                    System.err.println("null originAtDragStart -- drag skipped!");
                return;
            }
            
            //-------------------------------------------------------
            // Stop component dragging if the mouse leaves our component (the viewer)
            //-------------------------------------------------------
            
            if (!e.getComponent().contains(screenX, screenY)) {
                // limit the mouse-drag point to container locations
                if (screenX < 0)
                    screenX = 0;
                else if (screenX >= getWidth())
                    screenX = getWidth()-1;
                if (screenY < 0)
                    screenY = 0;
                else if (screenY >= getHeight())
                    screenY = getHeight()-1;
            }
            
            if (!activeTool.supportsDraggedSelector(e) && !activeTool.supportsResizeControls()) 
                return;
            // todo: dragControls could be skipped! [WAS TRUE W/OUT RESIZE CONTROL CHECK ABOVE]
            // todo serious: now text tool leaves a dragged box around!
            
            if (dragComponent == null && isDraggingSelectorBox) {
                //-------------------------------------------------------
                // We're doing a drag select-in-region.
                // Update the dragged selection box.
                //-------------------------------------------------------
                scrollToMouse(e);
                dragResizeSelectorBox(screenX, screenY);
                return;
            } else {
                draggedSelectorBox = null;
                lastPaintedSelectorBox = null;
            }
            
            float mapX = screenToMapX(screenX);
            float mapY = screenToMapY(screenY);
            
            MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, null, draggedSelectorBox);
            
            Rectangle2D.Float repaintRegion = new Rectangle2D.Float();
            
            if (dragControl != null) {
                
                //-------------------------------------------------------
                // Move a control point that's being dragged
                //-------------------------------------------------------
                
                dragControl.controlPointMoved(dragControlIndex, mme);
                scrollToMouse(e);
                
            } else if (dragComponent != null) {
                
                // todo opt: do all this in dragStart
                //-------------------------------------------------------
                // Compute repaint region based on what's being dragged
                //-------------------------------------------------------
                
                // todo: the LWGroup drawgComponent is NOT updating its bounds based on what's in it...
                
                if (OPTIMIZED_REPAINT) repaintRegion.setRect(dragComponent.getBounds());
                //System.out.println("Starting " + repaintRegion);
                
                //if (repaintRegion == null) {// todo: this is debug
                //new Throwable("mouseDragged: null bounds dragComponent " + dragComponent).printStackTrace();
                //    repaintRegion = new Rectangle2D.Float();
                //}
                
                /*
                  // this should now be handled by above
                if (OPTIMIZED_REPAINT && dragComponent instanceof LWLink) {
                    LWLink lwl = (LWLink) dragComponent;
                    LWComponent c = lwl.getComponent1();
                    if (c != null) repaintRegion.add(c.getBounds());
                    c = lwl.getComponent2();
                    if (c != null) repaintRegion.add(c.getBounds());
                }
                 */
                
                //-------------------------------------------------------
                // Reposition the component due to mouse drag
                //-------------------------------------------------------
                
                dragComponent.setLocation(mapX + dragOffset.x,
                                          mapY + dragOffset.y);
                //dragPosition.setLocation(mapX + dragOffset.x,mapY + dragOffset.y);
                
                if (inScrollPane)
                    //scrollToVisible(dragComponent); // unexpected behaviour with large selections
                    scrollToMouse(e);
                
                //-------------------------------------------------------
                // Compute more repaint region
                //-------------------------------------------------------
                
                //System.out.println("  Adding " + dragComponent.getBounds());
                if (OPTIMIZED_REPAINT) repaintRegion.add(dragComponent.getBounds());
                //if (DEBUG.PAINT) System.out.println("     Got " + repaintRegion);
                
                if (OPTIMIZED_REPAINT && dragComponent instanceof LWLink) {
                    // todo: not currently used as link dragging disabled
                    // todo: fix with new dragComponent being link as control point
                    LWLink l = (LWLink) dragComponent;
                    LWComponent c = l.getComponent1();
                    if (c != null) repaintRegion.add(c.getBounds());
                    c = l.getComponent2();
                    if (c != null) repaintRegion.add(c.getBounds());
                }
            }
            
            if (activeTool.handleMouseDragged(mme)) {
                ;
            }
            else if (!DEBUG_FINDPARENT_OFF
                     //&& (dragComponent instanceof LWNode || VueSelection.allOfType(LWNode.class)) //todo opt: cache type
                     //todo: dragComponent for moment is only ever the LWGroup or a LWLink
                     && dragComponent != null
                     //&& !(dragComponent instanceof LWLink) // todo: not possible -- dragComponent never a single LWC anymore
                     && !(VueSelection.allOfType(LWLink.class)) //todo opt: cache type
                     ) {
                
                //-------------------------------------------------------
                // vanilla drag -- check for node drop onto another node
                //-------------------------------------------------------

                // TODO: this code needs major cleanup, and needs to be made
                // container general instead of node specific.
                
                LWComponent over = null;
                /*
                LWComponent dragLWC = null;
                if (dragComponent instanceof LWGroup) {
                    // dragComponent is (always?)) a LWGroup these days...
                    LWGroup group = (LWGroup) dragComponent;
                    if (group.getChildList().size() == 1)
                        dragLWC = (LWComponent) group.getChildList().get(0);
                }
                if (dragLWC == null)
                    over = getMap().findLWNodeAt(mapX, mapY);
                else
                    over = getMap().findDeepestChildAt(mapX, mapY, dragLWC);
                */
                // is ignoreSelected good enough because possible children of
                // a dragged object are not selected?
                over = getMap().findDeepestChildAt(mapX, mapY, true);
                
                if (indication != null && indication != over) {
                    //repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null) {
                    if (isValidParentTarget(over))
                        setIndicated(over);
                    else if (isValidParentTarget(over.getParent()))
                        setIndicated(over.getParent());
                        
                    //repaintRegion.add(over.getBounds());
                }
            }
            
            if (dragComponent == null && dragControl == null)
                return;
            
            if (OPTIMIZED_REPAINT == false) {
                
                repaint();
                
            } else {
                //if (DEBUG.PAINT) System.out.println("MAP REPAINT REGION: " + repaintRegion);
                //-------------------------------------------------------
                //
                // Do Repaint optimzation: This makes a HUGE
                // difference when cavas is big, or when there are
                // alot of visible nodes to paint, and especially when
                // both conditions are true.  This is much faster even
                // with with all the computation & recursive list
                // generation we we're doing below.
                //
                //-------------------------------------------------------
                
                // todo: node bounds computation doesn't include
                // border stroke width (bounds falls in middle of
                // stroke, not outside) so outer half of border stroke
                // isn't being included in the clear region -- it's
                // hacked-patched for now with a fixed slop region.
                // Will also need to grow by stroke width of a dragged link
                // as it's corners are beyond bounds point with very wide strokes.
                
                LWComponent movingComponent = dragComponent;
                if (dragControl != null && dragControl instanceof LWComponent)
                    movingComponent = (LWComponent) dragControl;
                
                java.util.Iterator i = null;
                if (movingComponent instanceof LWLink) { // only happens thru a dragControl
                    LWLink l = (LWLink) movingComponent;
                    // todo bug: will fail with new chance of null link endpoint
                    //if (l.getControlCount() > 0)//add link bounds
                    
                    repaintRegion.add(l.getBounds());
                    
                    //i = new VueUtil.GroupIterator(l.getLinkEndpointsIterator(),
                    //                            l.getComponent1().getLinkEndpointsIterator(),
                    //                            l.getComponent2().getLinkEndpointsIterator());
                    
                } else {
                    // TODO OPT: compute this once when we start the drag!
                    // TODO BUG: sometimes movingComponent can be null when dragging control point??
                    // should even be here if dragging control point (happens when all selected??)
                    //i = movingComponent.getAllConnectedNodes().iterator();
                    // need to add links themselves because could be curved and have way-out control points
                    //i = new VueUtil.GroupIterator(movingComponent.getAllConnectedNodes(),
                    //movingComponent.getLinks());//won't work! dragComponent is always an LWGroup
                    
                    // TODO: if this isn't the active map, dragComponent/movingCompontent will be null!

                    //i = movingComponent.getAllConnectedComponents().iterator();
                    if (movingComponent != null)
                        i = movingComponent.getAllLinks().iterator();
                    // actually, we probably do NOT need to add the nodes at the other
                    // ends of the links anymore sinde the link always connects at the
                    // edge of the node...
                    
                    
                    // perhaps handle this whole thing thru event flow
                    // where somehow whenever a link or node moves/resizes it can add itself
                    // to the paint region...
                }
                while (i != null && i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    //if (DEBUG.PAINT) System.out.println("RR adding: " + c);
                    repaintRegion.add(c.getBounds());
                }
                //if (linkSource != null) repaintRegion.add(linkSource.getBounds());
                
                // TODO BUG: something extra is getting added into repaint region making
                // (between top diagnostic and here) that's make it way bigger than needed,
                // and controlPoints are causing 0,0 to be added to the repaint region.
                // create a RepaintRegion rectangle object that understands the idea
                // of an empty region (not just 0,0), and an unintialized RR that has no location or size.
                //if (DEBUG.PAINT) System.out.println("MAP REPAINT REGION: " + repaintRegion);
                Rectangle rr = mapToScreenRect(repaintRegion);
                growForSelection(rr);
                
                /*
                boolean draggingChild = false;
                if (!(movingComponent.getParent() instanceof LWMap)) {
                    movingComponent.setDisplayed(false);
                    draggingChild = true;
                    }*/
                
                //integerAlignRect(repaintRegion); // doesn't help aa clip-rect bug
                
                RepaintRegion = repaintRegion;
                
                // speeds up traversal: limits Graphics calls
                // speeds up painting: limits raw blitting
                repaint(rr);
                
                // TODO BUG: java is dithering strokes (and probably
                // everything) at the TOP edge of the repaint region
                // (graphics clip-rect) to whatever the background
                // color is... (if we fill the repaint region with
                // a color b4 painting, it will dither to that color)
                
                
            }
        }
        
        /*
        private void integerAlignRect(Rectangle2D.Float r)
        {
            r.x = (float) Math.floor(r.x);
            r.y = (float) Math.floor(r.y);
            r.width = (float) Math.ceil(r.width);
            r.height = (float) Math.ceil(r.height);
            }*/
        
        public void mouseReleased(MouseEvent e) {
            sDragUnderway = false;
            if (DEBUG.MOUSE) System.out.println("[" + e.paramString() + "]");
            
            viewer.setAutoscrolls(true); // in case had been cleared for panning
            setLastMousePoint(e.getX(), e.getY());
            
            MapMouseEvent mme = new MapMouseEvent(e, draggedSelectorBox);
            mme.setMousePress(lastMousePressX, lastMousePressY);
            
            if (mouseWasDragged && dragControl != null) {
                dragControl.controlPointDropped(dragControlIndex, mme);
            }
            else if (activeTool.handleMouseReleased(mme)) {
                repaint();
            }
            else if (mouseWasDragged && (indication == null || indication instanceof LWContainer)) {
                if (VUE.TUFTS) {
                    if (indication == null || indication instanceof LWNode)
                        checkAndHandleNodeDropReparenting();
                } else {
                    if (!e.isShiftDown())
                        checkAndHandleDroppedReparenting();
                }
            }
            
            // special case event notification for any other viewers
            // of this map that may now need to repaint (LWComponents currently
            // don't sent event notifications for location & size changes
            // for performance)
            if (mouseWasDragged)
                VUE.getUndoManager().mark("Drag");
            
            if (draggedSelectorBox != null && !activeTool.supportsDraggedSelector(e))
                System.err.println("Illegal state warning: we've drawn a selector box w/out tool that supports it!");
            
            // reset in-drag only state
            clearIndicated();
            
            if (draggedSelectorBox != null && activeTool.supportsDraggedSelector(e)) {
                
                //System.out.println("dragged " + draggedSelectorBox);
                //Rectangle2D.Float hitRect = (Rectangle2D.Float) screenToMapRect(draggedSelectorBox);
                //System.out.println("map " + hitRect);
                
                boolean handled = false;
                if (draggedSelectorBox.width > 10 && draggedSelectorBox.height > 10)
                    handled = activeTool.handleSelectorRelease(mme);
                
                if (!handled && activeTool.supportsSelection()) {
                    // todo: e.isControlDown always false? only on mac? on the laptop?
                    //java.util.List list = computeSelection(screenToMapRect(draggedSelectorBox),
                    //                                     e.isControlDown()
                    //                                     || activeTool == LinkTool);

                    List list = computeSelection(screenToMapRect(draggedSelectorBox),
                                                 activeTool.getSelectionType());
                    
                    if (e.isShiftDown())
                        selectionToggle(list.iterator());
                    else
                        selectionAdd(list.iterator());
                    
                }
                
                
                //-------------------------------------------------------
                // repaint optimization
                //-------------------------------------------------------
                draggedSelectorBox.width++;
                draggedSelectorBox.height++;
                RR(draggedSelectorBox);
                draggedSelectorBox = null;
                lastPaintedSelectorBox = null;
                //-------------------------------------------------------
                
                
                // bounds cache hack
                if (!VueSelection.isEmpty())
                    draggedSelectionGroup.useSelection(VueSelection);
                // todo: need to update draggedSelectionGroup here
                // so we can use it's cached bounds to compute
                // the painting of the selection -- rename to just
                // SelectionGroup if we keep using it this way.
                
            }
            
            VUE.getUndoManager().mark(); // in case anything happened
            
            if (toolKeyReleased) {
                toolKeyReleased = false;
                revertTemporaryTool();
            }
            
            //-------------------------------------------------------
            // reset all in-drag only state
            //-------------------------------------------------------
            
            adjustCanvasSize();
            // now that scroll region has been adjust to fit everything,
            // scroll to visible anything we may have dropped off the edge
            // of the screen.
            if (mouseWasDragged && dragComponent != null)
                scrollToVisible(dragComponent, 6);
            // pad arg: leave more room around final position
            // (make sure we bump up against edge of scroll region -- why need so big?)
            
            dragControl = null;
            dragComponent = null;
            isDraggingSelectorBox = false;
            mouseWasDragged = false;
            
            
            // todo opt: only need to do this if we don't draw selection
            // handles while dragging (this is to put them back if we werent)
            // use selection repaint region?
            //repaint();
            
        }
        
        
        /**
         * Take what's in the selection and drop it on the current indication,
         * or on the map if no current indication.
         */
        private void checkAndHandleDroppedReparenting() {
            //-------------------------------------------------------
            // check to see if any things could be dropped on a new parent
            // This got alot more complicated adding support for
            // dropping whole selections of components, especially
            // if there are embedded children selected.
            //-------------------------------------------------------
            
            LWContainer parentTarget;
            if (indication == null)
                parentTarget = getMap();
            else
                parentTarget = (LWContainer) indication;
            
            java.util.List moveList = new java.util.ArrayList();
            java.util.Iterator i = VueSelection.iterator();
            while (i.hasNext()) {
                LWComponent droppedChild = (LWComponent) i.next();
                // don't reparent links
                if (droppedChild instanceof LWLink)
                    continue;
                // can only pull something out of group via ungroup
                //if (droppedChild.getParent() instanceof LWGroup)
                //  continue; // not with new "page" groups
                // don't do anything if parent might be reparenting
                if (droppedChild.getParent().isSelected())
                    continue;
                // todo: actually re-do drop if anything other than map so will re-layout
                if (
                    (droppedChild.getParent() != parentTarget || parentTarget instanceof LWNode) &&
                    droppedChild != parentTarget) {
                    //-------------------------------------------------------
                    // we were over a valid NEW parent -- reparent
                    //-------------------------------------------------------
                    if (DEBUG.PARENTING)
                        System.out.println("*** REPARENTING " + droppedChild + " as child of " + parentTarget);
                    moveList.add(droppedChild);
                }
            }
            
            // okay -- what we want is to tell the parent we're moving
            // from to remove them all at once -- the problem is our
            // selection could contain components of multiple parents.
            // So we have to handle each source parent seperately, and
            // remove all it's children at once -- this is so the
            // parent won't re-lay itself out (call layout()) while
            // removing children, because if does it will re-set the
            // position of other siblings about to be removed back to
            // the parent's layout spot from the draggeed position
            // they currently occupy and we're trying to move them to.
            
            java.util.HashSet parents = new java.util.HashSet();
            i = moveList.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                parents.add(c.getParent());
            }
            java.util.Iterator pi = parents.iterator();
            while (pi.hasNext()) {
                LWContainer parent = (LWContainer) pi.next();
                if (DEBUG.PARENTING)  System.out.println("*** HANDLING PARENT " + parent);
                parent.reparentTo(parentTarget, moveList.iterator());
                //parent.removeChildren(moveList.iterator());
            }
        }
        
        private void checkAndHandleNodeDropReparenting() {
            //-------------------------------------------------------
            // check to see if any things could be dropped on a new parent
            // This got alot more complicated adding support for
            // dropping whole selections of components, especially
            // if there are embedded children selected.
            //-------------------------------------------------------
            
            LWContainer parentTarget;
            if (indication == null)
                parentTarget = getMap();
            else
                parentTarget = (LWNode) indication;
            
            java.util.List moveList = new java.util.ArrayList();
            java.util.Iterator i = VueSelection.iterator();
            while (i.hasNext()) {
                LWComponent droppedChild = (LWComponent) i.next();
                // don't reparent links
                if (droppedChild instanceof LWLink)
                    continue;
                // can only pull something out of group via ungroup
                //if (droppedChild.getParent() instanceof LWGroup)
                //  continue; // not with new "page" groups
                // don't do anything if parent might be reparenting
                if (droppedChild.getParent().isSelected())
                    continue;
                // todo: actually re-do drop if anything other than map so will re-layout
                if (
                    (droppedChild.getParent() != parentTarget || parentTarget instanceof LWNode) &&
                    droppedChild != parentTarget) {
                    //-------------------------------------------------------
                    // we were over a valid NEW parent -- reparent
                    //-------------------------------------------------------
                    if (DEBUG.PARENTING)
                        System.out.println("*** REPARENTING " + droppedChild + " as child of " + parentTarget);
                    moveList.add(droppedChild);
                }
            }
            
            // okay -- what we want is to tell the parent we're moving
            // from to remove them all at once -- the problem is our
            // selection could contain components of multiple parents.
            // So we have to handle each source parent seperately, and
            // remove all it's children at once -- this is so the
            // parent won't re-lay itself out (call layout()) while
            // removing children, because if does it will re-set the
            // position of other siblings about to be removed back to
            // the parent's layout spot from the draggeed position
            // they currently occupy and we're trying to move them to.
            
            java.util.HashSet parents = new java.util.HashSet();
            i = moveList.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                parents.add(c.getParent());
            }
            java.util.Iterator pi = parents.iterator();
            while (pi.hasNext()) {
                LWContainer parent = (LWContainer) pi.next();
                if (DEBUG.PARENTING)  System.out.println("*** HANDLING PARENT " + parent);
                parent.reparentTo(parentTarget, moveList.iterator());
                //parent.removeChildren(moveList.iterator());
            }
            /*
            i = moveList.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                parentTarget.addChild(c);
            }
             */
            // If we handled the above problem in LWContainer somehow,
            // we could just make this call:
            //parentTarget.addChildren(moveList.iterator());
            
            
            /*
            //-------------------------------------------------------
            // We just dragged something that could be reparented
            // depending on what it's over now that it's dropped.
            // Drop one node on another -- add as child
            //-------------------------------------------------------
             
            LWNode droppedChild = (LWNode) dragComponent;
             
            if (parentTarget != droppedChild.getParent()) {
            //-------------------------------------------------------
            // we were over a valid NEW parent -- reparent
            //-------------------------------------------------------
            //System.out.println("*** REPARENTING " + droppedChild + " as child of " + parentTarget);
            parentTarget.addChild(droppedChild);
            }
             */
        }
        
        private final boolean noModifierKeysDown(MouseEvent e) {
            return (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isDoubleClickEvent(MouseEvent e) {
            return e.getClickCount() == 2
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isSingleClickEvent(MouseEvent e) {
            return e.getClickCount() == 1
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isRightClickEvent(MouseEvent e) {
            // 1 click, button 2 or 3 pressed, button 1 not already down & ctrl not down
            return e.getClickCount() == 1
                && (e.getButton() == java.awt.event.MouseEvent.BUTTON3 ||
                    e.getButton() == java.awt.event.MouseEvent.BUTTON2)
                && (e.getModifiersEx() & java.awt.event.InputEvent.BUTTON1_DOWN_MASK) == 0
                && !e.isControlDown();
        }
        
        public void mouseClicked(MouseEvent e) {
            if (DEBUG.MOUSE) System.out.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");
            
            //if (activeTool != ArrowTool && activeTool != TextTool)
            //return;  check supportsClick, and add such to node tool
            
            
            if (!hitOnSelectionHandle) {
                
                if (isSingleClickEvent(e)) {
                    if (DEBUG.MOUSE) System.out.println("\tSINGLE-CLICK on: " + hitComponent);
                    
                    if (hitComponent != null && !(hitComponent instanceof LWGroup)) {
                        
                        boolean handled = false;
                        // move to arrow tool?
                        
                        if (activeTool == TextTool) {
                            activateLabelEdit(hitComponent);
                            handled = true;
                        } else {
                            handled = hitComponent.handleSingleClick(new MapMouseEvent(e, hitComponent));
                        }
                        //else if (hitComponent instanceof ClickHandler) {
                        //handled = ((ClickHandler)hitComponent).handleSingleClick(new MapMouseEvent(e, hitComponent));
                        //}
                        
                        //todo: below not triggering under arrow tool if we just dragged the link --
                        // justSelected must be inappropriately set to the dragged component
                        if (!handled &&
                            (activeTool == TextTool || hitComponent.isSelected() && hitComponent != justSelected))
                            activateLabelEdit(hitComponent);
                        
                    } else if (activeTool == TextTool || activeTool == NodeTool) {
                        
                        // on mousePressed, we request focus, and if there
                        // was an activeTextEdit TextBox, it lost focus
                        // and closed itself out -- treat this click as an
                        // edit-cancel in case of node/text tool so doesn't
                        // create a new item if they were just finishing
                        // the edit via the click on the map
                        
                        if (!mLabelEditWasActiveAtMousePress) {
                            if (activeTool == NodeTool)
                                Actions.NewNode.fire(MapViewer.this);
                            else
                                Actions.NewText.fire(MapViewer.this);
                        }
                    }
                /*
                if (activeTool.supportsClick()) {
                    //activeTool.handleClickEvent(e, hitComponent); send in mapxy
                }
                 */
                    
                } else if (isDoubleClickEvent(e) && toolKeyDown == 0 && hitComponent != null) {
                    if (DEBUG.MOUSE) System.out.println("\tDOULBLE-CLICK on: " + hitComponent);
                    
                    boolean handled = false;
                    
                    if (activeTool == TextTool) {
                        activateLabelEdit(hitComponent);
                        handled = true;
                    } else {
                        handled = hitComponent.handleDoubleClick(new MapMouseEvent(e, hitComponent));
                    }
                    //else if (hitComponent instanceof ClickHandler) {
                    //handled = ((ClickHandler)hitComponent).handleDoubleClick(new MapMouseEvent(e, hitComponent));
                    //}
                    
                    if (!handled && hitComponent.supportsUserLabel())
                        activateLabelEdit(hitComponent);
                }
            }
            hitOnSelectionHandle = false;
            justSelected = null;
        }
        
        
        /**
         * Make sure we don't create any loops
         */
        public boolean isValidParentTarget(LWComponent parentTarget) {
            if (parentTarget == null)
                return false;
            //if (dragComponent == draggedSelectionGroup && parentTarget.isSelected())
            if (parentTarget.isSelected())
                // meaning it's in the dragged selection, so it can never be a drop target
                return false;
            if (parentTarget == dragComponent)
                return false;
            if (parentTarget.getParent() == dragComponent)
                return false;
            if (parentTarget instanceof LWContainer == false || parentTarget instanceof LWMap)
                return false;
            return true;
        }
    }
    
    private Runnable focusIndicatorRepaint = new Runnable() { public void run() { mFocusIndicator.repaint(); }};
    
    /** VUE.activeViewerListener interface */
    public void activeViewerChanged(MapViewer viewer) {
        
        // We delay the repaint request for the focus indicator on this event because normally, it
        // happens while we're grabbing focus, which means it happens twice: once here on active
        // viewer change, and once later when we get the focusGained event.  Since the focus
        // indicator looks different in these two cases, it briefly flashes.  Delaying this paint
        // request ensures no flashing.  We still need to do this repaint on viewer change tho
        // because sometimes we ONLY see this event: e.g., if there is an active text edit (in
        // which cases we're the active viewer, but do NOT have keyboard focus), and then you mouse
        // over to another map, which then grabs the VUE application focus and becomes the active viewer.
        
        VUE.invokeAfterAWT(focusIndicatorRepaint);
    }
    
    /*
     * Make this viewer the active viewer (and thus our map the active map.
     * Does NOT call requestFocus to get the keyboard focus, as we don't
     * want to bother doing this if this is, say, from a focusEvent.
     */
    
    private void grabVueApplicationFocus(String from, ComponentEvent event) {
        if (DEBUG.FOCUS) {
            out("-------------------------------------------------------");
            out("GVAF: grabVueApplicationFocus triggered via " + from);
            if (DEBUG.META && event != null) System.out.println("\t" + event);
        }
        //tufts.macosx.Screen.dumpMainMenu();        
        this.VueSelection = VUE.ModelSelection;
        setFocusable(true);
        if (VUE.getActiveViewer() != this) {
            if (DEBUG.FOCUS) out("GVAF: " + from + " *** GRABBING ***");
            //new Throwable("REAL GRAB").printStackTrace();
            MapViewer activeViewer = VUE.getActiveViewer();
            // why are we checking this again if we just checked it???
            if (activeViewer != this) {
                LWMap oldActiveMap = null;
                if (activeViewer != null)
                    oldActiveMap = activeViewer.getMap();
                VUE.setActiveViewer(this);
                
                getMap().getChangeSupport().setPriorityListener(this);
                // TODO: VUE.getSelection().setPriorityListener(this);
                
                // hierarchy view switching: TODO: make an active map listener instead of this(?)
                if (VUE.getHierarchyTree() != null) {
                    if (this.map instanceof LWHierarchyMap)
                        VUE.getHierarchyTree().setHierarchyModel(((LWHierarchyMap)this.map).getHierarchyModel());
                    else
                        VUE.getHierarchyTree().setHierarchyModel(null);
                    // end of addition by Daisuke
                }
                
                if (oldActiveMap != this.map) {
                    if (DEBUG.FOCUS) out("GVAF: oldActive=" + oldActiveMap + " active=" + this.map + " CLEARING SELECTION");
                    resizeControl.active = false;
                    // clear and notify since the selected map changed.
                    VUE.ModelSelection.clear();
                    //VUE.ModelSelection.clearAndNotify(); // why must we force a notification here?
                }
            }
        } else {
            if (DEBUG.FOCUS) out("GVAF: already the active viewer");
        }
        if (DEBUG.FOCUS) {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != this)
                out("GVAF: current focus owner: " + focusOwner);
        }
        final int id = event == null ? 0 : event.getID();
        if (id == FocusEvent.FOCUS_GAINED || (id == MouseEvent.MOUSE_ENTERED && activeTextEdit != null)) {
            // in these cases, do NOT request the keyboard focus: either we just got it, our we
            // want to let an active on-map text edit keep it.
        } else {
            requestFocus();
        }
    }
    
    public void focusGained(FocusEvent e) {
        if (DEBUG.FOCUS) out("focusGained (from " + e.getOppositeComponent() + ")");
        repaintFocusIndicator();
        grabVueApplicationFocus("focusGained", e);
        fireViewerEvent(MapViewerEvent.FOCUSED);
    }
    
    public void focusLost(FocusEvent e) {
        if (DEBUG.FOCUS) out("focusLost (to " + e.getOppositeComponent() +")");
        
        Component lostTo = e.getOppositeComponent();
        
        if (VueUtil.isMacPlatform()) {
            
            // On Mac, our manual tool-tip popups sometimes (and sometimes inconsistently) when
            // they are a big heavy weight popups (e.g, 40 lines of notes) will actually grab the
            // focus away from the app!  We request to get the focus back, but it doesn't appear
            // that actually works.
            
            String opName = null;
            if (lostTo != null)
                opName = lostTo.getName();
            // hack: check the name against the special name of Popup$HeavyWeightWindow
            if (opName != null && opName.equals("###overrideRedirect###")) {
                if (DEBUG.FOCUS) System.out.println("\tLOST TO POPUP!");
                //requestFocus();
                // Actually, requestFocus can ADD to our problems if moving right from one rollover to another...
                // The bug is this: on Mac, rolling right from a tip that was HeavyWeight to one
                // that is LightWeight causes the second one (the light-weight) to appear then
                // immediately dissapear).
            }
        }
        
        // need to force revert on temporary tool here in case
        // they let go of the key while another component has focus
        // (e.g., a label edit, or another panel) in
        // which case we won't get the tool revert event.
        revertTemporaryTool();
        
        //if (activeTextEdit == null) // keep focus border even if our active text edit takes focus
            repaintFocusIndicator();
    }

    private void repaintFocusIndicator() {

        if (UseMacFocusBorder && inScrollPane && VueUtil.isMacAquaLookAndFeel()) {
            Component scrollPane = mViewport.getParent();
            if (DEBUG.FOCUS) out("repaintFocusIndicator " + scrollPane.getClass().getName());
            // this is slow because the whole map must also repaint
            scrollPane.repaint();
        } else
            mFocusIndicator.repaint();
    }
    
    public void setVisible(boolean doShow) {
        if (DEBUG.FOCUS) out("setVisible " + doShow);
        //if (!getParent().isVisible()) {
        if (doShow && getParent() == null) {
            if (DEBUG.FOCUS) out("IGNORING (parent null)");
            return;
        }
        super.setVisible(doShow);
        if (doShow) {
            // todo: only do this if we've just been opened
            //if (!isAnythingCurrentlyVisible())
            //zoomTool.setZoomFitContent(this);//todo: go thru the action
            setFocusable(true);
            grabVueApplicationFocus("setVisible", null);//requestFocus();
            fireViewerEvent(MapViewerEvent.DISPLAYED);
            // only need to do this if this viewer displaying a different MAP
            repaint();
        } else {
            setFocusable(false);
            fireViewerEvent(MapViewerEvent.HIDDEN);
        }
    }
    
    static class Box {
        Point ul = new Point(); // upper left corner
        Point lr = new Point(); // lower right corner
        int width;
        int height;
        
        public Box(Rectangle r) {
            ul.x = r.x;
            ul.y = r.y;
            lr.x = ul.x + r.width;
            lr.y = ul.y + r.height;
            width = r.width;
            height = r.width;
        }
        
        Rectangle getRect() {
            return new Rectangle(ul.x, ul.y, lr.x - ul.x, lr.y - ul.y);
        }
        
        // These set methods never let the box take negative width or height
        //void setULX(int x) { ul.x = (x > lr.x) ? lr.x : x; }
        //void setULY(int y) { ul.y = (y > lr.y) ? lr.y : y; }
        //void setLRX(int x) { lr.x = (x < ul.x) ? ul.x : x; }
        //void setLRY(int y) { lr.y = (y < ul.y) ? ul.y : y; }
    }
    
    
    public String toString() {
        return "MapViewer<" + instanceName + "> "
            + "\'" + (map==null?"nil":map.getLabel()) + "\'";
    }
    
    //-------------------------------------------------------
    // debugging stuff
    //-------------------------------------------------------
    
    static void installExampleNodes(LWMap map) {
        // create some test nodes & links
        if (false) {
            LWNode n1 = new LWNode("Test node1");
            LWNode n2 = new LWNode("Test node2");
            LWNode n3 = new LWNode("foo.txt");
            LWNode n4 = new LWNode("Tester Node Four");
            LWNode n5 = new LWNode("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
            LWNode n6 = new LWNode("abcdefghijklmnopqrstuvwxyz");
            
            n2.setResource("foo.jpg");
            n3.setResource("/tmp/foo.txt");
            n3.setNotes("I am a note.");
            
            n1.setLocation(100, 50);
            n2.setLocation(100, 100);
            n3.setLocation(100, 150);
            n4.setLocation(150, 150);
            n5.setLocation(150, 200);
            n6.setLocation(150, 250);
            //map.addNode(n1);
            //map.addNode(n2);
            //map.addNode(n3);
        }
            
        if (false) {
            // group resize testing
            map.addNode(new LWNode("aaa", 100,100));
            map.addNode(new LWNode("bbb", 150,130));
            map.addNode(new LWNode("ccc", 200,160));
        }
        if (true) {
            // node layout testing
            //map.addNode(new LWNode("PARENT CENTER", 50,50, new Ellipse2D.Float()));
            LWNode parent = new LWNode("PARENT CENTER *x", 0,0, new RectangularPoly2D.Diamond());
            parent.setStrokeWidth(0);
            parent.setFillColor(Color.lightGray);
            map.addNode(parent);
            map.addNode(new LWNode("child", 0,0, new Rectangle2D.Float())).setStrokeWidth(0);
            //map.addNode(new LWNode("c one", 100,100, new Rectangle2D.Float()));
            //map.addNode(new LWNode("c two", 150,130, new Rectangle2D.Float()));
            //map.addNode(new LWNode("PARENT BOX", 200,160, new Rectangle2D.Float())).setFillColor(Color.orange);
        }
        
        /*
        map.addNode(n4);
        map.addNode(n5);
        map.addNode(n6);
        map.addLink(new LWLink(n1, n2));
        map.addLink(new LWLink(n2, n3));
        map.addLink(new LWLink(n2, n4));
         
        map.addNode(new LWNode("One"));
        map.addNode(new LWNode("Two"));
        map.addNode(new LWNode("Three"));
        map.addNode(new LWNode("Four"));
        */
    }

    static void installZoomTestMap(LWMap map) {
        // for print testing & scroll testing
        LWNode n = new LWNode("+origin");
        n.setAutoSized(false);
        n.setShape(new Rectangle2D.Float());
        n.setStrokeWidth(0);
        n.setFrame(0,0, 100,25);
        n.setFillColor(Color.darkGray);
        n.setTextColor(Color.lightGray);
        Actions.FontBold.actOn(n);
        map.addNode(n);

        /*
        n = (LWNode) n.duplicate();
        n.setLabel("-origin");
        n.setFrame(-100,-25, 100,25);
        map.addNode(n);
        */

        n = (LWNode) n.duplicate();
        n.setFillColor(Color.gray);
        n.setTextColor(Color.black);
        n.setLabel("UL");
        n.setFrame(150,100, 50,50);
        map.addNode(n);

        n = (LWNode) n.duplicate();
        n.setLabel("LR");
        n.setLocation(200,150);
        map.addNode(n);

        /*
        LWNode center = new LWNode("center");
        center.setShape(new Rectangle2D.Float());
        center.setAutoSized(false);
        center.setFrame(200,125, 100,50);
        center.setStrokeWidth(0);
        center.setFillColor(Color.green);
        map.addNode(center);
        */

        LWNode end = new LWNode("400x300");
        end.setShape(new Rectangle2D.Float());
        end.setStrokeWidth(0);
        end.setAutoSized(false);
        end.setFrame(300,250, 100,50);
        end.setFillColor(Color.blue);
        Actions.FontBold.actOn(end);
        map.addNode(end);
    }
    
    
    private void out(Object o) {
        System.out.println(this + " " + (o==null?"null":o.toString()));
    }

    private String out(Point2D p) { return p==null?"<null Point2D>":(float)p.getX() + ", " + (float)p.getY(); }
    private String out(Rectangle2D r) { return ""
            + (float)r.getX() + ", " + (float)r.getY()
            + "  "
            + (float)r.getWidth() + " x " + (float)r.getHeight()
            ;
    }
    private String out(Dimension d) { return d.width + " x " + d.height; }
    
    private boolean DEBUG_MOUSE_MOTION = VueResources.getBool("mapViewer.debug.mouse_motion");//todo: make command line -D override these
    
    private boolean DEBUG_SHOW_ORIGIN = false;
    private boolean DEBUG_ANTI_ALIAS = true;
    private boolean DEBUG_RENDER_QUALITY = false;
    private boolean DEBUG_FINDPARENT_OFF = false;
    private boolean DEBUG_TIMER_ROLLOVER = false; // todo: preferences
    private boolean DEBUG_FONT_METRICS = false;// fractional metrics looks worse to me --SF
    private boolean OPTIMIZED_REPAINT = false;
    
    private Point _mouse = new Point();
    
    final Object AA_OFF = RenderingHints.VALUE_ANTIALIAS_OFF;
    Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;

    
    private static JFrame debugFrame;
    public static void main(String[] args) {
        System.out.println("MapViewer:main");
        
        DEBUG.Enabled = true;
        VUE.parseArgs(args);

        boolean test_zoom = false;
        boolean test_node = false;
        boolean show_panner = false;
        boolean use_scroller = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-zoom"))
                test_zoom = true;
            else if (args[i].equals("-node"))
                test_node = true;
            else if (args[i].equals("-panner"))
                show_panner = true;
            else if (args[i].equals("-scroll"))
                use_scroller = true;
        }
        
        
        javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new VueTheme(false) {
                public javax.swing.plaf.FontUIResource getControlTextFont() { return fontTiny; }
                public javax.swing.plaf.FontUIResource getMenuTextFont() { return fontTiny; }
                public javax.swing.plaf.FontUIResource getSmallFont() { return fontTiny; }
            });
            
        LWMap map = new LWMap("test");
        
        if (test_zoom) {
            DEBUG.EVENTS = DEBUG.SCROLL = DEBUG.VIEWER = DEBUG.MARGINS = true; // zoom test
            DEBUG.KEYS = DEBUG.MOUSE = true;
            installZoomTestMap(map);
        } else if (test_node) {
            DEBUG.BOXES = true; // node layout test
            installExampleNodes(map);
        }
        
        JFrame frame = null;
        
        if (test_zoom == false) {
            // raw, simple, non-scrolled mapviewer (WITHOUT actions attached!)
            DEBUG.FOCUS = true;
            VueUtil.displayComponent(new MapViewer(map), 400,300);

        } else {

            MapViewer viewer = new MapViewer(map);
            viewer.DEBUG_SHOW_ORIGIN = true;
            viewer.DEBUG_TIMER_ROLLOVER = false;
            viewer.setPreferredSize(new Dimension(500,300));
            if (use_scroller) {
                JScrollPane scrollPane = new JScrollPane(viewer);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                scrollPane.setWheelScrollingEnabled(false);
                frame = VueUtil.displayComponent(scrollPane);
            } else {
                frame = VueUtil.displayComponent(viewer);
            }
            JMenuBar menu = new VUE.VueMenuBar(null);
            menu.setFont(FONT_TINY);
            // set the menu bar just so we can get all the actions connected to MapViewer
            frame.setJMenuBar(menu);
            frame.pack();
            debugFrame = frame;
        }
            
        if (test_zoom || show_panner) {
            ToolWindow pannerTool = new ToolWindow("Panner", frame);
            pannerTool.setSize(120,120);
            pannerTool.addTool(new MapPanner());
            pannerTool.setVisible(true);
        }
    }
    
}


