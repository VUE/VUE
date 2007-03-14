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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.awt.geom.Rectangle2D;

/**
 *
 * Manage a group of LWComponents -- does not have a fixed shape or paint any backround
 * or border -- simply used for moving, resizing & layering a collection of objects
 * together.
 *
 * Consider re-architecting group usage to act purely as a selection convenience -- so
 * they'd actually be transparent to most everything (e.g., nothing in the GUI would
 * have to know how to handle a "group") -- it would just always be a group of
 * components that were selected together (and maybe the selection draws a little
 * differently).  To really do this would mean a group is no longer even a
 * container... thus no impact on layering or anything, which would dramatically
 * simplify all sorts of special case crap.  However, if it's no longer a container, you
 * couldn't drop them into nodes as children.  Actually, a hybrid is possible -- yes,
 * lets try that.
 *
 * @author Scott Fraize
 * @version $Revision: 1.45 $ / $Date: 2007-03-14 17:18:25 $ / $Author: sfraize $
 */
public class LWGroup extends LWContainer
{
    public LWGroup() {}

    public boolean supportsUserResize() {
        return !isTransparent();
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

        group.importNodes(selection);
        group.setSizeFromChildren();

        return group;
    }

    /** Make the given nodes members of this group, importing also any links that are been two members of the set.
     * This also makes sure to maintain the relative z-order of the imported nodes */
    public void importNodes(java.util.List<LWComponent> nodes)
    {
        final List<LWComponent> reparentList = new java.util.ArrayList();
        final Collection<LWComponent> allUniqueDescendents = new java.util.HashSet(); // enforce unique w/HashSet
        
        for (LWComponent c : nodes) {
            //if (c instanceof LWLink) // the only links allowed are ones we grab
            //  continue; // enabled in order to support reveal's with links in them
            reparentList.add(c);
            allUniqueDescendents.add(c);
            c.getAllDescendents(ChildKind.PROPER, allUniqueDescendents);
        }

        //final LWContainer defaultLinkParent = getMap();

        //----------------------------------------------------------------------------------------
        // If both ends of any link are in the selection of what's being added to the
        // group, or are descendents of what's be added to the group, and that link's
        // parent is not already something other than the default link parent, scoop it
        // up as a proper child of the new group.
        //----------------------------------------------------------------------------------------

        final HashSet uniqueLinks = new HashSet();

        for (LWComponent c : allUniqueDescendents) {
            if (DEBUG.PARENTING) out("ALL UNIQUE " + c);
            for (LWLink l : c.getLinkRefs()) {
                boolean bothEndsInPlay = !uniqueLinks.add(l);
                if (DEBUG.PARENTING) out("SEEING LINK " + l + " IN-PLAY=" + bothEndsInPlay);
                if (bothEndsInPlay && l.getParent() instanceof LWMap) {
                    if (DEBUG.PARENTING) out("GRABBING " + c + " (both ends in group)");
                    reparentList.add(l);
                }
            }
        }
        // Be sure to preserve the current relative ordering of all
        // these components the new group.
        for (LWComponent c : sort(reparentList, LWContainer.ReverseOrder))
            addChildImpl(c);

        // we can use addChildImpl instead of addChildren as this only
        // happens during creation, and we don't need to optimize
        // events (we're not in the model yet)
    }
    
