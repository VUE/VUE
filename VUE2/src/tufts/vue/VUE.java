/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.InputSource;

import tufts.Util;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.action.AboutAction;
import tufts.vue.action.ExitAction;
import tufts.vue.action.OpenAction;
import tufts.vue.action.SaveAction;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.FullScreen;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFrame;
import tufts.vue.gui.VueMenuBar;
import tufts.vue.ui.InspectorPane;
import edu.tufts.vue.compare.ui.MergeMapsControlPanel;
import edu.tufts.vue.dsm.impl.VueDataSourceManager;
import edu.tufts.vue.preferences.implementations.MetadataSchemaPreference;
import edu.tufts.vue.preferences.implementations.ShowAgainDialog;
import edu.tufts.vue.preferences.implementations.WindowPropertiesPreference;

import javax.swing.plaf.ProgressBarUI;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 * @version $Revision: 1.714 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $ 
 */

public class VUE
    implements VueConstants
{
    public static final boolean VUE3 = true;
    //public static final boolean VUE3_LAYERS = true;
    
    public static boolean BLOCKING_OSID_LOAD = edu.tufts.vue.dsm.impl.VueDataSourceManager.BLOCKING_OSID_LOAD;
    
    /** This is the root logger for all classes named tufts.* */
    private static final Logger TuftsLog = Logger.getLogger("tufts");
    /** This is the root logger for all classes named edu.tufts.* */
    private static final Logger EduTuftsLog = Logger.getLogger("edu.tufts");

    private static final Logger Log = Logger.getLogger(VUE.class);
    
    private static AppletContext sAppletContext 	= null;
    
    /** The currently active selection.
     * elements in ModelSelection should always be from the ActiveModel */
    static final LWSelection ModelSelection = new LWSelection();
    
    private static VueFrame ApplicationFrame;
    
    private static MapTabbedPane mMapTabsLeft;
    private static MapTabbedPane mMapTabsRight;
    public static JSplitPane mViewerSplit;
    public static DockWindow[] acrossTop = null;
    
    private static tufts.vue.ui.SlideViewer slideViewer;
    
    // TODO: get rid of this
    public static boolean  dropIsLocal = false;
    
    private static boolean isStartupUnderway = false;
    private static java.util.List FilesToOpen = Collections.synchronizedList(new java.util.ArrayList());

    private static InspectorPane inspectorPane = null;
    private static FormatPanel formattingPanel; 
    private static FloatingZoomPanel floatingZoomPanel; 
    private static PathwayPanel pathwayPanel = null;
    private static MapInspectorPanel mapInspectorPanel = null;
    private static JButton returnToMapButton = null;
    private static MergeMapsControlPanel mergeMapsControlPanel = null;
    private static MetadataSearchMainGUI metadataSearchMainPanel = null;
    private static InteractionTools interactionToolsPanel = null;
    private static ContentPanel contentPanel = null;
    private static JPopupMenu popup;
    private static JPopupMenu editPopup;

    static final SearchTextField mSearchTextField = new SearchTextField();
    
    public static final int FIRST_TAB_STOP = 6;   
    //public static JCheckBoxMenuItem  resetSettingsMenuItem;
    public static ExpandSelectionControl depthSelectionControl = null;
    public static JPanel searchPanel = new JPanel(new GridBagLayout());


    /** this was for VUE applet -- currently unused */
    public static void finalizeDocks()
    {
    	//SwingUtilities.invokeLater(new Runnable() { public void run() {
    	//inspectorPane.removeAll();
    //	inspectorPane = null;
    	
    //	formattingPanel.removeAll();
    	formattingPanel = null;
    	
    //	floatingZoomPanel.removeAll();
    	floatingZoomPanel = null;
    	
    	//pathwayPanel.removeAll();
    	//pathwayPanel = null;
    	
    //	mapInspectorPanel.removeAll();
    	mapInspectorPanel = null;
    	metadataSearchMainPanel = null;
    	//returnToMapButton = null;
    	mergeMapsControlPanel = null;
    	
    //	pathwayDock = null;
    //	pathwayDock = null;
    	formatDock = null;
    	slideDock = null;
    	pannerDock = null;;
    	MapInspector = null;
    	ObjectInspector = null;
    	outlineDock = null;
    	floatingZoomDock = null;
    	layersDock = null;
    	metadataSearchMainPanel = null;
    	mergeMapsControlPanel = null;
    	interactionToolsPanel = null;
    	contentPanel = null;
    //	EditorManager.destroy();
   // 	VueToolbarController.destroyController();
   // 	VUE.getActiveViewer().destroyContextMenus();
    	//

    }
    /** simplest form of threadsafe static lazy initializer: for CategoryModel */
    private static final class HolderCM {
        static final edu.tufts.vue.metadata.CategoryModel _CategoryModel = new edu.tufts.vue.metadata.CategoryModel();
    }
    /** simplest form of threadsafe static lazy initializer: for RDFIndex */
    private static final class HolderRDFIndex {
        static final edu.tufts.vue.rdf.RDFIndex _RDFIndex = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex();
    }
    
    public static  edu.tufts.vue.metadata.CategoryModel getCategoryModel() {
        return HolderCM._CategoryModel;
    }
    // Can't find anywhere in code-base this is currently used -- was it ever? SMF 2012-06-25
    public static edu.tufts.vue.rdf.RDFIndex getRDFIndex() {
        return SKIP_RDF_INDEX ? null : HolderRDFIndex._RDFIndex;
    }
    public static JButton getReturnToMapButton()
    {
    	return returnToMapButton;
    }
    
   
    /** active pathway handler -- will update active pathway-entry handler if needed */
    private static final ActiveInstance<LWPathway>
        ActivePathwayHandler = new ActiveInstance<LWPathway>(LWPathway.class) {
        @Override
        protected void onChange(ActiveEvent<LWPathway> e) {
            // Only run the update if this isn't already an auxillary sync update from ActivePathwayEntryHandler:
            if (e.source instanceof ActiveEvent && ((ActiveEvent)e.source).type == LWPathway.Entry.class)
                return;
            
            if (e.active != null)
                ActivePathwayEntryHandler.setActive(e, e.active.getCurrentEntry());
            else
                ActivePathwayEntryHandler.setActive(e, null);
                
        }
    };

    /**
     * active LWComponent handler (the active single-selection, if there is one).
     * Will guess at and update the active pathway entry if it can.
     */
    private static final ActiveInstance<LWComponent>
        ActiveComponentHandler = new ActiveInstance<LWComponent>(LWComponent.class) {
        @Override
        protected void onChange(ActiveEvent<LWComponent> e)
        {
            final LWComponent node = e.active;
            
            if (node == null) {
                // nothing to do
            }
            else if (node instanceof LWSlide && ((LWSlide)node).getEntry() != null) {
                ActivePathwayEntryHandler.setActive(e, ((LWSlide)node).getEntry());
            }
            else if (node instanceof LWPortal && node.hasEntries()) {
                final LWPathway knownUnique = node.getExclusiveVisiblePathway();
                if (knownUnique != null)
                    ActivePathwayEntryHandler.setActive(e, knownUnique.getFirstEntry(node));
                else if (node.inPathway(VUE.getActivePathway()))
                    ActivePathwayEntryHandler.setActive(e, VUE.getActivePathway().getFirstEntry(node));
                
            }

//             //-------------------------------------------------------
//             // now set the active Layer
//             //-------------------------------------------------------
            
//             if (node != null && node.getLayer() != null)
//                 ActiveInstance.set(LWMap.Layer.class, this, node.getLayer());

            //-------------------------------------------------------
            // now set the active Resource
            //-------------------------------------------------------

            if (node == null) {
                ActiveResourceHandler.setActive(e, null);
                return;
            } else {
                ActiveResourceHandler.setActive(e, node.getResource());
            }
            
            // Note: the below problem also applies to the selection of raw images (raw
            // images are nodes) that have been added to a pathway.  This is
            // particularly a problem there in that raw images on a pathway never have a
            // slide, so there's never an option for picking as the start of a
            // presentation via selection -- that will have to be handled specially in
            // the presentation tool.

            // SOLUTION: there should be an "add" ('+') button on each pathway line
            // in the pathway table, analogous to the "move here" button on each
            // layer line, instead of the one "add" button at top that applies
            // only to the selected pathway -- then we'd never have this problem.

            // However, for some cases, we should be able to work around this, yes?
            // E.g., if the node is only on ONE pathway, this should be safe.  Also,
            // we could only apply this to the VISIBLE pathways it's on, yes?
            // We'd still then have the problem of not being able to add to a hidden
            // pathway, and then this would also be confusing -- sometimes it would
            // appear to work, othertimes it wouldn't.

            // Or, howabout it selects the pathway entry ONLY if it's on the
            // currently selected pathway?  Then it's guaranteed never to change
            // the selected pathway, which fixes for now the add problem, and
            // gives us at least some benefit.

            LWPathway.Entry newActiveEntry = null;
            final LWPathway activePathway = ActivePathwayHandler.getActive();
            if (activePathway != null && node.inPathway(activePathway))
                newActiveEntry = activePathway.getEntry(activePathway.firstIndexOf(node));
            if (newActiveEntry != null)
                ActivePathwayEntryHandler.setActive(e, newActiveEntry);
            
//                 // This code will auto-select the first pathway entry for a selected node.
//                 // There is a problem if we do this tho, in that changing the active
//                 // entry also changes the active pathway, and doing that means
//                 // that once a node is added to a given pathway, it makes it
//                 // impossible to add it to any other pathways, because as soon
//                 // as it's selected, it then makes active the pathway it's on!
//                 // (and adding a node to a pathway always adds it to the active pathway).
//                 LWPathway.Entry newActiveEntry = null;
//                 if (node.numEntries() == 1) {
//                     newActiveEntry = node.mEntries.get(0);
//                 } else {
//                     final LWPathway activePathway = ActivePathwayHandler.getActive();
//                     if (activePathway != null && node.inPathway(activePathway))
//                         newActiveEntry = activePathway.getEntry(activePathway.firstIndexOf(node));
//                 }
//                 if (newActiveEntry != null)
//                     ActivePathwayEntryHandler.setActive(e, newActiveEntry);
            
        }
    };

    /**
     * Global active Layer handler for the active layer within the active map.
     */
    private static final ActiveInstance<LWMap.Layer>
        ActiveLayerHandler = new ActiveInstance<LWMap.Layer>(LWMap.Layer.class) {
//         @Override
//         protected void onChange(ActiveEvent<LWMap.Layer> e)
//         {
//             ////if (ActiveComponentHandler.getActive() == null)
//             if (getSelection().size() == 0 || getSelection().only() instanceof LWMap.Layer)
//             {
//                 getSelection().setTo(e.active);
//                 //ActiveComponentHandler.setActive(this, e.active);
//             }

//             // keep the active layer in the map up to date
//             e.active.getMap().setActiveLayer(e.active);
//         }
    };
    

    /**
     * Global active Resource handler.
     */
    private static final ActiveInstance<Resource>
        ActiveResourceHandler = new ActiveInstance<Resource>(Resource.class) {
        @Override
        protected void onChange(ActiveEvent<Resource> e)
        {
            if (VUE.inPresentMode()) {
                // do NOT attempt to update anything if we're in the
                // middle of a presentation (e.g., the current focal
                // is a raw image, making it also the active resource,
                // as the current focal in a presentation is also
                // set as the single selection).
            } else {
                if (e.active != null)
                    checkForAndHandleResourceUpdate(e.active);
            }
        }
    };


    /**
     * If and only if the given Resource has changed on disk: check all open maps for
     * any LWImage's using it, and tell them to reload. Also, find any OTHER
     * Resources that have changed on disk, and update their LWImage's.
     */
    
    public static boolean checkForAndHandleResourceUpdate(Resource r)
    {
        if (r != null && r.isImage() && r.dataHasChanged()) {

            VUE.activateWaitCursor();
                    
            try {

                final Collection<LWComponent> everything = new ArrayList(64);
                
                for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
                    LWMap map = mMapTabsLeft.getMapAt(i);
                    if (map == null)
                        continue;
                    everything.addAll(map.getAllDescendents(LWMap.ChildKind.ANY));
                }
                
                handleResourceUpdate(r, everything, true);

            } catch (Throwable t) {

                Log.error("resource update: " + r, t);
                
            } finally {

                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    VUE.clearWaitCursor();
                }});

            }

            return true;
            
        } else
            return false;
    }

    private static void handleResourceUpdate(Resource r, Iterable<LWComponent> everything, boolean firstPass) {

        r.flushCache();

        // This will find all LWImage's anywhere in any open map that points to the
        // same resource, and thus may (probably) need updatng.
        
        for (LWComponent c : everything) {
            if (c instanceof LWImage) {
                final LWImage image = (LWImage) c;
                if (r.equals(image.getResource())) {
                    if (r != image.getResource())
                        image.getResource().dataHasChanged(); // make sure all other instances are also current
                    Log.info("reloading in " + c.getMap().getLabel() + ": " + image.getUniqueComponentTypeLabel() + "; " + image.getLabel());
                    image.reloadImage();
                } else if (firstPass) {
                    // Check them all as long as we're updating.
                    if (image.hasResource() && image.getResource().dataHasChanged()) {
                        try {
                            handleResourceUpdate(image.getResource(), everything, false);
                        } catch (Throwable t) {
                            Log.error("auto-discovery resource update " + image.getResource(), t);
                        }
                    }
                }
            }
        }
    }
    

    /**
     * The active pathway-entry handler -- will update the active pathway handler if needed.
     * If there is an on-map node associated with the pathway entry, it will force a single-selection of the
     * of that node (triggering an change to the active LWComponent), unless the LWSelection is
     * is the middle of a notification of it's own.
     */
    private static final ActiveInstance<LWPathway.Entry>
        ActivePathwayEntryHandler = new ActiveInstance<LWPathway.Entry>(LWPathway.Entry.class) {
        @Override
        protected void onChange(ActiveEvent<LWPathway.Entry> e) {
            // Only run the update if this isn't already an auxillary sync update from ActivePathwayHandler:
            if (e.source instanceof ActiveEvent && ((ActiveEvent)e.source).type == LWPathway.class)
                return;
            
            if (e.active != null)
                ActivePathwayHandler.setActive(e, e.active.pathway);
            else
                ActivePathwayHandler.setActive(e, null);

            //if (e.active != null && e.active.getSelectable() != null && !e.active.isPathway()) {
            if (e.active != null && e.active.getSelectable() != null) {

                if (false && e.active.isPathway()) {
                    final LWComponent activeNode = ActiveComponentHandler.getActive();
                               
                    // hack: only allow a pathway to become the active component (so we can see its
                    // notes), if the current active component is NOT a slide owned by a pathway
                    // (meaning really: not a pathway entry) So if you want to see pathway notes,
                    // you have to select an entry first.  This is to support the workflow of
                    // selecting a node, then selecting a pathway to add it to.  If we didn't
                    // exclude this case, selecting a node, then selecting a pathway, would
                    // DE-select the node, and you could never add it to the pathway!  The only
                    // slides that currently WOULDN'T be pathway owned would be on-map slides,
                    // which aren't offically supported at the moment, but just in case we check that.
                    
                    if (activeNode == null || activeNode instanceof LWSlide == false || !activeNode.isPathwayOwned()) {
                        ; // allow pathway selection
                    } else {
                        return;
                    }
                }

                final LWSelection s = VUE.getSelection();
                synchronized (s) {
                    // as long as we're not already here due to a selection notification,
                    // auto-select the appropriate component for this entry
                    if (!s.inNotify())
                        s.setTo(e.active.getSelectable());
                }
            }

            if ((DEBUG.BOXES&&DEBUG.META) && e.active.isPathway() && !Images.lowMemoryConditions()) {
                // for visually verifying the load-order
                e.active.pathway.preCacheContent();
            }
        }
    };


    /**
     * The active map handler -- on changes, will update the active pathway handler, as well as
     * the global undo action labels.
     */
    private static final ActiveInstance<LWMap>
        ActiveMapHandler = new ActiveInstance<LWMap>(LWMap.class) {
        @Override
        protected void onChange(ActiveEvent<LWMap> e, final LWMap nowActive) {
            if (nowActive != null) {
                ActivePathwayHandler.setActive(e, nowActive.getActivePathway());
                setMapActionsEnabled(true);
                if (nowActive.getUndoManager() != null)
                    nowActive.getUndoManager().updateGlobalActionLabels();
            } else {
                ActivePathwayHandler.setActive(e, null);
                setMapActionsEnabled(false);
            }
        }
    };

    private static void setMapActionsEnabled(boolean enable) {
        //Util.printStackTrace("ENABLED MAP ACTIONS " + enable);    	
        VueMenuBar.RootMenuBar.saveAction.setEnabled(enable);
        VueMenuBar.RootMenuBar.saveAsAction.setEnabled(enable);
        if (VUE.isApplet() && VueApplet.isZoteroApplet())
        	Actions.SaveCopyToZotero.setEnabled(enable);
        VueMenuBar.RootMenuBar.publishMenu.setEnabled(enable);
        Actions.CloseMap.setEnabled(enable);
        VueMenuBar.RootMenuBar.printAction.setEnabled(enable);
        VueMenuBar.RootMenuBar.pdfExportMenu.setEnabled(enable);  
        Actions.Undo.setEnabled(enable);
        Actions.Redo.setEnabled(enable);
        Actions.Paste.setEnabled(enable);
        Actions.SelectAll.setEnabled(enable);
        Actions.SelectAllNodes.setEnabled(enable);
        Actions.SelectAllLinks.setEnabled(enable);
        Actions.Reselect.setEnabled(enable);
        Actions.Revert.setEnabled(enable);
        
        Actions.ZoomIn.setEnabled(enable);
        Actions.ZoomOut.setEnabled(enable);
        Actions.ZoomFit.setEnabled(enable);
        Actions.ZoomActual.setEnabled(enable);
        Actions.ToggleFullScreen.setEnabled(enable);
        Actions.ToggleSplitScreen.setEnabled(enable);
        Actions.ToggleSlideIcons.setEnabled(enable);
        Actions.TogglePruning.setEnabled(enable);
        Actions.ToggleAutoZoom.setEnabled(enable);
        
       	VueMenuBar.RootMenuBar.setMenuEnableFlg(enable);
        
        VueMenuBar.RootMenuBar.transformMenu.setEnabled(enable); 
        VueMenuBar.RootMenuBar.arrangeMenu.setEnabled(enable); 
        VueMenuBar.RootMenuBar.alignMenu.setEnabled(enable); 
        VueMenuBar.RootMenuBar.layoutMenu.setEnabled(enable); 
        VueMenuBar.RootMenuBar.linkMenu.setEnabled(enable); 
       
        Actions.NewNode.setEnabled(enable);
        Actions.NewRichText.setEnabled(enable);
        
        VueMenuBar.RootMenuBar.playbackMenu.setEnabled(enable);
    }
    
    /**
     * For the currently active viewer (e.g., is visible
     * and has focus).  Actions (@see Actions.java) are performed on
     * the active map in the active viewer.
     * The active viewer can be null, which happens when we close the active viewer
     * and until another grabs the application focus (unles it was the last viewer).
     *
     * Will update the active map handler.
     */
    private static final ActiveInstance<MapViewer>
        ActiveViewerHandler = new ActiveInstance<MapViewer>(MapViewer.class) {
        @Override
        protected void notifyListeners(ActiveEvent<MapViewer> e) {
            if (!(e.active instanceof tufts.vue.ui.SlideViewer)) {
                // SlideViewer not treated as application-level viewer: ignore when gets selected
                super.notifyListeners(e);
                if (e.active != null)
                    ActiveMapHandler.setActive(e, e.active.getMap());
                else
                    ActiveMapHandler.setActive(e, null);
            }
        }
        protected void onChange(ActiveEvent<MapViewer> e) {
            
        }
    };

    /**
     * For the currently active tufts.vue.DataSource
     */
    private static final ActiveInstance<tufts.vue.DataSource>
        ActiveDataSourceHandler = new ActiveInstance<tufts.vue.DataSource>(tufts.vue.DataSource.class) {
        @Override
        protected void onChange(ActiveEvent<tufts.vue.DataSource> e) {
            // perhaps set active tufts.vue.BrowseDataSource
            
        }
    };
    


    public static LWMap getActiveMap() { return ActiveMapHandler.getActive(); }
    public static LWPathway getActivePathway() { return ActivePathwayHandler.getActive(); }
    public static LWPathway.Entry getActiveEntry() { return ActivePathwayEntryHandler.getActive(); }
    public static MapViewer getActiveViewer() { return ActiveViewerHandler.getActive(); }
    public static LWComponent getActiveComponent() { return ActiveComponentHandler.getActive(); }
    public static Resource getActiveResource() { return ActiveResourceHandler.getActive(); }
    public static LWMap.Layer getActiveLayer() { return ActiveLayerHandler.getActive(); }
    public static tufts.vue.DataSource getActiveDataSource() { return ActiveDataSourceHandler.getActive(); }

    public static LWComponent getActiveFocal() {
        MapViewer viewer = getActiveViewer();
        if (viewer != null)
            return viewer.getFocal();
        else
            return null;
    }

    /** @return the active tool as reported by the toolbar controller, which may only be a top-level tool*/
    public static VueTool getActiveTool() { return VueToolbarController.getActiveTool(); }
    
    public static boolean inPresentMode() {
        return inFullScreen() && getActiveTool() instanceof PresentationTool;
    }
    
    // TODO: refactor this so that the only place that makes a distinction between a top
    // level tool and a sub tool (e.g., SelectionTool v.s. SelectionTool.Direct) is in the toolbar
    // controller.
    /** @return the active subtool, or the active top-level tool if it has no active sub-tool */
    public static VueTool getActiveSubTool() { 

        final VueTool tool = getActiveTool();
        final VueTool subTool = tool == null ? null : tool.getSelectedSubTool();
        
        if (subTool != null && subTool.getClass() != VueSimpleTool.class)
            return subTool;
        else
            return tool;
    }
    
    
    public static void setActive(Class clazz, Object source, Object newActive) {
        if (DEBUG.EVENTS && DEBUG.META) {
            Util.printStackTrace("Generic setActive of " + clazz + " from " + source + ": " + newActive);
        }
        if (newActive == null || clazz.isInstance(newActive))
            ActiveInstance.getHandler(clazz).setActive(source, newActive);
        else
            tufts.Util.printStackTrace("not an instance of " + clazz + ": " + newActive);
    }

    public static Object getActive(Class clazz) {
        // todo: if a handler doesn't already exist for this class, just return null
        return ActiveInstance.getHandler(clazz).getActive();
    }
    
    public static void addActiveListener(Class clazz, ActiveListener listener) {
        ActiveInstance.addListener(clazz, listener);
    }
    public static void addActiveListener(Class clazz, Object reflectedListener) {
        ActiveInstance.addListener(clazz, reflectedListener);
    }
    public static void removeActiveListener(Class clazz, ActiveListener listener) {
        ActiveInstance.removeListener(clazz, listener);
    }
    public static void removeActiveListener(Class clazz, Object reflectedListener) {
        ActiveInstance.removeListener(clazz, reflectedListener);
    }	
    
    public static void setAppletContext(AppletContext ac) {
        sAppletContext = ac;
    }
    public static AppletContext getAppletContext() {
        return sAppletContext;
    }
    public static boolean isApplet() {
    	if (VueApplet.getInstance() !=null)
    		return VueApplet.getInstance().getAppletContext() != null;
    	else
    		return false;
      //  return sAppletContext != null;
    }

    public static String getSystemProperty(String name) {
        // If we're an applet, System.getProperty will throw an AccessControlException
        if (false && isApplet())
            return null;
        else {
            String prop;
            try {
                prop = System.getProperty(name);
                if (DEBUG.INIT) Log.debug("fetched system property " + name + "=[" + prop + "]");
            } catch (java.security.AccessControlException e) {
                System.err.println(e);
                prop = null;
            }
            return prop;
        }
    }

    public static final String NullSystemProperty = "";
    
    /**
     * Getl's a system property, guaranteeing a non-null return value.
     * @return value or given system property, or an empty String
     */
    public static String getSystemPropertyValue(String name) {
        String value = getSystemProperty(name);
        if (value == null)
            return NullSystemProperty;
        else
            return value;
    }
    
    public static boolean isSystemPropertyTrue(String name) {
        String value = getSystemProperty(name);
        return value != null && value.toLowerCase().equals("true");
    }
    
    public static boolean hasSystemProperty(String name) {
        return getSystemProperty(name) != null;
    }
    
    
    /*
    public static java.net.URL getResource(String name) {
        java.net.URL url = null;
        // First, check the current directory:
        java.io.File f = new java.io.File(name);
        boolean foundInCWD = false;
        if (f.exists()) {
            try {
                url = f.toURL();
                foundInCWD = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // If not found in the current directory, check the classpath:
        if (url == null)
            url = ClassLoader.getSystemResource(name);
        if (foundInCWD)
            System.out.println("resource \"" + name + "\" found in CWD at " + url);
        else
            System.out.println("resource \"" + name + "\" found in classpath at " + url);
        return url;
    }
    */
        
    public static LWSelection getSelection() {
        return ModelSelection;
    }
    
    public static tufts.vue.ui.SlideViewer getSlideViewer()
    {
    	return slideViewer;
    }
    
    public static InspectorPane getInspectorPane() {
    	 return inspectorPane;
    }
    
    public static boolean isStartupUnderway() {
        return isStartupUnderway;
    }
    
    public static void activateWaitCursor() {
        GUI.activateWaitCursor();
    }
    
    public static void clearWaitCursor() {
        GUI.clearWaitCursor();
    }
    
    static void initUI() {

    	GUI.init();
        
// Doesn't work in Snow Leopard & not needed
//         try {
//             if (DEBUG.Enabled && Util.isMacPlatform() && !VUE.isApplet()) {
//                 // This is for debugging.  The application icon for a distributed version
//                 // of VUE is set via an icons file specified in the Info.plist from
//                 // the VUE.app directory.
//                 // Be sure to call this after GUI initialized, or we are hidden from the OSX app dock.
//                 tufts.macosx.MacOSX.setApplicationIcon
//                     (VUE.class.getResource("/tufts/vue/images/vueicon32x32.gif").getFile());
//             }
//         } catch (Throwable t) {
//             t.printStackTrace();
//         }

        if (DEBUG.Enabled) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // This, of course, must run in the AWT thread to do what we want:
                Thread.currentThread().setName("*AWT*");
                //Thread.currentThread().setName(Util.TERM_RED + "*AWT*" + Util.TERM_CLEAR);
            } });
        }
    }

    /** initialize based on command line args, and the initlaize the GUI */
    public static void init(String[] args) {
        if (args != null)
            parseArgs(args);
        initUI();
    }

    public static void init() {
        init(null);
    }
    
    public static void parseArgs(String[] args) {
        String allArgs = "";
        for (int i = 0; i < args.length; i++) {
            allArgs += "[" + args[i] + "]";
            if (args[i].equals("-nosplash")) {
                SKIP_SPLASH = true;
            } else if (args[i].equals("-nodr")) {
                SKIP_DR = true;
                SKIP_SPLASH = true;
            } else if (args[i].equals("-noem")) {
                SKIP_EDITOR_MANAGER = true;
            } else if (args[i].equals("-noidx")) {
                SKIP_RDF_INDEX = true;
            } else if (args[i].equals("-nocat")) {
                SKIP_CAT = true;
            } else if (args[i].equals("-skip")) {
                SKIP_DR = true;
                SKIP_CAT = true;
                SKIP_SPLASH = true;
            } else if (args[i].equals("-exit_after_init")) // for startup time trials
                exitAfterInit = true;
            else
                DEBUG.parseArg(args[i]);

            if (args[i].startsWith("-debug")) DEBUG.Enabled = true;

        }

        GUI.parseArgs(args);

        if (DEBUG.INIT) System.out.println("VUE: parsed args " + allArgs);

        if (DEBUG.Enabled)
            debugInit(DEBUG.TRACE || DEBUG.META);

    }

    public static final PatternLayout MasterLogPattern = new PatternLayout("VUE %d %5p [%t]%x %c{1}: %m%n");
    
    public static void debugInit(boolean heavy) {
        if (heavy) {
            // this handy for finding code locations:
            // Note: %F, %C and %M are "very slow"
            MasterLogPattern.setConversionPattern("@%6r [%t] %5p %x "
                                                  + Util.TERM_RED + "(%F/%C/%M)" + Util.TERM_CLEAR
                                                  + " %m%n"); 
        } else {
            MasterLogPattern.setConversionPattern("@%.1p%6r [%t]%x %c{1}: %m%n");
            //MasterLogPattern.setConversionPattern("@%6r %5p [%t]%x %c{1}: %m%n");
            //MasterLogPattern.setConversionPattern("@%6r [%t] %5p %c %x %m%n");
        }

        // This will enable it for every logger in any jar, which is tons of stuff.
        // Logger.getRootLogger().setLevel(Level.DEBUG);
        
        TuftsLog.setLevel(Level.DEBUG);
        EduTuftsLog.setLevel(Level.DEBUG);
    }

    /*
    // no longer needed as is: can clean up to dump all loggers in the VM if we like:
    private static void updateTuftsLoggers(Level newLevel)
    {
        if (newLevel == null)
            newLevel = DEBUG.Enabled ? Level.DEBUG : Level.INFO;
        
        Enumeration<Logger> e = LogManager.getCurrentLoggers();
        while (e.hasMoreElements()) {
            Logger l = e.nextElement();
            //System.out.println("Found logger: " + l + "; " + l.getName() + " at " + l.getLevel()
            if (DEBUG.Enabled && DEBUG.META)
                System.out.println("Found in "
                                   + l.getParent() + ": " 
                                   + l + "; " + l.getName() + "; at " + l.getLevel()
                                   //+ " " + l.getParent().getName()
                                   );
            if (newLevel != null && l.getName().startsWith("tufts")) {
                Level curLevel = l.getLevel();
                if (true) {
                    System.out.println("\tlogger " + l.getName() + "; " + curLevel + " -> " + l.getLevel());
                    continue;
                }
                if (curLevel == null || newLevel.toInt() > curLevel.toInt()) {
                    l.setLevel(newLevel);
                    if (true||DEBUG.Enabled)
                        System.out.println("\tlogger " + l.getName() + "; " + curLevel + " -> " + l.getLevel());
                }
            }
        }
    }
    */
    
        
    

    
    //-----------------------------------------------------------------------------
    // Variables used during VUE startup
    //-----------------------------------------------------------------------------
    
    private static boolean exitAfterInit = false;
    private static boolean SKIP_DR = false; // don't load DRBrowser, no splash & no startup map
    private static boolean SKIP_CAT = false; // don't load category model
    private static boolean SKIP_SPLASH = false;
    private static boolean SKIP_EDITOR_MANAGER = false;
    private static boolean SKIP_RDF_INDEX = false;
    private static String NAME;
	
    private static DockWindow pathwayDock;
    private static DockWindow formatDock;
    private static DockWindow slideDock;
    private static DockWindow pannerDock;
    private static DockWindow MapInspector;
    private static DockWindow ObjectInspector;
    private static DockWindow outlineDock;
    private static DockWindow floatingZoomDock;
    private static DockWindow layersDock;
    private static DockWindow metaDataSearchDock;
    private static DockWindow interactionToolsDock;
    private static DockWindow contentDock;
    private static DockWindow mergeMapsDock;
    private static DockWindow anchor;

    private static boolean UseLeopardAnchor = false;

    static {
        Logger.getRootLogger().removeAllAppenders(); // need to do this or we get everything twice
        //BasicConfigurator.configure();
        //Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("VUE %d [%t] %-5p %c:%x %m%n")));
        //final org.apache.log4j.Layout pattern = new PatternLayout("VUE %d [%t] %5p %x %m%n");
        //final PatternLayout pattern = new PatternLayout("VUE %d [%t] %5p %x %F/%C/%M %m%n");
        final PatternLayout pattern = MasterLogPattern;
        //Logger.getRootLogger().addAppender(new ConsoleAppender(pattern, "System.err"));
        Logger.getRootLogger().addAppender(new ConsoleAppender(pattern));
        Logger.getRootLogger().addAppender(new WriterAppender(pattern, Util.getLogWriter()));
        Logger.getRootLogger().setLevel(Level.INFO);
        //Log.addAppender(new ConsoleAppender(new PatternLayout("[%t] %-5p %c %x - %m%n")));

        //set tooltips to psuedo-perm
        ToolTipManager.sharedInstance().setDismissDelay(240000);
        ToolTipManager.sharedInstance().setInitialDelay(500);
        //if (VueUtil.isMacPlatform())
        //    installMacOSXApplicationEventHandlers();
        
        //Preference initialzation for UI.
        MetadataSchemaPreference.getInstance();
    }

    /** push a short diagnostic string onto the log output stack */
    public static void diagPush(String s) {
        s = "[" + s + "]";
        //s = "#" + s;
        if (org.apache.log4j.NDC.getDepth() == 0)
            org.apache.log4j.NDC.push(" " + s);
        else
            org.apache.log4j.NDC.push(s);
    }

    public static void diagPop() {
        org.apache.log4j.NDC.pop();
    }
    
    public static void main(String[] args)
    {
        VUE.isStartupUnderway = true;
        
        parseArgs(args);

        if (DEBUG.Enabled) {
            // dump a date if debug is on, as it will have installed a log format that leaves it out
            Log.info("VUE startup: " + new Date());
        }
        

        Log.info("VUE build: " + tufts.vue.Version.AllInfo);
        Log.info("Current Platform: " + Util.getPlatformName());
        final String requestedJavaArch = System.getenv("JAVA_ARCH");
        if (requestedJavaArch != null)
            Log.info("JAVA_ARCH REQUEST: " + requestedJavaArch);
        Log.info("Running JVM: " +
                 getSystemProperty("java.runtime.version") + " / " + getSystemProperty("sun.arch.data.model") + " bit"
                 + "; MaxMemory(-Xmx)=" + VueUtil.abbrevBytes(Runtime.getRuntime().maxMemory())
                 + ", CurMemory(-Xms)=" + VueUtil.abbrevBytes(Runtime.getRuntime().totalMemory())
                 + ", FreeMemory=" + VueUtil.abbrevBytes(Runtime.getRuntime().freeMemory())
                 );

        Log.info("VUE version: " + VueResources.getString("vue.version"));
        Log.info("Current Working Directory: " + getSystemProperty("user.dir"));

        String host = System.getenv("HOST");
        if (host == null) host = System.getenv("HOSTNAME");
        if (host == null) host = System.getenv("COMPUTERNAME");
        if (host == null) host = System.getenv("USERDOMAIN");
        Log.info("User/host: " + getSystemProperty("user.name") + "@" + host);
        
        if (VueUtil.isMacPlatform()) {
            try {
                installMacOSXApplicationEventHandlers();
            } catch (Throwable t) {
                Log.error("unable to install handler for Finder double-click to open .vue files; /System/Library/Java may be missing or not in the CLASSPATH", t);
            }
        }
            
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null || args[i].length() < 1 || args[i].charAt(0) == '-')
                continue;
            if (DEBUG.INIT) out("command-line file to open " + args[i]);
            VUE.FilesToOpen.add(args[i]);
        }
        
        try {

            initUI();
            diagPush("init");
            initApplication();
            diagPop();
            
        } catch (Throwable t) {
            Util.printStackTrace(t, "VUE init failed");
            VueUtil.alert(VueResources.getString("dialog.initfailed.message"), t);
        }

        
        VUE.isStartupUnderway = false;

        Log.info("UI startup completed.");
        
        //-------------------------------------------------------
        // Handle any outstanding file (map) open requests.
        //-------------------------------------------------------
        
        try {
            
            // This must now happen AFTER data-sources are loaded, as it's possible that
            // they will be providing authentication that will be required to get
            // content on the maps that will be opened [VUE-879]

            // Note: if BLOCKING_OSID_LOAD is false (the new default, so VUE startup
            // can't be hung by any single repository problem), the OSID's repository
            // configuration threads in the VDSM will be in a race with the the below
            // map open below if it contains any content URL's that require
            // authentication keys. That content will fail to load unless the relevant
            // repository configure thread has managed to complete before the URL access
            // is attempted.  Resources don't currently know what repository object they
            // came from, so we can't wait for the relevant keys to be present, and any
            // repository may hang, so we can't just wait for them all.
            // UrlAuthentication does now dynamically update itself as new repositories
            // come online, so this isn't such an issue, but the VUE-879 bug will appear
            // if a URL authenticating repository is not present at all (which has
            // always been an unresolved issue if a user shares a map with authenticated
            // content with someone who doesn't have the authenticating data source
            // installed), or if the configure thread loses the race with the map open.
            
            // todo: someday, Resources can be tagged as requiring authentication (or
            // actually reference some kind of repository object that knows this) so
            // when URL content is requested, we could know to wait on the
            // DS_ALL_CONFIGURED message from the VDSM before attempting access, or even
            // better, the DS_CONFIGURED message for the relevant DataSource/Repository.
            
            handleOutstandingVueMapFileOpenRequests();
            
        } catch (Throwable t) {
            Log.error("failed to handle file open at init: " + FilesToOpen, t);
        }

        if (DEBUG.KEYS) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                for (AbstractAction a : VueAction.getAllActions())  {
                    KeyStroke ks = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                    if (ks != null && a.getPropertyChangeListeners().length == 0)
                        Log.warn("Action has no listeners, may be unable to activate by keystroke: " + Util.tags(a) + "; " + ks);
                }
            }});
        }
        
        if (exitAfterInit) {
            out("init completed: exiting");
            System.exit(0);
        }

        //-------------------------------------------------------
        // Trigger the load of the OSID's and set up UrlAuthentication
        //-------------------------------------------------------
        
        initDataSources();
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (contentPanel != null)
                contentPanel.loadDataSourceViewer();

            // Kick-off tufts.vue.VueDataSource viewer build threads:
            // must be done in AWT to be threadsafe, as involves
            // non-synhcronized code in tufts.vue.VueDataSource while
            // setting up the threads
            
            // DataSourceViewer.cacheDataSourceViewers();
            
        }});

                
        //-------------------------------------------------------
        // complete the rest of our tasks at min priority
        //-------------------------------------------------------

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        
        if (!SKIP_SPLASH) {
            // start in another thread as may pop dialog
            // that will block further progress on the
            // run-out of main
            final Thread versionThread = new Thread("version-check") {
                    public void run() {
                        if (DEBUG.THREAD) Log.debug("version-check kicked off");
                        checkLatestVersion();
                    }
                };
            versionThread.setPriority(Thread.MIN_PRIORITY);
            versionThread.setDaemon(true);
            // delay kickoff until after any already outstanding AWT invocations (e.g., map open's, data source loads)
            GUI.invokeAfterAWT(new Runnable() { public void run() {            
                versionThread.start();
            }});
        }

