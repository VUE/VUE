package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

import javax.swing.text.*;

import java.util.ArrayList;

/**
 * LWNode.java
 *
 * Draws a view of a Node on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
public class LWNode extends LWContainer
    implements Node, ClickHandler
{
    //------------------------------------------------------------------
    // Constants affecting the internal layout of nodes & any children
    //------------------------------------------------------------------
    private static final int PadX = 12;
    private static final int PadY = 6;
    private static final int VerticalChildGap = 2; // vertical space between children
    private static final float ChildScale = 0.75f;   // % scale-down of children
    
    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled, used for computing hits
    protected NodeShape nodeShape;
    protected boolean equalAspect = false;
    //todo: probably collapse off of the above into NodeShape
    
    private ImageIcon imageIcon = null;
    private boolean autoSized = true; // compute size from label & children

    private float fontHeight;
    private float fontStringWidth;
    private float borderWidth = 2; // what is this really?

    private RectangularShape genIcon = new RoundRectangle2D.Float(0,0, 20,15, 10,10);
    
    public LWNode(String label)
    {
        this(label, 0, 0);
    }

    public LWNode(String label, RectangularShape shape)
    {
        this(label, 0, 0, shape);
    }

    /*
    public LWNode(String label, String shapeName, float x, float y)
    {
        super.label = label; // todo: this for debugging
        setFillColor(COLOR_NODE_DEFAULT);
        setNodeShape(getNamedNodeShape(shapeName));
        setStrokeWidth(2f);//todo config: default node stroke
        setLocation(x, y);
        //if (getAbsoluteWidth() < 10 || getAbsoluteHeight() < 10)
        setSize(10,10);
        setLabel(label);
    }
    */
    // internal convenience
    LWNode(String label, float x, float y)
    {
        this(label, x, y, null);
    }

    LWNode(String label, float x, float y, RectangularShape shape)
    {
        super.label = label; // todo: this for debugging
        setFillColor(COLOR_NODE_DEFAULT);
        if (shape == null)
            setNodeShape(StandardShapes[4]);
        else
            setShape(shape);
        setStrokeWidth(2f);//todo config: default node stroke
        setLocation(x, y);
        //if (getAbsoluteWidth() < 10 || getAbsoluteHeight() < 10)
        setSize(10,10);
        setLabel(label);
    }
    
    // internal convenience
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }
    // internal convenience todo: remove -- uses old shape impl
    LWNode(String label, int shapeType)
    {
        this(label);
        setNodeShape(StandardShapes[shapeType]);
    }

    // create a duplicate style
    public LWComponent duplicate()
    {
        LWNode newNode = (LWNode) super.duplicate();
        newNode.autoSized = this.autoSized;
        // TODO: do this as a class and we don't have to keep handling the newInstance everywhere we setNodeShape
        if (getShape() != null)
            newNode.setShape((RectangularShape)((RectangularShape)getShape()).clone());
        else if (getNodeShape() != null) // todo: for backward compat only 
            newNode.setNodeShape(getNodeShape());
        return newNode;
    }
    
    /** for save/restore only */
    public LWNode()
    {
        setNodeShape(StandardShapes[3]);
        setAutoSized(false);
        //todo: remove this setShape eventually (or change to plain rectangle)
        // this is only here for temporary backward compat
        // with saved map files that have no shape information
    }
    
    // Enable this to use differently shaped generated icons
    // depending on if the resource is local or not
    public void X_setResource(Resource resource)
    {
        if (resource != null) {
            if (resource.isLocalFile())
                genIcon = new Rectangle2D.Float(0,0, 20,15);
            else
                genIcon = new RoundRectangle2D.Float(0,0, 20,15, 10,10);
        }
        // need to call this last because it calls layout, which checks getResource
        // and references genIcon
        super.setResource(resource);
    }

    public boolean handleDoubleClick(Point2D p)
    {
        System.out.println("handleDoubleClick " + p + " " + this);
        if (getResource() != null) {
            if (genIcon.contains(p)) {
                getResource().displayContent();
                return true;
            }
        }
        return false;
    }

    // todo: remove this eventually
    static LWNode createTextNode(String text)
    {
        LWNode node = new LWNode(text);
        //node.setNodeShape(StandardShapes[3]);
        node.setShape(new java.awt.geom.Rectangle2D.Float());
        node.setStrokeWidth(0f);
        node.setFillColor(COLOR_TRANSPARENT);
        return node;
    }
    
    public void setIcon(javax.swing.ImageIcon icon) {}
    public javax.swing.ImageIcon getIcon() { return null; }
    
    /** If true, compute node size from label & children */
    public boolean isAutoSized()
    {
        return this.autoSized;
    }
    public boolean setAutoSized(boolean tv)
    {
        return this.autoSized = tv;
    }

    /** for persistance */
    public NodeShape getNodeShape()
    {
        //return this.nodeShape;
        return null;
    }
    public void setNodeShape(NodeShape nodeShape)
    {
        this.nodeShape = nodeShape;
        this.equalAspect = nodeShape.equalAspect;
        setShape(nodeShape.getShapeInstance());
        // todo: getShapeInstance is redundant during restores
    }

    /**
     * @param shape a new instance of a shape for us to use
     */
    public void setShape(RectangularShape shape)
    {
        //System.out.println("SETSHAPE " + shape + " in " + this);
        //System.out.println("SETSHAPE bounds " + shape.getBounds());
        //if (shape instanceof RoundRectangle2D.Float) {
        //RoundRectangle2D.Float rr = (RoundRectangle2D.Float) shape;
        //    System.out.println("RR arcs " + rr.getArcWidth() +"," + rr.getArcHeight());
        //}
        this.boundsShape = shape;
        this.drawnShape = (RectangularShape) shape.clone();
        adjustDrawnShape();
        layout();
        //this.lastLabel = null;
        // this will cause size to be computed at the next rendering
    }

    public Shape getShape()
    {
        return this.boundsShape;
    }

    /*
    public Rectangle2D getBounds()
    {
        Rectangle2D b = this.boundsShape.getBounds2D();
        double sw = getStrokeWidth();
        if (sw > 0) {
            double adj = sw / 2;
            b.setRect(b.getX()-adj, b.getY()-adj, b.getWidth()+sw, b.getHeight()+sw);
        }
        return b;
        //return this.boundsShape.getBounds2D();
    }
    */

    public boolean intersects(Rectangle2D rect)
    {
        // If we've never been painted, graphics won't be set.
        // This is so that LWContainer repaint optimization
        // will be sure to paint us at least once so we can
        // compute our bounds based on our size (assuming we're
        // auto-sized).  todo: find a way to do this cleaner
        //if (this.graphics == null)
         //   return true;
        return boundsShape.intersects(rect);
        //return getBounds().intersects(rect);
    }

    public boolean contains(float x, float y)
    {
        if (imageIcon != null)
            return super.contains(x,y);

        return boundsShape.contains(x, y);
    }
    
    void setImage(Image image)
    {
        // experimental
        imageIcon = new ImageIcon(image, "Image Description");
        setAutoSized(false);
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public void addChild(LWComponent c)
    {
        super.addChild(c);
        setScale(getScale());// todo: only to propagate color toggle hack
        layout();
    }

    public void setSize(float w, float h)
    {
        if (DEBUG_LAYOUT) System.out.println("*** " + this + " setSize " + w + "x" + h);
        setSizeNoLayout(w, h);
        layout();
    }

    private void setSizeNoLayout(float w, float h)
    {
        if (DEBUG_LAYOUT) System.out.println("*** " + this + " setSizeNoLayout " + w + "x" + h);
        if (equalAspect) {
            if (w > h)
                h = w;
            else
                w = h;
        }
        super.setSize(w, h);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
        adjustDrawnShape();
    }

    void setScale(float scale)
    {
        super.setScale(scale);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
    }
    void setScaleOnChild(float scale, LWComponent c)
    {
        // todo: temporary hack color change for children
        if (c.isManagedColor()) {
            if (COLOR_NODE_DEFAULT.equals(getFillColor()))
                c.setFillColor(COLOR_NODE_INVERTED);
            else
                c.setFillColor(COLOR_NODE_DEFAULT);
        }
        c.setScale(scale * ChildScale);
    }
    
    private void adjustDrawnShape()
    {
        if (DEBUG_LAYOUT) System.out.println(this + " adjustDrawnShape " + getAbsoluteWidth() + "x" + getAbsoluteHeight());
        this.drawnShape.setFrame(0, 0, getAbsoluteWidth(), getAbsoluteHeight());
    }
    
    private void X_adjustDrawnShape()
    {
        // shrink the drawn shape size by border width
        // so it fits entirely inside the bounds shape.
        
        //double x = boundsShape.getX() + borderWidth / 2.0;
        //double y = boundsShape.getY() + borderWidth / 2.0;
        double x = 0;
        double y = 0;
        if (VueUtil.StrokeBug05) {
            // note that boundsShape.getBounds() is different than on PC
            x += 0.5;
            y += 0.5;
        }
        double w = boundsShape.getWidth() - borderWidth;
        double h = boundsShape.getHeight() - borderWidth;
        //System.out.println("boundsShape.bounds: " + boundsShape.getBounds());
        //System.out.println("drawnShape.setFrame " + x + "," + y + " " + w + "x" + h);
        this.drawnShape.setFrame(x, y, w, h);
    }
    

    public void setLocation(float x, float y)
    {
        //System.out.println("setLocation " + this);
        super.setLocation(x, y);
        //getNode().setPosition(x, y);
        this.boundsShape.setFrame(x, y, getWidth(), getHeight());
        //this.boundsShape.setFrame(x, y, this.width, this.height);
        adjustDrawnShape();
        layoutChildren();
        // todo BUG: if parent and some child both in selection and you
        // drag, the selection and the parent fight to control the location
        // of the child.
    }
    
    protected void layout()
    {
        if (DEBUG_LAYOUT) System.out.println("*** LAYOUT " + this);
        if (isAutoSized() || hasChildren())
            setPreferredSize(!isAutoSized());
        layoutChildren();
        
        // could set size from label first, then layout children and
        // have it return child bounds, and set size again based on
        // that if bigger so don't have to reproduce layout logic in
        // both getAllChildren bounds and layoutChildren

        // todo: handle thru event?
        if (getParent() != null) // parent could be null constructing coded demo maps
            getParent().layout();
    }
      
    
    private void setPreferredSize(boolean growOnly)
    {
        Dimension s = getLabelBox().getPreferredSize();
        float width = s.width + PadX;
        float height = s.height + PadY;
        
        if (hasChildren()) {
            // resize to inclued size of children
            height += PadY;
            Rectangle2D childBounds = getAllChildrenBounds();
            height += childBounds.getHeight();
            if (width < childBounds.getWidth() + PadX*2)
                width = (float) childBounds.getWidth() + PadX*2;
        }
        if (getResource() != null)
            width += PadX*1.5 + genIcon.getWidth();

        if (growOnly) {
            if (this.width > width)
                width = this.width;
            if (this.height > height)
                height = this.height;
            if (width > this.width || height > this.height)
                setSizeNoLayout(width, height);
        } else
            setSizeNoLayout(width, height);
    }

    /*        if (this.width != oldWidth && lastLabel != null &&
            !(getParent() instanceof LWNode)) // todo: this last test really depends on if parent is laying us out
        {
            // on resize, keep the node's center the same
            setLocation(getX() + (oldWidth - this.width) / 2, getY());
            }*/

    private Rectangle2D getAllChildrenBounds()
    {
        // compute bounds based on a vertical stacking layout
        java.util.Iterator i = getChildIterator();
        float height = 0;
        float maxWidth = 0;
        float width;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            height += c.getBoundsHeight() + VerticalChildGap;
            width = c.getBoundsWidth();
            //height += c.height + VerticalChildGap;
            //width = c.width;
            if (width > maxWidth)
                maxWidth = width;
            
        }
        // If WE'RE already scaled, these totals will be off
        // This is way confusing -- I hope we can
        // can get rid of this feature soon.
        height /= getScale();
        maxWidth /= getScale();
        return new Rectangle2D.Float(0f, 0f, maxWidth, height);
    }
    
    // todo: okay, we do NOT want to do this every damn paint --
    // makes it impossible to drag out a child!
    protected void layoutChildren()
    {
        if (!hasChildren())
            return;
        //new Throwable("layoutChildren " + this).printStackTrace();
        //System.out.println("layoutChildren " + this);
        java.util.Iterator i = getChildIterator();
        //float y = (relativeLabelY() + PadY) * getScale();
        // relaveLabelY used to be the BASELINE for the text -- now it's the UL of the label object
        float y = (relativeLabelY() + getLabelBox().getHeight() + PadY/2) * getScale();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            //float childX = PadX * getScale();
            float childX = (this.getWidth() - c.getWidth()) / 2;
            c.setLocation(getX() + childX, getY() + y);
            y += c.getBoundsHeight();
            y += VerticalChildGap * getScale();
        }
    }

    public float getLabelX()
    {
        return getX() + relativeLabelX();
    }
    public float getLabelY()
    {
        return getY() + relativeLabelY();
        /*
        if (this.labelBox == null)
            return getY() + relativeLabelY();
        else
            return (getY() + relativeLabelY()) - this.labelBox.getHeight();
        */
        //return (getY() + relativeLabelY()) - this.fontHeight;
    }

    private float relativeLabelX()
    {
        float offset;
        if (getResource() != null) {
            offset = (float) (PadX*1.5 + genIcon.getWidth());
        } else {
            int w = getLabelBox().getPreferredSize().width;
            offset = (this.width - w) / 2;
        }
        return offset;
    }
    private float relativeLabelY()
    {
        if (hasChildren())
            return (PadY/2) * getScale();
        else
            return (this.height - getLabelBox().getPreferredSize().height) / 2;
        //return (this.height - getLabelBox().getHeight()) / 2;
        /*
        if (hasChildren())
            return this.fontHeight + PadY * getScale();
        else
            return (this.height+this.fontHeight) / 2f;
        */
    }

    public void draw(Graphics2D g)
    {
        //this.graphics = g;
        
        g.translate(getX(), getY());
        float scale = getScale();
        if (scale != 1f)
            g.scale(scale, scale);
            
        String label = getLabel();

        // System.out.println("draw " + label);

        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
        if (imageIcon != null) {
            // experimental
            //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
            imageIcon.paintIcon(null, g, 0, 0);
        } else {
            /*
            if (label != lastLabel) {
                //System.out.println("label " + lastLabel + " -> " + label);
                layout();
                lastLabel = label;
                }*/
            Color fillColor = getFillColor();
            if (fillColor != null) { // transparent if null
                g.setColor(fillColor);
                g.fill(drawnShape);
            }
        }

        //-------------------------------------------------------
        // Draw the indicated border if any
        //-------------------------------------------------------
        // todo perf: factor out these conditionals
        if (isIndicated()) {
            g.setColor(COLOR_INDICATION);
            if (STROKE_INDICATION.getLineWidth() > getStrokeWidth())
                g.setStroke(STROKE_INDICATION);
            else
                g.setStroke(this.stroke);
            g.draw(drawnShape);
        } else if (getStrokeWidth() > 0) {
            //if (LWSelection.DEBUG_SELECTION && isSelected())
            if (isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(getStrokeColor());
            g.setStroke(this.stroke);
            g.draw(drawnShape);
        //} else if (isSelected() && getFillColor() == null) {
            // If stroke is zero & there's no fill color then there's
            // no way to see what the shape is. So if we're selected,
            // we draw a faint one anyway so you can see what shape
            // this object is. [ now handled in mapviewer with ghost I images ]
            //g.setStroke(STROKE_ONE);
            //g.setColor(COLOR_SELECTION);
            //g.draw(drawnShape);
        }
        // todo: would be nice if this shape has no border and isn't rectangular
        // (e.g., a circle or a triangle) to draw a selection border around it's
        // actual border when selected so you can see the shape if it happens to
        // be transparent or on a background of the same color

        //-------------------------------------------------------
        // Draw the text label if any
        //-------------------------------------------------------
        
        /*
        if (label != null && label.length() > 0) {
            float textBaseline = relativeLabelY();
            g.setFont(getFont());
            g.setColor(getTextColor());
            g.drawString(label, relativeLabelX(), textBaseline);
        }
        */


        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        // OKAY, create that DrawContext, which can have
        // the zoom factor in there (as well as handle anti-alias,
        // and allow for paint requests back to the parent)

        // Here we'll check the zoom level, and if iit's say,
        // over 800%, we could draw the resource string in a tiny
        // font right in the icon.
        

        if (getResource() != null) {
            //double iy = relativeLabelY() + genIcon.getHeight() / 2;
            float iconHeight = (float) genIcon.getHeight();
            float iconWidth = (float) genIcon.getWidth();
            double iy = (getHeight() - iconHeight) / 2;
            double ix = PadX;
            genIcon.setFrame(ix, iy, genIcon.getWidth(), genIcon.getHeight());
            g.setColor(Color.white);
            g.fill(genIcon);
            g.setStroke(STROKE_HALF);
            g.setColor(Color.black);
            g.draw(genIcon);
            g.setFont(FONT_ICON);
            String extension = getResource().getExtension();
            TextLayout row = new TextLayout(extension, g.getFont(), g.getFontRenderContext());            
            //g.drawString(extension, 0, (int)(genIcon.getHeight()/1.66));
            Rectangle2D.Float tb = (Rectangle2D.Float) row.getBounds();

            if (DEBUG_LAYOUT) System.out.println("[" + extension + "] bounds="+tb);

            // Mac & PC 1.4.1 implementations haved reversed baselines
            // and differ in how descents are factored into bounds offsets
            g.translate(ix, iy);
            float xoff = (iconWidth - tb.width) / 2;
            float yoff = (iconHeight - tb.height) / 2;
            if (VueUtil.isMacPlatform()) {
                yoff += tb.height;
                yoff += tb.y;
                xoff += tb.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
                row.draw(g, xoff, yoff);

                if (DEBUG_LAYOUT) {
                    // draw a red bounding box for testing
                    tb.x += xoff;
                    tb.y = -tb.y; // if any descent below baseline, will be non-zero
                    tb.y += yoff;
                    tb.y -= tb.height;
                    g.setStroke(new java.awt.BasicStroke(0.1f));
                    g.setColor(Color.red);
                    g.draw(tb);
                }
                
            } else {
                // This is cleaner, thus I'm assuming the PC
                // implementation is also cleaner, and worthy of being
                // the default case.
                
                row.draw(g, -tb.x + xoff, -tb.y + yoff);

                if (DEBUG_LAYOUT) {
                    // draw a red bounding box for testing
                    tb.x = xoff;
                    tb.y = yoff;
                    g.setStroke(new java.awt.BasicStroke(0.1f));
                    g.setColor(Color.red);
                    g.draw(tb);
                }
                
            }
                

            
                
            /*
            ix += tb.x;
            iy += tb.y + tb.height;
            g.translate(tb.x, tb.y + tb.height);
            // get's actual shape outline of letters, but spacing isn't right
            Shape outline = row.getOutline(null);
            g.setColor(Color.green);
            g.draw(outline);
            */

            g.translate(-ix, -iy);
        }

        // todo: create drawLabel, drawBorder & drawBody
        // LWComponent methods so can automatically turn
        // this off in MapViewer, adjust stroke color for
        // selection, etc.
        if (this.labelBox != null && this.labelBox.getParent() == null) {
            // if parent is not null, this box is an active edit on the map
            // and we don't want to paint it here as AWT/Swing is handling
            // that at the moment (and at a possibly slightly different offset)
            float lx = relativeLabelX();
            float ly = relativeLabelY();
            g.translate(lx, ly);
            this.labelBox.draw(g);
            g.translate(-lx, -ly);
        }
        
        //-------------------------------------------------------
        // Restore graphics context
        //-------------------------------------------------------
        if (scale != 1f)
            g.scale(1/scale, 1/scale);
        g.translate(-getX(), -getY());

        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------
        super.draw(g);

    }

    public static class NodeShape {
        String name;
        RectangularShape shape;
        boolean equalAspect;

        private NodeShape(String name, RectangularShape shape, boolean equalAspect)
        {
            this.name = name;
            this.shape = shape;
            this.equalAspect = equalAspect;
        }
        private NodeShape(String name, RectangularShape shape)
        {
            this(name, shape, false);
        }

        /** for XML persistance */
        public NodeShape() {}

        public RectangularShape getShape()
        {
            return shape;
        }
        /** for XML persistance */
        public void setShape(RectangularShape s)
        {
            shape = s;
        }
        /** for XML persistance */
        public boolean isEqualAspect()
        {
            return equalAspect;
        }
        /** for XML persistance */
        public void setEqualAspect(boolean tv)
        {
            equalAspect = tv;
        }
        RectangularShape getShapeInstance()
        {
            return (RectangularShape) shape.clone();
        }
    }

    // load these from some kind of resource definition?
    static final NodeShape StandardShapes[] = {
        //new NodeShape("Oval", new RoundRectangle2D.Float(0,0, 0,0, 180,180)),
        new NodeShape("Oval", new Ellipse2D.Float(0,0,10,10)),
        // todo: convert square & circle do ellipse & rectangle and
        // then set a "locked aspect ratio" bit on the LWComponent,
        // that setSize can attend to (and then LWComponent can
        // store an aspect ration, which will get initialized to 1 in this case).
        new NodeShape("Circle", new Ellipse2D.Float(0,0,10,10), true),
        new NodeShape("Square", new Rectangle2D.Float(0,0,10,10), true),
        new NodeShape("Rectangle", new Rectangle2D.Float(0,0,10,10)),
        new NodeShape("Rounded Rectangle", new RoundRectangle2D.Float(0,0, 10,10, 20,20)),
        //new NodeShape("Triangle", new tufts.vue.shape.Triangle2D(0,0, 60,120)),
        //new NodeShape("Diamond", new tufts.vue.shape.Diamond2D(0,0, 60,60)),
        new NodeShape("Triangle", new tufts.vue.shape.RectangularPoly2D(3, 0,0, 60,120)),
        new NodeShape("Diamond", new tufts.vue.shape.RectangularPoly2D(4, 0,0, 120,120)),
        new NodeShape("Hexagon", new tufts.vue.shape.RectangularPoly2D(5, 0,0, 120,120)),
        new NodeShape("Pentagon", new tufts.vue.shape.RectangularPoly2D(6, 0,0, 120,120)),
        new NodeShape("Octagon", new tufts.vue.shape.RectangularPoly2D(8, 0,0, 120,120)),

        // Polygon class not a RectangularShape...
        //new NodeShape("Poly3", new Polygon(new int[] {0, 10, 20}, new int[] {0, 20, 0}, 3)),
        //new NodeShape("Parallelogram", null),
    };
    
    
}

