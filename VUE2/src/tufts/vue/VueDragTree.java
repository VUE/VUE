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



import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;
 
import javax.swing.tree.*;
import java.util.Iterator;


/**
 *
 * @author  rsaigal
 */
public class VueDragTree extends JTree implements DragGestureListener,
		DragSourceListener {
    
    private static final String searchURL = "http://googlesearch.tufts.edu/search?submit.y=5&site=tufts01&submit.x=11&output=xml_no_dtd&client=tufts01&q=";
    
    private static final String  XML_MAPPING = "tufts/google/google.xml";
    private static final String  NXML_MAPPING = "google.xml";
   
    
    private static int NResults = 10;
    
    private static String result ="";
    
    private static URL url;  
   
   
    
    public VueDragTree(String top,String query) {
       
        //create the treemodel
        
        setModel(createTreeModel(top,query));
        
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
                                /*

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
                                 */
			}
		});
         
                DragTreeCellRenderer renderer = new DragTreeCellRenderer(this);
               
         
              
                  this.setCellRenderer(renderer);
                  
                ToolTipManager.sharedInstance().registerComponent(this);
        
       
        
    }
    
    private DefaultTreeModel createTreeModel(String top, String query){
       
        DefaultMutableTreeNode  baseRoot = new DefaultMutableTreeNode(top);
        
       
         //Do Google Search and build the tree
         
         try {
           result = "";
           url = new URL(searchURL+query);
           InputStream input = url.openStream();
           int c;
           while((c=input.read())!= -1) {
               result = result + (char) c;
           }
           String filename = "google_result.xml";
           System.out.println(filename);
           FileWriter fileWriter = new FileWriter(filename);
           
           fileWriter.write(result);
           fileWriter.close();
         
          GSP gsp = loadGSP(filename);
          
           Iterator i = gsp.getRES().getResultList().iterator();
       
           while(i.hasNext()) {
               Result r = (Result)i.next();
               
               System.out.println(r.getTitle()+" "+r.getUrl());
               ResultNode resultnode = new ResultNode(r);
              
               baseRoot.add(resultnode);
               
                   
                   
           } 
        } catch (Exception e) {}
          
        return new DefaultTreeModel(baseRoot);
       
    }
    
   public void dragGestureRecognized(DragGestureEvent e)
    {
        // drag anything ...
        Result result = getResult();
        String resultUrl = result.getUrl();
        
        if (resultUrl != null) {
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
			new StringSelection(resultUrl), // transferable
			this);  // drag source listener
        }
    }
    public void dragDropEnd(DragSourceDropEvent e) {}
    public void dragEnter(DragSourceDragEvent e) {}
    public void dragExit(DragSourceEvent e) {}
    public void dragOver(DragSourceDragEvent e) {}
    public void dropActionChanged(DragSourceDragEvent e) {}  
    
  /*  
    class ResultSelection extends Vector implements Transferable
    {
        final static int FILE = 0;
        final static int STRING = 1;
       
        DataFlavor flavors[] = {DataFlavor.javaFileListFlavor,
                                DataFlavor.stringFlavor,
                               };
        public ResultSelection(String resultUrl)
        {
            addElement(resultUrl);
        }
        /* Returns the array of flavors in which it can provide the data. */
    /*   
    public synchronized DataFlavor[] getTransferDataFlavors() {
    	return flavors;
        }
        /* Returns whether the requested flavor is supported by this object. */
    /*  
    public boolean isDataFlavorSupported(DataFlavor flavor) {
            boolean b  = false;
            b |=flavor.equals(flavors[FILE]);
            b |= flavor.equals(flavors[STRING]);
           
        	return (b);
        }
        /**
         * If the data was requested in the "java.lang.String" flavor,
         * return the String representing the selection.
         */
        /*
        public synchronized Object getTransferData(DataFlavor flavor)
    			throws UnsupportedFlavorException, IOException {
    	if (flavor.equals(flavors[FILE])) {
    	    return this;
    	 	} 
        else if (flavor.equals(flavors[STRING])) {
    	    return this;
    	} else {
    	    throw new UnsupportedFlavorException(flavor);
    	}
        }
    }
    
    */
 public Result getResult() {
        TreePath path = getLeadSelectionPath();
        if (path == null)
            return null;
        ResultNode node = (ResultNode)path.getLastPathComponent();
        return ((Result)node.getUserObject());
    }
    
 
 
 
 
 //Cell Renderer
class DragTreeCellRenderer extends DefaultTreeCellRenderer {
    protected VueDragTree tree;
    protected ResultNode lastNode;
    private String metaData;
    
public DragTreeCellRenderer(VueDragTree pTree) {
        this.tree = pTree;
        metaData = "deafult metadata";
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

    }

  
    public String getToolTipText(){
         return metaData;
        
    }
} 
    //Part of the Marshaller ---------
    
     private static GSP loadGSP(String filename)
    {
       
        try {
            Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource(new FileReader(filename)));
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP[" + filename + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
   private static GSP loadGSP(URL url)
    {
       try {
         InputStream input = url.openStream();
         int c;
         while((c=input.read())!= -1) {
               result = result + (char) c;
         }
       
           Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource());
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP " + e);
            e.printStackTrace();
            return null;
        }
    }
    

    private static Unmarshaller unmarshaller = null;
    private static Unmarshaller getUnmarshaller()
    {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                mapping.loadMapping(NXML_MAPPING);
                unmarshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("getUnmarshaller: " + e);
            }
        }
        return unmarshaller;
    }
    
    
    
    
}
