/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Digital Repository Browser
 *
 * This single panel abstracts the collective handling of a list of
 * selectable data sources, a search pane to be automatically
 * displayed and updated when searchable data sources are selected,
 * and a browse pane to be automatically populated and displayed when
 * browseable data sources are selected.
 *
 * The impl provideds for either splitting these up as separate
 * DockWindow's, or having them be combined into one panel as separate
 * Widget's.  This is probably overkill -- at this point -- we're
 * unlikely to split these back up into separate windows. That version
 * of the code is long untestest and probably out of date, and most
 * everything is now actually handled by the DataSourceViewer,
 * including the production and display of search-result panes when a
 * search is run.
 *
 * This code, along with DataSourceViewer, DataSourceList, and
 * DataSourceListCellRenderer, are due for collective refactoring.  It
 * would be best to do away with the JList / renderer impl, which adds
 * much complexity, and doesn't get us much: we're unlikely to ever
 * have so many data sources that the GUI performance gain of not
 * having full-time swing components present for each data source line
 * is worth it, and without having to go through a ListCellRenderer,
 * we'd have much more flexibility in layout out, drawing and
 * repainting the data sources (e.g., we could add spinners to loading
 * data sources -- that's wouldn't possible now without adding a
 * special timer thread to repaint the entire least each time the
 * spinner wanted to draw the next image.)
 *
 * Also, it would be very handy to have a single interface or class
 * that abstracts BOTH types of data-sources: OSID's (edu.tufts.vue.dsm.DataSource),
 * and browseable VUE ("old-style") data sources (tufts.vue.DataSource).
 * We'd probably need a delegating impl tho to handle that.
 *
 *
 * @version $Revision: 1.74 $ / $Date: 2009-02-22 19:23:15 $ / $Author: sfraize $ 
 */
