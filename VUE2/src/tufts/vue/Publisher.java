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
 * Publisher.java
 *
 * Created on January 7, 2004, 10:09 PM
 */

package tufts.vue;

import edu.tufts.vue.dsm.*;

import javax.swing.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import org.osid.repository.RepositoryException;
import org.osid.shared.SharedException;

import java.io.*;
import java.util.*;

/**
 * @author  akumar03
 * @version $Revision: 1.97 $ / $Date: 2009-03-14 03:50:16 $ / $Author: vaibhav $
 */
public class Publisher extends JDialog implements ActionListener,tufts.vue.DublinCoreConstants   {
    
    private static final Logger logger = Logger.getLogger(Publisher.class);
    
    /** Creates a new instance of Publisher */
    //TODO: Create an interface for datasources and have separate implementations for each type of datasource.
    public static final String TITLE = "Publisher";
    public static final String FILE_PREFIX = "file://";
    public static final int PUB_WIDTH = 550;
    public static final int PUB_HEIGHT = 250;
    
    public static final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public static final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    
    private static final String DUPLICATE_OBJ_ERR_MESG  =  "The map already exists in the Repostiory. Do you want to overwrite it?";
    private static final String DUPLICATE_OBJ_ERR_TITLE =  "Duplicate Resource";
    
    public static final String[] PUBLISH_INFORMATION = {"The \"Publish\" function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.",
    "\"Map only\" saves only the map to the digital repository. Digital resources are not attached, but the resource paths are maintained, whether to a local computer or the web.",
    "\"VUE package (vpk)\" embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a VUE package (vpk) file, which can be uploaded to a registered digital repository or saved locally.",
    "\"Map and resources\" creates a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resouces are accessible to all users viewing the map.",
    "\"Publish IMSCP Map to Sakai\" saves concept map in Sakai content hosting system.","Zips map with local resources."
    };
    public static final String[] MODE_LABELS = {"Map only","Map and resources","VUE package (vpk)"};
    
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
    
    org.osid.shared.Type fedoraDataSourceType = edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE;
//    private static final org.osid.shared.Type _collectionAssetType =  edu.tufts.vue.dsm.DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE;
    
