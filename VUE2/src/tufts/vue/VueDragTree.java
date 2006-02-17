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
 * VueDragTree.java
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
 * @version $Revision: 1.47 $ / $Date: 2006-02-17 20:24:58 $ / $Author: jeff $
 * @author  rsaigal
 */
public class VueDragTree extends JTree
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
	private javax.swing.JPanel previewPanel = null;
	private tufts.vue.gui.DockWindow previewDockWindow = null;
    
    public VueDragTree(Object  obj, String treeName) {
        setModel(createTreeModel(obj, treeName));
        this.setRootVisible(true);
        this.expandRow(0);
        this.expandRow(1);
        this.setRootVisible(false);
        implementDrag(this);
        createPopupMenu();
        
       this. getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
       resourceSelection = VUE.getResourceSelection();
        addTreeSelectionListener(this);
    }
    
    public VueDragTree(Object  obj, 
					   String treeName, 
					   tufts.vue.gui.DockWindow previewDockWindow,
					   javax.swing.JPanel previewPanel) {
        setModel(createTreeModel(obj, treeName));
        this.setRootVisible(true);
        this.expandRow(0);
        this.expandRow(1);
        this.setRootVisible(false);
        implementDrag(this);
        createPopupMenu();
		this.previewPanel = previewPanel;
		this.previewDockWindow = previewDockWindow;
        
		this. getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		resourceSelection = VUE.getResourceSelection();
        addTreeSelectionListener(this);
    }
    
    public VueDragTree(FavoritesNode favoritesNode) {
        setModel(new DefaultTreeModel(favoritesNode));
        
        this.expandRow(0);
        createPopupMenu();
        
        implementDrag(this);
        resourceSelection = VUE.getResourceSelection();
        addTreeSelectionListener(this);
    }
    
    
    private void  implementDrag(VueDragTree tree){
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
        
        
        VueDragTreeCellRenderer renderer = new VueDragTreeCellRenderer(this);
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
                            new VueDragTreeNodeSelection(resource), // transferable
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
        System.out.println("VueDragTree: dropActionChanged  to  " + tufts.vue.gui.GUI.dropName(e.getDropAction()));
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
                resourceSelection.clear();
				Resource resource = (Resource)((ResourceNode)e.getPath().getLastPathComponent()).getResource();
                resourceSelection.add(resource);
				
				this.previewDockWindow.setVisible(true);
				this.previewPanel.removeAll();
				this.previewPanel.add(resource.getPreview());
				this.previewPanel.repaint();
				this.previewPanel.validate();
                //resourceSelection.remove(createResource(e.getPath().getLastPathComponent()));
            }
            /**
         if(e.getPath().getPathComponent(0) != null) {
             //resourceSelection.add(createResource(e.getPath().getLastPathComponent()));
             System.out.println("Added Resource = "+createResource(e.getPath().getLastPathComponent())+" : size = "+resourceSelection.size());
         }
             **/
        } catch(Exception ex) {
            // VueUtil.alert(null,ex.toString(),"Error in VueDragTree Selection");
            System.out.println("VueDragTree.valueChanged "+ex.getMessage());
            ex.printStackTrace();
        }
        // System.out.println("elements in path = "+e.getPath().getPathCount());
    }
    
    class VueDragTreeCellRenderer extends DefaultTreeCellRenderer{
        String meta = "";
        protected VueDragTree tree;   
        public VueDragTreeCellRenderer(VueDragTree vdTree) {
            this.tree = vdTree;
            vdTree.addMouseMotionListener(new MouseMotionAdapter() {    
                public void mouseClicked(MouseEvent me){         
                    if  (me.getClickCount() == 1) {
                        TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                    }
                }
                public void mouseMoved(MouseEvent me) {
                    //tree.clearSelection();
                    TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                    //tree.setSelectionPath(treePath);      
                }   
            });    
        }
        /* -----------------------------------  */   
        
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
            
            if (value instanceof FavoritesNode) {
                
                if ( ((FavoritesNode)value).getChildCount() >0 ) {
                    setIcon(activeIcon);
                }
                else {
                    setIcon(inactiveIcon);
                }
            }
            else if (leaf) {
				/*
				 If we are dealing with an Asset, we can see if it has a preview
				 */
				if (value instanceof ResourceNode) {
					ImageIcon i = ((ResourceNode)value).getResource().getIcon();
					if (i == null) {
						setIcon(nleafIcon);
					} else {						
						setIcon(i);
					}
				} else {
					setIcon(nleafIcon);
				}
			}
            else { 
				/*
				 If we are dealing with an Asset, we can see if it has a preview
				 */
				if (value instanceof ResourceNode) {
					Icon i = ((ResourceNode)value).getResource().getIcon();
					if (i == null) {
						setIcon(activeIcon);
					} else {
						setIcon(i);
					}
				} else {
					setIcon(activeIcon); 
				}
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
        if (VueDragTree.this.getSelectionPath() != null){
        if (e.isPopupTrigger()) {
            
            
            popup.show(e.getComponent(),
            e.getX(), e.getY());
            
        }
    }
    }
}
}



