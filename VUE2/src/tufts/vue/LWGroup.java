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
import static tufts.Util.*;

import java.util.*;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;


/**
 *
 * Manage a group of LWComponents -- does not have a fixed shape or paint any backround
 * or border -- simply used for moving, resizing & layering a collection of objects
 * together.
 *
 * @author Scott Fraize
 * @version $Revision: 1.71 $ / $Date: 2007-07-12 02:09:13 $ / $Author: sfraize $
 */
public class LWGroup extends LWContainer
{
    private static final boolean FancyGroups = true;
    private static final boolean AbsoluteChildren = false;

    private static final boolean ZERO_SCALE = false;

    public LWGroup() {
        if (!FancyGroups)
            disablePropertyTypes(KeyType.STYLE);
    }

//     @Override
//     public boolean supportsReparenting() {
//         //return !AbsoluteChildren;
//         // for now, allow reparenting to the map, but not to anything else
//         // TODO: boolean canParentTo(parentTarget ...
//         return getParent() instanceof LWMap == false;
//     }
    
//     /** @return true */
//     @Override
//     public boolean hasAbsoluteChildren() {
//         return AbsoluteChildren;
//     }

//     /** @return true (if groups absolute) -- this means parenting the group will not work as things stand */
//     @Override
//     public boolean hasAbsoluteMapLocation() {
//         return AbsoluteChildren;
//     }

    @Override
    public boolean supportsChildren() {
        return FancyGroups;
    }
    
    @Override
    public boolean supportsUserResize() {
//         if (FancyGroups)
//             return !isTransparent();
//         else
            return false;
    }

    @Override
    public boolean supportsUserLabel() {
        return false;
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
        super.children = selection;
    }

    /**
     * Create a new LWGroup, reparenting all the LWComponents
     * in the selection to the new group.
     */
    // not using this because we want the group on the map so it can deliver child
    // location events as they translated locally...
    static LWGroup create(LWSelection selection)
    {
        LWGroup group = new LWGroup();

        //group.setFrame(selection.getBounds());

        // Group location starts at 0,0 -- all non-link imported nodes
        // will have their location made relative to the group, which
        // means they won't change at all initially -- they we
        // normalize the group.
        
        group.setShapeFromContents(selection);
        group.importNodes(selection);
        System.out.println("CREATED: " + group);
        //group.normalize();
        //System.out.println("NORMALD: " + group);

        return group;
    }

