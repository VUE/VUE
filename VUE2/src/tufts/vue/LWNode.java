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

import tufts.vue.shape.*;

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
 *
 * This is the core graphical object in VUE.
 * Code needs cleanup: particularly the internal layout of the node.
 *
 * @author Scott Fraize
 * @version 6/16/04
 */
public class LWNode extends LWContainer
{
    public static final Font  DEFAULT_NODE_FONT = VueResources.getFont("node.font");
    public static final Color DEFAULT_NODE_FILL = VueResources.getColor("node.fillColor");
    public static final int   DEFAULT_NODE_STROKE_WIDTH = VueResources.getInt("node.strokeWidth");
    public static final Color DEFAULT_NODE_STROKE_COLOR = VueResources.getColor("node.strokeColor");
    public static final Font  DEFAULT_TEXT_FONT = VueResources.getFont("text.font");
    
    /** how much smaller children are than their immediately enclosing parent (is cumulative) */
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    
    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled, used for computing hits
    protected boolean autoSized = true; // compute size from label & children

    //-----------------------------------------------------------------------------
    // consider moving all the below stuff into a layout object

    private transient Line2D dividerUnderline = new Line2D.Float();
    private transient Line2D dividerStub = new Line2D.Float();

    private transient boolean mIsRectShape = true;
    //private transient boolean mIsTextNode = false; // todo: are we saving this in XML???

    private transient Line2D.Float mIconDivider = new Line2D.Float(); // vertical line between icon block & node label / children
    private transient Point2D.Float mLabelPos = new Point2D.Float(); // for use with irregular node shapes
    private transient Point2D.Float mChildPos = new Point2D.Float(); // for use with irregular node shapes


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

    /** internal convenience */
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
    
    /** internal convenience */
    LWNode(String label, float x, float y)
    {
        this(label, x, y, null);
    }

    /** internal convenience */
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
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
    
    public boolean supportsUserLabel() {
        return true;
    }
    public boolean supportsUserResize() {
        return true;
    }
    
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