//         try {
//             // any immediate user UI requests (mouse-click) for a tufts.vue.DataSource
//             // viewer will happen in the higher priority AWT thread, and will be loaded
//             // immediately, no matter where we are in the caching process.  The cache /
//             // viewer construction code is fully synchronized, so if the desired viewer
//             // is already loading, AWT will just block until it's complete.
//             DataSourceViewer.cacheDataSourceViewers();
//         } catch (Throwable t) {
//             t.printStackTrace();
//         }

        try {
            Log.debug("loading fonts...");
            // let this run in the remainder of the main thread
            FontEditorPanel.getFontNames();
        } catch (Throwable t) {
            Log.warn("font cache", t);
        }

        Log.info("main complete");
    }

    private static boolean didDataInit = false;

    /**
     * Unmarshall the user installed data sources and get them configured with their
     * repositories, and ensure UrlAuthentication is initialized, which will listen for
     * configuring DataSources and scan them for authentication keys
     */
    static synchronized void initDataSources() {
        
        if (!didDataInit && SKIP_DR == false ){//&& !VUE.isApplet()) {
            
            VUE.diagPush("initDS");
            
            Log.debug("initDataSources: started");
            
            try {
                _initDataSources();
                didDataInit = true;
            } catch (Throwable t) {
                Log.error("failed to init data sources", t);
            }
            
            Log.debug("initDataSources: completed");
            
            VUE.diagPop();
        }
        
    }

    private static void _initDataSources()
    {
        // PROBLEM:
        // org.exolab.castor.xml.MarshalException: Declared encoding "UTF-8" does not
        // match actual one "MacRoman"; this might not be an error.{File: [not
        // available]; line: [not available]; column: [not available]}
        
        // VUE-879: UrlAuthentication should initialized BEFORE
        // startRepositoryConfiguration.  It will listen for events from VDSM to scan
        // each DataSource as it's configured for any authentication credentials.
       if (VUE.isApplet())
    	   return;
    	UrlAuthentication.getInstance();
            
        final VueDataSourceManager VDSM =
            edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();

        // unmarshall the installed data sources
        VDSM.load();
            
        // As of 2008-12-21: This will instance a DataSourceViewer, which calls
        // VDSM.getDataSources(), which will will trigger a load (unmarshalling) of
        // DataSources if they're not already loaded.  If BLOCKING_OSID_LOAD is true, it
        // won't return until they're all configured.  If false, it will return right
        // away, returning the list of unconfigured DataSources, which will normally be
        // quickly configured on separate threads and issue callbacks.
        //DR_BROWSER.loadDataSourceViewer();

        if (!BLOCKING_OSID_LOAD) {
            // Will deliver DS_CONFIGURED events to UrlAuthentication as they come in
            VDSM.startRepositoryConfiguration(null);
        }
    }

    static void initApplication()
    {
        final Window splashScreen;
        if (VUE.isApplet())
        {
        	//SKIP_DR=true;
        	SKIP_SPLASH=true;
        	SKIP_CAT=true;
        }
        if (SKIP_DR || SKIP_SPLASH ) {
            splashScreen = null;
            //DEBUG.Enabled = true;
        } else
            splashScreen = new SplashScreen();
        
        //------------------------------------------------------------------	
        // Make sure these classes are all fully loaded to establish
        // their Keys.  todo: can get all subclasses of LWComponent
        // and newInstance them just to be sure.  In any case, this
        // probably isn't even required, but it's helping debugging
        // while implementing the new Key & Property LWComponent
        // code. -- SMF

        Log.debug("pre-constructing core LW types...");
        
        new LWComponent();
        new LWLink();
        new LWImage();
        new LWNode();
        new LWText();
        
        // Load images even before building the interface, in case
        // UI may trigger image-icon (thumbnail) loads of user content.
        // (E.g., "My Saved Content")
        Log.debug("loading disk cache...");
        Images.loadDiskCache();
        Log.debug("loading disk cache: done");

        //------------------------------------------------------------------
        
        Log.debug("building interface...");
        
     
        diagPush("build");
        buildApplicationInterface();
        diagPop();
        
        
        if (Util.isMacLeopard()) {
            // Critical for keeping DockWindow's on top.
            // See tufts.vue.gui.FullScreen.FSWindow constructor for more on this.
            DockWindow.raiseAll();
        }
        
        Log.debug("interface built; splash down...");
        
        if (splashScreen != null)
            splashScreen.setVisible(false);

        
        //------------------------------------------------------------------

        //VUE.clearWaitCursor();

// No longer used:
//         if (SKIP_DR == false) {
//             Log.debug("caching tool panels...");
//             NodeTool.getNodeToolPanel();
//            // LinkTool.getLinkToolPanel();
//         }
        
        // Must call this after all LWEditors have been created and
        // put in the AWT hierarchy:
        if (!SKIP_EDITOR_MANAGER)
            EditorManager.install();
        
        // initialize enabled state of actions via a selection set:
        VUE.getSelection().clearAndNotify();

//         //---------------------------------------------
//         // Start the loading of the data source viewer
//         if (SKIP_DR == false && DR_BROWSER != null && FilesToOpen.size() > 0)
//             initDataSources();
            
//         //---------------------------------------------
        
        //Preferences p = Preferences.userNodeForPackage(VUE.class);
        //p.put("DRBROWSER.RUN", "yes, it has");

        // MAC v.s. PC WINDOW PARENTAGE & FOCUS BEHAVIOUR:
        //
        // Window's that are shown before their parent's are shown do NOT adopt a
        // stay-on-top-of-parent behaviour! (at least on mac).  FURTHERMORE: if you iconfiy the
        // parent and de-iconify it, the keep-on-top is also lost permanently!  (Even if you
        // hide/show the child window after that) None of this happens on the PC, only Mac OS X.
        // Iconifying also hides the child windows on the PC, but not on Mac.  On the PC, there's
        // also no automatic way to install the action behaviours to take effect (the ones in the
        // menu bar) when a tool window has focus.  Actually, mac appears to do something smart
        // also: if parent get's MAXIMIZED, it will return to the keep on top behaviour, but you
        // have to manually hide/show it to get it back on top.
        //
        // Also: for some odd reason, if we use an intermediate root window as the
        // master parent, the MapPanner display doesn't repaint itself when dragging it
        // or it's map!
        //
        // Addendum: keep-on-top now appears to survive iconification on mac.
        //
        // [ Assuming this is java 1.4 -- 1.5? ]
        
        // is done in buildApplicationInterface
        //getRootWindow().setVisible(true);

        //out("ACTIONTMAP " + java.util.Arrays.asList(frame.getRootPane().getActionMap().allKeys()));
        //out("INPUTMAP " + java.util.Arrays.asList(frame.getRootPane().getInputMap().allKeys()));
        //out("\n\nACTIONTMAP " + java.util.Arrays.asList(frame.getActionMap().allKeys()));
        //out("ACTIONTMAP " + Arrays.asList(VUE.getActiveViewer().getActionMap().allKeys()));
        //out("INPUTMAP " + Arrays.asList(VUE.getActiveViewer().getInputMap().keys()));
        //out("INPUTMAP " + Arrays.asList(getInputMap().keys()));

        //VUE.clearWaitCursor();

        
        if (!SKIP_DR) // SKIP_CAT?
            getCategoryModel(); // load the category model

        Log.debug("initApplication completed.");
    }



    private static void handleOutstandingVueMapFileOpenRequests()
    {
        boolean openedUserMap = false;

        if (FilesToOpen.size() > 0) {
            Log.info("outstanding file open requests: " + Util.tags(FilesToOpen));
            if (DEBUG.Enabled) Util.dump(FilesToOpen);
            
            // [OLD: in case not already loaded, make absolutely sure
            // all data sources are loaded (VUE-879), so any needed
            // authentication keys have been found] -- We don't
            // actually need this until the map is displayed, as no
            // content should be requested until then. SMF 2008-12-21
            //
            // initDataSources();
            
            try {
                Iterator i = FilesToOpen.iterator();
                while (i.hasNext()) {
                    final String fileName = (String) i.next();
                    openedUserMap = true;
                    GUI.invokeAfterAWT(new Runnable() {
                            public void run() {
                                VUE.activateWaitCursor();
                                //LWMap map = OpenAction.loadMap(fileName);
                                if (DEBUG.Enabled) Log.debug("opening map during startup " + fileName);
                                if (fileName != null) {
                                    displayMap(new File(fileName));
                                    //openedUserMap = true;
                                }
                            }});
                }
            } finally {
                VUE.clearWaitCursor();                
            }
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // ensure first item gets selected
                if (mMapTabsLeft.getTabCount() > 0)
                    mMapTabsLeft.setSelectedIndex(0); 
                // above would normally focus grab, but it skips these during startup,
                // so do it manually here: (so the pathway panel loads, etc)
                // [re-enabled focus grabs even during startup -- was mainly for command line debug loads]
                //mMapTabsLeft.getViewerAt(0).grabVueApplicationFocus("startup", null);
            }});
        }


        if (false && DEBUG.Enabled && !openedUserMap) {
        
            //if (SKIP_DR && FilesToOpen.size() == 0) {
            //-------------------------------------------------------
            // create example map(s)
            //-------------------------------------------------------
            //LWMap map1 = new LWMap("Map 1");
            LWMap map2 = new LWMap("Map 2");
            
            //installExampleNodes(map1);
            installExampleMap(map2);
            
            //map1.setFillColor(new Color(255, 255, 192));
            
            //displayMap(map1);
            displayMap(map2);
            //toolPanel.add(new JLabel("Empty Label"), BorderLayout.CENTER);
        }

        //VUE.clearWaitCursor();
    }
    

    private static void installMacOSXApplicationEventHandlers()
    {
        if (!VueUtil.isMacPlatform())
            throw new RuntimeException("can only install OSX event handlers on Mac OS X");

        VUE.Log.debug("INSTALLING MAC OSX APPLICATION HANDLER");

        File test = new File("/System/Library/Java/com/apple/cocoa/application/NSWindow.class");

        if (test.exists()) 
            Log.info("cocoa-java bridge appears present; found " + test);
        else
            Log.info("cocoa-java bridge is not present; couldn't find " + test);

        tufts.macosx.MacOSX.registerApplicationListener(new tufts.macosx.MacOSX.ApplicationListener() {
                public boolean handleOpenFile(String filename) {
                    VUE.Log.info("OSX OPEN FILE " + filename);
                    if (VUE.isStartupUnderway)
                        VUE.FilesToOpen.add(filename);
                    else
                        VUE.displayMap(new File(filename));
                    return true;
                }
                public boolean handleQuit() {
                    VUE.Log.debug("OSX QUIT");
                    ExitAction.exitVue();
                    // Always return false.  If we claim this is "handled",
                    // OSX  will do the quit for us, and even if the ExitAction
                    // was aborted, we'd exit anyway...
                    return false;
                }
                public boolean handleAbout() {
                    VUE.Log.debug("OSX ABOUT");
                    new AboutAction().act();
                    return true;
                }
                public boolean handlePreferences() {
                    VUE.Log.debug("OSX PREFERENCES");
                    Actions.Preferences.actionPerformed(null);
                    return true;
                }
                
            });
    }

    private static final boolean ToolbarAtTopScreen = false && VueUtil.isMacPlatform();

    private static void buildApplicationInterface() {

        //-------------------------------------------------------
        // Create the tabbed panes for the viewers
        //-------------------------------------------------------
    	if (!VUE.isApplet())
    	{
    		mMapTabsLeft = new MapTabbedPane("*left", true);
    		mMapTabsRight = new MapTabbedPane("right", false);
    	}
    	else
    		mMapTabsLeft = new MapTabbedPane("*left",true);//VueApplet.getMapTabbedPane();
        
        
        //-------------------------------------------------------
        // Create the split pane
        //-------------------------------------------------------
    	mViewerSplit = buildSplitPane(mMapTabsLeft, mMapTabsRight);
    	if (VUE.isApplet())
    	{
    		mViewerSplit.setBackground(new Color(244,244,244));
    		
    	}
    	//GUI.applyToolbarColor(mMapTabsRight);
        
        //-------------------------------------------------------
        // create a an application frame and layout components
        //-------------------------------------------------------
        
        if (DEBUG.INIT) out("creating VueFrame...");

        if (!VUE.isApplet())
        	VUE.ApplicationFrame = new VueFrame();
        
        if (DEBUG.INIT) out("created VueFrame");

        //------------------------------
        // Set popups heavyweight throughout the application
        //-------------------------------
        /* Originally doing this created some problems, but it looks like previous
         * changes I made in the heavyweight popup stuff have made.  I can't find 
         * a case where this would *really* help out on the mac so I'm strictly 
         * using this for windows/unix right now.  Unix because when i booted this 
         * up in linux i noticed the same problem melanie reported on windows
         */
        //if (Util.isWindowsPlatform() || Util.isUnixPlatform())
     //   if (VUE.isApplet())
        //PopupFactory.setSharedInstance(new VuePopupFactory(PopupFactory.getSharedInstance()));
        //-----------------------------------------------------------------------------
        // Man VUE Toolbar (map editing tool)
        //-----------------------------------------------------------------------------
        
        // The real tool palette window withtools and contextual tools
        VueToolbarController tbc = VueToolbarController.getController();
        
        ModelSelection.addListener(tbc);

        DockWindow toolbarDock = null;

        final JComponent toolbar;
        JPanel toolbarPanel = null;        
        if (!VUE.isApplet())
        {
        	 if (VueToolPanel.IS_CONTEXTUAL_TOOLBAR_ENABLED)
                 toolbar = tbc.getToolbar();
             else
                 toolbar = tbc.getToolbar().getMainToolbar();
        	 
        	toolbarPanel = constructToolPanel(toolbar);
        	if (ToolbarAtTopScreen) {
                 toolbarDock = GUI.createToolbar("Toolbar", toolbar);
             } else {
                ApplicationFrame.addComp(toolbarPanel, BorderLayout.NORTH);
             }
        }
        
        createDockWindows();

        
        final tufts.vue.gui.Screen screen = GUI.getScreenForWindow(null);
        
        if (!VUE.isApplet())
        {
        	// GUI.createDockWindow("Font").add(new FontEditorPanel()); // just add automatically?
        	
        	//final DockWindow fontDock = GUI.createToolbar("Font", new FontPropertyPanel());
        	// final DockWindow fontDock = GUI.createToolbar("Font", new FontEditorPanel(LWKey.Font));
        	//final DockWindow linkDock = GUI.createToolbar("Link", new LinkPropertyPanel());
        	//final DockWindow actionDock = GUI.createToolbar("Actions", new VueActionBar());
        	//final DockWindow fontDock = null;
        	//final DockWindow linkDock = null;
        	//final DockWindow actionDock = null;
        	//fontDock.setResizeEnabled(false);
        	//linkDock.setResizeEnabled(false);
        
        	//pannerDock.setChild(linkDock);
        
        	//fontDock.setChild(linkDock);
        	
        	//fontDock.setLowerRightCorner(GUI.GScreenWidth, GUI.GScreenHeight);
        
        	/*
        	 * This isn't currently used now but I have a feeling it'll come back if not i'll remove it.
        	 */
        	// Now that we have all the DockWindow's created the VueMenuBar, which needs the
        	// list of Windows for the Window's menu.  The order they appear in this list is
        	// the order they appear in the Window's menu.
        	//VUE.ToolWindows = new Object[] {
        		//unused stuff.
        		//searchDock,
        		/* keywords goes here when its done*/
        		//ObjectInspector,        	
        		/* Linear View goes here when its done*/
        		//MapInspector,
        		/* node inspector */
        		/*notes didn't end up getting its own window*/
        		//outlineDock,
        		//pannerDock,        	
        		//DR_BROWSER_DOCK,
        		//slideDock,                         
        		//resourceDock,
        		//formatDock,            
        		//htWindow,
        		//pathwayDock,            
        		//actionDock,
        		//fontDock,
        		//linkDock,
        		// toolbarDock,                                               
        		//};

        	// adding the menus and toolbars
        	if (DEBUG.INIT) out("setting JMenuBar...");
        	ApplicationFrame.setJMenuBar(VueMenuBar.RootMenuBar = new VueMenuBar(/*VUE.ToolWindows*/));
        	if (DEBUG.INIT) out("VueMenuBar installed.");;
        
        	if (true){
        		ApplicationFrame.addComp(mViewerSplit, BorderLayout.CENTER);
        	}
        	else{
        		ApplicationFrame.addComp(mMapTabsLeft, BorderLayout.CENTER);
        	}
        
        	try {
        		ApplicationFrame.pack();
        	} catch (ArrayIndexOutOfBoundsException e) {
        		Log.error("OSX TIGER JAVA BUG at frame.pack()", e);
        	}
        
        	/*
        	if (SKIP_DR) {
            	ApplicationFrame.setSize(750,450);
        	} else {
            	ApplicationFrame.setSize(800,600);
            	// todo: make % of screen, make sure tool windows below don't go off screen!
        	}
        	 */
        
        	//if (DEBUG.INIT) out("validating frame...");
        	ApplicationFrame.validate();
        	//if (DEBUG.INIT) out("frame validated");

        	//int appWidth = (int) (GUI.GScreenWidth * 0.75);
        	//int appHeight = (int) (GUI.GScreenHeight * 0.75);

        	// If you've got a wide screen, leave at least 600
        	// pixels at the right for two full 300pix DockWindow's
        	/* VUE-795 replaces the default screen sizing logic...
        	 * if (GUI.GScreenWidth >= 1600) { 
            	int maxWidth = GUI.GScreenWidth - (GUI.GInsets.left + DockWindow.DefaultWidth * 2);
            	if (appWidth > maxWidth)
                	appWidth = maxWidth;
        		}

        	if (appWidth > 1600)
            	appWidth = 1600;
        	if (appHeight > 1024)
            	appHeight = 1024;
        	 */

        	int appWidth = (int) (screen.width * 0.90);
        	int appHeight = (int) (screen.height * 0.90);
        
        	if (screen.width > 1280) {             
                    appWidth = (int) (screen.width * 0.75);
                    appHeight = (int) (screen.height * 0.90);
        	}

        	WindowPropertiesPreference wpframe = ApplicationFrame.getWindowProperties();
        
        	Dimension sz = wpframe.getWindowSize();
        	Point pos = wpframe.getWindowLocationOnScreen();
    	
        	if (wpframe.isEnabled() && !wpframe.isAllValuesDefaults() &&
        			ApplicationFrame.isPointFullyOnScreen(pos,sz))
        	{
                    if ((sz.getWidth() < 100) || (sz.getHeight() < 100))
                        ApplicationFrame.setSize((int)appWidth, (int)appHeight);
                    else        		
                        ApplicationFrame.setSize((int)sz.getWidth(), (int)sz.getHeight());
                    
                    if ((pos.getX() < 0) || (pos.getY() < 0)) {
                        ApplicationFrame.setLocation
                            (screen.margin.left,
                             screen.margin.top + (ToolbarAtTopScreen ? DockWindow.ToolbarHeight : 0));
                    } else {
                        ApplicationFrame.setLocation((int)pos.getX(),(int)pos.getY());
                    }
        	}
        	else
                    {        	        
                    ApplicationFrame.setSize(appWidth, appHeight);
                    ApplicationFrame.setLocation
                        (screen.margin.left,
                         screen.margin.top + (ToolbarAtTopScreen ? DockWindow.ToolbarHeight : 0));
        	}
        	// MAC NOTE WITH MAXIMIZING: if Frame's current location y value
        	// is less than whatever's it's maximized value is set to, maximizing
        	// it will use the y value, not the max value.  True even if set
        	// y value after setting to maximized but before it's put on screen.
        
        	//GUI.centerOnScreen(ApplicationFrame);

                /*-------------------------------------------------------
        	final boolean loadTopDock = false;

        	if (loadTopDock && DockWindow.getMainDock() != null) {
        		// leave room for dock at top
        		Rectangle maxBounds = GUI.getMaximumWindowBounds();
        		int adj = DockWindow.getCollapsedHeight();
        		maxBounds.y += adj;
        		maxBounds.height -= adj;
        		ApplicationFrame.setMaximizedBounds(maxBounds);
        	}
            
        	if (false) ApplicationFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
                -------------------------------------------------------*/

        	/*
        	if (!SKIP_DR) {
            	LWMap startupMap = null;
            	try {
                	final java.net.URL startupURL;
                	startupURL = VueResources.getURL("resource.startmap");
                	startupMap = OpenAction.loadMap(startupURL);
                	startupMap.setFile(null); // dissassociate startup map from it's file so we don't write over it
                	startupMap.setLabel("Welcome");
                	startupMap.markAsSaved();
            	} catch (Exception ex) {
                	ex.printStackTrace();
                	VueUtil.alert(null, "Cannot load the Start-up map", "Start Up Map Error");
            	}

            	try {
                	if (startupMap != null)
                    	displayMap(startupMap);
            	} catch (Exception ex) {
                	ex.printStackTrace();
                	VueUtil.alert(null, "Failed to display Start-up Map", "Internal Error");
            	}
            
        	} else {
            	//pannerTool.setVisible(true);
        	}
        	 */

        	if (FilesToOpen.size() == 0)
        		VUE.displayMap(new LWMap(VueResources.getString("vue.main.newmap")));

        	// Generally, we need to wait until java 1.5 JSplitPane's have been validated to
        	// use the % set divider location.  Unfortunately there's a bug in at MacOS java
        	// 1.5 BasicSplitPaneUI (it's not in the 1.4 version), where setKeepHidden isn't
        	// being called when the divider goes to the wall via setDividerLocation, only when
        	// the one-touch buttons are manually clicked.  So, for example, if the user
        	// de-maximizes the frame, suddenly a hidden split-pane will pop out!  So, we've
        	// hacked into the UI code, grabbed the damn right-one-touch button, grabbed
        	// it's action listener, and here just call it directly...
        	// 
        	// See javax.swing.plaf.basic.BasicSplitPaneDivider.OneTouchActionHandler.
        	//
        	// It appears on Windows we need to actually wait till the frame is shown also...

        	// show before split adjust on pc
        	if (!Util.isMacPlatform())
        		ApplicationFrame.setVisible(true);
        
        	if (SplitPaneRightButtonOneTouchActionHandler != null) {
        		if (DEBUG.INIT) Util.printStackTrace("\"pressing\": " + SplitPaneRightButtonOneTouchActionHandler);

        		// Not reliable on PC unless we invokeLater
        		if (!VUE.isApplet())
        		{
        			GUI.invokeAfterAWT(new Runnable() { public void run() {
        			SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);                  
        			}});
        		}
        		// this is also eventually getting eaten in java 1.5: no matter where
        		// we put this call during init: will have to patch w/more hacking
        		// or live with it.  Actually, it get's eaten eventually in java 1.4.2
        		// also.

        		// Maybe because we maximized the frame before it was shown?
        		// [ not making a difference]

        		// Well. this is working at least the first time now by
        		// doing it BEFORE the peers are created.
        		//mViewerSplit.setResizeWeight(0.5d);
            
        	} else {
        		// for java 1.4.2
        		mViewerSplit.setDividerLocation(1.0);
        	}

        	// can show after split adjust on mac (turns out: only on older, slower macs)
        	if (Util.isMacPlatform()) {
        		ApplicationFrame.setVisible(true);

            if (SplitPaneRightButtonOneTouchActionHandler != null) {
                // This is backup: hit it one more time just in case, as on the
                // newer, faster intel Mac's, the timing is changed and the
                // above is no longer catching it.
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);                  
                }});
            }
        }
        
        }
        if (toolbarDock != null) {
            toolbarDock.suggestLocation(screen.top, screen.left);
            toolbarDock.setWidth(screen.width);
            toolbarDock.setVisible(true);
        }

        //-----------------------------------------------------------------------------
        //
        // Set locations for the inspector windows and make some of them visible
        //
        //-----------------------------------------------------------------------------

        
        positionDockWindows(screen);

        mapInspectorPanel.metadataPanel.refresh();
        
    
        	//I'm just putting a comment in here becuase this seems odd to me, and I wanted it to be clear it was intentional.
        	//"As we move away from a "datasource" centric vision of VUE, the "Content" window should be collapsed when launching VUE"
        	//This will only take effect the first time VUE is started or when preference to remember window position is disabled.
        	// -MK

        	if (!formatDock.getWindowProperties().isEnabled())
        	{
        		formatDock.setLowerLeftCorner(VueResources.getInt("formatting.location.x"),
        			VueResources.getInt("formatting.location.y")+100);
//        	formatDock.setLocation(,
  //                                     );        	
        	
        		DockWindow.flickerAnchorDock();
            	if (!VUE.isApplet())
            		formatDock.setVisible(true);
        	}
        
    }

    /**
     * There are two main cases to deal with: windows that have no saved position (as when VUE is
     * run the first time, or the window position saving preference is turned off) -- these must be
     * given a reasonable initial position.
     *
     * The other case is to restore the old user size and position of the
     * window.
     *
     * Care must be taken that these two cases don't run into conflict, in particular accidentally
     * giving a "resonable" position to a window that was supposed to be restored, and/or moving
     * windows around in front of the user after they appear on the screen. 
     */
    
    private static void positionDockWindows(final tufts.vue.gui.Screen screen)
    {
        // by default, order the windows left to right across the top
        final List<DockWindow> acrossTopList = new ArrayList<DockWindow>();
        
        if (!MapInspector.getWindowProperties().isEnabled() || !MapInspector.getWindowProperties().isWindowVisible())
            acrossTopList.add(MapInspector);        
       
        if (!pathwayDock.getWindowProperties().isEnabled() || !pathwayDock.getWindowProperties().isWindowVisible())
            acrossTopList.add(pathwayDock);
        
        // if (!DR_BROWSER_DOCK.getWindowProperties().isEnabled() || !DR_BROWSER_DOCK.getWindowProperties().isWindowVisible())
        //     acrossTopList.add(DR_BROWSER_DOCK);        
        if (!ObjectInspector.getWindowProperties().isEnabled() || !ObjectInspector.getWindowProperties().isWindowVisible())
            acrossTopList.add(ObjectInspector);
        
       // if (!metaDataSearchDock.getWindowProperties().isEnabled() || !metaDataSearchDock.getWindowProperties().isWindowVisible())
       // 	acrossTopList.add(metaDataSearchDock);
        
       // if (!mergeMapsDock.getWindowProperties().isEnabled() || !mergeMapsDock.getWindowProperties().isWindowVisible())
       // 	acrossTopList.add(mergeMapsDock);
       // if (!ontologyDock.getWindowProperties().isEnabled() || !ontologyDock.getWindowProperties().isWindowVisible())
        //	acrossTopList.add(ontologyDock);
       
      //  acrossTopList.add(formatDock);
        acrossTopList.add(outlineDock);
        acrossTopList.add(pannerDock);
        acrossTopList.add(metaDataSearchDock);
        acrossTopList.add(layersDock);
        acrossTopList.add(contentDock);
        acrossTopList.add(mergeMapsDock);
        acrossTopList.add(interactionToolsDock);
        acrossTop = acrossTopList.toArray(new DockWindow[acrossTopList.size()]);
        
        if (outlineDock != null)
            outlineDock.setLowerLeftCorner(screen.left, screen.bottomIn);
        if (pannerDock != null)
            pannerDock.setLowerRightCorner(screen.rightIn, screen.bottomIn);
        
        if (DockWindow.getTopDock() != null) {
            // This was for old experimental GUI code -- getTopDock() always returns null these days -- SMF 2012
            prepareForTopDockDisplay(acrossTop);
        }
        
        if (acrossTop.length > 0) {
            // Run after AWT to ensure all peers to have been created & shown
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // TODO: doesn't appear to be allowing old state to rule as is supposed to:
                assignDefaultPositions(acrossTop);
                if (!VUE.isApplet())
                    DockWindow.RestoreAllWindowStates();
            }});
        }
        
        // restoreSavedDockWindowPositions();
        // If we do this here, positionForDocking could run after us, blowing away our restore positions...
        
        // // old positioning code
        // int inspectorx = ApplicationFrame.getX() + ApplicationFrame.getWidth();
        // MapInspector.suggestLocation(inspectorx, ApplicationFrame.getY());
        // ObjectInspector.suggestLocation(inspectorx, ApplicationFrame.getY() + MapInspector.getHeight() );
        // pannerDock.suggestLocation(ApplicationFrame.getX() - pannerDock.getWidth(), ApplicationFrame.getY());
    }

    private static void saveAllWindowProperties() {
        DockWindow.SaveAllWindowStates();
        ApplicationFrame.saveWindowProperties();
    }
    
    // private static void restoreSavedDockWindowPositions() {
    //     if (!VUE.isApplet()) {
    //         // Restore the size & position of windows
    //         // to check: are we affected by Lion+ app window auto-restore?
    //         pathwayDock.positionWindowFromProperties();
    //         formatDock.positionWindowFromProperties();
            
    //         if (slideDock != null)
    //             slideDock.positionWindowFromProperties();
    //         pannerDock.positionWindowFromProperties();
    //         MapInspector.positionWindowFromProperties();
    //         metaDataSearchDock.positionWindowFromProperties();
    //         interactionToolsDock.positionWindowFromProperties();
    //         if (contentDock != null)
    //             contentDock.positionWindowFromProperties();
    //         mergeMapsDock.positionWindowFromProperties();
    //         ObjectInspector.positionWindowFromProperties();
    //         if (outlineDock != null)
    //             outlineDock.positionWindowFromProperties();       
    //         if (layersDock != null)
    //             layersDock.positionWindowFromProperties(); 
    //     }   
    // }
    
    protected static void createDockWindows()
    {
        final tufts.vue.gui.Screen screen = GUI.getScreenForWindow(null);
        
    	  //=============================================================================
        //
        // Create all the DockWindow's
        //
        //=============================================================================

        //-----------------------------------------------------------------------------
        // Pathways panel
        //-----------------------------------------------------------------------------
        pathwayPanel = new PathwayPanel(VUE.getDialogParentAsFrame());
        if (pathwayDock == null  || VUE.isApplet())
        pathwayDock = GUI.createDockWindow(VueResources.getString("dockWindow.presentation.title"),
        		VueResources.getString("dockWindow.Pathways.helpText"), pathwayPanel);

       
        

        //-----------------------------------------------------------------------------
        // Formatting
        //-----------------------------------------------------------------------------

        //formatDock = null;
        floatingZoomPanel = new FloatingZoomPanel();
        if (floatingZoomDock == null || VUE.isApplet())
        {
        	floatingZoomDock = GUI.createDockWindow("Floating Zoom",true);
        	floatingZoomDock.setContent(floatingZoomPanel);
        	//floatingZoomDock.setFocusable(true); // can grab key events causing MapViewer actions to be disabled
        	//floatingZoomDock.setSize(new Dimension(280,30));
        	floatingZoomDock.setHeight(40);
        	floatingZoomDock.setLocation(screen.left, screen.topIn + 15);
        }
        //-----------------------------------------------------------------------------
        // Panner
        //-----------------------------------------------------------------------------
        if (pannerDock == null || VUE.isApplet())
        {
        	pannerDock = GUI.createDockWindow(VueResources.getString("dockWindow.panner.title"),
        			VueResources.getString("dockWindow.Panner.helpText"), new MapPanner());
        	//pannerDock.getWidgetPanel().setBorder(new javax.swing.border.MatteBorder(5,5,5,5, Color.green));
        	//pannerDock.getContentPanel().setBorder(new EmptyBorder(1,2,2,2));
        	//pannerDock.setSize(120,120);
        	//pannerDock.setSize(112,120);
        	//pannerDock.setUpperRightCorner(GUI.GScreenWidth, 150);

        	if (Util.isMacPlatform()) {
        		// Can't do this on PC as 'x' close button is on right
        		pannerDock.setMenuActions(new Action[] {
                    Actions.ZoomFit,
                    Actions.ZoomActual
                });
        	}
        }

        //-----------------------------------------------------------------------------
        // Map Inspector
        //-----------------------------------------------------------------------------
        if (MapInspector == null || VUE.isApplet())
        {
        	MapInspector = GUI.createDockWindow(VueResources.getString("mapInspectorTitle"),
        			VueResources.getString("dockWindow.MapInfo.helpText"));
        	mapInspectorPanel = new MapInspectorPanel(MapInspector);
        	//        MapInspector.setContent(mapInspectorPanel.getMapInfoStack());
        	//      MapInspector.setHeight(450);
        }
        //-----------------------------------------------------------------------------
        // Meta data Search
        //-----------------------------------------------------------------------------
        if (metaDataSearchDock == null || VUE.isApplet())
        {        	
        	metaDataSearchDock = GUI.createDockWindow(VueResources.getString("dockWindow.search.title"),
        			VueResources.getString("dockWindow.Search.helpText"));        	
        	metadataSearchMainPanel = new MetadataSearchMainGUI(metaDataSearchDock);       	
        }
        //-----------------------------------------------------------------------------
        // Interaction Tools panel
        //-----------------------------------------------------------------------------
        if (interactionToolsDock == null || VUE.isApplet())
        {
        	interactionToolsDock = GUI.createDockWindow(VueResources.getString("dockWindow.interactionTools.title"),
        			VueResources.getString("dockWindow.ExplorationTools.helpText"));
        	interactionToolsPanel = new InteractionTools(interactionToolsDock);

            if (depthSelectionControl != null) {
            	depthSelectionControl.addExpandSelectionListener(interactionToolsPanel);
            	interactionToolsPanel.addExpandSelectionListener(depthSelectionControl);
            }
        }
        //-----------------------------------------------------------------------------
        // Content window
        //-----------------------------------------------------------------------------
        if (!SKIP_DR && (contentDock == null || VUE.isApplet()))
        {
        	contentDock = GUI.createDockWindow(VueResources.getString("dockWindow.contentPanel.title"),
        			VueResources.getString("dockWindow.Content.helpText"));
        	contentPanel = new ContentPanel(contentDock);
        	contentDock.setSize(300, (int) (screen.height * 0.75));

        }
      //-----------------------------------------------------------------------------
        // Merge Maps
        //-----------------------------------------------------------------------------
        if (mergeMapsDock == null || VUE.isApplet())
        {        	
        	mergeMapsDock = GUI.createDockWindow(VueResources.getString("dockWindow.mergemaps.title"),
        			VueResources.getString("dockWindow.MergeMaps.helpText"));        	
        	mergeMapsControlPanel = new MergeMapsControlPanel(mergeMapsDock);       	
        }
        //-----------------------------------------------------------------------------
        // Object Inspector / Resource Inspector
        //-----------------------------------------------------------------------------

        //final DockWindow resourceDock = GUI.createDockWindow("Resource Inspector", new ResourcePanel());
        inspectorPane = new tufts.vue.ui.InspectorPane();
        if (ObjectInspector == null || VUE.isApplet())
        {
        	ObjectInspector = GUI.createDockWindow(VueResources.getString("dockWindow.info.title"),
        			VueResources.getString("dockWindow.Info.helpText"));
        	ObjectInspector.setContent(inspectorPane.getWidgetStack());
        	ObjectInspector.setMenuName(VueResources.getString("dockWindow.infopreview.menu"));
        	ObjectInspector.setHeight(575);
        }
        
    	if (layersDock == null || VUE.isApplet())
    	{
    		layersDock = GUI.createDockWindow(VueResources.getString("dockWindow.layers.title"),
    				VueResources.getString("dockWindow.Layers.helpText"), new tufts.vue.ui.LayersUI());
    		//layersDock.setFocusableWindowState(false);
    		layersDock.setSize(300,260);        		
    		layersDock.setLocation(0, 144);
    	}
        
        //-----------------------------------------------------------------------------
        // Slide Viewer
        //-----------------------------------------------------------------------------
        if (false && DEBUG.Enabled) {
            slideViewer = new tufts.vue.ui.SlideViewer(null);
            slideDock = GUI.createDockWindow(slideViewer);
            slideDock.setLocation(100,100);
            VueAction defSize;
            slideDock.setMenuActions(new Action[] {
                    Actions.ZoomFit,
                    Actions.ZoomActual,
                    defSize = new VueAction("1/8 Screen") {
                            public void act() {
                                GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(slideDock.window());
                                Rectangle screen = gc.getBounds();
                                slideDock.setContentSize(screen.width / 4, screen.height / 4);
                            }
                        },
                    new VueAction("1/4 Screen") {
                        public void act() {
                            GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(slideDock.window());
                            Rectangle screen = gc.getBounds();
                            slideDock.setContentSize(screen.width / 2, screen.height / 2);
                        }
                    },
                    new VueAction("Maximize") {
                        public void act() {
                            slideDock.setBounds(GUI.getMaximumWindowBounds(slideDock.window()));
                        }
                    },
                
                });
            defSize.act();
        }
        
        
        //-----------------------------------------------------------------------------
        // Object Inspector
        //-----------------------------------------------------------------------------

        //GUI.createDockWindow("Test OI", new ObjectInspectorPanel()).setVisible(true);
        /*
        ObjectInspector = GUI.createDockWindow(VueResources.getString("objectInspectorTitle"));
        ObjectInspectorPanel = new ObjectInspectorPanel();
        ModelSelection.addListener(ObjectInspectorPanel);
        ObjectInspector.setContent(ObjectInspectorPanel);
        */
        
        //-----------------------------------------------------------------------------
        // Pathway Panel
        //-----------------------------------------------------------------------------

        // todo
        
        //-----------------------------------------------------------------------------
        // Outline View
        //-----------------------------------------------------------------------------

        if (true||!SKIP_DR) {
        
            OutlineViewTree outlineTree = new OutlineViewTree();
            JScrollPane outlineScroller = new JScrollPane(outlineTree);
            VUE.getSelection().addListener(outlineTree);
            //VUE.addActiveMapListener(outlineTree);
            VUE.ActiveMapHandler.addListener(outlineTree);
            outlineScroller.setPreferredSize(new Dimension(500, 300));
            //outlineScroller.setBorder(null); // so DockWindow will add 1 pixel to bottom
            if (outlineDock == null  || VUE.isApplet())
            	outlineDock =  GUI.createDockWindow(VueResources.getString("dockWindow.outline.title"),
            			VueResources.getString("dockWindow.Outline.helpText"), outlineScroller);

            DataSourceViewer.initUI();
            
        }

        //-----------------------------------------------------------------------------
        // Formatting
        //-----------------------------------------------------------------------------

        //formatDock = null;
        formattingPanel = new FormatPanel();
        if (formatDock == null || VUE.isApplet())
        {
        	formatDock = GUI.createDockWindow(VueResources.getString("dockWindow.format.title"),true,true);
        	formatDock.setContent(formattingPanel);
        }
        //formatDock.setFocusable(true);
        //-----------------------------------------------------------------------------

        VUE.UseLeopardAnchor =
               Util.getMacOSXVersion() >= 10.5 // Leopard
            && Util.getMacOSXVersion() <= 10.6 // Snow Leopard
            //Util.isMacLeopard()
            && Util.getJavaVersion() >= 1.5f
            && DockWindow.useManagedWindowHacks()
            && !VUE.isApplet();

        // 2009-07-23 SMF: Current Mac OS X 1.5 JVM appears to NOT need the Leopard anchor,
        // but java 1.6 JVM still does.  This just with one test -- we'll need to keep
        // an eye on this. SMF 2012-06-16: Lion appears not to need the anchor -- disabled
        // for Lion + future versions -- only tested java version "1.6.0_33" 64bit.

        if (VUE.UseLeopardAnchor) {
            // Workaround for DockWindow's going behind the VUE Window on Leopard bug, especially
            // when there's only one DockWindow open.  So we literally create a DockWindow
            // to always hang around, and just set it off screen.
            // See DockWindow.ShowPreviouslyHiddenWindows and FullScreenWindow.FSWindow for more related comments.
            // SMF 2008-04-22
            JTextArea t = new JTextArea("Do Not Close This Window.\nSee http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6373178");
            t.setFont(VueConstants.SmallFont);
            t.setEditable(false);
            anchor = GUI.createDockWindow(VueResources.getString("dockWindow.vueleopard.title"), t);
            if (!DEBUG.DOCK)
                GUI.setOffScreen(anchor.window());
            anchor.pack();
            anchor.setVisible(true);
            // NOTE: changes to the graphics configuration appear to cause a
            // COMPONENT_MOVED event on this window, which could be a way to
            // to auto-detect changes after a "Detect Displays"
        }

    }
    protected static JPanel constructToolPanel(JComponent toolbar)
    {
    	JPanel toolbarPanel = new JPanel();
    	toolbarPanel.setLayout(new GridBagLayout());
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        flowLayout.setVgap(0);
        //toolbarPanel.
        //toolbarPanel.setLayout(flowLayout);
        GridBagConstraints gBC = new GridBagConstraints();

        gBC.fill = GridBagConstraints.NONE;			
		gBC.gridx = 0;
		gBC.gridy = 0;
		gBC.weightx = 0.0;
		gBC.insets = new Insets(0, 0, 0, 0);
		//add(searchResultTbl, gBC);
        //toolbarPanel.add(returnToMapButton,gBC);
        toolbarPanel.add(toolbar,gBC);

        gBC.fill = GridBagConstraints.NONE;			
        gBC.gridx = 1;
        gBC.gridy = 0;
        gBC.weightx = 0.0;
        gBC.insets = new Insets(0, 0, 0, 0);
//        toolbarPanel.add(new BackwardForwardPanel(), gBC);

        returnToMapButton = new JButton(VueResources.getString("returnToMap.label"));        
        returnToMapButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e)
        	{
        		Actions.ReturnToMap.act();
        	}
        });
        tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class,new ActiveListener()
        {

			public void activeChanged(ActiveEvent e) {
				if ((VUE.getActiveViewer()!=null && VUE.getActiveViewer().getFocal()!= null) 
						&& (VUE.getActiveViewer().getFocal() instanceof LWSlide ||
								VUE.getActiveViewer().getFocal() instanceof MasterSlide || 
								VUE.getActiveViewer().getFocal() instanceof LWGroup))
				{
					returnToMapButton.setVisible(true);					
				}
				else
					returnToMapButton.setVisible(false);				    
			}
        }); 
        returnToMapButton.setVisible(false);
		gBC.fill = GridBagConstraints.NONE;			
		gBC.gridx = 2;
		gBC.gridy = 0;
		gBC.weightx = 0.0;
		gBC.insets = new Insets(5, 0, 5, 0);		
        toolbarPanel.add(returnToMapButton,gBC);        

        // Empty JPanel to take up extra horizontal room
        gBC.fill = GridBagConstraints.HORIZONTAL;			
        gBC.gridx = 3;
        gBC.gridy = 0;
        gBC.weightx = 1.0;
        gBC.insets = new Insets(0, 0, 0, 0);
        toolbarPanel.add(new JPanel(), gBC);

        JPanel searchPnl = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        JLabel searchLbl = new JLabel();
        JLabel arrowLbl = new JLabel();
        JLabel clearLbl = new JLabel();
        final JTextField valueTxt = new JTextField();        
        valueTxt.setMinimumSize(new Dimension(100,23));
        valueTxt.setPreferredSize(new Dimension(100,23));
        searchLbl.setIcon(tufts.vue.VueResources
				.getImageIcon("search.toolbar.find"));
        searchLbl.addMouseListener(new MouseAdapter()
		{			   
		    public void mouseClicked(MouseEvent e)
		    {
		       //TO DO
//		       System.err.println("Clicked on Search Icon:::::");
		    }
		});
        arrowLbl.setIcon(tufts.vue.VueResources
				.getImageIcon("search.toolbar.arrow"));	 
        arrowLbl.addMouseListener(new MouseAdapter()
		{			   
		    public void mouseClicked(MouseEvent e)
		    {
		       //TO DO
//		       System.err.println("Clicked on Down Arrow:::::");
		    }
		});
        clearLbl.setIcon(tufts.vue.VueResources
				.getImageIcon("search.toolbar.clear"));	 
        clearLbl.addMouseListener(new MouseAdapter()
		{       	
        	 public void mouseClicked(MouseEvent e)
 		    {        		
        		valueTxt.setText("");  		
        		
 		    }        	 
		});
        searchPnl.add(searchLbl);
        searchPnl.add(arrowLbl);
        valueTxt.setBorder(BorderFactory.createMatteBorder(
                1, 0, 1, 0, Color.gray));       
        
        searchPnl.add(valueTxt);
        searchPnl.add(clearLbl);
        searchPnl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); 
        searchPnl.setPreferredSize(new Dimension(155,23));
        searchPnl.setSize(new Dimension(155,23));
        searchPnl.setMaximumSize(new Dimension(155,23));
