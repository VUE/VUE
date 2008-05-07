 /*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.util.*;
import tufts.vue.LinkTool.LinkModeTool;


/**
 * Track the selection and change the current contextual tool panel as appropriate,
 * as well as relaying selection events to the current contextual tool panel.
 * Also maintin list of VueToolSelectionListeners, for those who want to know
 * when the currently active tool changes.
 * 
 * This could use a re-write, along with VueToolPanel, VueTool, and the way
 * contextual toolbars are handled.
 *
 * @version $Revision: 1.75 $ / $Date: 2008-05-07 18:40:18 $ / $Author: sfraize $
 *
 **/
public class VueToolbarController  
    implements LWSelection.Listener, LWComponent.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueToolbarController.class);
    
    /** the list of tool names is under this key in the resources **/
    public static final String DefaultToolsKey = "mainToolbarToolNames";
    private static VueToolbarController sController;
	
	
    private VueToolPanel mToolPanel = null;
	
    /* a list of tool selection listeners 
    private Vector mToolSelectionListeners = new Vector();
    */
	
    /** a list of available tools **/
    private VueTool[] mVueTools = null;
    private int[] separatorPositions = null;
    /* current contextual panel **/
    //private JPanel mContextualPanel = null;
	
    // the currently selected tool 
    private VueTool mSelectedTool = null;

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
        mVueTools = VueToolUtils.loadTools(DefaultToolsKey);
        separatorPositions = VueToolUtils.getSeparatorPositions(DefaultToolsKey);
        mToolPanel = createDefaultToolbar();
        VUE.addActiveListener(VueTool.class, this);

        configureOntologyTools();
        }

    /** @return array of all the top-level VueTool's */
    public VueTool[] getTopLevelTools() {
        return mVueTools;
    }
    
   /* private LWCToolPanel getLWCToolPanel()
    {
        if (mLWCToolPanel == null)
            mLWCToolPanel = new LWCToolPanel();
        return mLWCToolPanel;
    }
	*/	    	

    /**
     * There's no good way to dynamically control tools, and if there's a real case for doing this, 
     * there probably needs to be a better way.  But I'm not even sure this will stay around so this
     * is a bit of hack to allow it.
     */
    private Component ontologyNodeComponent;
    private Component ontologyLinkComponent;
    
    private void configureOntologyTools()
    {
    	final VueTool nodeTool = VueTool.getInstance(tufts.vue.NodeTool.NodeModeTool.class);
        final VueTool linkTool = VueTool.getInstance(tufts.vue.LinkTool.LinkModeTool.class);
        final VueTool ontologyNodeTool = VueTool.getInstance(tufts.vue.NodeTool.OntologyNodeTool.class);
        final VueTool ontologyLinkTool = VueTool.getInstance(tufts.vue.LinkTool.OntologyLinkModeTool.class);
        
        final int ONTOLOGY_NODE_POSITION=1;
        final int ONTOLOGY_LINK_POSITION=1;
        
        Map buttons = getToolbar().getToolButtons();
        PaletteButton parentButton = (PaletteButton)buttons.get(nodeTool.getParentTool().getID());
        PaletteButton parentLinkButton = (PaletteButton)buttons.get(linkTool.getParentTool().getID());
        Component[] c = parentButton.mPopup.getComponents();
        ontologyNodeComponent = c[ONTOLOGY_NODE_POSITION];
        
        c = parentLinkButton.mPopup.getComponents();
        ontologyLinkComponent = c[ONTOLOGY_LINK_POSITION];
        
        parentButton.removePopupComponent(ontologyNodeComponent);
        parentLinkButton.removePopupComponent(ontologyLinkComponent);
     
        edu.tufts.vue.ontology.ui.OntologyBrowser.getBrowser().getDockWindow().addComponentListener(new ComponentAdapter()
        {

			public void componentHidden(ComponentEvent arg0) {
				hideOntologicalTools();
			}

			public void componentShown(ComponentEvent arg0) {				
			
				showOntologicalTools();
			}
        	
        });

    }
    
    public void showOntologicalTools()
    {
    	final VueTool nodeTool = VueTool.getInstance(tufts.vue.NodeTool.NodeModeTool.class);
        final VueTool linkTool = VueTool.getInstance(tufts.vue.LinkTool.LinkModeTool.class);
        
    	Map buttons = getToolbar().getToolButtons();
        PaletteButton parentButton = (PaletteButton)buttons.get(nodeTool.getParentTool().getID());		        
        PaletteButton parentLinkButton = (PaletteButton)buttons.get(linkTool.getParentTool().getID());

        parentButton.addPopupComponent(ontologyNodeComponent);
        parentLinkButton.addPopupComponent(ontologyLinkComponent);
        parentButton.repaint();
        parentLinkButton.repaint();

    }
    public void hideOntologicalTools()
    {
    	final VueTool nodeTool = VueTool.getInstance(tufts.vue.NodeTool.NodeModeTool.class);
        final VueTool linkTool = VueTool.getInstance(tufts.vue.LinkTool.LinkModeTool.class);
        final Component ontologyNodeComponent;
        final Component ontologyLinkComponent;
        final int ONTOLOGY_NODE_POSITION=1;
        final int ONTOLOGY_LINK_POSITION=1;
        
        Map buttons = getToolbar().getToolButtons();
        PaletteButton parentButton = (PaletteButton)buttons.get(nodeTool.getParentTool().getID());
        PaletteButton parentLinkButton = (PaletteButton)buttons.get(linkTool.getParentTool().getID());
        Component[] c = parentButton.mPopup.getComponents();
        ontologyNodeComponent = c[ONTOLOGY_NODE_POSITION];
        
        c = parentLinkButton.mPopup.getComponents();
        ontologyLinkComponent = c[ONTOLOGY_LINK_POSITION];
        
    	
        parentButton.removePopupComponent(ontologyNodeComponent);
        parentLinkButton.removePopupComponent(ontologyLinkComponent);
        
        VueTool tool = getToolbar().getSelectedTool();
        //System.out.println(nodeTool.getID());
        if (tool.getID().equals(nodeTool.getParentTool().getID()))
        {
        	getToolbar().setSelectedTool(linkTool);
        	getToolbar().setSelectedTool(nodeTool);
        	nodeTool.setSelectedSubTool(nodeTool);
        	setSelectedTool(nodeTool);		        
        }
        else if (tool.getID().equals(linkTool.getParentTool().getID()))
        {
        	getToolbar().setSelectedTool(nodeTool);
        	getToolbar().setSelectedTool(linkTool);
        	nodeTool.setSelectedSubTool(linkTool);
        	setSelectedTool(linkTool);
        }
        else
        {
        	getToolbar().setSelectedTool(nodeTool);
        	getToolbar().setSelectedTool(linkTool);
        	getToolbar().setSelectedTool(tool);
        }
        
        
        
        parentButton.repaint();
        parentLinkButton.repaint();
    }
    /**
     * A factory method to generate the toolbar.  Tools must already
     * be loaded before calling.
     **/
    private VueToolPanel createDefaultToolbar() {
        VueToolPanel toolbar = new VueToolPanel();
        toolbar.addTools(mVueTools,separatorPositions);
        toolbar.addTool(PresentationTool.getTool(), false);
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
    public void setSelectedTool(VueTool pTool) {

        final VueTool activeTool = getSelectedTool();

        if (DEBUG.TOOL) Log.debug("setSelectedTool: " + pTool);

        if (activeTool != null && !activeTool.permitsToolChange()) {
            Log.warn("tool change not permitted to: " + pTool + "; active tool denies: " + activeTool);
            //return;
        }
            

        if( mToolPanel != null) {
            mToolPanel.setSelectedTool( pTool);
        }
        handleToolSelection( pTool);
    }
	
    public void activeChanged(ActiveEvent e, VueTool tool) {
        setSelectedTool(tool);
    }
	
    /**
     * @return the instance of a known tool
     * @param pID id (name) of the tool
     **/
/*    public VueTool getTool(String pID) {
        for (VueTool t : mAllTools)
            if (t.getID().equals(pID))
                return t;
        tufts.Util.printStackTrace("failed to find tool [" + pID + "]");
        return null;
    }
	*/
    /**
     * @return the VueToolPanel
     **/
    public VueToolPanel getToolbar() {
        return mToolPanel;
    }
		

    /*
     * addToolSelectionListener
     * Adds a VueToolSelectionListener to receive notification when
     * a tool is activated.
     * @param VueToolSelectionListener the listener object
     *
    public void addToolSelectionListener( VueToolSelectionListener pListener) {
        mToolSelectionListeners.add( pListener);
    }
	  
    public void removeToolSelectionListener( VueToolSelectionListener pListener) {
        mToolSelectionListeners.remove( pListener );
    }
    */
	  
	  
    /**
     * This method is called when a tool selection event happens.
     * It will notify all toolbar
     **/
    protected void handleToolSelection(VueTool pTool)
    {
        if (DEBUG.TOOL) out("handleToolSelection " + pTool);
        
        VueTool rootTool = pTool;
		
        // TODO: need to seriously refactor this code: the tools shouldn't be self activating
        // -- e.g., their installing their cursor even if the tool change is denied, so
        // we're going to allow the tool selection for now...
        if (mSelectedTool != null && !mSelectedTool.permitsToolChange()) {
            Log.warn("tool change not permitted to: " + pTool + "; active tool denies: " + mSelectedTool);
            //return;
        }
        
       if (pTool.getParentTool() != null && pTool.getClass() == tufts.vue.VueSimpleTool.class)          
        		rootTool = pTool.getParentTool();
		
    //    String selectionID = pTool.getSelectionID();
        
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
        //updateContextualToolPanel();
        /** OLD CODE
            JPanel contextualPanel = rootTool.getContextualPanel();
            if( contextualPanel == null) {
            contextualPanel = getContextualToolForSelection();
            }
            getToolbar().setContextualToolPanel( contextualPanel );
            END OLD CODE ****/
		
        final VueTool oldActive = mSelectedTool;

        
        
        mSelectedTool = rootTool;

        // notify listeners
        VUE.setActive(VueTool.class, this, rootTool);
        
        if (oldActive != null && oldActive != mSelectedTool)
            oldActive.handleToolSelection(false, mSelectedTool);
        mSelectedTool.handleToolSelection(true, oldActive);
        
        /*
        int size = mToolSelectionListeners.size();
        for (int i = 0; i < size; i++) {
            VueToolSelectionListener listener = (VueToolSelectionListener) mToolSelectionListeners.get(i);
            if (DEBUG.TOOL) System.out.println("\tVueToolbarController -> toolSelected " + listener);
            listener.toolSelected(rootTool);
        }
        */

    }

	 
    /*
     * This method checks to see if the current tool has
     * a contextual tool.  If so, it uses that panel.  If
     * not it uses the selection to get a panel.
     **
    private void updateContextualToolPanel() {
        VueTool tool = getSelectedTool();
        if (tool.getParentTool() != null)
            tool = tool.getParentTool();

        JPanel panel = tool.getContextualPanel();
		
        if (panel == null)
            panel = getContextualPanelForSelection();

        if (panel != null)
            initContextualPanelFromSelection(panel);

   //     getToolbar().setContextualToolPanel(panel);
    }
    */
	 
    public JPanel getSuggestedContextualPanel() { return null; }
	 
    /*
     * @return the suggest tool panel to use based on the current state
     * of the controller (selected tool) and the map selection.
     **
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
                else
                    panel = NodeTool.getNodeToolPanel(); // the generic default for now
            
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
        *
        
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
    */
	 
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

        //updateContextualToolPanel();
        if (singleSelection != null)
            loadToolValue(null, singleSelection);
    }

    public void LWCChanged(LWCEvent e) {
        loadToolValue(e.key, e.component);
    }

    /**
     * Load the value from src, indicated by propertyKey, into any of our tool's that are LWPropertyProducer's.
     * If propertyKey is null, all producers will attempt to load what there interested in, even if
     * the object doesn't support the property (in which case the value will be null and should be ignored).
     */
    // todo: cache the property producers
    private void loadToolValue(Object propertyKey, LWComponent src)
    {
        if (src == null)
            return;
        
        for (VueTool tool : mVueTools) {
            if (tool instanceof LWEditor) {
                LWEditor editor = (LWEditor) tool;
                if (propertyKey == null)
                    propertyKey = editor.getPropertyKey();
                else if (propertyKey != editor.getPropertyKey())
                    continue;
                
                if (src.supportsProperty(propertyKey)) {
                    Object value = src.getPropertyValue(editor.getPropertyKey());
                    if (DEBUG.TOOL) out("loadToolValue: loading producer "
                                        + editor
                                        + " key=" + editor.getPropertyKey()
                                        + " with val=" + value
                                        + " from " + src);
                    editor.displayValue(value);
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

        VUE.init(args);
        
        
        tufts.Util.displayComponent(getController().mToolPanel);

        /*
        //FontEditorPanel.sFontNames = new String[] { "Lucida Sans Typewriter", "Courier", "Arial" };
        // so doesn't bother to load system fonts
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
        */
    }
}
