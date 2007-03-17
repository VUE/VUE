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

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Manage a group of children within a parent.
 *
 * Handle rendering, hit-detection, duplication, adding/removing children.
 *
 * @version $Revision: 1.100 $ / $Date: 2007-03-17 22:31:55 $ / $Author: sfraize $
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


    /**
     * return first ancestor of type clazz, or if no matching ancestor
     * is found, return the upper most ancestor (one that has no
     * parent, which is normally the LWMap)
     */
    public LWContainer getFirstAncestor(Class clazz)
    {
        if (getParent() == null)
            return this;
        else if (clazz.isInstance(getParent()))
            return getParent();
        else
            return getParent().getFirstAncestor(clazz);
    }

    
    /*
    public Iterator getPathwayIterator()
    {
        return getPathwayList().iterator();
    }*/
    

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

    /*
    protected String getNextUniqueID()
    {
        if (getParent() == null)
            throw new IllegalStateException("LWContainer needs a parent subclass of LWContainer that implements getNextUniqueID: " + this);
        else
            return getParent().getNextUniqueID();
    }
    */

    /** In case the container subclass can do anything to lay out it's children
     *  (e.g., so that LWNode can override & handle chil layout).
     */
    void layoutChildren() { }


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
    public void reparentTo(LWContainer newParent, Iterator possibleChildren)
    {
        notify(LWKey.HierarchyChanging);

        List reparenting = new ArrayList();
        while (possibleChildren.hasNext()) {
            LWComponent c = (LWComponent) possibleChildren.next();
            if (c.getParent() == this)
                reparenting.add(c);
        }
        removeChildren(reparenting.iterator());
        newParent.addChildren(reparenting);
    }

    final void addChild(LWComponent c) {
        addChildren(new VueUtil.SingleIterator(c));
    }
    final void removeChild(LWComponent c) {
        removeChildren(new VueUtil.SingleIterator(c));
    }

    /**
     * This will add the given list of children to the LWContainer, and
     * will sort the add list by Y value first in case this container has
     * a controlled layout that is vertical.
     */

    public void addChildren(List addList)
    {
        LWComponent[] toAdd = (LWComponent[]) addList.toArray(new LWComponent[addList.size()]);
        if (this instanceof LWGroup) {
            if (toAdd[0].getParent() == null) {
                // if first item in list has no parent, we assume none do (they're
                // system drag or paste orphans), and we can't sort them based
                // on layer, so we leave them alone for now until all nodes have a z-order
                // value.
            } else {
                java.util.Arrays.sort(toAdd, LayerSorter);
            }
        } else {
            java.util.Arrays.sort(toAdd, LWComponent.YSorter);
        }
        addChildren(new tufts.Util.ArrayIterator(toAdd));
    }
    
    /** If all the children do not have the same parent, the sort order won't be 100% correct. */
    private static final java.util.Comparator LayerSorter = new java.util.Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                LWContainer parent1 = c1.getParent();
                LWContainer parent2 = c2.getParent();

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
    
    public void addChildren(Iterator<LWComponent> i)
    {
        notify(LWKey.HierarchyChanging);
        
        ArrayList<LWComponent> addedChildren = new ArrayList();
        while (i.hasNext()) {
            LWComponent c = i.next();
            addChildImpl(c);
            addedChildren.add(c);
        }

        // need to do this afterwords so everyone has a parent to check
        Iterator<LWComponent> in = addedChildren.iterator();
        while (in.hasNext())
            ensureLinksPaintOnTopOfAllParents(in.next());
        
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
    protected void removeChildren(Iterator i)
    {
        notify(LWKey.HierarchyChanging);

        ArrayList removedChildren = new ArrayList();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getParent() == this) {
                removeChildImpl(c);
                removedChildren.add(c);
            } else
                throw new IllegalArgumentException(this + " asked to remove child it doesn't own: " + c);
        }
        if (removedChildren.size() > 0) {
            notify(LWKey.ChildrenRemoved, removedChildren);
            layout();
        }
    }
    
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
        c.setParent(this);
        c.addNotify(this);
        ensureID(c);
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
        // todo: maybe better to handle this in LWNode somehow as that's only
        // place nodes actually get scaled at all right now -- either that or
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
        // now keep their parent reference until their removed from the model, so the
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
                System.out.println("ELPOTOAP: ignoring link with no parent: " + l);
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
                    System.err.println("note: couldn't find common parent for " + component);
                    new Throwable("ELPOTOAP debug (ignorable stack trace)").printStackTrace();
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

    protected LWComponent defaultPick(PickContext pc)
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
    
    void setScale(float scale)
    {
        //System.out.println("Scale set to " + scale + " in " + this);
        
        super.setScale(scale);

        for (LWComponent c : this.children) 
            setScaleOnChild(scale, c);

        layoutChildren(); // we do this for our rollover zoom hack so children are repositioned
    }

    void setScaleOnChild(float scale, LWComponent c)
    {
        // vanilla containers don't scale down their children -- only nodes do
        c.setScale(scale);
    }

    protected void drawImpl(DrawContext dc)
    {
        if (getFillColor() != null) {
            dc.g.setColor(getFillColor());
            dc.g.fill(getShape());
        }
        drawChildren(dc);
    }

    public void drawChildren(DrawContext dc)
    {
        int nodes = 0;
        int links = 0;
        int images = 0;
        
        if (this.children.size() > 0) {

            Rectangle clipBounds = dc.g.getClipBounds();

            // fudge clip bounds to deal with anti-aliasing
            // edges that are being missed.
            // TODO: so this is growing every time we descend into
            // a container?  We only want to do this at the LWMap level...
            if (clipBounds != null)
                clipBounds.grow(1,1);
            
            if (DEBUG.PAINT) {
                //System.out.println("DRAWING " + this);
                out("drawChildren: clipBounds="+clipBounds);
                //System.out.println("      mvrr="+MapViewer.RepaintRegion);
            }
                
            LWComponent focused = null;
            for (LWComponent child : getChildList()) {

                final LWComponent c = child.getView();

                // make sure the rollover is painted on top
                // a bit of a hack to do this here -- better MapViewer
                //if (c.isRollover() && c.getParent() instanceof LWNode) {
                if (c.isZoomedFocus()) {
                    focused = c;
                    continue;
                }

                if (c != child) c.setLocation(child.getX(), child.getY());
                
                //-------------------------------------------------------
                // This is a huge speed optimzation.  Eliminating all
                // the Graphics2D calls that would end up having to
                // check the clipBounds internally makes a giant
                // difference.
                // -------------------------------------------------------
                
                // if filtered, don't draw, unless has children, in which case
                // we need to draw just in case any of the children are NOT filtered.
                if (c.isVisible()
                    && (!c.isFiltered() || c.hasChildren())
                    && c.getLayer() <= dc.getMaxLayer()
                    && (clipBounds == null || c.intersects(clipBounds))
                    )
                {
                    drawChildSafely(dc, c);
                    if (DEBUG.PAINT) {
                             if (c instanceof LWLink) links++;
                        else if (c instanceof LWNode) nodes++;
                        else if (c instanceof LWImage) images++;
                    }
                }
            }

            if (focused != null) {
                setFocusComponent(focused);
                drawChildSafely(dc, focused);
            } else
                setFocusComponent(null);
                
            if (DEBUG.PAINT) 
                out(this + " painted " + links + " links, " + nodes + " nodes, " + images + " images");
        }
        /*
        if (DEBUG.CONTAINMENT) {
            dc.g.setColor(java.awt.Color.green);
            dc.g.setStroke(STROKE_ONE);
            dc.g.draw(getBounds());
        }
        */
    }

    private void drawChildSafely(DrawContext _dc, LWComponent c)
    {
        // todo opt: don't create all these GC's?
        // todo: if selection going to draw in map, consolodate it here!
        // todo: same goes for pathway decorations!
        DrawContext dc = _dc.create();
        try {
            if (c.doesRelativeDrawing())
                dc.g.translate(c.getX(), c.getY());
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

    

    public void drawChild(LWComponent child, DrawContext dc)
    {
        /*
          // this works to draw w/all children & coords scaled
        if (child.getScale() != 1f) {
            final float scaleX, scaleY;
            scaleX = scaleY = child.getScale();
            //dc = new DrawContext(dc);
            child.out("scaled to: " + scaleX);
            dc.g.scale(scaleX, scaleY);
        }
        */
        
        child.draw(dc);
    }

    /**
     * Be sure to duplicate all children and set parent/child references,
     * and if we weren't given a LinkPatcher, to patch up any links
     * among our children.
     */
    public LWComponent duplicate(LinkPatcher linkPatcher)
    {
        boolean isPatcherOwner = false;
        
        if (linkPatcher == null && hasChildren()) {

            // Normally VUE Actions (e.g. Duplicate, Copy, Paste)
            // provide a patcher for duplicating a selection of
            // objects, but anyone else may not have provided one.
            // This will take care of arbitrary single instances of
            // duplication, including duplicating an entire Map.
            
            linkPatcher = new LinkPatcher();
            isPatcherOwner = true;
        }
        
        LWContainer containerCopy = (LWContainer) super.duplicate(linkPatcher);
        
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            LWComponent childCopy = c.duplicate(linkPatcher);
            containerCopy.children.add(childCopy);
            childCopy.setParent(containerCopy);
        }

        if (isPatcherOwner)
            linkPatcher.reconnectLinks();
            
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
