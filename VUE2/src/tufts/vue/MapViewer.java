package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
//import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

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
               , LWComponent.Listener
               , LWSelection.Listener
{
    public interface Listener extends java.util.EventListener
    {
        public void mapViewerEventRaised(MapViewerEvent e);
    }

    java.util.List tools = new java.util.ArrayList();

    protected LWMap map;                   // the map we're displaying & interacting with
    private MapTextEdit activeTextEdit;          // Current on-map text edit

    //-------------------------------------------------------
    // Selection support
    //-------------------------------------------------------
    protected LWSelection VueSelection = VUE.ModelSelection;
    protected LWGroup draggedSelectionGroup = LWGroup.createTemporary(VueSelection);
    protected Rectangle draggedSelectionBox;     // currently dragged selection box
    protected boolean draggingSelectionBox;     // are we currently dragging a selection box?
    protected boolean inDrag;                   // are we currently in a drag?

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
    ZoomTool zoomTool;
    
    public MapViewer(LWMap map)
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

        MapDropTarget mapDropTarget = new MapDropTarget(this);// new CanvasDropHandler
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

        //setPreferredSize(new Dimension(400,300));
        setBackground(map.getFillColor());
        setFont(VueConstants.DefaultFont);
        loadMap(map);

        //-------------------------------------------------------
        // If this map was just restored, there might
        // have been an existing userZoom or userOrigin
        // set -- we honor that last user configuration here.
        //-------------------------------------------------------
        zoomTool.setZoom(getMap().getUserZoom(), false);
        Point2D p = getMap().getUserOrigin();
        setMapOriginOffset(p.getX(), p.getY());
            
        // we repaint any time the global selection changes
        VueSelection.addListener(this);

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
    Point getLastMousePressPoint()
    {
        return new Point(lastMousePressX, lastMousePressY);
    }
    private void setLastMousePoint(int x, int y)
    {
        lastMouseX = x;
        lastMouseY = y;
    }
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
        super.reshape(x,y, w,h);
        repaint(250);
        requestFocus();
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
    
    public void selectionChanged(LWSelection l)
    {
        //System.out.println("MapViewer: selectionChanged");
        repaint();
    }
    
    public void LWCChanged(LWCEvent e)
    {
        // todo: optimize -- we get tons of location events
        // when dragging, esp if there are children if
        // we have those events turned in...
        //System.out.println("MapViewer: " + e);
        if (e.getWhat().equals("location"))
            return;
        if (e.getWhat().equals("deleting")) {
            // FYI, the selection itself could listen for this,
            // but that's a ton of events for this one thing.
            // todo: maybe have LWContainer check isSelected & manage it in deleteChild?
            LWComponent c = e.getComponent();
            boolean wasSelected = c.isSelected();
            selectionRemove(c); // ensure isn't in selection

            // if we just dispersed a group that was selected,
            // put all the former children into the selection instead
            if (wasSelected && c instanceof LWGroup)
                selectionAdd(((LWGroup)c).getChildIterator());
        }
        if (e.getSource() == this)
            return;
        repaint(); // todo opt: could opt region 
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

    /**
     * Java Swing JComponent.paintComponent -- paint the map on the map viewer canvas
     */
    private static int paints=0;
    public void paintComponent(Graphics g)
    {
        //System.out.println("paint " + paints++);
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
        
        //-------------------------------------------------------
        // Paint all the nodes & links
        // Nodes are responsible for painting their children.
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
        this.map.draw(g2);
        if (creationLink.isDisplayed())
            creationLink.draw(g2);
            //drawComponent(g2, creationLink);
        // render the current indication on top
        //if (indication != null)
        //indication.draw(g2);
            //drawComponent(g2, indication);

        /*
        if (dragComponent != null) {
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
            dragComponent.draw(g2);
        }
        */

        //-------------------------------------------------------
        //-------------------------------------------------------
        //paintLWComponents(g2);
        ////super.paintChildren(g2);
        
        //-------------------------------------------------------
        // Restore us to raw screen coordinates & turn off
        // anti-aliasing to draw selection indicators
        //-------------------------------------------------------
        
        if (zoomFactor != 1)
            g2.scale(1.0/zoomFactor, 1.0/zoomFactor);
        g2.translate(getOriginX(), getOriginY());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AA_OFF);

        paintSelection(g2);

        if (DEBUG_SHOW_MOUSE_LOCATION) {
            g2.setColor(Color.red);
            g2.setStroke(new java.awt.BasicStroke(0.01f));
            g2.drawLine(mouseX,mouseY, mouseX,mouseY);

            int iX = (int) (screenToMapX(mouseX) * 100);
            int iY = (int) (screenToMapY(mouseY) * 100);
            float mapX = iX / 100f;
            float mapY = iY / 100f;

            g2.setFont(VueConstants.DefaultFont);
            int y = 0;
            g2.drawString("screen(" + mouseX + "," +  mouseY + ")", 10, y+=20);
            g2.drawString("mapX " + mapX, 10, y+=20);
            g2.drawString("mapY " + mapY, 10, y+=20);;
            g2.drawString("zoom " + getZoomFactor(), 10, y+=20);
            g2.drawString("anitAlias " + !DEBUG_ANTIALIAS_OFF, 10, y+=20);
            g2.drawString("findParent " + !DEBUG_FINDPARENT_OFF, 10, y+=20);
            g2.drawString("repaintOptimize " + !DEBUG_REPAINT_OPTIMIZE_OFF, 10, y+=20);
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
            super(lwc.getLabel());
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
            lwc.setLabel(e.getActionCommand());
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
            System.out.println("mapTextEdit focusLost to " + e.getOppositeComponent());
            removeLabelEdit();
        }
        public void focusGained(FocusEvent e)
        {
            //System.out.println("map edit focusGained");
        }
    
    
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
        if (activeTextEdit != null && activeTextEdit.lwc == lwc)
            return;
        removeLabelEdit();
        activeTextEdit = new MapTextEdit(lwc);

        float ew = screenToMapDim(activeTextEdit.getWidth());
        float eh = screenToMapDim(activeTextEdit.getHeight());
        float cx = lwc.getX() + (lwc.getWidth() - ew) / 2f;
        float cy;
        LWNode node = null;
        if (lwc instanceof LWNode)
            node = (LWNode) lwc;
        if (node != null && node.isAutoSized() && !node.hasChildren())
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
    
    /*
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
        try {
            c.draw((Graphics2D) g2.create());
        } catch (Throwable e) {
            System.err.println("Render exception: " + e);
            e.printStackTrace();
        }
    }
    void paintLWComponents(Graphics2D g2)
    {
        // anti-alias shapes by default
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // anti-alias text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Do we need fractional metrics?  Gives us slightly more accurate
        // string widths on noticable on long strings
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        //-------------------------------------------------------
        // Draw the components.
        // Draw links first so their ends aren't visible.
        //-------------------------------------------------------
        
        // render the LWLinks first so ends are obscured later
        // when the LWNodes are rendered.
        //drawComponentList(g2, linkList);
        
        // render any arbitrary LWComponents
        drawComponentList(g2, componentList);
        
        // render the LWNodes
        // (also need to render nodes b4 links so links can compute their location)
        drawComponentList(g2, nodeList);

        // render the LWLinks LAST so sure to see links to child nodes
        drawComponentList(g2, linkList);
        
        if (creationLink.isDisplayed())
            drawComponent(g2, creationLink);

        // render the current selection on top
        //if (lastSelection != null && VueSelection.size() == 1)
        //  drawComponent(g2, lastSelection);
        
        // render the current indication on top
        if (indication != null)
            drawComponent(g2, indication);
    }
*/


    // todo: move all this code to LWSelection?
    // don't want to make LWSelection a LWComponent tho
    void paintSelection(Graphics2D g2)
    {
        if (VueSelection.size() > 0) {
            g2.setColor(COLOR_SELECTION);
            g2.setStroke(STROKE_SELECTION);
            java.util.Iterator it = VueSelection.iterator();
            while (it.hasNext()) {
                LWComponent c = (LWComponent) it.next();
                drawComponentSelectionBox(g2, c);
            }
        }
        
        if (draggedSelectionBox != null) {
            //-------------------------------------------------------
            // draw the selection box being dragged by the user
            //-------------------------------------------------------
            
            // todo opt: would this be any faster done on a glass pane?
            
            //-------------------------------------------------------
            //
            // *** Okay -- all this only happens when repaint optimization is on
            // -- a bug in repainting with clip region?  might try
            // manually setting the region instead of using repaint(region)
            //
            // TODO BUG: pixels seems to subtly shift for SOME nodes as they
            // pass in and out of the drag region on PC JVM 1.4 -- doesn't
            // depend on region on screen -- actually the node!!
            // Happens to text more often -- somtimes text & strokes.
            // Happens much more frequently at zoom exactly 100%.
            
            // BTW, XOR isn't the problem -- maybe if we used
            // Graphics2D.draw(Shape) instead of drawRect?
            // Doesn't appear to have anything to do with the layer of
            // the node either.  Perhaps if it's shape contains rounded
            // components?  Or maybe this is an anti-alias bug?
            // doesn't appear to be anti-alias or fractional-metrics
            // related for the text, tho switchin AA off stops
            // it when the whole node is sometimes slightly streteched
            // or compressed off to the right.

            // Doesn't seem to happen right away either -- have
            // to zoom in/out some and/or pan the map around first
            // Poss requirement: change zoom (in worked), then PAN
            // while at the zoom level, then zoom back to 100%
            // this seems to do it for the text shifting anyway --
            // shifting of everything takes something else I guess.
            //-------------------------------------------------------
            
            //g2.setXORMode(COLOR_SELECTION_DRAG);
            g2.setColor(COLOR_SELECTION_DRAG);
            g2.setStroke(STROKE_SELECTION_DYNAMIC);
            g2.drawRect(draggedSelectionBox.x, draggedSelectionBox.y,
                        draggedSelectionBox.width, draggedSelectionBox.height);
            // end paint dragged selection box
        }// else
        //if (!VueSelection.isEmpty() && (!inDrag || draggingSelectionBox)) {
        if (!VueSelection.isEmpty()) {
            g2.setStroke(STROKE_ONE);
            // todo opt: don't recompute bounds here every paint ---
            // can cache in draggedSelectionGroup
            Rectangle2D selectionBounds;
            selectionBounds = VueSelection.getBounds();
            /*
              bounds cache hack
            if (VueSelection.size() == 1)
                selectionBounds = VueSelection.first().getBounds();
            else
                selectionBounds = draggedSelectionGroup.getBounds();
            */
            //System.out.println("mapSelectionBounds="+selectionBounds);
            Rectangle2D.Float sb = mapToScreenRect2D(selectionBounds);
            //System.out.println("screenSelectionBounds="+sb);
            drawSelectionBox(g2, sb);
        }

    }

    static final Rectangle2D SelectionHandle = new Rectangle2D.Float(0,0,0,0);
    static final int SelectionHandleSize = 6; // selection handle fill size -- todo: config
    // exterior drawn box will be 1 pixel bigger
    private void drawSelectionHandle(Graphics2D g, float x, float y)
    {
        SelectionHandle.setFrame(x, y, SelectionHandleSize, SelectionHandleSize);
        g.setColor(Color.white);
        g.fill(SelectionHandle);
        g.setColor(COLOR_SELECTION);
        g.draw(SelectionHandle);
    }
    void drawSelectionBox(Graphics2D g, Rectangle2D.Float r)
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
    static final int chs = 5; // component handle size -- todo: config
    void drawComponentSelectionBox(Graphics2D g, LWComponent c)
    {
        Rectangle2D.Float r = mapToScreenRect2D(c.getBounds());
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
            //mapPopup.add("Visible");
            mapPopup.setBackground(Color.gray);
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
            //cPopup.add(VUE.alignMenu);
            //cPopup.add(VUE.alignMenu.getPopupMenu());


            // This menu has gotten too big at top level...
            cPopup.addSeparator();
            cPopup.add(Actions.AlignLeftEdges);
            cPopup.add(Actions.AlignRightEdges);
            cPopup.add(Actions.AlignTopEdges);
            cPopup.add(Actions.AlignBottomEdges);
            cPopup.addSeparator();
            cPopup.add(Actions.AlignCentersRow);
            cPopup.add(Actions.AlignCentersColumn);
            cPopup.addSeparator();
            cPopup.add(Actions.DistributeVertically);
            cPopup.add(Actions.DistributeHorizontally);
            cPopup.addSeparator();
            cPopup.add(Actions.Delete);
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
                    DEBUG_REPAINT_OPTIMIZE_OFF = !DEBUG_REPAINT_OPTIMIZE_OFF;
                } else if (c == 'P') {
                    DEBUG_FINDPARENT_OFF = !DEBUG_FINDPARENT_OFF;
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
            
            setLastMousePressPoint(e.getX(), e.getY());
            dragComponent = null;
            
            float mapX = screenToMapX(e.getX());
            float mapY = screenToMapY(e.getY());

            hitComponent = getMap().findLWComponentAt(mapX, mapY);
            if (DEBUG_MOUSE)
                //if (hitComponent != null)
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
                        draggedSelectionGroup.useSelection(VueSelection);
                        dragComponent = draggedSelectionGroup;
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
                draggingSelectionBox = true;
            }

            if (dragComponent != null)
                dragOffset.setLocation(dragComponent.getX() - mapX,
                                       dragComponent.getY() - mapY);

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

        private int drags=0;
        public void mouseDragged(MouseEvent e)
        {
            inDrag = true;
            //System.out.println("drag " + drags++);
            if (mouseWasDragged == false) {
                // we're just starting this drag
                if (dragComponent != null)
                    mouseWasDragged = true;
            }
            
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
            
            // Stop component dragging if the mouse leaves our component
            // todo: auto-pan as we get close to edge
            if (!e.getComponent().contains(screenX, screenY))
                return;

            
            //-------------------------------------------------------
            // Update the dragged selection box
            //-------------------------------------------------------
            if (dragComponent == null && draggingSelectionBox) {
                // Set repaint-rect to where old selection is
                Rectangle repaintRect;
                if (draggedSelectionBox != null)
                    repaintRect = draggedSelectionBox;
                else
                    repaintRect = new Rectangle();
                
                // Set the current selection box
                int sx = dragStart.x < screenX ? dragStart.x : screenX;
                int sy = dragStart.y < screenY ? dragStart.y : screenY;
                draggedSelectionBox = new Rectangle(sx, sy,
                                             Math.abs(dragStart.x - screenX),
                                             Math.abs(dragStart.y - screenY));

                if (!DEBUG_REPAINT_OPTIMIZE_OFF) {
                    // Now add to repaint-rect the new selection
                    repaintRect.add(draggedSelectionBox);
                    repaintRect.grow(1,1);
                    repaint(repaintRect);
                } else {
                    repaint();
                }
                return;
            } else
                draggedSelectionBox = null;

            float mapX = screenToMapX(screenX);
            float mapY = screenToMapY(screenY);

            Rectangle2D repaintRegion = null;

            if (dragComponent != null) {
                //-------------------------------------------------------
                // Compute repaint region based on what's being dragged
                //-------------------------------------------------------
                repaintRegion = dragComponent.getBounds();

                if (repaintRegion == null) {// todo: this is debug
                    new Throwable("mouseDragged: null bounds dragComponent " + dragComponent).printStackTrace();
                    repaintRegion = new Rectangle2D.Float();
                }

                if (dragComponent instanceof LWLink) {
                    LWLink lwl = (LWLink) dragComponent;
                    // todo: not currently used as link dragging disabled
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
                //-------------------------------------------------------
                // Reposition the component due to mouse drag
                //-------------------------------------------------------
                dragComponent.setLocation((float) (mapX + dragOffset.getX()),
                                          (float) (mapY + dragOffset.getY()));
                //-------------------------------------------------------
                // Compute more repaint region
                //-------------------------------------------------------
                repaintRegion.add(dragComponent.getBounds());
                if (dragComponent instanceof LWLink) {
                    // todo: not currently used as link dragging disabled
                    LWLink lwl = (LWLink) dragComponent;
                    repaintRegion.add(lwl.getComponent1().getBounds());
                    repaintRegion.add(lwl.getComponent2().getBounds());
                }
            }
            
            if (linkSource != null) {
                //-------------------------------------------------------
                // we're dragging a new link looking for an
                // allowable endpoint
                //-------------------------------------------------------
                LWComponent over = findLWLinkTargetAt(mapX, mapY);
                if (indication != null && indication != over) {
                    repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidLinkTarget(over)) {
                    setIndicated(over);
                    repaintRegion.add(over.getBounds());
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
                    repaintRegion.add(indication.getBounds());
                    clearIndicated();
                }
                if (over != null && isValidParentTarget(over)) {
                    setIndicated(over);
                    if (repaintRegion == null) // todo: this is debug
                        new Throwable("mouseDragged: null rr").printStackTrace();
                    Rectangle2D bounds = over.getBounds();
                    if (bounds == null) // todo: this is debug
                        new Throwable("mouseDragged: null bounds for over " + over).printStackTrace();
                    repaintRegion.add(over.getBounds());
                }
            }

            
            if (dragComponent != null && DEBUG_REPAINT_OPTIMIZE_OFF) {
                
                repaint();

            } else if (dragComponent != null) {

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

                java.util.Iterator i;
                if (dragComponent instanceof LWLink) {
                    // todo: not in use as moment as Link dragging disabled...
                    LWLink lwl = (LWLink) dragComponent;
                    i = new VueUtil.GroupIterator(lwl.getLinkEndpointsIterator(),
                                                  lwl.getComponent1().getLinkEndpointsIterator(),
                                                  lwl.getComponent2().getLinkEndpointsIterator());
                                                      
                } else
                    i = dragComponent.getAllConnectedNodes().iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    repaintRegion.add(c.getBounds());
                }
                if (linkSource != null)
                    repaintRegion.add(linkSource.getBounds());
                Rectangle rr = mapToScreenRect(repaintRegion);
                // We grow the bounds here to include any selection
                // handles that may be rendering as we drag the component
                //rr.grow(SelectionHandleSize-1,SelectionHandleSize-1);
                rr.grow(SelectionHandleSize+3,SelectionHandleSize+3);//todo: tmp hack slop region
                repaint(rr);
            }
        }

        public void mouseReleased(MouseEvent e)
        {
            inDrag = false;
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + "]");

            setLastMousePoint(e.getX(), e.getY());
            
            if (linkSource != null) {
                creationLink.setDisplayed(false);
                LWComponent linkDest = indication;
                if (linkDest != null && linkDest != linkSource)
                {
                    LWLink lwl = linkDest.getLinkTo(linkSource);
                    if (lwl != null) {
                        // There's alreay a link tween these two -- increment the weight
                        lwl.incrementWeight();
                    } else {
                        getMap().addLink(new LWLink(linkSource, linkDest));
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
                    if (droppedChild.getParent() != parentTarget && droppedChild != parentTarget) {
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
            draggingSelectionBox = false;
            mouseWasDragged = false;

            if (indication != null)
                clearIndicated();
            
            if (draggedSelectionBox != null) {
                //System.out.println("dragged " + draggedSelectionBox);
                Rectangle2D.Float hitRect = (Rectangle2D.Float) screenToMapRect(draggedSelectionBox);
                //System.out.println("map " + hitRect);
                java.util.List list = computeSelection(hitRect, e.isControlDown());
                if (e.isShiftDown())
                    selectionToggle(list.iterator());
                else
                    selectionAdd(list.iterator());
                draggedSelectionBox = null;
                
                // bounds cache hack
                if (!VueSelection.isEmpty())
                    draggedSelectionGroup.useSelection(VueSelection);
                // todo: need to update draggedSelectionGroup here
                // so we can use it's cached bounds to compute
                // the painting of the selection -- rename to just
                // SelectionGroup if we keep using it this way.
            }
            repaint();
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
            if (DEBUG_MOUSE) System.err.println("[" + e.paramString() + (e.isPopupTrigger() ? " POP":"") + "]");

            if (isSingleClickEvent(e)) {
                if (hitComponent != null && !(hitComponent instanceof LWGroup)) {
                    if (hitComponent.isSelected() && hitComponent != justSelected)
                        activateLabelEdit(hitComponent);
                }
            } else if (isDoubleClickEvent(e) && toolKeyDown == 0) {
                if (hitComponent instanceof LWNode) {
                    Resource resource = hitComponent.getResource();
                    if (resource != null) {
                        // todo: some kind of animation or something to show
                        // we're "opening" this node -- maybe an indication
                        // flash -- we'll need another thread for that.
                        System.err.println("opening resource for: " + hitComponent);
                        resource.displayContent();
                    } else
                        activateLabelEdit(hitComponent);
                }
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
	return getMap() + super.paramString();
    }

    private boolean isAnythingCurrentlyVisible()
    {
        Rectangle mapRect = mapToScreenRect(getMap().getBounds());
        Rectangle viewerRect = getBounds(null);
        return mapRect.intersects(viewerRect);
    }

    public void setVisible(boolean doShow)
    {
        super.setVisible(doShow);
        if (doShow) {
            // todo: only do this if we've just been opened
            //if (!isAnythingCurrentlyVisible())
            //zoomTool.setZoomFitContent(this);//todo: go thru the action
            requestFocus();
            new MapViewerEvent(this, MapViewerEvent.DISPLAYED).raise();
            VUE.ModelSelection.clear(); // same as VueSelection / selectionClear()
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
        //mapView.setPreferredSize(new Dimension(400,300));
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
                      System.out.println("InfoField "+ infoField+" Value " );
                      String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getValue().toString();
                      infoRecordMenu.add(new FedoraAction(infoField.getValue().toString(),method));
                  }
                  
                  returnMenu.add(infoRecordMenu);
            }
        } catch(Exception e) { System.out.println("MapViewer.getAssetMenu"+e);}
        return returnMenu;
    }   
// this class will move out of here

    public String toString()
    {
        return "MapViewer@" + Integer.toHexString(hashCode());
    }
  
    
    //-------------------------------------------------------
    // debugging stuff
    //-------------------------------------------------------
    
    private boolean DEBUG_SHOW_ORIGIN = false;
    private boolean DEBUG_ANTIALIAS_OFF = false;
    private boolean DEBUG_SHOW_MOUSE_LOCATION = false; // slow (constant repaint)
    private boolean DEBUG_KEYS = false;
    private boolean DEBUG_MOUSE = false;
    private boolean DEBUG_REPAINT_OPTIMIZE_OFF = false;
    private boolean DEBUG_FINDPARENT_OFF = false;
    private int mouseX;
    private int mouseY;

    private final Object AA_OFF = RenderingHints.VALUE_ANTIALIAS_OFF;
    private Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;



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