//        toolbarPanel.add(searchPnl, SwingConstants.LEFT); 
//        JPanel searchPanel = new JPanel();
//        searchPanel.setPreferredSize(new Dimension(200,25));
//        searchPanel.setSize(new Dimension(200,25));
//        searchPanel.setMaximumSize(new Dimension(200,25));        
//       
//        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));        
//        
        //searchPanel.setBorder(BorderFactory.createLineBorder(Color.red,1));
        //sliderSearchPanel = new JPanel(new FlowLayout());         
               
        //framesPerSecond.setMajorTickSpacing(6);
        //framesPerSecond.setPaintTicks(true);

        final int leftPad = 4;
        final int rightPad = 4;

        final GridBagConstraints searchPanelGBC =
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0,leftPad,0,rightPad),
                                   0, 0);

        depthSelectionControl = new ExpandSelectionControl();
        searchPanel.add(depthSelectionControl, searchPanelGBC);

        // This empty border will align search field with the MapViewer below on Mac OS X.
        // We'll probably want to tweak this by a few pixels either way for Windows.
        //searchPanel.setBorder(GUI.makeSpace(0,0,0,13-rightPad));
        
        if (interactionToolsPanel != null) {
        	depthSelectionControl.addExpandSelectionListener(interactionToolsPanel);
        	interactionToolsPanel.addExpandSelectionListener(depthSelectionControl);
        }
     
        //final JComponent searchField = new SearchTextField();
        final JComponent searchField = mSearchTextField;
        
        // final javax.swing.ImageIcon searchTigerImg = VueResources.getImageIcon("search.tiger.searchicon");
        // searchPanelGBC.gridx = 0;
        // searchPanelGBC.weightx = 1;
        // searchPanelGBC.anchor = GridBagConstraints.WEST;
        // searchPanelGBC.fill = GridBagConstraints.NONE;
        // searchPanel.add(new JLabel(searchTigerImg));
        
        searchPanelGBC.gridx = 1;
        searchPanelGBC.anchor = GridBagConstraints.EAST;
        searchPanelGBC.fill = GridBagConstraints.HORIZONTAL;
        searchPanelGBC.weightx = 1.0;
        searchPanel.add(searchField, searchPanelGBC);

        if (!VUE.isApplet()) {
            gBC.fill = GridBagConstraints.HORIZONTAL;			
            gBC.gridx = 4;
            gBC.gridy = 0;
            gBC.weightx = 1.0;
            gBC.insets = new Insets(0, 0, 0, 0);
            toolbarPanel.add(searchPanel, gBC);
            // Need to add a tiny bit of pad at right:
            //if (DEBUG.Enabled) toolbarPanel.add(new JLabel(" "));
        }

        if (DEBUG.INIT) out("created ToolBar");
        
        return toolbarPanel;

    }   
    static class TabStopDocument extends PlainDocument {

    	  final JTextField textField;

    	  int tabStop;

    	  TabStopDocument(JTextField textField, int tabStop){

    	    this.textField = textField;
    	    this.textField.addFocusListener(new FocusAdapter() {
        		  public void focusLost(FocusEvent e) {
        			
        		  }
        		  public void focusGained(FocusEvent e) {
        			  
        		  }
        	});     
    	    this.tabStop = tabStop;

    	  }

    	 

    	  private String tabStopString(){

    	    String tabStopString = "";

    	    for(int i=0; i<tabStop; ++i){

    	      tabStopString += " ";
    	    }

    	    return tabStopString;

    	  }

    	 

    	  protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {

    	    int offset = chng.getOffset();

    	    if ( offset <= tabStop){

    	      return;

    	    }
    	    super.insertUpdate(chng, attr);

    	  }

    	 

    	  protected void removeUpdate(DefaultDocumentEvent chng) {

    	    int offset = chng.getOffset()+1;

    	    if ( offset <= tabStop){

    	      chng.undo();

    	      return;

    	    }  	 

    	    super.removeUpdate(chng);

    	  }
    	 

    	  public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {    		  

    		    if (str == null)

    		      return;

    		    if (textField.getText().trim().length() == 0 ){

    		      if ( textField.getText().length() == tabStop){
    		       

    		      } else {

    		        if (textField.getText().trim().length() == 0 && offset <= tabStop) {

    		          str = tabStopString() + str;

    		        }

    		      }

    		    }      		 

    		    if (textField.getText().trim().length() != 0 && offset <= tabStop) {

    		      return;

    		    }
    		 super.insertString(offset, str, attr);
    	}
    }
    static public class SubtleSquareBorder implements Border
    {
	    protected int m_w = 6;
	    protected int m_h = 6;
	    protected Color m_topColor = Color.gray;
	    protected Color m_bottomColor = Color.gray;
	    protected boolean roundc = false; // Do we want rounded corners on the border?
	
	    public SubtleSquareBorder(boolean round_corners)
	    {	
	      roundc = round_corners;
	    }
	
	    public Insets getBorderInsets(Component c)
	    {
	    return new Insets(m_h, m_w, m_h, m_w);
	    }
	
	    public boolean isBorderOpaque()
	    {
	    return true;
	    }
	
	    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h)
	    {
	    w = w - 3;
	    h = h - 3;
	    x ++;
	    y ++;
	
	    // Rounded corners
	    if(roundc)
	    {
	    g.setColor(m_topColor);
	    g.drawLine(x, y + 2, x, y + h - 2);
	    g.drawLine(x + 2, y, x + w - 2, y);
	    g.drawLine(x, y + 2, x + 2, y); // Top left diagonal
	    g.drawLine(x, y + h - 2, x + 2, y + h); // Bottom left diagonal
	    g.setColor(m_bottomColor);
	    g.drawLine(x + w, y + 2, x + w, y + h - 2);
	    g.drawLine(x + 2, y + h, x + w -2, y + h);
	    g.drawLine(x + w - 2, y, x + w, y + 2); // Top right diagonal
	    g.drawLine(x + w, y + h - 2, x + w -2, y + h); // Bottom right diagonal	    
	    }
	
	    // Square corners
	    else
	    {
	    g.setColor(m_topColor);
	    g.drawLine(x, y, x, y + h);
	    g.drawLine(x, y, x + w, y);
	    g.setColor(m_bottomColor);
	    g.drawLine(x + w, y, x + w, y + h);
	    g.drawLine(x, y + h, x + w, y + h);
	    }
	
	    }

    }
    public static FormatPanel getFormattingPanel()
    {
    	return formattingPanel;
    }
        
    public static FloatingZoomPanel getFloatingZoomPanel()
    {
    	return floatingZoomPanel;
    }
    public static DockWindow getInfoDock()
    {
    	return ObjectInspector;
    }
    static void _setInfoDock(DockWindow dw) { ObjectInspector = dw; }
    
    public static DockWindow getFloatingZoomDock()
    {
    	return floatingZoomDock;
    }
    
    public static DockWindow getPannerDock()
    {
    	return pannerDock;
    }
    
    public static DockWindow getOutlineDock()
    {
    	return outlineDock;
    }
    
    public static DockWindow getSlideDock()
    {
    	return slideDock;
    }
    
    public static MapInspectorPanel getMapInspectorPanel()
    {
    	return mapInspectorPanel;
    }    
    
    public static DockWindow getMapInfoDock()
    {
    	return MapInspector;
    }
    
    public static MetadataSearchMainGUI getMetadataSearchMainPanel()
    {
    	return metadataSearchMainPanel;
    }    
    
    public static DockWindow getMetadataSearchMainGUI()
    {
    	return metaDataSearchDock;
    }
    
    public static InteractionTools getInteractionToolsPanel()
    {
    	return interactionToolsPanel;
    }    
    
    public static DockWindow getInteractionToolsDock()
    {
    	return interactionToolsDock;
    }    
    
    public static ContentPanel getContentPanel()
    {
    	return contentPanel;
    }    
    
    public static DockWindow getContentDock()
    {
    	return contentDock;
    }    
    
    public static MergeMapsControlPanel getMergeMapsControlPanel()
    {
    	return mergeMapsControlPanel;
    }    
    
    public static DockWindow getMergeMapsDock()
    {
    	return mergeMapsDock;
    }
    public static DockWindow getFormatDock()
    {
    	return formatDock;
    }
    
    public static DockWindow getAnchorDock()
    {
    	return anchor;
    }
    
    public static boolean usingAnchorDock()
    {
    	return VUE.UseLeopardAnchor;
    }
    
    public static DockWindow getPresentationDock()
    {
    	return pathwayDock;
    }
    
    public static DockWindow getLayersDock()
    {
    	return layersDock;
    }
    
    public static PathwayPanel getPathwayPanel()
    {
    	return pathwayPanel;
    }

    /**
     * Get the given windows displayed, but off screen, ready to be moved
     * into position.
     */
    private static void prepareForTopDockDisplay(final DockWindow[] preShown)
    {
        if (DEBUG.INIT || DEBUG.DOCK) Util.printStackTrace("\n\n***ROLLING UP OFFSCREEN");

        // get the peer's created so we can turn off their shadow if need be
        
        for (int i = 0; i < preShown.length; i++) {
            DockWindow dw = preShown[i];
            if (dw == null)
                continue;
            GUI.setOffScreen(dw.window());

            dw.setDockTemporary(DockWindow.getTopDock());
            
            dw.showRolledUp();
        }

    }

    /**
     * This used to be called "positionForDocking" when we we're using the idea of ui DockRegions.
     */
    public static void assignDefaultPositions(DockWindow[] preShown) {
        // Set last in preSown at the right, moving back up list
        // setting them to the left of that, and then set first in
        // preShown at left edge of screen

        if (DEBUG.DOCK) Log.debug("assignDefaultPositions " + Arrays.asList(preShown));
        if (DEBUG.INIT || (DEBUG.DOCK && DEBUG.META)) Util.printStackTrace("\n\nSTARTING PLACEMENT");

        final tufts.vue.gui.Screen screen = GUI.getScreenForWindow(null);
        
        int top = screen.topIn;

        if (ToolbarAtTopScreen)
            top += DockWindow.ToolbarHeight;
        else
            top += 152; // todo: tweak for PC

        boolean squeezeDown = screen.width < 1200;
        boolean didSqueeze = false;

        int nextLayout = preShown.length - 1;

        if (squeezeDown) {
            // Swap last two, so "last" is the one pushed down
            DockWindow tmp = preShown[nextLayout];
            preShown[nextLayout] = preShown[nextLayout - 1];
            preShown[nextLayout - 1] = tmp;
        }
        
        DockWindow toRightDW = null;
       
        toRightDW = preShown[nextLayout];
        
        toRightDW.setUpperRightCorner(screen.width, top);
        if (squeezeDown)
            toRightDW.setHeight(toRightDW.getHeight() / 2);
        DockWindow curDW = null;
        while (--nextLayout > 0) {
            curDW = preShown[nextLayout];
            if (curDW == null) {
                // This will happen if we run with -nodr
                Log.debug("Missing DockWindow at index " + nextLayout);
                continue;
            }
            if (squeezeDown && !didSqueeze) {
                didSqueeze = true;
                toRightDW.addChild(curDW);
            } else {
            	int spot = screen.right;
            	if (nextLayout %3 != 0) {
                    spot = toRightDW.getX();
            	} else
                    top +=50;
                curDW.setUpperRightCorner(spot, top);
                toRightDW = curDW;
//             	int spot=GUI.GScreenWidth;
//             	if (nextLayout %3 != 0) {
//                     spot = toRightDW.getX();
//             	} else
//                     top +=50;
//                 curDW.setUpperRightCorner(spot, top);
//                 toRightDW = curDW;
            }
        }
        if (preShown.length > 1)
            preShown[0].setLocation(0, top);
            
        DockWindow.assignAllDockRegions();
    }
    
    

    
    private static ActionListener SplitPaneRightButtonOneTouchActionHandler = null;

    public static boolean toggleSplitScreen()
    {
        // Oops -- no good for updating the action state -- will need
        // to extract both the AbstractButtons for the expand & collapse
        // (currently we only grab the ActionListener for the collapse button),
        // and manually add an action listener to them in our ToggleSplitScreen
        // action.  Or, maybe easier, would be to monitor the display events
        // on the right MapViewer component or PropertyChangeEvents on mViewerSplit.
        
    	if (mViewerSplit.getDividerLocation() >= mViewerSplit.getMaximumDividerLocation()) {
            // open to split:
            mViewerSplit.setDividerLocation(0.50D);
            return true;
    	} else {
            // close the split:
            SplitPaneRightButtonOneTouchActionHandler.actionPerformed(null);
            return false;
        }
    }      
       
    	 
    private static JSplitPane buildSplitPane(Component leftComponent, Component rightComponent)
    {
        JSplitPane split;
        if (Util.getJavaVersion() < 1.5f) {
            split = new JSplitPane();
        } else {

            // Only appears to happen on the Mac?  But even if we're
            // running with Metal Look and Feel??
            
            split = new JSplitPane() {
                
                // This JSplitPane hack is dependent on the UI implementation, but it only
                // uses the cross platform parts: see bottom of this method for more info.
                
                Container divider;
                public void XsetUI(javax.swing.plaf.SplitPaneUI newUI) {
                    Util.printStackTrace("setUI: " + newUI);
                    super.setUI(newUI);
                }
                @Override
                protected void addImpl(Component c, Object constraints, int index) {
                    //out("splitPane.addImpl: index=" + index + " constraints=" + constraints + " " + GUI.name(c));
                    if (c instanceof javax.swing.plaf.basic.BasicSplitPaneDivider) {
                        //Util.printStackTrace("addImpl: divider is " + c);
                        divider = (Container) c;
                    }
                    super.addImpl(c, constraints, index);
                }
                @Override
                public void addNotify() {
                    //Util.printStackTrace("splitPane.addNotify");
                    super.addNotify();
                    try {
                    
                        // Util.printStackTrace("addNotify");
                        //AbstractButton jumpLeft = (AbstractButton) divider.getComponent(0);
                        
                        AbstractButton jumpRight = (AbstractButton) divider.getComponent(1);
                        //System.err.println("child0 " + jumpLeft);
                        //System.err.println("child1 " + jumpRight);
                        
                        //System.err.println(Arrays.asList(jumpLeft.getActionListeners()));
                        //System.err.println(Arrays.asList(jumpRight.getActionListeners()));
                        
                        // todo: as long as we're grabbing this out, add a short-cut key
                        // to activate it: the arrow buttons are so damn tiny!

                        SplitPaneRightButtonOneTouchActionHandler
                            = jumpRight.getActionListeners()[0];

                        // BTW: can't call action listener now: must wait till after validation
                        
                    } catch (Throwable t) {
                        Util.printStackTrace(t);
                    }
                }
            };
        }

        split.setName("splitPane");
        split.setResizeWeight(0.5d);
        split.setOneTouchExpandable(true);
        split.setRightComponent(rightComponent);
        
        // NOTE: set left component AFTER set right component -- the LAST set left/right
        // call determines the default focus component  It needs to be the LEFT
        // component as the right one isn't even visible at startup.
        
        split.setLeftComponent(leftComponent);
        

        split.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    //System.out.println("VS " + e);
                    if (!e.getPropertyName().equals("dividerLocation"))
                        return;
                    if (DEBUG.TOOL || DEBUG.INIT || DEBUG.FOCUS)
                        out("split.propertyChange[" + e.getPropertyName() + "] "
                                        + "\n\tnew=" + e.getNewValue().getClass().getName()
                                        + " " + e.getNewValue()
                                        + "\n\told=" + e.getOldValue()
                                        + "\n\tsrc=" + GUI.name(e.getSource())
                                        );

                        //Util.printStackTrace();
                    
                    MapViewer leftViewer = null;
                    MapViewer rightViewer = null;
                    if (mMapTabsLeft != null)
                        leftViewer = mMapTabsLeft.getSelectedViewer();
                    if (mMapTabsRight != null)
                        rightViewer = mMapTabsRight.getSelectedViewer();

                    if (multipleMapsVisible()) {
                        /*
                          // should be handled by MapVewer.reshape
                        if (leftViewer != null)
                            leftViewer.fireViewerEvent(MapViewerEvent.PAN);
                        if (rightViewer != null)
                            rightViewer.fireViewerEvent(MapViewerEvent.PAN);
                        */
                        // why did we ever need to handle this condition?  was this just-in-case code?
                        if (leftViewer != null) leftViewer.setVisible(true);
                        if (rightViewer != null) rightViewer.setVisible(true);
                    } else {
                        if (leftViewer != null && leftViewer != getActiveViewer()) {
                            if (DEBUG.TOOL || DEBUG.FOCUS)
                                out("split: active viewer: " + getActiveViewer()
                                    + " focus going to " + leftViewer);
                            leftViewer.requestFocus();
                            if (rightViewer != null)
                                //rightViewer.fireViewerEvent(MapViewerEvent.HIDDEN);
                                rightViewer.setVisible(false);
                        }
                    }
                }});

        return split;
    }

    /*
    private static void XbuildToolbar(DockWindow toolbarDock, JPanel toolPanel) {
        
        if (JIDE_TEST) {
            /* JIDE ENABLE
            frame.getDockableBarManager().addDockableBar(new VueToolBar());
            frame.getDockableBarManager().setShowInitial(false);            
            frame.getDockableBarManager().resetToDefault();
            *
        } else if (true||VUE.TUFTS) {
            //toolBarPanel = new JPanel();
            //toolBarPanel.add(tbc.getToolbar());
            if (toolbarDock == null)
                //ApplicationFrame.addComp(tbc.getToolbar(), BorderLayout.NORTH);
                ApplicationFrame.addComp(toolPanel, BorderLayout.NORTH);
        } else {

            //JDialog.setDefaultLookAndFeelDecorated(false);
            
            JPanel toolBarPanel = null;
            toolBarPanel = new JPanel(new BorderLayout());
            //toolBarPanel.add(tbc.getToolbar(), BorderLayout.NORTH);
            JPanel floatingToolbarContainer = new JPanel(new BorderLayout());
            //JPanel floatingToolbarContainer = new com.jidesoft.action.DockableBarDockableHolderPanel(frame);
            
            //floatingToolbarContainer.setPreferredSize(new Dimension(500,50));
            //floatingToolbarContainer.setMinimumSize(new Dimension(500,5));
            floatingToolbarContainer.setBackground(Color.orange);
            VueToolBar vueToolBar = new VueToolBar();
            floatingToolbarContainer.add(vueToolBar, BorderLayout.PAGE_START);
            //toolBarPanel.add(new VueToolBar(), BorderLayout.SOUTH);
            if (false) {
                // Yes: drop-downs work in a JToolBar (note that our MenuButtons
                // that are rounded become square tho)
                JToolBar tb = new JToolBar();
                tb.add(tbc.getToolbar());
                toolBarPanel.add(tb);
            } else {
                toolBarPanel.add(tbc.getToolbar(), BorderLayout.NORTH);
            }
            toolBarPanel.add(floatingToolbarContainer, BorderLayout.SOUTH);
            ApplicationFrame.addComp(toolBarPanel, BorderLayout.NORTH);

            ////frame.getDockableBarManager().addDockableBar(vueToolBar);
            
        }
    }

*/
    
