package tufts.vue;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * ConceptMapView.java
 *
 * Implements a panel for displaying & interacting with
 * an instance of ConceptMap.  
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
class ConceptMapView extends javax.swing.JPanel
    implements MapChangeListener
{
    static final Font panelFont = new Font("SansSerif", Font.PLAIN, 11);// todo: prefs
    
    java.util.List components = new java.util.ArrayList();
    java.util.List nodeViews = new java.util.ArrayList();
    java.util.List linkViews = new java.util.ArrayList();

    private ConceptMap map;
    private LWComponent selection;

    private final LWComponent dynamicLinkEndpoint = new LWComponent();
    private final LinkView dynamicLink = new LinkView(dynamicLinkEndpoint);

    private final int scaleDown = 1; // experiment (1 = do nothing)
    private final double scaleFactor = 1.0 / scaleDown; // experiment

    public ConceptMapView(ConceptMap map)
    {
        dynamicLink.setDisplayed(false);
        addLink(dynamicLink);
        //setLayout(new NoLayout());
        setLayout(null);
        MouseHandler m = new MouseHandler();
        addMouseListener(m);
        addMouseMotionListener(m);

        MapDropTarget mapDropTarget = new MapDropTarget(this);
        this.setDropTarget(new DropTarget(this, mapDropTarget));
        setFont(panelFont);

        loadMap(map);
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

    NodeView findNodeView(Node node)
    {
        // fixme: slow way to find nodeViews -- will
        // need to speed up for big maps
        java.util.Iterator i = nodeViews.iterator();
        while (i.hasNext()) {
            NodeView nv = (NodeView) i.next();
            if (nv.getNode() == node)
                return nv;
        }
        return null;
    }
    
    LWComponent findComponent(MapItem mapItem)
    {
        // fixme
        return null;
    }

    public void mapItemAdded(MapChangeEvent e)
    {
        MapItem src = e.getItem();
        if (src instanceof Node)
            addNode(new NodeView((Node) src));
        else if (src instanceof Link) {
            Link link = (Link) src;
            NodeView nv1 = findNodeView(link.getNode1());
            NodeView nv2 = findNodeView(link.getNode2());
            addLink(new LinkView(link, nv1, nv2));
        }
        else {
            throw new RuntimeException("unhandled: " + e);
            //fixme: real exception
        }
    }
    public void mapItemRemoved(MapChangeEvent e)
    {
        removeComponent(findComponent(e.getItem()));
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
    public NodeView addNode(NodeView nv)
    {
        nodeViews.add(nv);
        repaint();
        return nv;
    }
    public boolean removeNode(NodeView nv)
    {
        boolean success = nodeViews.remove(nv);
        repaint();
        return success;
    }
    public LinkView addLink(LinkView lv)
    {
        repaint();
        linkViews.add(lv);
        return lv;
    }
    public boolean removeLink(LinkView lv)
    {
        boolean success = linkViews.remove(lv);
        repaint();
        return success;
    }

    private static MapItemInspector mii;
    public static void main(String[] args) {
        /*
         * create an example map
         */
        ConceptMap map = new ConceptMap("Example Map");
        
        /*
         * create the viewer
         */
        javax.swing.JComponent mapView = new ConceptMapView(map);
        mapView.setPreferredSize(new Dimension(400,300));

        /*
         * create some test nodes & links
         */
        
        Node n1 = new Node("Test node1");
        Node n2 = new Node("Test node2");
        Node n3 = new Node("foo.txt", new Resource("/tmp/foo.txt"));
        Node n4 = new Node("Tester Node Four");
        n1.setPosition(100, 50);
        n2.setPosition(100, 100);
        n3.setPosition(50, 150);
        n4.setPosition(150, 150);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addLink(new Link(n1, n2));
        map.addLink(new Link(n2, n3));
        map.addLink(new Link(n2, n4));
        
        /*
         * create a an application frame with some components
         */
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        toolPanel.add(mii = new MapItemInspector(), BorderLayout.SOUTH);

        JFrame frame = new JFrame("VUE: Tufts Concept Map Tool");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mapView, BorderLayout.CENTER);
        frame.getContentPane().add(new JToolBar(), BorderLayout.NORTH);
        frame.getContentPane().add(toolPanel, BorderLayout.WEST);
        
        frame.pack();
        frame.show();
    }

    public Component fast_locate(int x, int y) {
	return this;
    }

    public LWComponent getLWComponentAt(int x, int y)
    {
        java.util.Iterator i;
        i = components.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(x, y))
                return c;
        }
        i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(x, y))
                return c;
        }
        i = linkViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(x, y))
                return c;
        }

        return null;
    }
    
    
    public void paintComponent(Graphics g)
    {
        if (scaleDown != 1)
            ((Graphics2D)g).scale(scaleFactor, scaleFactor);
        super.paintComponent(g);
        /*
         * Draw the components.
         * Draw links first so their ends aren't visible.
         */
        java.util.Iterator i;
        i = linkViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed())
                c.draw(g);
        }
        i = components.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed())
                c.draw(g);
        }
        i = nodeViews.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.isDisplayed())
                c.draw(g);
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

        /*// hack in liu of event
        if (mii != null) {
            if (newSelection != null)
                mii.setItem(newSelection.getMapItem());
            else
                mii.setItem(null);
        }
        */
        
    }

    class MouseHandler extends tufts.vue.MouseAdapter
    {
        LWComponent dragComponent;
        LWComponent linkSource;
        LWComponent indicatedComponent;
        int dragXoffset;
        int dragYoffset;
        
        public void mousePressed(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            if (scaleDown != 1) { 
                x *= scaleDown;
                y *= scaleDown;
            }
            LWComponent c = getLWComponentAt(x, y);
            //System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName() + " at " + c);
            if (c != null) {
                if (e.isControlDown()) {
                    linkSource = c;
                    dragXoffset = 0;
                    dragYoffset = 0;
                    dynamicLink.setSource(linkSource);
                    dynamicLink.setDisplayed(true);
                    dragComponent = dynamicLinkEndpoint;
                } else {
                    dragComponent = c;
                    dragXoffset = c.getX() - x;
                    dragYoffset = c.getY() - y;
                }
            }
        }
        public void mouseReleased(MouseEvent e)
        {
            if (linkSource != null) {
                dynamicLink.setDisplayed(false);
                LWComponent linkDest = getLWComponentAt(e.getX(), e.getY());
                if (linkDest instanceof NodeView
                    && linkSource instanceof NodeView
                    && linkDest != linkSource)
                {
                    // fixme todo: don't create if already a link between
                    // these two -- increment the weight.
                    getMap().addLink(new Link(((NodeView)linkSource).getNode(),
                                              ((NodeView)linkDest).getNode()));
                }
                linkSource = null;
            }
            dragComponent = null;
            if (indicatedComponent != null) {
                indicatedComponent.setIndicated(false);
                indicatedComponent = null;
            }
            repaint();
        }
        
        public void mouseClicked(MouseEvent e)
        {
            setSelection(getLWComponentAt(e.getX(), e.getY()));
        }

        public void mouseDragged(MouseEvent e)
        {
            //System.err.println("[" + e.paramString() + "] on " + e.getSource().getClass().getName());
            int x = e.getX();
            int y = e.getY();
            if (scaleDown != 1) {
                x *= scaleDown;
                y *= scaleDown;
            }
            if (dragComponent != null) {
                // fixme todo: don't allow drag off screen...
                dragComponent.setLocation(x + dragXoffset, y + dragYoffset);
            }
            if (linkSource != null) {
                LWComponent over = getLWComponentAt(x, y);
                if (indicatedComponent != null && indicatedComponent != over)
                    indicatedComponent.setIndicated(false);
                if (over != null
                    && over instanceof NodeView
                    && over != linkSource)
                {
                    over.setIndicated(true);
                    indicatedComponent = over;
                }
            }
            if (linkSource != null || dragComponent != null)
                repaint();
        }
    }
}


