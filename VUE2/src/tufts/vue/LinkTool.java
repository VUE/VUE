

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class LinkTool extends VueTool
{
    public LinkTool()
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

    public boolean supportsSelection() { return true; }
    
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);
        g.setColor(java.awt.Color.blue);
        super.drawSelector(g, r);
    }


}