//     public static int openMapCount() {
//         return ActiveMapHandler.instanceCount();
//         //return mMapTabsLeft == null ? 0 : mMapTabsLeft.getTabCount();
//     }

    
    public static boolean multipleMapsVisible() {
        if (mViewerSplit == null)
            return false;
        // TODO: this is no longer a reliable method of determining this in java 1.5
        int dl = mViewerSplit.getDividerLocation();
        return dl >= mViewerSplit.getMinimumDividerLocation()
            && dl <= mViewerSplit.getMaximumDividerLocation();
        
    }
    
    public static JTabbedPane getTabbedPane() {
        return getLeftTabbedPane();
    }
    
    public static MapTabbedPane getLeftTabbedPane() {
        return mMapTabsLeft;
    }

    public static MapTabbedPane getRightTabbedPane() {
        return mMapTabsRight;
    }

    public static Collection<LWMap> getAllMaps() {
        if (mMapTabsLeft != null)
            return mMapTabsLeft.getAllMapsBag();
        else
            return Collections.EMPTY_LIST;
    }

    public static void layoutAllMaps(Object layoutKey) {
        for (LWMap map : getAllMaps())
            map.layoutAll(layoutKey);
    }
    

    public static boolean isActiveViewerOnLeft() {
        final MapViewer activeViewer = ActiveViewerHandler.getActive();
        return activeViewer == null || activeViewer.getName().startsWith("*");
    }

    public static boolean isActiveViewerOnRight() {
        final MapViewer activeViewer = ActiveViewerHandler.getActive();
        return activeViewer != null && activeViewer.getName().equals("right");
    }
    
    
    public static UndoManager getUndoManager() {
        
        // todo: eventually, way may want to ask the active viewer for
        // it's undo manager, e.g. -- if we want the slide viewer to
        // have it's own undo queue.
        
        LWMap map = getActiveMap();
        if (map != null)
            return map.getUndoManager();
        else
            return null;
    }
    
    public static void markUndo() {
        markUndo(null);
    }
    
    /** mark prior change(s) with the given undo name */
    public static void markUndo(String name) {
        LWMap map = getActiveMap();
        if (map != null) {
            UndoManager um = map.getUndoManager();
            if (um != null) {
                if (name != null)
                    um.markChangesAsUndo(name);
                else
                    um.mark();
            }
        }
    }

    /**
     * If any open maps have been modified and not saved, run
     * dialogs to determine what to do.
     * @return true if we're cleared to exit, false if we want to abort the exit
     */
    public static boolean isOkayToExit() {
       //update the windows properties

        try {
            saveAllWindowProperties();
        } catch (Throwable t) {
            Log.error("saving window props", t);
        }
       
        if (mMapTabsLeft == null) // so debug harnesses can quit (no maps displayed)
            return true;
       
        // TODO: use active map instances
        int tabs = mMapTabsLeft.getTabCount();
     //   LWMap ensureChecked = getActiveMap(); // in case of full-screen
        // disabled indexing
/**        
        for (int i = 0; i < tabs; i++) {
            final LWMap map = mMapTabsLeft.getMapAt(i);
            if (VUE.getIndex() != null) {
                System.out.println("indexing map "+map.getLabel()+ " index size="+VUE.getIndex().size());
                try {
                    VUE.getIndex().index(map);
                } catch (Throwable t) {
                    Util.printStackTrace(t, "Exception indexing duing exit: " + map);
                }
            }
        
//             if (map == ensureChecked)
//                 ensureChecked = null;
            if (!askSaveIfModified(mMapTabsLeft.getMapAt(i)))
                return false;
        }
   **/     
/**
        if (getIndex() != null) {
            try {
                VUE.getIndex().write(new FileWriter(VueUtil. getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file")));
                System.out.println("Writing index to"+VueUtil. getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file"));
            } catch (Throwable t) {
                System.out.println("Exception attempting to save index " +t);
                t.printStackTrace();
            }
        }
**/
        LWMap ensureChecked = getActiveMap(); // in case of full-screen
        for (int i = 0; i < tabs; i++) 
        {
        	
           final LWMap map = mMapTabsLeft.getMapAt(i);
           if (map == ensureChecked)           
        	   ensureChecked = null;
           
        	   if (!askSaveIfModified(mMapTabsLeft.getMapAt(i)))
        		   return false;
          
        }
        
        if (ensureChecked != null)
        {
        	   if (!askSaveIfModified(ensureChecked))
        		   return false;
        }
        return true;
        
   //      if (ensureChecked != null && !askSaveIfModified(ensureChecked))
   //          return false;
   //      else
   //          return true;
             
    }
    private static boolean askIfRevertOK(LWMap map)
    {
    	final Object[] defaultOrderButtons = { VueResources.getString("optiondialog.revertlastsave.yes"),VueResources.getString("optiondialog.revertlastsave.cancel")};
    	
    	 if (!map.isModified())
             return true;
    	 
    	   // todo: won't need this if full screen is child of root frame
         if (inNativeFullScreen())
             toggleFullScreen();
         
         Component c = VUE.getDialogParent();
         
         if (VUE.getDialogParent() != null)
         {
         	//Get the screen size
         	Toolkit toolkit = Toolkit.getDefaultToolkit();
         	Dimension screenSize = toolkit.getScreenSize();
         	
         	
         	
         	Point p = c.getLocationOnScreen();
         	
         	if ((p.x + c.getWidth() > screenSize.width) ||
         			(p.y + c.getHeight() > screenSize.height))
         	{
         		c = null;
         	}
         }
         int response = VueUtil.option
             (c,
              VueResources.getString("optiondialog.revertlastsave.message"),         
              VueResources.getString("optiondialog.revertlastsave.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.PLAIN_MESSAGE,
              defaultOrderButtons,             
              VueResources.getString("optiondialog.revertlastsave.cancel")
              );
         
               
         // If they change focus to another button, then hit "return"
         // (v.s. "space" for kbd button press), do action of button
         // that had focus instead of always save?
         
         if (response == JOptionPane.YES_OPTION) { // Save
             return true;
         } else if (response == JOptionPane.NO_OPTION) { // Don't Save
             // don't save -- just close
             return false;
         } else // anything else (Cancel or dialog window closed)
             return false;
    }
    /*
     * Returns true if either they save it or say go ahead and close w/out saving.
     */
    static boolean askSaveIfModified(LWMap map) {
        //final Object[] defaultOrderButtons = { "Save", "Don't Save", "Cancel"};
    	final Object[] defaultOrderButtons = { VueResources.getString("optiondialog.savechages.dontsave"),VueResources.getString("optiondialog.revertlastsave.cancel"),VueResources.getString("optiondialog.savechages.save")};
    	final Object[] macOrderButtons = { VueResources.getString("optiondialog.savechages.save"),VueResources.getString("optiondialog.revertlastsave.cancel"),VueResources.getString("optiondialog.savechages.dontsave")};
        // oddly, mac aqua is reversing order of these buttons
        //final Object[] macAquaOrderButtons = { "Cancel", "Don't Save", "Save" };
        
    	
        if (!map.isModified() || !map.hasContent())
            return true;

        // todo: won't need this if full screen is child of root frame
        if (inNativeFullScreen())
            toggleFullScreen();
        
        Component c = VUE.getDialogParent();
        
        if (VUE.getDialogParent() != null)
        {
        	//Get the screen size
        	Toolkit toolkit = Toolkit.getDefaultToolkit();
        	Dimension screenSize = toolkit.getScreenSize();
        	
        	
        	
        	Point p = c.getLocationOnScreen();
        	
        	if ((p.x + c.getWidth() > screenSize.width) ||
        			(p.y + c.getHeight() > screenSize.height))
        	{
        		c = null;
        	}
        }

        final String debug;

        if (DEBUG.EVENTS || DEBUG.UNDO)
            debug = "\n[modifications="+map.getModCount()+"]";
        else
            debug = "";
        
        int response = VueUtil.option
            (c,
             VueResources.getString("optiondialog.savechages.message")
             + " '" + map.getLabel() + "'?"
             + debug,
        
             VueResources.getString("optiondialog.savechages.title"),
             JOptionPane.YES_NO_CANCEL_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons,             
             VueResources.getString("optiondialog.savechages.save")
             );
        
     
        if (!Util.isMacPlatform()) {
            switch (response) {
            case 0: response = 1; break;
            case 1: response = 2; break;
            case 2: response = 0; break;
            }
        } else {
            switch (response) {
            case 0: response = 0; break;
            case 1: response = 2; break;
            case 2: response = 1; break;
            }
        }
        
        // If they change focus to another button, then hit "return"
        // (v.s. "space" for kbd button press), do action of button
        // that had focus instead of always save?
        
        if (response == JOptionPane.YES_OPTION) { // Save
            return SaveAction.saveMap(map);
        } else if (response == JOptionPane.NO_OPTION) { // Don't Save
            // don't save -- just close
            return true;
        } else // anything else (Cancel or dialog window closed)
            return false;
    }
    
    public static void closeMap(LWMap map) {
        closeMap(map,false);
    }
    
    public static void closeMap(LWMap map, boolean reverting) {
    
    	if (!reverting)
    	{
    		if (askSaveIfModified(map)) {
    			 try{
    		 
    			mMapTabsLeft.closeMap(map);
    			}
    			catch(ArrayIndexOutOfBoundsException abe){}
    			try
    			{
    				if (mMapTabsRight != null)
    					mMapTabsRight.closeMap(map);
    			}
    			catch(ArrayIndexOutOfBoundsException abe){}
    		 
    		}
    	}
    	else
    	{
    		if (askIfRevertOK(map)) {
    			mMapTabsLeft.closeMap(map);
    			if (mMapTabsRight != null)
    				mMapTabsRight.closeMap(map);
    		}	
    	}    	
    	if (mMapTabsRight !=null)
    	{
    		int selectedIndex = mMapTabsRight.getTabCount();
    		if(selectedIndex>0){
    			setMapActionsEnabled(true);
    		}else{
    		setMapActionsEnabled(false);
    		}
    	}
    	else if (mMapTabsLeft == null)
    	{
    		setMapActionsEnabled(false);    		
    	}
    	else
    	{
    		int selectedIndex = mMapTabsLeft.getTabCount();
    		if(selectedIndex>0){
    			setMapActionsEnabled(true);
    		}else{
    		setMapActionsEnabled(false);
    		}
    	}

//    	if(selectedIndex>0){
//    		VueMenuBar.RootMenuBar.saveAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(true);
//        	Actions.Revert.setEnabled(true);
//    	}else{
//    		VueMenuBar.RootMenuBar.saveAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(false);
//        	Actions.Revert.setEnabled(false);
//    	}

    }
    
    
    /**
     * If we already have open a map tied to the given file, display it.
     * Otherwise, open it anew and display it.
     */
    public static void displayMap(File file) {
        if (VUE.isStartupUnderway() || DEBUG.INIT || DEBUG.IO) Log.info("displayMap " + Util.tags(file));

        // Call initDataSources again just in case a user can make it to the file
        // open-recent menu before the data sources finish loading on the remaining
        // "main" thread.  If it's still running, we'll just block until it's done, as
        // this method is synchronized.
        initDataSources();

        if (file == null)
            return;

        if (VUE.isApplet())
        {
        	if (	(getActiveMap() != null) && 
    				!getActiveMap().hasContent() && 
    				getActiveMap().getFile() == null)
    		{
    			try
    			{
    				closeMap(getActiveMap());
    			}
    			catch(ArrayIndexOutOfBoundsException abe)
    			{
    				abe.printStackTrace();
    			}
    		}
        }
        else
        {
        	/*
        	 * If there is 1 map open, and it has no content and hasn't been saved yet close it.
        	 * requested in vue-520 
        	 */
        	if (isActiveViewerOnLeft())
        	{
        		if ((mMapTabsLeft != null) && 
        				mMapTabsLeft.getTabCount() == 1 && 
        				(getActiveMap() != null) && 
        				!getActiveMap().hasContent() && 
        				getActiveMap().getFile() == null)
        		{
        			try
        			{
        				closeMap(getActiveMap());
        			}
        			catch(ArrayIndexOutOfBoundsException abe)
        			{
        				abe.printStackTrace();
        			}
        		}
        	
        	
        	} else 
        	{
        		if ((mMapTabsRight != null) && 
            			mMapTabsRight.getTabCount() == 1 && 
            			(getActiveMap() != null) && 
            			!getActiveMap().hasContent() && 
            			getActiveMap().getFile() == null)
            		closeMap(getActiveMap());
            	
        	}
        }

        
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(file)) {
                if (DEBUG.Enabled) out("displayMap found existing open map " + map + " matching file " + file);
                if (isActiveViewerOnLeft())
                    mMapTabsLeft.setSelectedIndex(i);
                else
                    mMapTabsRight.setSelectedIndex(i);
                return;
            }
        }
        
