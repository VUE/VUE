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
 	
     public LinkToolPanel() {
         //new Throwable("LINKTOOLPANEL CREATED").printStackTrace();
         
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

         Color bakColor = VueResources.getColor("toolbar.background");
         Box box = Box.createHorizontalBox();
         setBackground( bakColor);
         setBorder(new javax.swing.border.EmptyBorder(2,1,2,1));//t,l,b,r
                
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
         mLinkColorButton.setBackground( bakColor);
         mLinkColorButton.addPropertyChangeListener( this);
 		
 		
         mArrowStartButton = new JToggleButton();
         mArrowStartButton.setIcon( VueResources.getImageIcon( "arrowStartOffIcon") );
         mArrowStartButton.setSelectedIcon( VueResources.getImageIcon("arrowStartOnIcon") );
         mArrowStartButton.setBackground( bakColor);
         mArrowStartButton.setBorderPainted(false);
         mArrowStartButton.setMargin(ButtonInsets);
         mArrowStartButton.addActionListener( this);
		
         mArrowEndButton = new JToggleButton();
         mArrowEndButton.setIcon( VueResources.getImageIcon( "arrowEndOffIcon") );
         mArrowEndButton.setSelectedIcon( VueResources.getImageIcon("arrowEndOnIcon") );
         mArrowEndButton.addActionListener( this);
         mArrowEndButton.setMargin(ButtonInsets);
         mArrowEndButton.setBorderPainted(false);
		
 		
         box.add( mLinkColorButton);
         box.add( mArrowStartButton);
         box.add( mArrowEndButton);
 		
         this.add( box);
 	
         initDefaultState();
     }
 	
 	
    protected javax.swing.JComponent getLabelComponent() {
        javax.swing.JComponent label = new javax.swing.JLabel("   Link:");
        label.setFont(VueConstants.FONT_SMALL);
        return label;
    }
     ////////////////
     // Methods
     /////////////////
 	
 	
     protected void initDefaultState() {
         LWLink link = LWLink.setDefaults(new LWLink());
         mDefaultState = VueBeans.getState(link);
     }
 	
 	
     /**
      * setValue
      * Generic property editor access
      **/
     public void setValue( Object pValue) {
         //System.out.println("LinkToolPanel setValue " + pValue);
         VueBeanState state = null;
         if( pValue instanceof LWComponent) {
             if (!(pValue instanceof LWLink))
                 return;
             state = VueBeans.getState( pValue);
             //System.out.println("got state " + state);
         }
 			
         mState = state;
 		
         // stop listening while we change the values
         enablePropertyChangeListeners( false);

         if( mState.hasProperty( LWKey.Font) ) {
             Font font = (Font) state.getPropertyValue( LWKey.Font);
             mFontPanel.setValue( font);
         }
         else {
             debug("missing font property in state");
         }
 		
         if( mState.hasProperty( LWKey.StrokeWidth) ) {
             Float weight = (Float) state.getPropertyValue( LWKey.StrokeWidth);
             mStrokeButton.setStroke( weight.floatValue() );
         }
         else {
             debug("missing stroke weight proeprty in state.");
         }
 			
         if( mState.hasProperty( LWKey.LinkArrows) ) {
             Integer arrows = (Integer) state.getPropertyValue( LWKey.LinkArrows );
             int arrowState = arrows.intValue();
             mArrowStartButton.setSelected( (arrowState & LWLink.ARROW_EP1) == LWLink.ARROW_EP1);
             mArrowEndButton.setSelected( (arrowState & LWLink.ARROW_EP2) == LWLink.ARROW_EP2);
         }
         else {
             debug("missing arrow state property in state");
         }
 		
         if( mState.hasProperty( LWKey.StrokeColor) ) {
             Color fill = (Color) state.getPropertyValue( LWKey.StrokeColor);
             mLinkColorButton.setColor( fill);
         }
         else {
             debug(" missing link stroke color property.");
         }
 		
         if( mState.hasProperty( LWKey.TextColor) ) {
             Color text = (Color) state.getPropertyValue( LWKey.TextColor);
             mTextColorButton.setColor( text);
         }
         else {
             debug("missing text color property in state.");
         }
         // all done setting... start listening  again.
         enablePropertyChangeListeners( true);
     }

 	
     protected void enablePropertyChangeListeners(boolean pEnable) {
         super.enablePropertyChangeListeners(pEnable);
         
         if (pEnable) {	
             mLinkColorButton.addPropertyChangeListener(this);
             mArrowStartButton.addActionListener(this);
             mArrowEndButton.addActionListener(this);
         } else {
             mLinkColorButton.removePropertyChangeListener(this);
             mArrowStartButton.removeActionListener(this);
             mArrowEndButton.removeActionListener(this);
	 }
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
