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
    public final int ADD_MODE = 0;
    public final int EDIT_MODE = 1;
    
    public static final String[] dataSourceTypes = {"Filing-Local","Favorites", "Filing-Remote","Fedora","Google"};
    private static int  filinglocal = 0,favorites = 0,fedora=0,google=0,filingremote=0;
    private static boolean  problemloadingfromfile = false;
    private static String begIndex = "NONE";
    
    
    private static boolean dataSourceChanged = false, loadingFromFile = false;
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
    AbstractAction refreshAction;
    
    final static String XML_MAPPING =  VueResources.getURL("mapping.lw").getFile();
    
    public DataSourceViewer(DRBrowser drBrowser){
        
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("DataSource"));
        this.drBrowser = drBrowser;
        resourcesPanel = new JPanel();
        dataSourceList = new DataSourceList(this);  
        loadDataSources();
        if (loadingFromFile)dataSourceChanged = false;
        setPopup(); 
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
                
            }
        });

        
           // GRID: addConditionButton
        JButton refreshButton=new VueButton("refresh");
        
        refreshButton.setBackground(this.getBackground());
        refreshButton.setToolTipText("Refresh Local Datasource");
        
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               
                refreshDataSourceViewer();
            }
        });
        
        JLabel questionLabel = new JLabel(VueResources.getImageIcon("smallInfo"), JLabel.LEFT);
        questionLabel.setPreferredSize(new Dimension(22, 17));
        questionLabel.setToolTipText("Add/Delete/Refresh a Data Source");
        
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));
       
        
        topPanel.add(addButton);
        //topPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0);

       // topPanel.setBorder(BorderFactory.createEmptyBorder(3,6,3,0));
        topPanel.add(deleteButton);
        topPanel.add(refreshButton);
        topPanel.add(questionLabel);
     
        
       
        JPanel  dataSourcePanel = new JPanel();
        
        dataSourcePanel.setLayout(new BorderLayout());
        dataSourcePanel.add(topPanel,BorderLayout.NORTH);
      
        
       JScrollPane dataJSP = new JScrollPane(dataSourceList);
        
      
        
        dataSourceList.addKeyListener(this);

        dataSourcePanel.add(dataJSP,BorderLayout.CENTER);
     
        add(dataSourcePanel,BorderLayout.CENTER);
       
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
       /*
        this.activeDataSource = ds;
        ds.setActiveDataSource(true);
        */
        
        dataSourceList.setSelectedValue(ds,true);
        if (ds.getType() == DataSource.BREAK){
            
            int i = dataSourceList.getSelectedIndex();
            
            
            ds = (DataSource)dataSourceList.getModel().getElementAt(i-1);
            dataSourceList.setSelectedValue(ds,true);
        }
        
        this.activeDataSource = ds;
        
        ds.setActiveDataSource(true);
        if (ds.getType() == DataSource.FAVORITES)((FavoritesWindow)ds.getResourceViewer()).favoritesPane.setSelectedIndex(2);
        drBrowser.remove(resourcesPanel);
        resourcesPanel  = new JPanel();
        resourcesPanel.setLayout(new BorderLayout());
        resourcesPanel.setBorder(new TitledBorder(activeDataSource.getDisplayName()));
        resourcesPanel.add(activeDataSource.getResourceViewer(),BorderLayout.CENTER);
        drBrowser.add(resourcesPanel,BorderLayout.CENTER);
        drBrowser.repaint();
        drBrowser.validate();
      
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
        refreshAction =  new AbstractAction("Refresh") {
            public void actionPerformed(ActionEvent e) {
                refreshDataSourceViewer();
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
    
    public void addNewDataSource(String displayName, String name,String searchURL, String address, String user, String password, int type, boolean active) throws java.net.MalformedURLException{
        
        
        
        DataSource ds;
        int insertAt = 0,postBreakSpot = 0, preBreakSpot = 0;
        int preExists = 0, postExists = 0;
      // System.out.println("This is address" + address+"type" +type);
      
       
        if (address.compareTo("") != 0){
            
        if (type == DataSource.FILING_LOCAL)  {
            
         File testFile = new File(address);
        
         if (!(testFile.isDirectory())){
             VueUtil.alert(null, "You can only mount directories or drives", "Invalid Data Source");
             
              return; 
         }
          
            
        }
    }
        
        try{
             // System.out.println("This is address" + address+"type" +type);
            ds = new DataSource("id", displayName, name, searchURL,address, user, password, type);
        }catch (Exception ex){
           
            VueUtil.alert(null,"There was a problem adding this Data Source","DataSource not added");
           //System.out.println("There was a problem adding this Data Source" + ex);
            return;
        }
        
        dataSourceChanged = true;
        if (type == DataSource.GOOGLE){
            //System.out.println("I am Search URL " + type +searchURL);
        }
        
        if (dataSourceList.getModel().getSize() == 0){//only element in the list
            
            if (type == DataSource.FILING_LOCAL)begIndex = "FILING_LOCAL";
            else if (type == DataSource.FAVORITES)begIndex = "FAVORITES";
            else if (type == DataSource.FILING_REMOTE)begIndex = "FILING_REMOTE";
            else if (type == DataSource.DR_FEDORA)begIndex = "DR_FEDORA";
            else if (type == DataSource.GOOGLE)begIndex = "GOOGLE";
            
            
        }
        else{//Have to find the right place
            
            if (type == DataSource.FILING_LOCAL){
                
                
                if (begIndex.compareTo("FILING_LOCAL") == 0){//local file syatem already exists
                    
                    if (favorites > 0) {insertAt = favorites - 1;}
                    else if (filingremote > 0) {insertAt = filingremote - 1;}
                    else if (fedora > 0){insertAt = fedora - 1;}
                    else if (google > 0){insertAt = google - 1;}
                    else {insertAt = dataSourceList.getModel().getSize();}
                    
                }
                
                else{//no lcoal file system
                    
                    insertAt = 0;
                    postBreakSpot = 1;
                    begIndex = "FILING_LOCAL";
                    
                }
                
                if (favorites > 0) {favorites = favorites + postBreakSpot + 1;}
                if (filingremote > 0) {filingremote = filingremote + postBreakSpot + 1;}
                if (fedora > 0){fedora = fedora + postBreakSpot + 1;}
                if (google > 0){google = google + postBreakSpot + 1;}
                
                
                
            }
            else if (type == DataSource.FAVORITES){
                if ((begIndex.compareTo("FAVORITES") == 0) || (favorites > 0)){//Favorites already exists
                    if (filingremote > 0) {insertAt = filingremote - 1;}
                    else if (fedora > 0){insertAt = fedora - 1;}
                    else if (google > 0){insertAt = google - 1;}
                    else {insertAt = dataSourceList.getModel().getSize();}
                } 
                else{//no favorites    
                    if (begIndex.compareTo("FILING_LOCAL") == 0){
                        if (filingremote > 0) {preBreakSpot  = filingremote - 1; insertAt = preBreakSpot + 1;}
                        else if (fedora > 0){  preBreakSpot  = fedora - 1;insertAt =preBreakSpot + 1;                 }
                        else if (google > 0){preBreakSpot  = google - 1;insertAt =preBreakSpot + 1; }
                        else {preBreakSpot = dataSourceList.getModel().getSize(); insertAt = preBreakSpot +1;}
                        favorites = insertAt;
                        
                    }
                    
                    else{
                        insertAt = 0;
                        begIndex = "FAVORITES";
                        postBreakSpot = 1;
                        
                        
                    }
                    
                    
                    if (preBreakSpot > 0){preExists = 1;}
                    if (postBreakSpot > 0){postExists = 1;}
                    
                }
                
                if (filingremote > 0) {filingremote = filingremote +preExists + postExists + 1;}
                if (fedora > 0){fedora = fedora + preExists + postExists + 1;}
                if (google > 0){google = google + preExists + postExists + 1;}
                
                
            }
            
            else if (type == DataSource.FILING_REMOTE){
                
                
                
                if ((begIndex.compareTo("FILING_REMOTE") == 0) || (filingremote > 0)){//Filingremote already exists
                    
                    if (fedora > 0){insertAt = fedora - 1;}
                    else if (google > 0){insertAt = google - 1;}
                    else {insertAt = dataSourceList.getModel().getSize();}
                    
                    
                    
                }
                
                else{//no filing remote
                    
                    if (((favorites> 0) || (begIndex.compareTo("FILING_LOCAL") == 0)) || (begIndex.compareTo("FAVORITES") == 0)){
                        if (fedora > 0){  preBreakSpot  = fedora - 1;insertAt =preBreakSpot + 1;                 }
                        else if (google > 0){preBreakSpot  = google - 1;insertAt =preBreakSpot + 1; }
                        else {preBreakSpot = dataSourceList.getModel().getSize(); insertAt = preBreakSpot +1;}
                        filingremote  = insertAt;
                        
                    }
                    
                    else{
                        insertAt = 0;
                        begIndex = "FILINGREMOTE";
                        postBreakSpot = 1;
                        
                        
                    }
                    
                    if (preBreakSpot > 0){preExists = 1;}
                    if (postBreakSpot > 0){postExists = 1;}
                    
                    
                }
                
                if (fedora > 0){fedora = fedora + preExists + postExists + 1;}
                if (google > 0){google = google + preExists + postExists + 1;}
                
            }
            
            else  if (type == DataSource.DR_FEDORA){
                
                if ((begIndex.compareTo("FEDORA") == 0) || (fedora > 0)){//Fedora already exists
                    
                    if (google > 0){insertAt = google - 1;}
                    else {insertAt = dataSourceList.getModel().getSize();}
                    
                }
                
                else{//no fedora
                    
                    
                    if ((((favorites> 0) || (begIndex.compareTo("FILING_LOCAL") == 0)) || (begIndex.compareTo("FAVORITES") == 0)) || (begIndex.compareTo("FILING_REMOTE") == 0)){
                        
                        
                        if (google > 0){preBreakSpot  = google - 1;insertAt =preBreakSpot + 1; }
                        else {preBreakSpot = dataSourceList.getModel().getSize(); insertAt = preBreakSpot +1;}
                        fedora = insertAt;
                        
                    }
                    
                    else{
                        insertAt = 0;
                        begIndex = "FEDORA";
                        postBreakSpot = 1;
                        
                        
                    }
                    
                    
                    
                    
                    if (preBreakSpot > 0){preExists = 1;}
                    if (postBreakSpot > 0){postExists = 1;}
                    
                    
                    
                    
                }
                
                
                if (google > 0){google = google + preBreakSpot + postBreakSpot + 1;}
                
                
                
                
            }
            
            else {
                
                
                if ((begIndex.compareTo("GOOGLE") == 0) || (google > 0)){//Google already exists
                    
                    insertAt = dataSourceList.getModel().getSize();
                    
                    
                    
                }
                
                else{//no google
                    
                    
                    
                    preBreakSpot = dataSourceList.getModel().getSize(); insertAt = preBreakSpot + 1;
                    google = insertAt;
                    
                    
                    
                    
                }
                
                
                
                
            }
            
            
            
        }
        
     //  System.out.println("preBreak" +preBreakSpot +"insertAt" + insertAt +"postBreakSpot" +postBreakSpot);
        
        if (preBreakSpot > 0){
            
            try{
                DataSource pds = new DataSource("pds","","","", "", "", "", DataSource.BREAK);
                
                dataSourceList.getContents().insertElementAt(pds, preBreakSpot);
            }catch (Exception EX) {}
            preBreakSpot = 0;
            
        }
        
        
        if (dataSourceList.getContents().isEmpty()){dataSourceList.getContents().addElement(ds);}
        
        
        else{dataSourceList.getContents().insertElementAt(ds,insertAt);}
        
        if (postBreakSpot > 0){
            
            try{
                DataSource bds = new DataSource("bds","","","", "", "", "", DataSource.BREAK);
                
                dataSourceList.getContents().insertElementAt(bds, preBreakSpot);
            }catch (Exception EX) {}
            postBreakSpot = 0;
        }
        
        try{
            if (active)setActiveDataSource(ds);
        }catch (Exception ex){setActiveDataSource((DataSource)dataSourceList.getContents().getElementAt(0));}
        
        
       // System.out.println("This is stuff at end"+"fav" + favorites + "rem" + filingremote+"fed" +fedora+"goo" + google + "beg"+begIndex);
        
        
        drBrowser.repaint();
        drBrowser.validate();
        
        
    }
    
    
    
    
    public void deleteDataSource(DataSource dataSource) {
        int currIndex;DataSource nextDs,breakDs = new DataSource(),preBreakDs = new DataSource(); int type; int breakExists = 0;boolean lastElement = false, onlyElement = false;
        int nDs;
        
        //System.out.println("DataSource --del beg"+ filinglocal+"favorites" + favorites+"filingremote" +filingremote+"fedora" +fedora+"google"+google);
        int choice = JOptionPane.showConfirmDialog(null,"Do you want to delete Datasource "+dataSource.getDisplayName(),"Confirm Delete",JOptionPane.YES_NO_CANCEL_OPTION);
        if(choice == 0) {
            
            dataSourceChanged = true;
            currIndex = dataSourceList.getSelectedIndex();
           // System.out.println("datasource here crr" + dataSource +"Currindex" + currIndex);
            if (currIndex == 0){//first Elsement
                
                if (dataSourceList.getModel().getSize() == 1){//only one element in the list
                    
                    dataSourceList.getContents().removeElement(dataSource);
                    filinglocal = 0; favorites = 0; filingremote = 0; fedora = 0; google = 0;
                    
                }
                else{//not only element on the list
                    
                    
                    
                    
                    nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex + 1);
                    
                    
                    
                    
                   // System.out.println("datasource here next" + nextDs);
                    if (nextDs.getType() == DataSource.BREAK){
                        breakDs = nextDs;
                        breakExists = 1;
                        
                        nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex + 2);
                     //   System.out.println("datasource here and" + nextDs);
                    }
                    
                    type = dataSource.getType();
                    
                    if (type == DataSource.FILING_LOCAL){
                        
                        if (favorites > 0){nDs = favorites -1;}
                        else if (filingremote  > 0){nDs = filingremote - 1;}
                        else if (fedora > 0) {nDs = fedora - 1;}
                        else if (google > 0){nDs = google - 1;}
                        else nDs = dataSourceList.getContents().getSize();
                        
                        if (nDs == 1 ){dataSourceList.getContents().removeElement(breakDs);}
                        
                        
                        else {breakExists = 0;}
                        
                        
                        if (favorites > 0) favorites = favorites - breakExists - 1;
                        if (fedora > 0) fedora = fedora - breakExists - 1;
                        if (filingremote  > 0) filingremote = filingremote- breakExists - 1;
                        if (google > 0) google = google - breakExists - 1;
                        
                        
                       // System.out.println("Stuff at local" +"nds"+nDs+"br" +breakExists+ "fav" +favorites+"rem"+filingremote+"fed"+fedora+"goo"+google);
                        
                    }
                    else if(type == DataSource.FAVORITES){
                        
                        if (filingremote  > 0){nDs = filingremote -favorites- 1;}
                        else if (fedora > 0) {nDs = fedora - favorites - 1;}
                        else if (google > 0){nDs = google - favorites - 1;}
                        
                        else nDs = (dataSourceList.getContents().getSize() - favorites);
                        
                        if ((nDs == 1 ) && (breakExists == 1)){dataSourceList.getContents().removeElement(breakDs);
                        favorites = 0;
                        }
                        
                        
                        else {breakExists = 0;}
                        
                        if (fedora > 0) fedora = fedora - breakExists - 1;
                        if (filingremote  > 0) filingremote = filingremote- breakExists - 1;
                        if (google > 0) google = google - breakExists - 1;
                        
                    }
                    else if(type == DataSource.FILING_REMOTE){
                        if (fedora > 0) {nDs = fedora - filingremote - 1;}
                        else if (google > 0){nDs = google - filingremote - 1;}
                        else nDs = (dataSourceList.getContents().getSize() - filingremote);
                        
                        
                        if ((nDs == 1) && (breakExists == 1)){
                            dataSourceList.getContents().removeElement(breakDs);
                            filingremote = 0;
                            
                        }
                        else {breakExists = 0;}
                        
                        if (fedora > 0) fedora = fedora - breakExists - 1;
                        if (google > 0) google = google - breakExists - 1;
                    }
                    
                    else if(type == DataSource.DR_FEDORA) {
                        if (google > 0){nDs = google - fedora - 1;}
                        else nDs = (dataSourceList.getContents().getSize() - fedora);
                        
                        if ((nDs == 1) && (breakExists == 1)){
                            dataSourceList.getContents().removeElement(breakDs);
                            fedora = 0;
                            
                        }
                        else {breakExists = 0;}
                        
                        
                        if (google > 0) google = google - breakExists - 1;
                        
                        
                    }
                    else{
                        nDs = (dataSourceList.getContents().getSize() - google);
                        if ((nDs == 1) && (breakExists == 1)){
                            dataSourceList.getContents().removeElement(breakDs);
                            google = 0;
                            
                        }
                        else {breakExists = 0;}
                        
                        
                        
                        
                    }
                    
                    this.setActiveDataSource(nextDs);
                    dataSourceList.getContents().removeElement(dataSource);
                    
                    
                    
                    
                    
                }
            }
            
            
            else{//not the first element
                
                if (currIndex ==  (dataSourceList.getModel().getSize() -1)){//this is the last element
                    
                    lastElement = true;
                    nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex - 1);
                    if (nextDs.getType() == DataSource.BREAK){
                       // System.out.println("I am here last" + nextDs);
                        breakExists = 1;
                        
                        breakDs = nextDs;
                        nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex - 2);
                    }
                    
                    
                }
                
                else{//not the last element
                    nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex + 1);
                    
                    if (nextDs.getType() == DataSource.BREAK){
                        
                        breakExists = 1;
                        breakDs = nextDs;
                        
                        nextDs = (DataSource)dataSourceList.getModel().getElementAt(currIndex + 2);
                        
                        if (((DataSource)dataSourceList.getModel().getElementAt(currIndex -1)).getType() == DataSource.BREAK){
                            lastElement = true;
                            
                        }
                        
                    }
                }
                
                type = dataSource.getType();
                if (type == DataSource.FILING_LOCAL){
                    
                    if (favorites > 0){nDs = favorites -1;}
                    else if (filingremote  > 0){nDs = filingremote - 1;}
                    else if (fedora > 0) {nDs = fedora - 1;}
                    else if (google > 0){nDs = google - 1;}
                    else nDs = dataSourceList.getContents().getSize();
                    
                    if (nDs == 1 ){dataSourceList.getContents().removeElement(breakDs);}
                    
                    
                    else {breakExists = 0;}
                    
                    
                    if (favorites > 0) favorites = favorites - breakExists - 1;
                    if (fedora > 0) fedora = fedora - breakExists - 1;
                    if (filingremote  > 0) filingremote = filingremote- breakExists - 1;
                    if (google > 0) google = google - breakExists - 1;
                    
                    
                   // System.out.println("Stuff at local" +"nds"+nDs+"br" +breakExists+ "fav" +favorites+"rem"+filingremote+"fed"+fedora+"goo"+google);
                    
                }
                else if(type == DataSource.FAVORITES){
                    
                    if (filingremote  > 0){nDs = filingremote- favorites- 1;}
                    else if (fedora > 0) {nDs = fedora - favorites - 1;}
                    else if (google > 0){nDs = google - favorites - 1;}
                    
                    else nDs = (dataSourceList.getContents().getSize() - favorites);
                    
                    if ((nDs == 1 ) && (breakExists == 1)){dataSourceList.getContents().removeElement(breakDs);
                    favorites = 0;
                    }
                    
                    
                    else {breakExists = 0;}
                    
                    if (fedora > 0) fedora = fedora - breakExists - 1;
                    if (filingremote  > 0) filingremote = filingremote- breakExists - 1;
                    if (google > 0) google = google - breakExists - 1;
                    
                }
                else if(type == DataSource.FILING_REMOTE){
                    if (fedora > 0) {nDs = fedora - filingremote - 1;}
                    else if (google > 0){nDs = google - filingremote - 1;}
                    else nDs = (dataSourceList.getContents().getSize() - filingremote);
                    
                    
                    if ((nDs == 1) && (breakExists == 1)){
                        dataSourceList.getContents().removeElement(breakDs);
                        filingremote = 0;
                        
                    }
                    else {breakExists = 0;}
                    
                    if (fedora > 0) fedora = fedora - breakExists - 1;
                    if (google > 0) google = google - breakExists - 1;
                }
                
                else if(type == DataSource.DR_FEDORA) {
                    if (google > 0){nDs = google - fedora - 1;}
                    else nDs = (dataSourceList.getContents().getSize() - fedora);
                    
                    if ((nDs == 1) && (breakExists == 1)){
                        dataSourceList.getContents().removeElement(breakDs);
                        fedora = 0;
                        
                    }
                    else {breakExists = 0;}
                    
                    
                    if (google > 0) google = google - breakExists - 1;
                    
                    
                }
                else{
                    nDs = (dataSourceList.getContents().getSize() - google);
                    if ((nDs == 1) && (breakExists == 1)){
                        dataSourceList.getContents().removeElement(breakDs);
                        google = 0;
                        
                    }
                    else {breakExists = 0;}
                    
                    
                    
                    
                }
                
                
                
                
                this.setActiveDataSource(nextDs);
                
                dataSourceList.getContents().removeElement(dataSource);
              //  System.out.println("datasource here nextDs" + nextDs);
                
                
                
            }
            
            
            if (dataSourceList.getModel().getSize() > 0){
                if (((DataSource)dataSourceList.getModel().getElementAt(0)).getType() == DataSource.FILING_LOCAL)begIndex = "FILING_LOCAL";
                else if (((DataSource)dataSourceList.getModel().getElementAt(0)).getType() == DataSource.FAVORITES)begIndex = "FAVORITES";
                else if (((DataSource)dataSourceList.getModel().getElementAt(0)).getType() == DataSource.FILING_REMOTE)begIndex = "FILING_REMOTE";
                else if (((DataSource)dataSourceList.getModel().getElementAt(0)).getType() == DataSource.DR_FEDORA)begIndex = "FEDORA";
                else if (((DataSource)dataSourceList.getModel().getElementAt(0)).getType() == DataSource.GOOGLE)begIndex = "GOOGLE";
                
            }
            else begIndex = "NONE";
            
        }
        
      //  System.out.println("Stuff at end delete" + "fav" +favorites+"rem"+filingremote+"fed"+fedora+"goo"+google);
        
    }
    
    private boolean checkValidUser(String userName,String password,int type) {
        if(type == DataSource.DR_FEDORA) {
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
    
    public void refreshDataSourceViewer(){
        
        if (((DataSource)dataSourceList.getSelectedValue()).getType() == DataSource.FILING_LOCAL){
            
            DataSource ds = (DataSource)dataSourceList.getSelectedValue();
            try{
                ds.setViewer();
                
            }
            catch (Exception ex){System.out.println("Problem refreshing resource");}
            
            setActiveDataSource(ds);
            
        }
        
        
        
    }
    
    public static void saveDataSourceViewer(){
        
        if (dataSourceChanged){
            int choice = JOptionPane.showConfirmDialog(null,"Data Sources have been changed. Would you like to save them? ","Confirm Save",JOptionPane.YES_NO_CANCEL_OPTION);
            if(choice == 0) {
                
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
        }
        
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
                
                if (type == DataSource.DR_FEDORA){
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
                
                if (type == DataSource.GOOGLE){
                    
                    if (searchURLStr.length() < 1){
                        VueUtil.alert(null, "You need to enter a URL for google search", "Invalid Search String");
                        return;
                        
                    }
                    
                    
                }
                
                if((type == 2) && (!checkValidUser( userStr,pwStr,type))) {
                    VueUtil.alert(null, "Not valid Tufts User. You are not allowed to create this dataSource", "Invalid User");
                    return;
                }
                try {
                   // System.out.println("Add data source params: " + type + ", " + dsNameStr + ", " + nameStr + ", " + adrStr + ", " + userStr + ", " + pwStr);
                    dsv.addNewDataSource(dsNameStr, nameStr, searchURLStr, adrStr, userStr, pwStr, type,false);
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
        JTextField nameField = new JTextField(activeDataSource.getName());
        JTextField urlField = new JTextField(activeDataSource.getSearchURL());
        JTextField adrField = new JTextField(activeDataSource.getAddress());
        JTextField userField = new JTextField(activeDataSource.getUserName());
        JTextField pwField = new JTextField(activeDataSource.getPassword());
        
        
        //  Set them to a uniform size.
        Dimension dim = new Dimension(150, 22);
        dsNameField.setPreferredSize(dim);
        nameField.setPreferredSize(dim);
        adrField.setPreferredSize(dim);
        userField.setPreferredSize(dim);
        pwField.setPreferredSize(dim);
        urlField.setPreferredSize(dim);
        
        //  Create the labels.
        JLabel dsNameLabel = new JLabel("Display Name: ");
        JLabel adrLabel =   new JLabel("Address: ");
        
        JLabel userLabel =   new JLabel("User name: ");
        JLabel pwLabel =     new JLabel("Password: ");
        JLabel searchURLLabel = new JLabel("Search URL");
        //JLabel anon1 =        new JLabel("(For anonymous, leave ");
        //anon1.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        //JLabel anon2 =        new JLabel("user and password blank)");
        //anon2.setFont (new Font (typeLabel.getFont().getName(),Font.PLAIN, 10));
        JLabel blank = new JLabel(" ");
        
        //  Create the buttons we need.
        JButton okBut = new JButton("Submit");
        okBut.setName("Submit Button");
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
                dataSourceChanged = true;
                setActiveDataSource(activeDataSource); // reset resource panel after edits
                dia.hide();
            }
        });
        
    }
    
    
    
    private void loadDataSources() {
        //--Marshalling etc
        favorites = 0;fedora=0;google=0;filingremote = 0;
        boolean  problemloadingfromfile = true;
        
        boolean debug = false;
        File f  = new File(VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.datasources"));
        
        if (f.exists() && !debug){
            loadingFromFile = true;
            SaveDataSourceViewer rViewer = unMarshallMap(f);
            if (rViewer == null) {
                
                problemloadingfromfile = true;
               System.out.println("There was a problem loading previously saved DataSources. Using default Data Sources");
                
            } else {
                Vector rsources = rViewer.getSaveDataSources();
                while (!(rsources.isEmpty())){
                    DataSource ds = (DataSource)rsources.remove(0);
                   // System.out.println(ds.getDisplayName()+"Is this active ---  "+ds.isActiveDataSource());
                    try {
                        addNewDataSource(ds.getDisplayName(),
                        ds.getName(),ds.getSearchURL(),ds.getAddress(), ds.getUserName(),
                        ds.getPassword(), ds.getType(),ds.isActiveDataSource());
                    }
                    catch(Exception ex) {problemloadingfromfile = true;}
                }
                problemloadingfromfile = false;
            }
        }
        
        if (problemloadingfromfile) {
            
            // this should be created automatically from a config file. That will be done in future.
            
            loadingFromFile = false;
          //  System.out.println("this is load " + DataSource.FILING_LOCAL+"rem"
           // +DataSource.FILING_REMOTE+"Fav"+DataSource.FAVORITES+"goo"+DataSource.GOOGLE);
            try {
                addNewDataSource("My Computer", "My Computer", "", "", "", "", DataSource.FILING_LOCAL,true);
                addNewDataSource("My Favorites", "My Favorites", "", "", "", "", DataSource.FAVORITES,true);
                addNewDataSource("Tufts Digital Library", "Fedora", "", VueResources.getString("dataSouceFedoraPublishableAddress"),VueResources.getString("dataSouceFedoraPublishableUser"), VueResources.getString("dataSouceFedoraPublishablePassword"), DataSource.DR_FEDORA,true);
                addNewDataSource("UVA Finding Aids", "uva:fedora", "", "dl.lib.virginia.edu", "test", "test", DataSource.DR_FEDORA, false);
                addNewDataSource("Tufts Web","google",VueResources.getString("url.google"), "","", "", DataSource.GOOGLE,false);
                addNewDataSource("NYU Web", "google","http://google.nyu.edu/search?site=NYUWeb_Main&client=NYUWeb_Main&output=xml_no_dtd&q=nyu&btnG.x=15&btnG.y=9", "","", "", DataSource.GOOGLE, false);
                
            } catch (Exception ex) {
              
            }
        }
    }
    
    
    public  static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
        Marshaller marshaller = null;
        
        
        Mapping mapping = new Mapping();
        
        try {
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
    
    
    public  SaveDataSourceViewer unMarshallMap(File file) {
        Unmarshaller unmarshaller = null;
        SaveDataSourceViewer sviewer = null;
        
        
        
        Mapping mapping = new Mapping();
        
        
        try{
            unmarshaller = new Unmarshaller();
            mapping.loadMapping(XML_MAPPING);
            unmarshaller.setMapping(mapping);
            
            FileReader reader = new FileReader(file);
            
            sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
            
            
            reader.close();
            
        }
        catch (Exception e) {
            //System.err.println("DataSourceViewer.SaveDataSourceViewer " + e);
            loadingFromFile = false;
           //e.printStackTrace();
            sviewer = null;
        }
        
        return sviewer;
    }
    
    
   /*
    * static method that returns all the datasource where Maps can be published.
    * Only FEDORA @ Tufts is available at present
    */
    public static Vector getPublishableDataSources(int publishMode) {
        Vector mDataSources = new Vector();
        Enumeration enum =  dataSourceList.getContents().elements();
        while(enum.hasMoreElements()) {
            DataSource mDataSource = (DataSource)enum.nextElement();
            if(mDataSource.getPublishMode() == publishMode || mDataSource.getPublishMode() == Publisher.PUBLISH_ALL_MODES) {
                mDataSources.add(mDataSource);
            }
        }
       
        
        return mDataSources;
        
    }
    
    public void keyPressed(KeyEvent e) {
      
        if(e.getKeyCode() == 116){
              
             refreshDataSourceViewer();
             
            
         }
   }
   
   public void keyReleased(KeyEvent e) {
       
   }
   
   public void keyTyped(KeyEvent e) {
       
        
       
   }

}
