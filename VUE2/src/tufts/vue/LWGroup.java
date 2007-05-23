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

import java.util.*;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

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
 * @version $Revision: 1.61 $ / $Date: 2007-05-23 03:48:51 $ / $Author: sfraize $
 */
public class LWGroup extends LWContainer
{
    private static final boolean FancyGroups = false;
    private static final boolean AbsoluteChildren = false;
    
    public LWGroup() {
        if (!FancyGroups)
            disablePropertyTypes(KeyType.STYLE);
    }

    //public boolean hasAbsoluteMapLocation() { return AbsoluteChildren; }
    
    @Override
    public boolean supportsChildren() {
        return FancyGroups;
    }
    
    @Override
    public boolean supportsUserResize() {
        if (FancyGroups)
            return !isTransparent();
        else
            return false;
    }

    /** @return 1 */
    @Override
    public int getPickLevel() {
        return 1;
    }

    // atttempt to disable relative coords
    
    /*
    @Override public AffineTransform getLocalTransform() { return IDENTITY_TRANSFORM; }
    @Override public AffineTransform transformLocal(AffineTransform a) { return a; }
    @Override public void transformLocal(Graphics2D g) {}
    @Override public void transformRelative(Graphics2D g) {}
    
    protected double getMapXPrecise() { return 0; }
    protected double getMapYPrecise() { return 0; }
    public float getMapX() {
        return (float) (parent == null ? 0 : parent.getMapXPrecise()) + getX();
    }
    public float getMapY() {
        return (float) (parent == null ? 0 : parent.getMapYPrecise()) + getY();
    }
    */
    

    
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
    static LWGroup create(LWSelection selection)
    {
        LWGroup group = new LWGroup();

        //group.setFrame(selection.getBounds());
        group.importNodes(selection);
        group.setSizeFromChildren();

        return group;
    }

