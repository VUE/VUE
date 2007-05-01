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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import tufts.vue.gui.ColorMenuButton;
import tufts.vue.gui.GUI;

/**
 * TextToolPanel
 */
 
public class TextToolPanel extends ToolPanel
{
    /** the Font selection combo box **/protected FontEditorPanel mFontPanel;
    
    public boolean isPreferredType(Object o) {
        return o instanceof LWNode && ((LWNode)o).isTextNode();
    }

    protected void buildBox() {
    	
        mFontPanel = new FontEditorPanel(LWKey.Font);
        //GUI.applyToolbarColor(mFontPanel);
	
        // Don't know if we need these constraints here, tho the FontEditorPanel layout
        // is broken on the mac right now (is clipped) -- SMF 2007-05-01
    	GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        mBox.add(mFontPanel,gbc);
        
    }
}
