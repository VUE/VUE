

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class NodeTool extends VueTool
                              implements VueConstants
{
    public NodeTool()
    {
        super();
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
    
    public boolean supportsXORSelectorDrawing()
    {
        return false;
    }
    
    private final java.awt.geom.RectangularShape currentShape = new tufts.vue.shape.RectangularPoly2D(4);
    public void drawSelectorBox(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);
        g.draw(r);
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
    
}
