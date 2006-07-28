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

import tufts.vue.gui.GUI;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.Widget;
import tufts.vue.ui.MetaDataPane;
import tufts.vue.ui.ResourceList;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;
import java.util.*;
import java.net.URL;

// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;

import tufts.vue.gui.*;

public class DataSourceViewer extends JPanel
    implements KeyListener, edu.tufts.vue.fsm.event.SearchListener
{
    private static final boolean UseFederatedSearchManager = false;
    
    private static DRBrowser DRB;
    private static Object activeDataSource;
    String breakTag = "";
    
    public final static int ADD_MODE = 0;
    public final static int EDIT_MODE = 1;
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT = VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    public static final org.osid.shared.Type favoritesRepositoryType = new edu.tufts.vue.util.Type("edu.tufts","favorites","Favorites");
    JPopupMenu popup;
    
    AddLibraryDialog addLibraryDialog;
    UpdateLibraryDialog updateLibraryDialog;
    EditLibraryDialog editLibraryDialog;
    RemoveLibraryDialog removeLibraryDialog;
    GetLibraryInfoDialog getLibraryInfoDialog;
    
    AbstractAction checkForUpdatesAction;
    AbstractAction addLibraryAction;
    AbstractAction editLibraryAction;
    AbstractAction removeLibraryAction;
    AbstractAction getLibraryInfoAction;
    MouseListener refreshDSMouseListener;
    
    JButton optionButton = new VueButton("add");
    JButton searchButton = new JButton("Search");
    
    public static Vector  allDataSources = new Vector();
    public static DataSourceList dataSourceList;
    
    DockWindow resultSetDockWindow;
    DockWindow editInfoDockWindow;
    javax.swing.JScrollPane resultSetTreeJSP;
    JPanel previewPanel = null;
    
    static edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
    //static edu.tufts.vue.dsm.DataSource dataSources[];
    static edu.tufts.vue.fsm.FederatedSearchManager federatedSearchManager;
    static edu.tufts.vue.fsm.QueryEditor queryEditor;
    private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager;
    
    private java.awt.Dimension resultSetPanelDimensions = new java.awt.Dimension(400,200);
    private javax.swing.JPanel resultSetPanel = new javax.swing.JPanel();
    
    org.osid.shared.Type searchType = new edu.tufts.vue.util.Type("mit.edu","search","keyword");
    org.osid.shared.Type thumbnailType = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
    ImageIcon noImageIcon;
    
    private org.osid.OsidContext context = new org.osid.OsidContext();
    org.osid.registry.Provider checked[];

    private final ThreadGroup mSearchThreadGroup;
    
    public DataSourceViewer(DRBrowser drBrowser) {
        GUI.activateWaitCursor();
        
        setLayout(new BorderLayout());
        this.DRB = drBrowser;
        dataSourceList = new DataSourceList(this);
        Widget.setExpanded(DRB.browsePane, false);
        edu.tufts.vue.dsm.DataSource dataSources[] = null;
        try {
            // load new data sources
            dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            VUE.Log.info("DataSourceViewer; loading Installed data sources via Data Source Manager");
            edu.tufts.vue.dsm.impl.VueDataSourceManager.load();
            dataSources = dataSourceManager.getDataSources();
            VUE.Log.info("DataSourceViewer; finished loading data sources.");
            for (int i=0; i < dataSources.length; i++) {
                VUE.Log.info("DataSourceViewer; adding data source to data source list UI: " + dataSources[i]);
                dataSourceList.addOrdered(dataSources[i]);
            }
        } catch (Throwable t) {
            VueUtil.alert("Error loading data source","Error");
        }
        try {
            // load old-style data sources
            VUE.Log.info("DataSourceViewer; Loading old style data sources...");
            loadDataSources();
            VUE.Log.info("DataSourceViewer; Loaded old style data sources.");
        } catch (Throwable t) {
            VueUtil.alert("Error loading old data source","Error");
        }
        federatedSearchManager = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
        sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
        queryEditor = federatedSearchManager.getQueryEditorForType(searchType);
        queryEditor.addSearchListener(this);
        
        mSearchThreadGroup = new ThreadGroup("VUE-SearchParent");
        
        // select the first new data source, if any
        if ((dataSources != null) && (dataSources.length > 0)) {
            setActiveDataSource(dataSources[0]);
        }
        DRB.searchPane.removeAll();
        DRB.searchPane.add((JPanel) queryEditor, DRBrowser.SEARCH_EDITOR);
        DRB.searchPane.revalidate();
        DRB.searchPane.repaint();
        // WORKING: stop using this preview panel?
        //this.previewPanel = previewDockWindow.getWidgetPanel();
        //resultSetDockWindow = DRB.searchDock;
        setPopup();
        addListeners();
        
        if (false) {
            JScrollPane dataJSP = new JScrollPane(dataSourceList);
            dataJSP.setMinimumSize(new Dimension(100,100));
            add(dataJSP);
        } else {
            add(dataSourceList);
        }
        
        Widget.setHelpAction(DRB.librariesPanel,VueResources.getString("dockWindow.Content.libraryPane.helpText"));;
        Widget.setHelpAction(DRB.browsePane,VueResources.getString("dockWindow.Content.browsePane.helpText"));;
        Widget.setHelpAction(DRB.resultsPane,VueResources.getString("dockWindow.Content.resultsPane.helpText"));;
        Widget.setHelpAction(DRB.searchPane,VueResources.getString("dockWindow.Content.searchPane.helpText"));;
        GUI.clearWaitCursor();
    }
    
    private void addListeners() {
        dataSourceList.addKeyListener(this);
        // WORKING: commented out
        //librariesDockWindow.setVisible(true); // try to make menu appear
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Object o = ((JList)e.getSource()).getSelectedValue();
                if (o !=null) {
                    // for the moment, we are doing double work to keep old data sources
                    if (o instanceof tufts.vue.DataSource) {
                        DataSource ds = (DataSource)o;
                        DataSourceViewer.this.setActiveDataSource(ds);
                        refreshEditInfo(ds);
                    } else if (o instanceof edu.tufts.vue.dsm.DataSource) {
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                        DataSourceViewer.this.setActiveDataSource(ds);
                        refreshEditInfo(ds);
                    } else {
                        int index = ((JList)e.getSource()).getSelectedIndex();
                        o = dataSourceList.getContents().getElementAt(index-1);
                        if (o instanceof tufts.vue.DataSource) {
                            DataSource ds = (DataSource)o;
                            DataSourceViewer.this.setActiveDataSource(ds);
                            refreshEditInfo(ds);
                        } else if (o instanceof edu.tufts.vue.dsm.DataSource) {
                            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                            DataSourceViewer.this.setActiveDataSource(ds);
                            refreshEditInfo(ds);
                        }
                    }
                }
                refreshMenuActions();
            }}
        );
        
        dataSourceList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (activeDataSource instanceof DataSource) {
                        displayEditOrInfo((DataSource)activeDataSource);
                    } else {
                        displayEditOrInfo((edu.tufts.vue.dsm.DataSource)activeDataSource);
                    }
                } else {
                    Point pt = e.getPoint();
                    if ( (activeDataSource instanceof edu.tufts.vue.dsm.DataSource) && (pt.x <= 40) ) {
                        int index = dataSourceList.locationToIndex(pt);
                        
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)
                        dataSourceList.getModel().getElementAt(index);
                        boolean included = !ds.isIncludedInSearch();
                        if (DEBUG.DR) out("DataSource " + ds + " [" + ds.getProviderDisplayName() + "] inclusion: " + included);
                        ds.setIncludedInSearch(included);
                        dataSourceList.repaint();
                        queryEditor.refresh();
                        
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            try {
                                synchronized (dataSourceManager) {
                                    if (DEBUG.DR) out("DataSourceManager saving...");
                                    dataSourceManager.save();
                                    if (DEBUG.DR) out("DataSourceManager saved.");
                                }
                            } catch (Throwable t) {
                                tufts.Util.printStackTrace(t);
                            }
                        }});
                    }
                    //if(e.getButton() == e.BUTTON3)
                    //popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
    }
    public static Object getActiveDataSource() {
        return activeDataSource;
    }
    
    public void setActiveDataSource(DataSource ds){
        if (DEBUG.DR) out("Set active data source: " + ds);
        this.activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.searchPane, false);
        Widget.setExpanded(DRB.browsePane, true);
        Widget.setTitle(DRB.browsePane, "Browse: " + ds.getDisplayName());
        DRB.browsePane.removeAll();
        DRB.browsePane.add(ds.getResourceViewer());
        
        if (ds instanceof LocalFileDataSource)
        {
        	Widget.setRefreshAction(DRB.browsePane,(MouseListener)refreshDSMouseListener);
        }
        else
        {
        	Widget.setRefreshAction(DRB.browsePane,null);
        }
        
        DRB.browsePane.revalidate();
        DRB.browsePane.repaint();
    }
    
    public void setActiveDataSource(edu.tufts.vue.dsm.DataSource ds) {
        this.activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.browsePane, false);
        Widget.setExpanded(DRB.searchPane, true);
        queryEditor.refresh();
        
        if (ds instanceof LocalFileDataSource)
        {
        	Widget.setRefreshAction(DRB.browsePane,(MouseListener)refreshDSMouseListener);
        }
        else
        {
        	Widget.setRefreshAction(DRB.browsePane,null);
        }
    }
    
    public static void refreshDataSourcePanel(edu.tufts.vue.dsm.DataSource ds) {
        queryEditor.refresh();
        //TODO: actually replace the whole editor if need be
    }
    
    public void  setPopup() {
        popup = new JPopupMenu();
        
        checkForUpdatesAction = new AbstractAction("Update Resources") {
            public void actionPerformed(ActionEvent e) {
                try {
                    edu.tufts.vue.dsm.OsidFactory factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
                    org.osid.provider.ProviderIterator providerIterator = factory.getProvidersNeedingUpdate();
                    if (providerIterator.hasNextProvider()) {
                        if (updateLibraryDialog == null) {
                            updateLibraryDialog = new UpdateLibraryDialog(dataSourceList);
                        } else {
                            updateLibraryDialog.refresh();
                            updateLibraryDialog.setVisible(true);
                        }
                    } else javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
                            "There are no resource updates available at this time",
                            "Update Resources",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                } catch (Throwable t) {
                    VueUtil.alert(t.getMessage(),"Error");
                }
            }
        };
        addLibraryAction = new AbstractAction("Add Resources") {
            public void actionPerformed(ActionEvent e) {
                try {
                    // there are always resources that can be added, e.g. a local file system
                    if (addLibraryDialog == null) {
                        addLibraryDialog = new AddLibraryDialog(dataSourceList);
                    } else {
                        addLibraryDialog.refresh();
                        addLibraryDialog.setVisible(true);
                    }
                    
                    // reflect addition, if any, in UI
                    DataSource ds = addLibraryDialog.getOldDataSource();
                    if (ds != null) {
                        setActiveDataSource(ds);
                    } else {
                        edu.tufts.vue.dsm.DataSource ds1 = addLibraryDialog.getNewDataSource();
                        if (ds1 != null) {
                            setActiveDataSource(ds1);
                        }
                    }
                } catch (Throwable t) {
                    VueUtil.alert(t.getMessage(),"Error");
                }
            }
        };
        
        editLibraryAction = new AbstractAction("About this Resource") {
            public void actionPerformed(ActionEvent e) {
                Object o = dataSourceList.getSelectedValue();
                if (o != null) {
                    // for the moment, we are doing double work to keep old data sources
                    if (o instanceof edu.tufts.vue.dsm.DataSource) {
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                        displayEditOrInfo(ds);
                    } else {
                        displayEditOrInfo((DataSource)o);
                    }
                }
            }
        };
        
        refreshDSMouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            	refreshDataSourceList();
                
            }
        };
        removeLibraryAction = new AbstractAction("Delete Resource") {
            public void actionPerformed(ActionEvent e) {
                Object o = dataSourceList.getSelectedValue();
                if (o != null) {
                    // for the moment, we are doing double work to keep old data sources
                    if (o instanceof edu.tufts.vue.dsm.DataSource) {
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                        if (javax.swing.JOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                "Do you really want to delete " + ds.getRepositoryDisplayName(),
                                "Delete Resource",
                                javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                            dataSourceManager.remove(ds.getId());
							GUI.invokeAfterAWT(new Runnable() { public void run() {
								try {
									synchronized (dataSourceManager) {
										if (DEBUG.DR) out("DataSourceManager saving...");
										dataSourceManager.save();
										if (DEBUG.DR) out("DataSourceManager saved.");
									}
								} catch (Throwable t) {
									tufts.Util.printStackTrace(t);
								}
							}});
                            dataSourceList.getContents().removeElement(ds);
                            saveDataSourceViewer();
                        }
                    } else if( o instanceof tufts.vue.DataSource) {
                        tufts.vue.DataSource ds = (tufts.vue.DataSource) o;
                        if (javax.swing.JOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                "Do you really want to delete " + ds.getDisplayName(),
                                "Delete Resource",
                                javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                            dataSourceList.getContents().removeElement(ds);
                            saveDataSourceViewer();
                        }
                    }
                }
                DataSourceViewer.this.popup.setVisible(false);
                
            }
        };
        
        refreshMenuActions();
    }
    
    public void refreshDataSourceList(){
    	
    	GUI.activateWaitCursor();
    
    	DataSource ds = null;
    	DataSource oldDataSource = null;
    	try
    	{
         oldDataSource = (DataSource) activeDataSource;
    	}
    	catch(ClassCastException cce)
    	{
    	 return;	
    	}
    	finally
    	{
    		GUI.clearWaitCursor();
    	}
    	
    	if (oldDataSource instanceof LocalFileDataSource)
    	{
    		String address = ((LocalFileDataSource)oldDataSource).getAddress();
    		String displayName = ((LocalFileDataSource)oldDataSource).getDisplayName();
    		try
    		{
    			ds = new LocalFileDataSource(displayName,address);
    		}
    		catch(DataSourceException dse)
    		{
    			System.out.println("Error refreshing datasource");    			
    			ds = oldDataSource;
    		}
            
    	}
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.searchPane, false);
        Widget.setExpanded(DRB.browsePane, true);
        Widget.setTitle(DRB.browsePane, "Browse: " + ds.getDisplayName());
       
        DRB.browsePane.removeAll();
        DRB.browsePane.add(ds.getResourceViewer());
        DRB.browsePane.revalidate();
        DRB.browsePane.repaint();
               
        GUI.clearWaitCursor();        
    }
    
    private void refreshMenuActions() {
        Object o = dataSourceList.getSelectedValue();
        if (o != null) {
            // for the moment, we are doing double work to keep old data sources
            if (o instanceof edu.tufts.vue.dsm.DataSource) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                removeLibraryAction.setEnabled(true);  
            } else if (o instanceof RemoteFileDataSource) {
                // FTP
                removeLibraryAction.setEnabled(true); 
            } else {
                // My Computer and My Saved Content
                removeLibraryAction.setEnabled(true);
            }
            editLibraryAction.setEnabled(true);
        } else {
            removeLibraryAction.setEnabled(false);
            editLibraryAction.setEnabled(false);
        }
        
        //TODO : This is temporary but they decided to disable update for 1.5 -mikek
        checkForUpdatesAction.setEnabled(false);
        Widget.setMenuActions(DRB.librariesPanel,
                new Action[] {
        			addLibraryAction,
        			checkForUpdatesAction,
        			null,
        			editLibraryAction,                    
                    removeLibraryAction
        });
                
    }
    
    private boolean checkValidUser(String userName,String password,int type) {
        if(type == 3) {
            try {
                TuftsDLAuthZ tuftsDL =  new TuftsDLAuthZ();
                osid.shared.Agent user = tuftsDL.authorizeUser(userName,password);
                if(user == null)
                    return false;
                if(tuftsDL.isAuthorized(user, TuftsDLAuthZ.AUTH_VIEW))
                    return true;
                else
                    return false;
            } catch(Exception ex) {
                VueUtil.alert(null,"DataSourceViewer.checkValidUser - Exception :" +ex, "Validation Error");
                ex.printStackTrace();
                return false;
            }
        } else
            return true;
    }
    
    public void loadDataSources() {
        boolean init = true;
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        if(DEBUG.DR) System.out.println("Data source file: " + f.getAbsolutePath());
        if (!f.exists()) {
            if(DEBUG.DR) System.out.println("Loading Default Datasource");
            loadDefaultDataSources();
        } else {
            int type;
            try{
                if(DEBUG.DR) System.out.println("Loading Existing Datasource");
                SaveDataSourceViewer rViewer = unMarshallMap(f);
                Vector rsources = rViewer.getSaveDataSources();
                while (!(rsources.isEmpty())){
                    DataSource ds = (DataSource)rsources.remove(0);
                    ds.setResourceViewer();
                    try {
                        dataSourceList.addOrdered(ds);
                    } catch(Exception ex) {System.out.println("DataSourceViewer.loadDataSources"+ex);}
                }
            } catch (Exception ex) {
                System.out.println("Datasource loading problem = "+ex);
                ex.printStackTrace();
                loadDefaultDataSources();
            }
        }
    }
    
    private void loadDefaultDataSources() {
        try {
            String breakTag = "";
            //dataSourceList.getContents().addElement(breakTag);
            DataSource ds1 = new FavoritesDataSource("My Saved Content");
            dataSourceList.addOrdered(ds1);
            //dataSourceList.getContents().addElement(breakTag);
            DataSource ds2 = new LocalFileDataSource("My Computer","");
            dataSourceList.addOrdered(ds2);
            // default selection
            dataSourceList.setSelectedValue(ds2,true);
            DataSourceViewer.saveDataSourceViewer();
        } catch(Exception ex) {
            if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
        }
        
    }
    
    private ImageIcon getThumbnail(org.osid.repository.Asset asset) {
        try {
            org.osid.repository.RecordIterator recordIterator = asset.getRecords();
            while (recordIterator.hasNextRecord()) {
                org.osid.repository.Record record = recordIterator.nextRecord();
                org.osid.repository.PartIterator partIterator = record.getParts();
                while (partIterator.hasNextPart()) {
                    org.osid.repository.Part part = partIterator.nextPart();
                    if (part.getPartStructure().getType().isEqual(thumbnailType)) {
//						ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage((String)part.getValue()));
                        ImageIcon icon = new ImageIcon(new URL((String)part.getValue()));
                        return icon;
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return noImageIcon;
    }
    
    public void searchPerformed(edu.tufts.vue.fsm.event.SearchEvent se) {
        Widget.setExpanded(DRB.browsePane, false);
        if (DEBUG.DR) {
            System.out.println("\n");
            out("Search includes:");
            edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
            for (int i = 0; i < dataSources.length; i++) {
                edu.tufts.vue.dsm.DataSource ds = dataSources[i];
                System.out.print("\t");
                if (ds.isIncludedInSearch()) {
                    System.out.print("+ ");
                } else {
                    System.out.print("- ");
                }
                System.out.println(ds.getProviderDisplayName() + " \t" + ds);
            }
        }

        if (UseFederatedSearchManager) {
            new Thread("VUE-Search") {
                public void run() {
                    if (DEBUG.DR || DEBUG.THREAD) out("search thread kicked off");
                    try {
                        performFederatedSearchAndDisplayResults();
                    } catch (Throwable t) {
                        tufts.Util.printStackTrace(t);
                        if (DEBUG.Enabled)
                            VueUtil.alert("Search Error", t);
                        else
                            VueUtil.alert(t.getMessage(), "Search Error");
                    } finally {
                        queryEditor.refresh();
                    }
                }
            }.start();
        } else {
            performParallelSearchesAndDisplayResults();
        }
    }
    
    private static JLabel SearchingLabel;
    private static final boolean UseSingleScrollPane = true;
    
    private static class StatusLabel extends JLabel {
        StatusLabel(String s, boolean center) {
            super(s, center ? CENTER : LEFT);
            if (center)
                setBorder(new EmptyBorder(3,0,3,0));
            else
                setBorder(new EmptyBorder(3,22,3,0));
            setMinimumSize(new Dimension(getWidth(), WidgetStack.TitleHeight));
            setPreferredSize(new Dimension(getWidth(), WidgetStack.TitleHeight));
        }
    }

    private static class Osid2AssetResourceFactory {

        // TODO: Resources want to be atomic, so we should cache
        // the result of converting the Asset to a Resource, and
        // store the resource in a hash based on the Asset to 
        // return for future lookups.
        
        static Resource createResource(org.osid.repository.Asset asset,
                                       org.osid.repository.Repository repository,
                                       org.osid.OsidContext context)
            throws org.osid.repository.RepositoryException
        {
            Resource r = new Osid2AssetResource(asset, context);
            if (DEBUG.DR) r.addProperty("@_Repository", repository.getDisplayName());
            return r;
        }
    }

    private static int SearchCounter = 0;
    
    private  class SearchThread extends Thread
    {
        public final Widget mResultPane;

        private final org.osid.repository.Repository mRepository;
        private final String mSearchString;
        
        private java.io.Serializable mSearchCriteria;
        private org.osid.shared.Type mSearchType;
        private org.osid.shared.Properties mSearchProperties;

        private final JLabel mStatusLabel;
        private final String mRepositoryName;

        public SearchThread(org.osid.repository.Repository r,
                            String searchString,
                            Serializable searchCriteria,
                            org.osid.shared.Type searchType,
                            org.osid.shared.Properties searchProperties)
            throws org.osid.repository.RepositoryException
        {
            super(mSearchThreadGroup, "VUE.Search" + (SearchCounter++) + " " + searchString + " in " + r.getDisplayName());
            setDaemon(true);
            
            mRepository = r;
            mSearchString = searchString;
            mSearchCriteria = searchCriteria;
            mSearchType = searchType;
            mSearchProperties = searchProperties;

            mRepositoryName = r.getDisplayName();

            mResultPane = new Widget("Searching " + mRepositoryName);
            mStatusLabel = new StatusLabel("Searching for " + mSearchString + " ...", true);
            mResultPane.add(mStatusLabel);

            if (DEBUG.DR) out("created search thread for: " + mRepositoryName + " \t" + mRepository);
        }

        public void run() {

            if (stopped())
                return;

            if (DEBUG.DR) out("KICKED OFF");
            
            try {
                adjustQuery();
                if (stopped())
                    return;

                // TODO: ultimately, the repository will need some kind of callback
                // architecture, so that a search can be aborted even while waiting for
                // the server to come back, tho it'll probably need to use channel based
                // NIO to really get that working.  Should that day come, the federated
                // search manager could handle this full threading and calling us back
                // as results come in, so we could skip our threading code here, and so
                // other GUI's could take advantage of the fully parallel search code.
                
                // INVOKE THE SEARCH, and immediately hand off to processResultsAndDisplay
                processResultsAndDisplay(mRepository.getAssetsBySearch(mSearchCriteria, mSearchType, mSearchProperties));
                
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
                if (stopped())
                    return;
                mResultPane.setTitle("Results: " + mRepositoryName);
                mResultPane.removeAll();
                JTextArea textArea = new JTextArea(mRepositoryName + ": Search Error: " + t);
                textArea.setBorder(new EmptyBorder(4,22,6,0));
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setOpaque(false);
                mResultPane.add(textArea);
            }

            // must call revalidate because we're coming from another thread:
            mResultPane.revalidate();
        }

        // As we create a new Widget for the output of every search, in terms of a new
        // search replacing a still running search, we're already safe UI wise even if
        // we never interrupted a search, but we might as well be careful about it / not
        // waste cycles, and it's nice if the user can abort if desired.
        
        private boolean stopped() {
            if (isInterrupted()) {
                if (DEBUG.DR) out("ABORTED");
                mResultPane.setTitle(mRepositoryName + " (Aborted)");
                mStatusLabel.setText("Search Aborted.");
                return true;
            } else
                return false;
        }

        private void adjustQuery()
            throws org.osid.repository.RepositoryException
        {
            //if (DEBUG.DR) out("checking for query adjustment");
            edu.tufts.vue.fsm.QueryAdjuster adjuster = federatedSearchManager
                .getQueryAdjusterForRepository(mRepository.getId());
            if (adjuster != null) {
                edu.tufts.vue.fsm.Query q = adjuster.adjustQuery(mRepository,
                                                                 mSearchCriteria,
                                                                 mSearchType,
                                                                 mSearchProperties);
                mSearchCriteria = q.getSearchCriteria();
                mSearchType = q.getSearchType();
                mSearchProperties = q.getSearchProperties();
                if (DEBUG.DR) out("adjusted query");
            }
            //if (DEBUG.DR) out("done checking for query adjustment");
        }

        private void processResultsAndDisplay(org.osid.repository.AssetIterator assetIterator)
            throws org.osid.repository.RepositoryException
        {
            if (stopped())
                return;
            
            if (DEBUG.DR) out("processing AssetIterator...");

            final java.util.List resourceList = new java.util.ArrayList();

            final int maxResult = 100;
            int resultCount = 0;
            while (assetIterator.hasNextAsset()) {
                org.osid.repository.Asset asset = assetIterator.nextAsset();
                if (++resultCount > maxResult)
                    continue;
                resourceList.add(Osid2AssetResourceFactory.createResource(asset,
                                                                          mRepository,
                                                                          DataSourceViewer.this.context));
            }

            if (DEBUG.DR) out("done processing AssetIterator");
            
            String name = "Results: " + mRepositoryName;

            if (DEBUG.DR) {
                if (resultCount > maxResult)
                    out(name + "; returned a total of " + resultCount + " matches");
                out(name + "; " + resourceList.size() + " results");
            }
            
            if (resourceList.size() > 0)
                name += " (" + resourceList.size() + ")";

            if (stopped())
                return;
            
            mResultPane.setTitle(name);
            mResultPane.removeAll();
            
            if (resourceList.size() == 0) {
                mResultPane.add(new StatusLabel("No results for " + mSearchString, false));
            } else {
                mResultPane.add(new ResourceList(resourceList, name));
            }
        }
    }
                                              

    private synchronized void performParallelSearchesAndDisplayResults()
    {
        if (DEBUG.DR) {
            synchronized (System.out) {
                System.out.println("Current search thread group:");
                mSearchThreadGroup.list();
            }
        }
        
        mSearchThreadGroup.interrupt();
        
        final String searchString = "\"" + queryEditor.getSearchDisplayName() + "\"";
        final WidgetStack resultsStack = new WidgetStack("searchResults " + searchString);
        final org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
        final java.io.Serializable searchCriteria = queryEditor.getCriteria();
        final org.osid.shared.Type searchType = queryEditor.getSearchType();
        final org.osid.shared.Properties searchProperties = queryEditor.getProperties();
        final Thread[] threads = new Thread[repositories.length];
        
        if (DEBUG.DR) {
            out("Searching criteria [" + searchString + "] in selected repositories."
                + "\n\tsearchType=" + searchType
                + "\n\tsearchProps=" + searchProperties);
        }
    
        for (int i = 0; i < repositories.length; i++) { 
            final org.osid.repository.Repository repository = repositories[i];

            SearchThread searchThread = null;
            try {

                searchThread = new SearchThread(repository, searchString,
                                                searchCriteria, searchType, searchProperties);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "Failed to create search in " + repository);
                if (DEBUG.Enabled)
                    VueUtil.alert("Search Error", t);
                else
                    VueUtil.alert(t.getMessage(), "Search Error");
            }
            
            threads[i] = searchThread;
            resultsStack.addPane(searchThread.mResultPane, 0f);
        }
        
        DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);

        //-----------------------------------------------------------------------------
        // KICK OFF THE SEARCH THREADS
        //-----------------------------------------------------------------------------

        for (int i = 0; i < threads.length; i++)
            threads[i].start();
    }
    
    private synchronized void performFederatedSearchAndDisplayResults()
        throws org.osid.repository.RepositoryException,
               org.osid.shared.SharedException
    {
        //final String dockTitle = "Search Results for \"" + queryEditor.getSearchDisplayName() + "\"";
        final String searchString = "\"" + queryEditor.getSearchDisplayName() + "\"";
        
        /*
          Store our results since we will fill a panel with each repository's results and one with all.
          We can't get the iterator contents again, without re-doing the search.
         
          We know the repositories we searched.  Some may have returned results, others may not.  We will
          make a vector for each set of results with a parallel vector of repository ids.
         */
        
        final java.util.List resultList = new java.util.ArrayList();
        final java.util.List dataSourceIdStringList = new java.util.ArrayList();
        final java.util.List repositoryDisplayNameList = new java.util.ArrayList();
        
        org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
        edu.tufts.vue.dsm.DataSource[] dataSources = sourcesAndTypesManager.getDataSourcesToSearch(); // will be same length
        
        final WidgetStack resultsStack = new WidgetStack("searchResults " + searchString);
        final Widget[] resultPanes = new Widget[repositories.length];
        
        for (int i = 0; i < repositories.length; i++) {
            org.osid.repository.Repository r = repositories[i];
            if (DEBUG.DR) out("to search: " + r.getDisplayName() + " \t" + r);
            
            dataSourceIdStringList.add(dataSources[i].getId().getIdString());
            repositoryDisplayNameList.add(r.getDisplayName());
            resultList.add(new java.util.ArrayList());
            
            resultPanes[i] = new Widget("Searching " + r.getDisplayName());
            resultPanes[i].add(new StatusLabel("Searching for " + searchString + " ...", true));
            resultsStack.addPane(resultPanes[i], 0f);
        }
        
        DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);
        
        // get our search results
        java.io.Serializable searchCriteria = queryEditor.getCriteria();
        if (DEBUG.DR) {
            out("Searching criteria [" + searchCriteria + "] in selected repositories. SearchProps=" + queryEditor.getProperties());
        }
        org.osid.shared.Properties searchProperties = queryEditor.getProperties();
        
        edu.tufts.vue.fsm.ResultSetManager resultSetManager
                = federatedSearchManager.getResultSetManager(searchCriteria,
                queryEditor.getSearchType(),
                searchProperties);
        if (DEBUG.DR) out("got result set manager " + resultSetManager);
        
        for (int i=0; i < dataSources.length; i++) {
            org.osid.repository.AssetIterator assetIterator = resultSetManager.getAssets(dataSources[i].getId().getIdString());
            int counter = 0;
            while (assetIterator.hasNextAsset() && (counter <= 100)) {
                org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
                counter++;
                String dataSourceIdString = dataSources[i].getId().getIdString();
                int index = dataSourceIdStringList.indexOf(dataSourceIdString);
                java.util.List v = (java.util.List) resultList.get(index);
                
                // TODO: Resources eventually want to be atomic, so a factory
                // should be queried for a resource based on the asset.
                Osid2AssetResource resource = new Osid2AssetResource(nextAsset, this.context);
                v.add(resource);
            }
        }
        
        // Display the results in the result panes
        for (int i = 0; i < repositories.length; i++) {
            java.util.List resourceList = (java.util.List) resultList.get(i);
            String name = "Results: " + (String) repositoryDisplayNameList.get(i);
            if (DEBUG.DR) out(name + ": " + resourceList.size() + " results");
            
            if (resourceList.size() > 0)
                name += " (" + resourceList.size() + ")";
            
            resultPanes[i].setTitle(name);
            resultPanes[i].removeAll();
            
            if (resourceList.size() == 0) {
                //resultsStack.addPane(name, new JLabel("  No results"), 0f);
                // there might have been an exception
                String message = resultSetManager.getExceptionMessage(i);
                if (message != null) {
                    resultPanes[i].add(new StatusLabel(message, false));
                } else {
                    resultPanes[i].add(new StatusLabel("No results for " + searchString, false));
                }
            } else {
                resultPanes[i].add(new ResourceList(resourceList));
            }
        }
    }
    
    private void displayEditOrInfo(edu.tufts.vue.dsm.DataSource ds) {
        refreshEditInfo(ds);
        editInfoDockWindow.setVisible(true);
    }
    
    private void displayEditOrInfo(DataSource ds) {
        refreshEditInfo(ds);
        editInfoDockWindow.setVisible(true);
    }
    
    private PropertyMap buildPropertyMap(edu.tufts.vue.dsm.DataSource dataSource)
    {
    	PropertyMap map = new PropertyMap();    	
    	
    	try {
			org.osid.repository.Repository repository = dataSource.getRepository();
			
		
					
			map.addProperty("Repository Id",(Object)repository.getId().getIdString());
			map.addProperty("Name",(Object)repository.getDisplayName());
			map.addProperty("Description",(Object)repository.getDescription());
			map.addProperty("Type",(Object)edu.tufts.vue.util.Utilities.typeToString(repository.getType()));
			map.addProperty("Creator",(Object)dataSource.getCreator());
			map.addProperty("Publisher",(Object)dataSource.getPublisher());
			map.addProperty("Release Date",(Object)edu.tufts.vue.util.Utilities.dateToString(dataSource.getReleaseDate()));
			map.addProperty("Provider Id",(Object)dataSource.getProviderId().getIdString());
			String osidName = dataSource.getOsidName() + " " + dataSource.getOsidVersion();
			map.addProperty("Osid Service",(Object)osidName);
			map.addProperty("Osid Load Key",(Object)dataSource.getOsidLoadKey());
			map.addProperty("Provider Display Name",(Object)dataSource.getProviderDisplayName());
			map.addProperty("Provider Description",(Object)dataSource.getProviderDescription());
			String online = dataSource.isOnline() ? "Yes" : "No";
			map.addProperty("Online?",(Object)online);
			String supportsUpd = dataSource.supportsUpdate() ? "The Library Supports Updating" : "The Library Is Read Only";
			map.addProperty("Supports Update?",(Object)supportsUpd);
			
			org.osid.shared.TypeIterator typeIterator = repository.getAssetTypes();
			StringBuffer assetTypes = new StringBuffer();
			while (typeIterator.hasNextType()) {
				assetTypes.append(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType()));
				assetTypes.append(", ");
			}
			
			if (assetTypes.length() > 0)
				assetTypes.delete(assetTypes.length()-2,assetTypes.length()-1);
			
			map.addProperty("Asset Types",(Object)assetTypes.toString());
			
			typeIterator = repository.getSearchTypes();
			StringBuffer searchTypes = new StringBuffer();
			while (typeIterator.hasNextType()) {
				searchTypes.append(edu.tufts.vue.util.Utilities.typeToString(typeIterator.nextType()));				
				searchTypes.append(", ");
				
			}
			if (searchTypes.length() > 0)
				searchTypes.delete(searchTypes.length()-2,searchTypes.length()-1);
			
			map.addProperty("Search Types",(Object)searchTypes.toString());
	
			
/*			java.awt.Image image = null;
			if ( (image = dataSource.getIcon16x16()) != null ) {		
				gbConstraints.gridx = 0;
				gbConstraints.gridy++;
				add(new javax.swing.JLabel(new javax.swing.ImageIcon(image)),gbConstraints);
			}*/
		} catch (Throwable t) {
			//t.printStackTrace();
			System.out.println(t.getMessage());
		}
    	
    	return map;
    }
    
    private void refreshEditInfo(edu.tufts.vue.dsm.DataSource ds) {
        String dockTitle = ds.getRepositoryDisplayName();
        PropertyMap dsProps = buildPropertyMap(ds);
        
        final WidgetStack editInfoStack = new WidgetStack();
        
        if (editInfoDockWindow == null) {
            if (ds.hasConfiguration()) {
                editInfoStack.addPane("Configuration",new javax.swing.JScrollPane(new EditLibraryPanel(this,ds)));
            } else {
                JPanel jc = new JPanel();
                jc.add(new JLabel("None"));
                editInfoStack.addPane("Configuration",jc);
                Widget.setExpanded(jc,false);
            }
            
            
            //JPanel descriptionPanel = new JPanel();
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
            //gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            //gbConstraints.insets = new java.awt.Insets(10,12,10,12);
            //descriptionPanel.setLayout(gbLayout);
            MetaDataPane metaDataPane = new MetaDataPane();
            metaDataPane.loadProperties(dsProps);
            //descriptionPanel.add(metaDataPane,gbConstraints);
            //descriptionPanel.add(new LibraryInfoPanel(ds),gbConstraints);
            //editInfoStack.addPane("Description",new javax.swing.JScrollPane(descriptionPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
            editInfoStack.addPane("Content Description",metaDataPane);
            
            editInfoDockWindow = GUI.createDockWindow(dockTitle, editInfoStack);
            editInfoDockWindow.setWidth(300);
            editInfoDockWindow.setHeight(300);
            editInfoDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                    DRB.dockWindow.getY());
            editInfoDockWindow.setTitle(dockTitle);
        } else if (editInfoDockWindow.isVisible() || (!dockTitle.equals(editInfoDockWindow.getTitle()))) {
            if (ds.hasConfiguration()) {
                editInfoStack.addPane("Configuration", new javax.swing.JScrollPane(new EditLibraryPanel(this,ds)));
            } else {
                JPanel jc = new JPanel();
                jc.add(new JLabel("None"));
                editInfoStack.addPane("Configuration",jc);
                Widget.setExpanded(jc,false);
            }
            //JPanel descriptionPanel = new JPanel();
            //java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            //java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
            //gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            //gbConstraints.insets = new java.awt.Insets(10,12,10,12);
            //descriptionPanel.setLayout(new BorderLayout());
            MetaDataPane metaDataPane = new MetaDataPane();
            metaDataPane.loadProperties(dsProps);
            metaDataPane.setPreferredSize(new Dimension(editInfoDockWindow.getWidth(),metaDataPane.getHeight()));
            //descriptionPanel.add(metaDataPane,BorderLayout.CENTER);
            //descriptionPanel.add(new LibraryInfoPanel(ds),gbConstraints);
            editInfoStack.addPane("Content Description",metaDataPane);
            
            editInfoDockWindow.setTitle(dockTitle);
            editInfoDockWindow.setContent(editInfoStack);
        }
    }
    
    private void refreshEditInfo(DataSource ds) {
        String dockTitle = "Library Information for " + ds.getDisplayName();
        final WidgetStack editInfoStack = new WidgetStack();
        
        if (editInfoDockWindow == null) {
            editInfoStack.addPane("Configuration",new javax.swing.JScrollPane(new EditLibraryPanel(this,ds)));
            editInfoDockWindow = GUI.createDockWindow(dockTitle, editInfoStack);
            editInfoDockWindow.setWidth(300);
            editInfoDockWindow.setHeight(300);
            editInfoDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                    DRB.dockWindow.getY());
            editInfoDockWindow.setTitle(dockTitle);
        } else if (editInfoDockWindow.isVisible() || (!dockTitle.equals(editInfoDockWindow.getTitle()))) {
            editInfoStack.addPane("Configuration", new javax.swing.JScrollPane(new EditLibraryPanel(this,ds)));
            editInfoDockWindow.setTitle(dockTitle);
            editInfoDockWindow.setContent(editInfoStack);
        }
    }
    
    /*
     * static method that returns all the datasource where Maps can be published.
     * Only FEDORA @ Tufts is available at present
     */
    public static Vector getPublishableDataSources(int i) {
        Vector mDataSources = new Vector();
        if (dataSourceList != null) {
            Enumeration e = dataSourceList.getContents().elements();
            while(e.hasMoreElements() ) {
                Object mDataSource = e.nextElement();
                if(mDataSource instanceof Publishable)
                    mDataSources.add(mDataSource);
            }
        }
        return mDataSources;
        
    }
    /*
     * returns the default favorites resources.  This is will be used to add favorites and perform search
     */
    public static org.osid.repository.Repository getDefualtFavoritesRepository() {
        DefaultListModel model = dataSourceList.getContents();
        
        try {
            for(int i = 0; i<model.size();i++){
                Object o = model.getElementAt(i);
                if(o instanceof edu.tufts.vue.dsm.DataSource){
                    edu.tufts.vue.dsm.DataSource datasource = (edu.tufts.vue.dsm.DataSource)o;
                    org.osid.repository.Repository repository = datasource.getRepository();
                    if(repository.getType().isEqual(favoritesRepositoryType)) {
                        return repository;
                    }
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
    public static void saveDataSourceViewer() {
        if (dataSourceList == null) {
            System.err.println("DataSourceViewer: No dataSourceList to save.");
            return;
        }
        int size = dataSourceList.getModel().getSize();
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        Vector sDataSources = new Vector();
        for (int i = 0; i<size; i++) {
            Object item = dataSourceList.getModel().getElementAt(i);
            if (DEBUG.DR) System.out.println("saveDataSourceViewer: item " + i + " is " + item.getClass().getName() + "[" + item + "] of " + size);
            if (item instanceof DataSource) {
                sDataSources.add((DataSource)item);
            } else {
                if (DEBUG.DR) System.out.println("\tskipped item of " + item.getClass());
            }
        }
        try {
            if (DEBUG.DR) System.out.println("saveDataSourceViewer: creating new SaveDataSourceViewer");
            SaveDataSourceViewer sViewer= new SaveDataSourceViewer(sDataSources);
            if (DEBUG.DR) System.out.println("saveDataSourceViewer: marshallMap: saving " + sViewer + " to " + f);
            marshallMap(f,sViewer);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    
    public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
        Marshaller marshaller = null;
        Mapping mapping = new Mapping();
        
        try {
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: loading mapping " + XML_MAPPING_DEFAULT);
            mapping.loadMapping(XML_MAPPING_DEFAULT);
            marshaller.setMapping(mapping);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: marshalling " + dataSourceViewer + " to " + file + "...");
            marshaller.marshal(dataSourceViewer);
            if (DEBUG.DR) System.out.println("DataSourceViewer.marshallMap: done marshalling.");
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("DRBrowser.marshallMap " + t.getMessage());
        }
    }
    
    public  SaveDataSourceViewer unMarshallMap(File file) throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException{
        Unmarshaller unmarshaller = null;
        SaveDataSourceViewer sviewer = null;
        Mapping mapping = new Mapping();
        unmarshaller = new Unmarshaller();
        mapping.loadMapping(XML_MAPPING_DEFAULT);
        unmarshaller.setMapping(mapping);
        FileReader reader = new FileReader(file);
        sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
        reader.close();
        return sviewer;
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
    private void out(Object o) {
        System.err.println("DSV "
                + new Long(System.currentTimeMillis()).toString().substring(8)
                + " [" + Thread.currentThread().getName() + "] "
                + (o==null?"null":o.toString()));
    }
}
