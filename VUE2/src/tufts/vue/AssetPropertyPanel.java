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
	
	
