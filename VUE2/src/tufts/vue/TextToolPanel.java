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
 	
 	
	Object mDefualtState = null;
	
	Object mState = null;
	
	 	 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
 	public TextToolPanel() {
 		
 		Box box = Box.createHorizontalBox();
 		
 		
 		
 		
 		 Color [] textColors = VueResources.getColorArray( "textColorValues");
 		 mTextColorButton = new ColorMenuButton( textColors, null, true);
 		ImageIcon textIcon = VueResources.getImageIcon("textColorIcon");
		BlobIcon textBlob = new BlobIcon();
		textBlob.setOverlay( textIcon );
		mTextColorButton.setIcon(textBlob);
 		mTextColorButton.setPropertyName("nodeTextColor");
 		mTextColorButton.addPropertyChangeListener( this);
 		
 		mFontPanel = new FontEditorPanel();
 		mFontPanel.addPropertyChangeListener( this);

 		
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
 		/**
 		if( pValue instanceof LWNode) {
 			
 			}
 		**/
 	
 	}
 	
 	/**
 	 * getValue
 	 *
 	 **/
 	public Object getValue() {
 		return mState;
 	}
 	
 	
 	public void propertyChange( PropertyChangeEvent pEvent) {
 		System.out.println("Link property chaged: "+pEvent.getPropertyName());
  	}
 	
 	
 	
 	
 }