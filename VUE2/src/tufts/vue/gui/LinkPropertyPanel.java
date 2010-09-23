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

package tufts.vue.gui;

import tufts.vue.Actions;
import tufts.vue.LWKey;
import tufts.vue.LWPropertyHandler;
import tufts.vue.LWLink;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;


/**
 * A property editor panel for LWLink's.
 * Unused new code...
 */
public class LinkPropertyPanel extends Box
{
    public LinkPropertyPanel()
    {
	super(BoxLayout.X_AXIS);
        /*

        final AbstractButton mArrowStartButton = new VueButton.Toggle("link.button.arrow.start");
        final AbstractButton mArrowEndButton = new VueButton.Toggle("link.button.arrow.end");
        
        final Action[] LinkTypeActions = new Action[] { 
            Actions.LinkMakeStraight,
            Actions.LinkMakeQuadCurved,
            Actions.LinkMakeCubicCurved
        };

        AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkCurves, LinkTypeActions);
        linkTypeMenu.setToolTipText("Link Style");
        linkTypeMenu.addPropertyChangeListener(GUI.getPropertyChangeHandler());
        
        add(Box.createHorizontalStrut(3));
        add(linkTypeMenu);
        add(Box.createHorizontalStrut(2));
        add(mArrowStartButton);
        add(mArrowEndButton);
        add(Box.createHorizontalStrut(5));

        final ActionListener arrowPropertyHandler =
            new LWPropertyHandler(LWKey.LinkArrows, GUI.getPropertyChangeHandler()) {
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

        //addPropertyProducer(arrowPropertyHandler);
        mArrowStartButton.addActionListener(arrowPropertyHandler);
        mArrowEndButton.addActionListener(arrowPropertyHandler);
        */
    }
     
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        tufts.vue.VUE.init(args);
        //tufts.Util.displayComponent(new LinkPropertyPanel());
        //new Frame("A Frame").show();
        GUI.createToolbar("Link", new LinkPropertyPanel()).setVisible(true);
    }
}