    public Publisher(edu.tufts.vue.dsm.DataSource dataSource) {
        super(VUE.getDialogParentAsFrame(),TITLE,true);
        // perform this only when map is saved.
        if(isMapSaved()) {
            //logger.setLevel(Level.DEBUG);
            setUpButtonPanel();
            try {
                fedoraDataSourceType = dataSource.getRepository().getType();
            } catch(Throwable t) {
                t.printStackTrace();
            }
            repList = new JList();
            repList.setModel(new DatasourceListModel(fedoraDataSourceType));
            repList.setSelectedValue(dataSource,false);
            if(fedoraDataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
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
    }
    
    
    public Publisher(org.osid.shared.Type type) {
        super(VUE.getDialogParentAsFrame(),TITLE,true);
        // perform this only when map is saved.
        if(isMapSaved())  {
            
            //logger.setLevel(Level.DEBUG);
            this.fedoraDataSourceType = type;
            initialize();
            if(fedoraDataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                nextButton.setActionCommand(AC_SETUP_W);
            } else{
                nextButton.setActionCommand(AC_SETUP_M);
            }
        }
    }
    
    public boolean  isMapSaved()  {
        if(VUE.getActiveMap() == null) {
            VueUtil.alert(VueResources.getString("dialog.mapnotsave.message"),VueResources.getString("dialog.mapnotsave.title"));
            return false;
        } else if(VUE.getActiveMap().getFile() == null) {
            VueUtil.alert(VueResources.getString("dialog.mapnotsave.message"),VueResources.getString("dialog.mapnotsave.title"));
            return false;
        } else return true;
        
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
        repList.setModel(new DatasourceListModel(fedoraDataSourceType));
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
   //     publishZipRButton.setEnabled(false);
        
        modeButtons.add(publishMapRButton);
        modeButtons.add(publishMapAllRButton);
        modeButtons.add(publishZipRButton);
        optionPanel.add(publishMapRButton);
        
        
        optionPanel.add(publishMapAllRButton);
        optionPanel.add(publishZipRButton);
        
        publishMapRButton.addActionListener(this);
        publishZipRButton.addActionListener(this);
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
            if(fedoraDataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)_tp.getLastPathComponent();
                String fileName = VUE.getActiveMap().getFile().getName();
                org.osid.repository.Asset asset =  ((SakaiSiteUserObject)(treeNode.getUserObject())).getAsset();
                /**
                 * int confirm = 0;
                 * try {
                 * if(isFilePresent( asset,   fileName )) {
                 * confirm = VueUtil.confirm(DUPLICATE_OBJ_ERR_MESG,DUPLICATE_OBJ_ERR_TITLE);
                 * }
                 * }catch(Throwable t) {
                 * t.printStackTrace();
                 * alert(this,"An error occurred while checking existence of resource in repository. Error message: "+t.getMessage(),"Publish Error");
                 * this.dispose();
                 * }
                 * if(confirm == JOptionPane.NO_OPTION) {
                 * return;
                 * }
                 */
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
                alert(this,VueResources.getString("dialog.norepository.message"), VueResources.getString("dialog.norepository.title"));
                return;
            }
            getContentPane().remove(rPanel);
            //validateTree();
            if(fedoraDataSourceType.isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
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
        if(e.getActionCommand().equals(MODE_LABELS[2])){
            modeInfo.setText(PUBLISH_INFORMATION[2]);
        }
        
    }
    
    private void publishMapToDL() {
        if(repList.getSelectedValue() == null) {
            alert(this,VueResources.getString("dialog.norepository.message"), VueResources.getString("dialog.norepository.title"));
        }
        edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)repList.getSelectedValue();
        try{
            if(ds.getRepository().getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE)) {
                if(publishMapRButton.isSelected())
                    FedoraPublisher.uploadMap(ds,VUE.getActiveMap());
                else if(publishMapAllRButton.isSelected())
                    FedoraPublisher.uploadMapAll(ds,VUE.getActiveMap());
                else if(publishZipRButton.isSelected())
                    FedoraPublisher.uploadArchive(ds,VUE.getActiveMap());
                else
                    alert(VUE.getDialogParent(), VueResources.getString("dialog.publishnotsupported.message"), VueResources.getString("dialog.publishnotsupported.title"));
            } else if(ds.getRepository().getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)_tp.getLastPathComponent();
                String siteId = ((SakaiSiteUserObject)(treeNode.getUserObject())).getId();
                // TODO: Verify that a site is selected before proceeding - pdw 10-nov-07
                String fileName = VUE.getActiveMap().getFile().getName();
                org.osid.repository.Asset asset =  ((SakaiSiteUserObject)(treeNode.getUserObject())).getAsset();
                int confirm = JOptionPane.YES_OPTION;  //TODO: do we really want the default to be overwrite? - pdw 05-feb-07
                if(isFilePresent( asset,   fileName )) {
                    confirm = JOptionPane.showConfirmDialog(this, DUPLICATE_OBJ_ERR_MESG, DUPLICATE_OBJ_ERR_TITLE, JOptionPane.YES_NO_OPTION );
                }
                if(confirm == JOptionPane.NO_OPTION) {
                    this.dispose();
                    return;
                }
                if(publishMapRButton.isSelected()) {
                    SakaiPublisher.uploadMap( ds, siteId, VUE.getActiveMap(), confirm );
                }else if(publishMapAllRButton.isSelected()){
                    SakaiPublisher.uploadMapAll( ds, siteId, VUE.getActiveMap(), confirm );
                }   else
                    alert(VUE.getDialogParent(), VueResources.getString("dialog.publishnotsupported.message"), VueResources.getString("dialog.publishnotsupported.title"));
            }
        } catch(Throwable t) {
            t.printStackTrace();
            alert(this,VueResources.getString("dialog.notauthorized.message")+ds.getRepositoryDisplayName()+". Error message: "+t.getMessage(),VueResources.getString("dialog.norepository.title"));
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
            org.osid.repository.AssetIterator assetIterator =
                    repository.getAssetsByType(DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE);
            logger.debug( "repository is " + repository.getDisplayName());
            
            while (assetIterator.hasNextAsset()) {
                org.osid.repository.Asset asset = assetIterator.nextAsset();
                logger.debug( "asset is " + asset.getDisplayName());
                
                // Populate only with Sakai collections, ignore non-folder types
                if( asset.getAssetType().isEqual(DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE)) {
                    addWorkSiteNode(root, treeModel, asset);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return treeModel;
    }
    
    
    /**
     * @param parentNode Node in tree under which the new node is placed
     * @param treeModel Tree containing work sites
     * @param asset Asset representing work site
     * @throws SharedException
     */
    private void addWorkSiteNode(
            DefaultMutableTreeNode parentNode,
            DefaultTreeModel treeModel,
            org.osid.repository.Asset asset)
            throws SharedException {
        // Only display folders, not the resources themselves, since we want
        // to show nodes that can contain the published map.
        if( !asset.getAssetType().isEqual(DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE) ) {
            return;
        }
        // Don't display folders that are VUE maps
        if( isVueMapFolder(asset) ) {
            return;
        }
        SakaiSiteUserObject userObject = new SakaiSiteUserObject();
        userObject.setId(asset.getId().getIdString());
        userObject.setDisplayName(asset.getDisplayName());
        userObject.setAsset(asset);
        logger.debug( "another obj " + userObject);
        
        DefaultMutableTreeNode nextTreeNode = new DefaultMutableTreeNode(userObject);
        treeModel.insertNodeInto( nextTreeNode, parentNode, 0 );
        
        org.osid.repository.AssetIterator assetIterator =
                asset.getAssetsByType(DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE);
        logger.debug( "asset is " + asset.getDisplayName());
        
        while (assetIterator.hasNextAsset()) {
            org.osid.repository.Asset childAsset = assetIterator.nextAsset();
            logger.debug( "asset is " + childAsset.getDisplayName());
            
            addWorkSiteNode(nextTreeNode, treeModel, childAsset);
        }
    }
    
    class DatasourceListCellRenderer extends   DefaultListCellRenderer {
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
    
    /**
     * @param asset
     * @return true if
     * @throws RepositoryException
     */
    private static boolean isVueMapFolder( org.osid.repository.Asset asset )
    throws RepositoryException {
        String folderName = asset.getDisplayName();
        if( folderName.endsWith( SakaiPublisher.VUE_MAP_FOLDER_SUFFIX )) {
            return true;
        } else {
            return false;
        }
    }
    
    /** This method is called to check whether a folder exists in a Sakai
     * folder that is the same as would be created if fileName was published
     *
     * @param collectionId
     * @param fileName
     * @return true if publishing this fileName would attempt to overwrite an
     * existing folder
     */
    public static boolean isFilePresent( org.osid.repository.Asset asset, String fileName )
    throws RepositoryException {
        org.osid.repository.AssetIterator assetIterator =
                asset.getAssetsByType(DataSourceTypes.SAKAI_COLLECTION_ASSET_TYPE);
        logger.debug( "asset is " + asset.getDisplayName());
        
        while( assetIterator.hasNextAsset() ) {
            org.osid.repository.Asset childAsset = assetIterator.nextAsset();
            logger.debug( "asset is " + childAsset.getDisplayName());
            
            if( (SakaiPublisher.makeSakaiFolderFromVueMap(fileName).equals(childAsset.getDisplayName())) ) {
                return true;
            }
        }
        return false;
    }
    
}

