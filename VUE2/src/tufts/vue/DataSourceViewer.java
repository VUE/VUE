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
 * @version $Revision: 1.102 $ / $Date: 2006-03-25 02:20:57 $ / $Author: jeff $ *
 * @author  akumar03
 */

import tufts.vue.gui.VueButton;


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

public class DataSourceViewer  extends JPanel implements KeyListener, edu.tufts.vue.fsm.event.SearchListener
{
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
	javax.swing.JScrollPane resultSetTreeJSP;
	JPanel previewPanel = null;
	
	static edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
	static edu.tufts.vue.dsm.DataSource dataSources[];
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

    public DataSourceViewer(DRBrowser drBrowser)
    {
        /*
          librariesDockWindow = librariesDWindow;
          searchDockWindow = searchDWindow;
          searchDockWindow.setMenuActions(new Action[0]);
          browseDockWindow = browseDWindow;
          savedResourcesDockWindow = savedResourcesDWindow;
          previewDockWindow = previewDWindow;
          resourcesPanel = new JPanel();
        */
		
        setLayout(new BorderLayout());
        //setBorder(new TitledBorder("Libraries"));
        this.DRB = drBrowser;
				
        dataSourceList = new DataSourceList(this);
        dataSourceList.addKeyListener(this);
                
        dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
        edu.tufts.vue.dsm.DataSource dataSources[] = dataSourceManager.getDataSources();
        for (int i=0; i < dataSources.length; i++) {
            dataSourceList.getContents().addElement(dataSources[i]);
        }
        loadDefaultDataSources();
				
        federatedSearchManager = edu.tufts.vue.fsm.impl.VueFederatedSearchManager.getInstance();		
        sourcesAndTypesManager = edu.tufts.vue.fsm.impl.VueSourcesAndTypesManager.getInstance();
        queryEditor = federatedSearchManager.getQueryEditorForType(new edu.tufts.vue.util.Type("mit.edu","search","keyword"));
        queryEditor.addSearchListener(this);

        if (false) {
            //((JPanel)queryEditor).setBackground(VueResources.getColor("FFFFFF")); // not in properties file
            ((JPanel)queryEditor).setSize(new Dimension(100,90));
            ((JPanel)queryEditor).setPreferredSize(new Dimension(100,90));
            ((JPanel)queryEditor).setMinimumSize(new Dimension(100,90));
        }

        DRB.searchPane.removeAll();
        DRB.searchPane.add((JPanel) queryEditor);
		
        // WORKING: stop using this preview panel?
        //this.previewPanel = previewDockWindow.getWidgetPanel();

        initResultSetDockWindow();
		
        dataSourceList.clearSelection();
		
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
                        }
                        else
                            {
                                int index = ((JList)e.getSource()).getSelectedIndex();
                                o = dataSourceList.getContents().getElementAt(index-1);
                                if (o instanceof tufts.vue.DataSource) {
                                    DataSource ds = (DataSource)o;
                                    DataSourceViewer.this.setActiveDataSource(ds);
                                } else if (o instanceof edu.tufts.vue.dsm.DataSource) {
                                    edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
                                    DataSourceViewer.this.setActiveDataSource(ds);
                                }
                            }
                    }
                }}
            );

        dataSourceList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Point pt = e.getPoint();
                    // see if we are far enough over to the left to be on the checkbox
                    if ( (activeDataSource instanceof edu.tufts.vue.dsm.DataSource) && (pt.x <= 20) ) {
                        int index = dataSourceList.locationToIndex(pt);
                        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)dataSourceList.getModel().getElementAt(index);
                        boolean included = !ds.isIncludedInSearch();
                        ds.setIncludedInSearch(included);
                        if (DEBUG.DR) out("DataSource " + ds + " [" + ds.getProviderDisplayName() + "] inclusion: " + included); 
                        //ds.setIncludedInSearch(!ds.isIncludedInSearch());
                        dataSourceManager.save();
                        dataSourceList.repaint();
                        queryEditor.refresh();
                    }
                    //                if(e.getButton() == e.BUTTON3) {
                    //                    popup.show(e.getComponent(), e.getX(), e.getY());
                    //                }
                }
            });

