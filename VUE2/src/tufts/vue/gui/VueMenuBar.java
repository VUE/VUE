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
package tufts.vue.gui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.action.*;

import edu.tufts.vue.ontology.action.OntologyControlsOpenAction;

import java.io.File;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;

/**
 * The main VUE application menu bar.
 *
 * @version $Revision: 1.112 $ / $Date: 2008-07-16 18:58:59 $ / $Author: anoop $
 * @author Scott Fraize
 */
public class VueMenuBar extends javax.swing.JMenuBar
    implements java.awt.event.FocusListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(VueMenuBar.class);
    
    public static VueMenuBar RootMenuBar;
    private static JCheckBoxMenuItem fullScreenToolbarItem = null;
    // this may be created multiple times as a workaround for the inability
    // to support a single JMenuBar for the whole application on the Mac
/*    public VueMenuBar()
    {
        this(VUE.ToolWindows);
    }
*/
    /*
      public void paint(Graphics g) {
      System.err.println("\nVueMenuBar: paint");

      }
    */

    private static class VueMenu extends JMenu {
        private boolean unadjusted = true;
        
        VueMenu(String name) {
            super(name);
            
            /* on the mac this works fine on windows the menus paint behind the dockwindows
             * so we'll install our own UI which creates heavyweight popups.
             */
            if (Util.isWindowsPlatform() ||Util.isUnixPlatform())
            	this.getPopupMenu().setUI(new VuePopupMenuUI());
            
                       
        }

        @Override
        public void addNotify() {
            if (unadjusted) {
                GUI.adjustMenuIcons(this);
                unadjusted = false;
            }
            super.addNotify();
        }

//         @Override
//         protected JMenuItem createActionComponent(Action a) {

//             JMenuItem mi;

//             if (false && a == Actions.ToggleAutoZoom) {
//                 mi = new JCheckBoxMenuItem((String)a.getValue(Action.NAME), (Icon)a.getValue(Action.SMALL_ICON));
//             } else {
//                 //mi = new JMenuItem((String)a.getValue(Action.NAME), (Icon)a.getValue(Action.SMALL_ICON));
//                 mi = new JMenuItem(a);
                
// //                     protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
// //                         PropertyChangeListener pcl = createActionChangeListener(this);
// //                         if (pcl == null) {
// //                             pcl = super.createActionPropertyChangeListener(a);
// //                         }
// //                         return pcl;
// //                     }
// //                 };
//             }
//             mi.setHorizontalTextPosition(JButton.TRAILING);
//             mi.setVerticalTextPosition(JButton.CENTER);
//             mi.setEnabled(a.isEnabled());   
//             return mi;
//         }
        
    }

    private static JMenu makeMenu(String name) {
        return new VueMenu(name);
        
//         if (Util.isMacPlatform())
//             return new JMenu(name);
//         else
//             return new VueMenu(name);
    }


