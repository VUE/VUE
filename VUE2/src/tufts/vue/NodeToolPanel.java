package tufts.vue;

import tufts.vue.beans.*;
import javax.swing.JLabel;

/**
 * NodeToolPanel
 * This creates an editor panel for LWNode's
 */
 
public class NodeToolPanel extends LWCToolPanel
{
     public NodeToolPanel() {
         JLabel label = new JLabel("   Node:");
         label.setFont(VueConstants.FONT_SMALL);
         getBox().add(label, 0);
     }
         
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
 	
}
