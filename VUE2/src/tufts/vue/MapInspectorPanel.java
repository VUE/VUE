/*******
**  MapInspectorPanel.java
**
**
*********/

package tufts.vue;


import java.io.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
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
	// Statics
	/////////////
	
	static public final int SELECT_ACTION = 0;
	static public final int FILTER_ACTION = 1;
	static public final int ANY_MODE = 0;
	static public final int ALL_MODE = 1;
	static public final int NOT_ANY_MODE = 2;
	static public final int NONE_MODE = 3;
	
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
	PathwayPane mPathPanel = null;
	
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
		mPathPanel = new PathwayPane();
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
		
		if( mMap == null) {
			//clear it
			}
		else {
			mInfoPanel.updatePanel( mMap);
			mPathPanel.updatePanel( mMap);
			mFilterPanel.updatePanel( mMap);
			}
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

	
	public void activatePathwayTab() {
		mTabbedPane.setSelectedComponent( mPathPanel);
	}
	
	public void activateInfoTab() {
		mTabbedPane.setSelectedComponent( mInfoPanel);
	}
	
	public void activateFilterTab() {
		mTabbedPane.setSelectedComponent( mFilterPanel);
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
	public class InfoPanel extends JPanel implements ActionListener, PropertyChangeListener {
	
		JScrollPane mInfoScrollPane = null;
		
		Box mInfoBox = null;
		
		JTextField mTitleEditor = null;
		JTextField mAuthorEditor = null;
		JLabel mDate = null;
		JLabel mLocation = null;
		
		PropertyPanel mPropPanel = null;
		
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
			
			mTitleEditor = new JTextField();
			
			mAuthorEditor = new JTextField();
			mDate = new JLabel();
			mLocation = new JLabel();
			mPropPanel  = new PropertyPanel();
			mPropPanel.addProperty( "Title:", mTitleEditor);
			mPropPanel.addProperty("Author:", mAuthorEditor);
			mPropPanel.addProperty("Date:", mDate);
			mPropPanel.addProperty("Location:",mLocation);
			mInfoBox.add( mPropPanel);
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
			mDate.setText( mMap.getDate() );
			mTitleEditor.setText( mMap.getLabel() );
			mAuthorEditor.setText( mMap.getAuthor() );
			File file = mMap.getFile() ;
			String path = "";
			if( file != null) {
				path = file.getPath();
				}
			mLocation.setText( path);
		}
		
		protected void saveInfo() {
			if( mMap != null) {
				mMap.setLabel( mTitleEditor.getText() );
				mMap.setAuthor(  mAuthorEditor.getText() );
				}
		}
		
		public void actionPerformed( ActionEvent pEvent) {
			Object source = pEvent.getSource();
			if( source == mTitleEditor) {
				saveInfo();
				}
			if( source == mAuthorEditor ) {
				saveInfo();
				}
		}
		
		public void propertyChange( PropertyChangeEvent pEvent) {
		
		}
		
	}
	
	
	/**
	 * This is the Pathway Panel for the Map Inspector
	 *
	 **/
	public class PathwayPane extends JPanel {
		
		/** the path scroll pane **/
		JScrollPane mPathScrollPane = null;
		
		/** the path display area **/
		//JPanel mPathDisplay = null;
		
		PathwayTab mPathDisplay = null;
		
		/**
		 * PathwayPane
		 * Constructs a pathway panel
		 **/
		public PathwayPane() {
			
                        setLayout(new BorderLayout());
			//mPathDisplay = new JPanel();
			//mPathDisplay.add( new JLabel("Pathway offline") );
			
                        mPathDisplay = new PathwayTab(VUE.frame);
                        
			mPathScrollPane = new JScrollPane();
			mPathScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mPathScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mPathScrollPane.setLocation(new Point(8, 9));
			mPathScrollPane.setVisible(true);
			mPathScrollPane.getViewport().add( mPathDisplay);
                        
                        add(mPathScrollPane, BorderLayout.CENTER);
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
                     
                        mPathDisplay.setPathwayManager(pMap.getPathwayManager());
		 // update display based on the LWMap
		 }
	}
	
	
	/**
	 * FilterPanel
	 * This is the Map Filtering Panel for the Map Inspector
	 *
	 **/
	public class FilterPanel extends JPanel implements ActionListener {
		
		/** the scroll pane **/
		JScrollPane mFilterScrollPane = null;
		
		
		/** the main filter panel **/
		JPanel mMainFilterPanel = null;
		
		/** the buttons **/
		JPanel mLowerPanel = null;
		
		JPanel mMoreFewerPanel = null;
		
		/**  the top part panel **/
		JPanel mUpperPanel = null;
		
		/** the vertical box container **/
		Box mFilterBox  = null;
		
		/** the filter button **/
		JButton mFilterButton = null;
		
		/** the stop filter button **/
		JButton mClearFilterButton = null;
				
		/** the more button **/
		JButton mMoreButton = null;
		
		/** the fewer button **/
		JButton mFewerButton = null;
		
		/** mode combo **/
		JComboBox mModeCombo = null;
		
		/** action combo Select or Hide **/
		JComboBox mActionCombo = null;
		
		JComboBox mAnyAllCombo = null;
		
		LWCFilter mFilter = null;
		Vector mStatementEditors = new Vector();
		
		
		
		////////////
		// Constructors
		////////////////
		
		/**
		 * FilterPanel Constructor
		 **/
		public FilterPanel() {
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder( 4,4,4,4) );
			
			mMainFilterPanel = new JPanel();
			mMainFilterPanel.setLayout( new BorderLayout() );
			mLowerPanel = new JPanel();
			mLowerPanel.setLayout( new BorderLayout() );
			mUpperPanel = new JPanel();
			mUpperPanel.setLayout( new BorderLayout() );
			
			mActionCombo = new JComboBox();
			mActionCombo.addItem("Select");
			mActionCombo.addItem("Show");
			
			mAnyAllCombo = new JComboBox();
			mAnyAllCombo.addItem("match any");
			mAnyAllCombo.addItem("match all");
			mAnyAllCombo.addItem("don't match any");
			mAnyAllCombo.addItem("match none");
			
			
		/*******
			mModeCombo = new JComboBox();
			mModeCombo.addItem("Hide objects on map that match ANY of:");
			mModeCombo.addItem("Hide objects on map that match ALL of:");
			mModeCombo.addItem("Select objects that match ANY of:");
			mModeCombo.addItem("Select objects that match ALL of:");
		******/
		
			mUpperPanel.add( BorderLayout.NORTH, new JLabel("Create a filter:"));
			Box topBox = Box.createHorizontalBox();
			topBox.add( mActionCombo);
			JLabel clause = new JLabel("map items that");
			topBox.add( clause);
			topBox.add( mAnyAllCombo);
			
			mUpperPanel.add( BorderLayout.SOUTH, topBox);
			
			mFilterButton = new JButton( "Apply Now");
			mClearFilterButton = new JButton("Show All");
			mMoreButton = new JButton("More");
			mFewerButton = new JButton("Fewer");
			mFewerButton.hide();
			
			mFilterButton.addActionListener( this);
			mClearFilterButton.addActionListener( this);
			mMoreButton.addActionListener( this);
			mFewerButton.addActionListener( this);
			
			
			Box moreBox = Box.createHorizontalBox();
			moreBox.add( mFewerButton);
			moreBox.add( mMoreButton);
			mMoreFewerPanel = new JPanel();
			mMoreFewerPanel.setLayout( new BorderLayout() );
			mMoreFewerPanel.add( BorderLayout.WEST, moreBox);
			//mLowerPanel.add( BorderLayout.NORTH, mMoreFewerPanel);
			

			JPanel abp = new JPanel();
			abp.setLayout( new BorderLayout() );
			Box abBox = Box.createHorizontalBox();
			abBox.add( mClearFilterButton);
			abBox.add( mFilterButton);
			abp.add( BorderLayout.EAST, abBox);
			mLowerPanel.add( BorderLayout.SOUTH, abp);
			
			
			mFilterBox = Box.createVerticalBox();
			mFilter = new LWCFilter();
			FilterStatementEditor fse = new FilterStatementEditor( mFilter, mFilter.getLogicalStatements()[0] );
			mStatementEditors.add( fse);
			mFilterBox.add( fse);
			mFilterBox.add( mMoreFewerPanel);
			
			mFilterScrollPane = new JScrollPane();
			
			
			
			mMainFilterPanel.add( BorderLayout.NORTH, mUpperPanel);
			mMainFilterPanel.add( BorderLayout.SOUTH, mLowerPanel);
			mMainFilterPanel.add( BorderLayout.CENTER, mFilterBox);
			
			mFilterScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			mFilterScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mFilterScrollPane.setLocation(new Point(8, 9));
			mFilterScrollPane.setVisible(true);
			mFilterScrollPane.getViewport().add( mMainFilterPanel);
			
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

			boolean hasMap = pMap != null;
			
			mFilterButton.enable(hasMap);
			mClearFilterButton.enable( hasMap);
			mMoreButton.enable( hasMap);
			mFewerButton.enable( hasMap);
			
			if( hasMap) {
				mFilter = pMap.getLWCFilter();
				}
			else {
				mFilter = new LWCFilter();
			
				}
			
			int val = SELECT_ACTION;
			if( mFilter.isFiltering() ) {
				val = FILTER_ACTION;
				}
			mActionCombo.setSelectedIndex( val);
			val = ANY_MODE;
			if( !mFilter.getIsAny() ) {
				val = ALL_MODE;
				}
			if( mFilter.isLogicalNot() ) {
				val += 2;
				}
			mAnyAllCombo.setSelectedIndex( val);
			
			buildFilterBox( pMap);
				
		}
		
		public LWCFilter makeNewFIlter() {
			LWCFilter filter = new LWCFilter();
			
			LWCFilter.LogicalStatement [] satements = new LWCFilter.LogicalStatement[ mStatementEditors.size() ];
		
			
			return filter;
		}
		
		
		public void buildFilterBox(  LWMap pMap) {
			
			LWCFilter filter = null;
			
			if( pMap != null) {
				filter = pMap.getLWCFilter();
				}
			System.out.println(" - building Filter Box");
			
			if( filter == null) {
				filter = new LWCFilter( pMap);
				}
				
			LWCFilter.LogicalStatement [] statements = filter.getLogicalStatements();
			mFilterBox.removeAll();
			mStatementEditors.removeAllElements();
			
			for( int i=0; i< statements.length; i++) {
				System.out.println("   !!! adding statement "+statements[i]);
				FilterStatementEditor fse = new FilterStatementEditor( filter, statements[i] );
				mFilterBox.add( fse);
				mStatementEditors.add( fse);
				}
			mFilterBox.add( mMoreFewerPanel );
			if( statements.length <= 1) {
				mFewerButton.hide();
				}
			else {
				mFewerButton.show();
				}
			validate();
		}
		
		public LWCFilter makeFilter() {
			LWCFilter filter = new LWCFilter( mStatementEditors );
			filter.setMap( mMap);
			filter.setIsFiltering( ( mActionCombo.getSelectedIndex() == FILTER_ACTION) );
			int mode = mAnyAllCombo.getSelectedIndex();
			filter.setIsAny( (mode == ANY_MODE) || (mode == NOT_ANY_MODE) );
			filter.setLogicalNot( (mode == NONE_MODE) || (mode == NOT_ANY_MODE) );
			return filter;
		}
		
		public void applyFilter() {
			mFilter = makeFilter();
			if( mMap != null) {
				mMap.setLWCFilter( mFilter);
				mFilter.setMap( mMap);
				mFilter.applyFilter();
				}
		}
		
		public void clearFilter() {

			if( mMap == null) 
				return;
 	       java.util.List list = mMap.getAllDescendents();

			Iterator it = list.iterator();
			while (it.hasNext()) {
				LWComponent c = (LWComponent) it.next();
				c.setIsFiltered( false);
				}
			mMap.notify( this, "repaint");
		}
		
		public void addStatement() {
			System.out.println("Adding staement!");
			mFewerButton.show();
			LWCFilter.LogicalStatement ls = mFilter.createLogicalStatement() ;
			FilterStatementEditor fse = new FilterStatementEditor( mFilter, ls);
			mStatementEditors.add( fse);
			
			mFilterBox.remove( mMoreFewerPanel);
			mFilterBox.add( fse);
			mFilterBox.add( mMoreFewerPanel);
			validate();
		}
		
		public void removeStatement() {
			FilterStatementEditor fse = (FilterStatementEditor) mStatementEditors.lastElement();
			mFilterBox.remove( fse);
			mStatementEditors.remove( fse);
			if( mStatementEditors.size() <= 1 ) {
				mFewerButton.hide();
				}
			validate();
		}
		
		public void actionPerformed( ActionEvent pEvent) {
			Object source = pEvent.getSource();
			
			if( source == mFilterButton )
				applyFilter();
			else
			if( source == mMoreButton)
				addStatement();
			else
			if( source == mFewerButton)
				removeStatement();
			else
			if( source == mClearFilterButton ) 
				clearFilter();
		}
	
	}

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

}





