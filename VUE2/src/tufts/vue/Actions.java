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

import java.util.Iterator;

import tufts.Util;
import tufts.vue.NodeTool.NodeModeTool;
import static tufts.vue.LWComponent.Flag;
import java.util.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import edu.tufts.vue.preferences.ui.PreferencesDialog;
import tufts.vue.action.SaveAction;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.WindowDisplayAction;

/**
 * VUE actions, all subclassed from VueAction, of generally these types:
 *      - application actions (e.g., new map)
 *      - actions that work on the active viewer (e.g., zoom)
 *      - actions that work on the active map (e.g., undo, select all)
 *      - actions that work on the current selection (e.g., font size, delete)
 *        (These are LWCAction's)
 *
 * @author Scott Fraize
 * @version March 2004
 */

public class Actions implements VueConstants
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Actions.class);
    
    public static final int COMMAND = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
    public static final int LEFT_OF_SPACE = VueUtil.isMacPlatform() ? Event.META_MASK : Event.ALT_MASK;
    public static final int CTRL = Event.CTRL_MASK;
    public static final int SHIFT = Event.SHIFT_MASK;
    public static final int ALT = Event.ALT_MASK;
    
    public static final int CTRL_ALT = VueUtil.isMacPlatform() ? CTRL+COMMAND : CTRL+ALT;
    
    static final private KeyStroke keyStroke(int vk, int mod) {
        return KeyStroke.getKeyStroke(vk, mod);
    }
    static final private KeyStroke keyStroke(int vk) {
        return keyStroke(vk, 0);
    }
    
    
    //--------------------------------------------------
    // PDF Export Notes Actions
    //--------------------------------------------------
    
    public static final VueAction NodeNotesOutline =
    	new VueAction("Node notes outline") {
		public void act() 
		{
			File pdfFile = getFileForActiveMap("Node_Outline");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createNodeOutline(pdfFile);
		}
    };
    
    private static final File getFileForPresentation(String type)
    {
    	if (VUE.getActivePathway() == null || VUE.getActivePathway().getEntries().isEmpty())
    	{
			VueUtil.alert(null,VueResources.getString("presentationNotes.invalidPresentation.message"), VueResources.getString("presentationNotes.invalidPathway.title"));
			return null;
    	}
		VueFileChooser chooser = new VueFileChooser();
		File pdfFileName = null;
		chooser.setDialogTitle("Save PDF as");
		
		String baseName = VUE.getActivePathway().getLabel();
		if (baseName.indexOf(".") > 0)
			baseName = VUE.getActiveMap().getLabel().substring(0, baseName.lastIndexOf("."));
		baseName = baseName.replaceAll("\\*","")+"_"+type;
		
		chooser.setSelectedFile(new File(baseName));
        int option = chooser.showSaveDialog(tufts.vue.VUE.getDialogParent());
        if (option == VueFileChooser.APPROVE_OPTION) 
        {
            pdfFileName = chooser.getSelectedFile();

            if (pdfFileName == null) 
            	return null;

            if(!pdfFileName.getName().endsWith(".pdf")) 
            	pdfFileName = new File(pdfFileName.getAbsoluteFile()+".pdf");                	
            
            if (pdfFileName.exists()) {
                int n = JOptionPane.showConfirmDialog(null, VueResources.getString("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                        VueResources.getString("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                      
            }           
            
            return pdfFileName;
        }
        else
        	return null;
		
    }
    private static final File getFileForActiveMap()
    {
    	return getFileForActiveMap(null);
    }
    private static final File getFileForActiveMap(String type)
    {
    	if (VUE.getActiveMap() == null)
    	{
			VueUtil.alert(null,"There is no active map, please open a valid map.", "Invalid Map");
			return null;
    	}
		VueFileChooser chooser = new VueFileChooser();
		File pdfFileName = null;
		
		String baseName = VUE.getActiveMap().getLabel();
		if (baseName.indexOf(".") > 0)
			baseName = VUE.getActiveMap().getLabel().substring(0, baseName.lastIndexOf("."));
		if (type != null)
			baseName = baseName.replaceAll("\\*","") +"_"+type;
		else
			baseName = baseName.replaceAll("\\*","");
		
		chooser.setSelectedFile(new File(baseName));
		chooser.setDialogTitle("Save PDF as");
        int option = chooser.showSaveDialog(tufts.vue.VUE.getDialogParent());
        if (option == VueFileChooser.APPROVE_OPTION) 
        {
            pdfFileName = chooser.getSelectedFile();

            if (pdfFileName == null) 
            	return null;

            if(!pdfFileName.getName().endsWith(".pdf")) 
            	pdfFileName = new File(pdfFileName.getAbsoluteFile()+".pdf");                	
            
            if (pdfFileName.exists()) {
                int n = JOptionPane.showConfirmDialog(null, VueResources.getString("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                        VueResources.getString("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                      
            }           
            
            return pdfFileName;
        }
        else
        	return null;
		
    }
              
    
    public static final VueAction SpeakerNotes1 =
    	new VueAction("Slides - speaker notes (1 per page)") {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createSpeakerNotes1PerPage(pdfFile);
		}
    };
    public static final VueAction SpeakerNotes4 =
    	new VueAction("Slides - speaker notes (up to 4 per page)") {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createSpeakerNotes4PerPage(pdfFile);
		}
    };
    public static final VueAction NodeNotes4 =
    	new VueAction("Nodes and notes (up to 4 per page)") {
		public void act() 
		{
			File pdfFile = getFileForActiveMap("Node_Notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createNodeNotes4PerPage(pdfFile);
		}
    };
    public static final VueAction SpeakerNotesOutline =
    	new VueAction("Speaker notes only (outline)") {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createOutline(pdfFile);
		}
    };
    public static final VueAction Slides8PerPage =
    	new VueAction("Slides (8 per page)") {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Slides");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createPresentationNotes8PerPage(pdfFile);
		}
    };
    

    public static final VueAction AudienceNotes =
    	new VueAction("Audience Notes") {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Audience notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createAudienceNotes(pdfFile);
		}
    };
    

    public static final VueAction FullPageSlideNotes =
    	new VueAction("Slides (1 per page)") {
		public void act() 
		{			
			File pdfFile = getFileForPresentation("Slides");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createPresentationSlidesDeck(pdfFile);
		}
    };
    
    public static final VueAction MapAsPDF =
    	new VueAction("Map") {
		public void act() 
		{			
			File pdfFile = getFileForActiveMap();
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createMapAsPDF(pdfFile);
		}
    };
   
    //------------------------------------------------------------
    // Preference Action
    //------------------------------------------------------------
    public static class PreferenceAction extends AbstractAction
    {
    	
    	public PreferenceAction()
    	{
    		   putValue(NAME, "Preferences");
               putValue(SHORT_DESCRIPTION, "Preferences");            
               putValue(ACCELERATOR_KEY, keyStroke(KeyEvent.VK_COMMA, COMMAND));
    		
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	PreferencesDialog dialog = new PreferencesDialog(null, "Preferences",
				      edu.tufts.vue.preferences.PreferencesManager.class, true, null, false);
			dialog.setVisible(true);
        }
    };
    //-------------------------------------------------------
    // Selection actions
    //-------------------------------------------------------
    
    public static final Action Preferences = new PreferenceAction(); 
    	
    public static final Action SelectAll =
    new VueAction("Select All", keyStroke(KeyEvent.VK_A, COMMAND)) {
        public void act() {
            VUE.getSelection().setTo(VUE.getActiveViewer().getFocal().getAllDescendents());
            //VUE.getSelection().setTo(VUE.getActiveMap().getAllDescendentsIterator());
            
            //VUE.getSelection().setTo(VUE.getActiveMap().getAllDescendentsGroupOpaque()); // TODO: handle when moving selection: don't move objects if parent is moving
            //VUE.getSelection().setTo(VUE.getActiveMap().getAllDescendents(LWComponent.ChildKind.VISIBLE));
            
            // SelectAll now ONLY does the top level of the map -- maybe direct selection tool could do deep selection
            //VUE.getSelection().setTo(VUE.getActiveMap().getChildList());
        }
    };
    
    public static final Action SelectAllLinks =
        new VueAction("Select Links") {
            public void act() {
                VUE.getSelection().setTo(VUE.getActiveViewer().getFocal().getAllLinks());            
            }
        };

     public static final Action SelectAllNodes =
        new VueAction("Select Nodes") {
            public void act() {
            	VUE.getSelection().setTo(VUE.getActiveViewer().getMap().getNodeIterator());            
            }
        };
                
    public static final Action DeselectAll =
    new LWCAction("Deselect All", keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        public void act() {
            VUE.getSelection().clear();
        }
    };

    public static final Action Reselect =
        new VueAction("Reselect", keyStroke(KeyEvent.VK_R, COMMAND)) {
            public void act() {
                VUE.getSelection().reselect();
            }
        };
    
    
    public static final Action AddPathwayItem =
    new LWCAction(VueResources.getString("actions.addPathwayItem.label")) {
        public void act(Iterator i) {
        	LWPathway pathway = VUE.getActivePathway();
        	if (!pathway.isOpen())
        		pathway.setOpen(true);
            VUE.getActivePathway().add(i);
            GUI.makeVisibleOnScreen(VUE.getActiveViewer(), PathwayPanel.class);
            
        }
        boolean enabledFor(LWSelection s) {
            // items can be added to pathway as many times as you want
            return VUE.getActivePathway() != null && s.size() > 0;
        }
    };
    
    public static final Action RemovePathwayItem =
    new LWCAction(VueResources.getString("actions.removePathwayItem.label")) {
        public void act(Iterator i) {
            VUE.getActivePathway().remove(i);
        }
        boolean enabledFor(LWSelection s) {
            LWPathway p = VUE.getActivePathway();
            return p != null && s.size() > 0 && (s.size() > 1 || p.contains(s.first()));
        }
    };
    
    public static final Action AddResource =
        new VueAction("Add Resource") {
            public void act() {
            	
            	DataSourceViewer.getAddLibraryAction().actionPerformed(null);
            	GUI.makeVisibleOnScreen(this, VUE.getContentDock().getClass());
            }                       
        };
        
    public static final Action UpdateResource =
    new VueAction("Update Resource") {
        public void act() {
        	
        	DataSourceViewer.getUpdateLibraryAction().actionPerformed(null);
        	GUI.makeVisibleOnScreen(this, VUE.getContentDock().getClass());
        }
    };

    public static final Action SearchFilterAction =
        new VueAction("Search") {
    		
            public void act() {
                if(tufts.vue.ui.InspectorPane.META_VERSION == tufts.vue.ui.InspectorPane.OLD)
                {    
            	  VUE.getMapInfoDock().setVisible(true);
            	  VUE.getMapInspectorPanel().activateFilterTab();
                }
                else
                {
                  tufts.vue.gui.DockWindow searchWindow = edu.tufts.vue.metadata.ui.MetadataSearchGUI.getDockWindow();
                  searchWindow.setVisible(true);
                  edu.tufts.vue.metadata.ui.MetadataSearchGUI.afterDockVisible();
                }
            }
        };

    
    //-------------------------------------------------------
    // Alternative View actions
    //-------------------------------------------------------
    
    /**Addition by Daisuke Fujiwara*/
    
    public static final Action HierarchyView =
    new LWCAction("Hierarchy View") {
        public void act(LWNode n) {
            LWNode rootNode = n;
            String name = new String(rootNode.getLabel() + "'s Hierarchy View");
            String description = new String("Hierarchy view model of " + rootNode.getLabel());
            
            LWHierarchyMap hierarchyMap = new LWHierarchyMap(name);
            
            tufts.oki.hierarchy.HierarchyViewHierarchyModel model =
            new tufts.oki.hierarchy.HierarchyViewHierarchyModel(rootNode, hierarchyMap, name, description);
            
            hierarchyMap.setHierarchyModel(model);
            hierarchyMap.addAllComponents();
            VUE.displayMap((LWMap)hierarchyMap);
        }
        
        boolean enabledFor(LWSelection s) {
            return s.size() == 1 && s.first() instanceof LWNode;
        }
    };
    
    /**End of Addition by Daisuke Fujiwara*/
    
    public static final Action PreviewInViewer =
        new LWCAction("In Viewer") {
            public void act(Iterator i) {
                GUI.makeVisibleOnScreen(VUE.getActiveViewer(), tufts.vue.ui.SlideViewer.class);                
            }
            boolean enabledFor(LWSelection s) {
                return s.size() == 1 && s.first() instanceof LWSlide;
            }
        };
    
     public static final Action MasterSlide = new VueAction("Master Slide")
     {
    	public void act()
    	{
            if (VUE.getSlideDock() != null) {
    		VUE.getSlideDock().setVisible(true);
    		VUE.getSlideViewer().showMasterSlideMode();
            }
    	}
     };
     public static final Action PreviewOnMap =
            new LWCAction("Slide Edit") {
                public void act(LWComponent c) {
                	
                    final MapViewer viewer = VUE.getActiveViewer();

                    if (viewer.getFocal() == c) {
                        viewer.popFocal(true, true);
                        return;
                        //return false;
                    }

                    final Rectangle2D viewerBounds = viewer.getVisibleMapBounds();
                    final Rectangle2D mapBounds = c.getMapBounds();
                    final Rectangle2D overlap = viewerBounds.createIntersection(mapBounds);
                    final double overlapArea = overlap.getWidth() * overlap.getHeight();
                    //final double viewerArea = viewerBounds.getWidth() * viewerBounds.getHeight();
                    final double nodeArea = mapBounds.getWidth() * mapBounds.getHeight();
                    final boolean clipped = overlapArea < nodeArea;
                    
                    final double overlapWidth = mapBounds.getWidth() / viewerBounds.getWidth();
                    final double overlapHeight = mapBounds.getHeight() / viewerBounds.getHeight();

                    final boolean focusNode; // otherwise, re-focus map      
                    
                    if (clipped) {
                        focusNode = true;
                    } else if (overlapWidth > 0.8 || overlapHeight > 0.8) {
                        focusNode = false;
                    } else
                        focusNode = true;
                    
                    boolean AnimateOnZoom=false;
                    
                    if (focusNode) {
                        viewer.clearRollover();
                        
                        if (true) {
                            // loadfocal animate only currently works when popping (to a parent focal)
                            //viewer.loadFocal(this, true, AnimateOnZoom);
                            ZoomTool.setZoomFitRegion(viewer,
                                                      mapBounds,
                                                      0,
                                                      AnimateOnZoom);
                            viewer.loadFocal(c);
                        } else {
                            ZoomTool.setZoomFitRegion(viewer,
                                                      mapBounds,
                                                      -LWPathway.PathBorderStrokeWidth / 2,
                                                      AnimateOnZoom);
                        }
                    } else {
                        // just re-fit to the map
                        viewer.fitToFocal(AnimateOnZoom);
                    }
                    
                	
                	
                }
                boolean enabledFor(LWComponent s) {
                    return s instanceof LWSlide;
                }
            };
                
    
            public static final VueAction LaunchPresentation = new VueAction("Preview")
            {
            	public void act()
            	{
            		final PresentationTool presTool = PresentationTool.getTool();
              
            		GUI.invokeAfterAWT(new Runnable() { public void run() {
            			VUE.toggleFullScreen(true);
            		}});
            		GUI.invokeAfterAWT(new Runnable() { public void run() {
            			//VueToolbarController.getController().setSelectedTool(presTool);
            			VUE.setActive(VueTool.class, this, presTool);
            		}});
            		GUI.invokeAfterAWT(new Runnable() { public void run() {
            			presTool.startPresentation();
            		}});
            	}
            };
            
            public static final Action DeleteSlide = new VueAction("Delete")
            {
            	public void act()
            	{
                	//delete the current entry
//          		 This is a heuristic to try and best guess what the user might want to
                  // actually remove.  If nothing in selection, and we have a current pathway
                  // index/element, remove that current pathway element.  If one item in
                  // selection, also remove whatever the current element is (which ideally is
                  // usually also the selection, but if it's different, we want to prioritize
                  // the current element hilighted in the PathwayTable).  If there's MORE than
                  // one item in selection, do a removeAll of everything in the selection.
                  // This removes ALL instances of everything in selection, so that, for
                  // instance, a SelectAll followed by pathway delete is guaranteed to empty
                  // the pathway entirely.

            	  LWPathway pathway = VUE.getActivePathway();
            	  
                  if (pathway.getCurrentIndex() >= 0 && VUE.ModelSelection.size() < 2) {
                      pathway.remove(pathway.getCurrentIndex());
                  } else {
                      pathway.remove(VUE.getSelection().iterator());
                  }
        

            	}
            };            
    //-----------------------------------------------------------------------------
    // Link actions
    //-----------------------------------------------------------------------------
    
    public static final LWCAction LinkMakeStraight =
        //new LWCAction("Straight", VueResources.getIcon("linkTool.line.raw")) {
        new LWCAction("Straight", VueResources.getIcon("link.style.straight")) {
            void init() { putValue("property.value", new Integer(0)); } // for use in a MenuButton
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 0 : true;
            }
            public void act(LWLink c) { c.setControlCount(0); }
        };
    public static final LWCAction LinkMakeQuadCurved =
        //new LWCAction("Curved", VueResources.getIcon("linkTool.curve1.raw")) {
        new LWCAction("Curved", VueResources.getIcon("link.style.curved")) {
            void init() { putValue("property.value", new Integer(1)); }
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 1 : true;
            }
            public void act(LWLink c) { c.setControlCount(1); }
        };
    public static final LWCAction LinkMakeCubicCurved =
        //new LWCAction("S-Curved", VueResources.getIcon("linkTool.curve2.raw")) {
        new LWCAction("S-Curved", VueResources.getIcon("link.style.s-curved")) {
            void init() { putValue("property.value", new Integer(2)); }
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 2 : true;
            }
            public void act(LWLink c) { c.setControlCount(2); }
        };
    public static final Action LinkArrows =
        new LWCAction("Arrows", keyStroke(KeyEvent.VK_L, COMMAND)/*, VueResources.getIcon("outlineIcon.link")*/) {
            boolean enabledFor(LWSelection s) { return s.containsType(LWLink.class); }
            public void act(LWLink c) { c.rotateArrowState(); }
        };
    
    
    /** Helper for menu creation.  Null's indicate good places
     * for menu separators. */
    public static final Action[] LINK_MENU_ACTIONS = {
        LinkMakeStraight,
        LinkMakeQuadCurved,
        LinkMakeCubicCurved,
        LinkArrows
    };
    
    //-----------------------------------------------------------------------------
    // Node actions
    //-----------------------------------------------------------------------------
    
    public static final LWCAction NodeMakeAutoSized =
    new LWCAction("Set Auto-Sized") {
        boolean enabledFor(LWSelection s) {
            if (!s.containsType(LWNode.class))
                return false;
            return s.size() == 1 ? ((LWNode)s.first()).isAutoSized() == false : true;
        }
        public void act(LWNode c) {
            c.setAutoSized(true);
        }
    };
    
    /** Helper for menu creation.  Null's indicate good places
     * for menu separators. */
    public static final Action[] NODE_MENU_ACTIONS = {
        NodeMakeAutoSized
    };
    
    //-------------------------------------------------------
    // Edit actions: Duplicate, Cut, Copy & Paste
    // These actions all make use of the statics
    // below.
    //-------------------------------------------------------

    
    private static final List<LWComponent> ScratchBuffer = new ArrayList();
    //private static LWContainer ScratchMap;
    private static LWComponent StyleBuffer; // this holds the style copied by "Copy Style"
    
    private static final LWComponent.CopyContext CopyContext = new LWComponent.CopyContext(new LWComponent.LinkPatcher(), true);
    private static final List<LWComponent> DupeList = new ArrayList(); // cache for dupe'd items
    
    private static final int CopyOffset = 10;
    
    public static Collection<LWComponent> duplicatePreservingLinks(Iterable<LWComponent> iterable) {
        CopyContext.reset();
        DupeList.clear();

        // TODO: preserve z-order layering of duplicated elements.
        // probably merge LinkPatcher into CopyContext, and
        // while at it change dupe action to add all the
        // children to the new parent with a single addChildren event.
        
        for (LWComponent c : iterable) {

            // TODO: all users of this method may not be depending on items being
            // selected!  CopyContext should sort out duped items with a HashSet, only
            // invoking duplicate on items in set who don't have an ancestor in the set.
            // Can we use a TreeSet to track & preserve z-order?  When duping a whole
            // node w/children, z-order is already preserved -- it's only the order
            // amongst the top-level selected items we need to preserve (e.g., don't
            // just rely on the random selection order, unless we want to change the
            // selection to a SortedSet (TreeSet)).
            
            if (c.isAncestorSelected() || !canEdit(c)) {
                
                // Duplicate is hierarchical action: don't dupe if parent is going to do
                // it for us.  Note that when we call this on paste, parent will always
                // be null as these are orphans in the cut buffer, and thus
                // isAncestorSelected will always be false, but we culled them here when
                // we put them in, so we're all set.
                
                continue;
            }
            LWComponent copy = c.duplicate(CopyContext);
            if (copy != null)
                DupeList.add(copy);
            //System.out.println("duplicated " + copy);
        }
        CopyContext.complete();
        CopyContext.reset();
        return DupeList;
    }

    /** @return true if we can cut/copy/delete/duplicate this selection */
    private static boolean canEdit(LWSelection s) {
        if (s.size() == 1) {
            return canEdit(s.first());
        } else
            return s.size() > 1;
                               
        //return s.size() > 0 && !(s.only() instanceof LWSlide);
    }

    private static boolean canEdit(LWComponent c) {
        if (c.hasFlag(Flag.LOCKED))
            return false;
        else if (c instanceof LWSlide && !DEBUG.META)
            return false;
        else if (c.getParent() instanceof LWPathway) // old-style slides not map-owned
            return false;
        else
            return true;
    }

    public static final LWCAction Duplicate =
    new LWCAction("Duplicate", keyStroke(KeyEvent.VK_D, COMMAND)) {
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        // hierarchicalAction set to true: if parent being duplicated, don't duplicate
        // any selected children, creating extra siblings.
        boolean hierarchicalAction() { return true; }

        // TODO: preserve layering order of components -- don't
        // just leave in the arbitrary selection order.
        void act(Iterator i) {
            DupeList.clear();
            CopyContext.reset();
            super.act(i);
            CopyContext.complete();
            VUE.getSelection().setTo(DupeList);
            if (DupeList.size() == 1 && DupeList.get(0).supportsUserLabel())
                VUE.getActiveViewer().activateLabelEdit(DupeList.get(0));
            DupeList.clear();
        }
        
        void act(LWComponent c) {
            if (canEdit(c)) {
                LWComponent copy = c.duplicate(CopyContext);
                if (copy != null) {
                    DupeList.add(copy);
                    copy.translate(CopyOffset, CopyOffset);
                    c.getParent().pasteChild(copy);
                }
            }
        }
        
    };

    
    public static final Action Cut =
    new LWCAction("Cut", keyStroke(KeyEvent.VK_X, COMMAND)) {
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        void act(LWSelection selection) {
            Copy.act(selection);
            Delete.act(selection);
            //ScratchMap = null;  // okay to paste back in same location
        }
    };
    
    public static final LWCAction Copy =
    new LWCAction("Copy", keyStroke(KeyEvent.VK_C, COMMAND)) {
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        void act(LWSelection selection) {
            ScratchBuffer.clear();
            ScratchBuffer.addAll(duplicatePreservingLinks(selection));
            //ScratchMap = VUE.getActiveMap();
            // paste differs from duplicate in that the new parent is
            // always the top level map -- not the old parent -- so a node
            // that was a child has to have it's scale set back to 1.0
            // todo: do this automatically in removeChild?
            for (LWComponent c : ScratchBuffer) {
                if (c.getScale() != 1f)
                    c.setScale(1f);
            }

            // Enable if want to use system clipboard.  FYI: the clip board manager
            // will immediately grab all the data available from the transferrable
            // to cache in the system.
            //Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            //clipboard.setContents(VUE.getActiveViewer().getTransferableSelection(), null);
            
        }
    };

    public static final LWCAction Delete =
        // "/tufts/vue/images/delete.png" looks greate (from jide), but too unlike others
        new LWCAction("Delete", keyStroke(KeyEvent.VK_DELETE), ":general/Delete") {
            // We could use BACK_SPACE instead of DELETE because that key is bigger, and
            // on the mac it's actually LABELED "delete", even tho it sends BACK_SPACE.
            // BUT, if we use backspace, trying to use it in a text field in, say
            // the object inspector panel causes it to delete the selection instead of
            // backing up a char...
            // The MapViewer special-case handles both anyway as a backup.
                      
        // hierarchicalAction is true: if parent being deleted,
        // let it handle deleting the children (ignore any
        // children in selection who's parent is also in selection)
        boolean hierarchicalAction() { return true; }
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        
        void act(Iterator i) {
            super.act(i);
            
            // LWSelection does NOT listen for events among what's selected (an
            // optimization & we don't want the selection updating iself and issuing
            // selection change events AS a delete takes place for each component as
            // it's deleted) -- it only needs to know about deletions, so they're
            // handled special case.  Here, all we need to do is clear the selection as
            // we know everything in it has just been deleted.
            
            VUE.getSelection().clear();
        }
        void act(LWComponent c) {
            LWContainer parent = c.getParent();
            
            if (parent == null) {
                info("skipping: null parent (already deleted): " + c);
            } else if (c.isDeleted()) {
                info("skipping (already deleted): " + c);
            } else if (parent.isDeleted()) { // after prior check, this case should be impossible now
                info("skipping (parent already deleted): " + c); // parent will call deleteChildPermanently
            } else if (parent.isSelected()) { // if parent selected, it will delete it's children
                info("skipping - parent selected & will be deleting: " + c);
            } else if (c.hasFlag(Flag.LOCKED)) {
                info("not permitted: " + c);
            } else if (!canEdit(c)) {
                info("cannot edit: " + c);
            } else {
                parent.deleteChildPermanently(c);
            }
        }
    };
    

    public static final LWCAction CopyStyle =
    new LWCAction("Copy Style", keyStroke(KeyEvent.VK_C, CTRL+LEFT_OF_SPACE)) {
        boolean enabledFor(LWSelection s) { return s.size() == 1; }
        void act(LWComponent c) {
            try {
                StyleBuffer = c.getClass().newInstance();
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            StyleBuffer.setLabel("styleHolder");
            StyleBuffer.copyStyle(c);
        }
    };
    
    public static final LWCAction PasteStyle =
    new LWCAction("Apply Style", keyStroke(KeyEvent.VK_V, CTRL+LEFT_OF_SPACE)) {
        boolean enabledFor(LWSelection s) { return s.size() > 0 && StyleBuffer != null; }
        void act(LWComponent c) {
            c.copyStyle(StyleBuffer);
        }
    };
    
    public static final VueAction Paste =
    new VueAction("Paste", keyStroke(KeyEvent.VK_V, COMMAND)) {
        //public boolean isEnabled() //would need to listen for scratch buffer fills
        
        private Point2D.Float lastMouseLocation;
        private Point2D.Float lastPasteLocation;
        public void act() {
            
            final MapViewer viewer = VUE.getActiveViewer();
            final Collection pasted = duplicatePreservingLinks(ScratchBuffer);
            final Point2D.Float mouseLocation = viewer.getLastFocalMousePoint();
            final Point2D.Float pasteLocation;

            if (mouseLocation.equals(lastMouseLocation) && lastPasteLocation != null) {
                pasteLocation = lastPasteLocation;
                // translate both current and last paste location:
                pasteLocation.x += CopyOffset;
                pasteLocation.y += CopyOffset;
            } else {
                pasteLocation = mouseLocation;
                lastPasteLocation = pasteLocation;
            }
            
            MapDropTarget.setCenterAt(pasted, pasteLocation); // only works on un-parented nodes
            viewer.getFocal().pasteChildren(pasted);
            viewer.getSelection().setTo(pasted);
            lastMouseLocation = mouseLocation;
        }

        
        // stub code for if we want to start using the system clipboard for cut/paste
        void act_system() {
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            VUE.getActiveViewer().getMapDropTarget().processTransferable(clipboard.getContents(this), null);
        }
        
    };
    
    //-----------------------
    // Context Menu Actions
    //-----------------------
    public static final Action KeywordAction = new KeywordActionClass(VueResources.getString("mapViewer.componentMenu.keywords.label"));
    public static final Action ContextKeywordAction = new KeywordActionClass("Add Keywords");
    
    public static class KeywordActionClass extends VueAction
    {
    	
    	public KeywordActionClass(String s)
    	{
    		super(s);
    	}
    	
        public void act() {
        	VUE.getInfoDock().setVisible(true);
        	VUE.getInspectorPane().showKeywordView();
        	GUI.makeVisibleOnScreen(this, tufts.vue.ui.InspectorPane.class);        	
        	VUE.getInfoDock().setRolledUp(false,true);
        }
        //public void act() { VUE.ObjectInspector.setVisible(true); }
    };
    /*
    public static final LWCAction AddImageAction = new LWCAction(VueResources.getString("mapViewer.componentMenu.addImage.label")) {
        public void act(LWComponent c) 
        {
        	VueFileChooser chooser = new VueFileChooser();
    		File fileName = null;

                // TODO: this is broken -- it should do almost exactly the same thing
                // as AddFileAction -- the only difference would when adding a new item
                // entirely, create an image instead of a node (and perhaps use
                // an image selecting file filter)
    		
            int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
            if (option == VueFileChooser.APPROVE_OPTION) 
            {
                fileName = chooser.getSelectedFile();

                if (fileName == null) 
                	return;

                //if(!pdfFileName.getName().endsWith(".pdf")) 
                //	pdfFileName = new File(pdfFileName.getAbsoluteFile()+".pdf");                	
                
                //if (pdfFileName.exists()) {
                 //   int n = JOptionPane.showConfirmDialog(null, VueResources.getString("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                 //           VueResources.getString("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                 //                         
                //}
                //LWNode node = NodeModeTool.createNewNode();
                final LWImage image = new LWImage();
                image.setResource(fileName);
                //node.addChild(image);
                c.addChild(image);
            }
        	
        }
    };
    */
    public static final LWCAction ImageToNaturalSize = new LWCAction("Make Natural Size") {
            public void act(LWImage c) {
                c.setToNaturalSize();
            }
        };
    

    
    public static final LWCAction AddFileAction = new LWCAction(VueResources.getString("mapViewer.componentMenu.addFile.label")) {
        public void act(LWComponent c) 
        {
        	VueFileChooser chooser = null;
        	if (VueUtil.isCurrentDirectoryPathSet()) 
    		{
    			/*
    			 * Despite Quaqua fixes in 3.9 you can still only set the 
    			 * current directory if you set it in the constructor, 
    			 * setCurrentDirectory fails to do anything but cause the
    			 * top bar and the panels to be out of sync.... -MK 10/29
    			 */
        		chooser = new VueFileChooser(new File(VueUtil.getCurrentDirectoryPath()));
    		}
    		else
    			chooser = new VueFileChooser();
        	
        	
    		File fileName = null;
    		
            int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
            if (option == VueFileChooser.APPROVE_OPTION) 
            {
                fileName = chooser.getSelectedFile();
                
                if (fileName == null) 
                	return;
                
                if (fileName.exists()) 
           		 	VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());

                if (c instanceof LWNode)
                {
                	//Resource r = c.getResource();
              
                	VUE.setActive(LWComponent.class, this, null);
              
                	c.setResource(fileName);
                	VUE.setActive(LWComponent.class, this, c);
                }
                else if (c instanceof LWSlide)
                {                	                     
                  	VUE.setActive(LWComponent.class, this, null);
                 
                  	String f = (fileName.getName()).toLowerCase();
                  	String extension = f.substring(f.lastIndexOf(".")+1,f.length());
                  	LWComponent node = null;
                  	//System.out.println("STRING : " + extension);
                  	if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif"))
                  		node = new LWImage();
                  	else
                  	{
                  		node = NodeModeTool.createNewNode();
                  		 ((LWNode)node).setAsTextNode(true);
                  	}
                                      	
                    Resource resource = c.getResourceFactory().get(fileName);                    
                    node.setAutoSized(false);
                    node.setLabel(resource.getTitle());                                                            
                    
                    node.setResource(resource);
                    VUE.getActiveViewer().getFocal().pasteChild(node);
                    VUE.getActiveViewer().getSelection().setTo(node);
                    VUE.setActive(LWComponent.class, this, c);
                
                }
            //  }
            /*  else            	  
              {
            	  final Object[] defaultOrderButtons = { "Replace","Add","Cancel"};
                  int response = JOptionPane.showOptionDialog
                  ((Component)VUE.getApplicationFrame(),
                   new String("Do you want to replace the current resource or add this resource as a child node?"),
                   "Replace Resource?",
                   JOptionPane.YES_NO_CANCEL_OPTION,
                   JOptionPane.PLAIN_MESSAGE,
                   null,
                   defaultOrderButtons,             
                   "Add"
                   );                  
                  
               
                  if (response == JOptionPane.YES_OPTION) { // Save
                      //c.setResource(new URLResource(fileName.getAbsolutePath()));
                      c.setResource(fileName);
                  } 
                  else if (response == JOptionPane.NO_OPTION) { // Don't Save
                      {
                          //LWNode node = NodeModeTool.createNewNode();
                          
                          Resource resource = c.getResourceFactory().get(fileName);
                          LWNode node= new LWNode(resource.getTitle());
                          node.setResource(resource);
                          //node.addChild(image);                         
                          c.addChild(node);
                      }
                  } else // anything else (Cancel or dialog window closed)
                      return;
              }*/                                                                  	
        }
        }
    };
    
    public static final LWCAction AddURLAction = new LWCAction(VueResources.getString("mapViewer.componentMenu.addURL.label")) {
            public void act(LWComponent c) 
            {
    		File fileName = null;
    		final Object[] defaultButtons = { "OK","Cancel"};
    		/*String option = (String)JOptionPane.showInputDialog((Component)VUE.getApplicationFrame(), 
                                                                    ,
                                                                    "Add URL to Node",
                                                                    	,
                                                                    null,
                                                                    null,
                                                                    "http://");*/
    		//JOptionPane.
    		String resourceString = "http://";
    		Resource r =c.getResource();
    		if (r != null)
    			resourceString = r.getSpec();
    		
    			
            JOptionPane optionPane= new JOptionPane("Enter the URL to add: ",JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION,null,defaultButtons,"OK");
            javax.swing.JDialog dialog = optionPane.createDialog((Component)VUE.getApplicationFrame(), "Add URL to Node");
            dialog.setModal(true);
            
            optionPane.setInitialSelectionValue(resourceString);
            optionPane.setWantsInput(true);
            optionPane.enableInputMethods(true);

            dialog.setSize(new Dimension(350,125));
            dialog.setVisible(true);
            
            String option = (String)optionPane.getInputValue();
            
                if (option == null || option.length() <= 0 || optionPane.getValue().equals("Cancel"))
                    return;
                
    				
              //  if (!option.startsWith("http://") || !option.startsWith("https://") || !option.startsWith("file://"))
                //	option = "http://" + option;
                //int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
                //if (option != null && option.length() > 0) {

            	URI uri = null;
            	
                try {
                    uri = new URI(option);
                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                                                  "Malformed URL, resource could not be added.", 
                                                  "Malformed URL", 
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
             
                r = c.getResource();
               // if (r == null) {
                    r = c.getResourceFactory().get(uri);
                    if (r == null) {
                        JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                                                      "Malformed URL, resource could not be added.", 
                                                      "Malformed URL", 
                                                      JOptionPane.ERROR_MESSAGE);
                    } else
                    {
                    	if (c instanceof LWNode)
                    	{
                    		VUE.setActive(LWComponent.class, this, null);
                    		c.setResource(r);                        
                    		VUE.setActive(LWComponent.class, this, c);
                    	}
                        else if (c instanceof LWSlide)
                        {
                        	 
                            
                          	VUE.setActive(LWComponent.class, this, null);
                            LWNode node = NodeModeTool.createNewNode();
                            Resource resource = c.getResourceFactory().get(uri);
                            //node.setStyle(c.getStyle());                    
                            //LWNode node= new LWNode(resource.getTitle());                  
                            
                            //node.addChild(image);
                            VUE.getActiveViewer().getFocal().dropChild(node);
                            
                            node.setLabel(uri.toString());
                            node.setResource(resource);
                            VUE.setActive(LWComponent.class, this, c);
                        
                        }
                    		
                    }
//                     try {
//                         c.setResource(new URLResource(url.toURL()));
//                     } catch (MalformedURLException e) {
//                         JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
//                                                       "Malformed URL, resource could not be added.", 
//                                                       "Malformed URL", 
//                                                       JOptionPane.ERROR_MESSAGE);
//                     }  
                //}
                /*else {
                    final Object[] defaultOrderButtons = { "Replace","Add","Cancel"};
                    int response = JOptionPane.showOptionDialog
                        ((Component)VUE.getApplicationFrame(),
                         new String("Do you want to replace the current resource or add this resource as a child node?"),
                         "Replace Resource?",
                         JOptionPane.YES_NO_CANCEL_OPTION,
                         JOptionPane.PLAIN_MESSAGE,
                         null,
                         defaultOrderButtons,             
                         "Add"
                         );                  
                  
               
                    if (response == JOptionPane.YES_OPTION) { // Save
                        r = c.getResourceFactory().get(uri);
                        if (r == null) {
                            JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                                                          "Malformed URL, resource could not be added.", 
                                                          "Malformed URL", 
                                                          JOptionPane.ERROR_MESSAGE);
                        } else
                            c.setResource(r);
                        
//                         try {
//                             c.setResource(new URLResource(url.toURL()));
//                         } catch (MalformedURLException e) {
//                             JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
//                                                           "Malformed URL, resource could not be added.", 
//                                                           "Malformed URL", 
//                                                           JOptionPane.ERROR_MESSAGE);
//                             return;
//                         }
                        
                    } 
                    else if (response == JOptionPane.NO_OPTION) { // Don't Save
                        //LWNode node = NodeModeTool.createNewNode();
                        
//                             URLResource urlResource;
//                             try {
//                                 urlResource = new URLResource(url.toURL());
//                             } catch (MalformedURLException e) {
//                                 JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
//                                                               "Malformed URL, resource could not be added.", 
//                                                               "Malformed URL", 
//                                                               JOptionPane.ERROR_MESSAGE);
//                                 return;
//                             }
//                             URLResource urlResource;

                        r = c.getResourceFactory().get(uri);
                        if (r == null) {
                            JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                                                          "Malformed URL, resource could not be added.", 
                                                          "Malformed URL", 
                                                          JOptionPane.ERROR_MESSAGE);
                        } else {
                            final LWNode node = new LWNode(uri.toString());
                            node.setResource(r);
                            //node.addChild(image);                         
                            c.addChild(node);
                        }
                    } // else // anything else (Cancel or dialog window closed)
                } */                                                                 	
            }
        };


    public static final LWCAction EditMasterSlide = new LWCAction("Edit master slide")
    {
    	public void act(LWSlide slide)
    	{
            final LWSlide masterSlide = slide.getMasterSlide();
            if (VUE.getActiveViewer() != null)
            {
            	if (VUE.getActiveViewer().getFocal().equals(masterSlide))
            	{
            		VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
            		VUE.setActive(LWMap.class, this, VUE.getActiveMap());
            		/*ZoomTool.setZoomFitRegion(VUE.getActiveViewer(),
                            zoomBounds,
                            0,
                            false);
                            */
            		//Point2D.Float originOffset = VUE.getActiveMap().getTempUserOrigin();
            	//	ZoomTool.setZoom(VUE.getActiveMap().getTempZoom());
            		//if (originOffset != null)
            			//VUE.getActiveViewer().setMapOriginOffset(originOffset.getX(), originOffset.getY());
            		ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
            		VUE.getReturnToMapButton().setVisible(false);
            	}
            	else
            	{
            		if (!(VUE.getActiveViewer().getFocal() instanceof LWSlide))
            		{
            		//	zoomFactor = VUE.getActiveViewer().getZoomFactor();
            		//	VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());            			
            			VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());
                		VUE.getReturnToMapButton().setVisible(true);
                		VUE.getActiveMap().setTempBounds(VUE.getActiveViewer().getVisibleMapBounds());
            		//	VUE.getActiveMap().setTempUserOrigin(VUE.getActiveViewer().getOriginLocation());
            		}
            		VUE.getActiveViewer().loadFocal(masterSlide);
            		 // update inspectors (optional -- may not actually want to do this, but
                    // currently required if you want up/down arrows to subsequently navigate
                    // the pathway)
            		
                    VUE.setActive(tufts.vue.MasterSlide.class, this, masterSlide);
            	}
                
            
           
            }
//     		long now = System.currentTimeMillis();
//     		MapMouseEvent mme = new MapMouseEvent(new MouseEvent(VUE.getActiveViewer(),
//                                                                      MouseEvent.MOUSE_CLICKED,
//                                                                      now,
//                                                                      5,5,5,5,
//                                                                      false));
//     		((LWSlide)c).getPathwayEntry().pathway.getMasterSlide().doZoomingDoubleClick(mme);
    	}
    };

	//private static double zoomFactor =0;  
	//private static Point2D originOffset = null;

	public static class ReturnToMapAction extends VueAction
    {
        public void act() 
        {
        	if (VUE.getActiveViewer() != null)
            {
            	if (VUE.getActiveViewer().getFocal() instanceof LWSlide || VUE.getActiveViewer().getFocal() instanceof MasterSlide)
            	{
            		VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
            		VUE.setActive(LWMap.class, this, VUE.getActiveMap());
            		/*ZoomTool.setZoomFitRegion(VUE.getActiveViewer(),
                            zoomBounds,
                            0,
                            false);
                            */
            		
            		//Point2D.Float originOffset = VUE.getActiveMap().getTempUserOrigin();
            		//double tempZoom = VUE.getActiveMap().getTempZoom();
            		
            		//System.out.println("temp #s : " +originOffset + " " + tempZoom);
            	//	ZoomTool.setZoom(tempZoom);
            		//if (originOffset != null)
            			//VUE.getActiveViewer().setMapOriginOffset(originOffset.getX(), originOffset.getY());
            		if (VUE.getActiveMap().getTempBounds() != null)
            			ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
            		VUE.getReturnToMapButton().setVisible(false);
            		//ZoomTool.setZoom(zoomFactor);
            	    

            		
            	}
            	else if (VUE.getActiveViewer().getFocal() instanceof LWGroup)
            	{
            		VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
            		if (VUE.getActiveMap().getTempBounds() != null)
            			ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
            		VUE.getReturnToMapButton().setVisible(false);
            	}
            }
        }
    };
    public static final VueAction ReturnToMap = new ReturnToMapAction();
    public static final LWCAction EditSlide = new LWCAction("Edit slide")
    {
    	public void act(LWSlide slide)
    	{
            //final LWSlide masterSlide = slide.getPathwayEntry().pathway.getMasterSlide();
    		if (VUE.getActiveViewer() != null)
            {
            	if (VUE.getActiveViewer().getFocal().equals(slide) || VUE.getActiveViewer().getFocal() instanceof MasterSlide)
            	{
            		VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
            		VUE.setActive(LWMap.class, this, VUE.getActiveMap());
            		/*ZoomTool.setZoomFitRegion(VUE.getActiveViewer(),
                            zoomBounds,
                            0,
                            false);
                            */
            		
            		//Point2D.Float originOffset = VUE.getActiveMap().getTempUserOrigin();
            		//double tempZoom = VUE.getActiveMap().getTempZoom();
            		//System.out.println("temp #s : " +originOffset + " " + tempZoom);
            		//ZoomTool.setZoom(tempZoom);
            		//if (originOffset != null)
            		//	VUE.getActiveViewer().setMapOriginOffset(originOffset.getX(), originOffset.getY());
            		if (VUE.getActiveMap().getTempBounds() != null)
            			ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
            		VUE.getReturnToMapButton().setVisible(false);
            		//ZoomTool.setZoom(zoomFactor);
            	    

            		
            	}
            	else
            	{
            		
            		VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());
            		VUE.getReturnToMapButton().setVisible(true);
            		VUE.getActiveMap().setTempBounds(VUE.getActiveViewer().getVisibleMapBounds());
            		//VUE.getActiveMap().setTempUserOrigin(VUE.getActiveViewer().getOriginLocation());
            		
            		//VUE.getActiveMap().setUserOrigin(p)
            		//VUE.getActiveViewer().getO
            		//originOffset = VUE.getActiveViewer().get
            		//zoomBounds = VUE.getActiveViewer().getDisplayableMapBounds();
            		//Point2D.Float originOffset = VUE.getActiveMap().getTempUserOrigin();
            		//double tempZoom = VUE.getActiveMap().getTempZoom();
            		//System.out.println("2temp #s : " +originOffset + " " + tempZoom);
            		VUE.getActiveViewer().loadFocal(slide);
            		
            		 // update inspectors (optional -- may not actually want to do this, but
                    // currently required if you want up/down arrows to subsequently navigate
                    // the pathway)
            		VUE.setActive(LWSlide.class, this, slide);
            	}
                
            
           
            }
            
             
            // update inspectors (optional -- may not actually want to do this, but
            // currently required if you want up/down arrows to subsequently navigate
            // the pathway)
            
            
//     		long now = System.currentTimeMillis();
//     		MapMouseEvent mme = new MapMouseEvent(new MouseEvent(VUE.getActiveViewer(),
//                                                                      MouseEvent.MOUSE_CLICKED,
//                                                                      now,
//                                                                      5,5,5,5,
//                                                                      false));
//     		((LWSlide)c).getPathwayEntry().pathway.getMasterSlide().doZoomingDoubleClick(mme);
    	}
    };
    private static boolean hasSyncable(LWSelection s) {
        return getSyncable(s) != null;
    }
    
