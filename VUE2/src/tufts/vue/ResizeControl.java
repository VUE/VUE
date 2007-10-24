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
    private Rectangle2D.Float mNewDraggedBounds;
    private Object[]  mOriginal_each_bounds; // Rectangle2D.Float for everything but links
    private Box2D resize_box = null;
    private Point2D mapMouseDown;

    ResizeControl() {
        for (int i = 0; i < handles.length; i++)
            handles[i] = new LWSelection.Controller();
    }
        
    /** interface ControlListener -- our control's are numbered starting at 0 in the upper left corner,
     * and increasing in index value in the clockwise direction.
     */
    public LWSelection.Controller[] getControlPoints(double zoom) {
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

        final LWSelection selection = VUE.getSelection().clone();
        
        //mOriginalGroup_bounds = (Rectangle2D.Float) selection.getShapeBounds();
        mOriginalGroup_bounds = (Rectangle2D.Float) selection.getBounds();

        if (mOriginalGroup_bounds == null) {
            // if some code is written that clears the selection at the wrong time, this might happen.
            System.err.println(this + " control point pressed with empty selection " + selection);
            if (DEBUG.Enabled) tufts.Util.printStackTrace();
            return;
        }
        
        if (DEBUG.LAYOUT) System.out.println(this + " originalGroup_bounds " + mOriginalGroup_bounds);
        mOriginalGroupULC_bounds = LWMap.getULCBounds(selection.iterator());
        mOriginalGroupLRC_bounds = LWMap.getLRCBounds(selection.iterator());
        resize_box = new Box2D(mOriginalGroup_bounds);
        mNewDraggedBounds = resize_box.getRect();
        //mNewDraggedBounds = (Rectangle2D.Float) mOriginalGroup_bounds.getBounds2D();
            
        //------------------------------------------------------------------
        // save the original locations & sizes of everything in the selection
        //------------------------------------------------------------------
            
        mOriginal_each_bounds = new Object[selection.size()];
        int idx = 0;
        for (LWComponent c : selection) {
            //System.out.println("PROCESSING " + c);
            if (c.isManagedLocation())
                continue;
            if (c instanceof LWLink) {
                mOriginal_each_bounds[idx] = ((LWLink)c).getMoveableControls().clone(); // be sure to clone, as this is changing all the time
                //c.out("ResizeControl GOT CONTROLS " + java.util.Arrays.asList(mOriginal_each_bounds[idx]));
            } else {
                //mOriginal_each_bounds[idx] = c.getShapeBounds();
                mOriginal_each_bounds[idx] = c.getBounds();
                if (DEBUG.LAYOUT) System.out.println(this + " " + c + " bounds " + c.getBounds());
                //if (DEBUG.LAYOUT) System.out.println(this + " " + c + " shapeBounds " + c.getShapeBounds());
            }
            idx++;
            //mOriginal_each_bounds[idx++] = (Rectangle2D.Float) c.getBounds();
        }
        mapMouseDown = e.getMapPoint();
    }
        
    void draw(DrawContext dc) { // this only called if viewer or layout debug is on
        if (mNewDraggedBounds != null) {
            MapViewer viewer = VUE.getActiveViewer();


            dc.g.setStroke(STROKE_TWO);
            dc.g.setColor(java.awt.Color.blue);
            dc.g.draw(viewer.mapToScreenRect(mOriginalGroup_bounds));

            dc.g.setStroke(STROKE_ONE);
            dc.g.setColor(java.awt.Color.red);
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
            
            dragReshapeSelection(i,
                                 VUE.getSelection(),
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
        //final boolean lockedLocation = c.getParent() instanceof LWNode; // todo: have a locked flag
        final boolean lockedLocation = c.isManagedLocation(); // todo: this also checks selection, which we may not want...

        final float requestWidth = request.width / c.getMapScaleF();
        final float requestHeight = request.height / c.getMapScaleF();

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
            final float oldX = c.getMapX();
            final float oldY = c.getMapY();

            // First set size and find out what size was actually
            // taken before adjusting location.  Would be better
            // to do this by getting the minimum size first, but
            // that's not working at the moment for floating text
            // layout's.
                    
            c.userSetSize(requestWidth, requestHeight, e);

            final float newWidth = c.getWidth();
            final float newHeight = c.getHeight();
            float newX, newY;

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

            if (moved) {
                
                //if (!c.hasAbsoluteMapLocation()) {
                if (true) {
                    if (DEBUG.WORK) System.out.format("RC: new absolute loc: %6.1f,%-6.1f; %s\n", newX, newY, c);
                    final Point2D.Float p = new Point2D.Float();
                    c.getParent().transformMapToZeroPoint(new Point2D.Float(newX, newY), p);
                    newX = p.x;
                    newY = p.y;
                    //newX -= c.getParent().getMapX();
                    //newY -= c.getParent().getMapY();
                    if (DEBUG.WORK) System.out.format("RC: new relative loc: %6.1f,%-6.1f; %s\n", newX, newY, c);
                }
                
                c.setLocation(newX, newY); // todo: setAbsoluteLocation would be nice
            }
                
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
    private void dragReshapeSelection(final int cpi,
                                      final LWSelection selection,
                                      final double dScaleX,
                                      final double dScaleY,
                                      final boolean reshapeObjects)
    {
        int idx = 0;
        //System.out.println("scaleX="+scaleX);System.out.println("scaleY="+scaleY);
        LWLink currentLink;
        for (LWComponent c : selection) {
            if (c.isManagedLocation()) // must match conditinal aboice where we collect mOriginal_each_bounds[] -- OVERKILL, allow reshaping of child (managed loc) objects
                continue;
            if (false && c.getParent().isSelected()) // skip if our parent also being resized -- race conditions possible -- todo: deeper nesting???
                continue;

            
            if (c instanceof LWLink) {
                int controlIndex = -1;
                Point2D.Float result = new Point2D.Float();
                for (Point2D.Float originalPoint : ((Point2D.Float[]) mOriginal_each_bounds[idx++])) {
                    controlIndex++;
                    if (originalPoint == null)
                        continue;
                    //c.out("HANDLING CPI " + controlIndex);
                    Point2D.Float newPoint = translatePoint(originalPoint, result);
                    ((LWLink)c).setControllerLocation(controlIndex, newPoint);
                }
                continue;
            }
            
            // TODO: need to change entire method to at least move object on-center, and
            // possible to support four different movement aspects: one for each
            // direction the selection edge is moving in (left/right/up/down), as what
            // we really want is for objects to never exceed the mo1ving edge.  If the
            // selection gets to small, they may exceed the non-moving edge of the
            // original group, where we'll start getting errors, tho we could also force
            // a stop a that point.
            
            // turn  on DEBUG.LAYOUT to see the red box that everything is actually
            // being laid-out inside...

            Rectangle2D.Float c_original_bounds = (Rectangle2D.Float) mOriginal_each_bounds[idx++];

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

            // TODO: even if managed location, if reshaping, should allow a child
            // of an object being reshaped to also be reshaped.
                
            //-------------------------------------------------------
            // Don't try to reposition child nodes -- their parents
            // handle their layout (todo: flag for this -- e.g. isManagedLocation)
            //-------------------------------------------------------
            //if ((c.getParent() instanceof LWNode) == false) {

            Point2D.Float centerPoint = null;
            if (true) { // if "reposition allowed"
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
                    // based on their lower right corner. (? is this still true?)

                    // dx/dy are the CUMULATIVE delta's from the position at the start of
                    // the drag operation

                    // CRAP, was this ever right?  This doesn't look normalized....
                    float dx = (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    float dy = (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                    
                    c_new_x = mNewDraggedBounds.x + dx;
                    c_new_y = mNewDraggedBounds.y + dy;

                    // This is better, in that everything gets squshed the same now matter from what direction, tho then
                    // we can get empty curves, which are blowing our bounds to infinity (i think) and
                    // the selection dissappearing... fix that before enabling this.
                    
                    /*
                    centerPoint = translatePoint(new Point2D.Float((float)c_original_bounds.getCenterX(),
                                                                   (float)c_original_bounds.getCenterY()));
                    */

                    
                    
                }
                    
                if (reshapeObjects) {
                    if (isLeftCtrl(cpi)) {
                        float c_width = resized ? c_new_width * c.getMapScaleF() : c.getWidth();
                        if (c_new_x + c_width > resize_box.lr.x)
                            c_new_x = (float) resize_box.lr.x - c_width;
                    }
                    if (isTopCtrl(cpi)) {
                        float c_height = resized ? c_new_height * c.getMapScaleF() : c.getHeight();
                        if (c_new_y + c_height > resize_box.lr.y)
                            c_new_y = (float) resize_box.lr.y - c_height;
                    }
                }
                repositioned = true;
            }

            //if (repositioned && !c.hasAbsoluteMapLocation()) {
            if (repositioned) {
                c_new_x -= c.getParent().getMapX();
                c_new_y -= c.getParent().getMapY();
                if (DEBUG.WORK) System.out.println("new relative loc: " + c_new_x + "," + c_new_y + " for " + c);
            }


            if (resized && repositioned) {
                c.userSetFrame(c_new_x, c_new_y,
                               c_new_width / c.getMapScaleF(),
                               c_new_height / c.getMapScaleF());
            } else if (resized) {
                c.userSetSize(c_new_width / c.getMapScaleF(),
                              c_new_height / c.getMapScaleF());
            } else if (repositioned) {
                if (centerPoint != null)
                    c.setCenterAt(centerPoint);
                else
                    c.userSetLocation(c_new_x, c_new_y);
            } else
                throw new IllegalStateException("Unhandled dragResizeReshape");

        }
    }

    private Point2D.Float translatePoint(Point2D.Float originalPoint)
    {
        return translatePoint(originalPoint, originalPoint);
    }
    private Point2D.Float translatePoint(Point2D.Float originalPoint, Point2D.Float result)
    {
        float normal_x = (originalPoint.x - mOriginalGroup_bounds.x);
        float normal_y = (originalPoint.y - mOriginalGroup_bounds.y);
        float ratio_x = normal_x / mOriginalGroup_bounds.width;
        float ratio_y = normal_y / mOriginalGroup_bounds.height;
        
        final Rectangle2D.Float newBounds = mNewDraggedBounds;
        //final Rectangle2D.Float newBounds = (Rectangle2D.Float) selection.getBounds(); // grows continually on it's own... (rounding error?)
        
        float new_normal_x = newBounds.width * ratio_x;
        float new_normal_y = newBounds.height * ratio_y;
        float new_x = newBounds.x + new_normal_x;
        float new_y = newBounds.y + new_normal_y;

        result.x = new_x;
        result.y = new_y;

        return result;


        /*
          System.out.format("RATIOX %.2f orig-x %.2f  normal-x %.1f  orig-width %.1f  new-width %.1f  new-normal-x %.1f\n",
          ratio_x,
          originalPoint.x,
          normal_x,
          mOriginalGroup_bounds.width,
          mNewDraggedBounds.width,
          new_normal_x
          );
        */
        
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
