
package tufts.vue;
/*
 * SaveNode.java
 *
 * Created on October 14, 2003, 1:01 PM
 */

/**
 *
 * @author  rsaigal
 */

import java.util.Vector;
import javax.swing.tree.*;
import javax.swing.*;
import java.io.*;
import java.util.Enumeration;

public class SaveNode{
    private String nodeType;
    private String resourceName;
    private Vector children;
    
    public SaveNode(){
        
    }
    
    public SaveNode(Object obj){
             
          if (obj instanceof FavoritesNode){
            this.setNodeType("FavoritesNode");
            this.setResourceName(((FavoritesNode)obj).toString());
        }else if (obj instanceof FileNode){
            this.setNodeType("FileNode");
            File file = (File)((FileNode)obj).getUserObject();
            this.setResourceName(file.toString());
            
       }else if (obj instanceof AssetNode){
            this.setNodeType("AssetNode");
            this.setResourceName(((AssetNode)obj).toString());
            
        }else{
             this.setNodeType("DefaultMutableTreeNode");
             this.setResourceName(((DefaultMutableTreeNode)obj).toString());
            
        }  
        Enumeration e = ((DefaultMutableTreeNode)obj).children();
        Vector v = new Vector();
       
        while (e.hasMoreElements())
        {
            SaveNode child = new SaveNode(e.nextElement());
            v.add(child);
        }
        
        this.setChildren(v);
       
    }
    
    public void setNodeType(String nodetype){
        this.nodeType = nodetype;
        
    }
    
    public String getNodeType(){
        return(this.nodeType);
        
    }
    
    
    public void setResourceName(String resource){
        
        this.resourceName = resource;
    }
    
    public String getResourceName(){
        return (this.resourceName);
        
    }
    
    
    public void setChildren(Vector children){
        
        this.children= children;
        
        
    }
    
    public Vector getChildren(){
         return (this.children);
    }
    
}  
