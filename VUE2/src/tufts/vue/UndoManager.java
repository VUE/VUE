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
import static tufts.Util.*;

import java.util.*;
import java.awt.Point;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.Action;

/**
 * Records all changes that take place in a LWMap (as seen from
 * LWCEvent delivery off the LWMap) and provides for arbitrarily
 * marking named points of rollback.
 *
 * For robustness, if the application fails to mark any changes,
 * they'll either have been rolled into another undo action, or
 * stuffed into an un-named Undo action if they attempt an undo while
 * there are unmarked changes.
 *
 * @version $Revision: $ / $Date: $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class UndoManager
    implements LWComponent.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(UndoManager.class);
    
    private boolean mUndoUnderway = false;
    private boolean mRedoUnderway = false;
    private boolean mCleanupUnderway = false;

    /** The list of undo actions (named groups of property changes) */
    private UndoActionList UndoList = new UndoActionList("Undo"); 
    /** The list of redo actions (named groups of property changes generated from Undo's) */
    private UndoActionList RedoList = new UndoActionList("Redo"); 
    
    /** The map who's modifications we're tracking. */
    protected LWMap mMap; 
    
    /** All recorded changes since last mark, mapped by component (for detecting & ignoring repeats) */
    private Map mComponentChanges = new HashMap();
    /** All recorded changes since last mark, marked for sequential processing */
    //private List mUndoSequence = new ArrayList(); 
    /** The last LWCEvent we didn't ignore since last mark -- used for guessing at good Undo action title names */
    private LWCEvent mLastEvent;
    
    ///** The total number of recorded or compressed changes since last mark (will be >= mUndoSequence.size()) */
    //private int mChangeCount;

    private int mEventsSeenSinceLastMark;

    /** The current collector of changes, to be permanently recorded and named when a user "mark" is established by a GUI */
    private UndoAction mCurrentUndo;

    /** map of threads currently attched to a particular undo mark */
    private Map mThreadsWithMark = Collections.synchronizedMap(new HashMap());

    ///** map of threads currently attched to no undo manager (events to discard) */
    //private static Map ThreadsToIgnore = Collections.synchronizedMap(new HashMap());
    

    public UndoManager(LWMap map)
    {
        mMap = map;
        mCurrentUndo = new UndoAction();
        map.addLWCListener(this);
        //VUE.addActiveListener(LWMap.class, this);
        updateGlobalActionLabels(); // make sure actions disabled at start
    }

    
    /**
     * This aggregates a sequence of property changes under a single
     * named user action.
     *
     * A named sequence of triples: LWComponent, property key, and old value.
     * LWCEvents are undone in the reverse order that they happened: the changes
     * are peeled back.
     */
    private static class UndoAction {
        private String name = null;
        private List undoSequence; // list of UndoItem's -- will be sorted by sequence index before first use
        /** The total number of recorded or compressed changes that happened on our watch (will be >= undoSequence.size()) */
        private int eventCount = 0;
        private boolean sorted = false;
        private List<Thread> attachedThreads;

        UndoAction() {
            undoSequence = new ArrayList();
        }
        /*
        UndoAction(String name, List undoSequence) {
            this.name = name;
            this.undoSequence = undoSequence;
        }
        */

        synchronized void addAttachedThread(Thread t) {
            if (attachedThreads == null)
                attachedThreads = new ArrayList();
            attachedThreads.add(t);
        }

        int changeCount() {
            return undoSequence.size();
        }

        int size() { return undoSequence.size(); }

        void mark(String name) {
            this.name = name;
            if (DEBUG.UNDO) {
                Log.debug(this + " MARKED with [" + name + "]");
                //tufts.Util.printStackTrace(this + " MARKED with [" + name + "]");
            }
                    
        }

        boolean isIncomplete() {
            return size() > 0 && name != null;
        }
        boolean isMarked() {
            return name != null;
        }
        
//         synchronized void undo() {
//             try {
//                 sUndoUnderway = true;
//                 run_undo();
//             } finally {
//                 sUndoUnderway = false;
//             }
//         }

        // todo: if there are any UndoableThread's attached to us, they should
        // ideally be interrupted.
        //private synchronized void run_undo() {
        synchronized void undoAggregateUserAction() {
            if (DEBUG.UNDO) Log.debug(this + " undoing sequence of size " + changeCount());

            if (attachedThreads != null) {
                // First: interrupt any running threads that may yet deliver events
                // to this UndoAction.
                if (DEBUG.Enabled) Log.debug(this + " making sure " + attachedThreads.size() + " attached threads are stopped");
                for (Thread t : attachedThreads) {
                    if (t.isAlive()) {
                        if (DEBUG.Enabled) Log.debug(this + " INTERRUPTING " + t);
                        t.interrupt();
                    }
                }
                // clear all attached threads: only need to interrupt the first time.
                attachedThreads.clear();
                attachedThreads = null;
            }

            if (!sorted) {
                Collections.sort(undoSequence);
                sorted = true;
                if (DEBUG.UNDO){
                    System.out.println("=======================================================");
                    VueUtil.dumpCollection(undoSequence);
                    System.out.println("-------------------------------------------------------");
                }
            }

            //boolean hierarchyChanged = false; // now handled in fireUserActionCompleted
            
            //-------------------------------------------------------
            // First, process all hierarchy events
            //-------------------------------------------------------
            
            ListIterator<UndoItem> i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = i.previous();
                if (undoItem.propKey == LWKey.HierarchyChanging) {
                    undoItem.undo();
                    //hierarchyChanged = true;
                }
            }

            //-------------------------------------------------------
            // Second, process all property change events
            //-------------------------------------------------------
            
            i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = i.previous();
                if (undoItem.propKey != LWKey.HierarchyChanging)
                    undoItem.undo();
            }
            
