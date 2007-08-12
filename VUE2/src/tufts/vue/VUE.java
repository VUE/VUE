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

import tufts.Util;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.action.*;
import tufts.vue.gui.*;
import tufts.vue.ui.InspectorPane;

import java.util.*;
import java.util.List;
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

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.NDC;
import org.apache.log4j.Level;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.PatternLayout;

import edu.tufts.vue.preferences.implementations.WindowPropertiesPreference;


/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 * @version $Revision: 1.471 $ / $Date: 2007-08-12 19:12:19 $ / $Author: sfraize $ 
 */

public class VUE
    implements VueConstants
{
    public static final Logger Log = Logger.getLogger(VUE.class);
    
    private static AppletContext sAppletContext = null;
    
    /** The currently active selection.
     * elements in ModelSelection should always be from the ActiveModel */
    static final LWSelection ModelSelection = new LWSelection();
    
    /** array of tool windows, used for repeatedly creating JMenuBar's for on all Mac JFrame's */
    // todo: wanted package private: should be totally private.
    public static Object[] ToolWindows; // VueMenuBar currently needs this

    /** teh global resource selection static model **/
    private static ResourceSelection sResourceSelection;
    
    //private static com.jidesoft.docking.DefaultDockableHolder frame;
    public static VueFrame ApplicationFrame;
    
    private static MapTabbedPane mMapTabsLeft;
    private static MapTabbedPane mMapTabsRight;
    public static JSplitPane mViewerSplit;
    
    //static ObjectInspectorPanel ObjectInspectorPanel;
    private static tufts.vue.ui.SlideViewer slideViewer;
    
    // TODO: get rid of this
    public static boolean  dropIsLocal = false;
    
    private static boolean isStartupUnderway = false;
    private static java.util.List FilesToOpen = Collections.synchronizedList(new java.util.ArrayList());

    private static InspectorPane inspectorPane = null;
    private static FormatPanel formattingPanel; 
    private static FloatingZoomPanel floatingZoomPanel; 
    private static PathwayPanel pathwayPanel = null;
    private static MapInspectorPanel mapInspectorPanel = null;
    private static edu.tufts.vue.rdf.RDFIndex RDFIndex;
    private static Object RDFIndexLock= new Object();

    //public static edu.tufts.vue.rdf.RDFIndex index = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex();

    public static edu.tufts.vue.rdf.RDFIndex getIndex() {
        if (RDFIndex == null && !SKIP_RDF_INDEX) {
            synchronized (RDFIndexLock) {
                if (RDFIndex == null)
                    RDFIndex = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex();
            }
        }
        return RDFIndex;
    }

   
    private static final ActiveInstance<LWPathway>
        ActivePathwayHandler = new ActiveInstance<LWPathway>(LWPathway.class) {
        protected void onChange(ActiveEvent<LWPathway> e) {
            // Only run the update if this isn't already an auxillary sync update from ActivePathwayEntryHandler:
            if (e.source instanceof ActiveEvent && ((ActiveEvent)e.source).type == LWPathway.Entry.class)
                return;
            
            if (e.active != null)
                ActivePathwayEntryHandler.setActive(e, e.active.getCurrentEntry());
            else
                ActivePathwayEntryHandler.setActive(e, null);
                
        }
    };


    private static final ActiveInstance<LWPathway.Entry>
        ActivePathwayEntryHandler = new ActiveInstance<LWPathway.Entry>(LWPathway.Entry.class) {
        protected void onChange(ActiveEvent<LWPathway.Entry> e) {
            // Only run the update if this isn't already an auxillary sync update from ActivePathwayHandler:
            if (e.source instanceof ActiveEvent && ((ActiveEvent)e.source).type == LWPathway.class)
                return;
            
            if (e.active != null)
                ActivePathwayHandler.setActive(e, e.active.pathway);
            else
                ActivePathwayHandler.setActive(e, null);

            if (e.active != null && e.active.node != null) {
                final LWSelection s = VUE.getSelection();
                if (!s.inNotify()) {
                    synchronized (s) {
                        if (!s.inNotify())
                            s.setTo(e.active.node);
                    }
                }
            }
        }
    };


    private static final ActiveInstance<LWMap>
        ActiveMapHandler = new ActiveInstance<LWMap>(LWMap.class) {
        protected void onChange(ActiveEvent<LWMap> e) {
            if (e.active != null) {
                ActivePathwayHandler.setActive(e, e.active.getActivePathway());
                if (e.active.getUndoManager() != null)
                    e.active.getUndoManager().updateGlobalActionLabels();
            } else
                ActivePathwayHandler.setActive(e, null);
        }
    };
    
    /**
     * For the currently active viewer (e.g., is visible
     * and has focus).  Actions (@see Actions.java) are performed on
     * the active map in the active viewer.
     * The active viewer can be null, which happens when we close the active viewer
     * and until another grabs the application focus (unles it was the last viewer).
     */
    private static final ActiveInstance<MapViewer> ActiveViewerHandler
        = new ActiveInstance<MapViewer>(MapViewer.class) {
        protected void notifyListeners(ActiveEvent<MapViewer> e) {
            if (!(e.active instanceof tufts.vue.ui.SlideViewer)) {
                // SlideViewer not treated as application-level viewer: ignore when gets selected
                super.notifyListeners(e);
                if (e.active != null)
                    ActiveMapHandler.setActive(e, e.active.getMap());
                else
                    ActiveMapHandler.setActive(e, null);
            }
        }
    };


    public static LWMap getActiveMap()          { return ActiveMapHandler.getActive(); }
    public static LWPathway getActivePathway()  { return ActivePathwayHandler.getActive(); }
    public static LWPathway.Entry getActiveEntry()  { return ActivePathwayEntryHandler.getActive(); }
    public static MapViewer getActiveViewer()   { return ActiveViewerHandler.getActive(); }
    
    public static void setActive(Class clazz, Object source, Object newActive) {
        if (newActive == null || clazz.isInstance(newActive))
            ActiveInstance.getHandler(clazz).setActive(source, newActive);
        else
            tufts.Util.printStackTrace("not an instance of " + clazz + ": " + newActive);
    }

    public static Object getActive(Class clazz) {
        // todo: if a handler doesn't already exist for this class, just return null
        return ActiveInstance.getHandler(clazz).getActive();
    }
    
    public static void addActiveListener(Class clazz, ActiveListener listener) {
        ActiveInstance.addListener(clazz, listener);
    }
    public static void addActiveListener(Class clazz, Object reflectedListener) {
        ActiveInstance.addListener(clazz, reflectedListener);
    }
    public static void removeActiveListener(Class clazz, ActiveListener listener) {
        ActiveInstance.removeListener(clazz, listener);
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
        // If we're an applet, System.getProperty will throw an AccessControlException
        if (false && isApplet())
            return null;
        else {
            String prop;
            try {
                prop = System.getProperty(name);
                if (DEBUG.INIT) out("got property " + name);
            } catch (java.security.AccessControlException e) {
                System.err.println(e);
                prop = null;
            }
            return prop;
        }
    }

    public static final String NullSystemProperty = "";
    
    /**
     * Getl's a system property, guaranteeing a non-null return value.
     * @return value or given system property, or an empty String
     */
    public static String getSystemPropertyValue(String name) {
        String value = getSystemProperty(name);
        if (value == null)
            return NullSystemProperty;
        else
            return value;
    }
    
    public static boolean isSystemPropertyTrue(String name) {
        String value = getSystemProperty(name);
        return value != null && value.toLowerCase().equals("true");
    }
    
    public static boolean hasSystemProperty(String name) {
        return getSystemProperty(name) != null;
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
        
    public static LWSelection getSelection() {
        return ModelSelection;
    }
    
    public static tufts.vue.ui.SlideViewer getSlideViewer()
    {
    	return slideViewer;
    }
    
    public static InspectorPane getInspectorPane() {
    	 return inspectorPane;
    }
    
    public static boolean isStartupUnderway() {
        return isStartupUnderway;
    }
    
    public static void activateWaitCursor() {
        GUI.activateWaitCursor();
    }
    
    public static void clearWaitCursor() {
        GUI.clearWaitCursor();
    }
    
    static void initUI() {
        GUI.init();
        
        try {
            if (DEBUG.Enabled && Util.isMacPlatform()) {
                // This is for debugging.  The application icon for a distributed version
                // of VUE is set via an icons file specified in the Info.plist from
                // the VUE.app directory.
                // Be sure to call this after GUI initialized, or we are hidden from the OSX app dock.
                tufts.macosx.MacOSX.setApplicationIcon
                    (VUE.class.getResource("/tufts/vue/images/vueicon32x32.gif").getFile());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
    }

    /** initialize based on command line args, and the initlaize the GUI */
    public static void init(String[] args) {
        if (args != null)
            parseArgs(args);
        initUI();
    }

    public static void init() {
        init(null);
    }
    
    public static void parseArgs(String[] args) {
        String allArgs = "";
        for (int i = 0; i < args.length; i++) {
            allArgs += "[" + args[i] + "]";
            if (args[i].equals("-nosplash")) {
                SKIP_SPLASH = true;
            } else if (args[i].equals("-nodr")) {
                DEBUG.Enabled = true;
                SKIP_DR = true;
            } else if (args[i].equals("-noem")) {
                SKIP_EDITOR_MANAGER = true;
            } else if (args[i].equals("-noidx")) {
                SKIP_RDF_INDEX = true;
            } else if (args[i].equals("-exit_after_init")) // for startup time trials
                exitAfterInit = true;
            else
                DEBUG.parseArg(args[i]);

            if (args[i].startsWith("-debug")) DEBUG.Enabled = true;

        }

        GUI.parseArgs(args);
        
        if (DEBUG.INIT) System.out.println("VUE: parsed args " + allArgs);
    }

    
    //-----------------------------------------------------------------------------
    // Variables used during VUE startup
    //-----------------------------------------------------------------------------
    
    private static boolean exitAfterInit = false;
    private static boolean SKIP_DR = false; // don't load DRBrowser, no splash & no startup map
    private static boolean SKIP_SPLASH = false;
    private static boolean SKIP_EDITOR_MANAGER = false;
    private static boolean SKIP_RDF_INDEX = false;
    private static String NAME;
	
    private static DRBrowser DR_BROWSER;
    private static DockWindow DR_BROWSER_DOCK;
    private static DockWindow pathwayDock;
    private static DockWindow formatDock;
    private static DockWindow slideDock;
    private static DockWindow pannerDock;
    private static DockWindow MapInspector;
    private static DockWindow ObjectInspector;
    private static DockWindow outlineDock;
    private static DockWindow floatingZoomDock;
    
    static {
        Logger.getRootLogger().removeAllAppenders(); // need to do this or we get everything twice
        //BasicConfigurator.configure();
        //Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("VUE %d [%t] %-5p %c:%x %m%n")));
        final PatternLayout pattern = new PatternLayout("VUE %d [%t] %-5p %x %m%n");
        Logger.getRootLogger().addAppender(new ConsoleAppender(pattern));
        Logger.getRootLogger().addAppender(new WriterAppender(pattern, Util.getLogWriter()));
        //Log.addAppender(new ConsoleAppender(new PatternLayout("[%t] %-5p %c %x - %m%n")));
        Log.setLevel(Level.INFO);

        //set tooltips to psuedo-perm
        ToolTipManager.sharedInstance().setDismissDelay(240000);
        ToolTipManager.sharedInstance().setInitialDelay(500);
        //if (VueUtil.isMacPlatform())
        //    installMacOSXApplicationEventHandlers();
    }
    
    public static void main(String[] args)
    {
        VUE.isStartupUnderway = true;

        parseArgs(args);
        
        Log.info("Startup; VUE build: " + tufts.vue.Version.AllInfo);
        Log.info("Platform: " + Util.getPlatformName());
        Log.info("Running in Java VM: " + getSystemProperty("java.runtime.version")
                 + "; MaxMemory(-Xmx)=" + VueUtil.abbrevBytes(Runtime.getRuntime().maxMemory())
                 + ", CurMemory(-Xms)=" + VueUtil.abbrevBytes(Runtime.getRuntime().totalMemory())
                 );

        Log.info("VUE version: " + VueResources.getString("vue.version"));
        Log.info("Current Working Directory: " + getSystemProperty("user.dir"));
        Log.info("User/host: " + getSystemProperty("user.name") + "@" + System.getenv("HOST"));
        
        if (DEBUG.Enabled)
            Log.setLevel(Level.DEBUG);

        if (VueUtil.isMacPlatform())
            installMacOSXApplicationEventHandlers();
            
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null || args[i].length() < 1 || args[i].charAt(0) == '-')
                continue;
            if (DEBUG.INIT) out("command-line file to open " + args[i]);
            VUE.FilesToOpen.add(args[i]);
        }
        
        try {

            initUI();
            initApplication();
            
        } catch (Throwable t) {
            Util.printStackTrace(t, "VUE init failed");
            VueUtil.alert("VUE init failed", t);
            /*
            StringWriter buf = new StringWriter();
            t.printStackTrace(new java.io.PrintWriter(buf));
            JComponent msg = new JTextArea("VUE init failed:\n" + buf);
            msg.setBorder(new EmptyBorder(22,22,22,22));
            tufts.Util.displayComponent(msg);
            */
        }


        VUE.isStartupUnderway = false;
        
        Log.info("startup completed.");
        
        if (exitAfterInit) {
            out("init completed: exiting");
            System.exit(0);
        }
    }

    private static void initApplication()
    {
        /*
        if (VUE.TUFTS)
            Log.debug("TUFTS features only (no MIT/development)");
        else
            Log.debug("MIT/development features enabled");
        */

        final Window splashScreen;

        if (SKIP_DR || SKIP_SPLASH) {
            splashScreen = null;
            DEBUG.Enabled = true;
        } else
            splashScreen = new SplashScreen();
        
        // Make sure these classes are all fully loaded to establish
        // their Keys.  todo: can get all subclasses of LWComponent
        // and newInstance them just to be sure.  In any case, this
        // probably isn't even required, but it's helping debugging
        // while implementing the new Key & Property LWComponent
        // code. -- SMF
        new LWLink();
        new LWImage();
        new LWNode();

        buildApplicationInterface();

        if (splashScreen != null)
            splashScreen.setVisible(false);

        boolean openedUserMap = false;

        Log.debug("loading disk cache...");
        Images.loadDiskCache();
        Log.debug("loading disk cache: done");

        if (FilesToOpen.size() > 0) {
            try {
                Iterator i = FilesToOpen.iterator();
                while (i.hasNext()) {
                    final String fileName = (String) i.next();
                    openedUserMap = true;
                    GUI.invokeAfterAWT(new Runnable() {
                            public void run() {
                                VUE.activateWaitCursor();
                                //LWMap map = OpenAction.loadMap(fileName);
                                if (DEBUG.INIT) out("opening map during startup " + fileName);
                                if (fileName != null) {
                                    displayMap(new File(fileName));
                                    //openedUserMap = true;
                                }
                            }});
                }
            } finally {
                VUE.clearWaitCursor();                
            }
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // ensure first item gets selected
                if (mMapTabsLeft.getTabCount() > 0)
                    mMapTabsLeft.setSelectedIndex(0); 
                // above would normally focus grab, but it skips these during startup,
                // so do it manually here: (so the pathway panel loads, etc)
                // [re-enabled focus grabs even during startup -- was mainly for command line debug loads]
                //mMapTabsLeft.getViewerAt(0).grabVueApplicationFocus("startup", null);
            }});
        }


        if (DEBUG.Enabled && !openedUserMap) {
        
            //if (SKIP_DR && FilesToOpen.size() == 0) {
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

        /*
        if (drBrowser != null) {
            drBrowser.loadDataSourceViewer();
            //if (VUE.TUFTS) // leave collapsed if NarraVision
            //splitPane.resetToPreferredSizes();
        }
        */

        //VUE.clearWaitCursor();

        Log.debug("loading fonts...");
        FontEditorPanel.getFontNames();
        
        if (SKIP_DR == false) {
            Log.debug("caching tool panels...");
            NodeTool.getNodeToolPanel();
           // LinkTool.getLinkToolPanel();
        }
        
        // Must call this after all LWEditors have been created and
        // put in the AWT hierarchy:
        if (!SKIP_EDITOR_MANAGER)
            EditorManager.install();
        
        // Start the loading of the data source viewer
        if (SKIP_DR == false && DR_BROWSER != null)
            DR_BROWSER.loadDataSourceViewer();
            /*
            DR_BROWSER.loadDataSourceViewer(DR_BROWSER_DOCK,
                                            searchDock,
                                            browseDock,
                                            savedResourcesDock,
                                            previewDock);
            */
        
        //Preferences p = Preferences.userNodeForPackage(VUE.class);
        //p.put("DRBROWSER.RUN", "yes, it has");

        // MAC v.s. PC WINDOW PARENTAGE & FOCUS BEHAVIOUR:
        //
        // Window's that are shown before their parent's are shown do NOT adopt a
        // stay-on-top-of-parent behaviour! (at least on mac).  FURTHERMORE: if you
        // iconfiy the parent and de-iconify it, the keep-on-top is also lost
        // permanently!  (Even if you hide/show the child window after that) None of
        // this happens on the PC, only Mac OS X.  Iconifying also hides the child
        // windows on the PC, but not on Mac.  On the PC, there's also no automatic way
        // to install the action behaviours to take effect (the ones in the menu bar)
        // when a tool window has focus.  Actually, mac appears to do something smart
        // also: if parent get's MAXIMIZED, it will return to the keep on top behaviour,
        // but you have to manually hide/show it to get it back on top.
        //
        // Also: for some odd reason, if we use an intermediate root window as the
        // master parent, the MapPanner display doesn't repaint itself when dragging it
        // or it's map!
        //
        // Addendum: keep-on-top now appears to survive iconification on mac.
        //
        // [ Assuming this is java 1.4 -- 1.5? ]
        
        // is done in buildApplicationInterface
        //getRootWindow().setVisible(true);

        //out("ACTIONTMAP " + java.util.Arrays.asList(frame.getRootPane().getActionMap().allKeys()));
        //out("INPUTMAP " + java.util.Arrays.asList(frame.getRootPane().getInputMap().allKeys()));
        //out("\n\nACTIONTMAP " + java.util.Arrays.asList(frame.getActionMap().allKeys()));
        //out("ACTIONTMAP " + Arrays.asList(VUE.getActiveViewer().getActionMap().allKeys()));
        //out("INPUTMAP " + Arrays.asList(VUE.getActiveViewer().getInputMap().keys()));
        //out("INPUTMAP " + Arrays.asList(getInputMap().keys()));

        //VUE.clearWaitCursor();

        
        Log.debug("initApplication completed.");
    }

    private static void installMacOSXApplicationEventHandlers()
    {
        if (!VueUtil.isMacPlatform())
            throw new RuntimeException("can only install OSX event handlers on Mac OS X");

        VUE.Log.debug("INSTALLING MAC OSX APPLICATION HANDLER");

        tufts.macosx.MacOSX.registerApplicationListener(new tufts.macosx.MacOSX.ApplicationListener() {
                public boolean handleOpenFile(String filename) {
                    VUE.Log.info("OSX OPEN FILE " + filename);
                    if (VUE.isStartupUnderway)
                        VUE.FilesToOpen.add(filename);
                    else
                        VUE.displayMap(new File(filename));
                    return true;
                }
                public boolean handleQuit() {
                    VUE.Log.debug("OSX QUIT");
                    ExitAction.exitVue();
                    // Always return false.  If we claim this is "handled",
                    // OSX  will do the quit for us, and even if the ExitAction
                    // was aborted, we'd exit anyway...
                    return false;
                }
                public boolean handleAbout() {
                    VUE.Log.debug("OSX ABOUT");
                    new AboutAction().act();
                    return true;
                }
                public boolean handlePreferences() {
                    VUE.Log.debug("OSX PREFERENCES");
                    Actions.Preferences.act();
                    return true;
                }
                
            });
    }

    private static final boolean ToolbarAtTopScreen = false && VueUtil.isMacPlatform();

    private static void buildApplicationInterface() {

        //-------------------------------------------------------
        // Create the tabbed panes for the viewers
        //-------------------------------------------------------
        
        mMapTabsLeft = new MapTabbedPane("*left", true);
        mMapTabsRight = new MapTabbedPane("right", false);
        
        //-------------------------------------------------------
        // Create the split pane
        //-------------------------------------------------------

        mViewerSplit = buildSplitPane(mMapTabsLeft, mMapTabsRight);
        
        //-------------------------------------------------------
        // create a an application frame and layout components
        //-------------------------------------------------------
        
        if (DEBUG.INIT) out("creating VueFrame...");

        VUE.ApplicationFrame = new VueFrame();

        if (DEBUG.INIT) out("created VueFrame");
        
        //-----------------------------------------------------------------------------
        // Man VUE Toolbar (map editing tool)
        //-----------------------------------------------------------------------------
        
        // The real tool palette window withtools and contextual tools
        VueToolbarController tbc = VueToolbarController.getController();
        
        ModelSelection.addListener(tbc);

        DockWindow toolbarDock = null;

        final JComponent toolbar;
        
        if (VueToolPanel.IS_CONTEXTUAL_TOOLBAR_ENABLED)
            toolbar = tbc.getToolbar();
        else
            toolbar = tbc.getToolbar().getMainToolbar();


        if (ToolbarAtTopScreen) {
            toolbarDock = GUI.createToolbar("Toolbar", toolbar);
        } else {
            ApplicationFrame.addComp(toolbar, BorderLayout.NORTH);
        }
        
        if (DEBUG.INIT) out("created ToolBar");

        
        //=============================================================================
        //
        // Create all the DockWindow's
        //
        //=============================================================================

        //-----------------------------------------------------------------------------
        // Pathways panel
        //-----------------------------------------------------------------------------
        pathwayPanel = new PathwayPanel(VUE.getDialogParentAsFrame());
        pathwayDock = GUI.createDockWindow(VueResources.getString("dockWindow.presentation.title"),
                                                            pathwayPanel);

        //-----------------------------------------------------------------------------
        // Formatting
        //-----------------------------------------------------------------------------

        //formatDock = null;
        formattingPanel = new FormatPanel();
        formatDock = GUI.createDockWindow("Format",true,true);
        formatDock.setContent(formattingPanel);
        formatDock.setFocusable(true);
        

        //-----------------------------------------------------------------------------
        // Formatting
        //-----------------------------------------------------------------------------

        //formatDock = null;
        floatingZoomPanel = new FloatingZoomPanel();
        floatingZoomDock = GUI.createDockWindow("Floating Zoom",true);
        floatingZoomDock.setContent(floatingZoomPanel);
        floatingZoomDock.setFocusable(true);
        floatingZoomDock.setSize(new Dimension(280,30));
        floatingZoomDock.setLocation(0,GUI.GInsets.top+15);        
        
        //-----------------------------------------------------------------------------
        // Panner
        //-----------------------------------------------------------------------------

        pannerDock = GUI.createDockWindow("Panner", new MapPanner());
        //pannerDock.getWidgetPanel().setBorder(new javax.swing.border.MatteBorder(5,5,5,5, Color.green));
        //pannerDock.getContentPanel().setBorder(new EmptyBorder(1,2,2,2));
        //pannerDock.setSize(120,120);
        //pannerDock.setSize(112,120);
        //pannerDock.setUpperRightCorner(GUI.GScreenWidth, 150);

        if (Util.isMacPlatform()) {
            // Can't do this on PC as 'x' close button is on right
            pannerDock.setMenuActions(new Action[] {
                    Actions.ZoomFit,
                    Actions.ZoomActual
                });
        }


        //-----------------------------------------------------------------------------
        // Resources Stack (Libraries / DRBrowser)
        // DRBrowser class initializes the DockWindow itself.
        //-----------------------------------------------------------------------------
        
        DR_BROWSER_DOCK = GUI.createDockWindow("Resources");
        //DockWindow searchDock = GUI.createDockWindow("Search");
        DockWindow searchDock = null;
        DR_BROWSER = new DRBrowser(true, DR_BROWSER_DOCK, searchDock);
        DR_BROWSER_DOCK.setSize(300, (int) (GUI.GScreenHeight * 0.75));
		
        //-----------------------------------------------------------------------------
        // Map Inspector
        //-----------------------------------------------------------------------------

        MapInspector = GUI.createDockWindow(VueResources.getString("mapInspectorTitle"));
        mapInspectorPanel = new MapInspectorPanel();
        if(InspectorPane.META_VERSION == InspectorPane.NEW)
          MapInspector.setContent(new edu.tufts.vue.metadata.ui.MetadataSearchGUI());
        else
          MapInspector.setContent(mapInspectorPanel);
        
        //-----------------------------------------------------------------------------
        // Object Inspector / Resource Inspector
        //-----------------------------------------------------------------------------

        //final DockWindow resourceDock = GUI.createDockWindow("Resource Inspector", new ResourcePanel());
        inspectorPane = new tufts.vue.ui.InspectorPane();
        ObjectInspector = GUI.createDockWindow("Info",inspectorPane);
        ObjectInspector.setMenuName("Info / Preview");
        ObjectInspector.setHeight(575);
        slideViewer = new tufts.vue.ui.SlideViewer(null);
        slideDock = GUI.createDockWindow(slideViewer);
        slideDock.setLocation(100,100);
        VueAction defSize;
        slideDock.setMenuActions(new Action[] {
                Actions.ZoomFit,
                Actions.ZoomActual,
                defSize = new VueAction("1/8 Screen") {
                    public void act() {
                        GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(slideDock);
                        Rectangle screen = gc.getBounds();
                        slideDock.setContentSize(screen.width / 4, screen.height / 4);
                    }
                },
                new VueAction("1/4 Screen") {
                    public void act() {
                        GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(slideDock);
                        Rectangle screen = gc.getBounds();
                        slideDock.setContentSize(screen.width / 2, screen.height / 2);
                    }
                },
                new VueAction("Maximize") {
                    public void act() {
                        slideDock.setBounds(GUI.getMaximumWindowBounds(slideDock));
                    }
                },
                
            });
        defSize.act();
        
        
        //-----------------------------------------------------------------------------
        // Object Inspector
        //-----------------------------------------------------------------------------

        //GUI.createDockWindow("Test OI", new ObjectInspectorPanel()).setVisible(true);
        /*
        ObjectInspector = GUI.createDockWindow(VueResources.getString("objectInspectorTitle"));
        ObjectInspectorPanel = new ObjectInspectorPanel();
        ModelSelection.addListener(ObjectInspectorPanel);
        ObjectInspector.setContent(ObjectInspectorPanel);
        */
        
        //-----------------------------------------------------------------------------
        // Pathway Panel
        //-----------------------------------------------------------------------------

        // todo
        
        //-----------------------------------------------------------------------------
        // Outline View
        //-----------------------------------------------------------------------------

        OutlineViewTree outlineTree = new OutlineViewTree();
        JScrollPane outlineScroller = new JScrollPane(outlineTree);
        VUE.getSelection().addListener(outlineTree);
        //VUE.addActiveMapListener(outlineTree);
        VUE.ActiveMapHandler.addListener(outlineTree);
        outlineScroller.setPreferredSize(new Dimension(500, 300));
        //outlineScroller.setBorder(null); // so DockWindow will add 1 pixel to bottom
        outlineDock =  GUI.createDockWindow("Outline", outlineScroller);
        
        //-----------------------------------------------------------------------------


        // GUI.createDockWindow("Font").add(new FontEditorPanel()); // just add automatically?

        //final DockWindow fontDock = GUI.createToolbar("Font", new FontPropertyPanel());
       // final DockWindow fontDock = GUI.createToolbar("Font", new FontEditorPanel(LWKey.Font));
        //final DockWindow linkDock = GUI.createToolbar("Link", new LinkPropertyPanel());
        //final DockWindow actionDock = GUI.createToolbar("Actions", new VueActionBar());
        //final DockWindow fontDock = null;
        final DockWindow linkDock = null;
        final DockWindow actionDock = null;

        //fontDock.setResizeEnabled(false);
        //linkDock.setResizeEnabled(false);
        
        //pannerDock.setChild(linkDock);
        
        //fontDock.setChild(linkDock);

        //fontDock.setLowerRightCorner(GUI.GScreenWidth, GUI.GScreenHeight);
        
        //=============================================================================
        //
        // Now that we have all the DockWindow's created the VueMenuBar, which needs the
        // list of Windows for the Window's menu.  The order they appear in this list is
        // the order they appear in the Window's menu.
        //
        //=============================================================================

        /*
         * This isn't currently used now but I have a feeling it'll come back if not i'll remove it.
         */
        //VUE.ToolWindows = new Object[] {
        	//unused stuff.
        	//searchDock,
        	/* keywords goes here when its done*/
        	//ObjectInspector,        	
        	/* Linear View goes here when its done*/
        	//MapInspector,
        	/* node inspector */
        	/*notes didn't end up getting its own window*/
        	//outlineDock,
        	//pannerDock,        	
        	//DR_BROWSER_DOCK,
        	//slideDock,                         
            //resourceDock,
            //formatDock,            
            //htWindow,
            //pathwayDock,            
        	//actionDock,
            //fontDock,
            //linkDock,
            // toolbarDock,                                               
            //};

        // adding the menus and toolbars
        if (DEBUG.INIT) out("setting JMenuBar...");
        ApplicationFrame.setJMenuBar(VueMenuBar.RootMenuBar = new VueMenuBar(/*VUE.ToolWindows*/));
        if (DEBUG.INIT) out("VueMenuBar installed.");;
        
        if (true)
            ApplicationFrame.addComp(mViewerSplit, BorderLayout.CENTER);
        else
            ApplicationFrame.addComp(mMapTabsLeft, BorderLayout.CENTER);
        
        try {
            ApplicationFrame.pack();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.error("OSX TIGER JAVA BUG at frame.pack()", e);
        }
        
        /*
        if (SKIP_DR) {
            ApplicationFrame.setSize(750,450);
        } else {
            ApplicationFrame.setSize(800,600);
            // todo: make % of screen, make sure tool windows below don't go off screen!
        }
        */
        
        //if (DEBUG.INIT) out("validating frame...");
        ApplicationFrame.validate();
        //if (DEBUG.INIT) out("frame validated");

        int appWidth = (int) (GUI.GScreenWidth * 0.75);
        int appHeight = (int) (GUI.GScreenHeight * 0.75);

        // If you've got a wide screen, leave at least 600
        // pixels at the right for two full 300pix DockWindow's
        if (GUI.GScreenWidth >= 1600) { 
            int maxWidth = GUI.GScreenWidth - (GUI.GInsets.left + DockWindow.DefaultWidth * 2);
            if (appWidth > maxWidth)
                appWidth = maxWidth;
        }

        if (appWidth > 1600)
            appWidth = 1600;
        if (appHeight > 1024)
            appHeight = 1024;

        WindowPropertiesPreference wpframe = ApplicationFrame.getWindowProperties();
        
        Dimension sz = wpframe.getWindowSize();
    	Point pos = wpframe.getWindowLocationOnScreen();
    	
        if (wpframe.isEnabled() && !wpframe.isAllValuesDefaults() &&
        	ApplicationFrame.isPointFullyOnScreen(pos,sz))
        {
        	
        	ApplicationFrame.setSize((int)sz.getWidth(), (int)sz.getHeight());

        	ApplicationFrame.setLocation((int)pos.getX(),(int)pos.getY());        	
        }
        else
        {        	        
        	ApplicationFrame.setSize(appWidth, appHeight);

        	ApplicationFrame.setLocation(GUI.GInsets.left,
                                     GUI.GInsets.top
                                     + (ToolbarAtTopScreen ? DockWindow.ToolbarHeight : 0));
        }
        // MAC NOTE WITH MAXIMIZING: if Frame's current location y value
        // is less than whatever's it's maximized value is set to, maximizing
        // it will use the y value, not the max value.  True even if set
        // y value after setting to maximized but before it's put on screen.
        
        //GUI.centerOnScreen(ApplicationFrame);

        final boolean loadTopDock = false;

        if (loadTopDock && DockWindow.getMainDock() != null) {
            // leave room for dock at top
            Rectangle maxBounds = GUI.getMaximumWindowBounds();
            int adj = DockWindow.getCollapsedHeight();
            maxBounds.y += adj;
            maxBounds.height -= adj;
            ApplicationFrame.setMaximizedBounds(maxBounds);
        }
            
        if (false)
            ApplicationFrame.setExtendedState(Frame.MAXIMIZED_BOTH);

        /*
        if (!SKIP_DR) {
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
        */

        if (FilesToOpen.size() == 0)
            VUE.displayMap(new LWMap("New Map"));

        // Generally, we need to wait until java 1.5 JSplitPane's have been validated to
        // use the % set divider location.  Unfortunately there's a bug in at MacOS java
        // 1.5 BasicSplitPaneUI (it's not in the 1.4 version), where setKeepHidden isn't
        // being called when the divider goes to the wall via setDividerLocation, only when
        // the one-touch buttons are manually clicked.  So, for example, if the user
        // de-maximizes the frame, suddenly a hidden split-pane will pop out!  So, we've
        // hacked into the UI code, grabbed the damn right-one-touch button, grabbed
        // it's action listener, and here just call it directly...
        // 
        // See javax.swing.plaf.basic.BasicSplitPaneDivider.OneTouchActionHandler.
        //
        // It appears on Windows we need to actually wait till the frame is shown also...

        // show before split adjust on pc
        if (!Util.isMacPlatform())
            ApplicationFrame.setVisible(true);
        
        if (SplitPaneRightButtonOneTouchActionHandler != null) {
            if (DEBUG.INIT) Util.printStackTrace("\"pressing\": " + SplitPaneRightButtonOneTouchActionHandler);

            // Not reliable on PC unless we invokeLater
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);                  
            }});
        
            // this is also eventually getting eaten in java 1.5: no matter where
            // we put this call during init: will have to patch w/more hacking
            // or live with it.  Actually, it get's eaten eventually in java 1.4.2
            // also.

            // Maybe because we maximized the frame before it was shown?
            // [ not making a difference]

            // Well. this is working at least the first time now by
            // doing it BEFORE the peers are created.
            //mViewerSplit.setResizeWeight(0.5d);
            
        } else {
            // for java 1.4.2
            mViewerSplit.setDividerLocation(1.0);
        }

        // can show after split adjust on mac (turns out: only on older, slower macs)
        if (Util.isMacPlatform()) {
            ApplicationFrame.setVisible(true);

            if (SplitPaneRightButtonOneTouchActionHandler != null) {
                // This is backup: hit it one more time just in case, as on the
                // newer, faster intel Mac's, the timing is changed and the
                // above is no longer catching it.
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);                  
                }});
            }
        }
        

        if (toolbarDock != null) {
            toolbarDock.suggestLocation(0,0);
            toolbarDock.setWidth(GUI.GScreenWidth);
            toolbarDock.setVisible(true);
        }

        //-----------------------------------------------------------------------------
        //
        // Set locations for the inspector windows and make some of them visible
        //
        //-----------------------------------------------------------------------------


        // order the windows left to right for the top dock
        List<DockWindow> acrossTopList = new ArrayList<DockWindow>();
        /*{
            MapInspector,
            //GUI.isSmallScreen() ? null : fontDock,
            pathwayDock,
            //formatDock,
            DR_BROWSER_DOCK,
            ObjectInspector,
            //resourceDock,
        };
          */
        if (!MapInspector.getWindowProperties().isEnabled() || !MapInspector.getWindowProperties().isWindowVisible())
        	acrossTopList.add(MapInspector);
          
        if (!pathwayDock.getWindowProperties().isEnabled() || !pathwayDock.getWindowProperties().isWindowVisible())
        	acrossTopList.add(pathwayDock);
        
        if (!DR_BROWSER_DOCK.getWindowProperties().isEnabled() || !DR_BROWSER_DOCK.getWindowProperties().isWindowVisible())
        	acrossTopList.add(DR_BROWSER_DOCK);        
        if (!ObjectInspector.getWindowProperties().isEnabled() || !ObjectInspector.getWindowProperties().isWindowVisible())
        	acrossTopList.add(ObjectInspector);
        
        final DockWindow[] acrossTop = acrossTopList.toArray(new DockWindow[acrossTopList.size()]);
        
        outlineDock.setLowerLeftCorner(0,
                                       GUI.GScreenHeight - GUI.GInsets.bottom);
        pannerDock.setLowerRightCorner(GUI.GScreenWidth - GUI.GInsets.right,
                                       GUI.GScreenHeight - GUI.GInsets.bottom);
        if (DockWindow.getTopDock() != null)
            prepareForTopDockDisplay(acrossTop);
        if (acrossTop.length > 0)
        {
        
        // Run after AWT to ensure all peers to have been created & shown                
        	GUI.invokeAfterAWT(new Runnable() { public void run() {
        		positionForDocking(acrossTop);
        	}});
       }
        
        if (false) {
            // old positioning code
            int inspectorx = ApplicationFrame.getX() + ApplicationFrame.getWidth();
            MapInspector.suggestLocation(inspectorx, ApplicationFrame.getY());
            ObjectInspector.suggestLocation(inspectorx, ApplicationFrame.getY() + MapInspector.getHeight() );
            pannerDock.suggestLocation(ApplicationFrame.getX() - pannerDock.getWidth(), ApplicationFrame.getY());
        }
        
        //restore window
        DR_BROWSER_DOCK.positionWindowFromProperties();
        pathwayDock.positionWindowFromProperties();

       	formatDock.positionWindowFromProperties();
        
        slideDock.positionWindowFromProperties();
        pannerDock.positionWindowFromProperties();
        MapInspector.positionWindowFromProperties();
        ObjectInspector.positionWindowFromProperties();
        outlineDock.positionWindowFromProperties();

        if (!SKIP_DR && (!DR_BROWSER_DOCK.getWindowProperties().isEnabled() || DR_BROWSER_DOCK.getWindowProperties().isAllValuesDefaults()))
        {
        	//I'm just putting a comment in here becuase this seems odd to me, and I wanted it to be clear it was intentional.
        	//"As we move away from a "datasource" centric vision of VUE, the "Content" window should be collapsed when launching VUE"
        	//This will only take effect the first time VUE is started or when preference to remember window position is disabled.
        	// -MK

        	//Different sized combobox components on each platform require this box to be two different
        	//sizes.
        	if (Util.isMacPlatform())
                    ;//formatDock.setSize(new Dimension(690,54));
        	else
        		formatDock.setSize(new Dimension(620,54));
        	//DR_BROWSER_DOCK.showRolledUp();
        	formatDock.setLocation(GUI.GInsets.left+300,
                    GUI.GInsets.top+25);        	
        	
            	
        //	formatDock.setVisible(true);
        	//DR_BROWSER_DOCK.setVisible(true);
            
        }


        /*
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            //pannerDock.setVisible(true);
            if (DEBUG.Enabled) linkDock.setVisible(true);
            if (DEBUG.Enabled) fontDock.setVisible(true);
        }});
        */


    }

    public static FormatPanel getFormattingPanel()
    {
    	return formattingPanel;
    }
        
    public static FloatingZoomPanel getFloatingZoomPanel()
    {
    	return floatingZoomPanel;
    }
    public static DockWindow getInfoDock()
    {
    	return ObjectInspector;
    }
    
    public static DockWindow getFloatingZoomDock()
    {
    	return floatingZoomDock;
    }
    
    public static DockWindow getContentDock()
    {
    	return DR_BROWSER_DOCK;
    }
    
    public static DockWindow getPannerDock()
    {
    	return pannerDock;
    }
    
    public static DockWindow getOutlineDock()
    {
    	return outlineDock;
    }
    
    public static DockWindow getSlideDock()
    {
    	return slideDock;
    }
    
    public static MapInspectorPanel getMapInspectorPanel()
    {
    	return mapInspectorPanel;
    }
    
    public static DockWindow getMapInfoDock()
    {
    	return MapInspector;
    }
    
    public static DockWindow getFormatDock()
    {
    	return formatDock;
    }
    
    public static DockWindow getPresentationDock()
    {
    	return pathwayDock;
    }
    
    public static PathwayPanel getPathwayPanel()
    {
    	return pathwayPanel;
    }

    /**
     * Get the given windows displayed, but off screen, ready to be moved
     * into position.
     */
    private static void prepareForTopDockDisplay(final DockWindow[] preShown)
    {
        if (DEBUG.INIT || DEBUG.DOCK) Util.printStackTrace("\n\n***ROLLING UP OFFSCREEN");

        // get the peer's created so we can turn off their shadow if need be
        
        for (int i = 0; i < preShown.length; i++) {
            DockWindow dw = preShown[i];
            if (dw == null)
                continue;
            GUI.setOffScreen(dw);

            dw.setDockTemporary(DockWindow.getTopDock());
            
            dw.showRolledUp();
        }

    }

    /**
     * Get the given windows displayed, but off screen, ready to be moved
     * into position.
     */
    // todo: this no longer as to do with our DockRegions: is just use for positioning
    private static void positionForDocking(DockWindow[] preShown) {
        // Set last in preSown at the right, moving back up list
        // setting them to the left of that, and then set first in
        // preShown at left edge of screen

        if (DEBUG.INIT || (DEBUG.DOCK /*&& DEBUG.META*/)) Util.printStackTrace("\n\nSTARTING PLACEMENT");
        
        int top = GUI.GInsets.top;

        if (ToolbarAtTopScreen)
            top += DockWindow.ToolbarHeight;
        else
            top += 52; // todo: tweak for PC

        boolean squeezeDown = GUI.GScreenWidth < 1200;
        boolean didSqueeze = false;

        int nextLayout = preShown.length - 1;

        if (squeezeDown) {
            // Swap last two, so "last" is the one pushed down
            DockWindow tmp = preShown[nextLayout];
            preShown[nextLayout] = preShown[nextLayout - 1];
            preShown[nextLayout - 1] = tmp;
        }
        
        DockWindow toRightDW = preShown[nextLayout];
        toRightDW.setUpperRightCorner(GUI.GScreenWidth, top);
        if (squeezeDown)
            toRightDW.setHeight(toRightDW.getHeight() / 2);
        DockWindow curDW = null;
        while (--nextLayout > 0) {
            curDW = preShown[nextLayout];
            if (squeezeDown && !didSqueeze) {
                didSqueeze = true;
                toRightDW.addChild(curDW);
            } else {
                curDW.setUpperRightCorner(toRightDW.getX(), top);
                toRightDW = curDW;
            }
        }
        if (preShown.length > 1)
            preShown[0].setLocation(0, top);
            
        DockWindow.assignAllDockRegions();
    }
    
    

    
    private static ActionListener SplitPaneRightButtonOneTouchActionHandler = null;

    public static void toggleSplitScreen()
    {
    	if (mViewerSplit.getDividerLocation() >= mViewerSplit.getMaximumDividerLocation())    	
    		mViewerSplit.setDividerLocation(0.50D);    		
    	else
    		  SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);
      
       
    	 
    }
    private static JSplitPane buildSplitPane(Component leftComponent, Component rightComponent)
    {
        JSplitPane split;
        if (Util.getJavaVersion() < 1.5f) {
            split = new JSplitPane();
        } else {

            // Only appears to happen on the Mac?  But even if we're
            // running with Metal Look and Feel??
            
            split = new JSplitPane() {
                
                // This JSplitPane hack is dependent on the UI implementation, but it only
                // uses the cross platform parts: see bottom of this method for more info.
                
                Container divider;
                public void XsetUI(javax.swing.plaf.SplitPaneUI newUI) {
                    Util.printStackTrace("setUI: " + newUI);
                    super.setUI(newUI);
                }
                protected void addImpl(Component c, Object constraints, int index) {
                    //out("splitPane.addImpl: index=" + index + " constraints=" + constraints + " " + GUI.name(c));
                    if (c instanceof javax.swing.plaf.basic.BasicSplitPaneDivider) {
                        //Util.printStackTrace("addImpl: divider is " + c);
                        divider = (Container) c;
                    }
                    super.addImpl(c, constraints, index);
                }
                public void addNotify() {
                    //Util.printStackTrace("splitPane.addNotify");
                    super.addNotify();
                    try {
                    
                        // Util.printStackTrace("addNotify");
                        //AbstractButton jumpLeft = (AbstractButton) divider.getComponent(0);
                        AbstractButton jumpRight = (AbstractButton) divider.getComponent(1);
                        //System.err.println("child0 " + jumpLeft);
                        //System.err.println("child1 " + jumpRight);
                        
                        //System.err.println(Arrays.asList(jumpLeft.getActionListeners()));
                        //System.err.println(Arrays.asList(jumpRight.getActionListeners()));
                        
                        // todo: as long as we're grabbing this out, add a short-cut key
                        // to activate it: the arrow buttons are so damn tiny!

                        SplitPaneRightButtonOneTouchActionHandler
                            = jumpRight.getActionListeners()[0];
                        
                        // BTW: can't call action listener now: must wait till after validation
                        
                    } catch (Throwable t) {
                        Util.printStackTrace(t);
                    }
                }
            };
        }

        split.setName("splitPane");
        split.setResizeWeight(0.5d);
        split.setOneTouchExpandable(true);
        split.setRightComponent(rightComponent);
        
        // NOTE: set left component AFTER set right component -- the LAST set left/right
        // call determines the default focus component  It needs to be the LEFT
        // component as the right one isn't even visible at startup.
        
        split.setLeftComponent(leftComponent);
        

        split.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    //System.out.println("VS " + e);
                    if (!e.getPropertyName().equals("dividerLocation"))
                        return;
                    if (DEBUG.TOOL || DEBUG.INIT || DEBUG.FOCUS)
                        out("split.propertyChange[" + e.getPropertyName() + "] "
                                        + "\n\tnew=" + e.getNewValue().getClass().getName()
                                        + " " + e.getNewValue()
                                        + "\n\told=" + e.getOldValue()
                                        + "\n\tsrc=" + GUI.name(e.getSource())
                                        );

                        //Util.printStackTrace();
                    
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
                        leftViewer.setVisible(true);
                        rightViewer.setVisible(true);
                    } else {
                        if (leftViewer != null && leftViewer != getActiveViewer()) {
                            if (DEBUG.TOOL || DEBUG.FOCUS)
                                out("split: active viewer: " + getActiveViewer()
                                    + " focus going to " + leftViewer);
                            leftViewer.requestFocus();
                            if (rightViewer != null)
                                //rightViewer.fireViewerEvent(MapViewerEvent.HIDDEN);
                                rightViewer.setVisible(false);
                        }
                    }
                }});

        return split;
    }

    /*
    private static void XbuildToolbar(DockWindow toolbarDock, JPanel toolPanel) {
        
        if (JIDE_TEST) {
            /* JIDE ENABLE
            frame.getDockableBarManager().addDockableBar(new VueToolBar());
            frame.getDockableBarManager().setShowInitial(false);            
            frame.getDockableBarManager().resetToDefault();
            *
        } else if (true||VUE.TUFTS) {
            //toolBarPanel = new JPanel();
            //toolBarPanel.add(tbc.getToolbar());
            if (toolbarDock == null)
                //ApplicationFrame.addComp(tbc.getToolbar(), BorderLayout.NORTH);
                ApplicationFrame.addComp(toolPanel, BorderLayout.NORTH);
        } else {

            //JDialog.setDefaultLookAndFeelDecorated(false);
            
            JPanel toolBarPanel = null;
            toolBarPanel = new JPanel(new BorderLayout());
            //toolBarPanel.add(tbc.getToolbar(), BorderLayout.NORTH);
            JPanel floatingToolbarContainer = new JPanel(new BorderLayout());
            //JPanel floatingToolbarContainer = new com.jidesoft.action.DockableBarDockableHolderPanel(frame);
            
            //floatingToolbarContainer.setPreferredSize(new Dimension(500,50));
            //floatingToolbarContainer.setMinimumSize(new Dimension(500,5));
            floatingToolbarContainer.setBackground(Color.orange);
            VueToolBar vueToolBar = new VueToolBar();
            floatingToolbarContainer.add(vueToolBar, BorderLayout.PAGE_START);
            //toolBarPanel.add(new VueToolBar(), BorderLayout.SOUTH);
            if (false) {
                // Yes: drop-downs work in a JToolBar (note that our MenuButtons
                // that are rounded become square tho)
                JToolBar tb = new JToolBar();
                tb.add(tbc.getToolbar());
                toolBarPanel.add(tb);
            } else {
                toolBarPanel.add(tbc.getToolbar(), BorderLayout.NORTH);
            }
            toolBarPanel.add(floatingToolbarContainer, BorderLayout.SOUTH);
            ApplicationFrame.addComp(toolBarPanel, BorderLayout.NORTH);

            ////frame.getDockableBarManager().addDockableBar(vueToolBar);
            
        }
    }

*/
    
