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
 * LinkToolPanel
 * This creates an editor panel for editing LWLink's
 *
 **/
 
 public class LinkToolPanel extends LWCToolPanel
 {
     private ColorMenuButton mLinkColorButton;
     private AbstractButton mArrowStartButton;
     private AbstractButton mArrowEndButton;
 	
     protected void buildBox()
     {
         mArrowStartButton = new VueButton.Toggle("link.button.arrow.start", this);
         mArrowEndButton = new VueButton.Toggle("link.button.arrow.end", this);
         
         JLabel label = new JLabel("   Link: ");
         label.setFont(VueConstants.FONT_SMALL);
         getBox().add(label);
         
         getBox().add(mArrowStartButton);
         getBox().add(mArrowEndButton);
         
         getBox().add(mStrokeColorButton);
         getBox().add(mStrokeButton);
         getBox().add(mFontPanel);
         getBox().add(mTextColorButton);
    }
     
         /*
         mArrowStartButton = new JToggleButton();
         mArrowStartButton.setIcon(VueResources.getImageIcon( "arrowStartOffIcon") );
         mArrowStartButton.setSelectedIcon(VueResources.getImageIcon("arrowStartOnIcon") );
         mArrowStartButton.setMargin(ButtonInsets);
         mArrowStartButton.setBorderPainted(false);
         mArrowStartButton.addActionListener(this);
		
         mArrowEndButton = new JToggleButton();
         mArrowEndButton.setIcon(VueResources.getImageIcon( "arrowEndOffIcon") );
         mArrowEndButton.setSelectedIcon(VueResources.getImageIcon("arrowEndOnIcon") );
         mArrowEndButton.setMargin(ButtonInsets);
         mArrowEndButton.setBorderPainted(false);
         mArrowEndButton.addActionListener(this);
         */

         //----

         /*
         Color [] linkColors = VueResources.getColorArray( "linkColorValues");
         String [] linkColorNames = VueResources.getStringArray( "linkColorNames");
         mLinkColorButton = new ColorMenuButton( linkColors, linkColorNames, true);
         ImageIcon fillIcon = VueResources.getImageIcon("linkFillIcon");
         BlobIcon fillBlob = new BlobIcon();
         fillBlob.setOverlay( fillIcon );
         mLinkColorButton.setIcon(fillBlob);
         mLinkColorButton.setPropertyName(LWKey.StrokeColor);
         mLinkColorButton.setBorderPainted(false);
         mLinkColorButton.setMargin(ButtonInsets);
         mLinkColorButton.addPropertyChangeListener( this);
         */

 	
     
     protected void initDefaultState() {
         LWLink link = LWLink.setDefaults(new LWLink());
         mDefaultState = VueBeans.getState(link);
     }
 	
 	
     void loadValues(Object pValue) {
         super.loadValues(pValue);
         if (!(pValue instanceof LWLink))
             return;
 		
         setIgnorePropertyChangeEvents(true);

         // ick: we're relying on the side-effect of mState having been set in parent call
         // TODO: either force everything to use a loadValues(state), or have a loadValues(LWComponent)
         // and loadValues(VueBeanState), or perhaps get rid of the hairy-ass VueBeanState crap
         // alltogether.
         if (mState.hasProperty(LWKey.LinkArrows)) {
             int arrowState = mState.getIntValue(LWKey.LinkArrows);
             mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
               mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
         } else
             System.out.println(this + " missing arrow state property in state");
 		
         /*
         if (mState.hasProperty( LWKey.StrokeColor) ) {
             Color c = (Color) mState.getPropertyValue(LWKey.StrokeColor);
             mLinkColorButton.setColor(c);
         } else 
             debug("missing link stroke color property.");
         */

         setIgnorePropertyChangeEvents(false);
     }
     
 	
     public void actionPerformed( ActionEvent pEvent) {
         Object source = pEvent.getSource();
 		
         // the arrow on/off buttons where it?
         if( source instanceof JToggleButton ) {
             JToggleButton button = (JToggleButton) source;
             if( (button == mArrowStartButton) || (button == mArrowEndButton) ) {
                 int oldState = -1;
                 int state = LWLink.ARROW_NONE;
                 if( mArrowStartButton.isSelected() ) {
                     state += LWLink.ARROW_EP1;
                 }
                 if( mArrowEndButton.isSelected() ) {
                     state += LWLink.ARROW_EP2;
                 }
                 Integer newValue = new Integer( state);
                 Integer oldValue = new Integer( oldState);
                 PropertyChangeEvent event = new PropertyChangeEvent( button, LWKey.LinkArrows, oldValue, newValue);
                 propertyChange( event);
             }
         }
     }
 	
 	
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.initUI(true);
        VueUtil.displayComponent(new LinkToolPanel());
    }
     
 }
