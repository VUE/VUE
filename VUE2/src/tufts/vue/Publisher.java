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
import fedora.client.ingest.AutoIngestor;

import tufts.vue.action.*;
/**
 *
 * @author  akumar03
 */
public class Publisher extends JDialog implements ActionListener,tufts.vue.DublinCoreConstants {
    
    /** Creates a new instance of Publisher */
    
    public static final int PUBLISH_NO_MODES = 0;;
    public static final int PUBLISH_MAP = 1; // just the map
    public static final int PUBLISH_CMAP = 2; // the map with selected resources in IMSCP format
    public static final int PUBLISH_ALL = 3; // all resources published to fedora and map published with pointers to resources.
    public static final int PUBLISH_ALL_MODES = 10; // this means that datasource can publish to any mode.
    //todo: create a pubishable interface for Datatasources. Create an interface for datasources and have separate implementations for each type of datasource.
    
    public final int SELECTION_COL = 0; // boolean selection column in resource table
    public final int RESOURCE_COL = 1; // column that displays the name of resource
    public final int SIZE_COL = 2; // the column that displays size of files.
    public final int STATUS_COL = 3;// the column that displays the status of objects that will be ingested.
    public final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    public final int WIDTH = 600;
    public final int HEIGHT = 250;
    public final String[] PUBLISH_INFORMATION = {" The “Export” function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.",
    " “Export Map” saves only the map. Digital resources are not attached, but the resources’ paths are maintained. “Export Map” is the equivalent of the “Save” function for a registered digital repository.",
    "“Export IMSCP Map” embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a “zip” file, which can be uploaded to a registered digital repository or saved locally. VUE can open zip files it originally created. (IMSCP: Instructional Management Services Content Package.)",
    "“Export All” creates a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resources are accessible to all users viewing the map."
    
    };
    
    private int publishMode = PUBLISH_MAP;
    private final int BUFFER_SIZE = 10240;// size for transferring files
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
    JButton cancelButton;
    JButton nextButton;
    JButton backButton;
    JButton finishButton;
    JRadioButton publishMapRButton;
    JRadioButton publishCMapRButton;
    JRadioButton publishAllRButton;
    JTextArea informationArea;
    Vector resourceVector;
    File activeMapFile;
    ResourceTableModel resourceTableModel;
    JTable resourceTable;
    JComboBox dataSourceComboBox;
    
    public Publisher(Frame owner,String title) {
        //testing
        super(owner,title);
        
        nextButton = new JButton("Next >");
        finishButton = new JButton("Finish");
        cancelButton = new JButton("Cancel");
        backButton = new JButton("< Back");
        cancelButton.addActionListener(this);
        finishButton.addActionListener(this);
        nextButton.addActionListener(this);
        backButton.addActionListener(this);
        setUpModeSelectionPanel();
        
        getContentPane().add(modeSelectionPanel);
        
        stage = 1;
        
        setLocation(X_LOCATION,Y_LOCATION);
        setModal(true);
        setSize(WIDTH,HEIGHT);
        setResizable(false);
        show();
        
    }
    
    private void setUpModeSelectionPanel() {
        
        modeSelectionPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        modeSelectionPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,9,2,2);
        
        ButtonGroup modeSelectionGroup = new ButtonGroup();
        JLabel topLabel = new JLabel("Select the Export Mode");
        
        //area for displaying information about publishing mode
        informationArea = new JTextArea(" The “Export” function allows a user to deposit a concept map into a registered digital repository. Select the different modes to learn more.");
        informationArea.setEditable(false);
        informationArea.setLineWrap(true);
        informationArea.setWrapStyleWord(true);
        informationArea.setRows(4);
        informationArea.setBorder(new LineBorder(Color.BLACK));
        //informationArea.setBackground(Color.WHITE);
        informationArea.setSize(WIDTH-50, HEIGHT/3);
        
        JLabel dsLabel = new JLabel("Where would you like to save the map:");
        dataSourceComboBox = new JComboBox(DataSourceViewer.getPublishableDataSources(Publisher.PUBLISH_ALL_MODES));
        
        JPanel buttonPanel = new JPanel();
        publishMapRButton = new JRadioButton("Export Map");
        publishCMapRButton = new JRadioButton("Export IMSCP Map");
        publishAllRButton = new JRadioButton("Export All");
        publishMapRButton.addActionListener(this);
        publishCMapRButton.addActionListener(this);
        publishAllRButton.addActionListener(this);
        
