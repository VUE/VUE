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

import tufts.vue.beans.*;

/**
 * TextToolPanel
 * This creates an editor panel for text LWNode's (isTextNode() == true)
 * This is really a sub-set of an LWCToolPanel: we use only the font
 * panel and the text color properties.
 */
 
public class TextToolPanel extends LWCToolPanel
{
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode && ((LWNode)o).isTextNode();
    }

    protected void buildBox() {
        addComponent(mFontPanel);
        addComponent(mTextColorButton);
    }
     
    protected void initDefaultState() {
        //System.out.println("TextToolPanel.initDefaultState");
        super.mDefaultState = VueBeans.getState(NodeTool.initTextNode(new LWNode()));
    }
}
