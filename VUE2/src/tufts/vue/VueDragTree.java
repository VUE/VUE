
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

import tufts.Util;
import tufts.vue.gui.GUI;
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
 * @version $Revision: 1.68 $ / $Date: 2007-10-18 19:37:20 $ / $Author: sfraize $
 * @author  rsaigal
 */
public class VueDragTree extends JTree
    implements ResourceSelection.Listener,
               DragGestureListener,
               DragSourceListener,
               TreeSelectionListener,
               ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueDragTree.class);
    
    public static ResourceNode oldnode;
    private ResourceSelection resourceSelection = null;
    private static final ImageIcon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
    private static final ImageIcon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
    private static final ImageIcon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;
    private static final int DOUBLE_CLICK = 2;
    ///private javax.swing.JPanel previewPanel = null;
    //	private tufts.vue.gui.DockWindow previewDockWindow = null;

    private static final boolean SlowStartup = VueUtil.isMacPlatform() && !DEBUG.Enabled;
    
    public VueDragTree(Object obj, String treeName) {
        //Util.printStackTrace("NEW: " + getClass() + "; " + treeName + "; " + obj);
        if (DEBUG.Enabled) Log.debug("NEW: " + treeName + "; " + obj);
        setModel(createTreeModel(obj, treeName));
        setName(treeName);
        setRootVisible(true);
        if (SlowStartup) {
            this.expandRow(0);
            this.expandRow(1);
            this.setRootVisible(false);
        }
        implementDrag(this);
        createPopupMenu();
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeSelectionListener(this);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me){
                if  (me.getClickCount() != DOUBLE_CLICK)
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

        if (DEBUG.SELECTION) Util.printStackTrace(GUI.namex(this) + " constructed from obj " + obj + " treeName " + treeName);
    }
    
    public VueDragTree(FavoritesNode favoritesNode) {

        //Util.printStackTrace("NEW: " + getClass() + "; " + favoritesNode + "; " + favoritesNode.getResource());
        if (DEBUG.Enabled) Log.debug("NEW: " + favoritesNode + "; " + favoritesNode.getResource());;

        setName(favoritesNode.toString());
        setModel(new DefaultTreeModel(favoritesNode));
        expandRow(0);
        createPopupMenu();
        implementDrag(this);
        addTreeSelectionListener(this);

        if (DEBUG.SELECTION) Util.printStackTrace(GUI.namex(this) + " constructed from FavoritesNode " + favoritesNode);
        
    }

    /*
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
                this.previewPanel.setPreferredSize(new java.awt.Dimension(100,100));
                this.previewDockWindow = previewDockWindow;
     
                this. getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                resourceSelection = VUE.getResourceSelection();
        addTreeSelectionListener(this);
    }
     */


    public void addNotify() {
        super.addNotify();
        if (resourceSelection == null) {
            // Only do this on addNotify, as workaround for buggy multiple
            // instances of data sources being spuriously generated by
            // the data source loader.
            resourceSelection = VUE.getResourceSelection();
            resourceSelection.addListener(this);
        }
        if (DEBUG.SELECTION) System.out.println(GUI.namex(this) + " addNotify");
    }
    public void removeNotify() {
        super.removeNotify();
        if (DEBUG.SELECTION) System.out.println(GUI.namex(this) + " removeNotify");
    }

    
    /** ResourceSelection.Listener */
    public void resourceSelectionChanged(tufts.vue.ResourceSelection.Event e) {
        if (e.source == this) {
            return;
        //if (getPicked() == e.selected) {
        //    ; // do nothing; already selected
        } else {
            // todo: if contains selected item, select it
            // TODO: clearing the selection isn't working! don't know
            // if is just a repaint issue.
            clearSelection();
            //setSelectionRow(-1);
            repaint();
        }
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
        ResourceNode root = new ResourceNode(Resource.getFactory().get(treeName));
        if (obj instanceof Iterator){
            Iterator i = (Iterator)obj;
            while (i.hasNext()){
                Object resource = i.next();
                if (DEBUG.DR) Log.debug("\tchild: " + resource);
                if (resource instanceof CabinetResource) {
                    CabinetResource cabRes = (CabinetResource) resource;
                    CabinetEntry entry = cabRes.getEntry();
                    CabinetNode cabNode = null;
                    if (entry instanceof RemoteCabinetEntry)
                        cabNode = new CabinetNode(cabRes, CabinetNode.REMOTE);
                    else
                        cabNode = new CabinetNode(cabRes, CabinetNode.LOCAL);
                    root.add(cabNode);
                    if (SlowStartup) {
                        // Do NOT DO THIS AUTOMATICALLY -- it can dramaticaly slow startup times.
                        // by tens of seconds (!) -- SMF 2007-10-10
                        if ((new File(cabRes.getSpec())).isDirectory())
                            cabNode.explore();
                        else if (cabNode.getCabinet() != null)
                            cabNode.explore();
                    }

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
                GUI.startRecognizedDrag(e, resource, this);
            }
            
//             if (resource != null) {
//                 Image imageIcon = nleafIcon.getImage();
//                 if (resource.getClientType() == Resource.DIRECTORY) {
//                     imageIcon = activeIcon.getImage();
//                 } else if (oldnode instanceof CabinetNode) {
//                     CabinetNode cn = (CabinetNode) oldnode;
//                     if (!cn.isLeaf())
//                         imageIcon = activeIcon.getImage();
//                 }
//                 e.startDrag(DragSource.DefaultCopyDrop, // cursor
//                         imageIcon, // drag image
//                         new Point(-10,-10), // drag image offset
//                         new tufts.vue.gui.GUI.ResourceTransfer(resource),
//                         //new VueDragTreeNodeSelection(resource), // transferable
//                         this);  // drag source listener
//             }
            
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
        if (DEBUG.DND) System.out.println("VueDragTree: dropActionChanged  to  " + tufts.vue.gui.GUI.dropName(e.getDropAction()));
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
            if (e.isAddedPath() && e.getPath().getLastPathComponent() != null ) {
                Resource resource = (Resource)((ResourceNode)e.getPath().getLastPathComponent()).getResource();
                resourceSelection.setTo(resource, this);
            }
        } catch(Exception ex) {
            // VueUtil.alert(null,ex.toString(),"Error in VueDragTree Selection");
            System.out.println("VueDragTree.valueChanged "+ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    class VueDragTreeCellRenderer extends DefaultTreeCellRenderer{
        String meta = "";
        protected VueDragTree tree;
        public VueDragTreeCellRenderer(VueDragTree vdTree) {
            this.tree = vdTree;
            
            
        }
        /* -----------------------------------  */
        
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);

            //if (VueUtil.isMacPlatform()) {
            if (true) {
                if (leaf && value instanceof ResourceNode) {
                    Resource r = ((ResourceNode)value).getResource();
                    Icon icon = r.getTinyIcon();
                    if (icon != null)
                        setIcon(icon);
//                     // TODO: standardize on URLResource / a to-be abstract Resource class,
//                     // and cache the damn ImageIcon...  and TODO: if the image we
//                     // get back is bigger than 16px, force a scale down (maybe in the
//                     // GUI method call)
//                     Image image = GUI.getSystemIconForExtension(r.getExtension(), 16);
//                     //if (image != null && image.getWidth(null) <= 16)
//                     if (image != null) {
//                         if (image.getWidth(null) > 16)
//                             image = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH); // TODO: CACHE IN RESOURCE
//                         setIcon(new javax.swing.ImageIcon(image)); // create imageicon that can force-scale down size
//                     }
                }
                return this;
            }
            
            
            
            if (value instanceof FavoritesNode) {
                
                if ( ((FavoritesNode)value).getChildCount() >0 ) {
                    setIcon(activeIcon);
                } else {
                    setIcon(inactiveIcon);
                }
            } else if (leaf) {
                                /*
                                 If we are dealing with an Asset, we can see if it has a preview
                                 */
                if (value instanceof ResourceNode) {
                    //Icon i = ((ResourceNode)value).getResource().getIcon();
                    Icon i = null;
                    if (i == null) {
                        setIcon(nleafIcon);
                    } else {
                        setIcon(i);
                    }
                } else {
                    setIcon(nleafIcon);
                }
            } else {
                                /*
                                 If we are dealing with an Asset, we can see if it has a preview
                                 */
                if (value instanceof ResourceNode) {
                    //Icon i = ((ResourceNode)value).getResource().getIcon();
                    Icon i = null;
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
    protected final Resource resource;
    public ResourceNode(Resource resource) {
        
        this.resource = resource;
        setUserObject(resource);
    }

    protected ResourceNode() {
        resource = null;
    }
    
    public Resource getResource() {
        return resource;
    }
    
    public String toString() {
        String title = resource.getTitle();
        if (title == null || title.length() == 0)
            return resource.getSpec();
        else
            return title;
    }
}


class CabinetNode extends ResourceNode {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CabinetNode.class);
    
    //DefaultTreeModel dataModel;
    public static final String LOCAL = "local";
    public static final String REMOTE = "remote";
    private String type = "unknown";
    private boolean explored = false;
    
    public CabinetNode(Resource resource, String type) {
        super(resource);
        this.type = type;
    }
    
    public static CabinetNode  getCabinetNode(String title, File file, ResourceNode rootNode, DefaultTreeModel model){
        CabinetNode node= null;
        try{
            //LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
            osid.shared.Agent agent = null;
            LocalCabinetEntry cab;
            if(file.isDirectory()){
                cab = LocalCabinet.instance(file.getAbsolutePath(),agent,null);
            } else {
                // todo: use factory, also -- should this be a bytestore?
                cab = new LocalCabinetEntry(file.getAbsolutePath(),agent,null);
                Log.debug("new " + cab);
            }
            CabinetResource res = CabinetResource.create(cab);
            CabinetEntry entry = res.getEntry();

            //if (title != null) res.setTitle(title);

            if (entry instanceof RemoteCabinetEntry)
                node =  new CabinetNode(res, CabinetNode.REMOTE);
            else
                node = new CabinetNode(res, CabinetNode.LOCAL);
            model.insertNodeInto(node, rootNode, (rootNode.getChildCount()));
            //node.explore();
        } catch(Exception ex){
            System.out.println("CabinetNode: "+ex);
            ex.printStackTrace();
        }
        return node;
    }
    /**
     *  Return true if this node is a leaf.
     */
    public boolean isLeaf() {
        boolean flag = true;
        CabinetResource res = (CabinetResource) getUserObject();
        if(res != null && res.getEntry() != null){
           // System.out.println("CabinetNode.isLeaf: type-"+this.type);
            // TODO: if we really want CabinetNode to do lazy setSpec, don't to this getSpec here,
            // is it completely defeats the purpose...
            // TODO: this is a rediculously slow way to go about figuring this flag, which
            // we should already know...
            if((new File(res.getSpec()).isDirectory())) {
                flag = false;
            } else if(this.type.equals(CabinetNode.REMOTE) && ((RemoteCabinetEntry)res.getEntry()).isCabinet())
                flag = false;
            else if(this.type.equals(CabinetNode.LOCAL) && ((LocalCabinetEntry)res.getEntry()).isCabinet()) {
                flag = false;
            }
        }
        return flag;
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
        if(getCabinet() != null) {
            try {
                if (this.type.equals(CabinetNode.REMOTE)) {
                    CabinetEntryIterator i = (RemoteCabinetEntryIterator) getCabinet().entries();
                    while (i.hasNext()) {
                        CabinetEntry ce = (RemoteCabinetEntry) i.next();
                        if (ce.getDisplayName().startsWith(".")) // don't display dot files
                            continue;
                        CabinetResource res = CabinetResource.create(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
                        this.add(rootNode);
                    }
                } else if (this.type.equals(CabinetNode.LOCAL)) {
                    CabinetEntryIterator i = (LocalCabinetEntryIterator) getCabinet().entries();
                    
                    while (i.hasNext()) {
                        CabinetEntry ce = (LocalCabinetEntry) i.next();
                        if (ce.getDisplayName().startsWith(".")) // don't display dot files
                            continue;
                        CabinetResource res = CabinetResource.create(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
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
        } else return;
        
        
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
        
        // Code w/no apparent effect commented out -- SMF 2007-10-05
        //try{
        //    MapResource resource = new  MapResource(file.toURL().toString());
        //}catch (Exception ex){};
        
    }
    public boolean getAllowsChildren() { return isDirectory(); }
    public boolean isLeaf() 	{ return !isDirectory(); }
    public File getFile()		{ return (File)getUserObject(); }
    public boolean isExplored() { return explored; }
    public boolean isDirectory() {
        File file = getFile();
        if (file != null) {
            return file.isDirectory();
        } else {
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
    private boolean explored = false;
    public FavoritesNode(Resource resource){
        super(resource);
        // ensure is marked as favorites (some versions of VUE may have left marked as type NONE)
        resource.setClientType(Resource.FAVORITES);
    }
    public boolean isExplored() { return explored; }
    /*
    public boolean isLeaf() {
        File file = new File(resource.getSpec());
        System.out.println("Resource: "+resource.getTitle()+ " Type:"+resource.getType()+" leaf?"+file.isDirectory());
        if (file.isDirectory()) {
            //System.out.println("Resource: "+resource.getSpec()+ " Type:"+resource.getType());
            return false;
        }
        return true;
    }
    public void explore() {
        File file = new File(resource.getSpec());
        if(!file.isDirectory())
            return;
        if(!isExplored()) {
            File[] contents = file.listFiles();
            for(int i=0; i < contents.length; ++i)
                add(new FileNode(contents[i]));
            explored = true;
        }
    }
     */
}