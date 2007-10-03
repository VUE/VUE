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
 * Publisher.java
 *
 * Created on January 7, 2004, 10:09 PM
 */

package tufts.vue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.LineBorder;
import java.util.Vector;
import java.util.Iterator;
import javax.swing.table.*;
import java.io.*;
import java.net.*;
import org.apache.commons.net.ftp.*;
import java.util.*;

import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
//import fedora.client.ingest.AutoIngestor;

import tufts.vue.action.*;

//required for publishing to Fedora

import fedora.client.FedoraClient;
import fedora.client.utility.ingest.AutoIngestor;
import fedora.client.utility.AutoFinder;
import fedora.server.types.gen.Datastream;
import fedora.client.Uploader;
/**
 *
 * @author  akumar03
 * @version $Revision: 1.58 $ / $Date: 2007-10-03 14:27:57 $ / $Author: anoop $
 */
public class Publisher extends JDialog implements ActionListener,tufts.vue.DublinCoreConstants   {
    
    /** Creates a new instance of Publisher */
    //todo: Create an interface for datasources and have separate implementations for each type of datasource.
    
    public static final String FILE_PREFIX = "file://";
    public static final int PUB_WIDTH = 500;
    public static final int PUB_HEIGHT = 250;
    
    public static final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public static final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    public static final String[] PUBLISH_INFORMATION = {"The “Export” function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.",
    "“Publish Map” saves only the map. Digital resources are not attached, but the resources’ paths are maintained. “Export Map” is the equivalent of the “Save” function for a registered digital repository.",
    "“Publish IMSCP Map” embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a “zip” file, which can be uploaded to a registered digital repository or saved locally. VUE can open zip files it originally created. (IMSCP: Instructional Management Services Content Package.)",
    "“Publish All” creates a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resources are accessible to all users viewing the map.",
    "“Publish IMSCP Map to Sakai” saves concept map in Sakai content hosting system.","Zips map with local resources."
    };
    public static final String[] MODE_LABELS = {"Map only","Map and resources","Zip bundle"};
    
    
    public static final String NEXT = "Next";
    public static final String CANCEL = "Cancel";
    public static final String PUBLISH = "Publish";
    private int publishMode = Publishable.PUBLISH_MAP;
    
    private int stage; // keep tracks of the screen
    
    
    private org.osid.shared.Type sakaiRepositoryType = new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
    private org.osid.shared.Type fedoraRepositoryType = new edu.tufts.vue.util.Type("tufts.edu","repository","fedora_2_2");
    
    
    int count = 0;
    
    JPanel modeSelectionPanel;
    JPanel resourceSelectionPanel;
    
    JButton backButton;
    JButton finishButton;
    JRadioButton publishMapRButton ;
    JRadioButton    publishMapAllRButton ;
    JRadioButton    publishZipRButton ;
    
    // JRadioButton publishCMapRButton;
    JRadioButton publishSakaiRButton;
    
    // JRadioButton publishAllRButton;
    JTextArea informationArea;
    JPanel buttonPanel;
    JTextArea modeInfo  = new JTextArea(PUBLISH_INFORMATION[0]);
    JPanel rPanel  = new JPanel(); // repository panel
    JPanel mPanel  = new JPanel(); // Mode Selection Panel
    JPanel pPanel = new JPanel(); // publish panel
    JButton nextButton = new JButton(NEXT);
    JButton cancelButton = new JButton(CANCEL);
    JButton publishButton = new JButton(PUBLISH);
    //JButton modeNext = new JButton("Next");
    JButton modeCancel = new JButton("Cancel");
    public static Vector resourceVector;
    File activeMapFile;
    public static JTable resourceTable;
    JComboBox dataSourceComboBox;
    
    ButtonGroup modeButtons = new ButtonGroup();
    java.util.List<JRadioButton> modeRadioButtons;
    JList repList;
    public Publisher(Frame owner,String title) {
        //testing
        super(owner,title,true);
        initialize();
    }
    
