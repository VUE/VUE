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

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.net.URLEncoder;
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

import org.apache.commons.lang.ArrayUtils;

import tufts.Util;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.LWComponent.Flag;
import tufts.vue.LWComponent.HideCause;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.gui.DeleteSlideDialog;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.FullScreen;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.renderer.SearchResultTableModel;
import edu.tufts.vue.metadata.MetadataList;
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

    public static final String MENU_INDENT = "  ";
    
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
    	new VueAction(VueResources.local("menu.file.nodenotes")) {
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
			VueUtil.alert(null,VueResources.local("presentationNotes.invalidPresentation.message"), VueResources.local("presentationNotes.invalidPathway.title"));
			return null;
    	}
		VueFileChooser chooser = VueFileChooser.getVueFileChooser();

		File pdfFileName = null;
		chooser.setDialogTitle(VueResources.local("dialog.title.saveaspdf"));
		
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
                int n = VueUtil.confirm(null, VueResources.local("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                        VueResources.local("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                      
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
			VueUtil.alert(null,VueResources.local("dialog.activemap.message"), VueResources.local("dialog.activemap.title"));
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
		chooser.setDialogTitle(VueResources.local("dialog.title.saveaspdf"));
        int option = chooser.showSaveDialog(tufts.vue.VUE.getDialogParent());
        if (option == VueFileChooser.APPROVE_OPTION) 
        {
            pdfFileName = chooser.getSelectedFile();

            if (pdfFileName == null) 
            	return null;

            if(!pdfFileName.getName().endsWith(".pdf")) 
            	pdfFileName = new File(pdfFileName.getAbsoluteFile()+".pdf");                	
            
            if (pdfFileName.exists()) {
                int n = VueUtil.confirm(null, VueResources.local("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                        VueResources.local("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                      
            }           
            
            return pdfFileName;
        }
        else
        	return null;
		
    }
      /*     
    public static final VueAction ZoteroAction =
    	new VueAction("Import Zotero collection") {
		public void act() 
		{			
			VueFileChooser chooser = VueFileChooser.getVueFileChooser();
			File zoteroFile = null;
										
	        int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
	        if (option == VueFileChooser.APPROVE_OPTION) 
	        {
	            zoteroFile = chooser.getSelectedFile();
	           
				edu.tufts.vue.zotero.ZoteroAction.importZotero(zoteroFile);
	        }

		}
    };
*/
    public static final VueAction SpeakerNotes1 =
    	new VueAction(VueResources.local("menu.file.exporthandout.speakernotes1")) {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createSpeakerNotes1PerPage(pdfFile);
		}
    };
    public static final VueAction SpeakerNotes4 =
    	new VueAction(VueResources.local("menu.file.exporthandout.speakernotes4")) {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createSpeakerNotes4PerPage(pdfFile);
		}
    };
    public static final VueAction NodeNotes4 =
    	new VueAction(VueResources.local("menu.file.exporthandout.nodenotes4")) {
		public void act() 
		{
			File pdfFile = getFileForActiveMap("Node_Notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createNodeNotes4PerPage(pdfFile);
		}
    };
    public static final VueAction SpeakerNotesOutline =
    	new VueAction(VueResources.local("menu.file.exporthandout.speakernotesoutline")) {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Speaker notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createOutline(pdfFile);
		}
    };
    public static final VueAction Slides8PerPage =
    	new VueAction(VueResources.local("menu.file.exporthandout.slides8perpage")) {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Slides");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createPresentationNotes8PerPage(pdfFile);
		}
    };
    

    public static final VueAction AudienceNotes =
    	new VueAction(VueResources.local("menu.file.exporthandout.audiencenotes")) {
		public void act() 
		{
			File pdfFile = getFileForPresentation("Audience notes");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createAudienceNotes(pdfFile);
		}
    };
    

    public static final VueAction FullPageSlideNotes =
    	new VueAction(VueResources.local("menu.file.exporthandout.fullpageslidenotes")) {
		public void act() 
		{			
			File pdfFile = getFileForPresentation("Slides");
			if (pdfFile != null)
				tufts.vue.PresentationNotes.createPresentationSlidesDeck(pdfFile);
		}
    };
    
    public static final VueAction MapAsPDF =
    	new VueAction(VueResources.local("menu.file.exporthandout.mapaspdf")) {
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
    		   putValue(NAME, VueResources.local("menu.edit.preferences"));
               putValue(SHORT_DESCRIPTION, VueResources.local("menu.edit.preferences"));            
               putValue(ACCELERATOR_KEY, keyStroke(KeyEvent.VK_COMMA, COMMAND));
    		
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	PreferencesDialog dialog = new PreferencesDialog(null, VueResources.local("menu.edit.preferences"),
				      edu.tufts.vue.preferences.PreferencesManager.class, true, null, false);
			dialog.setVisible(true);
        }
    };
    //-------------------------------------------------------
    // Selection actions
    //-------------------------------------------------------
    
    public static final Action Preferences = new PreferenceAction(); 
    	
    public static final Action SelectAll =
    new VueAction(VueResources.local("menu.edit.selectall"), keyStroke(KeyEvent.VK_A, COMMAND)) {
        public void act() {
            selection().setTo(focal().getAllDescendents(ChildKind.EDITABLE));
        }
    };
    
    public static final Action SelectAllLinks =
        new VueAction(VueResources.local("menu.edit.selectlink")) {
            public void act() {
                selection().setTo(focal().getDescendentsOfType(ChildKind.EDITABLE, LWLink.class));
            }
        };

     public static final Action SelectAllNodes =
        new VueAction(VueResources.local("menu.edit.selectnodes")) {
            public void act() {
            	selection().setTo(focal().getDescendentsOfType(ChildKind.EDITABLE, LWNode.class));
            }
        };
                
    public static final Action DeselectAll =
    new LWCAction(VueResources.local("menu.edit.deselectall"), keyStroke(KeyEvent.VK_A, SHIFT+COMMAND)) {
        boolean enabledFor(LWSelection s) { return s.size() > 0; }
        public void act() {
            selection().clear();
        }
    };

    public static final Action Reselect =
        new VueAction(VueResources.local("menu.edit.reselect"), keyStroke(KeyEvent.VK_R, COMMAND)) {
            public void act() {
                selection().reselect();
            }
        };

    public static final LWCAction ExpandSelection =
        new LWCAction(VueResources.local("menu.edit.expandselection"), keyStroke(KeyEvent.VK_SLASH, COMMAND)) {
            public void act() {
            	VUE.getInteractionToolsPanel().doExpand();
            }
            boolean enabledFor(LWSelection s) {
                return s.size() > 0 && VUE.getInteractionToolsPanel().canExpand();
            }
        };

    public static final LWCAction ShrinkSelection =
        new LWCAction(VueResources.local("menu.edit.shrinkselection"), keyStroke(KeyEvent.VK_PERIOD, COMMAND)) {
            public void act() {
            	VUE.getInteractionToolsPanel().doShrink();
            }
            boolean enabledFor(LWSelection s) {
                return s.size() > 0 && VUE.getInteractionToolsPanel().canShrink();
            }
        };

    public static final Action AddPathwayItem =
    new LWCAction(VueResources.local("actions.addPathwayItem.label")) {
        @Override public void act(LWSelection s) {
            LWPathway pathway = VUE.getActivePathway();
            if (!pathway.isOpen())
                pathway.setOpen(true);
            LWComponent[] sorted = s.asArray();
            java.util.Arrays.sort(sorted, LWComponent.GridSorter);
            VUE.getActivePathway().add(Util.asList(sorted).iterator());
            GUI.makeVisibleOnScreen(VUE.getActiveViewer(), PathwayPanel.class);
        }
//         public void act(Iterator i) {
//         	LWPathway pathway = VUE.getActivePathway();
//         	if (!pathway.isOpen())
//         		pathway.setOpen(true);
//             VUE.getActivePathway().add(i);
//             GUI.makeVisibleOnScreen(VUE.getActiveViewer(), PathwayPanel.class);
//         }
        boolean enabledFor(LWSelection s) {
            // items can be added to pathway as many times as you want
            return VUE.getActivePathway() != null && s.size() > 0;
        }
    };
    
    public static final Action RemovePathwayItem =
    new LWCAction(VueResources.local("actions.removePathwayItem.label")) {
        public void act(Iterator i) {
            VUE.getActivePathway().remove(i);
        }
        boolean enabledFor(LWSelection s) {
            LWPathway p = VUE.getActivePathway();
            return p != null && s.size() > 0 && (s.size() > 1 || p.contains(s.first()));
        }
    };
    
    public static final Action AddResource =
        new VueAction(VueResources.local("action.addresource")) {
            public void act() {
            	
            	DataSourceViewer.getAddLibraryAction().actionPerformed(null);
            	GUI.makeVisibleOnScreen(this, VUE.getContentDock().getClass());
                VUE.getContentPanel().showResourcesTab();
            }                       
        };
        
    public static final Action UpdateResource =
    new VueAction(VueResources.local("action.updateresource")) {
        public void act() {
        	
        	DataSourceViewer.getUpdateLibraryAction().actionPerformed(null);
        	GUI.makeVisibleOnScreen(this, VUE.getContentDock().getClass());
            VUE.getContentPanel().showResourcesTab();
        }
    };

    public static final Action SearchFilterAction =
        new VueAction(VueResources.local("action.search")) {
    		
            public void act() {             	
                VUE.getMetadataSearchMainGUI().setVisible(true);   
               
                
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
    new LWCAction(VueResources.local("action.hierarchyview")) {
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
        new LWCAction(VueResources.local("action.inviewer")) {
            public void act(Iterator i) {
                GUI.makeVisibleOnScreen(VUE.getActiveViewer(), tufts.vue.ui.SlideViewer.class);                
            }
            boolean enabledFor(LWSelection s) {
                return s.size() == 1 && s.first() instanceof LWSlide;
            }
        };
    
     public static final Action MasterSlide = new VueAction(VueResources.local("action.masterslide"))
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
            new LWCAction(VueResources.local("menu.pathways.editslide")) {
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

    public static void startPresentation(final LWPathway pathway, final Object source)
    {
        VUE.setActive(LWPathway.class, source, pathway);

        // TODO: we should be able to start the pre-cache from
        // PresentationTool.startPresentation(), but currently the map does a
        // full-repaint on the full-screen viewer before we load the new focal for the
        // first item in the presentation.  We should do this in such a way that the
        // entire map does NOT paint on the full-screen viewer before the presentation
        // starts.
        
        if (pathway != null && !Images.lowMemoryConditions()) {
            // If running really low on memory, this might make a presentation worse.
            pathway.preCacheContent();
        }
        
        final PresentationTool presTool = PresentationTool.getTool();

// We ideally want to do this first, so we don't seen a full-screen paint of the entire map,
// but it's causing some problem when the presentation exits, where it leaves the viewer
// at the last focal, instead of back out to the map.        
//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             presTool.startPresentation();
//         }});

        final LWSelection savedSelection = VUE.getSelection().clone();
        // activating the presentation tool is going to clear the selection,
        // so we need to save it here and then pass it to startPresentation.
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            VUE.toggleFullScreen(true);
        }});
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            //VueToolbarController.getController().setSelectedTool(presTool);
            VUE.setActive(VueTool.class, source, presTool);
        }});
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            presTool.startPresentation(savedSelection);
        }});
    }
                
    
    public static final VueAction LaunchPresentation = new VueAction(VueResources.local("action.preview")) {
            @Override public void act() {
                startPresentation(VUE.getActivePathway(), this);
            }
            @Override public boolean overrideIgnoreAllActions() { return true; }        
            
        };
            
            public static final Action DeleteSlide = new VueAction(VueResources.local("action.delete"))
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
        new LWCAction(VueResources.local("menu.format.link.straight"), VueResources.getIcon("link.style.straight")) {
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
        new LWCAction(VueResources.local("menu.format.link.curved"), VueResources.getIcon("link.style.curved")) {
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
        new LWCAction(VueResources.local("menu.format.link.scurved"), VueResources.getIcon("link.style.s-curved")) {
            void init() { putValue("property.value", new Integer(2)); }
            boolean enabledFor(LWSelection s) {
                if (!s.containsType(LWLink.class))
                    return false;
                return s.size() == 1 ? ((LWLink)s.first()).getControlCount() != 2 : true;
            }
            public void act(LWLink c) { c.setControlCount(2); }
        };
    public static final Action LinkArrows =
        new LWCAction(VueResources.local("menu.format.link.arrow"), keyStroke(KeyEvent.VK_L, COMMAND)/*, VueResources.getIcon("outlineIcon.link")*/) {
            boolean enabledFor(LWSelection s) { return s.containsType(LWLink.class); }
            public void act(LWLink c) { c.rotateArrowState(); }
        };
    
        public static final Action ResizeNode =
            new LWCAction(VueResources.local("menu.format.node.resize")/*, VueResources.getIcon("outlineIcon.link")*/) {
                boolean enabledFor(LWSelection s) { 
                
                	
                	
                	if (s.size()==1 && s.containsType(LWNode.class))
                	{
                		LWNode n = (LWNode)s.get(0);
                		Size minSize = n.getMinimumSize();
                		
                		if (minSize.height == n.height && minSize.width == n.width)
                			return false;
                		else
                			return true;

                	}
                	else 
                		return false;
                	}
                public void act(LWNode c) { c.setToNaturalSize();}
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
    new LWCAction(VueResources.local("action.setautosized")) {
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
    private static LWComponent StyleBuffer; // this holds the style copied by "Copy Style"
    
    private static final LWComponent.CopyContext CopyContext = new LWComponent.CopyContext(new LWComponent.LinkPatcher(), true);
    private static final List<LWComponent> DupeList = new ArrayList(); // cache for dupe'd items
    
    private static final int CopyOffset = 10;
    
    private static final boolean RECORD_OLD_PARENT = true; // not a flag: a constant for readability
    private static final boolean SORT_BY_Z_ORDER = true; // not a flag: a constant for readability
    
    public static List<LWComponent> duplicatePreservingLinks(Collection<LWComponent> items) {
        return duplicatePreservingLinks(items, !RECORD_OLD_PARENT, !SORT_BY_Z_ORDER);
    }

    /**
     * @param recordOldParent - if true, the old parent will be stored in the copy as a clientProperty
     * @param sortByZOrder - if true, will maintain the relative z-order of components in the dupe-set
     *
     * note: preserving old parents has different effect when duplicating the ScratchBuffer -- it will copy over old-parent client data
     * that was collected when the ScratchBuffer was loaded
     */
    public static List<LWComponent> duplicatePreservingLinks
        (final Collection<LWComponent> items,
         boolean recordOldParent,
         final boolean sortByZOrder)
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
            if (recordOldParent) {
                // note check for the special instance of ScratchBuffer as the input Collection to this method:
                if (items == ScratchBuffer) {
                    // parent will be null -- copy over the client data we stored when loading the ScratchBuffer
                    copy.setClientData(LWKey.OLD_PARENT, c.getClientData(LWKey.OLD_PARENT));
                } else {
                    // we could store this as the actual parent, but if we do that,
                    // changes made to the components before they're fully baked
                    // (e.g., link reconnections, translations) will generate events
                    // that will confuse the UndoManager.
                    copy.setClientData(LWKey.OLD_PARENT, c.parent);
                }
                // Note: forcing the addition of client-data (a HashMap) on every
                // component during a duplicate/cut/paste just to store the old parent
                // is a bit expensive given how little we currently use
                // LWComponent.clientData.
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
    new LWCAction(VueResources.local("menu.edit.duplicate"), keyStroke(KeyEvent.VK_D, COMMAND)) {
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

            final LWContainer parent0 = dupes.get(0).getClientData(LWKey.OLD_PARENT);
            boolean allHaveSameParent = true;

            // dupes may have fewer items in it that the selection: it will only
            // contain the top-level items duplicated -- not any of their children

            for (LWComponent copy : dupes) {
                if (copy.getClientData(LWKey.OLD_PARENT) != parent0)
                    allHaveSameParent = false;
                copy.translate(CopyOffset, CopyOffset);
            }

            //-----------------------------------------------------------------------------
            // Add the newly duplicated items to the appropriate new parent
            //-----------------------------------------------------------------------------

            if (allHaveSameParent) {
                parent0.addChildren(dupes, LWComponent.ADD_PASTE);
            } else {
                // Todo: would be smoother to collect all the nodes by parent
                // and do a separate collective adds for each parent
                for (LWComponent copy : dupes) 
                    copy.getClientData(LWKey.OLD_PARENT).pasteChild(copy);
                    
            }

            //-----------------------------------------------------------------------------
            
            // clear out old parent references now that we're done with them
            for (LWComponent copy : dupes) {
                //copy.flushAllClientData(); // start entirely fresh
                copy.setClientData(LWKey.OLD_PARENT, null);
            }

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
    new LWCAction(VueResources.local("menu.edit.copy"), keyStroke(KeyEvent.VK_C, COMMAND)) {
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        void act(LWSelection selection) {
            ScratchBuffer.clear();
            // always record old parent when loading the ScratchBuffer -- the client data
            // that gets added never needs to be cleared, as client data isn't copied
            // when an individual LWComponent is duplicated, and once a LWComponent is in the
            // ScratchBuffer, that instance will never appear anywhere in a map -- it's only
            // used as a duplicating source.
            ScratchBuffer.addAll(duplicatePreservingLinks(selection, RECORD_OLD_PARENT, SORT_BY_Z_ORDER));
            
            // Enable if want to use system clipboard.  FYI: the clip board manager
            // will immediately grab all the data available from the transferrable
            // to cache in the system.
            //Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            //clipboard.setContents(VUE.getActiveViewer().getTransferableSelection(), null);
            
        }
    };

    public static final VueAction Paste =
    new VueAction(VueResources.local("menu.edit.paste"), keyStroke(KeyEvent.VK_V, COMMAND)) {
        //public boolean isEnabled() //would need to listen for scratch buffer fills
        
        private Point2D.Float lastMouseLocation;
        private Point2D.Float lastPasteLocation;
        public void act() {
            
            final MapViewer viewer = viewer();
            // note: preserving old parents has different effect when duplicating the ScratchBuffer -- it will copy over old-parent client data
            final List<LWComponent> pasted = duplicatePreservingLinks(ScratchBuffer, RECORD_OLD_PARENT, !SORT_BY_Z_ORDER);
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

            final LWComponent newParent = viewer.getDropFocal();

            if (newParent instanceof LWSlide) {
                // When pasting content from one slide to another slide, keep the relative x/y
                // position of the content from the old slide
                
                //final List<LWComponent> pasteToOldLocations = new ArrayList();
                final List<LWComponent> pasteToNewLocations = new ArrayList();
                for (LWComponent c : pasted) {
                    final LWContainer oldParent = c.getClientData(LWKey.OLD_PARENT);
                    if (oldParent instanceof LWSlide && oldParent != newParent) {
                        // if old parent was a slide (a different one), leave it's x/y alone when pasting
                        //pasteToOldLocations.add(c);
                        // don't actually need to record these
                    } else {
                        pasteToNewLocations.add(c);
                    }
                }
                if (pasteToNewLocations.size() > 0)
                    MapDropTarget.setCenterAt(pasteToNewLocations, pasteLocation);
            } else {
                MapDropTarget.setCenterAt(pasted, pasteLocation); // note: this method only works on un-parented nodes
            }
            
            newParent.addChildren(pasted, LWComponent.ADD_PASTE);

            for (LWComponent c : pasted) {
                //c.flushAllClientData(); // start entirely fresh
                c.setClientData(LWKey.OLD_PARENT, null); // clear out any old-parent client data
            }
            
            selection().setTo(pasted);
            lastMouseLocation = mouseLocation;
        }

        
        // stub code for if we want to start using the system clipboard for cut/paste
        void act_system() {
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            VUE.getActiveViewer().getMapDropTarget().processTransferable(clipboard.getContents(this), null);
        }
        
    };
    
    public static final Action Cut =
    new LWCAction(VueResources.local("menu.edit.cut"), keyStroke(KeyEvent.VK_X, COMMAND)) {
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
        new LWCAction(VueResources.local("menu.edit.delete"), keyStroke(KeyEvent.VK_BACK_SPACE), ":general/Delete") {
      //new LWCAction(VueResources.local("menu.edit.delete"), keyStroke(KeyEvent.VK_DELETE), ":general/Delete") {
            
            // OLD: [We could use BACK_SPACE instead of DELETE because that key is bigger, and
            // on the mac it's actually LABELED "delete", even tho it sends BACK_SPACE.
            // BUT, if we use backspace, trying to use it in a text field in, say the
            // object inspector panel causes it to delete the selection instead of
            // backing up a char...  The MapViewer special-case handles both anyway as a
            // backup.]

            // Update, SMF Oct 2009: BACK_SPACE is the default key-code when hitting "delete" on a
            // laptop, so it's better to use BACK_SPACE here.  Again, the MapViewer directly
            // handles both keys in special-case code so we don't need to worry about what happens
            // when the MapViewer has focus.  The issue with hitting the "delete" key in text fields
            // triggering the global Delete action appear to have gone away.  Changing this to
            // BACK_SPACE now is allowing the use of hitting "delete" after clicking on nodes in
            // the DataTree -- the unconsumed KeyPress is relayed through the FocusManager to the
            // VueMenuBar which then can correctly recognize the KeyPress as triggering the Delete
            // action, w/out the user having to press Fn-Delete, which is whats needed to actually
            // generate the VK_DELETE key code.
                      
        boolean mayModifySelection() { return true; }
        boolean enabledFor(LWSelection s) { return canEdit(s); }
        
            void act(LWSelection s) {

                s.clearAncestorSelected();

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
//                 s.clearAncestorSelected();
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
        new LWCAction(VueResources.local("menu.format.copystyle"), keyStroke(KeyEvent.VK_C, COMMAND+SHIFT)) {
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
    new LWCAction(VueResources.local("menu.format.applystyle"), keyStroke(KeyEvent.VK_V, COMMAND+SHIFT)) {
        boolean enabledFor(LWSelection s) { return s.size() > 0 && StyleBuffer != null; }
        void act(LWComponent c) {
            c.copyStyle(StyleBuffer);
        }
    };
    
    //-----------------------
    // Context Menu Actions
    //-----------------------
    public static final Action KeywordAction = new KeywordActionClass(MENU_INDENT + VueResources.local("mapViewer.componentMenu.keywords.label"));
    public static final Action ContextKeywordAction = new KeywordActionClass(VueResources.local("actions.addkeywords"));
    
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
    public static final LWCAction AddImageAction = new LWCAction(VueResources.local("mapViewer.componentMenu.addImage.label")) {
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
                 //   int n = JOptionPane.showConfirmDialog(null, VueResources.local("replaceFile.text") + " \'" + pdfFileName.getName() + "\'", 
                 //           VueResources.local("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
    
    public static final LWCAction AddFileAction = new LWCAction(VueResources.local("mapViewer.componentMenu.addFile.label")) {
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
    
    public static final VueAction SaveCopyToZotero = new VueAction(VueResources.getString("zotero.saveCopy"))
    {
   	    
      	public void act()
      	{    	if (VUE.askSaveIfModified(VUE.getActiveMap())) {
      	      netscape.javascript.JSObject win = (netscape.javascript.JSObject) netscape.javascript.JSObject.getWindow(VueApplet.getInstance());
      	      String[] arguments = { VUE.getActiveMap().getFile().getAbsolutePath(),VUE.getActiveMap().getDisplayLabel() };
      	      win.call("doImportMap", arguments);
      	     // System.out.println("JS CALLED");
      		}
      	}
    };
    
    public static final LWCAction AddResourceToZotero = new LWCAction(VueResources.local("zotero.addResource")) {
    	public void act(LWComponent c)
    	{
    		Resource r = c.getResource();
    		if (r !=null)
    		{
    			String spec = r.getSpec();
    			
    			
    			if (spec.startsWith("http") || spec.startsWith("https"))
    			{
    				//import from url
    				netscape.javascript.JSObject win = (netscape.javascript.JSObject) netscape.javascript.JSObject.getWindow(VueApplet.getInstance());
  	      	      String[] arguments = { spec };
  	      	      win.call("doImportUrl", arguments);

    			}
    			else
    			{
    				//import from file..
    				netscape.javascript.JSObject win = (netscape.javascript.JSObject) netscape.javascript.JSObject.getWindow(VueApplet.getInstance());
  	      	      String[] arguments = { spec, r.getTitle() };
  	      	      win.call("doImportFile", arguments);

    			}
    		}
    	}
    };
    public static final LWCAction AddURLAction = new LWCAction(VueResources.local("mapViewer.componentMenu.addURL.label")) {
            public void act(LWComponent c) 
            {
                File fileName = null;
                String resourceString = "http://";
                Resource r =c.getResource();
                if (r != null)
                    resourceString = r.getSpec();
    		
                String title = VueResources.local((c instanceof LWNode ) ? "dialog.addurl.node.title" : "dialog.addurl.link.title");
                String option = (String)VueUtil.input(VUE.getApplicationFrame(), VueResources.local("dialog.addurl.label"), title, JOptionPane.PLAIN_MESSAGE, null, resourceString);

                if (option == null || option.length() <= 0)
                    return;
                
                /*
                 * At one point I was trying to do something clever if you tried to type a url with GET parameters
                 * into the Add URL box but it seems to have caused more problems then it solved at this point.
                 */
               /* if (option.indexOf("?") > 0)
                {
                	String encoded = option.substring(option.indexOf("?")+1);
                	
                	encoded = URLEncoder.encode(encoded);

                	option = option.substring(0,option.indexOf("?")+1) + encoded;
                }*/
              //  if (!option.startsWith("http://") || !option.startsWith("https://") || !option.startsWith("file://"))
                //	option = "http://" + option;
                //int option = chooser.showOpenDialog(tufts.vue.VUE.getDialogParent());
                //if (option != null && option.length() > 0) {

            	URI uri = null;
            	
                try {
                    uri = new URI(option);
                } catch (URISyntaxException e) {
                    VueUtil.alert((Component)VUE.getApplicationFrame(),
                                                  VueResources.local("dialog.addurlaction.message"), 
                                                  VueResources.local("dialog.addurlaction.title"), 
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
             
                r = c.getResource();
               // if (r == null) {
                    r = c.getResourceFactory().get(uri);
                    if (r == null) {
                        VueUtil.alert((Component)VUE.getApplicationFrame(),
                        							   VueResources.local("dialog.addurlaction.message"), 
                        							   VueResources.local("dialog.addurlaction.title"), 
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


    public static final LWCAction EditMasterSlide = new LWCAction(VueResources.local("menu.pathways.editmasterslide"))
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
            		VUE.depthSelectionControl.setVisible(true);
            	}
            	else
            	{
            		if (!(VUE.getActiveViewer().getFocal() instanceof LWSlide))
            		{
            		//	zoomFactor = VUE.getActiveViewer().getZoomFactor();
            		//	VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());            			
            			VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());
                		VUE.getReturnToMapButton().setVisible(true);
                		VUE.depthSelectionControl.setVisible(false);
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
                VUE.depthSelectionControl.setVisible(true);
                //ZoomTool.setZoom(zoomFactor);
            	    

            		
            }
            else if (focal instanceof LWGroup) {
                VUE.getActiveViewer().loadFocal(VUE.getActiveMap());
                if (VUE.getActiveMap().getTempBounds() != null)
                    ZoomTool.setZoomFitRegion(VUE.getActiveMap().getTempBounds());
                VUE.getReturnToMapButton().setVisible(false);
                VUE.depthSelectionControl.setVisible(true);
            }
        }

        @Override
        public boolean overrideIgnoreAllActions() { return true; }        
    }
    
    public static final VueAction ReturnToMap = new ReturnToMapAction();
    
    public static final LWCAction EditSlide = new LWCAction(VueResources.local("action.editslide"))
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
            		VUE.depthSelectionControl.setVisible(true);
            		//ZoomTool.setZoom(zoomFactor);
            	    

            		
            	}
            	else
            	{
            		
            		VUE.getActiveMap().setTempZoom(VUE.getActiveViewer().getZoomFactor());
            		VUE.getReturnToMapButton().setVisible(true);
            		VUE.depthSelectionControl.setVisible(false);
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
    
    public static final LWCAction SyncToNode = new LWCAction(VueResources.local("mapViewer.componentMenu.syncMenu.slide2node")) 
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
    
    public static final LWCAction SyncToSlide = new LWCAction(VueResources.local("mapViewer.componentMenu.syncMenu.node2slide")) 
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
    
    public static final LWCAction SyncAll = new LWCAction(VueResources.local("mapViewer.componentMenu.syncMenu.all")) 
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
    
    public static final LWCAction RemoveResourceAction = new LWCAction(VueResources.local("mapViewer.componentMenu.removeResource.label")) {
        public void act(LWComponent c) 
        {        	    
        	final LWSelection sel = new LWSelection();
        	Resource resource = c.getResource();
        	//sel.clear();
        	URLResource nullResource = null;
        	c.setResource(nullResource);            
        	
        	if (c.hasChildren())
        	{
        		List<LWComponent> children = c.getChildren();
        		Iterator<LWComponent> childIterator = children.iterator();
        		
        		while (childIterator.hasNext())
        		{
        			LWComponent comp = childIterator.next();
        			if (comp instanceof LWImage)
        			{
    					LWImage image = ((LWImage)comp);
        				if (image.getResource().equals(resource))
        					sel.add(comp);        				         			
        			}
        		}
        
        		for (LWContainer parent : sel.getParents()) 
        		{
                      if (DEBUG.Enabled) info("deleting for parent " + parent);
                       parent.deleteChildrenPermanently(sel);

                       // someday: would be nice if this could simply be
                       // handled as a traversal on the map: pass down
                       // the list of items to remove, and any parent
                       // that notices one of it's children removes it.
                       
                }		
        	}
        }
    };
    
    public static final LWCAction RemoveResourceKeepImageAction = new LWCAction(VueResources.local("mapViewer.componentMenu.removeResourceKeepImage.label")) {
        public void act(LWComponent c) 
        {        	    
       
        	URLResource nullResource = null;
        	c.setResource(nullResource);                  
        }
    };

    //m.add(Actions.AddURLAction);
//    m.add(Actions.RemoveResourceAction);
    public static final Action NotesAction = new NotesActionClass(MENU_INDENT + VueResources.local("mapViewer.componentMenu.notes.label"));
    public static final Action ContextNotesAction = new NotesActionClass(VueResources.local("actions.addnotes"));
    	
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
    
    
    
    public static final Action InfoAction = new VueAction(VueResources.local("mapViewer.componentMenu.info.label")) {
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
        new LWCAction(VueResources.local("menu.format.group"), keyStroke(KeyEvent.VK_G, COMMAND), "/tufts/vue/images/xGroup.gif") {
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
        new LWCAction(VueResources.local("menu.format.ungroup"), keyStroke(KeyEvent.VK_G, COMMAND+SHIFT), "/tufts/vue/images/xUngroup.png") {
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
        new LWCAction(VueResources.local("menu.edit.rename"), VueUtil.isWindowsPlatform() ? keyStroke(KeyEvent.VK_F2) : keyStroke(KeyEvent.VK_ENTER)) {
        boolean undoable() { return false; } // label editor handles the undo
            
        boolean enabledFor(LWSelection s) {
            return s.size() == 1 && s.first().supportsUserLabel() && !s.first().isLocked();
        }
        void act(LWComponent c) {
            // todo: throw interal exception if c not in active map
            // todo: not working in slide viewer...

            // BUG: can happen on hitting enter in the search box when a single node selected SMF logged 2012-06-10 19:40.28 Sunday SFAir.local
            // Fixed by changing SearchTextField key handlers to operate on keyPressed v.s. keyReleased and being sure to consume the event.
            //if (VUE.mSearchTextField.hasFocus()) return;
            
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
    new LWCAction(VueResources.local("menu.format.arrange.bringtofront"),
    VueResources.local("menu.format.arrange.bringtofront.tooltip"),
    keyStroke(KeyEvent.VK_F, ALT)) {
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
    new LWCAction(VueResources.local("menu.format.arrange.sendtoback"),
    VueResources.local("menu.format.arrange.sendtoback.tooltip"),
    keyStroke(KeyEvent.VK_B, ALT)) {
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
    new LWCAction(VueResources.local("menu.format.arrange.bringforward")) {
        boolean enabledFor(LWSelection s) { return BringToFront.enabledFor(s); }
        void act(LWSelection selection) {
            LWContainer.bringForward(selection);
        }
    };

    public static final LWCAction SendBackward =
    new LWCAction(VueResources.local("menu.format.arrange.sendbackward")) {
        boolean enabledFor(LWSelection s) { return SendToBack.enabledFor(s); }
        void act(LWSelection selection) {
            LWContainer.sendBackward(selection);
        }
    };
    
    //-------------------------------------------------------
    // Font/Text Actions
    //-------------------------------------------------------
    
    public static final LWCAction FontSmaller =
    new LWCAction(VueResources.local("menu.format.font.fontsmaller"), keyStroke(KeyEvent.VK_MINUS, COMMAND+SHIFT)) {
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
    new LWCAction(VueResources.local("menu.format.font.fontbig"), keyStroke(KeyEvent.VK_EQUALS, COMMAND+SHIFT)) {
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
    new LWCAction(VueResources.local("menu.format.font.fontbold"), keyStroke(KeyEvent.VK_B, COMMAND)) {
        void act(LWComponent c) {
            c.mFontStyle.set(c.mFontStyle.get() ^ Font.BOLD);
        }
    };
    public static final LWCAction FontItalic =
    new LWCAction(VueResources.local("menu.format.font.fontitalic"), keyStroke(KeyEvent.VK_I, COMMAND)) {
        void act(LWComponent c) {
            c.mFontStyle.set(c.mFontStyle.get() ^ Font.ITALIC);
        }
    };
    
    public static final LWCAction FontUnderline =
        new LWCAction(VueResources.local("menu.format.font.fontunderline"), keyStroke(KeyEvent.VK_U, COMMAND)) {
            void act(LWComponent c) {
                c.mFontUnderline.set((c.mFontUnderline.get().toString()).equals("underline") ? "normal" : "underline");

            }
        };

    /** this will toggle the collapsed state flag on the selected nodes */
    public static final LWCAction Collapse =
    new LWCAction(VueResources.local("menu.view.collapse")) {
        boolean enabledFor(LWSelection s) {
            final int nodeCount = s.count(LWNode.class);
            return nodeCount > 1 || s.size() == 1 && s.only().hasChildren();
        }
        @Override void act(LWComponent c) {
            c.setCollapsed(!c.isCollapsed());
        }
    };
    
    public static final VueAction ToggleGlobalCollapse =
        new VueAction(VueResources.local("menu.view.collapseAll"), keyStroke(KeyEvent.VK_K, COMMAND)) {
        public void act() {

            LWComponent.toggleGlobalCollapsed();
            VUE.layoutAllMaps(LWComponent.Flag.COLLAPSED);
            viewer().getFocal().notify(this, LWKey.Repaint);

            // Currently, this action is ONLY fired via a menu item.  If other code
            // points might set this directly, this should be changed to a toggleState
            // action (impl getToggleState), and those code points should call this
            // action to do the toggle, so the menu item checkbox state will stay
            // synced.
        }
            
            @Override public boolean overrideIgnoreAllActions() { return true; }        
            
    };

    // TODO: need a ViewerAction subclass of VueAction that is for
    // actions that are only enabled as long as there is an active viewer
    // (also may want a MapAction subclass?  should be same semantics -- we don't support empty MapViewer's)
    
    public static final VueAction ViewBackward =
        new VueAction(VueResources.local("menu.view.backward"), VueResources.local("menu.view.backward.tooltip"),
        		keyStroke(KeyEvent.VK_LEFT, COMMAND), null) {
        public void act() {
            viewer().viewBackward();
        }
    };
    public static final VueAction ViewForward =
        new VueAction(VueResources.local("menu.view.forward"), VueResources.local("menu.view.forward.tooltip"),
        		keyStroke(KeyEvent.VK_RIGHT, COMMAND), null) {
        public void act() {
            viewer().viewForward();
        }
    };

    static {
        new MapViewer.Listener() {
            { EventHandler.addListener(MapViewer.Event.class, this); } // this ref only thing preventing GC 
            public void eventRaised(MapViewer.Event e) {

                if (e.id != MapViewer.Event.VIEWS_CHANGED)
                    return;
                
                if (e.viewer != null) {
                    ViewBackward.setEnabled(e.viewer.hasBackwardViews());
                    ViewForward.setEnabled(e.viewer.hasForwardViews());
                } else {
                    // todo: never happens -- provide auxillary somewhere event just for this case?
                    ViewBackward.setEnabled(false);
                    ViewForward.setEnabled(false);
                }
            }
            public String toString() { return getClass().getEnclosingClass().getName() + "(back/forward menu updater)"; }
        };
    }
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
        static float oldCenterX = Float.NaN,oldCenterY= Float.NaN;
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
            // TODO: change mParents in LWSelection to be a multi-set, then just do the arrange
            // based on the most top level parent with the most entries (even just doing the
            // first most top-level parent would handle most cases)
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
        	for (LWComponent c : selection) {
        		arrange(c);
        	}
        }
        /**
        void arrange(LWSelection selection,float centerX,float centerY) {
        	for (LWComponent c : selection)
                arrange(c,centerX,centerY);
        }
        */
        
        void arrange(LWComponent c) { throw new RuntimeException("unimplemented arrange action"); }
        /**
        void arrange(LWComponent c,float centerX,float centerY) {
        	arrange(c);
        }
*/
        protected static void clusterNodesAbout(final LWComponent center, final Collection<LWComponent> clustering) {

            if (DEBUG.Enabled) Log.debug("clusterNodesAbout: " + center + ": " + Util.tags(clustering));

            // recording the current action time on the centering node can later help
            // us determine the layout priority for new data items when adding to the map
            // (by looking at the most recent clustering centers)
            center.setClientData(tufts.vue.ds.DataAction.ClusterTimeKey, currentActionTime);
            
            final LWContainer commonParent = center.getParent();
            final List<LWComponent> toReparent = new ArrayList();

            // this is important both to remove any linked that may be our descendents, as
            // well as grab any linked that are currently children of something else
            // (unfortunately, this will also grab them out of other layers if they were there,
            // which isn't technically needed, but okay for now).
            for (LWComponent c : clustering) {
                if (c.getParent() != commonParent)
                    toReparent.add(c);
            }

            //-----------------------------------------------------------------------------
            // TODO: if center is a child of any one of cluster, remove it first!
            // That way we can go back and forth between different relationship priorities & styles.
            // (possibly if it's a child of anything?)
            //-----------------------------------------------------------------------------
            
            if (toReparent.size() > 0) {
                if (toReparent.contains(commonParent)) {
                    // TOFIX: if we attempt to cluster a child node linked to it's parent, we get this:
                    throw new Error("clusterNodesAbout: setup failure, toReparent contains commonParent");
                }
                commonParent.addChildren(toReparent, LWComponent.ADD_CHILD_TO_SIBLING);
            }
            
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

        public static void clusterLinked(final LWComponent center) {
            if (DEBUG.Enabled) Log.debug("clustering linked " + center);
            
            clusterNodesAbout(center, center.getClustered());
        }
        
        // todo: smarter algorithm that lays out concentric rings, with more nodes in
        // each larger ring (compute ellipse circumference); tricky: either need a good
        // guess at the number of rings, or just leave the last ring far more spread out
        // (remainder nodes will be left for the last right
        
        public static void clusterNodes(Collection<LWComponent> nodes)
        {
            // todo: if a link-chain detected, lay out in link-order e.g., start with
            // any non-linked nodes, then find any with one link (into our set), and
            // then follow the link chain laying out any nodes found in our selection
            // first (removing them from the layout list), then continue to the next
            // node, etc.  Also, can prefer link directionality if there are arrow
            // heads.
        
            final double slice = (Math.PI * 2) / nodes.size();

            final int maxTierSize = 20;
            final int tiers = nodes.size() / maxTierSize;
            //final int tiers = 3;

            final double startAngle;

            if (nodes.size() == 1) {
                // Add 90 degrees so the "clock" starts at bottom (the single clustered item appears at the bottom)
                startAngle = Math.PI/2;
            } else {
                // Add 270 degrees so the "clock" starts at the top -- so something
                // will be laid out at exactly the 12 o'clock position
                startAngle = Math.PI/2*3;
            }

            
            Color fill = Color.white;

            int i = 0;

            // TODO: if we keep the spiral layout, could note what Field we're clustering
            // on (if any), and find the field with the next highest number of enumerated
            // values, and auto-organize by that value (which you wouldn't see until
            // you did the search, but would be a niceity)
            
            if (nodes.size() > maxTierSize) {

                //------------------------------------------------------------------
                // tiered circular layout or "spiral" -- begins to spiral beyond 2 tiers,
                // and has a distinct spiral appearance upwards of about 100 nodes
                // (when nodes are small and uniform)
                //------------------------------------------------------------------
                
                for (LWComponent c : nodes) {
                    final double angle = startAngle + slice * i;
                    final int tier = i % tiers;
                    final double factor = 1 + tier * 0.33;
                    final double rwide = radiusWide * factor;
                    final double rtall = radiusTall * factor;
                    c.setCenterAt(centerX + rwide * Math.cos(angle),
                                  centerY + rtall * Math.sin(angle));
                    i++;

                    //c.setFillColor(fill);
                    //fill = Util.factorColor(fill, 0.99);
                }
                
            } else {

                //------------------------------------------------------------------
                // pure circular layout
                //------------------------------------------------------------------

                for (LWComponent c : nodes) {
                    final double angle = startAngle + slice * i++;
                    c.setCenterAt(centerX + radiusWide * Math.cos(angle),
                                  centerY + radiusTall * Math.sin(angle));
                }
            }
        
        }

//         protected void old_clusterNodes(Collection<LWComponent> nodes)
//         {
//             // todo: if a link-chain detected, lay out in link-order e.g., start with
//             // any non-linked nodes, then find any with one link (into our set), and
//             // then follow the link chain laying out any nodes found in our selection
//             // first (removing them from the layout list), then continue to the next
//             // node, etc.  Also, can prefer link directionality if there are arrow
//             // heads.
        
//             final double slice = (Math.PI * 2) / nodes.size();
//             int i = 0;

//             final int maxTierSize = 20;
//             final int tiers = nodes.size() / maxTierSize;
//             //final int tiers = 3;

//             java.awt.Color fill = java.awt.Color.white;

//             for (LWComponent c : nodes) {
//                 // We add Math.PI/2*3 (270 degrees) so the "clock" always starts at the top -- so something
//                 // is always is laid out at exactly the 12 o'clock position
//                 final double angle = Math.PI/2*3 + slice * i;

                
//                 if (false && nodes.size() > 200) {
//                     // random layout
//                     double rand = Math.random()+.1;
//                     c.setCenterAt(centerX + radiusWide * rand * Math.cos(angle),
//                                   centerY + radiusTall * rand * Math.sin(angle));

//                 } else if (nodes.size() > maxTierSize) {
//                     // tiered circular layout -- begins to spiral beyond 2 tiers
//                     final int tier = i % tiers;
//                     final double factor = 1 + tier * 0.33;
//                     final double rwide = radiusWide * factor;
//                     final double rtall = radiusTall * factor;
// //                     final double rwide = (radiusWide / tiers) * (tier+1);
// //                     final double rtall = (radiusTall / tiers) * (tier+1);
//                     c.setCenterAt(centerX + rwide * Math.cos(angle),
//                                   centerY + rtall * Math.sin(angle));
//                          if (tier == 0) c.setFillColor(Color.magenta);
//                     else if (tier == 1) c.setFillColor(Color.red);
//                     else if (tier == 2) c.setFillColor(Color.green);
//                     else if (tier == 3) c.setFillColor(Color.blue);
//                 } else {

//                     // circular layout
//                     c.setCenterAt(centerX + radiusWide * Math.cos(angle),
//                                   centerY + radiusTall * Math.sin(angle));
//                 }

//                 i++;

//                 //c.setFillColor(fill);
//                 //fill = Util.factorColor(fill, 0.99);
                    
//             }
        
//         }
        
        
        
    };

    public static LWComponent[] sortByX(LWComponent[] array) {
        java.util.Arrays.sort(array, LWComponent.XSorter);
        return array;
    }
    
    public static LWComponent[] sortByY(LWComponent[] array) {
        java.util.Arrays.sort(array, LWComponent.YSorter);
        return array;
    }
    
    
    public static final Action FillWidth = new ArrangeAction(VueResources.local("actions.fillwidth")) {
        void arrange(LWComponent c) {
            c.setFrame(minX, c.getY(), maxX - minX, c.getHeight());
        }
    };
    public static final Action FillHeight = new ArrangeAction(VueResources.local("actions.fillheight")) {
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
    
    
    public static final LWCAction PushOutLinked =
    new LWCAction(VueResources.local("menu.format.arrange.pushout"), keyStroke(KeyEvent.VK_CLOSE_BRACKET, ALT)) {
        
        boolean enabledFor(LWSelection s) {
            return enabledForPushPull(s);
        }
        // todo: for selection size > 1, push on bounding box
        void act(LWComponent c) {
            // although we don't currently want to support pushing inside anything other than
            // a layer, this generic call would handle other cases if we can support them
            projectNodes(c, PUSH_DISTANCE, PUSH_LINKED);
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
    
    public static final LWCAction PushOut =
    new LWCAction(VueResources.local("menu.format.arrange.pushout"), keyStroke(KeyEvent.VK_EQUALS, ALT)) {
        
        boolean enabledFor(LWSelection s) {
        //   return enabledForPushPull(s);
        	return s.size()>=1;
        }
        @Override void act(LWSelection s) {
        	if(s.size()==1) {
        		act(s.get(0));
        	} else {
        		LayoutAction.stretch.act(s);
        	}
        }
        // todo: for selection size > 1, push on bounding box
        @Override public void act(LWComponent c) {
            // although we don't currently want to support pushing inside anything other than
            // a layer, this generic call would handle other cases if we can support them
            projectNodes(c, PUSH_DISTANCE, PUSH_ALL);
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
    
    public static final LWCAction PullInLinked =
        new LWCAction(VueResources.local("menu.format.arrange.pullin"), keyStroke(KeyEvent.VK_OPEN_BRACKET, ALT)) {
            boolean enabledFor(LWSelection s) {
                return enabledForPushPull(s);
            }
            void act(LWComponent c) {
                projectNodes(c, -PUSH_DISTANCE, PUSH_LINKED);
                
            }
        };
        
        public static final LWCAction PullIn =
            new LWCAction(VueResources.local("menu.format.arrange.pullin"), keyStroke(KeyEvent.VK_MINUS, ALT)) {
                boolean enabledFor(LWSelection s) {
                    return enabledForPushPull(s);
                }
                void act(LWComponent c) {
                    projectNodes(c, -PUSH_DISTANCE, PUSH_ALL);
                    
                }
            };
            
    private static final boolean DEBUG_PUSH = false;
    public static final Object PUSH_ALL = "pushAll";
    public static final Object PUSH_LINKED = "pushLinked";
    
    /** pushing must be a member of a map -- cannot push non-map member nodes.  todo: allow passing in of the map for this */
    public static void projectNodes(final LWComponent pushing, final int distance, Object pushKey)
    {
        Collection<LWComponent> toPush = null;

        if (pushKey == PUSH_LINKED && pushing.hasLinks())
            toPush = pushing.getLinked();

        if (toPush == null || toPush.size() == 0) {
            // ideally, this would push all the top level children in the current FOCAL
            toPush = pushing.getMap().getTopLevelItems(ChildKind.EDITABLE);
        }
        //toPush = pushing.getMap().getAllDescendents(); // only want top level -- especially, don't push children inside groups!
        //toPush = pushing.getParent().getChildren();
        
        projectNodes(toPush, pushing, distance);
    }
        
        // todo: combine into a Geometry.java with computeIntersection, computeConnector, projectPoint code from VueUtil
        // todo: to handle pushing inside slides, we'd need to get rid of the references to map bounds,
        // and always use local bounds
        public static void projectNodes(final Iterable<LWComponent> toPush, final LWComponent pusher, final int distance)
        {
//             if (DEBUG.Enabled) Log.debug("projectNodes: "
//                                          + "\n\t  pusher: " + pushing
//                                          + "\n\t  toPush: " + Util.tags(toPush)
//                                          + "\n\tdistance: " + distance
//                                          );//,new Throwable("HERE"));
            
            if (DEBUG.Enabled) Log.debug("projectNodes: pusher=" + pusher);

            
            //pusher.getMapCenterY())
            //final Rectangle2D pushingRect = pushing.getMapBounds();
            
            final RectangularShape pushShape = pusher.getMapShape();

            final Collection exclude = java.util.Collections.singletonList(pusher);

            projectNodes(toPush, exclude, pusher, pushShape, distance);

        }
    
        private static void projectNodes
            (final Iterable<LWComponent> toPush,
             final Collection toExclude,
             final LWComponent pushing, // we want to remove this argument and only rely on pushShape, but we need alot more refactoring for that
             final RectangularShape pushShape,
             final int distance)
        {
            if (DEBUG.Enabled) Log.debug("projectNodes: "
                                         + "\n\t  pushing: " + pushing
                                         + "\n\tpushShape: " + pushShape
                                         + "\n\t   toPush: " + Util.tags(toPush)
                                         + "\n\t distance: " + distance
                                         );//,new Throwable("HERE"));

            final Point2D.Float groundZero = new Point2D.Float((float) pushShape.getCenterX(),
                                                               (float) pushShape.getCenterY());
            
            final java.util.List<LWComponent> links = new java.util.ArrayList();
            final java.util.List<LWComponent> nodes = new java.util.ArrayList();
        
            for (LWComponent node : toPush) {

                if (toExclude.contains(node))
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

                Point2D newCenter = null;
            
                float adjust = distance;

                //final boolean intersects = node.intersects(pushingRect); // problems w/slide icons
                final boolean intersects = pushShape.intersects(node.getMapBounds());

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
                
                    // If overlapping, we want to move the node along a line away from the center
                    // of the pushing node until it no longer overlaps.  As part of this process,
                    // we compute the point at the edge of the pushing node that the overlapping
                    // node would be at if all we were going to do was move it to the edge.  This
                    // isn't strictly needed to produce the end result (we could start iterating
                    // immediately, we don't need to start at the intersect), but it's useful for
                    // debugging, and it may be a useful location to know for future tweaks to this
                    // code.
                
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

                    if (Util.isBadPoint(farOut) || Util.isBadPoint(intersect)) {
                        Log.warn("bad projection points:"
                                 + "\n\tgroundZero: " + Util.fmt(groundZero)
                                 + "\n\t connector: " + Util.fmt(connector)
                                 + "\n\t    farOut: " + Util.fmt(farOut)
                                 + "\n\t   testRay: " + Util.fmt(testRay)
                                 + "\n\t intersect: " + Util.fmt(intersect)
                                 + "\n\t    pusher: " + pushing
                                 + "\n\t    pushee: " + node
                                 );
                    } else {
                        
                        for (int i = 0; i < 1000; i++) {
                            node.setCenterAt(VueUtil.projectPoint(intersect, connector, i * 2f));
                            //                         if (!node.intersects(pushingRect)) // problems w/slide icons
                            //                             break;
                            if (!pushShape.intersects(node.getMapBounds()))
                                break;
                            if (DEBUG_PUSH) Log.debug("PUSH ITER " + i + " on " + node);
                        }
                    }

                    adjust /= 2; // we'll only push half the standard amount from here
                }

                newCenter = VueUtil.projectPoint(node.getMapCenterX(), node.getMapCenterY(), connector, adjust);

                if (Util.isBadPoint(newCenter)) {
                    Log.error("bad newCenter: " + newCenter);
                    newCenter = null;
                }

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
                    if (newCenter != null)
                        n.setCenterAt(newCenter);
                } else {
                    if (newCenter != null)
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
    public static final LWCAction NudgeUp       = new NudgeAction(  0,  -1, VueResources.local("menu.format.align.nudgeup"),    keyStroke(KeyEvent.VK_UP));
    public static final LWCAction NudgeDown     = new NudgeAction(  0,   1, VueResources.local("menu.format.align.nudgedown"),  keyStroke(KeyEvent.VK_DOWN));
    public static final LWCAction NudgeLeft     = new NudgeAction( -1,   0, VueResources.local("menu.format.align.nudgeleft"),  keyStroke(KeyEvent.VK_LEFT));
    public static final LWCAction NudgeRight    = new NudgeAction(  1,   0, VueResources.local("menu.format.align.nudgeright"), keyStroke(KeyEvent.VK_RIGHT));

    public static final LWCAction BigNudgeUp    = new NudgeAction(  0, -10, VueResources.local("menu.format.align.bignudgeup"),    keyStroke(KeyEvent.VK_UP, SHIFT));
    public static final LWCAction BigNudgeDown  = new NudgeAction(  0,  10, VueResources.local("menu.format.align.bignudgedown"),  keyStroke(KeyEvent.VK_DOWN, SHIFT));
    public static final LWCAction BigNudgeLeft  = new NudgeAction(-10,   0, VueResources.local("menu.format.align.bignudgeleft"),  keyStroke(KeyEvent.VK_LEFT, SHIFT));
    public static final LWCAction BigNudgeRight = new NudgeAction( 10,   0, VueResources.local("menu.format.align.bignudgeright"), keyStroke(KeyEvent.VK_RIGHT, SHIFT));
        
    
    public static final ArrangeAction AlignTopEdges = new ArrangeAction(VueResources.local("menu.format.align.topedges"), keyStroke(KeyEvent.VK_UP, ALT)) {
        void arrange(LWComponent c) { c.setLocation(c.getX(), minY); }
    };
    public static final ArrangeAction AlignBottomEdges = new ArrangeAction(VueResources.local("menu.format.align.bottomedges"), keyStroke(KeyEvent.VK_DOWN, ALT)) {
        void arrange(LWComponent c) { c.setLocation(c.getX(), maxY - c.getHeight()); }
    };
    public static final ArrangeAction AlignLeftEdges = new ArrangeAction(VueResources.local("menu.format.align.leftedges"), keyStroke(KeyEvent.VK_LEFT, ALT)) {
        void arrange(LWComponent c) { c.setLocation(minX, c.getY()); }
    };
    public static final ArrangeAction AlignRightEdges = new ArrangeAction(VueResources.local("menu.format.align.rightedges"), keyStroke(KeyEvent.VK_RIGHT, ALT)) {
        void arrange(LWComponent c) { c.setLocation(maxX - c.getWidth(), c.getY()); }
    };
    public static final ArrangeAction AlignCentersRow = new ArrangeAction(VueResources.local("menu.format.align.centerinrow"), keyStroke(KeyEvent.VK_R, ALT)) {
        void arrange(LWComponent c) { c.setLocation(c.getX(),centerY - c.getHeight()/2); }
    };
    public static final ArrangeAction AlignCentersColumn = new ArrangeAction(VueResources.local("menu.format.align.centerincolumn"), keyStroke(KeyEvent.VK_C, ALT)) {
        void arrange(LWComponent c) { c.setLocation(centerX - c.getWidth()/2, c.getY()); }
    };
    
//     public static final ArrangeAction OLDMakeCluster = new ArrangeAction(VueResources.local("menu.format.align.makecluster"), keyStroke(KeyEvent.VK_PERIOD, ALT)) {
//             boolean supportsSingleMover() { return false; }
//             boolean enabledFor(LWSelection s) { return s.size() > 0; }
            
//             void arrange(LWSelection selection) {

//                 final double radiusWide, radiusTall;

//                 selection.resetStatistics(); // todo: why do we need to reset? is this a clone? (has no statistics)
//                 if (DEBUG.Enabled) Log.debug("DATAVALUECOUNT: " + selection.getDataValueCount());
//                 if (DEBUG.Enabled) Log.debug("DATA-ROW-COUNT: " + selection.getDataRowCount());

//                 final int nDataValues = selection.getDataValueCount();
//                 final int nDataRows = selection.getDataRowCount();
                
//                 if (selection.size() == 1) {

//                     // if a single item in selection, arrange all nodes linked to it in a circle around it

//                     final LWComponent center = selection.first();
//                     final Collection<LWComponent> linked = center.getLinked();
                    
// //                     final LWContainer commonParent = center.getParent();
// //                     final List<LWComponent> toReparent = new ArrayList();
// //                     // this is important both to remove any linked that may be our descendents, as
// //                     // well as grab any linked that are currently children of something else
// //                     // (unfortunately, this will also grab them out of other layers if they were there,
// //                     // which isn't technically needed, but okay for now).
// //                     for (LWComponent c : linked) {
// //                         if (c.getParent() != commonParent)
// //                             toReparent.add(c);
// //                     }

// //                     if (toReparent.size() > 0)
// //                         commonParent.addChildren(toReparent, LWComponent.ADD_CHILD_TO_SIBLING);

//                     clusterNodes(center, linked);
                    
//                     selection().setTo(center);
//                     selection().add(linked);
                    
//                 }
//                 else if (nDataValues == selection.size()) {

//                     // If all the items in the selection are single enumerated data
//                     // VALUES, (e.g., they were all selected by a single click on a
//                     // field in the DataTree, selecting all values for that field) then
//                     // perform a cluster operation on each value separately, clustering
//                     // all connected rows/nodes around each value.

//                     for (LWComponent center : selection)
//                         clusterNodes(center, center.getLinked());

//                 }
//                 else if (nDataValues == 1 && nDataRows == (selection.size() - 1)) {

//                     // If there's a single data VALUE in the selected, and everything
//                     // ELSE is a data ROW, assume we really want to just do a clustering
//                     // around the single data-value.  This is quite a leap to make
//                     // given that the rows could be completely unrelated, but it's
//                     // the most common use case at the moment.

//                     // A more sane approach would be to extract the one value node,
//                     // and do an arrange just with all other nodes found, and not
//                     // care if they're data-nodes or linked nodes or not -- as long
//                     // as we don't do anything nutty like arrange value nodes around
//                     // each other, this should be fine.
                    
//                     Log.debug("guessing at an all-related data-values selection");
                    
//                     // find the one data value and cluster the rest around it

//                     LWComponent center = null;
                    
//                     for (LWComponent c : selection) {
//                         if (c.isDataValueNode()) {
//                             center = c;
//                             break;
//                         }
//                     }
                    
//                     clusterNodes(center, center.getLinked());

//                 }
//                 else {
                    
// //                     radiusWide = (maxX - minX) / 2;
// //                     radiusTall = (maxY - minY) / 2;
                    
// //                     radiusWide = Math.max((maxX - minX) / 2, maxWide);
// //                     radiusTall = Math.max((maxY - minY) / 2, maxTall);
                    
//                     radiusWide = Math.max((maxX - minX) / 2, totalWidth/4);
//                     radiusTall = Math.max((maxY - minY) / 2, totalHeight/4);
                    
//                     //clusterNodes(centerX, centerY, radiusWide, radiusTall, selection);
//                     clusterNodes(selection);

//                     // The ring will expand on subsequent MakeCircle calls, because nodes are laid
//                     // out on the ring on-center, but the bounds used to create the initial ring
//                     // form the the top of the north-most mode to the bottom of the south-most node
//                     // (same for east/west), which on the next call will be a bigger ring.  Would
//                     // be hairy trying to figure out the the ring size that would contain the given
//                     // nodes inside a given rectangle when laid-out on-center. [ Actually, would
//                     // just computing the on-center bounds work? Better, but only perfectly if
//                     // there's a node at exaclty N/S/E/W on the dial, and the ring-order (currently
//                     // selection order, which is usually stacking order) hasn't changed.] If we
//                     // want such functionality, would be better handled via a persistent "ring"
//                     // layout object (like a group), that maintains a persistant, selectable oval
//                     // that can be resized directly -- the bounding box would only be used for
//                     // picking the initial size.

//                 }
//             }
            
//     };

    public static abstract class ClusterAction extends ArrangeAction {
        
        boolean supportsSingleMover() { return false; }
        boolean enabledFor(LWSelection s) { return s.size() > 0; }

        ClusterAction(String labelKey, KeyStroke stroke) {
            super(VueResources.local(labelKey), stroke);
        }

        public abstract void doClusterAction(LWComponent center, Collection<LWComponent> nodes);
            
        void arrange(LWSelection selection) {

            final double radiusWide, radiusTall;

            selection.resetStatistics(); // todo: why do we need to reset? is this a clone? (has no statistics)

            final int nDataValues = selection.getDataValueCount();
            final int nDataRows = selection.getDataRowCount();
            
            if (DEBUG.Enabled) {
                Log.debug("DATAVALUECOUNT: " + nDataValues);
                Log.debug("DATA-ROW-COUNT: " + nDataRows);
            }

            if (selection.size() == 1) {

                // if a single item in selection, arrange all nodes linked to it in a circle around it

                final LWComponent center = selection.first();
                final Collection<LWComponent> linked = center.getClustered();

                final List<LWComponent> toReparent = new ArrayList();

                for (LWComponent c : linked) {
                    if (c.hasAncestor(center))
                        toReparent.add(c);
                }

                if (toReparent.size() > 0)
                    center.getParent().addChildren(toReparent, LWComponent.ADD_CHILD_TO_SIBLING);

                doClusterAction(center, linked);
                    
                selection().setTo(center);
                selection().add(linked);
                    
            }
            // TODO: also handle the case when all values are rows (only from
            // the same schema?) useful when joining data-sets -- the row
            // itself may be clustering related nodes from another data-set

            //else if (nDataValues == selection.size() || nDataRows == selection.size()) {
            // problem: if we're just dealing with regular non-data nodes, we won't detect....
            // So if there are no links between anything in the selection, also presume
            // we just want to do the cluster action, tho really this only applies to
            // MakeDataLists and may not apply to the other actions....
            
            else if (nDataValues == selection.size() || nDataRows == selection.size()) {

                // If all the items in the selection are single enumerated data
                // VALUES, (e.g., they were all selected by a single click on a
                // field in the DataTree, selecting all values for that field) then
                // perform a cluster operation on each value separately, clustering
                // all connected rows/nodes around each value.  Unless there are
                // absolutely no links involved, in which case just circle them.

                // TODO: NOT ALWAYS WHAT'S WANTED: may have a central value node (e.g.,
                // Genre=Folk), surrounded by other value nodes (e.g., Artist),
                // connected by COUNT links.  If we see count links try something
                // different.  In the meantime, this case is also causing a stack
                // overflow, as a result of clustering on all of the nodes.

                boolean anyLinks = false;
                for (LWComponent c : selection) {
                    if (c.hasLinks()) {
                        anyLinks = true;
                        break;
                    }
                }

                // TODO: for each in selection, count INTRA-SELECTION links -- if all have one,
                // and one has all, use the one with all as the CENTER

                if (anyLinks) {
                    for (LWComponent asCenter : selection) {
                        Collection<LWComponent> outGroupLinked = new ArrayList(asCenter.getClustered());
                        outGroupLinked.removeAll(selection);
                        if (DEBUG.Enabled) Log.debug("asCenter: " + asCenter + "; outGroupLinked=" + Util.tags(outGroupLinked));
                        doClusterAction(asCenter, outGroupLinked);
                    }
                } else {
                    clusterNodes(selection);
                }

            }
            //else if (nDataValues == 1 && nDataRows == (selection.size() - 1)) {
            else if (nDataValues == 1) {

                // If there's a single data VALUE in the selected, and everything
                // ELSE is a data ROW, assume we really want to just do a clustering
                // around the single data-value.  This is quite a leap to make
                // given that the rows could be completely unrelated, but it's
                // the most common use case at the moment.

                // A more sane approach would be to extract the one value node,
                // and do an arrange just with all other nodes found, and not
                // care if they're data-nodes or linked nodes or not -- as long
                // as we don't do anything nutty like arrange value nodes around
                // each other, this should be fine.
                    
                Log.debug("guessing at an all-related data-values selection");
                    
                // find the one data value and cluster the rest around it

                LWComponent center = null;

                List<LWComponent> toCluster = new ArrayList(selection.size());
                    
                for (LWComponent c : selection) {
                    if (c.isDataValueNode()) {
                        center = c;
                    } else {
                        toCluster.add(c);
                    }
                }
                    
                doClusterAction(center, toCluster);
                //doClusterAction(center, center.getLinked());

            }
            else {
                    
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

    public static final ClusterAction MakeCluster = new ClusterAction("menu.format.layout.makecluster", keyStroke(KeyEvent.VK_PERIOD, ALT)) {
            @Override
            public void doClusterAction(LWComponent center, Collection<LWComponent> nodes) {
                clusterNodesAbout(center, nodes);
            }
        };

    public static final ClusterAction MakeDataLists = new ClusterAction("menu.format.layout.makedatalists", keyStroke(KeyEvent.VK_COMMA, ALT)) {
            
            // TODO: disabling this for multi-seletion breaks one of
            // the main great use cases for this action: whats the
            // issue we're addressing here?
            // @Override boolean enabledFor(LWSelection s) { return s.size() == 1 && s.first().hasLinks(); }
            
            public void doClusterAction(LWComponent c, Collection<LWComponent> nodes) {
                if (c instanceof LWNode) {
                    // grab linked
                    //c.addChildren(new ArrayList(c.getLinked()), LWComponent.ADD_MERGE);
                    c.addChildren(nodes, LWComponent.ADD_MERGE);
                }
            }
        };
    
//     public static final LWCAction MakeDataLists = new ArrangeAction(VueResources.local("menu.format.align.makedatalists"), keyStroke(KeyEvent.VK_COMMA, ALT)) {
//             boolean enabledFor(LWSelection s) { return s.size() == 1 && s.first().hasLinks(); }
//             // if we want this to be do-what-i-mean smart like MakeClusters, factor out
//             // the code there the identifies the single value node v.s. all the linked data nodes,
//             // and re-use it here for the same purpose. (That way you could easily swap back
//             // and forth between clustered and listed displays while all the effected nodes stay selected).
//             // ALSO, want to re-use the code to do a separate arrange when just a bunch of values are selected.
//             @Override
//             public void arrange(LWComponent c) {
//                 if (c instanceof LWNode) {
//                     // grab linked
//                     c.addChildren(new ArrayList(c.getLinked()), LWComponent.ADD_MERGE);
//                 }
//             }
//         };
    
    
//     public static final LWCAction MakeDataLinks = new LWCAction(VueResources.local("menu.format.layout.makedatalinks"), keyStroke(KeyEvent.VK_SLASH, ALT)) {
//             boolean enabledFor(LWSelection s) { return s.size() == 1; } // just one for now
//             Collection<? extends LWComponent> linkTargets = null;
// 
//             @Override
//             public void act(LWSelection s) {
//                 
//                 //tufts.vue.ds.DataAction.addDataLinksForNodes(getMap(), s, linkTargets);
//             }
// //             @Override
// //             public void act(LWSelection s) {
// //                 // we re-use linkTargets below, so we don't need to re-build the list for every node in the selection
// //                 linkTargets = tufts.vue.ds.DataAction.getLinkTargets(s.first().getMap());
// //                 super.act(s);
// //             }
// //             @Override
// //             public void act(LWNode c) {
// //                 tufts.vue.ds.DataAction.addDataLinksForNode(c, linkTargets);
// // //                 tufts.vue.ds.DataAction.addDataLinksForNodes(c.getMap(),
// // //                                                              java.util.Collections.singletonList(c),
// // //                                                              c.getDataValueField());
//                     
// //             }
//         };


    public static final ArrangeAction MakeRow = new ArrangeAction(VueResources.local("menu.format.arrange.makerow"), keyStroke(KeyEvent.VK_1, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 2; }
            // todo bug: an already made row is shifting everything to the left
            // (probably always, actually)
  
            void arrange(LWSelection selection) {
                AlignCentersRow.arrange(selection);
                AlignCentersRow.oldCenterX = AlignCentersRow.centerX;
                AlignCentersRow.oldCenterY = AlignCentersRow.centerY;
                maxX = minX + totalWidth;
                DistributeHorizontally.arrange(selection);
                // note that we need to check the global selection, not the passed in selection,
                // as the passed in selection for arrange actions have links filtered out.
                if (VUE.getSelection().size() == viewer().getMap().getAllDescendents(LWContainer.ChildKind.EDITABLE).size()) {
                    // Would this feature be better be served by a general LWCAction flag that says
                    // at the end of the action, make sure the entire selection is visible on the
                    // map?  We could do a zoom-fit to the bounds of everything currently visible
                    // on the map, plus everything in the current selection.  This covers both
                    // cases of partial map selection and full-map selection.  We could skip
                    // the zoom fit entirely if the current selection is already fully visible.
                    ZoomTool.setZoomOutFit();
               }
            }
    };

    
    public static final ArrangeAction MakeColumn = new ArrangeAction(VueResources.local("menu.format.arrange.makecolumn"), keyStroke(KeyEvent.VK_2, ALT)) {
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
                //Log.debug("   VUE-SELECTION: " + VUE.getSelection());
                //Log.debug("ACTION-SELECTION: " + selection);
                // note that we need to check the global selection, not the passed in selection,
                // as the passed in selection for arrange actions have links filtered out.
                if (VUE.getSelection().size() == viewer().getMap().getAllDescendents(LWContainer.ChildKind.EDITABLE).size()) {
                    ZoomTool.setZoomOutFit();
                }
            }
        };
    
    public static final ArrangeAction DistributeVertically = new ArrangeAction(VueResources.local("menu.format.arrange.distributevertically"), keyStroke(KeyEvent.VK_V, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            // use only *2* in selection if use our minimum layout region setting
            void arrange(LWSelection selection) {
                LWComponent[] comps = sortByY(sortByX(selection.asArray()));
                float layoutRegion = maxY - minY;
                //if (layoutRegion < totalHeight)
                //  layoutRegion = totalHeight;
                float verticalGap = (layoutRegion - totalHeight) / (selection.size() - 1);
                float y;
                if(Float.isNaN(oldCenterY)){
                	y = minY;
                } else {
                    y = oldCenterY - layoutRegion/2;
                }
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = comps[i];
                    c.setLocation(c.getX(), y);
                    y += c.getHeight() + verticalGap;
                }
            }
        };
    
    public static final ArrangeAction DistributeHorizontally = new ArrangeAction(VueResources.local("menu.format.arrange.distributehorizontally"), keyStroke(KeyEvent.VK_H, ALT)) {
            boolean supportsSingleMover() { return false; }
            boolean enabledFor(LWSelection s) { return s.size() >= 3; }
            void arrange(LWSelection selection) {
                final LWComponent[] comps = sortByX(sortByY(selection.asArray()));
                final float layoutRegion = maxX - minX;
                //if (layoutRegion < totalWidth)
                //  layoutRegion = totalWidth;
                final float horizontalGap = (layoutRegion - totalWidth) / (selection.size() - 1);
                float x;
                if(Float.isNaN(oldCenterX)) {
                	x = minX;
                } else {
                 x=  oldCenterX - layoutRegion/2;
                }
                for (LWComponent c : comps) {
                    c.setLocation(x, c.getY());
                    x += c.getWidth() + horizontalGap;
                }
        }
            
    };
    
   
    /** Helpers for menu creation.  Null's indicate good places
     * for menu separators. */

    public static final Action[] ALIGN_MENU_ACTIONS = {
        AlignLeftEdges,
        AlignRightEdges,
        AlignTopEdges,
        AlignBottomEdges,
        null,
        AlignCentersRow,
        AlignCentersColumn,
        null,
        FillWidth,
        FillHeight
    };

    public static final Action[] ARRANGE_MENU_ACTIONS = {
        MakeRow,
        MakeColumn,
        null,
        LayoutAction.table,
        LayoutAction.circle,
        LayoutAction.filledCircle,
        LayoutAction.random,
        LayoutAction.ripple,
        LayoutAction.cluster2,
        null,
        PushOut,
        PullIn,
        //PushOutLinked,
        null,
        DistributeVertically,
        DistributeHorizontally,
        null,
        BringToFront,
        SendToBack
    };


    public static final LWCAction ImageToNaturalSize = new LWCAction(VueResources.local("action.makenaturalsize")) {
            @Override
                boolean enabledFor(LWSelection s) {
                return s.containsType(LWImage.class)
                    || s.containsType(LWNode.class); // todo: really, only image nodes, but we have no key for that
            }
            public void act(LWImage c) {
                c.setToNaturalSize();
            }
            public void act(LWNode n) {
                LWImage i = n.getImage();
                if (i != null)
                    i.setToNaturalSize();
              
            }
        };

    private static class ImageSizeAction extends LWCAction {
        final int size;
        ImageSizeAction(String name) {
            super(name);
            this.size = -1;
        }
        ImageSizeAction(String name, KeyStroke shortcut) {
            super(name, shortcut);
            this.size = -1;
        }
        ImageSizeAction(int size) {
            super(size + "x" + size);
            this.size = size;
        }

        @Override
        boolean enabledFor(LWSelection s) {
            return s.containsType(LWImage.class)
                || s.containsType(LWNode.class); // todo: really, only image nodes, but we have no key for that
        }
        
        protected void imageAct(LWImage im, Object actionKey) {
            final int newDim;

            //Log.debug(this + " on " + im);

            if (actionKey == IMAGE_SHOW) {
                if (im.isHidden(HideCause.IMAGE_ICON_OFF)) {
                    im.clearHidden(HideCause.IMAGE_ICON_OFF);
                    im.getParent().layout("imageIconShow");
                }
                return;
            }
            
            if (actionKey == IMAGE_HIDE)
                newDim = Integer.MIN_VALUE;
            else if (actionKey == IMAGE_BIGGER)
                newDim = getBiggerSize(im); // will return same size if is currently OFF
            else if (actionKey == IMAGE_SMALLER)
                newDim = getSmallerSize(im);
            else // actionKey is an Integer representing the new desired size
                newDim = (Integer) actionKey;
            
            if (DEBUG.IMAGE) Log.debug("NEWDIM " + newDim);
            
            if (newDim == Integer.MIN_VALUE) {
                // hide
                if (im.isNodeIcon() || im.getParent() instanceof LWNode) {
                    im.setHidden(HideCause.IMAGE_ICON_OFF);
                    im.getParent().layout("imageIconHide");
                }
            } else if (newDim == Integer.MAX_VALUE) {
                // make natural size
                im.setToNaturalSize();
                if (im.isNodeIcon()) {
                    im.clearHidden(HideCause.IMAGE_ICON_OFF);
                    im.getParent().layout("imageIconShow");
                }
            } else {
                // adjust size
                im.setMaxDimension(newDim);
                if (im.isNodeIcon()) {
                    im.clearHidden(HideCause.IMAGE_ICON_OFF);
                    im.getParent().layout("imageIconShow");
                }
            }
        }
        
        @Override
        public void act(LWImage im) {
            imageAct(im, size);
        }
        
        @Override
        public void act(LWNode n) {
            final LWImage image = n.getImage();
            if (image != null)
                act(image);
        }            
    }

    private static final Object IMAGE_BIGGER = "bigger";
    private static final Object IMAGE_SMALLER = "smaller";
    private static final Object IMAGE_HIDE = "hide";
    private static final Object IMAGE_SHOW = "show";
    
    private static final class ImageAdjustAction extends ImageSizeAction {
        final Object actionKey;
        ImageAdjustAction(String localizationKey, Object key) {
            super(VueResources.local(localizationKey));
            this.actionKey = key;
        }
        ImageAdjustAction(String localizationKey, Object key, KeyStroke shortcut) {
            super(VueResources.local(localizationKey), shortcut);
            this.actionKey = key;
        }
        
        @Override public void act(LWImage im) {
            imageAct(im, actionKey);
        }
    }

    private static final LWCAction ImageBigger = new ImageAdjustAction("action.image.bigger", IMAGE_BIGGER, keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND+SHIFT));
    private static final LWCAction ImageSmaller = new ImageAdjustAction("action.image.smaller", IMAGE_SMALLER, keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND+SHIFT));
    private static final LWCAction ImageHide = new ImageAdjustAction("action.image.hide", IMAGE_HIDE);
    private static final LWCAction ImageShow = new ImageAdjustAction("action.image.show", IMAGE_SHOW);
    

    private static final int ImageSizes[] = { 1024, 768, 640, 512, 384, 256, 128, 64, 32, 16 };

    public static final Action[] IMAGE_MENU_ACTIONS;
    public static final Action[] NODE_FORMAT_MENU_ACTIONS = {ResizeNode};

    static {

        IMAGE_MENU_ACTIONS = new Action[ImageSizes.length + 5];

        int i = 0;
        
        IMAGE_MENU_ACTIONS[i++] = ImageBigger;
        IMAGE_MENU_ACTIONS[i++] = ImageSmaller;
        IMAGE_MENU_ACTIONS[i++] = ImageToNaturalSize;

        for (int x = 0; x < ImageSizes.length; x++) {
            IMAGE_MENU_ACTIONS[i++] = new ImageSizeAction(ImageSizes[x]);
        }

        IMAGE_MENU_ACTIONS[i++] = ImageHide;
        IMAGE_MENU_ACTIONS[i++] = ImageShow;

    }

    /** @return the next biggest size, unless the image icon is currently hidden, in which case return same size */
    private static int getBiggerSize(LWImage c)
    {
        final int maxDim = (int) Math.max(c.getWidth(), c.getHeight());

        if (c.isHidden(HideCause.IMAGE_ICON_OFF))
            return maxDim;
        
        //Log.debug("BIGGER MAXDIM " + maxDim);
        
        for (int i = ImageSizes.length - 1; i >= 0; i--) {
            if (ImageSizes[i] > maxDim) 
                return ImageSizes[i];
        }
        return Integer.MAX_VALUE;
    }

    private static int getSmallerSize(LWImage c) {
        final int maxDim = (int) Math.max(c.getWidth(), c.getHeight());

        //Log.debug("SMALLER MAXDIM " + maxDim);
        
        for (int i = 0; i < ImageSizes.length; i++) {
            if (ImageSizes[i] < maxDim)
                return ImageSizes[i];
        }
        
        return ImageSizes[ImageSizes.length - 1];
        //return Integer.MIN_VALUE; // will hide the image instead of going to smallest
    }
    
    
    //-----------------------------------------------------------------------------
    // VueActions
    //-----------------------------------------------------------------------------
    public static final Action GatherWindows =
        new VueAction(VueResources.local("menu.windows.gather")) {
            boolean undoable() { return false; }
            protected boolean enabled() { return true; }
            public void act() 
            {
            	GUI.reloadGraphicsInfo();
            	GUI.invokeAfterAWT(new Runnable() { public void run() {
            		DockWindow acrossTop[] = new DockWindow[VUE.acrossTop.length];
            		System.arraycopy(VUE.acrossTop, 0, acrossTop, 0, VUE.acrossTop.length);
            		//acrossTop[VUE.acrossTop.length] = VUE.getMergeMapsDock();
            		//acrossTop[VUE.acrossTop.length+1] = VUE.getFormatDock();
            		//acrossTop[VUE.acrossTop.length+1] = VUE.getInteractionToolsDock();
            		VUE.getFormatDock().setLocation(150,150);
            		VUE.getMergeMapsDock().setLocation(150,150);
            		VUE.assignDefaultPositions(acrossTop);
            	}});
            }
        };
    public static final Action NewMap =
    new VueAction(VueResources.local("menu.file.new"), keyStroke(KeyEvent.VK_N, COMMAND+SHIFT), ":general/New") {
        private int count = 1;
        boolean undoable() { return false; }
        protected boolean enabled() { return true; }
        public void act() {
            VUE.displayMap(new LWMap(VueResources.local("vue.main.newmap") + count++));
        }
    };
    public static final Action Revert =
        //new VueAction("Revert", keyStroke(KeyEvent.VK_R, COMMAND+SHIFT), ":general/Revert") { // conflicts w/align centers in row
        //new VueAction("Revert", null, ":general/Revert") {            
        new VueAction(VueResources.local("menu.file.revert")) {
            boolean undoable() { return false; }
            protected boolean enabled() 
            { 
            	 
            		return true;
            }
            public void act() {
                
            	if (tufts.vue.VUE.getActiveMap().getFile() == null)
            	{
            		VueUtil.alert(VUE.getApplicationFrame(),
            				                      VueResources.local("dialog.revert.message"),
            				                      VueResources.local("dialog.revert.title"),
            				                      JOptionPane.PLAIN_MESSAGE);
              
            		return;
            	}
                	LWMap map = tufts.vue.VUE.getActiveMap();
                	VUE.closeMap(map,true);
                	tufts.vue.action.OpenAction.reloadMap(map);                	
                                
            }
        };
    public static final Action CloseMap =
    new VueAction(VueResources.local("menu.file.close"), keyStroke(KeyEvent.VK_W, COMMAND)) {
        // todo: listen to map viewer display event to tag
        // with currently displayed map name
        boolean undoable() { return false; }
        public void act() {
            VUE.closeMap(VUE.getActiveMap());
        }
    };
    public static final Action Undo =
    new VueAction(VueResources.local("action.undo"), keyStroke(KeyEvent.VK_Z, COMMAND), ":general/Undo") {
        boolean undoable() { return false; }
        public void act() { VUE.getUndoManager().undo(); }
        
    };
    public static final Action Redo =
    new VueAction(VueResources.local("action.redo"), keyStroke(KeyEvent.VK_Z, COMMAND+SHIFT), ":general/Redo") {
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
    new VueAction(VueResources.local("menu.view.zoomin"), keyStroke(KeyEvent.VK_EQUALS, COMMAND), ":general/ZoomIn") {
        public void act() {
            ZoomTool.setZoomBigger(null);
        }
    };
    public static final VueAction ZoomOut =
    new VueAction(VueResources.local("menu.view.zoomout"), keyStroke(KeyEvent.VK_MINUS, COMMAND), ":general/ZoomOut") {
        public void act() {
            ZoomTool.setZoomSmaller(null);
        }
    };
    public static final VueAction ZoomFit =
        new VueAction(VueResources.local("menu.view.fitinwin"), keyStroke(KeyEvent.VK_CLOSE_BRACKET, COMMAND), ":general/Zoom") {
        public void act() {
            ZoomTool.setZoomFit();
        }
    };
    public static final VueAction ZoomActual =
    new VueAction(VueResources.local("actions.zoomActual.label"), keyStroke(KeyEvent.VK_QUOTE, COMMAND)) {
        // no way to listen for zoom change events to keep this current
        //boolean enabled() { return VUE.getActiveViewer().getZoomFactor() != 1.0; }
        public void act() {
            ZoomTool.setZoom(1.0);
        }
    };
    
    public static final Action ZoomToSelection =
    new LWCAction(VueResources.local("menu.view.selecfitwin"), keyStroke(KeyEvent.VK_OPEN_BRACKET, COMMAND)) {
        public void act(LWSelection s) {
            MapViewer viewer = VUE.getActiveViewer();
            ZoomTool.setZoomFitRegion(viewer, s.getBounds(), 16, false);
        }
    };

    
    public static final VueAction SuperScreen =
        new VueAction("Use all screens for Full Screen")
        {
            private final Object INIT = "init";
            private boolean selected;

            private final boolean ViswallMode;

            //--------------------------------------------
            // anonymous constructor init:
            {
                boolean foundViswall = false;
                
                try {
                    foundViswall = checkForViswall();
                } catch (Throwable t) {
                    Log.info("checking for viswall", t);
                }

                ViswallMode = foundViswall;
                if (!foundViswall)
                    update(INIT);
            }
            //--------------------------------------------

            boolean checkForViswall() {
                String host = System.getenv("HOST");
                if (host == null) host = System.getenv("HOSTNAME");
                if (host == null) host = System.getenv("COMPUTERNAME");
                if (host == null) host = System.getenv("USERDOMAIN");

                Rectangle specialBounds = null;
                
                if (false) { // testing
                    specialBounds = new Rectangle(128,128, 640,480);
                }
                else if ("VISWALL-WIN32".equalsIgnoreCase(host) && tufts.vue.gui.Screen.getAllScreens().length == 9) {
                    // TODO: The below configuration(s) need testing and may need adjusting:
                    // specialBounds = new Rectangle(1920,-1080, 4096,2160); // upper logical stero display
                       specialBounds = new Rectangle(1920,    0, 4096,2160); // lower logical stero display
                }
                //else if ("insert-viswall-linux-hostname" etc..
                // // config linux viswall bounds...
                //}

                if (specialBounds != null) {
                    // manually init the action, as that will (must) be skipped when we return true:
                    GUI.setSpecialWorkingBounds(specialBounds);
                    setEnabled(true);
                    setActionName("Enable Tufts VISWALL");
                    return true;
                } else {
                    return false;
                }
            }
            
            boolean undoable() { return false; }
            protected boolean enabled() { return true; }
            public void act() {
                GUI.reloadGraphicsInfo();
                update("firing");
                if (isEnabled()) {
                    
                    selected = !selected; // this line changes behavior of GUI.setFullScreen
                    
                    if (VUE.inWorkingFullScreen() && !VUE.inNativeFullScreen()) {
                        tufts.vue.gui.GUI.setFullScreen(GUI.getFullScreenWindow());
                    }
                    
                } else {
                    selected = false;
                }
            }
            @Override public Boolean getToggleState() {
        	return selected ? Boolean.TRUE : Boolean.FALSE;
            }
            // for GUI.java -- would be better as a listener
            @Override public void update(Object key) {
                if (DEBUG.Enabled) Log.debug("SuperScreen update: " + Util.tags(key));
                if (ViswallMode)
                    return;
                if (GUI.hasMultipleScreens()) {
                    java.awt.Rectangle b = GUI.getAllScreenBounds();
                    setActionName(String.format("All Screens (%dx%d)", b.width, b.height));
                    setEnabled(true);
                } else {
                    setEnabled(false);
                    setActionName("All Screens");
                }
            }
        };

    public static final VueAction KioskScreen =
	    new VueAction(VueResources.getString("kiosk.action"))
	    {
	        private final Object INIT = "init";
	        private boolean selected;
	
	        boolean undoable() { return false; }
	        protected boolean enabled() { return true; }
	        
	        KioskThread kt =null;//
	        Thread t = null;// new Thread(kt);
	        
	        public void act() 
	        {
	        	if (t == null)
	        	{
	        		VUE.toggleFullScreen(false,true);
	        		kt = new KioskThread();
	        		
	        		t = new Thread(kt);
	        		t.setPriority(Thread.MAX_PRIORITY);
	        		t.start();
	        	}
	        	else
	        	{
			        	VUE.toggleFullScreen(false,true);
	        			kt.done();
	        			t =null;
	        			
	        	}
	           
	        }
	        @Override public Boolean getToggleState() {
	    	return selected ? Boolean.TRUE : Boolean.FALSE;
	        }
	        
	    	class KioskThread implements Runnable
	    	{
	    		private boolean done = false;
	    		public void done()
	    		{
	    			done=true;
	    		}
	    		public void run()
	    		{	    			
                    
	    			int dx = 0;
	    			MapViewer mv = FullScreen.getLastActive();
	    			MapViewer mv2 = VUE.getActiveViewer();
	    			ZoomTool.setZoom(mv,mv2.getZoomFactor());
    				LWMap map = VUE.getActiveMap();
    				double maxX; 
    				double minX;
    				int mvWidth;
    				
	    			while (true)
	    			{
	    				if (done)
	    					return;
	    				
	    				 maxX =  mv.getVisibleBounds().getMaxX();
	     				 minX =mv.getVisibleBounds().getMinX(); 
	     				 mvWidth = mv.getWidth();	    				
	    				
//	    				System.out.println("Visible Bounds Max X:" + mv.getVisibleBounds().getMaxX() + " ::: " + mv.getWidth() + " :::" + mv.getVisibleBounds().getMinX());
	    				if ( maxX <	 mvWidth)
	    				{	    						    				
	    					mv.panScrollRegion((int)dx, (int)0,false);
		    				mv2.panScrollRegion((int)dx, (int)0,false);
	    				}

	    				try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
					
						}
						
						dx =1;
	    			}
	    			
	    		}
	    	}
	    };

	    
    public static final VueAction ToggleFullScreen =
        new VueAction(VueResources.local("menu.view.fullscreen"), keyStroke(KeyEvent.VK_BACK_SLASH, COMMAND)) {
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
        new VueAction(VueResources.local("menu.view.slidethumbnails"), keyStroke(KeyEvent.VK_T, SHIFT+COMMAND)) {
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
        new VueAction(VueResources.local("menu.view.splitscreen"), keyStroke(KeyEvent.VK_BACK_SLASH, COMMAND+SHIFT)) {
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
    

        public static final Action ToggleLinks =
            new VueAction(VueResources.local("menu.view.hideLinks"), keyStroke(KeyEvent.VK_L, CTRL_ALT)) {
            public void act() {
            	Actions.toggleLinkVisiblity();
            }
            
            public Boolean getToggleState() {
                return areLinksFiltered();
            }
        };
            	
        /*
         * I think because of the way this is proposed to work
         * we can't maintain a static state we have to always calculate the state
         * based on the selection. -MK
         */
        static void toggleLinkVisiblity() {

        	boolean filtered = areLinksFiltered();
    		LWSelection s = VUE.getSelection();
        	if (s.size() > 0) {
        		Iterator it = s.iterator();
        		for (LWComponent c : s) {
    				if (c instanceof LWLink) {
    					((LWLink)c).setFiltered(!filtered);
    				}
    			}
        	}
        	else 
        	for (LWComponent c : VUE.getActiveViewer().getMap().getAllDescendents()) {
               if (c instanceof LWLink)
                    ((LWLink)c).setFiltered(!filtered);
            }
        	VUE.getActiveViewer().repaint();
    	}
        
        static Boolean areLinksFiltered()
        {
        	LWSelection s = VUE.getSelection();
        	if (s.size() > 0) {
        		Iterator it = s.iterator();
        		for (LWComponent c : s) {
    				if (c instanceof LWLink) {
    					boolean isFiltered = ((LWLink)c).isFiltered();
    					
    					if (isFiltered)
    						return true;
    				}
    			}
        		return false;
        	}
        	else 
        	for (LWComponent c : VUE.getActiveViewer().getMap().getAllDescendents()) 
        	{
        		if (c instanceof LWLink)        	
                {
                    boolean isFiltered = ((LWLink)c).isFiltered();
                    
                    if (isFiltered)
                    	return true;
                }
        	}
        	return false;
        }
    public static final VueAction TogglePruning =
        new VueAction(VueResources.local("menu.view.pruning"), keyStroke(KeyEvent.VK_J, COMMAND)) {
        public void act() {
            final boolean wasEnabled = togglePruningEnabled();
            
            // Currently, this action is ONLY fired via a menu item.  If other code points might
            // set this directly (the global pruning state), this should be changed to a
            // toggleState action (impl getToggleState), and those code points should call this
            // action to do the toggle, so the menu item checkbox state will stay synced.

            VUE.layoutAllMaps(HideCause.PRUNE);
            viewer().repaint();
            
//             if (wasEnabled) {
//                 // turning off pruning
//                 for (LWMap map : VUE.getAllMaps()) {
//                     for (LWComponent c : map.getAllDescendents()) {
//                         c.clearHidden(HideCause.PRUNE);
//                         if (c instanceof LWLink)
//                             ((LWLink)c).clearPrunes();
//                     }
//                 }
//                 VUE.layoutAllMaps(HideCause.PRUNE);
//             } else {
//                 // turning on pruning -- show prune controls on any selected links
//                 viewer().repaint();
//             }
        }
    };

    private static boolean togglePruningEnabled() {
        final boolean wasEnabled = LWLink.isPruningEnabled();

        LWLink.setPruningEnabled(!wasEnabled);

        setAllPruneHidesEnabled(!wasEnabled);

        return wasEnabled;
    }

    
    public static final VueAction ClearAllPruning =
        new VueAction(VueResources.local("menu.view.clearpruning")) {
        public void act() {
            clearAllPruneStates(viewer().getMap());
            viewer().repaint();
        }
    };

    /** erase all pruning state from the given map */
    private static void clearAllPruneStates(LWMap map) {
        for (LWComponent c : map.getAllDescendents()) {
            c.setPruned(false);
            c.clearHidden(HideCause.PRUNE);
            if (c instanceof LWLink)
                ((LWLink)c).clearUserPrunes();
        }
    }
    

    private static void setAllPruneHidesEnabled(final boolean enable) {
        for (LWMap map : VUE.getAllMaps()) {
            for (LWComponent c : map.getAllDescendents()) {
                if (c.isPruned())
                    c.setHidden(HideCause.PRUNE, enable);
            }
        }
    }
    

    public static final VueAction ToggleLinkLabels =
        new VueAction("Link Labels") {
        public void act() {
            boolean enabled = LWLink.isDisplayLabelsEnabled();

            // Currently, this action is ONLY fired via a menu item.  If other code
            // points might set this directly, this should be changed to a toggleState
            // action (impl getToggleState), and those code points should call this
            // action to do the toggle, so the menu item checkbox state will stay
            // synced.

            LWLink.setDisplayLabelsEnabled(!enabled);

            VUE.getActiveMap().notify(this, LWKey.Repaint);
        }
    };
    
    public static final VueAction ToggleAutoZoom =
        
        // 'E' chosen for temporary mac shortcut until we find a workaround for not
        // being able to use Alt-Z because it's on the left of the keyboard, and it's
        // not 'W', which if the user accidently hits COMMAND-W, the map will close
        // (todo: see about just changing the Close shortcut entirely or getting rid of
        // it)

        new VueAction(VueResources.local("menu.format.autozoom"), keyStroke(KeyEvent.VK_E, COMMAND+SHIFT))
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
    
    public static final LWCAction NewSlide = new LWCAction(VueResources.local("actions.newSlide.label")) {        
                public void act(Iterator i) {
                    VUE.getActivePathway().add(i);
                    GUI.makeVisibleOnScreen(VUE.getActiveViewer(), PathwayPanel.class);
                    
                }
                boolean enabledFor(LWSelection s) {
                    // items can be added to pathway as many times as you want
                    return VUE.getActivePathway() != null && s.size() > 0;
                }            
     };
     
     public static final LWCAction MergeNodeSlide = new LWCAction(VueResources.local("actions.mergeNode.label")) {        
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
    new NewItemAction(VueResources.local("menu.content.addnode"), keyStroke(KeyEvent.VK_N, COMMAND)) {
        @Override
        LWComponent createNewItem() {
            return NodeModeTool.createNewNode();
        }
    };

    //This doesn't really make a lot of sense to have 2 methods do the
    //same thing but my MapViewer.java is a bit decomposed at the moment so
    //TODO: Come back here eliminate one of these and only call one from mapviewer.
    //MK
    public static final VueAction NewRichText =
    new NewItemAction(VueResources.local("menu.content.addtext"), keyStroke(KeyEvent.VK_T, COMMAND)) {
        @Override
        LWComponent createNewItem() {
            return NodeModeTool.createRichTextNode(VueResources.local("newtext.html"));
        }
    };


    public static final Action[] NEW_OBJECT_ACTIONS = {
        NewNode,
        NewRichText,
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
            final Point currentMouse = viewer.getLastMousePressPoint();
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

    public static final Action ResourcesAction = new ResourcesActionClass(MENU_INDENT + VueResources.local("dockWindow.contentPanel.resources.title"));

    public static class ResourcesActionClass extends VueAction {
        public ResourcesActionClass(String s) {
            super(s);
        }

        public void act() {
            VUE.getContentDock().setVisible(true);
            VUE.getContentPanel().showResourcesTab();
        }
    };

    public static final Action DatasetsAction = new DatasetsActionClass(MENU_INDENT + VueResources.local("dockWindow.contentPanel.datasets.title"));

    public static class DatasetsActionClass extends VueAction {
        public DatasetsActionClass(String s) {
            super(s);
        }

        public void act() {
            VUE.getContentDock().setVisible(true);
            VUE.getContentPanel().showDatasetsTab();
        }
    };

    public static final Action OntologiesAction = new OntologiesActionClass(MENU_INDENT + VueResources.local("dockWindow.contentPanel.ontologies.title"));

    public static class OntologiesActionClass extends VueAction {
        public OntologiesActionClass(String s) {
            super(s);
        }

        public void act() {
            VUE.getContentDock().setVisible(true);
            VUE.getContentPanel().showOntologiesTab();
        }
    };
}