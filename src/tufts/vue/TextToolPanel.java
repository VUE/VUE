 /*
* Copyright 2003-2010 Tufts University  Licensed under the
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
    
    public FontEditorPanel getFontEditorPanel()
    {
    	return mFontPanel;
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
