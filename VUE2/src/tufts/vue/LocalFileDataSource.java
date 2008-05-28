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
 
// $Header: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/tufts/vue/LocalFileDataSource.java,v 1.29 2008-05-28 00:06:43 sfraize Exp $

import javax.swing.*;
import java.util.Vector;
import java.util.*;
import java.awt.*;

import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

import java.io.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;
import tufts.vue.action.*;
import tufts.Util;


/**
 * @version $Revision: 1.29 $ / $Date: 2008-05-28 00:06:43 $ / $Author: sfraize $
 * @author  rsaigal
 */

public class LocalFileDataSource extends VueDataSource implements Publishable{

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LocalFileDataSource.class);    
    
    private static final LocalFilingManager LocalFileManager = produceManager();

    private static LocalFilingManager produceManager() {
        try {
            return new LocalFilingManager();
        } catch (FilingException t) {
            tufts.Util.printStackTrace(t, "new LocalFilingManager");
        }
        return null;
    }

    public static LocalFilingManager getLocalFilingManager() {
        return LocalFileManager;
    }
    
    public LocalFileDataSource() {
    }
    
    public LocalFileDataSource(String displayName, String address) throws DataSourceException
    {
        if (DEBUG.DR) out("NEW: name=" + Util.tags(displayName) + "; address=" + Util.tag(address) + "; " + address);
        //if (DEBUG.Enabled) Util.printStackTrace(Util.tags(this) + " NEW: name=" + Util.tags(displayName) + "; address=" + Util.tag(address) + "; " + address);
        this.setDisplayName(displayName);
        this.setAddress(address);
        
    }
    
    @Override
    public String getTypeName() {
        return "Local Directory";
    }
    
    @Override
    protected JComponent buildResourceViewer()
    {
        if (DEBUG.Enabled) out("buildResourceViewer...");
        Vector cabVector = new Vector();
        
        if (getDisplayName().equals("My Computer")) {
            // This is a bit of a hack, but we need to do this for
            // now because when the resource viewer saves it's state,
            // it saves even the defaults (so we can't make this a subclass).
            if (DEBUG.Enabled) out("installDesktopFolders...");
            installDesktopFolders(cabVector);
            if (DEBUG.Enabled) out("installDesktopFolders: " + cabVector);
        }

        if (this.getAddress().length() > 0) {
            osid.shared.Agent agent = null; //  This may cause problems later.
            LocalCabinet rootNode = LocalCabinet.instance(this.getAddress(),agent,null);
            CabinetResource res = CabinetResource.create(rootNode);
            cabVector.add(res);
        }
        
        VueDragTree fileTree = new VueDragTree(cabVector, this.getDisplayName());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);

        if (DEBUG.Enabled) out("buildResourceViewer: completed.");
        return fileTree;

//         if (false) {
//             JPanel localPanel = new JPanel();
//             JScrollPane rSP = new JScrollPane(fileTree);
//             localPanel.setMinimumSize(new Dimension(290,100));
//             localPanel.setLayout(new BorderLayout());
//             localPanel.add(rSP,BorderLayout.CENTER);
//             this.resourceViewer = localPanel;
//         } else {
//             this.resourceViewer = fileTree;
//         }
            
        //DataSourceViewer.refreshDataSourcePanel(this);
        
    }

