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

import java.lang.*;
import java.util.*;
import javax.swing.*;

public class ImageTool extends VueTool
{
    public ImageTool() {
        super();
    }

    public JPanel getContextualPanel() {
        JPanel p = new JPanel();
        p.add(new JLabel("IMAGE TOOL"));
        return p;
    }
    
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }
    public boolean supportsSelection() { return true; }
    public boolean hasDecorations() { return true; }
    
    /*
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public void handleSelection() {
        
    }

    private static TextToolPanel sTextToolPanel;
	
    static TextToolPanel getTextToolPanel()
    {
        if (sTextToolPanel == null)
            sTextToolPanel = new TextToolPanel();
        return sTextToolPanel;
    }
    
    
    */
    
}
