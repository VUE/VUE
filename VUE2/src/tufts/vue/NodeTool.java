

package tufts.vue;

import tufts.vue.shape.*;

import java.lang.*;
import java.util.*;
import java.awt.*;
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
    
    private static NodeToolPanel getNodeToolPanel()
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
        VUE.ModelSelection.setTo(node);
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
        VUE.ModelSelection.setTo(node);
        VUE.getActiveViewer().activateLabelEdit(node);
    }
    */

    public static LWNode createNode(String name)
    {
        LWNode node = new LWNode(name, getActiveSubTool().getShapeInstance());
        VueBeanState state = getNodeToolPanel().getValue();
        if( state != null) {
        	state.applyState( node);
        	}
        
        node.setAutoSized(true);
        return node;
    }
    public static LWNode createNode()
    {
        return createNode(null);
    }
    public static LWNode createTextNode(String text)
    {
        LWNode node = createNode(text);
        node.setIsTextNode( true);
        node.setAutoSized(true);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        node.setFillColor(COLOR_TRANSPARENT);
        node.setFont(LWNode.DEFAULT_TEXT_FONT);
        
        VueBeanState state = getNodeToolPanel().getValue();
        if (state != null) {
            state.applyState( node);
        }
        
        return node;
    }
    
    public static NodeTool.SubTool getActiveSubTool()
    {
        return (SubTool) getTool().getSelectedSubTool();
    }

    public static class SubTool extends VueSimpleTool
    {
        private Class shapeClass = null;
        private RectangularShape cachedShape = null;
            
        public SubTool() {}

	public void setID(String pID) {
            super.setID(pID);
            //System.out.println(this + " ID set");
            getShape(); // cache it for fast response first time
        }

        public RectangularShape getShapeInstance()
        {
            if (shapeClass == null) {
                String shapeClassName = getAttribute("shapeClass");
                System.out.println(this + " got shapeClass " + shapeClassName);
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

        //private static final Color bgColor = new Color(127,127,127);
        //private static final Color bgColor = new Color(230,230,230);
        //private static final Color fillColor = new Color(175,191,198);
        private static final int ICON_DEFAULT = 0;
        private static final int ICON_DOWN = 1;
        private static final int ICON_SELECTED = 2;

        //private static final Color sShapeColor = new Color(255,178,208); // diagnostic color
        private static final Color sShapeColor = new Color(165,178,208); // melanie's steel blue
        private static final Color sButtonColor = new Color(222,222,222);
        private static final Color sOverColor = Color.gray;
        //private static final Color sDownColor = new Color(211,211,211);
        private static final Color sDownColor = sOverColor;
        //private static final Color sOverColor = Color.white;
        //private static final Color sOverColor = new Color(244,244,244);
        
        class ShapeIcon implements Icon
        {
            final int w = 38;
            final int h = 27;
            final int xInset = 7;
            final int yInset = 6;
            final int arc = 15; // arc of rounded toolbar button border

            int type = ICON_DEFAULT;

            private Color mColor = new Color(230,230,230);
            //private Color mColor = new Color(200,200,200);
            //private Color mColor = Color.gray;

            private boolean mIsDown = false;
            private boolean mDrawButton = false;

            protected ShapeIcon(int type)
            {
                this.type = type;
            }
            
            protected ShapeIcon(Color c, boolean drawButton, boolean down) {
                mColor = c;
                mDrawButton = drawButton; // else: draw rounded border
                mIsDown = down;
            }
            
            protected ShapeIcon(Color c) {
                this(c, false, false);
            }
            
            public int getIconWidth() {return w; }
            public int getIconHeight() { return h; }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                //g2.setColor(Color.red);
                //g2.fillRect(0,0, 99,99);
                if (VueUtil.isMacPlatform())
                    g2.translate(3,3);
                else
                    g2.translate(1,1);
                float gw = w;
                //GradientPaint gradient = new GradientPaint(gw/2,0,Color.white,gw/2,h/2,mColor,true);
                //GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw/2,h/2,mColor,true);
                GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw*.33f,h/2,mColor,true);
                // Set gradient for the whole button.
                if (mColor.equals(sDownColor))
                    g2.setPaint(gradient);
                else
                    g2.setColor(mColor);
                if (mDrawButton) {
                    g2.setColor(mColor);
                    g2.fill3DRect(0,0, w-1,h-1, true);
                    g2.setPaint(gradient);
                    g2.fillRect(1,1, w-3,h-3);
                    //g2.drawRect(0,0, w-3,h-3);
                } else {
                    g2.fillRoundRect(0,0, w-3,h-3, arc,arc);
                    g2.setColor(Color.black);
                    g2.drawRoundRect(0,0, w-3,h-3, arc,arc);
                }
                
                g2.setColor(sShapeColor);
                RectangularShape shape = getShape();
                if (shape instanceof RoundRectangle2D) {
                    // hack to deal with arcs being too small on a tiny icon
                    shape = getShapeInstance();
                    // plus 2 on x/y inset for mac?
                    ((RoundRectangle2D)shape).setRoundRect(xInset, yInset, 20,12, 8,8);
                } else
                    shape.setFrame(xInset,yInset, 20,12);
                //shape.setFrame(xInset,yInset, w-xInset*2, h-yInset*2);                
                g2.fill(shape);
                g2.setColor(Color.black);
                g2.draw(shape);
            }
        }

        private boolean debug = false;
        
        // TOOLBAR: Unselected/default
        public Icon getIcon() {
            //return new ShapeIcon(ICON_DEFAULT);
            if (debug) return new ShapeIcon(Color.magenta);
            return new ShapeIcon(sButtonColor);
	}
        // TOOLBAR: Rollover
        public Icon getRolloverIcon() {
            if (debug) return new ShapeIcon(Color.yellow);
            return new ShapeIcon(sOverColor);
	}
        // TOOLBAR: flashes briefly after sub-menu item selected
        // or hen the mouse is being held down over it, but only
        // AFTER the sub-menu was displayed, then you hold mouse
        // down, and the sub-menu goes away, and you get this icon.
        // Rarely seen, so maybe don't even bother with anything different.
	public Icon getDownIcon() {
            if (debug) return new ShapeIcon(Color.red, false, true);
            return getSelectedIcon();

        }
        // TOOLBAR: Selected (down)
	public Icon getSelectedIcon() {
            if (debug) return new ShapeIcon(Color.orange);
            return new ShapeIcon(sDownColor, false, true);
	}

        // SUB-MENU: default display
	public Icon getMenuItemIcon() {
            //return mMenuItemIcon;
            return new ShapeIcon(sButtonColor, true, false);
	}
        
        // SUB-MENU: Rollver (or down-simulation)
	public Icon getMenuItemSelectedIcon() {
            //return mMenuItemSelectedIcon;
            if (debug) return new ShapeIcon(Color.green, true, true);
            return new ShapeIcon(sOverColor, true, true);
	}
	
	public Icon getDisabledIcon() {
            return new ShapeIcon(Color.cyan);
	}


        // The corner icon
	public Icon getOverlayUpIcon() {
            return mOverlayUpIcon;
	}
	public Icon getOverlayDownIcon() {
            return getOverlayUpIcon();
            // down-overlay out of position?
            // Oh, for down, whole icon supposed to
            // move down and to the right...
            //return mOverlayDownIcon;
	}

    }
    
}