        modeSelectionGroup.add(publishMapRButton);
        modeSelectionGroup.add(publishCMapRButton);
        modeSelectionGroup.add(publishAllRButton);
        buttonPanel.add(publishMapRButton);
        buttonPanel.add(publishCMapRButton);
        buttonPanel.add(publishAllRButton);
        
        JPanel bottomPanel = new JPanel();
        // bottomPanel.setBorder(new LineBorder(Color.BLACK));
        bottomPanel.add(nextButton);
        bottomPanel.add(finishButton);
        bottomPanel.add(cancelButton);
        //bottomPanel.setSize(WIDTH/3, HEIGHT/10);
        
        
        nextButton.setEnabled(false);
        finishButton.setEnabled(false);
        
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 6;
        c.anchor = GridBagConstraints.WEST;
        c.insets = defaultInsets;
        gridbag.setConstraints(topLabel, c);
        modeSelectionPanel.add(topLabel);
        
        c.gridy = 1;
        c.insets = new Insets(2,0,2, 2);
        gridbag.setConstraints(buttonPanel, c);
        modeSelectionPanel.add(buttonPanel);
        
        c.gridy = 2;
        c.gridwidth = 6;
        c.insets = defaultInsets;
        gridbag.setConstraints(informationArea, c);
        modeSelectionPanel.add(informationArea);
        
        c.gridy = 3;
        c.gridwidth =2;
        c.insets = new Insets(10,9,2, 2);
        gridbag.setConstraints(dsLabel,c);
        modeSelectionPanel.add(dsLabel);
        
        c.gridy = 4;
        c.gridwidth =2;
        c.insets = defaultInsets;
        gridbag.setConstraints(dataSourceComboBox,c);
        modeSelectionPanel.add(dataSourceComboBox);
        
