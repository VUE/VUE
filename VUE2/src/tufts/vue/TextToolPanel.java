package tufts.vue;

import tufts.vue.beans.*;

/**
 * TextToolPanel
 * This creates an editor panel for text LWNode's (isTextNode() == true)
 */
 
public class TextToolPanel extends NodeToolPanel
{
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode && ((LWNode)o).isTextNode();
    }
     
    protected javax.swing.JComponent getLabelComponent() {
        javax.swing.JComponent label = new javax.swing.JLabel("   Text:");
        label.setFont(VueConstants.FONT_SMALL);
        return label;
    }

    protected void initDefaultState() {
        //System.out.println("TextToolPanel.initDefaultState");
        super.mDefaultState = VueBeans.getState(NodeTool.initTextNode(new LWNode()));
    }
}
