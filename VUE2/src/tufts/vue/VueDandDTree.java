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
  import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

import java.awt.geom.Point2D;

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
    //todo make only a favoritesnode  droppable//
    
    private final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY |
        DnDConstants.ACTION_LINK |
        DnDConstants.ACTION_MOVE;
        private final boolean debug = true;
        private final int FAVORITES = 4;
        private final boolean sametree = true;
        private final int newfavoritesnode = 0;
       
        
    public VueDandDTree(FavoritesNode root){ 
            
            super(root);
           
            this.setEditable(true);
            this.setShowsRootHandles(true);
            this.expandRow(0);
            this.setExpandsSelectedPaths(true);
            this.getModel().addTreeModelListener(new VueTreeModelListener());

            VueDandDTreeCellRenderer renderer = new VueDandDTreeCellRenderer(this);
            this.setCellRenderer(renderer);
            new DropTarget(this, // component
            ACCEPTABLE_DROP_TYPES, // actions
            this);

   }
        
    public void drop(DropTargetDropEvent e ) {       
        java.awt.Point dropLocation = e.getLocation();
        TreePath treePath = this.getPathForLocation(dropLocation.x, dropLocation.y);
        
        
        if ( (treePath != null) && (((ResourceNode)treePath.getLastPathComponent()).getResource().getType() == FAVORITES) ){
            if (e.isLocalTransfer()) 
                e.acceptDrop(DnDConstants.ACTION_MOVE);
            else 
                e.acceptDrop(DnDConstants.ACTION_COPY);
            boolean success = false;
            Transferable transfer = e.getTransferable();
            DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
            String resourceName = null;
            java.util.List fileList = null;
            java.util.List resourceList = null;
           
            if (debug) System.out.println("drop: found " + dataFlavors.length + " dataFlavors");
            try {
                if (transfer.isDataFlavorSupported(VueDragTreeNodeSelection.resourceFlavor)) {
                    if (debug) System.out.println("RESOURCE FOUND" );
                  
                    resourceList = (java.util.List) transfer.getTransferData(VueDragTreeNodeSelection.resourceFlavor);
                    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
                    
                    java.util.Iterator iter = resourceList.iterator();
                    
                    
                    DefaultTreeModel model = (DefaultTreeModel)this.getModel(); 
                    
                    while(iter.hasNext()) {
                        Resource resource = (Resource) iter.next();
                        System.out.println("RESOURCE FOUND 2" + resource + resource.getTitle() + resource.getSpec());
                         
                                   if (resource instanceof CabinetResource){
                             
                                     CabinetEntry entry = ((CabinetResource)resource).getEntry();
                                      CabinetNode cabNode = null;
                                       if (entry instanceof RemoteCabinetEntry)
                                                cabNode = new CabinetNode ((CabinetResource)resource, CabinetNode.REMOTE);
                                         else
                                                 cabNode = new CabinetNode ((CabinetResource)resource, CabinetNode.LOCAL);
                                   
                                        cabNode.explore();
                             
                                      this.setRootVisible(true);
                                        model.insertNodeInto(cabNode, rootNode, (rootNode.getChildCount()));
                                      this.expandPath(new TreePath(rootNode.getPath()));
                                    
                                      this.setRootVisible(false);
                                    }
                         else {
                             
                             ResourceNode newNode =new ResourceNode(resource);
                             this.setRootVisible(true);
                              model.insertNodeInto(newNode, rootNode, (rootNode.getChildCount()));
                              this.expandPath(new TreePath(rootNode.getPath()));
                            this.expandRow(0);
                            this.setRootVisible(false);
                             
                         }   
                      
                      
                          
                    }
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                //System.out.println(ex);
                //continue;
            }
            
            
            e.dropComplete(success);
            }else{
                if (debug) System.out.println("Invalid Drop Node");
            }

    }
   
 
  class VueTreeModelListener implements TreeModelListener {
    public void treeNodesChanged(TreeModelEvent e) {
        ResourceNode node;
        node = (ResourceNode)
                 (e.getTreePath().getLastPathComponent());

        /*
         * If the event lists children, then the changed
         * node is the child of the node we've already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */
        try {
            int index = e.getChildIndices()[0];
            node = (ResourceNode)
                   (node.getChildAt(index));
        } catch (NullPointerException exc) {}

        System.out.println("The user has finished editing the node.");
        System.out.println("New value: " + node.getUserObject());
        
       MapResource resource = (MapResource)node.getResource();
        resource.setTitle(node.getUserObject().toString());
        clearSelection();
     
        
    }
    public void treeNodesInserted(TreeModelEvent e) {
    }
    public void treeNodesRemoved(TreeModelEvent e) {
    }
    public void treeStructureChanged(TreeModelEvent e) {
    }
}

  
 
 
 
  
 class VueDandDTreeCellRenderer extends DefaultTreeCellRenderer {
    protected VueDandDTree tree;
   
    private String metaData;
           
   public VueDandDTreeCellRenderer(VueDandDTree pTree) {
        
       
                this.tree = pTree;
        
                tree.addMouseMotionListener(new MouseMotionAdapter() {
             
                         public void mouseClicked(MouseEvent me){
                 
                         if  (me.getClickCount() == 1) {
                        TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                               }
                           }
                         public void mouseMoved(MouseEvent me) {
                         TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                         
                
                            }          
            
                  });
                  
                   

   
    } 
 
   public Component getTreeCellRendererComponent(JTree tree,Object value, boolean sel,boolean expanded,boolean leaf,int row,
                            boolean hasFocus) 
   {
                                
                       
                     Icon leafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
                     Icon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
                     Icon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;
                     
                 
                   super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
                       if ((((ResourceNode)value).getResource()).getType()==FAVORITES) 
                      {
                          
                                   if ((((DefaultMutableTreeNode)value).getChildCount()) > 0 ){ setIcon(activeIcon);}
                                   else {setIcon(inactiveIcon);}
                        
                          
                       }
                       
                       else if (leaf){ setIcon(leafIcon);}
                     
                       else {setIcon(activeIcon);}
     
                    return this;
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

     


