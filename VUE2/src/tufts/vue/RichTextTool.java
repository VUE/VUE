package tufts.vue;

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

// import java.util.*;
// import javax.swing.*;

public class RichTextTool extends VueTool
{
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
