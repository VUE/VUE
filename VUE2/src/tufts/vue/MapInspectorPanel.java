/*******
**  MapInspectorPanel.java
**
**
*********/

package tufts.vue;


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
* ObjectInspectorPanel
*
* The Object  Inspector Panel!
*
\**/
public class MapInspectorPanel  extends JPanel 
			implements LWSelection.Listener
{


	/////////////
	// Fields
	//////////////
	
	/** The tabbed panel **/
	JTabbedPane mTabbedPane = null;
	
	/** The map we are inspecting **/
	LWMap mMap = null;
	
	/** info tab panel **/
	InfoPanel mInfoPanel = null;
	
	/** pathways panel **/
	PathwayPanel mPathPanel = null;
	
	/** filter panel **/
	FilterPanel mFilterPanel = null;
	
	///////////////////
	// Constructors
	////////////////////
	
	public MapInspectorPanel() {
		super();
		
		setMinimumSize( new Dimension( 150,200) );
		setLayout( new BorderLayout() );
		setBorder( new EmptyBorder( 5,5,5,5) );
		mTabbedPane = new JTabbedPane();
		VueResources.initComponent( mTabbedPane, "tabPane");
		
		
		mInfoPanel = new InfoPanel();
		mPathPanel = new PathwayPanel();
		mFilterPanel = new FilterPanel();
		
		mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
		mTabbedPane.addTab( mPathPanel.getName(),  mPathPanel);
		mTabbedPane.addTab( mFilterPanel.getName(), mFilterPanel);
	
		add( BorderLayout.CENTER, mTabbedPane );
		validate();
		show();
	}
	
	
	
	////////////////////
	// Methods
	///////////////////
	
	
	/**
	 * setMap
	 * Sets the LWMap component and updates teh display
	 *
	 * @param pMap - the LWMap to inspect
	 **/
	public void setMap( LWMap pMap) {
		
		// if we have a change in maps... 
		if( pMap != mMap) {
			mMap = pMap;
			updatePanels();
			}
	}
	
	
	/**
	 * updatePanels
	 * This method updates the panel's content pased on the selected
	 * Map
	 *
	 **/
	public void updatePanels() {
		
		mInfoPanel.updatePanel( mMap);
		mPathPanel.updatePanel( mMap);
		mFilterPanel.updatePanel( mMap);
	}
	
	//////////////////////
	// OVerrides
	//////////////////////


	public Dimension getPreferredSize()  {
		Dimension size =  super.getPreferredSize();
		if( size.getWidth() < 200 ) {
			size.setSize( 200, size.getHeight() );
			}
		if( size.getHeight() < 250 ) {
			size.setSize( size.getWidth(), 250);
			}
		return size;
	}

	
	
	/////////////
		// LWSelection.Listener Interface Implementation
		/////////
		public void selectionChanged( LWSelection pSelection) {
			
			LWMap map = VUE.getActiveMap();
			setMap( map);
		}
	
	
	
	
	/////////////////
	// Inner Classes
	////////////////////
	
	
	
	
	/**
	 * InfoPanel
	 * This is the tab panel for displaying Map Info
	 *
	 **/
	public class InfoPanel extends JPanel {
	
		JScrollPane mInfoScrollPane = null;
		
		Box mInfoBox = null;
		
		public InfoPanel() {
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder(4,4,4,4) );
			
			mInfoBox = Box.createVerticalBox();
		
			mInfoScrollPane = new JScrollPane();
			mInfoScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mInfoScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mInfoScrollPane.setLocation(new Point(8, 9));
			mInfoScrollPane.setVisible(true);
			mInfoScrollPane.getViewport().add( mInfoBox);
		
			add( BorderLayout.CENTER, mInfoScrollPane );
		}
		
		
		public String getName() {
			return VueResources.getString("mapInfoTabName") ;
		}
		
		
		
		/**
		 * updatePanel
		 * Updates the Map info panel
		 * @param LWMap the map
		 **/
		public void updatePanel( LWMap pMap) {
			// update the display
		}
	}
	
	
	/**
	 * This is the Pathway Panel for the Map Inspector
	 *
	 **/
	public class PathwayPanel extends JPanel {
		
		/** the path scroll pane **/
		JScrollPane mPathScrollPane = null;
		
		/** the path display area **/
		JPanel mPathDisplay = null;
		
		
		
		/**
		 * PathwayPanel
		 * Constructs a pathway panel
		 **/
		public PathwayPanel() {
			
			mPathDisplay = new JPanel();
			mPathDisplay.add( new JLabel("Pathway offline") );
			
			mPathScrollPane = new JScrollPane();
			mPathScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mPathScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mPathScrollPane.setLocation(new Point(8, 9));
			mPathScrollPane.setVisible(true);
			mPathScrollPane.getViewport().add( mPathDisplay);
		}
		
		
		public String getName() {
			return VueResources.getString("mapPathwayTabName") ;
		}
		
		
		/**
		 * updatePanel
		 * This updates the Panel display based on a new LWMap
		 *
		 **/
		 public void updatePanel( LWMap pMap) {
		 // update display based on the LWMap
		 }
	}
	
	
	/**
	 * FilterPanel
	 * This is the Map Filtering Panel for the Map Inspector
	 *
	 **/
	public class FilterPanel extends JPanel {
		
		/** the scroll pane **/
		JScrollPane mFilterScrollPane = null;
		
		/** the vertical box container **/
		Box mFilterBox  = null;
		
		/** the filter button **/
		JButton mFilterButton = null;
		
		Box mButtonBox = null;
		
		/** the more button **/
		JButton mMoreButton = null;
		
		/** the fewer button **/
		JButton mFewerButton = null;
		
		public FilterPanel() {
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder( 4,4,4,4) );
			
			mFilterButton = new JButton( "Filter");
			mMoreButton = new JButton("More");
			mFewerButton = new JButton("Fewer");
			mButtonBox = Box.createHorizontalBox();
			
			mButtonBox.add( mMoreButton);
			mButtonBox.add( mFewerButton);
			
			mFilterBox = Box.createVerticalBox();
			mFilterBox.add( new JLabel("Filters unavailable at this time.") );
			mFilterScrollPane = new JScrollPane();
			
			mFilterBox.add( new JLabel(" Create a filter:") );
			
			mFilterBox.add( new JLabel("[field]  [contains]  [value]") );
			mFilterBox.add( mButtonBox);
			mFilterBox.add( mFilterButton);
			
			mFilterScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mFilterScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mFilterScrollPane.setLocation(new Point(8, 9));
			mFilterScrollPane.setVisible(true);
			mFilterScrollPane.getViewport().add( mFilterBox);
			
			add( BorderLayout.CENTER, mFilterScrollPane );
		}
		
		
		public String getName() {
			return VueResources.getString("mapFilterTabName") ;
		}
		
		
		/**
		 * updatePanel
		 * Updates teh panel based on the passed in LWMap
		 * @param the LWMap
		 **/
		public void updatePanel( LWMap pMap) {
			// use the LWMap and update self
		}
	
	}
}





