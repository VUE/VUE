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

import tufts.vue.action.ActionUtil;

import javax.swing.JFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.tree.*;
import java.util.*;
import java.io.*;
// castor classes
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.xml.sax.InputSource;
import osid.dr.*;
/**
 *
 * @author  Ranjani Saigal
 * Modified by Anoop Kumar 04/29/06
 */
public class FavoritesWindow extends JPanel implements ActionListener, ItemListener,KeyListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FavoritesWindow.class);
    
    public static final String NEW_FAVORITES = VueResources.getString("dialog.favoriteswindow.newfFavoritesfolder");
    public static final String ADD_FAVORITES = VueResources.getString("dialog.favoriteswindow.addfavoritesfolder");
    public static final String REMOVE_FAVORITES = VueResources.getString("dialog.favoriteswindow.removefavoritesfolder");
    public static final String OPEN_RESOURCE = VueResources.getString("dialog.favoriteswindow.openresource");
    public static final String REMOVE_RESOURCE = VueResources.getString("dialog.favoriteswindow.removeresource");
    public static final String CONFIRM_DEL_RESOURCE =VueResources.getString("dialog.favoriteswindow.deleteresource");
    public static final String TITLE_DEL_RESOURCE = VueResources.getString("dialog.favoriteswindow.delresourceconf");
