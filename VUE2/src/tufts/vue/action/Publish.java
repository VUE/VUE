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
import java.awt.Frame;

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
    Frame owner;
    String label;
    public Publish() {
    }
    
    public Publish(Frame owner,String label) {
        super(label);
        this.owner = owner;
        this.label  = label;
    }
    public void actionPerformed(java.awt.event.ActionEvent e) {
        try {
            Publisher publisher = new Publisher(owner,label);
      
        } catch(Exception ex) {
            VueUtil.alert(null, ex.getMessage(), "Publish Error");
           ex.printStackTrace();
        }
    }
        
}
