package tufts.vue;
/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
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
 
/*
 * LocalFileDataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */



import javax.swing.*;
import java.util.Vector;
import java.util.*;
import java.awt.*;

import javax.swing.border.*;

import java.io.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;
import tufts.vue.action.*;


/**
 *
 * @author  rsaigal
 */


public class LocalFileDataSource extends VueDataSource implements Publishable{
    
    private JComponent resourceViewer;
    
    public LocalFileDataSource(){
        
        
    }
    
    public LocalFileDataSource(String DisplayName, String address){
        this.setDisplayName(DisplayName);
        this.setAddress(address);
        
    }
    
    public void setAddress(String address) {
        super.setAddress(address);
        this.setResourceViewer();
    }
    
    public void  setResourceViewer(){
        
        Vector cabVector = new Vector();
        try{
            LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
            
            if (this.getAddress().compareTo("") == 0){
                LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
                osid.shared.Agent agent = null; //  This may cause problems later.
                while(rootCabs.hasNext()){
                    LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
                    CabinetResource res = new CabinetResource(rootNode);
                    cabVector.add(res);
                }
                
            }
            
            // setPublishMode(Publisher.PUBLISH_CMAP);
            
            
            else {
                osid.shared.Agent agent = null;
                LocalCabinet rootNode = new LocalCabinet(this.getAddress(),agent,null);
                CabinetResource res = new CabinetResource(rootNode);
                cabVector.add(res);
            }
        }catch (Exception ex) {VueUtil.alert(null,ex.getMessage(),"Error Setting Reseource Viewer");}
        VueDragTree fileTree = new VueDragTree(cabVector.iterator(), this.getDisplayName());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);
        JPanel localPanel = new JPanel();
        JScrollPane rSP = new JScrollPane(fileTree);
        localPanel.setMinimumSize(new Dimension(290,100));
        localPanel.setLayout(new BorderLayout());
        localPanel.add(rSP,BorderLayout.CENTER);
        this.resourceViewer = localPanel;
        DataSourceViewer.refreshDataSourcePanel(this);
        
    }
    
    public JComponent getResourceViewer(){
        
        return this.resourceViewer;
        
    }
    
    public int[] getPublishableModes() {
        int modes[] = {Publishable.PUBLISH_MAP,Publishable.PUBLISH_CMAP};
        return modes;
    }
    
    public boolean supportsMode(int mode) {
        if(mode == Publishable.PUBLISH_ALL)
            return false;
        else 
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
        
        File savedMap = PublishUtil.saveMap(map);
        InputStream istream = new BufferedInputStream(new FileInputStream(savedMap));
        OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("ConceptMap","vue")));
        
        int fileLength = (int)savedMap.length();
        byte bytes[] = new  byte[fileLength];
        while (istream.read(bytes,0,fileLength) != -1)
            ostream.write(bytes,0,fileLength);
        istream.close();
        ostream.close();
        
    }
    private void publishCMap(LWMap map) throws IOException {
        try{
            File savedCMap = PublishUtil.createIMSCP(Publisher.resourceVector);
            InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("IMSCP","zip")));
            
            int fileLength = (int)savedCMap.length();
            byte bytes[] = new  byte[fileLength];
            while (istream.read(bytes,0,fileLength) != -1)
                ostream.write(bytes,0,fileLength);
            istream.close();
            ostream.close();
        } catch(IOException ex){
            throw ex;
        } catch(Exception ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(VUE.getInstance(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
            
        }
    }
    private void publishAll(LWMap map) {
          JOptionPane.showMessageDialog(VUE.getInstance(), "Export all Not supported","Export Error",JOptionPane.PLAIN_MESSAGE);
    }
}









