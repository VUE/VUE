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
    private Color displayColor = Color.BLACK;
    private JComponent resourceViewer;
    private boolean autoConnect = false;
    
    /** Creates a new instance of DataSource */
  
    public DataSource() {
    }
    
    /**  Creates a DataSource given an id, display name, and name. */
    public DataSource(String id,String displayName,String name) {
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        resourceViewer = new JLabel(displayName+" : No Viewer Available");
        this.displayColor = Color.GRAY;
    }
    
    /**  Creates a DataSource given an id, display name, name, and type. */
    public DataSource(String id,String displayName,String name,int type){
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        this.type=type;
        setViewer();
    }
    
    /**  Creates a DataSource given an id, display name, name, address, user name, password and type. */
    public DataSource (String id, String displayName, String name, String address, String user, String password, int type) {
        this(id, displayName, name, type);
        this.address = address;
        this.userName = user;
        this.password = password;
    }
    
    /**
     *  Intializes resource viewer and colors based on data source type.
     */
    private void setViewer () {
        if(type == FAVORITES) {
            this.resourceViewer = new FavoritesWindow(displayName);
            this.displayColor = Color.BLUE;
        }
        else if(type == FILING_LOCAL) {
            Vector fileVector  = new Vector();
            fileVector.add(new File("C:\\"));
            VueDragTree fileTree = new VueDragTree(fileVector.iterator(),displayName);
            JScrollPane jSP = new JScrollPane(fileTree);   
            this.resourceViewer = jSP;
            this.displayColor = Color.BLACK;
        }
        else if (type == FILING_REMOTE) {
            
        }
        else if(type== DR_FEDORA) {
            this.resourceViewer = new DRViewer("fedora.conf", id,displayName,displayName);
            this.displayColor = Color.RED;
        } 
        else  if (type== GOOGLE) {
            this.resourceViewer = new TuftsGoogle();
            this.displayColor = Color.GREEN;
        } else {
            this.resourceViewer = new JLabel(displayName+" : No Viewer Available");
            this.displayColor = Color.GRAY;
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
    public String getPasword() {
        return password;
    }
    public void setDisplayColor(Color color) {
        this.displayColor = color;
    }
    public Color getDisplayColor() {
        return displayColor;
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
        return "tufts.vue.DataSource:"+id;
    }
    
   
}
