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
 * DataSourceViewer.java
 *
 * Created on October 15, 2003, 1:03 PM
 */

package tufts.vue;
/**
 *
 * @author  akumar03
 */

import tufts.vue.gui.VueButton;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;
import java.io.*;
import java.util.*;
// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;



//

public class DataSourceViewer  extends JPanel implements KeyListener{
    /** Creates a new instance of DataSourceViewer */
   
    
    
    DRBrowser drBrowser;
    DataSource activeDataSource;
    JPanel resourcesPanel,dataSourcePanel;
    String breakTag = "";
    
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;

    JPopupMenu popup;       // add edit popup
    JDialog addEditDialog = null;   //  The add/edit dialog box.
    AbstractAction addAction;//
    AbstractAction editAction;
    AbstractAction deleteAction;
    AbstractAction saveAction;
    AbstractAction refreshAction;

   
    public static Vector  allDataSources = new Vector();
      
       
     public static DataSourceList dataSourceList;
     final static java.net.URL XML_MAPPING =  VueResources.getURL("mapping.lw");
     
     public DataSourceViewer(DRBrowser drBrowser){
        
        
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        
        dataSourceList = new DataSourceList(this);
        dataSourceList.addKeyListener(this);
        
        
        loadDataSources();  
     
       // if (loadingFromFile)dataSourceChanged = false;
       setPopup(); 
        dataSourceList.addListSelectionListener(new ListSelectionListener() {
           public void valueChanged(ListSelectionEvent e) {
               
              if ((DataSource)((JList)e.getSource()).getSelectedValue()!=null){
              if (!(((JList)e.getSource()).getSelectedValue() instanceof String)){
               DataSourceViewer.this.setActiveDataSource(((DataSource)((JList)e.getSource()).getSelectedValue()));
              }
               
            }}
        });
        dataSourceList.addMouseListener(new MouseAdapter() {
           public void mouseClicked(MouseEvent e) {
               if(e.getButton() == e.BUTTON3) {
                  popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
       });
        
        
        // GRID: addConditionButton
        JButton addButton=new VueButton("add");
        addButton.setBackground(this.getBackground());
        addButton.setToolTipText("Add/Edit Datasource Information");
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               showAddEditWindow(0);
                
            }
        });
        
        
        // GRID: deleteConditionButton
        JButton deleteButton=new VueButton("delete");
        deleteButton.setBackground(this.getBackground());
        deleteButton.setToolTipText("Remove a Datasource from VUE");
        
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               deleteDataSource(activeDataSource);
              
