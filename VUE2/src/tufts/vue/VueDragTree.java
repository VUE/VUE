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
public class VueDragTree extends JTree implements DragGestureListener,
		DragSourceListener {
    
      private DefaultMutableTreeNode oldnode;          
                 
    public VueDragTree(Object obj, String treeName) {
       
        //create the treemodel
       
        if (obj instanceof FavoritesNode){
         
          
               setModel(new DefaultTreeModel((FavoritesNode)obj));
                  this.setShowsRootHandles(true);
                   this.expandRow(0);
              
          
        }
        else{
          
             setModel(createTreeModel(obj, treeName));
              this.expandRow(0);
              this.expandRow(1);
       
        }
        
       
                              
        implementDrag(this);
        
      
    }   
  
     
    private void  implementDrag(VueDragTree Tree){
      
        DragSource dragSource = DragSource.getDefaultDragSource();

		dragSource.createDefaultDragGestureRecognizer(
					Tree, // component where drag originates
					DnDConstants.ACTION_COPY_OR_MOVE, // actions
					Tree); // drag gesture recognizer

                
 
		addTreeExpansionListener(new TreeExpansionListener(){
			public void treeCollapsed(TreeExpansionEvent e) {
			}
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
         
                VueDragTreeCellRenderer renderer = new VueDragTreeCellRenderer(Tree);
               
         
              
                  Tree.setCellRenderer(renderer);
                  
                ToolTipManager.sharedInstance().registerComponent(Tree);
        
       
        
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
                if (resource instanceof File)
                {
                             
                    FileNode rootNode = new FileNode((File)resource);
                    root.add(rootNode);
                    rootNode.explore();
                }
                else {
                    root.add(new DefaultMutableTreeNode(resource));
                 }
             }
            
            
        }else {}
       //todo we need a constructor with just tree name      
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
    

 
 
 
 //Cell Renderer
class VueDragTreeCellRenderer extends DefaultTreeCellRenderer {
    protected VueDragTree tree;
   // protected ResultNode lastNode;
    private String metaData;
    
public VueDragTreeCellRenderer(VueDragTree pTree) {
        this.tree = pTree;
        metaData = "default metadata";
        /*
        tree.addMouseMotionListener(new MouseMotionAdapter() {
                //JPopupMenu popUp
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                
                if (treePath!=null) {
                          
                 System.out.println("This is in my house");   
                }
                    
        /*
         tree.addMouseMotionListener(new MouseMotionAdapter() {
                //JPopupMenu popUp
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                
                if (treePath!=null) {
                          //obj = treePath.getLastPathComponent();
                          Node = (ResultNode)treePath.getLastPathComponent();
                          Result resultObj = Node.getResult();
                          String resultTitle = resultObj.getTitle();
                          metaData =  resultTitle; 
               
        
                tree.repaint();
               
                } else {
                    Node  = null;
       
                }
                if (Node!=lastNode) {
                    lastNode = Node;
              
                   // System.out.println("Last Node");
                }
                
                
            }
            
        
            
        });
         */

         
    }

  
    public String getToolTipText(){
        
         return metaData;
        
    }
 } 

 public class AssetNode extends DefaultMutableTreeNode {
	private boolean explored = false;
        private Asset asset;
	public AssetNode(Asset asset) 	{ 
            this.asset = asset;
            setUserObject(asset); 
	}
        public  Asset getAsset() {
            return this.asset;
        }
	public String toString(){
            String returnString = "Fedora Object";
            try {
                returnString = asset.getDisplayName();
            } catch (Exception e) { System.out.println("FedoraNode.toString() "+e);}
            return returnString;
	}
}
    
}

class FileNode extends DefaultMutableTreeNode {
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

  class FavoritesNode extends DefaultMutableTreeNode {
        public FavoritesNode(String displayName){
            super(displayName);
            
        }
        public void explore() {
                this.explore();
		
		}
}    
 
  class VueDragTreeNodeSelection extends Vector implements Transferable{
       final static int FILE = 0;
        final static int STRING = 1;
        final static int PLAIN = 2;
        final static int ASSET = 0;
        public static DataFlavor assetFlavor;
        String displayName = "";
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
            if (resource instanceof Asset){
            try {
             assetFlavor = new DataFlavor(Class.forName("osid.dr.Asset"),"asset");
         //    assetFlavor = new DataFlavor("asset","asset");
                displayName = ((Asset)elementAt(0)).getDisplayName();
            } catch (Exception e) { System.out.println("FedoraSelection "+e);}
            

            if(assetFlavor != null) {
                flavors[ASSET] = assetFlavor;
            }
            }else if(resource instanceof File){
                flavors[FILE] = DataFlavor.javaFileListFlavor;
                displayName = ((File)elementAt(0)).getName();
            }else
            displayName = elementAt(0).toString();
                
        }
        /* Returns the array of flavors in which it can provide the data. */
        public synchronized DataFlavor[] getTransferDataFlavors() {
    	return flavors;
        }
        /* Returns whether the requested flavor is supported by this object. */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            boolean b  = false;
            b |=flavor.equals(flavors[ASSET]);
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
            System.out.println("I am here"+this.elementAt(0));
    	    return this.elementAt(0);
    	} else if (flavor.equals(flavors[PLAIN])) {
             System.out.println("I am plain"+this.elementAt(0));
    	    return new StringReader(displayName);
    	} else if (flavor.equals(flavors[ASSET])) {
    	    return this;
        } else if (flavor.equals(flavors[FILE])){
            return this;
    	} else {
    	    throw new UnsupportedFlavorException(flavor);
    	}
        }
    }