//     private static class MenuToggleItem extends JCheckBoxMenuItem
//     {
//         MenuToggleItem(VueAction va) {
//             super(va);
//             va.trackToggler(this);
//         }
// //         @Override
// //         public final void setSelected(boolean selected) {
// //             if (DEBUG.EVENTS) Log.debug(GUI.name(this) + "; setSelected " + selected + " (isSelected=" + isSelected() + ")");
// //             super.setSelected(selected);
// //         }
//     }

    private static JCheckBoxMenuItem makeCheckBox(VueAction a) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(a);
        //final JCheckBoxMenuItem item = new JCheckBoxMenuItem(a.getPermanentActionName());
        item.setAccelerator(a.getKeyStroke());
        return item;
    }
    private static JCheckBoxMenuItem makeLinkedCheckBox(VueAction a) {
        final JCheckBoxMenuItem item = makeCheckBox(a);
        a.trackToggler(item);
        return item;
    }
    

    private final JCheckBoxMenuItem viewFullScreen = makeCheckBox(Actions.ToggleFullScreen);

    public VueMenuBar()
    {
        final int metaMask = tufts.vue.Actions.COMMAND;
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Top Level Menus
        ////////////////////////////////////////////////////////////////////////////////////

        final JMenu fileMenu = makeMenu("File");
        final JMenu recentlyOpenedMenu = makeMenu("Open recent");
        final JMenu editMenu = makeMenu("Edit");
        final JMenu viewMenu = makeMenu("View");
        final JMenu formatMenu = makeMenu("Format");
        final JMenu transformMenu = makeMenu("Font");
        final JMenu arrangeMenu = makeMenu("Arrange");
        final JMenu contentMenu = makeMenu("Content");
        final JMenu presentationMenu = makeMenu(VueResources.getString("menu.pathway.label"));
        final JMenu analysisMenu = makeMenu("Analysis");
        final JMenu windowMenu = makeMenu("Windows");
        final JMenu alignMenu = makeMenu("Align");
        final JMenu extendMenu = makeMenu("Extend");
        final JMenu linkMenu = makeMenu("Link");
        final JMenu helpMenu = add(makeMenu("Help"));
        
      //  final JMenu slidePreviewMenu = new JMenu("Slide preview");
        final JMenu notesMenu = new JMenu("Handouts and Notes");
        final JMenu playbackMenu = new JMenu(VueResources.getString("menu.playback.play.label"));
        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Actions
        ////////////////////////////////////////////////////////////////////////////////////

        final JMenuItem splitScreenItem = new JCheckBoxMenuItem(Actions.ToggleSplitScreen);
        final JMenuItem toggleSlideIconsItem = makeLinkedCheckBox(Actions.ToggleSlideIcons);
        final JMenuItem togglePruningItem = new JCheckBoxMenuItem(Actions.TogglePruning);

        final JMenuItem toggleAutoZoomItem = makeCheckBox(Actions.ToggleAutoZoom);
        toggleAutoZoomItem.setSelected(Actions.ToggleAutoZoom.getToggleState());

        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Actions
        ////////////////////////////////////////////////////////////////////////////////////
                
        final SaveAction saveAction = new SaveAction("Save", false);
        final SaveAction saveAsAction = new SaveAction("Save As...");
        //final SaveAction exportAction = new SaveAction("Export ...",true,true);
        final OpenAction openAction = new OpenAction("Open...");
        final ExitAction exitAction = new ExitAction("Quit");
        final JMenu publishMenu = makeMenu("Publish");
        final JMenu dataMenu = makeMenu("Import Dataset");
        //final JMenu publishAction =  Publish.getPublishMenu();
        final RDFOpenAction rdfOpen = new RDFOpenAction();
        
        final TextOpenAction textOpen = new TextOpenAction();
        final CreateCM createCMAction = new CreateCM("Connectivity Analysis...");
        final AnalyzeCM analyzeCMAction = new AnalyzeCM("Merge Maps...");
        final OntologyControlsOpenAction ontcontrls = new OntologyControlsOpenAction("Ontologies");

        // Actions added by the power team
        final PrintAction printAction = PrintAction.getPrintAction();
    //    final PDFTransform pdfAction = new PDFTransform("PDF");
    /*    final HTMLConversion htmlAction = new HTMLConversion("HTML");
        final ImageConversion imageAction = new ImageConversion("JPEG");
        final ImageMap imageMap = new ImageMap("IMAP");
        final SVGConversion svgAction = new SVGConversion("SVG");
        final XMLView xmlAction = new XMLView("XML View");*/
        final RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();

        rofm.getPreference().addVuePrefListener(new VuePrefListener()
        {
			public void preferenceChanged(VuePrefEvent prefEvent) {
				rebuildRecentlyOpenedItems(fileMenu, recentlyOpenedMenu, rofm);
				
			}
        	
        });
        // create menu for dataset import
        for(edu.tufts.vue.dataset.AbstractLayout layout: edu.tufts.vue.dataset.LayoutFactory.getInstance().getAvailableLayouts()) {
            dataMenu.add(layout);
        }
         
        
        edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().addDataSourceListener(new edu.tufts.vue.dsm.DataSourceListener() {
            public void changed(edu.tufts.vue.dsm.DataSource[] dataSource) {
                if (DEBUG.Enabled) Log.debug("DataSourceListener: data sources changed");
           
                int count = 0;
                publishMenu.removeAll();
                boolean fedoraFlag = false;
                boolean sakaiFlag = false;
                for(int i =0;i<dataSource.length &&  !(fedoraFlag && sakaiFlag);i++) {
                 try {
                     final org.osid.repository.Repository r = dataSource[i].getRepository();
                     if (r == null) {
                         Log.warn("null repository in " + dataSource[i]);
                         continue;
                     }
                     if (r.getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE)) {
                       fedoraFlag = true;
                     } else if (r.getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                         sakaiFlag = true;
                     }
                 } catch(org.osid.repository.RepositoryException ex) {
                     Log.error("changed:", ex);
                 }
                }
                if(fedoraFlag) publishMenu.add(PublishActionFactory.createPublishAction(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE));
                if(sakaiFlag) publishMenu.add(PublishActionFactory.createPublishAction(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE));
                if(fedoraFlag || sakaiFlag) {
                    publishMenu.addSeparator();    
                    for(int i =0;i<dataSource.length;i++) {
                        try {
                            final org.osid.repository.Repository r = dataSource[i].getRepository();
                            if (r == null) {
                                Log.warn("null repository in " + dataSource[i]);
                                continue;
                            }
                            if (r.getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.FEDORA_REPOSITORY_TYPE) ||
                                    r.getType().isEqual(edu.tufts.vue.dsm.DataSourceTypes.SAKAI_REPOSITORY_TYPE)) {
                                publishMenu.add(PublishActionFactory.createPublishAction(dataSource[i]));
                                count++;
                            }
                        } catch(org.osid.repository.RepositoryException ex) {
                            Log.error("changed:", ex);
                        }
                    }
                } else {
                    publishMenu.add((createWindowItem(VUE.getContentDock(),KeyEvent.VK_5, "Add publishable resources through Resource Window")));
                }
                publishMenu.setEnabled(true);
                fileMenu.remove(publishMenu);
                fileMenu.add(publishMenu,11);
                
            }
        });
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Initializing DEBUG code
        ////////////////////////////////////////////////////////////////////////////////////

        if (false && DEBUG.Enabled) {
            // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG:
            // we're hitting bug in java (1.4.2, 1.5) on Tiger (OSX 10.4.2) here
            // (apple.laf.ScreenMenuBar array index out of bounds exception)
            JButton u = new JButton(Actions.Undo);
            JButton r = new JButton(Actions.Redo);
            JButton p = new JButton(printAction);
            JButton v = new JButton(printAction);
            v.setText("Print Visible");
            
            u.setBackground(Color.white);
            r.setBackground(Color.white);
            add(u).setFocusable(false);
            add(r).setFocusable(false);
            add(p).setFocusable(false);
            add(v).setFocusable(false);

            //menuBar.add(new tufts.vue.gui.VueButton(Actions.Undo)).setFocusable(false);
            // not picking up icon yet...
        }
        /*
        if (false && DEBUG.Enabled) {
            // THIS CODE IS TRIGGERING THE TIGER ARRAY BOUNDS BUG (see above)
            JMenu exportMenu = add(makeMenu("Export"));
            exportMenu.add(htmlAction);
           // exportMenu.add(pdfAction);
            exportMenu.add(imageAction);
            exportMenu.add(svgAction);
            exportMenu.add(xmlAction);
            exportMenu.add(imageMap);
        }
        */
        ////////////////////////////////////////////////////////////////////////////////////
        // Build File Menu
        ////////////////////////////////////////////////////////////////////////////////////

        fileMenu.add(Actions.NewMap);
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(Actions.CloseMap);
        fileMenu.addSeparator();
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask+Event.SHIFT_MASK));                
        fileMenu.add(Actions.Revert);
        fileMenu.addSeparator();   
        fileMenu.add(dataMenu);
        String includeText = VueResources.getString("text.file.menu.include");
        if(includeText != null && includeText.equals("TRUE"))
        {
          fileMenu.add(textOpen);
        }
        //fileMenu.add(exportAction);
        fileMenu.add(rdfOpen);
        publishMenu.setEnabled(false);
        fileMenu.add(publishMenu);
        
        final JMenu pdfExportMenu = new JMenu("Export Handouts and Notes (PDF)");
        //pdfExportMenu.add(Actions.MapAsPDF);
        final JMenuItem fullPageSlideNotesItem = new JMenuItem(Actions.FullPageSlideNotes);
        final JMenuItem slides8PerPageItem = new JMenuItem(Actions.Slides8PerPage);
        final JMenuItem speakerNotes1Item = new JMenuItem(Actions.SpeakerNotes1);
        final JMenuItem speakerNotes4Item = new JMenuItem(Actions.SpeakerNotes4);
        final JMenuItem audienceNotesItem = new JMenuItem(Actions.AudienceNotes);
        final JMenuItem speakerNotesOutlineItem = new JMenuItem(Actions.SpeakerNotesOutline);
        final JMenuItem nodeNotes4Item = new JMenuItem(Actions.NodeNotes4);
        final JMenuItem nodeNotesOutlineItem = new JMenuItem(Actions.NodeNotesOutline);
        
        pdfExportMenu.add(fullPageSlideNotesItem);
        pdfExportMenu.add(slides8PerPageItem);
        pdfExportMenu.add(speakerNotes1Item);
        pdfExportMenu.add(speakerNotes4Item);
        pdfExportMenu.add(audienceNotesItem);        
        pdfExportMenu.add(speakerNotesOutlineItem);
        pdfExportMenu.addSeparator();
        pdfExportMenu.add(nodeNotes4Item);
        pdfExportMenu.add(nodeNotesOutlineItem);        
      
        fileMenu.addMenuListener(new MenuListener(){
			public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation() {
				LWPathway p =VUE.getActivePathway();
				if (p == null || p.length() == 0)
				{
					fullPageSlideNotesItem.setEnabled(false);
					slides8PerPageItem.setEnabled(false);
					speakerNotes1Item.setEnabled(false);
					speakerNotes4Item.setEnabled(false);
					audienceNotesItem.setEnabled(false);
					speakerNotesOutlineItem.setEnabled(false);
					
				}
					//pdfExportMenu.setEnabled(false);			
				else
				{
					fullPageSlideNotesItem.setEnabled(true);
					slides8PerPageItem.setEnabled(true);
					speakerNotes1Item.setEnabled(true);
					speakerNotes4Item.setEnabled(true);
					audienceNotesItem.setEnabled(true);
					speakerNotesOutlineItem.setEnabled(true);
				}
				if (VUE.getActiveMap()!=null && VUE.getActiveMap().hasContent())
				{
					nodeNotesOutlineItem.setEnabled(true);
					nodeNotes4Item.setEnabled(true);
				}
				else
				{
					nodeNotesOutlineItem.setEnabled(false);
					nodeNotes4Item.setEnabled(false);
				}
					//pdfExportMenu.setEnabled(true);				
			}});                   
      //  pdfExportMenu.add(Actions.NodeNotesOutline);
        fileMenu.addSeparator();
        fileMenu.add(printAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, metaMask));
        fileMenu.add(printAction).setText("Print Visible...");
        fileMenu.add(pdfExportMenu);        
        rebuildRecentlyOpenedItems(fileMenu, recentlyOpenedMenu, rofm);
      
        if (VUE.isApplet() || (VUE.isSystemPropertyTrue("apple.laf.useScreenMenuBar") && GUI.isMacAqua())) {
            // Do NOT add quit to the file menu.
            // Either we're an applet w/no quit, or it's already in the mac application menu bar.
            // FYI, MRJAdapter.isSwingUsingScreenMenuBar() is not telling us the truth.
        } else {
            fileMenu.addSeparator();
            fileMenu.add(exitAction);
        }
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Build Edit Menu
        ////////////////////////////////////////////////////////////////////////////////////
        
        editMenu.add(Actions.Undo);
        editMenu.add(Actions.Redo);
        editMenu.addSeparator();
        editMenu.add(Actions.Cut);
        editMenu.add(Actions.Copy);
        editMenu.add(Actions.Paste);
        editMenu.add(Actions.Duplicate);
        editMenu.add(Actions.Rename);
        editMenu.add(Actions.Delete);                        
        editMenu.addSeparator();
        editMenu.add(Actions.SelectAll);
        editMenu.add(Actions.SelectAllNodes);
        editMenu.add(Actions.SelectAllLinks);
        editMenu.add(Actions.Reselect);
        editMenu.add(Actions.DeselectAll);
        if (!tufts.Util.isMacPlatform())
        {   editMenu.addSeparator();
            editMenu.add(Actions.Preferences);
        }
        
        if (DEBUG.IMAGE)
            editMenu.add(Images.ClearCacheAction);

        ////////////////////////////////////////////////////////////////////////////////////
        // Build View Menu
        ////////////////////////////////////////////////////////////////////////////////////
                
        viewMenu.add(Actions.ZoomIn);
        viewMenu.add(Actions.ZoomOut);
        viewMenu.add(Actions.ZoomFit);
        viewMenu.add(Actions.ZoomActual);
        viewMenu.add(Actions.ZoomToSelection);
        viewMenu.addSeparator();            
        if (!Util.isUnixPlatform())
        	viewMenu.add(viewFullScreen);
        viewMenu.add(splitScreenItem);
        viewMenu.addSeparator();
        viewMenu.add(toggleSlideIconsItem);
        viewMenu.addSeparator();
        viewMenu.add(togglePruningItem);
        
        // JAVA BUG: ADDING A JMenuItem (maybe just JCheckBoxMenuItem)
        // already constructe, instead of letting the menu code do it
        // itself, breaks the display of accelerators in the item:
        // anything other than APPLE (ctrl/alt) display as the APPLE
        // glpyh.  This is true on Tiger java 1.5, Leopard java 1.5
        // and 1.6 -- SMF 2008-05-31
        
        viewMenu.add(toggleAutoZoomItem);
        if (DEBUG.Enabled && DEBUG.KEYS) viewMenu.add(Actions.ToggleAutoZoom);
        