/*---------------*/
//If someday we want to drop favorites onto the map
/*
class ResourceTransfer extends Object  {
    private Resource resource;
    private ResourceNode parent;
    private Vector children;
    
    public ResourceTransfer(){
    }
    
    
    public ResourceTransfer(ResourceNode parent,ResourceNode selectedNode){
        this.parent = parent;
        this.resource = selectedNode.getResource();
        this.children = new Vector();
        int i;
        for (i = 1; i < selectedNode.children(); i++){
            
            
            
        }
        
        
        
        
        }
     public Resource getResource() {
        return resource;
    }
     public ResourceNode getParent() {
        return parent;
    }
    
     public Vector getChildren() {
        return children;
    }
    
}

*/
class ResourceNode extends DefaultMutableTreeNode {
    private boolean explored = false;
    private Resource resource;
    public ResourceNode() {
    }
    public ResourceNode(Resource resource) {
        
        this.resource = resource;
        setUserObject(resource);        
    }
    public Resource getResource() {
        return resource;
    }
    public String toString() {
        return resource.getTitle();
    }
}


class CabinetNode extends ResourceNode {
    //DefaultTreeModel dataModel;
    public static final String LOCAL = "local";
    public static final String REMOTE = "remote";
    private String type = "unknown";
    private boolean explored = false;
    
    
    public CabinetNode(CabinetResource cabinet, String type) {
        super(cabinet);
        this.type = type;
    }
    
    
    /**
     *  Return true if this node is a leaf.
     */
    public boolean isLeaf() {
        CabinetResource res = (CabinetResource) getUserObject();
        if (res.getEntry() == null) return true;
        if(this.type.equals(CabinetNode.REMOTE) && ((RemoteCabinetEntry)res.getEntry()).isCabinet())
            return false;
        else if(this.type.equals(CabinetNode.LOCAL) && ((LocalCabinetEntry)res.getEntry()).isCabinet())
            return false;
        else
            return true;
    }
    
    /**
     *  Return the cabinet entry associated with this tree node.  If it is a cabinet,
     *  then return it.  Otherwise, return null.
     */
    public Cabinet getCabinet() {
        CabinetResource res = (CabinetResource) getUserObject();
        if (res.getEntry() instanceof Cabinet)
            return (Cabinet) res.getEntry();
        return null;
    }
    
