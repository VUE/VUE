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
import java.awt.geom.*;
import java.awt.font.TextLayout;

import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.util.ArrayList;
import java.util.Iterator;

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
    implements Node
{
    public static final Font  DEFAULT_NODE_FONT = VueResources.getFont("node.font");
    public static final Color DEFAULT_NODE_FILL = VueResources.getColor("node.fillColor");
    public static final int   DEFAULT_NODE_STROKE_WIDTH = VueResources.getInt("node.strokeWidth");
    public static final Color DEFAULT_NODE_STROKE_COLOR = VueResources.getColor("node.strokeColor");
    public static final Font  DEFAULT_TEXT_FONT = VueResources.getFont("text.font");
    
    //------------------------------------------------------------------
    // Constants affecting the internal layout of nodes & any children
    //------------------------------------------------------------------
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    private static final boolean AlwaysShowIcon = false;
        
    private static final int EdgePadY = 3;
    private static final int PadTop = EdgePadY;

    private static final int IconGutterWidth = 26;

    private static final int IconPadLeft = 2;
    private static final int IconPadRight = 0;
    private static final int IconWidth = IconGutterWidth - IconPadLeft; // 22 is min width that will fit "www" in our icon font
    private static final int IconHeight = 12;
    
    //private static final int IconPadRight = 4;
    private static final int IconMargin = IconPadLeft + IconWidth + IconPadRight;
    /** this is the descent of the closed icon down below the divider line */
    private static final float IconDescent = IconHeight / 3f;
    /** this is the rise of the closed icon above the divider line */
    private static final float IconAscent = IconHeight - IconDescent;
    private static final int IconPadBottom = (int) IconAscent;
    private static final int IconMinY = IconPadLeft;

    private static final int LabelPadLeft = 6; // distance to right of iconMargin dividerLine
    private static final int LabelPadX = LabelPadLeft;
    private static final int LabelPadY = EdgePadY;
    private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadLeft;

    // TODO: need to multiply all these by ChildScale (huh?)
    
    private static final int ChildOffsetX = IconMargin + LabelPadLeft; // X offset of children when icon showing
    private static final int ChildOffsetY = 4; // how far children down from bottom of label divider line
    private static final int ChildPadY = ChildOffsetY;
    private static final int ChildPadX = 5; // min space at left/right of children
    private static final int ChildVerticalGap = 3; // vertical space between children
    private static final int ChildHorizontalGap = 3; // horizontal space between children
    private static final int ChildrenPadBottom = ChildPadX - ChildVerticalGap; // make same as space at right
    //    private static final int ChildrenPadBottom = 3; // space at bottom after all children
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case, which we're guessing at here
    // based on a small set of random test cases.
    private static final float TextWidthFudgeFactor = 1 + 0.1f; // 10% fudge
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.
    //private static final float TextWidthFudgeFactor = 1;
    //private static final int DividerStubPadX = TextWidthFudgeAmount;

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    

    
    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled, used for computing hits
    //protected NodeShape nodeShape;
    //protected boolean equalAspect = false;
    //todo: could collapse all of the above into NodeShape if we want to resurrect it
    
    private ImageIcon imageIcon = null;
    private boolean autoSized = true; // compute size from label & children

    //private RectangularShape genIcon = new RoundRectangle2D.Float(0,0, IconWidth,IconHeight, 12,12);
    //private RectangularShape genIcon = new Rectangle2D.Float(0,0, IconWidth,IconHeight);
    private Line2D dividerUnderline = new Line2D.Float();
    private Line2D.Float dividerMarginLine = new Line2D.Float();
    private Line2D dividerStub = new Line2D.Float();

    private transient boolean mIsRectShape = true;
    //private transient boolean mIsTextNode = false; // todo: are we saving this in XML???

    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL,
                         LWIcon.Block.COORDINATES_COMPONENT);

    private transient Point2D.Float mLabel = new Point2D.Float();
    
    public LWNode(String label)
    {
        this(label, 0, 0);
    }

    public LWNode(String label, RectangularShape shape)
    {
        this(label, 0, 0, shape);
    }

    public boolean supportsUserLabel() {
        return true;
    }
    public boolean supportsUserResize() {
        return true;
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
        setFillColor(DEFAULT_NODE_FILL);
        if (shape == null)
            //setNodeShape(StandardShapes[4]);
            setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20));
        else
            setShape(shape);
        setStrokeWidth(DEFAULT_NODE_STROKE_WIDTH);
        setStrokeColor(DEFAULT_NODE_STROKE_COLOR);
        setLocation(x, y);
        //if (getAbsoluteWidth() < 10 || getAbsoluteHeight() < 10)
        setSize(10,10);
        setLabel(label);
        setFont(DEFAULT_NODE_FONT);
    }
    
    // internal convenience
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }
    // internal convenience todo: remove -- uses old shape impl
    /*
    LWNode(String label, int shapeType)
    {
        this(label);
        setNodeShape(StandardShapes[shapeType]);
    }
    */

    // create a duplicate style
    public LWComponent duplicate()
    {
        LWNode newNode = (LWNode) super.duplicate();
        // TODO: do this as a class and we don't have to keep handling the newInstance everywhere we setNodeShape
        if (getShape() != null)
            newNode.setShape((RectangularShape)((RectangularShape)getShape()).clone());

        newNode.setSize(super.getWidth(), super.getHeight()); // make sure shape get's set with old size
        //else if (getNodeShape() != null) // todo: for backward compat only 
        //newNode.setNodeShape(getNodeShape());
        return newNode;
    }
    
    /** for save/restore only */
    public LWNode()
    {
        setShape(new java.awt.geom.Rectangle2D.Float());
        //setNodeShape(StandardShapes[3]);
        setAutoSized(false);
        //todo: remove this setShape eventually (or change to plain rectangle)
        // this is only here for temporary backward compat
        // with saved map files that have no shape information
    }
    
    // Enable this to use differently shaped generated icons
    // depending on if the resource is local or not
    /*
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
    */
    
    private boolean iconShowing()
    {
        //return AlwaysShowIcon || hasResource() || hasNotes() || hasMetaData() || inPathway();
        return AlwaysShowIcon || mIconBlock.isShowing(); // remember not current till after a layout
    }

    // was text box hit?  coordinates are component local
    private boolean textBoxHit(float cx, float cy)
    {
        float lx = relativeLabelX() - IconPadRight;
        float ly = relativeLabelY() - PadTop;
        float height = getLabelBox().getHeight() + PadTop;
        float width = (IconPadRight + getLabelBox().getWidth()) * TextWidthFudgeFactor;

        return
            cx >= lx &&
            cy >= ly &&
            cx <= lx + width &&
            cy <= ly + height;
    }

    public void setResource(Resource resource)
    {
        super.setResource(resource);
        //ttResource = null;
    }
    public void setNotes(String notes)
    {
        super.setNotes(notes);
        //ttNotes = null;
    }

    public void mouseOver(MapMouseEvent e)
    {
        //if (textBoxHit(cx, cy)) System.out.println("over label");

        if (mIconBlock.isShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }

    public boolean handleDoubleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleDoubleClick " + e + " " + this);

        float cx = e.getComponentX();
        float cy = e.getComponentY();

        if (textBoxHit(cx, cy)) {
            e.getViewer().activateLabelEdit(this);
        } else {
            if (!mIconBlock.handleDoubleClick(e)) {
                // by default, a double-click anywhere else in
                // node opens the resource
                if (hasResource()) {
                    getResource().displayContent();
                    // todo: some kind of animation or something to show
                    // we're "opening" this node -- maybe an indication
                    // flash -- we'll need another thread for that.
                    
                    //mme.getViewer().setIndicated(this); or
                    //mme.getComponent().paintImmediately(mapToScreenRect(getBounds()));
                    //or mme.repaint(this)
                    // now open resource, and then clear indication
                    //clearIndicated();
                    //repaint();
                }
            }
        }
        return true;
    }

    public boolean handleSingleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleSingleClick " + e + " " + this);
        // "handle", but don't actually do anything, if they single click on
        // the icon (to prevent activating a label edit if they click here)
        //return iconShowing() && genIcon.contains(e.getComponentPoint());

        // for now, never activate a label edit on just a single click.
        // --prob better to conifg somehow than to depend on MapViewer side-effects
        return true;
    }

    public void setIcon(javax.swing.ImageIcon icon) {}
    public javax.swing.ImageIcon getIcon() { return null; }
    
    
    public void setIsTextNode(boolean asText) {
        if (asText)
            setFillColor(null);
        else
            setFillColor(getParent().getFillColor());
    	//mIsTextNode = pState;
    }
    
    public boolean isTextNode() {
        // Just what a text node is is a bit confusing right now, but it's useful
        // guess for now.
    	//return (mIsTextNode || (getFillColor() == null && mIsRectShape)) && !hasChildren();
    	return getFillColor() == null && mIsRectShape && !hasChildren();
    }
    
    /** If true, compute node size from label & children */
    public boolean isAutoSized() {
        return this.autoSized;
    }

    /**
     * For explicitly restoring the autoSized bit to true.
     *
     * The autoSize bit is only *cleared* via automatic means: when the
     * node's size is explicitly set to something bigger than that
     * size it would have if it took on it's automatic size.
     *
     * Clearing the autoSize bit on a node manually would have no
     * effect, because as soon as it was next laid out, it would
     * notice it has it's minimum size, and would automatically
     * set the bit.
     */
    
    public void setAutoSized(boolean makeAutoSized)
    {
        if (autoSized == makeAutoSized)
            return;
        if (DEBUG.LAYOUT) out("*** " + this + " setAutoSized " + makeAutoSized);

        // We only need an undo event if going from not-autosized to
        // autosized: i.e.: it wasn't an automatic shift triggered via
        // set size. Because size events aren't delieverd if autosized
        // is on (which would normally notice the size change), we need
        // to remember the old size manually here if turning autosized
        // back on)

        Object old = null;
        if (makeAutoSized)
            old = new Point2D.Float(this.width, this.height);
        this.autoSized = makeAutoSized;
        if (autoSized && !inLayout)
            layout();
        if (makeAutoSized)
            notify("node.autosized", new Undoable(old) {
                    void undo() {
                        Point2D.Float p = (Point2D.Float) old;
                        setSize(p.x, p.y);
                    }});
    }
    
    /**
     * For triggering automatic shifts in the auto-size bit based on a call
     * to setSize or as a result of a layout
     */
    private void setAutomaticAutoSized(boolean tv)
    {
        if (isOrphan()) // if this is during a restore, don't do any automatic auto-size computations
            return;
        if (autoSized == tv)
            return;
        if (DEBUG.LAYOUT) out("*** " + this + " setAutomaticAutoSized " + tv);
        this.autoSized = tv;
    }
    

    private boolean isSameShape(Shape s1, Shape s2) {
        if (s1 == null || s2 == null)
            return false;
        if (s1.getClass() == s2.getClass()) {
            if (s1 instanceof RoundRectangle2D) {
                RoundRectangle2D rr1 = (RoundRectangle2D) s1;
                RoundRectangle2D rr2 = (RoundRectangle2D) s2;
                return
                    rr1.getArcWidth() == rr2.getArcWidth() &&
                    rr1.getArcHeight() == rr2.getArcHeight();
            } else
                return true;
        } else
            return false;
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

        if (isSameShape(this.boundsShape, shape))
            return;

        Object old = this.boundsShape;
        this.mIsRectShape = (shape instanceof Rectangle2D || shape instanceof RoundRectangle2D);
        this.boundsShape = shape;
        this.drawnShape = (RectangularShape) shape.clone();
        adjustDrawnShape();
        layout();
        notify("node.shape", new Undoable(old) { void undo() { setShape((RectangularShape)old); }} );
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
        final float strokeWidth = getStrokeWidth();
        if (strokeWidth > 0 || isSelected()) {
            // todo opt: cache this
            final Rectangle2D.Float r = new Rectangle2D.Float();
            r.setRect(rect);
            
            // this isn't so pretty -- expanding the test rectangle to
            // compensate for the border width, but it works mostly --
            // only a little off on non-rectangular sides of shapes.
            // (todo: sharp points are problem too -- e.g, a flat diamond)
            
            float totalStroke = strokeWidth;
            if (isSelected())
                totalStroke += SelectionStrokeWidth;
            final float adj = totalStroke / 2;
            r.x -= adj;
            r.y -= adj;
            r.width += totalStroke;
            r.height += totalStroke;
            return boundsShape.intersects(r);
        } else
            return boundsShape.intersects(rect);
        
        //return getBounds().intersects(rect);
    }

    public boolean contains(float x, float y)
    {
        if (imageIcon != null)
            return super.contains(x,y);
        else {
            // TODO: util irregular shapes can still give access to children
            // outside their bounds, we're checking everything in the bounding box
            // for the moment if there are any children.
            if (hasChildren())
                return super.contains(x,y);
            else if (mIsRectShape) {
                return boundsShape.contains(x, y);
            } else {
                float cx = x - getX();
                float cy = y - getY();
                // if we end up using these zillion checks, be sure to
                // first surround with a fast-reject bounding-box check
                return boundsShape.contains(x, y)
                    || textBoxHit(cx, cy)
                    ;
                    //|| mIconBlock.contains(cx, cy)
            }
        }
        
        // to compensate for stroke width here, could get mathy here
        // and move the x/y strokeWidth units along a line toward
        // the center of the object, which wouldn't be perfect
        // but would be reasonable.
    }
    
    void setImage(Image image)
    {
        // experimental
        imageIcon = new ImageIcon(image, "Image Description");
        setAutoSized(false);
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public void addChildren(Iterator i)
    {
        super.addChildren(i);
        setScale(getScale()); // make sure children get shrunk
        layout();
    }

    public void setSize(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** " + this + " setSize         " + w + "x" + h);
        if (isAutoSized() && (w > this.width || h > this.height))
            setAutomaticAutoSized(false);
        // todo: FIRST, get size from layout, then if it changes, actually set the size!
        setSizeNoLayout(w, h);
        layout();
    }

    private void setSizeNoLayout(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** " + this + " setSizeNoLayout " + w + "x" + h);
        super.setSize(w, h);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
        adjustDrawnShape();
    }

    void setScale(float scale)
    {
        super.setScale(scale);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
    }
    
    void setScaleOnChild(float scale, LWComponent c) {
        c.setScale(scale * ChildScale);
    }
    
    private void adjustDrawnShape()
    {
        // This was to shrink the drawn shape size by border width
        // so it fits entirely inside the bounds shape, tho
        // we're not making use of that right now.
        if (DEBUG.LAYOUT) out("*** " + this + " adjstDrawnShape " + getAbsoluteWidth() + "x" + getAbsoluteHeight());
        //System.out.println("boundsShape.bounds: " + boundsShape.getBounds());
        //System.out.println("drawnShape.setFrame " + x + "," + y + " " + w + "x" + h);
        this.drawnShape.setFrame(0, 0, getAbsoluteWidth(), getAbsoluteHeight());
    }

    public void setLocation(float x, float y)
    {
        //System.out.println("setLocation " + this);
        super.setLocation(x, y);
        this.boundsShape.setFrame(x, y, getWidth(), getHeight());
        adjustDrawnShape();

        // Must lay-out children seperately from layout() -- if we
        // just call layout here we'll recurse when setting the
        // location of our children as they they try and notify us
        // back that we need to layout.
        
        layoutChildren();
    }
    
    private boolean inLayout = false;
    private boolean isCenterLayout = false;
    protected void layout()
    {
        if (inLayout) {
            new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
            return;
        }
        inLayout = true;
        if (DEBUG.LAYOUT) out("*** " + this + " LAYOUT");

        mIconBlock.layout(); // in order to compute the size & determine if anything showing

        if (DEBUG.LAYOUT && getLabelBox().getHeight() != getLabelBox().getPreferredSize().height) {
            // NOTE: prefHeight often a couple of pixels less than getHeight
            System.err.println("prefHeight != height in " + this);
            System.err.println("\tpref=" + getLabelBox().getPreferredSize().height);
            System.err.println("\treal=" + getLabelBox().getHeight());
        }

        // The current width & height is at this moment still a
        // "request" size -- e.g., the user may have attempted to drag
        // us to a size smaller than our minimum size.  During that
        // operation, the size of the node is momentarily set to
        // whatever the user requests, but then is immediately laid
        // out here, during which we will revert the node size to the
        // it's minimum size if bigger than the requested size.
        
        Size request = new Size(getWidth(), getHeight());
        Size min;
        
        if (mIsRectShape) {
            isCenterLayout = false;
            min = layout_boxed();
        } else {
            isCenterLayout = true;
            min = layout_centered();
        }

        if (DEBUG.LAYOUT) out("*** " + this + " computed=" + min);

        // If the size gets set to less than or equal to
        // minimize size, lock back into auto-sizing.
        if (request.height <= min.height && request.width <= min.width)
            setAutomaticAutoSized(true);
        
        if (!isAutoSized()) {
            // we always compute the minimum size, and
            // never let us get smaller than that -- so
            // only use given size if bigger than min size.
            if (min.height < request.height)
                min.height = request.height;
            if (min.width < request.width)
                min.width = request.width;
        }

        setSizeNoLayout(min.width, min.height);

        dividerMarginLine.setLine(IconMargin, MarginLinePadY, IconMargin, min.height-MarginLinePadY);
        if (!mIsRectShape)
            VueUtil.clipToYCrossings(dividerMarginLine, drawnShape, MarginLinePadY);
    
        if (getParent() != null && !(getParent() instanceof LWMap)) {
            //if (getParent() != null && (givenWidth != getWidth() || givenHeight != getHeight())) {
            getParent().layout();
        }
        
        inLayout = false;
    }

    private Size getTextSize() {
        Size s = new Size(getLabelBox().getPreferredSize());
        s.width *= TextWidthFudgeFactor;
        return s;
    }

    private Point2D.Float labelP = new Point2D.Float();
    private Point2D.Float childP = new Point2D.Float();
    private static final float CenterPad = 5;
    private Size layout_centered()
    {
        Size box = get_minimum_content_size();
        Size node = new Size(box);
        //min.width += CenterPad * 2;
        //min.height += CenterPad * 2;

        node.fitWidth(getWidth());
        node.fitHeight(getHeight());

        float xoff = (node.width - box.width) / 2;
        float yoff = (node.height - box.height) / 2;
        
        Size text = getTextSize();
        labelP.x = xoff + (box.width - text.width) / 2;
        labelP.y = yoff;

        yoff += text.height + LabelPadY;

        if (iconShowing()) {
            float yIconBlock = (node.height - mIconBlock.height) / 2;
            mIconBlock.setLocation(xoff, yIconBlock);
            xoff += mIconBlock.width;
        }

        if (hasChildren()) {
            xoff += ChildPadX;
            yoff += ChildPadY;
            childP.x = xoff;
            childP.y = yoff;
            layoutChildren(xoff, yoff);
        }
        return box;
    }
    
    private Size get_minimum_content_size()
    {
        Size box = new Size();
        Size text = getTextSize();

        //box.width  = LabelPadX + text.width + LabelPadX;
        //box.height = LabelPadY + text.height + LabelPadY;
        box.width  = text.width;
        box.height = text.height + LabelPadY;

        if (iconShowing()) {
            box.width += mIconBlock.width;
            box.fitHeight(mIconBlock.height);
        }

        if (hasChildren()) {
            Size children = layoutChildren(new Size(), true);
            box.fitWidth(ChildPadX + children.width + ChildPadX);
            box.height += ChildOffsetY + children.height + + ChildrenPadBottom;
        }
        
        return box;
    }
    
    private Size layout_boxed()
    {
        final float givenWidth = getWidth();
        final float givenHeight = getHeight();

        Size min = new Size();
        Dimension text = getLabelBox().getPreferredSize(); // may be important to use pref size -- keep for now

        min.width = text.width * TextWidthFudgeFactor; // adjust for scaled fonts understating their width
        min.height = EdgePadY + text.height + EdgePadY;

        //float width = s.width + PadX;
        //float height = s.height + PadY;
        //float height = getLabelBox().getHeight() + IconHeight/3f;
        //float height = getLabelBox().getHeight() + IconDescent;
        
        // *** set icon Y position in all cases to a centered vertical
        // position, but never such that baseline is below bottom of
        // first icon -- this is tricky tho, as first icon can move
        // down a bit to be centered with the label!

        if (!iconShowing()) {
            min.width += LabelPadLeft * 2;
        } else {
            float dividerY = EdgePadY + text.height;
            double stubX = LabelPositionXWhenIconShowing + (text.width * TextWidthFudgeFactor);
            double stubHeight = DividerStubAscent;
            
            //dividerUnderline.setLine(0, dividerY, stubX, dividerY);
            dividerUnderline.setLine(IconMargin, dividerY, stubX, dividerY);
            dividerStub.setLine(stubX, dividerY, stubX, dividerY - stubHeight);

            ////height = PadTop + (float)dividerY + IconDescent; // for aligning 1st icon with label bottom
            min.width = (float)stubX + IconPadLeft; // be symmetrical with left padding
            //width += IconPadLeft;
        }
        
        if (hasChildren()) {
            Size children = layoutChildren(new Size(), false);
            if (min.width < childOffsetX() + children.width + ChildPadX)
                min.width = childOffsetX() + children.width + ChildPadX;
            min.height += children.height;
            min.height += ChildOffsetY + ChildrenPadBottom; // additional space below last child before bottom of node
        }
        //        else if (iconShowing()) {
        //            height += IconPadBottom;
        //        }
        //else add pad or make sure vertical centering the plain label

        //-------------------------------------------------------
        // display any icons
        //-------------------------------------------------------
        
        if (iconShowing()) {
            float iconWidth = IconWidth;
            float iconHeight = IconHeight;
            float iconX = IconPadLeft;
            //float iconY = dividerY - IconAscent;
            //float iconY = dividerY - iconHeight; // align bottom of 1st icon with bottom of label
            //float iconY = PadTop;

            /*
            if (iconY < IconMinY) {
                // this can happen if font size is very small when
                // alignining the first icon with the bottom of the text label
                iconY = IconMinY;
                dividerY = iconY + IconAscent;
            }
            */

            float iconPillarX = iconX;
            float iconPillarY = IconPillarPadY;
            //iconPillarY = EdgePadY;

            //float totalIconHeight = icons * IconHeight;
            float totalIconHeight = (float) mIconBlock.getHeight();
            float iconPillarHeight = totalIconHeight + IconPillarPadY * 2;


            if (min.height < iconPillarHeight) {
                min.height += iconPillarHeight - min.height;
            } else if (mIsRectShape) {
                // special case prettification -- if vertically centering
                // the icon stack would only drop it down by up to a few
                // pixels, go ahead and do so because it's so much nicer
                // to look at.
                float centerY = (min.height - totalIconHeight) / 2;
                if (centerY > IconPillarPadY+3)
                    centerY = IconPillarPadY+3;
                iconPillarY = centerY;
            }
            
            if (!mIsRectShape) {
                float height;
                if (isAutoSized())
                    height = min.height;
                else
                    height = Math.max(min.height, givenHeight);
                iconPillarY = height / 2 - totalIconHeight / 2;
            }
            
            mIconBlock.setLocation(iconPillarX, iconPillarY);
        }

        return min;
    }

    private float childOffsetX() {
        if (isCenterLayout)
            return childP.x;
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }
    private float childOffsetY() {
        if (isCenterLayout)
            return childP.y;
        float baseY;
        if (iconShowing()) {
            //baseY = (float) (mIconResource.getY() + IconHeight + ChildOffsetY);
            baseY = (float) dividerUnderline.getY1();
        } else {
            baseY = relativeLabelY() + getLabelBox().getHeight();
        }
        baseY += ChildOffsetY;
        return baseY;
    }
    
    /**
     * Need to be able to do this seperately from layout -- this
     * get's called everytime a node's location is changed so
     * that's it's children will follow along with it.
     *
     * Children are laid out relative to the parent, but given
     * absolute map coordinates.  Note that because if this, anytime
     * we're computing a location for a child, we have to factor in
     * the current scale factor of the parent.
     */
    
    void layoutChildren() {
        layoutChildren(null, false);
    }
    
    // for computing size only
    private Size layoutChildren(Size result) {
        return layoutChildren(0, 0, result);
    }

    private Rectangle2D child_box = new Rectangle2D.Float(); // for debug
    private Size layoutChildren(Size result, boolean sizeOnly)
    {
        if (!hasChildren())
            return Size.None;

        float baseX = 0;
        float baseY = 0;

        if (!sizeOnly) {
            baseX = getX() + childOffsetX() * getScale();
            baseY = getY() + childOffsetY() * getScale();
        }

        //childBaseX = baseX;
        //childBaseY = baseY;
        // for relative-to-parent child layouts
        //baseX = baseY = 0;

        return layoutChildren(baseX, baseY, result);
    }
        
    private void layoutChildren(float baseX, float baseY) {
        layoutChildren(baseX, baseY, null);
    }
    
    private Size layoutChildren(float baseX, float baseY, Size result)
    {
        if (true)
            layoutChildrenSingleColumn(baseX, baseY, result);
        else
            layoutChildrenGrid(baseX, baseY, result, 1);

        if (result != null) {
            result.width /= getScale();
            result.height /= getScale();
            //if (DEBUG.BOXES)
                child_box.setRect(baseX, baseY, result.width, result.height);
        }
        return result;
    }

        
    protected void layoutChildrenSingleColumn(float baseX, float baseY, Size result)
    {
        float y = baseY;
        float maxWidth = 0;
        
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(baseX, y);
            y += c.getHeight();
            y += ChildVerticalGap * getScale();

            if (result != null) {
                // track max width
                float w = c.getBoundsWidth();
                if (w > maxWidth)
                    maxWidth = w;
            }
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }

    class Column extends java.util.ArrayList
    {
        float width;
        float height;

        void layout(float baseX, float baseY, boolean center)
        {
            float y = baseY;
            Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (center)
                    c.setLocation(baseX + (width - c.getBoundsWidth())/2, y);
                else
                    c.setLocation(baseX, y);
                y += c.getHeight();
                y += ChildVerticalGap * getScale();
                // track size
                //float w = c.getBoundsWidth();
                //if (w > width)
                //  width = w;
            }
            height = y - baseY;
        }

        void add(LWComponent c)
        {
            super.add(c);
            float w = c.getBoundsWidth();
            if (w > width)
                width = w;
        }
    }

    protected void layoutChildrenGrid(float baseX, float baseY, Size result, int nColumn)
    {
        float y = baseY;
        float totalWidth = 0;
        float maxHeight = 0;
        
        Column[] cols = new Column[nColumn];
        java.util.Iterator i = getChildIterator();
        int curCol = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (cols[curCol] == null)
                cols[curCol] = new Column();
            cols[curCol].add(c);
            if (++curCol >= nColumn)
                curCol = 0;
        }

        float colX = baseX;
        float colY = baseY;
        for (int x = 0; x < cols.length; x++) {
            Column col = cols[x];
            if (col == null)
                break;
            col.layout(colX, colY, nColumn == 1);
            colX += col.width + ChildHorizontalGap;
            totalWidth += col.width + ChildHorizontalGap;
            if (col.height > maxHeight)
                maxHeight = col.height;
        }
        // peel back the last gap as no more columns to right
        totalWidth -= ChildHorizontalGap;

        if (result != null) {
            result.width = totalWidth;
            result.height = maxHeight;
        }
    }

    public float getLabelX()
    {
        return getX() + relativeLabelX() * getScale();
    }
    public float getLabelY()
    {
        return getY() + relativeLabelY() * getScale();
        /*
        if (this.labelBox == null)
            return getY() + relativeLabelY();
        else
            return (getY() + relativeLabelY()) - this.labelBox.getHeight();
        */
    }

    private float relativeLabelX()
    {
        if (isCenterLayout) {
            return labelP.x;
        } else if (isTextNode() && strokeWidth == 0) {
            return 1;
            //return 1 + (strokeWidth == 0 ? 0 : strokeWidth / 2);
        } else if (iconShowing()) {
            //offset = (float) (PadX*1.5 + genIcon.getWidth());
            //offset = (float) genIcon.getWidth() + 7;
            //offset = IconMargin + LabelPadLeft;
            return LabelPositionXWhenIconShowing;
        } else {
            float offset;
            // horizontally center if no resource icon
            int w = getLabelBox().getPreferredSize().width;
            offset = (this.width - w) / 2;
            offset++;
            //offset = 7;
            return offset;
        }
    }
    private float relativeLabelY()
    {
        //return EdgePadY;
        
        if (isCenterLayout) {
            return labelP.y;
        } else if (hasChildren()) {
            return EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            float textHeight = getLabelBox().getPreferredSize().height;
            return (this.height - textHeight) / 2;

            /*
            float textHeight = getLabelBox().getPreferredSize().height;
            if (iconShowing() && textHeight < IconHeight)
                return iconPillarY + (IconHeight - textHeight) / 2;
            else
                return EdgePadY;
            */
        }
        
        /*
          // for single resource icon style layout
        if (iconShowing() || hasChildren()) {
            if (iconShowing())
                return (float) dividerUnderline.getY1() - getLabelBox().getPreferredSize().height;
            else
                return PadTop;
        }
        else // center vertically
            return (this.height - getLabelBox().getPreferredSize().height) / 2;
        */
    }

    private static final AlphaComposite ZoomTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    public Color getRenderFillColor()
    {
        if (DEBUG.LAYOUT) if (!isAutoSized()) return Color.green;
            
        Color c = getFillColor();
        if (getParent() instanceof LWNode) {
            if (c != null && c.equals(getParent().getRenderFillColor()))
                c = VueUtil.darkerColor(c);
        }
        return c;
    }
    
    public void draw(DrawContext dc)
    {
        if (isFiltered() == false) {

            super.drawPathwayDecorations(dc);
            
            dc.g.translate(getX(), getY());
            float scale = getScale();
            if (scale != 1f) dc.g.scale(scale, scale);

            drawNode(dc);

            //-------------------------------------------------------
            // Restore graphics context
            //-------------------------------------------------------
            // todo arch: consider not restoring the scale before we draw
            // the children, and maybe even handling this in LWContainer,
            // as a way to see if we could get rid of all the confusing "x
            // * getScale()" code & awkward recursive setScale code.
            // Actually, we couldn't attempt this unless we also fully
            // changed the children be drawn in a translated GC, and the
            // hit-detection was compensated for more at search time
            // instead of by resizing the object by having getHeight, etc,
            // auto multiply by the scale factor, and actually resizing
            // the bounds-shape when we scale an object.
            
            if (scale != 1f) dc.g.scale(1/scale, 1/scale);
            dc.g.translate(-getX(), -getY());

            if (DEBUG.BOXES && hasChildren()) {
                dc.g.setColor(Color.blue);
                dc.setAbsoluteStroke(0.5);
                dc.g.draw(child_box);
            }
        }

        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------

        // This produces the cleanest code in all above -- don't
        // need to manage scaling if we translate to a region
        // where all the nodes will lie within, and then their
        // positioning auto-collapses as they're scaled down...
        if (hasChildren()) {
            //g.translate(childBaseX * ChildScale, childBaseY * ChildScale);
            //g.scale(ChildScale, ChildScale);
            //super.draw(dc.createScaled(ChildScale)); // not using this
            //g.setComposite(childComposite);
            if (isZoomedFocus())
                dc.g.setComposite(ZoomTransparency);
            super.drawChildren(dc);
        }
    }
        
    private void drawNode(DrawContext dc)
    {
        Graphics2D g = dc.g;
        
        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
        if (isSelected() && !dc.isPrinting()) {
            LWPathway p = VUE.getActivePathway();
            if (p != null && p.isVisible() && p.getCurrent() == this) {
                // SPECIAL CASE:
                // as the current element on the current pathway draws a huge
                // semi-transparent stroke around it, skip drawing our fat 
                // transparent selection stroke on this node.  So we just
                // do nothing here.
            } else {
                g.setColor(COLOR_HIGHLIGHT);
                g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
                //g.setStroke(new BasicStroke(stroke.getLineWidth() + SelectionStrokeWidth));
                g.draw(drawnShape);
            }
        }
        
        if (imageIcon != null) {
            // experimental
            //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
            imageIcon.paintIcon(null, g, 0, 0);
        } else {
            Color fillColor = getRenderFillColor();
            if (fillColor != null) { // transparent if null
                g.setColor(fillColor);
                if (isZoomedFocus())
                    g.setComposite(ZoomTransparency);
                g.fill(drawnShape);
                if (isZoomedFocus())
                    g.setComposite(AlphaComposite.Src);
            }
        }

        //-------------------------------------------------------
        // Draw the indicated border if any
        //-------------------------------------------------------

        /*
        if (!isAutoSized()) { // debug
            g.setColor(Color.green);
            g.setStroke(STROKE_ONE);
            g.draw(drawnShape);
        }
        else if (false&&isRollover()) { // debug
            // temporary debug
            //g.setColor(new Color(0,0,128));
            g.setColor(Color.blue);
            g.draw(drawnShape);
        }
        else*/
        
        if (isIndicated() && !dc.isPrinting()) {
            // todo: okay, it is GROSS to handle the indication here --
            // do it all in the viewer!
            g.setColor(COLOR_INDICATION);
            if (STROKE_INDICATION.getLineWidth() > getStrokeWidth())
                g.setStroke(STROKE_INDICATION);
            else
                g.setStroke(this.stroke);
            g.draw(drawnShape);
        }
        else if (getStrokeWidth() > 0) {
            //if (LWSelection.DEBUG_SELECTION && isSelected())
            //if (isSelected())
            //g.setColor(COLOR_SELECTION);
            //else
                g.setColor(getStrokeColor());
            g.setStroke(this.stroke);
            g.draw(drawnShape);
        }


        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        drawNodeDecorations(dc);

        // todo: create drawLabel, drawBorder & drawBody
        // LWComponent methods so can automatically turn
        // this off in MapViewer, adjust stroke color for
        // selection, etc.
        
        // TODO BUG: label sometimes getting "set" w/out sending layout event --
        // has to do with case where we pre-fill a textbox with "label", and
        // if they type nothing we don't set a label, but that's not working
        // entirely -- it manages to not trigger an update event, but somehow
        // this.label is still getting set -- maybe we have to null it out
        // manually (and maybe labelBox also)
        
        if (hasLabel() && this.labelBox != null && this.labelBox.getParent() == null) {
            // if parent is not null, this box is an active edit on the map
            // and we don't want to paint it here as AWT/Swing is handling
            // that at the moment (and at a possibly slightly different offset)
            float lx = relativeLabelX();
            float ly = relativeLabelY();
            g.translate(lx, ly);
            this.labelBox.draw(dc);
            g.translate(-lx, -ly);

            // todo: this (and in LWLink) is a hack -- can't we
            // do this relative to the node?
            //this.labelBox.setMapLocation(getX() + lx, getY() + ly);
        }

    }

    public boolean doesRelativeDrawing() { return false; }

    /*
    public void XX_drawChild(LWComponent child, DrawContext dc)
    {
        // can use this if children could ever do anything to the scale
        // (thus we'd need to protect each child from changes made
        // by others)
        //child.draw(dc.createScaled(ChildScale));
    }
    
    public void X_drawChild(LWComponent child, DrawContext dc)
    {
        //Graphics2D g = dc.g;
        //g.translate(childBaseX * ChildScale, childBaseY * ChildScale);
        // we double the translation because the translation done by
        // the child will happen in a shrunk context -- but that only works if ChildScale == 0.5!
        //g.translate((double)child.getX() * ChildScale, (double)child.getY() * ChildScale);
        dc.g.translate(child.getX(), child.getY());
        dc.g.scale(ChildScale, ChildScale);
        child.draw(dc);
        //g.translate(-childBaseX, -childBaseY);
    }

    public LWComponent relative_findLWComponentAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWCNode.findLWComponentAt[" + getLabel() + "]");
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());

        mapX -= getX() + childBaseX;
        mapY -= getY() + childBaseY;
        mapX /= ChildScale;
        mapY /= ChildScale;
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWContainer)c).findLWComponentAt(mapX, mapY);
                else
                    return c;
            }
        }
        return this;
    }
    */
    

    private void drawNodeDecorations(DrawContext dc)
    {
        Graphics2D g = dc.g;
        //float iconWidth = IconWidth;
        //float iconHeight = IconHeight;
        //float iconX = iconPillarX;
        //float iconY = iconPillarY;

        if (DEBUG.BOXES) {
            //-------------------------------------------------------
            // paint a divider line
            //-------------------------------------------------------
            g.setColor(Color.gray);
            g.setStroke(STROKE_EIGHTH);
            g.draw(dividerUnderline);
            g.draw(dividerStub);
        }
            
        //-------------------------------------------------------
        // paint the node icons
        //-------------------------------------------------------

        if (iconShowing()) {
            Color renderFill = getRenderFillColor();
            Color marginColor;
            if (renderFill != null) {
                if (renderFill.equals(Color.black))
                    marginColor = Color.darkGray;
                else
                    marginColor = renderFill.darker();
            } else {
                // transparent fill: base on stroke color
                marginColor = getStrokeColor().brighter();
            }
            g.setColor(marginColor);
            g.setStroke(STROKE_ONE);
            g.draw(dividerMarginLine);
            mIconBlock.draw(dc);
        }
    }
}



    

    /*
    public NodeShape getNodeShape()
    {
        //System.err.println("*** Warning: deprecated use of LWNode.getNodeShape in " + this);
        //return this.nodeShape;
        return null;
    }
    public void setNodeShape(NodeShape nodeShape)
    {
        //System.err.println("*** Warning: deprecated use of LWNode.setNodeShape on " + this);
        this.nodeShape = nodeShape;
        this.equalAspect = nodeShape.equalAspect;
        setShape(nodeShape.getShapeInstance());
        // todo perf: getShapeInstance is redundant during restores --
        // a new object was already allocated for us.
    }

    // TODO: NodeShape is only here at the moment for backward compatability
    // with old save files.
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

        // for XML persistance
        public NodeShape() {}

        public RectangularShape getShape()
        {
            return shape;
        }
        // for XML persistance 
        public void setShape(RectangularShape s)
        {
            shape = s;
        }
        // for XML persistance 
        public boolean isEqualAspect()
        {
            return equalAspect;
        }
        // for XML persistance 
        public void setEqualAspect(boolean tv)
        {
            equalAspect = tv;
        }
        RectangularShape getShapeInstance()
        {
            return (RectangularShape) shape.clone();
        }
    }

    // @remove this -- only here for ancient backward compat
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

    //private final boolean debug = true;
    */






    /*
    protected void OLD_layoutChildren()
    {
        if (!hasChildren())
            return;
        //System.out.println("layoutChildren " + this);
        java.util.Iterator i = getChildIterator();
        //float y = (relativeLabelY() + PadY) * getScale();
        // relaveLabelY used to be the BASELINE for the text -- now it's the UL of the label object
        //float y = (relativeLabelY() + getLabelBox().getHeight() + PadY/2) * getScale();
        float y = relativeLabelY() + getLabelBox().getHeight();
        if (genIcon != null)
            y += genIcon.getHeight() / 2f;
        y *= getScale();
        float childX = relativeLabelX() * getScale();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(getX() + childX, getY() + y);
            y += c.getHeight();
            y += ChildVerticalGap * getScale();
        }
    }
    */
      
    /*
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
        if (hasResource) {
            width += PadX*1.5 + genIcon.getWidth();
            //height += genIcon.getHeight(); // better match to spec
            height += genIcon.getHeight() * (2f/3f); //crude for now
        }

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
    */

    /*        if (this.width != oldWidth && lastLabel != null &&
            !(getParent() instanceof LWNode)) // todo: this last test really depends on if parent is laying us out
        {
            // on resize, keep the node's center the same
            setLocation(getX() + (oldWidth - this.width) / 2, getY());
            }*/

    /*
    private Rectangle2D getAllChildrenBounds()
    {
        // compute bounds based on a vertical stacking layout
        java.util.Iterator i = getChildIterator();
        float height = 0;
        float maxWidth = 0;
        float width;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            height += c.getBoundsHeight() + ChildVerticalGap;
            width = c.getBoundsWidth();
            //height += c.height + ChildVerticalGap;
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
    
    
    */
