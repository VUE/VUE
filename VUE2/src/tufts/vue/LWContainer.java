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
import static tufts.Util.copy;

import java.util.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * Manage a group of children within a parent.
 *
 * Handle rendering, duplication, adding/removing and reordering (z-order) of children.
 *
 * @version $Revision: 1.168 $ / $Date: 2010-01-19 20:36:28 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public abstract class LWContainer extends LWComponent
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWContainer.class);

    private static final Object REMOVE_DEFAULT = "default";
    private static final Object REMOVE_DELETE = "delete";

    protected java.util.List<LWComponent> mChildren = NO_CHILDREN;

    @Override
    public void XML_fieldAdded(Object context, String name, Object child) {
        super.XML_fieldAdded(context, name, child);
        if (child instanceof LWComponent) {
            ((LWComponent)child).setParent(this);
        }
    }

    /*
     * Child handling code
     */
    @Override
    public boolean hasChildren() {
        return mChildren.size() > 0;
    }
    @Override
    public int numChildren() {
        return mChildren.size();
    }

    /** @return true: default allows children dragged in and out */
    @Override
    public boolean supportsChildren() { return true;
    }

    /** @return true by default */
    @Override
    public boolean supportsSlide() { return true; }
    
    @Override
    public boolean hasChild(LWComponent c) {
        return mChildren.contains(c);
    }

    /** @return true if we have any children */
    @Override
    public boolean hasContent() {
        return mChildren.size() > 0;
    }
    
    /**
     * @return the list of children.  Note that during a restore, this will always return a list so
     * that castor can add children to it.  This should be the only time the returned list should
     * be modified -- callers of this for iteration should NOT modify the list contents. */
    public java.util.List<LWComponent> getXMLChildList()
    {
        if (mXMLRestoreUnderway) {
            if (mChildren == NO_CHILDREN)
                mChildren = new ArrayList();
            return mChildren;
        } else
            return mChildren;
    }

    /**
     * @return the list of children, or NO_CHILDREN if we've never had any children
     * The collection returned is guaranteed to iterate in the z-order of the children (top most is last). */
    @Override
    public java.util.List<LWComponent> getChildren()
    {
        return mChildren;
    }

    /** @deprecated - use getChildren */
    @Override
    public java.util.List<LWComponent> getChildList()
    {
        return getChildren();
    }
    
    

    /** return child at given index, or null if none at that index */
    @Override
    public LWComponent getChild(int index) {
        if (mChildren.size() > index)
            return mChildren.get(index);
        else
            return null;
    }


    public Iterator getChildIterator()
    {
        return mChildren == NO_CHILDREN ? Util.EmptyIterator : mChildren.iterator();
    }


    /** In case the container subclass can do anything to lay out it's children
     *  (e.g., so that LWNode can override & handle chil layout).
     */
    void layoutChildren() { }

    @Override
    protected void notifyMapLocationChanged(LWComponent src, double mdx, double mdy) {
        super.notifyMapLocationChanged(src, mdx, mdy);
        if (hasChildren()) {
            for (LWComponent c : getChildren())
                c.notifyMapLocationChanged(src, mdx, mdy); // is overcalling updateConnectedLinks (+1 for each depth!), but it's cheap
        }
    }
    
    @Override
    protected void updateConnectedLinks(LWComponent movingSrc)
    {
        super.updateConnectedLinks(movingSrc);
        if (updatingLinks()) {
            // these components are moving in absolute map coordinates, even tho their local location isn't changing
            for (LWComponent c : getChildren()) 
                c.updateConnectedLinks(movingSrc);
        }
    }

    /** called by LWChangeSupport, available here for override by parent classes that want to
     * monitor what's going on with their children */
    void broadcastChildEvent(LWCEvent e) {
        notifyLWCListeners(e);
    }

    /**
     * @param possibleChildren should contain at least one child of this container
     * to be reparented.  Children not in this container are ignored.
     * @param newParent is the new parent for any children of ours found in possibleChildren
     */
    public void reparentTo(final LWContainer newParent, Collection<LWComponent> possibleChildren)
    {
        if (newParent == null) {
            Log.warn(this + "; reparentTo: null new parent reparenting " + possibleChildren);
            return;
        }
        if (newParent == this) {
            //Util.printStackTrace(this + "; attempting to reparent back to ourself: " + possibleChildren);
            Log.warn(this + "; reparentTo: attempting to reparent back to ourself: " + possibleChildren);
            return;
        }
        
        notify(LWKey.HierarchyChanging);

        final List<LWComponent> reparenting = new ArrayList();
        for (LWComponent c : possibleChildren) {
            if (c.getParent() == this)
                reparenting.add(c);
        }
        removeChildren(reparenting);
        /*
        for (LWComponent c : reparenting) {
            float x = c.getX();
            float y = c.getY();
            c.setLocation(getMapX() + x, getMapY() + y);
        }
        */
        newParent.addChildren(reparenting);
    }

    final void removeChild(LWComponent c) {
        removeChildren(Util.iterable(c));
    }

    /** we're moving an entire list of children from one container to another -- preseve z-order, and prevent concurrent modication exceptions */
    public void takeAllChildren(LWContainer source) {

        if (source == this)
            throw new IllegalArgumentException("takeAllChildren: source is target: " + this);

        final List<LWComponent> taking = copy(source.getChildren());
        
        // we can't add the instance of the child list directly, as it's going to be
        // modified as it's iterated by addChildImpl, which is going to extract it from
        // from the source parent's list as each child is moved over
        
        source.removeChildren(taking);
        this.addChildren(taking, ADD_PRESORTED);
        
        //addChildren(taking.toArray(new LWComponent[taking.size()]));
    }

    /** Add the given LWComponents to us as children, using the order they appear in the array
     * for child order (z-order and/or visual order, depending on component impl) */
    public void addChildren(LWComponent[] toAdd)
    {
        addChildren(Arrays.asList(toAdd), ADD_PRESORTED);
    }

