 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.vue.action.*;
import tufts.vue.gui.*;

import java.util.*;
import java.util.prefs.*;
import java.io.*;
import java.net.URL;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.border.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.applet.AppletContext;

import net.roydesign.mac.MRJAdapter;
import net.roydesign.event.ApplicationEvent;
//import com.apple.mrj.*;


// $Header: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/tufts/vue/VUE.java,v 1.308 2005-08-31 15:40:06 sfraize Exp $
    
/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 */

public class VUE
    implements VueConstants
{
    private static AppletContext sAppletContext = null;
    
    /** The currently active viewer (e.g., is visible
     * and has focus).  Actions (@see Actions.java) are performed on
     * the active model (sometimes querying the active viewer). */
    private static MapViewer ActiveViewer = null;
    /** The currently active selection.
     * elements in ModelSelection should always be from the ActiveModel */
    static final LWSelection ModelSelection = new LWSelection();
    
    /** array of tool windows, used for repeatedly creating JMenuBar's for on all Mac JFrame's */
    private static ToolWindow[] ToolWindows;

            /** teh global resource selection static model **/
    public final static ResourceSelection sResourceSelection = new ResourceSelection();
    
    private static VueFrame frame;   // make this private!
    
    private static MapTabbedPane mMapTabsLeft;
    private static MapTabbedPane mMapTabsRight;
    private static JSplitPane viewerSplit;
    
    static ToolWindow sMapInspector;
    static ToolWindow objectInspector;
    static ObjectInspectorPanel objectInspectorPanel;
    
    //hierarchy view tree window component
    private static LWHierarchyTree hierarchyTree;
    
    //overview tree window component
    public static LWOutlineView outlineView;
    
    //public static DataSourceViewer dataSourceViewer;
    public static FavoritesWindow favoritesWindow;
    public static boolean  dropIsLocal = false;
    private static boolean isStartupUnderway = true;

    private static java.util.List sActiveMapListeners = new java.util.ArrayList();
    private static java.util.List sActiveViewerListeners = new java.util.ArrayList();
    public interface ActiveMapListener {
        public void activeMapChanged(LWMap map);
    }
    public interface ActiveViewerListener {
        public void activeViewerChanged(MapViewer viewer);
    }

    public static void setAppletContext(AppletContext ac) {
        sAppletContext = ac;
    }
    public static AppletContext getAppletContext() {
        return sAppletContext;
    }
    public static boolean isApplet() {
        return sAppletContext != null;
    }

    public static String getSystemProperty(String name) {
        // If we're an applet, System.getProperty will trhow an AccessControlException
        if (false && isApplet())
            return null;
        else {
            String prop;
            try {
                prop = System.getProperty(name);
                if (DEBUG.INIT) {
                    out("got property " + name);
                    if (name.equals("apple.awt.brushMetalLook"))
                        new Throwable("apple.awt.brushMetalLook").printStackTrace();
                }
            } catch (java.security.AccessControlException e) {
                System.err.println(e);
                prop = null;
            }
            return prop;
        }
    }

    public static boolean isSystemPropertyTrue(String name) {
        String value = getSystemProperty(name);
        return value != null && value.toLowerCase().equals("true");
    }
    
    
    /*
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
    */
    
    static class VueFrame extends JFrame
        implements MapViewer.Listener, MouseWheelListener
    {
        final static int TitleChangeMask =
            MapViewerEvent.DISPLAYED |
            MapViewerEvent.FOCUSED;
            //MapViewerEvent.ZOOM;        // title includes zoom
        
        VueFrame() {
            super(VueResources.getString("application.title"));
            setIconImage(VueResources.getImageIcon("vueIcon32x32").getImage());
            //addMouseWheelListener(this); this causing stack overflows in JVM 1.4 & 1.5, and only works for unclaimed areas
            // (e.g., not mapviewer, even if it hasn't registered a wheel listener)
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            System.err.println("VUE MSW");
        }
        
        protected void processEvent(AWTEvent e) {
            // if (e instanceof MouseWheelEvent) System.err.println("VUE MSM PE"); only works w/listener, which has problem as above
            // try a generic AWT event queue listener?
            
            if (e instanceof MouseEvent) {
                super.processEvent(e);
                return;
            }
        
            if (DEBUG.FOCUS) out("VueFrame: processEvent " + e);
            // todo: if frame is getting key events, handle them
            // and/or pass off to MapViewer (e.g., tool switch events!)
            // or: put tool's w/action events in vue menu
            if (e instanceof WindowEvent) {
                switch (e.getID()) {
                case WindowEvent.WINDOW_CLOSING:
                case WindowEvent.WINDOW_CLOSED:
                case WindowEvent.WINDOW_ICONIFIED:
                    //case WindowEvent.WINDOW_DEACTIVATED:
                    super.processEvent(e);
                    return;
                    /*
                case WindowEvent.WINDOW_ACTIVATED:
                    tufts.macosx.Screen.dumpWindows();
                case WindowEvent.WINDOW_OPENED:
                case WindowEvent.WINDOW_DEICONIFIED:
                case WindowEvent.WINDOW_GAINED_FOCUS:
                    if (VUE.getRootWindow() != VUE.getMainWindow())
                        VUE.getRootWindow().toFront();
                    */
                }
            }

            // why do we do this?  Must have to do with full-screen or something...
            if (VUE.getRootWindow() != VUE.getMainWindow()) {
                if (DEBUG.Enabled) out("VueFrame: processEvent: root != main: forcing root visible & front");
                VUE.getRootWindow().setVisible(true);
                VUE.getRootWindow().toFront();
            }
            super.processEvent(e);
        }

        public void addComp(Component c, String constraints) {
            getContentPane().add(c, constraints);
        }

        /*
        public void show() {
            out("VueFrame: show");
            super.show();
            pannerTool.toFront();
        }
        public void toFront()
        {
            //if (DEBUG.FOCUS)
                out("VueFrame: toFront");
            super.toFront();
        }
        */

        /** never let the frame be hidden -- always ignored */
        public void setVisible(boolean tv) {
            //System.out.println("VueFrame setVisible " + tv + " OVERRIDE");
            
            // The frame should never be "hidden" -- iconification
            // doesn't trigger that (nor Mac os "hide") -- so if we're
            // here the OS window manager is attempting to hide us
            // (the 'x' button on the window frame).
            
            //super.setVisible(true);
            super.setVisible(tv);
        }
        public void mapViewerEventRaised(MapViewerEvent e) {
            if ((e.getID() & TitleChangeMask) != 0)
                setTitleFromViewer(e.getMapViewer());
        }
        
        private void setTitleFromViewer(MapViewer viewer) {
            String title = VUE.NAME + ": " + viewer.getMap().getLabel();
            //if (viewer.getMap().isCurrentlyFiltered())
            // will need to listen to map for filter change state or this gets out of date
            //    title += " (Filtered)";
            setTitle(title);
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
    
    public static LWSelection getSelection() {
        return ModelSelection;
    }

    public static boolean isStartupUnderway() {
        return isStartupUnderway;
    }
    
    private static Cursor oldRootCursor;
    private static Cursor oldViewerCursor;
    private static MapViewer waitedViewer;
    // todo: as a stack
    public static synchronized void activateWaitCursor() {
        if (oldRootCursor != null) {
            out("multiple wait-cursors: already have " + oldRootCursor + "\n");
            return;
        }
        if (getActiveViewer() != null) {
            waitedViewer = getActiveViewer();
            oldViewerCursor = waitedViewer.getCursor();
            waitedViewer.setCursor(CURSOR_WAIT);
        }
        JRootPane root = SwingUtilities.getRootPane(VUE.frame);
        if (root != null) {
            //out("ACTIVATING WAIT CURSOR: current =  " + oldRootCursor + "\n");
            oldRootCursor = root.getCursor();
            root.setCursor(CURSOR_WAIT);
        }
    }
    
    public static void clearWaitCursor() {
        //_clearWaitCursor();
        VUE.invokeAfterAWT(new Runnable() { public void run() { _clearWaitCursor(); }});
    }
    
    private static void _clearWaitCursor() {
        //out("restoring old cursor " + oldRootCursor + "\n");
        if (oldRootCursor == null)
            return;
        if (waitedViewer != null) {
            waitedViewer.setCursor(oldViewerCursor);
            waitedViewer = null;
        }
        SwingUtilities.getRootPane(VUE.frame).setCursor(oldRootCursor);
        oldRootCursor = null;
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
    
    static void initUI() {
        initUI(false);
    }

    static class VueAquaLookAndFeel extends apple.laf.AquaLookAndFeel {
        public String getDescription() { return super.getDescription() + " (VUE Derivative)"; }
        public void initComponentDefaults(UIDefaults table)
        {
            super.initComponentDefaults(table);
            //table.put("TitledBorder.font", fontMedium.deriveFont(Font.BOLD));
            //table.put("Button.font", getFont());

            Font font = table.getFont("Label.font");
            //System.out.println(font);
            font = makeFont(font.deriveFont(10f));
            //System.out.println(font);
            
            table.put("Label.font", font);
            table.put("Tree.font", font);
            table.put("TextField.font", font);
            table.put("TextArea.font", font);
            table.put("TextPane.font", font);
            table.put("Table.font", font);
            table.put("TableHeader.font", font);
            //table.put("ComboBox.font", font);

            /*
            Object newFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/NewFolder.gif");
            Object upFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/UpFolder.gif");
            Object homeFolderIcon = LookAndFeel.makeIcon(getClass(), "icons/HomeFolder.gif");
            Object detailsViewIcon = LookAndFeel.makeIcon(getClass(), "icons/DetailsView.gif");
            Object listViewIcon = LookAndFeel.makeIcon(getClass(), "icons/ListView.gif");
            Object directoryIcon = LookAndFeel.makeIcon(getClass(), "icons/Directory.gif");
            Object fileIcon = LookAndFeel.makeIcon(getClass(), "icons/File.gif");
            Object computerIcon = LookAndFeel.makeIcon(getClass(), "icons/Computer.gif");
            Object hardDriveIcon = LookAndFeel.makeIcon(getClass(), "icons/HardDrive.gif");
            Object floppyDriveIcon = LookAndFeel.makeIcon(getClass(), "icons/FloppyDrive.gif");
            */
        }

        private static javax.swing.plaf.FontUIResource makeFont(Font font) {
            return new javax.swing.plaf.FontUIResource(font);
        }
        private javax.swing.plaf.FontUIResource getFont() {
            return new javax.swing.plaf.FontUIResource(VueConstants.MediumFont);
        }
    }

    private static void installVueAquaLAF() {
        try {
            javax.swing.UIManager.setLookAndFeel(new VueAquaLookAndFeel());
        } catch (javax.swing.UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }
    
    private static boolean useMacLAF = false;
    static void initUI(boolean debug)
    {
        String lafn = null;
        //lafn = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //lafn = "javax.swing.plaf.basic.BasicLookAndFeel"; // not a separate L&F -- baseclass
        
        if (useMacLAF) {
            // not using metal, so theme will have no effect -- need a LAF to change things.
            // if on mac, java look & feel will have been defaulted to mac look
            // if on PC and you specify mac theme, our Metal theme won't be installed
            installVueAquaLAF();
            themeSet = true;
        } else {
            // by default, force windows L&F on the mac.
            if (VueUtil.isMacPlatform()) {
                lafn = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
            }
        }

        // Note that it is essential that the theme be set before a single
        // GUI object of any kind is created.  If, for instance, a static
        // member in any class initializes a swing gui object, this will end
        // up having no effect here, and the entire theme will be silently
        // ignored.  This includes the call below to UIManager.setLookAndFeel,
        // which is why we need to tell the VueTheme about the laf instead
        // of having it ask for the LAF itself, as it may not have been set
        // yet.  Note that when using the Mac Aqua L&F, we don't need
        // to set the theme for Metal (as it's not being used and would
        // have no effect), but we still need to initialize the theme,
        // as it's still queried througout the code.

        boolean macAquaLAF = VueUtil.isMacPlatform() && useMacLAF;
        VueTheme vueTheme = VueTheme.getTheme(macAquaLAF);
        
        if (!themeSet) {
            out("Installing VUE MetalLookAndFeel theme.");
            MetalLookAndFeel.setCurrentTheme(vueTheme);
            //MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.OceanTheme());
        }
        
        try {
            if (lafn != null)
                javax.swing.UIManager.setLookAndFeel(lafn);
            //javax.swing.UIManager.setLookAndFeel(new VueLookAndFeel());
        } catch (Exception e) {
            System.err.println(e);
        }

        out("LookAndFeel: " + javax.swing.UIManager.getLookAndFeel().getName());
        out("             " + javax.swing.UIManager.getLookAndFeel().getDescription());
        out("             " + javax.swing.UIManager.getLookAndFeel().getClass());
    }

    public static void parseArgs(String[] args) {
        String allArgs = "";
        for (int i = 0; i < args.length; i++) {
            allArgs += "[" + args[i] + "]";
            if (args[i].equals("-nodr"))
                nodr = true;
            else if (args[i].equals("-mac") || args[i].equals("-useMacLookAndFeel"))
                useMacLAF = true;
            else if (args[i].equals("-debug_init"))     DEBUG.INIT = true;
            else if (args[i].equals("-debug_focus"))    DEBUG.FOCUS = true;
            else if (args[i].equals("-debug_dr"))       DEBUG.DR = true;
            else if (args[i].equals("-debug_tool"))     DEBUG.TOOL = true;
            else if (args[i].equals("-debug_drop"))     DEBUG.DND = true;
            else if (args[i].equals("-debug_undo"))     DEBUG.UNDO = true;
            else if (args[i].equals("-debug_castor"))   DEBUG.CASTOR = true;
            else if (args[i].equals("-debug_xml"))      DEBUG.XML = true;
            else if (args[i].equals("-debug_paint"))    DEBUG.PAINT = true;
            else if (args[i].equals("-debug_mouse"))    DEBUG.MOUSE = true;
            else if (args[i].startsWith("-debug_event"))   DEBUG.EVENTS = true;
            else if (args[i].startsWith("-debug_thread"))  DEBUG.THREAD = true;
            else if (args[i].startsWith("-debug_image"))   DEBUG.IMAGE = true;
            else if (args[i].equals("-exit_after_init")) // for startup time trials
                exitAfterInit = true;

            if (args[i].startsWith("-debug")) DEBUG.Enabled = true;
        }
        out("parsed args " + allArgs);
    }

    
    private static JPanel toolPanel;
    private static boolean themeSet = false;
    private static boolean nodr = false;
    private static boolean exitAfterInit = false;


    public static final boolean TUFTS = VueResources.getBool("application.features.tufts");
    public static final boolean NARRAVISION = !TUFTS;
    public static final String NAME = VueResources.getString("application.name");
    
    
    public static void main(String[] args) {
        System.out.println("VUE: main");
        System.out.println("VUE: build: " + tufts.vue.Version.AllInfo);
        System.out.println("VUE: name: " + VUE.NAME);
        //System.out.println("VUE: fully built: " + build.version.AllInfo);

        //VUE.TUFTS = 
        if (VUE.TUFTS)
            System.out.println("VUE: TUFTS features only (no MIT/development)");
        else
            System.out.println("VUE: MIT/development features enabled");
        
        parseArgs(args);

        // initUI installs the VueTheme (unless mac look), which must be done
        // before any other GUI code (including the SlpashScreen)
        // or our VueTheme gets ignored by swing.
        initUI();

        Window splashScreen = null;
        if (nodr)
            DEBUG.Enabled = true;
        else
            splashScreen = new SplashScreen();

        //Preferences p = Preferences.userNodeForPackage(VUE.class);
        //p.put("DRBROWSER.RUN", "yes, it has");
        

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
        if (nodr == false)  {
            drBrowser = new DRBrowser(true);
            //if (VueUtil.isMacAquaLookAndFeel()) drBrowser.setBackground(SystemColor.control);
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
        }

        
        JSplitPane splitPane = new JSplitPane();
        //splitPane.setResizeWeight(0.40); // 25% space to the left component
        splitPane.setContinuousLayout(false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        if (VUE.NARRAVISION)
            splitPane.setDividerLocation(0);
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
                    MapViewer leftViewer = null;
                    MapViewer rightViewer = null;
                    if (mMapTabsLeft != null)
                        leftViewer = mMapTabsLeft.getSelectedViewer();
                    if (mMapTabsRight != null)
                        rightViewer = mMapTabsRight.getSelectedViewer();

                    if (multipleMapsVisible()) {
                        /*
                          // should be handled by MapVewer.reshape
                        if (leftViewer != null)
                            leftViewer.fireViewerEvent(MapViewerEvent.PAN);
                        if (rightViewer != null)
                            rightViewer.fireViewerEvent(MapViewerEvent.PAN);
                        */
                    } else {
                        if (leftViewer != null && leftViewer != getActiveViewer()) {
                            if (DEBUG.FOCUS) out("viewerSplit: default focus to " + leftViewer);
                            leftViewer.requestFocus();
                            if (rightViewer != null)
                                rightViewer.fireViewerEvent(MapViewerEvent.HIDDEN);
                        }
                    }
                }});
        
        
        //splitPane.setRightComponent(mMapTabsLeft);
        splitPane.setRightComponent(viewerSplit);
        //JPanel vuePanel = new AAPanel();
        JPanel vuePanel = new JPanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);

        if (DEBUG.INIT) out("creating VueFrame...");

        VUE.frame = new VueFrame();

        if (DEBUG.INIT) out("created VueFrame");
        
        // Create the tool windows
        ToolWindow pannerTool = createToolWindow("Panner");
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner());

        if (DEBUG.INIT) out("created PannerTool");
        
        ToolWindow inspectorTool = null;
        /*
        if (nodr) {
            inspectorTool = createToolWindow("Inspector");
            inspectorTool.addTool(new LWCInspector());
        }
        */
        
        ToolWindow drBrowserTool = null;
        //DataSourceViewer currently breaks if more than one DRBrowser
        //ToolWindow drBrowserTool = createToolWindow("Data Sources", frame);
        //if (drBrowser != null) drBrowserTool.addTool(drBrowser);
        
        // The real tool palette window withtools and contextual tools
        ToolWindow toolbarWindow = null;
        VueToolbarController tbc = VueToolbarController.getController();
        ModelSelection.addListener(tbc);
        /*
        ToolWindow toolbarWindow = createToolWindow( VueResources.getString("tbWindowName"));
        tbc.setToolWindow( toolbarWindow);
        toolbarWindow.getContentPane().add( tbc.getToolbar() );
        toolbarWindow.pack();
         */

        //frame.getContentPane().add(tbc.getToolbar(), BorderLayout.NORTH);
                
        JPanel toolBarPanel = null;
        if (VUE.TUFTS) {
            //toolBarPanel = new JPanel();
            //toolBarPanel.add(tbc.getToolbar());
            frame.addComp(tbc.getToolbar(), BorderLayout.NORTH);
        } else {
            toolBarPanel = new JPanel(new BorderLayout());
            toolBarPanel.add(tbc.getToolbar(), BorderLayout.NORTH);
            toolBarPanel.add(new VueToolBar(), BorderLayout.SOUTH);
            frame.addComp(toolBarPanel, BorderLayout.NORTH);
        }

        if (DEBUG.INIT) out("created VueToolBar");
        // Map Inspector
        
        // get the proper scree/main frame size
        sMapInspector = createToolWindow(VueResources.getString("mapInspectorTitle"));
        MapInspectorPanel mi = new MapInspectorPanel();
        sMapInspector.addTool(mi);
        
        //ToolWindow objectInspector = createToolWindow( VueResources.getString("objectInspectorTitle"), frame);
        objectInspector = createToolWindow(VueResources.getString("objectInspectorTitle"));
        objectInspectorPanel = new ObjectInspectorPanel();
        ModelSelection.addListener(objectInspectorPanel);
        //sResourceSelection.addListener( objectInspectorPanel);
        objectInspector.addTool(objectInspectorPanel);
        
        
        if (false) {
            JFrame testFrame = new JFrame("Debug");
            testFrame.setSize(300,300);
            //testFrame.getContentPane().add( new NodeInspectorPanel() );
            testFrame.getContentPane().add(objectInspectorPanel);
            testFrame.show();
        }
        
        if (DEBUG.INIT) out("creating LWOutlineView...");
        outlineView = new LWOutlineView(getRootFrame());
        //outlineView = new LWOutlineView(VUE.frame);
        
        VUE.ToolWindows = new ToolWindow[] {
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
        if (DEBUG.INIT) out("setting JMenuBar...");
        frame.setJMenuBar(new VueMenuBar(VUE.ToolWindows));
        if (DEBUG.INIT) out("VueMenuBar installed.");;

        // On Mac, need to set any frame's to have a duplicate
        // of the main menu bar, so it stay's active at top
        // when they have focus.
        if (useMacLAF && VueUtil.isMacPlatform()) {
            for (int i = 0; i < ToolWindows.length; i++) {
                ToolWindow toolWindow = VUE.ToolWindows[i];
                if (toolWindow == null)
                    continue;
                Window w = toolWindow.getWindow();

                if (w instanceof JFrame) {
                    if (nodr) {
                        // we're hitting bug in java 1.4.2 on Tiger here (apple.laf.ScreenMenuBar bounds exception)
                        // Mysteriously, it only happens using the debug option -nodr for no DR browser.
                        if (DEBUG.INIT) out("adding menu bar to " + w);
                    }
                    try {
                        ((JFrame)w).setJMenuBar(new VueMenuBar(ToolWindows));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        out("OSX TIGER JAVA BUG");
                        e.printStackTrace();
                    }
                    toolWindow.setProcessKeyBindingsToMenuBar(false);
                }
            }
            if (DEBUG.INIT) out("Mac ToolWindow VueMenuBar's installed.");
        }
        
        frame.addComp(vuePanel,BorderLayout.CENTER);
        //frame.getContentPane().setBackground(Color.red);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        //frame.setBackground(Color.white);
        try {
            frame.pack();
        } catch (ArrayIndexOutOfBoundsException e) {
            out("OSX TIGER JAVA BUG at frame.pack()");
            e.printStackTrace();
        }
        if (nodr) {
            frame.setSize(750,450);
        } else {
            frame.setSize(800,600);// todo: make % of screen, make sure tool windows below don't go off screen!
            if (VUE.NARRAVISION)
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        if (DEBUG.INIT) out("validating frame...");
        frame.validate();
        if (DEBUG.INIT) out("frame validated");

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

        VUE.isStartupUnderway = false;
        
        if (!nodr) {
            LWMap startupMap = null;
            try {
                final java.net.URL startupURL;
                startupURL = VueResources.getURL("resource.startmap");
                startupMap = OpenAction.loadMap(startupURL);
                startupMap.setFile(null); // dissassociate startup map from it's file so we don't write over it
                startupMap.setLabel("Welcome");
                startupMap.markAsSaved();
            } catch (Exception ex) {
                ex.printStackTrace();
                VueUtil.alert(null, "Cannot load the Start-up map", "Start Up Map Error");
            }

            try {
                if (startupMap != null)
                    displayMap(startupMap);
            } catch (Exception ex) {
                ex.printStackTrace();
                VueUtil.alert(null, "Failed to display Start-up Map", "Internal Error");
            }
            
        } else {
            //pannerTool.setVisible(true);
        }

        out("showing frame...");
        frame.show();
        if (DEBUG.INIT) out("frame visible");
        
        if (splashScreen != null)
            splashScreen.setVisible(false);

        VUE.activateWaitCursor();

        boolean gotMapFromCommandLine = false;
        
        if (args.length > 0) {
            try {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].charAt(0) == '-')
                        continue;
                    LWMap map = OpenAction.loadMap(args[i]);
                    if (map != null) {
                        displayMap(map);
                        gotMapFromCommandLine = true;
                    }
                }
            } finally {
                //VUE.clearWaitCursor();                
            }
        }
        
        if (nodr && gotMapFromCommandLine == false) {
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
            //toolPanel.add(new JLabel("Empty Label"), BorderLayout.CENTER);
        }

        if (DEBUG.INIT) out("map loaded");
        if (drBrowser != null) {
            drBrowser.loadDataSourceViewer();
            if (VUE.TUFTS) // leave collapsed if NarraVision
                splitPane.resetToPreferredSizes();
        }

        out("loading fonts...");
        FontEditorPanel.getFontNames();
        out("caching tool panels...");
        NodeTool.getNodeToolPanel();
        LinkTool.getLinkToolPanel();
        if (drBrowser != null && drBrowserTool != null)
            drBrowserTool.addTool(new DRBrowser());

        if (VueUtil.isMacPlatform())
            installMacOSXApplicationEventHandlers();

        // MAC v.s. PC WINDOW PARENTAGE & FOCUS BEHAVIOUR:
        //
        // Window's that are shown before their parent's are shown do
        // NOT adopt a stay-on-top-of-parent behaviour! (at least on
        // mac).  FURTHERMORE: if you iconfiy the parent and
        // de-iconify it, the keep-on-top is also lost permanently!
        // (Even if you hide/show the child window after that) None of
        // this happens on the PC, only Mac OS X.  Iconifying also
        // hides the child windows on the PC, but not on Mac.  On the
        // PC, there's also no automatic way to install the action
        // behaviours to take effect (the ones in the menu bar) when a
        // tool window has focus.  Actually, mac appears to do
        // something smart also: if parent get's MAXIMIZED, it
        // will return to the keep on top behaviour, but you
        // have to manually hide/show it to get it back on top.
        //
        // Also: for some odd reason, if we use an intermediate
        // root window as the master parent, the MapPanner display
        // doesn't repaint itself when dragging it or it's map!
        
        getRootWindow().show();

        //out("ACTIONTMAP " + java.util.Arrays.asList(frame.getRootPane().getActionMap().allKeys()));
        //out("INPUTMAP " + java.util.Arrays.asList(frame.getRootPane().getInputMap().allKeys()));
        //out("\n\nACTIONTMAP " + java.util.Arrays.asList(frame.getActionMap().allKeys()));
        //out("ACTIONTMAP " + Arrays.asList(VUE.getActiveViewer().getActionMap().allKeys()));
        //out("INPUTMAP " + Arrays.asList(VUE.getActiveViewer().getInputMap().keys()));
        //out("INPUTMAP " + Arrays.asList(getInputMap().keys()));

        VUE.clearWaitCursor();
        
        out("main completed.");

        if (exitAfterInit)
            System.exit(0);
    }

    private static void installMacOSXApplicationEventHandlers()
    {
        if (!VueUtil.isMacPlatform())
            throw new RuntimeException("can only install OSX event handlers on Mac OS X");
        
        MRJAdapter.addQuitApplicationListener(new ExitAction());
        MRJAdapter.addAboutListener(new AboutAction());
        MRJAdapter.addOpenApplicationListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("VUE: OpenApplication " + e);
                }
            });
        MRJAdapter.addOpenDocumentListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("VUE: OpenDocument " + e);
                    ApplicationEvent ae = (ApplicationEvent) e;
                    VUE.displayMap(ae.getFile());
                }
            });
        
        // this was working for double-click launch AND open of a .vue file --
        // above MRJAdapater callbacks aren't getting the open call after launch...
        // from com.apple.mrj.* -- deprecated old api.  Consider using an
        // OSXAdapter styled impl instead of net.roydesign stuff, due to the above failure
        // with opening the app on double-click.  (Create our own pass-thru class
        // that get's compiled only the on the mac, and bundled as a lib for the main
        // build for other platforms).
        //
        // Note that attempting to combine the below with the above forces one of them to always break.
        
        /*
        MRJApplicationUtils.registerOpenDocumentHandler(new MRJOpenDocumentHandler() {
                public void handleOpenFile(File file) {
                    System.out.println("VUE: MRJOpenDocumentHandler: " + file);
                    VUE.displayMap(file);
                }
            });
        */

        
        // An attempt to get the mac metal look picked up by something other than a frame
        // & other window experiments.
        /*
        
        Frame emptyFrame = new Frame();
        Window emptyWindow = new Window(VUE.frame);
        
        //Window w = new java.awt.VFrame(frame);
        //Window w = new Frame(); // only full-bread Frame's/JFrame's are picking up mac brushed metal look...
        //Window w = new JWindow(VUE.frame);
        //Window w = new Dialog(VUE.frame);
        //Window w = new JDialog(VUE.frame);
        Window w = new TestWindow(VUE.frame);
        w.setLayout(new FlowLayout());
        w.add(new JLabel("Hello."));
        JComponent tf = new JTextField("text", 10);
        w.add(tf);
        tf.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    new Throwable("tf got focus " + e).printStackTrace();
                }
            });
        // tf.setFocusable(false); // disable's field
        //w.setFocusable(false);
        //w.setFocusableWindowState(false);
        w.setSize(200,100);
        tufts.Util.centerOnScreen(w);
        tf.enableInputMethods(true);
        tf.requestFocus();
        //w.setBackground(UIManager.getColor("info")); // tried window,control,desktop,windowBorder,menu,activeCaption,info
        //w.setBackground(SystemColor.control);
        //w.setBackground(frame.getBackground());
        w.show();

        */
        
    
    }
            
    static class TestWindow extends Window {
        TestWindow(Window parent) {
            super(parent);
        }
    }
    
    public static int openMapCount() {
        return mMapTabsLeft == null ? 0 : mMapTabsLeft.getTabCount();
    }
    
    private static Object LOCK = new Object();
    
    public static void addActiveMapListener(ActiveMapListener l) {
        synchronized (LOCK) {
            sActiveMapListeners.add(l);
        }
    }
    public static void removeActiveMapListener(ActiveMapListener l) {
        synchronized (LOCK) {
            sActiveMapListeners.remove(l);
        }
    }
    public static void addActiveViewerListener(ActiveViewerListener l) {
        synchronized (LOCK) {
            sActiveViewerListeners.add(l);
        }
    }
    public static void removeActiveViewerListener(ActiveViewerListener l) {
        synchronized (LOCK) {
            sActiveViewerListeners.remove(l);
        }
    }
    
    /**
     * Viewer can be null, which happens when we close the active viewer
     * and until another grabs the application focus (unles it was the last viewer).
     */
    public static void setActiveViewer(MapViewer viewer) {
        synchronized (LOCK) {
            if (ActiveViewer != viewer) {
                LWMap oldActiveMap = null;
                if (ActiveViewer != null)
                    oldActiveMap = ActiveViewer.getMap();
                ActiveViewer = viewer;
                if (DEBUG.FOCUS) out("ActiveViewer set to " + viewer);
                if (ActiveViewer != null) {
                    java.util.Iterator i = sActiveViewerListeners.iterator();
                    while (i.hasNext())
                        ((ActiveViewerListener)i.next()).activeViewerChanged(viewer);
                    if (oldActiveMap != ActiveViewer.getMap()) {
                        LWMap activeMap = viewer.getMap();
                        i = sActiveMapListeners.iterator();
                        if (DEBUG.FOCUS) out("ActiveMap set to " + activeMap);
                        while (i.hasNext()) {
                            ActiveMapListener aml = (ActiveMapListener) i.next();
                            if (DEBUG.EVENTS) out("activeMapChanged -> " + aml);
                            aml.activeMapChanged(activeMap);
                        }
                    }
                }
            } else {
                // prob don't need this now that we're synchronized
                ActiveViewer = viewer;
            }
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
        return getLeftTabbedPane();
    }
    
    public static MapTabbedPane getLeftTabbedPane() {
        return mMapTabsLeft;
    }

    public static MapTabbedPane getRightTabbedPane() {
        return mMapTabsRight;
    }

    public static LWMap getActiveMap() {
        if (getActiveViewer() != null)
            return getActiveViewer().getMap();
        else
            return null;
    }

    
    public static UndoManager getUndoManager() {
        LWMap map = getActiveMap();
        if (map != null)
            return map.getUndoManager();
        else
            return null;
    }
    
    public static void markUndo() {
        markUndo(null);
    }
    
    /** mark prior change(s) with the given undo name */
    public static void markUndo(String name) {
        LWMap map = getActiveMap();
        if (map != null) {
            UndoManager um = map.getUndoManager();
            if (um != null) {
                if (name != null)
                    um.markChangesAsUndo(name);
                else
                    um.mark();
            }
        }
    }
    
    /**
     * If any open maps have been modified and not saved, run
     * dialogs to determine what to do.
     * @return true if we're cleared to exit, false if we want to abort the exit
     */
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

        if (inNativeFullScreen())
            toggleFullScreen();
        
        int response = JOptionPane.showOptionDialog
        (VUE.getRootParent(),
        
        "Do you want to save the changes you made to \n"
        + "'" + map.getLabel() + "'?"
        + (DEBUG.EVENTS?("\n[modifications="+map.getModCount()+"]"):""),
        
        " Save changes?",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE,
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
    
    
    /**
     * If we already have open a map tied to the given file, display it.
     * Otherwise, open it anew and display it.
     */
    public static void displayMap(File mapFile) {
        out("displayMap " + mapFile);
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(mapFile)) {
                out("displayMap found existing open map " + map);
                mMapTabsLeft.setSelectedIndex(i);
                return;
            }
        }
        OpenAction.displayMap(mapFile);
    }
    
    /**
     * Create a new viewer and display the given map in it.
     */
    public static MapViewer displayMap(LWMap pMap) {
        out("displayMap " + pMap);
        MapViewer leftViewer = null;
        MapViewer rightViewer = null;
        
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(pMap.getFile())) {
                System.err.println("** VUE.displayMap found open map with same file! " + map);
                // TODO: pop dialog asking to revert existing if there any changes.
                //break;
            }
        }
        
        if (leftViewer == null) {
            leftViewer = new MapViewer(pMap, "*LEFT");
            rightViewer = new MapViewer(pMap, "right");
            rightViewer.setFocusable(false); // so doesn't grab focus till we're ready

            out("displayMap: currently active viewer: " + getActiveViewer());
            out("displayMap: created new left viewer: " + leftViewer);

            mMapTabsLeft.addViewer(leftViewer);
            mMapTabsRight.addViewer(rightViewer);
        }
        
        mMapTabsLeft.setSelectedComponent(leftViewer);

        return leftViewer;
    }

    /**
     * deprecated - use getRootParent, getRootWindow or getRootFrame
     */
    public static Frame getInstance() {
        return getRootFrame();
    }

    /** return the root VUE component used for parenting */
    public static Component getRootParent() {
        return getRootWindow();
    }

    private static Window rootWindow;
    /** return the root VUE window, mainly for those who'd like it to be their parent */
    public static Window getRootWindow() {
        return VUE.frame;
        /*
        if (true) {
            return VUE.frame;
        } else {
            if (rootWindow == null) {
                //rootWindow = makeRootFrame();
                rootWindow = makeRootWindow();
            }
            return rootWindow;
        }
        */
    }
    /*
    private static Window makeRootWindow() {
        if (true||DEBUG.INIT) out("making the ROOT WINDOW with parent " + VUE.frame);
        Window w = new ToolWindow("Vue Root", VUE.frame);
        //w.show();
        return w;
    }
    */

    public static VueMenuBar getJMenuBar() {
        return (VueMenuBar) ((VueFrame)getRootWindow()).getJMenuBar();
    }
    

    /** Return the main VUE window.  Usually == getRoowWindow, unless we're
     * using a special root window for parenting the tool windows.
     */
    static Window getMainWindow() {
        return VUE.frame;
    }

    private static boolean makingRootFrame = false;
    private static Frame makeRootFrame() {
        if (makingRootFrame) {
            new Throwable("RECURSIVE MAKE ROOT WINDOW CALL").printStackTrace();
            return null;
        }
        makingRootFrame = true;
        JFrame f = null;
        try {
            if (DEBUG.INIT) out("creating the ROOT WINDOW");
            f = new JFrame("Vue Root");
            if (VueUtil.isMacPlatform() && useMacLAF) {
                JMenuBar menu = new VUE.VueMenuBar();
                f.setJMenuBar(menu);
            }
            f.show();
            //rootFrame = createFrame();
        } finally {
            makingRootFrame = false;
        }
        return f;
    }
    /** return the root VUE frame, mainly for those who'd like it to be their parent */
    public static Frame getRootFrame() {
        if (getRootWindow() instanceof Frame)
            return (Frame) getRootWindow();
        else
            return VUE.frame;
    }
    
    /**
     * Factory method for creating frames in VUE.  On PC, this
     * is same as new new JFrame().  In Mac Look & Feel it adds a duplicate
     * menu-bar to the frame as every frame needs one
     * or we lose the mebu-bar.
     */
    public static JFrame createFrame()
    {
        return createFrame(null);
    }
    
    public static JFrame createFrame(String title)
    {
        JFrame newFrame = new JFrame(title);
        if (VueUtil.isMacPlatform() && useMacLAF) {
            JMenuBar menu = new VUE.VueMenuBar();
            newFrame.setJMenuBar(menu);
        }
        return newFrame;
    }

    /** @return a new JWindow, parented to the root VUE window */
    public static JWindow createWindow()
    {
        return new JWindow(getRootWindow());
    }

    /** @return a new ToolWindow, parented to getRootWindow() */
    public static ToolWindow createToolWindow(String title) {
        return createToolWindow(title, null);
    }
    /** @return a new ToolWindow, containing the given component, parented to getRootWindow() */
    public static ToolWindow createToolWindow(String title, JComponent component) {
        //Window parent = getRootFrame();
        Window parent = getRootWindow();
        if (DEBUG.INIT) out("creating ToolWindow " + title + " with parent " + parent);
        ToolWindow w = new ToolWindow(title, parent);
        if (component != null)
            w.addTool(component);
        /*
          // ToolWindows not set yet...
        if (VueUtil.isMacPlatform() && useMacLAF && w instanceof JFrame)
            ((JFrame)w).setJMenuBar(new VUE.VueMenuBar());
        */
        return w;
    }

    /** call the given runnable after all pending AWT events are completed */
    static void invokeAfterAWT(Runnable runnable) {
        java.awt.EventQueue.invokeLater(runnable);
    }

    static class VueToolBar extends JToolBar
    {
        public VueToolBar()
        {
            super("Toolbar");
            add(Actions.NewMap);
            add(new OpenAction());
            add(new SaveAction());
            add(new PrintAction()); // deal with print singleton issue / getactioncommand is null here
            //addSeparator(); // not doing much
            add(Actions.Undo);
            add(Actions.Redo);
            add(Actions.Group);
            add(Actions.Ungroup);
            add(Actions.ZoomIn);
            add(Actions.ZoomOut);
            add(Actions.ZoomFit);
            add(Actions.Delete);

            setRollover(true);
            setMargin(new Insets(0,0,0,0));
        }

        public JButton add(Action a) {
            //return super.add(a);
            JButton b = makeButton(a);
            super.add(b);
            return b;
        }

        private static JButton makeButton(Action a) {
            VueButton b = new VueButton(a);
            b.setAsToolbarButton(true);
            return b;
        }
    }

    
    static class VueMenuBar extends JMenuBar
        implements FocusListener
    {
        // this may be created multiple times as a workaround for the inability
        // to support a single JMenuBar for the whole application on the Mac
        public VueMenuBar()
        {
            this(VUE.ToolWindows);
        }

        /*
        public void paint(Graphics g) {
            System.err.println("\nVueMenuBar: paint");

        }
        */
        
        public VueMenuBar(ToolWindow[] toolWindows)
        {
            addFocusListener(this);
            final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
            JMenu fileMenu = new JMenu("File");
            JMenu editMenu = new JMenu("Edit");
            JMenu viewMenu = new JMenu("View");
            JMenu formatMenu = new JMenu("Format");
            JMenu arrangeMenu = new JMenu("Arrange");
            JMenu windowMenu = null;
            JMenu alignMenu = new JMenu("Arrange/Align");
            //JMenu optionsMenu = menuBar.add(new JMenu("Options"))l
            JMenu helpMenu = add(new JMenu("Help"));

            //adding actions
            SaveAction saveAction = new SaveAction("Save", false);
            SaveAction saveAsAction = new SaveAction("Save As...");
            OpenAction openAction = new OpenAction("Open Map...");
            ExitAction exitAction = new ExitAction("Quit");
            Publish publishAction = new Publish("Export");
        
            // Actions added by the power team
            PrintAction printAction = PrintAction.getPrintAction();
            PDFTransform pdfAction = new PDFTransform("PDF");
            HTMLConversion htmlAction = new HTMLConversion("HTML");
            ImageConversion imageAction = new ImageConversion("JPEG");
            ImageMap imageMap = new ImageMap("IMAP");
            SVGConversion svgAction = new SVGConversion("SVG");
            XMLView xmlAction = new XMLView("XML View");
        
            if (false && DEBUG.Enabled) {
                // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG:
                // we're hitting bug in java (1.4.2, 1.5) on Tiger (OSX 10.4.2) here
                // (apple.laf.ScreenMenuBar array index out of bounds exception)
                JButton u = new JButton(Actions.Undo);
                JButton r = new JButton(Actions.Redo);
                JButton p = new JButton(printAction);
                JButton v = new JButton(printAction);
                v.setText("Print Visible");
            
                u.setBackground(Color.white);
                r.setBackground(Color.white);
                add(u).setFocusable(false);
                add(r).setFocusable(false);
                add(p).setFocusable(false);
                add(v).setFocusable(false);

                //menuBar.add(new tufts.vue.gui.VueButton(Actions.Undo)).setFocusable(false);
                // not picking up icon yet...
            }

            if (false && DEBUG.Enabled) {
                // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG (see above)
                JMenu exportMenu = add(new JMenu("Export"));
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
            fileMenu.add(printAction).setText("Print Visible...");
            fileMenu.add(publishAction);
            // GET RECENT FILES FROM PREFS!
            //fileMenu.add(exportMenu);

            if (isApplet() || (VUE.isSystemPropertyTrue("apple.laf.useScreenMenuBar") && VueUtil.isMacAquaLookAndFeel())) {
                // Do NOT add quit to the file menu.
                // Either we're an applet w/no quit, or it's already in the mac application menu bar.
                // FYI, MRJAdapter.isSwingUsingScreenMenuBar() is not telling us the truth.
            } else {
                fileMenu.addSeparator();
                fileMenu.add(exitAction);
            }
        
            editMenu.add(Actions.Undo);
            editMenu.add(Actions.Redo);
            editMenu.addSeparator();
            editMenu.add(Actions.NewNode);
            editMenu.add(Actions.NewText);
            editMenu.add(Actions.Rename);
            editMenu.add(Actions.Duplicate);
            editMenu.add(Actions.Delete);
            editMenu.addSeparator();
            editMenu.add(Actions.Cut);
            editMenu.add(Actions.Copy);
            editMenu.add(Actions.Paste);
            editMenu.addSeparator();
            editMenu.add(Actions.SelectAll);
            editMenu.add(Actions.DeselectAll);
            editMenu.addSeparator();
            editMenu.add(Actions.editDataSource);
            editMenu.addSeparator();
            editMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        
            viewMenu.add(Actions.ZoomIn);
            viewMenu.add(Actions.ZoomOut);
            viewMenu.add(Actions.ZoomFit);
            viewMenu.add(Actions.ZoomActual);
            viewMenu.add(Actions.ToggleFullScreen);

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
        
            int index = 0;
            if (toolWindows != null) {

                windowMenu = add(new JMenu("Window"));
                
                for (int i = 0; i < toolWindows.length; i++) {
                    //System.out.println("adding " + toolWindows[i]);
                    ToolWindow toolWindow = toolWindows[i];
                    if (toolWindow == null)
                        continue;
                    final WindowDisplayAction windowAction = new WindowDisplayAction(toolWindow);
                    final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(KeyEvent.VK_1 + index++, Actions.COMMAND);
                    windowAction.putValue(Action.ACCELERATOR_KEY, acceleratorKey);
                    JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
                    windowAction.setLinkedButton(checkBox);
                    windowMenu.add(checkBox);
                }
            }
        
            //optionsMenu.add(new UserDataAction());
        
            helpMenu.add(new ShowURLAction("VUE Online", "http://vue.tccs.tufts.edu/"));
            helpMenu.add(new ShowURLAction("User Guide", "http://vue.tccs.tufts.edu/userdoc/"));
            helpMenu.add(new AboutAction());
            helpMenu.add(new ShortcutsAction());

            add(fileMenu);
            add(editMenu);
            add(viewMenu);
            add(formatMenu);
            add(arrangeMenu);
            if (windowMenu != null)
                add(windowMenu);
            add(helpMenu);
            
        }

        public boolean doProcessKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            return super.processKeyBinding(ks, e, condition, pressed);
        }

        public void setVisible(boolean b) {
            out("VMB: setVisible: " + b);
            super.setVisible(b);
        }
        public void focusGained(FocusEvent e) {
            out("VMB: focusGained from " + e.getOppositeComponent());
        }
        public void focusLost(FocusEvent e) {
            out("VMB: focusLost to " + e.getOppositeComponent());
        }
    }

    private static class ShortcutsAction extends VueAction {
        private static ToolWindow window;
        ShortcutsAction() {
            super("Short Cuts");
        }

        void act() {
            if (window == null)
                window = createWindow();
            window.setVisible(true);
        }
        private ToolWindow createWindow() {
            return createToolWindow(VUE.NAME + " Short-Cut Keys", createShortcutsList());
        }

        private JComponent createShortcutsList() {
            String text = new String();
            
            // get tool short-cuts
            VueTool[] tools =  VueToolbarController.getController().getTools();
            for (int i = 0; i < tools.length; i++) {
                VueTool tool = tools[i];
                if (tool.getShortcutKey() != 0)
                    text += " " + tool.getShortcutKey() + " - " + tool.getToolName() + "\n";
            }
            text += "\n";
            // get action short-cuts
            Iterator i = getAllActions().iterator();
            while (i.hasNext()) {
                VueAction a = (VueAction) i.next();
                KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                if (k != null) {
                    String keyName = " "
                        + KeyEvent.getKeyModifiersText(k.getModifiers())
                        + " "
                        + KeyEvent.getKeyText(k.getKeyCode());
                    if (keyName.length() < 10)
                        keyName += ": \t\t";
                    else
                        keyName += ": \t";
                    text += keyName + a.getPermanentActionName();
                    text += "\n";
                }
            }
            JTextArea t = new JTextArea();
            t.setFont(FONT_SMALL);
            t.setEditable(false);
            t.setFocusable(false);
            t.setText(text);
            t.setOpaque(false);
            return t;
        }
        
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
        private AbstractButton mLinkedButton;
        private Window mWindow;
        private String mTitle;
        private boolean firstDisplay = true;
        private static final boolean showActionLabel = false;
        
        public WindowDisplayAction(Window w) {
            super("window: " + w.getName());
            init(extractTitle(w), w);
        }
        
        public WindowDisplayAction(ToolWindow tw) {
            super("window: " + tw.getWindow().getName());
            init(tw.getTitle(), tw.getWindow());
        }

        private void init(String title, Window w) {
            mTitle = title;
            updateActionTitle(true);
            mWindow = w;
            mWindow.addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) { handleShown(); }
                    public void componentHidden(ComponentEvent e) { handleHidden(); }
            });
        }

        /*
        private String getTitle() {
            return mTitle;
            //return extractTitle(mWindow);
        }
        */

        private static String extractTitle(Window w) {
            if (w instanceof Frame)
                return ((Frame)w).getTitle();
            else if (w instanceof Dialog)
                return ((Dialog)w).getTitle();
            else
                return ((Window)w).getName();
        }

        private void handleShown() {
            //out("handleShown [" + getTitle() + "]");
            setButtonState(true);
            updateActionTitle(false);
        }
        private void handleHidden() {
            //out("handleHidden [" + getTitle() + "]");
            setButtonState(false);
            updateActionTitle(false);
        }
        
        private void updateActionTitle(boolean firstTime) {
            if (!firstTime && !showActionLabel)
                return;
            if (showActionLabel) {
                String action = "Show ";
                if (mLinkedButton != null && mLinkedButton.isSelected())
                    action = "Hide ";
                putValue(Action.NAME, action + mTitle);
            } else {
                putValue(Action.NAME, mTitle);
            }
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
                mWindow.setLocation(20,22);
            }
            firstDisplay = false;
            if (mLinkedButton.isSelected()) {
                mWindow.setVisible(true);
                mWindow.toFront();
                //VUE.ensureToolWindowVisibility(mTitle);
            } else {
                mWindow.setVisible(false);
                //VUE.ensureToolWindowVisibility(null);
            }
            
        }
    }

    static boolean inFullScreen() {
        return FullScreen.inFullScreen();
    }
    static boolean inNativeFullScreen() {
        return FullScreen.inNativeFullScreen();
    }

    static void toggleFullScreen() {
        toggleFullScreen(false);
    }
    
    static void toggleFullScreen(boolean goNative) {
        FullScreen.toggleFullScreen(goNative);
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
    
    public static void installExampleMap(LWMap map) {

        /*
         * create some test nodes & links
         */

        //map.addLWC(new LWImage(new MapResource("/Users/sfraize/Desktop/Test Image.jpg"))).setLocation(350, 90);
        
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

    static protected void out(Object o) {
        System.out.println("VUE: " + (o==null?"null":o.toString()));
    }
}
