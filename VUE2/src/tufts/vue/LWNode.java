package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.TextLayout;
//import java.awt.font.LineBreakMeasurer;
//import java.awt.font.TextAttribute;
//import java.text.AttributedString;

//import javax.swing.JLabel;
//import javax.swing.JTextArea;
//import javax.swing.JTextPane;
//import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

//import javax.swing.text.*;

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
    implements Node, ClickHandler
{
    //------------------------------------------------------------------
    // Constants affecting the internal layout of nodes & any children
    //------------------------------------------------------------------
    static final float ChildScale = 0.75f;   // % scale-down of children
    
    private static final boolean AlwaysShowIcon = false;
        
    private static final int PadTop = 3;
    //private static final int PadY = PadTop + 3;
    //private static final int PadX = 12;

    private static final int IconWidth = 28;
    private static final int IconHeight = 19;
    private static final int IconPadLeft = 4;
    private static final int IconPadRight = 4;
    private static final int IconMargin = IconPadLeft + IconWidth + IconPadRight;
    /** this is the descent of the closed icon down below the divider line */
    private static final float IconDescent = IconHeight / 3f;
    /** this is the rise of the closed icon above the divider line */
    private static final float IconAscent = IconHeight - IconDescent;
    private static final int IconPadBottom = (int) IconAscent;
    private static final int IconMinY = IconPadLeft;

    // TODO: need to multiply all these by ChildScale now...
    private static final int ChildOffsetX = IconMargin; // X offset of children when icon showing
    private static final int ChildOffsetY = 1; // how far children down from bottom of icon
    private static final int ChildPadX = 5; // min space at left/right of children
    private static final int ChildVerticalGap = 3; // vertical space between children
    private static final int ChildHorizontalGap = 3; // horizontal space between children
    private static final int ChildrenPadBottom = ChildPadX - ChildVerticalGap; // make same as space at right
    //    private static final int ChildrenPadBottom = 3; // space at bottom after all children
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case.
    private static final int DividerStubPadX = 9;
    
    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled, used for computing hits
    protected NodeShape nodeShape;
    protected boolean equalAspect = false;
    //todo: could collapse all of the above into NodeShape if we want to resurrect it
    
    private ImageIcon imageIcon = null;
    private boolean autoSized = true; // compute size from label & children

    private RectangularShape genIcon = new RoundRectangle2D.Float(0,0, IconWidth,IconHeight, 12,12);
    private Line2D dividerLine = new Line2D.Float();
    private Line2D dividerStub = new Line2D.Float();

    private boolean isRectShape = true;
    
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
        setStrokeWidth(1f);// todo
        setLocation(x, y);
        //if (getAbsoluteWidth() < 10 || getAbsoluteHeight() < 10)
        setSize(10,10);
        setLabel(label);
        setFont(FONT_NODE_DEFAULT);
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
        return AlwaysShowIcon || getResource() != null;
    }

    // was text box hit?  coordinates are component local
    private boolean textBoxHit(float cx, float cy)
    {
        float lx = relativeLabelX() - IconPadRight;
        float ly = relativeLabelY() - PadTop;
        float height = getLabelBox().getHeight() + PadTop;
        float width = IconPadRight + getLabelBox().getWidth() + DividerStubPadX;

        return
            cx >= lx &&
            cy >= ly &&
            cx <= lx + width &&
            cy <= ly + height;
    }
    
    
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleDoubleClick " + e + " " + this);

        // need to get mapmouseevent here so can ask viewer
        // to activate label edit.

        float cx = e.getComponentX();
        float cy = e.getComponentY();

        if (textBoxHit(cx, cy)) {
            e.getViewer().activateLabelEdit(this);
        } else {
            // by default, a double-click anywhere else in
            // node opens the resource
            
            if (getResource() != null) {
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
        return true;
    }

    public boolean handleSingleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleSingleClick " + e + " " + this);
        // "handle", but don't actually do anything, if they single click on
        // the icon (to prevent activating a label edit if they click here)
        //return iconShowing() && genIcon.contains(e.getComponentPoint());

        // for now, never activate a label edit on just a single click.
        return true;
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

        // optimization hack
        isRectShape = (shape instanceof Rectangle2D || shape instanceof RoundRectangle2D);
        
        this.boundsShape = shape;
        this.drawnShape = (RectangularShape) shape.clone();
        adjustDrawnShape();
        layout();
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
        if (strokeWidth > 0) {
            // todo opt: cache this
            final Rectangle2D.Float r = new Rectangle2D.Float();
            r.setRect(rect);
            
            // todo: this is a hack -- expanding the test rectangle to
            // compensate for the border width, but it works
            // mostly -- only a little off on non-rectangular sides
            // of shapes.
            
            final float adj = strokeWidth / 2;
            r.x -= adj;
            r.y -= adj;
            r.width += strokeWidth;
            r.height += strokeWidth;
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
            if (isRectShape) {
                return boundsShape.contains(x, y);
            } else {
                float cx = x - getX();
                float cy = y - getY();
                return boundsShape.contains(x, y)
                    || textBoxHit(cx, cy)
                    || genIcon.contains(cx, cy);
            }
        }
        // if shape is not rectangular, check textBoxHit & genIcon hit
        
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
        //layoutChildren(); // we do this for our rollover zoom hack so children are repositioned
        // LWContainer.setScale handles this
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
        // This was to shrink the drawn shape size by border width
        // so it fits entirely inside the bounds shape, tho
        // we're not making use of that right now.
        if (DEBUG_LAYOUT) System.out.println(this + " adjustDrawnShape " + getAbsoluteWidth() + "x" + getAbsoluteHeight());
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
    protected void layout()
    {
        if (inLayout) {
            new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
            return;
        }
        inLayout = true;
        if (DEBUG_LAYOUT) System.out.println("*** LAYOUT " + this);

        float oldWidth = getWidth();
        float oldHeight = getHeight();

        //if (isAutoSized() || hasChildren())
        //setPreferredSize(!isAutoSized());

        boolean growOnly = false;//for non-autosized
        Dimension text = getLabelBox().getPreferredSize(); // may be important to use pref size -- keep for now
        float width = text.width;
        //float width = s.width + PadX;
        //float height = s.height + PadY;
        //float height = getLabelBox().getHeight() + IconHeight/3f;
        //float height = getLabelBox().getHeight() + IconDescent;
        float height = PadTop + text.height;
        
        if (getLabelBox().getHeight() != text.height) {
            // NOTE: prefHeight often a couple of pixels less than getHeight
            System.err.println("prefHeight != height in " + this);
            System.err.println("\tpref=" + text.height);
            System.err.println("\treal=" + getLabelBox().getHeight());
        }
        
        //-------------------------------------------------------
        // resource icon
        //-------------------------------------------------------
        
        if (iconShowing()) {
            double dividerY = PadTop + text.height;
            
            float iconWidth = IconWidth;
            float iconHeight = IconHeight;
            double iconX = IconPadLeft;
            double iconY = dividerY - IconAscent;

            if (iconY < IconMinY) {
                // this can happen if font size is very small
                iconY = IconMinY;
                dividerY = iconY + IconAscent;
            }
            
            //if (hasNotes()) iconHeight *= 2;
            genIcon.setFrame(iconX, iconY, iconWidth, iconHeight);

            double stubX = relativeLabelX() + text.width + DividerStubPadX;
            double stubHeight = DividerStubAscent;
            
            dividerLine.setLine(0, dividerY, stubX, dividerY);
            dividerStub.setLine(stubX, dividerY, stubX, dividerY - stubHeight);

            height = PadTop + (float)dividerY + IconDescent;
            width = (float)stubX + IconPadLeft; // be symmetrical with left padding
        } else {
            width += 12;
        }

        //-------------------------------------------------------
        // set size (was setPreferredSize)
        //-------------------------------------------------------

        if (hasChildren()) {
            float[] size = new float[2];
            layoutChildren(size);
            float childrenWidth = size[0];
            float childrenHeight = size[1];
            if (width < childOffsetX() + childrenWidth + ChildPadX)
                width = childOffsetX() + childrenWidth + ChildPadX;
            height += childrenHeight;
            height += ChildOffsetY + ChildrenPadBottom; // additional space below last child before bottom of node
        } else if (iconShowing()) {
            height += IconPadBottom;
        }
        //else add pad or make sure vertical centering the plain label
        
        setSizeNoLayout(width, height);

        /*
        if (growOnly) {
            if (this.width > width)
                width = this.width;
            if (this.height > height)
                height = this.height;
            if (width > this.width || height > this.height)
                setSizeNoLayout(width, height);
        } else
            setSizeNoLayout(width, height);
        */
        

        // todo: handle thru event?
        if (getParent() != null && (oldWidth != getWidth() || oldHeight != getHeight())) {
            //new Throwable("LAYING OUT PARENT " + this).printStackTrace();
            getParent().layout();
        }

        inLayout = false;
    }

    public void layoutChildren()
    {
        layoutChildren(null);
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
    
    private float childBaseX = 0;
    private float childBaseY = 0;
    protected void layoutChildren(float[] size)
    {
        if (!hasChildren())
            return;

        float baseX = childOffsetX() * getScale();
        float baseY = 0;
        if (iconShowing()) {
            baseY = (float) (genIcon.getY() + IconHeight + ChildOffsetY);
        } else {
            baseY = relativeLabelY() + getLabelBox().getHeight();
        }
        baseY *= getScale();
        baseX += getX();
        baseY += getY();

        //System.out.println("layoutChildren " + this);

        childBaseX = baseX;
        childBaseY = baseY;
        // for relative-to-parent child layouts
        //baseX = baseY = 0;
        
        //layoutChildrenSingleColumn(baseX, baseY, size);
        layoutChildrenGrid(baseX, baseY, size);

        if (size != null) {
            size[0] /= getScale();
            size[1] /= getScale();
            //size[0] *= ChildScale;
            //size[1] *= ChildScale;
        }
        
    }

        
    protected void layoutChildrenSingleColumn(float baseX, float baseY, float[] size)
    {
        float y = baseY;
        float maxWidth = 0;
        
        java.util.Iterator i = getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(baseX, y);
            //c.setAbsoluteLocation(getAbsoluteX() + childBaseX + baseX,
            //                    getAbsoluteY() + childBaseY + y);
            y += c.getHeight();
            //y += c.getAbsoluteHeight();
            y += ChildVerticalGap * getScale();

            if (size != null) {
                // track max width
                float w = c.getBoundsWidth();
                //float w = c.getAbsoluteWidth();
                if (w > maxWidth)
                    maxWidth = w;
            }
        }

        if (size != null) {
            size[0] = maxWidth;
            size[1] = (y - baseY);
        }
    }

    class Column extends java.util.ArrayList
    {
        float width;
        float height;

        public void layout(float baseX, float baseY)
        {
            float y = baseY;
            Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.setLocation(baseX, y);
                y += c.getHeight();
                y += ChildVerticalGap * getScale();
                // track size
                float w = c.getBoundsWidth();
                if (w > width)
                    width = w;
            }
            height = y - baseY;
        }
        
    }

    final static int nColumn = 2;
        
    protected void layoutChildrenGrid(float baseX, float baseY, float[] size)
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
            col.layout(colX, colY);
            colX += col.width + ChildHorizontalGap;
            totalWidth += col.width + ChildHorizontalGap;
            if (col.height > maxHeight)
                maxHeight = col.height;
        }
        totalWidth -= ChildHorizontalGap;

        if (size != null) {
            size[0] = totalWidth;
            size[1] = maxHeight;
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

    private int childOffsetX()
    {
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }

    private float relativeLabelX()
    {
        float offset;
        if (iconShowing()) {
            //offset = (float) (PadX*1.5 + genIcon.getWidth());
            //offset = (float) genIcon.getWidth() + 7;
            offset = IconMargin;
        } else {
            // Center if no resource icon
            int w = getLabelBox().getPreferredSize().width;
            offset = (this.width - w) / 2;
            offset++;
            //offset = 7;
        }
        return offset;
    }
    private float relativeLabelY()
    {
        if (iconShowing() || hasChildren()) {
            if (iconShowing())
                return (float) dividerLine.getY1() - getLabelBox().getPreferredSize().height;
            else
                return PadTop;
        }
        else // center vertically
            return (this.height - getLabelBox().getPreferredSize().height) / 2;
    }

    //private static AlphaComposite childComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    
    public void draw(DrawContext dc)
    {
        Graphics2D g = dc.g;
        
        g.translate(getX(), getY());
        float scale = getScale();
        if (scale != 1f) g.scale(scale, scale);

        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
        if (imageIcon != null) {
            // experimental
            //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
            imageIcon.paintIcon(null, g, 0, 0);
        } else {
            Color fillColor = getFillColor();
            if (fillColor != null) { // transparent if null
                g.setColor(fillColor);
                //g.setColor(new Color(128,128,128,128));
                if (isZoomedFocus())
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                g.fill(drawnShape);
            }
        }

        //-------------------------------------------------------
        // Draw the indicated border if any
        //-------------------------------------------------------
        if (false&&isRollover()) {
            // temporary debug
            //g.setColor(new Color(0,0,128));
            g.setColor(Color.blue);
            g.draw(drawnShape);
        }
        else if (isIndicated()) {
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
            if (isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(getStrokeColor());
            g.setStroke(this.stroke);
            g.draw(drawnShape);
        }


        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        // Here we'll check the zoom level, and if iit's say,
        // over 800%, we could draw the resource string in a tiny
        // font right in the icon.

        if (iconShowing())
            drawUnderlineAndIcon(dc);

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
            this.labelBox.draw(g);
            g.translate(-lx, -ly);

            // todo: this (and in LWLink) is a hack -- can't we
            // do this relative to the node?
            //this.labelBox.setMapLocation(getX() + lx, getY() + ly);
        }
        
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
        
        if (scale != 1f) g.scale(1/scale, 1/scale);
        g.translate(-getX(), -getY());

        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------

        // This produces the cleanest code in all above -- don't
        // need to manage scaling if we translate to a region
        // where all the nodes will lie within, and then their
        // positioning auto-collapses as their scaled down...
        if (hasChildren()) {
            //g.translate(childBaseX * ChildScale, childBaseY * ChildScale);
            //g.scale(ChildScale, ChildScale);
            //super.draw(dc.createScaled(ChildScale)); // not using this
            //g.setComposite(childComposite);
            super.draw(dc);
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
    

    private static final String NoResource = VueUtil.isMacPlatform() ? "---" : "__";
    // On PC, two underscores look better than "---" in default Trebuchet font,
    // which leaves the dashes high in the box.
    
    
    private static Font MinisculeFont = new Font("SansSerif", Font.PLAIN, 1);
    //private static Font MinisculeFont = new Font("Arial Narrow", Font.PLAIN, 1);

    private void drawUnderlineAndIcon(DrawContext dc)
    {
        Graphics2D g = dc.g;
        float iconHeight = (float) genIcon.getHeight();
        float iconWidth = (float) genIcon.getWidth();
        float iconX = (float) genIcon.getX();
        float iconY = (float) genIcon.getY();

        //-------------------------------------------------------
        // paint the divider line
        //-------------------------------------------------------

        g.setColor(Color.black);
        g.setStroke(STROKE_HALF);
        g.draw(dividerLine);
        g.draw(dividerStub);
            
        //-------------------------------------------------------
        // paint the resource icon
        //-------------------------------------------------------

        g.setColor(Color.white);
        g.fill(genIcon);
        g.setStroke(STROKE_HALF);
        g.setColor(Color.black);
        g.draw(genIcon);

        //-------------------------------------------------------
        // draw the short icon name 
        //-------------------------------------------------------
            
        g.setFont(FONT_ICON);
        String extension = NoResource;
        if (getResource() != null)
            extension = getResource().getExtension();
        TextLayout row = new TextLayout(extension, g.getFont(), g.getFontRenderContext());
        //g.drawString(extension, 0, (int)(genIcon.getHeight()/1.66));
        Rectangle2D.Float tb = (Rectangle2D.Float) row.getBounds();
        
        if (DEBUG_LAYOUT) System.out.println("[" + extension + "] bounds="+tb);

        // Mac & PC 1.4.1 implementations haved reversed baselines
        // and differ in how descents are factored into bounds offsets
        g.translate(iconX, iconY);
        float xoff = (iconWidth - tb.width) / 2;
        float yoff = (IconHeight - tb.height) / 2;
        float baseline = 0;
        if (VueUtil.isMacPlatform()) {
            yoff += tb.height;
            yoff += tb.y;
            xoff += tb.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
            row.draw(g, xoff, yoff);

            tb.y += yoff;
            tb.y -= tb.height;
            baseline = tb.y + tb.height;
            
            if (debug||DEBUG_LAYOUT) {
                // draw a red bounding box for testing
                tb.x += xoff;
                tb.y = -tb.y; // if any descent below baseline, will be non-zero
                g.setStroke(new java.awt.BasicStroke(0.5f));
                g.setColor(Color.red);
                g.draw(tb);
            }
                
        } else {
            // This is cleaner, thus I'm assuming the PC
            // implementation is also cleaner, and worthy of being
            // the default case.
                
            row.draw(g, -tb.x + xoff, -tb.y + yoff);
            baseline = yoff + tb.height;

            if (debug||DEBUG_LAYOUT) {
                // draw a red bounding box for testing
                tb.x = xoff;
                tb.y = yoff;
                g.setStroke(new java.awt.BasicStroke(0.5f));
                g.setColor(Color.red);
                g.draw(tb);
            }
        }

        // an experiment in semantic zoom
        if (dc.zoom >= 8.0 && getResource() != null) {
            g.setFont(MinisculeFont);
            g.drawString(getResource().toString(), 0, IconHeight+2);
        }


        if (hasNotes()) {
            Font f = FONT_ICON.deriveFont((float) (FONT_ICON.getSize() - 3));
            g.setFont(f);
            g.setColor(Color.gray);
            g.drawString("notes", IconWidth+IconPadRight, IconHeight-1);
            //g.drawString("notes", childOffseX(), IconHeight-1);
            //g.drawString("notes", IconWidth/4, IconHeight-1);
        }
        
        g.translate(-iconX, -iconY);
    }
    

    /** for persistance
     * @deprecated */
    public NodeShape getNodeShape()
    {
        //return this.nodeShape;
        return null;
    }
    /** @deprecated */
    public void setNodeShape(NodeShape nodeShape)
    {
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

    private final boolean debug = false;
    
    
}






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
        if (getResource() != null) {
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
