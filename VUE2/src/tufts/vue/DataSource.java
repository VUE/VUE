/*
 * DataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Vector;
import java.io.*;
import java.net.URL;
import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;




public class DataSource {
    public static final int FAVORITES = 1;
    public static final int FILING_LOCAL = 0;
    public static final int FILING_REMOTE = 2;
    public static final int DR_FEDORA = 3;
    public static final int GOOGLE = 4;
    public static final int BREAK = 5;
 
    private String id;
    private int type;
    private String displayName; 
    private String name;  
    private String address;   
    private String userName;    
    private String password;
    private String searchURL;
    private boolean activeDataSource = false;
    private JComponent resourceViewer;
    private boolean autoConnect = false;
    
    /** Creates a new instance of DataSource */
  
    public DataSource() {
    }
    
    /**  Creates a DataSource given an id, display name, and name. */
    public DataSource(String id,String displayName,String name) throws java.net.MalformedURLException{
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        resourceViewer = new JLabel(displayName+" : No Viewer Available");
        
    }
   
    
    /**  Creates a DataSource given an id, display name, name, and type. */
    public DataSource(String id,String displayName,String name,int type) throws java.net.MalformedURLException,osid.filing.FilingException {
        this(id,displayName,name);
        this.type=type;
        setViewer();
    }
    
    /**  Creates a DataSource given an id, display name, name, address, user name, password and type. */
    public DataSource (String id, String displayName, String name, String searchURL, String address, String user, String password, int type) throws java.net.MalformedURLException,osid.filing.FilingException {
        this(id, displayName, name);
        this.address = address;
        this.userName = user;
        this.searchURL = searchURL;
        this.type=type;
        this.password = password;
        setViewer();
    }
    
    /**
     *  Intializes resource viewer and colors based on data source type.
     */
    public  void setViewer () throws java.net.MalformedURLException,osid.filing.FilingException {
        if(type == FAVORITES) {
            
           // VUE.favoritesWindow = new FavoritesWindow(displayName);
            //this.resourceViewer =  VUE.favoritesWindow;
            System.out.println("was I here" + this);
            this.resourceViewer = new FavoritesWindow(displayName);
             System.out.println("Done with Favorties");
                     
        }
         else if(type == FILING_LOCAL) {
             
                
              Vector cabVector = new Vector();
            LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
            
            if (address.compareTo("") == 0){
          
            LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots(); 
            osid.shared.Agent agent = null; //  This may cause problems later.
             System.out.println("Was I here in filing local after. Found cabs ="+rootCabs);
            while(rootCabs.hasNext()){
               
                LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
                 CabinetResource res = new CabinetResource (rootNode);
                   System.out.println(" Resource ="+res.getSpec());
               
                cabVector.add (res);

                
            }  
            }
            else {
                
                osid.shared.Agent agent = null;
        
                LocalCabinet rootNode = new LocalCabinet(this.getAddress(),agent,null);
                CabinetResource res = new CabinetResource (rootNode);
                   cabVector.add (res);
                
                
                
            }
            System.out.println("Was I here in Vuedrag" +displayName);
            
            VueDragTree fileTree = new VueDragTree (cabVector.iterator(), displayName);
              fileTree.setRootVisible(true);
                fileTree.setShowsRootHandles(true);
              fileTree.expandRow(0);
             fileTree.setRootVisible(false);
           
            
            JPanel localPanel = new JPanel();
            JScrollPane rSP = new JScrollPane (fileTree);
            localPanel.setMinimumSize(new Dimension(300,100));
            localPanel.setLayout(new BorderLayout());
            localPanel.add(rSP,BorderLayout.CENTER);
            this.resourceViewer = localPanel;
        }
        else if (type == FILING_REMOTE) {
            //FilingCabDragTree fcdt = new FilingCabDragTree ("ftp", address, userName, password);
            //JScrollPane rSP = new JScrollPane (fcdt);
            //this.resourceViewer = rSP;
            
            Vector cabVector = new Vector();
            
            RemoteFilingManager manager = new RemoteFilingManager();   // get a filing manager
            manager.createClient(address,userName,password);       // make a connection to the ftp site 
            RemoteCabinetEntryIterator rootCabs = (RemoteCabinetEntryIterator) manager.listRoots(); 
            osid.shared.Agent agent = null; //  This may cause problems later.
            while(rootCabs.hasNext()){
                RemoteCabinetEntry rootNode = (RemoteCabinetEntry)rootCabs.next();
                CabinetResource res = new CabinetResource (rootNode);
                cabVector.add (res);
               
            }    

            VueDragTree fileTree = new VueDragTree (cabVector.iterator(), displayName);
            JPanel remotePanel = new JPanel();
            JScrollPane rSP = new JScrollPane (fileTree);
            remotePanel.setMinimumSize(new Dimension(300,100));
            remotePanel.setLayout(new BorderLayout());
            remotePanel.add(rSP,BorderLayout.CENTER);
            this.resourceViewer = remotePanel;
        }

       
       
        else if(type== DR_FEDORA) {
            this.resourceViewer = new DRViewer("fedora.conf", id,displayName,displayName,new URL("http",this.address,8080,"fedora/"),userName,password);
            
        } 
        else  if (type== GOOGLE) {
            this.resourceViewer = new TuftsGoogle(displayName,searchURL);
           
        }else if (type == BREAK){
            this.resourceViewer = new JPanel();
            
        }
        
        else {
            this.resourceViewer = new JLabel(displayName+" : No Viewer Available");
            
        }
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    public void setSearchURL(String searchURL) {
        this.searchURL = searchURL;
       try{
           this.setViewer();
       }catch (Exception ex){}
    }
    
    public String getSearchURL() {
        return searchURL;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getAddress() {
        return address;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getUserName() {
        return userName;
    }
    public void setPassword(String password)  {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }
   
     public String getId() {
        return this.id;
    }
    
    public void setId(String id){
        this.id = id;
    }

    public boolean isActiveDataSource(){
       
        return this.activeDataSource;
    }
    public void setActiveDataSource(boolean active){
        this.activeDataSource =active;
    }
        
    public boolean isAutoConnect() {
        return this.autoConnect;
    }
    
    public void setAutoConnect(boolean autoConnect){
        this.autoConnect = autoConnect;
    }

    public void setResourceViewer(JComponent resourceViewer) {
      
        this.resourceViewer = resourceViewer;
    }
    
    public int getType() {
        return this.type;
    }
    
    public void setType(int type){
        this.type = type;
    }
    
    public JComponent getResourceViewer() {
        resourceViewer.validate();
        return resourceViewer;
    }
    
    public String toString() {
        return displayName;
    }
    
   
}