//     protected List<LWComponent> sortToMeaningfulZOrder(List<LWComponent> toAdd)
//     {
//         // Do what we can to preserve any meaninful order already
//         // present in the new incoming children.

//         // If we're a group or a layer, use the ZOrderSorter if we can for the add order,
//         // otherwise, everything else uses the YSorter (e.g., a standard VUE node,
//         // which stacks it's children, will then display them in the same vertical
//         // order they had on the map).
        
//         LWComponent[] sorted = null;
            
//         // todo: the default should be z-order sorting: LWNode can override
//         // to use the YSorter
        
//         if (this instanceof LWGroup || this instanceof LWMap.Layer) {
//             //if (sorted[0].getParent() == null) {
//             if (toAdd.get(0).getParent() == null) {
//                 // if first item in list has no parent, we assume none do (they're
//                 // system drag or paste orphans), and we can't sort them based
//                 // on layer, so we leave them alone for now until all nodes have a z-order
//                 // value.
//             } else {
//                 sorted = sort(toAdd, ZOrderSorter);
//             }
//         } else {
//             sorted = sort(toAdd, LWComponent.YSorter);
//         }

//         if (sorted == null)
//             return toAdd;
//         else
//             return Arrays.asList(sorted);
//     }


    //protected List<? extends LWComponent> sortForIncomingZOrder(Collection<? extends LWComponent> toAdd)
    protected Collection<? extends LWComponent> sortForIncomingZOrder(Collection<? extends LWComponent> toAdd)
    {
        // Do what we can to preserve any meaninful order already
        // present in the new incoming children.

        // the default is to replicate the existing z-order sort

        if (toAdd.size() == 0)
            return Collections.EMPTY_LIST;

        Collection<? extends LWComponent> sorted;

        if (Util.getFirst(toAdd).getParent() == null) {
            // if first item in list has no parent, we assume none do (they're
            // probably system drag or paste orphans), and we can't sort them based
            // on z-order, so we leave them alone for now until all nodes have a z-order
            // value.
            sorted = toAdd;
        } else {
            sorted = Arrays.asList(sort(toAdd, ZOrderSorter));
        }
//         if (toAdd.get(0).getParent() == null) {
//             // if first item in list has no parent, we assume none do (they're
//             // probably system drag or paste orphans), and we can't sort them based
//             // on z-order, so we leave them alone for now until all nodes have a z-order
//             // value.
//         } else {
//             toAdd = Arrays.asList(sort(toAdd, ZOrderSorter));
//         }

        return sorted;
    }

//     /** can only be called on constructing containers, and as long as all being added are either also out-model,
//      * or are about to be removed from the model (deleted)
//      */
//     public void addAll(List<LWComponent> toAdd) {
//         if (getParent() != null)
//             throw new IllegalStateException(this + " can only add directly on out-model constructing LWContainers");

//         if (mChildren == NO_CHILDREN)
//             mChildren = new ArrayList(toAdd.size());

//         mChildren.addAll(toAdd);
//     }
    
//     /** absoulute minimum done to reparent -- for layers, where coordinate systems are fully registered */
//     public void setChildren(final ArrayList<LWComponent> children) {

//         if (getParent() != null)
//             notify(LWKey.HierarchyChanging);

//         if (children == null) {
//             mChildren = NO_CHILDREN;
//         } else {
//             mChildren = children;

//             for (LWComponent c : children) {
//                 if (c.parent instanceof LWMap.Layer)
//                     c.parent = this;
//                 else
//                     Log.error("setChildren: cannot steal from a non-layer: " + c + "; in: " + c.parent);
//             }
//         }
//     }

    public void addOnTop(LWComponent existingChild, LWComponent newChild) {

        final int index = indexOf(existingChild);

        final Object context;

        if (index < 0) {
            Log.warn("not an existing child: " + existingChild);
            context = ADD_DEFAULT;
        } else {
            context = Integer.valueOf(index + 1);
        }

        addChildren(Collections.singletonList(newChild), context);
    }

//     final void addChildren(Collection<LWComponent> toAdd, Object context) {
//         if (toAdd instanceof List) {
//             addChildren((List) toAdd, context);
//         } else {
//             addChildren(new ArrayList(toAdd), context);
//         }
//     }
    
    
    /**
     * This will add the contents of the iterable as children to the LWContainer.  If
     * the iterable is a Collection of size greater than 1, we will sort the add list
     * by Y value first to preserve any visual ordering that may be present as best
     * we can (e.g., a vertical arrangement of nodes on a map, if dropped into a node,
     * should keep that order in the node's vertical layout.  So we apply the
     * on-map Y-ordering to the natural order).

     * Special case: if we're a Group, we sort by z-order to preserve visual layer.
     */
    @Override
    public void addChildren(Collection<? extends LWComponent> toAdd, Object context)
    {
        if (DEBUG.PARENTING) track("addChildren/"+context, toAdd);

        //if (toAdd.size() == 1) Util.printStackTrace("ADDONE");
        
        if (toAdd == null || toAdd.size() < 1)
            return;
        
        notify(LWKey.HierarchyChanging);

        if (mChildren == NO_CHILDREN)
            mChildren = new ArrayList(toAdd.size());

        if (toAdd.size() > 1 && context != ADD_PRESORTED)
            toAdd = sortForIncomingZOrder(toAdd);

        // in case the passed in toAdd is a list being used elsewhere, we create a copy
        // to use in the undo queue (and it's also possible that an individual call to
        // addChildImpl will fail) Note: the added list is for components that may be
        // trying to track what's going on in the model: the UndoManager handles
        // hierarchy changes is it's own internal, very reliable way: saving the entire
        // list of children the first time a HierarchyChanging event is seen during a
        // user action, and after that, it can ignore further changes on the same
        // parent: it has all it needs to restore the parent's state before the action.
        
        final List<LWComponent> added = new ArrayList(toAdd.size());

        final long timestamp = System.currentTimeMillis();

        for (LWComponent c : toAdd) {
//             if (c.getParent() == this) // currently needed to support re-dropping into an LWNode
//                 continue;
            try {

                // although a time-stamp will be set if one isn't already provided via
                // the call to ensureID in addChildImpl, we do this here so that add
                // events that involve a collection of nodes will provide the exact same
                // timestamp for all the brand new nodes in the collection
                if (c.getCreated() == 0)
                    c.setCreated(timestamp);
                
                addChildImpl(c, context);
                added.add(c);
            } catch (Throwable t) {
                Log.error(this + "; addChildImpl: " + c + ";", t);
            }
        }

        notify(LWKey.ChildrenAdded, added);

        // todo: for consistency, we should also be issuing a general HierarchyChanged
        // event.  Note we'll have to check into how this effects the UndoManager
        // implemention.  Also: consider an impl that gets rid of
        // ChildrenAdded/ChildrenRemoved completely, and handles it via an internal
        // setChildren that just issues a HierarchyChanged event (simlar to the design
        // of LWPathway.setEntries)
        
        layout();
    }