    /*
     * "Borrow" the children in the list for the sole
     * purpose of computing total bounds and moving
     * them around en-mass -- used for dragging a selection.
     * Does NOT reparent the components in any way.
     * TODO: get rid of this and just have useSelection,
     * or move this code to LWSelection itself.
     */
    static LWGroup createTemporary(java.util.ArrayList selection)
    {
        LWGroup group = new LWGroup();
        if (DEBUG.Enabled) group.setLabel("<=SELECTION=>");
        group.children = (java.util.ArrayList) selection.clone();
        group.setSizeFromChildren();
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
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
    
    protected void setSizeFromChildren()
    {
        final Rectangle2D bounds = getChildBounds();
        super.setSize((float)bounds.getWidth(),
                      (float)bounds.getHeight());
        super.setLocation((float)bounds.getX(),
                          (float)bounds.getY());

    }
    
    protected Rectangle2D getChildBounds()
    {
        return LWMap.getBounds(getChildIterator());
    }

    public void setSize(float w, float h) {
        final Rectangle2D bounds = getChildBounds();

        bounds.add(getX(), getY());

        if (bounds.getWidth() > w)
            w = (float) bounds.getWidth();
        if (bounds.getHeight() > h)
            h = (float) bounds.getHeight();
        super.setSize(w, h);
    }

    private static final Rectangle2D EmptyBounds = new Rectangle2D.Float();

    // TODO: do we really need different methods for computing bounds: here and setSizeFromChildren?
    public Rectangle2D getBounds()
    {
        if (supportsUserResize())
            return super.getBounds();

        // Without user-resize, always report size as bounds of what we contain
        
        Rectangle2D.Float bounds = null;
        Iterator i = getChildIterator();
        if (i.hasNext()) {
            bounds = new Rectangle2D.Float();
            bounds.setRect(((LWComponent)i.next()).getBounds());
        } else {
            // this happens normally on group dispersal
            //System.out.println(this + " getBounds: EMPTY!");
            return EmptyBounds;
        }

        while (i.hasNext())
            bounds.add(((LWComponent)i.next()).getBounds());
        //System.out.println(this + " getBounds: " + bounds);

        // how safe is this?
        // [ todo: not entirely: setLocation needs special updateConnectedLinks call because of this ]
        setX(bounds.x);
        setY(bounds.y);
        setAbsoluteWidth(bounds.width);
        setAbsoluteHeight(bounds.height);
        return bounds;
    }

    public void setZoomedFocus(boolean tv)
    {
        // the group object never takes zoomed focus
    }


    protected void removeChildrenFromModel()
    {
        // groups don't actually delete their children
        // when the group object goes away.  Overriden
        // so LWContainer.removeChildrenFromModel
        // is skipped.
    }
    
    /**
     * Remove all children and re-parent to this group's parent,
     * then remove this now empty group object from the parent.
     */
    public void disperse()
    {
        // todo: better to insert all the children back into the
        // parent at the layer of group object instead of on top of
        // everything else.

        if (DEBUG.PARENTING) out("dispersing group " + this);

        // todo: if group in group, want to disperse to parent group,
        // not the map!
        LWContainer newParent = getFirstAncestor(LWMap.class);
        if (hasChildren()) {
            List tmpChildren = new ArrayList(children);
            newParent.addChildren(tmpChildren.iterator());
        }
        getParent().deleteChildPermanently(this);
    }

    /*
    public String getLabel()
    {
        if (super.getLabel() == null)
            return "[LWGroup #" + getID() + " nChild=" + (children==null?-1:children.size()) + "]";
        else
            return super.getLabel();
    }
    */

    /** groups are transparent -- defer to parent for background fill color */
    public java.awt.Color X_getFillColor()
    {
        return getParent() == null ? null : getParent().getFillColor();
    }

    public void setLocation(float x, float y)
    {
//setAspect(1280f / 854f);
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
            //if (inGroup && c.getParent() != this)
            //continue;
            // allowed for new "page" style groups

            c.translate(dx, dy);
        }
        // this setLocation I think never has any effect, as our
        // bounds dynamically change with with content, quietly
        // updating x & y, so it never appears we've moved!
        super.setLocation(x, y);
        updateConnectedLinks();

        // todo: possibly redesign rendering to happen node-local
        // so don't have to do all this
    }
    
    // TODO: Can't sanely support scaling of a group because we'd also
    // have to scale the space between the the children -- to make
    // this work we'll have to set the scale for the whole group, draw
    // it (and tell the children NOT to set their scale values
    // individually) and sort out the rest of a mess getting all that
    // to work will imply.
    
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

    public boolean contains(final float x, final float y)
    {
        if (isTransparent()) {
            for (LWComponent c : getChildList())
                if (c.contains(x, y))
                    return true;
            return false;
        } else
            return super.contains(x, y);
    }
    
    public boolean intersects(final Rectangle2D rect)
    {
        if (hasVisibleFeatures())
            return super.intersects(rect);
        
        for (LWComponent c : getChildList())
            if (c.intersects(rect))
                return true;
        
        return false;
    }

