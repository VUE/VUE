package tufts.vue;

import java.awt.event.*;
//HO 23/06/2010 BEGIN *********************
import java.io.File;
import tufts.Util;
//HO 23/06/2010 END *********************

public class IBISNodeTool extends NodeTool {
	
	private static IBISNodeTool singleton = null;
	
	/** the contextual tool panel **/
    private static IBISNodeToolPanel ibisNodeToolPanel;

    /** this constructed called via VueResources.properties init */
    public IBISNodeTool()
    {
        super();
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
    
        VueToolUtils.setToolProperties(this,"ibisNodeTool");	
    }
    
    /** return the singleton instance of this class */
    public static IBISNodeTool getTool()
    {
        if (singleton == null) {
            new IBISNodeTool();
        }
        return singleton;
    }
    
    private static final Object LOCK = new Object();
    // todo: promote a generic static to VueTool that handles the lock,
    // and calls subclass factory for creating tool panel
    static IBISNodeToolPanel getIBISNodeToolPanel()
    {
        if (DEBUG.Enabled) tufts.Util.printStackTrace("deprecated");
        synchronized (LOCK) {
            if (ibisNodeToolPanel == null) {
                ibisNodeToolPanel = new IBISNodeToolPanel();
            }
        }
        return ibisNodeToolPanel;
    }
    
    private java.awt.geom.RectangularShape currentShape;
    // todo: if we had a DrawContext here instead of just the graphics,
    // we could query it for zoom factor (passed in from mapViewer) so the stroke width would
    // look right.
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
    	g.draw(r);
        currentShape = getActiveSubTool().getShape();
        currentShape.setFrame(r);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(COLOR_SELECTION);
        g.draw(currentShape);
    }
    
    public static class IBISNodeModeTool extends NodeModeTool
    {
    	private LWNode creationNode = new LWNode("");
    	//private LWNode creationNode = new LWNode("New Node");

    	public IBISNodeModeTool()
    	{
            super();
            creationNode.setAutoSized(false);
            setActiveWhileDownKeyCode(KeyEvent.VK_X);   
          //HO 23/06/2010 BEGIN *********************
            File fileName = new File("/Users/helenoliver/Documents/chii_whistling.tiff");
            if ((fileName != null) && (fileName.exists())) {
            	creationNode.setResource(fileName);
            }
            	
            
          //HO 23/06/2010 END *********************
    	}
        
        @Override
    	public boolean handleSelectorRelease(MapMouseEvent e)
        {
            final LWNode node = (LWNode) creationNode.duplicate();
            node.setAutoSized(false);
            node.setFrame(e.getMapSelectorBox());
            node.setLabel(VueResources.getString("newibisnode.html"));
            MapViewer viewer = e.getViewer();
            viewer.getFocal().addChild(node);
            VUE.getUndoManager().mark("New IBIS Node");
            VUE.getSelection().setTo(node);
            viewer.activateLabelEdit(node);
            return true;
        }
    	
        /** @return a new node with the default VUE new-node label, initialized with a style from the current editor property states */
        public static LWNode createNewNode() {
            return createNewNode(VueResources.getString("newibisnode.html"));
        }
        
        private static LWNode initAsTextNode(LWNode node)
        {
            if (node != null)
                node.setAsTextNode(true);
            return node;
        }

    }

}