//     @Override
//     public void notifyHierarchyChanging() {
//         super.notifyHierarchyChanging();
//         for (LWComponent c : getChildren())
//             c.notifyHierarchyChanging();
//     }
    
    
    @Override
    public void notifyHierarchyChanged() {
        super.notifyHierarchyChanged();
        for (LWComponent c : getChildren())
            c.notifyHierarchyChanged();
    }

    private void track(String where, Object o) {
        if (DEBUG.Enabled)
            Log.debug(String.format("\"%-8.8s\" %23s: %s",
                                    getDisplayLabel(),
                                    where,
                                    o instanceof LWComponent ? o : Util.tags(o)));
    }

    

    // TODO: deleteAll: can removeChildren on all, then remove all from model
    
    protected void addChildImpl(LWComponent c, Object context)
    {
        if (DEBUG.PARENTING) track("addChildImpl/"+context, c);
        //new Throwable("ADDCHILDIMPL").printStackTrace();

        if (c == this) 
            throw new Error("attempt to add self as child");

        if (context == ADD_MERGE && c.hasAncestor(this))
            return;

        if (c.getParent() != null && c.getParent().hasChild(c)) {
            //if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent());
            if (DEBUG.PARENTING)
                //if (DEBUG.META) tufts.Util.printStackTrace("FYI["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent()); else
                track("addChildImpl/steal from", c.getParent());
            //out("auto-deparenting " + c + " from " + c.getParent());
            if (c.getParent() == this) {
                //if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] ADD-BACK " + c + " (already our child)");
                if (DEBUG.PARENTING) out("ADD-BACK " + c + " (already our child)");
                // this okay -- in fact useful for child node re-drop on existing parent to trigger
                // re-ordering & re-layout
            }
            //c.notifyHierarchyChanging();
            c.getParent().removeChild(c); // is LWGroup requesting cleanup???
        }

        //if (c.getFont() == null)//todo: really want to do this? only if not manually set?
        //    c.setFont(getFont());

        if (mChildren == NO_CHILDREN)
            mChildren = new ArrayList();
        
        if (context.getClass() == Integer.class) {
            // if context is an Integer, it means a specific index to add at 
            mChildren.add( (Integer)context, c);
        } else {
            mChildren.add(c);
        }

        // consider a real notifyAdd, or notifyAdding/notifyAdded

        //----------------------------------------------------------------------------------------
        // Delicately reparent, taking care that the model does not generate events while
        // in an indeterminate state.
        //----------------------------------------------------------------------------------------

        setAsChildAndLocalize(c);

//         final double newParentMapScale = getMapScale();
//         if (oldParentMapScale != newParentMapScale) {
//             notifyMapScaleChanged(oldParentMapScale, newParentMapScale);
//         }

        //c.reparentNotify(this);
        ensureID(c);

        c.notifyHierarchyChanged();
        
    }

    protected void setAsChildAndLocalize(LWComponent c) {
        
        final LWContainer oldParent = c.getParent();

        //----------------------------------------------------------------------------------------
        // Delicately reparent, taking care that the model does not generate events while
        // in an indeterminate state.
        //----------------------------------------------------------------------------------------
        
        if (oldParent != null && !isManagingChildLocations()) {

            // If we're managing child locations (e.g., and LWNode), no need
            // to localize, as the node will have it's position explicitly assigned
            // by the parent when it lays itself out
        
            // Save current mapX / mapY before setting the parent (which would change the reported mapX / mapY)
            final float oldMapX = c.getMapX();
            final float oldMapY = c.getMapY();
            final double oldParentMapScale = c.getMapScale();

            if (false) {
                localizeCoordinates(c, oldParent, oldParentMapScale, oldMapX, oldMapY);
                c.setParent(this);
            } else {
        
                // Now set the parent, so that when the new location is set, it's already in it's
                // new parent, and it's mapX / mapY will report correctly when asked (e.g., the
                // bounds are immediatley correct for anyone listening to the location event).
                c.setParent(this);
                try {
                    localizeCoordinates(c, oldParent, oldParentMapScale, oldMapX, oldMapY);
                } catch (Throwable t) {
                    Util.printStackTrace(t);
                }
            }
            
        } else {
            
            c.setParent(this);
            
        }
        
    }

