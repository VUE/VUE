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
 */
public class LinkPropertyPanel extends JPanel
{
    public LinkPropertyPanel()
    {
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
        
        add(linkTypeMenu);
        //add(Box.createHorizontalStrut(3));
        add(mArrowStartButton);
        add(mArrowEndButton);

        final LWPropertyHandler arrowPropertyHandler =
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
    }
     
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        tufts.vue.VUE.init(args);
        //tufts.Util.displayComponent(new LinkPropertyPanel());
        new Frame("A Frame").show();
        new DockWindow("Link", new LinkPropertyPanel()).show();
    }
}