    //public void setIcon(javax.swing.ImageIcon icon) {}
    //public javax.swing.ImageIcon getIcon() { return null; }
    
    
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
        notify(LWKey.Shape, new Undoable(old) { void undo() { setShape((RectangularShape)old); }} );
    }

    /** @return shape object with map coordinates -- can be used for hit testing, drawing, etc */
    public Shape getShape() {
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
        if (imageIcon != null) {
            return super.contains(x,y);
        } else {
            if (true) {
                return boundsShape.contains(x, y);
            } else {
                // DEBUG: util irregular shapes can still give access to children
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
        }
        
        // to compensate for stroke width here, could get mathy here
        // and move the x/y strokeWidth units along a line toward
        // the center of the object, which wouldn't be perfect
        // but would be reasonable.
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
    private boolean isCenterLayout = false;// todo: get rid of this and use mChildPos, etc for boxed layout also
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

        if (mIsRectShape) {
            // todo: cleaner move this to layout_boxed, and have layout methods handle
            // the auto-size check (min gets set to request if request is bigger), as
            // layout_centered has to compute that now anyway.
            mIconDivider.setLine(IconMargin, MarginLinePadY, IconMargin, min.height-MarginLinePadY);
        } else {
            // No longer need to clip: can just use content height!
            //if (iconShowing())
            //    VueUtil.clipToYCrossings(mIconDivider, drawnShape, MarginLinePadY);
        }

    
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

    
    /**
     * Layout the contents of the node centered, and return the min size of the node.
     * @return the minimum rectangular size of node shape required to to contain all
     * the visual node contents
     */
    private Size layout_centered()
    {
        NodeContent content = getLaidOutNodeContent();
        Size minSize = new Size(content);
        Size node = new Size(content);

        // Current node size is largest of current size, or
        // minimum content size.
        if (!isAutoSized()) {
            node.fitWidth(getWidth());
            node.fitHeight(getHeight());
        }

        //Rectangle2D.Float content = new Rectangle2D.Float();
        //content.width = minSize.width;
        //content.height = minSize.height;

        RectangularShape nodeShape = (RectangularShape) drawnShape.clone();
        nodeShape.setFrame(0,0, content.width, content.height);
        //nodeShape.setFrame(0,0, minSize.width, minSize.height);
        
        // todo perf: allow for skipping of searching for minimum size
        // if current size already big enough for content

        if (growShapeUntilContainsContent(nodeShape, content)) {
            // content x/y is now at the center location of our MINUMUM size,
            // even tho our current size may be bigger and require moving it..
            minSize.fit(nodeShape);
            node.fit(minSize);
        }

        //Size text = getTextSize();
        //mLabelPos.x = content.x + (((float)nodeShape.getWidth()) - text.width) / 2;
        //mLabelPos.x = content.x + (node.width - text.width) / 2;

        nodeShape.setFrame(0,0, node.width, node.height);
        layoutContentInShape(nodeShape, content);
        if (DEBUG.LAYOUT) System.out.println("*** " + this + " content placed at " + content + " in " + nodeShape);

        content.layoutTargets(node);
        
        return minSize;
    }

    /**
     * Brute force increase the size of the given arbitrary shape until it's borders fully
     * contain the given rectangle when it is centered in the shape.  Algorithm starts
     * with size of content for shape (which would work it it was rectangular) then increases
     * width & height incrememntally by %10 until content is contained, then backs off 1 pixel
     * at a time to tighten the fit.
     *
     * @param shape - the shape to grow: expected be zero based (x=y=0)
     * @param content - the rectangle to ensure we can contain (x/y is ignored: it's x/y value at end will be centered)
     * @return true if the shape was grown
     */
    private boolean growShapeUntilContainsContent(RectangularShape shape, NodeContent content)
    {
        final int MaxTries = 1000; // in case of loops (e.g., a broke shape class whose contains() never succeeds)
        final float increment;
        if (content.width > content.height)
            increment = content.width * 0.1f;
        else
            increment = content.height * 0.1f;
        final float xinc = increment;
        final float yinc = increment;
        //final float xinc = content.width * 0.1f;
        //final float yinc = (content.height / content.width) * xinc;
        //System.out.println("xinc=" + xinc + " yinc=" + yinc);
        int tries = 0;
        while (!shape.contains(content) && tries < MaxTries) {
        //while (!content.fitsInside(shape) && tries < MaxTries) {
            shape.setFrame(0, 0, shape.getWidth() + xinc, shape.getHeight() + yinc);
            //System.out.println("trying size " + shape + " for content " + content);
            layoutContentInShape(shape, content);
            tries++;
        }
        if (tries > 0) {
            final float shrink = 1f;
            System.out.println("Contents of " + shape + "  rought  fit  to " + content + " in " + tries + " tries");
            do {
                shape.setFrame(0, 0, shape.getWidth() - shrink, shape.getHeight() - shrink);
                //System.out.println("trying size " + shape + " for content " + content);
                layoutContentInShape(shape, content);
                tries++;
            } while (shape.contains(content) && tries < MaxTries);
            //} while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth() + shrink, shape.getHeight() + shrink);
            //layoutContentInShape(shape, content);

            /*
            if (getLabel().indexOf("*s") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }

            if (getLabel().indexOf("*ml") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }
            */
            
        }
        
        if (tries >= MaxTries) {
            System.err.println("Contents of " + shape + " failed to contain " + content + " after " + tries + " tries.");
        } else if (tries > 0) {
            if (DEBUG.Enabled) System.out.println("Contents of " + shape + " grown to contain " + content + " in " + tries + " tries");
        } else
            if (DEBUG.Enabled) System.out.println("Contents of " + shape + " already contains " + content);
        if (DEBUG.LAYOUT) System.out.println("*** " + this + " content minput at " + content + " in " + shape);
        return tries > 0;
    }
    
    /**
     * Layout the given content rectangle in the given shape.  The default is to center
     * the content rectangle in the shape, however, if the shape in an instance
     * of tufts.vue.shape.RectangularPoly2D, it will call getContentGravity() to
     * determine layout gravity (CENTER, NORTH, EAST, etc).
     *
     * @param shape - the shape to layout the content in
     * @param content - the region to layout in the shape: x/y values will be set
     *
     * @see tufts.vue.shape.RectangularPoly2D, 
     */
    private void layoutContentInShape(RectangularShape shape, NodeContent content)
    {
        final float width = (float) shape.getWidth();
        final float height = (float) shape.getHeight();
        final float margin = 0.5f; // safety so 100% sure will be in-bounds
        boolean content_laid_out = false;

        if (shape instanceof RectangularPoly2D) {
            int gravity = ((RectangularPoly2D)shape).getContentGravity();
            content_laid_out = true;
            if (gravity == RectangularPoly2D.CENTER) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.EAST) {
                content.x = margin;
                content.y = (float) (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.WEST) {
                content.x = (width - content.width) + margin;
                content.y = (float) Math.floor((height - content.height) / 2);
            } else if (gravity == RectangularPoly2D.NORTH) {
                content.x = (width - content.width) / 2;
                content.y = margin;
            } else if (gravity == RectangularPoly2D.SOUTH) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) - margin;
            } else {
                System.err.println("Unsupported content gravity " + gravity + " on shape " + shape + "; defaulting to CENTER");
                content_laid_out = false;
            }
        }
        if (!content_laid_out) {
            // default is center layout
            content.x = (width - content.width) / 2;
            content.y = (height - content.height) / 2;
        }
    }

    //private static final Rectangle2D.Float EmptyRegion = new Rectangle2D.Float(0,0,0,0);

    private class NodeContent extends Rectangle2D.Float {

        // regions for icons, label & children
        private Rectangle2D.Float rIcons;
        private Rectangle2D.Float rLabel = new Rectangle2D.Float();
        private Rectangle2D.Float rChildren;

        /**
         * Initial position is 0,0.  Regions are all normalized to offsets from 0,0.
         * Construct node content layout object: layout the regions for
         * icons, label & children.  Does NOT do the final layout (setting
         * LWNode member variables, laying out the child nodes, etc, until
         * layoutTargts() is called).
         */
        NodeContent()
        {
            if (hasLabel()) {
                Size text = getTextSize();
                //rLabel = new Rectangle2D.Float(0,0, text.width, text.height);
                rLabel.width = text.width;
                rLabel.height = text.height;
                this.width = text.width;
                this.height = text.height;
            } 
            if (iconShowing()) {
                rIcons = new Rectangle2D.Float(0, 0, mIconBlock.width, mIconBlock.height);
                width += mIconBlock.width;
                // move label to right to make room for icon block at left
                rLabel.x += mIconBlock.width + ChildPadX;
            }
            if (hasChildren()) {
                Size children = layoutChildren(new Size(), true);
                //float childx = rLabel.x + ChildPadX;
                float childx = rLabel.x;
                float childy = rLabel.height + ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);
                height = rLabel.height + ChildPadY + children.height;

                fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
                /*
                if (rIcons != null) {
                    fitWidth(mIconBlock.width + ChildPadX + children.width);
                } else {
                    fitWidth(children.width + ChildPadX);
                }
                */
                //height += ChildOffsetY + children.height + ChildrenPadBottom;
            }
            
            if (rIcons != null) {
                fitHeight(mIconBlock.height);
                // vertically center icon block
                // [no point for now as we are never smaller than the full content bounding box]
                //rIcons.y = (height - rIcons.height) / 2;

                // fudge the icon block down a bit in same way as layout_boxed
                // as prettification.

                // todo: even better: first priority should be to
                // center the top SEGMENT of the icon block
                // verticallly in the height of the FIRST LINE of the
                // text label (which is usually taller) and then only
                // move up if we're pressed for space.  We don't have
                // a way to get the height of that first line handy
                // at the moment tho.

                float iconBlockExtraY = height - mIconBlock.height;
                if (iconBlockExtraY > 0) {
                    if (iconBlockExtraY >= IconPillarFudgeY)
                        rIcons.y = IconPillarFudgeY;
                    else
                        rIcons.y = iconBlockExtraY;
                }
            }
        }

        /** layout the actual targets of our regions */
        void layoutTargets(Size nodeSize) {
            if (DEBUG.LAYOUT) System.out.println("*** " + this + " laying out targets");
            mLabelPos.setLocation(x + rLabel.x, y + rLabel.y);
            if (rIcons != null) {
                mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
                mIconDivider.setLine(mIconBlock.x + mIconBlock.width, this.y,
                                     mIconBlock.x + mIconBlock.width, this.y + this.height);
                //mIconDivider.setLine(mIconBlock.x + mIconBlock.width, 0,
                //                     mIconBlock.x + mIconBlock.width, nodeSize.height);
            }
            if (rChildren != null) {
                mChildPos.setLocation(x + rChildren.x, y + rChildren.y);
                layoutChildren();
            }
        }
        
        private Rectangle2D.Float checker = new Rectangle2D.Float();
        boolean fitsInside(RectangularShape shape) {
            //return shape.contains(this);
            boolean fit = true;
            copyTranslate(rLabel, checker, x, y);
            fit &= shape.contains(checker);
            System.out.println(this + " checked " + VueUtil.out(shape) + " for label " + VueUtil.out(rLabel) + " RESULT=" + fit);
            if (rIcons != null) {
                copyTranslate(rIcons, checker, x, y);
                fit &= shape.contains(checker);
                System.out.println("Contains    icons: " + fit);
            }
            if (rChildren != null) {
                copyTranslate(rChildren, checker, x, y);
                fit &= shape.contains(checker);
                System.out.println("Contains children: " + fit);
            }
            return fit;
        }

        private void copyTranslate(Rectangle2D.Float src, Rectangle2D.Float dest, float xoff, float yoff) {
            dest.width = src.width;
            dest.height = src.height;
            dest.x = src.x + xoff;
            dest.y = src.y + yoff;
        }

        private void fitWidth(float w) {
            if (width < w)
                width = w;
        }
        private void fitHeight(float h) {
            if (height < h)
                height = h;
        }
        
        /** layout the REGIONS only: not the underlying components */
        /*
        void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }
        */

        public String toString() {
            return "NodeContent[" + VueUtil.out(this) + "]";
        }
    }

    /** 
     * @return internal node content already laid out
     */
    
    private NodeContent _lastNodeContent;
    private NodeContent getLaidOutNodeContent()
    {
        return _lastNodeContent = new NodeContent();
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
                if (centerY > IconPillarPadY+IconPillarFudgeY)
                    centerY = IconPillarPadY+IconPillarFudgeY;
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
        if (isCenterLayout) {
            //System.out.println("\tchildPos.x=" + mChildPos.x);
            return mChildPos.x;
        }
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }
    private float childOffsetY() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.y=" + mChildPos.y);
            return mChildPos.y;
        }
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

    //private Rectangle2D child_box = new Rectangle2D.Float(); // for debug
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
        if (DEBUG.LAYOUT) System.out.println("*** " + this + " layoutChildren at " + baseX + "," + baseY);
        //if (baseX > 0) new Throwable("LAYOUT-CHILDREN").printStackTrace();
        if (true)
            layoutChildrenSingleColumn(baseX, baseY, result);
        else
            layoutChildrenGrid(baseX, baseY, result, 1);

        if (result != null) {
            result.width /= getScale();
            result.height /= getScale();
            //if (DEBUG.BOXES)
            //child_box.setRect(baseX, baseY, result.width, result.height);
        }
        return result;
    }

        
    protected void layoutChildrenSingleColumn(float baseX, float baseY, Size result)
    {
        float y = baseY;
        float maxWidth = 0;
        boolean first = true;
        java.util.Iterator i = getChildIterator();
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();
            c.setLocation(baseX, y);
            y += c.getHeight();

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

    //todo: change to use membar variable only (can get rid of method actually if do so)
    private float relativeLabelX()
    {
        if (isCenterLayout) {
            return mLabelPos.x;
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
    //todo: change to use membar variable only (can get rid of method actually if do so)
    private float relativeLabelY()
    {
        //return EdgePadY;
        
        if (isCenterLayout) {
            return mLabelPos.y;
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


        if (DEBUG.BOXES) {
            dc.g.setColor(Color.darkGray);
            dc.setAbsoluteStroke(0.5);
            //if (hasChildren()) dc.g.draw(child_box);
            if (_lastNodeContent != null && !mIsRectShape)
                dc.g.draw(_lastNodeContent);
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
            if (DEBUG.LAYOUT) System.out.println("*** " + this + " drawing label at " + lx + "," + ly);
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

        if (DEBUG.BOXES && mIsRectShape) {
            //-------------------------------------------------------
            // paint a divider line
            //-------------------------------------------------------
            g.setColor(Color.gray);
            dc.setAbsoluteStroke(0.5);
            g.draw(dividerUnderline);
            g.draw(dividerStub);
        }
            
        //-------------------------------------------------------
        // paint the node icons
        //-------------------------------------------------------

        if (iconShowing()) {
            final Color renderFill = getRenderFillColor();
            final Color marginColor;
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
            g.draw(mIconDivider);
            mIconBlock.draw(dc);
        }
    }


    // experimental
    private transient ImageIcon imageIcon = null;
    // experimental
    void setImage(Image image)
    {
        imageIcon = new ImageIcon(image, "Image Description");
        setAutoSized(false);
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    //------------------------------------------------------------------
    // Constants for layout of the visible objects in a node
    // (label, icons & children, etc)
    //------------------------------------------------------------------

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
    //private static final float TextWidthFudgeFactor = 1 + 0.1f; // 10% fudge
    private static final float TextWidthFudgeFactor = 1; // off for debugging (Almost uneeded in new Mac JVM's)
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.
    //private static final float TextWidthFudgeFactor = 1;
    //private static final int DividerStubPadX = TextWidthFudgeAmount;

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    private static final int IconPillarFudgeY = 4; // attempt to get top icon to align with top of 1st caps char in label text box
    
}

