/*******
**  VueToolbarController.java
**
**
*********/


package tufts.vue;

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
* VueToolbarController
*
*
**/
public class VueToolbarController  
		implements LWSelection.Listener  
 {
	//////////
	// Statics
	//////////
	
	/** debug state true on/ false off **/
	private static boolean sDebug = false;
	
	/** the list of tools **/
	public static final String kDefaultToolsKey = "defaultToolNames";
	
	static private VueToolbarController sController = new VueToolbarController();
	
	
	
	
	//////////
	// Fields
	//////////
	
	// FIX: mToolPanel could be moved to an interface to support a different view of the tools
	/** a table of toolbars for each map **/
	private VueToolPanel mToolPanel = null;
	
	/** a list of tool selection listeners **/
	private Vector mToolSelectionListeners = new Vector();
	
	/** a list of available tools **/
	private VueTool []  mVueTools = null;
	
	/** current contextual panel **/
	private JPanel mContextualPanel = null;
	
	// current tool panel
	private JPanel mMainToolbar = null;
	
	// the currently selected tool 
	private VueTool mSelectedTool = null;
	
	// current selection id
	private String mCurSelectionID = new String();
	
	
	////////////////
	//  Constructors
	///////////////////
	
	/***
	* VUEToolbarControler()
	* The constructor that builds an initial VUE ToolPanel
	**/
	protected  VueToolbarController() {
		super();
		loadTools();
		mToolPanel = createDefaultToolbar();
		
	}
	
	
	////////////////
	//  Methods
	////////////////

     public VueTool[] getTools()
     {
         return mVueTools;
     }
	
	
	/**
	 * loadTools()
	 * Loads the default tools specified in a resource file
	 **/
	protected synchronized void loadTools() {
		
		String [] names = VueResources.getStringArray( kDefaultToolsKey );
		if( names != null) {
			mVueTools = new VueTool[ names.length];
			for( int i=0; i< names.length; i++) {
				debug("Loading tool "+names[i] );
				mVueTools[i] = loadTool( names[i] );
				}
			}
	}
	
	/**
	 * loadTool
	 * This method loads a VueTool with the bivien name from the
	 * vue properties file.
	 *
	 **/
	public VueTool loadTool( String pName) {
		
		String className = VueResources.getString( pName+".class") ;
		VueTool tool = null;
		
		try {
			Class toolClass = getClass().getClassLoader().loadClass( className);
			
			tool = (VueTool) toolClass.newInstance();
					
			// set the tool's properties...
			tool.setID( pName);
			tool.setToolName( VueResources.getString(( pName+".name") ) );
			tool.setToolTipText( VueResources.getString( pName + ".tooltip") );
			
			tool.setIcon( VueResources.getImageIcon( pName+".up"));
			tool.setDownIcon( VueResources.getImageIcon( pName+".down"));
			tool.setSelectedIcon( VueResources.getImageIcon(pName+".selected"));
			tool.setDisabledIcon( VueResources.getImageIcon(pName+".disabled"));
			tool.setRolloverIcon( VueResources.getImageIcon( pName+".rollover"));
			tool.setMenuItemIcon( VueResources.getImageIcon( pName+".menu"));
			tool.setMenuItemSelectedIcon( VueResources.getImageIcon( pName+".menuselected"));
			tool.setShortcutKey(VueResources.getChar( pName+".shortcutKey"));

                        int cursorID = VueResources.getInt(pName+".cursorID", -1);
                        if (cursorID >= 0) {
                            tool.setCursorByID(cursorID);
                        } else {
                            ImageIcon icon = VueResources.getImageIcon( pName+".cursor");
                            if (icon != null) {
                                Toolkit toolkit = Toolkit.getDefaultToolkit();
                                //System.out.println("Creating cursor for " + icon);
                                tool.setCursor(toolkit.createCustomCursor(icon.getImage(), new Point(0,0), pName));
                            }
                        }
			
			String subtools[];
			String defaultTool = null;
			subtools = VueResources.getStringArray( pName+".subtools");
			if( subtools != null) {
				
				tool.setOverlayUpIcon( VueResources.getImageIcon( pName+".overlay"));
				tool.setOverlayDownIcon( VueResources.getImageIcon( pName+".overlaydown") );
			
				for(int i=0; i<subtools.length; i++) {
					VueTool subTool = this.loadTool( pName+"."+subtools[i] );
					subTool.setParentTool( tool);
					tool.addSubTool(subTool);
					}
				// load menu overlays (if any)
				
				// setup default tool (if any)
				defaultTool = VueResources.getString( pName+".defaultsubtool");
				if( defaultTool == null) {
					defaultTool = subtools[0];
					}
				
				
					
				VueTool dst = tool.getSubTool( pName + "."+defaultTool );
				if( dst != null) {
					tool.setIcon( dst.getIcon() );
					tool.setDownIcon( dst.getDownIcon() );
					tool.setSelectedIcon( dst.getSelectedIcon() );
					tool.setRolloverIcon( dst.getRolloverIcon() );
					tool.setDisabledIcon( dst.getDisabledIcon() );
					tool.setSelectedSubTool( dst);
					}
				else {
					// should never happen unless bad properties file
					debug("  !!! Error: missing subtool: "+defaultTool );
					
					}
				// tool.set
				}
			} catch (Exception e) {
				debug("loadTool() exception:");
				e.printStackTrace();
			}
			
		return tool;
	}
	
	/**
	 * createDefaultToolbar()
	 *
	 * A factory method to generate the toolbar
	 **/
	public VueToolPanel createDefaultToolbar() {
		VueToolPanel toolbar = new VueToolPanel();
		toolbar.addTools( mVueTools );
		
		return toolbar;
	}
	
	/**
	 * getController()
	 *
	 * @return the static controller
	 **/
	 static public VueToolbarController getController() {

		return sController;
	 }

	
	static ToolWindow sToolWindow = null;
	
	/**
	 * getToolWindow()
	 * Returns the default toolwindow for the tools and sub tools
	 *
	 **/
	public ToolWindow getToolWindow() {
		return sToolWindow;
	}
	public void setToolWindow( ToolWindow pWindow ) {
		sToolWindow = pWindow;
	}
	
	/**
	 * getSelectedTool()
	 * This method returns the selected VueTool
	 * @returns VueTool
	 **/
	 public VueTool getSelectedTool() {
	 	VueTool tool = null;
	 	
	 	if( getToolbar() != null) {
	 		tool = getToolbar().getSelectedTool();
	 		}
	 	return tool;
	 }
	 
	 /**
	  * setSelectedTool()
	  * This method sets teh slected VueTool
	  * @param VeuTool - selected tool
	  **/
	  public void setSelectedTool( VueTool pTool) {
	  	mSelectedTool = pTool;
	  }
	
	
	/**
	 * getTool
	 * This returns the active instance of a known tool, if active and availabe.
	 * @param id java.lang.STring id of the tool
	 **/
	public VueTool getTool( String pID) {
		
		for( int i=0; i<mVueTools.length; i++) {
			if( mVueTools[i].getID().equals( pID) ) {
				return mVueTools[i];
				}
			}
		return null;
	}
	
	/**
	 * getToolbar
	 * 
	 * This method gets teh toolbar for a given Map object.
	 * If there is not currently a toolbar for the upa, it
	 * will build one.
	 *
	 * @param Map the map
	 * @return Jpanel the toolbar panel
	 *
	 **/
	public VueToolPanel getToolbar( ) {
		
		return mToolPanel;
	}
		
	
	
	public JPanel getContextualPanel() { 
		return mContextualPanel;
	}
	
	public void setContextualPanel( JPanel pPanel) {
		mContextualPanel = pPanel;
	}
	
	

	/**
	 *  updateToolbar
	 *  This method will update teh existing toolbar of a Map.
	 * You should callt his if the prefs or set of tools changes for a map
	 *
	 * @param Map the Map object
	 **/
	 public void setEnabledToolsFromMap( LWMap pMap) {
	 		 	
	 	// FIX
	 	//  update to just remove/disable
	 	
	 }
	 
	  
	  /**
	   * addToolSelectionListener
	   * Adds a VueToolSelectionListener to receive notification when
	   * a tool is activated.
	   * @param VueToolSelectionListener the listener object
	   **/
	  public void addToolSelectionListener( VueToolSelectionListener pListener) {
	  	mToolSelectionListeners.add( pListener);
	  }
	  
	  public void removeToolSelectionListener( VueToolSelectionListener pListener) {
	  	mToolSelectionListeners.remove( pListener );
	  }
	  
	  
	 /**
	  * handleToolSelection
	  * This method is called when a tool selection event happens.
	  * It will notify all toolbar
	  *
	  **/
	  public void handleToolSelection( VueTool pTool) {
		
		VueTool rootTool = pTool;
		
		if( pTool.getParentTool() != null ) {
			rootTool = pTool.getParentTool();
			}
		
		// check to see if it is the same selection...
		String selectionID = pTool.getSelectionID();
		if( mCurSelectionID.equals( selectionID) ) {
			return;
			}
			
		// new tool selection...
	  	debug(">>> new tool selection from: "+mCurSelectionID+ " to: "+selectionID );
		mCurSelectionID = selectionID;	  	

		// update contextual panel
		getToolbar().setContextualToolPanel( rootTool.getContextualPanel() );
		// notify listeners
		int size = mToolSelectionListeners.size();
		for(int i=0; i<size; i++) {
	  		VueToolSelectionListener listener= (VueToolSelectionListener) mToolSelectionListeners.get(i);
	  		listener.toolSelected( rootTool);
	  		}
	  }
	 
	 /**
	  * handleMapEvent
	  *
	  **/
	  public void handleMapEvent( Event pEvent) {
	  	
	  	// FIX: This handles selection events...
	  
	  }
	
 	
 	/**
 	 * selectionChanged
 	 * The implemenation of the LWSelection.Listener
 	 *
 	 * This method updates toolbars based on the new selection
 	 **/
 	public void selectionChanged( LWSelection pSelection)  {
 		debug("!!! ToolbarController checkign contextual tools");
 	}


	private void debug(String pMsg) {
		if( sDebug )
			System.out.println( pMsg);
	}
}
