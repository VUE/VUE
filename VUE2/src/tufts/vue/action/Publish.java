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
    private final int BUFFER_SIZE = 10240;
   
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
            Publisher publisher = new Publisher();
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
    
    private void saveActiveMap() throws IOException {
        LWMap map = tufts.vue.VUE.getActiveMap();
        activeMapFile = map.getFile();
        if(activeMapFile == null) {
            String prefix = "vueMap";
            String suffix = ".xml";
            activeMapFile  = File.createTempFile(prefix, suffix);
        }
        ActionUtil.marshallMap(activeMapFile, map);    
    }
    
    private void transferMap() throws IOException,osid.filing.FilingException {
        String host = "dl.tccs.tufts.edu";
        String url = "http://dl.tccs.tufts.edu/~vue/fedora/";
        int port = 21;
        String userName = "vue";
        String password = "vue@at";
        String directory = "public_html/fedora";
        
       // saveActiveMap(); // saving the activeMap;
        
        // transfering it to web-server
          
        fileName = url+activeMapFile.getName();
        FTPClient client = new FTPClient();
        client.connect(host,port);
        client.login(userName,password);
        client.changeWorkingDirectory(directory); 
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.storeFile(activeMapFile.getName(),new FileInputStream(activeMapFile));
        client.logout();
        client.disconnect();
    }
    
    private void createMETSFile() throws IOException, FileNotFoundException, javax.xml.rpc.ServiceException{
        StringBuffer sb = new StringBuffer();
        String s = new String();
        String templateFile = "obj-binary.xml";
        FileInputStream fis = new FileInputStream(new File(templateFile));
        DataInputStream in = new DataInputStream(fis); 

        byte[] buf = new byte[BUFFER_SIZE];
        int ch;
        int len;
        while((len =fis.read(buf)) > 0) {
            s = s+ new String(buf);
          
        }
        fis.close();
        in.close();
      //  s = sb.toString();
        String r =  s.replaceAll("%file.location%", fileName).trim();
        
        //writing the to outputfile
        File METSfile = File.createTempFile("vueMETSMap",".xml");
        FileOutputStream fos = new FileOutputStream(METSfile);
        fos.write(r.getBytes());
        fos.close();
        AutoIngestor a = new AutoIngestor("130.64.77.144", 8080,"fedoraAdmin","fedoraAdmin");
        String pid = a.ingestAndCommit(new FileInputStream(METSfile),"Test Ingest");
        
        System.out.println("INDEX = "+s.indexOf("%file.location%")+" METSfile= " + METSfile.getPath()+" PID = "+pid);
        
    }
    
    
    private void createIMSCP() throws IOException,URISyntaxException {
        
        LWMap map = tufts.vue.VUE.getActiveMap();
        IMSCP imscp = new IMSCP();
        saveActiveMap();
        System.out.println("Writing Active Map : "+activeMapFile.getName());
        imscp.putEntry(activeMapFile);
        Vector localResourceVector = new Vector();
        setLocalResourceVector(localResourceVector,map);
        Iterator i = localResourceVector.iterator();
        
        while(i.hasNext()) {
            Resource r = (Resource)i.next();
            File file = new File(r.getSpec().substring(8));
            if(file.isFile()) {
                 imscp.putEntry("resource/"+file.getName(),file);
                 System.out.println("Resource = " + r+"size = "+r.getSize()+ " FileName = "+file.getName());
            }    
           
        }
       
        imscp.closeZOS();
        activeMapFile = imscp.getFile();
        
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
