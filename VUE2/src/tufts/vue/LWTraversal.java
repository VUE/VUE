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

import java.util.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * Traversals are used to descend into a map, starting from any given root
 * node, and visiting desired nodes based on the accept and acceptTraversal
 * methods.  If acceptTraversal returns true, the node's children are
 * traversed, if not, all the children are excluded.  If accept returns
 * true, and all ancestorors acceptTraversal returned true, the node is visited.
 *
 * If POST_ORDER, an accepted node who's acceptTraversal returns false
 * is also not accepted, if PRE_ORDER, and accepted node is visited even
 * if it's acceptTraversal returns false.
 * 
 * This class is meant to be overriden to do something useful.
 *
 * @version $Revision: 1.51 $ / $Date: 2009-09-02 16:25:33 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */

// todo for core traversal functionality: add capability for handling
// LWComponent.ChildKind, so we have the option of traversing ChildKind.ANY, as opposed
// to the current impl, which does a traversal that gives us all the same components
// that ChildKind.PROPER does.

// todo: now that LWTraversal takes a PickContext, this is really no longer a generic
// traversal: it's a PickTraversal

public class LWTraversal {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWTraversal.class);

    protected static final boolean PRE_ORDER = true;
    protected static final boolean POST_ORDER = false;

    protected final boolean preOrder;
    protected boolean done = false;
    protected int depth = 0;
    
    protected final PickContext pc;
    
    private final List<LWComponent> pickCache = new ArrayList();
    private boolean iteratingPickCache;
    
    /** Note: if preOrder is true, a node can be visited if accept(node) is true, even if acceptTraversal(node) is false */
    LWTraversal(boolean preOrder, PickContext pc) {
        this.preOrder = preOrder;
        this.pc = pc;
        if (DEBUG.PICK && DEBUG.WORK) Log.debug("pick cache: " + Util.tags(pickCache));
    }

    LWTraversal(PickContext pc) {
        this(POST_ORDER, pc);
    }


    public void traverse(LWComponent c) {

        if (c == null)
            return;

        if (preOrder && accept(c)) {
            visit(c);
            if (done) return;
        }
            
        if (!acceptTraversal(c)) {
            if (DEBUG.PICK) eoutln("TRV DENY: " + c);
        } else {
            if (DEBUG.PICK) eoutln("Travers0: " + c);
            if (acceptChildren(c)) {
                if (DEBUG.PICK) eoutln("Travers1: " + c);
                depth++;

                List<LWComponent> pickables = null;

                final List<LWComponent> curCache;
                if (iteratingPickCache) {
                    
                    // We really only need this if getPickList actually returns slide icons,
                    // but we don't know that in advance, so we have to allocate just in case.
                    // This currently only happens if we encounter a node with a slide icon that
                    // is a descendent of another node that also has a slide icon.
                    
                    if (DEBUG.PICK && DEBUG.WORK) Log.debug("allocating tmp pick cache for " + c);
                    curCache = new ArrayList(); 
                } else {
                    curCache = pickCache;
                }
                
                try {

                    // todo performace: get rid of all this pickCache stuff, and change
                    // getPickList to getPickIter, and require it to iterate in the pick
                    // order (reverse of child order), and the impl's can use some kind
                    // of ReverseListIter by default, and combine it with some kind of
                    // GroupIterator that would wrap children iterator + seenSlideIcons
                    // iter (and then apply the reverse on top of it).

                    if (pc != null)
                        pickables = c.getPickList(pc, curCache);
                    else
                        pickables = c.getChildList();
                } catch (Throwable t) {
                    Util.printStackTrace(t, "pickList fetch failed for " + c + "; pickables=" + pickables);
                }

                if (depth > 15) {
                    Util.printStackTrace("aborting pick at depth " + depth + " in case of loop; pickables: " + pickables);
                    done = true;
                    return;
                }
                
                if (pickables == pickCache)
                    iteratingPickCache = true;

                try {

                    if (DEBUG.PICK && DEBUG.META) Util.dumpCollection(pickables);
                    traversePicks(c, pickables);
                } catch (Throwable t) {
                    Util.printStackTrace(t, "traversal failure on " + c + "; pickables=" + pickables);
                } finally {
                    depth--;
                    if (pickables == pickCache)
                        iteratingPickCache = false;
                }
                
//                 if (true || c.isManagingChildLocations())
//                     traverseChildrenZoomFocusIsUnderSiblings(c.getPickList(pc, pickList));
//                 //traverseChildrenZoomUnderSiblings(c.getChildList());
//                 else
//                     traverseChildren(c.getPickList(pc, pickList));
//                 //traverseChildren(c.getChildList());
                
            }
            if (done) return;
            if (!preOrder && accept(c))
                visit(c);
        }
    }

    public boolean acceptChildren(LWComponent c) {
        return true;
    }
        
    public void traversePicks(LWComponent curTop, java.util.List<LWComponent> children)
    {
        // if we encounder a zoomed rollover, all siblings get priority
        // (so you can get to siblings that might have been obscurved by it's increased size)

        // Note that this shouldn't be used in instances where the siblings might be actually
        // overlapping when non-zoomed, as we get flashing back and forth every time
        // the mouse moves.
        
        LWComponent zoomedFocus = null;
        
        if (DEBUG.PICK && DEBUG.META) eoutln("TRAVERSE " + Util.tags(children));
        for (ListIterator<LWComponent> i = children.listIterator(children.size()); i.hasPrevious();) {
            final LWComponent c = i.previous();
            if (c == curTop) {
                Util.printStackTrace("found local root in pick list (loop!), skipping: " + c);
                continue;
            }
            if (false && c.isZoomedFocus()) { // Sibling priority turned off 2008-04-22 -- SMF
                zoomedFocus = c;
            } else {
                traverse(c);
                if (done)
                    return;
            }
        }

        if (zoomedFocus != null) 
            traverse(zoomedFocus);
    }
    
