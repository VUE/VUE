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
	
    static private VueToolbarController sController;
	
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

    private LWCToolPanel mLWCToolPanel;
	
	
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

    private LWCToolPanel getLWCToolPanel()
    {
        if (mLWCToolPanel == null)
            mLWCToolPanel = new LWCToolPanel();
        return mLWCToolPanel;
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

            System.out.println("Loading tool " + pName + " " + toolClass);
            tool = (VueTool) toolClass.newInstance();
					
            // set the tool's properties...
            tool.setID( pName);
            tool.setToolName( VueResources.getString(( pName+".name") ) );
            tool.setToolTipText( VueResources.getString( pName + ".tooltip") );

            Icon rawIcon = VueResources.getImageIcon(pName+".raw");
            if (rawIcon != null) {
                tool.setGeneratedIcons(rawIcon);
            } else {
                Icon i;
                if ((i = VueResources.getImageIcon(pName+".up")) != null)
                    tool.setIcon(i);
                if ((i = VueResources.getImageIcon(pName+".down")) != null)
                    tool.setDownIcon(i);
                if ((i = VueResources.getImageIcon(pName+".selected")) != null)
                    tool.setSelectedIcon(i);
                if ((i = VueResources.getImageIcon(pName+".disabled")) != null)
                    tool.setDisabledIcon(i);
                if ((i = VueResources.getImageIcon(pName+".rollover")) != null)
                    tool.setRolloverIcon(i);
                if ((i = VueResources.getImageIcon(pName+".menu")) != null)
                    tool.setMenuItemIcon(i);
                if ((i = VueResources.getImageIcon(pName+".menuselected")) != null)
                    tool.setMenuItemSelectedIcon(i);
            }

            tool.setShortcutKey(VueResources.getChar( pName+".shortcutKey"));

            int cursorID = VueResources.getInt(pName+".cursorID", -1);
            if (cursorID >= 0) {
                //System.out.println(tool + " found cursor ID: " + cursorID);
                tool.setCursorByID(cursorID);
            } else {
                Cursor cursor = VueResources.getCursor(pName+".cursor");
                if (cursor != null)
                    tool.setCursor(cursor);
                /*
                ImageIcon icon = VueResources.getImageIcon( pName+".cursor");
                if (icon != null) {
                    //System.out.println(tool + " found cursor icon: " + icon);
                    //System.out.println(tool + " cursor icon image: " + icon.getImage());
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    //System.out.println("Creating cursor for " + icon);
                    tool.setCursor(toolkit.createCustomCursor(icon.getImage(), new Point(0,0), pName+":"+icon.toString()));
                }
                */
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
        if (sController == null)
            sController = new VueToolbarController();
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
     * It will attempt to update teh button group in the Toolbar
     * and notify listners by calling selectionChanged.
     *
     * @param VeuTool - selected tool
     **/
    public void setSelectedTool( VueTool pTool) {

        if( mToolPanel != null) {
            mToolPanel.setSelectedTool( pTool);
        }
        handleToolSelection( pTool);
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
    protected void handleToolSelection( VueTool pTool) {
		
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
        updateContextualToolPanel();
        /** OLD CODE
            JPanel contextualPanel = rootTool.getContextualPanel();
            if( contextualPanel == null) {
            contextualPanel = getContextualToolForSelection();
            }
            getToolbar().setContextualToolPanel( contextualPanel );
            END OLD CODE ****/
		
        // notify listeners
        int size = mToolSelectionListeners.size();
        for(int i=0; i<size; i++) {
            VueToolSelectionListener listener= (VueToolSelectionListener) mToolSelectionListeners.get(i);
            listener.toolSelected( rootTool);
        }
    }
	 
    /**
     * updateContexxtualTools
     * This method checks to see if the current tool has
     * a contextual tool.  If so, it uses that panel.  If
     * not it uses the selection to get a panel.
     **/
    private void updateContextualToolPanel() {
        VueTool tool = getSelectedTool();
        if (tool.getParentTool() != null)
            tool = tool.getParentTool();

        JPanel panel = tool.getContextualPanel();
		
        if (panel == null)
            panel = getContextualPanelForSelection();

        if (panel != null)
            initContextualPanelFromSelection(panel);
        
        getToolbar().setContextualToolPanel(panel);
    }
	 
	 
    /**
     * getSuggestedContextualPanel()
     * Retusn the suggest tool panel to use based on the current state
     * of the controller and map.
     **/
    public JPanel getSuggestedContextualPanel() {
        JPanel panel = null;
        VueToolbarController controller = VueToolbarController.getController();
        LWSelection selection = VUE.getSelection();
        VueTool tool = null;
	 	
        if( (!selection.isEmpty() ) && ( selection.allOfSameType()) ) {
            LWComponent c = (LWComponent) selection.get(0);
            if (c instanceof LWNode) {
                if (((LWNode)c).isTextNode())
                    tool = controller.getTool( "textTool");
                else
                    tool = controller.getTool( "nodeTool" );
            } else
                if (c instanceof LWLink)
                    tool = controller.getTool( "linkTool");
        }
        if (tool != null)
            panel = tool.getContextualPanel();
        if (DEBUG.SELECTION) System.out.println("getSuggestedContextualPanel returning " + panel);
        return panel;
    }
	 
     
    private JPanel getContextualPanelForSelection() {
        JPanel panel = null;
        LWSelection selection = VUE.getSelection();
        // TODO BUG: LWSelection.allOfSameType doesn't distinguish between text nodes & regular nodes!
        // if we end up keeping text nodes around, go make them a real subclass
        if (selection.size() > 0 && selection.allOfSameType()) {
            LWComponent c = selection.first();
            if (c instanceof LWNode) {
                if( ((LWNode) c).isTextNode())
                    panel = TextTool.getTextToolPanel();
                else
                    panel = NodeTool.getNodeToolPanel();
            } else
                if (c instanceof LWLink)
                    panel = LinkTool.getLinkToolPanel();
            
        } else {
            panel = getLWCToolPanel();
        }
        if (DEBUG.SELECTION) System.out.println("getContextualPanelForSelection returning " + panel);
        return panel;
    }
	 
    private void initContextualPanelFromSelection( JPanel panel) {
        /*
        LWSelection selection = VUE.getSelection();
        LWComponent c = null;
        if (selection != null && selection.size() > 0) {
            // TODO: something more sophisticated than just loading
            // all the props from the first item in the selection and
            // disregarding all the others.
            c = selection.first();
        } else 
            return;
        */
        
        if (panel instanceof LWCToolPanel)
            ((LWCToolPanel)panel).loadValues(VUE.getSelection());
        else {
            if (DEBUG.Enabled)
                System.out.println(this + " IGNORING initContextualPanelFrom Selection on unknown JPanel " + panel);
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
        updateContextualToolPanel();
        debug("!!! ToolbarController checkign contextual tools");
    }


    private void debug(String pMsg) {
        if( sDebug )
            System.out.println( pMsg);
    }

    public static void main(String[] args) {
        System.out.println("VueToolbarController:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.initUI(true);

        FontEditorPanel.sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts
        VueToolbarController controller = getController();

        //JComponent comp = controller.createDefaultToolbar();
        VueToolPanel comp = controller.createDefaultToolbar();
        comp.setContextualToolPanel(new NodeToolPanel());

        JFrame frame = new JFrame(comp.getClass().getName());
        comp.setSize(comp.getPreferredSize());
        frame.setContentPane(comp);
        frame.pack();
        frame.validate();
        VueUtil.centerOnScreen(frame);
        frame.show();
    }
     
}



     // Contextual tool panels for various selection types 
     //static private NodeToolPanel sNodeSelectionContextualPanel;
     //static private TextToolPanel sTextSelectionContextualPanel;
     //static private LinkToolPanel sLinkSelectionContextualPanel;
     /*
     private static NodeToolPanel getNodeSelectionContextualPanel()
     {
         if (sNodeSelectionContextualPanel == null)
             sNodeSelectionContextualPanel = new NodeToolPanel();
         return sNodeSelectionContextualPanel;
     }

     private static LinkToolPanel getLinkSelectionContextualPanel()
     {
         if (sLinkSelectionContextualPanel == null)
             sLinkSelectionContextualPanel = new LinkToolPanel();
         return sLinkSelectionContextualPanel;
     }
     private static TextToolPanel getTextSelectionContextualPanel()
     {
         if (sTextSelectionContextualPanel == null)
             sTextSelectionContextualPanel = new TextToolPanel();
         return sTextSelectionContextualPanel;
     }
     */
