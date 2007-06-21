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

import java.util.*;

import java.awt.Font;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.font.*;
import java.awt.geom.*;

import javax.swing.JTextArea;

/**
 * Draws a view of a Link on a java.awt.Graphics2D context,
 * and offers code for user interaction.
 *
 * Note that links have position (always their mid-point) only so that
 * there's a place to connect for another link and/or a place for
 * the label.  Having a size doesn't actually make much sense, tho
 * we inherit from LWComponent.
 *
 * @author Scott Fraize
 * @version $Revision: 1.154 $ / $Date: 2007-06-21 00:26:19 $ / $Author: sfraize $
 */
public class LWLink extends LWComponent
    implements LWSelection.ControlListener, Runnable
{
    public static final boolean LOCAL_LINKS = false;
    
    // Ideally, we want this to be false: it's a more accurate representation of
    // what's displayed: the control points only show up when selected.
    private static final boolean IncludeControlPointsInBounds = false;
    
    public final static Font DEFAULT_FONT = VueResources.getFont("link.font");
    public final static Color DEFAULT_LABEL_COLOR = java.awt.Color.darkGray;
    
    /** neither endpoint has arrow */   public static final int ARROW_NONE = 0;
    /** head has an arrow */            public static final int ARROW_HEAD = 0x1;
    /** tail has an arrow */            public static final int ARROW_TAIL = 0x2;
    /** both endpoints have arrows */   public static final int ARROW_BOTH = ARROW_HEAD + ARROW_TAIL;
    
    /** @deprecated -- use ARROW_HEAD */ public static final int ARROW_EP1 = ARROW_HEAD;
    /** @deprecated -- use ARROW_TAIL */ public static final int ARROW_EP2 = ARROW_TAIL;
    
    // todo: create set of arrow types
    private final static float ArrowBase = 5;
    private final static RectangularShape HeadShape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    private final static RectangularShape TailShape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);

    
    /**
     * Holds data and defines basic functionality for each endpoint.  Currently, we
     * always have exactly two endpoints, each of which may or not be connected to
     * another node.
     *
     * If we ever support more than one endpoint on a link (e.g., fan-out links), this
     * will give us a good start.
     */

    // consider subclassing Point2D.Float
    private static class End { 
        float x, y; // point at node where the connection is made, or disconnected map location
        LWComponent node; // if null, not connected
        boolean isPruned;
        double rotation; // normalizing rotation
        
        // maybe keep the parent of the endpoint node?
        //float lineX, lineY; // end of curve / line -- can be different than x / y if there is a connector shape
        //RectangularShape shape; // e.g. an arrow -- null means none
        
        final Point2D.Float point = new Point2D.Float();
        final Point2D.Float mapPoint = new Point2D.Float();
        
        // for control points
        float getX(LWContainer focal) {
            return node == null ? x : (float) node.getX(focal);
        }
        float getY(LWContainer focal) {
            return node == null ? y : (float) node.getY(focal);
        }

        boolean hasPrunedNode() {
            return node != null && node.isHidden(HideCause.PRUNE);
        }
        
        boolean isConnected() {
            return node != null;
        }
        
        boolean hasNode() {
            return node != null;
        }

        Point2D.Float getPoint() {
            point.x = x;
            point.y = y;
            return point;
        }

        Point2D.Float getMapPoint() {
            if (LOCAL_LINKS == false) {
                mapPoint.x = x;
                mapPoint.y = y;
            }
            
            return mapPoint;
        }

        //-----------------------------------------------------------------------------
        // Prune control support
        //-----------------------------------------------------------------------------

        float pruneCtrlOffset;

        private class PruneCtrl extends LWSelection.Controller {
            final AffineTransform tx = new AffineTransform();
            double ctrlRotation;
            void update(double onScreenScale) {
                super.x = super.y = 0;
                tx.setToTranslation(mapPoint.x, mapPoint.y);
                tx.rotate(rotation);
                tx.translate(0, pruneCtrlOffset / onScreenScale);
                tx.transform(this,this);
                setColor(isPruned ? Color.red : Color.lightGray);
                ctrlRotation = rotation + Math.PI / 4; // rotate to square parallel on line, plus 45 degrees to get diamond display
            }
        
            public final RectangularShape getShape() { return PruneCtrlShape; }
            public final double getRotation() { return ctrlRotation; }
        }

        // todo opt: could lazy create these...
        final PruneCtrl pruneControl = new PruneCtrl();

    };
    
    private final End head = new End();
    private final End tail = new End();

