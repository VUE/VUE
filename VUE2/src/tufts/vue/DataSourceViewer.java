/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import org.osid.repository.Repository;
import org.osid.repository.RepositoryException;

// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.osid.provider.ProviderException;
import org.xml.sax.InputSource;

import tufts.vue.gui.*;

public class DataSourceViewer extends JPanel
    implements KeyListener, edu.tufts.vue.fsm.event.SearchListener, ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataSourceViewer.class);
    //private static final boolean UseFederatedSearchManager = false;
    
    private static DRBrowser DRB;
    private static Object activeDataSource;
    String breakTag = "";
    
    public final static int ADD_MODE = 0;
    public final static int EDIT_MODE = 1;
    public static final org.osid.shared.Type favoritesRepositoryType = new edu.tufts.vue.util.Type("edu.tufts","favorites","Favorites");
    JPopupMenu popup;
    
    AddLibraryDialog addLibraryDialog;
    UpdateLibraryDialog updateLibraryDialog;
    EditLibraryDialog editLibraryDialog;
    RemoveLibraryDialog removeLibraryDialog;
    GetLibraryInfoDialog getLibraryInfoDialog;
    
    static AbstractAction checkForUpdatesAction;
    static AbstractAction addLibraryAction;
    AbstractAction editLibraryAction;
    AbstractAction removeLibraryAction;
    AbstractAction getLibraryInfoAction;
    MouseListener refreshDSMouseListener;
    
