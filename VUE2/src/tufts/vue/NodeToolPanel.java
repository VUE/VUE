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

import tufts.vue.gui.*;
import tufts.vue.beans.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.RectangularShape;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.border.*;

/**
 * NodeToolPanel
 * This creates an editor panel for LWNode's
 */
 
public class NodeToolPanel extends LWCToolPanel
{
    private ShapeMenuButton mShapeButton;
    
    public NodeToolPanel() {
        JLabel label = new JLabel("   Node: ");
        label.setFont(VueConstants.FONT_SMALL);
        getBox().add(mShapeButton = new ShapeMenuButton(), 0);
        mShapeButton.addPropertyChangeListener(this);
        addPropertyProducer(mShapeButton);
        getBox().add(label, 0);
    }
    
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    private static class ShapeMenuButton extends MenuButton
    {
        protected RectangularShape mShape; // an instance of the currently selected shape

        public ShapeMenuButton() {
            setToolTipText("Node Shape");
            setPropertyKey(LWKey.Shape);
            Action[] actions = NodeTool.getTool().getShapeSetterActions();

            /*
            if (DEBUG.Enabled) {
                for (int i = 0; i < actions.length; i++) {
                    Action a = actions[i];
                    System.out.println("*** " + a + " " + a.getValue(LWKey.Shape).getClass());
                }
            }
            */
            buildMenu(actions);

            // start with icon set to that of first item in the menu
            //setButtonIcon(((AbstractButton)super.mPopup.getComponent(0)).getIcon());
        }

        protected Dimension getButtonSize() {
            return new Dimension(37,22);
        }

        /** @param o an instance of RectangularShape */
        public void setPropertyValue(Object value) {
            if (DEBUG.TOOL) System.out.println(this + " setPropertyValue " + value.getClass() + " [" + value + "]");

            if (mShape == null || !mShape.getClass().equals(value.getClass())) {
                mShape = (RectangularShape) value;

                // This is inefficent in that we there are already shape icons out there 
                // (produced in getShapeSetterActions()) that we could use, but doing
                // it this way (creating a new one every time) will allow for ANY
                // rectangular shape to display properly in the tool menu, even it is
                // a deprecated shape or non-standard shape (not defined as a standard
                // from for the node tool in VueResources.properties).  (This is especially
                // in-effecient if you look at what setButtonIcon does in MenuButton: it
                // creates first a proxy icon, and then creates and installs a whole set of VueButtonIcons
                // for all the various states the button can take, for a totale of 7 objects
                // every time we do this (1 for the clone, 1 for proxy, 5 via VueButtonIcon.installGenerated)
                
                setButtonIcon(makeIcon(value));
            }
        }

        /** @return new icon for the given shape */
        protected Icon makeIcon(Object value) {
            RectangularShape shape = (RectangularShape) value;
            return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
        }
	 
        /** @return  an instanceof RectangularShape, suitable for cloning & installing as a new node shape via setShape */
        public Object getPropertyValue() {
            return mShape;
        }

        /*
        protected void handleMenuSelection(ActionEvent e) {
            Icon i = ((AbstractButton)e.getSource()).getIcon();
            System.out.println(this + " handeMenuSelection: selection was " + i);
            if (i != null) {
                setButtonIcon(i);// note: inefficient as creates a new set of VueButtonIcons every time
                setToolTipText("Node Shape: " + e.getActionCommand());
            }
            super.handleMenuSelection(e);
            //mPopup.setSelected(o);
        }
        */
        
    }


    /*
    void loadValues(Object data) {
        super.loadValues(data);
        if (data instanceof LWNode) {
            if (DEBUG.TOOL) out("loadValues (NodeToolPanel) " + data);
            LWNode node = (LWNode) data;
            System.out.println(this + " set the selected shape menu button to " + node.getShape());
            //mShapeMenuButton.setPropertyValue(...)
            // and if not save to change above to automatically do the below:
            //mShapeMenuButton.setButtonIcon(...)
        }
    }
    */
    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
}
