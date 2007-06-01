/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import tufts.Util;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Manage a group of children within a parent.
 *
 * Handle rendering, hit-detection, duplication, adding/removing children.
 *
 * @version $Revision: 1.117 $ / $Date: 2007-06-01 20:34:05 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public abstract class LWContainer extends LWComponent
{
    protected java.util.List<LWComponent> children = new java.util.ArrayList<LWComponent>();
    protected LWComponent focusComponent;
    
    public void XML_fieldAdded(String name, Object child) {
        super.XML_fieldAdded(name, child);
        if (child instanceof LWComponent) {
            ((LWComponent)child).setParent(this);
        }
    }

    /*
     * Child handling code
     */
    public boolean hasChildren() {
        return children != null && children.size() > 0;
    }
    public int numChildren() {
        return children == null ? 0 : children.size();
    }

    /** @return false -- default impl is child coordinates are relatve to the parent -- overide if subclass impl changes this */
    public boolean hasAbsoluteChildren() {
        return false;
    }

    /** @return true: default allows children dragged in and out */
    @Override
    public boolean supportsChildren() {
        return true;
    }
    

    public boolean hasChild(LWComponent c) {
        return children.contains(c);
    }

    /** @return true if we have any children */
    public boolean hasContent() {
        return children != null || children.size() > 0;
    }
    
    public java.util.List<LWComponent> getChildList()
    {
        return this.children;
    }

    /** return child at given index, or null if none at that index */
    public LWComponent getChild(int index) {
        if (children != null && children.size() > index)
            return children.get(index);
        else
            return null;
    }

    public java.util.Iterator<LWComponent> getChildIterator()
    {
        return this.children.iterator();
    }


//     /**
//      * return first ancestor of type clazz, or if no matching ancestor
//      * is found, return the upper most ancestor (one that has no
//      * parent, which is normally the LWMap)
//      */
//     public LWContainer getFirstAncestor(Class clazz)
//     {
//         if (getParent() == null)
//             return this;
//         else if (clazz.isInstance(getParent()))
//             return getParent();
//         else
//             return getParent().getFirstAncestor(clazz);
//     }

    
    public Iterator getNodeIterator()    { return getNodeList().iterator(); }
    // todo: temporary for html?
    private List getNodeList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWNode)
                list.add(c);
        }
        return list;
    }
    public Iterator getLinkIterator()    { return getLinkList().iterator(); }
    // todo: temporary for html?
    private List getLinkList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink)
                list.add(c);
        }
        return list;
    }

    /** In case the container subclass can do anything to lay out it's children
     *  (e.g., so that LWNode can override & handle chil layout).
     */
    void layoutChildren() { }

    protected void updateConnectedLinks()
    {
        super.updateConnectedLinks();
        if (VUE.RELATIVE_COORDS) {
            // these components are now moving on the map, even tho their local location isn't changing
            for (LWComponent c : getChildList()) 
                c.updateConnectedLinks();
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
    public void reparentTo(LWContainer newParent, Collection<LWComponent> possibleChildren)
    {
        notify(LWKey.HierarchyChanging);

        List<LWComponent> reparenting = new ArrayList();
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

    public final void addChild(LWComponent c) {
        addChildren(new VueUtil.SingleIterator(c));
    }
    
    final void removeChild(LWComponent c) {
        removeChildren(new VueUtil.SingleIterator(c));
    }

//     public void addChildrenPreservingOrder(List addList)
//     {
//         LWComponent[] toAdd = (LWComponent[]) addList.toArray(new LWComponent[addList.size()]);
//         if (this instanceof LWGroup) {
//             if (toAdd[0].getParent() == null) {
//                 // if first item in list has no parent, we assume none do (they're
//                 // system drag or paste orphans), and we can't sort them based
//                 // on layer, so we leave them alone for now until all nodes have a z-order
//                 // value.
//             } else {
//                 java.util.Arrays.sort(toAdd, LayerSorter);
//             }
//         } else {
//             java.util.Arrays.sort(toAdd, LWComponent.YSorter);
//         }
//         addChildren(toAdd);
//     }
    
    /** If all the children do not have the same parent, the sort order won't be 100% correct. */
    private static final Comparator<LWComponent> LayerSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                final LWContainer parent1 = c1.getParent();
                final LWContainer parent2 = c2.getParent();

                // We can't get z-order on a node if it's an orphan (no
                // parent), which is what any paste's or system drags
                // will get us.  So we'll need to keep a sync'd a z-order
                // value in LWComponent to support this in all cases.

                if (parent1 == parent2)
                    return parent1.children.indexOf(c1) - parent2.children.indexOf(c2);
                else
                    return 0;
                // it's possible to figure out which parent is deepest,
                // but we'll save that for later.
            }
        };
    
    // public void addChildren(Iterator<LWComponent> i) {}

    
    /** Add the given LWComponents to us as children, using the order they appear in the array
     * for child order (z-order and/or visual order, depending on component impl) */
    protected void addChildren(LWComponent[] toAdd)
    {
        // note: as ArrayIterator is not a collection, it won't
        // be re-sorted below.
        addChildren(new tufts.Util.ArrayIterator(toAdd));
    }
    
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
    public void addChildren(Iterable<LWComponent> iterable)
    {
        notify(LWKey.HierarchyChanging);

        if (iterable instanceof Collection && ((Collection)iterable).size() > 1) {

            // Do what we can to preserve any meaninful order already
            // present in the new incoming children.

            final Collection<LWComponent> bag = (Collection) iterable;
            final LWComponent[] sorted = bag.toArray(new LWComponent[bag.size()]);

            // If we're a group, use the LayerSorted if we can for the add order,
            // otherwise, everything else uses the YSorter
            
            if (this instanceof LWGroup) {
                if (sorted[0].getParent() == null) {
                    // if first item in list has no parent, we assume none do (they're
                    // system drag or paste orphans), and we can't sort them based
                    // on layer, so we leave them alone for now until all nodes have a z-order
                    // value.
                } else {
                    java.util.Arrays.sort(sorted, LayerSorter);
                }
            } else {
                java.util.Arrays.sort(sorted, LWComponent.YSorter);
            }
            iterable = new tufts.Util.ArrayIterator(sorted);
        }
        
        final List<LWComponent> addedChildren = new ArrayList();

        for (LWComponent c : iterable) {
            addChildImpl(c);
            addedChildren.add(c);
        }

        // need to do this afterwords so everyone has a parent to check
        for (LWComponent c : addedChildren)
            ensureLinksPaintOnTopOfAllParents(c);
        
        
        if (addedChildren.size() > 0) {
            notify(LWKey.ChildrenAdded, addedChildren);
            // todo: should be able to do this generically here
            // instead of having to override addChildren in LWNode
            //setScale(getScale()); // make sure children get shrunk
            layout();
        }
    }

    
    /**
     * Remove any children in this iterator from this container.
     */
    protected void removeChildren(Iterable<LWComponent> iterable)
    {
        notify(LWKey.HierarchyChanging);

        ArrayList removedChildren = new ArrayList();
        for (LWComponent c : iterable) {
            if (c.getParent() == this) {
                removeChildImpl(c);
                removedChildren.add(c);
            } else
                throw new IllegalStateException(this + " asked to remove child it doesn't own: " + c);
        }
        if (removedChildren.size() > 0) {
            notify(LWKey.ChildrenRemoved, removedChildren);
            layout();
        }
    }

    // TODO: deleteAll: can removeChildren on all, then remove all from model
    
    protected void addChildImpl(LWComponent c)
    {
        if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] ADDING   " + c);
        if (c.getParent() != null && c.getParent().getChildList().contains(c)) {
            //if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent());
            if (DEBUG.PARENTING)
                if (DEBUG.META)
                    tufts.Util.printStackTrace("FYI["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent());
                else
                    out("auto-deparenting " + c + " from " + c.getParent());
            if (c.getParent() == this) {
                if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] ADD-BACK " + c + " (already our child)");
                // this okay -- in fact useful for child node re-drop on existing parent to trigger
                // re-ordering & re-layout
            }
            c.getParent().removeChild(c);
        }
        if (c.getFont() == null)//todo: really want to do this? only if not manually set?
            c.setFont(getFont());
        this.children.add(c);

        //----------------------------------------------------------------------------------------
        // Delicately reparent, taking care that the model does not generate events while
        // in an indeterminate state.
        //----------------------------------------------------------------------------------------
        
        // Save current mapX / mapY before setting the parent (which would change the reported mapX / mapY)
        final float oldMapX = c.getMapX();
        final float oldMapY = c.getMapY();
        final LWContainer oldParent = c.getParent();

        // Now set the parent, so that when the new location is set, it's already in it's
        // new parent, and it's mapX / mapY will report correctly when asked (e.g., the
        // bounds are immediatley correct for anyone listening to the location event).
        c.setParent(this);
        
        if (!hasAbsoluteChildren() && oldParent != null && !oldParent.hasAbsoluteChildren()) {
            // todo: if we have abs children, but OLD parent had relative children, we need to
            // translate back to absolute map coords
            // if it didn't have a parent, assume coords we're local (e.g., from a duplication)
            translateLocationToLocalCoordinates(c, oldMapX, oldMapY);
        }
        //c.reparentNotify(this);
        ensureID(c);
    }

    // TODO: only works when moving from higher to lower nesting -- not reverse (e.g., group dispersal)
    protected void translateLocationToLocalCoordinates(LWComponent c, float oldMapX, float oldMapY) {
        if (VUE.RELATIVE_COORDS && !c.hasAbsoluteMapLocation()) {
            if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("translateToLocal " + c);
            c.setLocation((oldMapX - getMapX()) / getMapScale(),
                          (oldMapY - getMapY()) / getMapScale());
        }
    }

    protected void removeChildImpl(LWComponent c)
    {
        if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] REMOVING " + c);
        if (this.children == null) {
            // this should never be possible now
            new Throwable(this + " CHILD LIST IS NULL TRYING TO REMOVE " + c).printStackTrace();
            return;
        }
        if (isDeleted()) {
            // just in case:
            new Throwable(this + " FYI: ZOMBIE PARENT DELETING CHILD " + c).printStackTrace();
            return;
        }
        if (!this.children.remove(c)) {
            throw new IllegalStateException(this + " didn't contain child for removal: " + c);
            /*
            if (DEBUG.PARENTING) {
                System.out.println(this + " FYI: didn't contain child for removal: " + c);
                if (DEBUG.META) new Throwable().printStackTrace();
            }
            */
        }
        //c.setParent(null);

        // If this child was scaled inside us (as all children are except groups)
        // be sure to restore it's scale back to 1 when de-parenting it.
        // TODO: better to handle this in LWNode removeChildImpl as that's only
        // place nodes actually get auto scaled right now -- either that or
        // when it's added back into it's new parent, which can set it based
        // on whatever scale policy it implements.
        if (c.getScale() != 1f)
            c.setScale(1f);
    }
    

    /**
     * Delete a child and PERMANENTLY remove it from the model.
     * Differs from removeChild / removeChildren, which just
     * de-parent the nodes, tho leave any listeners & links to it in place.
     */
    public void deleteChildPermanently(LWComponent c)
    {
        if (DEBUG.UNDO || DEBUG.PARENTING) System.out.println("["+getLabel() + "] DELETING PERMANENTLY " + c);

        // We did the "deleting" notification first, so anybody listening can still see
        // the node in it's full current state before anything changes.  But children
        // now keep their parent reference until they're removed from the model, so the
        // only thing different when removeFromModel issues it's LWKey.Deleting event is
        // the parent won't list it as a child, but since it still has the parent ref,
        // event up-notification will still work, which is good enough.  (It's probably
        // not safe to deliver more than one LWKey.Deleting event -- if need to put it
        // back here, have to be able to tell removeFromModel optionally not to issue
        // the event).

        //c.notify(LWKey.Deleting);
        
        removeChild(c);
        c.removeFromModel();
    }

    protected void removeChildrenFromModel()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.removeFromModel();
        }
    }
    
    protected void prepareToRemoveFromModel()
    {
        removeChildrenFromModel();
        if (this.children == VUE.ModelSelection) // todo: tmp debug
            throw new IllegalStateException("attempted to delete selection");
    }
    
    protected void restoreToModel()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setParent(this);
            c.restoreToModel();
        }
        super.restoreToModel();
    }

    // todo: should probably just get rid of this helper -- not worth bother
    // If keep, this code may belong on the node as it only implies to
    // the embedded nature of child components in nodes.
    private void ensureLinksPaintOnTopOfAllParents()
    {
        ensureLinksPaintOnTopOfAllParents((LWComponent) this);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            ensureLinksPaintOnTopOfAllParents(c);
            if (c instanceof LWContainer)
                ensureLinksPaintOnTopOfAllParents((LWContainer)c);
        }
    }

    private static void ensureLinksPaintOnTopOfAllParents(LWComponent component)
    {
        for (LWLink l : component.getLinks()) {
            LWContainer parent1 = null;
            LWContainer parent2 = null;
            if (l.getHead() != null)
                parent1 = l.getHead().getParent();
            if (l.getTail() != null)
                parent2 = l.getTail().getParent();
            // don't need to do anything if link doesn't cross a (logical) parent boundry
            if (parent1 == parent2)
                    continue;
            
            // also don't need to do anything if link is BETWEEN a parent and a child
            // (in which case, at the moment, we don't even see the link)
            if (l.isParentChildLink())
                continue;
            /*
            System.err.println("*** ENSURING " + l);
            System.err.println("    (parent) " + l.getParent());
            System.err.println("  ep1 parent " + l.getHead().getParent());
            System.err.println("  ep2 parent " + l.getTail().getParent());
            */
            LWContainer commonParent = l.getParent();
            if (commonParent == null) {
                System.out.println("ELPOTOAP: ignoring link with no parent: " + l + " for " + component);
                continue;
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
                        VUE.Log.info(msg);
                } else
                    commonParent.ensurePaintSequence(topMostParentThatIsSiblingOfLink, l);
            }
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

    public java.util.List getAllLinks()
    {
        java.util.List list = new java.util.ArrayList();
        list.addAll(super.getAllLinks());
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            list.addAll(c.getAllLinks());
        }
        return list;
    }
    


    public Collection<LWComponent> getAllDescendents() {
        return getAllDescendents(ChildKind.PROPER);
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind) {
        return getAllDescendents(kind, new java.util.ArrayList());
    }    

    /** @param list -- if provided, must be non-null: results will go there, and this object also returned */
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection bag)
    {
        for (LWComponent c : this.children) {
            if (kind == ChildKind.VISIBLE && c.isHidden())
                continue;
            bag.add(c);
            c.getAllDescendents(kind, bag);
        }

        super.getAllDescendents(kind, bag);
        
        return bag;
    }

    /**
     * @deprecated -- use getAllDescendents variants
     * Lighter weight than getAllDescendents, but must be sure not to modify
     * map hierarchy (do any reparentings) while iterating or may get concurrent
     * modification exceptions.
     */
    public Iterator getAllDescendentsIterator()
    {
        VueUtil.GroupIterator gi = new VueUtil.GroupIterator();
        gi.add(children.iterator());
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.hasChildren())
                gi.add(((LWContainer)c).getAllDescendentsIterator());
        }
        return gi;
    }

    /**
     * Get all descendents, but do not seperately include
     * the children of groups.
     * @deprecated - needs replacement: see Actions.java
     */
    public java.util.List getAllDescendentsGroupOpaque()
    {
        java.util.List list = new java.util.ArrayList();
        list.addAll(children);
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.hasChildren() && !(c instanceof LWGroup))
                list.addAll(((LWContainer)c).getAllDescendents());
        }
        return list;
    }

    public void setFocusComponent(LWComponent c)
    {
        this.focusComponent = c;
    }
    
    /*
     * Find child at mapX, mapY -- may actually return this component also.

    private static ArrayList curvedLinks = new ArrayList();
    public LWComponent findChildAt(float mapX, float mapY)
    {
        if (DEBUG.CONTAINMENT) System.out.println("LWContainer.findChildAt[" + getLabel() + "]");

        // if there's a focus (zoomed) component, it's always on top
        if (focusComponent != null && focusComponent.contains(mapX, mapY)) {
            if (focusComponent instanceof LWContainer)
                return ((LWContainer)focusComponent).findChildAt(mapX, mapY);
            else
                return focusComponent;
        }

        curvedLinks.clear();
        // hit detection must traverse list in reverse as top-most
        // components are at end
        for (ListIterator i = children.listIterator(children.size()); i.hasPrevious();) {
            LWComponent c = (LWComponent) i.previous();
            if (c.isHidden())
                continue;
            if (c.isFiltered() && !c.hasChildren())  // even if filtered out, children may not be
                continue;
            if (c instanceof LWLink && ((LWLink)c).getControlCount() > 0) {
                curvedLinks.add(c);
                continue;
            }
            if (c.contains(mapX, mapY)) {
                return c.findChildAt(mapX, mapY);
                /*
                if (c.hasChildren())
                    return ((LWContainer)c).findChildAt(mapX, mapY);
                else 
                    return c.isFiltered() ? null : c;
                *
            }
        }
        // we check curved links last because they can take up so much
        // hit-space (the entire interior of their arc)
        // todo: this is a hack: do curved link detection via checking all segments...
        for (Iterator i = curvedLinks.iterator(); i.hasNext();) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(mapX, mapY))
                return c.isFiltered() ? null : c;
        }
        return defaultHitComponent();
    }

    
    // TODO: better to replace with a traversal/visit mechanism
    // and completely remove the focusComponent crap: handle that another way (e.g., like pop-up rollover)
    private LWComponent pickDeepestChildAt(float mapX, float mapY, LWComponent excluded, boolean ignoreSelected)
    {
        // TODO: change this gross focusComponent hack to a cleaner special case:
        // have the entire LWMap maintain a list of all the current focus components,
        // (the deepest + all it's parents) and always check that first & no matter what
        if (focusComponent != null && focusComponent.contains(mapX, mapY))
            return focusComponent.findDeepestChildAt(mapX, mapY, excluded, ignoreSelected);
        
        // hit detection must traverse list in reverse as top-most
        // components are at end
        
        curvedLinks.clear();
        
        for (ListIterator i = children.listIterator(children.size()); i.hasPrevious();) {
            LWComponent c = (LWComponent) i.previous();
            if (c == excluded)
                continue;
            if (ignoreSelected && c.isSelected())
                continue;
            if (c.isHidden())
                continue;
            if (c.isFiltered() && !c.hasChildren())  // even if filtered out, children may not be
                continue;
            if (c instanceof LWLink && ((LWLink)c).getControlCount() > 0) {
                curvedLinks.add(c);
                continue;
            }
            if (c instanceof LWContainer) {
                LWContainer container = (LWContainer) c;
                // focus component may temporarily extend outside the bounds
                // of it's parent, so we have to check them manually, as
                // opposed for checking the parent bounds below before
                // we bother to look within the container.
                if (container.focusComponent != null && container.focusComponent.contains(mapX, mapY))
                    return container.focusComponent.findDeepestChildAt(mapX, mapY, excluded, ignoreSelected);
            }
            if (c.contains(mapX, mapY))
                return c.findDeepestChildAt(mapX, mapY, excluded, ignoreSelected);
        }
        // we check curved links last because they can take up so much
        // hit-space (the entire interior of their arc)
        for (Iterator i = curvedLinks.iterator(); i.hasNext(); ) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(mapX, mapY))
                return c;
        }
                 
        return defaultHitComponent();
    }

    */

    protected LWComponent defaultPickImpl(PickContext pc)
    {
        //return isDrawn() ? this : null; // should already be handled now in the PointPick traversal
        return this;
    }

    protected LWComponent defaultDropTarget(PickContext pc) {
        return this;
    }
    
    
    /** subclasses can override this to change the picking results of children */
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        return c;
    }

    public boolean isOnTop(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return indexOf(c) == children.size()-1;
    }
    public boolean isOnBottom(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return indexOf(c) == 0;
    }

    public int getLayer(LWComponent c)
    {
        return indexOf(c);
    }
    
    protected int indexOf(Object c)
    {
        if (isDeleted())
            throw new IllegalStateException("*** Attempting to get index of a child of a deleted component!"
                                            + "\n\tdeleted parent=" + this
                                            + "\n\tseeking index of child=" + c);
        return children.indexOf(c);
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

    protected static LWComponent[] sort(List selection, Comparator comparator)
    {
        LWComponent[] array = new LWComponent[selection.size()];
        selection.toArray(array);
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
        // Move to END of list, so it will paint last (visually on top)
        int idx = children.indexOf(c);
        int idxLast = children.size() - 1;
        if (idx == idxLast)
            return false;
        //System.out.println("bringToFront " + c);
        notify(LWKey.HierarchyChanging);
        children.remove(idx);
        children.add(c);
        // we layout the parent because a parent node may lay out
        // it's children in the order they appear in this list
        notify("hier.move.front", c);
        c.getParent().layoutChildren();
        return true;
    }
    public boolean sendToBack(LWComponent c)
    {
        // Move to FRONT of list, so it will paint first (visually on bottom)
        int idx = children.indexOf(c);
        if (idx <= 0)
            return false;
        //System.out.println("sendToBack " + c);
        notify(LWKey.HierarchyChanging);
        children.remove(idx);
        children.add(0, c);
        notify("hier.move.back", c);
        c.getParent().layoutChildren();
        return true;
    }
    public boolean bringForward(LWComponent c)
    {
        // Move toward the END of list, so it will paint later (visually on top)
        int idx = children.indexOf(c);
        int idxLast = children.size() - 1;
        if (idx == idxLast)
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
        // Move toward the FRONT of list, so it will paint sooner (visually on bottom)
        int idx = children.indexOf(c);
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
        children.set(i, children.set(j, children.get(i)));
    }

    // essentially this implements an "insert-after" of top relative to bottom
    void ensurePaintSequence(LWComponent onBottom, LWComponent onTop)
    {
        if (onBottom.getParent() != this || onTop.getParent() != this) {
            System.out.println(this + "ensurePaintSequence: both aren't children " + onBottom + " " + onTop);
            return;
            //throw new IllegalArgumentException(this + "ensurePaintSequence: both aren't children " + onBottom + " " + onTop);
        }
        int bottomIndex = indexOf(onBottom);
        int topIndex = indexOf(onTop);
        if (bottomIndex < 0 || topIndex < 0)
            throw new IllegalStateException(this + "ensurePaintSequence: both aren't in list! " + bottomIndex + " " + topIndex);
        //if (DEBUG.PARENTING) System.out.println("ENSUREPAINTSEQUENCE: " + onBottom + " " + onTop);
        if (topIndex == (bottomIndex - 1)) {
            notify(LWKey.HierarchyChanging);
            swap(topIndex, bottomIndex);
            notify("hier.sequence");
            if (DEBUG.PARENTING) System.out.println("ensurePaintSequence: swapped " + onTop);
        } else if (topIndex < bottomIndex) {
            notify(LWKey.HierarchyChanging);
            children.remove(topIndex);
            // don't forget that after above remove the indexes have all been shifted down one
            if (bottomIndex >= children.size())
                children.add(onTop);
            else
                children.add(bottomIndex, onTop);
            notify("hier.sequence");
            if (DEBUG.PARENTING) System.out.println("ensurePaintSequence: inserted " + onTop);
        } else {
            //if (DEBUG.PARENTING) System.out.println("ensurePaintSequence: already sequenced");
        }
        //if (DEBUG.PARENTING) System.out.println("ensurepaintsequence: " + onBottom + " " + onTop);
        
    }
    
   
    /* for use during restore
       // now handled in LWMap.completeXMLRestore
    protected void setChildScaleValues()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(c.getScale());
        }
    }
    */
    
    @Override
    void setScale(double scale)
    {
        //System.out.println("Scale set to " + scale + " in " + this);
        
        super.setScale(scale);

        for (LWComponent c : getChildList()) 
            setScaleOnChild(scale, c);

        layoutChildren(); // we do this for our rollover zoom hack so children are repositioned
    }

    void setScaleOnChild(double scale, LWComponent c)
    {
        // need this for undo of dropping a node into another node: when re-parented
        // back to the map, it needs to get it's default scale back.
        c.setScale(scale);
        
//         // vanilla containers don't scale down their children -- only nodes do
//         if (VUE.RELATIVE_COORDS)
//             ; //throw new Error("relative coordinate impl doesn't apply parent scale to child scale");
//         else
//             c.setScale(scale);
    }

    /**
     * Default impl just fills the background and draws any children.
     */
    protected void drawImpl(DrawContext dc)
    {
        if (!isTransparent()) {
            dc.g.setColor(getFillColor());
            dc.g.fill(getLocalShape());
        }
        
        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(getLocalShape());
        }
        
        drawChildren(dc);
    }

    protected void drawChildren(DrawContext dc)
    {
        if (this.children.size() <= 0)
            return;

        if (!VUE.RELATIVE_COORDS && !hasAbsoluteMapLocation()) {
            // restore us to absolute map coords for drawing the children
            // if we were made relative
            
            // TODO: change to a straight inversion of the local transform,
            // or create a transformLocalInverse
            // Actually: BETTER: keep a saveTransform we can simply
            // restore -- either in the LWContainer, or the DrawContext
            /*
            try {
                dc.g.transform(getLocalTransform().createInverse());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            */
            
            if (getScale() != 1f) {
                double scaleInverse = 1.0 / getScale();
                dc.g.scale(scaleInverse, scaleInverse);
            }
            dc.g.translate(-getX(), -getY());
        }
        

        int nodes = 0;
        int links = 0;
        int images = 0;
        int other = 0;
        
        /*
        final Rectangle2D clipBounds;
        final Shape clip = dc.g.getClip();
        if (clip instanceof Rectangle2D) {
            clipBounds = (Rectangle2D) clip;
            if (DEBUG.PAINT) System.out.println("CLIPBOUNDS=" + Util.out(clipBounds) + " for " + this);
            //System.out.println("      mvrr="+MapViewer.RepaintRegion);
        } else {
            clipBounds = dc.g.getClipBounds();
            if (DEBUG.PAINT || DEBUG.CONTAINMENT) {
                out("CURRENT SHAPE CLIP: " + clip + " for " + this);
                out("        CLIPBOUNDS: " + Util.out(clipBounds));
            }

            // fudge clip bounds to deal with anti-aliasing
            // edges that are being missed (only at the top level)
//               if (dc.focal == this) {
//               if (clipBounds != null)
//               clipBounds.grow(1,1);
//               }
        }
        */

                
        LWComponent focused = null;
        for (LWComponent c : getChildList()) {

            // make sure the rollover is painted on top
            // a bit of a hack to do this here -- better MapViewer
            //if (c.isRollover() && c.getParent() instanceof LWNode) {
            if (c.isZoomedFocus()) {
                focused = c;
                continue;
            }

            //-------------------------------------------------------
            // This is a huge speed optimzation.  Eliminating all
            // the Graphics2D calls that would end up having to
            // check the clipBounds internally makes a giant
            // difference.
            // -------------------------------------------------------

            // if filtered, don't draw, unless has children, in which case
            // we need to draw just in case any of the children are NOT filtered.
//             if (!c.isVisible() || (c.isFiltered() && !c.hasChildren()))
//                 continue;
//             if (c.getLayer() > dc.getMaxLayer())
//                 continue;

            if (!c.requiresPaint(dc))
                continue;

            drawChildSafely(dc, c);
            
            if (DEBUG.PAINT) {
                if (c instanceof LWLink) links++;
                else if (c instanceof LWNode) nodes++;
                else if (c instanceof LWImage) images++;
                else other++;
            }
        }

        if (focused != null) {
            setFocusComponent(focused);
            drawChildSafely(dc, focused);
        } else
            setFocusComponent(null);
                
        if (DEBUG.PAINT && (DEBUG.META || this instanceof LWMap)) 
            System.out.println("PAINTED " + links + " links, " + nodes + " nodes, " + images + " images, " + other + " other; for " + this);
    }

    private void drawChildSafely(DrawContext _dc, LWComponent c)
    {
        // todo opt: don't create all these GC's?
        // todo: if selection going to draw in map, consolodate it here!
        // todo: same goes for pathway decorations!
        final DrawContext dc = _dc.create();
        try {
            drawChild(c, dc);
        } catch (Throwable t) {
            synchronized (System.err) {
                tufts.Util.printStackTrace(t);
                System.err.println("*** Exception drawing: " + c);
                System.err.println("***         In parent: " + this);
                System.err.println("***    Graphics-start: " + _dc.g);
                System.err.println("***      Graphics-end: " + dc.g);
                System.err.println("***   Transform-start: " + _dc.g.getTransform());
                System.err.println("***     Transform-end: " + dc.g.getTransform());
                System.err.println("***              clip: " + dc.g.getClip());
                System.err.println("***        clipBounds: " + dc.g.getClipBounds());
            }
        } finally {
            dc.g.dispose();
        }
    }

    

    protected void drawChild(LWComponent child, DrawContext dc)
    {
        /*
        if (child.hasAbsoluteMapLocation()) {
            child.draw(dc);
            return;
        }
        dc.g.translate(child.getX(), child.getY());
        if (child.getScale() != 1f) {
            final float scale = child.getScale();
            dc.g.scale(scale, scale);
        }
        */

        /*
          moved to LWComponent.draw, so random use of LWComponent.draw will use it's location if it has one
        if (VUE.RELATIVE_COORDS) {
            if (child.hasAbsoluteMapLocation())
                dc.resetMapDrawing();
            else// this will cascade to all children when they draw, combining with their calls to transformRelative
                child.transformRelative(dc.g);
        } else {
            // this will be reset here for each child
            child.transformLocal(dc.g);
        }
        */

        if (hasAbsoluteChildren()) {
            child.draw(dc);
        } else {
            child.drawInParent(dc);
        }
    }

    /**
     * Be sure to duplicate all children and set parent/child references,
     * and if we weren't given a LinkPatcher, to patch up any links
     * among our children.
     */
    public LWComponent duplicate(CopyContext cc)
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
        
        LWContainer containerCopy = (LWContainer) super.duplicate(cc);

        if (cc.dupeChildren) {
            for (LWComponent c : getChildList()) {
                LWComponent childCopy = c.duplicate(cc);
                containerCopy.children.add(childCopy);
                childCopy.setParent(containerCopy);
            }
        }

        if (isPatcherOwner)
            cc.patcher.reconnectLinks();
            
        return containerCopy;
    }

    public String paramString()
    {
        if (children != null && children.size() > 0)
            return super.paramString() + " chld=" + children.size();
        else
            return super.paramString();
            
    }
    
    
}
