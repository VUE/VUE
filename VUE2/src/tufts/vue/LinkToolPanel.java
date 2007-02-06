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
 *
 * @version $Revision: 1.34 $ / $Date: 2007-02-06 21:50:39 $ / $Author: sfraize $
 * 
 */

public class LinkToolPanel extends LWCToolPanel
{
    public boolean isPreferredType(Object o) {
        return o instanceof LWLink;
    }
    
    protected void buildBox()
    {
        final AbstractButton mArrowStartButton = new VueButton.Toggle("link.button.arrow.start");
        final AbstractButton mArrowEndButton = new VueButton.Toggle("link.button.arrow.end");
        
        //setting up tooltips for link specific buttons.
        mArrowStartButton.setToolTipText(VueResources.getString("linkToolPanel.startArrow.toolTip"));
        mArrowEndButton.setToolTipText(VueResources.getString("linkToolPanel.endArrow.toolTip"));
        
        final Action[] LinkTypeActions = new Action[] { 
            Actions.LinkMakeStraight,
            Actions.LinkMakeQuadCurved,
            Actions.LinkMakeCubicCurved
        };
        
        AbstractButton linkTypeMenu = new VuePopupMenu(LWKey.LinkCurves, LinkTypeActions);
        linkTypeMenu.setToolTipText("Link Style");
        linkTypeMenu.addPropertyChangeListener(this);
        
        final LWPropertyHandler arrowPropertyHandler =
            new LWPropertyHandler<Integer>(LWKey.LinkArrows, LinkToolPanel.this) {
                public Integer produceValue() {
                    int arrowState = LWLink.ARROW_NONE;
                    if (mArrowStartButton.isSelected())
                        arrowState |= LWLink.ARROW_EP1;
                    if (mArrowEndButton.isSelected())
                        arrowState |= LWLink.ARROW_EP2;
                    return arrowState;
                }
                public void displayValue(Integer i) {
                    int arrowState = i;
                    mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
                      mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
                }
            };

        //LWCToolPanel.InstallHandler(mArrowStartButton, arrowPropertyHandler);
        //LWCToolPanel.InstallHandler(mArrowEndButton, arrowPropertyHandler);

        // We can't just rely on the each handler hanging free without knowing about it.
        // It works when the editor activates -- we can find which tool panel it's in
        // (up the AWT chain), and could find the right default state to work with
        // (node/link/text, etc).  But when a selection happens and the tool panel needs
        // to LOAD UP all these property editors, this is the only way we can know about
        // it...  Otherwise, we'd have to make every LWPropertyHandler a selection
        // listener in it's own right (tho this wouldn't be instance, given that every
        // single action in the system is also a selection listener!)
        
        //super.addEditor(arrowPropertyHandler);

        mArrowStartButton.addActionListener(arrowPropertyHandler);
        mArrowEndButton.addActionListener(arrowPropertyHandler);
        //mArrowStartButton.addItemListener(arrowPropertyHandler);
        //mArrowEndButton.addItemListener(arrowPropertyHandler);

        addComponent(linkTypeMenu);
        addComponent(Box.createHorizontalStrut(3));
        addComponent(mArrowStartButton);
        addComponent(mArrowEndButton);
        addComponent(mStrokeColorButton);
        addComponent(mStrokeButton);
        addComponent(mFontPanel);
        addComponent(mTextColorButton);
    }
     
    //protected VueBeanState getDefaultState() { return VueBeans.getState(LWLink.setDefaults(new LWLink())); }
    protected LWComponent createDefaultStyle() {
        LWLink l = new LWLink();
        l.setLabel("defaultLinkStyle");
        return LWLink.SetDefaults(l);
    }
            
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.init(args);
        VueUtil.displayComponent(new LinkToolPanel());
    }
}

