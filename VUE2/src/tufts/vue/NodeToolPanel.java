package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;

import tufts.vue.beans.*;



/**
 * NodeToolPanel
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
 
 public class NodeToolPanel extends JPanel implements ActionListener, PropertyChangeListener {
 
 	////////////
 	// Statics
 	/////////////
 	
 	private static float [] sStrokeValues = { 0,1,2,3,4,5,6};
 	private static String [] sStrokeMenuLabels = { "nonne",
 											   "1 pixel",
 											   "2 pixels",
 											   "3 pixels",
 											   "4 pixels",
 											   "5 pixels",
 											   "6 pixels"  };
 											   
 // End of array list
 
 	
	///////////
	// Fields 
	////////////
	
	
 	
 	/** fill button **/
 	ColorMenuButton mFillColorButton = null;
 	
 	/** stroke color editor button **/
 	ColorMenuButton mStrokeColorButton = null;
 	
 	/** Text color menu editor **/
 	ColorMenuButton mTextColorButton = null;
 	
 	/** stroke size selector menu **/
 	StrokeMenuButton mStrokeButton = null;
 	
 	/** the Font selection combo box **/
 	FontEditorPanel mFontPanel = null;
 	
 	
	Object mDefualtState = null;
	
	Object mState = null;
	
	 	 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
 	public NodeToolPanel() {
 		
 		Box box = Box.createHorizontalBox();
 		
 		
 		Color [] fillColors = VueResources.getColorArray( "fillColorValues");
 		mFillColorButton = new ColorMenuButton( fillColors, null, true);
 		ImageIcon fillIcon = VueResources.getImageIcon("nodeFillIcon");
		BlobIcon fillBlob = new BlobIcon();
		fillBlob.setOverlay( fillIcon );
		mFillColorButton.setIcon(fillBlob);
 		mFillColorButton.setPropertyName( VueLWCPropertyMapper.kFillColor);
 		mFillColorButton.setBorder(null);
 		mFillColorButton.addPropertyChangeListener( this);
 		
 		Color [] strokeColors = VueResources.getColorArray( "strokeColorValues");
 		mStrokeColorButton = new ColorMenuButton( strokeColors, null, true);
 		ImageIcon strokeIcon = VueResources.getImageIcon("nodeStrokeIcon");
		BlobIcon strokeBlob = new BlobIcon();
		strokeBlob.setOverlay( strokeIcon );
		mStrokeColorButton.setPropertyName( VueLWCPropertyMapper.kStrokeColor);
		mStrokeColorButton.setIcon( strokeBlob);
 		mStrokeColorButton.addPropertyChangeListener( this);
 		
 		 Color [] textColors = VueResources.getColorArray( "textColorValues");
 		 mTextColorButton = new ColorMenuButton( textColors, null, true);
 		ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
		if( textIcon == null) System.out.println("issing resource: textColorIcon");
		BlobIcon textBlob = new BlobIcon();
		textBlob.setOverlay( textIcon );
		mTextColorButton.setIcon(textBlob);
 		mTextColorButton.setPropertyName( VueLWCPropertyMapper.kTextColor);
 		mTextColorButton.addPropertyChangeListener( this);
 		
 		mFontPanel = new FontEditorPanel();
 		mFontPanel.setPropertyName( VueLWCPropertyMapper.kFont );
 		mFontPanel.addPropertyChangeListener( this);

 		
 		
		
		mStrokeButton = new StrokeMenuButton( sStrokeValues, sStrokeMenuLabels, true, false);
		LineIcon lineIcon = new LineIcon( 16,12);
		mStrokeButton.setIcon( lineIcon);
		mStrokeButton.setStroke( (float) 1);
 		mStrokeButton.setPropertyName( VueLWCPropertyMapper.kStrokeWeight);
 		mStrokeButton.addPropertyChangeListener( this );
 		
 		
 		box.add( mFillColorButton);
 		box.add( mStrokeColorButton);
 		box.add( mStrokeButton);
 		box.add(mTextColorButton);
 		box.add( mFontPanel);
 		
 		this.add( box);
 	}
 	
 	
 	////////////////
 	// Methods
 	/////////////////
 	
 	
 	
 	
 	/**
 	 * setValue
 	 * Generic property editor access
 	 **/
 	public void setValue( Object pValue) {
 		VueBeanState state = null;
 		if( pValue instanceof LWComponent) {
 			state = VueBeans.getState( pValue);
 			}
 		Font font = (Font) state.getPropertyValue( VueLWCPropertyMapper.kFont);
 		mFontPanel.setValue( font);
 		
 		Float weight = (Float) state.getPropertyValue( VueLWCPropertyMapper.kStrokeWeight);
 		mStrokeButton.setStroke( weight.floatValue() );
 		
 		Color fill = (Color) state.getPropertyValue( VueLWCPropertyMapper.kFillColor);
 		mFillColorButton.setColor( fill);
 		
 		Color stroke = (Color) state.getPropertyValue( VueLWCPropertyMapper.kStrokeColor);
 		mStrokeColorButton.setColor( stroke);
 		
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
 		System.out.println("Node property chaged: "+pEvent.getPropertyName());
  		String name = pEvent.getPropertyName();
  		if( !name.equals("ancestor") ) {
	  		
	  		VueBeans.setPropertyValueForLWSelection( VUE.ModelSelection, name, pEvent.getNewValue() );
  			
  			}
  	}
 	
 	public void actionPerformed( ActionEvent pEvent) {
 	
 	}
 	
 	
 	
 }