//     @Override
//     protected void notifyMapScaleChanged(double oldParentMapScale, double newParentMapScale) {
//         for (LWComponent c : getChildList()) {
//             c.notifyMapScaleChanged(oldParentMapScale, newParentMapScale);
//         }

//     }
    
    protected void localizeCoordinates(LWComponent c,
                                       LWContainer oldParent,
                                       double oldParentMapScale,
                                       float oldMapX,
                                       float oldMapY)
    {

        // Even if the new parent is managing the location, and is about to re-layout and set a new
        // location for this component, we first need to make sure it's location is at least within
        // the coordinate space of it's new parent (it's location is relative to it's new parent),
        // so that when the generic setLocation later runs, it can accuratly compute it's x-map
        // delta-x and delta-y, and make mapLocationChanged calls to update descendents.  If it's
        // old location at that point relative to another, unknown parent, we'd have no way of
        // actually knowing it's old absolute map location.
        
        // This can be a problem during new object creation (e.g., LWGroups) -- if the new object
        // is grabbing children from the map, but the new object is under construction and is not
        // ON the map yet, these setLocation's will never make it do the undo manager to be undone
        // later: This is why we've added the special setLocation API with an event source.

        if (getParent() == null) {
            //if (DEBUG.Enabled) Util.printStackTrace("skipping coordinate localization in unparented (new?) LWContainer: " + this + "; for " + c);
            if (DEBUG.Enabled) Log.warn("coordinate localization in unparented (new?) LWContainer: " + this + "; for " + c);
        } //else

        if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("localizing coordinates: " + c + " oldParent=" + oldParent);

        final LWComponent eventSource;

        // c.getMap() should == getMap() at this point; setParent to this LWContainer has been done above
        if (c.getMap() != getMap())
            Util.printStackTrace("different maps? " + c.getMap() + " != " + getMap());
            
        if (c.getID() != null && c.getMap() == null) {
            // if ID is null, the object is still being created (and we don't need to worry about undoing it's initializations)
            if (oldParent == null || oldParent.getMap() == null) {
                if (DEBUG.Enabled) Util.printStackTrace("FYI: no event source for: " + c
                                                        + ";\n\t           localizing new parent: " + this
                                                        + ";\n\toldParent is not in model either: " + oldParent
                                                        + ";\n\tlocation events will not be available for UNDO");
                eventSource = c;
            } else
                eventSource = oldParent; // if the component has no map, it's not in the model yet, and nobody can hear it.
        } else
            eventSource = c;

        // The version of LWComponent.setLocation with an eventSource argument was created
        // specifically for this call right here:
        final double scale = getMapScale();
        c.setLocation((float) ((oldMapX - getMapX()) / scale),
                      (float) ((oldMapY - getMapY()) / scale),
                      eventSource,
                      false);
        //oldParent == null);

        // The last param above if true means make calls to mapLocationChanged.

        // We'll need something to handle this for LWlinks when dropped into a
        // scaled on-map LWSlide (future feature), tho maybe that'll be handled by a
        // scaleNotify.  This works right now because normally you can only drop
        // into a node, in which case it's layout code will make another setLocation
        // call that the link can pick up, or something at 100% scale, in which case
        // when the drop happens, the curved link's control points are already
        // exactly where they need to be, right where they are, as set by the
        // dragging code.

        if (DEBUG.Enabled||DEBUG.PARENTING || DEBUG.CONTAINMENT) out("         now localized: " + c);
    }
    
    /**
     * Remove any children in this iterator from this container.
     */
    protected final void removeChildren(Iterable<LWComponent> iterable) {
        removeChildren(iterable, REMOVE_DEFAULT);
    }
    
    //private void removeChildren(Iterable<LWComponent> iterable, boolean permanent)

    /**
     * Remove any children in this iterator from this container.
     * If context is REMOVE_DELETE, just ignore objects that are not currently our children.
     */
    protected void removeChildren(Iterable<LWComponent> iterable, Object context)
    {
        final boolean permanent = (context == REMOVE_DELETE);
        
        notify(LWKey.HierarchyChanging);

        ArrayList removedChildren = new ArrayList();
        for (LWComponent c : iterable) {
            if (c.getParent() == this) {
                //c.notifyHierarchyChanging();
                removeChildImpl(c);
                removedChildren.add(c);

                if (permanent)
                    c.removeFromModel();
                
            } else if (!permanent) {
                Log.error(this + " asked to de-parent child it doesn't own: " + c);
            }
        }
        if (removedChildren.size() > 0) {
            notify(LWKey.ChildrenRemoved, removedChildren);
            layout();
        }
        // todo: for consistency, we should also be issuing a general HierarchyChanged event.
    }

    public final void deleteChildrenPermanently(Iterable<LWComponent> iterable)
    {
        removeChildren(iterable, REMOVE_DELETE);
    }
    
    protected void removeChildImpl(LWComponent c)
    {
        //if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] REMOVING " + c);
        if (DEBUG.PARENTING) track("removing", c);
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace(this + "; null child list is null trying to remove: " + c);
            return;
        }
        if (isDeleted()) {
            // just in case:
            new Throwable(this + " FYI: ZOMBIE PARENT DELETING CHILD " + c).printStackTrace();
            return;
        }
        if (!mChildren.remove(c)) {
            Log.warn(this + "; didn't contain child for removal: " + c);
            /*
            if (DEBUG.PARENTING) {
                System.out.println(this + " FYI: didn't contain child for removal: " + c);
                if (DEBUG.META) new Throwable().printStackTrace();
            }
            */
        }
        //c.setParent(null);
    }


    /**
     * Delete a child and PERMANENTLY remove it from the model.
     * Differs from removeChild / removeChildren, which just
     * de-parent the nodes, tho leave any listeners & links to it in place.
     */
    public final void deleteChildPermanently(LWComponent c)
    {
        removeChildren(Util.iterable(c), REMOVE_DELETE);
    }
        
