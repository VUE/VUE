package tufts.vue;

import java.util.*;

/**
 * LWPathwayList.java
 *
 * Keeps a list of all pathways for a given LWMap.  Also tracks the
 * current pathway selected for the given map.  Provides for
 * aggregating all the events that happen with in the pathways and
 * their contents, and rebroadcasting them to interested parties, such
 * as the PathwayTableModel.
 *
 * @author Scott Fraize
 * @version February 20
 *
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
        if (mActive != pathway) {
            mActive = pathway;
            getMap().notify(this, "pathway.list.active");
        }
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
    
    public void add(LWPathway p) {
        p.setMap(getMap());
        mElements.add(p);
        setActivePathway(p);
        LWCEvent e = new LWCEvent(this, p, "pathway.create", new Undoable(p) { void undo() { remove((LWPathway)old); }} );
        // dispatch the event to map for it's listeners, particularly an undo manager if present
        getMap().notifyProxy(e);
        // dispatch the event to any direct listeners of this LWPathwayList
        // (such as the Pathway gui code)
        LWComponent.dispatchLWCEvent(this, mListeners, e);
        p.addLWCListener(this);
    }
    
    public void addPathway(LWPathway pathway){
        add(pathway);
    }

    public void remove(LWPathway p)
    {
        // p.removeLWCListener(this); // happens auto in removeFromModel
        p.removeFromModel();
        if (!mElements.remove(p))
            throw new IllegalStateException(this + " didn't contain " + p + " for removal");
        if (mActive == p)
            setActivePathway(getFirst());

        LWCEvent e = new LWCEvent(this, p, "pathway.delete", new Undoable(p) { void undo() { add((LWPathway)old); }} );
        getMap().notifyProxy(e);
        LWComponent.dispatchLWCEvent(this, mListeners, e);
    }

    public void LWCChanged(LWCEvent e) {
        //if (DEBUG.PATHWAY) System.out.println(this + " " + e + " REBROADCASTING");
        LWComponent.dispatchLWCEvent(this, mListeners, e);
    }
    
    public int getCurrentIndex() {
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
    
    public String toString()
    {
        return "LWPathwayList[n=" + size()
            + " map=" + (getMap()==null?"null":getMap().getLabel())
            + "]";
    }
}