//     private boolean headNodeIsPruned() {
//         return head != null && head.isHidden(HideCause.PRUNE);
//     }
//     private boolean tailNodeIsPruned() {
//         return tail != null && tail.isHidden(HideCause.PRUNE);
//     }
//     private LWComponent head;
//     private LWComponent tail;
//     private transient double mRotationHead;
//     private transient double mRotationTail;
//     private transient AffineTransform mHeadCtrlTx = new AffineTransform();
//     private transient AffineTransform mTailCtrlTx = new AffineTransform();
//     private float headX;       // todo: either consistently use these or the values in mLine
//     private float headY;
//     private float tailX;
//     private float tailY;
//     private boolean headIsPruned, tailIsPruned;
    
    /** used when link is straight */
    private Line2D.Float mLine = new Line2D.Float();
    /** used when link is a quadradic curve (1 control point) */
    private QuadCurve2D.Float mQuad = null;
    /** used when link is a cubic curve (2 control points) */
    private CubicCurve2D.Float mCubic = null;
    /** convenience alias for current curve */
    private Shape mCurve = null;
    private transient float mCurveCenterX;
    private transient float mCurveCenterY;
    

    /** x/y point pairs on a flattened-into-segments version of the curve for hit detection */
    private float[] mPoints;
    /** current last real point in mPoints, which may contain unused values at the end */
    private int mLastPoint;
    /** the current total length of the line / curve (estimated by segments for curves) */
    private float mLength; 

    /** number of curve control points in use: 0=straight, 1=quad curved, 2=cubic curved */
    private int mCurveControls = 0; 
    
    private float centerX;
    private float centerY;
    
    private boolean ordered = false; // not doing anything with this yet
    
    /** has an endpoint moved since we last computed shape? */
    private transient boolean endpointMoved = true;

    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         11, 9,
                         Color.darkGray,
                         LWIcon.Block.HORIZONTAL,
                         LWIcon.Block.COORDINATES_MAP);


    /**
     * Used only for restore -- must be public
     */
    public LWLink() {
        initLink();
    }

    /**
     * Create a new link between two LWC's
     */
    public LWLink(LWComponent head, LWComponent tail)
    {
        initLink();
        //if (ep1 == null || ep2 == null) throw new IllegalArgumentException("LWLink: ep1=" + ep1 + " ep2=" + ep2);
        SetDefaults(this);
        setHead(head);
        setTail(tail);
        computeLink();
    }

    private void initLink() {
        disableProperty(KEY_FillColor);
    }
    
    static LWLink SetDefaults(LWLink l)
    {
        l.setFont(DEFAULT_FONT);
        l.setTextColor(DEFAULT_LABEL_COLOR);
        l.setStrokeWidth(1f); //todo config: default link width
        return l;
    }

    
    // FYI: javac (mac java version "1.5.0_07") complains about an incompatible return
    // type in getSlot here if we don't compile this file at the same time as
    // LWCopmonent.java... (this is a javac bug)
    public static final Key KEY_LinkArrows = new Key<LWLink,Object>("link.arrows", "vue-head;vue-tail") {
        final Property getSlot(LWLink l) {
            return l.mArrowState; // if getting a type-mismatch on mLine, feed this file to javac with LWComponent.java at the same time
        }
    };
    private final IntProperty mArrowState = new IntProperty(KEY_LinkArrows, ARROW_TAIL) {
            void onChange() { endpointMoved = true; layout(); }
        };


    public static final Key KEY_LinkShape = new Key<LWLink,Integer>("link.shape") { // do we want this to be a KeyType.STYLE? could argue either way...
        @Override
        public void setValue(LWLink link, Integer linkStyle) {
            link.setControlCount(linkStyle);
        }
        @Override
        public Integer getValue(LWLink link) {
            return link.getControlCount();
        }
    };

    /*
    public enum LinkStyle { STRAIGHT, QUAD_CURVED, CUBIC_CURVED; }
    public static final Key KEY_LinkShape = new Key<LWLink,LinkStyle>("link.shape") { // do we want this to be a KeyType.STYLE? could argue either way...
        @Override public void setValue(LWLink link, LinkStyle linkStyle) {
            link.setControlCount(linkStyle.ordinal());
        }
        @Override public LinkStyle getValue(LWLink link) {
            int cc = link.getControlCount();
            if (cc == 0)
                return LinkStyle.STRAIGHT;
            else if (cc == 1)
                return LinkStyle.QUAD_CURVED;
            else
                return LinkStyle.CUBIC_CURVED;
        }
    };
    */
    
    
    
    public static final Key KEY_LinkHeadPoint = new Key<LWLink,Point2D>("link.head.location") {
        @Override
        public void setValue(LWLink l, Point2D val) { l.setHeadPoint(val); }
        @Override
        public Point2D getValue(LWLink l) { return l.getHeadPoint(); }
    };
    public static final Key KEY_LinkTailPoint = new Key<LWLink,Point2D>("link.tail.location") {
        @Override
        public void setValue(LWLink l, Point2D val) { l.setTailPoint(val); }
        @Override
        public Point2D getValue(LWLink l) { return l.getTailPoint(); }
    };

    private final static String Key_Control_0 = "link.control.0";
    private final static String Key_Control_1 = "link.control.1";

    /**
     * @param key property key (see LWKey)
     * @return object representing appropriate value
     */
    public Object getPropertyValue(Object key)
    {
        //if (key == LWKey.LinkCurves)       return new Integer(getControlCount());else
        
             if (key == Key_Control_0)          return getCtrlPoint0();
        else if (key == Key_Control_1)          return getCtrlPoint1();
        else
            return super.getPropertyValue(key);
    }

    public void setProperty(final Object key, Object val)
    {
        //if (key == LWKey.LinkCurves)       setControlCount(((Integer) val).intValue());else
        
             if (key == Key_Control_0)          setCtrlPoint0((Point2D)val);
        else if (key == Key_Control_1)          setCtrlPoint1((Point2D)val);
        else
            super.setProperty(key, val);
    }

    @Override
    public boolean supportsUserLabel() {
        return true;
    }
    @Override
    public boolean supportsReparenting() {
        return false;
    }
    
    public boolean handleSingleClick(MapMouseEvent e)
    {
        // returning true will disallow label-edit
        // when single clicking over an icon.
        return mIconBlock.contains(e.getMapX(), e.getMapY());
    }
    
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        return mIconBlock.handleDoubleClick(e);
    }

    /** @return the component connected at the head end, or null if none or if it's pruned */
    public LWComponent getHead() {
        if (head.node == null || head.node.isHidden(HideCause.PRUNE))
            return null;
        else
            return head.node;
        //return headIsPruned ? null : head;
    }
    /** @return the component connected at the tail end, or null if none */
    public LWComponent getTail() {
        if (tail.node == null || tail.node.isHidden(HideCause.PRUNE))
            return null;
        else
            return tail.node;
        //return tailIsPruned ? null : tail;
    }

    public void setHeadPoint(float x, float y) {
        if (head.isConnected()) throw new IllegalStateException("Can't set pixel start point for connected link");
        Object old = new Point2D.Float(head.x, head.y);
        head.x = x;
        head.y = y;
        endpointMoved = true;
        notify(KEY_LinkHeadPoint, old);
    }

    public void setTailPoint(float x, float y) {
        if (tail.isConnected()) throw new IllegalStateException("Can't set pixel end point for connected link");
        Object old = new Point2D.Float(tail.x, tail.y);
        tail.x = x;
        tail.y = y;
        endpointMoved = true;
        notify(KEY_LinkTailPoint, old);
    }
    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e) {
        if (index == CPruneHead && head.hasNode()) {
            toggleHeadPrune();
        } else if (index == CPruneTail && tail.hasNode()) {
            toggleTailPrune();
        }
    }

    private void toggleHeadPrune() {
        pruneToggle(!head.isPruned, getEndpointChain(head.node));
        head.isPruned = !head.isPruned;
    }

    private void toggleTailPrune() {
        pruneToggle(!tail.isPruned, getEndpointChain(tail.node));
        tail.isPruned = !tail.isPruned;
    }


    private void pruneToggle(final boolean hide, Collection<LWComponent> bag) {
        for (LWComponent c : bag) {
            if (c == this)
                continue; // never hide us: the source of the prune
            if (hide)
                c.setHidden(HideCause.PRUNE);
            else
                c.clearHidden(HideCause.PRUNE);
        }
    }

    /** @return 1.0 -- links never scaled by themselves */
    @Override
    public double getScale() {
        return 1.0;
    }

    @Override
    void setScale(double scale) {
        ; // do nothing: links don't take on a scale of their own
    }

    // links are absolute in their parent: nothing to add to the transforms or local(raw) v.s. map shapes
    //@Override public Shape getMapShape() { return getShape(); }
    
    
    @Override
    public Rectangle2D.Float getBounds() {
        
        if (endpointMoved)
            computeLink();

        // as we currently always have absolute map location, we can just use getX/getY
        // tho we use getMapWidth/getMapHeight just in case we're in a scaled context
        // (tho we're trying to avoid this for now...)
        final Rectangle2D.Float bounds =
            addStrokeToBounds(new Rectangle2D.Float(getX(), getY(), getWidth(), getHeight()),
                              0);
        // do NOT want map-width / map-height: even if in a scaled parent, links are always pure on-map objects,
        // and the width/height is computed by our endpoints and controlpoints, set in computeLink --
        // there's never any scale to appeal to with links.
        //addStrokeToBounds(new Rectangle2D.Float(getX(), getY(), getMapWidth(), getMapHeight()),0);
                                                    
        // todo: would be better to just include this in the width via computeLink,
        // tho then we'd need to invalidate the link if the label changes.
        if (hasLabel())
            bounds.add(getLabelBox().getMapBounds());
        
        return bounds;
    }

    @Override
    public Rectangle2D.Float getPaintBounds()
    {
        return getBounds();
    }
    
    
    /** @return same as super class impl, but by default add our own two endpoints */
    @Override
    public Rectangle2D.Float getFanBounds(Rectangle2D.Float r)
    {
        final Rectangle2D.Float rect = super.getFanBounds(r);
        if (head.hasNode())
            rect.add(head.node.getBounds());
        if (tail.hasNode())
            rect.add(tail.node.getBounds());
        return rect;
    }
    
    public Collection<LWComponent> getEndpointChain(LWComponent endpoint) {
        HashSet set = new HashSet();
        set.add(this);
        // pre-add us to the set, so we can't back up through our other endpoint
        return endpoint.getLinkChain(set);
    }

    
    /**
     * @return all linked LWComponents: for a link, this is usually just it's endpoints,
     *  but like any other LWComponent, it will also include any other links that connect
     *  directly to us (IS EFFECTED BY PRUNING)
     */
    public Collection<LWComponent> getLinked(Collection bag) {
        if (head.hasNode() && !head.isPruned)
            bag.add(head.node);
        if (tail.hasNode() && !tail.isPruned)
            bag.add(tail.node);
        return super.getLinked(bag);
    }

    public Collection<LWComponent> getLinked() {
        return getLinked(new ArrayList(getLinks().size() + 2));
    }

    

    /* overrides superclass as optimization 
    public Collection<LWComponent> getLinked() {
        if (getLinks().size() > 0 || (head == null && tail == null))
            return super.getLinked();

        // now we know we have no links to this link

        if (head == null || tail == null) {
        }
    }
    
*/    

    /*
    public java.util.Iterator getLinkEndpointsIterator()
    {
        java.util.List endpoints = new java.util.ArrayList(2);
        if (this.head.hasNode()) endpoints.add(this.head);
        if (this.tail.hasNode()) endpoints.add(this.tail);
        return new VueUtil.GroupIterator(endpoints,
                                         super.getLinkEndpointsIterator());
        
    }
    
    public java.util.List getAllConnectedComponents()
    {
        java.util.List list = new java.util.ArrayList(getLinkRefs().size() + 2);
        list.addAll(getLinkRefs());
        list.add(getHead());
        list.add(getTail());
        return list;
    }
    */
    
    
    
    /** interface ControlListener handler
     * One of our control points (an endpoint or curve control point).
     */
    public void controlPointMoved(int index, MapMouseEvent e) {
        setControllerLocation(index, e.getMapX(), e.getMapY(), e);
    }
    
    /** for use by ResizeControl */
    void setControllerLocation(int index, Point2D.Float point) {
        setControllerLocation(index, (float) point.getX(), (float) point.getY(), null);
    }
    /** for use by ResizeControl */
    void setControllerLocation(int index, float x, float y) {
        setControllerLocation(index, x, y, null);
    }
    /**
     * dual use: controlPointMoved for ControlListener, and ResizeControl for moving each movable
     * control in a link separatly.  If MapMouseEvent is null, ResizeControl is making use of this.
     */
    
    private void setControllerLocation(int index, float mapX, float mapY, MapMouseEvent e)
    {
        final Point2D.Float local = transformMapToLocalPoint(new Point2D.Float(mapX, mapY));
        
        //System.out.println("LWLink: control point " + index + " moved");

        // TODO: need to getLocalTransform().inverseTransform the x/y back down to local coords.
        // Would be better if the coords were already translated to local coords?
        
        if (index == CHead && !head.isPruned) {
            setHead(null); // disconnect from node (already so if e == null)
            setHeadPoint(local.x, local.y);
            if (e != null)
                LinkTool.setMapIndicationIfOverValidTarget(tail.node, this, e);
        } else if (index == CTail && !tail.isPruned) {
            setTail(null);  // disconnect from node (already so if e == null)
            setTailPoint(local.x, local.y); 
            if (e != null)
                LinkTool.setMapIndicationIfOverValidTarget(head.node, this, e);
        } else if (index == CCurve1 || index == CCurve2) {
                // optional control 0 for curve
            if (mCurveControls == 1) {
                setCtrlPoint0(local.x, local.y);
            } else {
                // TODO: have LWSelection.Controller provide dx/dy, or maybe MapMouseEvent can,
                // -- these are trailing behind by one repaint!
                // Or just reflect the line once to double it's length.
                float dx = mapX - mControlPoints[index].x;
                float dy = mapY - mControlPoints[index].y;

                Point2D p = index == CCurve1 ? getCtrlPoint0() : getCtrlPoint1();

                if (index == CCurve1)
                    setCtrlPoint0((float) p.getX() + dx,
                                  (float) p.getY() + dy);
                else
                    setCtrlPoint1((float) p.getX() + dx,
                                  (float) p.getY() + dy);
            }
        }
    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        LWComponent dropTarget = e.getViewer().getIndication();
        // TODO BUG: above doesn't work if everything is selected
        if (DEBUG.MOUSE) System.out.println("LWLink: control point " + index + " dropped on " + dropTarget);
        if (dropTarget != null && !e.isShiftDown()) {
            if (index == CHead && head.node == null && tail.node != dropTarget)
                setHead(dropTarget);
            else if (index == CTail && tail.node == null && head.node != dropTarget)
                setTail(dropTarget);
            // todo: ensure paint sequence same as LinkTool.makeLink
        }
    }

    // The order of these determine the priority for what can
    // be selected if the controls overlap.  Curve controls
    // should be sure to have priority over prune controls,
    // as they can be moved to expose the prune controls if
    // need be, whereas the prune controls don't move on their own.
    
    private static final int CHead = 0;
    private static final int CTail = 1;
    private static final int CCurve1 = 2;
    private static final int CCurve2 = 3;
    private static final int CPruneHead = 4;
    private static final int CPruneTail = 5;
    private static final int MAX_CONTROL = CPruneTail + 1;

    private final LWSelection.Controller[] mControlPoints = new LWSelection.Controller[MAX_CONTROL];

    private static final RectangularShape ConnectCtrlShape = new Ellipse2D.Float(0,0, 9,9);
    private static final RectangularShape CurveCtrlShape = new Ellipse2D.Float(0,0, 8,8);
    private static final RectangularShape PruneCtrlShape = new Rectangle2D.Float(0,0,8,8);

    private static class ConnectCtrl extends LWSelection.Controller {
        //ConnectCtrl(End end) {
        ConnectCtrl(float x, float y, boolean isConnected) {
            super(x, y);
            setColor(isConnected ? null : COLOR_SELECTION_HANDLE);
            //super(end.x, end.y);
            //setColor(end.isConnected() ? null : COLOR_SELECTION_HANDLE);
        }
        public final RectangularShape getShape() { return ConnectCtrlShape; }
    }
    private static class CurveCtrl extends LWSelection.Controller {
        CurveCtrl(Point2D p) {
            super(p);
            setColor(COLOR_SELECTION_CONTROL);
            //super(p, COLOR_SELECTION_HANDLE);
            //super(p, COLOR_SELECTION);
        }
        CurveCtrl(Point2D p, float epx, float epy) {
            //super(p);
            super((float) (p.getX() + epx) / 2,
                  (float) (p.getY() + epy) / 2);
            setColor(COLOR_SELECTION_CONTROL);
        }
        public final RectangularShape getShape() { return CurveCtrlShape; }
    }
