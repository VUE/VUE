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
    Vector resources;
    
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
        getContentPane().setLayout(new GridLayout(1,0));
        getContentPane().add(modeSelectionPanel);
        getContentPane().setSize(300, 500);
        stage = 1;
        setLocation(300,300);
        setModal(true);
        pack();
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
        //c.fill = GridBagConstraints.HORIZONTAL;
        JPanel resourceListPanel = new JPanel();
        JComponent resourceListPane = getResourceListPane();
        resourceListPanel.add(resourceListPane);
        resourceListPanel.setSize(500, 200);
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
        String[] columnNames = {"Selection","Display Name","Size","Status"};
        //String[] columnNames = {"Selecetd","Display Name","Size"};
        Object[][] resources = {{new Boolean(true),"readme.txt",new Integer(1)},
                                {new Boolean(true),"readme.txt",new Integer(1)},
                                {new Boolean(true),"readme.txt",new Integer(1)}};
        //setLocalResourceObject(resources,VUE.getActiveMap());
                                
        Vector columnNamesVector = new Vector();
        columnNamesVector.add("Selection");
        columnNamesVector.add("Display Name");
        columnNamesVector.add("Size ");
        columnNamesVector.add("Status");
        Vector resourceVector = new Vector();
        setLocalResourceVector(resourceVector,VUE.getActiveMap());
        ResourceTableModel resourceTableModel = new ResourceTableModel(resourceVector, columnNamesVector);
        
        JTable resourceTable = new JTable(resourceTableModel);
       

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
                   Vector row = new Vector();
                   row.add(new Boolean(true));
                   row.add(resource);
                   row.add(new Long(resource.getSize()));
                   row.add("Ready");
                  
                   vector.add(row);
               }
           }
           if(component instanceof LWContainer) {
                setLocalResourceVector(vector,(LWContainer)component);
           }
       }
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == cancelButton) {   
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
    
    public class ResourceTable extends JTable {
        
        public ResourceTable(Vector data,Vector title) {
            super(data,title);
       
        }
        
    }
    public class ResourceTableCellRenderer extends DefaultTableCellRenderer{
        
        private final Font selectedFont = new Font("SansSerif", Font.BOLD, 12);
        private final Font normalFont = new Font("SansSerif", Font.PLAIN, 12);

        public ResourceTableCellRenderer(){

        }

        public Component getTableCellRendererComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               boolean hasFocus,
                                               int row,
                                               int col) {
        System.out.println("value class: "+value.getClass());
        System.out.println("this class:  "+this.getClass());
        
            if(col == 0){
                JCheckBox box = new JCheckBox();
                return box;
            }
            else if(col == 1){
                JLabel lab = new JLabel(((Resource)value).getSpec());
                return lab;
            }
            return new JLabel("None");
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
            System.out.println("Initialized ResourceTableModel ="+data.size()+ " : columnsize ="+columnNames.size());
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
            System.out.println("Reading "+ row+" , "+col+" : "+((Vector)data.elementAt(row)).elementAt(col));
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
