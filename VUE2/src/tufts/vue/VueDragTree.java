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
    
   
    
    public VueDragTree(Object obj, String treeName) {
       
        //create the treemodel
        
        
        
        setModel(createTreeModel(obj, treeName));
        
        //Implement Drag
        
        
        DragSource dragSource = DragSource.getDefaultDragSource();

		dragSource.createDefaultDragGestureRecognizer(
					this, // component where drag originates
					DnDConstants.ACTION_COPY_OR_MOVE, // actions
					this); // drag gesture recognizer

		addTreeExpansionListener(new TreeExpansionListener(){
			public void treeCollapsed(TreeExpansionEvent e) {
			}
			public void treeExpanded(TreeExpansionEvent e) {
				TreePath path = e.getPath();
                                if(path != null) {
					FileNode node = (FileNode)
								   path.getLastPathComponent();

					if( !node.isExplored()) {
						DefaultTreeModel model = 
									(DefaultTreeModel)getModel();
						node.explore();
						model.nodeStructureChanged(node);
					}
				}
			}
		});
         
                VueDragTreeCellRenderer renderer = new VueDragTreeCellRenderer(this);
               
         
              
                  this.setCellRenderer(renderer);
                  
                ToolTipManager.sharedInstance().registerComponent(this);
        
       
        
    }
    
    
    //Creating treemodel
    
    private DefaultTreeModel createTreeModel(Object obj, String treeName){
        
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(treeName); 
          
        
   
        if (obj instanceof AssetIterator){
            AssetIterator i = (AssetIterator)obj;
            try{
                while (i.hasNext())
                            root.add(new AssetNode(i.next()));
            }catch (Exception e){System.out.println("VueDragTree.createTreeModel"+e);}
                      
        } else{
            Iterator i = (Iterator)obj;
             while (i.hasNext()){
                Object resource = i.next(); 
                if (resource instanceof File)
                {
                             
                    FileNode rootNode = new FileNode((File)resource);
                    root.add(rootNode);
                    rootNode.explore();
                }
                else 
                    root.add(new DefaultMutableTreeNode(resource));
            }
        }
                  
     return new DefaultTreeModel(root);
       
    }
   
    //****************************************
    
   public void dragGestureRecognized(DragGestureEvent e)
    {
        // drag anything ...
        
        Object resource = getObject();
        
        if (resource != null) {
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
			new VueDragTreeNodeSelection(resource), // transferable
			this);  // drag source listener
        }
    }
    public void dragDropEnd(DragSourceDropEvent e) {}
    public void dragEnter(DragSourceDragEvent e) {}
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
                //JPopupMenu popUp;
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                ResultNode Node;
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
  class AssetNode extends DefaultMutableTreeNode {
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