    /*
     *  Expand the tree (ie. find the cabinet entries below this node).
     *  This only applies if the current node is a cabinet.
     */
    public void explore() {

        if (this.explored)
            return;
        
        //  If this is not a cabinet, then it cannot be expanded.
        //if(getCabinet() != null) {
       // System.out.println(" Cabinet ="+getUserObject()+ " is dir" +isLeaf()+" Extension = "+((CabinetResource)getUserObject()).getExtension());
        //if(((CabinetResource)getUserObject()).getExtension().equals("dir")) {
       if(getCabinet() != null) {
 
        
           // System.out.println("In cabinet--"+getCabinet());
            
            try {
                if (this.type.equals(CabinetNode.REMOTE)) {
                    
                    CabinetEntryIterator i = (RemoteCabinetEntryIterator) getCabinet().entries();
                    
                    while (i.hasNext()) {
                        CabinetEntry ce = (RemoteCabinetEntry) i.next();
                        if (ce.getDisplayName().startsWith(".")) // don't display dot files
                            continue;
                        CabinetResource res = new CabinetResource(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
                        this.add(rootNode);
                    }
                }
                else if (this.type.equals(CabinetNode.LOCAL)) {
                    CabinetEntryIterator i = (LocalCabinetEntryIterator) getCabinet().entries();
                    
                    while (i.hasNext()) {
                        CabinetEntry ce = (LocalCabinetEntry) i.next();
                       // System.out.println ("CabinetNode explore: "+ce.getDisplayName());
                        if (ce.getDisplayName().startsWith(".")) // don't display dot files
                            continue;
                        CabinetResource res = new CabinetResource(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
                        //rootNode.explore();
                        this.add(rootNode);
                        // todo fix: note, this is still happening twice per
                        // directory on startup! SMF 2005-03-11
                        //System.out.println(this + " adding " + rootNode);
                    }
                    this.explored = true;
                }
            } catch (FilingException e) {
               e.printStackTrace();
                //return;
            }
            return;
        }
        else return;
    }
    
    /**
     *  Return a string version of the node.  In this implementation, the display name
     *  of the cabinet entry is returned.
     */
    public String toString() {
        CabinetResource res = (CabinetResource) getUserObject();
        if (res.getTitle() != null)
            return res.getTitle();
        try {
            CabinetEntry ce = (CabinetEntry) res.getEntry();
            return ce.getDisplayName();
        } catch (Exception e) {
            return userObject.getClass().toString();
        }
    }
    
    /**
     *  Return the data model used.
     */
    DefaultTreeModel getDataModel() {
        explore();
        return new DefaultTreeModel(this);
    }
}

class FileNode extends ResourceNode {
    private boolean explored = false;
    
    public FileNode(File file) 	{
        
      
        setUserObject(file);
        try{
        MapResource resource = new  MapResource(file.toURL().toString());
        }catch (Exception ex){};
      
    }
    public boolean getAllowsChildren() { return isDirectory(); }
    public boolean isLeaf() 	{ return !isDirectory(); }
    public File getFile()		{ return (File)getUserObject(); }
    
    public boolean isExplored() { return explored; }
    
    public boolean isDirectory() {
        
        File file = getFile();
        
        if (file != null) {
            return file.isDirectory();
        }
        else {
            return false;
        }
        
    }
    public String toString() {
        File file = (File)getUserObject();
        String filename = file.toString();
        int index = filename.lastIndexOf(File.separator);
        
        return (index != -1 && index != filename.length()-1) ?
        filename.substring(index+1) :
            filename;
    }
    
   public void displayContent(){
       
       
       try{
           URL url = getFile().toURL();
       VueUtil.openURL(url.toString().replaceFirst("/",""));
       }catch (Exception ex){System.out.println("problem opening conten");}
   }
    
   
    public void explore() {
        
        if(!isDirectory())
            return;
        
        if(!isExplored()) {
            File file = getFile();
            File[] contents = file.listFiles();
            
            for(int i=0; i < contents.length; ++i)
                add(new FileNode(contents[i]));
            
            explored = true;
        }
    }
}
class FavoritesNode extends ResourceNode {
    
    public FavoritesNode(Resource resource){
        super(resource);
        
        
    }
    public void explore() {
        this.explore();
    }
}


class VueDragTreeNodeSelection extends Vector implements Transferable {
    /**
     * try {
     * assetFlavor = new DataFlavor(Class.forName("osid.dr.Asset"),"asset");
     * } catch (Exception e) { System.out.println("FedoraSelection "+e);}
     **/
    
    /*
    private DataFlavor flavors[] = {
        DataFlavor.stringFlavor,
        //DataFlavor.javaFileListFlavor
    };
    */

    //private String displayName = "";
   
    private java.util.List flavors = new java.util.ArrayList(3);
    
    public VueDragTreeNodeSelection(Object resource) {
        addElement(resource);

        flavors.add(DataFlavor.stringFlavor);

        if (resource instanceof MapResource) {
            
            flavors.add(Resource.DataFlavor);
            /*
            try {
                displayName = ((Resource)elementAt(0)).getTitle();
            } catch (Exception e) { System.out.println("FedoraSelection "+e);}
            */

        } else if (resource instanceof File) {

            flavors.add(DataFlavor.javaFileListFlavor);
            //displayName = ((File)elementAt(0)).getName();

        } else {
            
            //displayName = elementAt(0).toString();
            
        }
    }
    
    /* Returns the array of flavors in which it can provide the data. */
    public synchronized java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[]) flavors.toArray(new DataFlavor[flavors.size()]);
    }
    
    /* Returns whether the requested flavor is supported by this object. */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (flavor == null)
            return false;

        for (int i = 0; i < flavors.size(); i++)
            if (flavor.equals(flavors.get(i)))
                return true;
        
        return false;
    }
    
    /**
     * If the data was requested in the "java.lang.String" flavor,
     * return the String representing the selection.
     */
    public synchronized Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        if (DEBUG.DND && DEBUG.META) System.out.println("VueDragTreeNodeSelection: getTransferData, flavor=" + flavor);
        
        Object result = null;
        
        if (DataFlavor.stringFlavor.equals(flavor)) {
            
            // Always support something for the string flavor, or
            // we get an exception thrown (even tho I think that
            // may be against the published API).
            Object o = get(0);
            if (o instanceof File)
                result = ((File)o).toString();
            else
                result = ((Resource)o).getSpec();
            
        } else if (Resource.DataFlavor.equals(flavor)) {
            
            result = (java.util.List) this;
            
        } else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            
            result = (java.util.List) this;

        } else {
        
            throw new UnsupportedFlavorException(flavor);
        }
        
        if (DEBUG.DND && DEBUG.META) System.out.println("\treturning " + result.getClass() + "[" + result + "]");

        return result;
    }
    
}



