package tufts.vue;

import tufts.vue.shape.*;

import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import tufts.vue.beans.VueBeanState;

public class NodeTool extends VueTool
    implements VueConstants
{
	///////////
	// Fields
	/////////////
	
    
    private static NodeTool singleton = null;
    
    /** the contextual tool panel **/
    private static NodeToolPanel sNodeToolPanel;
    
    
    public NodeTool()
    {
        super();
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
    }


    /** return the singleton instance of this class */
    public static NodeTool getTool()
    {
        if (singleton == null)
            throw new IllegalStateException("NodeTool.getTool: class not initialized by VUE");
        return singleton;
    }
    
    static NodeToolPanel getNodeToolPanel()
    {
        if (sNodeToolPanel == null)
            sNodeToolPanel = new NodeToolPanel();
        return sNodeToolPanel;
    }
    
    public JPanel getContextualPanel() {
        return getNodeToolPanel();
    }

    public boolean supportsSelection() { return true; }
    
    private java.awt.geom.RectangularShape currentShape = new tufts.vue.shape.RectangularPoly2D(4);
    // todo: if we had a DrawContext here instead of just the graphics,
    // we could query it for zoom factor (passed in from mapViewer) so the stroke width would
    // look right.
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);

        g.draw(r);
        currentShape = getActiveSubTool().getShape();
        currentShape.setFrame(r);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setColor(COLOR_NODE_DEFAULT);
        //g.fill(currentShape);
        //g.setColor(COLOR_BORDER);
        //g.setStroke(STROKE_ONE); // todo: scale based on the scale in the GC affine transform
        //g.setStroke(new BasicStroke(2f * (float) g.getTransform().getScaleX())); // GC not scaled while drawing selector...
        g.setColor(COLOR_SELECTION);
        g.draw(currentShape);
        /*
        if (VueUtil.isMacPlatform()) // Mac 1.4.1 handles XOR differently than PC
            g.draw(currentShape);
        else
            g.fill(currentShape);
        */
    }
    
    public boolean handleSelectorRelease(MapMouseEvent e)
    {
        LWNode node = createNode();
        node.setAutoSized(false);
        node.setFrame(e.getMapSelectorBox());
        e.getMap().addNode(node);
        VUE.getUndoManager().mark("New Node");
        VUE.getSelection().setTo(node);
        e.getViewer().activateLabelEdit(node);
        return true;
    }
    /*
    public void handleSelectorRelease(java.awt.geom.Rectangle2D mapRect)
    {
        LWNode node = createNode();
        node.setAutoSized(false);
        node.setFrame(mapRect);
        VUE.getActiveMap().addNode(node);
        VUE.getSelection().setTo(node);
        VUE.getActiveViewer().activateLabelEdit(node);
    }
    */

    public static LWNode createNode(String name)
    {
        LWNode node = new LWNode(name, getActiveSubTool().getShapeInstance());
        VueBeanState state = getNodeToolPanel().getValue();
        if (state != null)
            state.applyState( node);
        node.setAutoSized(true);
        return node;
    }
    public static LWNode createNode()
    {
        return createNode(null);
    }
    
    static LWNode initTextNode(LWNode node)
    {
        node.setIsTextNode( true);
        node.setAutoSized(true);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        node.setFillColor(COLOR_TRANSPARENT);
        node.setFont(LWNode.DEFAULT_TEXT_FONT);
        
        return node;
    }
    
    public static LWNode createTextNode(String text)
    {
        LWNode node = new LWNode();
        initTextNode(node);
        VueBeanState state = TextTool.getTextToolPanel().getValue();
        if (state != null)
            state.applyState( node);
        return node;
    }
    
    public static NodeTool.SubTool getActiveSubTool()
    {
        return (SubTool) getTool().getSelectedSubTool();
    }

    
    public Action[] getShapeSetterActions() {
        Action[] actions = new Action[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            NodeTool.SubTool nt = (NodeTool.SubTool) getSubTool(id);
            actions[i++] = nt.getShapeSetterAction();
        }
        return actions;
    }

    private static final Color ToolbarColor = VueResources.getColor("toolbar.background");
    
    public static class SubTool extends VueSimpleTool
    {
        private Class shapeClass = null;
        private RectangularShape cachedShape = null;
        private Action shapeSetterAction = null;
            
        public SubTool() {}

	public void setID(String pID) {
            super.setID(pID);
            //System.out.println(this + " ID set");
            //getShape(); // cache it for fast response first time
            setGeneratedIcons(new ShapeIcon());
        }
        
        public Action getShapeSetterAction() {
            if (shapeSetterAction == null) {
                shapeSetterAction = new Actions.LWCAction(getToolName(), new ShapeIcon()) {
                        void act(LWNode n) {
                            n.setShape(getShapeInstance());
                        }
                    };
            }
            return shapeSetterAction;
        }

        public RectangularShape getShapeInstance()
        {
            if (shapeClass == null) {
                String shapeClassName = getAttribute("shapeClass");
                //System.out.println(this + " got shapeClass " + shapeClassName);
                try {
                    this.shapeClass = getClass().getClassLoader().loadClass(shapeClassName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            RectangularShape rectShape = null;
            try {
                rectShape = (RectangularShape) shapeClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return rectShape;
        }
        
        public RectangularShape getShape()
        {
            if (cachedShape == null)
                cachedShape = getShapeInstance();
            return cachedShape;
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
        
        //private static final Color sShapeColor = new Color(165,178,208); // Melanie's steel blue
        private static final Color sShapeColor = new Color(93,98,162); // Melanie's icon blue/purple
        private static final Color sShapeColorLight = VueUtil.factorColor(sShapeColor, 1.3);
        //private static final boolean sPaintBorder = false;
        private static GradientPaint sShapeGradient;
        private static int sWidth;
        private static int sHeight;
        
        static {

            // Select a width/height that will perfectly center within
            // the parent button icon.  If parent width is even, our
            // width should be even, if odd, we should be odd.  This
            // is independent of the 50% size of the parent button
            // we're using as a baseline (before the pixel tweak).
            // This also means if somebody goes to center us in the
            // parent (ToolIcon), that computation will always have an
            // even integer result, thus perfectly pixel aligned.
            // NOTE: if you paint a 1 pix border on the shape, when
            // anti-aliased this generally adds a total of 1 pixel to
            // the height & width.  If painting a border on the
            // dyanmic shape, you need to account for that for perfect
            // centering.
            
            if (ToolIcon.width % 2 == 0)
                sWidth = nearestOdd(ToolIcon.width / 2);
            else
                sWidth = nearestEven(ToolIcon.width / 2);
            if (ToolIcon.height % 2 == 0)
                sHeight = nearestOdd(ToolIcon.height / 2);
            else
                sHeight = nearestEven(ToolIcon.height / 2);

            sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColorLight, sWidth/2,sHeight/2,sShapeColor,true); // horizontal dark center
            //sShapeGradient = new GradientPaint(sWidth/2,0,sShapeColor, sWidth/2,sHeight/2,sShapeColorLight,true); // horizontal light center
            //sShapeGradient = new GradientPaint(0,sHeight/2,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // vertical
            //sShapeGradient = new GradientPaint(0,0,sShapeColor.brighter(), sWidth/2,sHeight/2,sShapeColor,true); // diagonal
            
        }
        
        class ShapeIcon implements Icon
        {
            public int getIconWidth() { return sWidth; }
            public int getIconHeight() { return sHeight; }

            private RectangularShape mShape;
            ShapeIcon()
            {
                mShape = getShapeInstance();
                if (mShape instanceof RoundRectangle2D) {
                    // hack to deal with arcs being too small on a tiny icon
                    ((RoundRectangle2D)mShape).setRoundRect(0, 0, sWidth,sHeight, 8,8);
                } else
                    mShape.setFrame(0,0, sWidth,sHeight);
            }
            
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x,y);
                if (sShapeGradient != null)
                    g2.setPaint(sShapeGradient);
                else
                    g2.setColor(sShapeColor);
                g2.fill(mShape);
                g2.setColor(Color.black);
                //g2.setStroke(STROKE_EIGHTH);
                //g2.draw(mShape);
                g2.translate(-x,-y);
            }

            public String toString() {
                return "ShapeIcon[" + mShape + "]";
            }
        }

        
    }
}
