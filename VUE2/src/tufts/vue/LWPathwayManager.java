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
    private LWMap map = null;
    
    public LWPathwayManager() {
        pathways = new ArrayList();
    }
    
    public LWPathwayManager(LWMap map) {
        pathways = new ArrayList();
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
    
    public LWPathway getPathway(int index)
    {
        return (LWPathway)pathways.get(index);
    }

    public void setPathwayList(ArrayList pathways)
    {
        System.out.println("setting the pathways in the manager");
        this.pathways = pathways;
    }
     
    public ArrayList getPathwayList()
    {
        System.out.println("getting the pathways in the manager");
        return pathways;
    }
    
    public LWPathway getCurrentPathway() {
        return current;
    }
    
    public void setCurrentPathway(LWPathway pathway) {
        current = pathway;
        
        VUE.getPathwayInspector().setPathway(pathway);
        VUE.getActiveViewer().repaint();
    }
   
    public LWPathway getFirst(){
        if (length() != 0)
            return (LWPathway)pathways.get(0);        
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
        System.out.println("Current Pathway is now " + index);
        try
        {
            current = (LWPathway)pathways.get(index);
        }
        
        catch (IndexOutOfBoundsException ie)
        {
            current = null;
        }
    }
    
    /**End of Castor Interface*/
    
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
    */    
}