public class DRBrowser extends JPanel
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DRBrowser.class);
    
    public static final Object SEARCH_EDITOR = "search_editor_layout_constraint";
    public static final Object SEARCH_RESULT = "search_result_layout_constraint";
    
    private static final boolean SingleDockWindowImpl = true; // these two must be exclusive
    private static final boolean DoubleDockWindowImpl = false;
    
    final JComponent searchPane = new Widget("Search") {
            private Component editor, result;
            {
                setOpaque(false);
                if (DoubleDockWindowImpl)
                    setWantsScroller(true);
            }
            
            protected void addImpl(Component c, Object constraints, int idx) {
                if (DEBUG.DR) out("SEARCH-WIDGET addImpl: " + GUI.name(c) + " " + constraints + " idx=" + idx);
                JComponent jc = null;
                if (c instanceof JComponent)
                    jc = (JComponent) c;
                if (constraints == SEARCH_EDITOR) {
                    if (editor != null)
                        remove(editor);
                    editor = c;
                    constraints = BorderLayout.NORTH;
                    if (jc != null)
                        jc.setBorder(GUI.WidgetInsetBorder);
                } else if (constraints == SEARCH_RESULT) {

                    if (SingleDockWindowImpl) {
                        // this method of setting this is a crazy hack for now, but
                        // it's perfect for allowing us to try different layouts
                        resultsPane.removeAll();
                        resultsPane.add(jc);
                        resultsPane.setHidden(false);
                        resultsPane.validate();
                        return;
                    }
                    
                    if (result != null)
                        remove(result);
                    result = c;
                    constraints = BorderLayout.CENTER;
                } else {
                    tufts.Util.printStackTrace("illegal search pane constraints: " + constraints);
                }
                
                //constraints = bc;
                super.addImpl(c, constraints, idx);
                revalidate();
            }

            //public void doLayout() { GUI.dumpSizes(this, "doLayout"); super.doLayout(); }
            //public void setPreferredSize(Dimension d) { Util.printStackTrace("setPreferredSize " + d);}
            //public void setLayout(LayoutManager m) {Util.printStackTrace("setLayout; " + m);super.setLayout(m);}
            

        };
    
    final JPanel librariesPanel;
    final Widget browsePane = new Widget("Browse");
    final Widget resultsPane = new Widget("Search Results");

    /*
    final Widget browsePane = new Widget("Browse") {
            public void setHidden(boolean hidden) {
                if (!hidden) Widget.setHiddenImpl(resultsPane, true);
                super.setHidden(hidden);
                if (hidden) Widget.setHiddenImpl(resultsPane, false);
            }
        };
    
    final Widget resultsPane = new Widget("Search Results") {
            public void setHidden(boolean hidden) {
                if (!hidden)
                    Widget.setHiddenImpl(browsePane, true);
                super.setHidden(hidden);
            }
        };
            
    */
            
            /*
            protected void addImpl(Component c, Object lc, int idx) {
                if (DEBUG.DR) out("RESULTS-WIDGET addImpl " + GUI.name(c));
                if (c instanceof WidgetStack)
                    ((JComponent)c).setBorder(new MatteBorder(1,0,0,0, Color.darkGray));
                super.addImpl(c, lc, idx);
            }
            */
            
            /*
            final JScrollPane scroller = new JScrollPane(null,
                                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            { scroller.setViewportBorder(new EmptyBorder(4,3,0,0)); }
            protected void addImpl(Component c, Object lc, int idx) {
                if (DEBUG.DR) out("RESULTS-WIDGET addImpl " + GUI.name(c));
                if (c instanceof JLabel) {
                    super.addImpl(c, lc, idx);
                    revalidate();
                } else {
                    scroller.setViewportView(c);
                    super.addImpl(scroller, lc, idx);
                    scroller.revalidate();
                    scroller.repaint();
                }
            }
            */

    
    //final Widget previewPane = new Widget("Preview");
    //final Widget savedResourcesPane = new Widget("Saved Resources");

    final DockWindow dockWindow;
    final DockWindow searchDock;
    final DockWindow resourceDock;
    
    private JLabel loadingLabel;

    private DataSourceViewer DSV;
    
    
    public DRBrowser(boolean delayedLoading, DockWindow resourceDock, DockWindow searchDock)
    {
        super(new BorderLayout());
        if (DEBUG.DR || DEBUG.INIT) out("Creating DRBrowser");
        setName("Resources");
        //Dimension startSize = new Dimension(300,160);
        //setPreferredSize(startSize);
        //setMinimumSize(startSize);

        //this.dockWindow = dockWindow;
        this.dockWindow = resourceDock;
        this.resourceDock = resourceDock;
        this.searchDock = searchDock;
        this.librariesPanel = this;

        //setOpaque(true);
        //setBackground(Color.white);

        buildWidgets();
        
        if (delayedLoading) {
            
//             if (Util.isMacLeopard()) {
//                 JProgressBar bar = new JProgressBar();
//                 bar.setAlignmentX(SwingConstants.CENTER); // no effect
//                 bar.setIndeterminate(true);
//                 bar.putClientProperty("JProgressBar.style", "circular");
//                 bar.setString("Loading...");// no effect
//                 bar.setStringPainted(true); // no effect
//                 add(bar);
//             }

            loadingLabel = new JLabel(VueResources.getString("dockWindow.Resources.loading.label"), SwingConstants.CENTER);
            loadingLabel.setMinimumSize(new Dimension(150, 80));
            loadingLabel.setBorder(new EmptyBorder(8,0,8,0));
            GUI.apply(GUI.StatusFace, loadingLabel);
            add(loadingLabel);
            
        } else {
            loadDataSourceViewer();
        }

        if (DoubleDockWindowImpl)
            buildDoubleDockWindow();
        else if (SingleDockWindowImpl)
            buildSingleDockWindow();
        else
            buildMultipleDockWindows();

        if (DEBUG.DR || DEBUG.INIT) out("Instantiated.");
    }

    private void buildSingleDockWindow()
    {
        resultsPane.setTitleHidden(true);
        resultsPane.setHidden(true);

        WidgetStack stack = new WidgetStack(getName());

        Widget.setWantsScroller(stack, true);

        stack.addPane(librariesPanel, 0f);
        stack.addPane(searchPane, 0f);
        stack.addPane(browsePane, 1f);
        stack.addPane(resultsPane, 0f);

        if (false) {
            JLabel startLabel = new JLabel("Search Results", JLabel.CENTER);
            startLabel.setPreferredSize(new Dimension(100, 100));
            startLabel.setBorder(new MatteBorder(1,0,0,0, Color.darkGray));
            resultsPane.add(startLabel);
        }

        this.dockWindow.setContent(stack);
    }

    private void buildDoubleDockWindow() {
        WidgetStack stack = new WidgetStack();

        stack.addPane(librariesPanel, 0f);
        stack.addPane(browsePane, 1f); 
        resourceDock.setContent(stack);

        //WidgetStack searchStack = new WidgetStack();
        //searchStack.addPane(searchPane, 0f);
        searchDock.setContent(searchPane);

    }
    
    private void buildMultipleDockWindows()
    {
        // make sure the loading label will be visible
        this.dockWindow.setContent(librariesPanel);
                              
        // now create the stack of DockWindows
        DockWindow drBrowserDock = this.dockWindow;
        
        DockWindow searchDock = GUI.createDockWindow(searchPane);
        DockWindow browseDock = GUI.createDockWindow(browsePane); 
        //DockWindow previewDock = GUI.createDockWindow(previewPane);
        //DockWindow savedResourcesDock = GUI.createDockWindow(savedResourcesPane);
        
        drBrowserDock.setStackOwner(true);
        drBrowserDock.addChild(searchDock);
        drBrowserDock.addChild(browseDock); 
        //drBrowserDock.addChild(previewDock);
        //previewDock.setLocation(300,300);
        //drBrowserDock.addChild(savedResourcesDock);
        
        searchDock.setContent(searchPane);
        searchDock.setRolledUp(true);
        
        browseDock.setContent(browsePane);
        browseDock.setRolledUp(true);
        //browsePane.setHidden(true);
        //browseDock.setVisible(false); // won't work till after displayed
        
        //savedResourcesDock.setContent(savedResourcesPane);
        //savedResourcesDock.setRolledUp(true);
        
        //previewDock.setContent(previewPane);
        //previewDock.setRolledUp(true);

    }
    
    private void buildWidgets()
    {
        Dimension startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
                                            100);        

        //-----------------------------------------------------------------------------
        // Search
        //-----------------------------------------------------------------------------
        
        searchPane.setBackground(Color.white);
        JLabel please = new JLabel("Please select a searchable resource", JLabel.CENTER);
        GUI.apply(GUI.StatusFace, please);
        searchPane.add(please, SEARCH_EDITOR);
		
        //-----------------------------------------------------------------------------
        // Local File Data Source and Favorites
        //-----------------------------------------------------------------------------
        
        try {
            browsePane.setBackground(Color.white);
            browsePane.setExpanded(false);
            browsePane.setLayout(new BorderLayout());
            //startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400, 300);
            //startSize.height = GUI.GScreenHeight / 5;
            //browsePanel.setPreferredSize(startSize);
            if (false) {
                // SMF 2008-04-17: This is redundant, and can dramatically slow down startup,
                // and make diagnosing problems much harder.  Appears vestigal.
                LocalFileDataSource localFileDataSource = new LocalFileDataSource("My Computer","");
                JComponent comp = localFileDataSource.getResourceViewer();
                comp.setVisible(true);
                browsePane.add(comp);
            }
             
        } catch (Exception ex) {
            if (DEBUG.DR) out("Problem loading local file library");
        }
		
        //-----------------------------------------------------------------------------
        // Saved Resources
        //-----------------------------------------------------------------------------
		
        //savedResourcesPane.setBackground(Color.white);
        //savedResourcesPane.setPreferredSize(startSize);
        //savedResourcesPane.add(new JLabel("saved resources"));
	
        if (DEBUG.DR) out("build widgets complete");	
    }
    
    public DataSourceViewer getDataSourceViewer()
    {
    	return DSV;
    }
    
    public void loadDataSourceViewer()
    {
        Log.debug("loading the DataSourceViewer...");
            
        try {
            DSV = new DataSourceViewer(this);
            DSV.setName("Data Source Viewer");
            /*
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            */
            if (loadingLabel != null)
                librariesPanel.remove(loadingLabel);

            librariesPanel.add(DSV);

            revalidate();
            // must do this to get re-laid out: apparently, the hierarchy
            // events from the add don't automatically do this!
            
            // TODO; As the DSV top-level is normally a scroll-pane, it's
            // preferred/min size is useless (always will go down
            // to one line), so either we'll need a manual size
            // set anytime there's a scroll pane, or maybe the WidgetStack
            // can detect the scroll pane, and look at the pref size of it's
            // contents.
            //setPreferredSize(dsv.getPreferredSize());
            
        } catch (Throwable e) {
            Log.error(e);
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }
        
        if (DEBUG.DR || DEBUG.Enabled) out("done loading DataSourceViewer");
    }

    private static void out(String s) {
        //System.out.println("DRBrowser: " + s);
        Log.info(s);
    }

    

    public static void main(String args[])
    {
        VUE.init(args);
        
//         Frame owner = new Frame("parentWindow");
//         owner.setVisible(true);
//         //owner.setFocusable(true);
//         //owner.setFocusableWindowState(true);

        Log.debug("loading disk cache...");
        Images.loadDiskCache();
        Log.debug("loading disk cache: done");

        DockWindow.setManagedWindows(false);
        
        DockWindow drDock = GUI.createDockWindow("Content");
        //DockWindow drDock = new DockWindow("Content", owner);
		
        DRBrowser drBrowser = new DRBrowser(true, drDock, null);

        //drDock.setFocusableWindowState(true);
        
        tufts.vue.ui.InspectorPane inspectorPane = new tufts.vue.ui.InspectorPane();
        //ObjectInspector = GUI.createDockWindow("Info");
        //ObjectInspector.setContent(inspectorPane.getWidgetStack());

        
        DockWindow inspector = GUI.createDockWindow("Info", new tufts.vue.ui.InspectorPane());
        //DockWindow inspector = new DockWindow("Info", owner, inspectorPane.getWidgetStack(), false);
        inspector.setMenuName("Info / Preview");
        VUE._setInfoDock(inspector);

        int maxHeight = GUI.getMaximumWindowBounds().height;

        int inspectorWidth = GUI.GScreenWidth / 2;
        if (inspectorWidth > 800)
            inspectorWidth = 800;

        inspector.setSize(inspectorWidth, maxHeight);
        inspector.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
        inspector.setVisible(true);
        inspector.setFocusableWindowState(true);
        
        drDock.setSize(300, (int) (GUI.GScreenHeight * 0.75));
        drDock.setUpperRightCorner(GUI.GScreenWidth - inspector.getWidth(), GUI.GInsets.top);
        drDock.setVisible(true);
        drDock.setFocusableWindowState(true);

        drBrowser.loadDataSourceViewer();

//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             DataSourceViewer.cacheDataSourceViewers();
//         }});

        edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance()
            .startRepositoryConfiguration(null);
                //DataSourceViewer.configureOSIDs();

        //-------------------------------------------------------
        // TODO: mods to the data-sources might wipe out the saved
        // datasources, as the internal code currently handles
        // auto-saving on change (need to fix that), but may not have
        // been fully populated by our test load here?
        // -------------------------------------------------------
            
        /*
        DockWindow dw = GUI.createDockWindow("Test Resources");
        DRBrowser drb = new DRBrowser(true, dw, null);
        
        if (false) {
            //DRBrowser drb = new DRBrowser(true, dw, GUI.createDockWindow("Search")); 
            dw.setSize(300,400);
            dw.setVisible(true);
        } else {
            drb.setPreferredSize(new Dimension(200,400));
            tufts.Util.displayComponent(drb);
        }
        
        drb.loadDataSourceViewer();

        GUI.makeVisibleOnScreen(drb);

        */
        /*
        drb.loadDataSourceViewer();
        drb.setSize(200,200);
        tufts.Util.displayComponent(drb);
        if (args.length > 1)
            tufts.vue.ui.InspectorPane.displayTestPane(null);
        */
        
        /*
        tufts.Util.displayComponent(drb);
        try {
            java.util.prefs.Preferences p = tufts.oki.dr.fedora.FedoraUtils.getDefaultPreferences(null);
            p.exportSubtree(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        
    }

       
    
}
