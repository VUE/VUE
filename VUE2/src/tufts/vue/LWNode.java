package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

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
    implements Node
{
    private final int VerticalChildGap = 2;
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled
    
    private float borderWidth = 2;
    private ArrayList borderList = new ArrayList();
    private ImageIcon imageIcon = null;
    private boolean fixedAspect = false;
    private boolean autoSized = true; // compute size from label & children

    // Internal spacial layout
    private final int padX = 12;
    private final int padY = 6;
    private float fontHeight;
    private float fontStringWidth;

    
    public LWNode(String label)
    {
        this(label, 0, 0);
    }
        
    // internal convenience
    LWNode(String label, float x, float y)
    {
        setLabel(label);
        // set default shape -- todo: get this from NodeTool
        setShape(StandardShapes[4]);
        setFillColor(COLOR_NODE_DEFAULT);
        setLocation(x, y);
        setStrokeWidth(2f);
    }
    // internal convenience
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }
    // for save/restore only
    public LWNode()
    {
        setShape(StandardShapes[4]);//todo: from persist
    }

    static LWNode createTextNode(String text)
    {
        LWNode node = new LWNode(text);
        node.setShape(StandardShapes[3]);
        node.setStrokeWidth(0f);
        node.setFillColor(null);
        return node;
    }
    
    // temporary convience
    LWNode(String label, int shapeType)
    {
        this(label);
        setShape(StandardShapes[shapeType]);
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
    
    public Shape getShape()
    {
        return this.boundsShape;
    }

    public void setShape(NodeShape nodeShape)
    {
        this.fixedAspect = nodeShape.equalAspect;
        setShape(nodeShape.getShape());
    }
    
    // for persistance
    public void setShape(Shape shape)
    {
        setShape((RectangularShape)shape);
    }
    private void setShape(RectangularShape shape)
    {
        //System.out.println("SETSHAPE " + shape + " in " + this);
        //System.out.println("SETSHAPE bounds " + shape.getBounds());
        //if (shape instanceof RoundRectangle2D.Float) {
        //    RoundRectangle2D.Float rr = (RoundRectangle2D.Float) shape;
        //    System.out.println("RR arcs " + rr.getArcWidth() +"," + rr.getArcHeight());
        //}
        this.boundsShape = shape;
        this.drawnShape = (RectangularShape) shape.clone();
        adjustDrawnShape();
        this.lastLabel = null;
        // this will cause size to be computed at the next rendering
    }
    
    protected void setBorderLayer(Color color){
        borderList.add(color);
    }
    
    protected ArrayList getBorderLayers(){
        return borderList;
    }
    
    void setImage(Image image)
    {
        // experimental
        imageIcon = new ImageIcon(image, "Image Description");
        setAutoSized(false);
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public Rectangle2D getBounds()
    {
        return getShape().getBounds2D();
    }

    public void addChild(LWComponent c)
    {
        super.addChild(c);
        //c.setScale(getScale() * ChildScale);
        setScale(getScale());// to propagate color toggle hack
        layout();
    }

    public void setSize(float w, float h)
    {
        if (DEBUG_LAYOUT) System.out.println("*** LWNode setSize " + w + "x" + h + " " + this);
        setSizeNoLayout(w, h);
        layout();
    }
    
    private void setSizeNoLayout(float w, float h)
    {
        //System.out.println("setSize " + w + "x" + h);
        if (this.fixedAspect) {
            // todo: remember aspect so can keep it if other than 1/1
            if (w > h)
                h = w;
            else
                w = h;
        }
        super.setSize(w, h);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
        adjustDrawnShape();
    }

    public void setScale(float scale)
    {
        super.setScale(scale);
        this.boundsShape.setFrame(getX(), getY(), getWidth(), getHeight());
    }
    
    private void adjustDrawnShape()
    {
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
    
    public boolean contains(float x, float y)
    {
        if (imageIcon != null)
            return super.contains(x, y);
        else
            return boundsShape.contains(x, y);
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        return boundsShape.intersects(rect);
    }

    
    class PLabel extends JLabel
    //class PLabel extends JTextArea
    {
        PLabel(String txt)
        {
            super(txt);
            //setSize(getWidth()*2, getHeight());
            //setBorder(new LineBorder(Color.red));
            setOpaque(false);
            setFont(SmallFont);
            setSize(getPreferredSize());
        }
        public void draw(Graphics2D g)
        {
            //super.paintBorder(g);
            super.paintComponent(g);
            g.setColor(Color.red);
            g.setStroke(new BasicStroke(1/8f));
            g.drawRect(0,0, getWidth(), getHeight());
        }
    }

    /*
    private PLabel pLabel;
    public void mapItemChanged(MapItemEvent e)
    {
        System.out.println("mapItemChanged in LWNode " + e);
        MapItem mi = e.getSource();
        if (mi.getLabel() != lastLabel && this.fontMetrics != null) {
            // add or remove child -- recompute size based on label
            layout();
            lastLabel = mi.getLabel();
        }
        //if  (e.getWhat().endsWith("Child")) {
          //  // add or remove child -- recompute size
            //layout();
        //}
    }
*/
    
    private Rectangle2D getAllChildrenBounds()
    {
        // compute bounds based on a vertical stacking layout
        java.util.Iterator i = getChildIterator();
        float height = 0;
        float maxWidth = 0;
        float width;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            height += c.getHeight() + VerticalChildGap;
            width = c.getWidth();
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
        
    public void setLocation(float x, float y)
    {
        //System.out.println("setLocation " + this);
        super.setLocation(x, y);
        //getNode().setPosition(x, y);
        this.boundsShape.setFrame(x, y, getWidth(), getHeight());
        //this.boundsShape.setFrame(x, y, this.width, this.height);
        adjustDrawnShape();
        layoutChildren();
    }
    
    protected void layout()
    {
        if (DEBUG_LAYOUT) System.out.println("*** LAYOUT " + this);
        if (isAutoSized())
            setPreferredSize();
        layoutChildren();
        
        // could set size from label first, then layout children and
        // have it return child bounds, and set size again based on
        // that if bigger so don't have to reproduce layout logic in
        // both getAllChildren bounds and layoutChildren

        // todo: handle thru event?
        if (getParent() != null) // parent could be null constructing coded demo maps
            getParent().layout();
    }
      
    private String lastLabel;
    private Graphics graphics;

    public void setFont(Font font)
    {
        super.setFont(font);
        layout();
    }

    private FontMetrics getFontMetrics()
    {
        if (this.graphics == null)
            return null;
        return this.graphics.getFontMetrics(getFont());
    }
    
    private void setPreferredSize()
    {
        FontMetrics fm = getFontMetrics();
        if (fm == null) {
            // Happens first time, or in another view that hasn't  painted yet
            return;
        }
        
        String label = getLabel();
        //System.out.println("setPreferredSize " + label);

        float oldWidth = getWidth();
        this.fontHeight = fm.getAscent() - fm.getDescent() / 1;
        //this.fontHeight = fm.getAscent() + fm.getDescent();
        this.fontStringWidth = fm.stringWidth(label);
        float width = this.fontStringWidth + (this.padX*2) + borderWidth;
        float height = this.fontHeight + (this.padY*2) + borderWidth;
        
        if (hasChildren()) {
            // resize to inclued size of children
            height += this.padY;
            Rectangle2D childBounds = getAllChildrenBounds();
            height += childBounds.getHeight();
            if (width < childBounds.getWidth() + this.padX*2)
                width = (float) childBounds.getWidth() + this.padX*2;
        }
        
        setSizeNoLayout(width, height);
        
        if (this.width != oldWidth && lastLabel != null && !isChild()) {
            // on resize, keep the node's center the same
            setLocation(getX() + (oldWidth - this.width) / 2, getY());
        }

        /*
        pLabel = new PLabel(label);
        float w = pLabel.getWidth();
        if (w < width && !label.startsWith("<html>")) {
            pLabel.setSize((int)width, pLabel.getHeight());
            w = width;
        }
        setSize(w, pLabel.getHeight());
        */
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
        float y = (labelY() + this.padY) * getScale();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            //float childX = this.padX * getScale();
            float childX = (this.getWidth() - c.getWidth()) / 2;
            c.setLocation(getX() + childX, getY() + y);
            y += c.getHeight();
            y += VerticalChildGap * getScale();
        }
    }

    public float getLabelX()
    {
        return getX() + labelX();
    }
    public float getLabelY()
    {
        return (getY() + labelY()) - this.fontHeight;
    }

    private float labelX()
    {
        return (this.width - this.fontStringWidth) / 2;
        //return this.padX;
    }
    private float labelY()
    {
        if (hasChildren())
            return this.fontHeight + this.padY * getScale();
        else
            return (this.height+this.fontHeight) / 2f;
    }

    public void draw(Graphics2D g)
    {
        this.graphics = g;
        
        g.translate(getX(), getY());
        float scale = getScale();
        if (scale != 1f)
            g.scale(scale, scale);
        g.setFont(getFont());
            
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
            if (label != lastLabel) {
                //System.out.println("label " + lastLabel + " -> " + label);
                layout();
                lastLabel = label;
            }
            Color fillColor = getFillColor();
            if (fillColor != null) { // transparent if null
                g.setColor(fillColor);
                g.fill(drawnShape);
            }
        }

        //-------------------------------------------------------
        // Draw the border if any
        //-------------------------------------------------------
        
        if (isIndicated()) {
            g.setColor(COLOR_INDICATION);
            if (STROKE_INDICATION.getLineWidth() > this.stroke.getLineWidth())
                g.setStroke(STROKE_INDICATION);
            else
                g.setStroke(this.stroke);
            g.draw(drawnShape);
        } else if (getStrokeWidth() > 0) {
            g.setColor(getStrokeColor());
            g.setStroke(this.stroke);
            g.draw(drawnShape);
        }
        
        //draw any borders for pathways
        if (this.getBorderLayers().size() > 0) {
            ArrayList colors = getBorderLayers();
            for(int i = 0; i < getBorderLayers().size(); i++){
                g.setColor((Color)getBorderLayers().get(i));
                if (STROKE_INDICATION.getLineWidth() > this.stroke.getLineWidth())
                    g.setStroke(STROKE_INDICATION);
                else
                    g.setStroke(this.stroke);
                g.draw(drawnShape);
            }
        }
        
        //g.setStroke(new java.awt.BasicStroke(0.001f));
        //g.setColor(Color.green);
        //g.draw(boundsShape);

        //-------------------------------------------------------
        // Draw the text label if any
        //-------------------------------------------------------
        
        if (label != null && label.length() > 0) {
            float textBaseline = labelY();
            if (false) {
                // box the text for seeing layout metrics
                g.setStroke(new BasicStroke(0.0001f));
                g.setColor(Color.black);
                g.draw(new Rectangle2D.Float(this.padX,
                                             textBaseline-fontHeight,
                                             fontStringWidth,
                                             fontHeight));
            }
            g.setColor(getTextColor());
            g.drawString(label, labelX(), textBaseline);
            //g.drawString(label, getX() + labelX(), getY() + textBaseline);
        }

        /*
          // temp: show the resource--  todo: display an icon
        if (getNode().getResource() != null) {
            g.setFont(VueConstants.SmallFont);
            g.setColor(Color.black);
            g.drawString(getNode().getResource().toString(), 0, getHeight()+12);
            //g.drawString(getNode().getResource().toString(), getX(), getY() + getHeight()+17);
            }
        */

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

    static class NodeShape {
        private final String name;
        private final RectangularShape shape;
        private final boolean equalAspect;
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
        RectangularShape getShape()
        {
            return (RectangularShape) shape.clone();
        }
    }
    
    static final NodeShape StandardShapes[] = {
        //new NodeShape("Oval", new RoundRectangle2D.Float(0,0, 0,0, 180,180)),
        new NodeShape("Oval", new Ellipse2D.Float()),
        new NodeShape("Circle", new Ellipse2D.Float(), true),
        new NodeShape("Square", new Rectangle2D.Float(), true),
        new NodeShape("Rectangle", new Rectangle2D.Float()),
        new NodeShape("Rounded Rectangle", new RoundRectangle2D.Float(0,0, 0,0, 20,20)),
        //new NodeShape("Diamond", null),
        //new NodeShape("Parallelogram", null),
    };
    

    
}
