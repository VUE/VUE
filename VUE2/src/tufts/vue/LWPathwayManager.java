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
    //private boolean currentOpen = false;
    
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
    
    void completeXMLRestore()
    {
        System.out.println(this + " completeXMLRestore");
        Iterator i = getPathwayIterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            p.completeXMLRestore(getMap());
        }
    }
    
    /* methods for accessing data from and manipulating the arraylist */
    public Object getPathwaysElement(int index){
        return pathways.get(index);
    }
    /*
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
    }*/
    
    public int getPathwayIndex(LWPathway path){
        return this.pathways.indexOf(path);
    }

    public void setPathwayList(ArrayList pathways){
        //System.out.println(this + " setPathwayList *** " + pathways);
        this.pathways = pathways;
    }
     
    public ArrayList getPathwayList(){
        //System.out.println(this + " getPathwayList *** " + pathways);
        return pathways;
    }
    
    public void addPathwayElement(LWComponent comp, LWPathway path){
        if(path.getOpen()){
            this.hidePathwayElements();
            path.addElement(comp);
            this.showPathwayElements();
        }else{
            path.addElement(comp);
        }
    }
    
    public void addPathwayElements(LWComponent[] array, LWPathway path){
        if(path.getOpen()){
            this.hidePathwayElements();
            for (int i = 0; i < array.length; i++){
                path.addElement(array[i]);                        
            }
            this.showPathwayElements();
        }else{
            for (int i = 0; i < array.length; i++){
                path.addElement(array[i]);                        
            }
        }
    }
    
    private void showPathwayElements(){
        ArrayList list = new ArrayList();
        for(int i = 0; i < pathways.size(); i++){
            if(pathways.get(i) instanceof LWPathway && ((LWPathway)pathways.get(i)).getOpen())
                list.add((LWPathway)pathways.get(i));
        }
        
        for(int j = 0; j < list.size(); j++){
            LWPathway path = (LWPathway)list.get(j);
            java.util.List elementList = path.getElementList();
            this.pathways.addAll(this.getPathwayIndex(path)+1, elementList);
        }
        
        /*boolean c = true;
        int index = 0;
        
        while(c){
            LWPathway path = null;
            
            for(int i = index; i < pathways.size(); i++){
                boolean chosen = false;
                if(pathways.get(i) instanceof LWPathway && ((LWPathway)pathways.get(i)).getOpen() && !chosen){
                    path = (LWPathway)pathways.get(i);
                    chosen = true;
                    index = i;
                }
            }
            
            if(path == null)
                c =false;
            else{
                java.util.List list = path.getElementList();
                this.pathways.addAll(this.getCurrentIndex()+1, list);
                index += list.size();
            }               
        }*/
    }
    
    public void setPathOpen(LWPathway path){
        this.hidePathwayElements();
        path.setOpen();
        this.showPathwayElements();
    }
    
    private void hidePathwayElements(){
        //LWPathway pathway = this.getCurrentPathway();
        
        ArrayList list = new ArrayList();
        for(int i = 0; i < pathways.size(); i++){
            if(pathways.get(i) instanceof LWComponent){
                list.add(pathways.get(i));
            }
        }
        
        for(int j = 0; j < list.size(); j++){
            if(list.get(j) instanceof LWComponent)
                pathways.remove(list.get(j));
        }
    }
    
    public LWPathway getPathwayforElementAt(int index){
        System.out.println("in pathway manager get p for e:: index: " +index);
        if(index > 0){
            for(int i = index; i > -1; i--){
                System.out.println("in pathway manager get p for e:: i: " +i+", inst: " + pathways.get(i).getClass());
                if(pathways.get(i) instanceof LWPathway){
                    return (LWPathway)pathways.get(i);
                }
            }
        }
        return null;
    }
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        //if(this.getCurrentPathway() != null && !this.getCurrentPathway().equals(pathway))
        //    this.hidePathwayElements();
        current = pathway;
        if (VUE.getActiveMap() != null)
            VUE.getActiveMap().notify(this, LWCEvent.Repaint);
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
        if(this.getCurrentPathway().getOpen()){
            this.hidePathwayElements();
            this.getCurrentPathway().removeElement(comp);
            this.showPathwayElements();
        }else
            this.getCurrentPathway().removeElement(comp);
    }
    
    public void removePathway(LWPathway pathway){
        this.hidePathwayElements();
        pathways.remove(pathway);
        this.showPathwayElements();
        if(current.equals(pathway)){
            if(this.getFirst() != null && this.getFirst() instanceof LWPathway)
                this.setCurrentPathway((LWPathway)this.getFirst());
            else{
                this.setCurrentPathway(null);
            }
        }
            
    }

    public String toString()
    {
        return "LWPathwayManger[pathways="
            + (pathways==null?-1:pathways.size())
            + " map=" + (map==null?"null":map.getLabel())
            + "]";
    }
}
