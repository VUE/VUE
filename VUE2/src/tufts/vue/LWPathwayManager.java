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
    
    public LWPathwayManager(int test){
        this();
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
        this.current = pathway;
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
    
<<<<<<< LWPathwayManager.java
    public void addPathway(LWPathway pathway){
        pathways.add(pathway);
        if(current == null)
            current = pathway;              
=======
    public boolean addPathway(LWPathway pathway){
        if(pathways.add(pathway)){
            if(current.equals(null))
                current = pathway;
            return true;    
        }
        else return false;
            
>>>>>>> 1.2
    }
    
<<<<<<< LWPathwayManager.java
    public void removePathway(LWPathway pathway){
        pathways.remove(pathway);
        if(current == pathway)
            current = getFirst();
=======
    public boolean removePathway(LWPathway pathway){
        if(pathways.remove(pathway)){
            if(current.equals(pathway))
                current = getFirst();
            return true;    
        }
        else return false;
>>>>>>> 1.2
    }
}