    /** Make the given nodes members of this group, importing also any links that are been two members of the set.
     * This also makes sure to maintain the relative z-order of the imported nodes */
    public void importNodes(Collection<LWComponent> nodes)
    {
        // we enforce non-repeated memberships w/HashSet
        final Collection<LWComponent> allUniqueDescendents = new HashSet();
        final Collection<LWComponent> reparenting = 
            new HashSet<LWComponent>() {
                @Override
                public boolean add(LWComponent c) {
                    if (LWLink.LOCAL_LINKS && c instanceof LWLink && ((LWLink)c).isBound()) {
                        // don't add any links that are connected to anything: they'll reparent themselves
                        return false;
                    } else
                        return super.add(c);
                }
            }; 
        
        for (LWComponent c : nodes) {
            //if (c instanceof LWLink) // the only links allowed are ones we grab
            //  continue; // enabled in order to support reveal's with links in them
            reparenting.add(c);
            allUniqueDescendents.add(c);
            c.getAllDescendents(ChildKind.PROPER, allUniqueDescendents);
        }

        if (LWLink.LOCAL_LINKS == false) { // links auto-handle this with local-links

            //----------------------------------------------------------------------------------------
            // If both ends of any link are in the selection of what's being added to the
            // group, or are descendents of what's be added to the group, and that link's
            // parent is not already something other than the default link parent, scoop it
            // up as a proper child of the new group.
            //----------------------------------------------------------------------------------------

            final HashSet uniqueLinks = new HashSet();

            // TODO: need to update link membership when single items removed from groups...
            // TODO: change this entirely to be handled by a clean-up task in LWLink --
            //       will juse need an setEndpointHasReparented ala setEnpointMoved,
            //       and a bunch of testing...
        
            for (LWComponent c : allUniqueDescendents) {
                if (DEBUG.PARENTING) out("ALL UNIQUE " + c);
                for (LWLink l : c.getLinks()) {
                    boolean bothEndsInPlay = !uniqueLinks.add(l);
                    if (DEBUG.PARENTING) out("SEEING LINK " + l + " IN-PLAY=" + bothEndsInPlay);
                    //if (bothEndsInPlay && l.getParent() instanceof LWMap) { // why this LWMap check? is old in any case: need to allow slides
                    if (bothEndsInPlay && !(c.getParent() instanceof LWGroup)) { // don't pull out of embedded group
                        if (DEBUG.PARENTING) out("GRABBING " + c + " (both ends in group)");
                        reparenting.add(l);
                    }
                }
            }

        }

        // TODO: we grab links, even if they were down in another sub-group...
        // and those reparentings are not tracked (they're added here),
        // so they're not undoable...
        
        // Be sure to preserve the current relative ordering of all
        // these components the new group.
        addChildren(sort(reparenting, LWContainer.ReverseOrder));
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
        group.isForSelection = true;
        if (DEBUG.Enabled) group.setLabel("<=SELECTION=>");
        group.children = (java.util.ArrayList) selection.clone();
        group.setShapeFromChildren();
        if (DEBUG.CONTAINMENT) System.out.println("LWGroup.createTemporary " + group);
        return group;
    }

    
    protected Rectangle2D.Float getMapBounds(Iterable<LWComponent> iterable) {
        //return LWMap.getBounds(iterable.iterator());
        return LWMap.getPaintBounds(iterable.iterator());
    }
    
//     protected Rectangle2D.Float getChildBounds()
//     {
//         return LWMap.getBounds(getChildIterator());
//         //return LWMap.getPaintBounds(getChildIterator());
//     }

    protected void setShapeFromContents(Iterable<LWComponent> contents)
    {
        final Rectangle2D.Float bounds = getMapBounds(contents);
        super.setSize(bounds.width,
                      bounds.height);
        super.setLocation(bounds.x,
                          bounds.y);
                          
    }
    
    protected void setShapeFromChildren()
    {
        setShapeFromContents(getChildList());
    }

    void normalize()
    {
        if (DEBUG.WORK) {System.out.println(); out("NORMALIZING");}
        
        final Rectangle2D.Float curBounds = super.getBounds();
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

            // TODO: prevent all the link endpoint moved events?  The absolute position
            // of no component in the map, except the group itself, should actually be
            // changing as a result of the normalization...
            
            super.setLocation(getX() + dx,
                              getY() + dy,
                              this,
                              false);

            // Could theoretically handle this via mapLocationChanged calls, if above
            // was a real call to setLocation, but then there'd have to be a check
            // to see if the parent specifically was an LWGroup.
            
            if (DEBUG.WORK) out("normalizing relative children: dx=" + dx + " dy=" + dy);
            for (LWComponent c : getChildList()) {

                if (c.hasAbsoluteMapLocation()) {

                    // If an object has absolute map location (e.g., a LWLink), we don't
                    // need to do anthing to the location of any of their sub-parts when
                    // the group location changes (during normalization) because all
                    // their coordinates are at the map-level, -- they're not relative
                    // to the location of the group.
                    
                } else {
                    
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

                    if (c instanceof LWLink) {
                        // links to links can get out of sync with updates
                        // depending on the order they exist in the child list.
                        // this should help for at least most first tier cases:
                        ((LWLink)c).layout();
                    }
                }
            }
            
        }

