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
    
    public DataSource(String id,String displayName,String name) {
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        resourceViewer = new JLabel(displayName+" : No Viewer Available");
    }
    
    public DataSource(String id,String displayName,String name,int type){
        this.id = id;
        this.displayName = displayName;
        this.name = name;
        if(type == FAVORITES) {
            setResourceViewer(new FavoritesWindow(displayName));
        }else if(type == FILING_LOCAL) {
            Vector fileVector  = new Vector();
            fileVector.add(new File("C:\\"));
            VueDragTree fileTree = new VueDragTree(fileVector.iterator(),displayName);
            JScrollPane jSP = new JScrollPane(fileTree);   
            setResourceViewer(jSP);
        }else if(type== DR_FEDORA) {
            setResourceViewer(new DRViewer("fedora.conf", id,displayName,displayName));
        } 
         else if(type == GOOGLE) {
             TuftsGoogle jSP = new TuftsGoogle();
             System.out.println("I am in Google");
            setResourceViewer(jSP);
        }
        else {
            setResourceViewer(new JLabel(displayName+" : No Viewer Available"));
        }
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
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
