/*
 * FilterPanel.java
 *
 * Created on February 24, 2004, 4:29 PM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import java.io.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


import tufts.vue.filter.*;


public class FilterPanel extends JPanel implements  VUE.ActiveMapListener{
    
    static public final int ANY_MODE = 0;
    static public final int ALL_MODE = 1;
    static public final int NOT_ANY_MODE = 2;
    static public final int NONE_MODE = 3;
    JTabbedPane mTabbedPane = null;
    
    /** The map we are inspecting **/
    LWMap mMap = null;
    
    FilterCreatePanel filterCreatePanel = null;
    
    FilterApplyPanel filterApplyPanel = null;
    
    /** Creates a new instance of FilterPanel */
    public FilterPanel() {   
        super();
        VUE.addActiveMapListener(this);
        //setMinimumSize( new Dimension( 150,200) );
        mTabbedPane = new JTabbedPane();
        VueResources.initComponent( mTabbedPane, "tabPane");
        filterCreatePanel = new FilterCreatePanel();
        filterApplyPanel = new FilterApplyPanel();
        //mTabbedPane.setBackground(VueResources.getColor("filterPanelColor"));        
        //filterCreatePanel.setBackground(VueResources.getColor("filterPanelColor"));
        //filterApplyPanel.setBackground(VueResources.getColor("filterPanelColor"));
        mTabbedPane.addTab(filterCreatePanel.getName(),filterCreatePanel);
        mTabbedPane.addTab(filterApplyPanel.getName(),filterApplyPanel);
        mTabbedPane.setSelectedComponent(filterApplyPanel);
        mTabbedPane.setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());
        add( mTabbedPane,BorderLayout.CENTER );
        setMap(VUE.getActiveMap());
        validate();
        show();
        
    }
    
    public void activeMapChanged(LWMap map) {
        setMap(map);
    }
    public void setMap( LWMap pMap) {
        
        // if we have a change in maps...
        if( pMap != mMap) {
            mMap = pMap;
            updatePanels();
        }
    }
    
    public void updatePanels() {
        
        if( mMap == null) {
            //clear it
        }
        else {
            filterCreatePanel.updatePanel(mMap);
            filterApplyPanel.updatePanel(mMap);
        }
    }
    
    public String getName() {
        return "Filters"; // this should come from VueResources
    }
    /**
     * FilterPanel
     * This is the Map Filtering Panel for the Map Inspector
     *
     **/
    public class FilterApplyPanel extends JPanel implements ActionListener {
        
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
        
        /** action combo Hide/Show/Select **/
        JComboBox mActionCombo = null;
        
        JComboBox mAnyAllCombo = null;
        
        LWCFilter mFilter = null;
        Vector mStatementEditors = new Vector();
        
        FilterEditor filterEditor;
        
        
        ////////////
        // Constructors
        ////////////////
        
        /**
         * FilterPanel Constructor
         **/
        public FilterApplyPanel() {
            
            setLayout( new BorderLayout() );
            setBorder( new EmptyBorder( 4,4,4,4) );
            mMainFilterPanel = new JPanel();
            mMainFilterPanel.setLayout( new BorderLayout() );
            mLowerPanel = new JPanel();
            mLowerPanel.setLayout( new BorderLayout() );
            mUpperPanel = new JPanel();
            mUpperPanel.setLayout( new BorderLayout() );
            
            mActionCombo = new JComboBox();
            mActionCombo.addItem(LWCFilter.ACTION_SHOW); 
            mActionCombo.addItem(LWCFilter.ACTION_SELECT);
            mActionCombo.addItem(LWCFilter.ACTION_HIDE);
           
            
            // disabled for now. May add a modified version later
            mAnyAllCombo = new JComboBox();
            mAnyAllCombo.addItem("match any");
            mAnyAllCombo.addItem("match all");
            mAnyAllCombo.addItem("don't match any");
            mAnyAllCombo.addItem("match none");
            
            
            
            
            mUpperPanel.add( BorderLayout.NORTH, new JLabel("Display Criteria:"));
            Box topBox = Box.createHorizontalBox();
            topBox.add( mActionCombo);
            JLabel clause = new JLabel(" objects on map ");
            topBox.add( clause);
            //topBox.add( mAnyAllCombo);
            
            mUpperPanel.add( BorderLayout.SOUTH, topBox);
            
            mFilterButton = new JButton( "Apply Filter");
            mClearFilterButton = new JButton("Disable Filter");
            mMoreButton = new VueButton("add");
            mFewerButton = new VueButton("delete");
            
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
            /**
             * FilterStatementEditor fse = new FilterStatementEditor( mFilter, mFilter.getLogicalStatements()[0] );
             * mStatementEditors.add( fse);
             * mFilterBox.add( fse);
             * mFilterBox.add( mMoreFewerPanel);
             */
            
            mFilterScrollPane = new JScrollPane();
            
            filterEditor = new FilterEditor();
            
            mFilterBox.add(mUpperPanel);
            mFilterBox.add(filterEditor);
            mFilterBox.add(mLowerPanel);
            mMainFilterPanel.add( BorderLayout.NORTH, mFilterBox);
            
            mFilterScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            mFilterScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mFilterScrollPane.setLocation(new Point(8, 9));
            mFilterScrollPane.setVisible(true);
            mFilterScrollPane.getViewport().add( mMainFilterPanel);
            mFilterScrollPane.setBorder( BorderFactory.createEmptyBorder());
            
            add( BorderLayout.CENTER, mFilterScrollPane );
        }
        
        
        public String getName() {
            //return VueResources.getString("mapFilterTabName") ;
            return "Apply";
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
            /**
            if (hasMap)
                mFilter = pMap.getLWCFilter();
            else
                mFilter = new LWCFilter();
            **/
            
            
            if(pMap.getLWCFilter() == null) {
                pMap.setLWCFilter(new LWCFilter(pMap));
                System.out.println("Created new filter for map ="+pMap);
            }
            if(pMap.getLWCFilter().getStatements() == null) {
                 pMap.getLWCFilter().setStatements(new Vector());
                 System.out.println("Created new statements ="+pMap.getLWCFilter());
            }
            mFilter = pMap.getLWCFilter();
            filterEditor.getFilterTableModel().setFilters(mFilter.getStatements());
            mActionCombo.setSelectedItem(mFilter.getFilterAction());
            
            int val = ANY_MODE;
            if( !mFilter.getIsAny() )
                val = ALL_MODE;
            if( mFilter.isLogicalNot() )
                val += 2;
            
            mAnyAllCombo.setSelectedIndex( val);
            //buildFilterBox( pMap);
            /**
            mMainFilterPanel.remove(filterEditor);

            filterEditor = new FilterEditor();
            mFilter.setStatements(filterEditor.getFilterTableModel());          
            mMainFilterPanel.add(BorderLayout.CENTER, filterEditor);
             */
            mMainFilterPanel.validate();
           
   

            
            
        }
        
        public LWCFilter makeNewFIlter() {
            LWCFilter filter = new LWCFilter();  
            LWCFilter.LogicalStatement [] satements = new LWCFilter.LogicalStatement[ mStatementEditors.size() ];
            return filter;
        }
        
      /**  
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
            mMainFilterPanel.remove(filterEditor);
            filterEditor = new FilterEditor(filter.getStatements());
            mMainFilterPanel.add(BorderLayout.CENTER, filterEditor);
            mMainFilterPanel.validate();
            validate();
        }
        **/
        public LWCFilter makeFilter() {
            LWCFilter filter = new LWCFilter( mStatementEditors );
            //LWCFilter filter = new LWCFilter(filterEditor.getFilterTableModel());
            filter.setStatements(filterEditor.getFilterTableModel().getFilters());
            filter.setMap( mMap);
            filter.setFilterAction(mActionCombo.getSelectedItem());
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
                if( source == mClearFilterButton )
                    clearFilter();
        }
        
    }
    public class FilterCreatePanel extends JPanel implements ActionListener, PropertyChangeListener {
        MapFilterModelEditor mapFilterModelEditor = null;
        
        public FilterCreatePanel() {
            
            setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
            setBorder( new EmptyBorder(4,4,4,4) );
            
        }
        
        public FilterCreatePanel(LWMap map) {
            
            setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
            setBorder( new EmptyBorder(4,4,4,4) );
            mapFilterModelEditor = new MapFilterModelEditor(map.getMapFilterModel());
            add(mapFilterModelEditor);
            
        }
        
        public void actionPerformed(ActionEvent e) {
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
        }
        public String getName() {
            return "Create"; // this should come from VueResources
        }
        public void updatePanel( LWMap pMap) {
            // update the display
             if(mapFilterModelEditor == null) {
                mapFilterModelEditor = new MapFilterModelEditor(pMap.getMapFilterModel());
                add(mapFilterModelEditor);
            }else {
                mapFilterModelEditor.setMapFilterModel(pMap.getMapFilterModel());
            }        
            validate();
        }  
    } 
}
