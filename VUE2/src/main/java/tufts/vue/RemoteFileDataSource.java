/*
* Copyright 2003-2010 Tufts University  Licensed under the
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


public class RemoteFileDataSource extends BrowseDataSource
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(RemoteFileDataSource.class);
    
    public static final String ANONYMOUS = "anonymous";
    
    private String UserName;
    private String password;
    
    public RemoteFileDataSource() {
        try {
            setDisplayName("Unconfigured FTP");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
 
    public RemoteFileDataSource(String DisplayName, String address, String username, String password) throws DataSourceException {
        this.setDisplayName(DisplayName);
        this.setAddress(address);
        this.setUserName(username);
        this.setPassword(password);
    }
    
    @Override
    public void setConfiguration(java.util.Properties p) {

        super.setConfiguration(p);
        
        String val = null;
        try {
            if ((val = p.getProperty("username")) != null)
                setUserName(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
        try {
            if ((val = p.getProperty("password")) != null)
                setPassword(val);
        } catch (Throwable t) {
            Log.error("val=" + val, t);
        }
    }
    
    
    @Override
    public String getTypeName() {
        return "Remote Directory";
    }
    
    public void setUserName(String username){
        this.UserName = username;
    }
    public String getUserName(){
        return this.UserName;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    @Override
    protected JComponent buildResourceViewer() {
        
        Vector cabVector = new Vector();
        
        try {
            
            RemoteFilingManager manager = new RemoteFilingManager();   // get a filing manager
            manager.createClient(this.getAddress(),this.getUserName(),this.getPassword());       // make a connection to the ftp site
            RemoteCabinetEntryIterator rootCabs = (RemoteCabinetEntryIterator) manager.listRoots();
            osid.shared.Agent agent = null; //  This may cause problems later.

            while (rootCabs.hasNext()) {
                RemoteCabinetEntry rootNode = (RemoteCabinetEntry)rootCabs.next();
                CabinetResource res = CabinetResource.create(rootNode);
                cabVector.add(res);
            }
            
        } catch (osid.filing.FilingException e) {
            throw new DataSourceException(null, e);
        }

        VueDragTree fileTree = new VueDragTree(cabVector, getDisplayName());
        // do we really need to show then hide the root here?
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);

        return fileTree;
        
//         VueDragTree fileTree = new VueDragTree(cabVector, this.getDisplayName());
//         JScrollPane rSP = new JScrollPane(fileTree);
//         JPanel localPanel = new JPanel();
//         localPanel.setMinimumSize(new Dimension(290,100));
//         localPanel.setLayout(new BorderLayout());
//         localPanel.add(rSP,BorderLayout.CENTER);
//         return localPanel;
// //        DataSourceViewer.refreshDataSourcePanel(this);
    }
    
//     @Override
//     public synchronized JComponent getResourceViewer() {

//         if (resourceViewer == null)
//             resourceViewer = buildResourceViewer();
        
//         return resourceViewer;
        
//     }
    
    
    
}











