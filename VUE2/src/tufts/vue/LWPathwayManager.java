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
    
    private ArrayList pathways = new ArrayList();
    private LWPathway current = null;
    
    public LWPathwayManager() {
        pathways = new ArrayList();
    }
    
    public java.util.Iterator getPathwayIterator() {
        return pathways.iterator();
    }
    
    public LWPathway getPathway(int index)
    {
        return (LWPathway)pathways.get(index);
    }

    public void setPathways(ArrayList pathways)
    {
        this.pathways = pathways;
    }
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        System.out.println("current pathway in manager being set to: "+pathway);
        current = pathway;
        VUE.getPathwayInspector().setPathway(pathway);
    }
   
    public LWPathway getFirst(){
        if (length() != 0)
            return (LWPathway)pathways.get(0);        
        else
            return null;
    }
    
    public LWPathway getLast(){
        if (length() != 0)
            return (LWPathway)pathways.get(length() - 1);        
        else
            return null;
    }
    
    public int length(){
        return pathways.size();
    }
    
    public void addPathway(LWPathway pathway){
        pathways.add(pathway);
        
        //if(current == null)
        current = pathway;              
    }
    
    public void removePathway(LWPathway pathway){
        pathways.remove(pathway);
        
        if(current == pathway)
          current = getFirst();
    }
    
    /*    
    public void setPathway(int index, LWPathway pathway)
    {
        pathways.set(index, pathway);
    }
    
    public ArrayList getPathways()
    {
        return pathways;
    }
    */    
}
