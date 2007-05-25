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

import java.awt.*;
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
 * @version $Revision: 1.149 $ / $Date: 2007-05-25 21:48:04 $ / $Author: sfraize $
 */
public class LWLink extends LWComponent
    implements LWSelection.ControlListener
{
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

    // member variables:
    
    /*
    private static class EndPoint { // todo: like this (could also subclass Point2D.Float)
        float x, y; // point at node where the connection is made, or disconnected map location
        LWComponent node; // if null, not connected
        float lineX, lineY; // end of curve / line -- can be different than x / y if there is a connector shape
        RectangularShape connectorShape; // e.g. an arrow -- null means none
        boolean isPruned;
        double rotation;
        AffineTransform normalizer;
    };
    private EndPoint head, tail;
    */


    private LWComponent head;
    private LWComponent tail;
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
    private transient double mRotationHead;
    private transient double mRotationTail;
    private transient AffineTransform mHeadCtrlTx = new AffineTransform();
    private transient AffineTransform mTailCtrlTx = new AffineTransform();
    

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
    private float headX;       // todo: either consistently use these or the values in mLine
    private float headY;
    private float tailX;
    private float tailY;
    private boolean headIsPruned, tailIsPruned;
    
    private boolean ordered = false; // not doing anything with this yet
    
    private transient boolean endpointMoved = true; // has an endpoint moved since we last computed shape?

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
    private final IntProperty mArrowState = new IntProperty(KEY_LinkArrows, ARROW_TAIL) { void onChange() { endpointMoved = true; layout(); } };


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
        if (head == null || head.isHidden(HideCause.PRUNE))
            return null;
        else
            return head;
        //return headIsPruned ? null : head;
    }
    /** @return the component connected at the tail end, or null if none */
    public LWComponent getTail() {
        if (tail == null || tail.isHidden(HideCause.PRUNE))
            return null;
        else
            return tail;
        //return tailIsPruned ? null : tail;
    }

    private boolean headNodeIsPruned() {
        return head != null && head.isHidden(HideCause.PRUNE);
    }
    private boolean tailNodeIsPruned() {
        return tail != null && tail.isHidden(HideCause.PRUNE);
    }
    

    public void setHeadPoint(float x, float y) {
        if (head != null) throw new IllegalStateException("Can't set pixel start point for connected link");
        Object old = new Point2D.Float(headX, headY);
        headX = x;
        headY = y;
        endpointMoved = true;
        notify(KEY_LinkHeadPoint, old);
    }

    public void setTailPoint(float x, float y) {
        if (tail != null) throw new IllegalStateException("Can't set pixel end point for connected link");
        Object old = new Point2D.Float(tailX, tailY);
        tailX = x;
        tailY = y;
        endpointMoved = true;
        notify(KEY_LinkTailPoint, old);
    }
    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e) {
        if (index == CPruneHead && head != null) {
            toggleHeadPrune();
        } else if (index == CPruneTail && tail != null) {
            toggleTailPrune();
        }
    }

    private void toggleHeadPrune() {
        pruneToggle(!headIsPruned, getEndpointChain(head));
        headIsPruned = !headIsPruned;
    }

    private void toggleTailPrune() {
        pruneToggle(!tailIsPruned, getEndpointChain(tail));
        tailIsPruned = !tailIsPruned;
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

    /** @return same as super class impl, but by default add our own two endpoints */
    @Override
    public Rectangle2D.Float getFanBounds(Rectangle2D.Float r)
    {
        final Rectangle2D.Float rect = super.getFanBounds(r);
        if (head != null)
            rect.add(head.getBounds());
        if (tail != null)
            rect.add(tail.getBounds());
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
        if (head != null && !headIsPruned)
            bag.add(head);
        if (tail != null && !tailIsPruned)
            bag.add(tail);
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
        if (this.head != null) endpoints.add(this.head);
        if (this.tail != null) endpoints.add(this.tail);
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
    
    private void setControllerLocation(int index, float x, float y, MapMouseEvent e)
    {
        //System.out.println("LWLink: control point " + index + " moved");
        
        if (index == CHead && !headIsPruned) {
            setHead(null); // disconnect from node (already so if e == null)
            setHeadPoint(x, y);
            if (e != null)
                LinkTool.setMapIndicationIfOverValidTarget(tail, this, e);
        } else if (index == CTail && !tailIsPruned) {
            setTail(null);  // disconnect from node (already so if e == null)
            setTailPoint(x, y); 
            if (e != null)
                LinkTool.setMapIndicationIfOverValidTarget(head, this, e);
        } else if (index == CCurve1 || index == CCurve2) {
                // optional control 0 for curve
            if (mCurveControls == 1) {
                setCtrlPoint0(x, y);
            } else {
                // TODO: have LWSelection.Controller provide dx/dy, or maybe MapMouseEvent can,
                // as I think these are trailing depending on fast we repaint!
                // Or just reflect the line once to double it's length.
                float dx = x - mControlPoints[index].x;
                float dy = y - mControlPoints[index].y;

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
            if (index == CHead && head == null && tail != dropTarget)
                setHead(dropTarget);
            else if (index == CTail && tail == null && head != dropTarget)
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
        ConnectCtrl(float x, float y, boolean isConnected) {
            super(x, y);
            setColor(isConnected ? null : COLOR_SELECTION_HANDLE);
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
    private static class PruneCtrl extends LWSelection.Controller {
        private final double rotation;
        PruneCtrl(AffineTransform tx, double rot, boolean active)
        {
            tx.transform(this,this);
            setColor(active ? Color.red : Color.lightGray);
            this.rotation = rot + Math.PI / 4; // rotate to square parallel on line, plus 45 degrees to get diamond display
        }
        public final RectangularShape getShape() { return PruneCtrlShape; }
        public final double getRotation() { return rotation; }
    }
    
    /** interface ControlListener */
    public LWSelection.Controller[] getControlPoints() {
        return getControls(false);
    }
    
    /** for ResizeControl */
    public LWSelection.Controller[] getMoveableControls() {
        return getControls(true);
    }
        
    private LWSelection.Controller[] getControls(boolean moveableOnly)
    {
        if (endpointMoved)
            computeLink();

        //-------------------------------------------------------
        // Connection control points
        //-------------------------------------------------------

        if (/*false &&*/ headNodeIsPruned() || (moveableOnly && head != null))
            mControlPoints[CHead] = null;
        else 
            mControlPoints[CHead] = new ConnectCtrl(headX, headY, head != null);

        if (/*false &&*/ tailNodeIsPruned() || (moveableOnly && tail != null))
            mControlPoints[CTail] = null;
        else
            mControlPoints[CTail] = new ConnectCtrl(tailX, tailY, tail != null);

        //-------------------------------------------------------
        // Curve control points
        //-------------------------------------------------------
        
        if (mCurveControls == 1) {
            mControlPoints[CCurve1] = new CurveCtrl(mQuad.getCtrlPt());
            mControlPoints[CCurve2] = null;
        } else if (mCurveControls == 2) {
            mControlPoints[CCurve1] = new CurveCtrl(mCubic.getCtrlP1(), headX, headY);
            mControlPoints[CCurve2] = new CurveCtrl(mCubic.getCtrlP2(), tailX, tailY);
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

            if (headIsPruned || getHead() != null)
                mControlPoints[CPruneHead] = new PruneCtrl(mHeadCtrlTx, mRotationHead, headIsPruned);
            else
                mControlPoints[CPruneHead] = null;
            
            if (tailIsPruned || getTail() != null)
                mControlPoints[CPruneTail] = new PruneCtrl(mTailCtrlTx, mRotationTail, tailIsPruned);
            else
                mControlPoints[CPruneTail] = null;
        }
            
        return mControlPoints;
    }
    
    /** called by LWComponent.updateConnectedLinks to let
     * us know something we're connected to has moved,
     * and thus we need to recompute our drawn shape.
     */
    void setEndpointMoved(boolean tv)
    {
        this.endpointMoved = tv;
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
        if (headIsPruned)
            toggleHeadPrune();
        if (tailIsPruned)
            toggleTailPrune();
        
        super.removeFromModel();
        if (head != null) head.removeLinkRef(this);
        if (tail != null) tail.removeLinkRef(this);
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        if (head != null) head.addLinkRef(this);
        if (tail != null) tail.addLinkRef(this);
        endpointMoved = true; // for some reason cached label position is off on restore
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        if (head == null || tail == null)
            return false;
        return head.getParent() == tail || tail.getParent() == head;
    }

    public boolean isConnectedTo(LWComponent c) {
        return head == c || tail == c;
    }

    /** @return the endpoint of this link that is not the given source */
    public LWComponent getFarPoint(LWComponent source)
    {
        if (head == source)
            return tail;
        else if (tail == source)
            return head;
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
        if (head == null || tail == null)
            return getParent() instanceof LWNode;
        if (head.getParent() == tail || tail.getParent() == head)
            return true;
        return head.getParent() == tail.getParent() && head.getParent() instanceof LWNode;
    }
    
    public Shape getShape()
    {
        if (endpointMoved)
            computeLink();
        if (mCurveControls > 0)
            return mCurve;
        else
            return mLine;
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
            
            seg.x2 = headX;
            seg.y2 = headY;
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
    
    protected boolean intersectsImpl(Rectangle2D rect)
    {
        if (endpointMoved)
            computeLink();

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
    
    private static final int LooseSlopSq = 15*15;
    public boolean looseContains(float x, float y)
    {
        // return true; // let the picker sort out who's closest -- not good enough: will always pick *some* link!
        
        if (endpointMoved)
            computeLink();
        
        if (mCurve != null) {
            // Java curve shapes check the entire concave region for containment.
            // This is a quick way to check for loose-picks on curves.
            // (Could also use distanceToEdgeSq, but this hits more area).
            return mCurve.contains(x, y);
        }  else {
            // for straight links:
            return mLine.ptSegDistSq(x, y) < LooseSlopSq;
        }
    }
    
    void disconnectFrom(LWComponent c)
    {
        if (head == c)
            setHead(null);
        else if (tail == c)
            setTail(null);
        else
            throw new IllegalArgumentException(this + " cannot disconnect: not connected to " + c);
    }
            
    public void setHead(LWComponent c)
    {
        if (c == head)
            return;
        if (head != null)
            head.removeLinkRef(this);            
        final LWComponent oldHead = this.head;
        this.head = c;
        if (c != null)
            c.addLinkRef(this);
        //head_ID = null;
        endpointMoved = true;
        notify("link.head.connect", new Undoable(oldHead) { void undo() { setHead(oldHead); }} );
    }
    
    public void setTail(LWComponent c)
    {
        if (c == tail)
            return;
        if (tail != null)
            tail.removeLinkRef(this);            
        final LWComponent oldTail = this.tail;
        this.tail = c;
        if (c != null)
            c.addLinkRef(this);
        //tail_ID = null;
        endpointMoved = true;
        notify("link.tail.connect", new Undoable(oldTail) { void undo() { setTail(oldTail); }} );
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


    /**
     * Any free (unattached) endpoints get translated by
     * how much we're moving, as well as any control points.
     * If both ends of this link are connected and it has
     * no control points (it's straight, not curved) calling
     * setLocation will have absolutely no effect on it.
     */

    public void translate(float dx, float dy)
    {
        if (head == null)
            setHeadPoint(headX + dx, headY + dy);

        if (tail == null)
            setTailPoint(tailX + dx, tailY + dy);

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

    public void setLocation(float x, float y) {
        float dx = x - getX();
        float dy = y - getY();

        translate(dx, dy);
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
        if (DEBUG.LINK) out("rotHeadINIT " + mRotationHead + " rotTailINIT " + mRotationTail);

        if (mCurveControls == 2)
            axisOffset = axisLen / 4;
        else
            axisOffset = axisLen / 3; // do this via a log: grows slowing with length increaase
            
        //out("axisLen " + axisLen + " offset " + axisOffset);

        final AffineTransform centerLeft = AffineTransform.getTranslateInstance(centerX, centerY);
        //double deltaX = Math.abs(headX - tailX);
        //double deltaY = Math.abs(headY - tailY);

        int existingCurveCount = 0;
        if (head != null) {
            // subtract one so we don't count us -- the new curved link
            existingCurveCount = head.countCurvedLinksTo(tail) - 1;
        }


        boolean reverse = existingCurveCount % 2 == 1;
        final int further = 1 + existingCurveCount / 2;
        
        if (tailX > headX) {
            centerLeft.rotate(mRotationTail);
            //centerLeft.rotate(mCurveControls == 2 ? mRotationTail : mRotationHead);
        } else {
            centerLeft.rotate(mRotationHead);
            //centerLeft.rotate(mCurveControls == 2 ? mRotationTail : mRotationHead);
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


    // links are absolute in their parent: nothing to add to the transforms or local(raw) v.s. map shapes

//     @Override public AffineTransform getLocalTransform() { return IDENTITY_TRANSFORM; }
//     @Override public AffineTransform transformLocal(AffineTransform a) { return a; }
//     @Override public void transformLocal(Graphics2D g) {}
//     @Override public void transformRelative(Graphics2D g) {}
    @Override public Shape getLocalShape() { return getShape(); }
    //@Override public Shape getMapShape() { return getShape(); }
    //@Override /** @return 1 -- links never scaled */ public final double getMapScale() { return 1; }
    @Override /** @return 1 -- links never scaled by themselves */ public final double getScale() { return 1; }
    
    /** @return getX() -- links coords are always map/absolute */
    public float getMapX() {
        if (VUE.RELATIVE_COORDS)
            return getX();
        else
            return super.getMapX();
    }
    
    /** @return getY() -- links coords are always map/absolute */
    public float getMapY() {
        if (VUE.RELATIVE_COORDS)
            return getY();
        else
            return super.getMapY();
    }


    private float[] intersection = new float[2]; // result cache for intersection coords

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
        
        if (mCurveControls > 0 && mCurve == null)
            initCurveControlPoints();

        // Start with head & tail locations at center of the object at
        // each endpoint:
        
        if (head != null) {
            headX = head.getCenterX();
            headY = head.getCenterY();
        }
        if (tail != null) {
            tailX = tail.getCenterX();
            tailY = tail.getCenterY();
        }

        float srcX, srcY;
        final Shape shapeAtHead = (head == null ? null : head.getLocalShape()); // use raw shape mecause we use the local transform below
        // if either endpoint shape is a straight line, we don't need to
        // bother computing the shape intersection -- it will just
        // be the default connection point -- the center point.
        
        // todo bug: if it's a CURVED LINK we're connect to, a floating
        // connection point works out if the link approaches from
        // the convex side, but from the concave side, it winds
        // up at the center point for a regular straight link.

        if (shapeAtHead != null && !(shapeAtHead instanceof Line2D)) {
            if (mCurveControls == 1) {
                srcX = mQuad.ctrlx;
                srcY = mQuad.ctrly;
            } else if (mCurveControls == 2) {
                srcX = mCubic.ctrlx1;
                srcY = mCubic.ctrly1;
            } else {
                srcX = tailX;
                srcY = tailY;
            }
            float[] result = VueUtil.computeIntersection(headX, headY, srcX, srcY, shapeAtHead, head.getLocalTransform(), intersection, 1);
            // If intersection fails for any reason, leave endpoint as center
            // of object at the head.
            if (result != VueUtil.NoIntersection) {
                 headX = intersection[0];
                 headY = intersection[1];
            }
        }
        final Shape shapeAtTail = (tail == null ? null : tail.getLocalShape());
        if (shapeAtTail != null && !(shapeAtTail instanceof Line2D)) {
            if (mCurveControls == 1) {
                srcX = mQuad.ctrlx;
                srcY = mQuad.ctrly;
            } else if (mCurveControls == 2) {
                srcX = mCubic.ctrlx2;
                srcY = mCubic.ctrly2;
            } else {
                srcX = headX;
                srcY = headY;
            }
            float[] result = VueUtil.computeIntersection(srcX, srcY, tailX, tailY, shapeAtTail, tail.getLocalTransform(), intersection, 1);
            // If intersection fails for any reason, leave endpoint as center
            // of object at tail.
            if (result != VueUtil.NoIntersection) {
                 tailX = intersection[0];
                 tailY = intersection[1];
            }
        }
        
        this.centerX = headX - (headX - tailX) / 2;
        this.centerY = headY - (headY - tailY) / 2;
        
        mLine.setLine(headX, headY, tailX, tailY);

        // length is currently always set to the length of the straight line: curve length not compute
        mLength = lineLength(mLine);

        Rectangle2D.Float curveBounds = null;

        if (mCurveControls > 0)
            curveBounds = computeCurvedLink();
        
        //---------------------------------------------------------------------------------------------------
        // Compute rotations for arrows or for moving linearly along the link
        //---------------------------------------------------------------------------------------------------

        if (DEBUG.LINK) out("head " + headX+","+headY + " tail " + tailX+","+tailY + " line " + Util.out(mLine));

        if (mCurveControls == 1) {
            mRotationHead = computeVerticalRotation(headX, headY, mQuad.ctrlx, mQuad.ctrly);
            mRotationTail = computeVerticalRotation(tailX, tailY, mQuad.ctrlx, mQuad.ctrly);
        } else if (mCurveControls == 2) {
            mRotationHead = computeVerticalRotation(headX, headY, mCubic.ctrlx1, mCubic.ctrly1);
            mRotationTail = computeVerticalRotation(tailX, tailY, mCubic.ctrlx2, mCubic.ctrly2);
        } else {
            mRotationHead = computeVerticalRotation(mLine.x1, mLine.y1, mLine.x2, mLine.y2);
            mRotationTail = mRotationHead + Math.PI;  // can just flip head rotation: add 180 degrees
        }

        if (DEBUG.LINK) out("rotHead0 " + mRotationHead + " rotTail0 " + mRotationTail);

        float controlOffset = (float) HeadShape.getHeight() * 2;
        //final int controlSize = 6;
        //final double minControlSize = MapViewer.SelectionHandleSize / dc.zoom;
        // can get zoom by passing into getControlPoints from MapViewer.drawSelection,
        // which could then pass it to computeLink, so we could have it here...
        final float minControlSize = 2; // fudged: ignoring zoom for now
        final float room = mLength - controlOffset * 2;

        if (room <= minControlSize*2)
            controlOffset = mLength/3;

        if (DEBUG.LINK) out("controlOffset " + controlOffset);
        //if (room <= controlSize*2)
        //    controlOffset = mLength/2 - controlSize;

        mHeadCtrlTx.setToTranslation(headX, headY);
        mHeadCtrlTx.rotate(mRotationHead);
        mHeadCtrlTx.translate(0, controlOffset);
        mTailCtrlTx.setToTranslation(tailX, tailY);
        mTailCtrlTx.rotate(mRotationTail);
        mTailCtrlTx.translate(0, controlOffset);

        //----------------------------------------------------------------------------------------
        // We set the size & location here so LWComponent.getBounds can do something
        // reasonable with us for computing/drawing a selection box, and for
        // LWMap.getBounds in computing entire area need to display everything on the
        // map (so we need to include control point so a curve swinging out at the edge
        // is sure to be included in visible area).
        //----------------------------------------------------------------------------------------
        
        if (mCurveControls > 0) {
            
            // Set a size & location w/out triggering update events:
            setX(curveBounds.x);
            setY(curveBounds.y);
            takeSize(curveBounds.width,
                     curveBounds.height);

        } else {
            Rectangle2D.Float bounds = new Rectangle2D.Float();
            bounds.width = Math.abs(headX - tailX);
            bounds.height = Math.abs(headY - tailY);
            bounds.x = centerX - bounds.width/2;
            bounds.y = centerY - bounds.height/2;
            
            // Set a size & location w/out triggering update events:
            takeSize(Math.abs(headX - tailX),
                     Math.abs(headY - tailY));
            setX(this.centerX - getWidth()/2);
            setY(this.centerY - getHeight()/2);
        }

        layout();
        // if there are any links connected to this link, make sure they
        // know that this endpoint has moved.
        updateConnectedLinks();
        
    }

    private Rectangle2D.Float computeCurvedLink()
    {
        final Rectangle2D.Float bounds = new Rectangle2D.Float(headX, headY, 0, 0);
        
        if (mCurveControls == 1) {

            if (false && (mArrowState.get() & ARROW_HEAD) != 0) {
                // This backs up the curve endpoint to the tail of the arrow
                // This will slightly move the curve, but it keeps the connection
                // to the arrow much cleaner.
                Point2D.Float hp = new Point2D.Float();
                AffineTransform tx = new AffineTransform();
                tx.setToTranslation(headX, headY);
                tx.rotate(mRotationHead);
                tx.translate(0, HeadShape.getHeight());
                tx.transform(hp, hp);
                mQuad.x1 = hp.x;
                mQuad.y1 = hp.y;
            } else {
                mQuad.x1 = headX;
                mQuad.y1 = headY;
            }
            
            mQuad.x2 = tailX;
            mQuad.y2 = tailY;

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

            bounds.add(mQuad.ctrlx, mQuad.ctrly);

        } else if (mCurveControls == 2) {
            mCubic.x1 = headX;
            mCubic.y1 = headY;
            mCubic.x2 = tailX;
            mCubic.y2 = tailY;

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

            // Add the centers of the two control lines, where we put the controllers.
            bounds.add((mCubic.ctrlx1 + headX) / 2,
                       (mCubic.ctrly1 + headY) / 2);
            bounds.add((mCubic.ctrlx2 + tailX) / 2,
                       (mCubic.ctrly2 + tailY) / 2);
                
            
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
            // throw out first point -- kept as headX/headY
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
        
        // we currently use the stroke width drawn around the arrows
        // to keep them reasonably sized relative to the line, but
        // we don't want any dash-pattern in the stroke for this
        if (mStrokeStyle.get() == StrokeStyle.SOLID)
            dc.g.setStroke(this.stroke);
        else
            dc.g.setStroke(StrokeStyle.SOLID.makeStroke(mStrokeWidth.get()));
            
        if ((mArrowState.get() & ARROW_HEAD) != 0) {
            dc.g.setColor(getStrokeColor());
            dc.g.translate(headX, headY);
            dc.g.rotate(mRotationHead);

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
            dc.g.translate(tailX, tailY);
            dc.g.rotate(mRotationTail);
            dc.g.translate(-TailShape.getWidth() / 2, 0); // center shape on point 
            dc.g.fill(TailShape);
            dc.g.draw(TailShape);
            
            dc.g.setTransform(savedTransform);
        }
    }

    public boolean hasAbsoluteMapLocation() { return true; }

    
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
        if (head != null && tail != null) { // todo cleanup
        if ((head != null && head.getScale() != 1f) || (tail != null && tail.getScale() != 1f)) {
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
                float scale = getMapScaleF();
                if (scale == 1f)
                    g.setStroke(stroke);
                else
                    g.setStroke(mStrokeStyle.get().makeStroke(strokeWidth * scale));
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
        
        if (headIsPruned || tailIsPruned) {
            float size = 7;
            //if (dc.zoom < 1) size /= dc.zoom;
            RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, size,size);
            //Composite composite = dc.g.getComposite();
            //dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            dc.g.setColor(Color.red);
            if (headIsPruned) {
                dot.setFrameFromCenter(headX, headY, headX+size/2, headY+size/2);
                dc.g.fill(dot);
            }
            if (tailIsPruned) {
                dot.setFrameFromCenter(tailX, tailY, tailX+size/2, tailY+size/2);
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
                dot.setFrameFromCenter(headX, headY, headX+size/2, headY+size/2);
                dc.g.fill(dot);
            }
            if (tailgroup || DEBUG.BOXES) {
                dot.setFrameFromCenter(tailX, tailY, tailX+size/2, tailY+size/2);
                if (DEBUG.BOXES) dc.g.setColor(Color.red);
                dc.g.fill(dot);
            }
            dc.g.setComposite(composite);
        }
        */
                
        if (DEBUG.CONTAINMENT) {
            dc.setAbsoluteStroke(0.5);
            dc.g.setColor(COLOR_SELECTION);
            g.draw(getBounds());
        }

        //if (dc.drawAbsoluteLinks) dc.setAbsoluteDrawing(false);
        
    }

    @Override public Color getRenderFillColor(DrawContext dc) {
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
                double scale = getMapScale();
                if (scale != 1)
                    dc.g.scale(scale, scale);
                //if (isZoomedFocus()) g.scale(getScale(), getScale());
                // todo: need to re-center label when this component relative to scale,
                // and patch contains to understand a scaled label box...
                textBox.draw(dc);
                
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


    protected void layoutImpl(Object triggerKey)
    {
        float cx;
        float cy;

        if (mCurveControls > 0) {
            cx = mCurveCenterX;
            cy = mCurveCenterY;
        } else {
            cx = getCenterX();
            cy = getCenterY();
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

    public float getCenterX() {
        return mCurveControls > 0 ? mCurveCenterX : (headX + tailX) / 2;
    }
    public float getCenterY() {
        return mCurveControls > 0 ? mCurveCenterY : (headY + tailY) / 2;
    }


    /** Create a duplicate LWLink.  The new link will
     * not be connected to any endpoints */
    public LWComponent duplicate(CopyContext cc)
    {
        //todo: make sure we've got everything (styles, etc)
        LWLink link = (LWLink) super.duplicate(cc);
        link.headX = headX;
        link.headY = headY;
        link.tailX = tailX;
        link.tailY = tailY;
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
    
    public String paramString()
    {
        String s =
            " " + (int)headX+","+(int)headY
            + " -> " + (int)tailX+","+(int)tailY;
        if (getControlCount() == 1)
            s += " cc1"; // quadratic
        else if (getControlCount() == 2)
            s += " cc2"; // cubic

        //s += "\n\t" + head + "\n\t" + tail;
        
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
    
    /** @deprecated -- for persistance/init ONLY */
    public void setHeadPoint(Point2D p) {
        if (mXMLRestoreUnderway) {
            headX = (float) p.getX();
            headY = (float) p.getY();
        } else {
            setHeadPoint((float)p.getX(), (float)p.getY());
        }
    }
    /** @deprecated -- for persistance/init ONLY  */
    public void setTailPoint(Point2D p) {
        if (mXMLRestoreUnderway) {
            tailX = (float) p.getX();
            tailY = (float) p.getY();
        } else {
            setTailPoint((float)p.getX(), (float)p.getY());
        }
    }
    /** for persistance/init ONLY */
    public Point2D.Float getHeadPoint() {
        return new Point2D.Float(headX, headY);
    }
    /** for persistance/init ONLY */
    public Point2D.Float getTailPoint() {
        return new Point2D.Float(tailX, tailY);
    }

    // these two to support a special dynamic link
    // which we use while creating a new link
    //boolean viewerCreationLink = false;
    // todo: this boolean a hack until we no longer need to use
    // clip-regions to draw the links
    LWLink(LWComponent tail)
    {
        initLink();
        //viewerCreationLink = true;
        this.tail = tail;
        setStrokeWidth(2f); //todo config: default link width
    }
    
    // sets head WIHOUT adding a link ref -- used for
    // temporary drawing of link hack during drag outs --
    // you know, we should just skip using a LWLink object
    // for that crap alltogether. TODO
    void setTemporaryEndPoint1(LWComponent head)
    {
        this.head = head;
    }
    
    
}