        // todo minor: preNormalBounds was sometimes varying the size by miniscule
        // amounts, possibly due to rounding errors (espcially I think if there's a
        // link/curved link in the group?)  tho I'm not seeing this anymore -- may have
        // been something else.  Anyway, not a big deal -- is creating some extra size
        // events, but it could be cleaner.  We may have to convert everything to
        // doubles to really fix this, tho just changing LWGroups local bounds method
        // computation code to doubles might do it.
        
        if (DEBUG.WORK) {
            out(String.format("curShape %f,%f %fx%f", getX(), getY(), super.width, super.height));
            out(String.format("newShape %f,%f %fx%f", dx, dy, preBounds.width, preBounds.height));
        }
        
        super.setSize(preBounds.width, preBounds.height);
        if (DEBUG.WORK) out("NORMALIZED");
    }
    
    private Rectangle2D.Float getPreNormalBounds()
    {
        if (getChildList().size() < 2) // if only zero or one child, we should be about to disperse...
            return LWMap.EmptyBounds;
        
        Iterator<LWComponent> i = getChildList().iterator();
        final Rectangle2D.Float rect = getLocalizedPaintBounds(i.next());
        
        while (i.hasNext()) 
            rect.add(getLocalizedPaintBounds(i.next()));

        return rect;
    }

