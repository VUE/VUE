package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
//import java.util.*;
import javax.swing.*;
//import javax.swing.text.JTextComponent;

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

//todo: rename LWViewer or LWCanvas?
public class MapViewer extends javax.swing.JPanel
    // We use a swing component instead of AWT to get double buffering.
    // (The mac AWT impl has does this anyway, but not the PC).
    implements VueConstants
               , FocusListener
               , LWComponent.Listener
               , LWSelection.Listener
               , VueToolSelectionListener
{
    private Rectangle2D.Float RepaintRegion = null;
    private Rectangle paintedSelectionBounds = null;

    public interface Listener extends java.util.EventListener
    {
        public void mapViewerEventRaised(MapViewerEvent e);
    }

    java.util.List tools = new java.util.ArrayList();

    protected LWMap map;                   // the map we're displaying & interacting with
    private TextBox activeTextEdit;          // Current on-map text edit
    //private MapTextEdit activeTextEdit;          // Current on-map text edit

    //-------------------------------------------------------
    // Selection support
    //-------------------------------------------------------

    /** an alias for the global selection -- sometimes taking on the value null */
    protected LWSelection VueSelection = VUE.ModelSelection;
    /** a group that contains everything in the current selection.
     *  Used for doing operations on the entire group (selection) at once */
    protected LWGroup draggedSelectionGroup = LWGroup.createTemporary(VueSelection);
    /** the currently dragged selection box */
    protected Rectangle draggedSelectorBox;
    /** the last selector box drawn -- for repaint optimization */
    protected Rectangle lastPaintedSelectorBox;
    /** are we currently dragging a selection box? */
    protected boolean draggingSelectorBox;
    /** are we currently in a drag of any kind? (mouseDragged being called) */
    protected boolean inDrag;                  

    //-------------------------------------------------------
    // For dragging out new links
    //-------------------------------------------------------
    private final LWComponent invisibleLinkEndpoint = new LWComponent();
    private final LWLink creationLink = new LWLink(invisibleLinkEndpoint);
    protected LWComponent indication;   // current indication (rollover hilite)

    //-------------------------------------------------------
    // Pan & Zoom Support
    //-------------------------------------------------------
    private double zoomFactor = 1.0;
    private float mapOriginX = 0;
    private float mapOriginY = 0;

    private VueTool activeTool;
    //CSB tool not tied to viewers:  // csb Tool gone:  ZoomTool zoomTool;
    
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

    public MapViewer(LWMap map)
    {
        //super(false); // turn off double buffering -- frame seems handle it?
        setOpaque(true);
        creationLink.setDisplayed(false);
        invisibleLinkEndpoint.addLinkRef(creationLink);
        setLayout(null);
        //setLayout(new NoLayout());
        //setLayout(new FlowLayout());
        //addMouseListener(ih);
        //addMouseMotionListener(ih);
        addKeyListener(inputHandler);
        addFocusListener(this);
        
        MapDropTarget mapDropTarget = new MapDropTarget(this);// new CanvasDropHandler
        this.setDropTarget(new java.awt.dnd.DropTarget(this, mapDropTarget));

        // todo: tab to show/hide all tool windows
        
        
        //FIX:  tool gone addTool(this.zoomTool = new ZoomTool(this));
        
        /*

        /******
        // CSB:  gone addTool(new ZoomTool(this, new int[]
            { KEY_ZOOM_IN_0,
              KEY_ZOOM_IN_1,
              KEY_ZOOM_OUT_0,
              KEY_ZOOM_OUT_1,
              KEY_ZOOM_FIT,
              KEY_ZOOM_ACTUAL },
              CURSOR_ZOOM_IN,
              CURSOR_ZOOM_OUT));

        addTool(new PanTool(this, KEY_TOOL_PAN, CURSOR_PAN));
        addTool(new SelectTool(this, KEY_TOOL_SELECT, CURSOR_SELECT));
        addTool(new NodeTool(this, KEY_TOOL_NODE));
        addTool(new LinkTool(this, KEY_TOOL_LINK));
        addTool(new TextTool(this, KEY_TOOL_TEXT));
        addTool(new PathwayTool(this, KEY_TOOL_PATHWAY));

        Tool's will also handle creating their own undo/redo
        objects with descriptions.

        */

        //setPreferredSize(new Dimension(cw,ch));
        //setSize(new Dimension(cw,ch));
        
        setPreferredSize(mapToScreenDim(map.getBounds()));
        
        //-------------------------------------------------------
        // set the background color here on the panel instead
        // of querying the map BG color every time in paintComponent
        // because the MapPanner, for instance, wants to use
        // it's own background color setting (hmm: should we let it?)
        //-------------------------------------------------------
        setBackground(map.getFillColor()); // todo: will need to listen for fill color changsae
        
        setFont(VueConstants.DefaultFont);
        loadMap(map);

        //-------------------------------------------------------
        // If this map was just restored, there might
        // have been an existing userZoom or userOrigin
        // set -- we honor that last user configuration here.
        //-------------------------------------------------------
        // Tool gone:  zoomTool.setZoom(getMap().getUserZoom(), false);
        Point2D p = getMap().getUserOrigin();
        setMapOriginOffset(p.getX(), p.getY());
            
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
    	
    	// listen to tool selection events
    	VueToolbarController.getController().addToolSelectionListener( this);
    }
    
    
    /** The currently selected tool **/
    private VueTool mCurTool;
    
    /**
     * getCurrentTool()
     * Gets the current VueTool that is selected.
     * @return the slected VueTool
     **/
    public VueTool getCurrentTool() {
    	return mCurTool;
    }
    
    /**
     * setCurrentTool(VueTool)
     * Sets the current VueTool for the map viewer.
     * Updates any selection or state issues pased on the tool
     * @param pTool - the new tool
     **/
    public void toolSelected( VueTool pTool) {
    	
    	mCurTool = pTool;
    	// DEBUG
    	System.out.println("MapViewer has a new tool: "+pTool.getID() );
    	//FIX: Update user input here?
    	// Dear Scott F.: enjoy this hook
    }

    void addTool(VueTool tool)
    {
        tools.add(tool);
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
    }
    public void setMapOriginOffset(double screenX, double screenY) {
        setMapOriginOffset((float) screenX, (float) screenY);
    }
    public Point2D getOriginLocation() {
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
    Point2D screenToMapPoint(Point p) {
        return screenToMapPoint(p.x, p.y);
    }
    Point2D screenToMapPoint(int x, int y) {
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
        if (mapRect.getWidth() < 0 || mapRect.getHeight() < 0)
            throw new IllegalArgumentException("mapDim<0");
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

    /* to be called by ZoomTool */
    void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        getMap().setUserZoom(zoomFactor);
        new MapViewerEvent(this, MapViewerEvent.ZOOM).raise();
        repaint();
    }
                    
    public double getZoomFactor()
    {
        return this.zoomFactor;
    }
    
    public void reshape(int x, int y, int w, int h)
    {
        if (DEBUG_PAINT) System.out.println(this + " reshape " + x + "," + y + " " + w + "x" + h);
        super.reshape(x,y, w,h);
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
        Rectangle r = mapToScreenRect(mapRect);
        r.width++;
        r.height++;
        return r;
    }

    private void repaintMapRegion(Rectangle2D mapRect)
    {
        if (OPTIMIZED_REPAINT)
            repaint(mapRectToPaintRegion(mapRect));
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
            VueSelection = null;
        else
            VueSelection = VUE.ModelSelection;
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
            // FYI, the selection itself could listen for this,
            // but that's a ton of events for this one thing.
            // todo: maybe have LWContainer check isSelected & manage it in deleteChild?
            LWComponent c = e.getComponent();
            boolean wasSelected = c.isSelected();

            if (wasSelected) {
                selectionRemove(c); // ensure isn't in selection
                
                // if we just dispersed a group that was selected,
                // put all the former children into the selection instead
                if (c instanceof LWGroup)
                    selectionAdd(((LWGroup)c).getChildIterator());
            }
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
     * By default, add all nodes hit by this box to a list for doing selection.
     * If NO nodes are in the list, search for links within the region
     * instead.  Of @param onlyLinks is true, only search for links.
     */
    private int SELECT_DEFAULT = 1; // nodes if any, links otherwise
    private int SELECT_LINKS = 2; // only select links
    private int SELECT_ALL = 3; // nodes & links (everything)
    private java.util.List computeSelection(Rectangle2D mapRect, boolean onlyLinks)
    {
        java.util.List hits = new java.util.ArrayList();
        java.util.Iterator i = getMap().getChildIterator();
        // todo: if want nested children to get seleced, will need a descending iterator

        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            boolean isLink = c instanceof LWLink;
            if (!onlyLinks && isLink)
                continue;
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
    
    public void setIndicated(LWComponent c)
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
    public void clearIndicated()
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
            if (redrawingSelector && draggedSelectorBox != null) {
                redrawSelectorBox((Graphics2D)g);
                redrawingSelector = false;
            } else
                super.paint(g);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("*paint* Exception painting in: " + this);
            System.err.println("*paint* VueSelection: " + VueSelection);
            System.err.println("*paint* Graphics: " + g);
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
            if (DEBUG_PAINT && RepaintRegion != null) {
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
        
        // anti-alias shapes by default
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_ON);
        // anti-alias text
        if (!DEBUG_ANTIALIAS_OFF)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Do we need fractional metrics?  Gives us slightly more accurate
        // string widths on noticable on long strings
        if (!DEBUG_ANTIALIAS_OFF)
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);


        //-------------------------------------------------------
        // Draw the map: Ask the model to render itself to our GC
        //-------------------------------------------------------

        this.map.draw(g2);

        //-------------------------------------------------------
        // Draw any link in the process of being dragged out
        //-------------------------------------------------------
        if (creationLink.isDisplayed())
            creationLink.draw(g2);

        // render the current indication on top
        //if (indication != null) indication.draw(g2);

        /*
        if (dragComponent != null) {
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
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

        if (!VueUtil.isMacPlatform()) // try aa selection on mac for now (todo)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);

        //-------------------------------------------------------
        // draw selection if there is one
        //-------------------------------------------------------
        
        if (VueSelection != null && !VueSelection.isEmpty())
            drawSelection(g2);

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
            g2.drawLine(mouseX,mouseY, mouseX,mouseY);

            int iX = (int) (screenToMapX(mouseX) * 100);
            int iY = (int) (screenToMapY(mouseY) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            g2.setFont(VueConstants.MediumFont);
            int y = 0;
            g2.drawString("screen(" + mouseX + "," +  mouseY + ")", 10, y+=15);
            g2.drawString("mapX " + mapX, 10, y+=15);
            g2.drawString("mapY " + mapY, 10, y+=15);;
            g2.drawString("zoom " + getZoomFactor(), 10, y+=15);
            g2.drawString("anitAlias " + !DEBUG_ANTIALIAS_OFF, 10, y+=15);
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

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_ON);
        
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

    /** redraw the selection box being dragged by the user
     * (erase old box, draw new box) */
    private void redrawSelectorBox(Graphics2D g2)
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
    
    private void drawSelectorBox(Graphics2D g2, Rectangle r)
    {
        // todo opt: would this be any faster done on a glass pane?
        //g2.setColor(COLOR_SELECTION_DRAG);
        g2.setXORMode(COLOR_SELECTION_DRAG);// using XOR may be working around below clip-edge bug
        g2.setStroke(STROKE_SELECTION_DYNAMIC);
        g2.draw(r);
    }

    /* Java/JVM 1.4.1 PC (Win32) Graphics Bugs

    #1: bottom edge clip-region STROKE ERASE BUG
    #2: clip-region (top edge?) TEXT WIGGLE BUG

    Can only see these bugs with repaint opt turned on -- where a clip
    region smaller than the whole panel is used during painting.

    #1 appears to go away when using XOR erase/redraw of selector box
    (currently a mac only option).
            
    Diagnosis 4: XOR selector erase/redraw seems to be a workaround
    for #1.  Can still reliablly produce using below trigger method
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
    private void drawSelection(Graphics2D g2)
    {
        g2.setColor(COLOR_SELECTION);
        g2.setStroke(STROKE_SELECTION);
        java.util.Iterator it = VueSelection.iterator();
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if (!(c instanceof LWLink))
                drawComponentSelectionBox(g2, c);
        }
        
        //if (!VueSelection.isEmpty() && (!inDrag || draggingSelectorBox)) {
        
        g2.setStroke(STROKE_ONE);
        // todo opt: don't recompute bounds here every paint ---
        // can cache in draggedSelectionGroup
        Rectangle2D selectionBounds = VueSelection.getBounds();
        /*
          bounds cache hack
          if (VueSelection.size() == 1)
          selectionBounds = VueSelection.first().getBounds();
          else
          selectionBounds = draggedSelectionGroup.getBounds();
        */
        //System.out.println("mapSelectionBounds="+selectionBounds);
        Rectangle2D.Float sb = mapToScreenRect2D(selectionBounds);
        paintedSelectionBounds = mapToScreenRect(selectionBounds);
        growForSelection(paintedSelectionBounds);
        //System.out.println("screenSelectionBounds="+sb);
        drawSelectionBox(g2, sb);
    }

    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final int SelectionHandleSize = 6; // selection handle fill size -- todo: config
    // exterior drawn box will be 1 pixel bigger
    private void drawSelectionHandle(Graphics2D g, float x, float y)
    {
        //x = Math.round(x);
        //y = Math.round(y);
        SelectionHandle.setFrame(x, y, SelectionHandleSize, SelectionHandleSize);
        g.setColor(COLOR_SELECTION_HANDLE);
        g.fill(SelectionHandle);
        g.setColor(COLOR_SELECTION);
        g.draw(SelectionHandle);
    }
    void drawSelectionBox(Graphics2D g, Rectangle2D.Float r)
    {
        g.draw(r);
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
    
    static final Rectangle2D ComponentHandle = new Rectangle2D.Float(0,0,0,0);
    static final int chs = 5; // component handle size -- todo: config
    // todo: if move this to LWComponent as a default, LWLink could
    // override with it's own, and ultimately users of our API could
    // implement their own selection rendering -- tho that would also
    // mean having an api for what happens when they drag the selection,
    // or even how they hit the selection handles in he first place.
    void drawComponentSelectionBox(Graphics2D g, LWComponent c)
    {
        g.setColor(COLOR_SELECTION);
//         if (c instanceof LWLink) {
//             LWLink l = (LWLink) c;
// need to center on stroke & convert to screen coords
//             ComponentHandle.setFrame(c.getCenterX(), c.getCenterY() , chs, chs);
//             g.fill(ComponentHandle);
//         } else {
        Rectangle2D.Float r = mapToScreenRect2D(c.getShapeBounds());
        g.draw(r);
        r.x -= (chs-1)/2;
        r.y -= (chs-1)/2;
        if (chs % 2 == 0) {
            // if box size is even, bias to inside the selection border
            r.height--;
            r.width--;
        }
        ComponentHandle.setFrame(r.x, r.y , chs, chs);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x, r.y + r.height, chs, chs);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x + r.width, r.y, chs, chs);
        g.fill(ComponentHandle);
        ComponentHandle.setFrame(r.x + r.width, r.y + r.height, chs, chs);
        g.fill(ComponentHandle);
//        }
        
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
    private JPopupMenu cPopup = null;
    private JMenu assetMenu = null;
    private JPopupMenu getMapPopup()
    {
        // this is just example menu code for the moment
        if (mapPopup == null) {
            mapPopup = new JPopupMenu("Map Menu");
            mapPopup.addSeparator();
            mapPopup.add(Actions.NewNode);
            mapPopup.add(Actions.NewText);
            mapPopup.addSeparator();
            mapPopup.add(Actions.SelectAll);
            //mapPopup.add(Actions.DeselectAll);
            //would be pointless to add deselect at moment as this menu only pops when no selection
            //mapPopup.add("Visible");
            //mapPopup.setBackground(Color.gray);
        }
        return mapPopup;
    }
    private JPopupMenu getComponentPopup(LWComponent c)
    {
        if (cPopup == null) {
            cPopup = new JPopupMenu("Item Menu");
            cPopup.add(Actions.Rename);
            cPopup.add(Actions.Duplicate);
            cPopup.addSeparator();
            cPopup.add(Actions.Group);
            cPopup.add(Actions.Ungroup);
            cPopup.addSeparator();
            cPopup.add(Actions.BringToFront);
            cPopup.add(Actions.BringForward);
            cPopup.add(Actions.SendToBack);
            cPopup.add(Actions.SendBackward);
            cPopup.addSeparator();
            for (int i = 0; i < Actions.ALIGN_MENU_ACTIONS.length; i++) {
                Action a = Actions.ALIGN_MENU_ACTIONS[i];
                if (a == null)
                    cPopup.addSeparator();
                else
                    cPopup.add(a);
            }
            cPopup.addSeparator();
            cPopup.add(Actions.Delete);
            
            cPopup.addSeparator();
            
            //added by Daisuke and Jay
            
            cPopup.add(Actions.AddPathwayNode);
            cPopup.add(Actions.DeletePathwayNode);
            cPopup.addSeparator();
            cPopup.add(Actions.HierarchyView);
            
            //end of addition
            
            // todo: special add-to selection action that adds
            // hitComponent to selection so have way other
            // than shift-click to add to selection (so you
            // can do it all with the mouse)
        }
        if (c instanceof LWNode) {
            LWNode n = (LWNode) c;
            Resource r = n.getResource();
            Asset a = r == null ? null : r.getAsset();  
            if(a != null && assetMenu == null) {
               assetMenu = getAssetMenu(a);
               cPopup.add(assetMenu);
            }else if(a != null) {
                cPopup.remove(assetMenu);
                assetMenu = getAssetMenu(a);
                cPopup.add(assetMenu);
            }else if(a == null && assetMenu != null) {
                cPopup.remove(assetMenu);
            }
        }
        return cPopup;
    }
    
    static final int RIGHT_BUTTON_MASK =
        java.awt.event.InputEvent.BUTTON2_MASK
        | java.awt.event.InputEvent.BUTTON3_MASK;
    static final int ALL_MODIFIER_KEYS_MASK =
        java.awt.event.InputEvent.SHIFT_MASK
        | java.awt.event.InputEvent.CTRL_MASK
        | java.awt.event.InputEvent.META_MASK
        | java.awt.event.InputEvent.ALT_MASK;
    
    
    class InputHandler extends tufts.vue.MouseAdapter
        implements java.awt.event.KeyListener
    {
        // temporary tool activators (while the key is held down)
        // They require a further mouse action to actually
        // do anythiing.
        static final int KEY_TOOL_PAN   = KeyEvent.VK_SPACE;
        static final int KEY_TOOL_ZOOM  = KeyEvent.VK_Z;
        static final int KEY_TOOL_ARROW = KeyEvent.VK_A;
        static final int KEY_ABORT_ACTION = KeyEvent.VK_ESCAPE;
        
        LWComponent dragComponent;
        LWComponent linkSource;
        boolean mouseWasDragged = false;
        LWComponent justSelected;    // for between mouse press & click

        /**
         * dragStart: screen location (within this java.awt.Container)
         * of mouse-down that started this drag. */
        Point dragStart = new Point();

        /**
         * dragOffset: absolute map distance mouse was from the
         * origin of the current dragComponent when the mouse was
         * pressed down. */
        Point2D dragOffset = new Point2D.Float();
        
        // toolKeyDown: a key being held down to temporarily activate
        // a particular tool;
        int toolKeyDown = 0;
        KeyEvent toolKeyEvent = null; // to get at kbd modifiers active at time of keypress

        private void setCursor(Cursor cursor)
        {
            SwingUtilities.getRootPane(MapViewer.this).setCursor(cursor);
            // could compute cursor-set pane in addNotify
        }
        
        public void keyPressed(KeyEvent e)
        {
            if (DEBUG_KEYS) System.out.println("[" + e.paramString() + "]");

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
                    double oldX = screenToMapX(dragStart.x) + dragOffset.getX();
                    double oldY = screenToMapY(dragStart.y) + dragOffset.getY();
                    dragComponent.setLocation(oldX, oldY);
                    dragComponent = null;
                    mouseWasDragged = false;
                    clearIndicated(); // incase dragging new link
                    repaint();
                }
                //removeLabelEdit();
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

            if (toolKeyDown == 0) {
                switch (key) {
                case KEY_TOOL_PAN:
                    if (dragComponent == null) {
                        // don't start dragging map if we're already
                        // dragging something on it.
                        toolKeyDown = key;
                        setCursor(CURSOR_PAN);
                    }
                    break;
                case KEY_TOOL_ZOOM:
                    toolKeyDown = key;
                    setCursor(e.isShiftDown() ? VUE.CURSOR_ZOOM_OUT : VUE.CURSOR_ZOOM_IN);
                    break;
                case KEY_TOOL_ARROW:
                    toolKeyDown = key;
                    setCursor(CURSOR_ARROW);
                    break;
                }
                if (toolKeyDown != 0)
                    toolKeyEvent = e;
            }

            // Now check for immediate action commands

            java.util.Iterator i = tools.iterator();
            while (i.hasNext()) {
                VueTool tool = (VueTool) i.next();
                if (tool.handleKeyPressed(e))
                    break;
            }


            // BUTTON1_DOWN_MASK Doesn't appear to be getting set in mac Java 1.4!
            //if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            if (e.isShiftDown()) {
                // display debugging features
                char c = e.getKeyChar();
                boolean did = true;
                if (c == 'M') {
                    DEBUG_SHOW_MOUSE_LOCATION = !DEBUG_SHOW_MOUSE_LOCATION;
                } else if (c == 'A') {
                    DEBUG_ANTIALIAS_OFF = !DEBUG_ANTIALIAS_OFF;
                    if (DEBUG_ANTIALIAS_OFF)
                        AA_ON = RenderingHints.VALUE_ANTIALIAS_OFF;
                    else
                        AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;
                } else if (c == 'O') {
                    DEBUG_SHOW_ORIGIN = !DEBUG_SHOW_ORIGIN;
                } else if (c == 'R') {
                    OPTIMIZED_REPAINT = !OPTIMIZED_REPAINT;
                } else if (c == 'F') {
                    DEBUG_FINDPARENT_OFF = !DEBUG_FINDPARENT_OFF;
                } else if (c == 'P') {
                    DEBUG_PAINT = !DEBUG_PAINT;
                } else
                    did = false;
                if (did) {
                    System.err.println("MapViewer diagnostic '" + c + "' toggled.");
                    repaint();
                }
            }
        }
        
        public void keyReleased(KeyEvent e)
        {
            if (toolKeyDown == e.getKeyCode()) {
                /*if (! (VueUtil.isMacPlatform() && toolKeyDown == KEY_TOOL_PAN)) {*/
                    toolKeyDown = 0;
                    toolKeyEvent = null;
                    setCursor(CURSOR_DEFAULT);
                    //}
            }
        }

        public void keyTyped(KeyEvent e) // not very useful -- has keyChar but no key-code
        {
            // System.err.println("[" + e.paramString() + "]");
        }


        private LWComponent hitComponent = null;
        private Point2D originAtDragStart;
        private Point viewportAtDragStart;
        public void mousePressed(MouseEvent e)
        {
            if (DEBUG_MOUSE)
                System.out.println(MapViewer.this + "[" + e.paramString()
                                   + (e.isPopupTrigger() ? " POP":"") + "]");
            
            grabVueApplicationFocus();
            requestFocus();
            
            dragStart.setLocation(e.getX(), e.getY());
            if (DEBUG_MOUSE)
                System.out.println("dragStart set to " + dragStart);
            
            //-------------------------------------------------------
            // If any "tool" keys are being held down, start special
            // operations and return (e.g., spacebar down for map drag)
            //-------------------------------------------------------

            if (toolKeyDown == KEY_TOOL_PAN) {
                originAtDragStart = getOriginLocation();
                if (getParent() instanceof JViewport)
                    viewportAtDragStart = ((JViewport)getParent()).getViewPosition();
                else
                    viewportAtDragStart = null;
                return;
            } else if (toolKeyDown == KEY_TOOL_ZOOM) {
                // FIX: zoomTool.setZoomPoint(screenToMapPoint(e.getPoint()));
               // if (toolKeyEvent.isShiftDown())
               //     //zoomTool.setZoomSmaller();
               // else
               //     zoomTool.setZoomBigger();
                return;
            }
            
            setLastMousePressPoint(e.getX(), e.getY());
            dragComponent = null;

            //-------------------------------------------------------
            // Check for hits on selection handles
            //-------------------------------------------------------
            
            // if (SelectionHandles.
            
            //-------------------------------------------------------
            // Check for hits on map LWComponents
            //-------------------------------------------------------
                
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());

            hitComponent = getMap().findLWComponentAt(mapX, mapY);
            if (DEBUG_MOUSE && hitComponent != null)
                System.out.println("\t    on " + hitComponent + "\n" + 
                                   "\tparent " + hitComponent.getParent());
            
            int mods = e.getModifiers();
            //e.isPopupTrigger()
            // java 1.4.0 bug on PC(w2k): isPopupTrigger isn't true for right-click!
            //if ((mods & RIGHT_BUTTON_MASK) != 0 && (mods & java.awt.Event.CTRL_MASK) == 0)
            
            if ((mods & RIGHT_BUTTON_MASK) != 0 && !e.isControlDown())
            {
                //-------------------------------------------------------
                // MOUSE: We've pressed the right button down, so pop
                // a context menu depending on what's in selection.
                //-------------------------------------------------------
                
                if (VueSelection.isEmpty()) {
                    getMapPopup().show(e.getComponent(), e.getX(), e.getY());
                } else {
                    getComponentPopup(hitComponent).show(e.getComponent(), e.getX(), e.getY());
                    
                    //enables and disables the add/delete pathway nodes menu 
                    //depending on whether there is a selected pathway and the selected component 
                    //belongs to the pathway
                    if(getMap().getPathwayManager().getCurrentPathway()!=null){
                        if(getMap().getPathwayManager().getCurrentPathway().contains(hitComponent)){
                            Actions.AddPathwayNode.setEnabled(false);
                            Actions.DeletePathwayNode.setEnabled(true);
                        }else{
                            Actions.AddPathwayNode.setEnabled(true);
                            Actions.DeletePathwayNode.setEnabled(false);
                        }   
                    }else{
                        Actions.AddPathwayNode.setEnabled(false);
                        Actions.DeletePathwayNode.setEnabled(false);
                    }
                    
                    if (VUE.ModelSelection.size() == 1 && VUE.ModelSelection.get(0) instanceof LWNode)
                      Actions.HierarchyView.setEnabled(true);
                    
                    else
                      Actions.HierarchyView.setEnabled(false);
                }
            }
            else if (hitComponent != null)
            {
                //-------------------------------------------------------
                // MOUSE: We've pressed the left (normal) mouse on SOME LWComponent
                //-------------------------------------------------------
                
                if (e.isControlDown() || e.isAltDown()) {
                    //-------------------------------------------------------
                    // Mod-drag off a component: NEW LINK CREATION
                    //-------------------------------------------------------
                    if (hitComponent instanceof LWGroup)
                        hitComponent = ((LWGroup)hitComponent).findLWSubTargetAt(mapX, mapY);
                    linkSource = hitComponent;
                    dragOffset.setLocation(0,0);
                    creationLink.setSource(linkSource);
                    creationLink.setDisplayed(true);
                    invisibleLinkEndpoint.setLocation(mapX, mapY);
                    dragComponent = invisibleLinkEndpoint;
                } else if (e.isShiftDown()) {
                    //-------------------------------------------------------
                    // Shift was down: TOGGLE SELECTION STATUS
                    //-------------------------------------------------------

                    selectionToggle(hitComponent);
                    
                } else {
                    //-------------------------------------------------------
                    // Vanilla mouse press:
                    //          (1) SET SELECTION
                    //          (2) GET READY FOR A POSSIBLE UPCOMING DRAG
                    // Clear any existing selection, and set to hitComponent.
                    // Also: mark drag start in case they start dragging
                    //-------------------------------------------------------

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
                    if (!(hitComponent instanceof LWLink)) { // makes no sense to drag links at moment.
                        draggedSelectionGroup.useSelection(VueSelection);
                        dragComponent = draggedSelectionGroup;
                    }
                  //} else {
                        // just pick up the single component
                        //dragComponent = hitComponent;
                  //}

                }
            } else {
                //-------------------------------------------------------
                // hitComponent was null
                //-------------------------------------------------------

                if (noModifierKeysDown(e) &&
                    VueSelection.size() > 1 &&
                    VueSelection.contains(mapX, mapY)) {
                    //-------------------------------------------------------
                    // PICK UP A GROUP SELECTION FOR DRAGGING
                    //
                    // If we clicked on nothing, but are actually within
                    // the bounds of an existing selection, pick it
                    // up for dragging.
                    //-------------------------------------------------------
                    draggedSelectionGroup.useSelection(VueSelection);
                    dragComponent = draggedSelectionGroup;
                } else if (!e.isShiftDown()) {
                    //-------------------------------------------------------
                    // CLEAR CURRENT SELECTION & START DRAGGING FOR A NEW ONE
                    //
                    // If we truly clicked on nothing, clear the selection,
                    // unless shift was down, which is easy to accidentally
                    // have happen if user is toggling the selection.
                    //-------------------------------------------------------
                    selectionClear();
                }
                draggingSelectorBox = true;
            }

            if (dragComponent != null)
                dragOffset.setLocation(dragComponent.getX() - mapX,
                                       dragComponent.getY() - mapY);

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
                repaint();
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

            if (VueUtil.isMacPlatform()) // todo bug: PC graphics context contains garbage when we do this
                redrawingSelector = true;
            
            if (OPTIMIZED_REPAINT)
                //paintImmediately(repaintRect);
                repaint(repaintRect);
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
            if (DEBUG_MOUSE_MOTION)
                System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
            // Workaround for known Apple Mac OSX Java 1.4.1 bug:
            // Radar #3164718 "Control-drag generates mouseMoved, not mouseDragged"
            if (dragComponent != null && VueUtil.isMacPlatform()) {
                if (DEBUG_MOUSE_MOTION) System.out.println("manually invoking mouseDragged");
                mouseDragged(e);
            }
        }

        //private int drags=0;
        public void mouseDragged(MouseEvent e)
        {
            inDrag = true;
            //System.out.println("drag " + drags++);
            if (mouseWasDragged == false) {
                // dragStart
                // we're just starting this drag
                if (dragComponent != null)
                    mouseWasDragged = true;
                lastDrag.setLocation(dragStart);
            }
            
            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            if (DEBUG_MOUSE) System.out.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            // todo:
            // activeTool.mouseDragged(e)
            // Tools implement MouseListener/MouseMotionListener

            int screenX = e.getX();
            int screenY = e.getY();
            Point currentMousePosition = e.getPoint();
            
            if (toolKeyDown == KEY_TOOL_PAN) {
                // drag the entire map
                if (originAtDragStart != null) {
                    dragRepositionViewport(currentMousePosition);
                    lastDrag.setLocation(currentMousePosition);
                    return;
                } else
                    throw new RuntimeException("null originAtDragStart");
            }
            
            //-------------------------------------------------------
            // Stop component dragging if the mouse leaves our component
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
            
            if (dragComponent == null && draggingSelectorBox) {
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

            Rectangle2D.Float repaintRegion = new Rectangle2D.Float();

            if (dragComponent != null) {
                // todo opt: do all this in dragStart
                //-------------------------------------------------------
                // Compute repaint region based on what's being dragged
                //-------------------------------------------------------
                repaintRegion.setRect(dragComponent.getBounds());
                //System.out.println("Starting " + repaintRegion);

                //if (repaintRegion == null) {// todo: this is debug
                //new Throwable("mouseDragged: null bounds dragComponent " + dragComponent).printStackTrace();
                //    repaintRegion = new Rectangle2D.Float();
                //}

                if (dragComponent instanceof LWLink) {
                    // todo: not currently used as link dragging disabled
                    LWLink lwl = (LWLink) dragComponent;
                    repaintRegion.add(lwl.getComponent1().getBounds());
                    repaintRegion.add(lwl.getComponent2().getBounds());
                }

                //-------------------------------------------------------
                // Reposition the component due to mouse drag
                //-------------------------------------------------------

                dragComponent.setLocation((float) (mapX + dragOffset.getX()),
                                          (float) (mapY + dragOffset.getY()));

                //-------------------------------------------------------
                // Compute more repaint region
                //-------------------------------------------------------

                //System.out.println("  Adding " + dragComponent.getBounds());
                repaintRegion.add(dragComponent.getBounds());
                //if (DEBUG_PAINT) System.out.println("     Got " + repaintRegion);
                
                if (dragComponent instanceof LWLink) {
                    // todo: not currently used as link dragging disabled
                    LWLink l = (LWLink) dragComponent;
                    repaintRegion.add(l.getComponent1().getBounds());
                    repaintRegion.add(l.getComponent2().getBounds());
                }
            }
            
            if (linkSource != null) {
                //-------------------------------------------------------
                // we're dragging a new link looking for an
                // allowable endpoint
                //-------------------------------------------------------
                LWComponent over = findLWLinkTargetAt(mapX, mapY);
                if (indication != null && indication != over) {
                    //repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidLinkTarget(over)) {
                    setIndicated(over);
                    //repaintRegion.add(over.getBounds());
                }
                //} else if (dragComponent instanceof LWNode && !DEBUG_FINDPARENT_OFF) {
            } else if (!DEBUG_FINDPARENT_OFF
                       //&& (dragComponent instanceof LWNode || VueSelection.allOfType(LWNode.class)) //todo opt: cache type
                       //todo: dragComponent for moment is only ever the LWGroup or a LWLink
                       && dragComponent != null
                       && !(dragComponent instanceof LWLink)
                       && !(VueSelection.allOfType(LWLink.class)) //todo opt: cache type
                    ) {
                
                // regular drag -- check for node drop onto another
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

            if (dragComponent == null) {
                return;
            }

            if (!OPTIMIZED_REPAINT) {

                repaint();

            } else {
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

                java.util.Iterator i = null;
                if (dragComponent instanceof LWLink) {
                    // todo: not in use as moment as Link dragging disabled...
                    LWLink l = (LWLink) dragComponent;
                    i = new VueUtil.GroupIterator(l.getLinkEndpointsIterator(),
                                                  l.getComponent1().getLinkEndpointsIterator(),
                                                  l.getComponent2().getLinkEndpointsIterator());
                                                      
                } else {
                    // TODO OPT: compute this once when we start the drag!
                    i = dragComponent.getAllConnectedNodes().iterator();
                }
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    repaintRegion.add(c.getBounds());
                }
                if (linkSource != null)
                    repaintRegion.add(linkSource.getBounds());

                Rectangle rr = mapToScreenRect(repaintRegion);
                growForSelection(rr);

                /*
                boolean draggingChild = false;
                if (!(dragComponent.getParent() instanceof LWMap)) {
                    dragComponent.setDisplayed(false);
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
            
            if (linkSource != null) {
                repaintMapRegionAdjusted(creationLink.getBounds());
                creationLink.setDisplayed(false);
                LWComponent linkDest = indication;
                if (linkDest != null && linkDest != linkSource)
                {
                    LWLink l = linkDest.getLinkTo(linkSource);
                    if (l != null) {
                        // There's already a link tween these two -- increment the weight
                        l.incrementWeight();
                    } else {
                        LWContainer addParent = getMap();
                        if (linkSource.getParent() == linkDest.getParent() &&
                            linkSource.getParent() != addParent)
                            addParent = linkSource.getParent(); // common parent
                        // todo: if parents different, add to the upper most parent
                        addParent.addChild(new LWLink(linkSource, linkDest));
                    }
                }
                linkSource = null;
//            } else if (mouseWasDragged && dragComponent instanceof LWNode) {
                // indication.allowsChildren()
            } else if (mouseWasDragged &&
                       (indication == null || indication instanceof LWNode)) {

                //-------------------------------------------------------
                // check to see if any things could be dropped on a new parent
                // This got alot more complicated adding support for
                // dropping whole selections of components.
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
                    if (droppedChild instanceof LWLink) // don't reparent links!
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

                // okay -- what we want is to tell the parent we're
                // moving from to remove them all at once -- the
                // problem is our selection could contain components
                // of multiple parents.  So we have to handle each
                // source parent seperately, and remove all it's
                // children at once -- this is so the parent won't
                // re-lay itself out (call layout()) while removing
                // children, because if does it will re-set the
                // position of other siblings about to be removed back
                // to the parent's layout spot from the draggeed
                // position they currently occupy and we're trying to
                // move them to.

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
            
            dragComponent = null;
            draggingSelectorBox = false;
            mouseWasDragged = false;

            if (indication != null)
                clearIndicated();
            
            if (draggedSelectorBox != null) {
                //System.out.println("dragged " + draggedSelectorBox);
                Rectangle2D.Float hitRect = (Rectangle2D.Float) screenToMapRect(draggedSelectorBox);
                //System.out.println("map " + hitRect);
                java.util.List list = computeSelection(hitRect, e.isControlDown());
                if (e.isShiftDown())
                    selectionToggle(list.iterator());
                else
                    selectionAdd(list.iterator());

                draggedSelectorBox.width++;
                draggedSelectorBox.height++;
                RR(draggedSelectorBox);
                draggedSelectorBox = null;
                lastPaintedSelectorBox = null;

                // bounds cache hack
                if (!VueSelection.isEmpty())
                    draggedSelectionGroup.useSelection(VueSelection);
                // todo: need to update draggedSelectionGroup here
                // so we can use it's cached bounds to compute
                // the painting of the selection -- rename to just
                // SelectionGroup if we keep using it this way.
                
            }

            if (getParent() instanceof JViewport)
                setPreferredSize(mapToScreenDim(getMap().getBounds()));

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
        
        public void mouseClicked(MouseEvent e)
        {
            if (DEBUG_MOUSE) System.out.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");

            if (isSingleClickEvent(e)) {
                if (DEBUG_MOUSE) System.out.println("\tSINGLE-CLICK on: " + hitComponent);
                if (hitComponent != null && !(hitComponent instanceof LWGroup)) {
                    if (hitComponent.isSelected() && hitComponent != justSelected)
                        activateLabelEdit(hitComponent);
                }
            } else if (isDoubleClickEvent(e) && toolKeyDown == 0) {
                if (DEBUG_MOUSE) System.out.println("\tDOULBLE-CLICK on: " + hitComponent);
                if (hitComponent instanceof LWNode) {
                    Resource resource = hitComponent.getResource();
                    if (resource != null) {
                        // todo: some kind of animation or something to show
                        // we're "opening" this node -- maybe an indication
                        // flash -- we'll need another thread for that.
                        if(resource.getAsset() != null) {
                            //AssetViewer a = new AssetViewer(resource.getAsset());
                            //a.setSize(600,400);
                            //a.setLocation(e.getX(),e.getY());
                            //8a.show();
                        } else {
                            resource.displayContent();
                        }
                        System.out.println("opening resource for: " + hitComponent);
                    } else
                        activateLabelEdit(hitComponent);
                } else if (hitComponent instanceof LWLink)
                    // todo: need LWComponent flag as to if supports displaying a label
                    activateLabelEdit(hitComponent);
            }

            justSelected = null;
        }

        public LWComponent findLWLinkTargetAt(float x, float y)
        {
            LWComponent directHit = getMap().findLWSubTargetAt(x, y);
            if (directHit != null)
                return directHit;
            
            java.util.List targets = new java.util.ArrayList();
            java.util.Iterator i = getMap().getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.targetContains(x, y) && isValidLinkTarget(c))
                    targets.add(c);
            }
            return findClosestEdge(targets, x, y);
        }
    
        /**
         * Make sure we don't create any links back on themselves.
         */
        public boolean isValidLinkTarget(LWComponent linkTarget)
        {
            if (linkTarget == linkSource)
                return false;
            if (linkTarget.getParent() == linkSource ||
                linkSource.getParent() == linkTarget)
                return false;
            
            boolean ok = true;
            if (linkTarget instanceof LWLink) {
                LWLink lwl = (LWLink) linkTarget;
                ok &= (lwl.getComponent1() != linkSource &&
                       lwl.getComponent2() != linkSource);
            }
            if (linkSource instanceof LWLink) {
                LWLink lwl = (LWLink) linkSource;
                ok &= (lwl.getComponent1() != linkTarget &&
                       lwl.getComponent2() != linkTarget);
            }
            return ok;
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

    private boolean isAnythingCurrentlyVisible()
    {
        Rectangle mapRect = mapToScreenRect(getMap().getBounds());
        Rectangle viewerRect = getBounds(null);
        return mapRect.intersects(viewerRect);
    }

    public void focusLost(FocusEvent e)
    {
        //if (DEBUG_FOCUS) System.out.println(this + " focusLost (to " + e.getOppositeComponent() +")");
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

    private void grabVueApplicationFocus()
    {
        if (VUE.getActiveViewer() != this) {
            MapViewer activeViewer = VUE.getActiveViewer();
            if (activeViewer != this) {
                LWMap oldActiveMap = null;
                if (activeViewer != null)
                    oldActiveMap = activeViewer.getMap();
                VUE.setActiveViewer(this);
                
                //added by Daisuke Fujiwara
                //accomodates pathway manager swapping when the displayed map is changed
                //can this be moved to setActiveViewer method????
                if (this == VUE.getActiveViewer())
                {
                    VUE.getPathwayControl().setPathwayManager(this.map.getPathwayManager());
                    VUE.getOverviewTree().switchMap(this.map);
                }
                //end of addition
                
                if (oldActiveMap != this.map)
                    VUE.ModelSelection.clear();
            }
        } 
        VueSelection = VUE.ModelSelection;
    }
    public void focusGained(FocusEvent e)
    {
        grabVueApplicationFocus();
        repaint();
        if (DEBUG_FOCUS) System.out.println(this + " focusGained (from " + e.getOppositeComponent() + ")");
        new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise();
    }

    /**
     * Make sure everybody 
     */
    public void setVisible(boolean doShow)
    {
        if (DEBUG_FOCUS) System.out.println(this + " setVisible " + doShow);
        super.setVisible(doShow);
        if (doShow) {
            // todo: only do this if we've just been opened
            //if (!isAnythingCurrentlyVisible())
            //zoomTool.setZoomFitContent(this);//todo: go thru the action
            grabVueApplicationFocus();
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
        System.out.println("MapViewer findComponentAt " + x + "," + y);
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
        frame.pack();
        if (VueUtil.getJavaVersion() >= 1.4) {
            Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            p.x -= frame.getWidth() / 2;
            p.y -= frame.getHeight() / 2;
            frame.setLocation(p);
        }
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
        
        n3.setResource("/tmp/foo.txt");
        
        n1.setLocation(100, 50);
        n2.setLocation(100, 100);
        n3.setLocation(50, 150);
        n4.setLocation(150, 150);
        n5.setLocation(150, 200);
        n6.setLocation(150, 250);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
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

    }
    private JMenu getAssetMenu(Asset asset) {
        JMenu returnMenu = new JMenu("Behaviors");
        
        InfoRecordIterator i;
        try {
            i = (InfoRecordIterator)asset.getInfoRecords();
            while(i.hasNext()) {
                  InfoRecord infoRecord = (InfoRecord)i.next();
                  JMenu infoRecordMenu = new  JMenu(infoRecord.getId().getIdString());
                  InfoFieldIterator inf = (InfoFieldIterator)infoRecord.getInfoFields();
                  while(inf.hasNext()) {
                      InfoField infoField = (InfoField)inf.next();
                      String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getValue().toString();
                      infoRecordMenu.add(new FedoraAction(infoField.getValue().toString(),method));
                  }
                  
                  returnMenu.add(infoRecordMenu);
            }
        } catch(Exception e) { System.out.println("MapViewer.getAssetMenu"+e);}
        return returnMenu;
    }   
// this class will move out of here

    protected String paramString() {
	return getMap() + super.paramString();
    }

    public String toString()
    {
        return "MapViewer[" + (isRightSide ? "right" : "left") + "] "
            + "(" + getMap().getLabel() + ")"
            + Integer.toHexString(hashCode());
    }
  
    
    //-------------------------------------------------------
    // debugging stuff
    //-------------------------------------------------------
    
    private boolean DEBUG_SHOW_ORIGIN = false;
    private boolean DEBUG_ANTIALIAS_OFF = false;
    private boolean DEBUG_SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
    private boolean DEBUG_KEYS = false;
    private boolean DEBUG_MOUSE = false;
    private boolean DEBUG_MOUSE_MOTION = false;
    private boolean DEBUG_FINDPARENT_OFF = false;
    private boolean DEBUG_FOCUS = false;
    private boolean OPTIMIZED_REPAINT = false;
    static boolean DEBUG_PAINT = false;
    private int mouseX;
    private int mouseY;

    final Object AA_OFF = RenderingHints.VALUE_ANTIALIAS_OFF;
    Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;



}

class FedoraAction extends AbstractAction {
    
    /** Creates a new instance of exitAction */
    static final String FEDORA_URL= "http://hosea.lib.tufts.edu:8080/fedora/";
    String method = null;
    public FedoraAction() {
    }
    
    public FedoraAction(String label,String method) {
        super(label);
        this.method = method;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
      try {
        VueUtil.openURL(FEDORA_URL+"get/"+method);
      } catch(Exception e) { System.out.println("AbstractAction.actionPerformed" +e);} 
    }
}
