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

        List children = getChildList();
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
        getParent().notify("childrenAdded", this);
        getParent().deleteChildPermanently(this);
    }

    public String getLabel()
    {
        if (super.getLabel() == null)
            return "[LWGroup #" + getID() + " nChild=" + children.size() + "]";
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

            // If parent and some child both in selection and you
            // drag, the selection (an LWGroup) and the parent fight
            // to control the location of the child.  There may be a
            // cleaner way to handle this, but checking it here works.
            // TODO BUG: when we drop in above case, the children
            // think they've been seperatly dragged, and thus get
            // "dropped" out of the parent!
            
            if (c.isSelected() && c.getParent().isSelected())
                continue;
            
            c.translate(dx, dy);
        }
        super.setLocation(x, y);

        // todo: possibly redesign rendering to happen node-local
        // so don't have to do all this
    }
    
    // todo: Can't sanely support scaling of a group because
    // we'd also have to scale the space between the
    // the children, and scale is already a hack feature
    // that I'm hoping we'll be able to make go away.
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
    
    /**
     * A hit on any component in the group finds the whole group,
     * not that component.  A hit within the bounds of the entire
     * group but not on any component hits <b>nothing</b>
     */
    public LWComponent findLWComponentAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWGroup.findLWComponentAt " + getLabel());
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
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
            g.setColor(java.awt.Color.red);
            if (isIndicated())
                g.setStroke(STROKE_INDICATION);
            else
                g.setStroke(STROKE_TWO);
            g.draw(getBounds());
        }
    }
    
    
}
