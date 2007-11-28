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

/*
 * Publisher.java
 *
 * Created on January 7, 2004, 10:09 PM
 */

package tufts.vue;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
//import javax.swing.border.LineBorder;
//import java.util.Vector;
//import java.util.Iterator;
//import javax.swing.table.*;
import javax.swing.event.*;
import java.io.*;
//import java.net.*;
//import org.apache.commons.net.ftp.*;
import java.util.*;

//import fedora.server.management.FedoraAPIM;
//import fedora.server.utilities.StreamUtility;
//import fedora.client.ingest.AutoIngestor;

//import tufts.vue.action.*;

//required for publishing to Fedora

//import fedora.client.FedoraClient;
//import fedora.client.utility.ingest.AutoIngestor;
//import fedora.client.utility.AutoFinder;
//import fedora.server.types.gen.Datastream;
//import fedora.client.Uploader;

/**
 * @author  akumar03
 * @version $Revision: 1.84 $ / $Date: 2007-11-28 16:08:02 $ / $Author: peter $
 */
public class Publisher extends JDialog implements ActionListener,tufts.vue.DublinCoreConstants   {
    
	private static final org.apache.log4j.Logger Log = 
		org.apache.log4j.Logger.getLogger(Publisher.class);
    
   /** Creates a new instance of Publisher */
    //TODO: Create an interface for datasources and have separate implementations for each type of datasource.
    public static final String TITLE = "Publisher";
    public static final String FILE_PREFIX = "file://";
    public static final int PUB_WIDTH = 550;
    public static final int PUB_HEIGHT = 250;
    
    public static final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public static final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    public static final String[] PUBLISH_INFORMATION = {"The \"Publish\" function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.",
    "\"Map only\" saves only the map to the digital repository. Digital resources are not attached, but the resource paths are maintained, whether to a local computer or the web.",
    "\"Publish IMSCP Map\" embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a \"zip\" file, which can be uploaded to a registered digital repository or saved locally. VUE can open zip files it originally created. (IMSCP: Instructional Management Services Content Package.)",
    "\"Map and resources\" create a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resouces are accessible to all users viewing the map.",
    "\"Publish IMSCP Map to Sakai\" saves concept map in Sakai content hosting system.","Zips map with local resources."
    };
    public static final String[] MODE_LABELS = {"Map only","Map and resources","Zip bundle"};
    
    private static final String NEXT_BUTTON_TEXT    = "Next";
    private static final String BACK_BUTTON_TEXT    = "< Back";
    private static final String FINISH_BUTTON_TEXT  = "Finish";
    private static final String CANCEL_BUTTON_TEXT  = "Cancel";
    private static final String PUBLISH_BUTTON_TEXT = "Publish";
    private static final String DONE_BUTTON_TEXT    = "Done";
    
    // action commands
    public static final String AC_SETUP_R = "AC_SETUP_R"; // repository selection
    public static final String AC_SETUP_M = "AC_SETUP_M"; // mode selection
    public static final String AC_SETUP_W = "AC_SETUP_W"; // workspace selection
    public static final String AC_SETUP_P = "AC_SETUP_P"; //  publish
    public static final String AC_SETUP_C = "AC_SETUP_C"; // confirm
    
    
    JButton backButton;
    JButton finishButton;
    JRadioButton publishMapRButton ;
    JRadioButton publishMapAllRButton ;
    JRadioButton publishZipRButton ;
    JRadioButton publishSakaiRButton;
    
    JTextArea informationArea;
    JPanel buttonPanel;
    JTextArea modeInfo  = new JTextArea(PUBLISH_INFORMATION[1]);
    JPanel rPanel = new JPanel(); // repository panel
    JPanel mPanel = new JPanel(); // Mode Selection Panel
    JPanel pPanel = new JPanel(); // publish panel
    JPanel wPanel = new JPanel(); // workspace selection
    JPanel cPanel = new JPanel(); // confirming publish
    
    JButton nextButton = new JButton(NEXT_BUTTON_TEXT);
    JButton cancelButton = new JButton(CANCEL_BUTTON_TEXT);
    JButton publishButton = new JButton(PUBLISH_BUTTON_TEXT);
    JButton doneButton = new JButton(DONE_BUTTON_TEXT);
    public static Vector resourceVector;
    File activeMapFile;
    public static JTable resourceTable;
    JComboBox dataSourceComboBox;
    
