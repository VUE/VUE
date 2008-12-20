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

import tufts.Util;
import tufts.vue.gui.*;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import osid.dr.*;

import java.util.*;

import java.io.*;
import java.io.IOException;
import java.net.URL;
import java.util.regex.*;

import javax.swing.*;
import java.awt.event.*;

import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;

import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

/**
 * A List that is droppable for the datasources. Only My favorites will
 * take a drop.
 *
 * This class assocates the DataSourceListCellRenderer with this list,
 * includes "addOrdered" for adding to the model yet maintaining an
 * internal sort order based on type, and provides a DropTarget with
 * drag/drop code for indicating droppable items in the list on
 * drag-over and accepting drops.  Most of the code in this class is
 * some hairy drop code, especially for parsing out dropped
 * Resources/Files, that should be elsewhere: e.g., right in the
 * data-source that accept drops: FavoritesDataSource (and potentially
 * someday: RSSDataSource).
 *
 * Note that this currently creates it's own DefaultListModel to keep
 * the data sources ordered as it would like, and is a list made of up
 * of both tufts.vue.DataSource's and edu.tufts.vue.dsm.DataSource's,
 * which are obtained via separate API calls. his would be better
 * handled if it was directly backed by re-orderable list(s) of
 * data sources that comes from the VueDataSourceManager (which
 * already handles persistance of the lists).  This would also allow
 * user re-ordering of the data sources, which would automatically be
 * persistent.
 * 
 * @version $Revision: 1.61 $ / $Date: 2008-12-20 20:05:21 $ / $Author: sfraize $
 * @author Ranjani Saigal
 */

// TODO: remove constructor reference to DataSourceViewer: handle via globally posted events
// (this handle is only used in one place here).

