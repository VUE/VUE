 package tufts.vue;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.geom.Rectangle2D;

/**
 * LWGroup.java
 *
 * Manage a group of LWComponents -- does not have a fixed
 * shape or paint any backround or border -- simply used
 * for moving, resizing & layering a collection of objects
 * together.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public final class LWGroup extends LWContainer
{
    public LWGroup() {}

    public boolean supportsUserResize() {
        return true;
    }
    
    /**
     * For the viewer selection code -- we're mainly interested
     * in the ability of a group to move all of it's children
     * with it.
     */
    void useSelection(LWSelection selection)
    {
        Rectangle2D bounds = selection.getBounds();
        if (bounds != null) {
            super.setSize((float)bounds.getWidth(),
                          (float)bounds.getHeight());
            super.setLocation((float)bounds.getX(),
                              (float)bounds.getY());
        }  else {
            System.err.println("null bounds in LWGroup.useSelection");
        }
        super.children = selection;
    }

    /**
     * Create a new LWGroup, reparenting all the LWComponents
     * in the selection to the new group.
     */
    static LWGroup create(java.util.List selection)
    {
        LWGroup group = new LWGroup();

        /*
        // Track what links are explicitly in the selection so we
        // don't "grab" them later even if both endpoints are in the
        // selection.
        HashSet linksInSelection = new HashSet();
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink)
                linksInSelection.add(c);
        }
        */

        // "Grab" links -- automatically add links
        // to this group who's endpoints are BOTH
        // also being added to the group.

        // todo: odd case -- create a link tween 2 grouped items,
        // and the new link will NOT be in the group -- how
        // concerned to we need to be? "problem" arises if you
        // then dropped the grouped 2+ nodes into a parent, and
        // the parent was on a higher layer than the link -- the
        // link would "dissapear" behind the parent.
        
        HashSet linkSet = new HashSet();
        List moveList = new java.util.ArrayList();
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink) // the only links allowed are ones we grab
                continue;
            moveList.add(c);
            //-------------------------------------------------------
            // If both ends of any link are in the selection,
            // also add those links as children of the group.
            //-------------------------------------------------------
            // todo: need to check all descendents: LWContainer.getAllDescedents
            Iterator li = c.getLinkRefs().iterator();
            //todo: need to recursively check children for link refs also
            while (li.hasNext()) {
                LWLink l = (LWLink) li.next();
                //if (linksInSelection.contains(l))
                //continue;
                if (!linkSet.add(l)) {
                    if (DEBUG_PARENTING) System.out.println("["+group.getLabel() + "] GRABBING " + c + " (both ends in group)");
                    moveList.add(l);
                }
            }
        }
        // Be sure to preserve the current relative ordering of all
        // these components the new group
        LWComponent[] comps = sort(moveList, ReverseOrder);
        for (int x = 0; x < comps.length; x++) {
            group.addChildInternal(comps[x]);
        }
        group.setSizeFromChildren();
        // todo: catch any child size change events (e.g., due to font)
        // so we can recompute our bounds
        return group;
    }
    
    /**
     * "Borrow" the children in the list for the sole
     * purpose of computing total bounds and moving
     * them around en-mass -- used for dragging a selection.
     * Does NOT reparent the components in any way.
     */
    static LWGroup createTemporary(java.util.ArrayList selection)
    {
        //LWGroup group = new LWGroup(selection);
        LWGroup group = new LWGroup();
        group.children = (java.util.ArrayList) selection.clone();
        group.setSizeFromChildren();
        if (DEBUG_CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
        return group;
    }

    /** Set size & location of this group based on LWComponents in selection */
    /*
    private LWGroup(java.util.List selection)
    {
        Rectangle2D bounds = LWMap.getBounds(selection.iterator());
        super.setSize((float)bounds.getWidth(),
                      (float)bounds.getHeight());
        super.setLocation((float)bounds.getX(),
                          (float)bounds.getY());
    }
    */
    
    private void setSizeFromChildren()
    {
        Rectangle2D bounds = LWMap.getBounds(getChildIterator());
        super.setSize((float)bounds.getWidth(),
                      (float)bounds.getHeight());
        super.setLocation((float)bounds.getX(),
                          (float)bounds.getY());
    }
    
    public void setZoomedFocus(boolean tv)
    {
        // the group object never takes zoomed focus
    }


    /**
     * Remove all children and re-parent to this group's parent,
     * then remove this now empty group object from the parent.
     */
    public void disperse()
    {
        // todo: turn off all events while this reorg is happening?
        // todo: better to insert all the children back into the
        //      parent at the layer of group object...

        if (DEBUG_PARENTING) System.out.println("dispersing group " + this);

        ArrayList children = getChildList();
        Iterator i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // set parent to null first so that addChild
            // isn't calling us back with a remove child,
            // doing needless work and concurrently modifying
            // our iteration!
            c.setParent(null);
            getParent().addChildInternal(c);
        }
        i = children.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.notify("added", getParent());
        }
        getParent().notify("childrenAdded", children);
        getParent().deleteChildPermanently(this);
    }

    public String getLabel()
    {
        if (super.getLabel() == null)
            return "[LWGroup #" + getID() + " nChild=" + (children==null?-1:children.size()) + "]";
        else
            return super.getLabel();
    }

    /** groups are transparent -- defer to parent for background fill color */
    public java.awt.Color getFillColor()
    {
        return getParent() == null ? null : getParent().getFillColor();
    }

    public void setLocation(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();
        //System.out.println(getLabel() + " setLocation");
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();

            boolean inGroup = c.getParent() instanceof LWGroup;
                
            // If parent and some child both in selection and you
            // drag, the selection (an LWGroup) and the parent fight
            // to control the location of the child.  There may be a
            // cleaner way to handle this, but checking it here works.
            // Also, do NOT skip if we're in a group -- that condition
            // is caught below.
            if (!inGroup && c.isSelected() && c.getParent().isSelected())
                continue;
            
            // If this component is part of the "temporary" group for the selection,
            // don't move it -- only it's real parent group should reposition it.
            // (The only way we could be here without "this" being the parent is
            // if we're in the MapViewer special use draggedSelectionGroup)
            if (inGroup && c.getParent() != this)
                continue;

            c.translate(dx, dy);
        }
        super.setLocation(x, y);

        // todo: possibly redesign rendering to happen node-local
        // so don't have to do all this
    }
    
    // todo: Can't sanely support scaling of a group because
    // we'd also have to scale the space between the
    // the children -- to make this work we'll have
    // to set the scale for the whole group, draw it
    // (and tell the children NOT to set their scale values
    // individually) and sort out the rest of a mess
    // getting all that to work will imply.
    void setScale(float scale)
    {
        // intercept any attempt at scaling us and turn it off
        super.setScale(1f);
    }

    public void XsetScale(float scale)
    {
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(scale);
        }
        setSizeFromChildren();
    }
    public boolean contains(float x, float y)
    {
        // TODO PERF: track and check group bounds here instead of every component!
        // Right now we're calling contains twice on everything in a group
        // when use findDeepestComponent
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.contains(x, y))
                return true;
        }
        return false;
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        // todo opt: use our cached bounds already computed
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.intersects(rect))
                return true;
        }
        return false;
    }
    
    protected LWComponent defaultHitComponent()
    {
        return null;
    }
    
    /**
     * A hit on any component in the group finds the whole group,
     * not that component.  A hit within the bounds of the entire
     * group but not on any component hits <b>nothing</b>
     */
    public LWComponent findChildAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWGroup.findChildAt " + getLabel());
        // hit detection must traverse list in reverse as top-most
        // components are at end
        //todo: handle focusComponent here?
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.isHidden())
                continue;
            if (c.contains(mapX, mapY))
                return this;
        }
        return null;
    }
    
    /*
    public LWComponent findLWSubTargetAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWGroup.findLWSubTargetAt[" + getLabel() + "]");
        LWComponent c = super.findLWSubTargetAt(mapX, mapY);
        return c == this ? null : c;
        }*/
    
    /** The group itself can never serve as a general target
     * (e.g., for linking to)
     */
    public boolean targetContains(float x, float y)
    {
        return false;
    }

    private static final Rectangle2D EmptyBounds = new Rectangle2D.Float();

    public Rectangle2D getBounds()
    {
        Rectangle2D bounds = null;
        Iterator i = getChildIterator();
        if (i.hasNext()) {
            bounds = new Rectangle2D.Float();
            bounds.setRect(((LWComponent)i.next()).getBounds());
        } else {
            System.out.println(this + " getBounds: EMPTY!");
            return EmptyBounds;
        }

        while (i.hasNext())
            bounds.add(((LWComponent)i.next()).getBounds());
        //System.out.println(this + " getBounds: " + bounds);
        return bounds;
    }

    /*
    public void layout()
    {
        super.layout();
        setFrame(computeBounds());
    }
    */
    
    
    public void draw(java.awt.Graphics2D g)
    {
        if (getStrokeWidth() == -1) { // todo: temporary debug
            System.err.println("hiding " + this);
            return;
        }
        super.draw(g);
        if (isIndicated()) {
            // this shouldn't happen, but just in case...
            if (isIndicated()) {
                g.setColor(COLOR_INDICATION);
                g.setStroke(STROKE_INDICATION);
                g.draw(getBounds());
            }
        }

        if (DEBUG_CONTAINMENT) {
            if (isRollover())
                g.setColor(java.awt.Color.green);
            else
                g.setColor(java.awt.Color.red);
            if (isIndicated())
                g.setStroke(STROKE_INDICATION);
            else
                g.setStroke(STROKE_TWO);
            g.draw(getBounds());
        }
    }
    
    
}
