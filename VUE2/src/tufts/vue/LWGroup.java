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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;


/**
 *
 * Manage a group of LWComponents.  A group defines it's bounds by it's members.
 * Default top-level behavour for "groups" as generally defined is that selecting
 * anything in a group with a default selection tool will actually select the group
 * itself, and allow one to reposition the group with all it's memebers at once,
 * such that all members maintain their relative position to one another.
 *
 * By default, groups in VUE have no border or fill, but these can be set
 * if desired.  Also significantly, the entire group can be dropped into
 * a scaled context (or scaled itself), and all members will still keep
 * stable positions relative to each other in the scaled context.
 *
 * @author Scott Fraize
 * @version $Revision: 1.91 $ / $Date: 2008-07-23 22:35:28 $ / $Author: sfraize $
 */

// TODO: the FORMING of groups is broken on slides -- the new children are repositioned!

public class LWGroup extends LWContainer
{
    private static final boolean FancyGroups = true;

    private boolean isForSelection = false;

    public LWGroup() {
        if (!FancyGroups)
            disablePropertyTypes(KeyType.STYLE);
        
        disableProperty(LWKey.Font);
        disableProperty(LWKey.FontName);
        disableProperty(LWKey.FontSize);
        disableProperty(LWKey.FontStyle);
        disableProperty(LWKey.TextColor);
        
    }

    @Override
    public boolean supportsChildren() { return FancyGroups; }
    
    @Override
    public boolean supportsUserResize() { return false; }
//         if (FancyGroups)
//             return !isTransparent();
//         else
//             return false;
//     }

    @Override
    public boolean supportsUserLabel() { return false; }

    @Override
    public boolean isOrphan() {
        return isForSelection || super.isOrphan();
    }

    
    /**
     * For the viewer selection code -- we're mainly interested
     * in the ability of a group to move all of it's children
     * with it.
     */
    void useSelection(LWSelection selection)
    {
        if (!isForSelection)
            Util.printStackTrace("NOT FOR SELECTION!: " + this);
        Rectangle2D bounds = selection.getBounds();
        if (bounds != null) {
            super.setSize((float)bounds.getWidth(),
                          (float)bounds.getHeight());
            super.setLocation((float)bounds.getX(),
                              (float)bounds.getY());
        }  else {
            System.err.println("null bounds in LWGroup.useSelection");
        }
        super.mChildren = selection;
    }

    /**
     * Create a new LWGroup, reparenting all the LWComponents
     * in the selection to the new group.
     */
    static LWGroup create(LWSelection selection)
    {
        final LWGroup group = new LWGroup();

        // performance: pre-allocate the child list for the maximum child content size
        group.mChildren = new ArrayList(selection.size());
        
        group.createFromNonLinks(selection);
        if (DEBUG.CONTAINMENT) System.out.println("CREATED: " + group);

        return group;
    }

