package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.ImageIcon;

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
    static Color defaultNodeColor = new Color(200, 200, 255);
    
    private Node node;
    private ImageIcon imageIcon = null;

    protected RectangularShape shape = new RoundRectangle2D.Float(0,0, 0,0, 30,30);
    //protected RectangularShape shape = new Ellipse2D.Float(0,0, 0,0);
    
    public LWNode(Node node)
    {
        if (node == null)
            throw new java.lang.IllegalArgumentException("LWNode: node is null");
        this.node = node;
        super.setLocation(node.getPosition());
    }

    // experimental
    void setImage(Image image)
    {
        imageIcon = new ImageIcon(image, "Image Description");
        setShape(new Rectangle2D.Float(0,0, 0,0));
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }

    public MapItem getMapItem()
    {
        return node;
    }
    public Node getNode()
    {
        return node;
    }

    public void setSize(int w, int h)
    {
        super.setSize(w, h);
        shape.setFrame(x,y, w,h);
    }

    public boolean contains(int x, int y)
    {
        if (imageIcon != null)
            return super.contains(x, y);
        else
            return shape.contains(x, y);
    }
    
    public void setLocation(int x, int y)
    {
        super.setLocation(x, y);
        node.setPosition(new java.awt.Point(x, y));
        shape.setFrame(x, y, this.width, this.height);
    }


    public void setShape(RectangularShape shape)
    {
        this.shape = shape;
        this.lastLabel = null;
        // this will cause size to be computed at the next rendering
    }

    //-------------------------------------------------------
    // todo perf: better to move this to a computation
    // when we know the label -- need to listen
    // to change event from our node, or
    // using a factory, couldn't we subclass the node?
    //-------------------------------------------------------
    private final int pad = 11;
    private int fontHeight;
    private void setSizeFromText(Graphics g, String label)
    {
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(label) + (pad*2);
        fontHeight = fm.getAscent() - fm.getDescent() / 2;
        int height = fontHeight + (pad*1);
        setSize(width, height);
    }
    
    private String lastLabel;
    public void draw(Graphics2D g)
    {
        super.draw(g);

        String label = this.node.getLabel();

        if (imageIcon != null) {
            // experimental
            imageIcon.paintIcon(null, g, getX(), getY());
        } else {
            if (label != lastLabel) {
                setSizeFromText(g, label);
                lastLabel = label;
            }
            g.setColor(defaultNodeColor);
            g.fill(shape);
        }

        // Draw the border
        
        if (isIndicated())
            g.setColor(COLOR_INDICATION);
        else if (isSelected())
            g.setColor(COLOR_SELECTION);
        else
            g.setColor(COLOR_DEFAULT);
        g.setStroke(STROKE_TWO);
        g.draw(shape);

        // draw the text

        if (imageIcon == null) {
            g.setColor(Color.black);
            int textBaseline = this.y + (height+fontHeight)/2;
            // box the text for seeing layout metrics
            // g.setStroke(STROKE_ONE);g.drawRect(this.x + pad, textBaseline-fontHeight, width, fontHeight);
            g.drawString(label, this.x + pad, textBaseline);
            if (node.getResource() != null) {
                g.setFont(MapViewer.smallFont);
                g.drawString(node.getResource().toString(), this.x, textBaseline+20);
            }
        }
    }
    
}
