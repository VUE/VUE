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
import java.io.*;
import java.util.Iterator;
// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;



//

public class DataSourceViewer  extends JPanel{
    /** Creates a new instance of DataSourceViewer */
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
 
    public static final String[] dataSourceTypes = {"Filing-Local","Favorites", "Filing-Remote","Fedora","Google"};
    private static int filinglocal,favorites,fedora,google,filingremote,preBreakSpot,postBreakSpot;
   
    public static java.util.Vector dataSources;
    DataSource activeDataSource;
    DRBrowser drBrowser;
    JPopupMenu popup;       // add edit popup
    public static DataSourceList dataSourceList;
    JPanel resourcesPanel;
    JDialog addEditDialog = null;   //  The add/edit dialog box.
    AbstractAction addAction;//
    AbstractAction editAction; 
    AbstractAction deleteAction;
    AbstractAction saveAction;
    final static String XML_MAPPING =  VueResources.getURL("mapping.lw").getFile();

    public DataSourceViewer(DRBrowser drBrowser){
        
       // super("DataSource",true,true,true);// incase we dicide to use JInternalFrame
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        
       // dataSources = new java.util.Vector();
        setPopup();
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        dataSourceList = new DataSourceList();
       
       
        loadDataSources();
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
    /*
    public Vector getDataSources(){
        return this.dataSources;
        
    }
     */
    public DataSource getActiveDataSource() {
        return this.activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){ 
        if(this.getActiveDataSource()!= null)this.getActiveDataSource().setActiveDataSource(false);
        this.activeDataSource = ds;
        ds.setActiveDataSource(true);;
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
        
         saveAction =  new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                saveDataSourceViewer();
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
        popup.addSeparator();
        popup.add(deleteAction);
        popup.addSeparator();
        popup.add(saveAction);
    }
    
    public void showAddEditWindow(int mode) {
     if ((addEditDialog == null)  || true) { // always true, need to work for cases where case where the dialog already exists
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
            createEditPanel(editPanel); 
            tabbedPane.setSelectedComponent(addPanel);
            if(mode == EDIT_MODE) { 
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
    
    public void addNewDataSource (String displayName, String name,String searchURL, String address, String user, String password, int type, boolean active) throws java.net.MalformedURLException{   
       try{
          //System.out.print("Search string" + searchURL );
           System.out.println("type" + type+ "filinglocal"+ filinglocal+"favorites" + favorites+"filingremote" +filingremote+"fedora" +fedora+"google"+google);
        DataSource ds = new DataSource ("id", displayName, name, searchURL,address, user, password, type);
        int insertBefore =  0 ; boolean isBeginning = false; postBreakSpot = 0;
        if (type == DataSource.FILING_LOCAL){
            
            if (filinglocal == 0){
                if (favorites > 0 ){insertBefore = favorites - 1;
                }
                else if (filingremote > 0 ){insertBefore = filingremote - 1;}
                else if (fedora > 0 ) {insertBefore = fedora - 1;}
                else if(google >0 ){insertBefore = google - 1;}
                else{}
            }
            
            else{
                
                insertBefore = filinglocal;
                
            }
            
            if ((filinglocal == 0)  && (favorites > 0 || filingremote > 0 || fedora > 0 || google > 0)){postBreakSpot = 1; isBeginning = true;}
            
            if (favorites >0 )favorites = postBreakSpot + favorites +1;
            
            if (filingremote > 0 ) filingremote = postBreakSpot + filingremote +1;
            if (fedora > 0 ) fedora = postBreakSpot + fedora + 1;
            if(google >0 ) google = postBreakSpot + google + 1;
            
            filinglocal = filinglocal + 1;
            
            
            
            
        }
        else if (type == DataSource.FAVORITES){
            
            
            if (filingremote > 0 ){insertBefore = filingremote - 1;
            
            }
            else if (fedora > 0 ) {insertBefore = fedora - 1 ;}
            else if(google >0 ){insertBefore = google - 1;
            }
            else {
                if(!(dataSourceList.getContents().isEmpty()))insertBefore = dataSourceList.getContents().getSize();
                
            }
            
            
            if (favorites == 0) {
                
                
                if (filinglocal >0 ){
                    
                    preBreakSpot = insertBefore;
                    insertBefore = insertBefore + 1;
                    if (filingremote > 0 ) filingremote = filingremote + 1;
                    if (fedora > 0 ) fedora = fedora + 1;
                    if  (google >0 ) google = google +  1;
                    
                    
                }
                
                
                
                favorites = insertBefore;
                
                
                
            }
            if (dataSourceList.getContents().isEmpty()){favorites = favorites + 1;}
            
            
            if (filingremote > 0 ) filingremote = filingremote + 1;
            if (fedora > 0 ) fedora = fedora + 1;
            if  (google >0 ) google = google +  1;
            
            
            
            
            
            System.out.println("isnertBef" +insertBefore );
            
        }
        else if (type == DataSource.FILING_REMOTE){
            
            System.out.println("fed in remote " + fedora );
            
            if (fedora > 0 ) insertBefore = fedora - 1 ;
            else if(google >0 )insertBefore = google - 1;
            
            else {
                if(!(dataSourceList.getContents().isEmpty()))insertBefore = dataSourceList.getContents().getSize();
                
            }
            
            
            if (filingremote == 0) {
                
                if (filinglocal > 0 || favorites > 0 ){
                    
                    
                    preBreakSpot = insertBefore;
                    insertBefore = insertBefore + 1;
                    if (fedora > 0 ) fedora = fedora +  1;
                    if  (google >0 ) google = google  + 1;
                    
                }
                
                filingremote = insertBefore;
                
                
                
            }
            
            
            if (dataSourceList.getContents().isEmpty()){filingremote = filingremote + 1;}
            
            
            if (fedora > 0 ) fedora = fedora +  1;
            if  (google >0 ) google = google  + 1;
            
            
            
            
            
            
            
            
            
        }
        else if (type == DataSource.DR_FEDORA){
            System.out.println("google in fedora " + google );
            
            if(google >0 ){insertBefore = google - 1;
            }
            else {
                if(!(dataSourceList.getContents().isEmpty()))insertBefore = dataSourceList.getContents().getSize();
                
            }
            
            
            if (fedora == 0) {
                
                if (filinglocal > 0 || favorites > 0 || filingremote > 0){
                    
                    preBreakSpot = insertBefore;
                    insertBefore = insertBefore + 1;
                    
                    
                }
                
                
                
                
                fedora = insertBefore;
                
                
                
            }
            
            
            if (dataSourceList.getContents().isEmpty()){fedora = fedora + 1;}
            
            if  (google >0 ) google = google + preBreakSpot + 1;
            
            System.out.println("isnertBef" +insertBefore + "preBreakSpot" + preBreakSpot);
            
            
            
            
            
        }
        
        else {
            
            
            if(!(dataSourceList.getContents().isEmpty()))insertBefore = dataSourceList.getContents().getSize();
            
            if (google == 0) {
                
                if (favorites  > 0 | filinglocal > 0 | filingremote > 0 | fedora > 0){
                    
                    preBreakSpot = insertBefore;
                    insertBefore = insertBefore + 1;
                    
                }
                
                google  = insertBefore;
                
                
                
            }
            
            if (dataSourceList.getContents().isEmpty()){google = google + 1;}
     
        }
        
        
        
        
        
      
        
        
        if (preBreakSpot  > 0){
            
            DataSource bds = new DataSource("id", ".", "",DataSource.BREAK);
            if (preBreakSpot != dataSourceList.getContents().getSize()){
               // dataSources.insertElementAt(bds,preBreakSpot);
                dataSourceList.getContents().insertElementAt(bds,preBreakSpot);
            }
            else{
                //dataSources.add(bds);
                dataSourceList.getContents().addElement(bds);
            }
            
            preBreakSpot = 0;
        }
        
        System.out.println("Insert Before " + insertBefore+"type" + type);
        
        if ((insertBefore > 0) || isBeginning){
            //dataSources.insertElementAt(ds, insertBefore);
            dataSourceList.getContents().insertElementAt(ds,insertBefore);
            
        }
        else {
            
            //dataSources.add(ds);
            dataSourceList.getContents().addElement(ds);
        }
        
        if (postBreakSpot  > 0){
            
            DataSource pds = new DataSource("id", ".", "",DataSource.BREAK);
            //dataSources.insertElementAt(pds,postBreakSpot);
            dataSourceList.getContents().insertElementAt(pds,postBreakSpot);
            postBreakSpot = 0;
        }
        
        
        
        System.out.println("filinglocal"+ filinglocal+"favorites" + favorites+"filingremote" +filingremote+"fedora" +fedora+"google"+google);
        if (active) setActiveDataSource(ds);
       }catch (Exception ex){}
       drBrowser.repaint();
       drBrowser.validate();
    }
    
    public void deleteDataSource(DataSource dataSource) {
          int prev = 0;boolean onlyone = false; DataSource breakDs = new DataSource();
          System.out.println("DataSource --filinglocal"+ filinglocal+"favorites" + favorites+"filingremote" +filingremote+"fedora" +fedora+"google"+google);
        int choice = JOptionPane.showConfirmDialog(null,"Do you want to delete Datasource "+dataSource.getDisplayName(),"Confirm Delete",JOptionPane.YES_NO_CANCEL_OPTION);
        if(choice == 0) {
            
            prev = this.dataSourceList.getSelectedIndex() - 1;
            
            if (prev > 0){
            DataSource prevds = (DataSource)(dataSourceList.getModel()).getElementAt(prev);
            if (prevds.getType() == DataSource.BREAK){
                                    breakDs = (DataSource)(dataSourceList.getModel()).getElementAt(prev);
                                        prev = prev -1;
                                  
                                  prevds = (DataSource)(dataSourceList.getModel()).getElementAt(prev);}
              setActiveDataSource(prevds);
            }                            
            
            
            
            // THIS PART NEEDS TO BE FIXED.  Doesn't handle deletion of first datasource.
            //if(dataSources.size() >0 ) {
              //  setActiveDataSource((DataSource)dataSources.firstElement());
            //}
           
                
            if (dataSource.getType() == DataSource.FILING_LOCAL) {
                                                                  
                 filinglocal = filinglocal -1;
                 if (filinglocal == 1)onlyone = true;
                 
                 if (favorites > 0 )favorites = favorites - 1;
                 if (filingremote > 0)filingremote = filingremote - 1;
                 if (fedora > 0)fedora = fedora - 1;
                 if (google > 0)google = google - 1;
                 if (onlyone){
                     breakDs = (DataSource)(dataSourceList.getModel()).getElementAt(1);
 
     if (dataSourceList.getContents().getSize() > 1)setActiveDataSource((DataSource)(dataSourceList.getModel().getElementAt(2)));
                     if (favorites > 0 )favorites = favorites - 1;
                     if (filingremote > 0)filingremote = filingremote - 1;
                     if (fedora > 0)fedora = fedora - 1;
                     if (google > 0)google = google - 1;
                     
                 }
                 
            }
            
            else if  (dataSource.getType() == DataSource.FAVORITES){
                if (favorites == 1){onlyone = true;}
                favorites =favorites - 1;
                if (filingremote > 0 )filingremote = filingremote - 1;
                if (fedora > 0)fedora = fedora - 1;
                if (google > 0)google = google - 1;
                
                if (onlyone){
                    if (filingremote > 0 )filingremote = filingremote - 1;
                    if (fedora > 0)fedora = fedora - 1;
                    if (google > 0)google = google - 1;
                }
                
            }
            else if (dataSource.getType() == DataSource.FILING_REMOTE){ filingremote = filingremote -1 ;
                                                              if (fedora > 0)fedora = fedora - 1;
                                                                  if (google > 0)google = google - 1;
            }
            else if (dataSource.getType() == DataSource.DR_FEDORA){ fedora = fedora - 1;
                                                                    if (google > 0)google = google - 1;
            }
                 
               System.out.println("Delete -after filinglocal"+ filinglocal+"favorites" + favorites+"filingremote" +filingremote+"fedora" +fedora+"google"+google);
              // dataSources.remove(dataSource);
            dataSourceList.getContents().removeElement(dataSource);
            if (onlyone) {dataSourceList.getContents().removeElement(breakDs);}
            
        }
    }
    private boolean checkValidUser(String userName,String password,int type) {
        if(type == DataSource.DR_FEDORA) {
            try {
                TuftsDLAuthZ tuftsDL =  new TuftsDLAuthZ ();
                osid.shared.Agent user = tuftsDL.authorizeUser (userName,password);
                if(user == null) 
                    return false;
                if(tuftsDL.isAuthorized (user, TuftsDLAuthZ.AUTH_VIEW))
                    return true;
                else
                    return false;
            } catch(Exception ex) {
                VueUtil.alert(null,"DataSourceViewer.checkValidUser - Exception :" +ex, "Validation Error");
                ex.printStackTrace();
                return false;
            }
        } else 
            return true;
    }

    public static void saveDataSourceViewer(){
            File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
            Vector sDataSources = new Vector();
            int size = dataSourceList.getModel().getSize();
            int i;
            for (i = 0; i< size; i++){
                    DataSource sds = (DataSource)dataSourceList.getModel().getElementAt(i); 
                     if (sds.getType() != DataSource.BREAK) sDataSources.add(sds);
     
            }
            SaveDataSourceViewer sViewer= new SaveDataSourceViewer(sDataSources);
            marshallMap(f,sViewer);
    }
        
    private void createAddPanel(JPanel addPanel) {
        JComboBox typeField = new JComboBox(dataSourceTypes);    //  This will be a menu, later.
        JTextField dsNameField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField searchURLField = new JTextField();
        JTextField adrField = new JTextField();
        JTextField userField = new JTextField();
        JPasswordField pwField = new JPasswordField();

        //  Set them to a uniform size.
        Dimension dim = new Dimension (150, 22);
        typeField.setPreferredSize (dim);
        dsNameField.setPreferredSize (dim);
        nameField.setPreferredSize (dim);
        adrField.setPreferredSize (dim);
        userField.setPreferredSize (dim);
        pwField.setPreferredSize (dim);
        searchURLField.setPreferredSize (dim);
        
        //  Create the labels.
        JLabel typeLabel =      new JLabel("Data Source Type: ");
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel searchURLLabel = new JLabel("Search URL");
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
       
        
        addPanel.add(adrLabel);         //  4:  address label.
        addPanel.add(adrField);         //  5:  address field.
        addPanel.add(userLabel);        //  6:  user label.
        addPanel.add(userField);        //  7:  user field.
        addPanel.add(pwLabel);          //  8:  pw label.
        addPanel.add(pwField);          //  9:  pw field.
        addPanel.add(searchURLLabel);   //  10:   searchURL Label
        addPanel.add(searchURLField);   //  11:   searchURL

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
                String nameStr = dsNameStr;
                JTextField adrField = (JTextField) panel.getComponent(5);
                String adrStr = adrField.getText();
                JTextField userField = (JTextField) panel.getComponent(7);
                String userStr = userField.getText();
                JTextField pwField = (JTextField) panel.getComponent(9);
                String pwStr = pwField.getText();
                JTextField  searchURLField = (JTextField)panel.getComponent(11);
                String  searchURLStr = searchURLField.getText();
                 
                /**
                int type = 0;                      
                if (typeStr.compareTo("favorites") == 0) type = DataSource.FAVORITES;
                else if (typeStr.compareTo("local") == 0) type = DataSource.FILING_LOCAL;
                else if (typeStr.compareTo("remote") == 0) type = DataSource.FILING_REMOTE;
                else if (typeStr.compareTo("fedora") == 0) type = DataSource.DR_FEDORA;
                 */
                System.out.println ("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);
                if(dsNameStr.length() < 1) {
                    VueUtil.alert(null, "Datasourcename should be atleast 1 char long", "Invalid DataSource Name");
                    return;
                }
                
                if((type == 2) && (!checkValidUser( userStr,pwStr,type))) {
                    VueUtil.alert(null, "Not valid Tufts User. You are not allowed to create this dataSource", "Invalid User");
                    return;
                }
                try {
                     System.out.println ("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);
                    dsv.addNewDataSource (dsNameStr, nameStr, searchURLStr, adrStr, userStr, pwStr, type,false);
                } catch (Exception ex) {
                    VueUtil.alert(null,"Cannot add Datasource"+nameStr+": "+ex.getMessage(), "Datasource can't be added");
                }
                System.out.println ("New data source added.");

                dia.hide();
                
            }
        });
    }
    
    private void createEditPanel(JPanel editPanel) {
        JTextField dsNameField = new JTextField(activeDataSource.getDisplayName());
        JTextField nameField = new JTextField(activeDataSource.getName());
        JTextField urlField = new JTextField(activeDataSource.getSearchURL());
        JTextField adrField = new JTextField(activeDataSource.getAddress());
        JTextField userField = new JTextField(activeDataSource.getUserName());
        JTextField pwField = new JTextField(activeDataSource.getPassword());
       
        
        //  Set them to a uniform size.
        Dimension dim = new Dimension (150, 22);
        dsNameField.setPreferredSize (dim);
        nameField.setPreferredSize (dim);
        adrField.setPreferredSize (dim);
        userField.setPreferredSize (dim);
        pwField.setPreferredSize (dim);
        urlField.setPreferredSize(dim);

        //  Create the labels.
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel adrLabel =   new JLabel("Address: ");
       
        JLabel userLabel =   new JLabel("User name: ");
        JLabel pwLabel =     new JLabel("Password: ");
        JLabel searchURLLabel = new JLabel("Search URL");
        JLabel anon1 =        new JLabel("(For anonymous, leave ");
        //anon1.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel anon2 =        new JLabel("user and password blank)");
        //anon2.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel blank = new JLabel(" ");

        //  Create the buttons we need.
        JButton okBut = new JButton ("Submit");
        okBut.setName ("Submit Button");
        //JButton canBut = new JButton ("Cancel");
        adrField.setEditable(false); // this feature cannot be supported yet.  Once  DR is created it is difficult to change IP and be OKI compliant

        //  Add the gadgets to the Add Panel.
        editPanel.add(dsNameLabel);      //  1:  data source name label.
        editPanel.add(dsNameField);      //  2:  data source field.
       
        editPanel.add(adrLabel);         //  3:  address label.
        editPanel.add(adrField);         //  4:  address field.
        editPanel.add(userLabel);        //  5:  user label.
        editPanel.add(userField);        //  6:  user field.
        editPanel.add(pwLabel);          //  7:  pw label.
        editPanel.add(pwField);          //  8:  pw field.
        editPanel.add(searchURLLabel);   //  9:   searchURL Label
        editPanel.add(urlField);   //  10:   searchURL

        editPanel.add(anon1);            //  11:  anonymous label.
        editPanel.add(anon2);            //  12:  anonymous label.
        editPanel.add(blank);            //  13:  blank label.
        editPanel.add(okBut);            //  14:  submit button.
       
     
        
         //editPanel.add(canBut);         //  15:  cancel button.
        okBut.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent ev) {
                DataSourceViewer dsv = DRBrowser.dsViewer;
                JDialog dia = dsv.getAddEditDialog();
                JTabbedPane tabs = (JTabbedPane)dia.getContentPane().getComponent(0);
                JPanel panel = (JPanel) tabs.getSelectedComponent();    // Either add or edit panel.

               
                JTextField dsNameField = (JTextField) panel.getComponent(1);
                JTextField adrField = (JTextField) panel.getComponent(3);
                JTextField userField = (JTextField) panel.getComponent(5); 
                JTextField pwField = (JTextField) panel.getComponent(7);
              //  System.out.println("This is component 10 " + panel.getComponent(9).getClass()+"---"+panel.getComponent(9)+"===");
                JTextField urlField = (JTextField) panel.getComponent(9);
                
                if(dsNameField.getText().length() < 1) {
                    VueUtil.alert(null, "Datasourcename should be atleast 1 char long", "Invalid DataSource Name");
                    return;
                }
                if(!checkValidUser( userField.getText(),pwField.getText(),activeDataSource.getType())) {
                    VueUtil.alert(null, "Not valid Tufts User. You are not allowed to create this dataSource", "Invalid User");
                    return;
                }
                activeDataSource.setDisplayName(dsNameField.getText());
                activeDataSource.setName(dsNameField.getText());
                activeDataSource.setAddress(adrField.getText());
                activeDataSource.setUserName(userField.getText());
                activeDataSource.setPassword(pwField.getText());
                activeDataSource.setSearchURL(urlField.getText());

                try {
                    activeDataSource.setViewer();
                } catch(Exception ex) {
                    ex.printStackTrace();
                } 
                setActiveDataSource(activeDataSource); // reset resource panel after edits
                dia.hide();
            }
        });

    }
            
        
            
