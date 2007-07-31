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

import tufts.Util;
import static tufts.Util.*;

import tufts.vue.gui.GUI;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.FocusManager;
import tufts.vue.gui.MapScrollPane;
import tufts.vue.gui.TimedASComponent;
import tufts.vue.gui.WindowDisplayAction;
import tufts.vue.NodeTool;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.geom.*;

import java.util.*;

import javax.swing.*;

import edu.tufts.vue.preferences.implementations.BooleanPreference;
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.ColorPreference;

import tufts.oki.dr.fedora.*;
import tufts.vue.shape.*;

import osid.dr.*;

/**
 * Implements a component for displaying & interacting with an instance of LWMap.
 * Handles drawing the LWSelection & providing interaction with it.  Provides for moving
 * LWNode's around, dropping them on other LWNodes as children.  Provides context
 * menus. Defers to the active tool for the current cursor, as well as what to when
 * dragging out a selector-box.
 *
 * Implemented as a swing JComponent to be sure to get double-buffering on the PC (is
 * automatic on Mac), and because of course the rest of VUE uses Swing.
 *
 * Note that all the mapToScreen & screenToMap conversion routines would have been more
 * aptly name canvasToMap and mapToCanvas, as they no longer represent actualy on-screen
 * (Panel) locations once the viewer has been put into a JScrollPane.  (If not running
 * in a scroll-pane, they original semantics still apply).
 *
 * @author Scott Fraize
 * @version $Revision: 1.422 $ / $Date: 2007-07-31 02:04:44 $ / $Author: sfraize $ 
 */

// Note: you'll see a bunch of code for repaint optimzation, which is not a complete
// feature, and is not turned on.

// TODO: this class is offically a major mess.  The key/mouse input, tool delegation,
// selection and dragging code all need a full visit to refactoring rehab.


