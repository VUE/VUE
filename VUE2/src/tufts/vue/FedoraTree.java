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
import tufts.dr.fedora.*;
import java.net.*;

import osid.dr.*;
import osid.OsidException;
class FedoraTree extends JTree implements DragGestureListener,DragSourceListener{
	public FedoraTree(DigitalRepository dr) throws DigitalRepositoryException {
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
   
    private DefaultTreeModel createTreeModel(DigitalRepository dr)  throws DigitalRepositoryException{
	DefaultMutableTreeNode baseroot = new DefaultMutableTreeNode("Fedora@Tufts");
        AssetIterator ai;
        try {
            ai = dr.getAssets();
            while(ai.hasNext()) {
                baseroot.add(new FedoraNode((Asset)ai.next()));
            }
        } catch(DigitalRepositoryException e) { 
            
            //System.out.println("FedoraTree.createTreeModel() "+e);
            throw e;
        }
        //File[] roots = File.listRoots();        
	return new DefaultTreeModel(baseroot);
                       
             
        }
    
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        DigitalRepository dr = new DR();
        
        FedoraTree  fedoraTree = null;
        try {
            fedoraTree = new FedoraTree(dr);
        } catch(OsidException e) {
            JOptionPane.showMessageDialog(frame,"Cannot connect to FEDORA Server.","FEDORA Alert", JOptionPane.ERROR_MESSAGE);
          //  System.out.println(e);
        }
        if(fedoraTree != null) {
            JScrollPane jspFedora  = new JScrollPane(fedoraTree);
            frame.setContentPane(jspFedora);
        }
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });  
        frame.pack();
        frame.setVisible(true);
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
        
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
           
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
                JFrame frame = new AssetViewer(((FedoraNode)obj).getAsset());
                frame.setLocation(me.getX(),me.getY());
                frame.pack();
                frame.setVisible(true);
            }
        });
                
        
    }

  
    public String getToolTipText(){
         return metaData;
        
    }
}

class AssetViewer extends JFrame {
    static final String FEDORA_URL= "http://hosea.lib.tufts.edu:8080/fedora/get/";
    public AssetViewer(Asset asset) { 
        super("Asset Viewer");
        JTabbedPane assetPane= new JTabbedPane();
        getContentPane().setLayout(new BorderLayout());
        InfoRecordIterator i;
        try {
            i = asset.getInfoRecords();
            while(i.hasNext()) {
                  InfoRecord infoRecord = i.next();
                  InfoFieldIterator inf = infoRecord.getInfoFields();
                   JTabbedPane infoRecordPane = new JTabbedPane();
                  //infoRecordPane.setTabPlacement(JTabbedPane.LEFT);
                  while(inf.hasNext()) {
                      InfoField infoField = inf.next();
                      String method = asset.getId().getIdString()+"/"+infoRecord.getId().getIdString()+"/"+infoField.getValue().toString();
                      DisplayPane dPane  = new DisplayPane(FEDORA_URL+method);
                      infoRecordPane.addTab(infoField.getValue().toString(),dPane);
                  }

                  assetPane.addTab(infoRecord.getId().getIdString(),infoRecordPane);
            }
        } catch(Exception e) { System.out.println("MapViewer.getAssetMenu"+e);}
        getContentPane().setSize(600,600);
        getContentPane().add(assetPane,BorderLayout.CENTER);
    }
}
class DisplayPane extends JPanel{
    
    /** Creates a new instance of EditorPaneDemo */
    static JEditorPane editorPane;  
    static URL url;
    static  ImageIcon image;
    public DisplayPane(String location) {
      
        image = new ImageIcon();
        setLayout(new BorderLayout());
        try {
            url = new URL(location);
            URLConnection uConn = url.openConnection();
            System.out.println("Content-type: "+uConn.getContentType());
            if((uConn.getContentType().equals("text/html")) || (uConn.getContentType().equals("text/xml")) || uConn.getContentType().equals("text/html; charset=UTF-8")) {
                editorPane = new JEditorPane();
                editorPane.setEditable(false);
                editorPane.setPage(url);
                JScrollPane jSP = new JScrollPane(editorPane);
                add(jSP,BorderLayout.CENTER);
            } else if (uConn.getContentType().equals("image/jpeg") || uConn.getContentType().equals("image/gif")){   
                image = new ImageIcon(url);
                JButton imageButton = new JButton(image);
                imageButton.setBorderPainted(false);
                add(imageButton,BorderLayout.CENTER);
            } else {
                add(new JLabel("Not Implemented"),BorderLayout.CENTER);
            }
        } catch (Exception e) {
            System.err.println("Attempted to read a bad URL: " + url);
        }
       
    }

    
}