    ButtonGroup modeButtons = new ButtonGroup();
    java.util.List<JRadioButton> modeRadioButtons;
    JList repList;
    JTree wTree; // list of workspaces
    private TreePath _tp = null; // Selection from Sakai workspace panel
    
    org.osid.shared.Type dataSourceType =edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE;
    private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("sakaiproject.org","asset","siteCollection");
    
    public Publisher(edu.tufts.vue.dsm.DataSource dataSource) {
        super(VUE.getDialogParentAsFrame(),TITLE,true);
        setUpButtonPanel();
        try {
            dataSourceType = dataSource.getRepository().getType();
        } catch(Throwable t) {
            t.printStackTrace();
        }
        repList = new JList();
        repList.setModel(new DatasourceListModel(dataSourceType));
        repList.setSelectedValue(dataSource,false);
        if(dataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
            setUpWorkspaceSelectionPanel();
            getContentPane().add(wPanel, BorderLayout.CENTER);
            nextButton.setActionCommand(AC_SETUP_M);  // "Next" on ws panel activates Mode panel - pdw 10-nov-07 
        } else{
            setUpModeSelectionPanel();
            getContentPane().add(mPanel, BorderLayout.CENTER);
            nextButton.setActionCommand(AC_SETUP_M);
        }
        setLocation(X_LOCATION,Y_LOCATION);
        setModal(true);
        setSize(PUB_WIDTH, PUB_HEIGHT);
        setResizable(false);
        setVisible(true);
    }
    
    
    public Publisher(org.osid.shared.Type type) {
        super(VUE.getDialogParentAsFrame(),TITLE,true);
        this.dataSourceType = type;
        initialize();
        if(dataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
            nextButton.setActionCommand(AC_SETUP_W);
        } else{
            nextButton.setActionCommand(AC_SETUP_M);
        }
    }
    private void initialize() {
        
        finishButton = new JButton(FINISH_BUTTON_TEXT);
        backButton = new JButton(BACK_BUTTON_TEXT);
        finishButton.addActionListener(this);
        backButton.addActionListener(this);
        setUpRepositorySelectionPanel();
        getContentPane().add(rPanel,BorderLayout.CENTER);
        setUpButtonPanel();
        getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        setLocation(X_LOCATION,Y_LOCATION);
        setModal(true);
        setSize(PUB_WIDTH, PUB_HEIGHT);
        setResizable(false);
        setVisible(true);
    }
    
