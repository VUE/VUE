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

import tufts.vue.gui.GUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Iterator;
import java.util.Map;

public class LaunchPresentationTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public LaunchPresentationTool() {
        super();
        
    }
    
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public boolean handleMousePressed(MapMouseEvent e)
    {
            return false;
    }

    
    public void actionPerformed(ActionEvent e) {
        Actions.LaunchPresentation.act();
        return;
    }


    public boolean supportsSelection() { return false; }

    // todo: need selection, but no drag, and click-to-deselect still working
    public boolean supportsDraggedSelector(MapMouseEvent e) { return false; }
}