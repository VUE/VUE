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

package tufts.vue;

/*
 *
 *A List that is droppable for the datasources. Only My favorites will
 *take a drop.
 *
 *Author Ranjani Saigal
 */

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import osid.dr.*;


import java.util.List;
import java.util.Vector;
import java.util.Iterator;

import java.io.*;
import java.io.IOException;
import java.net.URL;
import java.util.regex.*;

import javax.swing.*;

import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;





public class DataSourceList extends JList implements DropTargetListener{
    
    DropTarget dropTarget = null;
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    
    private final int ACCEPTABLE_DROP_TYPES =
    DnDConstants.ACTION_COPY |
    DnDConstants.ACTION_LINK |
    DnDConstants.ACTION_MOVE;
    private final boolean debug = false;
    private final Icon myComputerIcon = VueResources.getImageIcon("dataSourceMyComputer");
    private final Icon myFavoritesIcon = VueResources.getImageIcon("dataSourceMyFavorites");
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private final PolygonIcon breakIcon = new PolygonIcon(Color.LIGHT_GRAY);
    DataSourceViewer dsViewer;
    
    
    
    
    public DataSourceList(DataSourceViewer dsViewer) {
        super(new DefaultListModel());
        this.dsViewer = dsViewer;
        this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.setFixedCellHeight(-1);
        dropTarget = new DropTarget(this,  ACCEPTABLE_DROP_TYPES, this);
        
        breakIcon.setIconWidth(1600);
        breakIcon.setIconHeight(1);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,Object value, int index, boolean iss,boolean chf)   {
                
                if (value instanceof String){
                    
                    super.getListCellRendererComponent(list,"",index,iss,chf);
                }
                        else{
                    
                super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);
                        }
                
               
                
                if (value instanceof FavoritesDataSource){
                    setIcon(myFavoritesIcon);
                    this.setPreferredSize(new Dimension(200,20));
                }
                
                else if (value instanceof LocalFileDataSource){
                    
                     setIcon(myComputerIcon);
                    this.setPreferredSize(new Dimension(200,20)); 
                    
                }
               
                else  if (value instanceof String){
                    
                    
                  
                     JPanel linePanel = new JPanel() {
                   protected void paintComponent(Graphics g) {
                     Graphics2D g2d = (Graphics2D)g;
                        g2d.setColor(Color.LIGHT_GRAY);
                         float dash1[] = {3.0f};
                        BasicStroke dashed = new BasicStroke(1.0f, 
                                                      BasicStroke.CAP_BUTT, 
                                                      BasicStroke.JOIN_MITER, 
                                                     10.0f, dash1, 0.0f);
                        g2d.setStroke(dashed);
                        JPanel tpanel =(JPanel)(DataSourceList.this.dsViewer).getComponent(0);
                        JScrollPane jsp = (JScrollPane)tpanel.getComponent(1);
                        int width = jsp.getViewport().getViewSize().width;
                        g2d.drawLine(0, 3, width-10, 3);

                 
                   }
                   };
                       
                   
                   
                   
                  this.setPreferredSize(new Dimension(200,3));
                    
                  
                  return linePanel;
                    
                   
                    
                }
                 
