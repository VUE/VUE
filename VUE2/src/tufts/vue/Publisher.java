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
 * @version $Revision: 1.48 $ / $Date: 2007-09-20 13:52:50 $ / $Author: anoop $
 */
public class Publisher extends JDialog implements ActionListener,tufts.vue.DublinCoreConstants {
    
    /** Creates a new instance of Publisher */
    //todo: Create an interface for datasources and have separate implementations for each type of datasource.
    
    public static final String FILE_PREFIX = "file://";
    public static final int SELECTION_COL = 0; // boolean selection column in resource table
    public static final int RESOURCE_COL = 1; // column that displays the name of resource
    public static final int SIZE_COL = 2; // the column that displays size of files.
    public static final int STATUS_COL = 3;// the column that displays the status of objects that will be ingested.
    public static final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public static final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    public static final int PUB_WIDTH = 500;
    public static final int PUB_HEIGHT = 250;
    public static final String[] PUBLISH_INFORMATION = {"The “Export” function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.",
    "“Export Map” saves only the map. Digital resources are not attached, but the resources’ paths are maintained. “Export Map” is the equivalent of the “Save” function for a registered digital repository.",
    "“Export IMSCP Map” embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a “zip” file, which can be uploaded to a registered digital repository or saved locally. VUE can open zip files it originally created. (IMSCP: Instructional Management Services Content Package.)",
    "“Export All” creates a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resources are accessible to all users viewing the map.",
    "“Export IMSCP Map to Sakai” saves concept map in Sakai content hosting system.","Zips map with local resources."
    };
    
    public static final String[] MODE_LABELS = {"Map only","Map and resources","Zip bundle"};
    
    
    public static final String NEXT = "Next";
    public static final String CANCEL = "Cancel";
    public static final String PUBLISH = "Publish";
    private int publishMode = Publishable.PUBLISH_MAP;
    
    private int stage; // keep tracks of the screen
    private String IMSManifest; // the string is written to manifest file;
    private static final  String VUE_MIME_TYPE = VueResources.getString("vue.type");
    private static final  String BINARY_MIME_TYPE = "application/binary";
    private static final  String ZIP_MIME_TYPE = "application/zip";
    
    private static final String IMSCP_MANIFEST_ORGANIZATION = "%organization%";
    private static final String IMSCP_MANIFEST_METADATA = "%metadata%";
    private static final String IMSCP_MANIFEST_RESOURCES = "%resources%";
    
    
    
    JPanel modeSelectionPanel;
    JPanel resourceSelectionPanel;
    
    JButton backButton;
    JButton finishButton;
    JRadioButton publishMapRButton;
    // JRadioButton publishCMapRButton;
    JRadioButton publishSakaiRButton;
    JRadioButton publishZipRButton;
    // JRadioButton publishAllRButton;
    JTextArea informationArea;
    JPanel buttonPanel;
    JTextArea modeInfo  = new JTextArea(PUBLISH_INFORMATION[0]);
    JPanel rPanel  = new JPanel();
    JPanel mPanel  = new JPanel();
    JButton nextButton = new JButton(NEXT);
    JButton cancelButton = new JButton(CANCEL);
    JButton publishButton = new JButton(PUBLISH);
    JButton modeNext = new JButton("Next");
    JButton modeCancel = new JButton("Cancel");
    public static Vector resourceVector;
    File activeMapFile;
    public static ResourceTableModel resourceTableModel;
    public static JTable resourceTable;
    JComboBox dataSourceComboBox;
    
    ButtonGroup modeButtons;
    public Publisher(Frame owner,String title) {
        //testing
        super(owner,title,true);
        initialize();
    }
    private void initialize() {
        
        finishButton = new JButton("Finish");   
        backButton = new JButton("< Back");
        cancelButton.addActionListener(this);
        finishButton.addActionListener(this);
        nextButton.addActionListener(this);
        backButton.addActionListener(this);
        nextButton.addActionListener(this);
        cancelButton.addActionListener(this);
        modeNext.addActionListener(this);
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
        String[] data = {"Repository 1", "Repository 2", "Repository 3"};
        JList repList = new JList(data);
        JScrollPane repPane = new JScrollPane(repList);
        JPanel scrollPanel = new JPanel(new BorderLayout());
        scrollPanel.add(repPane);
        scrollPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        rPanel.add(scrollPanel,BorderLayout.CENTER);
    }
    
