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
      
                
               int FAVORITES = 4;
                
                Vector v = rootSNode.getChildren();
                
              System.out.println("Thi s is v FWWWWW" + v);
                
                 if (v != null){
                    
                    
                        int i = v.size();
                                while (i > 0){
                                 i = i -1;
                                SaveNode nextSNode = (SaveNode)v.elementAt(i);
                                if (nextSNode.getResource() instanceof CabinetResource){
                                    
                                    System.out.println("oopsy daisy");
                                }
                                else{
                                  System.out.println("I am a ha" + nextSNode.getResource().getType());
                                }  
                                  if (((nextSNode.getResource()).getType()) == FAVORITES){
                                    //System.out.println("I am a favorite" + nextSNode.getResource());
                                    FavoritesNode nextFNode = new FavoritesNode(nextSNode.getResource());
                                     model.insertNodeInto(nextFNode,rootNode,0); 
                                    restoreModel(model, nextFNode, nextSNode);
                                }
                                else{
                                    
                                    ResourceNode nextNode = new ResourceNode(nextSNode.getResource());
                                     model.insertNodeInto(nextNode,rootNode,0); 
                                     restoreModel(model, nextNode, nextSNode);
                                }
                                
                                              }
                                      }
                  
                        
               
   }
   
}
