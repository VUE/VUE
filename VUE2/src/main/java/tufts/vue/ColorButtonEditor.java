/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
* ColorButtonEditor
*
* This class provides a color property editor button to pick a 
* color
*
*
* @author csb
* @version 1.0
**/
public class ColorButtonEditor extends JButton implements ActionListener {

	
	static final String kDefaultName = "Color";
	static final String kDefaultTitle = "Choose Color";
	
	/** The currently selected palette item--if any **/
	private Color mColor = Color.black;
		
	private String mTitle = VueResources.getString("dialog.selectcolor.title");
	
	private String mName = "color";
	/**
	 * Constructor
	 *
	 * Creates a new PaletteButton with no menus
	 *
	 **/
	public ColorButtonEditor() {
		this( Color.black);
	}
	
	public ColorButtonEditor( Color pColor) {
		this( pColor, null);
	}
	

	public ColorButtonEditor( Color pColor, String pName) {
		this( pColor, pName, kDefaultTitle);
	}
	
	public ColorButtonEditor( Color pColor, String pName, String pTitle) {
		super();
		mColor = pColor;
		setBackground( pColor);
		mName = pName;
		mTitle = pTitle;
		addActionListener( this);
	}
	
	
	
	public void setColor( Color pColor) {
		mColor = pColor;
		mColor = pColor;
		this.setBackground( pColor);
		repaint();
	}
	
	
	
	/**
	 * setValue 
	 * Hook for PropertyEditor
	 **/
	public void setValue( Object pValue) {
		if( pValue instanceof Color) 
			setColor( (Color) pValue);
	}
	


	/**
	 * fireColorChanged
	 * Notifies listeners that a new color has been selected.
	 *
	 * @param Color the new color
	 **/
	 protected void fireColorChanged( Color oldCOlor, Color newColor) {
	 	// tell someone
	 }
	
	/**
	 * actionPerformed
	 * This method handles teh button press action and opens
	 * up a color picker dialog.  
	 *
	 **/
	public void actionPerformed( ActionEvent pEvent) {
	
		Color newColor = null;
		Color oldColor = mColor;
		//try {
			
			newColor = JColorChooser.showDialog( this, mTitle, mColor );
			if( (newColor != null) && ( mColor.equals( newColor) ) ) {
				setColor( newColor);
				fireColorChanged( oldColor,  newColor);
				
				}
		//} catch (HeadlessException e) {
		
		//}
		
	}

	/**
	 * Paint
	 *
	 * Override if drawing boring button without image
	 **/
	public void paint( Graphics pGraphics) {
		
		if( false ) {
			Rectangle bounds = this.getBounds();
			Color old = pGraphics.getColor();
			pGraphics.setColor( mColor);
			pGraphics.fillRect( 0, 0, (int) bounds.getWidth(), (int) bounds.getHeight());
			pGraphics.setColor( old);
			}
		else {
			super.paint( pGraphics);
			}
	}
	
}  // end of class






