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



import javax.swing.tree.*;
import java.util.Iterator;

/**
 *
 * @author  rsaigal
 */
public class VueDragTree extends JTree implements DragGestureListener,DragSourceListener,TreeSelectionListener {
    
    private DefaultMutableTreeNode oldnode;     
    private ResourceSelection resourceSelection = null;
                 
    public VueDragTree(Object obj, String treeName) { 
        if (obj instanceof FavoritesNode){
            setModel(new DefaultTreeModel((FavoritesNode)obj));
            //this.setShowsRootHandles(true);
            this.setRootVisible(false);
            this.expandRow(0);
        }
        else{
            setModel(createTreeModel(obj, treeName));
            this.expandRow(0);
            this.expandRow(1);
        }        
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
         
          VueDragTreeCellRenderer renderer = new VueDragTreeCellRenderer();
         tree.setCellRenderer(renderer);

     
        ToolTipManager.sharedInstance().registerComponent(tree);
    }
    
    
  
   
    private DefaultTreeModel createTreeModel(Object obj, String treeName ){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(treeName); 
        if (obj instanceof AssetIterator){
            AssetIterator i = (AssetIterator)obj;
            try{
                if(!i.hasNext()) {
                    root.add(new DefaultMutableTreeNode("No hits"));
                }else {
                    do {
                        root.add(new AssetNode(i.next()));
                    }while (i.hasNext());
                }   
            }catch (Exception e){System.out.println("VueDragTree.createTreeModel"+e);}
                      
        } else if(obj instanceof Iterator){
            Iterator i = (Iterator)obj;
             while (i.hasNext()){
                Object resource = i.next(); 
                System.out.println("Object = "+resource+" type ="+resource.getClass().getName());
                if(resource instanceof Resource) {
                  ResourceNode node = new ResourceNode((Resource)resource);
                  root.add(node);
                }else if(resource instanceof File) {
                             
                    FileNode rootNode = new FileNode((File)resource);
                    root.add(rootNode);
                    rootNode.explore();
                }else {
                    root.add(new DefaultMutableTreeNode(resource));
                 }
             }
            
            
        }
        return new DefaultTreeModel(root);  
    }
   
    //****************************************
    
   public void dragGestureRecognized(DragGestureEvent e)
    {
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
    public void dropActionChanged(DragSourceDragEvent e) {}  
    

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
             resourceSelection.add((Resource)e.getPath().getLastPathComponent());
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
/**
 private Resource createResource(Object object) throws osid.dr.DigitalRepositoryException,osid.OsidException {
     if(object instanceof String) {
         return new Resource((String)object);
     } else if(object instanceof osid.dr.Asset) {
         Asset asset = (Asset)object; 
         Resource resource = new Resource(asset.getDisplayName());
         resource.setAsset(asset);
         return resource;
     } else if(object instanceof FileNode) {
         FileNode fileNode = (FileNode) object;
         return new Resource(fileNode.getFile().getName());
     } else if(object instanceof AssetNode) {
         Asset asset = ((AssetNode)object).getAsset(); 
         Resource resource = new Resource(asset.getDisplayName());
         resource.setAsset(asset);
         return resource;
   
         
     }else {
         throw new RuntimeException(object.getClass()+" : Not Supported");
     }
 }
 **/
 
 //Cell Renderer
 
 //Cell Renderer
 
  public class VueDragTreeCellRenderer extends DefaultTreeCellRenderer{
   String meta = "";
           
   public VueDragTreeCellRenderer() {
       
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
                     
                            if (value instanceof FavoritesNode)
                      {
                          
                        if ( ((FavoritesNode)value).getChildCount() >0 )
                        {
                          setIcon(activeIcon);
                        }
                        else
                        {
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
        

 }
 
 

 
 /*
class VueDragTreeCellRenderer extends DefaultTreeCellRenderer {
    protected VueDragTree tree;
   // protected ResultNode lastNode;
    private String metaData;
    
    public VueDragTreeCellRenderer(VueDragTree pTree) {
        this.tree = pTree;
        metaData = "default metadata";
       

         
    }

  
    public String getToResource.javaolTipText(){
        
         return metaData;
        
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
                   
                    if (file != null)
                    {
                        return file.isDirectory();
                    }
                    else
                    {
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
        public FavoritesNode(String displayName){
            super(new MapResource(displayName));
            
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
         try {
                assetFlavor = new DataFlavor(Class.forName("osid.dr.Asset"),"asset");
            } catch (Exception e) { System.out.println("FedoraSelection "+e);}
            **/
        DataFlavor flavors[] = {DataFlavor.plainTextFlavor,
                                DataFlavor.stringFlavor,
                                DataFlavor.plainTextFlavor};

      public VueDragTreeNodeSelection(Object resource)
        {
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
            b |=flavor.equals(flavors[RESOURCE]);
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
       
            throw new UnsupportedFlavorException(flavors[STRING]);
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