//     public final void deleteChildPermanently(LWComponent c)
//     {
// //         if (c.isLocked()) {
// //             if (DEBUG.Enabled) out("is locked; deletion not permitted: " + c);
// //             return;
// //         }
        
//         if (DEBUG.UNDO || DEBUG.PARENTING) System.out.println("["+getLabel() + "] DELETING PERMANENTLY " + c);

//         // We did the "deleting" notification first, so anybody listening can still see
//         // the node in it's full current state before anything changes.  But children
//         // now keep their parent reference until they're removed from the model, so the
//         // only thing different when removeFromModel issues it's LWKey.Deleting event is
//         // the parent won't list it as a child, but since it still has the parent ref,
//         // event up-notification will still work, which is good enough.  (It's probably
//         // not safe to deliver more than one LWKey.Deleting event -- if need to put it
//         // back here, have to be able to tell removeFromModel optionally not to issue
//         // the event).

//         //c.notify(LWKey.Deleting);
        
//         removeChild(c);
//         c.removeFromModel();
//     }

    protected void removeChildrenFromModel()
    {
        // in certian cases, removing an object from the model will trigger the
        // auto-deleting of other objects from the model (possible also children of
        // ours), so we iterate over a copy of the the list to ensure we don't get any
        // ConcurrentModificationException's
        
        for (LWComponent c : copy(getChildren()))
            c.removeFromModel();
    }
    
    @Override
    protected void prepareToRemoveFromModel()
    {
        removeChildrenFromModel();
        if (mChildren == VUE.ModelSelection) // todo: tmp debug
            throw new IllegalStateException("attempted to delete selection");
    }
    
    @Override
    protected void restoreToModel()
    {
        for (LWComponent c : getChildren()) {
            c.setFlag(Flag.UNDELETING);
            c.setParent(this);
            c.restoreToModel(); // todo: take parent as argument
        }
        super.restoreToModel();
    }

    // todo: should probably just get rid of this helper -- not worth bother
    // If keep, this code may belong on the node as it only implies to
    // the embedded nature of child components in nodes.
    private void ensureLinksPaintOnTopOfAllParents()
    {
        ensureLinksPaintOnTopOfAllParents((LWComponent) this);
        for (LWComponent c : getChildren()) {
            ensureLinksPaintOnTopOfAllParents(c);
            if (c instanceof LWContainer)
                ensureLinksPaintOnTopOfAllParents((LWContainer)c);
        }
    }

    private static void ensureLinksPaintOnTopOfAllParents(LWComponent component)
    {
        for (LWLink link : component.getLinks())
            ensureLinkPaintsOverAllAncestors(link, component);
    }


    static void ensureLinkPaintsOverAllAncestors(LWLink link, LWComponent component)
    {
        LWContainer parent1 = null;
        LWContainer parent2 = null;
        if (link.getHead() != null)
            parent1 = link.getHead().getParent();
        if (link.getTail() != null)
            parent2 = link.getTail().getParent();
        // don't need to do anything if link doesn't cross a (logical) parent boundry
        if (parent1 == parent2)
            return;
            
        // also don't need to do anything if link is BETWEEN a parent and a child
        // (in which case, at the moment, we don't even see the link)
        if (link.isParentChildLink())
            return;
        /*
          System.err.println("*** ENSURING " + l);
          System.err.println("    (parent) " + l.getParent());
          System.err.println("  ep1 parent " + l.getHead().getParent());
          System.err.println("  ep2 parent " + l.getTail().getParent());
        */
        LWContainer commonParent = link.getParent();
        if (commonParent == null) {
            System.out.println("ELPOTOAP: ignoring link with no parent: " + link + " for " + component);
            return;
        }
        if (commonParent != component.getParent()) {
            // If we don't have the same parent, we may need to shuffle the deck
            // so that any links to us will be sure to paint on top of the parent
            // we do have, so you can see the link goes to us (this), and not our
            // parent.  todo: nothing in runtime that later prevents user from
            // sending link to back and creating a very confusing visual situation,
            // unless all of our parents happen to be transparent.
            LWComponent topMostParentThatIsSiblingOfLink = component.getParentWithParent(commonParent);
            if (topMostParentThatIsSiblingOfLink == null) {
                // this could happen for stuff in cutbuffer w/out parent?
                String msg = "FYI; ELPOTOAP couldn't find common parent for " + component;
                if (DEBUG.LINK)
                    tufts.Util.printStackTrace(msg);
                else
                    Log.info(msg);
            } else
                commonParent.ensurePaintSequence(topMostParentThatIsSiblingOfLink, link);
        }
    }
    
    /*
    public java.util.List getAllConnectedNodes()
    {
        // todo opt: could cache this list, or organize as giant iterator tree
        // see LWComponent comment for where to go next
        // Could also make this faster with a cached bounds
        // and explicitly call a getRepaintRegion here that
        // just returns that + any nodes(links) connected to any children
        java.util.List list = super.getAllConnectedNodes();
        list.addAll(children);
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            list.addAll(c.getAllConnectedNodes());
        }
        return list;
    }

    public java.util.List getAllConnectedComponents()
    {
        java.util.List list = super.getAllConnectedComponents();
        list.addAll(children);
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            list.addAll(c.getAllConnectedComponents());
        }
        return list;
    }
    */

    /**
     * The default is to get all ChildKind.PROPER children
     */
    @Override
    public Collection<LWComponent> getAllDescendents() {
        return getAllDescendents(ChildKind.PROPER);
    }
    
    /**
     * The default Order is Order.TREE, and bag is an ArrayList
     */
    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind) {
        return getAllDescendents(kind, new java.util.ArrayList(numChildren()*2), Order.TREE);
    }    

    @Override
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag, Order order)
    {
        if (DEBUG.PARENTING) Log.debug("getAllDescendents " + kind + "," + order + "; in=" +  Util.tags(bag));

        final boolean visibleOnly = (kind == ChildKind.VISIBLE || kind == ChildKind.EDITABLE);
        final boolean editableOnly = (kind == ChildKind.EDITABLE);
        
        for (LWComponent child : getChildren()) {

            if (visibleOnly) {

                // This is the central place where VISIBLE/EDITABLE logic is handled,
                // tho there is some in LWMap in dealing with layers as well.  Note: be
                // careful with changes here: breaking this, or the semantics of
                // isHidden / isFiltered, will break all sorts of operations throughout
                // VUE that fetch descendent lists.
                
                if (child.isHidden()) {
                    // stop all descent: if a node isn't visible, it's children
                    // are guaranteed not to be visible (and thus also not editable).
                    continue;
                } else if (child.isFiltered()) {
                    // we still want to process any further children who may not be filtered,
                    // and thus may still be visible, but we can ignore this child
                    // as being filtered means it's not visible or editable.
                    child.getAllDescendents(kind, bag, order);
                    continue;
                }

                if (editableOnly && (child.isLocked() || child.hasFlag(Flag.ICON))) { // ICON is set for node-icons
                    // no descent: children of locked items are also locked
                    continue;
                }
                
                // go ahead and process normally, paying attention or Order (TREE or DEPTH),
                // as we'll now be adding both this child and it's descendents.
            }

            if (order == Order.TREE) {
                bag.add(child); // outline-style: parent added before children
                child.getAllDescendents(kind, bag, order);
            } else {
                // Order.DEPTH:
                child.getAllDescendents(kind, bag, order);
                bag.add(child); // depth style: deepest components appear first (children before parents)
            }
        }

        // super.getAllDescendents(kind, bag, order); // is a NO-OP
        
        return bag;
    }


    /** @return an iterable containing all ChildKind.PROPER descendents that are instances of the given class */
    public <A extends LWComponent> Iterable<A> getDescendentsOfType(ChildKind kind, Class<A> clazz) {
        
        return Util.typeFilter(getAllDescendents(kind), clazz);

//         final List list = new ArrayList();
//         for (LWComponent c : getAllDescendents())
//             if (clazz.isInstance(c))
//                 list.add(c);
//         return list;
    }
    
    /** @return an iterable containing all direct children that are instances of the given class */
    public <A extends LWComponent> Iterable<A> getChildrenOfType(Class<A> clazz) {

        return Util.typeFilter(getChildren(), clazz);

//         final List list = new ArrayList(numChildren());
//         for (LWComponent c : getChildren())
//             if (clazz.isInstance(c))
//                 list.add(c);
//         return list;
        
    }

    /** same us using: for (LWNode node : getDescendentsOfType(LWNode.class)) { ... } */
    @Override
    public Iterator<LWNode> getAllNodesIterator()    { return getDescendentsOfType(LWNode.class).iterator(); }
    /** same as using: for (LWLink link : getDescendentsOfType(LWLink.class)) { ... } */
    @Override
    public Iterator<LWLink> getAllLinksIterator()    { return getDescendentsOfType(LWLink.class).iterator(); }

    /** same us using: for (LWNode node : getChildrenOfType(LWNode.class)) { ... } */
    @Override
    public Iterator<LWNode> getChildNodeIterator()    { return getChildrenOfType(LWNode.class).iterator(); }
    /** same us using: for (LWLink link : getChildrenOfType(LWLink.class)) { ... } */
    @Override
    public Iterator<LWLink> getChildLinkIterator()    { return getChildrenOfType(LWLink.class).iterator(); }
    

    /** @deprecated - use getChildNodeIterator */
    public Iterator getNodeIterator()    { return getChildNodeIterator(); }
    /** @deprecated - use getChildLinkIterator */
    public Iterator getLinkIterator()    { return getChildLinkIterator(); }

    /** @return the total number of descendents */
    @Override
    public int getDescendentCount() {
        int count = 0;
        for (LWComponent c : getChildren()) {
            count++;
            count += c.getDescendentCount();
        }
        return count;
    }

