

package tufts.vue;

import tufts.vue.shape.*;

import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class NodeTool extends VueTool
                              implements VueConstants
{
    private static NodeTool singleton = null;
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
    
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public void handleSelection() {
        
    }
    
    public JPanel getContextualPanel() {
        return null;
    }

    public boolean supportsSelection() { return false; }
    
    /*
    public boolean supportsXORSelectorDrawing()
    {
        return false;
    }
    */
    
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
        g.setColor(COLOR_NODE_DEFAULT);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.fill(currentShape);
        g.setColor(COLOR_BORDER);
        g.setStroke(STROKE_TWO); // todo: scale based on the scale in the GC affine transform
        //g.setStroke(new BasicStroke(2f * (float) g.getTransform().getScaleX())); // GC not scaled while drawing selector...
        g.draw(currentShape);
        /*
        if (VueUtil.isMacPlatform()) // Mac 1.4.1 handles XOR differently than PC
            g.draw(currentShape);
        else
            g.fill(currentShape);
        */
    }
    
    public void handleSelectorRelease(java.awt.geom.Rectangle2D mapRect)
    {
        LWNode node = createNode();
        node.setAutoSized(false);
        node.setFrame(mapRect);
        VUE.getActiveMap().addNode(node);
        VUE.ModelSelection.setTo(node);
    }


    public static LWNode createNode(String name)
    {
        LWNode node = new LWNode(name, getActiveSubTool().getShapeInstance());
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
        node.setAutoSized(true);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        node.setFillColor(COLOR_TRANSPARENT);
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

        //private static final Color bgColor = new Color(127,127,127);
        //private static final Color bgColor = new Color(230,230,230);
        //private static final Color fillColor = new Color(175,191,198);
        private static final int ICON_DEFAULT = 0;
        private static final int ICON_DOWN = 1;
        private static final int ICON_SELECTED = 2;
        private static final Color fillColor = new Color(165,178,208);
        class ShapeIcon implements Icon
        {
            final int w = 38;
            final int h = 27;
            final int xInset = 7;
            final int yInset = 6;
            final int arc = 13;

            int type = ICON_DEFAULT;

            private Color bgColor = new Color(230,230,230);
            //private Color bgColor = new Color(200,200,200);
            //private Color bgColor = Color.gray;

            protected ShapeIcon(int type)
            {
                this.type = type;
            }
            
            protected ShapeIcon(Color c)
            {
                bgColor = c;
            }
            
            public int getIconWidth() {return w; }
            public int getIconHeight() { return h; }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.red);
                g2.fillRect(0,0, 99,99);
                g2.translate(3,3);
                float gw = w;
                //GradientPaint gradient = new GradientPaint(gw/2,0,Color.white,gw/2,h/2,bgColor,true);
                //GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw/2,h/2,bgColor,true);
                GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw*.33f,h/2,bgColor,true);
                g2.setPaint(gradient);                
                //g2.setColor(bgColor);
                g2.fillRoundRect(0,0, w-3,h-3, arc,arc);
                g2.setColor(Color.black);
                g2.drawRoundRect(0,0, w-3,h-3, arc,arc);
                g2.setColor(fillColor);
                RectangularShape shape = getShape();
                if (shape instanceof RoundRectangle2D) {
                    // hack to deal with arcs being too small on a tiny icon
                    shape = getShapeInstance();
                    ((RoundRectangle2D)shape).setRoundRect(xInset, yInset, 20,12, 7,7);
                } else
                    shape.setFrame(xInset,yInset, 20,12);
                //shape.setFrame(xInset,yInset, w-xInset*2, h-yInset*2);                
                g2.fill(shape);
                g2.setColor(Color.black);
                g2.draw(shape);
            }
        }

        /*
	public Icon getIcon() {
            //return new ShapeIcon(Color.yellow);
            return new ShapeIcon(ICON_DEFAULT);
	}
        // downIcon only drawn when the mouse is being held down over it
	public Icon getDownIcon() {
            //return new ShapeIcon(Color.red);
            return getSelectedIcon();
	}
        // The selected "down" icon (depressed)
	public Icon getSelectedIcon() {
            //return new ShapeIcon(ICON_DEFAULT);
            //return new ShapeIcon(new Color(200,200,200));
            return new ShapeIcon(Color.lightGray);
	}
	public Icon getRolloverIcon() {
            return new ShapeIcon(Color.white);
	}
	public Icon getDisabledIcon() {
            return new ShapeIcon(Color.black);
	}
        */

    }
    
}