//     private static LWSlide getSyncable(LWSelection s) {
//         return getSyncable(true);
//     }
    
    private static LWSlide getSyncable(LWSelection s) {
        if (s.only() instanceof LWSlide && ((LWSlide)s.only()).canSync()) {
            return (LWSlide) s.only();
        } else {
            LWPathway.Entry e = VUE.getActiveEntry();
            if (e != null && e.canProvideSlide() && !e.isMapView())
                return e.getSlide();
        }
        return null;
    }
    
    public static final LWCAction SyncToNode = new LWCAction(VueResources.getString("mapViewer.componentMenu.syncMenu.slide2node")) 
    {
        boolean enabledFor(LWSelection s) { return hasSyncable(s); }
    	public void act(LWSelection s)
    	{    		
            Slides.synchronizeSlideToNode(getSyncable(s));
    	}
    	public String getUndoName()
    	{
    		return "Sync";
    	}
    };
    
    public static final LWCAction SyncToSlide = new LWCAction(VueResources.getString("mapViewer.componentMenu.syncMenu.node2slide")) 
    {
        boolean enabledFor(LWSelection s) { return hasSyncable(s); }
    	public void act(LWSelection s)
    	{    		
            Slides.synchronizeNodeToSlide(getSyncable(s));
    	}
    	public String getUndoName()
    	{
    		return "Sync";
    	}
    };
    
    public static final LWCAction SyncAll = new LWCAction(VueResources.getString("mapViewer.componentMenu.syncMenu.all")) 
    {
        boolean enabledFor(LWSelection s) { return hasSyncable(s); }
    	public void act(LWSelection s)
    	{    		
            Slides.synchronizeAll(getSyncable(s));
    	}
    	public String getUndoName()
    	{
    		return "Sync";
    	}
    };
    
    public static final LWCAction RemoveResourceAction = new LWCAction(VueResources.getString("mapViewer.componentMenu.removeResource.label")) {
        public void act(LWComponent c) 
        {        	             
        	URLResource nullResource = null;
        	c.setResource(nullResource);                                    
        }
    };

    //m.add(Actions.AddURLAction);
