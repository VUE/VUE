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
import java.util.Properties;


import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
import fedora.client.ingest.AutoIngestor;

import tufts.vue.action.*;
/**
 *
 * @author  akumar03
 */
public class Publisher extends JDialog implements ActionListener {
    
    /** Creates a new instance of Publisher */
    public final int PUBLISH_MAP = 0; // just the map
    public final int PUBLISH_CMAP = 1; // the map with selected resources in IMSCP format
    public final int PUBLISH_ALL = 2; // all resources published to fedora and map published with pointers to resources.
    public final int SELECTION_COL = 0; // boolean selection column in resource table
    public final int RESOURCE_COL = 1; // column that displays the name of resource
    public final int SIZE_COL = 2; // the column that displays size of files.
    public final int STATUS_COL = 3;// the column that displays the status of objects that will be ingested.
    public final int X_LOCATION = 300; // x co-ordinate of location where the publisher appears
    public final int Y_LOCATION = 300; // y co-ordinate of location where the publisher appears
    public final int WIDTH = 600; 
    public final int HEIGHT = 250;
    public final String[] PUBLISH_INFORMATION = {" “Export Map” saves only the map. Digital resources are not attached, but the resources’ paths are maintained. “Export Map” is the equivalent of the “Save” function for a registered digital repository.",
                          "“Export IMSCP Map” embeds digital resources within the map. The resources are accessible to all users viewing the map. This mode creates a “zip” file, which can be uploaded to a registered digital repository or saved locally. VUE can open zip files it originally created. (IMSCP: Instructional Management Services Content Package.)",
                          "“Export All” creates a duplicate of all digital resources and uploads these resources and the map to a registered digital repository. The resources are accessible to all users viewing the map."

                          };
    
    private int publishMode = PUBLISH_MAP; 
    private final int BUFFER_SIZE = 10240;// size for transferring files
    private int stage; // keep tracks of the screen
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
        Vector dataSourceVector = new Vector();
        Iterator  i = DataSourceViewer.getPublishableDataSources();
        while(i.hasNext())  
            dataSourceVector.add(i.next());
        
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
        informationArea.setRows(4);
        informationArea.setBorder(new LineBorder(Color.BLACK));
        //informationArea.setBackground(Color.WHITE);
        informationArea.setSize(WIDTH-50, HEIGHT/3);
        
