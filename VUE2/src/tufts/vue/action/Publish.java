/*
 * Publish.java
 *
 * Created on December 24, 2003, 1:46 PM
 */

package tufts.vue.action;

import javax.swing.AbstractAction;
import java.io.*;
import tufts.vue.*;
import java.util.Iterator;
import java.util.Vector;
import java.net.*;
import org.apache.commons.net.ftp.*;


import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;
import fedora.client.ingest.AutoIngestor;

/**
 *
 * @author  akumar03
 */
public class Publish   extends AbstractAction  {
    
    /** Creates a new instance of Publish */
 
   
    String fileName;
    String tempMETSfileName;
    File activeMapFile;
    public Publish() {
    }
    
    public Publish(String label) {
        super(label);
    }
    public void actionPerformed(java.awt.event.ActionEvent e) {
        try {
           // Publisher publisher = new Publisher();
       /**
            createIMSCP();
           
            transferMap();
           createMETSFile();
          **/
        } catch(Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
           ex.printStackTrace();
        }
    }
    
  
    private void setLocalResourceVector(Vector vector,LWContainer map) {
       
       Iterator i = map.getChildIterator();
       while(i.hasNext()) {
           LWComponent component = (LWComponent) i.next();
           if(component.hasResource()){
               Resource resource = component.getResource();
               if(resource.isLocalFile())
                   vector.add(resource);
           }
           if(component instanceof LWContainer) {
                setLocalResourceVector(vector,(LWContainer)component);
           }
       }
     
    }
        
}