    /**
     * Establish a newly created group from the memebrs of the given selection.
     * Will initially ignore any links (for a variety of reasons) and leave
     * it up each link cleanup task to decide if it should add itself to the group.
     * This also makes sure to maintain the relative z-order of the imported nodes.
     */
    private void createFromNonLinks(Collection<LWComponent> selection)
    {
        // The creation of new groups, especially ones containing links, and especially
        // curved links, creates a number of thorny problems that need to be dealt with
        // to do it right, and to make sure undo of the group creation is going to work.

        // When the new children of the group are pulled from their existing parent, we
        // want to localize the coordinates within the new group (this happens
        // automatically).  However, if the new group doesn't know it's bounds yet, we
        // can't meaninfully localize the coordinates.  So when creating groups, we use
        // setShapeFromContents to provide the initial location and size of the group
        // (as opposed to relying on generic normalization).  We can't normalize until
        // the group has at least bootstrapped to know it's initial location (the upper
        // left hand corner of the upper left most member).

        // There are also problems with adding curved links initially at group creation
        // time (UNDO breaks: see below), so we never allow the explicit grabbing of a
        // link into a group when it is initially created.  The links will add
        // themselves as needed later via the link cleanup task.  So newly created
        // groups go through two phases: in the first phase, completed in this method,
        // there will be a group containing all non-links, fully normalized when we're
        // done.  The normalization process is different during creation in that we set
        // the location & size first (based on just the non-link content), then we
        // normalize only the added children (by calling normalize(false)) -- so their
        // locations are properly relative to the newly created group.  In the second
        // phase, all connected links to anything that was added to the group will
        // automatically run their cleanup tasks (having detected changes in the
        // endpoints), and will then be able to join an already stabilized group and
        // map-parented group, so that if it's a curved link, when localizeCoordinates
        // is called to translate it's control points into the new group coordinate
        // space, the undo manager will be able to record the old positions of those
        // control points, so they can be faithfully restored on undo.  Theoretically,
        // localizeCoordinates / setLocation on the entire link could handle this during
        // undo, tho we've had so many problems faithfully handling the redirection of
        // setLocation on links to a translate for all link sub-points (as links don't
        // have a 0,0 of their own: just sub-points recorded within their parent), that
        // ensuring we have real coordinates to restore for undo is the only safe way to
        // handle this.
        
        // Another potential problem is that if we're creating this new group inside
        // another group, there's an issue with the cleanup tasks: there's nothing that
        // ensures that the child group's cleanup task runs before the parents, which
        // would be critical if a newly created group were to rely on it's standard
        // later normalization pass to sort itself out.  This is why on creation, we
        // ensure the group is in a fully normalized and stable state before it's done
        // being created, even if later it will be re-normalized by link cleanup tasks
        // adding themselves to the group.  We could be hit again someday with this
        // issue of cleanup tasks not enforcing a depth-first task order, tho our
        // current FIFO ordering appears to cover us (as long as the group is normalized
        // by the end of it's creation).
        
        final Collection<LWComponent> reparenting;
        
        if (true) {
            reparenting = 
            new HashSet<LWComponent>() {
                @Override
                public boolean add(LWComponent c) {
                    //if (c instanceof LWLink && ((LWLink)c).isBound()) 
                    //// don't add any links that are connected to anything: they'll reparent themselves
                    if (c instanceof LWLink) {
                        // don't add any links AT ALL for now -- safer -- see below problem comment
                        return false;
                    } else
                        return super.add(c);
                }
            };

            reparenting.addAll(selection);
            
            setShapeFromContents(reparenting);
        }

        else
            
        {
        
        //----------------------------------------------------------------------------------------
        // If both ends of any link are in the selection of what's being added to the
        // group, or are descendents of what's be added to the group, and that link's
        // parent is not already something other than the default link parent, scoop it
        // up as a proper child of the new group.
        //
        // Although the link cleanup task also enforces this condition, adding this code
        // here is much more perfomant when creating large groups, and it simplifies the
        // initial group creation code.
        //
        // OOPS -- PROBLEM: if we grab ANY links here, if they have any control points, when
        // their coordinates are localized, there's no undo manager to catch those events,
        // because this is a group under creation, and as such isn't in the model till
        // it's done being created!  (Mind you, this is also true for nodes, but curved
        // links have this special case problem of trying to undo a translate, as links
        // don't have a real location...)
        //
        // So the upshot is that our slow method of only adding links at the end
        // with cleanup tasks handles this better, because then the undo manager
        // gets all the events needed to sort things out...
        //----------------------------------------------------------------------------------------
        //final Collection<LWComponent> reparenting = new HashSet();
        reparenting = new HashSet();
        final Collection<LWComponent> allUniqueDescendents = new HashSet();
        for (LWComponent c : selection) {
            reparenting.add(c);
            allUniqueDescendents.add(c);
            c.getAllDescendents(ChildKind.PROPER, allUniqueDescendents);
        }
        final HashSet uniqueLinks = new HashSet();
        for (LWComponent c : allUniqueDescendents) {
            if (DEBUG.PARENTING) out("ALL UNIQUE " + c);
            for (LWLink l : c.getLinks()) {
                boolean bothEndsInPlay = !uniqueLinks.add(l);
                if (DEBUG.PARENTING) out("SEEING LINK " + l + " IN-PLAY=" + bothEndsInPlay);
                //if (bothEndsInPlay && l.getParent() instanceof LWMap) { // why this LWMap check? is old in any case: need to allow slides
                // TODO: right way to do this: only if link parent is currently the same as the top
                // level parent all the selection contents are coming from...
                if (bothEndsInPlay && !(c.getParent() instanceof LWGroup)) { // don't pull out of embedded group
                    if (DEBUG.PARENTING) out("GRABBING " + l + " (both ends in group)");
                    reparenting.add(l);
                }
            }
        }
        
        setShapeFromContents(reparenting);
        }
        //----------------------------------------------------------------------------------------
        
        
        // Be sure to preserve the current relative ordering of all
        // these components the new group.
        addChildren(sort(reparenting, LWContainer.ReverseOrder));

        // At first we set our size and location for all the imported nodes (no links)
        // above via setShapeFromContents.  Now that we've imported the non-link
        // children, they need to be normalized (coordinates made relative to the group).
        // In this special init bootstrapping case, we tell normalize NOT to change
        // the location or size of the group itself -- we already know that, and
        // to do that again before the new children's coordinates are made relative
        // would produce bogus results for the new location.
        
        normalize(false);
    }
    
