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

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import javax.swing.JTextArea;

/**
 * LWLink.java
 *
 * Draws a view of a Link on a java.awt.Graphics2D context,
 * and offers code for user interaction.
 *
 * Note that links have position (always their mid-point) only so that
 * there's a place to connect for another link and/or a place for
 * the label.  Having a size doesn't actually make much sense, tho
 * we inherit from LWComponent.
 *
 * @author Scott Fraize
 * @version 6/1/03
 */
public class LWLink extends LWComponent
    implements Link,
               LWSelection.ControlListener
{
    public final static Font DEFAULT_FONT = VueResources.getFont("link.font");
    public final static Color DEFAULT_LABEL_COLOR = java.awt.Color.darkGray;
    
    //private static final Color ContrastFillColor = new Color(255,255,255,224);
    //private static final Color ContrastFillColor = new Color(255,255,255);
    // transparency fill is actually just distracting
    
    private LWComponent ep1;
    private LWComponent ep2;
    private Line2D.Float line = new Line2D.Float();
    private QuadCurve2D.Float quadCurve = null;
    private CubicCurve2D.Float cubicCurve = null;
    private Shape curve = null;
    private float mCurveCenterX;
    private float mCurveCenterY;

    private int curveControls = 0; // 0=straight, 1=quad curved, 2=cubic curved
    
    private float centerX;
    private float centerY;
    private float startX;       // todo: either consistently use these or the values in this.line
    private float startY;
    private float endX;
    private float endY;
    private String endPoint1_ID; // used only during restore
    private String endPoint2_ID; // used only during restore
    
    private boolean ordered = false; // not doing anything with this yet
    private int endPoint1Style = 0;
    private int endPoint2Style = 0;
    
    // todo: create set of arrow types
    private final float ArrowBase = 5;
    private RectangularShape ep1Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    private RectangularShape ep2Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);

    private boolean endpointMoved = true; // has an endpoint moved since we last compute shape?

    /** neither endpoint has arrow */
    public static final int ARROW_NONE = 0;
    /** endpoint 1 has arrow */
    public static final int ARROW_EP1 = 0x1;
    /** endpoint 2 has arrow */
    public static final int ARROW_EP2 = 0x2;
    /** both endpoints have arrows */
    public static final int ARROW_BOTH = ARROW_EP1+ARROW_EP2;
    
    private int mArrowState = ARROW_NONE;
    
    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         11, 9,
                         Color.darkGray,
                         LWIcon.Block.HORIZONTAL,
                         LWIcon.Block.COORDINATES_MAP);
    
    /**
     * Used only for restore -- must be public
     */
    public LWLink() {}

    /**
     * Create a new link between two LWC's
     */
    public LWLink(LWComponent ep1, LWComponent ep2)
    {
        if (ep1 == null || ep2 == null)
            throw new IllegalArgumentException("LWLink: ep1=" + ep1 + " ep2=" + ep2);
        setDefaults(this);
        setComponent1(ep1);
        setComponent2(ep2);
        computeLinkEndpoints();
    }

    private final String Key_LinkStartPoint = "link.start.location";
    private final String Key_LinkEndPoint = "link.end.location";

    /**
     * @param key property key (see LWKey)
     * @return object representing appropriate value
     */
    public Object getPropertyValue(Object key)
    {
        // if we create key objects, get/set property value methods
        // could all be reduced to a single one in LWComponent,
        // that calls Key.getValue(component) -- well, almost
        // would still need to cast down, so every new class
        // would still have get/set prop value, but it just
        // does a cast.  The key classes would share a superclass,
        // but be declared locally, and made public if desired,
        // and then when referenced globally, would appear as:
        // LWLink.Key_Arrows.  Or, if maintated as a group,
        // LWLink.Keys.Arrows.
        // May still want some global keys: hierachy changing?
        // Well, I suppose thouse could be LWContainer events...

        // so a key is a way of linking a setter/getter to
        // a name, with the new option of adding properties
        // on the key.  Also, it can include value interpolators
        // for animations.  We'll really need to have the prop
        // key handling the interpolators because if we just
        // do it by type, we'll try and interpolate, sa
        // the Integer value for arrow state, which is
        // really a discrete 3 state value.  Altho, 
        // it would be handly for the Key superclass to
        // have a bunch of built-in type interpolaters that
        // anyone could use.
        
        if (key == LWKey.LinkArrows)
            return new Integer(getArrowState());
        else if (key == LWKey.LinkCurves)
            return new Integer(getControlCount());
        else if (key == Key_LinkStartPoint)
            return getPoint1();
        else if (key == Key_LinkEndPoint)
            return getPoint2();
        else
            return super.getPropertyValue(key);
    }

    public void setProperty(final Object key, Object val)
    {
        if (key == LWKey.LinkArrows)
            setArrowState(((Integer) val).intValue());
        else if (key == LWKey.LinkCurves)
            setControlCount(((Integer) val).intValue());
        else if (key == Key_LinkStartPoint)
            setPoint1((Point2D.Float)val);
        else if (key == Key_LinkEndPoint)
            setPoint2((Point2D.Float)val);
        else
            super.setProperty(key, val);
    }
    

    static LWLink setDefaults(LWLink l)
    {
        l.setFont(DEFAULT_FONT);
        l.setTextColor(DEFAULT_LABEL_COLOR);
        l.setStrokeWidth(1f); //todo config: default link width
        return l;
    }

    public boolean supportsUserLabel() {
        return true;
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

    private void setStartPoint(Point2D p) {
        setStartPoint((float)p.getX(), (float)p.getY());
    }

    private void setStartPoint(float x, float y) {
        Object old = new Point2D.Float(startX, startY);
        startX = x;
        startY = y;
        endpointMoved = true;
        notify(Key_LinkStartPoint, old);
        //notify("link.ep1.location", new Undoable(old) { void undo() { setStartPoint((Point2D) old); }} );
    }
    private void setEndPoint(Point2D p) {
        setEndPoint((float)p.getX(), (float)p.getY());
    }
    private void setEndPoint(float x, float y) {
        Object old = new Point2D.Float(endX, endY);
        endX = x;
        endY = y;
        endpointMoved = true;
        notify(Key_LinkEndPoint, old);
        //notify("link.ep2.location", new Undoable(old) { void undo() { setEndPoint((Point2D) old); }} );
    }

    
    /** interface ControlListener handler */
    public void controlPointPressed(int index, MapMouseEvent e) { }
    
    /** interface ControlListener handler
     * One of our control points (an endpoint or curve control point).
     */
    public void controlPointMoved(int index, MapMouseEvent e)
    {
        //System.out.println("LWLink: control point " + index + " moved");
        
        if (index == 0) {
            // endpoint 1 (start)
            setComponent1(null); // disconnect from node
            setStartPoint(e.getMapPoint());
            LinkTool.setMapIndicationIfOverValidTarget(ep2, this, e);
        } else if (index == 1) {
            // endpoint 2 (end)
            setComponent2(null);  // disconnect from node
            setEndPoint(e.getMapPoint());
            LinkTool.setMapIndicationIfOverValidTarget(ep1, this, e);
        } else if (index == 2) {
            // optional control 0 for curve
            setCtrlPoint0(e.getMapPoint());
        } else if (index == 3) {
            // optional control 1 for curve
            setCtrlPoint1(e.getMapPoint());
        } else
            throw new IllegalArgumentException("LWLink ctrl point > 2");

    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, MapMouseEvent e)
    {
        LWComponent dropTarget = e.getViewer().getIndication();
        // TODO BUG: above doesn't work if everything is selected
        if (DEBUG.MOUSE) System.out.println("LWLink: control point " + index + " dropped on " + dropTarget);
        if (dropTarget != null) {
            if (index == 0 && ep1 == null && ep2 != dropTarget)
                setComponent1(dropTarget);
            else if (index == 1 && ep2 == null && ep1 != dropTarget)
                setComponent2(dropTarget);
            // todo: ensure paint sequence same as LinkTool.makeLink
        }
    }


    //private Point2D.Float[] controlPoints = new Point2D.Float[2];
    //public Point2D.Float[] getControlPoints()
    private LWSelection.ControlPoint[] controlPoints = new LWSelection.ControlPoint[2];
    /** interface ControlListener */
    private final Color freeEndpointColor = new Color(128,0,0);
    public LWSelection.ControlPoint[] getControlPoints()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        // todo opt: don't create these new Point2D's all the time --
        // we iterate through this on every paint for each link in selection
        // todo: need to indicate a color for these so we
        // can show a connection as green and a hanging endpoint as red
        //controlPoints[0] = new Point2D.Float(startX, startY);
        //controlPoints[1] = new Point2D.Float(endX, endY);
        controlPoints[0] = new LWSelection.ControlPoint(startX, startY, COLOR_SELECTION);
        controlPoints[1] = new LWSelection.ControlPoint(endX, endY, COLOR_SELECTION);
        controlPoints[0].setColor(null); // no fill (transparent)
        controlPoints[1].setColor(null);
        if (this.ep1 == null) controlPoints[0].setColor(COLOR_SELECTION_HANDLE);
        if (this.ep2 == null) controlPoints[1].setColor(COLOR_SELECTION_HANDLE);
        if (curveControls == 1) {
            //controlPoints[2] = (Point2D.Float) quadCurve.getCtrlPt();
            controlPoints[2] = new LWSelection.ControlPoint(quadCurve.getCtrlPt(), COLOR_SELECTION_CONTROL);
        } else if (curveControls == 2) {
            //controlPoints[2] = (Point2D.Float) cubicCurve.getCtrlP1();
            //controlPoints[3] = (Point2D.Float) cubicCurve.getCtrlP2();
            controlPoints[2] = new LWSelection.ControlPoint(cubicCurve.getCtrlP1(), COLOR_SELECTION_CONTROL);
            controlPoints[3] = new LWSelection.ControlPoint(cubicCurve.getCtrlP2(), COLOR_SELECTION_CONTROL);
        }

        return controlPoints;
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
        return curveControls > 0;
    }

    /**
     * This sets a link's curve controls to 0, 1 or 2 and manages
     * switching betweens states.  0 is straignt, 1 is quad curve,
     * 2 is cubic curve.  Also called by persistance to establish
     * curved state of a link.
     */
    public void setControlCount(int newControlCount)
    {
        //System.out.println(this + " setting CONTROL COUNT " + newControlCount);
        if (newControlCount > 2)
            throw new IllegalArgumentException("LWLink: max 2 control points " + newControlCount);

        if (curveControls == newControlCount)
            return;

        // Note: Float.MIN_VALUE is used as a special marker
        // to say that that control point hasn't been initialized
        // yet.

        if (curveControls == 0 && newControlCount == 1) {
            if (quadCurve != null) {
                // restore old curve
                this.curve = quadCurve;
            } else {
                this.curve = this.quadCurve = new QuadCurve2D.Float();
                this.quadCurve.ctrlx = Float.MIN_VALUE;
            }
        }
        else if (curveControls == 0 && newControlCount == 2) {
            if (cubicCurve != null) {
                // restore old curve
                this.curve = cubicCurve;
            } else {
                this.curve = this.cubicCurve = new CubicCurve2D.Float();
                this.cubicCurve.ctrlx1 = Float.MIN_VALUE;
                this.cubicCurve.ctrlx2 = Float.MIN_VALUE;
            }
        }
        else if (curveControls == 1 && newControlCount == 2) {
            // adding one (up from quadCurve to cubicCurve)
            if (cubicCurve != null) {
                // restore old cubic curve if had one
                this.curve = cubicCurve;
            } else {
                this.curve = this.cubicCurve = new CubicCurve2D.Float();
                // if new & had quadCurve, keep the old ctrl point as one of the new ones
                this.cubicCurve.ctrlx1 = quadCurve.ctrlx;
                this.cubicCurve.ctrly1 = quadCurve.ctrly;
                this.cubicCurve.ctrlx2 = Float.MIN_VALUE;
            }
        }
        else if (curveControls == 2 && newControlCount == 1) {
            // removing one (drop from cubicCurve to quadCurve)
            if (quadCurve != null) {
                // restore old quad curve if had one
                this.curve = quadCurve;
            } else {
                this.curve = this.quadCurve = new QuadCurve2D.Float();
                this.quadCurve.ctrlx = cubicCurve.ctrlx1;
                this.quadCurve.ctrly = cubicCurve.ctrly1;
            }
        } else {
            // this means we're straight (newControlCount == 0)
            this.curve = null;
        }
        
        Object old = new Integer(curveControls);
        curveControls = newControlCount;
        this.controlPoints = new LWSelection.ControlPoint[2 + curveControls];
        endpointMoved = true;
        notify(LWKey.LinkCurves, old);
    }

    /** for persistance */
    public int getControlCount()
    {
        return curveControls;
    }

    /** for persistance */
    public Point2D.Float getPoint1()
    {
        return new Point2D.Float(startX, startY);
    }
    /** for persistance */
    public Point2D.Float getPoint2()
    {
        return new Point2D.Float(endX, endY);
    }
    /** for persistance */
    public void setPoint1(Point2D.Float p)
    {
        startX = p.x;
        startY = p.y;
    }
    /** for persistance */
    public void setPoint2(Point2D.Float p)
    {
        endX = p.x;
        endY = p.y;
    }
    
    /** for persistance */
    public Point2D getCtrlPoint0()
    {
        if (curveControls == 0)
            return null;
        else if (curveControls == 2)
            return cubicCurve.getCtrlP1();
        else
            return quadCurve.getCtrlPt();
    }
    /** for persistance */
    public Point2D getCtrlPoint1()
    {
        return (curveControls == 2) ? cubicCurve.getCtrlP2() : null;
    }
    
    /** for persistance and ControlListener */
    public void setCtrlPoint0(Point2D point) {
        setCtrlPoint0((float) point.getX(), (float) point.getY());
    }

    public void setCtrlPoint0(float x, float y)
    {
        if (curveControls == 0) {
            setControlCount(1);
            if (DEBUG.UNDO) System.out.println("implied curved link by setting control point 0 " + this);
        }
        Object old;
        if (curveControls == 2) {
            old = new Point2D.Float(cubicCurve.ctrlx1, cubicCurve.ctrly1); 
            cubicCurve.ctrlx1 = x;
            cubicCurve.ctrly1 = y;
        } else {
            old = new Point2D.Float(quadCurve.ctrlx, quadCurve.ctrly);
            quadCurve.ctrlx = x;
            quadCurve.ctrly = y;
        }
        endpointMoved = true;
        notify("link.control.0", new Undoable(old) { void undo() { setCtrlPoint0((Point2D)old); }} );
    }

    /** for persistance and ControlListener */
    public void setCtrlPoint1(Point2D point) {
        setCtrlPoint1((float) point.getX(), (float) point.getY());
    }
    public void setCtrlPoint1(float x, float y)
    {
        if (curveControls < 2) {
            setControlCount(2);
            if (DEBUG.UNDO) System.out.println("implied cubic curved link by setting a control point 1 " + this);
        }
        Object old = new Point2D.Float(cubicCurve.ctrlx2, cubicCurve.ctrly2); 
        cubicCurve.ctrlx2 = x;
        cubicCurve.ctrly2 = y;
        endpointMoved = true;
        notify("link.control.1", new Undoable(old) { void undo() { setCtrlPoint1((Point2D)old); }} );
    }

    protected void removeFromModel()
    {
        super.removeFromModel();
        if (ep1 != null) ep1.removeLinkRef(this);
        if (ep2 != null) ep2.removeLinkRef(this);
    }

    protected void restoreToModel()
    {
        super.restoreToModel();
        if (ep1 != null) ep1.addLinkRef(this);
        if (ep2 != null) ep2.addLinkRef(this);
        endpointMoved = true; // for some reason cached label position is off on restore
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        if (ep1 == null || ep2 == null)
            return false;
        return ep1.getParent() == ep2 || ep2.getParent() == ep1;
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
        if (ep1 == null || ep2 == null)
            return getParent() instanceof LWNode;
        if (ep1.getParent() == ep2 || ep2.getParent() == ep1)
            return true;
        return ep1.getParent() == ep2.getParent() && ep1.getParent() instanceof LWNode;
    }
    
    public Shape getShape()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        if (curveControls > 0)
            return this.curve;
        else
            return this.line;
    }

    public void mouseOver(MapMouseEvent e)
    {
        if (mIconBlock.isShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }
    
    private final int MaxZoom = 1; //todo: get from Zoom code
    private final float SmallestScaleableStrokeWidth = 1 / MaxZoom;
    public boolean intersects(Rectangle2D rect)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        float w = getStrokeWidth();
        if (true || w <= SmallestScaleableStrokeWidth) {
            //if (isCurved) { System.err.println("curve intersects=" + rect.intersects(curve.getBounds2D())); }
            if (curve != null) {
                if (curve.intersects(rect)) // checks entire INTERIOR (concave region) of the curve
                //if (curve.getBounds2D().intersects(rect)) // todo perf: cache bounds -- why THIS not working???
                    return true;
            } else {
                if (rect.intersectsLine(this.line))
                    return true;
            }
            if (mIconBlock.intersects(rect))
                return true;
            else if (hasLabel())
                return labelBox.intersectsMapRect(rect);
            //return rect.intersects(getLabelX(), getLabelY(),
            //                         labelBox.getWidth(), labelBox.getHeight());
            else
                return false;
        } else {
            // todo: finish 
            Shape s = this.stroke.createStrokedShape(this.line); // todo: cache this
            return s.intersects(rect);
            // todo: ought to compensate for stroke shrinkage
            // due to a link to a child (or remove that feature)
            
            /*
            //private Line2D edge1 = new Line2D.Float();
            //private Line2D edge2 = new Line2D.Float();
            // probably faster to do it this way for vanilla lines,
            // tho also need to compute perpedicular segments from
            // endpoints.
            edge1.setLine(this.line);//move "left"
            if (rect.intersectsLine(edge1))
                return true;
            edge1.setLine(this.line); //move "right"
            if (rect.intersectsLine(edge2))
                return true;
            */
        }
    }

    public boolean contains(float x, float y)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        if (curve != null) {
            // Java curve shapes check the entire concave region for containment.
            // todo perf: would be more accurate to coursely flatten the curve
            // and check the segments using stroke width and distance
            // from each segment as we do below when link is line,
            // tho this would be much slower.  Could cache flattening
            // iterator or it's resultant segments to make faster.
            if (curve.contains(x, y))
                return true;
        } else {
            float maxDist = getStrokeWidth() / 2;
            final int slop = 2; // near miss on line still hits it
            // todo: can make slop bigger if implement a two-pass
            // hit detection process that does absolute on first pass,
            // and slop hits on second pass (otherwise, if this too big,
            // clicking in a node near a link to it that's on top of it
            // will select the link, and not the node).
            if (maxDist < slop)
                maxDist = slop;
            if (line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1)
                return true;
        }
        if (!isNestedLink()) {
            if (mIconBlock.contains(x, y))
                return true;
            else if (hasLabel())
                return labelBox.containsMapLocation(x, y); // bit of a hack to do this way
        }
        return false;
    }
    
    /**
     * Does x,y fall within the selection target for this component.
     * For links, we need to get within 20 pixels of the center.
     */
    public boolean targetContains(float x, float y)
    {
        if (endpointMoved)
            computeLinkEndpoints();
        float swath = getStrokeWidth() / 2 + 20; // todo: config/preference
        float sx = this.centerX - swath;
        float sy = this.centerY - swath;
        float ex = this.centerX + swath;
        float ey = this.centerY + swath;
        
        return x >= sx && x <= ex && y >= sy && y <= ey;
    }
    
    /* TODO FIX: not everybody is going to be okay with these returning null... */
    public LWComponent getComponent1() { return ep1; }
    public LWComponent getComponent2() { return ep2; }
    public MapItem getItem1() { return ep1; }
    public MapItem getItem2() { return ep2; }

    void disconnectFrom(LWComponent c)
    {
        boolean changed = false;
        if (ep1 == c)
            setComponent1(null);
        else if (ep2 == c)
            setComponent2(null);
        else
            throw new IllegalArgumentException(this + " cannot disconnect: not connected to " + c);
    }
            
    void setComponent1(LWComponent c)
    {
        if (c == ep1)
            return;
        if (ep1 != null)
            ep1.removeLinkRef(this);            
        Object old = this.ep1;
        this.ep1 = c;
        if (c != null)
            c.addLinkRef(this);
        endPoint1_ID = null;
        endpointMoved = true;
        notify("link.ep1.connect", new Undoable(old) { void undo() { setComponent1((LWComponent)old); }} );
    }
    void setComponent2(LWComponent c)
    {
        if (c == ep2)
            return;
        if (ep2 != null)
            ep2.removeLinkRef(this);            
        Object old = this.ep2;
        this.ep2 = c;
        if (c != null)
            c.addLinkRef(this);
        endPoint2_ID = null;
        endpointMoved = true;
        notify("link.ep2.connect", new Undoable(old) { void undo() { setComponent2((LWComponent)old); }} );
    }

    
    // used only during save
    public String getEndPoint1_ID()
    {
        //System.err.println("getEndPoint1_ID called for " + this);
        if (this.ep1 == null)
            return this.endPoint1_ID;
        else
            return this.ep1.getID();
    }
    // used only during save
    public String getEndPoint2_ID()
    {
        //System.err.println("getEndPoint2_ID called for " + this);
        if (this.ep2 == null)
            return this.endPoint2_ID;
        else
            return this.ep2.getID();
    }

    // used only during restore
    public void setEndPoint1_ID(String s)
    {
        this.endPoint1_ID = s;
    }
    // used only during restore
    public void setEndPoint2_ID(String s)
    {
        this.endPoint2_ID = s;
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

    public java.util.Iterator getLinkEndpointsIterator()
    {
        java.util.List endpoints = new java.util.ArrayList(2);
        if (this.ep1 != null) endpoints.add(this.ep1);
        if (this.ep2 != null) endpoints.add(this.ep2);
        return new VueUtil.GroupIterator(endpoints,
                                         super.getLinkEndpointsIterator());
        
    }
    
    public java.util.List getAllConnectedComponents()
    {
        java.util.List list = new java.util.ArrayList(getLinkRefs().size() + 2);
        list.addAll(getLinkRefs());
        list.add(getComponent1());
        list.add(getComponent2());
        return list;
    }
    
    /**
     * Any free (unattached) endpoints get translated by
     * how much we're moving, as well as any control points.
     * If both ends of this link are connected and it has
     * no control points (it's straight, not curved) calling
     * setLocation will have absolutely no effect on it.
     */

    public void setLocation(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();

        if (ep1 == null)
            setStartPoint(startX + dx, startY + dy);

        if (ep2 == null)
            setEndPoint(endX + dx, endY + dy);

        if (curveControls == 1) {
            setCtrlPoint0(quadCurve.ctrlx + dx,
                          quadCurve.ctrly + dy);
        } else if (curveControls == 2) {
            setCtrlPoint0(cubicCurve.ctrlx1 + dx,
                          cubicCurve.ctrly1 + dy);
            setCtrlPoint1(cubicCurve.ctrlx2 + dx,
                          cubicCurve.ctrly2 + dy);
        }
    }


    /**
     * Compute the endpoints of this link based on the edges
     * of the shapes we're connecting.  To do this we draw
     * a line from the center of one shape to the center of
     * the other, and set the link endpoints to the places where
     * this line crosses the edge of each shape.  If one of
     * the shapes is a straight line, or for some reason
     * a shape doesn't have a facing "edge", or if anything
     * unpredicatable happens, we just leave the connection
     * point as the center of the object.
     */
    private float[] intersection = new float[2]; // result cache for intersection coords
    void computeLinkEndpoints()
    {
        //if (ep1 == null || ep2 == null) throw new IllegalStateException("LWLink: attempting to compute shape w/out endpoints");
        // we clear this at the top in case another thread
        // (e.g., AWT paint) clears it again while we're
        // in here
        endpointMoved = false;

        if (ep1 != null) {
            startX = ep1.getCenterX();
            startY = ep1.getCenterY();
        }
        if (ep2 != null) {
            endX = ep2.getCenterX();
            endY = ep2.getCenterY();
        }

        
        // TODO: sort out setting cubic control points when
        // we're in here the first time and we haven't even
        // computed the real intersected endpoints yet.
        // (same applies to quadcurves but seems to be working better)

        if (curveControls > 0) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
            this.centerX = startX - (startX - endX) / 2;
            this.centerY = startY - (startY - endY) / 2;

            if (curveControls == 2) {
                    /*
                    // disperse the 2 control points -- todo: get working
                    float offX = Math.abs(startX - centerX) * 0.66f;
                    float offY = Math.abs(startY - centerY) * 0.66f;
                    cubicCurve.ctrlx1 = startX + offX;
                    cubicCurve.ctrly1 = startY + offY;
                    cubicCurve.ctrlx2 = endX - offX;
                    cubicCurve.ctrly2 = endY - offY;
                    */
                if (cubicCurve.ctrlx1 == Float.MIN_VALUE) {
                    cubicCurve.ctrlx1 = centerX;
                    cubicCurve.ctrly1 = centerY;
                }
                if (cubicCurve.ctrlx2 == Float.MIN_VALUE) {
                    cubicCurve.ctrlx2 = centerX;
                    cubicCurve.ctrly2 = centerY;
                }
            } else {
                if (quadCurve.ctrlx == Float.MIN_VALUE) {
                    // unintialized control points
                    quadCurve.ctrlx = centerX;
                    quadCurve.ctrly = centerY;
                }
            }
        }
        

        float srcX, srcY;
        Shape ep1Shape = ep1 == null ? null : ep1.getShape();
        // if either endpoint shape is a straight line, we don't need to
        // bother computing the shape intersection -- it will just
        // be the default connection point -- the center point.
        
        // todo bug: if it's a CURVED LINK we're connect to, a floating
        // connection point works out if the link approaches from
        // the convex side, but from the concave side, it winds
        // up at the center point for a regular straight link.

        if (ep1Shape != null && !(ep1Shape instanceof Line2D)) {
            if (curveControls == 1) {
                srcX = quadCurve.ctrlx;
                srcY = quadCurve.ctrly;
            } else if (curveControls == 2) {
                srcX = cubicCurve.ctrlx1;
                srcY = cubicCurve.ctrly1;
            } else {
                srcX = endX;
                srcY = endY;
            }
            float[] result = VueUtil.computeIntersection(startX, startY, srcX, srcY, ep1Shape, intersection, 1);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            //if (!Float.isNaN(intersection[0])) startX = intersection[0];
            //if (!Float.isNaN(intersection[1])) startY = intersection[1];
            if (result != VueUtil.NoIntersection) {
                 startX = intersection[0];
                 startY = intersection[1];
            }
        }
        Shape ep2Shape = ep2 == null ? null : ep2.getShape();
        if (ep2Shape != null && !(ep2Shape instanceof Line2D)) {
            if (curveControls == 1) {
                srcX = quadCurve.ctrlx;
                srcY = quadCurve.ctrly;
            } else if (curveControls == 2) {
                srcX = cubicCurve.ctrlx2;
                srcY = cubicCurve.ctrly2;
            } else {
                srcX = startX;
                srcY = startY;
            }
            float[] result = VueUtil.computeIntersection(srcX, srcY, endX, endY, ep2Shape, intersection, 1);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            //if (!Float.isNaN(intersection[0])) endX = intersection[0];
            //if (!Float.isNaN(intersection[1])) endY = intersection[1];
            if (result != VueUtil.NoIntersection) {
                 endX = intersection[0];
                 endY = intersection[1];
            }
        }
        
        this.centerX = startX - (startX - endX) / 2;
        this.centerY = startY - (startY - endY) / 2;
        
        // We only set the size & location here so LWComponent.getBounds
        // can do something reasonable with us for computing/drawing
        // a selection box, and for LWMap.getBounds in computing entire
        // area need to display everything on the map (so we need
        // to include control point so a curve swinging out at the
        // edge is sure to be included in visible area).

        if (curveControls > 0) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
            /*
            if (isCubicCurve) {
                if (false&&cubicCurve.ctrlx1 == Float.MIN_VALUE) {
                    // unintialized control points
                    float offX = Math.abs(startX - centerX) * 0.66f;
                    float offY = Math.abs(startY - centerY) * 0.66f;
                    cubicCurve.ctrlx1 = startX + offX;
                    cubicCurve.ctrly1 = startY + offY;
                    cubicCurve.ctrlx2 = endX - offX;
                    cubicCurve.ctrly2 = endY - offY;
                }
            } else {
                if (false&&quadCurve.ctrlx == Float.MIN_VALUE) {
                    // unintialized control points
                    quadCurve.ctrlx = centerX;
                    quadCurve.ctrly = centerY;
                }
            }
            */

            Rectangle2D.Float bounds = new Rectangle2D.Float();
            bounds.width = Math.abs(startX - endX);
            bounds.height = Math.abs(startY - endY);
            bounds.x = centerX - bounds.width/2;
            bounds.y = centerY - bounds.height/2;
            if (curveControls == 2) {
                bounds.add(cubicCurve.ctrlx1, cubicCurve.ctrly1);
                bounds.add(cubicCurve.ctrlx2, cubicCurve.ctrly2);
            } else {
                bounds.add(quadCurve.ctrlx, quadCurve.ctrly);
            }
            try {
                mChangeSupport.setEventsSuspended();
                // todo check: any problem with events off here?
                setSize(bounds.width, bounds.height);
                setX(bounds.x);
                setY(bounds.y);
            } finally {
                mChangeSupport.setEventsResumed();
            }

        } else {
            try {
                mChangeSupport.setEventsSuspended();
                // todo check: any problem with events off here?
                setSize(Math.abs(startX - endX), Math.abs(startY - endY));
                setX(this.centerX - getWidth()/2);
                setY(this.centerY - getHeight()/2);
            } finally {
                mChangeSupport.setEventsResumed();
            }
        }

        //-------------------------------------------------------
        // Set the stroke line
        //-------------------------------------------------------
        this.line.setLine(startX, startY, endX, endY);
        if (curveControls == 1) {
            quadCurve.x1 = startX;
            quadCurve.y1 = startY;
            quadCurve.x2 = endX;
            quadCurve.y2 = endY;

            // compute approximate on-curve "center" for label

            // We compute a line from the center of control line 1 to
            // the center of control line 2: that line segment is a
            // tangent to the curve who's center is on the curve.
            // (See QuadCurve2D.subdivide)
            
            float ctrlx1 = (quadCurve.x1 + quadCurve.ctrlx) / 2;
            float ctrly1 = (quadCurve.y1 + quadCurve.ctrly) / 2;
            float ctrlx2 = (quadCurve.x2 + quadCurve.ctrlx) / 2;
            float ctrly2 = (quadCurve.y2 + quadCurve.ctrly) / 2;
            mCurveCenterX = (ctrlx1 + ctrlx2) / 2;
            mCurveCenterY = (ctrly1 + ctrly2) / 2;
            
        } else if (curveControls == 2) {
            cubicCurve.x1 = startX;
            cubicCurve.y1 = startY;
            cubicCurve.x2 = endX;
            cubicCurve.y2 = endY;

            // compute approximate on-curve "center" for label
            // (See CubicCurve2D.subdivide)
            float centerx = (cubicCurve.ctrlx1 + cubicCurve.ctrlx2) / 2;
            float centery = (cubicCurve.ctrly1 + cubicCurve.ctrly2) / 2;
            float ctrlx1 = (cubicCurve.x1 + cubicCurve.ctrlx1) / 2;
            float ctrly1 = (cubicCurve.y1 + cubicCurve.ctrly1) / 2;
            float ctrlx2 = (cubicCurve.x2 + cubicCurve.ctrlx2) / 2;
            float ctrly2 = (cubicCurve.y2 + cubicCurve.ctrly2) / 2;
            float ctrlx12 = (ctrlx1 + centerx) / 2;
            float ctrly12 = (ctrly1 + centery) / 2;
            float ctrlx21 = (ctrlx2 + centerx) / 2;
            float ctrly21 = (ctrly2 + centery) / 2;
            mCurveCenterX = (ctrlx12 + ctrlx21) / 2;
            mCurveCenterY = (ctrly12 + ctrly21) / 2;
        }
        
        layout();
        // if there are any links connected to this link, make sure they
        // know that this endpoint has moved.
        updateConnectedLinks();
        
    }

    /**
     * Compute the angle of rotation of the line defined by the two given points
     */
    private double computeAngle(double x1, double y1, double x2, double y2)
    {
        double xdiff = x1 - x2;
        double ydiff = y1 - y2;
        double slope = xdiff / ydiff;
        double slopeInv = 1 / slope;
        double r0 = -Math.atan(slope);
        double deg = Math.toDegrees(r0);
        if (xdiff >= 0 && ydiff >= 0)
            deg += 180;
        else if (xdiff <= 0 && ydiff >= 0)
            deg = -90 - (90-deg);

        if (false&&VueUtil.isMacPlatform()) {
            // Mac MRJ 69.1 / Java 1.4.1 java bug: approaching 45/-45 & 225/-135 degrees,
            // rotations seriously fuck up (most shapes are translated to infinity and
            // back, except at EXACTLY 45 degrees, where it works fine).
            // (FYI: fixed at least as of MRJ 117.1 (probably in java 1.4.2 release)
            final int ew = 10; // error-window: # of degrees around 45 that do broken rotations
            if (deg > 45-ew && deg < 45+ew)
                deg = 45;
            if (deg > -135-ew && deg < -135+ew)
                deg = -135;
        }
        return  Math.toRadians(deg);


        // diagnostics
        /*
        this.label =
            ((float)xdiff) + "/" + ((float)ydiff) + "=" + (float) slope
            + " atan=" + (float) r
            + " deg=[ " + (float) Math.toDegrees(r)
            + " ]";
        getLabelBox().setText(this.label);
        */
        
    }

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

    private void drawArrows(DrawContext dc)
    {
        //-------------------------------------------------------
        // Draw arrows
        //-------------------------------------------------------

        ////ep1Shape.setFrame(this.line.getP1(), new Dimension(arrowSize, arrowSize));
        ////ep1Shape.setFrame(this.line.getX1() - arrowSize/2, this.line.getY1(), arrowSize, arrowSize*2);
        //ep1Shape.setFrame(0,0, arrowSize, arrowSize*2);

        double rotation1 = 0;
        double rotation2 = 0;

        if (curveControls == 1) {
            rotation1 = computeAngle(startX, startY, quadCurve.ctrlx, quadCurve.ctrly);
            rotation2 = computeAngle(endX, endY, quadCurve.ctrlx, quadCurve.ctrly);
        } else if (curveControls == 2) {
            rotation1 = computeAngle(startX, startY, cubicCurve.ctrlx1, cubicCurve.ctrly1);
            rotation2 = computeAngle(endX, endY, cubicCurve.ctrlx2, cubicCurve.ctrly2);
        } else {
            rotation1 = computeAngle(line.getX1(), line.getY1(), line.getX2(), line.getY2());
            rotation2 = rotation1 + Math.PI;  // flip: add 180 degrees
        }

        
        AffineTransform savedTransform = dc.g.getTransform();
        
        dc.g.setStroke(this.stroke);
        dc.g.setColor(getStrokeColor());

        // draw the first arrow
        // todo: adjust the arrow shape with the stroke width
        // do the adjustment in setStrokeWidth, actually.
        //dc.g.translate(line.getX1(), line.getY1());

        if ((mArrowState & ARROW_EP1) != 0) {
            dc.g.translate(startX, startY);
            dc.g.rotate(rotation1);
            dc.g.translate(-ep1Shape.getWidth() / 2, 0); // center shape on point (makes some assumption)
            dc.g.fill(ep1Shape);
            dc.g.draw(ep1Shape);
            dc.g.setTransform(savedTransform);
        }
        
        if ((mArrowState & ARROW_EP2) != 0) {
            // draw the second arrow
            //dc.g.translate(line.getX2(), line.getY2());
            dc.g.translate(endX, endY);
            dc.g.rotate(rotation2);
            dc.g.translate(-ep2Shape.getWidth()/2, 0); // center shape on point 
            dc.g.fill(ep2Shape);
            dc.g.draw(ep2Shape);
            dc.g.setTransform(savedTransform);
        }
    }

    
    public void draw(DrawContext dc)
    {
        if (endpointMoved)
            computeLinkEndpoints();

        super.draw(dc);

        BasicStroke stroke = this.stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        // todo: do we really even want this functionality?
        /*
        if (ep1 != null && ep2 != null) { // todo cleanup
        if ((ep1 != null && ep1.getScale() != 1f) || (ep2 != null && ep2.getScale() != 1f)) {
            float strokeWidth = getStrokeWidth();
            if (ep1.getScale() < ep2.getScale())
                strokeWidth *= ep1.getScale();
            else
                strokeWidth *= ep2.getScale();
            //g.setStroke(new BasicStroke(strokeWidth));
            stroke = new BasicStroke(strokeWidth);
        } else {
            //g.setStroke(this.stroke);
            stroke = this.stroke;
        }
        }
        */
        Graphics2D g = dc.g;
        
        if (isSelected() && !dc.isPrinting()) {
            g.setColor(COLOR_HIGHLIGHT);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));//todo:config
            g.draw(getShape());
        }

        if (DEBUG.BOXES) {
            // Split the curves into green & red halves for debugging
            Composite composite = dc.g.getComposite();
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            
            if (curveControls == 1) {
                QuadCurve2D left = new QuadCurve2D.Float();
                QuadCurve2D right = new QuadCurve2D.Float();
                quadCurve.subdivide(left,right);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(strokeWidth+4));
                g.draw(left);
                g.setColor(Color.red);
                g.draw(right);
            } else if (curveControls == 2) {
                CubicCurve2D left = new CubicCurve2D.Float();
                CubicCurve2D right = new CubicCurve2D.Float();
                cubicCurve.subdivide(left,right);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(strokeWidth+4));
                g.draw(left);
                g.setColor(Color.red);
                g.draw(right);
            }
            dc.g.setComposite(composite);
        }
        
        //-------------------------------------------------------
        // Draw the stroke
        //-------------------------------------------------------

        if (isIndicated() && !dc.isPrinting())
            g.setColor(COLOR_INDICATION);
        //else if (isSelected())
        //  g.setColor(COLOR_SELECTION);
        else
            g.setColor(getStrokeColor());

        g.setStroke(stroke);
        
        if (this.curve != null) {
            //-------------------------------------------------------
            // draw the curve
            //-------------------------------------------------------

            g.draw(this.curve);

            if (!dc.isPrinting() && (isSelected() || DEBUG.BOXES)) {
                //-------------------------------------------------------
                // draw faint lines to control points if selected
                // TODO: need to do this at time we paint the selection,
                // so these are always on top -- perhaps have a
                // LWComponent drawSkeleton, who's default is to
                // just draw an outline shape, which can replace
                // the manual code in MapViewer, and in the case of
                // LWLink, can also draw the control lines.
                //-------------------------------------------------------
                g.setColor(COLOR_SELECTION);
                //g.setColor(Color.red);
                dc.setAbsoluteStroke(0.5);
                if (curveControls == 2) {
                    Line2D ctrlLine = new Line2D.Float(line.getP1(), cubicCurve.getCtrlP1());
                    g.draw(ctrlLine);
                    //float clx1 = line.x1 + cubicCurve.ctrlx
                    ctrlLine.setLine(line.getP2(), cubicCurve.getCtrlP2());
                    g.draw(ctrlLine);
                } else {
                    Line2D ctrlLine = new Line2D.Float(line.getP1(), quadCurve.getCtrlPt());
                    g.draw(ctrlLine);
                    ctrlLine.setLine(line.getP2(), quadCurve.getCtrlPt());
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
            g.draw(this.line);
        }

        if (mArrowState > 0)
            drawArrows(dc);

        if (!isNestedLink())
            drawLinkDecorations(dc);
        
        if (DEBUG.BOXES) {
            RectangularShape dot = new java.awt.geom.Ellipse2D.Float(0,0, 10,10);
            dot.setFrameFromCenter(startX, startY, startX+5, startY+5);
            Composite composite = dc.g.getComposite();
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            dc.g.setColor(Color.green);
            dc.g.fill(dot);
            dot.setFrameFromCenter(endX, endY, endX+5, endY+5);
            dc.g.setColor(Color.red);
            dc.g.fill(dot);
            dc.g.setComposite(composite);
        }
                
        if (DEBUG.CONTAINMENT) { dc.setAbsoluteStroke(0.1); g.draw(getBounds()); }
    }


    private void drawLinkDecorations(DrawContext dc)
    {
        //-------------------------------------------------------
        // Paint label if there is one
        //-------------------------------------------------------
        
        //float textBoxWidth = 0;
        //float textBoxHeight = 0;
        //boolean textBoxBeingEdited = false;

        Color fillColor;
        if (dc.isDraftQuality() || DEBUG.BOXES) {
            fillColor = null;
        } else {
            if (dc.isPrinting() || !isSelected())
                fillColor = getFillColor();
            else
                fillColor = COLOR_HIGHLIGHT;
            if (fillColor == null && getParent() != null)
                fillColor = getParent().getFillColor();
            //fillColor = ContrastFillColor;
        }
        
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

                if (fillColor == null) {
                    textBox.setOpaque(false);
                } else {
                    textBox.setBackground(fillColor);
                    textBox.setOpaque(true);
                }
                
                dc.g.translate(lx, ly);
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
            if (fillColor != null) {
                dc.g.setColor(fillColor);
                dc.g.fill(mIconBlock);
            }
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


    //private Point2D.Float[] mPoints = null;
    //private int mPointCount;
    public void layout()
    {
        float cx;
        float cy;

        if (curveControls > 0) {
            cx = mCurveCenterX;
            cy = mCurveCenterY;
        } else {
            cx = getCenterX();
            cy = getCenterY();
        }
        
        /*
         * For very fancy computation of a curve "center", use below
         * code and then walk the segments computing actual
         * length of curve, then walk again searching for
         * segment at middle of that distance...
         
        if (curveControls > 0) {
            if (mPoints == null)
                mPoints = new Point2D.Float[128];
            // If curved, guess at center of curve via middle segment
            PathIterator i = new java.awt.geom.FlatteningPathIterator(getShape().getPathIterator(null), 0.1);
            float[] point = new float[2];
            int pcnt = 0;
            while (!i.isDone()) {
                i.currentSegment(point);
                if (mPoints[pcnt] == null)
                    mPoints[pcnt] = new Point2D.Float();
                mPoints[pcnt].x = point[0];
                mPoints[pcnt].y = point[1];
                //System.out.println(point[0] + "," + point[1]);
                pcnt++;
                i.next();
            }
            mPointCount = pcnt;
            if (pcnt == 2) {
                cx = getCenterX();
                cy = getCenterY();
            } else {
                int centerp = (int) (pcnt/2+0.5);
                cx = mPoints[centerp].x;
                cy = mPoints[centerp].y;
            }
            System.out.println("CURVE POINTS: " + pcnt);
        } else {
            cx = getCenterX();
            cy = getCenterY();
        }
        */            

        
        float totalHeight = 0;
        float totalWidth = 0;

        boolean putBelow = hasResource();
        
        // Always call LWIcon.Block.layout first to have it compute size/determine if showing
        // before asking it if isShowing()
        
        boolean vertical = false;
        if (hasLabel() && !putBelow) {
            // Check to see if we want to make it vertical
            mIconBlock.setOrientation(LWIcon.Block.VERTICAL);
            mIconBlock.layout();
            vertical = (labelBox.getMapHeight() >= mIconBlock.getHeight());
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
        
        // at right
        //float ibx = getLabelX() + textBoxWidth;
        //float iby = getLabelY();
        // at bottom
        //float ibx = getCenterX() - mIconBlock.width / 2;
        //float iby = getLabelY() + textBoxHeight;
    }

    /*
    public float getLabelY()
    {
        if (hasLabel())
            return labelBox.getMapY();
        else
            return getCenterY();
    }
    public float getLabelX()
    {
        if (hasLabel())
            return labelBox.getMapX();
        else
            return getCenterX();
    }
    
    public float X_getLabelY()
    {
        float y = getCenterY();
        if (hasLabel()) {
            y -= labelBox.getMapHeight() / 2;
            if (mIconBlock.isShowing())
                y -= mIconBlock.getHeight() / 2;
        }
        return y;
    }
    */
    

    /** Create a duplicate LWLink.  The new link will
     * not be connected to any endpoints */
    public LWComponent duplicate()
    {
        //todo: make sure we've got everything (styles, etc)
        LWLink link = (LWLink) super.duplicate();
        link.startX = startX;
        link.startY = startY;
        link.endX = endX;
        link.endY = endY;
        link.centerX = centerX;
        link.centerY = centerY;
        link.ordered = ordered;
        link.mArrowState = mArrowState;
        if (curveControls > 0) {
            link.setCtrlPoint0(getCtrlPoint0());
            if (curveControls > 1)
                link.setCtrlPoint1(getCtrlPoint1());
        }
        return link;
    }
    
    public String paramString()
    {
        String s =
            " " + (int)startX+","+(int)startY
            + " -> " + (int)endX+","+(int)endY;
        if (getControlCount() == 1)
            s += " cc1"; // quadratic
        else if (getControlCount() == 2)
            s += " cc2"; // cubic
        return s;
    }

    // these two to support a special dynamic link
    // which we use while creating a new link
    //boolean viewerCreationLink = false;
    // todo: this boolean a hack until we no longer need to use
    // clip-regions to draw the links
    LWLink(LWComponent ep2)
    {
        //viewerCreationLink = true;
        this.ep2 = ep2;
        setStrokeWidth(2f); //todo config: default link width
    }
    
    // sets ep1 WIHOUT adding a link ref -- used for
    // temporary drawing of link hack during drag outs --
    // you know, we should just skip using a LWLink object
    // for that crap alltogether. TODO
    void setTemporaryEndPoint1(LWComponent ep1)
    {
        this.ep1 = ep1;
    }
    
    
}