               refreshDataSourceList();
               if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
               else{ 
                     DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                     DataSourceViewer.this.resourcesPanel  = new JPanel();
                     DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                     DataSourceViewer.this.drBrowser.repaint();
                     DataSourceViewer.this.drBrowser.validate();
               }
            }
        });

        
           // GRID: addConditionButton
        
        
        JButton refreshButton=new VueButton("refresh");
        
        refreshButton.setBackground(this.getBackground());
        refreshButton.setToolTipText("Refresh Local Datasource");
        
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               activeDataSource.setResourceViewer();
               refreshDataSourceList();
               
            }
        });
        
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("Add/Delete/Refresh a Data Source");
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
       
        
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);
        topPanel.add(questionLabel);
     
        
       
        dataSourcePanel = new JPanel();
        dataSourcePanel.setLayout(new BorderLayout());
        dataSourcePanel.add(topPanel,BorderLayout.NORTH);
      
        
        JScrollPane dataJSP = new JScrollPane(dataSourceList);
        dataSourcePanel.add(dataJSP,BorderLayout.CENTER);
        add(dataSourcePanel,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);

   
        
    }
   
    public void addDataSource(DataSource ds){
        
       int type;
       
       if (ds instanceof LocalFileDataSource) type = 0;
       else if (ds instanceof FavoritesDataSource) type = 1;
         else  if (ds instanceof RemoteFileDataSource) type = 2;
       else  if (ds instanceof FedoraDataSource) type = 3;
       else  if (ds instanceof GoogleDataSource) type = 4;
       else type = 5;
  
        Vector dataSourceVector = (Vector)allDataSources.get(type);
        dataSourceVector.add(ds);
      
    
    } 
    
     public void deleteDataSource(DataSource ds){
        
       int type;
       
       if (ds instanceof LocalFileDataSource) type = 0;
       else if (ds instanceof FavoritesDataSource) type = 1;
        else  if (ds instanceof RemoteFileDataSource) type = 2;
       else  if (ds instanceof FedoraDataSource) type = 3;
       else  if (ds instanceof GoogleDataSource) type = 4;
       else type = 5;
  
        Vector dataSourceVector = (Vector)allDataSources.get(type);
        dataSourceVector.removeElement(ds);
      
    
    } 
    
    public void refreshDataSourceList(){
          
       int i =0; Vector dsVector;
       String breakTag = "";
      int NOOFTYPES = 5;
     
      
        if (!(dataSourceList.getContents().isEmpty()))dataSourceList.getContents().clear(); 
       
         for (i = 0; i < NOOFTYPES; i++){
            
          
                  dsVector = (Vector)allDataSources.get(i);
                
                     if (!dsVector.isEmpty()){
                         
                       
                
                              int j = 0;
                              for(j = 0; j < dsVector.size(); j++){
                                 dataSourceList.getContents().addElement(dsVector.get(j));
               
                                 }
                
                               boolean breakNeeded = false; int typeCount = i+1;
                 
                               while ((!breakNeeded) && (typeCount < NOOFTYPES)){ 
                                   
                                   if (!((Vector)allDataSources.get(i)).isEmpty())breakNeeded = true;
                               
                                   typeCount++;
                               }
                
                               if (breakNeeded) dataSourceList.getContents().addElement(breakTag);
              
                              }
          
                  }
       
     
         dataSourceList.setSelectedValue(this.getActiveDataSource(),true);
        
         dataSourceList.validate();
       
     
   
    }
      
      
     public DataSource getActiveDataSource() {
        return this.activeDataSource;
    }
    public void setActiveDataSource(DataSource ds){
       
       

      
        this.activeDataSource = ds;
        

      
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        
        resourcesPanel.setBorder(new TitledBorder(activeDataSource.getDisplayName()));
         JPanel dsviewer = (JPanel)ds.getResourceViewer();
        resourcesPanel.add(dsviewer,BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
       dataSourceList.setSelectedValue(ds,true);
        
      
      
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
                refreshDataSourceList();
               if (!dataSourceList.getContents().isEmpty())dataSourceList.setSelectedIndex(0);
               else{ 
                     DataSourceViewer.this.drBrowser.remove(resourcesPanel);
                     DataSourceViewer.this.resourcesPanel  = new JPanel();
                     DataSourceViewer.this.drBrowser.add(resourcesPanel,BorderLayout.CENTER);
                     DataSourceViewer.this.drBrowser.repaint();
                     DataSourceViewer.this.drBrowser.validate();
               }
            
                
            }
        };
        
        saveAction =  new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
               // saveDataSourceViewer();
            }
        };
        refreshAction =  new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refreshDataSourceList();
            }
        };
        popup.add(addAction);
        popup.addSeparator();
        popup.add(editAction);
        popup.addSeparator();
        popup.add(deleteAction);
        // popup.addSeparator();
        // popup.add(saveAction);
        popup.addSeparator();
        popup.add(refreshAction);
        
    }
      private boolean checkValidUser(String userName,String password,int type) {
        if(type == 3) {
            try {
                TuftsDLAuthZ tuftsDL =  new TuftsDLAuthZ();
                osid.shared.Agent user = tuftsDL.authorizeUser(userName,password);
                if(user == null)
                    return false;
                if(tuftsDL.isAuthorized(user, TuftsDLAuthZ.AUTH_VIEW))
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

    public void showAddEditWindow(int mode) {
        if ((addEditDialog == null)  || true) { // always true, need to work for cases where case where the dialog already exists
            addEditDialog = new JDialog(tufts.vue.VUE.getInstance(),"Add/Edit Dialog",true);
           
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setPreferredSize(new Dimension(300,400));
            tabbedPane.setName("Tabbed Pane");
            JPanel addPanel = new JPanel(new GridLayout(8, 2));
           // addPanel.setPreferredSize(new Dimension(300, 300));
            addPanel.setName("Add Panel");
            JPanel outerAddPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            outerAddPanel.add(addPanel);
            JPanel editPanel = new JPanel(new GridLayout(7,2));
            editPanel.setName("Edit Panel");
           // editPanel.setPreferredSize(new Dimension(300, 300));
            JPanel outerEditPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            outerEditPanel.add(editPanel);
            tabbedPane.add("Add", outerAddPanel);
            tabbedPane.add("Edit",outerEditPanel);
            createAddPanel(addPanel);
            createEditPanel(editPanel);
            tabbedPane.setSelectedComponent(outerAddPanel);
            if(mode == EDIT_MODE) {
                tabbedPane.setSelectedComponent(outerEditPanel);
            }
            
            
           // addEditDialog.getContentPane().add(tabbedPane);
           // addEditDialog.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT));
           
           
            addEditDialog.getContentPane().add(tabbedPane);
            addEditDialog.pack();
            addEditDialog.setLocation(300,300);
            addEditDialog.setSize(new Dimension(325, 245));
            
            //dialog.setResizable(false);
            addEditDialog.show();
        }
        else
            addEditDialog.show();
    }
    
    
    
    public JDialog getAddEditDialog() {
        return addEditDialog;
    }
    

    private void createAddPanel(JPanel addPanel) {
        
        String[] dataSourceTypes = {"Filing-Local","Favorites", "Filing-Remote","Fedora","Google","OSID-DR"};
        JComboBox typeField = new JComboBox(dataSourceTypes);    //  This will be a menu, later.
        JTextField dsNameField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField searchURLField = new JTextField();
        JTextField adrField = new JTextField();
        JTextField userField = new JTextField();
        JPasswordField pwField = new JPasswordField();
        
        //  Set them to a uniform size.
        Dimension dim = new Dimension(150, 22);
        typeField.setPreferredSize(dim);
        dsNameField.setPreferredSize(dim);
        nameField.setPreferredSize(dim);
        adrField.setPreferredSize(dim);
        userField.setPreferredSize(dim);
        pwField.setPreferredSize(dim);
        searchURLField.setPreferredSize(dim);
        
        //  Create the labels.
        JLabel typeLabel =      new JLabel("Data Source Type: ");
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel searchURLLabel = new JLabel("Search URL");
        JLabel adrLabel =   new JLabel("Address: ");
        JLabel userLabel =   new JLabel("User name: ");
        JLabel pwLabel =     new JLabel("Password: ");
        JLabel anon1 =        new JLabel("(For anonymous, leave ");
        //anon1.setFont(new Font(typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel anon2 =        new JLabel("user and password blank)");
        //anon2.setFont(new Font(typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel blank = new JLabel(" ");
        
        //  Create the buttons we need.
        JButton okBut = new JButton("Submit");
        okBut.setName("Submit Button");
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
        
      //  addPanel.add(anon1);            //  12:  anonymous label.
       // addPanel.add(anon2);            //  13:  anonymous label.
        
        addPanel.add(blank);            //  14:  blank label.
        addPanel.add(okBut);            //  15:  submit button.
        
        
        
        
        
        //addPanel.add(canBut);         //  16:  cancel button.
        
        //  Add a listener for the Submit button.
        okBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DataSourceViewer dsv = DRBrowser.dsViewer;
                JDialog dia = dsv.getAddEditDialog();
                JTabbedPane tabs = (JTabbedPane)dia.getContentPane().getComponent(0);
                JPanel outerPanel = (JPanel) tabs.getSelectedComponent();    // Either add or edit panel.
                JPanel panel = (JPanel)outerPanel.getComponent(0);
                //System.out.println("Did I get here in ok" + panel.getComponent(0));
                JComboBox typeField = (JComboBox) panel.getComponent(1);
                int type = typeField.getSelectedIndex();
                 // System.out.println("Did I get here in ok");
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
                
                
             //   System.out.println("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);
                if(dsNameStr.length() < 1) {
                    VueUtil.alert(null, "Datasourcename should be atleast 1 char long", "Invalid DataSource Name");
                    return;
                }
                
                if (type == 3){
                    if (adrStr.length() < 1){
                        VueUtil.alert(null, "Please type a suitable address", "Invalid Address");
                        return;
                        
                        
                    }
                    if (userStr.length() < 1){
                        VueUtil.alert(null, "Please type  a username", "Invalid Username");
                        return;
                        
                        
                    }
                    
                    if (pwStr.length() < 1){
                        VueUtil.alert(null, "Please type a  password", "Invalid Username");
                        return;
                        
                        
                    }
                    
                }
                
                if (type == 4){
                    
                    if (searchURLStr.length() < 1){
                        VueUtil.alert(null, "You need to enter a URL for google search", "Invalid Search String");
                        return;
                        
                    }
                    
                    
                }
                
                if (type == 5){
                    
                    if (adrStr.length() < 1){
                        VueUtil.alert(null, "You need to enter a package name for the DR OSID implementation", "Invalid Address");
                        return;
                        
                    }
                    
                    
                }
                if((type == 2) && (!checkValidUser( userStr,pwStr,type))) {
                    VueUtil.alert(null, "Not valid Tufts User. You are not allowed to create this dataSource", "Invalid User");
                    return;
                }
                try {
                   // System.out.println("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);
                   // dsv.addNewDataSource(dsNameStr, nameStr, searchURLStr, adrStr, userStr, pwStr, type,false);
                } catch (Exception ex) {
                    VueUtil.alert(null,"Cannot add Datasource"+nameStr+": "+ex.getMessage(), "Datasource can't be added");
                }
               // System.out.println("New data source added.");
                
                dia.hide();
                
            }
        });
    }
    
    private void createEditPanel(JPanel editPanel) {
        
        
        JTextField dsNameField = new JTextField(activeDataSource.getDisplayName());
     
        Dimension dim = new Dimension(150, 22);
        dsNameField.setPreferredSize(dim);
       
        //  Create the labels.
        JLabel dsNameLabel = new JLabel("Display Name: ");
       
        JLabel blank = new JLabel(" ");
        
        //  Create the buttons we need.
        JButton okBut = new JButton("Submit");
        okBut.setName("Submit Button");
       
        //  Add the gadgets to the Add Panel.
        editPanel.add(dsNameLabel);      //  1:  data source name label.
        editPanel.add(dsNameField);      //  2:  data source field.
        
       
        //editPanel.add(anon1);            //  11:  anonymous label.
        //editPanel.add(anon2);            //  12:  anonymous label.
        editPanel.add(blank);            //  13:  blank label.
        editPanel.add(okBut);            //  14:  submit button.
        
        // System.out.println("This is component 3 " + panel.getComponent(3).getClass()+"---"+panel.getComponent(3)+"===");
          //       System.out.println("This is component 4 " + panel.getComponent(4).getClass()+"---"+panel.getComponent(4)+"===");
        
        //editPanel.add(canBut);         //  15:  cancel button.
        okBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DataSourceViewer dsv = DRBrowser.dsViewer;
                JDialog dia = dsv.getAddEditDialog();
                JTabbedPane tabs = (JTabbedPane)dia.getContentPane().getComponent(0);
              
                JPanel outerPanel = (JPanel) tabs.getSelectedComponent();    // Either add or edit panel.
                JPanel panel = (JPanel)outerPanel.getComponent(0);
                
                JTextField dsNameField = (JTextField) panel.getComponent(1);
               // JTextField adrField = (JTextField) panel.getComponent(3);
             
                if(dsNameField.getText().length() < 1) {
                    VueUtil.alert(null, "Datasourcename should be atleast 1 char long", "Invalid DataSource Name");
                    return;
                }
              
                activeDataSource.setDisplayName(dsNameField.getText());
               
                
            
               
                setActiveDataSource(activeDataSource); // reset resource panel after edits
                dia.hide();
            }
        });
        
       
        

    }
    
 
    
    public void loadDataSources(){
        
          Vector dataSource0 = new Vector();       
          Vector dataSource1 = new Vector();
         Vector dataSource2 = new Vector();
          Vector dataSource3 = new Vector();
         Vector dataSource4 = new Vector();
      
         allDataSources.add(dataSource0);
         allDataSources.add(dataSource1);
         allDataSources.add(dataSource2);
            allDataSources.add(dataSource3);
         allDataSources.add(dataSource4);
       
        
         
           
               DataSource ds1 = new LocalFileDataSource("My Computer", "");
               addDataSource(ds1);
        
               DataSource ds2 = new FavoritesDataSource("My Favorites");
               addDataSource(ds2);
               
             
               DataSource ds3 = new FedoraDataSource("Tufts Digital Library","vue-dl.tccs.tufts.edu","test","test");
               addDataSource(ds3);
       
        
               DataSource ds4 = new GoogleDataSource("Tufts Google", VueResources.getString("url.google"));
               addDataSource(ds4);
               
             
               
          
             
           setActiveDataSource(ds1);
               refreshDataSourceList();
             
             
             
    
        
        
    }
    
    public void keyPressed(KeyEvent e) {
    }    

    public void keyReleased(KeyEvent e) {
    }    
    
    public void keyTyped(KeyEvent e) {
    }    
    
    
    
}
