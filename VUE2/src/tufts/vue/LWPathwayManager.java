/*
 * LWPathwayManager.java
 *
 * Created on June 23, 2003, 3:03 PM
 */

package tufts.vue;

import java.util.ArrayList;



/**
 *
 * @author  Jay Briedis
 */
public class LWPathwayManager {
    
    private ArrayList pathways = null;
    private LWPathway current = null;
    
    public LWPathwayManager() {
        pathways = new ArrayList();
    }
    
    public java.util.Iterator getPathwayIterator() {
        return pathways.iterator();
    }
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        this.current = pathway;
    }
   
    public LWPathway getFirst(){
        return (LWPathway)pathways.get(0);
    }
    
    public LWPathway getLast(){
        return (LWPathway)pathways.get(length());
    }
    
    public int length(){
        return pathways.size();
    }
    
    public boolean addPathway(LWPathway pathway){
        if(pathways.add(pathway)){
            if(current.equals(null))
                current = pathway;
            return true;    
        }else return false;
            
    }
    
    public boolean removePathway(LWPathway pathway){
        if(pathways.remove(pathway)){
            if(current.equals(pathway))
                current = getFirst();
            return true;    
        }else return false;
    }
}
