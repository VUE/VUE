
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
   
    private Resource resource;
    private Vector children;
    
    public SaveNode(){
        
    }
    
    public SaveNode(ResourceNode resourceNode){
      
            
    
        Enumeration e = resourceNode.children();
        this.setResource(resourceNode.getResource());
        Vector v = new Vector();
       
        while (e.hasMoreElements())
        {
            SaveNode child = new SaveNode((ResourceNode)e.nextElement());
           
            v.add(child);
        }
          
        this.setChildren(v);
     //  System.out.println("I am resource" + this.getResource()+this.getResource().getType()); 
       
    }
    
    
    public void setResource(Resource resource){
        
        this.resource = resource;
    }
    
    public Resource getResource(){
        return (this.resource);
        
    }
    
    
    public void setChildren(Vector children){
        
        this.children= children;
        
        
    }
    
    public Vector getChildren(){
         return (this.children);
    }
    
}  
