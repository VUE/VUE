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
	 * InfoPanel
	 * This is the tab panel for displaying Map Info
	 *
	 **/
	public class AssetInspectorPanel extends JPanel {
	
		JScrollPane mAssetScrollPane = null;
		Resource mAsset = null;
		Box mAssetBox = null;
		
		public AssetInspectorPanel() {
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder(4,4,4,4) );
			
			mAssetBox = Box.createVerticalBox();
			mAssetBox.add( new JLabel( VueResources.getString("jlabel.assetdisplayoffline")) );
			
			mAssetScrollPane = new JScrollPane();
			mAssetScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mAssetScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mAssetScrollPane.setLocation(new Point(8, 9));
			mAssetScrollPane.setVisible(true);
			mAssetScrollPane.getViewport().add( mAssetBox);
		
			add( BorderLayout.CENTER, mAssetScrollPane );
		}
		
		
		/**
		 * getName
		 * This returns the display name of the panel
		 * @return String the name
		 **/
		public String getName() {
			String name = VueResources.getString( "assetInspectorName");
			if( name == null) {
				name = super.getName();
				}
			return name;
		}
		 
		
		
		/**
		 * updatePanel
		 * Updates the Map info panel
		 * @param LWMap the map
		 **/
		public void updatePanel( Resource pAsset) {
			// update the display
		}
	}
	
	
