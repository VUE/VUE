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

import tufts.Util;

import java.util.*;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

/**
 *
 * Maintains the VUE global list of selected LWComponent's.
 *
 * @version $Revision: 1.90 $ / $Date: 2008-07-19 19:20:29 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */

// could have this listen to everyone in it, and then folks who want
// to monitor the selected object can only once regiester as a
// selectedEventListener, instead of always adding and removing from
// the individual objects.

public class LWSelection extends java.util.ArrayList<LWComponent>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWSelection.class);
    
    public interface Acceptor extends tufts.vue.Acceptor<LWComponent> {
        /** @return true if the given object acceptable for selection  */
        public boolean accept(LWComponent c);
    }
    private List listeners = new java.util.ArrayList();
    private List controlListeners = new java.util.LinkedList();
    private Rectangle2D.Float mBounds = null;
    private LWSelection lastSelection;
    
    private boolean isClone = false;

    private int mWidth = -1, mHeight = -1;  // only used for manually created selections

    private Object source;
    private LWComponent focal; // root of selection tree

    private long mEditablePropertyKeys;

    private final Set<LWContainer> mParents = new java.util.HashSet();
    private final Set<Class> mTypes = new java.util.HashSet();
    private List<LWComponent> mSecureList = null;

    public LWSelection() {}

    public Object getSource() {
        return source;
    }

    public void setSource(Object src) {
        this.source = src;
    }
    public void setSelectionSourceFocal(LWComponent focal) {
        this.focal = focal;
    }
    public LWComponent getSelectionSourceFocal() {
        return focal;
    }

    // currently only used for special case manually created selections
    public int getWidth() {
        return mWidth;
    }

    // currently only used for special case manually created selections
    public int getHeight() {
        return mHeight;
    }

    public boolean isSized() {
        return mWidth > 0 && mHeight > 0;
    }

    public void setSize(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    /** @return an unmodifable list of our contents useful for iteration */
    public List<LWComponent> contents() {
        if (mSecureList == null)
            mSecureList = java.util.Collections.unmodifiableList(this);
        return mSecureList;
    }

    /** create a temporary selection that contains just the given component */
    public LWSelection(LWComponent c) {
        //if (DEBUG.Enabled) tufts.Util.printStackTrace("selection singleton for " + c);
        isClone = true;
        addSilent(c);
    }

    /** create a temporary selection that contains just the given components */
    public LWSelection(java.util.List<? extends LWComponent> list) {
        isClone = true;
        super.addAll(list);
    }
    
    public interface Listener extends java.util.EventListener {
        void selectionChanged(LWSelection selection);
    }

    public static class Controller extends Point2D.Float
    {
        protected java.awt.Color color = VueConstants.COLOR_SELECTION_HANDLE;
        
        public Controller() {}
        public Controller(float x, float y) {
            super(x, y);
        }
        public Controller(java.awt.geom.Point2D p) {
            this((float)p.getX(), (float)p.getY());
        }

        public void setColor(java.awt.Color c) {
            this.color = c;
        }
        
        public java.awt.Color getColor() {
            return color;
        }

        /** @return override for an optional shape (besides the default square) */
        public RectangularShape getShape() { return null; }
        /** @return override for an optional rotation for the shape (in radians) */
        public double getRotation() { return 0; }

    }
    public interface ControlListener extends java.util.EventListener {
        void controlPointPressed(int index, MapMouseEvent e);
        void controlPointMoved(int index, MapMouseEvent e);
        void controlPointDropped(int index, MapMouseEvent e);
        Controller[] getControlPoints(double zoom);
    }
    

    private void addControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) debug("adding control listener " + listener);
        controlListeners.add(listener);
    }
    
    private void removeControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) debug("removing control listener " + listener);
        if (!controlListeners.remove(listener))
            throw new IllegalStateException(this + " didn't contain control listener " + listener);
    }

    java.util.List<ControlListener> getControlListeners()
    {
        return controlListeners;
    }

    public synchronized void addListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) debug("adding listener   " + l);
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) debug("removing listener " + l);
        listeners.remove(l);
    }

    public boolean inNotify() {
        return inNotify;
    }

    private Listener[] listener_buf = new Listener[128];
    private boolean inNotify = false;
    private synchronized void notifyListeners()
    {
        //if (isClone) throw new IllegalStateException(this + " clone's can't notify listeners! " + this);
        if (isClone) {
            if (DEBUG.Enabled) Log.debug(this + ": clone skipping notification");
            return;
        }

        if (notifyUnderway())
            return;
        
        try {
            inNotify = true;
        
            if (DEBUG.SELECTION || DEBUG.EVENTS) {
                if (DEBUG.SELECTION)
                    System.out.println("\n-----------------------------------------------------------------------------");
                debug("NOTIFYING " + listeners.size() + " LISTENERS");
            }
            Listener[] listener_iter = (Listener[]) listeners.toArray(listener_buf);
            int nlistener = listeners.size();
            long start = 0;
            for (int i = 0; i < nlistener; i++) {
                if (DEBUG.SELECTION && DEBUG.META) debug("notifying: #" + (i+1) + " " + (i<9?" ":""));
                Listener l = listener_iter[i];
                try {
                    if (DEBUG.SELECTION) {
                        System.err.format("%70s...", Util.tags(l));
                        start = System.currentTimeMillis();
                    }
                    l.selectionChanged(this);
                    if (DEBUG.SELECTION) {
                        long delta = System.currentTimeMillis() - start;
                        System.out.println(delta + "ms");
                    }
                } catch (Exception ex) {
                    System.err.println(this + " notifyListeners: exception during selection change notification:"
                                       + "\n\tselection: " + this
                                       + "\n\tfailing listener: " + l);
                    ex.printStackTrace();
                    //java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }

            if (size() == 1)
                VUE.setActive(LWComponent.class, this, first());
            else
                VUE.setActive(LWComponent.class, this, null);
            
        } finally {
            inNotify = false;
        }
    }

    private boolean notifyUnderway() {
        if (inNotify) {
            Util.printStackTrace(this + " attempt to change selection during selection change notification: denied. src="+source);
            return true;
        } else {
            return false;
        }
    }

    /** for Actions.java */
    java.util.List getListeners()
    {
        return this.listeners;
    }

    public synchronized void setTo(LWComponent c)
    {
        if (size() == 1 && first() == c)
            return;
        if (notifyUnderway())
            return;

        clearSilent();
        add(c);
        
    }
    
    public void setTo(Iterable bag)
    {
        setTo(bag.iterator());
    }
    
    public synchronized void setTo(Iterator i)
    {
        if (notifyUnderway())
            return;

        final boolean hadContents = !isEmpty();
        
        clearSilent();
        add(i);

        if (hadContents && isEmpty()) {
            // we ended up changing the the selection by
            // clearing it out and then setting it to nothing
            notifyListeners();
        }
        
    }
    
    synchronized public boolean add(LWComponent c)
    {
        if (notifyUnderway())
            return false;
        if (!c.isSelected()) {
            if (addSilent(c) && !isClone)
                notifyListeners();
        } else {
            if (DEBUG.SELECTION) debug("addToSelection(already): " + c);
            return false;
        }
        return true;
        
    }
    
    //public boolean add(Object o) {
    //throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    //}
    
    /** Make sure all in Iterable are in selection & do a single change notify at the end */
    public void add(Iterable<LWComponent> iterable) {
        add(iterable.iterator());
    }
        
    /** Make sure all in iterator are in selection & do a single change notify at the end */
    synchronized void add(Iterator<LWComponent> i)
    {
        if (notifyUnderway())
            return;
        
        LWComponent c;
        boolean changed = false;
        while (i.hasNext()) {
            c = i.next();
            if (!c.isSelected() && c.isDrawn()) {
                if (addSilent(c))
                    changed = true;
            }
        }
        if (changed)
            notifyListeners();
    }
    
    /** Change the selection status of all LWComponents in iterator */
    synchronized void toggle(Iterable<LWComponent> iterable)
    {
        if (notifyUnderway())
            return;
        
        boolean changed = false;
        boolean removed = false;
        for (LWComponent c : iterable) {
            if (c.isSelected()) {
                changed = true;
                removed = true;
                removeSilent(c);
            } else {
                if (addSilent(c))
                    changed = true;
            }
        }

        if (removed)
            resetStatistics();
        
        if (changed)
            notifyListeners();
    }

    protected boolean isSelectable(LWComponent c) {
        return c instanceof LWMap.Layer == false;
    }
    
    private synchronized boolean addSilent(LWComponent c)
    {
        if (DEBUG.SELECTION && DEBUG.META || DEBUG.EVENTS) debug("addSilent " + c + " src=" + source);

        if (c == null) {
            tufts.Util.printStackTrace("can't add null to a selection");
            return false;
        }

        if (!isSelectable(c)) {
            Util.printStackTrace("not allowed to select " + c);
            return false;
        }

        if (notifyUnderway())
            return false;

        if (!isClone && size() > 0) { // clones can break the rules if they want
            
            // special case for items such as LWMap or LWPathway. We do NOT allow 
            // instances of these items to be added to the selection if there's anything else in
            // it, and if there's already a one of them in the selection, clear it out if we add
            // anything else.  (because these items cannot be operated on with other
            // items on the map at the same time: e.g., dragged, or show their boundary)
            
            if (!first().supportsMultiSelection())
                clearSilent();
            else if (!c.supportsMultiSelection())
                return false; // don't add
        }

        if (DEBUG.SELECTION) debug("add " + c);
        
        if (!c.isSelected()) {
            if (!isClone) c.setSelected(true);
            super.add(c);
            mBounds = null;
            mTypes.add(c.getClass());
            mParents.add(c.getParent());
            mEditablePropertyKeys = 0;  // set to recompute
            if (!isClone && c instanceof ControlListener)
                addControlListener((ControlListener)c);
            return true;
        } else
            throw new RuntimeException(this + " attempt to add already selected component " + c);
    }
    
//     public synchronized void remove(LWComponent c)
//     {
//         removeSilent(c);
//         resetStatistics();
//         notifyListeners();
//     }

    private synchronized void removeSilent(LWComponent c)
    {
        if (DEBUG.SELECTION) debug("remove " + c);
        if (notifyUnderway())
            return;
        if (!isClone) c.setSelected(false);
        if (!isClone && c instanceof ControlListener)
            removeControlListener((ControlListener)c);

        if (!super.remove(c))
            throw new RuntimeException(this + " remove: list doesn't contain " + c);
        
    }
    
    /**
     * clearAndNotify
     * This emthod clears teh selection and always notifies
     * listeners of a change.
     *
     **/
    public synchronized void clearAndNotify() {
    	clearSilent();
        if (DEBUG.SELECTION) debug("clearAndNotify: forced notification after clear");
    	notifyListeners();
    }
    
    public synchronized void clear()
    {
        if (clearSilent())
            notifyListeners();
    }

    public synchronized void reselect() {
        if (lastSelection != null) {
            LWSelection reselecting = lastSelection;
            lastSelection = null;
            clearSilent();
            add(reselecting);
        } else 
            clear();
    }

    private synchronized boolean clearSilent()
    {
        if (isEmpty())
            return false;
        if (notifyUnderway())
            return false;

        if (DEBUG.SELECTION) debug("clearSilent");

        if (!isClone) {
            for (LWComponent c : this)
                c.setSelected(false);
            lastSelection = clone();
        }
        
        controlListeners.clear();

        super.clear();

        resetStatistics();
        
        return true;
    }

    /** Remove from selection anything that's been deleted */


    synchronized void clearDeleted()
    {
        // This is special case code called by the UndoManager during an undo whenever
        // there are any hierarchy changes.  Would be cleaner to for the selection to
        // listen to all it's members for deletion events (expensive to always
        // add/remove all those listeners), or listen to the map for all deletion events
        // and and check if any are on our members.

        if (DEBUG.SELECTION) debug("clearDeleted");
        boolean removed = false;
        LWComponent[] elements = new LWComponent[size()];
        toArray(elements);
        for (int i = 0; i < elements.length; i++) {
            LWComponent c = elements[i];
            if (c.isDeleted()) {
                if (DEBUG.SELECTION) debug("clearDeleted: clearing " + c);
                removeSilent(c);
                removed = true;
            }
        }
        if (removed) {
            resetStatistics();
            notifyListeners();
        }
    }

    private void resetStatistics() {
        mBounds = null;
        mTypes.clear();
        mParents.clear();
        mEditablePropertyKeys = 0; // set to recompute
        if (size() > 0) {
            if (DEBUG.Enabled) Log.debug("RECOMPUTING STATISTICS");
            for (LWComponent c : this) {
                mParents.add(c.getParent());
                mTypes.add(c.getClass());
            }
        }
    }

    /** return bounds of map selection in map (not screen) coordinates */
    public Rectangle2D getBounds()
    {
        if (size() == 0)
            return null;
        //todo:not really safe to cache as we don't know if anything in has has moved?
        //if (bounds == null) {
        mBounds = LWMap.getBounds(iterator());
            //System.out.println("COMPUTED SELECTION BOUNDS=" + bounds);
            //}

        if (isSized()) {
            mBounds.width = mWidth;
            mBounds.height = mHeight;
        }
                
        return mBounds;
    }

//     /** return shape bounds of map selection in map (not screen) coordinates
//      * Does NOT inclde any stroke widths. */
//     public Rectangle2D getShapeBounds()
//     {
//         if (size() == 0)
//             return null;
//         return LWMap.getBounds(iterator());
//     }

    void flushBounds()
    {
        mBounds = null;
    }

    public boolean contains(float mapX, float mapY)
    {
        if (size() == 0)
            return false;
	return getBounds().contains(mapX, mapY);
    }

    public LWComponent first() {
        return size() == 0 ? null : get(0);
    }

    /** @return the single component in the selection if there is only one, otherwise, null */
    public LWComponent only() {
        return size() == 1 ? get(0) : null;
    }
    
    public LWComponent last()
    {
        return size() == 0 ? null : (LWComponent) get(size()-1);
    }
    
    public int countTypes(Class clazz)
    {
        int count = 0;

        for (Class contentClass : mTypes)
            if (clazz.isAssignableFrom(contentClass))
                count++;
        
//         Iterator i = iterator();
//         while (i.hasNext())
//             if (clazz.isInstance(i.next()))
//                 count++;
        
        return count;
    }
    
    public boolean containsType(Class clazz)
    {
        for (Class contentClass : mTypes)
            if (clazz.isAssignableFrom(contentClass))
        return true;
        
//         Iterator i = iterator();
//         while (i.hasNext())
//             if (clazz.isInstance(i.next()))
//                 return true;
        
        return false;
    }
    
    public boolean allOfType(Class clazz)
    {
        for (Class contentClass : mTypes)
            if (!clazz.isAssignableFrom(contentClass))
                return false;
        
        return size() != 0;
        
//         Iterator i = iterator();
//         while (i.hasNext())
//             if (!clazz.isInstance(i.next()))
//                 return false;
//         return size() != 0;
    }

    public boolean allOfSameType()
    {
        return size() < 2 || mTypes.size() == 1;
        
//         if (size() <= 1)
//             return true;
//         final Iterator i = iterator();
//         final Class firstType = i.next().getClass();
//         while (i.hasNext())
//             if (i.next().getClass() != firstType)
//                 return false;
//         return true;
    }

    public Collection<LWContainer> getParents() {
        return mParents;
    }

    public boolean allHaveSameParent()
    {
        return mParents.size() < 2;
    }
    
    public boolean allHaveSameParentOfType(Class clazz)
    {
        if (size() == 0)
            return false;

        return allHaveSameParent() && clazz.isInstance(first().getParent());
    }

    public LWComponent[] asArray()
    {
        LWComponent[] array = new LWComponent[size()];
        super.toArray(array);
        return array;
    }

    public long getEditablePropertyBits() {
        if (mEditablePropertyKeys == 0 && !isEmpty()) {
            // keys may be zero if something was removed from the
            // selection, requiring us to recompute from scratch
            for (LWComponent c : this)
                mEditablePropertyKeys |= c.getSupportedPropertyBits();
        }
            
        return mEditablePropertyKeys;
    }
    
    public boolean hasEditableProperty(Object key) {
        if (key instanceof LWComponent.Key)
            return ( getEditablePropertyBits() & ((LWComponent.Key)key).bit ) != 0;
        else
            return true;
    }

    @Override
    public LWSelection clone()
    {
        LWSelection copy = (LWSelection) super.clone();
        copy.isClone = true;
        // if anybody tries to use these we want a NPE
        copy.listeners = null;
        copy.controlListeners = null;
        return copy;
    }

    private void debug(String s) {
        Log.debug(String.format("%s %s", this, s));
        //Log.debug(String.format("[%s] %s", paramString(), s));
    }

    protected String paramString()
    {
        String content = (size() != 1 ? "" : " (" + first().toString() + ")");

        return String.format("%d src=%s%s", size(), source, isClone ? " CLONE" : content);
                             
//         return "LWSelection["
//             + size()
//             + " src=" + source
//             + 
//             (isClone
//              ? " CLONE]"
//              : ("]" + content)
//              );
    }

    @Override
    public String toString() {
        return "LWSelection[" + paramString() + "]";
    }
    
    
}
