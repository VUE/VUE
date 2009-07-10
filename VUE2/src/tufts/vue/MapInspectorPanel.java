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

package tufts.vue;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.beans.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import edu.tufts.vue.metadata.ui.MetadataEditor;

import tufts.vue.filter.*;
import tufts.vue.gui.*;

/**
 * A tabbed-pane collection of property sheets that apply
 * globally to a given map.
 *
 * @version $Revision: 1.73 $ / $Date: 2009-07-10 04:20:11 $ / $Author: mike $ 
 *
 */
public class MapInspectorPanel extends JPanel
    implements ActiveListener<LWMap>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapInspectorPanel.class);

    static public final int ANY_MODE = 0;
    static public final int ALL_MODE = 1;
    static public final int NOT_ANY_MODE = 2;
    static public final int NONE_MODE = 3;
    
    static public final String dcCreator = "http://purl.org/dc/elements/1.1/#Creator";
    static public final String dcDescription = "http://purl.org/dc/elements/1.1/#Description";
    
    /** The tabbed panel **/
    //private JTabbedPane mTabbedPane = null;
    
    /** The map we are inspecting **/
    private LWMap mMap = null;
    
    /** info tab panel **/
    private InfoPanel mInfoPanel = null;
    private VueAimPanel mVueAimPanel = null;
    //private PathwayPane mPathPanel = null;
    
    /** filter panel **/
    //private FilterApplyPanel mFilterApplyPanel = null;
    
    /** Filter Create Panel **/
    //private FilterCreatePanel mFilterCreatePanel = null;
    /** Metadata Panel **/
    //MetadataPanel metadataPanel = null; // metadata added to infoPanel
    MetadataEditor metadataPanel = null;
    
    VueTextPane mDescriptionEditor = null;
    JTextField mAuthorEditor = null;
    
    private WidgetStack mapInfoStack = null;
    
    public MapInspectorPanel(DockWindow w) {
        super();
        mapInfoStack = new WidgetStack("Map Info");
        VUE.addActiveListener(LWMap.class, this);
        setMinimumSize( new Dimension( 180,200) );
        setLayout( new BorderLayout() );
        //setBorder( new EmptyBorder( 5,5,5,5) );
        //mTabbedPane = new JTabbedPane();
       // VueResources.initComponent( mTabbedPane, "tabPane");
        
        mInfoPanel = new InfoPanel();
        mInfoPanel.setName(VueResources.getString("mapinspectorpanel.mapinfo"));
        //mPathPanel = new PathwayPane();
        //mFilterApplyPanel = new FilterApplyPanel();
        //mFilterCreatePanel = new FilterCreatePanel();
        //metadataPanel = new MetadataPanel();
        metadataPanel = new MetadataEditor(VUE.getActiveMap(),false,false);
        
        metadataPanel.setName(VueResources.getString("mapinspectorpanel.keywords"));
        //mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
        //mTabbedPane.addTab( mPathPanel.getName(),  mPathPanel);
        //if(tufts.vue.ui.InspectorPane.META_VERSION == tufts.vue.ui.InspectorPane.OLD)
       // {
       //   mTabbedPane.addTab( mFilterApplyPanel.getName(), mFilterApplyPanel);
       //   mTabbedPane.addTab(mFilterCreatePanel.getName(),mFilterCreatePanel);
       // }
        
        if (DEBUG.IM)
        {
        	mVueAimPanel = new VueAimPanel();
        	mVueAimPanel.setName(VueResources.getString("im.tabname"));
        }
        
        Widget.setWantsScroller(mapInfoStack, true);
        
        //mTabbedPane.addTab(metadataPanel.getName(),metadataPanel);
        mapInfoStack.addPane(mInfoPanel,0f);
        mapInfoStack.addPane(metadataPanel,0f);
        if (DEBUG.IM)
        	mapInfoStack.addPane(mVueAimPanel ,0f);
        metadataPanel.adjustColumnModel();
        
        //Widget.setWantsScroller(mapInfoStack, true);
        
        Widget.setHelpAction(metadataPanel,VueResources.getString("dockWindow.MapInfo.MapKeywords.helpText"));
        
        w.setContent(mapInfoStack);
        w.setHeight(450);
      //  add( BorderLayout.CENTER, mTabbedPane );
      //  add(BorderLayout.SOUTH,metadataPanel);
        setMap(VUE.getActiveMap());
        validate();
        setVisible(true);
    }
    
    public WidgetStack getMapInfoStack()
    {
        return mapInfoStack;
    }
    
    /**
     * setMap
     * Sets the LWMap component and updates teh display
     *
     * @param pMap - the LWMap to inspect
     **/
    public void setMap( LWMap pMap) {
        
        // if we have a change in maps...
        //if( pMap != mMap) {
        //    mMap = pMap;
        //}
        mMap = pMap;
        updatePanels();
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
        	metadataPanel.setVisible(false);
        }
        else {
        	metadataPanel.setVisible(true);
            mInfoPanel.updatePanel( mMap);
            
            //mPathPanel.updatePanel( mMap);
      //      mFilterApplyPanel.updatePanel(mMap);
        //    mFilterCreatePanel.updatePanel(mMap);
            //   metadataPanel.updatePanel(mMap);
        }
    }
    
    // override
    public Dimension XgetPreferredSize()  {
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
        //mTabbedPane.setSelectedComponent( mPathPanel);
        tufts.Util.printStackTrace("pathway panel moved");
    }
    
    public void activateInfoTab() {
      //  mTabbedPane.setSelectedComponent( mInfoPanel);
    }
    
    public void activateFilterTab() {
        //mTabbedPane.setSelectedComponent( mFilterApplyPanel);
    }
    /**
     * public void activateMetadataTab() {
     * mTabbedPane.setSelectedComponent( metadataPanel);
     * }
     *
     **/
    public void activeChanged(ActiveEvent<LWMap> e) {
        //tufts.Util.printStackTrace("AMC START");
    	
        if(e.active == null && mAuthorEditor != null && mDescriptionEditor != null)
        {
            mAuthorEditor.setText("");
            mDescriptionEditor.setText("");
        }
        
        setMap(e.active);
        //tufts.Util.printStackTrace("AMC END");
    }
    
    
    
    
    /////////////////
    // Inner Classes
    ////////////////////
    
    
    
    
    /**
     * InfoPanel
     * This is the tab panel for displaying Map Info
     *
     **/
    public class InfoPanel extends JPanel implements PropertyChangeListener, FocusListener
    {
        //JTextField mTitleEditor = null;
        //JText Field mAuthorEditor = null;
        //VueTextPane mAuthorEditor = null;
        JLabel mDate = null;
        
        // VUE-1001
        //JLabel mLocation = null;
        //mDescriptionEditor = null;
        PropertyPanel mPropPanel = null;
        PropertiesEditor propertiesEditor = null;
        ColorMenuButton mMapColor = new ColorMenuButton(VueResources.getColorArray("fillColorValues"),true);
        public InfoPanel() {
        
             mMapColor.setToolTipText(VueResources.getString("mapinspectorpanel.mapcolor"));
             mMapColor.setName(VueResources.getString("mapinspectorpanel.map_color"));
  
             mMapColor.setPropertyKey(null);
             mMapColor.getPopupWindow().addFocusListener(new FocusListener()
             {

				public void focusGained(FocusEvent arg0) {
					// TODO Auto-generated method stub
					
				}

				public void focusLost(FocusEvent arg0) {
                                    final LWMap map = VUE.getActiveMap();
                                    if (map != null) {
                                        map.setFillColor(mMapColor.getColor());
                                        map.getUndoManager().mark();
                                    }
				}
            	 
             });
             
             /*{

				public void propertyChange(PropertyChangeEvent arg0) {
					if (VUE.getActiveMap() != null)
						VUE.getActiveMap().setFillColor(mMapColor.getColor());				
				}
            	 
             });*/
           
            JPanel innerPanel = new JPanel();
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
        
            //BoxLayout boxLayout = new BoxLayout(innerPanel,BoxLayout.Y_AXIS);
            innerPanel.setLayout(gridbag);
            //mTitleEditor = new JTextField();
            mAuthorEditor = new JTextField();
            
            mDescriptionEditor = new VueTextPane("Map Description");
            mDescriptionEditor.setMinimumSize(new Dimension(180, 60));
            JScrollPane descriptionScroller = new JScrollPane(mDescriptionEditor);
            descriptionScroller.setMinimumSize(new Dimension(180, 60));
            descriptionScroller.setPreferredSize(new Dimension(180,100));
            descriptionScroller.setOpaque(false);
            
            //descriptionScroller.setBorder(new CompoundBorder(new EmptyBorder(3,0,0,0), descriptionScroller.getBorder()));
            
            //descriptionScroller.setBorder(new CompoundBorder(new EmptyBorder(9,0,0,0), BorderFactory.createLineBorder(Color.DARK_GRAY)));
            //descriptionScroller.setBorder(new EmptyBorder(9,0,0,0));
            //mDescriptionEditor.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));


            /*
              // TODO: need to add MenuButton functionality to allow it
              // NOT to participate in the global selection, and "load"
              // objects manually, or track only a specific LWComponent or something.
              // Create a subclass of LWEditor, LWTypeEditor, with a machesType method
              // we check for instanceof LWMap.class, which the tool manager can
              // handle, and then here we can have an non class instanceof ColorMenuButton
              // that overrides handleMenuSelection and manually sets the current
              // map fill color (because we won't normally actually select the map),
              // and gets manually loaded with displayValue.
              
            final Color[] fillColors = VueResources.getColorArray("fillColorValues");
            final String[] fillColorNames = VueResources.getStringArray("fillColorNames");
            final ColorMenuButton mapFill;
            mapFill = new ColorMenuButton(fillColors, fillColorNames, true);
            mapFill.setPropertyKey(LWKey.FillColor);
            */
                        
            
            mDate = new JLabel();
            
            // VUE-1001
            //mLocation = new JLabel();
            

            //saveButton = new JButton("Save");
            //saveButton.addActionListener(this);
            mPropPanel  = new PropertyPanel();
            //mPropPanel.addProperty( "Label:", mTitleEditor); // initially Label was title
            mPropPanel.addProperty(VueResources.getString("mapinspectorpanel.creator"), mAuthorEditor); //added through metadata
            mPropPanel.addProperty(VueResources.getString("mapinspectorpanel.background"),mMapColor);
            mPropPanel.addProperty(VueResources.getString("mapinspectorpanel.created"), mDate);
            
            // VUE-1001
            //mPropPanel.addProperty("Location:",mLocation);            
            
            //mPropPanel.addProperty("Background:", mapFill);
            mPropPanel.addProperty(VueResources.getString("mapinspectorpanel.description"), descriptionScroller);
            //mPropPanel.addProperty("Description:", descriptionScroller);
            //mPropPanel.setBorder(BorderFactory.createEmptyBorder(6,9,6, 6));
            //mInfoBox.add(saveButton,BorderLayout.EAST); added focuslistener
             c.weightx = 1.0;
             c.gridwidth = GridBagConstraints.REMAINDER;
             c.anchor = GridBagConstraints.NORTHWEST;
             c.fill = GridBagConstraints.HORIZONTAL;
             gridbag.setConstraints(mPropPanel,c);
             innerPanel.add(mPropPanel);
            
            /**
             * JPanel metaDataLabelPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
             * metaDataLabelPanel.add(new JLabel("Metadata"));
             *
             * innerPanel.add(metaDataLabelPanel);
             */
            
            
            JPanel linePanel = new JPanel() {
                protected void paintComponent(Graphics g) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawLine(0,15, this.getSize().width, 15);
                }
            };
            
            //c.gridwidth = GridBagConstraints.REMAINDER;
            //c.fill = GridBagConstraints.HORIZONTAL;
            //gridbag.setConstraints(linePanel,c);
            //innerPanel.add(linePanel);
            //linePanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
            propertiesEditor = new PropertiesEditor(true);
            JPanel metadataPanel = new JPanel(new BorderLayout());
//             if(tufts.vue.ui.InspectorPane.META_VERSION == tufts.vue.ui.InspectorPane.OLD)
//             {
//               metadataPanel.add(propertiesEditor,BorderLayout.CENTER);
//             }
            //metadataPanel.setBorder(BorderFactory.createEmptyBorder(0,9,0,6));
            
            
             mAuthorEditor.setFont(GUI.LabelFace);
             mDate.setFont(GUI.LabelFace);
             // VUE 1001
             //mLocation.setFont(GUI.LabelFace);
             mDescriptionEditor.setFont(GUI.LabelFace);
             
             mAuthorEditor.addFocusListener(new FocusAdapter(){
                public void focusLost(FocusEvent e)
                {
                    LWMap currentMap = VUE.getActiveMap();
                    if(currentMap == null)
                        return;
                    edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                    String[] pairedValue = {dcCreator,mAuthorEditor.getText()};
                    vme.setObject(pairedValue);
                    vme.setType(edu.tufts.vue.metadata.VueMetadataElement.RESOURCE_CATEGORY);
                    if(currentMap.getMetadataList().findRCategory(dcCreator) != -1)
                    {
                      mMap.getMetadataList().modify(vme);
                    }
                    else
                    {
                      mMap.getMetadataList().getMetadata().add(vme);
                    }
                }
             });
             
             mDescriptionEditor.addFocusListener(new FocusAdapter(){
                public void focusLost(FocusEvent e)
                {
                    LWMap currentMap = VUE.getActiveMap();
                    edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                    String[] pairedValue = {dcDescription,LWComponent.escapeWhitespace(mDescriptionEditor.getText())};
                    vme.setObject(pairedValue);
                    vme.setType(edu.tufts.vue.metadata.VueMetadataElement.RESOURCE_CATEGORY);
                    if(currentMap == null)
                        return;
                    if(currentMap.getMetadataList().findRCategory(dcDescription) != -1)
                    {
                      mMap.getMetadataList().modify(vme);
                    }
                    else
                    {
                      mMap.getMetadataList().getMetadata().add(vme);
                    }
                }
             });
             
            
            
            c.weighty = 1.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            gridbag.setConstraints(metadataPanel,c);
            innerPanel.add(metadataPanel);
            //innerPanel.add(mInfoScrollPane,BorderLayout.CENTER);
            //mInfoScrollPane.setSize( new Dimension( 200, 400));
            //mInfoScrollPane.getViewport().setLayout(new BorderLayout());
            //mInfoScrollPane.getViewport().add( innerPanel,BorderLayout.CENTER);
            //mInfoScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            setLayout(new BorderLayout());
            //setLayout(new BorderLayout());
            //setBorder( new EmptyBorder(4,4,4,4) );
            //add(mInfoScrollPane,BorderLayout.NORTH);
            add(innerPanel,BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
            addFocusListener(this);
        }
        
        public String getName() {
            return VueResources.getString("mapInfoTabName") ;
        }
        
        
        
        /**
         * updatePanel
         * Updates the Map info panel
         * @param LWMap the map
         **/
        public void updatePanel(LWMap pMap) {
            // update the display
            mDate.setText( mMap.getDate() );
            //mTitleEditor.setText( mMap.getLabel() );
            edu.tufts.vue.metadata.VueMetadataElement creator = 
                    mMap.getMetadataList().get(dcCreator);
            String creatorValue = "";
            if(creator != null)
            {
                creatorValue = creator.getValue();
            }
            mAuthorEditor.setText(creatorValue);
            
            mDescriptionEditor.attachProperty(mMap, LWKey.Notes);
            
            edu.tufts.vue.metadata.VueMetadataElement description = 
                    mMap.getMetadataList().get(dcDescription);
            if(description != null)
            {
                String descriptionValue = description.getValue();
                mDescriptionEditor.setText(LWComponent.unEscapeWhitespace(descriptionValue));
            }
            
            File file = mMap.getFile() ;
            String path = "";
            if( file != null) {
                path = file.getPath();
            }
            
            // VUE-1001
            //mLocation.setText(path);
            //mLocation.setToolTipText(path);
            propertiesEditor.setProperties(pMap.getMetadata(),true);
            if (DEBUG.EVENTS) Log.debug(getClass().getSimpleName() + ".updatePanel: " + VUE.getActiveMap().getFillColor());
            mMapColor.setColor(VUE.getActiveMap().getFillColor());
        }
        
        private void saveInfo() {
            //System.out.println("MIP saveInfo " + mDescriptionEditor.getText());
            if( mMap != null) {
                // for now, only description/notes needs saving, and it handles that it itself
                // add back in Author until/unless synched with metadata list: VUE-951
                //mMap.setLabel( mTitleEditor.getText() );
                //mMap.setAuthor(  mAuthorEditor.getText() );
                //mMap.setNotes(mDescriptionEditor.getText());
                //mMap.setDescription(mDescriptionEditor.getText());
            }
        }
        /**
         * public void actionPerformed( ActionEvent pEvent) {
         * Object source = pEvent.getSource();
         * System.out.println("Action Performed :"+source);
         * if( (source == saveButton) || (source == mTitleEditor) || (source == mAuthorEditor) || (source == mDescriptionEditor) ) {
         * saveInfo();
         * }
         * }
         **/
        public void propertyChange( PropertyChangeEvent pEvent) {
            
        }
        
        public void focusGained(FocusEvent e) {
        }
        
        public void focusLost(FocusEvent e) {
            saveInfo();
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
        
        PathwayPanel mPathDisplay = null;
        
        /**
         * PathwayPane
         * Constructs a pathway panel
         **/
        public PathwayPane() {
            
            setLayout(new BorderLayout());
            //mPathDisplay = new JPanel();
            //mPathDisplay.add( new JLabel("Pathway offline") );
            
            mPathDisplay = new PathwayPanel(VUE.getDialogParentAsFrame());
            
            mPathScrollPane = new JScrollPane();
            mPathScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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
            
            //PATH TODO: mPathDisplay.setPathwayManager(pMap.getPathwayManager());
            // update display based on the LWMap
        }
    }
    
    
    /**
     * FilterPanel
     * This is the Map Filtering Panel for the Map Inspector
     *
     **/
    
    
    public class MetadataPanel extends JPanel implements ActionListener, PropertyChangeListener {
        PropertiesEditor propertiesEditor = null;
        public MetadataPanel() {
            //setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
            setLayout(new BorderLayout());
            setBorder( BorderFactory.createEmptyBorder(10,10,0,6));
            setName(VueResources.getString("mapinspectorpanel.keywords"));
        }
        
        public MetadataPanel(LWMap map) {
            this();
            propertiesEditor = new PropertiesEditor(map.getMetadata(),true);
            add(propertiesEditor,BorderLayout.WEST);
            
        }
        
        public void actionPerformed(ActionEvent e) {
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
        }

        public void updatePanel( LWMap pMap) {
            // update the display
            if(propertiesEditor != null) {
                propertiesEditor.setProperties(pMap.getMetadata(),true);
            } else {
                propertiesEditor = new PropertiesEditor(pMap.getMetadata(),true);
                add(propertiesEditor,BorderLayout.WEST);
            }
            validate();
            
            
        }
        
        
    }
    
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
        JToggleButton mFilterButton = null;
        
        /** the stop filter button **/
        JButton mClearFilterButton = null;
        
        /** the more button **/
        JButton mMoreButton = null;
        
        /** the fewer button **/
        JButton mFewerButton = null;
        
        /** Radio Buttons **/
        JRadioButton mShowButton = null;
        JRadioButton mSelectButton = null;
        JRadioButton mHideButton = null;
        ButtonGroup modeSelectionGroup = null;
        
        
        /** mode combo **/
        JComboBox mModeCombo = null;
        
        /** action combo Hide/Show/Select **/
        JComboBox mActionCombo = null;
        
        
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
            ButtonGroup criteriaSelectionGroup = new ButtonGroup();
            
            setLayout( new BorderLayout() );
            setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
            
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
            mShowButton = new JRadioButton(LWCFilter.ACTION_SHOW,true);
            mSelectButton = new JRadioButton(LWCFilter.ACTION_SELECT);
            mHideButton = new JRadioButton(LWCFilter.ACTION_HIDE);
            mShowButton.setFont(tufts.vue.VueConstants.FONT_MEDIUM);
            mShowButton.addActionListener(this);
            mSelectButton.addActionListener(this);
            mHideButton.addActionListener(this);
            
            modeSelectionGroup = new ButtonGroup();
            modeSelectionGroup.add(mShowButton);
            modeSelectionGroup.add(mHideButton);
            modeSelectionGroup.add(mSelectButton);
            
            //mUpperPanel.add( BorderLayout.NORTH, new JLabel("Display Criteria:"));
            Box topBox = Box.createHorizontalBox();
            //topBox.add( mActionCombo);
            topBox.add(mShowButton);
            topBox.add(mHideButton);
            topBox.add(mSelectButton);
            JLabel clause = new JLabel(VueResources.getString("jlabel.mapobjects"));
            topBox.add( clause);
            //topBox.add( mAnyAllCombo);
            
            mUpperPanel.add( BorderLayout.SOUTH, topBox);
            
            mFilterButton = new JToggleButton(VueResources.getString("button.disablefilter.label"),false);
            mFilterButton.setText(VueResources.getString("mapinspection.applyfilter.tooltip"));
            mClearFilterButton = new JButton(VueResources.getString("button.disablefilter.label"));
            mMoreButton = new VueButton(VueResources.getString("button.add.label"));
            mFewerButton = new VueButton(VueResources.getString("button.delete.label"));
            
            mFewerButton.setVisible(false);
            
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
            //abBox.add( mClearFilterButton);
            abBox.add( mFilterButton);
            abp.add( BorderLayout.EAST, abBox);
            mLowerPanel.add( BorderLayout.SOUTH, abp);
            mLowerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0,0,0));
            
            
            mFilterBox = Box.createVerticalBox();
            mFilterScrollPane = new JScrollPane();
            filterEditor = new FilterEditor();
            filterEditor.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
            mFilterBox.add(filterEditor);
            mFilterBox.add(mUpperPanel);
            mFilterBox.add(mLowerPanel);
            mMainFilterPanel.add( BorderLayout.NORTH, mFilterBox);
            
            mFilterScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            mFilterScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            mFilterScrollPane.setLocation(new Point(8, 9));
            mFilterScrollPane.setVisible(true);
            mFilterScrollPane.getViewport().add( mMainFilterPanel);
            mFilterScrollPane.setBorder( BorderFactory.createEmptyBorder());
            //mMainFilterPanel.setBackground(VueResources.getColor("filterPanelColor"));
            add( BorderLayout.CENTER, mFilterScrollPane );
        }
        
        
        public String getName() {
            //return VueResources.getString("mapFilterTabName") ;
            return "Filter";
        }
        
        
        /**
         * updatePanel
         * Updates teh panel based on the passed in LWMap
         * @param the LWMap
         **/
        public void updatePanel( LWMap pMap) {
            boolean hasMap = (pMap != null);
            
            mFilterButton.setEnabled(hasMap);
            mClearFilterButton.setEnabled( hasMap);
            mMoreButton.setEnabled( hasMap);
            mFewerButton.setEnabled( hasMap);
            
            if (hasMap) {
                mMap = pMap;
                mFilter = pMap.getLWCFilter();
            } else {
               return;
            }
            
            if(mFilter.getStatements() == null) {
                mFilter.setStatements(new Vector());
            }
            try {
                filterEditor.getFilterTableModel().setFilters(mFilter.getStatements());
            } catch (NullPointerException e) {
                // for testing: FilterEditor bombs if no active map
                e.printStackTrace();
            }
            //mActionCombo.setSelectedItem(mFilter.getFilterAction());
            if(mFilter.getFilterAction().toString().equals(LWCFilter.ACTION_HIDE)) {
                mHideButton.setSelected(true);
            } else if(mFilter.getFilterAction().toString().equals(LWCFilter.ACTION_SELECT)) {
                mSelectButton.setSelected(true);
            } else {
                mShowButton.setSelected(true);
            }
          
            if(mFilter.isFilterOn())  {
                mFilterButton.setSelected(true);
                mFilterButton.setText(VueResources.getString("mapinspection.disablefilter.tooltip"));
            }else {
                mFilterButton.setSelected(false);
                mFilterButton.setText(VueResources.getString("mapinspection.applyfilter.tooltip"));
            }
            // TODO FIX: basically, filter updating is very dumb right now: this doClick is ultimately
            // ALWAYS triggering a setFilter on the current map whenever the active map
            // changes, even if the filter is exactly the same, or there isn't even
            // one in place, requiring the map to filter itself or clear it's filter
            // every time the active map changes, no matter what (which will get expensive
            // with sizeable maps).  There's also a bug where the filter is getting turned
            // off when you switch to another map and then back again.
            //mFilterButton.doClick();
            mMainFilterPanel.validate();
        }
        /** Enabled the current filter, and tell the map of a filter change */
        
         public void makeFilter() {
            mFilter.setStatements(filterEditor.getFilterTableModel().getFilters());
            if(mHideButton.isSelected())
                mFilter.setFilterAction(LWCFilter.ACTION_HIDE);
            else if(mSelectButton.isSelected())
                mFilter.setFilterAction(LWCFilter.ACTION_SELECT);
            else
                mFilter.setFilterAction(LWCFilter.ACTION_SHOW);
        }
        
        public void applyFilter() {
            if (mMap != null) {
                makeFilter();
                mMap.applyFilter();
            }
        }
        
        /** Disable the current filter, and tell the map of a filter change */
        public void clearFilter() {
            if (mMap != null)
               mMap.clearFilter();
        }
        
        public void addStatement() {
            mFewerButton.setVisible(true);
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
                mFewerButton.setVisible(false);
            }
            validate();
        }
        
        public void actionPerformed( ActionEvent pEvent) {
            Object source = pEvent.getSource();
            filterEditor.stopEditing();
            if( source == mFilterButton ) {
                JToggleButton button = (JToggleButton)source;
                if(button.isSelected()) {
                    applyFilter();
                    button.setText(VueResources.getString("mapinspection.disablefilter.tooltip"));
                }else {
                    clearFilter();
                    button.setText(VueResources.getString("mapinspection.applyfilter.tooltip"));
                }
            }
        }
        
    }
    
    public class FilterCreatePanel extends JPanel implements ActionListener, PropertyChangeListener {
        MapFilterModelEditor mapFilterModelEditor = null;
        
        public FilterCreatePanel() {
            
            //  setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
            
        }
        
        public FilterCreatePanel(LWMap map) {
            
            this();
            mapFilterModelEditor = new MapFilterModelEditor(map.getMapFilterModel());
            add(mapFilterModelEditor,BorderLayout.NORTH);
            
        }
        
        public void actionPerformed(ActionEvent e) {
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
        }
        public String getName() {
            //return "Custom Metadata"; // this should come from VueResources
            return "Metadata"; // this should come from VueResources
        }
        public void updatePanel( LWMap pMap) {
            // update the display
            if(mapFilterModelEditor == null) {
                mapFilterModelEditor = new MapFilterModelEditor(pMap.getMapFilterModel());
                add(mapFilterModelEditor,BorderLayout.NORTH);
            }else {
                mapFilterModelEditor.setMapFilterModel(pMap.getMapFilterModel());
            }
            validate();
        }
    }

    public static void main(String args[]) {
        VUE.init(args);
        DEBUG.Enabled = DEBUG.EVENTS = true;
        LWMap map = new LWMap("test_map");
        map.setFile(new java.io.File("/tmp/test.vue"));
        MapInspectorPanel inspector = new MapInspectorPanel(null);
        //VUE.setActiveMap(map);
        inspector.setMap(map);
        DockWindow w = GUI.createDockWindow(VueResources.getString("dockWindow.mapinspector.title"), inspector);
        w.setVisible(true);
        if (args.length > 1)
            VueUtil.displayComponent(new VueTextPane(map, LWKey.Notes, null));
    }
    
}