    private boolean hasVisibleFeatures() 
    {
        return getStrokeWidth() > 0 || !isTransparent();
    }
    
    
    /** If it paints a background or draws a border, it's empty space can be picked, otherwise, only pick via children */
    protected LWComponent defaultPick(PickContext pc) {
        if (hasVisibleFeatures())
            return this;
        else 
            return null;
    }

    
    /**
     * A hit on any component in the group finds the whole group,
     * not that component.
     */
    protected LWComponent pickChild(PickContext pc, LWComponent c) {

        //if (pc.pickDepth > 0 || pc.dropping != null)

        // If we're dropping, we can/should allow a drop into the
        // child of a group -- but if it's the group that's being
        // dragged, and it was selected by selecting a child,
        // we could drop the group into itself, creating an
        // infinite loop...  Well either need to always enfore
        // all members of a group selected together (which would
        // work cause we ignore selected when looking for pick
        // targets), or more robust, always excude the entire list
        // of everything in the selection and all decendents from
        // picks... (very heavy duty tho)

        if (pc.pickDepth > 0)
            return c;
        else
            return this;
    }

    /*
    public LWComponent findChildAt(float mapX, float mapY)
    {
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.findChildAt " + getLabel());
        // hit detection must traverse list in reverse as top-most
        // components are at end
        //todo: handle focusComponent here?
        java.util.ListIterator i = children.listIterator(children.size());
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (!c.isDrawn())
                continue;
            if (c.contains(mapX, mapY))
                return this;
        }
        return defaultHitComponent();
    }
    */
    
    /*
    public LWComponent findLWSubTargetAt(float mapX, float mapY)
    {
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.findLWSubTargetAt[" + getLabel() + "]");
        LWComponent c = super.findLWSubTargetAt(mapX, mapY);
        return c == this ? null : c;
        }*/
    
    /** use the raw bounds: don't add a target swath */
    public boolean targetContains(float x, float y)
    {
        return contains(x, y);
    }

    void broadcastChildEvent(LWCEvent e) {
        if (e.key == LWKey.Location || e.key == LWKey.Size)
            updateConnectedLinks();
        super.broadcastChildEvent(e);
    }
    
    /*
    public void layout()
    {
        super.layout();
        setFrame(computeBounds());
    }
    */
    
    
    protected void drawImpl(DrawContext dc)
    {
        if (getStrokeWidth() == -1) { // todo: temporary debug
            System.err.println("hiding " + this);
            return;
        }

        //drawSelectionDecorations(dc);

        //out("transform at draw: " + dc.g.getTransform());        
        //out("    shape to fill: " + getShape());
        //final float scale = getScale();
        //if (scale != 1f) dc.g.scale(scale, scale);
        //out("transform at fill: " + dc.g.getTransform());        
        
        if (getFillColor() != null) {
            dc.g.setColor(getFillColor());
            dc.g.fill(getShape());
        }

        
        /*
        if (isSelected()) {
            dc.g.setColor(COLOR_HIGHLIGHT);
            dc.g.setStroke(new java.awt.BasicStroke(SelectionStrokeWidth));
            dc.g.draw(getBounds());
        }
        */


        //if (scale != 1F) dc.g.scale(scale, scale);
        // Only need to scale down for drawing children, as getShape for default
        // rectangles includes the current scale factor when being computed...

        // draw children, pathway decorations, etc.
        super.drawImpl(dc);

        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(getShape());
            //dc.g.draw(new Rectangle2D.Float(0,0, getAbsoluteWidth(), getAbsoluteHeight()));
        }

        
        /*
        if (isIndicated()) {
            // this should never happen, but just in case...
            dc.g.setColor(COLOR_INDICATION);
            dc.g.setStroke(STROKE_INDICATION);
            dc.g.draw(getBounds());
        }
        */
        

        if (DEBUG.CONTAINMENT) {
            if (isRollover())
                dc.g.setColor(java.awt.Color.green);
            else
                dc.g.setColor(java.awt.Color.blue);
            //if (isIndicated())
            //    dc.g.setStroke(STROKE_INDICATION);
            //else
            dc.g.setStroke(STROKE_TWO);
            dc.g.draw(getBounds());
        }

    }
    
    
}
