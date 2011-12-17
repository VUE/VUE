package tufts.vue;

import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class ObliqueStrategiesNodeTool extends NodeTool {
	
	private static ObliqueStrategiesNodeTool singleton = null;
	
	private java.awt.geom.RectangularShape currentShape;
	

	public static class ObliqueStrategiesNodeModeTool extends NodeModeTool {

	    	private LWObliqueNode creationNode = new LWObliqueNode("Oblique Strategies");

	    	public ObliqueStrategiesNodeModeTool()
	    	{
	            super();
	            creationNode.setAutoSized(false);
	            setActiveWhileDownKeyCode(KeyEvent.VK_X); 
	            //HO 23/06/2010 BEGIN *********************
	            /* File fileName = new File("/Users/helenoliver/Documents/chii_whistling.tiff");
	            if ((fileName != null) && (fileName.exists())) {
	            	creationNode.setResource(fileName);
	            } */
	            	
	            
	          //HO 23/06/2010 END *********************

	    	}

	    	
	        /** @return a new node with the default VUE new-node label, initialized with a style from the current editor property states */
	        public static LWObliqueNode createNewNode() {
	            return createNewNode(VueResources.getString("newobliquestrategiesnode.html"));
	        }
	        
	        public static LWObliqueNode[] createNewDeck() {
	        	ObliqueStrategiesDeck theDeck = new ObliqueStrategiesDeck();
	        	int decksize = theDeck.size();
	        	LWObliqueNode[] deck = new LWObliqueNode[decksize+1];
	        	deck[0] = createNewNode(VueResources.getString("newobliquestrategiesnode.html"));
	    		// create a node for each card in shuffle order
	        	List cardnums = theDeck.getCardnums();
	    		Enumeration nume = theDeck.keys();
	    		int i = 1;
	    		while(nume.hasMoreElements()) {
	    			int nextcardnum = (Integer) nume.nextElement();
	    			String strNextLabel = theDeck.get(nextcardnum).toString();
	    			deck[i] = createNewNode(strNextLabel);
	    			i++;			
	    		}
	    		
	    		return deck;
	        	
	        }
	        
	        
	        @Override
	    	public boolean handleSelectorRelease(MapMouseEvent e)
	        {
	            final LWObliqueNode node = (LWObliqueNode) creationNode.duplicate();
	            node.setAutoSized(false);
	            node.setFrame(e.getMapSelectorBox());
	            node.setLabel(drawCardFromNewDeck());
	            MapViewer viewer = e.getViewer();
	            viewer.getFocal().addChild(node);
	            VUE.getUndoManager().mark("Oblique Strategies");
	            VUE.getSelection().setTo(node);
	            viewer.activateLabelEdit(node);
	            return true;
	        }
	        
	        private String drawCardFromNewDeck() {
	            LWObliqueNode[] newDeck = createNewDeck();
	            
	            int rnd = 1 + (int)(Math.random() * ((100 - 1) + 1));
	            return newDeck[rnd].getLabel();
	        }
	        
	        /** @return a new node with the given label, initialized with a style from the current editor property states */
	        public static LWObliqueNode createNewNode(String label) {
	            LWObliqueNode node = createDefaultNode(label);
	            EditorManager.targetAndApplyCurrentProperties(node);
	            return node;
	        }
	        
	        /** @return a new node initialized to internal VUE defaults -- ignore tool states */
	        public static LWObliqueNode createDefaultNode(String label) {
	            return new LWObliqueNode(label);
	        }
	        
	        /** @return a "text" node initialized to the current style in the VUE editors.
            [old: Will adjust text size for current zoom level]
	        */
	        public static LWNode createTextNode(String text)
	        {
	            LWNode node = buildTextNode(text);
	            EditorManager.targetAndApplyCurrentProperties(node);
	            
	//             if (VUE.getActiveViewer() != null) {
	//                 // Okay, for now this completely overrides the font size from the text toolbar...
	//                 final Font font = node.getFont();
	//                 final float curZoom = (float) VUE.getActiveViewer().getZoomFactor();
	//                 final int minSize = LWNode.DEFAULT_TEXT_FONT.getSize();
	//                 if (curZoom * font.getSize() < minSize)
	//                     node.setFont(font.deriveFont(minSize / curZoom));
	//             }
	                
	            return node;
	        }
	        
	        private static LWObliqueNode initAsTextNode(LWObliqueNode node)
	        {
	            if (node != null)
	                node.setAsTextNode(true);
	            return node;
	        }
	
	        public static LWObliqueNode createDefaultTextNode(String text) {
	            LWObliqueNode node = new LWObliqueNode("Oblique Strategies");
	            node.setLabel(text);
	            initAsTextNode(node);
	            return node;
	        }
	        
	        // deprecate - use createDefaultTextNode 
	        public static LWObliqueNode buildTextNode(String text) {
	            return createDefaultTextNode(text);
	        }


	    }

}
