package tufts.vue;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
    implements Link
{
    private LWComponent ep1;
    private LWComponent ep2;
    private Line2D.Float line = new Line2D.Float();
    
    private float centerX;
    private float centerY;
    
    private String endPoint1_ID; // used only during restore
    private String endPoint2_ID; // used only during restore
    
    private boolean ordered = false; // not doing anything with this yet
    private int endPoint1Style = 0;
    private int endPoint2Style = 0;
    
    // todo: create set of arrow types
    private final float ArrowBase = 8;
    private RectangularShape ep1Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    private RectangularShape ep2Shape = new tufts.vue.shape.Triangle2D(0,0, ArrowBase,ArrowBase*1.3);
    
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
        computeLineShape();

        setTextColor(Color.red); // todo: TEMPORARY ANGLE DEBUG
    }

    protected void removeFromModel()
    {
        super.removeFromModel();
        ep1.removeLinkRef(this);
        ep2.removeLinkRef(this);
    }

    /** Is this link between a parent and a child? */
    public boolean isParentChildLink()
    {
        return ep1.getParent() == ep2 || ep2.getParent() == ep1;
    }
    
    public Line2D getLine(){
        return this.line;
    }
    
    private final int MaxZoom = 1; //todo: get from Zoom code
    private final float SmallestScaleableStrokeWidth = 1 / MaxZoom;
    public boolean intersects(Rectangle2D rect)
    {
        // todo: handle StrokeBug05
        float w = getStrokeWidth();
        if (true || w <= SmallestScaleableStrokeWidth) {
            if (rect.intersectsLine(this.line))
                return true;
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
        if (VueUtil.StrokeBug05) {
            x -= 0.5f;
            y -= 0.5f;
        }
        float maxDist = getStrokeWidth() / 2;
        if (line.ptSegDistSq(x, y) <= (maxDist * maxDist) + 1)
            return true;
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
        if (this.ep2 != null)
            computeLineShape();
        //System.out.println(this + " ep1 = " + c);
    }
    void setEndPoint2(LWComponent c)
    {
        if (c == null) throw new IllegalArgumentException(this + " attempt to set endPoint2 to null");
        this.ep2 = c;
        //if (c == null) System.err.println(this + " endPointd2 set to null");
        //else
        c.addLinkRef(this);
        if (this.ep1 != null)
            computeLineShape();
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
    
    /** a links location is always a computed value -- ignore setLocation calls */
    public void setLocation(float x, float y) {}

    
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

    public Shape getShape()
    {
        return this.line;
        // return stroked shape?
    }

    /*
     * Compute the intersection point of two lines, as defined
     * by two given points for each line.
     * This already assumes that we know they intersect somewhere (are not parallel), 
     */
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

        return new float[] { x, y };
    }

    private static final String[] SegTypes = { "MOVEto", "LINEto", "QUADto", "CUBICto", "CLOSE" };
    
    private static float[] computeShapeIntersection(Shape shape,
                                                    float rayX1, float rayY1,
                                                    float rayX2, float rayY2)
    {
        PathIterator i = shape.getPathIterator(null);
        // todo: only need to flatten if contains curves...
        // todo: if keep flattener, can ignore CUBICTO code below
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
            if (segType == PathIterator.SEG_CUBICTO) {
                endX = seg[4];
                endY = seg[5];
            } else {
                endX = seg[0];
                endY = seg[1];
            }
            if (cnt > 0) {
                if (Line2D.linesIntersect(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1])) {
                    //System.out.println("intersection at segment #" + cnt + " " + SegTypes[segType]);
                    return computeLineIntersection(rayX1, rayY1, rayX2, rayY2, lastX, lastY, seg[0], seg[1]);
                    //return new float[] { (lastX + endX) / 2, (lastY + endY) / 2 };
                }
            }
            cnt++;
            lastX = endX;
            lastY = endY;
            i.next();
        }
        return new float[] { Float.NaN, Float.NaN };
    }

    void computeLineShape()
    {
        float startX = ep1.getCenterX();
        float startY = ep1.getCenterY();
        float endX = ep2.getCenterX();
        float endY = ep2.getCenterY();

        /*
          // a different way of computing connection
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

        Shape ep1Shape = ep1.getShape();
        if (ep1Shape != null && !(ep1Shape instanceof Line2D)) {
            float[]intersection = computeShapeIntersection(ep1Shape, startX, startY, endX, endY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0]))
                startX = intersection[0];
            if (!Float.isNaN(intersection[1]))
                startY = intersection[1];
        }
        Shape ep2Shape = ep2.getShape();
        if (ep2Shape != null && !(ep2Shape instanceof Line2D)) {
            float[]intersection = computeShapeIntersection(ep2Shape, startX, startY, endX, endY);
            // If intersection fails for any reason, leave endpoint as center
            // of object.
            if (!Float.isNaN(intersection[0]))
                endX = intersection[0];
            if (!Float.isNaN(intersection[1]))
                endY = intersection[1];
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

        setSize(Math.abs(startX - endX),
                Math.abs(startY - endY));
        setX(this.centerX - getWidth()/2);
        setY(this.centerY - getHeight()/2);
        
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
    }

    private void drawArrows(Graphics2D gg)
    {
        //-------------------------------------------------------
        // Draw arrows
        //-------------------------------------------------------

        ////ep1Shape.setFrame(this.line.getP1(), new Dimension(arrowSize, arrowSize));
        ////ep1Shape.setFrame(this.line.getX1() - arrowSize/2, this.line.getY1(), arrowSize, arrowSize*2);
        //ep1Shape.setFrame(0,0, arrowSize, arrowSize*2);

        double xdiff = line.getX1() - line.getX2();
        double ydiff = line.getY1() - line.getY2();
        double slope = xdiff / ydiff;
        double slopeInv = 1 / slope;
        double r0 = -Math.atan(slope);
        double deg = Math.toDegrees(r0);
        if (xdiff >= 0 && ydiff >= 0)
            deg += 180;
        else if (xdiff <= 0 && ydiff >= 0)
            deg = -90 - (90-deg);

        if (VueUtil.isMacPlatform()) {
            final int ew = 8; // error-window: # of degrees around 45 that are fucked up
            // Mac MRJ 69.1 / Java 1.4.1 java bug: approaching 45 & 225 degrees,
            // rotations seriously fuck up.
            if (deg > 45-ew && deg < 45+ew)
                deg = 45;
            if (deg > -135-ew && deg < -135+ew)
                deg = -135;
        }
        double r = Math.toRadians(deg);

        AffineTransform savedTransform = gg.getTransform();
        
        gg.setColor(this.strokeColor);
        gg.setStroke(this.stroke);
        
        // draw the first arrow
        gg.translate(line.getX1(), line.getY1());
        gg.rotate(r);
        gg.translate(-ArrowBase/2, 0);
        //gg.setColor(Color.red);
        gg.fill(ep1Shape);
        gg.setColor(this.strokeColor);
        gg.draw(ep1Shape);

        gg.setTransform(savedTransform);

        // draw the second arrow
        gg.translate(line.getX2(), line.getY2());
        gg.rotate(r + Math.PI); // rotate 180 degrees for other arrow
        gg.translate(-ArrowBase/2, 0);
        //gg.setColor(Color.red);
        gg.fill(ep2Shape);
        gg.setColor(this.strokeColor);
        gg.draw(ep2Shape);

        gg.setTransform(savedTransform);


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

    
    public void draw(Graphics2D g)
    {
        computeLineShape(); // compute this.line

        // Clip the node shape so the link doesn't draw into it.
        // We need to do this instead of just drawing links first
        // because SOME links need to be on top -- links to child links,
        // for instance, or maybe just a link you want on the top layer.
        // todo: this works, but it may be a big performance hit,
        // and it doesn't solve the problem of knowing the true
        // visible link length so we can properly center the label
        // todo: this will eventually be replace by links knowing
        // their exact endpoint at edge of the shape of each node --
        // we need to compute the intersection of a shape and a line segment

        //if ((ep1.getShape() != null && !ep1.isChild())
        //|| (ep2.getShape() != null && !ep2.isChild())) {
        //if (ep1.getShape() != null || ep2.getShape() != null) {

        // todo!!: FYI -- doing this clip stuff is what's responsible
        // for the "edge-of-clip-region" erasing bug!  So that's
        // another thing to look forward to when we don't need
        // this code anymore.
        
        // temporarily disable while we work on arrows
        if (false&&!viewerCreationLink) {
            if (!(ep1 instanceof LWLink && ep2 instanceof LWLink)
                && !(ep1.getShape() == null && ep2.getShape() == null)) {
                Area clipArea = new Area(g.getClipBounds());
                if (!(ep1 instanceof LWLink) && ep1.getShape() != null)
                    clipArea.subtract(new Area(ep1.getShape()));
                if (!(ep2 instanceof LWLink) && ep2.getShape() != null)
                    clipArea.subtract(new Area(ep2.getShape()));
                g.clip(clipArea);
            }
        }

        
        /*
        // temporary: draw hit box
        // todo: make a handle?
        g.setColor(Color.lightGray);
        g.setStroke(STROKE_ONE);
        //g.drawRect((int)getX(), (int)getY(), (int)getWidth(), (int)getHeight());
        if (VueUtil.StrokeBug05)
            box.setRect(getX()-0.5, getY()-0.5, getWidth(), getHeight());
        else
            box.setRect(getX(), getY(), getWidth(), getHeight());
        g.draw(box);
        */

        //
        //strokeWidth = getWeight() * WEIGHT_RENDER_RATIO;
        //if (strokeWidth > MAX_RENDER_WIDTH)
        //    strokeWidth = MAX_RENDER_WIDTH;
        

        BasicStroke stroke;

        // If either end of this link is scaled, scale stroke
        // to smallest of the scales (even better: render the stroke
        // in a variable width narrowing as it went...)
        // todo: cache this scaled stroke
        if (ep1.getScale() != 1f || ep2.getScale() != 1f) {
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
    
        //-------------------------------------------------------
        // If selected or indicated, draw a standout stroke
        // bigger than the actual stroke first.
        //-------------------------------------------------------
        /*
        if (isIndicated() || isSelected()) {
            if (isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(COLOR_INDICATION);
            g.setStroke(new BasicStroke(stroke.getLineWidth() + 2));
            g.draw(this.line);
        }
        */
        
        //-------------------------------------------------------
        // Draw the stroke
        //-------------------------------------------------------
        if (isSelected())
            g.setColor(COLOR_SELECTION);
        else if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else
            g.setColor(getStrokeColor());

        g.setStroke(stroke);
        g.draw(this.line);

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
                // todo: only need to do above set location when computing line
                // or text changes somehow (content, font) or alignment changes
                g.translate(lx, ly);
                textBox.draw(g);
                g.translate(-lx, -ly);
            }

            /*
            g.setColor(getTextColor());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            float w = fm.stringWidth(label);
            g.drawString(label, centerX - w/2, centerY - (strokeWidth/2));
            */
        }
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
