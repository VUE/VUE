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
    
    private static Icon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
    private static        Icon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
    private static        Icon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;
            
    
    
    private final int ACCEPTABLE_DROP_TYPES =
    
    
    DnDConstants.ACTION_COPY |
    DnDConstants.ACTION_LINK |
    DnDConstants.ACTION_MOVE;
    private final boolean debug = true;
    private final int FAVORITES = DataSource.FAVORITES;
    private final boolean sametree = true;
    private final int newfavoritesnode = 0;
    
    
    public VueDandDTree(FavoritesNode root){
        
        super(root);
        
        this.setEditable(true);
        this.setShowsRootHandles(true);
        this.expandRow(0);
        this.setExpandsSelectedPaths(true);
        this.getModel().addTreeModelListener(new VueTreeModelListener());
        this. getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        VueDandDTreeCellRenderer renderer = new VueDandDTreeCellRenderer(this);
      
        this.setCellRenderer(renderer);
        new DropTarget(this, // component
        ACCEPTABLE_DROP_TYPES, // actions
        this);
        
    }
    
    
    
    
    public void drop(DropTargetDropEvent e ) {
        java.awt.Point dropLocation = e.getLocation();
        
        ResourceNode rootNode;
        
        
        System.out.println("get me source"+  e.getSource());
        
        boolean success = false;
        
        Transferable transfer = e.getTransferable();
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        String resourceName = null;
        // java.util.List fileList = null;
        //java.util.List resourceList = null;
        java.util.List resourceList = null;
        java.util.List fileList = null;
        String droppedText = null;
        DataFlavor foundFlavor = null;
        Object foundData = null;
        
        
        if (debug) System.out.println("drop: found " + dataFlavors.length +  dataFlavors.toString());
        
        
        
        try {
            if (transfer.isDataFlavorSupported(VueDragTreeNodeSelection.resourceFlavor)) {
                
                foundFlavor = VueDragTreeNodeSelection.resourceFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                resourceList = (java.util.List)foundData;
               
                
            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                
                foundFlavor = DataFlavor.javaFileListFlavor;
                
                foundData = transfer.getTransferData(foundFlavor);
                //System.out.println("I am a file DROPP 2");
                fileList = (java.util.List)foundData;
               // System.out.println("I am a file DROPP 3");
                
            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                
                foundFlavor = DataFlavor.stringFlavor;
                foundData = transfer.getTransferData(DataFlavor.stringFlavor);
                droppedText = (String)foundData;
                
                
            } else {
                System.out.println("TRANSFER: found no supported dataFlavors");
                
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer lied about supporting " + foundFlavor);
            e.dropComplete(false);
            return;
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer data did not match declared type! flavor="
            + foundFlavor + " data=" + foundData.getClass());
            e.dropComplete(false);
            return;
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: data no longer available");
            e.dropComplete(false);
            return;
        }
        
        
        
        DefaultTreeModel model = (DefaultTreeModel)this.getModel();
        // New Favorites FolderNew Favorites Folder
        //if ((this.getPathForLocation(dropLocation.x, dropLocation.y) == null)){rootNode = (ResourceNode)model.getRoot();
        //System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        //}
        //else
        
        
        if (dropLocation.x < 4) {rootNode = (ResourceNode)model.getRoot();
        System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        }
        else if ((this.getPathForLocation(dropLocation.x, dropLocation.y) == null)){rootNode = (ResourceNode)model.getRoot();
        System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        }
        
        else{
            rootNode = (ResourceNode)this.getPathForLocation(dropLocation.x, dropLocation.y).getLastPathComponent();
            System.out.println("loc1"+ dropLocation.x+dropLocation.y);
            if (rootNode == tufts.vue.VueDragTree.oldnode){//System.out.println("this is same");
            return;
            }
            boolean parentdrop  = false;
            if (rootNode.getParent() == tufts.vue.VueDragTree.oldnode){System.out.println("Cannot move a parent node into a child.. Can cause infinite loops");
            return;
            }
            
        }
        
        
        System.out.println("this is rootNode" + rootNode + "type" + rootNode.getResource().getType());
        
        if (rootNode.getResource().getType() == FAVORITES){
            
            if (resourceList != null){
                java.util.Iterator iter = resourceList.iterator();
                while(iter.hasNext()) {
                    Resource resource = (Resource) iter.next();
                    System.out.println("RESOURCE FOUND 2" + resource + resource.getTitle() + resource.getSpec());
                    
                    
                    
                    if (resource instanceof CabinetResource){
                        
                        CabinetEntry entry = ((CabinetResource)resource).getEntry();
                        CabinetNode cabNode = null;
                        if (entry instanceof RemoteCabinetEntry)
                            cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.REMOTE);
                        else
                            cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.LOCAL);
                        
                        cabNode.explore();
                        
                        this.setRootVisible(true);
                        model.insertNodeInto(cabNode, rootNode, (rootNode.getChildCount()));
                        this.expandPath(new TreePath(rootNode.getPath()));
                        
                        this.setRootVisible(false);
                        success =true;
                    }
                    else if (resource.getType() == FAVORITES){
                        
                       // System.out.println("Am I in Favorites? was I in this spot ever?");
                        ResourceNode newNode = (ResourceNode)tufts.vue.VueDragTree.oldnode.clone();
                        this.setRootVisible(true);
                        model.insertNodeInto(newNode,rootNode,(rootNode.getChildCount()));
                        
                        insertSubTree(tufts.vue.VueDragTree.oldnode,newNode,model);
                        
                        this.expandPath(new TreePath(rootNode.getPath()));
                        this.setRootVisible(false);
                    }
                    else {
                        
                        ResourceNode newNode =new ResourceNode(resource);
                        this.setRootVisible(true);
                        model.insertNodeInto(newNode, rootNode, (rootNode.getChildCount()));
                        this.expandPath(new TreePath(rootNode.getPath()));
                        //this.expandRow(0);
                        this.setRootVisible(false);
                        
                    }
                    
                }
            }
            
            else  if (fileList != null){
                
                java.util.Iterator iter = fileList.iterator();
                while(iter.hasNext()) {
                    
                    File file = (File)iter.next();
                    
                    System.out.println("this is file " +file);
                    try{
                        LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                        osid.shared.Agent agent = null;
                        
                        LocalCabinet cab = new LocalCabinet(file.getAbsolutePath(),agent,null);
                        CabinetResource res = new CabinetResource(cab);
                        
                        CabinetEntry entry = res.getEntry();
                        CabinetNode cabNode = null;
                        if (entry instanceof RemoteCabinetEntry)
                            cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                        else
                            
                            cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                        
                        
                        this.setRootVisible(true);
                        model.insertNodeInto(cabNode, rootNode, (rootNode.getChildCount()));
                        cabNode.explore();
                    }catch (Exception ex){}
                    this.expandPath(new TreePath(rootNode.getPath()));
                    //this.expandRow(0);
                    this.setRootVisible(false);
                    
                }
                
            }
            
            else  if (droppedText != null){
                
                ResourceNode newNode = new ResourceNode(new MapResource(droppedText));;
                this.setRootVisible(true);
                model.insertNodeInto(newNode, rootNode, (rootNode.getChildCount()));
                this.expandPath(new TreePath(rootNode.getPath()));
                //this.expandRow(0);
                this.setRootVisible(false);
                
                
            }
            
            else {
                
                // System.out.println("Vue Dand D tree it should not get here" );
                
            }
            
            
            
            if (e.isLocalTransfer()){
                tufts.vue.VUE.dropIsLocal = true;
                
                e.acceptDrop(DnDConstants.ACTION_MOVE);
                e.dropComplete(true);
                
            }
        }
        else{
            VueUtil.alert(null, "You can only add resources to a Favorites Folder", "Error Adding Resource to Favorites");
            
            //.dropComplete(false);
        }
        
        
    }
    private void insertSubTree(ResourceNode rootNode,ResourceNode cloneNode, DefaultTreeModel treeModel){
        
        
        int i; int childCount = rootNode.getChildCount();
        
        
        System.out.println("root" + rootNode +"childCount" + childCount);
        for (i = 0; i < childCount; i++){
            
            // ResourceNode newChildc = (ResourceNode)(((ResourceNode)(rootNode.getChildAt(i))).clone());
            ResourceNode newChild = (ResourceNode)(rootNode.getChildAt(i));
            ResourceNode newChildc = (ResourceNode)newChild.clone();
            treeModel.insertNodeInto(newChildc, cloneNode, i);
            insertSubTree(newChild,newChildc,treeModel);
            
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
        boolean hasFocus) {
            
            
          
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if ( !(value instanceof FileNode) && ((((ResourceNode)value).getResource()).getType()==FAVORITES) ){
                
                
                if ((((DefaultMutableTreeNode)value).getChildCount()) > 0 ){ setIcon(activeIcon);}
                else {setIcon(inactiveIcon);}
                
                
                
            }
            
            else if (leaf){ setIcon(nleafIcon);
            
           
            
            }
            
            
            else {setIcon(activeIcon);}
            
            
            
            
            return this;
        }
        
    }
    
    
    public void dragEnter(DropTargetDragEvent me) { }
    
    public void dragExit(DropTargetEvent e) {}
    public void dragOver(DropTargetDragEvent e) { }
    
    
    public void dropActionChanged(DropTargetDragEvent e) { }
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