class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    ConceptMapView mapView;
    Point lastPoint;

    public MapDropTarget(ConceptMapView mapView) {
       this.mapView = mapView;
    }

    public void dragEnter(DropTargetDragEvent e) {
        e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        System.err.println("[Target] dragEnter");
    }

    public void dragOver(DropTargetDragEvent e) {
        e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        lastPoint = e.getLocation();
        //System.err.println("[Target] dragOver");
    }

    public void dragExit(DropTargetEvent e) {
        System.err.println("[Target] dragExit");
    }

    static final String MAC_OSTYPE_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document

    public String handleTransfer(DataFlavor[] dataFlavors, Transferable transfer)
    {
        String value = null;
        java.util.List list = null;
        
        //DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();

        //System.err.println("plainTextFlavor=" + DataFlavor.plainTextFlavor);
        //System.err.println("plainTextMimeType=" + DataFlavor.plainTextFlavor.getMimeType());
        System.err.println("handleTransfer: found " + dataFlavors.length + " dataFlavors");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            Object data = null;
            try {
                data = transfer.getTransferData(flavor);
            } catch (Exception ex) {
                System.err.println(ex);
            }
            //System.err.println("flavor" + i + " " + flavor);
            System.err.println("flavor" + i + " " + flavor.getMimeType() + "  transferData=" + data);

            try {
            
                if (flavor.isFlavorJavaFileListType()) {
                    list = (java.util.List) transfer.getTransferData(flavor);
                    System.err.println("\tLIST, size= " + list.size());
                    java.util.Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        Object o = iter.next();
                        System.err.println("\t" + o.getClass().getName() + " " + o);
                    }
                    // fixme todo: pull out the whole list
                    value = list.get(0).toString();
                } else {
                    java.io.Reader reader = null;
                    try {
                        reader = flavor.getReaderForText(transfer);
                    } catch (Exception re) {
                        System.err.println(re);
                        if (flavor.isRepresentationClassInputStream()) {
                            System.err.println("\tINPUTSTREAM?");
                            if (data instanceof Object[]) {
                                Object array[] = (Object[]) data;
                                System.err.println("\tarray of length " + array.length);
                                for (int j = 0; j < array.length; j++) {
                                    System.err.println("\tloc" + j + " = " + array[j]);
                                }
                            } else if (data instanceof java.io.InputStream)
                                reader = new java.io.InputStreamReader((java.io.InputStream)data);
                        }
                    }
                    if (reader != null) {
                        System.err.println("\treader=" + reader);
                        char buf[] = new char[512];
                        int got = reader.read(buf);
                        value = new String(buf);
                        System.err.println("\t[" + value + "]");
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex);
                continue;
            }
        }
        return value;
    }

    public void drop(DropTargetDropEvent e) {
        System.err.println("[Target] drop, lastPoint=" + lastPoint);
        DropTargetContext targetContext = e.getDropTargetContext();

        boolean outcome = false;

        if ((e.getSourceActions() & DnDConstants.ACTION_COPY) != 0)
            e.acceptDrop(DnDConstants.ACTION_COPY);
        else {
            e.rejectDrop();
            return;
        }

        //DataFlavor[] dataFlavors = e.getCurrentDataFlavors();
        //handleTransfer(e.getCurrentDataFlavors(), e.getTransferable());
        String text = handleTransfer(e.getTransferable().getTransferDataFlavors(), e.getTransferable());

        if (text == null)
            text = "<null>";
        
        Insets mapInsets = mapView.getInsets();
        Point mapLocation = mapView.getLocation();
        System.err.println("mapView insets=" + mapInsets);
        System.err.println("mapView location=" + mapLocation);
        lastPoint.x -= mapLocation.x;
        lastPoint.y -= mapLocation.y;
        lastPoint.x -= mapInsets.left;
        lastPoint.y -= mapInsets.top;
        int i = text.lastIndexOf('/');
        String name = (i > 0) ? text.substring(i+1) : text;
            
        Node n = new Node(name, new Resource(text), lastPoint);
        mapView.getMap().addNode(n);
        mapView.setSelection(mapView.findNodeView(n));
        // fixme todo: raise an item dropped event
    }

    // fixme todo: handle drop-complete

    public void dragScroll(DropTargetDragEvent e) {
    }

    public void dropActionChanged(DropTargetDragEvent e) {
        System.err.println("[Target] dropActionChanged");
    }

}
