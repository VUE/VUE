package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;

/**
 * MapViewer.java
 *
 * Implements a panel for displaying & interacting with
 * an instance of ConceptMap.  
 *
 * @author Scott Fraize
 * @version 3/16/03
 */
public class MapViewer extends javax.swing.JPanel
    // We use a swing component instead of AWT to get double buffering.
    // (The mac AWT impl has does this anyway, but not the PC).
    implements MapChangeListener
{
    static Font defaultFont = new Font("SansSerif", Font.PLAIN, 18);// todo: prefs
    static Font smallFont = new Font("SansSerif", Font.PLAIN, 10);// todo: prefs
    
    // todo: create our own cursors for most of these
    // named cursor types
    static final Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static final Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    static final Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    static final Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    static final Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    
    // tool cursor types
    static final Cursor CURSOR_ZOOM_IN  = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    static final Cursor CURSOR_ZOOM_OUT = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    static final Cursor CURSOR_PAN      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static final Cursor CURSOR_ARROW    = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    static final Cursor CURSOR_SUBSELECT= Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // white arrow

    java.util.List components = new java.util.ArrayList();
    java.util.List nodeViews = new java.util.ArrayList();
    java.util.List linkViews = new java.util.ArrayList();

    protected ConceptMap map;            // the map we're displaying & interacting with
    protected LWComponent selection;    // current selection
    protected LWComponent indication;   // current indication (rollover hilite)

    private final LWComponent dynamicLinkEndpoint = new LWComponent();
    private final LWLink dynamicLink = new LWLink(dynamicLinkEndpoint);

    private double zoomFactor = 1.0;

    //-------------------------------------------------------
    // temporary debugging attributes
    private final boolean DEBUG_DRAW_ORIGIN = false;
    private final boolean DEBUG_SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
    private int mouseX;
    private int mouseY;
    //-------------------------------------------------------
    
    public MapViewer(ConceptMap map)
    {
        dynamicLink.setDisplayed(false);
        //setLayout(new NoLayout());
        setLayout(null);
        InputHandler ih = new InputHandler();
        addMouseListener(ih);
        addMouseMotionListener(ih);
        addKeyListener(ih);

        MapDropTarget mapDropTarget = new MapDropTarget(this, map);
        this.setDropTarget(new java.awt.dnd.DropTarget(this, mapDropTarget));

        /*

        addTool(new ZoomTool(this, new int[]
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

        //setSize(300,200);
        setPreferredSize(new Dimension(400,300));
        setBackground(Color.white);
        setFont(defaultFont);

        loadMap(map);
    }

    /**
     * Set's the viewport such that the upper left corner
     * displays at screenX, screenY.  Is precision
     * due to possibility of zooming.
     */
    
    private float mapOriginX = 0;
    private float mapOriginY = 0;
    public void setMapOriginOffset(float screenX, float screenY)
    {
        this.mapOriginX = screenX;
        this.mapOriginY = screenY;
        this.map.setOrigin(mapOriginX, mapOriginY);
        //System.out.println("setOrigin " + screenX + "," + screenY);
    }
    public void setMapOriginOffset(double screenX, double screenY)
    {
        setMapOriginOffset((float) screenX, (float) screenY);
    }
    public Point2D getOriginLocation()
    {
        return new Point2D.Float(mapOriginX, mapOriginY);
    }
    public float getOriginX()
    {
        return mapOriginX;
    }
    public float getOriginY()
    {
        return mapOriginY;
    }
    final float screenToMapX(float x)
    {
        return (float) ((x + getOriginX()) / zoomFactor);
    }
    final float screenToMapY(float y)
    {
        return (float) ((y + getOriginY()) / zoomFactor);
    }
    final float screenToMapX(int x)
    {
        return (float) ((x + getOriginX()) / zoomFactor);
    }
    final float screenToMapY(int y)
    {
        return (float) ((y + getOriginY()) / zoomFactor);
    }
    final Point2D screenToMapPoint(Point p)
    {
        return screenToMapPoint(p.x, p.y);
    }
    final Point2D screenToMapPoint(int x, int y)
    {
        return new Point2D.Float(screenToMapX(x), screenToMapY(y));
    }

    /*
     * Zooming code
     */
    
    private final int ZOOM_ACTUAL = 11; // index of the value 1.0 in ZoomDefaults
    private final int ZOOM_MANUAL = -1;
    private final double[] ZoomDefaults = {
        1.0/32, 1.0/24, 1.0/16, 1.0/12, 1.0/8, 1.0/6, 1.0/4, 1.0/3, 1.0/2, 2.0/3, 0.75,
        1.0,
        1.25, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64
    };
    private int curZoom = ZOOM_ACTUAL;

    private Point2D zoomPoint = null;
    // set the center-on point in the map for the next zoom
    private void setZoomPoint(Point2D mapLocation)
    {
        this.zoomPoint = mapLocation;
    }

    public boolean setZoomBigger()
    {
        if (curZoom == ZOOM_MANUAL) {
            // find next highest zoom default
            for (int i = 0; i < ZoomDefaults.length; i++) {
                if (ZoomDefaults[i] > zoomFactor) {
                    setZoom(ZoomDefaults[curZoom = i]);
                    break;
                }
            }
        } else if (curZoom >= ZoomDefaults.length - 1)
            return false;
        else
            setZoom(ZoomDefaults[++curZoom]);
        return true;
    }
    
    public boolean setZoomSmaller()
    {
        if (curZoom == ZOOM_MANUAL) {
            // find next lowest zoom default
            for (int i = ZoomDefaults.length - 1; i >= 0; i--) {
                if (ZoomDefaults[i] < zoomFactor) {
                    setZoom(ZoomDefaults[curZoom = i]);
                    break;
                }
            }
        } else if (curZoom < 1)
            return false;
        else
            setZoom(ZoomDefaults[--curZoom]);
        return true;
    }
    
    public void setZoom(double zoomFactor)
    {
        setZoom(zoomFactor, true);
    }
    
    private void setZoom(double newZoomFactor, boolean adjustViewport)
    {
        curZoom = ZOOM_MANUAL;
        for (int i = 0; i < ZoomDefaults.length; i++) {
            if (newZoomFactor == ZoomDefaults[i]) {
                curZoom = i;
                break;
            }
        }

        if (adjustViewport) {
            Container c = this;
            Point2D zoomMapCenter = null;
            if (this.zoomPoint == null) {
                // center on the viewport
                zoomMapCenter = new Point2D.Float(screenToMapX(c.getWidth() / 2),
                                                   screenToMapY(c.getHeight() / 2));
            } else {
                // center on given point (e.g., where user clicked)
                zoomMapCenter = this.zoomPoint;
                this.zoomPoint = null;
            }

            float offsetX = (float) (zoomMapCenter.getX() * newZoomFactor) - c.getWidth() / 2;
            float offsetY = (float) (zoomMapCenter.getY() * newZoomFactor) - c.getHeight() / 2;

            setMapOriginOffset(offsetX, offsetY);
            // make Zoomable method for ZoomTool
        }
        
        this.zoomFactor = newZoomFactor;
        // make Zoomable method for ZoomTool
        
        // Set the title of any parent frame to show the %zoom
        int displayZoom = (int) (zoomFactor * 10000.0);
        // round the display value down to 2 digits
        if ((displayZoom / 100) * 100 == displayZoom)
            setTitleMessage((displayZoom / 100) + "%");
        else
            setTitleMessage( (((float) displayZoom) / 100f) + "%");

        repaint();
    }

    private static final int ZOOM_FIT_PAD = 16;
    public void setZoomFitContent(Component viewport)
    {
        Rectangle2D bounds = getAllComponentBounds();
        //System.err.println("viewport="+viewport);
        int viewWidth = viewport.getWidth() - ZOOM_FIT_PAD*2;
        int viewHeight = viewport.getHeight() - ZOOM_FIT_PAD*2;
        double vertZoom = (double) viewHeight / bounds.getHeight();
        double horzZoom = (double) viewWidth / bounds.getWidth();
        boolean centerVertical;
        double newZoom;
        if (horzZoom < vertZoom) {
            newZoom = horzZoom;
            centerVertical = true;
        } else {
            newZoom = vertZoom;
            centerVertical = false;
        }

        // set zoom ratio, but don't reposition the viewport,
        // as we're going to do this below manually.
        setZoom(newZoom, false);
                    
        // Now center the components within the dimension
        // that had extra room to scale in.
                    
        double offsetX = bounds.getX()*newZoom - ZOOM_FIT_PAD;
        double offsetY = bounds.getY()*newZoom - ZOOM_FIT_PAD;

        if (centerVertical)
            offsetY -= (viewHeight - bounds.getHeight()*newZoom) / 2;
        else // center horizontal
            offsetX -= (viewWidth - bounds.getWidth()*newZoom) / 2;
                        
        setMapOriginOffset(offsetX, offsetY);
    }

                    
    public double getZoomFactor()
    {
        return this.zoomFactor;
    }

    /*
     * end zoom code
     */

    public void reshape(int x, int y, int w, int h)
    {
        //System.out.println("reshape0 " + this);
        super.reshape(x,y, w,h);
        //System.out.println("reshape1 " + this);
        // todo: do viewport zoom
        repaint(250);
    }

    public ConceptMap getMap()
    {
        return this.map;
    }

    void loadMap(ConceptMap map)
    {
        if (this.map != null)
            unloadMap();
        this.map = map;
        // fixme todo: add all the components
        this.map.addMapListener(this);
    }
    
    void unloadMap()
    {
        if (this.map != null)
            this.map.removeMapListener(this);
        this.components.clear();
    }

    LWNode findLWNode(Node node)
    {
        java.util.Iterator i = nodeViews.iterator();
        while (i.hasNext()) {
            LWNode nv = (LWNode) i.next();
            if (nv.getNode() == node)
                return nv;
        }
        return null;
    }
    
    LWComponent findLWComponent(MapItem mapItem)
    {
        java.util.Iterator i = new VueUtil.GroupIterator(nodeViews, linkViews);

        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getMapItem() == mapItem)
                return c;
        }
        return null;
    }

    public void mapItemAdded(MapChangeEvent e)
    {
        MapItem src = e.getItem();
        if (src instanceof Node)
            addNode(new LWNode((Node) src));
        else if (src instanceof Link) {
            Link link = (Link) src;
            LWComponent c1 = findLWComponent(link.getItem1());
            LWComponent c2 = findLWComponent(link.getItem2());
            addLink(new LWLink(link, c1, c2));
        }
        else {
            throw new RuntimeException("unhandled: " + e);
            //fixme: real exception
        }
    }
    public void mapItemRemoved(MapChangeEvent e)
    {
        removeComponent(findLWComponent(e.getItem()));
    }
    
    public void mapItemChanged(MapChangeEvent e)
    {
    }

    public LWComponent addComponent(LWComponent c)
    {
        components.add(c);
        repaint();
        return c;
    }
    public boolean removeComponent(LWComponent c)
    {
        boolean success = components.remove(c);
        repaint();
        return success;
    }
    public LWNode addNode(LWNode nv)
    {
        nodeViews.add(nv);
        repaint();
        return nv;
    }
    public boolean removeNode(LWNode nv)
    {
        boolean success = nodeViews.remove(nv);
        repaint();
        return success;
    }
    public LWLink addLink(LWLink lv)
    {
        repaint();
        linkViews.add(lv);
        return lv;
    }
    public boolean removeLink(LWLink lv)
    {
        boolean success = linkViews.remove(lv);
        repaint();
        return success;
    }

    public LWComponent getLWComponentAt(float x, float y)
    {
        java.util.List hits = new java.util.ArrayList();
        
        java.util.Iterator i = new VueUtil.GroupIterator(components, nodeViews, linkViews);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(x, y))
                hits.add(c);
        }
        return findClosestCenter(hits, x, y);
    }

    public LWComponent findClosestEdge(java.util.List hits, float x, float y)
    {
        return findClosest(hits, x, y, true);
    }
    
    public LWComponent findClosestCenter(java.util.List hits, float x, float y)
    {
        return findClosest(hits, x, y, false);
    }
    
    protected LWComponent findClosest(java.util.List hits, float x, float y, boolean toEdge)
    {
        if (hits.size() == 1)
            return (LWComponent) hits.get(0);
        else if (hits.size() == 0)
            return null;
        
        float shortest = Float.MAX_VALUE;
        float distance;
        LWComponent closest = null;
        java.util.Iterator i = hits.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (toEdge)
                distance = c.distanceToEdge(x, y);
            else
                distance = c.distanceToCenter(x, y);
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
     * accont, etc).
     */
    public Rectangle2D getAllComponentBounds()
    {
        float xMin = Float.MAX_VALUE;
        float yMin = Float.MAX_VALUE;
        float xMax = Float.MIN_VALUE;
        float yMax = Float.MIN_VALUE;
        
        java.util.Iterator i = new VueUtil.GroupIterator(components, nodeViews, linkViews);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float x = c.getX();
            float y = c.getY();
            float mx = x + c.getWidth();
            float my = y + c.getHeight();
            if (x < xMin) xMin = x;
            if (y < yMin) yMin = y;
            if (mx > xMax) xMax = mx;
            if (my > yMax) yMax = my;
        }

        // In case there's nothing in there
        if (xMin == Float.MAX_VALUE) xMin = 0;
        if (yMin == Float.MAX_VALUE) yMin = 0;
        if (xMax == Float.MIN_VALUE) xMax = 0;
        if (yMax == Float.MIN_VALUE) yMax = 0;

        return new Rectangle2D.Float(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    private void drawComponent(LWComponent c, Graphics2D g2)
    {
        // restore default graphics context
        g2.setFont(defaultFont);
        // draw the component
        c.draw(g2);
    }

    public void setIndicated(LWComponent c)
    {
        indication = c;
        c.setIndicated(true);
    }
    public void clearIndicated()
    {
        if (indication != null) {
            indication.setIndicated(false);
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
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // paint the background
        
        Graphics2D g2 = (Graphics2D) g;
        
        g2.translate(-getOriginX(), -getOriginY());
        if (zoomFactor != 1)
            g2.scale(zoomFactor, zoomFactor);

        if (DEBUG_DRAW_ORIGIN) {
            // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (VueUtil.isMacPlatform())
                g2.setStroke(new java.awt.BasicStroke(1.0001f)); // java bug: Mac 1.0 stroke width hardly scales...
            else
                g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(Color.lightGray);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }
        
        /*
         * Draw the components.
         * Draw links first so their ends aren't visible.
         */

        // anti-alias text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // anti-alias shapes by default
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        
        // now render all the LWComponents
        java.util.Iterator i;
        
        // render the LWLinks first so ends are obscured
        i = linkViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed())
                drawComponent(c, g2);
        }
        if (dynamicLink.isDisplayed())
            dynamicLink.draw(g2);

        // render any arbitrary LWComponents
        i = components.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed())
                drawComponent(c, g2);
        }

        // render the LWNodes
        i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed() && c != selection && c != indication)
                drawComponent(c, g2);
        }

        // render the current selection on top if it's a node
        if (selection != null && selection instanceof LWNode)
            drawComponent(selection, g2);
        // render the current indication on top
        if (indication != null)
            drawComponent(indication, g2);

        boolean scaled = true;
        if (DEBUG_DRAW_ORIGIN && zoomFactor >= 6.0) {
            g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
            scaled = false;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(Color.black);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }
        if (DEBUG_SHOW_MOUSE_LOCATION) {
            if (zoomFactor != 1 && scaled)
                g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
            g2.translate(getOriginX(), getOriginY());

            
            // okay, now we're moved back into screen space
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(0.01f));
            g2.drawLine(mouseX,mouseY, mouseX,mouseY);

            int iX = (int) (screenToMapX(mouseX) * 100);
            int iY = (int) (screenToMapY(mouseY) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            g2.setFont(defaultFont);
            g2.drawString("(" + mouseX + "," +  mouseY + ")screen", 10, 20);
            //g2.drawString("(" + mapX + "," +  mapY + ")map", 10, 40);
            g2.drawString("mapX " + mapX, 10, 40);
            g2.drawString("mapY " + mapY, 10, 60);
        }
    }

    
    void setSelection(LWComponent newSelection)
    {
        boolean doPaint = false;
        if (this.selection != null) {
            this.selection.setSelected(false);
            doPaint = true;
        }
        this.selection = newSelection;
        if (newSelection != null) {
            this.selection.setSelected(true);
            repaint();
            new MapSelectionEvent(this, newSelection.getMapItem()).raise();
            //System.out.println("selected: " + newSelection + " " + newSelection.getMapItem());
        }
        if (doPaint)
            repaint();
    }

    // todo temporary
    JPopupMenu mapPopup = null;
    JPopupMenu cPopup = null;
    
    static final int RIGHT_BUTTON_MASK =
        java.awt.event.InputEvent.BUTTON2_MASK
        | java.awt.event.InputEvent.BUTTON3_MASK;
    
    class InputHandler extends tufts.vue.MouseAdapter
        implements java.awt.event.KeyListener
    {
        // temporary tool activators (while the key is held down)
        // They require a further mouse action to actually
        // do anythiing.
        static final int KEY_TOOL_PAN   = KeyEvent.VK_SPACE;
        static final int KEY_TOOL_ZOOM  = KeyEvent.VK_Z;
        static final int KEY_TOOL_ARROW = KeyEvent.VK_A;
        
        // shortcuts -- these immediately do something
        static final int KEY_ZOOM_IN_0  = KeyEvent.VK_EQUALS;
        static final int KEY_ZOOM_IN_1  = KeyEvent.VK_ADD;
        static final int KEY_ZOOM_OUT_0 = KeyEvent.VK_MINUS;
        static final int KEY_ZOOM_OUT_1 = KeyEvent.VK_SUBTRACT;
        static final int KEY_ZOOM_FIT   = KeyEvent.VK_0;
        static final int KEY_ZOOM_ACTUAL= KeyEvent.VK_1;

        LWComponent dragComponent;
        LWComponent linkSource;

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

        static final boolean MOUSE_DEBUG = false;

        private void setCursor(Cursor cursor)
        {
            SwingUtilities.getRootPane(MapViewer.this).setCursor(cursor);
            // could compute cursor-set pane in addNotify
        }
        
        public void keyPressed(KeyEvent e)
        {
            // System.err.println("[" + e.paramString() + "]");

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
                    setCursor(e.isShiftDown() ? CURSOR_ZOOM_OUT : CURSOR_ZOOM_IN);
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

            char c = e.getKeyChar();
            if (e.isControlDown()) {
                // todo: if on a mac, make these
                // Meta chords instead (the apple key)
                switch (key) {
                case KEY_ZOOM_IN_0:
                case KEY_ZOOM_IN_1:
                    setZoomBigger();
                    break;
                case KEY_ZOOM_OUT_0:
                case KEY_ZOOM_OUT_1:
                    setZoomSmaller();
                    break;
                case KEY_ZOOM_ACTUAL:
                    setZoom(1.0);
                    break;
                case KEY_ZOOM_FIT:
                    setZoomFitContent(e.getComponent());
                    repaint();
                    break;
                }
            }
        }
        
        public void keyReleased(KeyEvent e)
        {
            if (toolKeyDown == e.getKeyCode()) {
                toolKeyDown = 0;
                toolKeyEvent = null;
                setCursor(CURSOR_DEFAULT);
            }
        }

        public void keyTyped(KeyEvent e) // not very useful -- has keyChar but no key-code
        {
            // System.err.println("[" + e.paramString() + "]");
        }

        private LWComponent hitComponent = null;
        private Point2D originBeforeDrag;
        public void mousePressed(MouseEvent e)
        {
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");
            
            requestFocus();
            dragStart.setLocation(e.getX(), e.getY());
            
            if (toolKeyDown == KEY_TOOL_PAN) {
                originBeforeDrag = getOriginLocation();
                return;
            } else if (toolKeyDown == KEY_TOOL_ZOOM) {
                setZoomPoint(screenToMapPoint(e.getPoint()));
                if (toolKeyEvent.isShiftDown())
                    setZoomSmaller();
                else
                    setZoomBigger();
                return;
            }
            
            // scale from screen coords to map coords
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            
            this.hitComponent = getLWComponentAt(mapX, mapY);
            if (MOUSE_DEBUG)
                System.err.println("\ton " + hitComponent);

            int mods = e.getModifiers();
            //e.isPopupTrigger()
            // java 1.4.0 bug on PC(w2k): isPopupTrigger isn't true for right-click!

            if ((mods & RIGHT_BUTTON_MASK) != 0 && (mods & java.awt.Event.CTRL_MASK) == 0)
            {
                // this is just example menu code for the moment
                if (mapPopup == null) {
                    mapPopup = new JPopupMenu("Map Menu");
                    mapPopup.add("New Node");
                    mapPopup.add("Fixed");
                    mapPopup.add("Visible");
                    mapPopup.setBackground(Color.gray);
                }
                if (cPopup == null) {
                    cPopup = new JPopupMenu("Item Menu");
                    cPopup.add("Edit");
                    cPopup.add("Rename");
                    cPopup.add("Delete");
                }
                if (hitComponent == null) {
                    mapPopup.show(MapViewer.this, e.getX(), e.getY());
                } else {
                    setSelection(hitComponent);
                    cPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            else if (hitComponent != null)
            {
                // Left (normal) mouse-press on some LWComponent
                
                if (e.isControlDown() || e.isAltDown()) {
                    // Mod-Drag off a component to offer new link creation
                    linkSource = hitComponent;
                    dragOffset.setLocation(0,0);
                    dynamicLink.setSource(linkSource);
                    dynamicLink.setDisplayed(true);
                    dragComponent = dynamicLinkEndpoint;
                } else {
                    setSelection(hitComponent);
                    dragComponent = hitComponent;
                    dragOffset.setLocation(hitComponent.getX() - mapX,
                                           hitComponent.getY() - mapY);
                }
            }
        }
        
        public void mouseMoved(MouseEvent e)
        {
            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
            // Workaround for known Apple Mac OSX Java 1.4.1 bug:
            // Radar #3164718 "Control-drag generates mouseMoved, not mouseDragged"
            if (dragComponent != null)
                mouseDragged(e);
        }

        public void mouseDragged(MouseEvent e)
        {
            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            int screenX = e.getX();
            int screenY = e.getY();
            
            // Stop all dragging if the mouse leaves our component
            if (!e.getComponent().contains(screenX, screenY))
                return;

            if (toolKeyDown == KEY_TOOL_PAN) {
                // drag the entire map
                int dx = dragStart.x - screenX;
                int dy = dragStart.y - screenY;
                setMapOriginOffset(originBeforeDrag.getX() + dx,
                                   originBeforeDrag.getY() + dy);
                repaint();
                return;
            }

            float mapX = screenToMapX(screenX);
            float mapY = screenToMapY(screenY);
            
            if (dragComponent != null)
                dragComponent.setLocation((float) (mapX + dragOffset.getX()),
                                          (float) (mapY + dragOffset.getY()));
            
            if (linkSource != null) {
                // we're dragging a new link looking for an
                // allowable endpoint
                LWComponent over = findLWLinkTargetAt((int)mapX, (int)mapY);
                if (indication != null && indication != over) {
                    // we've moved off old indicated -- clear it
                    clearIndicated();
                }
                if (over != null && validLinkTarget(over)) {
                    setIndicated(over);
                }
            }
            if (linkSource != null || dragComponent != null)
                repaint();
        }

        public void mouseReleased(MouseEvent e)
        {
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + "]");
            
            if (linkSource != null) {
                dynamicLink.setDisplayed(false);
                LWComponent linkDest = indication;
                if (linkDest != null && linkDest != linkSource)
                {
                    LWLink lwl = linkDest.getLinkTo(linkSource);
                    if (lwl != null) {
                        // There's alreay a link tween these two -- increment the weight
                        lwl.getLink().incrementWeight();
                    } else {
                        getMap().addLink(new Link(linkSource.getMapItem(), linkDest.getMapItem()));
                    }
                }
                linkSource = null;
            }
            dragComponent = null;
            if (indication != null)
                clearIndicated();
            repaint();
        }

        private final boolean isDoubleClickEvent(MouseEvent e)
        {
            return e.getClickCount() == 2
                && (e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0;
        }
        
        public void mouseClicked(MouseEvent e)
        {
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");

            if (isDoubleClickEvent(e) && toolKeyDown == 0) {
                if (hitComponent instanceof LWNode) {
                    Resource resource = ((LWNode)hitComponent).getNode().getResource();
                    if (resource != null) {
                        // todo: some kind of animation or something to show
                        // we're "opening" this node -- maybe an indication
                        // flash -- we'll need another thread for that.
                        System.err.println("opening resource for: " + hitComponent);
                        resource.displayContent();
                    }
                }
            }
        }

        public LWComponent findLWLinkTargetAt(int x, int y)
        {
            LWComponent directHit = getLWComponentAt(x, y);
            if (directHit != null)
                return directHit;
            
            java.util.List targets = new java.util.ArrayList();
            java.util.Iterator i = new VueUtil.GroupIterator(nodeViews, linkViews);
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.targetContains(x, y) && validLinkTarget(c))
                    targets.add(c);
            }
            return findClosestEdge(targets, x, y);
        }
    
        public boolean validLinkTarget(LWComponent linkTarget)
        {
            if (linkTarget == linkSource) {
                return false;
            } else if (linkTarget instanceof LWLink) {
                LWLink lwl = (LWLink) linkTarget;
                return lwl.getComponent1() != linkSource
                    && lwl.getComponent2() != linkSource;
            } else
                return true;
        }
        
    }


    private Frame parentFrame = null;
    // todo: ultimately make this an event a parent Container could honor
    void setTitleMessage(String s)
    {
        String title = "VUE: " + map.getLabel();
        if (s != null)
            title += " [" + s + "]";
        if (parentFrame != null)
            parentFrame.setTitle(title);
    }
    
    public void addNotify()
    {
        super.addNotify();
        Component c = this.getParent();
        while (c != null && !(c instanceof Frame))
            c = c.getParent();
        this.parentFrame = (Frame) c;
        requestFocus();
    }

    public static void main(String[] args) {
        /*
         * create an example map
         */
        tufts.vue.ConceptMap map = new tufts.vue.ConceptMap("Example Map");
        
        /*
         * create the viewer
         */
        //JComponent mapView = new MapViewer(map);
        //mapView.setPreferredSize(new Dimension(400,300));
        Container mapView = new tufts.vue.MapViewer(map);

        installExampleMap(map);
        
        /*
         * create a an application frame with some components
         */

        JFrame frame = new JFrame("VUE Concept Map Viewer");
        frame.setContentPane(mapView);
        
        //Frame frame = new Frame("VUE Map Viewer: " + map.getLabel());
        //frame.add(mapView);
        
        /*
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mapView, BorderLayout.CENTER);
        frame.getContentPane().add(mii = new MapItemInspector(), BorderLayout.WEST);
        */
        /*
        JFrame iframe = new JFrame("Inspector");
        iframe.setContentPane(new MapItemInspector());
        iframe.show();
        */
        
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
        frame.repaint(); // currently needed to compute text sizes
    }

    static void installExampleMap(ConceptMap map)
    {
        /*
         * create some test nodes & links
         */
        Node n1 = new Node("Test node1");
        Node n2 = new Node("Test node2");
        Node n3 = new Node("foo.txt", new Resource("/tmp/foo.txt"));
        Node n4 = new Node("Tester Node Four");
        Node n5 = new Node("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        Node n6 = new Node("abcdefghijklmnopqrstuvwxyz");
        n1.setPosition(100, 50);
        n2.setPosition(100, 100);
        n3.setPosition(50, 150);
        n4.setPosition(150, 150);
        n5.setPosition(150, 200);
        n6.setPosition(150, 250);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addNode(n5);
        map.addNode(n6);
        map.addLink(new Link(n1, n2));
        map.addLink(new Link(n2, n3));
        map.addLink(new Link(n2, n4));
    }
}
