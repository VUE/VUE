package tufts.vue;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.io.*;
import java.util.Vector;
import javax.swing.filechooser.FileSystemView;

class DragTree extends JTree implements DragGestureListener,
		DragSourceListener{
	public DragTree() {
		DragSource dragSource = DragSource.getDefaultDragSource();

		dragSource.createDefaultDragGestureRecognizer(
					this, // component where drag originates
					DnDConstants.ACTION_COPY_OR_MOVE, // actions
					this); // drag gesture recognizer

		setModel(createTreeModel());

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
                DragTreeCellRenderer renderer = new DragTreeCellRenderer(this);
               
         
              
                  this.setCellRenderer(renderer);
                  
                ToolTipManager.sharedInstance().registerComponent(this);
        
              
                
	}
	public void dragGestureRecognized(DragGestureEvent e) {
		// drag anything ...
		e.startDrag(DragSource.DefaultCopyDrop, // cursor
			new FileSelection(getFile()), // transferable
			this);  // drag source listener
	}
	public void dragDropEnd(DragSourceDropEvent e) {}
	public void dragEnter(DragSourceDragEvent e) {}
	public void dragExit(DragSourceEvent e) {}
	public void dragOver(DragSourceDragEvent e) {}
	public void dropActionChanged(DragSourceDragEvent e) {}

        
	public File getFile() {
		TreePath path = getLeadSelectionPath();
		FileNode node = (FileNode)path.getLastPathComponent();
	
                return ((File)node.getUserObject());
	}
        
	private DefaultTreeModel createTreeModel() {
	DefaultMutableTreeNode baseroot = new DefaultMutableTreeNode("Mounted Devices");
           
               File[] roots = File.listRoots();        
        for(int j=0;j<roots.length;j++)        
                 {            
               String  mountedDevice = roots[j].getAbsolutePath();        
               if (mountedDevice == "A:\\") {
                   DefaultMutableTreeNode floppyRoot = new DefaultMutableTreeNode(mountedDevice);
                           baseroot.add(floppyRoot);
               }
               else
               {
                File root = new File(mountedDevice);
	         FileNode rootNode = new FileNode(root); 
                baseroot.add(rootNode); 
                rootNode.explore();
                 }
        }
	return new DefaultTreeModel(baseroot);
                       
             
        }
}
class FileSelection extends Vector implements Transferable
    {
        final static int FILE = 0;
        final static int STRING = 1;
        final static int PLAIN = 2;
        DataFlavor flavors[] = {DataFlavor.javaFileListFlavor,
                                DataFlavor.stringFlavor,
                                DataFlavor.plainTextFlavor};
        public FileSelection(File file)
        {
            addElement(file);
        }
        /* Returns the array of flavors in which it can provide the data. */
        public synchronized DataFlavor[] getTransferDataFlavors() {
    	return flavors;
        }
        /* Returns whether the requested flavor is supported by this object. */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            boolean b  = false;
            b |=flavor.equals(flavors[FILE]);
            b |= flavor.equals(flavors[STRING]);
           // b |= flavor.equals(flavors[PLAIN]);
        	return (b);
        }
        /**
         * If the data was requested in the "java.lang.String" flavor,
         * return the String representing the selection.
         */
        public synchronized Object getTransferData(DataFlavor flavor)
    			throws UnsupportedFlavorException, IOException {
    	if (flavor.equals(flavors[FILE])) {
    	    return this;
    	} else if (flavor.equals(flavors[PLAIN])) {
    	    return new StringReader(((File)elementAt(0)).getAbsolutePath());
    	} else if (flavor.equals(flavors[STRING])) {
    	    return((File)elementAt(0)).getAbsolutePath();
    	} else {
    	    throw new UnsupportedFlavorException(flavor);
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
 
class DragTreeCellRenderer extends DefaultTreeCellRenderer {
    protected JTree tree;
    protected Object lastNode;
    private String metaData;
    
    public DragTreeCellRenderer(JTree pTree) {
        this.tree = pTree;
        metaData = "deafult metadata";
        tree.addMouseMotionListener(new MouseMotionAdapter() {
                //JPopupMenu popUp;
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                Object obj;
                if (treePath!=null) {
                    obj = treePath.getLastPathComponent();
                         metaData =  "Filesystem Metadata Path" + treePath.toString(); 
                
        
        
                tree.repaint();
                // System.out.println("Tree Path Not Null");
                } else {
                    obj = null;
       
                }
                if (obj!=lastNode) {
                    lastNode = obj;
              
                   // System.out.println("Last Node");
                }
                
                
            }
            
         
            
        });

    }

  
    public String getToolTipText(){
         return metaData;
        
    }
}
