package tufts.vue;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import javax.swing.*;
import tufts.oki.dr.fedora.*;

import osid.dr.*;

/**
 * MapViewer.java
 *
 * Implements a panel for displaying & interacting with
 * an instance of LWMap.
 *
 * @author Scott Fraize
 * @version 3/16/03
 */

public class MapViewer extends javax.swing.JPanel
    // We use a swing component instead of AWT to get double buffering.
    // (The mac AWT impl has does this anyway, but not the PC).
    implements VueConstants
               , FocusListener
               , LWComponent.Listener
               , LWSelection.Listener
               , VueToolSelectionListener
{
    // rename this class LWViewer or LWCanvas?
    static final int  RolloverAutoZoomDelay = VueResources.getInt("mapViewer.rolloverAutoZoomDelay");
    static final int  RolloverMinZoomDeltaTrigger_int = VueResources.getInt("mapViewer.rolloverMinZoomDeltaTrigger", 10);
    static final float RolloverMinZoomDeltaTrigger = RolloverMinZoomDeltaTrigger_int > 0 ? RolloverMinZoomDeltaTrigger_int / 100f : 0f;
    
    private Rectangle2D.Float RepaintRegion = null; // could handle in DrawContext
    private Rectangle paintedSelectionBounds = null;

    /**By Daisuke */
    private boolean outlineMode = false;
    private OutlineViewTree tree;
    private JScrollPane scrollPane;
    
    public void setMode(boolean flag)
    {   
        //deals with switching the display mode 
        //if it tries to switch to do the same mode, it does nothing
        
        if (outlineMode == false && flag == true)
        {
           outlineMode = flag;
           
           setBackground(Color.white);
           scrollPane.setVisible(true);
           
           //setLayout(new BorderLayout());
           //add(scrollPane, BorderLayout.CENTER);  
           //invalidate();
           //validate();
           revalidate();
        }
        
        else if (outlineMode == true && flag == false)
        {
           outlineMode = flag;
           
           setBackground(null);
           scrollPane.setVisible(false);
           
           //remove(scrollPane);
           //setLayout(null);
           //invalidate();
           //validate();
           revalidate();
        }
        
        System.out.println("about to issue a repaint()");
        repaint();
    }
    
    /**End*/
    
    public interface Listener extends java.util.EventListener
    {
        public void mapViewerEventRaised(MapViewerEvent e);
    }

    protected LWMap map;                   // the map we're displaying & interacting with
    private TextBox activeTextEdit;          // Current on-map text edit


    // resize stuff: todo: move to seperate tool
    // better: make a "ResizeControl" -- a control abstraction that's
    // less than a whole VueTool -- it depends on the current selection,
    // but can still do some drawing on the map while active --
    // (generically, something like a SelectionController -- provides ControlPoints)
    //private LWSelection.ControlPoint[] resizeHandles = new LWSelection.ControlPoint[8];
    //private boolean resizeHandlesActive = false;
    ResizeControl resizeControl = new ResizeControl();

    //-------------------------------------------------------
    // Selection support
    //-------------------------------------------------------

    /** an alias for the global selection -- sometimes taking on the value null */
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
    protected boolean inDrag;
    //protected Point2D.Float dragPosition = new Point2D.Float();

    protected LWComponent indication;   // current indication (drag rollover hilite)
    protected LWComponent rollover;   // current rollover (mouse rollover hilite)

    //-------------------------------------------------------
    // Pan & Zoom Support
    //-------------------------------------------------------
    private double zoomFactor = 1.0;
    private float mapOriginX = 0;
    private float mapOriginY = 0;

    //-------------------------------------------------------
    //
    //-------------------------------------------------------
    
    private boolean isRightSide = false;
    MapViewer(LWMap map, boolean rightSide)
    {
        this(map);
        this.isRightSide = true;
    }

    private InputHandler inputHandler = new InputHandler();
    public void addNotify()
    {
        super.addNotify();
        if (getParent() instanceof JViewport) {
            getParent().addMouseListener(inputHandler);
            getParent().addMouseMotionListener(inputHandler);
        } else {
            this.addMouseListener(inputHandler);
            this.addMouseMotionListener(inputHandler);
        }
        requestFocus();
    }

    // todo: temporary till break processTransferable out of MapDropTarget
    // (for paste action)
    private MapDropTarget mapDropTarget;
    MapDropTarget getMapDropTarget()
    {
        return mapDropTarget;
    }

    public MapViewer(LWMap map)
    {
        //super(false); // turn off double buffering -- frame seems handle it?
        setOpaque(true);
        
        setLayout(null);
        //setLayout(new NoLayout());
        //setLayout(new FlowLayout());
        //addMouseListener(ih);
        //addMouseMotionListener(ih);
        addKeyListener(inputHandler);
        addFocusListener(this);
         
        //MapDropTarget mapDropTarget = new MapDropTarget(this);// new CanvasDropHandler
        this.mapDropTarget = new MapDropTarget(this);// new CanvasDropHandler
        this.setDropTarget(new java.awt.dnd.DropTarget(this, mapDropTarget));

        // todo: tab to show/hide all tool windows

        //setPreferredSize(new Dimension(cw,ch));
        //setSize(new Dimension(cw,ch));
        
        setPreferredSize(mapToScreenDim(map.getBounds()));

        //for (int i = 0; i < resizeHandles.length; i++) {
        //resizeHandles[i] = new LWSelection.ControlPoint(COLOR_SELECTION_HANDLE);
        //}
        
        //-------------------------------------------------------
        // set the background color here on the panel instead
        // of querying the map BG color every time in paintComponent
        // because the MapPanner, for instance, wants to use
        // it's own background color setting (hmm: should we let it?)
        //-------------------------------------------------------
        setBackground(map.getFillColor()); // todo: will need to listen for fill color changes
        
        //setFont(VueConstants.DefaultFont);
        loadMap(map);

        // we repaint any time the global selection changes
        VUE.ModelSelection.addListener(this);

        // draggedSelectionGroup is always a selected component as
        // it's only used when it IS the selection
        // There was some reason we need to have the set -- what was it?
        draggedSelectionGroup.setSelected(true);

        /*
        Toolkit.getDefaultToolkit().addAWTEventListener(this,
                                                        AWTEvent.INPUT_METHOD_EVENT_MASK
                                                        | AWTEvent.TEXT_EVENT_MASK
                                                        | AWTEvent.MOUSE_EVENT_MASK);
        */
    	
        // TODO: need to remove us as listener for this & VUE selection
        // if this map is closed.
    	// listen to tool selection events
    	VueToolbarController.getController().addToolSelectionListener( this);
        
        /*By Daisuke*/
      
        //tree = new OutlineViewTree(map);
        //scrollPane = new JScrollPane(tree);
       // scrollPane.setVisible(false);
        
        //setLayout(new BorderLayout());
        //add(scrollPane, BorderLayout.CENTER);
        
        /*End*/

        //-------------------------------------------------------
        // If this map was just restored, there might
        // have been an existing userZoom or userOrigin
        // set -- we honor that last user configuration here.
        //-------------------------------------------------------
        setZoomFactor(getMap().getUserZoom());
        Point2D p = getMap().getUserOrigin();
        setMapOriginOffset(p.getX(), p.getY());
    }
    
    
    /** The currently selected tool **/
    private VueTool activeTool = ArrowTool;

    private static final VueTool ArrowTool = VueToolbarController.getController().getTool("arrowTool");
    private static final VueTool HandTool = VueToolbarController.getController().getTool("handTool");
    private static final VueTool ZoomTool = VueToolbarController.getController().getTool("zoomTool");
    private static final VueTool NodeTool = VueToolbarController.getController().getTool("nodeTool");
    private static final VueTool LinkTool = VueToolbarController.getController().getTool("linkTool");
    private static final VueTool TextTool = VueToolbarController.getController().getTool("textTool");
    private static final VueTool PathwayTool = VueToolbarController.getController().getTool("textTool");
    
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

    public void toolSelected(VueTool pTool)
    {
        System.out.println("tool selected in map viewer:" + pTool.getID());
        
        if (pTool == null) {
            System.err.println("*** MapViewer.toolSelected: NULL TOOL");
            return;
        }
        if (pTool.getID() == null) {
            System.err.println("*** MapViewer.toolSelected: NULL ID IN TOOL!");
            return;
        }
       
       // if (pTool.getID().equals("outlineTool"))
        //{
         //  System.out.println("switching display mode");
         //  setMode(!outlineMode);
            
           //maybe set the cursor to something else or no return
           //return;
         //  System.out.println("came back from the setmode function");
        //}
       
        activeTool = pTool;
    	//System.out.println("MapViewer.toolSelected: " + pTool.getID());
            
        setMapCursor(activeTool.getCursor());

        if (isDraggingSelectorBox) // in case we change tool via kbd shortcut in the middle of a drag
            repaint();
    }

    private void setMapCursor(Cursor cursor)
    {
        //SwingUtilities.getRootPane(this).setCursor(cursor);
        // could compute cursor-set pane in addNotify
        setCursor(cursor);
        // todo: also set this on the VueToolPanel so you can see cursor change
        // when selecting new tool -- actually, VueToolPanel should
        // do this itself as we're going to put the cursors right in
        // the tool

    }

    /**
     * Set's the viewport such that the upper left corner
     * displays at screenX, screenY.  The precision
     * is due to possibility of zooming.
     */
    public void setMapOriginOffset(float screenX, float screenY)
    {
        this.mapOriginX = screenX;
        this.mapOriginY = screenY;
        getMap().setUserOrigin(screenX, screenY);
        new MapViewerEvent(this, MapViewerEvent.PAN).raise();
        repaint();
    }
    public void setMapOriginOffset(double screenX, double screenY) {
        setMapOriginOffset((float) screenX, (float) screenY);
    }
    public Point2D.Float getOriginLocation() {
        return new Point2D.Float(mapOriginX, mapOriginY);
    }
    public float getOriginX() {
        return mapOriginX;
    }
    public float getOriginY() {
        return mapOriginY;
    }
    float screenToMapX(float x) {
        return (float) ((x + getOriginX()) / zoomFactor);
    }
    float screenToMapY(float y) {
        return (float) ((y + getOriginY()) / zoomFactor);
    }
    float screenToMapX(int x) {
        return (float) ((x + getOriginX()) / zoomFactor);
    }
    float screenToMapY(int y) {
        return (float) ((y + getOriginY()) / zoomFactor);
    }
    float screenToMapDim(int dim) {
        return (float) (dim / zoomFactor);
    }
    Point2D.Float screenToMapPoint(Point p) {
        return screenToMapPoint(p.x, p.y);
    }
    Point2D.Float screenToMapPoint(int x, int y) {
        return new Point2D.Float(screenToMapX(x), screenToMapY(y));
    }
    int mapToScreenX(double x) {
        return (int) (0.5 + ((x * zoomFactor) - getOriginX()));
    }
    int mapToScreenY(double y) {
        return (int) (0.5 + ((y * zoomFactor) - getOriginY()));
    }
    Point mapToScreenPoint(Point2D p) {
        return new Point(mapToScreenX(p.getX()), mapToScreenY(p.getY()));
    }
    int mapToScreenDim(double dim)
    {
        if (dim > 0)
            return (int) (0.5 + (dim * zoomFactor));
        else
            return (int) (0.5 + (-dim * zoomFactor));
    }
    Rectangle mapToScreenRect(Rectangle2D mapRect)
    {
        //if (mapRect.getWidth() < 0 || mapRect.getHeight() < 0)
        //    throw new IllegalArgumentException("mapDim<0");
        Rectangle screenRect = new Rectangle();
        // Make sure we round out to the largest possible pixel rectangle
        // that contains all map coordinates
        screenRect.x = (int) Math.floor(mapRect.getX() * zoomFactor - getOriginX());
        screenRect.y = (int) Math.floor(mapRect.getY() * zoomFactor - getOriginY());
        screenRect.width = (int) Math.ceil(mapRect.getWidth() * zoomFactor);
        screenRect.height = (int) Math.ceil(mapRect.getHeight() * zoomFactor);
        //screenRect.x = (int) Math.round(mapRect.getX() * zoomFactor - getOriginX());
        //screenRect.y = (int) Math.round(mapRect.getY() * zoomFactor - getOriginY());
        //screenRect.width = (int) Math.round(mapRect.getWidth() * zoomFactor);
        //screenRect.height = (int) Math.round(mapRect.getHeight() * zoomFactor);
        return screenRect;
    }

    Dimension mapToScreenDim(Rectangle2D mapRect)
    {
        Rectangle screenRect = mapToScreenRect(mapRect);
        return new Dimension(screenRect.width, screenRect.height);
    }
    
    Rectangle2D.Float mapToScreenRect2D(Rectangle2D mapRect)
    {
        if (mapRect.getWidth() < 0 || mapRect.getHeight() < 0)
            throw new IllegalArgumentException("mapDim<0");
        Rectangle2D.Float screenRect = new Rectangle2D.Float();
        screenRect.x = (float) (mapRect.getX() * zoomFactor - getOriginX());
        screenRect.y = (float) (mapRect.getY() * zoomFactor - getOriginY());
        screenRect.width = (float) (mapRect.getWidth() * zoomFactor);
        screenRect.height = (float) (mapRect.getHeight() * zoomFactor);
        return screenRect;
    }
    Rectangle2D screenToMapRect(Rectangle screenRect)
    {
        if (screenRect.width < 0 || screenRect.height < 0)
            throw new IllegalArgumentException("screenDim<0 " + screenRect);
        Rectangle2D mapRect = new Rectangle2D.Float();
        mapRect.setRect(screenToMapX(screenRect.x),
                        screenToMapY(screenRect.y),
                        screenToMapDim(screenRect.width),
                        screenToMapDim(screenRect.height));
        return mapRect;
    }
    
    public Rectangle2D getVisibleMapBounds()
    {
        return screenToMapRect(new Rectangle(0,0, getWidth(), getHeight()));
    }
    

    private int lastMouseX;
    private int lastMouseY;
    private int lastMousePressX;
    private int lastMousePressY;
    private void setLastMousePressPoint(int x, int y)
    {
        lastMousePressX = x;
        lastMousePressY = y;
        setLastMousePoint(x,y);
    }
    /** last place mouse pressed */
    Point getLastMousePressPoint()
    {
        return new Point(lastMousePressX, lastMousePressY);
    }
    private void setLastMousePoint(int x, int y)
    {
        lastMouseX = x;
        lastMouseY = y;
    }
    /** last place mouse was either pressed or released */
    Point getLastMousePoint()
    {
        return new Point(lastMouseX, lastMouseY);
    }

    /* to be called by zoom code */
    void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        getMap().setUserZoom(zoomFactor);
        repaint();
        new MapViewerEvent(this, MapViewerEvent.ZOOM).raise();
    }
                    
    public double getZoomFactor()
    {
        return this.zoomFactor;
    }
    
    //private Point mLastCorner;
    public void reshape(int x, int y, int w, int h)
    {
        if (DEBUG_PAINT) System.out.println(this + " reshape " + x + "," + y + " " + w + "x" + h);
        /*
        Point p=null;
        if (isShowing()) {
            p = getLocationOnScreen();
            if (mLastCorner != null && !p.equals(mLastCorner)) {
                int dx = mLastCorner.x - p.x;
                int dy = mLastCorner.y - p.y;
                setMapOriginOffset(this.mapOriginX - dx,
                                   this.mapOriginX - dy);
            }
        }
        */
        //System.out.println(" ul start: "+p);
        super.reshape(x,y, w,h);
        //if (p!=null) p = getLocationOnScreen();
        //System.out.println("ul finish: "+p);

        //if (isShowing()) mLastCorner = getLocationOnScreen();
        repaint(250);
        //requestFocus();
        new MapViewerEvent(this, MapViewerEvent.PAN).raise();
        // may be causing problems on mac --
        // some components in tabbed is getting a reshape call
        // when switching tabs
    }

    LWMap getMap()
    {
        return this.map;
    }
    
    private void unloadMap()
    {
        this.map.removeLWCListener(this);
        this.map = null;
    }
    
    private void loadMap(LWMap map)

    {
        if (map == null)
            throw new IllegalArgumentException("loadMap: null LWMap");
        if (this.map != null)
            unloadMap();
        this.map = map;
        this.map.addLWCListener(this);
        repaint();
    }
 
    private void RR(Rectangle r)
    {
        if (OPTIMIZED_REPAINT)
            super.repaint(0,r.x,r.y,r.width,r.height);
        else
            super.repaint();
    }
    
    private Rectangle mapRectToPaintRegion(Rectangle2D mapRect)
    {
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
    private void repaintMapRegion(Rectangle2D mapRect)
    {
        if (OPTIMIZED_REPAINT) {
            paintingRegion = true;
            repaint(mapRectToPaintRegion(mapRect));
        }
        else
            repaint();
    }
    
    private void repaintMapRegionGrown(Rectangle2D mapRect, float growth)
    {
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
        outside the edges of what's selected */
    private void repaintMapRegionAdjusted(Rectangle2D mapRect)
    {
        if (OPTIMIZED_REPAINT)
            repaint(growForSelection(mapToScreenRect(mapRect)));
        else
            repaint();
    }

    // We grow the bounds here to include any selection
    // handles that may be rendering as we drag the component
    // todo: do we have to do StrokeBug05 compensation here?
    private Rectangle growForSelection(Rectangle r)
    {
        // adding 2 to SHS at moment due to Mac bugs
        int adjust = SelectionHandleSize + 2;
        int margin = adjust / 2;
        r.x -= margin;
        r.y -= margin;
        r.width += adjust + 1;
        r.height += adjust + 1;
        return r;
    }
    
    public void selectionChanged(LWSelection s)
    {
        //System.out.println("MapViewer: selectionChanged");
        if (VUE.getActiveMap() != this.map)
            VueSelection = null; // insurance: nothing should be happening here if we're not active
        else {
            if (VueSelection != VUE.ModelSelection) {
                if (DEBUG_FOCUS) System.out.println("*** Pointing to selection: " + this);
                VueSelection = VUE.ModelSelection;
            }
        }
        repaintSelection();
    }
    
    /** update the regions of both the old selection & the new selection */
    public void repaintSelection()
    {
        if (paintedSelectionBounds != null)
            RR(paintedSelectionBounds);
        if (VueSelection != null) {
            Rectangle2D newBounds = VueSelection.getBounds();
            if (newBounds != null)
                repaintMapRegionAdjusted(newBounds);
        }
    }
    
    
    /**
     * Handle events coming off the LWMap we're displaying.
     */
    public void LWCChanged(LWCEvent e)
    {
        // todo: optimize -- we get tons of location events
        // when dragging, esp if there are children if
        // we have those events turned in...
        //System.out.println("MapViewer: " + e);
        if (e.getWhat().equals("location")
            || e.getWhat().equals("added") // depend on childAdded 
            )
            // || e.getWhat.equals("childRemoved"))
            // todo: deleting even will set up for repainting that node,
            // but the childRemoved event's component object is the whole map --
            // thus we'll repaint everything on every delete (or childAdded)
            return;
        if (e.getWhat().startsWith("child")) {
            // childAdded would clip if added outside edge
            // of any existing components!
            repaint();
            return;
        }
        if (e.getWhat().equals("deleting")) {
            // todo: better for the selection itself listen for this,
            //   but that's a ton of events for this one thing.
            // todo: maybe have LWContainer check isSelected & manage it in deleteChild?
            // todo: also, can we coalesce this for big group deletions?
            LWComponent c = e.getComponent();
            boolean wasSelected = c.isSelected();

            if (wasSelected) {
                selectionRemove(c); // ensure isn't in selection
                
                // if we just dispersed a group that was selected,
                // put all the former children into the selection instead
                if (c instanceof LWGroup)
                    selectionAdd(((LWGroup)c).getChildIterator());
            }

            if (rollover == c)
                clearRollover();
        }
        if (e.getSource() == this)//todo: still relevant?
            return;
        if (paintedSelectionBounds != null) {
            // this will handle any size shrinkages -- old selection bounds
            // will still include the old size (this depends on fact that
            // we can only change the properties of a selected component)
            RR(paintedSelectionBounds);
        }
        repaintMapRegionAdjusted(e.getComponent().getBounds());
    }

    /**
     * [TODO: changed] By default, add all nodes hit by this box to a list for doing selection.
     * If NO nodes are in the list, search for links within the region
     * instead.  Of @param onlyLinks is true, only search for links.
     */
    private java.util.List computeSelection(Rectangle2D mapRect, boolean onlyLinks)
    {
        java.util.List hits = new java.util.ArrayList();
        java.util.Iterator i = getMap().getChildIterator();
        // todo: if want nested children to get seleced, will need a descending iterator

        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
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
    
    private java.util.List computeSelection(Rectangle2D mapRect, Class selectionType)
    {
        java.util.List hits = new java.util.ArrayList();
        java.util.Iterator i = getMap().getChildIterator();
        // todo: if want nested children to get seleced, will need a descending iterator

        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (selectionType != null && !selectionType.isInstance(c))
                continue;
            if (c.intersects(mapRect))
                hits.add(c);
        }
        return hits;
    }
    
        
    public LWComponent findClosestEdge(java.util.List hits, float x, float y)
    {
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
    protected LWComponent findClosest(java.util.List hits, float x, float y, boolean toEdge)
    {
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

    
    /** 
     * Iterate over all components and return a bounding box
     * for the whole set.  This can't be a ConceptMap method
     * because we don't actually know the component sizes
     * until they're rendered (e.g., font metrics taken into
     * account, etc). todo: have only in LWMap
     */
    public Rectangle2D getAllComponentBounds()
    {
        return LWMap.getBounds(getMap().getChildIterator());
    }
    
    void setIndicated(LWComponent c)
    {
        if (indication != c) {
            clearIndicated();
            indication = c;
            c.setIndicated(true);
            if (indication.getStrokeWidth() < STROKE_INDICATION.getLineWidth())
                repaintMapRegionGrown(indication.getBounds(), STROKE_INDICATION.getLineWidth());
            else
                repaintMapRegion(indication.getBounds());
        }
    }
    void clearIndicated()
    {
        if (indication != null) {
            indication.setIndicated(false);
            if (indication.getStrokeWidth() < STROKE_INDICATION.getLineWidth())
                repaintMapRegionGrown(indication.getBounds(), STROKE_INDICATION.getLineWidth());
            else
                repaintMapRegion(indication.getBounds());
            indication = null;
        }
    }
    LWComponent getIndication() { return indication; }
    
    private Timer rolloverTimer = new Timer();
    private TimerTask rolloverTask = null;
    class RolloverTask extends TimerTask
    {
        public void run()
        {
            //System.out.println("task run " + this);
            float mapX = screenToMapX(lastMouseX);
            float mapY = screenToMapY(lastMouseY);
            // use deepest to penetrate into groups
            LWComponent hit = getMap().findDeepestChildAt(mapX, mapY);
            //LWComponent hit = getMap().findChildAt(mapX, mapY);
            if (DEBUG_ROLLOVER) System.out.println("RolloverTask: hit=" + hit);
            //if (hit != null && VueSelection.size() <= 1)
            if (hit != null)
                setRollover(hit);
            else
                clearRollover();

            rolloverTask = null;
        }
    }
        
    private float mZoomoverOldScale;
    private Point2D mZoomoverOldLoc = null;
    void setRollover(LWComponent c)
    {
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
                if (DEBUG_ROLLOVER) System.out.println("setRollover: " + c);
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
    void clearRollover()
    {
        if (rollover != null) {
            if (DEBUG_ROLLOVER) System.out.println("clrRollover: " + rollover);
            if (rolloverTask != null) {
                rolloverTask.cancel();
                rolloverTask = null;
            }
            Rectangle2D bigBounds = rollover.getBounds();
            rollover.setRollover(false);
            rollover.setZoomedFocus(false);
            if (true||mZoomoverOldScale != 1f) {
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
    void setTip(JComponent pJComponent, Rectangle2D pAvoidRegion, Rectangle2D pTipRegion)
    {
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
            Rectangle viewer = new Rectangle(0,0, getWidth(), getHeight());
            Box avoid = new Box(viewer.intersection(mapToScreenRect(pAvoidRegion)));
            Box rollover = new Box(mapToScreenRect(pTipRegion));
            
            SwingUtilities.convertPointToScreen(avoid.ul, this);
            SwingUtilities.convertPointToScreen(avoid.lr, this);
            SwingUtilities.convertPointToScreen(rollover.ul, this);
            //SwingUtilities.convertPointToScreen(rollover.lr, this); // unused

            Dimension tip = pJComponent.getPreferredSize();

            // Default placement starts from left of component,
            // at same height as the rollover region that triggered us
            // in the component.
            Point glass = new Point(avoid.ul.x - tip.width,  rollover.ul.y);

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
                    glass.y = rollover.ul.y;
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

    private void keepTipOnScreen(Point glass, Dimension tip)
    {
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
    
    void clearTip()
    {
        sTipComponent = null;
        if (sTipWindow != null) {
            sTipWindow.hide();
            sTipWindow = null;
        }
    }

    
    /**
     * Render all the LWComponents on the panel
     */
    // java bug: Do NOT create try and create an axis using Integer.{MIN,MAX}_VALUE
    // -- this triggers line rendering bugs in PC Java 1.4.1 (W2K)
    private static final Line2D Xaxis = new Line2D.Float(-3000, 0, 3000, 0);
    private static final Line2D Yaxis = new Line2D.Float(0, -3000, 0, 3000);

    //public boolean isOpaque() {return false;}

    private int paints=0;
    private boolean redrawingSelector = false;
    public void paint(Graphics g)
    {
        long start = 0;
        if (DEBUG_PAINT) {
            System.out.print("paint " + paints + " " + g.getClipBounds()+" "); System.out.flush();
            start = System.currentTimeMillis();
        }
        try {
            // This a special speed optimization for the selector box -- NO LONGER VIABLE
            // Try using a glass pane for this.
            /*
            if (redrawingSelector && draggedSelectorBox != null && activeTool.supportsXORSelectorDrawing()) {
                redrawSelectorBox((Graphics2D)g);
                redrawingSelector = false;
                } else*/
                super.paint(g);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("*paint* Exception painting in: " + this);
            System.err.println("*paint* VueSelection: " + VueSelection);
            System.err.println("*paint* Graphics: " + g);
            System.err.println("*paint* Graphics transform: " + ((Graphics2D)g).getTransform());
        }
        if (paints == 0 && getParent() instanceof JViewport) {
            setPreferredSize(mapToScreenDim(getMap().getBounds()));
            validate();
        }
        if (DEBUG_PAINT) {
            long delta = System.currentTimeMillis() - start;
            long fps = delta > 0 ? 1000/delta : -1;
            System.out.println("paint " + paints + " " + this + ": "
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
        Graphics2D g2 = (Graphics2D) g;
        
        Rectangle cb = g.getClipBounds();
        //if (DEBUG_PAINT && !OPTIMIZED_REPAINT && (cb.x>0 || cb.y>0))
        //System.out.println(this + " paintComponent: clipBounds " + cb);

        //-------------------------------------------------------
        // paint the background
        //-------------------------------------------------------
        
        g2.setColor(getBackground());
        g2.fill(cb);
        
        //-------------------------------------------------------
        // paint the focus border if needed (todo: change to some extra-pane method)
        //-------------------------------------------------------
        
        if (VUE.multipleMapsVisible() && VUE.getActiveViewer() == this && hasFocus()) {
            g.setColor(COLOR_ACTIVE_VIEWER);
            g.drawRect(0, 0, getWidth()-1, getHeight()-1);
            g.drawRect(1, 1, getWidth()-3, getHeight()-3);
        }
        
        if (OPTIMIZED_REPAINT) {
            // debug: shows the repaint region
            if (DEBUG_PAINT && (RepaintRegion != null || paintingRegion)) {
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
        
        //-------------------------------------------------------
        // adjust GC for pan & zoom
        //-------------------------------------------------------
        
        g2.translate(-getOriginX(), -getOriginY());
        if (zoomFactor != 1)
            g2.scale(zoomFactor, zoomFactor);
        
        if (DEBUG_SHOW_ORIGIN) {
            if (VueUtil.isMacPlatform()) {
                //g2.setStroke(new java.awt.BasicStroke(1.0001f)); // java bug: Mac 1.0 stroke width hardly scales...
                g2.setStroke(STROKE_TWO);
                // Using a stroke of 2 display's effect of VueUtil.StrokeBug05
            } else {
                g2.setStroke(STROKE_ONE);
            }
            g2.setColor(Color.lightGray);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }
        
        //-------------------------------------------------------
        // Draw the map: nodes, links, etc.
        // LWNode's & LWGroup's are responsible for painting
        // their children (as any instance of LWContainer).
        //-------------------------------------------------------

        DrawContext dc = new DrawContext(g2, getZoomFactor());
        
        dc.setAntiAlias(DEBUG_ANTI_ALIAS);
        dc.setPrioritizeQuality(DEBUG_RENDER_QUALITY);
        dc.setFractionalFontMetrics(DEBUG_FONT_METRICS);

        dc.disableAntiAlias(DEBUG_ANTI_ALIAS == false);

        // anti-alias text
        //if (!DEBUG_ANTIALIAS_OFF) g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Do we need fractional metrics?  Gives us slightly more accurate
        // string widths on noticable on long strings
        /*
        if (!DEBUG_ANTIALIAS_OFF) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        } else {
            // was hoping prioritizing render quality would improve computation of font string widths,
            // but no such luck...
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        if (DEBUG_FONT_METRICS)
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        else
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        */
        
        //-------------------------------------------------------
        // Draw the map: Ask the model to render itself to our GC
        //-------------------------------------------------------

        this.map.draw(dc);

        //-------------------------------------------------------
        // If current tool has anything it wants to draw, it
        // can do that here.
        //-------------------------------------------------------
        activeTool.handlePaint(dc);

        /*
        if (dragComponent != null) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            dragComponent.draw(g2);
        }
        */

        //if (draggingChild) {
        //    dragComponent.setDispalyed(true);
        //}
        
        //-------------------------------------------------------
        // Restore us to raw screen coordinates & turn off
        // anti-aliasing to draw selection indicators
        //-------------------------------------------------------
        
        if (zoomFactor != 1)
            g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
        g2.translate(getOriginX(), getOriginY());

        if (true||!VueUtil.isMacPlatform()) // try aa selection on mac for now (todo)
            //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);
            dc.setAntiAlias(true);

        //-------------------------------------------------------
        // draw selection if there is one
        //-------------------------------------------------------
        
        if (VueSelection != null && !VueSelection.isEmpty())
            drawSelection(dc);
        else
            resizeControl.active = false;

        //-------------------------------------------------------
        // draw the dragged selector box
        // Note: mac uses XOR method to update selector -- we'll
        // never hit this code -- see paint(Graphics).
        //-------------------------------------------------------
        
        if (draggedSelectorBox != null) {
            drawSelectorBox(g2, draggedSelectorBox);
            //if (VueSelection != null && !VueSelection.isEmpty())
            //    new Throwable("selection box while selection visible").printStackTrace();
            // totally reasonable if doing a shift-drag for SELECTION TOGGLE
        }
        
        if (DEBUG_SHOW_MOUSE_LOCATION) {
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(0.01f));
            g2.drawLine(mouse.x,mouse.y, mouse.x,mouse.y);

            int iX = (int) (screenToMapX(mouse.x) * 100);
            int iY = (int) (screenToMapY(mouse.y) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            g2.setFont(VueConstants.MediumFont);
            int y = 0;
            //g2.drawString("screen(" + mouse.x + "," +  mouse.y + ")", 10, y+=15);
            g2.drawString("screen " + mouse, 10, y+=15);
            g2.drawString("mapX " + mapX, 10, y+=15);
            g2.drawString("mapY " + mapY, 10, y+=15);;
            g2.drawString("zoom " + getZoomFactor(), 10, y+=15);
            g2.drawString("anitAlias " + DEBUG_ANTI_ALIAS, 10, y+=15);
            g2.drawString("renderQuality " + DEBUG_RENDER_QUALITY, 10, y+=15);
            g2.drawString("fractionalMetrics " + DEBUG_FONT_METRICS, 10, y+=15);
            g2.drawString("findParent " + !DEBUG_FINDPARENT_OFF, 10, y+=15);
            g2.drawString("optimizedRepaint " + OPTIMIZED_REPAINT, 10, y+=15);
        }

        if (DEBUG_SHOW_ORIGIN && zoomFactor >= 6.0) {
            //g2.setComposite(java.awt.AlphaComposite.Xor);
            g2.translate(-getOriginX(), -getOriginY());
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(Color.black);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }

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
    public void remove(Component c)
    {
        super.remove(c);
        if (c == activeTextEdit) {
            activeTextEdit = null;
            try {
                repaint();
                requestFocus();
            } finally {
                Actions.setAllIgnored(false);
            }
        }
    }

    void cancelLabelEdit()
    {
        if (activeTextEdit != null)
            remove(activeTextEdit);
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
    
    void activateLabelEdit(LWComponent lwc)
    {
        if (activeTextEdit != null && activeTextEdit.getLWC() == lwc)
            return;
        if (activeTextEdit != null)
            remove(activeTextEdit);
        Actions.setAllIgnored(true);
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

    private void drawSelectorBox(Graphics2D g2, Rectangle r)
    {
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
        //if (DEBUG_PAINT) System.out.println(g2);
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

    private void redrawSelectorBox(Graphics2D g2)
    {
        //throw new UnsupportedOperationException("XOR redraw no longer supported");
        //if (DEBUG_PAINT) System.out.println(g2);
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
            
    // todo: move all this code to LWSelection?
    private void drawSelection(DrawContext dc)
    {
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
        g2.translate(-getOriginX(), -getOriginY());
        if (zoomFactor != 1) g2.scale(zoomFactor, zoomFactor);
        it = VueSelection.iterator();
        g2.setStroke(new BasicStroke((float) (STROKE_HALF.getLineWidth() / zoomFactor)));
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if (inDrag || c.getStrokeWidth() == 0) {
                //g2.setColor(c.getStrokeColor());
                g2.draw(c.getShape());
            }
        }
        g2.setStroke(new BasicStroke((float) (STROKE_SELECTION.getLineWidth() / zoomFactor)));
        if (indication != null) {
            DrawContext dc2 = dc;//dc.create();
            dc2.g.setColor(COLOR_INDICATION);
            dc2.g.draw(indication.getShape());
            dc2.g.setColor(COLOR_SELECTION);
            // really, only the dragComponent should be transparent...
            //dc2.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            //indication.draw(dc2);
            //g2.setComposite(AlphaComposite.Src);
        }
        
        if (zoomFactor != 1) g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
        g2.translate(getOriginX(), getOriginY());
        g2.setStroke(STROKE_SELECTION);
        //g2.setComposite(AlphaComposite.Src);
        g2.setColor(COLOR_SELECTION);
            
        //if (!VueSelection.isEmpty() && (!inDrag || isDraggingSelectorBox)) {

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

        if (VueSelection.countTypes(LWNode.class) <= 0) {
            // todo: also alow groups to resize (make selected group resize
            // re-usable for a group -- perhaps move to LWGroup itself &
            // also use draggedSelectionGroup for this?)
            if (DEBUG_BOXES || VueSelection.size() > 1 || !VueSelection.allOfType(LWLink.class))
                g2.draw(mapSelectionBounds);
            // no resize handles if only links or groups
            resizeControl.active = false;
        } else {
            if (VueSelection.size() > 1) {
                g2.draw(mapSelectionBounds);
            } else {
                // Only one in selection:
                // Special case to keep control handles out of way of node icons
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
            //if (!inDrag)
            //drawSelectionBoxHandles(g2, mapSelectionBounds);

            setSelectionBoxResizeHandles(mapSelectionBounds);
            resizeControl.active = true;
            for (int i = 0; i < resizeControl.handles.length; i++) {
                LWSelection.ControlPoint cp = resizeControl.handles[i];
                drawSelectionHandleCentered(g2, cp.x, cp.y, cp.getColor());
            }
        }

        //if (inDrag) return;
        
        //-------------------------------------------------------
        // draw LWComponent requested control points
        //-------------------------------------------------------
        
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

        if (DEBUG_SHOW_MOUSE_LOCATION && resizeControl.draggedBounds != null) {
            dc.g.setColor(Color.red);
            dc.g.draw(mapToScreenRect(resizeControl.draggedBounds));
        }

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

    // exterior drawn box will be 1 pixel bigger
    static final int SelectionHandleSize = VueResources.getInt("mapViewer.selection.handleSize"); // fill size
    static final int CHS = VueResources.getInt("mapViewer.selection.componentHandleSize"); // fill size
    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final Rectangle2D ComponentHandle = new Rectangle2D.Float(0,0,0,0);

    private void drawSelectionHandleCentered(Graphics2D g, float x, float y, Color fillColor)
    {
        x -= SelectionHandleSize/2;
        y -= SelectionHandleSize/2;
        drawSelectionHandle(g, x, y, fillColor);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y)
    {
        drawSelectionHandle(g, x, y, COLOR_SELECTION_HANDLE);
    }
    private void drawSelectionHandle(Graphics2D g, float x, float y, Color fillColor)
    {
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
        g.setColor(COLOR_SELECTION);
        g.draw(SelectionHandle);
    }

    static final float sMinSelectEdge = SelectionHandleSize * 2;
    private void setSelectionBoxResizeHandles(Rectangle2D.Float r)
    {
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
    private void old_drawSelectionBoxHandles(Graphics2D g, Rectangle2D.Float r)
    {
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
    void drawComponentSelectionBox(Graphics2D g, LWComponent c)
    {
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

    protected void selectionAdd(LWComponent c)
    {
        VueSelection.add(c);
    }
    protected void selectionAdd(java.util.Iterator i)
    {
        VueSelection.add(i);
    }
    protected void selectionRemove(LWComponent c)
    {
        VueSelection.remove(c);
    }
    protected void selectionSet(LWComponent c)
    {
        VueSelection.setTo(c);
    }
    protected void selectionClear()
    {
        VueSelection.clear();
    }
    protected void selectionToggle(LWComponent c)
    {
        if (c.isSelected())
            selectionRemove(c);
        else
            selectionAdd(c);
    }
    protected void selectionToggle(java.util.Iterator i)
    {
        VueSelection.toggle(i);
    }
    
    private JPopupMenu mapPopup = null;
    private JPopupMenu groupPopup = null;
    private JPopupMenu componentPopup = null;
    private JMenu assetMenu = null;
    private JMenu linkMenu = null;
    private JMenu nodeMenu = null;

    private JMenu getLinkMenu()
    {
        if (linkMenu == null) {
            linkMenu = new JMenu("Link");
            for (int i = 0; i < Actions.LINK_MENU_ACTIONS.length; i++) {
                Action a = Actions.LINK_MENU_ACTIONS[i];
                if (a == null)
                    linkMenu.addSeparator();
                else
                    linkMenu.add(a);
            }
        }
        return linkMenu;
            
    }

    private JMenu getNodeMenu()
    {
        if (nodeMenu == null) {
            nodeMenu = new JMenu("Node");
            for (int i = 0; i < Actions.NODE_MENU_ACTIONS.length; i++) {
                Action a = Actions.NODE_MENU_ACTIONS[i];
                if (a == null)
                    nodeMenu.addSeparator();
                else
                    nodeMenu.add(a);
            }
        }
        return nodeMenu;
            
    }
    
    private JPopupMenu getMapPopup()
    {
        if (mapPopup == null) {
            mapPopup = new JPopupMenu("Map Menu");
            mapPopup.addSeparator();
            mapPopup.add(Actions.NewNode);
            mapPopup.add(Actions.NewText);
            mapPopup.addSeparator();
            mapPopup.add(Actions.ZoomFit);
            mapPopup.add(Actions.ZoomActual);
            mapPopup.addSeparator();
            mapPopup.add(Actions.SelectAll);
            //mapPopup.add("Visible");
            //mapPopup.setBackground(Color.gray);
        }
        return mapPopup;
    }

    private JPopupMenu buildGroupPopup()
    {
        JPopupMenu m = new JPopupMenu("Group Menu");

        m.add(Actions.Duplicate);
        m.addSeparator();
        m.add(Actions.Group);
        m.add(Actions.Ungroup);
        m.addSeparator();
        m.add(Actions.BringToFront);
        m.add(Actions.BringForward);
        m.add(Actions.SendToBack);
        m.add(Actions.SendBackward);
        m.addSeparator();
        for (int i = 0; i < Actions.ALIGN_MENU_ACTIONS.length; i++) {
            Action a = Actions.ALIGN_MENU_ACTIONS[i];
            if (a == null)
                m.addSeparator();
            else
                m.add(a);
        }
        m.addSeparator();
        m.add(Actions.Delete);
        return m;
    }
    
    private JPopupMenu buildComponentPopup()
    {
        JPopupMenu m = new JPopupMenu("Item Menu");

        m.add(Actions.Rename);
        m.add(Actions.Duplicate);
        m.addSeparator();
        m.add(Actions.BringToFront);
        m.add(Actions.BringForward);
        m.add(Actions.SendToBack);
        m.add(Actions.SendBackward);
        m.addSeparator();
        m.add(Actions.DeselectAll);
        m.addSeparator();
        m.add(Actions.Delete);
        m.addSeparator();
        m.add(Actions.AddPathwayNode);
        m.add(Actions.DeletePathwayNode);
        m.addSeparator();
        m.add(Actions.HierarchyView);
        
        //end of addition
        
        // todo: special add-to selection action that adds
        // hitComponent to selection so have way other
        // than shift-click to add to selection (so you
        // can do it all with the mouse)

        return m;
    }
    
    private JPopupMenu getComponentPopup(LWComponent c)
    {
        if (c == null)
            c = VueSelection.first(); // should be only thing in selection

        if (componentPopup == null)
            componentPopup = buildComponentPopup();
        
        if (c instanceof LWNode) {
            //componentPopup.add(getNodeMenu());
            //componentPopup.add(Actions.NodeMakeAutoSized);
            LWNode n = (LWNode) c;
            Resource r = n.getResource();
            Asset a = r == null ? null : r.getAsset();  
            if(a != null && assetMenu == null) {
               assetMenu = getAssetMenu(a);
               componentPopup.add(assetMenu);
            }else if(a != null) {
                componentPopup.remove(assetMenu);
                assetMenu = getAssetMenu(a);
                componentPopup.add(assetMenu);
            }else if(a == null && assetMenu != null) {
                componentPopup.remove(assetMenu);
            }
            
        }
        /*
          // TODO: if JUST a group is selected, need to add Ungroup
          // to the menu
        else if (c instanceof LWGroup) {
            componentPopup.add(Actions.Ungroup);
        } else {
            componentPopup.remove(Actions.Ungroup);
        }
        */


        if (c instanceof LWLink)
            componentPopup.add(getLinkMenu());
        else
            componentPopup.remove(getLinkMenu());

        if (c instanceof LWNode)
            componentPopup.add(getNodeMenu());
        else
            componentPopup.remove(getNodeMenu());

        return componentPopup;
    }
    
    private JPopupMenu getSelectionPopup(LWComponent c)
    {
        if (groupPopup == null)
            groupPopup = buildGroupPopup();

        if (VueSelection.allOfType(LWLink.class))
            groupPopup.add(getLinkMenu());
        else
            groupPopup.remove(getLinkMenu());

        return groupPopup;

        /*
        if (VueSelection.size() == 1 && !(VueSelection.first() instanceof LWGroup))
            return componentPopup;
        else
            return groupPopup;
        */
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
    // Mac overrides CONTROL-MOUSE to look like right-click (context menu popup) so we can't
    // use CTRL wih mouse drag -- todo: change to ALT for PC too -- might as well be consistent.
    static final int KEY_ABORT_ACTION = KeyEvent.VK_ESCAPE;
        

    private void revertTemporaryTool()
    {
        if (toolKeyDown != 0) {
            toolKeyDown = 0;
            //toolKeyEvent = null;
            toolSelected(toolKeyOldTool); // restore prior cursor
            toolKeyOldTool = null;
        }
    }

    
    class InputHandler extends tufts.vue.MouseAdapter
        implements java.awt.event.KeyListener
    {
        LWComponent dragComponent;//todo: RENAME dragGroup -- make a ControlListener??
        LWSelection.ControlListener dragControl;
        //boolean isDraggingControlHandle = false;
        int dragControlIndex;
        boolean mouseWasDragged = false;
        LWComponent justSelected;    // for between mouse press & click
        boolean hitOnSelectionHandle = false; // we moused-down on a selection handle 

        /**
         * dragStart: screen location (within this java.awt.Container)
         * of mouse-down that started this drag. */
        Point dragStart = new Point();

        /**
         * dragOffset: absolute map distance mouse was from the
         * origin of the current dragComponent when the mouse was
         * pressed down. */
        Point2D.Float dragOffset = new Point2D.Float();
        
        public void keyPressed(KeyEvent e)
        {
            if (DEBUG_KEYS) System.out.println("[" + e.paramString() + "]");

            clearTip();

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
            
            if (key == KeyEvent.VK_DELETE) {
                // todo: can't we add this to a keymap for the MapViewer JComponent?
                Actions.Delete.actionPerformed(new ActionEvent(this, 0, "Delete-via-viewer-key"));
                return;
            }
            
            if (key == KEY_ABORT_ACTION) {
                if (dragComponent != null) {
                    double oldX = screenToMapX(dragStart.x) + dragOffset.x;
                    double oldY = screenToMapY(dragStart.y) + dragOffset.y;
                    dragComponent.setLocation(oldX, oldY);
                    //dragPosition.setLocation(oldX, oldY);
                    dragComponent = null;
                    activeTool.handleDragAbort();
                    mouseWasDragged = false;
                    clearIndicated(); // incase dragging new link
                    // TODO: dragControl not abortable...
                    repaint();
                    return;
                }
                if (draggedSelectorBox != null) {
                    // cancel any drags
                    draggedSelectorBox = null;
                    isDraggingSelectorBox = false;
                    repaint();
                    return;
                }
            }

            /*if (VueUtil.isMacPlatform() && toolKeyDown == KEY_TOOL_PAN) {
                // toggle cause mac auto-repeats space-bar screwing everything up
                // todo: is this case only on my G4 kbd or does it happen on
                // USB kbd w/external screen also?
                toolKeyDown = 0;
                toolKeyEvent = null;
                setCursor(CURSOR_DEFAULT);
                return;
                }*/

            // If any modifier keys down, may be an action command.
            // Is actually okay if a mouse is down while we do this tho.
            if ((e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0 && (!inDrag || isDraggingSelectorBox)) {
                VueTool[] tools =  VueToolbarController.getController().getTools();
                for (int i = 0; i < tools.length; i++) {
                    VueTool tool = tools[i];
                    if (tool.getShortcutKey() == keyChar) {
                        VueToolbarController.getController().setSelectedTool(tool);
                        return;
                    }
                }
            }

            if (toolKeyDown == 0 && !isDraggingSelectorBox && !inDrag) {
                // todo: handle via resources
                VueTool tempTool = null;
                if      (key == KEY_TOOL_PAN) tempTool = HandTool;
                else if (key == KEY_TOOL_ZOOM) tempTool = ZoomTool;
                else if (key == KEY_TOOL_LINK) tempTool = LinkTool;
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

            // Now check for immediate action commands

            /*
            java.util.Iterator i = tools.iterator();
            while (i.hasNext()) {
                VueTool tool = (VueTool) i.next();
                if (tool.handleKeyPressed(e))
                    break;
            }
            */


            // BUTTON1_DOWN_MASK Doesn't appear to be getting set in mac Java 1.4!
            //if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            if (e.isShiftDown()) {
                // display debugging features
                char c = e.getKeyChar();
                boolean did = true;
                if (c == 'A') {
                    DEBUG_ANTI_ALIAS = !DEBUG_ANTI_ALIAS;
                    if (DEBUG_ANTI_ALIAS)
                         AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;
                    else AA_ON = RenderingHints.VALUE_ANTIALIAS_OFF;
                }
                else if (c == 'I') { DEBUG_SHOW_MOUSE_LOCATION = !DEBUG_SHOW_MOUSE_LOCATION; }
                else if (c == 'O') { DEBUG_SHOW_ORIGIN = !DEBUG_SHOW_ORIGIN; }
                else if (c == 'R') { OPTIMIZED_REPAINT = !OPTIMIZED_REPAINT; }
                //else if (c == 'F') { DEBUG_FINDPARENT_OFF = !DEBUG_FINDPARENT_OFF; }
                else if (c == 'F') { DEBUG_FOCUS = !DEBUG_FOCUS; }
                else if (c == 'P') { DEBUG_PAINT = !DEBUG_PAINT; }
                else if (c == 'K') { DEBUG_KEYS = !DEBUG_KEYS; }
                else if (c == 'M') { DEBUG_MOUSE = !DEBUG_MOUSE; }
                else if (c == 'T') { DEBUG_TIMER_ROLLOVER = !DEBUG_TIMER_ROLLOVER; }
                else if (c == 'Q') { DEBUG_RENDER_QUALITY = !DEBUG_RENDER_QUALITY; }
                else if (c == '|') { DEBUG_FONT_METRICS = !DEBUG_FONT_METRICS; }
                else
                    did = false;
                if (did) {
                    System.err.println("MapViewer diagnostic '" + c + "' toggled.");
                    repaint();
                }
            }
        }
        
        public void keyReleased(KeyEvent e)
        {
            if (DEBUG_KEYS) System.out.println("[" + e.paramString() + "]");

            if (toolKeyDown == e.getKeyCode()) {
                // Don't revert tmp tool if we're in the middle of a drag
                if (inDrag)
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
        private boolean checkAndHandleControlPointPress(MapMouseEvent e)
        {
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

        private boolean checkAndHandleControlListenerHits(LWSelection.ControlListener cl, MapMouseEvent e, boolean mapCoords)
        {
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
                        System.out.println("hit on control point " + i + " of controlListener " + cl);
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
        public void mousePressed(MouseEvent e)
        {
            if (DEBUG_MOUSE) System.out.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");
            
            mLabelEditWasActiveAtMousePress = (activeTextEdit != null);
            if (DEBUG_FOCUS) System.out.println("\tmouse-pressed active text edit="+mLabelEditWasActiveAtMousePress);
            // TODO: if we didn' HAVE focus, don't change the selection state --
            // only use the mouse click to gain focus.
            clearTip();
            grabVueApplicationFocus("mousePressed");
            requestFocus();

            dragStart.setLocation(e.getX(), e.getY());
            if (DEBUG_MOUSE) System.out.println("dragStart set to " + dragStart);
            
            if (activeTool == HandTool) {
                originAtDragStart = getOriginLocation();
                if (getParent() instanceof JViewport)
                    viewportAtDragStart = ((JViewport)getParent()).getViewPosition();
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
                hitComponent = getMap().findChildAt(mapX, mapY);
                if (DEBUG_MOUSE && hitComponent != null)
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
            if ((e.isPopupTrigger() || isRightClickEvent(e)) && !activeTool.usesRightClick())
            {
                if (hitComponent != null && !hitComponent.isSelected())
                    selectionSet(justSelected = hitComponent);                    
                    
                //-------------------------------------------------------
                // MOUSE: We've pressed the right button down, so pop
                // a context menu depending on what's in selection.
                //-------------------------------------------------------
                displayContextMenu(e, hitComponent);
            }
            else if (hitComponent != null)
            {
                // special case handling for KEY_TOOL_LINK which
                // doesn't want to be fully activated till the
                // key is down (ctrl) AND the left mouse has been
                // pressed over a component to drag a link off.
                if (toolKeyDown == KEY_TOOL_LINK)
                    toolSelected(LinkTool);
                
                //-------------------------------------------------------
                // MOUSE: We've pressed the left (normal) mouse on SOME LWComponent
                //-------------------------------------------------------
                
                activeTool.handleMousePressed(mme);
                
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
                    && VueSelection.contains(mapX, mapY)) {
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
                }
                isDraggingSelectorBox = true;
            }

            if (dragComponent != null)
                dragOffset.setLocation(dragComponent.getX() - mapX,
                                       dragComponent.getY() - mapY);

        }

        private void displayContextMenu(MouseEvent e, LWComponent hitComponent)
        {
            if (VueSelection.isEmpty()) {
                getMapPopup().show(e.getComponent(), e.getX(), e.getY());
            } else if (VueSelection.size() == 1) {

                //enables and disables the add/delete pathway nodes menu 
                //depending on whether there is a selected pathway and the selected component 
                //belongs to the pathway
                if (getMap().getPathwayManager().getCurrentPathway() != null) {
                    if (getMap().getPathwayManager().getCurrentPathway().contains(hitComponent)) {
                        Actions.AddPathwayNode.setEnabled(false);
                        Actions.DeletePathwayNode.setEnabled(true);
                    } else {
                        Actions.AddPathwayNode.setEnabled(true);
                        Actions.DeletePathwayNode.setEnabled(false);
                    }   
                }else{
                    Actions.AddPathwayNode.setEnabled(false);
                    Actions.DeletePathwayNode.setEnabled(false);
                }
                
                if (VueSelection.size() == 1 && VueSelection.get(0) instanceof LWNode)
                    Actions.HierarchyView.setEnabled(true);
                else
                    Actions.HierarchyView.setEnabled(false);

                getComponentPopup(hitComponent).show(e.getComponent(), e.getX(), e.getY());

            } else {

                getSelectionPopup(hitComponent).show(e.getComponent(), e.getX(), e.getY());
                
            }
        }
                
        
        private Point lastDrag = new Point();
        private void dragRepositionViewport(Point mouse)
        {
            if (DEBUG_MOUSE) {
                System.out.println("lastDragLoc " + lastDrag);
                System.out.println("lastMouseLoc " + mouse);
            }
            //int dx = dragStart.x - screenX;
            //int dy = dragStart.y - screenY;
            int dx = lastDrag.x - mouse.x;
            int dy = lastDrag.y - mouse.y;
            if (getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) getParent();
                Point location = viewport.getViewPosition();
                if (DEBUG_MOUSE) System.out.println("current " + location);
                if (DEBUG_MOUSE) System.out.println("ORIGIN  " + viewportAtDragStart + " dx=" + dx + " dy=" + dy);
                //Point newPosition = new Point(viewportAtDragStart);
                //if (Math.abs(dx) > Math.abs(dy))
                //dy = 0;
                //else
                //    dx = 0;
                location.translate(dx, dy);
                //newPosition.translate(dx, dy);
                //System.out.println("   new  " + newPosition);
                if (DEBUG_MOUSE) System.out.println("   new  " + location);
                viewport.setViewPosition(location);
                //viewport.setViewPosition(newPosition);
            } else {
                setMapOriginOffset(originAtDragStart.getX() + dx,
                                   originAtDragStart.getY() + dy);
            }
        }

                
        /** mouse has moved while dragging out a selector box -- update
            selector box shape & repaint */
        private void dragResizeSelectorBox(int screenX, int screenY)
        {
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
            if (DEBUG_PAINT && redrawingSelector)
                System.out.println("dragResizeSelectorBox: already repainting selector");

            // XOR drawing simply keeps repainting on an existing graphics context,
            // which is extremely fast (because we can just XOR erase the previous
            // paint by redrawing it again) but the PC graphics context gets
            // polluted with garbage when left around, and now it looks like on mac too?
            //if (VueUtil.isMacPlatform())
            //redrawingSelector = true; // todo: does mac now do this same with 1.4.1-1 update?
            
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


        public void mouseMoved(MouseEvent e)
        {
            if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            lastMouseX = e.getX();
            lastMouseY = e.getY();


            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            // use deepest to penetrate into groups
            LWComponent hit = getMap().findDeepestChildAt(mapX, mapY);
            //LWComponent hit = getMap().findChildAt(mapX, mapY);
            if (DEBUG_ROLLOVER) System.out.println("  mouseMoved: hit="+hit);

            if (hit != sMouseOver) {
                if (sMouseOver != null) {
                    clearTip(); // in case it had a tip displayed
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
                clearTip(); // if over nothing, always make sure no tip displayed
            
            sMouseOver = hit;

            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouse.x = lastMouseX;
                mouse.y = lastMouseY;
                repaint();
            }

            // Workaround for known Apple Mac OSX Java 1.4.1 bug:
            // Radar #3164718 "Control-drag generates mouseMoved, not mouseDragged"
            //if (dragComponent != null && VueUtil.isMacPlatform()) {
            //    if (DEBUG_MOUSE_MOTION) System.out.println("manually invoking mouseDragged");
            //    mouseDragged(e);
            //}

            // todo: don't use the Timer class here -- too unreliable
            if (RolloverAutoZoomDelay >= 0) {
                if (DEBUG_TIMER_ROLLOVER && !inDrag && !(activeTextEdit != null)) {
                    if (RolloverAutoZoomDelay > 10) {
                        if (rolloverTask != null)
                            rolloverTask.cancel();
                        rolloverTask = new RolloverTask();
                        rolloverTimer.schedule(rolloverTask, RolloverAutoZoomDelay);
                    } else {
                        new RolloverTask().run();
                    }
                }
            }
        }

        public void mouseEntered(MouseEvent e)
        {
            if (DEBUG_ROLLOVER) System.out.println(e);
            if (sMouseOver != null) {
                sMouseOver.mouseExited(new MapMouseEvent(e));
                sMouseOver = null;
            }
        }

        public void mouseExited(MouseEvent e)
        {
            if (DEBUG_ROLLOVER) System.out.println(e);
            if (false&&sMouseOver != null) {
                sMouseOver.mouseExited(new MapMouseEvent(e));
                sMouseOver = null;
            }

            // turned off to be SURE we get into a show/hide loop if pop-up obscures
            // the trigger region and mouse statys over it...  Okay, this should
            // never happen...

            clearTip();//todo: on a timer instead so no flashing of rollover the tip

            // would still be nice to do this tho because we get a mouse
            // exited when you rollover the tip-window itself, and if
            // it's right at the edge of the node and you're going for
            // the resize-control, better to have the note clear out
            // so you don't accidentally hit the tip when going for the control.
        }
        
        //private int drags=0;
        public void mouseDragged(MouseEvent e)
        {
            inDrag = true;
            clearRollover();
            //System.out.println("drag " + drags++);
            if (mouseWasDragged == false) {
                // dragStart
                // we're just starting this drag
                if (dragComponent != null || dragControl != null)
                    mouseWasDragged = true;
                lastDrag.setLocation(dragStart);
            }
            
            if (DEBUG_SHOW_MOUSE_LOCATION) mouse = e.getPoint();
            if (DEBUG_MOUSE_MOTION) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            int screenX = e.getX();
            int screenY = e.getY();
            Point currentMousePosition = e.getPoint();
            
            if (activeTool == HandTool) {
                // drag the entire map
                if (originAtDragStart != null) {
                    dragRepositionViewport(currentMousePosition);
                    lastDrag.setLocation(currentMousePosition);
                    return;
                } else
                    throw new RuntimeException("null originAtDragStart");
            }
            
            //-------------------------------------------------------
            // Stop component dragging if the mouse leaves our component (the viewer)
            // todo: auto-pan as we get close to edge
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

            if (!activeTool.supportsDraggedSelector(e)) // todo: dragControls could be skipped!
                return;
            
            if (dragComponent == null && isDraggingSelectorBox) {
                //-------------------------------------------------------
                // We're doing a drag select-in-region.
                // Update the dragged selection box.
                //-------------------------------------------------------
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

                //-------------------------------------------------------
                // Compute more repaint region
                //-------------------------------------------------------

                //System.out.println("  Adding " + dragComponent.getBounds());
                if (OPTIMIZED_REPAINT) repaintRegion.add(dragComponent.getBounds());
                //if (DEBUG_PAINT) System.out.println("     Got " + repaintRegion);
                
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
                
                //LWNode over = getMap().findLWNodeAt(mapX, mapY, dragComponent);
                LWNode over = getMap().findLWNodeAt(mapX, mapY);
                if (indication != null && indication != over) {
                    //repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidParentTarget(over)) {
                    setIndicated(over);
                    //repaintRegion.add(over.getBounds());
                }
            }

            if (dragComponent == null && dragControl == null) {
                return;
            }

            if (!OPTIMIZED_REPAINT) {

                repaint();

            } else {
                //if (DEBUG_PAINT) System.out.println("MAP REPAINT REGION: " + repaintRegion);                
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
                if (dragControl != null)
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

                    //i = movingComponent.getAllConnectedComponents().iterator();
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
                    //if (DEBUG_PAINT) System.out.println("RR adding: " + c);
                    repaintRegion.add(c.getBounds());
                }
                //if (linkSource != null) repaintRegion.add(linkSource.getBounds());

                // TODO BUG: something extra is getting added into repaint region making
                // (between top diagnostic and here) that's make it way bigger than needed,
                // and controlPoints are causing 0,0 to be added to the repaint region.
                // create a RepaintRegion rectangle object that understands the idea
                // of an empty region (not just 0,0), and an unintialized RR that has no location or size.
                //if (DEBUG_PAINT) System.out.println("MAP REPAINT REGION: " + repaintRegion);
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

        public void mouseReleased(MouseEvent e)
        {
            inDrag = false;
            if (DEBUG_MOUSE) System.out.println("[" + e.paramString() + "]");

            setLastMousePoint(e.getX(), e.getY());

            MapMouseEvent mme = new MapMouseEvent(e, draggedSelectorBox);
            mme.setMousePress(lastMousePressX, lastMousePressY);
            
            if (mouseWasDragged && dragControl != null) {
                dragControl.controlPointDropped(dragControlIndex, mme);
            }
            else if (activeTool.handleMouseReleased(mme)) {
                repaint();
            }
            else if (mouseWasDragged && (indication == null || indication instanceof LWNode)) {
                checkAndHandleNodeDropReparenting();
            }

            //-------------------------------------------------------
            // reset all in-drag only state
            //-------------------------------------------------------
            dragControl = null;
            dragComponent = null;
            isDraggingSelectorBox = false;
            mouseWasDragged = false;
            clearIndicated();
            
            if (draggedSelectorBox != null && !activeTool.supportsDraggedSelector(e))
                System.err.println("Illegal state warning: we've drawn a selector box w/out tool that supports it!");
            
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
                    Class selectionType = null;
                    // todo: use something link activeTool.getSelectionType
                    if (activeTool == LinkTool)
                        selectionType = LWLink.class;
                    else if (activeTool == NodeTool)
                        selectionType = LWNode.class;

                    List list = computeSelection(screenToMapRect(draggedSelectorBox), selectionType);
                                                 
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

            if (toolKeyReleased) {
                toolKeyReleased = false;
                revertTemporaryTool();
            }

            if (getParent() instanceof JViewport)
                setPreferredSize(mapToScreenDim(getMap().getBounds()));

            // todo opt: only need to do this if we don't draw selection
            // handles while dragging (this is to put them back if we werent)
            // use selection repaint region?
            //repaint();

        }
            

        /**
         * Take what's in the selection and drop it on the current indication,
         * or on the map if no current indication.
         */
        private void checkAndHandleNodeDropReparenting()
        {
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
                if (droppedChild.getParent() instanceof LWGroup)
                    continue;
                // don't do anything if parent might be reparenting
                if (droppedChild.getParent().isSelected())
                    continue;
                // todo: actually re-do drop if anything other than map so will re-layout
                if ((droppedChild.getParent() != parentTarget || parentTarget instanceof LWNode)
                    && droppedChild != parentTarget) {
                    //-------------------------------------------------------
                    // we were over a valid NEW parent -- reparent
                    //-------------------------------------------------------
                    if (DEBUG_PARENTING)
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
                if (DEBUG_PARENTING)  System.out.println("*** HANDLING PARENT " + parent);
                parent.removeChildren(moveList.iterator());
            }
            i = moveList.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                parentTarget.addChild(c);
            }
                
                
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
                
        private final boolean noModifierKeysDown(MouseEvent e)
        {
            return (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isDoubleClickEvent(MouseEvent e)
        {
            return e.getClickCount() == 2
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }
        
        private final boolean isSingleClickEvent(MouseEvent e)
        {
            return e.getClickCount() == 1
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0
                && (e.getModifiers() & ALL_MODIFIER_KEYS_MASK) == 0;
        }

        private final boolean isRightClickEvent(MouseEvent e)
        {
            // 1 click, button 2 or 3 pressed, button 1 not already down & ctrl not down
            return e.getClickCount() == 1
                && (e.getButton() == java.awt.event.MouseEvent.BUTTON3 ||
                    e.getButton() == java.awt.event.MouseEvent.BUTTON2)
                && (e.getModifiersEx() & java.awt.event.InputEvent.BUTTON1_DOWN_MASK) == 0
                && !e.isControlDown();
        }
        
        public void mouseClicked(MouseEvent e)
        {
            if (DEBUG_MOUSE) System.out.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");

            //if (activeTool != ArrowTool && activeTool != TextTool)
            //return;  check supportsClick, and add such to node tool
            

            if (!hitOnSelectionHandle) {
                
            if (isSingleClickEvent(e)) {
                if (DEBUG_MOUSE) System.out.println("\tSINGLE-CLICK on: " + hitComponent);

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
                            Actions.NewNode.act();
                        else
                            Actions.NewText.act();
                    }
                }
                /*
                if (activeTool.supportsClick()) {
                    //activeTool.handleClickEvent(e, hitComponent); send in mapxy
                }
                */
                
            } else if (isDoubleClickEvent(e) && toolKeyDown == 0 && hitComponent != null) {
                if (DEBUG_MOUSE) System.out.println("\tDOULBLE-CLICK on: " + hitComponent);
                
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
        public boolean isValidParentTarget(LWComponent parentTarget)
        {
            //if (dragComponent == draggedSelectionGroup && parentTarget.isSelected())
            if (parentTarget.isSelected())
                // meaning it's in the dragged selection, so it can never be a drop target
                return false;
            if (parentTarget == dragComponent)
                return false;
            if (parentTarget.getParent() == dragComponent)
                return false;
            return true;
        }
        
    }

    class ResizeControl implements LWSelection.ControlListener
    {
        private boolean active = false;
        private LWSelection.ControlPoint[] handles = new LWSelection.ControlPoint[8];

        private Rectangle2D.Float originalSelectionBounds;
        private Rectangle2D.Float draggedBounds;
        private Rectangle2D.Float[] original_lwc_bounds;
        private Box2D resize_box = null;

        ResizeControl()
        {
            for (int i = 0; i < handles.length; i++)
                handles[i] = new LWSelection.ControlPoint(COLOR_SELECTION_HANDLE);
        }
        
        /** interface ControlListener */
        public LWSelection.ControlPoint[] getControlPoints()
        {
            return handles;
        }

        private boolean isTopCtrl(int i) { return i == 0 || i == 1 || i == 2; }
        private boolean isLeftCtrl(int i) { return i == 0 || i == 6 || i == 7; }
        private boolean isRightCtrl(int i) { return i == 2 || i == 3 || i == 4; }
        private boolean isBottomCtrl(int i) { return i == 4 || i == 5 || i == 6; }
        
        /** interface ControlListener handler -- for handling resize on selection */
        public void controlPointPressed(int index, MapMouseEvent e)
        {
            System.out.println(this + " resize control point " + index + " pressed");
            originalSelectionBounds = (Rectangle2D.Float) VueSelection.getShapeBounds();
            //originalSelectionBounds = (Rectangle2D.Float) VueSelection.getBounds();
            resize_box = new Box2D(originalSelectionBounds);
            draggedBounds = resize_box.getRect();
            //draggedBounds = (Rectangle2D.Float) originalSelectionBounds.getBounds2D();

            original_lwc_bounds = new Rectangle2D.Float[VueSelection.size()];
            Iterator i = VueSelection.iterator();
            int idx = 0;
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (!(c instanceof LWLink)) 
                    original_lwc_bounds[idx++] = (Rectangle2D.Float) c.getShapeBounds();
                //original_lwc_bounds[idx++] = (Rectangle2D.Float) c.getBounds();
            }
        }

        /** interface ControlListener handler -- for handling resize on selection */
        public void controlPointMoved(int i, MapMouseEvent e)
        {
            //System.out.println(this + " resize control point " + i + " moved");
            
            // control points are indexed starting at 0 in the upper left,
            // and increasing clockwise ending at 7 at the middle left point.
            
                 if (isTopCtrl(i))    resize_box.setULY(e.getMapY());
            else if (isBottomCtrl(i)) resize_box.setLRY(e.getMapY());
                 if (isLeftCtrl(i))   resize_box.setULX(e.getMapX());
            else if (isRightCtrl(i))  resize_box.setLRX(e.getMapX());
            
            draggedBounds = resize_box.getRect();

            dragResizeReshapeSelection(i, VueSelection.iterator(), VueSelection.size() > 1 && e.isAltDown());
        }

        /** @param cpi - control point index (which ctrl point is being moved) */
        // todo: consider moving this code to LWGroup so that they can resize
        private void dragResizeReshapeSelection(int cpi, Iterator i, boolean repositionOnly)
        {
            int idx = 0;
            float scaleX = draggedBounds.width / originalSelectionBounds.width;
            float scaleY = draggedBounds.height / originalSelectionBounds.height;
            //System.out.println("scaleX="+scaleX);System.out.println("scaleY="+scaleY);
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c instanceof LWLink) // must match conditinal aboice where we collect original_lwc_bounds[]
                    continue;
                if (c.getParent().isSelected()) // skip if our parent also being resized -- race conditions possible
                    continue;
                Rectangle2D.Float c_original_bounds = original_lwc_bounds[idx++];
                Rectangle2D.Float c_new_bounds = new Rectangle2D.Float();

                if (c.supportsUserResize() && repositionOnly == false) {
                    //-------------------------------------------------------
                    // Resize
                    //-------------------------------------------------------
                    c_new_bounds.width = c_original_bounds.width * scaleX;
                    c_new_bounds.height = c_original_bounds.height * scaleY;
                    if (c instanceof LWNode)
                        ((LWNode)c).setAutoSized(false);
                    c.setAbsoluteSize(c_new_bounds.width, c_new_bounds.height);
                }
                
                if ((c.getParent() instanceof LWNode) == false) {
                    //-------------------------------------------------------
                    // Reposition (todo: needs work in the case of not resizing)
                    //-------------------------------------------------------
                    // if our parent is a node, parent handles positioning -- don't move it here
                    c_new_bounds.x = draggedBounds.x + (c_original_bounds.x - originalSelectionBounds.x) * scaleX;
                    c_new_bounds.y = draggedBounds.y + (c_original_bounds.y - originalSelectionBounds.y) * scaleY;

                    if (isLeftCtrl(cpi)) {
                        if (c_new_bounds.x + c.getWidth() > resize_box.lr.x)
                            c_new_bounds.x = resize_box.lr.x - c.getWidth();
                    }
                    if (isTopCtrl(cpi)) {
                        if (c_new_bounds.y + c.getHeight() > resize_box.lr.y)
                            c_new_bounds.y = resize_box.lr.y - c.getHeight();
                    }
                    c.setLocation(c_new_bounds.x, c_new_bounds.y);
                }
            }
        }
        
        /** interface ControlListener handler -- for handling resize on selection */
        public void controlPointDropped(int index, MapMouseEvent e)
        {
            //System.out.println("MapViewer: resize control point " + index + " dropped");
            draggedBounds = null;
            Actions.NodeMakeAutoSized.checkEnabled();
        }

    }
    //-------------------------------------------------------
    // end of class ResizeControl
    //-------------------------------------------------------

    private boolean isAnythingCurrentlyVisible()
    {
        Rectangle mapRect = mapToScreenRect(getMap().getBounds());
        Rectangle viewerRect = getBounds(null);
        return mapRect.intersects(viewerRect);
    }

    public void focusLost(FocusEvent e)
    {
        if (DEBUG_FOCUS) System.out.println(this + " focusLost (to " + e.getOppositeComponent() +")");

        if (VueUtil.isMacPlatform()) {

            // On Mac, our manual tool-tip popups sometimes (and
            // sometimes inconsistently) when they are a big heavy
            // weight popups (e.g, 40 lines of notes) will actually
            // grab the focus away from the app!  We request to get
            // the focus back, but it doesn't appear that actually
            // works.
            
            String opName = e.getOppositeComponent().getName();
            // hack: check the name against the special name of Popup$HeavyWeightWindow
            if (opName != null && opName.equals("###overrideRedirect###")) {
                if (DEBUG_FOCUS) System.out.println("\tLOST TO POPUP!");
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
        
        // todo: if focus is lost but NOT to another map viewer which then
        // grabs vue app focus, then we repaint here to clear our green
        // focus border, BUT, we still have application focus..

        // todo: going to have to have a *vue* application focus event
        // we can listen to so we simply know when any other viewer
        // grabs the focus from us.  (The vue application focus is
        // used to determine what viewer all the toolbar menu actions
        // should act upon)
        repaint();
    }

    private void grabVueApplicationFocus(String from)
    {
        if (DEBUG_FOCUS) System.out.println(this + " grabVueApplicationFocus triggered thru " + from);
        if (VUE.getActiveViewer() != this) {
            if (DEBUG_FOCUS) System.out.println(this + " grabVueApplicationFocus " + from + " *** GRABBING ***");
            //new Throwable("REAL GRAB").printStackTrace();
            MapViewer activeViewer = VUE.getActiveViewer();
            if (activeViewer != this) {
                LWMap oldActiveMap = null;
                if (activeViewer != null)
                    oldActiveMap = activeViewer.getMap();
                VUE.setActiveViewer(this);
                
                //added by Daisuke Fujiwara
                //accomodates pathway manager swapping when the displayed map is changed
                //can this be moved to setActiveViewer method????

                if (VUE.getPathwayInspector() != null) {
                    if (DEBUG_FOCUS) System.err.println("Setting pathway manager to: " + getMap().getPathwayManager());
                    VUE.getPathwayInspector().setPathwayManager(getMap().getPathwayManager());
                }
                    
                //outline view
                if (DEBUG_FOCUS) System.err.println("Notifying outline tree view...");
                  VUE.getOutlineViewTree().switchMap(getMap());
                    
                //hierarchy view
                /*
                Doesn't compile -- SMF 2004-01-04 12:32.51 Sunday
                if(this.map instanceof LWHierarchyMap)
                  VUE.getHierarchyTree().setHierarchyModel(((LWHierarchyMap)this.map).getHierarchyModel());
                
                else
                  VUE.getHierarchyTree().setHierarchyModel(null);
                */
                
                //end of addition
                
                this.VueSelection = VUE.ModelSelection;
                if (oldActiveMap != this.map) {
                    if (DEBUG_FOCUS) System.out.println("oldActive=" + oldActiveMap + " active=" + this.map + " CLEARING SELECTION");
                    resizeControl.active = false;
                    // clear and notify since the selected map changed.
                    VUE.ModelSelection.clearAndNotify();
                }
            }
        } 
    }
    public void focusGained(FocusEvent e)
    {
        if (DEBUG_FOCUS) System.out.println(this + " focusGained (from " + e.getOppositeComponent() + ")");
        grabVueApplicationFocus("focusGained");
        repaint();
        new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise();
    }

    /**
     * Make sure everybody 
     */
    public void setVisible(boolean doShow)
    {
        if (DEBUG_FOCUS) System.out.println(this + " setVisible " + doShow);
        //if (!getParent().isVisible()) {
        if (doShow && getParent() == null) {
            if (DEBUG_FOCUS) System.out.println(this + " IGNORING (parent null)");
            return;
        }
        super.setVisible(doShow);
        if (doShow) {
            // todo: only do this if we've just been opened
            //if (!isAnythingCurrentlyVisible())
            //zoomTool.setZoomFitContent(this);//todo: go thru the action
            grabVueApplicationFocus("setVisible");
            requestFocus();
            new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise();
            //new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise(); // handled in focusGained
            // only need to do this if this viewer displaying a different MAP
            repaint();
        } else {
            new MapViewerEvent(this, MapViewerEvent.HIDDEN).raise();
        }
    }

    /*
    public Component findComponentAt(int x, int y) {
        return super.findComponentAt(x,y);
    }

    public Component X_locate(int x, int y) {
        System.out.println("MapViewer locate " + x + "," + y);
        return super.locate(x,y);
    }
    public void X_eventDispatched(AWTEvent e)
    {
        System.out.println("*** " + e);
    }

    protected void processEvent(AWTEvent e)
    {
        //System.err.println("processEvent[" + e.paramString() + "] on " + e.getSource().getClass().getName());
        super.processEvent(e);
    }
    static class EventFrame extends JFrame
    {
        public EventFrame(String title)
        {
            super(title);
        }

        protected void X_processEvent(AWTEvent e)
        {
            System.out.println("### " + e);
            super.processEvent(e);
        }
        public Component X_locate(int x, int y) {
            System.out.println("EventFrame locate " + x + "," + y);
            return super.locate(x,y);
        }
        public Component findComponentAt(int x, int y) {
            Component c = super.findComponentAt(x,y);
            //System.out.println("EventFrame findComponentAt " + x + "," + y + " = " + c);
            return c;
        }

    }
    */

    public static void main(String[] args) {
        /*
         * create an example map
         */
        //tufts.vue.ConceptMap map = new tufts.vue.ConceptMap("Example Map");
        tufts.vue.LWMap map = new tufts.vue.LWMap("Example Map");
        
        /*
         * create the viewer
         */
        //JComponent mapView = new MapViewer(map);
        //VUE.getActiveViewer().setPreferredSize(new Dimension(400,300));
        installExampleNodes(map);
        MapViewer mapView = new tufts.vue.MapViewer(map);

        /*
         * create a an application frame with some components
         */

        JFrame frame = new JFrame("VUE Concept Map Viewer");
        //JFrame frame = new EventFrame("VUE Concept Map Viewer");
        frame.setContentPane(mapView);
        frame.setBackground(Color.gray);
        frame.setSize(500,400);
        //frame.pack();
        if (VueUtil.getJavaVersion() >= 1.4) {
            Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            p.x -= frame.getWidth() / 2;
            p.y -= frame.getHeight() / 2;
            frame.setLocation(p);
        }
        frame.validate();
        frame.show();
    }

    static void installExampleNodes(LWMap map)
    {
        // create some test nodes & links
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
        map.addNode(n2);
        map.addNode(n3);
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
    private JMenu getAssetMenu(Asset asset) {
        JMenu returnMenu = new JMenu("Behaviors");
        
        InfoRecordIterator i;
        try {
            i = asset.getInfoRecords();
            while(i.hasNext()) {
                  InfoRecord infoRecord = i.next();
                  JMenu infoRecordMenu = new  JMenu(infoRecord.getId().getIdString());
                  InfoFieldIterator inf = (InfoFieldIterator)infoRecord.getInfoFields();
                  while(inf.hasNext()) {
                      InfoField infoField = (InfoField)inf.next();
                      //String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getId().getIdString();
                      infoRecordMenu.add(FedoraUtils.getFedoraAction(infoField));
                      //infoRecordMenu.add(new FedoraAction(infoField.getId().getIdString(),method));
                  }
                  
                  returnMenu.add(infoRecordMenu);
            }
        } catch(Exception e) { System.out.println("MapViewer.getAssetMenu"+e);}
        return returnMenu;
    }   
// this class will move out of here

    class Box2D {
        Point2D.Float ul = new Point2D.Float(); // upper left corner
        Point2D.Float lr = new Point2D.Float(); // lower right corner

        public Box2D(Rectangle2D r) {
            ul.x = (float) r.getX();
            ul.y = (float) r.getY();
            lr.x = ul.x + (float) r.getWidth();
            lr.y = ul.y + (float) r.getHeight();
        }
        
        Rectangle2D.Float getRect() {
            return new Rectangle2D.Float(ul.x, ul.y, lr.x - ul.x, lr.y - ul.y);
        }
        
        // These set methods never let the box take negative width or height
        void setULX(float x) { ul.x = (x > lr.x) ? lr.x : x; }
        void setULY(float y) { ul.y = (y > lr.y) ? lr.y : y; }
        void setLRX(float x) { lr.x = (x < ul.x) ? ul.x : x; }
        void setLRY(float y) { lr.y = (y < ul.y) ? ul.y : y; }
    }
    
    class Box {
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


    /**
     * Convenience method that calculates the union of two rectangles
     * without allocating a new rectangle.
     *
     * @param x the x-coordinate of the first rectangle
     * @param y the y-coordinate of the first rectangle
     * @param width the width of the first rectangle
     * @param height the height of the first rectangle
     * @param dest  the coordinates of the second rectangle; the union
     *    of the two rectangles is returned in this rectangle
     * @return the <code>dest</code> <code>Rectangle</code>

    public static Rectangle computeUnion(Rectangle2D src, Rectangle2D dest) {
        int x1 = (x < dest.x) ? x : dest.x;
        int x2 = ((x+width) > (dest.x + dest.width)) ? (x+width) : (dest.x + dest.width);
        int y1 = (y < dest.y) ? y : dest.y;
        int y2 = ((y+height) > (dest.y + dest.height)) ? (y+height) : (dest.y + dest.height);

        dest.x = x1;
        dest.y = y1;
        dest.width = (x2 - x1);
        dest.height= (y2 - y1);
        return dest;
    }
     */
    
    protected String paramString() {
	return getMap() + super.paramString();
    }

    public String toString()
    {
        return "MapViewer<" + (isRightSide ? "right" : "*LEFT") + "> "
            + "\'" + getMap().getLabel() + "\' "
            + Integer.toHexString(hashCode());
    }
  
    
    //-------------------------------------------------------
    // debugging stuff
    //-------------------------------------------------------
    
    private boolean DEBUG_KEYS = VueResources.getBool("mapViewer.debug.keys");
    private boolean DEBUG_MOUSE = VueResources.getBool("mapViewer.debug.mouse");
    //private boolean DEBUG_MOUSE = true;
    private boolean DEBUG_MOUSE_MOTION = VueResources.getBool("mapViewer.debug.mouse_motion");
    
    private boolean DEBUG_SHOW_ORIGIN = false;
    private boolean DEBUG_ANTI_ALIAS = true;
    private boolean DEBUG_RENDER_QUALITY = false;
    private boolean DEBUG_SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
    private boolean DEBUG_FINDPARENT_OFF = false;
    private boolean DEBUG_TIMER_ROLLOVER = true;
    private boolean DEBUG_FONT_METRICS = false;// fractional metrics looks worse to me --SF
    private boolean OPTIMIZED_REPAINT = false;
    static boolean DEBUG_PAINT = false; // for all maps
    static boolean DEBUG_FOCUS = false; // for all maps

    private Point mouse = new Point();

    final Object AA_OFF = RenderingHints.VALUE_ANTIALIAS_OFF;
    Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;



}


