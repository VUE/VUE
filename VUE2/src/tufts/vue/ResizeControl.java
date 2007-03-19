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

import java.util.Iterator;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class handles the resizing and/or reposition of both single
 * objects, and selected groups of objects.  If more than one object is selected,
 * the default is to reposition all the objects in the group as the
 * the total bounding box of the group is resized, maintaining the relative
 * spatial relationship of all the objects in the bounding box.
 * As the box is dragged, it uses an ad-hoc algorithm, as
 * the control point being moved is not guarnteed to stay exactly under
 * the mouse.
 *
 * @version $Revision: 1. $ / $Date: 2006/01/20 17:17:29 $ / $Author: sfraize $ 
 */
class ResizeControl implements LWSelection.ControlListener, VueConstants
{
    // todo: consider implementing as or optionally as (perhaps
    // depending on shape) a point-transforming resize that instead
    // of setting the bounding box & letting shape handle it,
    // transforms all the points in the shape manualy.  Wouldn't
    // want to do this for, say RoundRect, as would throw off
    // corner arcs I think, but, polygons > sides 4 and, of
    // course, you'll HAVE to have this if you want to support
    // arbitrary polygons!
        
    boolean active = false;
    LWSelection.Controller[] handles = new LWSelection.Controller[8];
        
    // These are all in MAP coordinates
    private Rectangle2D.Float mOriginalGroup_bounds;
    private Rectangle2D.Float mOriginalGroupULC_bounds;
    private Rectangle2D.Float mOriginalGroupLRC_bounds;
    private Rectangle2D.Float mCurrent;
    private Rectangle2D.Float mNewDraggedBounds;
    private Rectangle2D.Float[] original_lwc_bounds;
    private Box2D resize_box = null;
    private Point2D mapMouseDown;

    ResizeControl() {
        for (int i = 0; i < handles.length; i++)
            handles[i] = new LWSelection.Controller();
    }
        
    /** interface ControlListener -- our control's are numbered starting at 0 in the upper left corner,
     * and increasing in index value in the clockwise direction.
     */
    public LWSelection.Controller[] getControlPoints() {
        return handles;
    }
        
    private boolean isTopCtrl(int i) { return i == 0 || i == 1 || i == 2; }
    private boolean isLeftCtrl(int i) { return i == 0 || i == 6 || i == 7; }
    private boolean isRightCtrl(int i) { return i == 2 || i == 3 || i == 4; }
    private boolean isBottomCtrl(int i) { return i == 4 || i == 5 || i == 6; }
    /** this ctrl point can effect only the size, never the location */
    private boolean isSizeOnlyCtrl(int i) { return i >= 3 && i <= 5; }
        
