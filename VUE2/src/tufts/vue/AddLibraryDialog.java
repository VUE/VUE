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
/*
 * AddEditDataSourceDialog.java
 * The UI to Add/Edit Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
 * @version $Revision: 1.82 $ / $Date: 2009-03-22 07:18:28 $ / $Author: vaibhav $
 * @author  akumar03
 */
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tufts.vue.ds.XmlDataSource;
import tufts.vue.gui.GUI;
import tufts.vue.gui.SizeRestrictedDialog;
import tufts.vue.gui.VueLabel;
import edu.tufts.vue.rss.RSSDataSource;

public class AddLibraryDialog extends SizeRestrictedDialog implements ListSelectionListener, ActionListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(AddLibraryDialog.class);
    
    JPanel addLibraryPanel = new JPanel();
    JList addLibraryList;
    JTextArea descriptionTextArea;
    DefaultListModel listModel = new DefaultListModel();
    JScrollPane listJsp;
    JScrollPane descriptionJsp;
    
//    JLabel progressBarLabel = new JLabel("Loading Data Sources...");
    
    edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
    edu.tufts.vue.dsm.OsidFactory factory;
    org.osid.provider.Provider checked[];
    java.util.Vector checkedVector = new java.util.Vector();
    JButton addButton = new JButton(VueResources.getString("addLibrary.ok.label"));
    JButton cancelButton = new JButton(VueResources.getString("addLibrary.cancel.label"));
    JPanel buttonPanel = new JPanel();
    DataSourceList dataSourceList;
    DataSource oldDataSource = null;
    edu.tufts.vue.dsm.DataSource newDataSource = null;
    
    private static String MY_COMPUTER = VueResources.getString("addLibrary.mycomputer.label");
    private static String MY_COMPUTER_DESCRIPTION = "Add a browse control for your filesystem.  You can configure where to start the tree.";
    private static String MY_SAVED_CONTENT = "My Saved Content";
    private static String MY_SAVED_CONTENT_DESCRIPTION = "Add a browse control for your saved content.  You can configure a name for this source.";
    private static String DS_FTP = "FTP";
    private static String DS_FTP_DESCRIPTION = "Add a browse control for an FTP site.  You must configure this.";
    private static String DS_RSS = "RSS Feed";
    private static String DS_RSS_DESCRIPTION = "RSS Feeds can be added by selecting this.";
    private static String DS_XML = "XML Data";
    private static String DS_XML_DESCRIPTION = "XML sources can be added by selecting this.";
    
    private static String TITLE = "Add a Resource";
    private static String AVAILABLE = "Resources available:";
    
    private static String LOADING = VueResources.getString("addLibrary.loading.label");
    
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private ProviderListCellRenderer providerListRenderer;
    private Timer timer;
    
    public AddLibraryDialog(DataSourceList dataSourceList) {    	
        super(VUE.getDialogParentAsFrame(),TITLE,true); 
        this.getRootPane().setDefaultButton(addButton);
        this.dataSourceList = dataSourceList;
        
        try {
            factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert(this,VueResources.getString("dialog.error.message"),VueResources.getString("dialog.error.title"));
        }
        
        try {
        	VueLabel helpButton = new VueLabel(VueResources.getImageIcon("addLibrary.helpIcon"));
        	
            helpButton.setToolTipText(VueResources.getString("addLibrary.tooltip"));
            
            String helpText = VueResources.getString("addLibrary.helpText");
            
            if (helpText != null)
                helpButton.setToolTipText(helpText);
            
        	getContentPane().setLayout(new GridBagLayout());
            addLibraryList = new JList(listModel);
            addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
            addLibraryList.addListSelectionListener(this);
            addLibraryList.setFixedCellHeight(25);
            addLibraryList.addListSelectionListener(new ListSelectionListener(){

				public void valueChanged(ListSelectionEvent arg0) {
					Object o = ((JList)arg0.getSource()).getSelectedValue();
					
					
					if (o instanceof String)
					{
						String s =(String)o;
						if (s.equals(LOADING))
						{
							addLibraryList.setSelectedIndex(0);
							addLibraryList.repaint();
						}
					}
					
				}
            	
            });
            providerListRenderer = new ProviderListCellRenderer();
            addLibraryList.setCellRenderer(providerListRenderer);
            
            descriptionTextArea = new JTextArea();
            descriptionTextArea.setEditable(false);
            descriptionTextArea.setMargin(new Insets(4,4,4,4));
            descriptionTextArea.setLineWrap(true);
            descriptionTextArea.setWrapStyleWord(true);                              
                        
            listJsp = new JScrollPane(addLibraryList);
            listJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            listJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            listJsp.setPreferredSize(new Dimension(300,180));
            
            descriptionTextArea.setText(VueResources.getString("addLibraryDailog.textarea"));
            descriptionJsp = new JScrollPane(descriptionTextArea);
            descriptionJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            descriptionJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            descriptionJsp.setPreferredSize(new Dimension(300,180));
            
            addLibraryPanel.setBackground(VueResources.getColor("White"));
            setBackground(VueResources.getColor("White"));
            
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();

            gbConstraints.insets = new java.awt.Insets(2,2,2,2);
            addLibraryPanel.setLayout(gbLayout);
            
            JLabel avail = new JLabel(AVAILABLE);
            JPanel availabilityPanel = new JPanel();            
            availabilityPanel.setLayout(new BorderLayout());
            availabilityPanel.add(avail,BorderLayout.CENTER);
            availabilityPanel.add(helpButton,BorderLayout.EAST);
            

            gbConstraints.gridx = 0;
            gbConstraints.gridy = 0;
            gbConstraints.gridwidth = 1;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weightx=1;
            gbConstraints.weighty=0;
            gbConstraints.insets = new Insets(4,15,4,15);
            
            addLibraryPanel.add(availabilityPanel,gbConstraints);
                                                            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 1;
            gbConstraints.weighty=1;
            addLibraryPanel.add(listJsp,gbConstraints);
 
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 2;
            addLibraryPanel.add(descriptionJsp,gbConstraints);
            
            
            
            buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));            
            buttonPanel.add(Box.createHorizontalGlue());
            addButton.setPreferredSize(cancelButton.getPreferredSize());
            buttonPanel.add(cancelButton);            
            cancelButton.addActionListener(this);
            buttonPanel.add(Box.createHorizontalStrut(6));
            buttonPanel.add(addButton);
            addButton.addActionListener(this);
            
            getRootPane().setDefaultButton(addButton);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 3;
            gbConstraints.weighty=0;            
            gbConstraints.anchor=GridBagConstraints.EAST;
            addLibraryPanel.add(buttonPanel,gbConstraints);           
            
            //buttonPanel.add(progressBarLabel,gbConstraints,0);
  
            getContentPane().add(addLibraryPanel,gbConstraints);//,BorderLayout.CENTER);
            pack();
            setResizable(false);
            setLocation(300,300);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        this.setMinSizeRestriction(this.getWidth(),this.getHeight());        
        
        
        //populate();
        PopulateThread t = new PopulateThread();
        t.start();
        setVisible(true);
    }
    
    public void refresh() 
    {    
        //buttonPanel.add(progressBarLabel,0);
    	PopulateThread t = new PopulateThread();
        t.start();           
    }

    /** apparently, the number of data sources before "LOADING" ? */
    private static final int LIST_PARTITION_CONSTANT = 5;
    
    private void populate() {
        listModel.removeAllElements();
        
       
        
		this.oldDataSource = null;
		this.newDataSource = null;
		descriptionTextArea.setText("");

        try {
       //     GUI.activateWaitCursor();
            if (dataSourceManager == null) {
                dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            }
            
            //listModel.removeAllElements();
            
            //add all data sources we include with VUE
            listModel.addElement(MY_COMPUTER);            
            listModel.addElement(MY_SAVED_CONTENT);         
            listModel.addElement(DS_RSS);
            listModel.addElement(DS_XML);
            listModel.addElement(DS_FTP);
            listModel.addElement(LOADING);
       int ONE_TNTH_SECOND = 100;
            providerListRenderer.invokeWaitingMode(LIST_PARTITION_CONSTANT);
            timer = new Timer(ONE_TNTH_SECOND, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    repaint();
                }});
                timer.start();
                
			//System.out.println("In Add Library Dialog, asking Provider for list of Providers");
			// get what's available
			java.util.Vector providerIdStringVector = new java.util.Vector();
            org.osid.provider.ProviderIterator providerIterator = factory.getProviders();
            providerListRenderer.endWaitingMode();
            listModel.remove(LIST_PARTITION_CONSTANT);
            while (providerIterator.hasNextProvider()) {
                org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				// only latest
				if (nextProvider.needsUpdate() == false) {  
					listModel.addElement(nextProvider);
					checkedVector.addElement(nextProvider);
					providerIdStringVector.addElement(nextProvider.getId().getIdString());
				}
            }
            
			// get what's installed and not available
			providerIterator = factory.getInstalledProviders();
			
            while (providerIterator.hasNextProvider()) {
                org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
				// only latest and non-duplicate
				if ( (nextProvider.needsUpdate() == false) &&
					 (!providerIdStringVector.contains(nextProvider.getId().getIdString())) ) {
					listModel.addElement(nextProvider);
					checkedVector.addElement(nextProvider);
					providerIdStringVector.addElement(nextProvider.getId().getIdString());				
				}
			}
			
            // copy to an array            
            int size = listModel.size()-LIST_PARTITION_CONSTANT;
            checked = new org.osid.provider.Provider[size];
            for (int i=0; i < size; i++) {
                checked[i] = (org.osid.provider.Provider)checkedVector.elementAt(i);
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert(this,t.getMessage(),VueResources.getString("dialog.error.title"));
        } finally {
  //          GUI.clearWaitCursor();
        	 timer.stop();
        //	  buttonPanel.remove(progressBarLabel);
          	  buttonPanel.validate();
          	  buttonPanel.repaint();
        }        
    }
    
    public void valueChanged(ListSelectionEvent lse) {
        int index = ((JList)lse.getSource()).getSelectedIndex();
        if (index != -1) {
            try {
                if (((JList)lse.getSource()).getSelectedValue() instanceof String) {
                    String s = (String)(((JList)lse.getSource()).getSelectedValue());
                    if (s.equals(MY_COMPUTER)) {
                        descriptionTextArea.setText(MY_COMPUTER_DESCRIPTION);
                    } else if (s.equals(MY_SAVED_CONTENT)) {
                        descriptionTextArea.setText(MY_SAVED_CONTENT_DESCRIPTION);
                    } else if (s.equals(DS_FTP)) {
                        descriptionTextArea.setText(DS_FTP_DESCRIPTION);
                    } else if (s.equals(DS_RSS)) {
                        descriptionTextArea.setText(DS_RSS_DESCRIPTION);
                    } else if (s.equals(DS_XML)) {
                        descriptionTextArea.setText(DS_XML_DESCRIPTION);
                    }
                } else {
                    org.osid.provider.Provider p = (org.osid.provider.Provider)(((JList)lse.getSource()).getSelectedValue());
					String desc = p.getDescription();
					if (desc != null) {
						desc = java.net.URLDecoder.decode(desc);
						descriptionTextArea.setText(desc);
					} else {
						descriptionTextArea.setText("");
					}
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
    
    public void add() {
        
        try {
			boolean proceed = true;
            this.oldDataSource = null;
            Object o = addLibraryList.getSelectedValue();
            String xml = null;
            String s = null;
            
            if (o instanceof String) {
                s = (String)o;
                if (s.equals(MY_COMPUTER)) {
                    LocalFileDataSource ds = new LocalFileDataSource(MY_COMPUTER,"");
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Starting path</title><description>The path to start from</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>512</maxChars><ui>8</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    String address = ds.getAddress();
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    this.oldDataSource = ds;
                } else if (s.equals(MY_SAVED_CONTENT)) {
                    FavoritesDataSource ds = new FavoritesDataSource(MY_SAVED_CONTENT);
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    this.oldDataSource = ds;
                } else if (s.equals(DS_FTP)) {
                    RemoteFileDataSource ds = new RemoteFileDataSource();
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>FTP Address</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>256</maxChars><ui>0</ui></field><field><key>username</key><title>Username</title><description>FTP site username</description><default>DEFAULT_USERNAME</default><mandatory>true</mandatory><maxChars>64</maxChars><ui>9</ui></field><field><key>password</key><title>Password</title><description>FTP site password for username</description><default>DEFAULT_PASSWORD</default><mandatory>true</mandatory><maxChars></maxChars><ui>1</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    if (name == null) name = "";
                    String address = ds.getAddress();
                    if (address == null) address = "";
                    String username = ds.getUserName();
                    if (username == null) username = RemoteFileDataSource.ANONYMOUS;
                    String password = ds.getPassword();
                    if (password == null) password = "";
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    xml = xml.replaceFirst("DEFAULT_USERNAME",username);
                    xml = xml.replaceFirst("DEFAULT_PASSWORD",password);
                    this.oldDataSource = ds;
                } else if (s.equals(DS_RSS)) {
                    RSSDataSource ds = new RSSDataSource("", null);
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>RSS Url</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>1000</maxChars><ui>0</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    if (name == null) name = "";
                    String address = ds.getAddress();
                    if (address == null) address = "";
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    this.oldDataSource = ds;
                } else if (s.equals(DS_XML)) {
                    // TODO: get rid of all this duplicate code: e.g., this is copy of RSS case
                    XmlDataSource ds = new XmlDataSource("", null);
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>RSS Url</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>1000</maxChars><ui>8</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    if (name == null) name = "";
                    String address = ds.getAddress();
                    if (address == null) address = "";
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    this.oldDataSource = ds;
                }
            } else {
                org.osid.provider.Provider provider = (org.osid.provider.Provider)o;  
                edu.tufts.vue.dsm.DataSource ds = null;
                // show dialog containing license, if any
                try {
                    if (provider.requestsLicenseAcknowledgement()) {
                        String license = provider.getLicense();
                        if (license != null) {
                            javax.swing.JTextArea area = new javax.swing.JTextArea();
                            area.setLineWrap(true);
                            area.setWrapStyleWord(true);
                            area.setText(license);
                            area.setEditable(false);
                            area.setSize(new Dimension(500,300));
                            if (javax.swing.JOptionPane.showOptionDialog(this,
                                    area,
                                    VueResources.getString("optiondialog.addlibrary.message"),
                                    javax.swing.JOptionPane.DEFAULT_OPTION,
                                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    new Object[] {
                            	VueResources.getString("optiondialog.addlibrary.accept"), VueResources.getString("optiondialog.addlibrary.decline")
                            },
                            VueResources.getString("optiondialog.addlibrary.decline")) != 0) {
                            	//cancelButton.requestFocus();
                            	SwingUtilities.invokeLater(new Runnable() { 
                                    public void run() { 
                                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                                        AddLibraryDialog.this.getRootPane().setDefaultButton(cancelButton);
                                        cancelButton.requestFocusInWindow();
                                    } 
                            	} );
                                return;
                            }
                        }
                    }
                    
                    Log.info("checking if provider is installed");
                    if (proceed && (!provider.isInstalled())) {
                        Log.info("provider not yet installed, installing...");
                        factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
                        try {
                        	
                            GUI.activateWaitCursor();
                            factory.installProvider(provider.getId());                            
                        } catch (Throwable t1) {
                            Log.error("install failed " + provider.getId().getIdString());
                            VueUtil.alert(this,VueResources.getString("dialog.installerror.messaged"),VueResources.getString("dialog.error.title"));
                            //cancelButton.requestFocus();
                            SwingUtilities.invokeLater(new Runnable() { 
                                public void run() { 
                                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                                    AddLibraryDialog.this.getRootPane().setDefaultButton(cancelButton);
                                    cancelButton.requestFocusInWindow();
                                } 
                            } );
                            return;
                        } 
                    } else {
                        Log.info("provider already installed");
                    }
                    
                    if (proceed) {
                        // add to data sources list
                        try {
                            Log.info("creating data source");
                            ds = new edu.tufts.vue.dsm.impl.VueDataSource(factory.getIdManagerInstance().createId(),
                                    provider.getId(),
                                    true);
                        } catch (Throwable t) {
                            VueUtil.alert(this,VueResources.getString("dialog.loadfailed.message"),VueResources.getString("dialog.error.title"));
                            return;
                        }
                        Log.info("created data source");
                        
                        // show configuration, if needed
                        if (ds.hasConfiguration()) {
                            xml = ds.getConfigurationUIHints();
                        } else {
                            //System.out.println("No configuration to show");
                        }
						
                        this.newDataSource = ds;
                        Log.info("new data source:  " + this.newDataSource);
                    }
                } catch (Throwable t) {
                    //System.out.println("configuration setup failed");
                    VueUtil.alert(this,t.getMessage(),VueResources.getString("dialog.osidinstall.title"));
                    t.printStackTrace();
                    //cancelButton.requestFocus();
                    SwingUtilities.invokeLater(new Runnable() { 
                        public void run() { 
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                            AddLibraryDialog.this.getRootPane().setDefaultButton(cancelButton);
                            cancelButton.requestFocusInWindow();
                        } 
                    } );
                    return;
                }
            }
            
            if (xml != null) {
                edu.tufts.vue.ui.ConfigurationUI cui =
                        new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
                
                cui.setPreferredSize(new Dimension(350,(int)cui.getPreferredSize().getHeight()));
               
                
                if (javax.swing.JOptionPane.showOptionDialog(this,
                        cui,
                        
                        VueResources.getString("optiondialog.configuration.message"),
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.PLAIN_MESSAGE,
                        null,
                        new Object[] {
                	VueResources.getString("optiondialog.configuration.continue"), VueResources.getString("optiondialog.configuration.cancel")
                },
                VueResources.getString("optiondialog.configuration.continue")) == 1) {
					proceed = false;
				} else {
                    if (s != null) {
                        if (s.equals(MY_COMPUTER)) {
                            java.util.Properties p = cui.getProperties();
                            LocalFileDataSource ds = (LocalFileDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                            ds.setAddress(p.getProperty("address"));
                        } else if (s.equals(MY_SAVED_CONTENT)) {
                            java.util.Properties p = cui.getProperties();
                            FavoritesDataSource ds = (FavoritesDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                        } else if (s.equals(DS_FTP)) {
                            java.util.Properties p = cui.getProperties();
                            RemoteFileDataSource ds = (RemoteFileDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                            ds.setUserName(p.getProperty("username"));
                           try {
                                ds.setAddress(p.getProperty("address")); // this must be set last
                                ds.setPassword(p.getProperty("password"));
                            } catch (Exception ex) {
                                proceed = false;
                                VueUtil.alert(VueResources.getString("dialog.connectftp.message"),VueResources.getString("dialog.connectionerror.title"));
                                ex.printStackTrace();
                                // ignore any error for now
                            }
                            
                            
                        } else if (s.equals(DS_RSS) || s.equals(DS_XML)) {
                            java.util.Properties p = cui.getProperties();
                            BrowseDataSource ds = (BrowseDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                            ds.setAddress(p.getProperty("address"));
//                             java.util.Properties p = cui.getProperties();
//                             RSSDataSource ds = (RSSDataSource)this.oldDataSource;
//                             ds.setDisplayName(p.getProperty("name"));
//                             ds.setAddress(p.getProperty("address"));
                        }
                    } else {
                        try {
                            GUI.activateWaitCursor();
							//System.out.println("setting config " + cui.getProperties());
                            this.newDataSource.setConfiguration(cui.getProperties());
                            GUI.invokeAfterAWT(new Runnable() { public void run() {
                                try {
                                    //synchronized (dataSourceManager) {
                                    // DSM handles the synchronization itself -- locking hear can deadlock -- SMF 2007-11-19
                                        dataSourceManager.save();
                                        Log.info("saved");
                                    //}
                                } catch (Throwable t) {
                                    Log.error(t);
                                }
                            }});
                            
                        } catch (Throwable t2) {
                        	proceed=false;
                        	VueUtil.alert(this, VueResources.getString("dialog.addresourceerror.dialog"),VueResources.getString("dialog.addresourceerror.title"));
                            t2.printStackTrace();
                        } finally {
                            GUI.clearWaitCursor();
                        }
                    }
                }
            }
			if (proceed) {
				if (this.oldDataSource != null) {
					dataSourceList.addOrdered(this.oldDataSource);
				} else {
					dataSourceList.addOrdered(this.newDataSource);
					dataSourceManager.add(this.newDataSource);
				}
				providerListRenderer.setChecked(addLibraryList.getSelectedIndex());
			}
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            providerListRenderer.endWaitingMode();
            addLibraryList.repaint();
            GUI.clearWaitCursor();
            timer.stop();
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        
        DataSourceViewer.saveDataSourceViewer();
        //cancelButton.requestFocus();
        SwingUtilities.invokeLater(new Runnable() { 
            public void run() { 
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                AddLibraryDialog.this.getRootPane().setDefaultButton(cancelButton);
                cancelButton.requestFocusInWindow();
            } 
        } );
        return;
    }
	
	public DataSource getOldDataSource()
	{
		return this.oldDataSource;
	}
	
	public edu.tufts.vue.dsm.DataSource getNewDataSource()
	{
		return this.newDataSource;
	}
    
    public void actionPerformed(ActionEvent ae) {
        
        
        if (ae.getActionCommand().equals("Add")) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            GUI.activateWaitCursor();
            providerListRenderer.invokeWaitingMode(addLibraryList.getSelectedIndex());
            repaint();
            int ONE_TNTH_SECOND = 100;
            
            timer = new Timer(ONE_TNTH_SECOND, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    repaint();
                }});
                timer.start();
                
                AddDSThread t = new AddDSThread();
                t.start();
                
        } else {
            providerListRenderer.clearAllChecked();
            this.getRootPane().setDefaultButton(addButton);
            addButton.requestFocusInWindow();
            setVisible(false);
            
        }
    }
    private class AddDSThread extends Thread {
        public AddDSThread() {
            super();
        }
        public void run() {
            add();
        }
    }
    
    private class PopulateThread extends Thread {
        public PopulateThread() {
            super();
        }
        public void run() {
        	try
        	{
        		populate();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();        		
        	}            
        }
    }
}