//     /**
//      * @deprecated -- use getAllDescendents variants
//      * Lighter weight than getAllDescendents, but must be sure not to modify
//      * map hierarchy (do any reparentings) while iterating or may get concurrent
//      * modification exceptions.
//      */
//     public Iterator getAllDescendentsIterator()
//     {
//         VueUtil.GroupIterator gi = new VueUtil.GroupIterator();
//         gi.add(getChildIterator());
//         for (LWComponent c : getChildren()) {
//             if (c.hasChildren())
//                 gi.add(((LWContainer)c).getAllDescendentsIterator());
//         }
//         return gi;
//     }

    @Override
    protected LWComponent defaultPickImpl(PickContext pc)
    {
        //return isDrawn() ? this : null; // should already be handled now in the PointPick traversal
        return this;
    }

    @Override
    protected LWComponent defaultDropTarget(PickContext pc) {
        return this;
    }
    
    
    /** subclasses can override this to change the picking results of children */
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        return c;
    }

    public boolean isOnTop(LWComponent c)
    {
        return indexOf(c) == mChildren.size() - 1;
    }
    public boolean isOnBottom(LWComponent c)
    {
        return indexOf(c) == 0;
    }

//     public int getLayer(LWComponent c)
//     {
//         return indexOf(c);
//     }
    
    protected int indexOf(Object c)
    {
        if (isDeleted())
            throw new IllegalStateException("*** Attempting to get index of a child of a deleted component!"
                                            + "\n\tdeleted parent=" + this
                                            + "\n\tseeking index of child=" + c);
        return mChildren == NO_CHILDREN ? -1 : mChildren.indexOf(c);
    }
        
    /* To preseve the relative display order of a group of elements
     * we're moving forward or sending back, we need to move them in a
     * particular order depending on the operation and how the
     * operation functions.  Note that when moving a group
     * forward/back one move at a time, once the group moves all the
     * way to the front or the back of the list, it will start cycling
     * the order of the components if they're all right next to each
     * other.
     */

    protected static final Comparator ForwardOrder = new Comparator<LWComponent>() {
        public int compare(LWComponent c1, LWComponent c2) {
            return c2.getParent().indexOf(c2) - c1.getParent().indexOf(c1);
        }};
    
    protected static final Comparator ReverseOrder = new Comparator<LWComponent>() {
        public int compare(LWComponent c1, LWComponent c2) {
            LWContainer parent1 = c1.getParent();
            LWContainer parent2 = c2.getParent();
            if (parent1 == null)
                return Short.MIN_VALUE;
            else if (parent2 == null)
                return Short.MAX_VALUE;
            if (parent1 == parent2)
                return parent1.indexOf(c1) - parent1.indexOf(c2);
            else
                return parent1.getDepth() - parent2.getDepth();
        }};

    /** If all the children do not have the same parent, the sort order won't be 100% correct. */
    public static final Comparator ZOrderSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                final LWContainer parent1 = c1.getParent();
                final LWContainer parent2 = c2.getParent();

                // We can't get z-order on a node if it's an orphan (no
                // parent), which is what any paste's or system drags
                // will get us.  So we'll need to keep a sync'd a z-order
                // value in LWComponent to support this in all cases.

                if (parent1 == parent2)
                    return parent1.getChildren().indexOf(c1) - parent2.getChildren().indexOf(c2);
                else
                    return 0;
                // it's possible to figure out which parent is deepest,
                // but we'll save that for later.
            }
        };
    
    
    

    protected static LWComponent[] sort(Collection<? extends LWComponent> bag, Comparator comparator)
    {
        LWComponent[] array = new LWComponent[bag.size()];
        bag.toArray(array);
        java.util.Arrays.sort(array, comparator);
        // Note that it's okay that components with different
        // parents are in this list, as they only need to move
        // relative to layers of any other siblings in the list,
        // and it doesn't matter if re-layering is done to
        // a parent (LWContainer) at a time -- only that the movement
        // order of siblings within a parent is enforced.
        return array;
    }
    
    /** 
     * Make component(s) paint first & hit last (on bottom)
     */
    public static void bringToFront(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ReverseOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().bringToFront(comps[i]);
    }
    public static void bringForward(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ForwardOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().bringForward(comps[i]);
    }
    /** 
     * Make component(s) paint last & hit first (on top)
     */
    public static void sendToBack(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ForwardOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().sendToBack(comps[i]);
    }
    public static void sendBackward(List selectionList)
    {
        LWComponent[] comps = sort(selectionList, ReverseOrder);
        for (int i = 0; i < comps.length; i++)
            comps[i].getParent().sendBackward(comps[i]);
    }

    public boolean bringToFront(LWComponent c)
    {
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace("no children " + this + "; " + c);
            return false;
        }
        
        // Move to END of list, so it will paint last (visually on top)
        int idx = mChildren.indexOf(c);
        int idxLast = mChildren.size() - 1;
        if (idx < 0 || idx == idxLast)
            return false;
        //System.out.println("bringToFront " + c);
        notify(LWKey.HierarchyChanging);
        mChildren.remove(idx);
        mChildren.add(c);
        // we layout the parent because a parent node may lay out
        // it's children in the order they appear in this list
        notify("hier.move.front", c);
        c.getParent().layoutChildren();
        return true;
    }
    public boolean sendToBack(LWComponent c)
    {
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace("no children " + this + "; " + c);
            return false;
        }
        
        // Move to FRONT of list, so it will paint first (visually on bottom)
        int idx = mChildren.indexOf(c);
        if (idx <= 0)
            return false;
        //System.out.println("sendToBack " + c);
        notify(LWKey.HierarchyChanging);
        mChildren.remove(idx);
        mChildren.add(0, c);
        notify("hier.move.back", c);
        c.getParent().layoutChildren();
        return true;
    }
    public boolean bringForward(LWComponent c)
    {
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace("no children " + this + "; " + c);
            return false;
        }
        
        // Move toward the END of list, so it will paint later (visually on top)
        int idx = mChildren.indexOf(c);
        int idxLast = mChildren.size() - 1;
        if (idx < 0 || idx == idxLast)
            return false;
        //System.out.println("bringForward " + c);
        notify(LWKey.HierarchyChanging);
        swap(idx, idx + 1);
        notify("hier.move.forward", c);
        c.getParent().layoutChildren();
        return true;
    }
    public boolean sendBackward(LWComponent c)
    {
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace("no children " + this + "; " + c);
            return false;
        }
        
        // Move toward the FRONT of list, so it will paint sooner (visually on bottom)
        int idx = mChildren.indexOf(c);
        if (idx <= 0) 
            return false;
        //System.out.println("sendBackward " + c);
        notify(LWKey.HierarchyChanging);
        swap(idx, idx - 1);
        notify("hier.move.backward", c);
        c.getParent().layoutChildren();
        return true;
    }

    private void swap(int i, int j)
    {
        //System.out.println("swapping positions " + i + " and " + j);
        mChildren.set(i, mChildren.set(j, mChildren.get(i)));
    }

    // essentially this implements an "insert-after" of top relative to bottom
    void ensurePaintSequence(LWComponent onBottom, LWComponent onTop)
    {
        if (mChildren == NO_CHILDREN) {
            Util.printStackTrace("no children " + this + "; bot=" + onBottom + "; top=" + onTop);
            return;
        }

        if (onBottom.getParent() != this || onTop.getParent() != this) {
            System.out.println(this + "ensurePaintSequence: both aren't children " + onBottom + " " + onTop);
            return;
            //throw new IllegalArgumentException(this + "ensurePaintSequence: both aren't children " + onBottom + " " + onTop);
        }
        int bottomIndex = indexOf(onBottom);
        int topIndex = indexOf(onTop);
        if (bottomIndex < 0 || topIndex < 0) {
            if (DEBUG.Enabled)
                Util.printStackTrace(this + "ensurePaintSequence: both aren't in list! " + bottomIndex + " " + topIndex);
            return;
        }
        //if (DEBUG.PARENTING) System.out.println("ENSUREPAINTSEQUENCE: " + onBottom + " " + onTop);
        if (topIndex == (bottomIndex - 1)) {
            if (DEBUG.PARENTING) out("ensurePaintSequence: swapping adjacents " + onTop);
            notify(LWKey.HierarchyChanging);
            swap(topIndex, bottomIndex);
            notify("hier.sequence");
        } else if (topIndex < bottomIndex) {
            if (DEBUG.PARENTING) out("ensurePaintSequence: re-inserting after botIndex " + onTop);
            notify(LWKey.HierarchyChanging);
            mChildren.remove(topIndex);
            // don't forget that after above remove the indexes have all been shifted down one
            if (bottomIndex >= mChildren.size())
                mChildren.add(onTop);
            else
                mChildren.add(bottomIndex, onTop);
            notify("hier.sequence");
        } else {
            if (DEBUG.PARENTING) out("ensurePaintSequence: already sequenced: " + onTop);
        }
        //if (DEBUG.PARENTING) System.out.println("ensurepaintsequence: " + onBottom + " " + onTop);
        
    }
    
    
    @Override
    protected void setScale(double scale)
    {
        //System.out.println("Scale set to " + scale + " in " + this);
        
        super.setScale(scale);

        layoutChildren(); // we do this for our rollover zoom hack so children are repositioned
    }


    /**
     * Default impl just fills the background and draws any children.
     */
    @Override
    protected void drawImpl(DrawContext dc)
    {
        //if (!isTransparent()) {
        final Color fill = getRenderFillColor(dc);
        if (fill != null) {
            dc.g.setColor(fill);
            dc.g.fill(getZeroShape());
        }
        
        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(getZeroShape());
        }

        drawChildren(dc);
    }

    protected void drawChildren(DrawContext dc)
    {
        if (hasChildren() == false)
            return;

        for (LWComponent c : getChildren()) {

            //-------------------------------------------------------
            // Using a requiresPaint is a huge speed optimzation.
            // Eliminating all the Graphics2D calls that would end up
            // having to check the clipBounds internally makes a big
            // difference.
            // -------------------------------------------------------

            if (c.requiresPaint(dc)) {
                drawChildSafely(dc, c);
            }
        }
        //if (DEBUG) out("PAINTED " + types);

    }

    private void drawChildSafely(DrawContext _dc, LWComponent c)
    {
        // todo opt: potentially use dc.push/pop that instead of creating & disposing
        // GC's, records/resets the transform.

        final DrawContext dc = _dc.create();
        try {
            drawChild(c, dc);
        } catch (Throwable t) {
            synchronized (System.err) {
                tufts.Util.printStackTrace(t);
                System.err.println("*** Exception drawing: " + c);
                System.err.println("***         In parent: " + this);
                System.err.println("***         DC parent: " + _dc);
                System.err.println("***          DC child: " + dc);
                System.err.println("***   Graphics parent: " + _dc.g);
                System.err.println("***    Graphics child: " + dc.g);
                System.err.println("***   Transform-start: " + _dc.g.getTransform());
                System.err.println("***     Transform-end: " + dc.g.getTransform());
                System.err.println("***              clip: " + dc.g.getClip());
                System.err.println("***        clipBounds: " + dc.g.getClipBounds());
            }
        }
        finally {
            dc.dispose();
        }
    }

    

    protected void drawChild(LWComponent child, DrawContext dc)
    {
        child.drawLocal(dc);
    }

    /**
     * Be sure to duplicate all children and set parent/child references,
     * and if we weren't given a LinkPatcher, to patch up any links
     * among our children.
     */
    @Override
    public LWContainer duplicate(CopyContext cc)
    {
        boolean isPatcherOwner = false;
        
        if (cc.patcher == null && cc.dupeChildren && hasChildren()) {

            // Normally VUE Actions (e.g. Duplicate, Copy, Paste)
            // provide a patcher for duplicating a selection of
            // objects, but anyone else may not have provided one.
            // This will take care of arbitrary single instances of
            // duplication, including duplicating an entire Map.
            
            cc.patcher = new LinkPatcher();
            isPatcherOwner = true;
        }
        
        final LWContainer containerCopy = (LWContainer) super.duplicate(cc);

        if (cc.dupeChildren && mChildren != NO_CHILDREN) {

            containerCopy.mChildren = new ArrayList(mChildren.size());
            
            for (LWComponent c : getChildren()) {
                LWComponent childCopy = c.duplicate(cc);
                containerCopy.mChildren.add(childCopy);
                childCopy.setParent(containerCopy);
            }
        }

        if (isPatcherOwner)
            cc.patcher.reconnectLinks();
            
        return containerCopy;
    }

    @Override
    public String paramString()
    {
        if (hasChildren())
            return super.paramString() + " chld=" + numChildren();
        else
            return super.paramString();
            
    }
    
    
}