    /** interface ControlListener handler -- for handling resize on selection */
    public void controlPointPressed(int index, MapMouseEvent e) {
        if (DEBUG.LAYOUT||DEBUG.MOUSE) System.out.println(this + " resize control point " + index + " pressed");
        mOriginalGroup_bounds = (Rectangle2D.Float) VUE.getSelection().getShapeBounds();
        
        if (mOriginalGroup_bounds == null) {
            // if some code is written that clears the selection at the wrong time, this might happen.
            System.err.println(this + " control point pressed with empty selection " + VUE.getSelection());
            if (DEBUG.Enabled) tufts.Util.printStackTrace();
            return;
        }
        
        if (DEBUG.LAYOUT) System.out.println(this + " originalGroup_bounds " + mOriginalGroup_bounds);
        mOriginalGroupULC_bounds = LWMap.getULCBounds(VUE.getSelection().iterator());
        mOriginalGroupLRC_bounds = LWMap.getLRCBounds(VUE.getSelection().iterator());
        resize_box = new Box2D(mOriginalGroup_bounds);
        mNewDraggedBounds = resize_box.getRect();
        //mNewDraggedBounds = (Rectangle2D.Float) mOriginalGroup_bounds.getBounds2D();
            
        //------------------------------------------------------------------
        // save the original locations & sizes of everything in the selection
        //------------------------------------------------------------------
            
        original_lwc_bounds = new Rectangle2D.Float[VUE.getSelection().size()];
        Iterator i = VUE.getSelection().iterator();
        int idx = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink)
                continue;
            original_lwc_bounds[idx++] = (Rectangle2D.Float) c.getShapeBounds();
            if (DEBUG.LAYOUT) System.out.println(this + " " + c + " shapeBounds " + c.getShapeBounds());
            //original_lwc_bounds[idx++] = (Rectangle2D.Float) c.getBounds();
        }
        mapMouseDown = e.getMapPoint();
    }
        
    void draw(DrawContext dc) { // this only called if viewer or layout debug is on
        if (mNewDraggedBounds != null) {
            MapViewer viewer = VUE.getActiveViewer();
            dc.g.setColor(java.awt.Color.red);
            dc.g.setStroke(STROKE_ONE);
            dc.g.draw(viewer.mapToScreenRect(mNewDraggedBounds));
            if (false) {
                dc.g.setColor(java.awt.Color.green);
                dc.g.draw(viewer.mapToScreenRect(mOriginalGroupULC_bounds));
                dc.g.setColor(java.awt.Color.red);
                dc.g.draw(viewer.mapToScreenRect(mOriginalGroupLRC_bounds));
            }
        }
    }
        
    /** interface ControlListener handler -- for handling resize on selection */
    public void controlPointMoved(int i, MapMouseEvent e) {
        //System.out.println(this + " resize control point " + i + " moved");
            
        // control points are indexed starting at 0 in the upper left,
        // and increasing clockwise ending at 7 at the middle left point.
            
        if (isTopCtrl(i)) {
            resize_box.setULY(e.getMapY());
        } else if (isBottomCtrl(i)) {
            resize_box.setLRY(e.getMapY());
        }
        if (isLeftCtrl(i)) {
            resize_box.setULX(e.getMapX());
        } else if (isRightCtrl(i)) {
            resize_box.setLRX(e.getMapX());
        }
            
        if (DEBUG.LAYOUT) System.out.println(this + " resize_box " + resize_box);
        mNewDraggedBounds = resize_box.getRect();
        if (DEBUG.LAYOUT) System.out.println(this + " draggedBounds " + mNewDraggedBounds);
            

        if (VUE.getSelection().size() == 1) {
            // only one item in the selection
            LWComponent c = VUE.getSelection().first();
            // todo: put the selection in the ControlListener interface: don't look it up here.
            if (c.supportsUserResize())
                dragReshape(i, c, mNewDraggedBounds, e);
        } else {
            final double scaleX = mNewDraggedBounds.width / mOriginalGroup_bounds.width;
            final double scaleY = mNewDraggedBounds.height / mOriginalGroup_bounds.height;
            
            dragReshapeGroup(i,
                             VUE.getSelection().iterator(),
                             scaleX, scaleY,
                             e.isAltDown()); // resize if ALT is down, otherwise reposition only
        }
    }
        
        
    /**
     * reshape a single component
     * @param controlPoint - which control handle is being dragged (numbered clockwise from 
     * @param c - the component to reshape
     * @param request - the new requested bounds
     */
    private void dragReshape(final int controlPoint, final LWComponent c, final Rectangle2D.Float request, MapMouseEvent e)
    {
        final boolean lockedLocation = c.getParent() instanceof LWNode; // todo: have a locked flag

        final float requestWidth = request.width / c.getScale();
        final float requestHeight = request.height / c.getScale();

        if (lockedLocation || isSizeOnlyCtrl(controlPoint)) {
            
            c.userSetSize(requestWidth, requestHeight, e);
            
        } else {

                
//             if (c instanceof LWImage) {
//                 // hack for LWImage's which handle this specially
//                 c.userSetFrame(request.x, request.y, request.width, request.height, e);
//                 return;
//             }

            // an origin control point is any control point that might
            // change the location

            final float oldWidth = c.getWidth();
            final float oldHeight = c.getHeight();
            final float oldX = c.getX();
            final float oldY = c.getY();

            // First set size and find out what size was actually
            // taken before adjusting location.  Would be better
            // to do this by getting the minimum size first, but
            // that's not working at the moment for floating text
            // layout's.
                    
            c.userSetSize(requestWidth, requestHeight, e);

            final float newWidth = c.getWidth();
            final float newHeight = c.getHeight();
            final float newX, newY;

            boolean moved = false;
            if (newWidth != requestWidth) {
                //System.out.println("width stuck at " + newWidth + " can't go to " + requestWidth);
                if (isLeftCtrl(controlPoint) == false || newWidth == oldWidth) {
                    // do NOT move the X coord (tho Y coord might be moving)
                    newX = oldX;
                } else {
                    float dx = oldWidth - newWidth;
                    newX = oldX + dx;
                    //System.out.println("\tdid manage get from " + oldWidth + " to " + newWidth + " dx=" + dx);
                    moved = true;
                }
            } else {
                newX = request.x;
                // may have moved
                moved = true;
            }
                
            if (newHeight != requestHeight) {
                //System.out.println("height stuck at " + newHeight + " can't go to " + requestHeight);
                if (isTopCtrl(controlPoint) == false || newHeight == oldHeight) {
                    // do NOT move the Y coord (tho X coord might be moving)
                    newY = oldY;
                } else {
                    float dy = oldHeight - newHeight;
                    newY = oldY + dy;
                    //System.out.println("\tdid manage get from " + oldHeight + " to " + newHeight + " dy=" + dy);
                    moved = true;
                }
            } else {
                newY = request.y;
                // may have moved
                moved = true;
            }

            if (moved)
                c.setLocation(newX, newY);
                
            // TODO: get rid of getMinumumSize unless we fix layout floating_text
            // to really return minimum
                
        }
    }

    
    // Todo: make static methods that can operate on any collection of
    // lw components (not just selection) so could generically use
    // this for LWGroup resize also.  (or, if groups really just put
    // everything in the selection, it would automatically work).
                    
    /** @param cpi - control point index (which ctrl point is being moved) */
    // todo: consider moving this code to LWGroup so that they can resize
    // Note: this method will still work with just one item in the iterator, but
    // it doesn't prevent moving the object when it should, which is why we have
    // dragReshape above.
    private void dragReshapeGroup(final int cpi,
                                  final Iterator i,
                                  final double dScaleX,
                                  final double dScaleY,
                                  final boolean reshapeObjects)
    {
        int idx = 0;
        //System.out.println("scaleX="+scaleX);System.out.println("scaleY="+scaleY);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink) // must match conditinal aboice where we collect original_lwc_bounds[]
                continue;
            if (c.getParent().isSelected()) // skip if our parent also being resized -- race conditions possible
                continue;
            Rectangle2D.Float c_original_bounds = original_lwc_bounds[idx++];

            boolean resized = false;
            boolean repositioned = false;
            float c_new_width = 0;
            float c_new_height = 0;
            float c_new_x = 0;
            float c_new_y = 0;
            
            if (c.supportsUserResize() && reshapeObjects) {
                //-------------------------------------------------------
                // Resize -- must be done before any repositioning
                //-------------------------------------------------------
                if (DEBUG.LAYOUT) System.out.println("dScaleX=" + dScaleX);
                if (DEBUG.LAYOUT) System.out.println("dScaleY=" + dScaleY);
                c_new_width = (float) (c_original_bounds.width * dScaleX);
                c_new_height = (float) (c_original_bounds.height * dScaleY);
                resized = true;
            }
                
            
            //-------------------------------------------------------
            // Don't try to reposition child nodes -- their parents
            // handle their layout (todo: flag for this)
            //-------------------------------------------------------
            if ((c.getParent() instanceof LWNode) == false) {
                //-------------------------------------------------------
                // Reposition (todo: needs work in the case of not resizing)
                //-------------------------------------------------------
                    
                float scaleX = (float) dScaleX;
                float scaleY = (float) dScaleY;
                    
                if (reshapeObjects) {
                    // Note: if we're handling a single selected object, reshapeObjects is always true.
                    // When reshaping, we can adjust the component origin smoothly with the scale
                    // because their lower right edge is also growing with the scale.
                    c_new_x = mNewDraggedBounds.x + (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    c_new_y = mNewDraggedBounds.y + (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                } else {
                    // when just repositioning, we have to compute the new component positions
                    // based on their lower right corner.
                    float c_delta_x = (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    float c_delta_y = (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                        
                    c_new_x = mNewDraggedBounds.x + c_delta_x;
                    c_new_y = mNewDraggedBounds.y + c_delta_y;
                }
                    
                if (reshapeObjects){
                    if (isLeftCtrl(cpi)) {
                        float c_width = resized ? c_new_width * c.getScale() : c.getWidth();
                        if (c_new_x + c_width > resize_box.lr.x)
                            c_new_x = (float) resize_box.lr.x - c_width;
                    }
                    if (isTopCtrl(cpi)) {
                        float c_height = resized ? c_new_height * c.getScale() : c.getHeight();
                        if (c_new_y + c_height > resize_box.lr.y)
                            c_new_y = (float) resize_box.lr.y - c_height;
                    }
                }
                repositioned = true;
            }

            if (resized && repositioned)
                c.userSetFrame(c_new_x, c_new_y, c_new_width / c.getScale(), c_new_height / c.getScale());
            else if (resized)
                c.userSetSize(c_new_width / c.getScale(), c_new_height / c.getScale());
            else if (repositioned)
                c.userSetLocation(c_new_x, c_new_y);
            else
                throw new IllegalStateException("Unhandled dragResizeReshape");

        }
    }
    
    /** interface ControlListener handler -- for handling resize on selection */
    public void controlPointDropped(int index, MapMouseEvent e) {
        //System.out.println("MapViewer: resize control point " + index + " dropped");
        mNewDraggedBounds = null;
        Actions.NodeMakeAutoSized.checkEnabled();
    }
        

    static class Box2D {
        // We need double precision to make sure our computed
        // width in getRect agrees with that of the given rectangle.
        
        Point2D.Double ul = new Point2D.Double(); // upper left corner
        Point2D.Double lr = new Point2D.Double(); // lower right corner
        
        public Box2D(Rectangle2D r) {
            ul.x = r.getX();
            ul.y = r.getY();
            lr.x = ul.x + r.getWidth();
            lr.y = ul.y + r.getHeight();
        }
        
        Rectangle2D.Float getRect() {
            Rectangle2D.Float r = new Rectangle2D.Float();
            r.setRect(ul.x, ul.y, lr.x - ul.x, lr.y - ul.y);
            return r;
        }
        
        // These set methods never let the box take negative width or height
        void setULX(float x) { ul.x = (x > lr.x) ? lr.x : x; }
        void setULY(float y) { ul.y = (y > lr.y) ? lr.y : y; }
        void setLRX(float x) { lr.x = (x < ul.x) ? ul.x : x; }
        void setLRY(float y) { lr.y = (y < ul.y) ? ul.y : y; }
        
        public String toString() {
            return "Box2D[" + ul + " -> " + lr + "]";
        }
    }

}
