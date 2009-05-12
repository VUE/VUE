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
 * DataFinder - replacement for old Digital Repository Browser (DRBrowser.java),
 * currently use DataSourceHandler as a replacement for DataSourceViewer,
 * tho would probably make more sense to move that code here instead
 * of having DataSourceHandler at all, which is really just event handling
 * code, and doesn't need to be a UI component at all, as it simply
 * wraps a JList (previously, a DataSourceList).
 *
 * [ below comments are deprecated ]
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
 * @version $Revision: 1.3 $ / $Date: 2009-05-12 20:01:05 $ / $Author: mike $ 
 */
public class DataFinder extends WidgetStack
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataFinder.class);
    
    public static final Object SEARCH_EDITOR = "search_editor_layout_constraint";
    public static final Object SEARCH_RESULT = "search_result_layout_constraint";
    
    final JComponent searchPane = new Widget("Search") {
            private Component editor, result;
            {
                setOpaque(false);
            }
            
            @Override
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

                    // this method of setting this is a crazy hack for now, but
                    // it's perfect for allowing us to try different layouts
                    resultsPane.removeAll();
                    resultsPane.add(jc);
                    resultsPane.setHidden(false);
                    resultsPane.validate();

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
    
    final Widget sourcesPane = new Widget("Sources");
    final Widget browsePane = new Widget("Browse");
    final Widget resultsPane = new Widget("Search Results");

    private JLabel loadingLabel;

    private Class[] includedSources;
    private Class[] excludedSources;

    private DataSourceHandler DSH;
    
    public DataFinder(String name, Class[] included, Class[] excluded)
    {
        includedSources = included;
        excludedSources = excluded;
        //super(new BorderLayout());
        if (DEBUG.DR || DEBUG.INIT) out("DataFinder construct");
        setName(name);
        //Dimension startSize = new Dimension(300,160);
        //setPreferredSize(startSize);
        //setMinimumSize(startSize);

        //this.dockWindow = dockWindow;
        //this.dockWindow = resourceDock;
        //this.resourceDock = resourceDock;
       //this.searchDock = searchDock;
        //this.sourcesPane = this;

        //setOpaque(true);
        //setBackground(Color.white);

        buildWidgets();
        
        //if (delayedLoading) {
        if (true) {
            
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
            sourcesPane.add(loadingLabel);
            
        } else {
            loadDataSourceViewer();
        }

        buildUI();

        if (DEBUG.DR || DEBUG.INIT) out("Instantiated.");
    }

    private void buildUI()
    {
        resultsPane.setTitleHidden(true);
        resultsPane.setHidden(true);

        //WidgetStack stack = new WidgetStack(getName());

        Widget.setWantsScroller(this, true);

        
        // TODO: can any of these (beyond refresh), ever have an effect?
        Widget.setHelpAction(this,VueResources.getString("dockWindow.Resources.libraryPane.helpText"));;
        //Widget.setMiscAction(this, new MiscActionMouseListener(), "dockWindow.addButton");
        Widget.setHelpAction(browsePane,VueResources.getString("dockWindow.Resources.browsePane.helpText"));;
        Widget.setHelpAction(resultsPane,VueResources.getString("dockWindow.Resources.resultsPane.helpText"));;
        Widget.setHelpAction(searchPane,VueResources.getString("dockWindow.Resources.searchPane.helpText"));;

        Widget.setRefreshAction(browsePane, new MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    DSH.refreshBrowser();
                    //Log.error("UNIMPLEMENTED: REFRESH BROWSER");
                }
            });
        

        addPane(sourcesPane, 0f);
        addPane(searchPane, 0f);
        addPane(browsePane, 1f);
        addPane(resultsPane, 0f);

//         if (false) {
//             JLabel startLabel = new JLabel("Search Results", JLabel.CENTER);
//             startLabel.setPreferredSize(new Dimension(100, 100));
//             startLabel.setBorder(new MatteBorder(1,0,0,0, Color.darkGray));
//             resultsPane.add(startLabel);
//         }

        //this.dockWindow.setContent(stack);
    }
    
    private void buildWidgets()
    {
        Dimension startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
                                            100);        

        //-----------------------------------------------------------------------------
        // Search
        //-----------------------------------------------------------------------------
        
        searchPane.setBackground(Color.white);
        JLabel please = new JLabel(VueResources.getString("jlabel.searchableresource"), JLabel.CENTER);
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
    
    public void loadDataSourceViewer()
    {
        Log.debug("loading the DataSourceViewer...");
            
        try {
            // TODO: DataSourceHandler shouldn't really be an AWT component -- it just
            // wraps a data-sources list -- it's code is just about handling the user interactions
            
            //DataSourceHandler DSH = new DataSourceHandler(this, includedSources, excludedSources);
            DSH = new DataSourceHandler(this, includedSources, excludedSources);
            DSH.setName("dataSourceHandler");
            /*
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            */
//             if (loadingLabel != null)
//                 sourcesPane.remove(loadingLabel);
            sourcesPane.removeAll();

            sourcesPane.add(DSH);

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

    

//     public static void main(String args[])
//     {
//         VUE.init(args);
        
// //         Frame owner = new Frame("parentWindow");
// //         owner.setVisible(true);
// //         //owner.setFocusable(true);
// //         //owner.setFocusableWindowState(true);

//         Log.debug("loading disk cache...");
//         Images.loadDiskCache();
//         Log.debug("loading disk cache: done");

//         DockWindow.setManagedWindows(false);
        
//         DockWindow drDock = GUI.createDockWindow("Content");
//         //DockWindow drDock = new DockWindow("Content", owner);
		
//         DataFinder drBrowser = new DataFinder(true, drDock);

//         //drDock.setFocusableWindowState(true);
        
//         tufts.vue.ui.InspectorPane inspectorPane = new tufts.vue.ui.InspectorPane();
//         //ObjectInspector = GUI.createDockWindow("Info");
//         //ObjectInspector.setContent(inspectorPane.getWidgetStack());

        
//         DockWindow inspector = GUI.createDockWindow("Info", new tufts.vue.ui.InspectorPane());
//         //DockWindow inspector = new DockWindow("Info", owner, inspectorPane.getWidgetStack(), false);
//         inspector.setMenuName("Info / Preview");
//         VUE._setInfoDock(inspector);

//         int maxHeight = GUI.getMaximumWindowBounds().height;

//         int inspectorWidth = GUI.GScreenWidth / 2;
//         if (inspectorWidth > 800)
//             inspectorWidth = 800;

//         inspector.setSize(inspectorWidth, maxHeight);
//         inspector.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
//         inspector.setVisible(true);
//         inspector.setFocusableWindowState(true);
        
//         drDock.setSize(300, (int) (GUI.GScreenHeight * 0.75));
//         drDock.setUpperRightCorner(GUI.GScreenWidth - inspector.getWidth(), GUI.GInsets.top);
//         drDock.setVisible(true);
//         drDock.setFocusableWindowState(true);

//         drBrowser.loadDataSourceViewer();

//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             DataSourceViewer.cacheDataSourceViewers();
//         }});

//         DataSourceViewer.configureOSIDs();

//     }

       
    
}
