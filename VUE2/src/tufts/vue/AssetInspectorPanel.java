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
			mAssetBox.add( new JLabel( "Asset Display Offline") );
			
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
	
	
