/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package tufts.vue;

import static tufts.Util.grow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;

import tufts.vue.NodeTool.NodeModeTool;

// import java.util.*;
// import javax.swing.*;

public class RichTextTool extends VueTool
{
	private  LWText creationNode = null;
    public RichTextTool() {}


    /** @return LWNode.TYPE_TEXT */
    @Override
    public Object getSelectionType() {
        return LWText.TYPE_RICHTEXT;
    }
    

//     @Override
//     protected LWComponent createStyleCache() {
//         return NodeModeTool.createTextNode("StyleCache: " + getClass());
//     }

    /*
      // this prevents us from clicking on a regular node to immediately activate text edit...
    public boolean accept(LWComponent c) {
        return tufts.vue.LWNode.isTextNode(c);
    }
    */
    
    
    /*
    /* the contextual tool panel 
    //private static TextToolPanel sTextToolPanel;
	
    static TextToolPanel getTextToolPanel()
    {
        if (sTextToolPanel == null)
            sTextToolPanel = new TextToolPanel();
        return sTextToolPanel;
    }
    
    public JPanel getContextualPanel() {
        return getTextToolPanel();
    }
    */

    //public boolean supportsClick() { return true; }

        /*
    public void handleMouseClicked(java.awt.event.MouseEvent e, LWComponent hitComponent)
    {
        if (hitComponent != null) {
            if (!(hitComponent instanceof LWGroup))
                activateLabelEdit(hitComponent);
        } else {
            // we either need map X/Y also or access to mapviewer coversion routines...
        }
        
        System.out.println(this + " TexTool.handleMouseClicked");
    }
        */
    
    // todo: do we really want to do this?
     


    
    @Override
	public boolean handleSelectorRelease(MapMouseEvent e)
    {
    	 LWText node = NodeModeTool.buildRichTextNode("new text");
         node.getRichLabelBox(true).overrideTextColor(FontEditorPanel.mTextColorButton.getColor());
         node.setAutoSized(false);
         node.setFrame(e.getMapSelectorBox());
         MapViewer viewer = e.getViewer();
         viewer.getFocal().addChild(node);
         VUE.getUndoManager().mark("New Rich Text");
         VUE.getSelection().setTo(node);
       //  viewer.activateLabelEdit(node);
        
        return true;
    }
        
    @Override
    public void drawSelector(DrawContext dc, java.awt.Rectangle r)
    {
        dc.g.draw(r);
        
        // TODO: doesn't handle zoom
        // Also, handleSelectorRelease can now just dupe the creationNode
    	if (creationNode == null)
    	{
    		creationNode = NodeModeTool.buildRichTextNode(null);
    	}
        creationNode.setFillColor(VueConstants.COLOR_HIGHLIGHT);
        creationNode.setFrame(r);
    	dc.g.setColor(VueConstants.COLOR_HIGHLIGHT);
      

        creationNode.draw(dc);
  
    }    

}
