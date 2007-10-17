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


package tufts.vue.gui;

import tufts.vue.DEBUG;
import tufts.vue.VueUtil;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

/**
 * Provide an immutable single line of text with an exact bounding
 * box determined by font metrics of all characters in the string.
 *
 * We can't use just a TextLayout object, as there are Mac/PC platform
 * differences in the way the bounds offsets are provided, so we
 * handle that here in draw(), as well as provide for some visual debugging
 * diagnostics.
 *
 * @see java.awt.font.TextLayout
 */

public class TextRow
{
    private static final boolean FunkyMacTextBounds = VueUtil.isMacPlatform() && VueUtil.getMacMRJVersion() < 232;
    /** default FontRenderContext: anti-alias=true and fractional-metrics=false */
    private static final FontRenderContext DefaultFontContext = new FontRenderContext(null, true, false);
    
    private final TextLayout row;
    private final Rectangle2D.Float bounds;

    public final String text;
    public final float width;
    public final float height;
    
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
    }

    public TextRow(String text, Font font)
    {
        this(text, font, DefaultFontContext);
    }

//     public Rectangle2D getBounds() {
//         return bounds;
//     }
        
    private static final BasicStroke BorderStroke = new BasicStroke(0.05f);
    //private static final BasicStroke BorderStroke = new BasicStroke(1);

    public void draw(Graphics2D g2d, float xoff, float yoff)
    {
        // Mac & PC 1.4.1 implementations haved reversed baselines
        // and differ in how descents are factored into bounds offsets
        // Update: As of Mac java 1.4.2_09-232, it appears to use the
        // same method as the PC.

        if (FunkyMacTextBounds) {
            //System.out.println("TextRow[" + text + "]@"+tb);
            
            yoff += this.height;
            yoff += this.bounds.y;
            xoff += this.bounds.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
            
            row.draw(g2d, xoff, yoff);
            
            if (DEBUG.BOXES) {
                final Rectangle2D.Float tb = (Rectangle2D.Float) this.bounds.clone();
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
                Graphics2D g = (Graphics2D) g2d.create();
                g.setStroke(BorderStroke);
                g.setColor(Color.green);
                g.draw(tb);
            }
                
        } else {
            // This is cleaner, thus I'm assuming the PC
            // implementation is also cleaner, and worthy of being
            // the default case.
                
            row.draw(g2d, -bounds.x + xoff, -bounds.y + yoff);
            //baseline = yoff + tb.height;

            if (DEBUG.BOXES) {
                final Rectangle2D.Float tb = (Rectangle2D.Float) this.bounds.clone();
                // draw a red bounding box for testing
                tb.x = xoff;
                tb.y = yoff;
                g2d.setStroke(BorderStroke);
                g2d.setColor(Color.green);
                g2d.draw(tb);
            }
        }
    }

    public String toString() {
        return "TextRow[" + text + " " + bounds + "]";
    }


}