public class DataSourceList extends JList implements DropTargetListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataSourceList.class);
    
    static Object IndicatedDragOverValue;
    
    private static final boolean debug = true;
    
    private static final int ACCEPTABLE_DROP_TYPES = 0
            | DnDConstants.ACTION_COPY
            | DnDConstants.ACTION_LINK
            | DnDConstants.ACTION_MOVE
            ;
    
    private final DataSourceViewer dsViewer;
    
    public DataSourceList(DataSourceViewer dsViewer) {
        super(new DefaultListModel());
        this.dsViewer = dsViewer;
        this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.setFixedCellHeight(-1);
        
        // TODO: create a generic resource drop handler from MapDropTarget to use
        // in places such as this (this code originiated as a copy of MapDropTarget,
        // but is now completely old/out of sync with with it).
        
        new DropTarget(this,  ACCEPTABLE_DROP_TYPES, this);
        this.setCellRenderer(new DataSourceListCellRenderer());

        if (!VUE.BLOCKING_OSID_LOAD) {
            edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance().addDataSourceListener
                (new edu.tufts.vue.dsm.DataSourceListener() {
                        public void changed(edu.tufts.vue.dsm.DataSource[] dataSource,
                                            Object state,
                                            edu.tufts.vue.dsm.DataSource changed) {
                            if (DEBUG.Enabled) Log.debug("data sources changed: repaint");
                            repaint();
                        }
                    });
        }
    }
    
    private Object locationToValue(Point p) {
        int index = locationToIndex(p);
        return getModel().getElementAt(index);
    }
    
        /*
         We are going to add some cleverness to the way data sources are ordered.  There are four types of data sources:
         
         1. edu.tufts.vue.dsm.DataSource
         2. LocalFileDataSource
         3. RemoteFileDataSource
         4. FavoritesDataSource
         
         New additions to the list are inserted at the bottom of their "group"
         */
    
    public void addOrdered(Object o) {
        DefaultListModel model = (DefaultListModel)getModel();
        int size = model.size();
        if (o instanceof edu.tufts.vue.dsm.DataSource) {
            if ( ((edu.tufts.vue.dsm.DataSource)o).getRepositoryDisplayName().trim().length() == 0 ) {
                Log.debug("ignoring repository with empty display name: " + Util.tags(o));
                return;
            }
            for (int i=0; i < size; i++) {
                Object obj = model.elementAt(i);
                if (!(obj instanceof edu.tufts.vue.dsm.DataSource)) {
                    model.insertElementAt(o,i);
                    return;
                }
            }
            model.insertElementAt(o,size);
        } else if (o instanceof LocalFileDataSource) {
            for (int i=0; i < size; i++) {
                Object obj = model.elementAt(i);
                if ( (!(obj instanceof edu.tufts.vue.dsm.DataSource)) && (!(obj instanceof LocalFileDataSource)) ) {
                    model.insertElementAt(o,i);
                    return;
                }
            }
            model.insertElementAt(o,size);
        } else if (o instanceof RemoteFileDataSource) {
            for (int i=0; i < size; i++) {
                Object obj = model.elementAt(i);
                if (obj instanceof FavoritesDataSource) {
                    model.insertElementAt(o,i);
                    return;
                }
            }
            model.insertElementAt(o,size);
        } else {
            model.addElement(o);
        }
    }
    
    public DefaultListModel getModelContents() {
        return (DefaultListModel) getModel();
    }
    
    private void setIndicated(Object value) {
        if (IndicatedDragOverValue != value) {
            IndicatedDragOverValue = value;
            repaint();
        }
    }

    private static final boolean ALLOW_FEED_DROPS = false;
    
    public void dragOver(DropTargetDragEvent e) {
        Object over = locationToValue(e.getLocation());
        if (DEBUG.DND) out("dragOver: " + over);
        if (over instanceof FavoritesDataSource) {
            e.acceptDrag(e.getDropAction());
            setIndicated(over);
        } else {
            setIndicated(null);
            if (ALLOW_FEED_DROPS)
                e.acceptDrag(DnDConstants.ACTION_LINK);
            else
                e.rejectDrag();
        }
    }

    public void dragEnter(DropTargetDragEvent e) {
        if (DEBUG.DND) out("dragEnter");
        setIndicated(null);
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
    }
    
    public void dragExit(DropTargetEvent e) {
        if (DEBUG.DND) out("dragExit");
        setIndicated(null);
    }
    
    public void dropActionChanged( DropTargetDragEvent e ) {
        e.acceptDrag(ACCEPTABLE_DROP_TYPES);
    }
    
    public void drop(DropTargetDropEvent e) {
        setIndicated(null);
        
        final Object over = locationToValue(e.getLocation());
        
        if (DEBUG.DND) out("DROP over " + over);
        if (over instanceof FavoritesDataSource) {
            if (DEBUG.DND) out("drop ACCEPTED");
            e.acceptDrop(e.getDropAction());
        } else {
            
            if (ALLOW_FEED_DROPS) {
                
                // This will allow auto-creating RSS Feed data-sources on the drop of a
                // "feed:" string

                // TODO: for this to fully work, we need the refractoring of MapDropTarget
                // so that processTransferable will not try to add the resource automatically
                // to the map!

                // TODO: we should also really put add the feed to the VueDataSourceManager,
                // (where it could auto-persist it), then either handle an event from
                // the VDSM to trigger the addOrdered, or to it manually here.
                
                e.acceptDrop(e.getDropAction());
                final Transferable transfer = e.getTransferable();
                if (DEBUG.DND) try { new MapDropTarget(null).processTransferable(transfer, null); } catch (Throwable t) {}
                if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String txt = MapDropTarget.extractData(transfer, DataFlavor.stringFlavor, String.class);
                    if (txt.startsWith("feed:")) {
                        BrowseDataSource feed = new edu.tufts.vue.rss.RSSDataSource(txt, txt);
                        addOrdered(feed);
                        DataSourceViewer.saveDataSourceViewer();
                        DataSourceViewer.cacheDataSourceViewers(); // start it loading
                        return;
                    }
                }
            }
            
            if (DEBUG.DND) out("drop rejected");
            e.rejectDrop();
            return;
        }

        // TODO: after merging drop handling with global VUE transfer
        // handler, check to see if dropped resource is a "feed"
        // resource, and if so, auto-add an RSSDataSource if dropped
        // anywhere other than on a FavoritesDataSource.
        
