

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

    public boolean supportsSelection()
    {
        return false;
    }
    
    /*
    public boolean supportsXORSelectorDrawing()
    {
        return false;
    }
    */
    
    private java.awt.geom.RectangularShape currentShape = new tufts.vue.shape.RectangularPoly2D(4);
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
        g.setStroke(STROKE_TWO);
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
        return node;
    }
    public static LWNode createNode()
    {
        return createNode(null);
    }
    public static LWNode createTextNode(String text)
    {
        LWNode node = createNode(text);
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

        
    }
}
