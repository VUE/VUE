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

/*
 * SaveVueJtree.java
 *
 * Created on October 13, 2003, 1:14 PM
 */

package tufts.vue;

import javax.swing.tree.*;
import javax.swing.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;
/**
 *
 * @author  rsaigal
 */
public class SaveVueJTree {
    public static final  org.osid.shared.Type favoritesType = new edu.tufts.vue.util.Type("edu.tufts","favorites","Asset");

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SaveVueJTree.class);    
    
    private SaveNode saveTreeRoot;
    public SaveVueJTree() {
    }
    
    public SaveVueJTree(JTree tree){
        TreeModel treeModel = tree.getModel();
        setSaveTreeRoot(new SaveNode((ResourceNode)treeModel.getRoot()));
    }
    
    
    public void setSaveTreeRoot(SaveNode Snode){
        this.saveTreeRoot = Snode;
    }
    
    public SaveNode getSaveTreeRoot(){
        return(this.saveTreeRoot);
    }
    
    public VueDandDTree restoreTree(){
        VueDandDTree vueTree;
        FavoritesNode rootNode;
        SaveNode rootSNode = this.getSaveTreeRoot();
        rootNode = new FavoritesNode(rootSNode.getResource());
        vueTree = new VueDandDTree(rootNode);
        DefaultTreeModel model = (DefaultTreeModel)vueTree.getModel();
        restoreModel(model,rootNode,rootSNode);
        vueTree.expandRow(0);
        return vueTree;
    }
    
    private void restoreModel(DefaultTreeModel model, ResourceNode rootNode, SaveNode rootSNode){
        int FAVORITES = Resource.FAVORITES;
        Vector v = rootSNode.getChildren();
        if (v != null){
            int i = v.size();
            while (i > 0){
                i = i -1;
                final SaveNode nextSNode = (SaveNode)v.elementAt(i);
                final Resource resource = nextSNode.getResource();
                if (resource instanceof CabinetResource) {
                    final ResourceNode nextNode;
                    // todo: URLResource or CabinetResource should handle below (isFile/toFile)
                    final File file = new File(cleanFileName(resource.getSpec()));
                    if (file.isDirectory()) {
                        //nextNode = CabinetNode.getCabinetNode(file.getName(), file, rootNode, model);
                        // the title arg (first) should be ignored now anyway
                        nextNode = CabinetNode.getCabinetNode(resource.getTitle(), file, rootNode, model);
                    } else {
                        // this just in case it was a URLResource, not a CabinetResource (todo: should handle elsewhere commonly)
                        String title = resource.getTitle();
                        if (title == null || title.length() == 0) {
                            if (resource instanceof URLResource) {
                                // this is a total hack just to fix this crazy stuff up for now
                                // (we're editing the resource when it's restored!)
                                ((URLResource)resource).setTitle(file.getName());
                            }
                        }
                        nextNode = new ResourceNode(resource);
                    }
                    //nextNode = new CabinetNode(resource, CabinetNode.LOCAL);
                        
                    model.insertNodeInto(nextNode,rootNode,0);
                    restoreModel(model, nextNode, nextSNode);
                }else if (resource.getClientType() == FAVORITES){
                    FavoritesNode nextFNode = new FavoritesNode(resource);
                    model.insertNodeInto(nextFNode,rootNode,0);
                    restoreModel(model, nextFNode, nextSNode);
                }else{
                    ResourceNode nextNode = new ResourceNode(resource);
                    model.insertNodeInto(nextNode,rootNode,0);
                    restoreModel(model, nextNode, nextSNode);
                }
                
                try {
                    // todo: do we still need this?  What does this code do?
                    // We create an asset, but do nothing with it -- is there a side effect here?
                    if(DataSourceViewer.getDefualtFavoritesRepository() != null){
                        System.out.println("Restoring JTree, Default Favorites: "+DataSourceViewer.getDefualtFavoritesRepository().getDisplayName());
                        org.osid.repository.Repository repository = DataSourceViewer.getDefualtFavoritesRepository();
                        org.osid.repository.Asset asset = repository.createAsset(nextSNode.getResource().getTitle(),nextSNode.getResource().getToolTipText(),favoritesType);
                    }
                }catch(Throwable t) {
                    Log.error("restoreModel", t);
                }
            }
        }
    }
    
    private String cleanFileName(String fileName) {
        if (fileName.startsWith("file://"))
            return fileName.substring(7);
        else
            return fileName;
    }
}
