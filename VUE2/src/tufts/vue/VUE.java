package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import tufts.vue.action.*;
import java.util.LinkedList;
import java.util.prefs.*;
import java.io.*;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 */

public class VUE
    implements VueConstants
{
    public static final String CASTOR_XML_MAPPING = "lw_mapping.xml";
    public static final java.net.URL CASTOR_XML_MAPPING_RESOURCE = getResource("lw_mapping.xml");
    //    public static final java.net.URL CASTOR_XML_MAPPING_RESOURCE = ClassLoader.getSystemResource("lw_mapping.xml");
    //public final java.net.URL CASTOR_XML_MAPPING_RESOURCE = getClass().getResource("lw_mapping.xml");
    public static final String VUE_CONF = "vue.conf";
    
    // preferences for the application 
    public static Preferences prefs;
    
    /** The currently active viewer (e.g., is visible
        and has focus).  Actions (@see Actions.java) are performed on
        the active model (sometimes querying the active viewer). */
    public static MapViewer ActiveViewer = null;
    /** The currently active selection.
        elements in ModelSelection should always be from the ActiveModel */
    public static LWSelection ModelSelection = new LWSelection();

    /** teh global resource selection static model **/
    public static ResourceSelection sResourceSelection = new ResourceSelection();
    
    public static JFrame frame;
    
    private static MapTabbedPane mMapTabsLeft;
    private static MapTabbedPane mMapTabsRight;
    private static JSplitPane viewerSplit;
    
    //pathway components
    public static LWPathwayInspector pathwayInspector;
    //public static PathwayControl control;

    static ToolWindow objectInspector;
    static ObjectInspectorPanel objectInspectorPanel;
    
    //hierarchy view tree window component
    public static LWHierarchyTree hierarchyTree;
    
    //overview tree window component
    public static LWOutlineView outlineView;
    
    public static java.net.URL getResource(String name)
    {
        java.net.URL url = null;
        // First, check the current directory:
        java.io.File f = new java.io.File(name);
        boolean foundInCWD = false;
        if (f.exists()) {
            try {
                url = f.toURL();
                foundInCWD = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // If not found in the current directory, check the classpath:
        if (url == null)
            url = ClassLoader.getSystemResource(name);
        if (foundInCWD)
            System.out.println("resource \"" + name + "\" found in CWD at " + url);
        else
            System.out.println("resource \"" + name + "\" found in classpath at " + url);
        return url;
    }
    
        /*
        String imgLocation = "toolbarButtonGraphics/navigation/Back24.gif";
        URL imageURL = getClass().getResource(imgLocation);    FileOutputStream fos = new FileOutputStream("vue.conf");
            prefs.exportSubtree(fos);
     //       FileInputStream fis = new FileInputStream("tezt.xml");
     //       prefs.importPreferences(fis);
        } catch (Exception e) { System.out.println(e);}
        if (imageURL != null)
            button = new JButton(new ImageIcon(imageURL));
        */
    /*
    static {

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
    */

    static class VueFrame extends JFrame
        implements MapViewer.Listener
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
            String label = viewer.getMap().getLabel();
            if (viewer.getMap().getFile() != null)
                label = viewer.getMap().getFile().getPath();
            String title = "VUE: " + label;
            
            int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
            // Present the zoom factor as a percentange
            // truncated down to 2 digits
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
    
    /**Pathway related methods added by the PowerTeam*/
    public static LWPathwayInspector getPathwayInspector(){
        return pathwayInspector;
    }
    
    /*public static PathwayControl getPathwayControl()
    {
        return control;
    }*/
    
    /**End of pathway related methods*/
    
    /**Hierarchy View related method*/
    public static LWHierarchyTree getHierarchyTree() 
    {
        return hierarchyTree;
    }
    
    /**End of hierarchy view related method*/
    
    /**Overview related method*/
    public static LWOutlineView getOutlineViewTree()
    {
        return outlineView;
    }
    
    /**End of overview related method*/
    
    private VUE() {}
    
    static JPanel toolPanel;//todo: tmp hack
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
        
        // loading preferences
        prefs = java.util.prefs.Preferences.userRoot().node("/");
        try {
            FileInputStream fis = new FileInputStream(getResource(VUE_CONF).getPath());
            prefs.importPreferences(fis);
        } catch (Exception e) { System.out.println(e);}

        //-------------------------------------------------------
        // Create the tabbed pane for the viewers
        //-------------------------------------------------------

        mMapTabsLeft = new MapTabbedPane();
        mMapTabsLeft.setTabPlacement(SwingConstants.BOTTOM);
        mMapTabsLeft.setPreferredSize(new Dimension(300,400));
        
        mMapTabsRight = new MapTabbedPane();
        mMapTabsRight.setTabPlacement(SwingConstants.BOTTOM);
        mMapTabsRight.setPreferredSize(new Dimension(300,400));

        if (true||args.length < 1) { // pathway code currently blowing us out unless we have these maps loaded
            //-------------------------------------------------------
            // Temporary: create example map(s)
            //-------------------------------------------------------
            //LWMap map1 = new LWMap("Test Nodes");
            //LWMap map2 = new LWMap("Example Map");
            //LWMap map1 = new LWMap("Map 1");
            LWMap map2 = new LWMap("Map 2");

            //installExampleNodes(map1);
            installExampleMap(map2);

            //map1.setFillColor(new Color(255, 255, 192));
            
            //displayMap(map1);
            displayMap(map2);
            
        }
        
        
        //-------------------------------------------------------
        // create a an application frame and layout components
        //-------------------------------------------------------
        
        toolPanel = new JPanel();
        //JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        //DRBrowser drBrowser = new DRBrowser();
        DRBrowser drBrowser = null;
        if (args.length < 1 || !args[0].equals("-nodr"))  {
            drBrowser = new DRBrowser();
            toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        }
			        
        // DEMO FIX:
        // no lwinspector in left
        //toolPanel.add(new LWCInspector(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        //splitPane.setLeftComponent(leftScroller);

        viewerSplit = new JSplitPane();
        viewerSplit.setOneTouchExpandable(true);
        viewerSplit.setRightComponent(mMapTabsRight);
        // NOTE: set left component AFTER set right component -- the
        // LAST set left/right call determines the default focus component!
        // It needs to be the LEFT component as the right one isn't
        // even visible at startup!
        viewerSplit.setLeftComponent(mMapTabsLeft);
        viewerSplit.setResizeWeight(0.5);
        viewerSplit.setDividerLocation(9999);

        //splitPane.setRightComponent(mMapTabsLeft);
        splitPane.setRightComponent(viewerSplit);

        frame = new VueFrame();
        JPanel vuePanel = new VuePanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        
        // Create the tool windows
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner());

        ToolWindow inspectorTool = new ToolWindow("Inspector", frame);
        inspectorTool.addTool(new LWCInspector());
        
        ToolWindow drBrowserTool  = new ToolWindow("DR Browser", frame);
        if (drBrowser != null)
            drBrowserTool.addTool(drBrowser);
        
        // The real tool palette window withtools and contextual tools
        ToolWindow toolbarWindow = new ToolWindow( VueResources.getString("tbWindowName"), frame);
        VueToolbarController tbc = VueToolbarController.getController();
        tbc.setToolWindow( toolbarWindow);
        toolbarWindow.getContentPane().add( tbc.getToolbar() );
        toolbarWindow.pack();

        boolean scottHack =
            System.getProperty("user.name").equals("sfraize") &&
            System.getProperty("scottHackDisabled") == null;
        // Need to factor some stuff out for the moment as has some bugs -- SMF 2003-12-29 21:32.39 Monday

        if (!scottHack) ModelSelection.addListener(tbc);
        
        frame.getContentPane().add( tbc.getToolbar(), BorderLayout.NORTH);
		
		// Map Inspector
		
		// get the proper scree/main frame size
		ToolWindow mapInspector = new ToolWindow(  VueResources.getString("mapInspectorTitle"), frame);
		MapInspectorPanel mip = new MapInspectorPanel();
		if (!scottHack) ModelSelection.addListener( mip);
		mapInspector.addTool( mip );
		
		//ToolWindow objectInspector = new ToolWindow( VueResources.getString("objectInspectorTitle"), frame);
		objectInspector = new ToolWindow( VueResources.getString("objectInspectorTitle"), frame);
		objectInspectorPanel = new ObjectInspectorPanel();
		if (!scottHack) ModelSelection.addListener(objectInspectorPanel);
		sResourceSelection.addListener( objectInspectorPanel);
		objectInspector.addTool(objectInspectorPanel);
		
		
		if( false) {
			JFrame testFrame = new JFrame("Debug");
			testFrame.setSize( 300,300);
			//testFrame.getContentPane().add( new NodeInspectorPanel() );
			testFrame.getContentPane().add(objectInspectorPanel);
			testFrame.show();
		}
		
		
        //addtion by the power team
        pathwayInspector = new LWPathwayInspector(frame);
        //control = new PathwayControl(frame);
        
        hierarchyTree = new LWHierarchyTree(frame);
        outlineView = new LWOutlineView(frame);
        //end of addition
       
        Window[] toolWindows = {
            toolbarWindow,
            pannerTool,
            inspectorTool,
            drBrowserTool,
            pathwayInspector,
            hierarchyTree,
            mapInspector,
            objectInspector,
            outlineView,
        };
        
        // adding the menus and toolbars
        setMenuToolbars(frame, toolWindows);
        System.out.println("after setting menu toolbars...");
        frame.getContentPane().add(vuePanel,BorderLayout.CENTER);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.pack();

        Dimension d = frame.getToolkit().getScreenSize();
        int x = d.width/2 - frame.getWidth()/2;
        int y = d.height/2 - frame.getHeight()/2;
        frame.setLocation(x, y);
        
        // position inspectors pased on frame location
        int inspectorx = x + frame.getWidth() - mapInspector.getWidth();
        mapInspector.setLocation( inspectorx, y);
        objectInspector.setLocation( inspectorx, y + mapInspector.getHeight() );
        
        
        
        frame.show();
        System.out.println("after showing frame...");
        if (args.length > 0) {
            try {
                OpenAction oa = null;
                for (int i = 0; i < args.length; i++) {
                    if (args[i].charAt(0) == '-')
                        continue;
                    if (oa == null)
                        oa = new OpenAction();
                    VUE.activateWaitCursor();
                    LWMap map = oa.loadMap(args[i]);
                    if (map != null)
                        displayMap(map);
                }
            } finally {
                VUE.clearWaitCursor();
            }
        }
        //setViewerScrollbarsDisplayed(true);
        System.out.println("VUE.main completed.");
    }

    public static void setViewerScrollbarsDisplayed(boolean add)
    {
        if (add) {
            JScrollPane scroller = new JScrollPane(mMapTabsLeft.getComponentAt(0));
            //scroller.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
            mMapTabsLeft.addTab("scrolling test", scroller);
            //mMapTabsLeft.setComponentAt(0, scroller);
        }
    }

    public static int openMapCount()
    {
        return mMapTabsLeft.getTabCount();
    }
    
    public static void setActiveViewer(MapViewer viewer)
    {
        ActiveViewer = viewer;
    }

    public static boolean multipleMapsVisible()
    {
        if (viewerSplit == null)
            return false;
        int dl = viewerSplit.getDividerLocation();
        return dl >= viewerSplit.getMinimumDividerLocation()
            && dl <= viewerSplit.getMaximumDividerLocation();
            
    }
    
    public static MapViewer getActiveViewer()
    {
        /*   
        Object c = mMapTabsLeft.getSelectedComponent();
        if(c instanceof JScrollPane){
            
            String title = mMapTabsLeft.getTitleAt(mMapTabsLeft.getSelectedIndex());
            title = title.substring(0, title.length()-4);
            for(int i = 0; i < mMapTabsLeft.getTabCount(); i++){
                if(mMapTabsLeft.getTitleAt(i).equals(title)){
                    return (MapViewer) mMapTabsLeft.getComponentAt(i);
                }
            }
        } 
        */
        // don't know how this will impact the pathway stuff, but we're (?)
        // ActiveViewer now has to be maintained seperately, so we
        // can't query the tabbed panes.
        return ActiveViewer;

    }
    
    public static JTabbedPane getTabbedPane(){
        return mMapTabsLeft;
    }

    public static LWMap getActiveMap()
    {
        if (getActiveViewer() != null)
            return getActiveViewer().getMap();
        else
            return null;
    }

    /*
    public static void addViewer(MapViewer viewer)
    {
        mMapTabsLeft.addTab(viewer.getMap().getLabel(), viewer);
    }
    */
    
    /*
    public static void closeViewer(Component c)
    {
        // todo: as closeMap
        mMapTabsLeft.remove(c);
        mMapTabsRight.remove(c);
    }
    */

    
    public static void closeMap(LWMap map)
    {
        // TODO: check for modifications and ask for save!
        mMapTabsLeft.closeMap(map);
        mMapTabsRight.closeMap(map);
    }

    static class MapTabbedPane extends JTabbedPane
        implements LWComponent.Listener
    {
        public void addTab(LWMap map, Component c)
        {
            super.addTab(map.getLabel(), c);
            map.addLWCListener(this);
            // todo perf: we should be able to ask to listen only
            // for events from this object directly (that we don't
            // care to hear from it's children), and even that
            // we'd only like to see, e.g., LABEL events.
            // -- create bit masks in LWCEvent
        }

        public void LWCChanged(LWCEvent e)
        {
            LWComponent c = e.getComponent();
            if (c instanceof LWMap && e.getWhat().equals("label")) {
                //System.out.println("MapTabbedPane " + e);
                int i = findTabWithMap((LWMap)c);
                if (i >= 0)
                    setTitleAt(i, c.getLabel());
            }
        }

        private int findTabWithMap(LWMap map)
        {
            int tabs = getTabCount();
            for (int i = 0; i < tabs; i++) {
                MapViewer viewer = (MapViewer) getComponentAt(i);
                if (viewer.getMap() == map)
                    return i;
            }
            return -1;
        }

        public void closeMap(LWMap map)
        {
            remove(findTabWithMap(map));
        }
    }
    

    public static void displayMap(LWMap map)
    {
        //System.out.println("VUE.displayMap " + map);
        MapViewer mapViewer = null;
        // todo: figure out if we're already displaying this map

        //System.out.println("VUE.displayMap Looking for " + map.getFile());
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            MapViewer mv = (MapViewer) mMapTabsLeft.getComponentAt(i);
            File existingFile = mv.getMap().getFile();
            //System.out.println("VUE.displayMap matching " + existingFile);
            if (existingFile != null && existingFile.equals(map.getFile())) {
                mapViewer = mv;
                System.err.println("VUE.displayMap found existing open map " + map + " in " + mv);
                // TODO: pop dialog asking to revert existing if there any changes.
                //break;
            }
        }
        
        final boolean useScrollbars = false; // in-progress feature
        JScrollPane sp = null;
        if (mapViewer == null) {

            mapViewer = new tufts.vue.MapViewer(map);
            System.out.println("VUE.displayMap: currently active viewer: " + getActiveViewer());
            if (getActiveViewer() == null)
                setActiveViewer(mapViewer);// unless null, wait till viewer gets focus
            System.out.println("VUE.displayMap:      created new viewer: " + mapViewer);
            if (useScrollbars)
                mMapTabsLeft.addTab(map, sp = new JScrollPane(mapViewer));
            else
                mMapTabsLeft.addTab(map, mapViewer);

            // put BACKINGSTORE mode on a diag switch and test
            // performance difference -- the obvious difference is
            // vastly better performance if an inspector window is
            // obscuring any part of the canvas (or any other window
            // for that mater), which kills off a huge chunk of
            // BLIT_SCROLL_MODE's optimization.  However, using
            // backing store completely fucks up if we start
            // hand-panning the map, tho I'm presuming that's because
            // the hand panning isn't being done thru the viewport
            // yet.
            //
            //sp.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
            
            MapViewer mv2 = new tufts.vue.MapViewer(map, true);
            mMapTabsRight.addTab(map, mv2);

            //mMapTabsLeft.requestFocus();
            
        }
         
        int idx = mMapTabsLeft.indexOfComponent(mapViewer);
        /*
        //mMapTabsLeft.setBackgroundAt(idx, Color.blue);
        mMapTabsLeft.setForegroundAt(mMapTabsLeft.getSelectedIndex(), Color.black);
        mMapTabsLeft.setForegroundAt(idx, Color.blue);
        // need to add a listener to change colors -- PC gui feedback of which
        // tab is selected is completely horrible.
        */
        if (useScrollbars)
            mMapTabsLeft.setSelectedComponent(sp);
        else
            mMapTabsLeft.setSelectedComponent(mapViewer);

    }
    
    private static void  setMenuToolbars(JFrame frame, Window[] toolWindows)
    {
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        Color menuColor = VueResources.getColor( "menubarColor");
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground( menuColor);
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setBackground( menuColor);
        
        JMenu editMenu = new JMenu("Edit");
        editMenu.setBackground( menuColor);
        
        JMenu viewMenu = new JMenu("View");
        viewMenu.setBackground( menuColor);
        
        JMenu formatMenu = new JMenu("Format");
        formatMenu.setBackground( menuColor);
        
        JMenu arrangeMenu = new JMenu("Arrange");
        arrangeMenu.setBackground( menuColor);
        
        JMenu alignMenu = new JMenu("Align");
        alignMenu.setBackground(menuColor);

        JMenu windowMenu = new JMenu("Window");
        windowMenu.setBackground( menuColor);
        
        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setBackground( menuColor);
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setBackground( menuColor);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        menuBar.add(arrangeMenu);
        menuBar.add(optionsMenu);
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);
        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open");
        ExitAction exitAction = new ExitAction("Quit");
        
        /**Actions added by the power team*/
        JMenu exportMenu = new JMenu("Export");
        
        PDFTransform pdfAction = new PDFTransform("PDF");
        HTMLConversion htmlAction = new HTMLConversion("HTML");
        ImageConversion imageAction = new ImageConversion("JPEG");
        ImageMap imageMap = new ImageMap("IMAP");
        SVGConversion svgAction = new SVGConversion("SVG");
        PrintAction printAction = new PrintAction("Print");
        XMLView xmlAction = new XMLView("XML View");
        
        exportMenu.add(htmlAction);
        exportMenu.add(pdfAction);
        exportMenu.add(imageAction);
        exportMenu.add(svgAction);
        exportMenu.add(xmlAction);
        exportMenu.add(imageMap);
        /**End of addition*/
        
        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));
        fileMenu.add(Actions.CloseMap);
        fileMenu.add(printAction);
        fileMenu.add(exportMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitAction);
        
        editMenu.add(Actions.Undo);
        editMenu.add(Actions.Redo);
        editMenu.addSeparator();
        editMenu.add(Actions.NewNode);
        editMenu.add(Actions.NewText);
        editMenu.add(Actions.Rename);
        editMenu.add(Actions.Duplicate);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.DeselectAll);
        
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomActual);
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Resources"));
        viewMenu.add(new JMenuItem("Collection"));
        viewMenu.add(new JMenuItem("Inspector"));
        viewMenu.add(new JMenuItem("Pathway"));
        viewMenu.add(new JMenuItem("Toolbar"));
        viewMenu.add(new JMenuItem("Overview"));
        
        JMenu fontMenu = new JMenu("Font");

        /*
        // this list bigger than screen & menu isn't scrolling for us!
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        System.out.println(java.util.Arrays.asList(fonts));
        for (int i = 0; i < fonts.length; i++) {
            JMenuItem fm = new JMenuItem(fonts[i]);
            fontMenu.add(fm);
        }
        */
                           
        //formatMenu.add(fontMenu);
        formatMenu.add(Actions.FontSmaller);
        formatMenu.add(Actions.FontBigger);
        formatMenu.add(Actions.FontBold);
        formatMenu.add(Actions.FontItalic);
        //formatMenu.add(new JMenuItem("Size"));
        //formatMenu.add(new JMenuItem("Style"));
        //formatMenu.add("Text Justify").setEnabled(false);
        // TODO: ultimately better to break these out in to Node & Link submenus
        formatMenu.addSeparator();
        for (int i = 0; i < Actions.NODE_MENU_ACTIONS.length; i++) {
            Action a = Actions.NODE_MENU_ACTIONS[i];
            if (a == null)
                formatMenu.addSeparator();
            else
                formatMenu.add(a);
        }
        formatMenu.addSeparator();
        for (int i = 0; i < Actions.LINK_MENU_ACTIONS.length; i++) {
            Action a = Actions.LINK_MENU_ACTIONS[i];
            if (a == null)
                formatMenu.addSeparator();
            else
                formatMenu.add(a);
        }

        for (int i = 0; i < Actions.ALIGN_MENU_ACTIONS.length; i++) {
            Action a = Actions.ALIGN_MENU_ACTIONS[i];
            if (a == null)
                alignMenu.addSeparator();
            else
                alignMenu.add(a);
        }
        
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        arrangeMenu.addSeparator();
        arrangeMenu.add(Actions.Group);
        arrangeMenu.add(Actions.Ungroup);
        arrangeMenu.addSeparator();
        arrangeMenu.add(alignMenu);
        
        for (int i = 0; i < toolWindows.length; i++) {
            //System.out.println("adding " + toolWindows[i]);
            Window window = toolWindows[i];
            WindowDisplayAction windowAction = new WindowDisplayAction(window);
            JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
            windowAction.setLinkedButton(checkBox);
            windowMenu.add(checkBox);
        }

        optionsMenu.add(new JMenuItem("Node Types..."));
        optionsMenu.add(new JMenuItem("Map Preference..."));
        optionsMenu.add(new JMenuItem("Preferences..."));
        
        helpMenu.add(new JMenuItem("Help"));
        
        JToolBar toolBar = new JToolBar();
        toolBar.add(Actions.NewMap);
        toolBar.add(openAction);
        toolBar.add(Actions.CloseMap);
        toolBar.add(saveAction);
        toolBar.add(saveAsAction);
        toolBar.add(printAction);
        toolBar.add(imageAction);
        toolBar.add(htmlAction);
        toolBar.add(xmlAction);
        toolBar.add(pdfAction);
        toolBar.add(imageMap);
        toolBar.add(svgAction);
       // toolBar.add(new JButton(new ImageIcon("tufts/vue/images/ZoomOut16.gif")));
        toolBar.add(new JButton(new PolygonIcon(Color.RED)));
        frame.setJMenuBar(menuBar);
        //frame.getContentPane().add(toolBar,BorderLayout.NORTH);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});

    }
    
    static class WindowDisplayAction extends AbstractAction
    {
        AbstractButton mLinkedButton;
        Window mWindow;
        public WindowDisplayAction(Window w)
        {
            super("window: " + w.getName());
            if (w instanceof Frame)
                putValue(Action.NAME, ((Frame)w).getTitle());
            else if (w instanceof Dialog)
                putValue(Action.NAME, ((Dialog)w).getTitle());
            else if (w instanceof ToolWindow)
                putValue(Action.NAME, ((ToolWindow)w).getTitle());
            mWindow = w;
            mWindow.addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) { /*System.out.println(e);*/ setButtonState(true); }
                    public void componentHidden(ComponentEvent e) { /*System.out.println(e);*/ setButtonState(false); }
                });
        }
        void setLinkedButton(AbstractButton b) {
            mLinkedButton = b;
        }
        private void setButtonState(boolean tv) {
            if (mLinkedButton != null)
                mLinkedButton.setSelected(tv);
        }
        public void actionPerformed(ActionEvent e) {
            if (mLinkedButton == null)
                mLinkedButton = (AbstractButton) e.getSource();
            mWindow.setVisible(mLinkedButton.isSelected());
        }
    }


    static void installExampleNodes(LWMap map)
    {
        map.setFillColor(new Color(255,255,220));
        
        map.addLWC(new LWNode("Oval", 0)).setFillColor(Color.red);
        map.addLWC(new LWNode("Circle", 1)).setFillColor(Color.green);
        map.addLWC(new LWNode("Square", 2)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Rectangle", 3)).setFillColor(Color.blue);
        map.addLWC(new LWNode("Rounded Rectangle", 4)).setFillColor(Color.yellow);
        
        LWNode triangle = new LWNode("Triangle", 5);
        triangle.setAutoSized(false);
        triangle.setSize(60,60);
        triangle.setFillColor(Color.orange);
        map.addLWC(triangle);
        //map.addLWC(new LWNode("Triangle", 5)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Diamond", 6)).setFillColor(Color.yellow);
        
        map.addNode(new LWNode("One"));
        map.addNode(new LWNode("Two"));
        map.addNode(new LWNode("Three"));
        map.addNode(new LWNode("Four"));
        map.addNode(new LWNode("WWWWWWWWWWWWWWWWWWWW"));
        map.addNode(new LWNode("iiiiiiiiiiiiiiiiiiii"));
        
        map.addNode(NodeTool.createTextNode("jumping"));

        // Experiment in internal actions -- only works
        // partially here because they're all auto sized
        // based on text, and since haven't been painted yet,
        // and so don't really know their size.
        // Addendum: with new TextBox, above no longer true.
        LWSelection s = new LWSelection();
        s.setTo(map.getChildIterator());
        Actions.MakeColumn.act(s);
        s.clear(); // clear isSelected bits
    }
    
    static void installExampleMap(LWMap map)
    {
        /*
         * create some test nodes & links
         */
        LWNode n1 = new LWNode("Google", new Resource("http://www.google.com/"));
        LWNode n2 = new LWNode("Program Files", new Resource("C:\\Program Files"));
        LWNode n3 = new LWNode("readme.txt", new Resource("readme.txt"));
        LWNode n4 = new LWNode("Slash", new Resource("file:///"));
        n1.setLocation(100, 30);
        n2.setLocation(100, 100);
        n3.setLocation(50, 180);
        n4.setLocation(200, 180);
        n4.setNotes("I am a note.");
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        LWLink k1 = new LWLink(n1, n2);
        LWLink k2 = new LWLink(n2, n3);
        LWLink k3 = new LWLink(n2, n4);
        k1.setLabel("Link label");
        k1.setNotes("I am link note");
        k3.setControlCount(1);
        k2.setControlCount(2);
        map.addLink(k1);
        map.addLink(k2);
        map.addLink(k3);
        
        //creating test pathways
        if(map.getLabel().equals("Map 1")){
            LWPathway p1 = new LWPathway("Pathway 1");
            LinkedList linkedlist = new LinkedList();
            linkedlist.add(n1);
            linkedlist.add(n2);
            linkedlist.add(n3);
            linkedlist.add(k1);
            p1.setElementList(linkedlist);
            map.addPathway(p1);
        }/*else if(map.getLabel().equals("Test Nodes")){        
            LWPathway p2 = new LWPathway("Pathway 2");
            
            p2.setComment("A comment.");
            LinkedList anotherList = new LinkedList();
            anotherList.add(n3);
            anotherList.add(n4);
            anotherList.add(n2);
            anotherList.add(k2);
            anotherList.add(k3);
            p2.setElementList(anotherList);
            map.addPathway(p2);            
        }*/
    }
}
