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
    
    private static ArrayList pathways = new ArrayList();
    private static LWPathway current = new LWPathway();
    private static LWPathwayManager manager = null;
    
    private LWPathwayManager() {
        pathways = new ArrayList();
    }
    
    /** Constructor used for testing */
    /*
    private LWPathwayManager(int test){
        if(test == 1){
            LWPathway path1 = new LWPathway("Path 1");
            LWPathway path2 = new LWPathway("Path 2");
            LWPathway path3 = new LWPathway("Path 3");
            LWPathway path4 = new LWPathway("Path 4");
            LWPathway path5 = new LWPathway("Path 5");
            this.addPathway(path1);
            this.addPathway(path2);
            this.addPathway(path3);
            this.addPathway(path4);
            this.addPathway(path5);
        }
    }*/
    
    public static LWPathwayManager getInstance(){
        if(manager==null) manager = new LWPathwayManager();       
        return manager;
    }
    
    public java.util.Iterator getPathwayIterator() {
        return pathways.iterator();
    }
    
    public LWPathway getPathway(int index)
    {
        return (LWPathway)pathways.get(index);
    }
    
    public void setPathway(int index, LWPathway pathway)
    {
        pathways.set(index, pathway);
    }
    
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        current = pathway;
    }
   
    public LWPathway getFirst(){
        return (LWPathway)pathways.get(0);
    }
    
    public LWPathway getLast(){
        return (LWPathway)pathways.get(length() - 1);
    }
    
    public int length(){
        return pathways.size();
    }
    

    public void addPathway(LWPathway pathway){
        pathways.add(pathway);
        if(current == null)
            current = pathway;              
        //System.out.println("manager adding a pathway...");
    }
    
    public void removePathway(LWPathway pathway){
        pathways.remove(pathway);
        if(current == pathway)
            current = getFirst();
    }
}
