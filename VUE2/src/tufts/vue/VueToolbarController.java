 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Track the selection and change the current contextual tool panel as appropriate,
 * as well as relaying selection events to the current contextual tool panel.
 * Also maintin list of VueToolSelectionListeners, for those who want to know
 * when the currently active tool changes.
 * 
 * This could use a re-write, along with VueToolPanel, VueTool, and the way
 * contextual toolbars are handled.
 *
 * @version $Revision: 1.46 $ / $Date: 2005-11-27 14:55:55 $ / $Author: sfraize $
 *
 **/
public class VueToolbarController  
    implements LWSelection.Listener, LWComponent.Listener
{
    /** the list of tool names is under this key in the resources **/
    public static final String DefaultToolsKey = "defaultToolNames";
    private static VueToolbarController sController;
	
	
    private VueToolPanel mToolPanel = null;
	
    /** a list of tool selection listeners **/
    private Vector mToolSelectionListeners = new Vector();
	
    /** a list of available tools **/
    private VueTool[] mVueTools = null;
	
    /** current contextual panel **/
    private JPanel mContextualPanel = null;
	
    // the currently selected tool 
    private VueTool mSelectedTool = null;
	
    // current selection id
    //private String mCurSelectionID = new String();

    private LWCToolPanel mLWCToolPanel;
	
	
    /**
     * @return the singleton controller
     **/
    static public VueToolbarController getController() {
        if (sController == null)
            sController = new VueToolbarController();
        return sController;
    }

    static public VueTool getActiveTool() {
        if (sController == null)
            return null;
        else
            return sController.getSelectedTool();
    }

	
    /**
     * Load the tools as defined in the resource properties file, and
     * create the VueToolPanel.
     **/
    protected VueToolbarController() {
        loadTools();
        mToolPanel = createDefaultToolbar();
		
    }

    /** @return array of all the top-level VueTool's */
    public VueTool[] getTools() {
        return mVueTools;
    }

    private LWCToolPanel getLWCToolPanel()
    {
        if (mLWCToolPanel == null)
            mLWCToolPanel = new LWCToolPanel();
        return mLWCToolPanel;
    }
	
	
    /**
     * Loads the default tools specified in a resource file
     **/
    protected synchronized void loadTools()
    {
        String [] names = VueResources.getStringArray(DefaultToolsKey);
        if( names != null) {
            mVueTools = new VueTool[ names.length];
            for( int i=0; i< names.length; i++) {
                debug("Loading tool "+names[i] );
                mVueTools[i] = loadTool( names[i] );
            }
        }
    }
	
    /**
     * This method loads a VueTool with the given name from the
     * vue properties file.
     **/
    private VueTool loadTool(String pName)
    {
        final String classKey = pName + ".class";
        final String className = VueResources.getString(classKey);

        if (className == null) {
            System.err.println(this + " loadTool["
                               + pName + "]; missing class key in resources: [" + classKey + "]");
            return null;
        }
		
        VueTool tool = null;

        try {
            //if (DEBUG.Enabled) System.out.println("Loading tool class " + className);
            Class toolClass = getClass().getClassLoader().loadClass(className);

            if (DEBUG.INIT) System.out.println("Loading tool " + pName + " " + toolClass);
            tool = (VueTool) toolClass.newInstance();

            // set the tool's properties...
            
            tool.setID(pName);
            tool.initFromResources();

            String subtools[];
            String defaultTool = null;
            subtools = VueResources.getStringArray( pName+".subtools");
            if (subtools != null) {
				
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
     * A factory method to generate the toolbar.  Tools must already
     * be loaded before calling.
     **/
    private VueToolPanel createDefaultToolbar() {
        VueToolPanel toolbar = new VueToolPanel();
        toolbar.addTools(mVueTools);
        return toolbar;
    }
	
	
    /**
     * This method returns the selected VueTool
     * @returns currently active VueTool
     **/
    public VueTool getSelectedTool() {
        if (getToolbar() == null)
            return null;
        else
            return getToolbar().getSelectedTool();
    }
	 
    /**
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
     * @return the instance of a known tool
     * @param pID id (name) of the tool
     **/
    public VueTool getTool(String pID) {
        for( int i=0; i<mVueTools.length; i++) {
            if (mVueTools[i].getID().equals(pID)) {
                return mVueTools[i];
            }
        }
        return null;
    }
	
    /**
     * @return the VueToolPanel
     **/
    public VueToolPanel getToolbar() {
        return mToolPanel;
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
     * This method is called when a tool selection event happens.
     * It will notify all toolbar
     **/
    protected void handleToolSelection(VueTool pTool)
    {
        if (DEBUG.TOOL) out("handleToolSelection " + pTool);
        
        VueTool rootTool = pTool;
		
        if (pTool.getParentTool() != null)
            rootTool = pTool.getParentTool();
		
        String selectionID = pTool.getSelectionID();
        
        /*
        // check to see if it is the same selection...
        if (mCurSelectionID.equals(selectionID)) {
            if (DEBUG.TOOL) out("same selection: noop");
            return;
        }
        if (rootTool == mSelectedTool) {
            if (DEBUG.TOOL) out("same root tool: noop");
            return;
        }
        */
        // allow re-selection of existing tool: handy for use as "apply" to a current selection
        // if the changing the active sub-tool can have an effect on selected objects
        //mCurSelectionID = selectionID;	  	

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
        for (int i = 0; i < size; i++) {
            VueToolSelectionListener listener = (VueToolSelectionListener) mToolSelectionListeners.get(i);
            if (DEBUG.TOOL) System.out.println("\tVueToolbarController -> toolSelected " + listener);
            listener.toolSelected(rootTool);
        }

        mSelectedTool = rootTool;
        mSelectedTool.handleToolSelection();
    }
	 
    /**
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
     * @return the suggest tool panel to use based on the current state
     * of the controller (selected tool) and the map selection.
     **/
    // todo: the tool itself should be able to specify this -- at least for single selection
    public JPanel getSuggestedContextualPanel() {
        JPanel panel = null;
        VueToolbarController controller = VueToolbarController.getController();
        LWSelection selection = VUE.getSelection();
        VueTool tool = null;
	 	
        if( (!selection.isEmpty() ) && ( selection.allOfSameType()) ) {
            LWComponent c = (LWComponent) selection.get(0);
            if (c instanceof LWNode) {
                if ( false && ((LWNode)c).isTextNode())
                    tool = controller.getTool("textTool");
                else
                    tool = controller.getTool("nodeTool" );
            } else if (c instanceof LWLink) {
                tool = controller.getTool("linkTool");
            } else if (c instanceof LWImage) {
                tool = controller.getTool("imageTool");
            } else
                tool = controller.getTool("nodeTool" ); // make node tool the default for now
                
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
                if ( false && ((LWNode) c).isTextNode())
                    panel = TextTool.getTextToolPanel();
                else
                    panel = NodeTool.getNodeToolPanel();
            } else
                if (c instanceof LWLink)
                    panel = LinkTool.getLinkToolPanel();
            
        } else {
            panel = getLWCToolPanel();
            //panel = null;
        }
        if (DEBUG.SELECTION) System.out.println("getContextualPanelForSelection returning " + panel);
        return panel;
    }
	 
    private void initContextualPanelFromSelection(JPanel panel) {
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
        
        // Maybe better: interested tool panels should listen to selection themselves
        // The only value we get from this right now is that only the active tool panel
        // will get loadValues called, letting inactive ones keep their state, which is
        // handy for some cases, but as long as the tool could know if it's active or not,
        // we'd have all the functionality we need, and in a much simpler & more effective
        // manner.
        
        if (panel instanceof LWCToolPanel)
            ((LWCToolPanel)panel).loadSelection(VUE.getSelection());
        else {
            if (DEBUG.TOOL) System.out.println(this + " IGNORING initContextualPanelFromSelection on non LWCToolPanel " + panel);
        }
    }
	 
    /**
     * The implemenation of the LWSelection.Listener
     * This method updates toolbars based on the new selection
     **/
    private LWComponent singleSelection = null;
    public void selectionChanged(LWSelection s) {
        
        if (s.size() == 1) {
            if (singleSelection != null)
                singleSelection.removeLWCListener(this);
            singleSelection = s.first();
            //loadValues(singleSelection);
            singleSelection.addLWCListener(this);
        } else if (s.size() > 1) {
            // todo: if we are to populate the tool bar properties when
            // there's a multiple selection, what do we use?
            //loadValues(s.first());
        }
        if (s.size() != 1 && singleSelection != null) {
            singleSelection.removeLWCListener(this);
            singleSelection = null;
        }

        updateContextualToolPanel();
        if (singleSelection != null)
            loadToolValue(null, singleSelection);
    }

    public void LWCChanged(LWCEvent e) {
        loadToolValue(e.getWhat(), e.getComponent());
    }

    /**
     * Load the value from src, indicated by propertyKey, into any of our tool's that are LWPropertyProducer's.
     * If propertyKey is null, all producers will attempt to load what there interested in, even if
     * the object doesn't support the property (in which case the value will be null and should be ignored).
     */
    // todo: cache the property producers
    private void loadToolValue(Object propertyKey, LWComponent src)
    {
        for (int i = 0; i < mVueTools.length; i++) {
            if (mVueTools[i] instanceof LWPropertyProducer) {
                LWPropertyProducer p = (LWPropertyProducer) mVueTools[i];
                if (propertyKey == null || propertyKey == p.getPropertyKey()) {
                    Object value = src.getPropertyValue(p.getPropertyKey());
                    if (DEBUG.TOOL) out("loadToolValue: loading producer "
                                        + p
                                        + " key=" + p.getPropertyKey()
                                        + " with val=" + value
                                        + " from " + src);
                    p.setPropertyValue(value);
                }
            }
        }
    }


    private void out(String s) {
        System.out.println(this + " " + s);
    }

    public String toString() {
        return "VueToolbarController[" + mSelectedTool + "]";
    }
    
    private void debug(String pMsg) {
        if( sDebug )
            System.out.println( pMsg);
    }

    private static boolean sDebug = false;
	
    public static void main(String[] args) {
        System.out.println("VueToolbarController:main");
        DEBUG.Enabled = true;
        //DEBUG.INIT = true;
        DEBUG.TOOL = true;
        VUE.parseArgs(args);
        VUE.initUI(true);

        FontEditorPanel.sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" }; // so doesn't bother to load system fonts
        VueToolbarController controller = getController();

        //JComponent comp = controller.createDefaultToolbar();
        VueToolPanel comp = controller.mToolPanel;
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