    /**
     * "Borrow" the children in the list for the sole purpose of computing total bounds
     * and moving them around en-mass -- used for dragging a selection.  Does NOT
     * reparent the components in any way. 
     */

    // TODO: get rid of this and just have useSelection, or move this code to
    // LWSelection itself.  (Maybe have LWSelection subclass LWContainer so it can draw,
    // etc) -- this selection special stuff is a mess to have in LWGroup, which is
    // already some rediculously complicated code.

    static public LWGroup createTemporary(java.util.ArrayList selection)
    {
        LWGroup group = new LWGroup();
        group.isForSelection = true;
        if (DEBUG.Enabled) group.setLabel("<=SELECTION=>");
        group.mChildren = (java.util.ArrayList) selection.clone();
        group.setShapeFromChildren();
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
        return group;
    }

    private void setShapeFromContents(Iterable<LWComponent> contents)
    {
        // As we only allow creating groups from elements that all have the same parent,
        // we can use getLocalBorderBounds, which we also need to do to make sure
        // the location of the newly created group is correct within it's new parent.
        final Rectangle2D.Float bounds = LWMap.getLocalBorderBounds(contents);
        super.setSize(bounds.width,
                      bounds.height);
        super.setLocation(bounds.x,
                          bounds.y);
                          
    }
    
    private void setShapeFromChildren()
    {
        setShapeFromContents(getChildren());
    }

    protected void normalize() {
        normalize(true);
    }
    