    /** Make the given nodes members of this group, importing also any links that are been two members of the set.
     * This also makes sure to maintain the relative z-order of the imported nodes */
    public void importNodes(Collection<LWComponent> nodes)
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
            for (LWLink l : c.getLinks()) {
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
        addChildren(sort(reparentList, LWContainer.ReverseOrder));
        //for (LWComponent c : sort(reparentList, LWContainer.ReverseOrder))
        //    addChildImpl(c);

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
    private boolean isForSelection = false;
    static LWGroup createTemporary(java.util.ArrayList selection)
    {
        LWGroup group = new LWGroup();
        if (DEBUG.Enabled) group.setLabel("<=SELECTION=>");
        group.children = (java.util.ArrayList) selection.clone();
        group.setSizeFromChildren();
        group.isForSelection = true;
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
        return group;
    }

    
    protected void setSizeFromChildren()
    {
        final Rectangle2D bounds = getChildBounds();
        super.setSize((float)bounds.getWidth(),
                      (float)bounds.getHeight());
        super.setLocation((float)bounds.getX(),
                          (float)bounds.getY());
                          
    }
    
    protected Rectangle2D.Float getChildBounds()
    {
        return LWMap.getBounds(getChildIterator());
    }

    /*
    public void setSize(float w, float h) {

        final Rectangle2D.Float bounds = getChildBounds();
        
        if (FancyGroups) {

            bounds.add(getX(), getY());

            if (bounds.getWidth() > w)
                w = (float) bounds.getWidth();
            if (bounds.getHeight() > h)
                h = (float) bounds.getHeight();
            super.setSize(w, h);
            
        } else {
            super.setSize(bounds.width, bounds.height);
        }
    }
    */


    // TODO: do we really need different methods for computing bounds: here and setSizeFromChildren?
    /*
    private static final Rectangle2D.Float EmptyBounds = new Rectangle2D.Float(0,0,10,10);
    @Override
    public Rectangle2D.Float getBounds()
    {
        if (FancyGroups && (VUE.RELATIVE_COORDS || supportsUserResize()))
            return super.getBounds();

        // Without user-resize, always report size as bounds of what we contain
        
        Rectangle2D.Float bounds = null;
        Iterator<LWComponent> i = getChildIterator();
        if (i.hasNext()) {
            bounds = new Rectangle2D.Float();
            bounds.setRect(i.next().getBounds());
        } else {
            // this happens normally on group dispersal
            return EmptyBounds;
        }

        while (i.hasNext())
            bounds.add(i.next().getBounds());
        //System.out.println(this + " getBounds: " + bounds);

        // how safe is this?
        // [ todo: not entirely: setLocation needs special updateConnectedLinks call because of this ]
        setX(bounds.x);
        setY(bounds.y);
        setAbsoluteWidth(bounds.width);
        setAbsoluteHeight(bounds.height);
        return bounds;
    }
    */

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

    /* groups are always transparent -- defer to parent for background fill color */
    @Override public java.awt.Color getRenderFillColor(DrawContext dc)
    {
        if (FancyGroups)
            return super.getRenderFillColor(dc);
        else if (dc != null && (dc.focal == this || getParent() == null))
            return dc.getFill();
        else
            return getParent().getRenderFillColor(dc);
    }
    
    public java.awt.Color getFillColor()
    {
        if (FancyGroups)
            return super.getFillColor();
        else
            return null;
    }

    public void setLocation(float x, float y)
    {
        if (AbsoluteChildren || isForSelection || !VUE.RELATIVE_COORDS) {
            setAllLocations(x, y);
        } else {
            final float dx = x - getX();
            final float dy = y - getY();
            if (FancyGroups) {
                translateAbsoluteChildren(dx, dy);
                super.setLocation(x, y);
            } else {
                super.setLocation(x, y);
                translateAbsoluteChildren(dx, dy);
            }
        }
    }
    
    // todo: do this by default for any container (e.g., slides!)
    private void translateAbsoluteChildren(float dx, float dy)
    {
        for (LWComponent c : getChildList()) {
            if (c.hasAbsoluteMapLocation()) // if not fancy links, contents are all absolute
                c.translate(dx, dy);
        }
    }
    
    private void setAllLocations(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();
        //System.out.println(getLabel() + " setLocation");
        for (LWComponent c : getChildList()) {

            //boolean inGroup = c.getParent() instanceof LWGroup;
                
            // If parent and some child both in selection and you drag, the selection
            // (an LWGroup) and the parent fight to control the location of the child.
            // There may be a cleaner way to handle this, but checking it here works.
            // Also, do NOT skip if we're in a group -- that condition is caught below.

            if (c.isSelected() && c.isAncestorSelected())
                continue;
            
            c.translate(dx, dy);
        }
        
        // this setLocation I think never has any effect, as our bounds dynamically
        // change with with content, quietly updating x & y, so it never appears we've
        // moved!  ??? -- wait, is that for selection move, or group object move, or
        // moving of all group contents auto-creating the groups?  The latter?  really
        // got to move this to selection code.
        
        super.setLocation(x, y);
        updateConnectedLinks();
    }
    
    //@Override public double getScale() { return 1; }
    //@Override public double getMapScale() { return 1; }
    /*
    @Override void setScale(double scale)
    {
        if (FancyGroups && (VUE.RELATIVE_COORDS || DEBUG.Enabled)) {
            super.setScale(scale);
        } else {
            // intercept any attempt at scaling us and turn it off
            
            // if a group ever defines it's size by it's children, it can't have a scale
            // (too complicated -- the upper left position of the group is defined by
            // the children, and the children's ultimate map coordinate is defined by
            // the location of the group!), and in fact it's children's coords would
            // be better off non-local!
            
            super.setScale(1);
        }
    }
    */

    protected boolean containsImpl(final float x, final float y, float zoom)
    {
        if (hasDecoratedFeatures()) {
            return super.containsImpl(x, y, zoom);
        } else {
            // x/y have already been translated into the groups coordinate space
            // (including compressed if the group is scaled).
            // Might be better to have LWTraversal smartly handle this tho...
            for (LWComponent c : getChildList())
                if (c.containsParentCoord(x, y))
                    return true;
            return false;
        }
    }
    
    protected boolean intersectsImpl(final Rectangle2D rect)
    {
        if (hasDecoratedFeatures()) {
            return super.intersectsImpl(rect);
        } else {
            for (LWComponent c : getChildList())
                if (c.intersects(rect))                 // TODO: not in parent coords!
                    return true;
            return false;
        }
    }

    private boolean hasDecoratedFeatures() 
    {
        if (FancyGroups)
            return getStrokeWidth() > 0 || !isTransparent();
        else
            return getEntryToDisplay() != null;
    }
    
    
    /** If it paints a background or draws a border, it's empty space can be picked, otherwise, only pick via children */
    // containsImpl is handling knowing what to do already!
    protected LWComponent X_defaultPick(PickContext pc) {
        if (hasDecoratedFeatures())
            return this;
        else {
            // could check for children hit, then return LWGroup.this,
            // tho the coordinate in pc is the top-level map coordinate, and
            // needs to be transformed to every child again -- can't
            // we handle this gracefully somehow in LWTraversal?
            //for (LWComponent c : getChildList()) {}
            // christ -- what about groups inside groups??
            return null;
        }
    }

    
    /**
     * A hit on any component in the group finds the whole group,
     * not that component.
     */
    protected LWComponent pickChild(PickContext pc, LWComponent c) {

        //if (pc.pickDepth > 0 || pc.dropping != null)

        // If we're dropping, we can/should allow a drop into the child of a group --
        // but if it's the group that's being dragged, and it was selected by selecting
        // a child, we could drop the group into itself, creating an infinite loop...
        // Well either need to always enfore all members of a group selected together
        // (which would work cause we ignore selected when looking for pick targets), or
        // more robust, always excude the entire list of everything in the selection and
        // all decendents from picks... (very heavy duty tho)

        // TODO: this not good enough -- doesn't deal with grand-children (children
        // of group memebers -- they get picked even if group should have been picked)
        // -- will need to handle this in LWTraversal...

        // TODO: each LWComponent needs a traversal penetration parameter: either it
        // will prevent descent entirely, or it could increase a depth
        // value by a large enough number that it'll never be reached.

        if (pc.pickDepth > 0)
            return c;
        else
            return this;
    }

    /** use the raw bounds: don't add a target swath */
    public boolean targetContains(float x, float y)
    {
        return contains(x, y);
    }


    /*
    protected void notifyLWCListeners(LWCEvent e) {
        if (e.key == LWKey.ChildrenAdded || e.key == LWKey.ChildrenRemoved) {
            // need to check this hear instead of add/remove child impl's
            // as on undo, those aren't called, but we get a simulated event.
            updateBounds();
        }
        super.notifyLWCListeners(e);
    }
    */
    /*
    protected void addChildImpl(LWComponent c) {
        super.addChildImpl(c);
        updateBounds();
    }
    protected void removeChildImpl(LWComponent c) {
        super.removeChildImpl(c);
        updateBounds();
    }
    */

    
    void broadcastChildEvent(LWCEvent e)
    {
        if (e.key == LWKey.Location || e.key == LWKey.Size) {
            if (updateBounds()) {
                updateConnectedLinks();
                // events were replaced with new ones: no need to broadcast the old ones
                return;
            }
        }

        /*
        //if (e.key == LWKey.Location || e.key == LWKey.Size) {
        // if (e.key.isBoundsEvent() -- todo: see MapViewer code for similar; implement as bit in keys
        if (true) {
            Rectangle2D.Float newBounds = getChildBounds();
            boolean groupReshaped = false;
            if (newBounds.x != getX() || newBounds.y != getY()) {
                
                final LWComponent mover = e.getComponent();
                
                translateChildren(getX() - newBounds.x,
                                  getY() - newBounds.y,
                                  null);//mover);
                super.setLocation(newBounds.x, newBounds.y);
                //setX(newBounds.x);
                //setY(newBounds.y);
//mover.takeLocation(0,0); // not good enough yet: what if only exceeded left edge, or top edge
                // NEED TO GET UNDO WORKING: currently, mostly,
                // tho would need a way to know the difference between
                // undoing a setLocation that was just a drag of the group,
                // and a setLocation that came from the drag of a child,
                // where we had to translate all the children around...
//super.setSize(newBounds.width, newBounds.height);
                //mover.notify("location-group-relative");
                groupReshaped = true;
            }

            if (newBounds.width != getWidth() || newBounds.height != getHeight()) {
                groupReshaped = true;
            }
            
            if (groupReshaped) {
                super.setSize(newBounds.width, newBounds.height);
                return; // Ignore the original event: new ones were generated
            }
            updateConnectedLinks();
        }
        */
        super.broadcastChildEvent(e);
    }

//     public void layout() {
//         super.layout();
//         setFrame(computeBounds());
//     }
    protected void layoutImpl(Object trigger) {
        updateConnectedLinks();
        updateBounds();
    }
                          

    private boolean updateBounds()
    {
        Rectangle2D.Float newBounds = getChildBounds();
        boolean reshaped = false;
        if (newBounds.x != getX() || newBounds.y != getY()) {
                
            translateChildren(getX() - newBounds.x,
                              getY() - newBounds.y,
                              null);
            if (DEBUG.CONTAINMENT) out("UPDATING LOCATION\n");
            super.takeLocation(newBounds.x, newBounds.y);
            reshaped = true;
        }
        
        if (newBounds.width != getWidth() || newBounds.height != getHeight())
            reshaped = true;
        
        if (reshaped) {
            if (DEBUG.CONTAINMENT) out("UPDATING SIZE\n");
            super.takeSize(newBounds.width, newBounds.height);
            return true;
        } else
            return false;
        
    }

    protected void translateChildren(float dx, float dy, LWComponent exclude) {
        for (LWComponent c : getChildList()) {
            if (c != exclude) {
                //c.translate(dx, dy);
                c.takeLocation(c.getX() + dx,
                               c.getY() + dy);
                c.notify("location-group"); // mover map location changed, relative unchanged, all otherse the reverse
            }
        }
    }


    protected void drawImpl(DrawContext dc)
    {
        if (DEBUG.CONTAINMENT) {
            java.awt.Shape shape = getLocalShape();
            dc.g.setColor(new java.awt.Color(64,64,64,64));
            dc.g.fill(shape);
            dc.g.setColor(java.awt.Color.blue);
            dc.setAbsoluteStroke(1.0);
            dc.g.draw(shape);
        }
        
        if (FancyGroups)
            // draw fill, border & children
            super.drawImpl(dc);
        else
            // don't draw fill or border
            drawChildren(dc);

        if (isSelected() && dc.isInteractive() && dc.focal != this) {
            java.awt.Shape shape = getLocalShape();
            dc.g.setColor(COLOR_HIGHLIGHT);
            dc.g.fill(shape);
        }

    }

    
}
