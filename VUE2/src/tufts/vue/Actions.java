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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import tufts.Util;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.LWComponent.Flag;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.gui.DeleteSlideDialog;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.renderer.SearchResultTableModel;
import edu.tufts.vue.preferences.ui.PreferencesDialog;

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
		VueFileChooser chooser = VueFileChooser.getVueFileChooser();

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
		VueFileChooser chooser = VueFileChooser.getVueFileChooser();
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
            selection().setTo(focal().getAllDescendents(ChildKind.EDITABLE));
        }
    };
    
    public static final Action SelectAllLinks =
        new VueAction("Select Links") {
            public void act() {
                selection().setTo(focal().getDescendentsOfType(ChildKind.EDITABLE, LWLink.class));
            }
        };

     public static final Action SelectAllNodes =
        new VueAction("Select Nodes") {
            public void act() {
            	selection().setTo(focal().getDescendentsOfType(ChildKind.EDITABLE, LWNode.class));
            }
        };
                
    public static final Action DeselectAll =
    new LWCAction("Deselect All", keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        public void act() {
            selection().clear();
        }
    };

    public static final Action Reselect =
        new VueAction("Reselect", keyStroke(KeyEvent.VK_R, COMMAND)) {
            public void act() {
                selection().reselect();
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

                VUE.getMetadataSearchMainGUI().setVisible(true);
                VUE.getMetadataSearchMainPanel().fillSavedSearch();
                
//                 if(tufts.vue.ui.InspectorPane.META_VERSION == tufts.vue.ui.InspectorPane.OLD)
//                 {    
//             	  VUE.getMapInfoDock().setVisible(true);
//             	  VUE.getMapInspectorPanel().activateFilterTab();
//                 }
//                 else
//                 {
// //                  tufts.vue.gui.DockWindow searchWindow = tufts.vue.MetadataSearchMainGUI.getDockWindow();
// //                  searchWindow.setVisible(true);
//                 	VUE.getMetadataSearchMainGUI().setVisible(true);              	    
//                 }
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
            new LWCAction("Edit Slide") {
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

                @Override
                public boolean overrideIgnoreAllActions() { return true; }        
                
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
                	  DeleteSlideDialog dsd = PathwayPanel.getDeleteSlideDialog();
      		        java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
                      p.x -= dsd.getWidth() / 2;
                      p.y -= dsd.getHeight() / 2;
                      dsd.setLocation(p);
                      if (dsd.showAgain())
                      {
                      	dsd.setVisible(true);
                      }
                      
                      if (dsd.getOkCanel())
                    	  pathway.remove(pathway.getCurrentIndex());
                  } else {
                	  DeleteSlideDialog dsd = PathwayPanel.getDeleteSlideDialog();
      		          java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
                      p.x -= dsd.getWidth() / 2;
                      p.y -= dsd.getHeight() / 2;
                      dsd.setLocation(p);
                      if (dsd.showAgain())
                      {
                      	dsd.setVisible(true);
                      }
                      
                      if (dsd.getOkCanel())
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
    
    private static final boolean RECORD_OLD_PARENT = true;
    private static final boolean SORT_BY_Z_ORDER = true;
    
    public static List<LWComponent> duplicatePreservingLinks(Collection<LWComponent> items) {
        return duplicatePreservingLinks(items, !RECORD_OLD_PARENT, !SORT_BY_Z_ORDER);
    }

    /**
     * @param preserveParents - if true, the old parent will be stored in the copy as a clientProperty
     * @param sortByZOrder - if true, will maintain the relative z-order of components in the dupe-set
     */
    public static List<LWComponent> duplicatePreservingLinks
        (Collection<LWComponent> items, boolean preserveParents, boolean sortByZOrder)
    {
        CopyContext.reset();
        DupeList.clear();

        final Collection<LWComponent> ordered;
        if (sortByZOrder && items.size() > 1)
            ordered = Arrays.asList(LWContainer.sort(items, LWContainer.ZOrderSorter));
        else
            ordered = items;

        // todo: ideally to preserve z-order layering of duplicated
        // elements, probably merge LinkPatcher into CopyContext, and
        // while at it change dupe action to add all the children to
        // the new parent with a single addChildren event.
        
        for (LWComponent c : ordered) {

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
            if (preserveParents) {
                // we could store this as the actual parent, but if we do that,
                // changes made to the components before they're fully baked
                // (e.g., link reconnections, translations) will generate events
                // that will confuse the UndoManager.
                copy.setClientData(LWContainer.class, c.parent);
            }
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
        if (c.isLocked())
            return false;
        else if (c.hasFlag(Flag.FIXED_LOCATION))
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
        
        /**
         * this permits repeated duplicates: when a single duplicate is
         * made, it auto-activates label edit, which normally disables
         * all actions -- this way, we can duplicate again immediately.
         */
        @Override
        public boolean overrideIgnoreAllActions() { return true; }        
        
        @Override
        void act(final LWSelection selection) {

            if (!haveViewer())
                return;

//             if (viewer().hasActiveTextEdit() && selection().size() > 1) 
//                 return;

            final List<LWComponent> dupes =
                duplicatePreservingLinks(selection, RECORD_OLD_PARENT, SORT_BY_Z_ORDER);

            final LWContainer parent0 = dupes.get(0).getClientData(LWContainer.class);
            boolean allHaveSameParent = true;

            // dupes may have fewer items in it that the selection: it will only
            // contain the top-level items duplicated -- not any of their children

            for (LWComponent copy : dupes) {
                if (copy.getClientData(LWContainer.class) != parent0)
                    allHaveSameParent = false;
                copy.translate(CopyOffset, CopyOffset);
            }

            if (allHaveSameParent) {
                parent0.addChildren(dupes, LWComponent.ADD_PASTE);
            } else {
                // Todo: would be smoother to collect all the nodes by parent
                // and do a separate collective adds for each parent
                for (LWComponent copy : dupes) 
                    copy.getClientData(LWContainer.class).pasteChild(copy);
                    
            }

            // clear out old parent references now that we're done with them
            for (LWComponent copy : dupes)
                copy.setClientData(LWContainer.class, null);

//             if (selection.only() instanceof LWLink) {
//                 LWLink oneLink = (LWLink) selection.first();
//                 // if link is directed, and tail was connected,
//                 // re-connect the tail -- duping another "outbound"
//                 // link from a node.
//             }
            
            selection().setTo(dupes);
            
            if (dupes.size() == 1 && dupes.get(0).supportsUserLabel())
                viewer().activateLabelEdit(dupes.get(0));
        }
        
        
    };

    public static final LWCAction Copy =
    new LWCAction("Copy", keyStroke(KeyEvent.VK_C, COMMAND)) {
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        void act(LWSelection selection) {
            ScratchBuffer.clear();
            ScratchBuffer.addAll(duplicatePreservingLinks(selection, !RECORD_OLD_PARENT, SORT_BY_Z_ORDER));
            
            // Enable if want to use system clipboard.  FYI: the clip board manager
            // will immediately grab all the data available from the transferrable
            // to cache in the system.
            //Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            //clipboard.setContents(VUE.getActiveViewer().getTransferableSelection(), null);
            
        }
    };

    public static final VueAction Paste =
    new VueAction("Paste", keyStroke(KeyEvent.VK_V, COMMAND)) {
        //public boolean isEnabled() //would need to listen for scratch buffer fills
        
        private Point2D.Float lastMouseLocation;
        private Point2D.Float lastPasteLocation;
        public void act() {
            
            final MapViewer viewer = VUE.getActiveViewer();
            final List pasted = duplicatePreservingLinks(ScratchBuffer);
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
            viewer.getDropFocal().addChildren(pasted, LWComponent.ADD_PASTE);
            viewer.getSelection().setTo(pasted);
            lastMouseLocation = mouseLocation;
        }

        
        // stub code for if we want to start using the system clipboard for cut/paste
        void act_system() {
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            VUE.getActiveViewer().getMapDropTarget().processTransferable(clipboard.getContents(this), null);
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
    
    public static final LWCAction Delete =
        // "/tufts/vue/images/delete.png" looks greate (from jide), but too unlike others
        new LWCAction("Delete", keyStroke(KeyEvent.VK_DELETE), ":general/Delete") {
            
            // We could use BACK_SPACE instead of DELETE because that key is bigger, and
            // on the mac it's actually LABELED "delete", even tho it sends BACK_SPACE.
            // BUT, if we use backspace, trying to use it in a text field in, say
            // the object inspector panel causes it to delete the selection instead of
            // backing up a char...
            // The MapViewer special-case handles both anyway as a backup.
                      
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        
            void act(LWSelection s) {

                s.removeAncestorSelected();

                // the selection will now only contain the top levels in the
                // the hierarchy of what's selected.

                final Collection toDelete = new ArrayList();

                for (LWComponent c : s) {
                    if (canEdit(c))
                        toDelete.add(c);
                    else
                        Log.info("delete not permitted: " + c);
                }

                for (LWContainer parent : s.getParents()) {
                    if (DEBUG.Enabled) info("deleting for parent " + parent);
                    parent.deleteChildrenPermanently(toDelete);

                    // someday: would be nice if this could simply be
                    // handled as a traversal on the map: pass down
                    // the list of items to remove, and any parent
                    // that notices one of it's children removes it.
                    
                }
                
                // LWSelection does NOT listen for events among what's selected (an
                // optimization & we don't want the selection updating iself and issuing
                // selection change events AS a delete takes place for each component as
                // it's deleted) -- it only needs to know about deletions, so they're
                // handled special case.  Here, all we need to do is clear the selection as
                // we know everything in it has just been deleted.
            
                selection().clear();
            
            }
            
//             void act(LWSelection s) {
//                 s.removeAncestorSelected();
//                 act(s.iterator());
//                 selection().clear();
//             }
            
//         void act(LWComponent c) {
//             LWContainer parent = c.getParent();
            
//             if (parent == null) {
//                 info("skipping: null parent (already deleted): " + c);
//             } else if (c.isDeleted()) {
//                 info("skipping (already deleted): " + c);
//             } else if (parent.isDeleted()) { // after prior check, this case should be impossible now
//                 info("skipping (parent already deleted): " + c); // parent will call deleteChildPermanently
//             } else if (parent.isSelected()) { // if parent selected, it will delete it's children
//                 info("skipping - parent selected & will be deleting: " + c);
//             } else if (c.isLocked()) {
//                 info("not permitted: " + c);
//             } else if (!canEdit(c)) {
//                 info("cannot edit: " + c);
//             } else {
//                 parent.deleteChildPermanently(c);
//             }
//         }
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
    public static final LWCAction MakeNaturalSize = new LWCAction("Make Natural Size") {
            public void act(LWComponent c) {
                c.setToNaturalSize();
            }
        };
    

    public static final LWCAction ImageToNaturalSize = MakeNaturalSize;
    
    public static final LWCAction AddFileAction = new LWCAction(VueResources.getString("mapViewer.componentMenu.addFile.label")) {
        public void act(LWComponent c) 
        {
        	VueFileChooser chooser = VueFileChooser.getVueFileChooser();        	        	
        	
    		File fileName = null;
    		
            int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
            if (option == VueFileChooser.APPROVE_OPTION) 
            {
                fileName = chooser.getSelectedFile();
                
                if (fileName == null) 
                	return;
                
                if (fileName.exists()) 
           		 	VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());

                if (c instanceof LWNode || c instanceof LWLink)
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
                    VUE.getActiveViewer().getDropFocal().pasteChild(node);
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
            String title = "Add URL to " + ((c instanceof LWNode ) ? "Node" : "Link");
            javax.swing.JDialog dialog = optionPane.createDialog((Component)VUE.getApplicationFrame(), title);
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
                    	if (c instanceof LWNode || c instanceof LWLink)
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
                            VUE.getActiveViewer().getDropFocal().dropChild(node);
                            
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
            final LWComponent focal = VUE.getActiveFocal();
            
            if (focal instanceof LWSlide || focal instanceof MasterSlide) {
                
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
            else if (focal instanceof LWGroup) {
                VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
                if (VUE.getActiveMap().getTempBounds() != null)
                    ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
                VUE.getReturnToMapButton().setVisible(false);
            }
        }

        @Override
        public boolean overrideIgnoreAllActions() { return true; }        
    }
    
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
            		
                        // 2008-06-17 SMF: there is currently no "active slide" that is
                        // attended to, so this has never done anything.  Up/down arrows
                        // appear to work fine right now, so just leaving this out for now.
                        // If we find a problem, what we'd need to play with is setting
                        // the active LWComponent.class, not LWSlide.class

                        // update inspectors (optional -- may not actually want to do this, but
                        // currently required if you want up/down arrows to subsequently navigate
                        // the pathway)
            		//VUE.setActive(LWSlide.class, this, slide);
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
        	VUE.getInfoDock().raise();
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
            return ((s.size() - s.count(LWLink.class)) >= 2 && s.allHaveSameParent() && !(VUE.getActiveViewer().getFocal() instanceof LWSlide));
        }
        void act(LWSelection s) {
            if (s.size() == 2 && s.count(LWGroup.class) == 1) {
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
                return s.count(LWGroup.class) > 0 || s.allHaveSameParentOfType(LWGroup.class);
            }
            void act(LWSelection s) {
                final Collection<LWComponent> toSelect = new HashSet(); // ensure no duplicates
                if (s.count(LWGroup.class) > 0) {
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
                        toSelect.addAll(c.getChildren());
                        ((LWGroup)c).disperse();
                    }
                }
            }
        };
    
    public static final LWCAction Rename =
        new LWCAction("Rename", VueUtil.isWindowsPlatform() ? keyStroke(KeyEvent.VK_F2) : keyStroke(KeyEvent.VK_ENTER)) {
        boolean undoable() { return false; } // label editor handles the undo
            
        boolean enabledFor(LWSelection s) {
            return s.size() == 1 && s.first().supportsUserLabel() && !s.first().isLocked();
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

    /** this will toggle the collapsed state flag */
    public static final LWCAction Collapse =
    new LWCAction("Collapse", keyStroke(KeyEvent.VK_X, SHIFT+LEFT_OF_SPACE)) {
        
        @Override void act(LWSelection s) {
            super.act(s);
            // todo: below isn't really good enough: should be happening at the model
            // level -- e.g., undo manager could note if any Hidden or Collapsed events
            // of any kind were seen, and on user action completed, call this on the
            // global selection.
            s.removeHidden();
        }
        @Override void act(LWComponent c) {
            c.setCollapsed(!c.isCollapsed());
        }
    };
    

    private static class Stats {
        float minX, minY;
        float maxX, maxY;
        float centerX, centerY;
        float totalWidth, totalHeight; // added width/height of all in selection
        float maxWide, maxTall; // width of widest, height of tallest
    }
    
    //-------------------------------------------------------
    // Arrange actions
    //
    // todo bug: if items have a stroke width, there is an
    // error in adjustment such that repeated adjustments
    // nudge all the nodes by what looks like half the stroke width!
    // (error occurs even in first adjustment, but easier to notice
    // in follow-ons)
    //-------------------------------------------------------
    public abstract static class ArrangeAction extends LWCAction {
        static float minX, minY;
        static float maxX, maxY;
        static float centerX, centerY;
        static float totalWidth, totalHeight; // added width/height of all in selection
        static float maxWide, maxTall; // width of widest, height of tallest
        static double radiusWide, radiusTall;
        // note: static variables; obviously not thread-safe here
        
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

        public void act(List<? extends LWComponent> bag) {
            act(new LWSelection(bag));
        }
        
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
                    // need to allow for in-group components now.
                    // todo: unexpected behaviour if some in-group and some not?
                    if (c.isManagedLocation())
                        i.remove();
                }
            }

            // TODO: do we need to recompute statistics in the selection?  E.g., links from another
            // layer in selection should be removed. 
            if (selection.allHaveSameParent() || selection.allHaveTopLevelParent())
                ; // we're good
            else
                throw new DeniedException("all must have same or top-level parent");

            if (r == null)
                r = LWMap.getLayoutBounds(selection);

            if (selection.isSized()) {
                r.width = selection.getWidth();
                r.height = selection.getHeight();
            }

            computeStatistics(r, selection);

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

        static protected void computeStatistics(Rectangle2D.Float r, Collection<LWComponent> nodes) {
            
            if (r == null)
                r = LWMap.getLayoutBounds(nodes);

            minX = r.x;
            minY = r.y;
            maxX = r.x + r.width;
            maxY = r.y + r.height;
            centerX = (minX + maxX) / 2;
            centerY = (minY + maxY) / 2;
            totalWidth = totalHeight = 0;
            maxWide = maxTall = 0;

            for (LWComponent c : nodes) {
                totalWidth += c.getWidth();
                totalHeight += c.getHeight();
                if (c.getWidth() > maxWide)
                    maxWide = c.getWidth();
                if (c.getHeight() > maxTall)
                    maxTall = c.getHeight();
            }
        }
        
        void arrange(LWSelection selection) {
            for (LWComponent c : selection)
                arrange(c);
        }
        void arrange(LWComponent c) { throw new RuntimeException("unimplemented arrange action"); }


        protected void clusterNodes(final LWComponent center, final Collection<LWComponent> clustering) {

            computeStatistics(null, clustering);
            centerX = center.getMapCenterX(); // should probably be local center, not map center
            centerY = center.getMapCenterY();
        
            // todo: bump up if there are link labels to make room for
            // also, vertical diameter should be enough to stack half the nodes (half of totalHeight) vertically
            // add an analyize to ArrangeAction which we can use here to re-compute on the new set of linked nodes
            //radiusWide = center.getWidth() / 2 + maxWide / 2 + 50;
//             radiusWide = Math.max(totalWidth/8,  center.getWidth() / 2 + maxWide / 2 + 50);
//             radiusTall = Math.max(totalHeight/8, center.getHeight() / 2 + maxTall / 2 + 50);
            radiusWide = center.getWidth() / 2 + maxWide / 2 + 50;
            radiusTall = center.getHeight() / 2 + maxTall / 2 + 50;
        
            //clusterNodes(centerX, centerY, radiusWide, radiusTall, linked);
            clusterNodes(clustering);
        
        }

        protected void clusterLinked(final LWComponent center) {
            clusterNodes(center, center.getLinked());
        }
        
        
        //private static void clusterNodes(float centerX, float centerY, double radiusWide, double radiusTall, Collection<LWComponent> nodes)
        // todo: smarter algorithm that lays out concentric rings, with more nodes in each larger ring (compute ellipse circumference);
        // tricky: either need a good guess at the number of rings, or just leave the last ring far more spread out (remainder nodes will
        // be left for the last right
        protected void clusterNodes(Collection<LWComponent> nodes)
        {
            // todo: if a link-chain detected, lay out in link-order e.g., start
            // with any non-linked nodes, then find any with one link (into our
            // set), and then follow the link chain laying out any nodes found in
            // our selection first (removing them from the layout list), then
            // continue to the next node, etc.  Also, can prefer link directionality
            // if there are arrow heads.
        
            final double slice = (Math.PI * 2) / nodes.size();
            int i = 0;

            final int maxTierSize = 20;
            final int tiers = nodes.size() / maxTierSize;
            //final int tiers = 3;
        
                
            for (LWComponent c : nodes) {
                // We add Math.PI/2*3 (270 degrees) so the "clock" always starts at the top -- so something
                // is always is laid out at exactly the 12 o'clock position
                final double angle = Math.PI/2*3 + slice * i;

                if (false && nodes.size() > 200) {
                    // random layout
                    double rand = Math.random()+.1;
                    c.setCenterAt(centerX + radiusWide * rand * Math.cos(angle),
                                  centerY + radiusTall * rand * Math.sin(angle));

                } else if (nodes.size() > maxTierSize) {
                    // tiered circular layout -- begins to spiral beyond 2 tiers
                    final int tier = i % tiers;
                    final double factor = 1 + tier * 0.33;
                    final double rwide = radiusWide * factor;
                    final double rtall = radiusTall * factor;
//                     final double rwide = (radiusWide / tiers) * (tier+1);
//                     final double rtall = (radiusTall / tiers) * (tier+1);
                    c.setCenterAt(centerX + rwide * Math.cos(angle),
                                  centerY + rtall * Math.sin(angle));
//                          if (tier == 0) c.setFillColor(Color.magenta);
//                     else if (tier == 1) c.setFillColor(Color.red);
//                     else if (tier == 2) c.setFillColor(Color.green);
//                     else if (tier == 3) c.setFillColor(Color.blue);
                } else {

                    // circular layout
                    c.setCenterAt(centerX + radiusWide * Math.cos(angle),
                                  centerY + radiusTall * Math.sin(angle));
                }

                i++;
                    
            }
        
        }
        
        
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
        final int osdx, osdy; // on-screen delta-x, delta-y
        NudgeAction(int dx, int dy, String name, KeyStroke stroke) {
            super(name, stroke);
            osdx = dx;
            osdy = dy;
        }

        public static boolean enabledOn(LWSelection s) {
            return s.size() > 0
                && s.first().isMoveable()
                && VUE.getActiveViewer().isFocusOwner()
                && !(VUE.getActiveSubTool() instanceof tufts.vue.SelectionTool.Browse)
                ;
        }
        
        @Override
        boolean enabledFor(LWSelection s) {
            return enabledOn(s);
        }

        @Override
        void act(LWComponent c) { nudgeOrReorder(c, osdx, osdy); }
        
        private void nudgeOrReorder(LWComponent c, int x, int y) {

//             if (VUE.getActiveSubTool() instanceof tufts.vue.SelectionTool.Browse) {
//                 Log.debug("nudge disabled during browse");
//                 return;
//             }
            
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

    private static final int PUSH_DISTANCE = 24;

    private static boolean enabledForPushPull(LWSelection s) {
            return s.size() == 1
                && !(s.first() instanceof LWLink) // links giving us trouble
                && s.first().getParent() instanceof LWMap.Layer; // don't allow pushing inside slides, nodes / anything
    }
    
    
    public static final LWCAction PushOut =
    new LWCAction("Push Out", keyStroke(KeyEvent.VK_EQUALS, ALT)) {
        
        boolean enabledFor(LWSelection s) {
            return enabledForPushPull(s);
        }
        // todo: for selection size > 1, push on bounding box
        void act(LWComponent c) {
            // although we don't currently want to support pushing inside anything other than
            // a layer, this generic call would handle other cases if we can support them
            projectNodes(c, PUSH_DISTANCE);
            // currenly only pushes within a single layer: provide the map
            // as the focal if want to push in all layers
            //pushNodes(viewer().getDropFocal(), c); // push in active focal: will work for slides also
            // active focal should normally be a layer otherwise
            // ideally would ask the node for it's layer, as theoretically we could be dropping into
            // one layer then push in another
            //pushNearbyNodes(viewer().getMap(), c);
            //pushNearbyNodes(viewer().getDropFocal(), c);

        }
    };
    
    public static final LWCAction PullIn =
        new LWCAction("Pull In", keyStroke(KeyEvent.VK_MINUS, ALT)) {
            boolean enabledFor(LWSelection s) {
                return enabledForPushPull(s);
            }
            void act(LWComponent c) {
                projectNodes(c, -PUSH_DISTANCE);
                
            }
        };
        
        private static final boolean DEBUG_PUSH = false;
    
    private static void projectNodes(final LWComponent pushing, final int distance) {

        Collection<LWComponent> toPush = null;

        if (distance < 0 && pushing.hasLinks())
            toPush = pushing.getLinked();

        if (toPush == null || toPush.size() == 0)
            toPush = pushing.getParent().getChildren();
        
        projectNodes(toPush, pushing, distance);
    }
        
        
        // todo: combine into a Geometry.java with computeIntersection, computeConnector, projectPoint code from VueUtil
        // todo: to handle pushing inside slides, we'd need to get rid of the references to map bounds,
        // and always use local bounds
        private static void projectNodes(final Iterable<LWComponent> toPush, final LWComponent pushing, final int distance)
        {
            final Point2D.Float groundZero = new Point2D.Float(pushing.getMapCenterX(),
                                                               pushing.getMapCenterY());
            //final Rectangle2D pushingRect = pushing.getMapBounds();
            final RectangularShape pushingShape = pushing.getMapShape();

            final java.util.List<LWComponent> links = new java.util.ArrayList();
            final java.util.List<LWComponent> nodes = new java.util.ArrayList();
        
            for (LWComponent node : toPush) {

                if (node == pushing)
                    continue;
                
                if (node.isManagedLocation())
                    continue;
                
                if (node instanceof LWLink) {
                    LWLink link = (LWLink) node;
                    if (link.isConnected() || link.isCurved()) // both cases are buggy right now
                        continue;
                }

                final Line2D.Float connector = new Line2D.Float();
                final boolean overlap = VueUtil.computeConnectorAndCenterHit(pushing, node, connector);
                //VueUtil.computeConnector(pushing, node, connector);

                final Point2D newCenter;
            
                float adjust = distance;

                //final boolean intersects = node.intersects(pushingRect); // problems w/slide icons
                //final boolean intersects = pushingRect.intersects(node.getMapBounds());
                final boolean intersects = pushingShape.intersects(node.getMapBounds());

                final boolean moveToEdge = overlap || intersects;

                if (false && DEBUG_PUSH) {
                    // create a detached link from center of pushing to edge of each pushed to show vectors
                    LWLink link = new LWLink();
                    link.setHeadPoint(connector.getP1());
                    link.setTailPoint(connector.getP2());
                    link.setArrowState(LWLink.ARROW_TAIL);
                    link.setNotes("head: " + pushing + "\ntail: " + node);
                    links.add(link);
                }
            
                if (moveToEdge) {

                    if (distance < 0) // do nothing further if pulling on
                         continue;
                
                    // If overlapping, we want to move the node along a line away from
                    // the center of the pushing node until it no longer overlaps.  As
                    // part of this process, we compute the point at the edge of the
                    // pushing node that the overlapping node would be at if all we were
                    // going to do was move it to the edge.  This isn't strictly needed
                    // to produce the end result (we could start iterating immediately,
                    // we don't need to start at the intersect), but it's useful for
                    // debugging, and it may be a useful location to know for future
                    // tweaks to this code.
                
                    // first, find a point along the line from center of pushing to the center of node
                    // that we know is outside of the pushing node
                    final Point2D farOut = VueUtil.projectPoint(groundZero, connector, Short.MAX_VALUE);
                    // now produce a ray that shoots from that point back to the center of the pushing node
                    final Line2D.Float testRay = new Line2D.Float(farOut, groundZero);
                    // now find the point at the edge of the pushing node that the ray intersects it
                    final Point2D.Float intersect = VueUtil.computeIntersection(testRay, pushing);

                    // now project the node along the connector line from the intersect
                    // by small increments until the node no longer overlaps the
                    // pushing node
                        
                    for (int i = 0; i < 1000; i++) {
                        node.setCenterAt(VueUtil.projectPoint(intersect, connector, i * 2f));
//                         if (!node.intersects(pushingRect)) // problems w/slide icons
//                             break;
                        if (!pushingShape.intersects(node.getMapBounds()))
                            break;
                        if (DEBUG_PUSH) Log.debug("PUSH ITER " + i + " on " + node);
                    }

                    adjust /= 2; // we'll only push half the standard amount from here
                }

                newCenter = VueUtil.projectPoint(node.getMapCenterX(), node.getMapCenterY(), connector, adjust);

                if (DEBUG_PUSH) {
                    float dist = (float) connector.getP1().distance(connector.getP2());
                    String notes = String.format("distance: %.1f\nadjust: %.1f\n-center: %s\n+center: %s\nconnect: %s",
                                                 dist,
                                                 adjust,
                                                 Util.fmt(node.getMapCenter()),
                                                 Util.fmt(newCenter),
                                                 Util.fmt(connector)
                                                 );


                    if (intersects) notes += "\nINTERSECTS";
                    if (overlap) notes += "\nOVERLAP";

                    final LWComponent n;

                    if (false) {
                        n = node.duplicate();
                        node.setNotes(notes);
                        nodes.add(n);
                        n.setStrokeWidth(1);
                    } else
                        n = node;
                
                    if (moveToEdge) {
                        n.setTextColor(java.awt.Color.red);
                        n.mFontStyle.set(java.awt.Font.BOLD);
                    }
                    n.setNotes(notes);
                    n.setCenterAt(newCenter);
                } else {
                    node.setCenterAt(newCenter);
                }
            
            
            }

            if (DEBUG_PUSH) {
                pushing.getMap().sendToBack(pushing);
                pushing.getMap().addChildren(nodes);
                pushing.getMap().addChildren(links);
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
    
    public static final ArrangeAction MakeCluster = new ArrangeAction("Make Cluster", keyStroke(KeyEvent.VK_PERIOD, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            
            void arrange(LWSelection selection) {

                final double radiusWide, radiusTall;
                
                if (selection.size() == 1) {

                    // if a single item in selection, arrange all nodes linked to it in a circle around it                    

                    final LWComponent center = selection.first();
                    final Collection<LWComponent> linked = center.getLinked();

                    clusterNodes(center, linked);
                    
                    selection().setTo(center);
                    selection().add(linked);
                    
                } else {
                    
//                     radiusWide = (maxX - minX) / 2;
//                     radiusTall = (maxY - minY) / 2;
                    
//                     radiusWide = Math.max((maxX - minX) / 2, maxWide);
//                     radiusTall = Math.max((maxY - minY) / 2, maxTall);
                    
                    radiusWide = Math.max((maxX - minX) / 2, totalWidth/4);
                    radiusTall = Math.max((maxY - minY) / 2, totalHeight/4);
                    
                    //clusterNodes(centerX, centerY, radiusWide, radiusTall, selection);
                    clusterNodes(selection);

                    // The ring will expand on subsequent MakeCircle calls, because nodes are laid
                    // out on the ring on-center, but the bounds used to create the initial ring
                    // form the the top of the north-most mode to the bottom of the south-most node
                    // (same for east/west), which on the next call will be a bigger ring.  Would
                    // be hairy trying to figure out the the ring size that would contain the given
                    // nodes inside a given rectangle when laid-out on-center. [ Actually, would
                    // just computing the on-center bounds work? Better, but only perfectly if
                    // there's a node at exaclty N/S/E/W on the dial, and the ring-order (currently
                    // selection order, which is usually stacking order) hasn't changed.] If we
                    // want such functionality, would be better handled via a persistent "ring"
                    // layout object (like a group), that maintains a persistant, selectable oval
                    // that can be resized directly -- the bounding box would only be used for
                    // picking the initial size.

                }
            }
            
    };


    public static final LWCAction MakeDataClusters = new ArrangeAction("Make Data Clusters", keyStroke(KeyEvent.VK_SLASH, ALT)) {
            @Override
            public void arrange(LWComponent c) {
                if (c instanceof LWNode && !c.hasClientData(tufts.vue.ds.Schema.class))
                    clusterLinked(c);
            }
        };

    public static final LWCAction MakeDataLists = new ArrangeAction("Make Data Lists", keyStroke(KeyEvent.VK_COMMA, ALT)) {
            boolean enabledFor(LWSelection s) { return s.size() > 0; }
            @Override
            public void arrange(LWComponent c) {
                if (c instanceof LWNode && !c.hasClientData(tufts.vue.ds.Schema.class)) {
                    // grab linked
                    c.addChildren(new ArrayList(c.getLinked()), LWComponent.ADD_MERGE);
                }
            }
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
                final LWComponent[] comps = sortByX(sortByY(selection.asArray()));
                final float layoutRegion = maxX - minX;
                //if (layoutRegion < totalWidth)
                //  layoutRegion = totalWidth;
                final float horizontalGap = (layoutRegion - totalWidth) / (selection.size() - 1);
                float x = minX;
                for (LWComponent c : comps) {
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
        MakeCluster,
        MakeDataClusters,
        MakeDataLists,
        null,
        DistributeVertically,
        DistributeHorizontally,
        null,
        NudgeUp,
        NudgeDown,
        NudgeLeft,
        NudgeRight,
        null,
        PushOut,
        PullIn
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
        protected boolean enabled() { return true; }
        public void act() {
            VUE.displayMap(new LWMap("New Map " + count++));
        }
    };
    public static final Action Revert =
        //new VueAction("Revert", keyStroke(KeyEvent.VK_R, COMMAND+SHIFT), ":general/Revert") { // conflicts w/align centers in row
        //new VueAction("Revert", null, ":general/Revert") {            
        new VueAction("Revert") {
            boolean undoable() { return false; }
            protected boolean enabled() 
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

                // This won't do anything if something deeper in the map is the focal
                //VUE.getActiveMap().notify(this, LWKey.Repaint);
                
                VUE.getActiveFocal().notify(this, LWKey.Repaint);
                
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
    
    public static final VueAction TogglePruning =
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

    public static final VueAction ToggleAutoZoom =
        
        // 'E' chosen for temporary mac shortcut until we find a workaround for not
        // being able to use Alt-Z because it's on the left of the keyboard, and it's
        // not 'W', which if the user accidently hits COMMAND-W, the map will close
        // (todo: see about just changing the Close shortcut entirely or getting rid of
        // it)

        new VueAction("Auto Zoom",
                      (!Util.isMacPlatform() || (DEBUG.Enabled&&DEBUG.KEYS))
                      ? keyStroke(KeyEvent.VK_Z, ALT)
                      : keyStroke(KeyEvent.VK_E, COMMAND+SHIFT) // can only get away witl COMMAND root modifier for now
                      )
        {
            
            boolean state = edu.tufts.vue.preferences.implementations.AutoZoomPreference.getInstance().isTrue();
            { updateName(); }
            @Override
            public void act() {
                state = !state;
                updateName();
                edu.tufts.vue.preferences.implementations.AutoZoomPreference.getInstance().setValue(Boolean.valueOf(state));
            }
            void updateName() {
                if (DEBUG.Enabled && DEBUG.KEYS && Util.isMacPlatform()) {
                    // workaroud for mac java bug with accelerator glpyhs in JCheckBoxMenuItem's
                    if (state)
                        putValue(NAME, getPermanentActionName() + " (ON)");
                    else
                        putValue(NAME, getPermanentActionName() + " (off)");
                }
            }
            
            @Override
            public Boolean getToggleState() {
                return state;
            }    
            
            public boolean overrideIgnoreAllActions() { return true; }
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
    {
        public LWCAction(String name, String shortDescription, KeyStroke keyStroke, Icon icon) {
            super(name, shortDescription, keyStroke, icon);
            init();
        }
        LWCAction(String name, KeyStroke keyStroke, String iconName) {
            super(name, keyStroke, iconName);
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
        
        public void act() {
            LWSelection selection = selection();
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
                Log.error(getActionName() + ": Not enabled given this selection: " + selection);
            }
        }


//         public void fire(InputEvent e, LWComponent c) {
//             actionPerformed(new ActionEvent(source, 0, name));
//         }
        

        ///** option initialization code called at end of constructor */
        private void init() {
            //VUE.getSelection().addListener(this);
        }

        
        @Override
        protected boolean isSelectionWatcher() { return true; }


        /** @return true -- the default for LWCAction's */
        @Override
        boolean isEditAction() {
            return true;
        }

        @Override
        protected boolean enabled() { return VUE.getActiveViewer() != null && enabledFor(selection()); }
        
//         public void selectionChanged(LWSelection selection) {
//             if (VUE.getActiveViewer() == null)
//                 setEnabled(false);
//             else
//                 setEnabled(enabledFor(selection));
//         }
//         void checkEnabled() {
//             selectionChanged(VUE.getSelection());
//         }
        
        void checkEnabled() {
            //selectionChanged(VUE.getSelection());
            updateEnabled(selection());
        }
        
        protected void updateEnabled(LWSelection selection) {
            if (selection == null)
                setEnabled(false);
            else
                setEnabled(enabledFor(selection));
        }
        
        
        /** Is this action enabled given this selection? */
        @Override
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
        
//         /** hierarchicalAction: any children in selection who's
//          * parent is also in the selection are ignore during
//          * iterator -- for actions such as delete where deleting
//          * the parent will automatically delete any children.
//          */
//         boolean hierarchicalAction() { return false; }
        
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
        void act(Iterator<LWComponent> i) {
            while (i.hasNext()) {
                LWComponent c = i.next();
//                 if (hierarchicalAction() && c.isAncestorSelected()) {
//                     // If has no parent, must already have been acted on to get that way.
//                     // If parent is selected, action will happen via it's parent.
//                     continue;
//                 }
                act(c);
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
                if (DEBUG.SELECTION) Log.debug("LWCAction: ignoring " + getActionName() + " on " + c);
            
        }
        
        void act(LWLink c) { ignoredDebug(c); }
        void act(LWNode c) { ignoredDebug(c); }
        void act(LWImage c) { ignoredDebug(c); }
        void act(LWSlide c) { ignoredDebug(c); }

        private void ignoredDebug(LWComponent c) {
            if (DEBUG.Enabled) Log.debug("LWCAction: ignoring " + getActionName() + " on " + c);
            //if (DEBUG.SELECTION) System.out.println("LWCAction: ignoring " + getActionName() + " on " + c);
        }
        
        void actOn(LWComponent c) { act(c); } // for manual init calls from internal code

        @Override
        public String getUndoName(ActionEvent e, Throwable exception)
        {
            String name = super.getUndoName(e, exception);
            if (selection().size() == 1)
                name += " (" + selection().first().getComponentTypeLabel() + ")";
            return name;
    }
        
        
        //public String toString() { return "LWCAction[" + getActionName() + "]"; }
    }
    
}