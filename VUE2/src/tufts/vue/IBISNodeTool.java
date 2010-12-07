package tufts.vue;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
//HO 23/06/2010 BEGIN *********************
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import tufts.Util;
//HO 23/06/2010 END *********************
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.NodeTool.SubTool;
import tufts.vue.NodeTool.SubTool.ShapeIcon;
import tufts.vue.VueTool.ToolIcon;
import tufts.vue.ibisimage.IBISImage;

public class IBISNodeTool extends VueTool
	implements VueConstants {
	
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
    
        VueToolUtils.setToolProperties(this,"IBISNodeTool");	
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
    
    public void setSelectedSubTool(VueTool tool) {
        super.setSelectedSubTool(tool);
        if (VUE.getSelection().size() > 0) {
            IBISSubTool imageTool = (IBISSubTool) tool;
            imageTool.getImageSetterAction().fire(this);
        }
    }
    
    public JPanel getContextualPanel() {
        return getIBISNodeToolPanel();
    }
    
    public static IBISNodeTool.IBISSubTool getActiveSubTool()
    {
        return (IBISSubTool) getTool().getSelectedSubTool();
    }
    
    @Override
    public Class getSelectionType() { return LWIBISNode.class; }
    
    public boolean supportsSelection() { return true; }
    
    private ImageIcon currentIcon;
    
    // todo: if we had a DrawContext here instead of just the graphics,
    // we could query it for zoom factor (passed in from mapViewer) so the stroke width would
    // look right.
    public void drawSelector(java.awt.Graphics2D g, ImageIcon i)
    {
        //g.drawImage(i.getImage(), new AffineTransform(), new Component());
    	// HO 03/11/2010 BEGIN **** - why bother with this -
    	// add the icon to the JLabel and that's it, then add
    	// the JLabel to the panel...
        currentIcon = getActiveSubTool().getImageIcon();
        //currentIcon.setFrame(i);
        //g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setColor(COLOR_SELECTION);
        //g.draw(currentShape);
    }
    
	
    /** @return an array of actions, with icon set, that will set the shape of selected
     * LWIBISNodes */
    public Action[] getIconSetterActions() {
        Action[] actions = new Action[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            IBISNodeTool.IBISSubTool ist = (IBISNodeTool.IBISSubTool) getSubTool(id);
            actions[i++] = ist.getImageSetterAction();
        }
        return actions;
    }

    /** @return an array of standard supported IBIS icons for nodes */
    public Object[] getAllIconValues() {
        Object[] values = new Object[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            IBISNodeTool.IBISSubTool ibnt = (IBISNodeTool.IBISSubTool) getSubTool(id);
            values[i++] = ibnt.getImageIcon();
        }
        return values;
    }
    
    public static class IBISNodeModeTool extends VueTool
    {
    	// HO 10/11/2010 BEGIN ***************
    	private LWIBISNode creationNode = new LWIBISNode("", new tufts.vue.ibisimage.IBISAcceptedIssueImage());

    	// HO 10/11/2010 END ***************

    	public IBISNodeModeTool()
    	{
            super();

            creationNode.setAutoSized(false);
            setActiveWhileDownKeyCode(KeyEvent.VK_X);   
          //HO 23/06/2010 BEGIN *********************
            /* File fileName = new File("/Users/helenoliver/Documents/chii_whistling.tiff");
            if ((fileName != null) && (fileName.exists())) {
            	creationNode.setResource(fileName);
            }*/ 
            	
            
          //HO 23/06/2010 END *********************
    	}
    	
    	@Override
        public Class getSelectionType() { return LWIBISNode.class; }
    	
        @Override
        public boolean handleMousePressed(MapMouseEvent e) {
            EditorManager.applyCurrentProperties(creationNode);            
            return false;
        }
        
        @Override
    	public boolean handleSelectorRelease(MapMouseEvent e)
        {
            final LWIBISNode node = (LWIBISNode) creationNode.duplicate();
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
        
        @Override
        public void drawSelector(DrawContext dc, java.awt.Rectangle r)
        {
            dc.g.draw(r);
            
            // TODO: doesn't handle zoom
            // Also, handleSelectorRelease can now just dupe the creationNode
            
            creationNode.setFrame(r);
            creationNode.draw(dc);
        }
    	
        /** @return a new node with the default VUE new-node label, initialized with a style from the current editor property states */
        public static LWIBISNode createNewNode() {
            return createNewNode(VueResources.getString("newibisnode.html"));
        }
        
        /** @return a new node with the given label, initialized with a style from the current editor property states */
        public static LWIBISNode createNewNode(String label) {
            LWIBISNode node = createDefaultNode(label);
            EditorManager.targetAndApplyCurrentProperties(node);
            return node;
        }
        
        /** @return a new node initialized to internal VUE defaults -- ignore tool states */
        public static LWIBISNode createDefaultNode(String label) {
            return new LWIBISNode(label);
        }
        
        /** @return a "text" node initialized to the current style in the VUE editors.
        [old: Will adjust text size for current zoom level]
    */
    public static LWText createRichTextNode(String text)
    {
        LWText node = buildRichTextNode(text);
        node.getRichLabelBox(true).overrideTextColor(FontEditorPanel.mTextColorButton.getColor());
        node.setAutoSized(false);
        node.setSize(150,5);        
        return node;
    }
        
        /** @return a "text" node initialized to the current style in the VUE editors.
        [old: Will adjust text size for current zoom level]
	    */
	    public static LWIBISNode createTextNode(String text)
	    {
	        LWIBISNode node = buildTextNode(text);
	        EditorManager.targetAndApplyCurrentProperties(node);
	            
	        return node;
	    }
        
        private static LWIBISNode initAsTextNode(LWIBISNode node)
        {
            if (node != null)
                node.setAsTextNode(true);
            return node;
        }
        
        public static LWIBISNode createDefaultTextNode(String text) {
            LWIBISNode node = new LWIBISNode();
            node.setLabel(text);
            initAsTextNode(node);
            return node;
        }
        
        // deprecate - use createDefaultTextNode 
        public static LWIBISNode buildTextNode(String text) {
            return createDefaultTextNode(text);
        }
        
        public static LWText buildRichTextNode(String s) {
            LWText text = new LWText();
            text.setLabel(s);            
            return text;
        }

    }

    // HO 28/10/2010 BEGIN ***********
    public Class<? extends ImageIcon>[] getAllIconClasses() {
        Class<? extends ImageIcon>[] classes = new Class[getSubToolIDs().size()];
        int i = 0;
        for (Object o : getAllIconValues())
            classes[i++] = ((ImageIcon)o).getClass();
        return classes;
    }
    // HO 28/10/2010 END ****************
    
    // HO 11/11/2010 BEGIN ***********
    public Class<? extends LWImage>[] getAllImageClasses() {
        Class<? extends LWImage>[] classes = new Class[getSubToolIDs().size()];
        int i = 0;
        for (Object o : getAllImageValues())
            classes[i++] = ((LWImage)o).getClass();
        return classes;
    }
    
    /** @return an array of standard supported IBIS images for nodes */
    public Object[] getAllImageValues() {
        Object[] values = new Object[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            IBISNodeTool.IBISSubTool ibnt = (IBISNodeTool.IBISSubTool) getSubTool(id);
            values[i++] = ibnt.getImage();
        }
        return values;
    }
    // HO 11/11/2010 END ****************
    
    public LWImage getNamedImage(String name)
    {
        if (mSubToolMap.isEmpty())
            throw new Error("uninitialized sub-tools");
        
        for (VueTool t : mSubToolMap.values()) {
            if (name.equalsIgnoreCase(t.getAttribute("cssName"))) {
                return ((IBISSubTool)t).getImage();
            }
        }
        return null;
    }    
    
    /**
     * VueTool class for each of the specific IBIS node images.  Knows how to generate
     * an action for image setting, and creates a dynamic image based on the node type.
     */
    public static class IBISSubTool extends VueSimpleTool
    {
        private Class iconClass = null;
        private Class imageClass = null;
        private ImageIcon cachedIcon = null;
        private LWImage cachedImage = null;
        private VueAction imageSetterAction = null;
            
        public IBISSubTool() {}
        
        public void setID(String pID) {
            super.setID(pID);
            setGeneratedIcons(new IBISNodeIcon(getIconInstance()));
        }
        
        /** @return an action, with icon set, that will set the shape of selected
         * LWIBISNodes to the current image for this SubTool */
        public VueAction getImageSetterAction() {
            if (imageSetterAction == null) {
                imageSetterAction = new Actions.LWCAction(getToolName(), new IBISNodeIcon(getIconInstance())) {
                	// stick the kitten image in there as a stub
                		// File fileName = new File("/Users/helenoliver/Documents/chii_whistling.tiff");
                		// void act(LWNode n) { n.setResource(fileName);}
                		void act(LWIBISNode n) { n.setImageInstance(getImageInstance()); }
                	
                    };
                //imageSetterAction.putValue("property.key", LWKey.Shape);
                imageSetterAction.putValue("property.value", getImage()); // this may be handy
                // key is from: MenuButton.ValueKey
            }
            return imageSetterAction;
        }
        
        public ImageIcon getIconInstance()
        {
            if (iconClass == null) {
                String iconClassName = getAttribute("iconClass");
                try {
                    this.iconClass = getClass().getClassLoader().loadClass(iconClassName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            ImageIcon imgIcon = null;
            try {
                	imgIcon = (ImageIcon) iconClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return imgIcon;
        }
        
        public LWImage getImageInstance()
        {
            if (imageClass == null) {
                String imageClassName = getAttribute("imageClass");
                try {
                    this.imageClass = getClass().getClassLoader().loadClass(imageClassName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            LWImage lwImg = null;
            try {
                lwImg = (LWImage) imageClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lwImg;
        }
        
        /** @return true if given shape is of same type as us */
        public boolean isIcon(ImageIcon icon) {
            return icon != null && getImageIcon().getClass().equals(icon.getClass());
        }

        public ImageIcon getImageIcon()
        {
            if (cachedIcon == null)
                cachedIcon = getIconInstance();
            return cachedIcon;
        }
        
        public LWImage getImage()
        {
            if (cachedImage == null)
                cachedImage = getImageInstance();
            return cachedImage;
        }
        
        static final int nearestEven(double d)
        {
            if (Math.floor(d) == d && d % 2 == 1) // if exact odd integer, just increment
                return (int) d+1;
            if (Math.floor(d) % 2 == 0)
                return (int) Math.floor(d);
            else
                return (int) Math.ceil(d);
        }
        static final int nearestOdd(double d)
        {
            if (Math.floor(d) == d && d % 2 == 0) // if exact even integer, just increment
                return (int) d+1;
            if (Math.floor(d) % 2 == 1)
                return (int) Math.floor(d);
            else
                return (int) Math.ceil(d);
        }



        private static int sWidth;
        private static int sHeight;

        static {

            // Select a width/height that will perfectly center within the parent button
            // icon.  If parent width is even, our width should be even, if odd, we
            // should be odd.  This is independent of the 50% size of the parent button
            // we're using as a baseline (before the pixel tweak).  This also means if
            // somebody goes to center us in the parent (ToolIcon), that computation
            // will always have an even integer result, thus perfectly pixel aligned.
            // NOTE: if you paint a 1 pix border on the shape, when anti-aliased this
            // generally adds a total of 1 pixel to the height & width.  If painting a
            // border on the dyanmic shape, you need to account for that for perfect
            // centering.
            
            if (ToolIcon.Width % 2 == 0)
                sWidth = nearestOdd(ToolIcon.Width / 2);
            else
                sWidth = nearestEven(ToolIcon.Width / 2);
            if (ToolIcon.Height % 2 == 0)
                sHeight = nearestOdd(ToolIcon.Height / 2);
            else
                sHeight = nearestEven(ToolIcon.Height / 2);
            
            sHeight--; // new priority: for even veritcal alignment in the combo-box -- SMF 2007-05-01
            
            /* sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColorLight, sWidth/2,sHeight/2,sShapeColor,true); */ // horizontal dark center
            //sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColor, sWidth/2,sHeight/2,sShapeColorLight,true); // horizontal light center
            //sShapeGradient = new GradientPaint(0,sHeight/2,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // vertical
            //sShapeGradient = new GradientPaint(0,0,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // diagonal
            
        }

        // HO 15/11/2010 BEGIN *************
        // test to see what happens
        // public static class IBISNodeIcon implements Icon
        public static class IBISNodeIcon extends ImageIcon
        // HO 15/11/2010 END *************
        {
            public int getIconWidth() { return sWidth; }
            public int getIconHeight() { return sHeight; }

            private ImageIcon mIcon;
            
            public IBISNodeIcon(ImageIcon pIcon)
            {
                mIcon = pIcon;
            }
            
            public IBISNodeIcon(IBISImage pImage)
            {
                mIcon = pImage.getIcon();
            }
            
            public void setIcon(ImageIcon pIcon) {
            	mIcon = pIcon;
            }
            
            public ImageIcon getIcon() {
            	return mIcon;
            }
            
            // HO 15/11/2010 BEGIN *************
            // if this gets called just call the ImageIcon's native routine
            public void paintIcon(Component c, Graphics g, int x, int y) {
            	super.paintIcon(c, g, x, y);
            } 
            // HO 15/11/2010 END *************

            public String toString() {
                return "IBISNodeIcon[" + sWidth + "x" + sHeight + " " + mIcon + "]";
            }
        }
       
    }
}
