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

/*
 * DataSourceViewer.java
 *
 * Created on October 15, 2003, 1:03 PM
 */

package tufts.vue;
/**
 * @version $Revision: 1.125 $ / $Date: 2006-05-03 18:56:19 $ / $Author: anoop $ *
 * @author  akumar03
 */

import tufts.vue.gui.VueButton;
import tufts.vue.ui.ResourceList;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;
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

public class DataSourceViewer  extends JPanel implements KeyListener, edu.tufts.vue.fsm.event.SearchListener {
    private static DRBrowser DRB;
    private static Object activeDataSource;
    //private static JPanel resourcesPanel,dataSourcePanel;
    String breakTag = "";
 
    public final static int ADD_MODE = 0;
    public final static int EDIT_MODE = 1;
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT = VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    
    JPopupMenu popup;
    
    AddLibraryDialog addLibraryDialog;
    LibraryUpdateDialog libraryUpdateDialog;
    EditLibraryDialog editLibraryDialog;
    RemoveLibraryDialog removeLibraryDialog;
    GetLibraryInfoDialog getLibraryInfoDialog;
    
    AbstractAction checkForUpdatesAction;
    AbstractAction addLibraryAction;
    AbstractAction editLibraryAction;
    AbstractAction removeLibraryAction;
    AbstractAction getLibraryInfoAction;
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
    
    edu.tufts.vue.dsm.Registry registry;
    org.osid.registry.Provider checked[];
    