//     JButton optionButton = new VueButton("add");
//     JButton searchButton = new JButton("Search");
    
   // public static Vector  allDataSources = new Vector();
    public static DataSourceList dataSourceList;
    
    private DockWindow resultSetDockWindow;
    private static DockWindow editInfoDockWindow; // hack for now: need this set before DSV is created
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
    //org.osid.registry.Provider checked[];
    
    private final java.util.List<SearchThread> mSearchThreads = java.util.Collections.synchronizedList(new java.util.LinkedList<SearchThread>());
    
    public DataSourceViewer(DRBrowser drBrowser) {
        //GUI.activateWaitCursor();
        
        VUE.pushDiag("DSV");

        if (editInfoDockWindow == null)
            initUI();
            
        setLayout(new BorderLayout());
        this.DRB = drBrowser;
        dataSourceList = new DataSourceList(this);
        Widget.setExpanded(DRB.browsePane, false);
        edu.tufts.vue.dsm.DataSource dataSources[] = null;
        //org.apache.log4j.NDC.push(getClass().getSimpleName() + ";");
        try {
            // load old-style data sources
            Log.info("Loading old style data sources...");
            loadOldStyleDataSources();
            Log.info("Loaded old style data sources.");
        } catch (Throwable t) {
            VueUtil.alert("Error loading old Resource","Error");
        }
        try {
            // load new data sources
            dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            Log.info("loading Installed data sources via Data Source Manager");

            edu.tufts.vue.dsm.impl.VueDataSourceManager.load();

            dataSources = dataSourceManager.getDataSources();
            Log.info("finished loading data sources.");
            for (int i=0; i < dataSources.length; i++) {
                Log.info("adding to the UI: " + dataSources[i]);
                try {
                    dataSourceList.addOrdered(dataSources[i]);
                } catch (Throwable t) {
                    Log.error("adding to UI: " + dataSources[i], t);
                    VueUtil.alert("Error loading Resource #" + (i+1), "Error");
                    //VueUtil.alert("Error loading Resource " + dataSources[i], "Error");
                }
            }
        } catch (Throwable t) {
            //Util.printStackTrace(t, "DataSourceViewer construct");
            VueUtil.alert("Error loading Resource","Error");
        }
        
        federatedSearchManager = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
        sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
        if (DEBUG.Enabled) Log.debug("sourcesAndTypesManager: " + Util.tags(sourcesAndTypesManager));
        queryEditor = federatedSearchManager.getQueryEditorForType(searchType);
        queryEditor.addSearchListener(this);
        
        // select the first new data source, if any
        if ((dataSources != null) && (dataSources.length > 0)) {
            setActiveDataSource(dataSources[0]);
        }
        DRB.searchPane.removeAll();
        DRB.searchPane.add((JPanel) queryEditor, DRBrowser.SEARCH_EDITOR);
        DRB.searchPane.revalidate();
        DRB.searchPane.repaint();
        // WORKING: stop using this preview panel?
        queryEditor.addSearchListener(VUE.getInspectorPane());
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
        Widget.setRefreshAction(new JButton("B"),new MouseAdapter());
        Widget.setMiscAction(DRB.librariesPanel, new MiscActionMouseListener(), "dockWindow.addButton");
        Widget.setHelpAction(DRB.browsePane,VueResources.getString("dockWindow.Content.browsePane.helpText"));;
        Widget.setHelpAction(DRB.resultsPane,VueResources.getString("dockWindow.Content.resultsPane.helpText"));;
        Widget.setHelpAction(DRB.searchPane,VueResources.getString("dockWindow.Content.searchPane.helpText"));;

        VUE.popDiag();

        editInfoDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                                       DRB.dockWindow.getY());


        NoConfig.setMinimumSize(new Dimension(100,50));
        configMetaData.setName("Content Description");
        
        GUI.clearWaitCursor();
    }
    
    class MiscActionMouseListener extends MouseAdapter
    {
    	public void mouseClicked(MouseEvent e)
    	{
    		addLibraryAction.actionPerformed(null);
    	}
    }
    private void displayContextMenu(MouseEvent e) {
        getPopup(e).show(e.getComponent(), e.getX(), e.getY());
    	}

    	JPopupMenu m = null;
    	private static final JMenuItem aboutResource = new JMenuItem("About this Resource");
    	//private static final JMenuItem configureResource = new JMenuItem("Configure Resource");
    	private static final JMenuItem deleteResource = new JMenuItem("Delete Resource");
    	Point lastMouseClick = null;

    	public void actionPerformed(ActionEvent e)
    	{
    		if (e.getSource().equals(aboutResource))
    		{
    			int index = dataSourceList.locationToIndex(lastMouseClick);
    			//ResourceIcon o = (ResourceIcon)this.getModel().getElementAt(index);
    			if (dataSourceList.getModel().getElementAt(index) instanceof DataSource)
    			{
    				displayEditOrInfo((DataSource)dataSourceList.getModel().getElementAt(index));
    			}
    			else
    			{
    				displayEditOrInfo((edu.tufts.vue.dsm.DataSource)dataSourceList.getModel().getElementAt(index));
    			}
    		}
    		/*else if (e.getSource().equals(configureResource))
    		{
    			
    		}*/
    		else if (e.getSource().equals(deleteResource))
    		{
    			int index = dataSourceList.locationToIndex(lastMouseClick);
    			dataSourceList.setSelectedIndex(index);
    			removeLibraryAction.actionPerformed(e);
    		}
    	}

    	private JPopupMenu getPopup(MouseEvent e) 
    	{
    		if (m == null)
    		{
    			m = new JPopupMenu("Datasource Menu");
    		
    			m.add(aboutResource);
    			m.addSeparator();
    			//m.add(configureResource);
    			//m.addSeparator();
    			m.add(deleteResource);
    			aboutResource.addActionListener(this);
    			//configureResource.addActionListener(this);
    			deleteResource.addActionListener(this);
    		}
    		
    		return m;
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
                        if (DEBUG.DR) Log.debug("DataSource " + ds + " [" + ds.getProviderDisplayName() + "] inclusion: " + included);
                        ds.setIncludedInSearch(included);
                        dataSourceList.repaint();
                        queryEditor.refresh();
                        
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            try {
                                synchronized (dataSourceManager) {
                                    if (DEBUG.DR) Log.debug("DataSourceManager saving...");
                                    dataSourceManager.save();
                                    if (DEBUG.DR) Log.debug("DataSourceManager saved.");
                                }
                            } catch (Throwable t) {
                                tufts.Util.printStackTrace(t);
                            }
                        }});
                    }
                    if(e.getButton() == e.BUTTON3)
                    {
                     	lastMouseClick = e.getPoint();
           				displayContextMenu(e);
                    }
                    //popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
    }
    public static Object getActiveDataSource() {
        return activeDataSource;
    }
    
    public void setActiveDataSource(DataSource ds){
        if (DEBUG.DR) Log.debug("Set active data source: " + ds);
        this.activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.searchPane, false);
        Widget.setExpanded(DRB.browsePane, true);
        Widget.setTitle(DRB.browsePane, "Browse: " + ds.getDisplayName());
        DRB.browsePane.removeAll();
        DRB.browsePane.add(ds.getResourceViewer());
        
        if (ds instanceof LocalFileDataSource) {
            Widget.setRefreshAction(DRB.browsePane,(MouseListener)refreshDSMouseListener);
        } else {
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
        
        if (ds instanceof LocalFileDataSource) {
            Widget.setRefreshAction(DRB.browsePane,(MouseListener)refreshDSMouseListener);
        } else {
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
                            updateLibraryDialog = new UpdateLibraryDialog(dataSourceList,
                                    ((edu.tufts.vue.dsm.DataSource)dataSourceList.getSelectedValue()));
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
                        String displayName = ds.getRepositoryDisplayName();
                        
                        //figure out which one of the results needs to be deleted
                        DefaultListModel listComponents = dataSourceList.getContents();
                        int index = dataSourceList.getContents().indexOf(ds);
                        int instanceIndex = 0;
                        for (int p=0; p< index; p++) {
                            edu.tufts.vue.dsm.impl.VueDataSource o2 = (edu.tufts.vue.dsm.impl.VueDataSource)listComponents.get(p);
                            
                            if (o2.getRepositoryDisplayName() == ds.getRepositoryDisplayName())
                                instanceIndex++;
                        }
                        
                        if (javax.swing.JOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                "Do you really want to delete " + displayName + "?",
                                "Delete Resource",
                                javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                            dataSourceManager.remove(ds.getId());
                            GUI.invokeAfterAWT(new Runnable() { public void run() {
                                try {
                                    synchronized (dataSourceManager) {
                                        if (DEBUG.DR) Log.debug("DataSourceManager saving...");
                                        dataSourceManager.save();
                                        if (DEBUG.DR) Log.debug("DataSourceManager saved.");
                                    }
                                } catch (Throwable t) {
                                    tufts.Util.printStackTrace(t);
                                }
                            }});
                            dataSourceList.getContents().removeElement(ds);
                            saveDataSourceViewer();
                            
                            //delete it
                            WidgetStack widgetStack = null;
                            
                            if (DRB.resultsPane != null)
                            	widgetStack = (WidgetStack)DRB.resultsPane.getComponent(0);
                            
                            if (widgetStack != null) {
                                Component[] comps = widgetStack.getComponents();
                                int found =0;
                                for (int i = 0; i < comps.length; i++) {
                                    String compName = comps[i].getName();
                                    if ((compName != null) && (compName.indexOf(displayName)!= -1)) {
                                        if ((found == instanceIndex) || (found == instanceIndex+1))
                                            widgetStack.remove(comps[i]);
                                        
                                        found++;
                                    }
                                }
                            }
                        }
                    } else if( o instanceof tufts.vue.DataSource) {
                        tufts.vue.DataSource ds = (tufts.vue.DataSource) o;
                        String displayName = ds.getDisplayName();
                        
                        if (javax.swing.JOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                "Do you really want to delete " + displayName,
                                "Delete Resource",
                                javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                            dataSourceList.getContents().removeElement(ds);
                            saveDataSourceViewer();
                        }
                        DRB.browsePane.remove(ds.getResourceViewer());
                        DRB.browsePane.revalidate();
                        DRB.browsePane.repaint();
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
        try {
            oldDataSource = (DataSource) activeDataSource;
        } catch(ClassCastException cce) {
            return;
        } finally {
            GUI.clearWaitCursor();
        }
        
        if (oldDataSource instanceof LocalFileDataSource) {
            String address = ((LocalFileDataSource)oldDataSource).getAddress();
            String displayName = ((LocalFileDataSource)oldDataSource).getDisplayName();
            try {
                ds = new LocalFileDataSource(displayName,address);
            } catch(DataSourceException dse) {
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
        edu.tufts.vue.dsm.DataSource ds = null;
        if (o != null) {
            // for the moment, we are doing double work to keep old data sources
            if (o instanceof edu.tufts.vue.dsm.DataSource) {
                ds = (edu.tufts.vue.dsm.DataSource)o;
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
        
        checkForUpdatesAction.setEnabled(true);
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

    private static final Collection<tufts.vue.DataSource> oldStyleDataSources = new ArrayList();
    
    private void loadOldStyleDataSources() {

        VUE.pushDiag("old");
        
        boolean init = true;
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        if (DEBUG.DR) Log.debug("Data source file: " + f.getAbsolutePath());
        if (!f.exists()) {
            if (DEBUG.DR) System.out.println("Loading default DataSources (does not exist: " + f + ")");
            loadDefaultDataSources();
        } else {
            int type;
            try {
                if (DEBUG.DR) Log.debug("Loading saved DataSources from " + f + "; unmarshalling...");
                VUE.pushDiag("XML");
                final SaveDataSourceViewer dataSourceContainer = unMarshallMap(f);
                VUE.popDiag();
                if (DEBUG.DR) Log.debug("Unmarshalling completed from " + f);
                final Vector dataSources = dataSourceContainer.getSaveDataSources();
                synchronized (oldStyleDataSources) {
                    oldStyleDataSources.clear();
                    oldStyleDataSources.addAll(dataSources);
                }
                if (DEBUG.DR) Log.debug("Found " + dataSources.size() + " DataSources: " + dataSources);
                int i = 0;
                while (!(dataSources.isEmpty())){
                    final DataSource ds = (DataSource) dataSources.remove(0);
                    i++;
                    if (DEBUG.DR) Log.debug("#" + i + ": loading " + Util.tags(ds));
//                     VUE.pushDiag("#" + i);
//                     ds.setResourceViewer();
//                     VUE.popDiag();
                    try {
                        dataSourceList.addOrdered(ds);
                    } catch(Exception ex) {System.out.println("DataSourceViewer.loadDataSources"+ex);}
                }
            } catch (Exception ex) {
                Log.error("Loading DataSources; loading defaults as fallback", ex);
                loadDefaultDataSources();
            }
        }

        VUE.popDiag();
    }

    public static void cacheDataSourceViewers() {
        VUE.pushDiag("DSV-CACHE");

        final java.util.List<tufts.vue.DataSource> dataSources;
        synchronized (oldStyleDataSources) {
            dataSources = new ArrayList(oldStyleDataSources);
        }
        
        for (DataSource ds : dataSources) {
            Log.info("caching viewer for: " + Util.tags(ds));
            try {
                ds.getResourceViewer();
            } catch (Throwable t) {
                Log.error("caching viewer for " + ds, t);
            }
        }
        VUE.popDiag();
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
        } catch (Exception ex) {
            //if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
            Util.printStackTrace(ex, "Loading default data sources");
        }
        
    }
    
//     private ImageIcon getThumbnail(org.osid.repository.Asset asset) {
//         try {
//             org.osid.repository.RecordIterator recordIterator = asset.getRecords();
//             while (recordIterator.hasNextRecord()) {
//                 org.osid.repository.Record record = recordIterator.nextRecord();
//                 org.osid.repository.PartIterator partIterator = record.getParts();
//                 while (partIterator.hasNextPart()) {
//                     org.osid.repository.Part part = partIterator.nextPart();
//                     if (part.getPartStructure().getType().isEqual(thumbnailType)) {
// //						ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage((String)part.getValue()));
//                         ImageIcon icon = new ImageIcon(new URL((String)part.getValue()));
//                         return icon;
//                     }
//                 }
//             }
//         } catch (Throwable t) {
//             t.printStackTrace();
//         }
//         return noImageIcon;
//     }

    private synchronized void stopAllSearches() {
//         if (UseFederatedSearchManager) {
//             tufts.Util.printStackTrace("stopAllSearches unimplemented for FSM");
//             return;
//         }
        synchronized (mSearchThreads) {
            if (DEBUG.DR) Log.debug("STOPPING ALL ACTIVE SEARCHES; count=" + mSearchThreads.size());
            for (Thread t : mSearchThreads)
                t.interrupt();
        }

    }
    
    public static Action getAddLibraryAction()
    {
    	return addLibraryAction;
    }
    public static Action getUpdateLibraryAction()
    {
    	return checkForUpdatesAction;
    }
    public void searchPerformed(edu.tufts.vue.fsm.event.SearchEvent se) {

        if (se == null) {
            // null SearchEvent means abort last search
            stopAllSearches();
            return;
        }
        
        Widget.setExpanded(DRB.browsePane, false);
        if (DEBUG.DR) {
            try {
                System.out.println("\n");
                Log.debug("Search includes:");
                for (edu.tufts.vue.dsm.DataSource ds : dataSourceManager.getDataSources()) {
                    System.out.print("\t");
                    if (ds.isIncludedInSearch()) {
                        System.out.print("+ ");
                    } else {
                        System.out.print("- ");
                    }
                    System.out.println(ds + "; Provider=" + ds.getProviderDisplayName());
                }
            } catch (Throwable t) {
                Util.printStackTrace(t, this + "; debug code");
            }
        }
        
        performParallelSearchesAndDisplayResults();
        
//         if (UseFederatedSearchManager) {
//             new Thread("VUE-Search") {
//                 public void run() {
//                     if (DEBUG.DR || DEBUG.THREAD) Log.debug("search thread kicked off");
//                     try {
//                         performFederatedSearchAndDisplayResults();
//                     } catch (Throwable t) {
//                         tufts.Util.printStackTrace(t);
//                         if (DEBUG.Enabled)
//                             VueUtil.alert("Search Error", t);
//                         else
//                             VueUtil.alert(t.getMessage(), "Search Error");
//                     } finally {
//                         queryEditor.refresh();
//                     }
//                 }
//             }.start();
//         } else {
//             performParallelSearchesAndDisplayResults();
//         }
    }
    
    private static JLabel SearchingLabel;
    private static final boolean UseSingleScrollPane = true;
    
    private static class StatusLabel extends JPanel {
        private JLabel label = null;
        private JLabel waitIcon = null;
        
        StatusLabel(String s, boolean center, boolean useIcon) {
            super();
            
            if (center) {
                setLayout(new FlowLayout(FlowLayout.CENTER));
                setBorder(new EmptyBorder(3,0,3,0));
            } else {
                setLayout(new FlowLayout(FlowLayout.LEFT));
                setBorder(new EmptyBorder(3,10,3,0));
            }
            
            setBackground(VueResources.getColor("dsv.statuspanel.bgColor"));
            if (useIcon) {
                waitIcon = new JLabel(VueResources.getImageIcon("dsv.statuspanel.waitIcon"));
                this.add(waitIcon);
            }
            label = new JLabel(s);
            this.add(label);
            
            //setMinimumSize(new Dimension(getWidth(), WidgetStack.TitleHeight+14));
            //setPreferredSize(new Dimension(getWidth(), WidgetStack.TitleHeight+14));
        }
        StatusLabel(String s, boolean center) {
            this(s,center,true);
        }

        public void removeIcon() {
            if (waitIcon != null)
                remove(waitIcon);
        }
        
        public void setText(String s) {
            label.setText(s);
        }
    }

    //private static final String UNKNOWN_REPOSITORY_NAME = "Unknown Repository Name";

    private static String repositoryName(org.osid.repository.Repository r) {
        if (r == null)
            return "Unknown Repository";
        
        try {
            String s = r.getDisplayName();
            if (s == null || s.trim().length() < 1) {
//                 if (r.getId() != null)
//                     return "Unknown Name: Repository " + r.getId().getIdString();
//                 else
                    return "Unknown Repository Name";
            } else
                return s;
        } catch (Throwable t) {
            Util.printStackTrace(t);
            return "[RepositoryName: " + t + "]";
        }
    }
    
    private static int SearchCounter = 0;
    
    private class SearchThread extends Thread {
        public final Widget mResultPane;
        
        private final org.osid.repository.Repository mRepository;
        private final String mSearchString;
        
        private java.io.Serializable mSearchCriteria;
        private org.osid.shared.Type mSearchType;
        private org.osid.shared.Properties mSearchProperties;
        
        private final StatusLabel mStatusLabel;
        private final String mRepositoryName;
        
        public SearchThread(org.osid.repository.Repository r,
                String searchString,
                Serializable searchCriteria,
                org.osid.shared.Type searchType,
                org.osid.shared.Properties searchProperties)
                throws org.osid.repository.RepositoryException {
            super("Search" + (SearchCounter++) + " " + searchString + " in " + repositoryName(r));
            setDaemon(true);
            
            mRepository = r;
            mSearchString = searchString;
            mSearchCriteria = searchCriteria;
            mSearchType = searchType;
            mSearchProperties = searchProperties;
            
            mRepositoryName = repositoryName(r);
            
            //If the naming convention of this were to change, note there would
            //need to be a change in WidgetStack to properly color code the widget.
            mResultPane = new Widget("Searching " + mRepositoryName);
            
            mStatusLabel = new StatusLabel("Searching for " + mSearchString + " ...", false);
            mResultPane.add(mStatusLabel);
            
            if (DEBUG.DR) Log.debug("created search thread for: " + mRepositoryName + " \t" + mRepository);
        }
        
        public void run() {
            
            if (stopped())
                return;
            
            if (DEBUG.DR) Log.debug("RUN KICKED OFF");

            // TODO: all the swing access should be happening on the EDT for
            // absolute thread safety.  Refactor using SwingWorker.
            
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
                Util.printStackTrace(t);
                if (stopped())
                    return;
                final JTextArea textArea;
                if (DEBUG.Enabled) {
                    textArea = new JTextArea(mRepositoryName + ": Search Error: " + t);
                } else {
                    String msg = translateRepositoryException(t.getLocalizedMessage());
                    textArea = new JTextArea(mRepositoryName + ": Search Error: " + msg);
                }
                
                textArea.setBorder(new EmptyBorder(4,22,6,0));
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setOpaque(false);
                	
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    mResultPane.setTitle("Results: " + mRepositoryName);
                    mResultPane.removeAll();
                    mResultPane.add(textArea);
                }});
            }

            if (stopped()) {
                if (DEBUG.DR) Log.debug("DELAYED STOP; server returned, run completed.");
                return;
            }
            
            mSearchThreads.remove(this);
            if (DEBUG.DR) Log.debug("RUN COMPLETED, stillActive=" + mSearchThreads.size());
            
            // must call revalidate because we're coming from another thread:
            //mResultPane.revalidate();

            if (mSearchThreads.size() == 0) {
                // If we were stopped, the DefaultQueryEditor will have handled
                // calling completeSearch to restore the state of the "Search" button.
                if (DEBUG.DR) Log.debug("ALL SEARCHES COMPLETED for \"" + mSearchCriteria + "\"");
                if (queryEditor instanceof edu.tufts.vue.ui.DefaultQueryEditor)
                    ((edu.tufts.vue.ui.DefaultQueryEditor)queryEditor).completeSearch();
            }
        }
        
        private String translateRepositoryException(String msg)
        {
        	String s;
        	if (msg.equals(org.osid.repository.RepositoryException.OPERATION_FAILED))
        	{
        		s = VueResources.getString("repositoryexception.operationfailed"); 
        	}
        	else if (msg.equals(org.osid.repository.RepositoryException.PERMISSION_DENIED))
        	{
        		s = VueResources.getString("repositoryexception.permissiondenied");
        	}
        	else if (msg.equals(org.osid.repository.RepositoryException.CONFIGURATION_ERROR))
        	{
        		s = VueResources.getString("repositoryexception.configurationerror");
        	}
        	else
        	{
        		s = VueResources.getString("repositoryexception.genericmsg");
        	}
        	
        	return s;
        }
        // As we create a new Widget for the output of every search, in terms of a new
        // search replacing a still running search, we're already safe UI wise even if
        // we never interrupted a search, but we might as well be careful about it / not
        // waste cycles, and it's nice if the user can abort if desired.
        
        private boolean stopped() {
            if (isInterrupted()) {
                if (DEBUG.DR) Log.debug("STOPPING");
                return true;
            } else
                return false;
        }

        public void interrupt() {
            if (DEBUG.DR) Log.debug("INTERRUPTED " + this);
            super.interrupt();
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                mResultPane.setTitle(mRepositoryName + " (Stopped)");
                mStatusLabel.removeIcon();
                mStatusLabel.setText("Search stopped.");
            }});
        }
        
        private void adjustQuery()
        throws org.osid.repository.RepositoryException {
            //if (DEBUG.DR) Log.debug("checking for query adjustment");
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
                if (DEBUG.DR) Log.debug("adjusted query");
            }
            //if (DEBUG.DR) Log.debug("done checking for query adjustment");
        }
        
        private void processResultsAndDisplay(org.osid.repository.AssetIterator assetIterator)
            throws org.osid.repository.RepositoryException
        {
            if (stopped())
                return;
            
            if (DEBUG.DR) Log.debug("processing AssetIterator: " + Util.tags(assetIterator));

            final java.util.List resourceList = new java.util.ArrayList();
            
            final int maxResult = 10000;
            int resultCount = 0;
            if (assetIterator != null) {
                try {
                    while (assetIterator.hasNextAsset()) {
                        org.osid.repository.Asset asset = assetIterator.nextAsset();
                        if (++resultCount > maxResult)
                            continue;
                        if (asset == null) {
                            Log.warn("null asset in " + mRepositoryName + "; " + assetIterator);
                            continue;
                        }
                        try {
                            resourceList.add(Resource.instance(mRepository, asset, DataSourceViewer.this.context));
                        } catch (Throwable t) {
                            Log.warn("Failed to create resource for asset: " + Util.tags(asset), t);
                        }
                    }
                } catch (Throwable t) {
                    if (resourceList.size() < 1) {
                        if (t instanceof RepositoryException)
                            throw (RepositoryException) t;
                        else
                            throw new RuntimeException("processing assets for " + mRepositoryName, t);
                    } else {
                        // we have at least one result: dump exception and continue
                        Util.printStackTrace(t, "processing asset iterator for " + mRepositoryName);
                    }
                }
            
                if (DEBUG.DR) Log.debug("done processing AssetIterator; count=" + resultCount);
            }
            
            String name = "Results: " + mRepositoryName;
            
            if (DEBUG.DR) {
                if (resultCount > maxResult)
                    Log.debug(name + "; returned a total of " + resultCount + " matches");
                Log.debug(name + "; " + resourceList.size() + " results");
            }
            
            if (resourceList.size() > 0)
                name += " (" + resourceList.size() + ")";
            
            if (stopped())
                return;

            final String title = name;
            final JComponent results;
            
            if (resourceList.size() == 0) {
                if (assetIterator == null)
                    results = new StatusLabel("Empty results for " + mSearchString, false, false);
                else
                    results = new StatusLabel("No results for " + mSearchString, false, false);
            } else {
                results = new ResourceList(resourceList, title);
            }
            
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                mResultPane.setTitle(title);
                mResultPane.removeAll();
                mResultPane.add(results);
            }});
            
        }
    }
    
    
    private synchronized void performParallelSearchesAndDisplayResults()
    {
//         if (DEBUG.DR) {
//             synchronized (System.out) {
//                 System.out.println("Current search threads:");
//                 for (Thread t : mSearchThreads)
//                     System.out.println("\t" + t);
//                 //mSearchThreadGroup.list();
//             }
//         }
//         //mSearchThreadGroup.interrupt(); 
        
        final String searchString = "\"" + queryEditor.getSearchDisplayName() + "\"";
        final WidgetStack resultsStack = new WidgetStack("searchResults " + searchString);
        final org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
        final java.io.Serializable searchCriteria = queryEditor.getCriteria();
        final org.osid.shared.Type searchType = queryEditor.getSearchType();
        final org.osid.shared.Properties searchProperties = queryEditor.getProperties();

        mSearchThreads.clear();
        
        if (DEBUG.DR) {
            Log.debug("Searching criteria [" + searchString + "] in selected repositories."
                    + "\n\tsearchType=" + searchType
                    + "\n\tsearchProps=" + searchProperties);
        }
        
        for (int i = 0; i < repositories.length; i++) {
            final org.osid.repository.Repository repository = repositories[i];

            if (repository == null) {
                Util.printStackTrace("null repository #" + i + ": skipping search");
                continue;
            }
            
            SearchThread searchThread = null;
            try {
                searchThread = new SearchThread(repository, searchString,
                        searchCriteria, searchType, searchProperties);
            } catch (Throwable t) {
                Util.printStackTrace(t, "Failed to create search in " + repository);
                if (DEBUG.Enabled)
                    VueUtil.alert("Search Error", t);
                else
                    VueUtil.alert(t.getMessage(), "Search Error");
            }
            
            mSearchThreads.add(searchThread);
            resultsStack.addPane(searchThread.mResultPane, 0f);
        }
        
        DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);
        
        //-----------------------------------------------------------------------------
        // KICK OFF THE SEARCH THREADS
        //-----------------------------------------------------------------------------
        synchronized (mSearchThreads) {
            for (Thread t : mSearchThreads)
                t.start();
        }
    }
    