        JLabel dsLabel = new JLabel("Where would you like to save the map:");
        dataSourceComboBox = new JComboBox(dataSourceVector);
        
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
           // comp = resourceTable.getDefaultRenderer(resourceTableModel.getColumnClass(i)).getTableCellRendererComponent(resourceTable,resourceTableModel.longValues[i],false, false, 0, i);
            cellWidth = resourceTableModel.longValues[i].length();
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
       }
        resourceTable.setPreferredScrollableViewportSize(new Dimension(500,100));
      //  resourceList.setDefaultRenderer(String.class,resourceTableCellRenderer);
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
                        System.out.println("Resource = "+resource.getSpec());
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
    
    public void publishMap() {
        try {
            saveActiveMap();
            Properties metadata = VUE.getActiveMap().getMetadata();
            String pid = getDR().ingest(activeMapFile.getName(), "obj-binary.xml", activeMapFile, metadata).getIdString();
            JOptionPane.showMessageDialog(null, "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Exported Map: id = "+pid);
            this.dispose();
        } catch (Exception ex) {
             VueUtil.alert(null,  "Export Not Supported:"+ex.getMessage(), "Export Error");
             ex.printStackTrace();
        }
    }
    
    public void publishCMap() {
        try {
            File savedCMap = createIMSCP();
            Properties metadata  = VUE.getActiveMap().getMetadata();
            String pid = getDR().ingest(savedCMap.getName(), "obj-vue-concept-map-mc.xml", savedCMap, metadata).getIdString();
            JOptionPane.showMessageDialog(null, "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Exported Map: id = "+pid);
            this.dispose();
        } catch (Exception ex) {
             VueUtil.alert(null, "Export Not Supported:"+ex.getMessage(), "Export Error");
             ex.printStackTrace();
        }
   
    }
    
    public  void publishAll() {
        try {
            
            Iterator i = resourceVector.iterator();
            while(i.hasNext()) {
                Vector vector = (Vector)i.next();
                Resource r = (Resource)(vector.elementAt(1));
                Boolean b = (Boolean)(vector.elementAt(0));
               // File file = new File((String)vector.elementAt(1));
                System.out.println("RESOURCE = "+r.getSpec());
                File file = new File(new URL(r.getSpec()).getFile());
                if(file.isFile() && b.booleanValue()) {
                    resourceTable.getModel().setValueAt("Processing",resourceVector.indexOf(vector),STATUS_COL);
                    String pid = getDR().ingest(file.getName(),"obj-binary.xml",file, r.getProperties()).getIdString();
                    resourceTable.getModel().setValueAt("Done",resourceVector.indexOf(vector),STATUS_COL);
                    VUE.getActiveMap().replaceResource(r,new AssetResource(getDR().getAsset(new tufts.oki.dr.fedora.PID(pid))));
                    System.out.println("Resource = " + r+ " FileName = "+file.getName()+" pid ="+pid+" vector ="+resourceVector.indexOf(vector)+" table value= "+resourceTable.getValueAt(resourceVector.indexOf(vector),STATUS_COL));
                    
                }
              
            }
            publishMap();
            System.out.println("Export All");
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Export Error");
            ex.printStackTrace();
        }
    }
    
    private void saveActiveMap() throws IOException {
        LWMap map = tufts.vue.VUE.getActiveMap();
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "vueMap";
            String suffix = ".xml";
            activeMapFile  = File.createTempFile(prefix,suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);    
    }
    
    
  
    
    private File createIMSCP() throws IOException,URISyntaxException {
        
        LWMap map = tufts.vue.VUE.getActiveMap();
        IMSCP imscp = new IMSCP();
        saveActiveMap();
        System.out.println("Writing Active Map : "+activeMapFile.getName());
        imscp.putEntry(IMSCP.MAP_FILE,activeMapFile);        
        Iterator i = resourceVector.iterator();       
        while(i.hasNext()) {
            Vector vector = (Vector)i.next();
            Resource r = (Resource)(vector.elementAt(1));
            Boolean b = (Boolean)(vector.elementAt(0));
            File file = new File(r.getSpec());
               
           
            //File file = new File((String)vector.elementAt(1));
            if(file.isFile() && b.booleanValue()) {
                 System.out.println("FileName = "+file.getName()+" index ="+resourceVector.indexOf(vector));
                 resourceTable.setValueAt("Processing",resourceVector.indexOf(vector),STATUS_COL);
                 imscp.putEntry(IMSCP.RESOURCE_FILES+"/"+file.getName(),file);
                 resourceTable.setValueAt("Done",resourceVector.indexOf(vector),STATUS_COL);
            }    
           
        }   
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
            informationArea.setText(PUBLISH_INFORMATION[PUBLISH_MAP]);
        }
        if(e.getSource() == publishCMapRButton || e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_CMAP;
            informationArea.setText(PUBLISH_INFORMATION[PUBLISH_CMAP]);
        }
        if(e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_ALL;
            informationArea.setText(PUBLISH_INFORMATION[PUBLISH_ALL]);
        }
        
    }
    private tufts.oki.dr.fedora.DR getDR() {
        return ((tufts.oki.dr.fedora.DR)((DRViewer)((DataSource)dataSourceComboBox.getSelectedItem()).getResourceViewer()).getDR());
    }
 
    public class ResourceTableModel  extends AbstractTableModel {
        
        public final String[] longValues = {"Selection", "123456789012345678901234567890","12356789","Processing...."};
        Vector data;
        Vector columnNames;
                
        public ResourceTableModel (Vector data,Vector columnNames) {
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
