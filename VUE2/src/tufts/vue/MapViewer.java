package tufts.vue;

import java.awt.*;
import java.awt.event.*;
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
    static Font smallFont = new Font("SansSerif", Font.PLAIN, 11);// todo: prefs
    
    static final Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static final Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    static final Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    static final Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    static final Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    java.util.List components = new java.util.ArrayList();
    java.util.List nodeViews = new java.util.ArrayList();
    java.util.List linkViews = new java.util.ArrayList();

    protected ConceptMap map;            // the map we're displaying & interacting with
    protected LWComponent selection;    // current selection
    protected LWComponent indication;   // current indication (rollover hilite)

    private final LWComponent dynamicLinkEndpoint = new LWComponent();
    private final LWLink dynamicLink = new LWLink(dynamicLinkEndpoint);

    private double zoomFactor = 1.0;
    private double zoomMapper = 1.0 / zoomFactor;

    //-------------------------------------------------------
    // temporary debugging attributes
    private final boolean DRAW_ORIGIN = true;
    private final boolean SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
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

        //setSize(300,200);
        setPreferredSize(new Dimension(400,300));
        setBackground(Color.white);
        setFont(defaultFont);

        loadMap(map);
        requestFocus();
    }

    /**
     * Set's the viewport such that the upper left corner
     * displays mapX, mapY.
     */
    public void setViewportOrigin(int mapX, int mapY)
    {
        map.dSetOrigin(-mapX, -mapY);
    }
    public void setViewportOrigin(float mapX, float mapY)
    {
        map.dSetOrigin((int) -mapX, (int) -mapY);
    }
    
    private final int ZOOM_ACTUAL = 10;
    private final int ZOOM_MANUAL = -1;
    private final double[] ZoomDefaults = {
        1.0/32, 1.0/24, 1.0/16, 1.0/12, 1.0/8, 1.0/6, 1.0/4, 1.0/3, 1.0/2, 2.0/3, 0.75,
        1,
        1.25, 1.5, 2, 3, 4, 6, 8,
        12, 16, 24, 32, 48, 64 // illustrator uses these, may be overkill for us
    };
    private int curZoom = ZOOM_ACTUAL;

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
    
    private void setZoom(double zoomFactor, boolean centerZoom)
    {
        curZoom = ZOOM_MANUAL;
        for (int i = 0; i < ZoomDefaults.length; i++) {
            if (zoomFactor == ZoomDefaults[i]) {
                curZoom = i;
                break;
            }
        }
        
        // center the viewport on the new zoomed area
        
        if (centerZoom) {
            double viewportWidth = getWidth();
            double viewportHeight = getHeight();
            double oldViewportSpanWidth = viewportWidth / this.zoomFactor;
            double oldViewportSpanHeight = viewportHeight / this.zoomFactor;
            double newViewportSpanWidth = viewportWidth / zoomFactor;
            double newViewportSpanHeight = viewportHeight / zoomFactor;

            // todo fixme: this is fucked up and
            // is depending wrongly on zoom

            double viewportX = -map.dGetOriginX();
            double viewportY = -map.dGetOriginY();
            viewportX += (oldViewportSpanWidth - newViewportSpanWidth) / 2;
            viewportY += (oldViewportSpanHeight - newViewportSpanHeight) / 2;
            setViewportOrigin((float)viewportX, (float)viewportY);
        }
        

        this.zoomFactor = zoomFactor;
        this.zoomMapper = 1.0 / zoomFactor;

        // Rond the display value down to 2 digits
        int displayZoom = (int) (zoomFactor * 10000.0);
        setTitleMessage( (((float) displayZoom) / 100f) + "%");

        repaint();
    }

    public double getZoom()
    {
        return this.zoomFactor;
    }

    public void reshape(int x, int y, int w, int h)
    {
        //System.out.println("reshape");
        super.reshape(x,y, w,h);
        repaint(100);
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

    // This overrides java.awt.Container.locate(x,y)
    // todo perf: potential optimization
    // public Component locate(int x, int y) { return this; }
    
    public LWComponent getLWComponentAt(int x, int y)
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

    public LWComponent findClosestEdge(java.util.List hits, int x, int y)
    {
        return findClosest(hits, x, y, true);
    }
    
    public LWComponent findClosestCenter(java.util.List hits, int x, int y)
    {
        return findClosest(hits, x, y, false);
    }
    
    protected LWComponent findClosest(java.util.List hits, int x, int y, boolean toEdge)
    {
        if (hits.size() == 1)
            return (LWComponent) hits.get(0);
        else if (hits.size() == 0)
            return null;
        
        double shortest = Double.MAX_VALUE;
        double distance;
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
    public Rectangle getBounds()
    {
        int xMin = Integer.MAX_VALUE;
        int yMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMax = Integer.MIN_VALUE;
        
        java.util.Iterator i = new VueUtil.GroupIterator(components, nodeViews, linkViews);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            int x = c.getX();
            int y = c.getY();
            int mx = x + c.getWidth();
            int my = y + c.getHeight();
            if (x < xMin) xMin = x;
            if (y < yMin) yMin = y;
            if (mx > xMax) xMax = mx;
            if (my > yMax) yMax = my;
        }

        // In case there's nothing in there
        if (xMin == Integer.MAX_VALUE) xMin = 0;
        if (yMin == Integer.MAX_VALUE) yMin = 0;
        if (xMax == Integer.MIN_VALUE) xMax = 0;
        if (yMax == Integer.MIN_VALUE) yMax = 0;

        return new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin);
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
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); // paint the background
        
        Graphics2D g2 = (Graphics2D) g;
        
        if (DRAW_ORIGIN) {
            g.setColor(Color.lightGray);
            g.drawLine(Integer.MIN_VALUE, map.dGetOriginY(),
                       Integer.MAX_VALUE, map.dGetOriginY());
            g.drawLine(map.dGetOriginX(), Integer.MIN_VALUE,
                       map.dGetOriginX(), Integer.MAX_VALUE);
        }
        
        g2.translate(map.dGetOriginX(), map.dGetOriginY());
        if (zoomFactor != 1)
            g2.scale(zoomFactor, zoomFactor);

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

        if (SHOW_MOUSE_LOCATION) {
            if (zoomFactor != 1)
                g2.scale(zoomMapper, zoomMapper);
            g2.translate(-map.dGetOriginX(), -map.dGetOriginY());
            // okay, now we're moved back into screen space
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(0.01f));
            g2.drawLine(mouseX,mouseY, mouseX,mouseY);
            int mapX = mouseX - map.dGetOriginX();
            int mapY = mouseY - map.dGetOriginY();
            mapX *= zoomMapper;
            mapY *= zoomMapper;
            g2.setFont(defaultFont);
            g2.drawString("(" + mouseX + "," +  mouseY + ")screen", 10, 20);
            g2.drawString("(" + mapX + "," +  mapY + ")map", 10, 40);
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
        static final int KEY_DRAG_MAP = KeyEvent.VK_SPACE;
        
        static final int KEY_ZOOM_IN_0 = KeyEvent.VK_EQUALS;
        static final int KEY_ZOOM_IN_1 = KeyEvent.VK_ADD;
        static final int KEY_ZOOM_OUT_0 = KeyEvent.VK_MINUS;
        static final int KEY_ZOOM_OUT_1 = KeyEvent.VK_SUBTRACT;
        static final int KEY_ZOOM_FIT = KeyEvent.VK_0;
        static final int KEY_ZOOM_ACTUAL = KeyEvent.VK_1;

        /*
        static final char CHAR_DRAG_MAP = ' ';
        static final char CHAR_ZOOM_IN = '+';
        static final char CHAR_ZOOM_OUT = '-';
        */

        LWComponent dragComponent;
        LWComponent linkSource;
        int dragXoffset;
        int dragYoffset;
        boolean dragMapKeyDown = false;

        static final boolean MOUSE_DEBUG = false;
        
        public void keyPressed(KeyEvent e)
        {
            // System.err.println("[" + e.paramString() + "]");

            // FYI, Java 1.4.1 sends repeat key press events for
            // non-modal keys that are being held down (e.g. not for
            // shift, buf for spacebar)

            int key = e.getKeyCode();
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
                case KEY_ZOOM_FIT:
                    Rectangle bounds = getBounds();
                    Component viewport = e.getComponent();
                    System.err.println("viewport="+viewport);
                    int vw = viewport.getWidth() - 20;
                    int vh = viewport.getHeight() - 20;
                    double vertZoom = (double) vh / bounds.height;
                    double horzZoom = (double) vw / bounds.width;
                    System.err.println("bounds="+bounds + " vertZoom="+vertZoom + " horzZoom="+horzZoom);
                    // this is working, except the setting of the origin
                    // is being affected by the the scaling
                    setViewportOrigin(bounds.x, bounds.y);
                    setZoom(horzZoom < vertZoom ? horzZoom : vertZoom, false);
                    repaint();
                    break;
                case KEY_ZOOM_ACTUAL:
                    setZoom(1.0);
                    break;
                }
            } else {
                switch (key) {
                case KEY_DRAG_MAP:
                    if (!dragMapKeyDown)
                        SwingUtilities.getRootPane(e.getComponent()).setCursor(CURSOR_HAND);
                    dragMapKeyDown = true;
                    break;
                }
            }
        }
        
        public void keyReleased(KeyEvent e)
        {
            if (e.getKeyCode() == KEY_DRAG_MAP) {
                if (dragMapKeyDown)
                    SwingUtilities.getRootPane(e.getComponent()).setCursor(CURSOR_DEFAULT);
                dragMapKeyDown = false;
            }
        }

        public void keyTyped(KeyEvent e) // useless -- has keyChar but no key-code
        {
            // System.err.println("[" + e.paramString() + "]");
        }

        private LWComponent hitComponent = null;
        public void mousePressed(MouseEvent e)
        {
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");
            
            requestFocus();
            
            int x = e.getX() - map.dGetOriginX();
            int y = e.getY() - map.dGetOriginY();
            if (zoomMapper != 1) { 
                x *= zoomMapper;
                y *= zoomMapper;
            }

            if (dragMapKeyDown) {
                dragXoffset = x;
                dragYoffset = y;
                return;
            }
            
            this.hitComponent = getLWComponentAt(x, y);
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
                if (e.isControlDown() || e.isAltDown()) {
                    // If we drag after this, it's to create a link.
                    linkSource = hitComponent;
                    dragXoffset = 0;
                    dragYoffset = 0;
                    dynamicLink.setSource(linkSource);
                    dynamicLink.setDisplayed(true);
                    dragComponent = dynamicLinkEndpoint;
                } else {
                    setSelection(hitComponent);
                    dragComponent = hitComponent;
                    dragXoffset = hitComponent.getX() - x;
                    dragYoffset = hitComponent.getY() - y;
                }
            } else {
                dragXoffset = x;
                dragYoffset = y;
            }
        }
        
        public void mouseMoved(MouseEvent e)
        {
            if (SHOW_MOUSE_LOCATION) {
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
            if (SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            if (MOUSE_DEBUG) System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            int x = e.getX();
            int y = e.getY();
            
            // Stop all dragging if the mouse leaves our component
            if (!e.getComponent().contains(x, y))
                return;

            
            if (dragMapKeyDown) {
                // todo: fixme
                x *= zoomMapper;
                y *= zoomMapper;
                map.dSetOrigin(x - (int)(dragXoffset*zoomFactor), y - (int)(dragYoffset*zoomFactor));
                repaint();
                return;
            }

            // translate to map origin
            x -= map.dGetOriginX();
            y -= map.dGetOriginY();
            if (zoomMapper != 1) {
                x *= zoomMapper;
                y *= zoomMapper;
            }
            
            if (dragComponent != null) {
                dragComponent.setLocation(x + dragXoffset, y + dragYoffset);
            }
            
            if (linkSource != null) {
                // we're dragging a new link looking for an
                // allowable endpoint
                LWComponent over = findLWLinkTargetAt(x, y);
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

            if (isDoubleClickEvent(e)) {
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