//     private synchronized void performFederatedSearchAndDisplayResults()
//     throws org.osid.repository.RepositoryException,
//             org.osid.shared.SharedException {
//         //final String dockTitle = "Search Results for \"" + queryEditor.getSearchDisplayName() + "\"";
//         final String searchString = "\"" + queryEditor.getSearchDisplayName() + "\"";
        
//         /*
//           Store our results since we will fill a panel with each repository's results and one with all.
//           We can't get the iterator contents again, without re-doing the search.
         
//           We know the repositories we searched.  Some may have returned results, others may not.  We will
//           make a vector for each set of results with a parallel vector of repository ids.
//          */
        
//         final java.util.List resultList = new java.util.ArrayList();
//         final java.util.List dataSourceIdStringList = new java.util.ArrayList();
//         final java.util.List repositoryDisplayNameList = new java.util.ArrayList();
        
//         org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
//         edu.tufts.vue.dsm.DataSource[] dataSources = sourcesAndTypesManager.getDataSourcesToSearch(); // will be same length
        
//         final WidgetStack resultsStack = new WidgetStack("searchResults " + searchString);
//         final Widget[] resultPanes = new Widget[repositories.length];
        
//         for (int i = 0; i < repositories.length; i++) {
//             org.osid.repository.Repository r = repositories[i];
//             if (DEBUG.DR) Log.debug("to search: " + r.getDisplayName() + " \t" + r);
            