//         for (LWMap map : ActiveMapHandler.getAllInstances()) {
//             File existingFile = map.getFile();
//             if (existingFile != null && existingFile.equals(file)) {
//                 if (DEBUG.Enabled) out("displayMap found existing open map " + map + " matching file " + file);
//                 ActiveMapHandler.setActive(file, map);
//                 // TODO: sanity check this... (oh, and I supposed we can use the tab panes again... don't need active instances tracking!)
//                 //mMapTabsLeft.setSelectedIndex(i);
//                 return;
//             }
//         }

        final RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
        rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
        VUE.activateWaitCursor();
        LWMap loadedMap = null;
        boolean alerted = false;
        try {
            loadedMap = OpenAction.loadMap(file.getAbsolutePath());
            alerted = true; // OpenAction.loadMap now always alerts
            if (loadedMap != null)
                VUE.displayMap(loadedMap);            
            VUE.getMetadataSearchMainPanel().fillSavedSearch();
        } catch (Throwable t) {
            Util.printStackTrace(t, "failed to load map[" + file + "]");
            VUE.clearWaitCursor();
            alerted = true;
            VueUtil.alert(VueResources.getString("dialog.failedtoloadmap.message")+" " + file + "  \n"
                          + (t.getCause() == null ? t : t.getCause()),
                          VueResources.getString("dialog.failedtoloadmap.message")+" "+ file);
        } finally {
            VUE.clearWaitCursor();
        }
        if (loadedMap == null && !alerted)
            VueUtil.alert(VueResources.getString("dialog.failedtoloadmap.message")+" "+ file + "  \n", VueResources.getString("dialog.failedtoloadmap.message") + file);    
       
        