    /**
     * Normalize the group.
     *
     * The process of normalization is to expand/contract the group to fit the new
     * bounds of our contents if they've moved/resized, and then update the local
     * coordinates of our members to reflect their offset with the new group bounds, if
     * the upper left hand corner of the group has changed (it's 0,0 position in it's
     * parent has changed).  The point is to update the local child locations relative
     * to any new group position such that there is no net change to their absolute
     * position on the map.  So: if a group member has simply moved within the group
     * without moving beyond any current edge of the group, there is nothing to do.  If
     * a group member has moved below or to the right if our current bounds, the group
     * simply needs to increase it's size.  Howver, if a member has moved above or to
     * the left of our current bounds, the group needs to change it's location (as well
     * as size), then "normalize" all the children: translate them down and to the right
     * by the exact amount the group has moved up and to the left.
     *
     * @param reshape - if true, allow reshaping of the group.  This is the standard
     * case, except during group creation, where in order to bootstrap ourseleves,
     * the group separately estalishes an initial bounds, and then updates the child
     * locations.
     *
     */
    private void normalize(boolean reshape)
    {
        if (DEBUG.WORK) {System.out.println(); out("NORMALIZING" + (reshape?"":" W/OUT RESHAPE FOR INIT"));}
        
        final Rectangle2D.Float curBounds = getLocalBounds();
        final Rectangle2D.Float preBounds = getPreNormalBounds();

        final float dx = preBounds.x;
        final float dy = preBounds.y;

        if (DEBUG.WORK) {
            final float dw = preBounds.width - curBounds.width;
            final float dh = preBounds.height - curBounds.height;
            out("curBounds: " + Util.out(curBounds));
            out("preBounds: " + Util.out(preBounds) + String.format("; dx=%+.2f dy=%+.2f dw=%+.1f dh=%+.1f", dx, dy, dw, dh));
                //out("preBounds: " + Util.out(preBounds) + "; dx=" + dx + " dy=" + dy);
        }
        if (dx != 0.0f || dy != 0.0f) {

            // we must call the event generating setter for undo to work (v.s. takeLocation)
            // However, when the group normalizes it's location, we do not want to issue
            // mapLocationChanged calls to all descendents, as normalization is all
            // about changing the location of the group without changing the on-map
            // location of any of it's members, so we make the special call to
            // setLocation here that allows this.

            if (reshape)
                super.setLocation(getX() + dx,
                                  getY() + dy,
                                  this,
                                  false);

            // Could theoretically handle this via mapLocationChanged calls, if above
            // was a real call to setLocation, but then there'd have to be a check
            // to see if the parent specifically was an LWGroup.
            
            if (DEBUG.WORK) out("normalizing relative children: dx=" + dx + " dy=" + dy);
            for (LWComponent c : getChildren()) {
                    
                // we don't really need an event here (any descendents have already
                // been called with mapLocationChhanged if they need it due to
                // the above call to setLocation), and this translation is indended
                // to leave the component at it's exact current map location -- it's
                // position is only changing relative to it's parent group, which
                // was reshaped to the northwest -- this call moves all our non
                // absolute (non-link) members the same amount to the southeast.

                // HOWEVER, we still need to generate an event for undo...
                // So we're calling translate (instead of takeTranslation)
                    
                c.translate(-dx, -dy);

                if (c instanceof LWLink) { // do we still need this?
                    // links to links can get out of sync with updates
                    // depending on the order they exist in the child list.
                    // this should help for at least most first tier cases:
                    ((LWLink)c).layout();
                }
            }
            
        }
        
        if (DEBUG.WORK) {
            out(String.format("curShape %f,%f %fx%f", getX(), getY(), super.width, super.height));
            out(String.format("newShape %f,%f %fx%f", dx, dy, preBounds.width, preBounds.height));
        }

        if (reshape)
            super.setSize(preBounds.width, preBounds.height);
        
        if (DEBUG.WORK) out("NORMALIZED");
    }
    
    /** @return the bounds of of contents prior to normalization: these bounds 
     * are likely to be different than our current bounds: the process of normalization
     * ensures that the group bounds eventually match these bounds.  The bounds
     * are in the parent-local coordinate space (the groups).  So, for instance, if
     * the upper left most member of a group moves up 10 pixels, the pre-normalized
     * upper left hand corner bounds of the contents will be at 0,-10, relative to
     * the current group (the parent), and the pre-normal height will be 10 pixels
     * greater than the current height of the group.
     */
    private Rectangle2D.Float getPreNormalBounds()
    {
        if (numChildren() < 2) // if only zero or one child, we should be about to disperse...
            return LWMap.EmptyBounds;
        else
            return LWMap.getLocalBorderBounds(getChildren());
    }