    private void setUpModeSelectionPanel() {
        SpringLayout layout = new SpringLayout();
        
        // adding the modes
        JPanel mainPanel = new JPanel();
        //mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
        mainPanel.setLayout(layout);
        JLabel publishLabel = new JLabel("Publish as");
        publishLabel.setBorder(BorderFactory.createEmptyBorder(10,10,0,0));
        mainPanel.add(publishLabel);
        //adding the option Panel
        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel,BoxLayout.Y_AXIS));
        java.util.List<JRadioButton> radioButtons = createRadioButtons();
        for(JRadioButton b: radioButtons) {
            optionPanel.add(b);
             
        }
        optionPanel.setBorder(BorderFactory.createEmptyBorder(10,5,0,0));
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
    
    private  java.util.List<JRadioButton> createRadioButtons() {
        java.util.List<JRadioButton> radioList = new ArrayList<JRadioButton>();
        modeButtons = new ButtonGroup();
        for(int i=0;i < MODE_LABELS.length;i++) {
            JRadioButton button = new JRadioButton(MODE_LABELS[i]);
            radioList.add(button);
            modeButtons.add(button);
        }
        return radioList;
        
    }
    private void old_setUpModeSelectionPanel() {
        
        modeSelectionPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        modeSelectionPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,0,2,2);
        
        ButtonGroup modeSelectionGroup = new ButtonGroup();
        JLabel topLabel = new JLabel("Location");
        JLabel modeLabel = new JLabel("Mode");
        
        PolygonIcon lineIcon = new PolygonIcon(Color.DARK_GRAY);
        lineIcon.setIconWidth(PUB_WIDTH-40);
        lineIcon.setIconHeight(1);
        JLabel lineLabel = new JLabel(lineIcon);
        //area for displaying information about publishing mode
        informationArea = new JTextArea(" The “Export” function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.");
        informationArea.setEditable(false);
        informationArea.setLineWrap(true);
        informationArea.setWrapStyleWord(true);
        informationArea.setRows(4);
        informationArea.setBorder(new LineBorder(Color.BLACK));
        //informationArea.setBackground(Color.WHITE);
        informationArea.setSize(PUB_WIDTH-50, PUB_HEIGHT/3);
        JLabel dsLabel = new JLabel("Where would you like to save the map:");
        dataSourceComboBox = new JComboBox(DataSourceViewer.getPublishableDataSources(Publishable.PUBLISH_ALL_MODES));
        dataSourceComboBox.setToolTipText("Select export location.");
        dataSourceComboBox.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(buttonLayout);
        publishMapRButton = new JRadioButton("Export Map");
        publishZipRButton = new JRadioButton("Export to Zip File");
        //publishCMapRButton = new JRadioButton("Export IMSCP Map");
        publishSakaiRButton = new JRadioButton("Export IMSCP Map to Sakai");
        //       publishAllRButton = new JRadioButton("Export All");
        publishMapRButton.setToolTipText("Export map only without local resource files.");
        publishZipRButton.setToolTipText("Export map and local resources to a zip file");
        //    publishCMapRButton.setToolTipText("Export IMS content package that include local resource files.");
        publishSakaiRButton.setToolTipText("Export alreday saved IMS content package to Sakai content hosting.");