/*        
        // GRID: optionsConditionButton
        optionButton.setBackground(this.getBackground());
        optionButton.setToolTipText("Library Options");
        
        optionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				popup.setLocation(optionButton.getLocation().x+70,
								  optionButton.getLocation().y+70);
                popup.setVisible(true);
				popup.repaint();
				popup.validate();
            }
        });
               
        JButton refreshButton=new VueButton("refresh");
        
        refreshButton.setBackground(this.getBackground());
        refreshButton.setToolTipText("Refresh data sources");
        
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				try {
                    activeDataSource.setResourceViewer();
                } catch(Exception ex){
                    if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
                }
                refreshDataSourceList();
                
            }
        });
        
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("This panel lists data sources currently availabe to VUE. Use the data source panel buttons to edit, delete  or create new data sources");
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
        
        
        topPanel.add(optionButton);
//        topPanel.add(deleteButton);
//        topPanel.add(refreshButton);
        topPanel.add(questionLabel);
*/
        
        if (true) {

            // no scroll pane: always show's everything & sizes to fit
            add(dataSourceList);

        } else {

            JScrollPane dataJSP = new JScrollPane(dataSourceList);
            dataJSP.setMinimumSize(new Dimension(100,100));
            add(dataJSP);
            
            /*
            dataSourcePanel = new JPanel(new BorderLayout());
            //dataSourcePanel.add(topPanel,BorderLayout.NORTH);
            
            JScrollPane dataJSP = new JScrollPane(dataSourceList);
            dataSourcePanel.add(dataJSP,BorderLayout.CENTER);
            add(dataSourcePanel,BorderLayout.CENTER);
            //drBrowser.add(resourcesPanel,BorderLayout.CENTER);
            */
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
//        refreshDataSourceList();
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
    
/*
	public static void refreshDataSourceList(){
        int i =0; Vector dsVector;
        String breakTag = "";
        int NOOFTYPES = 8;
        if (!(dataSourceList.getContents().isEmpty()))dataSourceList.getContents().clear();
        for (i = 0; i < NOOFTYPES; i++){
            dsVector = (Vector)allDataSources.get(i);
            if (!dsVector.isEmpty()){
                int j = 0;
                for(j = 0; j < dsVector.size(); j++){
                    dataSourceList.getContents().addElement(dsVector.get(j));
                }
                boolean breakNeeded = false; int typeCount = i+1;
                while ((!breakNeeded) && (typeCount < NOOFTYPES)){
                    if (!((Vector)allDataSources.get(i)).isEmpty())breakNeeded = true;
                    typeCount++;
                }
                if (breakNeeded) dataSourceList.getContents().addElement(breakTag);
            }
        }
        dataSourceList.setSelectedValue(getActiveDataSource(),true);
        dataSourceList.validate();
    }
*/    
    
    public static Object getActiveDataSource() {
        return activeDataSource;
    }
	
    public void setActiveDataSource(DataSource ds){
        
        this.activeDataSource = ds;
        
        //        refreshDataSourcePanel(ds);
        
        dataSourceList.setSelectedValue(ds,true);
        
        DRB.searchPane.setExpanded(false);
        if (ds instanceof LocalFileDataSource) {
            DRB.browsePane.setExpanded(true);
            //DRB.savedResourcesPane.setExpanded(false);
        } else if (ds instanceof FavoritesDataSource) {
            //DRB.savedResourcesPane.setExpanded(true);
            DRB.browsePane.setExpanded(true);
        }
        
    }

    public void setActiveDataSource(edu.tufts.vue.dsm.DataSource ds)
    {        
        this.activeDataSource = ds;
        
        //        refreshDataSourcePanel(ds);
        
        dataSourceList.setSelectedValue(ds,true);
        
        DRB.searchPane.setExpanded(true);
        DRB.browsePane.setExpanded(false);
    }

    public static void refreshDataSourcePanel(edu.tufts.vue.dsm.DataSource ds)
	{
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
        addLibraryAction = new AbstractAction("Add Library") {
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
		
        editLibraryAction = new AbstractAction("Edit Library") {
            public void actionPerformed(ActionEvent e) {
				javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
														  "Under Construction",
														  "EDIT LIBRARY",
														  javax.swing.JOptionPane.INFORMATION_MESSAGE);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        removeLibraryAction = new AbstractAction("Remove Library") {
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
		
			
		/*
		 We only provide data source information for data sources.  We look at the current selection, which is
		 either a data source, and "old" data source, or a divider line.
		 */
        getLibraryInfoAction = new AbstractAction("Get Library Info") {
            public void actionPerformed(ActionEvent e) {
				Object o = dataSourceList.getSelectedValue();
				if (o != null) {
					// for the moment, we are doing double work to keep old data sources
					if (o instanceof edu.tufts.vue.dsm.DataSource) {
						edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
						getLibraryInfoDialog = new GetLibraryInfoDialog(ds);
						getLibraryInfoDialog.setVisible(true);
					} else if (o instanceof tufts.vue.DataSource) {
						javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
																  "There is no information available for this library",
																  "INFO",
																  javax.swing.JOptionPane.INFORMATION_MESSAGE);
					}
					else
					{
						int index = dataSourceList.getSelectedIndex();
						o = dataSourceList.getContents().getElementAt(index-1);
						if (o instanceof edu.tufts.vue.dsm.DataSource) {
							edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)o;
							getLibraryInfoDialog = new GetLibraryInfoDialog(ds);
							getLibraryInfoDialog.setVisible(true);
						} else if (o instanceof edu.tufts.vue.dsm.DataSource) {
							javax.swing.JOptionPane.showMessageDialog(VUE.getDialogParent(),
																	  "There is no information available for this library",
																	  "INFO",
																	  javax.swing.JOptionPane.INFORMATION_MESSAGE);
						}
					}
				}
				DataSourceViewer.this.popup.setVisible(false);
			}
		};
        

        /*
        popup.add(checkForUpdatesAction);
        popup.add(addLibraryAction);
        popup.add(editLibraryAction);
        popup.add(removeLibraryAction);
        popup.addSeparator();
        popup.add(getLibraryInfoAction);
        */
        /*
        Action actions[] = new Action[4];
        actions[0] = checkForUpdatesAction;
        actions[1] = addLibraryAction;
        actions[2] = editLibraryAction;
        actions[3] = removeLibraryAction;
        librariesDockWindow.setMenuActions(actions);
        */

        Widget.setMenuActions(DRB.librariesPanel,
                              new Action[] {
                                  checkForUpdatesAction,
                                  addLibraryAction,
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
    
/*
 public void showAddEditWindow(int mode) {
        if ((addEditDialog == null)  || true) { // always true, need to work for cases where case where the dialog already exists
            if (DEBUG.DR) System.out.println("Creating new addEditDialog...");
            addEditDialog = new AddEditDataSourceDialog();
            if (DEBUG.DR) System.out.println("Created new addEditDialog: " + addEditDialog + "; showing...");
            addEditDialog.show(mode);
            if (DEBUG.DR) System.out.println("Showed new addEditDialog: " + addEditDialog);
        }
    }
 */   
    
/*    
    public void loadDataSources(){
        
        Vector dataSource0 = new Vector();
        Vector dataSource1 = new Vector();
        Vector dataSource2 = new Vector();
        Vector dataSource3 = new Vector();
        Vector dataSource4 = new Vector();
        Vector dataSource5 = new Vector();
        Vector dataSource6 = new Vector();
        Vector dataSource7 = new Vector();
        
        allDataSources.add(dataSource0);
        allDataSources.add(dataSource1);
        allDataSources.add(dataSource2);
        allDataSources.add(dataSource3);
        allDataSources.add(dataSource4);
        allDataSources.add(dataSource5);
        allDataSources.add(dataSource6);
        allDataSources.add(dataSource7);
        
        boolean init = true;
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        
        int type;
        try{
            SaveDataSourceViewer rViewer = unMarshallMap(f);
            Vector rsources = rViewer.getSaveDataSources();
            while (!(rsources.isEmpty())){
                DataSource ds = (DataSource)rsources.remove(0);
                ds.setResourceViewer();
                System.out.println(ds.getDisplayName()+ds.getClass());
                try {
                    addDataSource(ds);
                    setActiveDataSource(ds);
                } catch(Exception ex) {System.out.println("this is a problem in restoring the datasources");}
            }
            saveDataSourceViewer();
            //refreshDataSourceList();
        }catch (Exception ex) {
            if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
            //VueUtil.alert(null,"Previously saved datasources file does not exist or cannot be read. Adding Default Datasources","Loading Datasources");
            loadDefaultDataSources();
        }
        //refreshDataSourceList();
    }
	*/    
    
    /**
     *If load datasources fails this method is called
     */
    private void loadDefaultDataSources() {
        try {
			String breakTag = "";
			dataSourceList.getContents().addElement(breakTag);
            DataSource ds2 = new FavoritesDataSource("My Favorites");
			dataSourceList.getContents().addElement(ds2);
			dataSourceList.getContents().addElement(breakTag);
            DataSource ds1 = new LocalFileDataSource("My Computer","");
			dataSourceList.getContents().addElement(ds1);
			dataSourceList.getContents().addElement(breakTag);
			//            DataSource ds3 = new FedoraDataSource("Tufts Digital Library","dl.tufts.edu", "test","test",8080);
//            addDataSource(ds3);
//            DataSource ds4 = new GoogleDataSource("Tufts Web","http://googlesearch.tufts.edu","tufts01","tufts01");
//            addDataSource(ds4);
//            DataSource ds5 = new tufts.artifact.DataSource("Artifact");
//            addDataSource(ds5);
//            saveDataSourceViewer();
//            setActiveDataSource(ds1);
        } catch(Exception ex) {
            if(DEBUG.DR) System.out.println("Datasource loading problem ="+ex);
        }
        
    }
	
	private void initResultSetDockWindow()
	{
	}
	
	private ImageIcon getThumbnail(org.osid.repository.Asset asset)
	{
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

    public void searchPerformed(edu.tufts.vue.fsm.event.SearchEvent se)
    {
        // this may take some time, so we change to the wait cursor
        GUI.activateWaitCursor();
		
        try {
            performSearchAndDisplayResults();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
        GUI.clearWaitCursor();
    }

    private void performSearchAndDisplayResults()
        throws org.osid.repository.RepositoryException,
               org.osid.shared.SharedException
    {
        final boolean UseSingleScrollPane = true;
            
        JPanel tmpAllResults = new JPanel(new BorderLayout()); // until splitting out results is working

        WidgetStack resultsStack = new WidgetStack();

        // get our search results
		java.io.Serializable searchCriteria = queryEditor.getCriteria();
        org.osid.shared.Properties searchProperties = queryEditor.getProperties();
		
        if (DEBUG.DR) out("Searching criteria [" + searchCriteria + "]...");
		
		// fill our results stack with individual results and aggregate results		
        org.osid.repository.Repository[] repositories = sourcesAndTypesManager.getRepositoriesToSearch();
        java.util.Vector repositoryIdStringVector = new java.util.Vector();
        java.util.Vector repositoryDisplayNameVector = new java.util.Vector();

		JPanel repositorySpecificResults[] = new JPanel[repositories.length];
		VueDragTree resultSetTrees[] = new VueDragTree[repositories.length];
		java.util.Vector searchResults[] = new java.util.Vector[repositories.length];
	
        for (int i = 0; i < repositories.length; i++) {
			edu.tufts.vue.fsm.ResultSetManager resultSetManager
            = federatedSearchManager.getResultSetManager(searchCriteria,
                                                         searchType,
                                                         searchProperties);		
			if (DEBUG.DR) out("got result set manager " + resultSetManager);
			
            String idStr = repositories[i].getId().getIdString();
			//System.out.println("Found repository " + idStr);
            String name = repositories[i].getDisplayName();
            if (DEBUG.DR) System.out.println("\t#" + i + " " + idStr + " [" + name + "]");
            repositoryIdStringVector.addElement(idStr);
            repositoryDisplayNameVector.addElement(name);

			// we should change the result set manager to return assets for a specific repository, if supplied its id
			searchResults[i] = new java.util.Vector();
			org.osid.repository.AssetIterator assetIterator = resultSetManager.getAssets();
			while (assetIterator.hasNextAsset()) {
				org.osid.repository.Asset a = (org.osid.repository.Asset) assetIterator.nextAsset();
				org.osid.shared.Id rid = a.getRepository();
				//System.out.println("Found asset in repository " + rid.getIdString());
				if (rid.isEqual(repositories[i].getId())) {
					//System.out.println("match");
					searchResults[i].addElement(new Osid2AssetResource(a, this.context));
				}
			}			

			resultSetTrees[i] = new VueDragTree(searchResults[i].iterator(), "Repository Search Results");
			resultSetTrees[i].setRootVisible(false);

			repositorySpecificResults[i] = new JPanel(new BorderLayout());
			if (UseSingleScrollPane)
				repositorySpecificResults[i].add(resultSetTrees[i]);
			else
				repositorySpecificResults[i].add(new JScrollPane(resultSetTrees[i]));

            resultsStack.addPane(name, repositorySpecificResults[i]);
			//System.out.println(searchResults[i].size() + " results for " + name); 
        }

		// now populate results for an aggregation across all repositories
        edu.tufts.vue.fsm.ResultSetManager resultSetManager
            = federatedSearchManager.getResultSetManager(searchCriteria,
                                                         searchType,
                                                         searchProperties);		
        if (DEBUG.DR) out("got result set manager " + resultSetManager);
		
        resultsStack.addPane("All Results", tmpAllResults);
                                                
        java.util.Vector results = new java.util.Vector();
        org.osid.repository.AssetIterator assetIterator = resultSetManager.getAssets();
        while (assetIterator.hasNextAsset()) {
            org.osid.repository.Asset a = (org.osid.repository.Asset) assetIterator.nextAsset();
            org.osid.shared.Id rid = a.getRepository();
            if (DEBUG.DR) out("repository " + rid.getIdString() + " found: " + a + " [" + a.getDisplayName() + "]");
            results.addElement(new Osid2AssetResource(a, this.context));
        }			

        Widget.setTitle(tmpAllResults, "All Results (" + results.size() + ")");
            
        VueDragTree resultSetTree = new VueDragTree(results.iterator(), "Repository Search Results");
        resultSetTree.setRootVisible(false);

        if (UseSingleScrollPane)
            tmpAllResults.add(resultSetTree);
        else
            tmpAllResults.add(new JScrollPane(resultSetTree));

        String dockTitle = "Search Results for \"" + queryEditor.getSearchDisplayName() + "\"";

        if (resultSetDockWindow == null) {
            if (UseSingleScrollPane) {
                resultSetTreeJSP = new javax.swing.JScrollPane(resultsStack);
                resultSetTreeJSP.setBorder(null);
                resultSetDockWindow = GUI.createDockWindow(dockTitle, resultSetTreeJSP);
            } else {
                resultSetDockWindow = GUI.createDockWindow(dockTitle, resultsStack);
            }
            resultSetDockWindow.setLocation(DRB.dockWindow.getX() + DRB.dockWindow.getWidth(),
                                            DRB.dockWindow.getY());
        } else {
            resultSetDockWindow.setTitle(dockTitle);
            if (UseSingleScrollPane) 
                resultSetTreeJSP.setViewportView(resultsStack);
            else
                resultSetDockWindow.setContent(resultsStack);
        }

        resultSetDockWindow.setVisible(true);

        if (false) {
            // if move result stack back under Resources stack, probably want this.
            DRB.searchPane.setExpanded(false);
        }

                        
        /*
        //searchDockWindow.setRolledUp(true);
        
		
          DockWindow resultSetDockWindow = GUI.createDockWindow("Search Results " + queryEditor.getSearchDisplayName(), resultSetTreeJSP);
          resultSetTreeJSP.setPreferredSize(resultSetPanelDimensions);
          if (GUI.isMacAqua()) {
          resultSetTreeJSP.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
          resultSetTreeJSP.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
          }

          //tufts.Util.displayComponent(resultSetTree);
          searchDockWindow.addChild(resultSetDockWindow);
          resultSetDockWindow.setVisible(true);
        */
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
        }
        catch (Throwable t) {
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
        System.err.println("DSV: " + (o==null?"null":o.toString()));
    }
    
    
    
}