    @Override
    protected void removeChildrenFromModel()
    {
        // groups don't actually delete their children
        // when the group object goes away.  Overriden
        // so LWContainer.removeChildrenFromModel
        // is skipped.
    }

    private class DisperseOrNormalize implements Runnable {
        final Object srcMsg;
        DisperseOrNormalize(Object srcMsg) {
            this.srcMsg = srcMsg;
        }
        public void run() {
            if (isDeleted())
                return;
            
            if (numChildren() < 2) {
                if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("AUTO-DISPERSING on child count " + numChildren());
                disperse();
            } else {
                normalize();
            }
        }
        public String toString() {
            return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[" + srcMsg + "]";
        }
    }

    private void requestCleanup(Object srcMsg) {
        //super.addCleanupTask(new DisperseOrNormalize(srcMsg)); // always allocates the darn task..

        //if (DEBUG.CONTAINMENT) out("requestCleanup on " + srcMsg);
        
        final UndoManager um = getUndoManager();
        if (um != null && !um.isUndoing() && !um.hasLastTask(this)) {
            if (DEBUG.CONTAINMENT) out("addLastTask/DisperseOrNormalize; on " + srcMsg);
            //if (DEBUG.Enabled) out(TERM_RED + "ADDING CLEANUP TASK on: " + srcMsg + TERM_CLEAR);
            //um.addCleanupTask(this, new DisperseOrNormalize(srcMsg));
            //super.addCleanupTask(new DisperseOrNormalize(srcMsg));
            um.addLastTask(this, new DisperseOrNormalize(srcMsg));
        }
    }
    
    @Override
    protected void removeChildren(Iterable<LWComponent> iterable, Object context)
    {
        super.removeChildren(iterable, context);
        requestCleanup("removeChildren");
    }

    @Override
    public void addChildren(List<LWComponent> iterable, Object context) {
        super.addChildren(iterable, context);
        requestCleanup("addChildren");
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

        if (DEBUG.PARENTING || DEBUG.CONTAINMENT) System.out.println("DISPERSING: " + this);

        if (hasChildren()) {
            final LWContainer newParent = getParentOfType(LWContainer.class);
            final List tmpChildren = new ArrayList(mChildren);
            //if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("DISPERSING " + tmpChildren.size() + " children");
                
            // we can brute-force remove our children, to skip de-parenting events,
            // as this group will be dissapeared at the end of this operation anyway
            // TODO: this is probably screwing up UNDO tho...
            //this.children.clear();
            newParent.addChildren(tmpChildren);
        }
        getParent().deleteChildPermanently(this);
    }



//     /* groups are always transparent -- defer to parent for background fill color */
//     @Override
//     public java.awt.Color getRenderFillColor(DrawContext dc)
//     {
//         if (FancyGroups)
//             return super.getRenderFillColor(dc);
        
//         if (dc != null && (dc.focal == this || getParent() == null))
//             return dc.getFill();
//         else if (getParent() != null)
//             return getParent().getRenderFillColor(dc);
//         else
//             return null;
//     }
    
//     public java.awt.Color getFillColor()
//     {
//         if (FancyGroups)
//             return super.getFillColor();
//         else
//             return null;
//     }

    @Override
    public void setMapLocation(double x, double y) {
        if (isForSelection) {
            final double dx = x - getX();
            final double dy = y - getY();
            translateSelection(dx, dy);
            super.setLocation((float)x, (float)y);
        } else
            super.setMapLocation(x, y);
    }
    
    private void translateSelection(double dx, double dy)
    {
        for (LWComponent c : getChildren()) {

            // If parent and some child both in selection and you drag, the selection
            // (an LWGroup) and the parent fight to control the location of the child.
            // There may be a cleaner way to handle this, but checking it here works.
            // Also, do NOT skip if we're in a group -- that condition is caught below.
            // (could check for selected ancestor of type LWGroup.class)

            if (c.isSelected() && c.isAncestorSelected())
                continue;
            else
                c.translateOnMap(dx, dy);
        }
    }
    