//     @Override
//     public synchronized JComponent getResourceViewer()
//     {
//         if (resourceViewer == null)
//             resourceViewer = buildResourceViewer();
//         return resourceViewer;
//     }
    
    
    
    private void installDesktopFolders(Vector cabVector)
    {
        osid.shared.Agent agent = null; //  This may cause problems later.

        File home = new File(VUE.getSystemProperty("user.home"));
        if (home.exists() && home.canRead()) {
            // This might be better handled via addRoot on the LocalFilingManager, but
            // we can't set the label (title) for it that way. -- SMF
            String[] dirs = { "Desktop", "My Documents", "Documents", "Pictures", "My Documents\\My Pictures", "Photos", "My Documents\\My Photos","Music", "My Documents\\My Music"};
            int added = 0;
            for (int i = 0; i < dirs.length; i++) {
                File dir = new File(home, dirs[i]);
                if (dir.exists() && dir.canRead() && dir.isDirectory()) { // TODO: create workaround for Vista bug
                    // TODO: the above tests ALWAYS RETURN TRUE ON WINDOWS VISTA as of 2008-04-08
                    // (listing the directory will generate an IO error for you, at least, but that will be a very slow way to do this...)
                    CabinetResource r = CabinetResource.create(LocalCabinet.instance(dir, agent, null));

                    // no longer needed: URLResource / CabinetResource will handle for us now
                    //r.setTitle(dirs[i].substring(dirs[i].lastIndexOf("\\") == -1 ? 0 : dirs[i].lastIndexOf("\\")+1, dirs[i].length()));
                    
                    cabVector.add(r);
                    added++;
                }
            }
            if (added == 0 || tufts.Util.isWindowsPlatform() == false) {
                CabinetResource r = CabinetResource.create(LocalCabinet.instance(home, agent, null));
                String title = "Home";
                String user = VUE.getSystemProperty("user.name");
                if (user != null)
                    title += " (" + user + ")";
                r.setTitle(title);
                cabVector.add(r);
            }
        }
        boolean gotSlash = false;
        File volumes = null;
        if (tufts.Util.isMacPlatform()) {
            volumes = new File("/Volumes");
        } else if (tufts.Util.isUnixPlatform()) {
            volumes = new File("/mnt");
        }
        if (volumes != null && volumes.exists() && volumes.canRead()) {
            File[] vols = volumes.listFiles();
            for (int i = 0; i < vols.length; i++) {
                File v = vols[i];
                if (!v.canRead() || v.getName().startsWith("."))
                    continue;
                CabinetResource r = CabinetResource.create(LocalCabinet.instance(v, agent, null));
                r.setTitle(v.getName());
                try {
                    //r.setTitle(v.getName() + " (" + v.getCanonicalPath() + ")");
                    if (v.getCanonicalPath().equals("/"))
                        gotSlash = true;
                } catch (Exception e) {
                    System.err.println(e);
                }
                cabVector.add(r);
            }
        }

		/*
		  I mentioned that at home VUE takes over 10 minutes to start up for me.  
  		  I've located the problem, its not particularly widespread.  The problem 
          is I have 6-7 tufts network shares that are still connected when I go 
          home even though I can't actually reach them off of Tufts network.  The 
          timeouts to reach these shares is what slows Vue to a crawl when starting 
          up. Specifically the call to get the name of the volume like:"xdrive on 
          'tftmwins1' or 'mkorcy01 on Titan\home-tccs$'.  If we're willing to scrap 
          the volume names on windows and just put drive letters "C:\" , "P:\" I can 
          get it from about 12 minutes to <10 seconds.  The calls to .exists() on
          the disconnected drives in LocalFilingManager also sent VUE into a similar
          long timeout waiting period.
		*/
        try {
        	
            FileSystemView fsview = null;
            
            if (!tufts.Util.isWindowsPlatform())
            	fsview = FileSystemView.getFileSystemView();
            
            final LocalFilingManager manager = getLocalFilingManager();   // get a filing manager
                
            LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
            while(rootCabs.hasNext()){
                LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
                CabinetResource res = CabinetResource.create(rootNode);
                if (rootNode instanceof LocalCabinet) {
                    File f = ((LocalCabinet)rootNode).getFile();
                    try {
                        if (f.getCanonicalPath().equals("/") && gotSlash)
                            continue;
                    } catch (Exception e) {
                        System.err.println(e);
                    }
            
                    String sysName = null;
                    if (!tufts.Util.isWindowsPlatform())
                    	sysName = fsview.getSystemDisplayName(f);
                    else
                    	sysName = f.toString();
                    
                    //System.out.println("SysName : " + sysName);
                    if (sysName != null)
                        res.setTitle(sysName);
                }
                cabVector.add(res);
            }
            // setPublishMode(Publisher.PUBLISH_CMAP);
        } catch (Exception ex) {
            ex.printStackTrace();
            VueUtil.alert(null,ex.getMessage(),"Error Setting Reseource Viewer");
        }
    }

        
    public int[] getPublishableModes() {
        int modes[] = {Publishable.PUBLISH_MAP,Publishable.PUBLISH_CMAP,Publishable.PUBLISH_ZIP};
        return modes;
    }
    
    public boolean supportsMode(int mode) {
        if(mode == Publishable.PUBLISH_ALL)
            return false;
        else 
            return true;
    }
    public void publish(int mode,LWMap map) throws IOException{
         System.out.println("ZIP File: "+map.getFile()+ ","+map+", mode:"+mode);            
        if(mode == Publishable.PUBLISH_MAP)
            publishMap(map);
        else if(mode == Publishable.PUBLISH_CMAP)
            publishCMap(map);
        else if(mode == Publishable.PUBLISH_ALL)
            publishAll(map);
        else if(mode == Publishable.PUBLISH_ZIP)
            publishZip(map);
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
        	// Note: resourceVector is never initialized in Publisher class. pdw 11-nov-07
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
            JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
            
        }
    }
    private void publishZip(LWMap map){
        try {
            if(map.getFile() == null) {
                VueUtil.alert("The map is not saved. Please save the map first and export it.","Zip Save Alert");
                return;
            }
        	// Note: resourceVector is never initialized in Publisher class. pdw 11-nov-07
            File savedCMap = PublishUtil.createZip(map,Publisher.resourceVector);
            InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("Export to Zip File","zip"))); 
            int fileLength = (int)savedCMap.length();
            byte bytes[] = new  byte[fileLength];
            while (istream.read(bytes,0,fileLength) != -1)
                ostream.write(bytes,0,fileLength);
            istream.close();
            ostream.close();
        } catch(Exception ex) {
            System.out.println(ex);
             JOptionPane.showMessageDialog(VUE.getDialogParent(), "Map cannot be exported "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace();
        }
    }
    private void publishAll(LWMap map) {
          JOptionPane.showMessageDialog(VUE.getDialogParent(), "Export all Not supported","Export Error",JOptionPane.PLAIN_MESSAGE);
    }


    private void out(String s) {
        Log.debug(String.format("@%x; %s(addr=%s): %s", System.identityHashCode(this), getDisplayName(), getAddress(), s));
    }

    
}









