/*
 * PolygonIcon.java
 *
 * Created on September 17, 2003, 4:49 PM
 */

package tufts.vue;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author  akumar03
 */
public class PolygonIcon implements Icon {
    
    
    private final int DEFAULT_HEIGHT = 5;
    private final int DEFAULT_WIDTH = 5;
    private Color color = Color.BLACK;
    private int height;
    private int width;
    /** Creates a new instance of PolygonIcon */
    public PolygonIcon() {
    }
    
    public PolygonIcon(Color color) {
        this.color = color;
        height = DEFAULT_HEIGHT;
        width = DEFAULT_WIDTH;
    }
    
    public int getIconWidth() {
        return width;
    }
    
    public int getIconHeight() {
        return height;
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
       Graphics2D g2d = (Graphics2D) g;
       Shape shape = new Rectangle(x,y,width,height);
       g2d.setColor(color);
       g2d.fill(shape);
    }
    
}
