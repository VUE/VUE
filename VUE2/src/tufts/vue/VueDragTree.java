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
 * @author  rsaigal
 */
public class VueDragTree extends JTree implements DragGestureListener,DragSourceListener,TreeSelectionListener,ActionListener {
    
    private DefaultMutableTreeNode oldnode;
    private ResourceSelection resourceSelection = null;
    
    public VueDragTree(Object  obj, String treeName) {
        setModel(createTreeModel(obj, treeName));
        
        this.expandRow(0);
        this.expandRow(1);
        
        implementDrag(this);
        createPopupMenu();
        
        
        resourceSelection = VUE.sResourceSelection;
        addTreeSelectionListener(this);
    }
    
    public VueDragTree(FavoritesNode favoritesNode) {
        setModel(new DefaultTreeModel(favoritesNode));
        
        this.expandRow(0);
        createPopupMenu();
        
        implementDrag(this);
        resourceSelection = VUE.sResourceSelection;
        addTreeSelectionListener(this);
    }
    
    
    private void  implementDrag(VueDragTree tree){
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_COPY_OR_MOVE,tree);
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
                    cabNode.getDataModel().reload();
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
                    
                    CabinetEntry entry = ((CabinetResource)resource).getEntry();
                    CabinetNode cabNode = null;
                    if (entry instanceof RemoteCabinetEntry)
                        cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.REMOTE);
                    else
                        
                        cabNode = new CabinetNode((CabinetResource)resource, CabinetNode.LOCAL);
                    
                    root.add(cabNode);
                    cabNode.explore();
                }
                
                
                else{
                    
                    
                    ResourceNode node = new ResourceNode((Resource)resource);
                    root.add(node);
                }
            }
            
            
        }
        
        
        return new DefaultTreeModel(root);
    }
    
    //****************************************
    
    public void dragGestureRecognized(DragGestureEvent e) {
        // drag anything ...
        TreePath path = getLeadSelectionPath();
        oldnode = (DefaultMutableTreeNode)path.getLastPathComponent();
        Object resource = getObject();
        if (resource != null) {
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
            new VueDragTreeNodeSelection(resource), // transferable
            this);  // drag source listener
        }
    }
    
    public void dragDropEnd(DragSourceDropEvent e) {
        if (e.getDropAction() == DnDConstants.ACTION_MOVE){
            DefaultTreeModel model = (DefaultTreeModel)this.getModel();
            model.removeNodeFromParent(oldnode);
        }
    }
    
    public void dragEnter(DragSourceDragEvent e) { }
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {
        System.out.println(this + " dropActionChanged " + e);
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
                resourceSelection.add((Resource)((ResourceNode)e.getPath().getLastPathComponent()).getResource());
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
        boolean hasFocus) {
            
            //    meta =    ((Resource)value).getToolTipInformation();
            Icon leafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
            Icon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
            Icon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;
            
            
            super.getTreeCellRendererComponent(
            tree, value, sel,
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
            else if (leaf){ setIcon(leafIcon);}
            else { setIcon(activeIcon); }
            
            
            
            
            
            
            
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
        System.out.println("Was I in vuedragtree popup menu?");
        
        
        //Add listener to the text area so the popup menu can come up.
        MouseListener popupListener = new PopupListener(popup);
        this.addMouseListener(popupListener);
    }
    public void actionPerformed(ActionEvent e) {
        
        if (e.getSource() instanceof JMenuItem){
            
            
            JMenuItem source = (JMenuItem)(e.getSource());
            TreePath tp = this.getSelectionPath();
            
            System.out.println("This is treePath in here" + tp);;
            
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
        if (e.isPopupTrigger()) {
            
            System.out.println(" did trigger ever happen? resource listen haha");
            
            popup.show(e.getComponent(),
            e.getX(), e.getY());
            
        }
    }
}
}



