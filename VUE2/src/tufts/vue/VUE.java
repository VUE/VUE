package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.action.*;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 * modified by John Briedis on 5/29/03
 * added support for Save as type html
 * and support for html image mapping
 */
public class VUE
    implements VueConstants
{
    public static Cursor CURSOR_ZOOM_IN;
    public static Cursor CURSOR_ZOOM_OUT;
    
    public static JFrame frame;
    //set to public so that action package can access it (Jay Briedis 6/4/03)
    public static JTabbedPane tabbedPane;
    
    static {
        /*
        String imgLocation = "toolbarButtonGraphics/navigation/Back24.gif";
        URL imageURL = getClass().getResource(imgLocation);
        if (imageURL != null)
            button = new JButton(new ImageIcon(imageURL));
        */

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image iconZoomIn;
        Image iconZoomOut;
        if (VueUtil.isMacPlatform()) {
            iconZoomIn = toolkit.getImage("images/ZoomIn16.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut16.gif");
        } else {
            iconZoomIn = toolkit.getImage("images/ZoomIn24.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut24.gif");
        }
        CURSOR_ZOOM_IN = toolkit.createCustomCursor(iconZoomIn, new Point(0,0), "ZoomIn");
        CURSOR_ZOOM_OUT = toolkit.createCustomCursor(iconZoomOut, new Point(0,0), "ZoomOut");
    }

    static class VueFrame extends JFrame
        implements MapViewerListener
    {
        final int TitleChangeMask = MapViewerEvent.DISPLAYED | MapViewerEvent.ZOOM;
        
        VueFrame()
        {
            super("VUE: Tufts Concept Map Tool");
        }
        public void mapViewerEventRaised(MapViewerEvent e)
        {
            if ((e.getID() & TitleChangeMask) != 0)
                setTitleFromViewer(e.getMapViewer());
        }

        private void setTitleFromViewer(MapViewer viewer)
        {
            String title = "VUE: " + viewer.getMap().getLabel();
            
            int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
            // round the display value down to 2 digits
            title += " [";
            if ((displayZoom / 100) * 100 == displayZoom)
                title += (displayZoom / 100) + "%";
            else
                title += (((float) displayZoom) / 100f) + "%";
            title += "]";
            setTitle(title);
        }
    }

    static class VuePanel extends JPanel
    {
        public void paint(Graphics g)
        {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.
            ((Graphics2D)g).setRenderingHint
                (RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }

    static {
        if (false && VueUtil.isMacPlatform()) {
            final String usmbProp = "apple.laf.useScreenMenuBar";
            final String appNameProp = "com.apple.mrj.application.apple.menu.about.name";
            if (System.getProperty(usmbProp) == null)
                System.setProperty(usmbProp, "true");
            // setting appNameProp here doesn't do anything anything since VM
            // has already made use of this property...
            System.setProperty(appNameProp, "VUE");
        }
    }

    public static void activateWaitCursor()
    {
        // todo: save current cursor and pop off stack when we clear
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_WAIT);
    }
    public static void clearWaitCursor()
    {
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_DEFAULT);
    }


    public static void main(String[] args)
    {
        String laf = null;
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //laf = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            if (laf != null)
                javax.swing.UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            System.err.println(e);
        }
        
        /*
         * create an example map (this will become
         * map loading code after the viewer is up)
         */
        ConceptMap map1 = new ConceptMap("Example One");
        ConceptMap map2 = new ConceptMap("Example Two");
        ConceptMap map3 = new ConceptMap("Empty Map");
        
        /*
         * create the map viewer
         */
        MapViewer mapViewer1 = new tufts.vue.MapViewer(map1);
        //Container mapViewer2 = new tufts.vue.MapViewer(map1);
        Container mapViewer3 = new tufts.vue.MapViewer(map2);
        Container mapViewer4 = new tufts.vue.MapViewer(map3);

        installExampleMap(map1);
        installExampleMap(map2);
        installExampleNodes(map1, mapViewer1);

        tabbedPane = new JTabbedPane();        
        tabbedPane.addTab(map1.getLabel(), mapViewer1);
        //tabbedPane.addTab(map1.getLabel() + "[View 2]", mapViewer2);
        tabbedPane.addTab(map2.getLabel(), mapViewer3);
        tabbedPane.addTab(map3.getLabel(), mapViewer4);
        
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setTabPlacement(SwingConstants.BOTTOM);
        //tabbedPane.setTabPlacement(SwingConstants.TOP);
        
        /*
         * create a an application frame and layout components
         */
        
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        //toolPanel.add(new MapPanner(mapViewer1), BorderLayout.CENTER);
        toolPanel.add(new LWCInspector(), BorderLayout.SOUTH);
        //toolPanel.add(new MapItemInspector(), BorderLayout.SOUTH);


        JSplitPane splitPane = new JSplitPane();
        //JScrollPane leftScroller = new JScrollPane(toolPanel);

        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        //splitPane.setLeftComponent(leftScroller);
        splitPane.setRightComponent(tabbedPane);


        frame = new VueFrame();
        JPanel vuePanel = new VuePanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        //vuePanel.add(splitPane);

        // Create the tool windows
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner(mapViewer1));

        ToolWindow inspectorTool = new ToolWindow("Inspector", frame);
        inspectorTool.addTool(new LWCInspector());
        //inspectorTool.addTool(new MapItemInspector());

        Action[] windowActions = { pannerTool.getDisplayAction(),
                                   inspectorTool.getDisplayAction() };

        // adding the menus and toolbars
        setMenuToolbars(frame, windowActions);
        
        frame.getContentPane().add(vuePanel,BorderLayout.CENTER);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.pack();

        Dimension d = frame.getToolkit().getScreenSize();
        int x = d.width/2 - frame.getWidth()/2;
        int y = d.height/2 - frame.getHeight()/2;
        frame.setLocation(x, y);
        
        frame.show();
        

    }


    static void installExampleNodes(ConceptMap map, MapViewer view)
    {
        view.addLWC(new LWNode("Oval", 0)).setFillColor(Color.red);
        view.addLWC(new LWNode("Circle", 1)).setFillColor(Color.green);
        view.addLWC(new LWNode("Square", 2)).setFillColor(Color.orange);
        view.addLWC(new LWNode("Rectangle", 3)).setFillColor(Color.blue);
        view.addLWC(new LWNode("Rounded Rectangle", 4)).setFillColor(Color.yellow);

        /*
          view.addLWC(new LWNode(map.addNode(new Node("Oval")), 0)).setFillColor(Color.red);
        view.addLWC(new LWNode(map.addNode(new Node("Circle")), 1)).setFillColor(Color.green);
        view.addLWC(new LWNode(map.addNode(new Node("Square")), 2)).setFillColor(Color.orange);
        view.addLWC(new LWNode(map.addNode(new Node("Rectangle")), 3)).setFillColor(Color.blue);
        view.addLWC(new LWNode(map.addNode(new Node("Rounded Rectangle")), 4)).setFillColor(Color.yellow);
        */
    }
    
    static void installExampleMap(ConceptMap map)
    {
        /*
         * create some test nodes & links
         */
        Node n1 = new Node("Google", new Resource("http://www.google.com/"));
        Node n2 = new Node("Program Files", new Resource("C:\\Program Files"));
        Node n3 = new Node("readme.txt", new Resource("readme.txt"));
        Node n4 = new Node("Slash", new Resource("file:///"));
        n1.setPosition(100, 30);
        n2.setPosition(100, 100);
        n3.setPosition(50, 180);
        n4.setPosition(200, 180);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addLink(new Link(n1, n2));
        map.addLink(new Link(n2, n3));
        map.addLink(new Link(n2, n4));

        map.addNode(new Node("One"));
        map.addNode(new Node("Two"));
        map.addNode(new Node("Three"));
        map.addNode(new Node("Four"));

    }

    public static ConceptMap getActiveMap()
    {
        MapViewer mapViewer = (MapViewer) tabbedPane.getSelectedComponent();
        return mapViewer.getMap();
    }

    public static void displayMap(ConceptMap map)
    {
        // todo: figure out if we're already displaying this map
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            MapViewer mv = (MapViewer) tabbedPane.getComponentAt(i);
        }
        MapViewer mapViewer = new tufts.vue.MapViewer(map);
        tabbedPane.addTab(map.getLabel(), mapViewer);
        tabbedPane.setSelectedComponent(mapViewer);
    }
    
    private static void  setMenuToolbars(JFrame frame, Action[] windowActions) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu formatMenu = new JMenu("Format");
        JMenu windowMenu = new JMenu("Window");
        JMenu optionsMenu = new JMenu("Options");
        JMenu helpMenu = new JMenu("Help");
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        menuBar.add(windowMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open");
        ExitAction exitAction = new ExitAction("Quit");
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.META_MASK));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.META_MASK));
        fileMenu.add(saveAsAction);
        //fileMenu.add(htmlAction);
        fileMenu.add(new JMenuItem("Export ..."));
        fileMenu.addSeparator();
        fileMenu.add(exitAction);
        
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Select All"));
        
        viewMenu.add(new JMenuItem("Zoom In"));
        viewMenu.add(new JMenuItem("Zoom Out"));
        viewMenu.add(new JMenuItem("Zoom Fit"));
        viewMenu.add(new JMenuItem("Zoom 100%"));
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Resources"));
        viewMenu.add(new JMenuItem("Collection"));
        viewMenu.add(new JMenuItem("Inspector"));
        viewMenu.add(new JMenuItem("Pathway"));
        viewMenu.add(new JMenuItem("Toolbar"));
        viewMenu.add(new JMenuItem("Overview"));
        
        formatMenu.add(new JMenuItem("Font"));
        formatMenu.add(new JMenuItem("Size"));
        formatMenu.add(new JMenuItem("Style"));
        formatMenu.add(new JMenuItem("Justify"));
        formatMenu.add(new JMenuItem("Align"));
        formatMenu.add(new JMenuItem("Group"));
        formatMenu.add(new JMenuItem("UnGroup"));
        
        for (int i = 0; i < windowActions.length; i++) {
            System.out.println("adding " + windowActions[i]);
            windowMenu.add(new JCheckBoxMenuItem(windowActions[i]));
        }

        optionsMenu.add(new JMenuItem("Node Types..."));
        optionsMenu.add(new JMenuItem("Map Preference..."));
        optionsMenu.add(new JMenuItem("Preferences..."));
        
        helpMenu.add(new JMenuItem("Help"));
        

        

        //extra additions by the power team members
        PDFConversion pdfAction = new PDFConversion("PDF");
        HTMLConversion htmlAction = new HTMLConversion("Html");
        ImageConversion imageAction = new ImageConversion("Jpeg");
        ImageMap imageMap = new ImageMap("Imap");
        SVGConversion svgAction = new SVGConversion("SVG");
<<<<<<< VUE.java
        PrintAction printAction = new PrintAction("Print");
=======
        

>>>>>>> 1.24
        
        JToolBar toolBar = new JToolBar();
        toolBar.add(openAction);
        toolBar.add(saveAction);
        toolBar.add(saveAsAction);
        toolBar.add(imageAction);
        toolBar.add(htmlAction);
        toolBar.add(pdfAction);
        toolBar.add(imageMap);
        toolBar.add(svgAction);
        toolBar.add(printAction);
        toolBar.add(new JButton(new ImageIcon("tufts/vue/images/ZoomOut16.gif")));
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(toolBar,BorderLayout.NORTH);
        frame.addWindowListener(new WindowListener(){
          public void windowClosing(WindowEvent e){System.exit(0);}
          public void windowOpened(WindowEvent e){}
          public void windowClosed(WindowEvent e){}
          public void windowIconified(WindowEvent e){}
          public void windowDeiconified(WindowEvent e){}
          public void windowActivated(WindowEvent e){}
          public void windowDeactivated(WindowEvent e){}
        });
    }
}