// NOT WHAT WE NEED: we need this on scaleNotify... or perhaps have a general hierachy
// event!  Which can tell us of our old parent, which we may end up needing if our
// LWContainer.establishLocalCoordinates doesn't set up us enough...
//     @Override
//     void setParent(LWContainer parent) {
//         Object oldParent = super.parent;
//         super.setParent(parent);
//         if (oldParent != null && !mXMLRestoreUnderway)
//             requestCleanup("reparented");
//     }


    // TODO: CRAP: align actions broken when in-group...
    // Anyway, if you enforce 1.0 scale inverstion for now and just
    // check stuff in, you should be good enough to get going...

    /** @return the paint bounds for the given object in our local coordinate space */
    private Rectangle2D.Float getLocalizedPaintBounds(LWComponent c)
    {
        if (c.hasAbsoluteMapLocation()) {
            // Currently, these are only be LWLink's.
            
            final Rectangle2D.Float r = c.getPaintBounds();
            
            // hack debugging scaled curved link bounds...  I'm thinking the problem is in textbox,
            // and maybe hasLabel always returning true... okay, that's not it.
            // Now we're not taking into account any label or stroke, and still it's messed when scaled...
            //final Rectangle2D.Float r = new Rectangle2D.Float(c.getX(), c.getY(), c.getWidth(), c.getHeight());
            
            final float scale = (float) this.getMapScale();
            if (DEBUG.CONTAINMENT && DEBUG.WORK) out("abs paint bounds: " + Util.out(r) + " " + c + " scale=" + scale);
            r.x -= getMapX(); // are these values good or in transition???  check moving a link northwest to see if we get behind...
            r.y -= getMapY();
            r.x /= scale;
            r.y /= scale;
            r.width /= scale;
            r.height /= scale;

            if (Float.isNaN(r.x) || Float.isNaN(r.y) || Float.isNaN(r.width) || Float.isNaN(r.height))
            
            // TODO: when scaled (at 0.75 first tier anyway), we appear to me missing a couple of pixels
            // of width at 100% map zoom...  doesn't seem to make any difference even if 10% zoom??

            
            if (DEBUG.CONTAINMENT && DEBUG.WORK) out("rel paint bounds: " + Util.out(r) + " " + c);
            return r;
        } else {
            return getParentLocalPaintBounds(c);
        }
        
    }

    /** @return the parent based, non-scaled bounds.  If the this component has absolute map location, we return getBounds() */
    private Rectangle2D.Float getParentLocalBounds(LWComponent c) {
        if (c.hasAbsoluteMapLocation())
            throw new Error("re-impl; getLocalizedPaintBounds should have handled");
            //return c.getBounds();
        else
            return new Rectangle2D.Float(c.getX(), c.getY(), c.getScaledWidth(), c.getScaledHeight());
        //return new Rectangle2D.Float(c.getX(), c.getY(), c.getMapWidth(), c.getMapHeight());
    }

    private Rectangle2D.Float getParentLocalPaintBounds(LWComponent c) {
        if (LWLink.LOCAL_LINKS && c instanceof LWLink) {
            // since this is in the group, we know the paint bounds will be local to parent bounds,
            // tho that API will probably be changing, and we'll need a getParentLocalPaintBounds on LWComponent
            // TODO: we should really get get
            return c.getPaintBounds();
//             if (mXMLRestoreUnderway)
//                 return c.getPaintBounds();
//             else
//                 return ((LWLink)c).getImmediateBounds();
        } if (c.hasAbsoluteMapLocation())
            throw new Error("re-impl; getLocalizedPaintBounds should have handled");
            //return c.getBounds();
        else
            return c.addStrokeToBounds(getParentLocalBounds(c), 0);
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
            
            if (getChildList().size() < 2) {
                if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("AUTO-DISPERSING on child count " + getChildList().size());
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

        if (DEBUG.CONTAINMENT) out("requestCleanup on " + srcMsg);
        
        final UndoManager um = getUndoManager();
        if (um != null && !um.isUndoing() && !um.hasLastTask(this)) {
            //if (DEBUG.Enabled) out(TERM_RED + "ADDING CLEANUP TASK on: " + srcMsg + TERM_CLEAR);
            //um.addCleanupTask(this, new DisperseOrNormalize(srcMsg));
            //super.addCleanupTask(new DisperseOrNormalize(srcMsg));
            um.addLastTask(this, new DisperseOrNormalize(srcMsg));
        }
    }
    
    @Override
    protected void removeChildren(Iterable<LWComponent> iterable)
    {
        super.removeChildren(iterable);
        requestCleanup("removeChildren");
    }

    @Override
    public void addChildren(Iterable<LWComponent> iterable) {
        super.addChildren(iterable);
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
            final LWContainer newParent = (LWContainer) getParentOfType(LWContainer.class);
            final List tmpChildren = new ArrayList(children);
            //if (DEBUG.PARENTING || DEBUG.CONTAINMENT) out("DISPERSING " + tmpChildren.size() + " children");
                
            // we can brute-force remove our children, to skip de-parenting events,
            // as this group will be dissapeared at the end of this operation anyway
            // TODO: this is probably screwing up UNDO tho...
            //this.children.clear();
            newParent.addChildren(tmpChildren);
        }
        getParent().deleteChildPermanently(this);
    }

    /* groups are always transparent -- defer to parent for background fill color */
    @Override
    public java.awt.Color getRenderFillColor(DrawContext dc)
    {
        if (FancyGroups)
            return super.getRenderFillColor(dc);
        
        if (dc != null && (dc.focal == this || getParent() == null))
            return dc.getFill();
        else if (getParent() != null)
            return getParent().getRenderFillColor(dc);
        else
            return null;
    }
    
    public java.awt.Color getFillColor()
    {
        if (FancyGroups)
            return super.getFillColor();
        else
            return null;
    }

//     // Crap: not very helpful: only called on top-level special selection group, which doesn't
//     // cascade down to calling userSetLocation / userTranslate, tho we could enforce that here...
//     @Override
//     public void userSetLocation(float x, float y)
//     {
//         final float dx = x - getX();
//         final float dy = y - getY();
//         if (isForSelection) {
//             translateChildren(dx, dy);
//             super.setLocation(x, y);
//             return;
//         }
//         super.setLocation(x, y);
//         for (LWComponent c : getChildList())
//             if (c.hasAbsoluteMapLocation())
//                 c.translate(dx, dy);
//     }


//     @Override
//     public void setProperty(final Object key, Object val)
//     {

//         // This is a bit of a hack, in that we're relying on the fact that the only
//         // thing to call setProperty with a Location key right now is the UndoManager
//         // The point being, on undo, we do NOT want to additionally translate any
//         // absolute children -- their location changes were already recorded and are
//         // will be undone on their own.