//        if(getActiveMap()==null){
//        	VueMenuBar.RootMenuBar.saveAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(false);
//        	Actions.Revert.setEnabled(false);
//        }else{
//        	VueMenuBar.RootMenuBar.saveAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(true);
//        	Actions.Revert.setEnabled(true);
//        }
    }

    /**
     * If we already have open a map tied to the given file, display it.
     * Otherwise, open it anew and display it.
     */
    public static void displayMap(java.net.URL url) {
        if (VUE.isStartupUnderway() || DEBUG.INIT || DEBUG.IO) Log.info("displayMap " + Util.tags(url));

        // Call initDataSources again just in case a user can make it to the file
        // open-recent menu before the data sources finish loading on the remaining
        // "main" thread.  If it's still running, we'll just block until it's done, as
        // this method is synchronized.
        initDataSources();

        if (url == null)
            return;

        if (VUE.isApplet())
        {
        	if (	(getActiveMap() != null) && 
    				!getActiveMap().hasContent() && 
    				getActiveMap().getFile() == null)
    		{
    			try
    			{
    				closeMap(getActiveMap());
    			}
    			catch(ArrayIndexOutOfBoundsException abe)
    			{
    				abe.printStackTrace();
    			}
    		}
        }
        else
        {
        	/*
        	 * If there is 1 map open, and it has no content and hasn't been saved yet close it.
        	 * requested in vue-520 
        	 */
        	if (isActiveViewerOnLeft())
        	{
        		if ((mMapTabsLeft != null) && 
        				mMapTabsLeft.getTabCount() == 1 && 
        				(getActiveMap() != null) && 
        				!getActiveMap().hasContent() && 
        				getActiveMap().getFile() == null)
        		{
        			try
        			{
        				closeMap(getActiveMap());
        			}
        			catch(ArrayIndexOutOfBoundsException abe)
        			{
        				abe.printStackTrace();
        			}
        		}
        	
        	
        	} else 
        	{
        		if ((mMapTabsRight != null) && 
            			mMapTabsRight.getTabCount() == 1 && 
            			(getActiveMap() != null) && 
            			!getActiveMap().hasContent() && 
            			getActiveMap().getFile() == null)
            		closeMap(getActiveMap());
            	
        	}
        }

      /*  
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(file)) {
                if (DEBUG.Enabled) out("displayMap found existing open map " + map + " matching file " + file);
                if (isActiveViewerOnLeft())
                    mMapTabsLeft.setSelectedIndex(i);
                else
                    mMapTabsRight.setSelectedIndex(i);
                return;
            }
        }
        */
        VUE.activateWaitCursor();
        LWMap loadedMap = null;
        boolean alerted = false;
        try {
            loadedMap = OpenAction.loadMap(url);
            if (loadedMap != null)
                VUE.displayMap(loadedMap);
        } catch (Throwable t) {
            Util.printStackTrace(t, "failed to load map[" + url + "]");
            VUE.clearWaitCursor();
            alerted = true;
            VueUtil.alert(VueResources.getString("dialog.failedtoloadmap.message")+" " + url + "  \n"
                          + (t.getCause() == null ? t : t.getCause()),
                          VueResources.getString("dialog.failedtoloadmap.title")+" "+ url);
        } finally {
            VUE.clearWaitCursor();
        }
        if (loadedMap == null && !alerted)
            VueUtil.alert(VueResources.getString("dialog.failedtoloadmap.message")+" " + url + "  \n", VueResources.getString("dialog.failedtoloadmap.title")+" " + url);
        
//        if(getActiveMap()==null){
//        	VueMenuBar.RootMenuBar.saveAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(false);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(false);
//        	Actions.Revert.setEnabled(false);
//        }else{
//        	VueMenuBar.RootMenuBar.saveAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.saveAsAction.setEnabled(true);
//        	VueMenuBar.RootMenuBar.publishMenu.setEnabled(true);
//        	Actions.Revert.setEnabled(true);
//        }
        
    }

    /**
     * Create a new viewer and display the given map in it.
     */
    public static MapViewer displayMap(LWMap pMap) {
        if (VUE.isStartupUnderway() || DEBUG.Enabled) out("displayMap " + pMap);
        diagPush("displayMap");
        if (DEBUG.INIT) out(pMap.toString());
        MapViewer leftViewer = null;
        MapViewer rightViewer = null;
        
        for (int i = 0; i < mMapTabsLeft.getTabCount(); i++) {
            LWMap map = mMapTabsLeft.getMapAt(i);
            if (map == null)
                continue;
            File existingFile = map.getFile();
            if (existingFile != null && existingFile.equals(pMap.getFile())) {
                Util.printStackTrace("warning: found open map with same file: " + map);
                //Log.error("** found open map with same file! " + map);
                // TODO: pop dialog asking to revert existing if there any changes.
                //break;
            }
        }

        

        
        if (leftViewer == null) {
            leftViewer = new MapViewer(pMap, "*LEFT");
            rightViewer = new MapViewer(pMap, "right");

            // Start them both off unfocusable, so we get no
            // focus transfers until we're ready to decide what
            // wants to get the focus.
            leftViewer.setFocusable(false);
            rightViewer.setFocusable(false);

//             if (rightViewer != null && isActiveViewerOnLeft()) {
//                 // so doesn't grab focus till we're ready
//                 // NOTE: grabVueApplicationFocus restore's focusability
//                 // when called directly -- which is why it must
//                 // be called directly to ensure focus grabs
//                 // in right viewers.
//                 rightViewer.setFocusable(false); 
//             }

            if (DEBUG.FOCUS) {
                out("currently active viewer: " + getActiveViewer());
                out("created new left viewer: " + leftViewer);
            }

            mMapTabsLeft.addViewer(leftViewer);
            if (mMapTabsRight != null)
            	mMapTabsRight.addViewer(rightViewer);

        }
        
        if (isActiveViewerOnLeft()) {
            mMapTabsLeft.setSelectedComponent(leftViewer);
        } else if (mMapTabsRight != null){
            mMapTabsRight.setSelectedComponent(rightViewer);
        }

        diagPop();
        
        if (VUE.isApplet())
        {
        	if (LWPathway.isShowingSlideIcons())
        		LWPathway.toggleSlideIcons();
        }        
        return leftViewer;
    }

    
    /**
     * @return the root VUE Frame used for parenting dialogs -- currently always NULL do to java
     * bugs w/dialogs
     */
    
    // WARNING: opening a dialog appears to cause our full-screen
    // window as root parent of everything hack to fail and
    // permit DockWindow's to start going over it -- thus we must
    // use "null" as a parent.  TODO: we'll prob need to do this
    // for all dialogs...  We can still manually center the
    // window if we like...
    // CORRECTION: popping this at ALL seems to do it
    
    public static Component getDialogParent() {
         	
        final Component dialogParent;

        // any dialog parent at all in 1.4.2 causes the full-screen
        // window to go behind the DockWindow's
        
        if (Util.getJavaVersion() >= 1.5f)
            dialogParent = getActiveViewer();
        else
            dialogParent = null;

        if (DEBUG.FOCUS) out("getDialogParent: " + dialogParent);
        
        return dialogParent;

        /*
        // this is not helping for preving dialogs from screwing us up and
        // sending them behind the full-screen window as soon as the dialog pops
        if (true)
            // this will put dialogs at screen bottom when it's off-screen
            return GUI.getFullScreenWindow();
        else
            return null;
        */
    }
    
    public static Frame getDialogParentAsFrame() {
        Frame frame;
        if (getDialogParent() instanceof Frame) {
            frame = (Frame) getDialogParent();
        } else {
            
            // JOptionPane's will take any Component as a parent, but they just do a
            // search up for the root frame as the parent of the JDialog.  But if we
            // return our real root frame here, to be used with a raw JDialog, things
            // behave differently: it allows the dialog to go behind.  Don't know what
            // JOptionPane is causing to happen differently.  E.g., the "are you sure"
            // before quit dialog works fine and doesn't go behind it's parent, but raw
            // JDialogs constructed with an invisible parent CAN go behind... Oh, wait a
            // sec.. what if we use our DockWindow parent...

            // We're in the java 1.5 case here: if it's parented to the application
            // frame, it lets DockWindow's go behind full-screen (yet JOptionPane
            // created dialogs don't).  If it's parented to a hidden frame, the dialog
            // itself can go behind either full-screen or ApplicationFrame.

            // So for now, in 1.5, Dialog's wanting a frame get this special hidden
            // frame, and the FocusManager forces them alwaysOnTop when they're shown.

            frame = GUI.getHiddenDialogParentFrame();
            
            //frame = DockWindow.getHiddenFrame();
            
        }

        if (DEBUG.FOCUS) out("getDialogParentAsFrame: " + frame);
        return frame;
    }

    
    public static VueMenuBar getJMenuBar() {
        return VueMenuBar.RootMenuBar;
        //return (VueMenuBar) ((VueFrame)getRootWindow()).getJMenuBar();
    }
    

    /** Return the main VUE window.  Usually == getRoowWindow, unless we're
     * using a special root window for parenting the tool windows.
     */
    // todo: wanted package private
    public static Window getMainWindow() {
        return VUE.ApplicationFrame;
    }
    
    /** return the root VUE window, mainly for those who'd like it to be their parent */
    public static Window getRootWindow() {
    	if (!VUE.isApplet())
    		return VUE.ApplicationFrame;
    	else
    	{
    		Frame[] frames = JFrame.getFrames();
    		//System.out.println("FRAME LENGTH " + frames.length);
    		JApplet app =  VueApplet.getInstance();
    		Container c = app.getParent();
    		while (!(c instanceof Window))
    		{
    			c = c.getParent();
    		}
    		return (Window)c;
    	}
    	/*
        if (true) {
            return VUE.frame;
        } else {
            if (rootWindow == null) {
                //rootWindow = makeRootFrame();
                rootWindow = makeRootWindow();
            }
            return rootWindow;
        }
        */
    }
    /*
    private static Window makeRootWindow() {
        if (true||DEBUG.INIT) out("making the ROOT WINDOW with parent " + VUE.frame);
        Window w = new ToolWindow("Vue Root", VUE.frame);
        //w.show();
        return w;
    }
    */


    /*
    private static boolean makingRootFrame = false;
    private static Frame makeRootFrame() {
        if (makingRootFrame) {
            new Throwable("RECURSIVE MAKE ROOT WINDOW CALL").printStackTrace();
            return null;
        }
        makingRootFrame = true;
        JFrame f = null;
        try {
            if (DEBUG.INIT) out("creating the ROOT WINDOW");
            f = new JFrame("Vue Root");
            if (VueUtil.isMacPlatform() && useMacLAF) {
                JMenuBar menu = new VueMenuBar();
                f.setJMenuBar(menu);
            }
            f.show();
            //rootFrame = createFrame();
        } finally {
            makingRootFrame = false;
        }
        return f;
    }
    */

    /* This mehtod checks if later version of VUE is available. In case later version
     * is available it prompts user to download it.
     */
    
   public static void checkLatestVersion() {
       Log.info("Checking for latest version of VUE");
        try {
            URL url = new URL(VueResources.getString("vue.release.url"));
            XPathFactory  factory=XPathFactory.newInstance();
            XPath xPath=factory.newXPath();
            if (DEBUG.Enabled) Log.debug("opening " + url);
            InputSource inputSource =  new InputSource(url.openStream());
            XPathExpression  xSession= xPath.compile("/current_release/version/text()");
            String version = xSession.evaluate(inputSource);
            if (DEBUG.Enabled) Log.debug("got current version id [" + version + "]");
            final String currentVersion = VueResources.getString("vue.version").trim();
            final String newVersion = version.trim();
            if (!isHigherVersion(currentVersion, newVersion))
            {
            	//final ShowAgainDialog sad = new ShowAgainDialog(VUE.getApplicationFrame(),"checkForNewVersion2","New Release Available","Remind me later",(String)null);
            	final ShowAgainDialog sad = new ShowAgainDialog(VUE.getApplicationFrame(),"checkForNewVersion2","New Release Available","Get latest version","Close Window");
            	JPanel panel = new JPanel(new GridLayout(1,1));
            	/*JLabel vLabel = new  JLabel("<html>A newer version of VUE is available ("
                                            + newVersion
                                            + ") &nbsp; <font color=\"#20316A\"><u>Get the latest version</u></font></html", JLabel.LEFT);*/
            	JLabel vLabel = new  JLabel("<html>"+VueResources.getString("jlabel.newversion") +" ("
                        + newVersion
                        + ") &nbsp; <font color=\"#20316A\"></html", JLabel.LEFT);
            	if(Util.isMacPlatform()){
            		panel.setPreferredSize(new Dimension(425,25));
            		panel.setSize(new Dimension(425,25));
            		panel.setMinimumSize(new Dimension(425,25));
            	}else{
            		panel.setPreferredSize(new Dimension(425,25));
            	}
            	//vLabel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
            	//panel.setBorder(BorderFactory.createLineBorder(Color.green, 1));
        	    panel.add(vLabel, FlowLayout.LEFT);
        	    sad.setContentPanel(panel);
                
                vLabel.addMouseListener(new javax.swing.event.MouseInputAdapter() {
                    public void mouseClicked(MouseEvent evt) {
                        try {
                            VueUtil.openURL(VueResources.getString("vue.download.url"));
                            sad.setVisible(false);
                            sad.dispose();
                        }catch (Throwable t) { t.printStackTrace();}
                    }
                    
                    
                });
                                                                                                      
                
                VueUtil.centerOnScreen(sad);
                if (sad.showAgain())
                {
                	sad.setVisible(true);
                	
                
                	sad.setVisible(false);
                    sad.dispose();
                
                }
    
                
              }
        }catch(Throwable t) {
            Log.error("Error Checking latest VUE release:", t);
        }
   }
   // check if v1 > v2
   private static boolean isHigherVersion(String v1,String v2) {
     
	   HashMap<String,Integer> priorityMap = new HashMap<String,Integer>();
	   priorityMap.put("alpha", 0);
	   priorityMap.put("beta", 1);
	   priorityMap.put("preview",2); 
	   priorityMap.put("gold", 3);
	   // if current version is same as latest version
		  
       if(v1.equalsIgnoreCase(v2)) {
           return true;
       }
       String[] v1Parts = v1.split("\\D+");
       String[] v2Parts = v2.split("\\D+");
      //check the first number in version
       if(v1Parts.length>0 && v2Parts.length > 0) {
    	   if(Integer.parseInt(v1Parts[0])> Integer.parseInt(v2Parts[0])) return true;
    	   else if(Integer.parseInt(v1Parts[0])< Integer.parseInt(v2Parts[0])) return false;
    	   else {
    		   System.out.println("0\t"+v1Parts[0]+"\t"+v2Parts[0]+"\t"+v1Parts.length+"\t"+v2Parts.length);
    		   if(v1Parts.length>1 && v2Parts.length > 1) {
    	    	   if(Integer.parseInt(v1Parts[1])> Integer.parseInt(v2Parts[1]))  return true;
    	    	   else if(Integer.parseInt(v1Parts[1])<  Integer.parseInt(v2Parts[1]))  return false;
    	    	   else {
    	    		   System.out.println("1\t"+v1Parts[1]+"\t"+v2Parts[1]);
    	    		   String p1 = getPriority(v1, priorityMap);
    	    		   String p2 = getPriority(v2,priorityMap);
    	    		   if(priorityMap.get(p1)> priorityMap.get(p2))  return true;
    	    		   else if(priorityMap.get(p1)<  priorityMap.get(p2))  return false;
    	    		   else {
    	    			   System.out.println("P\t"+p1+"\t"+p2);
    	    			   if(v1Parts.length>2 ) {
    	    				   if(v2Parts.length == 2)   return true;
    	    				   else {
    	    					   System.out.println("2\t"+v1Parts[2]+"\t"+v2Parts[2]);
    	    					   if(Integer.parseInt(v1Parts[2])> Integer.parseInt(v2Parts[2])) return true;
    	    					   else if(Integer.parseInt(v1Parts[2])< Integer.parseInt(v2Parts[2])) return false;
    	    					   else {
    	    						   if(v1Parts.length>3 ) {
    	    							   if(v2Parts.length == 3) return true;
    	    							   else {
    	    								   System.out.println("1\t"+v1Parts[3]+"\t"+v2Parts[3]);
    	    								   if(Integer.parseInt(v1Parts[3])>= Integer.parseInt(v2Parts[3]))  return true; 	    								    
    	    							   }
    	    						   }
    	    					   }
    	    				   } 
    	    			   }
    	    		   }
    	    	   }
    		   }
    	   }
       }
       
       return false;
   }
   private static String getPriority(String version,HashMap<String, Integer> priorityMap) {
	   String priority = "gold";
	   for(String key: priorityMap.keySet()) {
		   if(version.toLowerCase().contains(key)) {
			   return key;
		   }
	   }
	   return priority;
   }
   
    public static String getName() {
        if (NAME == null)
            NAME = VueResources.getString("application.name");
        return NAME;
    }
    
    /** return the root VUE application frame (where the documents are) */
    public static JFrame getApplicationFrame() {
        //if (getRootWindow() instanceof Frame)
        //    return (Frame) getRootWindow();
        //else
        return VUE.ApplicationFrame;
    }


    /** @return a new JWindow, parented to the root VUE window */
    public static JWindow createWindow()
    {
        return new JWindow(getRootWindow());
    }

    /* @return a new ToolWindow, parented to getRootWindow() 
    public static ToolWindow createToolWindow(String title) {
        return createToolWindow(title, null);
    }
    /** @return a new ToolWindow, containing the given component, parented to getRootWindow() 
    public static ToolWindow createToolWindow(String title, JComponent component) {
        return createToolWindow(title, component, false);
    }
    
    /* @return a new ToolWindow, containing the given component, parented to getRootWindow() 
    private static ToolWindow createToolWindow(String title, JComponent component, boolean palette) {
        //Window parent = getRootFrame();
        Window parent = getRootWindow();
        if (DEBUG.INIT) out("creating ToolWindow " + title + " with parent " + parent);

        final ToolWindow w;
        if (palette) {
            w = new ToolWindow(title, parent, false);
        } else {
            w = new ToolWindow(title, parent, true);
            if (component != null)
                w.addTool(component);
        }
        /*
          // ToolWindows not set yet...
        if (VueUtil.isMacPlatform() && useMacLAF && w instanceof JFrame)
            ((JFrame)w).setJMenuBar(new VUE.VueMenuBar());
        *
        return w;
    }

    /** @return a new ToolWindow styled as a ToolPalette 
    public static ToolWindow createToolPalette(String title) {
        return createToolWindow(title, null, true);
    }
    */

    /** call the given runnable after all pending AWT events are completed */
    public static void invokeAfterAWT(Runnable runnable) {
        java.awt.EventQueue.invokeLater(runnable);
    }

    /** @return true if in any full screen mode */
    public static boolean inFullScreen() {
        return FullScreen.inFullScreen();
    }
    
    /** @return true if in working full screen mode (menu still at top, DockWindow's can be seen at the same time) */
    public static boolean inWorkingFullScreen() {
        return FullScreen.inWorkingFullScreen();
    }
    
    /** @return true if in total full screen mode (no menu, and on mac, if any window even tries to display, we hang...) */
    public static boolean inNativeFullScreen() {
        return FullScreen.inNativeFullScreen();
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }
    
    public static void toggleFullScreen(boolean goNative) {
    	toggleFullScreen(goNative, false);
    }
    
    public static void toggleFullScreen(boolean goNative,boolean showFloatingToolbar)
    {
    	FullScreen.toggleFullScreen(goNative);
    	if (showFloatingToolbar)
    	{	
            DockWindow.flickerAnchorDock();
    		floatingZoomDock.setVisible(inWorkingFullScreen());
    	}
    }
    
    static void installExampleNodes(LWMap map) {
        map.setFillColor(new Color(255,255,220));

        /*
        map.addLWC(new LWNode("Oval", 0)).setFillColor(Color.red);
        map.addLWC(new LWNode("Circle", 1)).setFillColor(Color.green);
        map.addLWC(new LWNode("Square", 2)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Rectangle", 3)).setFillColor(Color.blue);
        map.addLWC(new LWNode("Rounded Rectangle", 4)).setFillColor(Color.yellow);
        
        LWNode triangle = new LWNode("Triangle", 5);
        triangle.setAutoSized(false);
        triangle.setSize(60,60);
        triangle.setFillColor(Color.orange);
        map.addLWC(triangle);
        //map.addLWC(new LWNode("Triangle", 5)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Diamond", 6)).setFillColor(Color.yellow);
        */
        
        map.addNode(new LWNode("One"));
        map.addNode(new LWNode("Two"));
        map.addNode(new LWNode("Three"));
        map.addNode(new LWNode("Four"));
        map.addNode(new LWNode("WWWWWWWWWWWWWWWWWWWW"));
        map.addNode(new LWNode("iiiiiiiiiiiiiiiiiiii"));
        
        map.addNode(NodeModeTool.createTextNode("jumping"));
        
        // Experiment in internal actions -- only works
        // partially here because they're all auto sized
        // based on text, and since haven't been painted yet,
        // and so don't really know their size.
        // Addendum: with new TextBox, above no longer true.
        LWSelection s = new LWSelection();
        s.setTo(map.getChildIterator());
        Actions.MakeColumn.act(s);
        s.clear(); // clear isSelected bits
    }
    
    public static void installExampleMap(LWMap map) {

        /*
         * create some test nodes & links
         */

        //map.addLWC(new LWImage(new MapResource("/Users/sfraize/Desktop/Test Image.jpg"))).setLocation(350, 90);
        
        LWNode n1 = new LWNode("Google", URLResource.create("http://www.google.com/"));
        LWNode n2 = new LWNode("Program\nFiles", URLResource.create("C:\\Program Files"));
        LWNode n3 = new LWNode("readme.txt", URLResource.create("readme.txt"));
        LWNode n4 = new LWNode("Slash", URLResource.create("file:///"));
        LWNode n5 = new LWNode("Program\nFiles 2", URLResource.create("\\Program Files"));

        n1.setLocation(100, 30);
        n2.setLocation(100, 100);
        n3.setLocation(50, 180);
        n4.setLocation(200, 180);
        n5.setLocation(150, 200);
        n4.setNotes("I am a note.");
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addNode(n5);
        LWLink k1 = new LWLink(n1, n2);
        LWLink k2 = new LWLink(n2, n3);
        LWLink k3 = new LWLink(n2, n4);
        k1.setLabel("Link label");
        k1.setNotes("I am link note");
        k3.setControlCount(1);
        k2.setControlCount(2);
        map.addLink(k1);
        map.addLink(k2);
        map.addLink(k3);

//         LWSlide slide = LWSlide.create();
//         slide.setLocation(300,100);
//         map.addLWC(slide);

        // create test pathways
        if (false) {
            // FYI: I dno't think PathwayTableModel will
            // detect this creation, so can't use this
            // for full testing (e.g., note setting, undo, etc)
            LWPathway p = new LWPathway("Test Pathway");
            p.add(n1);
            p.add(n2);
            p.add(n3);
            map.addPathway(p);
        }
        map.markAsSaved();
        
        /*else if(map.getLabel().equals("Test Nodes")){
        }/*else if(map.getLabel().equals("Test Nodes")){
            LWPathway p2 = new LWPathway("Pathway 2");
         
            p2.setComment("A comment.");
            LinkedList anotherList = new LinkedList();
            anotherList.add(n3);
            anotherList.add(n4);
            anotherList.add(n2);
            anotherList.add(k2);
            anotherList.add(k3);
            p2.setElementList(anotherList);
            map.addPathway(p2);
         
        map.markAsSaved();
         
        }*/
    }

    static protected void out(Object o) {
        //System.out.println(o == null ? "null" : o.toString());
        Log.info(o == null ? "null" : o.toString());
    }
	public static Color getPresentationBackground() {
		final Color defaultColor= new Color(32,32,32);
		if (VUE.getActiveMap() != null)
			return VUE.getActiveMap().getPresentationBackgroundValue();
		else
			return defaultColor;
	}	

	public static DRBrowser getDRBrowser() {
		return contentPanel.getDRBrowser();
	}
}