/*---------------*/




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
    private Cabinet getCabinet() {
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
        //  If this is not a cabinet, then it cannot be expanded.
        if(getCabinet() != null) {
            try {
                if (this.type.equals(CabinetNode.REMOTE)) {
                    
                    CabinetEntryIterator i = (RemoteCabinetEntryIterator) getCabinet().entries();
                    
                    while (i.hasNext()) {
                        CabinetEntry ce = (RemoteCabinetEntry) i.next();
                        CabinetResource res = new CabinetResource(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
                        this.add(rootNode);
                    }
                }
                else if (this.type.equals(CabinetNode.LOCAL)) {
                    CabinetEntryIterator i = (LocalCabinetEntryIterator) getCabinet().entries();
                    
                    while (i.hasNext()) {
                        CabinetEntry ce = (LocalCabinetEntry) i.next();
                        //System.out.println ("CabinetNode explore: "+ce.getDisplayName());
                        CabinetResource res = new CabinetResource(ce);
                        CabinetNode rootNode = new CabinetNode(res, this.type);
                        this.add(rootNode);
                    }
                    this.explored = true;
                }
            } catch (FilingException e) {
                return;
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
    public void explore() {
        
        if(!isDirectory())
            return;
        
        if(!isExplored()) {
            File file = getFile();
            File[] children = file.listFiles();
            
            for(int i=0; i < children.length; ++i)
                add(new FileNode(children[i]));
            
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


class VueDragTreeNodeSelection extends Vector implements Transferable{
    final static int FILE = 0;
    final static int STRING = 1;
    final static int PLAIN = 2;
    final static int RESOURCE = 0;
    public static DataFlavor resourceFlavor;
    String displayName = "";
    
    static {
        try {
            resourceFlavor = new DataFlavor(Class.forName("tufts.vue.Resource"),"Resource");
            //    assetFlavor = new DataFlavor("asset","asset");
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    /**
     * try {
     * assetFlavor = new DataFlavor(Class.forName("osid.dr.Asset"),"asset");
     * } catch (Exception e) { System.out.println("FedoraSelection "+e);}
     **/
    DataFlavor flavors[] = {DataFlavor.plainTextFlavor,
    DataFlavor.stringFlavor,
    DataFlavor.plainTextFlavor};
    
    public VueDragTreeNodeSelection(Object resource) {
        addElement(resource);
        if (resource instanceof Resource){
            if (resourceFlavor != null) {
                flavors[RESOURCE] = resourceFlavor;
                try {
                    displayName = ((Resource)elementAt(0)).getTitle();
                    
                } catch (Exception e) { System.out.println("FedoraSelection "+e);}
            }
        } else if(resource instanceof File){
            flavors[FILE] = DataFlavor.javaFileListFlavor;
            displayName = ((File)elementAt(0)).getName();
        } else
            displayName = elementAt(0).toString();
    }
    
    /* Returns the array of flavors in which it can provide the data. */
    public synchronized java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }
    /* Returns whether the requested flavor is supported by this object. */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (flavor == null)
            return false;
        boolean b  = false;
        b |= flavor.equals(flavors[RESOURCE]);
        b |= flavor.equals(flavors[STRING]);
        // b |= flavor.equals(flavors[PLAIN]);
        return (b);
    }
    /**
     * If the data was requested in the "java.lang.String" flavor,
     * return the String representing the selection.
     */
    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        
        if (flavor.equals(flavors[STRING])) {
            //throw new UnsupportedFlavorException(flavors[STRING]);
            // Always support something for the string flavor, or
            // we get an exception thrown (even tho I think that
            // may be against the published API).
            return get(0).toString();
        } else if (flavor.equals(flavors[PLAIN])) {
            System.out.println("I am plain"+this.elementAt(0));
            return new StringReader(displayName);
        } else if (flavor.equals(flavors[RESOURCE])) {
            return this;
        } else if (flavor.equals(flavors[FILE])){
            return this;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
}