    private void loadDataSources()  {
        
        
        //--Marshalling etc
            filinglocal = 0; favorites = 0;fedora=0;google=0;filingremote = 0;preBreakSpot = 0;postBreakSpot =0;
          
            
            File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
            
            if(f.exists()){
        
          SaveDataSourceViewer rViewer = unMarshallMap(f);
          Vector rsources = rViewer.getSaveDataSources();
          while (!(rsources.isEmpty())){
               DataSource ds = (DataSource)rsources.remove(0);
               System.out.println(ds.getDisplayName()+"Is this active ---  "+ds.isActiveDataSource());
               try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }
          catch(Exception ex) {}
          }
           
            }
       
            
            else{
                
        // this should be created automatically from a config file. That will be done in future.
                try {  
                    System.out.println("this is load " + DataSource.FILING_LOCAL+"rem" +DataSource.FILING_REMOTE+"Fav"+DataSource.FAVORITES+"goo"+DataSource.GOOGLE);
                    
                  
                    DataSource ds = new DataSource("ds1", "My Computer", "My Computer",DataSource.FILING_LOCAL);
                     try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),false);
                                 }catch (Exception ex){}
                
                  /*  
                     ds = new DataSource("ds9", ".", "",DataSource.BREAK);
                    
                    try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),false);
                                 }catch (Exception ex){}
                 
                   */
                    
                    ds = new DataSource("ds2", "My Favorites","favorites","","","","",DataSource.FAVORITES);
                     
                     
                   
                      try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }catch (Exception ex){}
                     
                     
                    ds =  new DataSource("ds3", "Tufts Digital Library","fedora","","130.64.77.144","test","test",DataSource.DR_FEDORA);
                  try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }catch (Exception ex){}
                     
                    setActiveDataSource(ds);
                   
                    ds= new DataSource("ds4","UVA: Finding Aids","uva:fedora","","dl.lib.virginia.edu", "test","test", DataSource.DR_FEDORA);
                    
                    try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }catch (Exception ex){}
                    
                   
                    ds = new DataSource("ds5", "Tufts Web","google",VueResources.getString("url.google"),"","","",DataSource.GOOGLE);
                 
                    
                    try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }catch (Exception ex){}
                    
                    ds = new DataSource("ds6", "NYU Web","google","http://google.nyu.edu/search?site=NYUWeb_Main&client=NYUWeb_Main&output=xml_no_dtd&q=nyu&btnG.x=15&btnG.y=9","","","",DataSource.GOOGLE);
                
                   
                    try {
                addNewDataSource(ds.getDisplayName(),
                                         ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(), 
                                         ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                                 }catch (Exception ex){}

                   
                }catch (Exception ex) {
                    System.out.println("Datasources can't be loaded");
                }
      
            }
            
    }
    
        
 public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer)
    {
        Marshaller marshaller = null;
        
        
        Mapping mapping = new Mapping();
            
        try 
        {  
            FileWriter writer = new FileWriter(file);
            
            marshaller = new Marshaller(writer);
            mapping.loadMapping(XML_MAPPING);
            marshaller.setMapping(mapping);
            
           
            marshaller.marshal(dataSourceViewer);
            
            writer.flush();
            writer.close();
            
        } 
        catch (Exception e) {System.err.println("DRBrowser.marshallMap " + e);}
      
    }
    
    
   public  SaveDataSourceViewer unMarshallMap(File file)
    {
        Unmarshaller unmarshaller = null;
        SaveDataSourceViewer sviewer = null;
      
        
       
        Mapping mapping = new Mapping();
            
        try 
        {
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);    
            unmarshaller.setMapping(mapping);  
            
            FileReader reader = new FileReader(file);
            
            sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
            
            reader.close();
        } 
        catch (Exception e) 
        {
            System.err.println("DataSourceViewer.SaveDataSourceViewer " + e);
            e.printStackTrace();
            sviewer = null;
        }
       
        
        return sviewer;
    } 
 
   
   /*
    * static method that returns all the datasource where Maps can be published.
    * Only FEDORA @ Tufts is available at present
    */
   public static Iterator getPublishableDataSources() {
       Vector mDataSources = new Vector();
       try {
           mDataSources.add(new DataSource("ds2", "Tufts Digital Library","fedora","","130.64.77.144","test","test",DataSource.DR_FEDORA));
       } catch (Exception ex) {
           System.out.println("Datasources can't be loaded");
       }
      
       /**
       Iterator i = dataSources.iterator();
       while(i.hasNext() ) {
           DataSource mDataSource = (DataSource)i.next();
           if(mDataSource.getType() == DataSource.DR_FEDORA)
               mDataSources.add(mDataSource);
       }
        */
       return mDataSources.iterator();
           
   }
}
