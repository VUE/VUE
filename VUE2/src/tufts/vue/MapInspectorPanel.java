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


import tufts.vue.filter.*;

/**
* ObjectInspectorPanel
*
* The Object  Inspector Panel!
*
\**/
public class MapInspectorPanel  extends JPanel 
			implements  VUE.ActiveMapListener {

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
        
        /** Metadata Panel **/
        MetadataPanel metadataPanel = null;
	
    
	///////////////////
	// Constructors
	////////////////////
	
	public MapInspectorPanel() {
		super();
		VUE.addActiveMapListener(this);
		setMinimumSize( new Dimension( 180,200) );
		setLayout( new BorderLayout() );
		setBorder( new EmptyBorder( 5,5,5,5) );
		mTabbedPane = new JTabbedPane();
		VueResources.initComponent( mTabbedPane, "tabPane");
		
		
		mInfoPanel = new InfoPanel();
		mPathPanel = new PathwayPane();
		mFilterPanel = new FilterPanel();
                metadataPanel = new MetadataPanel();
               
		mTabbedPane.addTab( mInfoPanel.getName(), mInfoPanel);
		mTabbedPane.addTab( mPathPanel.getName(),  mPathPanel);
		mTabbedPane.addTab( mFilterPanel.getName(), mFilterPanel);
                mTabbedPane.addTab(metadataPanel.getName(),metadataPanel);
               
		add( BorderLayout.CENTER, mTabbedPane );
                setMap(VUE.getActiveMap());
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
			
                        metadataPanel.updatePanel(mMap);
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
        public void activateMetadataTab() {
		mTabbedPane.setSelectedComponent( metadataPanel);
	}
	

        public void activeMapChanged(LWMap map) {
            setMap(map);
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
                JTextArea mDescriptionEditor = null;
		JButton saveButton = null;
		PropertyPanel mPropPanel = null;
		
		public InfoPanel() {
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder(4,4,4,4) );
			
			mInfoBox = Box.createVerticalBox();
		
			mInfoScrollPane = new JScrollPane();
			mInfoScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			mInfoScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			mInfoScrollPane.setLocation(new Point(8, 9));
			mInfoScrollPane.setVisible(true);
			mInfoScrollPane.getViewport().add( mInfoBox);
		
			add( BorderLayout.NORTH, mInfoScrollPane );
			
			mTitleEditor = new JTextField();
			
			mAuthorEditor = new JTextField();
                        mDescriptionEditor = new JTextArea();
                        mDescriptionEditor.setRows(5);
                        mDescriptionEditor.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                        mDate = new JLabel();
			mLocation = new JLabel();
                        saveButton = new JButton("Save");
			mPropPanel  = new PropertyPanel();
                        saveButton.addActionListener(this);
			
                        
			mPropPanel.addProperty( "Title:", mTitleEditor);
			mPropPanel.addProperty("Author:", mAuthorEditor);
			mPropPanel.addProperty("Date:", mDate);
			mPropPanel.addProperty("Location:",mLocation);
                        mPropPanel.addProperty("Description:",mDescriptionEditor);
			mInfoBox.add( mPropPanel);
                        mInfoBox.add(saveButton,BorderLayout.EAST);
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
                        mDescriptionEditor.setText(mMap.getDescrition());
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
                                mMap.setDescription(mDescriptionEditor.getText());
				}
		}
		
		public void actionPerformed( ActionEvent pEvent) {
			Object source = pEvent.getSource();
                        System.out.println("Action Performed :"+source);
			if( (source == saveButton) || (source == mTitleEditor) || (source == mAuthorEditor) || (source == mDescriptionEditor) ) {
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
		
		PathwayPanel mPathDisplay = null;
		
		/**
		 * PathwayPane
		 * Constructs a pathway panel
		 **/
		public PathwayPane() {
			
                        setLayout(new BorderLayout());
			//mPathDisplay = new JPanel();
			//mPathDisplay.add( new JLabel("Pathway offline") );
			
                        mPathDisplay = new PathwayPanel(VUE.frame);
                        
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
			setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
			setBorder( new EmptyBorder(4,4,4,4) );
                }
                
		
		public MetadataPanel(LWMap map) {
			
			setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
			setBorder( new EmptyBorder(4,4,4,4) );
                        propertiesEditor = new PropertiesEditor(map.getMetadata(),true);
                        add(propertiesEditor);
                       
                }
                
                public void actionPerformed(ActionEvent e) {
                }
                
                public void propertyChange(PropertyChangeEvent evt) {
                }
                public String getName() {
                    return "Metadata"; // this should come from VueResources
                }
                public void updatePanel( LWMap pMap) {
			// update the display
                    if(propertiesEditor != null)
                        remove(propertiesEditor);
                    propertiesEditor = new PropertiesEditor(pMap.getMetadata(),true);
                    add(propertiesEditor);
                    validate();

                        
		}
             
                
    }
    
    public class MapFilterPanel extends JPanel implements ActionListener, PropertyChangeListener {
                MapFilterModelEditor mapFilterModelEditor = null;
                
                public MapFilterPanel() {
			
			setLayout( new FlowLayout(FlowLayout.LEFT,6,6) );
			setBorder( new EmptyBorder(4,4,4,4) );
                       
                }
		
		public MapFilterPanel(LWMap map) {
			
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
                    return "Map Filters"; // this should come from VueResources
                }
                public void updatePanel( LWMap pMap) {
			// update the display
                    if(mapFilterModelEditor!= null)
                        remove(mapFilterModelEditor);
                    mapFilterModelEditor = new MapFilterModelEditor(pMap.getMapFilterModel());
                    add(mapFilterModelEditor);
                    validate();

                        
		}
             
                
    }
    
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

   
		
}





