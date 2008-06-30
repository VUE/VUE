/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.util.*;

/**
 * Keeps a list of all pathways for a given LWMap.  Also tracks the
 * current pathway selected for the given map.  Provides for
 * aggregating all the events that happen within the pathways and
 * their contents, and rebroadcasting them to interested parties, such
 * as the PathwayTableModel.
 *
 * @version $Revision: 1.36 $ / $Date: 2008-06-30 20:52:55 $ / $Author: mike $
 * @author Scott Fraize
 *
 */

public class LWPathwayList implements LWComponent.Listener, Iterable<LWPathway>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWPathwayList.class);

    private List<LWPathway> mPathways = new java.util.ArrayList();
    private LWMap mMap = null;
    private LWPathway mActive = null;
    private LWPathway mRevealer = null; // currently active "revealer" pathway, if any
    private LWChangeSupport mChangeSupport = new LWChangeSupport(this);

    public LWPathwayList(LWMap map) {
        setMap(map);
        // Always include an untitled example pathway for new maps
        LWPathway defaultPath = new LWPathway(map,VueResources.getString("pathways.defaultPathway.label"));
        VUE.setActive(LWPathway.class, this, defaultPath);
        //defaultPath.setVisible(false);
        add(defaultPath);
    }

    public LWPathway getRevealer() {
        return mRevealer;
    }

    public void setRevealer(LWPathway newRevealer) {
        if (mRevealer == newRevealer)
            return;
        Object old = mRevealer;
        if (mRevealer != null)
            mRevealer.setRevealer(false);
        mRevealer = newRevealer;
        if (newRevealer != null)
            newRevealer.setRevealer(true);
        notify(newRevealer, "pathway.revealer", new Undoable(old) { void undo() { setRevealer((LWPathway)old); }});
    }
    
    public int getRevealerIndex() {
        return mRevealer == null ? -1 : indexOf(mRevealer);
    }
    public void setRevealerIndex(int i) {
        if (mInRestore)
            mRevealerIndex = i;
        else {
            if (i >= 0 && i < size())
                mRevealer = (LWPathway) get(i);
            else
                mRevealer = null;
        }
    }
    
    public void setMap(LWMap map){
        mMap = map;
    }
    
    public LWMap getMap() {
        return mMap;
    }

    void completeXMLRestore(LWMap map)
    {
        if (DEBUG.INIT || DEBUG.IO || DEBUG.XML) Log.debug(this + " completeXMLRestore");
        setMap(map);
        Iterator i = iterator();
        while (i.hasNext()) {
            LWPathway p = (LWPathway) i.next();
            p.completeXMLRestore(getMap());
            p.addLWCListener(this);
        }
        if (mActive == null && mPathways.size() > 0)
            setActivePathway(getFirst());
        mInRestore = false;
        setRevealerIndex(mRevealerIndex);
    }

    /**
     * Add a listener for our children.  The LWPathwayList
     * listeners for and just rebroadcasts any events
     * from it's LWPathways.
     */
    public void addListener(LWComponent.Listener l) {
        mChangeSupport.addListener(l);
    }
    public void removeListener(LWComponent.Listener l) {
        mChangeSupport.removeListener(l);
    }

    private void notify(LWPathway changingPathway, String propertyKeyName, Object oldValue) {
        LWCEvent e = new LWCEvent(this, changingPathway, propertyKeyName, oldValue);
        // dispatch the event to map for it's listeners, particularly an undo manager if present
        getMap().notifyProxy(e);
        // dispatch the event to any direct listeners of this LWPathwayList
        // (such as the Pathway gui code in PathwayTableModel)
        mChangeSupport.dispatchEvent(e);
    }

    public Collection<LWPathway> getElementList() {
        return mPathways;
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
            notify(pathway, "pathway.list.active", LWCEvent.NO_OLD_VALUE);
        }
    }

    private Object get(int i) { return mPathways.get(i); }
    public int size() { return mPathways.size(); }
    public Iterator<LWPathway> iterator() { return mPathways.iterator(); }
    //public List<LWPathway> getList() { return mPathways; } // todo: should be unmodifiable
    public int indexOf(Object o) { return mPathways.indexOf(o); }
   
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
        mPathways.add(p);
        // we don't need to worry about calling removeFromModel on remove, as a created pathway will always
        // be empty by the time we get to undo it's create (because we'll have undone any child add's first).
        notify(p, "pathway.created", new Undoable(p) { void undo() { remove((LWPathway)old); }});
        setActivePathway(p);
        p.addLWCListener(this);
    }
    
    public void addPathway(LWPathway pathway){
        add(pathway);
    }

    public void remove(LWPathway p)
    {
        p.removeFromModel();
        if (!mPathways.remove(p))
            throw new IllegalStateException(this + " didn't contain " + p + " for removal");

        notify(p, "pathway.deleted",
               new Undoable(p) {
                   void undo() {
                       LWPathway p = (LWPathway) old;
                       p.restoreToModel();
                       add(p);
                   }
               });

        if (mActive == p)
        {
        	if (getFirst() == null)
        		VUE.setActive(LWPathway.class, this, null);
        	else
        		setActivePathway(getFirst());
        }
        if (mRevealer == p)
            setRevealer(null);
        
    }

    public void LWCChanged(LWCEvent e) {
        //if (DEBUG.PATHWAY) System.out.println(this + " " + e + " REBROADCASTING");
        mChangeSupport.dispatchEvent(e);
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

    /** @deprecated - persistance constructor only */
    private transient boolean mInRestore;
    private transient int mRevealerIndex = -1;
    public LWPathwayList() {
        mInRestore = true;
    }
    
    
}
