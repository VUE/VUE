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
    private final boolean debug = true;
    private final Icon myComputerIcon = new ImageIcon("tufts/vue/images/datasourceMyComputer.gif");
    private final Icon myFavoritesIcon = new ImageIcon("tufts/vue/images/datasourceMyFavorites.gif");
    private final Icon remoteIcon = new ImageIcon("tufts/vue/images/datasourceRemote.gif");
    
    
    public DataSourceList() {
        super(new DefaultListModel());
        this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);  
        dropTarget = new DropTarget(this,  ACCEPTABLE_DROP_TYPES, this);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,Object value, int index, boolean iss,boolean chf)   {
                super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);
                if (((DataSource)value).getType() == DataSource.FAVORITES)
                    setIcon(myFavoritesIcon);
                else if (((DataSource)value).getType() == DataSource.FILING_LOCAL)
                    setIcon(myComputerIcon);
                else 
                    setIcon(remoteIcon);
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
        System.out.println("What is the current index " + this.getSelectedIndex());
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
                                                cabNode = new CabinetNode ((CabinetResource)resource, CabinetNode.REMOTE);
                                         else
                                                 cabNode = new CabinetNode ((CabinetResource)resource, CabinetNode.LOCAL);
                                   
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
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                //continue;
            }
        
        
        e.dropComplete(success);
        
        favoritesTree.expandRow(0);
        favoritesTree.setRootVisible(false);
        this.setSelectedIndex(current);
         VueUtil.alert(null, "Successfully added resource to "+ds.getDisplayName(),"Resource Added");
        } catch (Exception ex) { 
            this.setSelectedIndex(current);
        
            VueUtil.alert(null, "You can only add resources to a Favorites Datasource","Resource Not Added");
            
        }
           
            
    }
    
    
    
    
    
    //---------------------Accept Drop end
    
    
    
    
    
    
    
    
    public void dropActionChanged( DropTargetDragEvent e ) {
        System.out.println( "list Drop action changed");
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




