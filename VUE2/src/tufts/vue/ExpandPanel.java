/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

/*******
**  ObjectInspectorPanel
**
**
*********/

package tufts.vue;


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


	/**
	 * ExpandPanel
	 * This is the panel for dis\playing Resource info
	 *
	 **/
	public class ExpandPanel extends JPanel implements ActionListener{
	
		
		//////////////
		// Fields
		/////////////
		
		
		/** the hide/show component **/
		Component mComponent = null;
		
		/** the toggle button **/
		JToggleButton mExpandButton = null;
		
		/** the title display **/
		JLabel mTitle = null;
		
		
		//////////////////
		// Constructors
		//////////////////
		
		/**
		 * 
		 **/
		public ExpandPanel( String pTitle, Component pComponent, boolean pShow) {
			
			setLayout( new BorderLayout() );
			
			mComponent = pComponent;
			mTitle = new JLabel();
			mTitle.setText( pTitle);
			
			JPanel titlePanel = new JPanel();
			titlePanel.setLayout( new BorderLayout() );
			titlePanel.add( BorderLayout.WEST, mTitle);
			
			mExpandButton = new JToggleButton();
			mExpandButton.setIcon( VueResources.getImageIcon( "assetExpandOff" ));
			mExpandButton.setSelectedIcon( VueResources.getImageIcon("assetExapndOn") );
			mExpandButton.setSelected( pShow);
			mExpandButton.addActionListener( this);
			titlePanel.add( BorderLayout.EAST, mExpandButton);
			
			add( BorderLayout.NORTH, titlePanel);
			if( pShow) {
				if( mComponent  != null) {
					add( BorderLayout.CENTER, mComponent);
					}
				}
			
		}
		
		
		/**
		 * getTitle
		 * This returns the title string
		 * @return String the name
		 **/
		public String getTitle() {
			return mTitle.getText();
		}
		
		
		
		////////////////
		// Methods
		////////////////
		
		
		public void setTitle( String pTitle) {
			mTitle.setText( pTitle);
		}
		
		public void setIcon( Icon pIcon) {
			mTitle.setIcon( pIcon);
		}
		 
		
		
	public void setBodyComponent( Component pComponent) {
		if( isExpanded() ) {
			if( mComponent != null) {
				remove( mComponent);
				}
			}
		
		mComponent = pComponent;
		
		if( isExpanded() ) {
			if( mComponent != null) {
				add( BorderLayout.CENTER, mComponent);
				}
			validate();
			}
	}
	
	public boolean isExpanded() {
		return mExpandButton.isSelected();
	}
	
	
	public void setIsExapnded( boolean pExpand) {
		
			
		if( pExpand) {
			System.out.println("  trying to add and expand");
			add( BorderLayout.CENTER, mComponent);
			}
		else {
			System.out.println("  ...removing item (Hiding) ");
			remove( mComponent);
			}
		validate();
	}
	
	
	public void actionPerformed( ActionEvent pEvent) {
		Object source = pEvent.getSource();
		if( source == mExpandButton) {
			System.out.println("Expand Toggle!");
			setIsExapnded( mExpandButton.isSelected() );
			}
	}
}
	
	
