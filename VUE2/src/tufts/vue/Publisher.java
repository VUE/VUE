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
    private final int PUBLISH_MAP = 0; // just the map
    private final int PUBLISH_CMAP = 1; // the map with selected resources in IMSCP format
    private final int PUBLISH_ALL = 2; // all resources published to fedora and map published with pointers to resources.
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
    Vector resourceVector;
    File activeMapFile;
    ResourceTableModel resourceTableModel;
    JTable resourceTable;
    
    public Publisher() {
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
        
        setLocation(300,300);
        setModal(true);
        setSize(600, 250);
        setResizable(false);
        show();
       
    }
    
    private void setUpModeSelectionPanel() {
        modeSelectionPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        modeSelectionPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        
        ButtonGroup modeSelectionGroup = new ButtonGroup();
        JLabel topLabel = new JLabel("Select the Publish Mode");
        JLabel dsLabel = new JLabel("Datasource selection");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));
        publishMapRButton = new JRadioButton("Publish Map");
        publishCMapRButton = new JRadioButton("Publish CMAP");
        publishAllRButton = new JRadioButton("Publish All");
        publishMapRButton.addActionListener(this);
        publishCMapRButton.addActionListener(this);
        publishAllRButton.addActionListener(this);
        
        nextButton.setEnabled(false);
        finishButton.setEnabled(false);
        
        
        modeSelectionGroup.add(publishMapRButton);
        modeSelectionGroup.add(publishCMapRButton);
        modeSelectionGroup.add(publishAllRButton);
        buttonPanel.add(publishMapRButton);
        buttonPanel.add(publishCMapRButton);
        buttonPanel.add(publishAllRButton);
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.insets = defaultInsets;
        gridbag.setConstraints(topLabel, c);
        modeSelectionPanel.add(topLabel);
        
        c.gridx = 0;
        c.gridy = 1;
        gridbag.setConstraints(buttonPanel, c);
        modeSelectionPanel.add(buttonPanel);
        
        c.gridx = 0;
        c.gridy = 2;
        gridbag.setConstraints(dsLabel,c);
        modeSelectionPanel.add(dsLabel);
        
        c.gridy = 3;
        c.gridx = 1;
        c.gridwidth = 1;
        c.insets = new Insets(2, 80,2, 2);
        gridbag.setConstraints(nextButton, c);
        modeSelectionPanel.add(nextButton);
        
        c.gridx = 2;
        c.insets = defaultInsets;
        gridbag.setConstraints(finishButton, c);
        modeSelectionPanel.add(finishButton);
        
        c.gridx = 3;
        gridbag.setConstraints(cancelButton,c);
        modeSelectionPanel.add(cancelButton);   
    }
    
    private void  setUpResourceSelectionPanel() {
        resourceSelectionPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        resourceSelectionPanel.setLayout(gridbag);
        Insets defaultInsets = new Insets(2,2,2,2);
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.insets = defaultInsets;
        JLabel topLabel = new JLabel("Following Objects will be ingested");
        gridbag.setConstraints(topLabel,c);
        resourceSelectionPanel.add(topLabel);
        
        c.gridx =0;
        c.gridy =1;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        JPanel resourceListPanel = new JPanel();
        JComponent resourceListPane = getResourceListPane();
        resourceListPanel.add(resourceListPane);
        gridbag.setConstraints(resourceListPanel,c);
        resourceSelectionPanel.add(resourceListPanel);
        
        c.gridy = 3;
        c.gridx = 1;
        c.gridwidth = 1;
        c.insets = new Insets(2, 80,2, 2);
        gridbag.setConstraints(backButton, c);
        resourceSelectionPanel.add(backButton);
        
        c.gridx = 2;
        c.insets = defaultInsets;
        gridbag.setConstraints(finishButton, c);
        resourceSelectionPanel.add(finishButton);
        finishButton.setEnabled(true);
        
        c.gridx = 3;
        gridbag.setConstraints(cancelButton,c);
        resourceSelectionPanel.add(cancelButton);   
        
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
        resourceTable.setPreferredScrollableViewportSize(new Dimension(500, 70));

      //  resourceList.setDefaultRenderer(String.class,resourceTableCellRenderer);
        return new JScrollPane(resourceTable);
        
        
    }
    private void setLocalResourceVector(Vector vector,LWContainer map) {

       
       Iterator i = map.getChildIterator();

       while(i.hasNext()) {
           LWComponent component = (LWComponent) i.next();
           if(component.hasResource()){
               Resource resource = component.getResource();
               if(resource.isLocalFile()) {
                   File file = new File(resource.toURLString().substring(8));
                    if(file.isFile()) {
                        Vector row = new Vector();
                        row.add(new Boolean(true));
                        row.add(resource);
                        row.add(new Long(file.length()));
                        row.add("Ready");
                        vector.add(row);
                    }
               }
           }
           if(component instanceof LWContainer) {
                setLocalResourceVector(vector,(LWContainer)component);
           }
       }
    }
    
    public void publishMap() {
        try {
            saveActiveMap();
            String transferredFileName = transferFile(activeMapFile,activeMapFile.getName());
            File METSfile = createMETSFile( transferredFileName,"obj-binary.xml");
            String pid = ingestToFedora(METSfile);
            System.out.println("Published Map: id = "+pid);
        } catch (Exception ex) {
             VueUtil.alert(null, ex.getMessage(), "Publish Error");
        }
    }
    
    public void publishCMap() {
        try {
            File savedCMap = createIMSCP();
           // String transferredFileNameLocal = activeMapFile.getName().split("\\.")[0] +".zip";
            String transferredFileName = transferFile(savedCMap,savedCMap.getName());
            File METSfile = createMETSFile( transferredFileName,"obj-vue-concept-map-mc.xml");
            String pid = ingestToFedora(METSfile);
            System.out.println("Published CMap: id = "+pid);
        } catch (Exception ex) {
             VueUtil.alert(null, ex.getMessage(), "Publish Error");
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
            File file = new File(r.getSpec().substring(8));
            if(file.isFile() && b.booleanValue()) {
                 String transferredFileName = transferFile(file,file.getName());
                 File METSFile = createMETSFile( transferredFileName,"obj-binary.xml");
                 String pid = ingestToFedora(METSFile);
                 System.out.println("Resource = " + r+"size = "+r.getSize()+ " FileName = "+file.getName()+" pid ="+pid);
              
            }    
           publishMap();
        }
            System.out.println("Publish All");
        } catch (Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
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
    
    private String  transferFile(File  file,String fileName) throws IOException,osid.filing.FilingException {
        String host = "dl.tccs.tufts.edu";
        String url = "http://dl.tccs.tufts.edu/~vue/fedora/";
        int port = 21;
        String userName = "vue";
        String password = "vue@at";
        String directory = "public_html/fedora";
        
       // saveActiveMap(); // saving the activeMap;
        
        // transfering it to web-server
          
        FTPClient client = new FTPClient();
        client.connect(host,port);
        client.login(userName,password);
        client.changeWorkingDirectory(directory); 
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.storeFile(fileName,new FileInputStream(file));
        client.logout();
        client.disconnect();
        fileName = url+fileName;
        return fileName;
    }
    
    private File createMETSFile(String fileName,String templateFileName) throws IOException, FileNotFoundException, javax.xml.rpc.ServiceException{
        StringBuffer sb = new StringBuffer();
        String s = new String();
        FileInputStream fis = new FileInputStream(new File(templateFileName));
        DataInputStream in = new DataInputStream(fis); 

        byte[] buf = new byte[BUFFER_SIZE];
        int ch;
        int len;
        while((len =fis.read(buf)) > 0) {
            s = s+ new String(buf);
          
        }
        fis.close();
        in.close();
      //  s = sb.toString();
        String r =  s.replaceAll("%file.location%", fileName).trim();
        
        //writing the to outputfile
        File METSfile = File.createTempFile("vueMETSMap",".xml");
        FileOutputStream fos = new FileOutputStream(METSfile);
        fos.write(r.getBytes());
        fos.close();
        return METSfile;
        
    }
    
    private String ingestToFedora(File METSfile) throws IOException, FileNotFoundException, javax.xml.rpc.ServiceException{
        AutoIngestor a = new AutoIngestor("130.64.77.144", 8080,"fedoraAdmin","fedoraAdmin");
        String pid = a.ingestAndCommit(new FileInputStream(METSfile),"Test Ingest");
        
        System.out.println(" METSfile= " + METSfile.getPath()+" PID = "+pid);
        return pid;
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
            File file = new File(r.getSpec().substring(8));
            if(file.isFile() && b.booleanValue()) {
                 System.out.println("Resource = " + r+"size = "+r.getSize()+ " FileName = "+file.getName()+" index ="+vector.indexOf(r));
                 resourceTable.setValueAt("Processing",resourceVector.indexOf(vector),3);
                 imscp.putEntry(IMSCP.RESOURCE_FILES+"/"+file.getName(),file);
                 resourceTable.setValueAt("Done",resourceVector.indexOf(vector),3);
                 
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
                
            this.dispose();
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
        }
        if(e.getSource() == publishCMapRButton || e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_CMAP;
        }
        if(e.getSource() == publishAllRButton) {
            finishButton.setEnabled(false);
            nextButton.setEnabled(true);
            publishMode = PUBLISH_ALL;
        }
        
    }
 
    public class ResourceTableModel  extends AbstractTableModel {
        
        public final Object[] longValues = {"Selection", "123456789012345678901234567890","12356789","Processing...."};
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
