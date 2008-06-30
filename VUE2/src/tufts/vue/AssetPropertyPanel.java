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
	 * AssetPropertyPanel
	 * This is the panel for displaying Resource info
	 *
	 **/
	public class AssetPropertyPanel extends ExpandPanel {
	
		Resource mResource = null;
		Box mAssetBox = null;
		PropertyPanel mPropPanel = null;
		
		public AssetPropertyPanel() {
			super( "Resources", null, true);
			setLayout( new BorderLayout() );
			
			mAssetBox = Box.createVerticalBox();
			mPropPanel = new PropertyPanel();
			mAssetBox.add( mPropPanel);
			setBodyComponent( mAssetBox);
			
		}
		
		
		
		public void setResource( Resource pResource ) {
			if( mResource != pResource) {
				mResource = pResource;
				updateDisplay();
				}
		}

		public void updateDisplay( ) {
			if( mResource != null) {
                            /**
				String [] names = mResource.getPropertyNames();
				if( names != null) {
					for( int i=0; i< names.length; i++) {
						Object value = mResource.getPropertyValue( names[i]);
						if( value != null) {
							mPropPanel.addProperty( names[i], value.toString());
							}
						}
					}
                             */
				}
			else {
				mPropPanel.removeAll();
				}
		}
		
	
}
	
	