//     private static class PruneCtrl extends LWSelection.Controller {
//         private final double rotation;
//         //PruneCtrl(AffineTransform tx, double rot, boolean active)
//         //PruneCtrl(double rot, boolean active)
//         PruneCtrl(End end)
//         {
//             end.pruneCtrlTx.setToTranslation(end.mapPoint.x, end.mapPoint.y);
//             end.pruneCtrlTx.rotate(end.rotation);
//             end.pruneCtrlTx.translate(0, end.pruneCtrlOffset);
//             end.pruneCtrlTx.transform(this,this);
//             setColor(end.isPruned ? Color.red : Color.lightGray);
//             this.rotation = end.rotation + Math.PI / 4; // rotate to square parallel on line, plus 45 degrees to get diamond display
//         }
//         public final RectangularShape getShape() { return PruneCtrlShape; }
//         public final double getRotation() { return rotation; }
//     }
    
    /** interface ControlListener */
    public LWSelection.Controller[] getControlPoints(double zoom) {
        return getControls(zoom, false);
    }
    
    /** for ResizeControl */
    public LWSelection.Controller[] getMoveableControls() {
        return getControls(1.0, true); // TODO: need zoom
    }
        
    private LWSelection.Controller[] getControls(double onScreenScale, boolean moveableOnly)
    {
        if (endpointMoved)
            computeLink();

        // head, tail & curve controls are all in local coordinates
        // (which for links is local to their parent) -- to produce
        // map coordinates, we apply the local transform to the
        // points to get the map location.

        // TODO OPT: if parent is a map, getLocalTransform is just creating
        // empty affine transforms, and we're calling transform here
        // which is going to be a noop.

        final AffineTransform mapTx = LOCAL_LINKS ? getLocalTransform() : new AffineTransform(); // noop if old impl
        final Point2D.Float mapHead = head.getMapPoint();
        final Point2D.Float mapTail = tail.getMapPoint();
        
        mapTx.transform(head.getPoint(), mapHead);
        mapTx.transform(tail.getPoint(), mapTail);

        //-------------------------------------------------------
        // Connection control points
        //-------------------------------------------------------

        if (head.hasPrunedNode() || (moveableOnly && head.hasNode()))
            mControlPoints[CHead] = null;
        else 
            mControlPoints[CHead] = new ConnectCtrl(mapHead.x, mapHead.y, head.isConnected());

        if (tail.hasPrunedNode() || (moveableOnly && tail.hasNode()))
            mControlPoints[CTail] = null;
        else
            mControlPoints[CTail] = new ConnectCtrl(mapTail.x, mapTail.y, tail.isConnected());

        //-------------------------------------------------------
        // Curve control points
        //-------------------------------------------------------
        
        if (mCurveControls == 1) {
            mControlPoints[CCurve1] = new CurveCtrl(mapTx.transform(mQuad.getCtrlPt(), null));
            mControlPoints[CCurve2] = null;
        } else if (mCurveControls == 2) {
            mControlPoints[CCurve1] = new CurveCtrl(mapTx.transform(mCubic.getCtrlP1(), null),  mapHead.x, mapHead.y);
            mControlPoints[CCurve2] = new CurveCtrl(mapTx.transform(mCubic.getCtrlP2(), null),  mapTail.x, mapTail.y);
        } else {
            mControlPoints[CCurve1] = null;
            mControlPoints[CCurve2] = null;
        }
            
        //-------------------------------------------------------
        // Pruning control points
        //-------------------------------------------------------

        if (moveableOnly) {
            mControlPoints[CPruneHead] = null;
            mControlPoints[CPruneTail] = null;
        } else {

            if (head.isPruned || getHead() != null) {
                head.pruneControl.update(onScreenScale);
                mControlPoints[CPruneHead] = head.pruneControl;
            } else
                mControlPoints[CPruneHead] = null;
            
            if (tail.isPruned || getTail() != null) {
                tail.pruneControl.update(onScreenScale);
                mControlPoints[CPruneTail] = tail.pruneControl;
            } else
                mControlPoints[CPruneTail] = null;
        }
            
        return mControlPoints;
    }
    
    /** This cleaup task can run so often, we put it right on the LWLink to prevent
     * all the extra new object creation.
     */
    public void run() {
        if (!isDeleted()) {
            reparentBasedOnEndpoints();
            // this is overkill, and could / should be re-implemented here
            // to be much faster and cleaner, but it should get the job done.
            // (e.g., only one call that ensures a re-ordering over both endpoints at once)
            if (head.hasNode())
                LWContainer.ensureLinkPaintsOverAllAncestors(this, head.node);
            if (tail.hasNode())
                LWContainer.ensureLinkPaintsOverAllAncestors(this, tail.node);
        }
    }

    /** @return true if we reparented */
    private boolean reparentBasedOnEndpoints() {
        final LWComponent commonAncestor = findCommonEndpointAncestor();

        if (commonAncestor == null || commonAncestor == parent) {
            if (DEBUG.LINK) out("SAME COMMON ANCESTOR: " + commonAncestor);
            return false;
        }

        if (DEBUG.LINK) out(Util.TERM_GREEN + "REPARENTING TO NEW COMMON ANCESTOR: " + commonAncestor + Util.TERM_CLEAR);

        commonAncestor.addChild(this);
        
        return true;
    }

    private LWComponent findCommonEndpointAncestor() {
        if (head.node == null) {
            if (tail.node == null)
                return null;
            else
                return tail.node.getParent();
        } else if (tail.node == null) {
            if (head.node == null)
                return null;
            else
                return head.node.getParent();

        }

        // These are some quick-check cases we can test
        // w/out having to generate the ancestor lists:

        if (head.node.parent == tail.node.parent)
            return head.node.parent;
            
        if (head.node.parent == tail.node.parent.parent)
            return head.node.parent;

        if (tail.node.parent == head.node.parent.parent)
            return tail.node.parent;

        // Okay, no success yet, generate the lists:

        final List<LWComponent> headAncestors = head.node.getAncestors();
        final List<LWComponent> tailAncestors = tail.node.getAncestors();

        if (DEBUG.PARENTING && DEBUG.META)
            out(Util.TERM_RED + "checking for common ancestor:"
                + "\n\t      in link: " + this
                + "\n\t         head: " + head.node
                + "\n\t         tail: " + tail.node
                + "\n\theadAncestors: " + headAncestors
                + "\n\ttailAncestors: " + tailAncestors
                + Util.TERM_CLEAR);
        
        
        for (LWComponent ha : headAncestors)
            for (LWComponent ta : tailAncestors)
                if (ta == ha)
                    return ta;

        Util.printStackTrace("failed to find common ancestor:"
                             + "\n\t      in link: " + this
                             + "\n\t         head: " + head.node
                             + "\n\t         tail: " + tail.node
                             + "\n\theadAncestors: " + headAncestors
                             + "\n\ttailAncestors: " + tailAncestors);

        return null;
        
    }
    
    

    public boolean isCurved()
    {
        return mCurveControls > 0;
    }

    /**
     * This sets a link's curve controls to 0, 1 or 2 and manages
     * switching betweens states.  0 is straignt, 1 is quad curve,
     * 2 is cubic curve.  Also called by persistance to establish
     * curved state of a link.
     */
    private static final boolean CacheCurves = true; // needs to be true undo to work perfectly for curves
    public void setControlCount(int newControlCount)
    {
        //System.out.println(this + " setting CONTROL COUNT " + newControlCount);
        if (newControlCount > 2)
            throw new IllegalArgumentException("LWLink: max 2 control points " + newControlCount);

        if (mCurveControls == newControlCount)
            return;

        // Note: Float.MIN_VALUE is used as a special marker
        // to say that that control point hasn't been initialized
        // yet.

        if (mCurveControls == 0 && newControlCount == 1) {
            if (CacheCurves && mQuad != null) {
                mCurve = mQuad; // restore old curve
            } else {
                mQuad = new QuadCurve2D.Float();
                mCurve = null; // mark for init
                mQuad.ctrlx = NEEDS_DEFAULT;
                mQuad.ctrly = NEEDS_DEFAULT;
            }
        }
        else if (mCurveControls == 0 && newControlCount == 2) {
            if (CacheCurves && mCubic != null) {
                mCurve = mCubic; // restore old curve
            } else {
                mCubic = new CubicCurve2D.Float();
                mCurve = null; // mark for init
                mCubic.ctrlx1 = NEEDS_DEFAULT;
                mCubic.ctrlx2 = NEEDS_DEFAULT;
            }
        }
        else if (mCurveControls == 1 && newControlCount == 2) {
            // adding one (up from QuadCurve to CubicCurve)
            if (CacheCurves && mCubic != null) {
                mCurve = mCubic; // restore old cubic curve if had one
            } else {
                mCubic = new CubicCurve2D.Float();
                mCurve = null; // mark for init
                mCubic.ctrlx2 = NEEDS_DEFAULT;
                mCubic.ctrly2 = NEEDS_DEFAULT;
                if (CacheCurves) {
                    // if new & had quadCurve, keep the old ctrl point as one of the new ones
                    mCubic.ctrlx1 = mQuad.ctrlx;
                    mCubic.ctrly1 = mQuad.ctrly;
                } else {
                    mCubic.ctrlx1 = NEEDS_DEFAULT;
                    mCubic.ctrly1 = NEEDS_DEFAULT;
                }
            }
        }
        else if (mCurveControls == 2 && newControlCount == 1) {
            // removing one (drop from CubicCurve to QuadCurve)
            if (CacheCurves && mQuad != null) {
                // restore old quad curve if had one
                mCurve = mQuad;
            } else {
                mQuad = new QuadCurve2D.Float();
                if (CacheCurves) {
                    mCurve = mQuad;
                    mQuad.ctrlx = mCubic.ctrlx1;
                    mQuad.ctrly = mCubic.ctrly1;
                } else {
                    mQuad.ctrlx = NEEDS_DEFAULT;
                    mQuad.ctrly = NEEDS_DEFAULT;
                    mCurve = null;
                }
            }
        } else {
            // this means we're straight (newControlCount == 0)
            mCurve = null;
        }
        
        Object old = new Integer(mCurveControls);
        mCurveControls = newControlCount;
        //this.mControlPoints = new LWSelection.Controller[MAX_CONTROL];
        endpointMoved = true;
        notify(LWKey.LinkShape, old);
    }

    /** for persistance */
    public int getControlCount()
    {
        return mCurveControls;
    }

    /** for persistance */
    public Point2D getCtrlPoint0()
    {
        if (mCurveControls == 0)
            return null;
        else if (mCurveControls == 2)
            return mCubic.getCtrlP1();
        else
            return mQuad.getCtrlPt();
    }
    
    /** for persistance */
    public Point2D getCtrlPoint1()
    {
        return (mCurveControls == 2) ? mCubic.getCtrlP2() : null;
    }
    
    /** for persistance and ControlListener */
    public void setCtrlPoint0(Point2D point) {
        setCtrlPoint0((float) point.getX(), (float) point.getY());
    }

    public void setCtrlPoint0(float x, float y)
    {
        if (mCurveControls == 0) {
            setControlCount(1);
            if (DEBUG.UNDO) System.out.println("implied curved link by setting control point 0 " + this);
        }
        Object old;
        if (mCurveControls == 2) {
            old = new Point2D.Float(mCubic.ctrlx1, mCubic.ctrly1); 
            mCubic.ctrlx1 = x;
            mCubic.ctrly1 = y;
        } else {
            old = new Point2D.Float(mQuad.ctrlx, mQuad.ctrly);
            mQuad.ctrlx = x;
            mQuad.ctrly = y;
        }
        endpointMoved = true;
        notify(Key_Control_0, old);
    }

    /** for persistance and ControlListener */
    public void setCtrlPoint1(Point2D point) {
        setCtrlPoint1((float) point.getX(), (float) point.getY());
    }
    public void setCtrlPoint1(float x, float y)
    {
        if (mCurveControls < 2) {
            setControlCount(2);
            if (DEBUG.UNDO) System.out.println("implied cubic curved link by setting a control point 1 " + this);
        }
        Object old = new Point2D.Float(mCubic.ctrlx2, mCubic.ctrly2); 
        mCubic.ctrlx2 = x;
        mCubic.ctrly2 = y;
        endpointMoved = true;
        notify(Key_Control_1, old);
    }

    protected void removeFromModel()
    {
        if (head.isPruned)
            toggleHeadPrune();
        if (tail.isPruned)
            toggleTailPrune();
        
        super.removeFromModel();
        if (head.hasNode()) head.node.removeLinkRef(this);
        if (tail.hasNode()) tail.node.removeLinkRef(this);
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        if (head.hasNode()) head.node.addLinkRef(this);
        if (tail.hasNode()) tail.node.addLinkRef(this);
        endpointMoved = true; // for some reason cached label position is off on restore
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        if (head.node == null || tail.node == null)
            return false;
        return head.node.getParent() == tail.node || tail.node.getParent() == head.node;
    }

    public boolean isConnectedTo(LWComponent c) {
        return head.node == c || tail.node == c;
    }

    /** @return the endpoint of this link that is not the given source */
    public LWComponent getFarPoint(LWComponent source)
    {
        if (head.node == source)
            return tail.node;
        else if (tail.node == source)
            return head.node;
        else
            throw new IllegalArgumentException("bad farpoint: " + source + " not connected to " + this);
    }
    
    
    /** @return the endpoint of this link that is not the given source, if congruent with the arrow directionality */
    public LWComponent getFarNavPoint(LWComponent source)
    {
        int arrows = getArrowState();
        if (getHead() == source) {
            if (arrows == ARROW_NONE || (arrows & ARROW_TAIL) != 0)
                return getTail();
        } else if (getTail() == source) {
            if (arrows == ARROW_NONE || (arrows & ARROW_HEAD) != 0)
                return getHead();
        } else
            throw new IllegalArgumentException("bad farpoint: " + source + " not connected to " + this);
        return null;
    }

    /**
     * This is a nested link (a visual characteristic) if it's not a curved link, and: both ends
     * of this link in the same LWNode parent, or it's a parent-child
     * link, or it's parent is a LWNode.
     */
    public boolean isNestedLink()
    {
        if (isCurved())
            return false;
        if (head.node == null || tail.node == null)
            return getParent() instanceof LWNode;
        if (head.node.getParent() == tail.node || tail.node.getParent() == head.node)
            return true;
        return head.node.getParent() == tail.node.getParent() && head.node.getParent() instanceof LWNode;
    }
    
    public void mouseOver(MapMouseEvent e)
    {
        if (mIconBlock.isShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }

    private class SegIterator implements Iterator<Line2D.Float>, Iterable<Line2D.Float> {
        private int idx;
        private final Line2D.Float seg = new Line2D.Float();
        
        public SegIterator() {
            // start with first point of first segment pre-loaded as last point in
            // the cached segment
            
            //seg.x2 = mPoints[0];
            //seg.y2 = mPoints[1];
            //idx = 2;
            
            seg.x2 = head.x;
            seg.y2 = head.y;
            idx = 0;
        }
        
        public boolean hasNext() { return idx < mLastPoint; }
        
        public Line2D.Float next() {
            seg.x1 = seg.x2;
            seg.y1 = seg.y2;
            seg.x2 = mPoints[idx++];
            seg.y2 = mPoints[idx++];
            return seg;
        }
        
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator<Line2D.Float> iterator() { return this; }
    }
    
    @Override
    protected boolean intersectsImpl(Rectangle2D rect)
    {
        if (endpointMoved)
            computeLink();

        if (LOCAL_LINKS && !(parent instanceof LWMap)) {
            // For the moment, use default impl of paint bounds:
            // TODO: need to take into account scaling / local coords on segments
            return super.intersectsImpl(rect);
        }
        

        if (mCurve != null) {
            for (Line2D seg : new SegIterator())
                if (seg.intersects(rect))
                    return true;
        } else {
            if (rect.intersectsLine(mLine))
                return true;
        }
        
        if (mIconBlock.intersects(rect))
            return true;
        else if (hasLabel())
            return labelBox.intersectsMapRect(rect);
        else
            return false;
    }

    /** compute shortest distance from the link to the given point (the nearest segment of the line) */
    public float distanceToEdgeSq(float x, float y) {

        double minDistSq = Float.MAX_VALUE;
        
        if (mCurve != null) {
            
            double segDistSq;
            for (Line2D seg : new SegIterator()) {
                segDistSq = seg.ptSegDistSq(x, y);
                if (segDistSq < minDistSq)
                    minDistSq = segDistSq;
            }
            
        } else {
            minDistSq = mLine.ptSegDistSq(x, y);
        }

        return (float) minDistSq;
        
    }

    protected boolean containsImpl(float x, float y, float zoom) {
        return pickDistance(x, y, zoom) == 0 ? true : false;
    }
    

    /** @return 0 means a hit, -1 a completely miss, > 0 means distance, to be sorted out by caller  */
    @Override
    protected float pickDistance(float x, float y, float zoom)
    {
        if (endpointMoved)
            computeLink();

        //if (!super.containsImpl(x, y)) // fast-reject on bounding box
        //    return false;
        // Can't: bounding box doesn't currently include the label,
        // which on a small link could be well outside the stroked path.

        //final float slop = 4; // near miss this number of on-screen pixels still hits it
        //final float maxDist = (getStrokeWidth() / 2f + slop) / zoom;

        // Change contains / containsImpl to return a distance: 0 means full-hit, -1 full miss, any positive value is a near-miss distance
        // perhaps create a new "hit" with contains defaults for everyone else, as only link really needs this
        
        //final float slop = 7; // too much -- intrudes into small nodes -
        //final float slop = 4; // near miss this number of on-screen pixels still hits it

        //final float hitDist = (getStrokeWidth() / 2f) / zoom;

        // ZOOM ONLY NEEDED FOR COMPUTING SLOP (if we handle that centrally in Picker, we can get rid of zoom arg)
        
        final float hitDist = getStrokeWidth() / 2f; 
        final float hitDistSq = hitDist * hitDist;

        float minDistSq = Float.MAX_VALUE;
        //float slopDistSq = -1;

        // TODO: can make slop bigger if implement a two-pass hit detection process that
        // does absolute on first pass, and slop hits on second pass (otherwise, if this
        // too big, clicking in a node near a link to it that's on top of it will select
        // the link, and not the node).

        // TODO: would be better if slop increased when zoomed way out,
        // as effective slop becomes zero in that case.
        
        if (mCurve != null) {
            // todo: fast reject: false if outside bounding box of end points and control points
            float distSq;

            // Check the distance from all the segments in the flattened curve
            for (Line2D seg : new SegIterator()) {
                distSq = (float) seg.ptSegDistSq(x, y);
                if (distSq <= hitDistSq)
                    return 0;
                else if (distSq < minDistSq)
                    minDistSq = distSq;
            }
            
        } else {
            final float distSq = (float) mLine.ptSegDistSq(x, y);
            if (distSq <= hitDistSq)
                return 0;
            else
                minDistSq = distSq;
        }
        
        if (!isNestedLink()) {
            if (mIconBlock.contains(x, y))
                return 0;
            else if (hasLabel() && labelBox.containsMapLocation(x, y)) // bit of a hack to do this way
                return 0;
        }
        
        return minDistSq - hitDistSq;
    }
    
//     private static final int LooseSlopSq = 15*15;
//     public boolean looseContains(float x, float y) {
//         if (endpointMoved)
//             computeLink();
//         if (mCurve != null) {
//             // Java curve shapes check the entire concave region for containment.
//             // This is a quick way to check for loose-picks on curves.
//             // (Could also use distanceToEdgeSq, but this hits more area).
//             return mCurve.contains(x, y);
//         }  else {
//             // for straight links:
//             return mLine.ptSegDistSq(x, y) < LooseSlopSq;
//         }
//     }
    
    void disconnectFrom(LWComponent c)
    {
        if (head.node == c)
            setHead(null);
        else if (tail.node == c)
            setTail(null);
        else
            throw new IllegalArgumentException(this + " cannot disconnect: not connected to " + c);
    }
            
    public void setHead(LWComponent c)
    {
        if (c == head.node)
            return;
        if (head.hasNode())
            head.node.removeLinkRef(this);            
        final LWComponent oldHead = head.node;
        head.node = c;
        if (c != null)
            c.addLinkRef(this);
        //head_ID = null;
        endpointMoved = true;
        addCleanupTask(this);        
        notify("link.head.connect", new Undoable(oldHead) { void undo() { setHead(oldHead); }} );
    }
    
    public void setTail(LWComponent c)
    {
        if (c == tail.node)
            return;
        if (tail.hasNode())
            tail.node.removeLinkRef(this);            
        final LWComponent oldTail = tail.node;
        tail.node = c;
        if (c != null)
            c.addLinkRef(this);
        //tail_ID = null;
        endpointMoved = true;
        addCleanupTask(this);        
        notify("link.tail.connect", new Undoable(oldTail) { void undo() { setTail(oldTail); }} );
    }


    public boolean isConnected() {
        return head.isConnected() || tail.isConnected();
    }

    public boolean isOrdered()
    {
        return this.ordered;
    }
    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    public int getWeight()
    {
        return (int) (getStrokeWidth() + 0.5f);
    }
    public void setWeight(int w)
    {
        setStrokeWidth((float)w);
    }
    public void setStrokeWidth(float w)
    {
        if (w <= 0f)
            w = 0.1f;
        super.setStrokeWidth(w);
    }
    public int incrementWeight()
    {
        //this.weight += 1;
        //return this.weight;
        setStrokeWidth(getStrokeWidth()+1);
        return getWeight();
    }

    @Override
    public void setLocation(float x, float y) {
        final float dx = x - getX();
        final float dy = y - getY();

        if (DEBUG.CONTAINMENT) out(String.format("             setLocation %+.1f,%+.1f", x, y));
        // TODO: adjust for scale???

        translate(dx, dy);
    }

    @Override
    protected void takeLocation(float x, float y) {
        VUE.Log.debug("takeLocation on Link: " + this);
        setLocation(x, y);
    }
    

    /**

     * Any free points on the link get translated by the given dx/dy.  This means as any
     * unattached endpoints, as well as any control points if it's a curved link.  If
     * both ends of this link are connected and it has no control points (it's straight,
     * not curved) this call has no effect.
     
     */

    @Override
    public void translate(float dx, float dy)
    {
        if (DEBUG.CONTAINMENT) out(String.format("           map translate %+.1f,%+.1f", dx, dy));

// Handle this in the caller (e.g., nudge or reorder)
//         final double scale = getMapScale();
//         dx /= scale;
//         dy /= scale;
//         if (DEBUG.CONTAINMENT) out(String.format("         local translate %+.1f,%+.1f", dx, dy));
        
        //if (DEBUG.CONTAINMENT) Util.printStackTrace(String.format("translate %+.1f,%+.1f", dx, dy));
        
        if (head.node == null)
            setHeadPoint(head.x + dx, head.y + dy);

        if (tail.node == null)
            setTailPoint(tail.x + dx, tail.y + dy);

        if (mCurveControls == 1) {
            setCtrlPoint0(mQuad.ctrlx + dx,
                          mQuad.ctrly + dy);
        } else if (mCurveControls == 2) {
            setCtrlPoint0(mCubic.ctrlx1 + dx,
                          mCubic.ctrly1 + dy);
            setCtrlPoint1(mCubic.ctrlx2 + dx,
                          mCubic.ctrly2 + dy);
        }
    }

//     private LWComponent firstScaledParent() {
//         for (LWComponent c : getAncestors()) { // TODO: this is slow
//             if (c.getScale() != 1.0)
//                 return c;
//         }
//         //Util.printStackTrace("found no scaled parent " + this);
//         return getParent();
//     }

    private void scaleCoordinatesRelativeToParent(final float scale)
    {
        if (scale == 1.0)
            return;

        if (Float.isNaN(scale) || Float.isInfinite(scale)) {
            Util.printStackTrace("bad scale: " + scale + " in " + this);
        }

        if (oldParent == this) {
            // this means we were just created: we can ignore this
            oldParent = null;
            return;
        }
        
        if (oldParent == null) {
            if (DEBUG.WORK) Util.printStackTrace("scaleCoords: no old parent (ok on creates): " + this);
            return;
        }

        //final LWComponent scaledParent = firstScaledParent();
        final LWComponent scaledParent = oldParent;
        final float px = scaledParent.getMapX();
        final float py = scaledParent.getMapY();

        if (DEBUG.WORK) out("scaleCoords: deltaScale=" + scale + "; scaledParent=" + scaledParent);
        
        //out("px=" + px + ". py=" + py);
        
        if (head.node == null)
            setHeadPoint(px + (head.x - px) * scale,
                         py + (head.y - py) * scale);

        if (tail.node == null)
            setTailPoint(px + (tail.x - px) * scale,
                         py + (tail.y - py) * scale);

        if (mCurveControls == 1) {
            setCtrlPoint0(px + (mQuad.ctrlx - px) * scale,
                          py + (mQuad.ctrly - py) * scale);
            
        } else if (mCurveControls == 2) {
            setCtrlPoint0(px + (mCubic.ctrlx1 - px) * scale,
                          py + (mCubic.ctrly1 - py) * scale);
            setCtrlPoint1(px + (mCubic.ctrlx2 - px) * scale,
                          py + (mCubic.ctrly2 - py) * scale);
        }
        
    }

    
    /** called by LWComponent.updateConnectedLinks to let
     * us know something we're connected to has moved,
     * and thus we need to recompute our drawn shape.
     */
    void notifyEndpointMoved(LWComponent end)
    {
        if (DEBUG.CONTAINMENT) System.out.format("notifyEndpointMoved %-70s src=%s\n", this, end);
        
        // TODO: can optimize and skip link recompute if
        // our parent is the same as the moving parent,
        // the the OTHER end of our link also has the same
        // parent (cache a bit for this) (or other head/tail is null --
        // we can only get this call if at least one endpoint
        // is connected)
        
        // if (end.parent != parent && (head == null || 
            this.endpointMoved = true;
    }

//     void notifyEndpointReparented(LWComponent end)
//     {
//         //Util.printStackTrace("ENDPOINT REPARENTED: " + this + "; which=" +  end);
//         //this.endpointReparented = true;
//         addCleanupTask(this);
//     }

    void notifyEndpointHierarchyChanged(LWComponent end)
    {
        //Util.printStackTrace("ENDPOINT REPARENTED: " + this + "; which=" +  end);
        //this.endpointReparented = true;
        addCleanupTask(this);
    }

    private double oldMapScale = 1.0;
    private LWComponent oldParent = this;
    @Override
    public void notifyHierarchyChanging()
    {
        super.notifyHierarchyChanging();
        oldParent = getParent();
        oldMapScale = getMapScale();
        if (DEBUG.WORK )out("NH CHANGING:  curScale=" + oldMapScale);
    }
    
    @Override
    public void notifyHierarchyChanged() {
        super.notifyHierarchyChanged();

        if (LOCAL_LINKS)
            return;
        
        final double newScale = getMapScale();
        final double deltaScale = newScale / oldMapScale;
        if (DEBUG.WORK) {
            out("NH  CHANGED:   oldScale=" + oldMapScale);
            out("NH  CHANGED:   newScale=" + newScale);
            out("NH  CHANGED: deltaScale=" + deltaScale);
            out("NH  CHANGED: ANCESTORS:");
            for (LWComponent c : getAncestors())
                System.out.format("\tscale %.2f %.2f in %s\n", c.getScale(), c.getMapScale(), c);
        }
        scaleCoordinatesRelativeToParent( (float) deltaScale );
            
        
    }
    
//     @Override
//     protected void notifyMapScaleChanged(double oldParentMapScale, double newParentMapScale) {
//         final double deltaScale = newParentMapScale / oldParentMapScale;
//         out("mapScaleChanged: " + deltaScale);
//         // wait for setLocations...
// //         addCleanupTask(new Runnable() { public void run() {
// //         }});
        
//     }
    

    /** We've been notified that our absolute location should change by the given map dx/dy */
    @Override
    protected void notifyMapLocationChanged(double mdx, double mdy) {
        super.notifyMapLocationChanged(mdx, mdy);

        if (DEBUG.CONTAINMENT) System.out.println
                                   (String.format("notifyMapLocationChanged %+.1f,%+.1f%s",
                                                  mdx,
                                                  mdy,
                                                  LOCAL_LINKS ? " (ignored:local-link-impl) " : " ")
                                    + this);
        
        if (LOCAL_LINKS) return;
        
        translate((float)mdx, (float)mdy);
    }
    

    private void initCurveControlPoints()
    {
        //-------------------------------------------------------
        // INTIALIZE CONTROL POINTS & CURVE ALIAS
        //-------------------------------------------------------

        // Rely on the old actual values to CONNECTION points, previously computed in mLine
        // and centerX/centerY.

        // Note that this is still very imperfect, as when we move from a line to a
        // curve, the connection points can change dramatically, so using the
        // current axis is limited.  Unfortunately, we can't know the new axis
        // until, of course, we first place the control points *somewhere*.

        final float axisLen = lineLength(mLine);
        final float axisOffset;

        if (DEBUG.LINK) out("AXIS LEN " + axisLen + " for line " + Util.out(mLine) + " center currently " + centerX + "," + centerY);
        if (DEBUG.LINK) out("rotHeadINIT " + head.rotation + " rotTailINIT " + tail.rotation);

        if (mCurveControls == 2)
            axisOffset = axisLen / 4;
        else
            axisOffset = axisLen / 3; // do this via a log: grows slowing with length increaase
            
        //out("axisLen " + axisLen + " offset " + axisOffset);

        final AffineTransform centerLeft = AffineTransform.getTranslateInstance(centerX, centerY);
        //double deltaX = Math.abs(head.x - tail.x);
        //double deltaY = Math.abs(head.y - tail.y);

        int existingCurveCount = 0;
        if (head.hasNode()) {
            // subtract one so we don't count us -- the new curved link
            existingCurveCount = head.node.countCurvedLinksTo(tail.node) - 1;
        }


        boolean reverse = existingCurveCount % 2 == 1;
        final int further = 1 + existingCurveCount / 2;
        
        if (tail.x > head.x) {
            centerLeft.rotate(tail.rotation);
            //centerLeft.rotate(mCurveControls == 2 ? tail.rotation : head.rotation);
        } else {
            centerLeft.rotate(head.rotation);
            //centerLeft.rotate(mCurveControls == 2 ? tail.rotation : head.rotation);
        }
        
        if (reverse)
            centerLeft.translate(+axisOffset * further, 0);
        else
            centerLeft.translate(-axisOffset * further, 0);
        
        final AffineTransform centerRight = new AffineTransform(centerLeft);
        centerRight.translate(axisOffset*2,0);
        final Point2D.Float p = new Point2D.Float();
            
        if (mCurveControls == 2) {
            mCurve = mCubic;
            if (mCubic.ctrlx1 == NEEDS_DEFAULT) {
                centerLeft.transform(p,p);
                mCubic.ctrlx1 = p.x;
                mCubic.ctrly1 = p.y;
            }
            if (mCubic.ctrlx2 == NEEDS_DEFAULT) {
                p.x = p.y = 0;
                centerRight.transform(p,p);
                mCubic.ctrlx2 = p.x;
                mCubic.ctrly2 = p.y;
            }
        } else {
            mCurve = mQuad;
            if (mQuad.ctrlx == NEEDS_DEFAULT) {
                centerLeft.transform(p,p);
                mQuad.ctrlx = p.x;
                mQuad.ctrly = p.y;
            }
        }
    }
    
    /** @return the shape in it's local context (which for links, is it's parent) */
    @Override
    public Shape getLocalShape() { return getShape(); }

    private Shape getShape()
    {
        if (endpointMoved)
            computeLink();
        if (mCurveControls > 0)
            return mCurve;
        else
            return mLine;
    }
    
    
    @Override
    public boolean hasAbsoluteMapLocation() { return !LOCAL_LINKS; }


    @Override
    public void transformRelative(final Graphics2D g) {
        if (LOCAL_LINKS)
            ;
//         if (LOCAL_LINKS && parent != null)
//             parent.transformRelative(g);
        else
            super.transformRelative(g);
    }
    @Override
    public void transformLocal(Graphics2D g) {
        if (LOCAL_LINKS) {
            if (parent != null)
                parent.transformLocal(g);
        } else
            super.transformLocal(g);
    }
    @Override
    public AffineTransform transformLocal(AffineTransform a) {
        return LOCAL_LINKS ? a : super.transformLocal(a);
        //return LOCAL_LINKS ? parent.transformLocal(a) : super.transformLocal(a);
    }
    @Override
    public AffineTransform getLocalTransform() {
        return LOCAL_LINKS ? parent.getLocalTransform() : super.getLocalTransform();
    }

    // TODO: for performance, get rid of the hasAbsoluteLocation checks in LWComponent,
    // and just provide the empty absolute impls here.
    

    // if we DON'T do the below, when a slide-icon draws on the map,
    // the link pops up to the map at full-size...

    /*
    
    @Override
    public boolean hasParentLocation() { return true; }

    // do nothing for these: leave us in the parent context: we may want a new bool: hasParentDrawingContext or hasNoDrawContext or something
    @Override
    public void transformRelative(final Graphics2D g) {}
    @Override
    public void transformLocal(Graphics2D g) {
        //getParent().transformLocal(g);
    }
    @Override
    public AffineTransform transformLocal(AffineTransform a) {
        //return getParent().transformLocal(a);
        return a;
    }
    @Override
    public AffineTransform getLocalTransform() {
        //return getParent().getLocalTransform();
        return (AffineTransform) IDENTITY_TRANSFORM.clone();
    }

    @Override
    public float getMapX() {
        return getX(); // always initialized from computeLink
        //return parent == null ? getX() : parent.getMapX() + getX();
    }
    
    @Override
    public float getMapY() {
        return getY(); // always initialized from computeLink
        //return parent == null ? getY() : parent.getMapY() + getY();
    }

    @Override
    public void drawInParent(DrawContext dc)
    {
        dc.setMapDrawing();
        drawImpl(dc);
    }
    */
    
    

//     /** @return getX() -- links coords are always map/absolute */
//     public float getMapX() {
//         if (VUE.RELATIVE_COORDS)
//             return getX();
//         else
//             return super.getMapX();
//     }
    
//     /** @return getY() -- links coords are always map/absolute */
//     public float getMapY() {
//         if (VUE.RELATIVE_COORDS)
//             return getY();
//         else
//             return super.getMapY();
//     }


    private final float[] intersection = new float[2]; // result cache for intersection coords

    /**
     * Compute the endpoints of this link based on the edges of the shapes we're
     * connecting.  To do this we draw a line from the center of one shape to the center
     * of the other, and set the link endpoints to the places where mLine crosses the
     * edge of each shape.  If one of the shapes is a straight line, or for some reason
     * a shape doesn't have a facing "edge", or if anything unpredicatable happens, we
     * just leave the connection point as the center of the object.
     *
     * We also compute and cache rotation values for normalizing the link
     * to vertical (or it's control lines to vertical if a curve) so we
     * can easily move along these lines to provide control points (Controllers) for the link.
     */
    
    private void computeLink()
    {
        endpointMoved = false;

        if (DEBUG.LINK) out("computeLink");
        
        if (mCurveControls > 0 && mCurve == null)
            initCurveControlPoints();

        // Start with head & tail locations at center of the object at each endpoint.
        // Note that links are computed in entirely absolute map coordinates.  To
        // compute the actual connection point, we pass the local transform for the
        // endpoint to computeIntersection, which uses that to produce a traversable
        // flattened path transformed down to the local scale of that endpoint.
        
        if (LOCAL_LINKS) {
            
            if (head.hasNode()) {
                head.x = head.node.getCenterX(parent);
                head.y = head.node.getCenterY(parent);
            }
            if (tail.hasNode()) {
                tail.x = tail.node.getCenterX(parent);
                tail.y = tail.node.getCenterY(parent);
            }
            
        } else {

            if (head.hasNode()) {
                head.x = head.node.getCenterX();
                head.y = head.node.getCenterY();
            }
            if (tail.hasNode()) {
                tail.x = tail.node.getCenterX();
                tail.y = tail.node.getCenterY();
            }
        }

        // Note, if what's at the endpoint we're connecting to is a LWLink, we do NOT
        // bother to establish a connection at the nearest point -- we leave the
        // connection at the center point of LWLink. (For curves this is defined by the
        // midpoint of the first sub-division -- the same place we put the label if
        // there is one).

        //-----------------------------------------------------------------------------
        // PROCESS THE HEAD END
        //-----------------------------------------------------------------------------

        final Shape headShape;
        final AffineTransform headTransform;
        if (head.node == null || head.node instanceof LWLink) {
            headShape = null;
            headTransform = null;
        } else if (LOCAL_LINKS) {
            // use raw shape because we use the relative transform in computeIntersection
            headShape = head.node.getLocalShape(); 
            headTransform = head.node.getRelativeTransform(parent);
        } else {
            // use raw shape because we use the local transform in computeIntersection
            headShape = head.node.getLocalShape();
            headTransform = head.node.getLocalTransform();
        }
        
        //if (headShape != null && !(headShape instanceof Line2D)) {
        if (headShape != null) {
            final float srcX, srcY;
            if (mCurveControls == 1) {
                srcX = mQuad.ctrlx;
                srcY = mQuad.ctrly;
            } else if (mCurveControls == 2) {
                srcX = mCubic.ctrlx1;
                srcY = mCubic.ctrly1;
            } else {
                srcX = tail.x;
                srcY = tail.y;
            }
            final float[] result =
                VueUtil.computeIntersection(head.x, head.y, srcX, srcY, headShape, headTransform, intersection, 1);
            // If intersection fails for any reason, leave endpoint as center of object at the head.
            if (result != VueUtil.NoIntersection) {
                 head.x = intersection[0];
                 head.y = intersection[1];
            }
        }
        
        //-----------------------------------------------------------------------------
        // PROCESS THE TAIL END
        //-----------------------------------------------------------------------------
        
        final Shape tailShape;
        final AffineTransform tailTransform;
        if (tail.node == null || tail.node instanceof LWLink) {
            tailShape = null;
            tailTransform = null;
        } else if (LOCAL_LINKS) {
            // use raw shape because we use the relative transform in computeIntersection
            tailShape = tail.node.getLocalShape(); 
            tailTransform = tail.node.getRelativeTransform(parent);
        } else {
            tailShape = tail.node.getLocalShape(); // use raw shape because we use the local transform below
            tailTransform = tail.node.getLocalTransform();
        }
        
        //if (tailShape != null && !(tailShape instanceof Line2D)) {
        if (tailShape != null) {
            final float srcX, srcY;
            if (mCurveControls == 1) {
                srcX = mQuad.ctrlx;
                srcY = mQuad.ctrly;
            } else if (mCurveControls == 2) {
                srcX = mCubic.ctrlx2;
                srcY = mCubic.ctrly2;
            } else {
                srcX = head.x;
                srcY = head.y;
            }
            final float[] result =
                VueUtil.computeIntersection(srcX, srcY, tail.x, tail.y, tailShape, tailTransform, intersection, 1);
            // If intersection fails for any reason, leave endpoint as center of object at tail.
            if (result != VueUtil.NoIntersection) {
                 tail.x = intersection[0];
                 tail.y = intersection[1];
            }
        }
        
        this.centerX = head.x - (head.x - tail.x) / 2;
        this.centerY = head.y - (head.y - tail.y) / 2;
        
        mLine.setLine(head.x, head.y, tail.x, tail.y);

        // length is currently always set to the length of the straight line: curve length not currently computed
        mLength = lineLength(mLine);

        Rectangle2D.Float curveBounds = null;

        if (mCurveControls > 0)
            curveBounds = computeCurvedLink();
        
        //---------------------------------------------------------------------------------------------------
        // Compute rotations for arrows or for moving linearly along the link
        //---------------------------------------------------------------------------------------------------

        if (DEBUG.LINK && DEBUG.META) out("head " + head.x+","+head.y + " tail " + tail.x+","+tail.y + " line " + Util.out(mLine));

        if (mCurveControls == 1) {
            head.rotation = computeVerticalRotation(head.x, head.y, mQuad.ctrlx, mQuad.ctrly);
            tail.rotation = computeVerticalRotation(tail.x, tail.y, mQuad.ctrlx, mQuad.ctrly);
        } else if (mCurveControls == 2) {
            head.rotation = computeVerticalRotation(head.x, head.y, mCubic.ctrlx1, mCubic.ctrly1);
            tail.rotation = computeVerticalRotation(tail.x, tail.y, mCubic.ctrlx2, mCubic.ctrly2);
        } else {
            head.rotation = computeVerticalRotation(mLine.x1, mLine.y1, mLine.x2, mLine.y2);
            tail.rotation = head.rotation + Math.PI;  // can just flip head rotation: add 180 degrees
        }

        if (DEBUG.LINK && DEBUG.META) out("rotHead0 " + head.rotation + " rotTail0 " + tail.rotation);

        float controlOffset = (float) HeadShape.getHeight() * 3;
        //final int controlSize = 6;
        //final double minControlSize = MapViewer.SelectionHandleSize / dc.zoom;
        // can get zoom by passing into getControlPoints from MapViewer.drawSelection,
        // which could then pass it to computeLink, so we could have it here...
        final float minControlSize = 2; // fudged: ignoring zoom for now
        final float room = mLength - controlOffset * 2;

        if (room <= minControlSize*2)
            controlOffset = mLength/3;

        if (DEBUG.LINK && DEBUG.META) out("controlOffset " + controlOffset);
        //if (room <= controlSize*2)
        //    controlOffset = mLength/2 - controlSize;

        head.pruneCtrlOffset = controlOffset;
        tail.pruneCtrlOffset = controlOffset;

        //----------------------------------------------------------------------------------------
        // We set the size & location here so LWComponent.getBounds can do something
        // reasonable with us for computing/drawing a selection box, and for
        // LWMap.getBounds in computing entire area need to display everything on the
        // map (so we need to include control point so a curve swinging out at the edge
        // is sure to be included in visible area).
        //----------------------------------------------------------------------------------------
        
        if (mCurveControls > 0) {

//            final Rectangle2D.Float curBounds = getBounds(); // todo: optimize this
            
            // Set a size & location w/out triggering update events:
            setX(curveBounds.x);
            setY(curveBounds.y);
            takeSize(curveBounds.width,
                     curveBounds.height);
            
//             if (!curBounds.equals(getBounds())) {
//                 // adding this so if member of a group, group knows to update bounds,
//                 // otherwise LWGroup would have to check for link.control and head/tail move events.
//                 // Yet another reason to have an isBoundsEvent bit in the keys.
//                 // We could get rid of this completely if LWGroups always dynamically
//                 // computed their bounds.
//                 // This is a bit of overkill at the moment, as group's only show
//                 // their bounds with debug (FancyGroups not enabled), and ignore
//                 // their bounds for picking, so it's actually okay if they
//                 // get out of date at the moment.
//                 notify(LWKey.Location); // better LWKey.Frame, tho really need that bounds bit in the Key class
//             }
                

//             else {
//                 // We recurse if we do this:
//                 setLocation(curveBounds.x, curveBounds.y);
//                 setSize(curveBounds.width,
//                         curveBounds.height);
//             }

            

        } else {
            Rectangle2D.Float bounds = new Rectangle2D.Float();
            bounds.width = Math.abs(head.x - tail.x);
            bounds.height = Math.abs(head.y - tail.y);
            bounds.x = centerX - bounds.width/2;
            bounds.y = centerY - bounds.height/2;
            
            if (true) {
                // Set a size & location w/out triggering update events:
                takeSize(Math.abs(head.x - tail.x),
                         Math.abs(head.y - tail.y));
                setX(this.centerX - getWidth()/2);
                setY(this.centerY - getHeight()/2);
            } else {
                setSize(Math.abs(head.x - tail.x),
                        Math.abs(head.y - tail.y));
                setLocation(this.centerX - getWidth()/2,
                            this.centerY - getHeight()/2);
            }
        }

        layout();
        // if there are any links connected to this link, make sure they
        // know that this endpoint has moved.
        updateConnectedLinks();
        
    }

    private Rectangle2D.Float computeCurvedLink()
    {
        final Rectangle2D.Float bounds = new Rectangle2D.Float(head.x, head.y, 0, 0);
        
        if (mCurveControls == 1) {

            if (false && (mArrowState.get() & ARROW_HEAD) != 0) {
                // This backs up the curve endpoint to the tail of the arrow
                // This will slightly move the curve, but it keeps the connection
                // to the arrow much cleaner.
                Point2D.Float hp = new Point2D.Float();
                AffineTransform tx = new AffineTransform();
                tx.setToTranslation(head.x, head.y);
                tx.rotate(head.rotation);
                tx.translate(0, HeadShape.getHeight());
                tx.transform(hp, hp);
                mQuad.x1 = hp.x;
                mQuad.y1 = hp.y;
            } else {
                mQuad.x1 = head.x;
                mQuad.y1 = head.y;
            }
            
            mQuad.x2 = tail.x;
            mQuad.y2 = tail.y;

            // compute approximate on-curve "center" for label

            // We compute a line from the center of control line 1 to
            // the center of control line 2: that line segment is a
            // tangent to the curve who's center is on the curve.
            // (See QuadCurve2D.subdivide)
            
            float ctrlx1 = (mQuad.x1 + mQuad.ctrlx) / 2;
            float ctrly1 = (mQuad.y1 + mQuad.ctrly) / 2;
            float ctrlx2 = (mQuad.x2 + mQuad.ctrlx) / 2;
            float ctrly2 = (mQuad.y2 + mQuad.ctrly) / 2;
            mCurveCenterX = (ctrlx1 + ctrlx2) / 2;
            mCurveCenterY = (ctrly1 + ctrly2) / 2;


            if (IncludeControlPointsInBounds)
                bounds.add(mQuad.ctrlx, mQuad.ctrly);

        } else if (mCurveControls == 2) {
            mCubic.x1 = head.x;
            mCubic.y1 = head.y;
            mCubic.x2 = tail.x;
            mCubic.y2 = tail.y;

            // compute approximate on-curve "center" for label
            // (See CubicCurve2D.subdivide)
            float centerx = (mCubic.ctrlx1 + mCubic.ctrlx2) / 2;
            float centery = (mCubic.ctrly1 + mCubic.ctrly2) / 2;
            float ctrlx1 = (mCubic.x1 + mCubic.ctrlx1) / 2;
            float ctrly1 = (mCubic.y1 + mCubic.ctrly1) / 2;
            float ctrlx2 = (mCubic.x2 + mCubic.ctrlx2) / 2;
            float ctrly2 = (mCubic.y2 + mCubic.ctrly2) / 2;
            float ctrlx12 = (ctrlx1 + centerx) / 2;
            float ctrly12 = (ctrly1 + centery) / 2;
            float ctrlx21 = (ctrlx2 + centerx) / 2;
            float ctrly21 = (ctrly2 + centery) / 2;
            mCurveCenterX = (ctrlx12 + ctrlx21) / 2;
            mCurveCenterY = (ctrly12 + ctrly21) / 2;

            if (IncludeControlPointsInBounds) {
                // Add the centers of the two control lines, where we put the controllers.
                bounds.add((mCubic.ctrlx1 + head.x) / 2,
                           (mCubic.ctrly1 + head.y) / 2);
                bounds.add((mCubic.ctrlx2 + tail.x) / 2,
                           (mCubic.ctrly2 + tail.y) / 2);
            }
                
            
        }


        //---------------------------------------------------------------------------------------------------
        // Compute length / segments
        //---------------------------------------------------------------------------------------------------
        
        /*
         * For very fancy computation of a curve "center", use below
         * code and then walk the segments computing actual
         * length of curve, then walk again searching for
         * segment at middle of that distance...
         */

        // Flatten the curve into a bunch of segments for hit detection.

        if (mCurve.getBounds().isEmpty()) {
            if (DEBUG.Enabled) out("empty curve " + mCurve + " " + mCurve.getBounds());
            //tufts.Util.printStackTrace("empty curve " + mCurve + " " + mCurve.getBounds());
            return bounds;
        }

        if (mPoints == null)
            mPoints = new float[16];
        mLastPoint = 0;

        //out("LINE: " + Util.out(mLine));
        //out("CURVE: " + mCurve + " bounds " + mCurve.getBounds2D());
        //final PathIterator i = new FlatteningPathIterator(mCurve.getPathIterator(null), .001f);
        final PathIterator i = new FlatteningPathIterator(mCurve.getPathIterator(null), 1f);
        final float[] point = new float[2];

        
        if (!i.isDone()) {
            // throw out first point -- kept as head.x/head.y
            // (the number segments often maxes out at a power of two,
            // meaing the total number of flattened points is often 2^x+1)
            i.next();
        }
            
        while (!i.isDone()) {
            i.currentSegment(point);
                
            if (mLastPoint >= mPoints.length) {
                // expand mPoints to allow room for more point pairs
                    
                // The current init / expand constants for mPoints are based on a
                // flattening path with a flatness of 1.0, where a small QuadCurve
                // appears to have about 17 points (34 x/y's) max, a small
                // CubicCurve on the order of 25 max points.
                    
                float[] oldPoints = mPoints;
                mPoints = new float[oldPoints.length * 2];
                System.arraycopy(oldPoints, 0, mPoints, 0, oldPoints.length);
                if (DEBUG.BOXES) out("NEW MAX SEGMENTS " + mPoints.length / 2);
            }
            
            mPoints[mLastPoint++] = point[0];
            mPoints[mLastPoint++] = point[1];
            bounds.add(point[0], point[1]);
            i.next();
        }

        //mLength = 0;
        //for (Line2D.Float seg : new SegIterator()) mLength += lineLength(seg);
        // Skip computing this for now and leave mLength of the length of the straight line.
        // Length isn't meaingfully used with curves for the moment.

        if (DEBUG.BOXES) out("SEGMENTS IN FLATTENED CURVE: " + mLastPoint / 2 + "; total length estimate=" + mLength + "; maxSeg=" + mPoints.length / 2);

        return bounds;
    }

    /**
     * Compute the rotation needed to normalize the ine segment to vertical orientation, making it
     * parrallel to the Y axis.  So vertical lines will return either 0 or Math.PI (180 degrees), horizontal lines
     * will return +/- PI/2.  (+/- 90 degrees).  In the rotated space, +y values will move down, +x values will move right.
     */

    private double computeVerticalRotation(double x1, double y1, double x2, double y2)
    {
        final double xdiff = x1 - x2;
        final double ydiff = y1 - y2;
        final double slope = xdiff / ydiff;
        double radians = -Math.atan(slope);

        if (xdiff >= 0 && ydiff >= 0)
            radians += Math.PI;
        else if (xdiff <= 0 && ydiff >= 0)
            radians -= Math.PI;

        // diagnostics
        if (DEBUG.BOXES) {
            if (DEBUG.LINK) out("normalizing rotation " + radians);
            if (DEBUG.META) {
                this.label =
                    Util.oneDigitDecimal(xdiff) + "/" + Util.oneDigitDecimal(ydiff) + "=" + (float) slope
                    + " atan=" + (float) radians
                    + " deg=[" + Util.oneDigitDecimal(Math.toDegrees(radians))
                    + "]";
                getLabelBox().setText(this.label);
            }
        }

        return radians;
    }

    public void setArrowState(int arrowState) { mArrowState.set(arrowState); }
    public int getArrowState() { return mArrowState.get(); }
    public void rotateArrowState() {
        int newState = getArrowState() + 1;
        if (newState > ARROW_BOTH)
            newState = ARROW_NONE;
        setArrowState(newState);
    }
    
    /*
    public void setArrowState(int arrowState)
    {
        if (mArrowState == arrowState)
            return;
        Object old = new Integer(mArrowState);
        if (arrowState < 0 || arrowState > ARROW_BOTH)
            throw new IllegalArgumentException("arrowState < 0 || > " + ARROW_BOTH + ": " + arrowState);
        mArrowState = arrowState;
        layout();
        notify(LWKey.LinkArrows, old);
    }
    public int getArrowState()
    {
        return mArrowState;
    }
    public void rotateArrowState()
    {
        int newState = mArrowState + 1;
        if (newState > ARROW_BOTH)
            newState = ARROW_NONE;
        setArrowState(newState);
    }
    */


    private void drawArrows(DrawContext dc)
    {
        //-------------------------------------------------------
        // Draw arrows
        //-------------------------------------------------------

        AffineTransform savedTransform = dc.g.getTransform();

        final double scale = getMapScale();
        
        // we currently use the stroke width drawn around the arrows
        // to keep them reasonably sized relative to the line, but
        // we don't want any dash-pattern in the stroke for this
        if (mStrokeStyle.get() == StrokeStyle.SOLID)
            dc.g.setStroke(this.stroke);
        else
            dc.g.setStroke(StrokeStyle.SOLID.makeStroke(mStrokeWidth.get()));
            
        if ((mArrowState.get() & ARROW_HEAD) != 0) {
            dc.g.setColor(getStrokeColor());
            dc.g.translate(head.x, head.y);
            dc.g.rotate(head.rotation);

            if (scale != 1)
                dc.g.scale(scale, scale);
        
            // Now we're operating in a coordinate space where the line is vertical.
            // Adjust the y value moves us up and down the line, whereas adjusting
            // the x value moves us horizontally off the line.  Positive y values
            // move down the screen, negative up.
            
            // Move back to the left half the width of the arrow, so
            // that when drawn it will be centered on the line.
            dc.g.translate(-HeadShape.getWidth() / 2, 0);
            dc.g.fill(HeadShape);
            //if (getStrokeWidth() > 0) dc.g.setStroke(new BasicStroke(getStrokeWidth() / 2));
            dc.g.draw(HeadShape);
            
            dc.g.setTransform(savedTransform);
        }
        
        if ((mArrowState.get() & ARROW_TAIL) != 0) {
            dc.g.setColor(getStrokeColor());
            // draw the second arrow
            //dc.g.translate(line.getX2(), line.getY2());
            dc.g.translate(tail.x, tail.y);
            dc.g.rotate(tail.rotation);
            
            if (scale != 1)
                dc.g.scale(scale, scale);

            dc.g.translate(-TailShape.getWidth() / 2, 0); // center shape on point 
            dc.g.fill(TailShape);
            dc.g.draw(TailShape);
            
            dc.g.setTransform(savedTransform);
        }
    }

    
    protected void drawImpl(DrawContext dc)
    {
        if (endpointMoved)
            computeLink();

        //if (dc.drawAbsoluteLinks) dc.setAbsoluteDrawing(true);

        //super.draw(dc);

        //BasicStroke stroke = this.stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        // todo: do we really even want this functionality?
        /*
        if (head.hasNode() && tail.hasNode()) { // todo cleanup
        if ((head.hasNode() && head.getScale() != 1f) || (tail.hasNode() && tail.getScale() != 1f)) {
            float strokeWidth = getStrokeWidth();
            if (head.getScale() < tail.getScale())
                strokeWidth *= head.getScale();
            else
                strokeWidth *= tail.getScale();
            //g.setStroke(new BasicStroke(strokeWidth));
            stroke = new BasicStroke(strokeWidth);
        } else {
            //g.setStroke(this.stroke);
            stroke = this.stroke;
        }
        }
        */
        final Graphics2D g = dc.g;
        
        if (isSelected() && dc.isInteractive()) {
            g.setColor(COLOR_HIGHLIGHT);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));//todo:config
            g.draw(getShape());
        }

        if (DEBUG.BOXES) {
            // Split the curves into green & red halves for debugging
            Composite composite = dc.g.getComposite();
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            
            if (mCurveControls == 1) {
                QuadCurve2D left = new QuadCurve2D.Float();
                QuadCurve2D right = new QuadCurve2D.Float();
                mQuad.subdivide(left,right);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(mStrokeWidth.get()+4));
                g.draw(left);
                g.setColor(Color.red);
                g.draw(right);
            } else if (mCurveControls == 2) {
                CubicCurve2D left = new CubicCurve2D.Float();
                CubicCurve2D right = new CubicCurve2D.Float();
                mCubic.subdivide(left,right);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(mStrokeWidth.get()+4));
                g.draw(left);
                g.setColor(Color.red);
                g.draw(right);
            }
            dc.g.setComposite(composite);
        }
        
        g.setColor(getStrokeColor());

        //-------------------------------------------------------
        // Draw arrow heads if there are any
        //-------------------------------------------------------
        
        if (mArrowState.get() != 0)
            drawArrows(dc);
        
        //-------------------------------------------------------
        // Draw the stroke
        //
        // Note that since links are always drawn at the
        // map level, we need to compensate for the current
        // scale by manually modifying the drawn stroke width,
        // as well as the text box.
        //-------------------------------------------------------

        float strokeWidth = mStrokeWidth.get();
        if (strokeWidth <= 0)
            strokeWidth = 0.5f;

        if (dc.drawAbsoluteLinks) {
            //dc.setAbsoluteStroke(stroke.getLineWidth() * getMapScale());
            g.setStroke(mStrokeStyle.get().makeStroke(strokeWidth / g.getTransform().getScaleX()));
        } else {
            if (stroke == STROKE_ZERO) { // mStrokeWidth.get() was 0
                // never draw an invisible link: draw zero strokes at small absolute scale tho
                float curScale = (float) g.getTransform().getScaleX();
                if (curScale > 1)
                    strokeWidth /= curScale;
                g.setStroke(mStrokeStyle.get().makeStroke(strokeWidth));
            } else {
                if (LOCAL_LINKS) {
                    g.setStroke(stroke);
                } else {
                    float scale = getMapScaleF();
                    if (scale == 1f)
                        g.setStroke(stroke);
                    else
                        g.setStroke(mStrokeStyle.get().makeStroke(strokeWidth * scale));
                }
            }
        }
        
        if (mCurve != null) {
            //-------------------------------------------------------
            // draw the curve
            //-------------------------------------------------------

            g.draw(mCurve);
            
            if (DEBUG.BOXES) {

                dc.setAbsoluteStroke(0.5);
                dc.g.setColor(COLOR_SELECTION);
                
                //Point2D first = new Point2D.Float(mPoints[0], mPoints[1]);
                //Point2D last = new Point2D.Float(mPoints[mLastPoint-2], mPoints[mLastPoint-1]);
                Point2D first = getHeadPoint();
                Point2D last = getTailPoint();
                
                for (Line2D seg : new SegIterator()) {
                    dc.g.draw(seg);
                    dc.g.draw(new Line2D.Float(first, seg.getP2()));
                    dc.g.draw(new Line2D.Float(last, seg.getP2()));
                }
            }
                    
            if (dc.isInteractive() && (isSelected() || DEBUG.BOXES || DEBUG.CONTAINMENT)) {
                //-------------------------------------------------------
                // draw faint lines to control points if selected TODO: need to do this
                // at time we paint the selection, so these are always on top -- perhaps
                // have a LWComponent drawSkeleton, who's default is to just draw an
                // outline shape, which can replace the manual code in MapViewer, and in
                // the case of LWLink, can also draw the control lines.
                //-------------------------------------------------------
                g.setColor(COLOR_SELECTION); // todo: move these to DrawContext
                dc.setAbsoluteStroke(0.5);
                if (mCurveControls == 2) {
                    Line2D ctrlLine = new Line2D.Float(mLine.getP1(), mCubic.getCtrlP1());
                    g.draw(ctrlLine);
                    //float clx1 = line.x1 + mCubic.ctrlx
                    ctrlLine.setLine(mLine.getP2(), mCubic.getCtrlP2());
                    g.draw(ctrlLine);
                } else {
                    Line2D ctrlLine = new Line2D.Float(mLine.getP1(), mQuad.getCtrlPt());
                    g.draw(ctrlLine);
                    ctrlLine.setLine(mLine.getP2(), mQuad.getCtrlPt());
                    g.draw(ctrlLine);
                }
                g.setStroke(stroke);
            }
            //g.drawLine((int)line.getX1(), (int)line.getY1(), (int)curve.getCtrlX(), (int)curve.getCtrlY());
            //g.drawLine((int)line.getX2(), (int)line.getY2(), (int)curve.getCtrlX(), (int)curve.getCtrlY());
        } else {
            //-------------------------------------------------------
            // draw the line
            //-------------------------------------------------------
            g.draw(mLine);
        }

        if (!isNestedLink())
            drawLinkDecorations(dc);
        
        if (head.isPruned || tail.isPruned) {
            float size = 7;
            //if (dc.zoom < 1) size /= dc.zoom;
            RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, size,size);
            //Composite composite = dc.g.getComposite();
            //dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            dc.g.setColor(Color.red);
            if (head.isPruned) {
                dot.setFrameFromCenter(head.x, head.y, head.x+size/2, head.y+size/2);
                dc.g.fill(dot);
            }
            if (tail.isPruned) {
                dot.setFrameFromCenter(tail.x, tail.y, tail.x+size/2, tail.y+size/2);
                dc.g.fill(dot);
            }
            //dc.g.setComposite(composite);
        }

        /*
        boolean headgroup = head instanceof LWGroup;
        boolean tailgroup = tail instanceof LWGroup;
        if ((headgroup || tailgroup) && dc.isInteractive() || DEBUG.BOXES) {
            float size = 8;
            if (dc.zoom < 1)
                size /= dc.zoom;
            RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, size,size);
            Composite composite = dc.g.getComposite();
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            dc.g.setColor(Color.green);
            if (headgroup || DEBUG.BOXES) {
                dot.setFrameFromCenter(head.x, head.y, head.x+size/2, head.y+size/2);
                dc.g.fill(dot);
            }
            if (tailgroup || DEBUG.BOXES) {
                dot.setFrameFromCenter(tail.x, tail.y, tail.x+size/2, tail.y+size/2);
                if (DEBUG.BOXES) dc.g.setColor(Color.red);
                dc.g.fill(dot);
            }
            dc.g.setComposite(composite);
        }
        */
                
        if (DEBUG.CONTAINMENT) {
            dc.setAbsoluteStroke(0.75);
            dc.g.setColor(COLOR_SELECTION);
            g.draw(getPaintBounds());
        }

        //if (dc.drawAbsoluteLinks) dc.setAbsoluteDrawing(false);
        
    }

    @Override
    public Color getRenderFillColor(DrawContext dc) {
        if (dc != null && dc.isInteractive() && isSelected())
            return COLOR_HIGHLIGHT;
        else {
//             Color c = super.getRenderFillColor(dc);
//             out("GOT SUPER RENDER FILL " + c);
//             return c;
            return super.getRenderFillColor(dc);
        }
    }

    //private static final Color ContrastFillColor = new Color(255,255,255,224);
    //private static final Color ContrastFillColor = new Color(255,255,255);
    // transparency fill is actually just distracting
    
    private void drawLinkDecorations(DrawContext dc)
    {
        //-------------------------------------------------------
        // Paint label if there is one
        //-------------------------------------------------------
        
        //float textBoxWidth = 0;
        //float textBoxHeight = 0;
        //boolean textBoxBeingEdited = false;

        /*
        Color fillColor;
        if (dc.isDraftQuality() || DEBUG.BOXES) {
            fillColor = null;
        } else {
            if (dc.isInteractive()) {
                // set a background fill paint
                if (isSelected())
                    fillColor = COLOR_HIGHLIGHT;
                else if (getParent() != null)
                    fillColor = getParent().getRenderFillColor();
                else
                    fillColor = null;
            } else {
                fillColor = null;
            }
            

//             if (!dc.isInteractive() || !isSelected())
//                 fillColor = null;
//               //fillColor = getFillColor();
//             else
//                 fillColor = COLOR_HIGHLIGHT;
//             if (fillColor == null && getParent() != null)
//                 fillColor = getParent().getFillColor();
//             //fillColor = ContrastFillColor;

        }
        */
        
        if (hasLabel()) {
            TextBox textBox = getLabelBox();
            // only draw if we're not an active edit on the map
            if (textBox.getParent() != null) {
                //textBoxBeingEdited = true;
            } else {
                float lx = getLabelX();
                float ly = getLabelY();

                // since links don't have a sensible "location" in terms of an
                // upper left hand corner, the textbox needs to have an absolute
                // map location we can check later for hits -- we set it here
                // everytime we paint -- its a hack.
                //textBox.setMapLocation(lx, ly);

                // We force a fill color on link labels to make sure we create
                // a contrast between the text and the background, which otherwise
                // would include the usually black link stroke in the middle, obscuring
                // some of the text.
                // todo perf: only set opaque-bit/background once/when it changes.
                // (probably put a textbox factory on LWComponent and override in LWLink)

//                 if (fillColor == null || !dc.isInteractive()) {
//                     textBox.setOpaque(false);
//                 } else {
//                     textBox.setBackground(fillColor);
//                     textBox.setOpaque(true);
//                 }

                textBox.setBackground(getRenderFillColor(dc));
                textBox.setOpaque(true);
                
                dc.g.translate(lx, ly);

                double scale = 1.0; // LOCAL_LINKS only
                if (!LOCAL_LINKS) {
                    scale = getMapScale();
                    if (scale != 1)
                        dc.g.scale(scale, scale);
                }
                //if (isZoomedFocus()) g.scale(getScale(), getScale());
                // todo: need to re-center label when this component relative to scale,
                // and patch contains to understand a scaled label box...
                textBox.draw(dc);

                if (LOCAL_LINKS && DEBUG.Enabled) {
                    dc.g.setColor(Color.red);
                    dc.g.setFont(getFont().deriveFont(Font.BOLD, 6f));
                    dc.g.drawString("("+parent.getUniqueComponentTypeLabel()+")", 0, 15);
                    //dc.g.drawString(parent.getDiagnosticLabel(), 0, 30);
                }


                if (!LOCAL_LINKS) {
                    if (scale != 1)
                        dc.g.scale(1/scale, 1/scale);
                }
                
                /* draw border
                if (isSelected()) {
                    Dimension s = textBox.getSize();
                    g.setColor(COLOR_SELECTION);
                    //g.setStroke(STROKE_HALF); // todo: needs to be unscaled / handled by selection
                    g.setStroke(new BasicStroke(1f / (float) dc.zoom));
                    // -- i guess we could compute based on zoom level -- maybe MapViewer could
                    // keep such a stroke handy for us... (DrawContext would be handy again...)
                    g.drawRect(0,0, s.width, s.height);
                }
                */
                
                //if (isZoomedFocus()) g.scale(1/getScale(), 1/getScale());
                dc.g.translate(-lx, -ly);
                
                if (false) { // debug
                    // draw label in center of bounding box just for
                    // comparing to our on-curve center computation
                    lx = getCenterX() - textBox.getMapWidth() / 2;
                    ly = getCenterY() - textBox.getMapHeight() / 2;
                    dc.g.translate(lx,ly);
                    //textBox.setBackground(Color.lightGray);
                    textBox.setOpaque(false);
                    dc.g.setColor(Color.blue);
                    textBox.draw(dc);
                    dc.g.translate(-lx,-ly);
                }
            }
        }

        if (mIconBlock.isShowing()) {
            //dc.g.setStroke(STROKE_HALF);
            //dc.g.setColor(Color.gray);
            //dc.g.draw(mIconBlock);
//             if (fillColor != null) {
//                 dc.g.setColor(fillColor);
//                 dc.g.fill(mIconBlock);
//             }
            mIconBlock.draw(dc);
        }
        // todo perf: don't have to compute icon block location every time
        /*
        if (!textBoxBeingEdited && mIconBlock.isShowing()) {
            mIconBlock.layout();
            // at right
            //float ibx = getLabelX() + textBoxWidth;
            //float iby = getLabelY();
            // at bottom
            float ibx = getCenterX() - mIconBlock.width / 2;
            float iby = getLabelY() + textBoxHeight;
            mIconBlock.setLocation(ibx, iby);
            mIconBlock.draw(dc);
        }
        */
    }

    private float lineLength(float x1, float y1, float x2, float y2) {
        final float dx = x1 - x2;
        final float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    private float lineLength(Line2D.Float l) {
        return lineLength(l.x1, l.y1, l.x2, l.y2);
    }


    @Override
    protected void layoutImpl(Object triggerKey)
    {
        final float cx;
        final float cy;

        if (mCurveControls > 0) {
            cx = mCurveCenterX;
            cy = mCurveCenterY;
        } else {
            cx = (head.x + tail.x) / 2;
            cy = (head.y + tail.y) / 2;
        }
        
        float totalHeight = 0;
        float totalWidth = 0;

        boolean putBelow = hasResource();
        
        // Always call LWIcon.Block.layout first to have it compute size/determine if showing
        // before asking it if isShowing()

        TextBox textBox;
        
        boolean vertical = false;
        if (hasLabel() && !putBelow) {
            // Check to see if we want to make it vertical
            mIconBlock.setOrientation(LWIcon.Block.VERTICAL);
            mIconBlock.layout();
            vertical = (getLabelBox().getMapHeight() >= mIconBlock.getHeight());
            if (!vertical) {
                mIconBlock.setOrientation(LWIcon.Block.HORIZONTAL);
                mIconBlock.layout();
            }
        } else {
            // default to horizontal
            mIconBlock.setOrientation(LWIcon.Block.HORIZONTAL);
            mIconBlock.layout();
        }
        
        boolean iconBlockShowing = mIconBlock.isShowing(); // must ask isShowing *after* mIconBlock.layout()
        if (iconBlockShowing) {
            totalWidth += mIconBlock.getWidth();
            totalHeight += mIconBlock.getHeight();
        }


        float lx = 0;
        float ly = 0;
        if (hasLabel()) {
            getLabelBox(); // make sure labelBox is set
            // since links don't have a sensible "location" in terms of an
            // upper left hand corner, the textbox needs to have an absolute
            // map location we can check later for hits
            totalWidth += labelBox.getMapWidth();
            totalHeight += labelBox.getMapHeight();
            if (putBelow) {
                // for putting icons below
                lx = cx - labelBox.getMapWidth() / 2;
                ly = cy - totalHeight / 2;
                //if (iconBlockShowing)
                // put label just over center so link splits block & label if horizontal                
                //ly = cy - (labelBox.getMapHeight() + getStrokeWidth() / 2);
            } else {
                // for putting icons at right
                lx = cx - totalWidth / 2;
                ly = cy - labelBox.getMapHeight() / 2;
            }
            labelBox.setMapLocation(lx, ly);
        }
        if (iconBlockShowing) {
            float ibx, iby;
            if (putBelow) {
                // for below
                ibx = (float) (cx - mIconBlock.getWidth() / 2);
                if (hasLabel())
                    iby = labelBox.getMapY() + labelBox.getMapHeight();
                else
                    iby = (float) (cy - mIconBlock.getHeight() / 2f);
                // we're seeing a sub-pixel gap -- this should fix
                iby -= 0.5;
            } else {
                // for at right
                if (hasLabel())
                    ibx = (float) lx + labelBox.getMapWidth();
                else
                    ibx = (float) (cx - mIconBlock.getWidth() / 2);
                iby = (float) (cy - mIconBlock.getHeight() / 2);
                // we're also seeing a sub-pixel gap here -- this should fix
                ibx -= 0.5;
            }
            mIconBlock.setLocation(ibx, iby);
        }
    }

    @Override
    public float getCenterX() {
        if (LOCAL_LINKS)
            return getCenterX(getMap()); // todo: slow
        else
            return mCurveControls > 0 ? mCurveCenterX : (head.x + tail.x) / 2;
    }
    @Override
    public float getCenterY() {
        if (LOCAL_LINKS)
            return getCenterY(getMap()); // todo: slow
        else
            return mCurveControls > 0 ? mCurveCenterY : (head.y + tail.y) / 2;
    }


    /** Create a duplicate LWLink.  The new link will
     * not be connected to any endpoints */
    @Override
    public LWComponent duplicate(CopyContext cc)
    {
        //todo: make sure we've got everything (styles, etc)
        LWLink link = (LWLink) super.duplicate(cc);
        link.head.x = head.x;
        link.head.y = head.y;
        link.tail.x = tail.x;
        link.tail.y = tail.y;
        link.centerX = centerX;
        link.centerY = centerY;
        link.ordered = ordered;
        //link.mArrowState = mArrowState;
        if (mCurveControls > 0) {
            link.setCtrlPoint0(getCtrlPoint0());
            if (mCurveControls > 1)
                link.setCtrlPoint1(getCtrlPoint1());
        }
        computeLink();
        layout();
        return link;
    }
    
    @Override
    public String paramString()
    {
        String s = String.format("%.0f,%.0f-->%.0f,%.0f", head.x, head.y, tail.x, tail.y);
        if (getControlCount() == 1)
            s += String.format(" (%.0f,%.0f)", mQuad.ctrlx,  mQuad.ctrly);
        else if (getControlCount() == 2)
            s += String.format(" (%.0f,%.0f & %.0f,%.0f)",
                               mCubic.ctrlx1,  mCubic.ctrly1, mCubic.ctrlx2,  mCubic.ctrly2);
        return s + " " + mStrokeStyle.get();            
    }

    /** @deprecated -- use getHead */ public LWComponent getComponent1() { return getHead(); }
    /** @deprecated -- use getTail */ public LWComponent getComponent2() { return getTail(); }
    /** @deprecated -- use setHeadPoint */ public void setStartPoint(float x, float y) { setHeadPoint(x, y); }
    /** @deprecated -- use setTailPoint */ public void setEndPoint(float x, float y) { setTailPoint(x, y); }
    /** @deprecated -- use getHeadPoint */ public Point2D getPoint1() { return getHeadPoint(); }
    /** @deprecated -- use getTailPoint */ public Point2D getPoint2() { return getTailPoint(); }
    
    /** @deprecated -- no longer needed (now using castor references), always returns null */
    public String getHead_ID() { return null; }
    /** @deprecated -- no longer needed (now using castor references), always returns null */
    public String getTail_ID() { return null; }
    
    /** for persistance/init/undo ONLY */
    public void setHeadPoint(Point2D p) {
        if (mXMLRestoreUnderway) {
            head.x = (float) p.getX();
            head.y = (float) p.getY();
        } else {
            setHeadPoint((float)p.getX(), (float)p.getY());
        }
    }
    /** for persistance/init/undo ONLY  */
    public void setTailPoint(Point2D p) {
        if (mXMLRestoreUnderway) {
            tail.x = (float) p.getX();
            tail.y = (float) p.getY();
        } else {
            setTailPoint((float)p.getX(), (float)p.getY());
        }
    }
    /** for persistance/init ONLY */
    public Point2D.Float getHeadPoint() {
        return new Point2D.Float(head.x, head.y);
    }
    /** for persistance/init ONLY */
    public Point2D.Float getTailPoint() {
        return new Point2D.Float(tail.x, tail.y);
    }

    // these two to support a special dynamic link
    // which we use while creating a new link
    //boolean viewerCreationLink = false;
    // todo: this boolean a hack until we no longer need to use
    // clip-regions to draw the links
    LWLink(LWComponent tailNode)
    {
        initLink();
        //viewerCreationLink = true;
        tail.node = tailNode;
        setStrokeWidth(2f); //todo config: default link width
    }
    
    // sets head WIHOUT adding a link ref -- used for
    // temporary drawing of link hack during drag outs --
    // you know, we should just skip using a LWLink object
    // for that crap alltogether. TODO
    void setTemporaryEndPoint1(LWComponent headNode)
    {
        head.node = headNode;
    }
    
    
}
