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


/*
 * VueDragGrid.java
 *
 * Created on May 5, 2003, 4:08 PM
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
import java.util.Vector;
import javax.swing.event.*;
import osid.dr.*;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;


import javax.swing.tree.*;
import java.util.Iterator;

/**
 *
 * @version $Revision: 1.4 $ / $Date: 2006-04-08 01:49:07 $ / $Author: sfraize $
 * @author  rsaigal
 */
public class VueDragGrid extends JTree
    implements DragGestureListener,
               DragSourceListener,
               TreeSelectionListener,
               ActionListener
{
    
    public static ResourceNode oldnode;
    private ResourceSelection resourceSelection = null;
    private static  ImageIcon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
    private static ImageIcon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
    private static  ImageIcon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;
    ///private javax.swing.JPanel previewPanel = null;
    //	private tufts.vue.gui.DockWindow previewDockWindow = null;

    public VueDragGrid(Object  obj, String treeName) {
        setModel(createTreeModel(obj, treeName));
        setName(treeName);
        setRowHeight(40);
        this.setRootVisible(true);
        this.expandRow(0);
        this.expandRow(1);
        this.setRootVisible(false);
        implementDrag(this);
        createPopupMenu();
        
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        //getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);

       resourceSelection = VUE.getResourceSelection();
       addTreeSelectionListener(this);

       addMouseListener(new MouseAdapter() {    
               public void mouseClicked(MouseEvent me){
                   if  (me.getClickCount() != 2)
                       return;
                   
                   TreePath path = getPathForLocation(me.getX(), me.getY());
                   if (path == null)
                       return;
                   
                   Object c = path.getLastPathComponent();
                   if (c instanceof CabinetNode) {
                       CabinetNode cabNode = (CabinetNode) path.getLastPathComponent();
                       Object uo = cabNode.getUserObject();
                       if (uo instanceof Resource)
                           ((Resource)uo).displayContent();
                   }
                   
               }
           });
        
    }
    
    public VueDragGrid(FavoritesNode favoritesNode)
    {
        setModel(new DefaultTreeModel(favoritesNode));
        
        this.expandRow(0);
        createPopupMenu();
        
        implementDrag(this);
        resourceSelection = VUE.getResourceSelection();
        addTreeSelectionListener(this);
    }
    
    
    private void  implementDrag(VueDragGrid tree){
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(tree,
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      tree);
        addTreeExpansionListener(new TreeExpansionListener(){
            public void treeCollapsed(TreeExpansionEvent e) {}
            public void treeExpanded(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if(path != null) {
                    if (path.getLastPathComponent() instanceof FileNode){
                        FileNode node = (FileNode)path.getLastPathComponent();
                        if( !node.isExplored()) {
                            DefaultTreeModel model =
                            (DefaultTreeModel)getModel();
                            node.explore();
                            model.nodeStructureChanged(node);
                        }
                    }
                }
            }
        });
        //  Add a tree will expand listener.
        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (path.getLastPathComponent() instanceof CabinetNode) {
                    CabinetNode cabNode = (CabinetNode) path.getLastPathComponent();
                  
                    if (cabNode == null) return;
                    
                    setSelectionPath(path);
                      
                   if (cabNode.getCabinet() != null)cabNode.getDataModel().reload();
                 
                    
                   
                }
            }
            public void treeWillCollapse(TreeExpansionEvent e) {}
        });
        
        
        VueDragGridCellRenderer renderer = new VueDragGridCellRenderer(tree);
        tree.setCellRenderer(renderer);
        
        
        ToolTipManager.sharedInstance().registerComponent(tree);
    }
    
    
    
    
    private DefaultTreeModel createTreeModel(Object obj, String treeName ){
        
        ResourceNode root = new ResourceNode(new MapResource(treeName));
        
        if (obj instanceof Iterator){
            
            Iterator i = (Iterator)obj;
            
            while (i.hasNext()){
                Object resource = i.next();
         
                if (resource instanceof CabinetResource) {
                    CabinetResource cabRes = (CabinetResource) resource;
                    CabinetEntry entry = cabRes.getEntry();
                    CabinetNode cabNode = null;
                    if (entry instanceof RemoteCabinetEntry)
                        cabNode = new CabinetNode(cabRes, CabinetNode.REMOTE);
                    else
                        
                        cabNode = new CabinetNode(cabRes, CabinetNode.LOCAL);
                    //System.out.println("ADDING CABNODE " + cabNode + " resource title " + cabRes.getTitle());
                    root.add(cabNode);
                    //System.out.println(" I am here in Vue drag" + cabNode.getCabinet());
                   if (cabNode.getCabinet() != null)cabNode.explore();
                    //root.add(new ResourceNode((Resource)resource));
                } else {
                    ResourceNode node = new ResourceNode((Resource)resource);
                    root.add(node);
                }
            }
        }
        return new DefaultTreeModel(root);
    }
    
    //****************************************
    
    public void dragGestureRecognized(DragGestureEvent e) {
      
        if (getSelectionPath() != null) {
            TreePath path = getLeadSelectionPath();
            oldnode = (ResourceNode)path.getLastPathComponent();
            ResourceNode parentnode = (ResourceNode)oldnode.getParent();

            //Object resource = getObject();
            Resource resource = oldnode.getResource();
            
            if (DEBUG.DND) System.out.println(this + " dragGestureRecognized " + e);
            if (DEBUG.DND) System.out.println("selected node is " + oldnode.getClass() + "[" + oldnode + "] resource=" + resource);

            if (resource != null) {

                Image imageIcon = nleafIcon.getImage();
                if (resource.getType() == Resource.DIRECTORY) {
                    imageIcon = activeIcon.getImage();
                } else if (oldnode instanceof CabinetNode) {
                    CabinetNode cn = (CabinetNode) oldnode;
                    if (!cn.isLeaf())
                        imageIcon = activeIcon.getImage();
                }
                
                e.startDrag(DragSource.DefaultCopyDrop, // cursor
                            imageIcon, // drag image
                            new Point(-10,-10), // drag image offset
                            new tufts.vue.gui.GUI.ResourceTransfer(resource),
                            //new VueDragGridNodeSelection(resource), // transferable
                            this);  // drag source listener
            }
        }
    }
    
    
    public void dragDropEnd(DragSourceDropEvent e) {
        if (tufts.vue.VUE.dropIsLocal == true){
            DefaultTreeModel model = (DefaultTreeModel)this.getModel();
            model.removeNodeFromParent(oldnode);
            tufts.vue.VUE.dropIsLocal = false;
        }
    }
    
    public void dragEnter(DragSourceDragEvent e) { }
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {
        if (DEBUG.DND) System.out.println("VueDragGrid: dropActionChanged  to  " + tufts.vue.gui.GUI.dropName(e.getDropAction()));
    }
    
    
    public Object getObject() {
        TreePath path = getLeadSelectionPath();
        if (path == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        return (node.getUserObject());
    }
    
    public void valueChanged(TreeSelectionEvent e) {
        try {
            if(e.getPath().getLastPathComponent() != null ) {
                Resource resource = (Resource)((ResourceNode)e.getPath().getLastPathComponent()).getResource();
                resourceSelection.setTo(resource);
				
                /*
                this.previewDockWindow.setVisible(true);
                this.previewPanel.removeAll();
                this.previewPanel.add(resource.getPreview());
                this.previewPanel.repaint();
                this.previewPanel.validate();
                */
                //resourceSelection.remove(createResource(e.getPath().getLastPathComponent()));
            }
            /**
         if(e.getPath().getPathComponent(0) != null) {
             //resourceSelection.add(createResource(e.getPath().getLastPathComponent()));
             System.out.println("Added Resource = "+createResource(e.getPath().getLastPathComponent())+" : size = "+resourceSelection.size());
         }
             **/
        } catch(Exception ex) {
            // VueUtil.alert(null,ex.toString(),"Error in VueDragGrid Selection");
            System.out.println("VueDragGrid.valueChanged "+ex.getMessage());
            ex.printStackTrace();
        }
        // System.out.println("elements in path = "+e.getPath().getPathCount());
    }
    
    static class VueDragGridCellRenderer extends DefaultTreeCellRenderer{
        String meta = "";
        protected VueDragGrid tree;   
		
        public VueDragGridCellRenderer(VueDragGrid vdTree) {
            this.tree = vdTree;
        }
		
        public Component getTreeCellRendererComponent(
                                                      JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel,
                                               expanded, leaf, row,
                                               hasFocus);
            if (leaf) {
                //Osid2AssetResource resource = (Osid2AssetResource)((DefaultMutableTreeNode)value).getUserObject();
                Resource resource = (Resource)((DefaultMutableTreeNode)value).getUserObject();
                setIcon(resource.getIcon(36,36));
            }
            return this;				 
        }
        
        public String getToolTipText(){
            return meta;
        }
    }
    
    
    public void createPopupMenu() {
        JMenuItem menuItem;
        
        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        menuItem = new JMenuItem("Open Resource");
        menuItem.addActionListener(this);
        popup.add(menuItem);
       
        
        
        //Add listener to the text area so the popup menu can come up.
        MouseListener popupListener = new PopupListener(popup);
        this.addMouseListener(popupListener);
    }
    public void actionPerformed(ActionEvent e) {
        
        if (e.getSource() instanceof JMenuItem){
            
            
            JMenuItem source = (JMenuItem)(e.getSource());
            TreePath tp = this.getSelectionPath();
            
           
            
            if (tp != null){
                ResourceNode resNode = (ResourceNode)tp.getLastPathComponent();
                resNode.getResource().displayContent();
            }
        }
    }
    
 class PopupListener extends MouseAdapter {
    JPopupMenu popup;
    
    
    PopupListener(JPopupMenu popupMenu) {
        popup = popupMenu;
    }
    
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }
    
    public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
    }
    
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }
    
    private void maybeShowPopup(MouseEvent e) {
        if (VueDragGrid.this.getSelectionPath() != null){
        if (e.isPopupTrigger()) {
            
            
            popup.show(e.getComponent(),
            e.getX(), e.getY());
            
        }
    }
    }
}
}

