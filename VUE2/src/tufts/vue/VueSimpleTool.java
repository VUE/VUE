

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class VueSimpleTool extends VueTool
{
    public VueSimpleTool() {
        super();
    }
    
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public void handleSelection() {
        // getMapViewer().setCurrentTool( this);
    }
    
    public JPanel getContextualPanel() {
        return null;
    }

}
