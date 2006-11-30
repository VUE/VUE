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
 * @version $Revision: 1.1 $ / $Date: 2006-11-30 16:48:40 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class LWTraversal {

    protected static final boolean PRE_ORDER = true;
    protected static final boolean POST_ORDER = false;

    protected final boolean preOrder;
    protected boolean done = false;
    protected int depth = 0;

    /** Note: if preOrder is true, a node can be visited if accept(node) is true, even if acceptTraversal(node) is false */
    LWTraversal(boolean preOrder) {
        this.preOrder = preOrder;
    }

    LWTraversal() {
        this(POST_ORDER);
    }

    public void traverse(LWComponent c) {

        if (preOrder && accept(c)) {
            visit(c);
            if (done) return;
        }
            
        if (acceptTraversal(c)) {
            if (DEBUG.PICK) c.out("ACCEPTED TRAVERSAL");
            depth++;
            traverseChildren(c.getChildList());
            depth--;
            if (done) return;
            if (!preOrder && accept(c))
                visit(c);
        }
    }
        
    public void traverseChildren(java.util.List<LWComponent> children)
    {
        // default behaviour of traversals is to traverse list in reverse so as 
        // that top-most components are seen first
            
        for (java.util.ListIterator<LWComponent> i = children.listIterator(children.size()); i.hasPrevious();) {
            traverse(i.previous());
            if (done) return;
        }
    }

        
    /** It's possible we may accept the traversal of an object, without accepting it for a visit
     * (e.g., we're only interesting in visiting children).  However, if we're POST_ORDER,
     * and a node is NOT accepted for traversal, it is also not accepted for visiting.
     */
    public boolean acceptTraversal(LWComponent c) { return c.hasChildren(); }
        
    /**
     * @return true if this component meets our criteria for visiting
     */
    public boolean accept(LWComponent c) { return true; }
        
    /** Visit the node: it's been accepted, now analyize and/or do something with it */
    public void visit(LWComponent c) {
        System.out.println("Visited " + c);
    }


    /**
     * The primary code for interative, GUI based traversals of the map.  Based on the PickContext,
     * this will visit all "relevant" nodes in a current viewer (not hidden, filtered, possibly of
     * a desired type (e.g., modified by the kind of tool currently active), etc.)
     */
    public static abstract class Picker extends LWTraversal
    {
        protected final PickContext pc;
        //protected final boolean strayChildren = true; // for now, always search for children even outside of bounds (performance issue only)

        Picker(PickContext pc) {
            this.pc = pc;
            if (DEBUG.PICK) System.out.println("Picker created: " + pc);
        }
    
        
        /** If we reject traversal, we are also rejecting all children of this object */
        public boolean acceptTraversal(LWComponent c) {
            if (c.isHidden())
                return false;
            else if (depth > pc.maxDepth)
                return false;
            else if (c.getLayer() > pc.maxLayer)
                return false;
            //else return strayChildren || c.contains(mapX, mapY); // for now, ALWAYS work as if strayChildren was true
            else
                return true;
        }
        
        /** Should we visit the given LWComponent to see if we might have picked it?
         * This is in effect a fast-reject.
         */
        public boolean accept(LWComponent c) {
            if (c == pc.excluded)
                return false;
            else if (c.isFiltered()) // note: children may NOT be filtered out, but default acceptTraversal will get us there
                return false;
            else if (pc.ignoreSelected && c.isSelected())
                return false;
            else if (pc.pickType != null && !pc.pickType.isInstance(c))
                return false;
            else
                return true;
        }

    
        /** create subclasses with this overridden to do useful stuff */
        public abstract void visit(LWComponent c);
        
    }

    public static class PointPick extends LWTraversal.Picker
    {
        final float mapX, mapY;

        private LWComponent hit;
        //private LWComponent loosePicked;

        public PointPick(PickContext pc, float x, float y) {
            super(pc);
            mapX = x;
            mapY = y;
        }

        public PointPick(MapMouseEvent e) {
            this(e.getViewer().getPickContext(), e.getMapX(), e.getMapY());
        }
        

        public void visit(LWComponent c) {

            if (DEBUG.PICK) c.out("PointPick VISITED");
            
            if (c.contains(mapX, mapY)) {
                // If we expand impl to handle the contained children optimization (non-strayChildren):
                //      Since we're POST_ORDER, if strayChildren is false, we already know this
                //      object contains the point, because acceptTraversal had to accept it.
                hit = c;
                done = true;
                return;
            }
        }

        public static LWComponent pick(PickContext pc, float mapX, float mapY) {
            return new PointPick(pc, mapX, mapY).traverseAndPick(pc.root);
        }
        public static LWComponent pick(MapMouseEvent e) {
            PointPick pick = new PointPick(e);
            pick.traverse(pick.pc.root);
            return pick.getPicked();
        }
        

        public LWComponent traverseAndPick(LWComponent root) {
            traverse(root);
            return getPicked();
        }

        public LWComponent getPicked() {
            LWComponent picked = null;

            if (hit != null) {
                LWContainer parent = hit.getParent();
                if (parent != null)
                    picked = parent.pickChild(pc, hit);
                if (picked == hit) {
                    // only make use of defaultPick if pickChild didn't
                    // already redirect us to something else
                    if (picked != null)
                        picked = picked.defaultPick(pc);
                }
            }

            if (picked == null) {

                // First past didn't turn anything up: try a second pass, looking for loose hits.
                // (E.g., the concave region of curved links, or just near-link hits)
                
                final java.util.List<LWComponent> looseHits = new java.util.ArrayList();
                Picker loosePick = 
                    new Picker(pc) {
                        public void visit(LWComponent c) {
                            if (c.looseContains(mapX, mapY))
                                looseHits.add(c);
                        }
                    };
                loosePick.traverse(pc.root);

                if (DEBUG.PICK) System.out.println("PointPick: loose hits: " + looseHits.size());
                if (looseHits.size() > 1) {
                    // More than one loose hit: find the closest
                    float minDist = Float.MAX_VALUE;
                    for (LWComponent c : looseHits) {
                        float dist = c.distanceToEdgeSq(mapX, mapY);
                        if (DEBUG.PICK) c.out("distance: " + Math.sqrt(dist));
                        if (dist < minDist) {
                            minDist = dist;
                            picked = c;
                        }
                    }
                } else if (looseHits.size() == 1)
                    picked = looseHits.get(0);
            }
            
            if (DEBUG.PICK) System.out.println("PointPick: hit: " + hit + " picked " + picked + "\n");
            
            return picked;
            
        }
        
    }


    public static class RegionPick extends LWTraversal.Picker
    {
        final java.awt.geom.Rectangle2D mapRect;
        final java.util.List<LWComponent> hits = new java.util.ArrayList();

        public RegionPick(PickContext pc, java.awt.geom.Rectangle2D mapRect) {
            super(pc);
            this.mapRect = mapRect;
        }

        public void visit(LWComponent c) {
            if (DEBUG.PICK) c.out("REGION VISITED");
            // region picks should never select the root object the region is
            // being dragged inside
            if (c != pc.root && c.intersects(mapRect))
                hits.add(c);

        }

        public static java.util.List<LWComponent> pick(PickContext pc, java.awt.geom.Rectangle2D mapRect) {
            return new RegionPick(pc, mapRect).traverseAndPick(pc.root);
        }
        

        public java.util.List<LWComponent> traverseAndPick(LWComponent root) {
            traverse(root);
            return hits;
        }
        
    }
    
    


    
}


