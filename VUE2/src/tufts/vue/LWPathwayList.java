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

/**
 * @author Scott Fraize
 * @version February 2004
 */

public class LWPathwayList implements LWComponent.Listener
{
    private List mElements = new java.util.ArrayList();
    private LWMap mMap = null;
    private LWPathway mActive = null;
    private List mListeners = new java.util.ArrayList();

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

    /**
     * Add a listener for our children.  The LWPathwayList
     * listeners for and just rebroadcasts any events
     * from it's LWPathways.
     */
    public void addListener(LWComponent.Listener l) {
        mListeners.add(l);
    }
    public void removeListener(LWComponent.Listener l) {
        mListeners.remove(l);
    }

    public Collection getElementList() {
        return mElements;
    }
    
    public LWPathway getActivePathway() {
        return mActive;
    }
    
    /**
     * If this LWPathwayList is from the VUE active map, this sets
     * the VUE application master for the active pathway.
     */
    public void setActivePathway(LWPathway pathway) {
        mActive = pathway;
        
        if (VUE.getActiveMap() != null)
            VUE.getActiveMap().notify(this, LWCEvent.Repaint);
    }

    private Object get(int i) { return mElements.get(i); }
    public int size() { return mElements.size(); }
    public Iterator iterator() { return mElements.iterator(); }
    public int indexOf(Object o) { return mElements.indexOf(o); }
   
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
    
    public boolean add(LWPathway p) {
        p.setMap(getMap());
        setActivePathway(p);
        p.addLWCListener(this);
        return mElements.add(p);
    }
    
    public void addPathway(LWPathway pathway){
        add(pathway);
    }

    public boolean remove(LWPathway p)
    {
        p.removeLWCListener(this);
        p.removeFromModel();
        if (mActive == p)
            setActivePathway(getFirst());
        return mElements.remove(p);
    }

    public void LWCChanged(LWCEvent e) {
        //if (DEBUG.PATHWAY) System.out.println(this + " " + e + " REBROADCASTING");
        LWComponent.dispatchLWCEvent(this, mListeners, e);
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
        return "LWPathwayList[n=" + size()
            + " map=" + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }
}