//     public void traverseChildren(java.util.List<LWComponent> pickChildren)
//     {
//         // default behaviour of traversals is to traverse list in reverse so
//         // that top-most components are seen first

//         // TODO: could more cleanly handle our slide-icon hack by having a special
//         // call to ask for the child list, and if someone has a slide icon, return
//         // a list with that always at the the end (on top)

//         for (ListIterator<LWComponent> i = pickChildren.listIterator(pickChildren.size()); i.hasPrevious();) {
//             traverse(i.previous());
//             if (done)
//                 return;
//         }
//     }
    
//     public void traverseChildrenZoomFocusIsUnderSiblings(java.util.List<LWComponent> children)
//     {
//         // if we encounder a zoomed rollover, all siblings get priority
//         // (so you can get to siblings that might have been obscurved by it's increased size)

//         // Note that this shouldn't be used in instances where the siblings might be actually
//         // overlapping when non-zoomed, as we get flashing back and forth every time
//         // the mouse moves.
        
//         LWComponent zoomedFocus = null;
        
//         if (DEBUG.PICK && DEBUG.META) eoutln("TRAVERSE " + Util.tags(children));
//         for (ListIterator<LWComponent> i = children.listIterator(children.size()); i.hasPrevious();) {
//             final LWComponent c = i.previous();
//             if (c.isZoomedFocus()) {
//                 zoomedFocus = c;
//             } else {
//                 traverse(c);
//                 if (done)
//                     return;
//             }
//         }