//        publishAllRButton.setToolTipText("Export map and local resources as separate files.");
        publishMapRButton.addActionListener(this);
        publishZipRButton.addActionListener(this);
        // publishCMapRButton.addActionListener(this);
        publishSakaiRButton.addActionListener(this);
        //   publishAllRButton.addActionListener(this);
        modeSelectionGroup.add(publishMapRButton);
        modeSelectionGroup.add(publishZipRButton);
        // modeSelectionGroup.add(publishCMapRButton);
        modeSelectionGroup.add(publishSakaiRButton);
        //  modeSelectionGroup.add(publishAllRButton);
        //buttonPanel.add(modeLabel);
        buttonPanel.add(publishMapRButton);
        buttonPanel.add(publishZipRButton);
        // buttonPanel.add(publishCMapRButton);
        buttonPanel.add(publishSakaiRButton);
        //   buttonPanel.add(publishAllRButton);
        JPanel bottomPanel = new JPanel();
        // bottomPanel.setBorder(new LineBorder(Color.BLACK));
        
        bottomPanel.add(nextButton);
        bottomPanel.add(finishButton);
        bottomPanel.add(cancelButton);
        //bottomPanel.setSize(PUB_WIDTH/3, PUB_HEIGHT/10);
        
        
        nextButton.setEnabled(false);
        finishButton.setEnabled(false);
        
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20,5,5, 2);;
        gridbag.setConstraints(topLabel, c);
        modeSelectionPanel.add(topLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(dataSourceComboBox,c);
        modeSelectionPanel.add(dataSourceComboBox);
        
        
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5,5,5, 2);
        gridbag.setConstraints(modeLabel,c);
        modeSelectionPanel.add(modeLabel,c);
        
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        //c.insets = new Insets(10,0,10, 2);
        //c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(buttonPanel, c);
        modeSelectionPanel.add(buttonPanel);
        
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(20,0,0, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(lineLabel, c);
        modeSelectionPanel.add(lineLabel);
        
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10,0,10, 2);
        gridbag.setConstraints(bottomPanel, c);
        modeSelectionPanel.add(bottomPanel);
        
    }
    
    private void  setUpResourceSelectionPanel() {
        resourceSelectionPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        resourceSelectionPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        
        
        JPanel bottomPanel = new JPanel();
        // bottomPanel.setBorder(new LineBorder(Color.BLACK));
        bottomPanel.add(backButton);
        bottomPanel.add(finishButton);
        bottomPanel.add(cancelButton);
        finishButton.setEnabled(true);
        
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 6;
        c.insets = defaultInsets;
        c.anchor = GridBagConstraints.WEST;
        JLabel topLabel = new JLabel("The following objects will be published with the map:");
        gridbag.setConstraints(topLabel,c);
        resourceSelectionPanel.add(topLabel);
        
        c.gridx =0;
        c.gridy =1;
        c.gridheight = 2;
        JPanel resourceListPanel = new JPanel();
        JComponent resourceListPane = getResourceListPane();
        resourceListPanel.add(resourceListPane);
        gridbag.setConstraints(resourceListPanel,c);
        resourceSelectionPanel.add(resourceListPanel);
        
        c.gridy = 3;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(bottomPanel, c);
        resourceSelectionPanel.add(bottomPanel);
        
        // c.insets = new Insets(2, 60,2, 2);
        
    }
    
    private JComponent getResourceListPane() {
        Vector columnNamesVector = new Vector();
        columnNamesVector.add("Selection");
        columnNamesVector.add("Display Name");
        columnNamesVector.add("Size ");
        columnNamesVector.add("Status");
        resourceVector = new Vector();
        setLocalResourceVector(resourceVector,VUE.getActiveMap());
        resourceTableModel = new ResourceTableModel(resourceVector, columnNamesVector);
        resourceTable = new JTable(resourceTableModel);
        
        // setting the cell sizes
        TableColumn column = null;
        Component comp = null;
        int headerWidth;
        int cellWidth;
        TableCellRenderer headerRenderer = resourceTable.getTableHeader().getDefaultRenderer();
        resourceTable.getColumnModel().getColumn(0).setPreferredWidth(12);
        for (int i = 0; i < 4; i++) {
            column = resourceTable.getColumnModel().getColumn(i);
            comp = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(),false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
            cellWidth = resourceTableModel.longValues[i].length();
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
        resourceTable.setPreferredScrollableViewportSize(new Dimension(450,110));
        return new JScrollPane(resourceTable);
    }
    
    
    private void setLocalResourceVector(Vector vector,LWContainer map) {
        Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            System.out.println("Component:"+component+" has resource:"+component.hasResource());
            if(component.hasResource() && (component.getResource() instanceof URLResource)){
                
                URLResource resource = (URLResource) component.getResource();
                System.out.println("Component:"+component+"file:" +resource.getSpec()+" has file:"+resource.getSpec().startsWith(FILE_PREFIX));
                
                //   if(resource.getType() == Resource.URL) {
                try {
                    // File file = new File(new URL(resource.getSpec()).getFile());
                    if(resource.isLocalFile()) {
                        File file = new File(resource.getSpec());
                        System.out.println("LWComponent:"+component.getLabel() + "Resource: "+resource.getSpec()+"File:"+file+" exists:"+file.exists());
                        Vector row = new Vector();
                        row.add(new Boolean(true));
                        row.add(resource);
                        row.add(new Long(file.length()));
                        row.add("Ready");
                        vector.add(row);
                    }
                }catch (Exception ex) {
                    System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
                    ex.printStackTrace();
                }
                
            }
            
        }
    }
    
    public void publishMap(LWMap map) {
        
        try {
            ((Publishable)dataSourceComboBox.getSelectedItem()).publish(Publishable.PUBLISH_MAP,tufts.vue.VUE.getActiveMap());
            this.dispose();
        } catch (Exception ex) {
            alert(VUE.getDialogParent(), "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
    }
    
    public void publishZip() {
        try {
            ((Publishable)dataSourceComboBox.getSelectedItem()).publish(Publishable.PUBLISH_ZIP,tufts.vue.VUE.getActiveMap());
            this.dispose();
        } catch (Exception ex) {
            alert(VUE.getDialogParent(), "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
    }
    public void publishMap() {
        try {
            publishMap((LWMap)VUE.getActiveMap().clone());
        } catch (Exception ex) {
            alert(VUE.getDialogParent(), "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
        
    }
    
    public void publishCMap() {
        try {
            
            ((Publishable)dataSourceComboBox.getSelectedItem()).publish(Publishable.PUBLISH_CMAP,tufts.vue.VUE.getActiveMap());
            
            this.dispose();
        } catch (Exception ex) {
            VueUtil.alert(null, "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
        
    }
    
    public  void publishAll() {
        try {
            ((Publishable)dataSourceComboBox.getSelectedItem()).publish(Publishable.PUBLISH_ALL,tufts.vue.VUE.getActiveMap());
            
            this.dispose();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            VueUtil.alert(null, ex.getMessage(), "Export Error");
        }
    }
    
    
    
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(CANCEL)) {
            this.dispose();
        }else if(e.getActionCommand().equals(NEXT)) {
            getContentPane().remove(rPanel);
            validateTree();
            setUpModeSelectionPanel();
            getContentPane().add(mPanel, BorderLayout.CENTER);
            getContentPane().validate();
            validateTree();
        }else if(e.getActionCommand().equals(PUBLISH)) {
            System.out.println("Publishing Map to Default Fedora Repository");
            publishActiveMapToVUEDL();
        }
    }
   private void   publishActiveMapToVUEDL() {
       FedoraPublisher.uploadMap("https", "vue-dl.tccs.tufts.edu", 8443, "fedoraAdmin", "vuefedora",VUE.getActiveMap());
      
   }
    public void old_actionPerformed(ActionEvent e) {
        if(e.getSource() == cancelButton) {
            this.dispose();
        }
        
        if(e.getSource() == finishButton) {
            this.dispose();
            if(stage == 1) {
                if(publishMapRButton.isSelected())
                    publishMap();
            }else {
                if(publishZipRButton.isSelected())
                    publishZip();
            }
            
        }
        if(e.getSource() == nextButton) {
            this.getContentPane().remove(modeSelectionPanel);
            this.getContentPane().validate();
            this.validateTree();
            if(stage == 1) {
                setUpResourceSelectionPanel();
                this.getContentPane().add(resourceSelectionPanel);
                this.getContentPane().validate();
            }
            stage++;
            
        }
        if(e.getSource() == backButton) {
            this.getContentPane().remove(resourceSelectionPanel);
            setUpModeSelectionPanel();
            modeSelectionPanel.validate();
            this.getContentPane().add(modeSelectionPanel);
            this.getContentPane().validate();
            this.validateTree();
            stage--;
        }
        
        if(e.getSource() == publishMapRButton) {
            finishButton.setEnabled(true);
            nextButton.setEnabled(false);
            publishMode = Publishable.PUBLISH_MAP;
            updatePublishPanel();
        }
        if(e.getSource() == publishZipRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = Publishable.PUBLISH_ZIP;
            updatePublishPanel();
        }
        /*
        if(e.getSource() == publishCMapRButton ) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_CMAP;
            updatePublishPanel();
        }
         */
        if(e.getSource() == publishSakaiRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = Publishable.PUBLISH_SAKAI;
            //updatePublishPanel();
            System.out.println("Sakai Data Sources:");
            try {
                SakaiExport sakaiExport = new SakaiExport(DataSourceViewer.dataSourceManager);
                SakaiCollectionDialog scd = new SakaiCollectionDialog(sakaiExport.getSakaiDataSources());
            } catch (Throwable t) {
                
            }
            this.dispose();
        }
                /*   if(e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_ALL;
            updatePublishPanel();
        }*/
        if(e.getSource() == dataSourceComboBox) {
            Publishable p = (Publishable)dataSourceComboBox.getSelectedItem();
            if(p.supportsMode(Publishable.PUBLISH_MAP))
                publishMapRButton.setEnabled(true);
            else
                publishMapRButton.setEnabled(false);
            //  if(p.supportsMode(Publishable.PUBLISH_CMAP))
            //      publishCMapRButton.setEnabled(true);
            //  else
            //     publishCMapRButton.setEnabled(false);
           /* if(p.supportsMode(Publishable.PUBLISH_ALL))
                publishAllRButton.setEnabled(true);
            else
                publishAllRButton.setEnabled(false);
            */
        }
        
    }
    
    
    private void updatePublishPanel() {
        informationArea.setText(PUBLISH_INFORMATION[publishMode]);
        //this.dataSourceComboBox.setModel(new DefaultComboBoxModel(DataSourceViewer.getPublishableDataSources(publishMode)));
    }
    
    
    private void alert(Component parentComponent,String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(parentComponent,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    
    
    
    public class ResourceTableModel  extends AbstractTableModel {
        
        public final String[] longValues = {"Selection", "12345678901234567890123456789012345678901234567890","12356789","Processing...."};
        Vector data;
        Vector columnNames;
        
        public ResourceTableModel(Vector data,Vector columnNames) {
            super();
            
            this.data = data;
            this.columnNames = columnNames;
        }
        
        
        public int getColumnCount() {
            return columnNames.size();
        }
        
        public int getRowCount() {
            return data.size();
        }
        
        public String getColumnName(int col) {
            return (String)columnNames.elementAt(col);
        }
        
        
        public Object getValueAt(int row, int col) {
            return ((Vector)data.elementAt(row)).elementAt(col);
        }
        
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col > 1) {
                return false;
            } else {
                return true;
            }
        }
        
        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        
        public void setValueAt(Object value, int row, int col) {
            ((Vector)data.elementAt(row)).setElementAt(value,col);
            fireTableCellUpdated(row, col);
        }
    }
    
}

class SpringUtilities {
    /**
     * A debugging utility that prints to stdout the component's
     * minimum, preferred, and maximum sizes.
     */
    public static void printSizes(Component c) {
        System.out.println("minimumSize = " + c.getMinimumSize());
        System.out.println("preferredSize = " + c.getPreferredSize());
        System.out.println("maximumSize = " + c.getMaximumSize());
    }
    
    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component is as big as the maximum
     * preferred width and height of the components.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeGrid(Container parent,
            int rows, int cols,
            int initialX, int initialY,
            int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeGrid must use SpringLayout.");
            return;
        }
        
        Spring xPadSpring = Spring.constant(xPad);
        Spring yPadSpring = Spring.constant(yPad);
        Spring initialXSpring = Spring.constant(initialX);
        Spring initialYSpring = Spring.constant(initialY);
        int max = rows * cols;
        
        //Calculate Springs that are the max of the width/height so that all
        //cells have the same size.
        Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).
                getWidth();
        Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).
                getWidth();
        for (int i = 1; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                    parent.getComponent(i));
            
            maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
        }
        
        //Apply the new width/height Spring. This forces all the
        //components to have the same size.
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                    parent.getComponent(i));
            
            cons.setWidth(maxWidthSpring);
            cons.setHeight(maxHeightSpring);
        }
        
        //Then adjust the x/y constraints of all the cells so that they
        //are aligned in a grid.
        SpringLayout.Constraints lastCons = null;
        SpringLayout.Constraints lastRowCons = null;
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                    parent.getComponent(i));
            if (i % cols == 0) { //start of new row
                lastRowCons = lastCons;
                cons.setX(initialXSpring);
            } else { //x position depends on previous component
                cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST),
                        xPadSpring));
            }
            
            if (i / cols == 0) { //first row
                cons.setY(initialYSpring);
            } else { //y position depends on previous row
                cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH),
                        yPadSpring));
            }
            lastCons = cons;
        }
        
        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH,
                Spring.sum(
                Spring.constant(yPad),
                lastCons.getConstraint(SpringLayout.SOUTH)));
        pCons.setConstraint(SpringLayout.EAST,
                Spring.sum(
                Spring.constant(xPad),
                lastCons.getConstraint(SpringLayout.EAST)));
    }
    
    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(
            int row, int col,
            Container parent,
            int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }
    
    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeCompactGrid(Container parent,
            int rows, int cols,
            int initialX, int initialY,
            int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }
        
        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                        getConstraintsForCell(r, c, parent, cols).
                        getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }
        
        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                        getConstraintsForCell(r, c, parent, cols).
                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }
        
        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }
}

