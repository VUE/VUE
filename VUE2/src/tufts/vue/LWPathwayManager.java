/*
 * LWPathwayManager.java
 *
 * Created on June 23, 2003, 3:03 PM
 *
 * Keeps arraylist of all pathways for a given concept map
 * Also tracks the current pathway selected for the given map
 */

package tufts.vue;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayManager {
    
    private ArrayList pathways = null;
    private LWPathway current = null;
    private LWMap map = null;
    private boolean currentOpen = false;
    
    /* beginning of castor mapping methods */
    
    /* constructors */
    public LWPathwayManager() {
        pathways = new ArrayList();
    }
    
    public LWPathwayManager(LWMap map) {
        pathways = new ArrayList();
        this.map = map;
    }
    
    /* get and set the concept map for the arraylist of pathways*/
    public void setMap(LWMap map){
        this.map = map;
    }
    
    public LWMap getMap(){
        return map;
    }
    
    /* iterates through arraylist of pathways elements */
    public java.util.Iterator getPathwayIterator() {
        return pathways.iterator();
    }
    
    /* methods for accessing data from and manipulating the arraylist */
    public Object getPathwaysElement(int index){
        return pathways.get(index);
    }
    
    public boolean getCurrentOpen(){
        return this.currentOpen;
    }
    
    public void setCurrentOpen(boolean currentOpen){
        this.currentOpen = currentOpen;
        if(this.currentOpen){
            this.showPathwayElements();
        }else{
            this.hidePathwayElements();
        }
    }
    
    public void setCurrentOpen(){
        boolean newValue = !this.currentOpen;
        this.setCurrentOpen(newValue);
    }
    
    public int getPathwayIndex(LWPathway path){
        return this.pathways.indexOf(path);
    }

    public void setPathwayList(ArrayList pathways){
        this.pathways = pathways;
    }
     
    public ArrayList getPathwayList(){
        return pathways;
    }
    
    public void addPathwayElement(LWComponent comp){
        if(this.getCurrentOpen()){
            this.hidePathwayElements();
            this.getCurrentPathway().addElement(comp);
            this.showPathwayElements();
        }else
            this.getCurrentPathway().addElement(comp);
    }
    
    public void addPathwayElements(LWComponent[] array){
        if(this.getCurrentOpen()){
            this.hidePathwayElements();
            for (int i = 0; i < array.length; i++){
                this.getCurrentPathway().addElement(array[i]);                        
            }
            this.showPathwayElements();
        }else{
            for (int i = 0; i < array.length; i++){
                this.getCurrentPathway().addElement(array[i]);                        
            }
        }
    }
    
    private void showPathwayElements(){
        LWPathway pathway = this.getCurrentPathway();
        java.util.List list = pathway.getElementList();
        this.pathways.addAll(this.getCurrentIndex()+1, list);
    }
    
    private void hidePathwayElements(){
        LWPathway pathway = this.getCurrentPathway();
        Iterator iter = pathway.getElementIterator();
        while(iter.hasNext()){
            LWComponent comp = (LWComponent)iter.next();
            if(pathways.contains(comp))
                pathways.remove(comp);   
        }
    }
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        if(this.getCurrentPathway() != null && !this.getCurrentPathway().equals(pathway) && this.getCurrentOpen())
            this.hidePathwayElements();
        current = pathway;
        VUE.getActiveViewer().repaint();
    }
   
    public Object getFirst(){
        if (length() != 0)
            return pathways.get(0);        
        else
            return null;
    }
    
    /**Interface for Castor by Daisuke Fujiwara
       In order to prevent redundancy, the current pathway is saved as an index instead of the entire pathway
     */
    
    public int getCurrentIndex()
    {
        return pathways.indexOf(current);
    }
    
    public void setCurrentIndex(int index)
    {
        try{
            this.setCurrentPathway((LWPathway)pathways.get(index));
        }catch (IndexOutOfBoundsException ie){
            if(this.getFirst() != null)
                this.setCurrentPathway((LWPathway)this.getFirst());
            else
                this.setCurrentPathway(null);
        }
    }
    
    /**End of Castor Interface*/
    
    public Object getLast(){
        if (length() != 0)
            return pathways.get(length() - 1);        
        else
            return null;
    }
    
    public int length(){
        return pathways.size();
    }
    
    public void addPathway(LWPathway pathway){
        pathways.add(pathway);
        this.setCurrentPathway(pathway);
    }
    
    public void removeElement(LWComponent comp){
        if(this.getCurrentOpen()){
            this.hidePathwayElements();
            this.getCurrentPathway().removeElement(comp);
            this.showPathwayElements();
        }else
            this.getCurrentPathway().removeElement(comp);
    }
    
    public void removePathway(LWPathway pathway){
        pathways.remove(pathway);
        
        if(current.equals(pathway)){
            if(this.getFirst() != null && this.getFirst() instanceof LWPathway)
                this.setCurrentPathway((LWPathway)this.getFirst());
            else{
                this.setCurrentPathway(null);
            }
        }
            
    } 
}
