package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.beans.*;


/**
 * LinkToolPanel
 * This creates an editor panel for editing LWLink's
 *
 **/
 
 public class LinkToolPanel extends LWCToolPanel
 {
     /** link color button **/
     ColorMenuButton mLinkColorButton = null;

     /** arrow head toggle button **/
     JToggleButton mArrowStartButton = null;
 	
     /** arrow tail toggle button **/
     JToggleButton mArrowEndButton = null;
 	
     public LinkToolPanel()
     {
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
		
         getBox().add(mArrowEndButton, 0);
         getBox().add(mArrowStartButton, 0);
         
         JLabel label = new JLabel("   Link:");         
         label.setFont(VueConstants.FONT_SMALL);
         getBox().add(label, 0);
 	
         initDefaultState();
     }
 	
     protected void initDefaultState() {
         LWLink link = LWLink.setDefaults(new LWLink());
         mDefaultState = VueBeans.getState(link);
     }
 	
 	
     void loadValues(Object pValue) {
         super.loadValues(pValue);
         if (!(pValue instanceof LWLink))
             return;
 		
         setIgnorePropertyChangeEvents(true);

         if (mState.hasProperty(LWKey.LinkArrows)) {
             int arrowState = mState.getIntValue(LWKey.LinkArrows);
             mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
               mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
         } else
             debug("missing arrow state property in state");
 		
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
 	
 	
 	
 	boolean sDebug = true;
 	private void debug( String str) {
 		if( sDebug ) {
 			System.out.println("  LinkToolPanel - "+str);
 			}
 	}

    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.initUI(true);
        VueUtil.displayComponent(new LinkToolPanel());
    }
     
 }
