package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.text.JTextComponent;

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
    implements VueConstants
               , MapListener
               //, AWTEventListener
{
    java.util.List components = new java.util.ArrayList();
    java.util.List nodeViews = new java.util.ArrayList();
    java.util.List linkViews = new java.util.ArrayList();
    java.util.List tools = new java.util.ArrayList();

    protected ConceptMap map;                   // the map we're displaying & interacting with
    private MapTextEdit activeTextEdit;          // Current on-map text edit

    //-------------------------------------------------------
    // Selection support
    //-------------------------------------------------------
    protected LWComponent lastSelection;   // the most recently selected component
    protected LWComponent justSelected;    // temporary lastSelection for between mouse press&click
    protected Rectangle draggedSelectionBox;     // currently dragged selection box
    protected boolean draggingSelectionBox;     // currently dragged selection box
    protected Rectangle2D selectionBounds;  // max bounds of all components in current selection
    protected java.util.List selectionList = new java.util.ArrayList();
    //protected LWComponent selection;    // current selection

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

    //-------------------------------------------------------
    // temporary debugging stuff
    //-------------------------------------------------------
    private boolean DEBUG_SHOW_ORIGIN = false;
    private boolean DEBUG_SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
    private boolean DEBUG_KEYS = false;
    private boolean DEBUG_MOUSE = false;
    private int mouseX;
    private int mouseY;
    //-------------------------------------------------------

    private VueTool activeTool;
    private ZoomTool zoomTool; // todo: get rid of this hard reference
    
    public MapViewer(ConceptMap map)
    {
        super(false); // turn off double buffering -- frame seems handle it?
        setOpaque(true);
        creationLink.setDisplayed(false);
        //setLayout(new NoLayout());
        setLayout(null);
        InputHandler ih = new InputHandler();
        addMouseListener(ih);
        addMouseMotionListener(ih);
        addKeyListener(ih);

        MapDropTarget mapDropTarget = new MapDropTarget(this, map);
        this.setDropTarget(new java.awt.dnd.DropTarget(this, mapDropTarget));

        // todo: tab to show/hide all tool windows
        addTool(this.zoomTool = new ZoomTool(this));
        
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
        setFont(VueConstants.DefaultFont);

        loadMap(map);

        /*
        Toolkit.getDefaultToolkit().addAWTEventListener(this,
                                                        AWTEvent.INPUT_METHOD_EVENT_MASK
                                                        | AWTEvent.TEXT_EVENT_MASK
                                                        | AWTEvent.MOUSE_EVENT_MASK);
        */
    }

    public boolean isEmpty()
    {
        return components.size() + linkViews.size() + nodeViews.size() == 0;
    }
      

    
    void addTool(VueTool tool)
    {
        tools.add(tool);
    }

    /**
     * Set's the viewport such that the upper left corner
     * displays at screenX, screenY.  Is precision
     * due to possibility of zooming.
     */
    
    public void setMapOriginOffset(float screenX, float screenY)
    {
        this.mapOriginX = screenX;
        this.mapOriginY = screenY;
        this.map.setOrigin(mapOriginX, mapOriginY);
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
        /*
        screenRect.x = (int) Math.round(mapRect.getX() * zoomFactor - getOriginX());
        screenRect.y = (int) Math.round(mapRect.getY() * zoomFactor - getOriginY());
        screenRect.width = (int) Math.round(mapRect.getWidth() * zoomFactor);
        screenRect.height = (int) Math.round(mapRect.getHeight() * zoomFactor);
        */
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

    public void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        new MapViewerEvent(this, MapViewerEvent.ZOOM).raise();
        repaint();
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
        super.reshape(x,y, w,h);
        repaint(250);
        requestFocus();
        new MapViewerEvent(this, MapViewerEvent.PAN).raise();
        // may be causing problems on mac --
        // some components in tabbed is getting a reshape call
        // when switching tabs
    }

    public ConceptMap getMap()
    {
        return this.map;
    }

    void loadMap(ConceptMap map)
    {
        if (map == null)
            return;
        if (this.map != null)
            unloadMap();
        this.map = map;

        java.util.Iterator i;
        i = map.getNodeIterator();
        while (i.hasNext()) {
            Node node = (Node) i.next();
            System.out.println("loaded " + node);
            addNode(new LWNode(node));
        }

        i = map.getLinkIterator();        
        while (i.hasNext()) {
            Link link = (Link) i.next();
            //System.out.println("loaded " + l + " "+findLWComponent(l.getItem1())+" "+l.getItem2());
            System.out.println("loaded " + link);
            LWComponent c1 = findLWComponent(link.getItem1());
            LWComponent c2 = findLWComponent(link.getItem2());
            addLink(new LWLink(link, c1, c2));
        }
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

    public void mapItemAdded(MapEvent e)
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
    
    public void mapItemRemoved(MapEvent e)
    {
        removeLWC(findLWComponent(e.getItem()));
    }
    
    public void mapItemChanged(MapEvent e)
    {
        System.out.println("MapViewer repaint on " + e);
        repaint();
    }

    private LWNode addNode(LWNode nv)
    {
        nodeViews.add(nv);
        repaint();
        return nv;
    }
    private LWLink addLink(LWLink lv)
    {
        linkViews.add(lv);
        repaint();
        return lv;
    }
    LWComponent addLWC(LWComponent c)
    {
        if (c instanceof LWNode)
            nodeViews.add(c);
        else if (c instanceof LWLink)
            linkViews.add(c);
        else
            components.add(c);
        repaint();
        return c;
    }
    private boolean removeLWC(LWComponent c)
    {
        boolean success = false;
        if (c instanceof LWNode)
            success = nodeViews.remove(c);
        else if (c instanceof LWLink)
            success = linkViews.remove(c);
        else
            success = components.remove(c);
        repaint();
        return success;
    }
    /*
    public LWComponent addComponent(LWComponent c)
    {
        components.add(c);
        repaint();
        return c;
    }
    public boolean removeNode(LWNode nv)
    {
        boolean success = nodeViews.remove(nv);
        repaint();
        return success;
    }
    public boolean removeLink(LWLink lv)
    {
        boolean success = linkViews.remove(lv);
        repaint();
        return success;
    }
    */

    public LWComponent getLWComponentAt(float mapX, float mapY)
    {
        java.util.List hits = new java.util.ArrayList();
        
        java.util.Iterator i = new VueUtil.GroupIterator(components, nodeViews, linkViews);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(mapX, mapY)) {
                hits.add(c);
                //hits.add(c.getComponentAt(mapX, mapY));
                // LWC.getComponentAt(x,y) returns self
                // if no child hits
            }
        }
        return findClosestCenter(hits, mapX, mapY);
    }

    public LWNode getLWNodeAt(float mapX, float mapY)
    {
        java.util.List hits = new java.util.ArrayList();
        
        java.util.Iterator i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(mapX, mapY))
                hits.add(c);
        }
        return (LWNode) findClosestCenter(hits, mapX, mapY);
    }
    /**
     * Used to look for a node we can drop the given
     * node onto.
     */
    public LWNode getLWNodeUnder(float mapX, float mapY, LWComponent dragging)
    {
        java.util.List hits = new java.util.ArrayList();
        //float centerX = dragging.getCenterX();
        //float centerY = dragging.getCenterY();
        java.util.Iterator i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // if either mouse or the center of the dragged component
            // is within the bounds of the component, consider it
            // a possible drop target (to add dragging as a child)
            //if (c != dragging && (c.contains(mapX, mapY) || c.contains(centerX, centerY)))
            if (c != dragging && c.contains(mapX, mapY))
                hits.add(c);
        }
        return (LWNode) findClosestCenter(hits, mapX, mapY);
    }
    
    public java.util.List getLWComponentsHitBy(Rectangle2D mapRect)
    {
        java.util.List hits = new java.util.ArrayList();
        
        java.util.Iterator i = new VueUtil.GroupIterator(components, nodeViews);
        //java.util.Iterator i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.intersects(mapRect))
                hits.add(c);
        }
        if (hits.size() == 0) {
            i = linkViews.iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
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
    
    public LWComponent findClosestCenter(java.util.List hits, float x, float y)
    {
        return findClosest(hits, x, y, false);
    }
    
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
        }

        float shortest = Float.MAX_VALUE;
        float distance;
        LWComponent closest = null;
        i = topLayer.iterator();
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
     * account, etc).
     */
    public Rectangle2D getAllComponentBounds()
    {
        return getComponentBounds(new VueUtil.GroupIterator(components, nodeViews, linkViews));
    }
    
    public Rectangle2D getComponentBounds(java.util.Iterator i)
    {
        float xMin = Float.POSITIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY;
        float xMax = Float.NEGATIVE_INFINITY;
        float yMax = Float.NEGATIVE_INFINITY;
        
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
        if (xMin == Float.POSITIVE_INFINITY) xMin = 0;
        if (yMin == Float.POSITIVE_INFINITY) yMin = 0;
        if (xMax == Float.NEGATIVE_INFINITY) xMax = 0;
        if (yMax == Float.NEGATIVE_INFINITY) yMax = 0;

        return new Rectangle2D.Float(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    public void setIndicated(LWComponent c)
    {
        if (indication != c) {
            clearIndicated();
            indication = c;
            c.setIndicated(true);
        }
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
        // paint the background
        Rectangle r = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(r.x, r.y, r.width, r.height);
        //super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
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
        
        paintLWComponents(g2);
        //super.paintChildren(g2);
        
        // Restore us to raw screen coordinates & turn off
        // anti-aliasing to draw selection(s)
        
        if (zoomFactor != 1)
            g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
        g2.translate(getOriginX(), getOriginY());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        paintSelection(g2);

        if (DEBUG_SHOW_ORIGIN && zoomFactor >= 6.0) {
            g2.translate(-getOriginX(), -getOriginY());
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(Color.black);
            g2.draw(Xaxis);
            g2.draw(Yaxis);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //setOpaque(false);
        //super.paintComponent(g);
        super.paintChildren(g2);
        //setOpaque(true);
        
    }

    protected void paintChildren(Graphics g) {
        // super.paint() will call this, and
        // we want it to do nothing because
        // we need to invoke this ourself
    }

    class MapTextEdit extends JTextField
        implements ActionListener
                   , KeyListener
                   , FocusListener
    {
        LWComponent lwc;
        
        MapTextEdit(LWComponent lwc)
        {
            super(lwc.getMapItem().getLabel());
            if (getColumns() < 4)
                setColumns(4);
            this.lwc = lwc;
            addActionListener(this);
            addFocusListener(this);
            addKeyListener(this);
            Font baseFont = lwc.getFont();
            int pointSize = (int) (baseFont.getSize() * zoomFactor * lwc.getScale());
            if (pointSize < 10)
                pointSize = 10;
            Font f = new Font(baseFont.getName(), baseFont.getStyle(), pointSize);
            
            //System.out.println(DefaultFont.getAttributes());
            setFont(f);
            FontMetrics fm = getFontMetrics(f);
            //System.out.println("margin="+getMargin());
            Dimension prefSize = getPreferredSize();

            int prefWidth = mapToScreenDim(lwc.getWidth()-14);
            int textWidth = fm.stringWidth(getText()) + 4; // add 4 for borders
            if (prefWidth < textWidth)
                prefWidth = textWidth;
            if (prefWidth < 50)
                prefWidth = 50;
            if (prefSize.width < prefWidth)
                prefSize.width = prefWidth;
            prefSize.height = fm.getAscent() + fm.getDescent();
            setSize(prefSize);
            setSelectionColor(Color.yellow);
            //setSelectionColor(SystemColor.textHighlight);
            selectAll();

            /*
            System.out.println("Actions supported:");
            Object[] actions = getActions();
            for (int i=0; i < actions.length; i++) {
                System.out.println("\t" + i + " " + actions[i]);
            }
            */
        }

        public void actionPerformed(ActionEvent e)
        {
            //System.out.println("MapTextEdit " + e);
            lwc.getMapItem().setLabel(e.getActionCommand());
            removeLabelEdit();
        }

        public void keyPressed(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                removeLabelEdit();
        }
        public void keyReleased(KeyEvent e) {}
        public void keyTyped(KeyEvent e) {}
        
        public void focusLost(FocusEvent e)
        {
            removeLabelEdit();
        }
        public void focusGained(FocusEvent e) { }
    
    
        public void X_paint(Graphics g)
        {
            //System.out.println("paint mtf");
            ((Graphics2D)g).scale(zoomFactor, zoomFactor);
            super.paint(g);
            ((Graphics2D)g).scale(1/zoomFactor, 1/zoomFactor);
        }

        /*
        private void removeMouseListeners()
        {
            // We may want to use this if we allow zooming while a
            // real JComponent is displayed -- we can handle repaints
            // just by scaling the Graphics in paint methods, but to
            // handle an interactive component, we'd need to have the
            // parent retarget mouse events based on the zoomed screen
            // size of the object.  If we go for that, we'd ideally
            // just override the add(Component) method on the panel,
            // and have it insert itself as a relay-proxy for mouse
            // events for any components that are listening for
            // them. (the retargeting code should be easy).
            // (Actually, setting an interceptor glassPane is probably
            // an even cleaner way to do this).
            
            MouseListener[] ml = getMouseListeners();
            MouseMotionListener[] mml = getMouseMotionListeners();
            for (int i = 0; i < ml.length; i++) {
                System.out.println("Removing MouseListener " + ml[i].getClass() + " " + ml[i]);
                removeMouseListener(ml[i]);
            }
            for (int i = 0; i < mml.length; i++) {
                System.out.println("Removing MouseMotionListener " + mml[i].getClass() + " " + mml[i]);
                removeMouseMotionListener(mml[i]);
            }
        }
        */


    }
    
    void removeLabelEdit()
    {
        if (activeTextEdit != null) {
            remove(activeTextEdit);
            activeTextEdit = null;
            repaint();
            requestFocus();
        }
    }

    void activateLabelEdit(LWComponent lwc)
    {
        if (lwc.getMapItem() == null)
            return;
        if (activeTextEdit != null && activeTextEdit.lwc == lwc)
            return;
        removeLabelEdit();
        activeTextEdit = new MapTextEdit(lwc);

        float ew = screenToMapDim(activeTextEdit.getWidth());
        float eh = screenToMapDim(activeTextEdit.getHeight());
        float cx = lwc.getX() + (lwc.getWidth() - ew) / 2f;
        float cy;
        if (lwc.isAutoSized() && !lwc.hasChildren())
            cy = lwc.getY() + (lwc.getHeight() - eh) / 2f;
        else
            cy = lwc.getLabelY();
        
        activeTextEdit.setLocation(mapToScreenX(cx), mapToScreenY(cy));
        // we must add the component to the container in order to get events.
        // super.paintChildren is called in paintComponent only to handle
        // the case where a field like this is active on the panel.
        // Note that this component only simulates zoom by scaling it's font,
        // so we must not zoom the panel while this component is active.
        add(activeTextEdit);
        activeTextEdit.requestFocus();
    }
    
    private void drawComponentList(Graphics2D g2, java.util.List componentList)
    {
        java.util.Iterator i = componentList.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed() && c != indication && !c.isChild())
                drawComponent(g2, c);
        }
    }
    
    private void drawComponent(Graphics2D g2, LWComponent c)
    {
        final boolean translate = !c.absoluteDrawing();
        try {
            Graphics2D lg = (Graphics2D) g2.create();
            if (translate)
                lg.translate(c.getX(), c.getY());
            c.draw(lg);
        } catch (Throwable e) {
            System.err.println("Render exception: " + e);
            e.printStackTrace();
        }
    }

        
    private boolean firstPaint = true;
    void paintLWComponents(Graphics2D g2)
    {
        // anti-alias shapes by default
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // anti-alias text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Do we need fractional metrics?  Gives us slightly more accurate
        // string widths on noticable on long strings
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        /*
         * Draw the components.
         * Draw links first so their ends aren't visible.
         */

        if (firstPaint) {
            firstPaint = false;
            // render nodes so links can compute their location
            drawComponentList(g2, nodeViews);
        }
        
        // render the LWLinks first so ends are obscured later
        // when the LWNodes are rendered.
        //drawComponentList(g2, linkViews);
        
        // render any arbitrary LWComponents
        drawComponentList(g2, components);
        
        // render the LWNodes
        drawComponentList(g2, nodeViews);

        // render the LWLinks LAST so sure to see links to child nodes
        drawComponentList(g2, linkViews);
        
        if (creationLink.isDisplayed())
            drawComponent(g2, creationLink);

        // render the current selection on top
        //if (lastSelection != null && selectionList.size() == 1)
        //  drawComponent(g2, lastSelection);
        
        // render the current indication on top
        if (indication != null)
            drawComponent(g2, indication);

    }


    void paintSelection(Graphics2D g2)
    {
        if (DEBUG_SHOW_MOUSE_LOCATION) {
            // okay, now we're moved back into screen space
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(0.01f));
            g2.drawLine(mouseX,mouseY, mouseX,mouseY);

            int iX = (int) (screenToMapX(mouseX) * 100);
            int iY = (int) (screenToMapY(mouseY) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            g2.setFont(VueConstants.DefaultFont);
            g2.drawString("(" + mouseX + "," +  mouseY + ")screen", 10, 20);
            //g2.drawString("(" + mapX + "," +  mapY + ")map", 10, 40);
            g2.drawString("mapX " + mapX, 10, 40);
            g2.drawString("mapY " + mapY, 10, 60);
            g2.drawString("zoom " + getZoomFactor(), 10, 80);
        }

        if (selectionList.size() > 0) {
            g2.setColor(COLOR_SELECTION);
            g2.setStroke(STROKE_SELECTION);
            java.util.Iterator it = selectionList.iterator();
            while (it.hasNext()) {
                LWComponent c = (LWComponent) it.next();
                drawComponentSelectionBox(g2, c);
            }
        }
        
        if (draggedSelectionBox != null) {
            // draw the selection drag box
            // todo: consider doing this on the glass pane for speed?
            g2.setXORMode(COLOR_SELECTION_DRAG);
            g2.setStroke(STROKE_SELECTION_DYNAMIC);
            g2.drawRect(draggedSelectionBox.x, draggedSelectionBox.y,
                        draggedSelectionBox.width, draggedSelectionBox.height);
        } else if (selectionBounds != null) {
            g2.setStroke(new java.awt.BasicStroke(1f));
            //System.out.println("mapSelectionBounds="+selectionBounds);
            Rectangle sb = mapToScreenRect(selectionBounds);
            //System.out.println("screenSelectionBounds="+sb);
            drawSelectionBox(g2, sb);
        }

    }

    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final int SelectionHandleSize = 5; // selection handle fill size -- todo: config
    // exterior drawn box will be 1 pixel bigger
    private void drawSelectionHandle(Graphics2D g, int x, int y)
    {
        SelectionHandle.setFrame(x, y, SelectionHandleSize, SelectionHandleSize);
        g.setColor(Color.white);
        g.fill(SelectionHandle);
        g.setColor(COLOR_SELECTION);
        g.draw(SelectionHandle);
    }
    void drawSelectionBox(Graphics2D g, Rectangle r)
    {
        g.draw(r);
        r.x -= SelectionHandleSize/2;
        r.y -= SelectionHandleSize/2;

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
    void drawComponentSelectionBox(Graphics2D g, LWComponent c)
    {
        final int chs = 3; // component handle size -- todo: config
        Rectangle r = mapToScreenRect(c.getBounds());
        g.setColor(COLOR_SELECTION);
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
        
    }


    void addToSelection(LWComponent c)
    {
        if (!c.isSelected()) {
            c.setSelected(true);
            selectionList.add(c);
            lastSelection = c;
            justSelected = c;
        }
    }
    void removeFromSelection(LWComponent c)
    {
        c.setSelected(false);
        selectionList.remove(c);
        if (lastSelection == c)
            lastSelection = null;
    }
    void clearSelection()
    {
        selectionBounds = null;
        java.util.Iterator i = selectionList.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setSelected(false);
        }
        selectionList.clear();
        lastSelection = null;
    }

    void setSelection(LWComponent c)
    {
        //if (selectionList.size() == 1 && selectionList.get(0) == lastSelection)
        //  return;
        clearSelection();
        addToSelection(c);
    }
    
    // todo temporary
    JPopupMenu mapPopup = null;
    JPopupMenu cPopup = null;
    
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
            if (DEBUG_KEYS) System.err.println("[" + e.paramString() + "]");

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

            if (key == KeyEvent.VK_F2 && lastSelection instanceof LWNode) {
                activateLabelEdit(lastSelection);
                return;
            }
            
            if (key == KEY_ABORT_ACTION) {
                if (dragComponent != null) {
                    double oldX = screenToMapX(dragStart.x) + dragOffset.getX();
                    double oldY = screenToMapY(dragStart.y) + dragOffset.getY();
                    dragComponent.setLocation(oldX, oldY);
                    dragComponent = null;
                    clearIndicated(); // incase dragging new link
                    repaint();
                }
                //removeLabelEdit();
            }
            

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


            // debug
            char c = e.getKeyChar();
            if (c == 'M') {
                DEBUG_SHOW_MOUSE_LOCATION = !DEBUG_SHOW_MOUSE_LOCATION;
                repaint();
            } else if (c == 'O') {
                DEBUG_SHOW_ORIGIN = !DEBUG_SHOW_ORIGIN;
                repaint();
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
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");
            
            requestFocus();
            dragStart.setLocation(e.getX(), e.getY());
            
            if (toolKeyDown == KEY_TOOL_PAN) {
                originBeforeDrag = getOriginLocation();
                return;
            } else if (toolKeyDown == KEY_TOOL_ZOOM) {
                zoomTool.setZoomPoint(screenToMapPoint(e.getPoint()));
                if (toolKeyEvent.isShiftDown())
                    zoomTool.setZoomSmaller();
                else
                    zoomTool.setZoomBigger();
                return;
            }
            
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());
            
            this.hitComponent = getLWComponentAt(mapX, mapY);
            if (DEBUG_MOUSE)
                System.err.println("\ton " + hitComponent);

            int mods = e.getModifiers();
            //e.isPopupTrigger()
            // java 1.4.0 bug on PC(w2k): isPopupTrigger isn't true for right-click!

            boolean editActivated = false;
            
            if ((mods & RIGHT_BUTTON_MASK) != 0 && (mods & java.awt.Event.CTRL_MASK) == 0)
                // We've pressed the right button down
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
                    //setSelection(hitComponent);
                    cPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            else if (hitComponent != null)
                // We've pressed the left (normal) mouse on some LWComponent
            {
                
                if (e.isControlDown() || e.isAltDown()) {
                    // Mod-Drag off a component to offer new link creation
                    linkSource = hitComponent;
                    dragOffset.setLocation(0,0);
                    creationLink.setSource(linkSource);
                    creationLink.setDisplayed(true);
                    invisibleLinkEndpoint.setLocation(mapX, mapY);
                    dragComponent = invisibleLinkEndpoint;
                } else if (e.isShiftDown()) {
                    if (hitComponent.isSelected())
                        removeFromSelection(hitComponent);
                    else
                        addToSelection(hitComponent);
                } else {
                    //if (hitComponent.isSelected()) {
                        //activateLabelEdit(hitComponent);
                    //    editActivated = true;
                    //} else {
                        setSelection(hitComponent);
                        dragComponent = hitComponent;
                        dragOffset.setLocation(hitComponent.getX() - mapX,
                                               hitComponent.getY() - mapY);
                        //}
                }
            } else {
                if (!e.isShiftDown())
                    // don't clear if shift down -- we may have missed our target
                    // and have to manually do all our selection over again
                    clearSelection();
                draggingSelectionBox = true;
            }
            repaint();
            //if (!editActivated)
            //  removeLabelEdit();
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
            if (dragComponent != null && VueUtil.isMacPlatform())
                mouseDragged(e);
        }

        public void mouseDragged(MouseEvent e)
        {
            mouseWasDragged = true;
            
            if (DEBUG_SHOW_MOUSE_LOCATION) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());

            // todo:
            // activeTool.mouseDragged(e)
            // Tools implement MouseListener/MouseMotionListener

            int screenX = e.getX();
            int screenY = e.getY();
            
            // Stop all dragging if the mouse leaves our component
            // todo: auto-pan as we get close to edge
            if (!e.getComponent().contains(screenX, screenY))
                return;

            if (toolKeyDown == KEY_TOOL_PAN) {
                // drag the entire map
                if (originBeforeDrag != null) {
                    int dx = dragStart.x - screenX;
                    int dy = dragStart.y - screenY;
                    setMapOriginOffset(originBeforeDrag.getX() + dx,
                                       originBeforeDrag.getY() + dy);
                    repaint();
                    return;
                }
            }
            
            // selection box
            if (dragComponent == null && draggingSelectionBox) {
                int sx = dragStart.x < screenX ? dragStart.x : screenX;
                int sy = dragStart.y < screenY ? dragStart.y : screenY;
                Rectangle repaintRect = null;
                if (draggedSelectionBox != null)
                    repaintRect = draggedSelectionBox;
                else
                    repaintRect = new Rectangle();
                draggedSelectionBox = new Rectangle(sx, sy,
                                             Math.abs(dragStart.x - screenX),
                                             Math.abs(dragStart.y - screenY));
                repaintRect.add(draggedSelectionBox);
                repaintRect.grow(1,1);
                repaint(repaintRect);
                return;
            } else
                draggedSelectionBox = null;

            float mapX = screenToMapX(screenX);
            float mapY = screenToMapY(screenY);
            Rectangle2D repaintRegion = null;

            if (dragComponent != null) {
                repaintRegion = dragComponent.getBounds();
                if (dragComponent instanceof LWLink) {
                    LWLink lwl = (LWLink) dragComponent;
                    
                    // todo: will help to add topmost parent of linked-to
                    // component to rr because text labels are being
                    // subtly shifted when the clip region passes through
                    // painted text, and this only appears to apply 
                    // when zoom level is <= 100%.  Actually, adding
                    // topmost parent only helps for case of dragging
                    // a node around a parent that has a link to an
                    // inner child, thus the clip region freq passes
                    // thru parent -- it's not even a general fix.
                    
                    repaintRegion.add(lwl.getComponent1().getBounds());
                    repaintRegion.add(lwl.getComponent2().getBounds());
                }
                dragComponent.setLocation((float) (mapX + dragOffset.getX()),
                                          (float) (mapY + dragOffset.getY()));
                repaintRegion.add(dragComponent.getBounds());
                if (dragComponent instanceof LWLink) {
                    LWLink lwl = (LWLink) dragComponent;
                    repaintRegion.add(lwl.getComponent1().getBounds());
                    repaintRegion.add(lwl.getComponent2().getBounds());
                }
            }
            
            if (linkSource != null) {
                // we're dragging a new link looking for an
                // allowable endpoint
                LWComponent over = findLWLinkTargetAt(mapX, mapY);
                if (indication != null && indication != over) {
                    repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidLinkTarget(over)) {
                    setIndicated(over);
                    repaintRegion.add(over.getBounds());
                }
            } else if (dragComponent instanceof LWNode) {
                // regular drag -- check for node drop onto another
                LWNode over = getLWNodeUnder(mapX, mapY, dragComponent);
                if (indication != null && indication != over) {
                    repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidParentTarget(over)) {
                    setIndicated(over);
                    repaintRegion.add(over.getBounds());
                }
            }

            
            if (dragComponent != null) {

                if (!dragComponent.hasChildren()) {
                    //-------------------------------------------------------
                    // Do some repaint optimzation:
                    // This makes a big difference when cavas is big.
                    // At moment we skip this if LWC has any children as
                    // we don't have a traversal that produces all child links.
                    //-------------------------------------------------------
                    java.util.Iterator i;
                    if (dragComponent instanceof LWLink) {
                        LWLink lwl = (LWLink) dragComponent;
                        i = new VueUtil.GroupIterator(lwl.getLinkEndpointsIterator(),
                                                      lwl.getComponent1().getLinkEndpointsIterator(),
                                                      lwl.getComponent2().getLinkEndpointsIterator());
                                                      
                    } else
                        i = dragComponent.getLinkEndpointsIterator();
                    while (i.hasNext()) {
                        LWComponent c = (LWComponent) i.next();
                        repaintRegion.add(c.getBounds());
                    }
                    if (linkSource != null)
                        repaintRegion.add(linkSource.getBounds());
                    Rectangle rr = mapToScreenRect(repaintRegion);
                    // We fudge the bounds here to include any selection
                    // handles that may be rendering as we drag the component
                    rr.grow(SelectionHandleSize-1,SelectionHandleSize-1);
                    // todo: ALSO: need to grow by stroke width of a dragged link
                    // as it's corners are beyond point with a wide stroke
                    //repaint(rr);
                    repaint();
                } else
                    repaint();
            }
        }

        public void mouseReleased(MouseEvent e)
        {
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + "]");
            
            if (linkSource != null) {
                creationLink.setDisplayed(false);
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
            } else if (mouseWasDragged && dragComponent instanceof LWNode) {
                // drop one node on another -- add as child
                LWNode droppedChild = (LWNode) dragComponent;
                LWNode targetParent = (LWNode) indication;
                /*
                if (targetParent == null)
                    System.out.println("DE-PARENTING " + droppedChild);
                else
                    System.out.println("adding " + droppedChild + " as child of " + targetParent);
                */
                //getMap().removeNode(droppedChild.getNode());
                //targetParent.getNode().addChild(droppedChild.getNode());
                // todo: any other LWComponent views listenting to this concept map
                // won't get updated properly -- this isn't a current requirement,
                // but we should support it.
                //removeLWC(droppedChild);
                if (droppedChild.getParent() != null)
                    droppedChild.getParent().removeChild(droppedChild);
                if (targetParent != null)
                    targetParent.addChild(droppedChild);
            }
            
            dragComponent = null;
            draggingSelectionBox = false;

            if (indication != null)
                clearIndicated();
            
            if (draggedSelectionBox != null) {
                //System.out.println("dragged " + draggedSelectionBox);
                Rectangle2D.Float hitRect = (Rectangle2D.Float) screenToMapRect(draggedSelectionBox);
                //System.out.println("map " + hitRect);
                java.util.List list = getLWComponentsHitBy(hitRect);
                //java.util.List list = getLWComponentsHitBy(screenToMapRect(draggedSelectionBox));
                java.util.Iterator i = list.iterator();
                LWComponent lwc = null;
                while (i.hasNext()) {
                    lwc = (LWComponent) i.next();
                    addToSelection(lwc);
                }
                draggedSelectionBox = null;
            }
            if (selectionList.size() > 0) {
                selectionBounds = getComponentBounds(selectionList.iterator());
                new MapSelectionEvent(MapViewer.this, selectionList).raise();
            }
            repaint();
            mouseWasDragged = false;
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
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");

            if (isSingleClickEvent(e)) {
                if (hitComponent != null) {
                    if (hitComponent.isSelected()
                        //&& hitComponent != justSelected
                        )
                        activateLabelEdit(hitComponent);
                }
            } else if (isDoubleClickEvent(e) && toolKeyDown == 0) {
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

            justSelected = null;
        }

        public LWComponent findLWLinkTargetAt(float x, float y)
        {
            LWComponent directHit = getLWComponentAt(x, y);
            if (directHit != null)
                return directHit;
            
            java.util.List targets = new java.util.ArrayList();
            java.util.Iterator i = new VueUtil.GroupIterator(nodeViews, linkViews);
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (c.targetContains(x, y) && isValidLinkTarget(c))
                    targets.add(c);
            }
            return findClosestEdge(targets, x, y);
        }
    
        /**
         * Make sure we don't create any loops
         */
        public boolean isValidParentTarget(LWComponent parentTarget)
        {
            if (parentTarget == dragComponent)
                return false;
            if (parentTarget.getParent() == dragComponent)
                return false;
            //if (dragComponent.getParent() == parentTarget)
            //return false;
            return true;
        }
        
        /**
         * Make sure we don't create any links back on themselves.
         */
        public boolean isValidLinkTarget(LWComponent linkTarget)
        {
            if (linkTarget == linkSource)
                return false;
            if (linkTarget.getParent() == linkSource
                || linkSource.getParent() == linkTarget)
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
        
    }

    public void addNotify()
    {
        super.addNotify();
        requestFocus();

        /*
        LWContainer c = new LWContainer();
        add(c.getAWTComponent());
        c.setLocation(100,100);
        addComponent(c);
        */
    }

    protected String paramString() {
	return map + super.paramString();
    }

    
    public void setVisible(boolean doShow)
    {
        super.setVisible(doShow);
        if (doShow) {
            requestFocus();
            new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise();
            if (selectionList.size() > 0)
                new MapSelectionEvent(MapViewer.this, selectionList).raise();
            //new MapSelectionEvent(this, ((LWComponent)selectionList.get(0)).getMapItem()).raise();
            
            repaint();
            
        } else {
            new MapViewerEvent(this, MapViewerEvent.HIDDEN).raise();
        }
    }

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
        MapViewer mapView = new tufts.vue.MapViewer(map);

        installExampleMap(map);
        
        /*
         * create a an application frame with some components
         */

        //JFrame frame = new JFrame("VUE Concept Map Viewer");
        JFrame frame = new EventFrame("VUE Concept Map Viewer");
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
        //frame.repaint(); // currently needed to compute text sizes
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

        map.addNode(new Node("One"));
        map.addNode(new Node("Two"));
        map.addNode(new Node("Three"));
        map.addNode(new Node("Four"));

    }
}