//         GUI.getFullScreenWindow().addWindowFocusListener(new WindowFocusListener()
//         {
//             public void windowGainedFocus(WindowEvent arg0) {
//                 viewFullScreen.setSelected(true);
//             }
            
//             public void windowLostFocus(WindowEvent arg0) {
//                 viewFullScreen.setSelected(false);
//             }
//             });

        ////////////////////////////////////////////////////////////////////////////////////
        // Build Format Menu
        ////////////////////////////////////////////////////////////////////////////////////
        
        if (VUE.getFormatDock() != null)
        {
        	formatMenu.add(createWindowItem(VUE.getFormatDock(),-1,"Formatting Palette"));
        	formatMenu.addSeparator();
        }
        
        formatMenu.add(Actions.CopyStyle);
        formatMenu.add(Actions.PasteStyle);
        formatMenu.addSeparator();        
        
        //build format submenus...
        buildMenu(extendMenu,Actions.EXTEND_MENU_ACTIONS);
        buildMenu(alignMenu, Actions.ARRANGE_MENU_ACTIONS);
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.SendBackward);
        transformMenu.add(Actions.FontSmaller);
        transformMenu.add(Actions.FontBigger);
        transformMenu.add(Actions.FontBold);
        transformMenu.add(Actions.FontItalic);                
        formatMenu.add(transformMenu);        
        formatMenu.add(arrangeMenu);
        formatMenu.add(alignMenu);
        formatMenu.add(extendMenu);
        formatMenu.addSeparator();
        formatMenu.add(Actions.Group);
        formatMenu.add(Actions.Ungroup);
        formatMenu.addSeparator();
        buildMenu(linkMenu, Actions.LINK_MENU_ACTIONS);
        formatMenu.add(linkMenu);
        
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Build Content Menu
        ////////////////////////////////////////////////////////////////////////////////////
        GUI.addToMenu(contentMenu, Actions.NEW_OBJECT_ACTIONS);
        
        final JMenuItem addFileItem = new JMenuItem(Actions.AddFileAction);
        final JMenuItem addURLItem = new JMenuItem(Actions.AddURLAction);
        final JMenuItem newSlideItem = new JMenuItem(Actions.NewSlide);
        final JMenuItem newMergeNodeItem = new JMenuItem(Actions.MergeNodeSlide);        
        final JMenuItem removeResourceItem = new JMenuItem(Actions.RemoveResourceAction);
        
        contentMenu.add(addFileItem);
        contentMenu.add(addURLItem);
        contentMenu.add(removeResourceItem);
        
        formatMenu.addMenuListener(new MenuListener(){        	
        	public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation()
			{
				LWSelection selection = VUE.getSelection();
				if (selection.size() > 1 && selection.countTypes(LWLink.class) ==0)
				{
					extendMenu.setEnabled(true);
				}
				else
				{
					extendMenu.setEnabled(false);
				}
				
				if (selection.countTypes(LWText.class) > 0)
				{
					transformMenu.setEnabled(false);
				}
				else
				{
					transformMenu.setEnabled(true);
				}
				
			}});
        contentMenu.addMenuListener(new MenuListener(){
			public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation()
			{
				LWComponent c =VUE.getActiveComponent();
				if (c instanceof LWNode)
				{
					if ((c).hasResource())
					{
					
						removeResourceItem.setEnabled(true);
						addFileItem.setLabel(VueResources.getString("mapViewer.componentMenu.replaceFile.label"));
						addURLItem.setLabel(VueResources.getString("mapViewer.componentMenu.replaceURL.label"));						
					}
					else
					{
						removeResourceItem.setEnabled(false);
						addFileItem.setLabel(VueResources.getString("mapViewer.componentMenu.addFile.label"));
						addURLItem.setLabel(VueResources.getString("mapViewer.componentMenu.addURL.label"));
					}
				}
			}});
        
  //      contentMenu.addSeparator();
  //      contentMenu.add(Actions.AddResource);
  //      contentMenu.add(Actions.UpdateResource).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, metaMask));
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Build Presentation Menu
        ////////////////////////////////////////////////////////////////////////////////////
        if (VUE.getPresentationDock() != null)
        {
        	presentationMenu.add(createWindowItem(VUE.getPresentationDock(),-1, null));        
        	presentationMenu.addSeparator();
    
        }
        
        presentationMenu.addMenuListener(new MenuListener(){
			public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation()
			{
				LWComponent c =VUE.getActiveComponent();
				if (c instanceof LWNode)
				{
					if (c instanceof LWPortal)
					{					
						newSlideItem.setEnabled(false);
						newMergeNodeItem.setEnabled(false);										
					}					
					else
					{
						newSlideItem.setEnabled(true);
						newMergeNodeItem.setEnabled(true);
					}
				}
			}});
        presentationMenu.add(newSlideItem);
        presentationMenu.add(newMergeNodeItem);
        presentationMenu.addSeparator();
        if (VUE.getSlideDock() != null) {
            presentationMenu.add(Actions.MasterSlide);                
            presentationMenu.add(Actions.PreviewInViewer);
        }
        //slidePreviewMenu.add(Actions.PreviewOnMap);
        presentationMenu.add(Actions.PreviewOnMap);
        presentationMenu.add(Actions.EditMasterSlide);
        // slidePreviewMenu.add(Actions.PreviewInViewer);
      //  presentationMenu.add(slidePreviewMenu);
   /*
        notesMenu.addMenuListener(new MenuListener()
        {

			public void menuCanceled(MenuEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void menuDeselected(MenuEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void menuSelected(MenuEvent e) {
				notesMenu.removeAll();
				final LWPathwayList pathways = VUE.getActiveMap().getPathwayList();
				
				//Iterator i = pathways.iterator();
				final Collection coll = pathways.getElementList();
				final Iterator i = coll.iterator();
				while (i.hasNext())
				{
					final LWPathway path = (LWPathway) i.next();
					final JMenu menuLevel1 = new JMenu(path.getDisplayLabel());
					
					//	menuLevel1.setEnabled(false);
						
					
					notesMenu.add(menuLevel1);
									    
					final JMenuItem item1 = new JMenuItem(Actions.FullPageSlideNotes.getActionName());
			        item1.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.FullPageSlideNotes.act();
			        	}
			        });
			        
			        final JMenuItem item2 = new JMenuItem(Actions.Slides8PerPage.getActionName());
			        item2.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.Slides8PerPage.act();
			        	}
			        });
			        
			        final JMenuItem item3 = new JMenuItem(Actions.SpeakerNotes1.getActionName());
			        item3.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.SpeakerNotes1.act();
			        	}
			        });

			        final JMenuItem item4 = new JMenuItem(Actions.SpeakerNotes4.getActionName());
			        item4.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.SpeakerNotes4.act();
			        	}
			        });


			        final JMenuItem item5 = new JMenuItem(Actions.AudienceNotes.getActionName());
			        item5.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.AudienceNotes.act();
			        	}
			        });

			        final JMenuItem item6 = new JMenuItem(Actions.SpeakerNotesOutline.getActionName());
			        item6.addActionListener(new ActionListener()
			        {
			        	public void actionPerformed(ActionEvent e)
			        	{
			        		VUE.setActive(LWPathway.class, this, path);
			        		Actions.SpeakerNotesOutline.act();
			        	}
			        });
			        if (!path.getEntries().isEmpty())
					{
			        	menuLevel1.add(item1);
			        	menuLevel1.add(item2);
			        	menuLevel1.add(item3);
			        	menuLevel1.add(item4);
			        	menuLevel1.add(item5);
			        	menuLevel1.add(item6);
					}
			        else
			        {
			        	final JMenuItem emptyItem = new JMenuItem("no presentation is available");
			        	emptyItem.setEnabled(false);
			        	menuLevel1.add(emptyItem);
			        }
			    }
				
			}
        	
        });
        */
        playbackMenu.addMenuListener(new MenuListener()
        {

			public void menuCanceled(MenuEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void menuDeselected(MenuEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void menuSelected(MenuEvent arg0) {
				playbackMenu.removeAll();
				final LWPathwayList pathways = VUE.getActiveMap().getPathwayList();
				
				//Iterator i = pathways.iterator();
				final Collection coll = pathways.getElementList();
				final Iterator i = coll.iterator();
				while (i.hasNext())
				{
					final LWPathway path = (LWPathway) i.next();
					final JMenuItem menuItem = new JMenuItem(path.getDisplayLabel());
					if (path.getEntries().isEmpty())
						menuItem.setEnabled(false);

					menuItem.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							VUE.setActive(LWPathway.class, this, path);
								//VUE.getActiveMap().getPathwayList().setActivePathway(path);
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
					});
					playbackMenu.add(menuItem);
				}
				
			}
        	
        });
        
        notesMenu.add(Actions.MapAsPDF);
        notesMenu.add(Actions.FullPageSlideNotes);
        notesMenu.add(Actions.Slides8PerPage);
        notesMenu.add(Actions.SpeakerNotes1);
        notesMenu.add(Actions.SpeakerNotes4);
        notesMenu.add(Actions.AudienceNotes);
        notesMenu.add(Actions.SpeakerNotesOutline);   
        
        presentationMenu.add(notesMenu);
        presentationMenu.addMenuListener(new MenuListener(){
			public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation() {
				LWPathway p =VUE.getActivePathway();
				if (p == null || p.length() == 0)
					notesMenu.setEnabled(false);			
				else	
					notesMenu.setEnabled(true);				
			}});             
        presentationMenu.addSeparator();
        presentationMenu.add(playbackMenu);
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Build Analysis Menu
        ////////////////////////////////////////////////////////////////////////////////////
        analysisMenu.add(createCMAction);
        analysisMenu.add(analyzeCMAction);
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Window Menu
        ////////////////////////////////////////////////////////////////////////////////////

        if (VUE.getFormatDock() != null)        
        	windowMenu.add(createWindowItem(VUE.getFormatDock(),KeyEvent.VK_1,"Formatting Palette"));                
        windowMenu.addSeparator();                
        if (VUE.getInfoDock() !=null)
        	windowMenu.add(createWindowItem(VUE.getInfoDock(),KeyEvent.VK_2, "Info"));
        windowMenu.add(Actions.KeywordAction);
        windowMenu.add(Actions.NotesAction);
        windowMenu.addSeparator();
        if (VUE.getMapInfoDock() !=null)
        	windowMenu.add(createWindowItem(VUE.getMapInfoDock(),KeyEvent.VK_3, "Map Info"));
        windowMenu.add(ontcontrls);
        if (VUE.getOutlineDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getOutlineDock(),KeyEvent.VK_5, "Outline"));
        if (VUE.getPannerDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getPannerDock(),KeyEvent.VK_6, "Panner"));
        if (VUE.getPresentationDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getPresentationDock(),KeyEvent.VK_7, "Pathways"));
        if (VUE.getContentDock() !=null)	
        	windowMenu.add(createWindowItem(VUE.getContentDock(),KeyEvent.VK_8, "Resources"));
        if (VUE.getLayersDock() != null)	
            windowMenu.add(createWindowItem(VUE.getLayersDock(), 0, "Layers"));
                
                
           
        final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(KeyEvent.VK_9, Actions.COMMAND);
    	Actions.SearchFilterAction.putValue(Action.ACCELERATOR_KEY, acceleratorKey);
        windowMenu.add(Actions.SearchFilterAction);
        windowMenu.addSeparator();
        if (VUE.getFloatingZoomDock()!=null)
        {
        	fullScreenToolbarItem = createWindowItem(VUE.getFloatingZoomDock(),KeyEvent.VK_0, "FullScreen Toolbar");
        	fullScreenToolbarItem.setEnabled(false);
        	windowMenu.add(fullScreenToolbarItem);        	
        }        
        
        
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Build Help Menu
        ////////////////////////////////////////////////////////////////////////////////////

        if (tufts.Util.isMacPlatform() == false) {
            // already in standard MacOSX place
            helpMenu.add(new AboutAction());
            helpMenu.addSeparator();
        }
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.userGuide.label"), VueResources.getString("helpMenu.userGuide.url")));        
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.feedback.label"), VueResources.getString("helpMenu.feedback.url")));
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.vueWebsite.label"), VueResources.getString("helpMenu.vueWebsite.url")));
      
      /*
       * This feature was removed from the VUE website because of a security issue in the web-feature, it doesn't make 
       * sense to have it here until we decided we're going to support it again on the website.
       *  helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.mymaps.label"), VueResources.getString("helpMenu.mymaps.url"))); 
       */                
        
        helpMenu.addSeparator();