//         if (key == LWKey.Location) {
//             Point2D p = (Point2D) val;
//             super.setLocation((float) p.getX(), (float) p.getY());
//         } else
//             super.setProperty(key, val);
//     }

    // TODO: when an owning parent node contains this group, it will call setLocation,
    // translating abs children... what we want??
    
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
        for (LWComponent c : getChildList()) {

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

//     @Override
//     //public void setLocation(float x, float y)
//     public void setLocation(float x, float y, LWComponent hearableEventSource, boolean isUndo)
//     {
//         if (isForSelection) {
//             Util.printStackTrace("setLocation on selection group " + x + "," + y + " " + this);
//             return;
// //             final float dx = x - getX();
// //             final float dy = y - getY();
// //             translateSelection(dx, dy);
// //             super.setLocation(x, y);
//         } else {
//             // Even if our location has not changed relatively speaking, it may have changed in the map
//             // (if we're a child of something else)
//             // TODO: THIS DOESN'T WORK -- it's too late -- the parent as already moved, so mapX/mapY has
//             // already changed...  this is what we need the new API for...
// //             final float mx = getMapX();
// //             final float my = getMapY();
// //             out(String.format("     setLocation: %.1f,%.1f; mapX=%.1f mapX=%.1f", x, y, mx, my));
//             //out(String.format("     setLocation: %.1f,%.1f", x, y));
//             super.setLocation(x, y);
// //             final float nmx = getMapX();
// //             final float nmy = getMapY();
// //             final float dx = nmx - mx;
// //             final float dy = nmy - my;
// //             out(String.format("new map location: %.1f,%.1f  dmx=%.2f dmy=%.2f", nmx, nmy, dx, dy));
// //             if (dx != 0.0 || dy != 0.0) {
// //                 for (LWComponent c : getChildList()) {
// //                     if (c.hasAbsoluteMapLocation()) {
// //                         out(String.format("translate absolute child: dmx=%.1f dmy=%.1f: %s", dx, dy, c));
// //                         // TODO: make this a takeLocation (so no broadcast event), or do something to skip our normalization cleanup
// //                         c.translate(dx, dy); 
// //                     }
// //                 }
// //             } else {
// //                 out("NO ABSOLUTE MOVEMENT");
// //             }
//         }
//     }
    
    private boolean linksAreTranslatingWithUs = false;
    @Override
    protected void notifyMapLocationChanged(double mdx, double mdy) {
        if (!isForSelection) {
            try {
                // this is just an optimization -- it's okay to over-normalize, but it
                // makes sorting through the resulting diagnistic event stream easier.
                linksAreTranslatingWithUs = true;
                super.notifyMapLocationChanged(mdx, mdy);
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
        updateConnectedLinks();

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
    

    
    /** children of groups have absolute map locations -- this is overriden to a noop */
    @Override
    protected void establishLocalCoordinates(LWComponent c, LWContainer oldParent, double oldParentMapScale, float oldMapX, float oldMapY) {
        if (AbsoluteChildren)
            //; // do nothing: leave them absolute
            tufts.Util.printStackTrace("should not be called if children are absolute");
        else {
            //; // Do nothing for relative children:
            if (DEBUG.CONTAINMENT) out("establish local coords for: " + c);
            super.establishLocalCoordinates(c, oldParent, oldParentMapScale, oldMapX, oldMapY);
        }
    }

//     protected void translateRelativeChildren(float dx, float dy, LWComponent exclude) {
//         for (LWComponent c : getChildList()) {
//             if (c != exclude) {
//                 c.takeLocation(c.getX() + dx,
//                                c.getY() + dy);
//                 c.notify("location-group"); // mover map location changed, relative unchanged, all otherse the reverse
//             }
//         }
//     }

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
    

    @Override
    protected void drawImpl(DrawContext dc)
    {
        if (DEBUG.CONTAINMENT && !isForSelection) {
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
            if (DEBUG.CONTAINMENT) out("drawing selection bounds shape " + shape);
            dc.g.setColor(COLOR_HIGHLIGHT);
            dc.g.fill(shape);
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
        else
            return this;
    }

    
    /** @return false unless decordated: groups contain no points themselves --
     * only a point over a child is "contained" by the group. If decorated,
     * the standard impl applies of containing any point in the bounding box.
     */
    @Override
    protected boolean containsImpl(final float x, final float y, float zoom) {
        if (hasDecoratedFeatures())
            return super.containsImpl(x, y, zoom);
        else
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
    
//     @Override
//     void setScaleOnChild(double scale, LWComponent c)
//     {
//         // group handles the entire scale: children not scaled down further
//         // need to make sure this happens so on reparentings, if this
//         // node was previously scaled down (was a vanilla child node),
//         // it's scale gets's set back.
//         c.setScale(1.0);
//     }
    
    /** Currently ignored: always forces scale of 1.0 */
    @Override
    public void setScale(double scale) {
//         double curScale = getScale();
//         if (curScale != scale) {
//             super.setScale(scale);
//             notify(LWKey.Repaint);
//         }
        //super.setScale(0.5); // testing hack
        super.setScale(1.0);
        // With a proper scaleNotify similar to mapLocationChanged, we could
        // enforce an effective 1.0 scale if we want to!
        //super.setScale(1.0 / parent.getMapScale());
    }

    @Override
    public double getMapScale() {
        return ZERO_SCALE ? 1.0 : super.getMapScale();
    }
    
    @Override
    /** For now, return the inverse of the parent map scale, forcing a net scale of 1.0 */
    public double getScale() {
        return ZERO_SCALE ?
            (parent == null ? 1.0 : (1.0 / parent.getMapScale()))
            : super.getScale();
    } 


    
    public void setZoomedFocus(boolean tv)
    {
        // the group object never takes zoomed focus
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
    

    
}



// OKAY: scaling the group itself appears to be much more complicated than just allowing
// it to exist in an already scaled context (Besides ResizeControl going haywire,
// normalization becomes a nightmare).  Existing in a scaled context only currently
// happen if it's at least a grand-child of the node -- as the first child of a node,
// it's still at 100% -- this is more than good enough to support.  Actually, I even
// think that's working with ResizeControl now.

// So it may be that the only thing not working is the translation of the control points
// of CURVED LINKS when the group is the child of the node -- the control point stays
// put on the map (as it has abs map coords), tho the next time you do something in the
// group, it does get properly re-normalized, including the proper bounds of the the
// curved link, tho this is some unexpected jumping around of your group contents at
// that point, tho at least it appears to be properly normalizing / resizing.  The
// problem here is that the group's location isn't actually changing when it's the child
// of something else (due to relative coordinates), so setLocation, which even if called
// (which it is sometimes), has the same location, so there's no translation amount for
// contents with absolute coords.  We'd either need to know setLocation is being called
// and check for mapX/mapY change, or need another API for objects to let them know
// their absolute position has changed, just in case they want to do something about it
// (this could replace or be the place where updateConnctedLinks is called -- our
// existing calls to this could be our markers for where to install this new API).

// TODO: ResizeControl still goes haywire if the group is in a scaled context, (is a
// grand-child or deeper) tho at least it seems to stay local to the group and not start
// translating all over the map -- we could maybe even live with this.

// minor bug: if you group two nodes in list as child nodes, you get a tiny messy looking group.
// should probably just not allow this.

// minor bug: when in a scaled contect (grand-child or deeper), the bounds effect
// of the stroke width of group members is being understated, so the group
// bounds are a bit too small -- only show's up on selection tho (only
// time we currently show group bounds), and it's a only off by a very small
// amount -- not really noticable unless a very fat stroke and/or at huge zoom.


