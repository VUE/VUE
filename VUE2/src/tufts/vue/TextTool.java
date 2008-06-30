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

// import java.util.*;
// import javax.swing.*;
import tufts.vue.NodeTool.NodeModeTool;

public class TextTool extends VueTool
{
    public TextTool() {}


    /** @return LWNode.TYPE_TEXT */
    @Override
    public Object getSelectionType() {
        return LWNode.TYPE_TEXT;
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
        /*
    
    public void handleSelectorRelease(java.awt.geom.Rectangle2D mapRect)
    {
        LWNode node = NodeTool.createTextNode("new text");
        node.setAutoSized(false);
        node.setFrame(mapRect);
        VUE.getActiveMap().addNode(node);
        VUE.getSelection().setTo(node);
        VUE.getActiveViewer().activateLabelEdit(node);
    }
        */
    

}
