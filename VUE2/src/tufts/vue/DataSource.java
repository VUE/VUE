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


public class DataSource {
    public static final int FAVORITES = 0;
    public static final int FILING_LOCAL = 1;
    public static final int FILING_REMOTE = 2;
    public static final int DR_FEDORA = 3;
    public static final int GOOGLE = 4;
    private String id;
    private int type;
    private String displayName; 
    private String name;  
    private String address;   
    private String userName;    
    private String password;
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
    public DataSource(String id,String displayName,String name,int type) throws java.net.MalformedURLException{
        this(id,displayName,name);
        this.type=type;
        setViewer();
    }
    
    /**  Creates a DataSource given an id, display name, name, address, user name, password and type. */
    public DataSource (String id, String displayName, String name, String address, String user, String password, int type) throws java.net.MalformedURLException{
        this(id, displayName, name);
        this.address = address;
        this.userName = user;
        this.type=type;
        this.password = password;
        setViewer();
    }
    
    /**
     *  Intializes resource viewer and colors based on data source type.
     */
    private void setViewer () throws java.net.MalformedURLException{
        if(type == FAVORITES) {
            
            VUE.favoritesWindow = new FavoritesWindow(displayName);
            this.resourceViewer =  VUE.favoritesWindow;

                     
        }
        else if(type == FILING_LOCAL) {
            Vector fileVector  = new Vector();
            if (VueUtil.isWindowsPlatform()) {
                fileVector.add(new File("C:\\"));
            } else if (VueUtil.isMacPlatform()) {
                // todo: if OSX, add dirs in /Volumes (other mounted disks)
                // Also would be nice if we could label "/" as "Macintosh HD",
                // or even find out from the OS what the user has it labeled.
                fileVector.add(new File("/"));
            } else
                fileVector.add(new File("/"));
            VueDragTree fileTree = new VueDragTree(fileVector.iterator(),displayName);
            JScrollPane jSP = new JScrollPane(fileTree);   
            this.resourceViewer = jSP;
            
        }
        else if (type == FILING_REMOTE) {
            
        }
        else if(type== DR_FEDORA) {
            this.resourceViewer = new DRViewer("fedora.conf", id,displayName,displayName,new URL("http",this.address,8080,"fedora/"),userName,password);
            
        } 
        else  if (type== GOOGLE) {
            this.resourceViewer = new TuftsGoogle();
           
        } else {
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
