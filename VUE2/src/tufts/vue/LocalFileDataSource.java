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
 */
 
// $Header: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/tufts/vue/LocalFileDataSource.java,v 1.10 2005-03-24 23:38:05 sfraize Exp $

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


/**
 * @author  rsaigal
 */

public class LocalFileDataSource extends VueDataSource implements Publishable{
    
    private JComponent resourceViewer;
    
    public LocalFileDataSource() {        
    }
    
    public LocalFileDataSource(String displayName, String address) throws DataSourceException
    {
        this.setDisplayName(displayName);
        this.setAddress(address);
        
    }
    
    public void setAddress(String address)  throws DataSourceException{
        super.setAddress(address);
        this.setResourceViewer();
    }

    public void setResourceViewer()
    {
        Vector cabVector = new Vector();
        
        if (getDisplayName().equals("My Computer")) {
            // This is a bit of a hack, but we need to do this for
            // now because when the resource viewer saves it's state,
            // it saves even the defaults (so we can't make this a subclass).
            installDesktopFolders(cabVector);
        }

        if (this.getAddress().length() > 0) {
            osid.shared.Agent agent = null; //  This may cause problems later.
            LocalCabinet rootNode = new LocalCabinet(this.getAddress(),agent,null);
            CabinetResource res = new CabinetResource(rootNode);
            cabVector.add(res);
        }
        
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
    
    private void installDesktopFolders(Vector cabVector)
    {
        osid.shared.Agent agent = null; //  This may cause problems later.

        File home = new File(System.getProperty("user.home"));
        if (home.exists() && home.canRead()) {
            // This might be better handled via addRoot on the LocalFilingManager, but
            // we can't set the label (title) for it that way. -- SMF
            String[] dirs = { "Desktop", "My Documents", "Documents", "Pictures", "My Pictures", "Photos", "My Photos"};
            int added = 0;
            for (int i = 0; i < dirs.length; i++) {
                File dir = new File(home, dirs[i]);
                if (dir.exists() && dir.canRead()) {
                    CabinetResource r = new CabinetResource(new LocalCabinet(dir.getPath(), agent, null));
                    r.setTitle(dirs[i]);
                    cabVector.add(r);
                    added++;
                }
            }
            if (added == 0 || tufts.Util.isWindowsPlatform() == false) {
                CabinetResource r = new CabinetResource(new LocalCabinet(home.getPath(), agent, null));
                String title = "Home";
                String user = System.getProperty("user.name");
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
                CabinetResource r =
                    new CabinetResource(new LocalCabinet(v.getPath(), agent, null));
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

        try {
            final FileSystemView fsview = FileSystemView.getFileSystemView();
            final LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                
            LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
            while(rootCabs.hasNext()){
                LocalCabinetEntry rootNode = (LocalCabinetEntry)rootCabs.next();
                CabinetResource res = new CabinetResource(rootNode);
                if (rootNode instanceof LocalCabinet) {
                    File f = ((LocalCabinet)rootNode).getFile();
                    try {
                        if (f.getCanonicalPath().equals("/") && gotSlash)
                            continue;
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                    String sysName = fsview.getSystemDisplayName(f);
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









