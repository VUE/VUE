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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;


/**
 * A property editor panel for LWLink's.
 */
public class LinkToolPanel extends LWCToolPanel
{
    protected void buildBox()
    {
        /*
        final AbstractButton linkStraight = new VueButton.Toggle("linkTool.line");
        final AbstractButton linkCurved = new VueButton.Toggle("linkTool.curve1");
        final AbstractButton linkCurvedS = new VueButton.Toggle("linkTool.curve2");
        ButtonGroup linkTypeGroup = new ButtonGroup();
        linkTypeGroup.add(linkStraight);
        linkTypeGroup.add(linkCurved);
        linkTypeGroup.add(linkCurvedS);
        */

        final AbstractButton mArrowStartButton = new VueButton.Toggle("link.button.arrow.start");
        final AbstractButton mArrowEndButton = new VueButton.Toggle("link.button.arrow.end");
        
        //JLabel label = new JLabel("   Link: ");
        //label.setFont(VueConstants.FONT_SMALL);
        //addComponent(label);
        
        //addComponent(linkStraight);
        //addComponent(linkCurved);
        //addComponent(linkCurvedS);
        
        //LinkMenuButton linkTypeMenu = new LinkMenuButton();
        final Action[] LinkTypeActions = new Action[] { 
            Actions.LinkMakeStraight,
            Actions.LinkMakeQuadCurved,
            Actions.LinkMakeCubicCurved
        };
        
        AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkCurves, LinkTypeActions);
        linkTypeMenu.setToolTipText("Link Style");
                
        linkTypeMenu.addPropertyChangeListener(this);
        addComponent(linkTypeMenu);
        addComponent(Box.createHorizontalStrut(3));
        addComponent(mArrowStartButton);
        addComponent(mArrowEndButton);
        addComponent(mStrokeColorButton);
        addComponent(mStrokeButton);
        addComponent(mFontPanel);
        addComponent(mTextColorButton);

        final LWPropertyHandler arrowPropertyHandler =
            new LWPropertyHandler(LWKey.LinkArrows, this) {
                public Object getPropertyValue() {
                    int arrowState = LWLink.ARROW_NONE;
                    if (mArrowStartButton.isSelected())
                        arrowState |= LWLink.ARROW_EP1;
                    if (mArrowEndButton.isSelected())
                        arrowState |= LWLink.ARROW_EP2;
                    return new Integer(arrowState);
                }
                public void setPropertyValue(Object o) {
                    int arrowState = ((Integer)o).intValue();
                    mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
                      mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
                }
            };

        addPropertyProducer(arrowPropertyHandler);
        mArrowStartButton.addActionListener(arrowPropertyHandler);
        mArrowEndButton.addActionListener(arrowPropertyHandler);

        
        /*
        final LWPropertyHandler curvePropertyHandler =
            new LWPropertyHandler(LWKey.LinkCurves, this) {
                public Object getPropertyValue() {
                    if (linkStraight.isSelected())
                        return new Integer(0);
                    else
                        return new Integer(1);
                }
                public void setPropertyValue(Object o) {
                    int curves = ((Integer)o).intValue();
                    if (curves == 0)
                        linkStraight.setSelected(true);
                    else
                        linkCurved.setSelected(true);
                }
            };
        addPropertyProducer(curvePropertyHandler);
        linkStraight.addActionListener(curvePropertyHandler);
        linkCurved.addActionListener(curvePropertyHandler);
        */
        
    }
     
    protected VueBeanState getDefaultState() {
        return VueBeans.getState(LWLink.setDefaults(new LWLink()));
    }
            
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.initUI(true);
        VueUtil.displayComponent(new LinkToolPanel());
    }
}


/*
    private static class LinkMenuButton extends MenuButton {
        private static Action[] LinkTypes = new Action[] { 
                          Actions.LinkMakeStraight,
                          Actions.LinkMakeQuadCurved,
                          Actions.LinkMakeCubicCurved
        };

        private int mCurves = -1;

        LinkMenuButton() {
            setPropertyKey(LWKey.LinkCurves);
            buildMenu(LinkTypes);
            setPropertyValue(new Integer(0));
        }

        /** set our menu-state to reflect the given integer property value 
        public void setPropertyValue(Object propertyValue) {
            System.out.println(this + " set value " + propertyValue);
            int newValue = ((Integer)propertyValue).intValue();
            if (mCurves != newValue) {
                mCurves = newValue;

                // if we move selecting the icon to an automatic
                // process via "property.value" searches thru the
                // JMenuItems (presuming menu icons same as button
                // icon) and we generically support an
                // objectPropertyValue in MenuButton, we could
                // eliminate this whole class, and just have a
                // MenuButton (or VueMenuButton) that initializes with
                // the action array & the property key (and it could
                // do a setPropertyValue to the first property.value
                // in the list)

                // So the whole init could look like:
                // new VueMenuButton(LWKey.LinkCurves, LinkTypes);

                setButtonIcon((Icon) LinkTypes[mCurves].getValue(Action.SMALL_ICON));
            }
        }
        public Object getPropertyValue() {
            System.out.println(this + " get value " + mCurves);
            return new Integer(mCurves);
        }
    }
*/