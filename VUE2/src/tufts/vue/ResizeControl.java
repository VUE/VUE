package tufts.vue;

import java.util.Iterator;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class ResizeControl implements LWSelection.ControlListener, VueConstants
{
    // todo: consider implementing as or optionally as (perhaps
    // depending on shape) a point-transforming resize that instead
    // of setting the bounding box & letting shape handle it,
    // transforms all the points in the shape manuall.  Wouldn't
    // want to do this for, say RoundRect, as would throw off
    // corner arcs I think, but, polygons > sides 4 and, of
    // course, you'll HAVE to have this if you want to support
    // arbitrary polygons!
        
    boolean active = false;
    LWSelection.ControlPoint[] handles = new LWSelection.ControlPoint[8];
        
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
            handles[i] = new LWSelection.ControlPoint(COLOR_SELECTION_HANDLE);
    }
        
    /** interface ControlListener */
    public LWSelection.ControlPoint[] getControlPoints() {
        return handles;
    }
        
    private boolean isTopCtrl(int i) { return i == 0 || i == 1 || i == 2; }
    private boolean isLeftCtrl(int i) { return i == 0 || i == 6 || i == 7; }
    private boolean isRightCtrl(int i) { return i == 2 || i == 3 || i == 4; }
    private boolean isBottomCtrl(int i) { return i == 4 || i == 5 || i == 6; }
        
    /** interface ControlListener handler -- for handling resize on selection */
    public void controlPointPressed(int index, MapMouseEvent e) {
        if (DEBUG.LAYOUT||DEBUG.MOUSE) System.out.println(this + " resize control point " + index + " pressed");
        mOriginalGroup_bounds = (Rectangle2D.Float) VUE.getSelection().getShapeBounds();
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
        
    void draw(DrawContext dc) { // debug -- will need MapViewer arg to re-enable this (put in DrawContext??)
        if (mNewDraggedBounds != null) {
            /*
              dc.g.setColor(Color.orange);
              dc.g.setStroke(STROKE_HALF);
              dc.g.draw(mapToScreenRect(mNewDraggedBounds));
              dc.g.setColor(Color.green);
              dc.g.draw(mapToScreenRect(mOriginalGroupULC_bounds));
              dc.g.setColor(Color.red);
              dc.g.draw(mapToScreenRect(mOriginalGroupLRC_bounds));
            */
        }
    }
        
    /** interface ControlListener handler -- for handling resize on selection */
    public void controlPointMoved(int i, MapMouseEvent e) {
        //System.out.println(this + " resize control point " + i + " moved");
            
        // control points are indexed starting at 0 in the upper left,
        // and increasing clockwise ending at 7 at the middle left point.
            
        /*
          if (isTopCtrl(i))    resize_box.setULY(e.getMapY());
          else if (isBottomCtrl(i)) resize_box.setLRY(e.getMapY());
          if (isLeftCtrl(i))   resize_box.setULX(e.getMapX());
          else if (isRightCtrl(i))  resize_box.setLRX(e.getMapX());
        */
            
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
            
        double scaleX;
        double scaleY;
            
        /*
          if (isLeftCtrl(i)) {
          scaleX = mNewDraggedBounds.width / mOriginalGroup_bounds.width;
          scaleY = mNewDraggedBounds.height / mOriginalGroup_bounds.height;
          } else {
          scaleX = mNewDraggedBounds.width / mOriginalGroup_bounds.width;
          scaleY = mNewDraggedBounds.height / mOriginalGroup_bounds.height;
          }
        */
            
        scaleX = mNewDraggedBounds.width / mOriginalGroup_bounds.width;
        scaleY = mNewDraggedBounds.height / mOriginalGroup_bounds.height;
        //dragResizeReshapeSelection(i, VUE.getSelection().iterator(), VUE.getSelection().size() > 1 && e.isAltDown());
            
        dragResizeReshape(i,
                          VUE.getSelection().iterator(),
                          scaleX, scaleY,
                          VUE.getSelection().size() == 1 || e.isAltDown());
    }
        
    /** @param cpi - control point index (which ctrl point is being moved) */
    // todo: consider moving this code to LWGroup so that they can resize
    private void dragResizeReshape(int cpi, Iterator i, double dScaleX, double dScaleY, boolean reshapeObjects) {
        int idx = 0;
        //System.out.println("scaleX="+scaleX);System.out.println("scaleY="+scaleY);
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c instanceof LWLink) // must match conditinal aboice where we collect original_lwc_bounds[]
                continue;
            if (c.getParent().isSelected()) // skip if our parent also being resized -- race conditions possible
                continue;
            Rectangle2D.Float c_original_bounds = original_lwc_bounds[idx++];
            //Rectangle2D.Float c_new_bounds = new Rectangle2D.Float();
                
            if (c.supportsUserResize() && reshapeObjects) {
                //-------------------------------------------------------
                // Resize
                //-------------------------------------------------------
                if (DEBUG.LAYOUT) System.out.println("ScaleX=" + dScaleX);
                if (DEBUG.LAYOUT) System.out.println("ScaleY=" + dScaleY);
                float c_new_width = (float) (c_original_bounds.width * dScaleX);
                float c_new_height = (float) (c_original_bounds.height * dScaleY);
                    
                c.setAbsoluteSize(c_new_width, c_new_height);
            }
                
            float scaleX = (float) dScaleX;
            float scaleY = (float) dScaleY;
                
            //-------------------------------------------------------
            // Don't try to reposition child nodes -- they're parents
            // handle they're layout.
            //-------------------------------------------------------
            if ((c.getParent() instanceof LWNode) == false) {
                //-------------------------------------------------------
                // Reposition (todo: needs work in the case of not resizing)
                //-------------------------------------------------------
                    
                // Todo: move this class to own file, and make
                // static methods that can operate on any collection
                // of lw components (not just selection) so could
                // generically use this for LWGroup resize also.
                // (or, if groups really just put everything in
                // the selection, it would automatically work).
                    
                    
                float c_new_x;
                float c_new_y;
                    
                if (reshapeObjects){
                    // when reshaping, we can adjust the component origin smoothly with the scale
                    // because their lower right edge is also growing with the scale.
                    c_new_x = mNewDraggedBounds.x + (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    c_new_y = mNewDraggedBounds.y + (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                } else {
                    // when just repositioning, we have to compute the new component positions
                    // based on their lower right corner.
                    float c_original_lrx = c_original_bounds.x + c_original_bounds.width;
                    float c_original_lry = c_original_bounds.y + c_original_bounds.height;
                    float c_new_lrx;
                    float c_new_lry;
                    float c_delta_x;
                    float c_delta_y;
                        
                    if (false&&isRightCtrl(cpi)) {
                        c_delta_x = (mOriginalGroupLRC_bounds.x - c_original_bounds.x) * scaleX;
                        c_delta_y = (mOriginalGroupLRC_bounds.y - c_original_bounds.y) * scaleY;
                        //c_delta_x = (c_original_bounds.x - mOriginalGroupLRC_bounds.x) * scaleX;
                        //c_delta_y = (c_original_bounds.y - mOriginalGroupLRC_bounds.y) * scaleY;
                    } else {
                        c_delta_x = (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                        c_delta_y = (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                    }
                        
                    //c_delta_x = (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    //c_delta_y = (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                        
                    if (false) {
                        // this was crap
                        c_new_lrx = c_original_lrx + c_delta_x;
                        c_new_lry = c_original_lry + c_delta_y;
                        //c_new_lrx = mNewDraggedBounds.x + (c_original_lrx - mOriginalGroup_bounds.x) * scaleX;
                        //c_new_lry = mNewDraggedBounds.y + (c_original_lry - mOriginalGroup_bounds.y) * scaleY;
                        c_new_x = c_new_lrx - c_original_bounds.width;
                        c_new_y = c_new_lry - c_original_bounds.height;
                            
                        // put back into drag region
                        c_new_x += mNewDraggedBounds.x;
                        c_new_y += mNewDraggedBounds.y;
                    } else {
                        c_new_x = mNewDraggedBounds.x + c_delta_x;
                        c_new_y = mNewDraggedBounds.y + c_delta_y;
                    }
                        
                        
                        
                    //c_new_x = mNewDraggedBounds.x + (c_original_bounds.x - mOriginalGroup_bounds.x) * scaleX;
                    //c_new_y = mNewDraggedBounds.y + (c_original_bounds.y - mOriginalGroup_bounds.y) * scaleY;
                        
                    // this moves everything as per regular selection drag -- don't think we'll need that here
                    //c_new_x = c_original_bounds.x + ((float)mapMouseDown.getX() - resize_box.lr.x);
                    //c_new_y = c_original_bounds.y + ((float)mapMouseDown.getY() - resize_box.lr.y);
                }
                    
                if (reshapeObjects){
                    if (isLeftCtrl(cpi)) {
                        if (c_new_x + c.getWidth() > resize_box.lr.x)
                            c_new_x = (float) resize_box.lr.x - c.getWidth();
                    }
                    if (isTopCtrl(cpi)) {
                        if (c_new_y + c.getHeight() > resize_box.lr.y)
                            c_new_y = (float) resize_box.lr.y - c.getHeight();
                    }
                }
                c.setLocation(c_new_x, c_new_y);
            }
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