    public DataSourceViewer(DRBrowser drBrowser) {
        setLayout(new BorderLayout());
        this.DRB = drBrowser;
        dataSourceList = new DataSourceList(this);
        dataSourceList.addKeyListener(this);
        dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
        edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
        for (int i=0; i < dataSources.length; i++) {
            dataSourceList.getContents().addElement(dataSources[i]);
            dataSources[i].setIncludedInSearch(true);
        }
        loadDefaultDataSources();
        
        federatedSearchManager = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();
        sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
        queryEditor = federatedSearchManager.getQueryEditorForType(new edu.tufts.vue.util.Type("mit.edu","search","keyword"));
        queryEditor.addSearchListener(this);
        
        DRB.searchPane.removeAll();
        DRB.searchPane.add((JPanel) queryEditor, DRBrowser.SEARCH_EDITOR);
        DRB.searchPane.revalidate();
        DRB.searchPane.repaint();
        
        // WORKING: stop using this preview panel?
        //this.previewPanel = previewDockWindow.getWidgetPanel();
        
        //resultSetDockWindow = DRB.searchDock;
        
        setPopup();
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
                Point pt = e.getPoint();
                // see if we are far enough over to the left to be on the checkbox
                if ( (activeDataSource instanceof edu.tufts.vue.dsm.DataSource) && (pt.x <= 40) ) {
                    int index = dataSourceList.locationToIndex(pt);
                    edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceList.getModel().getElementAt(index);
                    boolean included = !ds.isIncludedInSearch();
                    ds.setIncludedInSearch(included);
                    if (DEBUG.DR) out("DataSource " + ds + " [" + ds.getProviderDisplayName() + "] inclusion: " + included);
                    //ds.setIncludedInSearch(!ds.isIncludedInSearch());
                    try {
                        dataSourceManager.save();
                    } catch (Throwable t) {
                        
                    }
                    dataSourceList.repaint();
                    queryEditor.refresh();
                }
                //                if(e.getButton() == e.BUTTON3) {
                //                    popup.show(e.getComponent(), e.getX(), e.getY());
                //                }
            }
        });
        
        if (false) {
            JScrollPane dataJSP = new JScrollPane(dataSourceList);
            dataJSP.setMinimumSize(new Dimension(100,100));
            add(dataJSP);
        } else {
            add(dataSourceList);
        }
        
    }
    
    public static void addDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if  (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else  if (ds instanceof Osid2DataSource) type = 6;
        else if(ds instanceof tufts.artifact.DataSource) type = 7;
        else if(ds instanceof tufts.googleapi.DataSource) type = 8;
        else type = 9;
        
        Vector dataSourceVector = (Vector)allDataSources.get(type);
        dataSourceVector.add(ds);
//        saveDataSourceViewer();
    }
    
    public void deleteDataSource(DataSource ds){
        
        int type;
        
        if (ds instanceof LocalFileDataSource) type = 0;
        else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
        else  if (ds instanceof FedoraDataSource) type = 3;
        else  if (ds instanceof GoogleDataSource) type = 4;
        else  if (ds instanceof OsidDataSource) type = 5;
        else  if (ds instanceof Osid2DataSource) type = 6;
        else if(ds instanceof tufts.artifact.DataSource) type = 6;
        else if(ds instanceof tufts.googleapi.DataSource) type = 7;
        else type = 9;
        
        if(VueUtil.confirm(this,"Are you sure you want to delete DataSource :"+ds.getDisplayName(),"Delete DataSource Confirmation") == JOptionPane.OK_OPTION) {
            Vector dataSourceVector = (Vector)allDataSources.get(type);
            dataSourceVector.removeElement(ds);
        }
        saveDataSourceViewer();
        
    }
    
    public static Object getActiveDataSource() {
        return activeDataSource;
    }
    
    public void setActiveDataSource(DataSource ds){
        if (DEBUG.DR) out("SET ACTIVE DATA SOURCE " + ds);
        this.activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.searchPane, false);
        if (ds instanceof LocalFileDataSource) {
            Widget.setTitle(DRB.browsePane, "Browse: " + ds.getDisplayName());
            Widget.setExpanded(DRB.browsePane, true);
            DRB.browsePane.removeAll();
            DRB.browsePane.add(ds.getResourceViewer());
            //Widget.setHidden(DRB.browsePane, false);
            //Widget.setExpanded(DRB.browsePane, true);
            //DRB.savedResourcesPane.setExpanded(false);
            if (DEBUG.DR) out("Local Filing Selected " + ds) ;
        } else if (ds instanceof FavoritesDataSource) {
            Widget.setExpanded(DRB.browsePane, true);
            DRB.browsePane.removeAll();
            DRB.browsePane.add(ds.getResourceViewer());
            //Widget.setHidden(DRB.browsePane, false);
            //Widget.setExpanded(DRB.browsePane, true);
            if (DEBUG.DR) out("Local Filing Selected " + ds) ;
        } else
            Widget.setExpanded(DRB.browsePane, false);
        
    }
    
    public void setActiveDataSource(edu.tufts.vue.dsm.DataSource ds) {
        this.activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        Widget.setExpanded(DRB.browsePane, false);
        Widget.setExpanded(DRB.searchPane, true);
    }
    
    public static void refreshDataSourcePanel(edu.tufts.vue.dsm.DataSource ds) {
        queryEditor.refresh();
        //TODO: actually replace the whole editor if need be
    }
    
    public void  setPopup() {
        popup = new JPopupMenu();
        checkForUpdatesAction = new AbstractAction("Check For Updates") {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (registry == null) {
                        registry = edu.tufts.vue.dsm.impl.VueRegistry.getInstance();
                    }
                    edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
                    checked = registry.checkRegistryForUpdated(dataSources);
                    if (checked.length == 0) {
                        javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
                                "There are no library updates available at this time",
                                "LIBRARY UPDATE",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        if (libraryUpdateDialog == null) {
                            libraryUpdateDialog = new LibraryUpdateDialog();
                        } else {
                            libraryUpdateDialog.update(LibraryUpdateDialog.CHECK);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        addLibraryAction = new AbstractAction("Add Resources") {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (registry == null) {
                        registry = edu.tufts.vue.dsm.impl.VueRegistry.getInstance();
                    }
                    edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
                    checked = registry.checkRegistryForNew(dataSources);
                    if (checked.length == 0) {
                        javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
                                "There are no new libraries available at this time",
                                "ADD A LIBRARY",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        DataSourceViewer.this.popup.setVisible(false);
                    } else {
                        if (addLibraryDialog == null) {
                            addLibraryDialog = new AddLibraryDialog(dataSourceList);
                        } else {
                            addLibraryDialog.setVisible(true);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        
        editLibraryAction = new AbstractAction("View Properties") {
            public void actionPerformed(ActionEvent e) {
                Object o = dataSourceList.getSelectedValue();
                if (o != null) {
                    // for the moment, we are doing double work to keep old data sources
                    if (o instanceof edu.tufts.vue.dsm.DataSource) {
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                        displayEditOrInfo(ds);
                    }
                }
            }
        };
        
        removeLibraryAction = new AbstractAction("Remove Resource") {
            public void actionPerformed(ActionEvent e) {
                Object o = dataSourceList.getSelectedValue();
                if (o != null) {
                    // for the moment, we are doing double work to keep old data sources
                    if (o instanceof edu.tufts.vue.dsm.DataSource) {
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                        if (javax.swing.JOptionPane.showConfirmDialog(VUE.getDialogParent(),
                                "Do you really want to remove " + ds.getRepositoryDisplayName(),
                                "Remove Library",
                                javax.swing.JOptionPane.OK_CANCEL_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                            dataSourceManager.remove(ds);
                            dataSourceManager.save();
                            dataSourceManager.refresh();
                            dataSourceList.getContents().removeElement(ds);
                        }
                    }
                }
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        
        refreshMenuActions();
    }
    
    private void refreshMenuActions() {
        Object o = dataSourceList.getSelectedValue();
        if (o != null) {
            // for the moment, we are doing double work to keep old data sources
            if (o instanceof edu.tufts.vue.dsm.DataSource) {
                edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                removeLibraryAction.setEnabled(true);
                editLibraryAction.setEnabled(true);
            } else {
                removeLibraryAction.setEnabled(false);
                editLibraryAction.setEnabled(false);
            }
        } else {
            removeLibraryAction.setEnabled(false);
            editLibraryAction.setEnabled(false);
        }
        Widget.setMenuActions(DRB.librariesPanel,
                new Action[] {
            addLibraryAction,
                    checkForUpdatesAction,
                    editLibraryAction,
                    removeLibraryAction,
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
    
    private void loadDefaultDataSources() {
        try {
            String breakTag = "";
            dataSourceList.getContents().addElement(breakTag);
            DataSource ds1 = new FavoritesDataSource("My Saved Content");
            dataSourceList.getContents().addElement(ds1);
            dataSourceList.getContents().addElement(breakTag);
            DataSource ds2 = new LocalFileDataSource("My Computer","");
            dataSourceList.getContents().addElement(ds2);
            // default selection
            dataSourceList.setSelectedValue(ds2,true);
            dataSourceList.getContents().addElement(breakTag);
            DataSource ds4 = new LocalFileDataSource("My Maps","");
            dataSourceList.getContents().addElement(ds4);
            dataSourceList.getContents().addElement(breakTag);
            //            DataSource ds3 = new FedoraDataSource("Tufts Digital Library","dl.tufts.edu", "test","test",8080);
//            addDataSource(ds3);
//            DataSource ds4 = new GoogleDataSource("Tufts Web","http://googlesearch.tufts.edu","tufts01","tufts01");
//            addDataSource(ds4);
//            saveDataSourceViewer();
//            setActiveDataSource(ds1);
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
        
        
        new Thread("VUE-Search") {
            public void run() {
                if (DEBUG.DR || DEBUG.THREAD) out("search thread kicked off");
                try {
                    performSearchAndDisplayResults();
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
    
    private synchronized void performSearchAndDisplayResults()
    throws org.osid.repository.RepositoryException,
            org.osid.shared.SharedException {
        //final String dockTitle = "Search Results for \"" + queryEditor.getSearchDisplayName() + "\"";
        final String searchString = "\"" + queryEditor.getSearchDisplayName() + "\"";
        
        /*
          Store our results since we will fill a panel with each repository's results and one with all.
          We can't get the iterator contents again, without re-doing the search.
         
          We know the repositories we searched.  Some may have returned results, others may not.  We will
          make a vector for each set of results with a parallel vector of repository ids.
         */
        
        final java.util.List resultList = new java.util.ArrayList();
        final java.util.List repositoryIdStringList = new java.util.ArrayList();
        final java.util.List repositoryDisplayNameList = new java.util.ArrayList();
        //final java.util.Vector allResultList = new java.util.Vector();
        
        org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
        
        final WidgetStack resultsStack = new WidgetStack("searchResults " + searchString);
        final Widget[] resultPanes = new Widget[repositories.length];
        
        for (int i = 0; i < repositories.length; i++) {
            org.osid.repository.Repository r = repositories[i];
            if (DEBUG.DR) out("to search: " + r.getDisplayName() + " \t" + r);
            repositoryIdStringList.add(r.getId().getIdString());
            repositoryDisplayNameList.add(r.getDisplayName());
            resultList.add(new java.util.ArrayList());
            
            resultPanes[i] = new Widget("Searching " + r.getDisplayName());
            resultPanes[i].add(new StatusLabel("Searching for " + searchString + " ...", true));
            resultsStack.addPane(resultPanes[i], 0f);
        }
        
        DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);
        
        /*
        if (SearchingLabel == null) {
            SearchingLabel = new JLabel("Searching...", JLabel.CENTER);
            SearchingLabel.setOpaque(false);
        }
         
        DRB.searchPane.add(SearchingLabel, DRBrowser.SEARCH_RESULT);
         
        if (resultSetDockWindow != null) {
            resultSetDockWindow.setTitle(dockTitle);
         
            if (false&&UseSingleScrollPane)
                resultSetTreeJSP.setViewportView(SearchingLabel);
            else
                resultSetDockWindow.setContent(SearchingLabel);
        }
         */
        
        
        // get our search results
        java.io.Serializable searchCriteria = queryEditor.getCriteria();
        if (DEBUG.DR) {
            out("Searching criteria [" + searchCriteria + "] in selected repositories. SearchProps=" + queryEditor.getProperties());
        }
        org.osid.shared.Properties searchProperties = queryEditor.getProperties();
        
        edu.tufts.vue.fsm.ResultSetManager resultSetManager
                = federatedSearchManager.getResultSetManager(searchCriteria,
                searchType,
                searchProperties);
        if (DEBUG.DR) out("got result set manager " + resultSetManager);
        
        
        org.osid.repository.AssetIterator assetIterator = resultSetManager.getAssets();
        while (assetIterator.hasNextAsset()) {
            org.osid.repository.Asset nextAsset = assetIterator.nextAsset();
            String repositoryIdString = nextAsset.getRepository().getIdString();
            int index = repositoryIdStringList.indexOf(repositoryIdString);
            java.util.List v = (java.util.List) resultList.get(index);
            
            // TODO: Resources eventually want to be atomic, so a factory
            // should be queried for a resource based on the asset.
            Osid2AssetResource resource = new Osid2AssetResource(nextAsset, this.context);
            v.add(resource);
            //allResultList.addElement(resource);
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
                resultPanes[i].add(new StatusLabel("No results for " + searchString, false));
            } else {
                resultPanes[i].add(new ResourceList(resourceList));
            }
            
            /*
            JComponent resultSet = new ResourceList(v.iterator());
            if (UseSingleScrollPane) {
                resultsStack.addPane(name, resultSet);
            } else {
                resultsStack.addPane(name, new JScrollPane(resultSet));
            }
             */
        }
        
    }
    
    /*
    // Do this on AWT thread to make sure we
    // don't collide with anything going on there.
    GUI.invokeAfterAWT(new Runnable() { public void run() { displaySearchResults(resultsStack, dockTitle); } });
    private void XdisplaySearchResults(WidgetStack resultsStack, String dockTitle) {
        if (DEBUG.DR || DEBUG.THREAD) out("diplaying results: " + dockTitle);
     
        DRB.searchPane.add(resultsStack, DRBrowser.SEARCH_RESULT);
        //Widget.setExpanded(DRB.searchPane, true);
     
        if (resultSetDockWindow == null) {
            if (UseSingleScrollPane) {
                resultSetTreeJSP = new javax.swing.JScrollPane(resultsStack);
                resultSetTreeJSP.setBorder(null);
                resultSetDockWindow = GUI.createDockWindow(dockTitle, resultSetTreeJSP);
                resultSetDockWindow.setHeight(575);
            } else {
                resultSetDockWindow = GUI.createDockWindow(dockTitle, resultsStack);
            }
            if (false)
                // put results to right of data sources
                resultSetDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                                                DRB.dockWindow.getY());
            else {
                // put results below search panel
                DockWindow searchDock = (DockWindow) SwingUtilities.getWindowAncestor(DRB.searchPane);
                searchDock.addChild(resultSetDockWindow);
            }
     
        } else {
     
            if (UseSingleScrollPane) {
                resultSetTreeJSP.setViewportView(resultsStack);
                resultSetDockWindow.setContent(resultSetTreeJSP);
            } else
                resultSetDockWindow.setContent(resultsStack);
        }
        resultSetDockWindow.setVisible(true);
    }
     */
    
    private void displayEditOrInfo(edu.tufts.vue.dsm.DataSource ds) {
        refreshEditInfo(ds);
        editInfoDockWindow.setVisible(true);
    }
    
    private void refreshEditInfo(edu.tufts.vue.dsm.DataSource ds) {
        String dockTitle = "Library Information for " + ds.getRepositoryDisplayName();
        final WidgetStack editInfoStack = new WidgetStack();
        
        if (editInfoDockWindow == null) {
            if (ds.hasConfiguration()) {
                editInfoStack.addPane("Configuration",new javax.swing.JScrollPane(new EditLibraryPanel(ds)));
            } else {
                JPanel jc = new JPanel();
                jc.add(new JLabel("None"));
                editInfoStack.addPane("Configuration",jc);
                Widget.setExpanded(jc,false);
            }
            
            JPanel descriptionPanel = new JPanel();
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
            gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gbConstraints.insets = new java.awt.Insets(10,12,10,12);
            descriptionPanel.setLayout(gbLayout);
            descriptionPanel.add(new LibraryInfoPanel(ds),gbConstraints);
            editInfoStack.addPane("Description",new javax.swing.JScrollPane(descriptionPanel));
            
            editInfoDockWindow = GUI.createDockWindow(dockTitle, editInfoStack);
            editInfoDockWindow.setWidth(300);
            editInfoDockWindow.setHeight(300);
            editInfoDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                    DRB.dockWindow.getY());
            editInfoDockWindow.setTitle(dockTitle);
        } else if (editInfoDockWindow.isVisible()) {
            if (ds.hasConfiguration()) {
                editInfoStack.addPane("Configuration", new javax.swing.JScrollPane(new EditLibraryPanel(ds)));
            } else {
                JPanel jc = new JPanel();
                jc.add(new JLabel("None"));
                editInfoStack.addPane("Configuration",jc);
                Widget.setExpanded(jc,false);
            }
            JPanel descriptionPanel = new JPanel();
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
            gbConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gbConstraints.insets = new java.awt.Insets(10,12,10,12);
            descriptionPanel.setLayout(gbLayout);
            descriptionPanel.add(new LibraryInfoPanel(ds),gbConstraints);
            editInfoStack.addPane("Description",new javax.swing.JScrollPane(descriptionPanel));
            
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
        /**
         * try {
         * mDataSources.add(new FedoraDataSource("Tufts Digital Library","snowflake.lib.tufts.edu","test","test"));
         *
         * } catch (Exception ex) {
         * System.out.println("Datasources can't be loaded");
         * }
         **/
        
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
    
    public static void saveDataSourceViewer() {
        if (dataSourceList == null) {
            System.err.println("DataSourceViewer: No dataSourceList to save.");
            return;
        }
        int size = dataSourceList.getModel().getSize();
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        Vector sDataSources = new Vector();
        for (int i = 0; i< size; i++) {
            Object item = dataSourceList.getModel().getElementAt(i);
            if (DEBUG.DR) System.out.println("saveDataSourceViewer: item " + i + " is " + item.getClass().getName() + "[" + item + "]");
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
