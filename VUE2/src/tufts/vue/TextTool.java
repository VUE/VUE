

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class TextTool extends VueTool
{
    public TextTool() {
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
    
    // todo: do we really want to do this?
    public void handleSelectorRelease(java.awt.geom.Rectangle2D mapRect)
    {
        LWNode node = NodeTool.createTextNode("new text");
        node.setAutoSized(false);
        node.setFrame(mapRect);
        VUE.getActiveMap().addNode(node);
        VUE.ModelSelection.setTo(node);
        VUE.getActiveViewer().activateLabelEdit(node);
    }
    

}