    private void setUpButtonPanel() {
        cancelButton.addActionListener(this);
        nextButton.addActionListener(this);
        publishButton.addActionListener(this);
        buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(7,7,7,7),BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.DARK_GRAY),BorderFactory.createEmptyBorder(5,0,0,0))));
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(cancelButton,BorderLayout.WEST);
        buttonPanel.add(nextButton,BorderLayout.EAST);
        getContentPane().add(buttonPanel,BorderLayout.SOUTH);
    }
    private void setUpRepositorySelectionPanel() {
        rPanel.setLayout(new BorderLayout());
        JLabel repositoryLabel = new JLabel("Select a repository Instance");
        repositoryLabel.setBorder(BorderFactory.createEmptyBorder(15,10,0,0));
        rPanel.add(repositoryLabel,BorderLayout.NORTH);
        repList = new JList();
        repList.setModel(new DatasourceListModel(dataSourceType));
        repList.setCellRenderer(new DatasourceListCellRenderer());
        JScrollPane repPane = new JScrollPane(repList);
        JPanel scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(repPane);
        // nextButton.setActionCommand(NEXT_SAKAI_WS);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        rPanel.add(scrollPanel,BorderLayout.CENTER);
    }
    
    private void setUpWorkspaceSelectionPanel() {
        wPanel.setLayout(new BorderLayout());
        JLabel wLabel = new JLabel("Select a workspace in "+ ((edu.tufts.vue.dsm.DataSource)repList.getSelectedValue()).getRepositoryDisplayName());
        wLabel.setBorder(BorderFactory.createEmptyBorder(15,10,0,0));
        wPanel.add(wLabel,BorderLayout.NORTH);
        edu.tufts.vue.dsm.DataSource selectedDataSource = (edu.tufts.vue.dsm.DataSource) repList.getSelectedValue();
        wTree = new JTree(getWorkSpaceTreeModel(selectedDataSource));
        wTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        wTree.addTreeSelectionListener(new TreeSelectionListener() {
        	public void valueChanged( TreeSelectionEvent tse ) {
        		TreePath tp = tse.getNewLeadSelectionPath();
        		_tp = tp;  
        	}
        });
        JScrollPane wPane = new JScrollPane(wTree);
        JPanel scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(wPane);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        wPanel.add(scrollPanel,BorderLayout.CENTER);
        nextButton.setActionCommand(AC_SETUP_M);
    }
    
    private void setUpModeSelectionPanel() {
        SpringLayout layout = new SpringLayout();
        
        // adding the modes
        JPanel mainPanel = new JPanel();
        
         mainPanel.setLayout(layout);
        JLabel publishLabel = new JLabel("Publish as");
        publishLabel.setBorder(BorderFactory.createEmptyBorder(10,10,0,0));
        publishLabel.setFont(tufts.vue.gui.GUI.LabelFace);
        mainPanel.add(publishLabel);
        
        //adding the option Panel
        JPanel optionPanel = new JPanel(new GridLayout(0, 1));
        //optionPanel.setLayout(new BoxLayout(optionPanel,BoxLayout.Y_AXIS));
        publishMapRButton = new JRadioButton(MODE_LABELS[0]);
        publishMapRButton.setSelected(true);
        publishMapAllRButton = new JRadioButton(MODE_LABELS[1]);
        publishZipRButton = new JRadioButton(MODE_LABELS[2]);
        publishZipRButton.setEnabled(false);
        
        modeButtons.add(publishMapRButton);
        modeButtons.add(publishMapAllRButton);
        modeButtons.add(publishZipRButton);
        optionPanel.add(publishMapRButton);
        
        
        optionPanel.add(publishMapAllRButton);
        optionPanel.add(publishZipRButton);
        
        publishMapRButton.addActionListener(this);
        publishMapAllRButton.addActionListener(this);
        
        //   modeButtons.setSelected(radioButtons.get(0).getModel(),true);
        optionPanel.setBorder(BorderFactory.createEmptyBorder(10,5,0,0));
        optionPanel.validate();
        mainPanel.add(optionPanel);
        
        // adding modeInfo
        modeInfo = new JTextArea(PUBLISH_INFORMATION[1]);
        modeInfo.setEditable(false);
        modeInfo.setLineWrap(true);
        modeInfo.setWrapStyleWord(true);
        modeInfo.setRows(7);
        modeInfo.setColumns(28);
        modeInfo.setVisible(true);
        modeInfo.setFont(tufts.vue.gui.GUI.LabelFace);
        modeInfo.setHighlighter(null);
        
        //modeInfo.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        modeInfo.setBorder( BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY),BorderFactory.createEmptyBorder(5,5,5,5)));
        mainPanel.add(modeInfo);
        mainPanel.validate();
        
        // setting up constraints
        layout.putConstraint(SpringLayout.WEST, publishLabel,10,SpringLayout.WEST, mainPanel);
        layout.putConstraint(SpringLayout.WEST, optionPanel,3,SpringLayout.EAST, publishLabel);
        layout.putConstraint(SpringLayout.WEST,modeInfo,20,SpringLayout.EAST, optionPanel);
        layout.putConstraint(SpringLayout.NORTH, publishLabel,10,SpringLayout.NORTH, mainPanel);
        layout.putConstraint(SpringLayout.NORTH, optionPanel,10,SpringLayout.NORTH, mainPanel);
        layout.putConstraint(SpringLayout.NORTH, modeInfo,20,SpringLayout.NORTH, mainPanel);
        mPanel.setLayout(new BorderLayout());
        mPanel.add(mainPanel,BorderLayout.CENTER);
        
        // Removing next button and adding publish button
        buttonPanel.remove(nextButton);
        buttonPanel.add(publishButton,BorderLayout.EAST);
    }
    
    
    
    private void  setUpPublishPanel() {
        JLabel pLabel = new JLabel("publising to: "+((edu.tufts.vue.dsm.DataSource)repList.getSelectedValue()).getRepositoryDisplayName(),VueResources.getImageIcon("dsv.statuspanel.waitIcon"),JLabel.CENTER);
        pLabel.setBorder(BorderFactory.createEmptyBorder(10,10,0,0));
        pPanel.add(pLabel);
        buttonPanel.remove(publishButton);
    }
    
    private void setUpConfirmPanel() {
        JLabel cLabel = new JLabel("Publishing to "+((edu.tufts.vue.dsm.DataSource)repList.getSelectedValue()).getRepositoryDisplayName()+" was successful.");
        cLabel.setBorder(BorderFactory.createEmptyBorder(80,10,0,0));
        cPanel.add(cLabel);
        buttonPanel.remove(cancelButton);
        buttonPanel.repaint();
        doneButton.addActionListener(this);
        buttonPanel.add(doneButton,BorderLayout.EAST);
        getContentPane().remove(pPanel);
        getContentPane().add(cPanel, BorderLayout.CENTER);
        getContentPane().validate();
        validateTree();
    }
    
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(CANCEL_BUTTON_TEXT)) {
            this.dispose();
        } else if(e.getActionCommand().equals(DONE_BUTTON_TEXT)) {
            this.dispose();
        }else if(e.getActionCommand().equals(AC_SETUP_W)) {
            getContentPane().remove(rPanel);
            setUpWorkspaceSelectionPanel();
            getContentPane().add(wPanel, BorderLayout.CENTER);
            getContentPane().validate();
            validateTree();
        }  else if(e.getActionCommand().equals(AC_SETUP_M)) {
            if(dataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                getContentPane().remove(wPanel);
            } else {
                getContentPane().remove(rPanel);
            }
            setUpModeSelectionPanel();
            getContentPane().add(mPanel, BorderLayout.CENTER);
            getContentPane().validate();
            validateTree();
        }  else if(e.getActionCommand().equals(NEXT_BUTTON_TEXT)) {
            System.out.println("Selected Repository: "+repList.getSelectedValue());
             if(repList.getSelectedValue() == null) {
                alert(this,"No repository is selected. Please  select a repository","Publish Error");
                return;
            }
            getContentPane().remove(rPanel);
            //validateTree();
            if(dataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                setUpWorkspaceSelectionPanel();
                getContentPane().add(wPanel, BorderLayout.CENTER);
            } else {
                setUpModeSelectionPanel();
                getContentPane().add(mPanel, BorderLayout.CENTER);
            }
            getContentPane().validate();
            validateTree();
        } else if(e.getActionCommand().equals(PUBLISH_BUTTON_TEXT)) {
            
            final Thread t = new Thread() {
                public void run() {
                    getContentPane().remove(mPanel);
                    mPanel.remove(modeInfo);
                    getContentPane().remove(buttonPanel);
                    setUpPublishPanel();
                    
                    getContentPane().add(pPanel,BorderLayout.WEST);
                    getContentPane().add(buttonPanel,BorderLayout.SOUTH);
                    validate();
                    repaint();
                }
            };
            Thread invokeThread = new Thread() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(t);
                        publishMapToDL();
                        setUpConfirmPanel();
                    }catch(Throwable tw) {
                        tw.printStackTrace();
                    }
                }
            };
            invokeThread.start();           
        }
        if(e.getActionCommand().equals(MODE_LABELS[0])){
            modeInfo.setText(PUBLISH_INFORMATION[1]);
        }
        if(e.getActionCommand().equals(MODE_LABELS[1])){
            modeInfo.setText(PUBLISH_INFORMATION[3]);
        }
        
    }
    
    private void publishMapToDL() {
        if(repList.getSelectedValue() == null) {
             alert(this,"No repository is selectd. Please go back and select a repository","Publish Error");  
        }
        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)repList.getSelectedValue();
        try{
            if(ds.getRepository().getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE)) {
                 if(publishMapRButton.isSelected())
                    FedoraPublisher.uploadMap(ds,VUE.getActiveMap());
                 else if(publishMapAllRButton.isSelected())
                    FedoraPublisher.uploadMapAll(ds,VUE.getActiveMap());
                 else
                    alert(VUE.getDialogParent(), "Publish mode not yet supported", "Mode Not Suported");
            } else if(ds.getRepository().getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
            	DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)_tp.getLastPathComponent();
            	String siteId = ((SakaiSiteUserObject)(treeNode.getUserObject())).getId();
            	// TODO: Verify that a site is selected before proceeding - pdw 10-nov-07 
                if(publishMapRButton.isSelected()) {
                    SakaiPublisher.uploadMap( ds, siteId, VUE.getActiveMap());
                }else if(publishMapAllRButton.isSelected()){
                	SakaiPublisher.uploadMapAll( ds, siteId, VUE.getActiveMap());
                }   else
                    alert(VUE.getDialogParent(), "Publish mode not yet supported", "Mode Not Suported");
            }
        } catch(Throwable t) {
            t.printStackTrace();
            alert(this,"You are not authorized to publish map to "+ds.getRepositoryDisplayName()+". Error message: "+t.getMessage(),"Publish Error");
            this.dispose();
        }
    }
    
    
    private void alert(Component parentComponent,String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(parentComponent,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    
    private DefaultTreeModel getWorkSpaceTreeModel(edu.tufts.vue.dsm.DataSource dataSource) {
        String ROOT_LABEL = "Sites";
        javax.swing.tree.DefaultMutableTreeNode root = new javax.swing.tree.DefaultMutableTreeNode(ROOT_LABEL);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        try {
            org.osid.repository.Repository repository = dataSource.getRepository();
            org.osid.repository.AssetIterator assetIterator = repository.getAssetsByType(_collectionAssetType);
            Log.debug( "repository is " + repository.getDisplayName());
            
            while (assetIterator.hasNextAsset()) {
                org.osid.repository.Asset asset = assetIterator.nextAsset();
                Log.debug( "asset is " + asset.getDisplayName());
                
                SakaiSiteUserObject userObject = new SakaiSiteUserObject();
                userObject.setId(asset.getId().getIdString());
                userObject.setDisplayName(asset.getDisplayName());
                Log.debug( "another obj " + userObject);
                
                javax.swing.tree.DefaultMutableTreeNode nextTreeNode = new javax.swing.tree.DefaultMutableTreeNode(userObject);
                treeModel.insertNodeInto(nextTreeNode,root,0);
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return treeModel;
    }
    
    class DatasourceListCellRenderer extends   DefaultListCellRenderer  {
        
        private JPanel composite = new JPanel(new BorderLayout());
        private JRadioButton radioButton;
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(
                    list, (value instanceof edu.tufts.vue.dsm.DataSource? ((edu.tufts.vue.dsm.DataSource)value).getRepositoryDisplayName():value) , index, isSelected, cellHasFocus);
            label.setBackground(Color.WHITE);
            label.setBorder(null);
            label.setFont(tufts.vue.gui.GUI.LabelFace);
            if(isSelected) {
                label.setForeground(Color.BLACK);
            } else {
                label.setForeground(Color.DARK_GRAY);
            }
            if (composite.getComponentCount() == 0) {
                radioButton = new JRadioButton();
                radioButton.setBackground(Color.WHITE);
    
                composite.add(label, BorderLayout.CENTER);
                composite.add(radioButton, BorderLayout.WEST);
            }
            radioButton.setSelected(isSelected);
            radioButton.setFont(tufts.vue.gui.GUI.LabelFace);
            return composite;
        }
    }
    class DatasourceListModel extends DefaultListModel {
        org.osid.shared.Type type;
        public DatasourceListModel(org.osid.shared.Type type) {
            this.type = type;
        }
        public Object getElementAt(int index) {
            return(getResources().get(index));
        }
        
        public int getSize() {
            return getResources().size();
            
        }
        
        private java.util.List<edu.tufts.vue.dsm.DataSource> getResources() {
            return Publisher.getPublishableDatasources(type);
            
        }
    }
    /**
     * class  WorkSpaceTreeModel extends DefaultTreeModel {
     * public static final String ROOT = "Sites";
     * edu.tufts.vue.dsm.DataSource dataSource;
     *
     * public WorkSpaceTreeModel(edu.tufts.vue.dsm.DataSource dataSource) {
     * super(new DefaultMutableTreeNode(ROOT));
     * this.dataSource = dataSource;
     * }
     * }
     */
    public  static java.util.List<edu.tufts.vue.dsm.DataSource> getPublishableDatasources(org.osid.shared.Type type) {
        edu.tufts.vue.dsm.DataSource[] datasources = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().getDataSources();
        java.util.List<edu.tufts.vue.dsm.DataSource> resourceList = new ArrayList<edu.tufts.vue.dsm.DataSource>();
        for(int i=0;i<datasources.length;i++) {
            try {
                if (datasources[i].getRepository().getType().isEqual(type)) {
                    resourceList.add(datasources[i]);
                }
            } catch(org.osid.repository.RepositoryException ex) {
                ex.printStackTrace();
            }
        }
        return resourceList;
    }
    
}

