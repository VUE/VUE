package tufts.vue;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

public class TextRow
    implements VueConstants
{
    private final boolean debug = DEBUG_BOXES;
    
    private String text;
    private Graphics2D g2d;
    private TextLayout row;

    private static final BasicStroke BorderStroke = new BasicStroke(0.05f);

    private Rectangle2D.Float bounds;

    float width;
    float height;
    
    public TextRow(String text, Font font, FontRenderContext frc)
    {
        this.text = text;
        this.row = new TextLayout(text, font, frc);
        this.bounds = (Rectangle2D.Float) row.getBounds();
        this.width = bounds.width;
        this.height = bounds.height;
    }

    public TextRow(String text, Graphics g)
    {
        this(text, g.getFont(), ((Graphics2D)g).getFontRenderContext());
        this.g2d = (Graphics2D) g;
    }

    public TextRow(String text, Font font)
    {
        // default FRC: anti-alias & fractional metrics
        this(text, font, new FontRenderContext(null, true, true));
    }
    
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void draw(float xoff, float yoff)
    {
        draw(this.g2d, xoff, yoff);
    }
        
    public void draw(Graphics2D g2d, float xoff, float yoff)
    {
        //g.drawString(extension, 0, (int)(genIcon.getHeight()/1.66));
        
        //if (true||DEBUG_LAYOUT) System.out.println("TextRow[" + text + "]@"+bounds);

        // Mac & PC 1.4.1 implementations haved reversed baselines
        // and differ in how descents are factored into bounds offsets

        Rectangle2D.Float tb = (Rectangle2D.Float) row.getBounds();
        float baseline = 0; // not used currently
        if (VueUtil.isMacPlatform()) {
            yoff += tb.height;
            yoff += tb.y;
            xoff += tb.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
            row.draw(g2d, xoff, yoff);

            
            if (debug||DEBUG_LAYOUT) {
                // draw a red bounding box for testing
                tb.x += xoff;
                // tb.y seems to default at to -1, and if
                // any chars have descent below baseline, tb.y
                // even less --  e.g., "txt" yields -1, "jpg" yields -2
                // that with fractional metrics OFF.  With fractional
                // metrics on, the x/y & size values go from integer
                // aligned to making use of the floating point.
                // The mac doesn't fill in tb.x, and thus when fractional
                // metrics is on, it's a tiny bit off in on the x-axis.
                tb.y = -tb.y;
                tb.y += yoff;
                tb.y -= tb.height;
                g2d.setStroke(BorderStroke);
                g2d.setColor(Color.red);
                g2d.draw(tb);
            }
                
        } else {
            // This is cleaner, thus I'm assuming the PC
            // implementation is also cleaner, and worthy of being
            // the default case.
                
            row.draw(g2d, -tb.x + xoff, -tb.y + yoff);
            baseline = yoff + tb.height;

            if (debug||DEBUG_LAYOUT) {
                // draw a red bounding box for testing
                tb.x = xoff;
                tb.y = yoff;
                g2d.setStroke(BorderStroke);
                g2d.setColor(Color.red);
                g2d.draw(tb);
            }
        }
    }
}