//     public static int openMapCount() {
//         return ActiveMapHandler.instanceCount();
//         //return mMapTabsLeft == null ? 0 : mMapTabsLeft.getTabCount();
//     }

    
    public static boolean multipleMapsVisible() {
        if (mViewerSplit == null)
            return false;
        // TODO: this is no longer a reliable method of determining this in java 1.5
        int dl = mViewerSplit.getDividerLocation();
        return dl >= mViewerSplit.getMinimumDividerLocation()
            && dl <= mViewerSplit.getMaximumDividerLocation();
        
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

    public static boolean isActiveViewerOnLeft() {
        final MapViewer activeViewer = ActiveViewerHandler.getActive();
        return activeViewer == null || activeViewer.getName().startsWith("*");
    }

    public static boolean isActiveViewerOnRight() {
        final MapViewer activeViewer = ActiveViewerHandler.getActive();
        return activeViewer != null && activeViewer.getName().equals("right");
    }
    
    
    public static UndoManager getUndoManager() {
        
        // todo: eventually, way may want to ask the active viewer for
        // it's undo manager, e.g. -- if we want the slide viewer to
        // have it's own undo queue.
        
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
       //update the windows properties
        DR_BROWSER_DOCK.saveWindowProperties();
        pathwayDock.saveWindowProperties();
        if (formatDock != null)
        	formatDock.saveWindowProperties();
        slideDock.saveWindowProperties();
        pannerDock.saveWindowProperties();
        MapInspector.saveWindowProperties();
        ObjectInspector.saveWindowProperties();
        outlineDock.saveWindowProperties();
        ApplicationFrame.saveWindowProperties();
       
        if (mMapTabsLeft == null) // so debug harnesses can quit (no maps displayed)
            return true;
       
        // TODO: use active map instances
        int tabs = mMapTabsLeft.getTabCount();
        //LWMap ensureChecked = getActiveMap(); // in case of full-screen
        for (int i = 0; i < tabs; i++) {
            final LWMap map = mMapTabsLeft.getMapAt(i);
            if (VUE.getIndex() != null) {
                System.out.println("indexing map "+map.getLabel()+ " index size="+VUE.getIndex().size());
                try {
                    VUE.getIndex().index(map);
                } catch (Throwable t) {
                    Util.printStackTrace(t, "Exception indexing duing exit: " + map);
                }
            }
            
//             if (map == ensureChecked)
//                 ensureChecked = null;
            if (!askSaveIfModified(mMapTabsLeft.getMapAt(i)))
                return false;
        }

        if (getIndex() != null) {
            try {
                VUE.getIndex().write(new FileWriter(VueUtil. getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file")));
                System.out.println("Writing index to"+VueUtil. getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file"));
            } catch (Throwable t) {
                System.out.println("Exception attempting to save index " +t);
                t.printStackTrace();
            }
        }

        return true;
        
//         if (ensureChecked != null && !askSaveIfModified(ensureChecked))
//             return false;
//         else
//             return true;
             
    }
    
    /*
     * Returns true if either they save it or say go ahead and close w/out saving.
     */
    private static boolean askSaveIfModified(LWMap map) {
        //final Object[] defaultOrderButtons = { "Save", "Don't Save", "Cancel"};
    	final Object[] defaultOrderButtons = { "Don't Save","Cancel","Save"};
    	final Object[] macOrderButtons = { "Save","Cancel","Don't Save"};
        // oddly, mac aqua is reversing order of these buttons
        //final Object[] macAquaOrderButtons = { "Cancel", "Don't Save", "Save" };
        
    	
        if (!map.isModified())
            return true;

        // todo: won't need this if full screen is child of root frame
        if (inNativeFullScreen())
            toggleFullScreen();
        
        int response = JOptionPane.showOptionDialog
            (VUE.getDialogParent(),
        
             "Do you want to save the changes you made to \n"
             + "'" + map.getLabel() + "'?"
             + (DEBUG.EVENTS?("\n[modifications="+map.getModCount()+"]"):""),
        
             " Save changes?",
             JOptionPane.YES_NO_CANCEL_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             null,
             Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
             "Save"
             );
        
     
        if (!Util.isMacPlatform()) {
            switch (response) {
            case 0: response = 1; break;
            case 1: response = 2; break;
            case 2: response = 0; break;
            }
        } else {
            switch (response) {
            case 0: response = 0; break;
            case 1: response = 2; break;
            case 2: response = 1; break;
            }
        }
        
        // If they change focus to another button, then hit "return"
        // (v.s. "space" for kbd button press), do action of button
        // that had focus instead of always save?
        
        if (response == JOptionPane.YES_OPTION) { // Save
            return SaveAction.saveMap(map);
        } else if (response == JOptionPane.NO_OPTION) { // Don't Save
            // don't save -- just close
            return true;
        } else // anything else (Cancel or dialog window closed)
            return false;
    }
    
    public static void closeMap(LWMap map) {
        if (askSaveIfModified(map)) {
            mMapTabsLeft.closeMap(map);
            mMapTabsRight.closeMap(map);
        }
    }
    
    
    /**
     * If we already have open a map tied to the given file, display it.
     * Otherwise, open it anew and display it.
     */
    public static void displayMap(File file) {
        if (DEBUG.INIT || DEBUG.IO) out("displayMap " + file);

        if (file == null)
            return;

		/*
		* If there is 1 map open, and it has no content and hasn't been saved yet close it.
		* requested in vue-520 
		*/
        if (isActiveViewerOnLeft())
        {
        	if ((mMapTabsLeft != null) && 
        			mMapTabsLeft.getTabCount() == 1 && 
        			(getActiveMap() != null) && 
        			!getActiveMap().hasContent() && 
        			getActiveMap().getFile() == null)
        		closeMap(getActiveMap());
        	
        } else 
        {
            	if ((mMapTabsRight != null) && 
            			mMapTabsRight.getTabCount() == 1 && 
            			(getActiveMap() != null) && 
            			!getActiveMap().hasContent() && 
            			getActiveMap().getFile() == null)
            		closeMap(getActiveMap());
            	
        }

        
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(file)) {
                if (DEBUG.Enabled) out("displayMap found existing open map " + map + " matching file " + file);
                if (isActiveViewerOnLeft())
                    mMapTabsLeft.setSelectedIndex(i);
                else
                    mMapTabsRight.setSelectedIndex(i);
                return;
            }
        }
        
//         for (LWMap map : ActiveMapHandler.getAllInstances()) {
//             File existingFile = map.getFile();
//             if (existingFile != null && existingFile.equals(file)) {
//                 if (DEBUG.Enabled) out("displayMap found existing open map " + map + " matching file " + file);
//                 ActiveMapHandler.setActive(file, map);
//                 // TODO: sanity check this... (oh, and I supposed we can use the tab panes again... don't need active instances tracking!)
//                 //mMapTabsLeft.setSelectedIndex(i);
//                 return;
//             }
//         }

        final RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
        rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
        VUE.activateWaitCursor();
        LWMap loadedMap = null;
        boolean alerted = false;
        try {
            loadedMap = OpenAction.loadMap(file.getAbsolutePath());
            if (loadedMap != null)
                VUE.displayMap(loadedMap);
        } catch (Throwable t) {
            Util.printStackTrace(t, "failed to load map[" + file + "]");
            VUE.clearWaitCursor();
            alerted = true;
            VueUtil.alert("Failed to load map: " + file + "  \n"
                          + (t.getCause() == null ? t : t.getCause()),
                          "Map error: " + file);
        } finally {
            VUE.clearWaitCursor();
        }
        if (loadedMap == null && !alerted)
            VueUtil.alert("Failed to load map: " + file + "  \n", "Map error: " + file);
        
    }
    
    /**
     * Create a new viewer and display the given map in it.
     */
    public static MapViewer displayMap(LWMap pMap) {
        NDC.push("[displayMap]");
        if (DEBUG.INIT) out(pMap.toString());
        MapViewer leftViewer = null;
        MapViewer rightViewer = null;
        
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(pMap.getFile())) {
                Util.printStackTrace("warning: found open map with same file: " + map);
                //Log.error("** found open map with same file! " + map);
                // TODO: pop dialog asking to revert existing if there any changes.
                //break;
            }
        }

        
        if (leftViewer == null) {
            leftViewer = new MapViewer(pMap, "*LEFT");
            rightViewer = new MapViewer(pMap, "right");

            // Start them both off unfocusable, so we get no
            // focus transfers until we're ready to decide what
            // wants to get the focus.
            leftViewer.setFocusable(false);
            rightViewer.setFocusable(false);

//             if (rightViewer != null && isActiveViewerOnLeft()) {
//                 // so doesn't grab focus till we're ready
//                 // NOTE: grabVueApplicationFocus restore's focusability
//                 // when called directly -- which is why it must
//                 // be called directly to ensure focus grabs
//                 // in right viewers.
//                 rightViewer.setFocusable(false); 
//             }

            if (DEBUG.FOCUS) {
                out("currently active viewer: " + getActiveViewer());
                out("created new left viewer: " + leftViewer);
            }

            mMapTabsLeft.addViewer(leftViewer);
            mMapTabsRight.addViewer(rightViewer);
        }
        
        if (isActiveViewerOnLeft()) {
            mMapTabsLeft.setSelectedComponent(leftViewer);
        } else {
            mMapTabsRight.setSelectedComponent(rightViewer);
        }

        NDC.pop();
        return leftViewer;
    }

    
    /**
     * @return the root VUE Frame used for parenting dialogs -- currently always NULL do to java
     * bugs w/dialogs
     */
    
    // WARNING: opening a dialog appears to cause our full-screen
    // window as root parent of everything hack to fail and
    // permit DockWindow's to start going over it -- thus we must
    // use "null" as a parent.  TODO: we'll prob need to do this
    // for all dialogs...  We can still manually center the
    // window if we like...
    // CORRECTION: popping this at ALL seems to do it
    
    public static Component getDialogParent() {
         	
        final Component dialogParent;

        // any dialog parent at all in 1.4.2 causes the full-screen
        // window to go behind the DockWindow's
        
        if (Util.getJavaVersion() >= 1.5f)
            dialogParent = getActiveViewer();
        else
            dialogParent = null;

        if (DEBUG.FOCUS) out("getDialogParent: " + dialogParent);
        
        return dialogParent;

        /*
        // this is not helping for preving dialogs from screwing us up and
        // sending them behind the full-screen window as soon as the dialog pops
        if (true)
            // this will put dialogs at screen bottom when it's off-screen
            return GUI.getFullScreenWindow();
        else
            return null;
        */
    }
    
    public static Frame getDialogParentAsFrame() {
        Frame frame;
        if (getDialogParent() instanceof Frame) {
            frame = (Frame) getDialogParent();
        } else {
            
            // JOptionPane's will take any Component as a parent, but they just do a
            // search up for the root frame as the parent of the JDialog.  But if we
            // return our real root frame here, to be used with a raw JDialog, things
            // behave differently: it allows the dialog to go behind.  Don't know what
            // JOptionPane is causing to happen differently.  E.g., the "are you sure"
            // before quit dialog works fine and doesn't go behind it's parent, but raw
            // JDialogs constructed with an invisible parent CAN go behind... Oh, wait a
            // sec.. what if we use our DockWindow parent...

            // We're in the java 1.5 case here: if it's parented to the application
            // frame, it lets DockWindow's go behind full-screen (yet JOptionPane
            // created dialogs don't).  If it's parented to a hidden frame, the dialog
            // itself can go behind either full-screen or ApplicationFrame.

            // So for now, in 1.5, Dialog's wanting a frame get this special hidden
            // frame, and the FocusManager forces them alwaysOnTop when they're shown.

            frame = GUI.getHiddenDialogParentFrame();
            
            //frame = DockWindow.getHiddenFrame();
            
        }

        if (DEBUG.FOCUS) out("getDialogParentAsFrame: " + frame);
        return frame;
    }

    
    public static VueMenuBar getJMenuBar() {
        return VueMenuBar.RootMenuBar;
        //return (VueMenuBar) ((VueFrame)getRootWindow()).getJMenuBar();
    }
    

    /** Return the main VUE window.  Usually == getRoowWindow, unless we're
     * using a special root window for parenting the tool windows.
     */
    // todo: wanted package private
    public static Window getMainWindow() {
        return VUE.ApplicationFrame;
    }
    
    /** return the root VUE window, mainly for those who'd like it to be their parent */
    public static Window getRootWindow() {
        return VUE.ApplicationFrame;
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


    /*
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
                JMenuBar menu = new VueMenuBar();
                f.setJMenuBar(menu);
            }
            f.show();
            //rootFrame = createFrame();
        } finally {
            makingRootFrame = false;
        }
        return f;
    }
    */



    public static String getName() {
        if (NAME == null)
            NAME = VueResources.getString("application.name");
        return NAME;
    }
    
    public static ResourceSelection getResourceSelection() {
        if (sResourceSelection == null)
            sResourceSelection = new ResourceSelection();
        return sResourceSelection;
    }

    
    /** return the root VUE application frame (where the documents are) */
    public static Frame getApplicationFrame() {
        //if (getRootWindow() instanceof Frame)
        //    return (Frame) getRootWindow();
        //else
        return VUE.ApplicationFrame;
    }
    

    /** @return a new JWindow, parented to the root VUE window */
    public static JWindow createWindow()
    {
        return new JWindow(getRootWindow());
    }

    /* @return a new ToolWindow, parented to getRootWindow() 
    public static ToolWindow createToolWindow(String title) {
        return createToolWindow(title, null);
    }
    /** @return a new ToolWindow, containing the given component, parented to getRootWindow() 
    public static ToolWindow createToolWindow(String title, JComponent component) {
        return createToolWindow(title, component, false);
    }
    
    /* @return a new ToolWindow, containing the given component, parented to getRootWindow() 
    private static ToolWindow createToolWindow(String title, JComponent component, boolean palette) {
        //Window parent = getRootFrame();
        Window parent = getRootWindow();
        if (DEBUG.INIT) out("creating ToolWindow " + title + " with parent " + parent);

        final ToolWindow w;
        if (palette) {
            w = new ToolWindow(title, parent, false);
        } else {
            w = new ToolWindow(title, parent, true);
            if (component != null)
                w.addTool(component);
        }
        /*
          // ToolWindows not set yet...
        if (VueUtil.isMacPlatform() && useMacLAF && w instanceof JFrame)
            ((JFrame)w).setJMenuBar(new VUE.VueMenuBar());
        *
        return w;
    }

    /** @return a new ToolWindow styled as a ToolPalette 
    public static ToolWindow createToolPalette(String title) {
        return createToolWindow(title, null, true);
    }
    */

    /** call the given runnable after all pending AWT events are completed */
    public static void invokeAfterAWT(Runnable runnable) {
        java.awt.EventQueue.invokeLater(runnable);
    }

    public static boolean inFullScreen() {
        return FullScreen.inFullScreen();
    }
    public static boolean inNativeFullScreen() {
        return FullScreen.inNativeFullScreen();
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }
    
    public static void toggleFullScreen(boolean goNative) {
    	toggleFullScreen(goNative, false);
    }
    
    public static void toggleFullScreen(boolean goNative,boolean showFloatingToolbar)
    {
    	FullScreen.toggleFullScreen(goNative);
    	if (showFloatingToolbar)
            floatingZoomDock.setVisible(FullScreen.inFullScreen());                	
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
        
        map.addNode(NodeModeTool.createTextNode("jumping"));
        
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
        LWNode n2 = new LWNode("Program\nFiles", new MapResource("C:\\Program Files"));
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

//         LWSlide slide = LWSlide.create();
//         slide.setLocation(300,100);
//         map.addLWC(slide);

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
        System.out.println(o == null ? "null" : o.toString());
    }
}
