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


public class DataSource {
  
    private String id;
    private String type;
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
    
    public JComponent getResourceViewer() {
        resourceViewer.validate();
        return resourceViewer;
    }
    
    public String toString() {
        return "tufts.vue.DataSource:"+id;
    }
    
   
}