//             dataSourceIdStringList.add(dataSources[i].getId().getIdString());
//             repositoryDisplayNameList.add(r.getDisplayName());
//             resultList.add(new java.util.ArrayList());
            
//             resultPanes[i] = new Widget("Searching " + r.getDisplayName());
//             resultPanes[i].add(new StatusLabel("Searching for " + searchString + " ...", true));
//             resultsStack.addPane(resultPanes[i], 0f);
//         }
        
//         DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);
        
//         // get our search results
//         java.io.Serializable searchCriteria = queryEditor.getCriteria();
//         if (DEBUG.DR) {
//             Log.debug("Searching criteria [" + searchCriteria + "] in selected repositories. SearchProps=" + queryEditor.getProperties());
//         }
//         org.osid.shared.Properties searchProperties = queryEditor.getProperties();
        
//         edu.tufts.vue.fsm.ResultSetManager resultSetManager
//                 = federatedSearchManager.getResultSetManager(searchCriteria,
//                 queryEditor.getSearchType(),
//                 searchProperties);
//         if (DEBUG.DR) Log.debug("got result set manager " + resultSetManager);
        
//         for (int i=0; i < dataSources.length; i++) {
//             org.osid.repository.AssetIterator assetIterator = resultSetManager.getAssets(dataSources[i].getId().getIdString());
//             int counter = 0;
//             while (assetIterator.hasNextAsset() && (counter <= 100)) {
//                 org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
//                 counter++;
//                 String dataSourceIdString = dataSources[i].getId().getIdString();
//                 int index = dataSourceIdStringList.indexOf(dataSourceIdString);
//                 java.util.List v = (java.util.List) resultList.get(index);
                