        c.gridy = 5;
        c.gridx = 3;
        c.gridwidth =3;
        c.anchor = GridBagConstraints.EAST;
        c.insets = defaultInsets;
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
        for (int i = 1; i < 4; i++) {
            column = resourceTable.getColumnModel().getColumn(i);
            comp = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(),false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
            cellWidth = resourceTableModel.longValues[i].length();
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
        resourceTable.setPreferredScrollableViewportSize(new Dimension(500,100));
        return new JScrollPane(resourceTable);
    }
    
    
    private void setLocalResourceVector(Vector vector,LWContainer map) {
        Iterator i = map.getChildIterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource()){
                Resource resource = component.getResource();
                //   if(resource.getType() == Resource.URL) {
                try {
                    // File file = new File(new URL(resource.getSpec()).getFile());
                    File file = new File(new URL(resource.getSpec()).getFile());
                    if(file.isFile()) {
                        Vector row = new Vector();
                        row.add(new Boolean(true));
                        row.add(resource);
                        row.add(new Long(file.length()));
                        row.add("Ready");
                        vector.add(row);
                    }
                }catch (Exception ex) {
                    System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
                }
                // }
            }
            if(component instanceof LWContainer) {
                setLocalResourceVector(vector,(LWContainer)component);
            }
        }
    }
    
    public void publishMap(LWMap map) {
        
        try {
            saveMap(map);
            Properties metadata = map.getMetadata();
            
            String pid = getDR().ingest(activeMapFile.getName() ,"obj-binary.xml", VUE_MIME_TYPE,activeMapFile, metadata).getIdString();
            JOptionPane.showMessageDialog(VUE.getInstance(), "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Exported Map: id = "+pid);
            this.dispose();
        } catch (Exception ex) {
            alert(VUE.getInstance(),  "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
    }
    
    public void publishMap() {
        try {
            publishMap((LWMap)VUE.getActiveMap().clone());
        } catch (Exception ex) {
            alert(VUE.getInstance(),  "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
        
    }
    
    
    
    public void publishCMap() {
        try {
            File savedCMap = createIMSCP();
            if(((DataSource)dataSourceComboBox.getSelectedItem()).getType() == DataSource.FILING_LOCAL) {
                InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("IMSCP","zip")));
                
                int fileLength = (int)savedCMap.length();
                byte bytes[] = new  byte[fileLength];
                while (istream.read(bytes,0,fileLength) != -1)
                    ostream.write(bytes,0,fileLength);
                istream.close();
                ostream.close();
            } else {
                Properties metadata  = VUE.getActiveMap().getMetadata();
                String pid = getDR().ingest(savedCMap.getName(), "obj-vue-concept-map-mc.xml",ZIP_MIME_TYPE, savedCMap, metadata).getIdString();
                JOptionPane.showMessageDialog(VUE.getInstance(), "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Exported Map: id = "+pid);
                
            }
            
            this.dispose();
        } catch (Exception ex) {
            VueUtil.alert(null, "Export Not Supported:"+ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
        
    }
    
    public  void publishAll() {
        try {
            LWMap saveMap = (LWMap) tufts.vue.VUE.getActiveMap().clone();
            
            Iterator i = resourceVector.iterator();
            while(i.hasNext()) {
                Vector vector = (Vector)i.next();
                Resource r = (Resource)(vector.elementAt(1));
                Boolean b = (Boolean)(vector.elementAt(0));
                // File file = new File((String)vector.elementAt(1));
                System.out.println("RESOURCE = "+r.getSpec());
                URL url = new URL(r.getSpec());
                File file = new File(url.getFile());
                if(file.isFile() && b.booleanValue()) {
                    resourceTable.getModel().setValueAt("Processing",resourceVector.indexOf(vector),STATUS_COL);
                    String pid = getDR().ingest(file.getName(),"obj-binary.xml",url.openConnection().getContentType(),file, r.getProperties()).getIdString();
                    resourceTable.getModel().setValueAt("Done",resourceVector.indexOf(vector),STATUS_COL);
                    replaceResource(saveMap,r,new AssetResource(getDR().getAsset(new tufts.oki.dr.fedora.PID(pid))));
                    
                }
                
            }
            publishMap(saveMap);
            this.dispose();
            System.out.println("Export All");
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
    }
    
    private void saveActiveMap() throws IOException, CloneNotSupportedException {
        LWMap map = (LWMap) tufts.vue.VUE.getActiveMap().clone();
        
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "concept_map";
            String suffix = ".vue";
            activeMapFile  = File.createTempFile(prefix,suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);
    }
    
    private void saveMap(LWMap map) throws IOException {
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "concept_map";
            String suffix = ".vue";
            activeMapFile  = File.createTempFile(prefix,suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);
        
    }
    
    
    
    
    private File createIMSCP() throws IOException,URISyntaxException,CloneNotSupportedException {
        String IMSCPMetadata = "";
        String IMSCPOrganization ="";
        String IMSCPResources = "";
        int resourceCount =2; //resourceIdentifier 1 is used for map
        IMSCPMetadata += getMetadataString(tufts.vue.VUE.getActiveMap().getMetadata());
        IMSCPResources += getResourceTag(tufts.vue.VUE.getActiveMap().getMetadata(), IMSCP.MAP_FILE,1);
        IMSCPOrganization += "<organization identifier=\"TOC1\" structure=\"hierarchical\">";
        IMSCPOrganization += "<title>IMS Content Package of VUE Map</title> ";
        IMSCPOrganization += "<item identifier=\"ITEM1\" identifierref=\"RESOURCE1\">";
        IMSCPOrganization += "<title> VUE Cocept Map</title>";            
        LWMap saveMap = (LWMap) tufts.vue.VUE.getActiveMap().clone();
        IMSCP imscp = new IMSCP();
        Iterator i = resourceVector.iterator();
        while(i.hasNext()) {
            Vector vector = (Vector)i.next();
            Resource r = (Resource)(vector.elementAt(1));
            Boolean b = (Boolean)(vector.elementAt(0));
            File file = new File(new URL(r.getSpec()).getFile());
            //File file = new File((String)vector.elementAt(1));
            if(file.isFile() && b.booleanValue()) {
                System.out.println("FileName = "+file.getName()+" index ="+resourceVector.indexOf(vector));
                resourceTable.setValueAt("Processing",resourceVector.indexOf(vector),STATUS_COL);
                String entry = IMSCP.RESOURCE_FILES+"/"+file.getName();
                imscp.putEntry(entry,file);
                IMSCPResources += getResourceTag(r.getProperties(),entry,resourceCount);
                IMSCPOrganization += getItemTag("ITEM"+resourceCount, "RESOURCE"+resourceCount,"Resource "+resourceCount+" in Concept Map");
                resourceTable.setValueAt("Done",resourceVector.indexOf(vector),STATUS_COL);
                replaceResource(saveMap,r,new MapResource(IMSCP.RESOURCE_FILES+"/"+file.getName()));
                resourceCount++;
            }
        }
        saveMap(saveMap);
        imscp.putEntry(IMSCP.MAP_FILE,activeMapFile);
        IMSCPOrganization +="</item>";  
        IMSCPOrganization +="</organization>";  
        IMSManifest = readRawManifest();
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_METADATA, IMSCPMetadata).trim();
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_ORGANIZATION, IMSCPOrganization);
        IMSManifest = IMSManifest.replaceAll(IMSCP_MANIFEST_RESOURCES, IMSCPResources);
          
        File IMSManifestFile = File.createTempFile("imsmanifest",".xml");
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(IMSManifestFile));
        fos.write(IMSManifest.getBytes());
        fos.close();
        imscp.putEntry(IMSCP.MANIFEST_FILE,IMSManifestFile);
        System.out.println("Writing Active Map : "+activeMapFile.getName());
        imscp.closeZOS();
        return imscp.getFile();
    }
    
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == cancelButton) {
            this.dispose();
        }
        if(e.getSource() == finishButton) {
            if(stage == 1) {
                if(publishMapRButton.isSelected())
                    publishMap();
            }else {
                if(publishCMapRButton.isSelected())
                    publishCMap();
                if(publishAllRButton.isSelected())
                    publishAll();
            }
        }
        if(e.getSource() == nextButton) {
            this.getContentPane().remove(modeSelectionPanel);
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
            stage--;
        }
        
        if(e.getSource() == publishMapRButton) {
            finishButton.setEnabled(true);
            nextButton.setEnabled(false);
            publishMode = PUBLISH_MAP;
            updatePublishPanel();
        }
        if(e.getSource() == publishCMapRButton || e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_CMAP;
            updatePublishPanel();
        }
        if(e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_ALL;
            updatePublishPanel();
        }
        
    }
    
    private  void replaceResource(LWMap map,Resource r1,Resource r2) {
        Iterator i = map.getAllDescendentsIterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource()){
                Resource resource = component.getResource();
                if(resource.getSpec().equals(r1.getSpec()))
                    component.setResource(r2);
            }
        }
    }
    private void updatePublishPanel() {
        informationArea.setText(PUBLISH_INFORMATION[publishMode]);
        this.dataSourceComboBox.setModel(new DefaultComboBoxModel(DataSourceViewer.getPublishableDataSources(publishMode)));
    }
    private tufts.oki.dr.fedora.DR getDR() {
        return ((tufts.oki.dr.fedora.DR)((DRViewer)((DataSource)dataSourceComboBox.getSelectedItem()).getResourceViewer()).getDR());
    }
    
    private void alert(javax.swing.JFrame frame,String message,String title) {
        javax.swing.JOptionPane.showMessageDialog(frame,message,title,javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    public static boolean isSupportedMetadataField(String field){
        for(int i=0;i<DC_FIELDS.length;i++) {
            if(DC_FIELDS[i].equalsIgnoreCase(field))
                return true;
        }
        return false;
    }
    public static String getMetadataString(Properties dcFields) {
        String metadata = "";
        Enumeration e = dcFields.keys();
        while(e.hasMoreElements()) {
            String field = (String)e.nextElement();
            if(isSupportedMetadataField(field))
                metadata += "<"+DC_NAMESPACE+field+">"+dcFields.getProperty(field)+"</"+DC_NAMESPACE+field+">";
        }
        return metadata;
    }  
    private String getResourceTag(Properties dcFields,String entry,int resourceCount) {
        String resourceTag = "";
        String identifier = "RESOURCE"+resourceCount;
        resourceTag = "<resource identifier=\""+identifier+"\" type=\"webcontent\" href=\""+entry+"\">\n";
        resourceTag += "<file  href=\""+entry+"\"/>\n";
        resourceTag += "<metadata>\n";
        resourceTag += "<schema>Dublin Core</schema> \n";
        resourceTag += "<schemaversion>1.1</schemaversion> \n";
        resourceTag += getMetadataString(dcFields);
        resourceTag += "</metadata>\n";
        resourceTag += "</resource>\n";
        return resourceTag;
    }
    
    private String getItemTag(String item,String resource, String title) {
        String itemTag = "";
        itemTag += "<item identifier=\""+item+"\" identifierref=\""+resource+"\">";
        itemTag += "<title>"+title+"</title>";
        itemTag += "</item>";
        return itemTag;
    }
    private String readRawManifest() {
        String s = "";
        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(VueResources.getFile("imsmanifest")));
            byte[] buf = new byte[BUFFER_SIZE];
            int ch;
            int len;
            while((len =fis.read(buf)) > 0) {
                s = s+ new String(buf);
            }
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }
    public class ResourceTableModel  extends AbstractTableModel {
        
        public final String[] longValues = {"Selection", "123456789012345678901234567890","12356789","Processing...."};
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
