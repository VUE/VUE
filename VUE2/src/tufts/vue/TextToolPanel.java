package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.beans.*;



/**
 * TextToolPanel
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
 
 public class TextToolPanel extends JPanel implements  PropertyChangeListener {
 
 	////////////
 	// Statics
 	/////////////
 	
 
 	
	///////////
	// Fields 
	////////////
	
	
 	 	
 	
 	/** Text color menu editor **/
 	ColorMenuButton mTextColorButton = null;
 	 	
 	/** the Font selection combo box **/
 	FontEditorPanel mFontPanel = null;
 	
 	
	VueBeanState mDefaultState = null;
	
	Object mState = null;
	
	 	 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
 	public TextToolPanel() {
 		
  		Color bakColor = VueResources.getColor("toolbar.background");
 		Box box = Box.createHorizontalBox();
 		setBackground( bakColor);
 		
 		
 		
 		 Color [] textColors = VueResources.getColorArray( "textColorValues");
 		 String [] textColorNames = VueResources.getStringArray( "textColorNames");
 		 mTextColorButton = new ColorMenuButton( textColors, textColorNames, true);
 		ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
		BlobIcon textBlob = new BlobIcon();
		textBlob.setOverlay( textIcon );
		textBlob.setColor( VueResources.getColor("defaultTextColor"));
		mTextColorButton.setIcon(textBlob);
 		mTextColorButton.setPropertyName( VueLWCPropertyMapper.kTextColor);
 		mTextColorButton.setColor( VueResources.getColor( "defaultTextColor"));
 		mTextColorButton.setBackground( bakColor );
 		mTextColorButton.addPropertyChangeListener( this);
 		
 		mFontPanel = new FontEditorPanel();
 		mFontPanel.setBackground( bakColor);
 		mFontPanel.setPropertyName( VueLWCPropertyMapper.kFont );
		mFontPanel.addPropertyChangeListener( this);

 		
 		box.add(mTextColorButton);
 		box.add( mFontPanel);
 		
 		this.add( box);
 		
 		initDefaultState();
 	}
 	
 	
 	////////////////
 	// Methods
 	/////////////////
 	
 	private void initDefaultState() {
 	
 		LWNode node = new LWNode();
 		node.setIsTextNode( true);
 		mDefaultState = VueBeans.getState( node);
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
 		else
 		if( pValue instanceof VueBeanState ) {
 			state = (VueBeanState) pValue;
 			}
 		
 		if( state == null)  {
 			state = mDefaultState;
 			}
 		
 		mState = state;
 		
 		Font font = (Font) state.getPropertyValue( VueLWCPropertyMapper.kFont);
 		mFontPanel.setValue( font);
 		
 		
 		
 		Color text = (Color) state.getPropertyValue( VueLWCPropertyMapper.kTextColor);
 		mTextColorButton.setColor( text);
 	}
 	
 	/**
 	 * getValue
 	 *
 	 **/
 	public Object getValue() {
 		return mState;
 	}
 	
 	
 	public void propertyChange( PropertyChangeEvent pEvent) {
 		//System.out.println("Text property chaged: "+pEvent.getPropertyName());
  		String name = pEvent.getPropertyName();
  		if( !name.equals("ancestor") ) {
	  		
	  		VueBeans.setPropertyValueForLWSelection( VUE.ModelSelection, name, pEvent.getNewValue() );
  			
  			}
  	}
 	
 	
 	
 	
 }