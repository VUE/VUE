package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.ImageIcon;
import javax.swing.JTextField;

/**
 * LWNode.java
 *
 * Draws a view of a Node on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
class LWNode extends LWComponent
{
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
    
    private float borderWidth = 2;
    
    private ImageIcon imageIcon = null;
    private boolean equalAspect = false;

    protected RectangularShape boundsShape;
    protected RectangularShape drawnShape;
    
    public LWNode(Node node)
    {
        super(node);
        super.setLocation(node.getX(), node.getY());

        // set default shape -- todo: get this from NodeTool
        setShape(StandardShapes[4]);
    }

    public void setShape(NodeShape nodeShape)
    {
        this.equalAspect = nodeShape.equalAspect;
        setShape(nodeShape.getShape());
    }
    
    private void setShape(RectangularShape shape)
    {
        this.boundsShape = shape;
        this.drawnShape = (RectangularShape) shape.clone();
        adjustDrawnShape();
        this.lastLabel = null;
        // this will cause size to be computed at the next rendering
    }
    
    // experimental
    void setImage(Image image)
    {
        imageIcon = new ImageIcon(image, "Image Description");
        setShape(new Rectangle2D.Float());
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public Node getNode()
    {
        return (Node) getMapItem();
    }

    public void setSize(float w, float h)
    {
        //System.out.println("setSize " + w + "x" + h);
        if (this.equalAspect) {
            if (w > h)
                h = w;
            else
                w = h;
        }
        super.setSize(w, h);
        this.boundsShape.setFrame(getX(), getY(), w, h);
        //System.out.println("boundsShape.setFrame " + x + "," + y + " " + w + "x" + h);
        adjustDrawnShape();
    }

    private void adjustDrawnShape()
    {
        // shrink the drawn shape size by border width
        // so it fits entirely inside the bounds shape.
        
        double x = boundsShape.getX() + borderWidth / 2.0;
        double y = boundsShape.getY() + borderWidth / 2.0;
        if (VueUtil.isMacPlatform()) {
            // mac osx 1.4.1 bug?
            // note that boundsShape.getBounds() is different than on PC
            x -= 0.5;
            y -= 0.5;
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

    public void setLocation(float x, float y)
    {
        super.setLocation(x, y);
        getNode().setPosition(x, y);
        // todo arch: if this was initiated by user, we're going to be called twice here
        // because the MapItem always does at least one callback.
        this.boundsShape.setFrame(x, y, this.width, this.height);
        adjustDrawnShape();
    }

    private final int pad = 12;
    private float fontHeight;
    private float fontStringWidth;
    private void setSizeFromText(Graphics g, String label)
    {
        float oldWidth = getWidth();
        
        FontMetrics fm = g.getFontMetrics();
        this.fontHeight = fm.getAscent() - fm.getDescent() / 1;
        this.fontStringWidth = fm.stringWidth(label);
        float width = this.fontStringWidth + (pad*2) + borderWidth;
        float height = this.fontHeight + (pad*1) + borderWidth;
        if (width != oldWidth) {
            // keep the node's center the same.
            // Besides nice, this is actually important so
            // that any links to us are still rendered to our
            // center (links don't set position until they paint)
            setLocation(getX() + (oldWidth - width) / 2, getY());
        }
        // todo fixme: what's happening in other views when we do this???
        // problem: node in other view is also recomputing size and
        // then setting that size back to node!  LWComponent
        // needs to also be handling the text change & setSize
        // in the callback, and we also need once again to know
        // that THIS LWComponent should ignore the callback...
        setSize(width, height);
    }

    public Point2D getLabelOffset()
    {
        return new Point2D.Float(getLabelX(), getLabelY());
    }
    float getLabelX() {
        return this.pad;
    }
    float getLabelY() {
        return (this.height+this.fontHeight) / 2f;
    }
    
    /*
    public void mapItemChanged(MapItemChangeEvent e)
    {
        super.mapItemChanged(e);
        lastLabel = null;
        // todo: fixme - this doesn't help because links can only set their endpoint
        // AFTER the node has once been rendered at it's new size,
        // and links are rendered first in the paint loop.
        }*/
    
    private String lastLabel;
    public void draw(Graphics2D g)
    {
        super.draw(g);

        String label = getNode().getLabel();

        // System.out.println("draw " + label);

        // Fill the shape
        
        if (imageIcon != null) {
            // experimental
            imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
        } else {
            if (label != lastLabel) {
                setSizeFromText(g, label);
                lastLabel = label;
            }
            g.setColor(DEFAULT_NODE_COLOR);
            g.fill(drawnShape);
        }

        // Draw the border
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        //else if (isSelected())
            //g.setColor(COLOR_SELECTION);
        else
            g.setColor(COLOR_DEFAULT);
        g.setStroke(new java.awt.BasicStroke(borderWidth));
        g.draw(drawnShape);

        if (false) {
            g.setStroke(new java.awt.BasicStroke(0.001f));
            g.setColor(Color.green);
            g.draw(boundsShape);
        }

        if (imageIcon != null)
            return;
        
        // Draw the text
        float textBaseline = getLabelY();
        if (false) {
            // box the text for seeing layout metrics
            g.setStroke(new BasicStroke(0.0001f));
            g.setColor(Color.black);
            g.draw(new Rectangle2D.Float(getX() + this.pad,
                                         textBaseline-fontHeight,
                                         fontStringWidth,
                                         fontHeight));
        }
        g.setColor(Color.black);
        g.drawString(label, getX() + getLabelX(), getY() + textBaseline);
        if (getNode().getResource() != null) {
            g.setFont(VueConstants.SmallFont);
            g.setColor(Color.black);
            g.drawString(getNode().getResource().toString(), getX(), getY() + textBaseline+17);
        }
    }
}
