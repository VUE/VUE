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
     

     public DataSourceList() {
        super(new DefaultListModel());
        

        this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        dropTarget = new DropTarget (this,  ACCEPTABLE_DROP_TYPES, this);

    
         DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,Object value, int index, boolean iss,boolean chf)   {
                super.getListCellRendererComponent(list,((DataSource)value).getDisplayName(), index, iss, chf);
                setIcon(new PolygonIcon(((DataSource)value).getDisplayColor()));
                return this;
            }
        };
        this.setCellRenderer(renderer);      
      }
     
     public DefaultListModel getContents() {
        return (DefaultListModel)getModel();
     }

  public void dragEnter (DropTargetDragEvent e) { }

   public void dragExit (DropTargetEvent e) {}
   public void dragOver (DropTargetDragEvent e) {}
   
   public void drop (DropTargetDropEvent e) {
      
     e.acceptDrop(DnDConstants.ACTION_COPY);
     
   int dropLocation = locationToIndex(e.getLocation());
   this.setSelectedIndex(dropLocation);
   
   DataSource ds = (DataSource)getSelectedValue();
   FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();
   VueDandDTree favoritesTree = fw.getFavoritesTree();
   DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
   FavoritesNode rootNode = (FavoritesNode)model.getRoot();
   
   
   //---------------------transferable Business
   
             
           
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
                 
                                                        
                                                         
                  

                       FileNode newNode = new FileNode(file);
                   
                       model.insertNodeInto(newNode, rootNode, 0);    
                      
                   
                                                        
                                                }
                            success = true;
                      
                                break;
                } else if (flavor.getHumanPresentableName().equals("asset")) {
                     if (debug) System.out.println("ASSET FOUND");
                    assetList = (java.util.List) transfer.getTransferData(flavor);
                   

                      
                       
                         java.util.Iterator iter = assetList.iterator();
                              
          
              while(iter.hasNext()) {
                       Asset asset = (Asset) iter.next();
                       AssetNode newNode =new  AssetNode(asset);             
                      
                       model.insertNodeInto(newNode, rootNode, 0);  
                       
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
                       
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(resourceName);
                        model.insertNodeInto(newNode, rootNode, 0);  
                         
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
       favoritesTree.expandRow(0);
        
       }
       
       
      
   
   
   //---------------------Accept Drop end
   
   
   
      
  

 

  public void dropActionChanged ( DropTargetDragEvent e ) {
      System.out.println( "list Drop action changed");
  }
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