//    m.add(Actions.RemoveResourceAction);
    public static final Action NotesAction = new NotesActionClass(VueResources.getString("mapViewer.componentMenu.notes.label"));
    public static final Action ContextNotesAction = new NotesActionClass("Add Notes");
    	
    public static class NotesActionClass extends VueAction
    {
    	public NotesActionClass(String s)
    	{
    		super(s);
    	}
        public void act() {        	
        	
        	//GUI.makeVisibleOnScreen(VUE.getInfoDock());
        	VUE.getInfoDock().setVisible(true);
        	VUE.getInfoDock().setRolledUp(false,true);
        	VUE.getInfoDock().toFront();
        	VUE.getInspectorPane().showNotesView();
        	}
        //public void act() { VUE.ObjectInspector.setVisible(true); }
    };
    
    
    
    public static final Action InfoAction = new VueAction(VueResources.getString("mapViewer.componentMenu.info.label")) {
        public void act() { 
        	VUE.getInspectorPane().showInfoView();
        	GUI.makeVisibleOnScreen(this, tufts.vue.ui.InspectorPane.class);
        	VUE.getInfoDock().setRolledUp(false,true);
        	}
        //public void act() { VUE.ObjectInspector.setVisible(true); }
    };
    
    //-------------------------------------------------------
    // Group/Ungroup
    //-------------------------------------------------------
    
    public static final Action Group =
        new LWCAction("Group", keyStroke(KeyEvent.VK_G, COMMAND), "/tufts/vue/images/xGroup.gif") {
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) {
            
            // TODO: allow even if all DON'T have same parent: e.g., if you select
            // all, and this includes the children of some nodes selected, still allow
            // everything into one group, and just ignore the children of the non-map.
            // E.g., implement as a special case: if multiple parents, and at least
            // one has the map has a parent, grab all elements in selection that are also
            // children of the map, and group them.

            // Would be nice to fully know up front if we're going to allow the grouping tho.
            // E.g., if either all have same parent, or there's at least two items in the
            // group which have the map as a parent.  Could easily have the selection
            // keep a count for each parent class type encountered (in a hash).

            // As long as doing that, might as well keep a hash of all types in selection,
            // tho we only appear to ever use this for checking the group count (maybe special
            // case).
            
            //return s.size() >= 2;
            
            
            // enable only when two or more objects in selection,
            // and all share the same parent
            //return s.size() >= 2 && s.allHaveSameParent();
            
            // below condition doesn't allow explicit grouping of links -- was this causing trouble somewhere?
            return ((s.size() - s.countTypes(LWLink.class)) >= 2 && s.allHaveSameParent() && !(VUE.getActiveViewer().getFocal() instanceof LWSlide));
        }
        void act(LWSelection s) {
            if (s.size() == 2 && s.countTypes(LWGroup.class) == 1) {
                // special case: join the group (really need another action for this)
                LWGroup toJoin;
                LWComponent toAdd;
                if (s.first() instanceof LWGroup) {
                    toJoin = (LWGroup) s.first();
                    toAdd = s.last();
                } else {
                    toJoin = (LWGroup) s.last();
                    toAdd = s.first();
                }
                toJoin.addChild(toAdd);
            } else {
                LWContainer parent = s.first().getParent(); // all have same parent
                LWGroup group = LWGroup.create(s);
                parent.addChild(group);
                VUE.getSelection().setTo(group);
            }
        }
    };


    /**
     * If there are any groups in the selection, those groups will be dispersed, and
     * everything else in selection is ignored.
     * Otherwise, if everything in the selection has the same parent group,
     * they'll all be removed from that group.
     * If neither of the above conditions are met, the action is disabled.
     *
     * If groups were dispersed, the selection will be set to the contents of the
     * dispersed groups.
     */
    public static final LWCAction Ungroup =
        //new LWCAction("Ungroup", keyStroke(KeyEvent.VK_G, COMMAND+SHIFT), "/tufts/vue/images/GroupGC.png") {
        //new LWCAction("Ungroup", keyStroke(KeyEvent.VK_G, COMMAND+SHIFT), "/tufts/vue/images/GroupUnGC.png") {
        new LWCAction("Ungroup", keyStroke(KeyEvent.VK_G, COMMAND+SHIFT), "/tufts/vue/images/xUngroup.png") {
            boolean mayModifySelection() { return true; }
            boolean enabledFor(LWSelection s) {
                return s.countTypes(LWGroup.class) > 0 || s.allHaveSameParentOfType(LWGroup.class);
            }
            void act(LWSelection s) {
                final Collection<LWComponent> toSelect = new HashSet(); // ensure no duplicates
                if (s.countTypes(LWGroup.class) > 0) {
                    if (DEBUG.EVENTS) out("Ungroup: dispersing any selected groups");
                    disperse(s, toSelect);
                } else {
                    if (DEBUG.EVENTS) out("Ungroup: de-grouping any selected inside a group");
                    degroup(s, toSelect);
                }
                
                if (toSelect.size() > 0)
                    VUE.getSelection().setTo(toSelect);
                else
                    VUE.getSelection().clear();
            }

            private void degroup(Iterable<LWComponent> iterable, Collection toSelect)
            {
                final List<LWComponent> removing = new ArrayList();
                
                for (LWComponent c : iterable) {
                    if (c.getParent() instanceof LWGroup) {
                        //if (LWLink.LOCAL_LINKS && c instanceof LWLink && ((LWLink)c).isConnected()) {
                        if (c instanceof LWLink && ((LWLink)c).isConnected()) {
                            // links control their own parentage when connected
                            continue;
                        } else
                            removing.add(c);
                    }
                }

                // This action only enabled if all the selected components have
                // exactly the same parent group.
                
                if (removing.size() > 0) {
                    final LWComponent first = (LWComponent) removing.get(0);
                    final LWGroup group = (LWGroup) first.getParent(); // the group losing children
                    final LWContainer newParent = group.getParent();
                    group.removeChildren(removing); // more control & efficient events
                    newParent.addChildren(removing);
                    toSelect.addAll(removing);

                    // LWGroups now handle auto-dispersal themseleves if all children are removed,
                    // so we don't ened to worry about auto-dispersing any groups that end up
                    // up with less than two children in them.

                }

                //VUE.getSelection().setTo(toSelect);
                    
                
            }

            private void disperse(Iterable<LWComponent> iterable, Collection toSelect) {
                for (LWComponent c : iterable) {
                    if (c instanceof LWGroup) {
                        toSelect.addAll(c.getChildList());
                        ((LWGroup)c).disperse();
                    }
                }
            }
        };
    
    public static final LWCAction Rename =
        new LWCAction("Rename", VueUtil.isWindowsPlatform() ? keyStroke(KeyEvent.VK_F2) : keyStroke(KeyEvent.VK_ENTER)) {
        boolean undoable() { return false; } // label editor handles the undo
            
        boolean enabledFor(LWSelection s) {
            return s.size() == 1 && s.first().supportsUserLabel() && !s.first().hasFlag(Flag.LOCKED);
        }
        void act(LWComponent c) {
            // todo: throw interal exception if c not in active map
            // todo: not working in slide viewer...
            VUE.getActiveViewer().activateLabelEdit(c);
        }
    };

    /*
      // doesn't help unless is actually in the VueMenuBar -- change this, so all keystrokes
      // are processed if in menu bar or not (hack into VueMenuBar, or maybe FocusManger?)
    public static final Action Rename2 =
        new LWCAction("Rename", VueUtil.isWindowsPlatform() ? keyStroke(KeyEvent.VK_ENTER) : keyStroke(KeyEvent.VK_F2)) {
        boolean undoable() { return false; } // label editor handles the undo
        boolean enabledFor(LWSelection s) {
            return s.size() == 1 && s.first().supportsUserLabel();
        }
        void act(LWComponent c) {
            // todo: throw interal exception if c not in active map
            VUE.getActiveViewer().activateLabelEdit(c);
        }
    };
    */
    
    
    //-------------------------------------------------------
    // Arrange actions
    //-------------------------------------------------------
    
    public static final LWCAction BringToFront =
    new LWCAction("Bring to Front",
    "Raise object to the top, completely unobscured",
    keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND+SHIFT)) {
        boolean enabledFor(LWSelection s) {
            if (s.size() == 1)
                return true;
              //return !s.first().getParent().isOnTop(s.first()); // todo: not always getting updated
            return s.size() >= 2;
        }
        void act(LWSelection selection) {
            LWContainer.bringToFront(selection);
        }
    };
    public static final LWCAction SendToBack =
    new LWCAction("Send to Back",
    "Make sure this object doesn't obscure any other object",
    keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND+SHIFT)) {
        boolean enabledFor(LWSelection s) {
            if (s.size() == 1)
                return true;
              //return !s.first().getParent().isOnBottom(s.first()); // todo: not always getting updated
            return s.size() >= 2;
        }
        void act(LWSelection selection) {
            LWContainer.sendToBack(selection);
        }
    };
    public static final LWCAction BringForward =
    new LWCAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND)) {
        boolean enabledFor(LWSelection s) { return BringToFront.enabledFor(s); }
        void act(LWSelection selection) {
            LWContainer.bringForward(selection);
        }
    };
    public static final LWCAction SendBackward =
    new LWCAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND)) {
        boolean enabledFor(LWSelection s) { return SendToBack.enabledFor(s); }
        void act(LWSelection selection) {
            LWContainer.sendBackward(selection);
        }
    };
    
    //-------------------------------------------------------
    // Font/Text Actions
    //-------------------------------------------------------
    
    public static final LWCAction FontSmaller =
    new LWCAction("Font Smaller", keyStroke(KeyEvent.VK_MINUS, COMMAND+SHIFT)) {
        void act(LWComponent c) {
            int size = c.mFontSize.get();
            if (size > 1) {
                if (size >= 14 && size % 2 == 0)
                    size -= 2;
                else
                    size--;
                c.mFontSize.set(size);
            }
        }
    };
    public static final LWCAction FontBigger =
    new LWCAction("Font Bigger", keyStroke(KeyEvent.VK_EQUALS, COMMAND+SHIFT)) {
        void act(LWComponent c) {
            int size = c.mFontSize.get();
            if (size >= 12 && size % 2 == 0)
                size += 2;
            else
                size++;
            c.mFontSize.set(size);
        }
    };
    public static final LWCAction FontBold =
    new LWCAction("Font Bold", keyStroke(KeyEvent.VK_B, COMMAND)) {
        void act(LWComponent c) {
            c.mFontStyle.set(c.mFontStyle.get() ^ Font.BOLD);
        }
    };
    public static final LWCAction FontItalic =
    new LWCAction("Font Italic", keyStroke(KeyEvent.VK_I, COMMAND)) {
        void act(LWComponent c) {
            c.mFontStyle.set(c.mFontStyle.get() ^ Font.ITALIC);
        }
    };
    
    //-------------------------------------------------------
    // Arrange actions
    //
    // todo bug: if items have a stroke width, there is an
    // error in adjustment such that repeated adjustments
    // nudge all the nodes by what looks like half the stroke width!
    // (error occurs even in first adjustment, but easier to notice
    // in follow-ons)
    //-------------------------------------------------------
    abstract static class ArrangeAction extends LWCAction {
        static float minX, minY;
        static float maxX, maxY;
        static float centerX, centerY;
        static float totalWidth, totalHeight; // added width/height of all in selection
        // obviously not thread-safe here
        
        private ArrangeAction(String name, KeyStroke keyStroke) {
            super(name, keyStroke);
        }
        private ArrangeAction(String name, int keyCode) {
            super(name, keyStroke(keyCode, COMMAND+SHIFT));
        }
        private ArrangeAction(String name) {
            super(name);
        }
        boolean mayModifySelection() { return true; }

        boolean enabledFor(LWSelection s) {
            return s.size() >= 2
                || (s.size() == 1 && s.first().getParent() instanceof LWSlide); // todo: a have capability check (free-layout?  !isLaidOut() ?)
        }

        boolean supportsSingleMover() { return true; }
        
        void act(LWSelection selection) {
            LWComponent singleMover = null;
            
            Rectangle2D.Float r = null; // will be the total bounds area we're going to layout into

            if (supportsSingleMover() && selection.size() == 1 && selection.first().getParent() instanceof LWSlide) { // todo: capability check
                singleMover = selection.first();
                r = singleMover.getParent().getZeroBounds();
            } else if (!selection.allOfType(LWLink.class)) {
                Iterator<LWComponent> i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = i.next();
                    // remove all links from our cloned copy of the selection
                    if (c instanceof LWLink)
                        i.remove();
                    // remove all children of nodes or groups, who's parent handles their layout
                    //if (!(c.getParent() instanceof LWMap)) // really: c.isLaidOut()
                    // need to allow for in-group components now.
                    // todo: unexpected behaviour if some in-group and some not?
                    if (c.getParent() instanceof LWNode) // really: c.isLaidOut()
                        i.remove();
                }
            }

            if (!selection.allHaveSameParent())
                throw new DeniedException("all must have same parent");

            if (r == null)
                r = LWMap.getLayoutBounds(selection);

            if (selection.isSized()) {
                r.width = selection.getWidth();
                r.height = selection.getHeight();
            }

            minX = r.x;
            minY = r.y;
            maxX = r.x + r.width;
            maxY = r.y + r.height;
            centerX = (minX + maxX) / 2;
            centerY = (minY + maxY) / 2;
            totalWidth = totalHeight = 0;

            for (LWComponent c : selection) {
                totalWidth += c.getWidth();
                totalHeight += c.getHeight();
            }

            if (singleMover != null) {
                // If we're a single selected object laying out in a parent,
                // only bother to arrange that one object -- make sure
                // we can never touch the parent (it used to be added to
                // the selection above to compute our total bounds, tho we do
                // that manually now).
                arrange(singleMover);
            } else {
                arrange(selection);
            }
        }
        
        void arrange(LWSelection selection) {
            for (LWComponent c : selection)
                arrange(c);
        }
        void arrange(LWComponent c) { throw new RuntimeException("unimplemented arrange action"); }
        
    };

    public static LWComponent[] sortByX(LWComponent[] array) {
        java.util.Arrays.sort(array, LWComponent.XSorter);
        return array;
    }
    
    public static LWComponent[] sortByY(LWComponent[] array) {
        java.util.Arrays.sort(array, LWComponent.YSorter);
        return array;
    }
    
    
    public static final Action FillWidth = new ArrangeAction("Fill Width") {
        void arrange(LWComponent c) {
            c.setFrame(minX, c.getY(), maxX - minX, c.getHeight());
        }
    };
    public static final Action FillHeight = new ArrangeAction("Fill Height") {
        void arrange(LWComponent c) {
            c.setFrame(c.getX(), minY, c.getWidth(), maxY - minY);
        }
    };

    public static class NudgeAction extends LWCAction {
        final int osdx, osdy; // on-screen delta-x, deltay-y
        NudgeAction(int dx, int dy, String name, KeyStroke stroke) {
            super(name, stroke);
            osdx = dx;
            osdy = dy;
        }

        public static boolean enabledOn(LWSelection s) {
            return s.size() > 0 && s.first().isMoveable();
        }
        
        boolean enabledFor(LWSelection s) {
            return enabledOn(s);
        }

        @Override
        void act(LWComponent c) { nudgeOrReorder(c,  osdx, osdy); }
        
        private void nudgeOrReorder(LWComponent c, int x, int y) {
            if (c.getParent() instanceof LWNode) { // TODO: a more abstract test... inVisuallyOrderedContainer?
                if (x < 0 || y < 0)
                    c.getParent().sendBackward(c);
                else
                    c.getParent().bringForward(c);
            } else {
                // With relative coords, if we want to enforce a certian on-screen pixel change,
                // we need to adjust for the current zoom, as well as the net map scaling present
                // in the parent of the moving object.
                final double unit = VUE.getActiveViewer().getZoomFactor() * c.getParent().getMapScale();
                final float dx = (float) (x / unit);
                final float dy = (float) (y / unit);
                c.translate(dx, dy);
            }
        }
    }
    
    // Note: if JScrollPane has focus, it will grap unmodified arrow keys.  If, say, a random DockWindow
    // has focus (e.g., not a field that would also grab arrow keys), they get through.
    // So the MapViewer has to specially check for these arrows keys to invoke these actions to
    // override it's parent JScrollPane.
    public static final LWCAction NudgeUp       = new NudgeAction(  0,  -1, "Nudge Up",    keyStroke(KeyEvent.VK_UP));
    public static final LWCAction NudgeDown     = new NudgeAction(  0,   1, "Nudge Down",  keyStroke(KeyEvent.VK_DOWN));
    public static final LWCAction NudgeLeft     = new NudgeAction( -1,   0, "Nudge Left",  keyStroke(KeyEvent.VK_LEFT));
    public static final LWCAction NudgeRight    = new NudgeAction(  1,   0, "Nudge Right", keyStroke(KeyEvent.VK_RIGHT));

    public static final LWCAction BigNudgeUp    = new NudgeAction(  0, -10, "Big Nudge Up",    keyStroke(KeyEvent.VK_UP, SHIFT));
    public static final LWCAction BigNudgeDown  = new NudgeAction(  0,  10, "Big Nudge Down",  keyStroke(KeyEvent.VK_DOWN, SHIFT));
    public static final LWCAction BigNudgeLeft  = new NudgeAction(-10,   0, "Big Nudge Left",  keyStroke(KeyEvent.VK_LEFT, SHIFT));
    public static final LWCAction BigNudgeRight = new NudgeAction( 10,   0, "Big Nudge Right", keyStroke(KeyEvent.VK_RIGHT, SHIFT));
        
    
    public static final ArrangeAction AlignTopEdges = new ArrangeAction("Align Top Edges", KeyEvent.VK_UP) {
        void arrange(LWComponent c) { c.setLocation(c.getX(), minY); }
    };
    public static final ArrangeAction AlignBottomEdges = new ArrangeAction("Align Bottom Edges", KeyEvent.VK_DOWN) {
        void arrange(LWComponent c) { c.setLocation(c.getX(), maxY - c.getHeight()); }
    };
    public static final ArrangeAction AlignLeftEdges = new ArrangeAction("Align Left Edges", KeyEvent.VK_LEFT) {
        void arrange(LWComponent c) { c.setLocation(minX, c.getY()); }
    };
    public static final ArrangeAction AlignRightEdges = new ArrangeAction("Align Right Edges", KeyEvent.VK_RIGHT) {
        void arrange(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
    };
    public static final ArrangeAction AlignCentersRow = new ArrangeAction("Align Centers in Row", KeyEvent.VK_R) {
        void arrange(LWComponent c) { c.setLocation(c.getX(), centerY - c.getHeight()/2); }
    };
    public static final ArrangeAction AlignCentersColumn = new ArrangeAction("Align Centers in Column", KeyEvent.VK_C) {
        void arrange(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
    };
    
    public static final ArrangeAction MakeRow = new ArrangeAction("Make Row", keyStroke(KeyEvent.VK_R, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 2; }
            // todo bug: an already made row is shifting everything to the left
            // (probably always, actually)
            void arrange(LWSelection selection) {
                AlignCentersRow.arrange(selection);
                maxX = minX + totalWidth;
                DistributeHorizontally.arrange(selection);
            }
    };
    public static final ArrangeAction MakeColumn = new ArrangeAction("Make Column", keyStroke(KeyEvent.VK_C, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 2; }
            void arrange(LWSelection selection) {
                AlignCentersColumn.arrange(selection);
//                 float height;
//                 if (selection.getHeight() > 0)
//                     height = selection.getHeight();
//                 else
//                     height = totalHeight;
//                 maxY = minY + height;
                maxY = minY + totalHeight;
                DistributeVertically.arrange(selection);
            }
        };
    
    public static final ArrangeAction DistributeVertically = new ArrangeAction("Distribute Vertically", KeyEvent.VK_V) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            // use only *2* in selection if use our minimum layout region setting
            void arrange(LWSelection selection) {
                LWComponent[] comps = sortByY(sortByX(selection.asArray()));
                float layoutRegion = maxY - minY;
                //if (layoutRegion < totalHeight)
                //  layoutRegion = totalHeight;
                float verticalGap = (layoutRegion - totalHeight) / (selection.size() - 1);
                float y = minY;
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(c.getX(), y);
                    y += c.getHeight() + verticalGap;
                }
            }
        };
    
    public static final ArrangeAction DistributeHorizontally = new ArrangeAction("Distribute Horizontally", KeyEvent.VK_H) {
            boolean supportsSingleMover() { return false; }
        boolean enabledFor(LWSelection s) { return s.size() >= 3; }
        void arrange(LWSelection selection) {
            LWComponent[] comps = sortByX(sortByY(selection.asArray()));
            float layoutRegion = maxX - minX;
            //if (layoutRegion < totalWidth)
            //  layoutRegion = totalWidth;
            float horizontalGap = (layoutRegion - totalWidth) / (selection.size() - 1);
            float x = minX;
            for (int i = 0; i < comps.length; i++) {
                LWComponent c = comps[i];
                c.setLocation(x, c.getY());
                x += c.getWidth() + horizontalGap;
            }
        }
    };
    
   
    public static final Action[] EXTEND_MENU_ACTIONS = {
    	  FillWidth,
          FillHeight
    };
    /** Helper for menu creation.  Null's indicate good places
     * for menu separators. */
    public static final Action[] ARRANGE_MENU_ACTIONS = {
        AlignLeftEdges,
        AlignRightEdges,
        AlignTopEdges,
        AlignBottomEdges,
        null,
        AlignCentersRow,
        AlignCentersColumn,
        null,    
        MakeRow,
        MakeColumn,
        null,
        DistributeVertically,
        DistributeHorizontally,
        null,
        NudgeUp,
        NudgeDown,
        NudgeLeft,
        NudgeRight
    };
    public static final Action[] ARRANGE_SINGLE_MENU_ACTIONS = {
        NudgeUp,
        NudgeDown,
        NudgeLeft,
        NudgeRight
    };
    
    //-----------------------------------------------------------------------------
    // VueActions
    //-----------------------------------------------------------------------------
    public static final Action NewMap =
    new VueAction("New", keyStroke(KeyEvent.VK_N, COMMAND+SHIFT), ":general/New") {
        private int count = 1;
        boolean undoable() { return false; }
        boolean enabled() { return true; }
        public void act() {
            VUE.displayMap(new LWMap("New Map " + count++));
        }
    };
    public static final Action Revert =
        //new VueAction("Revert", keyStroke(KeyEvent.VK_R, COMMAND+SHIFT), ":general/Revert") { // conflicts w/align centers in row
        new VueAction("Revert", null, ":general/Revert") {            
            boolean undoable() { return false; }
            boolean enabled() 
            { 
            	 
            		return true;
            }
            public void act() {
                
            	if (tufts.vue.VUE.getActiveMap().getFile() == null)
            	{
            		JOptionPane.showMessageDialog(VUE.getApplicationFrame(),
            				"There is no saved version of this map to revert to.",
            				"Can Not Revert",
            				JOptionPane.PLAIN_MESSAGE);
              
            		return;
            	}
                	LWMap map = tufts.vue.VUE.getActiveMap();
                	VUE.closeMap(map,true);
                	tufts.vue.action.OpenAction.reloadMap(map);                	
                                
            }
        };
    public static final Action CloseMap =
    new VueAction("Close", keyStroke(KeyEvent.VK_W, COMMAND)) {
        // todo: listen to map viewer display event to tag
        // with currently displayed map name
        boolean undoable() { return false; }
        public void act() {
            VUE.closeMap(VUE.getActiveMap());
        }
    };
    public static final Action Undo =
    new VueAction("Undo", keyStroke(KeyEvent.VK_Z, COMMAND), ":general/Undo") {
        boolean undoable() { return false; }
        public void act() { VUE.getUndoManager().undo(); }
        
    };
    public static final Action Redo =
    new VueAction("Redo", keyStroke(KeyEvent.VK_Z, COMMAND+SHIFT), ":general/Redo") {
        boolean undoable() { return false; }
        public void act() { VUE.getUndoManager().redo(); }
    };
    
    
    
    
    //-------------------------------------------------------
    // Zoom actions
    // Consider having the ZoomTool own these actions -- any
    // other way to have mutiple key values trigger an action?
    // Something about this feels kludgy.
    //-------------------------------------------------------
    
    public static final VueAction ZoomIn =
    //new VueAction("Zoom In", keyStroke(KeyEvent.VK_PLUS, COMMAND)) {
    new VueAction("Zoom In", keyStroke(KeyEvent.VK_EQUALS, COMMAND), ":general/ZoomIn") {
        public void act() {
            ZoomTool.setZoomBigger(null);
        }
    };
    public static final VueAction ZoomOut =
    new VueAction("Zoom Out", keyStroke(KeyEvent.VK_MINUS, COMMAND), ":general/ZoomOut") {
        public void act() {
            ZoomTool.setZoomSmaller(null);
        }
    };
    public static final VueAction ZoomFit =
        new VueAction("Fit in Window", keyStroke(KeyEvent.VK_0, COMMAND+SHIFT), ":general/Zoom") {
        public void act() {
            ZoomTool.setZoomFit();
        }
    };
    public static final VueAction ZoomActual =
    new VueAction(VueResources.getString("actions.zoomActual.label"), keyStroke(KeyEvent.VK_1, COMMAND+SHIFT)) {
        // no way to listen for zoom change events to keep this current
        //boolean enabled() { return VUE.getActiveViewer().getZoomFactor() != 1.0; }
        public void act() {
            ZoomTool.setZoom(1.0);
        }
    };
    
    public static final Action ZoomToSelection =
    new LWCAction("Selection Fit Window", keyStroke(KeyEvent.VK_2, COMMAND+SHIFT)) {
        public void act(LWSelection s) {
            MapViewer viewer = VUE.getActiveViewer();
            ZoomTool.setZoomFitRegion(viewer, s.getBounds(), 16, false);
        }
    };

    
    public static final VueAction ToggleFullScreen =
        new VueAction("Full Screen", VueUtil.isMacPlatform() ?
                      keyStroke(KeyEvent.VK_BACK_SLASH, COMMAND) :
                      keyStroke(KeyEvent.VK_F11)) {
    	
            public void act() {
                if (PresentationTool.ResumeActionName.equals(getActionName())) {
                    PresentationTool.ResumePresentation();
                    revertActionName(); // go back to original action
                } else {
                    VUE.toggleFullScreen(false,true);
                }
            }
            
            @Override
            public Boolean getToggleState() {
        	return tufts.vue.gui.FullScreen.inFullScreen();
            }
            
            public boolean overrideIgnoreAllActions() { return true; }
        

    };
    
    public static final VueAction ToggleSlideIcons =
        new VueAction("Slide Thumbnails", keyStroke(KeyEvent.VK_T, SHIFT+COMMAND)) {
            public void act() {
                LWPathway.toggleSlideIcons();
                PathwayPanel.getInstance().updateShowSlidesButton();

                VUE.getActiveMap().notify(this, LWKey.Repaint);
                
//                 if (VUE.getActivePathway() != null) {
//                     //VUE.getActivePathway().notify("pathway.showSlides");
//                     VUE.getActivePathway().notify(this, LWKey.Repaint);
//                 } else {
//                     VUE.getActiveMap().notify(this, LWKey.Repaint);
//                 }
            }

            @Override
            public Boolean getToggleState() {
                return LWPathway.isShowingSlideIcons();
            }    
            
            public boolean overrideIgnoreAllActions() { return true; }
        };

    

    public static final Action ToggleSplitScreen =
        new VueAction("Split Screen") {
            boolean state;
            public void act() {
                // todo: doesn't work (see VUE.java)
                state = VUE.toggleSplitScreen();
            }
            
            @Override
            public Boolean getToggleState() {
                return state;
            }    
            
            public boolean overrideIgnoreAllActions() { return true; }
    };
    
    public static final Action TogglePruning =
        new VueAction("Pruning") {
        public void act() {
            boolean enabled = LWLink.isPruningEnabled();

            // Currently, this action is ONLY fired via a menu item.  If other code
            // points might set this directly, this should be changed to a toggleState
            // action (impl getToggleState), and those code points should call this
            // action to do the toggle, so the menu item checkbox state will stay
            // synced.

            LWLink.setPruningEnabled(!enabled);
        }
    };
    public static final LWCAction NewSlide = new LWCAction(VueResources.getString("actions.newSlide.label")) {        
                public void act(Iterator i) {
                    VUE.getActivePathway().add(i);
                    GUI.makeVisibleOnScreen(VUE.getActiveViewer(), PathwayPanel.class);
                    
                }
                boolean enabledFor(LWSelection s) {
                    // items can be added to pathway as many times as you want
                    return VUE.getActivePathway() != null && s.size() > 0;
                }            
     };
     
     public static final LWCAction MergeNodeSlide = new LWCAction(VueResources.getString("actions.mergeNode.label")) {        
         public void act(Iterator i) {
             final LWComponent node = VUE.getActivePathway().createMergedNode(VUE.getSelection());
             node.setLocation(VUE.getActiveViewer().getLastMousePressMapPoint());
             VUE.getActiveViewer().getMap().add(node);
             VUE.getActivePathway().add(node);
         }
         boolean enabledFor(LWSelection s) {
             // items can be added to pathway as many times as you want
             return VUE.getActivePathway() != null && s.size() > 0;
         }            
};


     
    public static final VueAction NewNode =
    new NewItemAction("Add Node", keyStroke(KeyEvent.VK_N, COMMAND)) {
        @Override
        LWComponent createNewItem() {
            return NodeModeTool.createNewNode();
        }
    };

    //This doesn't really make a lot of sense to have 2 methods do the
    //same thing but my MapViewer.java is a bit decomposed at the moment so
    //TODO: Come back here eliminate one of these and only call one from mapviewer.
    //MK
    public static final VueAction NewText =
    new NewItemAction("Add Text", keyStroke(KeyEvent.VK_T, COMMAND)) {
        @Override
        LWComponent createNewItem() {
            return NodeModeTool.createRichTextNode("new text");
        }
    };

    public static final VueAction NewRichText =
    //new NewItemAction("New Rich Text", keyStroke(KeyEvent.VK_R, COMMAND)) {
      new NewItemAction("New Rich Text", null) { // SMF 2008-04-19 removed keystroke: was in no menus, and was conflicting
            @Override
            LWComponent createNewItem() {
                return NodeModeTool.createRichTextNode("new text");
            }
        };

    public static final Action[] NEW_OBJECT_ACTIONS = {
        NewNode,
        NewText,
        //AddImageAction,
        //AddFileAction,
        //NewSlide
    };
    
    static class NewItemAction extends VueAction {
        static LWComponent lastItem = null;
        static Point lastMouse = null;
        static Point2D lastLocation = null;
        
        NewItemAction(String name, KeyStroke keyStroke) {
            super(name, null, keyStroke, null);
        }
        
        /** @return true -- while there's an on-map label edit active, all
         * actions are disabled, however, we want to permit repeated
         * new-item actions, and new item actions auto-activate a label
         * edit, so we allow this even if everything is disabled */
        @Override
        public boolean overrideIgnoreAllActions() {
            return VUE.getActiveViewer() != null && VUE.getActiveTool().supportsEditActions();
        }

        public void act() {
            final MapViewer viewer = VUE.getActiveViewer();
            final Point currentMouse = viewer.getLastMousePoint();
            final Point2D newLocation = viewer.screenToFocalPoint(currentMouse);
            
            if (currentMouse.equals(lastMouse) && lastItem.getLocation().equals(lastLocation)) {
                // would it be better to just put in a column instead of staggering?
                // staggering (the x adjustment) does give them more flexibility on future
                // arrange actions tho.
                newLocation.setLocation(lastLocation.getX() + 10,
                                        lastLocation.getY() + lastItem.getLocalBorderHeight());
            }

            lastItem = createNewItem(viewer, newLocation);
            lastLocation = newLocation;
            lastMouse = currentMouse;
        }
        
        /**
         * The default creator: add's to map at current location and activates label edit
         * if label is supported on the object -- override if want something different.
         */
        LWComponent createNewItem(final MapViewer viewer, Point2D newLocation)
        {
            final LWComponent newItem = createNewItem();
            
            newItem.setLocation(newLocation);
            //newItem.setCenterAt(newLocation); // better but screws up NewItemAction's serial item creation positioning
            
            // maybe: run a timer and do this if no activity (e.g., node creation)
            // for 250ms or something
            viewer.getFocal().dropChild(newItem);

            //GUI.invokeAfterAWT(new Runnable() { public void run() {
                viewer.getSelection().setTo(newItem);
            //}});
            

            if (newItem.supportsUserLabel()) {
                // Just in case, do this later:
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    viewer.activateLabelEdit(newItem);
                }});
            }

            return newItem;
        }

        LWComponent createNewItem() {
            throw new UnsupportedOperationException("NewItemAction: unimplemented create");
        }
        
        
    }
    
    /**
     * LWCAction: actions that operate on one or more LWComponents.
     * Provides a number of convenience methods to allow code in
     * each action to be tight & focused.
     */

    // TODO: set an activeViewer member in VueAction, so we don't have to fetch it again
    // in any of the actions, and more importantly we can know for certian it can never
    // change from null to non-null between the time we check for nulls and fetch it
    // again, tho this should in fact be "impossible"...
    
    public static class LWCAction extends VueAction
        implements LWSelection.Listener
    {
        LWCAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon) {
            super(name, shortDescription, keyStroke, icon);
            VUE.getSelection().addListener(this);
            init();
        }
        LWCAction(String name, String shortDescription, KeyStroke keyStroke) {
            this(name, shortDescription, keyStroke, (Icon) null);
        }
        LWCAction(String name) {
            this(name, null, null, (Icon) null);
        }
        LWCAction(String name, Icon icon) {
            this(name, null, null, icon);
        }
        LWCAction(String name, KeyStroke keyStroke) {
            this(name, null, keyStroke, (Icon) null);
        }
        LWCAction(String name, KeyStroke keyStroke, String iconName) {
            super(name, keyStroke, iconName);
            VUE.getSelection().addListener(this);
            init();
        }
        public void act() {
            LWSelection selection = VUE.getSelection();
            //System.out.println("LWCAction: " + getActionName() + " n=" + selection.size());
            if (enabledFor(selection)) {
                if (mayModifySelection()) {
                    selection = (LWSelection) selection.clone();
                }
                act(selection);
                VUE.getActiveViewer().repaintSelection();
            } else {
                // This shouldn't happen as actions should already
                // be disabled if they're not appropriate, tho
                // if the action depends on something other than
                // the selection and isn't listening for it, we'll
                // get here.
                java.awt.Toolkit.getDefaultToolkit().beep();
                System.err.println(getActionName() + ": Not enabled given this selection: " + selection);
            }
        }

        /** option initialization code called at end of constructor */
        void init() {}
        
        /*
          //debug
        public void setEnabled(boolean tv)
        {
            super.setEnabled(tv);
            System.out.println(this + " enabled=" + tv);
        }
         */

        /** @return true -- the default for LWCAction's */
        @Override
        boolean isEditAction() {
            return true;
        }

        @Override
        boolean enabled() { return VUE.getActiveViewer() != null && enabledFor(VUE.getSelection()); }
        
        public void selectionChanged(LWSelection selection) {
            if (VUE.getActiveViewer() == null)
                setEnabled(false);
            else
                setEnabled(enabledFor(selection));
        }
        void checkEnabled() {
            selectionChanged(VUE.getSelection());
        }
        
        /** Is this action enabled given this selection? */
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        
        /** mayModifySelection: the action may result in an event that
         * has the viewer change what's in the current selection
         * (e.g., on delete, the viewer makes sure the deleted object
         * is no longer in the selection group -- we need this because
         * actions usually iterate thru the selection, and if it might
         * change in the middle of the iteration, we have to clone it
         * before going thru it or we will get conncurrent
         * modification exceptions.  An action does NOT need to
         * declare that it may modification the selection if it just
         * changes the selection at the end of the iteration (e.g., by
         * setting the selection to newly copied nodes or something)
         * */
        
        boolean mayModifySelection() { return false; }
        
        /** hierarchicalAction: any children in selection who's
         * parent is also in the selection are ignore during
         * iterator -- for actions such as delete where deleting
         * the parent will automatically delete any children.
         */
        boolean hierarchicalAction() { return false; }
        
        void act(LWSelection selection) {
            act(selection.iterator());
        }
        
        /**
         * Automatically apply the action serially to everything in the
         * selection -- override if this isn't what the action
         * needs to do.
         *
         * Note that the default is to descend into instances of LWGroup
         * and apply the action seperately to each child, and NOT
         * to apply the action to any nodes that are children of
         * other nodes. If the child is already in selection (e.g.
         * a select all was done) be sure NOT to act on it, otherwise
         * the action will be done twice). [ Why was this? -- disabled 2007-05-30 -- SMF ]
         */
        void act(Iterator i) {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (hierarchicalAction() && c.isAncestorSelected()) {
                    // If has no parent, must already have been acted on to get that way.
                    // If parent is selected, action will happen via it's parent.
                    continue;
                }
                act(c);
//                 // it's possible c was deleted by above action,
//                 // so make sure we don't proceed if that's the case.
//                 if (c instanceof LWGroup && !hierarchicalAction() && !c.isDeleted()) {
//                     Iterator gi = ((LWGroup)c).getChildIterator();
//                     while (gi.hasNext()) {
//                         LWComponent gc = (LWComponent) gi.next();
//                         if (!gc.isSelected())
//                             act(gc);
//                     }
//                 }
            }
        }
        void act(LWComponent c) {
            if (c instanceof LWLink)
                act((LWLink)c);
            else if (c instanceof LWNode)
                act((LWNode)c);
            else if (c instanceof LWImage)
                act((LWImage)c);
            else if (c instanceof LWSlide)
                act((LWSlide)c);
            else
                if (DEBUG.SELECTION) System.out.println("LWCAction: ignoring " + getActionName() + " on " + c);
            
        }
        
        void act(LWLink c) {
            ignoredDebug(c);
        }
        void act(LWNode c) {
            ignoredDebug(c);
        }
        void act(LWImage c) {
            ignoredDebug(c);
        }
        void act(LWSlide c) {
            ignoredDebug(c);
        }

        private void ignoredDebug(LWComponent c) {
            if (DEBUG.Enabled) System.out.println("LWCAction: ignoring " + getActionName() + " on " + c);
            //if (DEBUG.SELECTION) System.out.println("LWCAction: ignoring " + getActionName() + " on " + c);
        }
        
        void actOn(LWComponent c) { act(c); } // for manual init calls from internal code
        
        //public String toString() { return "LWCAction[" + getActionName() + "]"; }
    }
    
}