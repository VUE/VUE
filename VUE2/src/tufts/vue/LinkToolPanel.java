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
 * @version $Revision: 1.32 $ / $Date: 2006-01-20 19:26:27 $ / $Author: sfraize $
 * 
 * deprecated - on the way out
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

        //addPropertyProducer(linkTypeMenu);
        addPropertyProducer(arrowPropertyHandler);

        
        mArrowStartButton.addActionListener(arrowPropertyHandler);
        mArrowEndButton.addActionListener(arrowPropertyHandler);
    }
     
    protected VueBeanState getDefaultState() {
        return VueBeans.getState(LWLink.setDefaults(new LWLink()));
    }
            
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.init(args);
        VueUtil.displayComponent(new LinkToolPanel());
    }
}

