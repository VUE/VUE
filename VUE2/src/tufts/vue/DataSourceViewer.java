/*
 * DataSourceViewer.java
 *
 * Created on October 15, 2003, 1:03 PM
 */

package tufts.vue;
/**
 *
 * @author  akumar03
 */

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;

public class DataSourceViewer  extends JPanel{
    /** Creates a new instance of DataSourceViewer */
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
    public static final String[] dataSourceTypes = {"Favorites","Filing-Local", "Filing-Remote","Fedora","Google"};
    java.util.Vector dataSources;
    DataSource activeDataSource;
    DRBrowser drBrowser;
    JPopupMenu popup;       // add edit popup
    DataSourceList dataSourceList;
    JPanel resourcesPanel;
    JDialog addEditDialog = null;   //  The add/edit dialog box.
    AbstractAction addAction;//
    AbstractAction editAction; 
    AbstractAction deleteAction;


    public DataSourceViewer(DRBrowser drBrowser){
        
       // super("DataSource",true,true,true);// incase we dicide to use JInternalFrame
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        
        dataSources = new java.util.Vector();
        setPopup();
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        dataSourceList = new DataSourceList();
        loadDataSources();
        
        
        dataSourceList.setSelectedIndex(1);
          
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                DataSourceViewer.this.setActiveDataSource(((DataSource)((JList)e.getSource()).getSelectedValue()));
            }
        });
       
       dataSourceList.addMouseListener(new MouseAdapter() {
           public void mouseClicked(MouseEvent e) {
               if(e.getButton() == e.BUTTON3) {
                  popup.show(e.getComponent(), e.getX(), e.getY());
               }
           }
       });
      
        JScrollPane jSP = new JScrollPane(dataSourceList);
        add(jSP,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
    }
    
    public DataSource getActiveDataSource() {
        return this.activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){ 
        activeDataSource = ds;
        dataSourceList.setSelectedValue(ds,true);
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        resourcesPanel.setBorder(new TitledBorder(activeDataSource.getDisplayName()));
        resourcesPanel.add(activeDataSource.getResourceViewer(),BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
        System.out.println("Setting active datasource = "+ds.getDisplayName());
    }
    
    public void  setPopup() {
        popup = new JPopupMenu();
        addAction = new AbstractAction("Add") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(0);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        editAction = new AbstractAction("Edit") {
            public void actionPerformed(ActionEvent e) {
                showAddEditWindow(1);
                DataSourceViewer.this.popup.setVisible(false);
            }
        };
        deleteAction =  new AbstractAction("Delete") {
            public void actionPerformed(ActionEvent e) {
                deleteDataSource(activeDataSource);
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
        popup.addSeparator();
        popup.add(deleteAction);
    }
    
    public void showAddEditWindow(int mode) {
     if ((addEditDialog == null)  || true) {
            addEditDialog = new JDialog();
            addEditDialog.setName("Add/Edit Dialog");
        
            //  Create a tabbed pane with two panes:  Add and Edit.
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setName("Tabbed Pane");
            JPanel addPanel = new JPanel(new GridLayout(8, 2));
            addPanel.setName("Add Panel");
            JPanel editPanel = new JPanel(new GridLayout(7,2));
            editPanel.setName("Edit Panel");
            tabbedPane.add("Add", addPanel);
            tabbedPane.add("Edit",editPanel);
            createAddPanel(addPanel);            
            tabbedPane.setSelectedComponent(addPanel);
            if(mode == EDIT_MODE) {
                createEditPanel(editPanel); 
                tabbedPane.setSelectedComponent(editPanel);
                 
            }
            addEditDialog.getContentPane().add(tabbedPane);
            addEditDialog.pack();
            addEditDialog.setLocation(300,300);
            addEditDialog.setSize (new Dimension (250, 185));
            //dialog.setResizable(false);
            addEditDialog.show();
        }
        else
            addEditDialog.show();

    }
    
    

    public JDialog getAddEditDialog() {
        return addEditDialog;
    }
    
    public void addNewDataSource (String displayName, String name, String address, String user, String password, int type) {
        DataSource ds = new DataSource ("id", displayName, name, address, user, password, type);
        //  Following example of above, we'd set color here, but I think it should be moved to DataSource, based on type.
        
        dataSources.addElement (ds);  //  Add datasource to data source vector.
        dataSourceList.getContents().addElement(ds); // SHOULD BE DONE IN SINGE STEP
       
        setActiveDataSource (ds);
    }
    
    public void deleteDataSource(DataSource dataSource) {
        int choice = JOptionPane.showConfirmDialog(null,"Do you want to delete Datasource "+dataSource.getDisplayName(),"Confirm Delete",JOptionPane.YES_NO_CANCEL_OPTION);
        if(choice == 0) {
            
            // THIS PART NEEDS TO BE FIXED.  Doesn't handle deletion of first datasource.
            if(dataSources.size() >0 ) {
                setActiveDataSource((DataSource)dataSources.firstElement());
            }
            dataSources.remove(dataSource);
            dataSourceList.getContents().removeElement(dataSource);
            
        }
    }

    private void createAddPanel(JPanel addPanel) {
        JComboBox typeField = new JComboBox(dataSourceTypes);    //  This will be a menu, later.
        JTextField dsNameField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField adrField = new JTextField();
        JTextField userField = new JTextField();
        JTextField pwField = new JTextField();

        //  Set them to a uniform size.
        Dimension dim = new Dimension (150, 22);
        typeField.setPreferredSize (dim);
        dsNameField.setPreferredSize (dim);
        nameField.setPreferredSize (dim);
        adrField.setPreferredSize (dim);
        userField.setPreferredSize (dim);
        pwField.setPreferredSize (dim);

        //  Create the labels.
        JLabel typeLabel =      new JLabel("Data Source Type: ");
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel nameLabel =   new JLabel("Name: ");
        JLabel adrLabel =   new JLabel("Address: ");
        JLabel userLabel =   new JLabel("User name: ");
        JLabel pwLabel =     new JLabel("Password: ");
        JLabel anon1 =        new JLabel("(For anonymous, leave ");
        anon1.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel anon2 =        new JLabel("user and password blank)");
        anon2.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel blank = new JLabel(" ");

        //  Create the buttons we need.
        JButton okBut = new JButton ("Submit");
        okBut.setName ("Submit Button");
        //JButton canBut = new JButton ("Cancel");

        //  Add the gadgets to the Add Panel.
        addPanel.add(typeLabel);        //  0:  type label.
        addPanel.add(typeField);        //  1:  type field.
        addPanel.add(dsNameLabel);      //  2:  data source name label.
        addPanel.add(dsNameField);      //  3:  data source field.
        addPanel.add(nameLabel);        //  4:  name label.
        addPanel.add(nameField);        //  5:  name field.
        addPanel.add(adrLabel);         //  6:  address label.
        addPanel.add(adrField);         //  7:  address field.
        addPanel.add(userLabel);        //  8:  user label.
        addPanel.add(userField);        //  9:  user field.
        addPanel.add(pwLabel);          //  10:  pw label.
        addPanel.add(pwField);          //  11:  pw field.

        addPanel.add(anon1);            //  12:  anonymous label.
        addPanel.add(anon2);            //  13:  anonymous label.
        addPanel.add(blank);            //  14:  blank label.
        addPanel.add(okBut);            //  15:  submit button.
        //addPanel.add(canBut);         //  16:  cancel button.

        //  Add a listener for the Submit button.
        okBut.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent ev) {
                DataSourceViewer dsv = DRBrowser.dsViewer;
                JDialog dia = dsv.getAddEditDialog();
                JTabbedPane tabs = (JTabbedPane)dia.getContentPane().getComponent(0);
                JPanel panel = (JPanel) tabs.getSelectedComponent();    // Either add or edit panel.

                JComboBox typeField = (JComboBox) panel.getComponent(1);
                int type = typeField.getSelectedIndex();
                JTextField dsNameField = (JTextField) panel.getComponent(3);
                String dsNameStr = dsNameField.getText();
                JTextField nameField = (JTextField) panel.getComponent(5);
                String nameStr = nameField.getText();
                JTextField adrField = (JTextField) panel.getComponent(7);
                String adrStr = adrField.getText();
                JTextField userField = (JTextField) panel.getComponent(9);
                String userStr = userField.getText();
                JTextField pwField = (JTextField) panel.getComponent(11);
                String pwStr = pwField.getText();
                /**
                int type = 0;                      
                if (typeStr.compareTo("favorites") == 0) type = DataSource.FAVORITES;
                else if (typeStr.compareTo("local") == 0) type = DataSource.FILING_LOCAL;
                else if (typeStr.compareTo("remote") == 0) type = DataSource.FILING_REMOTE;
                else if (typeStr.compareTo("fedora") == 0) type = DataSource.DR_FEDORA;
                 */
                System.out.println ("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);

                dsv.addNewDataSource (dsNameStr, nameStr, adrStr, userStr, pwStr, type);
                System.out.println ("New data source added.");

                dia.hide();
            }
        });
    }
    
    private void createEditPanel(JPanel editPanel) {
        JTextField dsNameField = new JTextField(activeDataSource.getDisplayName());
        JTextField nameField = new JTextField(activeDataSource.getName());
        JTextField adrField = new JTextField(activeDataSource.getAddress());
        JTextField userField = new JTextField(activeDataSource.getUserName());
        JTextField pwField = new JTextField(activeDataSource.getPasword());

        //  Set them to a uniform size.
        Dimension dim = new Dimension (150, 22);
        dsNameField.setPreferredSize (dim);
        nameField.setPreferredSize (dim);
        adrField.setPreferredSize (dim);
        userField.setPreferredSize (dim);
        pwField.setPreferredSize (dim);

        //  Create the labels.
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel nameLabel =   new JLabel("Name: ");
        JLabel adrLabel =   new JLabel("Address: ");
        JLabel userLabel =   new JLabel("User name: ");
        JLabel pwLabel =     new JLabel("Password: ");
        JLabel anon1 =        new JLabel("(For anonymous, leave ");
        //anon1.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel anon2 =        new JLabel("user and password blank)");
        //anon2.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel blank = new JLabel(" ");

        //  Create the buttons we need.
        JButton okBut = new JButton ("Submit");
        okBut.setName ("Submit Button");
        //JButton canBut = new JButton ("Cancel");

        //  Add the gadgets to the Add Panel.
        editPanel.add(dsNameLabel);      //  2:  data source name label.
        editPanel.add(dsNameField);      //  3:  data source field.
        editPanel.add(nameLabel);        //  4:  name label.
        editPanel.add(nameField);        //  5:  name field.
        editPanel.add(adrLabel);         //  6:  address label.
        editPanel.add(adrField);         //  7:  address field.
        editPanel.add(userLabel);        //  8:  user label.
        editPanel.add(userField);        //  9:  user field.
        editPanel.add(pwLabel);          //  10:  pw label.
        editPanel.add(pwField);          //  11:  pw field.

        editPanel.add(anon1);            //  12:  anonymous label.
        editPanel.add(anon2);            //  13:  anonymous label.
        editPanel.add(blank);            //  14:  blank label.
        editPanel.add(okBut);            //  15:  submit button.
        //editPanel.add(canBut);         //  16:  cancel button.
        okBut.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent ev) {
                DataSourceViewer dsv = DRBrowser.dsViewer;
                JDialog dia = dsv.getAddEditDialog();
                JTabbedPane tabs = (JTabbedPane)dia.getContentPane().getComponent(0);
                JPanel panel = (JPanel) tabs.getSelectedComponent();    // Either add or edit panel.

               
                JTextField dsNameField = (JTextField) panel.getComponent(1);
                JTextField nameField = (JTextField) panel.getComponent(3);
                JTextField adrField = (JTextField) panel.getComponent(5);
                JTextField userField = (JTextField) panel.getComponent(7); 
                JTextField pwField = (JTextField) panel.getComponent(9);
                
                activeDataSource.setDisplayName(dsNameField.getText());
                activeDataSource.setName(nameField.getText());
                activeDataSource.setAddress(adrField.getText());
                activeDataSource.setUserName(userField.getText());
                activeDataSource.setPassword(pwField.getText());
                dia.hide();
            }
        });

    }
            
        
            
    private void loadDataSources() {
         // this should be created automatically from a config file. That will be done in future.
  
        DataSource ds1 = new DataSource("ds1", "My Computer", "My Computer",DataSource.FILING_LOCAL);
        ds1.setDisplayColor(Color.BLACK);
        dataSources.add(ds1);
        dataSourceList.getContents().addElement(ds1);
        DataSource ds2 =  new DataSource("ds2", "Tufts Digital Library","fedora",DataSource.DR_FEDORA);
        ds2.setDisplayColor(Color.RED);
        dataSources.add(ds2);
        dataSourceList.getContents().addElement(ds2);
        setActiveDataSource(ds2);
        
       
        DataSource ds3 = new DataSource("ds3", "My Favorites","favorites",DataSource.FAVORITES);
        ds3.setDisplayColor(Color.BLUE);
        dataSources.add(ds3);
         dataSourceList.getContents().addElement(ds3);
      
        DataSource ds4 = new DataSource("ds4", "Tufts Google","google",DataSource.GOOGLE);
        ds4.setDisplayColor(Color.YELLOW);
        dataSources.add(ds4);
         dataSourceList.getContents().addElement(ds4);
      
        //drBrowser.add(dsMyComputer.getResourceViewer(),BorderLayout.CENTER);
        //drBrowser.add(dsMyComputer.getResourceViewer(),BorderLayout.SOUTH);
    }
}
