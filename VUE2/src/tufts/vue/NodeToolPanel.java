package tufts.vue;

import tufts.vue.beans.*;

/**
 * NodeToolPanel
 * This creates an editor panel for LWNode's
 */
 
public class NodeToolPanel extends LWCToolPanel
{
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    
    protected javax.swing.JComponent getLabelComponent() {
        javax.swing.JComponent label = new javax.swing.JLabel("   Node:");
        label.setFont(VueConstants.FONT_SMALL);
        return label;
    }
 	
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
 	
}
