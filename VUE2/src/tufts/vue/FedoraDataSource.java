/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version  (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import javax.swing.*;
import java.net.URL;
import java.io.*;
import java.util.*;
import tufts.vue.action.*;

/**
 *
 * @version $Revision: 1.12 $ / $Date: 2006-01-20 18:30:12 $ / $Author: sfraize $
 * @author  rsaigal
 *
 */
public class FedoraDataSource extends VueDataSource implements Publishable{
    
    private JComponent resourceViewer;
    
    private String UserName;
    private String password;
    private int port;
    
    public FedoraDataSource(){
        
        
    }
    public FedoraDataSource(String displayName, String address, String username, String password) throws DataSourceException{
        this.setDisplayName(displayName);
        this.setAddress(address);
        this.setUserName(username);
        this.setPassword(password);
        this.setResourceViewer(); 
    }
     public FedoraDataSource(String displayName, String address, String username, String password,int port) throws DataSourceException{
        this.setDisplayName(displayName);
        this.setAddress(address);
        this.setUserName(username);
        this.setPassword(password);
        this.setPort(port);
        this.setResourceViewer(); 
    }
    
    
    public void setUserName(String username){
        
        this.UserName = username;
        
    }
    public String getUserName(){
        
        return this.UserName;
        
    }
    
    public void setPassword(String password){
        
        this.password = password;
        
    }
    public int getPort(){
        
        return this.port;
        
    }
     public void setPort(int port){
        
        this.port = port;
        
    }
    public String getPassword(){
        
        return this.password;
        
    }
    
    
    public void  setResourceViewer() throws DataSourceException{
        
        try{
            this.resourceViewer = new DRViewer("fedora.conf",this.getDisplayName(),this.getDisplayName(),this.getDisplayName(),new URL("http",this.getAddress(),this.getPort(),"fedora/"),this.getUserName(),this.getPassword());
        }catch (Exception ex){
            throw new DataSourceException("FedoraDataSource.setResourceViewer "+ex);
        }
    }
    
    public JComponent getResourceViewer(){
        return this.resourceViewer;
        
    }
    public int[] getPublishableModes() {
        int modes[] = {Publishable.PUBLISH_MAP,Publishable.PUBLISH_CMAP,Publishable.PUBLISH_ALL};
        return modes;
    }
    
     public boolean supportsMode(int mode) {
       return true;
    }
     
    public void publish(int mode,LWMap map) throws IOException{
        if(mode == Publishable.PUBLISH_MAP)
            publishMap(map);
        else if(mode == Publishable.PUBLISH_CMAP)
            publishCMap(map);
        else if(mode == Publishable.PUBLISH_ALL)
            publishAll(map);
    }
    
    private void publishMap(LWMap map) throws IOException {
        try {
            File savedMap = PublishUtil.saveMap(map);
            Properties metadata = map.getMetadata();
            String pid = getDR().ingest(savedMap.getName() ,"obj-binary.xml", PublishUtil.VUE_MIME_TYPE,savedMap, metadata).getIdString();
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Exported Map: id = "+pid);
        } catch(IOException ex){
            throw ex;
        } catch(Exception ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
            
        }
    }
    private void publishCMap(LWMap map) throws IOException {
        try{
            File savedCMap = PublishUtil.createIMSCP(Publisher.resourceVector);
            Properties metadata  = map.getMetadata();
            String pid = getDR().ingest(savedCMap.getName(), "obj-vue-concept-map-mc.xml",PublishUtil.ZIP_MIME_TYPE, savedCMap, metadata).getIdString();
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map successfully exported. Asset ID for Map = "+pid, "Map Exported",JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Exported Map: id = "+pid);
        } catch(IOException ex){
            throw ex;
        } catch(Exception ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
            
        }
    }
    private void publishAll(LWMap map) throws IOException {
        try {
            LWMap saveMap = (LWMap)map.clone();
            
            Iterator i = Publisher.resourceVector.iterator();
            while(i.hasNext()) {
                Vector vector = (Vector)i.next();
                Resource r = (Resource)(vector.elementAt(1));
                Boolean b = (Boolean)(vector.elementAt(0));
                // File file = new File((String)vector.elementAt(1));
                System.out.println("RESOURCE = "+r.getSpec());
                URL url = new URL(r.getSpec());
                File file = new File(url.getFile());
                if(file.isFile() && b.booleanValue()) {
                    Publisher.resourceTable.getModel().setValueAt("Processing",Publisher.resourceVector.indexOf(vector),Publisher.STATUS_COL);
                    String pid = getDR().ingest(file.getName(),"obj-binary.xml",url.openConnection().getContentType(),file, r.getProperties()).getIdString();
                    Publisher.resourceTable.getModel().setValueAt("Done",Publisher.resourceVector.indexOf(vector),Publisher.STATUS_COL);
                    PublishUtil.replaceResource(saveMap,r,new AssetResource(getDR().getAsset(new tufts.oki.dr.fedora.PID(pid))));   
                }
            }
            publishMap(saveMap);
        } catch(IOException ex){
            throw ex;
        } catch(Exception ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
            
        }
    }
    
    private tufts.oki.dr.fedora.DR getDR() {
        return ((tufts.oki.dr.fedora.DR)((DRViewer)getResourceViewer()).getDR());
    }
}











