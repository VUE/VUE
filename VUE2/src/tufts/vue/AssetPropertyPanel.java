/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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
	
	