//         if (zoomedFocus != null) 
//             traverse(zoomedFocus);
//     }

    

        
    /** It's possible we may accept the traversal of an object, without accepting it for a visit
     * (e.g., we're only interesting in visiting children).  However, if we're POST_ORDER,
     * and a node is NOT accepted for traversal, it is also not accepted for visiting.
     */
    public boolean acceptTraversal(LWComponent c) { return c.hasPicks(); }
    //public boolean acceptTraversal(LWComponent c) { return c.hasChidren(); }
        
    /**
     * @return true if this component meets our criteria for visiting
     */
    public boolean accept(LWComponent c) { return true; }
        
    /** Visit the node: it's been accepted, now analyize and/or do something with it */
    public void visit(LWComponent c) {
        eoutln("VISITED: " + c);
    }

    protected void eout(String s) {
        for (int x = 0; x < depth; x++) System.out.print("          ");
        System.out.print(s);
    }
    protected void eoutln(String s) {
        eout(s + "\n");
    }
    


    /**
     * The primary code for interative, GUI based traversals of the map.  Based on the PickContext,
     * this will visit all "relevant" nodes in a current viewer (not hidden, filtered, possibly of
     * a desired type (e.g., modified by the kind of tool currently active), etc.)
     */
    public static abstract class Picker extends LWTraversal
    {
        //protected final PickContext pc;
        //protected final boolean strayChildren = true; // for now, always search for children even outside of bounds (performance issue only)

        Picker(PickContext pc) {
            //this.pc = pc;
            super(pc);
            if (DEBUG.PICK) System.out.println("Picker created: " + getClass().getName() + "; " +  pc);
        }
    
        
        /** If we reject traversal, we are also rejecting all children of this object */
        @Override
        public boolean acceptTraversal(LWComponent target) {

            if (target == pc.dropping) {
                if (DEBUG.PICK) eoutln("DENIED: dropping == target: " + target);
                return false;
            }

            if (target != pc.root) {

                // The below checks never apply to the root
                
                if (target.isLocked()) {
                    if (DEBUG.PICK) eoutln("DENIED: locked " + target);
                    return false;
                }
                
                if (!target.isDrawn()) {
                    if (DEBUG.PICK) eoutln("DENIED: not-drawn " + target);
                    return false;
                }
                
                if (depth > pc.maxDepth) {
                    if (DEBUG.PICK) eoutln("DENIED: depth " + target + " depth " + depth + " > maxDepth " + pc.maxDepth);
                    return false;
                }
                
            }
            
            if (pc.dropping != null && target instanceof LWLink && pc.dropping instanceof LWComponent) {

                // If we're dragging a node around a map that has links to it, and
                // we drag it over another node to drop it in, we'd often hit (pick) the
                // connected links, and in this case, we clearly should just be ignoring
                // them (never picking any links connected to us).

                // This code would probably be better handled via some LWComponent API,
                // so we're not directly referring to LWLink here.
                if (((LWLink)target).hasEndpoint((LWComponent)pc.dropping)) {
                    if (DEBUG.PICK)
                        eoutln("DENIED: dropping: " + pc.dropping + "; onto connected link: " + target.getDiagnosticLabel());
                    return false;
                }
            }
                

            //else return strayChildren || c.contains(mapX, mapY); // for now, ALWAYS work as if strayChildren was true
            return true;
        }

        @Override
        public boolean acceptChildren(LWComponent c) {
            // NEED TO ALLOW THIS FOR GROUPS, so can
            // use our new clean pickDistance for finding
            // the children first, then letting the group's
            // pickChild choose itself or the child depending
            // on pickDepth -- can we integrate with our
            // getPickLevel() feature?  Just throw it out if we gotta..
            //return pc.pickDepth >= c.getPickLevel();
            //return true;
            //return c.hasChildren();
            if (c.isPathwayOwned() && c instanceof LWSlide && pc.root != c)
                return false;
            else
                return c.hasPicks();
        }
        

        /** Should we visit the given LWComponent to see if we might have picked it?
         * This is in effect a fast-reject.
         */
        @Override
        public boolean accept(LWComponent c) {
            if (c == pc.excluded)
                return false;
            else if (c.isFiltered()) // note: children may NOT be filtered out, but default acceptTraversal will get us there
                return false;
            else if (pc.ignoreSelected && c.isSelected())
                return false;
            //else if (pc.pickType != null && !pc.pickType.isInstance(c))
            else if (pc.acceptor != null && !pc.acceptor.accept(pc, c))
                return false;
            else
                return true;
        }

    
        /** create subclasses with this overridden to do useful stuff */
        public abstract void visit(LWComponent c);

    }

    public static class PointPick extends LWTraversal.Picker
    {
        private final Point2D.Float mapPoint = new Point2D.Float();
        private final Point2D.Float zeroPoint = new Point2D.Float();
            
        private LWComponent hit;
        private LWComponent closeHit;
        private float closestDistSq = Float.POSITIVE_INFINITY;

        public PointPick(PickContext pc) {
            super(pc);
            mapPoint.x = pc.x;
            mapPoint.y = pc.y;
        }

        public PointPick(MapMouseEvent e) {
            this(e.getViewer().getPickContext(e.getMapX(), e.getMapY()));
        }
        
//         public boolean acceptTraversal(LWComponent c) {
//             if (super.acceptTraversal(c)) {
//                 return true;
//             } else {
//                 return false;
//             }
//         }

        @Override
        public void visit(LWComponent c) {

            if (DEBUG.PICK) eout("   VISIT: " + c);

            // todo performance: if we don't ever move to having cached map transforms,
            // we could keep an AffineTransform in the picker, and for each traversal,
            // apply transformDownA on a clone of the top level object, passing that
            // transform down to further depth visits to be cloned and hava
            // transformDownA applied to it.  Then use that transform here to apply it's
            // inverse transform to the point.

            c.transformMapToZeroPoint(mapPoint, zeroPoint);
            //if (DEBUG.PICK && DEBUG.META) eoutln("relative pick: " + zeroPoint);

            // note: passing an uncloned PickContext down to each visited component
            // is a bit risky, as all implementations must be sure not to modify
            // it in any way.

            final float hitResult = c.pickDistance(zeroPoint.x,
                                                   zeroPoint.y,
                                                   pc);

            //if (DEBUG.PICK && DEBUG.META) {
            if (DEBUG.PICK) {
                if (hitResult > 0)
                    System.out.format("; distance=%.2f\n", Math.sqrt(hitResult));
                else
                    System.out.println("");
            }

            // Note that as soon as a we have a direct-hit, we stop checking for close
            // hits, even tho we may ultimately pick a close hit instead.  This sounds
            // wrong at first because how can we know the closest if we haven't checked
            // all possibilities?  This is okay because the only time we prioritize a
            // close-hit over a direct hit is if the close hit is a descendent of the
            // direct hit (the direct hit is the "background" of the close hit), and we
            // can only get a direct hit on an ancestor (background) of a close hit once
            // we've checked for close-hits on all it's descendents (picking is a
            // depth-first visiting operation).

            if (hitResult == 0) {
                // zero distance means direct hit within the visible bounds of the object
                hit = c;
                done = true;
            } else if (hitResult < 0) {
                // distance result -1: do nothing -- a complete miss
            } else if (hitResult < closestDistSq) {
                // the result is the square of the distance from the object
                closeHit = c;
                closestDistSq = hitResult;
            }

//             if (c.contains(p, pc.zoom)) {
//                 // If we expand impl to handle the contained children optimization (non-strayChildren):
//                 //      Since we're POST_ORDER, if strayChildren is false, we already know this
//                 //      object contains the point, because acceptTraversal had to accept it.
//                 //System.out.println("hit with " + p);
//                 hit = c;
//                 done = true;
//                 return;
//             }
            
        }
        
        public static LWComponent pick(PickContext pc) {
            return new PointPick(pc).traverseAndPick(pc.root);
        }
        
        public static LWComponent pick(MapMouseEvent e) {
            PointPick pick = new PointPick(e);
            return pick.traverseAndPick(pick.pc.root);
//             pick.traverse(pick.pc.root);
//             return pick.getPicked();
        }

        public LWComponent traverseAndPick(LWComponent root) {
            if (DEBUG.PERF) {
                final long start = System.nanoTime();
                traverse(root);
                final LWComponent pick = getPicked();
                final long delta = System.nanoTime() - start;
                Log.debug(String.format("PointPick elapsed: %.1fms", delta / 100000.0));
                return pick;
            } else {
                traverse(root);
                return getPicked();
            }
        }

        public LWComponent getPicked() {

            if (DEBUG.PICK) {
                if (pc.dropping != null) eoutln("PointPick:   DROPPING: " + Util.tags(pc.dropping));
                            eoutln("PointPick: DIRECT-HIT: " + hit);
                eout(String.format("PointPick:  CLOSE-HIT: %s; distance=%.2f;", closeHit, Math.sqrt(closestDistSq)));
            }

            if (hit == null || (closeHit != null && closeHit.hasAncestor(hit))) {

                // Anytime we have a close-hit on something and a direct hit on any
                // ancestor of the close-hit, we want to allow for the close-hit as
                // ancestors are considered "background" and should get lower priority.
                // We do NOT want to just always check for the close-hit, in case we
                // have a direct hit on say, a node, and a close hit on a sibling such
                // as a link that is connected to the node.  In that case we want to
                // stay with the direct hit on the node and ignore the link, no matter
                // how close we may be to it.  The one case this isn't ideal is if there
                // is a link to a node that is currently linking to it's center, and not
                // an edge, in which case the link actually does overlap it's sibling,
                // and we could theoretically find out which portion overlaps and allow
                // a close-hit on that, but this is a rare case and dealing with it
                // would hardly be worth it.

                final float closeEnoughSq;
                if (pc.zoom < 1) {
                    // allow more slop if zoomed way out (links are very small and hard to hit)
                    closeEnoughSq = (8 / pc.zoom) * (8 / pc.zoom);
                } else if (pc.zoom >= 4) {
                    final float zf = pc.zoom / 2;
                    closeEnoughSq = 8/zf * 8/zf;
                } else
                    closeEnoughSq = 8 * 8;
                if (DEBUG.PICK) System.out.format(" closeEnough=%.2f;", Math.sqrt(closeEnoughSq));
                //if (hit == null && closestDistSq < closeEnoughSq) {
                if (closestDistSq < closeEnoughSq) {
                    if (DEBUG.PICK) System.out.println(" (CLOSE ENOUGH)");
                    //if (DEBUG.PICK) eoutln("PointPick: closeHit: " + closeHit + " distance: " + Math.sqrt(closestDistSq));
                    hit = closeHit;
                } else {
                    if (DEBUG.PICK) System.out.println("");
                }
                
            } else {
                if (DEBUG.PICK) System.out.println("");
            }

            LWComponent picked = null;
            
            if (hit != null && hit.isPathwayOwned() && hit instanceof LWSlide) {
                
                // allow a slide-icon to be picked no matter what
                picked = hit;
                
            } else if (hit != null) {
                final LWContainer parent = hit.getParent();
                if (parent != null) {

                    // This is a special case for handling LWGroups's to replace our getPickLevel functionality:

                    final LWGroup topGroupAncestor = (LWGroup) parent.getTopMostAncestorOfType(LWGroup.class, pc.root);

                    if (topGroupAncestor != null) {

                        if (pc.pickDepth > 0) {
                            // DEEP PICK:
                            // Even if deep picking, only pick the deepest group we can find,
                            // not the contents.  TODO: Really, we should be picking the second
                            // top-most (that is, only penetrate through the top-level group when
                            // deep picking, not straight to the bottom.)
                            final LWGroup groupAncestor = (LWGroup) parent.getAncestorOfType(LWGroup.class, pc.root);
                            if (groupAncestor != topGroupAncestor)
                                picked = groupAncestor;
                        } else {
                            // SHALLOW PICK:
                            picked = topGroupAncestor;
                        }
                    }
                    
                    if (picked == null) {
                        // TODO FIX: if a CURVED LINK is a child of a slide (or a node or anything else for that matter),
                        // the curved link can actually be well outside it's parent, but still end up picking the parent
                        // here, because we were CLOSE ENOUGH on the child. (see test-linkhack.vue)
                        picked = parent.pickChild(pc, hit);
                    }
                    
                } else {
                    // would normally only get here for an LWMap                    
                    picked = hit; 
                }


                if (picked == hit) {
                    // only make use of defaultPick if pickChild didn't
                    // already redirect us to something else
                    if (picked != null)
                        picked = picked.defaultPick(pc);
                }

                if (picked != null && pc.dropping != null)
                    picked = picked.defaultDropTarget(pc);
            
            }

            if (picked != null && picked != pc.root && !picked.hasAncestor(pc.root)) {
                // Just in case, NEVER allow anything above the current pick root to be picked (e.g., above the current focal).
                // The group picking code is pretty hairy, and although it should catch this now,
                // we double-check here just in case.
                if (DEBUG.Enabled) Log.warn("PointPick: DENIED: " + picked + "; is above pick root: " + pc.root);
                picked = null;
            }

            //if (DEBUG.PICK || (DEBUG.DND && picked != null)) {
            if (DEBUG.PICK) {
                eoutln(Util.TERM_GREEN + "PointPick:     PICKED: " + picked + Util.TERM_CLEAR);
                if (DEBUG.PICK)
                    System.out.println("");
            }
            //else if (DEBUG.WORK && picked != null) System.out.println("PICKED " + picked);
            return picked;
        }
        
    }

    
    //public static Rectangle2D mapRect; // for testing rotated transformMapToZeroRect
    
    public static class RegionPick extends LWTraversal.Picker
    {
        final Rectangle2D mapRect;
        final java.util.List<LWComponent> hits = new java.util.ArrayList();

        public RegionPick(PickContext pc) {
            super(pc);
            this.mapRect = new Rectangle2D.Float(pc.x, pc.y, pc.width, pc.height);
        }

        @Override
        public void visit(LWComponent c) {
            if (DEBUG.PICK) eoutln("VISIT " + c);
            // region picks should never select the root object the region is
            // being dragged inside
            if (c != pc.root && c != pc.excluded && c.intersects(mapRect)) {
                if (DEBUG.PICK) eoutln("  HIT " + c);
                hits.add(c);
            }

        }

        @Override
        public void traverse(LWComponent c) {
            if (DEBUG.PICK) eoutln("RGN-TRVSE " + c);
            super.traverse(c);
        }

        public static java.util.List<LWComponent> pick(PickContext pc) {
            return new RegionPick(pc).traverseAndPick(pc.root);
        }
        

        public java.util.List<LWComponent> traverseAndPick(LWComponent root) {
            if (DEBUG.PERF) {
                final long start = System.nanoTime();
                traverse(root);
                final long delta = System.nanoTime() - start;
                Log.debug(String.format("RegionPick elapsed: %.1fms", delta / 100000.0));
                return hits;
            } else {
                traverse(root);
                return hits;
            }
        }
    }
    
    


    
}


