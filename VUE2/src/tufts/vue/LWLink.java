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
    // TODO: are we fully going to allow endpoints to be null?  Everyone who
    // calls getComponent[12] or getItem[12] will have to be prepared for that...
    private LWComponent ep1;
    private LWComponent ep2;
    private Line2D.Float line = new Line2D.Float();
    private QuadCurve2D.Float quadCurve = null;
    private CubicCurve2D.Float cubicCurve = null;
    private Shape curve = null;
    private boolean isCurved = false;
    private boolean isCubicCurve = false;
    
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
    private final float ArrowBase = 8;
    private RectangularShape ep1Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    private RectangularShape ep2Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);

    private boolean endpointMoved = true; // has an endpoint moved since we last compute shape?
    
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
        this.ep1 = ep1;
        this.ep2 = ep2;
        setSize(10,10);
        setFont(FONT_LINKLABEL);
        setTextColor(COLOR_LINK_LABEL);
        setEndPoint1(ep1);
        setEndPoint2(ep2);
        setStrokeWidth(2f); //todo config: default link width
        //computeLinkEndpoints();
        //setTextColor(Color.red); // todo: TEMPORARY ANGLE DEBUG
    }

    /*
    public void setSelected(boolean selected)
    {
        boolean wasSelected = this.selected;
        super.setSelected(selected);
        if (wasSelected != selected && isCurved) {
            //notify("requestSelectionHandle", getCtrlPoint())
            if (selected)
                VUE.ModelSelection.addSelectionControl(getCtrlPoint(), this);
            else
                VUE.ModelSelection.removeSelectionControl(this);
        }
        }*/

    /** interface ControlListener handler
     * One of our control points (an endpoint or curve control point).
     */
    public void controlPointMoved(int index, Point2D p)
    {
        //System.out.println("LWLink: control point " + index + " moved");
        
        if (index == 0) {
            if (ep1 != null) {
                // TODO: removeEndpoint & notify
                ep1.removeLinkRef(this);
                ep1 = null;
                endPoint1_ID = null;
            }
            startX = (float) p.getX();
            startY = (float) p.getY();
            endpointMoved = true;
        } else if (index == 1) {
            if (ep2 != null) {
                // TODO: as above;
                ep2.removeLinkRef(this);
                ep2 = null;
                endPoint2_ID = null;
            }
            endX = (float) p.getX();
            endY = (float) p.getY();
            endpointMoved = true;
        } else if (index == 2) {
            setCtrlPoint0(p);
        } else if (index == 3) {
            setCtrlPoint1(p);
        } else
            throw new IllegalArgumentException("LWLink ctrl point > 2");

    }

    /** interface ControlListener handler */
    public void controlPointDropped(int index, Point2D p)
    {
        // TODO: add getParentMap to LWComponent to ensure getting whole map...
        LWComponent c = getParent().findLWNodeAt((float)p.getX(), (float)p.getY());
        // TODO BUG: above doesn't work if everything is selected
        System.out.println("LWLink: control point " + index + " dropped on " + c);
        // TODO: CAN WE CONSOLODATE NEW LINK CREATION CODE HERE FROM MAPVIEWER???
        // (e.g., handle re-linking from one node to another doing an increment,
        // or perhaps when doing an endpoing drag, don't allow to connect two
        // nodes that are already connected).
        if (c != null && c instanceof LWNode) {
            if (index == 0 && ep1 == null && (ep2 != c || false/*isCubic*/))
                setEndPoint1(c);
            else if (index == 1 && ep2 == null && (ep1 != c || false/*isCubic*/))
                setEndPoint2(c);
        }
    }

    private Point2D.Float[] controlPoints = new Point2D.Float[2];
    /** interface ControlListener */
    public Point2D.Float[] getControlPoints()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        // todo opt: don't create these new Point2D's all the time --
        // we iterate through this ALOT
        controlPoints[0] = new Point2D.Float(startX, startY);
        controlPoints[1] = new Point2D.Float(endX, endY);
        if (isCurved) {
            if (isCubicCurve) {
                controlPoints[2] = (Point2D.Float) cubicCurve.getCtrlP1();
                controlPoints[3] = (Point2D.Float) cubicCurve.getCtrlP2();
            } else {
                controlPoints[2] = (Point2D.Float) quadCurve.getCtrlPt();
            }
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
        return this.isCurved;
    }

    /** set this link to be curved or not.  defaults to a Quadradic (1 control point) curve. */
    public void setCurved(boolean curved)
    {
        this.isCurved = curved;
        System.out.println(this + " SET CURVED " + curved + " cubic="+isCubicCurve);
        if (isCurved) {
            if (isCubicCurve) {
                this.curve = this.cubicCurve = new CubicCurve2D.Float();
                this.controlPoints = new Point2D.Float[4];
                this.cubicCurve.ctrlx1 = Float.MIN_VALUE;
            } else {
                this.curve = this.quadCurve = new QuadCurve2D.Float();
                this.controlPoints = new Point2D.Float[3];
                this.quadCurve.ctrlx = Float.MIN_VALUE;
            }
        } else {
            this.controlPoints = new Point2D.Float[2];
            this.quadCurve = null;
            this.cubicCurve = null;
            this.curve = null;
        }
        endpointMoved = true;
    }

    /** for persistance or setting CubicCurve */
    public void setControlCount(int points)
    {
        System.out.println(this + " setting CONTROL COUNT " + points);
        if (points > 2)
            throw new IllegalArgumentException("LWLink: max 2 control points " + points);
        this.isCurved = (points > 0);
        this.isCubicCurve = (points > 1);
        setCurved(isCurved);
    }

    /** for persistance */
    public int getControlCount()
    {
        if (isCurved)
            return isCubicCurve ? 2 : 1;
        return 0;
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
        return isCurved ? (isCubicCurve ? cubicCurve.getCtrlP1() : quadCurve.getCtrlPt()) : null;
    }
    /** for persistance */
    public Point2D getCtrlPoint1()
    {
        return (isCurved && isCubicCurve) ? cubicCurve.getCtrlP2() : null;
    }
    
    /** for persistance and ControlListener */
    public void setCtrlPoint0(Point2D point)
    {
        if (!isCurved) { // during a restore, this is how we know we're curved
            setCurved(true);
            System.out.println("implied curved link by setting control point 0 " + this);
        }
        if (isCubicCurve) {
            cubicCurve.ctrlx1 = (float) point.getX();
            cubicCurve.ctrly1 = (float) point.getY();
        } else {
            quadCurve.ctrlx = (float) point.getX();
            quadCurve.ctrly = (float) point.getY();
        }
        endpointMoved = true;
    }

    /** for persistance and ControlListener */
    public void setCtrlPoint1(Point2D point)
    {
        if (!isCurved) { // during a restore, this is how we know we're curved
            setCurved(true);
            System.out.println("implied curved link by setting a control point 1 " + this);
            if (!isCubicCurve) {
                System.out.println("implied cubic curve by setting control point 1 " + this);
                this.isCubicCurve = true;
            }
        }
        cubicCurve.ctrlx2 = (float) point.getX();
        cubicCurve.ctrly2 = (float) point.getY();
        endpointMoved = true;
    }

    protected void removeFromModel()
    {
        super.removeFromModel();
        if (ep1 != null) ep1.removeLinkRef(this);
        if (ep2 != null) ep2.removeLinkRef(this);
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        // todo fix: if parent is null this may provide incorrect results
        return (ep1 != null && ep1.getParent() == ep2) || (ep2 != null && ep2.getParent() == ep1);
    }
    
    public Shape getShape()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        if (isCurved)
            return this.curve;
        else
            return this.line;
    }

    /** @deprecated -- use getShape() */
    public Line2D getLine()
    {
        if (endpointMoved)
            computeLinkEndpoints();
        return this.line;
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
            if (isCurved) {
                if (curve.intersects(rect)) // checks entire INTERIOR (concave region) of the curve
                //if (curve.getBounds2D().intersects(rect)) // todo perf: cache bounds -- why THIS not working???
                    return true;
            } else {
                if (rect.intersectsLine(this.line))
                    return true;
            }
            if (this.labelBox != null)
                return labelBox.intersectsMapRect(rect);
            //return rect.intersects(getLabelX(), getLabelY(),
            //                         labelBox.getWidth(), labelBox.getHeight());
            else
                return false;
        } else {
            // todo: finish this!
            Shape s = this.stroke.createStrokedShape(this.line); // todo: cache this!
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
        if (VueUtil.StrokeBug05) {
            x -= 0.5f;
            y -= 0.5f;
        }
        float maxDist = getStrokeWidth() / 2;
        if (isCurved) {
            // QuadCurve2D actually checks the entire concave region for containment
            // todo perf: would be more accurate to coursely flatten the curve
            // and check the segments using stroke width and distance
            // from each segment as we do below when link is line,
            // tho this would be much slower.  Could cache flattening
            // iterator or it's resultant segments to make faster.
            if (curve.contains(x, y))
                return true;
        } else {
            if (line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1)
                return true;
        }
        if (this.labelBox != null)
            return labelBox.containsMapLocation(x, y);
        else
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
        if (VueUtil.StrokeBug05) {
            x -= 0.5f;
            y -= 0.5f;
        }
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

    void setEndPoint1(LWComponent c)
    {
        if (c == null) throw new IllegalArgumentException(this + " attempt to set endPoint1 to null");
        this.ep1 = c;
        //if (c == null) System.err.println(this + " endPoint1 set to null");
        //else
        c.addLinkRef(this);
        //if (this.ep2 != null)
        //computeLinkEndpoints();
        endpointMoved = true;
        notify("link.endpointChanged");
        //System.out.println(this + " ep1 = " + c);
    }
    void setEndPoint2(LWComponent c)
    {
        if (c == null) throw new IllegalArgumentException(this + " attempt to set endPoint2 to null");
        this.ep2 = c;
        //if (c == null) System.err.println(this + " endPointd2 set to null");
        //else
        c.addLinkRef(this);
        //if (this.ep1 != null)
        //computeLinkEndpoints();
        endpointMoved = true;
        notify("link.endpointChanged");
        //System.out.println(this + " ep2 = " + c);
    }
    // interface
    
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
        endpoints.add(getComponent1());
        endpoints.add(getComponent2());
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
    
    public void setLocation(float x, float y)
    {
        float dx = x - getX();
        float dy = y - getY();
        //System.out.println(getLabel() + " setLocation");
        if (ep1 == null) {
            startX += dx;
            startY += dy;
            endpointMoved = true;
        }
        if (ep2 == null) {
            endX += dx;
            endY += dy;
            endpointMoved = true;
        }
        if (isCurved) {
            if (isCubicCurve) {
                cubicCurve.ctrlx1 += dx;
                cubicCurve.ctrly1 += dy;
                cubicCurve.ctrlx2 += dx;
                cubicCurve.ctrly2 += dy;
            } else {
                quadCurve.ctrlx += dx;
                quadCurve.ctrly += dy;
            }
            endpointMoved = true;
        }
    }

    
    /*
    public void X_setLocation(float x, float y)
    {
        float dx = getX() - x;
        float dy = getY() - y;
        //System.out.println(this + " ("+x+","+y+") dx="+dx+" dy="+dy);
        // fixme: moving a link tween links sends
        // multiple move events to nodes at their
        // ends, causing them to move nlinks or more times
        // faster than we're dragging.
        // todo fixme: what if both are children? better
        // perhaps to actually have a child move it's parent
        // around here, yet we can't do generally in setLocation
        // or then we couldn't individually drag a parent
        if (!ep1.isChild())
            ep1.setLocation(ep1.getX() - dx, ep1.getY() - dy);
        if (!ep2.isChild())
            ep2.setLocation(ep2.getX() - dx, ep2.getY() - dy);
        super.setLocation(x,y);
    }
    */

    /**
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */
    private static final float[] _intersection = new float[2];
    private static float[] computeLineIntersection(float s1x1, float s1y1, float s1x2, float s1y2,
                                                    float s2x1, float s2y1, float s2x2, float s2y2)
    {
        // We are defining a line here using the formula:
        // y = mx + b  -- m is slope, b is y-intercept (where crosses x-axis)
        
        boolean m1vertical = (s1x1 == s1x2);
        boolean m2vertical = (s2x1 == s2x2);
        float m1 = Float.NaN;
        float m2 = Float.NaN;
        if (!m1vertical)
            m1 = (s1y1 - s1y2) / (s1x1 - s1x2);
        if (!m2vertical)
            m2 = (s2y1 - s2y2) / (s2x1 - s2x2);
        
        // solve for b using any two points from each line
        // to solve for b:
        //      y = mx + b
        //      y + -b = mx
        //      -b = mx - y
        //      b = -(mx - y)
        // float b1 = -(m1 * s1x1 - s1y1);
        // float b2 = -(m2 * s2x1 - s2y1);
        // System.out.println("m1=" + m1 + " b1=" + b1);
        // System.out.println("m2=" + m2 + " b2=" + b2);

        // if EITHER line is vertical, the x value of the intersection
        // point will obviously have to be the x value of any point
        // on the vertical line.
        
        float x = 0;
        float y = 0;
        if (m1vertical) {   // first line is vertical
            //System.out.println("setting X to first vertical at " + s1x1);
            float b2 = -(m2 * s2x1 - s2y1);
            x = s1x1; // set x to any x point from the first line
            // using y=mx+b, compute y using second line
            y = m2 * x + b2;
        } else {
            float b1 = -(m1 * s1x1 - s1y1);
            if (m2vertical) { // second line is vertical (has no slope)
                //System.out.println("setting X to second vertical at " + s2x1);
                x = s2x1; // set x to any point from the second line
            } else {
                // second line has a slope (is not veritcal: m is valid)
                float b2 = -(m2 * s2x1 - s2y1);
                x = (b2 - b1) / (m1 - m2);
            }
            // using y=mx+b, compute y using first line
            y = m1 * x + b1;
        }
        //System.out.println("x=" + x + " y=" + y);

        _intersection[0] = x;
        _intersection[1] = y;
        return _intersection;
        //return new float[] { x, y };
    }

    // this for debug
    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" };
    
    /*
     * Compute the intersection of an arbitrary shape and a line.
     * If no intersection, returns Float.NaN values for x/y.
     */
    private static final float[] NoIntersection = { Float.NaN, Float.NaN };
    private static float[] computeShapeIntersection(Shape shape,
                                                    float rayX1, float rayY1,
                                                    float rayX2, float rayY2)
    {
        PathIterator i = shape.getPathIterator(null);
        // todo performance: if this shape has no curves (CUBICTO or QUADTO)
        // this flattener is redundant.  Also, it would be faster to
        // actually do the math for arcs and compute the intersection
        // of the arc and the line, tho we can save that for another day.
        i = new java.awt.geom.FlatteningPathIterator(i, 0.5);
        
        float[] seg = new float[6];
        float firstX = 0f;
        float firstY = 0f;
        float lastX = 0f;
        float lastY = 0f;
        int cnt = 0;
        while (!i.isDone()) {
            int segType = i.currentSegment(seg);
            if (cnt == 0) {
                firstX = seg[0];
                firstY = seg[1];
            } else if (segType == PathIterator.SEG_CLOSE) {
                seg[0] = firstX; 
                seg[1] = firstY; 
            }
            float endX, endY;
            //if (segType == PathIterator.SEG_CUBICTO) {
            //    endX = seg[4];
            //    endY = seg[5];
            //} else {
                endX = seg[0];
                endY = seg[1];
            //}
            if (cnt > 0 && Line2D.linesIntersect(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1])) {
                //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                return computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1]);
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return NoIntersection;
    }

        /*
          // a different way of computing link connection
          // points that minimizes over-stroke of
          // our parent (if we have one)
          
        if (ep1.isChild()) {
            //Point2D p = ep1.nearestPoint(endX, endY);
            //startX = (float) p.getX();
            //startY = (float) p.getY();
            // nearest corner
            if (endX > startX)
                startX += ep1.getWidth() / 2;
            else if (endX < startX)
                startX -= ep1.getWidth() / 2;
            if (endY > startY)
                startY += ep1.getHeight() / 2;
            else if (endY < startY)
                startY -= ep1.getHeight() / 2;
        }
        if (ep2.isChild()) {
            //Point2D p = ep2.nearestPoint(startX, startY);
            //endX = (float) p.getX();
            //endY = (float) p.getY();
            // nearest corner
            if (endX > startX)
                endX -= ep2.getWidth() / 2;
            else if (endX < startX)
                endX += ep2.getWidth() / 2;
            if (endY > startY)
                endY -= ep2.getHeight() / 2;
            else if (endY < startY)
                endY += ep2.getHeight() / 2;
        }
        */
    

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
    private void computeLinkEndpoints()
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

        if (isCurved) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
            this.centerX = startX - (startX - endX) / 2;
            this.centerY = startY - (startY - endY) / 2;
            if (isCubicCurve) {
                if (cubicCurve.ctrlx1 == Float.MIN_VALUE) {
                    // unintialized control points
                    float offX = Math.abs(startX - centerX) * 0.66f;
                    float offY = Math.abs(startY - centerY) * 0.66f;
                    cubicCurve.ctrlx1 = startX + offX;
                    cubicCurve.ctrly1 = startY + offY;
                    cubicCurve.ctrlx2 = endX - offX;
                    cubicCurve.ctrly2 = endY - offY;
                    
                    // tmp
                    cubicCurve.ctrlx1 = centerX;
                    cubicCurve.ctrly1 = centerY;
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
            if (isCurved) {
                if (isCubicCurve) {
                    srcX = cubicCurve.ctrlx1;
                    srcY = cubicCurve.ctrly1;
                } else {
                    srcX = quadCurve.ctrlx;
                    srcY = quadCurve.ctrly;
                }
            } else {
                srcX = endX;
                srcY = endY;
            }
            float[]intersection = computeShapeIntersection(ep1Shape, startX, startY, srcX, srcY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0])) startX = intersection[0];
            if (!Float.isNaN(intersection[1])) startY = intersection[1];
        }
        Shape ep2Shape = ep2 == null ? null : ep2.getShape();
        if (ep2Shape != null && !(ep2Shape instanceof Line2D)) {
            if (isCurved) {
                if (isCubicCurve) {
                    srcX = cubicCurve.ctrlx2;
                    srcY = cubicCurve.ctrly2;
                } else {
                    srcX = quadCurve.ctrlx;
                    srcY = quadCurve.ctrly;
                }
            } else {
                srcX = startX;
                srcY = startY;
            }
            float[]intersection = computeShapeIntersection(ep2Shape, srcX, srcY, endX, endY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0])) endX = intersection[0];
            if (!Float.isNaN(intersection[1])) endY = intersection[1];
        }
        
        this.centerX = startX - (startX - endX) / 2;
        this.centerY = startY - (startY - endY) / 2;

        
        // Set our location to the midpoint between
        // the nodes we're connecting.
        // todo: as this happens every paint for every link,
        // make sure we don't raise locations events
        // (override if we decide we LWComponent's normally
        // sending location events, which we don't now).
        //super.setLocation(locX - getWidth()/2,
        //                locY - getHeight()/2);
        //todo: eventually have LWComponent setLocation
        // tell all connected links to recompute themselves...

        // We only set the size & location here so LWComponent.getBounds
        // can do something reasonable with us for computing/drawing
        // a selection box, and for LWMap.getBounds in computing entire
        // area need to display everything on the map (so we need
        // to include control point so a curve swinging out at the
        // edge is sure to be included in visible area).

        if (isCurved) {
            //-------------------------------------------------------
            // INTIALIZE CONTROL POINTS
            //-------------------------------------------------------
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

            Rectangle2D.Float bounds = new Rectangle2D.Float();
            bounds.width = Math.abs(startX - endX);
            bounds.height = Math.abs(startY - endY);
            bounds.x = centerX - bounds.width/2;
            bounds.y = centerY - bounds.height/2;
            if (isCubicCurve) {
                bounds.add(cubicCurve.ctrlx1, cubicCurve.ctrly1);
                bounds.add(cubicCurve.ctrlx2, cubicCurve.ctrly2);
            } else {
                bounds.add(quadCurve.ctrlx, quadCurve.ctrly);
            }
            setSize(bounds.width, bounds.height);
            setX(bounds.x);
            setY(bounds.y);
        } else {
            setSize(Math.abs(startX - endX),
                    Math.abs(startY - endY));
            setX(this.centerX - getWidth()/2);
            setY(this.centerY - getHeight()/2);
        }

        if (VueUtil.StrokeBug05) {
            startX -= 0.5;
            startY -= 0.5;
            endX -= 0.5;
            endY -= 0.5;
        }

        //-------------------------------------------------------
        // Set the stroke line
        //-------------------------------------------------------
        this.line.setLine(startX, startY, endX, endY);
        if (this.isCurved) {
            if (isCubicCurve) {
                cubicCurve.x1 = startX;
                cubicCurve.y1 = startY;
                cubicCurve.x2 = endX;
                cubicCurve.y2 = endY;
                //cubicCurve.setCurve(startX, startY, ctrlX, ctrlY, ctrlX, ctrlY + 20, endX, endY);
            } else {
                quadCurve.x1 = startX;
                quadCurve.y1 = startY;
                quadCurve.x2 = endX;
                quadCurve.y2 = endY;
                //quadCurve.setCurve(startX, startY, ctrlX, ctrlY, endX, endY);
            }
        }
        
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

        if (VueUtil.isMacPlatform()) {
            // Mac MRJ 69.1 / Java 1.4.1 java bug: approaching 45/-45 & 225/-135 degrees,
            // rotations seriously fuck up (most shapes are translated to infinity and
            // back, except at EXACTLY 45 degrees, where it works fine).
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

    private void drawArrows(Graphics2D gg)
    {
        //-------------------------------------------------------
        // Draw arrows
        //-------------------------------------------------------

        ////ep1Shape.setFrame(this.line.getP1(), new Dimension(arrowSize, arrowSize));
        ////ep1Shape.setFrame(this.line.getX1() - arrowSize/2, this.line.getY1(), arrowSize, arrowSize*2);
        //ep1Shape.setFrame(0,0, arrowSize, arrowSize*2);

        double rotation1 = 0;
        double rotation2 = 0;

        if (isCurved) {
            if (isCubicCurve) {
                rotation1 = computeAngle(startX, startY, cubicCurve.ctrlx1, cubicCurve.ctrly1);
                rotation2 = computeAngle(endX, endY, cubicCurve.ctrlx2, cubicCurve.ctrly2);
            } else {
                rotation1 = computeAngle(startX, startY, quadCurve.ctrlx, quadCurve.ctrly);
                rotation2 = computeAngle(endX, endY, quadCurve.ctrlx, quadCurve.ctrly);
            }
        } else {
            rotation1 = computeAngle(line.getX1(), line.getY1(), line.getX2(), line.getY2());
            rotation2 = rotation1 + Math.PI;  // flip: add 180 degrees
        }

        
        AffineTransform savedTransform = gg.getTransform();
        
        gg.setStroke(this.stroke);

        // draw the first arrow
        // todo: adjust the arrow shape with the stroke width
        // do the adjustment in setStrokeWidth, actually.
        //gg.translate(line.getX1(), line.getY1());
        gg.translate(startX, startY);
        gg.rotate(rotation1);
        gg.translate(-ep1Shape.getWidth() / 2, 0); // center shape on point (makes some assumption)
        gg.fill(ep1Shape);
        gg.draw(ep1Shape);

        gg.setTransform(savedTransform);
        
        // draw the second arrow
        //gg.translate(line.getX2(), line.getY2());
        gg.translate(endX, endY);
        gg.rotate(rotation2);
        gg.translate(-ep2Shape.getWidth()/2, 0); // center shape on point 
        gg.fill(ep2Shape);
        gg.draw(ep2Shape);

        gg.setTransform(savedTransform);
    }

    
    public void draw(Graphics2D g)
    {
        if (endpointMoved)
            computeLinkEndpoints();

        //strokeWidth = getWeight() * WEIGHT_RENDER_RATIO;
        //if (strokeWidth > MAX_RENDER_WIDTH)
        //    strokeWidth = MAX_RENDER_WIDTH;
        
        BasicStroke stroke = this.stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        // todo: do we really even want this functionality?
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
    
        //-------------------------------------------------------
        // Fancy border selection: If selected or indicated, draw a standout stroke
        // bigger than the actual stroke first.
        //-------------------------------------------------------
        /*
        if (isIndicated() || isSelected()) {
            if (isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(COLOR_INDICATION);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 2));
            g.draw(getShape());
        }
        */
        
        //-------------------------------------------------------
        // Draw the stroke
        //-------------------------------------------------------

        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else if (isSelected())
            g.setColor(COLOR_SELECTION);
        else
            g.setColor(getStrokeColor());

        g.setStroke(stroke);
        
        if (this.isCurved) {
            //-------------------------------------------------------
            // draw the curve
            //-------------------------------------------------------

            g.draw(this.curve);

            if (isSelected()) {
                // draw faint lines to control points if selected
                g.setColor(COLOR_SELECTION);
                //g.setColor(Color.red);
                //g.setStroke(new BasicStroke(0.5f));
                g.setStroke(new BasicStroke(0.2f));
                // todo opt: less object allocation
                if (isCubicCurve) {
                    Line2D ctrlLine = new Line2D.Float(line.getP1(), cubicCurve.getCtrlP1());
                    g.draw(ctrlLine);
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

        // todo: conditional
        drawArrows(g);

        //-------------------------------------------------------
        // Paint label if there is one
        //-------------------------------------------------------
        
        String label = getLabel();
        if (label != null && label.length() > 0)
        {
            TextBox textBox = getLabelBox();
            if (textBox.getParent() == null) {
                // only draw if we're not an active edit on the map
                float lx = getLabelX();
                float ly = getLabelY();
                textBox.setMapLocation(lx, ly);

                // We force a fill color on link labels to make sure we create
                // a contrast between the text and the background, which otherwise
                // would include the usually black link stroke in the middle, obscuring
                // some of the text.
                // todo perf: only set opaque-bit/background once/when it changes.
                // (probably put a textbox factory on LWComponent and override in LWLink)
                Color c = getFillColor();
                if (c == null)
                    c = getParent().getFillColor(); // todo: maybe have a getBackroundColor which searches up parents
                textBox.setBackground(c);
                textBox.setOpaque(true);
                
                // todo: only need to do above set location when computing line
                // or text changes somehow (content, font) or alignment changes
                g.translate(lx, ly);
                textBox.draw(g);
                if (isSelected()) {
                    Dimension s = textBox.getSize();
                    g.setColor(COLOR_SELECTION);
                    g.setStroke(STROKE_HALF); // todo: needs to be unscaled / handled by selection
                    // -- i guess we could compute based on zoom level -- maybe MapViewer could
                    // keep such a stroke handy for us...
                    g.drawRect(0,0, s.width, s.height);
                }
                g.translate(-lx, -ly);
                
            }
        }
    }

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
        link.isCubicCurve = isCubicCurve;
        link.setCurved(isCurved);
        if (isCurved) {
            link.setCtrlPoint0(getCtrlPoint0());
            if (isCubicCurve)
                link.setCtrlPoint1(getCtrlPoint1());
        }
        return link;
    }
    
    public String paramString()
    {
        return " " + startX+","+startY
            + " -> " + endX+","+endY
            +  " ctrl=" + getControlCount();
    }

    // these two to support a special dynamic link
    // which we use while creating a new link
    boolean viewerCreationLink = false;
    // todo: this boolean a hack until we no longer need to use
    // clip-regions to draw the links
    LWLink(LWComponent ep2)
    {
        viewerCreationLink = true;
        this.ep2 = ep2;
        setStrokeWidth(2f); //todo config: default link width
    }
    
    void setSource(LWComponent ep1)
    {
        this.ep1 = ep1;
    }
    
    
}
