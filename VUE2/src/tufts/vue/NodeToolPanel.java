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
 * This creates an editor panel for LWNode's
 *
 * @version $Revision: 1.35 $ / $Date: 2007-04-16 21:28:34 $ / $Author: sfraize $
 */
 
public class NodeToolPanel extends LWCToolPanel
{
    public NodeToolPanel() {
        ShapeMenuButton mShapeButton;
    
        //JLabel label = new JLabel("   Node: ");
        //label.setFont(VueConstants.FONT_SMALL);
        getBox().add(mShapeButton = new ShapeMenuButton(), 0);
        mShapeButton.addPropertyChangeListener(this);
        addEditor(mShapeButton);
        //getBox().add(label, 0);
    }
    
    public boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    static class ShapeMenuButton extends VuePopupMenu<RectangularShape>
    {
        public ShapeMenuButton() {
            super(LWKey.Shape, NodeTool.getTool().getShapeSetterActions());
            setToolTipText("Node Shape");
        }

        protected Dimension getButtonSize() {
            return new Dimension(37,22);
        }

        /** @param o an instance of RectangularShape */
        public void displayValue(RectangularShape shape) {
            if (DEBUG.TOOL) System.out.println(this + " displayValue " + shape.getClass() + " [" + shape + "]");

            if (mCurrentValue == null || !mCurrentValue.getClass().equals(shape.getClass())) {
                mCurrentValue = shape;

                // This is inefficent in that we there are already shape icons out there (produced
                // in getShapeSetterActions()) that we could use, but doing it this way (creating a
                // new one every time) will allow for ANY rectangular shape to display properly in
                // the tool menu, even it is a deprecated shape or non-standard shape (not defined
                // as a standard from for the node tool in VueResources.properties).  (This is
                // especially in-effecient if you look at what setButtonIcon does in MenuButton: it
                // creates first a proxy icon, and then creates and installs a whole set of
                // VueButtonIcons for all the various states the button can take, for a totale of 7
                // objects every time we do this (1 for the clone, 1 for proxy, 5 via
                // VueButtonIcon.installGenerated)
                
                setButtonIcon(makeIcon(shape));
            }
        }

        /** @return new icon for the given shape */
        protected Icon makeIcon(RectangularShape shape) {
            return new NodeTool.SubTool.ShapeIcon((RectangularShape) shape.clone());
        }
	 
    }
    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.init(args);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }
}
