

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class TextTool extends VueTool
{
    /** the contextual tool panel **/
    private static TextToolPanel sTextToolPanel;
	
    public TextTool() {
        super();
    }
	
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public void handleSelection() {
        
    }
    
    static TextToolPanel getTextToolPanel()
    {
        if (sTextToolPanel == null)
            sTextToolPanel = new TextToolPanel();
        return sTextToolPanel;
    }
    
    public JPanel getContextualPanel() {
        return getTextToolPanel();
    }
    
    
    public boolean supportsSelection() { return true; }
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return false; }
    
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