/*        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.releaseNotes.label"), VueResources.getURL("helpMenu.releaseNotes.file"),
                new String("ReleaseNotes_" + VueResources.getString("vue.version") + ".htm").replace(' ', '_')
                ));
        helpMenu.addSeparator();*/
        helpMenu.add(new ShortcutsAction());
        
        helpMenu.addSeparator();
        helpMenu.add(new ShowLogAction());
      
        ////////////////////////////////////////////////////////////////////////////////////
        // Build final main menu bar
        ////////////////////////////////////////////////////////////////////////////////////
        
        //build out the main menus..
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(formatMenu);
        add(contentMenu);
        add(presentationMenu);
        add(analysisMenu);        
        add(windowMenu);
        add(helpMenu);
        
        if (DEBUG.Enabled) {
            add(new JMenu("    ")); // gap
            add(TitleItem);
            VUE.addActiveListener(LWMap.class, this);
        }
        
        if (RootMenuBar == null)
            RootMenuBar = this;
    }

    private final JMenuItem TitleItem = new JMenu("");
    //private final java.util.Map<LWMap,JMenuItem> items = new java.util.HashMap();
    
    public void activeChanged(tufts.vue.ActiveEvent e, LWMap map) {
        //TitleItem.setFont(VueConstants.SmallFont);

        if (VUE.isStartupUnderway())
            return;
        
        TitleItem.setLabel("[ " + (map==null?"?":map.getLabel()) + " ]");
        TitleItem.removeAll();
        
        MapTabbedPane tp = VUE.getLeftTabbedPane();
        for (int i = 0; i < tp.getTabCount(); i++) {
            final MapViewer v = tp.getViewerAt(i);
            final LWMap m = v.getMap();
            
            JMenuItem item = new JMenuItem(new VueAction(m.getSaveFileModelVersion() + " | " +  m.getLabel()) {
                    public void act() {
                        Log.debug("quik-map " + m);
                        // this currently only works in full-screen mode
                        ActiveInstance.getHandler(LWMap.class).setActive(this, m);
                        // VueTabbedPane should be auto-updating based
                        // on the active map event, but too close to
                        // release to mess with fundamental events right now.
                        VUE.getLeftTabbedPane().setSelectedMap(m);
                    }
                    public boolean overrideIgnoreAllActions() { return true; }
                }
                );

            if (m == map)
                item.setEnabled(false);
            
            item.setToolTipText(""+m.getFile());
            TitleItem.add(item);
        }
    }

    
    
    public static void toggleFullScreenTools()
    {
    	fullScreenToolbarItem.setEnabled(FullScreen.inFullScreen());
    }
    
    public JCheckBoxMenuItem createWindowItem(DockWindow dock,int accelKey, String text)
    {
    	final WindowDisplayAction windowAction = new WindowDisplayAction(dock);
    	final KeyStroke acceleratorKey = KeyStroke.getKeyStroke(accelKey, Actions.COMMAND);
    	if (accelKey > 0)
    		windowAction.putValue(Action.ACCELERATOR_KEY, acceleratorKey);    	
    	if (text !=null)
    		windowAction.setTitle(text);
    	
    	JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem(windowAction);
    	
    	
    		//checkBox.setText(text);
    	windowAction.setLinkedButton(checkBox);
    	
    	return checkBox;
    }
    protected void rebuildRecentlyOpenedItems(JMenu fileMenu, JMenu recentlyOpenedMenu, RecentlyOpenedFilesManager rofm) {
    	if (rofm.getFileListSize() > 0)
        {
        	List files = rofm.getRecentlyOpenedFiles();
        	Iterator i = files.iterator();

        	recentlyOpenedMenu.removeAll();
        	
        	while (i.hasNext())
        	{
        		
        		File f = new File((String)i.next());
        		if (f.exists())
        			recentlyOpenedMenu.add(new RecentOpenAction(f));
        		else 
        			f=null;
        	}
        	fileMenu.remove(recentlyOpenedMenu);
        	fileMenu.add(recentlyOpenedMenu,2);
        }
		
	}


	private KeyEvent alreadyProcessed;

    /*
    public boolean doProcessKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        //return super.processKeyBinding(ks, e, condition, pressed);
        //if (e != alreadyProcessed) {
            System.out.println("VueMenuBar: handling relayed " + ks);
            return processKeyBinding(ks, e, condition, pressed);
            //}
            //return true;
    }
    */

    public void processKeyEvent(KeyEvent e) {
        if (!e.isConsumed())
            super.processKeyEvent(e);
        else
            Log.debug("processKeyEvent: already consumed " + e);
    }
    
    void doProcessKeyEvent(KeyEvent e) {
        if (e != alreadyProcessed) {
            if (DEBUG.KEYS) Log.debug("doProcessKeyEvent " + e);
            processKeyEvent(e);
        }
        else if (DEBUG.KEYS) Log.debug("already processed " + e);
    }
    
    // todo: this doesn't work: safer if can get working instead of above
    void doProcessKeyPressEventToBinding(KeyEvent e) {

        if (e != alreadyProcessed) {
            //System.out.println("VueMenuBar: doProcessKeyPressEventToBinding " + e);
            Log.debug("KEY->BIND " + e);
            KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), false);
            super.processKeyBinding(ks, e, WHEN_FOCUSED, true);
        }
        else Log.debug("already processed " + e);
    }
    
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (e.isConsumed())
            Log.debug("GOT CONSUMED " + ks);

        if (!pressed) // we only ever handle on key-press
            return false;
            
        boolean didAction = super.processKeyBinding(ks, e, condition, pressed);
        if (DEBUG.KEYS) {
            String used = didAction ?
                "CONSUMED " :
                "NOACTION ";
            Log.debug("processKeyBinding " + used + ks + " " + e.paramString());
        }
        if (didAction)
            e.consume();
        alreadyProcessed = e;
        return didAction;
    }
    

    public void setVisible(boolean b) {
        Log.debug("VMB: setVisible: " + b);
        super.setVisible(b);
    }
    public void focusGained(java.awt.event.FocusEvent e) {
        Log.debug("VMB: focusGained from " + e.getOppositeComponent());
    }
    public void focusLost(java.awt.event.FocusEvent e) {
        Log.debug("VMB: focusLost to " + e.getOppositeComponent());
    }


    public static JMenu buildMenu(String name, Action[] actions) {
        return buildMenu(makeMenu(name), actions);
    }
    public static JMenu buildMenu(JMenu menu, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            if (a == null)
                menu.addSeparator();
            else
                menu.add(a);
        }
        return menu;
    }




    private static class RecentOpenAction extends VueAction {
    	private File file;
    	public RecentOpenAction(File f) {
            super(f.getName());
            if (DEBUG.Enabled)
            	putValue(SHORT_DESCRIPTION, f.toString());
            else
            	putValue(SHORT_DESCRIPTION, "");
            file = f;
    	}

        @Override
        public boolean isUserEnabled() { return true; }
        // Don't allow if locked into a presentation:
        //public boolean isUserEnabled() { return VUE.getActiveTool().permitsToolChange(); }
    	
    	public void act()
    	{
            OpenAction.displayMap(file);
    	}
    	
    }

    private static class ShowLogAction extends VueAction {
        private static DockWindow errorDock;
        private static JTextArea textArea;

        //private static final String ReportAddress = "vue-help@elist.tufts.edu";
        private static final String ReportAddress = "vue-report@fraize.org";
        
    	public ShowLogAction() {
            super("VUE Log");
    	}

        @Override
        public boolean isUserEnabled() { return true; }
    	
    	public void act() {
            if (errorDock == null)
                buildGUI();
            textArea.setText(tufts.Util.getExceptionLog().toString());
            errorDock.setVisible(true);
    	}

        private void buildGUI() {
            final JPanel panel = new JPanel(new BorderLayout());
            textArea = new JTextArea();
            textArea.setFont(VueConstants.SmallFixedFont);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

            if (!Util.isWindowsPlatform())
            // can't get enough log into the email for now on Windows to be useful.
                panel.add(new JButton("Submit Report") {
                    @Override
                    protected void fireActionPerformed(ActionEvent ae) {                        
                        //System.out.println("ACTION " + ae);
                        
                        final String body;

                        if (Util.isWindowsPlatform()) {
                            // There's a 2048 byte WinXP argument limit for url.dll,FileProtocolHandler,
                            // so don't add anything extra...
                            body = Util.getExceptionLog().toString();
                        } else {
                            body =
                                "Thank you for submitting a problem report.\n"
                                + "Please feel free to add any comments/feedback here:\n"
                                + "\n\n\n"
                                + Util.getExceptionLog()
                                + "\nEnd report: " + new java.util.Date() + ".\n";
                        }
                            
                        final String subject =
                            "VUE Log Report from " + VUE.getSystemProperty("user.name");
                        
                        try {
                            VueUtil.openURL(Util.makeQueryURL("mailto:" + ReportAddress,
                                                              "subject", subject,
                                                              "body", body));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                },
                BorderLayout.SOUTH);
            errorDock = GUI.createDockWindow("VUE Log", panel);
            errorDock.setSize(800,600);
        }
    }

    
    
    public static void main(String args[])
    {
        VUE.init(args);

        // Ensure the tools are loaded to we can see their shortcuts:
        VueToolbarController.getController();
        
        JFrame frame = new JFrame("vueParentWindow");

        // Ensure that all the Actions are instantiated so we can see them:
        tufts.vue.Actions.Delete.toString();
	         
        // Let us see the actual menu bar:
        frame.setJMenuBar(new VueMenuBar());
        frame.setVisible(true); // do this or we can't see the menu bar

        new ShortcutsAction().act();

    }
    

    

}

