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
 * @author Scott Fraize
 * @version Feb 2004
 */

public class LWPathwayList
{
    private java.util.List elements = new java.util.ArrayList();
    private LWMap mMap = null;
    private LWPathway mActive = null;

    /** persistance constructor only */
    public LWPathwayList() {}
    
    public LWPathwayList(LWMap map) {
        setMap(map);
    }
    
    public void setMap(LWMap map){
        mMap = map;
    }
    
    public LWMap getMap() {
        return mMap;
    }

    void completeXMLRestore(LWMap map)
    {
        System.out.println(this + " completeXMLRestore");
        setMap(map);
        Iterator i = iterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            p.completeXMLRestore(getMap());
        }
    }

    public Collection getElementList() {
        return elements;
    }
    
    /*
    public int getPathwayIndex(LWPathway p) {
        return indexOf(p);
    }
    */

    /*
    public void setPathwayList(ArrayList pathways){
        this.pathways = pathways;
    }
    public ArrayList getPathwayList(){
        return pathways;
    }
    */
    /*
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
    

    /*
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
    */

    //Object getPathwaysElement(int row) {return null;}//tmp smf
    //LWPathway getPathwayforElementAt(int row) {return null;}//tmp smf
    
    public LWPathway getActivePathway() {
        return mActive;
    }
    
    public void setActivePathway(LWPathway pathway) {
        mActive = pathway;
        
        if (VUE.getActiveMap() != null)
            VUE.getActiveMap().notify(this, LWCEvent.Repaint);
        //VUE.getPathwayInspector().repaint();
    }

    private Object get(int i) { return elements.get(i); }
    public int size() { return elements.size(); }
    public Iterator iterator() { return elements.iterator(); }
    public int indexOf(Object o) { return elements.indexOf(o); }
   
    public LWPathway getFirst(){
        if (size() != 0)
            return (LWPathway) get(0);
        else
            return null;
    }
    
    public LWPathway getLast(){
        if (size() != 0)
            return (LWPathway) get(size() - 1);
        else
            return null;
    }
    
    public boolean add(Object o) {
        LWPathway p = (LWPathway) o;
        setActivePathway(p);
        return elements.add(p);
    }
    
    public void addPathway(LWPathway pathway){
        add(pathway);
    }

    public boolean remove(Object o)
    {
        LWPathway p = (LWPathway) o;
        p.removeFromModel();
        if (mActive == p)
            setActivePathway(getFirst());
        return elements.remove(p);
    }
    
    /**Interface for Castor by Daisuke Fujiwara
       In order to prevent redundancy, the current pathway is saved as an index instead of the entire pathway
     */
    
    public int getCurrentIndex()
    {
        return indexOf(mActive);
    }
    
    public void setCurrentIndex(int index)
    {
        try {
            setActivePathway((LWPathway) get(index));
        } catch (IndexOutOfBoundsException ie){
            setActivePathway(getFirst());
        }
    }
    
    /**End of Castor Interface*/
    
    public String toString()
    {
        return "LWPathwayList[size=" + size()
            + " map=" + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }
}
