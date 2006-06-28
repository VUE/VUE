/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
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
    
    public void restoreModel(DefaultTreeModel model, ResourceNode rootNode, SaveNode rootSNode){
        int FAVORITES = Resource.FAVORITES;
        Vector v = rootSNode.getChildren();
        if (v != null){
            int i = v.size();
            while (i > 0){
                i = i -1;
                SaveNode nextSNode = (SaveNode)v.elementAt(i);
                if (((nextSNode.getResource()).getType()) == FAVORITES){
                    FavoritesNode nextFNode = new FavoritesNode(nextSNode.getResource());
                    model.insertNodeInto(nextFNode,rootNode,0);
                    restoreModel(model, nextFNode, nextSNode);
                } else{
                    ResourceNode nextNode = new ResourceNode(nextSNode.getResource());
                    model.insertNodeInto(nextNode,rootNode,0);
                    restoreModel(model, nextNode, nextSNode);
                }
                
                try {
                    if(DataSourceViewer.getDefualtFavoritesRepository() != null){
                        System.out.println("Restoring JTree, Default Favorites: "+DataSourceViewer.getDefualtFavoritesRepository().getDisplayName());
                        org.osid.repository.Repository repository = DataSourceViewer.getDefualtFavoritesRepository();
                        org.osid.repository.Asset asset = repository.createAsset(nextSNode.getResource().getTitle(),nextSNode.getResource().getToolTipInformation(),favoritesType);
                    }
                }catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
