/*
 * VueDandDTree.java
 *
 * Created on September 17, 2003, 11:41 AM
 */

package tufts.vue;
import tufts.google.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import javax.swing.tree.*;
import java.util.Vector;
import javax.swing.event.*;
import osid.dr.*;


import java.awt.geom.Point2D;
import javax.swing.tree.*;
import java.util.Iterator;
/**
 *
 * @author  rsaigal
 */
public class VueDandDTree extends VueDragTree implements DropTargetListener {
    static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    
    private final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY |
        DnDConstants.ACTION_LINK;
       private final boolean debug = true;
    
       public VueDandDTree(FavoritesNode root){ 
          
               
             super(root,"Bookmarks");
             this.setEditable(true);
             
            
       
                  
             VueDandDTreeCellRenderer renderer = new VueDandDTreeCellRenderer(this);
      
         this.setCellRenderer(renderer);
         
        
        new DropTarget(this, // component
        ACCEPTABLE_DROP_TYPES, // actions
         this);
   
       

        }
        
   public void drop(DropTargetDropEvent e ) {
        
       if ((e.getSourceActions() & ACCEPTABLE_DROP_TYPES) != 0) {
            
            e.acceptDrop(DnDConstants.ACTION_COPY);
        } else {
            if (debug) System.out.println("Dtree: rejecting drop");
            e.rejectDrop();
            return;
        }
       
            java.awt.Point dropLocation = e.getLocation();
    
         TreePath treePath = this.getPathForLocation(dropLocation.x, dropLocation.y);
        
       if (isvalidDropNode(treePath)){
         
          
        boolean success = false;
        Transferable transfer = e.getTransferable();
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();

        String resourceName = null;
         java.util.List fileList = null;
        java.util.List assetList = null;
        
          if (debug) System.out.println("drop: found " + dataFlavors.length + " dataFlavors");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            Object data = null;
            System.out.println("DATA FLAVOR "+flavor+"  Mime type" +flavor.getHumanPresentableName());
            
            if (debug) System.out.print("flavor" + i + " " + flavor.getMimeType());
            try {
                data = transfer.getTransferData(flavor);
            } catch (Exception ex) {
                System.out.println("getTransferData: " + ex);
            }
            if (debug) System.out.println(" transferData=" + data);

            try {
                if (flavor.isFlavorJavaFileListType()) {
                    
                      if (debug) System.out.println("FILE LIST FOUND");
                    fileList = (java.util.List) transfer.getTransferData(flavor);
                   
            java.util.Iterator iter = fileList.iterator();
            
            while (iter.hasNext()) {
                java.io.File file = (java.io.File) iter.next();
                if (debug) System.out.println("\t" + file.getClass().getName() + " " + file);
                 if (file.isDirectory()){
                      if (debug) System.out.println("Dropping Directories not allowed");
                     }
                     else{
                     System.out.println(" file welcome");
                       DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();

                       FileNode newNode = new FileNode(file);
                       DefaultTreeModel model = (DefaultTreeModel)this.getModel();
                       model.insertNodeInto(newNode, node, 0);             
                   
                   
                     }
            }
                success = true;
                      
                    break;
                } else if (flavor.getHumanPresentableName().equals("asset")) {
                     if (debug) System.out.println("ASSET FOUND");
                    assetList = (java.util.List) transfer.getTransferData(flavor);
                     DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();

                      
                       
                         java.util.Iterator iter = assetList.iterator();
                              DefaultTreeModel model = (DefaultTreeModel)this.getModel();
          
              while(iter.hasNext()) {
                       Asset asset = (Asset) iter.next();
                       AssetNode newNode =new  AssetNode(asset);             
                      
                       model.insertNodeInto(newNode, node, 0);             
              }
                    break;
                } else if (flavor.getMimeType().startsWith(MIME_TYPE_TEXT_PLAIN))
                    // && flavor.isFlavorTextType() -- java 1.4 only
                {
                    // checking isFlavorTextType() above should be
                    // enough, but some Windows apps (e.g.,
                    // Netscape-6) are leading the flavor list with
                    // 20-30 mime-types of "text/uri-list", but the
                    // reader only ever spits out the first character.

                    resourceName = readTextFlavor(flavor, transfer);
                    if (resourceName != null){
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
                        DefaultTreeModel model = (DefaultTreeModel)this.getModel();
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(resourceName);
                        model.insertNodeInto(newNode, node, 0);  
                    }
                        break;
                    
                } else {
                    //System.out.println("Unhandled flavor: " + flavor);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                continue;
            }
            
        }

      
   

        e.dropComplete(success);
       }
       
       
       else{
           
          if (debug) System.out.println("Invalid Drop Node");
       }
       
    }
   
   //A special node for book mark files
 
 public boolean isvalidDropNode(TreePath  treePath){
                    
                    if (treePath.getLastPathComponent() instanceof FavoritesNode){
                        
                      return true;
                  
                       }
                 else {
                    
                      return false;
                 }
     }
    
 
 
 
  
 class VueDandDTreeCellRenderer extends DefaultTreeCellRenderer {
    protected VueDandDTree tree;
   // protected ResultNode lastNode;
    private String metaData;
    
   public VueDandDTreeCellRenderer(VueDandDTree pTree) {
        this.tree = pTree;
       
        
                tree.addMouseMotionListener(new MouseMotionAdapter() {
             
             public void mouseClicked(MouseEvent me){
                 
                if  (me.getClickCount() == 1) {
                     TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                
                if (treePath!=null) {
                   
              if (treePath.getLastPathComponent() instanceof FileNode){
                      FileNode node = (FileNode)treePath.getLastPathComponent();
                      File fromNodeFile = node.getFile();
                      System.out.println("File on click" + fromNodeFile);
              }
               }
                    
             }
             }
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                
                if (treePath!=null) {
                   
              if (treePath.getLastPathComponent() instanceof FileNode){
                      FileNode node = (FileNode)treePath.getLastPathComponent();
                      
                   // System.out.println("This is FileNode");  
                  
                    
                 }
                 else {
                     // System.out.println("This is FavNode");  
                 }
                
                
               //  System.out.println("This is in my test");  
                 
                }
        
        
            
            }
            
           
            
            
            
        });
   
    }
   
   

 }

   
    public void dragEnter(DropTargetDragEvent me) { }
      
    public void dragExit(DropTargetEvent e) {}
    public void dragOver(DropTargetDragEvent e) { }
    
    
    public void dropActionChanged(DropTargetDragEvent e) { }
    private String readTextFlavor(DataFlavor flavor, Transferable transfer)
    {
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

     


