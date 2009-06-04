 /*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
    //private static final FontRenderContext DefaultFontContext = new FontRenderContext(null, true, true);
    
    private final TextLayout mRow;
    private final Rectangle2D.Float mBounds;

    public final String text;
    public final float width;
    public final float height;

    //private static final java.util.Map<String,TextRow> Cache = new java.util.HashMap();

    /** factory method, in case impl may want to cache instances of common short strings (e.g., file extensions) */
    public static TextRow instance(String txt, Font font) {

        return new TextRow(txt, font);        
        
//         // Would need to also key on the font!
//         if (txt.length() <= 3) {
//             TextRow r = Cache.get(txt);
//             if (r != null) {
//                 return r;
//             } else {
//                 r = new TextRow(txt, font);
//                 Cache.put(txt, r);
//                 return r;
//             }
//         } else {
//             return new TextRow(txt, font);
//         }
    }

    private TextRow(String text, Font font, FontRenderContext frc, TextLayout row, Rectangle2D bounds) {
        this.text = text;
        if (row == null)
            mRow = new TextLayout(text, font, frc);
        else
            mRow = row;

        final Rectangle2D.Float closeBounds = new Rectangle2D.Float();
        closeBounds.height = (float) mRow.getBounds().getHeight();
        closeBounds.width = (float) mRow.getBounds().getWidth();
        closeBounds.x = (float) mRow.getBounds().getX();
        closeBounds.y = (float) mRow.getBounds().getY();

        if (bounds == null) {
            mBounds = closeBounds;
        } else {
            if (DEBUG.TEXT) System.out.println("TextRow[" + text + "] rowbnds=" + tufts.Util.fmt(row.getBounds()));
            mBounds = (Rectangle2D.Float) bounds;
            mBounds.width = closeBounds.width; // take the more accurate width from the TextLayout
        }

        this.width = mBounds.width;
        this.height = mBounds.height;
        
        if (DEBUG.TEXT) System.out.println("TextRow[" + text + "]  bounds=" + tufts.Util.fmt(bounds));
    }
    
    public TextRow(String text, Font font, FontRenderContext frc)
    {
        this(text,
             font,
             frc,
             null,
             null);
    }

    public TextRow(String text, Graphics g)
    {
        this(text, g.getFont(), ((Graphics2D)g).getFontRenderContext());
    }

    public TextRow(String text, Font font)
    {
        // font.getStringBounds is less "precise", but more consistent for generating an
        // even bounding box around the text if you want to align the text.
        // E.g., TextLayout can report different pixel heights for strings as similar as "10", "11" and "12",
        // whereas getStringBounds, while reporting a bigger overall box, reports a consistent line height.
        
        //this(text, font, DefaultFontContext, null, font.getStringBounds(text, DefaultFontContext));
        this(text, font, DefaultFontContext, null, null); // turned off for now: leave node icons as before
        
    }

    public void drawCenter(tufts.vue.DrawContext dc, java.awt.geom.RectangularShape shape) {
        //System.out.println("Height of [" + text + "]=" + height);
        draw(dc,
             (float) (shape.getX() + (shape.getWidth() - width) / 2),
             (float) (shape.getY() + (shape.getHeight() - height) / 2));
    }
    
    public void draw(tufts.vue.DrawContext dc, float xoff, float yoff)
    {
        draw(dc.g, xoff, yoff);
    }

    public void draw(Graphics g, float xoff, float yoff) {
        draw((Graphics2D) g, xoff, yoff);
    }
    
        
    public void draw(Graphics2D g, float xoff, float yoff)
    {
        // Mac & PC 1.4.1 implementations haved reversed baselines
        // and differ in how descents are factored into bounds offsets
        // Update: As of Mac java 1.4.2_09-232, it appears to use the
        // same method as the PC.

        if (FunkyMacTextBounds) {
            //System.out.println("TextRow[" + text + "]@"+tb);
            
            yoff += this.height;
            yoff += mBounds.y;
            xoff += mBounds.x; // FYI, tb.x always appears to be zero in Mac Java 1.4.1
            
            mRow.draw(g, xoff, yoff);
            
            if (DEBUG.BOXES) {
                final Rectangle2D.Float tb = (Rectangle2D.Float) mBounds.clone();
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
                Graphics2D _g = (Graphics2D) g.create();
                tufts.vue.DrawContext.setAbsoluteStroke(_g, 1);
                _g.setColor(Color.green);
                _g.draw(tb);
                _g.dispose();
            }
                
        } else {
            // This is cleaner, thus I'm assuming the PC
            // implementation is also cleaner, and worthy of being
            // the default case.
                
            mRow.draw(g, -mBounds.x + xoff, -mBounds.y + yoff);
            //baseline = yoff + tb.height;

            if (DEBUG.BOXES) {
                final Rectangle2D.Float tb = (Rectangle2D.Float) mBounds.clone();
                // draw a red bounding box for testing
                tb.x = xoff;
                tb.y = yoff;
                tufts.vue.DrawContext.setAbsoluteStroke(g, 1);
                g.setColor(Color.green);
                g.draw(tb);
            }
        }
    }

    public String toString() {
        return "TextRow[" + text + " " + tufts.Util.fmt(mBounds) + "]";
    }


}
