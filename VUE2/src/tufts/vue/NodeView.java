package tufts.vue;

import java.awt.*;

/**
 * NodeView.java
 *
 * Draws a view of a Node on a java.awt.Graphics context,
 * and offers code for user interaction.
 *
 * @author Scott Fraize
 * @version 3/10/03
 */
class NodeView extends LWComponent
{
    private Node node;

    public NodeView(Node node)
    {
        if (node == null)
            throw new java.lang.IllegalArgumentException("NodeView: node is null");
        this.node = node;
        super.setLocation(node.getPosition());
    }

    public Node getNode()
    {
        return node;
    }

    public MapItem getMapItem()
    {
        return getNode();
    }

    public void setLocation(int x, int y)
    {
        super.setLocation(x, y);
        node.setPosition(new java.awt.Point(x, y));
    }

    public void draw(Graphics g)
    {
        super.draw(g);
        String label = this.node.getLabel();
        final int pad = 4;

        // compute our size
        // todo perf: move this to a computation up front
        // when we know the label
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(label) + (pad*2);
        int height = fm.getHeight() + (pad*1);
        setSize(width, height);
        
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width, height);
        if (isIndicated())
            g.setColor(Color.red);
        else if (isSelected())
            g.setColor(Color.blue);
        else
            g.setColor(Color.black);
        g.drawRect(x, y, width, height);
        g.setColor(Color.black);
        g.drawString(label, this.x + pad, this.y + (height-pad));
    }
    
}
