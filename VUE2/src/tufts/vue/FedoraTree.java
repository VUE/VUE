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

import osid.dr.*;
import osid.OsidException;
class FedoraTree extends JTree implements DragGestureListener,DragSourceListener{
	public FedoraTree(DigitalRepository dr) {
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(
					this, // component where drag originates
					DnDConstants.ACTION_COPY_OR_MOVE, // actions
					this); // drag gesture recognizer
	       setModel(createTreeModel(dr));
               FedoraTreeCellRenderer renderer = new FedoraTreeCellRenderer(this);
               this.setCellRenderer(renderer);   
               ToolTipManager.sharedInstance().registerComponent(this);
        
	}
        
    public void dragGestureRecognized(DragGestureEvent e)
    {
        // drag anything ...
        Asset asset  = getAsset();
        if(asset != null)
            e.startDrag(DragSource.DefaultCopyDrop,new FedoraSelection(asset),this);
    }
    public void dragDropEnd(DragSourceDropEvent e) {}
    public void dragEnter(DragSourceDragEvent e) {}
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {}

    private Asset getAsset() {
        TreePath path = getLeadSelectionPath();
        if (path == null)
            return null;
        FedoraNode node = (FedoraNode)path.getLastPathComponent();
        return ((Asset)node.getUserObject());
    }
   
    private DefaultTreeModel createTreeModel(DigitalRepository dr) {
	DefaultMutableTreeNode baseroot = new DefaultMutableTreeNode("Fedora@Tufts");
        AssetIterator ai;
        try {
            ai = dr.getAssets();
            while(ai.hasNext()) {
                baseroot.add(new FedoraNode((Asset)ai.next()));
            }
        } catch(Exception e) { System.out.println("FedoraTree.createTreeModel() "+e);}
        //File[] roots = File.listRoots();        
	return new DefaultTreeModel(baseroot);
                       
             
        }
}
class FedoraSelection extends Vector implements Transferable{
      //  final static int FILE = 0;
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
        public FedoraSelection(Asset asset)
        {
            addElement(asset);
            try {
             assetFlavor = new DataFlavor(Class.forName("osid.dr.Asset"),"asset");
         //    assetFlavor = new DataFlavor("asset","asset");
                displayName = ((Asset)elementAt(0)).getDisplayName();
            } catch (Exception e) { System.out.println("FedoraSelection "+e);}

            if(assetFlavor != null) {
                flavors[ASSET] = assetFlavor;
            }
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
    	    return displayName;
    	} else if (flavor.equals(flavors[PLAIN])) {
    	    return new StringReader(displayName);
    	} else if (flavor.equals(flavors[ASSET])) {
    	    return this;
    	} else {
    	    throw new UnsupportedFlavorException(flavor);
    	}
        }
    }

class FedoraNode extends DefaultMutableTreeNode {
	private boolean explored = false;
        private Asset asset;
	public FedoraNode(Asset asset) 	{ 
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
 
class FedoraTreeCellRenderer extends DefaultTreeCellRenderer {
    protected JTree tree;
    protected Object lastNode;
    private String metaData;
    
    public FedoraTreeCellRenderer(JTree pTree) {
        this.tree = pTree;
        metaData = "default metadata";
        tree.addMouseMotionListener(new MouseMotionAdapter() {
                //JPopupMenu popUp;
            public void mouseMoved(MouseEvent me) {
                TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                Object obj;
                if (treePath!=null) {
                    obj = treePath.getLastPathComponent();
                     metaData = treePath.toString(); 
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
