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
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
 
 public class LinkToolPanel extends JPanel implements ActionListener, PropertyChangeListener {
 
 	////////////
 	// Statics
 	/////////////
 	
 	private static float [] sStrokeValues = { 1,2,3,4,5,6};
 	private static String [] sStrokeMenuLabels = {  "1 pixel",
 											   "2 pixels",
 											   "3 pixels",
 											   "4 pixels",
 											   "5 pixels",
 											   "6 pixels"  };
 											   
 // End of array list
 
 	
	///////////
	// Fields 
	////////////
	
	
 	
 	/** link color button **/
 	ColorMenuButton mLinkColorButton = null;
 	
 	
 	/** Text color menu editor **/
 	ColorMenuButton mTextColorButton = null;
 	
 	/** stroke size selector menu **/
 	StrokeMenuButton mStrokeButton = null;
 	
 	/** Arrow Start toggle button **/
 	JToggleButton mArrowStartButton = null;
 	
 	/** end arrow button **/
 	JToggleButton mArrowEndButton = null;
 	
 	/** the Font selection combo box **/
 	FontEditorPanel mFontPanel = null;
 	
 	
	VueBeanState mDefaultState = null;
	
	VueBeanState mState = null;
	
	 	 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
 	public LinkToolPanel() {
 		
  		Color bakColor = VueResources.getColor("toolbar.background");
		Box box = Box.createHorizontalBox();
 		setBackground( bakColor);
 		
 		Color [] linkColors = VueResources.getColorArray( "linkColorValues");
 		String [] linkColorNames = VueResources.getStringArray( "linkColorNames");
 		mLinkColorButton = new ColorMenuButton( linkColors, linkColorNames, true);
 		ImageIcon fillIcon = VueResources.getImageIcon("nodeFillIcon");
		BlobIcon fillBlob = new BlobIcon();
		fillBlob.setOverlay( fillIcon );
		mLinkColorButton.setIcon(fillBlob);
 		mLinkColorButton.setPropertyName( VueLWCPropertyMapper.kStrokeColor);
 		mLinkColorButton.setBorder(null);
 		mLinkColorButton.setBackground( bakColor);
 		mLinkColorButton.addPropertyChangeListener( this);
 		
 		
 		 Color [] textColors = VueResources.getColorArray( "textColorValues");
 		 String [] textColorNames = VueResources.getStringArray( "textColorNames");
 		 mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
 		ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
		if( textIcon == null) System.out.println("issing resource: textColorIcon");
		BlobIcon textBlob = new BlobIcon();
		textBlob.setOverlay( textIcon );
		mTextColorButton.setIcon(textBlob);
 		mTextColorButton.setPropertyName("nodeTextColor");
 		mTextColorButton.setBackground( bakColor);
 		mTextColorButton.addPropertyChangeListener( this);
 		
 		mFontPanel = new FontEditorPanel();
 		mFontPanel.addPropertyChangeListener( this);

 		
 		
		
		mArrowStartButton = new JToggleButton();
		mArrowStartButton.setIcon( VueResources.getImageIcon( "arrowStartOffIcon") );
		mArrowStartButton.setSelectedIcon( VueResources.getImageIcon("arrowStartOnIcon") );
		mArrowStartButton.setBackground( bakColor);
		mArrowStartButton.addActionListener( this);
		
		mArrowEndButton = new JToggleButton();
		mArrowEndButton.setIcon( VueResources.getImageIcon( "arrowEndOffIcon") );
		mArrowEndButton.setSelectedIcon( VueResources.getImageIcon("arrowEndOnIcon") );
		mArrowEndButton.addActionListener( this);
		
		mStrokeButton = new StrokeMenuButton( sStrokeValues, sStrokeMenuLabels, true, false);
		LineIcon lineIcon = new LineIcon( 16,12);
		mStrokeButton.setIcon( lineIcon);
		mStrokeButton.setStroke( (float) 1);
 		mStrokeButton.setPropertyName( VueLWCPropertyMapper.kStrokeWeight);
 		mStrokeButton.setBackground( bakColor);
 		mStrokeButton.addPropertyChangeListener( this );
 		
 		
 		box.add( mLinkColorButton);
 		box.add( mArrowStartButton);
 		box.add( mStrokeButton);
 		box.add( mArrowEndButton);
 		box.add(mTextColorButton);
 		box.add( mFontPanel);
 		
 		this.add( box);
 	
 		initDefaultState();
 	}
 	
 	
 	////////////////
 	// Methods
 	/////////////////
 	
 	
 	private void initDefaultState() {
 		LWLink  link = new LWLink();
 		mDefaultState = VueBeans.getState( link);
 	}
 	
 	
 	/**
 	 * setValue
 	 * Generic property editor access
 	 **/
 	public void setValue( Object pValue) {

 		VueBeanState state = null;
 		if( pValue instanceof LWComponent) {
 			state = VueBeans.getState( pValue);
 			}
 			
 			
 		mState = state;
 		
 		// stop listening while we change the values
 		enablePropertyListeners( false);
 		
 		if( mState.hasProperty( VueLWCPropertyMapper.kFont) ) {
	 		Font font = (Font) state.getPropertyValue( VueLWCPropertyMapper.kFont);
 			mFontPanel.setValue( font);
 			}
 		else {
 			debug("missing font property in state");
 			}
 		
 		if( mState.hasProperty( VueLWCPropertyMapper.kStrokeWeight) ) {
			Float weight = (Float) state.getPropertyValue( VueLWCPropertyMapper.kStrokeWeight);
			mStrokeButton.setStroke( weight.floatValue() );
 			}
 		else {
 			debug("missing stroke weight proeprty in state.");
 			}
 			
 		if( mState.hasProperty( VueLWCPropertyMapper.kLinkArrowState) ) {
 			Integer arrows = (Integer) state.getPropertyValue( VueLWCPropertyMapper.kLinkArrowState );
 			int arrowState = arrows.intValue();
 			mArrowStartButton.setSelected( (arrowState & LWLink.ARROW_EP1) == LWLink.ARROW_EP1);
 			mArrowEndButton.setSelected( (arrowState % LWLink.ARROW_EP2) == LWLink.ARROW_EP2);
 			}
 		else {
 			debug("missing arrow state property in state");
 			}
 		
 		if( mState.hasProperty( VueLWCPropertyMapper.kStrokeColor) ) {
 			Color fill = (Color) state.getPropertyValue( VueLWCPropertyMapper.kStrokeColor);
 			mLinkColorButton.setColor( fill);
 			}
 		else {
 			debug(" missing link stroke color property.");
 			}
 		
 		if( mState.hasProperty( VueLWCPropertyMapper.kTextColor) ) {
 			Color text = (Color) state.getPropertyValue( VueLWCPropertyMapper.kTextColor);
 			mTextColorButton.setColor( text);
 			}
 		else {
 			debug("missing text color property in state.");
 			}
 		// all done setting... start listening  again.
 		enablePropertyListeners( true);
 	}
 	
 	/**
 	 * getValue
 	 *
 	 **/
 	public VueBeanState getValue() {
 		return mState;
 	}
 	
 	
 	public void propertyChange( PropertyChangeEvent pEvent) {
 		
 		String name = pEvent.getPropertyName();
  		if( !name.equals("ancestor") ) {
	  		System.out.println("! Link Property Change: "+name);	
	  		VueBeans.setPropertyValueForLWSelection( VUE.ModelSelection, name, pEvent.getNewValue() );
  			
  			if( mState != null) {
  				mState.setPropertyValue( name, pEvent.getNewValue() );
  				}
  			if( mDefaultState != null) {
  				mDefaultState.setPropertyValue( name,  pEvent.getNewValue() );
  				}
  			}
  	}
  	
  	
	private void enablePropertyListeners( boolean pEnable) {
	
	 	if( pEnable) {	
	 		mLinkColorButton.addPropertyChangeListener( this);
	 		mTextColorButton.addPropertyChangeListener( this);
	 		mFontPanel.addPropertyChangeListener( this);
			mArrowStartButton.addActionListener( this);
			mArrowEndButton.addActionListener( this);
	 		mStrokeButton.addPropertyChangeListener( this );
			}
		else {
	 		mLinkColorButton.removePropertyChangeListener( this);
	 		mTextColorButton.removePropertyChangeListener( this);
	 		mFontPanel.removePropertyChangeListener( this);
			mArrowStartButton.removeActionListener( this);
			mArrowEndButton.removeActionListener( this);
	 		mStrokeButton.removePropertyChangeListener( this );
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
				PropertyChangeEvent event = new PropertyChangeEvent( button, VueLWCPropertyMapper.kLinkArrowState, oldValue, newValue);
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
 }