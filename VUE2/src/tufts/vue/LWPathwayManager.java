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
    private HashMap pathwaysNotes = null;
    private LWPathway current = null;
    private LWMap map = null;
    
    public LWPathwayManager() {
        pathways = new ArrayList();
        pathwaysNotes = new HashMap();
    }
    
    public LWPathwayManager(LWMap map) {
        pathways = new ArrayList();
        pathwaysNotes = new HashMap();
        this.map = map;
    }
    
    public void setMap(LWMap map){
        this.map = map;
    }
    
    public LWMap getMap(){
        return map;
    }

    public java.util.Iterator getPathwayIterator() {
        return pathways.iterator();
    }
    
    void completeXMLRestore()
    {
        System.out.println(this + " completeXMLRestore");
        Iterator i = getPathwayIterator();
        while (i.hasNext()) {
            Object obj = (Object)i.next();
            if(obj instanceof LWPathway){
                LWPathway p = (LWPathway) obj;
                p.completeXMLRestore(getMap());
            }
        }
    }
    
    public Object getPathwaysElement(int index){
        if(index > -1)
            return pathways.get(index);
        return null;
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
    }
    
    public void setPathOpen(LWPathway path){
        this.hidePathwayElements();
        path.setOpen();
        this.showPathwayElements();
    }
    
    private void hidePathwayElements(){
        
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
        if(index > 0){
            for(int i = index; i > -1; i--){
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
        current = pathway;
        
        if (VUE.getActiveMap() != null)
            VUE.getActiveMap().notify(this, LWCEvent.Repaint);
        
        VUE.getPathwayInspector().repaint();
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
        pathway.removeFromModel();
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
