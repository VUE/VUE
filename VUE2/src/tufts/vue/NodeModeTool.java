 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


package tufts.vue;

import tufts.vue.shape.*;

import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import tufts.vue.beans.VueBeanState;

/**
 * VueTool for creating LWNodes.  Methods for creating default new nodes based on
 * tool states, and for handling the drag-create of new nodes.
 */

public class NodeModeTool extends VueTool
    implements VueConstants
{          

    
    /** this constructed called via VueResources.properties init */
    public NodeModeTool()
    {
        super();
//        if (singleton != null) 
//            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
//        singleton = this;
    }

    final public Object getPropertyKey() { return LWKey.Shape; }
    
    /** return the singleton instance of this class */
    public static NodeTool getTool()
    {
        return (NodeTool)VueToolbarController.getController().getTool("nodeTool");
    }
    
    
    
    public JPanel getContextualPanel() {
        return null;
    }

    public static NodeTool.SubTool getActiveSubTool()
    {
        NodeTool tool = (NodeTool)getTool();
        return (NodeTool.SubTool)tool.getSelectedSubTool();
    }

//    public Class getSelectionType() { return LWNode.class; }
    
    public boolean supportsSelection() { return true; }
    
    public boolean handleSelectorRelease(MapMouseEvent e)
    {
    	LWNode node = createNode(VueResources.getString("newnode.html"), true);
        node.setAutoSized(false);
        node.setFrame(e.getMapSelectorBox());
        MapViewer viewer = e.getViewer();
        viewer.getFocal().addChild(node);
        VUE.getUndoManager().mark("New Node");
        VUE.getSelection().setTo(node);
        viewer.activateLabelEdit(node);
        creationNodeCurrent = false;
        return true;
    }

    private LWNode creationNode = new LWNode();
    private boolean creationNodeCurrent = false;

    public void drawSelector(DrawContext dc, java.awt.Rectangle r)
    {
        dc.g.draw(r);

        /*
        // faster to pull directly from the known editor object, but this works, and is more general.
        RectangularShape currentShape = (RectangularShape) VUE.LWToolManager.GetPropertyValue(LWKey.Shape);
        Stroke currentStroke = (Stroke) VUE.LWToolManager.GetPropertyValue(LWKey.StrokeStyle);
        currentShape.setFrame(r);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,  java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(COLOR_SELECTION);
        g.draw(currentShape);
        */

        if (!creationNodeCurrent) {
            VUE.LWToolManager.ApplyProperties(creationNode);
            creationNodeCurrent = true;
        }

        // TODO: doesn't handle zoom
        // Also, handleSelectorRelease can now just dupe the creationNode
        
        //dc.setMapDrawing();
        creationNode.setFrame(r);
        creationNode.draw(dc);
        
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

    /**
     * Create a new node with the current default properties
     * @param name the name for the new node, can be null
     * @return the newly constructed node
     */
    public static LWNode createNode(String name) {
        return createNode(name, false);
    }

    
    /** @return a new default node with no label */
    public static LWNode createNode() {
        return createNode(null);
    }
    /** @return a new default node with the default new node label */
    public static LWNode createNewNode() {
        return createNode(VueResources.getString("newnode.html"));
    }
        
    
    public static LWNode initAsTextNode(LWNode node)
    {
        node.setIsTextNode(true);
        node.setAutoSized(true);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        //node.setFillColor(COLOR_TRANSPARENT);

        Font font = (Font) VUE.LWToolManager.GetPropertyValue(LWKey.Font);
        if (font == null)
            font = LWNode.DEFAULT_TEXT_FONT;
        node.setFont(font);
        
        return node;
    }

    public static LWNode buildTextNode(String text) {
        LWNode node = new LWNode();
        initAsTextNode(node);
        node.setLabel(text);
        return node;
    }

    
    
    /**
     * Create a new node with the current default properties.
     * @param name the name for the new node, can be null
     * @param [deprecated - ignored] useToolShape if true, shape of node is shape of node tool, otherwise, shape in contextual toolbar
     * @return the newly constructed node
     */
    public static LWNode createNode(String name, boolean useToolShape)
    {
        LWNode node = new LWNode(name);
        VUE.LWToolManager.ApplyProperties(node);
        /*
        if (useToolShape) {
            node.setShape(getActiveSubTool().getShape());
            VUE.LWToolManager.ApplyProperties(node, ~LWKey.Shape.bit);
        } else {
            VUE.LWToolManager.ApplyProperties(node);
        }
        */
        return node;
    }
    
    /** For creating text nodes through the tools and on the map: will adjust text size for current zoom level */
    public static LWNode createTextNode(String text)
    {
        LWNode node = buildTextNode(text);
        if (VUE.getActiveViewer() != null) {
            // Okay, for now this completely overrides the font size from the text toolbar...
            final Font font = node.getFont();
            final float curZoom = (float) VUE.getActiveViewer().getZoomFactor();
            final int minSize = LWNode.DEFAULT_TEXT_FONT.getSize();
            //if (curZoom * font.getSize() < minSize)
                node.setFont(font.deriveFont(minSize / curZoom));
        }
            
        return node;
    }
    
    /** @return an array of actions, with icon set, that will set the shape of selected
     * LWNodes */
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
    
    /** @return an array of standard supported shapes for nodes */
    public Object[] getAllShapeValues() {
        Object[] values = new Object[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            NodeTool.SubTool nt = (NodeTool.SubTool) getSubTool(id);
            values[i++] = nt.getShape();
        }
        return values;
    }

}
