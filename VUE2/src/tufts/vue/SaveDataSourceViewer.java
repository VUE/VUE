package tufts.vue;


import javax.swing.*;
import java.util.Vector;
import java.io.*;


/**
 *
 * @author  rsaigal
 */
public class SaveDataSourceViewer {
   
    private Vector SaveDataSources;
   
    
    
    public SaveDataSourceViewer() {
    }
    
    public SaveDataSourceViewer(Vector dataSources){
        
        setSaveDataSources(dataSources);
        
     }
   
     public Vector getSaveDataSources(){
            return (this.SaveDataSources);
           
      }
     
     public void setSaveDataSources(Vector dataSources){
          this.SaveDataSources = dataSources;
         
     }
     
   
}