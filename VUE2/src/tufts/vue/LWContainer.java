package tufts.vue;

import java.util.List;
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
 * LWContainer.java
 *
 * Manage a group of children within a parent that maintains it's size.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public abstract class LWContainer extends LWComponent
{
    protected List children = new java.util.ArrayList();
    protected LWComponent focusComponent;
    
    /** for use during restore */
    private int idStringToInt(String idStr)
    {
        int id = -1;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            System.err.println(e + " invalid ID: '" + idStr + "'");
            e.printStackTrace();
        }
        return id;
    }
    /** for use during restore */
    protected LWComponent findChildByID(String ID)
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getID().equals(ID))
                return c;
            if (c instanceof LWContainer) {
                c = ((LWContainer)c).findChildByID(ID);
                if (c != null)
                    return c;
            }
        }
        return null;
    }

    /**
     * For restore: To be called once after a persisted map
     * is restored.
     */
    protected void resolvePersistedLinks(LWContainer topLevelContainer)
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWContainer) {
                ((LWContainer)c).resolvePersistedLinks(topLevelContainer);
                continue;
            }
            if (!(c instanceof LWLink))
                continue;
            LWLink l = (LWLink) c;
            try {
                String ep1ID = l.getEndPoint1_ID();
                String ep2ID = l.getEndPoint2_ID();
                if (ep1ID != null) l.setComponent1(topLevelContainer.findChildByID(ep1ID));
                if (ep2ID != null) l.setComponent2(topLevelContainer.findChildByID(ep2ID));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("*** bad link? " + l);
                // we can now have links without endpoints
                //i.remove();
            }
        }
    }
    
    /** for use during restore */
    protected int findGreatestChildID()
    {
        int maxID = -1;
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.getID() == null) {
                System.err.println("*** FOUND LWC WITH NULL ID " + c + " (reparent to fix)");
                continue;
            }
            int curID = idStringToInt(c.getID());
            if (curID > maxID)
                maxID = curID;
            if (c instanceof LWContainer) {
                curID = ((LWContainer)c).findGreatestChildID();
                if (curID > maxID)
                    maxID = curID;
            }
        }
        return maxID;
    }
        
    /** for use during restore */
    protected void setChildParentReferences()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setParent(this);
            if (c instanceof LWContainer)
                ((LWContainer)c).setChildParentReferences();
        }
    }

    /** for use during restore */
    protected void setChildScaleValues()
    {
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(c.getScale());
        }
    }
    
    /*
     * Child handling code
     */
    public boolean hasChildren()
    {
        return children != null && children.size() > 0;
    }
    public List getChildList()
    {
        return this.children;
    }
    public java.util.Iterator getChildIterator()
    {
        return this.children.iterator();
    }

    public Iterator getNodeIterator()
    {
        return getNodeList().iterator();
    }
    public Iterator getLinkIterator()
    {
        return getLinkList().iterator();
    }
    /*
    public Iterator getPathwayIterator()
    {
        return getPathwayList().iterator();
    }*/
    
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
    /* -- pathways are no longer added as children to the map --
    private List getPathwayList()
    {
        ArrayList list = new ArrayList();
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWPathway)
                list.add(c);
        }
        return list;
    }*/

    protected String getNextUniqueID()
    {
        if (getParent() == null)
            throw new IllegalStateException("LWContainer needs a parent subclass of LWContainer that implements getNextUniqueID: " + this);
        else
            return getParent().getNextUniqueID();
    }

    /** In case the container subclass can do anything to lay out it's children
     *  (e.g., so that LWNode can override & handle chil layout).
     */
    void layoutChildren() { }


    /**
     * Make sure this LWComponent has an ID -- will have an effect on
     * on any brand new LWComponent exactly once per VM instance.
     */
    private void ensureID(LWComponent c)
    {
        if (c.getID() == null)
            c.setID(getNextUniqueID());

        if (c instanceof LWContainer) {
            Iterator i = ((LWContainer)c).getChildIterator();
            while (i.hasNext())
                ensureID((LWComponent) i.next());
        }
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
        newParent.addChildren(reparenting.iterator());
    }

    final void addChild(LWComponent c) {
        addChildren(new VueUtil.SingleIterator(c));
    }
    final void removeChild(LWComponent c) {
        removeChildren(new VueUtil.SingleIterator(c));
    }

    public void addChildren(Iterator i)
    {
        notify(LWKey.HierarchyChanging);
        
        ArrayList addedChildren = new ArrayList();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            addChildInternal(c);
            addedChildren.add(c);
        }

        // need to do this afterwords so everyone has a parent to check
        Iterator in = addedChildren.iterator();
        while (in.hasNext())
            ensureLinksPaintOnTopOfAllParents((LWComponent) in.next());
        
        if (addedChildren.size() > 0) {
            notify(LWKey.ChildrenAdded, addedChildren);
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
                removeChildInternal(c);
                removedChildren.add(c);
            } else
                throw new IllegalArgumentException(this + " asked to remove child it doesn't own: " + c);
        }
        if (removedChildren.size() > 0) {
            notify(LWKey.ChildrenRemoved, removedChildren);
            layout();
        }
    }
    
    protected void addChildInternal(LWComponent c)
    {
        if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] ADDING   " + c);
        if (c.getParent() != null && c.getParent().getChildList().contains(c)) {
            //if (DEBUG.PARENTING) System.out.println("["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent());
            if (DEBUG.PARENTING)
                new Throwable("["+getLabel() + "] auto-deparenting " + c + " from " + c.getParent()).printStackTrace();
                //out(this + " auto-deparenting " + c + " from " + c.getParent()).printStackTrace();
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
        ensureID(c);
    }
    
    protected void removeChildInternal(LWComponent c)
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
        if (true||DEBUG.PARENTING) System.out.println("["+getLabel() + "] DELETING PERMANENTLY " + c);

        // We did the "deleting" notification first, so anybody
        // listening can still see the node in it's full current state
        // before anything changes.  But children now keep their
        // parent reference until their removed from the model, so the
        // only thing different when removeFromModel issues it's
        // LWKey.Deleting event is the parent won't list it as a
        // child, but since it still has the parent ref, event
        // up-notification will still work, which is good enough.
        // (It's probably not safe to deliver more than one
        // LWKey.Deleting event -- if need to put it back here, have
        // to be able to tell removeFromModel optionally not to issue
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
        java.util.Iterator i = component.getLinkRefs().iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            LWContainer parent1 = null;
            LWContainer parent2 = null;
            if (l.getComponent1() != null)
                parent1 = l.getComponent1().getParent();
            if (l.getComponent2() != null)
                parent2 = l.getComponent2().getParent();
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
            System.err.println("  ep1 parent " + l.getComponent1().getParent());
            System.err.println("  ep2 parent " + l.getComponent2().getParent());
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
    */

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
    
    public java.util.List getAllDescendents()
    {
        java.util.List list = new java.util.ArrayList();
        list.addAll(children);
        java.util.Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWContainer)
                list.addAll(((LWContainer)c).getAllDescendents());
        }
        return list;
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
            if (c instanceof LWContainer && !(c instanceof LWGroup))
                list.addAll(((LWContainer)c).getAllDescendents());
        }
        return list;
    }

    public void setFocusComponent(LWComponent c)
    {
        this.focusComponent = c;
    }
    
    public LWComponent findChildAt(Point2D p)
    {
        return findChildAt((float)p.getX(), (float)p.getY());
    }


    /*
     * Find child at mapX, mapY -- may actually return this component also.
     */
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
                if (c.hasChildren())
                    return ((LWContainer)c).findChildAt(mapX, mapY);
                else 
                    return c.isFiltered() ? null : c;
            }
        }
        // we check curved links last because they can take up so much
        // hit-space (the entire interior of their arc)
        for (Iterator i = curvedLinks.iterator(); i.hasNext();) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(mapX, mapY))
                return c.isFiltered() ? null : c;
        }
        return defaultHitComponent();
    }

    /** Code is mostly duplicated from above here, but subclasses (e.g.,
     * LWGroup) handle this differently, and we can't reuse
     * above code due to recursive usage.
     */

    /*
     * Find deepest child at mapX, mapY -- may actually return this component also.
     */
    public LWComponent findDeepestChildAt(float mapX, float mapY, LWComponent excluded)
    {
        if (DEBUG.CONTAINMENT) System.out.println("LWContainer.findDeepestChildAt[" + getLabel() + "]");
        if (DEBUG.CONTAINMENT && focusComponent != null) System.out.println("\tfocusComponent=" + focusComponent);

        // TODO: change this gross focusComponent hack to a cleaner special case:
        // have the entire LWMap maintain a list of all the current focus components,
        // (the deepest + all it's parents) and always check that first & no matter what
        if (focusComponent != null && focusComponent.contains(mapX, mapY))
            return focusComponent.findDeepestChildAt(mapX, mapY, excluded);
        
        // hit detection must traverse list in reverse as top-most
        // components are at end
        
        curvedLinks.clear();
        
        for (ListIterator i = children.listIterator(children.size()); i.hasPrevious();) {
            LWComponent c = (LWComponent) i.previous();
            if (c == excluded)
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
                    return container.focusComponent.findDeepestChildAt(mapX, mapY, excluded);
            }
            if (c.contains(mapX, mapY))
                return c.findDeepestChildAt(mapX, mapY, excluded);
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

    /*
    public LWComponent findDeepestChildAt(float mapX, float mapY, LWComponent excluded)
    {
        if (DEBUG.CONTAINMENT) System.out.println("LWContainer.findDeepestChildAt[" + getLabel() + "]");
        if (DEBUG.CONTAINMENT && focusComponent != null) System.out.println("\tfocusComponent=" + focusComponent);
        if (focusComponent != null && focusComponent.contains(mapX, mapY)) {
            if (focusComponent instanceof LWContainer)
                return ((LWContainer)focusComponent).findDeepestChildAt(mapX, mapY, excluded);
            else
                return focusComponent;
        }
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c == excluded)
                continue;
            boolean childHasFocusComponent = false;
            if (c instanceof LWContainer) {
                LWContainer container = (LWContainer) c;
                // focus component may temporarily extend outside the bounds
                // of it's parent, so we have to check them manually
                //childHasFocusComponent = (container.focusComponent != null);
                if (container.focusComponent != null && container.focusComponent.contains(mapX, mapY))
                    return container.focusComponent.findDeepestChildAt(mapX, mapY, excluded);
            }
            //if (childHasFocusComponent || c.contains(mapX, mapY)) {
            if (c.contains(mapX, mapY)) {
                //return c.findDeepestChildAt(mapX, mapY, excluded);
                if (c.hasChildren())
                    return ((LWContainer)c).findDeepestChildAt(mapX, mapY, excluded);
                else
                    return c;
            }
        }
        return defaultHitComponent();
    }
    */

    protected LWComponent defaultHitComponent()
    {
        return (isHidden() || isFiltered()) ? null : this;
    }

    public LWComponent findDeepestChildAt(float mapX, float mapY)
    {
        return findDeepestChildAt(mapX, mapY, null);
    }
        
    public LWComponent findDeepestChildAt(Point2D p)
    {
        return findDeepestChildAt((float)p.getX(), (float)p.getY(), null);
    }

    
    //    public LWNode findLWNodeAt(float mapX, float mapY, LWComponent excluded)
    public LWNode findLWNodeAt(float mapX, float mapY)
    {
        // hit detection must traverse list in reverse as top-most
        // components are at end
        
        if (children.size() > 0) {
            java.util.ListIterator i = children.listIterator(children.size());
            while (i.hasPrevious()) {
                LWComponent c = (LWComponent) i.previous();
                if (c.isHidden())
                    continue;
                if (!(c instanceof LWNode))
                    continue;
                //if (c != excluded && c.contains(mapX, mapY)) {
                // todo: why do skip if node is selected here???
                // was a reason once but now I think this is a bug
                // OR: was this to make sure we never found
                // the node we're dragging itself?
                if (!c.isSelected() && c.contains(mapX, mapY)) {
                    if (c.hasChildren())
                        return ((LWContainer)c).findLWNodeAt(mapX, mapY);
                    else
                        return (LWNode) c;
                }
            }
        }
        if (this instanceof LWNode)
            return (LWNode) this;
        else
            return null;
    }

    public boolean isOnTop(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return getIndex(c) == children.size()-1;
    }
    public boolean isOnBottom(LWComponent c)
    {
        // todo opt: has to on avg scan half of list every time
        // (will slow down selection in checks to enable front/back actions)
        return getIndex(c) == 0;
    }

    public int getLayer(LWComponent c)
    {
        return getIndex(c);
    }
    private int getIndex(Object c)
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

    protected static final Comparator ForwardOrder = new Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                return c2.getParent().getIndex(c2) - c1.getParent().getIndex(c1);
            }};
    protected static final Comparator ReverseOrder = new Comparator() {
            public int compare(Object o1, Object o2) {
                LWComponent c1 = (LWComponent) o1;
                LWComponent c2 = (LWComponent) o2;
                return c1.getParent().getIndex(c1) - c2.getParent().getIndex(c2);
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
        notify(LWKey.HierarchyChanging);
        //System.out.println("bringToFront " + c);
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
        notify(LWKey.HierarchyChanging);
        //System.out.println("sendToBack " + c);
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
        notify(LWKey.HierarchyChanging);
        //System.out.println("bringForward " + c);
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
        notify(LWKey.HierarchyChanging);
        //System.out.println("sendBackward " + c);
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
        int bottomIndex = getIndex(onBottom);
        int topIndex = getIndex(onTop);
        if (bottomIndex < 0 || topIndex < 0)
            throw new IllegalStateException(this + "ensurePaintSequence: both aren't in list! " + bottomIndex + " " + topIndex);
        //if (DEBUG.PARENTING) System.out.println("ENSUREPAINTSEQUENCE: " + onBottom + " " + onTop);
        if (topIndex == (bottomIndex - 1)) {
            swap(topIndex, bottomIndex);
            notify("hier.sequence");
            if (DEBUG.PARENTING) System.out.println("ensurePaintSequence: swapped " + onTop);
        } else if (topIndex < bottomIndex) {
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
    
    public void replaceResource(Resource r1,Resource r2) {
        Iterator i = getChildIterator();
        while(i.hasNext()) {
            LWComponent component = (LWComponent) i.next();
            if(component.hasResource()){
                Resource resource = component.getResource();
                if(resource == r1) 
                    component.setResource(r2);
            }
            if(component instanceof LWContainer)
                replaceResource(r1,r2);
        }

  
    }

    void setScale(float scale)
    {
        //System.out.println("Scale set to " + scale + " in " + this);
        super.setScale(scale);
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            setScaleOnChild(scale, c);
        }
        //layout(); // okay, we were calling this here as a side effect for
        // everyone to compute their proper sizes as this gets called after a restore --
        // now handled properly in LWMap
        layoutChildren(); // we do this for our rollover zoom hack so children are repositioned
    }

    void setScaleOnChild(float scale, LWComponent c)
    {
        // vanilla containers don't scale down their children -- only nodes do
        c.setScale(scale);
    }

    public void draw(DrawContext dc)
    {
        drawChildren(dc);
    }

    public void drawChildren(DrawContext dc)
    {
        int nodes = 0;
        int links = 0;
        
        if (this.children.size() > 0) {

            Rectangle clipBounds = dc.g.getClipBounds();

            // fudge clip bounds to deal with anti-aliasing
            // edges that are being missed.
            // TODO: so this is growing every time we descend into
            // a container?  We only want to do this at the LWMap level...
            clipBounds.grow(1,1);
            
            /*if (false) {
                System.out.println("DRAWING " + this);
                System.out.println("clipBounds="+clipBounds);
                System.out.println("      mvrr="+MapViewer.RepaintRegion);
                }*/
                
            LWComponent focused = null;
            java.util.Iterator i = getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();

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
                if (c.isVisible() && (!c.isFiltered() || c.hasChildren()) && c.intersects(clipBounds)) {
                    _drawChild(dc, c);
                    if (MapViewer.DEBUG_PAINT) { // todo: remove MapViewer reference
                        if (c instanceof LWLink) links++;
                        else if (c instanceof LWNode) nodes++;
                    }
                }
            }

            if (focused != null) {
                setFocusComponent(focused);
                _drawChild(dc, focused);
            } else
                setFocusComponent(null);
                
            if (MapViewer.DEBUG_PAINT) // todo: remove MapViewer reference
                System.out.println(this + " painted " + links + " links, " + nodes + " nodes");
        }
        if (DEBUG.CONTAINMENT) {
            dc.g.setColor(java.awt.Color.green);
            dc.g.setStroke(STROKE_ONE);
            dc.g.draw(getBounds());
        }
    }

    private void _drawChild(DrawContext dc, LWComponent c)
    {
        // todo opt: don't create all these GC's?
        Graphics2D g = dc.g;
        Graphics2D gg = (Graphics2D) dc.g.create();
        try {
            if (c.doesRelativeDrawing())
                gg.translate(c.getX(), c.getY());
            dc.g = gg;
            drawChild(c, dc);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("*** Exception drawing: " + c);
            System.err.println("***         In parent: " + this);
            System.err.println("***    Graphics-start: " + g);
            System.err.println("***      Graphics-end: " + gg);
            System.err.println("***   Transform-start: " + g.getTransform());
            System.err.println("***     Transform-end: " + gg.getTransform());
        } finally {
            gg.dispose();
        }
        dc.g = g;
    }

    

    public void drawChild(LWComponent child, DrawContext dc)
    {
        child.draw(dc);
    }

    public LWComponent duplicate()
    {
        LWContainer containerCopy = (LWContainer) super.duplicate();
        
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            LWComponent childCopy = c.duplicate();
            containerCopy.children.add(childCopy);
            childCopy.setParent(containerCopy);
        }
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
