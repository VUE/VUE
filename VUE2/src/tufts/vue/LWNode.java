package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

/**
 * LWNode.java
 *
 * Draws a view of a Node on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
public class LWNode extends LWGroup
    implements Node
{
    private final int VerticalChildGap = 2;
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled
    
    private float borderWidth = 2;
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
    
    void setImage(Image image)
    {
        // experimental
        imageIcon = new ImageIcon(image, "Image Description");
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public void addChild(LWComponent c)
    {
        super.addChild(c);
        //c.setScale(getScale() * ChildScale);
        //setScale(getScale());// to prop color toggle hack
        setScale(getLayer());// to prop color toggle hack
        layout();
    }
    public void removeChild(LWComponent c)
    {
        super.removeChild(c);
        c.setScale(1f);
        if (c.isManagedColor())
            c.setFillColor(COLOR_NODE_DEFAULT);
        c.layout();
        layout();
    }

    public void setSize(float w, float h)
    {
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
        this.drawnShape.setFrame(0, 0, this.width, this.height);
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
            //height += c.getHeight() + VerticalChildGap;
            //width = c.getWidth();
            height += c.height + VerticalChildGap;
            width = c.width;
            if (width > maxWidth)
                maxWidth = width;
            
        }
        height *= ChildScale;
        maxWidth *= ChildScale;
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
        //System.out.println("layout " + this);
        setPreferredSize();
        layoutChildren();
        
        // could set size from label first, then layout children and
        // have it return child bounds, and set size again based on
        // that if bigger so don't have to reproduce layout logic in
        // both getAllChildren bounds and layoutChildren

        if (getParent() != null)
            getParent().layout();
    }
      
    private FontMetrics fontMetrics;
    private String lastLabel;

    private void saveFontMetrics(Graphics g)
    {
        FontMetrics oldMetrics = this.fontMetrics;
        this.fontMetrics = g.getFontMetrics();
        if (this.fontMetrics != oldMetrics)
            layout();
    }
    
    private void setPreferredSize()
    {
        String label = getLabel();
        //System.out.println("setPreferredSize " + label);
        if (this.fontMetrics == null) {
            //new Throwable("null FontMetrics in " + this).printStackTrace();
            // Can happen in another view that hasn't been painted yet
            return;
        }
        FontMetrics fm = this.fontMetrics;
        float oldWidth = getWidth();
        this.fontHeight = fm.getAscent() - fm.getDescent() / 1;
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

    protected void layoutChildren()
    {
        if (!hasChildren())
            return;
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
        g.translate(getX(), getY());
        float scale = getScale();
        if (scale != 1f)
            g.scale(scale, scale);
        g.setFont(getFont());
        saveFontMetrics(g);

        String label = getLabel();

        // System.out.println("draw " + label);

        // Fill the shape
        
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
            g.setColor(getFillColor());
            g.fill(drawnShape);
        }

        // Draw the border
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        //else if (isSelected())
            //g.setColor(COLOR_SELECTION);
        else
            g.setColor(getStrokeColor());
        if (imageIcon == null) {
            //g.setStroke(new java.awt.BasicStroke(borderWidth));
            // todo: cache this stroke object
            float w = getStrokeWidth();
            if (w > 0f) {
                g.setStroke(new java.awt.BasicStroke(getStrokeWidth()));
                g.draw(drawnShape);
            }
        }

        if (false) {
            g.setStroke(new java.awt.BasicStroke(0.001f));
            g.setColor(Color.green);
            g.draw(boundsShape);
        }

        if (imageIcon != null)
            return;
        
        // Draw the text
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

        /*
        if (hasChildren()) {
            if (scale != 1f)
                g.scale(1/scale, 1/scale);
            g.translate(-getX(), -getY());
            java.util.Iterator i = getChildIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.draw((Graphics2D) g.create());
            }
        }
        */
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
