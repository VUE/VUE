package tufts.vue;

import java.util.List;
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
public class LWGroup extends LWContainer
{
    /** Set size & location of this group based on LWComponents in selection */
    private LWGroup(java.util.List selection)
    {
        Rectangle2D bounds = Vue2DMap.getBounds(selection.iterator());
        super.setSize((float)bounds.getWidth(),
                      (float)bounds.getHeight());
        super.setLocation((float)bounds.getX(),
                          (float)bounds.getY());
    }

    /** to support restore only */
    public LWGroup() {}

    /**
     * Create a new LWGroup, reparenting all the LWComponents
     * in the selection to the new group.
     */
    static LWGroup create(java.util.List selection)
    {
        LWGroup group = new LWGroup(selection);
        // todo: turn off all events while this reorg is happening?
        // Now grab all the children
        Iterator i = selection.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            group.addChildInternal(c);
        }
        return group;
    }
    
    /**
     * Temporarily "borrow" the children in the list for the sole
     * purpose of computing total bounds and moving
     * them around en-mass -- used for dragging a selection.
     * Does NOT reparent the components in any way.
     */
    static LWGroup createTemporary(java.util.ArrayList selection)
    {
        LWGroup group = new LWGroup(selection);
        group.children = (java.util.ArrayList) selection.clone();
        if (DEBUG_CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
        return group;
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
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            // set parent to null first so that addChild
            // isn't calling us back with a remove child,
            // doing needless work and concurrently modifying
            // our iteration!
            c.setParent(null);
            getParent().addChild(c);
        }
        getParent().deleteChild(this);
    }

    public String getLabel()
    {
        if (super.getLabel() == null)
            return "[LWGroup #" + getID() + " nChild=" + children.size() + "]";
        else
            return super.getLabel();
    }

    public void setLocation(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();
        //System.out.println(getLabel() + " setLocation");
        Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.translate(dx, dy);
        }
        super.setLocation(x, y);

        // todo: possibly redesign rendering to happen node-local
        // so don't have to do all this
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
        super.draw(g);
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