//         final int current = this.getSelectedIndex();
//         final int index = locationToIndex(e.getLocation());
//         try {
//             setSelectedIndex(index);
//         } catch (Throwable t) {
//             Log.error("drop; setSelectedIndex " + index + ":", t);
//         }
//         DataSource ds = (DataSource)getSelectedValue();

        final tufts.vue.DataSource ds = (DataSource) over;
        
        if (DEBUG.DND) out("DROP ON DATA SOURCE: " + ds.getDisplayName());
        try {
            FavoritesWindow fw = (FavoritesWindow)ds.getResourceViewer();
            VueDandDTree favoritesTree = fw.getFavoritesTree();
            favoritesTree.setRootVisible(true);
            DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
            FavoritesNode rootNode = (FavoritesNode)model.getRoot();
            boolean success = true;
            Transferable transfer = e.getTransferable();
            DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
            //String resourceName = null; // never set elsewhere!
            java.util.List fileList = null;
            java.util.List resourceList = null;
            if (DEBUG.DND) out("RESOURCE TRANSFER FOUND: " + transfer);
            try {
                if (transfer.isDataFlavorSupported(Resource.DataFlavor)) {
                    final Object data = transfer.getTransferData(Resource.DataFlavor);
                    Collection<Resource> droppedResources = null;
                    if (data instanceof Resource) {
                        droppedResources = (Collection) Collections.singletonList(data);
                    } else if (data instanceof Collection) {
                        droppedResources = (Collection) data;
                    } else {
                        Util.printStackTrace("Unhandled drop type: " + Util.tag(data) + "; " + data);
                        return;
                    }
                    
                    for (Resource resource : droppedResources) {
                        if (DEBUG.DND) Log.debug("Found: " + Util.tags(resource));
                        ResourceNode newNode;

                        // TODO: ALL THIS CODE IS IDENNTICAL TO THAT IN VueDandDTree
                        
                        if (resource.isLocalFile()) {
                            final CabinetResource cr = (CabinetResource) resource;
                            newNode = new CabinetNode(cr, CabinetNode.LOCAL);
                            if (DEBUG.DND) Log.debug("CABINET RESOURCE: " + resource + "Entry: "+cr.getEntry()+ "entry type:"+cr.getEntry().getClass()+" type:"+cr.getEntry());
                        } else {
                            newNode    =new  ResourceNode(resource);
                        }
                        model.insertNodeInto(newNode, rootNode, newNode.getChildCount());
                        favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                        favoritesTree.setRootVisible(false);
                    }
                } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                    fileList = (java.util.List)transfer.getTransferData(DataFlavor.javaFileListFlavor);
                    java.util.Iterator iter = fileList.iterator();
                    while(iter.hasNext()){
                        File file = (File)iter.next();
                        if (file.isDirectory()){
                            try{
                                final LocalFilingManager manager = LocalFileDataSource.getLocalFilingManager();
                                osid.shared.Agent agent = null;
                                LocalCabinet cab = LocalCabinet.instance(file.getAbsolutePath(),agent,null);
                                // todo: need to extend Resource class and/or refactor this code so
                                // we don't need the LOCAL / REMOTE distinction, or can discover it
                                // generically.
                                //Resource res = Resource.instance(cab);
                                CabinetResource res = CabinetResource.create(cab);
                                CabinetEntry entry = res.getEntry();
                                if (file.getPath().toLowerCase().endsWith(".url")) {
                                    String url = MapDropTarget.convertWindowsURLShortCutToURL(file);
                                    if (url != null) {
                                        res.setSpec(url);
                                        String resName;
                                        if (file.getName().length() > 4)
                                            resName = file.getName().substring(0, file.getName().length() - 4);
                                        else
                                            resName = file.getName();
                                        res.setTitle(resName);
                                    }
                                }
                                CabinetNode cabNode = null;
                                if (entry instanceof RemoteCabinetEntry)
                                    cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                                else
                                    cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                                model.insertNodeInto(cabNode, rootNode, 0);
                                favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                                favoritesTree.setRootVisible(false);
                                cabNode.explore();
                            }catch (Exception ex) {System.out.println("DataSourceList.drop: "+ex);}
                        } else {


//                             // TODO: this uses an empty CabinetResource as simply a file path
//                             // holder, so we can create a CabinetNode (and maybe persist the
//                             // CabinetResource.  Can refactor most of this all out.  Handle the
//                             // shortcut processing FIRST, then can create a CabinetResource from a
//                             // java.io.File, tho even that is probably overkill...
                            
//                             FileNode fileNode = new FileNode(file);
                            
//                             //tufts.Util.printStackTrace("Unsupported DROP onto " + ds + "; of " + transfer);

//                             model.insertNodeInto(cabNode, rootNode, 0);
//                             favoritesTree.expandPath(new TreePath(rootNode.getPath()));
//                             favoritesTree.setRootVisible(false);

                                

                            try{
                                LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                                osid.shared.Agent agent = null;

                                // SMF 2008-04-17: THIS IS BROKEN ANYWAY -- did it every work?
                                // We always create a LocalCabinet, even if it's a LocalByteStore!
                                // So dropping directories in works fine, but drop anything else and you're hosed.
                                // Yeah: it's not working in the current build.  Can't tell if it ever has.
                                LocalCabinet cab = LocalCabinet.instance(file.getAbsolutePath(),agent,null);
                                CabinetResource res = CabinetResource.create(cab);
                                //res.setTitle(file.getAbsolutePath());
                                CabinetEntry oldentry = res.getEntry();
                                res.setEntry(null);
                                if (file.getPath().toLowerCase().endsWith(".url")) {
                                    // Search a windows .url file (an internet shortcut)
                                    // for the actual web reference.
                                    String url = MapDropTarget.convertWindowsURLShortCutToURL(file);
                                    if (url != null) {
                                        //resourceSpec = url;
                                        res.setSpec(url);
                                        String resName;
                                        if (file.getName().length() > 4)
                                            resName = file.getName().substring(0, file.getName().length() - 4);
                                        else
                                            resName = file.getName();
                                        //res.setTitle(resourceName); // was always set to null!!!
                                        res.setTitle(resName);
                                    }
                                }
                                CabinetNode cabNode = null;
                                if (oldentry instanceof RemoteCabinetEntry)
                                    cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                                else
                                    cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                                model.insertNodeInto(cabNode, rootNode, 0);
                                favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                                favoritesTree.setRootVisible(false);
                            }catch (Exception ex) {System.out.println("DataSourceList.drop: "+ex);}
                        }
                    }
                }
                
                else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)){
                    String dataString = (String)transfer.getTransferData(DataFlavor.stringFlavor);
                    Resource resource = URLResource.create(dataString);
                    ResourceNode newNode =new  ResourceNode(resource);
                    model.insertNodeInto(newNode, rootNode, 0);
                    favoritesTree.expandPath(new TreePath(rootNode.getPath()));
                    favoritesTree.setRootVisible(false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.dropComplete(success);
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);

            if (dsViewer.getBrowsedDataSource() == null) {
                dsViewer.setActiveDataSource(ds);
                //VUE.setActiveDataSource(ds);
            }
            
// Very annoying if you want to drag items from search results into My Favorites:
//             else if (dsViewer.getBrowsedDataSource() == ds)
//                 dsViewer.expandBrowse();
            
            //this.setSelectedIndex(current);
        } catch (Exception ex) {
            if (DEBUG.DND) tufts.Util.printStackTrace(ex);
            //this.setSelectedIndex(current);
            VueUtil.alert(null, "You can only add resources to a Favorites Datasource","Resource Not Added");
        }
    }
    
    private void out(String s) {
        Log.debug(s);
    }
    
}