public class MapViewer extends TimedASComponent//javax.swing.JComponent
    implements VueConstants
               , FocusListener
               , LWComponent.Listener
               , LWSelection.Listener
               //, VueToolSelectionListener
               //, DragGestureListener
               //, DragSourceListener
               , java.awt.event.KeyListener
               , java.awt.event.MouseListener
               , java.awt.event.MouseMotionListener
               , java.awt.event.MouseWheelListener               
{
    static int RolloverAutoZoomDelay = VueResources.getInt("mapViewer.rolloverAutoZoomDelay");
    //static int RolloverAutoZoomDelay = 1;
    //static final int RolloverMinZoomDeltaTrigger_int = VueResources.getInt("mapViewer.rolloverMinZoomDeltaTrigger", 10);
    //static final float RolloverMinZoomDeltaTrigger = RolloverMinZoomDeltaTrigger_int > 0 ? RolloverMinZoomDeltaTrigger_int / 100f : 0f;
    private static boolean autoZoomEnabled = PreferencesManager.getBooleanPrefValue(edu.tufts.vue.preferences.implementations.AutoZoomPreference.getInstance());

    /** automatically zoom-fit to map contents on new map load */
    private static final boolean AutoZoomToMapOnLoad = true;
    
    private Rectangle2D.Float RepaintRegion = null; // could handle in DrawContext
    private Rectangle paintedSelectionBounds = null;
    
    public interface Listener extends java.util.EventListener {
        public void mapViewerEventRaised(MapViewerEvent e);
    }

    /** The component we're currently displaying: usually an instanceof LWMap, unless presenting */
    protected LWComponent mFocal;
    /** The top-level map that owns the focal (usually the same as the focal) */
    protected LWMap mMap;
    /** The focal we just unloaded if any */
    protected LWComponent mLastFocal;
    /** Current on-map text edit, null if no edit active */
    protected TextBox activeTextEdit;
    
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
    protected final LWGroup draggedSelectionGroup = LWGroup.createTemporary(VUE.ModelSelection);
    /** the currently dragged selection box */
    protected Rectangle draggedSelectorBox;
    /** the last selector box drawn -- for repaint optimization */
    protected Rectangle lastPaintedSelectorBox;
    /** are we currently dragging a selection box? */
    protected boolean isDraggingSelectorBox;
    /** are we currently in a drag of any kind? (mouseDragged being called) */
    protected static boolean sDragUnderway;
    //protected Point2D.Float dragPosition = new Point2D.Float();
    
    protected static LWComponent indication;   // current indication (drag rollover hilite -- ONLY ONE PER ALL MAPS)
    
    private MapDropTarget mapDropTarget;
    
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
    protected VueTool activeTool;
    
    // todo: we should get rid of hard references to all the tools and handle functionality via tool API's
    private final VueTool HandTool = VueTool.getInstance(tufts.vue.HandTool.class);
    private final VueTool LinkTool = VueTool.getInstance(tufts.vue.LinkTool.class);
    private final VueTool TextTool = VueTool.getInstance(tufts.vue.TextTool.class);
    private final NodeTool NodeTool = (NodeTool) VueTool.getInstance(tufts.vue.NodeTool.class);

    //-------------------------------------------------------
    // Scroll-pane support
    //-------------------------------------------------------
    
    private boolean inScrollPane = false;
    private MapViewport mViewport;
    private boolean isFirstReshape = true;
    private boolean didReshapeZoomFit = false;
    private Component mFocusIndicator = new java.awt.Canvas(); // make sure is never null
    private boolean mFitToFocalRequested = false;

    //private InputHandler inputHandler = new InputHandler(this);
    private final MapViewer inputHandler; // == this
    private final MapViewer viewer;  // == this: for old InputHandler references

    //1 click node creation preference
    private final static BooleanPreference oneClickNodePref = BooleanPreference.create(
			edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
			"oneClickCreation", 
			"Node Creation", 
			"Enable one click node creation?",
			Boolean.FALSE,
			true);
    
    public MapViewer(LWMap map) {
        this(map, "");
    }
    
    private final String instanceName;
    public MapViewer(LWMap map, String instanceName)
    {
        this.instanceName = instanceName;
        this.activeTool = VueToolbarController.getActiveTool();
        if (activeTool == null) {
            // default tool is first in list
            activeTool = VueTool.getTools().get(0);
        }
        this.mapDropTarget = new MapDropTarget(this); // new CanvasDropHandler
        this.setDropTarget(new java.awt.dnd.DropTarget(this,
                                                       MapDropTarget.ACCEPTABLE_DROP_TYPES,
                                                       mapDropTarget));
        this.inputHandler = this;
        this.viewer = this;
        setName(instanceName);
        //setFocusable(false);
        setOpaque(true);
        setLayout(null);

        if (map != null) {
            //if (map.getFillColor() != null) setBackground(map.getFillColor());
            loadFocal(map);
        
            //-------------------------------------------------------
            // If this map was just restored, there might
            // have been an existing userZoom or userOrigin
            // set -- we honor that last user configuration here.
            //-------------------------------------------------------
            if (!AutoZoomToMapOnLoad && map.getUserZoom() != 1.0)
                setZoomFactor(getMap().getUserZoom(), false, null, false);
        }

        // TODO: need to remove us as listeners for everything if this viewer is closed!
        
        VUE.ModelSelection.addListener(this);
        VUE.addActiveListener(MapViewer.class, this);
        //VUE.addActiveListener(LWMap.class, this);
        VUE.addActiveListener(VueTool.class, this);
        //VueToolbarController.getController().addToolSelectionListener(this);        
        
        // draggedSelectionGroup is always a selected component as
        // it's only used when it IS the selection
        // There was some reason we need to have the set -- what was it?
        draggedSelectionGroup.setSelected(true);
        
        
        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        //this.add
        edu.tufts.vue.preferences.implementations.AutoZoomPreference.getInstance().addVuePrefListener(new VuePrefListener(){

    		public void preferenceChanged(VuePrefEvent prefEvent) {
    						
    			autoZoomEnabled = ((Boolean)prefEvent.getNewValue()).booleanValue();    			
    		}
        	   
           });
        
        if (DEBUG.INIT||DEBUG.FOCUS) out("CONSTRUCTED.");
    }

    boolean inScrollPane() {
        return inScrollPane;
    }

    // TODO: rework this due to fact this get's added/removed again
    // during full-screen swaps: could the focus stuff have been
    // messing us up?
    
    public void addNotify()
    {
        //VUE.Log.debug("addNotify(pre): " + this);
        super.addNotify();
        //VUE.Log.debug("addNotify(top): " + this + "; new parent=" + getParent());

        inScrollPane = (getParent() instanceof JViewport);

        if (inScrollPane) {
            if (getParent() instanceof MapViewport == false)
                throw new IllegalStateException("MapViewer will only work in ScrollPane if using MapViewport");
            
            mViewport = (MapViewport) getParent();
            setAutoscrolls(true);

            MapScrollPane mapScrollPane = (MapScrollPane) mViewport.getParent();
            mFocusIndicator = mapScrollPane.getFocusIndicator();

            // TODO: need to install the MouseWheelRelay here, as we get added/removed notify
            // whenever we go to full-screen mode, and the MapScrollPane is losing the
            // MouseWheelListener (do we want to use a different viewer for full screen?)
            
        } else {
            mViewport = null;

            // Only do this if not in a scroll-pane.  If we are,
            // it will add us, creating a relay so it can process
            // normaly any events we don't consume.
            addMouseWheelListener(getMouseWheelListener());
        }
        
        addFocusListener(this);
        
        if (mMap != null && mMap == mFocal) {
            Point2D p = mMap.getUserOrigin();
            setMapOriginOffset(p.getX(), p.getY());
        }

        requestFocus();
        //VUE.invokeAfterAWT(new Runnable() { public void run() { ensureMapVisible(); }});
    }

    public LWSelection getSelection() {
        return VueSelection;
    }

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
    
    
    public void activeChanged(ActiveEvent e, VueTool tool) {
        activateTool(tool, tool.isTemporary());
    }

    private void activateTool(VueTool tool) {
        activateTool(tool, false);
    }
    
    /**
     * Sets the current active VueTool in the MapViewer.
     * E.g., will update the current cursor, allow the
     * current tool to process mouse & key events, effect
     * what's draw, etc.
     **/
    private void activateTool(VueTool tool, boolean temporary)
    {
        if (DEBUG.FOCUS && VUE.getActiveViewer() == this) out("activateTool: " + tool.getID());
        
        if (tool == null) {
            System.err.println(this + " *** toolSelected: NULL TOOL");
            return;
        }
        if (tool.getID() == null) {
            System.err.println(this + " *** toolSelected: NULL ID IN TOOL!");
            return;
        }
        
        VueTool oldTool = activeTool; // might be safer to pull this from the ActiveEvent
        activeTool = tool;
        activeTool.setTemporary(temporary);
        setMapCursor(activeTool.getCursor());
        
        if (isDraggingSelectorBox) // in case we change tool via kbd shortcut in the middle of a drag
            repaint();
        else if (oldTool != null && oldTool.hasDecorations() || tool.hasDecorations())
            repaint();

        VUE.setActive(VueTool.class, this, tool);
    }
    
    private void setMapCursor(Cursor cursor) {
        //JRootPane rootPane = SwingUtilities.getRootPane(this);
        //if (DEBUG.FOCUS) out("setting cursor for RootPane " + GUI.name(rootPane));
        //rootPane.setCursor(cursor);
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
    
    public void fireViewerEvent(int id) {
        // TODO: REALLY need to change this from using EventRaiser -- esp given
        // how often this has to fire during animations!
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
        if (inScrollPane) {
            if (mFocal.hasContent())
                mViewport.adjustSize(expand, trimNorthWest, trimSouthEast);
            else
                mViewport.adjustSize(false, true, true);
        }
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

        if (!mFocal.hasContent()) {
            if (DEBUG.SCROLL) out("EMPTY OVERRIDE");
            //pReset = true;
            pZoomFactor = 1.0;
        }
        
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
        if (mFocal == mMap)
            mMap.setUserZoom(mZoomFactor);
        
        //------------------------------------------------------------------
        
        if (inScrollPane) {
            if (mapAnchor != null && !pReset) {
                mViewport.zoomAdjust(mapAnchor, screenPositionOfMapAnchor);
            } else {
                adjustCanvasSize(false, true, true);
            }
        } else {
            if (mapAnchor != null && offset != null)
                setMapOriginOffset(offset.x, offset.y);
        }
        
        repaint();
        fireViewerEvent(MapViewerEvent.ZOOM);
    }
    

    void panScrollRegion(int dx, int dy) {
        panScrollRegion(dx, dy, true);
    }
    
    void panScrollRegion(int dx, int dy, boolean allowGrowth) {
        panScrollRegionImpl(dx, dy, allowGrowth);
    }
    
    protected void panScrollRegionImpl(int dx, int dy, boolean allowGrowth) {
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
        setMapOriginOffsetImpl(panelX, panelY, update);
    }
    
    protected void setMapOriginOffsetImpl(float panelX, float panelY, boolean update) {
        if (DEBUG.SCROLL) out("setMapOriginOffset old:" + out(mOffset));
        if (DEBUG.SCROLL) out("setMapOriginOffset new:" + panelX + ", " + panelY);
        mOffset.x = panelX;
        mOffset.y = panelY;
        // todo: when in scroll region, user origin being offset 12 or so pixels
        // (probably width of scroll bar) -- would be nice to keep normalized to 0
        // so doesn't always offset it (will it do that cumulative every time we start??)
        if (mMap == mFocal) {
            if (VUE.getActiveViewer() == this)
                mMap.setUserOrigin(panelX, panelY);
            if (!inScrollPane && update) {
                repaint();
                fireViewerEvent(MapViewerEvent.PAN);
            }
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
    protected float screenToMapX(float x) {
        return (float) ((x + getOriginX()) * mZoomInverse);
    }
    protected float screenToMapY(float y) {
        return (float) ((y + getOriginY()) * mZoomInverse);
    }
    float screenToMapX(double x) {
        return (float) ((x + getOriginX()) * mZoomInverse);
    }
    float screenToMapY(double y) {
        return (float) ((y + getOriginY()) * mZoomInverse);
    }
    int mapToScreenX(double x) {
        return (int) (0.5 + ((x * mZoomFactor) - getOriginX()));
    }
    int mapToScreenY(double y) {
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
        

        screenRect.x = (int) Math.floor(mapRect.getX() * mZoomFactor - getOriginX());
        screenRect.y = (int) Math.floor(mapRect.getY() * mZoomFactor - getOriginY());
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

        screenRect.x = (float) (mapRect.getX() * mZoomFactor - getOriginX());
        screenRect.y = (float) (mapRect.getY() * mZoomFactor - getOriginY());
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
        if (DEBUG.SCROLL) out("visible size: " + out(d));
        return d;
    }
    
    /**
     * When in a JScrollPane, the currently visible portion of the
     * MapViewer canvas.  When not in a scroll pane, it's just
     * the size of the component (and x=y=0);
     */
    public Rectangle getVisibleBounds() {
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
        /*
        LWSlide slide;
        // TODO: temporary hack until / if slides actualy go on the map
        if (mFocal instanceof LWSlide)
            slide = (LWSlide) mFocal;
        else
            slide = (LWSlide) mFocal.getAncestorOfType(LWSlide.class);
        
        if (slide != null) {
            final LWComponent node = slide.getSourceNode();
            final Rectangle2D bounds = node.getBounds();
            if (node.isDrawingSlideIcon()) {
                // hack for slides which aren't really on the map: for MapPanner
                return bounds.createUnion(node.getMapSlideIconBounds());
            } else
                return bounds;
        } else
        */
        
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
    
    /** @return the bounds of all the objects in the map this viewer is currently
     * configured to be able to display 
     */
    public Rectangle2D getDisplayableMapBounds() {
        return getFocalBounds();
//         if (mFocal == mMap)
//             return mMap.getBounds();
//         //return mMap.getBounds(getMaxLayer());
//         else
//             return mFocal.getShapeBounds();
    }

    public static Rectangle2D.Float getFocalBounds(LWComponent c) {
        // TODO: add a getFocalBounds to LWComponent, and override in LWLink
        if (c instanceof LWLink)
            return c.getFanBounds();
        else
            return c.getBounds();
    }
    
    private Rectangle2D.Float getFocalBounds() {
        return getFocalBounds(mFocal);
    }

    private Shape getFocalClip() {
        if (mFocal instanceof LWLink) {
            Util.printStackTrace("Warning: use of link focal clip in " + this + "; focal=" + mFocal);
            return mFocal.getParent().getBounds();
        } else
            return mFocal.getMapShape();
    }

    /**
     * Return, in Map coords, a bounding box for all the LWComponents in the
     * displayed map, including room for possible selection handles or
     * rendered selection highlights.  Will in effect be just a bit
     * bigger than getMap().getBounds(). todo: account for zoom?
     */
    
    private final static float SelectionStrokeMargin = SelectionStrokeWidth/2;
    public Rectangle2D.Float getContentBounds() {
        //Rectangle2D.Float r = (Rectangle2D.Float) mFocal.getBounds().clone();
        Rectangle2D.Float r = (Rectangle2D.Float) getFocalBounds().clone();
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
        try {
            if (e instanceof MouseEvent) {
                super.processEvent(e);
                return;
            }
            if (DEBUG.VIEWER) out("MAPVIEWER: processEvent " + e);
            super.processEvent(e);
        } catch (Throwable t) {
            Util.printStackTrace(t, "MapViewer failed processing event " + e);
        }
    }
    
    public void reshape(int x, int y, int w, int h) {
        boolean ignore =
            getX() == x &&
            getY() == y &&
            getWidth() == w &&
            getHeight() == h;

        // We get reshape events during text edits with no change
        // in size, yet are crucial for repaint update (thus: no ignore if activeTextEdit)
        
        if (DEBUG.SCROLL||DEBUG.PAINT||DEBUG.EVENTS||DEBUG.FOCUS||DEBUG.VIEWER) {
            out("reshape",
                w + " x " + h
                + " "
                + x + "," + y
                + (ignore?" (IGNORING)":""));
            //Util.printStackTrace("reshape");
        }

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

        if (!ignore) {
            if (reshapeUnderway) {
                if (DEBUG.VIEWER) out("RESHAPE UNDERWAY");
            } else {
                reshapeUnderway = true;
                try {
                    //fireViewerEvent(MapViewerEvent.PAN); // fire AFTER reshapeImpl, and better, IN reshapeImpl
                    reshapeImpl(x,y,w,h);
                } finally {
                    reshapeUnderway = false;
                }
            }
        }
    }

    private boolean reshapeUnderway = false;
    protected void reshapeImpl(int x, int y, int w, int h)
    {
        if (DEBUG.VIEWER) out("reshapeImpl");
        if (!mFitToFocalRequested && (mFocal == null || mFocal instanceof LWMap)) {
            //if (DEBUG.PRESENT) out("reshapeImpl: skipped");
            //Util.printStackTrace("reshapeImpl");
            return;
        }
        mFitToFocalRequested = false;
        fitToFocal();
    }

    private boolean zoomFitUnderway = false;
    protected void fitToFocal() {
        if (zoomFitUnderway) {
            //out("ZOOM UNDERWAY");
            return;
        }
        
        if (getVisibleWidth() == 0 || getVisibleHeight() == 0) {
            if (DEBUG.PRESENT) out("requesting delayed autoZoom; visSize=" + getVisibleSize());
            mFitToFocalRequested = true;
            return;
        }
            
        zoomFitUnderway = true;
        try {
            doFitToFocal();
        } finally {
            zoomFitUnderway = false;
        }
    }
    
    private void doFitToFocal()
    {
        if (DEBUG.PRESENT || DEBUG.VIEWER) out("fitToFocal", mFocal);
        mFitToFocalRequested = false;

        if (mFocal == null) {
            // can happen if no maps open
            return;
        }

        final boolean animate = false;
        final Rectangle2D zoomBounds = getFocalBounds();

//         int margin = 30;
//         if (mFocal instanceof LWSlide ||
//             mFocal instanceof LWPortal ||
//             mFocal instanceof LWImage
//             )
//             margin = 0;

        if (DEBUG.PRESENT || DEBUG.VIEWER) out("fitToFocal", mFocal + " at " + zoomBounds);
        
        ZoomTool.setZoomFitRegion(this,
                                  zoomBounds,
                                  mFocal.getFocalMargin(),
                                  animate);
        
    }

    
    
    

    /** at startup make sure the contents of the map are visible in the viewport */
    private void ensureMapVisible()
    {
        if (mMap == mFocal && mMap != null && mMap.hasChildren()) {
            final int visibleObjects = computeSelection(getVisibleMapBounds()).size();
            if (DEBUG.INIT || DEBUG.VIEWER) out("i see " + visibleObjects + " components in visible map bounds " + getVisibleMapBounds());
            if (visibleObjects == 0)
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
    /** last place mouse pressed in map coords */
    public Point2D.Float getLastMousePressMapPoint() {
        return screenToMapPoint(lastMousePressX, lastMousePressY);
    }
    
    private void setLastMousePoint(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
    }
    
    /** last place mouse was either pressed or released (canvas coordinates) */
    public Point getLastMousePoint() {
        return new Point(lastMouseX, lastMouseY);
    }

    /** last place mouse was either pressed or released (map coordinates) */
    public Point2D.Float getLastMapMousePoint() {
        return screenToMapPoint(getLastMousePoint());
    }
    
    public LWMap getMap() {// TODO: make PRIVATE and then clean up to use focals instead as needed (e.g., for drops)
        return mMap == null ? (mFocal == null ? null : mFocal.getMap()) : mMap;
    }
    
    public LWComponent getFocal() {
        return mFocal;
    }

    private void unloadFocal() {
        mFocal.removeLWCListener(this);
        mMap = null;
        mFocal = null;
        mOffset.x = mOffset.y = 0;
    }
    
    /** let the active tool handle the focal transition if it wants */
    public void switchFocal(LWComponent newFocal) {
        if (DEBUG.PRESENT || DEBUG.VIEWER || DEBUG.WORK) out("switchFocal", newFocal);
        if (activeTool != null && activeTool.handleFocalSwitch(this, mFocal, newFocal)) {
            if (DEBUG.PRESENT || DEBUG.VIEWER || DEBUG.WORK) out("switchFocal", "activeTool handled: " + activeTool);
            ; // the active tool has handled the focal loading and any desired auto-fit
        } else {
            if (DEBUG.PRESENT || DEBUG.VIEWER || DEBUG.WORK) out("switchFocal", "vanilla load " + newFocal);
            loadFocal(newFocal);
        }
        
    }
        
    /** actualy load the new focal */
    public void loadFocal(LWComponent focal) {
        loadFocal(focal, true);
    }
    
    /** actualy load the new focal */
    public void loadFocal(LWComponent focal, boolean fitToFocal) {
        if (DEBUG.PRESENT || DEBUG.VIEWER || DEBUG.WORK) out("loadFocal", focal + "; autoFit=" + fitToFocal);
        //if (focal == null) throw new IllegalArgumentException(this + " loadFocal: focal is null");
        if (mFocal == focal) {
            if (fitToFocal)
                fitToFocal();
            return;
        }

        mLastFocal = mFocal;

        if (mFocal != null) {
            unloadFocal();
            // If we are switching from another focal, automatically do a zoom-fit
            //autoZoom = true;
        } else if (!(focal instanceof LWMap))
            ;//autoZoom = true;

        // todo: we should adjust offset to leave view position at same location
        // within the whole map (if we're moving up the heirarchy within a single map)
        //mOffset.x = mOffset.y = 0;
        mFocal = focal;

        if (mFocal != null) {
            mMap = mFocal.getMap();
            if (mMap == null)
                tufts.Util.printStackTrace("no map in focal! " + mFocal);
            mFocal.addLWCListener(this);
            
        } else
            mMap = null;
        
        if (mMap != null && mMap.getUndoManager() == null) {
            if (mMap.isModified()) {
                out("Note: this map has modifications undo will not see");
                //VueUtil.alert(this, "This map has modifications undo will not see.", "Note");
            }
            mMap.setUndoManager(new UndoManager(mMap));
        }

        //if (AutoZoomToMapOnLoad || autoZoom) {
        if (fitToFocal) {
            fitToFocal();
        }
        
        if (focal == null)
            repaint();
    }

    public void popToMapFocal() {
        popFocal(true);
    }

    protected boolean popFocal() {
        return popFocal(false);
    }
    
    /** refocus the viewer on the parent of the curent focal
     * @return true if we were able to change the focal
     */
    protected boolean popFocal(boolean toTopLevel)
    {
        if (DEBUG.PRESENT || DEBUG.WORK) out("popFocal", "up from " + mFocal + "; toTop=" + toTopLevel);
        if (mFocal == null)
            return false;

        if (toTopLevel) {
            switchFocal(mFocal.getMap());
            return true;
        }
        
        LWComponent parent = mFocal.getParent();
        if (parent instanceof LWPathway)
            switchFocal(parent.getMap());
        else if (parent != null)
            switchFocal(parent);
        else
            return false;
        
        return true;
    }
    

    /*
    void setFocal(LWComponent focal) {
        if (focal == null) {
            if (mFocal != null)
                mFocal.removeLWCListener(this);
            mFocal = mMap;
            mMap.addLWCListener(this);
        } else {
            mFocal = focal;
            mFocal.addLWCListener(this);
            mMap.removeLWCListener(this);
        }
    }
    
    private void unloadMap() {
        mMap.removeLWCListener(this);
        mMap = null;
        mFocal = null;
    }
    
    protected void loadMap(LWMap map) {
        if (map == null)
            throw new IllegalArgumentException(this + " loadMap: null LWMap");
        if (mMap == map)
            return;
        if (mMap != null)
            unloadMap();
        mMap = map;
        mFocal = map;
        mMap.addLWCListener(this);
        if (mMap.getUndoManager() == null) {
            if (mMap.isModified()) {
                out("Note: this map has modifications undo will not see");
                //VueUtil.alert(this, "This map has modifications undo will not see.", "Note");
            }
            mMap.setUndoManager(new UndoManager(mMap));
        }
        repaint();
    }
    */

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

        if (VUE.getActiveMap() != mMap) {
            if (DEBUG.FOCUS) out("NULLING SELECTION");
            VueSelection = null; // insurance: nothing should be happening here if we're not active
        } else {
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
    
    private boolean isBoundsEvent(Object k)
    {
        //if (true) return k == LWKey.UserActionCompleted || k == LWKey.Location;
        
        return k != LWKey.HierarchyChanging
            && k != LWKey.ChildrenRemoved
                ;
        /*
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
        */
    }
    
    /**
     * Handle events coming off the LWMap we're displaying.
     * For most actions this repaints.  It tracks deletiions
     * for updating the current rollover zoom.
     */
    public void LWCChanged(LWCEvent e) {
        if (DEBUG.EVENTS && DEBUG.META) out("LWCChanged: " + e);
        
        // If mFocal isn't a map, we must always update, as we'll never see the
        // user-action-completed off the map, as we're not listening to it.  (Actually,
        // that would be easy to fix: also listen to the focal's map, but this updating
        // is better anyway...)
//         if (mFocal == mMap && DEBUG.DYNAMIC_UPDATE == false) {
//             if (key == LWKey.RepaintAsync) {
//                 repaint();
//                 return;
//             } else if (VUE.getActiveViewer() != this) {
//                 // this prevents other viewers of same map from updating until an
//                 // action is completed in the active viewer.
//                 if (sDragUnderway || key != LWKey.UserActionCompleted)
//                     return;
//             } else {
//                 // The ACTIVE viewer can ignore these events,
//                 // because we've been repainting all the updates
//                 // due to events as they've been happening.
//                 if (key == LWKey.UserActionCompleted)
//                     return;
//             }
//         }

        if (e.key == LWKey.RepaintAsync) {
            repaint();
            return;
        }

        if (mMap != null && mMap.getUndoManager().hasCleanupTasks()) {
            // once we have cleanup tasks, we're in an intermediate state:
            // don't ever draw until we're complete.
            return;
        }
        
        
        // ? todo: optimize -- we get lots of extra location events
        // when dragging if there are children of the dragged
        // object (still true?)
        
        if (isBoundsEvent(e.key))
            adjustCanvasSize();
        
        if (e.key == LWKey.Deleting) {
            if (mRollover == e.getComponent())
                clearRollover();
//         } else if (e.key == LWKey.FillColor && e.getComponent() == mMap && mFocal == mMap) {
//             setBackground(mMap.getFillColor());
        } else if (e.key == LWKey.Hidden) {
            if (e.getComponent().isHidden() && e.getComponent().isSelected()) {
                VueSelection.remove(e.getComponent());
            } else
                repaint();
            return;
        }

        if (e.key == LWKey.RepaintComponent) {
            Rectangle r = mapToScreenRect(e.getComponent().getBounds());
            super.paintImmediately(r);
            //super.repaint(0,r.x,r.y,r.width,r.height);            
        } else if (OPTIMIZED_REPAINT == false) {
            LWComponent singleSrc = e.onlyComponent();
            if (singleSrc != null && singleSrc.isHidden() && !(singleSrc instanceof LWPathway)) {
                // todo: some kind of semantic check that knows pathway visibility
                // is irrelevant here, as opposed to the type check.
                if (DEBUG.Enabled) out("skipping update from hidden component: " + e);
            } else
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
    
    private java.util.List computeSelection(final Rectangle2D mapRect)
    {
        PickContext pc = getPickContext((Rectangle2D.Float) mapRect);

        return LWTraversal.RegionPick.pick(pc);
    }

    
    void setIndicated(LWComponent c) {
        if (indication == c)
            return;

        if (c == null) {
            clearIndicated();
            return;
        }

        if (c instanceof LWSlide && !c.isMoveable()) {
            //if (c instanceof LWSlide && mFocal != c) {

            // We never want to indicate the slide-icon on the main map for any reason,
            // as it's not really "there" right now.  E.g., you can't link to, or drop
            // objects into it, etc.  This is still allowed for slides that completely
            // own the viewer (they are the focal, e.g. as in SlideViewer) The
            // slide-icons can still be picked and selected via LWComponent hacks, but
            // we never want them indicated;

            // Actually, we never need the slide indicated at all for the moment:
            // no need to show it in the SlideViewer either.
        
            return;
        }
            
        clearIndicated();
        indication = c;
        if (DEBUG.PARENTING) out("indication  set  to " + c);
        //c.setIndicated(true);
        if (indication.getStrokeWidth() < STROKE_INDICATION.getLineWidth())
            repaintMapRegionGrown(indication.getBounds(), STROKE_INDICATION.getLineWidth());
        else
            repaintMapRegion(indication.getBounds());
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

    protected PickContext getPickContext(Point2D p) {
        return getPickContext((float)p.getX(), (float)p.getY());
    }
    
    private PickContext initPickContext(PickContext pc) {
        pc.zoom = (float) mZoomFactor;
        pc.root = mFocal;
        pc.acceptor = activeTool;
        //pc.maxLayer = getMaxLayer();
        if (mFocal != null) {
            // always allow picking through to children of the focal
            pc.pickDepth = mFocal.getPickLevel();
        }
        //pc.pickDepth = (mFocal == mMap) ? 0 : 1;
        return pc;
    }

    protected PickContext getPickContext(float x, float y)
    {
        final PickContext pc = initPickContext(new PickContext(x, y));
        
        if (mFocal instanceof LWPortal) {
            // we can pick right through the portal to the underlying map by using using
            // the map as the pick root (instead of the portal which would be useless
            // because they're always empty), and ensuring the portal is invisible to
            // the the pick (excluded).
            pc.root = mFocal.getMap();
            pc.excluded = mFocal;
        }

        return activeTool.initPick(pc, x, y);
    }
    
    protected PickContext getPickContext(Rectangle2D.Float rect)
    {
        final PickContext pc = initPickContext(new PickContext(rect));
        
        // never pick the focal for a dragged selection -- it's the background
        pc.excluded = mFocal;
        // for rectangular picks, only ever pick top-level items (no children)
        pc.maxDepth = 1;
        
        return activeTool.initPick(pc, rect);
    }
        
//     protected int getMaxLayer() {
//         return 0;
//     }

//     protected int getPickDepth() {
//         if (activeTool == DirectSelectTool) // todo: hand to the tool for PickContext modifications
//             return 1; //Short.MAX_VALUE;
//         else if (mFocal != mMap)
//             return 1; //Short.MAX_VALUE; // auto-deep pick for any non-map focal
//         //else if (!inScrollPane())
//         //    return 1; // todo: temporary hack for presentations -- find a clearer way
//         else
//             return 0;
//     }


    
    public LWComponent pickNode(Point2D.Float p) {
        return pickNode(p.x, p.y);
    }
    public LWComponent pickDropTarget(Point2D.Float p, Object dropping) {
        return pickDropTarget(p.x, p.y, dropping);
    }

    
    public LWComponent pickNode(float mapX, float mapY) {
        if (DEBUG.PICK) out("pickNode " + mapX + "," + mapY);
        return pick(mapX, mapY, false);
    }
    
    private static final LWComponent POSSIBLE_NODE = new LWComponent();
    private static final Object POSSIBLE_RESOURCE = new Object();
    
    public LWComponent pickDropTarget(float mapX, float mapY, Object dropping) {
        PickContext pc = getPickContext(mapX, mapY);
        if (dropping == null)
            pc.dropping = POSSIBLE_RESOURCE; // most lenient targeting if unknown
        else
            pc.dropping = dropping;
        LWComponent hit = LWTraversal.PointPick.pick(pc);
        if (hit != null && hit.supportsChildren()) {
            if (dropping instanceof LWComponent)
                return ((LWComponent)dropping).supportsReparenting() ? hit : null;
            else
                return hit;
        } else
            return null;
    }

    
    protected LWComponent pick(float mapX, float mapY, boolean ignoreSelected)
    {
        PickContext pc = getPickContext(mapX, mapY);
        pc.ignoreSelected = ignoreSelected;
        return LWTraversal.PointPick.pick(pc);
            
        /*
        if (mFocal instanceof LWContainer) {
            // we use deepest to penetrate into groups
            //return ((LWContainer)mFocal).findDeepestChildAt(mapX, mapY, ignoreSelected);
            PickContext pc = getPickContext();
            pc.ignoreSelected = ignoreSelected;
            return LWTraversal.PointPick.pick(pc, mapX, mapY);
        } else
            return mFocal;
        */
    }
    
    private Timer rolloverTimer = new Timer();
    private TimerTask rolloverTask = null;
    private void runRolloverTask() {
        //if (true) return;
        //System.out.println("task run " + this);
        final float mapX = screenToMapX(lastMouseX);
        final float mapY = screenToMapY(lastMouseY);

        //LWComponent hit = pickNode(mapX, mapY);
        final PickContext pc = getPickContext(mapX, mapY);
        pc.isZoomRollover = true;
        final LWComponent hit = LWTraversal.PointPick.pick(pc);
        
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
    
    private static LWComponent mRollover;   // current rollover (mouse rollover hilite)
    //private double mRolloverOldScale;
    //private double mZoomoverOldScale;
    //private Point2D mZoomoverOldLoc = null;

    private static boolean allowsZoomedRollover(LWComponent c) {
        if (c == null || c instanceof LWLink || c instanceof LWPortal || c instanceof LWSlide)
            return false;
        else
            return true;
    }
    
    void setRollover(LWComponent c) {

        if (mRollover == c || !allowsZoomedRollover(c))
            return;
        
        if (DEBUG.Enabled) out("**SET ROLLOVER " + c);

        if (mRollover != null) {
            if (c.hasAncestor(mRollover)) {
                if (DEBUG.Enabled) out("IS ANCESTOR ROLLOVER");
                //c.setZoomedFocus(true); // test hack for cascading zoomed-focus...
                // currnetly impossible given single comonent required for picking code,
                // as well as drawing code in MapViewer...
                return;
            }
            clearRollover();
        }

        final double mapZoom = getZoomFactor();
        double netZoom;

        if (mapZoom <= 1.0) {
            final double normalZoom = 1 / getZoomFactor(); // zoom needed to get to 100%
            netZoom = normalZoom * 2;
            if (netZoom > 4) {
                netZoom = normalZoom;
                if (netZoom > 6)
                    netZoom = 8;
            }
        } else if (mapZoom > 2.0) {
            if (DEBUG.Enabled) out("**SET ROLLOVER: skipped -- overzoom");
            return;
        } else
            netZoom = 2;
        
        if (DEBUG.Enabled) out("**SET ROLLOVER NET ZOOM: " + netZoom);
        LWComponent.ZoomRolloverScale = netZoom;
            
        mRollover = c;
        //mRolloverOldScale = c.getScale();
        mRollover.setZoomedFocus(true);

        repaint();

        //final double curMapScale = mRollover.getMapScale();

        //mRollover.setScale(1.0 / curMapZoom);
        //mRollover.setScale(2.0);
        
//         double newMapScale = mRollover.getMapScale();


//         //if (rollover != c && (c instanceof LWNode || c instanceof LWLink)) {
//         // link labels need more work to be zoomable
//         if (rollover != c && (c instanceof LWNode)) {
//             clearRollover();
//             // for moment rollover is really setTemporaryZoom
//             //rollover = c;
//             //c.setRollover(true);
//             mZoomoverOldScale = c.getScale();
            
//             double curZoom = getZoomFactor();
            
//             //double newScale = mZoomoverOldScale / curZoom;
//             double newScale = 1.0 / curZoom;
            
//             //if (newScale < 1.0) newScale = 1.0;
            
//             //if (true||mZoomoverOldScale != 1f) {
//             if (newScale > mZoomoverOldScale &&
//             newScale - mZoomoverOldScale > RolloverMinZoomDeltaTrigger) {
//                 //c.setScale(1f);
//                 rollover = c;
//                 if (DEBUG.ROLLOVER) System.out.println("setRollover: " + c);
//                 c.setRollover(true);
//                 c.setZoomedFocus(true);
//                 if (false&&c instanceof LWNode) {
//                     // center the zoomed node on it's original center
//                     mZoomoverOldLoc = c.getLocation();
//                     Point2D oldCenter = c.getCenterPoint();
//                     c.setScale(newScale);
//                     c.setCenterAtQuietly(oldCenter);
//                 } else
//                     c.setScale(newScale);
                
//                 repaintMapRegion(rollover.getBounds());
//             }
//         }
    }
    void clearRollover() {

        if (mRollover == null)
            return;
        
        if (DEBUG.Enabled) out("clear rollover " + mRollover);

        //mRollover.setScale(mRolloverOldScale);
        mRollover.setZoomedFocus(false);
        mRollover = null;

        repaint();
        
//         if (rollover != null) {
//             if (DEBUG.ROLLOVER) System.out.println("clrRollover: " + rollover);
//             if (rolloverTask != null) {
//                 rolloverTask.cancel();
//                 rolloverTask = null;
//             }
//             Rectangle2D bigBounds = rollover.getBounds();
//             rollover.setRollover(false);
//             rollover.setZoomedFocus(false);
//             if (true||mZoomoverOldScale != 1f) {
                
//                 // If deleted, don't put scale back or will throw
//                 // zombie event exception (should be okay to leave
//                 // scale in intermediate state on deleted node -- on
//                 // restore it should have it's scale set back thru
//                 // reparenting... if not, we'll need to clear rollover
//                 // on nodes b4 they're deleted, or allow the setScale
//                 // on a deleted node in LWComponent.
                
//                 if (!rollover.isDeleted())
//                     rollover.setScale(mZoomoverOldScale);
                
//                 //if (rollover.getParent() instanceof LWNode)
//                 // have the parent put it back in place
//                 //rollover.getParent().layoutChildren();
//                 //else
                
//                 // todo? also need to do this setLocation quietly: if they
//                 // move mouse back and forth tween two link endpoints
//                 // when no delay is on (easier to see in big curved link)
//                 // we're seeing the connection point change (still seeing this?)
//                 if (mZoomoverOldLoc != null) {
//                     rollover.setLocation(mZoomoverOldLoc);
//                     mZoomoverOldLoc = null;
//                 }
//             }
//             repaintMapRegion(bigBounds);
//             rollover = null;
//         }
    }
    
    /** The currently display JComponent in a tool-tip window.  Null if none is showing. */
    private static JComponent sTipComponent;

    /**
     * Every time we attempt to display a tool-tip, or even clear, a tool tip, we increment this
     * counter.  That way the ClearTipTimer can know exactly the display instance it should clear,
     * and ONLY clear it if this counter hasn't changed.  This way we know that even if the same
     * tip is displaying when the timer was started, if the display instance has changed, we
     * do NOT clear it, beacuse somebody else has requested it's display since then.
     */
    private static long sTipDisplayInstance = 0;

    /** The currently displayed tool-tip window.  Note that as Popup's use a window cache,
     * the window object may be the same even for different tool tips.  We set this to null
     * if there isn't one visible. */
    private static Popup sTipPopup;

    /** The last LWComponent to have the mouse over it */
    private static LWComponent sLastMouseOver;
    
    /** Synchronization lock for sTipComponent and sTipDisplayInstance.  Note that all
     * these are static across al* maps, as the mouse may roll from one map to another,
     * and this makes it easy to reliably clear the tip on the old map when rolling to the new map.
     */
    private static Object sTipLock = new Object();
    
    /**
     * Pop a tool-tip near the given LWComponent.
     *
     * @param pJComponent - the JComponent to display in the tool-tip window
     * @param pAvoidRegion - the region to avoid (usually LWComponent bounds)
     * @param pTipRegion - the region, in map coords, that triggered this tool-tip
     */
    void setTip(JComponent pJComponent, Rectangle2D pAvoidRegion, Rectangle2D pTipRegion) {
        if (pJComponent == null)
            throw new IllegalArgumentException("JComponent is null");

        if (VUE.inNativeFullScreen())
            return;

        synchronized (sTipLock) {
            if (pJComponent == sTipComponent) {
                sTipDisplayInstance++;
                return;
            }
        }
            
        if (sTipPopup != null)
            sTipPopup.hide();
            
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
            
        GUI.refreshGraphicsInfo();

        // Default placement starts from left of component,
        // at same height as the rollover region that triggered us
        // in the component.
        Point screen = new Point(avoid.ul.x - tip.width,  trigger.ul.y);
            
        if (screen.x < 0) {
            // if would go off left of screen, try placing above the component
            screen.x = avoid.ul.x;
            screen.y = avoid.ul.y - tip.height;
            GUI.keepLocationOnScreen(screen, tip);
            // if too tall and would then overlap rollover region, move to right of component
            //if (screen.y + tip.height >= placementLeft.y) {
            // if too tall and would then overlap component, move to right of component
            if (screen.y + tip.height > avoid.ul.y) {
                screen.x = avoid.lr.x;
                screen.y = trigger.ul.y;
            }
            // todo: consider moving tall tips from tip to right
            // of component -- looks ugly having all that on top.
                
            // todo: consider a 2nd pass to ensure not overlapping
            // the rollover region, to prevent window-exit/enter loop.
            // (flashes the rollover till mouse moved away)
        }
        
        GUI.keepLocationOnScreen(screen, tip);
            
        // todo java bug: there are some java bugs, perhaps in the Popup caching code
        // (happens on PC & Mac both), where the first time a pop-up appears (actually
        // only seeing with tall JTextArea's), it's height is truncated.  Sometimes even
        // first 1 or 2 times it appears!  If intolerable, just implement our own
        // windows and keep them around as a caching scheme -- will use alot more memory
        // but should work (use WeakReferences to help)

        // TODO JAVA BUG: allowing click in a LIGHTWEIGHT pop-up window
        // gives it focus, and the focus system sends it FOCUS_LOST
        // when you go elsewhere, but elsewhere never ever gets FOCUS_GAINED...
        // Hopefully we can workaround this in our FocusManager.
        // [ This only appears to happen with alwaysOnTop, which we're not using right now ]
            
        synchronized (sTipLock) {
            PopupFactory popupFactory = PopupFactory.getSharedInstance();
            
            sTipPopup = popupFactory.getPopup(this, pJComponent, screen.x, screen.y);
            sTipComponent = pJComponent;

            // this isn't helping: it's still allowing focus!  The problem case a
            // light-weight popup, which is done as a Panel added to the JLayeredPane.
            sTipComponent.setFocusable(false);
            if (sTipComponent.getName() == null)
                sTipComponent.setName("VUE-POPUP-COMPONENT");
            
            Window onTop = null;

            if (GUI.UseAlwaysOnTop && !Util.isMacPlatform()) {
                Window w = SwingUtilities.getWindowAncestor(sTipComponent);
                if (w != null && w.getName() != null && w.getName().startsWith("###")) {
                    GUI.setAlwaysOnTop(w, true);
                    onTop = w;
                }
            }

            sTipPopup.show();
            if (onTop != null)
                onTop.toFront();

            // Okay: this seems to be working for now?
            // OH: maybe this was just a problem when UseAlwaysOnTop was happening?
            //lockMediumWeightPopup(sTipComponent, false);
            
            sTipDisplayInstance++;
        }
    }

    // TODO: find a workaround for this java focus bug.  HeaveWeight pop-ups
    // are fine, but not medium weight (ones that use java.awt.Panel).
    // [Apparently, this is only a problem when using alwaysOnTop]
    private void lockMediumWeightPopup(Component c, boolean done) {
        if (c == null)
            return;
        if (done) {
            if (DEBUG.FOCUS) out("skipping " + Util.out(c.getBounds()) + "\t" + GUI.name(c));
        } else {
            if (DEBUG.FOCUS) out(" locking " + Util.out(c.getBounds()) + "\t" + GUI.name(c) + "\t" + GUI.name(c.getPeer()));

            // This apparently isn't getting reset when the pop-up is re-used,
            // which causes later lightweight menu's to stop functioning!
            
            //c.setFocusable(false);

            if (c instanceof java.awt.Panel) {
                //c.getParent().setFocusable(false); // try the JLayeredPane (no help)

                c.setName(GUI.POPUP_NAME);
                
                done = true;
                
                if (false && DEBUG.FOCUS == false) {
                    c.setEnabled(false);
                    // Well, this "fixes" problem, but it no longer gets even MOUSE_ENTERED,
                    // and so our timer doesn't know to leave it up when you mouse out
                    // of the node, and clicking on it is  nvisible: it goes thru to the map.
                }
                
            }
        }
        lockMediumWeightPopup(c.getParent(), done);
    }
    
    void clearTip() {
        synchronized (sTipLock) {
            sTipComponent = null;
            if (sTipPopup != null) {
                if (DEBUG.ROLLOVER && DEBUG.META) new Throwable("CLEARTIP").printStackTrace();
                sTipPopup.hide();
                sTipPopup = null;
            }
            sTipDisplayInstance++;
        }
    }
    
    /**
     * Render all the LWComponents on the panel
     */
    // java bug: Do NOT create try and create an axis using Integer.MIN_VALUE or Integer.MAX_VALUE
    // -- this triggers line rendering bugs in PC Java 1.4.1 (W2K) -- same for floats.
    //private static final int MinCoord = -10240;
    //private static final int MaxCoord = 10240;
    private static final int MinCoord = Short.MIN_VALUE;
    private static final int MaxCoord = Short.MAX_VALUE;
    private static final Line2D Xaxis = new Line2D.Float(MinCoord, 0, MaxCoord, 0);
    private static final Line2D Yaxis = new Line2D.Float(0, MinCoord, 0, MaxCoord);
    
    //public boolean isOpaque() {return false;}
    
    private int paints=0;
    //private boolean redrawingSelector = false;
    public void paint(Graphics g) {
        long start = 0;
        if (DEBUG.PAINT) {
            System.out.print("paint " + paints + " RAW-clipBounds=" + g.getClipBounds()+" "); System.out.flush();
            start = System.currentTimeMillis();
        }
        try {
            // This a special speed optimization for the selector box -- not sure it helps anymore tho...
//             if (redrawingSelector && draggedSelectorBox != null && activeTool.supportsXORSelectorDrawing()) {
//                 redrawSelectorBox((Graphics2D)g);
//                 redrawingSelector = false;
//             } else
                super.paint(g);
        } catch (Exception e) {
            System.err.println("*paint* Exception painting in: " + this);
            System.err.println("*paint* VueSelection: " + VueSelection);
            if (VueSelection != null)
                System.err.println("*paint* VueSelection.first: " + VueSelection.first());
            System.err.println("*paint* Graphics: " + g);
            System.err.println("*paint* Graphics transform: " + ((Graphics2D)g).getTransform());
            e.printStackTrace();
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


        if (mFocal == null || !mFocal.hasContent())
            paintEmptyMessage(g);
        
        paints++;
        RepaintRegion = null;
    }

    protected void paintEmptyMessage(Graphics g) {
        g.setColor(Color.lightGray);
        Font font = new Font("Verdana", Font.BOLD, 36);
        g.setFont(font);

        final String msg = getEmptyMessage();

        int w = getWidth() / 2;
        w -= GUI.stringLength(font, msg) / 2;
        g.drawString(msg, w, getHeight() / 2);
    }

    protected String getEmptyMessage() {
        if (mMap != null && mMap.isModified())
            return "Empty Map";
        else
            return "New Map";
    }
    
    
    /**
     * @return a DrawContext which has adjusted the Graphics for current pan and zoom
     */
    protected DrawContext getDrawContext(Graphics2D g) {
        DrawContext dc = new DrawContext(g, getZoomFactor(), -getOriginX(), -getOriginY(), getVisibleBounds(), mFocal, true);
        
        dc.setInteractive(true);
        dc.setAntiAlias(DEBUG_ANTI_ALIAS);
        dc.setPrioritizeQuality(DEBUG_RENDER_QUALITY);
        dc.setFractionalFontMetrics(DEBUG_FONT_METRICS);
        dc.disableAntiAlias(DEBUG_ANTI_ALIAS == false);
        //dc.setActiveTool(getCurrentTool());
        //dc.setMaxLayer(getMaxLayer());

        //dc.zoomedFocus = mRollover;
        
        return dc;
    }
    
    private static final Color rrColor = Color.yellow;
    /**
     * Java Swing JComponent.paintComponent -- paint the map on the map viewer canvas
     */
    public void paintComponent(final Graphics incomingGC)
    {
        final Graphics2D g = (Graphics2D) incomingGC.create();
        
        /*
        Rectangle cb = g.getClipBounds();
        //if (DEBUG.PAINT && !OPTIMIZED_REPAINT && (cb.x>0 || cb.y>0))
        //out("paintComponent: clipBounds " + cb);
        */
        
        if (OPTIMIZED_REPAINT) {
            // debug: shows the repaint region
            if (DEBUG.PAINT && (RepaintRegion != null || paintingRegion)) {
                paintingRegion = false;
                g.setColor(rrColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.black);
                g.setStroke(STROKE_ONE);
                Rectangle r = g.getClipBounds();
                r.width--;
                r.height--;
                g.draw(r);
            }
        }
        
        final DrawContext dc = activeTool.getDrawContext(getDrawContext(g));

        //-------------------------------------------------------
        // DRAW THE THE CURRENT FOCAL (usually the MAP)
        //-------------------------------------------------------

        drawFocal(dc);
        
        //-------------------------------------------------------
        
        dc.setMapDrawing();
        if (DEBUG_SHOW_ORIGIN) {
            out("DRAWING ORIGIN");
            // why isn't this working anymore?  was just after fill, but
            // now that tool does fill, we have to do on top, but can't
            // see it...
            //g2.setStroke(STROKE_ONE);
            //g2.setColor(Color.lightGray);
            dc.setAbsoluteStroke(1);
            g.setColor(Color.black);
            g.draw(Xaxis);
            g.draw(Yaxis);
            if (false && mZoomFactor >= 6.0) {
                dc.setAbsoluteStroke(1);
                g.setColor(Color.black);
                g.draw(Xaxis);
                g.draw(Yaxis);
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
        // DRAW THE CURRENT INDICATION, if any (for targeting during drags)
        //-------------------------------------------------------

        if (indication != null && indication != mFocal)
            drawIndication(dc);

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

        final LWSelection s = VueSelection;

        if (s == null || s.isEmpty() || !activeTool.supportsResizeControls()) {
            resizeControl.active = false;
        } else  {
            final LWComponent remoteFocal = s.getFocal();
            if (remoteFocal == null) {
                out("null remote focal");
            } else if (getFocal() != remoteFocal && remoteFocal.isMapVirtual()) {
                resizeControl.active = false;
            } else {
                drawSelection(dc, s);
            }
        }

        
        //-------------------------------------------------------
        // draw the dragged selector box
        //-------------------------------------------------------
        
        //if (draggedSelectorBox != null && activeTool.supportsDraggedSelector(null)) {
        if (draggedSelectorBox != null) {
            // todo: box should already be null of tool doesn't support selector
            dc.setRawDrawing();
            drawSelectorBox(dc, draggedSelectorBox);
            //if (VueSelection != null && !VueSelection.isEmpty())
            //    new Throwable("selection box while selection visible").printStackTrace();
            // totally reasonable if doing a shift-drag for SELECTION TOGGLE
        }
        
        if (DEBUG.VIEWER) {
            dc.setRawDrawing();
            g.setColor(Color.red);
            g.setStroke(new java.awt.BasicStroke(1f));
            g.drawLine(_mouse.x,_mouse.y, _mouse.x+1,_mouse.y+1);
            
            int iX = (int) (screenToMapX(_mouse.x) * 100);
            int iY = (int) (screenToMapY(_mouse.y) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            Point2D mapCoords = new Point2D.Float(mapX, mapY);
            Point canvas = getLocation();
            Point2D screen = new Point2D.Float(_mouse.x + canvas.x, _mouse.y + canvas.y);
                                                     
            
            g.setFont(VueConstants.FixedFont);
            int x = -getX() + 40;
            int y = -getY() + 100;
            //int x = dc.frame.x;
            //int y = dc.frame.y;
            //g2.drawString("screen(" + mouse.x + "," +  mouse.y + ")", 10, y+=15);
            if (true) {
                g.drawString(" origin offset: " + out(getOriginLocation()), x, y+=15);
                g.drawString("     map mouse: " + out(mapCoords), x, y+=15);
                g.drawString("  canvas mouse: " + out(_mouse), x, y+=15);
                g.drawString(" ~screen mouse: " + out(screen), x, y+=15);
                g.drawString("     canvas at: " + out(canvas), x, y+= 15);
                /*if (inScrollPane){
                Point extent = viewportToCanvasPoint(mouse);
                Point2D map = extentToMapPoint(extent);
                g2.drawString("  extent point: " + out(extent), x, y+=15);
                g2.drawString("     map point: " + out(map), x, y+=15);
                }*/
                if (inScrollPane){
                g.drawString("viewport----pos " + out(mViewport.getViewPosition()), x, y+=15);
                }
                g.drawString("map-canvas-size " + out(mapToScreenDim(getMap().getBounds())), x, y+=15);
                g.drawString("map-canvas-adju " + out(mapToScreenDim(getContentBounds())), x, y+=15);
                g.drawString("    canvas-size " + out(getSize()), x, y+=15);
                g.drawString("          frame " + out(dc.getFrame()), x, y+=15);
            }
            if (inScrollPane) {
                g.drawString("  viewport-size " + out(mViewport.getSize()), x, y+=15);
            }
            g.drawString("zoom " + getZoomFactor(), x, y+=15);
            g.drawString("anitAlias " + DEBUG_ANTI_ALIAS, x, y+=15);
            g.drawString("renderQuality " + DEBUG_RENDER_QUALITY, x, y+=15);
            g.drawString("fractionalMetrics " + DEBUG_FONT_METRICS, x, y+=15);
            //g.drawString("findParent " + !DEBUG_FINDPARENT_OFF, x, y+=15);
            g.drawString("optimizedRepaint " + OPTIMIZED_REPAINT, x, y+=15);
            g.drawString("Focal " + this.mFocal, x, y+=15);
            g.drawString("  MAP " + this.mMap, x, y+=15);

            Point2D center = getVisibleCenter();
            dc.setAbsoluteStroke(1);
            // easily gets lost when way zoomed in because coords > MaxCoord
            //g2.draw(new Line2D.Double(center.getX(), MinCoord, center.getX(), MaxCoord));
            //g2.draw(new Line2D.Double(MinCoord, center.getY(), MaxCoord, center.getY());
            g.drawLine(-99999, (int) Math.round(center.getY()), 99999, (int) Math.round(center.getY()));
            g.drawLine((int) Math.round(center.getX()), -99999, (int) Math.round(center.getX()), 99999);
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
            super.paintChildren(incomingGC); // add to layered pane instead?
        //setOpaque(true);
    }

    protected static final Color DefaultFillColor = Color.white;
    protected Color getBackgroundFillColor(DrawContext dc)
    {
        final Color bgFill;

        if (dc.isPresenting() && !inScrollPane()) {
            // if we're in a scroll-pane (standard non-full-screen map viewer),
            // we don't want to fill everything: just let the slide fill itself

            final LWPathway.Entry entry = VUE.getActiveEntry();

            if (entry != null)
                bgFill = entry.getFullScreenFillColor(dc);
            else
                bgFill = DefaultFillColor;

            // We CANNOT depend on looking to see if the focal is a slide
            // to know if we need a presentation full-screen fill color,
            // because if the pathway entry isn't a slide entry (e.g., a map-view
            // item or a portal), we won't know what to use...
            
        } else {
            if (mMap == null || mFocal != mMap)
                bgFill = DefaultFillColor;
            else
                bgFill = mMap.getFillColor();
        }

        return bgFill;
        
    }


    protected void drawFocal(DrawContext dc)
    {
        activeTool.handlePreDraw(dc, this);

        drawFocalRaw(dc);
        
        activeTool.handlePostDraw(dc, this);
    }

    protected void drawFocalRaw(DrawContext dc)
    {
        if (dc.getFill() == null) {
            // unless the active tool has already done some kind
            // of special fill, fill the entire background
            // before drawing anything else (must to do this
            // to clear out the prior graphics context).
            dc.fill(getBackgroundFillColor(dc));
        }
        
        if (mFocal == null)
            return;

        if (DEBUG.VIEWER && mRollover != null)
            mRollover.updateConnectedLinks(null);
        
        if (mFocal.isTranslucent() && mFocal != mMap) {
            // If our fill is in any way translucent, the underlying
            // map can show thru, thus we have to draw the whole map
            // to see the real result -- we just set the clip to
            // the shape of the focal.

            final Shape curClip;

            if (mFocal instanceof LWLink) {
                // Don't clip if it's a link: still draw entire map
                curClip = null;
            } else {
                curClip = dc.g.getClip();                
                final Shape focalClip = getFocalClip();
                dc.g.clip(focalClip);
                dc.setMasterClip(focalClip);
            }
            LWComponent parentSlide = mFocal.getParentOfType(LWSlide.class);
            // don't need to re-draw the focal itself, it's being
            // drawn in it's parent (slide or map)
            if (parentSlide != null) {
                parentSlide.draw(dc);
            } else {
                mFocal.getMap().draw(dc);
            }
            if (curClip != null)
                dc.setMasterClip(curClip);
        } else {
            // now draw the map / focal
            mFocal.draw(dc);
        }

        if (mRollover != null) {
            final DrawContext zdc = dc.create();
            zdc.setClipOptimized(false);
            zdc.setDrawPathways(false);
            //zoomDC.setAlpha(0.8f); // Not what we want here (for image generation only?)
            zdc.g.setComposite(ZoomTransparency);
            mRollover.transformZero(zdc.g);
            if (mRollover.isTransparent()) {
                Color fill = mRollover.getRenderFillColor(zdc);
                if (fill == null || fill.getAlpha() == 0)
                    fill = getMap().getFillColor();
                zdc.g.setColor(fill);
                zdc.g.fill(mRollover.getZeroShape());
            }
            mRollover.drawZero(zdc);
        }
    }

    private static final AlphaComposite ZoomTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
    
    
    /** This paintChildren is a no-op.  super.paint() will call this,
     * and we want it to do nothing because we need to invoke this
     * ourself at a time later than it normally would (we call
     * super.paintChildren directly, only if there is an activeTextEdit,
     * at the bottom of paintComponent()).
     */
    protected void paintChildren(Graphics g) {}
    
    /** overriden only to catch when the activeTextEdit is being
     * removed from the panel */
    // todo: Add the active text edit to the layered pane?
    public void remove(Component c) {
        try {
            super.remove(c);
        } finally {
            if (c == activeTextEdit) {
                activeTextEdit = null;
                try {
                    // TextBox now handles this, as it may want to reshape itself
                    // before repainting.
                    //repaint();
                    if (VUE.getActiveViewer() == this)
                        requestFocus();
                } finally {
                    // make absolutely certian no matter what
                    // that we re-enable actions.
                    VueAction.setAllActionsIgnored(false);
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
        if (!lwc.supportsUserLabel() || !lwc.supportsProperty(LWKey.Label))
            return;
        if (activeTextEdit != null)
            remove(activeTextEdit);
        // todo robust: make sure can never accidentally happen on a
        // closed map viewer, or all actions will go off and never
        // come back on again, because the textbox will never get
        // focus so it can lose it and turn them back on.
        VueAction.setAllActionsIgnored(true);
        activeTextEdit = lwc.getLabelBox();
        activeTextEdit.saveCurrentText();
        if (activeTextEdit.getText().length() < 1)
            activeTextEdit.setText("label");
        
        Point2D.Float point = activeTextEdit.getBoxPoint();
        if (DEBUG.TEXT || DEBUG.WORK) out("BOX POINT LOCAL: " + fmt(point));
        
        if (Float.isNaN(point.x)) {
            // Float.NaN is marker for an uninitialized TextBox location
            lwc.initTextBoxLocation(activeTextEdit);
            point = activeTextEdit.getBoxPoint();
            if (DEBUG.TEXT || DEBUG.WORK) out(" BOX POINT INIT: " + fmt(point));
            
        }

        lwc.getZeroTransform().transform(point, point);
        
        if (DEBUG.TEXT || DEBUG.WORK) out("  BOX POINT MAP: " + fmt(point));

        final int screenX = mapToScreenX(point.x);
        final int screenY = mapToScreenY(point.y);
        
        //if (DEBUG.WORK||DEBUG.CONTAINMENT) out(String.format("screen X/Y: %d,%d", screenX, screenY));
        
        activeTextEdit.setLocation(screenX, screenY);
        
        activeTextEdit.selectAll();
        add(activeTextEdit);
      //  VUE.getFormattingPanel().getTextPropsPane().setActiveTextControl(activeTextEdit);
        if (DEBUG.LAYOUT) System.out.println(activeTextEdit + " back from addNotify");
        activeTextEdit.requestFocus();
        if (DEBUG.LAYOUT) System.out.println(activeTextEdit + " back from requestFocus");
    }
    
    private void drawSelectorBox(DrawContext dc, Rectangle r) {
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
        dc.g.setStroke(STROKE_SELECTION_DYNAMIC);
        if (activeTool.supportsXORSelectorDrawing())
            dc.g.setXORMode(COLOR_SELECTION_DRAG);// using XOR may also be working around below clip-edge bug
        else
            dc.g.setColor(COLOR_SELECTION_DRAG);
        activeTool.drawSelector(dc, r);
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
     
    /*
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
    */
    
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

    private static void drawSelectionGhost(DrawContext dc, LWComponent c)
    {
        // todo: the ideal is to always draw the ghost (not just when
        // dragging) but figure out a way not to uglify the border if
        // it's visible with the blue streak -- may XOR draw to the border
        // color? (or it's inverse)
        Shape shape = c.getZeroShape();
        c.transformZero(dc.g);
        dc.setAbsoluteStroke(1.0);
        dc.g.draw(shape);
        if (false && shape instanceof RectangularPoly2D) {
            // Inscribe many sided poly's in a circle so the user can
            // more easily make all the sides of equal length if they want.
            // TODO: fix scaling
            if (((RectangularPoly2D)shape).getSides() > 4) {
                Ellipse2D inscribed = new Ellipse2D.Float();
                Rectangle2D b = shape.getBounds2D();
                if (DEBUG.BOXES) {
                    inscribed.setFrame(shape.getBounds());
                    dc.g.draw(inscribed);
                }
                /*
                  inscribed.setFrame(0,
                  (b.getHeight() - b.getWidth()) /2,
                  b.getWidth(),
                  b.getWidth());
                */
                inscribed.setFrame(0,
                                   (c.getHeight() - c.getWidth()) /2,
                                   c.getWidth(),
                                   c.getWidth());
                dc.g.draw(inscribed);
            }
        }
    }

    // TODO: don't draw unless all components are within mFocal...
    protected void drawSelection(DrawContext dc, final LWSelection selection)
    {
        dc.g.setColor(COLOR_SELECTION);
        dc.g.setStroke(STROKE_SELECTION);
        
        LWContainer mFocalParent = null;
        boolean drawSelectorBoxInThisViewer = true;
        
        if (mFocal == null) {
            // If we're "empty", can't possibly need to draw a selection
            resizeControl.active = false;
            return;
        } else if (mFocal != mMap) {
            if (mFocal.hasChildren()) {
                mFocalParent = (LWContainer) mFocal;
            } else {
                // if the focal has no children, we already know nothing in selection
                // could be a child of it.
                resizeControl.active = false;
                return;
            }
            
        }

        // Check selection and/or draw ghost outlines for selected objects
        
        dc.setMapDrawing();

        AffineTransform rawMapTransform = dc.g.getTransform();
        boolean atLeastOneVisible = false;
        for (LWComponent c : selection) {

            if (c instanceof LWSlide && !c.isMoveable()) {
                // hack for slides, which are currently not proper children of anyone
                // (prevents selection of a slide icon from drawing a selection
                // drag frame for the 0,0 based slide, which isn't really on
                // any map -- it's owned by the pathway).
                continue;
            }

            if (mFocalParent != null) {
                /*
                if (c == mFocalParent) {
                    // TODO: this is a pretty major hack: get this de-selected earlier
                    // (the LWSlide is being included in the selection generated
                    // by the dragged selector box, because the first mouse press
                    // at the start of the drag actually selected the slide itself,
                    // which we need to do, but then if a drag starts, we want to
                    // de-select it, unless we can change selection to happen on mouse-up)
                    // also, we're needing to iterate a selection clone here now
                    // to ensure against comodification exception.
                    selection.remove(c);
                    continue;
                }
                */
                if (!c.hasAncestor(mFocalParent)) {
                    // Something in selection is not in the current focal for this viewer,
                    // so don't draw the selection box here.
                    // TODO: crap: the slide itself (the focal) is in the selection:
                    // need to specal case remove that at start of selector box drag...
                    out(c + " in selection doesn't have ancestor " + mFocalParent);
                    drawSelectorBoxInThisViewer = false;
                    break;
                }
            }
//             else if (c.getLayer() > getMaxLayer()) {
//                 // Something in selection is not from a layer visible in this viewer,
//                 // so don't draw the selection box here.
//                 out("layer " + c.getLayer() + " for " + c + " >maxLayer=" + getMaxLayer());
//                 drawSelectorBoxInThisViewer = false;
//                 break;
//             }

            if (c.isDrawn())
                atLeastOneVisible = true;

            
            //-------------------------------------------------------
            // draw ghost shapes
            //-------------------------------------------------------
            //if (sDragUnderway || c.getStrokeWidth() == 0 || c instanceof LWLink) {

            Set parentGroups = null;
            
            if (true||sDragUnderway) {
                if (c instanceof LWMap && !DEBUG.CONTAINMENT)
                    continue;
                drawSelectionGhost(dc, c);

                // Hack for groups in groups: show the parent group:
                final LWComponent parent = c.getParent();
                
                if (parent instanceof LWGroup && !parent.isSelected() && (parentGroups == null || !parentGroups.contains(parent))) {
                    dc.g.setTransform(rawMapTransform);
                    drawSelectionGhost(dc, parent);

                    if (parentGroups == null) {
                        // todo: performance
                        parentGroups = new HashSet();
                        parentGroups.add(parent);
                    }
                }
                    
                dc.g.setTransform(rawMapTransform);
            }
                
        }
        
        //----------------------------------------------------------------------------------------
        // now return to "raw" (non-scaled / translated) drawing for the selection control handles
        // and on-screen diagnostics
        //----------------------------------------------------------------------------------------
        dc.setRawDrawing();
        //----------------------------------------------------------------------------------------

        if (!drawSelectorBoxInThisViewer || !atLeastOneVisible) {
            resizeControl.active = false;
            if (!drawSelectorBoxInThisViewer) out("selection contents not for this viewer");
            return;
        }
        
        dc.g.setStroke(STROKE_SELECTION);
        //g2.setComposite(AlphaComposite.Src);
        dc.g.setColor(COLOR_SELECTION);
        
        //if (!VueSelection.isEmpty() && (!sDragUnderway || isDraggingSelectorBox)) {
        
        // todo opt?: don't recompute bounds here every paint ---
        // can cache in draggedSelectionGroup (but what if underlying objects resize?)
        Rectangle2D selectionBounds = selection.getBounds();
        if (DEBUG.SELECTION) out("selectionBounds " + selectionBounds);
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
        if (DEBUG.SELECTION) out("paintedSelectionBounds " + paintedSelectionBounds);
        
        //System.out.println("screenSelectionBounds="+mapSelectionBounds);
        

//         if (//VueSelection.countTypes(LWNode.class) + VueSelection.countTypes(LWImage.class) <= 0
//             //||
//             //(VueSelection.size() == 1 && VueSelection.first() instanceof LWNode && ((LWNode)VueSelection.first()).isTextNode())
//             selection.allOfType(LWLink.class)
//             ) {

        final LWComponent only = selection.only();
        final LWComponent first = selection.first();

        final boolean TEST = DEBUG.LINK; // if TEST is true, we always show the selection bounds & handles no matter what

        //if (!selection.first().isMoveable() || !selection.first().supportsUserResize()) { // todo: check all, not any
        if (!TEST && selection.size() == 1 && (!only.isMoveable() || !only.supportsUserResize())) {
            resizeControl.active = false;
        } else if (!TEST && selection.allOfType(LWLink.class)) {
            // todo: this check is a hack: need to check if any in selection return true for supportsUserResize (change to merge w/isMoveable -- a dynamic property)
            // todo: also alow groups to resize (make selected group resize
            // re-usable for a group -- perhaps move to LWGroup itself &
            // also use draggedSelectionGroup for this?)
            if (DEBUG.BOXES || selection.size() > 1 /*|| !VueSelection.allOfType(LWLink.class)*/)
                dc.g.draw(mapSelectionBounds);
            // no resize handles if only links or groups
            resizeControl.active = false;
        } else {
      //} else if (mRollover == null) { // don't draw selection handles during zoomed rollover
            if (TEST || selection.size() > 1) {
                dc.g.draw(mapSelectionBounds);
            } else {
                // Only one in selection:
                // SPECIAL CASE to keep control handles out of way of node icons
                // when node is scaled way down:
                if (false && only.getScale() < 0.6) {
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
            
            // TODO: this a total hack: figure out via pickDepth / pickLevel, adding a getCurrentPickLevel to LWComponent
            final boolean deepSelection =
                selection.allHaveSameParentOfType(LWGroup.class) ||
                (selection.allHaveSameParentOfType(LWSlide.class) && first.getParent().isMoveable());
            
            setSelectionBoxResizeHandles(mapSelectionBounds);
            resizeControl.active = true;
            for (int i = 0; i < resizeControl.handles.length; i++) {
                LWSelection.Controller cp = resizeControl.handles[i];
                drawSelectionHandleCentered(dc.g,
                                            (float)cp.x,
                                            (float)cp.y,
                                            deepSelection ? COLOR_SELECTION : cp.getColor(),
                                            i);
            }
        }
        
        //if (sDragUnderway) return;
        
        //-------------------------------------------------------
        // draw LWComponent requested control points
        //-------------------------------------------------------

        //dc.g.setStroke(STROKE_HALF);
        
        for (LWSelection.ControlListener cl : selection.getControlListeners()) {
            LWSelection.Controller[] points = cl.getControlPoints(getZoomFactor());
            // draw them in reverse order, in case they overlap: will match hit detection forward-order
            for (int i = points.length - 1; i >= 0; i--) {
                LWSelection.Controller ctrl = points[i];
                if (ctrl == null)
                    continue;

                final AffineTransform saveTx = dc.g.getTransform();
                final RectangularShape shape = ctrl.getShape();
                
                double size = shape.getWidth(); // control shape forced to aspect of 1:1 for now (height ignored)
                //if (size <= 0)
                //    size = 9;
                //if (dc.zoom < 0.5) size /= (3.0/2.0);

                dc.g.translate(mapToScreenX(ctrl.x), mapToScreenY(ctrl.y));
                dc.g.rotate(ctrl.getRotation());
                // now center the control on the point
                dc.g.translate(-size/2, -size/2);
                //shape.setFrame(0,0, size,size); // can't do this if shape is a constant object!
                final Color fillColor;
                if (false && sDragUnderway) // hilight the dragging control
                    fillColor = Color.red; // don't do for ALL controls: just the active one...
                else
                    fillColor = ctrl.getColor();
                if (fillColor != null) {
                    dc.g.setColor(fillColor);
                    dc.g.fill(shape);
                }
                if (fillColor == COLOR_SELECTION)
                    dc.g.setColor(Color.white); // ensure border contrast
                else
                    dc.g.setColor(COLOR_SELECTION);
                dc.g.draw(shape);

                dc.g.setTransform(saveTx);
                
//                     // Old loop contents:
//                     drawSelectionHandleCentered(dc.g,
//                                                 mapToScreenX(ctrl.x),
//                                                 mapToScreenY(ctrl.y),
//                                                 ctrl.getColor(),
//                                                 -(i+1)
//                                                 );

            }
        }
        
        if (DEBUG.VIEWER||DEBUG.LAYOUT||DEBUG.CONTAINMENT) resizeControl.draw(dc); // debug
        
        /*
        it = selection.iterator();
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

        //dc = dc.create(); // overkill?
        dc.setMapDrawing();
        //setScaleDraw(dc.g);
        double minStroke = STROKE_SELECTION.getLineWidth() * 3;// * mZoomInverse;
        if (indication.getStrokeWidth() > minStroke)
            dc.g.setStroke(new BasicStroke(indication.getStrokeWidth()));
        else
            dc.g.setStroke(new BasicStroke((float) minStroke));

        //dc.g.setColor(new Color(Color.green.getRGB() + (128<<24), true)); // 128<<24 = 50% transparent
        //dc.g.setColor(COLOR_SELECTION);

        indication.transformZero(dc.g);

        if (DEBUG.PICK &&
            (indication instanceof LWLink || indication instanceof LWNode || indication instanceof LWImage)) {
            dc.g.setColor(Color.green);
            dc.setAlpha(0.5);
            dc.g.fill(indication.getZeroShape());
        } else {
            dc.g.setColor(COLOR_INDICATION);
            dc.g.draw(indication.getZeroShape());
        }

        //dc.g.setColor(new Color(Color.white.getRGB() + (128<<24), true));
        //dc.g.fill(indication.getLocalShape());
    }
    
    
    // exterior drawn box will be 1 pixel bigger
    static final int SelectionHandleSize = VueResources.getInt("mapViewer.selection.handleSize"); // fill size
    static final int CHS = VueResources.getInt("mapViewer.selection.componentHandleSize"); // fill size
    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final Rectangle2D ComponentHandle = new Rectangle2D.Float(0,0,0,0);
    //static final int SelectionMargin = SelectionHandleSize > SelectionStrokeWidth/2 ? SelectionHandleSize : SelectionStrokeWidth/2;
    // can't combine these: one rendered at scale and one not!
    
    private void drawSelectionHandleCentered(Graphics2D g, float x, float y, Color fillColor, int index) {
        x -= SelectionHandleSize/2;
        y -= SelectionHandleSize/2;
        drawSelectionHandle(g, x, y, fillColor, index);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y) {
        drawSelectionHandle(g, x, y, COLOR_SELECTION_HANDLE, -1);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y, Color fillColor, int index) {
        //x = Math.round(x);
        //y = Math.round(y);
        SelectionHandle.setFrame(x, y, SelectionHandleSize, SelectionHandleSize);
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fill(SelectionHandle);
        }
        if (DEBUG.BOXES) {
            g.setFont(new Font("Courier", Font.BOLD, 14));
            g.setColor(Color.red);
            if (index < 0)
                g.drawString("cp" + -(index+1), x, y-2);
            else
                g.drawString("sp" + index, x, y-2);
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
        Rectangle2D.Float r = mapToScreenRect2D(c.getBounds());
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
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.add(c);
    }
    protected void selectionAdd(java.util.Iterator i) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.add(i);
    }
    protected void selectionRemove(LWComponent c) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.remove(c);
    }
    protected void selectionSet(LWComponent c) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.setTo(c);
    }
    protected void selectionSet(java.util.Collection bag) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.setTo(bag);
    }
    protected void selectionSet(java.util.Iterator i) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.setTo(i);
    }
    protected void selectionClear() {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        VueSelection.clear();
    }
    protected void selectionToggle(LWComponent c) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
        if (c.isSelected())
            selectionRemove(c);
        else
            selectionAdd(c);
    }
    //protected void selectionToggle(java.util.Iterator i) {
    protected void selectionToggle(Iterable<LWComponent> i) {
        VueSelection.setSource(this);
        VueSelection.setFocal(getFocal());
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
            Action[] shapeActions = NodeTool.getTool().getShapeSetterActions();
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
    
    
    private JPopupMenu buildMultiSelectionPopup() {
        JPopupMenu m = new JPopupMenu("Multi-Component Menu");
        
        m.add(getNodeMenu("Nodes"));
        m.add(getLinkMenu("Links"));
        m.add(GUI.buildMenu("Arrange", Actions.ARRANGE_MENU_ACTIONS));
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

        GUI.adjustMenuIcons(m);
        
        m.setLightWeightPopupEnabled(false);
        return m;
    }
    
    //private static JMenuItem sNodeMenuItem;
    //private static JMenuItem sLinkMenuItem;
    private static JMenuItem sUngroupItem;
    private static Component sPathAddItem;
    private static Component sPathRemoveItem;
  //  private static Component sPathSeparator;
    private JPopupMenu buildSingleSelectionPopup() {
        JPopupMenu m = new JPopupMenu("Component Menu");
        
      //  sNodeMenuItem = getNodeMenu("Node");
      //  sLinkMenuItem = getLinkMenu("Link");
        sUngroupItem = new JMenuItem(Actions.Ungroup);
        
       // m.add(sNodeMenuItem);
       // m.add(sLinkMenuItem);
        m.add(sUngroupItem);
        m.add(Actions.Duplicate);
        m.add(Actions.Rename);
        m.add(Actions.Delete);
        m.add(Actions.DeselectAll);        
        m.addSeparator();
        
        m.add(Actions.NotesAction);
        m.add(Actions.KeywordAction);
        sPathAddItem = m.add(Actions.AddPathwayItem);
        sPathRemoveItem = m.add(Actions.RemovePathwayItem);
        
        JMenu arrangeMenu = new JMenu("Arrange");
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        m.addSeparator();
        
        
        WindowDisplayAction infoAction = new WindowDisplayAction(VUE.getInfoDock());
        infoAction.setTitle("Node Info");
        JCheckBoxMenuItem infoCheckBox = new JCheckBoxMenuItem(infoAction);
        
        WindowDisplayAction outlineAction = new WindowDisplayAction(VUE.getOutlineDock());
        JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(outlineAction);
        outlineAction.setTitle("Outline View");
        
        m.add(infoCheckBox);
        m.add(checkBox);
        m.addSeparator();
        m.add(arrangeMenu);
        m.addSeparator();
        
        //sPathSeparator = m.add(new JPopupMenu.Separator());
        
        
        //m.addSeparator();
        //m.add(Actions.HierarchyView);
        //m.add(GUI.buildMenu("Nudge", Actions.ARRANGE_SINGLE_MENU_ACTIONS));
        sAssetMenu = new JMenu("Disseminators");
        // todo: special add-to selection action that adds
        // hitComponent to selection so have way other
        // than shift-click to add to selection (so you
        // can do it all with the mouse)
        

        GUI.adjustMenuIcons(m);
        m.setLightWeightPopupEnabled(false);
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
            //sNodeMenuItem.setVisible(!((LWNode)c).isTextNode());
            //sLinkMenuItem.setVisible(false);
            //Actions.HierarchyView.setEnabled(true);
            
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
            
        } /*else {
            sLinkMenuItem.setVisible(c instanceof LWLink);
            sNodeMenuItem.setVisible(false);
            Actions.HierarchyView.setEnabled(false);
        }*/
        
        if (getMap().getPathwayList().getActivePathway() == null) {
            sPathAddItem.setVisible(false);
            sPathRemoveItem.setVisible(false);
          //  sPathSeparator.setVisible(false);
        } else {
            sPathAddItem.setVisible(true);
            sPathRemoveItem.setVisible(true);
     //       sPathSeparator.setVisible(true);
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
            GUI.addToMenu(sMapPopup, Actions.NEW_OBJECT_ACTIONS);
            sMapPopup.addSeparator();
            sMapPopup.add(Actions.ZoomFit);
            sMapPopup.add(Actions.ZoomActual);
            sMapPopup.add(Actions.ToggleFullScreen);
            sMapPopup.addSeparator();
            sMapPopup.add(Actions.SelectAll);
            sMapPopup.add(new VueAction(VueResources.getString("mapViewer.mapMenu.info.label")) {
                    public void act() { GUI.makeVisibleOnScreen(this, MapInspectorPanel.class); }
                    //public void act() { VUE.MapInspector.setVisible(true); }
                });

            GUI.adjustMenuIcons(sMapPopup);
            
            sMapPopup.setLightWeightPopupEnabled(false);
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
    
    
    /** The key-code for a key being held down that is temporarily activating a tool while the key is held */
    private int tempToolKeyDown = 0;
    private VueTool tempToolPendingActivation;
    private VueTool tempToolWasActive;
    private boolean tempToolKeyReleased = false;
    
    private void revertTemporaryTool() {
        if (tempToolKeyDown != 0) {
            tempToolKeyDown = 0;
            activateTool(tempToolWasActive); // restore prior cursor
            tempToolWasActive = null;
        }
    }

    public Transferable getTransferableSelection() {
        draggedSelectionGroup.useSelection(VueSelection);
        return new LWTransfer(draggedSelectionGroup);
    }
    
    public Transferable getTransferableHelper(LWComponent comp) {
        //draggedSelectionGroup.useSelection(VueSelection);
        
        return new LWTransfer(comp);
    }
    
    
    private final DataFlavor URLFlavor = GUI.makeDataFlavor(java.net.URL.class);
    
    private final DataFlavor LWFlavors[] = {
            LWComponent.DataFlavor,
            DataFlavor.stringFlavor,
            DataFlavor.imageFlavor,
            MapResource.DataFlavor,
            //URLFlavor, // try text/uri-list
        };

        public class LWTransfer implements Transferable
        {
            private final LWComponent LWC;

            public LWTransfer(LWComponent c) {
                this.LWC = c;
            }
    
            public DataFlavor[] getTransferDataFlavors() {
                return LWFlavors;
            }
            
            public boolean isDataFlavorSupported(DataFlavor flavor)
            {
                if (DEBUG.DND) out("LWTransfer: isDataFlavorSupported, flavor=" + flavor);
                
                if (flavor == null)
                    return false;
                
                for (int i = 0; i < LWFlavors.length; i++)
                    if (flavor.equals(LWFlavors[i]))
                        return true;

                return false;
            }
    
            public synchronized Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, java.io.IOException
            {
                //tufts.Util.printStackTrace("GTD " + flavor.getHumanPresentableName());
                // Note: the sun transfer handler (for drops to java) always requests
                // all data for all types during a drop, which is a horrible waste in
                // that we need to create a whole image every time, even it it's just
                // the resource being dropped.  If want to optimize this out, would need
                // to create our own Image class that delays creation of the actual
                // image until something is requested of it.  Drops to the OS, at
                // least in the case of MacOSX are smart and only request data for
                // what is ultimately dropped.
                
                if (DEBUG.DND && DEBUG.META) System.err.print("<LWTransfer.getTransferData("
                                                  + flavor.getHumanPresentableName() + ")>");
        
                Object data = null;
        
                if (DataFlavor.stringFlavor.equals(flavor)) {
                    
                    String s = null;
                    if (LWC instanceof LWMap && ((LWMap)LWC).getFile() != null)
                        s = ((LWMap)LWC).getFile().getAbsolutePath();
                    if (LWC.hasResource())
                        s = LWC.getResource().toString();
                    if (s == null && LWC.hasLabel())
                        s = LWC.getLabel();
                    if (s == null && LWC.hasNotes())
                        s = LWC.getNotes();
                    if (s == null)
                        s = LWC.getDisplayLabel();
                    //if (s != null) s += "\n";
                    data = s;
                    
                } else if (DataFlavor.imageFlavor.equals(flavor)) {
                    
                    data = LWC.getAsImage();
                    
                } else if (LWComponent.DataFlavor.equals(flavor)) {
                    
                    final java.util.Collection duplicates;
                    if (LWC == draggedSelectionGroup) {
                        duplicates = Actions.duplicatePreservingLinks(LWC.getChildList());
                    } else if (LWC instanceof LWMap) {
                        // don't send the actual map just yet...
                        duplicates = Actions.duplicatePreservingLinks(LWC.getChildList());
                    } else {
                        duplicates = java.util.Collections.singletonList(LWC.duplicate());
                    }
                        
                    data = duplicates;
                    
                } else if (Resource.DataFlavor.equals(flavor)) {

                    data = LWC.getResource();

                } else if (URLFlavor.equals(flavor) && LWC.getResource() instanceof URLResource) {

                    data = ((URLResource)LWC.getResource()).asURL();

                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
        
                //if (DEBUG.DND) out("LWTransfer: returning " + Util.tag(data));

                return data;
            }
        }
    

    public MouseWheelListener getMouseWheelListener() {
        return inputHandler;
    }

    protected void setToDrag(LWSelection s) {
        //if (s.only() instanceof LWSlide) s.clear(); // okay, this stopped us from picking up the slide, but too soon: can't change BG color
        if (s.size() > 0 && s.first().isMoveable() && activeTool.supportsSelection()) {
            if (DEBUG.WORK) out("set to drag " + s);
            draggedSelectionGroup.useSelection(s);
            setDragger(draggedSelectionGroup);
        } else {
            if (DEBUG.WORK) out("drag not allowed for " + s);
            setDragger(null);
        }
    }

    protected void setDragger(LWComponent c) {
        //out("\n***DRAG SET TO " + c);
        dragComponent = c;
        //if (c instanceof LWGroup) tufts.Util.printStackTrace("DRAGGERSET");
    }


    // todo: if java ever supports moving an inner class to another file,
    // move the InputHandler out: this file has gotten too big.
    // or: just get rid of this and make it all MapViewer methods.
//     private class InputHandler extends tufts.vue.MouseAdapter
//         implements java.awt.event.KeyListener, java.awt.event.MouseWheelListener
//     {
    
        LWComponent dragComponent;//todo: RENAME dragGroup -- make a ControlListener??
        LWSelection.ControlListener dragControl;
        //boolean isDraggingControlHandle = false;
        int dragControlIndex;
        boolean mouseWasDragged = false;
        LWComponent justSelected;    // for between mouse press & click
        boolean hitOnSelectionHandle = false; // we moused-down on a selection handle

    //MapViewer viewer; // getting ready to move this to another file.
    //InputHandler(MapViewer viewer) { this.viewer = viewer; }
        
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
        private static DockWindow DebugInspector;
        private static DockWindow DebugIntrospector;
        private DockWindow debugPanner;
        public void keyPressed(KeyEvent e) {
            if (DEBUG.KEYS) out("[" + e.paramString() + "] consumed=" + e.isConsumed());
            
            viewer.clearTip();
            
            // FYI, Java 1.4.1 sends repeat key press events for non-modal keys that are
            // being held down (e.g. not for shift, buf for spacebar)
            
            // Check for temporary tool activation via holding a key down.  Only one can
            // be active at a time, so this is ignored if anything is already set.
            
            // todo: we'll probably want to change this to a general tool-activation
            // scheme, and the active tool class will handle setting the cursor.  e.g.,
            // dispatchToolKeyPress(e);
            
            
            final int keyCode = e.getKeyCode();
            final char keyChar = e.getKeyChar();
            boolean handled = true;

            switch (keyCode) {
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                // todo: can't we add this to a keymap for the MapViewer JComponent?
                // (Why doesn't the entry for this in the Edit menu auto-provide this mapping?)
                if (!e.isConsumed() && Actions.Delete.enabled())
                    Actions.Delete.fire(this);
                else
                    handled = false;
                break;

            case KeyEvent.VK_ENTER:
                if (!(mFocal instanceof LWMap) && !(this instanceof tufts.vue.ui.SlideViewer)) { // total SlideViewer hack...
                    handled = popFocal(e.isShiftDown());
                } else if (Actions.Rename.isUserEnabled()) {
                    // since removing this action from the main menu, we have to fire it manually
                    // todo: handle this kind of thing generically (make sure all action key bindings installed)
                    Actions.Rename.fire(this);
                } else
                    handled = false;
                break;
                
            case KeyEvent.VK_ESCAPE: // general abort

                if (dragComponent != null) {
                    double oldX = viewer.screenToMapX(dragStart.x) + dragOffset.x;
                    double oldY = viewer.screenToMapY(dragStart.y) + dragOffset.y;
                    dragComponent.setMapLocation(oldX, oldY);
                    //dragPosition.setLocation(oldX, oldY);
                    setDragger(null);
                    activeTool.handleDragAbort();
                    mouseWasDragged = false;
                    clearIndicated(); // incase dragging new link
                    // TODO: dragControl not abortable...
                    repaint();
                } if (draggedSelectorBox != null) {
                    // cancel any drags
                    draggedSelectorBox = null;
                    isDraggingSelectorBox = false;
                    repaint();
                } else if (VUE.inFullScreen()) { // todo: can now more cleanly just handle this in FullScreen.FSWindow
                    VUE.toggleFullScreen(false, true);
                    if (mFocal != null)
                        loadFocal(mFocal.getMap()); // make sure top-level map is displayed
                    if (activeTool instanceof PresentationTool) // todo: need to do this in a more centralized location...
                        activateTool(VueTool.getInstance(SelectionTool.class));
                } else
                    handled = false;
                break;
                
            case KeyEvent.VK_BACK_SLASH:
                // too easy to accidentally hit this instead of the return
                // key while in presentation mode, so only allow if
                // not already in full-screen mode.
                //if (anyModifierKeysDown(e) || !DEBUG.Enabled || VUE.inFullScreen()) {
                if (anyModifierKeysDown(e) || VUE.inFullScreen()) {
                    // do NOT fire this internal shortcut of '\' for fullscreen
                    // if the actual action (Command-\) was fired.
                    handled = false;
                } else
                    VUE.toggleFullScreen(false, true);
                break;
                // fallthru:
//             case KeyEvent.VK_F11:
//                 if (!e.isConsumed())
//                     VUE.toggleFullScreen(false, true);
//                 break;
            default:
                handled = false;
            }

            if (!handled) {
                handled = activeTool.handleKeyPressed(e);
                if (handled) {
                    if (DEBUG.KEYS) out(e.paramString() + "; key handled by current tool: " + activeTool);
                    e.consume();
                    return;
                }
            }
            
            handled = true;
            
            switch (keyCode) {
                
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:

                if (VueSelection.isEmpty() || !Actions.NudgeAction.enabledOn(VueSelection)) {
                    if (e.isShiftDown()) {
                        // micro 1-pixel scroll adjustments (default in scroll-pane is bigger)
            	
                             if (keyCode == KeyEvent.VK_UP)    viewer.panScrollRegion( 0,-1);
                        else if (keyCode == KeyEvent.VK_DOWN)  viewer.panScrollRegion( 0, 1);
                        else if (keyCode == KeyEvent.VK_LEFT)  viewer.panScrollRegion(-1, 0);
                        else if (keyCode == KeyEvent.VK_RIGHT) viewer.panScrollRegion( 1, 0);
                    } else
                        handled = false;
                    
                } else if (!e.isAltDown() && !e.isMetaDown() && !e.isControlDown()) {
                    
                    // there's something in the selection, and only shift might be down: apply big or small nudge
                    if (e.isShiftDown()) {
                             if (keyCode == KeyEvent.VK_UP)    Actions.BigNudgeUp.fire(this);
                        else if (keyCode == KeyEvent.VK_DOWN)  Actions.BigNudgeDown.fire(this);
                        else if (keyCode == KeyEvent.VK_LEFT)  Actions.BigNudgeLeft.fire(this);
                        else if (keyCode == KeyEvent.VK_RIGHT) Actions.BigNudgeRight.fire(this);
                    } else {
                             if (keyCode == KeyEvent.VK_UP)    Actions.NudgeUp.fire(this);
                        else if (keyCode == KeyEvent.VK_DOWN)  Actions.NudgeDown.fire(this);
                        else if (keyCode == KeyEvent.VK_LEFT)  Actions.NudgeLeft.fire(this);
                        else if (keyCode == KeyEvent.VK_RIGHT) Actions.NudgeRight.fire(this);
                    }
                } else
                      handled = false;
                break;
                
            default:
                handled = false;
            }
            

            if (handled) {
                e.consume();
                return;
            }
            
            /*if (VueUtil.isMacPlatform() && tempToolKeyDown == KEY_TOOL_PAN) {
                // toggle cause mac auto-repeats space-bar screwing everything up
                // todo: is this case only on my G4 kbd or does it happen on
                // USB kbd w/external screen also?

                toolKeyEvent = null;
                setCursor(CURSOR_DEFAULT);
                return;
                }*/
            
            if (!activeTool.isLockingActiveTool()) {
            
                // Check for shortcut-keys that would activate another tool:
                
                // If any modifier keys down, may be an action command.
                // Is actually okay if a mouse is down while we do this tho.

                if ((e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0 && (!sDragUnderway || isDraggingSelectorBox)) {
                    for (VueTool tool : VueTool.getTools()) {
                        if (tool.getShortcutKey() == keyChar) {
                            VueToolbarController.getController().setSelectedTool(tool);
                            return;
                        }
                    }
                }
            }
            

            if (tempToolKeyDown == 0 && !isDraggingSelectorBox && !sDragUnderway && keyCode != 0) {
                VueTool tempTool = null;
                for (VueTool tool : VueTool.getTools()) {
                    if (tool.getActiveWhileDownKeyCode() == keyCode) {
                        tempTool = tool;
                        break;
                    }
                }
                if (tempTool != null) {
                    tempToolKeyDown = keyCode;
                    tempToolWasActive = activeTool;
                    if (keyCode == KeyEvent.VK_CONTROL ||
                        keyCode == KeyEvent.VK_ALT ||
                        keyCode == KeyEvent.VK_SHIFT ||
                        keyCode == KeyEvent.VK_META) {
                        // for temp tool activators that are modifier keys (e.g.,
                        // LinkTool), we wait until we get a mouse pressed before fully
                        // selecting because the modifier keys are too generally
                        // used/pressed to change the cursor for every time we hold it
                        // down.
                        // TODO: we're also currently requiring a click on a node(!),
                        // which is still the old special case link-tool code.
                        tempToolPendingActivation = tempTool;
                    } else
                        activateTool(tempTool, true);
                }
            }
            
            
            //-------------------------------------------------------
            // DEBUGGING
            //-------------------------------------------------------
            
            if (/*e.isShiftDown() &&*/ !e.isControlDown() && !e.isMetaDown() && !e.isAltDown()) {
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
                else if (c == 'D') { DEBUG.DOCK = !DEBUG.DOCK; }
                else if (c == 'E') { DEBUG.EVENTS = !DEBUG.EVENTS; }
                else if (c == 'F') { DEBUG.FOCUS = !DEBUG.FOCUS; }
                //else if (c == 'F') { DEBUG_FINDPARENT_OFF = !DEBUG_FINDPARENT_OFF; }
                else if (c == 'I') { DEBUG.IMAGE = !DEBUG.IMAGE; }
                else if (c == 'K') { DEBUG.KEYS = !DEBUG.KEYS; }
                else if (c == 'L') { DEBUG.LAYOUT = !DEBUG.LAYOUT; }
                else if (c == 'M') { DEBUG.MOUSE = !DEBUG.MOUSE; }
                else if (c == 'm') { DEBUG.MARGINS = !DEBUG.MARGINS; }
                else if (c == 'N') { DEBUG.NAV = !DEBUG.NAV; }
                else if (c == 'O') { DEBUG_SHOW_ORIGIN = !DEBUG_SHOW_ORIGIN; }
                else if (c == 'P') { DEBUG.PAINT = !DEBUG.PAINT; }
                else if (c == 'Q') { DEBUG_RENDER_QUALITY = !DEBUG_RENDER_QUALITY; }
                else if (c == 'R') { DEBUG.RESOURCE = !DEBUG.RESOURCE; }
                //else if (c == 'r') { DEBUG_TIMER_ROLLOVER = !DEBUG_TIMER_ROLLOVER; }
                else if (c == 'S') { DEBUG.SELECTION = !DEBUG.SELECTION; }
                else if (c == 'T') { DEBUG.TOOL = !DEBUG.TOOL; }
                else if (c == 'U') { DEBUG.UNDO = !DEBUG.UNDO; }
                else if (c == 'V') { DEBUG.VIEWER = !DEBUG.VIEWER; }
                else if (c == 'W') { DEBUG.WORK = !DEBUG.WORK; }
                else if (c == 'r') { DEBUG.ROLLOVER = !DEBUG.ROLLOVER; }
                else if (c == 'X') { DEBUG.TEXT = !DEBUG.TEXT; }
                else if (c == 'Z') { resetScrollRegion(); }
                
                else if (c == '@') { DEBUG.PRESENT = !DEBUG.PRESENT; }
                else if (c == '&') { DEBUG_FONT_METRICS = !DEBUG_FONT_METRICS; }
                else if (c == '^') { DEBUG.DR = !DEBUG.DR; }
                else if (c == '+') { DEBUG.META = !DEBUG.META; }
                else if (c == '?') { DEBUG.SCROLL = !DEBUG.SCROLL; }
                else if (c == '{') { DEBUG.PATHWAY = !DEBUG.PATHWAY; }
                else if (c == '}') { DEBUG.PARENTING = !DEBUG.PARENTING; }
                else if (c == '>') { DEBUG.DND = !DEBUG.DND; }
                else if (c == '<') { DEBUG.PICK = !DEBUG.PICK; }
                else if (c == '=') { DEBUG.THREAD = !DEBUG.THREAD; }
                else if (c == ';') { DEBUG.LINK = !DEBUG.LINK; }
                else if (c == '(') { DEBUG.setAllEnabled(true); }
                else if (c == ')') { DEBUG.setAllEnabled(false); }
                else if (c == '|') { VUE.toggleFullScreen(true); }
                //else if (c == '*') { tufts.vue.action.PrintAction.getPrintAction().fire(this); }
                //else if (c == '&') { tufts.macosx.Screen.fadeFromBlack(); }
                //else if (c == '@') { tufts.macosx.Screen.setMainAlpha(.5f); }
                //else if (c == '$') { tufts.macosx.Screen.setMainAlpha(1f); }
                else if (c == '~') { System.err.println("MapViewer debug abort."); System.exit(-1); }
                else if (c == '_') { DEBUG.DYNAMIC_UPDATE = !DEBUG.DYNAMIC_UPDATE; }
                else if (c == '*') { OPTIMIZED_REPAINT = !OPTIMIZED_REPAINT; }
                //else if (c == '\\') { VUE.toggleFullScreen(); }
                //else if (c == '|') { VUE.toggleFullScreen(true); // native full screen mode }
                else if (c == '!') {
                    DockWindow introspector = null;
                    if (DebugInspector == null) {
                        DebugInspector = GUI.createDockWindow("Inspector", new LWCInspector());
                        DebugIntrospector = GUI.createDockWindow("Inspector", new LWCInspector.Introspector());
                        DebugInspector.setWidth(500);
                        // below code creates DockWindow's that screw up the DockWindow layering
                        // in VUE, tho I think had the advantage of more reliably working in test cases
                        // where we're running a stand-alone MapViewer.
//                             = new DockWindow("Inspector",
//                                              SwingUtilities.getWindowAncestor(MapViewer.this),
//                                              new LWCInspector(),
//                                              false);
//                             = new DockWindow("Introspector",
//                                              SwingUtilities.getWindowAncestor(MapViewer.this),
//                                              new LWCInspector.Introspector(),
//                                              false);
                    }
                    DebugInspector.setVisible(true);
                    DebugIntrospector.setVisible(true);
                } else if (c == '@') {
                    if (debugPanner == null)
                        debugPanner = new DockWindow("Panner", new MapPanner());
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

            if (tempToolKeyDown == e.getKeyCode()) {
                // Don't revert tmp tool if we're in the middle of a drag
                if (sDragUnderway)
                    tempToolKeyReleased = true;
                else
                    revertTemporaryTool();
            }
            
            /*
            if (tempToolKeyDown == e.getKeyCode()) {
                //if (! (VueUtil.isMacPlatform() && tempToolKeyDown == KEY_TOOL_PAN)) {
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
            
//             LWSelection.Controller[] ctrlPoints = cl.getControlPoints();
//             for (int i = 0; i < ctrlPoints.length; i++) {
//                 Point2D.Double cp = ctrlPoints[i];
            int i = -1;
            for (LWSelection.Controller cp : cl.getControlPoints(getZoomFactor())) { // TODO OPT: already got them for drawing!
                i++;
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
                    if (DEBUG.MOUSE||DEBUG.LAYOUT||DEBUG.SELECTION) {
                        out("hit on control point " + i + " of controlListener " + cl + " s=" + VueSelection);
                    }
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

    private boolean activeToolAteMousePress = false;
    
    // TODO: if APPLE (Command) down when drag starts, do NOT select the object,
    // so can drag copies off map into slide viewer more easily (if it selects
    // on the map, it will swap out the slide displayed!)
        public void mousePressed(MouseEvent e) {
            //boolean wasFocusOwner = isFocusOwner();
            // Due to a bug in the focus system, sometimes we really should have been
            // the focus owner, but we mysteriously lost it to "null", so for now all
            // presses will be recognized, even on *application* focus gains (which is
            // what wasFocusOwner is there to detect) todo: fix
            boolean wasFocusOwner = true;
            
            if (DEBUG.MOUSE || DEBUG.FOCUS) {
                System.out.println("-----------------------------------------------------------------------------");
                out("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "] focusOwner=" + wasFocusOwner);
            }

            mLabelEditWasActiveAtMousePress = (activeTextEdit != null);
            if (DEBUG.FOCUS) System.out.println("\tmouse-pressed active text edit="+mLabelEditWasActiveAtMousePress);
            // TODO: if we didn' HAVE focus, don't change the selection state --
            // only use the mouse click to gain focus.
            viewer.clearTip();
            grabVueApplicationFocus("mousePressed", e);

            if (wasFocusOwner == false && !GUI.isMenuPopup(e)) {
                //if (DEBUG.FOCUS) out("ignoring click on viewer focus gain");
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                VUE.Log.info(MapViewer.this + " ignoring click on viewer focus gain; focusOwner=" + owner);
                e.consume();
                return;
            }

            //out("BUTTON " + e.getButton() + " mods: " + e.getModifiers() + " modEx: " + e.getModifiersEx() + " b2dm=" + InputEvent.BUTTON2_DOWN_MASK);
            if (e.getButton() == 0 && (e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
                // sometimes pressing the mouse-wheel sends an event that looks like this
                tufts.vue.ZoomTool.setZoomFit();
                return;
            }
            
            dragStart.setLocation(e.getX(), e.getY());
            if (DEBUG.MOUSE) System.out.println("dragStart location set to " + dragStart);
            
            if (activeTool == HandTool) {
                if (DEBUG.MOUSE) out("HandTool grabbing mouse");
                originAtDragStart = getOriginLocation();
                if (inScrollPane)
                    viewportAtDragStart = mViewport.getViewPosition();
                else
                    viewportAtDragStart = null;
                return;
            }
            
            setLastMousePressPoint(e.getX(), e.getY());
            
            setDragger(null);
            
            //-------------------------------------------------------
            // Check for hits on selection control points
            //-------------------------------------------------------
            
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            
            MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, null, null);
            
            if (activeTool.handleMousePressed(mme)) {
                activeToolAteMousePress = true;
                return;
            }
            
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
                //hitComponent = activeTool.pickNodeAt(getPickContext(mapX, mapY));
                hitComponent = pickNode(mapX, mapY);
                if (DEBUG.MOUSE && hitComponent != null)
                    System.out.println("\t    on " + hitComponent + "\n" +
                    "\tparent " + hitComponent.getParent());
                // if a LWSlide picked, animate zoom into it, and then load as focal
                mme.setPicked(hitComponent);
                
                // this is a hack:
                if (hitComponent instanceof LWSlide && mFocal instanceof LWMap) {
                    LWComponent node = ((LWSlide)hitComponent).getSourceNode();
                    if (node != null) {
                        LWPathway.Entry entry = node.getEntryToDisplay();
                        if (entry != null)
                            VUE.setActive(LWPathway.Entry.class, this, entry);
                    }
                }                                  
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
            if (GUI.isMenuPopup(e) && !activeTool.usesRightClick()) {
                if (hitComponent != null && !hitComponent.isSelected())
                    selectionSet(justSelected = hitComponent);
                
                //-------------------------------------------------------
                // MOUSE: We've pressed the right button down, so pop
                // a context menu depending on what's in selection.
                //-------------------------------------------------------
                displayContextMenu(e, hitComponent);
                return;
            }
            else if (hitComponent != null) {
                // special case handling for KEY_TOOL_LINK which
                // doesn't want to be fully activated till the
                // key is down (ctrl) AND the left mouse has been
                // pressed over a component to drag a link off.
                if (tempToolPendingActivation != null) {
                    activateTool(tempToolPendingActivation, true);
                    tempToolPendingActivation = null;
                }
                
                //-------------------------------------------------------
                // MOUSE: We've pressed the left (normal) mouse on SOME LWComponent
                //-------------------------------------------------------
                
                activeTool.handleComponentPressed(mme);
                
                if (mme.getDragRequest() != null) {
                    setDragger(mme.getDragRequest()); // TODO: okay, at least HERE, dragComponent CAN be a real component...
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

                    setToDrag(VUE.getSelection());

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
                    && (noModifierKeysDown(e) || isSystemDragStart(e))
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
                    setToDrag(VUE.getSelection());
                    //draggedSelectionGroup.useSelection(VueSelection);
                    //setDragger(draggedSelectionGroup);
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
//                 if (activeTool.supportsDraggedSelector(mme))
//                     isDraggingSelectorBox = true;
//                 else
//                     isDraggingSelectorBox = false;// todo ??? this was true?
            }

            if (dragComponent == null) {
                if (activeTool.supportsDraggedSelector(mme))
                    isDraggingSelectorBox = true;
                else
                    isDraggingSelectorBox = false;
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
            
            if (VueSelection.isEmpty() || VueSelection.only() instanceof LWMap) {
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
            //if (DEBUG.PAINT && redrawingSelector) System.out.println("dragResizeSelectorBox: already repainting selector");
            
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

            if (inScrollPane && !(e.isMetaDown() || e.isAltDown())) {
                // Do not consume, and let the event be passed
                // on to the BasicScrollPaneUI via MouseWheelRelay
                // in MapScrollPane.
                return;
            }

            e.consume();
            
            int rotation = e.getWheelRotation();
            if (rotation > 0)
                tufts.vue.ZoomTool.setZoomSmaller(null);
            else if (rotation < 0)
                tufts.vue.ZoomTool.setZoomBigger(null);
            //lastRotationTime = System.currentTimeMillis();
        }

        private Timer mTipTimer = new Timer();
        private boolean mMouseHasEnteredToolTip = false;
        
        private class ClearTipTask extends TimerTask {
            final Object tipWhenTimerStarted;
            final long tipDisplayInstance;
            ClearTipTask() {
                // note: we should be synchronized on sTipLock when this is constructed
                tipWhenTimerStarted = sTipComponent;
                tipDisplayInstance = sTipDisplayInstance;
                if (DEBUG.FOCUS)
                    out("ClearTipTask: scheduled for " + GUI.name(tipWhenTimerStarted) + " #" + tipDisplayInstance);
            }
            public void run() {

                synchronized (sTipLock) {
                
                // the the given tip component
                // if mouse has exited the viewer, we currently assume
                // it's eneter a tip window (tho it may also have have left
                // for elsewhere).
                //out("over nothing timeout " + VueUtil.objectTag(tipWhenTimerStarted) + " #" + tipDisplayInstance);
                    if (DEBUG.FOCUS) out("ClearTipTask: over nothing timeout for "
                                         + "#" + tipDisplayInstance
                                         + " during instance #" + sTipDisplayInstance);
                    if (mMouseHasEnteredToolTip == false) {
                        //if (sTipComponent == tipWhenTimerStarted && sTipDisplayInstance == tipDisplayInstance) {
                        if (sTipDisplayInstance == tipDisplayInstance) {
                            if (DEBUG.FOCUS) out("ClearTipTask: clearing tip " + sTipDisplayInstance);
                            viewer.clearTip();
                        }
                    } else
                        if (DEBUG.FOCUS) out("ClearTipTask: mouse entered tooltip (it exited map)");

                    
                }
                
            }
        }

        /** clear the current tip in a short while (e.g., 500ms) if we don't
            move into the tip itself, or, say, briefly move away and back
            to a tip region that activates the tip again */
        private void clearTipSoon() {
            synchronized (sTipLock) {
                if (sTipComponent != null)
                    mTipTimer.schedule(new ClearTipTask(), 500);
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            
            
            final float mapX = screenToMapX(e.getX());
            final float mapY = screenToMapY(e.getY());
            
            final PickContext pc = getPickContext(mapX, mapY);
            
            // for mouseEntered/mouseOver on map components, always use the deepest pick available
            // Note: will NOT want to do this if items such as groups will ever have a meaningful
            // mouseOver of their own.  If that's ever the case, just use:
            // final LWComponent hit = pickNode(mapX, mapY);
            // which will rely on the active tool for it's depth as is the default.
            
            pc.pickDepth = Short.MAX_VALUE;
            final LWComponent hit = LWTraversal.PointPick.pick(pc);
        
            
            if (DEBUG.ROLLOVER) System.out.println("  mouseMoved: hit="+hit);

            final MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, hit, null);

            if (hit != sLastMouseOver) {
                // we're over something different than we were
                if (DEBUG.ROLLOVER||DEBUG.FOCUS)
                    System.out.println("  mouseMoved: transition from " + sLastMouseOver);

                if (sLastMouseOver != null) {
                    // we were over a node (not just empty map space)
                    //viewer.clearTip(); // in case it had a tip displayed

                    //if (sLastMouseOver == mRollover && allowsZoomedRollover(hit)) // do we still need this?
                    //    clearRollover();
                    
                    sLastMouseOver.mouseExited(mme);
                }


                if (hit == null) {
                    // we were over something, now we're over nothing
                    mMouseHasEnteredToolTip = false;
                    clearTipSoon();
                }
                
                
            }

            boolean handled = false;
            if (activeTool.handleMouseMoved(mme)) {
                // don't process per-node mouse-over
                handled = true;
            } else if (hit != null) {
                //MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, hit, null);
                if (hit == sLastMouseOver)
                    hit.mouseMoved(mme);
                else
                    hit.mouseEntered(mme);
            } else {

                // We're currently over nothing (just empty map space).
                
                // TODO: for interactive tip regions, we want to allow
                // 500ms or so of time over nothing to move into the
                // tip region, in which case we should not clear the
                // tip.  Also need to handle this for the case where
                // this is a child node, and we briefly mouse over the
                // parent to get to the tip region.


                //viewer.clearTip(); // if over nothing, always make sure no tip displayed
            }
            
            if (DEBUG.PICK && !handled) {
                if (hit != null)
                    setIndicated(hit);
                else
                    clearIndicated();
            }
                
            sLastMouseOver = hit;
            
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

            if (getAutoZoomEnabled() && RolloverAutoZoomDelay > 0) {
                if (DEBUG_TIMER_ROLLOVER && !sDragUnderway && activeTextEdit == null) {
                    if (RolloverAutoZoomDelay > 10 && mRollover == null) {
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
            if (DEBUG.MOUSE||DEBUG.ROLLOVER) out(e.paramString());

            if (VUE.isStartupUnderway()) {
                // we are getting hangs on Windows 2000 if mouse
                // enter's map during startup.
                return;
            }
            
            if (sLastMouseOver != null) {
                sLastMouseOver.mouseExited(new MapMouseEvent(e));
                sLastMouseOver = null;
            }

            mMouseHasEnteredToolTip = false;
            clearTipSoon();
            //grabVueApplicationFocus("mouseEntered", e);

        }
    
        public void mouseExited(MouseEvent e) {
            if (DEBUG.MOUSE||DEBUG.ROLLOVER) out(e.paramString());

            if (sLastMouseOver != null && sLastMouseOver == mRollover)
                clearRollover();

            if (false && sLastMouseOver != null) {
                sLastMouseOver.mouseExited(new MapMouseEvent(e));
                sLastMouseOver = null;
            }
            
            //-----------------------------------------------------------------------------
            // If you roll the mouse into a tip window, the MapViewer will get a
            // mouseExited -- we clear the tip if this happens as we never want the tip
            // to obscure anything.  This is slighly dangerous in that if for some
            // reason the tip has been placed over it's own activation region, and you
            // put the mouse over the intersection area of the tip and the activation
            // region, we'll enter a show/hide loop: mouse into trigger region pops tip
            // window, which comes up under where the mouse is already at, immediately
            // triggering a mouseExited on the MapViewer, which bring us here in
            // mouseExited to clear the tip, and when it clears, the mouse enters the
            // map again, and triggers the tip window again, looping for as long as you
            // leave the mouse there (because you can still move the mouse away this
            // isn't a fatal error).  But since this is still very undesirable, we take
            // great pains in placing the tip window to never overlap the trigger
            // region. (see setTip)
            
            // -- turned off for now to allow us to mouse into the tip region
            //            viewer.clearTip();
            //-----------------------------------------------------------------------------


            // assume mouse has entered tool-tip, tho actually it may have just left the MapViewer
            // TODO: if we fix popup focus bug, make this know if it's really entering the tip
            // (maybe FOCUS_LOST opposite component)
            mMouseHasEnteredToolTip = true;

            // Is still nice to do this tho because we get a mouse exited when you
            // rollover the tip-window itself, and if it's right at the edge of the node
            // and you're going for the resize-control, better to have the note clear
            // out so you don't accidentally hit the tip when going for the control.

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

        private final Dimension MaxDragSize = new Dimension(256,256);
        private void startSystemDrag(MouseEvent e)
        {
            final LWComponent toDrag;

            if (VueSelection == null || VueSelection.isEmpty()) {
                // VueSelection could be null if the user
                // moves fast and starts a drag before the
                // viewer has had a chance to become the active one.
                toDrag = mFocal;
            } else if (VueSelection.size() == 1) {
                toDrag = VueSelection.first();
            } else {
                draggedSelectionGroup.useSelection(VueSelection);
                toDrag = draggedSelectionGroup;
            }

            // todo performance: don't need to create a whole image
            // buffer every time we do this drag: can just have
            // an LWComponent method that renders into GUI's
            // cached drag-image buffer -- map drags will start
            // faster and we'll save a ton of allocation & GC
            // (e.g. at 256x256x32bitxRGBA == 1MB per *drag*)
            GUI.startLWCDrag(MapViewer.this,
                             e,
                             toDrag,
                             new LWTransfer(toDrag));
        }

    private boolean isDropRequest(MouseEvent e) {
        return !e.isShiftDown();
    }

        //private int drags=0;
        public void mouseDragged(MouseEvent e) {

            if (DEBUG.VIEWER) _mouse = e.getPoint();

            if (DEBUG.MOUSE && DEBUG.DND) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            clearRollover();
            //System.out.println("drag " + drags++);
            if (mouseWasDragged == false) {

                // TODO: if the active tool claimed to have handled this event on mousePressed,
                // do not allow following drags to do anything...

                if (isSystemDragStart(e)) {
                    startSystemDrag(e);
                    // we'll get no more mouseDragged, and no mouseReleased
                    return;
                }

                // dragStart
                // we're just starting this drag
                //if (inScrollPane || dragComponent != null || dragControl != null) always set mousewasdragged
                if (dragComponent == null && dragControl == null)
                    viewer.setAutoscrolls(false);
                
                mouseWasDragged = true;
                lastDrag.setLocation(dragStart);
                if (DEBUG.MOUSE) System.out.println(" lastDragSet " + out(lastDrag));
                // if we pan, our canvas location might change, offsetting mouse coord each time
                if (inScrollPane)
                    lastDrag = SwingUtilities.convertPoint(MapViewer.this, lastDrag, getParent());

            }
            sDragUnderway = true;
            
            //if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

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

            final float mapX = screenToMapX(screenX);
            final float mapY = screenToMapY(screenY);
            final MapMouseEvent mme = new MapMouseEvent(e, mapX, mapY, null, draggedSelectorBox);
            
            
            if (!activeTool.supportsDraggedSelector(mme) && !activeTool.supportsResizeControls()) 
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
                
                dragComponent.setMapLocation(mapX + dragOffset.x,
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
                    LWComponent c = l.getHead();
                    if (c != null) repaintRegion.add(c.getBounds());
                    c = l.getTail();
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
                     && !(VueSelection != null && VueSelection.allOfType(LWLink.class)) //todo opt: cache type
                     ) {


                // TODO: above VueSelection should never be null.
                
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
                //over = pickDropTarget(mapX, mapY, true);

                PickContext pc = getPickContext(mapX, mapY);
                pc.ignoreSelected = true;

                // TODO: stop using group if just one item in selection, use
                // real component, and just go ahead and have different code
                // for handling resize of single objects and of selections
                if (VueSelection.size() == 1)
                    pc.dropping = VueSelection.first();
                else
                    pc.dropping = dragComponent;

                over = LWTraversal.PointPick.pick(pc);
                
                
                if (indication != null && indication != over) {
                    //repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isDropRequest(e)) { 
                    if (isValidParentTarget(VueSelection, over))
                        setIndicated(over);
                    else if (isValidParentTarget(VueSelection, over.getParent()))
                        setIndicated(over.getParent());
                    //repaintRegion.add(over.getBounds());
                } else
                    clearIndicated();
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
                // this allows dropping into a group
                if (dragComponent == null || !isDropRequest(e))
                    ; // nothing dragged, or shift requst to skip reparenting
                else
                    if (DEBUG.EVENTS) System.out.println(TERM_GREEN + "\nINTERNAL MAP MOUSE DROP EVENT in " + this
                                                         + "\n\t     event: " + e
                                                         + "\n\tindication: " + indication
                                                         + TERM_CLEAR);
                    checkAndHandleDroppedReparenting();
            }
            
            // special case event notification for any other viewers
            // of this map that may now need to repaint (LWComponents currently
            // don't sent event notifications for location & size changes
            // for performance)
            if (mouseWasDragged)
                VUE.getUndoManager().mark("Drag");
            
            if (draggedSelectorBox != null && !activeTool.supportsDraggedSelector(mme))
                System.err.println("Illegal state warning: we've drawn a selector box w/out tool that supports it!");
            
            // reset in-drag only state
            clearIndicated();
            
            if (draggedSelectorBox != null && activeTool.supportsDraggedSelector(mme)) {
                
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

//                     Class selectionType;
//                     if (e.isAltDown())
//                         selectionType = LWNode.class;
//                     else
//                         selectionType = activeTool.getSelectionType();

                    List list = computeSelection(screenToMapRect(draggedSelectorBox));
                    
                    if (e.isShiftDown())
                        selectionToggle(list);
                    else
                        selectionSet(list);
                    
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
//             else if (VueSelection.isEmpty() && e.isShiftDown()) {
//                 selectionSet(mFocal);
//             }
            
            VUE.getUndoManager().mark(); // in case anything happened
            
            if (tempToolKeyReleased) {
                tempToolKeyReleased = false;
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
            setDragger(null);
            isDraggingSelectorBox = false;
            mouseWasDragged = false;
            activeToolAteMousePress = false;
            
            
            // todo opt: only need to do this if we don't draw selection
            // handles while dragging (this is to put them back if we werent)
            // use selection repaint region?
            //repaint();
            
        }
        
        
        /**
         * Take what's in the selection and drop it on the current indication,
         * or on the map if no current indication.
         *
         * @return true if we did anything
         */
        private boolean checkAndHandleDroppedReparenting() {
            //-------------------------------------------------------
            // check to see if any things could be dropped on a new parent
            // This got alot more complicated adding support for
            // dropping whole selections of components, especially
            // if there are embedded children selected.
            //-------------------------------------------------------
            
            LWContainer parentTarget;
            if (indication == null) {
                if (mFocal instanceof LWContainer && mFocal.supportsChildren()) {
                    parentTarget = (LWContainer) mFocal;
                } else {
                    //VUE.Log.debug("MapViewer: drag check of non-container focal " + mFocal);
                    return false;
                }
            } else
                parentTarget = (LWContainer) indication;

            Collection<LWComponent> moveList = new java.util.ArrayList();
            for (LWComponent droppedChild : VueSelection) {
                if (!droppedChild.supportsReparenting())
                    continue;

                if (droppedChild instanceof LWSlide) // todo: something more abstract
                    continue;

                final LWContainer currentParent = droppedChild.getParent();

                if (currentParent == null) // must have grabbed the LWMap
                    continue;
                
                // even tho the indication has already checked this via isValidParentTarget,
                // if there's more than one item in the selection, we still need
                // to do do this check against bad cases -- TODO: not allowing
                // reparenting when child moving from the a group to become
                // a child of another group member.
                if (!currentParent.supportsChildren())
                    continue;
                //  continue; // not with new "page" groups
                // don't do anything if parent might be reparenting
                if (currentParent.isSelected())
                    continue;
                // todo: actually re-do drop if anything other than map so will re-layout
                if (
                    (currentParent != parentTarget || parentTarget instanceof LWNode) &&
                    droppedChild != parentTarget) {
                    //-------------------------------------------------------
                    // we were over a valid NEW parent -- reparent
                    //-------------------------------------------------------
                    if (DEBUG.PARENTING)
                        System.out.println("*** REPARENTING " + droppedChild + " as child of " + parentTarget);
                    moveList.add(droppedChild);
                }
            }
            
            if (moveList.size() > 0) {
            
                // okay -- what we want is to tell the parent we're moving from to remove
                // them all at once -- the problem is our selection could contain components
                // of multiple parents.  So we have to handle each source parent seperately,
                // and remove all it's children at once -- this is so the parent won't
                // re-lay itself out (call layout()) while removing children, because if
                // does it will re-set the position of other siblings about to be removed
                // back to the parent's layout spot from the draggeed position they
                // currently occupy and we're trying to move them to.
                
                Collection<LWContainer> parents = new java.util.HashSet();
                for (LWComponent c : moveList)
                    parents.add(c.getParent());

                for (LWContainer parent : parents) {
                    if (DEBUG.PARENTING)  System.out.println("*** HANDLING PARENT " + parent);
                    parent.reparentTo(parentTarget, moveList);
                    //parent.removeChildren(moveList.iterator());
                }
                
                selectionSet(moveList);
                return true;
            }
            return false;
        }

        private final boolean noModifierKeysDown(InputEvent e) {
            return !anyModifierKeysDown(e);
        }
    
        private final boolean anyModifierKeysDown(InputEvent e) {
            return (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) != 0;
        }
        
        /*
        // for initiating a Drag that can go off the map 
        private static final int SYSTEM_DRAG_MODIFIER =
        InputEvent.BUTTON1_DOWN_MASK
        | (
           VueUtil.isMacPlatform() ?
           InputEvent.META_DOWN_MASK :
           InputEvent.ALT_DOWN_MASK);
        
        private final boolean onlyModifierDown(MouseEvent e, int modifier) {
            //out("RAW MODIFIER TEST [" + InputEvent.getModifiersExText(InputEvent.META_DOWN_MASK + InputEvent.BUTTON1_DOWN_MASK) + "]");
            out("ONLY MODIFIER DOWN CHECKING AGAINST InputEvent [" + InputEvent.getModifiersExText(modifier) + "]");
            out("ONLY MODIFIER DOWN CHECKING AGAINST MouseEVent [" + MouseEvent.getMouseModifiersText(modifier) + "]");
            return (e.getModifiersEx() & modifier) == modifier;
            //return (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == modifier;
        }
        */

        private final int SYSTEM_DRAG_MODIFIER = VueUtil.isMacPlatform() ? InputEvent.META_MASK : InputEvent.ALT_MASK;
    
        private final boolean isSystemDragStart(MouseEvent e) {
            //out("   MODIFIERS ACCORDING TO InputEvent [" + InputEvent.getModifiersExText(e.getModifiers()) + "]");
            //out("   MODIFIERS ACCORDING TO MouseEvent [" + MouseEvent.getMouseModifiersText(e.getModifiers()) + "]");
            //out("EX MODIFIERS ACCORDING TO InputEvent [" + InputEvent.getModifiersExText(e.getModifiersEx()) + "]");
            //out("EX MODIFIERS ACCORDING TO MouseEvent [" + MouseEvent.getMouseModifiersText(e.getModifiersEx()) + "]");
            // button is 0 (!) on the PC, which is why <= 1 compare for getB

            if (VUE.getSelection().only() instanceof LWImage && ((LWImage)VUE.getSelection().only()).isNodeIcon())
                return true;
            else
                return !e.isPopupTrigger() && e.getButton() <= 1 && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == SYSTEM_DRAG_MODIFIER;
        }
        
        private final boolean isDoubleClickEvent(MouseEvent e) {
            return !activeToolAteMousePress
                && e.getClickCount() == 2
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isSingleClickEvent(MouseEvent e) {
            return e.getClickCount() == 1
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        
        public void mouseClicked(MouseEvent e) {
            if (DEBUG.MOUSE) System.out.println("["
                                                + e.paramString()
                                                + (e.isPopupTrigger() ? " POP":"")
                                                + (GUI.isMenuPopup(e) ? " MENU":"")
                                                + "]");
            
            //if (activeTool != ArrowTool && activeTool != TextTool)
            //return;  check supportsClick, and add such to node tool

            // TODO: we want to refactor the below very confusing code
            // and delegate to the tools w/out naming them directly.
            // To do this tho, the tools will need access to
            // mLabelEditWasActiveAtMousePress, for dealing with the
            // incredible subtlety of what happens when you click the
            // mouse.
            
            if (!hitOnSelectionHandle) {
                
                if (isSingleClickEvent(e)) {
                    if (DEBUG.MOUSE) System.out.println("\tSINGLE-CLICK on: " + hitComponent);
                    
                    if (hitComponent != null && hitComponent != mFocal && !(hitComponent instanceof LWGroup)) {
                        
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
                        
                    } else if (activeTool == TextTool || activeTool == tufts.vue.NodeTool.NodeModeTool.getInstance(tufts.vue.NodeTool.NodeModeTool.class)) {
                        
                        // on mousePressed, we request focus, and if there was an
                        // activeTextEdit TextBox, it lost focus and closed itself out
                        // -- treat this click as an edit-cancel in case of node/text
                        // tool so doesn't create a new item if they were just finishing
                        // the edit via the click on the map
                        
                        if (!mLabelEditWasActiveAtMousePress) {
                            if (activeTool == tufts.vue.NodeTool.NodeModeTool.getInstance(tufts.vue.NodeTool.NodeModeTool.class) && (oneClickNodePref.getValue() == Boolean.TRUE))
                                Actions.NewNode.fire(MapViewer.this);
                            else if (activeTool == TextTool)
                                Actions.NewText.fire(MapViewer.this);
                        }
                    }
                /*
                if (activeTool.supportsClick()) {
                    //activeTool.handleClickEvent(e, hitComponent); send in mapxy
                }
                 */
                    
                } else if (isDoubleClickEvent(e) && tempToolKeyDown == 0 && hitComponent != null) {
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
        public boolean isValidParentTarget(LWSelection s, LWComponent parentTarget) {
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
            if (parentTarget instanceof LWMap) // prob don't need this check, but just in case
                return false;
            if (s.size() == 1) {
                if (!s.first().supportsReparenting())
                    return false;
                //if (s.first().getParent() == parentTarget) // it's already the parent: don't bother indicating a parent change
                      //return false; // oops -- is leading to DE-parenting
                // todo: rework so can get away with not indicating the existing parent
            }
            return parentTarget.supportsChildren();
        }
    //} old InputHandler close
    
    private final Runnable focusIndicatorRepaint = new Runnable() { public void run() { mFocusIndicator.repaint(); }};
    
    public void activeChanged(ActiveEvent e, MapViewer viewer) {
        // We delay the repaint request for the focus indicator on this event because normally, it
        // happens while we're grabbing focus, which means it happens twice: once here on active
        // viewer change, and once later when we get the focusGained event.  Since the focus
        // indicator looks different in these two cases, it briefly flashes.  Delaying this paint
        // request ensures no flashing.  We still need to do this repaint on viewer change tho
        // because sometimes we ONLY see this event: e.g., if there is an active text edit (in
        // which cases we're the active viewer, but do NOT have keyboard focus), and then you mouse
        // over to another map, which then grabs the VUE application focus and becomes the active viewer.
        VUE.invokeAfterAWT(focusIndicatorRepaint);

//         if (viewer == this)
//             grabVueApplicationFocus(e.toString(), null);
     }

    
    /*
     * Make this viewer the active viewer (and thus our map the active map.
     * Does NOT call requestFocus to get the keyboard focus, as we don't
     * want to bother doing this if this is, say, from a focusEvent.
     */


    // TODO BUG: When focus switches to the viewer from a text field such as notes or
    // label in the object inspector via mouseEntered, changing the cursor doesn't work
    // (e.g., hold down space bar: no hand cursor, or select different tool) until you
    // actually click on the map.  This may be a java bug, as we succesfully get kbd
    // focus in this case.  As for workarounds: maybe simulate a mouse click event thru
    // AWT?

    private void becomeActiveViewer() {
        MapViewer activeViewer = VUE.getActiveViewer();
        // why are we checking this again if we just checked it???
        if (activeViewer != this) {
            LWMap oldActiveMap = null;
            if (activeViewer != null)
                oldActiveMap = activeViewer.getMap();
            VUE.setActive(MapViewer.class, this, this);
            if (mFocal != null)
                mFocal.getChangeSupport().setPriorityListener(this);
            else
                VUE.Log.warn("Active viewer has no focal: " + this);
            // TODO: VUE.getSelection().setPriorityListener(this);
                
            // hierarchy view switching: TODO: make an active map listener instead of this(?)
            /*
              if (VUE.getHierarchyTree() != null) {
              if (this.map instanceof LWHierarchyMap)
              VUE.getHierarchyTree().setHierarchyModel(((LWHierarchyMap)this.map).getHierarchyModel());
              else
              VUE.getHierarchyTree().setHierarchyModel(null);
              // end of addition by Daisuke
              }
            */
                
            if (oldActiveMap != mMap) {
                if (DEBUG.FOCUS) out("GVAF: oldActive=" + oldActiveMap + " active=" + mMap + " CLEARING SELECTION");
                resizeControl.active = false;
                // clear and notify since the selected map changed.
                VUE.ModelSelection.clear();
                //VUE.ModelSelection.clearAndNotify(); // why must we force a notification here?
            }
        }
    }
    
    public void grabVueApplicationFocus(String from, ComponentEvent event) {
        if (DEBUG.FOCUS || DEBUG.VIEWER) {
            // Util.printStackTrace();
            out("-------------------------------------------------------");
            out("GVAF: grabVueApplicationFocus triggered via " + from);
            if (DEBUG.META && event != null) System.out.println("\t" + event);
        }
        this.VueSelection = VUE.ModelSelection;
        setFocusable(true);

        if (VUE.getActiveViewer() != this) {
            
            if (DEBUG.FOCUS) out("GVAF: " + from + " *** GRABBING ***");
            becomeActiveViewer();
            
        } else {
            if (DEBUG.FOCUS) out("GVAF: already the active viewer");
        }
        
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (DEBUG.FOCUS) out("GVAF: current focus owner: " + GUI.name(focusOwner));
        
        final int id = event == null ? 0 : event.getID();

        boolean requestFocus = true;

        // in these cases, do NOT request the keyboard focus: either we just got it, our we
        // want to let an active on-map text edit keep it.
        if (id == FocusEvent.FOCUS_GAINED)
            requestFocus = false;
        else if (id == MouseEvent.MOUSE_ENTERED && activeTextEdit != null)
            requestFocus = false;
        else if (id == MouseEvent.MOUSE_PRESSED && GUI.isMenuPopup(event))
            requestFocus = false;
        else if (focusOwner == this || focusOwner == getRootPane()) {
            // If we're already focused and request focus, it's possible to lose focus!
            // This can happen when moved to a separate Window (for full-screen), that
            // is a child of the root frame.  Apparently, if the window has no
            // focus-holding components (could we make MapViewer such?), it gives the
            // focus back to the focusCycleRoot, which is ultimately the parent Frame.
            requestFocus = false;
        }

        // TODO: also do NOT grab focus if this is MOUSE_ENTERED and there is a pop-up menu about
        // or: stop grabbing on MOUSE_ENTERED completely (ah: but if we do that, we don't get
        // focus back after the pop-up is gone)
            
            
        if (requestFocus) {

            if (DEBUG.FOCUS) out("GVAF: requesting focus");
            requestFocus();
            
            // When kbd focus switches to the viewer via mouseEntered from a text field such as
            // notes or label in the object inspector, changing the cursor doesn't work (e.g., hold
            // down space bar: no hand cursor). Calling requstFocus works to deliver kdb events to
            // the viewer (we get focusGained), but until you actually click on the map, the
            // containg VueFrame does not get another kind of OS focus: the frame title stays
            // grayed out, and you can't change the mouse cursor.  Calling toFront() on the frame
            // un-grays the frame title, and allows us to change the cursor

            // Update: Is this causing lightweight menu's on the PC to fail to gain
            // focus?  the JRootPane is getting focus, and KBD focus goes to the menu,
            // but MOUSE focus doesn't!  Mouse events go to the map, so you can't even
            // click on a menu item!  This only happens sometimes...

            // As of 2006-01-07, in java 1.5, we're seeing the cursor related focus problem on
            // MacOSX, but not on the PC, and are not seeing the menu problem on the OSX,
            // so we'll only do this on the PC.  (This may be because the top of screen menu's
            // on the mac are always native heavy weight windows).

            // TODO TODO TODO:
            // TODO CRAP: we're now seeing this on the PC even when not doing the toFront...
            // Especially happens in full-screen mode, but easily happens even in regular mode...
            // Basically, when it's lightweight and the JRootPane gets focus, it's
            // a toss-up as to if it's really going to detect the mouse entering
            // the menu...

            // Details: seems to be fine when we get COMPONENT_ADDED for the ###VUE-POPUP###
            // panel, immediately followed by FOCUS_LOST on the MapViewer, but sometimes
            // we get a MOUSE_RELEASED & MOUSE_CLICKED in the MapViewer between the two,
            // and in those cases it seems to go dead...

            // Oh SHIT  :)  I think it's when it REUSES one of the light-weight
            // rollovers that had been locked up??

            // Okay, that's fixed: clean up lockMediumWeightPopup and this and
            // test medium weight pop-up locks again

            // And WHY on the PC do we get a ZILLION focusLost & focusGained calls?
            
            if (Util.isMacPlatform()) {
                try {
                    SwingUtilities.getWindowAncestor(this).toFront();
                    //VUE.getMainWindow().toFront(); // may also be full-screen window
                } catch (NullPointerException e) {} // if no main window, skip it
            }
        }
    }

    public void focusGained(FocusEvent e) {
        final Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (DEBUG.FOCUS) out("focusGained (from " + GUI.name(e.getOppositeComponent()) + ") parentWindow=" + parentWindow);

        if (parentWindow == null || !isDisplayed()) {
            
            // If parentWindow is null, we've been closed -- don't ask me why java will
            // still hand us the focus if we've been removed from the AWT component
            // hierarchy...  Also, if we're not display (e..g, a right viewer when the
            // split-pane is closed), ignore also.  Why we get the focus even if we're
            // not displayed is also quite the java mystery.
            
            return;
        }
        
        // TODO: mac bug, tho maybe only when loading maps from command line:
        // FIRST map selected in tab other than the one showing, properly
        // grabs focus from the MapTabbedPane notification, but then LOSES focus
        // to a DIFFERENT map in the RIGHT viewer (probably the first one there).
        // After this happens once, we no longer seem to have this problem.
        // To workaround, if the opposite component here is a right viewer,
        // and we're a left viewer, and the right viewers aren't showing,
        // do NOT grab the VUE application focus.
        repaintFocusIndicator();
        grabVueApplicationFocus("focusGained", e);
        fireViewerEvent(MapViewerEvent.FOCUSED);
    }
    
    public void focusLost(FocusEvent e) {
        if (DEBUG.FOCUS) out("focusLost (to " + GUI.name(e.getOppositeComponent()) +")");
        
        Component lostTo = e.getOppositeComponent();

        //if (DEBUG.Enabled && lostTo == null) Util.printStackTrace(MapViewer.this + " focus lost to null");
        
        if (VueUtil.isMacPlatform()) {
            
            // On Mac, our manual tool-tip popups sometimes (and sometimes inconsistently) when
            // they are a big heavy weight popups (e.g, 40 lines of notes) will actually grab the
            // focus away from the app!  We request to get the focus back, but it doesn't appear
            // that actually works.
            
            String opName = null;
            if (lostTo != null)
                opName = lostTo.getName();
            // hack: check the name against the special name of Popup$HeavyWeightWindow
            if (GUI.OVERRIDE_REDIRECT.equals(opName)) {
                //if (DEBUG.FOCUS) System.out.println("\tFOCUS LOST TO POPUP");
                VUE.Log.info(MapViewer.this + " focus lost to pop-up (overrideRedirect)");
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

        if (MapScrollPane.UseMacFocusBorder && inScrollPane && GUI.isMacAqua()) {
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

        /*
          // is breaking full-screen on the PC -- this must be an optimization for the right viewer?
        if (doShow && getParent() == null) {
            if (DEBUG.FOCUS) out("IGNORING (parent null)");
            return;
        }
        */

        final boolean isVisible = super.isVisible();
        final boolean changed = doShow != isVisible;

        if (!changed)
            return;

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
        return "MapViewer<" + instanceName + ">"
            + "[" + (mFocal==null?"nil":mFocal.getDiagnosticLabel()) + "]";
        //+ "\'" + (mFocal==null?"nil":mFocal.getDiagnosticLabel()) + "\'";
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
        n.setShape(Rectangle2D.Float.class);
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
        end.setShape(Rectangle2D.Float.class);
        end.setStrokeWidth(0);
        end.setAutoSized(false);
        end.setFrame(300,250, 100,50);
        end.setFillColor(Color.blue);
        Actions.FontBold.actOn(end);
        map.addNode(end);
    }
    
    
    protected void out(Object o) {
        System.out.println(this + " " + (o==null?"null":o.toString()));
    }
    
    protected void out(String method, Object msg) {
        if (method.charAt(0) == '@')
            tufts.Util.printStackTrace(method);
        System.out.format("%s %12s: %s\n", this, method, msg);
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
    protected boolean DEBUG_TIMER_ROLLOVER = true; // todo: preferences
    private boolean DEBUG_FONT_METRICS = false;// fractional metrics looks worse to me --SF
    private boolean OPTIMIZED_REPAINT = false;
    
    private Point _mouse = new Point();
    
    final Object AA_OFF = RenderingHints.VALUE_ANTIALIAS_OFF;
    Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;

    
    private static JFrame debugFrame;
    public static void main(String[] args) {
        System.out.println("MapViewer:main");
        
        DEBUG.Enabled = true;
        VUE.init(args);

        boolean test_zoom = false;
        boolean test_node = false;
        boolean test_map = false;
        boolean show_panner = false;
        boolean use_scroller = false;
        boolean use_menu = false;

        for (int i = 0; i < args.length; i++) {
                 if (args[i].equals("-zoom"))   test_zoom = true;
            else if (args[i].equals("-node"))   test_node = true;
            else if (args[i].equals("-panner")) show_panner = true;
            else if (args[i].equals("-scroll")) use_scroller = true;
            else if (args[i].equals("-menu"))   use_menu = true;
            else if (args[i].equals("-map"))    test_map = true;
        }
        
        
        /*
        javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new VueTheme() {
                public javax.swing.plaf.FontUIResource getControlTextFont() { return fontTiny; }
                public javax.swing.plaf.FontUIResource getMenuTextFont() { return fontTiny; }
                public javax.swing.plaf.FontUIResource getSmallFont() { return fontTiny; }
            });
        */

        LWMap map = new LWMap("test map");

        if (test_map)
            VUE.installExampleMap(map);
        else
            map.addLWC(new LWNode(VueResources.getString("newnode.html"), new Rectangle2D.Float()));
        
        /*
        LWNode tn = new LWNode("one two three", new Rectangle2D.Float());
        tn.setLocation(100,100);
        tn.setFillColor(null);
        tn.setStrokeColor(Color.black);
        map.addLWC(tn);
        */
        
        if (test_zoom) {
            DEBUG.EVENTS = DEBUG.SCROLL = DEBUG.VIEWER = DEBUG.MARGINS = true; // zoom test
            DEBUG.KEYS = DEBUG.MOUSE = true;
            installZoomTestMap(map);
        } else if (test_node) {
            DEBUG.BOXES = true; // node layout test
            installExampleNodes(map);
        }
        
        JFrame frame = null;
        
        if (test_zoom == false && use_menu == false) {
            // raw, simple, non-scrolled mapviewer (WITHOUT actions attached!)
            //DEBUG.FOCUS = true;
            VueUtil.displayComponent(new MapViewer(map), 400,300);

        } else {

            MapViewer viewer = new MapViewer(map);
            //viewer.DEBUG_SHOW_ORIGIN = true;
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
            if (use_menu) {
                JMenuBar menu = new tufts.vue.gui.VueMenuBar();
                menu.setFont(FONT_TINY);
                // set the menu bar just so we can get all the actions connected to MapViewer
                frame.setJMenuBar(menu);
            }
            frame.pack();
            debugFrame = frame;
        }

            
        if (test_zoom || show_panner) {
            DockWindow pannerTool = new DockWindow("Panner", frame);
            pannerTool.setSize(120,120);
            pannerTool.add(new MapPanner());
            pannerTool.setVisible(true);
        }
    }
    
    public static boolean getAutoZoomEnabled()
    {
    	return autoZoomEnabled;
    }

    public static void setAutoZoomEnabled(boolean enabled)
    {
    	autoZoomEnabled = enabled;
    }        
}