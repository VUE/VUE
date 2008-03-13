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
 * @version $Revision: 1.90 $ / $Date: 2008-03-13 20:54:28 $ / $Author: mike $
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

        public void addNotify() {
            if (unadjusted) {
                GUI.adjustMenuIcons(this);
                unadjusted = false;
            }
            super.addNotify();
        }
    }

    private final JCheckBoxMenuItem viewFullScreen = new JCheckBoxMenuItem(Actions.ToggleFullScreen);
    //public VueMenuBar(Object[] toolWindows)
    public VueMenuBar()
    {
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Top Level Menus
        ////////////////////////////////////////////////////////////////////////////////////

        final JMenu fileMenu = new VueMenu("File");
        final JMenu recentlyOpenedMenu = new VueMenu("Open recent");
        final JMenu editMenu = new VueMenu("Edit");
        final JMenu viewMenu = new VueMenu("View");
        final JMenu formatMenu = new VueMenu("Format");
        final JMenu transformMenu = new VueMenu("Font");
        final JMenu arrangeMenu = new VueMenu("Arrange");
        final JMenu contentMenu = new VueMenu("Content");
        final JMenu presentationMenu = new VueMenu(VueResources.getString("menu.pathway.label"));
        final JMenu analysisMenu = new VueMenu("Analysis");
        final JMenu windowMenu = new VueMenu("Windows");
        final JMenu alignMenu = new VueMenu("Align");
        final JMenu extendMenu = new VueMenu("Extend");
        final JMenu linkMenu = new VueMenu("Link");
        final JMenu helpMenu = add(new VueMenu("Help"));
        
        
        
      //  final JMenu slidePreviewMenu = new JMenu("Slide preview");
        final JMenu notesMenu = new JMenu("Handouts and Notes");
        final JMenu playbackMenu = new JMenu(VueResources.getString("menu.playback.play.label"));
        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Actions
        ////////////////////////////////////////////////////////////////////////////////////

        final JCheckBoxMenuItem splitScreenItem = new JCheckBoxMenuItem(Actions.ToggleSplitScreen);
        final JCheckBoxMenuItem togglePruningItem = new JCheckBoxMenuItem(Actions.TogglePruning);

        ////////////////////////////////////////////////////////////////////////////////////
        // Initialize Actions
        ////////////////////////////////////////////////////////////////////////////////////
                
        final SaveAction saveAction = new SaveAction("Save", false);
        final SaveAction saveAsAction = new SaveAction("Save As...");
        //final SaveAction exportAction = new SaveAction("Export ...",true,true);
        final OpenAction openAction = new OpenAction("Open...");
        final ExitAction exitAction = new ExitAction("Quit");
        final JMenu publishMenu = new VueMenu("Publish");
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
        
        edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().addDataSourceListener(new edu.tufts.vue.dsm.DataSourceListener() {
            public void changed(edu.tufts.vue.dsm.DataSource[] dataSource) {
           
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
            JMenu exportMenu = add(new VueMenu("Export"));
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
        pdfExportMenu.add(Actions.FullPageSlideNotes);
        pdfExportMenu.add(Actions.Slides8PerPage);
        pdfExportMenu.add(Actions.SpeakerNotes1);
        pdfExportMenu.add(Actions.SpeakerNotes4);
        pdfExportMenu.add(Actions.AudienceNotes);
        pdfExportMenu.add(Actions.SpeakerNotesOutline);
        pdfExportMenu.setEnabled(false);
      
        fileMenu.addMenuListener(new MenuListener(){
			public void menuCanceled(MenuEvent e) {/* no op	*/}
			public void menuDeselected(MenuEvent e) {/*no op */}
			public void menuSelected(MenuEvent e) {handleActivation();}
			private void handleActivation() {
				LWPathway p =VUE.getActivePathway();
				if (p == null || p.length() == 0)
					pdfExportMenu.setEnabled(false);			
				else	
					pdfExportMenu.setEnabled(true);				
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
        viewMenu.add(viewFullScreen);
        viewMenu.add(splitScreenItem);
        viewMenu.addSeparator();
        viewMenu.add(togglePruningItem);
        

        
        GUI.getFullScreenWindow().addWindowFocusListener(new WindowFocusListener()
        {
			public void windowGainedFocus(WindowEvent arg0) {
				viewFullScreen.setSelected(true);
				
			}

			public void windowLostFocus(WindowEvent arg0) {
				viewFullScreen.setSelected(false);
				
			}
		});

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
        helpMenu.add(new ShowURLAction(VueResources.getString("helpMenu.releaseNotes.label"),
                VueResources.getURL("helpMenu.releaseNotes.file"),
                new String("ReleaseNotes_" + VueResources.getString("vue.version") + ".htm").replace(' ', '_')
                ));
        helpMenu.addSeparator();
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
            
        if (RootMenuBar == null)
            RootMenuBar = this;
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
        return buildMenu(new VueMenu(name), actions);
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
    
    private static class ShortcutsAction extends VueAction {
        private static DockWindow window;
        ShortcutsAction() {
            super("Keyboard Shortcuts");
        }

        @Override
        public boolean isUserEnabled() { return true; }
        
        public void act() {
            if (window == null)
                window = createWindow();
            //window.setWidth(400);
            window.pack(); // fit to widest line
            window.setVisible(true);
        }
        private DockWindow createWindow() {
            return GUI.createDockWindow(VUE.getName() + " Short-Cut Keys", createShortcutsList());
        }

        private static String keyCodeName(int keyCode) {
            return keyCodeName(keyCode, false);
        }
        
        private static String keyCodeName(int keyCode, boolean lowerCase) {
            
            if (lowerCase && keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
                return String.valueOf((char)keyCode).toLowerCase();
            }
            
            if (keyCode == KeyEvent.VK_OPEN_BRACKET)
                return "[";
            else if (keyCode == KeyEvent.VK_CLOSE_BRACKET)
                return "]";
            else if (keyCode == 0)
                return "";
            else
                return KeyEvent.getKeyText(keyCode);
        }

        private JComponent createShortcutsList() {
            String text = new String();
            
            // get tool short-cuts
            for (VueTool t : VueTool.getTools()) {
                final int downKey = t.getActiveWhileDownKeyCode();
                if (DEBUG.TOOL) {
                    text += String.format(" %-25s (%c) %-12s %-23s %s \n",
                                          t.getID()+":",
                                          t.getShortcutKey() == 0 ? ' ' : t.getShortcutKey(),
                                          "("+keyCodeName(downKey) + ")",
                                          t.getToolName(),
                                          t.getClass().getName()
                                          );
                } else if (t.getShortcutKey() != 0) {
                    text += String.format(" (%c) %-12s %s \n",
                                          t.getShortcutKey(),
                                          downKey == 0 ? "" : "(" + keyCodeName(downKey, true) + ")",
                                          t.getToolName());
                }
            }
            
            text += "\n";

            // get action short-cuts
            for (VueAction a : getAllActions()) {
                
                KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
                if (k == null)
                    continue;
                
                String keyModifiers = KeyEvent.getKeyModifiersText(k.getModifiers());
                if (keyModifiers.length() > 0)
                    keyModifiers += " ";

                String strokeName = keyModifiers + keyCodeName(k.getKeyCode());

                text += String.format(" %-20s %s \n", strokeName, a.getPermanentActionName());
            }
            
            javax.swing.JTextArea t = new javax.swing.JTextArea();
//             javax.swing.JTextArea t = new javax.swing.JTextArea() {
//                     public void paint(java.awt.Graphics g) {
//                         ((java.awt.Graphics2D)g).setRenderingHint
//                             (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
//                              java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//                         super.paint(g);
//                     }};
            
            t.setFont(VueConstants.SmallFixedFont);
            t.setEditable(false);
          //  t.setFocusable(false);
            t.setText(text);
            t.setOpaque(false);
            return new JScrollPane(t,
                                   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                   );
        }
        
    }
    
    
    

    
    
}
