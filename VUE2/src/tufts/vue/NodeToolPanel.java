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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.RectangularShape;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.BorderFactory;
import javax.swing.border.*;

/**
 * NodeToolPanel
 * This creates an editor panel for LWNode's
 */
 
public class NodeToolPanel extends LWCToolPanel
{
     public NodeToolPanel() {
         JLabel label = new JLabel("   Node: ");
         label.setFont(VueConstants.FONT_SMALL);
         getBox().add(new ShapeMenuButton(), 0);
         getBox().add(label, 0);
     }
         
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    
    private static class ShapeMenuButton extends MenuButton
    {
        /** The currently selected shape */
        //protected RectangularShape mShape;

        protected Object mSelectedAction;
			
        public ShapeMenuButton() {
            setToolTipText("Node Shape");
            
            //setPropertyName(LWKey.Shape);
            // note: without a property name we won't pick up shape
            // values when a node is selected -- that's okay tho.
            
            // had this when MenuButtons were in etched borders;
            //setBorder(new CompoundBorder(getBorder(), new EmptyBorder(1,1,2,1)));
            
            //setBorder(new MatteBorder(3,3,4,3, Color.red));
            //setBorder(new EmptyBorder(3,3,3,3));

            buildMenu(NodeTool.getTool().getShapeSetterActions());

            // start with icon set to that of first item in the menu
            setIcon(((AbstractButton)super.mPopup.getComponent(0)).getIcon());
        }


        protected void X_handleMenuSelection(ActionEvent e) {
            setIcon(((AbstractButton)e.getSource()).getIcon());
            super.handleMenuSelection(e);
            // We don't need to handle setting the property
            // as the shape setter action does that.
        }
        
        public void setPropertyValue(Object o) {
            //System.out.println(this + " setProp " + o);
            mSelectedAction = o;
            //setShape((RectangularShape)o);
        }
	 
        public Object getPropertyValue() {
            //System.out.println(this + " getProp " + mSelectedAction);
            return mSelectedAction;
            //return getShape();
        }
        
        /*
        public void setShape(RectangularShape shape) {
            mShape = shape;
        }
        public RectangularShape getShape() {
            return mShape;
        }
        */

    }


    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
}
