package tufts.vue;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.beans.VueLWCPropertyMapper;



/**
 * FontEditorPanel
 * This creates a font editor panel for editing fonts in the UI
 *
 **/
 
 public class FontEditorPanel extends JPanel implements ActionListener, VueConstants {
 
 	////////////
 	// Statics
 	/////////////
 	
 	static Icon sItalicOn = VueResources.getImageIcon("italicOnIcon");
 	static Icon sItalicOff = VueResources.getImageIcon("italicOffIcon");
 	static Icon sBoldOn = VueResources.getImageIcon("boldOnIcon");
 	static Icon sBoldOff = VueResources.getImageIcon("boldOffIcon");
	///////////
	// Fields 
	////////////
	
	
 	/** the font list **/
 	static private String[] sFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
 
 	
 	/** Text color editor button **/
 	ColorMenuButton mColorButton = null;
 	
 	/** the Font selection combo box **/
 	JComboBox mFontCombo = null;
 	
 	/** the size edit area **/
 	NumericField mSizeField = null;
 	
 	/** bold botton **/
 	JToggleButton mBoldButton = null;
 	
 	/** italic button **/
 	JToggleButton mItalicButton = null;
 	
 	/** the size **/
 	int mSize = 14;
 	
 	/** the property name **/
 	String mPropertyName = VueLWCPropertyMapper.kFont;
 	
 	/** the font **/
 	Font mFont = null;
 	
 	
 	/////////////
 	// Constructors
 	//////////////////
 	
 	public FontEditorPanel() {
 		
 		Box box = Box.createHorizontalBox();
 		
 		mFontCombo = new JComboBox( sFontNames);
 		mFontCombo.addActionListener( this );
 		
 		mSizeField = new NumericField( NumericField.POSITIVE_INTEGER, 2 );
 		mSizeField.addActionListener( this);
 		
 		mBoldButton = new JToggleButton();
 		mBoldButton.setSelectedIcon( sBoldOn);
 		mBoldButton.setIcon( sBoldOff);
 		mBoldButton.addActionListener( this);
 		
 		mItalicButton = new JToggleButton();
 		mItalicButton.setSelectedIcon( sItalicOn );
 		mItalicButton.setIcon( sItalicOff);
 		mItalicButton.addActionListener( this);
 		
 		box.add( mFontCombo);
 		box.add( mSizeField);
 		box.add(mBoldButton);
 		box.add( mItalicButton);
 		this.add( box);
 	
 		setFontValue(  FONT_DEFAULT);
 	}
 	
 	
 	////////////////
 	// Methods
 	/////////////////
 	
 	
 	
 	public void setPropertyName( String pName) {
 		mPropertyName = pName;
 	}
 	
 	public String getPropertyName() {
 		return mPropertyName;
 	}
 	
 	/**
 	 * getFontValue()
 	 **/
 	public Font getFontValue() {
 		return mFont;
 	}
 	
 	/**
 	 * setFontValue()
 	 **/
 	public void setFontValue( Font pFont) {
 		setValue( pFont);
 	
 	}
 	
 	
 	/**
 	 * setValue
 	 * Generic property editor access
 	 **/
 	public void setValue( Object pValue) {
 		
 		if( pValue instanceof Font) {
 			Font font = (Font) pValue;
 			mFontCombo.setSelectedItem( font.getFontName() );
 			mItalicButton.setSelected( (Font.ITALIC & font.getStyle()) == Font.ITALIC );
 			mBoldButton.setSelected( (Font.BOLD & font.getStyle()) == Font.BOLD );
 			mSizeField.setValue( font.getSize() );
 			
 			}
 	
 	}
 	
 	public void fireFontChanged( Font pOld, Font pNew) {
 		
 		PropertyChangeListener [] listeners = getPropertyChangeListeners() ;
 		System.out.println("Fire font changed!");
 		PropertyChangeEvent  event = new PropertyChangeEvent( this, getPropertyName(), pOld, pNew);
 		if( listeners != null) {
 			for( int i=0; i<listeners.length; i++) {
 				listeners[i].propertyChange( event);
 				}
 			}
 	}
 	
 	public void actionPerformed( ActionEvent pEvent) {
 		Font old = mFont;
 		Font font = makeFont();
 		if( (old == null) || ( !old.equals( font)) ) {
 			fireFontChanged( old, font);
 			}
 	}
 	
 	/**
 	 * makeFont
 	 *
 	 **/
 	 public Font makeFont() {
 	 
 	 	String name = (String) mFontCombo.getSelectedItem() ;
 	 	
 	 	int style = Font.PLAIN;
 	 	if( mItalicButton.isSelected() ) {
 	 		style = style + Font.ITALIC;
 	 		}
 	 	if( mBoldButton.isSelected() ) {
 	 		style = style + Font.BOLD;
 	 		}
 	 		int size = (int) mSizeField.getValue();
 	 		
 	 	System.out.println("-- making new font: "+name+" style:"+style+" size:"+size);
 	 	Font font = new Font( name, style, size);
 	 	return font;
 	 }
 	
 	
 }