//             if (hierarchyChanged)
//                 VUE.getSelection().clearDeleted();
        }

        String getName() {
            return name;
        }
        
        /**
         *  massage the name of the property to produce a more human
         *  presentable name for the undo action.
         */
        String getDisplayName() {
            if (DEBUG.UNDO) return this.name + " {" + changeCount() + "}";
            String display = null;
            try {
                display = produceDisplayName();
            } catch (Throwable t) {
                Log.error(Util.tags(this.name), t);
            }
            return display == null ? this.name : display;
        }
        
        private String produceDisplayName() {

            if (this.name == LWKey.HierarchyChanging)
                return "(Hierarchy Change)"; // shouldn't see this
            
            String display = "";
            String uName = this.name;
            
            if (uName.startsWith("hier."))
                uName = uName.substring(5);
            // Replace all '.' with ' ' and capitalize first letter of each word
            uName = uName.replace('-', '.');
            String[] words = uName.split("\\.");
            for (int i = 0; i < words.length; i++) {
                final String word = words[i];
                if (word.length() == 0)
                    return null; // if seen multiple dots in a row, presume pre-formatted
                if (Character.isLowerCase(word.charAt(0)))
                    words[i] = Character.toUpperCase(word.charAt(0)) + word.substring(1);
                if (i > 0)
                    display += " ";
                display += words[i];
            }
            return display;
        }
    
        public String toString() {
            int s = size();
            return "UndoAction@" + Integer.toHexString(hashCode()) + "["
                + (name==null?"":name)
                + (s<10?" ":"") + s + " changes"
                + " from " + eventCount + " events"
                + "]";
        }
    }

    /**
     * A single property change on a single component.
     */
    private static class UndoItem implements Comparable
    {
        LWComponent component;
        Object propKey;
        Object oldValue;
        int order; // for sorting; highest values are most recent changes

        UndoItem(LWComponent c, Object propertyKey, Object oldValue, int order) {
            this.component = c;
            this.propKey = propertyKey;
            this.oldValue = oldValue;
            this.order = order;
        }

        void undo() {
            if (DEBUG.UNDO) Log.debug("UNDOING: " + this);
            if (propKey == LWKey.HierarchyChanging) {
                undoHierarchyChange((LWContainer) component, oldValue);
            } else if (oldValue instanceof Undoable) {
                ((Undoable)oldValue).undo();
            } else {
                if (false && DEBUG.Enabled && DEBUG.META) {
                    // ANIMATED UNDO CODE:
                    try {
                        Object curValue = component.getPropertyValue(propKey);
                        if (curValue != null)
                            undoAnimated();
                    } catch (Exception e) {
                        System.err.println("Exception during animated undo of [" + propKey + "] on " + component);
                        if (oldValue != null)
                            System.err.println("\toldValue is " + oldValue.getClass() + " " + oldValue);
                        e.printStackTrace();
                    }
                }

                if (component.isOrphan()) {
                    
                    // For the hairy event's that LWGroups produce when created inside groups.
                    // we'd be getting a zombie event complaint if we did this.  Turns out if we
                    // skip the property value completely, this is actually something we want to do
                    // to help group inside group undo/redo (zombie complaint shows up on undo, and
                    // then on redo, a location event goes thru that we actually don't want -- this
                    // was the conversion to local coordinates.
                    
                    if (DEBUG.Enabled) Log.debug("SKIPPING undo item for deleted (parentless) component: " + component + "; " + this);
                } else 
                    component.setProperty(propKey, oldValue);
                
            }
        }

        private void undoAnimated() {
            // redo not working if we suspend events here...
            // please don't tell me redo was happening by capturing
            // the zillions of animated events...
            
            // Also going to be tricky: animating through changes
            // in a bunch of nodes at the same time -- right now
            // a group drag animates each one back into place
            // one at a time in sequence...

            // Also: SEE COMMENT in LWLink.getPropertyValue
                    
            // experimental for animated presentation
            //component.getChangeSupport().setEventsSuspended();
            
            if (oldValue instanceof Point)
                animatedChange((Point)oldValue);
            else if (oldValue instanceof Point2D)
                animatedChange((Point2D)oldValue);
            else if (oldValue instanceof Color)
                animatedChange((Color)oldValue);
            else if (oldValue instanceof Size)
                animatedChange((Size)oldValue);
            else if (oldValue instanceof Integer)
                animatedChange((Integer)oldValue);
            else if (oldValue instanceof Float)
                animatedChange((Float)oldValue);
            else if (oldValue instanceof Double)
                animatedChange((Double)oldValue);
            //component.getChangeSupport().setEventsResumed();
        }

        private static final int segments = 5;
        
        private static void repaint() {
            //VUE.getActiveMap().notify("repaint");
            VUE.getActiveViewer().paintImmediately();
            //try { Thread.sleep(100); } catch (Exception e) {}
        }

        private void animatedChange(Size endValue) {
            Size curValue = (Size) component.getPropertyValue(propKey);
            final float winc = (endValue.width - curValue.width) / segments;
            final float hinc = (endValue.height - curValue.height) / segments;
            Size value = new Size(curValue);
            
            for (int i = 0; i < segments; i++) {
                value.width += winc;
                value.height += hinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Float endValue) {
            Float curValue = (Float) component.getPropertyValue(propKey);
            final float inc = (endValue.floatValue() - curValue.floatValue()) / segments;
            Float value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Float(curValue.intValue() + inc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Double endValue) {
            Double curValue = (Double) component.getPropertyValue(propKey);
            final double inc = (endValue.doubleValue() - curValue.doubleValue()) / segments;
            Double value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Double(curValue.intValue() + inc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Integer endValue) {
            Integer curValue = (Integer) component.getPropertyValue(propKey);
            final float inc = (endValue.intValue() - curValue.intValue()) / segments;
            Integer value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Integer((int) (curValue.intValue() + inc * i));
                component.setProperty(propKey, value);
                repaint();
            }
        }
        
        private void animatedChange(Color endValue) {
            Color curValue = (Color) component.getPropertyValue(propKey);
            final int rinc = (endValue.getRed() - curValue.getRed()) / segments;
            final int ginc = (endValue.getGreen() - curValue.getGreen()) / segments;
            final int binc = (endValue.getBlue() - curValue.getBlue()) / segments;
            Color value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Color(curValue.getRed() + rinc * i,
                                  curValue.getGreen() + ginc * i,
                                  curValue.getBlue() + binc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }

        private void animatedChange(Point2D endValue) {
            Point2D curValue = (Point2D) component.getPropertyValue(propKey);
            final double xinc = (endValue.getX() - curValue.getX()) / segments;
            final double yinc = (endValue.getY() - curValue.getY()) / segments;
            Point2D.Double value = new Point2D.Double(curValue.getX(), curValue.getY());
            
            for (int i = 0; i < segments; i++) {
                value.x += xinc;
                value.y += yinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
        
        private void animatedChange(Point endValue) {
            Point curValue = (Point) component.getPropertyValue(propKey);
            final double xinc = (endValue.getX() - curValue.getX()) / segments;
            final double yinc = (endValue.getY() - curValue.getY()) / segments;
            Point value = new Point(curValue);
            
            for (int i = 0; i < segments; i++) {
                value.x += xinc;
                value.y += yinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
            
        // TODO: detect condition that indicates we've got an event/undo event generation problem
        // on the front end: let us know if ever the same component get's multiple DIFFERENT
        // reparentings during a single undo: e.g., it appears in more than one hier.changing event
        // for different components, so whichever one comes last is the one it ends up being parented to...

        
        private static void undoHierarchyChange(final LWContainer parent, final Object oldValue)
        {
            if (DEBUG.UNDO) System.out.println("\trestoring children of " + parent + " to " + oldValue);

            parent.notify(LWKey.HierarchyChanging); // this event important for REDO

            // Create data for synthesized ChildrenAdded & ChildrenRemoved events
            // For our purposes here, new/added are the the old values we're restoring,
            // and old/removed are the current values we're replacing.
            final List newChildList = (List) oldValue;
            final List oldChildList = parent.mChildren;
            final List childrenAdded;
            final List childrenRemoved;
                
            if (newChildList == LWComponent.NO_CHILDREN) {
                childrenAdded = Collections.EMPTY_LIST;
            } else {
                childrenAdded = new ArrayList(newChildList);
                if (oldChildList != LWComponent.NO_CHILDREN)
                    childrenAdded.removeAll(oldChildList);
            }

            if (oldChildList == LWComponent.NO_CHILDREN) {
                childrenRemoved = Collections.EMPTY_LIST;
            } else {
                childrenRemoved = new ArrayList(oldChildList);
                if (newChildList != LWComponent.NO_CHILDREN)
                    childrenRemoved.removeAll(newChildList);
            }

            //-------------------------------------------------------
            // Do the swap in of the old list of children:
            //-------------------------------------------------------
            
            parent.mChildren = (List) oldValue;

            //-------------------------------------------------------
            // Now make sure all the children are properly parented,
            // and none of them are marked as deleted.
            //-------------------------------------------------------

            // TODO: this apparently never handled the REDO DELETE case, which would
            // ensure objects were once again removed from the model.  The problem is
            // that just because a node is in the childrenRemoved list, it doesn't mean
            // it's being deleted -- it may just be in the process of being reparented
            // elsewhere.  This means that REDO of deletes are leaving orphaned objects
            // out there with a non-null parent reference, and a missing DELETED bit,
            // which also means they're not getting cleared from the selection on a REDO
            // DELETE.

            // This could be handled via tracking LWKey.Deleted events, but that would
            // be a ton of extra events to record for large deletes.  The best way would
            // be to add a ChildrenDeleted event, issued in place of or in addition to
            // ChildrenRemoved, which could easily be done in LWContainer.removeChildren
            // when context == REMOVE_DELETE.

            // When we get around to fixing this, may want to see if we can do away with
            // the ChildrenAdded/ChildrenRemoved events, which are somewhat redundant
            // with the HierarchyChanged event, and rarely listened for (tho LWContainer
            // currently does NOT issue HierarchyChanged on adds/removes -- just
            // ChildrenAdded/ChildrenRemoved!).  The only place we currently listen
            // for ChildrenAdded/ChildrenRemoved that couldn't immediately be replaced
            // by HierarchyChanged is in the OutlineViewHierarchyModel impl.
            
            if (parent.mChildren != LWComponent.NO_CHILDREN) {

                if (parent instanceof LWPathway) {
                    
                    // Special case for pathways. todo: something cleaner (pathways don't "own" their children)
                    Log.error("LWPathway's don't have real children: " + parent + "; for children " + parent.mChildren);
                    
                } else {

                    for (LWComponent child : parent.mChildren) {
                        if (child.isDeleted())
                            child.restoreToModel(); // todo: take parent as argument, skip setParent
                        child.setParent(parent);
                    }
                    
//                 for (LWComponent child : parent.mChildren) {
//                     if (parent instanceof LWPathway) {
//                         // Special case for pathways. todo: something cleaner (pathways don't "own" their children)
//                         //((LWPathway)parent).addChildRefs(child);
//                         Util.printStackTrace("LWPathway's don't have real children: " + parent + "; for child " + child);
//                     } else {
//                         if (child.isDeleted())
//                             child.restoreToModel(); // todo: take parent as argument, skip setParent
//                         child.setParent(parent);
//                         //child.reparentNotify(parent);
//                     }
//                 }
                    
                }
            }

            parent.layout();
            // issue synthesized ChildrenAddded and/or ChildrenRemoved events
            if (childrenAdded.size() > 0) {
                if (DEBUG.UNDO) Log.debug("Synthetic event " + LWKey.ChildrenAdded + " " + childrenAdded);
                parent.notify(LWKey.ChildrenAdded, childrenAdded);
            }
            if (childrenRemoved.size() > 0) {
                if (DEBUG.UNDO) Log.debug("Synthetic event " + LWKey.ChildrenRemoved + " " + childrenRemoved);
                parent.notify(LWKey.ChildrenRemoved, childrenRemoved);
            }
            // issue the general hierarchy change event
            parent.notify(LWKey.HierarchyChanged);
        }

        public int compareTo(Object o) {
            return order - ((UndoItem)o).order;
        }
        
        public String toString() {
            Object old = oldValue;
            if (oldValue instanceof Collection) {
                Collection c = (Collection) oldValue;
                if (c.size() > 1)
                    old = c.getClass().getName() + "{" + c.size() + "}";
            }
            return "UndoItem["
                + order + (order<10?" ":"")
                + " " + TERM_CYAN + propKey + TERM_CLEAR
                + " " + component
                + " old=" + old
                + "]";
        }

    }

    private static class UndoActionList extends ArrayList
    {
        private String name;
        private int current = -1;

        UndoActionList(String name) {
            this.name = name;
        }
        
        public boolean add(Object o) {
            // when adding, flush anything after the top
            if (current < size() - 1) {
                int s = current + 1;
                int e = size();
                if (DEBUG.UNDO) out("flushing " + s + " to " + e + " in " + this);
                removeRange(s, e);
            }
            if (DEBUG.UNDO) out("adding: " + o);
            super.add(o);
            current = size() - 1;
            return true;
        }
        UndoAction pop() {
            if (current < 0)
                return null;
            return (UndoAction) get(current--);
        }
        UndoAction peek() {
            if (current < 0)
                return null;
            return (UndoAction) get(current);
        }

        void advance() {
            current++;
            if (current >= size())
                throw new IllegalStateException(this + " top >= size()");
        }

        public void clear() {
            super.clear();
            current = -1;
        }

        int top() {
            return current;
        }

        private void out(String s) {
            System.out.println("\tUAL[" + name + "] " + s);
        }
        
        public String toString() {
            return "UndoActionList[" + name + " top=" + top() + " size=" + size() + "]";
        }
        
        public void add(int index, Object element) { throw new UnsupportedOperationException(); }
        public Object remove(int index) { throw new UnsupportedOperationException(); }
    }

//     public void activeChanged(ActiveEvent<LWMap> e)
//     {
//         // We really don't need every undo manager listening for
//         // active map changes -- a single listener for the active map
//         // that then tells the active map's undo manager, if it has
//         // one, to update the menu labels in the global VUE would
//         // suffice, tho this works just fine with minimal overhead.
//         if (e.active == mMap)
//             updateGlobalActionLabels();
//     }

    public void updateGlobalActionLabels() {
        setActionLabel(Actions.Undo, UndoList);
        setActionLabel(Actions.Redo, RedoList);
    }

    /* If we are asked to do an undo (or redo), and find modifications
     * on the undo list that have not been collected into a user mark,
     * this is a problem -- all changes should be collected into a
     * user umark (otherwise, for instance, creating a new node would
     * show up as separate undo actions for every property that was
     * set on the node during it's contstruction).  If we find
     * unmarked modifications, we report it to the console, and
     * create a synthetic mark for all the unmarked changes, and
     * name the last property on the list (which we could make
     * look "normal" if there was only one unmarked change, but
     * we want to know if this is happening.)
     */
    private boolean checkAndHandleUnmarkedChanges() {
        if (mCurrentUndo.isIncomplete()) {
            new Throwable(this + " UNMARKED CHANGES! " + mComponentChanges).printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();
            boolean olddb = DEBUG.UNDO;
            DEBUG.UNDO = true;
            markChangesAsUndo("Unnamed Actions [last=" + mLastEvent.getName() + "]"); // collect whatever's there
            DEBUG.UNDO = olddb;
            return true;
        }
        return false;
    }

    private boolean mRedoCaptured = false; // debug
    public synchronized void redo()
    {
        checkAndHandleUnmarkedChanges();
        mRedoCaptured = false;
        UndoAction redoAction = RedoList.pop();
        if (DEBUG.UNDO) System.out.println(this + " redoing " + redoAction);
        if (redoAction != null) {
            try {
                mRedoUnderway = true;
                mUndoUnderway = true;
                redoAction.undoAggregateUserAction();
                //runCleanupTaskPhase(false);
            } finally {
                mUndoUnderway = false;
                mRedoUnderway = false;
            }
            UndoList.advance();
            fireUserActionCompleted();
        }
        updateGlobalActionLabels();
    }

    public synchronized void undo()
    {
        checkAndHandleUnmarkedChanges();
        
        UndoAction undoAction = UndoList.pop();
        if (DEBUG.UNDO) System.out.println("\n" + this + " undoing " + undoAction);
        if (undoAction != null) {
            mRedoCaptured = false;
            try {
                mUndoUnderway = true;
                undoAction.undoAggregateUserAction();
                //runCleanupTaskPhase(false);
            } finally {
                mUndoUnderway = false;
            }
            RedoList.add(collectChangesAsUndoAction(undoAction.name));
            fireUserActionCompleted();
        }
        updateGlobalActionLabels();
        // We've undo everything: we can mark the map as having no modifications
        if (UndoList.peek() == null)
            mMap.markAsSaved();
    }

    private void setActionLabel(Action a, UndoActionList undoList) {
        String label = undoList.name;
        if (DEBUG.UNDO) label += "#" + undoList.top() + "["+undoList.size()+"]";
        if (undoList.top() >= 0) {
            label += " " + undoList.peek().getDisplayName();
            if (DEBUG.UNDO) System.out.println(this + " now available: '" + label + "'");
            a.setEnabled(true);
        } else
            a.setEnabled(false);
        a.putValue(Action.NAME, label);
    }

    /** figure the name of the undo action from the last LWCEvent we stored
     * an old property value for */
    public void mark() {
        markChangesAsUndo(null);
    }

    /**
     * If only one property changed, use the name of that property,
     * otherwise use the @param aggregateName for the group of property
     * changes that took place.
     */
    
    public void mark(String aggregateName) {
        
        String name = aggregateName;
        
        if (name == null && mLastEvent != null) // going to need to put last event into UndoAction..
            name = mLastEvent.getName();
        
        markChangesAsUndo(name);
        
//         String name = null;
//         if (mCurrentUndo.size() == 1 && mLastEvent != null) // going to need to put last event into UndoAction..
//             name = mLastEvent.getName();
//         else
//             name = aggregateName;
//         markChangesAsUndo(name);
    }

    /**
     * We use LinkedHashMap's to maintain insertion order.
     * This class just adds a type name (for the kind of task) to the list.
     */
    private static class TaskMap extends java.util.LinkedHashMap<Object,Runnable> {
        final String type;

        public TaskMap(String name) {
            type = name;
        }

        public TaskMap clone() {
            return (TaskMap) super.clone();
        }

        public String toString() {
            return "TaskMap[" + type + " n=" + size() + "]";
        }
        
    }

    // Keeping two list is a quick and easy way to support two levels of priority.
    // If we end up needing more, implement a real priority system.
    
    /**  main/default cleanup tasks */
    private final TaskMap mCleanupTasks = new TaskMap("Cleanup");
    /** low/last priority tasks */
    private final TaskMap mLastTasks = new TaskMap("Last"); 
    
    /**
     * @see addCleanupTask(Object taskKey, Runnable task)
     * This defaults the taskKey to the task object itself.
     **/
    public void addCleanupTask(Runnable task) {
        addCleanupTask(task, task);
    }

    // Note: This impl doesn't currently allow more than one cleanup task per LWComponent without
    // creating a new non-static inner class Runnable to differentiate between the type of task to
    // be run (E.g., LWGroup.DisperseOrNormalize).  This isn't much a problem, but if ever we get
    // to a point where we've got LOTS of cleanup tasks, we may want to extend this to allow a key
    // that is based on the LWComponent.hashCode() + <some-string>, so we don't have to create all
    // those Runnable implementing class instances.  Se we'd use something other than Runnable,
    // (e.g., Taskable), that takes a key argument so that a single calls on the the LWComponent
    // (e.g., runTask("TaskA"), runTask("TaskB")), would allow it to switch out based on the given
    // task key to run the different cleanup tasks needed by that LWComponent (the LWComponent
    // itself would always become the primary key, and a string argument would be come the
    // secondary key).
    
    /** @return true if there are already any cleanup tasks with the given key */
    public boolean hasCleanupTask(Object taskKey) {
        return mCleanupTasks.containsKey(taskKey);
    }
    
    /** @return true if there are already any cleanup tasks with the given key */
    public boolean hasLastTask(Object taskKey) {
        return mLastTasks.containsKey(taskKey);
    }
    
    public boolean hasCleanupTasks() {
        return mCleanupTasks.size() > 0 || mLastTasks.size() > 0;
    }
    

    /**

     * Add a task to be run just before the next mark.  If code somewhere has decided it
     * needs to check that state at the end of all current user operations (which is
     * when we create undo-marks, explicitly throughout the code at the end of known
     * user action control points), it can add a task, which will run (possibly
     * generating more events and adding to the undo queue) just before the current undo
     * event queue is collected into an undo action as the mark is established.

     * E.g., LWGroup's add a task any time children are removed, so it can run at the
     * end to find out if it should auto-disperse itself (if it has only 0 or 1 members,
     * and hasn't already been deleted).  This is because many different operations may
     * remove children from a group, and we don't want to track them all and don't care
     * how they operate -- we just want to know what state we're left in when the dust
     * settles.

     * Tasks are NOT RUN during Undo/Redo -- they are "one-way" operations intended to
     * maintain model integrity / transactional integrity.  Once they've run and made
     * changes to the model, the events generated by these changes are sufficient for
     * what needs to happen during undo/redo -- the task is no longer needed.
     
     * For maximum reliability, the same task should be able to run multiple times with
     * no ill effect, as the UndoManager may run the same task more than once if new
     * tasks come in while cleanup is being run.  E.g., to use contrived examples, a
     * task that did nothing ever than increment a counter, or say blindly add 10 to the
     * x-value of a component, it not really a cleanup task, tho a task that ensured the
     * width of an object was always twice it's height is just fine: it's enforcing
     * a constraint (as long as there are no other tasks that also made the height
     * depend on the width in any way, as we could wind up with competing tasks,
     * neither of which will allow the model the ultimately resolve to a stable state).

     * @param taskKey - a key that can be used to check to see 
     * if something with the same key is already waiting to be
     * run at the next mark.

     * @param task -- a Runnable
     
     */
    public void addCleanupTask(Object taskKey, Runnable task) {
        addTask(mCleanupTasks, taskKey, task);
    }

    public void addLastTask(Object taskKey, Runnable task) {
        //if (DEBUG.WORK || DEBUG.UNDO) System.out.println(TERM_RED + "addLastTask " + taskKey + " " + task + TERM_CLEAR);
        addTask(mLastTasks, taskKey, task);
    }

    private void addTask(TaskMap taskMap, Object taskKey, Runnable task) {
        //if (mUndoUnderway && !(task instanceof LWLink.Recompute)) { // TODO: TEMP HACK TEMP HACK
        if (mUndoUnderway) {
            Util.printStackTrace(this + "; ignoring task during undo/redo in "
                                 + "\n\ttaskKey: " + taskKey
                                 + "\n\t   task: " + task);
            return;
        }

        synchronized (taskMap) {
            if (DEBUG.WORK || DEBUG.UNDO) {
                System.out.println(TERM_RED
                                   + "ADDING "
                                   + (mCleanupUnderway?"CASCADE ":"")
                                   + taskMap.type + " TASK: " + task
                                   + TERM_CLEAR
                                   + (taskKey == task ? "" : (" key=" + taskKey))
                                   );
            }
                
            final Runnable prior = taskMap.put(taskKey, task);
                
            if (prior != null)
                Util.printStackTrace("over-wrote existing cleanup task (won't be run): " + prior + " taskKey: " + taskKey);
        }
    }
    
    private void runCleanupTaskPhase(boolean debug) {
        synchronized (mCleanupTasks) {
            synchronized (mLastTasks) {
                if (mCleanupUnderway) {
                    Util.printStackTrace("serious problem: cleanup already underway!");
                    return;
                } 
                try {
                    mCleanupUnderway = true;
                    runPrioritizedCleanupTasks(debug);
                } finally {
                    mCleanupUnderway = false;
                }
            }
        }
    }

    private static final int MaxRecurse = 10;
    
    // should be run inside a synchronized block against mCleanupTasks & mLastTasks
    private void runPrioritizedCleanupTasks(boolean debug) {
        if (mCurrentUndo.size() == 0) {
//             if (DEBUG.Enabled)
//                 Util.printStackTrace("Running cleanup tasks with an empty undo queue: " + this);
//             else
                Log.info("Running cleanup tasks with an empty undo queue: " + this);
            
            debug = true;
        } else if (!debug)
            debug = DEBUG.WORK || DEBUG.UNDO;
        
        int recurseCount = 0; // first one doesn't count
        do {
            if (debug && recurseCount > 0)
                System.out.println(TERM_RED + "CLEANUP TASKS: extra model cleanup pass #" + recurseCount + TERM_CLEAR);
            if (mCleanupTasks.size() > 0)
                runCleanupTasks(debug, mCleanupTasks);
            if (mLastTasks.size() > 0)
                runCleanupTasks(debug, mLastTasks);
        } while (++recurseCount < MaxRecurse && (mCleanupTasks.size() > 0 || mLastTasks.size() > 0));

        if (recurseCount > 1) {
            if (recurseCount >= MaxRecurse)
                Util.printStackTrace(this + " cleanup task recursion count exceeded max at " + recurseCount);
            else
                Log.info("note: UndoManager cleanup task recusion count reached " + recurseCount + " extra passes before model settled down.");
        }
        
        // When we clear this out has very complex semantics.  Theoretically
        // each list should be cleaned out as run, tho practically, doing 
        // mCleanupTasks now as opposed to before running last tasks prevents
        // the queueing of new tasks during the last task that already
        // ran as cleanup tasks.  This may not be what we want it the end,
        // tho it suits us for now.
        
        //mCleanupTasks.clear();
        //mLastTasks.clear();
    }

    // should be run inside a synchronized block against mCleanupTasks & mLastTasks
    private void runCleanupTasks(boolean debug, TaskMap taskMap)
    {
        if (debug)
            coutln(TERM_CYAN, "\nHANDLING " + taskMap + " TASKS in " + Thread.currentThread());

        //final Set<Map.Entry<Object,Runnable>> entrySet = taskMap.clone().entrySet();
        //final Iterator<Map.Entry<Object,Runnable>> iterator = entrySet.iterator();
        //out("ITERABLE: " + Util.tag(entrySet));
        //out("ITERATOR: " + Util.tag(iterator));
        //while (iterator.hasNext()) {

        // todo performance: we clone the task list here in case any more tasks
        // come in while these tasks are running.  This is a bit of a blunt
        // instrument to handle the concurrent modification problem, but it's
        // very reliable.  If our tasks maps get large enough, we may want another
        // solution.
        
        int count = 0;
        for (Map.Entry<Object,Runnable> e : taskMap.clone().entrySet()) {
            count++;
            //final Map.Entry<Object,Runnable> e = iterator.next();
            final Runnable task = e.getValue();
            final Object key = e.getKey();
            if (debug) {
                coutln(TERM_CYAN,
                       "RUNNING " + taskMap.type + " TASK #" + count + ": " + task
                       + TERM_CLEAR
                       + (task == key ? "" : " key: " + key));
            }
            task.run();
            
            // now remove the task from live taskMap, which is may be adding new tasks
            // (we iterate through a clone of the list
            taskMap.remove(key);
        }

        if (debug) {
            if (taskMap.size() > 0) coutln(TERM_RED, "CASCADED TASKS: " + taskMap);
            coutln(TERM_CYAN, "COMPLETED " + count + " " + taskMap.type + " TASKS.");
        }
        
    }

    
    /** @return true if we're undoing or redoing */
    public boolean isUndoing() {
        return mUndoUnderway;
    }
    
    public synchronized void markChangesAsUndo(String name)
    {
        synchronized (mCleanupTasks) {
            synchronized (mLastTasks) {

                // Can we skip running the cleanup tasks if there's nothing in the undo
                // queue?  Do we NEED to do that, in case of multiple "just in case" marks,
                // where only the last one actually had anything, and we DON'T want to run
                // the cleanup tasks till then?
                
                if (mCleanupTasks.size() > 0 || mLastTasks.size() > 0)
                    runCleanupTaskPhase(false);
            }
        }
        
        if (mCurrentUndo.size() == 0) // if nothing changed, don't bother adding an UndoAction
            return;
        if (name == null) {
            if (mLastEvent == null)
                return;
            name = mLastEvent.getName();
        }
        
        UndoList.add(collectChangesAsUndoAction(name));
        RedoList.clear();
        fireUserActionCompleted();
        updateGlobalActionLabels();
    }

    private synchronized UndoAction collectChangesAsUndoAction(String name)
    {
        if (DEBUG.UNDO) out("collectChangesAsUndoAction " + name);

        final UndoAction markedUndo = mCurrentUndo;
        markedUndo.mark(name);
        resetMark();
        return markedUndo;
    }

    public synchronized void resetMark() {
        //mUndoSequence = new ArrayList();
        mCurrentUndo = new UndoAction();
        mComponentChanges.clear();
        mLastEvent = null;
        mEventsSeenSinceLastMark = 0;
        //mChangeCount = 0;
    }

    
    void flush() {
        UndoList.clear();
        RedoList.clear();
        mComponentChanges.clear();
        if (VUE.getActiveMap() == mMap)
            updateGlobalActionLabels();
    }

    
    /**
     * Store a key in the given UndoableThread that tells the UndoManager what UndoAction
     * is affected by LWCEvents coming from that thread.  This must be called BEFORE any
     * events in the given thread have been marked.  To ensure this, make sure this is
     * called before the thread has been started.
     */
    
    void attachThreadToNextMark(UndoableThread t) {
        if (t.isAlive())
            throw new Error(t + ": not safe to attach an UndoAction to an already started thread");
        t.setMarker(mCurrentUndo);
    }

    private static class UndoMark {
        final UndoManager manager;
        final UndoAction action;
        UndoMark(UndoManager m) {
            manager = m;
            action = manager.mCurrentUndo;
        }
        public String toString() {
            String s = "UndoMark[";
            if (action != manager.mCurrentUndo) 
                s += action + " / ";
            return s + manager + "]";
        }
    }
    
    /**
     * @return a key that marks the current location in the undo queue,
     * that can be used to attach a subsequent thread to.  That is,
     * by taking the returned key and later calling attachThreadToMark
     * in another thread, all further events received by the UndoManager from
     * that thread will be attched in the undo queue at the location
     * of the given mark.  This may return null, which means there
     * is no current UndoManager listening for events.
     */
    public static Object getKeyForNextMark(LWComponent c) {
        LWMap map = c.getMap();
        if (map == null)
            return null;
            //throw new Error("Component not yet in map: can't search for undo manager " + c);
        UndoManager undoManager = map.getUndoManager();
        if (undoManager == null)
            return null;
        else
            return undoManager.getKeyForNextMark();
        //return currentManager.getStringKeyForNextMark();
    }

    public Object getKeyForNextMark() {
        UndoMark mark = new UndoMark(this);
        if (DEBUG.UNDO || DEBUG.THREAD) out("GENERATED MARK " + mark);
        return mark;
    }

    
    /**
     * Attach the current thread to the location in the undo queue marked by the given key.
     *
     * @param undoActionKey key obtained from getKeyForNextMark, which may be null,
     * in which case this method does nothing.
     */

    static void attachCurrentThreadToMark(Object undoActionKey) {
        if (undoActionKey != null) {
            Thread thread = Thread.currentThread();
            
            //if (!thread.getName().startsWith("Image Fetcher"))
            //new Throwable("Warning: attaching mark to non-Image Fetch thread: " + thread).printStackTrace();

            // extract the mark, because it contains the manager we need to insert the thread:mark mapping
            UndoMark mark = (UndoMark) undoActionKey;
            // store the mark in the appropriate UndoManager, and notify of error if thread was already marked
            if (mark.manager.mThreadsWithMark.containsKey(thread)) {
                if (DEBUG.Enabled) Log.debug(thread + " already tied mark " + mark + " grouping as one undo for now");
                // this seems to actually be "working" as we get two undoables... ?
            } else {
                mark.manager.mThreadsWithMark.put(thread, mark);
                mark.action.addAttachedThread(thread);
                if (DEBUG.UNDO || DEBUG.THREAD) Log.debug("ATTACHED " + mark + " to " + thread);
            }
                
            /*
            UndoMark existingMark = (UndoMark) mark.manager.mThreadsWithMark.put(thread, mark);
            if (existingMark != null)
                new Throwable("Error: " + thread
                              + " was tied to mark " + existingMark
                              + ", superceeded by " + mark).printStackTrace();
            */
            
        }
        else if (DEBUG.UNDO||DEBUG.THREAD) System.out.println("null UndoMark");
    }
    
    static void detachCurrentThread(Object undoActionKey) {
        if (undoActionKey != null) {
            if (DEBUG.THREAD) Util.printStackTrace("detachCurrentThread: " + Thread.currentThread());
            
            UndoMark mark = (UndoMark) undoActionKey;
            mark.manager.mThreadsWithMark.remove(Thread.currentThread());

            // Tell everyone listing it's time to repaint.
            // todo: only need to do this if a property actually changed during this thread

            mark.manager.mMap.notify(mark.manager, LWKey.RepaintAsync);
            
            // todo: messy to require two events here..
            // This event will tell the ACTIVE viewer to repaint:
            //mark.manager.mMap.notify(mark.manager, LWKey.Repaint);
            // This event is for all NON-ACTIVE viewers, or Panners, other listeners, etc:
            //mark.manager.mMap.notify(mark.manager, LWKey.UserActionCompleted);
        }
        /*
        final Thread thread = Thread.currentThread();
        final String tn = thread.getName();
        if (tn.startsWith(UNDO_ACTION_TAG)) {
            thread.setName(tn.substring(tn.indexOf(')') + 2));
            System.out.println("Released thread " + thread);
        }
        */
    }

    /*
    private Map taggedUndoActions = new HashMap();
    private static final String UNDO_ACTION_TAG = "+VUA@(";
    static void attachCurrentThreadToStringMark(String undoActionKey) {
        if (undoActionKey != null) {
            Thread t = Thread.currentThread();
            if (!t.getName().startsWith("Image Fetcher")) {
                new Throwable("Warning: attaching mark to non-Image Fetch thread: " + t).printStackTrace();
            }
            // todo: cleaner if we kept a map of Thread:UndoAction's, but a tad slower
            String newName = undoActionKey + " " + t.getName();
            t.setName(newName);
            if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("Applied key " + undoActionKey + " to " + t);
        }
    }
    private String _getStringKeyForNextMark() {
        final String currentUndoKey = Integer.toHexString(mCurrentUndo.hashCode());
        synchronized (this) {
            taggedUndoActions.put(currentUndoKey, mCurrentUndo);
        }
        return UNDO_ACTION_TAG + currentUndoKey + ")";
    }
    */

    private boolean selectionCleanupForHidden;
    private boolean selectionCleanupForDeleted;

    private void fireUserActionCompleted() {

        if (selectionCleanupForHidden) {
            selectionCleanupForHidden = false;
            VUE.getSelection().clearHidden();
        }
        if (selectionCleanupForDeleted) {
            selectionCleanupForDeleted = false;
            VUE.getSelection().clearDeleted();
        }

        mMap.notify(this, LWKey.UserActionCompleted);        
    }
    
    /**
     * Every event anywhere in the map we're listening to, including events as a result of
     * an Undo or Redo, will get delivered to us here.  If the event has an old value in
     * it and we're not in a Redo, we save it for later Undo/Redo (this includes if the
     * event is a result of a current Undo).  If it's a hierarchy event (e.g., add /
     * remove / delete / forward / back, etc) we handle it specially.
     */

    public void LWCChanged(final LWCEvent e) {

        if (e.key == LWKey.Hidden || e.key == LWKey.Collapsed) {
            // technically, we only need to flag this if the LWComponent in the event is
            // also currently selected, tho theoretically there could be a list of
            // components marked as hidden all at once (each of which we'd have to
            // check), even tho we only currently fire list-based LWCEvent's for
            // hierarchy events.
            selectionCleanupForHidden = true;
        } else if (e.key == LWKey.HierarchyChanging) {
            // todo: better to check LWKey.Deleting?
            selectionCleanupForDeleted = true;
        }

        if (mRedoUnderway) // ignore everything during redo
            return;

        if (e.key == LWKey.RepaintAsync) // ignore these
            return;

        if (mUndoUnderway) {
            if (!mRedoCaptured && mCurrentUndo.size() > 0)  {
                Util.printStackTrace("Undo Error: have changes at start of redo record:"
                                     + "\n\t  current UndoAction: " + mCurrentUndo
                                     + "\n\tcurrent change count: " + mComponentChanges
                                     + "\n\t         UndoManager: " + this
                                     + "\n\t      incoming event: " + e
                                     );
            }
            mRedoCaptured = true;
            if (DEBUG.UNDO) System.out.print("\tredo: " + e);
        } else if (!mCleanupUnderway) {
            
            if (DEBUG.UNDO) Log.debug(this + " " + e);
            
            if (mCurrentUndo.size() == 0 && mCleanupTasks.size() > 0 && mEventsSeenSinceLastMark <= 0) {

                // This can happen if a task is adding during a new user action,
                // and we've seen events, but none of them have had an old value
                // (adding to the Undo queue).  Todo: track this so we know
                // if we've seen ANY events, so we can still see this warning,
                // which is important for catching undo/task bugs.

                // [ Now that we handle cascading tasks, it's safer NOT to run these. ]
                
                //Util.printStackTrace("Undo Warning: have un-run cleanup tasks on first incoming event (running now):"
                if (DEBUG.Enabled)
                    Util.printClassTrace("tufts.vue",
                                         "Undo Warning: have un-run cleanup tasks on first incoming event:"
                                         + "\n\t       UndoManager: " + this
                                         + "\n\t# of cleanup tasks: " + mCleanupTasks.size()
                                         + "\n\t     cleanup tasks: " + mCleanupTasks
                                         //+ "\n\t events since mark: " + mEventsSeenSinceLastMark
                                         + "\n\t    incoming event: " + e
                                         );
                // Fallback:
                // Run them now, to at least ensure as much model integrity as we can.
                // We defintely don't want to run them after more model changes take place,
                // they either need to be run or purged now.
                //runCleanupTasks(true);
            }
        }
        mEventsSeenSinceLastMark++;
        captureEvent(e);
    }

    private void captureEvent(LWCEvent e)
    {
        if (e.key == LWKey.HierarchyChanging || e.getName().startsWith("hier.")) {
            recordEvent(e, true);
        } else if (e.hasOldValue()) {
            recordEvent(e, false);
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (ignored: no old value)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    private void recordEvent(LWCEvent e, boolean hierarchyEvent)
    {
        final UndoAction relevantUndoAction;
        final Map perComponentChanges;
        final Thread thread = Thread.currentThread();

        if (thread instanceof UndoableThread) {
            relevantUndoAction = (UndoAction) ((UndoableThread)thread).getMarker();
            if (relevantUndoAction == null) {
                // This can happen if there was no UndoManager at the time
                // the UndoableThread was started, such as when loading
                // a map (we don't assign an undo manager to a map
                // until it's fully loaded).  In this case, there's
                // nothing to do: these property changes weren't supposed
                // to be undoable in the first place.
                return;
            }

            if (DEBUG.UNDO || DEBUG.THREAD)
                System.out.println("\nHandling UndoableThread " + thread + " event " + e);
            
            perComponentChanges = null; // we can live w/out "compression" for changes on UndoableThread's
            if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("\n" + thread + " initiating change in " + relevantUndoAction);

        } else if (mThreadsWithMark.size() > 0 && mThreadsWithMark.containsKey(thread)) {
            final UndoMark mark = (UndoMark) mThreadsWithMark.get(thread);
            if (DEBUG.UNDO || DEBUG.THREAD)
                Log.debug("FOUND MARK FOR CURRENT THREAD " + thread
                                   + "\n\t mark: " + mark
                                   + "\n\tevent: " + e);
            relevantUndoAction = mark.action;
            perComponentChanges = null;

/*      This code allowed us to tag a thread by tweaking it's name.  Not a very safe method.
        
        } else if (thread.getName().startsWith(UNDO_ACTION_TAG)) {
            String key = thread.getName().substring(UNDO_ACTION_TAG.length());
            key = key.substring(0, key.indexOf(')'));
            final UndoAction taggedUndo;
            synchronized (this) {
                taggedUndo = (UndoAction) taggedUndoActions.get(key);
            }
            System.out.println("Got from key [" + key + "] " + taggedUndo + " for " + e);
            relevantUndoAction = taggedUndo;
            perComponentChanges = null;
*/
        } else {
            //------------------------------------------------------------------
            // This is almost always where we wind up: all the above code
            // in this method is just in case we got an event from
            // a spawned thread, such as an ImageFetcher.
            //------------------------------------------------------------------
            relevantUndoAction = mCurrentUndo;
            perComponentChanges = mComponentChanges;
        }

        if (hierarchyEvent)
            recordUndoableChangeEvent(relevantUndoAction,
                                      perComponentChanges,
                                      LWKey.HierarchyChanging,
                                      (LWContainer) e.getSource(), // parent
                                      HIERARCHY_CHANGE_TAG);
        else
            recordUndoableChangeEvent(relevantUndoAction,
                                      perComponentChanges,
                                      e.key,
                                      e.getComponent(),
                                      e.oldValue);

        mLastEvent = e;

        //recordHierarchyChangingEvent(e, relevantUndoAction, perComponentChanges);
        //recordPropertyChangeEvent(e, relevantUndoAction, perComponentChanges);
    }

    private static final Object HIERARCHY_CHANGE_TAG = "hierarchy.change";
//     private void XrecordHierarchyChangingEvent(LWCEvent e, UndoAction undoAction, Map perComponentChanges)
//     {
//         LWContainer parent = (LWContainer) e.getSource();
//         //recordUndoableChangeEvent(mHierarchyChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
//         //recordUndoableChangeEvent(um.mUndoSequence, um.mComponentChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
//         recordUndoableChangeEvent(undoAction, perComponentChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE_TAG);
//         mLastEvent = e;
//     }
    
//     private void XrecordPropertyChangeEvent(LWCEvent e, UndoAction undoAction, Map perComponentChanges)
//     {
//         // e.getComponent can really be list... todo: warn us if list (should only be for hier events)
//         //recordUndoableChangeEvent(mPropertyChanges, e.getKey(), e.getComponent(), e.getOldValue());
//         recordUndoableChangeEvent(undoAction, perComponentChanges, e.key, e.getComponent(), e.getOldValue());
//         mLastEvent = e;
//     }

    /**
     * Record a property change to a given component with the given property key.  Our
     * objective is to store one old value (the oldest) for each changed
     * component:propertyKey pair.  As such, we can "compress" repeat events for the same
     * component:propertyKey pair.  E.g., a single component is dragged across a map, and
     * we get continuous events with propertyKey "location" for that component.  We only
     * need to store the old value from the FIRST of these events, and we can toss away
     * the old value from all subsequent "location" changes to that same component, as
     * these are just intermediate values over the course of one single user action.
     *
     * Note: if we did store all the intermediate values, along with the time each one
     * happened (UndoItem.order would become a long, set to System.currentTimeMillis() --
     * inefficient but easy), we'd actually be recording all user activity on the map, and
     * be able to play back how they used the tool, or use it to construct demo's or such.
     * Although our "animated undo" would be a much more efficient way of getting
     * essentially the same behavior (interpolate the intermediate values instead of
     * recording them all).
     *
     *
     */

    //private static void recordUndoableChangeEvent(List undoSequence,Map componentChanges,Object propertyKey,LWComponent component,Object oldValue)
    private static void recordUndoableChangeEvent(UndoAction undoAction,
                                                  Map perComponentChanges,
                                                  Object propertyKey,
                                                  LWComponent component,
                                                  Object oldValue)
    {
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        Map allChangesToComponent = null;
        TaggedPropertyValue alreadyStoredValue = null; // a value already stored for this (component,propertyKey)
        
        if (perComponentChanges != null) {
            // If we have a map of existing components, we can do compression (don't have it for UndoableThread's)
            // We look for find existing changes to this particular component, if any.
            allChangesToComponent = (Map) perComponentChanges.get(component);

            if (allChangesToComponent == null) {
                // No prior changes to this component: create a new map for this component for remembering changes in
                allChangesToComponent = new HashMap();
                perComponentChanges.put(component, allChangesToComponent);
            } else {
                // If we already have a change to the same component with the same propertyKey, we can
                //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
                //Object value = allChangesToComponent.get(propertyKey);
                alreadyStoredValue = (TaggedPropertyValue) allChangesToComponent.get(propertyKey);
                if (alreadyStoredValue != null) {
                    if (DEBUG.UNDO) System.out.println(" (compressed)");
                    compressed = true;
                } else if (propertyKey == LWKey.HierarchyChanging && allChangesToComponent.containsKey(LWKey.Created)) {
                    // this will happen once for every damn link auto-grabbed at the end of new group creation.
                    // We may want to re-enabled that auto-grabbing at group creation time for LWLinks...
                    // Also, is happening on new master slide creation...
                    Log.debug("UndoManager: compressing hier change event for newly created component: " + component);
                    if (DEBUG.UNDO) System.out.println(" (compressed:NEW COMPONENT IGNORES HIER CHANGES)");
                    compressed = true;
                }

            }
        }
        
        if (compressed) {
            // If compressed, still make sure the current property change UndoItem is
            // marked as being at the current end of the undo sequence.
            if (undoAction.size() > 1 && alreadyStoredValue != null) {
                //UndoItem undoItem = (UndoItem) undoSequence.get(alreadyStoredValue.index);
                UndoItem undoItem = (UndoItem) undoAction.undoSequence.get(alreadyStoredValue.index);
                if (DEBUG.UNDO&&DEBUG.META) System.out.println("Moving index "
                                                               +alreadyStoredValue.index+" to end index "+undoAction.eventCount
                                                               + " " + undoItem);
                undoItem.order = undoAction.eventCount++;
            }
        } else {
            if (oldValue == HIERARCHY_CHANGE_TAG) {
                final LWContainer container = (LWContainer) component;
                if (container.mChildren == LWComponent.NO_CHILDREN)
                    oldValue = LWComponent.NO_CHILDREN;
                else
                    oldValue = ((ArrayList)container.mChildren).clone(); // TODO: impl dependency on ArrayList
            }
            if (allChangesToComponent != null)
                allChangesToComponent.put(propertyKey, new TaggedPropertyValue(undoAction.size(), oldValue));
            undoAction.undoSequence.add(new UndoItem(component, propertyKey, oldValue, undoAction.eventCount));
            undoAction.eventCount++;
            if (DEBUG.UNDO) {
                System.out.println(" (stored: " + oldValue + ")");
                //if (DEBUG.META) 
                //else System.out.println(" (stored)");
            }
        }
    }

    private static class TaggedPropertyValue {
        int index; // index into undoSequence of this property value
        Object value;

        TaggedPropertyValue(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public String toString() {
            return value + "~" + index;
        }
    }
    
    private void out(String s) {
        Log.debug(mCurrentUndo + ": " + s);
        //Log.debug(this + ": " + s);
    }

    private static void coutln(String termColor, String s) {
        System.out.println(termColor + s + TERM_CLEAR);
    }

    private String paramString() {
        return "" + mCurrentUndo;
    }
    

    public String toString()
    {
        return ""+mCurrentUndo;
//         return "UndoManager[" + mMap.getLabel() + " "
//             + mCurrentUndo
//             + "]"
//             //+ hashCode()
//             ;
    }
    
}

