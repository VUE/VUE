package tufts.vue;

import tufts.vue.action.*;

import java.util.*;
import java.util.prefs.*;
import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 */

public class VUE
    implements VueConstants
{
    public static final String VUE_CONF = "vue.conf";
    
    // preferences for the application
    //public static Preferences prefs;
    
    /** The currently active viewer (e.g., is visible
     * and has focus).  Actions (@see Actions.java) are performed on
     * the active model (sometimes querying the active viewer). */
    private static MapViewer ActiveViewer = null;
    /** The currently active selection.
     * elements in ModelSelection should always be from the ActiveModel */
    static final LWSelection ModelSelection = new LWSelection();
    
    /** teh global resource selection static model **/
    public final static ResourceSelection sResourceSelection = new ResourceSelection();
    
    public static JFrame frame;
    
    private static MapTabbedPane mMapTabsLeft;
    private static MapTabbedPane mMapTabsRight;
    private static JSplitPane viewerSplit;
    
    static ToolWindow sMapInspector;
    static ToolWindow objectInspector;
    static ObjectInspectorPanel objectInspectorPanel;
    static ToolWindow aboutusTool;
    //private static MapInspectorPanel sMapInspectorPanel;
    
    //hierarchy view tree window component
    private static LWHierarchyTree hierarchyTree;
    
    //overview tree window component
    public static LWOutlineView outlineView;
    
    //public static DataSourceViewer dataSourceViewer;
    public static FavoritesWindow favoritesWindow;
    public static boolean  dropIsLocal = false;
    
    private static java.util.List sActiveMapListeners = new java.util.ArrayList();
    private static java.util.List sActiveViewerListeners = new java.util.ArrayList();
    public interface ActiveMapListener {
        public void activeMapChanged(LWMap map);
    }
    public interface ActiveViewerListener {
        public void activeViewerChanged(MapViewer viewer);
    }
    
    public static java.net.URL getResource(String name) {
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
    
    static class VueFrame extends JFrame
        implements MapViewer.Listener
    {
        final int TitleChangeMask =
            MapViewerEvent.DISPLAYED |
            MapViewerEvent.FOCUSED;
            //MapViewerEvent.ZOOM;        // title includes zoom
        
        VueFrame() {
            super("VUE: Visual Understanding Environment");
            //System.out.println("LOADING IMAGE"+Toolkit.getDefaultToolkit().getImage(VueResources.getURL("vueIcon16x16")));
            //setIconImage(Toolkit.getDefaultToolkit().getImage(VueResources.getURL("vueIcon32x32")));
            // On the PC, this is bombing out for me and halting the whole vue application -- doesn't
            // look like the case in the filename matches -- SMF
        }
        
        /** never let the frame be hidden -- always ignored */
        public void setVisible(boolean tv) {
            System.out.println("VueFrame setVisible " + tv);
            
            // The frame should never be "hidden" -- iconification
            // doesn't trigger that (nor Mac os "hide") -- so if we're
            // here the OS window manager is attempting to hide us
            // (the 'x' button on the window frame).
            
            super.setVisible(true);
        }
        public void mapViewerEventRaised(MapViewerEvent e) {
            if ((e.getID() & TitleChangeMask) != 0)
                setTitleFromViewer(e.getMapViewer());
        }
        
        private void setTitleFromViewer(MapViewer viewer) {
            setTitle("VUE: " + viewer.getMap().getLabel());
            //setTitle("VUE: " + getViewerTitle(viewer));
        }
        
        private String getViewerTitle(MapViewer viewer) {
            String title = viewer.getMap().getLabel();
            
            int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
            // Present the zoom factor as a percentange
            // truncated down to 2 digits
            title += " (";
            if ((displayZoom / 100) * 100 == displayZoom)
                title += (displayZoom / 100) + "%";
            else
                title += (((float) displayZoom) / 100f) + "%";
            title += ")";
            return title;
        }
    }
    
    
    static {
        if (false && VueUtil.isMacPlatform()) {
            final String usmbProp = "apple.laf.useScreenMenuBar";
            if (System.getProperty(usmbProp) == null)
                System.setProperty(usmbProp, "true");
            /*
            final String appNameProp = "com.apple.mrj.application.apple.menu.about.name";
            // setting appNameProp here doesn't do anything anything since VM
            // has already made use of this property...
            System.setProperty(appNameProp, "VUE");
             */
        }
    }
    
    public static LWSelection getSelection() {
        return ModelSelection;
    }
    
    private static Cursor oldCursor;
    public static synchronized void activateWaitCursor() {
        /*
        JRootPane rp = SwingUtilities.getRootPane(VUE.frame);
        if (oldCursor != null)
            System.out.println("multiple wait-cursors");
        else if (getActiveViewer() != null)
            oldCursor = getActiveViewer().getCursor();
        //oldCursor = rp.getCursor();
        // okay -- need to get this from the MapViewer..
        System.out.println("GOT OLD CURSOR " + oldCursor);
        rp.setCursor(CURSOR_WAIT);
         */
    }
    public static synchronized void clearWaitCursor() {
        /*
        if (getActiveViewer() != null)
            getActiveViewer().setCursor(oldCursor);
        SwingUtilities.getRootPane(VUE.frame).setCursor(oldCursor);
        System.out.println("USED OLD CURSOR " + oldCursor);
        oldCursor = null;
         */
    }
    
    
    /*public static LWPathwayInspector getPathwayInspector(){
        return pathwayInspector;
        }*/
    public static LWPathway getActivePathway() {
        LWPathway p = null;
        if (getActiveMap() != null && getActiveMap().getPathwayList() != null)
            p = getActiveMap().getPathwayList().getActivePathway();
        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println("getActivePathway: " + p);
        return p;
    }
    
    /*public static PathwayControl getPathwayControl()
    {
        return control;
    }*/
    
    /**End of pathway related methods*/
    
    /**Hierarchy View related method*/
    public static LWHierarchyTree getHierarchyTree() {
        return hierarchyTree;
    }
    
    /**End of hierarchy view related method*/
    
    /**Overview related method*/
    public static LWOutlineView getOutlineViewTree() {
        return outlineView;
    }
    
    /**End of overview related method*/
    
    private VUE() {}
    
    static void initUI() {
        initUI(false);
    }
    
    static void initUI(boolean debug)
    {
        if (!themeSet)
            MetalLookAndFeel.setCurrentTheme(VueTheme.getTheme());
        
        String lafn = null;
        //lafn = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //lafn = "javax.swing.plaf.basic.BasicLookAndFeel"; // not a separate L&F -- baseclass
        //if (debug)
        if (VueUtil.isMacPlatform()) // for now for testing
            lafn = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            if (lafn != null)
                javax.swing.UIManager.setLookAndFeel(lafn);
            //javax.swing.UIManager.setLookAndFeel(new VueLookAndFeel());
        } catch (Exception e) {
            System.err.println(e);
        }
        /*
        LookAndFeel laf = UIManager.getLookAndFeel();
        String lafName = laf.getName();
        System.out.println("LookAndFeel: \"" + lafName + "\" " + laf);
        if (lafName.equals("Metal") || lafName.equals("Windows")) {
        // may want to break out default setter if for overrides of windows look & feel
            UIManager.getLookAndFeelDefaults().put("TabbedPane.background", Color.lightGray);
            ...
        */
        
    }
    
    
    private static JPanel toolPanel;
    private static boolean themeSet = false;
    public static void main(String[] args) {
        System.out.println("VUE:main");
        
        // Must install theme before any other GUI code or our VueTheme gets ignored
        MetalLookAndFeel.setCurrentTheme(VueTheme.getTheme());
        themeSet = true;

        boolean nodr = (args.length > 0 && args[0].equals("-nodr"));
        Window splashScreen = null;

        if (nodr)
            DEBUG.Enabled = true;
        else
            splashScreen = new SplashScreen();
        
        initUI();
        
        //-------------------------------------------------------
        // Create the tabbed pane for the viewers
        //-------------------------------------------------------
        
        mMapTabsLeft = new MapTabbedPane("*left");
        mMapTabsLeft.setTabPlacement(SwingConstants.BOTTOM);
        mMapTabsLeft.setPreferredSize(new Dimension(300,400));
        
        mMapTabsRight = new MapTabbedPane("right");
        mMapTabsRight.setTabPlacement(SwingConstants.BOTTOM);
        mMapTabsRight.setPreferredSize(new Dimension(300,400));
        
        //-------------------------------------------------------
        // create a an application frame and layout components
        //-------------------------------------------------------
        
        toolPanel = new JPanel();
        //toolPanel.setMinimumSize(new Dimension(329,1)); // until DRBrowser loaded
        toolPanel.setLayout(new BorderLayout());
        DRBrowser drBrowser = null;
        if (!nodr)  {
            drBrowser = new DRBrowser(true);
            toolPanel.add(drBrowser, BorderLayout.CENTER);
            /*
            try {
                drBrowser = new DRBrowser();
                toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
            } catch (Throwable e) {
                e.printStackTrace();
                System.err.println("DR browser blowing up -- try another day.");
            }
            */
        } else {
            //-------------------------------------------------------
            // create example map(s)
            //-------------------------------------------------------
            //LWMap map1 = new LWMap("Map 1");
            LWMap map2 = new LWMap("Map 2");
            
            //installExampleNodes(map1);
            installExampleMap(map2);
            
            //map1.setFillColor(new Color(255, 255, 192));
            
            //displayMap(map1);
            displayMap(map2);
        }
        
        JSplitPane splitPane = new JSplitPane();
        //splitPane.setResizeWeight(0.40); // 25% space to the left component
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

        viewerSplit.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    //System.out.println("VS " + e);
                    if (!e.getPropertyName().equals("dividerLocation"))
                        return;
                    if (DEBUG.FOCUS) out("viewerSplit: " + e.getPropertyName()
                                       + "=" + e.getNewValue().getClass().getName()
                                       + " " + e.getNewValue());
                    if (multipleMapsVisible() == false) {
                        MapViewer leftViewer = mMapTabsLeft.getSelectedViewer();
                        if (leftViewer != null && leftViewer != getActiveViewer()) {
                            if (DEBUG.FOCUS) out("viewerSplit: default focus to " + leftViewer);
                            leftViewer.requestFocus();
                            //viewer.grabVueApplicationFocus("split-pane-other-hidden");
                            if (mMapTabsRight != null) {
                                MapViewer rightViewer = mMapTabsRight.getSelectedViewer();
                                if (rightViewer != null)
                                    rightViewer.fireViewerEvent(MapViewerEvent.HIDDEN);
                            }
                        }
                    }
                    
                }});
        
        
        //splitPane.setRightComponent(mMapTabsLeft);
        splitPane.setRightComponent(viewerSplit);
        frame = new VueFrame();
        //JPanel vuePanel = new AAPanel();
        JPanel vuePanel = new JPanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        
        // Create the tool windows
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner());
        
        ToolWindow inspectorTool = null;
        if (nodr) {
            inspectorTool = new ToolWindow("Inspector", frame);
            inspectorTool.addTool(new LWCInspector());
        }
        
        ToolWindow drBrowserTool = null;
        //DataSourceViewer currently breaks if more than one DRBrowser
        //ToolWindow drBrowserTool = new ToolWindow("Data Sources", frame);
        //if (drBrowser != null) drBrowserTool.addTool(drBrowser);
        
        // The real tool palette window withtools and contextual tools
        ToolWindow toolbarWindow = null;
        VueToolbarController tbc = VueToolbarController.getController();
        ModelSelection.addListener(tbc);
        /*
        ToolWindow toolbarWindow = new ToolWindow( VueResources.getString("tbWindowName"), frame);
        tbc.setToolWindow( toolbarWindow);
        toolbarWindow.getContentPane().add( tbc.getToolbar() );
        toolbarWindow.pack();
         */
        
        frame.getContentPane().add( tbc.getToolbar(), BorderLayout.NORTH);
        
        // Map Inspector
        
        // get the proper scree/main frame size
        sMapInspector = new ToolWindow(  VueResources.getString("mapInspectorTitle"), frame);
        MapInspectorPanel mi = new MapInspectorPanel();
        sMapInspector.addTool(mi);
        
        //ToolWindow objectInspector = new ToolWindow( VueResources.getString("objectInspectorTitle"), frame);
        objectInspector = new ToolWindow( VueResources.getString("objectInspectorTitle"), frame);
        objectInspectorPanel = new ObjectInspectorPanel();
        ModelSelection.addListener(objectInspectorPanel);
        sResourceSelection.addListener( objectInspectorPanel);
        objectInspector.addTool(objectInspectorPanel);
        
        
        if (false) {
            JFrame testFrame = new JFrame("Debug");
            testFrame.setSize(300,300);
            //testFrame.getContentPane().add( new NodeInspectorPanel() );
            testFrame.getContentPane().add(objectInspectorPanel);
            testFrame.show();
        }
        
        /*
        ToolWindow htWindow = new ToolWindow("Hierarchy Tree", frame);
        hierarchyTree = new LWHierarchyTree();
        htWindow.addTool(hierarchyTree);
         */
        
        outlineView = new LWOutlineView(frame);
        //end of addition
        
        Window[] toolWindows = {
            objectInspector,
            sMapInspector,
            drBrowserTool,
            toolbarWindow,
            pannerTool,
            //htWindow,
            outlineView,
            inspectorTool,
        };
        
        // adding the menus and toolbars
        setMenuToolbars(frame, toolWindows);
        out("menu toolbars set.");
        frame.getContentPane().add(vuePanel,BorderLayout.CENTER);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(VueResources.getURL("vueIcon32x32")));
        frame.pack();
        frame.setSize(800,600);// todo: make % of screen, make sure tool windows below don't go off screen!
        frame.validate();
        
        VueUtil.centerOnScreen(frame);
        
        // position inspectors pased on frame location
        //int inspectorx = frame.getX() + frame.getWidth() - sMapInspector.getWidth();
        int inspectorx = frame.getX() + frame.getWidth();
        sMapInspector.suggestLocation(inspectorx, frame.getY());
        objectInspector.suggestLocation(inspectorx, frame.getY() + sMapInspector.getHeight() );
        pannerTool.suggestLocation(frame.getX() - pannerTool.getWidth(), frame.getY());
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println(e);
                ExitAction.exitVue();
                //-------------------------------------------------------
                // if we get here, it means exit was aborted.
                // (something wasn't saved & they decided to cancel or
                // there was an error during the save)
                //-------------------------------------------------------
                //frame.show(); not working!  How to cancel this
                // windowClose?  According to WindowEvent.java &
                // WindowAdapter.java, canceling this windowClosing is
                // supposed to be possible, but they don't mention
                // how. Anyway, we've overriden setVisible on VueFrame
                // to make it impossible to hide it, and that works,
                // so this event just becomes the they've pressed on
                // the close button event.
                return;
            }
            public void windowClosed(WindowEvent e) {
                // I've never see us even get this event...
                System.err.println(e);
                System.err.println("Too late: window disposed: exiting.");
                System.exit(-1);
            }
            public void windowStateChanged(WindowEvent e) {
                System.out.println(e);
            }
        });
        
        frame.show();
        System.out.println("VUE: frame visible");
        
        if (!nodr) {
            try {
                File startupFile = new File(VueResources.getURL("resource.startmap").getFile());
                LWMap startupMap = OpenAction.loadMap(startupFile.getAbsolutePath());
                startupMap.setFile(null); // dissasociate startup map from it's file so we don't write over it
                startupMap.setLabel("Welcome");
                startupMap.markAsSaved();
                displayMap(startupMap);
            } catch(Exception ex) {
                VueUtil.alert(null, "Cannot load the Start up map", "Start Up Map Error");
                ex.printStackTrace();
            }
        } else {
            pannerTool.setVisible(true);
        }

        if (splashScreen != null)
            splashScreen.setVisible(false);

        if (args.length > 0) {
            try {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].charAt(0) == '-')
                        continue;
                    VUE.activateWaitCursor();
                    LWMap map = OpenAction.loadMap(args[i]);
                    if (map != null)
                        displayMap(map);
                }
            } finally {
                VUE.clearWaitCursor();
            }
        }
        
        if (drBrowser != null) {
            drBrowser.loadDataSourceViewer();
            splitPane.resetToPreferredSizes();
        }

        System.out.println("VUE.main: loading fonts...");
        FontEditorPanel.getFontNames();
        System.out.println("VUE.main completed.");

        if (drBrowser != null && drBrowserTool != null)
            drBrowserTool.addTool(new DRBrowser());
    }
    
    
    public static int openMapCount() {
        return mMapTabsLeft == null ? 0 : mMapTabsLeft.getTabCount();
    }
    
    public static void addActiveMapListener(ActiveMapListener l) {
        sActiveMapListeners.add(l);
    }
    public static void removeActiveMapListener(ActiveMapListener l) {
        sActiveMapListeners.remove(l);
    }
    public static void addActiveViewerListener(ActiveViewerListener l) {
        sActiveViewerListeners.add(l);
    }
    public static void removeActiveViewerListener(ActiveViewerListener l) {
        sActiveViewerListeners.remove(l);
    }
    
    
    /**
     * Viewer can be null, which happens when we close the active viewer
     * and until another grabs the application focus (unles it was the last viewer).
     */
    public static void setActiveViewer(MapViewer viewer) {
        // todo: does this make sense?
        //if (ActiveViewer == null || viewer == null || viewer.getMap() != ActiveViewer.getMap()) {
        if (ActiveViewer != viewer) {
            LWMap oldActiveMap = null;
            if (ActiveViewer != null)
                oldActiveMap = ActiveViewer.getMap();
            ActiveViewer = viewer;
            out("ActiveViewer set to " + viewer);
            if (ActiveViewer != null) {
                java.util.Iterator i = sActiveViewerListeners.iterator();
                while (i.hasNext())
                    ((ActiveViewerListener)i.next()).activeViewerChanged(viewer);
                if (oldActiveMap != ActiveViewer.getMap()) {
                    i = sActiveMapListeners.iterator();
                    while (i.hasNext())
                        ((ActiveMapListener)i.next()).activeMapChanged(viewer.getMap());
                }
            }
        } else {
            ActiveViewer = viewer;
        }
    }
    
    public static MapViewer getActiveViewer() {
        return ActiveViewer;
    }
    
    public static boolean multipleMapsVisible() {
        if (viewerSplit == null)
            return false;
        int dl = viewerSplit.getDividerLocation();
        return dl >= viewerSplit.getMinimumDividerLocation()
            && dl <= viewerSplit.getMaximumDividerLocation();
        
    }
    
    public static JTabbedPane getTabbedPane() {
        return mMapTabsLeft;
    }
    
    public static LWMap getActiveMap() {
        if (getActiveViewer() != null)
            return getActiveViewer().getMap();
        else
            return null;
    }

    /*
     * Returns instance of frame. used by orpahan dialogs
     * (what calls this?  I see no reference to it in any
     * VUE source -- does something using reflection call it?? --SMF)
     */
    public static JFrame getInstance() {
        return frame;
    }

    public static UndoManager getUndoManager() {
        LWMap map = getActiveMap();
        if (map != null)
            return map.getUndoManager();
        else
            return null;
    }
    
    public static boolean isOkayToExit() {
        int tabs = mMapTabsLeft.getTabCount();
        for (int i = 0; i < tabs; i++)
            if (!askSaveIfModified(mMapTabsLeft.getMapAt(i)))
                return false;
        return true;
    }
    
    /*
     * Returns true if either they save it or say go ahead and close w/out saving.
     */
    private static boolean askSaveIfModified(LWMap map) {
        final Object[] defaultOrderButtons = { "Save", "Don't Save", "Cancel"};
        // oddly, mac aqua is reversing order of these buttons
        final Object[] macAquaOrderButtons = { "Cancel", "Don't Save", "Save" };
        
        if (!map.isModified())
            return true;
        
        int response = JOptionPane.showOptionDialog
        (VUE.frame,
        
        "Do you want to save the changes you made to \n"
        + "'" + map.getLabel() + "'?"
        + (DEBUG.EVENTS?("\n[modifications="+map.getModCount()+"]"):""),
        
        " Save changes?",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        VueUtil.isMacAquaLookAndFeel() ? macAquaOrderButtons : defaultOrderButtons,
        "Save"
        );
        
        if (VueUtil.isMacAquaLookAndFeel())
            response = (macAquaOrderButtons.length-1) - response;
        
        if (response == JOptionPane.YES_OPTION) { // Save
            return SaveAction.saveMap(map);
        } else if (response == JOptionPane.NO_OPTION) { // Don't Save
            // don't save -- just close
            return true;
        } else // anything else (Cancel or dialog window closed)
            return false;
    }
    
    public static void closeMap(LWMap map) {
        // for now, we don't let them close the last open map as we get NPE's
        // all over the place if there's isn't an active map (we could have
        // a dummy map as a reasonable hack to solve the problem so everybody
        // doesn't have to check for a null active map)
        //if (mMapTabsLeft.getTabCount() > 1 && askSaveIfModified(map)) {
        if (askSaveIfModified(map)) {
            mMapTabsLeft.closeMap(map);
            mMapTabsRight.closeMap(map);
        }
    }
    
    
    public static void displayMap(LWMap pMap) {
        //System.out.println("VUE.displayMap " + map);
        MapViewer mapViewer = null;
        
        //System.out.println("VUE.displayMap Looking for " + map.getFile());
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            //System.out.println("VUE.displayMap matching " + existingFile);
            if (existingFile != null && existingFile.equals(pMap.getFile())) {
                //mapViewer = mv;
                System.err.println("VUE.displayMap found existing open map " + map);
                //System.err.println("VUE.displayMap found existing open map " + map + " in " + mv);
                // TODO: pop dialog asking to revert existing if there any changes.
                //break;
            }
        }
        
        if (mapViewer == null) {
            mapViewer = new tufts.vue.MapViewer(pMap);
            out("displayMap: currently active viewer: " + getActiveViewer());
            out("displayMap: created new left viewer: " + mapViewer);

            mMapTabsLeft.addViewer(mapViewer);
            mMapTabsRight.addViewer(new tufts.vue.MapViewer(pMap, true));

            //if (getActiveViewer() == null)
                //setActiveViewer(mapViewer);// unless null, wait till viewer gets focus
        }
        
        mMapTabsLeft.setSelectedComponent(mapViewer);
        
    }
    
    private static void  setMenuToolbars(JFrame frame, Window[] toolWindows) {
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = menuBar.add(new JMenu("File"));
        JMenu editMenu = menuBar.add(new JMenu("Edit"));
        JMenu viewMenu = menuBar.add(new JMenu("View"));
        JMenu formatMenu = menuBar.add(new JMenu("Format"));
        JMenu arrangeMenu = menuBar.add(new JMenu("Arrange"));
        JMenu windowMenu = menuBar.add(new JMenu("Tools"));
        JMenu alignMenu = menuBar.add(new JMenu("Arrange/Align"));
        //JMenu optionsMenu = menuBar.add(new JMenu("Options"))l
        JMenu helpMenu = menuBar.add(new JMenu("Help"));

        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open");
        ExitAction exitAction = new ExitAction("Quit");
        Publish publishAction = new Publish(frame,"Export");
        
        // Actions added by the power team
        PrintAction printAction = PrintAction.getPrintAction();
        PDFTransform pdfAction = new PDFTransform("PDF");
        HTMLConversion htmlAction = new HTMLConversion("HTML");
        ImageConversion imageAction = new ImageConversion("JPEG");
        ImageMap imageMap = new ImageMap("IMAP");
        SVGConversion svgAction = new SVGConversion("SVG");
        XMLView xmlAction = new XMLView("XML View");
        
        if (DEBUG.Enabled) {
            JButton u = new JButton(Actions.Undo);
            JButton r = new JButton(Actions.Redo);
            JButton p = new JButton(printAction);
            JButton v = new JButton(printAction);
            v.setText("Print View");
            
            u.setBackground(Color.white);
            r.setBackground(Color.white);
            menuBar.add(u).setFocusable(false);
            menuBar.add(r).setFocusable(false);
            menuBar.add(p).setFocusable(false);
            menuBar.add(v).setFocusable(false);
        }

        if (DEBUG.Enabled) {
            JMenu exportMenu = menuBar.add(new JMenu("Export"));
            exportMenu.add(htmlAction);
            exportMenu.add(pdfAction);
            exportMenu.add(imageAction);
            exportMenu.add(svgAction);
            exportMenu.add(xmlAction);
            exportMenu.add(imageMap);
        }
        
        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));
        fileMenu.add(Actions.CloseMap);
        fileMenu.add(printAction);
        fileMenu.add(printAction).setText("Print View...");
        fileMenu.add(publishAction);
        //fileMenu.add(exportMenu);
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
        /*
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Resources"));
        viewMenu.add(new JMenuItem("Collection"));
        viewMenu.add(new JMenuItem("Inspector"));
        viewMenu.add(new JMenuItem("Pathway"));
        viewMenu.add(new JMenuItem("Toolbar"));
        viewMenu.add(new JMenuItem("Overview"));
         */
        
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
        buildMenu(formatMenu, Actions.NODE_MENU_ACTIONS);
        formatMenu.addSeparator();
        buildMenu(formatMenu, Actions.LINK_MENU_ACTIONS);
        
        buildMenu(alignMenu, Actions.ARRANGE_MENU_ACTIONS);

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
            if (window == null)
                continue;
            WindowDisplayAction windowAction = new WindowDisplayAction(window);
            JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
            windowAction.setLinkedButton(checkBox);
            windowMenu.add(checkBox);
        }
        
        //windowMenu.add(new UserDataAction());
        /*
        optionsMenu.add(new UserDataAction());
        optionsMenu.add(new JMenuItem("Map Preference..."));
        optionsMenu.add(new JMenuItem("Preferences..."));
         */
        
        JMenuItem userGuide = new JMenuItem("User Guide");
        JMenuItem aboutUs = new JMenuItem("About Us");
        helpMenu.add(userGuide);
        helpMenu.add(aboutUs);
        aboutusTool = new ToolWindow("About Us",tufts.vue.VUE.getInstance());
        JPanel aboutusPanel = new JPanel();
        aboutusPanel.setMinimumSize(new Dimension(400,400));
        JTextArea jtf = new JTextArea("Version # 1.0 \n Created By:  Academic Technology \n Tufts University, Medford, MA \n Copyright@Tufts University 2004\nAll Rights Reserved ",5, 1);
        aboutusPanel.setLayout(new BorderLayout());
       
         aboutusPanel.add(jtf,BorderLayout.CENTER);
         aboutusTool.addTool(aboutusPanel);
       
        userGuide.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) 
               {
                   
               try{
                   VueUtil.openURL("http://vue.tccs.tufts.edu/userdoc/");
               }catch (Exception EX) {}
                   
               }    
                   
                       });
        
          aboutUs.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) 
               {
     
                aboutusTool.setLocation(200,200);
                aboutusTool.setVisible(true);
        
            }
        });
        
        
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
    }

    public static JMenu buildMenu(String name, Action[] actions) {
        return buildMenu(new JMenu(name), actions);
    }
    public static JMenu buildMenu(JMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        return menu;
    }
    
    static class WindowDisplayAction extends AbstractAction {
        AbstractButton mLinkedButton;
        Window mWindow;
        boolean firstDisplay = true;
        public WindowDisplayAction(Window w) {
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
            if (firstDisplay && mWindow.getX() == 0 && mWindow.getY() == 0) {
                mWindow.setLocation(20,20);
            }
            firstDisplay = false;
            mWindow.setVisible(mLinkedButton.isSelected());
        }
    }
    
    static void installExampleNodes(LWMap map) {
        map.setFillColor(new Color(255,255,220));

        /*
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
        */
        
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
    
    static void installExampleMap(LWMap map) {
        /*
         * create some test nodes & links
         */
        LWNode n1 = new LWNode("Google", new MapResource("http://www.google.com/"));
        LWNode n2 = new LWNode("Program Files", new MapResource("C:\\Program Files"));
        LWNode n3 = new LWNode("readme.txt", new MapResource("readme.txt"));
        LWNode n4 = new LWNode("Slash", new MapResource("file:///"));
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
        // create test pathways
        if (false) {
            // FYI: I dno't think PathwayTableModel will
            // detect this creation, so can't use this
            // for full testing (e.g., note setting, undo, etc)
            LWPathway p = new LWPathway("Test Pathway");
            p.add(n1);
            p.add(n2);
            p.add(n3);
            map.addPathway(p);
        }
        
        map.markAsSaved();
        
        /*else if(map.getLabel().equals("Test Nodes")){
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
         
        map.markAsSaved();
         
        }*/
    }

    private static void out(String s) {
        System.out.println("VUE: " + s);
    }
    
}