                else{
                    setIcon(remoteIcon);
                    this.setPreferredSize(new Dimension(200,20));
                    
                }
                
                
                return this;
                
            }
            
            
            
            
        };
        
        this.setCellRenderer(renderer);
    }
    
    
    
    
    
    
    public DefaultListModel getContents() {
        return (DefaultListModel)getModel();
    }
    
    public void dragEnter(DropTargetDragEvent e) { }
    
    public void dragExit(DropTargetEvent e) {}
    public void dragOver(DropTargetDragEvent e) {}
    
    public void drop(DropTargetDropEvent e) {
        e.acceptDrop(DnDConstants.ACTION_COPY);
        int current = this.getSelectedIndex();
       // System.out.println("What is the current index " + this.getSelectedIndex());
        int dropLocation = locationToIndex(e.getLocation());
        this.setSelectedIndex(dropLocation);
        
        
        DataSource ds = (DataSource)getSelectedValue();
        //DataSource ds =(DataSource)((this.getContents()).getElementAt(dropLocation));
        try {
            FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();
            
            
            VueDandDTree favoritesTree = fw.getFavoritesTree();
            favoritesTree.setRootVisible(true);
            DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
            FavoritesNode rootNode = (FavoritesNode)model.getRoot();
            
            
            //---------------------transferable Business
            
            
            
            boolean success = false;
            Transferable transfer = e.getTransferable();
            DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
            
            String resourceName = null;
            java.util.List fileList = null;
            java.util.List resourceList = null;
            try {
                if (transfer.isDataFlavorSupported(VueDragTreeNodeSelection.resourceFlavor)) {
                    if (debug) System.out.println("RESOURCE FOUND");
                    resourceList = (java.util.List) transfer.getTransferData(VueDragTreeNodeSelection.resourceFlavor);
                    java.util.Iterator iter = resourceList.iterator();
                    while(iter.hasNext()) {
                        Resource resource = (Resource) iter.next();
                        
                        if (resource instanceof CabinetResource){
                            
                            CabinetEntry entry = ((CabinetResource)resource).getEntry();
                            CabinetNode cabNode = null;
                            if (entry instanceof RemoteCabinetEntry)
                                cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.REMOTE);
                            else
                                cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.LOCAL);
                            
                            cabNode.explore();
                            
                            
                            model.insertNodeInto(cabNode, rootNode, 0);
                            favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                            
                            favoritesTree.setRootVisible(false);
                        }
                        else{
                            ResourceNode newNode =new  ResourceNode(resource);
                            
                            model.insertNodeInto(newNode, rootNode, 0);
                            favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                            favoritesTree.setRootVisible(false);
                            
                        }
                    }
                }
                else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                    
                    fileList = (java.util.List)transfer.getTransferData(DataFlavor.javaFileListFlavor);
                    java.util.Iterator iter = fileList.iterator();
                    
                    
                    while(iter.hasNext()){
                        
                        File file = (File)iter.next();
                        
                         // System.out.println("did i come out  here "+file);
                          
                          
                          
                          
                        if (file.isDirectory()){
                            
                           // System.out.println("did i come here "+file);
                        
                        try{
                            
                          
                            LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                            osid.shared.Agent agent = null;
                            
                            LocalCabinet cab = new LocalCabinet(file.getAbsolutePath(),agent,null);
                            
                            
                            
                            CabinetResource res = new CabinetResource(cab);
                            
                            
                            
                            CabinetEntry entry = res.getEntry();
                            
                            
                            
                            
                              if (file.getPath().toLowerCase().endsWith(".url")) {
                    // Search a windows .url file (an internet shortcut)
                    // for the actual web reference.
                    String url = convertWindowsURLShortCutToURL(file);
                    if (url != null) {
                       //resourceSpec = url;
                           res.setSpec("file://" + url);
                           String resName;
                        if (file.getName().length() > 4)
                            resName = file.getName().substring(0, file.getName().length() - 4);
                        else
                            resName = file.getName();
                           
                            res.setTitle(resName);
                           
                    }
                    
                }
                           
                            
                            
                            
                            CabinetNode cabNode = null;
                            if (entry instanceof RemoteCabinetEntry)
                                cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                            else
                                
                                cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                            
                       
                        
                            
                      
                        
                        
                            model.insertNodeInto(cabNode, rootNode, 0);
                            favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                            
                            
                            
                            
                            favoritesTree.setRootVisible(false);
                            
                            
                            
                        }catch (Exception EX) {}
                        
                    }
                    
                    else{
                       
                      try{
                            
                          
                            LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                            osid.shared.Agent agent = null;
                            
                            LocalCabinet cab = new LocalCabinet(file.getAbsolutePath(),agent,null);
                            
                            
                            
                            CabinetResource res = new CabinetResource(cab);
                            res.setTitle(file.getAbsolutePath());
                          
                            
                            CabinetEntry oldentry = res.getEntry();
                            
                              res.setEntry(null);
                              
                               if (file.getPath().toLowerCase().endsWith(".url")) {
                    // Search a windows .url file (an internet shortcut)
                    // for the actual web reference.
                    String url = convertWindowsURLShortCutToURL(file);
                    if (url != null) {
                       //resourceSpec = url;
                           res.setSpec("file://" + url);
                           String resName;
                        if (file.getName().length() > 4)
                            resName = file.getName().substring(0, file.getName().length() - 4);
                        else
                            resName = file.getName();
                           res.setTitle(resourceName);
                    }
                }
                            
                            
                              
                            
                            CabinetNode cabNode = null;
                            if (oldentry instanceof RemoteCabinetEntry)
                                cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                            else
                                
                                cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                            
                            model.insertNodeInto(cabNode, rootNode, 0);
                            favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                            
                      
                            
                            favoritesTree.setRootVisible(false);
                            
                            
                            
                        }catch (Exception EX) {}
                   
                        
                    }
                    
                    }
                }
                
                else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)){
                    
                    
                    
                    String dataString = (String)transfer.getTransferData(DataFlavor.stringFlavor);
                    
                    
                    
                    Resource resource = new MapResource(dataString);
                    ResourceNode newNode =new  ResourceNode(resource);
                    
                    model.insertNodeInto(newNode, rootNode, 0);
                    favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                    favoritesTree.setRootVisible(false);
                    
                    
                    
                    
                }
                
                
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                //continue;
            }
            
            
            e.dropComplete(success);
            
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);
            this.setSelectedIndex(current);
            fw.favoritesPane.setSelectedIndex(2);
            // VueUtil.alert(null, "Successfully added resource to "+ds.getDisplayName(),"Resource Added");
        } catch (Exception ex) {
            this.setSelectedIndex(current);
            
            VueUtil.alert(null, "You can only add resources to a Favorites Datasource","Resource Not Added");
            
        }
        
        
    }
     private static final Pattern URL_Line = Pattern.compile(".*^URL=([^\r\n]+).*", Pattern.MULTILINE|Pattern.DOTALL);
    
        private String convertWindowsURLShortCutToURL(File file)
    {
        String url = null;
        try {
            if (debug) System.out.println("*** Searching for URL in: " + file);
            FileInputStream is = new FileInputStream(file);
            byte[] buf = new byte[2048]; // if not in first 2048, don't bother
            int len = is.read(buf);
            is.close();
            String str = new String(buf, 0, len);
            if (debug) System.out.println("*** size="+str.length() +"["+str+"]");
            Matcher m = URL_Line.matcher(str);
            if (m.lookingAt()) {
                url = m.group(1);
                if (url != null)
                    url = url.trim();
                if (debug) System.out.println("*** FOUND URL ["+url+"]");
                int i = url.indexOf("|/");
                if (i > -1) {
                    // odd: have found "file:///D|/dir/file.html" example
                    // where '|' is where ':' should be -- still works
                    // for Windows 2000 as a shortcut, but NOT using
                    // Windows 2000 url DLL, so VUE can't open it.
                    url = url.substring(0,i) + ":" + url.substring(i+1);
                   // System.out.println("**PATCHED URL ["+url+"]");
                }
                // if this is a file:/// url to a local html page,
                // AND we can determine that we're on another computer
                // accessing this file via the network (can we?)
                // then we should not covert this shortcut.
                // Okay, this is good enough for now, tho it also
                // won't end up converting a bad shortcut, and
                // ideally that wouldn't be our decision.
                // [this is not worth it]
                /*
                URL u = new URL(url);
                if (u.getProtocol().equals("file")) {
                    File f = new File(u.getFile());
                    if (!f.exists()) {
                        url = null;
                        System.out.println("***  BAD FILE ["+f+"]");
                    }
                }
                */
            }
        } catch (Exception e) {
            //System.out.println(e);
        }
        return url;
    }
    
    
    //---------------------Accept Drop end
    
    
    
    
    
    
    
    
    public void dropActionChanged( DropTargetDragEvent e ) {
      
    }
    private String readTextFlavor(DataFlavor flavor, Transferable transfer) {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (debug) System.out.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (debug) System.out.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.out.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }
    
    
    
    
    
    
    
}