    @Override
    public void setLocation(float x, float y) {
        if (isForSelection)
            Util.printStackTrace("setLocation on selection group " + x + "," + y + " " + this);
        else
            super.setLocation(x, y);
    }

    private boolean linksAreTranslatingWithUs = false;
    @Override
    protected void notifyMapLocationChanged(LWComponent src, double mdx, double mdy) {
        if (!isForSelection) {
            try {
                // this is just an optimization -- it's okay to over-normalize, but it
                // makes sorting through the resulting diagnostic event stream easier.
                linksAreTranslatingWithUs = true;
                super.notifyMapLocationChanged(src, mdx, mdy);
            } finally {
                linksAreTranslatingWithUs = false;
            }
        }
    }

    @Override
    void broadcastChildEvent(LWCEvent e)
    {
        if (mXMLRestoreUnderway) {
            // okay to skip the actual event broadcast: nobody should be listening to us
            return;
        }

        // Until we can know if it's a bounds event (or have a layout event),
        // just make the bounds dirty no matter what, and just in case update
        // any connected links
        updateConnectedLinks(null);

        //if (DEBUG.EVENTS && DEBUG.CONTAINMENT) System.out.println(e + "; broadcastChildEvent inside " + this);
        
        super.broadcastChildEvent(e);

        // we don't actually want to req-request this during normalization if the
        // children are deliverying real child location events (unless using
        // takeLocation), but normalization runs during cleanup in the undo manager, and
        // until it's complete, the existing normalization task is still in the
        // UndoManager's cleanup queue, so we won't add another one.
        
        if (e.hasOldValue() && !linksAreTranslatingWithUs) {
            // only request cleanup if this event is real enough to be undoable
            // we're not interested in info-only events (really, we're only
            // interested in events that are "isBoundsEvents", but we
            // don't support that in Key's yet -- e.g., no point in responding
            // to color value changes -- no change of that changing our bounds.
            requestCleanup(e);
        }
    }

    @Override
    public void notifyHierarchyChanged() {
        requestCleanup("hierarchyChanged");
        super.notifyHierarchyChanged();
    }
    

    /** Overridden in to handle special selection LWGroup: if is asked to draw itself into another context (e.g., on an image),
     * it won't bother to transform locally -- just draw the children as they are.
     */
     
    @Override
    public void draw(DrawContext dc) {
        if (isForSelection)
            drawChildren(dc);
        else
            super.draw(dc);
    }

    private final Color getPathwayColor() {
        final LWPathway exclusive = getExclusiveVisiblePathway();
        if (exclusive != null)
            return exclusive.getColor();
        else if (inPathway(VUE.getActivePathway()) && VUE.getActivePathway().isDrawn())
            return VUE.getActivePathway().getColor();
        else
            return null;
    }

    private static final boolean TrackPathwayColor = false;

    // We won't need this at all assumung we go back to handled only as pathway overlap
    @Override
    public Color getRenderFillColor(DrawContext dc)
     {
         if (TrackPathwayColor && dc != null && dc.drawPathways() && isTransparent()) {
             return getPathwayColor();
//              final LWPathway exclusive = getExclusiveVisiblePathway();
//              if (exclusive != null)
//                  return exclusive.getColor();
//              else if (inPathway(VUE.getActivePathway()) && VUE.getActivePathway().isDrawn())
//                  return VUE.getActivePathway().getColor();
         }
         //return null;
         return super.getRenderFillColor(dc);
    }

