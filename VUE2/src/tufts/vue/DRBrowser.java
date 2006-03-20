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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import tufts.vue.gui.*;

/**
 * Digital Repository Browser
 *
 * @version $Revision: 1.39 $ / $Date: 2006-03-20 20:41:48 $ / $Author: sfraize $ 
 */
public class DRBrowser extends JPanel
{
    private static final boolean SingleDockWindowImpl = false;
    
    final JPanel librariesPanel;
    final Widget searchPane = new Widget("Search");
    final Widget browsePane = new Widget("Browse");
    final Widget savedResourcesPane = new Widget("Saved Resources");
    final Widget previewPane = new Widget("Preview");

    final DockWindow dockWindow;
    
    private JLabel loadingLabel;
    
    public DRBrowser(boolean delayedLoading, DockWindow dockWindow)
    {
        super(new BorderLayout());
        setName("Libraries");
        Dimension startSize = new Dimension(300,160);
        setPreferredSize(startSize);

        this.dockWindow = dockWindow;
        this.librariesPanel = this;

        buildWidgets();
        
        if (delayedLoading) {
            loadingLabel = new JLabel("Loading data sources...", SwingConstants.CENTER);
            loadingLabel.setMinimumSize(new Dimension(startSize.width/2, startSize.height/2));
            add(loadingLabel);
        } else {
            loadDataSourceViewer();
        }

        if (SingleDockWindowImpl)
            buildSingleDockWindow();
        else
            buildMultipleDockWindows();
    }

    private void buildSingleDockWindow() {
        WidgetStack stack = new WidgetStack();

        //searchPane.setExpanded(false);
        //browsePane.setExpanded(false);
        savedResourcesPane.setExpanded(false);

        stack.addPane(searchPane, 0f);
        stack.addPane(librariesPanel, 0f);
        stack.addPane(browsePane, 1f);
        stack.addPane(savedResourcesPane, 0f);

        this.dockWindow.setContent(stack);
    }

    private void buildMultipleDockWindows()
    {
        // make sure the loading label will be visible
        this.dockWindow.setContent(librariesPanel);
                              
        // now create the stack of DockWindows
        DockWindow drBrowserDock = this.dockWindow;
        
        DockWindow searchDock = GUI.createDockWindow(searchPane);
        DockWindow browseDock = GUI.createDockWindow(browsePane);
        DockWindow savedResourcesDock = GUI.createDockWindow(savedResourcesPane);
        DockWindow previewDock = GUI.createDockWindow(previewPane);

        
        drBrowserDock.setStackOwner(true);
        drBrowserDock.addChild(searchDock);
        drBrowserDock.addChild(browseDock);
        //drBrowserDock.addChild(previewDock);
        previewDock.setLocation(300,300);
        drBrowserDock.addChild(savedResourcesDock);
        
        searchDock.setContent(searchPane);
        searchDock.setRolledUp(true);
        
        browseDock.setContent(browsePane);
        browseDock.setRolledUp(true);
        
        savedResourcesDock.setContent(savedResourcesPane);
        savedResourcesDock.setRolledUp(true);
        
        previewDock.setContent(previewPane);
        previewDock.setRolledUp(true);

    }
    
    private void buildWidgets()
    {
        Dimension startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
                                            100);        

        //-----------------------------------------------------------------------------
        // Search
        //-----------------------------------------------------------------------------
        
        searchPane.setBackground(Color.white);
        searchPane.setLayout(new BorderLayout());
        searchPane.setPreferredSize(startSize);
        searchPane.add(new JLabel("Please Select A Library"), BorderLayout.CENTER);
		
        //-----------------------------------------------------------------------------
        // Local File Data Source
        //-----------------------------------------------------------------------------
        
        try {
            LocalFileDataSource localFileDataSource = new LocalFileDataSource("My Computer","");
            browsePane.setBackground(Color.white);
            browsePane.setLayout(new BorderLayout());
            startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
                                      300);
            if (true) startSize.height = GUI.GScreenHeight / 5;
            //browsePanel.setPreferredSize(startSize);
            JComponent comp = localFileDataSource.getResourceViewer();
            comp.setVisible(true);
            browsePane.add(comp);
        } catch (Exception ex) {
            if (DEBUG.DR) System.out.println("Problem loading local file library");
        }
		
        //-----------------------------------------------------------------------------
        // Saved Resources
        //-----------------------------------------------------------------------------
		
        savedResourcesPane.setBackground(Color.white);
        savedResourcesPane.setPreferredSize(startSize);
        savedResourcesPane.add(new JLabel("saved resources"));
		
        //-----------------------------------------------------------------------------
        // Preview
        //-----------------------------------------------------------------------------
		
        previewPane.setBackground(Color.white);
        previewPane.setPreferredSize(startSize);
        previewPane.add(new JLabel("no preview"));
        
    }
    
    /*
    public DRBrowser() {
        this(false, null);
    }
    */
    
    /*
    public void setDockWindow(tufts.vue.gui.DockWindow dWindow) {
        dockWindow = dWindow;
    }
    */
	
    public void loadDataSourceViewer()
    {
        if (DEBUG.DR || DEBUG.Enabled) System.out.println("DRBrowser: loading the DataSourceViewer...");
            
        try {
            DataSourceViewer dsv = new DataSourceViewer(this);
            dsv.setName("Data Source Viewer");
            /*
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            */
            if (loadingLabel != null)
                librariesPanel.remove(loadingLabel);
            //setMinimumSize(null); some data-sources smaller: don't allow shrinkage
            //librariesPanel.setPreferredSize(null);
            librariesPanel.add(dsv);

            if (false) {
                System.out.println("dsv == " + dsv);
                System.out.println("dsv sz " + dsv.getSize());
                System.out.println("dsv ps " + dsv.getPreferredSize());
                System.out.println("DRB.SZ " + getSize());
                System.out.println("DRB.PS " + getPreferredSize());
                validate();
                System.out.println("validate");
                System.out.println("dsv sz " + dsv.getSize());
                System.out.println("dsv ps " + dsv.getPreferredSize());
                System.out.println("DRB.SZ " + getSize());
                System.out.println("DRB.PS " + getPreferredSize());
                
                //setMinimumSize(dsv.getPreferredSize());
            }

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
            VUE.Log.error(e);
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }
        
        if (DEBUG.DR || DEBUG.Enabled) System.out.println("DRBrowser: done loading DataSourceViewer.");
    }

    public static void main(String args[])
    {
        VUE.init(args);
        
        DEBUG.DR = true;
        DockWindow dw = GUI.createDockWindow("Resources");
        DRBrowser drb = new DRBrowser(true, dw);

        dw.setVisible(true);

        drb.loadDataSourceViewer();

        if (args.length > 1)
            ResourcePanel.displayTestResourcePanel(null);
        
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