//    public static final String SAVE_FILE = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.favorites");
    private String saveFile;
    public static final int DEFAULT_SELECTION_ROW = 0;
    private DisplayAction displayAction = null;
    public  VueDandDTree favoritesTree ;
    //private JScrollPane browsePane;
    private static int FAVORITES = Resource.FAVORITES;
    JTextField keywords;
    boolean fileOpen = false;
    
    /** Creates a new instance of HierarchyTreeWindow */
    public FavoritesWindow(String displayName, String saveFile ) {
        setLayout(new BorderLayout());
        this.saveFile = saveFile;
        load();
        if (!fileOpen){
            Log.info("Creating new favorites: " + displayName);
            // todo: refactor such that we don't need to create this dummy "name only" resource
            // (e.g., create the FavoritesNode directly, which might subclass the new "RootNode")
            URLResource favResource = (URLResource) Resource.getFactory().get(displayName);
            favResource.setClientType(FAVORITES);
            favoritesTree = new VueDandDTree(new FavoritesNode(favResource));
            favoritesTree.setRootVisible(false);
        }
        createPopupMenu();
        if (true) {
            // now that entire Resource DockWindow is in a scroll pane,
            // this is just messy -- SMF 2008-04-15
            add(favoritesTree, BorderLayout.CENTER);
        } else {
            // re-enabled in case we really want the left-right scroll ability:
            add(new JScrollPane(favoritesTree), BorderLayout.CENTER);
        }
// //         browsePane = new JScrollPane(favoritesTree);
// //         add(browsePane,BorderLayout.CENTER);
        
    }
    public VueDandDTree getFavoritesTree(){
        return (this.favoritesTree);
    }
    
    public void setFavoritesTree(VueDandDTree favoritesTree){
        this.favoritesTree = favoritesTree;
    }
    
    public Action getDisplayAction(String displayName) {
        if (displayAction == null)
            displayAction = new DisplayAction(displayName);
        return (Action)displayAction;
    }
    
    private class DisplayAction extends AbstractAction {
        private AbstractButton aButton;
        
        public DisplayAction(String label) {
            super(label);
        }
        public void actionPerformed(ActionEvent e) {
            aButton = (AbstractButton) e.getSource();
            setVisible(aButton.isSelected());
        }
        public void setButton(boolean state) {
            aButton.setSelected(state);
        }
    }
    
    public void createPopupMenu() {
        JMenuItem menuItem;
        JPopupMenu popup = new JPopupMenu();
        menuItem = new JMenuItem(ADD_FAVORITES);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem(REMOVE_FAVORITES);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
        JPopupMenu popupResource = new JPopupMenu();
        menuItem = new JMenuItem(OPEN_RESOURCE);
        menuItem.addActionListener(this);
        popupResource.add(menuItem);
        menuItem = new JMenuItem(REMOVE_RESOURCE);
        menuItem.addActionListener(this);
        popupResource.add(menuItem);
        
        JPopupMenu popupAdd = new JPopupMenu();
        menuItem = new JMenuItem(ADD_FAVORITES);
        menuItem.addActionListener(this);
        popupAdd.add(menuItem);
        
        MouseListener popupListener = new PopupListener(popup,popupResource,popupAdd);
        favoritesTree.addMouseListener(popupListener);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        ResourceNode resNode;
        FavoritesNode favresNode;
        ResourceNode temp;
        if (e.getSource() instanceof JMenuItem){
            JMenuItem source = (JMenuItem)(e.getSource());
            TreePath tp = favoritesTree.getSelectionPath();
            DefaultTreeModel model = (DefaultTreeModel)favoritesTree.getModel();
            if (tp == null){ resNode = (ResourceNode)model.getRoot();
            } else{
                resNode = (ResourceNode)tp.getLastPathComponent();
            }
            if (e.getActionCommand().toString().equals(REMOVE_RESOURCE) | e.getActionCommand().toString().equals(REMOVE_FAVORITES)) {
                if (!resNode.isRoot()) {
                    if(VueUtil.confirm(this,CONFIRM_DEL_RESOURCE,TITLE_DEL_RESOURCE) == JOptionPane.OK_OPTION) {
                        TreePath parent = tp.getParentPath();
                        model.removeNodeFromParent(resNode);
                        if(parent.getPathCount()!=1)
                            favoritesTree.setSelectionPath(parent);
                        else
                            favoritesTree.setSelectionRow(DEFAULT_SELECTION_ROW);
                    }
                }
            } else if (e.getActionCommand().toString().equals(ADD_FAVORITES)){
                addFavorites(model,resNode);
            } else {
                if (resNode instanceof FileNode) { ((FileNode)resNode).displayContent();} else {
                    resNode.getResource().displayContent();
                }
            }
        } else {
            performSearch();
        }
    }
    
    private void addFavorites(DefaultTreeModel model, ResourceNode resNode){
        TreePath tp = new TreePath(resNode.getPath());
        FavoritesNode favresNode = (FavoritesNode)tp.getLastPathComponent();
        URLResource favResource = (URLResource) Resource.getFactory().get(NEW_FAVORITES);
        favResource.setTitle(NEW_FAVORITES);
        favResource.setClientType(FAVORITES);
        FavoritesNode favNode = new FavoritesNode(favResource);
        model.insertNodeInto(favNode,favresNode, (favresNode.getChildCount()));
        if ((favresNode.isLeaf())){
            if (model.getRoot() != resNode){
                favoritesTree.expandPath(new TreePath(favresNode.getPath()));
                favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
                favoritesTree.setRootVisible(true);
            } else {
                favoritesTree.setRootVisible(true);
                favoritesTree.expandRow(0);
                favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
            }
        } else{
            favoritesTree.setRootVisible(true);
            favoritesTree.expandPath(new TreePath(favresNode.getPath()));
            favoritesTree.startEditingAtPath(new TreePath(favNode.getPath()));
        }
        favoritesTree.setRootVisible(false);
        
    }
    
    public void itemStateChanged(ItemEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
    }
    //Saves to favorites.xml in applications default directory.  May want to have a separate file for every favorites
    public void save() {
        if (favoritesTree != null)  {
            tufts.vue.VueDandDTree ft = this.getFavoritesTree();
            ft.setRootVisible(true);
            tufts.vue.SaveVueJTree sFavTree = new tufts.vue.SaveVueJTree(ft);
            File favFile  = new File(saveFile);
            marshallFavorites(favFile,sFavTree);
        }
    }
    
    public void load() {
        File f  = new File(saveFile);
        if(f.exists()) {
            //try {
            SaveVueJTree restorefavtree = unMarshallFavorites(f);
            fileOpen = true;
            favoritesTree =restorefavtree.restoreTree();
            favoritesTree.setRootVisible(true);
            favoritesTree.expandRow(0);
            favoritesTree.setRootVisible(false);
            this.setFavoritesTree(favoritesTree);
//             } catch (Exception t) {
//                 Log.error("load " + this, t);
//                 fileOpen = false;
//             }
        } else {
            System.out.println("Favorites file not found: "+ f);
        }
    }
    public static void marshallFavorites(File file,SaveVueJTree favoritesTree) {
        Marshaller marshaller = null;
        Mapping mapping = new Mapping();
        try {
            if (DEBUG.IO) Log.debug("writing " + file);
            FileWriter writer = new FileWriter(file);
            marshaller = new Marshaller(writer);
            if (DEBUG.IO) Log.debug("got marshaller " + marshaller);
            marshaller.setValidation(false); // SMF: added 2008-04-13 -- was getting exceptions
            marshaller.setEncoding("US-ASCII");
            marshaller.setMarshalListener(new tufts.vue.action.ActionUtil.VueMarshalListener());
            marshaller.setMapping(ActionUtil.getDefaultMapping());
            if (DEBUG.IO) Log.debug("marshalling " + favoritesTree + "...");
            marshaller.marshal(favoritesTree);
            if (DEBUG.IO) Log.debug("marshalled " + favoritesTree);
            writer.flush();
            writer.close();
        } catch (Throwable t) {
            Log.error("marshallFavorites to " + tufts.Util.tags(file), t);
        }
    }
    
    public  SaveVueJTree unMarshallFavorites(File file) {
        Unmarshaller unmarshaller = null;
        SaveVueJTree sTree = null;
        try {
            //unmarshaller = new Unmarshaller(ActionUtil.getDefaultMapping());
            unmarshaller = ActionUtil.getDefaultUnmarshaller(file.toString());
            FileReader reader = new FileReader(file);
            sTree= (SaveVueJTree) unmarshaller.unmarshal(new InputSource(reader));
            reader.close();
        } catch (Exception e) {
            // e.printStackTrace();
            //System.err.println("FavoritesWindow.unmarshallFavorites: " + e);
            sTree = null;
        }
        return sTree;
    }
    
    public void performSearch(){
        int index = 0;
        JScrollPane jsp = new JScrollPane();
        String searchString = keywords.getText();
        //VueDragTree serResultTree = new VueDragTree("Search", "No Hits");
        VueDragTree serResultTree = new VueDragTree(null, "Search:No Hits"); // is this used anywhere???
        
        if (!searchString.equals("")){
            boolean foundit = false;
            // VueDragTree serResultTree = new VueDragTree("Search", "Search Results");
            DefaultTreeModel serTreeModel = (DefaultTreeModel)serResultTree.getModel();
            ResourceNode serRoot = (ResourceNode)serTreeModel.getRoot();
            TreeModel favTreeModel = this.favoritesTree.getModel();
            ResourceNode favRoot = (ResourceNode)favTreeModel.getRoot();
            int childCount = favRoot.getChildCount();
            if (childCount > 0){
                for (int outi = 0; outi < childCount ; ++outi){
                    if (favRoot.getChildAt(outi) instanceof FileNode){
                        FileNode childNode = (FileNode)favRoot.getChildAt(outi);
                        childNode.explore();
                    }
                    ResourceNode childNode = (ResourceNode)favRoot.getChildAt(outi);
                    foundit = compareNode(searchString,childNode,serRoot,false);
                    //  System.out.println("And here "+ childNode.toString()+foundit);
                    
                }
                
            }
            serResultTree.setRootVisible(true);
            serResultTree.setShowsRootHandles(true);
            serResultTree.expandRow(0);
            serResultTree.setRootVisible(false);
        }
    }
    public boolean compareNode(String searchString, ResourceNode Node, ResourceNode serRoot, boolean foundsomething) {
        if (searchString.compareToIgnoreCase(Node.toString()) == 0){
            addSearchNode(serRoot,Node);
            foundsomething = true;
        } else {
            int startIndex = 0;
            int lenSearchString = searchString.length();
            String thisString = Node.toString();
            int lenthisString = thisString.length();
            int endIndex = startIndex + lenSearchString;
            boolean found = false;
            while ((endIndex <= lenthisString) && !found){
                String   testString  = thisString.substring(startIndex,endIndex);
                if (searchString.compareToIgnoreCase(testString)== 0){
                    addSearchNode(serRoot,Node);
                    found = true;
                    foundsomething = true;
                }
                startIndex = startIndex + 1;
                endIndex = endIndex + 1;
            }
        }
        
        
        if (!(Node.isLeaf())){
            int childCount = Node.getChildCount();
            if (childCount  > 0){
                for (int i = 0; i < childCount; ++i){
                    ResourceNode childNode = (ResourceNode)Node.getChildAt(i);
                    if(compareNode(searchString,childNode,serRoot,foundsomething)) {
                        foundsomething = true;
                    }
                }
            }
        }
        return foundsomething;
    }
    
    public void addSearchNode(ResourceNode topNode, ResourceNode Node) {
        ResourceNode newNode = (ResourceNode)Node.clone();
        if (!(Node.isLeaf())){
            for(int inni = 0; inni < Node.getChildCount(); ++ inni){
                ResourceNode child = (ResourceNode)Node.getChildAt(inni);
                addSearchNode(newNode,child);
            }
        }
        topNode.add(newNode);
        
    }
    
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar()== KeyEvent.VK_ENTER) {
            
            performSearch();
        }
    }
    
    class PopupListener extends MouseAdapter {
        JPopupMenu popup,popupResource,popupAdd;
        VueDandDTree vtree = FavoritesWindow.this.getFavoritesTree();
        PopupListener(JPopupMenu popupMenu, JPopupMenu popupMenu2, JPopupMenu popupMenu3 ) {
            popup = popupMenu;
            popupResource = popupMenu2;
            popupAdd = popupMenu3;
        }
        
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }
        public void mouseClicked(MouseEvent e) {
            if (vtree.getSelectionPath() != null){
                DefaultMutableTreeNode selTreeNode = (DefaultMutableTreeNode)vtree.getSelectionPath().getLastPathComponent();
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)vtree.getClosestPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                if (treeNode != selTreeNode) { vtree.clearSelection();}
                TreeModel vmodel = vtree.getModel();
                if(e.getClickCount() == 2) {
                   if (((DefaultMutableTreeNode)vmodel.getRoot()).getChildCount() == 1) vtree.clearSelection();
                    if (!(selTreeNode instanceof FavoritesNode)) vtree.clearSelection();
                    if((selTreeNode instanceof ResourceNode)) {
                        TreePath path = vtree.getPathForLocation(e.getX(), e.getY());
                        if (path == null)
                            return;
                        Object uo = selTreeNode.getUserObject();
                        if (DEBUG.Enabled) System.out.println("instance of favorites node- res class"+ tufts.Util.tags(uo));
                    
                        if (uo instanceof Resource)
                            ((Resource)uo).displayContent();
                    }
                }
            }
            showPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (vtree.getSelectionPath() != null){
                    if (vtree.getSelectionPath().getLastPathComponent() instanceof FavoritesNode){
                        popup.show(e.getComponent(),e.getX(), e.getY());
                    } else{
                        popupResource.show(e.getComponent(),e.getX(), e.getY());
                    }
                } else {
                    popupAdd.show(e.getComponent(),e.getX(), e.getY());
                }
            }
        }
    }
    
    public static void main(String args[]) {
        VUE.init(args);
        new Frame("An Active Frame").setVisible(true);
        FavoritesWindow fw = new FavoritesWindow("http://www.tufts.edu","favorites.xml");
        fw.setVisible(true);
        tufts.Util.displayComponent(fw);
    }
}