/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package tufts.vue;
 
/*
 * RemoteFileDataSource.java
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



/**
 *
 * @author  rsaigal
 */


public class RemoteFileDataSource extends VueDataSource{
    public static final String ANONYMOUS = "anonymous";
    private JComponent resourceViewer  = new JPanel();
    
    private String UserName;
    private String password;
    
    public RemoteFileDataSource(){
		try {
			this.setDisplayName("Unconfigured FTP");
		} catch (Exception ex) {
			
		}
    }
 
    public RemoteFileDataSource(String DisplayName, String address, String username, String password) throws DataSourceException {
        this.setDisplayName(DisplayName);
        this.setAddress(address);
        this.setUserName(username);
        this.setPassword(password);
 
    }
    
    
    public void setAddress(String address) throws DataSourceException{
        
        super.setAddress(address);
       
        
    }
    
    
    public void setUserName(String username){
        
        this.UserName = username;
        
    }
    public String getUserName(){
        
        return this.UserName;
        
    }
    
    public void setPassword(String password) throws DataSourceException {
        
        this.password = password;
         this.setResourceViewer();
        
    }
    public String getPassword(){
        
        return this.password;
        
    }
    public void  setResourceViewer() throws DataSourceException {
        
        Vector cabVector = new Vector();
        try{
            RemoteFilingManager manager = new RemoteFilingManager();   // get a filing manager
            manager.createClient(this.getAddress(),this.getUserName(),this.getPassword());       // make a connection to the ftp site
            RemoteCabinetEntryIterator rootCabs = (RemoteCabinetEntryIterator) manager.listRoots();
            osid.shared.Agent agent = null; //  This may cause problems later.
            while(rootCabs.hasNext()){
                RemoteCabinetEntry rootNode = (RemoteCabinetEntry)rootCabs.next();
                CabinetResource res = CabinetResource.create(rootNode);
                cabVector.add(res);
                
            }
        }catch (Exception ex)  {
            throw new DataSourceException("RemoteFileDataSource.setResourceViewer:"+ex.getMessage());
        }
        VueDragTree fileTree = new VueDragTree(cabVector, this.getDisplayName());
        JScrollPane rSP = new JScrollPane(fileTree);
        JPanel localPanel = new JPanel();
        localPanel.setMinimumSize(new Dimension(290,100));
        localPanel.setLayout(new BorderLayout());
        localPanel.add(rSP,BorderLayout.CENTER);
        this.resourceViewer = localPanel;
//        DataSourceViewer.refreshDataSourcePanel(this);
    }
    
    public JComponent getResourceViewer(){
        
        return this.resourceViewer;
        
    }
    
    
    
}