    @Override
    protected void drawImpl(DrawContext dc)
    {
        // TODO: DON'T DRAW ANYTHING BUT CHILDREN IF WE'RE FILTERED OUT
        
        if (DEBUG.CONTAINMENT && !isForSelection) {
            java.awt.Shape shape = getZeroShape();
            dc.g.setColor(new java.awt.Color(64,64,64,64));
            dc.g.fill(shape);
            dc.g.setColor(java.awt.Color.blue);
            dc.setAbsoluteStroke(1.0);
            dc.g.draw(shape);
        }

        final Color fill;
        
        if (TrackPathwayColor) {
            if (dc.drawPathways() && dc.focal != this && isTransparent()) {
                final Color c = getPathwayColor();
                if (c != null)
                    fill = c;
            } else
                fill = getFillColor();
        } else {
            // this REALLY should be an overlay (or just the old style border) not a back fill, to make clear this isn't
            // a fill color on the object.
            fill = getFillColor();
        }

        if (fill != null && fill.getAlpha() != 0)  {
            dc.g.setColor(fill);
            dc.g.fill(getZeroShape());
        }
        
        if (getStrokeWidth() > 0) {
            dc.g.setStroke(this.stroke);
            dc.g.setColor(getStrokeColor());
            dc.g.draw(getZeroShape());
        }
        
        drawChildren(dc);

//         if (FancyGroups)
//             // draw fill, border & children
//             super.drawImpl(dc);
//         else
//             // don't draw fill or border
//             drawChildren(dc);

        if (dc.isInteractive()) {
            if (isSelected() && dc.focal != this) {
                final Shape shape = getZeroShape();
                if (DEBUG.CONTAINMENT) out("drawing selection bounds shape " + shape);
                dc.g.setColor(COLOR_HIGHLIGHT);
                dc.g.fill(shape);
            } else if (isZoomedFocus() && !hasDecoratedFeatures()) {
                dc.setAbsoluteStroke(1);
                dc.g.setColor(COLOR_SELECTION);
                dc.g.draw(getZeroShape());
            }
        }

    }


    /** @return 1 */
    @Override
    public int getPickLevel() {
        return 1;
    }

    @Override
    protected LWComponent pickChild(PickContext pc, LWComponent c) {
        // needed for groups embeeded in groups:
        if (pc.pickDepth > 0)
            return c;
        else if (c.isPathwayOwned() && c instanceof LWSlide)  // to allow slide icon picking 
            return c;
        else
            return this;
    }

    
    /** @return false unless decordated: groups contain no points themselves --
     * only a point over a child is "contained" by the group. If decorated,
     * the standard impl applies of containing any point in the bounding box.
     */
    @Override
    protected boolean containsImpl(final float x, final float y, PickContext pc) {
        if ((pc.isZoomRollover && pc.pickDepth < 1) || hasDecoratedFeatures() || pc.root == this) {
            // added check for us being the pick root (focal) so double-click would work to
            // get out of a zoomed focus
            return super.containsImpl(x, y, pc);
        } else
            return false;
    }

    @Override
    protected boolean intersectsImpl(final Rectangle2D rect)
    {
        if (isForSelection)
            return true;
        
        if (hasDecoratedFeatures()) {
            return super.intersectsImpl(rect);
        } else {
            for (LWComponent c : getChildren())
                if (c.intersects(rect))                 // todo: not in parent coords (?)
                    return true;
            return false;
        }
    }

    private boolean hasDecoratedFeatures() 
    {
        return getStrokeWidth() > 0 || !isTransparent();
        
//         if (FancyGroups)
//             return getStrokeWidth() > 0 || !isTransparent();
//         else
//             return getEntryToDisplay() != null;
    }
    
    
}


// TODO: ResizeControl still goes haywire if the group is in a scaled context, (is a
// grand-child or deeper) tho at least it seems to stay local to the group and not start
// translating all over the map -- we could maybe even live with this.

// minor bug: if you group two nodes in list as child nodes, you get a tiny messy looking group.
// should probably just not allow this.

// minor bug (still?): when in a scaled context (grand-child or deeper), the bounds effect
// of the stroke width of group members is being understated, so the group
// bounds are a bit too small -- only show's up on selection tho (only
// time we currently show group bounds), and it's a only off by a very small
// amount -- not really noticable unless a very fat stroke and/or at huge zoom.