    public Publisher(Frame owner,String title,edu.tufts.vue.dsm.DataSource ds) {
        this(owner,title);
        repList.setSelectedValue(ds,false);
        setUpModeSelectionPanel();
    }
    private void initialize() {
        
        finishButton = new JButton("Finish");
        backButton = new JButton("< Back");
        cancelButton.addActionListener(this);
        finishButton.addActionListener(this);
        nextButton.addActionListener(this);
        backButton.addActionListener(this);
        cancelButton.addActionListener(this);
        //modeNext.addActionListener(this);
        publishButton.addActionListener(this);
        //setUpModeSelectionPanel();
        setUpRepositorySelectionPanel();
        getContentPane().add(rPanel,BorderLayout.CENTER);
        // adding the buttonPanel
        buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(7,7,7,7),BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.DARK_GRAY),BorderFactory.createEmptyBorder(5,0,0,0))));
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(cancelButton,BorderLayout.WEST);
        buttonPanel.add(nextButton,BorderLayout.EAST);
        getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        stage = 1;
        setLocation(X_LOCATION,Y_LOCATION);
        setModal(true);
        setSize(PUB_WIDTH, PUB_HEIGHT);
        setResizable(false);
        setVisible(true);
    }
    
    private void setUpRepositorySelectionPanel() {
        rPanel.setLayout(new BorderLayout());
        JLabel repositoryLabel = new JLabel("Select a FEDORA Instance");
        repositoryLabel.setBorder(BorderFactory.createEmptyBorder(15,10,0,0));
        rPanel.add(repositoryLabel,BorderLayout.NORTH);
        //adding the repository list
        //TODO: Populate this with actual repositories
        repList = new JList();
        repList.setModel(new DatasourceListModel(fedoraRepositoryType));
        repList.setCellRenderer(new DatasourceListCellRenderer());
        JScrollPane repPane = new JScrollPane(repList);
        JPanel scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(repPane);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        rPanel.add(scrollPanel,BorderLayout.CENTER);
    }
    
    private void setUpModeSelectionPanel() {
        //System.out.println("Counter: "+count);
        //count++;
        SpringLayout layout = new SpringLayout();
        
        // adding the modes
        JPanel mainPanel = new JPanel();
        
        //mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
        mainPanel.setLayout(layout);
        JLabel publishLabel = new JLabel("Publish as");
        publishLabel.setBorder(BorderFactory.createEmptyBorder(10,10,0,0));
        mainPanel.add(publishLabel);
        
        //adding the option Panel
        JPanel optionPanel = new JPanel(new GridLayout(0, 1));
        //optionPanel.setLayout(new BoxLayout(optionPanel,BoxLayout.Y_AXIS));
        publishMapRButton = new JRadioButton(MODE_LABELS[0]);
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
        
        // addding modeInfo
        modeInfo = new JTextArea(PUBLISH_INFORMATION[0]);
        modeInfo.setEditable(false);
        modeInfo.setLineWrap(true);
        modeInfo.setWrapStyleWord(true);
        modeInfo.setRows(7);
        modeInfo.setColumns(30);
        modeInfo.setVisible(true);
        //modeInfo.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        modeInfo.setBorder( BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY),BorderFactory.createEmptyBorder(5,5,5,5)));
        mainPanel.add(modeInfo);
        mainPanel.validate();
        
        // setting up cnstraints
        layout.putConstraint(SpringLayout.WEST, publishLabel,10,SpringLayout.WEST, mainPanel);
        layout.putConstraint(SpringLayout.WEST, optionPanel,3,SpringLayout.EAST, publishLabel);
        layout.putConstraint(SpringLayout.WEST,modeInfo,25,SpringLayout.EAST, optionPanel);
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
    
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(CANCEL)) {
            this.dispose();
        }else if(e.getActionCommand().equals(NEXT)) {
            getContentPane().remove(rPanel);
            //validateTree();
            setUpModeSelectionPanel();
            getContentPane().add(mPanel, BorderLayout.CENTER);
            getContentPane().validate();
            validateTree();
        }else if(e.getActionCommand().equals(PUBLISH)) {
            getContentPane().remove(mPanel);
            mPanel.remove(modeInfo);
            getContentPane().remove(buttonPanel);
            setUpPublishPanel();
            getContentPane().add(pPanel,BorderLayout.WEST);
            getContentPane().add(buttonPanel,BorderLayout.SOUTH);
            validate();
            repaint();
            publishMapToDL();
            dispose();
        }
        if(e.getActionCommand().equals(MODE_LABELS[0])){
            modeInfo.setText(PUBLISH_INFORMATION[1]);
        }
        if(e.getActionCommand().equals(MODE_LABELS[1])){
            modeInfo.setText(PUBLISH_INFORMATION[3]);
        }
        
    }
    private void   testPublishActiveMapToVUEDL() {
        //      System.out.println("Button: "+ publishMapRButton.getActionCommand()+"class:"+publishMapRButton+" is selected: "+publishMapRButton.isSelected()+" Mode:"+modeButtons.getSelection());
        //        System.out.println("Button: "+ publishMapAllRButton.getActionCommand()+"class:"+publishMapAllRButton+" is selected: "+publishMapAllRButton.isSelected());
        //System.out.println("Button: "+ publishZipRButton.getActionCommand()+"class:"+publishZipRButton+" is selected: "+publishZipRButton.isSelected());
        
        if(publishMapRButton.isSelected())
            FedoraPublisher.uploadMap("https", "vue-dl.tccs.tufts.edu", 8443, "fedoraAdmin", "vuefedora",VUE.getActiveMap());
        else if(publishMapAllRButton.isSelected())
            FedoraPublisher.uploadMapAll("https", "vue-dl.tccs.tufts.edu", 8443, "fedoraAdmin", "vuefedora",VUE.getActiveMap());
        else
            alert(VUE.getDialogParent(), "Publish mode not yet supported", "Mode Not Suported");
        
        System.out.println("Selected"+ repList.getSelectedValue()+ " Class of selected:"+repList.getSelectedValue().getClass());
        
    }
    
    private void publishMapToDL() {
        try{
            edu.tufts.vue.dsm.DataSource ds = (edu.tufts.vue.dsm.DataSource)repList.getSelectedValue();
            if(ds.getRepository().getType().isEqual(fedoraRepositoryType)) {
                Properties properties = ds.getConfiguration();
                if(publishMapRButton.isSelected())
                    FedoraPublisher.uploadMap("https",  properties.getProperty("fedora22Address"), 8443, properties.getProperty("fedora22UserName"),  properties.getProperty("fedora22Password"),VUE.getActiveMap());
                else if(publishMapAllRButton.isSelected())
                    FedoraPublisher.uploadMapAll("https",   properties.getProperty("fedora22Address"), 8443,  properties.getProperty("fedora22UserName"), properties.getProperty("fedora22Password"),VUE.getActiveMap());
                else
                    alert(VUE.getDialogParent(), "Publish mode not yet supported", "Mode Not Suported");
                
                
            }
        } catch(org.osid.repository.RepositoryException ex) {
            ex.printStackTrace();
        }
    }
    
    
    private void alert(Component parentComponent,String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(parentComponent,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);
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

