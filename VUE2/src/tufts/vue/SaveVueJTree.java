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
    
    private SaveNode saveTreeRoot;
    
   
    public SaveVueJTree() {
    }

    public SaveVueJTree(JTree tree){
     
     TreeModel treeModel = tree.getModel();
     
    setSaveTreeRoot(new SaveNode(treeModel.getRoot()));
      
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
       
        
        String s = rootSNode.getNodeType();
        
      
        
        if (s.equals("FavoritesNode")){ 
            
              
               
         rootNode = new FavoritesNode(rootSNode.getResourceName());
       
           
          
      }
      else{
           rootNode = new FavoritesNode("Invalid Tree");
           
         
          
      }
      
      vueTree = new VueDandDTree(rootNode);
  
      DefaultTreeModel model = (DefaultTreeModel)vueTree.getModel();
      restoreModel(model,rootNode,rootSNode);
          vueTree.expandRow(0);
     
      return vueTree;
           
          
      
 
  }
   
   public void restoreModel(DefaultTreeModel model, Object obj, SaveNode rootSNode){
      
                
               
                
                Vector v = rootSNode.getChildren();
                
                //System.out.println("Thi s is v" + v);
                
                
                if (v != null){
                    
              
                    int i = v.size();
                
                 
                while (i > 0){
                    
                        i = i -1;
                     
                   SaveNode nextSNode = (SaveNode)v.elementAt(i);
                   
                   
                     if (nextSNode.getNodeType() == "FavortiesNode"){ 
                           
                     FavoritesNode nextNode = new FavoritesNode(nextSNode.getResourceName());
                      if (obj instanceof FavoritesNode){
                         model.insertNodeInto(nextNode, (FavoritesNode)obj, 0);
                         }
                         else if (obj instanceof FileNode){
                         model.insertNodeInto(nextNode,(FileNode)obj,0);
                         }
                         else if (obj instanceof AssetNode){
                         model.insertNodeInto(nextNode,(AssetNode)obj,0);
                             
                         }
                         else{
                          model.insertNodeInto(nextNode,(DefaultMutableTreeNode)obj,0); 
                         
                         }
                         
                              
                            restoreModel(model, nextNode, nextSNode);
                     
                   
                     }
                     else if (nextSNode.getNodeType() == "FileNode"){
                      
                      FileNode nextNode = new FileNode(new File(nextSNode.getResourceName()));
                      
                       if (obj instanceof FavoritesNode){
                         model.insertNodeInto(nextNode, (FavoritesNode)obj, 0);
                         }
                         else if (obj instanceof FileNode){
                         model.insertNodeInto(nextNode,(FileNode)obj,0);
                         }
                         else if (obj instanceof AssetNode){
                         model.insertNodeInto(nextNode,(AssetNode)obj,0);
                             
                         }
                         else{
                          model.insertNodeInto(nextNode,(DefaultMutableTreeNode)obj,0); 
                         
                         }
                         
                              
                            restoreModel(model, nextNode, nextSNode);
                     }
                   
                     else if (nextSNode.getNodeType() == "AssetNode"){
                       DefaultMutableTreeNode  nextNode = new DefaultMutableTreeNode(nextSNode.getResourceName());
                        if (obj instanceof FavoritesNode){
                         model.insertNodeInto(nextNode, (FavoritesNode)obj, 0);
                         }
                         else if (obj instanceof FileNode){
                         model.insertNodeInto(nextNode,(FileNode)obj,0);
                         }
                         else if (obj instanceof AssetNode){
                         model.insertNodeInto(nextNode,(AssetNode)obj,0);
                             
                         }
                         else{
                          model.insertNodeInto(nextNode,(DefaultMutableTreeNode)obj,0); 
                         
                         }
                         
                              
                            restoreModel(model, nextNode, nextSNode);
                     }
                   
                     else {
                         
                         DefaultMutableTreeNode nextNode = new DefaultMutableTreeNode(nextSNode.getResourceName());
                          if (obj instanceof FavoritesNode){
                         model.insertNodeInto(nextNode, (FavoritesNode)obj, 0);
                         }
                         else if (obj instanceof FileNode){
                         model.insertNodeInto(nextNode,(FileNode)obj,0);
                         }
                         else if (obj instanceof AssetNode){
                         model.insertNodeInto(nextNode,(AssetNode)obj,0);
                             
                         }
                         else{
                          model.insertNodeInto(nextNode,(DefaultMutableTreeNode)obj,0); 
                         
                         }
                              
                            restoreModel(model, nextNode, nextSNode);
                         
                           }
                         
                         
                     
                         
                        
                         
                         
                         }
                }
               
        
            }
  
}