//                 // TODO: Resources eventually want to be atomic, so a factory
//                 // should be queried for a resource based on the asset.
//                 Osid2AssetResource resource = new Osid2AssetResource(nextAsset, this.context);
//                 v.add(resource);
//             }
//         }
        
//         // Display the results in the result panes
//         for (int i = 0; i < repositories.length; i++) {
//             java.util.List resourceList = (java.util.List) resultList.get(i);
//             String name = "Results: " + (String) repositoryDisplayNameList.get(i);
//             if (DEBUG.DR) Log.debug(name + ": " + resourceList.size() + " results");
            
//             if (resourceList.size() > 0)
//                 name += " (" + resourceList.size() + ")";
            
//             resultPanes[i].setTitle(name);
//             resultPanes[i].removeAll();
            
//             if (resourceList.size() == 0) {
//                 //resultsStack.addPane(name, new JLabel("  No results"), 0f);
//                 // there might have been an exception
//                 String message = resultSetManager.getExceptionMessage(i);
//                 if (message != null) {
//                     resultPanes[i].add(new StatusLabel(message, false));
//                 } else {
//                     resultPanes[i].add(new StatusLabel("No results for " + searchString, false));
//                 }
//             } else {
//                 resultPanes[i].add(new ResourceList(resourceList));
//             }
//         }
//     }
    
    private PropertyMap buildPropertyMap(edu.tufts.vue.dsm.DataSource dataSource) {
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
            String supportsUpd = dataSource.supportsUpdate() ? "The Resource Supports Updating" : "The Resource Is Read Only";
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

    private static WidgetStack editInfoStack; // static hack: is needed before this class is constructed
    private static final JLabel NoConfig = new JLabel("No Configuration", JLabel.CENTER);
    
    private final MetaDataPane configMetaData = new MetaDataPane(true);
    private Object loadedDataSource;
    
    static void initUI() {
        editInfoDockWindow = buildConfigWindow();
    }
    
    private static DockWindow buildConfigWindow() {
        try {
            return _buildWindow();
        } catch (Throwable t) {
            Log.error("buildConfigWindow", t);
        }
        return null;
    }
        
    private void positionEditInfoWindow()
    {
    	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if ((DRB.dockWindow.getX() + DRB.dockWindow.getWidth() + editInfoDockWindow.getWidth()) < screenSize.getWidth())
            editInfoDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                                           DRB.dockWindow.getY());
        else
            editInfoDockWindow.setLocation(DRB.dockWindow.getX() - editInfoDockWindow.getWidth(),
                                           DRB.dockWindow.getY());
    }
    
    private void displayEditOrInfo(edu.tufts.vue.dsm.DataSource ds) {
        if (DEBUG.DR) Log.debug("DISPLAY " + Util.tags(ds));
        if (!editInfoDockWindow.isVisible())
            positionEditInfoWindow();
        refreshEditInfo(ds, true);
        editInfoDockWindow.setVisible(true);
        editInfoDockWindow.raise();
    }
    
    private void displayEditOrInfo(DataSource ds) {
        if (DEBUG.DR) Log.debug("DISPLAY " + Util.tags(ds));
        if (!editInfoDockWindow.isVisible())
            positionEditInfoWindow();
        refreshEditInfo(ds, true);     
        editInfoDockWindow.setVisible(true);
        editInfoDockWindow.raise();
    }
    
    // TODO: Dock title always "Resource: name", Configuration widget title
    // always "Configuration: <type>", e.g., OSID, LocalFileDataSource, RSSDataSource, etc.


    private static DockWindow _buildWindow() {

        final DockWindow dw = GUI.createDockWindow("Resource");
        
        editInfoStack = new WidgetStack();
        //editInfoStack.addPane("startup", new javax.swing.JLabel("config init"));
        editInfoStack.setMinimumSize(new Dimension(300,300));
        dw.setContent(editInfoStack);

        if (DEBUG.Enabled) {
            //editInfoStack.setMinimumSize(new Dimension(400,600));
            dw.setSize(500,800);
        } else {
            dw.setWidth(300);
            dw.setHeight(500);
        }

        
        
        // We don't have DRB yet to set location.
        return dw;
    }

    private void refreshEditInfo(edu.tufts.vue.dsm.DataSource ds) {
        refreshEditInfo(ds, false);
    }
    private void refreshEditInfo(tufts.vue.DataSource ds) {
        refreshEditInfo(ds, false);
    }

    private void doLoad(Object dataSource, String name) {
        editInfoStack.setTitleItem(name);
        //editInfoDockWindow.invalidate();
        //editInfoDockWindow.repaint();
        loadedDataSource = dataSource;
    }

    private void refreshEditInfo(edu.tufts.vue.dsm.DataSource ds, boolean force) {

        if (ds == loadedDataSource)
            return;

        if (DEBUG.DR) Log.debug("REFRESH " + Util.tags(ds));
            
        if (force || editInfoDockWindow.isVisible()) {
            
            if (DEBUG.DR) Log.debug("LOADING " + Util.tags(ds));
            
            editInfoStack.removeAll();
            
            final String name;
            if (DEBUG.Enabled)
                //name = "Configuration: " + ds.getClass().getName(); // always edu.tufts.vue.dsm.impl.VueDataSource
                name = "Configuration: " + ds.getRepository();
            else
                name = "Configuration";
                
            if (ds.hasConfiguration()) {
                editInfoStack.addPane(name, new EditLibraryPanel(this, ds));
            } else {
                editInfoStack.addPane(name, NoConfig);
                if (DEBUG.Enabled) {
                    ;
                } else {
                    Widget.setExpanded(NoConfig, false);
                }
            }
            
            final PropertyMap dsProps = buildPropertyMap(ds);
            
            configMetaData.loadProperties(dsProps);
            editInfoStack.addPane(configMetaData, 1f);
            
            doLoad(ds, ds.getRepositoryDisplayName());
        }
    }
    
    private void refreshEditInfo(tufts.vue.DataSource ds, boolean force) {

        if (ds == loadedDataSource)
            return;
        
        if (DEBUG.DR) Log.debug("REFRESH " + Util.tags(ds));
        
        if (force || editInfoDockWindow.isVisible()) {

            if (DEBUG.DR) Log.debug("LOADING " + Util.tags(ds));
            
            editInfoStack.removeAll();

            final String name;
            if (DEBUG.Enabled)
                name = "Configuration: " + ds.getClass().getName();
            else
                name = "Configuration: " + ds.getTypeName();

            editInfoStack.addPane(name, new EditLibraryPanel(this, ds), 1f);

            doLoad(ds, ds.getDisplayName());
        }
    }
    
    
    /**
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
    /**
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
    
    public static FavoritesDataSource getDefualtFavoritesDS() {
        DefaultListModel model = dataSourceList.getContents();
        
        try {
            for(int i = 0; i<model.size();i++){
                Object o = model.getElementAt(i);
                if(o instanceof FavoritesDataSource){
                    return (FavoritesDataSource)o;
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
        if (DEBUG.DR) Log.debug("saveDataSourceViewer: found " + size + " dataSources: scanning for local's to save...");
        for (int i = 0; i<size; i++) {
            Object item = dataSourceList.getModel().getElementAt(i);
            if (DEBUG.DR) System.err.print("\tsaveDataSourceViewer: item " + i + " is " + tufts.Util.tag(item) + "[" + item + "]...");
            if (item instanceof DataSource) {
                sDataSources.add((DataSource)item);
                if (DEBUG.DR) System.err.println("saving");
            } else {
                if (DEBUG.DR) System.err.println("skipping");
            }
        }
        try {
            if (DEBUG.DR) Log.debug("saveDataSourceViewer: creating new SaveDataSourceViewer");
            SaveDataSourceViewer sViewer= new SaveDataSourceViewer(sDataSources);
            if (DEBUG.DR) Log.debug("saveDataSourceViewer: marshallMap: saving " + sViewer + " to " + f);
            marshallMap(f,sViewer);
            if (DEBUG.DR) Log.debug("saveDataSourceViewer: saved");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    
    public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
        Marshaller marshaller = null;
        
        try {
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            marshaller.setMapping(tufts.vue.action.ActionUtil.getDefaultMapping());
            if (DEBUG.DR) Log.debug("marshallMap: marshalling " + dataSourceViewer + " to " + file + "...");
            marshaller.marshal(dataSourceViewer);
            if (DEBUG.DR) Log.debug("marshallMap: done marshalling.");
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("DRBrowser.marshallMap " + t.getMessage());
        }
    }
    
    public  SaveDataSourceViewer unMarshallMap(File file)
    throws java.io.IOException,
            org.exolab.castor.xml.MarshalException,
            org.exolab.castor.mapping.MappingException,
            org.exolab.castor.xml.ValidationException {
        Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
        FileReader reader = new FileReader(file);
        SaveDataSourceViewer sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
        reader.close();
        return sviewer;
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
//     private static void out(Object o) {

//         Log.debug(o);
        
// //         System.err.println("DSV "
// //                 + new Long(System.currentTimeMillis()).toString().substring(8)
// //                 + " [" + Thread.currentThread().getName() + "] "
// //                 + (o==null?"null":o.toString()));
//     }
    
}